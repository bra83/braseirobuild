package braseiro.ose.model

import kotlinx.serialization.Serializable

/**
 * One canonical OSE adventuring retainer; military mercenaries and specialists
 * are separate hire categories and must not be inserted into this registry.
 * Local source: GAME_SYSTEM_SPEC.md §31 (Classic pp. 56–58, Advanced equivalents).
 * Existing saves deserialize with an empty map via GameExtensions default value.
 */
@Serializable
data class RetainerContractSnapshot(
    val npcId: String,
    val employerCharacterId: String,
    val dailyWageGp: Int,
    /** Treasure shares expressed in half-shares: 1 = a half share, 2 = a full share. */
    val treasureHalfShares: Int,
    val loyalty: Int,
    val active: Boolean = true,
    val hiredAtTurn: Long = 0,
    val failedLoyaltyChecks: Int = 0,
    val lastLoyaltyCheckTurn: Long? = null,
    val lastLoyaltyCheckReason: String? = null
) {
    init {
        require(npcId.isNotBlank() && employerCharacterId.isNotBlank())
        require(dailyWageGp >= 0)
        require(treasureHalfShares >= 1)
        require(loyalty in 2..12)
        require(hiredAtTurn >= 0L && failedLoyaltyChecks >= 0)
        require(lastLoyaltyCheckTurn == null || lastLoyaltyCheckTurn >= 0L)
    }
}
