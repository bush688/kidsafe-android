package com.kidsafe.probe.dplevel

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kidsafe.probe.R
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import java.util.Locale

data class DpLevelWizardApply(
    val instrument: DpLevelInstrument,
    val mode: DpLevelMode,
    val zeroShiftDirection: DpZeroShiftDirection,
    val dpUnit: PressureUnit,
    val heightUnit: HeightUnit,
    val outputUnit: PressureUnit,
    val spanHeight: Double,
    val zeroShift: Double,
    val oilDensity: Double,
    val oilEquivalentHeight: Double,
    val mediumDensity: Double?,
    val lrv: Double?,
    val urv: Double?,
    val levelPercent: Double?,
    val dpNow: Double?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpLevelWizardDialog(
    initialDraft: DpLevelWizardDraft?,
    store: DpLevelWizardStore,
    onDismiss: () -> Unit,
    onApply: (DpLevelWizardApply) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var draftLoaded by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(0) }
    var completedStep by remember { mutableStateOf(-1) }

    var instrument by remember { mutableStateOf(DpLevelInstrument.DP) }
    var mode by remember { mutableStateOf(DpLevelMode.RHO_AND_HEIGHT) }
    var zeroShiftDirection by remember { mutableStateOf(DpZeroShiftDirection.NEGATIVE) }
    var dpUnit by remember { mutableStateOf(PressureUnit.KPA) }
    var heightUnit by remember { mutableStateOf(HeightUnit.M) }
    var outputUnit by remember { mutableStateOf(PressureUnit.KPA) }

    var spanHeightText by remember { mutableStateOf("1") }
    var zeroShiftText by remember { mutableStateOf("0") }
    var h2Text by remember { mutableStateOf("0") }
    var h3Text by remember { mutableStateOf("0") }
    var mediumDensityText by remember { mutableStateOf("1000") }
    var oilDensityText by remember { mutableStateOf("950") }
    var lrvText by remember { mutableStateOf("0") }
    var urvText by remember { mutableStateOf("100") }
    var levelPercentText by remember { mutableStateOf("50") }
    var dpNowText by remember { mutableStateOf("0") }

    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialDraft) {
        if (!draftLoaded) {
            val d = initialDraft
            if (d != null) {
                currentStep = d.currentStep
                completedStep = d.completedStep
                instrument = d.instrument
                mode = d.mode
                zeroShiftDirection = d.zeroShiftDirection
                dpUnit = d.dpUnit
                heightUnit = d.heightUnit
                outputUnit = d.outputUnit
                spanHeightText = formatNumber(d.spanHeight)
                zeroShiftText = formatNumber(d.zeroShift)
                h2Text = formatNumber(d.h2)
                h3Text = formatNumber(d.h3)
                mediumDensityText = formatNumber(d.mediumDensity)
                oilDensityText = formatNumber(d.oilDensity)
                lrvText = formatNumber(d.dpLrv)
                urvText = formatNumber(d.dpUrv)
                levelPercentText = formatNumber(d.levelPercent)
                dpNowText = formatNumber(d.dpNow)
            }
            draftLoaded = true
        }
    }

    fun persist(step: Int = currentStep, completed: Int = completedStep) {
        val span = spanHeightText.toDoubleOrNull() ?: 0.0
        val zero = zeroShiftText.toDoubleOrNull() ?: 0.0
        val h2 = h2Text.toDoubleOrNull() ?: 0.0
        val h3 = h3Text.toDoubleOrNull() ?: 0.0
        val rho = mediumDensityText.toDoubleOrNull() ?: 0.0
        val oilRho = oilDensityText.toDoubleOrNull() ?: 950.0
        val lrv = lrvText.toDoubleOrNull() ?: 0.0
        val urv = urvText.toDoubleOrNull() ?: 0.0
        val pct = levelPercentText.toDoubleOrNull() ?: 0.0
        val dpNow = dpNowText.toDoubleOrNull() ?: 0.0
        scope.launch {
            store.save(
                DpLevelWizardDraft(
                    currentStep = step,
                    completedStep = completed,
                    instrument = instrument,
                    mode = mode,
                    zeroShiftDirection = zeroShiftDirection,
                    dpUnit = dpUnit,
                    heightUnit = heightUnit,
                    outputUnit = outputUnit,
                    spanHeight = span,
                    zeroShift = zero,
                    h2 = h2,
                    h3 = h3,
                    mediumDensity = rho,
                    oilDensity = oilRho,
                    dpLrv = lrv,
                    dpUrv = urv,
                    levelPercent = pct,
                    dpNow = dpNow,
                )
            )
        }
    }

    fun validateStep(step: Int): Boolean {
        errorText = null
        val span = spanHeightText.toDoubleOrNull()
        val zero = zeroShiftText.toDoubleOrNull()
        val h2 = h2Text.toDoubleOrNull()
        val h3 = h3Text.toDoubleOrNull()
        val rho = mediumDensityText.toDoubleOrNull()
        val oilRho = oilDensityText.toDoubleOrNull()
        val dpNow = dpNowText.toDoubleOrNull()
        val lrv = lrvText.toDoubleOrNull()
        val urv = urvText.toDoubleOrNull()
        val pct = levelPercentText.toDoubleOrNull()

        fun invalid(): Boolean {
            errorText = context.getString(R.string.invalid_input)
            return false
        }

        return when (step) {
            0 -> true
            1 -> true
            2 -> {
                if (mode == DpLevelMode.RHO_AND_HEIGHT) {
                    if (span == null) return invalid()
                    if (spanHeightUnitToM(span, heightUnit) <= 0.0) return invalid()
                    if (zero == null) return invalid()
                    if (instrument == DpLevelInstrument.DUAL_FLANGE) {
                        if (h2 == null || h3 == null) return invalid()
                    }
                } else {
                    if (lrv == null || urv == null) return invalid()
                    if (urv <= lrv) return invalid()
                    if (pct == null || pct < 0.0 || pct > 100.0) return invalid()
                    if (dpNow == null) return invalid()
                }
                true
            }
            3 -> {
                if (mode == DpLevelMode.RHO_AND_HEIGHT) {
                    if (rho == null || rho <= 0.0) return invalid()
                }
                if (instrument == DpLevelInstrument.DUAL_FLANGE) {
                    if (oilRho == null || oilRho <= 0.0) return invalid()
                }
                true
            }
            4 -> true
            else -> true
        }
    }

    fun apply() {
        try {
            val spanRaw = spanHeightText.toDoubleOrNull() ?: return
            val zeroRaw = zeroShiftText.toDoubleOrNull() ?: return
            val h2Raw = h2Text.toDoubleOrNull() ?: 0.0
            val h3Raw = h3Text.toDoubleOrNull() ?: 0.0
            val oilRho = oilDensityText.toDoubleOrNull() ?: 950.0
            val rho = mediumDensityText.toDoubleOrNull()
            val lrv = lrvText.toDoubleOrNull()
            val urv = urvText.toDoubleOrNull()
            val pct = levelPercentText.toDoubleOrNull()
            val dpNow = dpNowText.toDoubleOrNull()

            val apply = DpLevelWizardApply(
                instrument = instrument,
                mode = mode,
                zeroShiftDirection = zeroShiftDirection,
                dpUnit = dpUnit,
                heightUnit = heightUnit,
                outputUnit = outputUnit,
                spanHeight = spanRaw,
                zeroShift = zeroRaw,
                oilDensity = oilRho,
                oilEquivalentHeight = h2Raw - h3Raw,
                mediumDensity = if (mode == DpLevelMode.RHO_AND_HEIGHT) rho else null,
                lrv = if (mode == DpLevelMode.RECALC_RANGE) lrv else null,
                urv = if (mode == DpLevelMode.RECALC_RANGE) urv else null,
                levelPercent = if (mode == DpLevelMode.RECALC_RANGE) pct else null,
                dpNow = if (mode == DpLevelMode.RECALC_RANGE) dpNow else null,
            )
            persist(step = 4, completed = 4)
            onApply(apply)
        } catch (e: Exception) {
            Log.e("DpLevelWizard", "Apply failed", e)
            errorText = context.getString(R.string.calc_failed)
        }
    }

    Dialog(onDismissRequest = {
        persist()
        onDismiss()
    }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val total = 5
                val stepText = context.getString(R.string.dp_level_wizard_step, currentStep + 1, total)
                Text(text = context.getString(R.string.dp_level_wizard_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stepText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = (currentStep + 1) / total.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (currentStep) {
                        0 -> {
                            Text(text = context.getString(R.string.dp_level_wizard_s1_desc))
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
                                        errorText = null
                                        persist()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ChoiceButton(
                                    title = context.getString(R.string.dp_level_instrument_dual),
                                    hint = context.getString(R.string.hint_dp_level_instrument_dual),
                                    selected = instrument == DpLevelInstrument.DUAL_FLANGE,
                                    onClick = {
                                        instrument = DpLevelInstrument.DUAL_FLANGE
                                        errorText = null
                                        persist()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        1 -> {
                            Text(text = context.getString(R.string.dp_level_wizard_s2_desc))
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
                                        errorText = null
                                        persist()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                ChoiceButton(
                                    title = context.getString(R.string.dp_level_mode_recalc),
                                    hint = context.getString(R.string.hint_dp_level_mode_recalc),
                                    selected = mode == DpLevelMode.RECALC_RANGE,
                                    onClick = {
                                        mode = DpLevelMode.RECALC_RANGE
                                        errorText = null
                                        persist()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        2 -> {
                            Text(text = context.getString(R.string.dp_level_wizard_s3_desc))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                ChoiceButton(
                                    title = HeightUnit.M.displayName,
                                    hint = context.getString(R.string.dp_level_wizard_unit_height_hint),
                                    selected = heightUnit == HeightUnit.M,
                                    onClick = { heightUnit = HeightUnit.M; persist() },
                                    modifier = Modifier.weight(1f),
                                )
                                ChoiceButton(
                                    title = HeightUnit.MM.displayName,
                                    hint = context.getString(R.string.dp_level_wizard_unit_height_hint),
                                    selected = heightUnit == HeightUnit.MM,
                                    onClick = { heightUnit = HeightUnit.MM; persist() },
                                    modifier = Modifier.weight(1f),
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
                                        onClick = { zeroShiftDirection = DpZeroShiftDirection.NEGATIVE; persist() },
                                        modifier = Modifier.weight(1f),
                                    )
                                    ChoiceButton(
                                        title = context.getString(R.string.dp_level_shift_positive),
                                        hint = context.getString(R.string.hint_dp_level_shift_positive),
                                        selected = zeroShiftDirection == DpZeroShiftDirection.POSITIVE,
                                        onClick = { zeroShiftDirection = DpZeroShiftDirection.POSITIVE; persist() },
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                OutlinedTextField(
                                    value = spanHeightText,
                                    onValueChange = { spanHeightText = filterNumber(it, false); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_span_height)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = heightUnit.displayName) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_span_height)) },
                                )
                                OutlinedTextField(
                                    value = zeroShiftText,
                                    onValueChange = { zeroShiftText = filterNumber(it, false); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_zero_shift_height)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = heightUnit.displayName) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_zero_shift_height2)) },
                                )

                                if (instrument == DpLevelInstrument.DUAL_FLANGE) {
                                    Text(text = context.getString(R.string.dp_level_wizard_s3_dual_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedTextField(
                                        value = h2Text,
                                        onValueChange = { h2Text = filterNumber(it, true); errorText = null; persist() },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(text = context.getString(R.string.dp_level_wizard_h2)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        suffix = { Text(text = heightUnit.displayName) },
                                        supportingText = { Text(text = context.getString(R.string.dp_level_wizard_h2_hint)) },
                                    )
                                    OutlinedTextField(
                                        value = h3Text,
                                        onValueChange = { h3Text = filterNumber(it, true); errorText = null; persist() },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(text = context.getString(R.string.dp_level_wizard_h3)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        suffix = { Text(text = heightUnit.displayName) },
                                        supportingText = { Text(text = context.getString(R.string.dp_level_wizard_h3_hint)) },
                                    )
                                    val h2 = h2Text.toDoubleOrNull() ?: 0.0
                                    val h3 = h3Text.toDoubleOrNull() ?: 0.0
                                    Text(
                                        text = context.getString(R.string.dp_level_wizard_hoil, formatNumber(h2 - h3), heightUnit.displayName),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Text(text = context.getString(R.string.dp_level_wizard_s3_recalc_desc))
                                OutlinedTextField(
                                    value = lrvText,
                                    onValueChange = { lrvText = filterNumber(it, true); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_lrv)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = dpUnit.displayName) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_lrv)) },
                                )
                                OutlinedTextField(
                                    value = urvText,
                                    onValueChange = { urvText = filterNumber(it, true); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_urv)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = dpUnit.displayName) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_urv)) },
                                )
                                OutlinedTextField(
                                    value = levelPercentText,
                                    onValueChange = { levelPercentText = filterNumber(it, false); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_level_percent)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = "%") },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_level_percent)) },
                                )
                                OutlinedTextField(
                                    value = dpNowText,
                                    onValueChange = { dpNowText = filterNumber(it, true); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_dp_now)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = dpUnit.displayName) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_dp_now)) },
                                )
                                UnitDropdown(
                                    title = context.getString(R.string.dp_level_wizard_dp_unit),
                                    hint = context.getString(R.string.dp_level_wizard_dp_unit_hint),
                                    unit = dpUnit,
                                    onUnitChange = { dpUnit = it; persist() },
                                )
                            }
                        }
                        3 -> {
                            Text(text = context.getString(R.string.dp_level_wizard_s4_desc))
                            if (mode == DpLevelMode.RHO_AND_HEIGHT) {
                                OutlinedTextField(
                                    value = mediumDensityText,
                                    onValueChange = { mediumDensityText = filterNumber(it, false); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_medium_density)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = context.getString(R.string.unit_kg_m3)) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_medium_density)) },
                                )
                            }

                            if (instrument == DpLevelInstrument.DUAL_FLANGE) {
                                OutlinedTextField(
                                    value = oilDensityText,
                                    onValueChange = { oilDensityText = filterNumber(it, false); errorText = null; persist() },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(text = context.getString(R.string.dp_level_oil_density)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = context.getString(R.string.unit_kg_m3)) },
                                    supportingText = { Text(text = context.getString(R.string.hint_dp_level_oil_density)) },
                                )
                            }

                            UnitDropdown(
                                title = context.getString(R.string.dp_level_output_unit),
                                hint = context.getString(R.string.dp_level_wizard_output_unit_hint),
                                unit = outputUnit,
                                onUnitChange = { outputUnit = it; persist() },
                            )
                        }
                        4 -> {
                            Text(text = context.getString(R.string.dp_level_wizard_s5_desc))
                            val preview = buildPreview(
                                instrument = instrument,
                                mode = mode,
                                dpUnit = dpUnit,
                                heightUnit = heightUnit,
                                outputUnit = outputUnit,
                                spanHeightText = spanHeightText,
                                zeroShiftText = zeroShiftText,
                                h2Text = h2Text,
                                h3Text = h3Text,
                                mediumDensityText = mediumDensityText,
                                oilDensityText = oilDensityText,
                                lrvText = lrvText,
                                urvText = urvText,
                                levelPercentText = levelPercentText,
                                dpNowText = dpNowText,
                                zeroShiftDirection = zeroShiftDirection,
                            )
                            Text(text = preview, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                val err = errorText
                if (err != null) {
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SecondaryButton(
                        title = context.getString(R.string.dp_level_wizard_back),
                        onClick = {
                            if (currentStep == 0) {
                                persist()
                                onDismiss()
                            } else {
                                val prev = (currentStep - 1).coerceAtLeast(0)
                                currentStep = prev
                                persist(step = prev)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        title = if (currentStep == 4) context.getString(R.string.dp_level_wizard_apply) else context.getString(R.string.dp_level_wizard_next),
                        onClick = {
                            if (!validateStep(currentStep)) return@SecondaryButton
                            if (currentStep == 4) {
                                apply()
                                onDismiss()
                            } else {
                                val next = (currentStep + 1).coerceAtMost(4)
                                val completed = maxOf(completedStep, currentStep)
                                completedStep = completed
                                currentStep = next
                                persist(step = next, completed = completed)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun buildPreview(
    instrument: DpLevelInstrument,
    mode: DpLevelMode,
    dpUnit: PressureUnit,
    heightUnit: HeightUnit,
    outputUnit: PressureUnit,
    spanHeightText: String,
    zeroShiftText: String,
    h2Text: String,
    h3Text: String,
    mediumDensityText: String,
    oilDensityText: String,
    lrvText: String,
    urvText: String,
    levelPercentText: String,
    dpNowText: String,
    zeroShiftDirection: DpZeroShiftDirection,
): String {
    val span = spanHeightText.toDoubleOrNull() ?: Double.NaN
    val zero = zeroShiftText.toDoubleOrNull() ?: Double.NaN
    val h2 = h2Text.toDoubleOrNull() ?: 0.0
    val h3 = h3Text.toDoubleOrNull() ?: 0.0
    val rho = mediumDensityText.toDoubleOrNull()
    val oilRho = oilDensityText.toDoubleOrNull() ?: 950.0
    val lrv = lrvText.toDoubleOrNull()
    val urv = urvText.toDoubleOrNull()
    val pct = levelPercentText.toDoubleOrNull()
    val dpNow = dpNowText.toDoubleOrNull()

    val spanM = spanHeightUnitToM(span, heightUnit)
    val zeroM = spanHeightUnitToM(zero, heightUnit)
    val hOilM = spanHeightUnitToM(h2 - h3, heightUnit)

    val input = DpLevelInput(
        instrument = instrument,
        mode = mode,
        mediumDensityKgM3 = if (mode == DpLevelMode.RHO_AND_HEIGHT) rho else null,
        oilDensityKgM3 = oilRho,
        spanHeightM = spanM,
        zeroShiftDirection = zeroShiftDirection,
        zeroShiftMediumM = zeroM,
        oilEquivalentHeightM = hOilM,
        dpLrvPa = if (mode == DpLevelMode.RECALC_RANGE && lrv != null) dpUnit.toPa(lrv) else null,
        dpUrvPa = if (mode == DpLevelMode.RECALC_RANGE && urv != null) dpUnit.toPa(urv) else null,
        levelPercent = if (mode == DpLevelMode.RECALC_RANGE) pct else null,
        dpNowPa = if (mode == DpLevelMode.RECALC_RANGE && dpNow != null) dpUnit.toPa(dpNow) else null,
    )
    val out = DpLevelCalculator.compute(input)
    val lrvOut = outputUnit.fromPa(out.lrvPa)
    val urvOut = outputUnit.fromPa(out.urvPa)
    val spanOut = outputUnit.fromPa(out.spanPa)

    val inst = if (instrument == DpLevelInstrument.DUAL_FLANGE) "双法兰" else "差压"
    val modeName = if (mode == DpLevelMode.RHO_AND_HEIGHT) "密度+高度" else "量程反算"
    return buildString {
        appendLine("仪表：$inst")
        appendLine("模式：$modeName")
        if (mode == DpLevelMode.RHO_AND_HEIGHT) {
            appendLine("迁移：${if (zeroShiftDirection == DpZeroShiftDirection.NEGATIVE) "负迁移" else "正迁移"}")
            appendLine("H：${formatNumber(span)} ${heightUnit.displayName}")
            appendLine("h0：${formatNumber(zero)} ${heightUnit.displayName}")
            if (instrument == DpLevelInstrument.DUAL_FLANGE) {
                appendLine("hOil：${formatNumber(h2 - h3)} ${heightUnit.displayName}")
                appendLine("ρ硅油：${formatNumber(oilRho)} kg/m³")
            }
            appendLine("ρ介质：${formatNumber(rho ?: Double.NaN)} kg/m³")
        } else {
            appendLine("LRV0：${formatNumber(lrv ?: Double.NaN)} ${dpUnit.displayName}")
            appendLine("URV0：${formatNumber(urv ?: Double.NaN)} ${dpUnit.displayName}")
            appendLine("液位%：${formatNumber(pct ?: Double.NaN)} %")
            appendLine("ΔPnow：${formatNumber(dpNow ?: Double.NaN)} ${dpUnit.displayName}")
        }
        appendLine("LRV：${formatNumber(lrvOut)} ${outputUnit.displayName}")
        appendLine("URV：${formatNumber(urvOut)} ${outputUnit.displayName}")
        append("SPAN：${formatNumber(spanOut)} ${outputUnit.displayName}")
    }
}

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

private fun formatNumber(v: Double): String = String.format(Locale.US, "%.4f", v)

private fun spanHeightUnitToM(value: Double, unit: HeightUnit): Double {
    if (value.isNaN()) return Double.NaN
    return unit.toM(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
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
