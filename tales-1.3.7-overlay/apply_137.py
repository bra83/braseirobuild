from pathlib import Path
import sys

root = Path(sys.argv[1]).resolve()


def replace_once(rel, old, new):
    path = root / rel
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"pattern not found in {rel}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


# Version bump.
replace_once("app/build.gradle.kts", 'versionCode = 9', 'versionCode = 10')
replace_once("app/build.gradle.kts", 'versionName = "1.3.6"', 'versionName = "1.3.7"')

# Always keep three safe player-facing suggestions when the Master omits optional choices.
replace_once(
    "app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt",
    "            val safeChoices = action.choices\n              .filter { it.title.isNotBlank() && it.description.isNotBlank() }\n              .take(3)",
    "            val safeChoices = action.choices\n              .filter { it.title.isNotBlank() && it.description.isNotBlank() }\n              .take(3)\n              .ifEmpty { SceneChoiceDefaults.standard() }",
)

# Accept both legacy nested arrays and object-style choices returned by Gemini.
replace_once(
    "app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt",
    'if (text.isEmpty()) null else MotorAction.Narration(text, parseChoices(a.optJSONArray("choices")))',
    'if (text.isEmpty()) null else MotorAction.Narration(text, parseChoices(a.opt("choices") ?: a.opt("options")))',
)
replace_once(
    "app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt",
    '''  private fun parseChoices(arr: JSONArray?): List<SceneChoice> {
    if (arr == null) return emptyList()
    val result = mutableListOf<SceneChoice>()
    for (i in 0 until minOf(arr.length(), 3)) {
      val row = arr.optJSONArray(i) ?: continue
      if (row.length() < 2) continue
      val title = row.optString(0).trim()
      val description = row.optString(1).trim()
      if (title.isNotEmpty() && description.isNotEmpty()) result += SceneChoice(title, description)
    }
    return result
  }''',
    '''  private fun parseChoices(value: Any?): List<SceneChoice> {
    val arr = value as? JSONArray ?: return emptyList()
    val result = mutableListOf<SceneChoice>()
    for (i in 0 until minOf(arr.length(), 3)) {
      val item = arr.opt(i)
      val choice = when (item) {
        is JSONArray -> {
          val title = item.optString(0).trim()
          val description = item.optString(1).trim()
          if (title.isNotEmpty() && description.isNotEmpty()) SceneChoice(title, description) else null
        }
        is JSONObject -> {
          val title = item.optString("title", item.optString("titulo", item.optString("label"))).trim()
          val description = item.optString("description", item.optString("descricao", item.optString("text"))).trim()
          if (title.isNotEmpty() && description.isNotEmpty()) SceneChoice(title, description) else null
        }
        else -> null
      }
      if (choice != null) result += choice
    }
    return result
  }''',
)

# CENA and LOCAL display safe suggestions even before/without a provider choice packet.
for rel in (
    "app/src/main/java/com/braseiro/talesvtt/screens/SceneScreen.kt",
    "app/src/main/java/com/braseiro/talesvtt/screens/LocationScreen.kt",
):
    path = root / rel
    text = path.read_text(encoding="utf-8")
    if "import com.braseiro.talesvtt.game.SceneChoiceDefaults" not in text:
        marker = "import com.braseiro.talesvtt.game.GameState\n"
        if marker not in text:
            raise SystemExit(f"import marker not found in {rel}")
        text = text.replace(marker, marker + "import com.braseiro.talesvtt.game.SceneChoiceDefaults\n", 1)
    old = "scene.choices.take(3).forEachIndexed { index, choice ->"
    if old not in text:
        raise SystemExit(f"choice loop not found in {rel}")
    text = text.replace(old, "scene.choices.take(3).ifEmpty { SceneChoiceDefaults.standard() }.forEachIndexed { index, choice ->", 1)
    path.write_text(text, encoding="utf-8")

# The deterministic first scene starts with suggestions immediately.
replace_once(
    "app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt",
    '            "O Mistério começou, mas nada além do que o Kid percebeu foi revelado."\n        ),\n        choices = emptyList()',
    '            "O Mistério começou, mas nada além do que o Kid percebeu foi revelado."\n        ),\n        choices = SceneChoiceDefaults.standard()',
)

print("TALES 1.3.7 source transformations applied")
