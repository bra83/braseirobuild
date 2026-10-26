import com.braseiro.talesvtt.game.*

private var pass = 0
private var fail = 0
private fun check(name: String, value: Boolean) {
  if (value) { pass++; println("PASS  $name") } else { fail++; println("FAIL  $name") }
}

fun main() {
  var s = GameState()
  check("max luck = 15-age", s.kid.maxLuck == 3)
  check("one active condition", s.kid.conditionPenalty == 1)

  s = TalesReducer.reduce(s, GameEvent.GmRequestTest(TestRequest("gate", "MENTE", 2, "INVESTIGAÇÃO", 2)))
  check("base pool 4", s.activeTest!!.basePool() == 4)
  check("condition makes pool 3", s.finalPool() == 3)

  s = TalesReducer.reduce(s, GameEvent.ApplyHelp("Anita"))
  check("help +1 die", s.finalPool() == 4)
  val h2 = TalesReducer.reduce(s, GameEvent.ApplyHelp("Jonas"))
  check("max one helper", h2.lastError != null)

  val badRoll = TalesReducer.reduce(s, GameEvent.RollResult(listOf(1,2,3)))
  check("reject wrong dice count", badRoll.lastError != null)

  s = TalesReducer.reduce(s, GameEvent.RollResult(listOf(1,2,3,4)))
  check("failed roll has 0 successes", s.testResolution?.successes == 0)
  s = TalesReducer.reduce(s, GameEvent.SpendLuck(listOf(1,2,3,4)))
  check("luck spent", s.kid.luck == 2 && s.testResolution?.luckUsed == true)
  val luckAgain = TalesReducer.reduce(s, GameEvent.SpendLuck(listOf(1,2,3,4)))
  check("max one luck per roll", luckAgain.lastError != null)

  s = TalesReducer.reduce(s, GameEvent.PushRoll(Condition.UPSET, listOf(1,2,3,4)))
  check("push marks chosen condition", Condition.UPSET in s.kid.conditions && s.kid.conditions.size == 2)
  check("push marks used", s.testResolution?.pushed == true)
  val duplicatePushCondition = TalesReducer.reduce(
    GameState(
      kid = KidState(conditions = setOf(Condition.SCARED)),
      activeTest = TestRequest("x", "MENTE", 2),
      testResolution = TestResolution(listOf(1), 0)
    ),
    GameEvent.PushRoll(Condition.SCARED, listOf(2))
  )
  check("push cannot mark an already active condition", duplicatePushCondition.lastError != null)
  val pushAgain = TalesReducer.reduce(s, GameEvent.PushRoll(Condition.EXHAUSTED, listOf(1,2,3,4)))
  check("push only once", pushAgain.lastError != null)

  s = TalesReducer.reduce(s, GameEvent.UsePride)
  check("pride +1 automatic success", s.testResolution?.successes == 1)
  check("pride consumed", !s.kid.prideAvailable)

  var c = GameState(kid = KidState(conditions = setOf(Condition.UPSET, Condition.SCARED, Condition.EXHAUSTED, Condition.INJURED)))
  c = TalesReducer.reduce(c, GameEvent.ApplyCondition(Condition.UPSET))
  check("duplicate condition rejected", c.lastError != null)
  c = TalesReducer.reduce(GameState(kid = KidState(conditions = setOf(Condition.UPSET, Condition.SCARED, Condition.EXHAUSTED, Condition.INJURED))), GameEvent.PushRoll(null, emptyList()))
  check("cannot push before roll", c.lastError != null)
  val fourConditionsPush = TalesReducer.reduce(
    GameState(
      kid = KidState(conditions = Condition.entries.toSet()),
      activeTest = TestRequest("x", "MENTE", 2),
      testResolution = TestResolution(listOf(1), 0)
    ),
    GameEvent.PushRoll(null, listOf(6))
  )
  check("push with four mild conditions makes Kid Broken", fourConditionsPush.kid.broken && fourConditionsPush.testResolution?.pushed == true)

  var b = GameState(kid = KidState(broken = true, conditions = Condition.entries.toSet()), activeTest = TestRequest("x", "CORPO", 3))
  check("broken pool zero", b.finalPool() == 0)
  b = TalesReducer.reduce(b, GameEvent.RollResult(emptyList()))
  check("broken auto failure", b.testResolution?.successes == 0)
  b = TalesReducer.reduce(b, GameEvent.HealAllConditions)
  check("healing clears broken", !b.kid.broken && b.kid.conditions.isEmpty())

  var r = GameState()
  val badConn = TalesReducer.reduce(r, GameEvent.RevealConnection("secret", "Portão de Serviço"))
  check("hidden connection rejected", badConn.lastError != null)
  r = TalesReducer.reduce(r, GameEvent.RevealClue("Sinal estranho"))
  r = TalesReducer.reduce(r, GameEvent.RevealConnection("Sinal estranho", "Portão de Serviço"))
  check("revealed connection accepted", ("Sinal estranho" to "Portão de Serviço") in r.revealed.connections)

  var e = GameState()
  e = TalesReducer.reduce(e, GameEvent.StartExtendedTrouble("stakes", 4, ThreatLevel.NORMAL, listOf(
    ExtendedKidScene("A","Investigação"), ExtendedKidScene("B","Furtividade"), ExtendedKidScene("C","Movimentação"), ExtendedKidScene("D","Programação")
  )))
  check("normal threat 2x kids", e.extended.target == 8)
  check("four scenes required", e.extended.scenes.size == 4)
  for (i in 0..3) e = TalesReducer.reduce(e, GameEvent.ResolveExtendedKidScene(i, 1))
  check("all scenes resolved", e.extended.allScenesResolved)
  check("half target enables compromise", e.extended.compromiseAvailable)
  e = TalesReducer.reduce(e, GameEvent.AcceptCompromise(listOf(
    CompromiseMark("Anita", Condition.UPSET),
    CompromiseMark("Anita", Condition.SCARED),
    CompromiseMark("Anita", Condition.EXHAUSTED),
    CompromiseMark("Anita", Condition.INJURED)
  )))
  check("extra conditions count as successes", e.extended.totalSuccesses == 8)
  check("compromise costs are actually marked on a Kid", e.group.members.first { it.name.startsWith("Anita") }.conditions.size == 4)
  check("compromise reaches target", e.extended.fullSuccess)


  var eBroken = GameState(
    group = GroupState(members = listOf(
      GroupMemberState("Lasse Svensson", "Informático", "tales_type_computer_geek", Condition.entries.toSet(), false)
    )),
    kid = KidState(conditions = Condition.entries.toSet()),
    extended = ExtendedTroubleState(
      active = true,
      stakes = "x",
      kids = 1,
      threatLevel = ThreatLevel.NORMAL,
      scenes = listOf(ExtendedKidScene("Lasse Svensson", "Investigação", 1))
    )
  )
  check("single-kid compromise is available at half target", eBroken.extended.compromiseAvailable)
  eBroken = TalesReducer.reduce(eBroken, GameEvent.AcceptCompromise(listOf(CompromiseMark("Lasse", null))))
  check("compromise may make Kid Broken for one success", eBroken.kid.broken && eBroken.extended.totalSuccesses == 2)


  var end = GameState(kid = KidState(xp = 3, conditions = setOf(Condition.SCARED), prideAvailable = false))
  end = TalesReducer.reduce(end, GameEvent.ApplyMysteryEnd(4))
  check("session end awards answers XP", end.kid.xp == 7)
  check("between mysteries heals conditions", end.kid.conditions.isEmpty() && !end.kid.broken)
  check("new mystery pride available", end.kid.prideAvailable)
  val endAgain = TalesReducer.reduce(end, GameEvent.ApplyMysteryEnd(4))
  check("session XP cannot be awarded twice", endAgain.lastError != null)

  var x = GameState(kid = KidState(xp = 3))
  x = TalesReducer.reduce(x, GameEvent.AwardXp(12))
  check("xp storage cap 10", x.kid.xp == 10)
  x = x.copy(sessionXpAwarded = true)
  val beforeInvestigacao = x.kid.skills["Investigação"] ?: 0
  x = TalesReducer.reduce(x, GameEvent.ImproveSkill("Investigação"))
  check("skill improvement costs 5", x.kid.xp == 5)
  check("skill improvement raises exactly one skill", x.kid.skills["Investigação"] == beforeInvestigacao + 1)
  val badSpend = TalesReducer.reduce(GameState(kid = KidState(xp = 4), sessionXpAwarded = true), GameEvent.ImproveSkill("Investigação"))
  check("cannot improve if under 5 xp", badSpend.lastError != null)
  val duringSession = TalesReducer.reduce(GameState(kid = KidState(xp = 7)), GameEvent.ImproveSkill("Investigação"))
  check("cannot spend xp during active session flow", duringSession.lastError != null)
  val maxSkill = TalesReducer.reduce(
    GameState(kid = KidState(xp = 10, skills = KidState().skills + ("Investigação" to 5)), sessionXpAwarded = true),
    GameEvent.ImproveSkill("Investigação")
  )
  check("skill maximum 5 enforced", maxSkill.lastError != null && maxSkill.kid.skills["Investigação"] == 5 && maxSkill.kid.xp == 10)
  val inventedSkillSpend = TalesReducer.reduce(
    GameState(kid = KidState(xp = 10), sessionXpAwarded = true),
    GameEvent.ImproveSkill("Hackear")
  )
  check("invented skill cannot receive xp", inventedSkillSpend.lastError != null)

  val item = TalesReducer.reduce(GameState(activeTest = TestRequest("x", "MENTE", 2)), GameEvent.ApplyApprovedItem(2))
  check("approved iconic item +2", item.finalPool() == 3) // 2 +2 -1 condition
  val badItem = TalesReducer.reduce(GameState(activeTest = TestRequest("x", "MENTE", 2)), GameEvent.ApplyApprovedItem(4))
  check("reject generic +4 item", badItem.lastError != null)

  val action = TalesReducer.reduce(GameState(), GameEvent.PlayerActionDeclared("Investigo o portão"))
  check("player action recorded", action.lastPlayerAction == "Investigo o portão")
  val blank = TalesReducer.reduce(GameState(), GameEvent.PlayerActionDeclared(" "))
  check("blank action rejected", blank.lastError != null)

  val extBad = TalesReducer.reduce(GameState(), GameEvent.StartExtendedTrouble("x", 4, ThreatLevel.DIFFICULT, listOf(ExtendedKidScene("A","S"))))
  check("extended trouble requires one scene per kid", extBad.lastError != null)


  val snapshotSource = GameState(
    kid = KidState(name = "Lasse Svensson", age = 12, luck = 2, conditions = setOf(Condition.SCARED), xp = 7),
    scene = SceneState(importedImageUri = "file:///scene_images/test.img"),
    lastPlayerAction = "Examino o portão",
    activeTest = TestRequest("Portão", "MENTE", 2, "INVESTIGAÇÃO", 2, approvedItemBonus = 2, helperKid = "Anita", extendedSceneIndex = 0),
    testResolution = TestResolution(listOf(6, 2, 4, 5, 1, 3), 1, luckUsed = true, reportedToGm = true),
    revealed = RevealedState(setOf("Foto das luzes no lago"), setOf("Portão de Serviço"), setOf("Foto das luzes no lago" to "Portão de Serviço")),
    extended = ExtendedTroubleState(true, "Reator", 4, ThreatLevel.DIFFICULT, listOf(
      ExtendedKidScene("Lasse", "Investigação", 2), ExtendedKidScene("Anita", "Furtividade", null),
      ExtendedKidScene("Jonas", "Movimentação", null), ExtendedKidScene("Malin", "Programação", null)
    ), compromiseDeclined = true),
    endingMode = EndingMode.MYSTERY,
    change = ChangeState(birthdayReviewed = true, birthdayPassed = false, birthdayApplied = true)
  )
  val encoded = GameSnapshotCodec.encode(snapshotSource)
  val decoded = GameSnapshotCodec.decode(encoded)
  check("snapshot codec decodes", decoded != null)
  check("snapshot preserves kid state", decoded?.kid == snapshotSource.kid)
  check("snapshot preserves Kid bio", decoded?.kid?.problem == snapshotSource.kid.problem && decoded?.kid?.relationships == snapshotSource.kid.relationships)
  check("snapshot preserves group state", decoded?.group == snapshotSource.group)
  check("snapshot preserves scene season/year", decoded?.scene?.seasonYear == snapshotSource.scene.seasonYear)
  check("snapshot preserves imported scene image binding", decoded?.scene?.importedImageUri == snapshotSource.scene.importedImageUri)
  check("snapshot preserves active test", decoded?.activeTest == snapshotSource.activeTest)
  check("snapshot preserves roll and result-report state", decoded?.testResolution == snapshotSource.testResolution && decoded?.testResolution?.reportedToGm == true)
  check("snapshot preserves revealed firewall state", decoded?.revealed == snapshotSource.revealed)
  check("snapshot preserves extended trouble", decoded?.extended == snapshotSource.extended)
  check("snapshot preserves ending/change state", decoded?.endingMode == EndingMode.MYSTERY && decoded?.change == snapshotSource.change)
  check("snapshot preserves extended test link", decoded?.activeTest?.extendedSceneIndex == 0)
  check("snapshot rejects unsupported version", GameSnapshotCodec.decode(encoded.replaceFirst(Regex("version=\\d+"), "version=99")) == null)

  val difficult = ExtendedTroubleState(active=true, kids=4, threatLevel=ThreatLevel.DIFFICULT, scenes=List(4){ExtendedKidScene("$it","S",0)})
  check("difficult threat 3x kids", difficult.target == 12)
  val impossible = difficult.copy(threatLevel=ThreatLevel.ALMOST_IMPOSSIBLE)
  check("almost impossible 4x kids", impossible.target == 16)



  // Motor Barbara event firewall: the GM never writes raw state.
  var gmPublic = GameState()
  var gmPrivate = GmPrivateState()
  var applied = MotorBarbaraFirewall.apply(
    gmPublic, gmPrivate,
    MotorPacket(actions = listOf(
      MotorAction.SecretClueAdd(PrivateClue("cl-1", "Marca de manutenção", "Código interno secreto", "Portão de Serviço")),
      MotorAction.RevealClue("cl-1"),
      MotorAction.Narration("Um estalo metálico corta o som da chuva.", listOf(SceneChoice("OUVIR", "Tentar localizar a origem do ruído."))),
      MotorAction.TestRequest("Investigação", 1, "Localizar o ruído")
    ))
  )
  gmPublic = applied.gameState
  gmPrivate = applied.privateState
  check("motor packet accepted cleanly", applied.isClean && applied.acceptedActions == 4)
  check("motor private clue stored outside public reveal", gmPrivate.clues["cl-1"]?.text == "Código interno secreto")
  check("motor reveal exposes title only", "Marca de manutenção" in gmPublic.revealed.clues && "Código interno secreto" !in gmPublic.revealed.clues)
  check("motor test uses authoritative kid sheet", gmPublic.activeTest?.attributeName == "MENTE" && gmPublic.activeTest?.attributeValue == 2 && gmPublic.activeTest?.skillValue == 2)
  check("motor test routes to trouble", applied.routeHint == MotorRouteHint.TROUBLE)

  val helpApproved = MotorBarbaraFirewall.apply(
    GameState(activeTest = TestRequest("x", "MENTE", 2, "INVESTIGAÇÃO", 2)),
    GmPrivateState(),
    MotorPacket(actions = listOf(MotorAction.ApproveHelp("Anita")))
  )
  check("motor-approved Help adds exactly one die", helpApproved.isClean && helpApproved.gameState.activeTest?.helperKid == "Anita Holmberg" && helpApproved.gameState.finalPool() == 4)
  val helpUnknown = MotorBarbaraFirewall.apply(
    GameState(activeTest = TestRequest("x", "MENTE", 2, "INVESTIGAÇÃO", 2)),
    GmPrivateState(),
    MotorPacket(actions = listOf(MotorAction.ApproveHelp("Pessoa inventada")))
  )
  check("motor rejects Help from non-group character", helpUnknown.rejected.isNotEmpty() && helpUnknown.gameState.activeTest?.helperKid == null)
  val helpSelf = MotorBarbaraFirewall.apply(
    GameState(activeTest = TestRequest("x", "MENTE", 2, "INVESTIGAÇÃO", 2)),
    GmPrivateState(),
    MotorPacket(actions = listOf(MotorAction.ApproveHelp("Lasse")))
  )
  check("motor rejects self Help", helpSelf.rejected.isNotEmpty())

  val iconicApproved = MotorBarbaraFirewall.apply(
    GameState(activeTest = TestRequest("x", "MENTE", 2, "INVESTIGAÇÃO", 2)),
    GmPrivateState(),
    MotorPacket(actions = listOf(MotorAction.ApproveIconicItem))
  )
  check("motor-approved Iconic Item applies canonical +2", iconicApproved.isClean && iconicApproved.gameState.activeTest?.approvedItemBonus == 2)
  val iconicNoTest = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(MotorAction.ApproveIconicItem)))
  check("motor cannot approve Item without active test", iconicNoTest.rejected.isNotEmpty())

  val fakeSkill = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.TestRequest("Hackear", 1, "Inventar perícia")
  )))
  check("motor rejects invented skill", fakeSkill.rejected.isNotEmpty() && fakeSkill.gameState.activeTest == null)

  val fakeDifficulty = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.TestRequest("Investigação", 4, "Dificuldade inventada")
  )))
  check("motor rejects trouble difficulty above 3", fakeDifficulty.rejected.isNotEmpty())

  val secretMove = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.WorldPatch(location = "Instalação secreta")
  )))
  check("motor cannot move player to unrevealed location", secretMove.rejected.isNotEmpty() && secretMove.gameState.scene.location == "Portão de Serviço")

  val fakeAsset = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.WorldPatch(visualAssetKey = "scene_inventada")
  )))
  check("motor rejects non-canonical scene asset", fakeAsset.rejected.isNotEmpty())

  val wrongProfile = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(profile = MotorProfile.TALES_KIDS, actions = emptyList()))
  check("motor profile Tales accepted", wrongProfile.rejected.isEmpty())

  val gmRevealBad = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(MotorAction.RevealClue("unknown"))))
  check("motor cannot reveal nonexistent private clue", gmRevealBad.rejected.isNotEmpty())

  val gmLoc = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.SecretLocationAdd(PrivateLocation("loc-x", "Túnel de manutenção")),
    MotorAction.RevealLocation("loc-x"),
    MotorAction.WorldPatch(location = "Túnel de manutenção", visualAssetKey = "scene_maintenance_corridor")
  )))
  check("motor reveal then world patch location", gmLoc.isClean && gmLoc.gameState.scene.location == "Túnel de manutenção")

  val gmConnSeed = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.SecretClueAdd(PrivateClue("a", "Pista A")),
    MotorAction.SecretClueAdd(PrivateClue("b", "Pista B")),
    MotorAction.SecretConnectionAdd(PrivateConnection("ab", "Pista A", "Pista B")),
    MotorAction.RevealClue("a"),
    MotorAction.RevealConnection("ab")
  )))
  check("motor blocks connection until both endpoints revealed", gmConnSeed.rejected.any { "Destino" in it || "destino" in it })

  val extMotor = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(
    MotorAction.StartExtendedTrouble(
      stakes = "O reator entra em sobrecarga.",
      threatLevel = ThreatLevel.DIFFICULT,
      scenes = listOf("Lasse" to "Investigação", "Anita" to "Furtividade", "Jonas" to "Movimentação", "Malin" to "Programação")
    )
  )))
  check("motor extended trouble uses 3x target", extMotor.isClean && extMotor.gameState.extended.target == 12)
  check("motor extended trouble routes to climax", extMotor.routeHint == MotorRouteHint.CLIMAX)

  val brokenByMotor = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(MotorAction.BreakKid)))
  check("motor can apply explicit Broken consequence", brokenByMotor.gameState.kid.broken)

  val recoverByMotor = MotorBarbaraFirewall.apply(brokenByMotor.gameState, GmPrivateState(), MotorPacket(actions = listOf(MotorAction.Recovery(true))))
  check("motor-approved recovery clears conditions and Broken", !recoverByMotor.gameState.kid.broken && recoverByMotor.gameState.kid.conditions.isEmpty())

  var xpState = TalesReducer.reduce(GameState(kid = KidState(xp = 3)), GameEvent.OpenEnding(EndingMode.MYSTERY))
  listOf(true, true, false, true, true).forEachIndexed { i, answer -> xpState = TalesReducer.reduce(xpState, GameEvent.SetXpAnswer(i, answer)) }
  xpState = TalesReducer.reduce(xpState, GameEvent.ConfirmMysteryEnd)
  check("xp is calculated from five answers, not GM", xpState.kid.xp == 7 && xpState.sessionXpAwarded)

  var reportState = GameState(
    activeTest = TestRequest("x", "MENTE", 2, "INVESTIGAÇÃO", 2),
    testResolution = TestResolution(listOf(6, 2, 3), 1)
  )
  reportState = TalesReducer.reduce(reportState, GameEvent.SetTestResultReported(true))
  check("result can be marked reported exactly in state", reportState.testResolution?.reportedToGm == true)
  val reportSnapshot = GameSnapshotCodec.decode(GameSnapshotCodec.encode(reportState))
  check("reported result survives autosave codec", reportSnapshot?.testResolution?.reportedToGm == true)

  val privateEncoded = GmPrivateSnapshotCodec.encode(gmPrivate)
  val privateDecoded = GmPrivateSnapshotCodec.decode(privateEncoded)
  check("GM private snapshot roundtrip", privateDecoded == gmPrivate)

  // Extended Trouble: current Kid scene is linked to a real TestRequest and finalized from the roll.
  var extFlow = TalesReducer.reduce(
    GameState(),
    GameEvent.StartExtendedTrouble(
      "Desligar o sistema",
      1,
      ThreatLevel.NORMAL,
      listOf(ExtendedKidScene("Lasse Svensson", "Investigação"))
    )
  )
  extFlow = TalesReducer.reduce(extFlow, GameEvent.BeginExtendedKidScene)
  check("extended scene opens authoritative Kid test", extFlow.activeTest?.extendedSceneIndex == 0 && extFlow.activeTest?.attributeName == "MENTE" && extFlow.finalPool() == 3)
  extFlow = TalesReducer.reduce(extFlow, GameEvent.RollResult(listOf(6, 2, 3)))
  extFlow = TalesReducer.reduce(extFlow, GameEvent.FinalizeExtendedKidSceneFromTest)
  check("extended scene uses final roll successes", extFlow.extended.scenes[0].successes == 1)
  val extFinalizeAgain = TalesReducer.reduce(extFlow, GameEvent.FinalizeExtendedKidSceneFromTest)
  check("extended finalization is idempotent", extFinalizeAgain.lastError == null && extFinalizeAgain.extended.scenes[0].successes == 1)

  val otherKidFlow = TalesReducer.reduce(
    TalesReducer.reduce(GameState(), GameEvent.StartExtendedTrouble("x", 1, ThreatLevel.NORMAL, listOf(ExtendedKidScene("Anita Holmberg", "Furtividade")))),
    GameEvent.BeginExtendedKidScene
  )
  check("client never invents another Kid roll", otherKidFlow.lastError != null && otherKidFlow.activeTest == null)

  var decline = GameState(extended = ExtendedTroubleState(
    active = true, stakes = "x", kids = 2, threatLevel = ThreatLevel.NORMAL,
    scenes = listOf(ExtendedKidScene("A", "Investigação", 1), ExtendedKidScene("B", "Furtividade", 1))
  ))
  check("compromise exists before decline", decline.extended.compromiseAvailable)
  decline = TalesReducer.reduce(decline, GameEvent.DeclineCompromise)
  check("player can decline compromise", decline.extended.compromiseDeclined && decline.extended.declinedCompromiseFailure)
  decline = TalesReducer.reduce(decline, GameEvent.EndExtendedTrouble)
  check("declined compromise can close extended trouble", !decline.extended.active)

  val wrongCompromiseCost = TalesReducer.reduce(
    GameState(extended = ExtendedTroubleState(
      active = true, stakes = "x", kids = 2, threatLevel = ThreatLevel.NORMAL,
      scenes = listOf(ExtendedKidScene("Lasse Svensson", "Investigação", 1), ExtendedKidScene("Anita Holmberg", "Furtividade", 1))
    )),
    GameEvent.AcceptCompromise(listOf(CompromiseMark("Anita", Condition.UPSET)))
  )
  check("compromise requires exact missing-success cost", wrongCompromiseCost.lastError != null)

  // Session end vs Mystery end are mechanically distinct.
  var sessionEnd = TalesReducer.reduce(
    GameState(kid = KidState(luck = 1, conditions = setOf(Condition.SCARED), prideAvailable = false)),
    GameEvent.OpenEnding(EndingMode.SESSION)
  )
  listOf(true, false, false, false, false).forEachIndexed { i, answer -> sessionEnd = TalesReducer.reduce(sessionEnd, GameEvent.SetXpAnswer(i, answer)) }
  sessionEnd = TalesReducer.reduce(sessionEnd, GameEvent.ConfirmMysteryEnd)
  check("session end does not heal Conditions", Condition.SCARED in sessionEnd.kid.conditions)
  sessionEnd = TalesReducer.reduce(sessionEnd, GameEvent.StartNextSession)
  check("new session refills Luck and Pride", sessionEnd.kid.luck == sessionEnd.kid.maxLuck && sessionEnd.kid.prideAvailable && sessionEnd.endingMode == EndingMode.NONE)

  var mysteryEnd = TalesReducer.reduce(
    GameState(kid = KidState(age = 12, luck = 2, conditions = setOf(Condition.SCARED))),
    GameEvent.OpenEnding(EndingMode.MYSTERY)
  )
  listOf(true, false, false, false, false).forEachIndexed { i, answer -> mysteryEnd = TalesReducer.reduce(mysteryEnd, GameEvent.SetXpAnswer(i, answer)) }
  mysteryEnd = TalesReducer.reduce(mysteryEnd, GameEvent.ConfirmMysteryEnd)
  check("mystery end heals Conditions", mysteryEnd.kid.conditions.isEmpty() && !mysteryEnd.kid.broken)
  val blockedBirthdaySkip = TalesReducer.reduce(mysteryEnd, GameEvent.StartNextSession)
  check("mystery cannot continue before explicit birthday review", blockedBirthdaySkip.lastError != null)
  mysteryEnd = TalesReducer.reduce(mysteryEnd, GameEvent.SetBirthdayPassed(true))
  val attrBeforeBirthday = mysteryEnd.kid.attributes["CORPO"] ?: 0
  mysteryEnd = TalesReducer.reduce(mysteryEnd, GameEvent.ApplyBirthdayAttribute("CORPO"))
  check("birthday gives exactly +1 Attribute and +1 age", mysteryEnd.kid.age == 13 && mysteryEnd.kid.attributes["CORPO"] == attrBeforeBirthday + 1)
  check("birthday reduces maximum Luck by age rule", mysteryEnd.kid.maxLuck == 2 && mysteryEnd.kid.luck <= mysteryEnd.kid.maxLuck)
  mysteryEnd = TalesReducer.reduce(mysteryEnd, GameEvent.StartNextSession)
  check("birthday-complete mystery can continue", mysteryEnd.lastError == null && mysteryEnd.endingMode == EndingMode.NONE)

  val maxAttributeBirthday = TalesReducer.reduce(
    GameState(
      kid = KidState(attributes = KidState().attributes + ("CORPO" to 5)),
      endingMode = EndingMode.MYSTERY, sessionXpAwarded = true,
      change = ChangeState(birthdayReviewed = true, birthdayPassed = true)
    ),
    GameEvent.ApplyBirthdayAttribute("CORPO")
  )
  check("birthday Attribute cannot exceed 5", maxAttributeBirthday.lastError != null)

  var age16 = TalesReducer.reduce(
    GameState(
      kid = KidState(age = 15, luck = 0),
      endingMode = EndingMode.MYSTERY, sessionXpAwarded = true,
      change = ChangeState(birthdayReviewed = true, birthdayPassed = true)
    ),
    GameEvent.ApplyBirthdayAttribute("MENTE")
  )
  check("turning 16 requires a new Kid", age16.kid.age == 16 && age16.change.newKidRequired)
  age16 = TalesReducer.reduce(age16, GameEvent.StartNextSession)
  check("age 16 blocks continuing with old Kid", age16.lastError != null)

  val motorSessionEnd = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(MotorAction.SessionEnd)))
  check("motor session_end opens session ending without awarding XP", motorSessionEnd.gameState.endingMode == EndingMode.SESSION && !motorSessionEnd.gameState.sessionXpAwarded && motorSessionEnd.routeHint == MotorRouteHint.ENDING)
  val motorMysteryEnd = MotorBarbaraFirewall.apply(GameState(), GmPrivateState(), MotorPacket(actions = listOf(MotorAction.MysteryEnd)))
  check("motor mystery_end opens mystery ending without awarding XP", motorMysteryEnd.gameState.endingMode == EndingMode.MYSTERY && !motorMysteryEnd.gameState.sessionXpAwarded && motorMysteryEnd.routeHint == MotorRouteHint.ENDING)

  val externalExtendedResult = MotorBarbaraFirewall.apply(
    GameState(extended = ExtendedTroubleState(active = true, stakes = "x", kids = 1, threatLevel = ThreatLevel.NORMAL, scenes = listOf(ExtendedKidScene("Anita Holmberg", "Furtividade")))),
    GmPrivateState(),
    MotorPacket(actions = listOf(MotorAction.ExtendedSceneResult("Anita", 2)))
  )
  check("verified external Kid result can advance extended scene", externalExtendedResult.isClean && externalExtendedResult.gameState.extended.scenes[0].successes == 2 && externalExtendedResult.routeHint == MotorRouteHint.CLIMAX)

  val extendedClearRoute = MotorBarbaraFirewall.apply(
    GameState(
      activeTest = TestRequest("x", "MENTE", 2, "INVESTIGAÇÃO", 2, extendedSceneIndex = 0),
      testResolution = TestResolution(listOf(6,2,3), 1),
      extended = ExtendedTroubleState(active = true, stakes = "x", kids = 1, threatLevel = ThreatLevel.NORMAL, scenes = listOf(ExtendedKidScene("Lasse Svensson", "Investigação", 1)))
    ),
    GmPrivateState(),
    MotorPacket(actions = listOf(MotorAction.TestClear))
  )
  check("clearing an extended test returns to Climax", extendedClearRoute.routeHint == MotorRouteHint.CLIMAX && extendedClearRoute.gameState.activeTest == null)

  val noChoiceState = TalesReducer.reduce(GameState(), GameEvent.GmNarration("Cena sem escolhas sugeridas.", emptyList()))
  val noChoiceRoundTrip = GameSnapshotCodec.decode(GameSnapshotCodec.encode(noChoiceState))
  check("snapshot preserves intentionally empty scene choices", noChoiceRoundTrip?.scene?.choices?.isEmpty() == true)

    println("RESULT $pass PASS / $fail FAIL")
  if (fail > 0) error("Engine contract checks failed")
}
