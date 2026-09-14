package braseiro.ose.character

import braseiro.ose.model.RuleProfile
import braseiro.ose.rules.advanced.AdvancedCharacterRules
import braseiro.ose.rules.api.*
import braseiro.ose.rules.classic.ClassicCharacterRules

class CharacterCreator(
    private val classic: CharacterRulesProvider = ClassicCharacterRules(),
    private val advanced: CharacterRulesProvider = AdvancedCharacterRules()
) {
    fun create(request: CharacterCreationRequest): CharacterCreationResult = when (request.profile) {
        RuleProfile.OSE_CLASSIC_FANTASY -> {
            if (request.method != CreationMethod.CLASSIC) CharacterCreationResult.Rejected(
                CreationFailureCode.PROFILE_METHOD_MISMATCH,
                "Classic profile cannot fall back to an Advanced creation method",
                RuleTrace(EvidenceStatus.MISSING_EVIDENCE, emptyList(), "PROFILE_DRIFT = FORBIDDEN")
            ) else classic.create(request)
        }
        RuleProfile.OSE_ADVANCED_FANTASY -> {
            if (request.method == CreationMethod.CLASSIC) CharacterCreationResult.Rejected(
                CreationFailureCode.PROFILE_METHOD_MISMATCH,
                "Advanced profile cannot fall back to Classic",
                RuleTrace(EvidenceStatus.MISSING_EVIDENCE, emptyList(), "PROFILE_DRIFT = FORBIDDEN")
            ) else advanced.create(request)
        }
    }
}
