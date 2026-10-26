package com.braseiro.talesvtt.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local secrets encrypted with Android Keystore; adapted from the D&D Gold Standard. */
class SecureSecretStore(context: Context) {
  private val prefs = context.applicationContext.getSharedPreferences("secure_secrets", Context.MODE_PRIVATE)

  fun put(name: String, value: String?): Boolean = runCatching {
    val keyName = safe(name)
    require(keyName.isNotBlank())
    if (value.isNullOrEmpty()) return prefs.edit().remove("secure_$keyName").commit()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val iv = cipher.iv
    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    val data = ByteBuffer.allocate(4 + iv.size + encrypted.size).putInt(iv.size).put(iv).put(encrypted).array()
    prefs.edit().putString("secure_$keyName", Base64.encodeToString(data, Base64.NO_WRAP)).commit()
  }.getOrDefault(false)

  fun get(name: String): String = runCatching {
    val raw = prefs.getString("secure_${safe(name)}", "").orEmpty()
    if (raw.isBlank()) return ""
    val data = ByteBuffer.wrap(Base64.decode(raw, Base64.NO_WRAP))
    val ivLength = data.int
    require(ivLength in 12..32 && data.remaining() > ivLength)
    val iv = ByteArray(ivLength).also(data::get)
    val encrypted = ByteArray(data.remaining()).also(data::get)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
    String(cipher.doFinal(encrypted), Charsets.UTF_8)
  }.getOrDefault("")

  fun has(name: String): Boolean = get(name).isNotBlank()
  fun remove(name: String): Boolean = put(name, null)

  private fun safe(name: String) = name.trim().replace(Regex("[^A-Za-z0-9_.-]"), "_")

  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return generator.generateKey()
  }

  companion object { private const val KEY_ALIAS = "braseiro_tales_local_secrets_v1" }
}
