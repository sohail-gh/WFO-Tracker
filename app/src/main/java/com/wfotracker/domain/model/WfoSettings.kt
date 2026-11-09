package com.wfotracker.domain.model

/**
 * Configuration settings for WFO calculation.
 *
 * @property targetPercentage Target monthly office attendance percentage (default: 40).
 *                            Must be between 1 and 100 inclusive.
 */
data class WfoSettings(
    val targetPercentage: Int = 40
) {
    init {
        require(targetPercentage in 1..100) {
            "Target percentage must be between 1 and 100, got: $targetPercentage"
        }
    }
}

