# ♟️ Modern Android Chess

A premium, feature-rich offline & local multiplayer **Android Chess Application** built with **Kotlin** and **Jetpack Compose**. 

Designed with a modern, glassmorphic UI aesthetic (inspired by Chess.com), high-performance 60fps animations, haptic feedback, local statistics tracking, offline Lichess puzzle engine, custom AI bot opponent, and zero-internet local multiplayer (Wi-Fi Local Network / Direct / Bluetooth).

---

## ✨ Features

### 🎮 Game Modes
- **Pass & Play (Local Offline)**: Two players on a single device with smart board rotation, move history timeline, move undo, and draw/resign options.
- **Offline Puzzles**: Over 1,000+ extracted offline Lichess puzzles with theme filtering, rating tracking, and hint assistance.
- **Play vs AI Bot**: Smart offline chess engine with adjustable difficulty levels.
- **Local Wi-Fi / Hotspot Multiplayer**: Instant low-latency peer-to-peer multiplayer across local network without any external internet server.
- **Bluetooth Multiplayer**: Connect and play directly with nearby Android devices.

### 🏆 Full Official Chess Rules Engine
- Standard movement & capture mechanics
- Check, Checkmate & Stalemate detection
- Castling (Kingside & Queenside)
- Pawn Promotion with interactive dialog selection
- *En Passant* captures
- Draw conditions: Threefold repetition, 50-move rule, Insufficient material

### 🎨 UI & UX Design
- **Interactive Animated Hero Header**: Continuous 3D canvas animation featuring infinite scrolling chessboard and floating ambient unicode pieces.
- **Glassmorphism Design System**: Sleek cards, dark green gradient themes, subtle drop shadows, and modern Material 3 typography.
- **Customizable Themes**: Multiple board colorways, piece styles, sound toggles, and customizable haptic feedback levels.
- **Interactive Game Replay**: Full PGN move timeline with step-by-step playback controls (Play, Pause, Jump to move).
- **User Profile & Stats**: Track wins, losses, win rates, game history, and play time locally.

---

## 🛠️ Architecture & Tech Stack

Built following **Clean Architecture**, **MVVM**, and modern Android engineering best practices:

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose + Material 3
- **Dependency Injection**: Hilt
- **State Management**: StateFlow, SharedFlow & ViewModel
- **Database**: Room Database (Game History & PGN storage)
- **Preferences**: DataStore Preferences
- **Async & Reactive**: Kotlin Coroutines & Flow
- **Network Protocol**: Custom Socket & Json Event Stream (TCP/UDP socket P2P)
- **Serialization**: Kotlinx Serialization
- **Graphics & Animation**: Compose Canvas & Infinite Transition Engine

---

## 📁 Package Structure

```
com.example.chess
├── animations/       # Custom canvas animations & infinite hero effects
├── core/             # Core application constants & dispatchers
├── database/         # Room Database entities, DAOs, & migrations
├── di/               # Hilt Dependency Injection modules
├── game/             # Pure Kotlin Chess Engine (Board, Rules, Move Generation)
├── history/          # Game history repository & timeline tracking
├── navigation/       # Navigation Compose graph & screen destinations
├── network/          # Socket-based P2P multiplayer protocol & serializers
├── profile/          # User profile state & data store
├── repository/       # Data repositories (Puzzles, Stats, Settings)
├── settings/         # App preferences & theme configuration
├── theme/            # Material 3 color schemes, shapes & typography
└── ui/               # Jetpack Compose UI Screens & Components
    ├── components/   # Reusable UI cards, dialogs & hero animation canvas
    └── screens/      # Home, Board, Bot, Puzzles, History, Profile, About screens
```

---

## ⚙️ Getting Started & Installation

### Prerequisites
- **Android Studio**: Jellyfish | 2023.3.1 or newer
- **JDK**: Java 17
- **Minimum SDK**: Android 8.0 (API Level 26)
- **Target SDK**: Android 14 (API Level 34)

### Building the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Yeamin-Talukder/Chess.git
   cd Chess
   ```

2. **Open in Android Studio**:
   Open Android Studio, select `Open an existing project`, and select the root directory.

3. **Build Debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

---

## 👤 Developer

Developed by **MD YEAMIN TALUKDER**

- GitHub: [@Yeamin-Talukder](https://github.com/Yeamin-Talukder)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
