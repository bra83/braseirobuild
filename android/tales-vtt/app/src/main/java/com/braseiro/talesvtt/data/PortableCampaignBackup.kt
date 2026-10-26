package com.braseiro.talesvtt.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.provider.DocumentsContract
import com.braseiro.talesvtt.game.GameSnapshotCodec
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.GmPrivateSnapshotCodec
import com.braseiro.talesvtt.game.GmPrivateState
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BACKUP_FORMAT = "braseiro-tales-backup"
private const val BACKUP_VERSION = 1

data class CampaignBackupPayload(val game: GameState, val gmPrivate: GmPrivateState)

object CampaignBackupCodec {
  fun encode(game: GameState, gmPrivate: GmPrivateState): String {
    val publicSnapshot = GameSnapshotCodec.encode(game)
    val privateSnapshot = GmPrivateSnapshotCodec.encode(gmPrivate)
    val checksum = sha256("$publicSnapshot\n--GM--\n$privateSnapshot")
    return JSONObject()
      .put("format", BACKUP_FORMAT)
      .put("version", BACKUP_VERSION)
      .put("profile", "tales_kid")
      .put("created_at", System.currentTimeMillis())
      .put("public_state", publicSnapshot)
      .put("gm_private_state", privateSnapshot)
      .put("sha256", checksum)
      .toString(2)
  }

  fun decode(text: String): CampaignBackupPayload {
    val root = JSONObject(text)
    require(root.optString("format") == BACKUP_FORMAT) { "Backup não pertence ao BRASEIRO Tales." }
    require(root.optInt("version") in 1..BACKUP_VERSION) { "Versão de backup não suportada." }
    require(root.optString("profile") == "tales_kid") { "Backup não é do perfil Tales/Kids." }
    val publicSnapshot = root.getString("public_state")
    val privateSnapshot = root.getString("gm_private_state")
    val expected = root.optString("sha256")
    if (expected.isNotBlank()) {
      require(expected == sha256("$publicSnapshot\n--GM--\n$privateSnapshot")) { "Checksum do backup inválido." }
    }
    val game = requireNotNull(GameSnapshotCodec.decode(publicSnapshot)) { "Estado público do backup é inválido." }
    val gm = requireNotNull(GmPrivateSnapshotCodec.decode(privateSnapshot)) { "Estado privado do Mestre é inválido." }
    return CampaignBackupPayload(game, gm)
  }

  private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
    .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

class PortableCampaignBackup(private val context: Context) {
  private val resolver = context.contentResolver
  private val prefs = context.getSharedPreferences("tftl_portable_backup", Context.MODE_PRIVATE)

  fun write(uri: Uri, game: GameState, gmPrivate: GmPrivateState) {
    val root = JSONObject(CampaignBackupCodec.encode(game, gmPrivate))
    game.scene.importedImageUri?.takeIf { it.isNotBlank() }?.let { imageUri ->
      runCatching {
        val bytes = resolver.openInputStream(Uri.parse(imageUri))?.use { it.readBytes() }
          ?: Uri.parse(imageUri).path?.let { java.io.File(it).takeIf(java.io.File::isFile)?.readBytes() }
        if (bytes != null && bytes.size <= 25 * 1024 * 1024) {
          root.put("attachments", JSONObject().put("scene_image_base64", Base64.encodeToString(bytes, Base64.NO_WRAP)))
        }
      }
    }
    val serialized = root.toString(2)
    resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8).use { out ->
      requireNotNull(out) { "Não foi possível abrir o destino do backup." }
      out.write(serialized)
      out.flush()
    }
    // Round-trip verification, adapted from the D&D Gold Standard.
    val reread = readText(uri)
    CampaignBackupCodec.decode(reread)
    require(serialized == reread) { "O provedor alterou o backup durante a gravação." }
  }

  fun read(uri: Uri): CampaignBackupPayload {
    val raw = readText(uri)
    var payload = CampaignBackupCodec.decode(raw)
    val attachment = runCatching { JSONObject(raw).optJSONObject("attachments")?.optString("scene_image_base64").orEmpty() }.getOrDefault("")
    if (attachment.isNotBlank()) {
      val bytes = Base64.decode(attachment, Base64.DEFAULT)
      require(bytes.size <= 25 * 1024 * 1024) { "Imagem anexada ao backup excede 25 MB." }
      val dir = java.io.File(context.filesDir, "scene_images").apply { mkdirs() }
      val file = java.io.File(dir, "restored_${System.currentTimeMillis()}.img").apply { writeBytes(bytes) }
      payload = payload.copy(game = payload.game.copy(scene = payload.game.scene.copy(importedImageUri = Uri.fromFile(file).toString())))
    }
    return payload
  }

  fun configureAutomaticDirectory(treeUri: Uri) {
    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    runCatching { resolver.takePersistableUriPermission(treeUri, flags) }
    prefs.edit().putString("tree_uri", treeUri.toString()).apply()
  }

  fun automaticDirectory(): Uri? = prefs.getString("tree_uri", null)?.let(Uri::parse)

  fun createAutomaticBackup(game: GameState, gmPrivate: GmPrivateState, retention: Int = 5): String? {
    val tree = automaticDirectory() ?: return null
    val treeId = DocumentsContract.getTreeDocumentId(tree)
    val root = DocumentsContract.buildDocumentUriUsingTree(tree, treeId)
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val name = "Braseiro_Tales_$stamp.json"
    val doc = DocumentsContract.createDocument(resolver, root, "application/json", name)
      ?: error("O provedor não criou o backup automático.")
    var complete = false
    try {
      write(doc, game, gmPrivate)
      complete = true
      prune(tree, retention.coerceIn(2, 20))
      prefs.edit().putLong("last_auto_backup", System.currentTimeMillis()).apply()
      return name
    } finally {
      if (!complete) runCatching { DocumentsContract.deleteDocument(resolver, doc) }
    }
  }

  fun shouldAutoBackup(intervalMs: Long = 15 * 60 * 1000L): Boolean {
    if (automaticDirectory() == null) return false
    val last = prefs.getLong("last_auto_backup", 0L)
    return System.currentTimeMillis() - last >= intervalMs
  }

  private fun prune(tree: Uri, keep: Int) {
    val treeId = DocumentsContract.getTreeDocumentId(tree)
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, treeId)
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_LAST_MODIFIED
    )
    val rows = mutableListOf<Triple<String, String, Long>>()
    resolver.query(children, projection, null, null, null)?.use { cursor ->
      val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
      val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
      val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
      while (cursor.moveToNext()) {
        val id: String = cursor.getString(idCol) ?: continue
        val name: String = cursor.getString(nameCol) ?: ""
        val modified = if (modCol >= 0 && !cursor.isNull(modCol)) cursor.getLong(modCol) else 0L
        if (name.startsWith("Braseiro_Tales_") && name.endsWith(".json", true)) rows += Triple(id, name, modified)
      }
    }
    rows.sortedByDescending { it.third }.drop(keep).forEach { (id, _, _) ->
      runCatching { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
    }
  }

  private fun readText(uri: Uri): String {
    val input = resolver.openInputStream(uri) ?: error("Não foi possível ler o arquivo.")
    return BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
  }
}
