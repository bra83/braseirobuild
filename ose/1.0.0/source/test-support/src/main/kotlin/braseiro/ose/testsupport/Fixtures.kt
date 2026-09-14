package braseiro.ose.testsupport

import braseiro.ose.model.*

object Fixtures {
    fun campaign(id: String = "campaign-fixture", profile: RuleProfile = RuleProfile.OSE_CLASSIC_FANTASY, createdAt: String = "2026-09-01T00:00:00Z") = CampaignEnvelope(
        campaignId = CampaignId(id), createdAtMetadata = createdAt, ruleProfile = profile,
        authorityRevision = AuthorityRevision(if (profile == RuleProfile.OSE_CLASSIC_FANTASY) "AUTH.CLASSIC" else "AUTH.ADVANCED", "fixture-v1", "authority-fixture-hash"),
        optionSet = OptionSet(listOf("OPT.B", "OPT.A")),
        generatorRegistry = GeneratorRegistry(listOf(GeneratorVersion("FOUNDATION", 1))),
        assetManifestRevision = AssetManifestRevision("BRASEIRO_VISUAL_FORGE_ASSET_MANIFEST_V2", "fixture", "manifest-fixture-hash"),
        campaignState = CampaignState(PartyState(listOf("char-b", "char-a")), TimeState(), PositionState(SpatialRef.Hex("world-1", 0, 0)), ResourceState(listOf("torch:6", "ration:7")), PlayerKnowledgeState(listOf("hex:0,0")))
    )
}
