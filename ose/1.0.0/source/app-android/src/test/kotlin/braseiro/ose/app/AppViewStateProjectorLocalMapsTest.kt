package braseiro.ose.app

import braseiro.ose.model.*
import org.junit.Assert.*
import org.junit.Test

class AppViewStateProjectorLocalMapsTest {
    private fun envelope(position: SpatialRef, game: GameExtensions): CampaignEnvelope = CampaignEnvelope(
        campaignId = CampaignId("maps-test"),
        createdAtMetadata = "test",
        ruleProfile = RuleProfile.OSE_CLASSIC_FANTASY,
        authorityRevision = AuthorityRevision("ose", "test", "test"),
        optionSet = OptionSet(),
        generatorRegistry = GeneratorRegistry(),
        assetManifestRevision = AssetManifestRevision("test", "test", "test"),
        campaignState = CampaignState(
            party = PartyState(),
            time = TimeState(),
            position = PositionState(position),
            game = game
        )
    )

    @Test fun settlementProjectionHidesUndiscoveredLocationIdentity() {
        val settlement = SettlementSnapshot(
            settlementId = "town",
            packageVersion = 1,
            locations = listOf(
                SettlementLocationSnapshot("INN", "Estalagem", discovered = true, tags = setOf("SERVICE")),
                SettlementLocationSnapshot("TEMPLE", "Templo Secreto", discovered = false, tags = setOf("SERVICE"))
            )
        )
        val view = AppViewStateProjector.project(
            envelope(SpatialRef.Settlement("town", "INN"), GameExtensions(settlement = settlement))
        )
        val local = view.getJSONObject("localMaps").getJSONObject("settlement")
        assertEquals("INN", local.getString("currentAnchorId"))
        val locations = local.getJSONArray("locations")
        val known = locations.getJSONObject(0)
        val hidden = locations.getJSONObject(1)
        assertEquals("Estalagem", known.getString("name"))
        assertEquals("Local desconhecido", hidden.getString("name"))
        assertEquals(0, hidden.getJSONArray("tags").length())
    }

    @Test fun dungeonProjectionExposesOnlyKnownNodesAndKnownEdges() {
        val dungeon = DungeonSnapshot(
            dungeonId = "D",
            generatorVersion = 1,
            seed = 42u,
            entranceNodeId = "R00",
            nodes = listOf(
                DungeonNodeSnapshot("R00", "ENTRANCE"),
                DungeonNodeSnapshot("R01", "ROOM"),
                DungeonNodeSnapshot("R02", "ROOM")
            ),
            edges = listOf(
                DungeonEdgeSnapshot("R00", "R01"),
                DungeonEdgeSnapshot("R01", "R02")
            ),
            knownNodeIds = setOf("R00", "R01"),
            visibleNodeIds = setOf("R00", "R01")
        )
        val view = AppViewStateProjector.project(
            envelope(SpatialRef.Dungeon("D", "R01"), GameExtensions(dungeon = dungeon))
        )
        val local = view.getJSONObject("localMaps").getJSONObject("dungeon")
        assertEquals("R01", local.getString("currentNodeId"))
        assertEquals(2, local.getJSONArray("nodes").length())
        assertEquals(1, local.getJSONArray("edges").length())
        assertFalse(local.toString().contains("R02"))
    }
}
