package com.wfotracker.domain.model

/**
 * Configuration settings for WFO calculation and appearance.
 *
 * @property targetPercentage Target monthly office attendance percentage (default: 40).
 *                            Must be between 1 and 100 inclusive.
 * @property glassOpacity Opacity percentage for widget glassmorphism background (default: 25).
 *                        Must be between 0 and 100 inclusive.
 */
data class WfoSettings(
    val targetPercentage: Int = 40,
    val glassOpacity: Int = 25
) {
    init {
        require(targetPercentage in 1..100) {
            "Target percentage must be between 1 and 100, got: $targetPercentage"
        }
        require(glassOpacity in 0..100) {
            "Glass opacity must be between 0 and 100, got: $glassOpacity"
        }
    }
}
