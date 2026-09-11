from pathlib import Path
import re
import sys

root = Path(sys.argv[1])

def read(rel: str) -> str:
    return (root / rel).read_text(encoding="utf-8")

def write(rel: str, text: str) -> None:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")

# Version bump only. All 1.3.8 fixes remain untouched.
rel = "app/build.gradle.kts"
t = read(rel)
t = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 13', t, count=1)
t = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.3.9"', t, count=1)
write(rel, t)

# FIX 1 — the GROUP screen must render the current Kid's real imported/custom portrait.
# GroupMemberState deliberately stays lightweight; the current Kid is authoritative for its own portrait.
rel = "app/src/main/java/com/braseiro/talesvtt/screens/GroupScreen.kt"
t = read(rel)
old = '''        rowMembers.forEach { member ->
          val stateLabel = when {'''
new = '''        rowMembers.forEach { member ->
          val isCurrentKid = member.name.equals(gameState.kid.name, ignoreCase = true)
          val currentPortraitAsset = if (isCurrentKid) gameState.kid.portraitAssetKey else member.portraitAssetKey
          val currentPortraitImage = if (isCurrentKid) gameState.kid.portraitImageBase64 else null
          val stateLabel = when {'''
if old not in t and "val isCurrentKid = member.name.equals(gameState.kid.name" not in t:
    raise SystemExit("GroupScreen member loop anchor not found")
t = t.replace(old, new, 1)

t = t.replace(
'''            imageRes = portraitDrawable(member.portraitAssetKey),
            state = stateLabel,''',
'''            imageRes = portraitDrawable(currentPortraitAsset),
            imageBase64 = currentPortraitImage,
            state = stateLabel,''',
1
)

t = t.replace(
'''  imageRes: Int,
  state: String?,''',
'''  imageRes: Int,
  imageBase64: String? = null,
  state: String?,''',
1
)

t = t.replace(
'''      imageRes = imageRes,
      size = 128.dp,''',
'''      imageRes = imageRes,
      imageBase64 = imageBase64,
      size = 128.dp,''',
1
)
write(rel, t)

# FIX 2 — Gemini image prompt represents the CURRENT visible scene, not a stale library composition.
rel = "app/src/main/java/com/braseiro/talesvtt/data/SceneImageExchange.kt"
t = read(rel)
start = t.find("  fun buildPrompt(state: GameState): String")
end = t.find("\n  fun prepareShare", start)
if start < 0 or end < 0:
    raise SystemExit("SceneImageExchange.buildPrompt anchor not found")
new_prompt = r'''  fun buildPrompt(state: GameState): String {
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
'''
t = t[:start] + new_prompt + t[end:]
write(rel, t)

# Resume record is stored with the reproducible source transformation.
resume = '''# TALES FROM THE LOOP — RETOMADA ATUAL\n\nAtualizado em 11/09/2026.\n\n## Base preservada\n- Continuidade parte da versão 1.3.8 já compilada no GitHub.\n- Preservadas as correções anteriores: início de aventura, TTS/fallback, opções de ação, parser, texto maior, narrativa sensorial e item icônico coerente.\n- Nenhuma tela aprovada foi redesenhada.\n\n## Bloco 1.3.9\n- GRUPO: o Kid atual agora usa a mesma foto/retrato personalizado exibido em KID, em vez do retrato genérico antigo do membro do grupo.\n- IMAGEM DA CENA: o prompt enviado ao Gemini agora usa o local atual e a narração visível recente como autoridade. A referência de composição da biblioteca foi removida do prompt.\n- O título do Mistério não pode mais deslocar visualmente a cena para outro local.\n\n## Verificações exigidas neste bloco\n- contrato estático das duas correções;\n- testes Gradle da fonte;\n- compilação Android real no GitHub;\n- assinatura/zipalign/validação do APK Android 10;\n- teste final no aparelho continua sendo evidência do usuário, não será fingido por automação.\n\n## Próximo teste no aparelho\n1. Abrir KID, trocar/importar a foto do Kid e conferir a mesma foto em GRUPO.\n2. Em uma cena diante da escola, usar ILUSTRAÇÃO e confirmar que o prompt descreve/resume a cena atual e mantém a escola como local.\n'''
write("TALES_RETOMADA_CURRENT.md", resume)

# Hard failures: do not silently publish a partial transform.
group = read("app/src/main/java/com/braseiro/talesvtt/screens/GroupScreen.kt")
prompt = read("app/src/main/java/com/braseiro/talesvtt/data/SceneImageExchange.kt")
assert "currentPortraitImage" in group
assert "imageBase64 = currentPortraitImage" in group
assert "gameState.kid.portraitImageBase64" in group
assert "NARRAÇÃO VISÍVEL DA CENA ATUAL" in prompt
assert "LOCAL ATUAL OBRIGATÓRIO" in prompt
assert "state.scene.narrative" in prompt
assert "Referência de composição da biblioteca" not in prompt
assert "visualAssetKey" not in prompt
print("TALES 1.3.9 transform: OK")
