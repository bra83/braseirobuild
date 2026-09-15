package com.braseiro.wwn.bridge

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import com.braseiro.wwn.audio.ProceduralAudioSynth
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/** Presentation-only audio runtime. Game rules/state remain entirely host-owned. */
class NativeAudioRuntime(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs ->
        // Safety migration: older builds auto-enabled procedural loops on first launch.
        // Disable only ambience/music once so an update immediately starts silent.
        if (!prefs.getBoolean(KEY_SAFE_AUDIO_DEFAULTS_APPLIED, false)) {
            prefs.edit()
                .putBoolean(KEY_AMBIENCE, false)
                .putBoolean(KEY_MUSIC, false)
                .putBoolean(KEY_SAFE_AUDIO_DEFAULTS_APPLIED, true)
                .apply()
        }
    }
    private val main = Handler(Looper.getMainLooper())
    private val ambienceSequence = AtomicLong(0)
    private val musicSequence = AtomicLong(0)
    private val ambienceRenderSequence = AtomicLong(0)
    private val musicRenderSequence = AtomicLong(0)
    private val loopSynthesisExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "wwn-audio-loop-synth").apply { isDaemon = true } }
    private val sfxSynthesisExecutor = ThreadPoolExecutor(
        1, 2, 20L, TimeUnit.SECONDS, ArrayBlockingQueue(8),
        { r -> Thread(r, "wwn-audio-sfx-synth").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardOldestPolicy()
    )
    private var ambienceTrack: AudioTrack? = null
    private var musicTrack: AudioTrack? = null
    private val oneShots = linkedSetOf<AudioTrack>()
    @Volatile private var ambienceAsset: String? = null
    @Volatile private var musicAsset: String? = null
    private var requestedAmbienceGain = 1f
    private var requestedMusicGain = 1f
    private var requestedSfxGain = 1f
    @Volatile private var narrationActive = false
    @Volatile private var pausedForLifecycle = false

    fun status(): JSONObject = JSONObject()
        .put("status", "OK")
        .put("ready", true)
        .put("ambienceEnabled", prefs.getBoolean(KEY_AMBIENCE, false))
        .put("musicEnabled", prefs.getBoolean(KEY_MUSIC, false))
        .put("sfxEnabled", prefs.getBoolean(KEY_SFX, true))
        .put("npcVoiceEnabled", prefs.getBoolean(KEY_NPC_VOICE, true))
        .put("masterVoiceEnabled", prefs.getBoolean(KEY_MASTER_VOICE, true))
        .put("masterVolume", masterVolume().toDouble())
        .put("ambienceAsset", ambienceAsset)
        .put("musicAsset", musicAsset)
        .put("narrationActive", narrationActive)
        .put("paused", pausedForLifecycle)
        .put("protocol", "barbara-audio-scene-plan-v1")
        .put("mechanicalMutation", false)

    fun configure(requestJson: String): JSONObject {
        require(requestJson.length <= 8_000) { "Configuração de áudio grande demais" }
        val o = JSONObject(requestJson)
        val e = prefs.edit()
        if (o.has("ambienceEnabled")) e.putBoolean(KEY_AMBIENCE, o.getBoolean("ambienceEnabled"))
        if (o.has("musicEnabled")) e.putBoolean(KEY_MUSIC, o.getBoolean("musicEnabled"))
        if (o.has("sfxEnabled")) e.putBoolean(KEY_SFX, o.getBoolean("sfxEnabled"))
        if (o.has("npcVoiceEnabled")) e.putBoolean(KEY_NPC_VOICE, o.getBoolean("npcVoiceEnabled"))
        if (o.has("masterVoiceEnabled")) e.putBoolean(KEY_MASTER_VOICE, o.getBoolean("masterVoiceEnabled"))
        if (o.has("masterVolume")) e.putFloat(KEY_VOLUME, o.getDouble("masterVolume").coerceIn(0.0, 1.0).toFloat())
        e.apply()
        main.post { applyVolumes(); if (!prefs.getBoolean(KEY_AMBIENCE, false)) stopLayer("ambience"); if (!prefs.getBoolean(KEY_MUSIC, false)) stopLayer("music") }
        return status()
    }

    fun applyScenePlan(planJson: String): JSONObject {
        require(planJson.length <= 48_000) { "Plano de áudio grande demais" }
        val plan = JSONObject(planJson)
        require(plan.optBoolean("canonical_state_affected", false).not()) { "Plano de áudio não pode alterar estado canônico" }
        val mix = plan.optJSONObject("mix") ?: JSONObject()
        requestedAmbienceGain = mix.optDouble("effective_ambience_gain", mix.optDouble("ambience_gain", 1.0)).coerceIn(0.0, 4.0).toFloat()
        requestedMusicGain = mix.optDouble("effective_music_gain", mix.optDouble("music_gain", 1.0)).coerceIn(0.0, 4.0).toFloat()
        requestedSfxGain = mix.optDouble("effective_sfx_gain", mix.optDouble("sfx_gain", 1.0)).coerceIn(0.0, 4.0).toFloat()
        narrationActive = plan.optBoolean("narration_active", narrationActive)
        val commands = plan.optJSONArray("commands") ?: JSONArray()
        val accepted = JSONArray()
        for (i in 0 until commands.length()) {
            val c = commands.optJSONObject(i) ?: continue
            val channel = c.optString("channel").lowercase()
            val asset = c.optString("host_asset_id", c.optString("asset")).trim().lowercase()
            if (asset.isBlank()) continue
            when (channel) {
                "ambience" -> if (prefs.getBoolean(KEY_AMBIENCE, false)) requestLoop("ambience", asset)
                "music" -> if (prefs.getBoolean(KEY_MUSIC, false)) requestLoop("music", asset)
                "sfx" -> if (prefs.getBoolean(KEY_SFX, true)) requestOneShot(asset)
            }
            accepted.put(JSONObject().put("channel", channel).put("asset", asset))
        }
        main.post { applyVolumes() }
        return JSONObject().put("status", "OK").put("accepted", accepted).put("mechanicalMutation", false)
    }

    fun setNarrationActive(active: Boolean): JSONObject {
        narrationActive = active
        main.post { applyVolumes() }
        return status()
    }

    fun stopAll(): JSONObject {
        narrationActive = false
        main.post {
            stopLayer("ambience"); stopLayer("music")
            oneShots.toList().forEach { runCatching { it.stop() }; runCatching { it.release() } }
            oneShots.clear()
        }
        return JSONObject().put("status", "OK").put("mechanicalMutation", false)
    }

    fun pauseForLifecycle() {
        pausedForLifecycle = true
        main.post {
            runCatching { ambienceTrack?.pause() }; runCatching { musicTrack?.pause() }
            oneShots.toList().forEach { runCatching { it.stop() }; runCatching { it.release() } }
            oneShots.clear()
        }
    }

    fun resumeFromLifecycle() {
        pausedForLifecycle = false
        main.post {
            if (prefs.getBoolean(KEY_AMBIENCE, false)) runCatching { ambienceTrack?.play() }
            if (prefs.getBoolean(KEY_MUSIC, false)) runCatching { musicTrack?.play() }
            applyVolumes()
        }
    }

    fun shutdown() {
        ambienceRenderSequence.incrementAndGet()
        musicRenderSequence.incrementAndGet()
        loopSynthesisExecutor.shutdownNow()
        sfxSynthesisExecutor.shutdownNow()
        main.post {
            stopLayer("ambience"); stopLayer("music")
            oneShots.toList().forEach { runCatching { it.stop() }; runCatching { it.release() } }
            oneShots.clear()
            main.removeCallbacksAndMessages(null)
        }
    }
    fun npcVoiceEnabled(): Boolean = prefs.getBoolean(KEY_NPC_VOICE, true)
    fun masterVoiceEnabled(): Boolean = prefs.getBoolean(KEY_MASTER_VOICE, true)

    private fun requestLoop(channel: String, asset: String) {
        val sequence = if (channel == "ambience") ambienceRenderSequence else musicRenderSequence
        val token = sequence.incrementAndGet()
        val currentAsset = if (channel == "ambience") ambienceAsset else musicAsset
        if (currentAsset == asset) { main.post { applyVolumes() }; return }
        loopSynthesisExecutor.execute {
            val pcm = runCatching {
                ProceduralAudioSynth.renderLoop(
                    asset, SAMPLE_RATE, if (channel == "music") 8 else 6,
                    ProceduralAudioSynth.stableSeed("WWN:$channel:$asset")
                )
            }.getOrNull() ?: return@execute
            if (token != sequence.get()) return@execute
            main.post {
                if (token != sequence.get()) return@post
                val enabled = if (channel == "ambience") prefs.getBoolean(KEY_AMBIENCE, false) else prefs.getBoolean(KEY_MUSIC, false)
                if (!enabled) return@post
                installLoop(channel, asset, pcm)
            }
        }
    }

    private fun installLoop(channel: String, asset: String, pcm: ShortArray) {
        val next = createTrack(pcm, looping = true, usage = AudioAttributes.USAGE_GAME)
        next.setVolume(0f)
        if (!pausedForLifecycle) next.play()
        val old = if (channel == "ambience") ambienceTrack else musicTrack
        if (channel == "ambience") { ambienceTrack = next; ambienceAsset = asset } else { musicTrack = next; musicAsset = asset }
        fadeSwap(old, next, channel)
    }

    private fun fadeSwap(old: AudioTrack?, next: AudioTrack, channel: String) {
        val sequence = if (channel == "ambience") ambienceSequence else musicSequence
        val token = sequence.incrementAndGet()
        val steps = 10
        for (step in 0..steps) main.postDelayed({
            if (token != sequence.get()) return@postDelayed
            val x = step.toFloat() / steps
            val target = layerVolume(channel)
            runCatching { next.setVolume(target * x) }
            old?.let { runCatching { it.setVolume(target * (1f - x)) } }
            if (step == steps && old != null) { runCatching { old.stop() }; runCatching { old.release() } }
        }, step * 65L)
    }

    private fun requestOneShot(asset: String) {
        try {
            sfxSynthesisExecutor.execute {
                val pcm = runCatching {
                    ProceduralAudioSynth.renderOneShot(asset, SAMPLE_RATE, System.nanoTime() xor ProceduralAudioSynth.stableSeed(asset))
                }.getOrNull() ?: return@execute
                main.post {
                    if (!prefs.getBoolean(KEY_SFX, true) || pausedForLifecycle) return@post
                    installOneShot(pcm)
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
            // Presentation-only SFX may be dropped under load rather than block gameplay/UI.
        }
    }

    private fun installOneShot(pcm: ShortArray) {
        val track = createTrack(pcm, looping = false, usage = AudioAttributes.USAGE_GAME)
        track.setVolume((masterVolume() * requestedSfxGain).coerceIn(0f, 1f))
        oneShots.add(track); track.play()
        val ms = ((pcm.size * 1000.0 / SAMPLE_RATE) + 180.0).roundToInt().toLong()
        main.postDelayed({ oneShots.remove(track); runCatching { track.stop() }; runCatching { track.release() } }, ms)
    }

    private fun createTrack(pcm: ShortArray, looping: Boolean, usage: Int): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        val written = track.write(pcm, 0, pcm.size)
        require(written == pcm.size) { "Falha ao preparar áudio procedural" }
        if (looping) track.setLoopPoints(0, pcm.size, -1)
        return track
    }

    private fun applyVolumes() {
        runCatching { ambienceTrack?.setVolume(layerVolume("ambience")) }
        runCatching { musicTrack?.setVolume(layerVolume("music")) }
    }
    private fun layerVolume(channel: String): Float {
        val base = masterVolume()
        return when (channel) {
            "ambience" -> (base * requestedAmbienceGain * if (narrationActive) 0.62f else 1f).coerceIn(0f, 1f)
            "music" -> (base * requestedMusicGain * if (narrationActive) 0.42f else 1f).coerceIn(0f, 1f)
            else -> base
        }
    }
    private fun masterVolume(): Float = prefs.getFloat(KEY_VOLUME, 0.72f).coerceIn(0f, 1f)
    private fun stopLayer(channel: String) {
        if (channel == "ambience") ambienceRenderSequence.incrementAndGet() else musicRenderSequence.incrementAndGet()
        val track = if (channel == "ambience") ambienceTrack else musicTrack
        runCatching { track?.stop() }; runCatching { track?.release() }
        if (channel == "ambience") { ambienceTrack = null; ambienceAsset = null } else { musicTrack = null; musicAsset = null }
    }

    companion object {
        private const val SAMPLE_RATE = 24_000
        private const val PREFS = "wwn_audio_v1"
        private const val KEY_AMBIENCE = "ambience_enabled"
        private const val KEY_MUSIC = "music_enabled"
        private const val KEY_SFX = "sfx_enabled"
        private const val KEY_NPC_VOICE = "npc_voice_enabled"
        private const val KEY_MASTER_VOICE = "master_voice_enabled"
        private const val KEY_VOLUME = "master_volume"
        private const val KEY_SAFE_AUDIO_DEFAULTS_APPLIED = "safe_audio_defaults_v2"
    }
}
