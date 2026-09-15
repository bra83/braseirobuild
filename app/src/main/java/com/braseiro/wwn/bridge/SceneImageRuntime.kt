package com.braseiro.wwn.bridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebResourceResponse
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class SceneImageRuntime(private val context: Context) {
    private val directory = File(context.filesDir, "scene-images-v1").apply { mkdirs() }

    fun importImage(uri: Uri): JSONObject {
        val temp = File.createTempFile("scene-import-", ".tmp", context.cacheDir)
        try {
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_IMAGE_BYTES) { "Scene image exceeds 12 MB" }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: throw IllegalStateException("Unable to read selected image")
            require(total > 0) { "Selected image is empty" }

            val signature = FileInputStream(temp).use { input -> ByteArray(16).also { input.read(it) } }
            val type = detectType(signature) ?: throw IllegalArgumentException("Only PNG, JPEG or WebP scene images are allowed")
            val declared = context.contentResolver.getType(uri)?.lowercase().orEmpty()
            if (declared.isNotBlank() && declared != "image/*") {
                require(declared in type.acceptedMimeTypes) { "Scene image MIME does not match file content" }
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight
            require(width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION) { "Scene image dimensions are invalid or too large" }
            require(width.toLong() * height.toLong() <= MAX_PIXELS) { "Scene image pixel count is too large" }

            val stored = normalizeForMobile(temp, type, width, height)
            try {
                val digest = sha256(stored.file)
                val name = "${digest.take(32)}.${stored.type.extension}"
                val target = File(directory, name)
                if (!target.exists()) stored.file.copyTo(target, overwrite = false)
                return JSONObject()
                    .put("status", "OK")
                    .put("mimeType", stored.type.mimeType)
                    .put("width", stored.width)
                    .put("height", stored.height)
                    .put("byteLength", stored.file.length())
                    .put("sourceByteLength", total)
                    .put("downscaled", stored.width != width || stored.height != height)
                    .put("sha256", digest)
                    .put("url", "$LOCAL_ORIGIN/private-scene/$name")
                    .put("privateStorage", true)
            } finally { if (stored.file != temp) stored.file.delete() }
        } finally {
            temp.delete()
        }
    }


    fun saveGeneratedImage(bytes: ByteArray, mimeType: String): JSONObject {
        require(bytes.isNotEmpty()) { "Generated image is empty" }
        require(bytes.size.toLong() <= MAX_IMAGE_BYTES) { "Generated image exceeds 12 MB" }
        val temp = File.createTempFile("scene-generated-", ".tmp", context.cacheDir)
        try {
            FileOutputStream(temp).use { it.write(bytes) }
            val signature = bytes.copyOfRange(0, minOf(bytes.size, 16))
            val type = detectType(signature) ?: throw IllegalArgumentException("Generated image format is unsupported")
            val declared = mimeType.lowercase().trim()
            if (declared.isNotBlank()) require(declared in type.acceptedMimeTypes) { "Generated image MIME does not match content" }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temp.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight
            require(width in 1..MAX_DIMENSION && height in 1..MAX_DIMENSION) { "Generated image dimensions are invalid or too large" }
            require(width.toLong() * height.toLong() <= MAX_PIXELS) { "Generated image pixel count is too large" }
            val stored = normalizeForMobile(temp, type, width, height)
            try {
                val digest = sha256(stored.file)
                val name = "${digest.take(32)}.${stored.type.extension}"
                val target = File(directory, name)
                if (!target.exists()) stored.file.copyTo(target, overwrite = false)
                return JSONObject()
                    .put("status", "OK")
                    .put("mimeType", stored.type.mimeType)
                    .put("width", stored.width)
                    .put("height", stored.height)
                    .put("byteLength", stored.file.length())
                    .put("sourceByteLength", bytes.size)
                    .put("downscaled", stored.width != width || stored.height != height)
                    .put("sha256", digest)
                    .put("url", "$LOCAL_ORIGIN/private-scene/$name")
                    .put("privateStorage", true)
                    .put("generated", true)
            } finally { if (stored.file != temp) stored.file.delete() }
        } finally {
            temp.delete()
        }
    }

    fun openWebResource(uri: Uri): WebResourceResponse? {
        if (uri.scheme != "https" || uri.host != LOCAL_HOST) return null
        val prefix = "/private-scene/"
        val path = uri.path ?: return null
        if (!path.startsWith(prefix)) return null
        val name = path.removePrefix(prefix)
        if (!SAFE_NAME.matches(name)) return blockedResponse()
        val file = File(directory, name)
        if (!file.isFile || file.canonicalFile.parentFile != directory.canonicalFile) return notFoundResponse()
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "jpg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> return blockedResponse()
        }
        return WebResourceResponse(mime, null, 200, "OK", mapOf("Cache-Control" to "private, max-age=31536000, immutable", "X-Content-Type-Options" to "nosniff"), FileInputStream(file))
    }

    private data class StoredImage(val file: File, val type: ImageType, val width: Int, val height: Int)

    private fun normalizeForMobile(file: File, type: ImageType, width: Int, height: Int): StoredImage {
        if (width <= MAX_STORED_DIMENSION && height <= MAX_STORED_DIMENSION && width.toLong() * height.toLong() <= MAX_STORED_PIXELS) {
            return StoredImage(file, type, width, height)
        }
        var sample = 1
        while (maxOf(width / sample, height / sample) > MAX_STORED_DIMENSION * 2) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalArgumentException("Unable to decode scene image")
        val maxSide = maxOf(decoded.width, decoded.height).toDouble()
        val pixelScale = kotlin.math.sqrt(MAX_STORED_PIXELS.toDouble() / (decoded.width.toLong() * decoded.height.toLong()).toDouble()).coerceAtMost(1.0)
        val dimensionScale = (MAX_STORED_DIMENSION / maxSide).coerceAtMost(1.0)
        val scale = minOf(pixelScale, dimensionScale)
        val targetWidth = maxOf(1, kotlin.math.round(decoded.width * scale).toInt())
        val targetHeight = maxOf(1, kotlin.math.round(decoded.height * scale).toInt())
        val scaled = if (targetWidth == decoded.width && targetHeight == decoded.height) decoded else Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
        val normalized = File.createTempFile("scene-mobile-", ".${type.extension}", context.cacheDir)
        try {
            FileOutputStream(normalized).use { out ->
                val format = when (type) {
                    ImageType.PNG -> Bitmap.CompressFormat.PNG
                    ImageType.JPEG -> Bitmap.CompressFormat.JPEG
                    ImageType.WEBP -> Bitmap.CompressFormat.WEBP
                }
                require(scaled.compress(format, if (type == ImageType.PNG) 100 else 90, out)) { "Unable to encode scene image" }
            }
        } catch (t: Throwable) { normalized.delete(); throw t } finally {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
        return StoredImage(normalized, type, targetWidth, targetHeight)
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun detectType(bytes: ByteArray): ImageType? = when {
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> ImageType.PNG
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> ImageType.JPEG
        bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> ImageType.WEBP
        else -> null
    }

    private enum class ImageType(val mimeType: String, val extension: String, val acceptedMimeTypes: Set<String>) {
        PNG("image/png", "png", setOf("image/png")),
        JPEG("image/jpeg", "jpg", setOf("image/jpeg", "image/jpg")),
        WEBP("image/webp", "webp", setOf("image/webp")),
    }

    companion object {
        const val LOCAL_HOST = "appassets.androidplatform.net"
        const val LOCAL_ORIGIN = "https://$LOCAL_HOST"
        private const val MAX_IMAGE_BYTES = 12L * 1024 * 1024
        private const val MAX_DIMENSION = 8192
        private const val MAX_PIXELS = 40_000_000L
        private const val MAX_STORED_DIMENSION = 2048
        private const val MAX_STORED_PIXELS = 4_000_000L
        private val SAFE_NAME = Regex("^[a-f0-9]{32}\\.(png|jpg|webp)$")
        fun blockedResponse() = WebResourceResponse("text/plain", "utf-8", 403, "Blocked", mapOf("Cache-Control" to "no-store"), ByteArrayInputStream("Blocked".toByteArray()))
        private fun notFoundResponse() = WebResourceResponse("text/plain", "utf-8", 404, "Not Found", mapOf("Cache-Control" to "no-store"), ByteArrayInputStream("Not Found".toByteArray()))
    }
}
