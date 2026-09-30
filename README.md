# 🎛️ DAP Console & Photography Pro Heritage

This repository contains two dedicated Android applications:
1. **DAP Console** (`DAP_Console/`): A lightweight, lightning-fast, folder-navigation-only Digital Audio Player (DAP) with detailed audio file specifications, gapless playback, duration remaining of song and folder/album, 5 themes (Dark, Light, System, Material U Expressive, AMOLED Black + Material U Accent), no album art, and classic Foobar2000 features.
2. **Photography Pro Heritage** (`Photography_Pro_Heritage/`): The complete, standalone replica of the Sony Xperia 1 V / 1 VIII Photography Pro Alpha camera app.

---

## 📥 Direct APK Downloads

| Application | APK File | Location | Direct Link |
| :--- | :--- | :--- | :--- |
| **DAP Console** (Latest Release) | `DAP_Console.apk` | `DAP_Console/` & Root | [`./DAP_Console/DAP_Console.apk`](./DAP_Console/DAP_Console.apk) |
| **Photography Pro** (Heritage Edition) | `PhotographyPro-Xperia-1-VIII.apk` | `Photography_Pro_Heritage/` & Root | [`./Photography_Pro_Heritage/PhotographyPro-Xperia-1-VIII.apk`](./Photography_Pro_Heritage/PhotographyPro-Xperia-1-VIII.apk) |

---

## 🎛️ DAP Console Overview

### Highlights:
- **Pure Folder Navigation**: Direct SD Card root selection. Each time the app opens, it immediately puts you inside your chosen music folder.
- **Deep Technical Specifications**: Codec (FLAC, WAV, ALAC, DSD, MP3, OPUS, OGG, AAC), Sample Rate (Hz/kHz), Bit Depth (16-bit, 24-bit, 32-bit), Bitrate (kbps), Channels (Stereo/Mono), File Size, and File System Path.
- **Dual Timings**: Elapsed time, remaining song duration (`-02:16`), folder total duration (`48:32`), and folder remaining duration (`-28:14`).
- **Gapless Playback Engine**: Powered by Android Media3 ExoPlayer with 0 ms transition gap.
- **Stereo VU Meter**: Real-time dual-channel peak meters (-40 dB to 0 dB).
- **Themes**:
  1. *Dark* (Studio Slate)
  2. *Light* (Day Studio)
  3. *System Default*
  4. *Material You Expressive*
  5. *AMOLED Black with Material You Accent* (Pure `#000000` true black)
- **Audiophile Extras**: A-B Repeat Loop, Speed selector (0.75x - 1.5x), Seekbar with scrub precision, Shuffle, Repeat Folder/Track.
- **No Album Art**: Distraction-free, minimal nostalgic DAP console layout prioritizing lightning-fast performance.

For complete DAP Console documentation, see [`DAP_Console/README.md`](./DAP_Console/README.md).

---

## 📷 Photography Pro Heritage Overview

The previous Sony Xperia 1 V / 1 VIII Photography Pro application is packaged and archived in [`Photography_Pro_Heritage/`](./Photography_Pro_Heritage/):
- Full source code in `Photography_Pro_Heritage/app/`
- Direct APK release: [`Photography_Pro_Heritage/PhotographyPro-Xperia-1-VIII.apk`](./Photography_Pro_Heritage/PhotographyPro-Xperia-1-VIII.apk)
- Alpha dials, 16mm-170mm continuous optical zoom, 60fps burst, dual-stage shutter emulation, and electronic artificial horizon.

---

## 🛠️ Building DAP Console

```bash
# Build DAP Console Debug APK
gradle assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
# or copy from:
# DAP_Console/DAP_Console.apk
```
