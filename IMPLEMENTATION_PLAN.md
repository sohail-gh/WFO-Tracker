# WFO Tracker — Implementation Plan

This document details the step-by-step phased engineering roadmap for implementing the **WFO Tracker** Android application.

---

## Phase 1: Build System & Gradle Scaffolding
- **Goal**: Establish a robust, reproducible, and minimal single-module Gradle configuration.
- **Tasks**:
  1. Generate root `settings.gradle.kts` declaring plugin management and project name `:WFO-Tracker`.
  2. Generate root `build.gradle.kts` with Android Gradle Plugin (AGP `8.8.2` or `8.9.0`) and Kotlin (`2.0.21` / `2.1.0`) plugins.
  3. Generate `gradle/wrapper/gradle-wrapper.properties` (Gradle `8.11.1` / `8.12`).
  4. Generate `app/build.gradle.kts` with:
     - `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`.
     - Java/Kotlin compile target `JavaVersion.VERSION_17` (or `VERSION_21`).
     - Jetpack Compose enabled with `composeCompiler` plugin.
     - Minimal dependencies: Compose BOM, Material 3, Glance AppWidget, DataStore Preferences, Kotlinx Serialization JSON.
  5. Setup `app/src/main/AndroidManifest.xml` (explicitly ensuring **no** `android.permission.INTERNET`).
  6. Add root `.gitignore` tailored for Android/Gradle projects.
  7. Validate clean sync and build with `./gradlew assembleDebug`.

---

## Phase 2: Domain Layer & Core Calculation Engine
- **Goal**: Implement deterministic, testable business logic for office attendance calculations without Android dependencies.
- **Key Concepts**:
  - `WfoStatus`: Enum `[WFO, WFH, LEAVE, HOLIDAY, WEEKEND]`.
  - `DayRecord`: Date + status + optional note.
  - `MonthlyStats`:
    - Total calendar days in selected month.
    - Non-working days (weekends + designated holidays).
    - Working days (total days - non-working days - approved leaves).
    - Attended WFO days.
    - Required WFO days based on target percentage: $\lceil \text{workingDays} \times (\text{targetPercent} / 100.0) \rceil$.
    - Current WFO percentage: $(\text{attendedWfoDays} / \text{workingDays}) \times 100$.
    - Remaining WFO days needed to hit target.
    - Feasibility calculation (can the user still hit the target given the remaining working days in the month?).
- **Deliverables**:
  - `com.wfotracker.domain.model.*`
  - `com.wfotracker.domain.WfoCalculator.kt`
  - Unit tests covering standard months, leap years, edge cases (0% target, 100% target, high leaves, holidays).

---

## Phase 3: Data Layer & Persistence
- **Goal**: Resilient, offline storage using Preferences DataStore and local JSON formatting.
- **Components**:
  - `UserSettingsRepository`:
    - Monthly target percentage (Int, default 40).
    - Standard weekend definition (default: Saturday, Sunday).
  - `AttendanceRepository`:
    - Persist day-by-day records (`LocalDate -> WfoStatus`).
    - Query records by month, range, or single date.
    - Flow-based observable streams emitting updates to UI and widget.
  - `HolidaysRepository`:
    - Add/remove/list manual holidays (past & future).
    - Add/remove/list approved leaves (past & future).
  - `BackupRestoreRepository`:
    - Export complete dataset (settings, holidays, leaves, attendance history) to an unencrypted JSON payload via SAF `ActivityResultContracts.CreateDocument`.
    - Import and validate JSON payload via `ActivityResultContracts.OpenDocument` with schema version check.

---

## Phase 4: Jetpack Compose User Interface (Material 3)
- **Goal**: Modern, accessible, fluid UI following Material Design 3 guidelines.
- **Screens**:
  1. **Dashboard Screen**:
     - Circular progress gauge showing current WFO % vs Target %.
     - Status card: "On Track" / "Needs X more days" / "Target Achieved".
     - Quick-action toggle: Mark today as WFO / WFH / Leave / Holiday in one tap.
     - Monthly summary card: Working days, Holidays, Leaves, Office days.
  2. **Calendar Screen**:
     - Month-by-month grid selector.
     - Color-coded badges per date (Blue for WFO, Green for WFH, Orange for Leave, Purple for Holiday).
     - Tap date to open quick status bottom sheet to adjust or annotate.
  3. **Holidays & Leave Management Screen**:
     - Segregated tabs for "Holidays" and "Leaves".
     - Ability to add past, present, or future single dates or date ranges.
     - Swipe-to-delete or edit entries.
  4. **Settings & Backup Screen**:
     - Slider and number input for monthly target % (1% - 100%, default 40%).
     - Custom weekend selector (e.g. 5-day week, 6-day week).
     - "Export Data to JSON" button (SAF file picker).
     - "Import Data from JSON" button (SAF file picker with confirmation dialog).

---

## Phase 5: Jetpack Glance App Widget
- **Goal**: Glancable, interactive home screen widget that updates instantly.
- **Widget Specifications**:
  - Sizes supported: `2x2`, `4x2`, `4x3`.
  - Content:
    - Current month name.
    - Percentage achieved with visual progress bar.
    - "X / Y days completed" (e.g. "8/10 days WFO").
    - "Remaining needed: Z days".
    - Interactive quick-tap action: "Mark WFO Today" directly from widget without launching full app.
  - Receiver: `GlanceAppWidgetReceiver` with immediate state update upon action.

---

## Phase 6: Verification, Testing & Polish
- **Goal**: Ensure zero bugs, high responsiveness, and verify strict offline compliance.
- **Checks**:
  - Run lint to guarantee no `INTERNET` permission or forbidden network APIs.
  - Complete unit test suite for calculation engine and repositories.
  - Test backup and restore with full roundtrip data integrity verification.
  - Build clean release/debug APK.

