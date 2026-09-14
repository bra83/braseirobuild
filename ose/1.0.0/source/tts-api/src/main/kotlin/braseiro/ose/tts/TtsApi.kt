package braseiro.ose.tts

enum class TtsState { IDLE, PREPARING, PLAYING, STOPPED, ERROR }

interface TtsPort {
    val state: TtsState
    fun playVisibleNarration(text: String)
    fun stop()
    fun shutdown()
}
