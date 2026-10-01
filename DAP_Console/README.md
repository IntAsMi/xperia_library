# 🎛️ DAP Console — Audiophile Digital Audio Player (v2.0)

A high-performance, folder-based Digital Audio Player (DAP) designed for dedicated audiophiles, featuring native SD card navigation, bit-perfect specifications readout, multi-disk timing breakdowns, interactive waveform scrubbers, and live reactive DSP visualizers.

---

## 🚀 Key Features & Capabilities

### 1. 🎛️ Minimalist Black & White Sony DAP Silhouette Icon
- Custom adaptive icon featuring a clean white vector silhouette of an audiophile Sony Walkman DAP on a solid `#000000` pitch black background.
- Generated and formatted across all mipmap densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) with circular raster masks.

### 2. ⚡ Lightning-Fast SD Card Folder Loading & Room Caching
- **Instant Folder Opening (< 20ms)**: Eliminates synchronous IPC blocking calls. Uses intelligent filename heuristic estimation for instant track rendering.
- **Room Database Metadata & Waveform Cache**: Caches tags, durations, bitrates, sample rates, and pre-computed waveform peak data into SQLite/Room for sub-millisecond repeat access.
- **Scanning Mode**: Supports both **Automatic** (background indexing upon folder change) and **Manual** (user-triggered re-indexing with visual progress bar).

### 3. 📻 Android System Media Playing & Background Playback
- **MediaSessionService**: Integrated with `androidx.media3.session.MediaSessionService` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- **System Media Controls**: Shows in Android Quick Settings, Lock Screen, and Bluetooth car/headset displays with album title, track position scrubber, and full playback controls.
- **Uninterrupted Background Audio**: Plays seamlessly with screen locked or while multitasking in other apps.

### 4. 📱 Two Home Screen Widgets
- **Simple 2x1 Widget (`DapSimpleWidgetProvider`)**: Compact widget displaying the track title, codec & technical spec (`FLAC 96kHz/24b`), playback state, and current elapsed / total time. Tap opens the app.
- **Controls 4x1 Widget (`DapControlsWidgetProvider`)**: Full interactive widget featuring Previous, Play/Pause toggle, Next buttons, track title, folder name, and real-time timings.

### 5. 🔄 Orientation Flexibility & Landscape Mode Optimization
- **Lock Portrait Mode Option**: Setting in preferences to lock the display strictly in portrait orientation or enable auto-rotation.
- **Landscape 2-Pane Vertical Split**: In landscape, the screen splits vertically into two balanced columns:
  - *Left Pane*: Real-time 32-band reactive spectrum visualizer, stereo VU dB meters, technical audio specifications grid, and output device routing banner.
  - *Right Pane*: Interactive waveform progress scrubber, multi-level timings (Song, Disk, Folder), large tactile buttons, speed selector, and A-B looper.

### 6. 🎧 Audiophile Audio Tuning Drawer (Now Playing)
- **Audio Output Switcher & Live Specs**: Detects and switches between Built-In Speaker, 3.5mm Analogue Headphone Jack, Bluetooth (A2DP), and USB DACs. Displays real DAC stream parameters (sample rate, bit depth, bit rate, uncompressed vs lossy transcode).
- **Audio Phase Switch**: Normal (0°) vs Inverted (180° reverse polarity).
- **Channel Isolation**: Stereo (L+R), Left Only (Right muted), Right Only (Left muted), and Mono Downmix.
- **Visualizer Channel Filter**: Toggle visualizers to monitor Stereo (L+R), Left Only, or Right Only.

### 7. ⏱️ 3-Level Hierarchical Timings (Song, Disk, Folder)
- **Song**: Elapsed (`01:24`), Remaining (`-03:15`), Total (`04:39`).
- **Disk**: `DISK 2 REMAINING: -28:14 | TOTAL: 54:30` (automatically parses disk numbers such as `2.01 - Sonata F Moll...` or ID3 disk tags and groups tracks per disk).
- **Folder**: `FOLDER REMAINING: -01:14:22 | TOTAL: 02:45:10`.

### 8. 〰️ Song Waveform Progress Line
- Interactive audio waveform scrubber rendered directly on the progress line.
- Highlights played vs unplayed waveform amplitude peaks in accent colors.
- Supports both horizontal dragging and instant tapping to seek to any point in the track.

### 9. 🎚️ Tactile Buttons with Tap & Hold
- Previous button: tap to skip track; hold to continuously rewind (-5s every 250ms).
- Next button: tap to skip track; hold to continuously fast-forward (+5s every 250ms).
- Play/Pause: prominent tactile button with subtle haptic feedback.

### 10. ⚙️ Additional Settings
- **Font Size**: Small (DAP Compact), Standard, Large, and Extra Large scaling.
- **Sleep Timer**: Off, 15m, 30m, 45m, 60m, End of Song, End of Disk/Folder.
- **Multi-Output Playback**: Option to output audio through speaker simultaneously.
- **RAM Pre-Buffering**: Shows loading progress bar for large (>40MB) high-res FLAC / DSD files.
- **Opaque Black Bottom Bar**: Browser bottom area from docked miniplayer downwards is solid `#000000` opaque black with zero blur or transparency.
