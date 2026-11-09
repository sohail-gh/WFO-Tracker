package com.wfotracker.data

import com.wfotracker.data.backup.WfoBackupManager
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class WfoBackupManagerTest {

    @Test
    fun `test export matches agreed JSON schema`() {
        val state = WfoAttendanceState(
            officeAttendanceDates = setOf(LocalDate.of(2026, 11, 2)),
            overrides = mapOf(
                LocalDate.of(2027, 1, 26) to DateOverride(
                    date = LocalDate.of(2027, 1, 26),
                    state = WfoDayState.HOLIDAY,
                    name = "Republic Day"
                )
            )
        )
        val settings = WfoSettings(targetPercentage = 40, glassOpacity = 25)

        val json = WfoBackupManager.exportToJson(state, settings)

        assertTrue(json.contains("\"target_percent\": 40"))
        assertTrue(json.contains("\"glass_opacity\": 25"))
        assertTrue(json.contains("\"2026-11-02\""))
        assertTrue(json.contains("\"2027-01-26\""))
        assertTrue(json.contains("\"state\": \"holiday\""))
        assertTrue(json.contains("\"name\": \"Republic Day\""))
    }

    @Test
    fun `test import exact agreed schema snippet from requirements`() {
        val snippet = """
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
        """.trimIndent()

        val result = WfoBackupManager.validateAndParse(snippet)
        assertTrue(result.isSuccess)

        val (state, settings) = result.getOrThrow()
        assertEquals(40, settings.targetPercentage)
        assertEquals(25, settings.glassOpacity)
        assertEquals(setOf(LocalDate.of(2026, 11, 2)), state.officeAttendanceDates)

        val override = state.overrides[LocalDate.of(2027, 1, 26)]
        assertEquals(WfoDayState.HOLIDAY, override?.state)
        assertEquals("Republic Day", override?.name)
    }

    @Test
    fun `test serialization roundtrip preserves complete state and settings`() {
        val originalState = WfoAttendanceState(
            officeAttendanceDates = setOf(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                LocalDate.of(2026, 11, 4)
            ),
            overrides = mapOf(
                LocalDate.of(2026, 11, 10) to DateOverride(
                    date = LocalDate.of(2026, 11, 10),
                    state = WfoDayState.HOLIDAY,
                    name = "Company Holiday"
                ),
                LocalDate.of(2026, 11, 11) to DateOverride(
                    date = LocalDate.of(2026, 11, 11),
                    state = WfoDayState.LEAVE,
                    name = "Personal Time Off"
                )
            )
        )
        val originalSettings = WfoSettings(targetPercentage = 45, glassOpacity = 30)

        val json = WfoBackupManager.exportToJson(originalState, originalSettings)
        val result = WfoBackupManager.validateAndParse(json)

        assertTrue(result.isSuccess)
        val (restoredState, restoredSettings) = result.getOrThrow()

        assertEquals(originalSettings, restoredSettings)
        assertEquals(originalState.officeAttendanceDates, restoredState.officeAttendanceDates)
        assertEquals(originalState.overrides, restoredState.overrides)
    }

    @Test
    fun `test missing optional fields use safe defaults`() {
        val minimalJson = """
        {
          "settings": {
            "target_percent": 50
          },
          "office": [],
          "overrides": {
            "2026-12-25": {
              "state": "holiday"
            }
          }
        }
        """.trimIndent()

        val result = WfoBackupManager.validateAndParse(minimalJson)
        assertTrue(result.isSuccess)

        val (state, settings) = result.getOrThrow()
        assertEquals(50, settings.targetPercentage)
        assertEquals(25, settings.glassOpacity) // Default glass opacity

        val christmas = state.overrides[LocalDate.of(2026, 12, 25)]
        assertEquals(WfoDayState.HOLIDAY, christmas?.state)
        assertNull(christmas?.name)
    }

    @Test
    fun `test malformed JSON fails validation`() {
        val malformed = "{ settings: not valid json ..."
        val result = WfoBackupManager.validateAndParse(malformed)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message!!.contains("Malformed JSON"))
    }

    @Test
    fun `test empty JSON fails validation`() {
        val empty = ""
        val result = WfoBackupManager.validateAndParse(empty)
        assertTrue(result.isFailure)
    }

    @Test
    fun `test invalid target percentage fails validation`() {
        val lowTarget = """
        {
          "settings": { "target_percent": 0, "glass_opacity": 25 },
          "office": []
        }
        """.trimIndent()
        assertTrue(WfoBackupManager.validateAndParse(lowTarget).isFailure)

        val highTarget = """
        {
          "settings": { "target_percent": 105, "glass_opacity": 25 },
          "office": []
        }
        """.trimIndent()
        assertTrue(WfoBackupManager.validateAndParse(highTarget).isFailure)
    }

    @Test
    fun `test invalid glass opacity fails validation`() {
        val lowOpacity = """
        {
          "settings": { "target_percent": 40, "glass_opacity": -1 },
          "office": []
        }
        """.trimIndent()
        assertTrue(WfoBackupManager.validateAndParse(lowOpacity).isFailure)

        val highOpacity = """
        {
          "settings": { "target_percent": 40, "glass_opacity": 101 },
          "office": []
        }
        """.trimIndent()
        assertTrue(WfoBackupManager.validateAndParse(highOpacity).isFailure)
    }

    @Test
    fun `test invalid date format fails validation`() {
        val invalidOfficeDate = """
        {
          "settings": { "target_percent": 40 },
          "office": ["not-a-valid-date"]
        }
        """.trimIndent()
        assertTrue(WfoBackupManager.validateAndParse(invalidOfficeDate).isFailure)

        val invalidOverrideDate = """
        {
          "settings": { "target_percent": 40 },
          "office": [],
          "overrides": {
            "2026-02-31": { "state": "holiday" }
          }
        }
        """.trimIndent()
        assertTrue(WfoBackupManager.validateAndParse(invalidOverrideDate).isFailure)
    }

    @Test
    fun `test invalid override state fails validation`() {
        val invalidState = """
        {
          "settings": { "target_percent": 40 },
          "office": [],
          "overrides": {
            "2026-11-10": { "state": "vacation" }
          }
        }
        """.trimIndent()
        val result = WfoBackupManager.validateAndParse(invalidState)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message!!.contains("Expected 'holiday' or 'leave'"))
    }

    @Test
    fun `test data integrity rule enforced when date is in both office and overrides`() {
        // If external JSON mistakenly claims 2026-11-04 is both an office attendance and a holiday,
        // the override takes priority and office attendance on that date is stripped.
        val conflictJson = """
        {
          "settings": { "target_percent": 40, "glass_opacity": 25 },
          "office": ["2026-11-04"],
          "overrides": {
            "2026-11-04": { "state": "holiday", "name": "Diwali" }
          }
        }
        """.trimIndent()

        val result = WfoBackupManager.validateAndParse(conflictJson)
        assertTrue(result.isSuccess)

        val (state, _) = result.getOrThrow()
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 11, 4)))
        assertFalse(state.officeAttendanceDates.contains(LocalDate.of(2026, 11, 4)))
    }

    @Test
    fun `test stream export and import roundtrip`() {
        val state = WfoAttendanceState(
            officeAttendanceDates = setOf(LocalDate.of(2026, 11, 2)),
            overrides = mapOf(
                LocalDate.of(2026, 11, 3) to DateOverride(
                    date = LocalDate.of(2026, 11, 3),
                    state = WfoDayState.LEAVE,
                    name = "Medical Leave"
                )
            )
        )
        val settings = WfoSettings(targetPercentage = 40, glassOpacity = 20)

        val outputStream = ByteArrayOutputStream()
        WfoBackupManager.exportToStream(outputStream, state, settings)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val importResult = WfoBackupManager.importFromStream(inputStream)

        assertTrue(importResult.isSuccess)
        val (restoredState, restoredSettings) = importResult.getOrThrow()

        assertEquals(settings, restoredSettings)
        assertEquals(state.officeAttendanceDates, restoredState.officeAttendanceDates)
        assertEquals(state.overrides, restoredState.overrides)
    }
}

