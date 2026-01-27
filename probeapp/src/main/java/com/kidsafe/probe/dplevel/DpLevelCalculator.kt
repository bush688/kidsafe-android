package com.kidsafe.probe.dplevel

object DpLevelCalculator {
    const val G = 9.81

    fun compute(input: DpLevelInput): DpLevelResult {
        val spanHeightM = input.spanHeightM
        val oilDensity = input.oilDensityKgM3

        return when (input.mode) {
            DpLevelMode.RHO_AND_HEIGHT -> {
                if (spanHeightM <= 0.0) return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                val mediumDensity = input.mediumDensityKgM3 ?: return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                if (mediumDensity <= 0.0 || mediumDensity.isNaN()) return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)

                val baseZeroPa = mediumDensity * G * kotlin.math.abs(input.zeroShiftMediumM) +
                    if (input.instrument == DpLevelInstrument.DUAL_FLANGE) oilDensity * G * kotlin.math.abs(input.oilEquivalentHeightM) else 0.0
                val zeroPa = when (input.zeroShiftDirection) {
                    DpZeroShiftDirection.NEGATIVE -> -baseZeroPa
                    DpZeroShiftDirection.POSITIVE -> baseZeroPa
                }
                val spanPa = mediumDensity * G * spanHeightM
                val lrv = zeroPa
                val urv = lrv + spanPa
                DpLevelResult(
                    mediumDensityKgM3 = mediumDensity,
                    lrvPa = lrv,
                    urvPa = urv,
                    spanPa = spanPa,
                )
            }

            DpLevelMode.RECALC_RANGE -> {
                val lrv0 = input.dpLrvPa ?: return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                val urv0 = input.dpUrvPa ?: return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                val dpNow = input.dpNowPa ?: return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                val p = input.levelPercent ?: return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                val span = urv0 - lrv0
                if (span <= 0.0 || span.isNaN()) return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                if (p.isNaN() || p < 0.0 || p > 100.0) return DpLevelResult(Double.NaN, Double.NaN, Double.NaN, Double.NaN)
                val fraction = p / 100.0
                val lrvNew = dpNow - fraction * span
                val urvNew = lrvNew + span
                DpLevelResult(
                    mediumDensityKgM3 = Double.NaN,
                    lrvPa = lrvNew,
                    urvPa = urvNew,
                    spanPa = span,
                )
            }
        }
    }
}
