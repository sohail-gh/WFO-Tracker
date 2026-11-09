package com.wfotracker.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wfotracker.data.repository.WfoRepository
import com.wfotracker.domain.WfoCalculator
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.MonthlyWfoSummary
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/**
 * UI state for WFO Tracker screens.
 */
data class WfoUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val settings: WfoSettings = WfoSettings(),
    val attendanceState: WfoAttendanceState = WfoAttendanceState(),
    val summary: MonthlyWfoSummary = WfoCalculator.calculateMonthlySummary(
        month = YearMonth.now(),
        attendanceState = WfoAttendanceState(),
        settings = WfoSettings(),
        today = LocalDate.now()
    )
)

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
}

class MainViewModel(
    private val repository: WfoRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<WfoUiState> = combine(
        _selectedMonth,
        repository.settings,
        repository.attendanceState
    ) { month, settings, attendance ->
        val today = LocalDate.now()
        val summary = WfoCalculator.calculateMonthlySummary(
            month = month,
            attendanceState = attendance,
            settings = settings,
            today = today
        )
        WfoUiState(
            selectedMonth = month,
            today = today,
            settings = settings,
            attendanceState = attendance,
            summary = summary
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WfoUiState()
    )

    fun setSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun goToTodayMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    fun toggleOffice(date: LocalDate) {
        scope.launch {
            repository.toggleOfficeAttendance(date)
        }
    }

    fun markHoliday(date: LocalDate, name: String?) {
        scope.launch {
            repository.markHoliday(date, name)
            _events.emit(UiEvent.ShowSnackbar("Marked $date as Holiday"))
        }
    }

    fun markHolidayRange(startDate: LocalDate, endDate: LocalDate, name: String?) {
        scope.launch {
            repository.markHolidayRange(startDate, endDate, name)
            _events.emit(UiEvent.ShowSnackbar("Marked $startDate to $endDate as Holiday"))
        }
    }

    fun clearHoliday(date: LocalDate) {
        scope.launch {
            repository.clearHoliday(date)
            _events.emit(UiEvent.ShowSnackbar("Cleared holiday on $date"))
        }
    }

    fun markLeave(date: LocalDate, reason: String?) {
        scope.launch {
            repository.markLeave(date, reason)
            _events.emit(UiEvent.ShowSnackbar("Marked $date as Leave"))
        }
    }

    fun markLeaveRange(startDate: LocalDate, endDate: LocalDate, reason: String?) {
        scope.launch {
            repository.markLeaveRange(startDate, endDate, reason)
            _events.emit(UiEvent.ShowSnackbar("Marked $startDate to $endDate as Leave"))
        }
    }

    fun clearLeave(date: LocalDate) {
        scope.launch {
            repository.clearLeave(date)
            _events.emit(UiEvent.ShowSnackbar("Cleared leave on $date"))
        }
    }

    fun clearOverride(date: LocalDate) {
        scope.launch {
            repository.clearOverride(date)
            _events.emit(UiEvent.ShowSnackbar("Restored $date to Working Day"))
        }
    }

    fun deleteOverride(date: LocalDate) = clearOverride(date)

    fun setTargetPercentage(percentage: Int) {
        scope.launch {
            repository.setTargetPercentage(percentage)
        }
    }

    fun setGlassOpacity(opacity: Int) {
        scope.launch {
            repository.setGlassOpacity(opacity)
        }
    }

    fun exportBackup(contentResolver: ContentResolver, uri: Uri) {
        scope.launch {
            try {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    repository.exportBackup(stream).getOrThrow()
                    _events.emit(UiEvent.ShowSnackbar("Backup exported successfully"))
                } ?: run {
                    _events.emit(UiEvent.ShowSnackbar("Failed to open output stream"))
                }
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowSnackbar("Export failed: ${e.message}"))
            }
        }
    }

    fun importBackup(contentResolver: ContentResolver, uri: Uri) {
        scope.launch {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val result = repository.importBackup(stream)
                    if (result.isSuccess) {
                        _events.emit(UiEvent.ShowSnackbar("Backup restored successfully"))
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        _events.emit(UiEvent.ShowSnackbar("Import validation failed: $error"))
                    }
                } ?: run {
                    _events.emit(UiEvent.ShowSnackbar("Failed to open backup file"))
                }
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowSnackbar("Import failed: ${e.message}"))
            }
        }
    }

    companion object {
        fun provideFactory(repository: WfoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository) as T
                }
            }
    }
}

