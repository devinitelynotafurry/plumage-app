package dev.plumage.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.plumage.domain.model.Settings
import dev.plumage.domain.model.SortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "plumage_settings")

@Singleton
class SettingsRepository(
    private val context: Context
) {
    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val BLOCKED_TAGS = stringPreferencesKey("blocked_tags")
        val FILTER_AI = booleanPreferencesKey("filter_ai")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LAST_QUERY = stringPreferencesKey("last_query")
        val SORT_MODE = stringPreferencesKey("sort_mode")
    }

    val settings: Flow<Settings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            Settings(
                username = prefs[Keys.USERNAME].orEmpty(),
                blockedTags = prefs[Keys.BLOCKED_TAGS] ?: Settings.DEFAULT_BLOCKED_TAGS,
                filterAiContent = prefs[Keys.FILTER_AI] ?: true,
                useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true
            )
        }

    val lastQuery: Flow<String> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.LAST_QUERY].orEmpty() }

    val sortMode: Flow<SortMode> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            runCatching { SortMode.valueOf(prefs[Keys.SORT_MODE] ?: SortMode.NEWEST.name) }
                .getOrDefault(SortMode.NEWEST)
        }

    /**
     * Read synchronously for the OkHttp User-Agent header, which runs off the main
     * thread inside an interceptor and cannot suspend. Cached from the flow below.
     */
    @Volatile
    var cachedUsername: String = ""
        private set

    suspend fun observeUsernameIntoCache() {
        settings.collect { cachedUsername = it.username }
    }

    suspend fun setUsername(value: String) = update { it[Keys.USERNAME] = value.trim() }
    suspend fun setBlockedTags(value: String) = update { it[Keys.BLOCKED_TAGS] = value }
    suspend fun setFilterAi(value: Boolean) = update { it[Keys.FILTER_AI] = value }
    suspend fun setDynamicColor(value: Boolean) = update { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setLastQuery(value: String) = update { it[Keys.LAST_QUERY] = value }
    suspend fun setSortMode(value: SortMode) = update { it[Keys.SORT_MODE] = value.name }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
