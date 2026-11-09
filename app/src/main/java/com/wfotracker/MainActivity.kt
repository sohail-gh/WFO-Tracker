package com.wfotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wfotracker.ui.MainViewModel
import com.wfotracker.ui.UiEvent
import com.wfotracker.ui.navigation.WfoNavScreen
import com.wfotracker.ui.screens.CalendarScreen
import com.wfotracker.ui.screens.DashboardScreen
import com.wfotracker.ui.screens.OverridesScreen
import com.wfotracker.ui.screens.SettingsScreen
import com.wfotracker.ui.theme.WFOTrackerTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(WfoApplication.getRepository(this))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WFOTrackerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var currentScreen by rememberSaveable { mutableStateOf(WfoNavScreen.DASHBOARD) }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.events.collectLatest { event ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> {
                                snackbarHostState.showSnackbar(event.message)
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = currentScreen.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            WfoNavScreen.entries.forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) }
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Crossfade(
                        targetState = currentScreen,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        label = "ScreenCrossfade"
                    ) { screen ->
                        when (screen) {
                            WfoNavScreen.DASHBOARD -> {
                                DashboardScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onGoToTodayMonth = { viewModel.goToTodayMonth() },
                                    onToggleTodayOffice = { viewModel.toggleOffice(uiState.today) },
                                    onMarkTodayHoliday = { name -> viewModel.markHoliday(uiState.today, name) },
                                    onMarkTodayLeave = { reason -> viewModel.markLeave(uiState.today, reason) },
                                    onClearTodayOverride = { viewModel.clearOverride(uiState.today) }
                                )
                            }
                            WfoNavScreen.CALENDAR -> {
                                CalendarScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onGoToTodayMonth = { viewModel.goToTodayMonth() },
                                    onToggleOffice = { date -> viewModel.toggleOffice(date) },
                                    onMarkHoliday = { date, name -> viewModel.markHoliday(date, name) },
                                    onMarkLeave = { date, reason -> viewModel.markLeave(date, reason) },
                                    onClearOverride = { date -> viewModel.clearOverride(date) }
                                )
                            }
                            WfoNavScreen.OVERRIDES -> {
                                OverridesScreen(
                                    uiState = uiState,
                                    onPreviousMonth = { viewModel.previousMonth() },
                                    onNextMonth = { viewModel.nextMonth() },
                                    onGoToTodayMonth = { viewModel.goToTodayMonth() },
                                    onMarkHoliday = { date, name -> viewModel.markHoliday(date, name) },
                                    onMarkHolidayRange = { start, end, name -> viewModel.markHolidayRange(start, end, name) },
                                    onMarkLeave = { date, reason -> viewModel.markLeave(date, reason) },
                                    onMarkLeaveRange = { start, end, reason -> viewModel.markLeaveRange(start, end, reason) },
                                    onDeleteOverride = { date -> viewModel.deleteOverride(date) }
                                )
                            }
                            WfoNavScreen.SETTINGS -> {
                                SettingsScreen(
                                    settings = uiState.settings,
                                    onTargetPercentageChange = { pct -> viewModel.setTargetPercentage(pct) },
                                    onGlassOpacityChange = { op -> viewModel.setGlassOpacity(op) },
                                    onExportBackup = { uri -> viewModel.exportBackup(contentResolver, uri) },
                                    onImportBackup = { uri -> viewModel.importBackup(contentResolver, uri) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
