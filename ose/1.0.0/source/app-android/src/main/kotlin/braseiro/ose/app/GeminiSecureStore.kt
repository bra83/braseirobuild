package braseiro.ose.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/**
 * Stores Gemini credentials encrypted by an Android Keystore key inside noBackupFilesDir.
 * Plaintext keys are never returned to the WebView and never written to logs/backups.
 */
class GeminiCredentialStore(private val context: Context) {
    enum class Slot(val wireName: String) { TEXT_SCENE("TEXT_SCENE"), TTS_AUDIO("TTS_AUDIO") }

    private val alias = "braseiro_ose_gemini_credentials_v1"
    private val file: File get() = File(context.noBackupFilesDir, "gemini_credentials_v1.json")
    private val lock = Any()

    fun has(slot: Slot): Boolean = synchronized(lock) { readEnvelope().has(slot.wireName) }

    fun set(slot: Slot, rawValue: String) = synchronized(lock) {
        val value = rawValue.trim()
        require(value.isNotEmpty()) { "EMPTY_GEMINI_KEY" }
        require(value.length <= 4096) { "GEMINI_KEY_TOO_LONG" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val entry = JSONObject()
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        val all = readEnvelope().put(slot.wireName, entry)
        writeEnvelope(all)
    }

    fun get(slot: Slot): String? = synchronized(lock) {
        val entry = readEnvelope().optJSONObject(slot.wireName) ?: return@synchronized null
        val iv = Base64.decode(entry.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(entry.getString("ciphertext"), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    fun delete(slot: Slot) = synchronized(lock) {
        val all = readEnvelope()
        all.remove(slot.wireName)
        writeEnvelope(all)
    }

    fun statusJson(): JSONObject = JSONObject()
        .put("textSceneConfigured", has(Slot.TEXT_SCENE))
        .put("ttsAudioConfigured", has(Slot.TTS_AUDIO))

    private fun readEnvelope(): JSONObject = try {
        if (!file.exists()) JSONObject() else JSONObject(file.readText(Charsets.UTF_8))
    } catch (_: Throwable) {
        JSONObject()
    }

    private fun writeEnvelope(json: JSONObject) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(json.toString(), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(json.toString(), Charsets.UTF_8)
            tmp.delete()
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
