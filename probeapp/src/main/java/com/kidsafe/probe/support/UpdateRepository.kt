package com.kidsafe.probe.support

import org.json.JSONObject

data class UpdateConfig(
    val latestVersionCode: Int,
    val minSupportedVersionCode: Int,
    val storeUrl: String,
    val summary: String,
)

class UpdateRepository(private val assets: AssetTextRepository) {
    suspend fun loadConfig(currentLang: String = SupportLocale.currentTag()): UpdateConfig? {
        val raw = assets.loadRawSupport("update_config.json") ?: return null
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val latest = obj.optInt("latestVersionCode", -1)
        val min = obj.optInt("minSupportedVersionCode", -1)
        val storeUrl = obj.optString("storeUrl", "").trim()
        if (latest <= 0 || min <= 0 || storeUrl.isBlank()) return null
        val summaryObj = obj.optJSONObject("summary")
        val summary = summaryObj?.optString(currentLang)?.takeIf { it.isNotBlank() }
            ?: summaryObj?.optString("en")?.takeIf { it.isNotBlank() }
            ?: ""
        return UpdateConfig(latest, min, storeUrl, summary)
    }
}

