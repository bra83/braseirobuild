package braseiro.ose.rules.shared

import braseiro.ose.model.RuleProfile
import braseiro.ose.rules.api.*
import kotlin.math.max

object SharedEvidence {
    fun attributes(profile: RuleProfile) = RuleEvidence(
        "ATTR.STR", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "pp.16-17" else "attribute tables audited in GAME_SYSTEM_SPEC §5.2"
    )
    fun attack(profile: RuleProfile) = RuleEvidence("RESOLVE.D20_ATTACK", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Referee/Player Tome",
        "GAME_SYSTEM_SPEC §§11,23-24")
    fun save(profile: RuleProfile) = RuleEvidence("RESOLVE.D20_SAVE", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Referee/Player Tome",
        "GAME_SYSTEM_SPEC §§11,27")
    fun ability(profile: RuleProfile) = RuleEvidence("RESOLVE.D20_ATTRIBUTE", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Referee/Player Tome",
        "GAME_SYSTEM_SPEC §11.1; Classic p.114 / Advanced audited equivalent")
    fun damage(profile: RuleProfile) = RuleEvidence("DAMAGE.BASE", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Referee/Player Tome",
        "GAME_SYSTEM_SPEC §25")
    fun initiative(profile: RuleProfile) = RuleEvidence("RESOLVE.D6_INITIATIVE", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        "GAME_SYSTEM_SPEC §§11,23.2")
    fun surprise(profile: RuleProfile) = RuleEvidence("RESOLVE.D6_SURPRISE", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        "GAME_SYSTEM_SPEC §§11,19")
    fun reaction(profile: RuleProfile) = RuleEvidence("RESOLVE.2D6_REACTION", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        "GAME_SYSTEM_SPEC §§11,20")
    fun morale(profile: RuleProfile) = RuleEvidence("RESOLVE.2D6_MORALE", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        "GAME_SYSTEM_SPEC §§11,21")
    fun xIn6(profile: RuleProfile) = RuleEvidence("RESOLVE.X_IN_6", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        "GAME_SYSTEM_SPEC §11")
    fun percentile(profile: RuleProfile) = RuleEvidence("RESOLVE.D100", profile,
        if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "Classic Rules Tome" else "Advanced Player's Tome",
        "GAME_SYSTEM_SPEC §11")
}

object SharedConfirmedRules {
    /** Universal single-prime-requisite XP modifier table from GAME_SYSTEM_SPEC §5. */
    fun singlePrimeXpModifier(score: Int): Int {
        require(score in 3..18)
        return when(score) {
            in 3..5 -> -20
            in 6..8 -> -10
            in 9..12 -> 0
            in 13..15 -> 5
            else -> 10
        }
    }

    fun attributeModifier(score: Int): Int {
        require(score in 3..18)
        return when (score) {
            3 -> -3
            in 4..5 -> -2
            in 6..8 -> -1
            in 9..12 -> 0
            in 13..15 -> 1
            in 16..17 -> 2
            18 -> 3
            else -> error("unreachable")
        }
    }

    fun descendingArmorClass(base: Int = 9, dexterity: Int): Int = base - attributeModifier(dexterity)
    fun ascendingArmorClass(base: Int = 10, dexterity: Int): Int = base + attributeModifier(dexterity)

    fun attack(profile: RuleProfile, input: AttackInput): AttackResult {
        require(input.d20 in 1..20)
        val evidence = SharedEvidence.attack(profile)
        if (input.d20 == 1) return AttackResult(false, 1, 1, input.targetArmorClass, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(evidence), "natural 1 misses"))
        if (input.d20 == 20) return AttackResult(true, 20, 20, input.targetArmorClass, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(evidence), "natural 20 hits"))
        return when (input.mode) {
            AttackArmorMode.DESCENDING -> {
                val required = input.thac0 - input.targetArmorClass
                val total = input.d20 + input.modifier
                AttackResult(total >= required, input.d20, total, required, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(evidence)))
            }
            AttackArmorMode.ASCENDING -> {
                val total = input.d20 + input.attackBonusAscending + input.modifier
                AttackResult(total >= input.targetArmorClass, input.d20, total, input.targetArmorClass,
                    RuleTrace(EvidenceStatus.OPTIONAL_RULE, listOf(evidence), "ascending AC active only when campaign option enables it"))
            }
        }
    }

    fun savingThrow(profile: RuleProfile, roll: Int, target: Int): SaveResult {
        require(roll in 1..20); require(target >= 2)
        return SaveResult(roll >= target, roll, target, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.save(profile))))
    }

    fun abilityCheck(profile: RuleProfile, roll: Int, attribute: Int, difficultyModifier: Int = 0): AbilityCheckResult {
        require(roll in 1..20); require(attribute in 3..18)
        val success = when (roll) { 1 -> true; 20 -> false; else -> roll + difficultyModifier <= attribute }
        return AbilityCheckResult(success, roll, roll + difficultyModifier, attribute,
            RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.ability(profile))))
    }

    fun sideInitiative(profile: RuleProfile, sideRoll: Int, opposingRoll: Int): InitiativeResult {
        require(sideRoll in 1..6); require(opposingRoll in 1..6)
        val outcome = when {
            sideRoll > opposingRoll -> InitiativeOutcome.ACTS_FIRST
            sideRoll < opposingRoll -> InitiativeOutcome.ACTS_SECOND
            else -> InitiativeOutcome.TIE
        }
        return InitiativeResult(sideRoll, opposingRoll, outcome, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.initiative(profile)), "Ties require reroll or simultaneous actions per source procedure."))
    }

    fun surprise(profile: RuleProfile, roll: Int): SurpriseResult {
        require(roll in 1..6)
        return SurpriseResult(roll, roll <= 2, RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, listOf(SharedEvidence.surprise(profile)), "1-2 on 1d6 = surprised when a surprise check is required."))
    }

    fun xIn6(profile: RuleProfile, roll: Int, target: Int): XIn6Result {
        require(roll in 1..6); require(target in 0..6)
        return XIn6Result(roll, target, roll <= target, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.xIn6(profile))))
    }

    fun percentile(profile: RuleProfile, roll: Int, chance: Int): PercentileResult {
        require(roll in 1..100); require(chance in 0..100)
        return PercentileResult(roll, chance, roll <= chance, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.percentile(profile))))
    }

    fun reaction(profile: RuleProfile, die1: Int, die2: Int, charismaModifier: Int = 0): ReactionResult {
        require(die1 in 1..6); require(die2 in 1..6)
        val total = die1 + die2 + charismaModifier
        val disposition = when {
            total <= 2 -> ReactionDisposition.HOSTILE_ATTACKS
            total <= 5 -> ReactionDisposition.ANTAGONISTIC_MAY_ATTACK
            total <= 8 -> ReactionDisposition.NEUTRAL_UNCERTAIN
            total <= 11 -> ReactionDisposition.INDIFFERENT_UNINTERESTED
            else -> ReactionDisposition.FRIENDLY_HELPFUL
        }
        return ReactionResult(die1, die2, charismaModifier, total, disposition, RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, listOf(SharedEvidence.reaction(profile)), "Reaction is contextual and does not erase NPC history."))
    }

    fun morale(profile: RuleProfile, die1: Int, die2: Int, morale: Int, situationalModifier: Int = 0): MoraleResult {
        require(die1 in 1..6); require(die2 in 1..6); require(morale in 2..12); require(situationalModifier in -2..2)
        val adjusted = if (morale == 2 || morale == 12) morale else (morale + situationalModifier).coerceIn(2,12)
        val total = die1 + die2
        return MoraleResult(die1, die2, morale, situationalModifier, adjusted, total, total <= adjusted, RuleTrace(EvidenceStatus.OPTIONAL_RULE, listOf(SharedEvidence.morale(profile)), "Morale is optional; campaign OptionSet must enable OPT.MORALE before this resolver is called."))
    }

    fun basicPlayerDamage(profile: RuleProfile, d6: Int, strengthModifier: Int = 0): DamageResult {
        require(d6 in 1..6)
        return DamageResult(max(1, d6 + strengthModifier), RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.damage(profile))))
    }

    fun naturalHealing(profile: RuleProfile, d3: Int, uninterruptedFullDayRest: Boolean): DamageResult {
        require(d3 in 1..3)
        val amount = if (uninterruptedFullDayRest) d3 else 0
        return DamageResult(amount, RuleTrace(EvidenceStatus.CANONICAL_RULE, listOf(SharedEvidence.damage(profile)), "1d3 HP only for a full uninterrupted day of rest"))
    }
}
