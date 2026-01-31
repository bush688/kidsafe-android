package com.kidsafe.probe.support

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kidsafe.probe.probeDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PrivacyConsent(
    val accepted: Boolean,
    val acceptedAtIso: String?,
    val policyId: String?,
)

class SupportPrefsStore(private val context: Context) {
    private val privacyKey = stringPreferencesKey("support_privacy_consent_v1")
    private val lastSeenReleaseKey = stringPreferencesKey("support_last_seen_release_v1")

    val privacyConsent: Flow<PrivacyConsent?> = context.probeDataStore.data.map { prefs ->
        val raw = prefs[privacyKey] ?: return@map null
        if (raw.isBlank()) return@map null
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return@map null
        PrivacyConsent(
            accepted = obj.optBoolean("accepted", false),
            acceptedAtIso = obj.optString("acceptedAtIso").takeIf { it.isNotBlank() },
            policyId = obj.optString("policyId").takeIf { it.isNotBlank() },
        )
    }

    val lastSeenReleaseVersionCode: Flow<Int?> = context.probeDataStore.data.map { prefs ->
        val raw = prefs[lastSeenReleaseKey] ?: return@map null
        raw.toIntOrNull()
    }

    suspend fun acceptPrivacy(policyId: String) {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val obj = JSONObject()
            .put("accepted", true)
            .put("acceptedAtIso", iso)
            .put("policyId", policyId)
        context.probeDataStore.edit { prefs -> prefs[privacyKey] = obj.toString() }
    }

    suspend fun withdrawPrivacyConsent() {
        context.probeDataStore.edit { prefs -> prefs.remove(privacyKey) }
    }

    suspend fun markReleaseSeen(versionCode: Int) {
        context.probeDataStore.edit { prefs -> prefs[lastSeenReleaseKey] = versionCode.toString() }
    }
}

