# AGENTS.md — Agent & Developer Operational Guide

## 1. Project Overview & Mission
**WFO Tracker** is an ultra-lean, 100% offline native Android application designed to track and optimize Return-to-Office (WFO) compliance against monthly organizational targets (default: 40%). It features a live Android home screen widget powered by Jetpack Glance, a Material 3 Jetpack Compose UI, and zero external network footprint.

## 2. Non-Negotiable Architectural Rules
1. **Zero Network / Absolute Privacy**:
   - The application **must never** request the `android.permission.INTERNET` permission in `AndroidManifest.xml`.
   - No analytics SDKs, crash reporters, ad networks, telemetry, or remote services are permitted.
2. **Minimal Dependency Footprint**:
   - Avoid heavy frameworks when standard AndroidX or pure Kotlin language features suffice.
   - Do not add dependency injection frameworks (Hilt, Koin) unless explicitly justified. Use constructor injection and pragmatic service locators/factories.
   - Do not add Room/SQLite if DataStore Preferences and structured local JSON fulfill the storage requirements without KSP/annotation processing bloat.
3. **Platform Constraints**:
   - **Language**: Kotlin 2.x.
   - **Min SDK**: `26` (Android 8.0 Oreo). No desugaring hacks required for `java.time` APIs.
   - **Target / Compile SDK**: `35` (compatible with modern Android 15/16).
   - **UI**: Jetpack Compose with Material 3.
   - **Widget**: Jetpack Glance AppWidget.
4. **Directory Isolation**:
   - Root directory is strictly `C:\Z-Drive\Code\Widget\WFO-Tracker`.
   - Never create nested root folders.
   - Never modify files outside the project root unless instructed for SDK environment configuration.

## 3. Environment & Local Toolchain
- **Operating System**: Windows 11
- **Android Studio**: Android Studio (build AI-262 / 2026.x)
- **Active Build JDK**: Eclipse Temurin OpenJDK `21.0.13` LTS (`C:\Users\khans\.jdks\temurin-21`)
  - Configured project-locally in `gradle.properties` (`org.gradle.java.home`).
  - *Note*: Bundled JBR is OpenJDK 25.0.4 (`C:\Program Files\Android\Android Studio\jbr`), which triggers `Unsupported class file major version 69` in Gradle 8 / AGP 8. JDK 21 LTS is used for stable builds.
- **Android SDK Location**: `C:\Users\khans\AppData\Local\Android\Sdk`
- **Installed SDK Platforms**: `android-35`, `android-37.0`
- **Installed Build-Tools**: `35.0.0`, `36.0.0`, `37.0.0`
- **Gradle Version**: Gradle `8.12` (via wrapper)
- **Android Gradle Plugin (AGP)**: `8.8.2`
- **Kotlin Version**: `2.0.21` (with native Compose Compiler plugin)

## 4. Development Workflow & Commands
When executing commands via PowerShell in this environment, always ensure Java and Android paths are respected:
```powershell
$env:JAVA_HOME = "C:\Users\khans\.jdks\temurin-21"
$env:ANDROID_HOME = "C:\Users\khans\AppData\Local\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
```

To run Gradle tasks once wrapper is generated:
```powershell
.\gradlew.bat test                # Run unit tests
.\gradlew.bat assembleDebug       # Build debug APK
.\gradlew.bat lintDebug           # Run Android lint
```

## 5. Coding Standards & Conventions
- **Attendance State Model**:
  - Valid states: `Working Day`, `Office Attendance`, `Holiday`, `Leave`.
  - `Weekend` is derived automatically from the day of week.
  - **Do NOT introduce a separate WFH state**. A working day without office attendance is implicitly non-office.
- **Data Integrity Rules**:
  - Marking a date as Holiday or Leave strictly removes any existing Office Attendance on that date.
  - Deleting/clearing a Holiday or Leave restores the date to a normal Working Day.
  - Previous office attendance is never automatically restored upon deleting a holiday or leave.
- **Clean Architecture & Separation of Concerns**:
  - `data`: Preferences DataStore, local file serialization, repository implementations.
  - `domain`: Pure Kotlin business logic (`WfoCalculator`), date math, zero Android UI dependencies.
  - `ui`: Composable screens, ViewModels (`StateFlow`), theme definitions.
  - `widget`: Jetpack Glance composables, action receivers, update triggers (best-effort date rollover, zero exact-alarm permissions).
- **Date Handling**:
  - Exclusively use `java.time` (`LocalDate`, `YearMonth`, `DayOfWeek`).
  - Do not use legacy `java.util.Date` or `java.util.Calendar`.
- **State Management**:
  - Single source of truth via Kotlin `StateFlow`.
  - Atomic updates using Preferences DataStore.
  - Immutable state models (`data class`) for UI and widget state.

