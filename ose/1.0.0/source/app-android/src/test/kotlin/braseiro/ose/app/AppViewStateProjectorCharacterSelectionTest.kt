package braseiro.ose.app

import braseiro.ose.character.CharacterCreator
import braseiro.ose.model.*
import braseiro.ose.rules.api.*
import org.junit.Assert.*
import org.junit.Test

class AppViewStateProjectorCharacterSelectionTest {
    private fun createCharacter(id: String, name: String): CharacterSnapshot {
        val result = CharacterCreator().create(CharacterCreationRequest(
            profile = RuleProfile.OSE_CLASSIC_FANTASY,
            method = CreationMethod.CLASSIC,
            characterId = id,
            name = name,
            rolledAttributes = Attributes(15,10,10,12,13,9),
            classIds = listOf("FIGHTER"),
            hpRolls = listOf(7)
        ))
        return (result as CharacterCreationResult.Created).character
    }

    @Test fun activeSheetProjectsRequestedIdAndFallsBackOnlyWhenInvalid() {
        val first = createCharacter("a", "Primeiro")
        val second = createCharacter("b", "Segundo")
        val envelope = CampaignEnvelope(
            campaignId = CampaignId("selection-test"),
            createdAtMetadata = "test",
            ruleProfile = RuleProfile.OSE_CLASSIC_FANTASY,
            authorityRevision = AuthorityRevision("ose", "test", "test"),
            optionSet = OptionSet(),
            generatorRegistry = GeneratorRegistry(),
            assetManifestRevision = AssetManifestRevision("test", "test", "test"),
            campaignState = CampaignState(
                party = PartyState(listOf("a", "b")),
                time = TimeState(),
                position = PositionState(SpatialRef.Hex("world", 0, 0)),
                game = GameExtensions(characters = mapOf("a" to first, "b" to second))
            )
        )
        val selected = AppViewStateProjector.project(envelope, "b")
        assertEquals(2, selected.getJSONArray("party").length())
        assertEquals("b", selected.getString("selectedCharacterId"))
        assertEquals("Segundo", selected.getJSONObject("selectedCharacter").getString("name"))
        val fallback = AppViewStateProjector.project(envelope, "missing")
        assertEquals("a", fallback.getString("selectedCharacterId"))
        assertEquals("Primeiro", fallback.getJSONObject("selectedCharacter").getString("name"))
    }
}
