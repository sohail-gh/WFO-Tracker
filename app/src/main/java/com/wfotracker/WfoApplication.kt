package com.wfotracker

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.wfotracker.data.preferences.WfoDataStoreKeys
import com.wfotracker.data.repository.WfoRepository
import com.wfotracker.data.repository.WfoRepositoryImpl

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = WfoDataStoreKeys.DATASTORE_NAME)

class WfoApplication : Application() {

    val repository: WfoRepository by lazy {
        WfoRepositoryImpl(dataStore)
    }

    companion object {
        fun getRepository(context: Context): WfoRepository {
            val app = context.applicationContext as? WfoApplication
            return app?.repository ?: WfoRepositoryImpl(context.dataStore)
        }
    }
}
