package braseiro.ose.referee

import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import braseiro.ose.rng.RngStreamId
import braseiro.ose.rules.api.*
import braseiro.ose.rules.shared.SharedConfirmedRules
import braseiro.ose.rules.shared.MovementLightRules
import kotlinx.serialization.Serializable

@Serializable data class ResolutionOutcome(
    val updatedState: CampaignState,
    val feedback: String,
    val trace: RuleTrace,
    val mechanicalMutation: Boolean,
    val rngConsumed: Boolean = false
)

interface RefereeResolutionPort {
    fun resolve(envelope: CampaignEnvelope, reactionText: String, rng: NamedRngStreams): ResolutionOutcome
}

class RulesRefereeBoundary : RefereeResolutionPort {
    override fun resolve(envelope: CampaignEnvelope, reactionText: String, rng: NamedRngStreams): ResolutionOutcome {
        val text=reactionText.trim()
        if(text.equals("WAIT_TURN",ignoreCase=true)) {
            val updated=advanceCampaignTurns(envelope.campaignState,1)
            return ResolutionOutcome(updated,"1 turno (10 minutos) transcorrido; durações de luz ativas foram atualizadas.",trace(envelope.ruleProfile,"TIME.TURN","GAME_SYSTEM_SPEC §§12,14: 1 turno = 10 min; recursos temporais decrementam com o tempo"),true)
        }
        if(text.equals("ROLL_D20",ignoreCase=true)) {
            val roll=rng.rollDie(20)
            return ResolutionOutcome(envelope.campaignState,"d20: $roll",RuleTrace(EvidenceStatus.REFEREE_JUDGMENT,emptyList(),"die roll only; no rule context supplied"),false,true)
        }
        val parts=text.split(':')
        WildernessRuntimeRules.resolve(envelope,parts,rng)?.let { return it }
        RuntimeClosureBindings.tryResolve(envelope,parts,rng)?.let { return it }
        if(parts.isNotEmpty()) when(parts[0].uppercase()) {
            "ENCOUNTER_START" -> if(parts.size==2 && parts[1].isNotBlank()) {
                val activeEncounter=envelope.campaignState.game.encounter
                if(activeEncounter!=null && activeEncounter.phase!=EncounterPhase.RESOLVED) {
                    return ResolutionOutcome(envelope.campaignState,"Já existe um encontro ativo; conclua-o antes de iniciar outro.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                val encounter=EncounterSnapshot(encounterId=parts[1].trim(),history=listOf("START"))
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=encounter))
                return ResolutionOutcome(updated,"Encontro iniciado. Próximo passo: surpresa, se necessária.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19: surprise → distance → initiative → actions → conclusion"),true,false)
            }
            "ENCOUNTER_SURPRISE" -> if(parts.size in 1..2) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.SURPRISE_PENDING) {
                    return ResolutionOutcome(envelope.campaignState,"Surpresa não está pendente neste encontro.",trace(envelope.ruleProfile,"ENCOUNTER.SURPRISE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                val scope=parts.getOrNull(1)?.uppercase() ?: "BOTH"
                if(scope !in setOf("BOTH","PARTY","OPPOSITION","NONE")) {
                    return ResolutionOutcome(envelope.campaignState,"Escopo de surpresa inválido. Use BOTH, PARTY, OPPOSITION ou NONE.",trace(envelope.ruleProfile,"ENCOUNTER.SURPRISE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                val checkParty=scope=="BOTH" || scope=="PARTY"
                val partyHasVisibleLight=envelope.campaignState.game.expedition.lightSources.any{it.lit && it.remainingTurns>0}
                val checkOpposition=(scope=="BOTH" || scope=="OPPOSITION") && !partyHasVisibleLight
                val partyRoll=if(checkParty) authoritativeD6(rng) else null
                val oppositionRoll=if(checkOpposition) authoritativeD6(rng) else null
                val partySurprised=partyRoll?.let { SharedConfirmedRules.surprise(envelope.ruleProfile,it).surprised } ?: false
                val oppositionSurprised=oppositionRoll?.let { SharedConfirmedRules.surprise(envelope.ruleProfile,it).surprised } ?: false
                val advantage=when {
                    partySurprised && !oppositionSurprised -> "OPPOSITION"
                    !partySurprised && oppositionSurprised -> "PARTY"
                    else -> null
                }
                val evidence=buildList {
                    if(partyRoll!=null) addAll(SharedConfirmedRules.surprise(envelope.ruleProfile,partyRoll).trace.evidence)
                    if(oppositionRoll!=null) addAll(SharedConfirmedRules.surprise(envelope.ruleProfile,oppositionRoll).trace.evidence)
                    if(partyHasVisibleLight && (scope=="BOTH" || scope=="OPPOSITION")) add(RuleEvidence("LIGHT.SURPRISE",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§14: visible light in darkness normally prevents that side from surprising opponents"))
                    if(isEmpty()) add(RuleEvidence("ENCOUNTER.SURPRISE",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§19: no check if encounter is expected"))
                }
                val next=encounter.copy(
                    phase=EncounterPhase.DISTANCE_PENDING,
                    partySurprised=partySurprised,
                    oppositionSurprised=oppositionSurprised,
                    surpriseAdvantageSide=advantage,
                    history=encounter.history+"SURPRISE scope=$scope party=${partyRoll ?: "NO_CHECK"} opposition=${oppositionRoll ?: if(partyHasVisibleLight) "NO_CHECK_VISIBLE_PARTY_LIGHT" else "NO_CHECK"} advantage=${advantage ?: "NONE"}"
                )
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                val rt=RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE,evidence,"Only sides not expecting the encounter roll. Visible party light prevents that party from surprising opponents. One surprised side loses the initial round; both surprised gives no advantage.")
                val consumed=partyRoll!=null || oppositionRoll!=null
                return ResolutionOutcome(updated,"Surpresa: grupo=${partyRoll ?: "sem teste"} (${if(partySurprised)"surpreendido" else "não"}); oposição=${oppositionRoll ?: "sem teste"} (${if(oppositionSurprised)"surpreendida" else "não"}). Próximo passo: distância.",rt,true,consumed)
            }
            "ENCOUNTER_DISTANCE_CLASSIC" -> if(parts.size==2) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.DISTANCE_PENDING) {
                    return ResolutionOutcome(envelope.campaignState,"Distância não está pendente neste encontro.",trace(envelope.ruleProfile,"ENCOUNTER.DISTANCE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                if(envelope.ruleProfile!=RuleProfile.OSE_CLASSIC_FANTASY) {
                    return ResolutionOutcome(envelope.campaignState,"Este resolvedor de distância está comprovado apenas para o perfil Classic; use o resolvedor Advanced para campanhas Advanced.",RuleTrace(EvidenceStatus.MISSING_EVIDENCE,listOf(RuleEvidence("ENCOUNTER.DISTANCE",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§19 / active profile source")),"Classic formula cannot be copied into Advanced."),false,false)
                }
                val context=runCatching{EncounterDistanceContext.valueOf(parts[1].uppercase())}.getOrNull()
                if(context!=null) {
                    val bothSurprised=encounter.partySurprised==true && encounter.oppositionSurprised==true
                    val (diceLabel,total)=when(context) {
                        EncounterDistanceContext.DUNGEON -> "2d6" to (authoritativeD6(rng)+authoritativeD6(rng))
                        EncounterDistanceContext.WILDERNESS, EncounterDistanceContext.WATERBORNE ->
                            if(bothSurprised) "1d4" to authoritativeD4(rng)
                            else "4d6" to (1..4).sumOf { authoritativeD6(rng) }
                    }
                    val meters=total*10
                    val next=encounter.copy(phase=EncounterPhase.INITIATIVE_PENDING,distanceContext=context,distanceMeters=meters,history=encounter.history+"DISTANCE CLASSIC context=${context.name} dice=$diceLabel meters=$meters")
                    val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                    return ResolutionOutcome(updated,"Distância Classic: ${context.name} = $diceLabel × 10 m → ${meters} m. Próximo passo: iniciativa.",trace(envelope.ruleProfile,"ENCOUNTER.DISTANCE","GAME_SYSTEM_SPEC §19 / Classic p.124"),true,true)
                }
            }
            "ENCOUNTER_DISTANCE_ADVANCED" -> if(parts.size==2) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.DISTANCE_PENDING) {
                    return ResolutionOutcome(envelope.campaignState,"Distância não está pendente neste encontro.",trace(envelope.ruleProfile,"ENCOUNTER.DISTANCE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                if(envelope.ruleProfile!=RuleProfile.OSE_ADVANCED_FANTASY) {
                    return ResolutionOutcome(envelope.campaignState,"Este resolvedor de distância pertence ao perfil Advanced; não há fallback para Classic.",RuleTrace(EvidenceStatus.MISSING_EVIDENCE,listOf(RuleEvidence("ENCOUNTER.DISTANCE",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§19 / active profile source")),"Advanced formula cannot be copied into Classic."),false,false)
                }
                val context=runCatching{EncounterDistanceContext.valueOf(parts[1].uppercase())}.getOrNull()
                if(context!=null) {
                    val anySurprised=encounter.partySurprised==true || encounter.oppositionSurprised==true
                    val (diceLabel,total,multiplier)=when(context) {
                        EncounterDistanceContext.DUNGEON -> Triple("2d6",authoritativeD6(rng)+authoritativeD6(rng),3)
                        EncounterDistanceContext.WILDERNESS, EncounterDistanceContext.WATERBORNE ->
                            if(anySurprised) Triple("1d4",authoritativeD4(rng),9)
                            else Triple("4d6",(1..4).sumOf { authoritativeD6(rng) },9)
                    }
                    val meters=total*multiplier
                    val next=encounter.copy(phase=EncounterPhase.INITIATIVE_PENDING,distanceContext=context,distanceMeters=meters,history=encounter.history+"DISTANCE ADVANCED context=${context.name} dice=$diceLabel x${multiplier}m meters=$meters")
                    val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                    return ResolutionOutcome(updated,"Distância Advanced: ${context.name} = $diceLabel × ${multiplier} m → ${meters} m. Próximo passo: iniciativa.",trace(envelope.ruleProfile,"ENCOUNTER.DISTANCE","GAME_SYSTEM_SPEC §19 / Advanced Player Tome p.228"),true,true)
                }
            }
            "ENCOUNTER_INIT_SIDE" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.INITIATIVE_PENDING) {
                    return ResolutionOutcome(envelope.campaignState,"Iniciativa não está pendente neste encontro.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §§19,23"),false,false)
                }
                val resultText:String
                val pRoll:Int?
                val oRoll:Int?
                val outcome:String
                val consumed:Boolean
                when {
                    encounter.partySurprised==true && encounter.oppositionSurprised==false -> {
                        pRoll=null; oRoll=null; outcome=InitiativeOutcome.ACTS_SECOND.name; consumed=false
                        resultText="O grupo está surpreendido; a oposição recebe a rodada de vantagem e o grupo não age nela."
                    }
                    encounter.partySurprised==false && encounter.oppositionSurprised==true -> {
                        pRoll=null; oRoll=null; outcome=InitiativeOutcome.ACTS_FIRST.name; consumed=false
                        resultText="A oposição está surpreendida; o grupo recebe a rodada de vantagem e a oposição não age nela."
                    }
                    else -> {
                        val side=authoritativeD6(rng); val opposing=authoritativeD6(rng)
                        val result=SharedConfirmedRules.sideInitiative(envelope.ruleProfile,side,opposing)
                        pRoll=side; oRoll=opposing; outcome=result.outcome.name; consumed=true
                        resultText="Iniciativa: grupo=$side, oposição=$opposing → ${result.outcome.name}."
                    }
                }
                val next=encounter.copy(
                    phase=EncounterPhase.ACTIONS,
                    initiativeParty=pRoll,
                    initiativeOpposition=oRoll,
                    initiativeOutcome=outcome,
                    history=encounter.history+"INIT party=${pRoll ?: "SURPRISED"} opposition=${oRoll ?: "SURPRISED"} outcome=$outcome"
                )
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                return ResolutionOutcome(updated,"$resultText Próximo passo: ações; encontro não significa combate.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §§19,23"),true,consumed)
            }
            "ENCOUNTER_REACTION" -> if(parts.size==2) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.ACTIONS) {
                    return ResolutionOutcome(envelope.campaignState,"Reação só é resolvida durante as ações quando a disposição não é óbvia.",trace(envelope.ruleProfile,"REACTION.MONSTER","GAME_SYSTEM_SPEC §§19-20"),false,false)
                }
                val modifier=parts[1].toIntOrNull()
                if(modifier!=null) {
                    val d1=authoritativeD6(rng); val d2=authoritativeD6(rng)
                    val result=SharedConfirmedRules.reaction(envelope.ruleProfile,d1,d2,modifier)
                    val next=encounter.copy(
                        reactionTotal=result.total,
                        reactionDisposition=result.disposition.name,
                        history=encounter.history+"REACTION total=${result.total} disposition=${result.disposition.name}"
                    )
                    val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                    return ResolutionOutcome(updated,"Reação: ${result.total} → ${result.disposition.name}. Resultado permanece contextual; combate não é presumido.",result.trace,true,true)
                }
            }
            "ENCOUNTER_EVADE" -> if(parts.size in 4..8) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.ACTIONS) {
                    return ResolutionOutcome(envelope.campaignState,"Evasão só pode ser tentada antes do combate, durante as ações do encontro.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22"),false,false)
                }
                val context=runCatching{EncounterDistanceContext.valueOf(parts[1].uppercase())}.getOrNull()
                val decision=parts[2].uppercase()
                val fleeingSide=runCatching{EncounterSide.valueOf(parts[3].uppercase())}.getOrNull()
                if(context==null || fleeingSide==null || decision !in setOf("PURSUE","LET_GO")) {
                    return ResolutionOutcome(envelope.campaignState,"Formato de evasão inválido.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22"),false,false)
                }
                val pursuingSide=if(fleeingSide==EncounterSide.PARTY) EncounterSide.OPPOSITION else EncounterSide.PARTY
                if(decision=="LET_GO") {
                    val chase=ChaseSnapshot(context,fleeingSide,pursuingSide,ChasePhase.ESCAPED,0,0,distanceMeters=encounter.distanceMeters,history=listOf("NO_PURSUIT_AUTO_ESCAPE"))
                    val next=encounter.copy(chase=chase,history=encounter.history+"EVASION no_pursuit escape")
                    return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"O lado oposto não persegue; evasão automaticamente bem-sucedida. O encontro pode ser concluído.",trace(envelope.ruleProfile,chaseRuleId(context),"GAME_SYSTEM_SPEC §§19,22: no pursuit = automatic evasion"),true,false)
                }
                val fleeingSpeed=parts.getOrNull(4)?.toIntOrNull()
                val pursuingSpeed=parts.getOrNull(5)?.toIntOrNull()
                if(fleeingSpeed==null || pursuingSpeed==null || fleeingSpeed<0 || pursuingSpeed<0) {
                    return ResolutionOutcome(envelope.campaignState,"Para PURSUE, informe velocidades não negativas do fugitivo e perseguidor.",trace(envelope.ruleProfile,chaseRuleId(context),"GAME_SYSTEM_SPEC §22"),false,false)
                }
                val fleeingCount=parts.getOrNull(6)?.toIntOrNull() ?: 1
                val pursuingCount=parts.getOrNull(7)?.toIntOrNull() ?: 1
                if(fleeingCount<1 || pursuingCount<1) return ResolutionOutcome(envelope.campaignState,"Tamanho dos grupos deve ser >=1.",trace(envelope.ruleProfile,chaseRuleId(context),"GAME_SYSTEM_SPEC §22"),false,false)
                val base=ChaseSnapshot(context,fleeingSide,pursuingSide,ChasePhase.EVASION_PENDING,fleeingSpeed,pursuingSpeed,fleeingCount,pursuingCount,distanceMeters=encounter.distanceMeters)
                return resolveInitialEvasion(envelope,encounter,base,rng)
            }
            "CHASE_DUNGEON_ROUND" -> if(parts.size==2) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22"),false,false)
                val chase=encounter.chase ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma perseguição ativa.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22"),false,false)
                if(chase.context!=EncounterDistanceContext.DUNGEON || chase.phase!=ChasePhase.CHASE_ACTIVE) return ResolutionOutcome(envelope.campaignState,"Perseguição de masmorra não está ativa.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1"),false,false)
                val lineOfSight=parts[1].uppercase()=="LOS"
                if(parts[1].uppercase() !in setOf("LOS","NO_LOS")) return ResolutionOutcome(envelope.campaignState,"Use LOS ou NO_LOS.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1"),false,false)
                val round=chase.chaseRounds+1
                val phase=if(!lineOfSight) ChasePhase.ESCAPED else ChasePhase.CHASE_ACTIVE
                val exhausted=round>=30
                val nextChase=chase.copy(phase=phase,chaseRounds=round,exhausted=exhausted,history=chase.history+"DUNGEON_ROUND $round LOS=$lineOfSight exhausted=$exhausted")
                val next=encounter.copy(chase=nextChase,history=encounter.history+"CHASE_DUNGEON round=$round")
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                val msg=if(phase==ChasePhase.ESCAPED) "Linha de visão perdida; a perseguição termina para a maioria dos monstros." else "Perseguição de masmorra: rodada $round${if(exhausted)"; fugitivos exaustos após 30 rodadas" else ""}."
                return ResolutionOutcome(updated,msg,trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1 / Classic p.126 / Advanced p.230"),true,false)
            }
            "CHASE_DUNGEON_DROP" -> if(parts.size==2) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22"),false,false)
                val chase=encounter.chase ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma perseguição ativa.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22"),false,false)
                if(chase.context!=EncounterDistanceContext.DUNGEON || chase.phase!=ChasePhase.CHASE_ACTIVE) return ResolutionOutcome(envelope.campaignState,"Perseguição de masmorra não está ativa.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1"),false,false)
                val kind=parts[1].uppercase(); if(kind !in setOf("TREASURE","FOOD")) return ResolutionOutcome(envelope.campaignState,"Use TREASURE ou FOOD.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1"),false,false)
                val roll=authoritativeD6(rng); val stopped=roll<=3
                val nextChase=chase.copy(phase=if(stopped) ChasePhase.ESCAPED else ChasePhase.CHASE_ACTIVE,history=chase.history+"DROP_$kind d6=$roll stopped=$stopped")
                val next=encounter.copy(chase=nextChase)
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"$kind abandonado: d6=$roll → ${if(stopped)"perseguição interrompida" else "perseguição continua"}.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1: 3-em-6 when the pursuer is eligible to stop for the dropped resource"),true,true)
            }
            "CHASE_WILDERNESS_DAY" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                val chase=encounter.chase ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma perseguição ativa.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                if(chase.context!=EncounterDistanceContext.WILDERNESS || chase.phase!=ChasePhase.CHASE_ACTIVE) return ResolutionOutcome(envelope.campaignState,"Perseguição nos ermos não está ativa.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                val faster=chase.pursuingSpeedMeters>chase.fleeingSpeedMeters
                val roll=if(faster) authoritativeD100(rng) else null
                val caught=roll!=null && roll<=50
                val phase=if(caught) ChasePhase.CAUGHT else ChasePhase.EVASION_PENDING
                val nextChase=chase.copy(phase=phase,chaseDays=chase.chaseDays+1,history=chase.history+"WILDERNESS_DAY ${chase.chaseDays+1} intercept=${roll ?: "NO_CHECK"} caught=$caught")
                val next=encounter.copy(chase=nextChase)
                val updated=envelope.campaignState.copy(time=envelope.campaignState.time.copy(days=envelope.campaignState.time.days+1),game=envelope.campaignState.game.copy(encounter=next))
                return ResolutionOutcome(updated,if(caught)"Perseguidor mais rápido: d100=$roll ≤50; fugitivos interceptados." else "Dia de perseguição concluído; fugitivos podem tentar nova evasão ou parar.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2 / Classic p.127 / Advanced p.231"),true,roll!=null)
            }
            "CHASE_EVADE_AGAIN" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                val chase=encounter.chase ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma perseguição registrada.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                if(chase.phase!=ChasePhase.EVASION_PENDING || chase.context!=EncounterDistanceContext.WILDERNESS) return ResolutionOutcome(envelope.campaignState,"Nova evasão só é possível no passo de evasão da perseguição nos ermos.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                return resolveWildernessEvasion(envelope,encounter,chase,rng)
            }
            "CHASE_STOP_FLEEING" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                val chase=encounter.chase ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma perseguição registrada.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2"),false,false)
                if(chase.phase !in setOf(ChasePhase.EVASION_PENDING,ChasePhase.CHASE_ACTIVE)) return ResolutionOutcome(envelope.campaignState,"A perseguição já terminou.",trace(envelope.ruleProfile,chaseRuleId(chase.context),"GAME_SYSTEM_SPEC §22"),false,false)
                val nextChase=chase.copy(phase=ChasePhase.CAUGHT,history=chase.history+"FLEEING_SIDE_STOPPED")
                val next=encounter.copy(chase=nextChase)
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"O lado fugitivo parou; foi interceptado.",trace(envelope.ruleProfile,chaseRuleId(chase.context),"GAME_SYSTEM_SPEC §22"),true,false)
            }
            "ENCOUNTER_COMBAT_START" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.ACTIONS) {
                    return ResolutionOutcome(envelope.campaignState,"Combate só pode começar a partir da fase de ações do encontro.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                val next=encounter.copy(phase=EncounterPhase.COMBAT,history=encounter.history+"COMBAT_START")
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                return ResolutionOutcome(updated,"A decisão do encontro entrou em combate. Nenhuma regra de ataque/dano foi resolvida por esta transição.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19 actions may lead to combat"),true,false)
            }
            "ENCOUNTER_MORALE" -> if(parts.size in 2..3) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase!=EncounterPhase.COMBAT) {
                    return ResolutionOutcome(envelope.campaignState,"Moral de combate não está disponível fora da fase COMBAT.",trace(envelope.ruleProfile,"MORALE.MONSTER","GAME_SYSTEM_SPEC §21"),false,false)
                }
                val activeRound=encounter.combatRound
                if(activeRound!=null && activeRound.phase!=CombatActionPhase.MORALE) {
                    return ResolutionOutcome(envelope.campaignState,"Moral só pode ser resolvida na etapa MORALE da rodada ativa.",trace(envelope.ruleProfile,"MORALE.MONSTER","GAME_SYSTEM_SPEC §23.1 step 3a"),false,false)
                }
                if(!OptionIds.enabled(envelope.optionSet, OptionIds.MORALE)) {
                    return ResolutionOutcome(envelope.campaignState,"Moral opcional está desativada no OptionSet congelado.",RuleTrace(EvidenceStatus.OPTIONAL_RULE,listOf(RuleEvidence("MORALE.MONSTER",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§21 / OPT.MORALE")),"Optional rule disabled; no RNG consumed."),false,false)
                }
                val morale=parts[1].toIntOrNull(); val modifier=parts.getOrNull(2)?.toIntOrNull()?:0
                if(morale!=null && morale in 2..12 && modifier in -2..2) {
                    val d1=authoritativeD6(rng); val d2=authoritativeD6(rng)
                    val result=SharedConfirmedRules.morale(envelope.ruleProfile,d1,d2,morale,modifier)
                    val next=encounter.copy(
                        lastMoraleTotal=result.total,
                        lastMoraleHolds=result.holds,
                        history=encounter.history+"MORALE total=${result.total} holds=${result.holds}"
                    )
                    val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                    return ResolutionOutcome(updated,"Moral: 2d6=${result.total} vs ML ${result.adjustedMorale} → ${if(result.holds)"continua" else "falha de moral"}. A consequência concreta segue o contexto/referee.",result.trace,true,true)
                }
            }
            "ENCOUNTER_RESOLVE" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                if(encounter.phase==EncounterPhase.RESOLVED) {
                    return ResolutionOutcome(envelope.campaignState,"Encontro já encerrado.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19"),false,false)
                }
                val next=encounter.copy(phase=EncounterPhase.RESOLVED,history=encounter.history+"RESOLVED +1_TURN")
                val withEncounter=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next))
                val updated=advanceCampaignTurns(withEncounter,1)
                return ResolutionOutcome(updated,"Encontro encerrado; 1 turno (10 minutos) contabilizado para conclusão/reagrupamento.",trace(envelope.ruleProfile,"ENCOUNTER.CORE","GAME_SYSTEM_SPEC §19 conclusion: at least 1 turn for completed encounter"),true,false)
            }
            "COMBAT_ROUND_START" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                if(encounter.phase!=EncounterPhase.COMBAT) {
                    return ResolutionOutcome(envelope.campaignState,"Rodada de combate exige encontro na fase COMBAT.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                }
                val prior=encounter.combatRound
                if(prior!=null && prior.phase!=CombatActionPhase.COMPLETE) {
                    return ResolutionOutcome(envelope.campaignState,"A rodada ${prior.roundNumber} ainda não foi concluída.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1"),false,false)
                }
                val round=CombatRoundSnapshot(roundNumber=(prior?.roundNumber ?: 0)+1,history=listOf("ROUND_START"))
                val next=encounter.copy(combatRound=round,history=encounter.history+"COMBAT_ROUND_${round.roundNumber}_START")
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Rodada ${round.roundNumber}: declarar feitiços e recuo/fuga corpo a corpo antes da iniciativa.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1 step 1"),true,false)
            }
            "COMBAT_DECLARE" -> if(parts.size==4) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                val round=encounter.combatRound
                    ?: return ResolutionOutcome(envelope.campaignState,"Inicie a rodada antes das declarações.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1"),false,false)
                if(round.phase!=CombatActionPhase.DECLARATIONS) {
                    return ResolutionOutcome(envelope.campaignState,"Declarações só podem ocorrer antes da iniciativa.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1 step 1"),false,false)
                }
                val side=runCatching{EncounterSide.valueOf(parts[1].uppercase())}.getOrNull()
                val actorId=parts[2].trim()
                val declaration=parts[3].uppercase()
                if(declaration=="SPELL") {
                    return ResolutionOutcome(envelope.campaignState,"Declaração de magia exige uma entrada memorizada canônica. Use COMBAT_DECLARE_SPELL:SIDE:actorId:memoryId.",trace(envelope.ruleProfile,"COMBAT.SPELL","GAME_SYSTEM_SPEC §§23.1,29.2-29.3"),false,false)
                }
                if(side==null || actorId.isBlank() || declaration !in setOf("NONE","RETREAT","FLEE")) {
                    return ResolutionOutcome(envelope.campaignState,"Declaração inválida. Use PARTY/OPPOSITION, actorId e NONE/RETREAT/FLEE; magia usa COMBAT_DECLARE_SPELL.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1 step 1"),false,false)
                }
                val key="${side.name}:$actorId"
                val nextRound=round.copy(declarations=round.declarations+(key to declaration),history=round.history+"DECLARE $key=$declaration")
                val next=encounter.copy(combatRound=nextRound)
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Declaração registrada para $actorId: $declaration.",trace(envelope.ruleProfile,when(declaration){"RETREAT"->"COMBAT.RETREAT";"FLEE"->"COMBAT.FLEE";else->"COMBAT.ROUND"},"GAME_SYSTEM_SPEC §23.1"),true,false)
            }
            "COMBAT_DECLARATIONS_DONE" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                val round=encounter.combatRound
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma rodada ativa.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                if(round.phase!=CombatActionPhase.DECLARATIONS) {
                    return ResolutionOutcome(envelope.campaignState,"A etapa de declarações já terminou.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1"),false,false)
                }
                val nextRound=round.copy(phase=CombatActionPhase.INITIATIVE,history=round.history+"DECLARATIONS_DONE")
                val next=encounter.copy(combatRound=nextRound)
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Declarações encerradas. Próximo passo: iniciativa por lado.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1 step 2"),true,false)
            }
            "COMBAT_INIT_SIDE" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                val round=encounter.combatRound
                    ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma rodada ativa.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                if(round.phase!=CombatActionPhase.INITIATIVE) {
                    return ResolutionOutcome(envelope.campaignState,"A iniciativa só ocorre depois das declarações.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §23.2"),false,false)
                }
                if(OptionPolicyContract.enabled(envelope.ruleProfile,envelope.optionSet,OptionIds.INDIVIDUAL_INITIATIVE)) {
                    return ResolutionOutcome(envelope.campaignState,"A campanha ativou iniciativa individual; esta rodada por lado não pode substituir a regra opcional.",RuleTrace(EvidenceStatus.OPTIONAL_RULE,listOf(RuleEvidence("INIT.INDIVIDUAL",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§23.2 / OPT.INDIVIDUAL_INITIATIVE")),"Individual initiative requires its own actor-level ordering; no silent fallback to side initiative."),false,false)
                }
                val party=authoritativeD6(rng); val opposition=authoritativeD6(rng)
                if(party==opposition) {
                    val tied=round.copy(partyInitiative=party,oppositionInitiative=opposition,history=round.history+"INIT_TIE $party-$opposition")
                    val next=encounter.copy(combatRound=tied)
                    return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Iniciativa empatada em $party. A fonte permite rerrolar ou ações simultâneas; este fluxo exige decisão explícita e não escolhe silenciosamente.",RuleTrace(EvidenceStatus.REFEREE_JUDGMENT,listOf(RuleEvidence("INIT.SIDE",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§23.2")),"Tie resolution is explicit referee choice."),true,true)
                }
                val winner=if(party>opposition) EncounterSide.PARTY else EncounterSide.OPPOSITION
                val nextRound=round.copy(phase=CombatActionPhase.MORALE,activeSide=winner,winningSide=winner,partyInitiative=party,oppositionInitiative=opposition,history=round.history+"INIT $party-$opposition winner=$winner")
                val next=encounter.copy(combatRound=nextRound)
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Iniciativa: grupo=$party, oposição=$opposition. $winner age primeiro; ordem: moral, movimento, projéteis, magia, corpo a corpo.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §23.1-23.2"),true,true)
            }
            "COMBAT_INIT_TIE_SIMULTANEOUS" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §23.2"),false,false)
                val round=encounter.combatRound ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma rodada ativa.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §23.2"),false,false)
                if(round.phase!=CombatActionPhase.INITIATIVE || round.partyInitiative==null || round.partyInitiative!=round.oppositionInitiative) {
                    return ResolutionOutcome(envelope.campaignState,"Não há empate de iniciativa pendente.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §23.2"),false,false)
                }
                val nextRound=round.copy(phase=CombatActionPhase.MORALE,activeSide=EncounterSide.PARTY,winningSide=null,simultaneous=true,history=round.history+"INIT_TIE_SIMULTANEOUS")
                val next=encounter.copy(combatRound=nextRound)
                return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Empate aceito como simultâneo. O executor deve preservar simultaneidade dos efeitos letais; a sequência de registro começa pelo grupo apenas como ordenação técnica.",trace(envelope.ruleProfile,"INIT.SIDE","GAME_SYSTEM_SPEC §23.2 tie may be simultaneous"),true,false)
            }
            "COMBAT_STEP_DONE" -> if(parts.size==1) {
                val encounter=envelope.campaignState.game.encounter ?: return ResolutionOutcome(envelope.campaignState,"Nenhum encontro ativo.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                val round=encounter.combatRound ?: return ResolutionOutcome(envelope.campaignState,"Nenhuma rodada ativa.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23"),false,false)
                if(round.phase in setOf(CombatActionPhase.DECLARATIONS,CombatActionPhase.INITIATIVE,CombatActionPhase.COMPLETE) || round.activeSide==null) {
                    return ResolutionOutcome(envelope.campaignState,"Não há etapa resolutiva ativa para concluir.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1"),false,false)
                }
                val nextPhase=when(round.phase) {
                    CombatActionPhase.MORALE->CombatActionPhase.MOVEMENT
                    CombatActionPhase.MOVEMENT->CombatActionPhase.MISSILE
                    CombatActionPhase.MISSILE->CombatActionPhase.SPELL
                    CombatActionPhase.SPELL->CombatActionPhase.MELEE
                    CombatActionPhase.MELEE->CombatActionPhase.COMPLETE
                    else->round.phase
                }
                if(nextPhase!=CombatActionPhase.COMPLETE) {
                    val nextRound=round.copy(phase=nextPhase,history=round.history+"STEP_DONE ${round.activeSide}:${round.phase}")
                    val next=encounter.copy(combatRound=nextRound)
                    return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"${round.activeSide}: ${round.phase} concluída; próxima etapa $nextPhase.",trace(envelope.ruleProfile,combatRuleId(nextPhase),"GAME_SYSTEM_SPEC §23.1 ordered side actions"),true,false)
                }
                val first=round.winningSide ?: EncounterSide.PARTY
                if(round.activeSide==first) {
                    val second=if(first==EncounterSide.PARTY) EncounterSide.OPPOSITION else EncounterSide.PARTY
                    val nextRound=round.copy(phase=CombatActionPhase.MORALE,activeSide=second,history=round.history+"SIDE_DONE $first")
                    val next=encounter.copy(combatRound=nextRound)
                    return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"$first concluiu sua sequência; $second age agora começando por moral.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §23.1 step 4"),true,false)
                }
                val completeRound=round.copy(phase=CombatActionPhase.COMPLETE,history=round.history+"ROUND_COMPLETE")
                val nextEncounter=encounter.copy(combatRound=completeRound,history=encounter.history+"COMBAT_ROUND_${round.roundNumber}_COMPLETE")
                val beforeCommit=envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=nextEncounter))
                val committed=RuntimeClosureBindings.commitSimultaneousAtRoundEnd(beforeCommit)
                val updated=advanceCampaignRounds(committed,1)
                return ResolutionOutcome(updated,"Rodada ${round.roundNumber} concluída; efeitos simultâneos foram consolidados atomicamente e 10 segundos contabilizados.",trace(envelope.ruleProfile,"COMBAT.ROUND","GAME_SYSTEM_SPEC §§12,23: simultaneous ties preserve both sides' actions; 1 round = 10 seconds"),true,false)
            }
            "EXPEDITION_LOAD" -> if(parts.size==5) {
                val armor=runCatching{ArmorLoadCategory.valueOf(parts[1].uppercase())}.getOrNull()
                val treasure=parts[2].toIntOrNull(); val equipment=parts[3].toIntOrNull()
                val significant=parts[4].toBooleanStrictOrNull()
                if(armor==null || treasure==null || equipment==null || treasure<0 || equipment<0 || significant==null) {
                    return ResolutionOutcome(envelope.campaignState,"Carga inválida. Use armor NONE/LIGHT/HEAVY, pesos não negativos e true/false para tesouro significativo.",trace(envelope.ruleProfile,"ENC.BASIC","GAME_SYSTEM_SPEC §13"),false,false)
                }
                val policy=OptionPolicyContract.resolve(envelope.ruleProfile,envelope.optionSet)
                if(!policy.valid) return ResolutionOutcome(envelope.campaignState,"OptionSet inválido: ${policy.errors.joinToString()}",RuleTrace(EvidenceStatus.MISSING_EVIDENCE,emptyList(),"Invalid frozen option policy blocks derived movement."),false,false)
                val rate=MovementLightRules.movementRate(envelope.ruleProfile,envelope.optionSet,armor,treasure,equipment,significant)
                val expedition=envelope.campaignState.game.expedition.copy(
                    armorLoad=armor,treasureWeightCoins=treasure,equipmentWeightCoins=equipment,significantTreasure=significant,
                    encumbranceMode=rate.encumbranceMode,baseMoveMeters=rate.baseMoveMeters,encounterMoveMeters=rate.encounterMoveMeters,immobile=rate.immobile
                )
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(expedition=expedition))
                return ResolutionOutcome(updated,"Carga ${rate.encumbranceMode}: base ${rate.baseMoveMeters}m, encontro ${rate.encounterMoveMeters}m${if(rate.immobile)"; imóvel por excesso de carga" else ""}.",trace(envelope.ruleProfile,when(rate.encumbranceMode){EncumbranceMode.NONE->"ENC.NONE";EncumbranceMode.BASIC->"ENC.BASIC";EncumbranceMode.DETAILED->"ENC.DETAILED"},"GAME_SYSTEM_SPEC §13 / profile source tables"),true,false)
            }
            "MOVE_LIMIT" -> if(parts.size in 2..3) {
                val context=parts[1].uppercase()
                val expedition=envelope.campaignState.game.expedition
                val rate=braseiro.ose.rules.shared.MovementRate(expedition.encumbranceMode,expedition.baseMoveMeters,expedition.encounterMoveMeters,expedition.immobile)
                val limit=when(context) {
                    "DUNGEON_UNKNOWN"->MovementLightRules.dungeonMoveLimitMeters(rate,false,false)
                    "DUNGEON_KNOWN"->MovementLightRules.dungeonMoveLimitMeters(rate,true,parts.getOrNull(2)?.uppercase()=="REFEREE_ALLOW_3X")
                    "ENCOUNTER_DUNGEON"->MovementLightRules.encounterMoveLimitMeters(rate,EncounterDistanceContext.DUNGEON)
                    "ENCOUNTER_WILDERNESS"->MovementLightRules.encounterMoveLimitMeters(rate,EncounterDistanceContext.WILDERNESS)
                    "SWIM"->MovementLightRules.swimMoveLimitMeters(rate)
                    else->-1
                }
                if(limit<0) return ResolutionOutcome(envelope.campaignState,"Contexto de movimento não suportado por este resolvedor.",trace(envelope.ruleProfile,"MOVE.BASE","GAME_SYSTEM_SPEC §12.3"),false,false)
                return ResolutionOutcome(envelope.campaignState,"Limite de movimento $context: ${limit}m.",trace(envelope.ruleProfile,when(context){"DUNGEON_UNKNOWN","DUNGEON_KNOWN"->"MOVE.DUNGEON";"ENCOUNTER_DUNGEON","ENCOUNTER_WILDERNESS"->"MOVE.ENCOUNTER";"SWIM"->"MOVE.SWIM";else->"MOVE.BASE"},"GAME_SYSTEM_SPEC §12.3"),false,false)
            }
            "LIGHT_IGNITE" -> if(parts.size==4) {
                val kind=runCatching{LightSourceKind.valueOf(parts[1].uppercase())}.getOrNull()
                val lightId=parts[2].trim(); val carrierId=parts[3].trim()
                if(kind==null || lightId.isBlank() || carrierId.isBlank()) return ResolutionOutcome(envelope.campaignState,"Fonte de luz inválida.",trace(envelope.ruleProfile,"LIGHT.SOURCE","GAME_SYSTEM_SPEC §14"),false,false)
                val expedition=envelope.campaignState.game.expedition
                if(expedition.lightSources.any{it.lightId==lightId && it.lit}) return ResolutionOutcome(envelope.campaignState,"A fonte $lightId já está acesa.",trace(envelope.ruleProfile,"LIGHT.SOURCE","GAME_SYSTEM_SPEC §14"),false,false)
                val source=LightSourceSnapshot(lightId,kind,carrierId,MovementLightRules.defaultLightDurationTurns(kind),true)
                val lights=expedition.lightSources.filterNot{it.lightId==lightId}+source
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(expedition=expedition.copy(lightSources=lights)))
                return ResolutionOutcome(updated,"${kind.name} $lightId acesa por $carrierId: ${source.remainingTurns} turnos de duração.",trace(envelope.ruleProfile,"LIGHT.DURATION","GAME_SYSTEM_SPEC §14 + equipment record: torch=6 turns; lantern oil flask=24 turns"),true,false)
            }
            "LIGHT_EXTINGUISH" -> if(parts.size==2) {
                val id=parts[1].trim(); val expedition=envelope.campaignState.game.expedition
                val idx=expedition.lightSources.indexOfFirst{it.lightId==id}
                if(idx<0) return ResolutionOutcome(envelope.campaignState,"Fonte de luz não encontrada.",trace(envelope.ruleProfile,"LIGHT.SOURCE","GAME_SYSTEM_SPEC §14"),false,false)
                val lights=expedition.lightSources.map{if(it.lightId==id)it.copy(lit=false) else it}
                val updated=envelope.campaignState.copy(game=envelope.campaignState.game.copy(expedition=expedition.copy(lightSources=lights)))
                return ResolutionOutcome(updated,"Fonte $id apagada; duração restante preservada.",trace(envelope.ruleProfile,"LIGHT.SOURCE","GAME_SYSTEM_SPEC §14"),true,false)
            }
            "SURPRISE" -> if(parts.size==1) {
                val roll=authoritativeD6(rng)
                val result=SharedConfirmedRules.surprise(envelope.ruleProfile,roll)
                return ResolutionOutcome(envelope.campaignState,"Surpresa: d6=$roll → ${if(result.surprised)"surpreendido" else "não surpreendido"}.",result.trace,false,true)
            }
            "INIT_SIDE" -> if(parts.size==1) {
                val side=authoritativeD6(rng); val opposing=authoritativeD6(rng)
                val result=SharedConfirmedRules.sideInitiative(envelope.ruleProfile,side,opposing)
                return ResolutionOutcome(envelope.campaignState,"Iniciativa: lado=$side, oposição=$opposing → ${result.outcome.name}.",result.trace,false,true)
            }
            "REACTION" -> if(parts.size==2) {
                val modifier=parts[1].toIntOrNull()
                if(modifier!=null) {
                    val d1=authoritativeD6(rng); val d2=authoritativeD6(rng)
                    val result=SharedConfirmedRules.reaction(envelope.ruleProfile,d1,d2,modifier)
                    return ResolutionOutcome(envelope.campaignState,"Reação: ${result.total} → ${result.disposition.name}.",result.trace,false,true)
                }
            }
            "MORALE" -> if(parts.size in 2..3) {
                if(!OptionIds.enabled(envelope.optionSet, OptionIds.MORALE)) {
                    return ResolutionOutcome(envelope.campaignState,"Moral opcional está desativada no OptionSet congelado.",RuleTrace(EvidenceStatus.OPTIONAL_RULE,listOf(RuleEvidence("MORALE.MONSTER",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§21 / OPT.MORALE")),"Optional rule disabled; no RNG consumed."),false,false)
                }
                val morale=parts[1].toIntOrNull(); val modifier=parts.getOrNull(2)?.toIntOrNull()?:0
                if(morale!=null && modifier in -2..2) {
                    val d1=authoritativeD6(rng); val d2=authoritativeD6(rng)
                    val result=SharedConfirmedRules.morale(envelope.ruleProfile,d1,d2,morale,modifier)
                    return ResolutionOutcome(envelope.campaignState,"Moral: 2d6=${result.total} vs ML ${result.adjustedMorale} → ${if(result.holds)"continua" else "falha de moral"}.",result.trace,false,true)
                }
            }
            "X_IN_6" -> if(parts.size==2) {
                val target=parts[1].toIntOrNull()
                if(target!=null && target in 0..6) {
                    val roll=authoritativeD6(rng); val result=SharedConfirmedRules.xIn6(envelope.ruleProfile,roll,target)
                    return ResolutionOutcome(envelope.campaignState,"Teste $target-em-6: $roll → ${if(result.success)"sucesso" else "falha"}.",result.trace,false,true)
                }
            }
            "D100" -> if(parts.size==2) {
                val chance=parts[1].toIntOrNull()
                if(chance!=null && chance in 0..100) {
                    val roll=authoritativeD100(rng); val result=SharedConfirmedRules.percentile(envelope.ruleProfile,roll,chance)
                    return ResolutionOutcome(envelope.campaignState,"d100: $roll vs $chance% → ${if(result.success)"sucesso" else "falha"}.",result.trace,false,true)
                }
            }
            "ABILITY" -> if(parts.size==3) {
                val key=runCatching{AttributeKey.valueOf(parts[1].uppercase())}.getOrNull()
                val activeCharacter=envelope.campaignState.party.characterIds
                    .firstOrNull()
                    ?.let(envelope.campaignState.game.characters::get)
                if(key!=null && activeCharacter!=null) {
                    val mod=parts[2].toIntOrNull()
                    if(mod!=null) {
                        val roll=authoritativeD20(rng)
                        val r=SharedConfirmedRules.abilityCheck(envelope.ruleProfile,roll,activeCharacter.attributes.get(key),mod)
                        return ResolutionOutcome(envelope.campaignState,"Teste de ${key.name}: ${if(r.success)"sucesso" else "falha"} (${r.modifiedRoll} vs ${r.attribute}; d20=$roll).",r.trace,false,true)
                    }
                }
            }
            "SAVE" -> if(parts.size==2) {
                val target=parts[1].toIntOrNull()
                if(target!=null) {
                    val roll=authoritativeD20(rng)
                    val r=SharedConfirmedRules.savingThrow(envelope.ruleProfile,roll,target)
                    return ResolutionOutcome(envelope.campaignState,"Jogada de proteção: ${if(r.success)"sucesso" else "falha"} ($roll vs $target).",r.trace,false,true)
                }
            }
            "ATTACK_DESC" -> if(parts.size==4) {
                val th=parts[1].toIntOrNull(); val ac=parts[2].toIntOrNull(); val mod=parts[3].toIntOrNull()
                if(listOf(th,ac,mod).all{it!=null}) {
                    val roll=authoritativeD20(rng)
                    val r=SharedConfirmedRules.attack(envelope.ruleProfile,AttackInput(roll,AttackArmorMode.DESCENDING,th!!,0,ac!!,mod!!))
                    return ResolutionOutcome(envelope.campaignState,"Ataque: ${if(r.hit)"acerto" else "erro"} (d20=$roll).",r.trace,false,true)
                }
            }
            "ATTACK_ASC" -> if(parts.size==4) {
                if(!OptionIds.enabled(envelope.optionSet, OptionIds.ASCENDING_ARMOR_CLASS)) {
                    return ResolutionOutcome(
                        envelope.campaignState,
                        "CAA está desativada no OptionSet congelado desta campanha.",
                        RuleTrace(EvidenceStatus.OPTIONAL_RULE,listOf(RuleEvidence("AC.ASCENDING",envelope.ruleProfile,"GAME_SYSTEM_SPEC.md","§24 / OPT.ASCENDING_AC")),"Optional rule disabled; no RNG consumed."),
                        false,false
                    )
                }
                val bonus=parts[1].toIntOrNull(); val ac=parts[2].toIntOrNull(); val mod=parts[3].toIntOrNull()
                if(listOf(bonus,ac,mod).all{it!=null}) {
                    val roll=authoritativeD20(rng)
                    val r=SharedConfirmedRules.attack(envelope.ruleProfile,AttackInput(roll,AttackArmorMode.ASCENDING,19,bonus!!,ac!!,mod!!))
                    return ResolutionOutcome(envelope.campaignState,"Ataque CAA: ${if(r.hit)"acerto" else "erro"} (d20=$roll).",r.trace,false,true)
                }
            }
            "DUNGEON_SEARCH_ROOM" -> {
                val state = advanceCampaignTurns(envelope.campaignState, 1)
                val activeChar = state.party.characterIds.firstOrNull()?.let(state.game.characters::get)
                val isDwarfOrElf = activeChar?.classIds?.any { it.contains("DWARF", ignoreCase = true) || it.contains("ELF", ignoreCase = true) } == true
                val target = if (isDwarfOrElf) 2 else 1
                val roll = authoritativeD6(rng)
                val success = roll <= target
                val msg = "Busca minuciosa na sala (1 turno / 10 min): d6=$roll vs $target-em-6. ${if (success) "Sucesso! Sinais, passagens secretas ou armadilhas identificadas." else "Nenhuma passagem secreta ou mecanismo oculto detectado."}"
                return ResolutionOutcome(
                    state, msg,
                    RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, listOf(RuleEvidence("DUNGEON.SEARCH", envelope.ruleProfile, "Classic Rules Tome p. 96", "Searching: 1 turn; 1-in-6 chance (demihumans 2-in-6)"))),
                    true, true
                )
            }
            "DUNGEON_LISTEN_DOOR" -> {
                val state = advanceCampaignTurns(envelope.campaignState, 1)
                val activeChar = state.party.characterIds.firstOrNull()?.let(state.game.characters::get)
                val isDemihuman = activeChar?.classIds?.any { it.contains("DWARF", ignoreCase = true) || it.contains("ELF", ignoreCase = true) || it.contains("HALFLING", ignoreCase = true) } == true
                val target = if (isDemihuman) 2 else 1
                val roll = authoritativeD6(rng)
                val success = roll <= target
                val msg = "Escutar ruídos na porta (1 turno / 10 min): d6=$roll vs $target-em-6. ${if (success) "Ruídos e movimentação abafada ouvidos do outro lado!" else "Silêncio absoluto do outro lado da porta."}"
                return ResolutionOutcome(
                    state, msg,
                    RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, listOf(RuleEvidence("DUNGEON.LISTEN", envelope.ruleProfile, "Classic Rules Tome p. 97", "Listening at doors: 1 turn; 1-in-6 chance (demihumans 2-in-6)"))),
                    true, true
                )
            }
            "DUNGEON_OPEN_STUCK_DOOR" -> {
                val state = advanceCampaignTurns(envelope.campaignState, 1)
                val activeChar = state.party.characterIds.firstOrNull()?.let(state.game.characters::get)
                val str = activeChar?.attributes?.get(AttributeKey.STR) ?: 10
                val target = when {
                    str >= 18 -> 5
                    str in 16..17 -> 4
                    str in 13..15 -> 3
                    str in 9..12 -> 2
                    else -> 1
                }
                val roll = authoritativeD6(rng)
                val success = roll <= target
                val msg = "Forçar porta emperrada (1 turno / 10 min; FOR $str): d6=$roll vs $target-em-6. ${if (success) "A porta range violentamente e cede!" else "A porta resiste com firmeza aos impactos."}"
                return ResolutionOutcome(
                    state, msg,
                    RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, listOf(RuleEvidence("DUNGEON.DOOR", envelope.ruleProfile, "Classic Rules Tome p. 97", "Open stuck door: 1 turn; modified by STR modifier / x-in-6 table"))),
                    true, true
                )
            }
            "SETTLEMENT_REST_DAY" -> {
                val s = envelope.campaignState
                val game = s.game
                val healed = game.characters.mapValues { (_, c) ->
                    if (c.hitPoints.dead) c else c.copy(hitPoints = c.hitPoints.heal(rng.rollDie(3))).canonical()
                }
                val t = s.time
                val turns = t.turns + 144
                val hours = t.hours + 24
                val days = t.days + 1
                val expedition = game.expedition
                val lights = MovementLightRules.advanceLightByTurns(expedition.lightSources, 144)
                val pools = MovementLightRules.advanceOilPoolsByTurns(expedition.oilPools, 144)
                val base = s.copy(
                    time = t.copy(turns = turns, hours = hours, days = days),
                    game = game.copy(characters = healed, expedition = expedition.copy(lightSources = lights, oilPools = pools))
                )
                val updated = RuntimeClosureBindings.advanceMagicTurns(base, 144)
                return ResolutionOutcome(
                    updated, "Dia completo de descanso no assentamento: 1d3 PV recuperados para personagens vivos; tempo avançado em 1 dia.",
                    RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, listOf(RuleEvidence("SETTLEMENT.REST", envelope.ruleProfile, "Classic Rules Tome p. 9", "Full day bed rest in safe settlement heals 1d3 hp"))),
                    true, true
                )
            }
        }
        val judgmentSource = if (envelope.ruleProfile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Referee's Tome v1.2"
        return ResolutionOutcome(
            envelope.campaignState,
            "A ação não exigiu uma mecânica específica; o Mestre deve arbitrar pelo contexto sem inventar um sistema universal de perícias.",
            RuleTrace(
                EvidenceStatus.REFEREE_JUDGMENT,
                listOf(RuleEvidence("REFEREE.JUDGMENT", envelope.ruleProfile, judgmentSource, "Referee procedures / role of the referee; GAME_SYSTEM_SPEC §40")),
                "Free text without a proven specific procedure is handled as explicit referee judgment; no universal skill/check system is invented."
            ),
            false
        )
    }

    private fun resolveInitialEvasion(envelope:CampaignEnvelope, encounter:EncounterSnapshot, chase:ChaseSnapshot, rng:NamedRngStreams):ResolutionOutcome = when(chase.context) {
        EncounterDistanceContext.DUNGEON -> {
            val escaped=chase.fleeingSpeedMeters>chase.pursuingSpeedMeters
            val nextChase=chase.copy(phase=if(escaped) ChasePhase.ESCAPED else ChasePhase.CHASE_ACTIVE,history=chase.history+"DUNGEON_INITIAL faster=$escaped")
            val next=encounter.copy(chase=nextChase)
            ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),if(escaped)"Fugitivo mais rápido: evasão automática." else "Fugitivo não é mais rápido: perseguição de masmorra iniciada.",trace(envelope.ruleProfile,"CHASE.DUNGEON","GAME_SYSTEM_SPEC §22.1 / Classic p.126 / Advanced p.230"),true,false)
        }
        EncounterDistanceContext.WILDERNESS -> resolveWildernessEvasion(envelope,encounter,chase,rng)
        EncounterDistanceContext.WATERBORNE -> {
            val chance=waterEvasionChance(chase.fleeingSpeedMeters,chase.pursuingSpeedMeters)
            val roll=authoritativeD100(rng); val escaped=roll<=chance
            val nextChase=chase.copy(phase=if(escaped) ChasePhase.ESCAPED else ChasePhase.CHASE_ACTIVE,evasionChancePercent=chance,lastEvasionRoll=roll,cannotBeReacquiredUntilNextDay=escaped,history=chase.history+"WATER_EVASION chance=$chance roll=$roll escaped=$escaped")
            val next=encounter.copy(chase=nextChase)
            ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Evasão aquática: $roll vs $chance% → ${if(escaped)"sucesso" else "falha; perseguição iniciada"}.",trace(envelope.ruleProfile,"CHASE.WATER","GAME_SYSTEM_SPEC §22.3 / Classic p.126 / Advanced p.230"),true,true)
        }
    }

    private fun resolveWildernessEvasion(envelope:CampaignEnvelope, encounter:EncounterSnapshot, chase:ChaseSnapshot, rng:NamedRngStreams):ResolutionOutcome {
        val fleeingSurprised=if(chase.fleeingSide==EncounterSide.PARTY) encounter.partySurprised==true else encounter.oppositionSurprised==true
        val pursuerSurprised=if(chase.pursuingSide==EncounterSide.PARTY) encounter.partySurprised==true else encounter.oppositionSurprised==true
        if(fleeingSurprised && !pursuerSurprised) {
            return ResolutionOutcome(envelope.campaignState,"O lado fugitivo está surpreendido; a fonte diz que normalmente não pode evadir. Qualquer chance ambiental exige julgamento explícito do árbitro.",RuleTrace(EvidenceStatus.REFEREE_JUDGMENT,listOf(RuleEvidence("CHASE.WILDERNESS",envelope.ruleProfile,if(envelope.ruleProfile==RuleProfile.OSE_CLASSIC_FANTASY)"Classic Rules Tome" else "Advanced Player's Tome","GAME_SYSTEM_SPEC §22.2")),"No universal numeric environment modifier invented."),false,false)
        }
        if(!fleeingSurprised && pursuerSurprised) {
            val nextChase=chase.copy(phase=ChasePhase.ESCAPED,evasionChancePercent=100,history=chase.history+"WILDERNESS_SURPRISE_AUTO_ESCAPE")
            val next=encounter.copy(chase=nextChase)
            return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"O lado fugitivo surpreendeu o perseguidor: evasão automática.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2 / Classic p.127 / Advanced p.231"),true,false)
        }
        val base=wildernessBaseChance(chase.fleeingCount,chase.pursuingCount)
        val speedMod=when { chase.pursuingSpeedMeters>=chase.fleeingSpeedMeters*2 && chase.fleeingSpeedMeters>0 -> -25; chase.fleeingSpeedMeters>=chase.pursuingSpeedMeters*2 && chase.pursuingSpeedMeters>0 -> 25; else -> 0 }
        val chance=(base+speedMod).coerceAtLeast(5).coerceAtMost(100)
        val roll=authoritativeD100(rng); val escaped=roll<=chance
        val nextChase=chase.copy(phase=if(escaped) ChasePhase.ESCAPED else ChasePhase.CHASE_ACTIVE,evasionChancePercent=chance,lastEvasionRoll=roll,history=chase.history+"WILDERNESS_EVASION base=$base speedMod=$speedMod chance=$chance roll=$roll escaped=$escaped")
        val next=encounter.copy(chase=nextChase)
        return ResolutionOutcome(envelope.campaignState.copy(game=envelope.campaignState.game.copy(encounter=next)),"Evasão nos ermos: $roll vs $chance% (base $base, velocidade ${if(speedMod>=0)"+" else ""}$speedMod) → ${if(escaped)"sucesso" else "falha; perseguição iniciada"}.",trace(envelope.ruleProfile,"CHASE.WILDERNESS","GAME_SYSTEM_SPEC §22.2 / Classic p.127 / Advanced p.231"),true,true)
    }

    private fun wildernessBaseChance(fleeing:Int,pursuing:Int):Int = when {
        fleeing<=4 -> when { pursuing==1 -> 50; pursuing<=3 -> 70; else -> 90 }
        fleeing<=12 -> when { pursuing<=3 -> 35; pursuing<=8 -> 50; else -> 70 }
        fleeing<=24 -> when { pursuing<=6 -> 25; pursuing<=16 -> 35; else -> 50 }
        else -> when { pursuing<=10 -> 10; pursuing<=30 -> 25; else -> 35 }
    }

    private fun waterEvasionChance(fleeingSpeed:Int,pursuingSpeed:Int):Int {
        if(fleeingSpeed>pursuingSpeed) return 80
        val slower=pursuingSpeed-fleeingSpeed
        return when { slower<=9 -> 50; slower<=18 -> 40; slower<=27 -> 35; slower<=36 -> 25; else -> 10 }
    }

    private fun chaseRuleId(context:EncounterDistanceContext)=when(context){EncounterDistanceContext.DUNGEON->"CHASE.DUNGEON";EncounterDistanceContext.WILDERNESS->"CHASE.WILDERNESS";EncounterDistanceContext.WATERBORNE->"CHASE.WATER"}

    private fun combatRuleId(phase:CombatActionPhase)=when(phase){
        CombatActionPhase.MORALE->"COMBAT.ROUND"
        CombatActionPhase.MOVEMENT->"COMBAT.MOVEMENT"
        CombatActionPhase.MISSILE->"COMBAT.MISSILE"
        CombatActionPhase.SPELL->"COMBAT.SPELL"
        CombatActionPhase.MELEE->"COMBAT.MELEE"
        else->"COMBAT.ROUND"
    }

    private fun advanceCampaignTurns(state:CampaignState, turns:Long):CampaignState {
        require(turns>=0)
        val afterTime=advanceTurns(state.time,turns)
        val expedition=state.game.expedition
        val lights=MovementLightRules.advanceLightByTurns(expedition.lightSources,turns)
        val pools=MovementLightRules.advanceOilPoolsByTurns(expedition.oilPools,turns)
        val base=state.copy(time=afterTime,game=state.game.copy(expedition=expedition.copy(lightSources=lights,oilPools=pools)))
        return RuntimeClosureBindings.advanceMagicTurns(base,turns.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    private fun advanceCampaignRounds(state:CampaignState, rounds:Long):CampaignState {
        require(rounds>=0)
        val oldRounds=state.time.rounds
        val newRounds=oldRounds+rounds
        val crossedTurns=newRounds/60-oldRounds/60
        val base=state.copy(time=state.time.copy(rounds=newRounds))
        val withMagic=RuntimeClosureBindings.advanceMagicRounds(base,rounds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        return if(crossedTurns>0) advanceCampaignTurns(withMagic,crossedTurns) else withMagic
    }

    private fun authoritativeD4(rng:NamedRngStreams):Int=rng.rollDie(4)
    private fun authoritativeD20(rng:NamedRngStreams):Int=rng.rollDie(20)
    private fun authoritativeD6(rng:NamedRngStreams):Int=rng.rollDie(6)
    private fun authoritativeD100(rng:NamedRngStreams):Int=rng.rollDie(100)

    private fun trace(profile:RuleProfile,id:String,note:String)=RuleTrace(EvidenceStatus.CANONICAL_RULE,listOf(RuleEvidence(id,profile,"GAME_SYSTEM_SPEC.md",note)))
    private fun advanceTurns(t:TimeState,n:Long):TimeState {
        require(n>=0)
        val newTurns=t.turns+n
        val crossedHours=newTurns/6 - t.turns/6
        val newHours=t.hours+crossedHours
        val crossedDays=newHours/24 - t.hours/24
        return t.copy(turns=newTurns,hours=newHours,days=t.days+crossedDays)
    }
}
