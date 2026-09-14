from pathlib import Path
import re, sys

root = Path(sys.argv[1])
repo = Path(__file__).resolve().parent

# Preserve every proven correction through 1.3.12.
ns = {"__name__": "__main__", "__file__": str(repo / "tales-1.3.12-transform.py")}
code = (repo / "tales-1.3.12-transform.py").read_text(encoding="utf-8")
old_argv = sys.argv[:]
sys.argv = [ns["__file__"], str(root)]
try:
    exec(compile(code, ns["__file__"], "exec"), ns)
finally:
    sys.argv = old_argv


def read(rel): return (root / rel).read_text(encoding="utf-8")
def write(rel, text):
    p = root / rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text, encoding="utf-8")
def replace_once(text, old, new, label):
    if old not in text: raise SystemExit(f"anchor not found: {label}")
    return text.replace(old, new, 1)

# Version 1.3.13 / versionCode 17.
gradle_rel = "app/build.gradle.kts"
g = read(gradle_rel)
g = re.sub(r'versionCode\s*=\s*\d+', 'versionCode = 17', g, count=1)
g = re.sub(r'versionName\s*=\s*"[^"]+"', 'versionName = "1.3.13"', g, count=1)
write(gradle_rel, g)

# Local mechanical authority. RAG/manual search is never a mechanics authority.
canonical = r'''package com.braseiro.talesvtt.game

/**
 * Local deterministic Tales/Kids mechanical authority.
 * This object contains the contracts the engine may enforce without PDFs, RAG or an LLM.
 * Unknown/unsupported edge cases stay blocked rather than borrowing Flood rules.
 */
data class TalesTypeRule(
  val id: String,
  val label: String,
  val keySkills: Set<String>
)

object CanonicalTalesRules {
  const val PROFILE = "tales_kid"
  const val MIN_AGE = 10
  const val MAX_CREATION_AGE = 15
  const val ATTRIBUTE_MIN = 1
  const val ATTRIBUTE_MAX = 5
  const val CREATION_SKILL_POINTS = 10
  const val CREATION_KEY_SKILL_MAX = 3
  const val CREATION_OTHER_SKILL_MAX = 1
  const val SKILL_MAX = 5
  const val XP_SKILL_COST = 5
  const val XP_STORAGE_MAX = 10
  const val HELP_BONUS = 1
  const val ICONIC_ITEM_BONUS = 2
  const val MYSTERY_ITEM_MIN_BONUS = 1
  const val MYSTERY_ITEM_MAX_BONUS = 3

  val attributes: List<String> = listOf("CORPO", "TECNOLOGIA", "CORAÇÃO", "MENTE")
  val skillToAttribute: Map<String, String> = linkedMapOf(
    "Furtividade" to "CORPO",
    "Força" to "CORPO",
    "Movimentação" to "CORPO",
    "Mecânica" to "TECNOLOGIA",
    "Programação" to "TECNOLOGIA",
    "Cálculo" to "TECNOLOGIA",
    "Contatos" to "CORAÇÃO",
    "Charme" to "CORAÇÃO",
    "Liderança" to "CORAÇÃO",
    "Investigação" to "MENTE",
    "Conhecimento" to "MENTE",
    "Empatia" to "MENTE"
  )

  val types: List<TalesTypeRule> = listOf(
    TalesTypeRule("TAL-TYPE-001", "Livro", setOf("Cálculo", "Investigação", "Conhecimento")),
    TalesTypeRule("TAL-TYPE-002", "Informático", setOf("Cálculo", "Programação", "Conhecimento")),
    TalesTypeRule("TAL-TYPE-003", "Caipira", setOf("Força", "Movimentação", "Mecânica")),
    TalesTypeRule("TAL-TYPE-004", "Jocão", setOf("Força", "Movimentação", "Contatos")),
    TalesTypeRule("TAL-TYPE-005", "Criança Popular", setOf("Contatos", "Charme", "Liderança")),
    TalesTypeRule("TAL-TYPE-006", "Rocker", setOf("Movimentação", "Charme", "Empatia")),
    TalesTypeRule("TAL-TYPE-007", "Encrenqueiro", setOf("Força", "Furtividade", "Liderança")),
    TalesTypeRule("TAL-TYPE-008", "Estranho", setOf("Furtividade", "Investigação", "Empatia"))
  )

  val mildConditions: Set<Condition> = setOf(Condition.UPSET, Condition.SCARED, Condition.EXHAUSTED, Condition.INJURED)

  fun maxLuck(age: Int): Int = (15 - age).coerceAtLeast(0)
  fun validMysteryItemBonus(bonus: Int): Boolean = bonus in MYSTERY_ITEM_MIN_BONUS..MYSTERY_ITEM_MAX_BONUS
  fun threatTarget(kids: Int, level: ThreatLevel): Int = kids * level.multiplier

  fun validItem(item: KidItemState): Boolean = when (item.kind) {
    KidItemKind.ORDINARY -> item.name.isNotBlank() && item.bonus == 0
    KidItemKind.MYSTERY -> item.name.isNotBlank() && validMysteryItemBonus(item.bonus)
  }

  fun localMechanicalContract(): String = """
PROFILE=TALES/KIDS. LOCAL ENGINE IS AUTHORITATIVE FOR MECHANICS.
Test = Attribute + canonical Skill + one approved Item bonus + at most one Help die - one die per mild Condition.
Each d6 showing 6 = one success. Required successes are 1..3 when a standard Trouble is requested.
Luck: max 15-age, after failure, one spend per roll, reroll non-6 dice.
Push: once per roll, mark a Condition first, reroll non-6 dice; may also follow success for permitted Bonus Effects.
Pride: +1 automatic success, never +1 die.
Help: one plausible helper, exactly +1 die, helper shares failure consequences.
Iconic Item: exactly +2 dice when applicable and GM-approved.
Mystery Item: only its stored fixed +1..+3 bonus when applicable; ordinary objects have no automatic bonus.
Conditions: Chateado, Assustado, Exausto, Machucado; each -1 die. Broken auto-fails until valid recovery.
NPCs never roll. No HP, initiative, rounds or tactical combat subsystem.
Extended Trouble target: Normal=2x Kids, Difficult=3x Kids, Almost Impossible=4x Kids; one scene/roll per Kid; compromise only after all scenes.
XP: five questions; 5 XP buys +1 Skill; Skill max 5; stored XP max 10; spending only outside active play.
Tales pool <=0 for a non-Broken Kid is TAL-EDGE-001 and remains BLOCKED/MISSING_EVIDENCE. Never import Flood minimum-one-die.
Flood-only Shame, Scars, Friction and Flood minimum-die rule are forbidden in this profile.
""".trimIndent()
}
'''
write("app/src/main/java/com/braseiro/talesvtt/game/CanonicalTalesRules.kt", canonical)

# Core: local catalog, real inventory state, deterministic add/remove, single-condition recovery.
core_rel = "app/src/main/java/com/braseiro/talesvtt/game/TalesCore.kt"
t = read(core_rel)
start = t.index("object TalesSkillCatalog {")
end = t.index("\n}\n\ndata class KidRelationship", start) + 3
t = t[:start] + '''object TalesSkillCatalog {\n  val skillToAttribute: Map<String, String> = CanonicalTalesRules.skillToAttribute\n\n  fun canonicalSkill(name: String): String? = skillToAttribute.keys.firstOrNull { it.equals(name.trim(), ignoreCase = true) }\n  fun attributeFor(skill: String): String? = canonicalSkill(skill)?.let(skillToAttribute::get)\n}\n''' + t[end:]

t = replace_once(t,
'''data class KidRelationship(\n  val name: String,\n  val description: String\n)\n\ndata class KidState(''',
'''data class KidRelationship(\n  val name: String,\n  val description: String\n)\n\nenum class KidItemKind { ORDINARY, MYSTERY }\n\ndata class KidItemState(\n  val name: String,\n  val description: String = "",\n  val bonus: Int = 0,\n  val kind: KidItemKind = KidItemKind.ORDINARY\n)\n\ndata class KidState(''', "KidItemState")

t = replace_once(t,
'''  val iconicItemName: String = "Chave inglesa antiga",\n  val relationships: List<KidRelationship> = listOf(''',
'''  val iconicItemName: String = "Chave inglesa antiga",\n  val favoriteSong: String = "",\n  val items: List<KidItemState> = emptyList(),\n  val relationships: List<KidRelationship> = listOf(''', "KidState items")

t = t.replace('val maxLuck: Int get() = 15 - age', 'val maxLuck: Int get() = CanonicalTalesRules.maxLuck(age)', 1)
t = t.replace('(if (test.helperKid != null) 1 else 0)', '(if (test.helperKid != null) CanonicalTalesRules.HELP_BONUS else 0)', 1)

t = replace_once(t,
'''  data class ApplyApprovedItem(val bonus: Int) : GameEvent\n  data class RollResult(val dice: List<Int>) : GameEvent''',
'''  data class ApplyApprovedItem(val bonus: Int) : GameEvent\n  data class AddItem(val item: KidItemState) : GameEvent\n  data class RemoveItem(val name: String) : GameEvent\n  data class HealCondition(val condition: Condition) : GameEvent\n  data class RollResult(val dice: List<Int>) : GameEvent''', "GameEvent inventory")

# Reducer inventory cases directly after item approval.
anchor = '''      is GameEvent.ApplyApprovedItem -> {\n        val test = state.activeTest ?: return fail("Não há teste ativo.")\n        if (state.testResolution != null) fail("Item deve ser aprovado antes da rolagem.")\n        else if (event.bonus !in 0..3) fail("Bônus de item inválido.")\n        else ok(state.copy(activeTest = test.copy(approvedItemBonus = event.bonus)))\n      }\n'''
if anchor not in t: raise SystemExit("ApplyApprovedItem reducer anchor not found")
insert = anchor + '''\n      is GameEvent.AddItem -> {\n        val item = event.item.copy(name = event.item.name.trim(), description = event.item.description.trim())\n        when {\n          !CanonicalTalesRules.validItem(item) -> fail("Item viola o contrato Tales/Kids.")\n          state.kid.items.any { it.name.equals(item.name, ignoreCase = true) } -> fail("O Kid já possui este Item.")\n          else -> ok(state.copy(kid = state.kid.copy(items = state.kid.items + item)))\n        }\n      }\n\n      is GameEvent.RemoveItem -> {\n        val item = state.kid.items.firstOrNull { it.name.equals(event.name.trim(), ignoreCase = true) }\n        if (item == null) fail("Item não pertence ao Kid.")\n        else ok(state.copy(kid = state.kid.copy(items = state.kid.items - item)))\n      }\n\n      is GameEvent.HealCondition -> {\n        when {\n          state.kid.broken -> fail("Recuperação por conforto não cura um Kid Quebrado.")\n          event.condition !in state.kid.conditions -> fail("Condição não está marcada.")\n          else -> ok(state.copy(kid = state.kid.copy(conditions = state.kid.conditions - event.condition)))\n        }\n      }\n'''
t = t.replace(anchor, insert, 1)

# Mystery items do not silently survive a Mystery boundary.
t = t.replace(
'''            prideAvailable = true\n          ),\n          sessionXpAwarded = true,\n          endingMode = EndingMode.MYSTERY''',
'''            prideAvailable = true,\n            items = state.kid.items.filterNot { it.kind == KidItemKind.MYSTERY }\n          ),\n          sessionXpAwarded = true,\n          endingMode = EndingMode.MYSTERY''', 1)
t = t.replace(
'''              prideAvailable = true\n            ),\n            sessionXpAwarded = true''',
'''              prideAvailable = true,\n              items = if (state.endingMode == EndingMode.MYSTERY) state.kid.items.filterNot { it.kind == KidItemKind.MYSTERY } else state.kid.items\n            ),\n            sessionXpAwarded = true''', 1)
write(core_rel, t)

# Random Kid creation uses the locally embedded type contracts and validates inventory state.
rel = "app/src/main/java/com/braseiro/talesvtt/game/RandomKidFactory.kt"
t = read(rel)
t = t.replace('TypeSpec("Caxias", "tales_type_bookworm"', 'TypeSpec("Livro", "tales_type_bookworm"', 1)
t = t.replace('TypeSpec("Esportista", "tales_type_jock"', 'TypeSpec("Jocão", "tales_type_jock"', 1)
t = t.replace('TypeSpec("Popular", "tales_type_popular"', 'TypeSpec("Criança Popular", "tales_type_popular"', 1)
t = t.replace('luck = 15 - age,', 'luck = CanonicalTalesRules.maxLuck(age),', 1)
t = t.replace('if (kid.luck !in 0..(15 - kid.age)) return false', 'if (kid.luck !in 0..CanonicalTalesRules.maxLuck(kid.age)) return false\n    if (kid.items.any { !CanonicalTalesRules.validItem(it) }) return false', 1)
t = t.replace('if (kid.luck != 15 - kid.age) return false', 'if (kid.luck != CanonicalTalesRules.maxLuck(kid.age)) return false\n    if (kid.items.any { !CanonicalTalesRules.validItem(it) }) return false', 1)
write(rel, t)

# Snapshot v10: inventory + favorite song survive save/load; old saves migrate with empty inventory.
rel = "app/src/main/java/com/braseiro/talesvtt/game/GameSnapshotCodec.kt"
t = read(rel)
t = t.replace('private const val VERSION = 9', 'private const val VERSION = 10', 1)
t = replace_once(t,
'''    line("kid.iconicItem", enc(state.kid.iconicItemName))\n    line("kid.relationships",''',
'''    line("kid.iconicItem", enc(state.kid.iconicItemName))\n    line("kid.favoriteSong", enc(state.kid.favoriteSong))\n    line("kid.items", state.kid.items.joinToString("|") { item ->\n      listOf(enc(item.name), enc(item.description), item.bonus.toString(), item.kind.name).joinToString("~")\n    })\n    line("kid.relationships",''', "snapshot encode items")

# Insert decoded item list before KidState construction.
anchor = '''    val name = decoded("kid.name").ifBlank { defaults.name }\n    val luck = int("kid.luck", 3)'''
insert = '''    val kidItems = if (version >= 10) map["kid.items"].orEmpty().split('|')\n      .filter { it.isNotBlank() }\n      .mapNotNull { raw ->\n        val parts = raw.split('~', limit = 4)\n        if (parts.size != 4) null else {\n          val kind = runCatching { KidItemKind.valueOf(parts[3]) }.getOrNull() ?: return@mapNotNull null\n          val item = KidItemState(dec(parts[0]), dec(parts[1]), parts[2].toIntOrNull() ?: 0, kind)\n          item.takeIf(CanonicalTalesRules::validItem)\n        }\n      } else emptyList()\n\n    val name = decoded("kid.name").ifBlank { defaults.name }\n    val luck = int("kid.luck", 3)'''
t = replace_once(t, anchor, insert, "snapshot decode items")
t = replace_once(t,
'''      iconicItemName = if (version >= 3) decoded("kid.iconicItem").ifBlank { defaults.iconicItemName } else defaults.iconicItemName,\n      relationships = kidRelationships,''',
'''      iconicItemName = if (version >= 3) decoded("kid.iconicItem").ifBlank { defaults.iconicItemName } else defaults.iconicItemName,\n      favoriteSong = if (version >= 10) decoded("kid.favoriteSong") else defaults.favoriteSong,\n      items = kidItems,\n      relationships = kidRelationships,''', "snapshot KidState args")
write(rel, t)

# Portable Kid JSON v3 carries the same deterministic inventory state.
rel = "app/src/main/java/com/braseiro/talesvtt/data/KidJsonCodec.kt"
t = read(rel)
t = t.replace('import com.braseiro.talesvtt.game.KidState', 'import com.braseiro.talesvtt.game.KidState\nimport com.braseiro.talesvtt.game.KidItemKind\nimport com.braseiro.talesvtt.game.KidItemState\nimport com.braseiro.talesvtt.game.CanonicalTalesRules', 1)
t = t.replace('private const val VERSION = 2', 'private const val VERSION = 3', 1)
t = replace_once(t,
'''      .put("iconic_item", kid.iconicItemName)\n      .put("age", kid.age)''',
'''      .put("iconic_item", kid.iconicItemName)\n      .put("favorite_song", kid.favoriteSong)\n      .put("items", JSONArray(kid.items.map { item ->\n        JSONObject().put("name", item.name).put("description", item.description).put("bonus", item.bonus).put("kind", item.kind.name.lowercase())\n      }))\n      .put("age", kid.age)''', "kid json encode")
anchor = '''    val portraitImageBase64 = j.optString("portrait_image_base64", "").takeIf { it.isNotBlank() && it != "null" }'''
insert = '''    val items = buildList {\n      val a = if (root.optInt("version") >= 3) j.optJSONArray("items") ?: JSONArray() else JSONArray()\n      for (i in 0 until a.length()) {\n        val item = a.optJSONObject(i) ?: continue\n        val kind = when (item.optString("kind").lowercase()) {\n          "mystery" -> KidItemKind.MYSTERY\n          else -> KidItemKind.ORDINARY\n        }\n        val parsed = KidItemState(item.optString("name").trim(), item.optString("description").trim(), item.optInt("bonus", 0), kind)\n        require(CanonicalTalesRules.validItem(parsed)) { "Item inválido na ficha importada." }\n        add(parsed)\n      }\n    }\n    val portraitImageBase64 = j.optString("portrait_image_base64", "").takeIf { it.isNotBlank() && it != "null" }'''
t = replace_once(t, anchor, insert, "kid json decode items")
t = replace_once(t,
'''      iconicItemName = j.optString("iconic_item"),\n      relationships = relationships,''',
'''      iconicItemName = j.optString("iconic_item"),\n      favoriteSong = j.optString("favorite_song"),\n      items = items,\n      relationships = relationships,''', "kid json KidState args")
write(rel, t)

# GM/firewall: items are actual state, never invented bonuses.
rel = "app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraContract.kt"
t = read(rel)
t = replace_once(t,
'''  data object ApproveIconicItem : MotorAction\n  data class ApplyCondition''',
'''  data object ApproveIconicItem : MotorAction\n  data class ApproveMysteryItem(val itemName: String, val bonus: Int) : MotorAction\n  data class GiveItem(val item: KidItemState) : MotorAction\n  data class RemoveItem(val itemName: String) : MotorAction\n  data class ApplyCondition''', "Motor item actions")

anchor = '''        MotorAction.ApproveIconicItem -> {\n          val test = state.activeTest\n          if (test == null) rejected += "ação[$index] iconic_item_approve: não há teste ativo."\n          else if (state.testResolution != null) rejected += "ação[$index] iconic_item_approve: item deve ser aprovado antes da rolagem."\n          else if (test.approvedItemBonus != 0) rejected += "ação[$index] iconic_item_approve: já existe item aplicado ao teste."\n          else if (reduce(GameEvent.ApplyApprovedItem(2), "iconic_item_approve")) raiseRoute(MotorRouteHint.TROUBLE)\n        }\n'''
if anchor not in t: raise SystemExit("Motor iconic anchor missing")
insert = anchor + '''\n        is MotorAction.ApproveMysteryItem -> {\n          val test = state.activeTest\n          val item = state.kid.items.firstOrNull { it.name.equals(action.itemName.trim(), ignoreCase = true) }\n          when {\n            test == null -> rejected += "ação[$index] mystery_item_approve: não há teste ativo."\n            state.testResolution != null -> rejected += "ação[$index] mystery_item_approve: item deve ser aprovado antes da rolagem."\n            test.approvedItemBonus != 0 -> rejected += "ação[$index] mystery_item_approve: já existe item aplicado ao teste."\n            item == null -> rejected += "ação[$index] mystery_item_approve: item não pertence ao Kid."\n            item.kind != KidItemKind.MYSTERY -> rejected += "ação[$index] mystery_item_approve: objeto comum não possui bônus automático."\n            item.bonus != action.bonus || !CanonicalTalesRules.validMysteryItemBonus(action.bonus) -> rejected += "ação[$index] mystery_item_approve: bônus não corresponde ao Item real."\n            else -> if (reduce(GameEvent.ApplyApprovedItem(item.bonus), "mystery_item_approve")) raiseRoute(MotorRouteHint.TROUBLE)\n          }\n        }\n\n        is MotorAction.GiveItem -> {\n          if (reduce(GameEvent.AddItem(action.item), "item_add")) raiseRoute(MotorRouteHint.SCENE)\n        }\n\n        is MotorAction.RemoveItem -> {\n          if (reduce(GameEvent.RemoveItem(action.itemName), "item_remove")) raiseRoute(MotorRouteHint.SCENE)\n        }\n'''
t = t.replace(anchor, insert, 1)
write(rel, t)

# Wire parser for actual inventory actions.
rel = "app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt"
t = read(rel)
t = replace_once(t,
'''    "help_approve" -> a.optString("helperKid").takeIf { it.isNotBlank() }?.let(MotorAction::ApproveHelp)\n    "iconic_item_approve" -> MotorAction.ApproveIconicItem\n\n    "condition" -> {''',
'''    "help_approve" -> a.optString("helperKid").takeIf { it.isNotBlank() }?.let(MotorAction::ApproveHelp)\n    "iconic_item_approve" -> MotorAction.ApproveIconicItem\n    "mystery_item_approve", "item_approve" -> {\n      val item = a.optString("item", a.optString("name")).trim()\n      val bonus = a.optInt("bonus", -1)\n      if (item.isBlank() || bonus !in 1..3) null else MotorAction.ApproveMysteryItem(item, bonus)\n    }\n    "item_add" -> {\n      val item = a.optJSONObject("item") ?: a\n      val name = item.optString("name").trim()\n      val description = item.optString("description").trim()\n      val bonus = item.optInt("bonus", 0)\n      val kind = when (item.optString("kind").lowercase()) {\n        "mystery" -> KidItemKind.MYSTERY\n        else -> KidItemKind.ORDINARY\n      }\n      if (name.isBlank()) null else MotorAction.GiveItem(KidItemState(name, description, bonus, kind))\n    }\n    "item_remove" -> a.optString("item", a.optString("name")).takeIf { it.isNotBlank() }?.let(MotorAction::RemoveItem)\n\n    "condition" -> {''', "wire item actions")
write(rel, t)

# Transport: publish local mechanics to the GM and demote RAG to optional lore/content only.
rel = "app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt"
t = read(rel)
t = replace_once(t,
'''        .put("iconicItem", state.kid.iconicItemName)\n        .put("attributes", JSONObject(state.kid.attributes))''',
'''        .put("iconicItem", state.kid.iconicItemName)\n        .put("items", JSONArray(state.kid.items.map { item ->\n          JSONObject().put("name", item.name).put("description", item.description).put("bonus", item.bonus).put("kind", item.kind.name.lowercase())\n        }))\n        .put("attributes", JSONObject(state.kid.attributes))''', "transport items state")
t = t.replace('.put("rag_context", ragContext)\n      .put("rag_instruction", "Os trechos RAG são evidência local dos manuais. Use apenas o que eles sustentam; não invente regra ausente e não misture outros sistemas ao perfil Tales/Kids.")',
'''.put("mechanical_contract", CanonicalTalesRules.localMechanicalContract())\n      .put("reference_context", ragContext)\n      .put("reference_instruction", "Contexto opcional somente para ambientação, nomes, lore e conteúdo narrativo. NUNCA use estes trechos para decidir ou alterar regras, pools, bônus, condições, XP, recuperação, Itens ou Problema Estendido. A autoridade mecânica é mechanical_contract + engine local; se houver conflito, ignore o contexto de referência.")''', 1)
t = t.replace('Se player_intent.type=item_proposal, use iconic_item_approve apenas quando o Item Icônico for claramente útil no teste ativo; nunca invente bônus.',
'Se player_intent.type=item_proposal, identifique o Item exato já presente em player_visible_state.kid. Para o Item Icônico use iconic_item_approve (+2 fixo). Para um Mystery Item existente use mystery_item_approve com o nome e exatamente o bônus armazenado (+1..+3). Objeto comum nunca recebe bônus automático.', 1)
t = t.replace('- Sorte, Forçar e Orgulho são resolvidos pelo engine. Ajuda e Item precisam primeiro de sua validação ficcional: help_approve aplica exatamente +1 dado; iconic_item_approve aplica exatamente +2 dados ao Item Icônico. Você nunca inventa outros bônus nem gasta recursos do jogador.',
'- Sorte, Forçar e Orgulho são resolvidos pelo engine. Ajuda e Item precisam primeiro de sua validação ficcional: help_approve aplica exatamente +1 dado; iconic_item_approve aplica exatamente +2 dados ao Item Icônico; mystery_item_approve só aceita o bônus +1..+3 já armazenado no Item real. Objeto comum tem bônus 0. Você nunca inventa outros bônus nem gasta recursos do jogador.', 1)
t = t.replace('- iconic_item_approve\n- condition', '- iconic_item_approve\n- mystery_item_approve {item,bonus}\n- item_add {item:{name,description,bonus,kind: ordinary|mystery}}\n- item_remove {item}\n- condition', 1)
t = t.replace('REGRAS INEGOCIÁVEIS:\n', 'REGRAS INEGOCIÁVEIS:\n- O bloco mechanical_contract recebido no pedido é a autoridade mecânica local. reference_context/RAG é opcional e nunca pode alterar regra.\n', 1)
write(rel, t)

# ViewModel exposes a real Mystery Item proposal, in addition to Iconic Item.
rel = "app/src/main/java/com/braseiro/talesvtt/game/GameViewModel.kt"
t = read(rel)
anchor = '''  fun submitIconicItemProposal() {\n    val test = _state.value.activeTest ?: return\n    if (_state.value.testResolution != null || test.approvedItemBonus != 0) return\n    requestMotor(MotorIntent(type = "item_proposal", text = "Proponho usar meu Item Icônico: ${_state.value.kid.iconicItemName}."))\n  }\n'''
insert = anchor + '''\n  fun submitMysteryItemProposal(itemName: String) {\n    val test = _state.value.activeTest ?: return\n    if (_state.value.testResolution != null || test.approvedItemBonus != 0) return\n    val item = _state.value.kid.items.firstOrNull { it.name.equals(itemName.trim(), ignoreCase = true) } ?: return\n    if (item.kind != KidItemKind.MYSTERY || !CanonicalTalesRules.validMysteryItemBonus(item.bonus)) {\n      _dataStatus.value = "Este objeto não possui bônus mecânico de Item do Mistério."\n      return\n    }\n    requestMotor(MotorIntent(type = "item_proposal", text = "Proponho usar o Item do Mistério '${item.name}', cujo bônus registrado no estado é +${item.bonus}."))\n  }\n'''
t = replace_once(t, anchor, insert, "ViewModel mystery item proposal")
write(rel, t)

# Navigation forwards Mystery Item proposals to the backpack.
rel = "app/src/main/java/com/braseiro/talesvtt/navigation/AppNavGraph.kt"
t = read(rel)
t = replace_once(t,
'''        gameState = gameState,\n        onUseIconicItem = gameViewModel::submitIconicItemProposal\n      )''',
'''        gameState = gameState,\n        onUseIconicItem = gameViewModel::submitIconicItemProposal,\n        onUseItem = gameViewModel::submitMysteryItemProposal\n      )''', "nav backpack callback")
write(rel, t)

# Backpack becomes state-driven. Remove four fake fixed objects.
rel = "app/src/main/java/com/braseiro/talesvtt/screens/BackpackScreen.kt"
t = read(rel)
t = t.replace('import com.braseiro.talesvtt.components.AssetImageSlot\n', '')
t = t.replace('import com.braseiro.talesvtt.game.GameState\n', 'import com.braseiro.talesvtt.game.GameState\nimport com.braseiro.talesvtt.game.KidItemKind\n')
t = replace_once(t,
'''  gameState: GameState,\n  onUseIconicItem: () -> Unit,\n  modifier: Modifier = Modifier''',
'''  gameState: GameState,\n  onUseIconicItem: () -> Unit,\n  onUseItem: (String) -> Unit,\n  modifier: Modifier = Modifier''', "Backpack signature")
start = t.index('    Row(\n      modifier = Modifier.fillMaxWidth(),\n      horizontalArrangement = Arrangement.SpaceBetween\n    ) {', t.index('SectionHeader(title = "ITENS POSSUÍDOS"'))
end = t.index('\n    Spacer(modifier = Modifier.height(Dimens.spacing16))', start)
replacement = '''    if (gameState.kid.items.isEmpty()) {\n      AppPanel {\n        Text(\n          text = "Nenhum objeto comum ou Item do Mistério está registrado nesta aventura.",\n          style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary)\n        )\n      }\n    } else {\n      gameState.kid.items.forEach { item ->\n        AppPanel(borderColor = if (item.kind == KidItemKind.MYSTERY) AppTheme.colors.accentAmber.copy(alpha = 0.45f) else AppTheme.colors.accentCyan.copy(alpha = 0.30f)) {\n          Row(verticalAlignment = Alignment.CenterVertically) {\n            Image(\n              painter = painterResource(inventoryItemDrawable(item.name)),\n              contentDescription = item.name,\n              modifier = Modifier.size(58.dp),\n              contentScale = ContentScale.Fit\n            )\n            Spacer(modifier = Modifier.width(10.dp))\n            Column(modifier = Modifier.weight(1f)) {\n              Text(item.name.uppercase(), style = AppTheme.typography.cardTitle)\n              if (item.description.isNotBlank()) Text(item.description, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))\n              Text(\n                text = if (item.kind == KidItemKind.MYSTERY) "+${item.bonus} dados quando este Item específico for claramente útil e aprovado pelo Mestre." else "Objeto comum • sem bônus automático.",\n                style = AppTheme.typography.bodySmall.copy(color = if (item.kind == KidItemKind.MYSTERY) AppTheme.colors.accentAmber else AppTheme.colors.textMuted)\n              )\n            }\n          }\n          if (item.kind == KidItemKind.MYSTERY && gameState.activeTest != null && gameState.testResolution == null && gameState.activeTest.approvedItemBonus == 0) {\n            Spacer(modifier = Modifier.height(8.dp))\n            com.braseiro.talesvtt.components.SecondaryButton(\n              text = "PROPOR ESTE ITEM NO TESTE",\n              onClick = { onUseItem(item.name) },\n              modifier = Modifier.fillMaxWidth()\n            )\n          }\n        }\n        Spacer(modifier = Modifier.height(6.dp))\n      }\n    }\n'''
t = t[:start] + replacement + t[end:]
# Append conservative visual resolver; the name controls only art, never mechanics.
t += '''\n\nprivate fun inventoryItemDrawable(name: String): Int {\n  val n = name.lowercase()\n  return when {\n    "lanterna" in n -> R.drawable.item_flashlight\n    "cassete" in n || "fita" in n || "gravador" in n -> R.drawable.item_audio_cassette\n    "binócul" in n -> R.drawable.item_binoculars\n    "caderno" in n || "mapa" in n || "anota" in n -> R.drawable.item_notebook\n    else -> R.drawable.item_notebook\n  }\n}\n'''
write(rel, t)

# Tests: local rules, inventory, persistence, no-RAG mechanics, dynamic locations.
test_dir = root / "app/src/test/java/com/braseiro/talesvtt/game"
test_dir.mkdir(parents=True, exist_ok=True)
(test_dir / "CanonicalRulesV1313Test.kt").write_text(r'''package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CanonicalRulesV1313Test {
  @Test fun canonicalCatalogIsLocalAndCompleteForCoreCharacterMath() {
    assertEquals(setOf("CORPO","TECNOLOGIA","CORAÇÃO","MENTE"), CanonicalTalesRules.attributes.toSet())
    assertEquals(12, CanonicalTalesRules.skillToAttribute.size)
    assertEquals(8, CanonicalTalesRules.types.size)
    assertEquals(3, CanonicalTalesRules.maxLuck(12))
    assertEquals(1, CanonicalTalesRules.HELP_BONUS)
    assertEquals(2, CanonicalTalesRules.ICONIC_ITEM_BONUS)
    assertEquals(5, CanonicalTalesRules.XP_SKILL_COST)
    assertEquals(10, CanonicalTalesRules.XP_STORAGE_MAX)
  }

  @Test fun eachMildConditionRemovesExactlyOneDieAndBrokenAutoFails() {
    val req = TestRequest("x","CORPO",3,"FORÇA",2,1)
    val base = GameState(kid = KidState(conditions=emptySet(), broken=false), activeTest=req)
    assertEquals(5, base.finalPool())
    val conditioned = base.copy(kid=base.kid.copy(conditions=setOf(Condition.UPSET, Condition.SCARED)))
    assertEquals(3, conditioned.finalPool())
    assertEquals(0, conditioned.copy(kid=conditioned.kid.copy(broken=true)).finalPool())
  }

  @Test fun zeroPoolDoesNotBorrowFloodMinimumDie() {
    val state=GameState(kid=KidState(conditions=setOf(Condition.UPSET,Condition.SCARED,Condition.EXHAUSTED,Condition.INJURED)), activeTest=TestRequest("x","CORPO",2,null,0,1))
    val result=TalesReducer.reduce(state, GameEvent.RollResult(emptyList()))
    assertTrue(result.lastError.orEmpty().contains("TAL-EDGE-001"))
  }

  @Test fun actualMysteryItemUsesOnlyItsStoredBonus() {
    val item=KidItemState("Detector improvisado","Montado durante o Mistério",3,KidItemKind.MYSTERY)
    var state=GameState(kid=KidState(items=listOf(item)), activeTest=TestRequest("x","TECNOLOGIA",3,"MECÂNICA",2,1))
    val good=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.ApproveMysteryItem(item.name,3))))
    assertEquals(3,good.gameState.activeTest?.approvedItemBonus)
    assertTrue(good.rejected.isEmpty())
    val bad=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.ApproveMysteryItem(item.name,2))))
    assertEquals(0,bad.gameState.activeTest?.approvedItemBonus)
    assertTrue(bad.rejected.isNotEmpty())
  }

  @Test fun ordinaryObjectNeverGetsAutomaticBonus() {
    val item=KidItemState("Caderno","anotações",0,KidItemKind.ORDINARY)
    val state=GameState(kid=KidState(items=listOf(item)), activeTest=TestRequest("x","MENTE",3,"INVESTIGAÇÃO",2,1))
    val result=MotorBarbaraFirewall.apply(state,GmPrivateState(),MotorPacket(actions=listOf(MotorAction.ApproveMysteryItem(item.name,1))))
    assertEquals(0,result.gameState.activeTest?.approvedItemBonus)
    assertTrue(result.rejected.isNotEmpty())
  }

  @Test fun itemAcquisitionAndRemovalAreEngineOwned() {
    val mystery=KidItemState("Chave codificada","abre o painel",2,KidItemKind.MYSTERY)
    val added=MotorBarbaraFirewall.apply(GameState(),GmPrivateState(),MotorPacket(actions=listOf(MotorAction.GiveItem(mystery))))
    assertEquals(mystery,added.gameState.kid.items.single())
    val removed=MotorBarbaraFirewall.apply(added.gameState,added.privateState,MotorPacket(actions=listOf(MotorAction.RemoveItem(mystery.name))))
    assertTrue(removed.gameState.kid.items.isEmpty())
  }

  @Test fun snapshotRoundTripPreservesInventoryAndFavoriteSong() {
    val item=KidItemState("Mapa transparente","marcado em vermelho",1,KidItemKind.MYSTERY)
    val state=GameState(kid=KidState(favoriteSong="Take On Me",items=listOf(item)))
    val decoded=GameSnapshotCodec.decode(GameSnapshotCodec.encode(state))!!
    assertEquals("Take On Me",decoded.kid.favoriteSong)
    assertEquals(item,decoded.kid.items.single())
  }

  @Test fun mysteryBoundaryDropsMysteryItemsButKeepsOrdinaryObjects() {
    val ordinary=KidItemState("Caderno","",0,KidItemKind.ORDINARY)
    val mystery=KidItemState("Detector","",2,KidItemKind.MYSTERY)
    val state=GameState(kid=KidState(items=listOf(ordinary,mystery)),endingMode=EndingMode.MYSTERY,xpAnswers=listOf(true,false,false,false,false))
    val ended=TalesReducer.reduce(state,GameEvent.ConfirmMysteryEnd)
    assertNull(ended.lastError)
    assertEquals(listOf(ordinary),ended.kid.items)
  }

  @Test fun locationsRemainDynamicVisibleStateNotHardcodedRuleCatalog() {
    var state=GameState(revealed=RevealedState(clues=emptySet(),locations=emptySet(),connections=emptySet()))
    state=TalesReducer.reduce(state,GameEvent.RevealLocation("Estação meteorológica abandonada"))
    assertTrue("Estação meteorológica abandonada" in state.revealed.locations)
  }

  @Test fun transportExplicitlySeparatesMechanicalAuthorityFromOptionalReferenceContext() {
    val candidates=listOf(File("src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt"),File("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt"))
    val src=candidates.first { it.isFile }.readText()
    assertTrue(src.contains("mechanical_contract"))
    assertTrue(src.contains("CanonicalTalesRules.localMechanicalContract()"))
    assertTrue(src.contains("reference_context"))
    assertTrue(src.contains("NUNCA use estes trechos para decidir ou alterar regras"))
    assertFalse(src.contains("rag_instruction"))
  }
}
''', encoding="utf-8")

# Resume checkpoint.
resume = root / "TALES_RETOMADA_CURRENT.md"
r = resume.read_text(encoding="utf-8") if resume.exists() else "# TALES FROM THE LOOP — RETOMADA ATUAL\n"
r += '''\n## Bloco 1.3.13 — autoridade mecânica local / inventário real\n- Regras mecânicas centrais Tales/Kids consolidadas em autoridade local determinística; Gemini/RAG não pode definir regra.\n- Contexto de manuais importados passou a ser somente referência opcional de ambientação/lore; conflitos mecânicos são ignorados pelo Motor.\n- MOCHILA deixou de exibir Lanterna/Fita/Caderno/Binóculos fictícios fixos. Agora mostra somente objetos realmente presentes no estado do Kid.\n- Objetos comuns têm bônus 0. Itens do Mistério guardam seu bônus específico +1..+3. Item Icônico permanece +2.\n- Mestre pode adicionar/remover Itens reais por ações validadas; bônus proposto só entra se nome/tipo/valor coincidirem exatamente com o estado persistido.\n- Itens do Mistério são removidos no limite de fim de Mistério; objetos comuns persistem.\n- Inventário e música favorita passam a persistir em save e JSON do Kid. Saves antigos migram com inventário vazio, sem itens inventados.\n- Locais continuam sendo estado dinâmico revelado pelo Mestre, não catálogo rígido nem dependência de RAG.\n- TAL-EDGE-001 continua bloqueado: nenhuma regra Flood de mínimo 1 dado foi importada.\n'''
resume.write_text(r,encoding="utf-8")

# Hard checks.
assert 'versionName = "1.3.13"' in read(gradle_rel)
assert 'data class KidItemState' in read(core_rel)
assert 'items: List<KidItemState>' in read(core_rel)
assert 'mechanical_contract' in read("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt")
assert 'rag_instruction' not in read("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraTransport.kt")
assert 'mystery_item_approve' in read("app/src/main/java/com/braseiro/talesvtt/game/MotorBarbaraWireAdapter.kt")
assert 'Nenhum objeto comum ou Item do Mistério' in read("app/src/main/java/com/braseiro/talesvtt/screens/BackpackScreen.kt")
assert 'title = "Lanterna"' not in read("app/src/main/java/com/braseiro/talesvtt/screens/BackpackScreen.kt")
print("TALES 1.3.13 transform: OK")
