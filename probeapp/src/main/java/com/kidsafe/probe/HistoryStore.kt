package com.kidsafe.probe

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CalcRecord(
    val chuanMm: Double,
    val vFar: Double,
    val vNear: Double,
    val epochMillis: Long,
)

class HistoryStore(private val context: Context) {
    private val keyHistory = stringPreferencesKey("history_v1")

    val history: Flow<List<CalcRecord>> = context.probeDataStore.data.map { prefs ->
        decodeHistory(prefs[keyHistory].orEmpty())
    }

    suspend fun add(record: CalcRecord) {
        context.probeDataStore.edit { prefs ->
            val current = decodeHistory(prefs[keyHistory].orEmpty())
            val updated = (listOf(record) + current).take(5)
            prefs[keyHistory] = encodeHistory(updated)
        }
    }

    suspend fun clear() {
        context.probeDataStore.edit { prefs ->
            prefs.remove(keyHistory)
        }
    }
}

private fun encodeHistory(list: List<CalcRecord>): String {
    return list.joinToString(separator = ";") { r ->
        "${r.chuanMm},${r.vFar},${r.vNear},${r.epochMillis}"
    }
}

private fun decodeHistory(raw: String): List<CalcRecord> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";")
        .mapNotNull { part ->
            val items = part.split(",")
            if (items.size != 4) return@mapNotNull null
            val c = items[0].toDoubleOrNull() ?: return@mapNotNull null
            val vf = items[1].toDoubleOrNull() ?: return@mapNotNull null
            val vn = items[2].toDoubleOrNull() ?: return@mapNotNull null
            val ts = items[3].toLongOrNull() ?: return@mapNotNull null
            CalcRecord(c, vf, vn, ts)
        }
        .sortedByDescending { it.epochMillis }
        .take(5)
}
