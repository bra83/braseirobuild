package braseiro.ose.character

import braseiro.ose.model.*
import braseiro.ose.rng.*
import braseiro.ose.rules.advanced.AdvancedCatalog
import braseiro.ose.rules.api.*
import braseiro.ose.rules.classic.ClassicCatalog
import braseiro.ose.rules.shared.CharacterCreationRuntimeRules

/** Native character creation. The UI supplies choices only; all dice come from persisted RULES_DICE. */
class NativeCharacterCreationCoordinator(private val creator: CharacterCreator = CharacterCreator()) {
    data class DraftResult(val envelope: CampaignEnvelope, val draft: CharacterCreationDraftSnapshot, val created: Boolean)
    data class CommitResult(val envelope: CampaignEnvelope, val result: CharacterCreationResult, val stateChanged: Boolean)

    fun startDraft(envelope: CampaignEnvelope, draftId: String): DraftResult {
        require(draftId.isNotBlank())
        envelope.campaignState.game.creationDrafts[draftId]?.let {
            require(it.profile == envelope.ruleProfile) { "DRAFT_PROFILE_MISMATCH" }
            return DraftResult(envelope, it, false)
        }
        val rng = restoreRng(envelope.campaignState.game)
        val r = CharacterCreationRuntimeRules.rollAttributes { sides -> die(rng, sides) }
        val attrs = Attributes(r.strength, r.intelligence, r.wisdom, r.dexterity, r.constitution, r.charisma)
        val draft = CharacterCreationDraftSnapshot(draftId, envelope.ruleProfile, attrs, envelope.campaignState.game.revision)
        val game = envelope.campaignState.game.copy(
            creationDrafts = envelope.campaignState.game.creationDrafts + (draftId to draft),
            rngRootSeed = rng.snapshot().rootSeed.toString(),
            rngStreams = snapshotRng(rng.snapshot()),
            revision = envelope.campaignState.game.revision + 1
        )
        return DraftResult(envelope.copy(campaignState = envelope.campaignState.copy(game = game)), draft, true)
    }

    fun commit(
        envelope: CampaignEnvelope,
        draftId: String,
        characterId: String,
        name: String,
        method: CreationMethod,
        classIds: List<String>,
        raceId: String?,
        adjustments: List<AttributeAdjustment> = emptyList()
    ): CommitResult {
        require(draftId.isNotBlank()); require(characterId.isNotBlank()); require(name.isNotBlank())
        envelope.campaignState.game.characters[characterId]?.let { existing ->
            return CommitResult(envelope, CharacterCreationResult.Created(existing, RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, emptyList(), "IDEMPOTENT_EXISTING_CHARACTER")), false)
        }
        val draft = envelope.campaignState.game.creationDrafts[draftId]
            ?: return CommitResult(envelope, CharacterCreationResult.Rejected(CreationFailureCode.MISSING_EVIDENCE,"Creation draft not found",RuleTrace(EvidenceStatus.MISSING_EVIDENCE, emptyList(),"DRAFT_REQUIRED")), false)
        if (draft.profile != envelope.ruleProfile) {
            return CommitResult(envelope, CharacterCreationResult.Rejected(CreationFailureCode.PROFILE_METHOD_MISMATCH,"Draft profile differs from campaign profile",RuleTrace(EvidenceStatus.MISSING_EVIDENCE, emptyList(),"PROFILE_DRIFT_FORBIDDEN")), false)
        }
        val hitDice = resolveHitDice(envelope.ruleProfile, method, classIds)
            ?: return CommitResult(envelope, CharacterCreationResult.Rejected(CreationFailureCode.UNKNOWN_CLASS,"Unable to resolve class hit dice for active profile/method",RuleTrace(EvidenceStatus.MISSING_EVIDENCE, emptyList(),"NO_CROSS_PROFILE_FALLBACK")), false)
        val rng = restoreRng(envelope.campaignState.game)
        val hpRolls = hitDice.map { die(rng, it) }
        val req = CharacterCreationRequest(
            profile = envelope.ruleProfile, method = method, characterId = characterId, name = name,
            rolledAttributes = draft.rolledAttributes, classIds = classIds, raceId = raceId,
            adjustments = adjustments, hpRolls = hpRolls,
            optionIds = envelope.optionSet.optionIds.map(OptionIds::canonical).toSet()
        )
        val result = creator.create(req)
        if (result !is CharacterCreationResult.Created) return CommitResult(envelope, result, false)
        val c = result.character
        val startingWealth = CharacterCreationRuntimeRules.rollStartingWealthGp { sides -> die(rng, sides) }
        val wealthRule = when (envelope.ruleProfile) {
            RuleProfile.OSE_CLASSIC_FANTASY -> "CLASSIC_CHARACTER_CREATION:C-10"
            RuleProfile.OSE_ADVANCED_FANTASY -> if (method == CreationMethod.ADVANCED_METHOD)
                "ADVANCED_METHOD_CHARACTER_CREATION:STEP-11" else "ADVANCED_BASIC_CHARACTER_CREATION:STEP-11"
        }
        val loadout = CharacterLoadoutSnapshot(c.characterId, startingWealth, startingWealth, sourceRuleId = wealthRule)
        val old = envelope.campaignState.game
        val game = old.copy(
            characters = old.characters + (c.characterId to c),
            creationDrafts = old.creationDrafts - draftId,
            loadouts = old.loadouts + (c.characterId to loadout),
            // Additional recruits must not rewind an adventure already underway.
            session = if (envelope.campaignState.party.characterIds.isEmpty() &&
                old.session.visibleNarration.isBlank() && envelope.campaignState.time.turns == 0L)
                old.session.copy(phase = "CHARACTER_LOADOUT") else old.session,
            rngRootSeed = rng.snapshot().rootSeed.toString(), rngStreams = snapshotRng(rng.snapshot()),
            revision = old.revision + 1
        )
        val state = envelope.campaignState.copy(
            party = PartyState((envelope.campaignState.party.characterIds + c.characterId).distinct()), game = game
        )
        return CommitResult(envelope.copy(campaignState = state), result, true)
    }

    private fun resolveHitDice(profile: RuleProfile, method: CreationMethod, classIds: List<String>): List<Int>? = when (profile) {
        RuleProfile.OSE_CLASSIC_FANTASY -> if (method == CreationMethod.CLASSIC && classIds.size == 1) ClassicCatalog.classes[classIds.single()]?.let { listOf(it.hitDie) } else null
        RuleProfile.OSE_ADVANCED_FANTASY -> when (method) {
            CreationMethod.CLASSIC -> null
            CreationMethod.ADVANCED_BASIC -> if (classIds.size == 1) AdvancedCatalog.basicClasses[classIds.single()]?.let { listOf(it.hitDie) } else null
            CreationMethod.ADVANCED_METHOD -> classIds.map { AdvancedCatalog.basicClasses[it]?.hitDie ?: return null }
        }
    }

    private fun die(rng: NamedRngStreams, sides: Int): Int = (rng.draw(RngStreamId.RULES_DICE) % sides.toUInt()).toInt() + 1

    private fun restoreRng(game: GameExtensions): NamedRngStreams {
        if (game.rngStreams.isEmpty()) return NamedRngStreams.fromRootSeed(game.rngRootSeed.toULong())
        val streams = RngStreamId.entries.associateWith { id ->
            val s = game.rngStreams[id.name] ?: error("Missing persisted RNG stream ${id.name}")
            Pcg32State(s.state.toULong(), s.increment.toULong(), s.counter, s.version)
        }
        return NamedRngStreams.fromSeedState(SeedState(game.rngRootSeed.toULong(), streams))
    }
    private fun snapshotRng(seed: SeedState): Map<String, RngStateSnapshot> = seed.streams.mapKeys { it.key.name }
        .mapValues { (_, s) -> RngStateSnapshot(s.state.toString(), s.increment.toString(), s.counter, s.version) }
}
