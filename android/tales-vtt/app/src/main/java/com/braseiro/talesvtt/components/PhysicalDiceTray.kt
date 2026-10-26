package com.braseiro.talesvtt.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.braseiro.talesvtt.ui.theme.AppTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Visual dice settle on the exact values provided by the deterministic game engine.
 * The animation is presentation only; it does not change, reroll or fake the results.
 */
@Composable
fun PhysicalDiceTray(dice: List<Int>, modifier: Modifier = Modifier) {
  val progress = remember(dice) { Animatable(0f) }
  LaunchedEffect(dice) {
    progress.snapTo(0f)
    progress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
  }
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "ROLAGEM VISÍVEL • D6  •  cada face 6 = 1 sucesso",
      style = AppTheme.typography.label.copy(color = AppTheme.colors.accentCyan)
    )
    Spacer(Modifier.height(8.dp))
    if (dice.isEmpty()) {
      Text("Nenhum dado foi rolado.", style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary))
    } else {
      dice.chunked(5).forEachIndexed { rowIndex, faces ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
          faces.forEachIndexed { position, face ->
            val t = progress.value
            val displacement = abs(sin((t * 3.25 + position * .10) * PI)).toFloat() * (1f - t) * 22f
            val spin = (1f - t) * (360f + position * 81f + rowIndex * 53f)
            DiceCube(
              value = face,
              modifier = Modifier.size(57.dp).graphicsLayer {
                rotationZ = spin
                rotationX = (1f - t) * 55f
                rotationY = (1f - t) * 38f
                translationY = -displacement
                scaleX = .82f + .18f * t
                scaleY = .82f + .18f * t
              }
            )
          }
        }
        Spacer(Modifier.height(8.dp))
      }
      Text(
        "Resultado registrado pelo motor: ${dice.joinToString(" · ")}. O visual acompanha estes mesmos valores.",
        style = AppTheme.typography.bodySmall.copy(color = AppTheme.colors.textSecondary)
      )
    }
  }
}

private val dots: Map<Int, List<Pair<Float, Float>>> = mapOf(
  1 to listOf(.5f to .5f),
  2 to listOf(.26f to .26f, .74f to .74f),
  3 to listOf(.26f to .26f, .5f to .5f, .74f to .74f),
  4 to listOf(.26f to .26f, .74f to .26f, .26f to .74f, .74f to .74f),
  5 to listOf(.26f to .26f, .74f to .26f, .5f to .5f, .26f to .74f, .74f to .74f),
  6 to listOf(.26f to .23f, .74f to .23f, .26f to .50f, .74f to .50f, .26f to .77f, .74f to .77f)
)

@Composable
private fun DiceCube(value: Int, modifier: Modifier = Modifier) {
  val success = value == 6
  val bodyColor = if (success) Color(0xFF2F6D58) else Color(0xFFE5DED2)
  val edgeColor = if (success) Color(0xFF142F28) else Color(0xFF6C6963)
  val dotColor = if (success) Color(0xFFF5EDCC) else Color(0xFF262930)
  Canvas(modifier) {
    val side = size.minDimension
    val sideOffset = side * .11f
    val frontSize = side - sideOffset - 2f
    val sidePath = Path().apply {
      moveTo(frontSize, sideOffset)
      lineTo(side - 1f, 0f)
      lineTo(side - 1f, side - sideOffset)
      lineTo(frontSize, side - 1f)
      close()
    }
    drawPath(sidePath, edgeColor)
    drawRoundRect(bodyColor, topLeft = Offset(1f, sideOffset), size = Size(frontSize - 2f, frontSize - 2f), cornerRadius = CornerRadius(side * .12f))
    drawRoundRect(edgeColor, topLeft = Offset(1f, sideOffset), size = Size(frontSize - 2f, frontSize - 2f), cornerRadius = CornerRadius(side * .12f), style = Stroke(width = 2f))
    for ((x, y) in dots[value].orEmpty()) {
      drawCircle(dotColor, radius = side * .068f, center = Offset(1f + x * frontSize, sideOffset + y * frontSize))
    }
  }
}