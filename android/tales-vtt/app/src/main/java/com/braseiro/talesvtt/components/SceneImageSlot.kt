package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun SceneImageSlot(
  modifier: Modifier = Modifier,
  locationName: String = "BJÖRKSTIGEN",
  sceneTone: String = "Suspense",
  onTtsClick: () -> Unit = {},
  showTts: Boolean = true,
  onIllustrationClick: () -> Unit = {},
  onCharactersClick: () -> Unit = {},
  onLocationClick: () -> Unit = {},
  imageRes: Int? = null,
  imageUri: String? = null
) {
  val shape = RoundedCornerShape(Dimens.radiusMedium)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(240.dp)
      .clip(shape)
      .background(AppTheme.colors.backgroundSecondary)
      .border(BorderStroke(Dimens.borderWidth, AppTheme.colors.borderSubtle), shape)
      .testTag("scene_image_slot")
  ) {
    // Prefer an imported scene image tied to this exact scene, then packaged production raster assets.
    val context = LocalContext.current
    val importedBitmap = produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, key1 = imageUri) {
      value = if (imageUri.isNullOrBlank()) null else runCatching {
        context.contentResolver.openInputStream(Uri.parse(imageUri)).use { input ->
          requireNotNull(input)
          requireNotNull(BitmapFactory.decodeStream(input)).asImageBitmap()
        }
      }.getOrNull()
    }.value
    when {
      importedBitmap != null -> Image(
        bitmap = importedBitmap,
        contentDescription = "Ilustração importada da cena",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      imageRes != null -> Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Ilustração da cena",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      else -> AtmosphericLoopCanvas(modifier = Modifier.fillMaxSize())
    }
    if (importedBitmap != null || imageRes != null) {
      Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.08f)))
    }

    // Overlay context elements
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(Dimens.spacing8),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top row: Left context tag & Right TTS button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing4)) {
          InfoChip(
            label = "ILUSTRAÇÃO",
            icon = AppIcons.Illustration,
            tintColor = AppTheme.colors.textPrimary,
            backgroundColor = AppTheme.colors.backgroundPrimary.copy(alpha = 0.85f),
            borderColor = AppTheme.colors.borderHighlight,
            onClick = onIllustrationClick
          )
          InfoChip(
            label = "PERSONAGENS",
            icon = AppIcons.Group,
            tintColor = AppTheme.colors.textSecondary,
            backgroundColor = AppTheme.colors.backgroundPrimary.copy(alpha = 0.85f),
            borderColor = AppTheme.colors.borderSubtle,
            onClick = onCharactersClick
          )
        }

        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(Dimens.spacing6)
        ) {
          if (showTts) {
            InfoChip(
              label = "OUVIR (TTS)",
              icon = AppIcons.AudioTts,
              tintColor = AppTheme.colors.accentCyan,
              backgroundColor = AppTheme.colors.backgroundPrimary.copy(alpha = 0.9f),
              borderColor = AppTheme.colors.accentCyan,
              onClick = onTtsClick
            )
          }

          // Street location sign overlay
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0xFF14242A).copy(alpha = 0.9f))
              .border(BorderStroke(1.dp, Color(0xFF2C4A54)), RoundedCornerShape(3.dp))
              .padding(horizontal = Dimens.spacing6, vertical = 2.dp)
          ) {
            Text(
              text = locationName,
              style = AppTheme.typography.caption.copy(
                color = AppTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp,
                letterSpacing = 1.0.sp
              )
            )
          }
        }
      }

      // Bottom Row: Location & Scene Tone + Center bottom handle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacing4)) {
          InfoChip(
            label = "LOCAL ATUAL",
            icon = AppIcons.Location,
            tintColor = AppTheme.colors.textPrimary,
            backgroundColor = AppTheme.colors.backgroundPrimary.copy(alpha = 0.85f),
            borderColor = AppTheme.colors.borderSubtle,
            onClick = onLocationClick
          )
          InfoChip(
            label = "TOM DA CENA",
            icon = AppIcons.AudioWaves,
            value = sceneTone,
            tintColor = AppTheme.colors.accentOlive,
            backgroundColor = AppTheme.colors.backgroundPrimary.copy(alpha = 0.9f),
            borderColor = AppTheme.colors.accentOlive.copy(alpha = 0.4f)
          )
        }

        // Center bottom indicator bar matching the master visual
        Box(
          modifier = Modifier
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(Dimens.radiusPill))
            .background(AppTheme.colors.accentCyan.copy(alpha = 0.8f))
        )

        // Spacer to balance
        Spacer(modifier = Modifier.width(60.dp))
      }
    }
  }
}

@Composable
fun AtmosphericLoopCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // 1. Twilight sky gradient: deep dark teal to dark amber horizon glow
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(
          Color(0xFF07141A),
          Color(0xFF0C2128),
          Color(0xFF142E36),
          Color(0xFF223533),
          Color(0xFF2B332A)
        ),
        startY = 0f,
        endY = h * 0.75f
      )
    )

    // 2. Distant massive Loop Particle Accelerator Ring (monumental arch)
    val ringCenter = Offset(w * 0.52f, h * 0.46f)
    val ringRadius = w * 0.19f
    drawCircle(
      color = Color(0xFF16272E),
      radius = ringRadius,
      center = ringCenter,
      style = Stroke(width = 16.dp.toPx())
    )
    drawCircle(
      color = Color(0x3343C6CF),
      radius = ringRadius - 6.dp.toPx(),
      center = ringCenter,
      style = Stroke(width = 2.dp.toPx())
    )

    // 3. Massive industrial cooling towers & beacon lights
    // Left towers
    drawTower(w * 0.40f, h * 0.28f, w * 0.08f, h * 0.36f)
    drawTower(w * 0.46f, h * 0.32f, w * 0.06f, h * 0.32f)
    // Right towers
    drawTower(w * 0.58f, h * 0.30f, w * 0.07f, h * 0.34f)
    drawTower(w * 0.64f, h * 0.34f, w * 0.06f, h * 0.30f)

    // Red/amber warning blinking lights on towers
    drawCircle(Color(0xFFE5A14A), radius = 2.dp.toPx(), center = Offset(w * 0.44f, h * 0.28f))
    drawCircle(Color(0xFFC8645A), radius = 2.dp.toPx(), center = Offset(w * 0.61f, h * 0.30f))

    // 4. Scandinavian Pine Tree forest horizon silhouette
    val forestPath = Path().apply {
      moveTo(0f, h * 0.62f)
      var currX = 0f
      while (currX < w) {
        val peakH = (Math.sin(currX.toDouble() * 0.05).toFloat() * 12.dp.toPx()) + h * 0.58f
        lineTo(currX + 8.dp.toPx(), peakH)
        currX += 16.dp.toPx()
      }
      lineTo(w, h * 0.65f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(forestPath, Color(0xFF0A161A))

    // 5. Wet Scandinavian suburban street / asphalt with reflective water puddles
    val roadPath = Path().apply {
      moveTo(w * 0.25f, h * 0.68f)
      lineTo(w * 0.75f, h * 0.68f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(
      roadPath,
      Brush.verticalGradient(
        colors = listOf(Color(0xFF0F1E22), Color(0xFF081215)),
        startY = h * 0.68f,
        endY = h
      )
    )

    // Warm puddle reflections
    drawOval(
      brush = Brush.radialGradient(
        colors = listOf(Color(0x30E5A14A), Color(0x00E5A14A)),
        center = Offset(w * 0.5f, h * 0.82f),
        radius = 45.dp.toPx()
      ),
      topLeft = Offset(w * 0.4f, h * 0.78f),
      size = Size(w * 0.22f, 14.dp.toPx())
    )

    // 6. Silhouette of 4 kids on bicycles looking towards the Loop
    drawKidBikeSilhouette(w * 0.26f, h * 0.84f, 0.95f)
    drawKidBikeSilhouette(w * 0.40f, h * 0.82f, 1.0f)
    drawKidBikeSilhouette(w * 0.56f, h * 0.82f, 1.0f)
    drawKidBikeSilhouette(w * 0.70f, h * 0.85f, 0.95f)
  }
}

private fun DrawScope.drawTower(x: Float, y: Float, width: Float, height: Float) {
  val path = Path().apply {
    moveTo(x, y + height)
    lineTo(x + width * 0.2f, y)
    lineTo(x + width * 0.8f, y)
    lineTo(x + width, y + height)
    close()
  }
  drawPath(path, Color(0xFF132227))
}

private fun DrawScope.drawKidBikeSilhouette(cx: Float, cy: Float, scale: Float) {
  val bikeColor = Color(0xFF040A0C)
  val jacketColor = Color(0xFF1B313A)
  val backpackColor = Color(0xFF6B482A)

  // Kid Head & Torso with retro jacket
  drawCircle(bikeColor, radius = 5.dp.toPx() * scale, center = Offset(cx, cy - 22.dp.toPx() * scale))
  drawRect(
    jacketColor,
    topLeft = Offset(cx - 6.dp.toPx() * scale, cy - 17.dp.toPx() * scale),
    size = Size(12.dp.toPx() * scale, 14.dp.toPx() * scale)
  )
  // Backpack
  drawRect(
    backpackColor,
    topLeft = Offset(cx - 5.dp.toPx() * scale, cy - 15.dp.toPx() * scale),
    size = Size(10.dp.toPx() * scale, 10.dp.toPx() * scale)
  )
  // Bicycle wheels
  drawCircle(bikeColor, radius = 6.dp.toPx() * scale, center = Offset(cx - 7.dp.toPx() * scale, cy + 2.dp.toPx() * scale), style = Stroke(1.5.dp.toPx()))
  drawCircle(bikeColor, radius = 6.dp.toPx() * scale, center = Offset(cx + 7.dp.toPx() * scale, cy + 2.dp.toPx() * scale), style = Stroke(1.5.dp.toPx()))
  // Bicycle frame
  drawLine(bikeColor, Offset(cx - 7.dp.toPx() * scale, cy + 2.dp.toPx() * scale), Offset(cx, cy - 5.dp.toPx() * scale), strokeWidth = 1.5.dp.toPx())
  drawLine(bikeColor, Offset(cx + 7.dp.toPx() * scale, cy + 2.dp.toPx() * scale), Offset(cx, cy - 5.dp.toPx() * scale), strokeWidth = 1.5.dp.toPx())
}
