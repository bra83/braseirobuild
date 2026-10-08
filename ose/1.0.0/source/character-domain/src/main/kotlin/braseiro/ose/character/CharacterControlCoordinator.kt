package braseiro.ose.character

import braseiro.ose.model.*

/**
 * Canonical authority boundary for party-member autonomy.
 * Mode changes and curated proposals never change time, RNG, inventory, or HP.
 * Only SessionEngine may commit an approved action through the referee.
 */
object CharacterControlCoordinator {
    fun mode(envelope: CampaignEnvelope, characterId: String): CharacterControlMode {
        require(characterId in envelope.campaignState.party.characterIds &&
            characterId in envelope.campaignState.game.characters) { "UNKNOWN_CHARACTER_ID" }
        return envelope.campaignState.game.characterControlModes[characterId] ?: CharacterControlMode.PLAYER
    }

    fun setMode(envelope: CampaignEnvelope, characterId: String, newMode: CharacterControlMode): CampaignEnvelope {
        require(characterId in envelope.campaignState.party.characterIds &&
            characterId in envelope.campaignState.game.characters) { "UNKNOWN_CHARACTER_ID" }
        val state = envelope.campaignState
        val game = state.game
        if (mode(envelope, characterId) == newMode) return envelope
        val modes = (game.characterControlModes + (characterId to newMode)).toSortedMap()
        // Switching control revokes unapproved proposals from the previous authority.
        val proposals = game.characterActionProposals.mapValues { (_, p) ->
            if (p.characterId == characterId && p.status in setOf(
                CharacterActionApproval.PENDING, CharacterActionApproval.APPROVED
            )) p.copy(status = CharacterActionApproval.REJECTED) else p
        }
        return envelope.copy(campaignState = state.copy(game = game.copy(
            characterControlModes = modes,
            characterActionProposals = proposals,
            revision = game.revision + 1
        ).canonical()))
    }

    fun propose(
        envelope: CampaignEnvelope,
        proposalId: String,
        characterId: String,
        text: String,
        mechanicalIntent: String
    ): CampaignEnvelope {
        val mode = mode(envelope, characterId)
        require(mode != CharacterControlMode.PLAYER) { "CHARACTER_PLAYER_CONTROLLED" }
        require(proposalId.isNotBlank() && proposalId.length <= 128) { "INVALID_PROPOSAL_ID" }
        require(text.isNotBlank() && text.length <= 2400 &&
            mechanicalIntent.isNotBlank() && mechanicalIntent.length <= 2400) { "INVALID_PROPOSAL_CONTENT" }
        val state = envelope.campaignState
        val game = state.game
        val previous = game.characterActionProposals[proposalId]
        if (previous != null) {
            require(previous.characterId == characterId &&
                previous.reactionText == text &&
                previous.mechanicalIntent == mechanicalIntent) { "PROPOSAL_ID_COLLISION" }
            return envelope
        }
        val proposal = CharacterActionProposalSnapshot(
            proposalId, characterId, text, mechanicalIntent,
            if (mode == CharacterControlMode.MASTER) CharacterActionApproval.APPROVED
            else CharacterActionApproval.PENDING, game.revision
        )
        return envelope.copy(campaignState = state.copy(game = game.copy(
            characterActionProposals = game.characterActionProposals + (proposalId to proposal),
            revision = game.revision + 1
        ).canonical()))
    }

    fun review(
        envelope: CampaignEnvelope,
        proposalId: String,
        approve: Boolean,
        editedText: String? = null,
        editedIntent: String? = null
    ): CampaignEnvelope {
        val state = envelope.campaignState
        val game = state.game
        val existing = game.characterActionProposals[proposalId] ?: error("UNKNOWN_PROPOSAL")
        require(mode(envelope, existing.characterId) == CharacterControlMode.CURATED) { "CHARACTER_NOT_CURATED" }
        if (existing.status == CharacterActionApproval.APPROVED && approve &&
            editedText == null && editedIntent == null) return envelope
        require(existing.status == CharacterActionApproval.PENDING) { "PROPOSAL_ALREADY_REVIEWED" }
        val text = editedText ?: existing.reactionText
        val intent = editedIntent ?: existing.mechanicalIntent
        require(text.isNotBlank() && text.length <= 2400 &&
            intent.isNotBlank() && intent.length <= 2400) { "INVALID_PROPOSAL_CONTENT" }
        val next = existing.copy(
            reactionText = text,
            mechanicalIntent = intent,
            status = if (approve) CharacterActionApproval.APPROVED else CharacterActionApproval.REJECTED
        )
        return envelope.copy(campaignState = state.copy(game = game.copy(
            characterActionProposals = game.characterActionProposals + (proposalId to next),
            revision = game.revision + 1
        ).canonical()))
    }

    fun requireActionAllowed(
        envelope: CampaignEnvelope,
        characterId: String,
        fromMaster: Boolean,
        proposalId: String?,
        text: String,
        mechanicalIntent: String
    ) {
        when (mode(envelope, characterId)) {
            CharacterControlMode.PLAYER ->
                require(!fromMaster && proposalId.isNullOrBlank()) { "CHARACTER_PLAYER_CONTROLLED" }
            CharacterControlMode.MASTER ->
                require(fromMaster) { "CHARACTER_MASTER_CONTROLLED" }
            CharacterControlMode.CURATED -> {
                require(fromMaster && !proposalId.isNullOrBlank()) { "CHARACTER_CURATOR_APPROVAL_REQUIRED" }
                val proposal = envelope.campaignState.game.characterActionProposals[proposalId]
                    ?: error("UNKNOWN_PROPOSAL")
                require(proposal.characterId == characterId &&
                    proposal.status in setOf(CharacterActionApproval.APPROVED, CharacterActionApproval.EXECUTED) &&
                    proposal.reactionText == text &&
                    proposal.mechanicalIntent == mechanicalIntent) { "CHARACTER_CURATOR_APPROVAL_REQUIRED" }
            }
        }
    }

    fun markExecuted(envelope: CampaignEnvelope, proposalId: String): CampaignEnvelope {
        val state = envelope.campaignState
        val game = state.game
        val p = game.characterActionProposals[proposalId] ?: error("UNKNOWN_PROPOSAL")
        if (p.status == CharacterActionApproval.EXECUTED) return envelope
        require(p.status == CharacterActionApproval.APPROVED) { "CHARACTER_CURATOR_APPROVAL_REQUIRED" }
        return envelope.copy(campaignState = state.copy(game = game.copy(
            characterActionProposals = game.characterActionProposals +
                (proposalId to p.copy(status = CharacterActionApproval.EXECUTED)),
            revision = game.revision + 1
        ).canonical()))
    }
}
