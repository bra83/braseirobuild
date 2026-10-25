package braseiro.ose.session

import braseiro.ose.barbara.BarbaraSupervisorPort
import braseiro.ose.character.CharacterControlCoordinator
import braseiro.ose.model.*
import braseiro.ose.persistence.api.*
import braseiro.ose.referee.RefereeResolutionPort
import braseiro.ose.rng.*
import braseiro.ose.rules.api.*
import braseiro.ose.world.WorldEventScheduler
import kotlinx.serialization.Serializable

@Serializable data class SessionResult(
    val campaignId: String,
    val narration: String,
    val feedback: String,
    val mechanicalMutation: Boolean,
    val trace: RuleTrace
)

@Serializable data class GMHelpResult(val answer: String, val stateRevision: Long)

class SessionEngine(
    private val repository: CampaignRepository,
    private val referee: RefereeResolutionPort,
    private val barbara: BarbaraSupervisorPort
) {
    /**
     * The only supported per-character action entrypoint. The caller cannot impersonate
     * player-controlled characters or execute curated GM actions before authorization.
     * The existing idempotent reaction receipt is the executor/rollback boundary.
     */
    @Synchronized
    fun submitCharacterAction(
        campaignId: CampaignId,
        reactionId: String,
        characterId: String,
        reaction: String,
        mechanicalIntent: String,
        fromMaster: Boolean = false,
        approvedProposalId: String? = null
    ): SessionResult {
        require(reactionId.isNotBlank() && reaction.isNotBlank() && mechanicalIntent.isNotBlank())
        val current = load(campaignId)
        require(characterId in current.campaignState.party.characterIds) { "UNKNOWN_CHARACTER_ID" }
        val character = current.campaignState.game.characters.getValue(characterId)
        val labelledReaction = "${character.name}: ${reaction.trim()}"
        val receipt = current.campaignState.game.session.reactionReceipts[reactionId]
        val proposal = approvedProposalId?.let {
            current.campaignState.game.characterActionProposals[it] ?: error("UNKNOWN_PROPOSAL")
        }
        // An executed proposal permits only the identical receipt to resume
        // interrupted narration; it cannot authorize a second mechanical action.
        if (receipt != null && fromMaster && proposal != null &&
            CharacterControlCoordinator.mode(current, characterId) != CharacterControlMode.PLAYER &&
            proposal.status == CharacterActionApproval.EXECUTED &&
            proposal.executedReactionId == reactionId &&
            proposal.characterId == characterId &&
            proposal.reactionText == reaction && proposal.mechanicalIntent == mechanicalIntent) {
            return submitPlayerReactionInternal(
                campaignId, reactionId, labelledReaction, mechanicalIntent, true, null, characterId
            )
        }
        require(receipt == null || !fromMaster || proposal == null) {
            "MASTER_PROPOSAL_RECEIPT_COLLISION"
        }
        CharacterControlCoordinator.requireActionAllowed(
            current, characterId, fromMaster, approvedProposalId, reaction, mechanicalIntent
        )
        require(!fromMaster || !approvedProposalId.isNullOrBlank()) {
            "MASTER_ACTION_REQUIRES_PROPOSAL"
        }
        return submitPlayerReactionInternal(
            campaignId, reactionId, labelledReaction, mechanicalIntent, true, approvedProposalId, characterId
        )
    }

    @Synchronized
    fun submitPlayerReaction(campaignId: CampaignId, reaction: String): SessionResult {
        val current = load(campaignId)
        val legacyId = "legacy-${current.campaignState.game.revision + 1}"
        return submitPlayerReaction(campaignId, legacyId, reaction)
    }

    @Synchronized
    fun submitPlayerReaction(campaignId: CampaignId, reactionId: String, reaction: String): SessionResult =
        submitPlayerReaction(campaignId, reactionId, reaction, reaction)

    @Synchronized
    fun submitPlayerReaction(campaignId: CampaignId, reactionId: String, reaction: String, mechanicalIntent: String): SessionResult =
        submitPlayerReactionInternal(campaignId, reactionId, reaction, mechanicalIntent, false, null, null)

    private fun submitPlayerReactionInternal(
        campaignId: CampaignId,
        reactionId: String,
        reaction: String,
        mechanicalIntent: String,
        characterAuthorized: Boolean,
        approvedProposalId: String?,
        actorCharacterId: String?
    ): SessionResult {
        require(reactionId.isNotBlank())
        require(reaction.isNotBlank())
        require(mechanicalIntent.isNotBlank())
        // Reject spoofed employer identities before reading receipt or consuming RNG.
        ActionAuthorityPolicy.requireBoundActor(mechanicalIntent, actorCharacterId)
        val loaded = load(campaignId)
        if (!characterAuthorized) {
            require(loaded.campaignState.party.characterIds.none { id ->
                loaded.campaignState.game.characterControlModes[id] in
                    setOf(CharacterControlMode.MASTER, CharacterControlMode.CURATED)
            }) { "CHARACTER_ACTION_ENTRYPOINT_REQUIRED" }
        }
        val existing = loaded.campaignState.game.session.reactionReceipts[reactionId]
        if (existing != null) {
            require(existing.reactionText == reaction &&
                (existing.mechanicalIntent == null || existing.mechanicalIntent == mechanicalIntent) &&
                (existing.actorCharacterId == null || existing.actorCharacterId == actorCharacterId)) {
                "REACTION_ID_COLLISION:$reactionId"
            }
            val trace = traceFromReceipt(existing, loaded.ruleProfile)
            val existingNarration=existing.narration
            if (existingNarration != null) {
                return SessionResult(campaignId.value, existingNarration, existing.mechanicalFeedback, existing.mechanicalMutation, trace)
            }
            return finishNarration(loaded, reactionId, reaction, existing, trace)
        }

        val rng=restoreRng(loaded.campaignState.game)
        val outcome=referee.resolve(loaded,mechanicalIntent,rng)
        val evidenceRefs=outcome.trace.evidence.map{it.ref()}
        val beforeGameRaw=outcome.updatedState.game
        val beforeTurn=loaded.campaignState.time.turns
        val afterTurn=outcome.updatedState.time.turns
        val beforeGame=if(afterTurn>beforeTurn) WorldEventScheduler.advance(beforeGameRaw,beforeTurn,afterTurn) else beforeGameRaw
        // Only the referee's committed time change may classify an event for the Life engine.
        // An attack attempt, descriptive success claim or unverified action is never a victory.
        val confirmedLifeCauseId = if (outcome.mechanicalMutation &&
            outcome.updatedState.time.days > loaded.campaignState.time.days) {
            when (mechanicalIntent.substringBefore(':').uppercase()) {
                "WILD_TRAVEL_DAY", "WILD_TRAVEL_FORAGE_DAY", "WILD_FORCED_MARCH_DAY" -> "cause-journey-1"
                "WILD_REST_DAY" -> "cause-care-1"
                else -> null
            }
        } else null
        val receipt = ReactionReceiptSnapshot(
            reactionId = reactionId,
            reactionText = reaction,
            mechanicalFeedback = outcome.feedback,
            mechanicalMutation = outcome.mechanicalMutation,
            traceStatus = outcome.trace.status.name,
            traceNote = outcome.trace.note,
            traceEvidenceRefs = evidenceRefs,
            mechanicalIntent = mechanicalIntent,
            actorCharacterId = actorCharacterId,
            lifeCauseId = confirmedLifeCauseId
        )
        val firstSession=beforeGame.session.copy(
            phase="ACTIVE",
            visibleMechanicalFeedback=outcome.feedback,
            lastPlayerReaction=reaction,
            reactionReceipts=beforeGame.session.reactionReceipts+(reactionId to receipt),
            pendingNarrationReactionId=reactionId
        )
        val executedProposals = if (approvedProposalId == null) beforeGame.characterActionProposals else {
            val proposal = beforeGame.characterActionProposals[approvedProposalId]
                ?: error("UNKNOWN_PROPOSAL")
            require(proposal.status == CharacterActionApproval.APPROVED &&
                proposal.characterId == actorCharacterId) { "CHARACTER_PROPOSAL_ALREADY_CONSUMED" }
            beforeGame.characterActionProposals + (approvedProposalId to proposal.copy(
                status = CharacterActionApproval.EXECUTED,
                executedReactionId = reactionId
            ))
        }
        val firstGame=beforeGame.copy(
            session=firstSession,
            characterActionProposals=executedProposals,
            actionLog=beforeGame.actionLog+ActionLogEntry(
                actionId="reaction:$reactionId",channel="PLAYER_REACTION",text=reaction,ruleEvidenceRefs=evidenceRefs
            ),
            rngRootSeed=rng.snapshot().rootSeed.toString(),
            rngStreams=snapshotRng(rng.snapshot()),
            revision=beforeGame.revision+1
        )
        val firstState=outcome.updatedState.copy(game=firstGame)
        repository.commit(campaignId,StateTransition("player-reaction:$reactionId",firstState))

        // The mechanical transition is committed before narration. A crash here is safe:
        // retrying the same reactionId resumes narration without invoking Rules/RNG again.
        val committed=load(campaignId)
        val committedReceipt=committed.campaignState.game.session.reactionReceipts.getValue(reactionId)
        return finishNarration(committed,reactionId,reaction,committedReceipt,outcome.trace)
    }

    private fun finishNarration(
        committed: CampaignEnvelope,
        reactionId: String,
        reaction: String,
        receipt: ReactionReceiptSnapshot,
        trace: RuleTrace
    ): SessionResult {
        receipt.narration?.let {
            return SessionResult(committed.campaignId.value,it,receipt.mechanicalFeedback,receipt.mechanicalMutation,trace)
        }
        val narration=barbara.narrate(committed,reaction,receipt.mechanicalFeedback,trace)
        val committedGame=committed.campaignState.game
        val session=committedGame.session
        val completedReceipt=receipt.copy(narration=narration)
        val finalSession=session.copy(
            phase="ACTIVE",
            visibleNarration=narration,
            journal=session.journal+narration,
            reactionReceipts=session.reactionReceipts+(reactionId to completedReceipt),
            pendingNarrationReactionId=if(session.pendingNarrationReactionId==reactionId) null else session.pendingNarrationReactionId
        )
        val finalGame=committedGame.copy(session=finalSession)
        repository.commit(
            committed.campaignId,
            StateTransition("narrative-projection:$reactionId",committed.campaignState.copy(game=finalGame))
        )
        return SessionResult(committed.campaignId.value,narration,receipt.mechanicalFeedback,receipt.mechanicalMutation,trace)
    }

    private fun traceFromReceipt(receipt: ReactionReceiptSnapshot, fallbackProfile: RuleProfile): RuleTrace {
        val status=runCatching { EvidenceStatus.valueOf(receipt.traceStatus) }.getOrDefault(EvidenceStatus.MISSING_EVIDENCE)
        val evidence=receipt.traceEvidenceRefs.mapNotNull { raw ->
            val p=raw.split('|',limit=4)
            if(p.size!=4) null else {
                val profile=runCatching { RuleProfile.valueOf(p[1]) }.getOrDefault(fallbackProfile)
                RuleEvidence(p[0],profile,p[2],p[3])
            }
        }
        return RuleTrace(status,evidence,receipt.traceNote)
    }


    fun startAdventure(campaignId: CampaignId): SessionResult {
        val loaded = load(campaignId)
        val currentSession = loaded.campaignState.game.session
        if (currentSession.visibleNarration.isNotBlank()) {
            return SessionResult(campaignId.value, currentSession.visibleNarration, "", false, RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, emptyList(), "Narrative opening already present"))
        }
        val trace = RuleTrace(EvidenceStatus.CANONICAL_PROCEDURE, emptyList(), "Narrative opening only; no mechanical resolution")
        val narration = barbara.narrate(
            loaded,
            "A aventura começa agora. Apresente a cena inicial ao jogador e descreva o que ele percebe ao redor antes da primeira decisão.",
            "",
            trace
        )
        val game = loaded.campaignState.game
        val nextSession = game.session.copy(phase = "ACTIVE", visibleNarration = narration, journal = game.session.journal + narration)
        val nextGame = game.copy(session = nextSession, revision = game.revision + 1)
        repository.commit(campaignId, StateTransition("adventure-start:${loaded.createdAtMetadata}:${game.revision}", loaded.campaignState.copy(game = nextGame)))
        return SessionResult(campaignId.value, narration, "", false, trace)
    }

    fun gmHelp(campaignId: CampaignId, question: String): GMHelpResult {
        val before=load(campaignId)
        val revision=before.campaignState.game.revision
        val answer=barbara.help(before,question)
        val after=load(campaignId)
        check(after.campaignState.canonical()==before.campaignState.canonical()) { "GM_HELP_STATE_DELTA must be EMPTY" }
        return GMHelpResult(answer,revision)
    }

    fun loadSession(campaignId: CampaignId): CampaignEnvelope=load(campaignId)

    private fun load(id:CampaignId):CampaignEnvelope=when(val r=repository.load(id)){
        is CampaignLoadResult.Loaded->r.envelope
        is CampaignLoadResult.NotFound->error("Campaign not found: ${id.value}")
        is CampaignLoadResult.ReadFailure->throw IllegalStateException("Campaign read failure: ${id.value}",r.cause)
    }

    private fun restoreRng(game:GameExtensions):NamedRngStreams {
        if(game.rngStreams.isEmpty()) return NamedRngStreams.fromRootSeed(game.rngRootSeed.toULong())
        val streams=RngStreamId.entries.associateWith { id ->
            val s=game.rngStreams[id.name] ?: error("Missing persisted RNG stream ${id.name}")
            Pcg32State(s.state.toULong(),s.increment.toULong(),s.counter,s.version)
        }
        return NamedRngStreams.fromSeedState(SeedState(game.rngRootSeed.toULong(),streams))
    }
    private fun snapshotRng(seed:SeedState):Map<String,RngStateSnapshot> = seed.streams.mapKeys{it.key.name}.mapValues{(_,s)->RngStateSnapshot(s.state.toString(),s.increment.toString(),s.counter,s.version)}
}
