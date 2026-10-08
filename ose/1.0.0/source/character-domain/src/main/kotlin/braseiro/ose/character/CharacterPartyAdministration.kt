package braseiro.ose.character

import braseiro.ose.model.*
import braseiro.ose.rules.shared.InventoryRuntimeRules

/**
 * Administrative removal of one member. The campaign world, turns, session and
 * shared resources remain intact. Deleted members are archived in the canonical
 * character map so old event IDs stay resolvable and cannot be silently reused.
 *
 * The repository caller must checkpoint and atomically commit the returned state.
 */
object CharacterPartyAdministration {
    fun delete(envelope: CampaignEnvelope, characterId: String, commandId: String): CampaignEnvelope {
        require(characterId.isNotBlank() && commandId.isNotBlank()) { "INVALID_CHARACTER_COMMAND" }
        val old = envelope.campaignState
        require(characterId in old.party.characterIds) { "UNKNOWN_CHARACTER_ID" }
        val character = old.game.characters[characterId] ?: error("UNKNOWN_CHARACTER_ID")
        val survivors = old.party.characterIds.filterNot { it == characterId }
        val inventory = old.game.inventory.copy(
            items = old.game.inventory.items.filterValues { it.ownerId != characterId },
            basicSignificantTreasureOwners = old.game.inventory.basicSignificantTreasureOwners - characterId
        ).canonical()
        val expedition = old.game.expedition.copy(
            lightSources = old.game.expedition.lightSources.filterNot { it.carrierId == characterId }
        )
        val movement = InventoryRuntimeRules.derivePartyExpedition(
            envelope.ruleProfile, envelope.optionSet, survivors, inventory, expedition
        )
        val game = old.game.copy(
            characters = old.game.characters + (characterId to character.copy(
                flags = character.flags + "ARCHIVED_DELETED"
            ).canonical()),
            inventory = inventory,
            loadouts = old.game.loadouts - characterId,
            characterControlModes = old.game.characterControlModes - characterId,
            characterActionProposals = old.game.characterActionProposals.mapValues { (_, p) ->
                if (p.characterId == characterId && p.status in setOf(
                    CharacterActionApproval.PENDING, CharacterActionApproval.APPROVED
                )) p.copy(status = CharacterActionApproval.REJECTED) else p
            },
            // Preserve XP history but make an archived sheet ineligible for future
            // XP awards and level-ups. Existing recipients remain addressable.
            experience = old.game.experience.copy(
                recipients = old.game.experience.recipients.mapValues { (id, recipient) ->
                    if (id == characterId) recipient.copy(surviving = false) else recipient
                }
            ).canonical(),
            expedition = movement.expedition,
            actionLog = old.game.actionLog + ActionLogEntry(
                "CHARACTER_DELETE:$commandId",
                "CAMPAIGN_ADMIN",
                "Ficha $characterId excluída da companhia (registro histórico preservado)",
                emptyList()
            ),
            revision = old.game.revision + 1
        ).canonical()
        return envelope.copy(campaignState = old.copy(party = PartyState(survivors), game = game))
    }
}
