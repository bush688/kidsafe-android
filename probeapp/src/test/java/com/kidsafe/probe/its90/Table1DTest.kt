package com.kidsafe.probe.its90

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Table1DTest {
    @Test
    fun forward_linearInterpolation() {
        val table = Table1D(
            x = doubleArrayOf(0.0, 10.0),
            y = doubleArrayOf(0.0, 100.0),
        )
        assertEquals(50.0, table.forward(5.0), 1e-12)
    }

    @Test
    fun inverse_linearInterpolation_increasing() {
        val table = Table1D(
            x = doubleArrayOf(0.0, 10.0),
            y = doubleArrayOf(0.0, 100.0),
        )
        assertEquals(5.0, table.inverse(50.0), 1e-12)
    }

    @Test
    fun inverse_linearInterpolation_decreasing() {
        val table = Table1D(
            x = doubleArrayOf(0.0, 10.0),
            y = doubleArrayOf(100.0, 0.0),
        )
        assertEquals(5.0, table.inverse(50.0), 1e-12)
    }

    @Test
    fun outOfRange_returnsNaN() {
        val table = Table1D(
            x = doubleArrayOf(0.0, 10.0),
            y = doubleArrayOf(0.0, 100.0),
        )
        assertTrue(table.forward(-1.0).isNaN())
        assertTrue(table.forward(11.0).isNaN())
        assertTrue(table.inverse(-1.0).isNaN())
        assertTrue(table.inverse(101.0).isNaN())
    }
}

