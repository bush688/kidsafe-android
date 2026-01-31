package com.kidsafe.probe.dplevel

object DpLevelCalculator {
    const val G = 9.81

    data class CalPoint(
        val levelPercent: Double,
        val dpNowPa: Double,
    )

    data class RangeFit(
        val lrvPa: Double,
        val urvPa: Double,
        val spanPa: Double,
        val rmsePa: Double,
        val maxAbsErrorPa: Double,
    )

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

    fun fitRangeFromPoints(
        lrv0Pa: Double,
        urv0Pa: Double,
        points: List<CalPoint>,
        keepSpan: Boolean,
    ): RangeFit {
        if (points.isEmpty()) return RangeFit(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
        val filtered = points.filter { p ->
            p.levelPercent.isFinite() &&
                p.levelPercent >= 0.0 &&
                p.levelPercent <= 100.0 &&
                p.dpNowPa.isFinite()
        }
        if (filtered.isEmpty()) return RangeFit(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)

        return if (keepSpan) {
            val span0 = urv0Pa - lrv0Pa
            if (!span0.isFinite() || span0 <= 0.0) return RangeFit(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
            val lrvCandidates = filtered.map { it.dpNowPa - (it.levelPercent / 100.0) * span0 }
            val lrv = lrvCandidates.average()
            val urv = lrv + span0
            val metrics = computeErrorMetrics(lrv, span0, filtered)
            RangeFit(lrv, urv, span0, metrics.first, metrics.second)
        } else {
            if (filtered.size < 2) return RangeFit(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
            val xs = filtered.map { it.levelPercent / 100.0 }
            val ys = filtered.map { it.dpNowPa }
            val xBar = xs.average()
            val yBar = ys.average()
            var num = 0.0
            var den = 0.0
            for (i in xs.indices) {
                val dx = xs[i] - xBar
                num += dx * (ys[i] - yBar)
                den += dx * dx
            }
            if (!den.isFinite() || den <= 0.0) return RangeFit(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
            val span = num / den
            if (!span.isFinite() || span <= 0.0) return RangeFit(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
            val lrv = yBar - span * xBar
            val urv = lrv + span
            val metrics = computeErrorMetrics(lrv, span, filtered)
            RangeFit(lrv, urv, span, metrics.first, metrics.second)
        }
    }

    private fun computeErrorMetrics(lrvPa: Double, spanPa: Double, points: List<CalPoint>): Pair<Double, Double> {
        var sumSq = 0.0
        var maxAbs = 0.0
        for (p in points) {
            val x = p.levelPercent / 100.0
            val pred = lrvPa + x * spanPa
            val err = p.dpNowPa - pred
            sumSq += err * err
            val abs = kotlin.math.abs(err)
            if (abs > maxAbs) maxAbs = abs
        }
        val rmse = kotlin.math.sqrt(sumSq / points.size.toDouble())
        return rmse to maxAbs
    }
}
