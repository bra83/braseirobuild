package braseiro.ose.app

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Base64
import org.json.JSONObject

/** Presentation-only media. Never feeds Rules Engine state. */
class PresentationMediaStore(context: Context) : WebViewAssetLoader.PathHandler {
    enum class Scope { SCENE, CHARACTER, NPC, ITEM }

    private val root = File(context.filesDir, "presentation-media-v1").apply { mkdirs() }
    private val metadataFile = File(root, "index.json")
    private val lock = Any()

    fun save(scope: Scope, stableId: String, mimeType: String, bytes: ByteArray): String = synchronized(lock) {
        require(stableId.isNotBlank()) { "MEDIA_ID_REQUIRED" }
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp")) { "UNSUPPORTED_IMAGE_TYPE" }
        require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { "IMAGE_SIZE_INVALID" }
        val ext = when (mimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val publicName = "${scope.name.lowercase()}-${sha256(stableId).take(24)}.$ext"
        File(root, publicName).writeBytes(bytes)
        val key = key(scope, stableId)
        val meta = readMetadata().put(key, JSONObject().put("file", publicName).put("mime", mimeType))
        writeMetadata(meta)
        mediaUrl(publicName)
    }

    fun saveDataUrl(scope: Scope, stableId: String, dataUrl: String): String {
        val match = Regex("^data:(image/(?:jpeg|png|webp));base64,(.+)$", RegexOption.DOT_MATCHES_ALL).matchEntire(dataUrl)
            ?: error("INVALID_IMAGE_DATA_URL")
        val bytes = Base64.getDecoder().decode(match.groupValues[2])
        return save(scope, stableId, match.groupValues[1], bytes)
    }

    fun url(scope: Scope, stableId: String): String? = synchronized(lock) {
        readMetadata().optJSONObject(key(scope, stableId))?.optString("file")?.takeIf { it.isNotBlank() }?.let(::mediaUrl)
    }

    override fun handle(path: String): WebResourceResponse? {
        val safe = path.substringAfterLast('/').takeIf { it.matches(Regex("[a-z]+-[0-9a-f]{24}\\.(jpg|png|webp)")) } ?: return null
        val file = File(root, safe)
        if (!file.isFile) return null
        val mime = when (file.extension.lowercase()) { "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg" }
        return WebResourceResponse(mime, null, FileInputStream(file))
    }

    private fun key(scope: Scope, stableId: String) = "${scope.name}:$stableId"
    private fun mediaUrl(publicName: String) = "https://appassets.androidplatform.net/media/$publicName"
    private fun readMetadata() = try { if (metadataFile.exists()) JSONObject(metadataFile.readText()) else JSONObject() } catch (_: Throwable) { JSONObject() }
    private fun writeMetadata(json: JSONObject) {
        val tmp = File(root, "index.tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(metadataFile)) { metadataFile.writeText(json.toString()); tmp.delete() }
    }
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
