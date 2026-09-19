package com.braseiro.oumanel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Caixa de entrada privada para imagens compartilhadas por outros aplicativos.
 *
 * O receptor copia a URI temporária concedida pelo aplicativo de origem para o
 * armazenamento interno antes de abrir a mesa. Assim a MainActivity não depende
 * da tarefa do Gemini, nem de uma permissão de URI que pode desaparecer quando o
 * compartilhamento termina.
 */
object SharedImageInbox {
    const val ACTION_IMPORT_SHARED_IMAGE = "com.braseiro.oumanel.action.IMPORT_SHARED_IMAGE"
    const val EXTRA_IMPORT_ERROR = "com.braseiro.oumanel.extra.IMPORT_ERROR"

    private const val PREFS = "braseiro_oumanel_private"
    private const val KEY_FILE = "pending_shared_image_file"
    private const val KEY_MIME = "pending_shared_image_mime"
    private const val KEY_TOKEN = "pending_shared_image_token"
    private const val KEY_CREATED_AT = "pending_shared_image_created_at"
    private const val MAX_BYTES = 64L * 1024L * 1024L

    data class StoredImage(val file: File, val mimeType: String, val token: String)

    fun directory(context: Context): File = File(context.filesDir, "shared_image_inbox").apply { mkdirs() }

    @Suppress("DEPRECATION")
    fun firstUri(intent: Intent): Uri? {
        val single = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        val multiple: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
        return single
            ?: multiple?.firstOrNull()
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: intent.data
    }

    private fun displayName(context: Context, uri: Uri): String {
        var name: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0)
        }
        return name ?: uri.lastPathSegment.orEmpty()
    }

    private fun extensionFor(context: Context, uri: Uri, mimeType: String): String {
        val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.lowercase()
        if (!fromMime.isNullOrBlank()) return fromMime
        val fromName = displayName(context, uri).substringAfterLast('.', "").lowercase()
        return fromName.takeIf { it in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "avif") } ?: "img"
    }

    fun storeFromIntent(context: Context, intent: Intent): StoredImage {
        require(intent.action in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            "O compartilhamento recebido não contém uma imagem."
        }
        val uri = firstUri(intent) ?: throw IllegalStateException("O aplicativo de origem não forneceu a imagem compartilhada.")
        val mimeType = intent.type.orEmpty().ifBlank { context.contentResolver.getType(uri).orEmpty() }
        if (mimeType.isNotBlank() && !mimeType.startsWith("image/")) {
            throw IllegalStateException("O conteúdo compartilhado não é uma imagem.")
        }

        val token = UUID.randomUUID().toString().replace("-", "")
        val extension = extensionFor(context, uri, mimeType)
        val folder = directory(context)
        folder.listFiles()?.forEach { it.delete() }
        val temporary = File(folder, ".$token.tmp")
        val target = File(folder, "shared_$token.$extension")

        var copied = 0L
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > MAX_BYTES) throw IllegalStateException("A imagem compartilhada excede 64 MB.")
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            } ?: throw IllegalStateException("O Android não conseguiu abrir a imagem compartilhada.")

            if (copied <= 0L) throw IllegalStateException("A imagem compartilhada está vazia ou indisponível.")
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
        } catch (failure: Exception) {
            temporary.delete()
            target.delete()
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_FILE).remove(KEY_MIME).remove(KEY_TOKEN).remove(KEY_CREATED_AT).apply()
            throw failure
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_FILE, target.name)
            .putString(KEY_MIME, mimeType.ifBlank { "image/*" })
            .putString(KEY_TOKEN, token)
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()
        return StoredImage(target, mimeType.ifBlank { "image/*" }, token)
    }

    fun pendingWebUrl(context: Context, trustedOrigin: String): String? {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fileName = preferences.getString(KEY_FILE, null)?.takeIf { it.matches(Regex("[A-Za-z0-9._-]+")) } ?: return null
        val file = File(directory(context), fileName)
        if (!file.isFile || file.length() <= 0L) {
            clear(context)
            return null
        }
        val token = preferences.getString(KEY_TOKEN, null).orEmpty()
        return "$trustedOrigin/shared-image/$fileName?v=$token"
    }

    fun mimeTypeFor(context: Context, fileName: String): String {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return preferences.getString(KEY_MIME, null)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substringAfterLast('.', "").lowercase())
            ?: "image/*"
    }

    fun clear(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fileName = preferences.getString(KEY_FILE, null)
        val deleted = fileName?.let { File(directory(context), it).delete() } ?: true
        preferences.edit().remove(KEY_FILE).remove(KEY_MIME).remove(KEY_TOKEN).remove(KEY_CREATED_AT).apply()
        directory(context).listFiles()?.filter { it.name.startsWith(".") }?.forEach { it.delete() }
        return deleted
    }
}
