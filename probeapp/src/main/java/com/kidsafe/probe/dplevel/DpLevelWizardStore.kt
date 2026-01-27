package com.kidsafe.probe.dplevel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kidsafe.probe.probeDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DpLevelWizardDraft(
    val currentStep: Int,
    val completedStep: Int,
    val instrument: DpLevelInstrument,
    val mode: DpLevelMode,
    val zeroShiftDirection: DpZeroShiftDirection,
    val dpUnit: PressureUnit,
    val heightUnit: HeightUnit,
    val outputUnit: PressureUnit,
    val spanHeight: Double,
    val zeroShift: Double,
    val h2: Double,
    val h3: Double,
    val mediumDensity: Double,
    val oilDensity: Double,
    val dpLrv: Double,
    val dpUrv: Double,
    val levelPercent: Double,
    val dpNow: Double,
) {
    val oilEquivalentHeight: Double get() = h2 - h3
}

class DpLevelWizardStore(private val context: Context) {
    private val keyDraft = stringPreferencesKey("dp_level_wizard_v1")

    val draft: Flow<DpLevelWizardDraft> = context.probeDataStore.data.map { prefs ->
        decode(prefs[keyDraft].orEmpty()) ?: defaultDraft()
    }

    suspend fun save(draft: DpLevelWizardDraft) {
        context.probeDataStore.edit { prefs ->
            prefs[keyDraft] = encode(draft)
        }
    }

    suspend fun clear() {
        context.probeDataStore.edit { prefs ->
            prefs.remove(keyDraft)
        }
    }
}

fun dpLevelWizardDefaultDraft(): DpLevelWizardDraft = defaultDraft()

private fun defaultDraft(): DpLevelWizardDraft {
    return DpLevelWizardDraft(
        currentStep = 0,
        completedStep = -1,
        instrument = DpLevelInstrument.DP,
        mode = DpLevelMode.RHO_AND_HEIGHT,
        zeroShiftDirection = DpZeroShiftDirection.NEGATIVE,
        dpUnit = PressureUnit.KPA,
        heightUnit = HeightUnit.M,
        outputUnit = PressureUnit.KPA,
        spanHeight = 1.0,
        zeroShift = 0.0,
        h2 = 0.0,
        h3 = 0.0,
        mediumDensity = 1000.0,
        oilDensity = 950.0,
        dpLrv = 0.0,
        dpUrv = 100.0,
        levelPercent = 50.0,
        dpNow = 0.0,
    )
}

private fun encode(d: DpLevelWizardDraft): String {
    fun line(k: String, v: String) = "$k=$v"
    return listOf(
        line("currentStep", d.currentStep.toString()),
        line("completedStep", d.completedStep.toString()),
        line("instrument", d.instrument.name),
        line("mode", d.mode.name),
        line("zeroShiftDirection", d.zeroShiftDirection.name),
        line("dpUnit", d.dpUnit.name),
        line("heightUnit", d.heightUnit.name),
        line("outputUnit", d.outputUnit.name),
        line("spanHeight", d.spanHeight.toString()),
        line("zeroShift", d.zeroShift.toString()),
        line("h2", d.h2.toString()),
        line("h3", d.h3.toString()),
        line("mediumDensity", d.mediumDensity.toString()),
        line("oilDensity", d.oilDensity.toString()),
        line("dpLrv", d.dpLrv.toString()),
        line("dpUrv", d.dpUrv.toString()),
        line("levelPercent", d.levelPercent.toString()),
        line("dpNow", d.dpNow.toString()),
    ).joinToString("\n")
}

private fun decode(raw: String): DpLevelWizardDraft? {
    if (raw.isBlank()) return null
    val map = raw.lines()
        .mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val k = line.substring(0, idx).trim()
            val v = line.substring(idx + 1).trim()
            if (k.isBlank()) return@mapNotNull null
            k to v
        }
        .toMap()

    fun int(k: String, def: Int): Int = map[k]?.toIntOrNull() ?: def
    fun dbl(k: String, def: Double): Double = map[k]?.toDoubleOrNull() ?: def
    fun <T : Enum<T>> enum(k: String, def: T, values: Array<T>): T {
        val rawVal = map[k] ?: return def
        return values.firstOrNull { it.name == rawVal } ?: def
    }

    val def = defaultDraft()
    val decodedMode = run {
        val rawVal = map["mode"] ?: return@run def.mode
        when (rawVal) {
            "DP_AND_HEIGHT" -> DpLevelMode.RECALC_RANGE
            else -> DpLevelMode.entries.firstOrNull { it.name == rawVal } ?: def.mode
        }
    }
    return DpLevelWizardDraft(
        currentStep = int("currentStep", def.currentStep).coerceIn(0, 4),
        completedStep = int("completedStep", def.completedStep).coerceIn(-1, 4),
        instrument = enum("instrument", def.instrument, DpLevelInstrument.entries.toTypedArray()),
        mode = decodedMode,
        zeroShiftDirection = enum("zeroShiftDirection", def.zeroShiftDirection, DpZeroShiftDirection.entries.toTypedArray()),
        dpUnit = enum("dpUnit", def.dpUnit, PressureUnit.entries.toTypedArray()),
        heightUnit = enum("heightUnit", def.heightUnit, HeightUnit.entries.toTypedArray()),
        outputUnit = enum("outputUnit", def.outputUnit, PressureUnit.entries.toTypedArray()),
        spanHeight = dbl("spanHeight", def.spanHeight),
        zeroShift = dbl("zeroShift", def.zeroShift),
        h2 = dbl("h2", def.h2),
        h3 = dbl("h3", def.h3),
        mediumDensity = dbl("mediumDensity", def.mediumDensity),
        oilDensity = dbl("oilDensity", def.oilDensity),
        dpLrv = dbl("dpLrv", def.dpLrv),
        dpUrv = dbl("dpUrv", def.dpUrv),
        levelPercent = dbl("levelPercent", def.levelPercent),
        dpNow = dbl("dpNow", def.dpNow),
    )
}
