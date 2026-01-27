package com.kidsafe.probe.cable

import java.util.Locale
import kotlin.math.sqrt

object CableReport {
    fun buildResistanceReport(input: CableResistanceInput, result: CableResistanceResult): String {
        val areaMm2 = input.conductorAreaMm2()
        val lengthM = input.lengthMetersOneWay()
        val rho20 = input.material.resistivityOhmMm2PerMAt20C
        val alpha = input.material.tempCoeffPerC
        val t = input.temperatureC
        val rhoT = result.resistivityOhmMm2PerM

        return buildString {
            appendLine("电缆线阻计算报告")
            appendLine()
            appendLine("输入参数")
            appendLine("- 材料：${input.material.displayName}")
            appendLine("- 截面积：${fmt4(areaMm2)} mm² (${input.areaUnit.displayName}${input.awg?.let { " ${it.displayName}" } ?: ""})")
            appendLine("- 单程长度：${fmt4(input.lengthOneWay)} ${input.lengthUnit.displayName} (${fmt4(lengthM)} m)")
            appendLine("- 导体温度：${fmt2(t)} ℃")
            appendLine()
            appendLine("采用参数")
            appendLine("- ρ20：${fmt6(rho20)} Ω·mm²/m (20℃)")
            appendLine("- α：${fmt6(alpha)} 1/℃")
            appendLine()
            appendLine("计算步骤")
            appendLine("- ρ(T) = ρ20 × (1 + α × (T - 20))")
            appendLine("  = ${fmt6(rho20)} × (1 + ${fmt6(alpha)} × (${fmt2(t)} - 20))")
            appendLine("  = ${fmt6(rhoT)} Ω·mm²/m")
            appendLine("- R = ρ(T) × L / A")
            appendLine("  = ${fmt6(rhoT)} × ${fmt4(lengthM)} / ${fmt4(areaMm2)}")
            appendLine("  = ${fmt6(result.resistancePerConductorOhm)} Ω (单根导体，单程)")
            appendLine("- R(Ω/km) = ρ(T) × 1000 / A")
            appendLine("  = ${fmt6(result.resistancePerKmOhm)} Ω/km")
            appendLine()
            appendLine("结果")
            appendLine("- 单根导体电阻：${fmt6(result.resistancePerConductorOhm)} Ω")
            appendLine("- 电阻(每千米)：${fmt6(result.resistancePerKmOhm)} Ω/km")
        }
    }

    fun buildVoltageDropReport(
        circuitType: CircuitType,
        wiring: VoltageDropWiring,
        supplyVoltageV: Double,
        loadCurrentA: Double,
        lengthOneWay: Double,
        lengthUnit: LengthUnit,
        temperatureC: Double,
        material: CableMaterial,
        areaMm2: Double,
        rPerConductorOhm: Double,
        xPerConductorOhm: Double,
        powerFactor: Double?,
        frequencyHz: Double?,
        inductanceMhPerKm: Double?,
        dropResult: VoltageDropResult,
        limitPercent: Double,
    ): String {
        val lengthM = lengthOneWay * lengthUnit.metersPerUnit
        val rho20 = material.resistivityOhmMm2PerMAt20C
        val alpha = material.tempCoeffPerC
        val rhoT = rho20 * (1.0 + alpha * (temperatureC - 20.0))

        val baseFormula = when {
            circuitType == CircuitType.AC && wiring.usesSqrt3 -> "ΔV ≈ √3 × I × (R × cosφ + X × sinφ)"
            circuitType == CircuitType.AC -> "ΔV ≈ I × (Rloop × cosφ + Xloop × sinφ)"
            wiring.usesSqrt3 -> "ΔV = √3 × I × R"
            else -> "ΔV = I × Rloop"
        }

        return buildString {
            appendLine("电压降计算报告")
            appendLine()
            appendLine("输入参数")
            appendLine("- 电路类型：${circuitType.displayName}")
            appendLine("- 系统/接线：${wiring.displayName}")
            appendLine("- 电源电压：${fmt2(supplyVoltageV)} V")
            appendLine("- 负载电流：${fmt2(loadCurrentA)} A")
            appendLine("- 材料：${material.displayName}")
            appendLine("- 截面积：${fmt4(areaMm2)} mm²")
            appendLine("- 单程长度：${fmt4(lengthOneWay)} ${lengthUnit.displayName} (${fmt4(lengthM)} m)")
            appendLine("- 导体温度：${fmt2(temperatureC)} ℃")
            if (circuitType == CircuitType.AC) {
                powerFactor?.let { appendLine("- 功率因数：${fmt3(it)}") }
                frequencyHz?.let { appendLine("- 频率：${fmt2(it)} Hz") }
                inductanceMhPerKm?.let { appendLine("- 电感(每千米)：${fmt4(it)} mH/km") }
            }
            appendLine("- 允许压降：${fmt2(limitPercent)} %")
            appendLine()
            appendLine("计算步骤")
            appendLine("- ρ(T) = ρ20 × (1 + α × (T - 20)) = ${fmt6(rhoT)} Ω·mm²/m")
            appendLine("- R = ρ(T) × L / A = ${fmt6(rPerConductorOhm)} Ω (单根导体，单程)")
            if (circuitType == CircuitType.AC) {
                appendLine("- X = ${fmt6(xPerConductorOhm)} Ω (单根导体，单程)")
                appendLine("- Z = √(R² + X²)")
            }
            if (wiring.usesReturnConductor) {
                appendLine("- Rloop = 2 × R = ${fmt6(dropResult.loopResistanceOhm)} Ω")
                if (circuitType == CircuitType.AC) {
                    appendLine("- Xloop = 2 × X = ${fmt6(dropResult.loopReactanceOhm)} Ω")
                    appendLine("- Zloop = √(Rloop² + Xloop²) = ${fmt6(dropResult.loopImpedanceOhm)} Ω")
                }
            } else {
                appendLine("- Rloop = R = ${fmt6(dropResult.loopResistanceOhm)} Ω")
                if (circuitType == CircuitType.AC) {
                    appendLine("- Xloop = X = ${fmt6(dropResult.loopReactanceOhm)} Ω")
                    appendLine("- Zloop = √(Rloop² + Xloop²) = ${fmt6(dropResult.loopImpedanceOhm)} Ω")
                }
            }
            appendLine("- $baseFormula")
            when {
                circuitType == CircuitType.AC && wiring.usesSqrt3 -> {
                    val pf = powerFactor ?: 1.0
                    appendLine("  = ${fmt6(sqrt(3.0))} × ${fmt2(loadCurrentA)} × (${fmt6(rPerConductorOhm)} × ${fmt3(pf)} + ${fmt6(xPerConductorOhm)} × sinφ)")
                }
                circuitType == CircuitType.AC -> {
                    val pf = powerFactor ?: 1.0
                    appendLine("  = ${fmt2(loadCurrentA)} × (${fmt6(dropResult.loopResistanceOhm)} × ${fmt3(pf)} + ${fmt6(dropResult.loopReactanceOhm)} × sinφ)")
                }
                wiring.usesSqrt3 -> appendLine("  = ${fmt6(sqrt(3.0))} × ${fmt2(loadCurrentA)} × ${fmt6(rPerConductorOhm)}")
                else -> appendLine("  = ${fmt2(loadCurrentA)} × ${fmt6(dropResult.loopResistanceOhm)}")
            }
            appendLine("  = ${fmt4(dropResult.voltageDropV)} V")
            appendLine("- 压降百分比 = ΔV / V × 100% = ${fmt3(dropResult.voltageDropPercent)} %")
            appendLine("- 线损功率(估算) = ${fmt2(dropResult.powerLossW)} W")
            appendLine()
            appendLine("结论")
            val status = if (dropResult.isWithinLimit) "合格" else "超限"
            appendLine("- 电压降：${fmt4(dropResult.voltageDropV)} V (${fmt3(dropResult.voltageDropPercent)} %) -> $status")
        }
    }

    private fun fmt2(v: Double): String = String.format(Locale.US, "%.2f", v)
    private fun fmt3(v: Double): String = String.format(Locale.US, "%.3f", v)
    private fun fmt4(v: Double): String = String.format(Locale.US, "%.4f", v)
    private fun fmt6(v: Double): String = String.format(Locale.US, "%.6f", v)
}
