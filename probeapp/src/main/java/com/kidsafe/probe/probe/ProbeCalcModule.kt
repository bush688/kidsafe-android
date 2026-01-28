package com.kidsafe.probe.probe

import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.CalcRecord
import com.kidsafe.probe.HistoryStore
import com.kidsafe.probe.ModuleInputStore
import com.kidsafe.probe.ProbeCalculator
import com.kidsafe.probe.ProbeSettings
import com.kidsafe.probe.R
import com.kidsafe.probe.SettingsStore
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

private enum class ChuanUnit {
    MM,
    DAO,
}

private enum class ZeroVoltageMode {
    PRESET,
    CUSTOM,
}

private enum class ProbeMode {
    CHUAN,
    VOLTAGE,
}

@Composable
fun ProbeCalcModule(
    modifier: Modifier = Modifier,
    onCopy: suspend (String) -> Unit,
    onMessage: suspend (String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val historyStore = remember(appContext) { HistoryStore(appContext) }
    val settingsStore = remember(appContext) { SettingsStore(appContext) }
    val inputStore = remember(appContext) { ModuleInputStore(appContext) }
    val scope = rememberCoroutineScope()
    val history by historyStore.history.collectAsState(initial = emptyList())
    val settings by settingsStore.settings.collectAsState(initial = ProbeSettings(ProbeCalculator.defaultZeroVoltage()))

    val presetZeroVoltages = remember {
        listOf(10.0, 9.5, 9.0, 8.5, 8.0, 7.5, 7.0, 5.0, 0.0)
    }

    var mode by remember { mutableStateOf(ProbeMode.CHUAN) }

    var zeroMode by remember { mutableStateOf(ZeroVoltageMode.PRESET) }
    var zeroVoltageV by remember { mutableStateOf(ProbeCalculator.defaultZeroVoltage()) }
    var zeroCustomText by remember { mutableStateOf("") }
    var zeroError by remember { mutableStateOf<String?>(null) }
    var presetExpanded by remember { mutableStateOf(false) }
    var presetSelectedV by remember { mutableStateOf(10.0) }
    var loadedFromStore by remember { mutableStateOf(false) }

    var chuanText by remember { mutableStateOf("") }
    var chuanUnit by remember { mutableStateOf(ChuanUnit.MM) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var vFar by remember { mutableStateOf<Double?>(null) }
    var vNear by remember { mutableStateOf<Double?>(null) }

    var voltageText by remember { mutableStateOf("") }
    var voltageError by remember { mutableStateOf<String?>(null) }
    var chuanFromVoltageMm by remember { mutableStateOf<Double?>(null) }
    var endSideLabel by remember { mutableStateOf<String?>(null) }
    var inputsLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val obj = inputStore.load("probe")
        if (obj != null) {
            runCatching { ProbeMode.valueOf(obj.optString("mode", mode.name)) }.getOrNull()?.let { mode = it }
            chuanText = obj.optString("chuanText", chuanText)
            runCatching { ChuanUnit.valueOf(obj.optString("chuanUnit", chuanUnit.name)) }.getOrNull()?.let { chuanUnit = it }
            voltageText = obj.optString("voltageText", voltageText)
        }
        inputsLoaded = true
    }

    LaunchedEffect(mode, chuanText, chuanUnit, voltageText, inputsLoaded) {
        if (!inputsLoaded) return@LaunchedEffect
        inputStore.save(
            "probe",
            JSONObject()
                .put("mode", mode.name)
                .put("chuanText", chuanText)
                .put("chuanUnit", chuanUnit.name)
                .put("voltageText", voltageText)
        )
    }

    LaunchedEffect(settings.zeroVoltageV) {
        if (!loadedFromStore) {
            val v = settings.zeroVoltageV.coerceIn(0.0, 10.0)
            zeroVoltageV = v
            val matchedPreset = presetZeroVoltages.firstOrNull { kotlin.math.abs(it - v) < 1e-6 }
            if (matchedPreset != null) {
                zeroMode = ZeroVoltageMode.PRESET
                presetSelectedV = matchedPreset
            } else {
                zeroMode = ZeroVoltageMode.CUSTOM
            }
            zeroCustomText = formatVolt2(v)
            loadedFromStore = true
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = context.getString(R.string.probe_title), style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChoiceButton(
                    title = context.getString(R.string.probe_mode_chuan),
                    hint = context.getString(R.string.hint_probe_mode_chuan),
                    selected = mode == ProbeMode.CHUAN,
                    onClick = { mode = ProbeMode.CHUAN },
                    modifier = Modifier.weight(1f)
                )
                ChoiceButton(
                    title = context.getString(R.string.probe_mode_voltage),
                    hint = context.getString(R.string.hint_probe_mode_voltage),
                    selected = mode == ProbeMode.VOLTAGE,
                    onClick = { mode = ProbeMode.VOLTAGE },
                    modifier = Modifier.weight(1f)
                )
            }

            ZeroVoltageCard(
                presetZeroVoltages = presetZeroVoltages,
                zeroMode = zeroMode,
                onZeroModeChange = { zeroMode = it; zeroError = null },
                presetExpanded = presetExpanded,
                onPresetExpandedChange = { presetExpanded = it },
                presetSelectedV = presetSelectedV,
                onPresetSelectedVChange = { presetSelectedV = it },
                zeroVoltageV = zeroVoltageV,
                onZeroVoltageVChange = { zeroVoltageV = it },
                zeroCustomText = zeroCustomText,
                onZeroCustomTextChange = { zeroCustomText = it },
                zeroError = zeroError,
                onZeroErrorChange = { zeroError = it },
                onPersist = { v -> scope.launch { settingsStore.setZeroVoltageV(v) } },
            )

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "probe_mode",
            ) { target ->
                when (target) {
                    ProbeMode.CHUAN -> ChuanToVoltageCard(
                        chuanText = chuanText,
                        onChuanTextChange = { chuanText = it; inputError = null },
                        chuanUnit = chuanUnit,
                        onChuanUnitChange = { chuanUnit = it },
                        inputError = inputError,
                        onInputErrorChange = { inputError = it },
                        vFar = vFar,
                        vNear = vNear,
                        onReset = {
                            chuanText = ""
                            inputError = null
                            vFar = null
                            vNear = null
                        },
                        onCalculate = {
                            try {
                                val raw = chuanText.toDoubleOrNull()
                                val c = raw?.let { toMillimeters(it, chuanUnit) }
                                when {
                                    raw == null || c == null -> {
                                        inputError = context.getString(R.string.invalid_input)
                                        vFar = null
                                        vNear = null
                                    }
                                    raw < 0 || c < 0 -> {
                                        inputError = context.getString(R.string.negative_input)
                                        vFar = null
                                        vNear = null
                                    }
                                    else -> {
                                        val far = ProbeCalculator.calcFarVoltage(c, zeroVoltageV)
                                        val near = ProbeCalculator.calcNearVoltage(c, zeroVoltageV)
                                        vFar = far
                                        vNear = near
                                        scope.launch {
                                            historyStore.add(
                                                CalcRecord(
                                                    chuanMm = c,
                                                    vFar = far,
                                                    vNear = near,
                                                    epochMillis = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ProbeCalcModule", "Chuan calc failed", e)
                                inputError = context.getString(R.string.calc_failed)
                                vFar = null
                                vNear = null
                            }
                        },
                        onCopy = {
                            val far = vFar
                            val near = vNear
                            if (far != null && near != null) {
                                val raw = chuanText.toDoubleOrNull()
                                val cMm = raw?.let { toMillimeters(it, chuanUnit) }
                                val text = buildCopyText(
                                    context = context,
                                    raw = raw,
                                    unit = chuanUnit,
                                    chuanMm = cMm,
                                    far = far,
                                    near = near,
                                    zeroVoltageV = zeroVoltageV
                                )
                                scope.launch { onCopy(text) }
                            }
                        }
                    )

                    ProbeMode.VOLTAGE -> VoltageToChuanCard(
                        voltageText = voltageText,
                        onVoltageTextChange = {
                            voltageText = it
                            voltageError = null
                            chuanFromVoltageMm = null
                            endSideLabel = null
                        },
                        voltageError = voltageError,
                        onVoltageErrorChange = { voltageError = it },
                        chuanFromVoltageMm = chuanFromVoltageMm,
                        endSideLabel = endSideLabel,
                        onResultChange = { mm, side ->
                            chuanFromVoltageMm = mm
                            endSideLabel = side
                        },
                        onCopy = {
                            val mm = chuanFromVoltageMm
                            if (mm != null) {
                                val side = endSideLabel.orEmpty()
                                val text = buildString {
                                    appendLine("${context.getString(R.string.probe_mode_voltage)}: ${voltageText} V")
                                    appendLine("${context.getString(R.string.reverse_result_label)}: ${formatMm3(mm)} mm")
                                    if (side.isNotBlank()) appendLine(context.getString(R.string.reverse_side_label, side))
                                    appendLine("${context.getString(R.string.zero_voltage_copy_line)}: ${formatVolt2(zeroVoltageV)} V")
                                }.trim()
                                scope.launch { onCopy(text) }
                            }
                        },
                        zeroVoltageV = zeroVoltageV,
                    )
                }
            }

            SecondaryButton(
                title = context.getString(R.string.restore_defaults),
                onClick = {
                    mode = ProbeMode.CHUAN
                    chuanText = ""
                    chuanUnit = ChuanUnit.MM
                    inputError = null
                    vFar = null
                    vNear = null
                    voltageText = ""
                    voltageError = null
                    chuanFromVoltageMm = null
                    endSideLabel = null

                    zeroMode = ZeroVoltageMode.PRESET
                    presetExpanded = false
                    presetSelectedV = 10.0
                    zeroCustomText = ""
                    zeroError = null
                    loadedFromStore = false

                    scope.launch {
                        inputStore.clear("probe")
                        settingsStore.setZeroVoltageV(ProbeCalculator.defaultZeroVoltage())
                        onMessage(context.getString(R.string.restored_defaults))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            ProbeHistoryCard(
                history = history,
                onClear = { scope.launch { historyStore.clear() } },
            )
        }
    }
}

@Composable
private fun ProbeHistoryCard(
    history: List<CalcRecord>,
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "C=${formatMm(record.chuanMm)} mm", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "V远=${formatVolt(record.vFar)} V    V近=${formatVolt(record.vNear)} V",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZeroVoltageCard(
    presetZeroVoltages: List<Double>,
    zeroMode: ZeroVoltageMode,
    onZeroModeChange: (ZeroVoltageMode) -> Unit,
    presetExpanded: Boolean,
    onPresetExpandedChange: (Boolean) -> Unit,
    presetSelectedV: Double,
    onPresetSelectedVChange: (Double) -> Unit,
    zeroVoltageV: Double,
    onZeroVoltageVChange: (Double) -> Unit,
    zeroCustomText: String,
    onZeroCustomTextChange: (String) -> Unit,
    zeroError: String?,
    onZeroErrorChange: (String?) -> Unit,
    onPersist: (Double) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = context.getString(R.string.zero_voltage_title), style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChoiceButton(
                title = context.getString(R.string.preset_mode),
                hint = context.getString(R.string.hint_zero_preset),
                selected = zeroMode == ZeroVoltageMode.PRESET,
                onClick = { onZeroModeChange(ZeroVoltageMode.PRESET) },
                modifier = Modifier.weight(1f)
            )
            ChoiceButton(
                title = context.getString(R.string.custom_mode),
                hint = context.getString(R.string.hint_zero_custom),
                selected = zeroMode == ZeroVoltageMode.CUSTOM,
                onClick = { onZeroModeChange(ZeroVoltageMode.CUSTOM) },
                modifier = Modifier.weight(1f)
            )
        }

        if (zeroMode == ZeroVoltageMode.PRESET) {
            ExposedDropdownMenuBox(
                expanded = presetExpanded,
                onExpandedChange = { onPresetExpandedChange(!presetExpanded) },
            ) {
                OutlinedTextField(
                    value = formatVolt2(presetSelectedV),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text(text = context.getString(R.string.zero_voltage_preset)) },
                    suffix = { Text(text = context.getString(R.string.unit_v)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                    supportingText = { Text(text = context.getString(R.string.hint_zero_preset_select)) },
                )
                ExposedDropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { onPresetExpandedChange(false) }
                ) {
                    presetZeroVoltages.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = "${formatVolt2(option)} ${context.getString(R.string.unit_v)}") },
                            onClick = {
                                onPresetExpandedChange(false)
                                onPresetSelectedVChange(option)
                                val v = option.coerceIn(0.0, 10.0)
                                onZeroVoltageVChange(v)
                                onZeroCustomTextChange(formatVolt2(v))
                                onZeroErrorChange(null)
                                onPersist(v)
                            }
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = zeroCustomText,
                onValueChange = { next ->
                    val filtered = next.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        onZeroCustomTextChange(filtered)
                        val v = filtered.toDoubleOrNull()
                        when {
                            filtered.isBlank() -> onZeroErrorChange(null)
                            v == null -> onZeroErrorChange(context.getString(R.string.invalid_input))
                            v < 0 || v > 10 -> onZeroErrorChange(context.getString(R.string.zero_voltage_range_error))
                            else -> {
                                onZeroErrorChange(null)
                                onZeroVoltageVChange(v)
                                onPersist(v)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = context.getString(R.string.zero_voltage_custom)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text(text = context.getString(R.string.unit_v)) },
                isError = zeroError != null,
                supportingText = {
                    val msg = zeroError
                    if (msg != null) Text(text = msg) else Text(text = context.getString(R.string.hint_zero_custom_input))
                }
            )
        }

        Text(
            text = context.getString(R.string.zero_voltage_current, formatVolt2(zeroVoltageV)),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChuanToVoltageCard(
    chuanText: String,
    onChuanTextChange: (String) -> Unit,
    chuanUnit: ChuanUnit,
    onChuanUnitChange: (ChuanUnit) -> Unit,
    inputError: String?,
    onInputErrorChange: (String?) -> Unit,
    vFar: Double?,
    vNear: Double?,
    onReset: () -> Unit,
    onCalculate: () -> Unit,
    onCopy: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = context.getString(R.string.forward_title), style = MaterialTheme.typography.titleSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChoiceButton(
                title = context.getString(R.string.unit_mm),
                hint = context.getString(R.string.hint_unit_mm),
                selected = chuanUnit == ChuanUnit.MM,
                onClick = { onChuanUnitChange(ChuanUnit.MM) },
                modifier = Modifier.weight(1f),
            )
            ChoiceButton(
                title = context.getString(R.string.unit_dao),
                hint = context.getString(R.string.hint_unit_dao),
                selected = chuanUnit == ChuanUnit.DAO,
                onClick = { onChuanUnitChange(ChuanUnit.DAO) },
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = chuanText,
            onValueChange = { next ->
                val filtered = next.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1) {
                    onChuanTextChange(filtered)
                    onInputErrorChange(null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = context.getString(R.string.input_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text(text = unitLabel(context, chuanUnit)) },
            isError = inputError != null,
            supportingText = {
                val msg = inputError
                if (msg != null) Text(text = msg) else Text(text = context.getString(R.string.hint_probe_chuan_input))
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SecondaryButton(title = context.getString(R.string.calculate), onClick = onCalculate, modifier = Modifier.weight(1f))
            SecondaryButton(title = context.getString(R.string.reset), onClick = onReset)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCopy,
                    enabled = vFar != null && vNear != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = context.getString(R.string.copy))
                }
                Text(
                    text = context.getString(R.string.hint_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ResultCard(title = context.getString(R.string.far_end), value = vFar?.let { "${formatVolt(it)} V" })
        ResultCard(title = context.getString(R.string.near_end), value = vNear?.let { "${formatVolt(it)} V" })
    }
}

@Composable
private fun VoltageToChuanCard(
    voltageText: String,
    onVoltageTextChange: (String) -> Unit,
    voltageError: String?,
    onVoltageErrorChange: (String?) -> Unit,
    chuanFromVoltageMm: Double?,
    endSideLabel: String?,
    onResultChange: (Double, String) -> Unit,
    onCopy: () -> Unit,
    zeroVoltageV: Double,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = context.getString(R.string.reverse_title), style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = voltageText,
            onValueChange = { next ->
                val filtered = next.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1) {
                    onVoltageTextChange(filtered)
                    val v = filtered.toDoubleOrNull()
                    if (filtered.isBlank()) {
                        onVoltageErrorChange(null)
                    } else if (v == null) {
                        onVoltageErrorChange(context.getString(R.string.invalid_input))
                    } else {
                        try {
                            applyReverseCalc(
                                voltageV = v,
                                zeroVoltageV = zeroVoltageV,
                                invalidNegativeMessage = context.getString(R.string.negative_voltage),
                                sideZero = context.getString(R.string.side_zero),
                                sideFar = context.getString(R.string.side_far),
                                sideNear = context.getString(R.string.side_near),
                                onError = { msg ->
                                    onVoltageErrorChange(msg)
                                },
                                onResult = { mm, side ->
                                    onVoltageErrorChange(null)
                                    onResultChange(mm, side)
                                }
                            )
                        } catch (e: Exception) {
                            Log.e("ProbeCalcModule", "Voltage calc failed", e)
                            onVoltageErrorChange(context.getString(R.string.calc_failed))
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = context.getString(R.string.voltage_input_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            suffix = { Text(text = context.getString(R.string.unit_v)) },
            isError = voltageError != null,
            supportingText = {
                val msg = voltageError
                if (msg != null) Text(text = msg) else Text(text = context.getString(R.string.hint_probe_voltage_input))
            }
        )

        ResultCard(
            title = context.getString(R.string.reverse_result_label),
            value = chuanFromVoltageMm?.let { "${formatMm3(it)} mm" }
        )
        val side = endSideLabel
        if (!side.isNullOrBlank()) {
            Text(text = context.getString(R.string.reverse_side_label, side), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCopy, enabled = chuanFromVoltageMm != null) {
                Icon(Icons.Default.ContentCopy, contentDescription = context.getString(R.string.copy))
            }
        }
    }
}

@Composable
private fun ResultCard(title: String, value: String?) {
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

private fun formatVolt(v: Double): String = String.format(Locale.US, "%.2f", v)
private fun formatVolt2(v: Double): String = String.format(Locale.US, "%.2f", v)
private fun formatMm(mm: Double): String = String.format(Locale.US, "%.2f", mm)
private fun formatMm3(mm: Double): String = String.format(Locale.US, "%.3f", mm)

private fun toMillimeters(value: Double, unit: ChuanUnit): Double {
    return when (unit) {
        ChuanUnit.MM -> value
        ChuanUnit.DAO -> value / 100.0
    }
}

private fun unitLabel(context: Context, unit: ChuanUnit): String {
    return when (unit) {
        ChuanUnit.MM -> context.getString(R.string.unit_mm)
        ChuanUnit.DAO -> context.getString(R.string.unit_dao)
    }
}

private fun buildCopyText(
    context: Context,
    raw: Double?,
    unit: ChuanUnit,
    chuanMm: Double?,
    far: Double,
    near: Double,
    zeroVoltageV: Double,
): String {
    val unitLabel = unitLabel(context, unit)
    val line1 = when {
        raw == null -> "窜量: —"
        chuanMm == null -> "窜量: ${formatMm(raw)} $unitLabel"
        unit == ChuanUnit.MM -> "窜量: ${formatMm(chuanMm)} mm"
        else -> "窜量: ${formatMm(raw)} 道（${formatMm(chuanMm)} mm）"
    }
    return buildString {
        appendLine(line1)
        appendLine("${context.getString(R.string.zero_voltage_copy_line)}: ${formatVolt2(zeroVoltageV)} V")
        appendLine("${context.getString(R.string.far_end)}: ${formatVolt(far)} V")
        appendLine("${context.getString(R.string.near_end)}: ${formatVolt(near)} V")
    }.trim()
}

private fun applyReverseCalc(
    voltageV: Double,
    zeroVoltageV: Double,
    invalidNegativeMessage: String,
    sideZero: String,
    sideFar: String,
    sideNear: String,
    onError: (String) -> Unit,
    onResult: (chuanAbsMm: Double, sideLabel: String) -> Unit
) {
    if (voltageV < 0) {
        onError(invalidNegativeMessage)
        return
    }
    val signed = ProbeCalculator.calcChuanMmSigned(voltageV, zeroVoltageV)
    val absMm = kotlin.math.abs(signed)
    val side = when {
        absMm < 1e-9 -> sideZero
        signed > 0 -> sideFar
        else -> sideNear
    }
    onResult(absMm, side)
}
