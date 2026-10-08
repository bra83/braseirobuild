package braseiro.ose.rules.shared

import braseiro.ose.model.*
import braseiro.ose.rules.api.OptionIds
import braseiro.ose.rules.api.OptionPolicyContract

@JvmInline
value class CombatActionKey(val value: String)

data class PartyLoadDerivation(
    val expedition: ExpeditionSnapshot,
    val perMember: Map<String, MovementRate>
)

data class InventoryLightMutation(
    val inventory: InventorySnapshot,
    val light: LightSourceSnapshot? = null,
    val oilPool: OilPoolSnapshot? = null,
    val error: String? = null
) {
    val success: Boolean get() = error == null
}

data class AttackRuntimeResult(
    val permitted: Boolean,
    val attackModifier: Int = 0,
    val damageDiceCount: Int = 0,
    val damageDieSides: Int = 6,
    val damageBonus: Int = 0,
    val reason: String? = null
)

object InventoryRuntimeRules {
    private fun ensureProfile(profile: RuleProfile, inventory: InventorySnapshot): String? {
        val mismatch = inventory.items.values.firstOrNull { it.quantity > 0 && it.profile != profile }
        return mismatch?.let { "PROFILE_MISMATCH:${it.itemId}:${it.profile}:$profile" }
    }

    fun derivePartyExpedition(
        profile: RuleProfile,
        optionSet: OptionSet,
        partyCharacterIds: List<String>,
        inventory: InventorySnapshot,
        prior: ExpeditionSnapshot
    ): PartyLoadDerivation {
        ensureProfile(profile, inventory)?.let { throw IllegalArgumentException(it) }
        val ids = partyCharacterIds.distinct()
        if (ids.isEmpty()) {
            val mode = MovementLightRules.encumbranceMode(profile, optionSet)
            return PartyLoadDerivation(
                prior.copy(encumbranceMode = mode, baseMoveMeters = 0, encounterMoveMeters = 0, immobile = true),
                emptyMap()
            )
        }
        val perMember = linkedMapOf<String, MovementRate>()
        var totalTreasure = 0
        var totalEquipment = 0
        var groupArmor = ArmorLoadCategory.NONE
        for (owner in ids) {
            val carried = inventory.items.values.filter { it.ownerId == owner && it.carried && it.quantity > 0 }
            val treasure = carried.filter { it.kind in treasureKinds }.sumOf { it.unitWeightCoins * it.quantity }
            val directlyWeighed = carried.filter { it.kind !in treasureKinds && it.kind !in collectiveAdventuringKinds }
                .sumOf { it.unitWeightCoins * it.quantity }
            // Both audited profiles treat miscellaneous adventuring equipment as one collective 80-coin load in DETAILED mode.
            val collectiveAdventuring = if (carried.any { it.kind in collectiveAdventuringKinds }) 80 else 0
            val equipment = directlyWeighed + collectiveAdventuring
            val armor = carried.asSequence()
                .filter { it.equipped && it.kind == InventoryItemKind.ARMOR }
                .mapNotNull { it.armorLoadCategory }
                .maxByOrNull(::armorRank) ?: ArmorLoadCategory.NONE
            val significant = owner in inventory.basicSignificantTreasureOwners
            val rate = MovementLightRules.movementRate(profile, optionSet, armor, treasure, equipment, significant)
            perMember[owner] = rate
            totalTreasure += treasure
            totalEquipment += equipment
            if (armorRank(armor) > armorRank(groupArmor)) groupArmor = armor
        }
        val slowestBase = perMember.values.minOf { it.baseMoveMeters }
        val slowestEncounter = perMember.values.minOf { it.encounterMoveMeters }
        val groupImmobile = perMember.values.any { it.immobile }
        val mode = perMember.values.first().encumbranceMode
        return PartyLoadDerivation(
            prior.copy(
                armorLoad = groupArmor,
                treasureWeightCoins = totalTreasure,
                equipmentWeightCoins = totalEquipment,
                significantTreasure = inventory.basicSignificantTreasureOwners.any { it in ids },
                encumbranceMode = mode,
                baseMoveMeters = slowestBase,
                encounterMoveMeters = slowestEncounter,
                immobile = groupImmobile
            ),
            perMember
        )
    }

    fun setBasicSignificantTreasure(inventory: InventorySnapshot, ownerId: String, significant: Boolean): InventorySnapshot {
        require(ownerId.isNotBlank())
        val next = inventory.basicSignificantTreasureOwners.toMutableSet()
        if (significant) next += ownerId else next -= ownerId
        return inventory.copy(basicSignificantTreasureOwners = next).canonical()
    }

    fun igniteCarriedLight(
        profile: RuleProfile,
        inventory: InventorySnapshot,
        existingLights: List<LightSourceSnapshot>,
        carrierId: String,
        sourceItemId: String,
        fuelItemId: String? = null
    ): InventoryLightMutation {
        ensureProfile(profile, inventory)?.let { return InventoryLightMutation(inventory, error = it) }
        val source = inventory.items[sourceItemId]
            ?: return InventoryLightMutation(inventory, error = "LIGHT_SOURCE_NOT_FOUND:$sourceItemId")
        if (source.ownerId != carrierId || !source.carried || source.quantity <= 0)
            return InventoryLightMutation(inventory, error = "LIGHT_SOURCE_NOT_CARRIED_BY:$carrierId")
        val kind = source.lightSourceKind
            ?: return InventoryLightMutation(inventory, error = "ITEM_NOT_LIGHT_SOURCE:$sourceItemId")
        val prior = existingLights.lastOrNull { it.sourceItemId == sourceItemId }
        if (prior != null && prior.lit && prior.remainingTurns > 0)
            return InventoryLightMutation(inventory, error = "LIGHT_ALREADY_LIT:$sourceItemId")
        if (kind == LightSourceKind.LANTERN && prior != null && !prior.lit && prior.remainingTurns > 0) {
            return InventoryLightMutation(
                inventory,
                light = prior.copy(lit = true, carrierId = carrierId)
            )
        }
        val nextItems = inventory.items.toMutableMap()
        var usedFuel: String? = null
        when (kind) {
            LightSourceKind.TORCH -> {
                nextItems[sourceItemId] = source.copy(quantity = source.quantity - 1)
            }
            LightSourceKind.LANTERN -> {
                val fuel = fuelItemId?.let(inventory.items::get)
                    ?: return InventoryLightMutation(inventory, error = "LANTERN_REQUIRES_OIL_FLASK")
                if (fuel.ownerId != carrierId || fuel.kind != InventoryItemKind.OIL_FLASK || !fuel.carried || fuel.quantity <= 0)
                    return InventoryLightMutation(inventory, error = "VALID_CARRIED_OIL_FLASK_REQUIRED")
                nextItems[fuel.itemId] = fuel.copy(quantity = fuel.quantity - 1)
                usedFuel = fuel.itemId
            }
        }
        val sequence = existingLights.count { it.sourceItemId == sourceItemId } + 1
        val light = LightSourceSnapshot(
            lightId = "$sourceItemId#$sequence",
            kind = kind,
            carrierId = carrierId,
            remainingTurns = MovementLightRules.defaultLightDurationTurns(kind),
            lit = true,
            sourceItemId = sourceItemId,
            fuelItemId = usedFuel
        )
        return InventoryLightMutation(inventory.copy(items = nextItems).canonical(), light = light)
    }

    fun igniteOilPool(
        profile: RuleProfile,
        inventory: InventorySnapshot,
        oilItemId: String,
        poolId: String
    ): InventoryLightMutation {
        ensureProfile(profile, inventory)?.let { return InventoryLightMutation(inventory, error = it) }
        val oil = inventory.items[oilItemId]
            ?: return InventoryLightMutation(inventory, error = "OIL_NOT_FOUND:$oilItemId")
        if (oil.kind != InventoryItemKind.OIL_FLASK || !oil.carried || oil.quantity <= 0)
            return InventoryLightMutation(inventory, error = "CARRIED_OIL_FLASK_REQUIRED")
        val items = inventory.items.toMutableMap()
        items[oilItemId] = oil.copy(quantity = oil.quantity - 1)
        return InventoryLightMutation(
            inventory.copy(items = items).canonical(),
            oilPool = OilPoolSnapshot(poolId = poolId, sourceItemId = oilItemId, remainingTurns = 1, active = true)
        )
    }

    private val collectiveAdventuringKinds = setOf(
        InventoryItemKind.ADVENTURING_GEAR, InventoryItemKind.FOOD, InventoryItemKind.LIGHT_SOURCE, InventoryItemKind.OIL_FLASK
    )

    private val treasureKinds = setOf(
        InventoryItemKind.COIN, InventoryItemKind.GEM, InventoryItemKind.JEWELRY,
        InventoryItemKind.SCROLL, InventoryItemKind.POTION, InventoryItemKind.WAND,
        InventoryItemKind.ROD, InventoryItemKind.STAFF
    )
    private fun armorRank(a: ArmorLoadCategory) = when (a) {
        ArmorLoadCategory.NONE -> 0
        ArmorLoadCategory.LIGHT -> 1
        ArmorLoadCategory.HEAVY -> 2
    }
}

object CombatRuntimeRules {
    fun key(side: EncounterSide, actorId: String) = CombatActionKey("${side.name}:$actorId")
    fun completionKey(side: EncounterSide, actorId: String, phase: CombatActionPhase) = "${side.name}:$actorId:${phase.name}"

    fun validateAction(round: CombatRoundSnapshot, side: EncounterSide, actorId: String, phase: CombatActionPhase): String? {
        if (round.phase != phase) return "WRONG_COMBAT_PHASE:${round.phase}:$phase"
        if (round.activeSide != side) return "WRONG_ACTIVE_SIDE:${round.activeSide}:$side"
        val completed = completionKey(side, actorId, phase)
        if (completed in round.completedActionKeys) return "ACTION_ALREADY_COMPLETED:$completed"
        return null
    }

    fun attackProfile(
        profile: RuleProfile,
        optionSet: OptionSet,
        actor: CombatantSnapshot,
        target: CombatantSnapshot,
        weaponItem: InventoryItemSnapshot,
        phase: CombatActionPhase,
        encounterDistanceMeters: Int?
    ): AttackRuntimeResult {
        if (!actor.alive || actor.hpCurrentNumerator <= 0) return AttackRuntimeResult(false, reason = "ACTOR_DEAD")
        if (!target.alive || target.hpCurrentNumerator <= 0) return AttackRuntimeResult(false, reason = "TARGET_DEAD")
        val ownerRef = actor.characterId ?: actor.actorId
        if (weaponItem.ownerId != ownerRef || !weaponItem.carried || weaponItem.quantity <= 0)
            return AttackRuntimeResult(false, reason = "WEAPON_NOT_CARRIED_BY_ACTOR")
        if (weaponItem.profile != profile) return AttackRuntimeResult(false, reason = "WEAPON_PROFILE_MISMATCH")
        val weapon = weaponItem.weapon ?: return AttackRuntimeResult(false, reason = "ITEM_NOT_WEAPON")
        val attackModifier: Int
        when (phase) {
            CombatActionPhase.MELEE -> {
                if (!weapon.melee) return AttackRuntimeResult(false, reason = "WEAPON_NOT_MELEE")
                if (encounterDistanceMeters == null || encounterDistanceMeters > 1)
                    return AttackRuntimeResult(false, reason = "MELEE_DISTANCE_NOT_PROVEN_WITHIN_1_5M")
                attackModifier = actor.meleeAttackModifier
            }
            CombatActionPhase.MISSILE -> {
                if (!weapon.missile) return AttackRuntimeResult(false, reason = "WEAPON_NOT_MISSILE")
                val d = encounterDistanceMeters ?: return AttackRuntimeResult(false, reason = "MISSILE_DISTANCE_UNKNOWN")
                if (d <= 1) return AttackRuntimeResult(false, reason = "MISSILE_TARGET_IN_MELEE_DISTANCE")
                val short = weapon.shortRangeMeters!!
                val medium = weapon.mediumRangeMeters!!
                val long = weapon.longRangeMeters!!
                attackModifier = when {
                    d <= short -> actor.missileAttackModifier + 1
                    d <= medium -> actor.missileAttackModifier
                    d <= long -> actor.missileAttackModifier - 1
                    else -> return AttackRuntimeResult(false, reason = "BEYOND_LONG_RANGE")
                }
            }
            else -> return AttackRuntimeResult(false, reason = "ATTACK_NOT_ALLOWED_IN_PHASE:$phase")
        }
        val variable = OptionPolicyContract.enabled(profile, optionSet, OptionIds.VARIABLE_WEAPON_DAMAGE)
        val partyStandard = actor.side == EncounterSide.PARTY && !variable
        val diceCount = if (partyStandard) 1 else weapon.damageDiceCount
        val dieSides = if (partyStandard) 6 else weapon.damageDieSides
        val baseBonus = if (partyStandard) 0 else weapon.damageBonus
        val meleeBonus = if (phase == CombatActionPhase.MELEE) actor.meleeDamageModifier else 0
        return AttackRuntimeResult(true, attackModifier, diceCount, dieSides, baseBonus + meleeBonus)
    }

    fun queueOrApplyDamage(
        game: GameExtensions,
        source: CombatantSnapshot,
        target: CombatantSnapshot,
        phase: CombatActionPhase,
        damageNumerator: Int,
        causeId: String
    ): GameExtensions {
        require(damageNumerator >= 1)
        val encounter = requireNotNull(game.encounter)
        val round = requireNotNull(encounter.combatRound)
        val effect = PendingCombatEffectSnapshot(
            effectId = "r${round.roundNumber}:${phase.name}:${source.actorId}:${target.actorId}:${round.pendingEffects.size}",
            sourceActorId = source.actorId,
            targetActorId = target.actorId,
            sourceSide = source.side,
            phase = phase,
            hpDeltaNumerator = -damageNumerator,
            causeId = causeId
        )
        return if (round.simultaneous) {
            game.copy(encounter = encounter.copy(combatRound = round.copy(pendingEffects = round.pendingEffects + effect)))
        } else {
            applyEffects(game, listOf(effect))
        }
    }

    fun commitSimultaneousEffects(game: GameExtensions): GameExtensions {
        val encounter = requireNotNull(game.encounter)
        val round = requireNotNull(encounter.combatRound)
        if (!round.simultaneous || round.pendingEffects.isEmpty()) return game
        val committed = applyEffects(game, round.pendingEffects)
        val committedEncounter = requireNotNull(committed.encounter)
        val committedRound = requireNotNull(committedEncounter.combatRound)
        return committed.copy(encounter = committedEncounter.copy(combatRound = committedRound.copy(
            pendingEffects = emptyList(),
            history = committedRound.history + "SIMULTANEOUS_EFFECTS_COMMITTED"
        )))
    }

    fun markActionComplete(round: CombatRoundSnapshot, side: EncounterSide, actorId: String, phase: CombatActionPhase = round.phase): CombatRoundSnapshot {
        val k = completionKey(side, actorId, phase)
        return round.copy(completedActionKeys = (round.completedActionKeys + k).toSortedSet())
    }

    private fun applyEffects(game: GameExtensions, effects: List<PendingCombatEffectSnapshot>): GameExtensions {
        var combatants = requireNotNull(game.encounter).combatants.toMutableMap()
        var characters = game.characters.toMutableMap()
        for (effect in effects) {
            val old = combatants[effect.targetActorId] ?: continue
            val hp = old.hpCurrentNumerator + effect.hpDeltaNumerator
            val alive = hp > 0
            val updated = old.copy(hpCurrentNumerator = hp, alive = alive)
            combatants[old.actorId] = updated
            old.characterId?.let { cid ->
                characters[cid]?.let { c ->
                    characters[cid] = c.copy(hitPoints = c.hitPoints.copy(currentNumerator = hp))
                }
            }
        }
        val encounter = requireNotNull(game.encounter)
        return game.copy(characters = characters, encounter = encounter.copy(combatants = combatants))
    }
}

object MagicRuntimeRules {
    fun declaredMemory(magic: MagicRuntimeSnapshot, profile: RuleProfile, actorId: String, memoryId: String): MemorizedSpellSnapshot? {
        val memory = magic.memorized[memoryId] ?: return null
        if (memory.ownerId != actorId || memory.profile != profile || memory.consumed) return null
        return memory
    }

    fun consumeDeclared(magic: MagicRuntimeSnapshot, memoryId: String, interrupted: Boolean): MagicRuntimeSnapshot {
        val memory = magic.memorized[memoryId] ?: return magic
        if (memory.consumed) return magic
        return magic.copy(memorized = magic.memorized + (memoryId to memory.copy(consumed = true, interrupted = interrupted))).canonical()
    }

    fun advanceActiveEffectsByRounds(magic: MagicRuntimeSnapshot, rounds: Int): MagicRuntimeSnapshot {
        require(rounds >= 0)
        if (rounds == 0) return magic
        val effects = magic.activeEffects.mapValues { (_, e) ->
            val remaining=e.remaining
            if (e.durationUnit != SpellDurationUnit.ROUND || remaining == null) e
            else e.copy(remaining = (remaining - rounds).coerceAtLeast(0))
        }.filterValues { e ->
            val remaining=e.remaining
            e.durationUnit != SpellDurationUnit.ROUND || remaining == null || remaining > 0
        }
        return magic.copy(activeEffects = effects).canonical()
    }

    fun advanceActiveEffectsByTurns(magic: MagicRuntimeSnapshot, turns: Int): MagicRuntimeSnapshot {
        require(turns >= 0)
        if (turns == 0) return magic
        val effects = magic.activeEffects.mapValues { (_, e) ->
            val remaining=e.remaining
            if (e.durationUnit != SpellDurationUnit.TURN || remaining == null) e
            else e.copy(remaining = (remaining - turns).coerceAtLeast(0))
        }.filterValues { e ->
            val remaining=e.remaining
            e.durationUnit != SpellDurationUnit.TURN || remaining == null || remaining > 0
        }
        return magic.copy(activeEffects = effects).canonical()
    }
}

data class XpAwardPlan(
    val awardId: String,
    val sourceEventIds: List<String>,
    val totalXpBeforeIndividualModifiers: Long,
    val deltasByRecipientClass: Map<String, Long>
)

data class XpPlanResult(
    val plan: XpAwardPlan? = null,
    val blocker: String? = null
) {
    val success: Boolean get() = plan != null && blocker == null
}

object TreasureXpRuntimeRules {
    private val allowedPrimeModifiers = setOf(-20, -10, 0, 5, 10)

    fun recoverTreasure(profile: RuleProfile, experience: ExperienceRuntimeSnapshot, treasureId: String): ExperienceRuntimeSnapshot? {
        val treasure = experience.treasures[treasureId] ?: return null
        if (treasure.profile != profile) return null
        if (treasure.lifecycle == TreasureLifecycle.RECOVERED) return experience
        return experience.copy(
            treasures = experience.treasures + (treasureId to treasure.copy(lifecycle = TreasureLifecycle.RECOVERED))
        ).canonical()
    }

    /**
     * Plans an XP award without mutating state. Every division must be exact.
     * The source proves equal division but does not specify integer remainder handling,
     * so any remainder is a hard MISSING_EVIDENCE boundary.
     */
    fun planAward(profile: RuleProfile, experience: ExperienceRuntimeSnapshot): XpPlanResult {
        val pendingTreasures = experience.treasures.values
            .filter { it.lifecycle == TreasureLifecycle.RECOVERED && !it.xpAwarded }
        val pendingDefeats = experience.defeatedMonsters.values.filter { !it.xpAwarded }

        val profileMismatch = (pendingTreasures.map { it.profile } + pendingDefeats.map { it.profile } +
            experience.recipients.values.filter { it.surviving && it.awardEligible }.map { it.profile }).firstOrNull { it != profile }
        if (profileMismatch != null) return XpPlanResult(blocker = "PROFILE_MISMATCH:$profileMismatch:$profile")

        val treasureXp = pendingTreasures.sumOf { if (it.magical) 0L else it.gpValue }
        val monsterXp = pendingDefeats.sumOf { it.boundXp }
        val total = treasureXp + monsterXp
        val recipients = experience.recipients.values.filter { it.surviving && it.awardEligible }.sortedBy { it.recipientId }
        if (total == 0L) return XpPlanResult(
            plan = XpAwardPlan(nextAwardId(experience), emptyList(), 0L, emptyMap())
        )
        if (recipients.isEmpty()) return XpPlanResult(blocker = "NO_ELIGIBLE_SURVIVORS_OR_RETAINERS")
        if (total % recipients.size != 0L)
            return XpPlanResult(blocker = "MISSING_EVIDENCE:XP_SPLIT_REMAINDER:$total/${recipients.size}")

        val perRecipient = total / recipients.size
        val deltas = linkedMapOf<String, Long>()
        for (recipient in recipients) {
            val classes = recipient.classIds.distinct().sorted()
            if (profile == RuleProfile.OSE_CLASSIC_FANTASY && classes.size != 1)
                return XpPlanResult(blocker = "CLASSIC_MULTICLASS_NOT_AUTHORIZED:${recipient.recipientId}")
            if (profile == RuleProfile.OSE_ADVANCED_FANTASY && classes.size > 3)
                return XpPlanResult(blocker = "ADVANCED_MULTICLASS_TOO_MANY_CLASSES:${recipient.recipientId}")
            if (perRecipient % classes.size != 0L)
                return XpPlanResult(blocker = "MISSING_EVIDENCE:XP_SPLIT_REMAINDER:${recipient.recipientId}:$perRecipient/${classes.size}")
            val rawClassShare = perRecipient / classes.size
            for (classId in classes) {
                val percent = recipient.primeModifierPercentByClass[classId]
                    ?: return XpPlanResult(blocker = "MISSING_EVIDENCE:PRIME_MODIFIER:${recipient.recipientId}:$classId")
                if (percent !in allowedPrimeModifiers)
                    return XpPlanResult(blocker = "INVALID_PRIME_REQUISITE_MODIFIER:$classId:$percent")
                val adjustedNumerator = rawClassShare * (100L + percent)
                // OSE retainers receive a -50% XP penalty (Classic Rules Tome,
                // Retainers, p.57); apply it after each individual's primary
                // requisite adjustment. Non-integral outcomes remain evidence-gated.
                val divisor = if (recipient.retainer) 200L else 100L
                if (adjustedNumerator % divisor != 0L)
                    return XpPlanResult(blocker = if (recipient.retainer)
                        "MISSING_EVIDENCE:XP_RETAINER_FRACTION:${recipient.recipientId}:$classId:$adjustedNumerator/$divisor"
                    else "MISSING_EVIDENCE:XP_PRIME_MODIFIER_FRACTION:${recipient.recipientId}:$classId:$adjustedNumerator/$divisor")
                deltas["${recipient.recipientId}:$classId"] = adjustedNumerator / divisor
            }
        }
        val sources = buildList {
            pendingTreasures.forEach { add("TREASURE:${it.treasureId}") }
            pendingDefeats.forEach { add("MONSTER:${it.eventId}") }
        }.sorted()
        return XpPlanResult(XpAwardPlan(nextAwardId(experience), sources, total, deltas.toSortedMap()))
    }

    fun applyAward(game: GameExtensions, plan: XpAwardPlan): GameExtensions {
        if (plan.totalXpBeforeIndividualModifiers == 0L && plan.sourceEventIds.isEmpty()) return game
        var experience = game.experience
        val recipients = experience.recipients.toMutableMap()
        for ((key, delta) in plan.deltasByRecipientClass) {
            val split = key.lastIndexOf(':')
            if (split <= 0) continue
            val recipientId = key.substring(0, split)
            val classId = key.substring(split + 1)
            val r = recipients[recipientId] ?: continue
            recipients[recipientId] = r.copy(xpByClass = r.xpByClass + (classId to ((r.xpByClass[classId] ?: 0L) + delta)))
        }
        val treasures = experience.treasures.mapValues { (_, t) ->
            if ("TREASURE:${t.treasureId}" in plan.sourceEventIds) t.copy(xpAwarded = true) else t
        }
        val defeated = experience.defeatedMonsters.mapValues { (_, d) ->
            if ("MONSTER:${d.eventId}" in plan.sourceEventIds) d.copy(xpAwarded = true) else d
        }
        val record = XpAwardRecordSnapshot(plan.awardId, plan.sourceEventIds, plan.totalXpBeforeIndividualModifiers, plan.deltasByRecipientClass)
        experience = experience.copy(treasures = treasures, defeatedMonsters = defeated, recipients = recipients, awards = experience.awards + record).canonical()

        // Mirror XP into CharacterSnapshot when the recipient is a player character.
        // Retainers without CharacterSnapshot remain canonical in ExperienceRuntimeSnapshot.
        val characters = game.characters.toMutableMap()
        for ((recipientId, recipient) in experience.recipients) {
            val character = characters[recipientId] ?: continue
            characters[recipientId] = character.copy(xpByClass = recipient.xpByClass)
        }
        return game.copy(experience = experience, characters = characters)
    }

    private fun nextAwardId(experience: ExperienceRuntimeSnapshot) = "XP_AWARD#${experience.awards.size + 1}"
}

