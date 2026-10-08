package braseiro.ose.rules.shared

/**
 * OSE Classic Rules Tome pp. 16, 56–58, 133; Advanced Player/Referee
 * retainers and morale, consolidated in GAME_SYSTEM_SPEC §§5, 21, 31.
 * This is pure, deterministic mechanics: no UI dice, no AI rules, no RAG.
 */
object RetainerMercenaryRules {
    data class CharismaRetainerLimits(
        val maxRetainers: Int,
        val baseLoyalty: Int,
        val hiringReactionModifier: Int
    )

    fun charisma(score: Int): CharismaRetainerLimits {
        require(score in 3..18)
        return when (score) {
            3 -> CharismaRetainerLimits(1, 4, -2)
            in 4..5 -> CharismaRetainerLimits(2, 5, -1)
            in 6..8 -> CharismaRetainerLimits(3, 6, -1)
            in 9..12 -> CharismaRetainerLimits(4, 7, 0)
            in 13..15 -> CharismaRetainerLimits(5, 8, 1)
            in 16..17 -> CharismaRetainerLimits(6, 9, 1)
            else -> CharismaRetainerLimits(7, 10, 2)
        }
    }

    enum class RecruitmentOutcome {
        OFFENDED, REFUSED, REROLL_REQUIRED, ACCEPTED, ACCEPTED_PLUS_ONE_LOYALTY
    }

    data class RecruitmentResult(
        val rawTwoDice: Int,
        val effectiveTotal: Int,
        val outcome: RecruitmentOutcome
    ) {
        val accepted: Boolean get() = outcome == RecruitmentOutcome.ACCEPTED ||
            outcome == RecruitmentOutcome.ACCEPTED_PLUS_ONE_LOYALTY
        val loyaltyBonus: Int get() =
            if (outcome == RecruitmentOutcome.ACCEPTED_PLUS_ONE_LOYALTY) 1 else 0
    }

    /**
     * Any offer/reputation judgment is supplied through a separate explicit
     * adjudication, never silently guessed from a generic NPC's backstory.
     * OSE hiring table: 2- offended, 3–5 refused, 6–8 reroll, 9–11 accept,
     * 12+ accept with +1 loyalty.
     */
    fun recruitment(dieA: Int, dieB: Int, hiringReactionModifier: Int, refereeModifier: Int = 0): RecruitmentResult {
        require(dieA in 1..6 && dieB in 1..6)
        require(hiringReactionModifier in -2..2)
        require(refereeModifier in -4..2)
        val raw = dieA + dieB
        val total = raw + hiringReactionModifier + refereeModifier
        val result = when {
            total <= 2 -> RecruitmentOutcome.OFFENDED
            total <= 5 -> RecruitmentOutcome.REFUSED
            total <= 8 -> RecruitmentOutcome.REROLL_REQUIRED
            total <= 11 -> RecruitmentOutcome.ACCEPTED
            else -> RecruitmentOutcome.ACCEPTED_PLUS_ONE_LOYALTY
        }
        return RecruitmentResult(raw, total, result)
    }

    data class LoyaltyResult(val diceTotal: Int, val threshold: Int, val holds: Boolean)
    /** 2d6 <= final loyalty: retainer stays. Never substitute combat morale. */
    fun loyalty(dieA: Int, dieB: Int, finalLoyalty: Int): LoyaltyResult {
        require(dieA in 1..6 && dieB in 1..6)
        require(finalLoyalty in 2..12)
        return LoyaltyResult(dieA + dieB, finalLoyalty, dieA + dieB <= finalLoyalty)
    }

    /** Troop categories are not retainer morale and ignore employer charisma. */
    enum class Troop { MILITIA, BARBARIAN_HORDE, TRAINED_WARRIORS }

    fun mercenaryMorale(
        troop: Troop,
        mounted: Boolean = false,
        elite: Boolean = false,
        fanatic: Boolean = false
    ): Int {
        val base = when (troop) {
            Troop.MILITIA -> 6
            Troop.BARBARIAN_HORDE -> 7
            Troop.TRAINED_WARRIORS -> 8
        }
        // The listed categories are source labels, not cumulative traits:
        // a fanatic mounted elite is not automatically +4 without referee authority.
        require(listOf(mounted, elite, fanatic).count { it } <= 1) { "MORALE_TRAIT_COMBINATION_REQUIRES_REFEREE" }
        return (base + (if (mounted || elite) 1 else if (fanatic) 2 else 0)).coerceAtMost(12)
    }

    data class DailyWageQuote(val daysOwed: Long, val amountGp: Long, val blocker: String? = null) {
        val payable: Boolean get() = blocker == null && daysOwed > 0L
    }
    /** Day boundaries from campaign clock only. Legacy histories must be reconciled. */
    fun quoteDailyWage(wageGp: Int, paidThroughDay: Long?, currentDay: Long): DailyWageQuote {
        require(wageGp >= 0 && currentDay >= 0L)
        if (paidThroughDay == null) return DailyWageQuote(0, 0, "MISSING_EVIDENCE:LEGACY_WAGE_BASELINE")
        if (paidThroughDay > currentDay) return DailyWageQuote(0, 0, "CLOCK_BEFORE_LAST_PAYMENT")
        val days = currentDay - paidThroughDay
        val amount = try { Math.multiplyExact(days, wageGp.toLong()) }
            catch (_: ArithmeticException) { return DailyWageQuote(days, 0, "WAGE_OVERFLOW") }
        return DailyWageQuote(days, amount)
    }

    /** Exact OSE half-share distribution; fractional coin handling stays evidence-gated. */
    data class TreasureShareResult(val playerShareGp: Long, val retainerSharesGp: List<Long>)
    fun divideTreasureExactly(totalGp: Long, playerCount: Int, retainerHalfShares: List<Int>): TreasureShareResult? {
        require(totalGp >= 0L && playerCount > 0)
        require(retainerHalfShares.all { it >= 1 })
        val units = 2L * playerCount + retainerHalfShares.sumOf { it.toLong() }
        if (units == 0L) return null
        // Half-share unit is totalGp/units; avoid inventing a rounding policy.
        if (totalGp % units != 0L) return null
        val eachHalf = totalGp / units
        return TreasureShareResult(eachHalf * 2L, retainerHalfShares.map { eachHalf * it })
    }
}
