package com.hairstyle.generator.aimakerover.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hairstyle_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recentPhotosKey = stringPreferencesKey("recent_photos")

    fun getRecentPhotos(): Flow<List<String>> =
        context.dataStore.data.map { prefs ->
            prefs[recentPhotosKey]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }

    suspend fun addRecentPhoto(uri: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[recentPhotosKey]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()

            current.remove(uri)
            current.add(0, uri)

            while (current.size > MAX_RECENT) {
                current.removeAt(current.lastIndex)
            }

            prefs[recentPhotosKey] = current.joinToString(SEPARATOR)
        }
    }

    companion object {
        private const val SEPARATOR = "|||"
        private const val MAX_RECENT = 30
    }
}
