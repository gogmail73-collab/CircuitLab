package com.circuitlab.app.model
import java.util.UUID
enum class ComponentType { BATTERY, RESISTOR, CAPACITOR, INDUCTOR, DIODE, LED, SWITCH, GROUND }
data class Pin(val x: Float, val y: Float, val node: String)
data class CircuitComponent(val id: String = UUID.randomUUID().toString(), val type: ComponentType, val label: String, val value: Double, val unit: String, val x: Float, val y: Float, val rotation: Int = 0, val enabled: Boolean = true)
data class Wire(val from: String, val to: String)
data class CircuitProject(val name: String = "دائرة جديدة", val components: List<CircuitComponent> = emptyList(), val wires: List<Wire> = emptyList())