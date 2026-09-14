package braseiro.ose.app

import braseiro.ose.model.*
import org.junit.Assert.*
import org.junit.Test

class ContextualDecisionSuggesterTest {
    private fun envelope(phase: String, narration: String, position: SpatialRef): CampaignEnvelope = CampaignEnvelope(
        campaignId = CampaignId("test"), createdAtMetadata = "test",
        ruleProfile = RuleProfile.OSE_CLASSIC_FANTASY,
        authorityRevision = AuthorityRevision("ose", "test", "test"), optionSet = OptionSet(),
        generatorRegistry = GeneratorRegistry(), assetManifestRevision = AssetManifestRevision("test", "test", "test"),
        campaignState = CampaignState(
            party = PartyState(), time = TimeState(), position = PositionState(position),
            game = GameExtensions(session = SessionSnapshot(phase = phase, visibleNarration = narration))
        )
    )

    @Test fun alwaysReturnsExactlyThreeDistinctVisibleStateSuggestions() {
        val e = envelope("ACTIVE", "O caminho segue pelas colinas.", SpatialRef.Hex("world", 1, 2))
        val first = ContextualDecisionSuggester.suggest(e)
        val second = ContextualDecisionSuggester.suggest(e)
        assertEquals(3, first.size)
        assertEquals(3, first.map { it.label }.distinct().size)
        assertEquals(first, second)
        assertTrue(first.all { it.label.isNotBlank() && it.reaction.isNotBlank() })
    }

    @Test fun combatSuggestionsNeverMutateAnything() {
        val e = envelope("COMBAT", "Uma ameaça visível bloqueia a passagem.", SpatialRef.Scene("scene", "1"))
        val before = e.canonicalMechanical()
        val suggestions = ContextualDecisionSuggester.suggest(e)
        assertEquals(before, e.canonicalMechanical())
        assertEquals(3, suggestions.size)
    }
}
