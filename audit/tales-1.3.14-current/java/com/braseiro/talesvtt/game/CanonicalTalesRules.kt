package com.braseiro.talesvtt.game

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
