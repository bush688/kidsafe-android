package com.kidsafe.probe.dplevel

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.ModuleInputStore
import com.kidsafe.probe.R
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpLevelModule(
    modifier: Modifier = Modifier,
    onCopy: suspend (String) -> Unit,
    onMessage: suspend (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val historyStore = remember(appContext) { DpLevelHistoryStore(appContext) }
    val history by historyStore.history.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val wizardStore = remember(appContext) { DpLevelWizardStore(appContext) }
    val wizardDraft by wizardStore.draft.collectAsState(initial = dpLevelWizardDefaultDraft())
    val inputStore = remember(appContext) { ModuleInputStore(appContext) }
    var showWizard by remember { mutableStateOf(false) }

    var instrument by remember { mutableStateOf(DpLevelInstrument.DP) }
    var mode by remember { mutableStateOf(DpLevelMode.RHO_AND_HEIGHT) }
    var zeroShiftDirection by remember { mutableStateOf(DpZeroShiftDirection.NEGATIVE) }

    var mediumDensityText by remember { mutableStateOf("1000") }
    var oilDensityText by remember { mutableStateOf("950") }

    var spanHeightText by remember { mutableStateOf("1") }
    var spanHeightUnit by remember { mutableStateOf(HeightUnit.M) }

    var zeroShiftText by remember { mutableStateOf("0") }
    var oilHeightText by remember { mutableStateOf("0") }

    var lrvText by remember { mutableStateOf("0") }
    var urvText by remember { mutableStateOf("100") }
    var levelPercentText by remember { mutableStateOf("50") }
    var dpNowText by remember { mutableStateOf("0") }
    var dpUnit by remember { mutableStateOf(PressureUnit.KPA) }

    var outputUnit by remember { mutableStateOf(PressureUnit.KPA) }

    var errorText by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<DpLevelResult?>(null) }
    var calibrationDetails by remember { mutableStateOf<String?>(null) }
    var inputsLoaded by remember { mutableStateOf(false) }

    var recalcPointMode by remember { mutableStateOf("multi") }
    var recalcFitSpan by remember { mutableStateOf(true) }
    val recalcPoints = remember {
        mutableStateListOf(
            CalPointState("10", "0"),
            CalPointState("25", "0"),
            CalPointState("80", "0"),
        )
    }

    LaunchedEffect(Unit) {
        val obj = inputStore.load("dp_level")
        if (obj != null) {
            runCatching { DpLevelInstrument.valueOf(obj.optString("instrument", instrument.name)) }.getOrNull()?.let { instrument = it }
            runCatching { DpLevelMode.valueOf(obj.optString("mode", mode.name)) }.getOrNull()?.let { mode = it }
            runCatching { DpZeroShiftDirection.valueOf(obj.optString("zeroShiftDirection", zeroShiftDirection.name)) }.getOrNull()?.let { zeroShiftDirection = it }
            mediumDensityText = obj.optString("mediumDensityText", mediumDensityText)
            oilDensityText = obj.optString("oilDensityText", oilDensityText)
            spanHeightText = obj.optString("spanHeightText", spanHeightText)
            runCatching { HeightUnit.valueOf(obj.optString("spanHeightUnit", spanHeightUnit.name)) }.getOrNull()?.let { spanHeightUnit = it }
            zeroShiftText = obj.optString("zeroShiftText", zeroShiftText)
            oilHeightText = obj.optString("oilHeightText", oilHeightText)
            lrvText = obj.optString("lrvText", lrvText)
            urvText = obj.optString("urvText", urvText)
            levelPercentText = obj.optString("levelPercentText", levelPercentText)
            dpNowText = obj.optString("dpNowText", dpNowText)
            runCatching { PressureUnit.valueOf(obj.optString("dpUnit", dpUnit.name)) }.getOrNull()?.let { dpUnit = it }
            runCatching { PressureUnit.valueOf(obj.optString("outputUnit", outputUnit.name)) }.getOrNull()?.let { outputUnit = it }
            recalcPointMode = obj.optString("recalcPointMode", recalcPointMode).ifBlank { recalcPointMode }
            recalcFitSpan = obj.optBoolean("recalcFitSpan", recalcFitSpan)
            val arr = obj.optJSONArray("recalcPoints")
            if (arr != null) {
                recalcPoints.clear()
                for (i in 0 until arr.length()) {
                    val pObj = arr.optJSONObject(i) ?: continue
                    val pText = pObj.optString("p", "")
                    val dpText = pObj.optString("dp", "")
                    if (pText.isNotBlank() || dpText.isNotBlank()) {
                        recalcPoints.add(CalPointState(pText.ifBlank { "0" }, dpText.ifBlank { "0" }))
                    }
                }
                if (recalcPoints.isEmpty()) {
                    recalcPoints.add(CalPointState(levelPercentText, dpNowText))
                }
            }
        }
        inputsLoaded = true
    }

    LaunchedEffect(
        instrument,
        mode,
        zeroShiftDirection,
        mediumDensityText,
        oilDensityText,
        spanHeightText,
        spanHeightUnit,
        zeroShiftText,
        oilHeightText,
        lrvText,
        urvText,
        levelPercentText,
        dpNowText,
        dpUnit,
        outputUnit,
        recalcPointMode,
        recalcFitSpan,
        recalcPoints.size,
        inputsLoaded,
    ) {
        if (!inputsLoaded) return@LaunchedEffect
        val pointsArr = JSONArray()
        recalcPoints.forEach { p ->
            pointsArr.put(
                JSONObject()
                    .put("p", p.percentText)
                    .put("dp", p.dpText)
            )
        }
        inputStore.save(
            "dp_level",
            JSONObject()
                .put("instrument", instrument.name)
                .put("mode", mode.name)
                .put("zeroShiftDirection", zeroShiftDirection.name)
                .put("mediumDensityText", mediumDensityText)
                .put("oilDensityText", oilDensityText)
                .put("spanHeightText", spanHeightText)
                .put("spanHeightUnit", spanHeightUnit.name)
                .put("zeroShiftText", zeroShiftText)
                .put("oilHeightText", oilHeightText)
                .put("lrvText", lrvText)
                .put("urvText", urvText)
                .put("levelPercentText", levelPercentText)
                .put("dpNowText", dpNowText)
                .put("dpUnit", dpUnit.name)
                .put("outputUnit", outputUnit.name)
                .put("recalcPointMode", recalcPointMode)
                .put("recalcFitSpan", recalcFitSpan)
                .put("recalcPoints", pointsArr)
        )
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = context.getString(R.string.dp_level_title), style = MaterialTheme.typography.titleMedium)
                SecondaryButton(
                    title = context.getString(R.string.dp_level_wizard_open),
                    onClick = { showWizard = true },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChoiceButton(
                    title = context.getString(R.string.dp_level_instrument_dp),
                    hint = context.getString(R.string.hint_dp_level_instrument_dp),
                    selected = instrument == DpLevelInstrument.DP,
                    onClick = {
                        instrument = DpLevelInstrument.DP
                        result = null
                        errorText = null
                    },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    title = context.getString(R.string.dp_level_instrument_dual),
                    hint = context.getString(R.string.hint_dp_level_instrument_dual),
                    selected = instrument == DpLevelInstrument.DUAL_FLANGE,
                    onClick = {
                        instrument = DpLevelInstrument.DUAL_FLANGE
                        result = null
                        errorText = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChoiceButton(
                    title = context.getString(R.string.dp_level_mode_rho_height),
                    hint = context.getString(R.string.hint_dp_level_mode_rho_height),
                    selected = mode == DpLevelMode.RHO_AND_HEIGHT,
                    onClick = {
                        mode = DpLevelMode.RHO_AND_HEIGHT
                        result = null
                        errorText = null
                    },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    title = context.getString(R.string.dp_level_mode_recalc),
                    hint = context.getString(R.string.hint_dp_level_mode_recalc),
                    selected = mode == DpLevelMode.RECALC_RANGE,
                    onClick = {
                        mode = DpLevelMode.RECALC_RANGE
                        result = null
                        errorText = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (mode == DpLevelMode.RHO_AND_HEIGHT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChoiceButton(
                        title = context.getString(R.string.dp_level_shift_negative),
                        hint = context.getString(R.string.hint_dp_level_shift_negative),
                        selected = zeroShiftDirection == DpZeroShiftDirection.NEGATIVE,
                        onClick = { zeroShiftDirection = DpZeroShiftDirection.NEGATIVE; errorText = null; result = null },
                        modifier = Modifier.weight(1f),
                    )
                    ChoiceButton(
                        title = context.getString(R.string.dp_level_shift_positive),
                        hint = context.getString(R.string.hint_dp_level_shift_positive),
                        selected = zeroShiftDirection == DpZeroShiftDirection.POSITIVE,
                        onClick = { zeroShiftDirection = DpZeroShiftDirection.POSITIVE; errorText = null; result = null },
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = mediumDensityText,
                    onValueChange = { mediumDensityText = filterNumber(it, allowNegative = false); errorText = null; result = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = context.getString(R.string.dp_level_medium_density)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(text = context.getString(R.string.unit_kg_m3)) },
                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_medium_density)) },
                )

                HeightInputRow(
                    title = context.getString(R.string.dp_level_span_height),
                    hint = context.getString(R.string.hint_dp_level_span_height),
                    valueText = spanHeightText,
                    onValueTextChange = { spanHeightText = it; errorText = null; result = null },
                    unit = spanHeightUnit,
                    onUnitChange = { spanHeightUnit = it; errorText = null; result = null },
                    allowNegative = false,
                )

                HeightInputRow(
                    title = context.getString(R.string.dp_level_zero_shift_height),
                    hint = context.getString(R.string.hint_dp_level_zero_shift_height2),
                    valueText = zeroShiftText,
                    onValueTextChange = { zeroShiftText = it; errorText = null; result = null },
                    unit = spanHeightUnit,
                    onUnitChange = { spanHeightUnit = it; errorText = null; result = null },
                    allowNegative = false,
                )

                if (instrument == DpLevelInstrument.DUAL_FLANGE) {
                    OutlinedTextField(
                        value = oilDensityText,
                        onValueChange = { oilDensityText = filterNumber(it, allowNegative = false); errorText = null; result = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.dp_level_oil_density)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = context.getString(R.string.unit_kg_m3)) },
                        supportingText = { Text(text = context.getString(R.string.hint_dp_level_oil_density)) },
                    )

                    HeightInputRow(
                        title = context.getString(R.string.dp_level_oil_equiv_height),
                        hint = context.getString(R.string.hint_dp_level_oil_equiv_height2),
                        valueText = oilHeightText,
                        onValueTextChange = { oilHeightText = it; errorText = null; result = null },
                        unit = spanHeightUnit,
                        onUnitChange = { spanHeightUnit = it; errorText = null; result = null },
                        allowNegative = false,
                    )
                }
            } else {
                PressureInputRow(
                    title = context.getString(R.string.dp_level_lrv),
                    hint = context.getString(R.string.hint_dp_level_lrv),
                    valueText = lrvText,
                    onValueTextChange = { lrvText = it; errorText = null; result = null },
                    unit = dpUnit,
                    onUnitChange = { dpUnit = it; errorText = null; result = null },
                    allowNegative = true,
                )
                PressureInputRow(
                    title = context.getString(R.string.dp_level_urv),
                    hint = context.getString(R.string.hint_dp_level_urv),
                    valueText = urvText,
                    onValueTextChange = { urvText = it; errorText = null; result = null },
                    unit = dpUnit,
                    onUnitChange = { dpUnit = it; errorText = null; result = null },
                    allowNegative = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChoiceButton(
                        title = context.getString(R.string.dp_level_recalc_single),
                        hint = context.getString(R.string.hint_dp_level_recalc_single),
                        selected = recalcPointMode == "single",
                        onClick = {
                            recalcPointMode = "single"
                            calibrationDetails = null
                            result = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        title = context.getString(R.string.dp_level_recalc_multi),
                        hint = context.getString(R.string.hint_dp_level_recalc_multi),
                        selected = recalcPointMode == "multi",
                        onClick = {
                            recalcPointMode = "multi"
                            if (recalcPoints.isEmpty()) {
                                recalcPoints.add(CalPointState(levelPercentText, dpNowText))
                            }
                            calibrationDetails = null
                            result = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (recalcPointMode == "single") {
                    OutlinedTextField(
                        value = levelPercentText,
                        onValueChange = { levelPercentText = filterNumber(it, allowNegative = false); errorText = null; result = null; calibrationDetails = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.dp_level_level_percent)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = "%") },
                        supportingText = { Text(text = context.getString(R.string.hint_dp_level_level_percent)) },
                    )
                    PressureInputRow(
                        title = context.getString(R.string.dp_level_dp_now),
                        hint = context.getString(R.string.hint_dp_level_dp_now),
                        valueText = dpNowText,
                        onValueTextChange = { dpNowText = it; errorText = null; result = null; calibrationDetails = null },
                        unit = dpUnit,
                        onUnitChange = { dpUnit = it; errorText = null; result = null; calibrationDetails = null },
                        allowNegative = true,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ChoiceButton(
                            title = context.getString(R.string.dp_level_recalc_fit_span),
                            hint = context.getString(R.string.hint_dp_level_recalc_fit_span),
                            selected = recalcFitSpan,
                            onClick = {
                                recalcFitSpan = true
                                calibrationDetails = null
                                result = null
                                errorText = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ChoiceButton(
                            title = context.getString(R.string.dp_level_recalc_keep_span),
                            hint = context.getString(R.string.hint_dp_level_recalc_keep_span),
                            selected = !recalcFitSpan,
                            onClick = {
                                recalcFitSpan = false
                                calibrationDetails = null
                                result = null
                                errorText = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = context.getString(R.string.hint_dp_level_recalc_points, dpUnit.displayName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    recalcPoints.forEachIndexed { index, p ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = p.percentText,
                                onValueChange = { next ->
                                    p.percentText = filterNumber(next, allowNegative = false)
                                    calibrationDetails = null
                                    result = null
                                    errorText = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(text = context.getString(R.string.dp_level_point_percent, index + 1)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = "%") },
                            )
                            OutlinedTextField(
                                value = p.dpText,
                                onValueChange = { next ->
                                    p.dpText = filterNumber(next, allowNegative = true)
                                    calibrationDetails = null
                                    result = null
                                    errorText = null
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(text = context.getString(R.string.dp_level_point_dp, index + 1)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = dpUnit.displayName) },
                            )
                            IconButton(
                                onClick = {
                                    if (recalcPoints.size > 1) {
                                        recalcPoints.removeAt(index)
                                        calibrationDetails = null
                                        result = null
                                        errorText = null
                                    }
                                },
                                enabled = recalcPoints.size > 1
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.delete))
                            }
                        }
                    }

                    SecondaryButton(
                        title = context.getString(R.string.dp_level_add_point),
                        onClick = {
                            recalcPoints.add(CalPointState("0", "0"))
                            calibrationDetails = null
                            result = null
                            errorText = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SecondaryButton(
                        title = context.getString(R.string.calculate),
                        onClick = {
                            try {
                                val spanHeightM = spanHeightUnit.toM(spanHeightText.toDoubleOrNull() ?: Double.NaN)
                                val zeroShiftM = spanHeightUnit.toM(zeroShiftText.toDoubleOrNull() ?: Double.NaN)
                                val oilHeightM = spanHeightUnit.toM(oilHeightText.toDoubleOrNull() ?: 0.0)
                                val oilDensity = oilDensityText.toDoubleOrNull() ?: 950.0
                                val mediumDensity = mediumDensityText.toDoubleOrNull()

                                val lrvPa = dpUnit.toPa(lrvText.toDoubleOrNull() ?: Double.NaN)
                                val urvPa = dpUnit.toPa(urvText.toDoubleOrNull() ?: Double.NaN)
                                val levelPercent = levelPercentText.toDoubleOrNull()
                                val dpNowPa = dpUnit.toPa(dpNowText.toDoubleOrNull() ?: Double.NaN)

                                val out: DpLevelResult
                                if (mode == DpLevelMode.RECALC_RANGE && recalcPointMode == "multi") {
                                    val points = recalcPoints.mapNotNull { p ->
                                        val pct = p.percentText.toDoubleOrNull()
                                        val dp = p.dpText.toDoubleOrNull()
                                        if (pct == null || dp == null) null else DpLevelCalculator.CalPoint(pct, dpUnit.toPa(dp))
                                    }
                                    val fit = DpLevelCalculator.fitRangeFromPoints(
                                        lrv0Pa = lrvPa,
                                        urv0Pa = urvPa,
                                        points = points,
                                        keepSpan = !recalcFitSpan,
                                    )
                                    out = DpLevelResult(
                                        mediumDensityKgM3 = Double.NaN,
                                        lrvPa = fit.lrvPa,
                                        urvPa = fit.urvPa,
                                        spanPa = fit.spanPa,
                                    )
                                    if (fit.lrvPa.isFinite() && fit.urvPa.isFinite() && fit.spanPa.isFinite()) {
                                        val rmse = formatPressure(outputUnit.fromPa(fit.rmsePa), outputUnit)
                                        val maxAbs = formatPressure(outputUnit.fromPa(fit.maxAbsErrorPa), outputUnit)
                                        calibrationDetails = context.getString(R.string.dp_level_fit_stats, rmse, maxAbs)
                                    } else {
                                        calibrationDetails = null
                                    }
                                } else {
                                    out = DpLevelCalculator.compute(
                                        DpLevelInput(
                                            instrument = instrument,
                                            mode = mode,
                                            mediumDensityKgM3 = mediumDensity,
                                            oilDensityKgM3 = oilDensity,
                                            spanHeightM = spanHeightM,
                                            zeroShiftDirection = zeroShiftDirection,
                                            zeroShiftMediumM = zeroShiftM,
                                            oilEquivalentHeightM = oilHeightM,
                                            dpLrvPa = if (mode == DpLevelMode.RECALC_RANGE) lrvPa else null,
                                            dpUrvPa = if (mode == DpLevelMode.RECALC_RANGE) urvPa else null,
                                            levelPercent = if (mode == DpLevelMode.RECALC_RANGE) levelPercent else null,
                                            dpNowPa = if (mode == DpLevelMode.RECALC_RANGE) dpNowPa else null,
                                        )
                                    )
                                    calibrationDetails = null
                                }
                                val invalid = out.lrvPa.isNaN() || out.urvPa.isNaN() || out.spanPa.isNaN()
                                if (invalid) {
                                    errorText = context.getString(R.string.invalid_input)
                                    result = null
                                    calibrationDetails = null
                                    return@SecondaryButton
                                }
                                result = out
                                errorText = null

                                val record = buildHistoryLine(
                                    context = context,
                                    instrument = instrument,
                                    mode = mode,
                                    out = out,
                                    outputUnit = outputUnit,
                                    spanHeightM = spanHeightM,
                                    zeroShiftM = zeroShiftM,
                                    levelPercent = if (mode == DpLevelMode.RECALC_RANGE && recalcPointMode == "multi") null else levelPercent,
                                    dpNowPa = dpNowPa,
                                    calibrationDetails = calibrationDetails,
                                )
                                scope.launch { historyStore.add(record) }
                            } catch (e: Exception) {
                                Log.e("DpLevelModule", "Calc failed", e)
                                errorText = context.getString(R.string.calc_failed)
                                result = null
                                calibrationDetails = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        title = context.getString(R.string.restore_defaults),
                        onClick = {
                            instrument = DpLevelInstrument.DP
                            mode = DpLevelMode.RHO_AND_HEIGHT
                            zeroShiftDirection = DpZeroShiftDirection.NEGATIVE
                            mediumDensityText = "1000"
                            oilDensityText = "950"
                            spanHeightText = "1"
                            spanHeightUnit = HeightUnit.M
                            zeroShiftText = "0"
                            oilHeightText = "0"
                            lrvText = "0"
                            urvText = "100"
                            levelPercentText = "50"
                            dpNowText = "0"
                            dpUnit = PressureUnit.KPA
                            outputUnit = PressureUnit.KPA
                            errorText = null
                            result = null
                            calibrationDetails = null
                            recalcPointMode = "multi"
                            recalcFitSpan = true
                            recalcPoints.clear()
                            recalcPoints.addAll(
                                listOf(
                                    CalPointState("10", "0"),
                                    CalPointState("25", "0"),
                                    CalPointState("80", "0"),
                                )
                            )
                            scope.launch {
                                inputStore.clear("dp_level")
                                onMessage(context.getString(R.string.restored_defaults))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = context.getString(R.string.hint_calculate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val r = result ?: return@IconButton
                            val text = buildCopyText(context, instrument, mode, r, outputUnit, calibrationDetails)
                            scope.launch { onCopy(text) }
                        },
                        enabled = result != null
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = context.getString(R.string.copy))
                    }
                    Text(
                        text = context.getString(R.string.hint_copy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            UnitOutputRow(
                title = context.getString(R.string.dp_level_output_unit),
                hint = context.getString(R.string.hint_dp_level_output_unit),
                unit = outputUnit,
                onUnitChange = { outputUnit = it },
            )

            val err = errorText
            if (err != null) {
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }

            val r = result
            ResultRow(
                title = context.getString(R.string.dp_level_lrv_out),
                value = r?.let { formatPressure(outputUnit.fromPa(it.lrvPa), outputUnit) }
            )
            ResultRow(
                title = context.getString(R.string.dp_level_urv_out),
                value = r?.let { formatPressure(outputUnit.fromPa(it.urvPa), outputUnit) }
            )
            ResultRow(
                title = context.getString(R.string.dp_level_span_kpa),
                value = r?.let { formatPressure(outputUnit.fromPa(it.spanPa), outputUnit) }
            )
            ResultRow(
                title = context.getString(R.string.dp_level_density_out),
                value = r?.let {
                    if (it.mediumDensityKgM3.isNaN()) "—" else "${formatNumber4(it.mediumDensityKgM3)} ${context.getString(R.string.unit_kg_m3)}"
                }
            )
            ResultRow(
                title = context.getString(R.string.dp_level_fit_detail_title),
                value = calibrationDetails,
            )

            HistoryCard(
                history = history,
                onClear = { scope.launch { historyStore.clear() } },
            )
        }
    }

    if (showWizard) {
        DpLevelWizardDialog(
            initialDraft = wizardDraft,
            store = wizardStore,
            onDismiss = { showWizard = false },
            onApply = { apply ->
                showWizard = false
                instrument = apply.instrument
                mode = apply.mode
                dpUnit = apply.dpUnit
                spanHeightUnit = apply.heightUnit
                outputUnit = apply.outputUnit
                zeroShiftDirection = apply.zeroShiftDirection

                spanHeightText = apply.spanHeight.toString()
                zeroShiftText = kotlin.math.abs(apply.zeroShift).toString()
                oilDensityText = apply.oilDensity.toString()
                oilHeightText = kotlin.math.abs(apply.oilEquivalentHeight).toString()

                if (apply.mode == DpLevelMode.RHO_AND_HEIGHT) {
                    mediumDensityText = (apply.mediumDensity ?: 1000.0).toString()
                } else {
                    lrvText = (apply.lrv ?: 0.0).toString()
                    urvText = (apply.urv ?: 0.0).toString()
                    levelPercentText = (apply.levelPercent ?: 0.0).toString()
                    dpNowText = (apply.dpNow ?: 0.0).toString()
                }

                result = null
                errorText = null
            }
        )
    }
}

@Composable
private fun ResultRow(title: String, value: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title)
            Text(text = value ?: "—")
        }
    }
}

@Composable
private fun HistoryCard(
    history: List<DpLevelHistoryRecord>,
    onClear: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = context.getString(R.string.history), style = MaterialTheme.typography.titleSmall)
            SecondaryButton(title = context.getString(R.string.clear_history), onClick = onClear)
        }
        if (history.isEmpty()) {
            Text(text = context.getString(R.string.empty_placeholder))
        } else {
            history.forEach { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = record.text,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitOutputRow(
    title: String,
    hint: String,
    unit: PressureUnit,
    onUnitChange: (PressureUnit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = unit.displayName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            label = { Text(text = title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = { Text(text = hint) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PressureUnit.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(text = opt.displayName) },
                    onClick = {
                        expanded = false
                        onUnitChange(opt)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeightInputRow(
    title: String,
    hint: String,
    valueText: String,
    onValueTextChange: (String) -> Unit,
    unit: HeightUnit,
    onUnitChange: (HeightUnit) -> Unit,
    allowNegative: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = valueText,
            onValueChange = { onValueTextChange(filterNumber(it, allowNegative = allowNegative)) },
            modifier = Modifier.weight(1f),
            label = { Text(text = title) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = { Text(text = hint) },
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(0.7f),
        ) {
            OutlinedTextField(
                value = unit.displayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text(text = contextUnitLabel(title)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                HeightUnit.entries.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(text = opt.displayName) },
                        onClick = {
                            expanded = false
                            onUnitChange(opt)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PressureInputRow(
    title: String,
    hint: String,
    valueText: String,
    onValueTextChange: (String) -> Unit,
    unit: PressureUnit,
    onUnitChange: (PressureUnit) -> Unit,
    allowNegative: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = valueText,
            onValueChange = { onValueTextChange(filterNumber(it, allowNegative = allowNegative)) },
            modifier = Modifier.weight(1f),
            label = { Text(text = title) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = { Text(text = hint) },
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(0.7f),
        ) {
            OutlinedTextField(
                value = unit.displayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text(text = contextUnitLabel(title)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PressureUnit.entries.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(text = opt.displayName) },
                        onClick = {
                            expanded = false
                            onUnitChange(opt)
                        }
                    )
                }
            }
        }
    }
}

private fun contextUnitLabel(title: String): String = "单位"

private fun filterNumber(input: String, allowNegative: Boolean): String {
    val out = StringBuilder()
    for (i in input.indices) {
        val ch = input[i]
        if (ch.isDigit()) out.append(ch)
        if (ch == '.' && out.count { it == '.' } == 0) out.append(ch)
        if (allowNegative && ch == '-' && i == 0 && out.isEmpty()) out.append(ch)
    }
    return out.toString()
}

private fun formatNumber4(v: Double): String = String.format(Locale.US, "%.4f", v)

private fun formatPressure(v: Double, unit: PressureUnit): String {
    val digits = when (unit) {
        PressureUnit.PA -> 0
        PressureUnit.MMH2O -> 1
        PressureUnit.BAR -> 5
        PressureUnit.KPA -> 3
    }
    return String.format(Locale.US, "%.${digits}f %s", v, unit.displayName)
}

private fun buildCopyText(
    context: android.content.Context,
    instrument: DpLevelInstrument,
    mode: DpLevelMode,
    out: DpLevelResult,
    unit: PressureUnit,
    calibrationDetails: String?,
): String {
    val inst = if (instrument == DpLevelInstrument.DUAL_FLANGE) context.getString(R.string.dp_level_instrument_dual) else context.getString(R.string.dp_level_instrument_dp)
    val modeName = if (mode == DpLevelMode.RHO_AND_HEIGHT) context.getString(R.string.dp_level_mode_rho_height) else context.getString(R.string.dp_level_mode_recalc)
    return buildString {
        appendLine("${context.getString(R.string.dp_level_title)}：$inst / $modeName")
        if (!out.mediumDensityKgM3.isNaN()) {
            appendLine("${context.getString(R.string.dp_level_density_out)}：${formatNumber4(out.mediumDensityKgM3)} ${context.getString(R.string.unit_kg_m3)}")
        }
        appendLine("${context.getString(R.string.dp_level_lrv_out)}：${formatPressure(unit.fromPa(out.lrvPa), unit)}")
        appendLine("${context.getString(R.string.dp_level_urv_out)}：${formatPressure(unit.fromPa(out.urvPa), unit)}")
        appendLine("${context.getString(R.string.dp_level_span_kpa)}：${formatPressure(unit.fromPa(out.spanPa), unit)}")
        if (!calibrationDetails.isNullOrBlank()) {
            appendLine("${context.getString(R.string.dp_level_fit_detail_title)}：$calibrationDetails")
        }
    }.trim()
}

private fun buildHistoryLine(
    context: android.content.Context,
    instrument: DpLevelInstrument,
    mode: DpLevelMode,
    out: DpLevelResult,
    outputUnit: PressureUnit,
    spanHeightM: Double,
    zeroShiftM: Double,
    levelPercent: Double?,
    dpNowPa: Double,
    calibrationDetails: String?,
): String {
    val instShort = if (instrument == DpLevelInstrument.DUAL_FLANGE) "双法兰" else "差压"
    val modeShort = if (mode == DpLevelMode.RHO_AND_HEIGHT) "ρ+H" else "校准"
    return if (mode == DpLevelMode.RHO_AND_HEIGHT) {
        "$instShort/$modeShort  H=${formatNumber4(spanHeightM)}m  h0=${formatNumber4(zeroShiftM)}m  ρ=${formatNumber4(out.mediumDensityKgM3)}  LRV=${formatPressure(outputUnit.fromPa(out.lrvPa), outputUnit)}  URV=${formatPressure(outputUnit.fromPa(out.urvPa), outputUnit)}"
    } else {
        val pct = levelPercent ?: Double.NaN
        val head = if (pct.isNaN()) "$instShort/$modeShort  多点" else "$instShort/$modeShort  %=${formatNumber4(pct)}"
        buildString {
            append(head)
            append("  ΔPnow=${formatPressure(outputUnit.fromPa(dpNowPa), outputUnit)}")
            append("  LRV=${formatPressure(outputUnit.fromPa(out.lrvPa), outputUnit)}")
            append("  URV=${formatPressure(outputUnit.fromPa(out.urvPa), outputUnit)}")
            if (!calibrationDetails.isNullOrBlank()) append("  $calibrationDetails")
        }
    }
}

private class CalPointState(
    percentText: String,
    dpText: String,
) {
    var percentText by mutableStateOf(percentText)
    var dpText by mutableStateOf(dpText)
}
