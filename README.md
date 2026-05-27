# 🌌 Zyphra Flux 
> 一個簡潔現代易於使用的結合 yt-dlp 的 Android 影片下載器。

[![Version](https://img.shields.io/badge/Version-1.14514-purple.svg?style=for-the-badge)](https://github.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge)](https://developer.android.com)

**Zyphra Flux** 不僅僅是一個下載器，它將強大的 `yt-dlp` 核心與優雅的系統原生介面完美結合，並且可使用系統內建的分享選單快速分享連結。由於本人看慣了網路上大多此類程式或網站皆充斥著許多廣告、惡意連結，並且要下載影片必須經過許多的操作，十分不便，因此我變開發了這款沒有任何綑綁、廣告，使用無腦簡單的社群影片下載器。
---

## ✨ 核心特色

- 🎨 **Android 17 視覺風格**：採用最新懸浮導覽列設計，介面靈動且直觀。
- 🌈 **Material 3 規範**：全介面支援自適應深淺色模式，色彩過渡自然。
- 🚀 **全自動核心管理**：首次啟動自動配置 `yt-dlp` 核心，背景靜默更新，無需手動干預。
- 💎 **高畫質支援**：進階模式解鎖 **2K / 4K** 下載，支援最高 **320kbps** 無損音質提取。
- 📂 **智慧儲存與管理**：
    - 影片預設儲存至 `Movies/ZyphraFlux`
    - 音檔預設儲存至 `Music/ZyphraFlux`
    - 支援 MediaStore 整合，下載完畢相簿立刻可見。
- 🔗 **深度系統整合**：支援系統「分享選單」直接跳轉下載，自動辨識多種主流影音平台。

---

## 📱 支援平台
**注意 : 目前 yt-dlp 還尚未支援 Threads 影片下載 !**
支援超過 1000+ 個網站 (yt-dlp 支援的都支援)，包含但不限於：

| 平台 | 支援程度 | 平台 | 支援程度 |
| :--- | :---: | :--- | :---: |
| **YouTube** | ✅ 4K / 60fps | **TikTok** | ✅ 無浮水印 |
| **Instagram** | ✅ 影片 / Reels | **Bilibili** | ✅ 高畫質 / UA 偽裝 |
| **Facebook** | ✅ 公開影片 | **Pornhub** | 🥵 這肯定要的 |
| **X (Twitter)** | ✅ 原畫質 | **Twitch** | ✅ 串流錄製 |

---

## 🛠️ 模式說明

### 🟢 標準模式 (Standard)
- **定位**：日常快速使用。
- **限制**：畫質鎖定為 **1080p60**。
- **特點**：極簡介面，一鍵即下，檔名自動命名為影片標題。

### 🟣 進階模式 (Advanced)
- **特點**：
    - **自定義路徑**：可透過系統文件夾選取器，自由選擇儲存位置。
    - **極致品質**：支援 4K 影片與 320kbps MP3 選項。
    - **獨立設定**：影片與音檔可分別設定不同的下載路徑。

---

## 🏗️ 技術架構
- **Language**: Kotlin 1.9.22
- **UI Framework**: Material Design 3 (M3)
- **Asynchronous**: Kotlin Coroutines + Lifecycle Scope
- **Core Engine**: `yt-dlp` via [youtubedl-android (JunkFood02 fork)](https://github.com/junkfood02/youtubedl-android)
- **Media Support**: FFmpeg 
- **Architecture**: ViewBinding pattern

---

## ⚖️ 免責聲明
本程式僅供個人學習與研究用途。請使用者在下載任何內容前，務必確認已獲得原作者授權，並遵守當地法律與各平台的服務條款。開發者不對使用者的任何侵權行為負責 !!

---

## 🤝 致謝
**特別感謝以下開源專案提供的強大支援：**
1. **yt-dlp - 世界上最強大的命令行下載工具。**
2. **youtubedl-android - 為 Android 提供的 Python 運行環境封裝。**



## ⭐ 如果你覺得這個專案有幫助，請給它一個 Star！這對我來說是莫大的鼓勵。
   
---

## 📥 安裝與建構
1. **Clone 專案**
2. **環境要求**
   - Android Studio Hedgehog (2023.1.1) 以上版本
   - JDK 17
   - Android SDK API 24+ (Android 7.0+)
3.**編譯**
直接在 Android Studio 中點擊 Sync Project with Gradle Files 並運行。
