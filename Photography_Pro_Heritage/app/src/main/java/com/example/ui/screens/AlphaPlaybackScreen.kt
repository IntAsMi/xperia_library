package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.PhotoEntity
import com.example.model.CreativeLook
import com.example.ui.components.AlphaHistogram
import com.example.ui.theme.SonyBorder
import com.example.ui.theme.SonyOrange
import com.example.ui.theme.SonyPanelDark
import com.example.ui.theme.SonyPanelLight
import com.example.ui.theme.SonyTextMuted
import com.example.ui.theme.SonyTextPrimary
import com.example.ui.theme.SonyTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlphaPlaybackScreen(
  photos: List<PhotoEntity>,
  selectedPhoto: PhotoEntity?,
  onSelectPhoto: (PhotoEntity) -> Unit,
  onDeletePhoto: (Long) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onClose)
  val context = LocalContext.current
  var showExifInfo by remember { mutableStateOf(true) }

  val activePhoto = selectedPhoto ?: photos.firstOrNull()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    if (activePhoto == null) {
      // Empty state
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "No Photos Captured",
          color = SonyTextSecondary,
          fontSize = 16.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    } else {
      // 1. Photo Main Display
      val imageModel = if (activePhoto.isSample) {
        activePhoto.sampleDrawableRes
      } else {
        File(activePhoto.filePath)
      }

      AsyncImage(
        model = ImageRequest.Builder(context)
          .data(imageModel)
          .crossfade(true)
          .build(),
        contentDescription = "Photo Preview",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .clickable { showExifInfo = !showExifInfo }
      )

      // 2. EXIF Info Overlay Panel (Sony Alpha Style)
      if (showExifInfo) {
        Column(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = 90.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SonyPanelDark.copy(alpha = 0.85f))
            .border(1.dp, SonyBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(SonyOrange)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "α SONY",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${activePhoto.lensFocalLength} ${activePhoto.aperture}",
              color = SonyTextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Exposure readout row: 1/250s | F1.9 | ISO 100 | 0.0 EV
          Text(
            text = "${activePhoto.shutterSpeed}s   ${activePhoto.aperture}   ISO ${activePhoto.iso}   ${activePhoto.ev} EV",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )

          Spacer(modifier = Modifier.height(4.dp))

          val dateStr = SimpleDateFormat("yyyy.MM.dd  HH:mm:ss", Locale.US).format(Date(activePhoto.timestamp))
          Text(
            text = "$dateStr  |  LOOK: ${activePhoto.creativeLook}  |  ${activePhoto.format}",
            color = SonyTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Mini Histogram
          val creative = runCatching { CreativeLook.valueOf(activePhoto.creativeLook) }.getOrDefault(CreativeLook.ST)
          AlphaHistogram(
            exposureComp = activePhoto.ev,
            creativeLook = creative,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    }

    // 3. Top Action Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .background(SonyPanelDark.copy(alpha = 0.85f))
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Viewfinder",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "PLAYBACK",
          color = SonyOrange,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.5.sp
        )
      }

      if (activePhoto != null) {
        Row {
          // Toggle info
          IconButton(onClick = { showExifInfo = !showExifInfo }) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "Toggle EXIF Info",
              tint = if (showExifInfo) SonyOrange else Color.White
            )
          }

          // Share
          IconButton(
            onClick = {
              try {
                val sendIntent = Intent().apply {
                  action = Intent.ACTION_SEND
                  putExtra(Intent.EXTRA_TEXT, "Shot with Sony Xperia Photography Pro: ${activePhoto.lensFocalLength} ${activePhoto.shutterSpeed}s ISO ${activePhoto.iso}")
                  type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Photo"))
              } catch (_: Exception) {}
            }
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share Photo",
              tint = Color.White
            )
          }

          // Delete
          IconButton(onClick = { onDeletePhoto(activePhoto.id) }) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete Photo",
              tint = SonyTextSecondary
            )
          }
        }
      }
    }

    // 4. Bottom Horizontal Thumbnail Strip
    if (photos.isNotEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(SonyPanelDark.copy(alpha = 0.92f))
          .border(1.dp, SonyBorder)
          .padding(vertical = 8.dp)
      ) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(photos) { photo ->
            val isSelected = photo.id == activePhoto?.id
            val thumbModel = if (photo.isSample) photo.sampleDrawableRes else File(photo.filePath)

            Box(
              modifier = Modifier
                .size(60.dp, 60.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (isSelected) SonyOrange else Color.White.copy(alpha = 0.2f),
                  shape = RoundedCornerShape(4.dp)
                )
                .clickable { onSelectPhoto(photo) }
            ) {
              AsyncImage(
                model = ImageRequest.Builder(context)
                  .data(thumbModel)
                  .crossfade(true)
                  .build(),
                contentDescription = "Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
              )
            }
          }
        }
      }
    }
  }
}
