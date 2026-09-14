package com.braseiro.talesvtt.game

enum class RulesProfile(val wireName:String) {
  TALES_KIDS("tales_kid"),
  FLOOD_TEENS("flood_teen");

  companion object {
    fun fromWire(value:String?):RulesProfile = when(value?.trim()?.lowercase()) {
      "flood_teen", "flood_teens", "things_from_the_flood", "flood" -> FLOOD_TEENS
      else -> TALES_KIDS
    }
  }
}

data class FloodScarState(
  val text:String,
  val accepted:Boolean=false,
  val usedForSuccess:Boolean=false
)

object RulesProfiles {
  fun motorProfile(profile:RulesProfile)=when(profile){
    RulesProfile.TALES_KIDS -> MotorProfile.TALES_KIDS
    RulesProfile.FLOOD_TEENS -> MotorProfile.FLOOD_TEENS
  }
  fun threatTarget(profile:RulesProfile,kids:Int,level:ThreatLevel):Int = kids * when(profile){
    RulesProfile.TALES_KIDS -> when(level){
      ThreatLevel.NORMAL->2; ThreatLevel.DIFFICULT->3; ThreatLevel.ALMOST_IMPOSSIBLE->4
    }
    RulesProfile.FLOOD_TEENS -> when(level){
      ThreatLevel.NORMAL->3; ThreatLevel.DIFFICULT->4; ThreatLevel.ALMOST_IMPOSSIBLE->5
    }
  }
  fun mechanicalContract(profile:RulesProfile)=when(profile){
    RulesProfile.TALES_KIDS -> CanonicalTalesRules.localMechanicalContract()
    RulesProfile.FLOOD_TEENS -> CanonicalFloodRules.localMechanicalContract()
  }
}
