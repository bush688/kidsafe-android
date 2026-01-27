package com.kidsafe.probe.dplevel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kidsafe.probe.probeDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DpLevelHistoryRecord(
    val text: String,
    val epochMillis: Long,
)

class DpLevelHistoryStore(private val context: Context) {
    private val keyHistory = stringPreferencesKey("dp_level_history_v1")

    val history: Flow<List<DpLevelHistoryRecord>> = context.probeDataStore.data.map { prefs ->
        decode(prefs[keyHistory].orEmpty())
    }

    suspend fun add(text: String) {
        context.probeDataStore.edit { prefs ->
            val current = decode(prefs[keyHistory].orEmpty())
            val updated = (listOf(DpLevelHistoryRecord(text, System.currentTimeMillis())) + current).take(20)
            prefs[keyHistory] = encode(updated)
        }
    }

    suspend fun clear() {
        context.probeDataStore.edit { prefs ->
            prefs.remove(keyHistory)
        }
    }
}

private fun encode(list: List<DpLevelHistoryRecord>): String {
    return list.joinToString(separator = "\n") { r ->
        "${r.epochMillis}\t${r.text.replace("\t", " ").replace("\n", " ")}"
    }
}

private fun decode(raw: String): List<DpLevelHistoryRecord> {
    if (raw.isBlank()) return emptyList()
    return raw.split("\n")
        .mapNotNull { line ->
            val idx = line.indexOf('\t')
            if (idx <= 0) return@mapNotNull null
            val ts = line.substring(0, idx).toLongOrNull() ?: return@mapNotNull null
            val text = line.substring(idx + 1)
            DpLevelHistoryRecord(text, ts)
        }
        .sortedByDescending { it.epochMillis }
        .take(20)
}

