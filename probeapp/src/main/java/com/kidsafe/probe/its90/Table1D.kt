package com.kidsafe.probe.its90

import kotlin.math.abs

internal class Table1D(
    private val x: DoubleArray,
    private val y: DoubleArray,
) {
    private val minYVal: Double
    private val maxYVal: Double

    init {
        require(x.size == y.size)
        require(x.isNotEmpty())

        var minY = y[0]
        var maxY = y[0]
        for (i in 1 until y.size) {
            val v = y[i]
            if (v < minY) minY = v
            if (v > maxY) maxY = v
        }
        minYVal = minY
        maxYVal = maxY
    }

    val minX: Double get() = x.first()
    val maxX: Double get() = x.last()
    val minY: Double get() = minYVal
    val maxY: Double get() = maxYVal

    fun forward(xv: Double): Double {
        if (xv <= minX) return if (abs(xv - minX) < 1e-12) y.first() else Double.NaN
        if (xv >= maxX) return if (abs(xv - maxX) < 1e-12) y.last() else Double.NaN

        val idx = upperBound(x, xv)
        val i0 = idx - 1
        val i1 = idx
        val x0 = x[i0]
        val x1 = x[i1]
        val y0 = y[i0]
        val y1 = y[i1]
        val t = (xv - x0) / (x1 - x0)
        return y0 + (y1 - y0) * t
    }

    fun inverse(yv: Double): Double {
        val increasing = y.last() >= y.first()
        val min = if (increasing) y.first() else y.last()
        val max = if (increasing) y.last() else y.first()

        if (yv <= min) return if (abs(yv - min) < 1e-12) x.first() else Double.NaN
        if (yv >= max) return if (abs(yv - max) < 1e-12) x.last() else Double.NaN

        val idx = if (increasing) upperBound(y, yv) else upperBoundDescending(y, yv)
        val i0 = idx - 1
        val i1 = idx
        val y0 = y[i0]
        val y1 = y[i1]
        val x0 = x[i0]
        val x1 = x[i1]
        val t = (yv - y0) / (y1 - y0)
        return x0 + (x1 - x0) * t
    }

    private fun upperBound(arr: DoubleArray, value: Double): Int {
        var lo = 0
        var hi = arr.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (arr[mid] <= value) lo = mid + 1 else hi = mid
        }
        return lo.coerceIn(1, arr.size - 1)
    }

    private fun upperBoundDescending(arr: DoubleArray, value: Double): Int {
        var lo = 0
        var hi = arr.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (arr[mid] >= value) lo = mid + 1 else hi = mid
        }
        return lo.coerceIn(1, arr.size - 1)
    }
}
