package braseiro.ose.backup

import braseiro.ose.model.CampaignEnvelope
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class BackupEnvelope(
    val formatVersion: Int = 1,
    val campaignJson: String,
    val campaignSha256: String,
    val schemaVersion: Int,
    val authorityId: String,
    val authorityRevision: String,
    val assetManifestRevision: String,
    val generatorVersions: Map<String, Int>
)

object BackupCodec {
    private val json = Json { encodeDefaults = true; explicitNulls = false; classDiscriminator = "kind"; ignoreUnknownKeys = false }
    fun exportCampaign(campaign: CampaignEnvelope): String {
        val payload = json.encodeToString(campaign)
        val meta = BackupEnvelope(
            campaignJson = payload,
            campaignSha256 = sha256(payload.toByteArray()),
            schemaVersion = campaign.schemaVersion,
            authorityId = campaign.authorityRevision.authorityId,
            authorityRevision = campaign.authorityRevision.revision,
            assetManifestRevision = campaign.assetManifestRevision.revision,
            generatorVersions = campaign.generatorRegistry.versions.associate { it.generatorId to it.version }.toSortedMap()
        )
        return json.encodeToString(meta)
    }
    fun importCampaign(raw: String): CampaignEnvelope {
        val backup = json.decodeFromString<BackupEnvelope>(raw)
        require(backup.formatVersion == 1) { "Unsupported backup format ${backup.formatVersion}" }
        require(sha256(backup.campaignJson.toByteArray()) == backup.campaignSha256) { "Backup checksum mismatch" }
        val campaign = json.decodeFromString<CampaignEnvelope>(backup.campaignJson)
        require(campaign.schemaVersion == backup.schemaVersion)
        require(campaign.authorityRevision.authorityId == backup.authorityId)
        require(campaign.authorityRevision.revision == backup.authorityRevision)
        require(campaign.assetManifestRevision.revision == backup.assetManifestRevision)
        return campaign
    }
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
