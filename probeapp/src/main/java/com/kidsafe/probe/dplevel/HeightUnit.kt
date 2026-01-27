package com.kidsafe.probe.dplevel

enum class HeightUnit(
    val displayName: String,
    private val meterPerUnit: Double,
) {
    M("m", 1.0),
    MM("mm", 0.001),
    ;

    fun toM(value: Double): Double = value * meterPerUnit
    fun fromM(m: Double): Double = m / meterPerUnit
}

