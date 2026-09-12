package com.braseiro.talesvtt.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.hypot

private enum class MapFilterMode { REVEALED, VISITED, CURRENT }

private data class MapPoi(
  val name: String,
  val x: Float,
  val y: Float,
  val iconRes: Int,
  val imageRes: Int
)

private val approvedPois = listOf(
  MapPoi("Escola", 238f / 415f, 126f / 531f, R.drawable.loc_school, R.drawable.scene_school_corridor),
  MapPoi("Casa de Lasse", 105f / 415f, 236f / 531f, R.drawable.loc_home, R.drawable.scene_family_kitchen),
  MapPoi("Praça Central", 205f / 415f, 237f / 531f, R.drawable.loc_shop, R.drawable.scene_town_square),
  MapPoi("Loja do Kjell", 312f / 415f, 236f / 531f, R.drawable.loc_shop, R.drawable.scene_living_room),
  MapPoi("Esconderijo", 121f / 415f, 393f / 531f, R.drawable.loc_hideout, R.drawable.scene_hideout),
  MapPoi("Torre de Comunicação", 212f / 415f, 365f / 531f, R.drawable.loc_radio_tower, R.drawable.scene_loop_gate),
  MapPoi("Loop Industries", 202f / 415f, 443f / 531f, R.drawable.loc_loop_facility, R.drawable.scene_loop_gate),
  MapPoi("Estrada Fechada", 322f / 415f, 356f / 531f, R.drawable.state_blocked, R.drawable.scene_town_square),
  MapPoi("Antiga Pedreira", 354f / 415f, 407f / 531f, R.drawable.loc_quarry, R.drawable.scene_quarry)
)

@Composable
fun MapScreen(
  onNavigate: (AppDestination) -> Unit,
  gameState: GameState,
  onDeclareTravelIntent: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedPoiName by rememberSaveable { mutableStateOf<String?>(null) }
  var filterModeName by rememberSaveable { mutableStateOf(MapFilterMode.REVEALED.name) }
  var showConnections by rememberSaveable { mutableStateOf(true) }
  var showNotes by rememberSaveable { mutableStateOf(false) }
  val filterMode = runCatching { MapFilterMode.valueOf(filterModeName) }.getOrDefault(MapFilterMode.REVEALED)

  val contextItems = listOf(
    ContextItemData("map_current", "Local Atual", gameState.scene.location, ContextIconType.LOCATION, isHighlighted = true),
    ContextItemData("map_kids", "Kids Presentes", gameState.group.members.size.toString(), ContextIconType.CHARACTERS),
    ContextItemData("map_light", "Luz do Dia", gameState.scene.daylight, ContextIconType.DAYLIGHT)
  )

  val revealedNames = gameState.revealed.locations.mapNotNull(::poiForLocation).map { it.name }.toSet()
  val visitedNames = gameState.routeHistory.mapNotNull(::poiForLocation).map { it.name }.toSet()
  val currentPoi = poiForLocation(gameState.scene.location)
  val selectedPoi = approvedPois.firstOrNull { it.name == selectedPoiName }

  AppShell(
    currentDestination = AppDestination.MAP,
    screenTitle = "Mälaren",
    chapterLabel = "MAPA DOS KIDS",
    sceneLabel = "Toque nos pinos, filtros e anotações",
    worldStatus = WorldStatusData(
      seasonYear = gameState.scene.seasonYear,
      weather = gameState.scene.weather,
      time = gameState.scene.time
    ),
    contextItems = contextItems,
    onNavigate = onNavigate,
    modifier = modifier
  ) {
    InteractiveMapCanvas(
      gameState = gameState,
      filterMode = filterMode,
      showConnections = showConnections,
      showNotes = showNotes,
      selectedPoiName = selectedPoiName,
      onSelectPoi = { selectedPoiName = it },
      onCycleFilter = {
        filterModeName = when (filterMode) {
          MapFilterMode.REVEALED -> MapFilterMode.VISITED.name
          MapFilterMode.VISITED -> MapFilterMode.CURRENT.name
          MapFilterMode.CURRENT -> MapFilterMode.REVEALED.name
        }
      },
      onToggleConnections = { showConnections = !showConnections },
      onToggleNotes = { showNotes = !showNotes }
    )

    if (selectedPoi != null) {
      Spacer(modifier = Modifier.height(8.dp))
      SelectedLocationPanel(
        poi = selectedPoi,
        isCurrent = currentPoi?.name == selectedPoi.name,
        isVisited = selectedPoi.name in visitedNames,
        isRevealed = selectedPoi.name in revealedNames,
        onTravel = {
          onDeclareTravelIntent("Quero viajar para ${selectedPoi.name}.")
          onNavigate(AppDestination.SCENE)
        }
      )
    }

    Spacer(modifier = Modifier.height(Dimens.spacing8))
    SectionHeader(title = "LOCAIS REVELADOS", color = AppTheme.colors.accentCyan)
    Spacer(modifier = Modifier.height(Dimens.spacing6))

    val cards = approvedPois.filter { poi ->
      poi.name in revealedNames || poi.name in visitedNames || currentPoi?.name == poi.name
    }
    if (cards.isNotEmpty()) {
      cards.chunked(3).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          rowItems.forEach { poi ->
            RevealedMapCard(
              title = poi.name,
              imageRes = poi.imageRes,
              iconRes = poi.iconRes,
              selected = selectedPoiName == poi.name,
              onClick = { selectedPoiName = poi.name },
              modifier = Modifier.weight(1f)
            )
          }
          repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(5.dp))
      }
    }

    val unmappedDestinations = gameState.revealed.locations
      .filter { poiForLocation(it) == null }
      .filterNot { it.equals(gameState.scene.location, ignoreCase = true) }
      .sorted()

    if (unmappedDestinations.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      unmappedDestinations.forEach { location ->
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
private fun InteractiveMapCanvas(
  gameState: GameState,
  filterMode: MapFilterMode,
  showConnections: Boolean,
  showNotes: Boolean,
  selectedPoiName: String?,
  onSelectPoi: (String) -> Unit,
  onCycleFilter: () -> Unit,
  onToggleConnections: () -> Unit,
  onToggleNotes: () -> Unit
) {
  val revealedNames = gameState.revealed.locations.mapNotNull(::poiForLocation).map { it.name }.toSet()
  val visitedNames = gameState.routeHistory.mapNotNull(::poiForLocation).map { it.name }.toSet()
  val currentPoi = poiForLocation(gameState.scene.location)
  val visiblePois = approvedPois.filter { poi ->
    when (filterMode) {
      MapFilterMode.REVEALED -> poi.name in revealedNames || poi.name in visitedNames || currentPoi?.name == poi.name
      MapFilterMode.VISITED -> poi.name in visitedNames || currentPoi?.name == poi.name
      MapFilterMode.CURRENT -> currentPoi?.name == poi.name
    }
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .height(531.dp)
      .clip(RoundedCornerShape(6.dp))
      .border(1.dp, AppTheme.colors.accentAmber.copy(alpha = 0.40f), RoundedCornerShape(6.dp))
  ) {
    val mapWidthPx = constraints.maxWidth.toFloat()
    val mapHeightPx = constraints.maxHeight.toFloat()

    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(visiblePois, mapWidthPx, mapHeightPx) {
          detectTapGestures { tap ->
            val nearest = visiblePois.minByOrNull { poi ->
              hypot(tap.x - mapWidthPx * poi.x, tap.y - mapHeightPx * poi.y)
            }
            if (nearest != null) {
              val distance = hypot(tap.x - mapWidthPx * nearest.x, tap.y - mapHeightPx * nearest.y)
              if (distance <= 44.dp.toPx()) onSelectPoi(nearest.name)
            }
          }
        }
    ) {
      Image(
        painter = painterResource(R.drawable.map_malaren_clean3),
        contentDescription = "Mapa de Mälaren feito pelos Kids",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
      )

      MapTravelRouteOverlay(
        routeHistory = gameState.routeHistory.ifEmpty { listOf(gameState.scene.location) },
        currentLocation = gameState.scene.location,
        modifier = Modifier.fillMaxSize()
      )

      if (showConnections) {
        MapConnectionsOverlay(
          connections = gameState.revealed.connections,
          modifier = Modifier.fillMaxSize()
        )
      }

      visiblePois.forEach { poi ->
        MapPoiMarker(
          poi = poi,
          selected = selectedPoiName == poi.name,
          isCurrent = currentPoi?.name == poi.name,
          x = maxWidth * poi.x,
          y = 531.dp * poi.y,
          onClick = { onSelectPoi(poi.name) }
        )
      }
    }

    MapControlHotspot(
      x = 5.dp,
      y = 181.dp,
      width = 80.dp,
      height = 38.dp,
      active = true,
      onClick = onCycleFilter
    )
    MapControlHotspot(
      x = 5.dp,
      y = 221.dp,
      width = 80.dp,
      height = 38.dp,
      active = showConnections,
      onClick = onToggleConnections
    )
    MapControlHotspot(
      x = 5.dp,
      y = 261.dp,
      width = 80.dp,
      height = 38.dp,
      active = showNotes,
      onClick = onToggleNotes
    )

    Text(
      text = when (filterMode) {
        MapFilterMode.REVEALED -> "REVELADOS"
        MapFilterMode.VISITED -> "VISITADOS"
        MapFilterMode.CURRENT -> "LOCAL ATUAL"
      },
      style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 6.dp)
        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp)
    )

    if (showNotes) {
      val noteLines = (gameState.revealed.clues.toList() + gameState.group.notes)
        .filter { it.isNotBlank() }
        .distinct()
        .takeLast(3)
      Column(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(8.dp)
          .width(165.dp)
          .background(Color(0xFFE5D3A6).copy(alpha = 0.94f), RoundedCornerShape(3.dp))
          .padding(7.dp)
      ) {
        Text("ANOTAÇÕES DOS KIDS", style = AppTheme.typography.label.copy(color = Color(0xFF342619)))
        if (noteLines.isEmpty()) {
          Text("Ainda não há pistas ou notas reveladas.", style = AppTheme.typography.bodySmall.copy(color = Color(0xFF342619)))
        } else {
          noteLines.forEach { Text("• $it", style = AppTheme.typography.bodySmall.copy(color = Color(0xFF342619))) }
        }
      }
    }
  }
}

@Composable
private fun MapControlHotspot(
  x: Dp,
  y: Dp,
  width: Dp,
  height: Dp,
  active: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .offset(x, y)
      .size(width, height)
      .clip(RoundedCornerShape(7.dp))
      .clickable(onClick = onClick)
      .border(
        width = if (active) 1.5.dp else 1.dp,
        color = if (active) AppTheme.colors.accentCyan.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f),
        shape = RoundedCornerShape(7.dp)
      )
  )
}

@Composable
private fun SelectedLocationPanel(
  poi: MapPoi,
  isCurrent: Boolean,
  isVisited: Boolean,
  isRevealed: Boolean,
  onTravel: () -> Unit
) {
  AppPanel(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = 9.dp,
    borderColor = AppTheme.colors.accentCyan.copy(alpha = 0.55f)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(poi.iconRes),
        contentDescription = null,
        modifier = Modifier.size(36.dp),
        contentScale = ContentScale.Fit
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(poi.name.uppercase(), style = AppTheme.typography.cardTitle)
        val status = when {
          isCurrent -> "VOCÊS ESTÃO AQUI"
          isVisited -> "VISITADO"
          isRevealed -> "REVELADO"
          else -> "DESCONHECIDO"
        }
        Text(status, style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.accentCyan))
      }
    }
    if (!isCurrent && (isVisited || isRevealed)) {
      Spacer(modifier = Modifier.height(7.dp))
      SecondaryButton(
        text = "IR PARA ${poi.name.uppercase()}",
        onClick = onTravel,
        modifier = Modifier.fillMaxWidth()
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

@Composable
private fun MapConnectionsOverlay(
  connections: Set<Pair<String, String>>,
  modifier: Modifier = Modifier
) {
  val drawable = connections.mapNotNull { (a, b) ->
    val p1 = poiForLocation(a)
    val p2 = poiForLocation(b)
    if (p1 != null && p2 != null) p1 to p2 else null
  }
  Canvas(modifier = modifier) {
    drawable.forEach { (a, b) ->
      drawLine(
        color = AppTheme.colors.accentCyan.copy(alpha = 0.72f),
        start = Offset(size.width * a.x, size.height * a.y),
        end = Offset(size.width * b.x, size.height * b.y),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
      )
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
    "praca" in n || ("bjorkasen" in n && "central" in n) -> "Praça Central"
    "kjell" in n -> "Loja do Kjell"
    "esconderijo" in n -> "Esconderijo"
    "torre" in n && ("comunic" in n || "radio" in n) -> "Torre de Comunicação"
    "loop industries" in n || "portao de servico" in n || ("portao" in n && "loop" in n) -> "Loop Industries"
    "estrada" in n && ("fechada" in n || "bloque" in n) -> "Estrada Fechada"
    "pedreira" in n -> "Antiga Pedreira"
    else -> null
  }
  return approvedPois.firstOrNull { it.name == name }
}

@Composable
private fun MapPoiMarker(
  poi: MapPoi,
  selected: Boolean,
  isCurrent: Boolean,
  x: Dp,
  y: Dp,
  onClick: () -> Unit
) {
  val hitSize = 58.dp
  val markerSize = 40.dp
  Box(
    modifier = Modifier
      .offset(x = x - hitSize / 2, y = y - hitSize / 2)
      .size(hitSize)
      .clip(CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    if (selected || isCurrent) {
      Box(
        modifier = Modifier
          .size(50.dp)
          .border(
            2.dp,
            if (isCurrent) AppTheme.colors.accentAmber else AppTheme.colors.accentCyan,
            CircleShape
          )
      )
    }
    Image(
      painter = painterResource(poi.iconRes),
      contentDescription = poi.name,
      modifier = Modifier.size(markerSize),
      contentScale = ContentScale.Fit
    )
  }
}

@Composable
private fun RevealedMapCard(
  title: String,
  imageRes: Int,
  iconRes: Int,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
      .border(
        if (selected) 2.dp else 1.dp,
        if (selected) AppTheme.colors.accentAmber else AppTheme.colors.accentCyan.copy(alpha = 0.55f),
        RoundedCornerShape(6.dp)
      )
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
