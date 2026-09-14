package braseiro.ose.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable @JvmInline value class CampaignId(val value: String) { init { require(value.isNotBlank()) } }
@Serializable enum class RuleProfile { OSE_CLASSIC_FANTASY, OSE_ADVANCED_FANTASY }
@Serializable data class AuthorityRevision(val authorityId: String, val revision: String, val contentHash: String)
@Serializable data class OptionSet(val optionIds: List<String> = emptyList()) { fun canonical() = copy(optionIds = optionIds.distinct().sorted()) }
@Serializable data class AssetManifestRevision(val schema: String, val revision: String, val contentHash: String)
@Serializable data class GeneratorVersion(val generatorId: String, val version: Int)
@Serializable data class GeneratorRegistry(val versions: List<GeneratorVersion> = emptyList()) { fun canonical() = copy(versions = versions.distinctBy { it.generatorId }.sortedBy { it.generatorId }) }

@Serializable sealed interface SpatialRef {
    val spatialEntityId: String
    @Serializable @SerialName("dungeon") data class Dungeon(override val spatialEntityId: String, val nodeId: String) : SpatialRef
    @Serializable @SerialName("hex") data class Hex(override val spatialEntityId: String, val q: Int, val r: Int) : SpatialRef
    @Serializable @SerialName("settlement") data class Settlement(override val spatialEntityId: String, val anchorId: String) : SpatialRef
    @Serializable @SerialName("scene") data class Scene(override val spatialEntityId: String, val sceneId: String) : SpatialRef
}

@Serializable data class PositionState(val primary: SpatialRef)
@Serializable data class TimeState(val rounds: Long = 0, val turns: Long = 0, val hours: Long = 0, val days: Long = 0) {
    init { require(rounds >= 0 && turns >= 0 && hours >= 0 && days >= 0) }
}
@Serializable data class PartyState(val characterIds: List<String> = emptyList()) { fun canonical() = copy(characterIds = characterIds.distinct().sorted()) }
@Serializable data class ResourceState(val resourceFacts: List<String> = emptyList()) { fun canonical() = copy(resourceFacts = resourceFacts.distinct().sorted()) }
@Serializable data class PlayerKnowledgeState(val knownFactIds: List<String> = emptyList()) { fun canonical() = copy(knownFactIds = knownFactIds.distinct().sorted()) }

@Serializable data class CampaignState(
    val party: PartyState,
    val time: TimeState,
    val position: PositionState,
    val resources: ResourceState = ResourceState(),
    val playerKnowledge: PlayerKnowledgeState = PlayerKnowledgeState(),
    val game: GameExtensions = GameExtensions()
) {
    fun canonical() = copy(party = party.canonical(), resources = resources.canonical(), playerKnowledge = playerKnowledge.canonical(), game = game.canonical())
}

@Serializable data class CampaignEnvelope(
    val schemaVersion: Int = 1,
    val campaignId: CampaignId,
    val createdAtMetadata: String,
    val ruleProfile: RuleProfile,
    val authorityRevision: AuthorityRevision,
    val optionSet: OptionSet,
    val generatorRegistry: GeneratorRegistry,
    val assetManifestRevision: AssetManifestRevision,
    val campaignState: CampaignState
) {
    init { require(schemaVersion > 0) }
    fun canonicalMechanical() = MechanicalCampaign(schemaVersion, campaignId, ruleProfile, authorityRevision, optionSet.canonical(), generatorRegistry.canonical(), assetManifestRevision, campaignState.canonical())
}

@Serializable data class MechanicalCampaign(
    val schemaVersion: Int,
    val campaignId: CampaignId,
    val ruleProfile: RuleProfile,
    val authorityRevision: AuthorityRevision,
    val optionSet: OptionSet,
    val generatorRegistry: GeneratorRegistry,
    val assetManifestRevision: AssetManifestRevision,
    val campaignState: CampaignState
)
