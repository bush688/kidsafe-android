package com.kidsafe.probe.cable

import android.content.Context
import com.kidsafe.probe.probeDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

data class CableScenario(
    val name: String,
    val material: CableMaterial,
    val areaUnit: AreaUnit,
    val areaMm2: Double?,
    val awgGauge: Int?,
    val length: Double,
    val lengthUnit: LengthUnit,
    val temperatureC: Double,
    val circuitType: CircuitType,
    val wiring: VoltageDropWiring,
    val currentA: Double,
    val supplyV: Double,
    val limitPercent: Double,
    val powerFactor: Double?,
    val frequencyHz: Double?,
    val inductanceMhPerKm: Double?,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("material", material.name)
        .put("areaUnit", areaUnit.name)
        .put("areaMm2", areaMm2)
        .put("awgGauge", awgGauge)
        .put("length", length)
        .put("lengthUnit", lengthUnit.name)
        .put("temperatureC", temperatureC)
        .put("circuitType", circuitType.name)
        .put("wiring", wiring.name)
        .put("currentA", currentA)
        .put("supplyV", supplyV)
        .put("limitPercent", limitPercent)
        .put("powerFactor", powerFactor)
        .put("frequencyHz", frequencyHz)
        .put("inductanceMhPerKm", inductanceMhPerKm)

    companion object {
        fun fromJson(obj: JSONObject): CableScenario? = runCatching {
            CableScenario(
                name = obj.optString("name").takeIf { it.isNotBlank() } ?: return null,
                material = CableMaterial.valueOf(obj.optString("material", CableMaterial.COPPER.name)),
                areaUnit = AreaUnit.valueOf(obj.optString("areaUnit", AreaUnit.MM2.name)),
                areaMm2 = if (obj.has("areaMm2")) obj.optDouble("areaMm2").takeIf { it.isFinite() } else null,
                awgGauge = if (obj.has("awgGauge")) obj.optInt("awgGauge") else null,
                length = obj.optDouble("length", 0.0),
                lengthUnit = LengthUnit.valueOf(obj.optString("lengthUnit", LengthUnit.METER.name)),
                temperatureC = obj.optDouble("temperatureC", 20.0),
                circuitType = CircuitType.valueOf(obj.optString("circuitType", CircuitType.DC.name)),
                wiring = VoltageDropWiring.valueOf(obj.optString("wiring", VoltageDropWiring.SINGLE_PHASE_2W.name)),
                currentA = obj.optDouble("currentA", 0.0),
                supplyV = obj.optDouble("supplyV", 220.0),
                limitPercent = obj.optDouble("limitPercent", 3.0),
                powerFactor = if (obj.has("powerFactor")) obj.optDouble("powerFactor").takeIf { it.isFinite() } else null,
                frequencyHz = if (obj.has("frequencyHz")) obj.optDouble("frequencyHz").takeIf { it.isFinite() } else null,
                inductanceMhPerKm = if (obj.has("inductanceMhPerKm")) obj.optDouble("inductanceMhPerKm").takeIf { it.isFinite() } else null,
            )
        }.getOrNull()
    }
}

class CableCalcStore(private val context: Context) {
    private val presetsKey = stringPreferencesKey("cable_presets_v1")
    private val lastKey = stringPreferencesKey("cable_last_v1")

    val presets: Flow<List<CableScenario>> = context.probeDataStore.data.map { prefs ->
        val raw = prefs[presetsKey] ?: return@map emptyList()
        decodeScenarioList(raw)
    }

    val lastScenario: Flow<CableScenario?> = context.probeDataStore.data.map { prefs ->
        val raw = prefs[lastKey] ?: return@map null
        decodeScenario(raw)
    }

    suspend fun saveLast(scenario: CableScenario) {
        context.probeDataStore.edit { prefs ->
            prefs[lastKey] = scenario.toJson().toString()
        }
    }

    suspend fun upsertPreset(scenario: CableScenario) {
        context.probeDataStore.edit { prefs ->
            val list = decodeScenarioList(prefs[presetsKey] ?: "")
                .filterNot { it.name == scenario.name }
                .toMutableList()
            list.add(0, scenario)
            prefs[presetsKey] = encodeScenarioList(list.take(30))
        }
    }

    suspend fun deletePreset(name: String) {
        context.probeDataStore.edit { prefs ->
            val list = decodeScenarioList(prefs[presetsKey] ?: "")
                .filterNot { it.name == name }
            prefs[presetsKey] = encodeScenarioList(list)
        }
    }

    private fun decodeScenario(raw: String): CableScenario? {
        if (raw.isBlank()) return null
        return runCatching { CableScenario.fromJson(JSONObject(raw)) }.getOrNull()
    }

    private fun decodeScenarioList(raw: String): List<CableScenario> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val s = CableScenario.fromJson(obj) ?: continue
                    add(s)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeScenarioList(list: List<CableScenario>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }
}
