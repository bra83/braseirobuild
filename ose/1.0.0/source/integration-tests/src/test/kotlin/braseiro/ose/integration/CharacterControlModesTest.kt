package braseiro.ose.integration

import braseiro.ose.backup.BackupCodec
import braseiro.ose.character.CharacterControlCoordinator
import braseiro.ose.character.CompanionJudgmentEngine
import braseiro.ose.character.CharacterPartyAdministration
import braseiro.ose.character.NativeCharacterCreationCoordinator
import braseiro.ose.model.*
import braseiro.ose.barbara.DeterministicBarbaraSupervisor
import braseiro.ose.persistence.api.*
import braseiro.ose.referee.RulesRefereeBoundary
import braseiro.ose.session.SessionEngine
import braseiro.ose.rules.api.CharacterCreationResult
import braseiro.ose.rules.api.CreationMethod
import braseiro.ose.testsupport.Fixtures
import kotlin.test.*

class CharacterControlModesTest {
    private class MemoryRepo : CampaignRepository {
        private val campaigns = mutableMapOf<String, CampaignEnvelope>()
        override fun create(envelope: CampaignEnvelope) {
            check(campaigns.putIfAbsent(envelope.campaignId.value, envelope) == null)
        }
        override fun load(campaignId: CampaignId): CampaignLoadResult =
            campaigns[campaignId.value]?.let { CampaignLoadResult.Loaded(it) }
                ?: CampaignLoadResult.NotFound(campaignId)
        override fun replace(envelope: CampaignEnvelope) { campaigns[envelope.campaignId.value] = envelope }
        override fun commit(campaignId: CampaignId, transition: StateTransition) {
            val current = campaigns.getValue(campaignId.value)
            campaigns[campaignId.value] = current.copy(campaignState = transition.updatedState)
        }
        override fun checkpoint(campaignId: CampaignId, checkpointId: String) { require(checkpointId.isNotBlank()) }
        override fun listCampaigns(): List<CampaignSummary> = campaigns.keys.sorted().map {
            CampaignSummary(CampaignId(it), false)
        }
        override fun archive(campaignId: CampaignId) { require(campaignId.value in campaigns) }
    }
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
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(e, "gm", true, null, "ataco", "ataco")
        }
        val gmProposed = CharacterControlCoordinator.propose(e, "master-1", "gm", "ataco", "ataco")
        CharacterControlCoordinator.requireActionAllowed(gmProposed, "gm", true, "master-1", "ataco", "ataco")
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
    @Test fun actualSessionEntryPointBlocksUnauthorizedAndReplaysApprovedOnce() {
        var e = party(RuleProfile.OSE_CLASSIC_FANTASY)
        e = CharacterControlCoordinator.setMode(e,"gm",CharacterControlMode.MASTER)
        e = CharacterControlCoordinator.setMode(e,"curated",CharacterControlMode.CURATED)
        val repo = MemoryRepo()
        repo.create(e)
        val session = SessionEngine(repo,RulesRefereeBoundary(),DeterministicBarbaraSupervisor())
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"blocked-player","player",
                "observa","observa",fromMaster=true)
        }
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"blocked-gm","gm",
                "observa","observa",fromMaster=false)
        }
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"blocked-curated","curated",
                "observa","observa",fromMaster=true)
        }
        val approvedPlayer = session.submitCharacterAction(
            e.campaignId,"player-one","player","escuto","escuto")
        assertTrue(approvedPlayer.narration.isNotBlank())
        var latest = session.loadSession(e.campaignId)
        latest = CharacterControlCoordinator.propose(latest,"curated-p1","curated","escuto","escuto")
        repo.replace(latest)
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"master-action:curated-p1",
                "curated","escuto","escuto",fromMaster=true,
                approvedProposalId="curated-p1")
        }
        latest = CharacterControlCoordinator.review(latest,"curated-p1",true)
        repo.replace(latest)
        val authorized = session.submitCharacterAction(e.campaignId,"master-action:curated-p1",
            "curated","escuto","escuto",fromMaster=true,
            approvedProposalId="curated-p1")
        val consumed = session.loadSession(e.campaignId).campaignState.game
            .characterActionProposals.getValue("curated-p1")
        assertEquals(CharacterActionApproval.EXECUTED, consumed.status)
        assertEquals("master-action:curated-p1", consumed.executedReactionId)
        val saved = CanonicalStateHash.sha256(session.loadSession(e.campaignId))
        val duplicate = session.submitCharacterAction(e.campaignId,"master-action:curated-p1",
            "curated","escuto","escuto",fromMaster=true,
            approvedProposalId="curated-p1")
        assertEquals(authorized.narration,duplicate.narration)
        assertEquals(saved,CanonicalStateHash.sha256(session.loadSession(e.campaignId)))
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"master-action:curated-p1-again",
                "curated","escuto","escuto",fromMaster=true,approvedProposalId="curated-p1")
        }
        assertFailsWith<IllegalArgumentException> {
            session.submitPlayerReaction(e.campaignId,"legacy-bypass","observa","WAIT_TURN")
        }
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"player-one","player","escuto","WAIT_TURN")
        }
        assertEquals(saved,CanonicalStateHash.sha256(session.loadSession(e.campaignId)))
        val restored=BackupCodec.importCampaign(BackupCodec.exportCampaign(session.loadSession(e.campaignId)))
        assertEquals(consumed,restored.campaignState.game.characterActionProposals.getValue("curated-p1"))
    }

    @Test fun masterCannotBypassProposalsOrCommandMixedGroupViaTypedAction() {
        var e=party(RuleProfile.OSE_ADVANCED_FANTASY)
        e=CharacterControlCoordinator.setMode(e,"gm",CharacterControlMode.MASTER)
        e=CharacterControlCoordinator.setMode(e,"curated",CharacterControlMode.CURATED)
        val repo=MemoryRepo()
        repo.create(e)
        val session=SessionEngine(repo,RulesRefereeBoundary(),DeterministicBarbaraSupervisor())
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"gm-bypass","gm","observo","observo",fromMaster=true)
        }
        val proposed=CharacterControlCoordinator.propose(e,"gm-global-proposal","gm","descanso","WAIT_TURN")
        repo.replace(proposed)
        assertFailsWith<IllegalArgumentException> {
            session.submitCharacterAction(e.campaignId,"gm-global","gm","descanso","WAIT_TURN",
                fromMaster=true,approvedProposalId="gm-global-proposal")
        }
        assertEquals(e.campaignState.time,session.loadSession(e.campaignId).campaignState.time)
        assertEquals(e.campaignState.resources,session.loadSession(e.campaignId).campaignState.resources)
    }

    @Test fun companionOpinionsAreReadOnlyGroundedDistinctAndNeverExposeUnknownFacts() {
        for (profile in RuleProfile.entries) {
            var e = party(profile)
            e = CharacterControlCoordinator.setMode(e, "gm", CharacterControlMode.MASTER)
            e = CharacterControlCoordinator.setMode(e, "curated", CharacterControlMode.CURATED)
            val before = CanonicalStateHash.sha256(e)
            val opinions = CompanionJudgmentEngine.opinions(e)
            assertEquals(setOf("gm", "curated"), opinions.map { it.characterId }.toSet())
            assertEquals(before, CanonicalStateHash.sha256(e))
            opinions.forEach { opinion ->
                assertTrue(opinion.perception.isNotBlank())
                assertTrue(opinion.hypothesis.isNotBlank())
                assertTrue(opinion.motivation.isNotBlank())
                assertTrue(opinion.risk.isNotBlank())
                assertTrue(opinion.reason.isNotBlank())
                assertTrue(opinion.suggestedAction.isNotBlank())
                assertTrue(opinion.stance in setOf("SUPPORT", "CAUTION", "OBJECT", "UNCERTAIN"))
                assertFalse(opinion.perception.contains("segredo", ignoreCase = true))
            }
            val gm = opinions.first { it.characterId == "gm" }
            val proposal = CharacterControlCoordinator.propose(e, "cognition-${profile.name}",
                "gm", gm.suggestedAction, gm.mechanicalIntent, gm)
            val record = proposal.campaignState.game.characterActionProposals.getValue("cognition-${profile.name}")
            assertEquals(gm.perception, record.perception)
            assertEquals(gm.hypothesis, record.hypothesis)
            assertEquals(gm.risk, record.risk)
            assertEquals(gm.motivation, record.motivation)
            assertEquals(e.campaignState.time, proposal.campaignState.time)
            assertEquals(e.campaignState.position, proposal.campaignState.position)
            assertEquals(e.campaignState.resources, proposal.campaignState.resources)
            assertEquals(e.campaignState.game.rngStreams, proposal.campaignState.game.rngStreams)
            val restored = BackupCodec.importCampaign(BackupCodec.exportCampaign(proposal))
            assertEquals(record, restored.campaignState.game.characterActionProposals.getValue("cognition-${profile.name}"))
        }
    }

    @Test fun disagreementCannotForceMovementAndCuratorRiskPrecedesApproval() {
        var e=party(RuleProfile.OSE_CLASSIC_FANTASY)
        e=CharacterControlCoordinator.setMode(e,"gm",CharacterControlMode.MASTER)
        e=CharacterControlCoordinator.setMode(e,"curated",CharacterControlMode.CURATED)
        val c=CompanionJudgmentEngine.assess(e,"curated")
        val before=CanonicalStateHash.sha256(e)
        val pending=CharacterControlCoordinator.propose(e,"disagree","curated",c.suggestedAction,c.mechanicalIntent,c)
        assertEquals(CharacterActionApproval.PENDING,
            pending.campaignState.game.characterActionProposals.getValue("disagree").status)
        assertTrue(pending.campaignState.game.characterActionProposals.getValue("disagree").risk.isNotBlank())
        assertEquals(e.campaignState.time,pending.campaignState.time)
        assertEquals(before,CanonicalStateHash.sha256(e))
        assertFailsWith<IllegalArgumentException> {
            CharacterControlCoordinator.requireActionAllowed(pending,"curated",true,"disagree",
                c.suggestedAction,c.mechanicalIntent)
        }
        val refused=CharacterControlCoordinator.review(pending,"disagree",false)
        assertEquals(CharacterActionApproval.REJECTED,
            refused.campaignState.game.characterActionProposals.getValue("disagree").status)
        assertEquals(e.campaignState.time,refused.campaignState.time)
        assertEquals(e.campaignState.resources,refused.campaignState.resources)
    }

}
