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
import androidx.compose.material.icons.filled.Whatshot
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
import com.kidsafe.probe.its90.ThermocoupleCalculator
import com.kidsafe.probe.its90.ThermocoupleType
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleDescriptor
import com.kidsafe.probe.modules.ModuleHost
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ThermocoupleModule : FeatureModule {
    private enum class Mode {
        TEMP_TO_VALUE,
        VALUE_TO_TEMP,
    }

    override val descriptor = ModuleDescriptor(
        id = "thermocouple",
        titleRes = R.string.feature_thermocouple,
        icon = Icons.Default.Whatshot,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(host: ModuleHost, state: Any?) {
        val context = host.context
        val appContext = remember(context) { context.applicationContext }
        val repo = remember(appContext) { Its90Repository(appContext) }
        val calc = remember(repo) { ThermocoupleCalculator(repo) }
        val historyStore = remember(appContext) { ThermoHistoryStore(appContext) }
        val history by historyStore.history.collectAsState(initial = emptyList())
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        var type by remember { mutableStateOf(ThermocoupleType.K) }
        var typeExpanded by remember { mutableStateOf(false) }
        var mode by remember { mutableStateOf(Mode.TEMP_TO_VALUE) }
        var useCjc by remember { mutableStateOf(true) }

        var tempText by remember { mutableStateOf("") }
        var mvText by remember { mutableStateOf("") }
        var cjText by remember { mutableStateOf("25") }

        var resultText by remember { mutableStateOf<String?>(null) }
        var errorText by remember { mutableStateOf<String?>(null) }

        val lookup = remember(type) { repo.thermocoupleTable(type) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = context.getString(R.string.tc_title), style = MaterialTheme.typography.titleMedium)

                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                    OutlinedTextField(
                        value = type.displayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text(text = context.getString(R.string.sensor_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        supportingText = { Text(text = context.getString(R.string.hint_tc_type)) },
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        ThermocoupleType.entries.forEach { option ->
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
                        title = context.getString(R.string.mode_temp_to_mv),
                        hint = context.getString(R.string.hint_mode_temp_to_mv),
                        selected = mode == Mode.TEMP_TO_VALUE,
                        onClick = {
                            mode = Mode.TEMP_TO_VALUE
                            resultText = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        title = context.getString(R.string.mode_mv_to_temp),
                        hint = context.getString(R.string.hint_mode_mv_to_temp),
                        selected = mode == Mode.VALUE_TO_TEMP,
                        onClick = {
                            mode = Mode.VALUE_TO_TEMP
                            resultText = null
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
                        title = context.getString(R.string.enabled),
                        hint = context.getString(R.string.hint_cjc_on),
                        selected = useCjc,
                        onClick = {
                            useCjc = true
                            resultText = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ChoiceButton(
                        title = context.getString(R.string.disabled),
                        hint = context.getString(R.string.hint_cjc_off),
                        selected = !useCjc,
                        onClick = {
                            useCjc = false
                            resultText = null
                            errorText = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

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
                                    R.string.hint_tc_hot_temp,
                                    context.getString(R.string.valid_temp_range, format4(lookup.minTemperatureC), format4(lookup.maxTemperatureC))
                                )
                            )
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = mvText,
                        onValueChange = { mvText = filterNumber(it, allowNegative = true) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.input_mv)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = context.getString(R.string.unit_mv)) },
                        supportingText = {
                            Text(
                                text = context.getString(
                                    R.string.hint_tc_mv,
                                    context.getString(R.string.valid_mv_range, format4(lookup.minValue), format4(lookup.maxValue))
                                )
                            )
                        }
                    )
                }

                if (useCjc) {
                    OutlinedTextField(
                        value = cjText,
                        onValueChange = { cjText = filterNumber(it, allowNegative = true) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.input_cj_temperature)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = context.getString(R.string.unit_c)) },
                        supportingText = {
                            Text(
                                text = context.getString(
                                    R.string.hint_tc_cj,
                                    context.getString(R.string.valid_temp_range, format4(lookup.minTemperatureC), format4(lookup.maxTemperatureC))
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
                                    val cj = if (useCjc) cjText.toDoubleOrNull() else null
                                    when (mode) {
                                        Mode.TEMP_TO_VALUE -> {
                                            val hot = tempText.toDoubleOrNull()
                                            if (hot == null) {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            if (useCjc && cj == null) {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val mv = calc.temperatureToMillivolt(type, hot, cj)
                                            if (mv.isNaN()) {
                                                errorText = context.getString(
                                                    R.string.out_of_range_with_hint,
                                                    context.getString(R.string.valid_temp_range, format4(lookup.minTemperatureC), format4(lookup.maxTemperatureC))
                                                )
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val out = "${format4(mv)} ${context.getString(R.string.unit_mv)}"
                                            resultText = out
                                            errorText = null
                                            val desc = buildString {
                                                append(context.getString(R.string.tc_label, type.displayName))
                                                append("  ")
                                                append("${format4(hot)}${context.getString(R.string.unit_c)}")
                                                if (useCjc) append("  ${context.getString(R.string.cj_short, format4(cj!!))}")
                                                append("  →  ")
                                                append(out)
                                            }
                                            scope.launch { historyStore.add(desc) }
                                        }

                                        Mode.VALUE_TO_TEMP -> {
                                            val mvIn = mvText.toDoubleOrNull()
                                            if (mvIn == null) {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            if (useCjc && cj == null) {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val hot = calc.millivoltToTemperature(type, mvIn, cj)
                                            if (hot.isNaN()) {
                                                errorText = context.getString(
                                                    R.string.out_of_range_with_hint,
                                                    context.getString(R.string.valid_mv_range, format4(lookup.minValue), format4(lookup.maxValue))
                                                )
                                                resultText = null
                                                return@SecondaryButton
                                            }
                                            val out = "${format4(hot)} ${context.getString(R.string.unit_c)}"
                                            resultText = out
                                            errorText = null
                                            val desc = buildString {
                                                append(context.getString(R.string.tc_label, type.displayName))
                                                append("  ")
                                                append("${format4(mvIn)}${context.getString(R.string.unit_mv)}")
                                                if (useCjc) append("  ${context.getString(R.string.cj_short, format4(cj!!))}")
                                                append("  →  ")
                                                append(out)
                                            }
                                            scope.launch { historyStore.add(desc) }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ThermocoupleModule", "calc failed", e)
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
                                host.copyToClipboard("thermo_calc", v)
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

                    ThermoResultRow(title = context.getString(R.string.output_label), value = resultText)
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
private fun ThermoResultRow(title: String, value: String?) {
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
