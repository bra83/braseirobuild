package braseiro.ose.rules.shared

import braseiro.ose.model.*
import braseiro.ose.rules.api.OptionIds
import braseiro.ose.rules.api.OptionPolicyContract

data class MovementRate(
    val encumbranceMode: EncumbranceMode,
    val baseMoveMeters: Int,
    val encounterMoveMeters: Int,
    val immobile: Boolean
)

object MovementLightRules {
    fun encumbranceMode(profile: RuleProfile, optionSet: OptionSet): EncumbranceMode {
        val value = OptionPolicyContract.value(profile, optionSet, OptionIds.ENCUMBRANCE) ?: "NONE"
        return EncumbranceMode.valueOf(value)
    }

    fun movementRate(
        profile: RuleProfile,
        optionSet: OptionSet,
        armor: ArmorLoadCategory,
        treasureWeightCoins: Int,
        equipmentWeightCoins: Int,
        significantTreasure: Boolean
    ): MovementRate {
        require(treasureWeightCoins >= 0)
        require(equipmentWeightCoins >= 0)
        val mode = encumbranceMode(profile, optionSet)
        if ((mode == EncumbranceMode.BASIC && treasureWeightCoins > 1600) ||
            (mode == EncumbranceMode.DETAILED && treasureWeightCoins + equipmentWeightCoins > 1600)) {
            return MovementRate(mode, 0, 0, true)
        }
        return when (mode) {
            EncumbranceMode.NONE -> MovementRate(mode, 36, 12, false)
            EncumbranceMode.BASIC -> {
                val pair = when (armor) {
                    ArmorLoadCategory.NONE -> if (significantTreasure) 27 to 9 else 36 to 12
                    ArmorLoadCategory.LIGHT -> if (significantTreasure) 18 to 6 else 27 to 9
                    ArmorLoadCategory.HEAVY -> if (significantTreasure) 9 to 3 else 18 to 6
                }
                MovementRate(mode, pair.first, pair.second, false)
            }
            EncumbranceMode.DETAILED -> {
                val total = treasureWeightCoins + equipmentWeightCoins
                val pair = when {
                    total <= 400 -> 36 to 12
                    total <= 600 -> 27 to 9
                    total <= 800 -> 18 to 6
                    total <= 1600 -> 9 to 3
                    else -> 0 to 0
                }
                MovementRate(mode, pair.first, pair.second, pair.first == 0)
            }
        }
    }

    fun treasureWeightCoins(profile: RuleProfile, kind: String, quantity: Int = 1): Int {
        require(quantity >= 0)
        val perItem = when (kind.trim().uppercase()) {
            "COIN", "GEM", "SCROLL" -> 1
            "JEWELRY" -> if (profile == RuleProfile.OSE_CLASSIC_FANTASY) 1 else 10
            "POTION", "WAND" -> 10
            "ROD" -> 20
            "STAFF" -> 40
            else -> throw IllegalArgumentException("UNKNOWN_TREASURE_WEIGHT_KIND:$kind")
        }
        return perItem * quantity
    }

    fun dungeonMoveLimitMeters(rate: MovementRate, knownArea: Boolean, refereeAllowsFastKnownMovement: Boolean): Int =
        if (rate.immobile) 0
        else if (knownArea && refereeAllowsFastKnownMovement) rate.baseMoveMeters * 3
        else rate.baseMoveMeters

    fun encounterMoveLimitMeters(rate: MovementRate, context: EncounterDistanceContext): Int =
        if (rate.immobile) 0
        else when (context) {
            EncounterDistanceContext.DUNGEON -> rate.encounterMoveMeters
            EncounterDistanceContext.WILDERNESS, EncounterDistanceContext.WATERBORNE -> rate.baseMoveMeters
        }

    fun swimMoveLimitMeters(rate: MovementRate): Int = rate.baseMoveMeters / 2

    fun defaultLightDurationTurns(kind: LightSourceKind): Int = when (kind) {
        LightSourceKind.TORCH -> 6
        LightSourceKind.LANTERN -> 24
    }

    fun advanceLightByTurns(sources: List<LightSourceSnapshot>, turns: Long): List<LightSourceSnapshot> {
        require(turns >= 0)
        if (turns == 0L) return sources
        return sources.map { light ->
            if (!light.lit || light.remainingTurns == 0) light
            else {
                val remaining = (light.remainingTurns.toLong() - turns).coerceAtLeast(0).toInt()
                light.copy(remainingTurns = remaining, lit = remaining > 0)
            }
        }
    }

    fun advanceOilPoolsByTurns(pools: List<OilPoolSnapshot>, turns: Long): List<OilPoolSnapshot> {
        require(turns >= 0)
        if (turns == 0L) return pools
        return pools.map { pool ->
            if (!pool.active || pool.remainingTurns == 0) pool
            else {
                val remaining = (pool.remainingTurns.toLong() - turns).coerceAtLeast(0).toInt()
                pool.copy(remainingTurns = remaining, active = remaining > 0)
            }
        }
    }
}
