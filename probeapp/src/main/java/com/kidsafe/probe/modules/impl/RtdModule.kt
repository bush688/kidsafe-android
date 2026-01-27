package com.kidsafe.probe.modules.impl

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kidsafe.probe.R
import com.kidsafe.probe.ThermoHistoryRecord
import com.kidsafe.probe.ThermoHistoryStore
import com.kidsafe.probe.its90.Its90Repository
import com.kidsafe.probe.its90.RtdCalculator
import com.kidsafe.probe.its90.RtdType
import com.kidsafe.probe.its90.RtdWiring
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleDescriptor
import com.kidsafe.probe.modules.ModuleHost
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RtdModule : FeatureModule {
    private enum class Mode {
        TEMP_TO_VALUE,
        VALUE_TO_TEMP,
    }

    override val descriptor = ModuleDescriptor(
        id = "rtd",
        titleRes = R.string.feature_rtd,
        icon = Icons.Default.Tune,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(host: ModuleHost, state: Any?) {
        val context = host.context
        val appContext = remember(context) { context.applicationContext }
        val repo = remember(appContext) { Its90Repository(appContext) }
        val calc = remember(repo) { RtdCalculator(repo) }
        val historyStore = remember(appContext) { ThermoHistoryStore(appContext) }
        val history by historyStore.history.collectAsState(initial = emptyList())
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        var type by remember { mutableStateOf(RtdType.Pt100) }
        var typeExpanded by remember { mutableStateOf(false) }
        var mode by remember { mutableStateOf(Mode.TEMP_TO_VALUE) }

        var wiring by remember { mutableStateOf(RtdWiring.WIRE_3) }
        var wiringExpanded by remember { mutableStateOf(false) }
        var leadText by remember { mutableStateOf("0") }

        var tempText by remember { mutableStateOf("") }
        var ohmText by remember { mutableStateOf("") }

        var resultText by remember { mutableStateOf<String?>(null) }
        var errorText by remember { mutableStateOf<String?>(null) }

        val lookup = remember(type) { repo.rtdTable(type) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = context.getString(R.string.rtd_title), style = MaterialTheme.typography.titleMedium)

                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text(text = context.getString(R.string.sensor_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        supportingText = { Text(text = context.getString(R.string.hint_rtd_type)) },
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        RtdType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option.displayName) },
                                onClick = {
                                    type = option
                                    typeExpanded = false
                                    resultText = null
                                    errorText = null
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChoiceButton(
                        title = context.getString(R.string.mode_temp_to_r),
                        hint = context.getString(R.string.hint_mode_temp_to_r),
                        selected = mode == Mode.TEMP_TO_VALUE,
                        onClick = {
                            mode = Mode.TEMP_TO_VALUE
                            resultText = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        title = context.getString(R.string.mode_r_to_temp),
                        hint = context.getString(R.string.hint_mode_r_to_temp),
                        selected = mode == Mode.VALUE_TO_TEMP,
                        onClick = {
                            mode = Mode.VALUE_TO_TEMP
                            resultText = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(expanded = wiringExpanded, onExpandedChange = { wiringExpanded = !wiringExpanded }) {
                    OutlinedTextField(
                        value = wiring.displayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text(text = context.getString(R.string.wiring)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wiringExpanded) },
                        supportingText = { Text(text = context.getString(R.string.hint_wiring)) },
                    )
                    ExposedDropdownMenu(expanded = wiringExpanded, onDismissRequest = { wiringExpanded = false }) {
                        RtdWiring.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option.displayName) },
                                onClick = {
                                    wiring = option
                                    wiringExpanded = false
                                    resultText = null
                                    errorText = null
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = leadText,
                    onValueChange = { leadText = filterNumber(it, allowNegative = false) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = context.getString(R.string.lead_resistance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = { Text(text = type.unitLabel) },
                    supportingText = { Text(text = context.getString(R.string.hint_lead_resistance)) },
                )

                if (mode == Mode.TEMP_TO_VALUE) {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = filterNumber(it, allowNegative = true) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.input_temperature)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = context.getString(R.string.unit_c)) },
                        supportingText = {
                            Text(
                                text = context.getString(
                                    R.string.hint_rtd_temp,
                                    context.getString(R.string.valid_temp_range, format4(lookup.minTemperatureC), format4(lookup.maxTemperatureC))
                                )
                            )
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = ohmText,
                        onValueChange = { ohmText = filterNumber(it, allowNegative = false) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.input_resistance)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = type.unitLabel) },
                        supportingText = {
                            Text(
                                text = context.getString(
                                    R.string.hint_rtd_resistance,
                                    context.getString(R.string.valid_ohm_range, format4(lookup.minValue), format4(lookup.maxValue))
                                )
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SecondaryButton(
                            title = context.getString(R.string.calculate),
                            onClick = {
                                try {
                                    val lead = leadText.toDoubleOrNull()
                                    if (lead == null) {
                                        errorText = context.getString(R.string.invalid_input)
                                        resultText = null
                                        return@SecondaryButton
                                    }
                                    when (mode) {
                                        Mode.TEMP_TO_VALUE -> {
                                            val t = tempText.toDoubleOrNull()
                                            if (t == null) {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val r = calc.temperatureToResistanceOhm(type, t, wiring, lead)
                                            if (r.isNaN()) {
                                                errorText = context.getString(
                                                    R.string.out_of_range_with_hint,
                                                    context.getString(R.string.valid_temp_range, format4(lookup.minTemperatureC), format4(lookup.maxTemperatureC))
                                                )
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val out = "${format4(r)} ${type.unitLabel}"
                                            resultText = out
                                            errorText = null
                                            val desc = "${context.getString(R.string.rtd_label, type.displayName)}  ${format4(t)}${context.getString(R.string.unit_c)}  →  $out"
                                            scope.launch { historyStore.add(desc) }
                                        }

                                        Mode.VALUE_TO_TEMP -> {
                                            val rIn = ohmText.toDoubleOrNull()
                                            if (rIn == null) {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val t = calc.resistanceOhmToTemperature(type, rIn, wiring, lead)
                                            if (t.isNaN()) {
                                                errorText = context.getString(
                                                    R.string.out_of_range_with_hint,
                                                    context.getString(R.string.valid_ohm_range, format4(lookup.minValue), format4(lookup.maxValue))
                                                )
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val out = "${format4(t)} ${context.getString(R.string.unit_c)}"
                                            resultText = out
                                            errorText = null
                                            val desc = "${context.getString(R.string.rtd_label, type.displayName)}  ${format4(rIn)}${type.unitLabel}  →  $out"
                                            scope.launch { historyStore.add(desc) }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("RtdModule", "calc failed", e)
                                    errorText = context.getString(R.string.calc_failed)
                                    resultText = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = context.getString(R.string.hint_calculate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                val v = resultText ?: return@IconButton
                                host.copyToClipboard("rtd_calc", v)
                                scope.launch { host.showMessage(context.getString(R.string.copied)) }
                            },
                            enabled = resultText != null
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

                val err = errorText
                if (err != null) {
                    Text(text = err, color = MaterialTheme.colorScheme.error)
                }

                    RtdResultRow(title = context.getString(R.string.output_label), value = resultText)
                }
            }

            ThermoHistoryPanel(
                title = context.getString(R.string.history),
                history = history,
                onClear = { scope.launch { historyStore.clear() } },
            )
        }
    }
}

@Composable
private fun RtdResultRow(title: String, value: String?) {
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
private fun ThermoHistoryPanel(
    title: String,
    history: List<ThermoHistoryRecord>,
    onClear: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            SecondaryButton(title = context.getString(R.string.clear_history), onClick = onClear)
        }
        if (history.isEmpty()) {
            Text(text = context.getString(R.string.empty_placeholder))
        } else {
            history.forEach { row ->
                ThermoHistoryRow(row)
            }
        }
    }
}

@Composable
private fun ThermoHistoryRow(record: ThermoHistoryRecord) {
    val time = remember(record.epochMillis) {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.epochMillis))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.text, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

private fun format4(v: Double): String = String.format(Locale.US, "%.4f", v)
