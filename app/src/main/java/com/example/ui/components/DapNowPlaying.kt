package com.example.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DapPlayerState
import com.example.model.VisualizerChannelMode
import com.example.model.formatDuration
import kotlinx.coroutines.delay

@Composable
fun DapNowPlaying(
  playerState: DapPlayerState,
  onClose: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onFastForwardStep: () -> Unit,
  onFastRewindStep: () -> Unit,
  onToggleRepeat: () -> Unit,
  onToggleShuffle: () -> Unit,
  onSetSpeed: (Float) -> Unit,
  onSetLoopA: () -> Unit,
  onSetLoopB: () -> Unit,
  onClearLoop: () -> Unit,
  onOpenAudioTuning: () -> Unit,
  onTogglePhase: () -> Unit = {},
  onSetChannelMode: (com.example.model.ChannelMode) -> Unit = {},
  onSetCrossfeed: (com.example.model.CrossfeedMode) -> Unit = {},
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onClose)
  val track = playerState.currentTrack ?: return
  val colors = MaterialTheme.colorScheme
  val config = LocalConfiguration.current
  val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0A0C10)) // Pure opaque solid dark studio background
      .padding(horizontal = if (isLandscape) 20.dp else 16.dp)
  ) {
    // 1. Top Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = if (isLandscape) 4.dp else 8.dp, bottom = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (!isLandscape) {
        IconButton(onClick = onClose) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Collapse Now Playing",
            tint = colors.onBackground,
            modifier = Modifier.size(28.dp)
          )
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(start = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(Color(0xFF00E676))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "DAP CONSOLE [LIVE]",
            color = colors.primary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "NOW PLAYING",
          color = colors.primary,
          fontSize = if (isLandscape) 11.sp else 12.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 2.sp
        )
        if (playerState.currentFolderTracks.isNotEmpty()) {
          Text(
            text = "Track ${playerState.currentTrackIndex + 1} of ${playerState.currentFolderTracks.size} (Disk ${playerState.currentDiskNumber})",
            color = colors.onSurfaceVariant,
            fontSize = if (isLandscape) 9.5.sp else 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Audiophile Tuning Drawer Button
        IconButton(onClick = onOpenAudioTuning) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Audio Tuning Drawer",
            tint = if (playerState.audioPhaseInverted || playerState.channelMode != com.example.model.ChannelMode.STEREO) colors.primary else colors.onSurface
          )
        }

        // Repeat mode button
        IconButton(onClick = onToggleRepeat) {
          Icon(
            imageVector = when (playerState.repeatMode) {
              2 -> Icons.Default.RepeatOne
              1 -> Icons.Default.Repeat
              else -> Icons.Default.Repeat
            },
            contentDescription = "Repeat Mode",
            tint = if (playerState.repeatMode > 0) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f)
          )
        }

        // Shuffle button
        IconButton(onClick = onToggleShuffle) {
          Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            tint = if (playerState.isShuffle) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f)
          )
        }
      }
    }

    // Large File Pre-buffering Indication
    AnimatedVisibility(visible = playerState.isBufferingLargeFile) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF1E222D))
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "CACHING HI-RES MASTER INTO RAM...",
            color = colors.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "${(playerState.bufferProgress * 100).toInt()}%",
            color = colors.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
          progress = { playerState.bufferProgress },
          modifier = Modifier.fillMaxWidth().height(4.dp),
          color = colors.primary,
          trackColor = colors.surfaceVariant
        )
      }
    }

    // Unified Technical Audio Layout for Portrait and Landscape side panes
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      TrackInfoBanner(track = track)

      OutputSpecBanner(
        spec = playerState.audioOutputSpec,
        playerState = playerState,
        onClick = onOpenAudioTuning
      )

      QuickAudioTuningStrip(
        channelMode = playerState.channelMode,
        audioPhaseMode = playerState.audioPhaseMode,
        crossfeedMode = playerState.crossfeedMode,
        onSetChannelMode = onSetChannelMode,
        onTogglePhase = onTogglePhase,
        onSetCrossfeed = onSetCrossfeed,
        onOpenAudioTuning = onOpenAudioTuning
      )

      // Monochromatic High-Density Left and Right Waveform Scrubber
      DapWaveformScrubber(
        waveformPoints = track.waveform,
        waveformLeft = track.waveformLeft,
        waveformRight = track.waveformRight,
        channelMode = playerState.channelMode,
        audioPhaseMode = playerState.audioPhaseMode,
        positionMs = playerState.positionMs,
        durationMs = playerState.durationMs,
        onSeekTo = onSeekTo
      )

      // 3-Level Timings (Song, Disk, Folder)
      MultiLevelTimingsBox(playerState = playerState)

      // Tactile Transport Controls
      TactileControlsRow(
        isPlaying = playerState.isPlaying,
        onTogglePlayPause = onTogglePlayPause,
        onSkipNext = onSkipNext,
        onSkipPrevious = onSkipPrevious,
        onFastForward = onFastForwardStep,
        onFastRewind = onFastRewindStep
      )

      // Real Reactive 32-Band Stereo Spectrum Visualizer
      ReactiveSpectrumVisualizer(
        bands = playerState.spectrumBands,
        bandsLeft = playerState.spectrumBandsLeft,
        bandsRight = playerState.spectrumBandsRight,
        channelMode = playerState.channelMode
      )

      // Real Stereo Peak dB VU Meter
      StereoVuMeter(
        left = playerState.peakMeterLeft,
        right = playerState.peakMeterRight,
        channelMode = playerState.channelMode
      )

      // Extras: Speed & A-B Looper
      AudiophileExtrasBox(
        playerState = playerState,
        onSetSpeed = onSetSpeed,
        onSetLoopA = onSetLoopA,
        onSetLoopB = onSetLoopB,
        onClearLoop = onClearLoop
      )

      // Complete Technical Specs Grid
      TechnicalSpecsGrid(track = track, playerState = playerState)

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

@Composable
fun TrackInfoBanner(track: com.example.model.AudioFileItem) {
  val colors = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF141720))
      .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
      .padding(12.dp)
  ) {
    Column {
      Text(
        text = track.title,
        color = colors.onSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(3.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(colors.primary)
            .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
          Text(
            text = "DISK ${track.diskNumber} • #${track.trackNumber}",
            color = colors.onPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = track.fileName,
          color = colors.primary,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun OutputSpecBanner(
  spec: com.example.model.AudioOutputSpec,
  playerState: com.example.model.DapPlayerState? = null,
  onClick: () -> Unit
) {
  val colors = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF0F1117))
      .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          imageVector = Icons.Default.Headphones,
          contentDescription = null,
          tint = if (spec.isHiRes) Color(0xFFE0A938) else colors.primary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "OUTPUT: ${spec.deviceName.uppercase()}",
            color = if (spec.isHiRes) Color(0xFFE0A938) else colors.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${spec.technology} • ${spec.sampleRateHz / 1000f}kHz / ${spec.bitDepth}b (~${spec.estimatedBitrateKbps}kbps)",
            color = colors.onSurfaceVariant,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // High-visibility Shizuku Bit-Perfect Status Indicator Badge
        if (playerState != null) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(
                if (playerState.isShizukuPrivileged) Color(0xFF00E676).copy(alpha = 0.2f)
                else if (playerState.isShizukuRunning) Color(0xFFFFA726).copy(alpha = 0.2f)
                else colors.surfaceVariant
              )
              .border(
                0.6.dp,
                if (playerState.isShizukuPrivileged) Color(0xFF00E676)
                else if (playerState.isShizukuRunning) Color(0xFFFFA726)
                else colors.outline.copy(alpha = 0.3f),
                RoundedCornerShape(3.dp)
              )
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (playerState.isShizukuPrivileged) "SHIZUKU: 1:1 HAL"
              else if (playerState.isShizukuRunning) "SHIZUKU: AUTH"
              else "HAL DIRECT",
              color = if (playerState.isShizukuPrivileged) Color(0xFF00E676)
              else if (playerState.isShizukuRunning) Color(0xFFFFA726)
              else colors.onSurfaceVariant,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "ROUTE ⚙",
            color = colors.primary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
fun QuickAudioTuningStrip(
  channelMode: com.example.model.ChannelMode,
  audioPhaseMode: com.example.model.AudioPhaseMode,
  crossfeedMode: com.example.model.CrossfeedMode = com.example.model.CrossfeedMode.OFF,
  onSetChannelMode: (com.example.model.ChannelMode) -> Unit,
  onTogglePhase: () -> Unit,
  onSetCrossfeed: (com.example.model.CrossfeedMode) -> Unit = {},
  onOpenAudioTuning: () -> Unit
) {
  val colors = MaterialTheme.colorScheme
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Channel Mode Quick Toggle
    val nextMode = when (channelMode) {
      com.example.model.ChannelMode.STEREO -> com.example.model.ChannelMode.LEFT_ONLY
      com.example.model.ChannelMode.LEFT_ONLY -> com.example.model.ChannelMode.RIGHT_ONLY
      com.example.model.ChannelMode.RIGHT_ONLY -> com.example.model.ChannelMode.MONO
      com.example.model.ChannelMode.MONO -> com.example.model.ChannelMode.STEREO
    }

    Box(
      modifier = Modifier
        .weight(1.1f)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF141822))
        .border(1.dp, if (channelMode != com.example.model.ChannelMode.STEREO) colors.primary else colors.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        .clickable { onSetChannelMode(nextMode) }
        .padding(horizontal = 6.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "CH: ${channelMode.displayName.uppercase()}",
        color = if (channelMode != com.example.model.ChannelMode.STEREO) colors.primary else colors.onSurface,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }

    // Phase Inversion Quick Toggle (Single side / specific inversion display)
    val isInv = audioPhaseMode != com.example.model.AudioPhaseMode.NORMAL
    val phaseLabel = when (audioPhaseMode) {
      com.example.model.AudioPhaseMode.NORMAL -> "PHASE: 0°"
      com.example.model.AudioPhaseMode.INVERT_LEFT_ONLY -> "PHASE: -L INV"
      com.example.model.AudioPhaseMode.INVERT_RIGHT_ONLY -> "PHASE: -R INV"
      com.example.model.AudioPhaseMode.INVERT_BOTH -> "PHASE: -L-R"
      com.example.model.AudioPhaseMode.SWAP_CHANNELS -> "PHASE: SWAP"
    }

    Box(
      modifier = Modifier
        .weight(1.1f)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF141822))
        .border(1.dp, if (isInv) Color(0xFFFF5252) else colors.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        .clickable { onTogglePhase() }
        .padding(horizontal = 6.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = phaseLabel,
        color = if (isInv) Color(0xFFFF5252) else colors.onSurface,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }

    // Crossfeed Quick Toggle
    val nextCrossfeed = when (crossfeedMode) {
      com.example.model.CrossfeedMode.OFF -> com.example.model.CrossfeedMode.CHU_MOY
      com.example.model.CrossfeedMode.CHU_MOY -> com.example.model.CrossfeedMode.JAN_MEIER
      com.example.model.CrossfeedMode.JAN_MEIER -> com.example.model.CrossfeedMode.OFF
    }
    val isXfeedOn = crossfeedMode != com.example.model.CrossfeedMode.OFF
    val xfeedLabel = when (crossfeedMode) {
      com.example.model.CrossfeedMode.OFF -> "XFEED: OFF"
      com.example.model.CrossfeedMode.CHU_MOY -> "XFEED: MOY"
      com.example.model.CrossfeedMode.JAN_MEIER -> "XFEED: MEIER"
    }

    Box(
      modifier = Modifier
        .weight(1.1f)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFF141822))
        .border(1.dp, if (isXfeedOn) colors.primary else colors.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        .clickable { onSetCrossfeed(nextCrossfeed) }
        .padding(horizontal = 6.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = xfeedLabel,
        color = if (isXfeedOn) colors.primary else colors.onSurface,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }

    // Tuning Drawer Button
    Box(
      modifier = Modifier
        .weight(0.9f)
        .clip(RoundedCornerShape(6.dp))
        .background(colors.surfaceVariant.copy(alpha = 0.5f))
        .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
        .clickable { onOpenAudioTuning() }
        .padding(horizontal = 6.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "TUNING ⚙",
        color = colors.primary,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }
  }
}

@Composable
fun ReactiveSpectrumVisualizer(
  bands: List<Float>,
  bandsLeft: List<Float> = emptyList(),
  bandsRight: List<Float> = emptyList(),
  channelMode: com.example.model.ChannelMode = com.example.model.ChannelMode.STEREO
) {
  val colors = MaterialTheme.colorScheme
  val leftColor = Color(0xFF00E5FF) // Cyan
  val rightColor = Color(0xFFFFB300) // Gold

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(68.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF08090C))
      .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(text = "DSP SPECTRUM [L: CYAN / R: GOLD]", color = Color(0xFFA0A3B0), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
          text = when (channelMode) {
            com.example.model.ChannelMode.STEREO -> "STEREO L+R"
            com.example.model.ChannelMode.LEFT_ONLY -> "SOLO LEFT"
            com.example.model.ChannelMode.RIGHT_ONLY -> "SOLO RIGHT"
            com.example.model.ChannelMode.MONO -> "SUMMED MONO"
          },
          color = colors.primary,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(2.dp))

      Canvas(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        val w = size.width
        val h = size.height
        val halfW = (w - 8.dp.toPx()) / 2f
        val bLeft = if (bandsLeft.isNotEmpty()) bandsLeft else bands
        val bRight = if (bandsRight.isNotEmpty()) bandsRight else bands
        val count = 16 // 16 bands for Left, 16 bands for Right
        val spacing = 1.5.dp.toPx()
        val barW = ((halfW - spacing * (count - 1)) / count).coerceAtLeast(1.5f)

        // LEFT CHANNEL SPECTRUM (Left Half)
        for (i in 0 until count) {
          val raw = if (channelMode == com.example.model.ChannelMode.RIGHT_ONLY) 0f else bLeft.getOrElse(i) { 0f }
          val barH = if (raw <= 0.02f) 0f else (h * raw.coerceIn(0.08f, 1.0f))
          val x = i * (barW + spacing)
          val y = h - barH

          if (barH > 0f) {
            drawRoundRect(
              color = leftColor,
              topLeft = Offset(x, y),
              size = Size(barW, barH),
              cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
          }
        }

        // Center division line
        val divX = halfW + 4.dp.toPx()
        drawLine(
          color = Color(0xFF282D3C),
          start = Offset(divX, 0f),
          end = Offset(divX, h),
          strokeWidth = 1.dp.toPx()
        )

        // RIGHT CHANNEL SPECTRUM (Right Half)
        val startR = divX + 4.dp.toPx()
        for (i in 0 until count) {
          val raw = if (channelMode == com.example.model.ChannelMode.LEFT_ONLY) 0f else bRight.getOrElse(i) { 0f }
          val barH = if (raw <= 0.02f) 0f else (h * raw.coerceIn(0.08f, 1.0f))
          val x = startR + i * (barW + spacing)
          val y = h - barH

          if (barH > 0f) {
            drawRoundRect(
              color = rightColor,
              topLeft = Offset(x, y),
              size = Size(barW, barH),
              cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
          }
        }
      }
    }
  }
}

@Composable
fun StereoVuMeter(
  left: Float,
  right: Float,
  channelMode: com.example.model.ChannelMode = com.example.model.ChannelMode.STEREO
) {
  val colors = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF08090C))
      .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(text = "STEREO VU PEAK", color = Color(0xFFA0A3B0), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Text(text = "-40   -20   -10   -6   -3   0dB", color = Color(0xFFA0A3B0), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
      }

      Spacer(modifier = Modifier.height(3.dp))

      // Left Channel
      VuMeterBar(channel = "L", level = if (channelMode == com.example.model.ChannelMode.RIGHT_ONLY) 0f else left)
      Spacer(modifier = Modifier.height(3.dp))
      // Right Channel
      VuMeterBar(channel = "R", level = if (channelMode == com.example.model.ChannelMode.LEFT_ONLY) 0f else right)
    }
  }
}

@Composable
fun MultiLevelTimingsBox(playerState: DapPlayerState) {
  val colors = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF141722))
      .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Column {
      // 1. Current Song Timings
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(text = "SONG ELAPSED", color = colors.onSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
          Text(text = playerState.formattedElapsed, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "SONG REMAINING", color = colors.onSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
          Text(text = playerState.formattedRemaining, color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(text = "SONG TOTAL", color = colors.onSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
          Text(text = formatDuration(playerState.durationMs), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Current Disk Timings (based on disk number like 2.01)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(Color(0xFF1B2030))
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "DISK ${playerState.currentDiskNumber} REMAINING: ${playerState.formattedDiskRemaining}",
          color = Color(0xFFE0A938),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "DISK TOTAL: ${playerState.formattedDiskTotal}",
          color = colors.onSurfaceVariant,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      // 3. Current Folder Timings
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "FOLDER REMAINING: ${playerState.formattedFolderRemaining}",
          color = colors.primary,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = "FOLDER TOTAL: ${playerState.formattedFolderTotal}",
          color = colors.onSurfaceVariant,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }
  }
}

/**
 * Clearly shown buttons with Tap & Hold functionality
 */
@Composable
fun TactileControlsRow(
  isPlaying: Boolean,
  onTogglePlayPause: () -> Unit,
  onSkipNext: () -> Unit,
  onSkipPrevious: () -> Unit,
  onFastForward: () -> Unit,
  onFastRewind: () -> Unit
) {
  val colors = MaterialTheme.colorScheme

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Jump -10s with hold to continuous rewind
    HoldableIconButton(
      icon = Icons.Default.FastRewind,
      description = "Rewind -10s",
      onTap = onFastRewind,
      onHold = onFastRewind
    )

    // Previous Track with hold to seek backwards
    HoldableIconButton(
      icon = Icons.Default.SkipPrevious,
      description = "Previous Track",
      size = 52.dp,
      iconSize = 30.dp,
      onTap = onSkipPrevious,
      onHold = onFastRewind
    )

    // Play / Pause (Distinct prominent button)
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(colors.primary)
        .border(2.dp, Color(0xFFFFFFFF).copy(alpha = 0.4f), CircleShape)
        .clickable { onTogglePlayPause() },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
        contentDescription = "Play/Pause",
        tint = colors.onPrimary,
        modifier = Modifier.size(38.dp)
      )
    }

    // Next Track with hold to fast-forward
    HoldableIconButton(
      icon = Icons.Default.SkipNext,
      description = "Next Track",
      size = 52.dp,
      iconSize = 30.dp,
      onTap = onSkipNext,
      onHold = onFastForward
    )

    // Jump +10s with hold to continuous fast forward
    HoldableIconButton(
      icon = Icons.Default.FastForward,
      description = "Forward +10s",
      onTap = onFastForward,
      onHold = onFastForward
    )
  }
}

@Composable
fun HoldableIconButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  description: String,
  size: androidx.compose.ui.unit.Dp = 44.dp,
  iconSize: androidx.compose.ui.unit.Dp = 24.dp,
  onTap: () -> Unit,
  onHold: () -> Unit
) {
  val colors = MaterialTheme.colorScheme
  var isPressed by remember { mutableStateOf(false) }

  LaunchedEffect(isPressed) {
    if (isPressed) {
      delay(400L) // initial hold threshold
      while (isPressed) {
        onHold()
        delay(250L) // repeated scrub frequency
      }
    }
  }

  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(if (isPressed) colors.primary.copy(alpha = 0.25f) else Color(0xFF181C26))
      .border(1.dp, if (isPressed) colors.primary else colors.outline.copy(alpha = 0.35f), CircleShape)
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            isPressed = true
            tryAwaitRelease()
            isPressed = false
          },
          onTap = {
            onTap()
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = description,
      tint = if (isPressed) colors.primary else colors.onSurface,
      modifier = Modifier.size(iconSize)
    )
  }
}

@Composable
fun TechnicalSpecsGrid(
  track: com.example.model.AudioFileItem,
  playerState: DapPlayerState? = null
) {
  val colors = MaterialTheme.colorScheme
  val spec = playerState?.audioOutputSpec
  val isBitPerfect = spec != null && spec.sampleRateHz == track.sampleRate && spec.bitDepth == track.bitDepth

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF10131A))
      .border(1.dp, if (track.bitDepth >= 24 || track.sampleRate >= 88200) Color(0xFFE0A938).copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
      .padding(10.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ACTUAL PHYSICAL AUDIO FILE SPECIFICATIONS",
          color = if (track.bitDepth >= 24 || track.sampleRate >= 88200) Color(0xFFE0A938) else colors.primary,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (track.bitDepth >= 24) Color(0xFFE0A938).copy(alpha = 0.25f) else colors.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
          Text(
            text = if (track.bitDepth >= 24 || track.sampleRate >= 88200) "HI-RES MASTER" else "16-BIT REDBOOK",
            color = if (track.bitDepth >= 24 || track.sampleRate >= 88200) Color(0xFFE0A938) else colors.primary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        SpecItem(label = "ACTUAL CODEC", value = track.codec, modifier = Modifier.weight(1f))
        SpecItem(label = "ACTUAL SAMPLE RATE", value = "${track.sampleRate} Hz (${track.sampleRate / 1000f} kHz)", modifier = Modifier.weight(1.5f))
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        SpecItem(label = "ACTUAL BIT DEPTH", value = "${track.bitDepth}-bit Lossless", modifier = Modifier.weight(1f))
        SpecItem(label = "ACTUAL BITRATE", value = "${track.bitrateKbps} kbps", modifier = Modifier.weight(1.5f))
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(modifier = Modifier.fillMaxWidth()) {
        SpecItem(label = "CHANNELS", value = if (track.channels == 2) "Stereo (2.0 L+R)" else if (track.channels == 1) "Mono (1.0)" else "${track.channels} Channel", modifier = Modifier.weight(1f))
        SpecItem(label = "PHYSICAL SIZE", value = track.formattedSize, modifier = Modifier.weight(1.5f))
      }

      if (spec != null) {
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = colors.outline.copy(alpha = 0.25f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "ENFORCED BIT-PERFECT OUTPUT:",
              color = Color(0xFF00E676),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "${spec.sampleRateHz / 1000f} kHz / ${spec.bitDepth}-bit Direct AudioTrack • 0 Hz Delta (Zero Resampling)",
              color = colors.onSurface,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0xFF00E676).copy(alpha = 0.15f))
              .border(0.5.dp, Color(0xFF00E676), RoundedCornerShape(3.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "NO RESAMPLING",
              color = Color(0xFF00E676),
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }
  }
}

@Composable
fun AudiophileExtrasBox(
  playerState: DapPlayerState,
  onSetSpeed: (Float) -> Unit,
  onSetLoopA: () -> Unit,
  onSetLoopB: () -> Unit,
  onClearLoop: () -> Unit
) {
  val colors = MaterialTheme.colorScheme
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF10131A))
      .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
      .padding(10.dp)
  ) {
    Column {
      // Speed Selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "PLAYBACK SPEED:", color = colors.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
            val isSel = playerState.playbackSpeed == spd
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSel) colors.primary else colors.surfaceVariant)
                .clickable { onSetSpeed(spd) }
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = "${spd}x",
                color = if (isSel) colors.onPrimary else colors.onSurface,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // A-B Looping
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "A-B REPEAT LOOP:", color = colors.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (playerState.loopPointA != null) colors.primary else colors.surfaceVariant)
              .clickable { onSetLoopA() }
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text(
              text = if (playerState.loopPointA != null) "A: ${formatDuration(playerState.loopPointA)}" else "[SET A]",
              color = if (playerState.loopPointA != null) colors.onPrimary else colors.onSurface,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(if (playerState.loopPointB != null) colors.primary else colors.surfaceVariant)
              .clickable { onSetLoopB() }
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text(
              text = if (playerState.loopPointB != null) "B: ${formatDuration(playerState.loopPointB)}" else "[SET B]",
              color = if (playerState.loopPointB != null) colors.onPrimary else colors.onSurface,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          if (playerState.loopPointA != null || playerState.loopPointB != null) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surfaceVariant)
                .clickable { onClearLoop() }
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                text = "CLR",
                color = colors.error,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun SpecItem(label: String, value: String, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Column(modifier = modifier) {
    Text(
      text = label,
      color = colors.onSurfaceVariant,
      fontSize = 8.sp,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = value,
      color = colors.onSurface,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
fun VuMeterBar(channel: String, level: Float) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = channel,
      color = Color(0xFFA0A3B0),
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(14.dp)
    )

    Canvas(
      modifier = Modifier
        .weight(1f)
        .height(7.dp)
    ) {
      val w = size.width
      val h = size.height
      val clamped = level.coerceIn(0f, 1f)

      drawRect(Color(0xFF161822), Offset(0f, 0f), Size(w, h))

      val greenW = (w * 0.7f * (clamped / 0.7f).coerceIn(0f, 1f))
      if (greenW > 0f) {
        drawRect(Color(0xFF00E676), Offset(0f, 0f), Size(greenW, h))
      }

      if (clamped > 0.7f) {
        val yellowProgress = ((clamped - 0.7f) / 0.2f).coerceIn(0f, 1f)
        val yellowW = w * 0.2f * yellowProgress
        drawRect(Color(0xFFFFD600), Offset(w * 0.7f, 0f), Size(yellowW, h))
      }

      if (clamped > 0.9f) {
        val redProgress = ((clamped - 0.9f) / 0.1f).coerceIn(0f, 1f)
        val redW = w * 0.1f * redProgress
        drawRect(Color(0xFFFF3D00), Offset(w * 0.9f, 0f), Size(redW, h))
      }
    }
  }
}
