package com.braseiro.talesvtt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import com.braseiro.talesvtt.ui.theme.AppTheme
import com.braseiro.talesvtt.ui.theme.Dimens

@Composable
fun PortraitSlot(
  name: String,
  modifier: Modifier = Modifier,
  size: Dp = 72.dp,
  archetype: String? = null,
  borderColor: Color = AppTheme.colors.borderMedium,
  imageRes: Int? = null,
  imageBase64: String? = null
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)
  Box(
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(AppTheme.colors.surfaceSecondary)
      .border(BorderStroke(Dimens.borderWidth, borderColor), shape)
      .testTag("portrait_slot_${name.lowercase().replace(" ", "_")}")
  ) {
    val importedBitmap = remember(imageBase64) {
      imageBase64?.takeIf { it.isNotBlank() }?.let { encoded ->
        runCatching {
          val bytes = Base64.decode(encoded, Base64.DEFAULT)
          BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
      }
    }
    if (importedBitmap != null || imageRes != null) {
      if (importedBitmap != null) {
        Image(
          bitmap = importedBitmap,
          contentDescription = name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else if (imageRes != null) {
        Image(
          painter = painterResource(id = imageRes),
          contentDescription = name,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .background(AppTheme.colors.backgroundPrimary.copy(alpha = 0.82f))
          .padding(horizontal = 4.dp, vertical = 3.dp)
      ) {
        Column {
          Text(
            text = name,
            style = AppTheme.typography.caption.copy(
              color = AppTheme.colors.textPrimary,
              fontSize = 9.5.sp
            )
          )
          if (archetype != null) {
            Text(
              text = archetype,
              style = AppTheme.typography.caption.copy(
                color = AppTheme.colors.textSecondary,
                fontSize = 7.5.sp
              )
            )
          }
        }
      }
    } else {
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = AppIcons.Kid,
          contentDescription = name,
          tint = AppTheme.colors.accentCyan,
          modifier = Modifier.size(size * 0.4f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = name,
          style = AppTheme.typography.caption.copy(
            color = AppTheme.colors.textPrimary,
            fontSize = 9.sp
          )
        )
        if (archetype != null) {
          Text(
            text = archetype,
            style = AppTheme.typography.caption.copy(
              color = AppTheme.colors.textMuted,
              fontSize = 7.5.sp
            )
          )
        }
      }
    }
  }
}

@Composable
fun AssetImageSlot(
  title: String,
  modifier: Modifier = Modifier,
  icon: ImageVector = AppIcons.Backpack,
  category: String? = null,
  size: Dp = 80.dp,
  accentColor: Color = AppTheme.colors.accentCyan,
  imageRes: Int? = null
) {
  val shape = RoundedCornerShape(Dimens.radiusSmall)
  Box(
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(AppTheme.colors.surfaceSecondary)
      .border(BorderStroke(Dimens.borderWidth, accentColor.copy(alpha = 0.3f)), shape)
      .testTag("asset_image_slot_${title.lowercase().replace(" ", "_")}"),
    contentAlignment = Alignment.Center
  ) {
    if (imageRes != null) {
      Image(
        painter = painterResource(id = imageRes),
        contentDescription = title,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .padding(5.dp)
      )
    } else {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(size * 0.4f)
        )
        Spacer(modifier = Modifier.height(Dimens.spacing4))
        Text(
          text = title,
          style = AppTheme.typography.caption.copy(
            color = AppTheme.colors.textPrimary,
            fontSize = 9.sp
          )
        )
        if (category != null) {
          Text(
            text = category,
            style = AppTheme.typography.caption.copy(
              color = AppTheme.colors.textSecondary,
              fontSize = 7.5.sp
            )
          )
        }
      }
    }
  }
}
