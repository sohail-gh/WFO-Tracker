package com.wfotracker.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.wfotracker.data.repository.WfoRepositoryImpl
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class WfoRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: WfoRepositoryImpl

    @Before
    fun setUp() {
        val testFile = tmpFolder.newFile("test_wfo.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = WfoRepositoryImpl(testDataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `test default settings`() = runBlocking {
        val settings = repository.getSettings()
        assertEquals(40, settings.targetPercentage)
        assertEquals(25, settings.glassOpacity)
    }

    @Test
    fun `test update settings`() = runBlocking {
        repository.setTargetPercentage(50)
        repository.setGlassOpacity(15)

        val updated = repository.getSettings()
        assertEquals(50, updated.targetPercentage)
        assertEquals(15, updated.glassOpacity)

        repository.updateSettings(WfoSettings(targetPercentage = 60, glassOpacity = 80))
        val updated2 = repository.getSettings()
        assertEquals(60, updated2.targetPercentage)
        assertEquals(80, updated2.glassOpacity)
    }

    @Test
    fun `test mark office and toggle office attendance`() = runBlocking {
        val workDay = LocalDate.of(2026, 11, 2) // Monday

        repository.markOfficeAttendance(workDay)
        var state = repository.getAttendanceState()
        assertTrue(state.officeAttendanceDates.contains(workDay))

        // Toggle removes it
        repository.toggleOfficeAttendance(workDay)
        state = repository.getAttendanceState()
        assertFalse(state.officeAttendanceDates.contains(workDay))

        // Toggle marks it again
        repository.toggleOfficeAttendance(workDay)
        state = repository.getAttendanceState()
        assertTrue(state.officeAttendanceDates.contains(workDay))

        // Remove explicitly
        repository.removeOfficeAttendance(workDay)
        state = repository.getAttendanceState()
        assertFalse(state.officeAttendanceDates.contains(workDay))
    }

    @Test
    fun `test toggle office on weekend does not mark attendance`() = runBlocking {
        val saturday = LocalDate.of(2026, 11, 7)
        repository.toggleOfficeAttendance(saturday)
        val state = repository.getAttendanceState()
        assertFalse(state.officeAttendanceDates.contains(saturday))
    }

    @Test
    fun `test marking holiday atomically purges office attendance`() = runBlocking {
        val workDay = LocalDate.of(2026, 11, 3) // Tuesday
        repository.markOfficeAttendance(workDay)
        assertTrue(repository.getAttendanceState().officeAttendanceDates.contains(workDay))

        // Rule 13: Marking holiday must atomically clear office attendance
        repository.markHoliday(workDay, "Local Holiday")
        val state = repository.getAttendanceState()
        assertFalse(state.officeAttendanceDates.contains(workDay))
        assertEquals(WfoDayState.HOLIDAY, state.overrides[workDay]?.state)
        assertEquals("Local Holiday", state.overrides[workDay]?.name)
    }

    @Test
    fun `test clearing holiday never restores previous office attendance`() = runBlocking {
        val workDay = LocalDate.of(2026, 11, 3)
        repository.markOfficeAttendance(workDay)
        repository.markHoliday(workDay, "Local Holiday")

        // Rule 14: Clearing holiday restores normal working day, never previous office attendance
        repository.clearHoliday(workDay)
        val state = repository.getAttendanceState()
        assertFalse(state.overrides.containsKey(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))
        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))
    }

    @Test
    fun `test marking leave atomically purges office attendance and clearing does not restore`() = runBlocking {
        val workDay = LocalDate.of(2026, 11, 4) // Wednesday
        repository.markOfficeAttendance(workDay)
        assertTrue(repository.getAttendanceState().officeAttendanceDates.contains(workDay))

        // Mark leave
        repository.markLeave(workDay, "Sick Leave")
        var state = repository.getAttendanceState()
        assertFalse(state.officeAttendanceDates.contains(workDay))
        assertEquals(WfoDayState.LEAVE, state.overrides[workDay]?.state)

        // Clear leave
        repository.clearLeave(workDay)
        state = repository.getAttendanceState()
        assertFalse(state.overrides.containsKey(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))
        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))
    }

    @Test
    fun `test atomic backup import replaces data cleanly`() = runBlocking {
        // Initial state
        repository.setTargetPercentage(40)
        repository.markOfficeAttendance(LocalDate.of(2026, 11, 2))

        val validBackupJson = """
        {
          "settings": {
            "target_percent": 60,
            "glass_opacity": 40
          },
          "office": ["2026-11-05", "2026-11-06"],
          "overrides": {
            "2026-11-12": {
              "state": "holiday",
              "name": "State Holiday"
            }
          }
        }
        """.trimIndent()

        val importResult = repository.importBackupFromJson(validBackupJson)
        assertTrue(importResult.isSuccess)

        val updatedSettings = repository.getSettings()
        assertEquals(60, updatedSettings.targetPercentage)
        assertEquals(40, updatedSettings.glassOpacity)

        val updatedState = repository.getAttendanceState()
        assertEquals(
            setOf(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 6)),
            updatedState.officeAttendanceDates
        )
        assertTrue(updatedState.overrides.containsKey(LocalDate.of(2026, 11, 12)))
    }

    @Test
    fun `test atomic backup import failure does not alter existing data`() = runBlocking {
        // Set existing known state
        repository.setTargetPercentage(45)
        repository.setGlassOpacity(35)
        val initialOfficeDate = LocalDate.of(2026, 11, 2)
        repository.markOfficeAttendance(initialOfficeDate)

        val malformedBackupJson = "{ settings: invalid json ..."

        val importResult = repository.importBackupFromJson(malformedBackupJson)
        assertTrue(importResult.isFailure)

        // Verify existing state was completely untouched (atomic rollback / non-destructive)
        val settings = repository.getSettings()
        assertEquals(45, settings.targetPercentage)
        assertEquals(35, settings.glassOpacity)

        val state = repository.getAttendanceState()
        assertTrue(state.officeAttendanceDates.contains(initialOfficeDate))
    }

    @Test
    fun `test markHolidayRange atomically marks range and clears office attendance`() = runBlocking {
        val start = LocalDate.of(2026, 12, 23)
        val middle = LocalDate.of(2026, 12, 24)
        val end = LocalDate.of(2026, 12, 25)

        repository.markOfficeAttendance(middle)
        repository.markHolidayRange(start, end, "Christmas Holidays")

        val state = repository.getAttendanceState()
        assertEquals(3, state.overrides.size)
        assertEquals(WfoDayState.HOLIDAY, state.overrides[start]?.state)
        assertEquals(WfoDayState.HOLIDAY, state.overrides[middle]?.state)
        assertEquals(WfoDayState.HOLIDAY, state.overrides[end]?.state)
        assertEquals("Christmas Holidays", state.overrides[middle]?.name)
        assertFalse("Office attendance must be cleared", state.officeAttendanceDates.contains(middle))
    }

    @Test
    fun `test markLeaveRange atomically marks range and clears office attendance`() = runBlocking {
        val start = LocalDate.of(2026, 8, 3)
        val middle = LocalDate.of(2026, 8, 4)
        val end = LocalDate.of(2026, 8, 5)

        repository.markOfficeAttendance(middle)
        repository.markLeaveRange(start, end, "Annual Leave")

        val state = repository.getAttendanceState()
        assertEquals(3, state.overrides.size)
        assertEquals(WfoDayState.LEAVE, state.overrides[start]?.state)
        assertEquals(WfoDayState.LEAVE, state.overrides[middle]?.state)
        assertEquals(WfoDayState.LEAVE, state.overrides[end]?.state)
        assertEquals("Annual Leave", state.overrides[middle]?.name)
        assertFalse("Office attendance must be cleared", state.officeAttendanceDates.contains(middle))
    }
}
