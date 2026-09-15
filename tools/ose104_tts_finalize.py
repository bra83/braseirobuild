from pathlib import Path
ROOT=Path('ose/1.0.0/source')

def read(p): return (ROOT/p).read_text()
def write(p,s): (ROOT/p).write_text(s)
def once(s,old,new,label):
    if new in s: return s
    if old not in s: raise SystemExit(f'MISSING:{label}')
    return s.replace(old,new,1)

# Harden TTS playback so the WebView receives real playback/error state.
p='app-android/src/main/kotlin/braseiro/ose/app/GeminiTtsController.kt'; s=read(p)
s=once(s,'enum class Result { STARTING, STOPPED, REPLAYING, MISSING_KEY, EMPTY_NARRATION }','enum class Result { STARTING, STOPPED, REPLAYING, MISSING_KEY, EMPTY_NARRATION, ERROR }','TTS_ENUM')
s=once(s,'    private var lastNarration: String? = null\n','    private var lastNarration: String? = null\n    @Volatile var onState: ((Result, String?) -> Unit)? = null\n','TTS_CALLBACK')
s=once(s,'''    fun stop(): Result {
        main.post { player?.stop(); player?.release(); player = null }
        return Result.STOPPED
    }
''','''    fun stop(): Result {
        main.post {
            runCatching { player?.stop() }
            player?.release(); player = null
            onState?.invoke(Result.STOPPED, null)
        }
        return Result.STOPPED
    }
''','TTS_STOP')
s=once(s,'''    private fun playFile(file: File) {
        main.post {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare()
                start()
            }
        }
    }
''','''    private fun playFile(file: File) {
        main.post {
            try {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnPreparedListener { media ->
                        media.start()
                        onState?.invoke(Result.STARTING, null)
                    }
                    setOnCompletionListener { media ->
                        media.release(); if (player === media) player = null
                        onState?.invoke(Result.STOPPED, null)
                    }
                    setOnErrorListener { media, what, extra ->
                        media.release(); if (player === media) player = null
                        onState?.invoke(Result.ERROR, "MEDIA_PLAYER_${what}_$extra")
                        true
                    }
                    prepareAsync()
                }
            } catch (t: Throwable) {
                player?.release(); player = null
                onState?.invoke(Result.ERROR, t.message ?: t::class.java.simpleName)
            }
        }
    }
''','TTS_PLAYER')
write(p,s)

# Wire async TTS state to UI and make synchronous generation failures visible instead of generic bridge errors.
p='app-android/src/main/kotlin/braseiro/ose/app/MainActivity.kt'; s=read(p)
s=once(s,'        tts = GeminiTtsController(applicationContext, credentials, runtimeSettings, gemini)\n','''        tts = GeminiTtsController(applicationContext, credentials, runtimeSettings, gemini)
        tts.onState = { state, error ->
            send(JSONObject().put("type", "TtsState").put("payload", JSONObject()
                .put("state", state.name)
                .put("message", error ?: JSONObject.NULL)
                .put("configured", credentials.has(GeminiCredentialStore.Slot.TTS_AUDIO))))
        }
''','MAIN_TTS_CALLBACK')
old='''                        "TtsCommand" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            // The WebView is not trusted to choose what TTS reads. Only canonical visible narration is accepted.
                            val result = when (payload.optString("command")) {
                                "play" -> tts.playVisibleNarration(current.campaignState.game.session.visibleNarration)
                                "replay" -> tts.replayVisibleNarration(current.campaignState.game.session.visibleNarration)
                                "stop" -> tts.stop()
                                else -> error("UNKNOWN_TTS_COMMAND")
                            }
                            send(JSONObject().put("type", "TtsState").put("payload", JSONObject()
                                .put("state", result.name)
                                .put("configured", credentials.has(GeminiCredentialStore.Slot.TTS_AUDIO))))
                        }
'''
new='''                        "TtsCommand" -> {
                            val current = session.loadSession(CAMPAIGN_ID)
                            // Only canonical visible narration can be spoken.
                            try {
                                val result = when (payload.optString("command")) {
                                    "play" -> tts.playVisibleNarration(current.campaignState.game.session.visibleNarration)
                                    "replay" -> tts.replayVisibleNarration(current.campaignState.game.session.visibleNarration)
                                    "stop" -> tts.stop()
                                    else -> error("UNKNOWN_TTS_COMMAND")
                                }
                                send(JSONObject().put("type", "TtsState").put("payload", JSONObject()
                                    .put("state", result.name)
                                    .put("message", JSONObject.NULL)
                                    .put("configured", credentials.has(GeminiCredentialStore.Slot.TTS_AUDIO))))
                            } catch (t: Throwable) {
                                send(JSONObject().put("type", "TtsState").put("payload", JSONObject()
                                    .put("state", "ERROR")
                                    .put("message", safeError(t))
                                    .put("configured", credentials.has(GeminiCredentialStore.Slot.TTS_AUDIO))))
                            }
                        }
'''
s=once(s,old,new,'MAIN_TTS_COMMAND')
write(p,s)

# Show useful TTS errors to the player.
p='web-ui-p1/p1.js'; s=read(p)
s=once(s,"  else if(m.type==='TtsState')showToast(ttsMessage(p.state));","  else if(m.type==='TtsState')showToast(p.state==='ERROR'?`TTS: ${p.message||'falha ao reproduzir o áudio.'}`:ttsMessage(p.state));",'JS_TTS_ERROR')
write(p,s)
print('OSE_1_0_4_TTS_FINALIZE_APPLIED')
