package braseiro.ose.assets

import kotlinx.serialization.json.*

class AssetManifestException(message: String) : IllegalStateException(message)

data class AssetRequest(
    val assetId: String,
    val expectedSha256: String? = null,
    val expectedClassification: String? = null,
    val requiredGeometryVersion: Int? = null
)

data class ResolvedAsset(
    val assetId: String,
    val assetName: String,
    val classification: String,
    val approvalStatus: String,
    val fileId: String,
    val sha256: String?,
    val geometryVersion: Int?
)

class AssetManifestResolver private constructor(private val root: JsonObject) {
    val schema: String = root["schema"]?.jsonPrimitive?.content ?: throw AssetManifestException("Manifest schema missing")
    val manifestGeometryVersion: Int? = root["HEX_GEOMETRY_VERSION"]?.jsonPrimitive?.intOrNull
    private val assets: Map<String, JsonObject> = root["assets"]?.jsonArray?.associate { element ->
        val obj = element.jsonObject
        val id = obj["manifest_asset_id"]?.jsonPrimitive?.content ?: throw AssetManifestException("Asset without manifest_asset_id")
        id to obj
    } ?: throw AssetManifestException("Manifest assets missing")

    fun resolve(request: AssetRequest): ResolvedAsset {
        val asset = assets[request.assetId] ?: throw AssetManifestException("Missing canonical asset ${request.assetId}")
        val classification = asset.requiredString("classification")
        if (request.expectedClassification != null && classification != request.expectedClassification) {
            throw AssetManifestException("Classification mismatch for ${request.assetId}: expected ${request.expectedClassification}, got $classification")
        }
        val sha = asset.sha256OrNull()
        if (request.expectedSha256 != null && sha != request.expectedSha256) {
            throw AssetManifestException("SHA-256 mismatch for ${request.assetId}: expected ${request.expectedSha256}, got ${sha ?: "missing"}")
        }
        if (request.requiredGeometryVersion != null && manifestGeometryVersion != request.requiredGeometryVersion) {
            throw AssetManifestException("Geometry version mismatch: expected ${request.requiredGeometryVersion}, got ${manifestGeometryVersion ?: "missing"}")
        }
        return ResolvedAsset(
            assetId = request.assetId,
            assetName = asset.requiredString("asset_name"),
            classification = classification,
            approvalStatus = asset.requiredString("approval_status"),
            fileId = asset.requiredString("file_id"),
            sha256 = sha,
            geometryVersion = manifestGeometryVersion
        )
    }

    fun contains(assetId: String): Boolean = assets.containsKey(assetId)
    fun assetCount(): Int = assets.size

    companion object {
        const val EXPECTED_SCHEMA = "BRASEIRO_VISUAL_FORGE_ASSET_MANIFEST_V2"
        fun parse(text: String): AssetManifestResolver {
            val root = Json.parseToJsonElement(text).jsonObject
            val resolver = AssetManifestResolver(root)
            if (resolver.schema != EXPECTED_SCHEMA) throw AssetManifestException("Unexpected manifest schema ${resolver.schema}")
            return resolver
        }
    }
}

private fun JsonObject.requiredString(key: String): String = this[key]?.jsonPrimitive?.content ?: throw AssetManifestException("Asset field $key missing")
private fun JsonObject.sha256OrNull(): String? {
    this["sha256"]?.jsonPrimitive?.contentOrNull?.let { return it }
    this["phase2_base_qa"]?.jsonObject?.get("sha256")?.jsonPrimitive?.contentOrNull?.let { return it }
    this["phase3_geometry_qa"]?.jsonObject?.get("sha256")?.jsonPrimitive?.contentOrNull?.let { return it }
    this["phase4_qa"]?.jsonObject?.get("sha256")?.jsonPrimitive?.contentOrNull?.let { return it }
    return null
}
