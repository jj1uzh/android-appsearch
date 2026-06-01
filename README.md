# AppSearcher

AppSearcher is a smart Android application designed to provide an ambiguous search experience for installed apps on your device, specifically optimized for Japanese users. It seamlessly handles Kanji, Hiragana, Katakana, and Romaji search queries using advanced morphological analysis.

## Core Features

- **Ambiguous Search Engine:** Integrates **Kuromoji** (ipadic) and **Android ICU Transliterator** to convert Kanji into Kana, and Kana into Romaji. This allows users to search for apps using natural pronunciations or even Romaji regardless of how the app name is written.
- **Smart Recents:** Remembers the apps you launch and keeps them pinned at the top of your drawer. Built natively using Android's Preferences DataStore.
- **Modern Architecture:** Entirely built using Kotlin, Jetpack Compose (Material Design 3), MVVM, and Coroutines.

## Building and Installing

To compile the application and install it directly onto your connected Android device or running emulator, use the Gradle wrapper from the terminal:

```bash
./gradlew installDebug
```
