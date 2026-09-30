# 📷 Sony Xperia 1 VIII — Photography Pro (Classic 1 V Heritage Edition)

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20%7C%20API%2034--36-FF6000.svg)](https://developer.android.com)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Camera](https://img.shields.io/badge/Camera-CameraX%201.5.0-00E676.svg)](https://developer.android.com/training/camerax)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

> A faithful, standalone replica of the iconic **Sony Xperia 1 V Photography Pro** app adapted for the **Xperia 1 VIII**, uniting the tactile Alpha camera heritage with next-generation optical periscope zoom (16mm–170mm), dedicated Tele-Macro, and real-time AI Subject Pose Estimation AF.

---

## 📥 Direct APK Downloads (Ready to Install)

The installable standalone `.apk` package is included directly in this repository:

| File | Location | Direct Link |
| :--- | :--- | :--- |
| **PhotographyPro-Xperia-1-VIII.apk** (Latest Release) | Root folder | [`./PhotographyPro-Xperia-1-VIII.apk`](./PhotographyPro-Xperia-1-VIII.apk) |
| **PhotographyPro-Xperia-1-VIII.apk** (Release folder) | `release/` directory | [`./release/PhotographyPro-Xperia-1-VIII.apk`](./release/PhotographyPro-Xperia-1-VIII.apk) |

### Quick Installation:
1. Download `PhotographyPro-Xperia-1-VIII.apk` directly to your Android device.
2. Tap the downloaded file in your notifications or file manager.
3. If prompted, toggle **"Allow installation from this source"**.
4. Tap **Install** and enjoy the authentic Sony Alpha Photography Pro experience!

---

## ✨ Features & Highlights

### 🎯 1. Authentic Sony Alpha Design & Controls
* **Sony Alpha Mode Dial**:
  * **BASIC**: Intuitive smartphone framing, quick aspect ratio switching, and color modes.
  * **AUTO**: Intelligent scene recognition, Auto HDR, and multi-point AF.
  * **P (Program Auto)**: Camera automatically controls shutter and aperture while giving direct control over ISO, EV, WB, Focus, and Drive.
  * **S (Shutter Priority)**: Manual shutter speed adjustment from **1/8000s up to 30s**.
  * **M (Manual Exposure)**: Full manual control over Shutter Speed, ISO, and Manual Focus with distance peaking.
  * **MR (Memory Recall)**: Instantly recall custom shooting setups (**M1**, **M2**, **M3**).

### 🔍 2. Xperia 1 VIII Next-Gen Optics + 1 V Heritage Mode
* **16mm F2.0 Ultra-Wide**: 1/2.0" Exmor T sensor (123° field of view).
* **24mm F1.8 Wide**: 1/1.28" Exmor T 48MP dual-layer transistor sensor.
* **48mm F1.8 Lossless Crop**: 2x sensor zoom with full optical resolution.
* **85mm–170mm F2.3–F3.5 Continuous Optical Periscope Zoom**: True continuous mechanical optical zoom with smooth stepless slider and preset stops (85mm, 135mm, 170mm).
* **Dedicated Tele-Macro (4cm)**: Direct toggle on the telephoto module for extreme macro close-ups with assisted focus peaking.
* **Hardware Profile Switcher**: Toggle on-the-fly between **Xperia 1 VIII** (16–170mm + AI) and **Xperia 1 V Classic** (16–125mm) profiles.

### 🔘 3. Two-Stage Textured Physical Shutter Emulation
Modeled after the textured diamond-knurled hardware shutter key on Xperia flagships:
* **Half-Press / Hold**: Locks AF/AE, calculates exposure meter, displays green Alpha target reticles, and emits the authentic **1750 Hz Sony double-beep**.
* **Full-Press / Release**: Triggers focal plane curtain click, screen flash, and photo capture.
* **High-Speed Continuous Burst**:
  * **Ultra (60 fps)**: Next-gen Xperia 1 VIII continuous AF/AE burst.
  * **Hi (30 fps)** & **Lo (10 fps)** with live burst counters.
  * **Self-Timers**: 3-second and 10-second countdowns with accelerating audio ticks.

### 🤖 4. AI Subject Recognition & Pose Estimation AF
Powered by Sony AI Processing Unit algorithms:
* **AI Human**: Real-time Eye AF + Human Skeletal Pose Estimation with joint tracking points.
* **AI Animal & Bird**: Real-time Eye AF tracking for pets and wildlife.
* **AI Vehicle**: Fast lock for motorsport, trains, and aircraft.
* **399-Point Wide AF Array**: High-density phase detection bracket visualization.

### 📐 5. Professional Viewfinder Displays & Overlays
* **Dual-Axis Electronic Level Gauge**: Real-time pitch and roll horizon that illuminates **Sony Alpha Green (`#00E676`)** when aligned within ±1.0°.
* **Live Multi-Channel Histogram**: Dynamic RGB & Luminance histogram responsive to exposure, ISO, and scene illumination.
* **Focus Peaking**: Color-assisted edge detection in Manual Focus (MF) mode with selectable peaking colors (**White**, **Yellow**, **Red**).
* **Framing Grids**: Rule of Thirds, Square (1:1), and Golden Ratio grid overlays.
* **Aspect Ratios**: 4:3, 16:9, 1:1, and Sony **21:9 CinemaWide**.

### 🎨 6. Sony Creative Look Color Science
Direct access to official Sony Alpha Creative Looks:
* `ST` (Standard) — Balanced contrast and natural saturation.
* `NT` (Neutral) — Low saturation, ideal for color grading.
* `VV` (Vivid) — Saturated, punchy landscapes and flowers.
* `FL` (Film) — Matte mood, deep blue/green cast, and lifted blacks.
* `IN` (Instant) — Faded vintage aesthetic with soft highlights.
* `SH` (Soft High-key) — Airy, transparent tones for portraits.
* `BW` (Black & White) — Classic monochrome with high microcontrast.
* `SE` (Sepia) — Nostalgic warm amber monochrome.

### 🖼️ 7. Playback & Full EXIF Inspector
* Review captured shots and bundled high-res Sony Alpha sample photography.
* Detailed EXIF display: Lens, Shutter Speed, Aperture, ISO, EV compensation, Creative Look, Format, and live mini-histogram.
* Native Android Share and Delete actions.

---

## 🛠️ Building from Source

### Prerequisites
* Android Studio Ladybug (or newer) / JDK 17+
* Android SDK 36 (compileSdk 36, minSdk 24)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/<your-username>/photography-pro-xperia.git
cd photography-pro-xperia

# Build Debug APK
gradle assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
```bash
adb install -r release/PhotographyPro-Xperia-1-VIII.apk
```

---

## 📁 Repository Structure

```
├── PhotographyPro-Xperia-1-VIII.apk   # Direct installable APK (Root)
├── release/
│   └── PhotographyPro-Xperia-1-VIII.apk  # Prebuilt APK in release folder
├── app/
│   ├── src/main/java/com/example/
│   │   ├── audio/                     # Synthesized Sony Alpha sounds & haptics
│   │   ├── data/                      # Room Database (EXIF storage & MR presets)
│   │   ├── model/                     # Lens, Mode, Drive, Creative Look models
│   │   ├── sensor/                    # SensorManager Electronic Level Gauge
│   │   ├── ui/
│   │   │   ├── components/            # Viewfinder, Histogram, Level Gauge, Shutter, Wheels
│   │   │   ├── screens/               # Main Camera & Playback Gallery screens
│   │   │   └── theme/                 # Sony Alpha Color Palette, Typography & Theme
│   │   └── MainActivity.kt            # Edge-to-edge Activity entry point
│   ├── src/main/res/                  # Icons, Drawables, Mipmaps, Strings
│   └── build.gradle.kts               # App build configuration & dependencies
├── build.gradle.kts                   # Root build script
├── settings.gradle.kts                # Project configuration
└── README.md                          # Repository documentation
```

---

## 📄 License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.

*Disclaimer: Sony, Xperia, Alpha, Exmor T, and Photography Pro are trademarks or registered trademarks of Sony Group Corporation. This project is an independent open-source recreation created for mobile photography enthusiasts.*
