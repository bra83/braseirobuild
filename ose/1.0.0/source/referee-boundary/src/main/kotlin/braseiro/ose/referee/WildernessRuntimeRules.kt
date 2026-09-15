package braseiro.ose.referee

import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import braseiro.ose.rng.RngStreamId
import braseiro.ose.rules.api.EvidenceStatus
import braseiro.ose.rules.api.RuleEvidence
import braseiro.ose.rules.api.RuleTrace
import braseiro.ose.rules.shared.AdvancedEncounterRuntimeRegistry
import braseiro.ose.rules.shared.ClassicWildernessEncounterRuntimeRegistry
import braseiro.ose.rules.shared.MovementLightRules

/**
 * Source-bound wilderness day procedure. Hexes are a BRASEIRO representation only:
 * Classic uses the source's typical small map scale of 9 km/hex; Advanced uses 6 miles/hex.
 * Both map the persisted normalized movement rates 36/27/18/9 to 4/3/2/1 clear hexes/day.
 */
object WildernessRuntimeRules {
    private data class Dir(val key:String,val dq:Int,val dr:Int)
    private val dirs=listOf(
        Dir("E",1,0),Dir("NE",1,-1),Dir("NW",0,-1),Dir("W",-1,0),Dir("SW",-1,1),Dir("SE",0,1)
    )

    fun resolve(envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams): ResolutionOutcome? = when(parts.firstOrNull()?.uppercase()) {
        "WILD_TRAVEL_DAY" -> travelDay(envelope,parts,rng,false,false)
        "WILD_TRAVEL_FORAGE_DAY" -> travelDay(envelope,parts,rng,false,true)
        "WILD_FORCED_MARCH_DAY" -> travelDay(envelope,parts,rng,true,false)
        "WILD_REST_DAY" -> restDay(envelope,parts,rng)
        "WILD_HUNT_DAY" -> huntDay(envelope,parts,rng)
        else -> null
    }

    private fun travelDay(envelope:CampaignEnvelope,parts:List<String>,rng:NamedRngStreams,forced:Boolean,forage:Boolean):ResolutionOutcome {
        if(parts.size!=2) return no(envelope,"Use ${if(forced)"WILD_FORCED_MARCH_DAY" else if(forage)"WILD_TRAVEL_FORAGE_DAY" else "WILD_TRAVEL_DAY"}:E|NE|NW|W|SW|SE.","WILD.TRAVEL")
        val intended=dirs.firstOrNull{it.key==parts[1].uppercase()} ?: return no(envelope,"Direção inválida.","WILD.TRAVEL")
        val state=envelope.campaignState; val game=state.game
        if(game.session.phase!="ACTIVE") return no(envelope,"Viagem nos ermos requer sessão ACTIVE.","WILD.TRAVEL")
        if(game.encounter?.phase?.let { it!=EncounterPhase.RESOLVED } == true) return no(envelope,"Conclua o encontro ativo antes de viajar.","WILD.TRAVEL")
        if(game.expedition.immobile || game.expedition.baseMoveMeters<=0) return no(envelope,"O grupo está imóvel pela carga atual.","WILD.TRAVEL")
        if(game.expedition.forcedMarchRestRequired) return no(envelope,"Após marcha forçada, a fonte exige um dia inteiro de descanso antes de nova viagem.","WILD.TRAVEL")
        val pos=state.position.primary as? SpatialRef.Hex ?: return no(envelope,"A posição canônica atual não é um hex de ermos.","WILD.TRAVEL")
        val world=game.hexWorld ?: return missing(envelope,"WILD.TRAVEL","HexWorld canônico ausente.")
        val origin=world.cells.firstOrNull{it.q==pos.q&&it.r==pos.r} ?: return missing(envelope,"WILD.TRAVEL","Hex atual ausente do mundo canônico.")

        // Determine losing direction before travel. Roads/trails are reliable routes for this product runtime.
        val intendedFirst=cell(world,pos.q+intended.dq,pos.r+intended.dr)
            ?: return no(envelope,"A rota escolhida deixa o mapa canônico.","WILD.TRAVEL")
        if(intendedFirst.hydrology==HydrologyKind.SEA_OCEAN) return no(envelope,"A rota entra em mar aberto; use procedimento aquático, não viagem terrestre.","WILD.TRAVEL")
        val lostChance=if(origin.road||origin.trail||intendedFirst.road||intendedFirst.trail) 0 else lostChance(intendedFirst.terrain)
        val lostRoll=if(lostChance>0) d6(rng) else null
        val lost=lostRoll!=null && lostRoll<=lostChance
        val actual=if(lost) {
            val alternatives=dirs.filter{it!=intended}.filter{cell(world,pos.q+it.dq,pos.r+it.dr)?.hydrology!=HydrologyKind.SEA_OCEAN && cell(world,pos.q+it.dq,pos.r+it.dr)!=null}
            if(alternatives.isEmpty()) intended else alternatives[(rng.draw(RngStreamId.WORLD_EVENTS)%alternatives.size.toUInt()).toInt()]
        } else intended

        val baseHexes=(game.expedition.baseMoveMeters/9).coerceAtLeast(1)
        val budget=baseHexes*6*(if(forced)3 else 2)/2 // normal=base*6; forced=base*9 (+50%)
        var spent=0; var q=pos.q; var r=pos.r; val traversed=mutableListOf<Pair<Int,Int>>()
        while(true){
            val n=cell(world,q+actual.dq,r+actual.dr) ?: break
            if(n.hydrology==HydrologyKind.SEA_OCEAN) break
            val cost=terrainCost(n)
            if(spent+cost>budget) break
            spent+=cost; q=n.q; r=n.r; traversed+=q to r
        }
        if(traversed.isEmpty()) return no(envelope,"A distância diária disponível não permite entrar no próximo hex por esta rota.","WILD.TRAVEL")

        var nextWorld=world
        for((tq,tr) in traversed) nextWorld=reveal(nextWorld,tq,tr)
        val destination=cell(nextWorld,q,r)!!
        val encounterChance=encounterChance(destination.terrain)
        val encounterRoll=d6(rng)
        val encounterHit=encounterRoll<=encounterChance
        val encounter=if(encounterHit) buildEncounter(envelope,destination,rng) else null

        val hasRanger=game.characters.values.any { "RANGER" in it.classIds }
        val forageChance=if(forage && envelope.ruleProfile==RuleProfile.OSE_ADVANCED_FANTASY && hasRanger) 2 else if(forage) 1 else 0
        val forageRoll=if(forage) d6(rng) else null
        val foragedFor=if(forageRoll!=null && forageRoll<=forageChance) d6(rng) else 0

        var nextGame=game.copy(
            hexWorld=nextWorld,
            encounter=encounter,
            expedition=game.expedition.copy(
                wildernessTravelDaysSinceRest=game.expedition.wildernessTravelDaysSinceRest+1,
                wildernessRestOverdue=game.expedition.wildernessTravelDaysSinceRest+1>6,
                forcedMarchRestRequired=forced
            )
        )
        val partyNeed=state.party.characterIds.size.coerceAtLeast(1)
        val rationResult=consumeDailyRations(nextGame,(partyNeed-foragedFor).coerceAtLeast(0))
        nextGame=rationResult.first
        val advanced=advanceOneDay(state.copy(position=PositionState(SpatialRef.Hex(pos.spatialEntityId,q,r)),game=nextGame))
        val facts=resourceFactsFromGame(advanced.game,state.resources,rationResult.second)
        val updated=advanced.copy(resources=facts,playerKnowledge=revealKnowledge(state.playerKnowledge,traversed))
        val status=if(lost) EvidenceStatus.REFEREE_JUDGMENT else EvidenceStatus.CANONICAL_PROCEDURE
        val src=if(envelope.ruleProfile==RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome pp.120-121" else "Advanced Player's Tome pp.224-225 (audited transcription; authorized gap source)"
        val ev=listOf(
            RuleEvidence("WILD.TRAVEL",envelope.ruleProfile,src,"daily travel, terrain modifiers, forced march, end-of-day resources"),
            RuleEvidence("WILD.LOST",envelope.ruleProfile,src,"lost chance by terrain"),
            RuleEvidence("WILD.ENCOUNTER",envelope.ruleProfile,src,"one normal wandering check/day; chance by terrain")
        )
        val note=buildString {
            append("BRASEIRO hex projection preserves source distance math. ")
            if(lost) append("Lost check succeeded; actual direction ${actual.key} selected by BRASEIRO_REFEREE_POLICY_LOST_DIRECTION_V1 as an explicit exercise of referee judgment. ")
            if(forage) append("forage=$forageRoll/$forageChance fed=$foragedFor. ")
            append("encounter=${encounter?.encounterId ?: "none"}; rationsConsumed=${rationResult.second}.")
        }
        val feedback="Viagem: ${traversed.size} hex(es) para ${actual.key}${if(lost)" (grupo perdeu a direção; curso pretendido ${intended.key})" else ""}; ${if(forage)"forragear d6=$forageRoll/$forageChance, alimenta=$foragedFor; " else ""}encontro d6=$encounterRoll/$encounterChance${if(encounterHit)" → encontrado" else " → nenhum"}; rações consumidas=${rationResult.second}."
        return ResolutionOutcome(updated,feedback,RuleTrace(status,ev,note),true,true)
    }

    private fun restDay(envelope:CampaignEnvelope,parts:List<String>,rng:NamedRngStreams):ResolutionOutcome {
        if(parts.size!=1) return no(envelope,"Use WILD_REST_DAY.","WILD.REST")
        val s=envelope.campaignState; val game=s.game
        if(game.session.phase!="ACTIVE") return no(envelope,"Descanso requer sessão ACTIVE.","WILD.REST")
        if(s.position.primary !is SpatialRef.Hex) return no(envelope,"Descanso de ermos requer posição em hex.","WILD.REST")
        if(game.encounter?.phase?.let { it!=EncounterPhase.RESOLVED } == true) return no(envelope,"Conclua o encontro antes do descanso de dia inteiro.","WILD.REST")
        val healed=game.characters.mapValues { (_,c) -> if(c.hitPoints.dead) c else c.copy(hitPoints=c.hitPoints.heal(d3(rng))).canonical() }
        val rationResult=consumeDailyRations(game.copy(characters=healed,expedition=game.expedition.copy(wildernessTravelDaysSinceRest=0,wildernessRestOverdue=false,forcedMarchRestRequired=false)),s.party.characterIds.size.coerceAtLeast(1))
        val advanced=advanceOneDay(s.copy(game=rationResult.first))
        val updated=advanced.copy(resources=resourceFactsFromGame(advanced.game,s.resources,rationResult.second))
        val src=if(envelope.ruleProfile==RuleProfile.OSE_CLASSIC_FANTASY)"Classic Rules Tome p.121 + healing" else "Advanced Player's Tome p.225 + healing"
        return ResolutionOutcome(updated,"Dia inteiro de descanso concluído; contador de viagem reiniciado; cura natural aplicada a personagens vivos; rações consumidas=${rationResult.second}.",RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE,listOf(RuleEvidence("WILD.REST",envelope.ruleProfile,src,"one rest day after each six travel days / full-day rest"),RuleEvidence("HEAL.NATURAL",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","full day complete rest: 1d3 hp"))),true,true)
    }

    private fun huntDay(envelope:CampaignEnvelope,parts:List<String>,rng:NamedRngStreams):ResolutionOutcome {
        if(parts.size!=1) return no(envelope,"Use WILD_HUNT_DAY.","WILD.HUNT")
        val s=envelope.campaignState; val game=s.game
        if(game.session.phase!="ACTIVE" || s.position.primary !is SpatialRef.Hex) return no(envelope,"Caça requer sessão ACTIVE em ermos.","WILD.HUNT")
        if(game.expedition.forcedMarchRestRequired) return no(envelope,"Após marcha forçada, o próximo dia deve ser descanso completo.","WILD.HUNT")
        if(game.encounter?.phase?.let { it!=EncounterPhase.RESOLVED } == true) return no(envelope,"Conclua o encontro antes de caçar.","WILD.HUNT")
        val hasRanger=game.characters.values.any { "RANGER" in it.classIds }
        val chance=if(envelope.ruleProfile==RuleProfile.OSE_ADVANCED_FANTASY && hasRanger) 5 else 1
        val roll=d6(rng); val prey=roll<=chance
        val pos=s.position.primary as SpatialRef.Hex; val world=game.hexWorld ?: return missing(envelope,"WILD.HUNT","HexWorld canônico ausente.")
        val current=cell(world,pos.q,pos.r) ?: return missing(envelope,"WILD.HUNT","Hex atual ausente.")
        val encounterChance=encounterChance(current.terrain); val encounterRoll=d6(rng); val encounterHit=encounterRoll<=encounterChance
        val encounter=if(encounterHit) buildEncounter(envelope,current,rng) else null
        val rationResult=consumeDailyRations(game.copy(encounter=encounter),s.party.characterIds.size.coerceAtLeast(1))
        val advanced=advanceOneDay(s.copy(game=rationResult.first))
        val updated=advanced.copy(resources=resourceFactsFromGame(advanced.game,s.resources,rationResult.second))
        val note=if(prey)"Presa adequada encontrada; a fonte condiciona alimento a conseguir capturá-la, portanto nenhum alimento foi criado automaticamente." else "Nenhuma presa adequada encontrada."
        return ResolutionOutcome(updated,"Caça: d6=$roll/$chance → ${if(prey)"presa encontrada" else "sem presa"}; encontro d6=$encounterRoll/$encounterChance${if(encounterHit)" → encontrado" else " → nenhum"}.",RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE,listOf(RuleEvidence("WILD.HUNT",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md §16","hunting consumes day; chance to find edible animals; no travel/rest")),note),true,true)
    }

    private fun buildEncounter(envelope:CampaignEnvelope,cell:HexCellSnapshot,rng:NamedRngStreams):EncounterSnapshot {
        val terrain=encounterTerrain(cell)
        val d8=d8(rng)
        val raw=if(envelope.ruleProfile==RuleProfile.OSE_CLASSIC_FANTASY){
            val d12=d12(rng); val r=ClassicWildernessEncounterRuntimeRegistry.resolve(envelope.ruleProfile,terrain,d8,d12)
                ?: error("CLASSIC_WILDERNESS_UNBOUND:$terrain:$d8:$d12")
            "CLASSIC table=${r.tableId} column=${r.column} d8=$d8 d12=$d12 type=${r.content.type} ref=${r.content.ref} raw=${r.content.raw}"
        } else {
            val d20=d20(rng); val r=AdvancedEncounterRuntimeRegistry.resolveWilderness(terrain,d8,d20)
            "ADVANCED table=${r.tableId} column=${r.column} d8=$d8 d20=$d20 type=${r.content.type} ref=${r.content.ref} raw=${r.content.raw} authority=${AdvancedEncounterRuntimeRegistry.SOURCE_AUTHORITY}"
        }
        return EncounterSnapshot(encounterId="wild:${envelope.campaignState.time.days+1}:${cell.q},${cell.r}:${envelope.campaignState.game.revision}",phase=EncounterPhase.SURPRISE_PENDING,distanceContext=EncounterDistanceContext.WILDERNESS,history=listOf("WILDERNESS_RANDOM_ENCOUNTER",raw))
    }

    private fun terrainCost(c:HexCellSnapshot):Int {
        if(c.road) return 4 // +50% speed versus normal cost 6
        return when(c.terrain){
            TerrainKind.HILLS,TerrainKind.DESERT,TerrainKind.FOREST,TerrainKind.BARREN_ADV,TerrainKind.BROKEN_LANDS_ADV -> 9 // 33% slower
            TerrainKind.MOUNTAINS,TerrainKind.SWAMP,TerrainKind.JUNGLE -> 12 // 50% slower
            TerrainKind.CLEAR_GRASSLANDS -> 6
        }
    }
    private fun lostChance(t:TerrainKind)=when(t){
        TerrainKind.CLEAR_GRASSLANDS->1
        TerrainKind.HILLS,TerrainKind.MOUNTAINS,TerrainKind.FOREST,TerrainKind.BARREN_ADV,TerrainKind.BROKEN_LANDS_ADV->2
        TerrainKind.DESERT,TerrainKind.SWAMP,TerrainKind.JUNGLE->3
    }
    private fun encounterChance(t:TerrainKind)=when(t){
        TerrainKind.CLEAR_GRASSLANDS->1
        TerrainKind.HILLS,TerrainKind.DESERT,TerrainKind.FOREST,TerrainKind.BARREN_ADV,TerrainKind.BROKEN_LANDS_ADV->2
        TerrainKind.MOUNTAINS,TerrainKind.SWAMP,TerrainKind.JUNGLE->3
    }
    private fun encounterTerrain(c:HexCellSnapshot):String=when(c.hydrology){
        HydrologyKind.LAKE_RIVER->"LAKE_RIVER"
        HydrologyKind.SEA_OCEAN->"OCEAN_SEA"
        HydrologyKind.NONE->when(c.terrain){
            TerrainKind.CLEAR_GRASSLANDS->"CLEAR_GRASSLANDS"
            TerrainKind.HILLS,TerrainKind.MOUNTAINS,TerrainKind.BARREN_ADV,TerrainKind.BROKEN_LANDS_ADV->"BARREN_HILLS_MOUNTAINS"
            TerrainKind.FOREST->"FOREST"; TerrainKind.DESERT->"DESERT"; TerrainKind.SWAMP->"SWAMP"; TerrainKind.JUNGLE->"JUNGLE"
        }
    }
    private fun cell(w:HexWorldSnapshot,q:Int,r:Int)=w.cells.firstOrNull{it.q==q&&it.r==r}
    private fun reveal(w:HexWorldSnapshot,q:Int,r:Int):HexWorldSnapshot {
        return w.copy(cells=w.cells.map { c ->
            val here = c.q == q && c.r == r
            c.copy(known = c.known || here, visible = here)
        }).canonical()
    }
    private fun hexDistance(q1:Int,r1:Int,q2:Int,r2:Int):Int { val dq=q1-q2; val dr=r1-r2; val ds=(q1+r1)-(q2+r2); return (kotlin.math.abs(dq)+kotlin.math.abs(dr)+kotlin.math.abs(ds))/2 }
    private fun revealKnowledge(k:PlayerKnowledgeState,path:List<Pair<Int,Int>>)=k.copy(knownFactIds=(k.knownFactIds+path.map{"hex:${it.first},${it.second}"}).distinct().sorted())

    private fun consumeDailyRations(game:GameExtensions,needed:Int):Pair<GameExtensions,Int>{
        var remaining=needed; val items=game.inventory.items.toMutableMap()
        for(item in items.values.filter{it.kind==InventoryItemKind.FOOD&&it.quantity>0}.sortedBy{it.itemId}){
            if(remaining<=0) break
            val use=minOf(remaining,item.quantity); items[item.itemId]=item.copy(quantity=item.quantity-use); remaining-=use
        }
        return game.copy(inventory=game.inventory.copy(items=items).canonical()) to (needed-remaining)
    }
    private fun resourceFactsFromGame(game:GameExtensions,old:ResourceState,rationsConsumed:Int):ResourceState {
        val rations=game.inventory.items.values.filter{it.kind==InventoryItemKind.FOOD}.sumOf{it.quantity}
        val torches=game.inventory.items.values.filter{it.lightSourceKind==LightSourceKind.TORCH}.sumOf{it.quantity}
        val preserved=old.resourceFacts.filterNot{it.startsWith("rations:")||it.startsWith("torch:")||it.startsWith("movement:")}
        return ResourceState(preserved+listOf("rations:$rations","torch:$torches","movement:${game.expedition.baseMoveMeters}","last_rations_consumed:$rationsConsumed")).canonical()
    }
    private fun advanceOneDay(s:CampaignState):CampaignState {
        // 1 day = 24 hours = 144 ten-minute turns. Keep time, light and spell durations coherent.
        val t=s.time; val turns=t.turns+144; val hours=t.hours+24; val days=t.days+1
        val expedition=s.game.expedition
        val lights=MovementLightRules.advanceLightByTurns(expedition.lightSources,144)
        val pools=MovementLightRules.advanceOilPoolsByTurns(expedition.oilPools,144)
        val base=s.copy(time=t.copy(turns=turns,hours=hours,days=days),game=s.game.copy(expedition=expedition.copy(lightSources=lights,oilPools=pools)))
        return RuntimeClosureBindings.advanceMagicTurns(base,144)
    }

    private fun no(e:CampaignEnvelope,msg:String,id:String)=ResolutionOutcome(e.campaignState,msg,RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE,listOf(RuleEvidence(id,e.ruleProfile,"GAME_SYSTEM_SPEC.md","§16"))),false,false)
    private fun missing(e:CampaignEnvelope,id:String,msg:String)=ResolutionOutcome(e.campaignState,msg,RuleTrace(EvidenceStatus.MISSING_EVIDENCE,listOf(RuleEvidence(id,e.ruleProfile,"GAME_SYSTEM_SPEC.md","§16")),msg),false,false)
    private fun d3(r:NamedRngStreams)=(r.draw(RngStreamId.RULES_DICE)%3u).toInt()+1
    private fun d6(r:NamedRngStreams)=(r.draw(RngStreamId.RULES_DICE)%6u).toInt()+1
    private fun d8(r:NamedRngStreams)=(r.draw(RngStreamId.RULES_DICE)%8u).toInt()+1
    private fun d12(r:NamedRngStreams)=(r.draw(RngStreamId.RULES_DICE)%12u).toInt()+1
    private fun d20(r:NamedRngStreams)=(r.draw(RngStreamId.RULES_DICE)%20u).toInt()+1
}
