package com.braseiro.oumanel

import android.annotation.SuppressLint
import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.max
import kotlin.math.abs
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private val trustedAppOrigin = "https://appassets.androidplatform.net"
    private val trustedAppPrefix = "$trustedAppOrigin/assets/"
    private val driveTokenPreference = "drive_access_token_encrypted"
    private val resumedImportPreference = "audio_import_tree_uri"
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    @Volatile private var audioFocusGranted = false
    private val focusPausedPlayers = ConcurrentHashMap.newKeySet<String>()
    private var receiverRegistered = false
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { change ->
        runOnUiThread { handleAudioFocusChange(change) }
    }
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_AUDIO_BECOMING_NOISY) return
            pauseNativeAudioForInterruption("Saída de áudio alterada; reprodução pausada para evitar som inesperado no altifalante.")
            pauseSong("Saída de áudio alterada; Song foi pausado.", true)
            stopNativeSpeech(true)
        }
    }
    private lateinit var webView: WebView
    @Volatile private var trustedOriginActive: Boolean = false
    private val barbaraSupervisor by lazy { BarbaraSupervisor(this) }
    private val diceAppearanceStore by lazy { DiceAppearanceStore(this) }
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var driveRequestPending = false
    private var audioImportPending = false
    private var audioFolderImportPending = false
    private data class PendingExport(
        val requestId: String,
        val fileName: String,
        val temporaryFile: File,
    )
    private var pendingExport: PendingExport? = null
    @Volatile private var pendingSharedImageReference: String? = null
    @Volatile private var pendingSharedImageError: String? = null

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var currentSpeechRootId: String? = null
    private data class SpeechCue(val id: String, val charIndex: Int)
    private data class SpeechSegment(val text: String, val offset: Int, val cues: List<SpeechCue>)
    private data class SpeechPart(val rootId: String, val index: Int, val total: Int, val offset: Int, val text: String, val cues: List<SpeechCue>)
    private val speechParts = ConcurrentHashMap<String, SpeechPart>()
    private val firedSpeechCues = ConcurrentHashMap.newKeySet<String>()
    private val speechHandler = Handler(Looper.getMainLooper())
    private var speechQueue: List<SpeechSegment> = emptyList()
    private var speechQueueIndex = 0
    private var speechRate = .96f
    private var speechWatchdog: Runnable? = null
    private val ttsPreferences by lazy { getSharedPreferences("braseiro_oumanel_tts", MODE_PRIVATE) }
    @Volatile private var activeTtsEnginePackage = ""
    @Volatile private var activeTtsEngineLabel = ""
    @Volatile private var selectedTtsVoiceName = ""
    @Volatile private var neuralTtsRate = 1.0f
    @Volatile private var startupTtsSelectionAttempted = false
    @Volatile private var lastNeuralPrewarmAt = 0L
    // Mesmo envelope de segmentação validado no Braseiro Forbidden Lands:
    // primeira camada 680 (primeiro bloco até 320), segunda camada Android ~150.
    private val neuralMinChars = 90
    private val neuralTargetChars = 125
    private val neuralMaxChars = 150

    private val audioDir by lazy { File(filesDir, "audio_library").apply { mkdirs() } }
    private val audioIndexFile by lazy { File(audioDir, "index.json") }
    private val soundKeywordLibraryFile by lazy { File(audioDir, "biblioteca_sons.json") }
    private val audioOverridesFile by lazy { File(audioDir, "audio_overrides.json") }
    private val audioCatalog by lazy { AudioCatalogDatabase(this) }
    private val audioRotationSelector by lazy { AudioRotationSelector(audioCatalog) }
    private val nativePlayers = ConcurrentHashMap<String, MediaPlayer>()
    private val songCacheDir by lazy { File(filesDir, "song_cache").apply { mkdirs() } }
    private var songPlayer: MediaPlayer? = null
    private var songFileId: String? = null
    private var songName: String = ""
    private var songState: String = "idle"
    private var songError: String? = null
    private var songPausedByFocus = false
    @Volatile private var songGeneration = 0
    private val loadedSoundPoolSamples = ConcurrentHashMap.newKeySet<Int>()
    private val soundPoolSamples = ConcurrentHashMap<String, Int>()
    private val activeSoundPoolStreams = ConcurrentHashMap<Int, String>()
    private val soundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(12)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
            .apply { setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loadedSoundPoolSamples.add(sampleId) } }
    }
    private val nativeVolumes = ConcurrentHashMap<String, Float>().apply {
        put("voice", 1f); put("ambience", .35f); put("weather", .28f); put("music", .14f); put("sfx", .8f)
    }
    private val privatePreferences by lazy { getSharedPreferences("braseiro_oumanel_private", MODE_PRIVATE) }
    private var freeGeminiCircuitOpenUntil = 0L
    private val geminiTtsDir by lazy { File(filesDir, "gemini_tts_cache").apply { mkdirs() } }
    @Volatile private var activeGeminiVoiceRequestId: String? = null

    private val voiceInputController by lazy { VoiceInputController(this) }
    @Volatile private var pendingVoicePermissionStart = false

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val shouldStart = pendingVoicePermissionStart
        pendingVoicePermissionStart = false
        if (granted && shouldStart) {
            startVoiceInputFromBridge()
        } else if (shouldStart) {
            emitVoiceError("Permissão de microfone negada. O teclado continua disponível normalmente.")
        }
    }

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            Log.w("BRASEIRO_DRIVE", "authorization resultCode=${result.resultCode} data=${result.data != null}")
            deliverDriveError("A autorização do Google Drive não foi concluída (resultado ${result.resultCode}).")
            return@registerForActivityResult
        }
        try {
            deliverDriveAuthorization(Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data!!))
        } catch (error: ApiException) {
            Log.e("BRASEIRO_DRIVE", "ApiException status=${error.statusCode}", error)
            deliverDriveError("Google Drive recusou a autorização (código ${error.statusCode}). Verifique pacote e SHA-1 no cliente OAuth Android.")
        } catch (error: Exception) {
            Log.e("BRASEIRO_DRIVE", "authorization error", error)
            deliverDriveError("Não foi possível concluir a autorização do Google Drive: ${error.message ?: "erro desconhecido"}")
        }
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val callback = fileCallback ?: return@registerForActivityResult
        fileCallback = null
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            callback.onReceiveValue(null)
            return@registerForActivityResult
        }
        val data = result.data!!
        val uris = collectUris(data)
        val takeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        uris.forEach { uri ->
            try { contentResolver.takePersistableUriPermission(uri, takeFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
        callback.onReceiveValue(if (uris.isEmpty()) null else uris.toTypedArray())
    }

    private val audioPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (!audioImportPending) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            audioImportPending = false
            evaluate("window.__braseiroAudioReject?.(new Error('Importação de sons cancelada.')); window.__braseiroAudioResolve=null; window.__braseiroAudioReject=null;")
            return@registerForActivityResult
        }
        val data = result.data!!
        val uris = collectUris(data)
        uris.forEach { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
        lifecycleScope.launch {
            val resultJson = withContext(Dispatchers.IO) { importAudioUris(uris) }
            audioImportPending = false
            evaluate("window.__braseiroAudioResolve?.(${resultJson}); window.__braseiroAudioResolve=null; window.__braseiroAudioReject=null; window.dispatchEvent(new Event('braseiro-native-audio-changed'));")
        }
    }

    private val audioFolderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (!audioFolderImportPending) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) {
            audioFolderImportPending = false
            evaluate("window.__braseiroAudioReject?.(new Error('Importação da pasta cancelada.')); window.__braseiroAudioResolve=null; window.__braseiroAudioReject=null;")
            return@registerForActivityResult
        }
        val treeUri = result.data!!.data!!
        try { contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        privatePreferences.edit().putString(resumedImportPreference, treeUri.toString()).apply()
        val resultJson = scheduleAudioFolderImport(treeUri)
        audioFolderImportPending = false
        evaluate("window.__braseiroAudioResolve?.(${resultJson}); window.__braseiroAudioResolve=null; window.__braseiroAudioReject=null;")
    }

    private val exportDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val pending = pendingExport
        pendingExport = null
        if (pending == null) return@registerForActivityResult
        if (uri == null) {
            pending.temporaryFile.delete()
            rejectExport(pending.requestId, "Exportação cancelada. Nenhum arquivo foi criado.")
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { writeAndVerifyExport(uri, pending) }
                resolveExport(pending.requestId, result)
            } catch (error: Exception) {
                Log.e("BRASEIRO_EXPORT", "Falha ao gravar documento", error)
                rejectExport(pending.requestId, error.message ?: "Falha ao gravar o arquivo exportado.")
            } finally {
                pending.temporaryFile.delete()
            }
        }
    }

    private fun exportCallbackScript(requestId: String, body: String): String {
        val quotedId = JSONObject.quote(requestId)
        return """
            (() => {
              const id = $quotedId;
              const callbacks = window.__braseiroExportCallbacks || {};
              const callback = callbacks[id];
              if (callback) { $body; delete callbacks[id]; }
              if (id.startsWith('legacy_export_')) {
                const value = typeof __braseiroLegacyExportValue === 'string' ? __braseiroLegacyExportValue : '';
                window.__braseiroBackupResolve?.(value);
                window.__braseiroBackupResolve = null;
                window.__braseiroBackupReject = null;
              }
            })();
        """.trimIndent()
    }

    private fun resolveExport(requestId: String, result: JSONObject) {
        if (requestId.startsWith("legacy_export_")) {
            val name = JSONObject.quote(result.optString("fileName", "arquivo.json"))
            evaluate("window.__braseiroBackupResolve?.($name); window.__braseiroBackupResolve=null; window.__braseiroBackupReject=null;")
            return
        }
        val payload = result.toString()
        evaluate(exportCallbackScript(requestId, "callback.resolve($payload)"))
    }

    private fun rejectExport(requestId: String, message: String) {
        val quotedMessage = JSONObject.quote(message)
        val quotedId = JSONObject.quote(requestId)
        evaluate("""
            (() => {
              const id = $quotedId;
              const callbacks = window.__braseiroExportCallbacks || {};
              const callback = callbacks[id];
              if (callback) { callback.reject(new Error($quotedMessage)); delete callbacks[id]; }
              if (id.startsWith('legacy_export_')) {
                window.__braseiroBackupReject?.(new Error($quotedMessage));
                window.__braseiroBackupResolve = null;
                window.__braseiroBackupReject = null;
              }
            })();
        """.trimIndent())
    }

    private fun digestStream(input: java.io.InputStream): Pair<String, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            digest.update(buffer, 0, read)
            total += read
        }
        return digest.digest().joinToString("") { "%02x".format(it) } to total
    }

    private fun writeAndVerifyExport(uri: Uri, pending: PendingExport): JSONObject {
        val expected = pending.temporaryFile.inputStream().buffered().use(::digestStream)
        contentResolver.openOutputStream(uri, "rwt")?.buffered()?.use { output ->
            pending.temporaryFile.inputStream().buffered().use { input -> input.copyTo(output, 64 * 1024) }
            output.flush()
        } ?: throw IllegalStateException("O Android não abriu o destino escolhido para gravação.")
        val actual = contentResolver.openInputStream(uri)?.buffered()?.use(::digestStream)
            ?: throw IllegalStateException("O Android gravou o arquivo, mas não permitiu verificar o conteúdo.")
        if (actual.second <= 0L) throw IllegalStateException("O arquivo criado ficou vazio.")
        if (actual.first != expected.first || actual.second != expected.second) {
            throw IllegalStateException("A verificação do arquivo exportado falhou. O conteúdo não foi confirmado.")
        }
        return JSONObject().apply {
            put("fileName", pending.fileName)
            put("uri", uri.toString())
            put("sizeBytes", actual.second)
            put("sha256", actual.first)
        }
    }

    private fun sanitizeExportFileName(value: String): String {
        val clean = value.trim().replace(Regex("[\\/:*?\"<>|\r\n]+"), "_").take(160)
        return when {
            clean.isBlank() -> "braseiro_export.json"
            clean.endsWith(".json", true) -> clean
            else -> "$clean.json"
        }
    }

    private fun resolveFileChooserMimeTypes(params: WebChromeClient.FileChooserParams?): Array<String> {
        val requested = params?.acceptTypes.orEmpty()
            .flatMap { it.split(',') }
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
        if (requested.isEmpty() || requested.any { it == "*/*" }) {
            return arrayOf(
                "application/pdf", "text/plain", "text/markdown", "application/json",
                "application/zip", "application/x-zip-compressed", "audio/*", "image/*",
            )
        }
        val resolved = linkedSetOf<String>()
        requested.forEach { value ->
            when {
                value == ".pdf" || value == "application/pdf" -> resolved += "application/pdf"
                value == ".txt" || value == "text/plain" -> resolved += "text/plain"
                value == ".md" || value == "text/markdown" -> resolved += "text/markdown"
                value == ".json" || value == "application/json" || value.contains("braseiro-character") -> resolved += "application/json"
                value == ".zip" || value.contains("zip") -> { resolved += "application/zip"; resolved += "application/x-zip-compressed" }
                value.startsWith("audio/") || value in setOf(".mp3", ".m4a", ".aac", ".ogg", ".wav", ".flac", ".opus") -> resolved += "audio/*"
                value.startsWith("image/") || value in setOf(".png", ".jpg", ".jpeg", ".webp") -> resolved += "image/*"
                value.contains('/') -> resolved += value
            }
        }
        return (resolved.ifEmpty { linkedSetOf("application/octet-stream") }).toTypedArray()
    }


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = android.graphics.Color.rgb(4, 8, 16)
        window.navigationBarColor = android.graphics.Color.rgb(4, 8, 16)
        // Não dependa da geração opcional de BuildConfig do AGP. O sinalizador
        // do próprio APK funciona em debug/release e evita erro de compilação.
        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        WebView.setWebContentsDebuggingEnabled(isDebuggable)
        runCatching { barbaraSupervisor.configureDeviceProfile(BarbaraDeviceMetrics.collect(this)) }
            .onFailure { AppLog.w("BarbaraDeviceProfile", "Falha ao configurar perfil do aparelho; fallback balanced será usado.", it) }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        ContextCompat.registerReceiver(
            this,
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
        textToSpeech = TextToSpeech(this, this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                audioCatalog.abortInterruptedSessions()
                if (audioCatalog.count() == 0 && readAudioIndex().length() > 0) rebuildAudioCatalog()
                resumeInterruptedAudioImportIfPossible()
            } catch (error: Exception) {
                Log.e("BRASEIRO_AUDIO", "Falha ao inicializar catálogo SQLite", error)
            }
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler(
                "/shared-image/",
                WebViewAssetLoader.InternalStoragePathHandler(this, SharedImageInbox.directory(this)),
            )
            .build()

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(4, 8, 16))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.setSupportZoom(false)
            addJavascriptInterface(NativeBridge(), "BraseiroNative")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (isTrustedAppUrl(uri)) return false
                    if (!request.isForMainFrame) return true
                    openExternalUri(uri)
                    return true
                }
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(request.url)
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    val trusted = isTrustedAppUrl(Uri.parse(url))
                    trustedOriginActive = trusted
                    if (!trusted) {
                        view.stopLoading()
                           view.loadUrl("$trustedAppPrefix/index.html?v=49197&immersive=1")
                    }
                }
                override fun onPageFinished(view: WebView, url: String) {
                    trustedOriginActive = isTrustedAppUrl(Uri.parse(url))
                    if (!trustedOriginActive) return
                    installJavascriptBridge()
                    dispatchPendingSharedImage()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                    Log.e("BRASEIRO_INIT", "WebView ${request.url}: ${error.description}")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                    Log.d("BRASEIRO_WEB", "${consoleMessage.message()} @${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}")
                    return true
                }
                override fun onShowFileChooser(webView: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                    fileCallback?.onReceiveValue(null)
                    fileCallback = callback
                    val acceptedMimeTypes = resolveFileChooserMimeTypes(params)
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = if (acceptedMimeTypes.size == 1) acceptedMimeTypes.first() else "*/*"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                        if (acceptedMimeTypes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, acceptedMimeTypes)
                    }
                    filePickerLauncher.launch(Intent.createChooser(intent, "Importar para O Um Anel"))
                    return true
                }
            }
        }

        // A versão 1.7.1 não usa edge-to-edge. O Android mede o WebView dentro
        // das barras de status, navegação e recortes físicos do aparelho. Assim,
        // nenhum cabeçalho ou botão fica oculto sob a interface do telefone.
        setContentView(webView)
        // LOAD_NO_CACHE + cache-buster já garantem os assets atuais. Limpar todo
        // o cache aqui bloqueava a thread principal justamente no cold start.
        webView.loadUrl("$trustedAppPrefix/index.html?v=49197&immersive=1")
        handleSharedImageIntent(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                evaluate("window.dispatchEvent(new PopStateEvent('popstate')); window.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));")
                if (webView.canGoBack() && isTrustedAppUrl(Uri.parse(webView.copyBackForwardList().getItemAtIndex(webView.copyBackForwardList().currentIndex - 1)?.url ?: ""))) webView.goBack()
                else finish()
            }
        })
    }

    private fun isTrustedAppUrl(uri: Uri?): Boolean {
        if (uri == null) return false
        return uri.scheme == "https" && uri.host == "appassets.androidplatform.net" && (uri.path ?: "").startsWith("/assets/")
    }

    private fun openExternalUri(uri: Uri) {
        if (uri.scheme !in setOf("https", "http", "mailto", "tel")) {
            Log.w("BRASEIRO_SECURITY", "URI externa rejeitada: $uri")
            return
        }
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (error: Exception) { Log.w("BRASEIRO_SECURITY", "Não foi possível abrir URI externa", error) }
    }

    private fun requestNativeAudioFocus(): Boolean {
        if (audioFocusGranted) return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        audioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioFocusGranted
    }

    private fun abandonNativeAudioFocusIfIdle(force: Boolean = false) {
        if (!audioFocusGranted) return
        val active = nativePlayers.isNotEmpty() || activeSoundPoolStreams.isNotEmpty() || currentSpeechRootId != null || songPlayer != null
        if (!force && active) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        audioFocusGranted = false
        audioFocusRequest = null
    }

    private fun handleAudioFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusGranted = true
                for (key in focusPausedPlayers.toList()) {
                    nativePlayers[key]?.let { player ->
                        try { player.start() } catch (_: Exception) {}
                    }
                    focusPausedPlayers.remove(key)
                }
                soundPool.autoResume()
                if (songPausedByFocus) {
                    try { songPlayer?.start(); songState = "playing"; songPausedByFocus = false; emitSongState() } catch (_: Exception) {}
                }
                try { songPlayer?.setVolume(1f, 1f) } catch (_: Exception) {}
                applyNativeVolumes()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                nativePlayers.forEach { (key, player) ->
                    val channel = key.substringBefore(':')
                    val volume = (nativeVolumes[channel] ?: 1f) * .2f
                    try { player.setVolume(volume, volume) } catch (_: Exception) {}
                }
                try { songPlayer?.setVolume(.2f, .2f) } catch (_: Exception) {}
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseNativeAudioForInterruption("Áudio pausado por interrupção temporária.")
                pauseSong("Song pausado por interrupção temporária.", true)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                pauseNativeAudioForInterruption("Áudio interrompido por outro aplicativo.")
                pauseSong("Song interrompido por outro aplicativo.", true)
                stopNativeSpeech(true)
                abandonNativeAudioFocusIfIdle(true)
            }
        }
    }

    private fun pauseNativeAudioForInterruption(message: String) {
        nativePlayers.forEach { (key, player) ->
            try {
                if (player.isPlaying) {
                    player.pause()
                    focusPausedPlayers.add(key)
                }
            } catch (_: Exception) {}
        }
        try { soundPool.autoPause() } catch (_: Exception) {}
        emitAudioDiagnostic("native-audio-interrupted", "all", message, "warning", result = "pausado")
    }

    private fun songStatusJson(): JSONObject = JSONObject().apply {
        val player = songPlayer
        put("state", songState)
        put("fileId", songFileId ?: JSONObject.NULL)
        put("name", songName)
        put("currentTime", try { (player?.currentPosition ?: 0) / 1000.0 } catch (_: Exception) { 0.0 })
        put("duration", try { (player?.duration ?: 0) / 1000.0 } catch (_: Exception) { 0.0 })
        put("error", songError ?: JSONObject.NULL)
        put("cached", songFileId?.let { cachedSongFile(it, songName).exists() } ?: false)
    }

    private fun emitSongState() {
        val detail = songStatusJson().toString()
        evaluate("window.dispatchEvent(new CustomEvent('braseiro-song-state',{detail:$detail}));")
    }

    private fun cachedSongFile(fileId: String, name: String): File {
        val ext = name.substringAfterLast('.', "audio").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "").take(6).ifBlank { "audio" }
        val safeId = fileId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(songCacheDir, "$safeId.$ext")
    }

    private fun deliverSongCallback(requestId: String, success: Boolean, message: String? = null) {
        val id = JSONObject.quote(requestId)
        val script = if (success) {
            "window.__braseiroSongCallbacks?.[$id]?.resolve?.(JSON.parse(BraseiroNative.getSongStatus())); delete window.__braseiroSongCallbacks?.[$id];"
        } else {
            val error = JSONObject.quote(message ?: "Falha no player Song.")
            "window.__braseiroSongCallbacks?.[$id]?.reject?.(new Error($error)); delete window.__braseiroSongCallbacks?.[$id];"
        }
        evaluate(script)
    }

    private fun startCachedSong(fileId: String, name: String, file: File, requestId: String) {
        if (!requestNativeAudioFocus()) { deliverSongCallback(requestId, false, "O Android não concedeu foco de áudio."); return }
        stopSong(false, false)
        songFileId = fileId; songName = name; songState = "loading"; songError = null; emitSongState()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(file.absolutePath)
                setOnPreparedListener {
                    try {
                        it.start(); songState = "playing"; songPausedByFocus = false; emitSongState(); deliverSongCallback(requestId, true)
                    } catch (error: Exception) {
                        songState = "error"; songError = error.message; emitSongState(); deliverSongCallback(requestId, false, error.message)
                    }
                }
                setOnCompletionListener { completed ->
                    songState = "stopped"; try { completed.seekTo(0) } catch (_: Exception) {}; emitSongState(); abandonNativeAudioFocusIfIdle()
                }
                setOnErrorListener { failed, what, extra ->
                    songState = "error"; songError = "MediaPlayer falhou ($what/$extra)."; emitSongState();
                    try { failed.release() } catch (_: Exception) {}; songPlayer = null; deliverSongCallback(requestId, false, songError); true
                }
                prepareAsync()
            }
            songPlayer = player
        } catch (error: Exception) {
            songState = "error"; songError = error.message; songPlayer = null; emitSongState(); deliverSongCallback(requestId, false, error.message)
        }
    }

    private fun pruneSongCache(requiredBytes: Long = 0L, keep: File? = null) {
        val maxBytes = 1024L * 1024L * 1024L
        val maxFiles = 30
        val files = songCacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".partial") && it != keep }?.sortedBy { it.lastModified() }?.toMutableList() ?: return
        var total = songCacheDir.listFiles()?.filter { it.isFile && !it.name.endsWith(".partial") }?.sumOf { it.length() } ?: 0L
        while (files.isNotEmpty() && (files.size + (if (keep?.exists() == true) 1 else 0) >= maxFiles || total + requiredBytes > maxBytes)) {
            val stale = files.removeAt(0); total -= stale.length(); stale.delete()
        }
    }

    private fun playDriveSong(fileId: String, name: String, mimeType: String, declaredSize: Long, requestId: String) {
        val generation = ++songGeneration
        val target = cachedSongFile(fileId, name)
        if (target.exists() && target.length() > 0L) { target.setLastModified(System.currentTimeMillis()); startCachedSong(fileId, name, target, requestId); return }
        songFileId = fileId; songName = name; songState = "loading"; songError = null; emitSongState()
        lifecycleScope.launch {
            try {
                val completed = withContext(Dispatchers.IO) {
                    val token = decryptSecret(privatePreferences.getString(driveTokenPreference, "") ?: "")
                    if (token.isBlank()) throw IllegalStateException("Autorize o Google Drive antes de tocar a música.")
                    val maximum = 250L * 1024L * 1024L
                    pruneSongCache(max(0L, declaredSize), target)
                    if (declaredSize > maximum) throw IllegalStateException("A música excede o limite de 250 MB.")
                    if (songCacheDir.usableSpace < max(32L * 1024L * 1024L, declaredSize + 32L * 1024L * 1024L)) throw IllegalStateException("Espaço livre insuficiente para armazenar a música.")
                    val partial = File(target.absolutePath + ".partial")
                    if (partial.exists()) partial.delete()
                    val connection = URL("https://www.googleapis.com/drive/v3/files/${URLEncoder.encode(fileId, "UTF-8")}?alt=media").openConnection() as HttpURLConnection
                    try {
                        connection.setRequestProperty("Authorization", "Bearer $token")
                        connection.connectTimeout = 20_000; connection.readTimeout = 60_000
                        val code = connection.responseCode
                        if (code !in 200..299) throw IllegalStateException("Google Drive devolveu HTTP $code.")
                        val length = connection.contentLengthLong
                        if (length > maximum) throw IllegalStateException("A música excede o limite de 250 MB.")
                        BufferedInputStream(connection.inputStream).use { input ->
                            FileOutputStream(partial).use { output ->
                                val buffer = ByteArray(128 * 1024); var total = 0L
                                while (true) { val count = input.read(buffer); if (count < 0) break; total += count; if (total > maximum) throw IllegalStateException("A música excede o limite de 250 MB."); output.write(buffer, 0, count) }
                                output.fd.sync()
                            }
                        }
                        if (!partial.renameTo(target)) { partial.copyTo(target, overwrite = true); partial.delete() }
                        target
                    } finally { connection.disconnect() }
                }
                if (generation != songGeneration) { deliverSongCallback(requestId, false, "A seleção de música foi substituída."); return@launch }
                completed.setLastModified(System.currentTimeMillis())
                startCachedSong(fileId, name, completed, requestId)
            } catch (error: Exception) {
                songState = "error"; songError = error.message; emitSongState(); deliverSongCallback(requestId, false, error.message)
            }
        }
    }

    private fun resumeSong(): Boolean {
        return try {
            val player = songPlayer ?: return false
            if (!requestNativeAudioFocus()) return false
            player.start()
            songState = "playing"
            songPausedByFocus = false
            songError = null
            emitSongState()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun pauseSong(message: String? = null, dueToFocus: Boolean = false): Boolean {
        return try {
            val player = songPlayer ?: return false
            if (player.isPlaying) player.pause()
            songState = "paused"
            songPausedByFocus = dueToFocus
            if (message != null) songError = message
            emitSongState()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun stopSong(clearSelection: Boolean = true, cancelPending: Boolean = true): Boolean {
        if (cancelPending) songGeneration += 1
        val player = songPlayer
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        songPlayer = null; songState = "stopped"; songPausedByFocus = false; songError = null
        if (clearSelection) { songFileId = null; songName = "" }
        emitSongState(); abandonNativeAudioFocusIfIdle(); return true
    }

    private fun clearSongCache(): Boolean = try {
        stopSong(true); songCacheDir.listFiles()?.forEach { it.delete() }; true
    } catch (_: Exception) { false }

    private fun applyNativeVolumes() {
        for ((key, player) in nativePlayers) {
            val channel = key.substringBefore(':')
            val volume = nativeVolumes[channel] ?: 1f
            try { player.setVolume(volume, volume) } catch (_: Exception) {}
        }
        val sfx = nativeVolumes["sfx"] ?: 1f
        for (streamId in activeSoundPoolStreams.keys) try { soundPool.setVolume(streamId, sfx, sfx) } catch (_: Exception) {}
    }

    private fun storageHealthJson(): JSONObject {
        val probe = File(filesDir, ".health_probe")
        var writable = false
        var error = ""
        try {
            probe.writeText("ok")
            writable = probe.readText() == "ok"
            probe.delete()
        } catch (failure: Exception) { error = failure.message ?: "Falha de escrita" }
        return JSONObject().apply {
            put("writable", writable)
            put("freeBytes", filesDir.usableSpace)
            put("totalBytes", filesDir.totalSpace)
            put("error", error)
        }
    }

    private fun hasStorageFor(bytes: Long, marginBytes: Long = 256L * 1024L * 1024L): Boolean {
        val needed = bytes.coerceAtLeast(0L) + marginBytes
        return filesDir.usableSpace > needed
    }

    private fun isBackgroundAudioImportRunning(): Boolean =
        audioCatalog.latestImportJob()?.optString("status") == "running"

    private fun resumeInterruptedAudioImportIfPossible() {
        val latest = audioCatalog.latestImportJob() ?: return
        if (latest.optString("status") !in setOf("running", "interrupted")) return
        val uriText = latest.optString("treeUri").ifBlank { privatePreferences.getString(resumedImportPreference, "") ?: "" }
        if (uriText.isBlank()) return
        val uri = Uri.parse(uriText)
        val permission = contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
        if (!permission) {
            audioCatalog.finishImportJob(latest.optString("id"), "permission_lost", latest.optInt("imported"), latest.optInt("skipped"), latest.optInt("failed") + 1, "A permissão persistente da pasta foi perdida.")
            return
        }
        val jobId = latest.optString("id").ifBlank { "audio_tree_${System.currentTimeMillis()}_${UUID.randomUUID()}" }
        try {
            scheduleAudioFolderImport(uri, jobId, latest.optString("sourceLabel").ifBlank { "Biblioteca sonora" }, resume = true)
            Log.i("BRASEIRO_AUDIO", "Importação persistente reprogramada: $jobId")
        } catch (error: Exception) {
            Log.e("BRASEIRO_AUDIO", "Falha ao reprogramar importação", error)
        }
    }

    private fun scheduleAudioFolderImport(
        treeUri: Uri,
        existingJobId: String? = null,
        sourceLabel: String? = null,
        resume: Boolean = false,
    ): String {
        val jobId = existingJobId ?: "audio_tree_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val label = sourceLabel ?: DocumentFile.fromTreeUri(this, treeUri)?.name ?: "Biblioteca sonora"
        if (!resume) audioCatalog.startImportJob(jobId, treeUri.toString(), label)
        val request = OneTimeWorkRequestBuilder<AudioLibraryImportWorker>()
            .setInputData(workDataOf(
                AudioLibraryImportWorker.KEY_TREE_URI to treeUri.toString(),
                AudioLibraryImportWorker.KEY_JOB_ID to jobId,
                AudioLibraryImportWorker.KEY_SOURCE_LABEL to label,
            ))
            .addTag(jobId)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            AudioLibraryImportWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        runOnUiThread { observeAudioImport(request.id, jobId) }
        return JSONObject().apply {
            put("jobId", jobId)
            put("scheduled", true)
            put("resumed", resume)
            put("imported", 0)
            put("skipped", 0)
            put("failed", 0)
            put("catalogCount", audioCatalog.count())
        }.toString()
    }

    private fun observeAudioImport(workId: UUID, jobId: String) {
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(workId).observe(this) { info ->
            if (info == null) return@observe
            val payload = if (info.state == WorkInfo.State.SUCCEEDED) info.outputData else info.progress
            val detail = JSONObject().apply {
                put("jobId", jobId)
                put("state", info.state.name.lowercase(Locale.ROOT))
                put("imported", payload.getInt("imported", 0))
                put("skipped", payload.getInt("skipped", 0))
                put("failed", payload.getInt("failed", 0))
                put("current", payload.getString("current") ?: "")
                put("catalogCount", payload.getInt("catalogCount", audioCatalog.count()))
                put("background", true)
            }
            evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-audio-progress',{detail:${detail}}));")
            if (info.state.isFinished) {
                evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-audio-changed',{detail:${detail}}));")
            }
        }
    }

    private fun logoutDrive(requestId: String) {
        lifecycleScope.launch {
            val id = JSONObject.quote(requestId)
            try {
                val token = decryptSecret(privatePreferences.getString(driveTokenPreference, "") ?: "")
                if (token.isNotBlank()) withContext(Dispatchers.IO) {
                    val encoded = URLEncoder.encode(token, "UTF-8")
                    val connection = URL("https://oauth2.googleapis.com/revoke?token=$encoded").openConnection() as HttpURLConnection
                    try {
                        connection.requestMethod = "POST"
                        connection.connectTimeout = 10_000
                        connection.readTimeout = 10_000
                        connection.responseCode
                    } finally { connection.disconnect() }
                }
                privatePreferences.edit().remove(driveTokenPreference).apply()
                evaluate("window.__braseiroDriveLogoutCallbacks?.[$id]?.resolve?.(true); delete window.__braseiroDriveLogoutCallbacks?.[$id];")
            } catch (error: Exception) {
                privatePreferences.edit().remove(driveTokenPreference).apply()
                val message = JSONObject.quote(error.message ?: "Falha ao revogar autorização do Google Drive.")
                evaluate("window.__braseiroDriveLogoutCallbacks?.[$id]?.reject?.(new Error($message)); delete window.__braseiroDriveLogoutCallbacks?.[$id];")
            }
        }
    }


    private fun collectUris(intent: Intent): List<Uri> = when {
        intent.clipData != null -> List(intent.clipData!!.itemCount) { intent.clipData!!.getItemAt(it).uri }
        intent.data != null -> listOf(intent.data!!)
        else -> emptyList()
    }

    private fun driveAuthorizationRequest(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.readonly")))
        .build()

    private fun requestDriveToken() {
        if (driveRequestPending) { deliverDriveError("A autorização do Google Drive já está em andamento."); return }
        driveRequestPending = true
        Identity.getAuthorizationClient(this).authorize(driveAuthorizationRequest())
            .addOnSuccessListener { result ->
                if (result.hasResolution() && result.pendingIntent != null) {
                    try { authorizationLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()) }
                    catch (error: Exception) { deliverDriveError("Não foi possível abrir a autorização: ${error.message}") }
                } else deliverDriveAuthorization(result)
            }
            .addOnFailureListener { error ->
                Log.e("BRASEIRO_DRIVE", "authorize failed", error)
                deliverDriveError("Falha ao solicitar autorização do Google Drive: ${error.message ?: "erro desconhecido"}")
            }
    }

    private fun deliverDriveAuthorization(result: AuthorizationResult) {
        val token = result.accessToken
        if (token.isNullOrBlank()) { deliverDriveError("O Google autorizou a conta, mas não devolveu o token do Drive."); return }
        driveRequestPending = false
        privatePreferences.edit().putString(driveTokenPreference, encryptSecret(token)).apply()
        val quoted = JSONObject.quote(token)
        evaluate("window.__braseiroDriveResolve?.($quoted); window.__braseiroDriveResolve=null; window.__braseiroDriveReject=null;")
    }

    private fun deliverDriveError(message: String) {
        driveRequestPending = false
        val quoted = JSONObject.quote(message)
        evaluate("window.__braseiroDriveReject?.(new Error($quoted)); window.__braseiroDriveResolve=null; window.__braseiroDriveReject=null;")
    }

    private fun evaluate(script: String) = runOnUiThread { if (::webView.isInitialized) webView.evaluateJavascript(script, null) }

    private fun emitAudioDiagnostic(
        action: String,
        channel: String,
        message: String,
        level: String = "success",
        soundId: String = "",
        soundName: String = "",
        backend: String = "Android",
        slot: String = "",
        scheduledAtMs: Long = 0L,
        result: String = ""
    ) {
        val payload = JSONObject().apply {
            put("action", action); put("channel", channel); put("message", message); put("level", level)
            put("soundId", soundId); put("soundName", soundName); put("backend", backend); put("slot", slot)
            put("scheduledAtMs", scheduledAtMs); put("actualAtMs", System.currentTimeMillis()); put("result", result)
        }
        evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-audio-diagnostic',{detail:${payload}}));")
    }

    private fun installJavascriptBridge() {
        if (!::webView.isInitialized || !trustedOriginActive) return
        evaluate("""
            (() => {
              window.__braseiroTtsCallbacks = window.__braseiroTtsCallbacks || {};
              window.__braseiroTtsConfigCallbacks = window.__braseiroTtsConfigCallbacks || {};
              window.__braseiroNativeSoundCallbacks = window.__braseiroNativeSoundCallbacks || {};
              window.__braseiroSongCallbacks = window.__braseiroSongCallbacks || {};
              window.__braseiroGeminiTtsCallbacks = window.__braseiroGeminiTtsCallbacks || {};
              window.__braseiroGeminiVoiceCallbacks = window.__braseiroGeminiVoiceCallbacks || {};
              window.__braseiroDiceCallbacks = window.__braseiroDiceCallbacks || {};
              // Migra automaticamente chaves salvas por versões WebView antigas.
              // Depois da cópia para o cofre Android, remove o texto do localStorage.
              try {
                const freeNames = ['braseiro.geminiApiKey.free', 'braseiro.geminiApiKey', 'geminiApiKey', 'GEMINI_API_KEY', 'API_KEY'];
                const paidNames = ['braseiro.geminiApiKey.paid', 'geminiApiKeyPaid'];
                const migrateTier = (tier, names) => {
                  if (BraseiroNative.hasGeminiApiKey(tier)) return;
                  for (const name of names) {
                    const value = (localStorage.getItem(name) || '').trim();
                    if (value) {
                      BraseiroNative.setGeminiApiKey(tier, value);
                      names.forEach(key => localStorage.removeItem(key));
                      break;
                    }
                  }
                };
                migrateTier('free', freeNames);
                migrateTier('paid', paidNames);
              } catch (error) { console.warn('Migração de chave antiga não concluída', error); }
              window.BraseiroAndroid = {
                requestDriveToken: () => new Promise((resolve, reject) => { window.__braseiroDriveResolve=resolve; window.__braseiroDriveReject=reject; BraseiroNative.requestDriveToken(); }),
                logoutDrive: () => new Promise((resolve,reject) => { const id='drive_logout_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroDriveLogoutCallbacks=window.__braseiroDriveLogoutCallbacks||{}; window.__braseiroDriveLogoutCallbacks[id]={resolve,reject}; BraseiroNative.logoutDrive(id); }),
                hasGeminiApiKey: tier => BraseiroNative.hasGeminiApiKey(String(tier||'free')),
                setGeminiApiKey: (tier,value) => BraseiroNative.setGeminiApiKey(String(tier||'free'), value || ''),
                geminiRequest: (model,body,paidOnly=false) => new Promise((resolve,reject) => { const id='gemini_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroGeminiCallbacks=window.__braseiroGeminiCallbacks||{}; window.__braseiroGeminiCallbacks[id]={resolve,reject}; BraseiroNative.geminiRequest(String(model),String(body),Boolean(paidOnly),id); }),
                generateTts: (model,text,voice,style,paidOnly=false) => new Promise((resolve,reject) => { const id='gemini_tts_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroGeminiTtsCallbacks[id]={resolve,reject}; BraseiroNative.generateTts(String(model),String(text||''),String(voice||'Charon'),String(style||'natural'),Boolean(paidOnly),id); }),
                playGeneratedVoice: (token,volume=1) => new Promise((resolve,reject) => { const id='gemini_voice_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroGeminiVoiceCallbacks[id]={resolve,reject}; BraseiroNative.playGeneratedVoice(String(token||''),Number(volume||1),id); }),
                isNativeTtsAvailable: () => BraseiroNative.isTtsReady(),
                getTtsCatalog: () => Promise.resolve(JSON.parse(BraseiroNative.getTtsCatalog() || '{}')),
                selectTtsEngine: packageName => new Promise((resolve,reject) => { const id='tts_cfg_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsConfigCallbacks[id]={resolve,reject}; BraseiroNative.selectTtsEngine(String(packageName||''),id); }),
                selectTtsVoice: voiceName => new Promise((resolve,reject) => { const id='tts_voice_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsConfigCallbacks[id]={resolve,reject}; BraseiroNative.selectTtsVoice(String(voiceName||''),id); }),
                setNeuralTtsRate: rate => new Promise((resolve,reject) => { const id='tts_rate_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsConfigCallbacks[id]={resolve,reject}; BraseiroNative.setNeuralTtsRate(Number(rate||1),id); }),
                prewarmTts: () => Promise.resolve(BraseiroNative.prewarmTts()),
                speakText: (text, rate=0.96) => new Promise((resolve,reject) => { const id='tts_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsCallbacks[id]={resolve,reject}; BraseiroNative.speakText(String(text||''), Number(rate||0.96), id); }),
                speakTextWithCues: (text, rate=0.96, cues=[]) => new Promise((resolve,reject) => { const id='tts_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsCallbacks[id]={resolve,reject}; BraseiroNative.speakTextWithCues(String(text||''), Number(rate||0.96), JSON.stringify(Array.isArray(cues)?cues:[]), id); }),
                stopSpeech: () => BraseiroNative.stopSpeech(),
                importAudioFiles: () => new Promise((resolve,reject) => { window.__braseiroAudioResolve=resolve; window.__braseiroAudioReject=reject; BraseiroNative.importAudioFiles(); }),
                importAudioFolder: () => new Promise((resolve,reject) => { window.__braseiroAudioResolve=resolve; window.__braseiroAudioReject=reject; BraseiroNative.importAudioFolder(); }),
                listNativeSounds: () => Promise.resolve(JSON.parse(BraseiroNative.listNativeSounds() || '[]')),
                getNativeSoundKeywordLibrary: () => Promise.resolve(BraseiroNative.getNativeSoundKeywordLibrary() || ''),
                selectCatalogAudio: request => Promise.resolve(JSON.parse(BraseiroNative.selectCatalogAudio(JSON.stringify(request || {})) || 'null')),
                getAudioCatalogStatus: () => Promise.resolve(JSON.parse(BraseiroNative.getAudioCatalogStatus() || '{}')),
                reindexAudioCatalog: () => Promise.resolve(JSON.parse(BraseiroNative.reindexAudioCatalog() || '{}')),
                markAudioFailure: id => BraseiroNative.markAudioFailure(String(id || '')),
                markAudioAvailable: id => BraseiroNative.markAudioAvailable(String(id || '')),
                beginAudioSession: (id,sceneId='',backend='hybrid') => BraseiroNative.beginAudioSession(String(id||''),String(sceneId||''),String(backend||'hybrid')),
                finishAudioSession: (id,status='finished') => BraseiroNative.finishAudioSession(String(id||''),String(status||'finished')),
                preloadNativeSounds: ids => new Promise((resolve,reject) => { const id='audio_preload_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroAudioPreloadCallbacks=window.__braseiroAudioPreloadCallbacks||{}; window.__braseiroAudioPreloadCallbacks[id]={resolve,reject}; BraseiroNative.preloadNativeSounds(JSON.stringify(Array.isArray(ids)?ids:[]),id); }),
                playNativeSound: (id,channel,loop,volume,slot='base',scheduledAtMs=Date.now()) => new Promise((resolve,reject) => { const requestId='native_sound_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroNativeSoundCallbacks[requestId]={resolve,reject}; BraseiroNative.playNativeSoundAsync(String(id),String(channel),Boolean(loop),Number(volume),String(slot),Number(scheduledAtMs),requestId); }),
                stopNativeAudio: (channel='',slot='') => BraseiroNative.stopNativeAudio(String(channel||''),String(slot||'')),
                setNativeAudioVolume: (channel,volume) => BraseiroNative.setNativeAudioVolume(String(channel),Number(volume)),
                getNativeAudioStatus: () => Promise.resolve(JSON.parse(BraseiroNative.getNativeAudioStatus() || '{}')),
                saveRecoveryBackup: (campaignId,json) => BraseiroNative.saveRecoveryBackup(String(campaignId||''),String(json||'')),
                getLatestRecoveryBackup: campaignId => BraseiroNative.getLatestRecoveryBackup(String(campaignId||'')) || null,
                playDriveSong: (fileId,name,mimeType='',size=0) => new Promise((resolve,reject) => { const id='song_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroSongCallbacks[id]={resolve,reject}; BraseiroNative.playDriveSong(String(fileId),String(name),String(mimeType||''),Number(size||0),id); }),
                resumeSong: () => Promise.resolve(BraseiroNative.resumeSong()),
                pauseSong: () => Promise.resolve(BraseiroNative.pauseSong()),
                stopSong: () => Promise.resolve(BraseiroNative.stopSong()),
                getSongStatus: () => Promise.resolve(JSON.parse(BraseiroNative.getSongStatus() || '{}')),
                clearSongCache: () => Promise.resolve(BraseiroNative.clearSongCache()),
                getNativeHealthStatus: () => Promise.resolve(JSON.parse(BraseiroNative.getNativeHealthStatus() || '{}')),
                deleteNativeSound: id => Promise.resolve(BraseiroNative.deleteNativeSound(String(id))),
                clearNativeSounds: () => Promise.resolve(BraseiroNative.clearNativeSounds()),
                exportBackup: (fileName,json) => new Promise((resolve,reject) => { const id='export_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroExportCallbacks=window.__braseiroExportCallbacks||{}; window.__braseiroExportCallbacks[id]={resolve,reject}; BraseiroNative.exportDocument(String(fileName),String(json),id); }),
                shareText: (title,text) => Promise.resolve(BraseiroNative.shareText(String(title||''),String(text||''))),
                setSharedImageTarget: json => Promise.resolve(BraseiroNative.setSharedImageTarget(String(json||''))),
                getSharedImageTarget: () => Promise.resolve(BraseiroNative.getSharedImageTarget() || null),
                clearSharedImageTarget: () => Promise.resolve(BraseiroNative.clearSharedImageTarget()),
                consumeSharedImage: () => Promise.resolve(BraseiroNative.consumeSharedImage() || null),
                peekSharedImage: () => Promise.resolve(BraseiroNative.peekSharedImage() || null),
                clearSharedImage: () => Promise.resolve(BraseiroNative.clearSharedImage()),
                barbaraHealth: () => Promise.resolve(JSON.parse(BraseiroNative.barbaraHealth() || '{}')),
                barbaraDeviceProfile: () => Promise.resolve(JSON.parse(BraseiroNative.barbaraDeviceProfile() || '{}')),
                barbaraSupervisorInspect: (hostState,text,importance='normal',inputChannel='PLAYER_ACTION') => Promise.resolve(JSON.parse(BraseiroNative.barbaraSupervisorInspect(JSON.stringify(hostState||{}),String(text||''),String(importance||'normal'),String(inputChannel||'PLAYER_ACTION')) || '{}')),
                barbaraSupervisorFinalize: (hostState,text,requestId,narration,importance='normal',inputChannel='PLAYER_ACTION',resolution=null,presentation=null) => Promise.resolve(JSON.parse(BraseiroNative.barbaraSupervisorFinalize(JSON.stringify(hostState||{}),String(text||''),String(requestId||''),String(narration||''),String(importance||'normal'),String(inputChannel||'PLAYER_ACTION'),JSON.stringify(resolution||null),JSON.stringify(presentation||null)) || '{}')),
                barbaraAudioScenePlan: (events,context={},catalog={},mix={},narrationActive=false) => Promise.resolve(JSON.parse(BraseiroNative.barbaraAudioScenePlan(JSON.stringify(events||[]),JSON.stringify(context||{}),JSON.stringify(catalog||{}),JSON.stringify(mix||{}),Boolean(narrationActive)) || '{}')),
                barbaraVisiblePois: (canonical=[],campaign=[],procedural=[],gm=false) => Promise.resolve(JSON.parse(BraseiroNative.barbaraVisiblePois(JSON.stringify(canonical||[]),JSON.stringify(campaign||[]),JSON.stringify(procedural||[]),Boolean(gm)) || '[]')),
                barbaraContract: () => Promise.resolve(JSON.parse(BraseiroNative.barbaraContract() || '{}')),
                barbaraPlan: (action,importance='normal',narrative=true,nonAdvancing=false,location='',imprint='',inputChannel='PLAYER_ACTION',storyOccasion='continuation') => Promise.resolve(JSON.parse(BraseiroNative.barbaraPlan(String(action||''),String(importance||'normal'),Boolean(narrative),Boolean(nonAdvancing),String(location||''),String(imprint||''),String(inputChannel||'PLAYER_ACTION'),String(storyOccasion||'continuation')) || '{}')),
                barbaraValidate: (envelope,action,importance='normal',occasion='continuation',inputChannel='PLAYER_ACTION') => Promise.resolve(JSON.parse(BraseiroNative.barbaraValidate(JSON.stringify(envelope||{}),String(action||''),String(importance||'normal'),String(occasion||'continuation'),String(inputChannel||'PLAYER_ACTION')) || '{}')),
                barbaraValidateState: hostState => Promise.resolve(JSON.parse(BraseiroNative.barbaraValidateState(JSON.stringify(hostState||{})) || '{}')),
                barbaraInterpretRoll: (featResults,successResults,target,mode,weary,modifier,hostRoll) => Promise.resolve(JSON.parse(BraseiroNative.barbaraInterpretRoll(JSON.stringify(featResults||[]),JSON.stringify(successResults||[]),Number(target||14),String(mode||'normal'),Boolean(weary),Number(modifier||0),JSON.stringify(hostRoll||{})) || '{}')),
                rollOneRingPhysical: (successDiceCount,rollMode='normal',actorId='',actorName='') => new Promise((resolve,reject) => { const id='one_ring_dice_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroDiceCallbacks[id]={resolve,reject}; BraseiroNative.rollOneRingPhysical(Number(successDiceCount||0),String(rollMode||'normal'),String(actorId||''),String(actorName||''),id); }),
                rollPhysicalDice: (formula,actorId='',actorName='') => new Promise((resolve,reject) => { const id='physical_dice_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroDiceCallbacks[id]={resolve,reject}; BraseiroNative.rollPhysicalDice(String(formula||''),String(actorId||''),String(actorName||''),id); })
              };
              navigator.serviceWorker?.getRegistrations?.().then(items => items.forEach(item => item.unregister()));
              window.caches?.keys?.().then(keys => keys.forEach(key => caches.delete(key)));
              window.dispatchEvent(new Event('braseiro-native-ready'));
            })();
        """.trimIndent())
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)
        // singleTask traz a mesa já aberta para a frente. A entrega é publicada
        // depois que a WebView retomou, sem criar uma segunda campanha/tela.
        webView.post { handleSharedImageIntent(newIntent) }
    }

    override fun onResume() {
        super.onResume()
        // O compartilhamento pode chegar enquanto a WebView está pausada. Reenvia
        // a imagem pendente toda vez que o aplicativo volta ao primeiro plano.
        refreshPendingSharedImage()
    }

    private fun handleSharedImageIntent(sourceIntent: Intent?) {
        val incoming = sourceIntent
        pendingSharedImageError = incoming?.getStringExtra(SharedImageInbox.EXTRA_IMPORT_ERROR)
        if (incoming?.action in setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE)) {
            // Compatibilidade defensiva com atalhos/instalações antigas. A versão
            // atual entrega ACTION_SEND à IncomingImageActivity, nunca à WebView.
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) { SharedImageInbox.storeFromIntent(this@MainActivity, incoming!!) }
                    refreshPendingSharedImage()
                } catch (error: Exception) {
                    pendingSharedImageError = error.message ?: "Não foi possível receber a imagem compartilhada."
                    dispatchPendingSharedImage()
                }
            }
            return
        }
        refreshPendingSharedImage()
    }

    private fun refreshPendingSharedImage() {
        pendingSharedImageReference = SharedImageInbox.pendingWebUrl(this, trustedAppOrigin)
        dispatchPendingSharedImage()
    }

    private fun dispatchPendingSharedImage() {
        pendingSharedImageError?.let { error ->
            val message = JSONObject.quote(error)
            evaluate("window.dispatchEvent(new CustomEvent('braseiro-shared-image-error',{detail:$message}));")
            pendingSharedImageError = null
        }
        val reference = pendingSharedImageReference ?: return
        val quoted = JSONObject.quote(reference)
        evaluate("window.dispatchEvent(new CustomEvent('braseiro-shared-image',{detail:$quoted}));")
    }

    private fun displayName(uri: Uri): String {
        var name: String? = null
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use { if (it.moveToFirst()) name = it.getString(0) }
        return name ?: uri.lastPathSegment ?: "audio_${System.currentTimeMillis()}"
    }

    private fun normalizeFileName(value: String): String = value.replace(Regex("[^A-Za-z0-9._ -]"), "_").takeLast(180)

    private fun isAudioOverrideName(name: String): Boolean =
        name.substringAfterLast('/').substringAfterLast('\\').equals("audio_overrides.json", ignoreCase = true)

    private fun validateAndSaveAudioOverrides(text: String): String {
        val clean = text.removePrefix("\uFEFF").trim()
        if (clean.isBlank()) throw IllegalArgumentException("audio_overrides.json está vazio.")
        if (JSONTokener(clean).nextValue() !is JSONObject) throw IllegalArgumentException("audio_overrides.json precisa conter um objeto JSON.")
        val temporary = File(audioDir, "audio_overrides.json.tmp")
        temporary.writeText(clean, Charsets.UTF_8)
        if (audioOverridesFile.exists()) audioOverridesFile.delete()
        if (!temporary.renameTo(audioOverridesFile)) {
            audioOverridesFile.writeText(clean, Charsets.UTF_8)
            temporary.delete()
        }
        return clean
    }

    private fun audioDurationMs(file: File): Long = try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally { retriever.release() }
    } catch (_: Exception) { 0L }

    private fun currentOverrides(): Map<String, AudioOverride> = AudioOverrideParser(audioOverridesFile).load()

    private fun currentKeywordTags(): Map<String, List<String>> = try {
        if (!soundKeywordLibraryFile.exists()) emptyMap() else {
            val parsed = JSONTokener(soundKeywordLibraryFile.readText(Charsets.UTF_8)).nextValue()
            if (parsed !is JSONObject) emptyMap() else buildMap {
                val keys = parsed.keys()
                while (keys.hasNext()) {
                    val fileName = keys.next()
                    val value = parsed.opt(fileName)
                    val tags = when (value) {
                        is JSONArray -> buildList {
                            for (index in 0 until value.length()) value.optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
                        }
                        is String -> listOf(value).filter(String::isNotBlank)
                        else -> emptyList()
                    }
                    put(fileName.lowercase(Locale.ROOT), tags)
                    put(fileName.substringAfterLast('/').substringAfterLast('\\').lowercase(Locale.ROOT), tags)
                }
            }
        }
    } catch (error: Exception) {
        Log.w("BRASEIRO_AUDIO", "Não foi possível incorporar as palavras-chave ao catálogo SQLite.", error)
        emptyMap()
    }

    private fun indexCatalogEntry(
        id: String,
        originalName: String,
        storedFile: File,
        overrideMap: Map<String, AudioOverride> = currentOverrides(),
        keywordMap: Map<String, List<String>> = currentKeywordTags(),
    ): AudioCatalogEntry {
        val lookup = originalName.substringAfterLast('/').substringAfterLast('\\').lowercase(Locale.ROOT)
        val override = overrideMap[originalName.lowercase(Locale.ROOT)] ?: overrideMap[lookup]
        val keywordTags = keywordMap[originalName.lowercase(Locale.ROOT)] ?: keywordMap[lookup] ?: emptyList()
        val classified = AudioFileClassifier.classify("$originalName ${keywordTags.joinToString(" ")}", override)
        val measuredDurationMs = audioDurationMs(storedFile)
        return AudioCatalogEntry(
            legacyId = id,
            fileName = originalName,
            storedFileName = storedFile.name,
            fileUri = storedFile.absolutePath,
            channel = classified.channel,
            familyKey = classified.familyKey,
            tags = (classified.tags + keywordTags).distinct(),
            environments = classified.environments,
            weather = classified.weather,
            intensity = classified.intensity,
            timeOfDay = classified.timeOfDay,
            durationMs = measuredDurationMs,
            sizeBytes = storedFile.length(),
            loopable = classified.loopable && measuredDurationMs >= 10_000L,
            weight = classified.weight,
        )
    }

    private fun rebuildAudioCatalog(): Int {
        val index = readAudioIndex()
        if (audioIndexFile.exists() && audioIndexFile.length() > 2L && index.length() == 0 && audioCatalog.count() > 0) {
            Log.w("BRASEIRO_AUDIO", "Índice legado não pôde ser lido; catálogo SQLite preservado.")
            return audioCatalog.count()
        }
        val overrides = currentOverrides()
        val keywordTags = currentKeywordTags()
        val retainedIds = mutableSetOf<String>()
        var indexed = 0
        for (i in 0 until index.length()) {
            val item = index.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val file = File(audioDir, item.optString("fileName"))
            if (id.isBlank() || !file.exists()) continue
            val fallbackExtension = file.extension.takeIf(String::isNotBlank)?.let { ".$it" } ?: ""
            val original = item.optString("originalFileName").ifBlank { item.optString("name") + fallbackExtension }
            audioCatalog.upsert(indexCatalogEntry(id, original, file, overrides, keywordTags))
            retainedIds += id
            indexed += 1
        }
        audioCatalog.listAll().filterNot { retainedIds.contains(it.legacyId) }.forEach { audioCatalog.delete(it.legacyId) }
        return indexed
    }

    private fun readAudioIndex(): JSONArray = try {
        if (!audioIndexFile.exists()) JSONArray() else JSONArray(audioIndexFile.readText(Charsets.UTF_8))
    } catch (error: Exception) {
        Log.e("BRASEIRO_AUDIO", "index read failed", error); JSONArray()
    }

    private fun writeAudioIndex(index: JSONArray) {
        val temporary = File(audioDir, "index.json.tmp")
        temporary.writeText(index.toString(), Charsets.UTF_8)
        if (audioIndexFile.exists()) audioIndexFile.delete()
        if (!temporary.renameTo(audioIndexFile)) {
            audioIndexFile.writeText(index.toString(), Charsets.UTF_8); temporary.delete()
        }
    }

    private fun isAudioName(name: String): Boolean = Regex("\\.(mp3|wav|ogg|m4a|aac|flac|opus)$", RegexOption.IGNORE_CASE).containsMatchIn(name)

    private fun isSoundKeywordLibraryName(name: String): Boolean =
        name.substringAfterLast('/').substringAfterLast('\\').equals("biblioteca_sons.json", ignoreCase = true)

    private fun validateAndSaveSoundKeywordLibrary(text: String): String {
        val clean = text.removePrefix("\uFEFF").trim()
        if (clean.isBlank()) throw IllegalArgumentException("biblioteca_sons.json está vazio.")
        val parsed = JSONTokener(clean).nextValue()
        if (parsed !is JSONObject && parsed !is JSONArray) throw IllegalArgumentException("biblioteca_sons.json precisa conter um objeto ou uma lista JSON.")
        val temporary = File(audioDir, "biblioteca_sons.json.tmp")
        temporary.writeText(clean, Charsets.UTF_8)
        if (soundKeywordLibraryFile.exists()) soundKeywordLibraryFile.delete()
        if (!temporary.renameTo(soundKeywordLibraryFile)) {
            soundKeywordLibraryFile.writeText(clean, Charsets.UTF_8)
            temporary.delete()
        }
        return clean
    }

    private fun readSoundKeywordLibrary(): String = try {
        if (soundKeywordLibraryFile.exists()) soundKeywordLibraryFile.readText(Charsets.UTF_8) else ""
    } catch (error: Exception) {
        Log.e("BRASEIRO_AUDIO", "keyword library read failed", error); ""
    }

    private fun preloadNativeSoundIds(ids: List<String>): List<Int> {
        val samples = mutableListOf<Int>()
        for (id in ids.distinct()) {
            val existing = soundPoolSamples[id]
            if (existing != null) { samples += existing; continue }
            val found = findAudio(id) ?: continue
            val durationMs = found.first.optLong("durationMs", 0L)
            val fileBytes = found.second.length()
            // SoundPool is deliberately restricted to small one-shot effects.
            // Large soundtrack files can decode only partially and sound like
            // they stop after ~1-2 seconds. Those must use MediaPlayer.
            if (durationMs <= 0L || durationMs > 6_000L || fileBytes > 1_000_000L) continue
            try {
                val sampleId = soundPool.load(found.second.absolutePath, 1)
                if (sampleId > 0) { soundPoolSamples[id] = sampleId; samples += sampleId }
            } catch (error: Exception) {
                Log.w("BRASEIRO_AUDIO", "preload failed $id", error)
            }
        }
        return samples
    }

    private fun unloadNativeSound(id: String) {
        val sample = soundPoolSamples.remove(id) ?: return
        loadedSoundPoolSamples.remove(sample)
        try { soundPool.unload(sample) } catch (_: Exception) {}
    }

    private fun saveAudioStream(name: String, mime: String, input: java.io.InputStream, originalSize: Long, index: JSONArray, signatures: MutableSet<String>): Boolean {
        if (!isAudioName(name)) return false
        val safeName = normalizeFileName(name.substringAfterLast('/').substringAfterLast('\\'))
        val signature = "${safeName.lowercase(Locale.ROOT)}_$originalSize"
        if (signatures.contains(signature)) return false
        if (originalSize > 0 && !hasStorageFor(originalSize)) throw IllegalStateException("Espaço insuficiente para importar $safeName com a margem de segurança de 256 MB.")
        val id = "native_${UUID.randomUUID()}"
        val extension = safeName.substringAfterLast('.', "bin")
        val target = File(audioDir, "$id.$extension")
        val temporary = File(audioDir, "$id.$extension.partial")
        try {
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(128 * 1024)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    if (copied % (16L * 1024L * 1024L) < buffer.size && !hasStorageFor(0L)) {
                        throw IllegalStateException("Espaço livre abaixo da margem de segurança durante a importação de $safeName.")
                    }
                }
                output.fd.sync()
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            val catalogEntry = indexCatalogEntry(id, safeName, target)
            val legacy = catalogEntry.toLegacyJson()
            legacy.put("mimeType", mime.ifBlank { "audio/*" })
            legacy.put("signature", signature)
            legacy.put("lastModified", System.currentTimeMillis())
            legacy.put("createdAt", System.currentTimeMillis())
            legacy.put("originalFileName", safeName)
            index.put(legacy)
            audioCatalog.upsert(catalogEntry)
            signatures.add(signature)
            return true
        } catch (error: Exception) {
            temporary.delete()
            target.delete()
            throw error
        }
    }

    private fun emitAudioProgress(imported: Int, skipped: Int, failed: Int, current: String, jobId: String? = null) {
        if (!jobId.isNullOrBlank()) audioCatalog.updateImportJob(jobId, imported, skipped, failed, current)
        val detail = JSONObject().apply {
            put("imported", imported); put("skipped", skipped); put("failed", failed); put("current", current)
        }
        evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-audio-progress',{detail:${detail}}));")
    }

    private fun importDocumentTreeNode(
        node: DocumentFile,
        index: JSONArray,
        signatures: MutableSet<String>,
        counters: IntArray,
        errors: JSONArray,
        keywordLibrary: Array<String?>,
        jobId: String,
    ) {
        if (node.isDirectory) {
            node.listFiles().forEach { child -> importDocumentTreeNode(child, index, signatures, counters, errors, keywordLibrary, jobId) }
            return
        }
        val name = node.name ?: return
        val mime = node.type ?: "application/octet-stream"
        try {
            if (name.endsWith(".zip", true) || mime.contains("zip", true)) {
                contentResolver.openInputStream(node.uri)?.use { stream ->
                    ZipInputStream(BufferedInputStream(stream, 128 * 1024)).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                when {
                                    isSoundKeywordLibraryName(entry.name) -> {
                                        keywordLibrary[0] = validateAndSaveSoundKeywordLibrary(zip.readBytes().toString(Charsets.UTF_8))
                                    }
                                    isAudioOverrideName(entry.name) -> {
                                        validateAndSaveAudioOverrides(zip.readBytes().toString(Charsets.UTF_8))
                                    }
                                    isAudioName(entry.name) -> {
                                        try {
                                            if (saveAudioStream(entry.name, "audio/*", zip, entry.size, index, signatures)) counters[0]++ else counters[1]++
                                        } catch (error: Exception) { counters[2]++; errors.put("${entry.name}: ${error.message}") }
                                        emitAudioProgress(counters[0], counters[1], counters[2], entry.name, jobId)
                                    }
                                }
                            }
                            zip.closeEntry(); entry = zip.nextEntry
                        }
                    }
                } ?: throw IllegalStateException("Não foi possível abrir $name")
            } else if (isSoundKeywordLibraryName(name)) {
                val text = contentResolver.openInputStream(node.uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("Não foi possível abrir $name")
                keywordLibrary[0] = validateAndSaveSoundKeywordLibrary(text)
            } else if (isAudioOverrideName(name)) {
                val text = contentResolver.openInputStream(node.uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException("Não foi possível abrir $name")
                validateAndSaveAudioOverrides(text)
            } else if (isAudioName(name) || mime.startsWith("audio/")) {
                contentResolver.openInputStream(node.uri)?.use { input ->
                    if (saveAudioStream(name, mime, input, node.length(), index, signatures)) counters[0]++ else counters[1]++
                } ?: throw IllegalStateException("Não foi possível abrir $name")
                emitAudioProgress(counters[0], counters[1], counters[2], name, jobId)
            } else counters[1]++
        } catch (error: Exception) {
            counters[2]++; errors.put("$name: ${error.message}"); emitAudioProgress(counters[0], counters[1], counters[2], name, jobId)
            Log.e("BRASEIRO_AUDIO", "folder import failed ${node.uri}", error)
        }
    }

    private fun importAudioTree(treeUri: Uri, existingJobId: String? = null, resume: Boolean = false): String {
        val jobId = existingJobId ?: "audio_tree_${System.currentTimeMillis()}_${UUID.randomUUID()}"
        val root = DocumentFile.fromTreeUri(this, treeUri) ?: run {
            audioCatalog.finishImportJob(jobId, "failed", 0, 0, 1, "O Android não abriu a pasta selecionada.")
            return JSONObject().apply {
                put("jobId", jobId); put("imported", 0); put("skipped", 0); put("failed", 1); put("errors", JSONArray().put("O Android não abriu a pasta selecionada."))
            }.toString()
        }
        if (!resume) audioCatalog.startImportJob(jobId, treeUri.toString(), root.name ?: "Biblioteca sonora")
        privatePreferences.edit().putString(resumedImportPreference, treeUri.toString()).apply()
        val index = readAudioIndex()
        val signatures = mutableSetOf<String>()
        for (i in 0 until index.length()) signatures.add(index.optJSONObject(i)?.optString("signature") ?: "")
        val counters = intArrayOf(0, 0, 0)
        val errors = JSONArray()
        val keywordLibrary = arrayOfNulls<String>(1)
        return try {
            importDocumentTreeNode(root, index, signatures, counters, errors, keywordLibrary, jobId)
            writeAudioIndex(index)
            val catalogCount = rebuildAudioCatalog()
            val status = if (counters[2] == 0) "completed" else "completed_with_errors"
            audioCatalog.finishImportJob(jobId, status, counters[0], counters[1], counters[2], if (counters[2] > 0) errors.toString() else null)
            JSONObject().apply {
                put("jobId", jobId); put("resumed", resume)
                put("imported", counters[0]); put("skipped", counters[1]); put("failed", counters[2]); put("errors", errors)
                put("keywordLibraryText", keywordLibrary[0] ?: readSoundKeywordLibrary())
                put("catalogCount", catalogCount)
            }.toString()
        } catch (error: Exception) {
            audioCatalog.finishImportJob(jobId, "interrupted", counters[0], counters[1], counters[2] + 1, error.message)
            throw error
        }
    }

    private fun importAudioUris(uris: List<Uri>): String {
        val index = readAudioIndex()
        val signatures = mutableSetOf<String>()
        for (i in 0 until index.length()) signatures.add(index.optJSONObject(i)?.optString("signature") ?: "")
        var imported = 0; var skipped = 0; var failed = 0
        val errors = JSONArray(); var keywordLibrary: String? = null
        for (uri in uris) {
            try {
                val name = displayName(uri)
                val mime = contentResolver.getType(uri) ?: "application/octet-stream"
                if (name.endsWith(".zip", true) || mime.contains("zip", true)) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        ZipInputStream(BufferedInputStream(stream, 128 * 1024)).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    when {
                                        isSoundKeywordLibraryName(entry.name) -> keywordLibrary = validateAndSaveSoundKeywordLibrary(zip.readBytes().toString(Charsets.UTF_8))
                                        isAudioOverrideName(entry.name) -> validateAndSaveAudioOverrides(zip.readBytes().toString(Charsets.UTF_8))
                                        isAudioName(entry.name) -> {
                                            try {
                                                if (saveAudioStream(entry.name, "audio/*", zip, entry.size, index, signatures)) imported++ else skipped++
                                            } catch (error: Exception) { failed++; errors.put("${entry.name}: ${error.message}") }
                                            emitAudioProgress(imported, skipped, failed, entry.name)
                                        }
                                    }
                                }
                                zip.closeEntry(); entry = zip.nextEntry
                            }
                        }
                    } ?: throw IllegalStateException("Não foi possível abrir $name")
                } else if (isSoundKeywordLibraryName(name)) {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw IllegalStateException("Não foi possível abrir $name")
                    keywordLibrary = validateAndSaveSoundKeywordLibrary(text)
                } else if (isAudioOverrideName(name)) {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw IllegalStateException("Não foi possível abrir $name")
                    validateAndSaveAudioOverrides(text)
                } else if (isAudioName(name) || mime.startsWith("audio/")) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val size = try { contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L } catch (_: Exception) { -1L }
                        if (saveAudioStream(name, mime, input, size, index, signatures)) imported++ else skipped++
                    } ?: throw IllegalStateException("Não foi possível abrir $name")
                    emitAudioProgress(imported, skipped, failed, name)
                } else skipped++
            } catch (error: Exception) {
                failed++; errors.put("${displayName(uri)}: ${error.message}")
                emitAudioProgress(imported, skipped, failed, displayName(uri))
                Log.e("BRASEIRO_AUDIO", "import failed $uri", error)
            }
        }
        writeAudioIndex(index)
        val catalogCount = rebuildAudioCatalog()
        return JSONObject().apply {
            put("imported", imported); put("skipped", skipped); put("failed", failed); put("errors", errors)
            put("keywordLibraryText", keywordLibrary ?: readSoundKeywordLibrary())
            put("catalogCount", catalogCount)
        }.toString()
    }

    private fun findAudio(id: String): Pair<JSONObject, File>? {
        val index = readAudioIndex()
        for (i in 0 until index.length()) {
            val item = index.optJSONObject(i) ?: continue
            if (item.optString("id") == id || item.optString("nativeId") == id) {
                val file = File(audioDir, item.optString("fileName"))
                if (file.exists()) return item to file
            }
        }
        return null
    }

    private fun fadePlayer(player: MediaPlayer, from: Float, to: Float, durationMs: Long, releaseAtEnd: Boolean = false) {
        val steps = 30
        lifecycleScope.launch {
            for (step in 0..steps) {
                val fraction = step / steps.toFloat()
                val value = from + (to - from) * fraction
                try { player.setVolume(value, value) } catch (_: Exception) { break }
                delay((durationMs / steps).coerceAtLeast(20L))
            }
            if (releaseAtEnd) {
                try { player.stop() } catch (_: Exception) {}
                try { player.release() } catch (_: Exception) {}
            }
        }
    }

    private fun playNativeSound(id: String, channel: String, loop: Boolean, volume: Float, slot: String, scheduledAtMs: Long = System.currentTimeMillis(), completion: ((Boolean, String?) -> Unit)? = null): Boolean {
        if (!requestNativeAudioFocus()) {
            emitAudioDiagnostic("native-focus-denied", channel, "O Android não concedeu foco de áudio.", "warning", id, result = "bloqueado")
            completion?.invoke(false, "O Android não concedeu foco de áudio.")
            return false
        }
        val found = findAudio(id) ?: run {
            audioCatalog.markFailure(id)
            emitAudioDiagnostic("native-file-missing", channel, "Arquivo nativo não encontrado para o ID $id.", "error", id, backend = "Android", slot = slot, scheduledAtMs = scheduledAtMs, result = "ausente")
            completion?.invoke(false, "Arquivo nativo não encontrado.")
            return false
        }
        val safeVolume = volume.coerceIn(0f, 1f)
        val knownDurationMs = found.first.optLong("durationMs", 0L)
        if (loop && knownDurationMs in 1L until 10_000L) {
            emitAudioDiagnostic("native-loop-rejected-short", channel, "Loop recusado: ${found.first.optString("name")} tem apenas ${knownDurationMs} ms.", "warning", id, found.first.optString("name"), "Android", slot, scheduledAtMs, "rejeitado")
            completion?.invoke(false, "Arquivos com menos de 10 segundos não podem ser usados em loop.")
            abandonNativeAudioFocusIfIdle()
            return false
        }
        val soundPoolEligible = channel == "sfx" && !loop &&
            knownDurationMs in 1L..6_000L && found.second.length() <= 1_000_000L
        if (soundPoolEligible) {
            val sampleId = soundPoolSamples[id]
            if (sampleId != null && loadedSoundPoolSamples.contains(sampleId)) {
                val streamId = soundPool.play(sampleId, safeVolume, safeVolume, 1, 0, 1f)
                if (streamId != 0) {
                    activeSoundPoolStreams[streamId] = id
                    emitAudioDiagnostic("native-sfx-started", channel, "SFX iniciado pelo SoundPool.", "success", id, found.first.optString("name"), "SoundPool", slot, scheduledAtMs, "reproduzindo")
                    completion?.invoke(true, null)
                    lifecycleScope.launch { delay(15_000); activeSoundPoolStreams.remove(streamId); abandonNativeAudioFocusIfIdle() }
                    return true
                }
            }
            preloadNativeSoundIds(listOf(id))
        }
        val key = if (loop) "$channel:${slot.ifBlank { "base" }}" else "$channel:${UUID.randomUUID()}"
        val previousLoop = if (loop) nativePlayers.remove(key) else null
        return try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(if (channel == "voice") AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY else AudioAttributes.USAGE_GAME)
                        .setContentType(if (channel == "voice") AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(found.second.absolutePath)
                isLooping = loop
                setVolume(if (loop) 0f else safeVolume, if (loop) 0f else safeVolume)
                setOnPreparedListener {
                    if (loop && it.duration < 10_000) {
                        emitAudioDiagnostic("native-loop-rejected-short", channel, "Loop recusado: ${found.first.optString("name")} tem apenas ${it.duration} ms.", "warning", id, found.first.optString("name"), "MediaPlayer", slot, scheduledAtMs, "rejeitado")
                        nativePlayers.remove(key)
                        it.release()
                        previousLoop?.let { old -> nativePlayers[key] = old }
                        completion?.invoke(false, "Arquivos com menos de 10 segundos não podem ser usados em loop.")
                    } else if (channel == "sfx" && it.duration > 14_000) {
                        audioCatalog.markFailure(id)
                        Log.w("BRASEIRO_AUDIO", "SFX longo rejeitado: ${found.first.optString("name")} (${it.duration}ms)")
                        emitAudioDiagnostic("native-sfx-rejected-duration", channel, "SFX longo demais: ${found.first.optString("name")} (${it.duration} ms).", "error", id, found.first.optString("name"), "MediaPlayer", slot, scheduledAtMs, "rejeitado")
                        nativePlayers.remove(key); it.release(); completion?.invoke(false, "SFX longo demais para o canal pontual.")
                    } else {
                        it.start()
                        if (loop) {
                            previousLoop?.let { old -> fadePlayer(old, nativeVolumes[channel] ?: safeVolume, 0f, 3000L, true) }
                            fadePlayer(it, 0f, safeVolume, 3000L)
                        }
                        emitAudioDiagnostic("native-audio-started", channel, "Áudio iniciado pelo MediaPlayer.", "success", id, found.first.optString("name"), "MediaPlayer", slot, scheduledAtMs, "reproduzindo")
                        completion?.invoke(true, null)
                    }
                }
                setOnCompletionListener { completed -> if (!loop) { emitAudioDiagnostic("native-audio-ended", channel, "Áudio nativo concluído.", "success", id, found.first.optString("name"), "MediaPlayer", slot, scheduledAtMs, "concluído"); nativePlayers.remove(key); completed.release(); abandonNativeAudioFocusIfIdle() } }
                setOnErrorListener { failedPlayer, what, extra ->
                    audioCatalog.markFailure(id)
                    Log.e("BRASEIRO_AUDIO", "MediaPlayer error $what/$extra")
                    emitAudioDiagnostic("native-media-error", channel, "MediaPlayer falhou ($what/$extra).", "error", id, found.first.optString("name"), "MediaPlayer", slot, scheduledAtMs, "falha")
                    nativePlayers.remove(key); failedPlayer.release(); abandonNativeAudioFocusIfIdle(); completion?.invoke(false, "MediaPlayer falhou ($what/$extra)."); true
                }
                prepareAsync()
            }
            nativePlayers[key] = player
            true
        } catch (error: Exception) {
            audioCatalog.markFailure(id)
            previousLoop?.let { nativePlayers[key] = it }
            Log.e("BRASEIRO_AUDIO", "play failed $id", error)
            emitAudioDiagnostic("native-play-exception", channel, error.message ?: "Falha ao reproduzir áudio nativo.", "error", id, found.first.optString("name"), "Android", slot, scheduledAtMs, "falha")
            abandonNativeAudioFocusIfIdle()
            completion?.invoke(false, error.message ?: "Falha ao reproduzir áudio nativo.")
            false
        }
    }

    private fun stopNativeAudio(channel: String = "", slot: String = "") {
        if (channel.isBlank() || channel == "sfx") {
            for (streamId in activeSoundPoolStreams.keys.toList()) {
                try { soundPool.stop(streamId) } catch (_: Exception) {}
                activeSoundPoolStreams.remove(streamId)
            }
        }
        val entries = nativePlayers.entries.toList()
        for ((key, player) in entries) {
            val channelMatch = channel.isBlank() || key.startsWith("$channel:")
            val slotMatch = slot.isBlank() || key == "$channel:$slot"
            if (channelMatch && slotMatch) {
                nativePlayers.remove(key)
                focusPausedPlayers.remove(key)
                try { player.stop() } catch (_: Exception) {}
                try { player.release() } catch (_: Exception) {}
                if (key == "voice:gemini") finishGeneratedVoice(activeGeminiVoiceRequestId, null)
            }
        }
        abandonNativeAudioFocusIfIdle()
    }

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("braseiro_gemini_keys", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("braseiro_gemini_keys", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private fun encryptSecret(value: String): String {
        if (value.isBlank()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val packed = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    private fun decryptSecret(value: String): String = try {
        if (value.isBlank()) "" else {
            val packed = Base64.decode(value, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, packed.copyOfRange(0, 12)))
            String(cipher.doFinal(packed.copyOfRange(12, packed.size)), Charsets.UTF_8)
        }
    } catch (error: Exception) { Log.e("BRASEIRO_KEYS", "decrypt failed", error); "" }

    private fun setGeminiSecret(tier: String, value: String) {
        val name = if (tier == "paid") "gemini_api_key_paid_encrypted" else "gemini_api_key_free_encrypted"
        privatePreferences.edit().putString(name, encryptSecret(value.trim())).apply()
    }

    private fun getGeminiSecret(tier: String): String {
        val name = if (tier == "paid") "gemini_api_key_paid_encrypted" else "gemini_api_key_free_encrypted"
        val encrypted = privatePreferences.getString(name, "") ?: ""
        if (encrypted.isNotBlank()) return decryptSecret(encrypted)
        // Migração única da versão anterior, que guardava uma chave em texto simples.
        if (tier != "paid") {
            val legacy = privatePreferences.getString("gemini_api_key", "") ?: ""
            if (legacy.isNotBlank()) {
                setGeminiSecret("free", legacy)
                privatePreferences.edit().remove("gemini_api_key").apply()
                return legacy
            }
        }
        return ""
    }

    private data class GeminiHttpResult(val status: Int, val body: String)

    private fun callGeminiOnce(model: String, body: String, key: String): GeminiHttpResult {
        require(Regex("^[A-Za-z0-9._-]{3,80}$").matches(model)) { "Modelo Gemini inválido." }
        val encodedKey = URLEncoder.encode(key, "UTF-8")
        val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$encodedKey").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 120_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            GeminiHttpResult(status, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "")
        } finally { connection.disconnect() }
    }

    private fun isTransientGeminiNetworkError(error: Throwable): Boolean {
        val text = buildString {
            append(error::class.java.simpleName)
            append(' ')
            append(error.message.orEmpty())
            append(' ')
            append(error.cause?.message.orEmpty())
        }.lowercase(Locale.ROOT)
        return listOf(
            "unknownhost", "unable to resolve host", "no address associated with hostname",
            "software caused connection abort", "connection abort", "connection reset",
            "sockettimeout", "timed out", "connectexception", "network is unreachable",
            "failed to connect", "temporary failure in name resolution"
        ).any(text::contains)
    }

    private fun callGemini(model: String, body: String, key: String): GeminiHttpResult {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                return callGeminiOnce(model, body, key)
            } catch (error: Throwable) {
                lastError = error
                if (!isTransientGeminiNetworkError(error) || attempt >= 2) {
                    if (isTransientGeminiNetworkError(error)) {
                        throw IllegalStateException("Conexão temporariamente indisponível para o Gemini. A cena atual foi preservada; verifique a internet e tente novamente.", error)
                    }
                    throw error
                }
                Thread.sleep(700L * (attempt + 1))
            }
        }
        throw IllegalStateException("Conexão temporariamente indisponível para o Gemini. A cena atual foi preservada; tente novamente.", lastError)
    }

    private fun performGeminiRequest(model: String, body: String, paidOnly: Boolean): JSONObject {
        val free = getGeminiSecret("free")
        val paid = getGeminiSecret("paid")
        if (!paidOnly && free.isNotBlank() && System.currentTimeMillis() >= freeGeminiCircuitOpenUntil) {
            repeat(2) { attempt ->
                val result = callGemini(model, body, free)
                if (result.status != 429) return JSONObject().put("status", result.status).put("body", result.body).put("tier", "free")
                if (attempt == 0) Thread.sleep(900)
            }
            freeGeminiCircuitOpenUntil = System.currentTimeMillis() + 5 * 60_000
        }
        if (paid.isNotBlank()) {
            val result = callGemini(model, body, paid)
            return JSONObject().put("status", result.status).put("body", result.body).put("tier", "paid")
        }
        val message = when {
            paidOnly -> "Nenhuma chave paga está configurada neste aparelho."
            free.isBlank() && paid.isBlank() -> "Nenhuma chave Gemini está configurada neste aparelho. Abra Configurações > Chaves Gemini."
            freeGeminiCircuitOpenUntil > System.currentTimeMillis() -> "A chave gratuita atingiu o limite temporário e não há chave paga configurada."
            else -> "Nenhuma chave Gemini utilizável está disponível."
        }
        return JSONObject().put("status", 401).put("body", message).put("tier", if (paidOnly) "paid" else "free")
    }


    private fun sha256Text(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun writePcm16MonoWav(target: File, pcm: ByteArray, sampleRate: Int = 24_000) {
        val dataSize = pcm.size
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        BufferedOutputStream(FileOutputStream(temporary)).use { out ->
            out.write(header)
            out.write(pcm)
            out.flush()
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    private fun storeGeminiAudioAsWav(target: File, audioBytes: ByteArray): Long {
        val alreadyWav = audioBytes.size >= 12 &&
            String(audioBytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(audioBytes, 8, 4, Charsets.US_ASCII) == "WAVE"
        if (alreadyWav) {
            val temporary = File(target.parentFile, "${target.name}.${UUID.randomUUID()}.tmp")
            BufferedOutputStream(FileOutputStream(temporary)).use { out -> out.write(audioBytes); out.flush() }
            if (!temporary.renameTo(target)) { temporary.copyTo(target, overwrite = true); temporary.delete() }
            return (((audioBytes.size - 44).coerceAtLeast(0) * 1000L) / 48_000L).coerceAtLeast(1L)
        }
        writePcm16MonoWav(target, audioBytes)
        return (audioBytes.size * 1000L / 48_000L).coerceAtLeast(1L)
    }

    private fun cleanupGeminiTtsCache(maxFiles: Int = 48) {
        geminiTtsDir.listFiles { file -> file.isFile && file.extension.equals("wav", true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(maxFiles)
            ?.forEach { it.delete() }
    }

    private fun performGeminiTtsRequest(model: String, text: String, voice: String, style: String, paidOnly: Boolean): JSONObject {
        require(text.isNotBlank()) { "A narração está vazia." }
        require(text.length <= 32_000) { "A narração é longa demais para uma única síntese." }
        val safeVoice = voice.takeIf { Regex("^[A-Za-z]{3,32}$").matches(it) } ?: "Charon"
        val stableStyle = style.trim().ifBlank { "natural" }.take(1200)
        val cacheId = sha256Text("tts-v9|$model|$safeVoice|pt-BR|$stableStyle|$text")
        val cached = File(geminiTtsDir, "$cacheId.wav")
        if (cached.exists() && cached.length() > 44L) {
            val durationMs = (((cached.length() - 44L) * 1000L) / 48_000L).coerceAtLeast(1L)
            cached.setLastModified(System.currentTimeMillis())
            return JSONObject().put("token", cacheId).put("durationMs", durationMs).put("model", model).put("tier", "cache")
        }
        val prompt = """Perfil de áudio fixo: $stableStyle
Idioma: português brasileiro.
Mantenha o mesmo locutor, timbre, idade aparente, sotaque, altura, ritmo e distância do microfone do início ao fim. Faça atuação contida e dicção natural. Preserve as pausas indicadas pela pontuação. Não leia estas instruções nem acrescente palavras. Leia exatamente o texto abaixo:

$text"""
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().apply {
                    put("languageCode", "pt-BR")
                    put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", safeVoice)))
                })
            })
        }.toString()
        val envelope = performGeminiRequest(model, body, paidOnly)
        val status = envelope.optInt("status", 500)
        val responseBody = envelope.optString("body")
        if (status !in 200..299) throw IllegalStateException("Gemini TTS HTTP $status: ${responseBody.take(700)}")
        val response = JSONObject(responseBody)
        val candidates = response.optJSONArray("candidates") ?: throw IllegalStateException("O Gemini TTS não devolveu candidatos.")
        var audioBase64 = ""
        var mimeType = ""
        outer@ for (candidateIndex in 0 until candidates.length()) {
            val parts = candidates.optJSONObject(candidateIndex)?.optJSONObject("content")?.optJSONArray("parts") ?: continue
            for (partIndex in 0 until parts.length()) {
                val inline = parts.optJSONObject(partIndex)?.optJSONObject("inlineData") ?: continue
                val data = inline.optString("data")
                if (data.isNotBlank()) { audioBase64 = data; mimeType = inline.optString("mimeType"); break@outer }
            }
        }
        if (audioBase64.isBlank()) throw IllegalStateException("O Gemini TTS respondeu sem áudio.")
        val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
        if (audioBytes.size < 4_800) throw IllegalStateException("O áudio devolvido pelo Gemini TTS está vazio ou incompleto.")
        val durationMs = storeGeminiAudioAsWav(cached, audioBytes)
        val spokenWords = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        // Very permissive lower bound (~375 wpm). This is only an integrity
        // guard against a tiny prefix being accepted as the complete request.
        val minimumCompleteDurationMs = if (spokenWords < 18) 900L else (spokenWords * 160L).coerceAtLeast(2_800L)
        if (durationMs < minimumCompleteDurationMs) {
            cached.delete()
            throw IllegalStateException("Gemini TTS returned truncated audio: duration=${durationMs}ms words=$spokenWords minimum=${minimumCompleteDurationMs}ms.")
        }
        cleanupGeminiTtsCache()
        Log.i("BRASEIRO_TTS", "Charon salvo localmente: ${audioBytes.size} bytes, ${durationMs}ms, $mimeType")
        return JSONObject().put("token", cacheId).put("durationMs", durationMs).put("model", model).put("tier", envelope.optString("tier", "free"))
    }

    private fun resolveGeminiTtsFile(token: String): File? {
        if (!Regex("^[a-f0-9]{64}$").matches(token)) return null
        return File(geminiTtsDir, "$token.wav").takeIf { it.exists() && it.isFile && it.length() > 44L }
    }

    private fun finishGeneratedVoice(requestId: String?, error: String? = null) {
        if (requestId.isNullOrBlank()) return
        if (activeGeminiVoiceRequestId == requestId) activeGeminiVoiceRequestId = null
        val id = JSONObject.quote(requestId)
        if (error == null) evaluate("window.__braseiroGeminiVoiceCallbacks?.[$id]?.resolve?.(); delete window.__braseiroGeminiVoiceCallbacks?.[$id];")
        else evaluate("window.__braseiroGeminiVoiceCallbacks?.[$id]?.reject?.(new Error(${JSONObject.quote(error)})); delete window.__braseiroGeminiVoiceCallbacks?.[$id];")
    }

    private fun playGeneratedVoiceFile(token: String, volume: Float, requestId: String) {
        val file = resolveGeminiTtsFile(token) ?: run { finishGeneratedVoice(requestId, "Arquivo da voz Charon não encontrado."); return }
        if (!requestNativeAudioFocus()) { finishGeneratedVoice(requestId, "O Android não concedeu foco de áudio para a voz Charon."); return }
        val key = "voice:gemini"
        val previousRequest = activeGeminiVoiceRequestId
        nativePlayers.remove(key)?.let { old -> try { old.stop() } catch (_: Exception) {}; try { old.release() } catch (_: Exception) {} }
        if (previousRequest != null && previousRequest != requestId) finishGeneratedVoice(previousRequest, null)
        activeGeminiVoiceRequestId = requestId
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                setDataSource(file.absolutePath)
                setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
                setOnPreparedListener {
                    val detail = JSONObject().put("token", token).put("durationMs", it.duration).put("requestId", requestId)
                    evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-gemini-voice-start',{detail:$detail}));")
                    emitAudioDiagnostic("native-charon-started", "voice", "Voz Charon iniciada pelo Android.", "success", token, "Charon", "MediaPlayer", "gemini", System.currentTimeMillis(), "reproduzindo")
                    it.start()
                }
                setOnCompletionListener { completed ->
                    nativePlayers.remove(key)
                    completed.release()
                    emitAudioDiagnostic("native-charon-ended", "voice", "Voz Charon concluída.", "success", token, "Charon", "MediaPlayer", "gemini", result = "concluído")
                    finishGeneratedVoice(requestId, null)
                    abandonNativeAudioFocusIfIdle()
                }
                setOnErrorListener { failed, what, extra ->
                    nativePlayers.remove(key)
                    failed.release()
                    finishGeneratedVoice(requestId, "Falha ao reproduzir Charon ($what/$extra).")
                    abandonNativeAudioFocusIfIdle()
                    true
                }
                prepareAsync()
            }
            nativePlayers[key] = player
        } catch (error: Exception) {
            nativePlayers.remove(key)
            finishGeneratedVoice(requestId, error.message ?: "Falha ao abrir o áudio de Charon.")
            abandonNativeAudioFocusIfIdle()
        }
    }

    private val kokoroBackendPackages = listOf(
        "com.k2fsa.sherpa.onnx.tts.engine.debug",
        "com.k2fsa.sherpa.onnx.tts.engine",
    )
    private val legacyKokoroPackage = "com.braseiro.kokoro.ptbr"

    private fun isKokoroBackendPackage(pkg:String):Boolean =
        pkg.trim() in kokoroBackendPackages

    private fun physicalTtsEngines():LinkedHashMap<String,String> {
        val found=linkedMapOf<String,String>()
        textToSpeech?.engines.orEmpty().forEach{info->
            val pkg=info.name.orEmpty().trim()
            if(pkg.isNotBlank()) found[pkg]=info.label.orEmpty().ifBlank{pkg}
        }
        runCatching{
            packageManager.queryIntentServices(
                android.content.Intent("android.intent.action.TTS_SERVICE"),0
            )
        }.getOrDefault(emptyList()).forEach{resolve->
            val service=resolve.serviceInfo?:return@forEach
            val pkg=service.packageName.orEmpty().trim()
            if(pkg.isBlank()) return@forEach
            val label=runCatching{resolve.loadLabel(packageManager)?.toString().orEmpty()}.getOrDefault("")
            found.putIfAbsent(pkg,label.ifBlank{pkg})
        }
        kokoroBackendPackages.forEach{pkg->
            runCatching{packageManager.getApplicationInfo(pkg,0)}.getOrNull()?.let{app->
                val label=runCatching{packageManager.getApplicationLabel(app).toString()}.getOrDefault("")
                found.putIfAbsent(pkg,label.ifBlank{pkg})
            }
        }
        return found
    }

    private fun preferredInstalledKokoroBackend(hint:String?=null):String? {
        val installed=physicalTtsEngines().keys.filter(::isKokoroBackendPackage)
        val h=hint.orEmpty().trim()
        if(h in installed) return h
        val active=activeTtsEnginePackage.trim()
        if(active in installed) return active
        return kokoroBackendPackages.firstOrNull{it in installed}
    }

    private fun migratePersistedEnginePackage(raw:String):String {
        val p=raw.trim()
        if(p.isBlank()) return ""
        if(p==legacyKokoroPackage) return preferredInstalledKokoroBackend().orEmpty()
        if(isKokoroBackendPackage(p)) return preferredInstalledKokoroBackend(p).orEmpty()
        return p
    }

    private fun discoverTtsEngines(): List<Pair<String,String>> {
        val physical=physicalTtsEngines()
        val found=linkedMapOf<String,String>()

        physical.forEach{(pkg,label)->
            if(pkg==legacyKokoroPackage || isKokoroBackendPackage(pkg)) return@forEach
            found[pkg]=label
        }

        preferredInstalledKokoroBackend()?.let{pkg->
            found[pkg]="Braseiro Kokoro PT-BR · neural"
        }

        return found.entries
            .map{it.key to it.value}
            .sortedBy{it.second.lowercase(Locale.ROOT)}
    }

    private fun isInstalledTtsEnginePackage(pkg:String):Boolean {
        val requested=migratePersistedEnginePackage(pkg)
        if(requested.isBlank()) return pkg.isBlank()
        return physicalTtsEngines().containsKey(requested)
    }

    override fun onInit(status: Int) {
        val savedRaw=ttsPreferences.getString("enginePackage","").orEmpty().trim()
        val saved=migratePersistedEnginePackage(savedRaw)
        if(saved!=savedRaw) {
            if(saved.isBlank()) ttsPreferences.edit().remove("enginePackage").apply()
            else ttsPreferences.edit().putString("enginePackage",saved).apply()
        }
        if (!startupTtsSelectionAttempted && saved.isNotBlank()) {
            startupTtsSelectionAttempted=true
            if (isInstalledTtsEnginePackage(saved)) { switchTtsEngine(saved,null); return }
            ttsPreferences.edit().remove("enginePackage").apply()
        }
        startupTtsSelectionAttempted=true
        configureInitializedTts(status,null)
    }

    private fun isNeuralEngineIdentity(pkg:String,label:String="",voice:String=""):Boolean {
        val x="$pkg $label $voice".lowercase(Locale.ROOT)
        return isKokoroBackendPackage(pkg)
    }
    private fun isNeuralTtsProfile()=isNeuralEngineIdentity(activeTtsEnginePackage,activeTtsEngineLabel,selectedTtsVoiceName)

    private fun installTtsListener(engine:TextToSpeech) {
        engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(id:String){ val p=speechParts[id]?:return; if(p.index==0)evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-tts-start'));"); fireSpeechCues(p,p.offset) }
            override fun onRangeStart(id:String,start:Int,end:Int,frame:Int){ val p=speechParts[id]?:return; evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-tts-boundary',{detail:${p.offset+start}}));"); fireSpeechCues(p,p.offset+end) }
            override fun onDone(id:String){ val p=speechParts.remove(id)?:return; cancelSpeechWatchdog(); fireSpeechCues(p,p.offset+p.text.length); runOnUiThread{advanceSpeechQueue(p.rootId)} }
            @Deprecated("Deprecated in Java") override fun onError(id:String){ val p=speechParts.remove(id)?:return; cancelSpeechWatchdog(); if(isNeuralTtsProfile())runOnUiThread{finishSpeech(p.rootId,"Falha TTS neural no bloco ${p.index+1}.")} else runOnUiThread{advanceSpeechQueue(p.rootId)} }
            override fun onError(id:String,code:Int){ val p=speechParts.remove(id)?:return; cancelSpeechWatchdog(); if(isNeuralTtsProfile())runOnUiThread{finishSpeech(p.rootId,"Falha TTS neural no bloco ${p.index+1} ($code).")} else runOnUiThread{advanceSpeechQueue(p.rootId)} }
        })
    }

    private fun configureInitializedTts(status:Int,callbackId:String?) {
        if(status!=TextToSpeech.SUCCESS){ttsReady=false;deliverTtsConfigCallback(callbackId,"O motor TTS selecionado não iniciou.");return}
        val e=textToSpeech?:run{ttsReady=false;deliverTtsConfigCallback(callbackId,"O Android não criou o motor TTS.");return}
        activeTtsEngineLabel=if(isKokoroBackendPackage(activeTtsEnginePackage))"Braseiro Kokoro PT-BR · neural" else discoverTtsEngines().firstOrNull{it.first==activeTtsEnginePackage}?.second.orEmpty()
        val neural=isNeuralTtsProfile()
        val lang=if(neural) listOf(Locale("pt","BR"),Locale("pt","PT"),Locale.getDefault()).distinct().asSequence().map{e.setLanguage(it)}.firstOrNull{it!=TextToSpeech.LANG_MISSING_DATA&&it!=TextToSpeech.LANG_NOT_SUPPORTED}?:TextToSpeech.LANG_NOT_SUPPORTED else e.setLanguage(Locale("pt","BR"))
        ttsReady=lang!=TextToSpeech.LANG_MISSING_DATA&&lang!=TextToSpeech.LANG_NOT_SUPPORTED
        e.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        neuralTtsRate=ttsPreferences.getFloat("neuralRate",1f).coerceIn(.65f,1.5f)
        val key=activeTtsEnginePackage.ifBlank{"__android_default__"}
        val savedVoice=ttsPreferences.getString("voice.$key","").orEmpty()
        val persistedVoice=e.voices.orEmpty().firstOrNull{it.name==savedVoice}
        selectedTtsVoiceName=""
        if(persistedVoice!=null && e.setVoice(persistedVoice)!=TextToSpeech.ERROR){
            selectedTtsVoiceName=persistedVoice.name
        }else if(savedVoice.isNotBlank()){
            ttsPreferences.edit().remove("voice.$key").apply()
        }
        installTtsListener(e)
        deliverTtsConfigCallback(callbackId,if(ttsReady)null else "O motor não oferece voz compatível com pt-BR.")
        if(ttsReady&&neural)prewarmNeuralTts()
    }

    private fun switchTtsEngine(pkg:String,callbackId:String?) {
        val requested=when{
            pkg.trim()==legacyKokoroPackage -> preferredInstalledKokoroBackend().orEmpty()
            isKokoroBackendPackage(pkg) -> preferredInstalledKokoroBackend(pkg).orEmpty()
            else -> pkg.trim()
        }
        if(pkg.trim().isNotBlank() && requested.isBlank()){
            deliverTtsConfigCallback(callbackId,"Braseiro Kokoro PT-BR não está instalado.")
            return
        }
        if(requested.isNotBlank()&&!physicalTtsEngines().containsKey(requested)){
            deliverTtsConfigCallback(callbackId,"Motor TTS não instalado: $requested")
            return
        }

        if(requested==activeTtsEnginePackage && textToSpeech!=null){
            ttsPreferences.edit().putString("enginePackage",requested).apply()
            deliverTtsConfigCallback(callbackId,null)
            return
        }

        if(currentSpeechRootId!=null)stopNativeSpeech(true)
        val old=textToSpeech
        runCatching{old?.shutdown()}
        ttsReady=false
        activeTtsEnginePackage=requested
        activeTtsEngineLabel=if(isKokoroBackendPackage(requested))"Braseiro Kokoro PT-BR · neural" else ""
        if(requested.isBlank()) ttsPreferences.edit().remove("enginePackage").apply()
        else ttsPreferences.edit().putString("enginePackage",requested).apply()

        val listener=TextToSpeech.OnInitListener{s->
            runOnUiThread{configureInitializedTts(s,callbackId)}
        }
        textToSpeech=if(requested.isBlank()) TextToSpeech(this,listener)
        else TextToSpeech(this,listener,requested)
    }

    private fun ttsCatalogJson():String {
        val e=textToSpeech
        val engines=JSONArray()
        engines.put(JSONObject()
            .put("packageName","")
            .put("label","Padrão do Android")
            .put("selected",activeTtsEnginePackage.isBlank())
            .put("neuralProfile",false))

        discoverTtsEngines().forEach{(pkg,label)->
            engines.put(JSONObject()
                .put("packageName",pkg)
                .put("label",label)
                .put("selected",pkg==activeTtsEnginePackage)
                .put("neuralProfile",isKokoroBackendPackage(pkg)))
        }

        val voices=JSONArray()
        runCatching{e?.voices.orEmpty()}.getOrDefault(emptySet())
            .filter{it.name.isNotBlank() && !it.name.equals("NOT_SET_VOICE",true)}
            .sortedWith(compareBy({it.locale?.toLanguageTag().orEmpty()},{it.name}))
            .forEach{v->
                val locale=v.locale?.toLanguageTag().orEmpty()
                val label=buildString{
                    append(v.name)
                    if(locale.isNotBlank()) append(" · ").append(locale)
                    append(if(v.isNetworkConnectionRequired)" · rede" else " · local")
                }
                voices.put(JSONObject()
                    .put("name",v.name)
                    .put("label",label)
                    .put("locale",locale)
                    .put("quality",v.quality)
                    .put("latency",v.latency)
                    .put("networkRequired",v.isNetworkConnectionRequired)
                    .put("selected",v.name==selectedTtsVoiceName))
            }

        return JSONObject()
            .put("ready",ttsReady)
            .put("selectedEnginePackage",activeTtsEnginePackage)
            .put("selectedEngineLabel",activeTtsEngineLabel.ifBlank{
                if(activeTtsEnginePackage.isBlank())"Padrão do Android" else activeTtsEnginePackage
            })
            .put("selectedVoice",selectedTtsVoiceName)
            .put("neuralProfile",isNeuralTtsProfile())
            .put("neuralRate",neuralTtsRate)
            .put("engines",engines)
            .put("voices",voices)
            .put("profile",JSONObject()
                .put("name",if(isNeuralTtsProfile())"Braseiro Kokoro PT-BR · neural" else "Android TTS")
                .put("minChars",neuralMinChars)
                .put("targetChars",neuralTargetChars)
                .put("maxChars",neuralMaxChars))
            .toString()
    }

    private fun deliverTtsConfigCallback(id:String?,error:String?){
        if(id.isNullOrBlank())return
        val q=JSONObject.quote(id)
        if(error==null){
            val payload=ttsCatalogJson()
            evaluate("window.__braseiroTtsConfigCallbacks?.[$q]?.resolve?.($payload); delete window.__braseiroTtsConfigCallbacks?.[$q];")
        }else{
            evaluate("window.__braseiroTtsConfigCallbacks?.[$q]?.reject?.(new Error(${JSONObject.quote(error)})); delete window.__braseiroTtsConfigCallbacks?.[$q];")
        }
    }

    private fun selectTtsVoiceInternal(name:String):String {
        val e=textToSpeech ?: throw IllegalStateException("O motor TTS ainda não foi inicializado.")
        val requested=name.trim()
        val key=activeTtsEnginePackage.ifBlank{"__android_default__"}

        if(requested.isBlank()){
            selectedTtsVoiceName=""
            ttsPreferences.edit().remove("voice.$key").apply()
            return ttsCatalogJson()
        }

        val voice=e.voices.orEmpty().firstOrNull{it.name==requested}
            ?: throw IllegalArgumentException("Voz TTS real não disponível no motor selecionado: $requested")
        if(e.setVoice(voice)==TextToSpeech.ERROR){
            throw IllegalStateException("O motor recusou a voz Android ${voice.name}.")
        }

        selectedTtsVoiceName=voice.name
        ttsPreferences.edit().putString("voice.$key",voice.name).apply()
        return ttsCatalogJson()
    }

    private fun setNeuralTtsRateInternal(rate:Float):String {
        neuralTtsRate=rate.coerceIn(.65f,1.5f)
        ttsPreferences.edit().putFloat("neuralRate",neuralTtsRate).apply()
        return ttsCatalogJson()
    }

    private fun neuralParams(volume:Float)=Bundle().apply{
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volume.coerceIn(0f,1f))
    }

    private fun prewarmNeuralTts(force:Boolean=false):Boolean =
        ttsReady && isNeuralTtsProfile()


    private fun parseSpeechCues(cuesJson: String, textLength: Int): List<SpeechCue> {
        return try {
            val array = JSONArray(cuesJson.ifBlank { "[]" })
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").trim()
                    if (id.isBlank() || !item.has("charIndex")) continue
                    add(SpeechCue(id, item.optInt("charIndex").coerceIn(0, textLength)))
                }
            }.distinctBy { "${it.id}:${it.charIndex}" }.sortedBy { it.charIndex }
        } catch (error: Exception) {
            Log.w("BRASEIRO_TTS", "Lista de marcações inválida", error)
            emptyList()
        }
    }

    private fun chooseNeuralBoundary(text:String,start:Int,maxChars:Int):Int{
        val high=(start+maxChars).coerceAtMost(text.length);if(high>=text.length)return text.length
        val low=(start+neuralMinChars.coerceAtMost(maxChars)).coerceAtMost(high);val target=(start+neuralTargetChars.coerceAtMost(maxChars)).coerceAtMost(high)
        fun closest(chars:Set<Char>,sentence:Boolean):Int?{var best:Int?=null;var dist=Int.MAX_VALUE;for(i in low until high){if(text[i] !in chars)continue;if(sentence&&i+1<text.length&&!text[i+1].isWhitespace())continue;val b=i+1;val d=abs(b-target);if(d<dist){best=b;dist=d}};return best}
        closest(setOf('.','!','?'),true)?.let{return it};closest(setOf(';',','),false)?.let{return it}
        var best:Int?=null;var dist=Int.MAX_VALUE;for(i in low until high)if(text[i].isWhitespace()){val d=abs(i-target);if(d<dist){best=i;dist=d}};return best?:high
    }
    private fun buildNeuralSpeechSegments(text:String,cues:List<SpeechCue>,engineMax:Int):List<SpeechSegment>{
        if(text.isBlank())return emptyList();val maxChars=neuralMaxChars.coerceAtMost(engineMax).coerceAtLeast(120);val out=mutableListOf<SpeechSegment>();var cursor=0
        while(cursor<text.length){while(cursor<text.length&&text[cursor].isWhitespace())cursor++;if(cursor>=text.length)break;val boundary=chooseNeuralBoundary(text,cursor,maxChars);var end=boundary.coerceAtMost(text.length);while(end>cursor&&text[end-1].isWhitespace())end--;if(end<=cursor)end=(cursor+maxChars).coerceAtMost(text.length);val part=text.substring(cursor,end);out+=SpeechSegment(part,cursor,cues.filter{it.charIndex in cursor..end});cursor=boundary.coerceAtLeast(end)}
        return out
    }
    private fun buildSpeechSegments(text:String,cues:List<SpeechCue>):List<SpeechSegment>{
        if(text.isBlank())return emptyList()
        val engineMax=TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(120)
        val androidMax=150.coerceAtMost(engineMax)
        // Duas camadas idênticas ao fluxo estável do Forbidden Lands. O VTT só
        // entrega texto ao android.speech.tts.TextToSpeech; nunca acessa modelo.
        return NarrationSegmenter.splitIntoChapters(text,680).flatMap{chapter->
            NarrationSegmenter.splitForAndroidTts(chapter,androidMax).map{s->
                val end=s.offset+s.text.length
                SpeechSegment(s.text,s.offset,cues.filter{it.charIndex in s.offset..end})
            }
        }
    }
    private fun fireSpeechCues(part:SpeechPart,absolutePosition:Int){part.cues.filter{it.charIndex<=absolutePosition}.forEach{cue->val key="${part.rootId}:${cue.id}:${cue.charIndex}";if(!firedSpeechCues.add(key))return@forEach;val detail=JSONObject().put("id",cue.id).put("charIndex",cue.charIndex).toString();evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-tts-cue',{detail:$detail}));")}}
    private fun cancelSpeechWatchdog(){speechWatchdog?.let(speechHandler::removeCallbacks);speechWatchdog=null}
    private fun scheduleSpeechWatchdog(part:SpeechPart){if(isNeuralTtsProfile())return;cancelSpeechWatchdog();val estimated=((part.text.length/(13.0*speechRate.coerceAtLeast(.65f)))*1000).toLong().coerceAtLeast(1200)+2500;speechWatchdog=Runnable{val active=currentSpeechRootId==part.rootId&&speechQueueIndex==part.index;if(!active)return@Runnable;speechParts.entries.filter{it.value.rootId==part.rootId&&it.value.index==part.index}.forEach{speechParts.remove(it.key)};textToSpeech?.stop();advanceSpeechQueue(part.rootId)}.also{speechHandler.postDelayed(it,estimated)}}
    private fun startCurrentSpeechPart(rootId:String){if(currentSpeechRootId!=rootId)return;if(speechQueueIndex>=speechQueue.size){finishSpeech(rootId,null);return};val s=speechQueue[speechQueueIndex];val id="$rootId::$speechQueueIndex";val p=SpeechPart(rootId,speechQueueIndex,speechQueue.size,s.offset,s.text,s.cues);speechParts[id]=p;val neural=isNeuralTtsProfile();val params=if(neural)neuralParams(nativeVolumes["voice"]?:1f)else Bundle().apply{putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,nativeVolumes["voice"]?:1f)};val e=textToSpeech;val mode=if(p.index>0)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH;val result=e?.speak(s.text,mode,params,id)?:TextToSpeech.ERROR;if(result==TextToSpeech.ERROR){speechParts.remove(id);if(neural)finishSpeech(rootId,"O motor TTS neural recusou o bloco ${p.index+1}.")else advanceSpeechQueue(rootId)}else if(!neural)scheduleSpeechWatchdog(p)}
    private fun advanceSpeechQueue(rootId:String){if(currentSpeechRootId!=rootId)return;speechQueueIndex++;if(speechQueueIndex>=speechQueue.size)finishSpeech(rootId,null)else startCurrentSpeechPart(rootId)}
    private fun speakNative(text:String,rate:Float,rootId:String,cues:List<SpeechCue> = emptyList()){if(!ttsReady||textToSpeech==null){finishSpeech(rootId,"A voz do Android ainda não está pronta.");return};if(!requestNativeAudioFocus()){finishSpeech(rootId,"O Android não concedeu foco de áudio para a narração.");return};if(currentSpeechRootId!=null&&currentSpeechRootId!=rootId)stopNativeSpeech(true);currentSpeechRootId=rootId;val neural=isNeuralTtsProfile();speechRate=if(neural)neuralTtsRate else rate.coerceIn(.65f,1.5f);textToSpeech!!.setSpeechRate(speechRate);firedSpeechCues.clear();speechQueue=buildSpeechSegments(text,cues);speechQueueIndex=0;if(speechQueue.isEmpty()){finishSpeech(rootId,null);return};startCurrentSpeechPart(rootId)}

    private fun stopNativeSpeech(resolveCanceled: Boolean) {
        val root = currentSpeechRootId
        currentSpeechRootId = null
        cancelSpeechWatchdog()
        textToSpeech?.stop()
        speechQueue = emptyList()
        speechQueueIndex = 0
        firedSpeechCues.clear()
        if (root != null) {
            speechParts.entries.filter { it.value.rootId == root }.forEach { speechParts.remove(it.key) }
            if (resolveCanceled) finishSpeech(root, null)
        }
    }

    private fun finishSpeech(rootId: String, error: String?) {
        if (currentSpeechRootId == rootId) currentSpeechRootId = null
        cancelSpeechWatchdog()
        speechParts.entries.filter { it.value.rootId == rootId }.forEach { speechParts.remove(it.key) }
        speechQueue = emptyList()
        speechQueueIndex = 0
        val id = JSONObject.quote(rootId)
        if (error == null) evaluate("window.__braseiroTtsCallbacks[$id]?.resolve?.(); delete window.__braseiroTtsCallbacks[$id];")
        else evaluate("window.__braseiroTtsCallbacks[$id]?.reject?.(new Error(${JSONObject.quote(error)})); delete window.__braseiroTtsCallbacks[$id];")
        abandonNativeAudioFocusIfIdle()
    }


    private fun emitVoiceEvent(name: String, payload: JSONObject) {
        evaluate("window.dispatchEvent(new CustomEvent(${JSONObject.quote(name)},{detail:${payload}}));")
    }

    private fun emitVoiceError(message: String) {
        emitVoiceEvent("braseiro-voice-error", JSONObject().put("message", message))
    }

    private fun startVoiceInputFromBridge() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingVoicePermissionStart = true
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        // Evita que o microfone transcreva a voz do próprio Mestre.
        stopNativeSpeech(true)
        stopNativeAudio("voice", "")

        voiceInputController.start("pt-BR", true, object : VoiceInputController.Listener {
            override fun onState(state: String, mode: String) {
                emitVoiceEvent("braseiro-voice-state", JSONObject().put("state", state).put("mode", mode))
            }

            override fun onPartial(text: String) {
                emitVoiceEvent("braseiro-voice-partial", JSONObject().put("text", text))
            }

            override fun onFinal(text: String) {
                emitVoiceEvent("braseiro-voice-final", JSONObject().put("text", text))
            }

            override fun onError(message: String) {
                emitVoiceError(message)
            }
        })
    }

    private fun stopVoiceInputFromBridge() {
        voiceInputController.stop()
        emitVoiceEvent("braseiro-voice-state", JSONObject().put("state", "processing"))
    }

    private fun cancelVoiceInputFromBridge() {
        voiceInputController.cancel()
        emitVoiceEvent("braseiro-voice-state", JSONObject().put("state", "idle"))
    }

    private fun stableDiceActorId(actorId: String): Long {
        var hash = 1125899906842597L
        actorId.forEach { hash = 31L * hash + it.code.toLong() }
        return hash and Long.MAX_VALUE
    }

    private fun resolveDice(requestId: String, result: JSONObject) {
        val id = JSONObject.quote(requestId)
        evaluate("const c=window.__braseiroDiceCallbacks; if(c&&c[$id]){c[$id].resolve(${result}); delete c[$id];}")
    }

    private fun rejectDice(requestId: String, message: String) {
        val id = JSONObject.quote(requestId)
        val msg = JSONObject.quote(message)
        evaluate("const c=window.__braseiroDiceCallbacks; if(c&&c[$id]){c[$id].reject(new Error($msg)); delete c[$id];}")
    }

    private inner class NativeBridge {

        @JavascriptInterface fun isVoiceInputAvailable(): Boolean = voiceInputController.isAvailable()
        @JavascriptInterface fun startVoiceInput() = runOnUiThread { startVoiceInputFromBridge() }
        @JavascriptInterface fun stopVoiceInput() = runOnUiThread { stopVoiceInputFromBridge() }
        @JavascriptInterface fun cancelVoiceInput() = runOnUiThread { cancelVoiceInputFromBridge() }
        @JavascriptInterface fun requestDriveToken() = runOnUiThread { this@MainActivity.requestDriveToken() }
        @JavascriptInterface fun logoutDrive(requestId: String) = this@MainActivity.logoutDrive(requestId)
        @JavascriptInterface fun hasGeminiApiKey(tier: String): Boolean = getGeminiSecret(tier).isNotBlank()
        @JavascriptInterface fun setGeminiApiKey(tier: String, value: String) = setGeminiSecret(tier, value)
        @JavascriptInterface fun geminiRequest(model: String, body: String, paidOnly: Boolean, requestId: String) {
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { performGeminiRequest(model, body, paidOnly) }
                    val id = JSONObject.quote(requestId)
                    val payload = JSONObject.quote(result.toString())
                    evaluate("window.__braseiroGeminiCallbacks?.[$id]?.resolve?.($payload); delete window.__braseiroGeminiCallbacks?.[$id];")
                } catch (error: Exception) {
                    Log.e("BRASEIRO_GEMINI", "request failed", error)
                    val id = JSONObject.quote(requestId)
                    val message = JSONObject.quote(error.message ?: "Falha na requisição Gemini.")
                    evaluate("window.__braseiroGeminiCallbacks?.[$id]?.reject?.(new Error($message)); delete window.__braseiroGeminiCallbacks?.[$id];")
                }
            }
        }
        @JavascriptInterface fun generateTts(model: String, text: String, voice: String, style: String, paidOnly: Boolean, requestId: String) {
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { performGeminiTtsRequest(model, text, voice, style, paidOnly) }
                    val id = JSONObject.quote(requestId)
                    evaluate("window.__braseiroGeminiTtsCallbacks?.[$id]?.resolve?.(${result}); delete window.__braseiroGeminiTtsCallbacks?.[$id];")
                } catch (error: Exception) {
                    Log.e("BRASEIRO_TTS", "Gemini TTS failed", error)
                    val id = JSONObject.quote(requestId)
                    evaluate("window.__braseiroGeminiTtsCallbacks?.[$id]?.reject?.(new Error(${JSONObject.quote(error.message ?: "Falha na voz Charon.")})); delete window.__braseiroGeminiTtsCallbacks?.[$id];")
                }
            }
        }
        @JavascriptInterface fun playGeneratedVoice(token: String, volume: Double, requestId: String) = runOnUiThread {
            playGeneratedVoiceFile(token, volume.toFloat(), requestId)
        }
        @JavascriptInterface fun isTtsReady(): Boolean = ttsReady
        @JavascriptInterface fun getTtsCatalog(): String = ttsCatalogJson()
        @JavascriptInterface fun selectTtsEngine(packageName: String, requestId: String) = runOnUiThread { switchTtsEngine(packageName, requestId) }
        @JavascriptInterface fun selectTtsVoice(voiceName: String, requestId: String) = runOnUiThread {
            try { selectTtsVoiceInternal(voiceName); deliverTtsConfigCallback(requestId,null) }
            catch (e:Throwable) { deliverTtsConfigCallback(requestId,e.message ?: "Falha ao selecionar voz TTS.") }
        }
        @JavascriptInterface fun setNeuralTtsRate(rate: Double, requestId: String) = runOnUiThread {
            try { setNeuralTtsRateInternal(rate.toFloat()); deliverTtsConfigCallback(requestId,null) }
            catch (e:Throwable) { deliverTtsConfigCallback(requestId,e.message ?: "Falha ao ajustar velocidade TTS.") }
        }
        @JavascriptInterface fun prewarmTts(): Boolean { runOnUiThread { prewarmNeuralTts() }; return isNeuralTtsProfile() }
        @JavascriptInterface fun speakText(text: String, rate: Double, utteranceId: String) = runOnUiThread { speakNative(text, rate.toFloat(), utteranceId.ifBlank { UUID.randomUUID().toString() }) }
        @JavascriptInterface fun speakTextWithCues(text: String, rate: Double, cuesJson: String, utteranceId: String) = runOnUiThread {
            speakNative(text, rate.toFloat(), utteranceId.ifBlank { UUID.randomUUID().toString() }, parseSpeechCues(cuesJson, text.length))
        }
        @JavascriptInterface fun stopSpeech() = runOnUiThread { stopNativeSpeech(true) }
        @JavascriptInterface fun importAudioFiles() = runOnUiThread {
            if (audioImportPending || audioFolderImportPending || isBackgroundAudioImportRunning()) { evaluate("window.__braseiroAudioReject?.(new Error('Uma importação já está em andamento.'));"); return@runOnUiThread }
            audioImportPending = true
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "application/json", "application/zip", "application/x-zip-compressed"))
            }
            audioPickerLauncher.launch(Intent.createChooser(intent, "Importar biblioteca de sons"))
        }
        @JavascriptInterface fun importAudioFolder() = runOnUiThread {
            if (audioImportPending || audioFolderImportPending || isBackgroundAudioImportRunning()) { evaluate("window.__braseiroAudioReject?.(new Error('Uma importação já está em andamento.'));"); return@runOnUiThread }
            audioFolderImportPending = true
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }
            audioFolderPickerLauncher.launch(Intent.createChooser(intent, "Selecionar pasta da biblioteca sonora"))
        }
        @JavascriptInterface fun listNativeSounds(): String {
            if (audioCatalog.count() == 0 && readAudioIndex().length() > 0) rebuildAudioCatalog()
            return JSONArray().apply { audioCatalog.listAll().forEach { put(it.toLegacyJson()) } }.toString()
        }
        @JavascriptInterface fun getNativeSoundKeywordLibrary(): String = readSoundKeywordLibrary()
        @JavascriptInterface fun selectCatalogAudio(requestJson: String): String = try {
            val selected = audioRotationSelector.select(AudioSelectionRequest.fromJson(JSONObject(requestJson)))
            selected?.toLegacyJson()?.toString() ?: "null"
        } catch (error: Exception) {
            Log.e("BRASEIRO_AUDIO", "catalog selection failed", error)
            "null"
        }
        @JavascriptInterface fun getAudioCatalogStatus(): String = JSONObject().apply {
            put("database", "audio_catalog.db")
            put("catalogCount", audioCatalog.count())
            put("overrideFile", audioOverridesFile.exists())
            put("keywordLibrary", soundKeywordLibraryFile.exists())
            val channels = JSONObject()
            audioCatalog.listAll().groupingBy { it.channel }.eachCount().forEach { (key, value) -> channels.put(key, value) }
            put("channels", channels)
        }.toString()
        @JavascriptInterface fun reindexAudioCatalog(): String = JSONObject().apply {
            if (isBackgroundAudioImportRunning()) throw IllegalStateException("Aguarde a importação em segundo plano terminar antes de reindexar.")
            val count = rebuildAudioCatalog()
            audioCatalog.reactivateAll()
            put("catalogCount", count)
            put("database", "audio_catalog.db")
        }.toString()
        @JavascriptInterface fun markAudioFailure(id: String) { if (id.isNotBlank()) audioCatalog.markFailure(id) }
        @JavascriptInterface fun markAudioAvailable(id: String) { if (id.isNotBlank()) audioCatalog.markAvailable(id) }
        @JavascriptInterface fun beginAudioSession(id: String, sceneId: String, backend: String): Boolean = try {
            if (id.isBlank()) false else { audioCatalog.startSession(id, sceneId.ifBlank { null }, backend.ifBlank { "hybrid" }); true }
        } catch (error: Exception) { Log.e("BRASEIRO_AUDIO", "Falha ao registrar sessão de áudio", error); false }
        @JavascriptInterface fun finishAudioSession(id: String, status: String): Boolean = try {
            if (id.isBlank()) false else { audioCatalog.finishSession(id, status.ifBlank { "finished" }); true }
        } catch (error: Exception) { Log.e("BRASEIRO_AUDIO", "Falha ao finalizar sessão de áudio", error); false }
        @JavascriptInterface fun saveRecoveryBackup(campaignId: String, json: String): Boolean = try {
            val safeId = campaignId.replace(Regex("[^A-Za-z0-9._-]"), "_").take(96)
            require(safeId.isNotBlank() && json.isNotBlank()) { "Campanha ou conteúdo de recuperação vazio." }
            val parsed = JSONObject(json)
            val storedId = parsed.optJSONObject("campaign")?.optString("id").orEmpty()
            require(storedId == campaignId) { "O backup nativo não pertence à campanha informada." }
            val directory = File(filesDir, "campaign_recovery").apply { mkdirs() }
            val target = File(directory, "${safeId}_${System.currentTimeMillis()}.json")
            val temporary = File(directory, "${target.name}.tmp")
            FileOutputStream(temporary).use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
                stream.fd.sync()
            }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            directory.listFiles { file -> file.isFile && file.name.startsWith("${safeId}_") && file.extension == "json" }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(12)
                ?.forEach { it.delete() }
            true
        } catch (error: Exception) {
            Log.e("BRASEIRO_RECOVERY", "Falha ao gravar backup nativo", error)
            false
        }
        @JavascriptInterface fun getLatestRecoveryBackup(campaignId: String): String = try {
            val safeId = campaignId.replace(Regex("[^A-Za-z0-9._-]"), "_").take(96)
            if (safeId.isBlank()) "" else {
                val directory = File(filesDir, "campaign_recovery")
                directory.listFiles { file -> file.isFile && file.name.startsWith("${safeId}_") && file.extension == "json" }
                    ?.maxByOrNull { it.lastModified() }
                    ?.readText(Charsets.UTF_8)
                    .orEmpty()
            }
        } catch (error: Exception) {
            Log.e("BRASEIRO_RECOVERY", "Falha ao ler backup nativo", error)
            ""
        }
        @JavascriptInterface fun preloadNativeSounds(idsJson: String, requestId: String) {
            lifecycleScope.launch {
                try {
                    val array = JSONArray(idsJson)
                    val ids = (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                    val samples = preloadNativeSoundIds(ids)
                    val deadline = System.currentTimeMillis() + 2500L
                    while (samples.any { !loadedSoundPoolSamples.contains(it) } && System.currentTimeMillis() < deadline) delay(25)
                    val loaded = samples.count { loadedSoundPoolSamples.contains(it) }
                    emitAudioDiagnostic("native-preload-complete", "sfx", "SoundPool preparou $loaded/${samples.size} amostras.", if (loaded == samples.size) "success" else "warning", backend = "SoundPool", result = if (loaded == samples.size) "concluído" else "parcial")
                    val id = JSONObject.quote(requestId)
                    evaluate("window.__braseiroAudioPreloadCallbacks?.[$id]?.resolve?.(); delete window.__braseiroAudioPreloadCallbacks?.[$id];")
                } catch (error: Exception) {
                    emitAudioDiagnostic("native-preload-error", "sfx", error.message ?: "Falha ao preparar efeitos sonoros.", "error", backend = "SoundPool", result = "falha")
                    val id = JSONObject.quote(requestId)
                    val message = JSONObject.quote(error.message ?: "Falha ao preparar efeitos sonoros.")
                    evaluate("window.__braseiroAudioPreloadCallbacks?.[$id]?.reject?.(new Error($message)); delete window.__braseiroAudioPreloadCallbacks?.[$id];")
                }
            }
        }
        @JavascriptInterface fun playNativeSound(id: String, channel: String, loop: Boolean, volume: Double, slot: String, scheduledAtMs: Double): Boolean {
            if (findAudio(id) == null) return false
            runOnUiThread { this@MainActivity.playNativeSound(id, channel, loop, volume.toFloat(), slot, scheduledAtMs.toLong()) }
            return true
        }
        @JavascriptInterface fun playNativeSoundAsync(id: String, channel: String, loop: Boolean, volume: Double, slot: String, scheduledAtMs: Double, requestId: String) {
            runOnUiThread {
                val quotedId = JSONObject.quote(requestId)
                this@MainActivity.playNativeSound(id, channel, loop, volume.toFloat(), slot, scheduledAtMs.toLong()) { started, error ->
                    val script = if (started) {
                        "window.__braseiroNativeSoundCallbacks?.[$quotedId]?.resolve?.(true); delete window.__braseiroNativeSoundCallbacks?.[$quotedId];"
                    } else {
                        val message = JSONObject.quote(error ?: "Falha ao iniciar o áudio nativo.")
                        "window.__braseiroNativeSoundCallbacks?.[$quotedId]?.reject?.(new Error($message)); delete window.__braseiroNativeSoundCallbacks?.[$quotedId];"
                    }
                    evaluate(script)
                }
            }
        }
        @JavascriptInterface fun stopNativeAudio(channel: String, slot: String) = runOnUiThread { this@MainActivity.stopNativeAudio(channel, slot) }
        @JavascriptInterface fun setNativeAudioVolume(channel: String, volume: Double) {
            nativeVolumes[channel] = volume.toFloat().coerceIn(0f, 1f)
            for ((key, player) in nativePlayers) if (key.startsWith("$channel:")) {
                val v = nativeVolumes[channel] ?: 1f; try { player.setVolume(v, v) } catch (_: Exception) {}
            }
            if (channel == "sfx") {
                val v = nativeVolumes[channel] ?: 1f
                for (streamId in activeSoundPoolStreams.keys) try { soundPool.setVolume(streamId, v, v) } catch (_: Exception) {}
            }
        }
        @JavascriptInterface fun getNativeAudioStatus(): String = JSONObject().apply {
            put("audioFiles", readAudioIndex().length())
            put("catalogFiles", audioCatalog.count())
            put("catalogDatabase", "audio_catalog.db")
            put("soundPoolLoadedSamples", loadedSoundPoolSamples.size)
            put("soundPoolRegisteredSamples", soundPoolSamples.size)
            put("activeSoundPoolStreams", activeSoundPoolStreams.size)
            put("activeMediaPlayers", nativePlayers.size)
            put("audioFocusGranted", audioFocusGranted)
            put("unavailableFiles", audioCatalog.unavailableCount())
            put("activeSessions", audioCatalog.activeSessionCount())
            put("latestImportJob", audioCatalog.latestImportJob() ?: JSONObject.NULL)
            put("ttsReady", ttsReady)
            put("volumes", JSONObject().apply { nativeVolumes.forEach { (key, value) -> put(key, value) } })
            put("audioDirectory", audioDir.absolutePath)
        }.toString()

        @JavascriptInterface fun playDriveSong(fileId: String, name: String, mimeType: String, size: Double, requestId: String) = runOnUiThread { this@MainActivity.playDriveSong(fileId, name, mimeType, size.toLong(), requestId) }
        @JavascriptInterface fun resumeSong(): Boolean = this@MainActivity.resumeSong()
        @JavascriptInterface fun pauseSong(): Boolean = this@MainActivity.pauseSong()
        @JavascriptInterface fun stopSong(): Boolean = this@MainActivity.stopSong(true)
        @JavascriptInterface fun getSongStatus(): String = songStatusJson().toString()
        @JavascriptInterface fun clearSongCache(): Boolean = this@MainActivity.clearSongCache()

        @JavascriptInterface fun getNativeHealthStatus(): String = JSONObject().apply {
            put("originTrusted", trustedOriginActive)
            put("storage", storageHealthJson())
            put("tts", JSONObject().put("ready", ttsReady).put("status", if (ttsReady) "verified" else "failed"))
            put("audio", JSONObject().apply {
                put("catalogCount", audioCatalog.count())
                put("unavailableCount", audioCatalog.unavailableCount())
                put("focusGranted", audioFocusGranted)
                put("latestImportJob", audioCatalog.latestImportJob() ?: JSONObject.NULL)
                put("status", if (audioCatalog.count() > 0) "verified" else "untested")
            })
            put("drive", JSONObject().apply {
                put("authorized", decryptSecret(privatePreferences.getString(driveTokenPreference, "") ?: "").isNotBlank())
                put("status", if (decryptSecret(privatePreferences.getString(driveTokenPreference, "") ?: "").isNotBlank()) "verified" else "untested")
            })
            put("gemini", JSONObject().apply {
                put("freeKey", getGeminiSecret("free").isNotBlank())
                put("paidKey", getGeminiSecret("paid").isNotBlank())
                put("status", if (getGeminiSecret("free").isNotBlank() || getGeminiSecret("paid").isNotBlank()) "configured_not_tested" else "untested")
            })
        }.toString()

        @JavascriptInterface fun deleteNativeSound(id: String): Boolean {
            if (isBackgroundAudioImportRunning()) return false
            val index = readAudioIndex(); val replacement = JSONArray(); var deleted = false
            for (i in 0 until index.length()) {
                val item = index.optJSONObject(i) ?: continue
                if (item.optString("id") == id) { unloadNativeSound(id); File(audioDir, item.optString("fileName")).delete(); audioCatalog.delete(id); deleted = true }
                else replacement.put(item)
            }
            if (deleted) writeAudioIndex(replacement); return deleted
        }
        @JavascriptInterface fun clearNativeSounds(): Boolean {
            if (isBackgroundAudioImportRunning()) return false
            stopNativeAudio()
            soundPoolSamples.keys.toList().forEach { unloadNativeSound(it) }
            val preserved = setOf("index.json", "biblioteca_sons.json", "audio_overrides.json")
            audioDir.listFiles()?.filter { it.name !in preserved }?.forEach { it.delete() }
            writeAudioIndex(JSONArray())
            audioCatalog.clear()
            return true
        }
        @JavascriptInterface fun shareText(title: String, text: String) = runOnUiThread {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            try {
                startActivity(Intent(sendIntent).setPackage("com.google.android.apps.bard"))
            } catch (_: Exception) {
                startActivity(Intent.createChooser(sendIntent, "Enviar prompt ao Gemini"))
            }
        }
        @JavascriptInterface fun setSharedImageTarget(json: String): Boolean = try {
            SharedImageInbox.setTarget(this@MainActivity, json)
        } catch (error: Exception) {
            Log.e("BRASEIRO_SHARE", "Falha ao persistir destino da imagem", error)
            false
        }
        @JavascriptInterface fun getSharedImageTarget(): String = SharedImageInbox.pendingTarget(this@MainActivity)
        @JavascriptInterface fun clearSharedImageTarget(): Boolean = SharedImageInbox.clearTarget(this@MainActivity)
        @JavascriptInterface fun consumeSharedImage(): String = pendingSharedImageReference.orEmpty()
        @JavascriptInterface fun peekSharedImage(): String = pendingSharedImageReference.orEmpty()
        @JavascriptInterface fun clearSharedImage(): Boolean {
            pendingSharedImageReference = null
            return SharedImageInbox.clear(this@MainActivity)
        }
        @JavascriptInterface fun barbaraHealth(): String = barbaraSupervisor.health().toString()
        @JavascriptInterface fun barbaraDeviceProfile(): String = barbaraSupervisor.deviceProfile().toString()
        @JavascriptInterface fun barbaraSupervisorInspect(hostStateJson: String, text: String, importance: String, inputChannel: String): String = try {
            barbaraSupervisor.supervisorInspect(JSONObject(hostStateJson.ifBlank { "{}" }), text, importance, inputChannel).toString()
        } catch (error: Exception) { JSONObject().put("available", false).put("reason", "invalid_supervisor_state").put("error", error.message ?: "invalid json").toString() }
        @JavascriptInterface fun barbaraSupervisorFinalize(hostStateJson: String, text: String, requestId: String, narration: String, importance: String, inputChannel: String, resolutionJson: String, presentationJson: String): String = try {
            val resolution = if (resolutionJson.isBlank() || resolutionJson == "null") null else JSONObject(resolutionJson)
            val presentation = if (presentationJson.isBlank() || presentationJson == "null") null else JSONObject(presentationJson)
            barbaraSupervisor.supervisorFinalizeNarrative(JSONObject(hostStateJson.ifBlank { "{}" }), text, requestId, narration, importance, inputChannel, resolution, presentation).toString()
        } catch (error: Exception) { JSONObject().put("available", true).put("valid", false).put("reason", "invalid_supervisor_envelope").put("error", error.message ?: "invalid json").toString() }
        @JavascriptInterface fun barbaraAudioScenePlan(eventsJson: String, contextJson: String, catalogJson: String, mixJson: String, narrationActive: Boolean): String = try {
            barbaraSupervisor.audioScenePlan(JSONArray(eventsJson.ifBlank { "[]" }), JSONObject(contextJson.ifBlank { "{}" }), JSONObject(catalogJson.ifBlank { "{}" }), JSONObject(mixJson.ifBlank { "{}" }), narrationActive).toString()
        } catch (error: Exception) { JSONObject().put("available", false).put("reason", "invalid_audio_envelope").put("error", error.message ?: "invalid json").toString() }
        @JavascriptInterface fun barbaraVisiblePois(canonicalJson: String, campaignJson: String, proceduralJson: String, gm: Boolean): String = try {
            barbaraSupervisor.visiblePois(JSONArray(canonicalJson.ifBlank { "[]" }), JSONArray(campaignJson.ifBlank { "[]" }), JSONArray(proceduralJson.ifBlank { "[]" }), gm).toString()
        } catch (error: Exception) { "[]" }
        @JavascriptInterface fun barbaraContract(): String = barbaraSupervisor.oneRingContract().toString()
        @JavascriptInterface fun barbaraPlan(action: String, importance: String, narrative: Boolean, nonAdvancing: Boolean, location: String, imprint: String, inputChannel: String, storyOccasion: String): String =
            barbaraSupervisor.plan(action, importance, narrative, nonAdvancing, location, imprint, inputChannel, storyOccasion).toString()
        @JavascriptInterface fun barbaraValidate(envelopeJson: String, action: String, importance: String, occasion: String, inputChannel: String): String = try {
            barbaraSupervisor.validate(JSONObject(envelopeJson.ifBlank { "{}" }), action, importance, occasion, inputChannel).toString()
        } catch (error: Exception) { JSONObject().put("valid", false).put("reason", "invalid_envelope_json").put("error", error.message ?: "invalid json").toString() }
        @JavascriptInterface fun barbaraValidateState(hostStateJson: String): String = try {
            barbaraSupervisor.validateState(JSONObject(hostStateJson.ifBlank { "{}" })).toString()
        } catch (error: Exception) { JSONObject().put("valid", false).put("reason", "invalid_state_json").put("error", error.message ?: "invalid json").toString() }
        @JavascriptInterface fun barbaraInterpretRoll(featJson: String, successJson: String, target: Double, mode: String, weary: Boolean, modifier: Double, hostRollJson: String): String = try {
            barbaraSupervisor.interpretOneRing(JSONArray(featJson.ifBlank { "[]" }), JSONArray(successJson.ifBlank { "[]" }), target.toInt(), mode, weary, modifier.toInt(), JSONObject(hostRollJson.ifBlank { "{}" })).toString()
        } catch (error: Exception) { JSONObject().put("available", true).put("valid", false).put("reason", "invalid_roll_envelope").put("error", error.message ?: "invalid json").toString() }

        @JavascriptInterface fun rollOneRingPhysical(successDiceCount: Double, rollMode: String, actorId: String, actorName: String, requestId: String) = runOnUiThread {
            try {
                OneRingDice3DOverlay(this@MainActivity, diceAppearanceStore).roll(successDiceCount.toInt(), rollMode, stableDiceActorId(actorId), actorName, object : OneRingDice3DOverlay.ResultCallback {
                    override fun onResult(result: JSONObject) { resolveDice(requestId, result) }
                    override fun onCancel() { rejectDice(requestId, "dice_cancelled") }
                    override fun onError(error: Throwable) { rejectDice(requestId, error.message ?: "physical_dice_error") }
                })
            } catch (error: Throwable) { rejectDice(requestId, error.message ?: "physical_dice_error") }
        }

        @JavascriptInterface fun rollPhysicalDice(formula: String, actorId: String, actorName: String, requestId: String) = runOnUiThread {
            try {
                Dice3DOverlay(this@MainActivity, diceAppearanceStore).roll("ROLAGEM 3D", formula, stableDiceActorId(actorId), actorName, null, object : Dice3DOverlay.ResultCallback {
                    override fun onResult(result: JSONObject) { resolveDice(requestId, result) }
                    override fun onCancel() { rejectDice(requestId, "dice_cancelled") }
                })
            } catch (error: Throwable) { rejectDice(requestId, error.message ?: "physical_dice_error") }
        }

        @JavascriptInterface fun exportDocument(fileName: String, json: String, requestId: String) = runOnUiThread {
            if (pendingExport != null) {
                rejectExport(requestId, "Uma exportação já está em andamento. Conclua ou cancele a janela atual.")
                return@runOnUiThread
            }
            lifecycleScope.launch {
                try {
                    val safeName = sanitizeExportFileName(fileName)
                    val temp = withContext(Dispatchers.IO) {
                        val dir = File(cacheDir, "pending_exports").apply { mkdirs() }
                        File(dir, "${UUID.randomUUID()}.json.tmp").also { target ->
                            FileOutputStream(target).use { output ->
                                output.write(json.toByteArray(Charsets.UTF_8))
                                output.flush()
                                output.fd.sync()
                            }
                            if (target.length() <= 0L) throw IllegalStateException("A ficha ficou vazia antes da exportação.")
                        }
                    }
                    pendingExport = PendingExport(requestId, safeName, temp)
                    exportDocumentLauncher.launch(safeName)
                } catch (error: Exception) {
                    pendingExport?.temporaryFile?.delete()
                    pendingExport = null
                    rejectExport(requestId, error.message ?: "Não foi possível preparar o arquivo para exportação.")
                }
            }
        }

        // Compatibilidade com bundles antigos. A versão atual usa exportDocument com callback único.
        @JavascriptInterface fun exportBackup(fileName: String, json: String) {
            exportDocument(fileName, json, "legacy_export_${System.currentTimeMillis()}")
        }
    }

    override fun onDestroy() {
        voiceInputController.destroy()
        fileCallback?.onReceiveValue(null)
        pendingExport?.temporaryFile?.delete()
        pendingExport = null
        stopNativeSpeech(true)
        textToSpeech?.shutdown()
        stopNativeAudio()
        stopSong(true)
        abandonNativeAudioFocusIfIdle(true)
        if (receiverRegistered) try { unregisterReceiver(becomingNoisyReceiver) } catch (_: Exception) {}
        receiverRegistered = false
        try { soundPool.release() } catch (_: Exception) {}
        try { audioCatalog.close() } catch (_: Exception) {}
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("BraseiroNative")
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}





