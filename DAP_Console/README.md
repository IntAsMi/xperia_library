# 🎛️ DAP Console — Audiophile Folder-Based Music Player

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20%7C%20API%2024--36-FF8A00.svg)](https://developer.android.com)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Engine-Media3%20ExoPlayer%20(Gapless)-00E5FF.svg)](https://developer.android.com/guide/topics/media/media3)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

> A dedicated, lightning-fast, folder-navigation-only Digital Audio Player (DAP) designed for audiophiles who organize music by folders on their SD Card or internal storage. Featuring deep technical metadata inspection, true gapless playback, comprehensive track & folder timing telemetry, stereo VU peak meters, and customizable studio themes. Inspired by the raw performance and clarity of classic Foobar2000.

---

## 📥 Direct APK Download

The installable standalone `.apk` package is ready right here:

| File | Location | Description |
| :--- | :--- | :--- |
| **`DAP_Console.apk`** | `DAP_Console/` | Standalone installable release package |
| **`DAP_Console.apk`** | Root directory | Quick access root copy |

### Installation Instructions
1. Download `DAP_Console.apk` to your Android device or SD card.
2. Tap the file in your notification panel or file manager.
3. Allow "Install unknown apps" if prompted.
4. Tap **Install** and launch DAP Console!

---

## 🌟 Key Features

### 📁 1. Pure Folder Navigation (No Clutter, No Scraping)
* **Zero Bloat**: No slow media scanner scraping, no confusing artist/album/genre auto-tag categorizations that break multidisc albums.
* **Persistent SD Card Root**: Select your music root folder from your SD Card once via Android's Storage Access Framework. The app will **always launch directly inside this folder** on every subsequent open.
* **Instant Breadcrumb & Fast Up-Navigation**: Navigate through folder hierarchies in milliseconds with the `[..]` up button and real-time directory paths.
* **Folder Level Controls**: Tap **PLAY** or **SHUFFLE** to play an entire folder at once with continuous queueing.
* **Total Folder Telemetry**: View total track count, total playback duration, and storage size of every folder at a glance.

### 🔬 2. Deep Audio Specifications & Metadata (Foobar2000 Style)
Unlike generic music players that hide technical details behind album art, DAP Console showcases all audio specs in a dedicated technical readout:
* **Audio Codec**: FLAC, WAV, ALAC, DSD (DSF/DFF), MP3, OGG, OPUS, AAC, APE, AIFF.
* **Exact Sample Rate**: e.g., `44,100 Hz (44.1 kHz)`, `48,000 Hz`, `96,000 Hz (96 kHz)`, `192,000 Hz`.
* **Bit Depth**: `16-bit PCM`, `24-bit Hi-Res`, `32-bit Float`, `1-bit Direct Stream Digital (DSD)`.
* **Bitrate**: Real-time transmission bitrate in kbps (e.g. `320 kbps`, `960 kbps`, `2950 kbps`, `5644 kbps`).
* **Channels**: `Stereo (2.0)`, `Mono (1.0)`, or multi-channel surround.
* **Exact File Size**: Precise megabytes / kilobytes and byte counts.
* **File System Path**: Complete raw storage path to the source audio file.
* **No Album Art**: Distraction-free, minimal nostalgic DAP console layout that prioritizes audio playback fidelity and instant responsiveness.

### ⏱️ 3. Dual Timings & Remaining Calculations
Precision timekeeping for albums and audiobooks:
* **Track Elapsed**: `02:14`
* **Track Remaining**: `-02:16`
* **Track Total Duration**: `04:30`
* **Folder / Album Total Duration**: Sum of all tracks in current directory (e.g. `48:32`)
* **Folder / Album Remaining Duration**: Current track remainder plus all subsequent tracks remaining in the album (e.g. `-28:14`)
* **Track Position**: `Track 4 of 12`

### ⚡ 4. Native Gapless Audio Engine (Media3 ExoPlayer)
* **Zero Gap**: Seamless track transitions without silence or buffer clipping (perfect for classical movements, live concerts, progressive rock, and DJ mixes).
* **Low Latency**: Lightweight memory footprint (< 28 MB RAM).
* **Hardware Volume & Scrubbing**: High-precision seekbar with millisecond scrub capability.
* **Tactile Controls**: Play/Pause, Next, Previous, Fast-forward (+10s), Fast-rewind (-10s).
* **Repeat Modes**: Repeat Off, Repeat Folder (Continuous), Repeat Track (Single).
* **Shuffle Engine**: Folder-scoped randomizer.
* **Playback Speed**: 0.75x, 1.0x, 1.25x, 1.5x with pitch compensation.
* **A-B Looper**: Set Point A and Point B to practice audio loops or transcribe music.

### 📊 5. Real-Time Stereo VU Peak Meters
* Responsive dual-channel (Left / Right) VU peak indicators with multi-segment green, yellow, and red clipping warnings (-40 dB to 0 dB).

### 🎨 6. 5 Studio Theme Settings
1. **Dark**: Technical studio slate aesthetic with high-contrast amber accents.
2. **Light**: Clean studio day theme with crisp typography.
3. **System Default**: Seamlessly follows Android OS day/night mode.
4. **Material You Expressive**: Dynamic color extraction from your system wallpaper (Android 12+).
5. **AMOLED Black + Material You Accent**: Pure `#000000` pitch black background for extreme battery saving on OLED displays, paired with Material You dynamic color accents.

---

## 🛠️ Build & Architecture

* **Build Tool**: Gradle (Kotlin DSL)
* **SDK Compatibility**: `minSdk 24` (Android 7.0) to `targetSdk 36` (Android 15+)
* **Dependencies**:
  * `androidx.media3:media3-exoplayer:1.5.1`
  * `androidx.media3:media3-common:1.5.1`
  * `androidx.compose.material3:material3`
  * `androidx.documentfile:documentfile:1.0.1`
  * `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7`

### How to Build
```bash
gradle assembleDebug
# Generated APK: app/build/outputs/apk/debug/app-debug.apk
```
