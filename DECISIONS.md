# Architectural Decision Records (ADR) — WFO Tracker

## ADR-001: Single-Module (`:app`) Project Structure
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: The product is a self-contained, offline Android utility with a focused scope (WFO tracking, Compose UI, Glance widget, and local DataStore persistence).
- **Decision**: Use a lean single-module structure (`:app`) rather than a premature multi-module hierarchy (e.g. `:core`, `:feature:dashboard`, etc.).
- **Consequences**:
  - Eliminates multi-module build configuration overhead and inter-module dependency resolution complexity.
  - Significantly faster Gradle sync and build times.
  - Can easily be refactored into feature modules if the scope substantially expands in the future.

---

## ADR-002: Preferences DataStore & Structured JSON over SQLite / Room
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: The application tracks monthly attendance (at most 365 days a year), user settings (target %, weekend preferences), and manual holidays/leaves. The data volume across multiple years is less than 100 KB.
- **Decision**: Use `androidx.datastore:datastore-preferences` combined with Kotlinx Serialization for structured JSON payloads instead of Room/SQLite.
- **Consequences**:
  - Eliminates KSP (Kotlin Symbol Processing) and Room compiler overhead.
  - DataStore provides transactional, asynchronous, and thread-safe file writes with reactive `Flow` emissions.
  - Backup and restore logic becomes virtually 1:1 with stored schema, removing complex SQL migration scripts for this scale.

---

## ADR-003: Absolute Zero Network Footprint (No INTERNET Permission)
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: Users expect their workplace attendance, leave patterns, and office schedules to remain entirely confidential and private on their personal device.
- **Decision**: Do not declare `android.permission.INTERNET` in `AndroidManifest.xml`. Do not include any network clients (OkHttp, Retrofit, Ktor), remote logging, or analytics libraries.
- **Consequences**:
  - The OS strictly denies socket creation and network hardware access at the kernel level.
  - Users can audit permissions and confirm zero network access.
  - No network connectivity handling or offline/online synchronization state machines required.

---

## ADR-004: Jetpack Glance for App Widget Implementation
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: Modern Android home screen widgets historically used XML `RemoteViews`, which are verbose, error-prone, and lack standard declarative reactive patterns.
- **Decision**: Implement the widget using `androidx.glance:glance-appwidget` and `androidx.glance:glance-material3`.
- **Consequences**:
  - Consistent declarative Kotlin composable syntax across both main app and widget.
  - Direct integration with Glance state management and simple action handling (`ActionCallback`) for one-tap WFO marking.
  - Smooth Material 3 theming aligned with the main application UI.

---

## ADR-005: Kotlin 2.x with Unified Compose Compiler Plugin
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: Jetpack Compose compilation is integrated directly into the Kotlin compiler plugin starting from Kotlin 2.0.0.
- **Decision**: Adopt Kotlin 2.0.21 / 2.1.0 using `org.jetbrains.kotlin.plugin.compose`.
- **Consequences**:
  - Eliminates Compose compiler version mismatch with Kotlin compiler versions.
  - Delivers strong skipping mode and memoization optimizations out of the box.
  - Faster compilation times and clean compatibility with Gradle 8.11+/8.12.

---

## ADR-006: Storage Access Framework (SAF) for Backup and Restore
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: Local backup and restore must work without requesting broad storage permissions (`MANAGE_EXTERNAL_STORAGE` or `READ_EXTERNAL_STORAGE`), adhering to Android scoped storage policies.
- **Decision**: Use Android's standard Storage Access Framework via `ActivityResultContracts.CreateDocument` (export) and `ActivityResultContracts.OpenDocument` (import) with MIME type `application/json`.
- **Consequences**:
  - Zero runtime permissions needed for file operations.
  - User explicitly selects where to save or load their backup file (local storage, external SD card, or user-chosen provider).
  - Clean error handling with JSON schema validation upon import.



---

## ADR-007: Adoption of JDK 21 LTS over Bundled JBR 25
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: The bundled Android Studio runtime is OpenJDK 25.0.4. Running Gradle 8.12 and AGP 8.8.2 on JDK 25 fails with `Unsupported class file major version 69` (Java 25 bytecode) in ASM and `IllegalArgumentException: 25.0.4` in Kotlin compiler/Kotlin DSL parsing.
- **Decision**: Configure `org.gradle.java.home=C\:\\Users\\khans\\.jdks\\temurin-21` in `gradle.properties` to target Eclipse Temurin OpenJDK 21.0.13 LTS.
- **Consequences**:
  - Provides a 100% stable, fully supported LTS compilation and build environment for AGP 8.8.2, Gradle 8.12, and Kotlin 2.0.21.
  - Keeps configuration project-scoped without modifying system-wide `JAVA_HOME`.
  - Android Studio seamlessly detects user JDKs located in `$USERPROFILE/.jdks/`.

---

## ADR-008: Five-State Attendance Domain Model (No Explicit WFH State)
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: Return-to-Office compliance measures office attendance against eligible working days. Having a separate WFH state introduces ambiguity and duplicate state tracking.
- **Decision**: Define attendance with four explicit states (`Working Day`, `Office Attendance`, `Holiday`, `Leave`) and one automatically derived state (`Weekend`). A working day without recorded office attendance represents non-office working.
- **Consequences**:
  - Eliminates state synchronization anomalies between WFH and unlogged working days.
  - Enforces strict data integrity: marking a date as Holiday/Leave strictly clears any Office Attendance; removing the Holiday/Leave returns it cleanly to an eligible Working Day without resurrecting stale attendance.


---

## ADR-009: Agreed JSON Schema and Atomic Validation Pattern for SAF Backups
- **Date**: 2026-11-09
- **Status**: Accepted
- **Context**: Users require reliable offline backup and restore of their attendance history and settings across devices without requiring network or external accounts. A corrupted, tampered, or malformed backup file must never leave the application in a partially updated or invalid state.
- **Decision**: 
  1. Standardize on the canonical JSON schema:
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
  2. Implement a two-phase atomic import in `WfoBackupManager` and `WfoRepository`:
     - Phase A: Rigorous validation of JSON syntax, date formatting (`yyyy-MM-dd`), value ranges (`target_percent` in 1..100, `glass_opacity` in 0..100), override states (`holiday` / `leave`), and resolution of conflicts (overrides take precedence over office attendance).
     - Phase B: Single-transaction atomic replacement in `dataStore.edit { ... }`.
- **Consequences**:
  - Malformed, empty, or invalid backup files fail fast and return `Result.failure` without touching stored user data.
  - No broad storage permissions required; fully compliant with Android Storage Access Framework (SAF).
