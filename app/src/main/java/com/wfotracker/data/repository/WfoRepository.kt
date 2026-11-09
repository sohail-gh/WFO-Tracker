package com.wfotracker.data.repository

import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

/**
 * Repository providing single source of truth for all WFO Tracker data.
 * All mutations are atomic and adhere strictly to domain integrity rules.
 */
interface WfoRepository {
    val attendanceState: Flow<WfoAttendanceState>
    val settings: Flow<WfoSettings>

    suspend fun getAttendanceState(): WfoAttendanceState
    suspend fun getSettings(): WfoSettings

    suspend fun setTargetPercentage(percentage: Int)
    suspend fun setGlassOpacity(opacity: Int)
    suspend fun updateSettings(settings: WfoSettings)

    suspend fun markOfficeAttendance(date: LocalDate)
    suspend fun removeOfficeAttendance(date: LocalDate)
    suspend fun toggleOfficeAttendance(date: LocalDate)

    suspend fun markHoliday(date: LocalDate, name: String? = null)
    suspend fun clearHoliday(date: LocalDate)

    suspend fun markLeave(date: LocalDate, reason: String? = null)
    suspend fun clearLeave(date: LocalDate)

    suspend fun clearOverride(date: LocalDate)

    suspend fun exportBackup(outputStream: OutputStream): Result<Unit>
    suspend fun importBackup(inputStream: InputStream): Result<Unit>
    suspend fun importBackupFromJson(jsonString: String): Result<Unit>
}
