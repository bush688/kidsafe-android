package com.kidsafe.probe.modules.impl

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.kidsafe.probe.ModuleInputStore
import com.kidsafe.probe.R
import com.kidsafe.probe.cable.AreaUnit
import com.kidsafe.probe.cable.AwgSize
import com.kidsafe.probe.cable.CableCalcStore
import com.kidsafe.probe.cable.CableCalculator
import com.kidsafe.probe.cable.CableMaterial
import com.kidsafe.probe.cable.CableReport
import com.kidsafe.probe.cable.CableResistanceInput
import com.kidsafe.probe.cable.CircuitType
import com.kidsafe.probe.cable.LengthUnit
import com.kidsafe.probe.cable.ReportExporter
import com.kidsafe.probe.cable.VoltageDropWiring
import com.kidsafe.probe.cable.CableScenario
import com.kidsafe.probe.modules.FeatureModule
import com.kidsafe.probe.modules.ModuleDescriptor
import com.kidsafe.probe.modules.ModuleHost
import com.kidsafe.probe.ui.ChoiceButton
import com.kidsafe.probe.ui.SecondaryButton
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.math.PI

private enum class CableTab { RESISTANCE, DROP }

object CableDropModule : FeatureModule {
    override val descriptor = ModuleDescriptor(
        id = "cable_drop",
        titleRes = R.string.feature_cable_drop,
        icon = Icons.Default.ElectricalServices,
    )

    override val useHostScroll: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content(host: ModuleHost, state: Any?) {
        val context = host.context
        val appContext = remember(context) { context.applicationContext }
        val store = remember(appContext) { CableCalcStore(appContext) }
        val inputStore = remember(appContext) { ModuleInputStore(appContext) }
        val presets by store.presets.collectAsState(initial = emptyList())
        val last by store.lastScenario.collectAsState(initial = null)
        val scope = rememberCoroutineScope()
        var inputsLoaded by remember { mutableStateOf(false) }
        var loadedFromInputStore by remember { mutableStateOf(false) }

        var tab by remember { mutableStateOf(CableTab.DROP) }

        var material by remember { mutableStateOf(CableMaterial.COPPER) }
        var areaUnit by remember { mutableStateOf(AreaUnit.MM2) }
        var mm2Mode by remember { mutableStateOf("standard") }
        var mm2Standard by remember { mutableStateOf(2.5) }
        var mm2StandardExpanded by remember { mutableStateOf(false) }
        var mm2Text by remember { mutableStateOf("2.5") }
        var awgGauge by remember { mutableStateOf(12) }
        var awgExpanded by remember { mutableStateOf(false) }

        var lengthUnit by remember { mutableStateOf(LengthUnit.METER) }
        var lengthUnitExpanded by remember { mutableStateOf(false) }
        var lengthText by remember { mutableStateOf("10") }
        var tempText by remember { mutableStateOf("20") }

        var circuitType by remember { mutableStateOf(CircuitType.DC) }
        var powerFactorText by remember { mutableStateOf("0.90") }
        var frequencyHzText by remember { mutableStateOf("50") }
        var inductanceMhPerKmText by remember { mutableStateOf("0") }

        var wiring by remember { mutableStateOf(VoltageDropWiring.SINGLE_PHASE_2W) }
        var wiringExpanded by remember { mutableStateOf(false) }
        var currentText by remember { mutableStateOf("10") }
        var supplyText by remember { mutableStateOf("220") }
        var limitMode by remember { mutableStateOf("3") }
        var limitText by remember { mutableStateOf("3") }

        var scenarioName by remember { mutableStateOf("") }
        var selectedPresetName by remember { mutableStateOf<String?>(null) }

        var resultText by remember { mutableStateOf<String?>(null) }
        var reportText by remember { mutableStateOf<String?>(null) }
        var errorText by remember { mutableStateOf<String?>(null) }
        val awg = remember(awgGauge) { AwgSize(awgGauge) }

        LaunchedEffect(Unit) {
            val obj = inputStore.load(descriptor.id)
            if (obj != null) {
                loadedFromInputStore = true
                runCatching { CableTab.valueOf(obj.optString("tab", tab.name)) }.getOrNull()?.let { tab = it }
                runCatching { CableMaterial.valueOf(obj.optString("material", material.name)) }.getOrNull()?.let { material = it }
                runCatching { AreaUnit.valueOf(obj.optString("areaUnit", areaUnit.name)) }.getOrNull()?.let { areaUnit = it }
                mm2Mode = obj.optString("mm2Mode", mm2Mode)
                obj.optDouble("mm2Standard").takeIf { it.isFinite() && it > 0.0 }?.let { mm2Standard = it }
                mm2Text = obj.optString("mm2Text", mm2Text)
                obj.optInt("awgGauge").takeIf { it > 0 }?.let { awgGauge = it }
                runCatching { LengthUnit.valueOf(obj.optString("lengthUnit", lengthUnit.name)) }.getOrNull()?.let { lengthUnit = it }
                lengthText = obj.optString("lengthText", lengthText)
                tempText = obj.optString("tempText", tempText)
                runCatching { CircuitType.valueOf(obj.optString("circuitType", circuitType.name)) }.getOrNull()?.let { circuitType = it }
                powerFactorText = obj.optString("powerFactorText", powerFactorText)
                frequencyHzText = obj.optString("frequencyHzText", frequencyHzText)
                inductanceMhPerKmText = obj.optString("inductanceMhPerKmText", inductanceMhPerKmText)
                runCatching { VoltageDropWiring.valueOf(obj.optString("wiring", wiring.name)) }.getOrNull()?.let { wiring = it }
                currentText = obj.optString("currentText", currentText)
                supplyText = obj.optString("supplyText", supplyText)
                limitMode = obj.optString("limitMode", limitMode)
                limitText = obj.optString("limitText", limitText)
                scenarioName = obj.optString("scenarioName", scenarioName)
                selectedPresetName = obj.optString("selectedPresetName").takeIf { it.isNotBlank() }
            }
            inputsLoaded = true
        }

        LaunchedEffect(last, inputsLoaded, loadedFromInputStore) {
            if (!inputsLoaded || loadedFromInputStore) return@LaunchedEffect
            val s = last ?: return@LaunchedEffect
            material = s.material
            areaUnit = s.areaUnit
            mm2Text = s.areaMm2?.toString() ?: mm2Text
            awgGauge = s.awgGauge ?: awgGauge
            lengthText = s.length.toString()
            lengthUnit = s.lengthUnit
            tempText = s.temperatureC.toString()
            circuitType = s.circuitType
            wiring = s.wiring
            currentText = s.currentA.toString()
            supplyText = s.supplyV.toString()
            limitText = s.limitPercent.toString()
            limitMode = if (absDiff(s.limitPercent, 3.0) < 1e-6) "3" else if (absDiff(s.limitPercent, 5.0) < 1e-6) "5" else "custom"
            s.powerFactor?.let { powerFactorText = it.toString() }
            s.frequencyHz?.let { frequencyHzText = it.toString() }
            s.inductanceMhPerKm?.let { inductanceMhPerKmText = it.toString() }
        }

        LaunchedEffect(
            tab,
            material,
            areaUnit,
            mm2Mode,
            mm2Standard,
            mm2Text,
            awgGauge,
            lengthUnit,
            lengthText,
            tempText,
            circuitType,
            powerFactorText,
            frequencyHzText,
            inductanceMhPerKmText,
            wiring,
            currentText,
            supplyText,
            limitMode,
            limitText,
            scenarioName,
            selectedPresetName,
            inputsLoaded,
        ) {
            if (!inputsLoaded) return@LaunchedEffect
            inputStore.save(
                descriptor.id,
                JSONObject()
                    .put("tab", tab.name)
                    .put("material", material.name)
                    .put("areaUnit", areaUnit.name)
                    .put("mm2Mode", mm2Mode)
                    .put("mm2Standard", mm2Standard)
                    .put("mm2Text", mm2Text)
                    .put("awgGauge", awgGauge)
                    .put("lengthUnit", lengthUnit.name)
                    .put("lengthText", lengthText)
                    .put("tempText", tempText)
                    .put("circuitType", circuitType.name)
                    .put("powerFactorText", powerFactorText)
                    .put("frequencyHzText", frequencyHzText)
                    .put("inductanceMhPerKmText", inductanceMhPerKmText)
                    .put("wiring", wiring.name)
                    .put("currentText", currentText)
                    .put("supplyText", supplyText)
                    .put("limitMode", limitMode)
                    .put("limitText", limitText)
                    .put("scenarioName", scenarioName)
                    .put("selectedPresetName", selectedPresetName ?: "")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == CableTab.RESISTANCE,
                    onClick = { tab = CableTab.RESISTANCE },
                    text = { Text(text = context.getString(R.string.tab_cable_resistance)) }
                )
                Tab(
                    selected = tab == CableTab.DROP,
                    onClick = { tab = CableTab.DROP },
                    text = { Text(text = context.getString(R.string.tab_cable_drop)) }
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(text = context.getString(R.string.cable_title), style = MaterialTheme.typography.titleMedium)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChoiceButton(
                            title = context.getString(R.string.material_copper),
                            hint = context.getString(R.string.hint_material_copper),
                            selected = material == CableMaterial.COPPER,
                            onClick = { material = CableMaterial.COPPER },
                            modifier = Modifier.weight(1f)
                        )
                        ChoiceButton(
                            title = context.getString(R.string.material_aluminum),
                            hint = context.getString(R.string.hint_material_aluminum),
                            selected = material == CableMaterial.ALUMINUM,
                            onClick = { material = CableMaterial.ALUMINUM },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChoiceButton(
                            title = AreaUnit.MM2.displayName,
                            hint = context.getString(R.string.hint_area_mm2),
                            selected = areaUnit == AreaUnit.MM2,
                            onClick = { areaUnit = AreaUnit.MM2 },
                            modifier = Modifier.weight(1f),
                        )
                        ChoiceButton(
                            title = AreaUnit.AWG.displayName,
                            hint = context.getString(R.string.hint_area_awg),
                            selected = areaUnit == AreaUnit.AWG,
                            onClick = { areaUnit = AreaUnit.AWG },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (areaUnit == AreaUnit.MM2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChoiceButton(
                                title = context.getString(R.string.standard),
                                hint = context.getString(R.string.hint_area_common),
                                selected = mm2Mode == "standard",
                                onClick = {
                                    mm2Mode = "standard"
                                    mm2Text = fmt4(mm2Standard).trimEnd('0').trimEnd('.')
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceButton(
                                title = context.getString(R.string.manual),
                                hint = context.getString(R.string.hint_area_mm2),
                                selected = mm2Mode == "manual",
                                onClick = { mm2Mode = "manual" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (mm2Mode == "standard") {
                            ExposedDropdownMenuBox(
                                expanded = mm2StandardExpanded,
                                onExpandedChange = { mm2StandardExpanded = !mm2StandardExpanded }
                            ) {
                                OutlinedTextField(
                                    value = fmt4(mm2Standard).trimEnd('0').trimEnd('.') ,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    label = { Text(text = context.getString(R.string.input_area)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mm2StandardExpanded) },
                                    suffix = { Text(text = AreaUnit.MM2.displayName) },
                                )
                                ExposedDropdownMenu(
                                    expanded = mm2StandardExpanded,
                                    onDismissRequest = { mm2StandardExpanded = false }
                                ) {
                                    standardMm2Sizes().forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(text = fmt4(option).trimEnd('0').trimEnd('.')) },
                                            onClick = {
                                                mm2Standard = option
                                                mm2Text = fmt4(option).trimEnd('0').trimEnd('.')
                                                mm2StandardExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = mm2Text,
                                onValueChange = { mm2Text = filterNumber(it, allowNegative = false) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = context.getString(R.string.input_area)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = AreaUnit.MM2.displayName) },
                                supportingText = { Text(text = context.getString(R.string.hint_area_common)) },
                            )
                        }
                    } else {
                        ExposedDropdownMenuBox(expanded = awgExpanded, onExpandedChange = { awgExpanded = !awgExpanded }) {
                            OutlinedTextField(
                                value = awg.displayName,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                label = { Text(text = context.getString(R.string.input_awg)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = awgExpanded) },
                                supportingText = { Text(text = context.getString(R.string.hint_awg_area, fmt4(awg.areaMm2()))) },
                            )
                            ExposedDropdownMenu(expanded = awgExpanded, onDismissRequest = { awgExpanded = false }) {
                                AwgSize.common().forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option.displayName) },
                                        onClick = {
                                            awgGauge = option.gauge
                                            awgExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = lengthText,
                            onValueChange = { lengthText = filterNumber(it, allowNegative = false) },
                            modifier = Modifier.weight(1.2f),
                            label = { Text(text = context.getString(R.string.input_length_oneway)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            supportingText = { Text(text = context.getString(R.string.hint_length_oneway)) },
                        )
                        ExposedDropdownMenuBox(
                            expanded = lengthUnitExpanded,
                            onExpandedChange = { lengthUnitExpanded = !lengthUnitExpanded },
                            modifier = Modifier.weight(0.8f),
                        ) {
                            OutlinedTextField(
                                value = lengthUnit.displayName,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                label = { Text(text = context.getString(R.string.unit_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lengthUnitExpanded) },
                                singleLine = true,
                            )
                            ExposedDropdownMenu(expanded = lengthUnitExpanded, onDismissRequest = { lengthUnitExpanded = false }) {
                                LengthUnit.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option.displayName) },
                                        onClick = {
                                            lengthUnit = option
                                            lengthUnitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = filterNumber(it, allowNegative = true) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = context.getString(R.string.input_temperature)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(text = context.getString(R.string.unit_c)) },
                        supportingText = { Text(text = context.getString(R.string.hint_cable_temperature)) },
                    )

                    if (tab == CableTab.DROP) {
                        ExposedDropdownMenuBox(expanded = wiringExpanded, onExpandedChange = { wiringExpanded = !wiringExpanded }) {
                            OutlinedTextField(
                                value = wiring.displayName,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                label = { Text(text = context.getString(R.string.wiring)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wiringExpanded) },
                                supportingText = { Text(text = context.getString(R.string.hint_wiring_drop)) },
                            )
                            ExposedDropdownMenu(expanded = wiringExpanded, onDismissRequest = { wiringExpanded = false }) {
                                VoltageDropWiring.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(text = option.displayName) },
                                        onClick = {
                                            wiring = option
                                            wiringExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChoiceButton(
                                title = context.getString(R.string.circuit_dc),
                                hint = context.getString(R.string.hint_circuit_dc),
                                selected = circuitType == CircuitType.DC,
                                onClick = { circuitType = CircuitType.DC },
                                modifier = Modifier.weight(1f),
                            )
                            ChoiceButton(
                                title = context.getString(R.string.circuit_ac),
                                hint = context.getString(R.string.hint_circuit_ac),
                                selected = circuitType == CircuitType.AC,
                                onClick = { circuitType = CircuitType.AC },
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (circuitType == CircuitType.AC) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = powerFactorText,
                                    onValueChange = { powerFactorText = filterNumber(it, allowNegative = false) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(text = context.getString(R.string.input_power_factor)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = frequencyHzText,
                                    onValueChange = { frequencyHzText = filterNumber(it, allowNegative = false) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text(text = context.getString(R.string.input_frequency)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    suffix = { Text(text = "Hz") },
                                    singleLine = true,
                                )
                            }

                            OutlinedTextField(
                                value = inductanceMhPerKmText,
                                onValueChange = { inductanceMhPerKmText = filterNumber(it, allowNegative = false) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = context.getString(R.string.input_inductance_per_km)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = "mH/km") },
                                supportingText = { Text(text = context.getString(R.string.hint_inductance_per_km)) },
                                singleLine = true,
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { currentText = filterNumber(it, allowNegative = false) },
                                modifier = Modifier.weight(1f),
                                label = { Text(text = context.getString(R.string.input_current)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = context.getString(R.string.unit_a)) },
                            )
                            OutlinedTextField(
                                value = supplyText,
                                onValueChange = { supplyText = filterNumber(it, allowNegative = false) },
                                modifier = Modifier.weight(1f),
                                label = { Text(text = context.getString(R.string.input_voltage)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = context.getString(R.string.unit_v)) },
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChoiceButton(
                                title = "3%",
                                hint = context.getString(R.string.hint_limit_3),
                                selected = limitMode == "3",
                                onClick = {
                                    limitMode = "3"
                                    limitText = "3"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceButton(
                                title = "5%",
                                hint = context.getString(R.string.hint_limit_5),
                                selected = limitMode == "5",
                                onClick = {
                                    limitMode = "5"
                                    limitText = "5"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceButton(
                                title = context.getString(R.string.custom),
                                hint = context.getString(R.string.hint_limit_custom),
                                selected = limitMode == "custom",
                                onClick = { limitMode = "custom" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (limitMode == "custom") {
                            OutlinedTextField(
                                value = limitText,
                                onValueChange = { limitText = filterNumber(it, allowNegative = false) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(text = context.getString(R.string.input_limit_percent)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = { Text(text = "%") },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SecondaryButton(
                            title = context.getString(R.string.calculate),
                            onClick = {
                                try {
                                    val length = lengthText.toDoubleOrNull()
                                    val temp = tempText.toDoubleOrNull()
                                    val areaMm2 = if (areaUnit == AreaUnit.MM2) mm2Text.toDoubleOrNull() else awg.areaMm2()
                                    if (length == null || temp == null || areaMm2 == null || areaMm2 <= 0.0) {
                                        errorText = context.getString(R.string.invalid_input)
                                        resultText = null
                                        reportText = null
                                        return@SecondaryButton
                                    }

                                    val rInput = CableResistanceInput(
                                        material = material,
                                        lengthOneWay = length,
                                        lengthUnit = lengthUnit,
                                        areaUnit = AreaUnit.MM2,
                                        areaMm2 = areaMm2,
                                        awg = null,
                                        temperatureC = temp,
                                    )
                                    val r = CableCalculator.dcResistance(rInput)

                                    val powerFactor = powerFactorText.toDoubleOrNull()?.takeIf { it.isFinite() }
                                    val frequencyHz = frequencyHzText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
                                    val inductanceMhPerKm = inductanceMhPerKmText.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }

                                    val scenario = CableScenario(
                                        name = scenarioName.ifBlank { "默认" },
                                        material = material,
                                        areaUnit = areaUnit,
                                        areaMm2 = if (areaUnit == AreaUnit.MM2) areaMm2 else null,
                                        awgGauge = if (areaUnit == AreaUnit.AWG) awg.gauge else null,
                                        length = length,
                                        lengthUnit = lengthUnit,
                                        temperatureC = temp,
                                        circuitType = circuitType,
                                        wiring = wiring,
                                        currentA = currentText.toDoubleOrNull() ?: 0.0,
                                        supplyV = supplyText.toDoubleOrNull() ?: 220.0,
                                        limitPercent = limitText.toDoubleOrNull() ?: 3.0,
                                        powerFactor = if (circuitType == CircuitType.AC) powerFactor else null,
                                        frequencyHz = if (circuitType == CircuitType.AC) frequencyHz else null,
                                        inductanceMhPerKm = if (circuitType == CircuitType.AC) inductanceMhPerKm else null,
                                    )
                                    scope.launch { store.saveLast(scenario) }

                                    if (tab == CableTab.RESISTANCE) {
                                        resultText = context.getString(
                                            R.string.cable_resistance_result,
                                            fmt6(r.resistancePerConductorOhm),
                                            fmt6(r.resistancePerKmOhm),
                                        )
                                        reportText = CableReport.buildResistanceReport(
                                            input = CableResistanceInput(
                                                material = material,
                                                lengthOneWay = length,
                                                lengthUnit = lengthUnit,
                                                areaUnit = areaUnit,
                                                areaMm2 = if (areaUnit == AreaUnit.MM2) areaMm2 else null,
                                                awg = if (areaUnit == AreaUnit.AWG) AwgSize(awgGauge) else null,
                                                temperatureC = temp,
                                            ),
                                            result = r
                                        )
                                    } else {
                                        val currentA = currentText.toDoubleOrNull()
                                        val supplyV = supplyText.toDoubleOrNull()
                                        val limitP = limitText.toDoubleOrNull()
                                        if (currentA == null || supplyV == null || limitP == null || limitP <= 0.0) {
                                            errorText = context.getString(R.string.invalid_input)
                                            resultText = null
                                            reportText = null
                                            return@SecondaryButton
                                        }
                                        val drop = CableCalculator.voltageDrop(
                                            wiring = wiring,
                                            supplyVoltageV = supplyV,
                                            loadCurrentA = currentA,
                                            resistancePerConductorOhm = r.resistancePerConductorOhm,
                                            limitPercent = limitP,
                                        )

                                        val lengthM = length * lengthUnit.metersPerUnit
                                        val lengthKm = lengthM / 1000.0
                                        val xPerConductorOhm = if (circuitType == CircuitType.AC) {
                                            val f = frequencyHz ?: 0.0
                                            val l = inductanceMhPerKm ?: 0.0
                                            val lTotalH = l * lengthKm / 1000.0
                                            2.0 * PI * f * lTotalH
                                        } else 0.0
                                        val pfForAc = if (circuitType == CircuitType.AC) {
                                            powerFactor?.takeIf { it in 0.0..1.0 } ?: run {
                                                errorText = context.getString(R.string.invalid_input)
                                                resultText = null
                                                reportText = null
                                                return@SecondaryButton
                                            }
                                        } else null
                                        val dropFinal = if (circuitType == CircuitType.AC) {
                                            CableCalculator.voltageDropAc(
                                                wiring = wiring,
                                                supplyVoltageV = supplyV,
                                                loadCurrentA = currentA,
                                                resistancePerConductorOhm = r.resistancePerConductorOhm,
                                                reactancePerConductorOhm = xPerConductorOhm,
                                                powerFactor = pfForAc ?: 1.0,
                                                limitPercent = limitP,
                                            )
                                        } else {
                                            drop
                                        }
                                        val status = if (dropFinal.isWithinLimit) context.getString(R.string.ok) else context.getString(R.string.exceed)

                                        resultText = if (circuitType == CircuitType.AC) {
                                            context.getString(
                                                R.string.cable_drop_result_ac,
                                                fmt6(r.resistancePerConductorOhm),
                                                fmt6(xPerConductorOhm),
                                                fmt6(dropFinal.loopResistanceOhm),
                                                fmt6(dropFinal.loopReactanceOhm),
                                                fmt6(dropFinal.loopImpedanceOhm),
                                                fmt4(dropFinal.voltageDropV),
                                                fmt3(dropFinal.voltageDropPercent),
                                                status,
                                            )
                                        } else {
                                            context.getString(
                                                R.string.cable_drop_result_dc,
                                                fmt6(r.resistancePerConductorOhm),
                                                fmt6(dropFinal.loopResistanceOhm),
                                                fmt4(dropFinal.voltageDropV),
                                                fmt3(dropFinal.voltageDropPercent),
                                                status,
                                            )
                                        }

                                        reportText = CableReport.buildVoltageDropReport(
                                            circuitType = circuitType,
                                            wiring = wiring,
                                            supplyVoltageV = supplyV,
                                            loadCurrentA = currentA,
                                            lengthOneWay = length,
                                            lengthUnit = lengthUnit,
                                            temperatureC = temp,
                                            material = material,
                                            areaMm2 = areaMm2,
                                            rPerConductorOhm = r.resistancePerConductorOhm,
                                            xPerConductorOhm = xPerConductorOhm,
                                            powerFactor = if (circuitType == CircuitType.AC) (pfForAc ?: 1.0) else null,
                                            frequencyHz = if (circuitType == CircuitType.AC) frequencyHz else null,
                                            inductanceMhPerKm = if (circuitType == CircuitType.AC) inductanceMhPerKm else null,
                                            dropResult = dropFinal,
                                            limitPercent = limitP,
                                        )
                                    }

                                    errorText = null
                                } catch (e: Exception) {
                                    Log.e("CableDropModule", "calc failed", e)
                                    errorText = context.getString(R.string.calc_failed)
                                    resultText = null
                                    reportText = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )

                        IconButton(
                            onClick = {
                                val report = reportText ?: return@IconButton
                                host.copyToClipboard("cable_report", report)
                                scope.launch { host.showMessage(context.getString(R.string.copied)) }
                            },
                            enabled = reportText != null,
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = context.getString(R.string.copy))
                        }

                        IconButton(
                            onClick = {
                                val report = reportText ?: return@IconButton
                                try {
                                    val base = ReportExporter.defaultFileBaseName("cable_report")
                                    val file = ReportExporter.writeTextReport(appContext, base, report)
                                    val intent = ReportExporter.shareFileIntent(appContext, file, "text/plain", context.getString(R.string.export_text))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    appContext.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("CableDropModule", "export text failed", e)
                                    scope.launch { host.showMessage(context.getString(R.string.export_failed)) }
                                }
                            },
                            enabled = reportText != null,
                        ) {
                            Icon(Icons.Default.Description, contentDescription = context.getString(R.string.export_text))
                        }

                        IconButton(
                            onClick = {
                                val report = reportText ?: return@IconButton
                                try {
                                    val base = ReportExporter.defaultFileBaseName("cable_report")
                                    val file = ReportExporter.writePdfReport(appContext, base, report)
                                    val intent = ReportExporter.shareFileIntent(appContext, file, "application/pdf", context.getString(R.string.export_pdf))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    appContext.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("CableDropModule", "export pdf failed", e)
                                    scope.launch { host.showMessage(context.getString(R.string.export_failed)) }
                                }
                            },
                            enabled = reportText != null,
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = context.getString(R.string.export_pdf))
                        }
                    }

                    val err = errorText
                    if (err != null) {
                        Text(text = err, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            ResultPanel(
                title = context.getString(R.string.output_label),
                value = resultText,
            )

            ScenarioPanel(
                presets = presets,
                selectedPresetName = selectedPresetName,
                scenarioName = scenarioName,
                onScenarioNameChange = { scenarioName = it },
                onSelectPreset = { preset ->
                    selectedPresetName = preset.name
                    material = preset.material
                    areaUnit = preset.areaUnit
                    preset.areaMm2?.let { mm2Text = fmt4(it).trimEnd('0').trimEnd('.') }
                        preset.awgGauge?.let { awgGauge = it }
                    lengthText = fmt4(preset.length).trimEnd('0').trimEnd('.')
                    lengthUnit = preset.lengthUnit
                    tempText = fmt2(preset.temperatureC).trimEnd('0').trimEnd('.')
                    circuitType = preset.circuitType
                    wiring = preset.wiring
                    currentText = fmt2(preset.currentA).trimEnd('0').trimEnd('.')
                    supplyText = fmt2(preset.supplyV).trimEnd('0').trimEnd('.')
                    limitText = fmt2(preset.limitPercent).trimEnd('0').trimEnd('.')
                    limitMode = if (absDiff(preset.limitPercent, 3.0) < 1e-6) "3" else if (absDiff(preset.limitPercent, 5.0) < 1e-6) "5" else "custom"
                    preset.powerFactor?.let { powerFactorText = fmt3(it).trimEnd('0').trimEnd('.') }
                    preset.frequencyHz?.let { frequencyHzText = fmt2(it).trimEnd('0').trimEnd('.') }
                    preset.inductanceMhPerKm?.let { inductanceMhPerKmText = fmt4(it).trimEnd('0').trimEnd('.') }
                },
                onSavePreset = {
                    val name = scenarioName.trim()
                    if (name.isBlank()) {
                        scope.launch { host.showMessage(context.getString(R.string.name_required)) }
                        return@ScenarioPanel
                    }
                    val scenario = buildScenarioOrNull(
                        name = name,
                        material = material,
                        areaUnit = areaUnit,
                        mm2Text = mm2Text,
                        awgGauge = awgGauge,
                        lengthText = lengthText,
                        lengthUnit = lengthUnit,
                        tempText = tempText,
                        circuitType = circuitType,
                        wiring = wiring,
                        currentText = currentText,
                        supplyText = supplyText,
                        limitText = limitText,
                        powerFactorText = powerFactorText,
                        frequencyHzText = frequencyHzText,
                        inductanceMhPerKmText = inductanceMhPerKmText,
                    )
                    if (scenario == null) {
                        scope.launch { host.showMessage(context.getString(R.string.invalid_input)) }
                        return@ScenarioPanel
                    }
                    scope.launch {
                        store.upsertPreset(scenario)
                        host.showMessage(context.getString(R.string.saved))
                    }
                },
                onDeletePreset = {
                    val name = selectedPresetName ?: return@ScenarioPanel
                    scope.launch {
                        store.deletePreset(name)
                        selectedPresetName = null
                        host.showMessage(context.getString(R.string.deleted))
                    }
                },
            )

            SecondaryButton(
                title = context.getString(R.string.restore_defaults),
                onClick = {
                    tab = CableTab.DROP
                    material = CableMaterial.COPPER
                    areaUnit = AreaUnit.MM2
                    mm2Mode = "standard"
                    mm2Standard = 2.5
                    mm2Text = "2.5"
                    awgGauge = 12
                    lengthUnit = LengthUnit.METER
                    lengthText = "10"
                    tempText = "20"
                    circuitType = CircuitType.DC
                    powerFactorText = "0.90"
                    frequencyHzText = "50"
                    inductanceMhPerKmText = "0"
                    wiring = VoltageDropWiring.SINGLE_PHASE_2W
                    currentText = "10"
                    supplyText = "220"
                    limitMode = "3"
                    limitText = "3"
                    scenarioName = ""
                    selectedPresetName = null
                    resultText = null
                    reportText = null
                    errorText = null
                    loadedFromInputStore = false
                    scope.launch {
                        inputStore.clear(descriptor.id)
                        store.clearLast()
                        host.showMessage(context.getString(R.string.restored_defaults))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ResultPanel(title: String, value: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = value ?: "—")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenarioPanel(
    presets: List<CableScenario>,
    selectedPresetName: String?,
    scenarioName: String,
    onScenarioNameChange: (String) -> Unit,
    onSelectPreset: (CableScenario) -> Unit,
    onSavePreset: () -> Unit,
    onDeletePreset: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = context.getString(R.string.scenario), style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = scenarioName,
                onValueChange = onScenarioNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = context.getString(R.string.input_scenario_name)) },
                supportingText = { Text(text = context.getString(R.string.hint_scenario_name)) },
            )

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedPresetName ?: context.getString(R.string.none),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text(text = context.getString(R.string.load_preset)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    supportingText = { Text(text = context.getString(R.string.hint_load_preset)) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(text = preset.name) },
                            onClick = {
                                expanded = false
                                onSelectPreset(preset)
                            }
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    title = context.getString(R.string.save_preset),
                    onClick = onSavePreset,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeletePreset, enabled = selectedPresetName != null) {
                    Icon(Icons.Default.Delete, contentDescription = context.getString(R.string.delete))
                }
            }
        }
    }
}

private fun buildScenarioOrNull(
    name: String,
    material: CableMaterial,
    areaUnit: AreaUnit,
    mm2Text: String,
    awgGauge: Int,
    lengthText: String,
    lengthUnit: LengthUnit,
    tempText: String,
    circuitType: CircuitType,
    wiring: VoltageDropWiring,
    currentText: String,
    supplyText: String,
    limitText: String,
    powerFactorText: String,
    frequencyHzText: String,
    inductanceMhPerKmText: String,
): CableScenario? {
    val length = lengthText.toDoubleOrNull() ?: return null
    val temp = tempText.toDoubleOrNull() ?: return null
    val mm2 = if (areaUnit == AreaUnit.MM2) mm2Text.toDoubleOrNull() else null
    val awgGaugeValue = if (areaUnit == AreaUnit.AWG) awgGauge else null
    val current = currentText.toDoubleOrNull() ?: 0.0
    val supply = supplyText.toDoubleOrNull() ?: 0.0
    val limit = limitText.toDoubleOrNull() ?: 0.0
    val pf = powerFactorText.toDoubleOrNull()?.takeIf { it.isFinite() }
    val freq = frequencyHzText.toDoubleOrNull()?.takeIf { it.isFinite() }
    val induct = inductanceMhPerKmText.toDoubleOrNull()?.takeIf { it.isFinite() }
    return CableScenario(
        name = name,
        material = material,
        areaUnit = areaUnit,
        areaMm2 = mm2,
        awgGauge = awgGaugeValue,
        length = length,
        lengthUnit = lengthUnit,
        temperatureC = temp,
        circuitType = circuitType,
        wiring = wiring,
        currentA = current,
        supplyV = supply,
        limitPercent = limit,
        powerFactor = if (circuitType == CircuitType.AC) pf else null,
        frequencyHz = if (circuitType == CircuitType.AC) freq else null,
        inductanceMhPerKm = if (circuitType == CircuitType.AC) induct else null,
    )
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

private fun absDiff(a: Double, b: Double): Double = kotlin.math.abs(a - b)
private fun fmt2(v: Double): String = String.format(Locale.US, "%.2f", v)
private fun fmt3(v: Double): String = String.format(Locale.US, "%.3f", v)
private fun fmt4(v: Double): String = String.format(Locale.US, "%.4f", v)
private fun fmt6(v: Double): String = String.format(Locale.US, "%.6f", v)

private fun standardMm2Sizes(): List<Double> = listOf(
    0.5, 0.75, 1.0, 1.5, 2.5, 4.0, 6.0, 10.0, 16.0, 25.0, 35.0, 50.0, 70.0, 95.0, 120.0, 150.0, 185.0, 240.0, 300.0, 400.0
)
