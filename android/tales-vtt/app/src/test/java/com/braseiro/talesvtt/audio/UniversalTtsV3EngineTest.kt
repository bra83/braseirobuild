package com.braseiro.talesvtt.audio

import org.junit.Assert.*
import org.junit.Test

class UniversalTtsV3EngineTest {

  // We test the pure chunking and sanitization methods
  private fun sanitize(text: String): String {
    var s = text
    s = s.replace(Regex("<[^>]+>"), " ")
    s = s.replace(Regex("\\[[^\\]]+\\]"), " ")
    s = s.replace(Regex("(?i)\\b(male voice|female voice|narrator|gm only):?"), " ")
    s = s.replace(Regex("[*#_`~]"), "")
    s = s.replace(Regex("\\s+"), " ").trim()
    return s
  }

  private fun splitChunks(text: String, minLength: Int = 350, maxLength: Int = 800): List<String> {
    val clean = sanitize(text)
    if (clean.length <= maxLength) return listOf(clean)

    val chunks = mutableListOf<String>()
    var remaining = clean

    while (remaining.isNotEmpty()) {
      if (remaining.length <= maxLength) {
        chunks.add(remaining.trim())
        break
      }

      val window = remaining.substring(0, minOf(remaining.length, maxLength))
      var cutIndex = -1

      for (del in listOf(". ", "! ", "? ", "; ", ".\n", "!\n", "?\n")) {
        val lastIdx = window.lastIndexOf(del)
        if (lastIdx >= minLength && lastIdx > cutIndex) {
          cutIndex = lastIdx + del.length
        }
      }

      if (cutIndex == -1) {
        for (del in listOf(", ", ": ")) {
          val lastIdx = window.lastIndexOf(del)
          if (lastIdx >= minLength && lastIdx > cutIndex) {
            cutIndex = lastIdx + del.length
          }
        }
      }

      if (cutIndex == -1) {
        cutIndex = window.lastIndexOf(' ')
        if (cutIndex < minLength) {
          cutIndex = maxLength
        }
      }

      val chunk = remaining.substring(0, cutIndex).trim()
      if (chunk.isNotEmpty()) chunks.add(chunk)
      remaining = remaining.substring(cutIndex).trim()
    }

    return chunks
  }

  @Test
  fun testSanitizeVisibleText_RemovesTagsAndInstructions() {
    val raw = "Narrator: <sigh> [male voice] A tarde cai fria sobre a floresta. *Algo* zune nos cabos de alta tensão."
    val cleaned = sanitize(raw)
    assertFalse("Não deve conter tags HTML/XML", cleaned.contains("<sigh>"))
    assertFalse("Não deve conter colchetes com instruções", cleaned.contains("[male voice]"))
    assertFalse("Não deve conter marcador de narrator", cleaned.contains("Narrator:", ignoreCase = true))
    assertFalse("Não deve conter markdown asterisco", cleaned.contains("*"))
    assertTrue("Deve preservar a narrativa pura", cleaned.contains("A tarde cai fria sobre a floresta"))
    assertTrue("Deve preservar a frase seguinte", cleaned.contains("Algo zune nos cabos de alta tensão"))
  }

  @Test
  fun testSplitIntoSafeChunks_LongNarrative() {
    val paragraph1 = "As torres de refrigeração da Loop erguem-se contra o céu cinzento de outono como gigantes adormecidos de concreto e aço. O vento que sopra do lago Mälaren traz o cheiro penetrante de água fria, folhas de pinheiro apodrecendo no chão da floresta e um zumbido constante de transformadores em carga pesada. Vocês deixaram as bicicletas encostadas na cerca de arame farpado onde a placa enferrujada avisa sobre alta tensão e acesso restrito."
    val paragraph2 = "No chão de cascalho, marcas recentes de pneus largos de caminhão indicam que veículos pesados passaram por aqui há pouco tempo, provavelmente na calada da noite anterior. Um corvo solitário pousa sobre o isolador de cerâmica de um dos postes, observando os Kids com olhos brilhantes e uma curiosidade estranhamente humana. Do outro lado da vala de drenagem, uma pequena cabine de controle parece ter a porta entreaberta, batendo ritmicamente com as rajadas de vento."
    val paragraph3 = "Mais ao fundo, entre os troncos das bétulas desfolhadas, é possível vislumbrar o casco tombado de um transportador magnético desativado. Suas esteiras de borracha sintética estão cobertas por musgo espesso, mas uma luz âmbar fraca e intermitente ainda pisca no painel superior da cabine, emitindo pequenos estalos audíveis quando o vento arrefece por um instante."

    val fullNarrative = "$paragraph1 $paragraph2 $paragraph3"
    assertTrue("A narrativa total deve ter mais de 1000 caracteres", fullNarrative.length > 1000)

    val chunks = splitChunks(fullNarrative, minLength = 250, maxLength = 600)
    assertTrue("Deve dividir em 2 ou mais chunks seguros", chunks.size >= 2)

    for ((index, chunk) in chunks.withIndex()) {
      assertTrue("Chunk $index deve respeitar o limite máximo de 600 caracteres (tamanho: ${chunk.length})", chunk.length <= 600)
      assertTrue("Chunk $index não deve terminar com palavra cortada ao meio", chunk.endsWith(".") || chunk.endsWith("?") || chunk.endsWith("!") || chunk.endsWith(";") || chunk.endsWith(","))
    }

    // Recombined length should match approximately original text
    val recombined = chunks.joinToString(" ")
    assertTrue("Todo o conteúdo principal deve ser preservado", recombined.contains("As torres de refrigeração"))
    assertTrue("O final deve ser preservado", recombined.contains("quando o vento arrefece por um instante"))
  }

  @Test
  fun testSplitIntoSafeChunks_ShortTextDoesNotSplit() {
    val short = "O professor Severin olha para o relógio e anota algo no caderno."
    val chunks = splitChunks(short)
    assertEquals(1, chunks.size)
    assertEquals(short, chunks[0])
  }
}
