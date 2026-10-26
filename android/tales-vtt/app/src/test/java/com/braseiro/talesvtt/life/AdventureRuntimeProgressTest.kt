package com.braseiro.talesvtt.life

import com.braseiro.talesvtt.game.MotorAction
import com.braseiro.talesvtt.game.MotorPacket
import org.junit.Assert.*
import org.junit.Test

class AdventureRuntimeProgressTest {
  private fun fixture(): Pair<AdventureInstance, AdventurePackage> {
    val json = """{"schema":"braseiro.adventure/v1","id":"adv-a1","system":"tales-kids","worldId":"loop","title":"Exemplo","story":{"premise":"Uma pista estranha","background":"A escola abriga um transmissor"},"start":{"locationId":"escola"},"locations":[{"id":"escola","name":"Escola"}],"objectives":[{"id":"obj1","description":"Encontrar a pista"}],"encounters":[{"id":"enc1","locationId":"escola"}]}"""
    val pkg = (AdventurePackageValidator.parseAndValidate(json) as AdventurePackageValidator.ValidationResult.Valid).adventurePackage
    return AdventureInstance("i1",pkg.id,pkg.worldId,"t1",1L,"escola") to pkg
  }
  @Test fun realObjectivesAndEncountersPersistWithoutDuplicates() {
    val (inst,pkg)=fixture()
    val packet=MotorPacket(actions=listOf(MotorAction.ObjectiveComplete("obj1"),MotorAction.EncounterTriggered("enc1")))
    val result=AdventureRuntimeProgress.apply(inst,pkg,packet)
    assertTrue("obj1" in result.completedObjectiveIds)
    assertTrue("enc1" in result.triggeredEncounterIds)
    assertEquals(result,AdventureRuntimeProgress.apply(result,pkg,packet))
  }
  @Test fun unknownIdsCannotBeWritten() {
    val (inst,pkg)=fixture()
    val result=AdventureRuntimeProgress.apply(inst,pkg,MotorPacket(actions=listOf(MotorAction.ObjectiveComplete("fake"),MotorAction.EncounterTriggered("fake"))))
    assertEquals(inst,result)
  }
}