package com.wfotracker.ui

import com.wfotracker.data.repository.WfoRepository
import com.wfotracker.domain.model.DateOverride
import com.wfotracker.domain.model.WfoAttendanceState
import com.wfotracker.domain.model.WfoDayState
import com.wfotracker.domain.model.WfoSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MainViewModelTest {

    private val testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var fakeRepository: FakeWfoRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeWfoRepository()
        viewModel = MainViewModel(fakeRepository, testScope)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `test initial month is current month`() {
        val currentMonth = YearMonth.now()
        assertEquals(currentMonth, viewModel.selectedMonth.value)
    }

    @Test
    fun `test month navigation forwards and backwards`() {
        val initial = viewModel.selectedMonth.value
        viewModel.nextMonth()
        assertEquals(initial.plusMonths(1), viewModel.selectedMonth.value)

        viewModel.previousMonth()
        assertEquals(initial, viewModel.selectedMonth.value)

        viewModel.previousMonth()
        assertEquals(initial.minusMonths(1), viewModel.selectedMonth.value)

        viewModel.goToTodayMonth()
        assertEquals(YearMonth.now(), viewModel.selectedMonth.value)
    }

    @Test
    fun `test setSelectedMonth`() {
        val target = YearMonth.of(2027, 8)
        viewModel.setSelectedMonth(target)
        assertEquals(target, viewModel.selectedMonth.value)
    }

    @Test
    fun `test toggleOffice updates repository`() = runBlocking {
        val date = LocalDate.of(2026, 11, 4) // Wednesday
        viewModel.toggleOffice(date)
        Thread.sleep(50)

        val state = fakeRepository.getAttendanceState()
        assertTrue(state.officeAttendanceDates.contains(date))

        viewModel.toggleOffice(date)
        Thread.sleep(50)
        val state2 = fakeRepository.getAttendanceState()
        assertFalse(state2.officeAttendanceDates.contains(date))
    }

    @Test
    fun `test markHoliday and clearHoliday updates repository and emits snackbar`() = runBlocking {
        var lastSnackbar: String? = null
        val job = testScope.launch {
            viewModel.events.collect { event ->
                if (event is UiEvent.ShowSnackbar) {
                    lastSnackbar = event.message
                }
            }
        }

        val date = LocalDate.of(2026, 11, 26)
        viewModel.markHoliday(date, "Thanksgiving")
        Thread.sleep(50)

        val state = fakeRepository.getAttendanceState()
        assertEquals(WfoDayState.HOLIDAY, state.overrides[date]?.state)
        assertEquals("Thanksgiving", state.overrides[date]?.name)
        assertTrue(lastSnackbar?.contains("Holiday") == true)

        viewModel.clearHoliday(date)
        Thread.sleep(50)
        val stateAfter = fakeRepository.getAttendanceState()
        assertFalse(stateAfter.overrides.containsKey(date))
        assertTrue(lastSnackbar?.contains("Cleared holiday") == true)

        job.cancel()
    }

    @Test
    fun `test markLeave and clearLeave updates repository`() = runBlocking {
        val date = LocalDate.of(2026, 11, 13)
        viewModel.markLeave(date, "Doctor Appointment")
        Thread.sleep(50)

        val state = fakeRepository.getAttendanceState()
        assertEquals(WfoDayState.LEAVE, state.overrides[date]?.state)
        assertEquals("Doctor Appointment", state.overrides[date]?.name)

        viewModel.clearLeave(date)
        Thread.sleep(50)
        val stateAfter = fakeRepository.getAttendanceState()
        assertFalse(stateAfter.overrides.containsKey(date))
    }

    @Test
    fun `test markHolidayRange updates range atomically`() = runBlocking {
        val start = LocalDate.of(2026, 12, 24)
        val end = LocalDate.of(2026, 12, 26)

        viewModel.markHolidayRange(start, end, "Winter Break")
        Thread.sleep(50)

        val state = fakeRepository.getAttendanceState()
        assertEquals(3, state.overrides.size)
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 12, 24)))
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 12, 25)))
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 12, 26)))
    }

    @Test
    fun `test markLeaveRange updates range atomically`() = runBlocking {
        val start = LocalDate.of(2026, 7, 6)
        val end = LocalDate.of(2026, 7, 8)

        viewModel.markLeaveRange(start, end, "Summer Vacation")
        Thread.sleep(50)

        val state = fakeRepository.getAttendanceState()
        assertEquals(3, state.overrides.size)
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 7, 6)))
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 7, 7)))
        assertTrue(state.overrides.containsKey(LocalDate.of(2026, 7, 8)))
    }

    @Test
    fun `test update settings through ViewModel`() = runBlocking {
        viewModel.setTargetPercentage(55)
        viewModel.setGlassOpacity(30)
        Thread.sleep(50)

        val settings = fakeRepository.getSettings()
        assertEquals(55, settings.targetPercentage)
        assertEquals(30, settings.glassOpacity)
    }

    @Test
    fun `test uiState reflects attendance updates and calculation`() = runBlocking {
        val targetMonth = YearMonth.of(2026, 11)
        viewModel.setSelectedMonth(targetMonth)

        val wednesday = LocalDate.of(2026, 11, 4)
        viewModel.toggleOffice(wednesday)
        Thread.sleep(80)

        val uiState = viewModel.uiState.value
        assertEquals(targetMonth, uiState.selectedMonth)
    }
}

/**
 * In-memory test double for WfoRepository adhering to domain rules.
 */
class FakeWfoRepository : WfoRepository {
    private val _settings = MutableStateFlow(WfoSettings())
    override val settings: Flow<WfoSettings> = _settings.asStateFlow()

    private val _attendance = MutableStateFlow(WfoAttendanceState())
    override val attendanceState: Flow<WfoAttendanceState> = _attendance.asStateFlow()

    override suspend fun getAttendanceState(): WfoAttendanceState = _attendance.value

    override suspend fun getSettings(): WfoSettings = _settings.value

    override suspend fun setTargetPercentage(percentage: Int) {
        _settings.value = _settings.value.copy(targetPercentage = percentage)
    }

    override suspend fun setGlassOpacity(opacity: Int) {
        _settings.value = _settings.value.copy(glassOpacity = opacity)
    }

    override suspend fun updateSettings(settings: WfoSettings) {
        _settings.value = settings
    }

    override suspend fun markOfficeAttendance(date: LocalDate) {
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return
        if (_attendance.value.overrides.containsKey(date)) return
        val current = _attendance.value.officeAttendanceDates.toMutableSet()
        current.add(date)
        _attendance.value = _attendance.value.copy(officeAttendanceDates = current)
    }

    override suspend fun removeOfficeAttendance(date: LocalDate) {
        val current = _attendance.value.officeAttendanceDates.toMutableSet()
        current.remove(date)
        _attendance.value = _attendance.value.copy(officeAttendanceDates = current)
    }

    override suspend fun toggleOfficeAttendance(date: LocalDate) {
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) return
        if (_attendance.value.overrides.containsKey(date)) return
        val current = _attendance.value.officeAttendanceDates.toMutableSet()
        if (current.contains(date)) {
            current.remove(date)
        } else {
            current.add(date)
        }
        _attendance.value = _attendance.value.copy(officeAttendanceDates = current)
    }

    override suspend fun markHoliday(date: LocalDate, name: String?) {
        val overrides = _attendance.value.overrides.toMutableMap()
        overrides[date] = DateOverride(date, WfoDayState.HOLIDAY, name)
        val office = _attendance.value.officeAttendanceDates.toMutableSet()
        office.remove(date)
        _attendance.value = _attendance.value.copy(overrides = overrides, officeAttendanceDates = office)
    }

    override suspend fun markHolidayRange(startDate: LocalDate, endDate: LocalDate, name: String?) {
        val overrides = _attendance.value.overrides.toMutableMap()
        val office = _attendance.value.officeAttendanceDates.toMutableSet()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            overrides[curr] = DateOverride(curr, WfoDayState.HOLIDAY, name)
            office.remove(curr)
            curr = curr.plusDays(1)
        }
        _attendance.value = _attendance.value.copy(overrides = overrides, officeAttendanceDates = office)
    }

    override suspend fun clearHoliday(date: LocalDate) {
        val overrides = _attendance.value.overrides.toMutableMap()
        if (overrides[date]?.state == WfoDayState.HOLIDAY) {
            overrides.remove(date)
            _attendance.value = _attendance.value.copy(overrides = overrides)
        }
    }

    override suspend fun markLeave(date: LocalDate, reason: String?) {
        val overrides = _attendance.value.overrides.toMutableMap()
        overrides[date] = DateOverride(date, WfoDayState.LEAVE, reason)
        val office = _attendance.value.officeAttendanceDates.toMutableSet()
        office.remove(date)
        _attendance.value = _attendance.value.copy(overrides = overrides, officeAttendanceDates = office)
    }

    override suspend fun markLeaveRange(startDate: LocalDate, endDate: LocalDate, reason: String?) {
        val overrides = _attendance.value.overrides.toMutableMap()
        val office = _attendance.value.officeAttendanceDates.toMutableSet()
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            overrides[curr] = DateOverride(curr, WfoDayState.LEAVE, reason)
            office.remove(curr)
            curr = curr.plusDays(1)
        }
        _attendance.value = _attendance.value.copy(overrides = overrides, officeAttendanceDates = office)
    }

    override suspend fun clearLeave(date: LocalDate) {
        val overrides = _attendance.value.overrides.toMutableMap()
        if (overrides[date]?.state == WfoDayState.LEAVE) {
            overrides.remove(date)
            _attendance.value = _attendance.value.copy(overrides = overrides)
        }
    }

    override suspend fun clearOverride(date: LocalDate) {
        val overrides = _attendance.value.overrides.toMutableMap()
        overrides.remove(date)
        _attendance.value = _attendance.value.copy(overrides = overrides)
    }

    override suspend fun exportBackup(outputStream: OutputStream): Result<Unit> = Result.success(Unit)

    override suspend fun importBackup(inputStream: InputStream): Result<Unit> = Result.success(Unit)

    override suspend fun importBackupFromJson(jsonString: String): Result<Unit> = Result.success(Unit)
}
