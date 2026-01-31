package com.kidsafe.probe.support

import org.json.JSONArray
import org.json.JSONObject

data class ReleaseNote(
    val versionCode: Int,
    val versionName: String,
    val date: String,
    val title: String,
    val summary: String,
    val bodyMd: String,
)

class ReleaseNotesRepository(private val assets: AssetTextRepository) {
    suspend fun loadNotes(): List<ReleaseNote> {
        val raw = assets.loadLocalizedText("release_notes.json") ?: return emptyList()
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val arr = obj.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val it = arr.optJSONObject(i) ?: continue
                val code = it.optInt("versionCode", -1)
                if (code <= 0) continue
                add(
                    ReleaseNote(
                        versionCode = code,
                        versionName = it.optString("versionName", code.toString()),
                        date = it.optString("date", ""),
                        title = it.optString("title", ""),
                        summary = it.optString("summary", ""),
                        bodyMd = it.optString("bodyMd", ""),
                    )
                )
            }
        }.sortedByDescending { it.versionCode }
    }
}

