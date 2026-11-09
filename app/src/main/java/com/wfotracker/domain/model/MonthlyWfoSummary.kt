package com.wfotracker.domain.model

/**
 * Monthly Return-to-Office compliance summary and progress metrics.
 *
 * @property totalCalendarDays Total days in the month (28, 29, 30, or 31).
 * @property workingDays Count of eligible business days (Mon-Fri excluding holidays and leaves).
 * @property attendedDays Count of verified office attendance days on eligible working days.
 * @property requiredOfficeDays Ceiled required office days using integer arithmetic:
 *                              ceil(workingDays * targetPercentage / 100).
 * @property remainingOfficeDays max(0, requiredOfficeDays - attendedDays).
 * @property daysLeft Eligible working days from today inclusive through the end of the month,
 *                    excluding dates already marked OFFICE.
 * @property isTargetMet True when remainingOfficeDays == 0.
 * @property isTargetOutOfReach True when remainingOfficeDays > daysLeft.
 * @property progressPercentage Office attendance progress against target, bounded between 0.0 and 100.0.
 */
data class MonthlyWfoSummary(
    val totalCalendarDays: Int,
    val workingDays: Int,
    val attendedDays: Int,
    val requiredOfficeDays: Int,
    val remainingOfficeDays: Int,
    val daysLeft: Int,
    val isTargetMet: Boolean,
    val isTargetOutOfReach: Boolean,
    val progressPercentage: Double
)

