package braseiro.ose.closure

import java.security.MessageDigest

/**
 * Source-level closure contract for the four runtime surfaces.  This layer is deliberately
 * UI-agnostic: SESSAO / MAPA / FICHA / COMPANHIA are projections of one canonical snapshot,
 * never independent authorities.  A baseline adapter is responsible for mapping CampaignEnvelope
 * to CanonicalViewInput; screen-local pan/zoom/selection is excluded from mechanical state.
 */
enum class OseProfile { CLASSIC, ADVANCED }
enum class SurfaceId { SESSAO, MAPA, FICHA, COMPANHIA }

data class TypedSpatialRef(
    val kind: Kind,
    val primaryId: String,
    val secondaryId: String? = null
) {
    enum class Kind { SCENE, HEX, DUNGEON, SETTLEMENT, WATERBORNE }
    init { require(primaryId.isNotBlank()) }
}

data class VisibleCharacter(
    val characterId: String,
    val name: String,
    val classIds: List<String>,
    val levelByClass: Map<String, Int>,
    val hpCurrentNumerator: Int,
    val hpMaxNumerator: Int,
    val hpDenominator: Int = 1
) {
    init { require(characterId.isNotBlank()); require(name.isNotBlank()); require(hpDenominator > 0) }
}

data class VisibleInventoryItem(
    val itemId: String,
    val ownerId: String,
    val catalogId: String,
    val quantity: Int,
    val carried: Boolean,
    val equipped: Boolean
) {
    init { require(itemId.isNotBlank()); require(ownerId.isNotBlank()); require(catalogId.isNotBlank()); require(quantity >= 0) }
}

data class VisibleLight(
    val lightId: String,
    val carrierId: String,
    val kind: String,
    val remainingTurns: Int,
    val lit: Boolean
) {
    init { require(lightId.isNotBlank()); require(carrierId.isNotBlank()); require(remainingTurns >= 0) }
}

data class PlayerKnowledgeView(
    val discoveredLocationIds: Set<String> = emptySet(),
    val visibleNpcIds: Set<String> = emptySet(),
    val knownFacts: Map<String, String> = emptyMap()
)

data class CanonicalViewInput(
    val projectionRevision: Long,
    val mechanicalStateHash: String,
    val profile: OseProfile,
    val authorityRevision: String,
    val optionSetHash: String,
    val position: TypedSpatialRef,
    val partyCharacterIds: List<String>,
    val characters: Map<String, VisibleCharacter>,
    val inventory: Map<String, VisibleInventoryItem>,
    val lights: Map<String, VisibleLight>,
    val baseMoveMeters: Int,
    val encounterMoveMeters: Int,
    val timeTurns: Long,
    val timeRounds: Long,
    val knowledge: PlayerKnowledgeView,
    val visibleNarration: String,
    /** Secret material is accepted here only to prove it is never copied to a player projection. */
    val secretFactIds: Set<String> = emptySet()
) {
    init {
        require(projectionRevision >= 0)
        require(mechanicalStateHash.matches(Regex("[0-9a-f]{64}")))
        require(authorityRevision.isNotBlank())
        require(optionSetHash.matches(Regex("[0-9a-f]{64}")))
        require(baseMoveMeters >= 0 && encounterMoveMeters >= 0)
        require(timeTurns >= 0 && timeRounds >= 0)
    }
}

data class SurfaceProjection(
    val surface: SurfaceId,
    val projectionRevision: Long,
    val mechanicalStateHash: String,
    val profile: OseProfile,
    val authorityRevision: String,
    val optionSetHash: String,
    val position: TypedSpatialRef,
    val partyCharacterIds: List<String>,
    val characters: List<VisibleCharacter>,
    val inventory: List<VisibleInventoryItem>,
    val lights: List<VisibleLight>,
    val baseMoveMeters: Int,
    val encounterMoveMeters: Int,
    val timeTurns: Long,
    val timeRounds: Long,
    val knowledge: PlayerKnowledgeView,
    val visibleNarration: String,
    val projectionHash: String
)

data class FourSurfaceViewState(
    val revision: Long,
    val mechanicalStateHash: String,
    val surfaces: Map<SurfaceId, SurfaceProjection>
) {
    init {
        require(surfaces.keys == SurfaceId.entries.toSet())
        require(surfaces.values.all { it.projectionRevision == revision })
        require(surfaces.values.all { it.mechanicalStateHash == mechanicalStateHash })
        require(surfaces.values.map { it.projectionHash }.distinct().size == 1)
    }
}

enum class LocalUiMutation { PAN, ZOOM, SELECT_HEX, SELECT_CHARACTER, OPEN_PANEL, COLLAPSE_PANEL }

data class LocalUiDelta(val mutation: LocalUiMutation, val mechanicalDelta: Boolean = false, val consumesRulesRng: Boolean = false)

object AppViewProjectionContract {
    fun project(input: CanonicalViewInput): FourSurfaceViewState {
        val visibleCharacters = input.partyCharacterIds.distinct().mapNotNull(input.characters::get)
            .sortedBy { it.characterId }
        val visibleOwners = visibleCharacters.map { it.characterId }.toSet()
        val visibleInventory = input.inventory.values.filter { it.ownerId in visibleOwners }.sortedBy { it.itemId }
        val visibleLights = input.lights.values.filter { it.carrierId in visibleOwners }.sortedBy { it.lightId }
        val knowledge = input.knowledge.copy(
            discoveredLocationIds = input.knowledge.discoveredLocationIds.toSortedSet(),
            visibleNpcIds = input.knowledge.visibleNpcIds.toSortedSet(),
            knownFacts = input.knowledge.knownFacts.filterKeys { it !in input.secretFactIds }.toSortedMap()
        )
        val commonHash = projectionHash(input, visibleCharacters, visibleInventory, visibleLights, knowledge)
        val surfaces = SurfaceId.entries.associateWith { surface ->
            SurfaceProjection(
                surface, input.projectionRevision, input.mechanicalStateHash, input.profile,
                input.authorityRevision, input.optionSetHash, input.position,
                input.partyCharacterIds.distinct(), visibleCharacters, visibleInventory, visibleLights,
                input.baseMoveMeters, input.encounterMoveMeters, input.timeTurns, input.timeRounds,
                knowledge, input.visibleNarration, commonHash
            )
        }
        return FourSurfaceViewState(input.projectionRevision, input.mechanicalStateHash, surfaces)
    }

    fun localUiMutation(kind: LocalUiMutation): LocalUiDelta = LocalUiDelta(kind)

    private fun projectionHash(
        input: CanonicalViewInput,
        chars: List<VisibleCharacter>, items: List<VisibleInventoryItem>, lights: List<VisibleLight>, knowledge: PlayerKnowledgeView
    ): String {
        // Stable summary deliberately excludes SurfaceId and all local UI state.
        val text = buildString {
            append(input.projectionRevision).append('|').append(input.mechanicalStateHash).append('|')
            append(input.profile).append('|').append(input.authorityRevision).append('|').append(input.optionSetHash).append('|')
            append(input.position.kind).append(':').append(input.position.primaryId).append(':').append(input.position.secondaryId ?: "").append('|')
            append(input.partyCharacterIds.distinct().joinToString(",")).append('|')
            chars.forEach { append(it.characterId).append(':').append(it.levelByClass.toSortedMap()).append(':').append(it.hpCurrentNumerator).append('/').append(it.hpDenominator).append(';') }
            append('|'); items.forEach { append(it.itemId).append(':').append(it.catalogId).append(':').append(it.quantity).append(';') }
            append('|'); lights.forEach { append(it.lightId).append(':').append(it.remainingTurns).append(':').append(it.lit).append(';') }
            append('|').append(input.baseMoveMeters).append(':').append(input.encounterMoveMeters)
            append('|').append(input.timeTurns).append(':').append(input.timeRounds)
            append('|').append(knowledge.discoveredLocationIds.sorted()).append('|').append(knowledge.visibleNpcIds.sorted()).append('|').append(knowledge.knownFacts.toSortedMap())
            append('|').append(input.visibleNarration)
        }
        return sha256(text)
    }

    internal fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
