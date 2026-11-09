package com.wfotracker.domain.model

/**
 * Valid attendance and calendar day states.
 *
 * Architectural constraints:
 * - [WEEKEND] is derived dynamically from DayOfWeek, never manually stored.
 * - No separate WFH state exists; [WORKING_DAY] represents remote/non-office work.
 */
enum class WfoDayState {
    WORKING_DAY,
    OFFICE,
    HOLIDAY,
    LEAVE,
    WEEKEND
}

