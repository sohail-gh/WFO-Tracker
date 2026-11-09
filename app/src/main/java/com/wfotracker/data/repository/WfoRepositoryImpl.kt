package com.wfotracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.wfotracker.data.backup.WfoBackupManager
import com.wfotracker.data.model.OverrideDetailDto
import com.wfotracker.data.preferences.WfoDataStoreKeys
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.LocalDate

class WfoRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : WfoRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override val settings: Flow<WfoSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val target = preferences[WfoDataStoreKeys.KEY_TARGET_PERCENT] ?: 40
            val glass = preferences[WfoDataStoreKeys.KEY_GLASS_OPACITY] ?: 25
            WfoSettings(
                targetPercentage = target.coerceIn(1, 100),
                glassOpacity = glass.coerceIn(0, 100)
            )
        }

    override val attendanceState: Flow<WfoAttendanceState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val officeDateStrings = preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] ?: emptySet()
            val overridesJson = preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}"

            val parsedOverrides = parseOverridesFromJson(overridesJson)
            val parsedOfficeDates = officeDateStrings.mapNotNull { iso ->
                try {
                    LocalDate.parse(iso)
                } catch (e: Exception) {
                    null
                }
            }.filterNot { parsedOverrides.containsKey(it) }.toSet()

            WfoAttendanceState(
                officeAttendanceDates = parsedOfficeDates,
                overrides = parsedOverrides
            )
        }

    override suspend fun getAttendanceState(): WfoAttendanceState = attendanceState.first()

    override suspend fun getSettings(): WfoSettings = settings.first()

    override suspend fun setTargetPercentage(percentage: Int) {
        require(percentage in 1..100) { "Target percentage must be between 1 and 100, got: $percentage" }
        dataStore.edit { preferences ->
            preferences[WfoDataStoreKeys.KEY_TARGET_PERCENT] = percentage
        }
    }

    override suspend fun setGlassOpacity(opacity: Int) {
        require(opacity in 0..100) { "Glass opacity must be between 0 and 100, got: $opacity" }
        dataStore.edit { preferences ->
            preferences[WfoDataStoreKeys.KEY_GLASS_OPACITY] = opacity
        }
    }

    override suspend fun updateSettings(settings: WfoSettings) {
        dataStore.edit { preferences ->
            preferences[WfoDataStoreKeys.KEY_TARGET_PERCENT] = settings.targetPercentage
            preferences[WfoDataStoreKeys.KEY_GLASS_OPACITY] = settings.glassOpacity
        }
    }

    override suspend fun markOfficeAttendance(date: LocalDate) {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        if (isWeekend) return

        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}")
            if (currentOverrides.containsKey(date)) return@edit

            val currentOffice = (preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] ?: emptySet()).toMutableSet()
            currentOffice.add(date.toString())
            preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] = currentOffice
        }
    }

    override suspend fun removeOfficeAttendance(date: LocalDate) {
        dataStore.edit { preferences ->
            val currentOffice = (preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] ?: emptySet()).toMutableSet()
            currentOffice.remove(date.toString())
            preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] = currentOffice
        }
    }

    override suspend fun toggleOfficeAttendance(date: LocalDate) {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        if (isWeekend) return

        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}")
            if (currentOverrides.containsKey(date)) return@edit

            val currentOffice = (preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] ?: emptySet()).toMutableSet()
            val dateStr = date.toString()
            if (currentOffice.contains(dateStr)) {
                currentOffice.remove(dateStr)
            } else {
                currentOffice.add(dateStr)
            }
            preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] = currentOffice
        }
    }

    override suspend fun markHoliday(date: LocalDate, name: String?) {
        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}").toMutableMap()
            currentOverrides[date] = DateOverride(date, WfoDayState.HOLIDAY, name)
            preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] = serializeOverridesToJson(currentOverrides)

            // Data integrity: Marking date as Holiday strictly removes any existing Office Attendance on that date.
            val currentOffice = (preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] ?: emptySet()).toMutableSet()
            currentOffice.remove(date.toString())
            preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] = currentOffice
        }
    }

    override suspend fun clearHoliday(date: LocalDate) {
        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}").toMutableMap()
            if (currentOverrides[date]?.state == WfoDayState.HOLIDAY) {
                currentOverrides.remove(date)
                preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] = serializeOverridesToJson(currentOverrides)
                // Data integrity: Previous office attendance is NEVER restored
            }
        }
    }

    override suspend fun markLeave(date: LocalDate, reason: String?) {
        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}").toMutableMap()
            currentOverrides[date] = DateOverride(date, WfoDayState.LEAVE, reason)
            preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] = serializeOverridesToJson(currentOverrides)

            // Data integrity: Marking date as Leave strictly removes any existing Office Attendance on that date.
            val currentOffice = (preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] ?: emptySet()).toMutableSet()
            currentOffice.remove(date.toString())
            preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] = currentOffice
        }
    }

    override suspend fun clearLeave(date: LocalDate) {
        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}").toMutableMap()
            if (currentOverrides[date]?.state == WfoDayState.LEAVE) {
                currentOverrides.remove(date)
                preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] = serializeOverridesToJson(currentOverrides)
                // Data integrity: Previous office attendance is NEVER restored
            }
        }
    }

    override suspend fun clearOverride(date: LocalDate) {
        dataStore.edit { preferences ->
            val currentOverrides = parseOverridesFromJson(preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] ?: "{}").toMutableMap()
            if (currentOverrides.containsKey(date)) {
                currentOverrides.remove(date)
                preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] = serializeOverridesToJson(currentOverrides)
            }
        }
    }

    override suspend fun exportBackup(outputStream: OutputStream): Result<Unit> {
        return runCatching {
            val state = getAttendanceState()
            val set = getSettings()
            WfoBackupManager.exportToStream(outputStream, state, set)
        }
    }

    override suspend fun importBackup(inputStream: InputStream): Result<Unit> {
        return runCatching {
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            importBackupFromJson(jsonString).getOrThrow()
        }
    }

    override suspend fun importBackupFromJson(jsonString: String): Result<Unit> {
        val validationResult = WfoBackupManager.validateAndParse(jsonString)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull()!!)
        }

        val (newState, newSettings) = validationResult.getOrThrow()

        // Atomic replacement inside a single DataStore transaction
        dataStore.edit { preferences ->
            preferences[WfoDataStoreKeys.KEY_TARGET_PERCENT] = newSettings.targetPercentage
            preferences[WfoDataStoreKeys.KEY_GLASS_OPACITY] = newSettings.glassOpacity
            preferences[WfoDataStoreKeys.KEY_OFFICE_DATES] = newState.officeAttendanceDates.map { it.toString() }.toSet()
            preferences[WfoDataStoreKeys.KEY_OVERRIDES_JSON] = serializeOverridesToJson(newState.overrides)
        }

        return Result.success(Unit)
    }

    private fun parseOverridesFromJson(jsonString: String): Map<LocalDate, DateOverride> {
        if (jsonString.isBlank() || jsonString == "{}") return emptyMap()
        return try {
            val rawMap = json.decodeFromString<Map<String, OverrideDetailDto>>(jsonString)
            rawMap.mapNotNull { (dateStr, detail) ->
                try {
                    val date = LocalDate.parse(dateStr)
                    val state = when (detail.state.lowercase()) {
                        "leave" -> WfoDayState.LEAVE
                        else -> WfoDayState.HOLIDAY
                    }
                    date to DateOverride(date, state, detail.name)
                } catch (e: Exception) {
                    null
                }
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeOverridesToJson(overrides: Map<LocalDate, DateOverride>): String {
        val dtoMap = overrides.map { (date, override) ->
            date.toString() to OverrideDetailDto(
                state = override.state.name.lowercase(),
                name = override.name
            )
        }.toMap()
        return json.encodeToString(dtoMap)
    }
}
