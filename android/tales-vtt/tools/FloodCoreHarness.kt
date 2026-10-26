import com.braseiro.talesvtt.game.*

private var pass = 0
private var fail = 0
private fun check(name: String, condition: Boolean) {
  if (condition) { pass++; println("PASS | $name") }
  else { fail++; println("FAIL | $name") }
}

private fun validTeen(type: String = "hacker") = FloodTeenState(
  typeId = type,
  age = 17,
  attributes = linkedMapOf("BODY" to 3, "TECH" to 5, "HEART" to 3, "MIND" to 3),
  skills = linkedMapOf(
    "Sneak" to 0, "Force" to 0, "Move" to 0,
    "Tinker" to 1, "Program" to 3, "Calculate" to 3,
    "Contact" to 0, "Charm" to 0, "Lead" to 0,
    "Investigate" to 1, "Comprehend" to 2, "Empathize" to 0
  )
)

fun main() {
  check("12 Flood skills", FloodSkillCatalog.skillToAttribute.size == 12)
  check("10 Flood types", FloodTypeCatalog.types.size == 10)
  check("all Flood types have 3 key skills", FloodTypeCatalog.types.all { it.keySkills.size == 3 })
  check("valid Teen creation", validTeen().creationErrors().isEmpty())
  check("age 13 rejected", validTeen().copy(age = 13).creationErrors().isNotEmpty())
  check("age 20 rejected", validTeen().copy(age = 20).creationErrors().isNotEmpty())
  check("attribute sum 14 required", validTeen().copy(attributes = validTeen().attributes + ("BODY" to 4)).creationErrors().isNotEmpty())
  check("skill sum 10 required", validTeen().copy(skills = validTeen().skills + ("Sneak" to 1)).creationErrors().isNotEmpty())
  check("non-key initial skill max 1", validTeen().copy(skills = validTeen().skills + ("Tinker" to 2) + ("Investigate" to 0)).creationErrors().isNotEmpty())
  check("runtime skill max 5", validTeen().copy(skills = validTeen().skills + ("Program" to 6)).runtimeErrors().isNotEmpty())
  check("XP capped 10", validTeen().copy(xp = 11).runtimeErrors().isNotEmpty())

  val baseRequest = validTeen().testForSkill("hack", "Program", 1)!!
  var s = FloodState(teen = validTeen())
  s = FloodReducer.reduce(s, FloodEvent.GmRequestTest(baseRequest))
  check("GM test accepted", s.activeTest != null && s.lastError == null)
  check("base Flood pool 8", s.finalPool() == 8)

  s = FloodReducer.reduce(s.copy(teen = s.teen.copy(conditions = FloodCondition.entries.toSet())), FloodEvent.ClearTest)
  s = FloodReducer.reduce(s, FloodEvent.GmRequestTest(FloodTestRequest("minimum", "BODY", 1, "Sneak", 0, 1)))
  check("minimum 1 die when not Broken", s.finalPool() == 1)
  s = s.copy(teen = s.teen.copy(broken = true))
  check("Broken pool is zero", s.finalPool() == 0)

  s = FloodState(teen = validTeen())
  s = FloodReducer.reduce(s, FloodEvent.GmRequestTest(baseRequest))
  s = FloodReducer.reduce(s, FloodEvent.ApplyHelp("Mikael"))
  check("one helper adds one die", s.finalPool() == 9)
  val secondHelp = FloodReducer.reduce(s, FloodEvent.ApplyHelp("Liv"))
  check("second helper blocked", secondHelp.lastError != null)
  s = FloodReducer.reduce(s, FloodEvent.ApplyApprovedItem(2))
  check("iconic-style +2 approved bonus", s.finalPool() == 11)

  s = FloodReducer.reduce(s, FloodEvent.RollResult(listOf(6,1,2,3,4,5,1,2,3,4,5)))
  check("6 counts as success", s.testResolution?.successes == 1)
  s = FloodReducer.reduce(s, FloodEvent.UseShame("m1", "s1", false))
  check("Shame adds automatic success", s.testResolution?.successes == 2 && s.testResolution?.shameUsed == true)
  val shameTwice = FloodReducer.reduce(s, FloodEvent.UseShame("m1", "s1", false))
  check("Shame cannot repeat in same test", shameTwice.lastError != null)

  val scarTeen = validTeen().copy(scars = listOf(FloodScar("scar-1", "Culpa", accepted = false)))
  var scarState = FloodState(teen = scarTeen)
  scarState = FloodReducer.reduce(scarState, FloodEvent.GmRequestTest(scarTeen.testForSkill("x", "Program", 1)!!))
  scarState = FloodReducer.reduce(scarState, FloodEvent.RollResult(List(8){1}))
  scarState = FloodReducer.reduce(scarState, FloodEvent.UseScar("scar-1"))
  check("Scar adds +1 success", scarState.testResolution?.successes == 1)
  check("Scar becomes accepted", scarState.teen.scars.first().accepted)
  check("accepted Scar cannot be used again", FloodReducer.reduce(scarState, FloodEvent.UseScar("scar-1")).lastError != null)

  var pushState = FloodState(teen = validTeen())
  pushState = FloodReducer.reduce(pushState, FloodEvent.GmRequestTest(FloodTestRequest("push", "BODY", 3, "Sneak", 0, 1)))
  pushState = FloodReducer.reduce(pushState, FloodEvent.RollResult(listOf(1,2,3)))
  pushState = FloodReducer.reduce(pushState, FloodEvent.PushRoll(FloodCondition.UPSET, listOf(6,2,3)))
  check("Push marks Condition", FloodCondition.UPSET in pushState.teen.conditions)
  check("Push rerolls non-six", pushState.testResolution?.dice == listOf(6,2,3))
  check("Push only once", FloodReducer.reduce(pushState, FloodEvent.PushRoll(FloodCondition.SCARED, listOf(1,1))).lastError != null)

  var brokenState = FloodState(teen = validTeen().copy(conditions = FloodCondition.entries.toSet()))
  brokenState = FloodReducer.reduce(brokenState, FloodEvent.ApplyCondition(null, FloodBreakSource.TROUBLE, "Primeira Scar"))
  check("fifth Condition -> Broken", brokenState.teen.broken)
  check("Broken creates first Scar", brokenState.teen.scars.size == 1)
  check("first Scar needs no death roll", brokenState.teen.scars.first().deathRoll == null)
  brokenState = brokenState.copy(teen = brokenState.teen.copy(broken = false, conditions = emptySet()))
  brokenState = FloodReducer.reduce(brokenState, FloodEvent.BecomeBroken(FloodBreakSource.TROUBLE, "Segunda Scar", deathRollIfRequired = 1))
  check("second Scar records d6", brokenState.teen.scars.last().deathRoll == 1)
  check("failed non-extended death roll is pending", brokenState.teen.removalPending && !brokenState.teen.removed)

  val extendedBase = FloodState(teen = validTeen())
  val scenes = listOf(
    FloodExtendedTeenScene("Sara","Program"),
    FloodExtendedTeenScene("Mikael","Force"),
    FloodExtendedTeenScene("Liv","Lead"),
    FloodExtendedTeenScene("Noah","Investigate")
  )
  var ext = FloodReducer.reduce(extendedBase, FloodEvent.StartExtendedTrouble("Salvar todos",4,FloodThreatLevel.NORMAL,scenes))
  check("Flood Normal target = 3x Teens", ext.extended.target == 12)
  ext = FloodReducer.reduce(ext, FloodEvent.ResolveExtendedTeenScene(0,2))
  ext = FloodReducer.reduce(ext, FloodEvent.ResolveExtendedTeenScene(1,2))
  ext = FloodReducer.reduce(ext, FloodEvent.ResolveExtendedTeenScene(2,2))
  ext = FloodReducer.reduce(ext, FloodEvent.ResolveExtendedTeenScene(3,1))
  check("above half -> compromise", ext.extended.outcome == FloodExtendedOutcome.COMPROMISE_AVAILABLE)

  var exact = FloodReducer.reduce(extendedBase, FloodEvent.StartExtendedTrouble("x",4,FloodThreatLevel.NORMAL,scenes))
  listOf(2,2,1,1).forEachIndexed { i, n -> exact = FloodReducer.reduce(exact, FloodEvent.ResolveExtendedTeenScene(i,n)) }
  check("exact half does not invent ruling", exact.extended.outcome == FloodExtendedOutcome.EXACT_HALF_REQUIRES_GM)

  var full = FloodReducer.reduce(extendedBase, FloodEvent.StartExtendedTrouble("x",4,FloodThreatLevel.NORMAL,scenes))
  listOf(3,3,3,3).forEachIndexed { i, n -> full = FloodReducer.reduce(full, FloodEvent.ResolveExtendedTeenScene(i,n)) }
  check("target reached -> objective complete", full.extended.outcome == FloodExtendedOutcome.OBJECTIVE_COMPLETE)

  var failed = FloodReducer.reduce(extendedBase, FloodEvent.StartExtendedTrouble("x",4,FloodThreatLevel.EXTREMELY_DIFFICULT,scenes))
  listOf(1,1,1,1).forEachIndexed { i, n -> failed = FloodReducer.reduce(failed, FloodEvent.ResolveExtendedTeenScene(i,n)) }
  check("below half -> complete failure", failed.extended.outcome == FloodExtendedOutcome.COMPLETE_FAILURE)
  check("Extremely Difficult target = 4x", failed.extended.target == 16)
  val almost = FloodExtendedTroubleState(active=true,teens=4,threatLevel=FloodThreatLevel.ALMOST_IMPOSSIBLE,scenes=scenes)
  check("Almost Impossible target = 5x", almost.target == 20)

  var rec = FloodState(teen = validTeen().copy(conditions = setOf(FloodCondition.UPSET, FloodCondition.SCARED), broken = true))
  rec = FloodReducer.reduce(rec, FloodEvent.RecoverWithAnchor)
  check("Anchor clears Conditions", rec.teen.conditions.isEmpty() && !rec.teen.broken)

  var accepted = FloodState(teen = validTeen().copy(scars = listOf(FloodScar("s","x",accepted=true))))
  accepted = FloodReducer.reduce(accepted, FloodEvent.HealAcceptedScar("s"))
  check("accepted Scar can heal", accepted.teen.scars.isEmpty())
  val unaccepted = FloodReducer.reduce(FloodState(teen=validTeen().copy(scars=listOf(FloodScar("s","x")))), FloodEvent.HealAcceptedScar("s"))
  check("unaccepted Scar cannot heal", unaccepted.lastError != null)

  var xp = FloodState(teen = validTeen().copy(xp=9))
  xp = FloodReducer.reduce(xp, FloodEvent.AwardXp(5))
  check("XP storage cap 10", xp.teen.xp == 10)
  xp = FloodReducer.reduce(xp, FloodEvent.ImproveSkill("Sneak"))
  check("5 XP buys +1 Skill", xp.teen.xp == 5 && xp.teen.skills["Sneak"] == 1)

  val source = java.io.File("app/src/main/java/com/braseiro/talesvtt/game/FloodCore.kt").readText()
  check("Flood core has no Tales Luck state", !Regex("\\b(val|var)\\s+luck\\b|SpendLuck|luckUsed").containsMatchIn(source))
  check("Flood core has no Tales Pride state", !Regex("\\b(val|var)\\s+pride\\b|UsePride|prideUsed").containsMatchIn(source))

  println("RESULT $pass PASS / $fail FAIL")
  if (fail > 0) error("FloodCoreHarness failed")
}
