package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.R
import com.braseiro.talesvtt.components.AppPanel
import com.braseiro.talesvtt.components.AppShell
import com.braseiro.talesvtt.components.SecondaryButton
import com.braseiro.talesvtt.components.SectionHeader
import com.braseiro.talesvtt.game.GameState
import com.braseiro.talesvtt.model.AppDestination
import com.braseiro.talesvtt.model.ContextIconType
import com.braseiro.talesvtt.model.ContextItemData
import com.braseiro.talesvtt.model.WorldStatusData
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens
import java.text.Normalizer

private data class MapPoi(
  val name: String,
  val x: Float,
  val y: Float,
  val iconRes: Int
)

private val approvedPois = listOf(
  MapPoi("Escola", 238f / 415f, 126f / 531f, R.drawable.loc_school),
  MapPoi("Casa de Lasse", 105f / 415f, 236f / 531f, R.drawable.loc_home),
  MapPoi("Praça Central", 205f / 415f, 237f / 531f, R.drawable.loc_shop),
  MapPoi("Loja do Kjell", 312f / 415f, 236f / 531f, R.drawable.loc_shop),
  MapPoi("Esconderijo", 121f / 415f, 393f / 531f, R.drawable.loc_hideout),
  MapPoi("Torre de Comunicação", 212f / 415f, 365f / 531f, R.drawable.loc_radio_tower),
  MapPoi("Loop Industries", 202f / 415f, 443f / 531f, R.drawable.loc_loop_facility),
  MapPoi("Estrada Fechada", 322f / 415f, 356f / 531f, R.drawable.state_blocked),
  MapPoi("Antiga Pedreira", 354f / 415f, 407f / 531f, R.drawable.loc_quarry)
)

@Composable
fun MapScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onDeclareTravelIntent: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val contextItems = listOf(
    ContextItemData("map_current", "Local Atual", gameState.scene.location, ContextIconType.LOCATION, isHighlighted = true),
    ContextItemData("map_kids", "Kids Presentes", gameState.group.members.size.toString(), ContextIconType.CHARACTERS),
    ContextItemData("map_light", "Luz do Dia", gameState.scene.daylight, ContextIconType.DAYLIGHT)
  )

  AppShell(
    currentDestination = AppDestination.MAP,
    screenTitle = "Mälaren",
    chapterLabel = "MAPA DOS KIDS",
    sceneLabel = "Exploração & locais conhecidos",
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = gameState.scene.weather,
      time = gameState.scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    ApprovedMapCanvas(gameState)

    Spacer(modifier = Modifier.height(Dimens.spacing8))
    SectionHeader(title = "LOCAIS REVELADOS", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing6))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      RevealedMapCard("Esconderijo", R.drawable.scene_hideout, R.drawable.loc_hideout, Modifier.weight(1f))
      RevealedMapCard("Björkåsen", R.drawable.scene_living_room, R.drawable.loc_home, Modifier.weight(1f))
      RevealedMapCard("Casa de Lasse", R.drawable.scene_family_kitchen, R.drawable.loc_home, Modifier.weight(1f))
      RevealedMapCard("Escola", R.drawable.scene_school_corridor, R.drawable.loc_school, Modifier.weight(1f))
      RevealedMapCard("Loop Industries", R.drawable.scene_loop_gate, R.drawable.loc_loop_facility, Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(Dimens.spacing8))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      AppPanel(
        modifier = Modifier.weight(1f),
        contentPadding = 7.dp,
        borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.55f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            painter = painterResource(R.drawable.state_current_location),
            contentDescription = null,
            modifier = Modifier.size(31.dp),
            contentScale = ContentScale.Fit
          )
          Spacer(modifier = Modifier.width(7.dp))
          Column {
            Text("LOCAL ATUAL", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
            Text(gameState.scene.location, style = AppTheme.typography.cardTitle)
            Text("ROTA · ${gameState.routeHistory.size.coerceAtLeast(1)} parada(s)", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan))
          }
        }
      }

      AppPanel(
        modifier = Modifier.weight(1f),
        contentPadding = 7.dp,
        borderColor = AppTheme.colors.accentAmber.copy(alpha = 0.55f)
      ) {
        Text("AÇÃO NO MAPA", style = AppTheme.typography.label.copy(color = AppTheme.colors.accentAmber))
        Text("Selecionar local", style = AppTheme.typography.cardTitle)
        Text(
          "Veja detalhes e peça a viagem ao Mestre.",
          style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan)
        )
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing10))

    val destinations = gameState.revealed.locations
      .filterNot { it.equals(gameState.scene.location, ignoreCase = true) }
      .sorted()

    if (destinations.isNotEmpty()) {
      destinations.forEach { location ->
        SecondaryButton(
          text = "SOLICITAR VIAGEM · $location",
          onClick = {
            onDeclareTravelIntent("Quero viajar para $location.")
            onNavigate(AppDestination.SCENE)
          },
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
    }

    Spacer(modifier = Modifier.height(Dimens.spacing16))
  }
}

@Composable
private fun ApprovedMapCanvas(gameState: GameState) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .height(531.dp)
      .clip(RoundedCornerShape(6.dp))
      .border(1.dp, AppTheme.colors.accentAmber.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
  ) {
    Image(
      painter = painterResource(R.drawable.map_malaren_clean3),
      contentDescription = "Mapa de Mälaren feito pelos Kids",
      modifier = Modifier.fillMaxWidth().height(531.dp),
      contentScale = ContentScale.FillBounds
    )

    MapTravelRouteOverlay(
      routeHistory = gameState.routeHistory.ifEmpty { listOf(gameState.scene.location) },
      currentLocation = gameState.scene.location,
      modifier = Modifier.fillMaxSize()
    )

    approvedPois.forEach { poi ->
      MapPoiMarker(
        iconRes = poi.iconRes,
        x = maxWidth * poi.x,
        y = 531.dp * poi.y
      )
    }
  }
}

@Composable
private fun MapTravelRouteOverlay(
  routeHistory: List<String>,
  currentLocation: String,
  modifier: Modifier = Modifier
) {
  val routeColor = Color(0xFF9E3E31)
  val currentColor = AppTheme.colors.accentAmber
  val points = routeHistory
    .mapNotNull(::poiForLocation)
    .fold(mutableListOf<MapPoi>()) { acc, poi ->
      if (acc.lastOrNull()?.name != poi.name) acc += poi
      acc
    }
  val current = poiForLocation(currentLocation)

  Canvas(modifier = modifier) {
    if (points.size >= 2) {
      val path = Path().apply {
        val first = points.first()
        moveTo(size.width * first.x, size.height * first.y)
        points.drop(1).forEach { poi -> lineTo(size.width * poi.x, size.height * poi.y) }
      }
      drawPath(
        path = path,
        color = Color.Black.copy(alpha = 0.24f),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
      )
      drawPath(
        path = path,
        color = routeColor.copy(alpha = 0.92f),
        style = Stroke(
          width = 3.dp.toPx(),
          cap = StrokeCap.Round,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(11.dp.toPx(), 5.dp.toPx()))
        )
      )
      points.forEach { poi ->
        val p = Offset(size.width * poi.x, size.height * poi.y)
        drawCircle(Color(0xFFE7D5A8).copy(alpha = 0.9f), radius = 4.6.dp.toPx(), center = p)
        drawCircle(routeColor, radius = 2.4.dp.toPx(), center = p)
      }
    }

    if (current != null) {
      val p = Offset(size.width * current.x, size.height * current.y)
      drawCircle(Color.Black.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = p)
      drawCircle(currentColor.copy(alpha = 0.95f), radius = 7.dp.toPx(), center = p, style = Stroke(width = 2.dp.toPx()))
    }
  }
}

private fun poiForLocation(raw: String): MapPoi? {
  val n = Normalizer.normalize(raw, Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .lowercase()

  val name = when {
    "escola" in n || "bjorkskolan" in n -> "Escola"
    "casa" in n && "lasse" in n -> "Casa de Lasse"
    "praca" in n || "bjorkasen" in n && "central" in n -> "Praça Central"
    "kjell" in n -> "Loja do Kjell"
    "esconderijo" in n -> "Esconderijo"
    "torre" in n && ("comunic" in n || "radio" in n) -> "Torre de Comunicação"
    "loop industries" in n || "portao de servico" in n || "portao" in n && "loop" in n -> "Loop Industries"
    "estrada" in n && ("fechada" in n || "bloque" in n) -> "Estrada Fechada"
    "pedreira" in n -> "Antiga Pedreira"
    else -> null
  }
  return approvedPois.firstOrNull { it.name == name }
}

@Composable
private fun MapPoiMarker(iconRes: Int, x: Dp, y: Dp) {
  val markerSize = 40.dp
  Image(
    painter = painterResource(iconRes),
    contentDescription = null,
    modifier = Modifier
      .offset(x = x - markerSize / 2, y = y - markerSize / 2)
      .size(markerSize),
    contentScale = ContentScale.Fit
  )
}

@Composable
private fun RevealedMapCard(
  title: String,
  imageRes: Int,
  iconRes: Int,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .border(1.dp, AppTheme.colors.accentCyan.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
  ) {
    Image(
      painter = painterResource(imageRes),
      contentDescription = title,
      modifier = Modifier.fillMaxWidth().height(53.dp),
      contentScale = ContentScale.Crop
    )
    Row(
      modifier = Modifier.padding(horizontal = 3.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(13.dp),
        contentScale = ContentScale.Fit
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(title, style = AppTheme.typography.bodySmall, maxLines = 1)
    }
  }
}
