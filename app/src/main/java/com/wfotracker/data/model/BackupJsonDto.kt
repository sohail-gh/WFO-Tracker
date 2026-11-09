package com.wfotracker.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for the standard WFO Tracker JSON backup and restore schema.
 */

@Serializable
data class BackupSettingsDto(
    @SerialName("target_percent") val targetPercent: Int = 40,
    @SerialName("glass_opacity") val glassOpacity: Int = 25
)

@Serializable
data class OverrideDetailDto(
    @SerialName("state") val state: String,
    @SerialName("name") val name: String? = null
)

@Serializable
data class BackupJsonDto(
    @SerialName("settings") val settings: BackupSettingsDto = BackupSettingsDto(),
    @SerialName("office") val office: List<String> = emptyList(),
    @SerialName("overrides") val overrides: Map<String, OverrideDetailDto> = emptyMap()
)
