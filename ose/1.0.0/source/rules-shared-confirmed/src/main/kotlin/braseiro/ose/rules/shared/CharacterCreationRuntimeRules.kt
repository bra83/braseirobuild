package braseiro.ose.rules.shared

import braseiro.ose.model.RuleProfile

/**
 * Source-bound dice primitives for normal character creation.
 * Dice values are produced by the RULES_DICE caller; no UI/caller numeric roll is accepted.
 */
data class CreationAttributeRolls(
    val strength: Int,
    val intelligence: Int,
    val wisdom: Int,
    val dexterity: Int,
    val constitution: Int,
    val charisma: Int,
    val diceConsumed: Int
) {
    init {
        require(listOf(strength,intelligence,wisdom,dexterity,constitution,charisma).all { it in 3..18 })
        require(diceConsumed == 18)
    }
    fun ordered(): List<Int> = listOf(strength,intelligence,wisdom,dexterity,constitution,charisma)
}

data class InitialHpRoll(
    val rawRoll: Int,
    val constitutionModifier: Int,
    val hitPoints: Int,
    val rerollRequiredByEnabledOption: Boolean,
    val status: String
)

data class CampaignRulesFreeze(
    val profile: RuleProfile,
    val authorityId: String,
    val authorityVersion: String,
    val authoritySha256: String,
    val optionFingerprint: String,
    val advancedCreationPolicy: String? = null
)

object CharacterCreationRuntimeRules {
    const val CLASSIC_AUTHORITY_SHA256 = "9b23bea5cb6d0b11ff72b7d6111cbf00a88c8054cd381e51d92f7438e586741d"
    const val ADVANCED_REFEREE_AUTHORITY_SHA256 = "63ea8b2cbcb32b671a70118862a2ecd73c5f670a031914da0e94e6955acf57a1"
    const val ADVANCED_PLAYER_AUTHORITY_SHA256 = "9c8c23ecf86c41bc17ee8b9acf409549c02898a6ccf77c7047b51ff8d85c7eb3"

    /** Canonical C-01 / Advanced creation base: six ordered 3d6 rolls. */
    fun rollAttributes(authoritativeDie: (Int) -> Int): CreationAttributeRolls {
        fun d6(): Int = authoritativeDie(6).also { require(it in 1..6) }
        fun threeD6(): Int = d6() + d6() + d6()
        return CreationAttributeRolls(
            strength = threeD6(), intelligence = threeD6(), wisdom = threeD6(),
            dexterity = threeD6(), constitution = threeD6(), charisma = threeD6(),
            diceConsumed = 18
        )
    }

    /** Classic C-10 and Advanced creation equipment procedures use starting wealth before purchases. */
    fun rollStartingWealthGp(authoritativeDie: (Int) -> Int): Int {
        fun d6(): Int = authoritativeDie(6).also { require(it in 1..6) }
        return (d6() + d6() + d6()) * 10
    }

    /** Backward-compatible explicit Advanced Method spelling. */
    fun rollAdvancedMethodStartingWealthGp(authoritativeDie: (Int) -> Int): Int = rollStartingWealthGp(authoritativeDie)

    /**
     * Initial HP roll. Minimum 1 after CON is proven.
     * The GSS proves that OPT.REROLL_HP_1_2 exists and applies when the initial result is 1/2,
     * but does not prove in the current accessible evidence whether reroll is once/repeat/keep-better.
     * Therefore an enabled option at 1/2 is surfaced without inventing a reroll algorithm.
     */
    fun rollInitialHp(
        hitDieSides: Int,
        constitutionModifier: Int,
        rerollHp1Or2OptionEnabled: Boolean,
        authoritativeDie: (Int) -> Int
    ): InitialHpRoll {
        require(hitDieSides >= 1)
        val raw = authoritativeDie(hitDieSides).also { require(it in 1..hitDieSides) }
        if (rerollHp1Or2OptionEnabled && raw <= 2) {
            return InitialHpRoll(
                rawRoll = raw,
                constitutionModifier = constitutionModifier,
                hitPoints = maxOf(1, raw + constitutionModifier),
                rerollRequiredByEnabledOption = true,
                status = "MISSING_EVIDENCE_EXACT_REROLL_PROCEDURE"
            )
        }
        return InitialHpRoll(raw, constitutionModifier, maxOf(1, raw + constitutionModifier), false, "CANONICAL")
    }

    /** Freeze the profile/source/options before dependent campaign state is created. */
    fun freezeCampaignRules(
        profile: RuleProfile,
        authorityId: String,
        authorityVersion: String,
        authoritySha256: String,
        normalizedOptions: Map<String,String>,
        advancedCreationPolicy: String? = null
    ): CampaignRulesFreeze {
        require(authorityId.isNotBlank() && authorityVersion.isNotBlank())
        val expected = when (profile) {
            RuleProfile.OSE_CLASSIC_FANTASY -> CLASSIC_AUTHORITY_SHA256
            RuleProfile.OSE_ADVANCED_FANTASY -> ADVANCED_REFEREE_AUTHORITY_SHA256
        }
        require(authoritySha256 == expected) { "PROFILE_AUTHORITY_MISMATCH" }
        if (profile == RuleProfile.OSE_ADVANCED_FANTASY) {
            require(advancedCreationPolicy in setOf("BASIC_ONLY","ADVANCED_ONLY","BOTH_ALLOWED"))
        } else require(advancedCreationPolicy == null)
        val fingerprint = normalizedOptions.toSortedMap().entries.joinToString(";") { "${it.key}=${it.value}" }
        return CampaignRulesFreeze(profile, authorityId, authorityVersion, authoritySha256, fingerprint, advancedCreationPolicy)
    }

    fun assertFrozenUnchanged(frozen: CampaignRulesFreeze, candidate: CampaignRulesFreeze) {
        require(frozen == candidate) { "FROZEN_CAMPAIGN_RULES_CHANGED" }
    }
}
