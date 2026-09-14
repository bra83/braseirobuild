package com.braseiro.talesvtt.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

data class ManualEntry(
  val id: String,
  val title: String,
  val pages: Int,
  val chunks: Int,
  val indexedAt: Long,
  val sourceHash: String
)

data class RagHit(
  val manualId: String,
  val title: String,
  val page: Int,
  val snippet: String,
  val score: Int
)

data class ManualLibraryStatus(
  val manuals: List<ManualEntry> = emptyList(),
  val busy: Boolean = false,
  val progress: String = "Biblioteca pronta.",
  val results: List<RagHit> = emptyList(),
  val lastError: String? = null
)

/**
 * Local-only PDF index adapted from the D&D Gold Standard LocalManualIndexer.
 * PDFs are copied into app-private storage, text is extracted with PDFBox and OCR is
 * used only for image/scanned pages. No PDF bytes are sent to Gemini.
 */
class ManualLibraryRepository(private val context: Context) {
  private val base = File(context.filesDir, "manual_library").apply { mkdirs() }
  private val metaDir = File(base, "meta").apply { mkdirs() }
  private val pdfDir = File(base, "pdf").apply { mkdirs() }
  private val indexDir = File(base, "index").apply { mkdirs() }

  init { PDFBoxResourceLoader.init(context.applicationContext) }

  fun listManuals(): List<ManualEntry> = metaDir.listFiles { f -> f.extension == "json" }?.mapNotNull { file ->
    runCatching {
      val j = JSONObject(file.readText())
      ManualEntry(
        id = j.getString("id"), title = j.getString("title"), pages = j.getInt("pages"),
        chunks = j.getInt("chunks"), indexedAt = j.getLong("indexed_at"), sourceHash = j.getString("source_hash")
      )
    }.getOrNull()
  }?.sortedBy { it.title.lowercase(Locale.getDefault()) } ?: emptyList()

  suspend fun importAndIndex(uri: Uri, onProgress: (String) -> Unit = {}): ManualEntry = withContext(Dispatchers.IO) {
    val displayName = queryName(uri) ?: "manual_${System.currentTimeMillis()}.pdf"
    require(displayName.endsWith(".pdf", true)) { "Selecione um arquivo PDF." }
    onProgress("Copiando $displayName…")
    val tmp = File.createTempFile("manual_", ".pdf", pdfDir)
    context.contentResolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { output -> input.copyTo(output, 64 * 1024) } }
      ?: error("Não foi possível abrir $displayName.")
    require(tmp.length() > 1024) { "PDF vazio ou inválido." }
    val hash = sha256(tmp)
    val id = hash.take(20)
    val target = File(pdfDir, "$id.pdf")
    if (target.exists()) tmp.delete() else tmp.renameTo(target)
    val existing = File(metaDir, "$id.json")
    if (existing.exists() && File(indexDir, "$id.jsonl").exists()) return@withContext listManuals().first { it.id == id }

    onProgress("Extraindo texto de $displayName…")
    val index = File(indexDir, "$id.jsonl")
    var pages = 0
    var chunks = 0
    index.bufferedWriter(Charsets.UTF_8).use { writer ->
      PDDocument.load(target).use { doc ->
        pages = doc.numberOfPages
        val stripper = PDFTextStripper()
        for (pageIndex in 0 until pages) {
          onProgress("Indexando $displayName • página ${pageIndex + 1}/$pages")
          stripper.startPage = pageIndex + 1
          stripper.endPage = pageIndex + 1
          var text = stripper.getText(doc).trim()
          if (text.length < 60) text = ocrPage(target, pageIndex).trim()
          chunk(text).forEach { piece ->
            writer.append(JSONObject().put("page", pageIndex + 1).put("text", piece).toString()).append('\n')
            chunks++
          }
        }
      }
    }
    val entry = ManualEntry(id, displayName.removeSuffix(".pdf"), pages, chunks, System.currentTimeMillis(), hash)
    File(metaDir, "$id.json").writeText(JSONObject()
      .put("id", entry.id).put("title", entry.title).put("pages", pages).put("chunks", chunks)
      .put("indexed_at", entry.indexedAt).put("source_hash", hash).toString(2))
    onProgress("$displayName indexado: $pages páginas, $chunks trechos.")
    entry
  }

  fun search(query: String, limit: Int = 6): List<RagHit> {
    val terms = normalize(query).split(' ').filter { it.length >= 3 }.distinct().take(14)
    if (terms.isEmpty()) return emptyList()
    val entries = listManuals().associateBy { it.id }
    val hits = mutableListOf<RagHit>()
    indexDir.listFiles { f -> f.extension == "jsonl" }?.forEach { file ->
      val manualId = file.nameWithoutExtension
      val meta = entries[manualId] ?: return@forEach
      file.useLines { lines ->
        lines.forEach { line ->
          val j = runCatching { JSONObject(line) }.getOrNull() ?: return@forEach
          val text = j.optString("text")
          val normalized = normalize(text)
          var score = 0
          terms.forEach { term ->
            val occurrences = Regex("\\b${Regex.escape(term)}").findAll(normalized).count()
            score += min(occurrences, 4) * 3
            if (normalized.contains(term)) score += 1
          }
          if (score > 0) hits += RagHit(manualId, meta.title, j.optInt("page", 1), compactSnippet(text, terms), score)
        }
      }
    }
    return hits.sortedWith(compareByDescending<RagHit> { it.score }.thenBy { it.title }.thenBy { it.page }).take(limit)
  }

  fun removeManual(id: String) {
    File(metaDir, "$id.json").delete()
    File(indexDir, "$id.jsonl").delete()
    File(pdfDir, "$id.pdf").delete()
  }

  private fun chunk(text: String, size: Int = 1200, overlap: Int = 180): List<String> {
    val clean = text.replace(Regex("\\s+"), " ").trim()
    if (clean.isBlank()) return emptyList()
    if (clean.length <= size) return listOf(clean)
    val out = mutableListOf<String>()
    var start = 0
    while (start < clean.length) {
      var end = min(clean.length, start + size)
      if (end < clean.length) {
        val boundary = clean.lastIndexOfAny(charArrayOf('.', '!', '?', ';'), end)
        if (boundary > start + size / 2) end = boundary + 1
      }
      out += clean.substring(start, end).trim()
      if (end >= clean.length) break
      start = (end - overlap).coerceAtLeast(start + 1)
    }
    return out
  }

  private fun ocrPage(pdf: File, pageIndex: Int): String {
    val pfd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
    return pfd.use { descriptor ->
      PdfRenderer(descriptor).use { renderer ->
        if (pageIndex !in 0 until renderer.pageCount) return@use ""
        renderer.openPage(pageIndex).use { page ->
          val maxW = 1600
          val scale = (maxW.toFloat() / page.width).coerceAtMost(2f).coerceAtLeast(1f)
          val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
          page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
          try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val task = recognizer.process(InputImage.fromBitmap(bitmap, 0))
            val text = Tasks.await(task, 45, TimeUnit.SECONDS).text
            recognizer.close()
            text
          } finally { bitmap.recycle() }
        }
      }
    }
  }

  private fun queryName(uri: Uri): String? = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
    if (c.moveToFirst()) c.getString(0) else null
  }

  private fun compactSnippet(text: String, terms: List<String>): String {
    val clean = text.replace(Regex("\\s+"), " ").trim()
    if (clean.length <= 420) return clean
    val normalized = normalize(clean)
    val pos = terms.map { normalized.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
    val start = (pos - 120).coerceAtLeast(0)
    val end = (start + 420).coerceAtMost(clean.length)
    return (if (start > 0) "…" else "") + clean.substring(start, end) + if (end < clean.length) "…" else ""
  }

  private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9á-úç ]"), " ").replace(Regex("\\s+"), " ").trim()

  private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
      val buffer = ByteArray(64 * 1024)
      while (true) {
        val n = input.read(buffer)
        if (n <= 0) break
        digest.update(buffer, 0, n)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }
}
