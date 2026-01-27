package com.kidsafe.probe.its90

class ThermocoupleCalculator(private val repo: Its90Repository) {
    fun temperatureToMillivolt(
        type: ThermocoupleType,
        hotJunctionC: Double,
        coldJunctionC: Double?,
    ): Double {
        val table = repo.thermocoupleTable(type)
        val eHot = table.temperatureToValue(hotJunctionC)
        if (eHot.isNaN()) return Double.NaN
        val cj = coldJunctionC ?: 0.0
        if (cj == 0.0) return eHot
        val eCj = table.temperatureToValue(cj)
        if (eCj.isNaN()) return Double.NaN
        return eHot - eCj
    }

    fun millivoltToTemperature(
        type: ThermocoupleType,
        measuredMillivolt: Double,
        coldJunctionC: Double?,
    ): Double {
        val table = repo.thermocoupleTable(type)
        val cj = coldJunctionC ?: 0.0
        val eRef = if (cj == 0.0) 0.0 else table.temperatureToValue(cj)
        if (eRef.isNaN()) return Double.NaN
        return table.valueToTemperature(measuredMillivolt + eRef)
    }
}

