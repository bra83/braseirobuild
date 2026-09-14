package com.braseiro.talesvtt.game

/**
 * Typed contract between the autonomous GM (Motor Barbara) and the Tales/Kids engine.
 * The transport may be Gemini, local mock, or another provider; none of them may mutate
 * GameState directly. Every directive passes through this firewall first.
 */
enum class MotorProfile { TALES_KIDS }

enum class MotorRouteHint { NONE, SCENE, TROUBLE, CLIMAX, ENDING }
enum class RecoveryKind { ANCHOR, HIDEOUT, COMFORT }

data class PrivateClue(
  val id: String,
  val title: String,
  val text: String = "",
  val location: String = ""
)

data class PrivateConnection(
  val id: String,
  val from: String,
  val to: String
)

data class PrivateLocation(
  val id: String,
  val name: String
)

data class GmPrivateState(
  val clues: Map<String, PrivateClue> = emptyMap(),
  val connections: Map<String, PrivateConnection> = emptyMap(),
  val locations: Map<String, PrivateLocation> = emptyMap()
)

sealed interface MotorAction {
  data class Narration(val text: String, val choices: List<SceneChoice> = emptyList()) : MotorAction
  data class WorldPatch(
    val location: String? = null,
    val weather: String? = null,
    val time: String? = null,
    val daylight: String? = null,
    val visualAssetKey: String? = null
  ) : MotorAction
  data class TestRequest(val skill:String,val requiredSuccesses:Int,val label:String,val purpose:TestPurpose=TestPurpose.STANDARD,val targetKid:String?=null) : MotorAction
  data object TestClear : MotorAction
  data class ApproveHelp(val helperKid: String) : MotorAction
  data object ApproveIconicItem : MotorAction
  data class ApproveMysteryItem(val itemName: String, val bonus: Int) : MotorAction
  data class GiveItem(val item: KidItemState) : MotorAction
  data class RemoveItem(val itemName: String) : MotorAction
  data class ApplyCondition(val condition: Condition) : MotorAction
  data object BreakKid : MotorAction
  data class Recovery(
    val approved: Boolean,
    val kind: RecoveryKind? = null,
    val condition: Condition? = null
  ) : MotorAction
  data class SecretClueAdd(val clue: PrivateClue) : MotorAction
  data class SecretConnectionAdd(val connection: PrivateConnection) : MotorAction
  data class SecretLocationAdd(val location: PrivateLocation) : MotorAction
  data class RevealClue(val clueId: String) : MotorAction
  data class RevealConnection(val connectionId: String) : MotorAction
  data class RevealLocation(val locationId: String) : MotorAction
  data class StartExtendedTrouble(
    val stakes: String,
    val threatLevel: ThreatLevel,
    val scenes: List<Pair<String, String>>
  ) : MotorAction
  data class ExtendedSceneResult(val kid: String, val successes: Int) : MotorAction
  data object EndExtendedTrouble : MotorAction
  data object SessionEnd : MotorAction
  data object MysteryEnd : MotorAction
  data class Message(val text: String) : MotorAction
}

data class MotorPacket(
  val profile: MotorProfile = MotorProfile.TALES_KIDS,
  val actions: List<MotorAction>
)

data class MotorApplyResult(
  val gameState: GameState,
  val privateState: GmPrivateState,
  val routeHint: MotorRouteHint,
  val acceptedActions: Int,
  val rejected: List<String>
) {
  val isClean: Boolean get() = rejected.isEmpty()
}

object MotorBarbaraFirewall {
  val allowedSceneAssets: Set<String> = setOf(
    "scene_family_kitchen",
    "scene_living_room",
    "scene_teen_bedroom",
    "scene_school_corridor",
    "scene_classroom",
    "scene_library",
    "scene_hideout",
    "scene_forest_path",
    "scene_lakeshore",
    "scene_farm",
    "scene_quarry",
    "scene_scrapyard",
    "scene_town_square",
    "scene_bus_stop",
    "scene_convenience_shop",
    "scene_gas_station",
    "scene_hospital",
    "scene_police_station",
    "scene_loop_gate",
    "scene_maintenance_corridor",
    "scene_control_room",
    "scene_reactor_hall",
    "scene_antenna_field",
    "scene_flooded_tunnel",
    "scene_home_arrival_rain",
    "scene_kitchen_empty_rain",
    "scene_bedroom_after_school",
    "scene_homework_rain",
    "scene_bedroom_music_rain",
    "scene_family_argument",
    "scene_bedroom_insomnia",
    "scene_bedroom_morning_winter",
    "scene_kitchen_quiet_night",
    "scene_bedroom_empty_retro",
    "scene_bedroom_late_study",
    "scene_bedroom_journal_dusk",
    "scene_home_return_evening",
    "scene_home_arrival_sunset",
    "scene_home_return_heavy_rain",
    "scene_school_arrival_rain",
    "scene_school_corridor_busy",
    "scene_classroom_after_hours",
    "scene_school_yard_rain",
    "scene_library_study_rain",
    "scene_school_lab_rain",
    "scene_locker_room_empty",
    "scene_principal_office_rain",
    "scene_school_exit_rain",
    "scene_school_yard_evening",
    "scene_bus_shelter_rain",
    "scene_neighborhood_bike_sunset",
    "scene_convenience_store_rain",
    "scene_group_bikes_sunset",
    "scene_neighborhood_loop_sunset",
    "scene_home_loop_rain",
    "scene_loop_towers_sunset",
    "scene_hideout_planning_rain",
    "scene_lakeshore_bikes_mist",
    "scene_quarry_overlook",
    "scene_abandoned_radar_facility",
    "scene_abandoned_amusement_park",
    "scene_abandoned_river_factory",
    "scene_group_bikes_ring_sunset",
    "scene_064_basement_party_lights",
    "scene_065_teen_porch",
    "scene_066_suburb_dusk",
    "scene_067_kids_loop_dusk",
    "scene_068_anchor_dinner",
    "scene_069_teen_room_rain_mystery",
    "scene_070_winter_suburb_bike",
    "scene_071_diner_rain",
    "scene_072_schoolyard_dusk",
    "scene_073_garage_dusk",
    "scene_074_distant_towers",
    "scene_075_waiting_room_comfort",
    "scene_076_forest_dawn",
    "scene_077_industrial_tunnel",
    "scene_078_teen_room_snow",
    "scene_079_laundromat_rain",
    "scene_080_teen_meeting_dusk",
    "scene_081_bus_rain_dusk",
    "scene_082_teens_rain_dusk",
    "scene_083_departure_dome",
    "scene_084_schoolyard_towers",
    "scene_085_diner_dusk",
    "scene_086_winter_suburb_tower",
    "scene_087_substation_teens",
    "scene_088_basement_party",
    "scene_089_workshop_dusk",
    "scene_090_kids_bikes_fog",
    "scene_091_teen_room_moon",
    "scene_092_railway_tunnel",
    "scene_093_waiting_room_cold",
    "scene_094_teen_loop_dusk",
    "scene_095_bus_sunset",
    "scene_096_kids_bikes_loop",
    "scene_097_kiosk_ring",
    "scene_098_youth_frozen_loop",
    "scene_099_suburb_edge_mystery",
    "scene_100_teen_room_rain_loop"
  )

  fun apply(
    gameState: GameState,
    privateState: GmPrivateState,
    packet: MotorPacket
  ): MotorApplyResult {
    if (packet.profile != MotorProfile.TALES_KIDS) {
      return MotorApplyResult(
        gameState = gameState,
        privateState = privateState,
        routeHint = MotorRouteHint.NONE,
        acceptedActions = 0,
        rejected = listOf("Perfil mecânico incompatível: somente TALES_KIDS é aceito neste projeto.")
      )
    }

    var state = gameState
    var gm = privateState
    var route = MotorRouteHint.NONE
    var accepted = 0
    val rejected = mutableListOf<String>()

    fun reduce(event: GameEvent, name: String): Boolean {
      val next = TalesReducer.reduce(state, event)
      if (next.lastError != null) {
        rejected += "$name: ${next.lastError}"
        return false
      }
      state = next
      accepted++
      return true
    }

    fun raiseRoute(candidate: MotorRouteHint) {
      val priority = mapOf(
        MotorRouteHint.NONE to 0,
        MotorRouteHint.SCENE to 1,
        MotorRouteHint.ENDING to 2,
        MotorRouteHint.CLIMAX to 3,
        MotorRouteHint.TROUBLE to 4
      )
      if ((priority[candidate] ?: 0) > (priority[route] ?: 0)) route = candidate
    }

    packet.actions.forEachIndexed { index, action ->
      when (action) {
        is MotorAction.Narration -> {
          if (action.text.isBlank()) rejected += "ação[$index] narration: texto vazio."
          else {
            val safeChoices = action.choices
              .filter { it.title.isNotBlank() && it.description.isNotBlank() }
              .take(3)
              .ifEmpty { SceneChoiceDefaults.standard() }
            if (reduce(GameEvent.GmNarration(action.text, safeChoices), "narration")) raiseRoute(MotorRouteHint.SCENE)
          }
        }

        is MotorAction.WorldPatch -> {
          if (action.location != null && action.location !in state.revealed.locations) {
            rejected += "ação[$index] world_patch: local ainda não revelado ao jogador."
          } else if (action.visualAssetKey != null && action.visualAssetKey.removeSuffix(".png") !in allowedSceneAssets) {
            rejected += "ação[$index] world_patch: asset de cena fora da lista canônica incorporada."
          } else if (reduce(
              GameEvent.GmWorldPatch(
                location = action.location,
                weather = action.weather,
                time = action.time,
                daylight = action.daylight,
                visualAssetKey = action.visualAssetKey
              ),
              "world_patch"
            )) {
            raiseRoute(MotorRouteHint.SCENE)
          }
        }

        is MotorAction.TestRequest -> {
          val canonical = TalesSkillCatalog.canonicalSkill(action.skill)
          val request = canonical?.let {
            state.kid.testForSkill(
              reason = action.label.trim().ifBlank { canonical },
              skill = canonical,
              requiredSuccesses = action.requiredSuccesses
            )?.copy(purpose=action.purpose,targetKid=action.targetKid)
          }
          if (action.requiredSuccesses !in 1..3) rejected += "ação[$index] test_request: dificuldade deve ser 1, 2 ou 3 sucessos."
          else if (request == null) rejected += "ação[$index] test_request: Perícia não canônica para Tales/Kids."
          else if (reduce(GameEvent.GmRequestTest(request), "test_request")) raiseRoute(MotorRouteHint.TROUBLE)
        }

        MotorAction.TestClear -> {
          val wasExtended = state.activeTest?.extendedSceneIndex != null
          if (reduce(GameEvent.ClearTest, "test_clear")) {
            raiseRoute(if (wasExtended) MotorRouteHint.CLIMAX else MotorRouteHint.SCENE)
          }
        }

        is MotorAction.ApproveHelp -> {
          val helper = action.helperKid.trim()
          val activeKidName = state.kid.name
          val knownMember = state.group.members.firstOrNull {
            it.name.equals(helper, ignoreCase = true) || it.name.substringBefore(' ').equals(helper, ignoreCase = true)
          }
          if (knownMember == null) rejected += "ação[$index] help_approve: ajudante não pertence ao grupo."
          else if (knownMember.name.equals(activeKidName, ignoreCase = true)) rejected += "ação[$index] help_approve: o Kid não pode ajudar a si mesmo."
          else if (reduce(GameEvent.ApplyHelp(knownMember.name), "help_approve")) raiseRoute(MotorRouteHint.TROUBLE)
        }

        MotorAction.ApproveIconicItem -> {
          val test = state.activeTest
          if (test == null) rejected += "ação[$index] iconic_item_approve: não há teste ativo."
          else if (state.testResolution != null) rejected += "ação[$index] iconic_item_approve: item deve ser aprovado antes da rolagem."
          else if (test.approvedItemBonus != 0) rejected += "ação[$index] iconic_item_approve: já existe item aplicado ao teste."
          else if (reduce(GameEvent.ApplyApprovedItem(2), "iconic_item_approve")) raiseRoute(MotorRouteHint.TROUBLE)
        }

        is MotorAction.ApproveMysteryItem -> {
          val test = state.activeTest
          val item = state.kid.items.firstOrNull { it.name.equals(action.itemName.trim(), ignoreCase = true) }
          when {
            test == null -> rejected += "ação[$index] mystery_item_approve: não há teste ativo."
            state.testResolution != null -> rejected += "ação[$index] mystery_item_approve: item deve ser aprovado antes da rolagem."
            test.approvedItemBonus != 0 -> rejected += "ação[$index] mystery_item_approve: já existe item aplicado ao teste."
            item == null -> rejected += "ação[$index] mystery_item_approve: item não pertence ao Kid."
            item.kind != KidItemKind.MYSTERY -> rejected += "ação[$index] mystery_item_approve: objeto comum não possui bônus automático."
            item.bonus != action.bonus || !CanonicalTalesRules.validMysteryItemBonus(action.bonus) -> rejected += "ação[$index] mystery_item_approve: bônus não corresponde ao Item real."
            else -> if (reduce(GameEvent.ApplyApprovedItem(item.bonus), "mystery_item_approve")) raiseRoute(MotorRouteHint.TROUBLE)
          }
        }

        is MotorAction.GiveItem -> {
          val test=state.activeTest; val result=state.testResolution
          val creating=test?.purpose in setOf(TestPurpose.BUILD_ITEM,TestPurpose.PROGRAM_ITEM) && result!=null
          val upgrades=result?.bonusEffects?.count{CanonicalTalesRules.effect(it.effectId)?.kind==BonusEffectKind.ITEM_BONUS_UPGRADE} ?: 0
          val expected=(1+upgrades).coerceAtMost(3)
          when {
            !CanonicalTalesRules.validItem(action.item) -> rejected += "ação[$index] item_add: Item viola bônus/tipo canônico."
            creating && (action.item.kind!=KidItemKind.MYSTERY || action.item.bonus!=expected) -> rejected += "ação[$index] item_add: Item criado deve ter bônus local +$expected."
            else -> reduce(GameEvent.AddItem(action.item),"item_add")
          }
        }

        is MotorAction.RemoveItem -> {
          if (reduce(GameEvent.RemoveItem(action.itemName), "item_remove")) raiseRoute(MotorRouteHint.SCENE)
        }

        is MotorAction.ApplyCondition -> reduce(GameEvent.ApplyCondition(action.condition), "condition")

        MotorAction.BreakKid -> reduce(GameEvent.BecomeBroken, "broken")

        is MotorAction.Recovery -> {
          if (!action.approved) {
            accepted++ // explicit denial is a valid no-op decision from the GM
          } else when (action.kind) {
            RecoveryKind.ANCHOR -> {
              if (reduce(GameEvent.HealAllConditions, "recovery_anchor")) raiseRoute(MotorRouteHint.SCENE)
            }
            RecoveryKind.HIDEOUT -> {
              if (state.group.members.size < 2) rejected += "ação[$index] recovery_hideout: exige ao menos 2 Kids na cena qualificada."
              else if (reduce(GameEvent.HealAllConditions, "recovery_hideout")) raiseRoute(MotorRouteHint.SCENE)
            }
            RecoveryKind.COMFORT -> {
              when {
                state.kid.broken -> rejected += "ação[$index] recovery_comfort: conforto por Perícia não cura Kid Quebrado."
                action.condition == null -> rejected += "ação[$index] recovery_comfort: informe exatamente uma Condição leve."
                else -> if (reduce(GameEvent.HealCondition(action.condition), "recovery_comfort")) raiseRoute(MotorRouteHint.SCENE)
              }
            }
            null -> rejected += "ação[$index] recovery: fonte de recuperação ausente; não existe cura genérica."
          }
        }

        is MotorAction.SecretClueAdd -> {
          val c = action.clue
          if (c.id.isBlank() || c.title.isBlank()) rejected += "ação[$index] secret_clue_add: id/título inválidos."
          else if (gm.clues.containsKey(c.id)) rejected += "ação[$index] secret_clue_add: id duplicado."
          else {
            gm = gm.copy(clues = gm.clues + (c.id to c))
            accepted++
          }
        }

        is MotorAction.SecretConnectionAdd -> {
          val c = action.connection
          if (c.id.isBlank() || c.from.isBlank() || c.to.isBlank()) rejected += "ação[$index] secret_connection_add: conexão inválida."
          else if (gm.connections.containsKey(c.id)) rejected += "ação[$index] secret_connection_add: id duplicado."
          else {
            gm = gm.copy(connections = gm.connections + (c.id to c))
            accepted++
          }
        }

        is MotorAction.SecretLocationAdd -> {
          val l = action.location
          if (l.id.isBlank() || l.name.isBlank()) rejected += "ação[$index] secret_location_add: local inválido."
          else if (gm.locations.containsKey(l.id)) rejected += "ação[$index] secret_location_add: id duplicado."
          else {
            gm = gm.copy(locations = gm.locations + (l.id to l))
            accepted++
          }
        }

        is MotorAction.RevealClue -> {
          val clue = gm.clues[action.clueId]
          if (clue == null) rejected += "ação[$index] reveal_clue: pista privada inexistente."
          else if (reduce(GameEvent.RevealClue(clue.title), "reveal_clue")) raiseRoute(MotorRouteHint.SCENE)
        }

        is MotorAction.RevealLocation -> {
          val location = gm.locations[action.locationId]
          if (location == null) rejected += "ação[$index] reveal_location: local privado inexistente."
          else if (reduce(GameEvent.RevealLocation(location.name), "reveal_location")) raiseRoute(MotorRouteHint.SCENE)
        }

        is MotorAction.RevealConnection -> {
          val connection = gm.connections[action.connectionId]
          if (connection == null) rejected += "ação[$index] reveal_connection: conexão privada inexistente."
          else if (reduce(GameEvent.RevealConnection(connection.from, connection.to), "reveal_connection")) raiseRoute(MotorRouteHint.SCENE)
        }

        is MotorAction.StartExtendedTrouble -> {
          if (action.stakes.isBlank()) rejected += "ação[$index] extended: apostas vazias."
          else if (action.scenes.isEmpty()) rejected += "ação[$index] extended: precisa de uma cena por Kid."
          else if (action.scenes.any { (_, skill) -> TalesSkillCatalog.canonicalSkill(skill) == null }) {
            rejected += "ação[$index] extended: Perícia não canônica."
          } else {
            val scenes = action.scenes.map { (kid, skill) ->
              ExtendedKidScene(kid = kid, skill = TalesSkillCatalog.canonicalSkill(skill)!!)
            }
            if (reduce(
                GameEvent.StartExtendedTrouble(
                  stakes = action.stakes,
                  kids = scenes.size,
                  threatLevel = action.threatLevel,
                  scenes = scenes
                ),
                "extended"
              )) raiseRoute(MotorRouteHint.CLIMAX)
          }
        }

        is MotorAction.ExtendedSceneResult -> {
          val ext = state.extended
          val sceneIndex = ext.nextUnresolvedIndex
          if (!ext.active || sceneIndex == null) rejected += "ação[$index] extended_scene_result: não há cena pendente."
          else {
            val expected = ext.scenes[sceneIndex]
            val kidMatches = expected.kid.equals(action.kid, ignoreCase = true) || expected.kid.substringBefore(' ').equals(action.kid, ignoreCase = true)
            if (!kidMatches) rejected += "extended_scene_result: o resultado não pertence ao próximo Kid da ordem."
            else if (action.successes < 0) rejected += "extended_scene_result: sucessos inválidos."
            else if (reduce(GameEvent.ResolveExtendedKidScene(sceneIndex, action.successes), "extended_scene_result")) raiseRoute(MotorRouteHint.CLIMAX)
          }
        }

        MotorAction.EndExtendedTrouble -> {
          if (reduce(GameEvent.EndExtendedTrouble, "extended_end")) raiseRoute(MotorRouteHint.SCENE)
        }

        MotorAction.SessionEnd -> {
          if (reduce(GameEvent.OpenEnding(EndingMode.SESSION), "session_end")) raiseRoute(MotorRouteHint.ENDING)
        }

        MotorAction.MysteryEnd -> {
          // Motor opens the Change/XP flow, but never awards XP by itself.
          if (reduce(GameEvent.OpenEnding(EndingMode.MYSTERY), "mystery_end")) raiseRoute(MotorRouteHint.ENDING)
        }

        is MotorAction.Message -> {
          // Diagnostic/status text is not authoritative game state.
          if (action.text.isBlank()) rejected += "ação[$index] message: texto vazio." else accepted++
        }
      }
    }

    return MotorApplyResult(
      gameState = state,
      privateState = gm,
      routeHint = route,
      acceptedActions = accepted,
      rejected = rejected
    )
  }
}
