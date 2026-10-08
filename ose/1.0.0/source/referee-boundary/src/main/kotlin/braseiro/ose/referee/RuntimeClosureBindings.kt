package braseiro.ose.referee

import braseiro.ose.model.*
import braseiro.ose.rng.NamedRngStreams
import braseiro.ose.rng.RngStreamId
import braseiro.ose.rules.api.*
import braseiro.ose.rules.shared.*

/**
 * Additive runtime bindings for the closure phase. Commands here are state-driven:
 * the UI supplies identities/intents only; all rolls, weights, ranges, damage dice,
 * saving throw targets and spell memory are read from canonical state/content bindings.
 */
object RuntimeClosureBindings {
    fun tryResolve(envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams): ResolutionOutcome? {
        if (parts.isEmpty()) return null
        return when (parts[0].uppercase()) {
            // Disable the old caller-supplied load path before the legacy switch can see it.
            "EXPEDITION_LOAD" -> outcome(
                envelope.campaignState,
                "Carga numérica fornecida pelo chamador foi desativada. Use inventário canônico + EXPEDITION_REFRESH.",
                "INVENTORY.CORE", envelope.ruleProfile, EvidenceStatus.CANONICAL_RULE,
                "GAME_SYSTEM_SPEC §§13,30: movement/load must derive from canonical inventory state.",
                mutation = false, rng = false
            )
            "EXPEDITION_REFEREE_SIGNIFICANT" -> refereeSignificant(envelope, parts)
            "EXPEDITION_REFRESH" -> refreshExpedition(envelope)

            // Disable direct creation of mechanical light duration without inventory consumption.
            "LIGHT_IGNITE" -> outcome(
                envelope.campaignState,
                "Acendimento direto sem inventário foi desativado. Use LIGHT_IGNITE_ITEM.",
                "LIGHT.SOURCE", envelope.ruleProfile, EvidenceStatus.CANONICAL_RULE,
                "GAME_SYSTEM_SPEC §§14,30: light is a temporal inventory resource.",
                mutation = false, rng = false
            )
            "LIGHT_IGNITE_ITEM" -> igniteItem(envelope, parts)
            "OIL_POOL_IGNITE" -> igniteOilPool(envelope, parts)

            "COMBAT_DECLARE_SPELL" -> declareSpell(envelope, parts)
            "COMBAT_MOVE" -> combatMove(envelope, parts)
            "COMBAT_ATTACK_MISSILE" -> combatAttack(envelope, parts, rng, CombatActionPhase.MISSILE)
            "COMBAT_ATTACK_MELEE" -> combatAttack(envelope, parts, rng, CombatActionPhase.MELEE)
            "COMBAT_SAVE" -> combatSave(envelope, parts, rng)
            "COMBAT_CAST_DECLARED" -> castDeclared(envelope, parts)

            "TREASURE_RECOVER" -> recoverTreasure(envelope, parts)
            "XP_AWARD" -> awardExperience(envelope, parts)
            "XP_LEVEL_UP" -> levelUp(envelope, parts, rng)
            "RETAINER_RECRUIT" -> recruitRetainer(envelope, parts, rng)
            "RETAINER_LOYALTY" -> testRetainerLoyalty(envelope, parts, rng)
            else -> null
        }
    }

    private fun refereeSignificant(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if (parts.size != 3) return outcome(envelope.campaignState,"Use EXPEDITION_REFEREE_SIGNIFICANT:ownerId:true|false.","ENC.BASIC",envelope.ruleProfile,EvidenceStatus.REFEREE_JUDGMENT,"GAME_SYSTEM_SPEC §13.2",false,false)
        val owner = parts[1].trim(); val significant = parts[2].toBooleanStrictOrNull()
        if (owner.isBlank() || significant == null) return outcome(envelope.campaignState,"Parâmetros inválidos.","ENC.BASIC",envelope.ruleProfile,EvidenceStatus.REFEREE_JUDGMENT,"GAME_SYSTEM_SPEC §13.2",false,false)
        if (owner !in envelope.campaignState.party.characterIds) return outcome(envelope.campaignState,"O personagem não pertence à companhia ativa.","ENC.BASIC",envelope.ruleProfile,EvidenceStatus.REFEREE_JUDGMENT,"GAME_SYSTEM_SPEC §13.2",false,false)
        val game = envelope.campaignState.game
        val inv = InventoryRuntimeRules.setBasicSignificantTreasure(game.inventory, owner, significant)
        val base = envelope.campaignState.copy(game = game.copy(inventory = inv))
        return refreshExpedition(envelope.copy(campaignState = base), EvidenceStatus.REFEREE_JUDGMENT,
            "Tesouro significativo BASIC registrado por julgamento do referee e movimento recalculado.")
    }

    private fun refreshExpedition(
        envelope: CampaignEnvelope,
        evidenceStatus: EvidenceStatus = EvidenceStatus.CANONICAL_RULE,
        message: String = "Carga e movimento recalculados a partir do inventário canônico; a taxa do grupo usa o membro mais lento."
    ): ResolutionOutcome {
        val game = envelope.campaignState.game
        return try {
            val derived = InventoryRuntimeRules.derivePartyExpedition(
                envelope.ruleProfile, envelope.optionSet, envelope.campaignState.party.characterIds,
                game.inventory, game.expedition
            )
            val updatedCombatants = game.encounter?.combatants?.mapValues { (_, c) ->
                val cid = c.characterId
                val rate = cid?.let(derived.perMember::get)
                if (rate == null) c else c.copy(encounterMoveMeters = rate.encounterMoveMeters)
            }
            val encounter = game.encounter?.let { e -> if (updatedCombatants == null) e else e.copy(combatants = updatedCombatants) }
            val updated = envelope.campaignState.copy(game = game.copy(expedition = derived.expedition, encounter = encounter))
            outcome(updated,message,"INVENTORY.CORE",envelope.ruleProfile,evidenceStatus,"GAME_SYSTEM_SPEC §§12.3,13,30",true,false)
        } catch (e: IllegalArgumentException) {
            outcome(envelope.campaignState,"Inventário recusado pelo firewall de perfil: ${e.message}","INVENTORY.CORE",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"Classic/Advanced inventory entries may not silently cross profiles.",false,false)
        }
    }

    private fun igniteItem(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if (parts.size !in 3..4) return outcome(envelope.campaignState,"Use LIGHT_IGNITE_ITEM:itemId:carrierId[:oilItemId].","LIGHT.SOURCE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§14,30",false,false)
        val itemId=parts[1].trim(); val carrier=parts[2].trim(); val fuel=parts.getOrNull(3)?.takeIf{it.isNotBlank()}
        if (carrier !in envelope.campaignState.party.characterIds) return outcome(envelope.campaignState,"Portador não pertence à companhia ativa.","LIGHT.SOURCE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §14 holder-bound light",false,false)
        val game=envelope.campaignState.game; val expedition=game.expedition
        val result=InventoryRuntimeRules.igniteCarriedLight(envelope.ruleProfile,game.inventory,expedition.lightSources,carrier,itemId,fuel)
        if(!result.success) return outcome(envelope.campaignState,"Fonte de luz não acesa: ${result.error}","LIGHT.SOURCE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§14,30",false,false)
        val light=requireNotNull(result.light)
        val lights=expedition.lightSources.filterNot{it.lightId==light.lightId || (it.sourceItemId==itemId && !it.lit && it.remainingTurns>0)}+light
        val updated=envelope.campaignState.copy(game=game.copy(inventory=result.inventory,expedition=expedition.copy(lightSources=lights)))
        val detail=if(light.kind==LightSourceKind.LANTERN)"1 frasco de óleo consumido; 24 turnos carregados." else "1 tocha consumida; 6 turnos carregados."
        return outcome(updated,"${light.kind} acesa por $carrier. $detail","LIGHT.DURATION",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §14 + equipment records",true,false)
    }

    private fun igniteOilPool(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if(parts.size!=3) return outcome(envelope.campaignState,"Use OIL_POOL_IGNITE:oilItemId:poolId.","LIGHT.SOURCE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"Equipment record: burning oil pool = 1 turn",false,false)
        val game=envelope.campaignState.game
        val result=InventoryRuntimeRules.igniteOilPool(envelope.ruleProfile,game.inventory,parts[1].trim(),parts[2].trim())
        if(!result.success) return outcome(envelope.campaignState,"Poça de óleo não criada: ${result.error}","LIGHT.SOURCE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"Equipment record: oil flask",false,false)
        val pool=requireNotNull(result.oilPool); val expedition=game.expedition
        val updated=envelope.campaignState.copy(game=game.copy(inventory=result.inventory,expedition=expedition.copy(oilPools=expedition.oilPools.filterNot{it.poolId==pool.poolId}+pool)))
        return outcome(updated,"Frasco de óleo consumido; poça $pool permanece acesa por 1 turno. Dano contextual continua a exigir o resolvedor aplicável.","LIGHT.DURATION",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"Equipment record: oil pool burns for 1 turn",true,false)
    }

    private fun declareSpell(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if(parts.size!=4) return outcome(envelope.campaignState,"Use COMBAT_DECLARE_SPELL:PARTY|OPPOSITION:actorId:memoryId.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §§23,29",false,false)
        val side=runCatching{EncounterSide.valueOf(parts[1].uppercase())}.getOrNull()
            ?: return outcome(envelope.campaignState,"Lado inválido.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §23",false,false)
        val actorId=parts[2].trim(); val memoryId=parts[3].trim()
        val game=envelope.campaignState.game; val encounter=game.encounter ?: return outcome(envelope.campaignState,"Nenhum encontro ativo.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §23",false,false)
        val round=encounter.combatRound ?: return outcome(envelope.campaignState,"Nenhuma rodada ativa.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §23",false,false)
        if(round.phase!=CombatActionPhase.DECLARATIONS) return outcome(envelope.campaignState,"Feitiço deve ser declarado antes da iniciativa.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §23.1",false,false)
        val actor=encounter.combatants[actorId]
        if(actor==null || actor.side!=side) return outcome(envelope.campaignState,"Combatente/lado inválido.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §23",false,false)
        val memory=MagicRuntimeRules.declaredMemory(game.magic,envelope.ruleProfile,actor.characterId ?: actorId,memoryId)
            ?: return outcome(envelope.campaignState,"Entrada memorizada não disponível para este combatente/perfil.","MAGIC.MEMORIZE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §29.2",false,false)
        val key=CombatRuntimeRules.key(side,actorId).value
        val nextRound=round.copy(
            declarations=round.declarations+(key to "SPELL"),
            declaredSpellMemoryIds=round.declaredSpellMemoryIds+(key to memory.memoryId),
            history=round.history+"DECLARE_SPELL $key=${memory.spellId}"
        )
        val updated=envelope.campaignState.copy(game=game.copy(encounter=encounter.copy(combatRound=nextRound)))
        return outcome(updated,"${memory.spellId} declarado por $actorId.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_PROCEDURE,"GAME_SYSTEM_SPEC §§23.1,29.3",true,false)
    }

    private fun combatMove(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if(parts.size!=4) return outcome(envelope.campaignState,"Use COMBAT_MOVE:PARTY|OPPOSITION:actorId:meters.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3",false,false)
        val side=runCatching{EncounterSide.valueOf(parts[1].uppercase())}.getOrNull()
            ?: return outcome(envelope.campaignState,"Lado inválido.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val actorId=parts[2].trim(); val meters=parts[3].toIntOrNull()
            ?: return outcome(envelope.campaignState,"Distância inválida.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3",false,false)
        if(meters<0) return outcome(envelope.campaignState,"Distância inválida.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3",false,false)
        val game=envelope.campaignState.game; val encounter=game.encounter ?: return outcome(envelope.campaignState,"Nenhum encontro ativo.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val round=encounter.combatRound ?: return outcome(envelope.campaignState,"Nenhuma rodada ativa.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        CombatRuntimeRules.validateAction(round,side,actorId,CombatActionPhase.MOVEMENT)?.let{return outcome(envelope.campaignState,it,"COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.1",false,false)}
        val actor=encounter.combatants[actorId]
        if(actor==null || actor.side!=side || !actor.alive) return outcome(envelope.campaignState,"Combatente inválido/inativo.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val key=CombatRuntimeRules.key(side,actorId).value
        if(envelope.ruleProfile==RuleProfile.OSE_ADVANCED_FANTASY && round.declarations[key]=="SPELL" && meters>0)
            return outcome(envelope.campaignState,"Advanced: conjurar é ação única; quem declarou feitiço não pode mover-se nesta rodada.","COMBAT.SPELL",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3 / Advanced casting",false,false)
        if(meters>actor.encounterMoveMeters) return outcome(envelope.campaignState,"Movimento excede a taxa canônica de ${actor.encounterMoveMeters}m.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§12.3,23.3",false,false)
        val nextRound=CombatRuntimeRules.markActionComplete(round.copy(movedActorKeys=(round.movedActorKeys+key).toSortedSet()),side,actorId)
        val updated=envelope.campaignState.copy(game=game.copy(encounter=encounter.copy(combatRound=nextRound)))
        return outcome(updated,"Movimento de $actorId registrado: ${meters}m.","COMBAT.MOVEMENT",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§12.3,23.3",true,false)
    }

    private fun combatAttack(envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams, phase: CombatActionPhase): ResolutionOutcome {
        val ruleId=if(phase==CombatActionPhase.MISSILE)"COMBAT.MISSILE" else "COMBAT.MELEE"
        if(parts.size!=5) return outcome(envelope.campaignState,"Use ${if(phase==CombatActionPhase.MISSILE)"COMBAT_ATTACK_MISSILE" else "COMBAT_ATTACK_MELEE"}:SIDE:actorId:targetId:itemId.",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23-25",false,false)
        val side=runCatching{EncounterSide.valueOf(parts[1].uppercase())}.getOrNull()
            ?: return outcome(envelope.campaignState,"Lado inválido.",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val actorId=parts[2].trim(); val targetId=parts[3].trim(); val itemId=parts[4].trim()
        var game=envelope.campaignState.game; val encounter=game.encounter ?: return outcome(envelope.campaignState,"Nenhum encontro ativo.",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val round=encounter.combatRound ?: return outcome(envelope.campaignState,"Nenhuma rodada ativa.",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        CombatRuntimeRules.validateAction(round,side,actorId,phase)?.let{return outcome(envelope.campaignState,it,ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.1",false,false)}
        val actor=encounter.combatants[actorId]; val target=encounter.combatants[targetId]; val item=game.inventory.items[itemId]
        if(actor==null || target==null || actor.side!=side || target.side==side || item==null) return outcome(envelope.campaignState,"Combatente/alvo/arma inválido.",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23-25",false,false)
        val ap=CombatRuntimeRules.attackProfile(envelope.ruleProfile,envelope.optionSet,actor,target,item,phase,encounter.distanceMeters)
        if(!ap.permitted) return outcome(envelope.campaignState,"Ataque recusado: ${ap.reason}",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23.3-23.4,25.1",false,false)
        val roll=authoritativeDie(rng,20)
        val ascending=OptionPolicyContract.enabled(envelope.ruleProfile,envelope.optionSet,OptionIds.ASCENDING_ARMOR_CLASS)
        val wildernessFatiguePenalty=if(actor.side==EncounterSide.PARTY && game.expedition.wildernessRestOverdue) -1 else 0
        val attack=if(ascending)
            SharedConfirmedRules.attack(envelope.ruleProfile,AttackInput(roll,AttackArmorMode.ASCENDING,19,actor.attackBonusAscending,target.armorClassAscending,ap.attackModifier+wildernessFatiguePenalty))
        else SharedConfirmedRules.attack(envelope.ruleProfile,AttackInput(roll,AttackArmorMode.DESCENDING,actor.thac0,0,target.armorClassDescending,ap.attackModifier+wildernessFatiguePenalty))
        var damage:Int?=null
        if(attack.hit) {
            val rolled=(1..ap.damageDiceCount).sumOf{authoritativeDie(rng,ap.damageDieSides)}
            damage=maxOf(1,rolled+ap.damageBonus+wildernessFatiguePenalty)
            game=CombatRuntimeRules.queueOrApplyDamage(game,actor,target,phase,damage,item.catalogId)
            if(!round.simultaneous) game=interruptDeclaredSpellIfEligible(game,target)
        }
        val currentRound=requireNotNull(requireNotNull(game.encounter).combatRound)
        val marked=CombatRuntimeRules.markActionComplete(currentRound,side,actorId)
        game=game.copy(encounter=requireNotNull(game.encounter).copy(combatRound=marked))
        val state=envelope.campaignState.copy(game=game)
        val suffix=if(attack.hit)"acerto; dano=$damage${if(wildernessFatiguePenalty<0)" (fadiga de ermos -1 ataque/dano)" else ""}${if(round.simultaneous)" armazenado para commit simultâneo" else " aplicado"}" else "erro${if(wildernessFatiguePenalty<0)" (fadiga de ermos -1 ataque)" else ""}"
        return outcome(state,"Ataque $ruleId de $actorId contra $targetId: d20=$roll, $suffix.",ruleId,envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23-25; authoritative RULES_DICE",true,true)
    }

    private fun combatSave(envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams): ResolutionOutcome {
        if(parts.size!=4) return outcome(envelope.campaignState,"Use COMBAT_SAVE:SIDE:actorId:saveKey.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC saves + §23.3 interruption",false,false)
        val side=runCatching{EncounterSide.valueOf(parts[1].uppercase())}.getOrNull()
            ?: return outcome(envelope.campaignState,"Lado inválido.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC saves",false,false)
        val actorId=parts[2].trim(); val saveKey=parts[3].uppercase(); var game=envelope.campaignState.game
        val encounter=game.encounter ?: return outcome(envelope.campaignState,"Nenhum encontro ativo.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val round=encounter.combatRound ?: return outcome(envelope.campaignState,"Nenhuma rodada ativa.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val actor=encounter.combatants[actorId]
        if(actor==null || actor.side!=side) return outcome(envelope.campaignState,"Combatente inválido.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC saves",false,false)
        val target=actor.savingThrowTargets[saveKey] ?: return outcome(envelope.campaignState,"Save '$saveKey' não está vinculado no estado canônico do combatente.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"Do not invent saving throw targets.",false,false)
        val roll=authoritativeDie(rng,20); val save=SharedConfirmedRules.savingThrow(envelope.ruleProfile,roll,target)
        if(!save.success && !round.simultaneous && round.activeSide!=side) game=interruptDeclaredSpellIfEligible(game,actor)
        val updated=envelope.campaignState.copy(game=game)
        return outcome(updated,"Save $saveKey de $actorId: d20=$roll vs $target => ${if(save.success)"sucesso" else "falha"}.","SAVE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC saving throw + §23.3",game!=envelope.campaignState.game,true)
    }

    private fun castDeclared(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if(parts.size!=3) return outcome(envelope.campaignState,"Use COMBAT_CAST_DECLARED:SIDE:actorId.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23.3,29",false,false)
        val side=runCatching{EncounterSide.valueOf(parts[1].uppercase())}.getOrNull()
            ?: return outcome(envelope.campaignState,"Lado inválido.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23,29",false,false)
        val actorId=parts[2].trim(); val game=envelope.campaignState.game; val encounter=game.encounter ?: return outcome(envelope.campaignState,"Nenhum encontro ativo.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        val round=encounter.combatRound ?: return outcome(envelope.campaignState,"Nenhuma rodada ativa.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23",false,false)
        CombatRuntimeRules.validateAction(round,side,actorId,CombatActionPhase.SPELL)?.let{return outcome(envelope.campaignState,it,"MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.1",false,false)}
        val actor=encounter.combatants[actorId]
        if(actor==null || actor.side!=side || !actor.alive) return outcome(envelope.campaignState,"Combatente inválido/inativo.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3",false,false)
        val key=CombatRuntimeRules.key(side,actorId).value; val memoryId=round.declaredSpellMemoryIds[key]
            ?: return outcome(envelope.campaignState,"Nenhum feitiço memorizado foi declarado para este combatente.","MAGIC.MEMORIZE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§23.1,29.2",false,false)
        val memory=MagicRuntimeRules.declaredMemory(game.magic,envelope.ruleProfile,actor.characterId ?: actorId,memoryId)
            ?: return outcome(envelope.campaignState,"Feitiço declarado já foi consumido/interrompido ou não pertence ao perfil.","MAGIC.MEMORIZE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §29.2",false,false)
        if(envelope.ruleProfile==RuleProfile.OSE_ADVANCED_FANTASY && actor.castingBlockedReason!=null)
            return outcome(envelope.campaignState,"Conjuração bloqueada no estado canônico: ${actor.castingBlockedReason}.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3 Advanced casting restrictions",false,false)
        if(envelope.ruleProfile==RuleProfile.OSE_ADVANCED_FANTASY && key in round.movedActorKeys)
            return outcome(envelope.campaignState,"Advanced: personagem que se moveu nesta rodada não pode conjurar.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §23.3",false,false)
        val magic=MagicRuntimeRules.consumeDeclared(game.magic,memoryId,interrupted=false)
        val nextRound=CombatRuntimeRules.markActionComplete(round.copy(history=round.history+"CAST $key=${memory.spellId}"),side,actorId)
        val updated=envelope.campaignState.copy(game=game.copy(magic=magic,encounter=encounter.copy(combatRound=nextRound)))
        return outcome(updated,"${memory.spellId} conjurado e removido da memória. Efeito específico só pode ser aplicado por resolvedor do feitiço com evidência própria.","MAGIC.CAST",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §29.2-29.6",true,false)
    }

    private fun recoverTreasure(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if (parts.size != 2) return outcome(envelope.campaignState,"Use TREASURE_RECOVER:treasureId.","TREASURE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§30.2,35.1",false,false)
        val id=parts[1].trim(); val game=envelope.campaignState.game
        val prior=game.experience.treasures[id]
            ?: return outcome(envelope.campaignState,"Tesouro não existe no estado canônico: $id","TREASURE.CORE",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"TREASURE_FOUND must originate from canonical world/content state.",false,false)
        if(prior.profile!=envelope.ruleProfile) return outcome(envelope.campaignState,"Tesouro pertence a outro perfil OSE.","TREASURE.CORE",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"Classic/Advanced treasure firewall.",false,false)
        val next=TreasureXpRuntimeRules.recoverTreasure(envelope.ruleProfile,game.experience,id)
            ?: return outcome(envelope.campaignState,"Tesouro não pôde ser recuperado.","TREASURE.CORE",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"No fallback across profiles.",false,false)
        if(next==game.experience) return outcome(envelope.campaignState,"Tesouro $id já estava RECOVERED; nenhuma mutação duplicada.","TREASURE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §35.1",false,false)
        return outcome(envelope.campaignState.copy(game=game.copy(experience=next)),"Tesouro $id mudou de FOUND para RECOVERED. Isso ainda não concede XP até XP_AWARD.","TREASURE.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§30.2,35.1",true,false)
    }

    private fun awardExperience(envelope: CampaignEnvelope, parts: List<String>): ResolutionOutcome {
        if(parts.size!=1) return outcome(envelope.campaignState,"Use XP_AWARD sem valores numéricos fornecidos pelo chamador.","XP.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §35.2; source XP comes from canonical treasure/monster state.",false,false)
        val synced=syncExperienceRecipients(envelope)
        if(synced.blocker!=null) return outcome(envelope.campaignState,"XP não distribuído: ${synced.blocker}","XP.PRIME_MOD",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"Character creation must freeze class-specific prime-requisite modifiers before XP is awarded.",false,false)
        val game=synced.state.game
        val planned=TreasureXpRuntimeRules.planAward(envelope.ruleProfile,game.experience)
        if(!planned.success) return outcome(envelope.campaignState,"XP não distribuído: ${planned.blocker}","XP.CORE",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"Equal division is canonical; remainder/unspecified fractional handling must not be invented.",false,false)
        val plan=requireNotNull(planned.plan)
        if(plan.totalXpBeforeIndividualModifiers==0L && plan.sourceEventIds.isEmpty())
            return outcome(envelope.campaignState,"Nenhuma fonte de XP recuperada/derrotada pendente.","XP.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §35.2",false,false)
        val nextGame=TreasureXpRuntimeRules.applyAward(game,plan)
        return outcome(synced.state.copy(game=nextGame),"XP distribuído igualmente entre sobreviventes/auxiliares elegíveis e depois ajustado por requisito primário; award=${plan.awardId}.","XP.CORE",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §35.2 and Advanced multiclass §37",true,false)
    }

    private fun levelUp(envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams): ResolutionOutcome {
        if(parts.size!=3) return outcome(envelope.campaignState,"Use XP_LEVEL_UP:recipientId:classId. Nenhum dado, limiar ou nível pode ser fornecido pelo chamador.","XP.LEVEL_UP",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§35-37; authoritative progression rows and RULES_DICE.",false,false)
        val recipientId=parts[1].trim(); val classId=parts[2].trim()
        if(recipientId.isBlank() || classId.isBlank()) return outcome(envelope.campaignState,"Identidade de recipiente/classe inválida.","XP.LEVEL_UP",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"No fuzzy/name fallback.",false,false)
        val synced=syncExperienceRecipients(envelope)
        if(synced.blocker!=null) return outcome(envelope.campaignState,"Level-up não aplicado: ${synced.blocker}","XP.PRIME_MOD",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"Character creation must freeze class-specific XP state before progression.",false,false)
        val game=synced.state.game
        val result=ClassProgressionRuntimeRules.planLevelUp(envelope.ruleProfile,game.experience,recipientId,classId){ sides -> authoritativeDie(rng,sides) }
        if(!result.success){
            val blocker=result.blocker ?: "LEVEL_UP_NOT_AVAILABLE"
            val evidenceGap=blocker.startsWith("MISSING_EVIDENCE") || blocker.startsWith("CLASS_PROGRESSION_NOT_BOUND") || blocker.startsWith("ADV_POST_MAX_NOT_AUTHORIZED")
            return outcome(envelope.campaignState,"Level-up não aplicado: $blocker","XP.LEVEL_UP",envelope.ruleProfile,if(evidenceGap) EvidenceStatus.MISSING_EVIDENCE else EvidenceStatus.CANONICAL_RULE,"Exact profile-bound class progression; one-level-per-session; no undocumented Advanced post-max progression.",false,false)
        }
        val plan=requireNotNull(result.plan)
        val nextExperience=ClassProgressionRuntimeRules.applyLevelUp(game.experience,plan)
        val committed=commitLevelUpToCharacter(envelope.ruleProfile,game,nextExperience,plan)
        if(committed.blocker!=null) return outcome(envelope.campaignState,"Level-up não aplicado: ${committed.blocker}","XP.LEVEL_UP",envelope.ruleProfile,EvidenceStatus.MISSING_EVIDENCE,"CharacterSnapshot commit requires all owned class progression rows; no partial ledger-only level-up.",false,false)
        val hp="${plan.hpGain.numerator}/${plan.hpGain.denominator}"
        val nextState=synced.state.copy(game=requireNotNull(committed.game))
        return outcome(nextState,"${plan.classId}: nível ${plan.fromLevel}→${plan.toLevel}; PV máximo +$hp; THAC0/saves/ficha atualizados pela tabela ${plan.row.sourceId}.","XP.LEVEL_UP",envelope.ruleProfile,EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §§35-37; exact class table row; HP gain updates maximum HP only; RULES_DICE used when a new HD is gained.",true,plan.hpGain.dieRolled!=null)
    }

    private data class ExperienceSyncResult(val state: CampaignState, val blocker: String? = null)
    private data class CharacterLevelCommitResult(val game: GameExtensions? = null, val blocker: String? = null)

    private fun syncExperienceRecipients(envelope: CampaignEnvelope): ExperienceSyncResult {
        val state=envelope.campaignState
        val game=state.game
        var experience=game.experience
        val recipients=experience.recipients.toMutableMap()
        for(characterId in state.party.characterIds) {
            if(characterId in recipients) continue
            val character=game.characters[characterId] ?: continue
            val missingPrime=character.classIds.firstOrNull { it !in character.primeModifierPercentByClass }
            if(missingPrime!=null) return ExperienceSyncResult(state,"MISSING_EVIDENCE:PRIME_MODIFIER:$characterId:$missingPrime")
            val progression=linkedMapOf<String,AppliedClassProgressionSnapshot>()
            for(classId in character.classIds) {
                val level=character.levelByClass[classId] ?: return ExperienceSyncResult(state,"MISSING_EVIDENCE:CHARACTER_LEVEL:$characterId:$classId")
                val row=ClassProgressionRuntimeRules.table(envelope.ruleProfile,classId)?.row(level)
                    ?: return ExperienceSyncResult(state,"CLASS_PROGRESSION_NOT_BOUND:${envelope.ruleProfile}:$classId:$level")
                progression[classId]=AppliedClassProgressionSnapshot(
                    classId,row.level,row.xpThreshold,row.hitDiceExpression,row.thac0,row.attackBonusAscending,row.saves,row.spellSlots,row.sourceId
                )
            }
            recipients[characterId]=XpRecipientSnapshot(
                recipientId=characterId,
                profile=envelope.ruleProfile,
                classIds=character.classIds,
                primeModifierPercentByClass=character.primeModifierPercentByClass,
                xpByClass=character.xpByClass,
                levelByClass=character.levelByClass,
                constitutionModifier=SharedConfirmedRules.attributeModifier(character.attributes.con),
                progressionByClass=progression
            )
        }
        experience=experience.copy(
            recipients=recipients,
            activeSessionId=experience.activeSessionId ?: "SESSION:${envelope.campaignId.value}:1"
        ).canonical()
        return ExperienceSyncResult(state.copy(game=game.copy(experience=experience)))
    }

    private fun commitLevelUpToCharacter(
        profile: RuleProfile,
        originalGame: GameExtensions,
        leveledExperience: ExperienceRuntimeSnapshot,
        plan: LevelUpPlanRuntime
    ): CharacterLevelCommitResult {
        val character=originalGame.characters[plan.recipientId]
            ?: return CharacterLevelCommitResult(originalGame.copy(experience=leveledExperience))
        val recipient=leveledExperience.recipients[plan.recipientId]
            ?: return CharacterLevelCommitResult(blocker="RECIPIENT_NOT_FOUND_AFTER_LEVEL:${plan.recipientId}")

        val rows=mutableListOf<ClassProgressionRowRuntime>()
        for(classId in character.classIds) {
            val level=recipient.levelByClass[classId] ?: return CharacterLevelCommitResult(blocker="MISSING_EVIDENCE:LEVEL:$classId")
            val row=ClassProgressionRuntimeRules.table(profile,classId)?.row(level)
                ?: return CharacterLevelCommitResult(blocker="MISSING_EVIDENCE:PROGRESSION_ROW:$classId:$level")
            rows += row
        }
        val saves=SavingThrows(
            deathPoison=rows.minOf { it.saves.getValue("DEATH_POISON") },
            wands=rows.minOf { it.saves.getValue("WANDS") },
            paralysisPetrification=rows.minOf { it.saves.getValue("PARALYSIS_PETRIFICATION") },
            breath=rows.minOf { it.saves.getValue("BREATH") },
            spellsRodsStaves=rows.minOf { it.saves.getValue("SPELLS_RODS_STAVES") }
        )
        val hp=addToMaximumHp(character.hitPoints,plan.hpGain.numerator,plan.hpGain.denominator)
        val updatedCharacter=character.copy(
            levelByClass=recipient.levelByClass,
            xpByClass=recipient.xpByClass,
            hitPoints=hp,
            thac0=rows.minOf { it.thac0 },
            attackBonusAscending=rows.maxOf { it.attackBonusAscending },
            savingThrows=saves,
            evidenceRefs=(character.evidenceRefs+"LEVEL.UP|${profile.name}|GAME_SYSTEM_SPEC.md|§36.1").distinct()
        ).canonical()
        val clearedRecipient=recipient.copy(pendingHpGainNumerator=0,pendingHpGainDenominator=1)
        val experience=leveledExperience.copy(
            recipients=leveledExperience.recipients+(plan.recipientId to clearedRecipient)
        ).canonical()
        return CharacterLevelCommitResult(
            originalGame.copy(
                characters=originalGame.characters+(plan.recipientId to updatedCharacter),
                experience=experience
            )
        )
    }

    private fun addToMaximumHp(hp: HitPoints, gainNumerator: Int, gainDenominator: Int): HitPoints {
        require(gainNumerator>=0 && gainDenominator>0)
        val den=lcm(hp.denominator,gainDenominator)
        val current=hp.currentNumerator*(den/hp.denominator)
        val maximum=hp.maxNumerator*(den/hp.denominator)+gainNumerator*(den/gainDenominator)
        val g=gcd(gcd(kotlin.math.abs(current),kotlin.math.abs(maximum)),den)
        return HitPoints(current/g,maximum/g,den/g)
    }
    private fun gcd(a0:Int,b0:Int):Int { var a=a0; var b=b0; while(b!=0){ val t=a%b; a=b; b=t }; return if(a==0)1 else kotlin.math.abs(a) }
    private fun lcm(a:Int,b:Int):Int = a/gcd(a,b)*b

    private fun interruptDeclaredSpellIfEligible(game: GameExtensions, target: CombatantSnapshot): GameExtensions {
        val encounter=game.encounter ?: return game; val round=encounter.combatRound ?: return game
        val key=CombatRuntimeRules.key(target.side,target.actorId).value
        if(round.declarations[key]!="SPELL") return game
        val memoryId=round.declaredSpellMemoryIds[key] ?: return game
        val memory=game.magic.memorized[memoryId] ?: return game
        if(memory.consumed) return game
        val magic=MagicRuntimeRules.consumeDeclared(game.magic,memoryId,interrupted=true)
        return game.copy(magic=magic,encounter=encounter.copy(combatRound=round.copy(history=round.history+"SPELL_INTERRUPTED $key=${memory.spellId}")))
    }

    fun commitSimultaneousAtRoundEnd(state: CampaignState): CampaignState {
        val game=state.game; val round=game.encounter?.combatRound ?: return state
        if(!round.simultaneous || round.pendingEffects.isEmpty()) return state
        return state.copy(game=CombatRuntimeRules.commitSimultaneousEffects(game))
    }

    fun advanceMagicRounds(state: CampaignState, rounds: Int): CampaignState =
        if(rounds<=0) state else state.copy(game=state.game.copy(magic=MagicRuntimeRules.advanceActiveEffectsByRounds(state.game.magic,rounds)))

    fun advanceMagicTurns(state: CampaignState, turns: Int): CampaignState =
        if(turns<=0) state else state.copy(game=state.game.copy(magic=MagicRuntimeRules.advanceActiveEffectsByTurns(state.game.magic,turns)))


    /**
     * PLAYER_REACTION-bound hiring: offer wage and treasure half-shares are
     * negotiated values, never secretly generated by Gemini or inferred from text.
     * A recruit's class/level must be explicitly bound before adventurer hiring.
     * Ref: GAME_SYSTEM_SPEC §§5,31; Classic Rules Tome pp.56–57.
     */
    private fun recruitRetainer(
        envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams
    ): ResolutionOutcome {
        val profile = envelope.ruleProfile
        fun blocked(reason:String, missing:Boolean=false) = outcome(
            envelope.campaignState,reason,"RETAINER.HIRE",profile,
            if(missing) EvidenceStatus.MISSING_EVIDENCE else EvidenceStatus.CANONICAL_PROCEDURE,
            "GAME_SYSTEM_SPEC §31.1; Classic pp.56–57; Advanced retainers",false,false
        )
        if(parts.size!=5) return blocked("Use RETAINER_RECRUIT:employerId:npcId:dailyWageGp:treasureHalfShares.")
        val employerId=parts[1].trim(); val npcId=parts[2].trim()
        val wage=parts[3].toIntOrNull() ?: return blocked("Oferta de salário inválida.")
        val halves=parts[4].toIntOrNull() ?: return blocked("Participação em tesouro inválida.")
        if(wage<0 || halves<1 || halves>100) return blocked("Salário não negativo e ao menos meia parte de tesouro são obrigatórios.")
        val state=envelope.campaignState
        val employer=state.game.characters[employerId]
            ?.takeIf { employerId in state.party.characterIds }
            ?: return blocked("Contratante deve pertencer à companhia atual.")
        val npc=state.game.npcs[npcId] ?: return blocked("Candidato não está no mundo conhecido.")
        if(npc.spatialRef != null && npc.spatialRef != state.position.primary)
            return blocked("Candidato não está na posição atual da companhia.")
        val level=npc.retainerLevel ?: return blocked("MISSING_EVIDENCE:NPC_RETAINER_LEVEL:$npcId",true)
        val employerLevel=employer.levelByClass.values.maxOrNull()
            ?: return blocked("MISSING_EVIDENCE:EMPLOYER_LEVEL:$employerId",true)
        if(level>employerLevel) return blocked("Auxiliar não pode superar o nível do contratante.")
        if(level>0) {
            val cls=npc.retainerClassId ?: return blocked("MISSING_EVIDENCE:NPC_RETAINER_CLASS:$npcId",true)
            if(ClassProgressionRuntimeRules.table(profile,cls)?.row(level)==null)
                return blocked("MISSING_EVIDENCE:NPC_RETAINER_CLASS_LEVEL:$cls:$level",true)
        }
        if(state.game.retainerContracts[npcId]?.active==true)
            return blocked("Candidato já está contratado.")
        val cha=RetainerMercenaryRules.charisma(employer.attributes.cha)
        val already=state.game.retainerContracts.values.count {
            it.active && it.employerCharacterId==employerId
        }
        if(already>=cha.maxRetainers) return blocked("Limite de auxiliares do CAR do contratante atingido.")
        val region=npc.spatialRef?.spatialEntityId ?: npc.locationRef
        val reputationPenalty=if(region in state.game.retainerOffendedLocations) -1 else 0
        val d1=authoritativeDie(rng,6);val d2=authoritativeDie(rng,6)
        val result=RetainerMercenaryRules.recruitment(d1,d2,cha.hiringReactionModifier,reputationPenalty)
        val gained=if(result.accepted) RetainerContractSnapshot(
            npcId=npcId, employerCharacterId=employerId, dailyWageGp=wage,
            treasureHalfShares=halves,
            loyalty=(cha.baseLoyalty+result.loyaltyBonus).coerceIn(2,12),
            hiredAtTurn=state.time.turns
        ) else null
        val nextNpc=npc.copy(
            reactionHistory=npc.reactionHistory+"RETAINER_RECRUIT:${result.outcome}:${result.effectiveTotal}",
            consequenceFlags=if(result.accepted) npc.consequenceFlags+"RETAINER_ACTIVE" else npc.consequenceFlags
        ).canonical()
        val game=state.game
        val nextGame=game.copy(
            npcs=game.npcs+(npcId to nextNpc),
            retainerContracts=if(gained!=null) game.retainerContracts+(npcId to gained) else game.retainerContracts,
            retainerOffendedLocations=if(result.outcome==RetainerMercenaryRules.RecruitmentOutcome.OFFENDED)
                game.retainerOffendedLocations+region else game.retainerOffendedLocations,
            actionLog=game.actionLog+ActionLogEntry(
                "RETAINER_OFFER:${game.revision}:$npcId", "RETAINER",
                "Oferta $employerId->$npcId: ${result.outcome}, 2d6=${result.rawTwoDice}, total=${result.effectiveTotal}",
                listOf("RETAINER.HIRE|GAME_SYSTEM_SPEC §31.1")
            ),
            revision=game.revision+1
        ).canonical()
        return outcome(state.copy(game=nextGame),
            "Recrutamento: ${result.outcome}; 2d6=${result.rawTwoDice}; total=${result.effectiveTotal}. "+
                if(result.accepted) "Auxiliar contratado; lealdade ${gained!!.loyalty}." else "Nenhum contrato novo.",
            "RETAINER.HIRE",profile,EvidenceStatus.CANONICAL_PROCEDURE,
            "Classic Rules Tome pp.56–57; GAME_SYSTEM_SPEC §31.1",true,true)
    }

    private fun testRetainerLoyalty(
        envelope: CampaignEnvelope, parts: List<String>, rng: NamedRngStreams
    ): ResolutionOutcome {
        val profile=envelope.ruleProfile
        fun blocked(reason:String)=outcome(
            envelope.campaignState,reason,"RETAINER.LOYALTY",profile,
            EvidenceStatus.CANONICAL_RULE,"GAME_SYSTEM_SPEC §31.1; Classic p.57",false,false
        )
        if(parts.size!=3) return blocked("Use RETAINER_LOYALTY:npcId:DANGER|AFTER_ADVENTURE.")
        val npcId=parts[1].trim();val reason=parts[2].uppercase()
        if(reason!="DANGER" && reason!="AFTER_ADVENTURE") return blocked("Momento de lealdade inválido.")
        val state=envelope.campaignState;val game=state.game
        val contract=game.retainerContracts[npcId]?.takeIf { it.active }
            ?: return blocked("Auxiliar não está contratado.")
        val d1=authoritativeDie(rng,6);val d2=authoritativeDie(rng,6)
        val result=RetainerMercenaryRules.loyalty(d1,d2,contract.loyalty)
        // Danger failure usually means flight but is subject to referee judgment.
        // After-adventure failure is an explicit end of employment in the source.
        val departed=!result.holds && reason=="AFTER_ADVENTURE"
        val updated=contract.copy(
            active=!departed,
            failedLoyaltyChecks=contract.failedLoyaltyChecks+if(result.holds) 0 else 1,
            lastLoyaltyCheckTurn=state.time.turns,
            lastLoyaltyCheckReason=reason
        )
        val npc=game.npcs[npcId]
        val updateNpc=npc?.copy(
            reactionHistory=npc.reactionHistory+"RETAINER_LOYALTY:$reason:${result.diceTotal}:${result.holds}",
            consequenceFlags=if(departed) (npc.consequenceFlags-"RETAINER_ACTIVE")+"RETAINER_DEPARTED" else npc.consequenceFlags
        )?.canonical()
        val next=game.copy(
            retainerContracts=game.retainerContracts+(npcId to updated),
            npcs=if(updateNpc==null) game.npcs else game.npcs+(npcId to updateNpc),
            actionLog=game.actionLog+ActionLogEntry(
                "RETAINER_LOYALTY:${game.revision}:$npcId","RETAINER",
                "2d6=${result.diceTotal} vs lealdade=${result.threshold}; causa=$reason; permanece=${result.holds}",
                listOf("RETAINER.LOYALTY|GAME_SYSTEM_SPEC §31.1")
            ),
            revision=game.revision+1
        ).canonical()
        return outcome(state.copy(game=next),
            "Teste de lealdade 2d6=${result.diceTotal}, alvo=${result.threshold}: "+
                if(result.holds) "auxiliar permanece." else if(departed) "auxiliar encerra o contrato." else
                    "falhou sob perigo; fuga depende do árbitro.",
            "RETAINER.LOYALTY",profile,EvidenceStatus.CANONICAL_RULE,
            "Classic Rules Tome p.57; GAME_SYSTEM_SPEC §31.1",true,true)
    }

    private fun authoritativeDie(rng: NamedRngStreams, sides: Int): Int {
        require(sides>=1)
        return (rng.draw(RngStreamId.RULES_DICE)%sides.toUInt()).toInt()+1
    }

    private fun outcome(state: CampaignState,message:String,ruleId:String,profile:RuleProfile,status:EvidenceStatus,note:String,mutation:Boolean,rng:Boolean)=
        ResolutionOutcome(state,message,RuleTrace(status,listOf(RuleEvidence(ruleId,profile,"GAME_SYSTEM_SPEC.md",note)),note),mutation,rng)
}
