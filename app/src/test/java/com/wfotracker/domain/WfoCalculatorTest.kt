package com.wfotracker.domain

import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Comprehensive unit test suite for [WfoCalculator] and [WfoAttendanceState].
 * Covers all required business rules, boundary conditions, and edge cases.
 */
class WfoCalculatorTest {

    // =========================================================================
    // 1. Integer Arithmetic Ceiling & Required Days Tests
    // =========================================================================

    @Test
    fun `test 20 working days at 40 percent equals 8 required days`() {
        val required = WfoCalculator.calculateRequiredOfficeDays(workingDays = 20, targetPercentage = 40)
        assertEquals(8, required)
    }

    @Test
    fun `test 21 working days at 40 percent equals 9 required days`() {
        val required = WfoCalculator.calculateRequiredOfficeDays(workingDays = 21, targetPercentage = 40)
        assertEquals(9, required)
    }

    @Test
    fun `test 22 working days at 40 percent equals 9 required days`() {
        val required = WfoCalculator.calculateRequiredOfficeDays(workingDays = 22, targetPercentage = 40)
        assertEquals(9, required)
    }

    @Test
    fun `test 23 working days at 40 percent equals 10 required days`() {
        val required = WfoCalculator.calculateRequiredOfficeDays(workingDays = 23, targetPercentage = 40)
        assertEquals(10, required)
    }

    @Test
    fun `test 20 working days at 50 percent equals 10 required days`() {
        val required = WfoCalculator.calculateRequiredOfficeDays(workingDays = 20, targetPercentage = 50)
        assertEquals(10, required)
    }

    @Test
    fun `test 21 working days at 100 percent equals 21 required days`() {
        val required = WfoCalculator.calculateRequiredOfficeDays(workingDays = 21, targetPercentage = 100)
        assertEquals(21, required)
    }

    // =========================================================================
    // 2. Holiday and Leave Exclusion Tests
    // =========================================================================

    @Test
    fun `test holiday exclusion on a working day reduces working days count`() {
        // November 2026: 30 days total
        // Nov 1 is Sunday. Total weekdays (Mon-Fri) = 21 days.
        val month = YearMonth.of(2026, 11)
        val holidayDate = LocalDate.of(2026, 11, 4) // Wednesday

        val overrides = mapOf(
            holidayDate to DateOverride(holidayDate, WfoDayState.HOLIDAY, "Diwali")
        )

        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            officeAttendanceDates = emptySet(),
            overrides = overrides,
            settings = WfoSettings(targetPercentage = 40),
            today = LocalDate.of(2026, 11, 1)
        )

        // Baseline 21 weekdays minus 1 holiday = 20 working days
        assertEquals(20, summary.workingDays)
        // 20 working days at 40% = 8 required days
        assertEquals(8, summary.requiredOfficeDays)
    }

    @Test
    fun `test leave exclusion reduces working days count`() {
        val month = YearMonth.of(2026, 11)
        val leaveDate1 = LocalDate.of(2026, 11, 5) // Thursday
        val leaveDate2 = LocalDate.of(2026, 11, 6) // Friday

        val overrides = mapOf(
            leaveDate1 to DateOverride(leaveDate1, WfoDayState.LEAVE, "Vacation"),
            leaveDate2 to DateOverride(leaveDate2, WfoDayState.LEAVE, "Vacation")
        )

        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            officeAttendanceDates = emptySet(),
            overrides = overrides,
            settings = WfoSettings(targetPercentage = 40),
            today = LocalDate.of(2026, 11, 1)
        )

        // Baseline 21 weekdays minus 2 leaves = 19 working days
        assertEquals(19, summary.workingDays)
        // 19 * 40 / 100 = 7.6 -> ceil = 8
        assertEquals(8, summary.requiredOfficeDays)
    }

    @Test
    fun `test holiday falling on a weekend does not reduce working-day count again`() {
        val month = YearMonth.of(2026, 11)
        val saturdayHoliday = LocalDate.of(2026, 11, 7) // Saturday
        val sundayHoliday = LocalDate.of(2026, 11, 8) // Sunday

        val overrides = mapOf(
            saturdayHoliday to DateOverride(saturdayHoliday, WfoDayState.HOLIDAY, "Weekend Holiday 1"),
            sundayHoliday to DateOverride(sundayHoliday, WfoDayState.HOLIDAY, "Weekend Holiday 2")
        )

        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            officeAttendanceDates = emptySet(),
            overrides = overrides,
            settings = WfoSettings(targetPercentage = 40),
            today = LocalDate.of(2026, 11, 1)
        )

        // Weekend holidays must not reduce working days; working days must remain exactly 21
        assertEquals(21, summary.workingDays)
        assertEquals(9, summary.requiredOfficeDays)
    }

    // =========================================================================
    // 3. Office Attendance & Data Integrity Transition Tests
    // =========================================================================

    @Test
    fun `test office attendance toggle and removal`() {
        val workDay = LocalDate.of(2026, 11, 2) // Monday
        var state = WfoAttendanceState()

        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))

        // Toggle to mark office
        state = state.toggleOffice(workDay)
        assertEquals(WfoDayState.OFFICE, state.resolveDayState(workDay))
        assertTrue(state.officeAttendanceDates.contains(workDay))

        // Toggle to remove office
        state = state.toggleOffice(workDay)
        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))

        // Mark explicitly and remove explicitly
        state = state.markOffice(workDay)
        assertEquals(WfoDayState.OFFICE, state.resolveDayState(workDay))
        state = state.removeOffice(workDay)
        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))
    }

    @Test
    fun `test holiday replacing existing office attendance`() {
        val workDay = LocalDate.of(2026, 11, 3) // Tuesday
        var state = WfoAttendanceState().markOffice(workDay)

        assertEquals(WfoDayState.OFFICE, state.resolveDayState(workDay))
        assertTrue(state.officeAttendanceDates.contains(workDay))

        // Rule 13: Marking date as holiday must remove office attendance
        state = state.markHoliday(workDay, "Ad-hoc Holiday")
        assertEquals(WfoDayState.HOLIDAY, state.resolveDayState(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))
    }

    @Test
    fun `test clearing holiday restores normal working day without restoring previous attendance`() {
        val workDay = LocalDate.of(2026, 11, 3) // Tuesday

        // User had previously marked office, then holiday was marked (which cleared office)
        var state = WfoAttendanceState()
            .markOffice(workDay)
            .markHoliday(workDay, "Ad-hoc Holiday")

        assertEquals(WfoDayState.HOLIDAY, state.resolveDayState(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))

        // Rule 14: Clearing holiday restores normal working day, never previous office attendance
        state = state.clearHoliday(workDay)
        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))
    }

    @Test
    fun `test leave replacing office attendance and clearing leave restores working day`() {
        val workDay = LocalDate.of(2026, 11, 4) // Wednesday

        var state = WfoAttendanceState()
            .markOffice(workDay)
            .markLeave(workDay, "Sick Leave")

        assertEquals(WfoDayState.LEAVE, state.resolveDayState(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))

        state = state.clearLeave(workDay)
        assertEquals(WfoDayState.WORKING_DAY, state.resolveDayState(workDay))
        assertFalse(state.officeAttendanceDates.contains(workDay))
    }

    // =========================================================================
    // 4. Future and Multi-Year Holiday Support
    // =========================================================================

    @Test
    fun `test future holiday exclusion across multi-year timeline`() {
        val futureMonth = YearMonth.of(2028, 8) // August 2028: 31 days
        // August 1, 2028 is a Tuesday. 23 weekdays in total.
        val independenceDay2028 = LocalDate.of(2028, 8, 15) // Tuesday

        val overrides = mapOf(
            independenceDay2028 to DateOverride(independenceDay2028, WfoDayState.HOLIDAY, "Independence Day")
        )

        val summary = WfoCalculator.calculateMonthlySummary(
            month = futureMonth,
            officeAttendanceDates = emptySet(),
            overrides = overrides,
            settings = WfoSettings(targetPercentage = 40),
            today = LocalDate.of(2026, 11, 9) // Today is in 2026
        )

        assertEquals(31, summary.totalCalendarDays)
        // 23 weekdays minus 1 holiday = 22 working days
        assertEquals(22, summary.workingDays)
        // 22 working days at 40% = 9 required days
        assertEquals(9, summary.requiredOfficeDays)
        // For a future month, all eligible working days are still available
        assertEquals(22, summary.daysLeft)
        assertFalse(summary.isTargetOutOfReach)
        assertFalse(summary.isTargetMet)
    }

    // =========================================================================
    // 5. Past and Future Month Calculations
    // =========================================================================

    @Test
    fun `test past month calculation with target achieved`() {
        val pastMonth = YearMonth.of(2026, 10) // October 2026: 31 days, 22 weekdays
        val today = LocalDate.of(2026, 11, 9)

        // 22 working days at 40% = 9 required days
        // Suppose user attended 10 days in October
        val officeDates = (1..14)
            .map { pastMonth.atDay(it) }
            .filter { it.dayOfWeek.value in 1..5 }
            .take(10)
            .toSet()

        val summary = WfoCalculator.calculateMonthlySummary(
            month = pastMonth,
            officeAttendanceDates = officeDates,
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = today
        )

        assertEquals(22, summary.workingDays)
        assertEquals(10, summary.attendedDays)
        assertEquals(9, summary.requiredOfficeDays)
        assertEquals(0, summary.remainingOfficeDays)
        assertEquals(0, summary.daysLeft) // Past month has 0 days left
        assertTrue(summary.isTargetMet)
        assertFalse(summary.isTargetOutOfReach)
        assertEquals(100.0, summary.progressPercentage, 0.001)
    }

    @Test
    fun `test past month calculation with target not met is out of reach`() {
        val pastMonth = YearMonth.of(2026, 10)
        val today = LocalDate.of(2026, 11, 9)

        // 22 working days at 40% = 9 required days. User only attended 5 days.
        val officeDates = (1..10)
            .map { pastMonth.atDay(it) }
            .filter { it.dayOfWeek.value in 1..5 }
            .take(5)
            .toSet()

        val summary = WfoCalculator.calculateMonthlySummary(
            month = pastMonth,
            officeAttendanceDates = officeDates,
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = today
        )

        assertEquals(9, summary.requiredOfficeDays)
        assertEquals(5, summary.attendedDays)
        assertEquals(4, summary.remainingOfficeDays)
        assertEquals(0, summary.daysLeft)
        assertFalse(summary.isTargetMet)
        assertTrue(summary.isTargetOutOfReach) // remaining (4) > daysLeft (0)
    }

    // =========================================================================
    // 6. Calendar Boundaries & Leap Year Tests
    // =========================================================================

    @Test
    fun `test leap-year February 2024 has 29 calendar days and 21 working days`() {
        val leapFeb = YearMonth.of(2024, 2)
        val today = LocalDate.of(2024, 2, 1)

        val summary = WfoCalculator.calculateMonthlySummary(
            month = leapFeb,
            officeAttendanceDates = emptySet(),
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = today
        )

        assertEquals(29, summary.totalCalendarDays)
        assertEquals(21, summary.workingDays)
        assertEquals(9, summary.requiredOfficeDays)
    }

    @Test
    fun `test non-leap February 2023 has 28 calendar days and 20 working days`() {
        val nonLeapFeb = YearMonth.of(2023, 2)
        val today = LocalDate.of(2023, 2, 1)

        val summary = WfoCalculator.calculateMonthlySummary(
            month = nonLeapFeb,
            officeAttendanceDates = emptySet(),
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = today
        )

        assertEquals(28, summary.totalCalendarDays)
        assertEquals(20, summary.workingDays)
        assertEquals(8, summary.requiredOfficeDays)
    }

    @Test
    fun `test December to January year boundary calculations`() {
        val dec2025 = YearMonth.of(2025, 12)
        val jan2026 = YearMonth.of(2026, 1)
        val boundaryToday = LocalDate.of(2025, 12, 31)

        val decSummary = WfoCalculator.calculateMonthlySummary(
            month = dec2025,
            officeAttendanceDates = emptySet(),
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = boundaryToday
        )

        // Dec 31, 2025 is a Wednesday
        assertEquals(31, decSummary.totalCalendarDays)
        // Dec 2025 has 23 weekdays. On Dec 31, only Dec 31 is left.
        assertEquals(1, decSummary.daysLeft)

        val janSummary = WfoCalculator.calculateMonthlySummary(
            month = jan2026,
            officeAttendanceDates = emptySet(),
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = boundaryToday
        )

        // Jan 2026 is in the future relative to boundaryToday (2025-12-31)
        assertEquals(31, janSummary.totalCalendarDays)
        assertEquals(22, janSummary.workingDays)
        assertEquals(22, janSummary.daysLeft)
    }

    // =========================================================================
    // 7. Edge Cases: Zero Working Days, Target Met, Out of Reach, Invalid Settings
    // =========================================================================

    @Test
    fun `test zero eligible working days when entire month is on leave`() {
        val month = YearMonth.of(2026, 11)
        // Mark every weekday as LEAVE
        val overrides = (1..30)
            .map { month.atDay(it) }
            .filter { it.dayOfWeek.value in 1..5 }
            .associateWith { DateOverride(it, WfoDayState.LEAVE, "Long Leave") }

        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            officeAttendanceDates = emptySet(),
            overrides = overrides,
            settings = WfoSettings(targetPercentage = 40),
            today = LocalDate.of(2026, 11, 1)
        )

        assertEquals(0, summary.workingDays)
        assertEquals(0, summary.requiredOfficeDays)
        assertEquals(0, summary.remainingOfficeDays)
        assertEquals(0, summary.daysLeft)
        assertTrue(summary.isTargetMet)
        assertFalse(summary.isTargetOutOfReach)
        assertEquals(0.0, summary.progressPercentage, 0.001)
    }

    @Test
    fun `test target met status when attended days reach required office days`() {
        val month = YearMonth.of(2026, 11) // 21 working days, 9 required
        val officeDates = (1..15)
            .map { month.atDay(it) }
            .filter { it.dayOfWeek.value in 1..5 }
            .take(9)
            .toSet()

        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            officeAttendanceDates = officeDates,
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = LocalDate.of(2026, 11, 16)
        )

        assertEquals(9, summary.attendedDays)
        assertEquals(9, summary.requiredOfficeDays)
        assertEquals(0, summary.remainingOfficeDays)
        assertTrue(summary.isTargetMet)
        assertFalse(summary.isTargetOutOfReach)
        assertEquals(100.0, summary.progressPercentage, 0.001)
    }

    @Test
    fun `test target out of reach when remaining needed days exceed days left`() {
        val month = YearMonth.of(2026, 11) // 21 working days, 9 required
        // Today is Nov 25 (Wednesday). Remaining weekdays in month: Nov 25, 26, 27, 30 = 4 days.
        val today = LocalDate.of(2026, 11, 25)
        // Suppose user only attended 2 office days earlier in the month
        val officeDates = setOf(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3))

        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            officeAttendanceDates = officeDates,
            overrides = emptyMap(),
            settings = WfoSettings(targetPercentage = 40),
            today = today
        )

        assertEquals(21, summary.workingDays)
        assertEquals(2, summary.attendedDays)
        assertEquals(9, summary.requiredOfficeDays)
        assertEquals(7, summary.remainingOfficeDays) // Needs 7 more days
        assertEquals(4, summary.daysLeft) // Only 4 working days left in month
        assertFalse(summary.isTargetMet)
        assertTrue(summary.isTargetOutOfReach) // 7 > 4 -> OUT OF REACH
        assertEquals((2.0 / 9.0) * 100.0, summary.progressPercentage, 0.001)
    }

    @Test
    fun `test invalid target percentage throws IllegalArgumentException`() {
        try {
            WfoSettings(targetPercentage = 0)
            fail("Expected IllegalArgumentException for targetPercentage = 0")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("between 1 and 100"))
        }

        try {
            WfoSettings(targetPercentage = 101)
            fail("Expected IllegalArgumentException for targetPercentage = 101")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("between 1 and 100"))
        }
    }
}

