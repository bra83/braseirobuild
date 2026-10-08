package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.character.CharacterPartyAdministration
import braseiro.ose.character.NativeCharacterCreationCoordinator
import braseiro.ose.character.NativeCharacterLoadoutCoordinator
import braseiro.ose.model.*
import braseiro.ose.rules.api.CreationMethod
import braseiro.ose.rules.api.CharacterCreationResult
import braseiro.ose.testsupport.Fixtures
import kotlin.test.*

class MultipleCharacterCampaignTest {
    private val creator = NativeCharacterCreationCoordinator()
    private val equipment = NativeCharacterLoadoutCoordinator()

    private fun base(profile: RuleProfile): CampaignEnvelope {
        val campaign = Fixtures.campaign("party-${profile.name}", profile)
        return campaign.copy(
            optionSet = OptionSet(listOf("OPT.ENCUMBRANCE=BASIC")),
            campaignState = campaign.campaignState.copy(
                party = PartyState(),
                resources = ResourceState(),
                game = GameExtensions(rngRootSeed = "541")
            )
        )
    }

    private fun recruit(envelope: CampaignEnvelope, id: String): CampaignEnvelope {
        val draft = creator.startDraft(envelope, "draft-$id")
        val method = if (envelope.ruleProfile == RuleProfile.OSE_CLASSIC_FANTASY)
            CreationMethod.CLASSIC else CreationMethod.ADVANCED_BASIC
        val committed = creator.commit(draft.envelope, "draft-$id", id, "Hero-$id",
            method, listOf("FIGHTER"), null)
        assertIs<CharacterCreationResult.Created>(committed.result)
        assertTrue(committed.stateChanged)
        return committed.envelope
    }

    @Test fun additionalRecruitmentPreservesActiveCampaignClassicAndAdvanced() {
        for (profile in RuleProfile.entries) {
            val first = recruit(base(profile), "first")
            val finished = equipment.finish(first, "first")
            assertTrue(finished.changed)
            val session = finished.envelope.campaignState.game.session.copy(
                phase = "ACTIVE", visibleNarration = "A floresta está viva."
            )
            val active = finished.envelope.copy(campaignState = finished.envelope.campaignState.copy(
                time = TimeState(turns = 23L),
                resources = ResourceState(listOf("rations:7", "gold:20")),
                game = finished.envelope.campaignState.game.copy(session = session)
            ))
            val second = recruit(active, "second")
            assertEquals(listOf("first", "second"), second.campaignState.party.characterIds)
            assertEquals("ACTIVE", second.campaignState.game.session.phase)
            assertEquals(session.visibleNarration, second.campaignState.game.session.visibleNarration)
            val secondFinished = equipment.finish(second, "second")
            assertTrue(secondFinished.changed)
            assertEquals("LOADOUT_COMPLETE", secondFinished.status)
            assertEquals(active.campaignState.time, secondFinished.envelope.campaignState.time)
            assertEquals(active.campaignState.resources, secondFinished.envelope.campaignState.resources)
            assertEquals("ACTIVE", secondFinished.envelope.campaignState.game.session.phase)
            assertEquals(session.visibleNarration, secondFinished.envelope.campaignState.game.session.visibleNarration)
            assertTrue(secondFinished.envelope.campaignState.game.inventory.items.values.any { it.ownerId == "second" })
        }
    }

    @Test fun deletingMiddleAndLastMemberKeepsWorldHistoryAndOtherInventories() {
        var envelope = base(RuleProfile.OSE_CLASSIC_FANTASY)
        for (id in listOf("first", "middle", "last")) {
            envelope = recruit(envelope, id)
            envelope = equipment.finish(envelope, id).envelope
        }
        envelope = envelope.copy(campaignState = envelope.campaignState.copy(
            time = TimeState(turns = 35),
            resources = ResourceState(listOf("rations:5", "torch:2")),
            game = envelope.campaignState.game.copy(
                experience = ExperienceRuntimeSnapshot(recipients =
                    listOf("first", "middle", "last").associateWith { id ->
                        XpRecipientSnapshot(id, RuleProfile.OSE_CLASSIC_FANTASY,
                            classIds = listOf("FIGHTER"),
                            xpByClass = mapOf("FIGHTER" to if (id == "middle") 200L else 100L))
                    }
                ),
                session = envelope.campaignState.game.session.copy(
                    phase = "ACTIVE", visibleNarration = "A vigília prossegue."
                )
            )
        ))
        val before = envelope.campaignState
        val middleRemoved = CharacterPartyAdministration.delete(envelope, "middle", "delete-middle")
        val after = middleRemoved.campaignState
        assertEquals(listOf("first", "last"), after.party.characterIds)
        assertEquals(before.time, after.time)
        assertEquals(before.position, after.position)
        assertEquals(before.resources, after.resources)
        assertEquals(before.game.session, after.game.session)
        assertEquals(before.game.npcs, after.game.npcs)
        assertEquals(before.game.worldEvents, after.game.worldEvents)
        assertTrue("ARCHIVED_DELETED" in after.game.characters.getValue("middle").flags)
        assertEquals(200L, after.game.experience.recipients.getValue("middle").xpByClass.getValue("FIGHTER"))
        assertFalse(after.game.experience.recipients.getValue("middle").surviving)
        assertTrue(after.game.experience.recipients.getValue("first").surviving)
        assertTrue(after.game.experience.recipients.getValue("last").surviving)
        assertFalse(after.game.inventory.items.values.any { it.ownerId == "middle" })
        assertTrue(after.game.inventory.items.values.any { it.ownerId == "first" })
        assertTrue(after.game.inventory.items.values.any { it.ownerId == "last" })
        assertFailsWith<IllegalArgumentException> {
            CharacterPartyAdministration.delete(middleRemoved, "middle", "delete-again")
        }

        val withoutFirst = CharacterPartyAdministration.delete(middleRemoved, "first", "delete-first")
        val emptyParty = CharacterPartyAdministration.delete(withoutFirst, "last", "delete-last")
        assertTrue(emptyParty.campaignState.party.characterIds.isEmpty())
        assertEquals(before.time, emptyParty.campaignState.time)
        assertEquals(before.game.session, emptyParty.campaignState.game.session)
        val addedAgain = recruit(emptyParty, "replacement")
        assertEquals(listOf("replacement"), addedAgain.campaignState.party.characterIds)
        assertEquals("ACTIVE", addedAgain.campaignState.game.session.phase)
        val backup = BackupCodec.exportCampaign(addedAgain, "replacement")
        assertEquals("replacement", BackupCodec.selectedCharacterId(backup))
        val restored = BackupCodec.importCampaign(backup)
        assertEquals(CanonicalStateHash.sha256(addedAgain), CanonicalStateHash.sha256(restored))
        assertTrue("ARCHIVED_DELETED" in restored.campaignState.game.characters.getValue("middle").flags)
    }
}
