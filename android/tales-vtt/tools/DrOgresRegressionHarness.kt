import com.braseiro.talesvtt.game.*
import kotlin.random.Random

private var pass = 0
private var fail = 0
private fun check(name: String, ok: Boolean) {
  if (ok) { pass++; println("PASS  $name") } else { fail++; println("FAIL  $name") }
}

fun main() {
  // Secondary implementation cross-check only. DrOgres/tftloop is not treated as rules authority.
  val kidReference = mapOf(
    "Informático" to setOf("Cálculo", "Programação", "Conhecimento"),
    "Caipira" to setOf("Força", "Movimentação", "Mecânica"),
    "Esportista" to setOf("Força", "Movimentação", "Contatos"),
    "Popular" to setOf("Contatos", "Charme", "Liderança"),
    "Rocker" to setOf("Movimentação", "Charme", "Empatia"),
    "Encrenqueiro" to setOf("Força", "Furtividade", "Liderança"),
    "Estranho" to setOf("Furtividade", "Investigação", "Empatia")
  )
  kidReference.forEach { (type, skills) ->
    check("DrOgres overlap Kid type $type key skills", RandomKidFactory.creationKeySkills(type) == skills)
  }
  check("Eight Tales creation types remain available", RandomKidFactory.creationTypeLabels().size == 8)

  val floodReference = mapOf(
    "hacker" to setOf("Program", "Calculate", "Comprehend"),
    "jock" to setOf("Force", "Move", "Lead"),
    "lone_wolf" to setOf("Sneak", "Comprehend", "Empathize"),
    "motorhead" to setOf("Tinker", "Calculate", "Charm"),
    "party_animal" to setOf("Move", "Contact", "Lead"),
    "raver" to setOf("Tinker", "Contact", "Charm"),
    "rocker" to setOf("Move", "Empathize", "Investigate"),
    "seeker" to setOf("Investigate", "Empathize", "Lead"),
    "snob" to setOf("Charm", "Contact", "Comprehend"),
    "street_kid" to setOf("Force", "Sneak", "Investigate")
  )
  floodReference.forEach { (id, skills) ->
    check("DrOgres overlap Flood type $id key skills", FloodTypeCatalog.resolve(id)?.keySkills == skills)
  }

  // DrOgres changelog records historical bugs around conditions, negative pools, rerolls,
  // XP and iconic items. These checks make those classes of regression explicit here.
  val four = Condition.entries.toSet()
  val talesZero = GameState(
    kid = KidState(conditions = four),
    activeTest = TestRequest("zero pool", "CORPO", 1)
  )
  check("Tales raw pool can reach zero", talesZero.finalPool() == 0)
  val talesZeroRoll = TalesReducer.reduce(talesZero, GameEvent.RollResult(emptyList()))
  check("Tales zero pool does NOT inherit Flood minimum-one-die rule", talesZeroRoll.lastError?.contains("TAL-EDGE-001") == true)

  val rescued = GameState(
    kid = KidState(conditions = four),
    activeTest = TestRequest("item rescue", "CORPO", 2, approvedItemBonus = 3)
  )
  check("Approved item bonus is included before final zero-floor", rescued.finalPool() == 1)
  check("Positive rescued pool rolls normally", TalesReducer.reduce(rescued, GameEvent.RollResult(listOf(4))).lastError == null)

  val pushStart = GameState(
    kid = KidState(conditions = emptySet(), luck = 2),
    activeTest = TestRequest("push", "MENTE", 3, requiredSuccesses = 3),
    testResolution = TestResolution(listOf(6, 2, 3), successes = 1)
  )
  val pushed = TalesReducer.reduce(pushStart, GameEvent.PushRoll(Condition.UPSET, listOf(4, 6)))
  check("Push preserves first-roll sixes", pushed.testResolution?.dice == listOf(6, 4, 6))
  check("Push accumulates successes correctly", pushed.testResolution?.successes == 2)
  val luckAfterPush = TalesReducer.reduce(pushed, GameEvent.SpendLuck(listOf(6)))
  check("Luck remains available after a pushed failure", luckAfterPush.lastError == null && luckAfterPush.testResolution?.luckUsed == true)
  check("Luck after Push preserves prior sixes", luckAfterPush.testResolution?.successes == 3)

  var xp = GameState(kid = KidState(xp = 10), sessionXpAwarded = true)
  val before = xp.kid.skills["Investigação"] ?: 0
  xp = TalesReducer.reduce(xp, GameEvent.ImproveSkill("Investigação"))
  check("XP tracker spends exactly five", xp.kid.xp == 5)
  check("XP tracker raises exactly one skill", xp.kid.skills["Investigação"] == before + 1)

  val iconic = TalesReducer.reduce(
    GameState(activeTest = TestRequest("item", "MENTE", 2)),
    GameEvent.ApplyApprovedItem(2)
  )
  check("Iconic item +2 remains supported", iconic.activeTest?.approvedItemBonus == 2)

  val generatedTypes = mutableSetOf<String>()
  val seeded = Random(0x71A1E5)
  var randomKidsValid = true
  repeat(10_000) {
    val kid = RandomKidFactory.create(seeded)
    generatedTypes += kid.archetype
    if (!RandomKidFactory.validateCreation(kid) || !RandomKidFactory.validatePersistentState(kid)) randomKidsValid = false
  }
  check("10,000 random Kids satisfy creation + persistence contracts", randomKidsValid)
  check("Random factory reaches all eight Tales types", generatedTypes == RandomKidFactory.creationTypeLabels())

  val relationshipSource = GameState(kid = KidState(relationships = listOf(KidRelationship("A", "confia em B"))))
  val relationshipRoundTrip = GameSnapshotCodec.decode(GameSnapshotCodec.encode(relationshipSource))
  check("Relationship survives snapshot round-trip", relationshipRoundTrip?.kid?.relationships == relationshipSource.kid.relationships)

  val floodZero = FloodState(
    teen = FloodTeenState(conditions = FloodCondition.entries.toSet()),
    activeTest = FloodTestRequest("zero flood", "BODY", 1)
  )
  check("Flood keeps its own minimum-one-die rule", floodZero.finalPool() == 1)

  println("RESULT $pass PASS / $fail FAIL")
  if (fail > 0) error("DrOgresRegressionHarness failed")
}
