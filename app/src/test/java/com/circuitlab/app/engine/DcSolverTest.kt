package com.circuitlab.app.engine

import com.circuitlab.app.model.CircuitComponent
import com.circuitlab.app.model.CircuitProject
import com.circuitlab.app.model.ComponentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DcSolverTest {
    private fun component(
        type: ComponentType,
        label: String,
        value: Double,
        enabled: Boolean = true
    ) = CircuitComponent(
        type = type,
        label = label,
        value = value,
        unit = "",
        x = 0.5f,
        y = 0.5f,
        enabled = enabled
    )

    @Test
    fun calculatesSeriesCurrentAndVoltageDrops() {
        val project = CircuitProject(components = listOf(
            component(ComponentType.BATTERY, "V1", 9.0),
            component(ComponentType.RESISTOR, "R1", 1000.0),
            component(ComponentType.RESISTOR, "R2", 2000.0)
        ))

        val result = DcSolver.solve(project)

        assertTrue(result.success)
        assertEquals(0.003, result.branchCurrents["R1"]!!, 1e-9)
        assertEquals(3.0, result.voltageDrops["R1"]!!, 1e-9)
        assertEquals(6.0, result.voltageDrops["R2"]!!, 1e-9)
        assertEquals(0.018, result.powers["R2"]!!, 1e-9)
    }

    @Test
    fun rejectsMissingVoltageSource() {
        val result = DcSolver.solve(CircuitProject(components = listOf(
            component(ComponentType.RESISTOR, "R1", 1000.0)
        )))
        assertFalse(result.success)
    }

    @Test
    fun rejectsZeroResistance() {
        val result = DcSolver.solve(CircuitProject(components = listOf(
            component(ComponentType.BATTERY, "V1", 9.0),
            component(ComponentType.RESISTOR, "R1", 0.0)
        )))
        assertFalse(result.success)
    }

    @Test
    fun openSwitchProducesZeroCurrent() {
        val result = DcSolver.solve(CircuitProject(components = listOf(
            component(ComponentType.BATTERY, "V1", 9.0),
            component(ComponentType.RESISTOR, "R1", 1000.0),
            component(ComponentType.SWITCH, "S1", 0.0, enabled = false)
        )))
        assertTrue(result.success)
        assertEquals(0.0, result.branchCurrents["R1"]!!, 0.0)
    }
}
