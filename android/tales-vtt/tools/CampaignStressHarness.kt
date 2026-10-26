import com.braseiro.talesvtt.game.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

private data class Stats(
  var campaigns:Int=0, var sessions:Int=0, var turns:Int=0, var tests:Int=0,
  var successes:Int=0, var failures:Int=0, var luckUses:Int=0, var pushes:Int=0,
  var prideUses:Int=0, var recoveries:Int=0, var extended:Int=0,
  var fullExtended:Int=0, var compromiseExtended:Int=0, var failedExtended:Int=0,
  var snapshots:Int=0, var gmPackets:Int=0, var secrets:Int=0, var reveals:Int=0,
  var expectedRejects:Int=0, var invariantChecks:Long=0
)

private class Harness(private val seed:Int) {
  val rnd=Random(seed)
  val stats=Stats()
  private val skills=TalesSkillCatalog.skillToAttribute.keys.toList()
  private val locations=listOf("Portão de Serviço","Casa de Lasse","Escola Björkskolan","Esconderijo")
  private val assets=MotorBarbaraFirewall.allowedSceneAssets.toList()
  private val conditions=Condition.entries
  private var secretCounter=0

  private fun invariant(s:GameState, gm:GmPrivateState) {
    stats.invariantChecks++
    require(s.kid.age in 10..16) { "age ${s.kid.age}" }
    require(s.kid.luck in 0..s.kid.maxLuck.coerceAtLeast(0)) { "luck ${s.kid.luck}/${s.kid.maxLuck}" }
    require(s.kid.xp in 0..10) { "xp ${s.kid.xp}" }
    require(s.kid.attributes.values.all{it in 1..5}) { "attribute outside 1..5" }
    require(s.kid.skills.values.all{it in 0..5}) { "skill outside 0..5" }
    require(s.finalPool() >= 0)
    s.activeTest?.let { require(it.requiredSuccesses in 1..3); require(it.attributeValue>=0 && it.skillValue>=0) }
    s.testResolution?.let { r -> require(r.dice.all{it in 1..6}); require(r.successes>=0) }
    val member=s.group.members.firstOrNull{it.name.equals(s.kid.name,true)}
    if(member!=null){ require(member.conditions==s.kid.conditions); require(member.broken==s.kid.broken) }
    if(s.extended.active){
      require(s.extended.scenes.size==s.extended.kids)
      require(s.extended.target==s.extended.kids*s.extended.threatLevel.multiplier)
      require(s.extended.resolvedScenes<=s.extended.kids)
    }
    // Private text must never appear in player-facing revealed IDs/titles.
    val publicBlob=(s.revealed.clues+s.revealed.locations+s.revealed.connections.flatMap{listOf(it.first,it.second)}).joinToString("\n")
    gm.clues.values.forEach { if(it.text.isNotBlank()) require(!publicBlob.contains(it.text)) { "private clue text leaked" } }
  }

  private fun roundTrip(s:GameState, gm:GmPrivateState):Pair<GameState,GmPrivateState>{
    val e=GameSnapshotCodec.encode(s)
    val d=requireNotNull(GameSnapshotCodec.decode(e)){"public snapshot decode failed"}
    require(d==s.copy(lastError=null)){"public snapshot mismatch\nORIG=$s\nDEC=$d"}
    val ge=GmPrivateSnapshotCodec.encode(gm)
    val gd=requireNotNull(GmPrivateSnapshotCodec.decode(ge)){"gm snapshot decode failed"}
    require(gd==gm){"gm snapshot mismatch"}
    stats.snapshots++
    invariant(d,gd)
    return d to gd
  }

  private fun rollDice(n:Int)=List(n){rnd.nextInt(1,7)}

  private fun applyPacket(s:GameState, gm:GmPrivateState, actions:List<MotorAction>):Pair<GameState,GmPrivateState>{
    val r=MotorBarbaraFirewall.apply(s,gm,MotorPacket(actions=actions))
    stats.gmPackets++
    if(r.rejected.isNotEmpty()) error("Unexpected GM rejection seed=$seed: ${r.rejected.joinToString()} actions=$actions")
    invariant(r.gameState,r.privateState)
    return r.gameState to r.privateState
  }

  private fun addAndRevealSecret(s0:GameState, gm0:GmPrivateState, turn:Int):Pair<GameState,GmPrivateState>{
    var s=s0; var gm=gm0
    val id="s${seed}_${secretCounter++}"
    val title="Pista $id"
    val secret="SEGREDO-$id-${rnd.nextInt()}"
    val add=MotorBarbaraFirewall.apply(s,gm,MotorPacket(actions=listOf(MotorAction.SecretClueAdd(PrivateClue(id,title,secret,s.scene.location)))))
    require(add.isClean); s=add.gameState; gm=add.privateState; stats.gmPackets++; stats.secrets++
    require(secret !in s.revealed.clues.joinToString(" "))
    if(turn%2==0){
      val rev=MotorBarbaraFirewall.apply(s,gm,MotorPacket(actions=listOf(MotorAction.RevealClue(id))))
      require(rev.isClean); s=rev.gameState; gm=rev.privateState; stats.gmPackets++; stats.reveals++
      require(title in s.revealed.clues); require(secret !in s.revealed.clues)
    }
    return s to gm
  }

  private fun doTest(s0:GameState, gm0:GmPrivateState, turn:Int):Pair<GameState,GmPrivateState>{
    var s=s0; var gm=gm0
    // Recover if Broken so long campaigns keep progressing.
    if(s.kid.broken){
      val x=applyPacket(s,gm,listOf(MotorAction.Recovery(true))); s=x.first;gm=x.second;stats.recoveries++
    }
    val skill=skills[rnd.nextInt(skills.size)]
    val req=1+rnd.nextInt(3)
    val start=applyPacket(s,gm,listOf(MotorAction.TestRequest(skill,req,"Turno $turn • $skill")))
    s=start.first; gm=start.second
    stats.tests++
    if(rnd.nextInt(100)<24){
      val helper=listOf("Anita","Jonas","Malin")[rnd.nextInt(3)]
      val x=applyPacket(s,gm,listOf(MotorAction.ApproveHelp(helper)));s=x.first;gm=x.second
    }
    if(rnd.nextInt(100)<18){ val x=applyPacket(s,gm,listOf(MotorAction.ApproveIconicItem));s=x.first;gm=x.second }
    val pool=s.finalPool()
    if(pool<=0 && !s.kid.broken){
      val blocked=TalesReducer.reduce(s,GameEvent.RollResult(emptyList()))
      require(blocked.lastError?.contains("TAL-EDGE-001")==true)
      stats.expectedRejects++
      s=TalesReducer.reduce(s,GameEvent.ClearTest)
      invariant(s,gm)
      return s to gm
    }
    s=TalesReducer.reduce(s,GameEvent.RollResult(rollDice(pool)))
    require(s.lastError==null)
    var success=(s.testResolution?.successes?:0)>=req
    if(!success && s.kid.luck>0 && rnd.nextInt(100)<35){
      val failed=s.testResolution!!.dice.count{it!=6}
      s=TalesReducer.reduce(s,GameEvent.SpendLuck(rollDice(failed)))
      require(s.lastError==null);stats.luckUses++
      success=s.testResolution!!.successes>=req
    }
    if(rnd.nextInt(100)<20 && !s.testResolution!!.pushed && !s.kid.broken){
      val failed=s.testResolution!!.dice.count{it!=6}
      val cond=conditions.firstOrNull{it !in s.kid.conditions}
      val canPush=(cond!=null || s.kid.conditions.size==4)
      if(canPush){
        s=TalesReducer.reduce(s,GameEvent.PushRoll(cond,rollDice(failed)))
        require(s.lastError==null);stats.pushes++
        success=s.testResolution!!.successes>=req
      }
    }
    if(!success && s.kid.prideAvailable && rnd.nextInt(100)<18){
      s=TalesReducer.reduce(s,GameEvent.UsePride);require(s.lastError==null);stats.prideUses++
      success=s.testResolution!!.successes>=req
    }
    if(success) stats.successes++ else {
      stats.failures++
      // Some failures carry a consequence. Avoid duplicate conditions.
      if(rnd.nextInt(100)<28 && !s.kid.broken){
        val c=conditions.firstOrNull{it !in s.kid.conditions}
        if(c!=null){ val x=applyPacket(s,gm,listOf(MotorAction.ApplyCondition(c)));s=x.first;gm=x.second }
        else { val x=applyPacket(s,gm,listOf(MotorAction.BreakKid));s=x.first;gm=x.second }
      }
    }
    s=TalesReducer.reduce(s,GameEvent.SetTestResultReported(true)); require(s.lastError==null)
    val cleared=applyPacket(s,gm,listOf(MotorAction.TestClear)); s=cleared.first; gm=cleared.second
    return s to gm
  }

  private fun doExtended(s0:GameState, gm0:GmPrivateState, mode:Int):Pair<GameState,GmPrivateState>{
    var s=s0;var gm=gm0
    if(s.kid.broken){val x=applyPacket(s,gm,listOf(MotorAction.Recovery(true)));s=x.first;gm=x.second;stats.recoveries++}
    val level=ThreatLevel.entries[rnd.nextInt(ThreatLevel.entries.size)]
    val scenes=listOf(
      "Lasse Svensson" to skills[rnd.nextInt(skills.size)],
      "Anita Holmberg" to skills[rnd.nextInt(skills.size)],
      "Jonas Berg" to skills[rnd.nextInt(skills.size)],
      "Malin Ek" to skills[rnd.nextInt(skills.size)]
    )
    val x=applyPacket(s,gm,listOf(MotorAction.StartExtendedTrouble("Apostas de stress $seed/$mode",level,scenes)));s=x.first;gm=x.second
    stats.extended++
    // Active client resolves Lasse's own scene.
    s=TalesReducer.reduce(s,GameEvent.BeginExtendedKidScene); require(s.lastError==null)
    val pool=s.finalPool(); val dice=when(mode%3){
      0 -> List(pool){6} // strong/full-success path
      1 -> List(pool){if(it==0)6 else 2} // middling path
      else -> List(pool){2} // failure path
    }
    s=TalesReducer.reduce(s,GameEvent.RollResult(dice));require(s.lastError==null)
    s=TalesReducer.reduce(s,GameEvent.FinalizeExtendedKidSceneFromTest);require(s.lastError==null)
    s=TalesReducer.reduce(s,GameEvent.ClearTest);require(s.lastError==null)
    // Other Kids' results are externally verified, never invented by this client.
    for(i in 1..3){
      val successCount=when(mode%3){0 -> s.extended.target/4 + 1;1 -> (s.extended.target/2)/4 + 1;else -> 0}
      val a=MotorAction.ExtendedSceneResult(s.extended.scenes[i].kid,successCount)
      val y=applyPacket(s,gm,listOf(a));s=y.first;gm=y.second
    }
    require(s.extended.allScenesResolved)
    when {
      s.extended.fullSuccess -> stats.fullExtended++
      s.extended.compromiseAvailable -> {
        stats.compromiseExtended++
        // To avoid infeasible condition allocations in random long campaigns, exercise decline here.
        s=TalesReducer.reduce(s,GameEvent.DeclineCompromise);require(s.lastError==null)
      }
      s.extended.completeFailure || s.extended.declinedCompromiseFailure -> stats.failedExtended++
      else -> { // exact-half can be neither completeFailure nor compromise by this implementation's threshold
        // decline is only legal if compromiseAvailable, so just classify as failure-like and continue only if end allows it.
        stats.failedExtended++
      }
    }
    val end=TalesReducer.reduce(s,GameEvent.EndExtendedTrouble)
    if(end.lastError!=null) {
      // Exact-half in current contract should be compromiseAvailable due ceil-half; fail loudly if not.
      error("Extended trouble could not end: ${end.lastError}; ext=${s.extended}")
    }
    s=end
    return s to gm
  }

  private fun endSession(s0:GameState, gm0:GmPrivateState, mystery:Boolean):Pair<GameState,GmPrivateState>{
    var s=s0;var gm=gm0
    if(s.activeTest!=null){val x=applyPacket(s,gm,listOf(MotorAction.TestClear));s=x.first;gm=x.second}
    if(s.extended.active){
      // Should not normally happen, but stress wants a consistent ending.
      if(!s.extended.allScenesResolved) error("attempted to end session during unresolved extended trouble")
      if(s.extended.compromiseAvailable) s=TalesReducer.reduce(s,GameEvent.DeclineCompromise)
      s=TalesReducer.reduce(s,GameEvent.EndExtendedTrouble);require(s.lastError==null)
    }
    val open=applyPacket(s,gm,listOf(if(mystery) MotorAction.MysteryEnd else MotorAction.SessionEnd));s=open.first;gm=open.second
    for(i in 0..4){s=TalesReducer.reduce(s,GameEvent.SetXpAnswer(i, i==0 || rnd.nextBoolean()));require(s.lastError==null)}
    s=TalesReducer.reduce(s,GameEvent.ConfirmMysteryEnd);require(s.lastError==null)
    if(mystery){
      // Most mysteries don't cross birthday. Some do, but keep ages <16 in stress campaign continuity.
      val passed=s.kid.age<15 && rnd.nextInt(100)<8
      s=TalesReducer.reduce(s,GameEvent.SetBirthdayPassed(passed));require(s.lastError==null)
      if(passed){
        val attr=s.kid.attributes.filterValues{it<5}.keys.random(rnd)
        s=TalesReducer.reduce(s,GameEvent.ApplyBirthdayAttribute(attr));require(s.lastError==null)
      }
    }
    // Random legal XP spend before next session.
    if(s.kid.xp>=5 && rnd.nextInt(100)<35){
      val eligible=s.kid.skills.filterValues{it<5}.keys.toList()
      if(eligible.isNotEmpty()){
        s=TalesReducer.reduce(s,GameEvent.ImproveSkill(eligible.random(rnd)));require(s.lastError==null)
      }
    }
    if(!s.change.newKidRequired){
      s=TalesReducer.reduce(s,GameEvent.StartNextSession);require(s.lastError==null)
    }
    return s to gm
  }

  fun runCampaign(campaign:Int, sessions:Int=8, turnsPerSession:Int=60) {
    stats.campaigns++
    var s=GameState()
    var gm=GmPrivateState()
    for(session in 0 until sessions){
      stats.sessions++
      for(turn in 0 until turnsPerSession){
        stats.turns++
        // Player action and narration every turn.
        s=TalesReducer.reduce(s,GameEvent.PlayerActionDeclared("Campanha $campaign sessão $session turno $turn"));require(s.lastError==null)
        val narration=applyPacket(s,gm,listOf(MotorAction.Narration("Narração stress $campaign/$session/$turn",listOf(SceneChoice("Continuar","Seguir a cena.")))));s=narration.first;gm=narration.second
        if(turn%11==0){val z=addAndRevealSecret(s,gm,turn);s=z.first;gm=z.second}
        if(turn%17==0){
          val loc=locations[(turn/17+session)%locations.size]
          val z=applyPacket(s,gm,listOf(MotorAction.WorldPatch(location=loc,weather=if(turn%34==0)"Chuva forte" else "Nublado",time="${10+(turn%12)}:${(turn*7)%60}".padStart(5,'0'),visualAssetKey=assets[(turn/17)%assets.size])));s=z.first;gm=z.second
        }
        if(turn%23==0 && turn>0){val z=doExtended(s,gm,turn+session);s=z.first;gm=z.second}
        else if(turn%3==0){val z=doTest(s,gm,turn);s=z.first;gm=z.second}
        if(turn%19==0 && s.kid.conditions.isNotEmpty()){
          val z=applyPacket(s,gm,listOf(MotorAction.Recovery(true)));s=z.first;gm=z.second;stats.recoveries++
        }
        if(turn%5==0){val rt=roundTrip(s,gm);s=rt.first;gm=rt.second}
        invariant(s,gm)
      }
      val mystery=(session%3==2)
      val z=endSession(s,gm,mystery);s=z.first;gm=z.second
      val rt=roundTrip(s,gm);s=rt.first;gm=rt.second
      if(s.change.newKidRequired) break
    }
  }
}

private fun fuzzFirewall(rounds:Int, seed:Int):Int{
  val rnd=Random(seed)
  var s=GameState();var gm=GmPrivateState();var rejects=0
  repeat(rounds){i->
    val actions=when(i%6){
      0->listOf<MotorAction>(MotorAction.TestRequest("Hackear",4,"bad"))
      1->listOf(MotorAction.WorldPatch(location="SECRETO_${i}"))
      2->listOf(MotorAction.WorldPatch(visualAssetKey="evil_${i}.png"))
      3->listOf(MotorAction.RevealClue("missing_${i}"))
      4->listOf(MotorAction.ApproveHelp("Intruso"))
      else->listOf(MotorAction.SecretClueAdd(PrivateClue("dup","x","hidden")),MotorAction.SecretClueAdd(PrivateClue("dup","x2","hidden2")))
    }
    val r=MotorBarbaraFirewall.apply(s,gm,MotorPacket(actions=actions))
    require(r.rejected.isNotEmpty())
    rejects+=r.rejected.size
    // The invalid actions must not create an active invented skill, reveal secret locations, or leak private text.
    require(r.gameState.activeTest?.skillName?.equals("HACKEAR",true)!=true)
    require(r.gameState.scene.location.startsWith("SECRETO_").not())
    require(r.gameState.revealed.clues.none{it.contains("hidden")})
    s=r.gameState;gm=r.privateState
    // Reset dup state so each iteration isolates the packet while preserving first accepted private clue behavior.
    if(i%6==5) gm=GmPrivateState()
  }
  return rejects
}

private fun snapshotFuzz(rounds:Int, seed:Int):Int{
  val rnd=Random(seed);var safe=0
  val base=GameSnapshotCodec.encode(GameState())
  repeat(rounds){
    val chars=base.toCharArray()
    repeat(1+rnd.nextInt(4)){
      val ix=rnd.nextInt(chars.size)
      chars[ix]=(' '..'~').random(rnd)
    }
    val mutated=String(chars)
    runCatching{GameSnapshotCodec.decode(mutated)}.getOrElse{error("snapshot decoder threw: $it")}
    safe++
  }
  return safe
}

fun main(args:Array<String>){
  val campaigns=args.getOrNull(0)?.toIntOrNull()?:100
  val sessions=args.getOrNull(1)?.toIntOrNull()?:8
  val turns=args.getOrNull(2)?.toIntOrNull()?:60
  val all=Stats()
  val elapsed=measureTimeMillis{
    for(c in 0 until campaigns){
      val h=Harness(0x51A7 + c*7919)
      h.runCampaign(c,sessions,turns)
      val st=h.stats
      all.campaigns+=st.campaigns;all.sessions+=st.sessions;all.turns+=st.turns;all.tests+=st.tests
      all.successes+=st.successes;all.failures+=st.failures;all.luckUses+=st.luckUses;all.pushes+=st.pushes;all.prideUses+=st.prideUses
      all.recoveries+=st.recoveries;all.extended+=st.extended;all.fullExtended+=st.fullExtended;all.compromiseExtended+=st.compromiseExtended;all.failedExtended+=st.failedExtended
      all.snapshots+=st.snapshots;all.gmPackets+=st.gmPackets;all.secrets+=st.secrets;all.reveals+=st.reveals;all.invariantChecks+=st.invariantChecks
    }
  }
  val rejects=fuzzFirewall(30000,99173)
  val fuzzed=snapshotFuzz(30000,77231)
  println("STRESS_PASS")
  println("campaigns=${all.campaigns}")
  println("sessions=${all.sessions}")
  println("turns=${all.turns}")
  println("tests=${all.tests} success=${all.successes} failure=${all.failures}")
  println("luckUses=${all.luckUses} pushes=${all.pushes} prideUses=${all.prideUses} recoveries=${all.recoveries}")
  println("extended=${all.extended} full=${all.fullExtended} compromise=${all.compromiseExtended} fail=${all.failedExtended}")
  println("snapshots=${all.snapshots}")
  println("gmPackets=${all.gmPackets} secrets=${all.secrets} reveals=${all.reveals}")
  println("invariantChecks=${all.invariantChecks}")
  println("firewallInvalidRounds=30000 rejects=$rejects")
  println("snapshotFuzzRounds=$fuzzed")
  println("elapsedMs=$elapsed")
}
