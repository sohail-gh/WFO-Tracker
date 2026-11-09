# WFO Tracker

[![Platform](https://img.shields.io/badge/Platform-Android_8.0%2B_(API_26%2B)-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Target SDK](https://img.shields.io/badge/Target_SDK-35_(Android_15)-0078D4)](https://developer.android.com/about/versions/15)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-BOM_2024.12.01-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Glance](https://img.shields.io/badge/Jetpack_Glance-1.1.1-34A853)](https://developer.android.com/jetpack/compose/glance)
[![Privacy](https://img.shields.io/badge/Network-100%25_Offline_(Zero_Permissions)-brightgreen)](https://github.com/sohail-gh/WFO-Tracker#privacy--offline-first-security)
[![Unit Tests](https://img.shields.io/badge/Unit_Tests-66%2F66_Passed-success)](https://github.com/sohail-gh/WFO-Tracker#developer-setup--build-instructions)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blue)](https://github.com/sohail-gh/WFO-Tracker/releases/tag/v1.0.0)

**WFO Tracker** is an ultra-lean, 100% offline native Android application and interactive home-screen widget engineered to track, visualize, and fulfill Return-to-Office (WFO) monthly compliance quotas (default: 40%).

Built strictly without network permissions, external telemetry, or cloud dependencies, WFO Tracker guarantees that employee attendance patterns, personal leave days, and corporate schedules remain strictly confidential on the user's device.

---

## Table of Contents

- [Overview & Purpose](#overview--purpose)
- [Key Features](#key-features)
- [Monthly WFO Calculation Engine](#monthly-wfo-calculation-engine)
  - [Target Percentage & Rounding](#target-percentage--rounding)
  - [Eligible Weekdays & Weekends](#eligible-weekdays--weekends)
  - [Holidays, Leaves, and Date Overrides](#holidays-leaves-and-date-overrides)
  - [Days Remaining & Pace Calculation](#days-remaining--pace-calculation)
  - [Out-of-Reach Alert](#out-of-reach-alert)
- [Calendar & Color Conventions](#calendar--color-conventions)
  - [Interactive Calendar](#interactive-calendar)
  - [Semantic Color Palette](#semantic-color-palette)
  - [Strict Data Integrity Rules](#strict-data-integrity-rules)
- [Native Home-Screen Widget](#native-home-screen-widget)
  - [Jetpack Glance Architecture](#jetpack-glance-architecture)
  - [Multi-Instance State Isolation](#multi-instance-state-isolation)
  - [Tap-to-App Navigation](#tap-to-app-navigation)
  - [Responsive Layout & Sizing](#responsive-layout--sizing)
  - [Zero-Alarm Midnight Rollover](#zero-alarm-midnight-rollover)
  - [Dynamic Glassmorphic Opacity](#dynamic-glassmorphic-opacity)
- [Settings & Customization](#settings--customization)
- [JSON Backup & Restore](#json-backup--restore)
  - [Storage Access Framework (SAF)](#storage-access-framework-saf)
  - [Canonical JSON Schema](#canonical-json-schema)
  - [Two-Phase Atomic Import](#two-phase-atomic-import)
- [Privacy & Offline-First Security](#privacy--offline-first-security)
- [Technology Stack & Architecture](#technology-stack--architecture)
- [Compatibility & SDK Support](#compatibility--sdk-support)
- [Installation Guide](#installation-guide)
- [Developer Setup & Build Instructions](#developer-setup--build-instructions)
  - [Prerequisites](#prerequisites)
  - [Building from Source](#building-from-source)
  - [Running Unit Tests](#running-unit-tests)
  - [Release Signing Configuration](#release-signing-configuration)
- [Project Structure](#project-structure)
- [Release Information & Download](#release-information--download)
- [Known Limitations](#known-limitations)
- [License](#license)

---

## Overview & Purpose

Many enterprise hybrid work policies mandate physical office attendance based on a monthly percentage (typically 40% or 50% of eligible working days). Employees frequently struggle with:
- Floating company holidays and personal leaves reducing the monthly denominator.
- Confusion over whether partial requirements round up or down.
- Lack of immediate visibility into remaining days required before month-end.
- Privacy concerns when using cloud-connected logging apps that transmit work patterns.

**WFO Tracker** solves this with a lightweight, mathematically exact, and totally private on-device tracker with a home-screen widget.

---

## Key Features

- **Real-Time Compliance Tracking**: View current office days logged, remaining days required, working days, and pace percentage.
- **Pure Domain Math Engine**: Ceiled integer arithmetic prevents floating-point rounding ambiguities.
- **Interactive Monthly Calendar Grid**: 7-column calendar (Monday to Sunday) with one-tap status toggling and detailed day-action dialogs.
- **Overrides Management**: Log public holidays and personal leaves individually or across multi-day date ranges.
- **Native Glance Home-Screen Widget**: Resizable, glassmorphic widget with full month calendar, progress metrics, and tap-to-app deep linking.
- **Independent Multi-Instance Widgets**: Place multiple widgets on different launcher pages; each widget can browse months independently without conflicts.
- **Instant Reactive Sync**: Edits in the app immediately refresh all home-screen widgets via DataStore flows.
- **Customizable Target & Glassmorphism**: Adjust target percentage (1–100%) and widget background opacity (0–100%) with live preview.
- **Offline JSON Backup & Restore**: Export and import complete attendance history and settings via Android's native file picker without storage permissions.
- **Zero Network Permissions**: Strictly free of internet permissions, crash reporters, analytics SDKs, and background telemetry.

---

## Monthly WFO Calculation Engine

The calculation engine (`com.wfotracker.domain.WfoCalculator`) is a pure Kotlin component with zero Android UI, Context, or system-clock dependencies, ensuring 100% deterministic, testable behavior.

### Target Percentage & Rounding

Most corporate policies require reaching or exceeding the target threshold. WFO Tracker calculates required office days using **integer arithmetic ceiling**:

$$\text{requiredOfficeDays} = \left\lfloor \frac{\text{workingDays} \times \text{targetPercentage} + 99}{100} \right\rfloor$$

This guarantees exact ceiling semantics without floating-point precision hazards:
- *Example 1*: 21 working days at 40% target $\rightarrow \lceil 8.4 \rceil = 9$ days required.
- *Example 2*: 20 working days at 40% target $\rightarrow \lceil 8.0 \rceil = 8$ days required.
- *Example 3*: 22 working days at 40% target $\rightarrow \lceil 8.8 \rceil = 9$ days required.

### Eligible Weekdays & Weekends

- **Working Days**: Monday through Friday are considered eligible working days by default.
- **Weekends**: Saturday and Sunday are automatically derived as non-working days.
- **No Double-Deduction**: If a public holiday falls on a weekend, it does not reduce the working day count twice.

### Holidays, Leaves, and Date Overrides

- **Holidays** and **Leaves** directly reduce the count of eligible working days in the month.
- Reducing working days automatically recalculates the required office days downward:
  - 21 working days (9 required) minus 2 holidays = 19 working days $\rightarrow \lceil 19 \times 0.40 \rceil = 8$ required office days.

### Days Remaining & Pace Calculation

- **Remaining Days Needed**:
  $$\text{remainingOfficeDays} = \max(0, \text{requiredOfficeDays} - \text{attendedDays})$$
- **Days Left in Month (`daysLeft`)**: Count of eligible working days from today (inclusive) through the end of the selected month that have **not** already been marked as attended.
- **Progress Percentage**:
  $$\text{progressPercentage} = \min\left(100.0, \frac{\text{attendedDays}}{\text{requiredOfficeDays}} \times 100\right)$$

### Out-of-Reach Alert

When $\text{remainingOfficeDays} > \text{daysLeft}$, the target is mathematically impossible to achieve in the remaining days of the month. WFO Tracker immediately flags this in both the app dashboard and the widget metrics bar with an **Out of reach** warning.

---

## Calendar & Color Conventions

### Interactive Calendar

The calendar screen displays a clean 7-column grid (Monday through Sunday) for the selected month.
- **Tapping a day** opens the `DayActionDialog` to:
  - Mark or unmark office attendance.
  - Mark single-date holidays or leaves (with custom labels/reasons).
  - Edit or clear existing overrides.
- **Month Navigation**: Jump backward, forward, or return to the current month with a single tap.

### Semantic Color Palette

| State | Light Theme | Dark Theme | Purpose |
| :--- | :--- | :--- | :--- |
| **Office Attendance** | Emerald Green (`#2E7D32`) | Light Green (`#81C784`) | Confirmed physical office day |
| **Public Holiday** | Royal Purple (`#7B1FA2`) | Soft Purple (`#CE93D8`) | Company/national holiday |
| **Personal Leave** | Deep Amber (`#E65100`) | Warm Amber (`#FFB74D`) | Vacation, sick, or personal time-off |
| **Weekend** | Dimmed Grey (`#9E9E9E`) | Subdued Grey (`#616161`) | Non-working Saturday / Sunday |
| **Today's Date** | Blue Accent Outline (`#1976D2`) | Light Blue Outline (`#90CAF9`) | Current calendar date marker |
| **Working Day** | Standard Surface | Standard Surface | Unattended working day (remote/WFH) |

> [!NOTE]
> **No Explicit WFH State**: By architectural design, WFO Tracker does not feature a separate "WFH" toggle. Any eligible working day without recorded office attendance, holiday, or leave represents remote working. This prevents duplicate state tracking and synchronization anomalies.

### Strict Data Integrity Rules

1. **Mutual Exclusivity**: Marking a date as **Holiday** or **Leave** automatically and atomically removes any existing **Office Attendance** on that date.
2. **Non-Restoration**: Removing or clearing a Holiday or Leave restores the date to a normal **Working Day**. It *never* resurrects previously cleared office attendance.
3. **Weekend Sanitization**: Office attendance cannot be recorded on weekends. Any weekend office entries in imported backups are sanitized automatically upon import.

---

## Native Home-Screen Widget

WFO Tracker includes a home-screen widget built with **Jetpack Glance 1.1.1**.

### Jetpack Glance Architecture

- Built with declarative Kotlin composables rendered into native Android `AppWidget` layouts.
- Automatically follows system dark/light styling with Glance theming.
- Reactive: DataStore emissions trigger `WfoWidgetUpdater.updateAll()`, providing immediate updates whenever attendance or settings change in the app.

### Multi-Instance State Isolation

Users can add multiple widget instances across different home-screen pages.
- Each widget instance retains its own displayed month (`widget_displayed_month`) via `PreferencesGlanceStateDefinition`.
- You can review last month on one widget while keeping the current month on another.
- All instances share identical underlying attendance data and calculations from `WfoRepository`.

### Tap-to-App Navigation

- **Tap Month Header**: Launches the application directly to the displayed month.
- **Tap Any Date Cell**: Launches the application and immediately pops open the `DayActionDialog` for that specific date via `MainActivity.EXTRA_SELECTED_DATE`.

### Responsive Layout & Sizing

The widget scales across home screen grid configurations:
- **Default / Recommended Size**: $4 \times 3$ grid cells.
- **Minimum Resize Size**: $3 \times 2$ / $3 \times 3$ cells ($180 \times 140$ dp).
- Dynamically scales padding, button sizes, font sizes (8 sp to 10 sp), and header formats ("Mon Tue..." vs "M T W T F S S") based on `LocalSize.current`.

### Zero-Alarm Midnight Rollover

To honor the strict zero-unnecessary-permissions policy, the widget avoids battery-draining exact alarm permissions (`SCHEDULE_EXACT_ALARM`). Instead, the date rollover (updating "Today" outline and days left) is achieved via:
1. System broadcasts: `ACTION_DATE_CHANGED`, `ACTION_TIME_SET`, and `ACTION_TIMEZONE_CHANGED` registered in `WfoWidgetReceiver`.
2. Periodic widget updates: `updatePeriodMillis="1800000"` (30-minute OS refresh interval).
3. Any user interaction with the widget or app instantly forces a fresh calculation.

### Dynamic Glassmorphic Opacity

The widget surface renders a frosted glass card with configurable opacity (0% to 100%). Users can adjust this in Settings to suit their home screen wallpaper.

---

## Settings & Customization

The Settings screen provides:
- **Monthly Target Percentage Slider**: Configurable from 1% to 100% (default: 40%). Updating the slider immediately updates all compliance targets across the dashboard and widgets.
- **Widget Glass Opacity Slider**: Configurable from 0% to 100% (default: 25%) with a real-time live preview swatch card.
- **Data Management**: One-click triggers for local JSON Backup and Restore.

---

## JSON Backup & Restore

WFO Tracker supports exporting and importing all data via standard JSON.

### Storage Access Framework (SAF)

- Uses Android's Storage Access Framework (`ActivityResultContracts.CreateDocument` and `OpenDocument`).
- **Zero Storage Permissions**: Does not require `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE`.
- You can save backups locally, to an SD card, or to any document provider available on your device.

### Canonical JSON Schema

```json
{
  "settings": {
    "target_percent": 40,
    "glass_opacity": 25
  },
  "office": [
    "2026-11-02",
    "2026-11-03",
    "2026-11-06"
  ],
  "overrides": {
    "2026-11-26": {
      "state": "holiday",
      "name": "Thanksgiving"
    },
    "2026-11-27": {
      "state": "leave",
      "name": "Personal Day"
    }
  }
}
```

### Two-Phase Atomic Import

1. **Validation Phase**: The backup manager validates JSON structure, date formats (`yyyy-MM-dd`), value ranges (`target_percent` $\in [1, 100]$, `glass_opacity` $\in [0, 100]$), and override states (`holiday` / `leave`). Conflicting office dates matching overrides are pruned, and weekend office dates are stripped.
2. **Atomic Write Phase**: If validation passes, data is committed in a single DataStore transaction (`dataStore.edit { ... }`). If the file is malformed, corrupted, or empty, the import fails safely without modifying existing records.

---

## Privacy & Offline-First Security

WFO Tracker is built with a zero-trust, privacy-first design:

- **No INTERNET Permission**: `android.permission.INTERNET` is omitted from `AndroidManifest.xml`. The OS prevents network socket creation at the kernel level.
- **Transitive Network Permissions Stripped**: Explicitly removes transitive permissions like `ACCESS_NETWORK_STATE` and `FOREGROUND_SERVICE` using `tools:node="remove"`.
- **Zero Third-Party SDKs**: No Google Analytics, Firebase, Sentry, ad networks, or remote services.
- **On-Device Storage**: Data is saved locally in private app storage via encrypted AndroidX Preferences DataStore.

### Android Manifest Audit

Verified permissions in the release artifact:
- `android.permission.WAKE_LOCK` (required by Glance / WorkManager)
- `android.permission.RECEIVE_BOOT_COMPLETED` (for widget restoration after reboot)
- `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (standard Android 14+ receiver safety)

---

## Technology Stack & Architecture

- **Language**: Kotlin `2.0.21`
- **UI Toolkit**: Jetpack Compose (BOM `2024.12.01`) + Material 3
- **Widget Engine**: Jetpack Glance `1.1.1` (`glance-appwidget`, `glance-material3`)
- **Persistence**: AndroidX Preferences DataStore `1.1.2`
- **Serialization**: Kotlinx Serialization JSON `1.7.3`
- **Build System**: Gradle `8.12` with Android Gradle Plugin `8.8.2`
- **Build JDK**: Eclipse Temurin OpenJDK `21.0.13` LTS
- **Architecture**: Clean Architecture with unidirectional data flow (UDF) and `StateFlow`:
  - `domain`: Pure Kotlin business logic and calculation engine (`WfoCalculator`).
  - `data`: DataStore repository, serialization models, and backup manager.
  - `ui`: Jetpack Compose screens, ViewModels, and Material 3 design system.
  - `widget`: Glance composables, action callbacks, and broadcast receivers.

---

## Compatibility & SDK Support

- **Minimum SDK**: Android 8.0 (API level 26, Oreo)
  - Full native `java.time` support (`LocalDate`, `YearMonth`, `DayOfWeek`) without desugaring overhead.
- **Target SDK**: Android 15 (API level 35, Vanilla Ice Cream)
- **Compile SDK**: Android 15 (API level 35)
- **Supported ABIs**: Universal APK supporting `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.

---

## Installation Guide

1. Download `app-release.apk` from the [GitHub Releases](https://github.com/sohail-gh/WFO-Tracker/releases/tag/v1.0.0) page.
2. Open your Android file manager or browser downloads.
3. Tap the downloaded APK file. If prompted, grant permission to "Install unknown apps" for that source.
4. Complete installation and open **WFO Tracker**.
5. Long-press your device's home screen, select **Widgets**, locate **WFO Tracker**, and place it on your home screen.

---

## Developer Setup & Build Instructions

### Prerequisites

- **Java Development Kit**: Eclipse Temurin OpenJDK 21 LTS (`21.0.13` or newer).
- **Android SDK**: Build-Tools `35.0.0`, SDK Platform `android-35`.
- **Git**

### Building from Source

```bash
# Clone the repository
git clone https://github.com/sohail-gh/WFO-Tracker.git
cd WFO-Tracker

# Set environment variables (example for PowerShell)
$env:JAVA_HOME = "path/to/jdk-21"
$env:ANDROID_HOME = "path/to/android-sdk"
$env:Path = "$env:JAVA_HOME/bin;$env:ANDROID_HOME/platform-tools;$env:Path"

# Assemble debug APK
./gradlew assembleDebug
```

The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

### Running Unit Tests

Execute the full suite of 66 unit tests:

```bash
./gradlew testDebugUnitTest
```

All 66 tests across 5 test suites (`WfoCalculatorTest`, `WfoRepositoryTest`, `WfoBackupManagerTest`, `MainViewModelTest`, `WfoWidgetLogicTest`) run without Android device emulators.

### Release Signing Configuration

Release signing is configured project-locally via environment variables or an untracked `keystore.properties` file:

```properties
# keystore.properties (git-ignored)
release.storeFile=/path/to/release.jks
release.storePassword=your_keystore_password
release.keyAlias=your_key_alias
release.keyPassword=your_key_password
```

Alternatively, set the environment variables `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.

To assemble the release APK:

```bash
./gradlew assembleRelease
```

---

## Project Structure

```text
WFO-Tracker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/wfotracker/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── WfoApplication.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── backup/WfoBackupManager.kt
│   │   │   │   │   ├── model/BackupJsonDto.kt
│   │   │   │   │   ├── preferences/WfoDataStoreKeys.kt
│   │   │   │   │   └── repository/WfoRepository.kt / WfoRepositoryImpl.kt
│   │   │   │   ├── domain/
│   │   │   │   │   ├── WfoCalculator.kt
│   │   │   │   │   └── model/
│   │   │   │   │       ├── DateOverride.kt
│   │   │   │   │       ├── MonthlyWfoSummary.kt
│   │   │   │   │       ├── WfoAttendanceState.kt
│   │   │   │   │       ├── WfoDayState.kt
│   │   │   │   │       └── WfoSettings.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   ├── components/ (DayCell, DayActionDialog, Gauge, Headers)
│   │   │   │   │   ├── navigation/WfoNavScreen.kt
│   │   │   │   │   ├── screens/ (Dashboard, Calendar, Overrides, Settings)
│   │   │   │   │   └── theme/ (Color, Theme)
│   │   │   │   └── widget/
│   │   │   │       ├── NavigateMonthAction.kt
│   │   │   │       ├── WfoGlanceWidget.kt
│   │   │   │       ├── WfoWidgetKeys.kt
│   │   │   │       ├── WfoWidgetReceiver.kt
│   │   │   │       └── WfoWidgetUpdater.kt
│   │   │   └── res/ (drawable, mipmap, values, xml/wfo_widget_info.xml)
│   │   └── test/java/com/wfotracker/ (66 Unit Tests)
│   └── build.gradle.kts
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Release Information & Download

### v1.0.0 Production Release

- **GitHub Release**: [`v1.0.0`](https://github.com/sohail-gh/WFO-Tracker/releases/tag/v1.0.0)
- **Direct Download**: [Download app-release.apk](https://github.com/sohail-gh/WFO-Tracker/releases/download/v1.0.0/app-release.apk)

| Attribute | Specification |
| :--- | :--- |
| **Filename** | `app-release.apk` |
| **Package Name** | `com.wfotracker` |
| **Version Name** | `1.0.0` |
| **Version Code** | `1` |
| **File Size** | `12,949,437 bytes` (~12.35 MB) |
| **Min / Target SDK** | `26` (Android 8.0) / `35` (Android 15) |
| **Signature Scheme** | APK Signature Scheme v2 (RSA 2048-bit) |
| **SHA-256 Checksum** | `ccc20ca1daa2dae08e9519eeb37bad1ec1e54f4536b98de557e0798f19562ab7` |
| **Signer Certificate SHA-256** | `1a00c19ed3386c2471bf6d28070bba4ed526ed6186d025f4115435799f836d48` |

#### Verifying APK Authenticity

On Windows (PowerShell):
```powershell
Get-FileHash -Algorithm SHA256 path\to\app-release.apk
```

On Linux / macOS:
```bash
sha256sum path/to/app-release.apk
```

Expected output:
```text
ccc20ca1daa2dae08e9519eeb37bad1ec1e54f4536b98de557e0798f19562ab7
```

---

## Known Limitations

- **Android 8.0+ Required**: Devices running Android 7.1 or older are not supported due to the requirement for native `java.time` APIs and modern Jetpack Compose.
- **Standard Working Week**: Office attendance calculations are modeled around standard business working days (Monday–Friday). Weekend office shifts are excluded by design.
- **Best-Effort Midnight Rollover**: Without battery-draining exact alarm permissions, the home-screen widget's midnight transition relies on Android OS broadcasts (`ACTION_DATE_CHANGED`) and 30-minute provider updates. Tapping the widget or opening the app guarantees an instant refresh.
- **Single Schedule Profile**: The application tracks one monthly percentage target at a time.

---

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.

