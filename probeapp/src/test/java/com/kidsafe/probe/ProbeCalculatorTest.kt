package com.kidsafe.probe

import org.junit.Assert.assertEquals
import org.junit.Test

class ProbeCalculatorTest {
    @Test
    fun forwardAndInverse_keepPrecision() {
        val zero = 10.0
        val c = 1.234
        val far = ProbeCalculator.calcFarVoltage(c, zero)
        val near = ProbeCalculator.calcNearVoltage(c, zero)
        assertEquals(zero + (c / 2.0) * ProbeCalculator.M_V_PER_MM, far, 1e-12)
        assertEquals(zero - (c / 2.0) * ProbeCalculator.M_V_PER_MM, near, 1e-12)

        val signed = ProbeCalculator.calcChuanMmSigned(far, zero)
        assertEquals(c, signed, 1e-12)
        assertEquals(c, ProbeCalculator.calcChuanMmAbs(far, zero), 1e-12)
    }
}

