package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelMode
import com.example.model.DapPlayerState
import com.example.model.VisualizerChannelMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DapAudioTuningSheet(
  playerState: DapPlayerState,
  onTogglePhase: () -> Unit,
  onSetChannelMode: (ChannelMode) -> Unit,
  onSetVisualizerChannelMode: (VisualizerChannelMode) -> Unit,
  onSelectOutputDevice: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val colors = MaterialTheme.colorScheme

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.surface,
    contentColor = colors.onSurface,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "AUDIOPHILE TUNING DRAWER",
              color = colors.primary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp
            )
            Text(
              text = "Phase switch, channel routing & output device selector",
              color = colors.onSurfaceVariant,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.onSurface)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section 1: Active Output Device & Real Output Specs
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Headphones, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "AUDIO OUTPUT ROUTING & SPECS",
          color = colors.onSurface,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Detailed Output Spec Banner
      val spec = playerState.audioOutputSpec
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0F1218))
          .border(1.dp, if (spec.isHiRes) Color(0xFFE0A938) else colors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
          .padding(12.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = spec.deviceName.uppercase(),
              color = if (spec.isHiRes) Color(0xFFE0A938) else colors.onSurface,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (spec.isHiRes) Color(0xFFE0A938).copy(alpha = 0.2f) else colors.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = if (spec.isHiRes) "HI-RES DIRECT" else if (spec.connectionType.contains("Bluetooth")) "LOSSY BT" else "STANDARD",
                color = if (spec.isHiRes) Color(0xFFE0A938) else colors.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "• Technology: ${spec.technology}",
            color = colors.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "• DAC Stream: ${spec.sampleRateHz / 1000f} kHz | ${spec.bitDepth}-bit PCM | ~${spec.estimatedBitrateKbps} kbps",
            color = colors.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = if (spec.connectionType.contains("Bluetooth")) {
              "Note: Bluetooth A2DP transmits lossy compressed audio (~328 kbps). For true lossless master fidelity, connect 3.5mm headphones or a USB DAC."
            } else {
              "Fidelity: Uncompressed direct analogue path with zero resampling degradation."
            },
            color = if (spec.connectionType.contains("Bluetooth")) Color(0xFFFFA726) else colors.tertiary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Output Devices List
      Text(
        text = "AVAILABLE OUTPUT DEVICES:",
        color = colors.onSurfaceVariant,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        playerState.availableOutputs.forEach { device ->
          val isSelected = device.id == playerState.selectedOutputDeviceId || (playerState.selectedOutputDeviceId == null && device.isDefault)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.4f))
              .border(
                width = 1.dp,
                color = if (isSelected) colors.primary else colors.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(6.dp)
              )
              .clickable { onSelectOutputDevice(device.id) }
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = device.name,
                color = if (isSelected) colors.primary else colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = device.specsSummary,
                color = colors.onSurfaceVariant,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            if (isSelected) {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(colors.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Active Output",
                  tint = colors.onPrimary,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section 2: Audio Phase Inversion (0° / 180°)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "AUDIO PHASE SWITCH",
            color = colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = if (playerState.audioPhaseInverted) "Phase: 180° Inverted (Reverse Polarity)" else "Phase: 0° Normal (Absolute Polarity)",
            color = if (playerState.audioPhaseInverted) colors.primary else colors.onSurfaceVariant,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Switch(
          checked = playerState.audioPhaseInverted,
          onCheckedChange = { onTogglePhase() },
          colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary
          )
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section 3: Audio Channel Isolation / Mute Left or Right
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Hearing, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "AUDIO CHANNEL ISOLATION (SWITCH OFF L / R)",
          color = colors.onSurface,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ChannelMode.values().forEach { mode ->
          val isSel = mode == playerState.channelMode
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSel) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.4f))
              .border(
                width = 1.dp,
                color = if (isSel) colors.primary else colors.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(6.dp)
              )
              .clickable { onSetChannelMode(mode) }
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = mode.displayName,
                color = if (isSel) colors.primary else colors.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = mode.description,
                color = colors.onSurfaceVariant,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
              )
            }
            if (isSel) {
              Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = colors.primary, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = colors.outline.copy(alpha = 0.3f))
      Spacer(modifier = Modifier.height(14.dp))

      // Section 4: Visualizer Channel Display (Turn off L or R in Visualizers)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "VISUALIZER CHANNEL CHANNELS",
          color = colors.onSurface,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        VisualizerChannelMode.values().forEach { vMode ->
          val isSel = vMode == playerState.visualizerChannelMode
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSel) colors.primary else colors.surfaceVariant.copy(alpha = 0.6f))
              .clickable { onSetVisualizerChannelMode(vMode) }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = when (vMode) {
                VisualizerChannelMode.STEREO -> "STEREO L+R"
                VisualizerChannelMode.LEFT_ONLY -> "LEFT ONLY"
                VisualizerChannelMode.RIGHT_ONLY -> "RIGHT ONLY"
              },
              color = if (isSel) colors.onPrimary else colors.onSurface,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}
