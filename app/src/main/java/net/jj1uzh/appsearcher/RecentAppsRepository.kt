package net.jj1uzh.appsearcher

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "recent_apps")

class RecentAppsRepository(private val context: Context) {
    private val RECENT_APPS_KEY = stringPreferencesKey("recent_apps_list")

    val recentAppsFlow: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val joined = preferences[RECENT_APPS_KEY] ?: ""
            if (joined.isEmpty()) emptyList() else joined.split(",")
        }

    suspend fun addRecentApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[RECENT_APPS_KEY] ?: ""
            val list = if (current.isEmpty()) mutableListOf() else current.split(",").toMutableList()
            
            list.remove(packageName)
            list.add(0, packageName)
            
            val trimmed = list.take(4)
            preferences[RECENT_APPS_KEY] = trimmed.joinToString(",")
        }
    }
}
