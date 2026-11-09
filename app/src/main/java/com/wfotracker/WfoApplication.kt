package com.wfotracker

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.wfotracker.data.preferences.WfoDataStoreKeys
import com.wfotracker.data.repository.WfoRepository
import com.wfotracker.data.repository.WfoRepositoryImpl
import com.wfotracker.widget.WfoWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = WfoDataStoreKeys.DATASTORE_NAME)

class WfoApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val repository: WfoRepository by lazy {
        WfoRepositoryImpl(dataStore)
    }

    override fun onCreate() {
        super.onCreate()
        observeDataChangesForWidget()
    }

    private fun observeDataChangesForWidget() {
        applicationScope.launch {
            combine(repository.attendanceState, repository.settings) { _, _ -> }
                .drop(1)
                .collectLatest {
                    WfoWidgetUpdater.updateAll(this@WfoApplication)
                }
        }
    }

    companion object {
        fun getRepository(context: Context): WfoRepository {
            val app = context.applicationContext as? WfoApplication
            return app?.repository ?: WfoRepositoryImpl(context.dataStore)
        }
    }
}
