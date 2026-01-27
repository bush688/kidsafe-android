package com.kidsafe.probe.dplevel

enum class PressureUnit(
    val displayName: String,
    private val pascalPerUnit: Double,
) {
    KPA("kPa", 1_000.0),
    PA("Pa", 1.0),
    BAR("bar", 100_000.0),
    MMH2O("mmH₂O", 9.80665),
    ;

    fun toPa(value: Double): Double = value * pascalPerUnit
    fun fromPa(pa: Double): Double = pa / pascalPerUnit
}

