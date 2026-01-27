package com.kidsafe.probe.dplevel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DpLevelCalculatorTest {
    @Test
    fun rhoAndHeight_basicSpan() {
        val out = DpLevelCalculator.compute(
            DpLevelInput(
                instrument = DpLevelInstrument.DP,
                mode = DpLevelMode.RHO_AND_HEIGHT,
                mediumDensityKgM3 = 1000.0,
                oilDensityKgM3 = 950.0,
                spanHeightM = 10.0,
                zeroShiftDirection = DpZeroShiftDirection.NEGATIVE,
                zeroShiftMediumM = 0.0,
                oilEquivalentHeightM = 0.0,
                dpLrvPa = null,
                dpUrvPa = null,
                levelPercent = null,
                dpNowPa = null,
            )
        )
        val expectedSpanPa = 1000.0 * DpLevelCalculator.G * 10.0
        val expectedZeroPa = 0.0
        assertRelativeWithin(out.spanPa, expectedSpanPa, 0.005)
        assertRelativeWithin(out.zeroPa, expectedZeroPa, 0.005)
    }

    @Test
    fun dualFlange_includesOilTermInZeroOnly() {
        val out = DpLevelCalculator.compute(
            DpLevelInput(
                instrument = DpLevelInstrument.DUAL_FLANGE,
                mode = DpLevelMode.RHO_AND_HEIGHT,
                mediumDensityKgM3 = 1100.0,
                oilDensityKgM3 = 950.0,
                spanHeightM = 5.0,
                zeroShiftDirection = DpZeroShiftDirection.POSITIVE,
                zeroShiftMediumM = 1.5,
                oilEquivalentHeightM = -0.4,
                dpLrvPa = null,
                dpUrvPa = null,
                levelPercent = null,
                dpNowPa = null,
            )
        )
        val expectedSpanPa = 1100.0 * DpLevelCalculator.G * 5.0
        val expectedZeroPa = 1100.0 * DpLevelCalculator.G * 1.5 + 950.0 * DpLevelCalculator.G * 0.4
        assertRelativeWithin(out.spanPa, expectedSpanPa, 0.005)
        assertRelativeWithin(out.zeroPa, expectedZeroPa, 0.005)
    }

    @Test
    fun negativeShift_defaultGivesNegativeZero() {
        val out = DpLevelCalculator.compute(
            DpLevelInput(
                instrument = DpLevelInstrument.DP,
                mode = DpLevelMode.RHO_AND_HEIGHT,
                mediumDensityKgM3 = 1000.0,
                oilDensityKgM3 = 950.0,
                spanHeightM = 10.0,
                zeroShiftDirection = DpZeroShiftDirection.NEGATIVE,
                zeroShiftMediumM = 2.0,
                oilEquivalentHeightM = 0.0,
                dpLrvPa = null,
                dpUrvPa = null,
                levelPercent = null,
                dpNowPa = null,
            )
        )
        assertTrue(out.lrvPa < 0.0)
    }

    @Test
    fun recalcRange_keepsSpanAndFitsPercent() {
        val lrv0 = PressureUnit.KPA.toPa(-10.0)
        val urv0 = PressureUnit.KPA.toPa(90.0)
        val span = urv0 - lrv0
        val percent = 25.0
        val dpNow = PressureUnit.KPA.toPa(5.0)
        val out = DpLevelCalculator.compute(
            DpLevelInput(
                instrument = DpLevelInstrument.DP,
                mode = DpLevelMode.RECALC_RANGE,
                mediumDensityKgM3 = null,
                oilDensityKgM3 = 950.0,
                spanHeightM = 1.0,
                zeroShiftDirection = DpZeroShiftDirection.NEGATIVE,
                zeroShiftMediumM = 0.0,
                oilEquivalentHeightM = 0.0,
                dpLrvPa = lrv0,
                dpUrvPa = urv0,
                levelPercent = percent,
                dpNowPa = dpNow,
            )
        )
        assertRelativeWithin(out.spanPa, span, 0.000001)
        val fraction = percent / 100.0
        val dpCheck = out.lrvPa + fraction * out.spanPa
        assertRelativeWithin(dpCheck, dpNow, 0.000001)
    }

    @Test
    fun pressureUnit_roundTrip() {
        val pa = 123_456.0
        PressureUnit.entries.forEach { u ->
            val v = u.fromPa(pa)
            val pa2 = u.toPa(v)
            assertEquals(pa, pa2, 1e-6)
        }
    }
}

private fun assertRelativeWithin(actual: Double, expected: Double, rel: Double) {
    if (expected == 0.0) {
        assertTrue(kotlin.math.abs(actual) <= rel)
        return
    }
    val r = kotlin.math.abs((actual - expected) / expected)
    assertTrue("relative error=$r", r <= rel)
}
