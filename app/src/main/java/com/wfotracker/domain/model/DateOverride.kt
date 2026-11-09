package com.wfotracker.domain.model

import java.time.LocalDate

/**
 * Represents a manual date override (either a public/company holiday or approved leave).
 *
 * @property date The specific date for this override.
 * @property state Must be either [WfoDayState.HOLIDAY] or [WfoDayState.LEAVE].
 * @property name Optional human-readable label or reason.
 */
data class DateOverride(
    val date: LocalDate,
    val state: WfoDayState,
    val name: String? = null
) {
    init {
        require(state == WfoDayState.HOLIDAY || state == WfoDayState.LEAVE) {
            "DateOverride state must be either HOLIDAY or LEAVE, got: $state"
        }
    }
}

