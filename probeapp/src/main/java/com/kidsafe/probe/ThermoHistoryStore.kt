package com.kidsafe.probe

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ThermoHistoryRecord(
    val text: String,
    val epochMillis: Long,
)

class ThermoHistoryStore(private val context: Context) {
    private val keyHistory = stringPreferencesKey("thermo_history_v1")

    val history: Flow<List<ThermoHistoryRecord>> = context.probeDataStore.data.map { prefs ->
        decode(prefs[keyHistory].orEmpty())
    }

    suspend fun add(text: String) {
        context.probeDataStore.edit { prefs ->
            val current = decode(prefs[keyHistory].orEmpty())
            val updated = (listOf(ThermoHistoryRecord(text, System.currentTimeMillis())) + current).take(20)
            prefs[keyHistory] = encode(updated)
        }
    }

    suspend fun clear() {
        context.probeDataStore.edit { prefs ->
            prefs.remove(keyHistory)
        }
    }
}

private fun encode(list: List<ThermoHistoryRecord>): String {
    return list.joinToString(separator = "\n") { r ->
        "${r.epochMillis}\t${r.text.replace("\t", " ").replace("\n", " ")}"
    }
}

private fun decode(raw: String): List<ThermoHistoryRecord> {
    if (raw.isBlank()) return emptyList()
    return raw.split("\n")
        .mapNotNull { line ->
            val idx = line.indexOf('\t')
            if (idx <= 0) return@mapNotNull null
            val ts = line.substring(0, idx).toLongOrNull() ?: return@mapNotNull null
            val text = line.substring(idx + 1)
            ThermoHistoryRecord(text, ts)
        }
        .sortedByDescending { it.epochMillis }
        .take(20)
}

