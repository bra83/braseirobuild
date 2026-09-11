package com.braseiro.talesvtt.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.data.SceneImageExchange
import com.braseiro.talesvtt.components.AppIcons
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.ChoiceCard
import com.braseiro.talesvtt.components.FreeActionPrompt
import com.braseiro.talesvtt.components.NarrativePanel
import com.braseiro.talesvtt.components.rememberNarrationTtsController
import com.braseiro.talesvtt.components.SceneImageSlot
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.game.SceneChoiceDefaults
import com.braseiro.talesvtt.game.MotorRuntimeStatus
import com.braseiro.talesvtt.game.SceneIllustrationSelector
import com.braseiro.talesvtt.game.SceneState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun SceneScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  motorStatus: MotorRuntimeStatus,
  onDeclareAction: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val scene = gameState.scene
  val context = LocalContext.current
  val tts = rememberNarrationTtsController()
  val contextItems = listOf(
    ContextItemData(
      id = "loc",
      label = "Local Atual",
      value = scene.location,
      iconType = ContextIconType.LOCATION
    ),
    ContextItemData(
      id = "kids",
      label = "Kids Presentes",
      value = "${gameState.group.members.size} Kids",
      iconType = ContextIconType.CHARACTERS,
      isHighlighted = true
    ),
    ContextItemData(
      id = "light",
      label = "Luz do Dia",
      value = scene.daylight,
      iconType = ContextIconType.DAYLIGHT
    )
  )

  AppShell(
    currentDestination = AppDestination.SCENE,
    screenTitle = scene.title,
    chapterLabel = scene.chapter,
    sceneLabel = scene.location,
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = scene.weather,
      time = scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    SceneImageSlot(
      imageRes = sceneDrawable(scene),
      locationName = scene.location.uppercase(),
      sceneTone = scene.daylight,
      onTtsClick = { tts.speakVisibleNarrative(scene.narrative) },
      onIllustrationClick = {
        val share = SceneImageExchange.prepareShare(context, gameState)
        context.startActivity(Intent.createChooser(share, "Enviar prompt da cena para o Gemini"))
      },
      onCharactersClick = { onNavigate(AppDestination.GROUP) },
      onLocationClick = { onNavigate(AppDestination.MAP) },
      imageUri = scene.importedImageUri
    )

    Spacer(modifier = Modifier.height(Dimens.spacing12))

    NarrativePanel(
      title = "Narrativa do Mestre",
      paragraphs = scene.narrative,
      enableInternalScroll = true,
      maxScrollHeight = 380
    )

    Spacer(modifier = Modifier.height(Dimens.spacing16))

    SectionHeader(
      title = "O QUE VOCÊ FAZ?",
      color = AppTheme.colors.accentCyan
    )

    Spacer(modifier = Modifier.height(Dimens.spacing6))

    Column {
      scene.choices.take(3).ifEmpty { SceneChoiceDefaults.standard() }.forEachIndexed { index, choice ->
        ChoiceCard(
          number = index + 1,
          title = choice.title,
          description = choice.description,
          icon = when (index) {
            0 -> AppIcons.ActionLook
            1 -> AppIcons.ActionMove
            else -> AppIcons.Group
          },
          accentColor = when (index) {
            0 -> AppTheme.colors.accentCyan
            1 -> AppTheme.colors.accentOlive
            else -> AppTheme.colors.accentAmber
          },
          onClick = { if (!motorStatus.busy) onDeclareAction("${choice.title}: ${choice.description}") }
        )
        Spacer(modifier = Modifier.height(Dimens.spacing8))
      }
    }

    FreeActionPrompt(onSubmit = { text -> if (!motorStatus.busy) onDeclareAction(text) })
    Spacer(modifier = Modifier.height(Dimens.spacing6))
    Text(
      text = motorStatus.lastMessage,
      style = AppTheme.typography.bodySmall.copy(
        color = if (motorStatus.busy) AppTheme.colors.accentAmber else if (motorStatus.transportConnected) AppTheme.colors.accentCyan else AppTheme.colors.textSecondary
      )
    )

    Spacer(modifier = Modifier.height(Dimens.spacing24))
  }
}

private fun sceneDrawable(scene: SceneState): Int = when (SceneIllustrationSelector.choose(scene)) {
  "scene_family_kitchen" -> R.drawable.scene_family_kitchen
  "scene_living_room" -> R.drawable.scene_living_room
  "scene_teen_bedroom" -> R.drawable.scene_teen_bedroom
  "scene_school_corridor" -> R.drawable.scene_school_corridor
  "scene_classroom" -> R.drawable.scene_classroom
  "scene_library" -> R.drawable.scene_library
  "scene_hideout" -> R.drawable.scene_hideout
  "scene_forest_path" -> R.drawable.scene_forest_path
  "scene_lakeshore" -> R.drawable.scene_lakeshore
  "scene_farm" -> R.drawable.scene_farm
  "scene_quarry" -> R.drawable.scene_quarry
  "scene_scrapyard" -> R.drawable.scene_scrapyard
  "scene_town_square" -> R.drawable.scene_town_square
  "scene_bus_stop" -> R.drawable.scene_bus_stop
  "scene_convenience_shop" -> R.drawable.scene_convenience_shop
  "scene_gas_station" -> R.drawable.scene_gas_station
  "scene_hospital" -> R.drawable.scene_hospital
  "scene_police_station" -> R.drawable.scene_police_station
  "scene_maintenance_corridor" -> R.drawable.scene_maintenance_corridor
  "scene_control_room" -> R.drawable.scene_control_room
  "scene_reactor_hall" -> R.drawable.scene_reactor_hall
  "scene_antenna_field" -> R.drawable.scene_antenna_field
  "scene_flooded_tunnel" -> R.drawable.scene_flooded_tunnel
  "scene_home_arrival_rain" -> R.drawable.scene_home_arrival_rain
  "scene_kitchen_empty_rain" -> R.drawable.scene_kitchen_empty_rain
  "scene_bedroom_after_school" -> R.drawable.scene_bedroom_after_school
  "scene_homework_rain" -> R.drawable.scene_homework_rain
  "scene_bedroom_music_rain" -> R.drawable.scene_bedroom_music_rain
  "scene_family_argument" -> R.drawable.scene_family_argument
  "scene_bedroom_insomnia" -> R.drawable.scene_bedroom_insomnia
  "scene_bedroom_morning_winter" -> R.drawable.scene_bedroom_morning_winter
  "scene_kitchen_quiet_night" -> R.drawable.scene_kitchen_quiet_night
  "scene_bedroom_empty_retro" -> R.drawable.scene_bedroom_empty_retro
  "scene_bedroom_late_study" -> R.drawable.scene_bedroom_late_study
  "scene_bedroom_journal_dusk" -> R.drawable.scene_bedroom_journal_dusk
  "scene_home_return_evening" -> R.drawable.scene_home_return_evening
  "scene_home_arrival_sunset" -> R.drawable.scene_home_arrival_sunset
  "scene_home_return_heavy_rain" -> R.drawable.scene_home_return_heavy_rain
  "scene_school_arrival_rain" -> R.drawable.scene_school_arrival_rain
  "scene_school_corridor_busy" -> R.drawable.scene_school_corridor_busy
  "scene_classroom_after_hours" -> R.drawable.scene_classroom_after_hours
  "scene_school_yard_rain" -> R.drawable.scene_school_yard_rain
  "scene_library_study_rain" -> R.drawable.scene_library_study_rain
  "scene_school_lab_rain" -> R.drawable.scene_school_lab_rain
  "scene_locker_room_empty" -> R.drawable.scene_locker_room_empty
  "scene_principal_office_rain" -> R.drawable.scene_principal_office_rain
  "scene_school_exit_rain" -> R.drawable.scene_school_exit_rain
  "scene_school_yard_evening" -> R.drawable.scene_school_yard_evening
  "scene_bus_shelter_rain" -> R.drawable.scene_bus_shelter_rain
  "scene_neighborhood_bike_sunset" -> R.drawable.scene_neighborhood_bike_sunset
  "scene_convenience_store_rain" -> R.drawable.scene_convenience_store_rain
  "scene_group_bikes_sunset" -> R.drawable.scene_group_bikes_sunset
  "scene_neighborhood_loop_sunset" -> R.drawable.scene_neighborhood_loop_sunset
  "scene_home_loop_rain" -> R.drawable.scene_home_loop_rain
  "scene_loop_towers_sunset" -> R.drawable.scene_loop_towers_sunset
  "scene_hideout_planning_rain" -> R.drawable.scene_hideout_planning_rain
  "scene_lakeshore_bikes_mist" -> R.drawable.scene_lakeshore_bikes_mist
  "scene_quarry_overlook" -> R.drawable.scene_quarry_overlook
  "scene_abandoned_radar_facility" -> R.drawable.scene_abandoned_radar_facility
  "scene_abandoned_amusement_park" -> R.drawable.scene_abandoned_amusement_park
  "scene_abandoned_river_factory" -> R.drawable.scene_abandoned_river_factory
  "scene_group_bikes_ring_sunset" -> R.drawable.scene_group_bikes_ring_sunset
  "scene_064_basement_party_lights" -> R.drawable.scene_064_basement_party_lights
  "scene_065_teen_porch" -> R.drawable.scene_065_teen_porch
  "scene_066_suburb_dusk" -> R.drawable.scene_066_suburb_dusk
  "scene_067_kids_loop_dusk" -> R.drawable.scene_067_kids_loop_dusk
  "scene_068_anchor_dinner" -> R.drawable.scene_068_anchor_dinner
  "scene_069_teen_room_rain_mystery" -> R.drawable.scene_069_teen_room_rain_mystery
  "scene_070_winter_suburb_bike" -> R.drawable.scene_070_winter_suburb_bike
  "scene_071_diner_rain" -> R.drawable.scene_071_diner_rain
  "scene_072_schoolyard_dusk" -> R.drawable.scene_072_schoolyard_dusk
  "scene_073_garage_dusk" -> R.drawable.scene_073_garage_dusk
  "scene_074_distant_towers" -> R.drawable.scene_074_distant_towers
  "scene_075_waiting_room_comfort" -> R.drawable.scene_075_waiting_room_comfort
  "scene_076_forest_dawn" -> R.drawable.scene_076_forest_dawn
  "scene_077_industrial_tunnel" -> R.drawable.scene_077_industrial_tunnel
  "scene_078_teen_room_snow" -> R.drawable.scene_078_teen_room_snow
  "scene_079_laundromat_rain" -> R.drawable.scene_079_laundromat_rain
  "scene_080_teen_meeting_dusk" -> R.drawable.scene_080_teen_meeting_dusk
  "scene_081_bus_rain_dusk" -> R.drawable.scene_081_bus_rain_dusk
  "scene_082_teens_rain_dusk" -> R.drawable.scene_082_teens_rain_dusk
  "scene_083_departure_dome" -> R.drawable.scene_083_departure_dome
  "scene_084_schoolyard_towers" -> R.drawable.scene_084_schoolyard_towers
  "scene_085_diner_dusk" -> R.drawable.scene_085_diner_dusk
  "scene_086_winter_suburb_tower" -> R.drawable.scene_086_winter_suburb_tower
  "scene_087_substation_teens" -> R.drawable.scene_087_substation_teens
  "scene_088_basement_party" -> R.drawable.scene_088_basement_party
  "scene_089_workshop_dusk" -> R.drawable.scene_089_workshop_dusk
  "scene_090_kids_bikes_fog" -> R.drawable.scene_090_kids_bikes_fog
  "scene_091_teen_room_moon" -> R.drawable.scene_091_teen_room_moon
  "scene_092_railway_tunnel" -> R.drawable.scene_092_railway_tunnel
  "scene_093_waiting_room_cold" -> R.drawable.scene_093_waiting_room_cold
  "scene_094_teen_loop_dusk" -> R.drawable.scene_094_teen_loop_dusk
  "scene_095_bus_sunset" -> R.drawable.scene_095_bus_sunset
  "scene_096_kids_bikes_loop" -> R.drawable.scene_096_kids_bikes_loop
  "scene_097_kiosk_ring" -> R.drawable.scene_097_kiosk_ring
  "scene_098_youth_frozen_loop" -> R.drawable.scene_098_youth_frozen_loop
  "scene_099_suburb_edge_mystery" -> R.drawable.scene_099_suburb_edge_mystery
  "scene_100_teen_room_rain_loop" -> R.drawable.scene_100_teen_room_rain_loop
  else -> R.drawable.scene_loop_gate
}
