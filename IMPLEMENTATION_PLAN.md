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
- **Status**: Completed & Verified
- **Goal**: Resilient, offline storage using Preferences DataStore and local JSON formatting.
- **Components**:
  - `WfoSettings`:
    - Monthly target percentage (Int, default 40, valid 1..100).
    - Glass opacity percentage for widget (Int, default 25, valid 0..100).
  - `WfoRepository`:
    - Single source of truth backed by Preferences DataStore.
    - Persist day-by-day records (`LocalDate -> WfoDayState`).
    - Flow-based observable streams emitting updates to UI and widget.
    - Atomic data integrity enforcement: marking holiday/leave immediately strips office attendance; clearing holiday/leave restores a normal working day without reviving stale office attendance.
  - `WfoBackupManager`:
    - Agreed JSON schema export and import:
      ```json
      {
        "settings": {
          "target_percent": 40,
          "glass_opacity": 25
        },
        "office": ["2026-11-02"],
        "overrides": {
          "2027-01-26": {
            "state": "holiday",
            "name": "Republic Day"
          }
        }
      }
      ```
    - Complete pre-import validation (dates, percentages, opacity, states).
    - Atomic DataStore import ensuring malformed or invalid files never partially overwrite state.

---

## Phase 4: Jetpack Compose User Interface (Material 3)
- **Status**: Completed & Verified
- **Goal**: Modern, accessible, fluid UI following Material Design 3 guidelines.
- **Screens**:
  1. **Dashboard Screen**:
     - Circular progress gauge showing current WFO % vs Target %.
     - Status card: "On Track" / "Needs X more days" / "Target Achieved" / "Out of Reach".
     - Quick-action toggle: Mark today as WFO / Holiday / Leave in one tap.
     - Monthly breakdown metrics: Working days, Attended days, Required days, Days left.
  2. **Calendar Screen**:
     - Month-by-month grid selector with weekday headers (`Mon..Sun`).
     - Color-coded badges per date (Blue for Office, Purple for Holiday, Amber for Leave, Grey for Weekend).
     - Tap date to open quick status dialog to toggle office, mark holiday with custom name, mark leave with custom reason, or restore working day.
     - Monthly progress banner and color legend.
  3. **Holidays & Leave Management Screen**:
     - Month filter (`This Month` vs `All Overrides`).
     - Add single date or multi-day date range with Material 3 DatePickers.
     - List cards with edit and delete confirmation dialogs.
  4. **Settings & Backup Screen**:
     - Sliders for Monthly Target % (1% - 100%, default 40%) and Widget Glass Opacity (0% - 100%, default 25%).
     - Real-time glass opacity preview swatch.
     - "Export JSON Backup" and "Restore JSON Backup" buttons using Android Storage Access Framework (SAF) with pre-restore confirmation dialog.
     - 100% offline & privacy verification badge.

---

## Phase 5: Jetpack Glance App Widget
- **Status**: Completed & Verified
- **Goal**: Glancable, interactive home screen widget that renders a full monthly calendar and updates instantly.
- **Implemented Components**:
  - `WfoGlanceWidget`: Genuine Jetpack Glance AppWidget (`androidx.glance:glance-appwidget:1.1.1` and `androidx.glance:glance-material3:1.1.1`).
  - **7-Column Calendar Grid**:
    - Complete month grid with `Mon..Sun` / `M..S` weekday headers.
    - Strict semantic color matching: Office (Green), Holiday (Purple), Leave (Amber), Today (Blue outline), Weekend (Dimmed).
    - Date cell tap handler opening `MainActivity` with `EXTRA_SELECTED_DATE` to deep-link to the exact month and date action dialog.
  - **Metrics & Indicators**:
    - Attended / required days counter (e.g. `8/10 Days (80%)`).
    - Remaining needed days or target achieved status.
    - Linear progress indicator matching compliance theme colors.
    - Real-time target out-of-reach warning.
  - **Multi-Instance Support**:
    - Independent per-widget displayed month stored in instance Preferences (`PreferencesGlanceStateDefinition`).
    - Interactive Previous (`◀`) and Next (`▶`) month navigation via `NavigateMonthAction : ActionCallback`.
    - Tap on month title to immediately reset to the current month.
    - Shared attendance records and configuration from `WfoRepository`.
  - **Glassmorphic Opacity**:
    - Live background alpha and frosted sheen border derived from user's `settings.glassOpacity` (0-100%).
  - **Resizing Adaptability**:
    - `SizeMode.Exact` supporting minimum 3x3 (~180x180dp) through standard 4x3 (~260x200dp) and larger layouts.
    - Dynamic breakpoint scaling for weekday headers (`M..S` vs `Mon..Sun`), month title formatting (`MMM yyyy` vs `MMMM yyyy`), and cell font sizing.
  - **Reactive Refresh & Date Rollover**:
    - `WfoWidgetUpdater` broadcasts immediate widget updates on any DataStore attendance or settings change.
    - Zero exact-alarm permissions: best-effort midnight rollover handled via `ACTION_DATE_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_TIME_SET`, and periodic widget provider updates.
  - **Zero Network Footprint**: Strict offline adherence with zero network permissions.

---

## Phase 6: Verification, Testing & Polish
- **Goal**: Ensure zero bugs, high responsiveness, and verify strict offline compliance.
- **Checks**:
  - Run lint to guarantee no `INTERNET` permission or forbidden network APIs.
  - Complete unit test suite for calculation engine and repositories.
  - Test backup and restore with full roundtrip data integrity verification.
  - Build clean release/debug APK.

