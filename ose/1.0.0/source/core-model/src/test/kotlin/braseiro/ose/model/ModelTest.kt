package braseiro.ose.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class ModelTest {
    private fun envelope(created: String, options: List<String>) = CampaignEnvelope(
        campaignId = CampaignId("c1"), createdAtMetadata = created, ruleProfile = RuleProfile.OSE_CLASSIC_FANTASY,
        authorityRevision = AuthorityRevision("AUTH.CLASSIC", "v1", "h"), optionSet = OptionSet(options),
        generatorRegistry = GeneratorRegistry(listOf(GeneratorVersion("g", 1))), assetManifestRevision = AssetManifestRevision("m", "v", "h2"),
        campaignState = CampaignState(PartyState(listOf("b", "a")), TimeState(), PositionState(SpatialRef.Hex("w", 1, 2)), playerKnowledge = PlayerKnowledgeState(listOf("z", "a")))
    )
    @Test fun `mechanical hash excludes timestamp and canonicalizes shells`() {
        assertEquals(CanonicalStateHash.sha256(envelope("a", listOf("B", "A"))), CanonicalStateHash.sha256(envelope("b", listOf("A", "B"))))
    }
    @Test fun `envelope serialization roundtrip preserves tagged profile and spatial type`() {
        val json = Json { classDiscriminator = "kind"; encodeDefaults = true }
        val source = envelope("fixed", listOf("A")); val decoded = json.decodeFromString<CampaignEnvelope>(json.encodeToString(source))
        assertEquals(source, decoded); assertEquals(RuleProfile.OSE_CLASSIC_FANTASY, decoded.ruleProfile); assertIs<SpatialRef.Hex>(decoded.campaignState.position.primary)
    }
}
