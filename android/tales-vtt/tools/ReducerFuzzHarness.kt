import com.braseiro.talesvtt.game.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun main(args:Array<String>){
  val rounds=args.getOrNull(0)?.toIntOrNull()?:250000
  val rnd=Random(0xBACE17)
  var s=GameState()
  var accepted=0;var rejected=0;var exceptions=0;var roundtrips=0
  val skills=TalesSkillCatalog.skillToAttribute.keys.toList()
  val conds=Condition.entries
  val ms=measureTimeMillis{
    repeat(rounds){i->
      val ev:GameEvent=when(rnd.nextInt(18)){
        0->GameEvent.PlayerActionDeclared(if(rnd.nextInt(10)==0)" " else "ação-$i")
        1->GameEvent.GmNarration(if(rnd.nextInt(20)==0)"" else "narração-$i")
        2->GameEvent.GmWorldPatch(location=listOf("Portão de Serviço","Casa de Lasse","Esconderijo").random(rnd),time="${rnd.nextInt(24)}:${rnd.nextInt(60)}")
        3->{ val sk=skills.random(rnd); GameEvent.GmRequestTest(s.kid.testForSkill("t-$i",sk,rnd.nextInt(0,5)) ?: TestRequest("bad","X",-1)) }
        4->GameEvent.ApplyHelp(listOf("Anita","Jonas","Lasse Svensson","").random(rnd))
        5->GameEvent.ApplyApprovedItem(rnd.nextInt(-1,5))
        6->GameEvent.RollResult(List(rnd.nextInt(0,10)){rnd.nextInt(0,8)})
        7->GameEvent.SpendLuck(List(rnd.nextInt(0,8)){rnd.nextInt(1,7)})
        8->GameEvent.PushRoll(conds.random(rnd),List(rnd.nextInt(0,8)){rnd.nextInt(1,7)})
        9->GameEvent.UsePride
        10->GameEvent.ApplyCondition(conds.random(rnd))
        11->GameEvent.HealAllConditions
        12->GameEvent.RevealClue("clue-${rnd.nextInt(100)}")
        13->GameEvent.RevealLocation("loc-${rnd.nextInt(100)}")
        14->GameEvent.RevealConnection("clue-${rnd.nextInt(100)}","loc-${rnd.nextInt(100)}")
        15->GameEvent.SetXpAnswer(rnd.nextInt(-1,7),rnd.nextBoolean())
        16->GameEvent.ImproveSkill(if(rnd.nextInt(10)==0)"Hackear" else skills.random(rnd))
        else->GameEvent.ClearTest
      }
      try{
        val n=TalesReducer.reduce(s,ev)
        if(n.lastError==null){
          accepted++;s=n
          require(s.kid.xp in 0..10)
          require(s.kid.skills.values.all{it in 0..5})
          require(s.finalPool()>=0)
          require(s.kid.luck>=0 && s.kid.luck<=s.kid.maxLuck.coerceAtLeast(0))
        } else rejected++
        if(i%1000==0){
          val d=requireNotNull(GameSnapshotCodec.decode(GameSnapshotCodec.encode(s)))
          if(d!=s.copy(lastError=null)){ println("SNAPSHOT_MISMATCH i=$i\nORIG=${s.copy(lastError=null)}\nDEC=$d\nENC=${GameSnapshotCodec.encode(s)}"); error("snapshot mismatch") };s=d;roundtrips++
        }
      }catch(t:Throwable){exceptions++;throw t}
    }
  }
  println("REDUCER_FUZZ_PASS rounds=$rounds accepted=$accepted rejected=$rejected exceptions=$exceptions roundtrips=$roundtrips elapsedMs=$ms")
}
