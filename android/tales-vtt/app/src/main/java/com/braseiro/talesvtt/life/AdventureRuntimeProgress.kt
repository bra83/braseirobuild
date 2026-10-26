package com.braseiro.talesvtt.life

import com.braseiro.talesvtt.game.MotorAction
import com.braseiro.talesvtt.game.MotorPacket

/** Only real package-defined IDs may change an active adventure instance. */
object AdventureRuntimeProgress {
  fun apply(instance: AdventureInstance, pkg: AdventurePackage, packet: MotorPacket): AdventureInstance {
    if (pkg.id != instance.packageId || pkg.worldId != instance.worldId || instance.concluded) return instance
    val knownObjectives = pkg.objectives.map { it.id }.toSet()
    val knownEncounters = pkg.encounters.associateBy { it.id }
    var objectives = instance.completedObjectiveIds
    var encounters = instance.triggeredEncounterIds
    for (action in packet.actions) {
      when (action) {
        is MotorAction.ObjectiveComplete -> if (action.objectiveId in knownObjectives) {
          objectives = objectives + action.objectiveId
        }
        is MotorAction.EncounterTriggered -> {
          val encounter = knownEncounters[action.encounterId] ?: continue
          val flagReady = encounter.triggerFlag.isNullOrBlank() || instance.flags[encounter.triggerFlag] == true
          val atLocation = encounter.locationId == instance.currentLocationId
          if (flagReady && atLocation) encounters = encounters + action.encounterId
        }
        else -> Unit
      }
    }
    return instance.copy(completedObjectiveIds = objectives, triggeredEncounterIds = encounters)
  }
}