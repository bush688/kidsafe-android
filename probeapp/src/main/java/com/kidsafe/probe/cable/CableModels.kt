package com.kidsafe.probe.cable

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.pow

@Immutable
enum class CableMaterial(
    val displayName: String,
    val resistivityOhmMm2PerMAt20C: Double,
    val tempCoeffPerC: Double,
) {
    COPPER("铜", resistivityOhmMm2PerMAt20C = 0.017241, tempCoeffPerC = 0.00393),
    ALUMINUM("铝", resistivityOhmMm2PerMAt20C = 0.028264, tempCoeffPerC = 0.00403),
}

@Immutable
data class AwgSize(
    val gauge: Int,
) {
    val displayName: String
        get() = when (gauge) {
            -3 -> "0000"
            -2 -> "000"
            -1 -> "00"
            0 -> "0"
            else -> gauge.toString()
        }

    fun areaMm2(): Double {
        val dInch = 0.005 * 92.0.pow((36.0 - gauge.toDouble()) / 39.0)
        val dMm = dInch * 25.4
        return (PI / 4.0) * dMm * dMm
    }

    companion object {
        fun common(): List<AwgSize> = buildList {
            add(AwgSize(-3))
            add(AwgSize(-2))
            add(AwgSize(-1))
            add(AwgSize(0))
            for (g in 1..40) add(AwgSize(g))
        }
    }
}

@Immutable
enum class LengthUnit(val displayName: String, val metersPerUnit: Double) {
    METER("m", 1.0),
    FOOT("ft", 0.3048),
}

@Immutable
enum class AreaUnit(val displayName: String) {
    MM2("mm²"),
    AWG("AWG"),
}

@Immutable
enum class CircuitType(val displayName: String) {
    DC("直流(DC)"),
    AC("交流(AC)"),
}

@Immutable
enum class VoltageDropWiring(
    val displayName: String,
    val phaseCount: Int,
    val usesReturnConductor: Boolean,
    val usesSqrt3: Boolean,
) {
    SINGLE_PHASE_2W("单相两线", phaseCount = 1, usesReturnConductor = true, usesSqrt3 = false),
    THREE_PHASE_3W("三相三线", phaseCount = 3, usesReturnConductor = false, usesSqrt3 = true),
    THREE_PHASE_4W("三相四线(相-中)", phaseCount = 3, usesReturnConductor = true, usesSqrt3 = false),
}

@Immutable
data class CableResistanceInput(
    val material: CableMaterial,
    val lengthOneWay: Double,
    val lengthUnit: LengthUnit,
    val areaUnit: AreaUnit,
    val areaMm2: Double?,
    val awg: AwgSize?,
    val temperatureC: Double,
) {
    fun conductorAreaMm2(): Double = when (areaUnit) {
        AreaUnit.MM2 -> areaMm2 ?: Double.NaN
        AreaUnit.AWG -> awg?.areaMm2() ?: Double.NaN
    }

    fun lengthMetersOneWay(): Double = lengthOneWay * lengthUnit.metersPerUnit
}

@Immutable
data class CableResistanceResult(
    val resistivityOhmMm2PerM: Double,
    val resistancePerConductorOhm: Double,
    val resistancePerKmOhm: Double,
) {
    val resistancePerMeterOhm: Double get() = resistancePerKmOhm / 1000.0
}

@Immutable
data class VoltageDropInput(
    val wiring: VoltageDropWiring,
    val supplyVoltageV: Double,
    val loadCurrentA: Double,
    val lengthOneWay: Double,
    val lengthUnit: LengthUnit,
    val resistancePerConductorOhm: Double,
    val limitPercent: Double,
) {
    fun lengthMetersOneWay(): Double = lengthOneWay * lengthUnit.metersPerUnit
}

@Immutable
data class VoltageDropResult(
    val loopResistanceOhm: Double,
    val loopReactanceOhm: Double,
    val loopImpedanceOhm: Double,
    val voltageDropV: Double,
    val voltageDropPercent: Double,
    val powerLossW: Double,
    val isWithinLimit: Boolean,
) 
