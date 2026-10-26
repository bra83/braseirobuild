package braseiro.ose.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        if (mimeType.contains("heic", ignoreCase = true) || mimeType.contains("heif", ignoreCase = true)) {
            throw IllegalArgumentException("HEIC_FORMAT_UNSUPPORTED: O formato HEIC não é suportado pelo renderizador nativo. Salve ou converta a foto para JPEG, PNG ou WebP.")
        }
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp")) { "UNSUPPORTED_IMAGE_TYPE: $mimeType" }
        require(bytes.isNotEmpty() && bytes.size <= 20 * 1024 * 1024) { "IMAGE_SIZE_INVALID: ${bytes.size} bytes (máx 20MB)" }

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        val w = boundsOpts.outWidth
        val h = boundsOpts.outHeight
        require(w > 0 && h > 0) { "CORRUPT_OR_MALFORMED_IMAGE: Não foi possível decodificar as dimensões da imagem." }
        require(w.toLong() * h.toLong() <= 100_000_000L) { "IMAGE_DIMENSIONS_EXCESSIVE" }

        // Resized WebP must not be written as JPEG under a .webp extension.
        // PNG also preserves the alpha channel in enlarged illustrations.
        val outputMime = if (maxOf(w, h) > 2048 && mimeType == "image/webp") "image/png" else mimeType
        val ext = when (outputMime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val publicName = "${scope.name.lowercase()}-${sha256(stableId).take(24)}.$ext"
        val targetFile = File(root, publicName)
        val tmpFile = File(root, "$publicName.tmp")

        try {
            val maxDim = maxOf(w, h)
            if (maxDim > 2048) {
                var sample = 1
                while (maxDim / sample > 2048) sample *= 2
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                    ?: throw IllegalArgumentException("IMAGE_DECODE_FAILED")
                try {
                    tmpFile.outputStream().use { out ->
                        val format = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                        bmp.compress(format, 90, out)
                    }
                } finally {
                    bmp.recycle()
                }
            } else {
                tmpFile.writeBytes(bytes)
            }
            if (!tmpFile.renameTo(targetFile)) {
                targetFile.delete()
                if (!tmpFile.renameTo(targetFile)) {
                    targetFile.writeBytes(tmpFile.readBytes())
                    tmpFile.delete()
                }
            }
        } catch (t: Throwable) {
            tmpFile.delete()
            throw IllegalArgumentException("MEDIA_PROCESSING_FAILED: ${t.message}", t)
        }

        val key = key(scope, stableId)
        // Unique revision prevents WebView from reusing the former image when
        // a character replaces their portrait at the same canonical file path.
        val revision = java.util.UUID.randomUUID().toString().take(12)
        val meta = readMetadata().put(key, JSONObject()
            .put("file", publicName).put("mime", outputMime).put("rev", revision))
        writeMetadata(meta)
        mediaUrl(publicName, revision)
    }

    fun saveDataUrl(scope: Scope, stableId: String, dataUrl: String): String = runCatching {
        val match = Regex("^data:(image/(?:jpeg|png|webp));base64,(.+)$", RegexOption.DOT_MATCHES_ALL).matchEntire(dataUrl)
            ?: error("INVALID_IMAGE_DATA_URL")
        val bytes = Base64.getDecoder().decode(match.groupValues[2])
        save(scope, stableId, match.groupValues[1], bytes)
    }.getOrElse { t ->
        throw IllegalArgumentException("MEDIA_SAVE_FAILED: ${t.message}", t)
    }

    fun remove(scope: Scope, stableId: String): Boolean = synchronized(lock) {
        val meta = readMetadata()
        val key = key(scope, stableId)
        val entry = meta.optJSONObject(key) ?: return false
        val filename = entry.optString("file")
        if (filename.isNotBlank()) {
            File(root, filename).delete()
        }
        meta.remove(key)
        writeMetadata(meta)
        true
    }

    fun exportDataUrl(scope: Scope, stableId: String): String? = synchronized(lock) {
        val meta = readMetadata().optJSONObject(key(scope, stableId)) ?: return null
        val filename = meta.optString("file")
        val mime = meta.optString("mime", "image/png")
        val file = File(root, filename)
        if (!file.isFile) return null
        val b64 = Base64.getEncoder().encodeToString(file.readBytes())
        "data:$mime;base64,$b64"
    }

    fun url(scope: Scope, stableId: String): String? = synchronized(lock) {
        val entry = readMetadata().optJSONObject(key(scope, stableId)) ?: return null
        val file = entry.optString("file")
        if (!file.matches(Regex("[a-z]+-[0-9a-f]{24}\\.(jpg|png|webp)")) || !File(root, file).isFile) return null
        mediaUrl(file, entry.optString("rev"))
    }

    override fun handle(path: String): WebResourceResponse? {
        val safe = path.substringAfterLast('/').takeIf { it.matches(Regex("[a-z]+-[0-9a-f]{24}\\.(jpg|png|webp)")) } ?: return null
        val file = File(root, safe)
        if (!file.isFile) return null
        val mime = when (file.extension.lowercase()) { "png" -> "image/png"; "webp" -> "image/webp"; else -> "image/jpeg" }
        return WebResourceResponse(mime, null, FileInputStream(file))
    }

    private fun key(scope: Scope, stableId: String) = "${scope.name}:$stableId"
    private fun mediaUrl(publicName: String, revision: String = "") =
        "https://appassets.androidplatform.net/media/$publicName" +
            if (revision.isBlank()) "" else "?v=$revision"
    private fun readMetadata() = try { if (metadataFile.exists()) JSONObject(metadataFile.readText()) else JSONObject() } catch (_: Throwable) { JSONObject() }
    private fun writeMetadata(json: JSONObject) {
        val tmp = File(root, "index.tmp")
        tmp.writeText(json.toString())
        if (!tmp.renameTo(metadataFile)) { metadataFile.writeText(json.toString()); tmp.delete() }
    }
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
