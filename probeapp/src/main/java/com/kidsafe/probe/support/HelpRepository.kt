package com.kidsafe.probe.support

import org.json.JSONArray
import org.json.JSONObject

data class HelpDocRef(
    val id: String,
    val title: String,
    val path: String,
)

data class HelpCategory(
    val id: String,
    val title: String,
    val items: List<HelpDocRef>,
)

class HelpRepository(private val assets: AssetTextRepository) {
    suspend fun loadIndex(): List<HelpCategory> {
        val raw = assets.loadLocalizedText("help/index.json") ?: return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("categories") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val itemsArr = c.optJSONArray("items") ?: JSONArray()
                val items = buildList {
                    for (j in 0 until itemsArr.length()) {
                        val it = itemsArr.optJSONObject(j) ?: continue
                        val id = it.optString("id", "").trim()
                        val title = it.optString("title", "").trim()
                        val path = it.optString("path", "").trim()
                        if (id.isBlank() || title.isBlank() || path.isBlank()) continue
                        add(HelpDocRef(id, title, path))
                    }
                }
                val id = c.optString("id", "").trim()
                val title = c.optString("title", "").trim()
                if (id.isBlank() || title.isBlank()) continue
                add(HelpCategory(id, title, items))
            }
        }
    }

    suspend fun loadDocMarkdown(docPath: String): String? {
        return assets.loadLocalizedText("help/$docPath")
    }
}

