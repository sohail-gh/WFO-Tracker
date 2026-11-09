package com.wfotracker.domain

import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.MonthlyWfoSummary
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure Kotlin Return-to-Office (WFO) calculation engine.
 * Fully deterministic, with zero Android UI, Context, or system-clock dependencies.
 */
object WfoCalculator {

    /**
     * Calculates the required office attendance days using integer arithmetic ceiling:
     * ceil(workingDays * targetPercentage / 100) = (workingDays * targetPercentage + 99) / 100.
     *
     * @param workingDays Number of eligible working days in the period.
     * @param targetPercentage Monthly compliance target (1 to 100).
     * @return Ceiled required office days as an Int.
     */
    fun calculateRequiredOfficeDays(workingDays: Int, targetPercentage: Int): Int {
        require(targetPercentage in 1..100) {
            "Target percentage must be between 1 and 100, got: $targetPercentage"
        }
        if (workingDays <= 0) return 0
        return (workingDays * targetPercentage + 99) / 100
    }

    /**
     * Determines whether a given calendar date is an eligible working day.
     * Business rules:
     * - Monday through Friday are working days.
     * - Saturday and Sunday are weekends.
     * - Manually marked holidays are excluded.
     * - Manually marked leaves are excluded.
     * - Holidays falling on weekends do not reduce working day count again.
     */
    fun isEligibleWorkingDay(date: LocalDate, overrides: Map<LocalDate, DateOverride>): Boolean {
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        if (isWeekend) return false

        val override = overrides[date]
        if (override != null && (override.state == WfoDayState.HOLIDAY || override.state == WfoDayState.LEAVE)) {
            return false
        }

        return true
    }

    /**
     * Calculates the [MonthlyWfoSummary] for a specified [YearMonth].
     *
     * @param month The target calendar month.
     * @param officeAttendanceDates Set of dates with recorded office attendance.
     * @param overrides Map of dates to manual holiday/leave overrides.
     * @param settings Configuration settings (including target percentage).
     * @param today Explicit reference date representing "today" for deterministic time evaluation.
     */
    fun calculateMonthlySummary(
        month: YearMonth,
        officeAttendanceDates: Set<LocalDate>,
        overrides: Map<LocalDate, DateOverride>,
        settings: WfoSettings = WfoSettings(),
        today: LocalDate
    ): MonthlyWfoSummary {
        val totalCalendarDays = month.lengthOfMonth()
        val currentMonthOfToday = YearMonth.from(today)

        var workingDays = 0
        var attendedDays = 0
        var daysLeft = 0

        for (day in 1..totalCalendarDays) {
            val date = month.atDay(day)
            val isEligible = isEligibleWorkingDay(date, overrides)

            if (isEligible) {
                workingDays++

                val isAttended = officeAttendanceDates.contains(date)
                if (isAttended) {
                    attendedDays++
                }

                // Days left calculation:
                // Eligible working days from today inclusive through the end of the selected month,
                // excluding dates already marked OFFICE.
                val isDateInFutureOrToday = when {
                    month.isBefore(currentMonthOfToday) -> false
                    month.isAfter(currentMonthOfToday) -> true
                    else -> date >= today
                }

                if (isDateInFutureOrToday && !isAttended) {
                    daysLeft++
                }
            }
        }

        val requiredOfficeDays = calculateRequiredOfficeDays(workingDays, settings.targetPercentage)
        val remainingOfficeDays = (requiredOfficeDays - attendedDays).coerceAtLeast(0)
        val isTargetMet = remainingOfficeDays == 0
        val isTargetOutOfReach = remainingOfficeDays > daysLeft

        val progressPercentage = when {
            requiredOfficeDays > 0 -> {
                ((attendedDays.toDouble() / requiredOfficeDays.toDouble()) * 100.0).coerceIn(0.0, 100.0)
            }
            workingDays == 0 -> 0.0
            else -> 100.0
        }

        return MonthlyWfoSummary(
            totalCalendarDays = totalCalendarDays,
            workingDays = workingDays,
            attendedDays = attendedDays,
            requiredOfficeDays = requiredOfficeDays,
            remainingOfficeDays = remainingOfficeDays,
            daysLeft = daysLeft,
            isTargetMet = isTargetMet,
            isTargetOutOfReach = isTargetOutOfReach,
            progressPercentage = progressPercentage
        )
    }

    /**
     * Overloaded convenience method accepting [WfoAttendanceState].
     */
    fun calculateMonthlySummary(
        month: YearMonth,
        attendanceState: WfoAttendanceState,
        settings: WfoSettings = WfoSettings(),
        today: LocalDate
    ): MonthlyWfoSummary {
        return calculateMonthlySummary(
            month = month,
            officeAttendanceDates = attendanceState.officeAttendanceDates,
            overrides = attendanceState.overrides,
            settings = settings,
            today = today
        )
    }
}

