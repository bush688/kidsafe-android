package com.kidsafe.probe

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONObject

class ModuleInputStore(private val context: Context) {
    private fun key(moduleId: String) = stringPreferencesKey("module_input_v1_$moduleId")

    suspend fun load(moduleId: String): JSONObject? {
        val raw = context.probeDataStore.data.first()[key(moduleId)] ?: return null
        if (raw.isBlank()) return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    suspend fun save(moduleId: String, value: JSONObject) {
        context.probeDataStore.edit { prefs ->
            prefs[key(moduleId)] = value.toString()
        }
    }

    suspend fun clear(moduleId: String) {
        context.probeDataStore.edit { prefs ->
            prefs.remove(key(moduleId))
        }
    }
}

