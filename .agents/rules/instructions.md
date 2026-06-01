# AppSearcher Developer Instructions

## Directory Structure
Here is an overview of the core project structure you will be working with:

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

If you need to modify the application logic:
*   The primary components for adjusting how Japanese text is mapped are in `SearchNormalizer.kt`.
*   Adjustments to the UI look and feel belong in `MainScreen.kt`.

To compile the application and install it directly onto the connected Android device or running emulator, always use the Gradle wrapper from the terminal:

```bash
./gradlew installDebug
```

You can seamlessly use this command to push changes to the connected device during development.
