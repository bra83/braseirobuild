package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.character.CharacterControlCoordinator
import braseiro.ose.character.CharacterPartyAdministration
import braseiro.ose.character.NativeCharacterCreationCoordinator
import braseiro.ose.model.*
import braseiro.ose.rules.api.CharacterCreationResult
import braseiro.ose.rules.api.CreationMethod
import braseiro.ose.testsupport.Fixtures
import kotlin.test.*

class CharacterControlModesTest {
    private fun party(profile: RuleProfile): CampaignEnvelope {
        val basic = Fixtures.campaign("control-${profile.name}", profile)
        var e = basic.copy(
            optionSet = OptionSet(listOf("OPT.ENCUMBRANCE=BASIC")),
            campaignState = basic.campaignState.copy(
                party = PartyState(),
                game = GameExtensions(rngRootSeed = "541")
            )
        )
        val creator = NativeCharacterCreationCoordinator()
        for (id in listOf("player", "gm", "curated")) {
            val draft = creator.startDraft(e, "draft-$id")
            val committed = creator.commit(draft.envelope, "draft-$id", id,
                "Hero-$id", if (profile == RuleProfile.OSE_CLASSIC_FANTASY)
                    CreationMethod.CLASSIC else CreationMethod.ADVANCED_BASIC,
                listOf("FIGHTER"), null)
            assertIs<CharacterCreationResult.Created>(committed.result)
            e = committed.envelope
        }
        return e
    }

    @Test fun modesAreIndependentAndBackwardsCompatible() {
        for (profile in RuleProfile.entries) {
            var e = party(profile)
            assertEquals(CharacterControlMode.PLAYER, CharacterControlCoordinator.mode(e, "player"))
            assertEquals(CharacterControlMode.PLAYER, CharacterControlCoordinator.mode(e, "gm"))
            e = CharacterControlCoordinator.setMode(e, "gm", CharacterControlMode.MASTER)
            e = CharacterControlCoordinator.setMode(e, "curated", CharacterControlMode.CURATED)
            assertEquals(CharacterControlMode.PLAYER, CharacterControlCoordinator.mode(e, "player"))
            assertEquals(CharacterControlMode.MASTER, CharacterControlCoordinator.mode(e, "gm"))
            assertEquals(CharacterControlMode.CURATED, CharacterControlCoordinator.mode(e, "curated"))
            val restored = BackupCodec.importCampaign(BackupCodec.exportCampaign(e))
            assertEquals(e.campaignState.game.characterControlModes, restored.campaignState.game.characterControlModes)
            assertEquals(e.campaignState.time, restored.campaignState.time)
            assertEquals(e.campaignState.position, restored.campaignState.position)
            assertEquals(e.campaignState.resources, restored.campaignState.resources)
            assertEquals(e.campaignState.game.inventory, restored.campaignState.game.inventory)
            assertEquals(e.campaignState.game.rngStreams, restored.campaignState.game.rngStreams)
            assertEquals(e, CharacterControlCoordinator.setMode(e, "gm", CharacterControlMode.MASTER))
        }
    }

    @Test fun unauthorizedMasterAndPlayerActionsCannotCrossAuthority() {
        val e = CharacterControlCoordinator.setMode(
            CharacterControlCoordinator.setMode(party(RuleProfile.OSE_CLASSIC_FANTASY),
                "gm", CharacterControlMode.MASTER),
            "curated", CharacterControlMode.CURATED
        )
        CharacterControlCoordinator.requireActionAllowed(e, "player", false, null, "escuto", "escuto")
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "player", true, null, "escuto", "escuto")
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "gm", false, null, "ataco", "ataco")
        }
        CharacterControlCoordinator.requireActionAllowed(e, "gm", true, null, "ataco", "ataco")
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "gm", true, null,
                "levo o grupo", "WILD_TRAVEL_DAY:E")
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "gm", true, null,
                "mudo a ficha", "XP_LEVEL_UP:player:FIGHTER")
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.propose(e, "p-bad", "player", "escuto", "escuto")
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "curated", true, null, "observo", "observo")
        }
    }

    @Test fun curatedActionsArePendingUntilApprovedOrVetoedAndEditingIsBound() {
        var e = CharacterControlCoordinator.setMode(
            party(RuleProfile.OSE_ADVANCED_FANTASY), "curated", CharacterControlMode.CURATED)
        e = CharacterControlCoordinator.propose(e, "proposal-1", "curated", "abro a porta", "OPEN_DOOR")
        assertEquals(CharacterActionApproval.PENDING,
            e.campaignState.game.characterActionProposals.getValue("proposal-1").status)
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "curated", true,
                "proposal-1", "abro a porta", "OPEN_DOOR")
        }
        val before=e.campaignState
        e=CharacterControlCoordinator.review(e,"proposal-1",true,"escuto à porta","escuto à porta")
        assertEquals(before.time,e.campaignState.time)
        assertEquals(before.resources,e.campaignState.resources)
        assertEquals(before.game.session,e.campaignState.game.session)
        assertEquals(before.game.rngStreams,e.campaignState.game.rngStreams)
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e,"curated",true,
                "proposal-1","abro a porta","OPEN_DOOR")
        }
        CharacterControlCoordinator.requireActionAllowed(e,"curated",true,
            "proposal-1","escuto à porta","escuto à porta")
        e=CharacterControlCoordinator.markExecuted(e,"proposal-1")
        assertEquals(CharacterActionApproval.EXECUTED,
            e.campaignState.game.characterActionProposals.getValue("proposal-1").status)
        assertEquals(e,CharacterControlCoordinator.markExecuted(e,"proposal-1"))
        val veto=CharacterControlCoordinator.propose(e,"proposal-2","curated","ataco","ATTACK")
        val rejected=CharacterControlCoordinator.review(veto,"proposal-2",false)
        assertEquals(CharacterActionApproval.REJECTED,
            rejected.campaignState.game.characterActionProposals.getValue("proposal-2").status)
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(rejected,"curated",true,
                "proposal-2","ataco","ATTACK")
        }
        assertEquals(CanonicalStateHash.sha256(rejected),
            CanonicalStateHash.sha256(BackupCodec.importCampaign(BackupCodec.exportCampaign(rejected))))
    }

    @Test fun modeChangesAndCharacterDeletionRevokeUnapprovedActions() {
        var e=CharacterControlCoordinator.setMode(
            party(RuleProfile.OSE_CLASSIC_FANTASY),"curated",CharacterControlMode.CURATED)
        e=CharacterControlCoordinator.propose(e,"p-before","curated","avanço","WILD_TRAVEL_DAY:E")
        e=CharacterControlCoordinator.setMode(e,"curated",CharacterControlMode.PLAYER)
        assertEquals(CharacterActionApproval.REJECTED,
            e.campaignState.game.characterActionProposals.getValue("p-before").status)
        e=CharacterControlCoordinator.setMode(e,"curated",CharacterControlMode.CURATED)
        e=CharacterControlCoordinator.propose(e,"p-delete","curated","ataco","ATTACK")
        e=CharacterPartyAdministration.delete(e,"curated","delete-c")
        assertFalse("curated" in e.campaignState.game.characterControlModes)
        assertEquals(CharacterActionApproval.REJECTED,
            e.campaignState.game.characterActionProposals.getValue("p-delete").status)
    }
}
