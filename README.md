# HypnoStream 📺✨

**HypnoStream** is a modern, high-performance IPTV player engineered for **Android phones, tablets, and Android TV / Google TV**. Designed with an OLED-first electric cyan aesthetic, snappy spring physics, and full D-Pad remote & touch support, it delivers instant channel zapping and seamless VOD binge-watching.

---

## 📸 Screenshots

| Home Dashboard | Cinema Player |
| :---: | :---: |
| ![Dashboard](YOUR_DASHBOARD_IMAGE.jpg) | ![Player](YOUR_PLAYER_IMAGE.jpg) |

---

## ✨ Key Features

- **⚡ Sub-Second Cached Startup (Stale-While-Revalidate):** Compresses and caches massive playlists (45,000+ channels, movies, and series) locally via GZIP for instant `< 0.9s` launches while silently syncing server updates in the background.
- **🎬 True Immersive Cinema Playback:** Built on **AndroidX Media3 / ExoPlayer** with fast-start buffer tuning (`600ms` lock-in), zero-black-screen live channel zapping, automatic reconnection, and full-screen edge-to-edge playback.
- **👆 Context-Aware Kinetic Gestures & D-Pad Navigation:**
  - **Movies & Series (VOD):** Double-tap left/right for stacking `±10s` skip ripples with haptic feedback, plus automatic resume tracking and a **10-second Binge-Mode Next Episode** countdown.
  - **Live TV:** Double-tap left/right or press `CH+ / CH-` to instantly zap between channels.
  - **Touch Sliders:** Swipe vertically on the left edge for screen brightness and the right edge for volume.
- **🌐 Native Bilingual Support (English & Arabic RTL):** Instant one-tap switching between English and Arabic with automatic category translation and full right-to-left layout mirroring.
- **🔍 Smart Per-Tab Search History:** Remembers recent searches separately across Live TV, Movies, and Series as one-tap chips—eliminating tedious typing on TV remotes.
- **💬 Advanced Subtitle & Audio Controls:** Supports embedded multi-audio tracks, external `.srt` / `.vtt` / `.ass` files or URLs, and live subtitle timing sync (`±0.5s`).
- **📺 4K-Ready Android TV Banner & PiP:** Includes a super-sampled `1920×1080` leanback launcher banner for sharp 4K TV home screens and Picture-in-Picture multitasking on mobile.

---

## 🔌 Supported Playlist Sources

- **Xtream Codes API** (Live TV, VOD Movies, Series with Season/Episode metadata)
- **M3U / M3U8 Playlist URLs** (with automatic Xtream URL detection)
- **Raw M3U Text Import**

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3) with adaptive Phone, Tablet, and Android TV Leanback layouts
- **Media Engine:** AndroidX Media3 (`ExoPlayer`, `HlsMediaSource`)
- **Image Loading:** Coil
- **CI/CD:** Automated APK builds & 4K TV banner generation via GitHub Actions
