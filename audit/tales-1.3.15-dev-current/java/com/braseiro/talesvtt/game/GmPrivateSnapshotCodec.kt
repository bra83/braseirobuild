package com.braseiro.talesvtt.game

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Private persistence for GM-only facts. Never expose through player UI StateFlow. */
object GmPrivateSnapshotCodec {
  private const val VERSION = 1
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
  }

  fun decode(text: String): GmPrivateState? = runCatching {
    val map = text.lineSequence().filter { '=' in it }
      .associate { it.substringBefore('=') to it.substringAfter('=') }
    if (map["version"]?.toIntOrNull() != VERSION) return null

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

    GmPrivateState(clues, connections, locations)
  }.getOrNull()
}
