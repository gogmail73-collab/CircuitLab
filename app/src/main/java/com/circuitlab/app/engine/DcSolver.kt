package com.circuitlab.app.engine

import com.circuitlab.app.model.*
import java.util.Locale
import kotlin.math.abs

/**
 * Educational steady-state DC solver for a single series loop.
 *
 * IMPORTANT: the current UI stores wire drawing coordinates, not electrical
 * net identities. Therefore this solver is not a general topology-aware/SPICE
 * solver. It computes the documented single-loop teaching case only.
 */
object DcSolver {
    data class Result(
        val success: Boolean,
        val message: String,
        val nodeVoltages: Map<String, Double> = emptyMap(),
        val branchCurrents: Map<String, Double> = emptyMap(),
        val voltageDrops: Map<String, Double> = emptyMap(),
        val powers: Map<String, Double> = emptyMap()
    )

    fun solve(project: CircuitProject): Result {
        val source = project.components.firstOrNull {
            it.type == ComponentType.BATTERY && it.enabled
        } ?: return Result(false, "أضف مصدر جهد فعالًا قبل بدء المحاكاة.")

        if (!source.value.isFinite() || source.value <= 0.0) {
            return Result(false, "قيمة مصدر الجهد يجب أن تكون رقمًا موجبًا وصالحًا.")
        }

        val resistors = project.components.filter {
            it.type == ComponentType.RESISTOR && it.enabled
        }
        if (resistors.isEmpty()) {
            return Result(false, "أضف مقاومة واحدة على الأقل إلى الحلقة.")
        }

        val invalid = resistors.firstOrNull { !it.value.isFinite() || it.value <= 0.0 }
        if (invalid != null) {
            return Result(false, "قيمة " + invalid.label + " يجب أن تكون أكبر من صفر.")
        }

        val openSwitch = project.components.firstOrNull {
            it.type == ComponentType.SWITCH && !it.enabled
        }
        if (openSwitch != null) {
            return Result(
                success = true,
                message = "الدائرة مفتوحة عند " + openSwitch.label + "؛ التيار يساوي 0 A.",
                nodeVoltages = mapOf("GND" to 0.0, "SOURCE+" to source.value),
                branchCurrents = resistors.associate { it.label to 0.0 },
                voltageDrops = resistors.associate { it.label to 0.0 },
                powers = resistors.associate { it.label to 0.0 }
            )
        }

        val totalResistance = resistors.sumOf { it.value }
        if (!totalResistance.isFinite() || totalResistance <= 0.0) {
            return Result(false, "تعذر حساب المقاومة المكافئة؛ راجع قيم العناصر.")
        }

        val current = source.value / totalResistance
        if (!current.isFinite()) {
            return Result(false, "نتيجة التيار خارج النطاق العددي الصالح.")
        }

        val drops = resistors.associate { it.label to current * it.value }
        val powers = resistors.associate { it.label to current * current * it.value }
        val totalDrop = drops.values.sum()
        val mismatch = abs(totalDrop - source.value)
        if (mismatch > maxOf(1e-8, source.value * 1e-6)) {
            return Result(false, "فشل التحقق من قانون كيرشوف للجهد.")
        }

        val details = resistors.joinToString("، ") { resistor ->
            val drop = drops[resistor.label] ?: 0.0
            resistor.label + ": " + format(drop) + " V"
        }
        return Result(
            success = true,
            message = "تحليل حلقة توالي تعليمي — R المكافئة = " + format(totalResistance) +
                " Ω، التيار = " + format(current) + " A، هبوط الجهد: " + details,
            nodeVoltages = mapOf("GND" to 0.0, "SOURCE+" to source.value),
            branchCurrents = resistors.associate { it.label to current },
            voltageDrops = drops,
            powers = powers
        )
    }

    private fun format(value: Double): String =
        String.format(Locale.US, "%.4f", value)
}
