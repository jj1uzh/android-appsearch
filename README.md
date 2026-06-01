# AppSearcher

AppSearcher is a smart Android application designed to provide an ambiguous search experience for installed apps on your device, specifically optimized for Japanese users. It seamlessly handles Kanji, Hiragana, Katakana, and Romaji search queries using advanced morphological analysis.

## Core Features

- **Ambiguous Search Engine:** Integrates **Kuromoji** (ipadic) and **Android ICU Transliterator** to convert Kanji into Kana, and Kana into Romaji. This allows users to search for apps using natural pronunciations or even Romaji regardless of how the app name is written.
- **Smart Recents:** Remembers the apps you launch and keeps them pinned at the top of your drawer. Built natively using Android's Preferences DataStore.
- **Modern Architecture:** Entirely built using Kotlin, Jetpack Compose (Material Design 3), MVVM, and Coroutines.

## Directory Structure

Here is an overview of the core project structure:

```text
android-appsearch/
├── app/
│   ├── build.gradle.kts                 # App-level build configurations and dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # Contains intent <queries> to allow fetching app lists
│       │   ├── res/                     # UI resources, themes, and icons
│       │   └── java/com/example/appsearcher/
│       │       ├── MainActivity.kt         # Application entry point
│       │       ├── AppInfo.kt              # Data model representing an installed app
│       │       ├── AppProvider.kt          # Interfaces with PackageManager to fetch installed apps
│       │       ├── SearchNormalizer.kt     # The core string processing engine (Kuromoji/Transliterator)
│       │       ├── RecentAppsRepository.kt # DataStore logic for tracking recently launched apps
│       │       ├── AppSearchViewModel.kt   # State management and filtering logic
│       │       └── ui/main/
│       │           └── MainScreen.kt       # Jetpack Compose UI definitions (SearchBar, Grid, Lists)
├── build.gradle.kts                     # Project-level build configuration
├── gradle.properties                    # Gradle environment properties
└── settings.gradle.kts                  # Gradle modules and repositories setup
```

## Developing Process & Building

If you wish to modify the application, the primary components to look at are `SearchNormalizer.kt` (for adjusting how Japanese text is mapped) and `MainScreen.kt` (for adjusting the UI look and feel).

To compile the application and install it directly onto your connected Android device or running emulator, use the Gradle wrapper from the terminal:

```bash
./gradlew installDebug
```

> **Note to AI Agents/Developers:** You can seamlessly use the `./gradlew installDebug` command directly in the environment to push changes to the connected device during development.
