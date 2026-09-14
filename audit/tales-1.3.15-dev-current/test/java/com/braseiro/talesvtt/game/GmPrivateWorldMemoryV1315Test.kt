package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test

class GmPrivateWorldMemoryV1315Test {
  @Test fun privateMemoryRoundTripV2() {
    val state=GmPrivateState(
      clues=mapOf("c1" to PrivateClue("c1","Pista","segredo","Escola")),
      npcs=mapOf("npc-1" to PrivateNpc("npc-1","Agnes","Âncora","Vizinha atenta","Casa de Agnes","active")),
      worldFacts=mapOf("f1" to PrivateWorldFact("f1","antena","emite pulsos à noite","mystery")),
      countdowns=mapOf("d1" to PrivateCountdown("d1","Falha da antena",2,4,"apagão"))
    )
    val decoded=GmPrivateSnapshotCodec.decode(GmPrivateSnapshotCodec.encode(state))!!
    assertEquals(state,decoded)
  }

  @Test fun legacyV1StillMigrates() {
    val old="version=1\nclues=c1~Pista~texto~Escola\nconnections=\nlocations=l1~Escola\n"
    val decoded=GmPrivateSnapshotCodec.decode(old)!!
    assertEquals("Pista",decoded.clues["c1"]!!.title)
    assertTrue(decoded.npcs.isEmpty())
    assertTrue(decoded.worldFacts.isEmpty())
    assertTrue(decoded.countdowns.isEmpty())
  }

  @Test fun privateUpsertsNeverMutatePlayerState() {
    val game=GameState()
    val packet=MotorPacket(actions=listOf(
      MotorAction.SecretNpcUpsert(PrivateNpc("n1","Rolf","Professor",location="Escola")),
      MotorAction.SecretWorldFactUpsert(PrivateWorldFact("f1","Rolf","esconde uma chave","mystery")),
      MotorAction.SecretCountdownUpsert(PrivateCountdown("d1","Patrulha",1,4,"guardas chegam"))
    ))
    val result=MotorBarbaraFirewall.apply(game,GmPrivateState(),packet)
    assertTrue(result.isClean)
    assertEquals(3,result.acceptedActions)
    assertEquals(game,result.gameState)
    assertEquals("Rolf",result.privateState.npcs["n1"]!!.name)
    assertEquals(1,result.privateState.countdowns["d1"]!!.stage)
  }

  @Test fun samePrivateIdUpdatesInsteadOfDuplicating() {
    var gm=GmPrivateState()
    val first=MotorBarbaraFirewall.apply(GameState(),gm,MotorPacket(actions=listOf(MotorAction.SecretNpcUpsert(PrivateNpc("n1","Rolf",location="Escola")))))
    gm=first.privateState
    val second=MotorBarbaraFirewall.apply(GameState(),gm,MotorPacket(actions=listOf(MotorAction.SecretNpcUpsert(PrivateNpc("n1","Rolf",location="Hospital",status="injured")))))
    assertTrue(second.isClean)
    assertEquals(1,second.privateState.npcs.size)
    assertEquals("Hospital",second.privateState.npcs["n1"]!!.location)
    assertEquals("injured",second.privateState.npcs["n1"]!!.status)
  }

  @Test fun invalidCountdownIsRejected() {
    val result=MotorBarbaraFirewall.apply(GameState(),GmPrivateState(),MotorPacket(actions=listOf(
      MotorAction.SecretCountdownUpsert(PrivateCountdown("d1","Ameaça",5,4,"fim"))
    )))
    assertFalse(result.isClean)
    assertTrue(result.privateState.countdowns.isEmpty())
    assertEquals(0,result.acceptedActions)
  }
}
