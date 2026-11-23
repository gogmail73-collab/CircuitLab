package com.circuitlab.app.engine

import com.circuitlab.app.model.CircuitComponent
import com.circuitlab.app.model.CircuitProject
import com.circuitlab.app.model.ComponentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DcSolverTest {
    private fun battery(volts: Double = 9.0) =
        CircuitComponent(type = ComponentType.BATTERY, label = "B1", value = volts, unit = "V", x = 0f, y = 0f)

    private fun resistor(label: String, ohms: Double) =
        CircuitComponent(type = ComponentType.RESISTOR, label = label, value = ohms, unit = "Ω", x = 0f, y = 0f)

    @Test fun seriesLoopObeysOhmsLawAndKirchhoffVoltageLaw() {
        val result = DcSolver.solve(CircuitProject(components = listOf(battery(), resistor("R1", 1000.0), resistor("R2", 2000.0))))
        assertTrue(result.success)
        assertEquals(0.003, result.branchCurrents["R1"]!!, 1e-12)
        assertEquals(3.0, result.voltageDrops["R1"]!!, 1e-9)
        assertEquals(6.0, result.voltageDrops["R2"]!!, 1e-9)
        assertEquals(0.009, result.powers["R1"]!!, 1e-9)
    }

    @Test fun openSwitchProducesZeroCurrent() {
        val result = DcSolver.solve(CircuitProject(components = listOf(
            battery(), resistor("R1", 1000.0),
            CircuitComponent(type = ComponentType.SWITCH, label = "S1", value = 0.0, unit = "", x = 0f, y = 0f, enabled = false)
        )))
        assertTrue(result.success)
        assertEquals(0.0, result.branchCurrents["R1"]!!, 0.0)
    }

    @Test fun missingBatteryIsRejected() {
        val result = DcSolver.solve(CircuitProject(components = listOf(resistor("R1", 1000.0))))
        assertFalse(result.success)
    }

    @Test fun nonPositiveResistanceIsRejected() {
        val result = DcSolver.solve(CircuitProject(components = listOf(battery(), resistor("R1", 0.0))))
        assertFalse(result.success)
    }

    @Test fun nonPositiveSourceIsRejected() {
        val result = DcSolver.solve(CircuitProject(components = listOf(battery(0.0), resistor("R1", 1000.0))))
        assertFalse(result.success)
    }
}
