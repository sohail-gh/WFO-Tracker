package com.wfotracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Immutable state representing recorded office attendance and date overrides (holidays / leaves).
 * Enforces business rules and data integrity guarantees.
 */
data class WfoAttendanceState(
    val officeAttendanceDates: Set<LocalDate> = emptySet(),
    val overrides: Map<LocalDate, DateOverride> = emptyMap()
) {
    /**
     * Resolves the effective [WfoDayState] for any calendar date.
     * Precedence:
     * 1. Weekend (Saturday or Sunday) - derived automatically.
     * 2. Override (Holiday or Leave) - explicit manual entry.
     * 3. Office Attendance - if present in attendance records.
     * 4. Working Day - default state (represents remote / non-office work).
     */
    fun resolveDayState(date: LocalDate): WfoDayState {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        if (isWeekend) {
            return WfoDayState.WEEKEND
        }

        val override = overrides[date]
        if (override != null) {
            return override.state
        }

        if (officeAttendanceDates.contains(date)) {
            return WfoDayState.OFFICE
        }

        return WfoDayState.WORKING_DAY
    }

    /**
     * Toggles or marks office attendance on a date.
     * Attendance is only applicable on eligible working days (not weekend, not holiday/leave).
     */
    fun markOffice(date: LocalDate): WfoAttendanceState {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        if (isWeekend || overrides.containsKey(date)) {
            return this
        }
        return copy(officeAttendanceDates = officeAttendanceDates + date)
    }

    /**
     * Removes recorded office attendance, cleanly restoring the date to [WfoDayState.WORKING_DAY].
     */
    fun removeOffice(date: LocalDate): WfoAttendanceState {
        return copy(officeAttendanceDates = officeAttendanceDates - date)
    }

    /**
     * Toggles office attendance: if already marked OFFICE, removes it; otherwise marks it.
     */
    fun toggleOffice(date: LocalDate): WfoAttendanceState {
        return if (officeAttendanceDates.contains(date)) {
            removeOffice(date)
        } else {
            markOffice(date)
        }
    }

    /**
     * Business Rule 13:
     * Marking a date as Holiday strictly removes any existing Office Attendance on that date.
     */
    fun markHoliday(date: LocalDate, name: String? = null): WfoAttendanceState {
        return copy(
            overrides = overrides + (date to DateOverride(date, WfoDayState.HOLIDAY, name)),
            officeAttendanceDates = officeAttendanceDates - date
        )
    }

    /**
     * Business Rule 14:
     * Clearing a Holiday restores a normal working day.
     * Previous office attendance is NEVER automatically restored.
     */
    fun clearHoliday(date: LocalDate): WfoAttendanceState {
        val current = overrides[date]
        return if (current?.state == WfoDayState.HOLIDAY) {
            copy(overrides = overrides - date)
        } else {
            this
        }
    }

    /**
     * Business Rule 13:
     * Marking a date as Leave strictly removes any existing Office Attendance on that date.
     */
    fun markLeave(date: LocalDate, reason: String? = null): WfoAttendanceState {
        return copy(
            overrides = overrides + (date to DateOverride(date, WfoDayState.LEAVE, reason)),
            officeAttendanceDates = officeAttendanceDates - date
        )
    }

    /**
     * Business Rule 14:
     * Clearing a Leave restores a normal working day.
     * Previous office attendance is NEVER automatically restored.
     */
    fun clearLeave(date: LocalDate): WfoAttendanceState {
        val current = overrides[date]
        return if (current?.state == WfoDayState.LEAVE) {
            copy(overrides = overrides - date)
        } else {
            this
        }
    }

    /**
     * Clears any override (Holiday or Leave) on the specified date.
     * Never restores previous office attendance.
     */
    fun clearOverride(date: LocalDate): WfoAttendanceState {
        return copy(overrides = overrides - date)
    }
}

