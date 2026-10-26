package com.braseiro.talesvtt.life

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.random.Random

data class MatrixManifestation(
  val id: String,
  val index: Int,
  val theme: String,
  val title: String,
  val description: String,
  val domain: String,
  val responseType: String,
  val targetProfile: String
)

data class MatrixOutcome(
  val causeRoll: Int,
  val domainRoll: Int,
  val manifestation: MatrixManifestation,
  val timingDays: Int,
  val responseHint: String,
  val passedCoherenceFilter: Boolean,
  val coherenceReason: String
)

class PersistentLifeEngine(
  private val context: Context,
  private val seed: Long = 19860925L
) {
  private val rng = Random(seed)
  private val cachedTalesManifestations = mutableListOf<MatrixManifestation>()
  private val cachedFloodManifestations = mutableListOf<MatrixManifestation>()
  private var loaded = false

  fun loadMatricesIfNeeded() {
    if (loaded) return
    try {
      val am = context.assets
      // Load Tales Kids pack
      val talesStream = am.open("braseiro_life_v2/data/matrices/packs/tales-kids.json")
      val talesJson = JSONObject(talesStream.bufferedReader().use { it.readText() })
      val talesArr = talesJson.optJSONArray("entries")
      if (talesArr != null) {
        for (i in 0 until talesArr.length()) {
          val item = talesArr.optJSONObject(i) ?: continue
          cachedTalesManifestations.add(parseManifestation(item, "TALES_KIDS"))
        }
      }

      // Load Flood Teens pack
      val floodStream = am.open("braseiro_life_v2/data/matrices/packs/flood-teens.json")
      val floodJson = JSONObject(floodStream.bufferedReader().use { it.readText() })
      val floodArr = floodJson.optJSONArray("entries")
      if (floodArr != null) {
        for (i in 0 until floodArr.length()) {
          val item = floodArr.optJSONObject(i) ?: continue
          cachedFloodManifestations.add(parseManifestation(item, "FLOOD_TEENS"))
        }
      }
      loaded = true
    } catch (e: Exception) {
      // Fallback in-memory generators if assets cannot be read
      generateFallbackMatrices()
      loaded = true
    }
  }

  private fun parseManifestation(obj: JSONObject, defaultProfile: String): MatrixManifestation {
    return MatrixManifestation(
      id = obj.optString("id", "matrix-item"),
      index = obj.optInt("index", 1),
      theme = obj.optString("theme", "geral"),
      title = obj.optString("title", "Acontecimento"),
      description = obj.optString("description", "Acontecimento cotidiano"),
      domain = obj.optString("domain", "social"),
      responseType = obj.optString("responseType", "neutro"),
      targetProfile = obj.optString("targetProfile", defaultProfile)
    )
  }

  private fun generateFallbackMatrices() {
    for (i in 1..100) {
      cachedTalesManifestations.add(
        MatrixManifestation(
          id = "tales-fb-$i",
          index = i,
          theme = "rotina_80s",
          title = "Acontecimento Cotidiano #$i",
          description = "Ruídos estáticos no rádio e bicicletas abandonadas perto do bosque do Loop.",
          domain = "tecnologia",
          responseType = "curiosidade",
          targetProfile = "TALES_KIDS"
        )
      )
      cachedFloodManifestations.add(
        MatrixManifestation(
          id = "flood-fb-$i",
          index = i,
          theme = "atrito_90s",
          title = "Conflito Juvenil #$i",
          description = "Água podre e máquinas enferrujadas geram tensão na periferia.",
          domain = "ambiente",
          responseType = "vergonha",
          targetProfile = "FLOOD_TEENS"
        )
      )
    }
  }

  /**
   * Evaluates a causal candidate manifestation using separate independent d100 rolls
   * and runs the Coherence Filter to protect player agency and thematic plausibility.
   */
  fun sampleLifeEvent(
    profile: String,
    turnCount: Int,
    currentLocation: String,
    isOffScene: Boolean = false
  ): MatrixOutcome {
    loadMatricesIfNeeded()

    // Independent d100 rolls
    val turnSeed = (seed xor (turnCount.toLong() * 31337L) xor System.currentTimeMillis())
    val turnRng = Random(turnSeed)

    val causeRoll = turnRng.nextInt(1, 101)
    val domainRoll = turnRng.nextInt(1, 101)
    val manifestationRoll = turnRng.nextInt(1, 101)
    val timingRoll = turnRng.nextInt(1, 15) // 1 to 14 fictional days delay

    val list = if (profile == "FLOOD_TEENS") cachedFloodManifestations else cachedTalesManifestations
    val safeIndex = ((manifestationRoll - 1) % list.size).coerceIn(0, list.size - 1)
    val candidate = list[safeIndex]

    // Coherence Filter predicates
    var passed = true
    var reason = "Coerente com o perfil e ambientação atual."

    if (candidate.targetProfile != profile) {
      passed = false
      reason = "Violação de Firewall: candidato ${candidate.targetProfile} não pertence ao perfil ativo $profile."
    }

    // Protect player agency: no forced conscious player decisions
    if (candidate.description.contains("o jogador decide", ignoreCase = true) ||
        candidate.description.contains("o personagem confessou", ignoreCase = true)
    ) {
      passed = false
      reason = "Violação de Agência: eventos da matriz não podem impor decisões conscientes ao personagem."
    }

    val responseHint = when {
      candidate.domain == "ambiente" -> "Refletir sensorialmente no clima e detalhes visuais da cena."
      candidate.domain == "social" -> "NPCs locais demonstram pequenas reações ou boatos espontâneos."
      candidate.domain == "tecnologia" -> "Aparelhos e máquinas ao redor apresentam falhas sutis ou anomalias."
      else -> "Introduzir como detalhe incidental que enriquece a cena sem forçar combate."
    }

    return MatrixOutcome(
      causeRoll = causeRoll,
      domainRoll = domainRoll,
      manifestation = candidate,
      timingDays = timingRoll,
      responseHint = responseHint,
      passedCoherenceFilter = passed,
      coherenceReason = reason
    )
  }
}
