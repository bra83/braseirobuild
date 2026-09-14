package braseiro.ose.rules.api

import braseiro.ose.model.*
import kotlinx.serialization.Serializable

@Serializable data class RuleEvidence(
    val ruleId: String,
    val profile: RuleProfile,
    val source: String,
    val location: String
) {
    fun ref(): String = "$ruleId|${profile.name}|$source|$location"
}

@Serializable enum class EvidenceStatus { CANONICAL_RULE, CANONICAL_PROCEDURE, OPTIONAL_RULE, PRODUCT_PRESET, PROCEDURAL_TABLE, ADVENTURE_LOCAL_RULE, REFEREE_JUDGMENT, MISSING_EVIDENCE }

@Serializable data class RuleTrace(
    val status: EvidenceStatus,
    val evidence: List<RuleEvidence>,
    val note: String = ""
)

@Serializable enum class CreationMethod { CLASSIC, ADVANCED_BASIC, ADVANCED_METHOD }
@Serializable data class AttributeAdjustment(val decrease: AttributeKey, val increase: AttributeKey)

@Serializable data class CharacterCreationRequest(
    val profile: RuleProfile,
    val method: CreationMethod,
    val characterId: String,
    val name: String,
    val rolledAttributes: Attributes,
    val classIds: List<String>,
    val raceId: String? = null,
    val adjustments: List<AttributeAdjustment> = emptyList(),
    val hpRolls: List<Int>,
    val optionIds: Set<String> = emptySet(),
    val allowNpcOnlyCombination: Boolean = false
)

@Serializable enum class CreationFailureCode {
    PROFILE_METHOD_MISMATCH,
    UNKNOWN_CLASS,
    UNKNOWN_RACE,
    CLASS_PREREQUISITE,
    RACE_PREREQUISITE,
    RACE_CLASS_ILLEGAL,
    NPC_ONLY_COMBINATION,
    MULTICLASS_OPTION_REQUIRED,
    TOO_MANY_CLASSES,
    HP_ROLL_INVALID,
    ATTRIBUTE_ADJUSTMENT_ILLEGAL,
    MISSING_EVIDENCE
}

@Serializable sealed class CharacterCreationResult {
    @Serializable data class Created(val character: CharacterSnapshot, val trace: RuleTrace) : CharacterCreationResult()
    @Serializable data class Rejected(val code: CreationFailureCode, val detail: String, val trace: RuleTrace) : CharacterCreationResult()
}

@Serializable data class ClassLevelOneDescriptor(
    val classId: String,
    val hitDie: Int,
    val thac0: Int,
    val attackBonusAscending: Int,
    val saves: SavingThrows,
    val primary: Set<AttributeKey>,
    val minimums: Map<AttributeKey, Int>,
    val basicRaceId: String,
    val evidence: RuleEvidence
)

@Serializable data class RaceDescriptor(
    val raceId: String,
    val minimums: Map<AttributeKey, Int>,
    val modifiers: Map<AttributeKey, Int>,
    val allowedClassMaxLevel: Map<String, Int>,
    val npcOnlyClasses: Set<String>,
    val evidence: RuleEvidence
)

interface CharacterRulesProvider {
    val profile: RuleProfile
    fun create(request: CharacterCreationRequest): CharacterCreationResult
}

@Serializable enum class AttackArmorMode { DESCENDING, ASCENDING }
@Serializable data class AttackInput(
    val d20: Int,
    val mode: AttackArmorMode,
    val thac0: Int,
    val attackBonusAscending: Int,
    val targetArmorClass: Int,
    val modifier: Int = 0
)
@Serializable data class AttackResult(val hit: Boolean, val natural: Int, val total: Int, val target: Int, val trace: RuleTrace)
@Serializable data class SaveResult(val success: Boolean, val roll: Int, val target: Int, val trace: RuleTrace)
@Serializable data class AbilityCheckResult(val success: Boolean, val roll: Int, val modifiedRoll: Int, val attribute: Int, val trace: RuleTrace)
@Serializable data class DamageResult(val damage: Int, val trace: RuleTrace)
@Serializable enum class InitiativeOutcome { ACTS_FIRST, ACTS_SECOND, TIE }
@Serializable enum class ReactionDisposition { HOSTILE_ATTACKS, ANTAGONISTIC_MAY_ATTACK, NEUTRAL_UNCERTAIN, INDIFFERENT_UNINTERESTED, FRIENDLY_HELPFUL }
@Serializable data class InitiativeResult(val sideRoll: Int, val opposingRoll: Int, val outcome: InitiativeOutcome, val trace: RuleTrace)
@Serializable data class SurpriseResult(val roll: Int, val surprised: Boolean, val trace: RuleTrace)
@Serializable data class XIn6Result(val roll: Int, val target: Int, val success: Boolean, val trace: RuleTrace)
@Serializable data class PercentileResult(val roll: Int, val chance: Int, val success: Boolean, val trace: RuleTrace)
@Serializable data class ReactionResult(val die1: Int, val die2: Int, val modifier: Int, val total: Int, val disposition: ReactionDisposition, val trace: RuleTrace)
@Serializable data class MoraleResult(val die1: Int, val die2: Int, val morale: Int, val situationalModifier: Int, val adjustedMorale: Int, val total: Int, val holds: Boolean, val trace: RuleTrace)

object OptionIds {
    // Canonical IDs match GAME_SYSTEM_SPEC RULE/OPTION names.
    const val ASCENDING_ARMOR_CLASS = "OPT.ASCENDING_AC"
    const val VARIABLE_WEAPON_DAMAGE = "OPT.VARIABLE_WEAPON_DAMAGE"
    const val INDIVIDUAL_INITIATIVE = "OPT.INDIVIDUAL_INITIATIVE"
    const val MORALE = "OPT.MORALE"
    const val ENCUMBRANCE = "OPT.ENCUMBRANCE"
    const val REROLL_LOW_HP = "OPT.REROLL_HP_1_2"
    const val MULTICLASS = "OPT.ADV_MULTICLASS"
    const val WEAPON_PROFICIENCY = "OPT.ADV_WEAPON_PROF"
    const val WEAPON_SPECIALIZATION = "OPT.ADV_WEAPON_SPEC"
    const val SECONDARY_SKILLS = "OPT.ADV_SECONDARY_SKILLS"
    const val CLASSIC_HIGH_LEVEL = "OPT.CLASSIC_HIGH_LEVEL"
    const val DIRECT_THAC0 = "OPT.DIRECT_THAC0"
    const val ADV_CREATION_BASIC = "OPT.ADV_CREATION_BASIC"
    const val ADV_CREATION_ADVANCED = "OPT.ADV_CREATION_ADVANCED"
    const val RETURN_FROM_DEATH = "OPT.ADV_RETURN_FROM_DEATH"
    const val LIFT_DEMIHUMAN_LIMITS = "OPT.LIFT_DEMIHUMAN_LIMITS"
    const val HUMAN_COMPENSATION = "OPT.HUMAN_COMPENSATION"
    const val TWO_WEAPON = "OPT.ADV_TWO_WEAPON"
    const val CHARGE = "OPT.ADV_CHARGE"
    const val MISSILE_INTO_MELEE = "OPT.ADV_MISSILE_INTO_MELEE"
    const val PARRY = "OPT.ADV_PARRY"
    const val SPLASH_WEAPONS = "OPT.ADV_SPLASH_WEAPONS"
    const val SUBDUAL = "OPT.SUBDUAL"
    const val INVULNERABILITY_EXTENSION = "OPT.INVULNERABILITY_EXTENSION"
    const val ILLUSIONIST_STAFF = "OPT.ADV_ILLUSIONIST_STAFF"
    const val VARIABLE_WEATHER_WIND = "OPT.VARIABLE_WEATHER_WIND"

    private val legacyAliases = mapOf(
        "ASCENDING_ARMOR_CLASS" to ASCENDING_ARMOR_CLASS,
        "VARIABLE_WEAPON_DAMAGE" to VARIABLE_WEAPON_DAMAGE,
        "INDIVIDUAL_INITIATIVE" to INDIVIDUAL_INITIATIVE,
        "MORALE" to MORALE,
        "REROLL_LOW_HP" to REROLL_LOW_HP,
        "MULTICLASS" to MULTICLASS,
        "WEAPON_PROFICIENCY" to WEAPON_PROFICIENCY,
        "WEAPON_SPECIALIZATION" to WEAPON_SPECIALIZATION,
        "SECONDARY_SKILLS" to SECONDARY_SKILLS,
        "OSE.OPTION.MORALE" to MORALE,
        "OSE.OPTION.ENCUMBRANCE_BASIC" to "$ENCUMBRANCE=BASIC"
    )

    fun canonical(raw: String): String = legacyAliases[raw] ?: raw
    fun enabled(optionSet: OptionSet, optionId: String): Boolean = optionSet.optionIds
        .map(::canonical)
        .any { it == optionId || it.startsWith("$optionId=") }
    fun value(optionSet: OptionSet, optionId: String): String? = optionSet.optionIds
        .map(::canonical)
        .firstOrNull { it.startsWith("$optionId=") }
        ?.substringAfter('=')
}
