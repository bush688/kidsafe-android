package com.kidsafe.probe.dplevel

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.R
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpLevelModule(
    modifier: Modifier = Modifier,
    onCopy: suspend (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val historyStore = remember(appContext) { DpLevelHistoryStore(appContext) }
    val history by historyStore.history.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val wizardStore = remember(appContext) { DpLevelWizardStore(appContext) }
    val wizardDraft by wizardStore.draft.collectAsState(initial = dpLevelWizardDefaultDraft())
    var showWizard by rememberSaveable { mutableStateOf(false) }

    var instrument by rememberSaveable { mutableStateOf(DpLevelInstrument.DP) }
    var mode by rememberSaveable { mutableStateOf(DpLevelMode.RHO_AND_HEIGHT) }
    var zeroShiftDirection by rememberSaveable { mutableStateOf(DpZeroShiftDirection.NEGATIVE) }

    var mediumDensityText by rememberSaveable { mutableStateOf("1000") }
    var oilDensityText by rememberSaveable { mutableStateOf("950") }

    var spanHeightText by rememberSaveable { mutableStateOf("1") }
    var spanHeightUnit by rememberSaveable { mutableStateOf(HeightUnit.M) }

    var zeroShiftText by rememberSaveable { mutableStateOf("0") }
    var oilHeightText by rememberSaveable { mutableStateOf("0") }

    var lrvText by rememberSaveable { mutableStateOf("0") }
    var urvText by rememberSaveable { mutableStateOf("100") }
    var levelPercentText by rememberSaveable { mutableStateOf("50") }
    var dpNowText by rememberSaveable { mutableStateOf("0") }
    var dpUnit by rememberSaveable { mutableStateOf(PressureUnit.KPA) }

    var outputUnit by rememberSaveable { mutableStateOf(PressureUnit.KPA) }

    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var result by rememberSaveable { mutableStateOf<DpLevelResult?>(null) }

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
                OutlinedTextField(
                    value = levelPercentText,
                    onValueChange = { levelPercentText = filterNumber(it, allowNegative = false); errorText = null; result = null },
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
                    onValueTextChange = { dpNowText = it; errorText = null; result = null },
                    unit = dpUnit,
                    onUnitChange = { dpUnit = it; errorText = null; result = null },
                    allowNegative = true,
                )
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

                                val out = DpLevelCalculator.compute(
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
                                val invalid = out.lrvPa.isNaN() || out.urvPa.isNaN() || out.spanPa.isNaN()
                                if (invalid) {
                                    errorText = context.getString(R.string.invalid_input)
                                    result = null
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
                                    levelPercent = levelPercent,
                                    dpNowPa = dpNowPa,
                                )
                                scope.launch { historyStore.add(record) }
                            } catch (e: Exception) {
                                Log.e("DpLevelModule", "Calc failed", e)
                                errorText = context.getString(R.string.calc_failed)
                                result = null
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
                            val text = buildCopyText(context, instrument, mode, r, outputUnit)
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
    var expanded by rememberSaveable { mutableStateOf(false) }
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
    var expanded by rememberSaveable { mutableStateOf(false) }
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
    var expanded by rememberSaveable { mutableStateOf(false) }
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
): String {
    val instShort = if (instrument == DpLevelInstrument.DUAL_FLANGE) "双法兰" else "差压"
    val modeShort = if (mode == DpLevelMode.RHO_AND_HEIGHT) "ρ+H" else "校准"
    return if (mode == DpLevelMode.RHO_AND_HEIGHT) {
        "$instShort/$modeShort  H=${formatNumber4(spanHeightM)}m  h0=${formatNumber4(zeroShiftM)}m  ρ=${formatNumber4(out.mediumDensityKgM3)}  LRV=${formatPressure(outputUnit.fromPa(out.lrvPa), outputUnit)}  URV=${formatPressure(outputUnit.fromPa(out.urvPa), outputUnit)}"
    } else {
        val pct = levelPercent ?: Double.NaN
        "$instShort/$modeShort  %=${formatNumber4(pct)}  ΔPnow=${formatPressure(outputUnit.fromPa(dpNowPa), outputUnit)}  LRV=${formatPressure(outputUnit.fromPa(out.lrvPa), outputUnit)}  URV=${formatPressure(outputUnit.fromPa(out.urvPa), outputUnit)}"
    }
}
