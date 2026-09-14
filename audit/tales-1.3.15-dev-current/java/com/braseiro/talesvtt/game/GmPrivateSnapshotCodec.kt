package com.braseiro.talesvtt.game

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Private persistence for GM-only facts. Never expose through player UI StateFlow. */
object GmPrivateSnapshotCodec {
  private const val VERSION = 2
  private fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8.name())
  private fun dec(v: String) = URLDecoder.decode(v, StandardCharsets.UTF_8.name())

  fun encode(state: GmPrivateState): String = buildString {
    append("version=").append(VERSION).append('\n')
    append("clues=").append(state.clues.values.joinToString("|") {
      listOf(enc(it.id), enc(it.title), enc(it.text), enc(it.location)).joinToString("~")
    }).append('\n')
    append("connections=").append(state.connections.values.joinToString("|") {
      listOf(enc(it.id), enc(it.from), enc(it.to)).joinToString("~")
    }).append('\n')
    append("locations=").append(state.locations.values.joinToString("|") {
      listOf(enc(it.id), enc(it.name)).joinToString("~")
    }).append('\n')
    append("npcs=").append(state.npcs.values.joinToString("|") {
      listOf(enc(it.id),enc(it.name),enc(it.role),enc(it.description),enc(it.location),enc(it.status)).joinToString("~")
    }).append('\n')
    append("worldFacts=").append(state.worldFacts.values.joinToString("|") {
      listOf(enc(it.id),enc(it.subject),enc(it.value),enc(it.scope)).joinToString("~")
    }).append('\n')
    append("countdowns=").append(state.countdowns.values.joinToString("|") {
      listOf(enc(it.id),enc(it.title),it.stage.toString(),it.maxStage.toString(),enc(it.consequence)).joinToString("~")
    }).append('\n')
  }

  fun decode(text: String): GmPrivateState? = runCatching {
    val map = text.lineSequence().filter { '=' in it }
      .associate { it.substringBefore('=') to it.substringAfter('=') }
    val version=map["version"]?.toIntOrNull() ?: return null
    if(version !in 1..VERSION) return null

    val clues = map["clues"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p = raw.split('~')
      if (p.size >= 4) PrivateClue(dec(p[0]), dec(p[1]), dec(p[2]), dec(p[3])) else null
    }.associateBy { it.id }

    val connections = map["connections"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p = raw.split('~')
      if (p.size >= 3) PrivateConnection(dec(p[0]), dec(p[1]), dec(p[2])) else null
    }.associateBy { it.id }

    val locations = map["locations"].orEmpty().split('|').filter { it.isNotBlank() }.mapNotNull { raw ->
      val p = raw.split('~')
      if (p.size >= 2) PrivateLocation(dec(p[0]), dec(p[1])) else null
    }.associateBy { it.id }

    val npcs = if(version>=2) map["npcs"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
      val p=raw.split('~'); if(p.size>=6) PrivateNpc(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3]),dec(p[4]),dec(p[5])) else null
    }.associateBy{it.id} else emptyMap()

    val facts = if(version>=2) map["worldFacts"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
      val p=raw.split('~'); if(p.size>=4) PrivateWorldFact(dec(p[0]),dec(p[1]),dec(p[2]),dec(p[3])) else null
    }.associateBy{it.id} else emptyMap()

    val countdowns = if(version>=2) map["countdowns"].orEmpty().split('|').filter{it.isNotBlank()}.mapNotNull { raw ->
      val p=raw.split('~'); if(p.size>=5) {
        val stage=p[2].toIntOrNull(); val max=p[3].toIntOrNull()
        if(stage!=null && max!=null && max in 1..12 && stage in 0..max) PrivateCountdown(dec(p[0]),dec(p[1]),stage,max,dec(p[4])) else null
      } else null
    }.associateBy{it.id} else emptyMap()

    GmPrivateState(clues,connections,locations,npcs,facts,countdowns)
  }.getOrNull()
}
