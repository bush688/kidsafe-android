package com.kidsafe.probe.dplevel

enum class DpLevelInstrument {
    DP,
    DUAL_FLANGE,
}

enum class DpLevelMode {
    RHO_AND_HEIGHT,
    RECALC_RANGE,
}

enum class DpZeroShiftDirection {
    NEGATIVE,
    POSITIVE,
}

data class DpLevelInput(
    val instrument: DpLevelInstrument,
    val mode: DpLevelMode,
    val mediumDensityKgM3: Double?,
    val oilDensityKgM3: Double,
    val spanHeightM: Double,
    val zeroShiftDirection: DpZeroShiftDirection,
    val zeroShiftMediumM: Double,
    val oilEquivalentHeightM: Double,
    val dpLrvPa: Double?,
    val dpUrvPa: Double?,
    val levelPercent: Double?,
    val dpNowPa: Double?,
)

data class DpLevelResult(
    val mediumDensityKgM3: Double,
    val lrvPa: Double,
    val urvPa: Double,
    val spanPa: Double,
) {
    val zeroPa: Double get() = lrvPa
}
