# 🎛️ DAP Console & Photography Pro Heritage

This repository contains two dedicated Android applications:
1. **DAP Console** (`DAP_Console/`): A lightweight, lightning-fast, folder-navigation-only Digital Audio Player (DAP) with detailed audio file specifications, gapless playback, duration remaining of song, disk, and folder, 5 themes (Dark, Light, System, Material U Expressive, AMOLED Black + Material U Accent), 2 widgets, real reactive visualizers, and audiophile controls.
2. **Photography Pro Heritage** (`Photography_Pro_Heritage/`): The complete, standalone replica of the Sony Xperia 1 V / 1 VIII Photography Pro Alpha camera app.

---

## 📥 Direct APK Downloads

| Application | APK File | Location | Direct Link |
| :--- | :--- | :--- | :--- |
| **DAP Console** (v2.0 Audiophile Release) | `DAP_Console.apk` | `DAP_Console/` & Root | [`./DAP_Console/DAP_Console.apk`](./DAP_Console/DAP_Console.apk) |
| **Photography Pro** (Heritage Edition) | `PhotographyPro-Xperia-1-VIII.apk` | `Photography_Pro_Heritage/` & Root | [`./Photography_Pro_Heritage/PhotographyPro-Xperia-1-VIII.apk`](./Photography_Pro_Heritage/PhotographyPro-Xperia-1-VIII.apk) |

---

## 🎛️ DAP Console v2.0 Overview

### Key Capabilities:
- **Black & White Sony DAP Silhouette Icon**: Custom adaptive launcher icon and mipmap densities.
- **Instant Folder Loading (< 20ms)**: Room SQLite database caching and non-blocking heuristic extraction.
- **Android Media Playing & Background Playback**: Android system media notification tile, lock screen controls, and `MediaSessionService`.
- **Two Home Screen Widgets**: Simple 2x1 info widget and 4x1 interactive playback controls widget.
- **Landscape Optimization**: 2-pane vertical split layout in landscape mode; optional Portrait Mode Lock.
- **Audiophile Audio Tuning Drawer**: Audio phase reversal (0° / 180°), channel isolation (L / R / Mono), visualizer channel filter, and audio output routing with real DAC stream specifications.
- **3-Level Hierarchical Timings**: Displays elapsed/remaining time for current Song, current Disk (e.g. `2.01`), and current Folder.
- **Song Waveform Progress Line**: Interactive scrub bar with rendered amplitude waveform peaks.
- **Live Reactive DSP Visualizer**: Real 32-band spectrum analyzer and stereo VU peak meters reacting to audio playback.
- **Tactile Buttons with Tap & Hold**: Press & hold Previous/Next to continuously rewind/fast-forward.
- **Extended Settings**: Font size scaling, sleep timer, multi-output playback, RAM pre-buffering, haptic feedback, and Automatic/Manual library scanning.

For complete DAP Console documentation, see [`DAP_Console/README.md`](./DAP_Console/README.md).
