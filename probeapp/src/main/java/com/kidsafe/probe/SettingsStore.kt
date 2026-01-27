package com.kidsafe.probe

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProbeSettings(
    val zeroVoltageV: Double,
)

class SettingsStore(private val context: Context) {
    private val keyZeroVoltage = stringPreferencesKey("zero_voltage_v1")

    val settings: Flow<ProbeSettings> = context.probeDataStore.data.map { prefs ->
        val v = prefs[keyZeroVoltage]?.toDoubleOrNull() ?: ProbeCalculator.defaultZeroVoltage()
        ProbeSettings(zeroVoltageV = v)
    }

    suspend fun setZeroVoltageV(value: Double) {
        context.probeDataStore.edit { prefs ->
            prefs[keyZeroVoltage] = value.toString()
        }
    }
}

