package com.kidsafe.probe.cable

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

object CableCalculator {
    fun dcResistance(input: CableResistanceInput): CableResistanceResult {
        val areaMm2 = input.conductorAreaMm2()
        val lengthM = input.lengthMetersOneWay()
        require(areaMm2 > 0.0 && areaMm2.isFinite()) { "invalid area" }
        require(lengthM >= 0.0 && lengthM.isFinite()) { "invalid length" }
        require(input.temperatureC.isFinite()) { "invalid temperature" }

        val rho20 = input.material.resistivityOhmMm2PerMAt20C
        val alpha = input.material.tempCoeffPerC
        val rhoT = rho20 * (1.0 + alpha * (input.temperatureC - 20.0))
        val r = rhoT * lengthM / areaMm2
        val rPerKm = rhoT * 1000.0 / areaMm2
        return CableResistanceResult(
            resistivityOhmMm2PerM = rhoT,
            resistancePerConductorOhm = r,
            resistancePerKmOhm = rPerKm,
        )
    }

    fun voltageDrop(
        wiring: VoltageDropWiring,
        supplyVoltageV: Double,
        loadCurrentA: Double,
        resistancePerConductorOhm: Double,
        limitPercent: Double,
    ): VoltageDropResult {
        require(supplyVoltageV > 0.0 && supplyVoltageV.isFinite()) { "invalid supplyVoltageV" }
        require(loadCurrentA >= 0.0 && loadCurrentA.isFinite()) { "invalid loadCurrentA" }
        require(resistancePerConductorOhm >= 0.0 && resistancePerConductorOhm.isFinite()) { "invalid resistancePerConductorOhm" }
        require(limitPercent > 0.0 && limitPercent.isFinite()) { "invalid limitPercent" }

        val loopResistance = when {
            wiring.usesReturnConductor -> 2.0 * resistancePerConductorOhm
            else -> resistancePerConductorOhm
        }

        val deltaV = when {
            wiring.usesSqrt3 -> sqrt(3.0) * loadCurrentA * resistancePerConductorOhm
            else -> loadCurrentA * loopResistance
        }

        val percent = deltaV / supplyVoltageV * 100.0

        val loss = when {
            wiring.usesSqrt3 -> 3.0 * loadCurrentA * loadCurrentA * resistancePerConductorOhm
            wiring.usesReturnConductor -> loadCurrentA * loadCurrentA * loopResistance
            else -> loadCurrentA * loadCurrentA * resistancePerConductorOhm
        }

        return VoltageDropResult(
            loopResistanceOhm = loopResistance,
            loopReactanceOhm = 0.0,
            loopImpedanceOhm = loopResistance,
            voltageDropV = deltaV,
            voltageDropPercent = percent,
            powerLossW = loss,
            isWithinLimit = percent <= limitPercent + 1e-12 && abs(percent).isFinite(),
        )
    }

    fun voltageDropAc(
        wiring: VoltageDropWiring,
        supplyVoltageV: Double,
        loadCurrentA: Double,
        resistancePerConductorOhm: Double,
        reactancePerConductorOhm: Double,
        powerFactor: Double,
        limitPercent: Double,
    ): VoltageDropResult {
        require(supplyVoltageV > 0.0 && supplyVoltageV.isFinite()) { "invalid supplyVoltageV" }
        require(loadCurrentA >= 0.0 && loadCurrentA.isFinite()) { "invalid loadCurrentA" }
        require(resistancePerConductorOhm >= 0.0 && resistancePerConductorOhm.isFinite()) { "invalid resistancePerConductorOhm" }
        require(reactancePerConductorOhm >= 0.0 && reactancePerConductorOhm.isFinite()) { "invalid reactancePerConductorOhm" }
        require(powerFactor in 0.0..1.0 && powerFactor.isFinite()) { "invalid powerFactor" }
        require(limitPercent > 0.0 && limitPercent.isFinite()) { "invalid limitPercent" }

        val sinPhi = sqrt(max(0.0, 1.0 - powerFactor * powerFactor))

        val loopResistance = when {
            wiring.usesReturnConductor -> 2.0 * resistancePerConductorOhm
            else -> resistancePerConductorOhm
        }
        val loopReactance = when {
            wiring.usesReturnConductor -> 2.0 * reactancePerConductorOhm
            else -> reactancePerConductorOhm
        }
        val loopImpedance = sqrt(loopResistance * loopResistance + loopReactance * loopReactance)

        val deltaV = when {
            wiring.usesSqrt3 -> sqrt(3.0) * loadCurrentA * (resistancePerConductorOhm * powerFactor + reactancePerConductorOhm * sinPhi)
            else -> loadCurrentA * (loopResistance * powerFactor + loopReactance * sinPhi)
        }

        val percent = deltaV / supplyVoltageV * 100.0

        val loss = when {
            wiring.usesSqrt3 -> 3.0 * loadCurrentA * loadCurrentA * resistancePerConductorOhm
            wiring.usesReturnConductor -> loadCurrentA * loadCurrentA * loopResistance
            else -> loadCurrentA * loadCurrentA * resistancePerConductorOhm
        }

        return VoltageDropResult(
            loopResistanceOhm = loopResistance,
            loopReactanceOhm = loopReactance,
            loopImpedanceOhm = loopImpedance,
            voltageDropV = deltaV,
            voltageDropPercent = percent,
            powerLossW = loss,
            isWithinLimit = percent <= limitPercent + 1e-12 && abs(percent).isFinite(),
        )
    }
}
