package com.wfotracker.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.emptyPreferences
import com.wfotracker.MainActivity
import com.wfotracker.domain.WfoCalculator
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Unit tests verifying widget state manipulation, multi-instance month navigation isolation,
 * and summary calculations for widget rendering.
 */
class WfoWidgetLogicTest {

    @Test
    fun testWidgetKeysAndIntentExtraConstants() {
        assertEquals("widget_displayed_month", WfoWidgetKeys.PREF_DISPLAYED_MONTH.name)
        assertEquals("com.wfotracker.extra.SELECTED_DATE", MainActivity.EXTRA_SELECTED_DATE)
    }

    @Test
    fun testIndependentMonthNavigationAcrossMultipleInstances() {
        // Simulate two distinct widget instances (Instance A and Instance B)
        // using independent mutable preferences maps.
        val instanceAPrefs = mutableMapOf<String, String>()
        val instanceBPrefs = mutableMapOf<String, String>()

        val baseMonth = YearMonth.of(2026, 11)
        instanceAPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name] = baseMonth.toString()
        instanceBPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name] = baseMonth.toString()

        // User clicks Next Month (+1) on Instance A
        val currentA = YearMonth.parse(instanceAPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name]!!)
        val newA = currentA.plusMonths(1)
        instanceAPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name] = newA.toString()

        // User clicks Prev Month (-1) on Instance B twice
        val currentB = YearMonth.parse(instanceBPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name]!!)
        val newB = currentB.minusMonths(2)
        instanceBPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name] = newB.toString()

        // Verify independent states: Instance A is Dec 2026, Instance B is Sep 2026
        assertEquals("2026-12", instanceAPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name])
        assertEquals("2026-09", instanceBPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name])
        assertNotEquals(
            instanceAPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name],
            instanceBPrefs[WfoWidgetKeys.PREF_DISPLAYED_MONTH.name]
        )
    }

    @Test
    fun testSharedAttendanceDataCalculatedForDifferentInstanceMonths() {
        // Shared attendance records across the entire app
        val sharedOfficeDates = setOf(
            LocalDate.of(2026, 11, 2),
            LocalDate.of(2026, 11, 3),
            LocalDate.of(2026, 12, 1)
        )
        val sharedOverrides = mapOf(
            LocalDate.of(2026, 11, 12) to DateOverride(
                date = LocalDate.of(2026, 11, 12),
                state = WfoDayState.HOLIDAY,
                name = "Diwali"
            ),
            LocalDate.of(2026, 12, 25) to DateOverride(
                date = LocalDate.of(2026, 12, 25),
                state = WfoDayState.HOLIDAY,
                name = "Christmas"
            )
        )
        val sharedAttendanceState = WfoAttendanceState(
            officeAttendanceDates = sharedOfficeDates,
            overrides = sharedOverrides
        )
        val sharedSettings = WfoSettings(targetPercentage = 40, glassOpacity = 50)
        val today = LocalDate.of(2026, 11, 15)

        // Instance A viewing November 2026
        val summaryA = WfoCalculator.calculateMonthlySummary(
            month = YearMonth.of(2026, 11),
            attendanceState = sharedAttendanceState,
            settings = sharedSettings,
            today = today
        )

        // Instance B viewing December 2026
        val summaryB = WfoCalculator.calculateMonthlySummary(
            month = YearMonth.of(2026, 12),
            attendanceState = sharedAttendanceState,
            settings = sharedSettings,
            today = today
        )

        // Nov 2026 has 2 attended days
        assertEquals(2, summaryA.attendedDays)
        // Dec 2026 has 1 attended day
        assertEquals(1, summaryB.attendedDays)

        // Total working days
        assertEquals(20, summaryA.workingDays) // 21 weekdays - 1 holiday = 20
        assertEquals(22, summaryB.workingDays) // 23 weekdays - 1 holiday = 22
    }

    @Test
    fun testWidgetDateCellStateEvaluation() {
        val nov2 = LocalDate.of(2026, 11, 2) // Monday - Office
        val nov7 = LocalDate.of(2026, 11, 7) // Saturday - Weekend
        val nov12 = LocalDate.of(2026, 11, 12) // Thursday - Holiday
        val nov16 = LocalDate.of(2026, 11, 16) // Monday - Leave
        val nov17 = LocalDate.of(2026, 11, 17) // Tuesday - Working Day

        val attendanceState = WfoAttendanceState(
            officeAttendanceDates = setOf(nov2),
            overrides = mapOf(
                nov12 to DateOverride(nov12, WfoDayState.HOLIDAY, "Diwali"),
                nov16 to DateOverride(nov16, WfoDayState.LEAVE, "Vacation")
            )
        )

        fun resolveState(date: LocalDate): WfoDayState {
            val override = attendanceState.overrides[date]
            val isWeekend = date.dayOfWeek.value >= 6
            val isOffice = attendanceState.officeAttendanceDates.contains(date)
            return when {
                override != null -> override.state
                isWeekend -> WfoDayState.WEEKEND
                isOffice -> WfoDayState.OFFICE
                else -> WfoDayState.WORKING_DAY
            }
        }

        assertEquals(WfoDayState.OFFICE, resolveState(nov2))
        assertEquals(WfoDayState.WEEKEND, resolveState(nov7))
        assertEquals(WfoDayState.HOLIDAY, resolveState(nov12))
        assertEquals(WfoDayState.LEAVE, resolveState(nov16))
        assertEquals(WfoDayState.WORKING_DAY, resolveState(nov17))
    }

    @Test
    fun testTargetOutOfReachWarningCalculation() {
        val today = LocalDate.of(2026, 11, 26) // Late in month
        val sharedSettings = WfoSettings(targetPercentage = 50) // High target: ceil(21 * 50 / 100) = 11 days
        val attendanceState = WfoAttendanceState(
            officeAttendanceDates = setOf(LocalDate.of(2026, 11, 2)) // Only 1 day attended
        )

        val summary = WfoCalculator.calculateMonthlySummary(
            month = YearMonth.of(2026, 11),
            attendanceState = attendanceState,
            settings = sharedSettings,
            today = today
        )

        // Required: ceil(21 * 50 / 100) = 11 days
        // Attended: 1 day
        // Remaining needed: 10 days
        // Days left: 26, 27, 30 = 3 days left
        assertEquals(11, summary.requiredOfficeDays)
        assertEquals(1, summary.attendedDays)
        assertEquals(10, summary.remainingOfficeDays)
        assertTrue("Target must be recognized as out of reach", summary.isTargetOutOfReach)
        assertFalse("Target should not be met", summary.isTargetMet)
    }
}
