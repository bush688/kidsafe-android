package com.kidsafe.probe.cable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CableCalculatorTest {
    @Test
    fun dcResistance_copper_1mm2_1km_20c() {
        val input = CableResistanceInput(
            material = CableMaterial.COPPER,
            lengthOneWay = 1000.0,
            lengthUnit = LengthUnit.METER,
            areaUnit = AreaUnit.MM2,
            areaMm2 = 1.0,
            awg = null,
            temperatureC = 20.0,
        )
        val r = CableCalculator.dcResistance(input)
        assertEquals(17.241, r.resistancePerConductorOhm, 0.05)
        assertEquals(17.241, r.resistancePerKmOhm, 0.05)
    }

    @Test
    fun dcResistance_aluminum_10mm2_100m_20c() {
        val input = CableResistanceInput(
            material = CableMaterial.ALUMINUM,
            lengthOneWay = 100.0,
            lengthUnit = LengthUnit.METER,
            areaUnit = AreaUnit.MM2,
            areaMm2 = 10.0,
            awg = null,
            temperatureC = 20.0,
        )
        val r = CableCalculator.dcResistance(input)
        assertEquals(0.28264, r.resistancePerConductorOhm, 1e-4)
    }

    @Test
    fun awgArea_awg12_isReasonable() {
        val area = AwgSize(12).areaMm2()
        assertTrue(area > 3.0 && area < 3.6)
    }

    @Test
    fun voltageDrop_singlePhase2w() {
        val drop = CableCalculator.voltageDrop(
            wiring = VoltageDropWiring.SINGLE_PHASE_2W,
            supplyVoltageV = 220.0,
            loadCurrentA = 10.0,
            resistancePerConductorOhm = 0.5,
            limitPercent = 5.0,
        )
        assertEquals(1.0, drop.loopResistanceOhm, 1e-12)
        assertEquals(0.0, drop.loopReactanceOhm, 1e-12)
        assertEquals(1.0, drop.loopImpedanceOhm, 1e-12)
        assertEquals(10.0, drop.voltageDropV, 1e-12)
        assertEquals(10.0 / 220.0 * 100.0, drop.voltageDropPercent, 1e-12)
        assertEquals(100.0, drop.powerLossW, 1e-12)
        assertTrue(drop.isWithinLimit)
    }

    @Test
    fun voltageDrop_threePhase3w() {
        val r = 0.1
        val i = 20.0
        val v = 400.0
        val drop = CableCalculator.voltageDrop(
            wiring = VoltageDropWiring.THREE_PHASE_3W,
            supplyVoltageV = v,
            loadCurrentA = i,
            resistancePerConductorOhm = r,
            limitPercent = 3.0,
        )
        assertEquals(r, drop.loopResistanceOhm, 1e-12)
        assertEquals(0.0, drop.loopReactanceOhm, 1e-12)
        assertEquals(r, drop.loopImpedanceOhm, 1e-12)
        assertEquals(sqrt(3.0) * i * r, drop.voltageDropV, 1e-12)
        assertEquals(sqrt(3.0) * i * r / v * 100.0, drop.voltageDropPercent, 1e-12)
        assertEquals(3.0 * i * i * r, drop.powerLossW, 1e-12)
    }

    @Test
    fun voltageDropAc_singlePhase2w() {
        val r = 0.5
        val x = 0.2
        val i = 10.0
        val v = 220.0
        val pf = 0.8
        val sin = 0.6
        val drop = CableCalculator.voltageDropAc(
            wiring = VoltageDropWiring.SINGLE_PHASE_2W,
            supplyVoltageV = v,
            loadCurrentA = i,
            resistancePerConductorOhm = r,
            reactancePerConductorOhm = x,
            powerFactor = pf,
            limitPercent = 5.0,
        )
        assertEquals(2.0 * r, drop.loopResistanceOhm, 1e-12)
        assertEquals(2.0 * x, drop.loopReactanceOhm, 1e-12)
        assertEquals(sqrt((2.0 * r) * (2.0 * r) + (2.0 * x) * (2.0 * x)), drop.loopImpedanceOhm, 1e-12)
        assertEquals(i * (2.0 * r * pf + 2.0 * x * sin), drop.voltageDropV, 1e-12)
        assertEquals(i * (2.0 * r * pf + 2.0 * x * sin) / v * 100.0, drop.voltageDropPercent, 1e-12)
        assertEquals(i * i * (2.0 * r), drop.powerLossW, 1e-12)
        assertTrue(drop.isWithinLimit)
    }

    @Test
    fun voltageDropAc_threePhase3w() {
        val r = 0.1
        val x = 0.05
        val i = 20.0
        val v = 400.0
        val pf = 0.9
        val sin = sqrt(1.0 - pf * pf)
        val drop = CableCalculator.voltageDropAc(
            wiring = VoltageDropWiring.THREE_PHASE_3W,
            supplyVoltageV = v,
            loadCurrentA = i,
            resistancePerConductorOhm = r,
            reactancePerConductorOhm = x,
            powerFactor = pf,
            limitPercent = 10.0,
        )
        assertEquals(r, drop.loopResistanceOhm, 1e-12)
        assertEquals(x, drop.loopReactanceOhm, 1e-12)
        assertEquals(sqrt(r * r + x * x), drop.loopImpedanceOhm, 1e-12)
        assertEquals(sqrt(3.0) * i * (r * pf + x * sin), drop.voltageDropV, 1e-12)
        assertEquals(sqrt(3.0) * i * (r * pf + x * sin) / v * 100.0, drop.voltageDropPercent, 1e-12)
        assertEquals(3.0 * i * i * r, drop.powerLossW, 1e-12)
    }
}
