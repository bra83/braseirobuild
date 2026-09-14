package braseiro.ose.tts.android

import android.content.Context
import android.speech.tts.TextToSpeech
import braseiro.ose.tts.TtsPort
import braseiro.ose.tts.TtsState
import java.util.Locale

class AndroidTtsController(context: Context) : TtsPort, TextToSpeech.OnInitListener {
    private var engine: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var pending: String? = null
    @Volatile override var state: TtsState = TtsState.PREPARING
        private set

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            state = TtsState.ERROR
            pending = null
            return
        }
        val tts = engine ?: return
        val language = tts.setLanguage(Locale("pt", "BR"))
        if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
            state = TtsState.ERROR
            pending = null
            return
        }
        state = TtsState.IDLE
        pending?.let { text -> pending = null; playVisibleNarration(text) }
    }

    override fun playVisibleNarration(text: String) {
        if (text.isBlank()) return
        val tts = engine ?: return
        if (state == TtsState.PREPARING) { pending = text; return }
        if (state == TtsState.ERROR) return
        tts.stop()
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "braseiro-visible-narration")
        state = if (result == TextToSpeech.SUCCESS) TtsState.PLAYING else TtsState.ERROR
    }

    override fun stop() {
        engine?.stop()
        if (state != TtsState.ERROR) state = TtsState.STOPPED
    }

    override fun shutdown() {
        pending = null
        engine?.stop()
        engine?.shutdown()
        engine = null
        state = TtsState.IDLE
    }
}
