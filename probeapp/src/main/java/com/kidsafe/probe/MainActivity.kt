package com.kidsafe.probe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProbeCalcScreen()
            }
        }
    }
}

private enum class ChuanUnit {
    MM,
    DAO,
}

private enum class ZeroVoltageMode {
    PRESET,
    CUSTOM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProbeCalcScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val historyStore = remember { HistoryStore(context) }
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val history by historyStore.history.collectAsState(initial = emptyList())
    val settings by settingsStore.settings.collectAsState(initial = ProbeSettings(ProbeCalculator.defaultZeroVoltage()))

    val presetZeroVoltages = remember {
        listOf(10.0, 9.5, 9.0, 8.5, 8.0, 7.5, 7.0, 5.0, 0.0)
    }

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

    var showHelp by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(zeroVoltageV) {
        vFar = null
        vNear = null
        inputError = null

        val v = voltageText.toDoubleOrNull()
        if (v != null) {
            applyReverseCalc(
                voltageV = v,
                zeroVoltageV = zeroVoltageV,
                invalidNegativeMessage = context.getString(R.string.negative_voltage),
                sideZero = context.getString(R.string.side_zero),
                sideFar = context.getString(R.string.side_far),
                sideNear = context.getString(R.string.side_near),
                onError = { voltageError = it },
                onResult = { mm, side ->
                    voltageError = null
                    chuanFromVoltageMm = mm
                    endSideLabel = side
                }
            )
        } else {
            voltageError = null
            chuanFromVoltageMm = null
            endSideLabel = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = context.getString(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = context.getString(R.string.help))
                    }
                    IconButton(
                        onClick = {
                            scope.launch { historyStore.clear() }
                        },
                        enabled = history.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.clear_history))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = context.getString(R.string.zero_voltage_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = context.getString(R.string.sensitivity_label, formatSensitivity(ProbeCalculator.M_V_PER_MM)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = context.getString(R.string.setting_mode), style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = {
                                zeroMode = ZeroVoltageMode.PRESET
                                zeroError = null
                                val v = presetSelectedV.coerceIn(0.0, 10.0)
                                zeroVoltageV = v
                                zeroCustomText = formatVolt2(v)
                                scope.launch { settingsStore.setZeroVoltageV(v) }
                            },
                            enabled = zeroMode != ZeroVoltageMode.PRESET
                        ) { Text(text = context.getString(R.string.preset_mode)) }
                        Button(
                            onClick = {
                                zeroMode = ZeroVoltageMode.CUSTOM
                                zeroError = null
                            },
                            enabled = zeroMode != ZeroVoltageMode.CUSTOM
                        ) { Text(text = context.getString(R.string.custom_mode)) }
                    }

                    if (zeroMode == ZeroVoltageMode.PRESET) {
                        ExposedDropdownMenuBox(
                            expanded = presetExpanded,
                            onExpandedChange = { presetExpanded = !presetExpanded },
                        ) {
                            OutlinedTextField(
                                value = formatVolt2(presetSelectedV),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                label = { Text(text = context.getString(R.string.zero_voltage_preset)) },
                                suffix = { Text(text = context.getString(R.string.unit_v)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                            )
                            ExposedDropdownMenu(
                                expanded = presetExpanded,
                                onDismissRequest = { presetExpanded = false }
                            ) {
                                presetZeroVoltages.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = "${formatVolt2(option)} ${context.getString(R.string.unit_v)}") },
                                        onClick = {
                                            presetExpanded = false
                                            presetSelectedV = option
                                            val v = option.coerceIn(0.0, 10.0)
                                            zeroVoltageV = v
                                            zeroCustomText = formatVolt2(v)
                                            zeroError = null
                                            scope.launch { settingsStore.setZeroVoltageV(v) }
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
                                    zeroCustomText = filtered
                                    val v = filtered.toDoubleOrNull()
                                    when {
                                        filtered.isBlank() -> {
                                            zeroError = null
                                        }
                                        v == null -> {
                                            zeroError = context.getString(R.string.invalid_input)
                                        }
                                        v < 0 || v > 10 -> {
                                            zeroError = context.getString(R.string.zero_voltage_range_error)
                                        }
                                        else -> {
                                            zeroError = null
                                            zeroVoltageV = v
                                            scope.launch { settingsStore.setZeroVoltageV(v) }
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
                                if (msg != null) Text(text = msg)
                            }
                        )
                    }

                    Text(
                        text = context.getString(R.string.zero_voltage_current, formatVolt2(zeroVoltageV)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = context.getString(R.string.forward_title), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = context.getString(R.string.unit_label), style = MaterialTheme.typography.bodyMedium)
                        UnitToggle(
                            selected = chuanUnit,
                            onSelected = { chuanUnit = it },
                        )
                    }

                    OutlinedTextField(
                        value = chuanText,
                        onValueChange = { next ->
                            val filtered = next.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                chuanText = filtered
                                inputError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.input_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = unitLabel(context, chuanUnit)) },
                        isError = inputError != null,
                        supportingText = {
                            val msg = inputError
                            if (msg != null) Text(text = msg)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
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
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = context.getString(R.string.calculate))
                        }

                        Button(
                            onClick = {
                                chuanText = ""
                                inputError = null
                                vFar = null
                                vNear = null
                            }
                        ) {
                            Text(text = context.getString(R.string.reset))
                        }

                        IconButton(
                            onClick = {
                                val far = vFar
                                val near = vNear
                                if (far != null && near != null) {
                                    val raw = chuanText.toDoubleOrNull()
                                    val cMm = raw?.let { toMillimeters(it, chuanUnit) }
                                    copyToClipboard(
                                        context = context,
                                        text = buildCopyText(
                                            context = context,
                                            raw = raw,
                                            unit = chuanUnit,
                                            chuanMm = cMm,
                                            far = far,
                                            near = near,
                                            zeroVoltageV = zeroVoltageV
                                        )
                                    )
                                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.copied)) }
                                }
                            },
                            enabled = vFar != null && vNear != null
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = context.getString(R.string.copy))
                        }
                    }

                    ResultCard(
                        title = context.getString(R.string.far_end),
                        value = vFar
                    )
                    ResultCard(
                        title = context.getString(R.string.near_end),
                        value = vNear
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = context.getString(R.string.reverse_title), style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = voltageText,
                        onValueChange = { next ->
                            val filtered = next.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                voltageText = filtered
                                val v = filtered.toDoubleOrNull()
                                if (filtered.isBlank()) {
                                    voltageError = null
                                    chuanFromVoltageMm = null
                                    endSideLabel = null
                                } else if (v == null) {
                                    voltageError = context.getString(R.string.invalid_input)
                                    chuanFromVoltageMm = null
                                    endSideLabel = null
                                } else {
                                    applyReverseCalc(
                                        voltageV = v,
                                        zeroVoltageV = zeroVoltageV,
                                        invalidNegativeMessage = context.getString(R.string.negative_voltage),
                                        sideZero = context.getString(R.string.side_zero),
                                        sideFar = context.getString(R.string.side_far),
                                        sideNear = context.getString(R.string.side_near),
                                        onError = { msg ->
                                            voltageError = msg
                                            chuanFromVoltageMm = null
                                            endSideLabel = null
                                        },
                                        onResult = { mm, side ->
                                            voltageError = null
                                            chuanFromVoltageMm = mm
                                            endSideLabel = side
                                        }
                                    )
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
                            if (msg != null) Text(text = msg)
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val v = voltageText.toDoubleOrNull()
                                if (v == null) {
                                    voltageError = context.getString(R.string.invalid_input)
                                    chuanFromVoltageMm = null
                                    endSideLabel = null
                                } else {
                                    applyReverseCalc(
                                        voltageV = v,
                                        zeroVoltageV = zeroVoltageV,
                                        invalidNegativeMessage = context.getString(R.string.negative_voltage),
                                        sideZero = context.getString(R.string.side_zero),
                                        sideFar = context.getString(R.string.side_far),
                                        sideNear = context.getString(R.string.side_near),
                                        onError = { msg ->
                                            voltageError = msg
                                            chuanFromVoltageMm = null
                                            endSideLabel = null
                                        },
                                        onResult = { mm, side ->
                                            voltageError = null
                                            chuanFromVoltageMm = mm
                                            endSideLabel = side
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = context.getString(R.string.calculate))
                        }

                        Button(
                            onClick = {
                                voltageText = ""
                                voltageError = null
                                chuanFromVoltageMm = null
                                endSideLabel = null
                            }
                        ) {
                            Text(text = context.getString(R.string.reset))
                        }
                    }

                    ReverseResultCard(
                        chuanMm = chuanFromVoltageMm,
                        endSideLabel = endSideLabel
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = context.getString(R.string.history), style = MaterialTheme.typography.titleMedium)

            if (history.isEmpty()) {
                Text(text = context.getString(R.string.empty_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                history.forEach { record ->
                    HistoryRow(record = record)
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text(text = context.getString(R.string.close)) }
            },
            title = { Text(text = context.getString(R.string.help_title)) },
            text = { Text(text = context.getString(R.string.help_text)) }
        )
    }
}

@Composable
private fun UnitToggle(
    selected: ChuanUnit,
    onSelected: (ChuanUnit) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val context = androidx.compose.ui.platform.LocalContext.current
        Button(
            onClick = { onSelected(ChuanUnit.MM) },
            enabled = selected != ChuanUnit.MM,
        ) {
            Text(text = context.getString(R.string.unit_mm))
        }
        Button(
            onClick = { onSelected(ChuanUnit.DAO) },
            enabled = selected != ChuanUnit.DAO,
        ) {
            Text(text = context.getString(R.string.unit_dao))
        }
    }
}

@Composable
private fun ResultCard(title: String, value: Double?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                val txt = value?.let { formatVolt(it) } ?: androidx.compose.ui.platform.LocalContext.current.getString(R.string.empty_placeholder)
                Text(text = "$txt V", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun ReverseResultCard(chuanMm: Double?, endSideLabel: String?) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = context.getString(R.string.reverse_result_label), style = MaterialTheme.typography.titleMedium)
            val mm = chuanMm?.let { formatMm3(it) } ?: context.getString(R.string.empty_placeholder)
            Text(text = "$mm ${context.getString(R.string.unit_mm)}", style = MaterialTheme.typography.headlineSmall)
            val side = endSideLabel ?: context.getString(R.string.empty_placeholder)
            Text(
                text = context.getString(R.string.reverse_side_label, side),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRow(record: CalcRecord) {
    val time = remember(record.epochMillis) {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.epochMillis))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatVolt(v: Double): String = String.format(Locale.US, "%.2f", v)

private fun formatVolt2(v: Double): String = String.format(Locale.US, "%.2f", v)

private fun formatMm(mm: Double): String = String.format(Locale.US, "%.2f", mm)

private fun formatMm3(mm: Double): String = String.format(Locale.US, "%.3f", mm)

private fun formatSensitivity(vPerMm: Double): String = String.format(Locale.US, "%.2f", vPerMm)

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

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("probe_calc", text))
}

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
