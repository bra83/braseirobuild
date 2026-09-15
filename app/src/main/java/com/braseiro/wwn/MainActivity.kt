package com.braseiro.wwn

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.ViewCompat
import androidx.webkit.WebViewAssetLoader
import com.braseiro.wwn.bridge.BraseiroBridge
import com.braseiro.wwn.bridge.GeminiRuntime
import com.braseiro.wwn.bridge.NativeFileActions
import com.braseiro.wwn.bridge.NativeDiceActions
import com.braseiro.wwn.bridge.NativeAudioRuntime
import com.braseiro.wwn.bridge.NativeTtsRuntime
import com.braseiro.wwn.bridge.RagRuntime
import com.braseiro.wwn.bridge.SceneImageRuntime
import com.braseiro.wwn.data.BraseiroDatabase
import com.braseiro.wwn.data.CampaignRepository
import com.braseiro.wwn.dice.BraseiroDice3DOverlay
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID

class MainActivity : ComponentActivity(), NativeFileActions, NativeDiceActions {
    private lateinit var webView: WebView
    private var pendingCharacterSaveJson: String? = null
    private var pendingBackupSaveJson: String? = null
    private var pendingRagAuthorityProfile: String? = null
    private lateinit var ragRuntime: RagRuntime
    private lateinit var geminiRuntime: GeminiRuntime
    private lateinit var audioRuntime: NativeAudioRuntime
    private lateinit var ttsRuntime: NativeTtsRuntime
    private lateinit var sceneImageRuntime: SceneImageRuntime
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val networkExecutor = Executors.newFixedThreadPool(2)
    private lateinit var webBackCallback: OnBackPressedCallback
    private val physicalDiceActive = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ragRuntime = RagRuntime(this)
        geminiRuntime = GeminiRuntime(this)
        audioRuntime = NativeAudioRuntime(this)
        ttsRuntime = NativeTtsRuntime(
            this,
            geminiRuntime,
            onSpeakingChanged = { speaking -> if (::audioRuntime.isInitialized) audioRuntime.setNarrationActive(speaking) },
            onTtsEvent = { detail -> emitTtsEvent(detail) },
        )
        sceneImageRuntime = SceneImageRuntime(this)
        WebView.setWebContentsDebuggingEnabled(false)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            // The visual reference is a 415 px design canvas. On phones whose CSS
            // viewport is narrower (for example 360 dp), fit the whole canvas to
            // the WebView instead of cropping the right edge.
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.domStorageEnabled = false
            settings.databaseEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.safeBrowsingEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                    sceneImageRuntime.openWebResource(request.url)?.let { return it }
                    val uri = request.url
                    if (!isInternal(uri)) return SceneImageRuntime.blockedResponse()
                    return assetLoader.shouldInterceptRequest(uri)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean = !isInternal(request.url)

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    syncWebBackState()
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    view?.destroy()
                    recreate()
                    return true
                }
            }
            addJavascriptInterface(
                BraseiroBridge(
                    CampaignRepository(BraseiroDatabase.get(this@MainActivity)),
                    this@MainActivity,
                    geminiRuntime,
                    ragRuntime,
                    ttsRuntime,
                    audioRuntime,
                    this@MainActivity,
                ),
                "BraseiroNative",
            )
        }

        webBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized && webView.canGoBack()) {
                    webView.goBack()
                    webView.post { syncWebBackState() }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, webBackCallback)

        // Android 15/16 enforce edge-to-edge for modern target SDKs. Keep the
        // WebView outside status/navigation bars and display cutouts explicitly.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(5, 8, 7))
            clipToPadding = true
        }
        root.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        setContentView(root)
        ViewCompat.requestApplyInsets(root)
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        if (savedInstanceState == null) webView.loadUrl("${SceneImageRuntime.LOCAL_ORIGIN}/assets/www/jornada.html")
        else webView.restoreState(savedInstanceState)
    }

    private fun syncWebBackState() {
        if (::webBackCallback.isInitialized && ::webView.isInitialized) {
            webBackCallback.isEnabled = webView.canGoBack()
        }
    }

    private fun isInternal(uri: Uri): Boolean = uri.scheme == "https" && uri.host == SceneImageRuntime.LOCAL_HOST

    override fun requestPhysicalDice(requestJson: String): String {
        return try {
            val request = JSONObject(requestJson)
            val requestId = request.optString("requestId").trim()
            val formula = request.optString("formula").trim()
            val material = request.optString("material", "bone").trim()
            require(requestId.matches(Regex("^[A-Za-z0-9._:-]{1,128}$"))) { "requestId inválido" }
            require(formula.length in 2..80) { "Fórmula de dados inválida" }
            if (!physicalDiceActive.compareAndSet(false, true)) {
                return JSONObject().put("status", "BUSY").put("requestId", requestId).put("reason", "Já existe uma rolagem 3D em andamento").toString()
            }
            runOnUiThread {
                BraseiroDice3DOverlay.roll(this, formula, material, object : BraseiroDice3DOverlay.Callback {
                    override fun onResult(result: BraseiroDice3DOverlay.RollResult) {
                        physicalDiceActive.set(false)
                        val payload = JSONObject()
                            .put("status", "OK")
                            .put("requestId", requestId)
                            .put("formula", result.formula)
                            .put("rolls", org.json.JSONArray(result.rolls))
                            .put("physicalFaces", org.json.JSONArray(result.physicalFaces))
                            .put("seed", result.seed.toString())
                            .put("physicsMs", result.physicsMs)
                            .put("forcedSettle", result.forcedSettle)
                            .put("visualQuality", result.visualQuality)
                            .put("authority", "PHYSICAL_TOP_FACE_AFTER_SETTLE")
                        emitDiceEvent(payload)
                    }

                    override fun onCancel() {
                        physicalDiceActive.set(false)
                        emitDiceEvent(JSONObject().put("status", "CANCELLED").put("requestId", requestId))
                    }

                    override fun onError(message: String) {
                        physicalDiceActive.set(false)
                        emitDiceEvent(JSONObject().put("status", "ERROR").put("requestId", requestId).put("reason", message))
                    }
                })
            }
            JSONObject().put("status", "PENDING").put("requestId", requestId).put("formula", formula).toString()
        } catch (t: Throwable) {
            physicalDiceActive.set(false)
            JSONObject().put("status", "ERROR").put("reason", t.message ?: "Falha ao iniciar a rolagem 3D").toString()
        }
    }

    private fun emitDiceEvent(detail: JSONObject) {
        val js = "window.dispatchEvent(new CustomEvent('braseiro-physical-dice',{detail:${detail}}));"
        if (::webView.isInitialized) webView.post { if (!isDestroyed && !isFinishing) webView.evaluateJavascript(js, null) }
    }

    override fun requestOpenCharacterJson() = launchJsonOpen(REQUEST_CHARACTER_OPEN)

    override fun requestSaveCharacterJson(json: String, suggestedName: String) {
        pendingCharacterSaveJson = json
        launchJsonSave(REQUEST_CHARACTER_SAVE, sanitizeJsonName(suggestedName, "wwn-character.json"))
    }

    override fun requestOpenBackupJson() = launchJsonOpen(REQUEST_BACKUP_OPEN)

    override fun requestSaveBackupJson(json: String, suggestedName: String) {
        pendingBackupSaveJson = json
        launchJsonSave(REQUEST_BACKUP_SAVE, sanitizeJsonName(suggestedName, "wwn-campaign-backup.json"))
    }

    override fun requestOpenRulesPdf(authorityProfile: String) {
        pendingRagAuthorityProfile = authorityProfile
        runOnUiThread {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
            startActivityForResult(intent, REQUEST_RULES_PDF_OPEN)
        }
    }

    override fun requestShareText(title: String, text: String) {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "ENVIAR PROMPT DA CENA"))
        }
    }

    override fun requestGeminiTextAsync(requestJson: String): String {
        val requestId = "gemini-${UUID.randomUUID()}"
        networkExecutor.execute {
            val result = try { geminiRuntime.generateText(requestJson) }
            catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message ?: t.javaClass.simpleName) }
            result.put("requestId", requestId)
            emitGeminiEvent(result)
        }
        return JSONObject().put("status", "PENDING").put("requestId", requestId).toString()
    }

    override fun requestGeminiImageAsync(requestJson: String): String {
        val requestId = "gemini-image-${UUID.randomUUID()}"
        networkExecutor.execute {
            val result = try { geminiRuntime.generateImage(requestJson) }
            catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message ?: t.javaClass.simpleName) }
            result.put("requestId", requestId).put("kind", "IMAGE")
            emitGeminiEvent(result)
        }
        return JSONObject().put("status", "PENDING").put("requestId", requestId).put("kind", "IMAGE").toString()
    }

    override fun requestRagSearchAsync(requestJson: String): String {
        val requestId = "rag-${UUID.randomUUID()}"
        ioExecutor.execute {
            val result = try { ragRuntime.search(requestJson) }
            catch (t: Throwable) { JSONObject().put("status", "ERROR").put("reason", t.message ?: t.javaClass.simpleName).put("results", org.json.JSONArray()) }
            result.put("requestId", requestId)
            emitRagEvent(result)
        }
        return JSONObject().put("status", "PENDING").put("requestId", requestId).toString()
    }

    override fun requestOpenSceneImage() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/png", "image/jpeg", "image/webp"))
            }
            startActivityForResult(intent, REQUEST_SCENE_IMAGE_OPEN)
        }
    }

    private fun launchJsonOpen(requestCode: Int) {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
            }
            startActivityForResult(intent, requestCode)
        }
    }

    private fun launchJsonSave(requestCode: Int, safeName: String) {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, safeName)
            }
            startActivityForResult(intent, requestCode)
        }
    }

    private fun sanitizeJsonName(suggestedName: String, fallback: String): String = suggestedName
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .take(96)
        .ifBlank { fallback }
        .let { if (it.endsWith(".json", ignoreCase = true)) it else "$it.json" }

    @Deprecated("Deprecated in Android API; retained for the minimum-API Activity shell used by this project")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val supported = requestCode in setOf(
            REQUEST_CHARACTER_OPEN, REQUEST_CHARACTER_SAVE, REQUEST_BACKUP_OPEN, REQUEST_BACKUP_SAVE,
            REQUEST_RULES_PDF_OPEN, REQUEST_SCENE_IMAGE_OPEN,
        )
        if (!supported) return

        val kind = when (requestCode) {
            REQUEST_CHARACTER_OPEN -> "CHARACTER_OPEN"
            REQUEST_CHARACTER_SAVE -> "CHARACTER_SAVE"
            REQUEST_BACKUP_OPEN -> "BACKUP_OPEN"
            REQUEST_BACKUP_SAVE -> "BACKUP_SAVE"
            REQUEST_RULES_PDF_OPEN -> "RULES_PDF_IMPORT"
            else -> "SCENE_IMAGE_IMPORT"
        }
        if (resultCode != RESULT_OK || data?.data == null) {
            if (requestCode == REQUEST_CHARACTER_SAVE) pendingCharacterSaveJson = null
            if (requestCode == REQUEST_BACKUP_SAVE) pendingBackupSaveJson = null
            if (requestCode == REQUEST_RULES_PDF_OPEN) pendingRagAuthorityProfile = null
            emitFileEvent(kind = kind, status = "CANCELLED")
            return
        }
        val uri = data.data ?: return
        when (requestCode) {
            REQUEST_RULES_PDF_OPEN -> {
                val authority = pendingRagAuthorityProfile ?: "WWN_RULEBOOK_AUTHORIZED"
                pendingRagAuthorityProfile = null
                runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                emitFileEvent(kind, "PROCESSING", text = JSONObject().put("status", "PROCESSING").put("phase", "OPENING").put("page", 0).put("totalPages", 0).toString())
                ioExecutor.execute {
                    try {
                        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                        val result = ragRuntime.importPdf(uri, name, authority) { page, totalPages ->
                            val progress = JSONObject().put("status", "PROCESSING").put("phase", "EXTRACTING").put("page", page).put("totalPages", totalPages).put("title", name ?: "PDF")
                            emitFileEvent(kind, "PROCESSING", text = progress.toString())
                        }
                        emitFileEvent(kind, result.optString("status", "ERROR"), text = result.toString(), reason = result.optString("reason").takeIf { it.isNotBlank() })
                    } catch (t: Throwable) { emitFileEvent(kind, "ERROR", reason = t.message) }
                }
            }
            REQUEST_SCENE_IMAGE_OPEN -> ioExecutor.execute {
                try {
                    val result = sceneImageRuntime.importImage(uri)
                    emitFileEvent(kind, result.optString("status", "ERROR"), text = result.toString(), reason = result.optString("reason").takeIf { it.isNotBlank() })
                } catch (t: Throwable) { emitFileEvent(kind, "ERROR", reason = t.message) }
            }
            REQUEST_CHARACTER_OPEN, REQUEST_BACKUP_OPEN -> ioExecutor.execute {
                try {
                    val maxBytes = if (requestCode == REQUEST_CHARACTER_OPEN) 2_000_000 else 50_000_000
                    val text = readUtf8Bounded(uri, maxBytes)
                    emitFileEvent(kind, "OK", text = text)
                } catch (t: Throwable) { emitFileEvent(kind, "ERROR", reason = t.message) }
            }
            REQUEST_CHARACTER_SAVE, REQUEST_BACKUP_SAVE -> {
                val text = if (requestCode == REQUEST_CHARACTER_SAVE) pendingCharacterSaveJson else pendingBackupSaveJson
                if (requestCode == REQUEST_CHARACTER_SAVE) pendingCharacterSaveJson = null else pendingBackupSaveJson = null
                ioExecutor.execute {
                    try {
                        require(text != null) { "No pending JSON" }
                        contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
                            ?: throw IllegalStateException("Unable to write selected JSON")
                        emitFileEvent(kind, "OK")
                    } catch (t: Throwable) { emitFileEvent(kind, "ERROR", reason = t.message) }
                }
            }
        }
    }

    private fun readUtf8Bounded(uri: Uri, maxBytes: Int): String {
        val output = java.io.ByteArrayOutputStream()
        contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "Selected JSON is too large" }
                output.write(buffer, 0, read)
            }
        } ?: throw IllegalStateException("Unable to read selected JSON")
        return output.toString(Charsets.UTF_8.name())
    }

    private fun emitFileEvent(kind: String, status: String, text: String? = null, reason: String? = null) {
        val detail = JSONObject().put("kind", kind).put("status", status)
        if (text != null) detail.put("text", text)
        if (reason != null) detail.put("reason", reason)
        val js = "window.dispatchEvent(new CustomEvent('braseiro-native-file',{detail:${detail}}));"
        if (::webView.isInitialized) webView.post { if (!isDestroyed && !isFinishing) webView.evaluateJavascript(js, null) }
    }

    private fun emitGeminiEvent(detail: JSONObject) {
        val js = "window.dispatchEvent(new CustomEvent('braseiro-gemini',{detail:${detail}}));"
        if (::webView.isInitialized) webView.post { if (!isDestroyed && !isFinishing) webView.evaluateJavascript(js, null) }
    }

    private fun emitRagEvent(detail: JSONObject) {
        val js = "window.dispatchEvent(new CustomEvent('braseiro-rag',{detail:${detail}}));"
        if (::webView.isInitialized) webView.post { if (!isDestroyed && !isFinishing) webView.evaluateJavascript(js, null) }
    }

    private fun emitTtsEvent(detail: JSONObject) {
        val js = "window.dispatchEvent(new CustomEvent('braseiro-tts',{detail:${detail}}));"
        if (::webView.isInitialized) webView.post { if (!isDestroyed && !isFinishing) webView.evaluateJavascript(js, null) }
    }

    override fun onStart() {
        super.onStart()
        if (::audioRuntime.isInitialized) audioRuntime.resumeFromLifecycle()
    }

    override fun onStop() {
        if (::ttsRuntime.isInitialized) ttsRuntime.stop()
        if (::audioRuntime.isInitialized) audioRuntime.pauseForLifecycle()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (::ttsRuntime.isInitialized) ttsRuntime.shutdown()
        if (::audioRuntime.isInitialized) audioRuntime.shutdown()
        ioExecutor.shutdownNow()
        networkExecutor.shutdownNow()
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("BraseiroNative")
            webView.destroy()
        }
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CHARACTER_OPEN = 2401
        private const val REQUEST_CHARACTER_SAVE = 2402
        private const val REQUEST_BACKUP_OPEN = 2411
        private const val REQUEST_BACKUP_SAVE = 2412
        private const val REQUEST_RULES_PDF_OPEN = 2421
        private const val REQUEST_SCENE_IMAGE_OPEN = 2431
    }
}
