package com.kidsafe.probe

object ProbeCalculator {
    const val L_MM: Double = 1.27
    const val M_V_PER_MM: Double = 7.87

    fun defaultZeroVoltage(): Double {
        return 10.0
    }

    fun calcFarVoltage(chuanMm: Double, zeroVoltageV: Double = defaultZeroVoltage()): Double {
        return zeroVoltageV + (chuanMm / 2.0) * M_V_PER_MM
    }

    fun calcNearVoltage(chuanMm: Double, zeroVoltageV: Double = defaultZeroVoltage()): Double {
        return zeroVoltageV - (chuanMm / 2.0) * M_V_PER_MM
    }

    fun calcChuanMmSigned(voltageV: Double, zeroVoltageV: Double = defaultZeroVoltage()): Double {
        return (voltageV - zeroVoltageV) * 2.0 / M_V_PER_MM
    }

    fun calcChuanMmAbs(voltageV: Double, zeroVoltageV: Double = defaultZeroVoltage()): Double {
        return kotlin.math.abs(calcChuanMmSigned(voltageV, zeroVoltageV))
    }
}
