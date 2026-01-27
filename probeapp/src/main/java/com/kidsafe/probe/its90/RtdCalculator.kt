package com.kidsafe.probe.its90

class RtdCalculator(private val repo: Its90Repository) {
    fun temperatureToResistanceOhm(
        type: RtdType,
        elementTemperatureC: Double,
        wiring: RtdWiring,
        leadResistanceOhmPerWire: Double,
    ): Double {
        val table = repo.rtdTable(type)
        val rElement = table.temperatureToValue(elementTemperatureC)
        if (rElement.isNaN()) return Double.NaN
        return rElement + wiringLeadEffect(wiring, leadResistanceOhmPerWire)
    }

    fun resistanceOhmToTemperature(
        type: RtdType,
        measuredResistanceOhm: Double,
        wiring: RtdWiring,
        leadResistanceOhmPerWire: Double,
    ): Double {
        val table = repo.rtdTable(type)
        val rElement = measuredResistanceOhm - wiringLeadEffect(wiring, leadResistanceOhmPerWire)
        return table.valueToTemperature(rElement)
    }

    private fun wiringLeadEffect(wiring: RtdWiring, leadResistanceOhmPerWire: Double): Double {
        val r = leadResistanceOhmPerWire.coerceAtLeast(0.0)
        return when (wiring) {
            RtdWiring.WIRE_2 -> 2.0 * r
            RtdWiring.WIRE_3 -> r
            RtdWiring.WIRE_4 -> 0.0
        }
    }
}

