package com.braseiro.wwn.bridge

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.ln

class RagRuntime(private val context: Context) {
    init { PDFBoxResourceLoader.init(context.applicationContext) }

    @Volatile private var cachedRoot: JSONObject? = null
    private val indexLock = Any()

    fun status(): JSONObject = try {
        val root = loadIndex()
        JSONObject().put("status", "OK").put("schema", INDEX_SCHEMA)
            .put("documentCount", root.getJSONArray("documents").length())
            .put("storage", "ANDROID_PRIVATE_FILE")
            .put("mechanicalAuthority", false)
            .put("ruleBindingRequired", true)
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message) }

    fun listDocuments(): JSONObject = try {
        val docs = loadIndex().getJSONArray("documents")
        val out = JSONArray()
        for (i in 0 until docs.length()) {
            val d = docs.getJSONObject(i)
            out.put(JSONObject().put("documentId", d.getString("documentId")).put("title", d.getString("title"))
                .put("authorityProfile", d.getString("authorityProfile")).put("visibility", d.getString("visibility"))
                .put("pageCount", d.getInt("pageCount")).put("chunkCount", d.getJSONArray("chunks").length())
                .put("sourceHash", d.getString("sourceHash")))
        }
        JSONObject().put("status", "OK").put("documents", out)
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).put("documents", JSONArray()) }

    fun removeDocument(documentId: String): JSONObject = try {
        val id = documentId.trim(); require(id.isNotBlank()) { "documentId required" }
        val root = loadIndex(); val docs = root.getJSONArray("documents"); val out = JSONArray(); var removed = false
        for (i in 0 until docs.length()) { val d = docs.getJSONObject(i); if (d.optString("documentId") == id) removed = true else out.put(d) }
        if (removed) { root.put("documents", out); saveIndex(root) }
        JSONObject().put("status", if (removed) "OK" else "NOT_FOUND").put("documentId", id)
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message) }

    fun importPdf(uri: Uri, displayName: String?, requestedAuthorityProfile: String, progress: ((Int, Int) -> Unit)? = null): JSONObject {
        val profile = normalizeProfile(requestedAuthorityProfile)
        val temp = File.createTempFile("wwn-rag-", ".pdf", context.cacheDir)
        return try {
            val digest = MessageDigest.getInstance("SHA-256"); var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(temp).use { output ->
                val buf = ByteArray(64 * 1024); while (true) { val n = input.read(buf); if (n < 0) break; total += n; require(total <= MAX_PDF_BYTES) { "PDF too large" }; digest.update(buf, 0, n); output.write(buf, 0, n) }
            } } ?: throw IllegalStateException("Unable to read selected PDF")
            require(total > 4 && temp.inputStream().use { val h = ByteArray(4); it.read(h); String(h, Charsets.ISO_8859_1) } == "%PDF") { "Selected file is not a PDF" }
            val sourceHash = digest.digest().joinToString("") { "%02x".format(it) }
            val documentId = "pdf-${sourceHash.take(24)}"
            val root = loadIndex(); val docs = root.getJSONArray("documents")
            for (i in 0 until docs.length()) if (docs.getJSONObject(i).optString("documentId") == documentId)
                return JSONObject().put("status", "ALREADY_INDEXED").put("documentId", documentId).put("sourceHash", sourceHash)

            val pages = JSONArray(); val chunks = JSONArray(); var charCount = 0
            val extractedPages = mutableListOf<String>()
            var totalPages = 0
            PDDocument.load(temp).use { pdf ->
                require(pdf.numberOfPages in 1..MAX_PAGES) { "Unsupported PDF page count" }
                val stripper = PDFTextStripper().apply { sortByPosition = true }
                totalPages = pdf.numberOfPages
                for (page in 1..totalPages) {
                    stripper.startPage = page; stripper.endPage = page
                    val text = normalize(stripper.getText(pdf)); extractedPages += text
                    if (page == 1 || page == totalPages || page % 5 == 0) progress?.invoke(page, totalPages)
                }
            }
            val needsOcr = extractedPages.withIndex().filter { it.value.length < MIN_TEXT_CHARS_PER_PAGE }.map { it.index }
            if (needsOcr.isNotEmpty() && totalPages <= MAX_OCR_PAGES) {
                val ocr = ocrPages(temp, needsOcr.toSet(), totalPages, progress)
                for ((index, text) in ocr) if (text.length > extractedPages[index].length) extractedPages[index] = text
            }
            for ((index, text) in extractedPages.withIndex()) {
                charCount += text.length; require(charCount <= MAX_EXTRACTED_CHARS) { "PDF extracted text too large" }
                val page = index + 1
                pages.put(JSONObject().put("pageNumber", page).put("charCount", text.length).put("ocr", needsOcr.contains(index) && text.isNotBlank()))
                chunkText(text).forEachIndexed { chunkIndex, part -> chunks.put(JSONObject().put("page", page).put("chunkIndex", chunkIndex).put("text", part)) }
            }
            require(chunks.length() > 0) { if (totalPages > MAX_OCR_PAGES) "PDF has no extractable text layer; scanned PDFs are supported up to $MAX_OCR_PAGES pages" else "PDF has no readable text after OCR" }
            val title = displayName?.trim().orEmpty().ifBlank { "Documento WWN" }.take(160)
            val visibility = if (profile == "GM_PRIVATE_REFERENCE") "GM_ONLY" else "PUBLIC"
            val doc = JSONObject().put("documentId", documentId).put("title", title).put("authorityProfile", profile)
                .put("visibility", visibility).put("rulesAuthority", profile == "WWN_RULEBOOK_AUTHORIZED")
                .put("narrativeEligible", profile != "GM_PRIVATE_REFERENCE").put("sourceHash", sourceHash)
                .put("byteCount", total).put("pageCount", pages.length()).put("charCount", charCount).put("pages", pages).put("chunks", chunks)
            docs.put(doc); require(root.toString().length <= MAX_INDEX_JSON_CHARS) { "RAG index capacity exceeded" }; saveIndex(root)
            JSONObject().put("status", "OK").put("documentId", documentId).put("title", title).put("authorityProfile", profile)
                .put("pageCount", pages.length()).put("chunkCount", chunks.length()).put("sourceHash", sourceHash)
                .put("mechanicalAuthority", false).put("ruleBindingRequired", true)
        } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).put("mechanicalAuthority", false) }
        finally { temp.delete() }
    }

    fun search(requestJson: String): JSONObject {
        return try {
        require(requestJson.length <= 50_000) { "RAG request too large" }
        val req = JSONObject(requestJson); val query = normalize(req.optString("query")); require(query.isNotBlank()) { "query required" }
        val scope = req.optString("scope", "RULES").uppercase(); require(scope in setOf("RULES", "NARRATIVE_PUBLIC", "GM")) { "invalid RAG scope" }
        val limit = req.optInt("limit", 5).coerceIn(1, 20); val queryTokens = tokens(query); require(queryTokens.isNotEmpty()) { "query has no searchable terms" }
        val docs = loadIndex().getJSONArray("documents"); val eligible = mutableListOf<JSONObject>()
        for (i in 0 until docs.length()) { val d = docs.getJSONObject(i); if (allowed(d, scope)) eligible += d }
        if (eligible.isEmpty()) return JSONObject().put("status", "MISSING_EVIDENCE").put("results", JSONArray()).put("mechanicalAuthority", false).put("ruleBindingRequired", true)
        data class PreparedChunk(val chunk: JSONObject, val tf: Map<String, Int>)
        data class PreparedDoc(val doc: JSONObject, val chunks: List<PreparedChunk>, val seenQueryTokens: Set<String>)
        val prepared = eligible.map { d ->
            val cs=d.getJSONArray("chunks"); val pcs=ArrayList<PreparedChunk>(cs.length()); val seen=mutableSetOf<String>()
            for(i in 0 until cs.length()) { val c=cs.getJSONObject(i); val ts=tokens(c.getString("text")); val tf=ts.groupingBy{it}.eachCount(); queryTokens.forEach { if ((tf[it]?:0)>0) seen += it }; pcs += PreparedChunk(c,tf) }
            PreparedDoc(d,pcs,seen)
        }
        val df=queryTokens.associateWith { token -> prepared.count { token in it.seenQueryTokens } }
        data class Hit(val score: Double, val doc: JSONObject, val chunk: JSONObject)
        val hits=mutableListOf<Hit>()
        prepared.forEach { pd -> pd.chunks.forEach { pc -> var score=0.0; for(q in queryTokens){ val count=pc.tf[q]?:0; if(count>0){ val idf=ln(1.0+(eligible.size+1.0)/((df[q]?:0)+1.0)); score+=(1.0+ln(count.toDouble()))*idf } }; if(score>0)hits += Hit(score,pd.doc,pc.chunk) } }
        val sorted = hits.sortedWith(compareByDescending<Hit>{it.score}.thenBy{it.doc.getString("documentId")}.thenBy{it.chunk.getInt("page")}.thenBy{it.chunk.getInt("chunkIndex")}).take(limit)
        val results = JSONArray(); sorted.forEach { h -> results.put(JSONObject().put("score", h.score).put("documentId", h.doc.getString("documentId"))
            .put("sourceId", h.doc.getString("documentId")).put("title", h.doc.getString("title")).put("page", h.chunk.getInt("page"))
            .put("chunkIndex", h.chunk.getInt("chunkIndex")).put("text", h.chunk.getString("text")).put("authorityProfile", h.doc.getString("authorityProfile"))
            .put("visibility", h.doc.getString("visibility")).put("sourceHash", h.doc.getString("sourceHash")).put("mechanicalAuthority", false).put("ruleBindingRequired", true)) }
        JSONObject().put("status", if (results.length() > 0) "OK" else "MISSING_EVIDENCE").put("scope", scope).put("results", results)
            .put("mechanicalAuthority", false).put("ruleBindingRequired", true)
    } catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message).put("results", JSONArray()).put("mechanicalAuthority", false).put("ruleBindingRequired", true) }
    }

    private fun ocrPages(file: File, pageIndexes: Set<Int>, totalPages: Int, progress: ((Int, Int) -> Unit)?): Map<Int, String> {
        if (pageIndexes.isEmpty()) return emptyMap()
        val out=mutableMapOf<Int,String>(); val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    for (index in pageIndexes.sorted()) {
                        renderer.openPage(index).use { page ->
                            val scale=(OCR_TARGET_WIDTH.toDouble()/page.width.toDouble()).coerceIn(1.0,2.4)
                            val bitmap=Bitmap.createBitmap((page.width*scale).toInt().coerceAtLeast(1),(page.height*scale).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val text=normalize(Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)), OCR_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS).text)
                            bitmap.recycle(); if(text.isNotBlank())out[index]=text
                        }
                        progress?.invoke(index+1,totalPages)
                    }
                }
            }
        } finally { recognizer.close() }
        return out
    }

    private fun allowed(d: JSONObject, scope: String): Boolean { val p = d.optString("authorityProfile"); val vis = d.optString("visibility"); return when(scope){ "RULES" -> p == "WWN_RULEBOOK_AUTHORIZED" && vis == "PUBLIC"; "NARRATIVE_PUBLIC" -> p == "CAMPAIGN_REFERENCE" && vis == "PUBLIC"; else -> true } }
    private fun allChunkText(d: JSONObject): String { val a = d.getJSONArray("chunks"); val b = StringBuilder(); for (i in 0 until a.length()) b.append(' ').append(a.getJSONObject(i).optString("text")); return b.toString() }
    private fun normalize(s: String): String = s.replace('\u0000',' ').replace(Regex("\\s+"), " ").trim()
    private fun tokens(s: String): List<String> = normalize(s).lowercase().replace(Regex("[^\\p{L}\\p{N}_-]+"), " ").split(Regex("\\s+")).filter { it.length > 1 && it !in STOPWORDS }
    private fun chunkText(text: String): List<String> { if (text.isBlank()) return emptyList(); val words = text.split(' ').filter { it.isNotBlank() }; val out = mutableListOf<String>(); var current = mutableListOf<String>(); for (word in words) { if (current.isNotEmpty() && (current.joinToString(" ").length + 1 + word.length) > CHUNK_CHARS) { out += current.joinToString(" "); val overlap = mutableListOf<String>(); var n = 0; for (i in current.indices.reversed()) { val w = current[i]; if (n + w.length + 1 > CHUNK_OVERLAP) break; overlap.add(0,w); n += w.length + 1 }; current = overlap }; current += word }; if(current.isNotEmpty()) out += current.joinToString(" "); return out }
    private fun normalizeProfile(value: String): String { val p = value.trim().uppercase(); require(p in setOf("WWN_RULEBOOK_AUTHORIZED","CAMPAIGN_REFERENCE","GM_PRIVATE_REFERENCE")) { "Invalid authority profile" }; return p }
    private fun loadIndex(): JSONObject = synchronized(indexLock) {
        cachedRoot?.let { return@synchronized it }
        val root = if (!indexFile.exists()) JSONObject().put("schema", INDEX_SCHEMA).put("documents", JSONArray())
        else JSONObject(indexFile.readText(Charsets.UTF_8))
        require(root.optString("schema") == INDEX_SCHEMA) { "Unsupported RAG index schema" }
        if (!root.has("documents")) root.put("documents", JSONArray())
        cachedRoot = root
        root
    }
    private fun saveIndex(root: JSONObject) = synchronized(indexLock) {
        ragDir.mkdirs(); val text = root.toString(); require(text.length <= MAX_INDEX_JSON_CHARS) { "RAG index too large" }
        val tmp = File(ragDir, "index-v1.json.tmp"); tmp.writeText(text, Charsets.UTF_8)
        if (indexFile.exists() && !indexFile.delete()) throw IllegalStateException("Cannot replace RAG index")
        require(tmp.renameTo(indexFile)) { "Cannot commit RAG index" }
        cachedRoot = root
    }
    private val ragDir get() = File(context.filesDir, "rag")
    private val indexFile get() = File(ragDir, "index-v1.json")
    companion object { private const val INDEX_SCHEMA="braseiro.wwn.rag.native.v1"; private const val MAX_PDF_BYTES=80L*1024*1024; private const val MAX_PAGES=2500; private const val MAX_OCR_PAGES=600; private const val MIN_TEXT_CHARS_PER_PAGE=24; private const val OCR_TARGET_WIDTH=1500; private const val OCR_TIMEOUT_SECONDS=25L; private const val MAX_EXTRACTED_CHARS=12_000_000; private const val MAX_INDEX_JSON_CHARS=45_000_000; private const val CHUNK_CHARS=1200; private const val CHUNK_OVERLAP=160; private val STOPWORDS=setOf("a","o","os","as","de","da","do","das","dos","e","em","no","na","nos","nas","um","uma","para","por","com","sem","que","se","ao","aos","the","and","or","to","in","on","with","for","from") }
}
