package com.wfotracker.data.backup

import com.wfotracker.data.model.BackupJsonDto
import com.wfotracker.data.model.BackupSettingsDto
import com.wfotracker.data.model.OverrideDetailDto
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Pure Kotlin backup manager providing serialization, deserialization,
 * validation, and streaming I/O for WFO Tracker data using standard JSON.
 */
object WfoBackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Serializes domain state to a formatted JSON string matching the agreed schema.
     */
    fun exportToJson(state: WfoAttendanceState, settings: WfoSettings): String {
        val sortedOfficeDates = state.officeAttendanceDates
            .map { it.toString() }
            .sorted()

        val sortedOverrides = state.overrides.toSortedMap()
            .map { (date, override) ->
                date.toString() to OverrideDetailDto(
                    state = override.state.name.lowercase(),
                    name = override.name
                )
            }.toMap()

        val payload = BackupJsonDto(
            settings = BackupSettingsDto(
                targetPercent = settings.targetPercentage,
                glassOpacity = settings.glassOpacity
            ),
            office = sortedOfficeDates,
            overrides = sortedOverrides
        )

        return json.encodeToString(BackupJsonDto.serializer(), payload)
    }

    /**
     * Exports the domain state to an [OutputStream] (used with SAF file creation).
     */
    fun exportToStream(outputStream: OutputStream, state: WfoAttendanceState, settings: WfoSettings) {
        val jsonString = exportToJson(state, settings)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(jsonString)
            writer.flush()
        }
    }

    /**
     * Parses and rigorously validates a JSON string.
     * Returns [Result.success] with sanitized domain state only if all validations pass,
     * otherwise returns [Result.failure] with details of the validation error.
     */
    fun validateAndParse(jsonString: String): Result<Pair<WfoAttendanceState, WfoSettings>> {
        return runCatching {
            require(jsonString.isNotBlank()) {
                "Backup file is empty"
            }

            val payload = try {
                json.decodeFromString(BackupJsonDto.serializer(), jsonString)
            } catch (e: Exception) {
                throw IllegalArgumentException("Malformed JSON payload: ${e.message}", e)
            }

            // 1. Validate settings
            val targetPercent = payload.settings.targetPercent
            require(targetPercent in 1..100) {
                "Invalid target_percent ($targetPercent). Must be between 1 and 100."
            }

            val glassOpacity = payload.settings.glassOpacity
            require(glassOpacity in 0..100) {
                "Invalid glass_opacity ($glassOpacity). Must be between 0 and 100."
            }

            val settings = WfoSettings(
                targetPercentage = targetPercent,
                glassOpacity = glassOpacity
            )

            // 2. Validate and parse overrides
            val parsedOverrides = mutableMapOf<LocalDate, DateOverride>()
            for ((dateStr, detail) in payload.overrides) {
                val parsedDate = try {
                    LocalDate.parse(dateStr)
                } catch (e: DateTimeParseException) {
                    throw IllegalArgumentException("Invalid date format in overrides: '$dateStr'", e)
                }

                val state = when (detail.state.lowercase().trim()) {
                    "holiday" -> WfoDayState.HOLIDAY
                    "leave" -> WfoDayState.LEAVE
                    else -> throw IllegalArgumentException(
                        "Invalid override state '${detail.state}' for date $dateStr. Expected 'holiday' or 'leave'."
                    )
                }

                parsedOverrides[parsedDate] = DateOverride(
                    date = parsedDate,
                    state = state,
                    name = detail.name?.takeIf { it.isNotBlank() }
                )
            }

            // 3. Validate and parse office attendance dates
            val parsedOfficeDates = mutableSetOf<LocalDate>()
            for (dateStr in payload.office) {
                val parsedDate = try {
                    LocalDate.parse(dateStr)
                } catch (e: DateTimeParseException) {
                    throw IllegalArgumentException("Invalid date format in office list: '$dateStr'", e)
                }
                parsedOfficeDates.add(parsedDate)
            }

            // 4. Enforce Data Integrity:
            // "Marking a date as holiday or leave must remove its office attendance."
            // If the backup file erroneously contained both for the same date, override wins.
            val sanitizedOfficeDates = parsedOfficeDates.filterNot { parsedOverrides.containsKey(it) }.toSet()

            val state = WfoAttendanceState(
                officeAttendanceDates = sanitizedOfficeDates,
                overrides = parsedOverrides
            )

            state to settings
        }
    }

    /**
     * Reads and validates backup data from an [InputStream] (used with SAF file open).
     */
    fun importFromStream(inputStream: InputStream): Result<Pair<WfoAttendanceState, WfoSettings>> {
        return runCatching {
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            validateAndParse(jsonString).getOrThrow()
        }
    }
}

