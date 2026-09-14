package com.braseiro.talesvtt.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.os.Bundle
import com.braseiro.talesvtt.MainActivity
import com.braseiro.talesvtt.game.GameState
import java.io.File
import java.security.MessageDigest

object SceneImageExchange {
  const val ACTION_CONSUME = "com.braseiro.talesvtt.CONSUME_SHARED_SCENE_IMAGE"
  const val EXTRA_TOKEN = "scene_image_token"
  const val EXTRA_ERROR = "scene_image_error"
  private const val PREFS = "tftl_scene_image_exchange"

  fun fingerprint(state: GameState): String {
    val raw = listOf(state.scene.title, state.scene.chapter, state.scene.location, state.scene.time, state.scene.narrative.joinToString("\n")).joinToString("|")
    return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).take(10).joinToString("") { "%02x".format(it) }
  }

  fun buildPrompt(state: GameState): String {
    val currentLocation = state.scene.location.trim().ifBlank { "Local atual não informado" }
    val visibleNarrative = state.scene.narrative
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .takeLast(5)
      .joinToString("\n\n")
      .take(5000)
      .ifBlank { "Nenhuma narração adicional foi exibida nesta cena." }

    return buildString {
      appendLine("Crie uma ilustração horizontal cinematográfica 3:2 que RESUMA VISUALMENTE A CENA ATUAL de Tales from the Loop.")
      appendLine("A imagem deve mostrar o instante que o jogador está vivendo AGORA. A narração visível abaixo é a fonte principal; transforme o que ela descreve em imagem, sem escrever a narração dentro da imagem.")
      appendLine("PRIORIDADE MÁXIMA: o local atual é $currentLocation. Não troque este local por fábrica, indústria, laboratório, interior, lago, quarto ou qualquer outro cenário só por causa do título do Mistério, de imagens anteriores ou de referências internas.")
      appendLine("Estética: retrofuturismo escandinavo dos anos 1980, cotidiano crível, tecnologia impossível integrada ao ambiente, pintura atmosférica, sem HUD, sem texto, sem logos, sem interface.")
      appendLine("Título do Mistério/cena: ${state.scene.title}")
      appendLine("LOCAL ATUAL OBRIGATÓRIO: $currentLocation")
      appendLine("Época: ${state.scene.seasonYear}")
      appendLine("Clima: ${state.scene.weather}")
      appendLine("Horário: ${state.scene.time}")
      appendLine("Luz: ${state.scene.daylight}")
      appendLine()
      appendLine("NARRAÇÃO VISÍVEL DA CENA ATUAL:")
      appendLine(visibleNarrative)
      appendLine()
      appendLine("REGRAS DE FIDELIDADE:")
      appendLine("- Resuma em um único quadro os elementos mais importantes e mais recentes da narração visível.")
      appendLine("- O local atual acima é soberano. Se a cena está diante de uma escola, mostre a frente da escola; se está numa rua, mostre a rua; não substitua o cenário por uma instalação industrial.")
      appendLine("- O título pode descrever o Mistério inteiro e NÃO autoriza mudar o local da cena atual.")
      appendLine("- Não use nem infira uma composição antiga da biblioteca quando ela conflitar com a cena atual.")
      appendLine("- Mostre somente o que seria visível ao jogador neste momento. Não revele pistas secretas, pensamentos privados do Mestre ou acontecimentos ainda não percebidos.")
      appendLine("- Não coloque legendas, texto, interface ou explicações na imagem.")
    }.trim()
  }

  fun prepareShare(context: Context, state: GameState): Intent {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    prefs.edit().putString("pending_fingerprint", fingerprint(state)).apply()
    return Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Prompt da cena — ${state.scene.title}")
      putExtra(Intent.EXTRA_TEXT, buildPrompt(state))
      addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }
  }

  fun acceptImportedToken(context: Context, token: String, current: GameState): String {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val expected = prefs.getString("pending_fingerprint", "").orEmpty()
    require(expected.isNotBlank()) { "Nenhuma cena está aguardando imagem." }
    require(expected == fingerprint(current)) { "A cena mudou depois da exportação do prompt; imagem não vinculada." }
    val path = prefs.getString("token_$token", "").orEmpty()
    require(path.isNotBlank() && File(path).isFile) { "Imagem importada não encontrada." }
    prefs.edit().remove("token_$token").remove("pending_fingerprint").apply()
    return Uri.fromFile(File(path)).toString()
  }

  internal fun storeIncoming(context: Context, source: Uri): String {
    val resolver = context.contentResolver
    val input = resolver.openInputStream(source) ?: error("Não foi possível abrir a imagem compartilhada.")
    val dir = File(context.filesDir, "scene_images").apply { mkdirs() }
    val token = "img_${System.currentTimeMillis()}_${(1000..9999).random()}"
    val target = File(dir, "$token.img")
    input.use { src -> target.outputStream().use { dst -> src.copyTo(dst, 64 * 1024) } }
    require(target.length() in 1..(25L * 1024L * 1024L)) { "Imagem vazia ou maior que 25 MB." }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(target.absolutePath, bounds)
    require(bounds.outWidth in 320..8192 && bounds.outHeight in 240..8192) { "Arquivo compartilhado não contém uma imagem válida em tamanho suportado." }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("token_$token", target.absolutePath).apply()
    return token
  }
}

class ShareReceiverActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    consume(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    consume(intent)
  }

  private fun consume(incoming: Intent?) {
    if (incoming?.action != Intent.ACTION_SEND || incoming.type?.startsWith("image/") != true) {
      forward(error = "O item compartilhado não é uma imagem compatível.")
      return
    }
    val source = if (android.os.Build.VERSION.SDK_INT >= 33) {
      incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
      @Suppress("DEPRECATION") incoming.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    } ?: incoming.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

    if (source == null) {
      forward(error = "O aplicativo de origem não forneceu a imagem.")
      return
    }
    Thread {
      val result = runCatching { SceneImageExchange.storeIncoming(this, source) }
      runOnUiThread { forward(result.getOrNull(), result.exceptionOrNull()?.message) }
    }.start()
  }

  private fun forward(token: String? = null, error: String? = null) {
    startActivity(Intent(this, MainActivity::class.java).apply {
      action = SceneImageExchange.ACTION_CONSUME
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      token?.let { putExtra(SceneImageExchange.EXTRA_TOKEN, it) }
      error?.let { putExtra(SceneImageExchange.EXTRA_ERROR, it) }
    })
    finish()
  }
}
