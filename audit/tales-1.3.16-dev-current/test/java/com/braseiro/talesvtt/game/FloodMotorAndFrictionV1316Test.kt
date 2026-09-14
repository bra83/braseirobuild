package com.braseiro.talesvtt.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FloodMotorAndFrictionV1316Test {
  private fun src(rel:String):String {
    val a=File("src/main/java/com/braseiro/talesvtt/$rel"); val b=File("app/src/main/java/com/braseiro/talesvtt/$rel")
    return listOf(a,b).first{it.isFile}.readText()
  }

  @Test fun wireRecognizesFrictionRecovery(){
    val parsed=MotorBarbaraWireAdapter.parse("""{"profile":"flood_teen","actions":[{"type":"recovery","approved":true,"source":"friction"}]}""")
    val r=parsed.packet.actions.single() as MotorAction.Recovery
    assertEquals(RecoveryKind.FRICTION,r.kind)
  }

  @Test fun wireFloodAliasAlsoRecognizesFrictionRecovery(){
    val parsed=MotorBarbaraWireAdapter.parse("""{"profile":"flood","actions":[{"type":"recovery","approved":true,"source":"fricção"}]}""")
    assertEquals(MotorProfile.FLOOD_TEENS,parsed.packet.profile)
    assertEquals(RecoveryKind.FRICTION,(parsed.packet.actions.single() as MotorAction.Recovery).kind)
  }

  @Test fun floodFrictionRecoveryHealsOnlyThroughFloodFirewall(){
    val teen=RandomTeenFactory.create(kotlin.random.Random(3)).copy(conditions=setOf(Condition.UPSET,Condition.SCARED))
    val group=GroupState(members=listOf(GroupMemberState(teen.name,teen.archetype,teen.portraitAssetKey,teen.conditions,false)))
    val flood=GameState(rulesProfile=RulesProfile.FLOOD_TEENS,kid=teen,group=group)
    val packet=MotorPacket(MotorProfile.FLOOD_TEENS,listOf(MotorAction.Recovery(true,RecoveryKind.FRICTION,null)))
    val out=MotorBarbaraFirewall.apply(flood,GmPrivateState(),packet)
    assertTrue(out.rejected.isEmpty()); assertTrue(out.gameState.kid.conditions.isEmpty())

    val tales=GameState()
    val talesPacket=MotorPacket(MotorProfile.TALES_KIDS,listOf(MotorAction.Recovery(true,RecoveryKind.FRICTION,null)))
    val blocked=MotorBarbaraFirewall.apply(tales,GmPrivateState(),talesPacket)
    assertTrue(blocked.rejected.isNotEmpty())
  }

  @Test fun systemPromptIsProfileNeutralAndMechanicalContractOwnsRules(){
    val s=src("game/MotorBarbaraTransport.kt")
    assertTrue(s.contains("active_profile e mechanical_contract"))
    assertTrue(s.contains("source: anchor|hideout|comfort|friction"))
    assertTrue(s.contains("Nunca misture perfis"))
    assertFalse(s.contains("Mestre autônomo do VTT BRASEIRO para Tales from the Loop no perfil Tales/Kids"))
    assertFalse(s.contains("Problema Estendido Tales: Normal=2x Kids"))
  }

  @Test fun groupScreenNeverOffersTalesHideoutRecoveryToFlood(){
    val s=src("screens/GroupScreen.kt")
    assertTrue(s.contains("TEENS & FRICTION"))
    assertTrue(s.contains("PEDIR CENA PARA RESOLVER FRICTION"))
    assertTrue(s.contains("if(flood)"))
  }
}
