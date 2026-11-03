package com.circuitlab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.circuitlab.app.engine.DcSolver
import com.circuitlab.app.model.*
import kotlin.math.*

private val Bg = Color(0xFF0B141C)
private val Panel = Color(0xFF172632)
private val Mint = Color(0xFF20D6B0)
private val TextSoft = Color(0xFF9FB2BF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CircuitLabApp() }
    }
}

@Composable
fun CircuitLabApp() {
    var mode by remember { mutableStateOf("breadboard") }
    var project by remember { mutableStateOf(CircuitProject()) }
    var chosen by remember { mutableStateOf<ComponentType?>(null) }
    var wireMode by remember { mutableStateOf(false) }
    var wireStart by remember { mutableStateOf<Offset?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<CircuitComponent?>(null) }
    var status by remember { mutableStateOf("اختار عنصر من المكتبة ثم اضغط على اللوحة لوضعه.") }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CircuitLab", color = Mint, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("مختبر الدوائر الإلكترونية", color = TextSoft, fontSize = 12.sp)
                }
                Text("BETA • OFFLINE", color = TextSoft, fontSize = 10.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(4.dp)) {
                listOf("breadboard" to "لوحة التجارب 3D", "schematic" to "المخطط").forEach { (key, label) ->
                    Box(Modifier.weight(1f).background(if (mode == key) Mint.copy(alpha = .18f) else Color.Transparent, RoundedCornerShape(11.dp))
                        .clickable { mode = key; wireMode = false; wireStart = null }.padding(12.dp), contentAlignment = Alignment.Center) {
                        Text(label, color = if (mode == key) Mint else TextSoft, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("مكتبة المكونات", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("اضغط للاختيار", color = TextSoft, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComponentType.entries.forEach { type ->
                    val active = chosen == type
                    Surface(color = if (active) Mint.copy(alpha = .22f) else Panel, shape = RoundedCornerShape(12.dp),
                        border = if (active) androidx.compose.foundation.BorderStroke(1.dp, Mint) else null,
                        modifier = Modifier.clickable { chosen = type; wireMode = false }) {
                        Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(type.symbol(), color = if (active) Mint else Color.White, fontSize = 18.sp)
                            Text(type.arName(), color = if (active) Mint else TextSoft, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (mode == "breadboard") "لوحة التجارب الحقيقية • نقاط توصيل" else "مخطط الدائرة • التوصيلات", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${project.components.size} عنصر", color = Mint, fontSize = 11.sp)
            }
            Spacer(Modifier.height(7.dp))
            CircuitWorkspace(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                mode = mode,
                project = project,
                selected = selected,
                wireMode = wireMode,
                onBoardTap = { point ->
                    if (wireMode) {
                        val first = wireStart
                        if (first == null) {
                            wireStart = point
                            status = "اختار نقطة النهاية للسلك."
                        } else {
                            val a = "${first.x.roundToInt()},${first.y.roundToInt()}"
                            val b = "${point.x.roundToInt()},${point.y.roundToInt()}"
                            if (a != b) project = project.copy(wires = project.wires + Wire(a, b))
                            wireStart = null
                            status = "تمت إضافة التوصيلة."
                        }
                    } else {
                        val type = chosen
                        if (type == null) {
                            status = "اختار مكونًا أولًا من المكتبة."
                        } else {
                            val (v, u) = type.defaultValue()
                            val count = project.components.count { it.type == type } + 1
                            val c = CircuitComponent(type = type, label = "${type.arName()} $count", value = v, unit = u,
                                x = point.x, y = point.y)
                            project = project.copy(components = project.components + c)
                            selected = c.id
                            status = "${c.label} اتضاف. اضغط على المكون لتعديل قيمته."
                        }
                    }
                },
                onComponentTap = { c -> selected = c.id; editTarget = c }
            )
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { wireMode = !wireMode; chosen = null; wireStart = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (wireMode) Mint else TextSoft),
                    modifier = Modifier.weight(1f)) { Text(if (wireMode) "إلغاء توصيل السلك" else "＋ توصيل سلك") }
                Button(onClick = {
                    val r = DcSolver.solve(project)
                    status = if (r.success) "نتيجة تعليمية: ${r.message}" else r.message
                }, modifier = Modifier.weight(1f)) { Text("▶ محاكاة DC") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val c = project.components.firstOrNull { it.id == selected }
                    if (c != null) { project = project.copy(components = project.components.filterNot { it.id == c.id }); selected = null; status = "تم حذف العنصر." }
                    else { project = CircuitProject(); status = "بدأت دائرة جديدة." }
                }) { Text(if (selected != null) "حذف المحدد" else "دائرة جديدة", color = TextSoft) }
                Text(status, color = Mint, fontSize = 10.sp, modifier = Modifier.weight(1f))
            }
        }
    }

    editTarget?.let { c ->
        var value by remember(c.id) { mutableStateOf(c.value.toString()) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("خصائص ${c.label}") },
            text = { OutlinedTextField(value, { value = it }, label = { Text("القيمة (${c.unit})") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    value.toDoubleOrNull()?.let { n ->
                        project = project.copy(components = project.components.map { if (it.id == c.id) it.copy(value = n) else it })
                        status = "تم تحديث قيمة العنصر."
                    }
                    editTarget = null
                }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("إلغاء") } },
            containerColor = Panel
        )
    }
}

@Composable
private fun CircuitWorkspace(
    modifier: Modifier,
    mode: String,
    project: CircuitProject,
    selected: String?,
    wireMode: Boolean,
    onBoardTap: (Offset) -> Unit,
    onComponentTap: (CircuitComponent) -> Unit
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    Box(modifier.background(Color(0xFF101D27), RoundedCornerShape(18.dp)).padding(5.dp)) {
        Canvas(Modifier.fillMaxSize().pointerInput(mode, wireMode, project.components) {
            detectTapGestures { tap ->
                val hit = project.components.lastOrNull { c ->
                    val p = boardPoint(c.x, c.y, size.width.toFloat(), size.height.toFloat(), mode)
                    (tap - p).getDistance() < 34f
                }
                if (hit != null && !wireMode) onComponentTap(hit) else {
                    val logical = Offset((tap.x / size.width).coerceIn(0f, 1f), (tap.y / size.height).coerceIn(0f, 1f))
                    onBoardTap(logical)
                }
            }
        }) {
            canvasSize = size
            if (mode == "breadboard") drawBreadboard(project, selected, size)
            else drawSchematic(project, selected, size)
        }
        Text(if (wireMode) "وضع توصيل الأسلاك: اضغط نقطتين" else if (mode == "breadboard") "اضغط اللوحة لوضع العنصر • اضغط العنصر لتعديل قيمته" else "اضغط مساحة المخطط لإضافة المكون",
            color = Color(0xFFB6C8D0), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp))
    }
}

private fun boardPoint(x: Float, y: Float, w: Float, h: Float, mode: String): Offset =
    if (mode == "breadboard") Offset(35f + x * (w - 70f), 38f + y * (h - 82f)) else Offset(25f + x * (w - 50f), 25f + y * (h - 50f))

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBreadboard(project: CircuitProject, selected: String?, size: Size) {
    val w = size.width; val h = size.height
    val left = 15f; val top = 18f; val right = w - 15f; val bottom = h - 20f
    val board = Path().apply { moveTo(left + 12, top + 18); lineTo(right - 22, top); lineTo(right, bottom - 20); lineTo(left + 28, bottom); close() }
    drawPath(board, Color(0xFF173E3A))
    drawPath(board, Color(0xFF397A6C), style = Stroke(5f))
    // raised top lip and side thickness
    drawLine(Color(0xFF0A2527), Offset(left + 28, bottom), Offset(right, bottom - 20), 13f)
    drawLine(Color(0xFF0A2527), Offset(right, bottom - 20), Offset(right - 22, top), 13f)
    val cols = 24; val rows = 30
    for (r in 0 until rows) {
        val y = top + 35f + r * ((bottom - top - 70f) / (rows - 1))
        val perspective = 1f - (r.toFloat() / rows) * .12f
        for (c in 0 until cols) {
            val x = left + 34f + c * ((right - left - 68f) / (cols - 1)) + (r * 0.45f)
            val centerGap = abs(r - 14.5f) < 1.6f
            if (!centerGap) {
                drawCircle(Color(0xFF092725), 4.1f * perspective, Offset(x, y + 2f))
                drawCircle(Color(0xFFB8D2C7), 2.5f * perspective, Offset(x, y))
                drawCircle(Color(0xFF527C70), 1.1f * perspective, Offset(x - 0.5f, y - 0.5f))
            }
        }
    }
    // power rails
    listOf(0 to Color(0xFFE65E63), 1 to Color(0xFF5D9CEB), 28 to Color(0xFFE65E63), 29 to Color(0xFF5D9CEB)).forEach { (r, color) ->
        val y = top + 35f + r * ((bottom - top - 70f) / (rows - 1))
        drawLine(color.copy(alpha = .9f), Offset(left + 27f, y), Offset(right - 10f, y - 5f), 2f)
    }
    // wire paths are visual leads between selected board coordinates
    project.wires.forEach { wire ->
        val a = wire.from.split(","); val b = wire.to.split(",")
        if (a.size == 2 && b.size == 2) {
            val p1 = Offset(a[0].toFloatOrNull() ?: 0f, a[1].toFloatOrNull() ?: 0f)
            val p2 = Offset(b[0].toFloatOrNull() ?: 0f, b[1].toFloatOrNull() ?: 0f)
            drawLine(Color(0xFFFFB84D), boardPoint(p1.x, p1.y, w, h, "breadboard"), boardPoint(p2.x, p2.y, w, h, "breadboard"), 5f, cap = StrokeCap.Round)
        }
    }
    project.components.forEach { c ->
        val p = boardPoint(c.x, c.y, w, h, "breadboard")
        val tint = if (c.id == selected) Mint else Color(0xFFE7EEF2)
        drawLine(Color(0xFFCBD7D9), Offset(p.x - 15f, p.y + 4f), Offset(p.x - 15f, p.y + 19f), 3f)
        drawLine(Color(0xFFCBD7D9), Offset(p.x + 15f, p.y + 4f), Offset(p.x + 15f, p.y + 19f), 3f)
        drawRoundRect(Color(0xFF07131B), Offset(p.x - 23, p.y - 13), Size(46f, 25f), androidx.compose.ui.geometry.CornerRadius(6f))
        drawRoundRect(if (c.type == ComponentType.LED) Color(0xFFE14C39) else if (c.type == ComponentType.RESISTOR) Color(0xFFC99B5A) else Color(0xFF315C78),
            Offset(p.x - 20, p.y - 16), Size(40f, 23f), androidx.compose.ui.geometry.CornerRadius(6f))
        if (c.type == ComponentType.RESISTOR) for (i in -1..1) drawLine(Color(0xFF39251B), Offset(p.x + i * 9f, p.y - 15), Offset(p.x + i * 9f, p.y + 5), 3f)
        drawRoundRect(tint.copy(alpha = .8f), Offset(p.x - 25, p.y - 20), Size(50f, 32f), androidx.compose.ui.geometry.CornerRadius(7f), style = Stroke(if (c.id == selected) 3f else 1f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSchematic(project: CircuitProject, selected: String?, size: Size) {
    val w = size.width; val h = size.height
    for (x in 0..(w / 28).toInt()) for (y in 0..(h / 28).toInt()) drawCircle(Color(0xFF29404B), 1.2f, Offset(x * 28f, y * 28f))
    project.wires.forEach { wire ->
        val a = wire.from.split(","); val b = wire.to.split(",")
        if (a.size == 2 && b.size == 2) drawLine(Mint, Offset((a[0].toFloatOrNull() ?: 0f) * w, (a[1].toFloatOrNull() ?: 0f) * h), Offset((b[0].toFloatOrNull() ?: 0f) * w, (b[1].toFloatOrNull() ?: 0f) * h), 3f)
    }
    project.components.forEach { c ->
        val p = boardPoint(c.x, c.y, w, h, "schematic")
        val color = if (c.id == selected) Mint else Color.White
        when (c.type) {
            ComponentType.RESISTOR -> {
                drawLine(color, Offset(p.x - 40, p.y), Offset(p.x - 22, p.y), 3f)
                val path = Path().apply { moveTo(p.x - 22, p.y); for (i in 0..6) lineTo(p.x - 22 + i * 7.3f, p.y + if (i % 2 == 0) -11f else 11f); lineTo(p.x + 22, p.y) }
                drawPath(path, color, style = Stroke(3f)); drawLine(color, Offset(p.x + 22, p.y), Offset(p.x + 40, p.y), 3f)
            }
            ComponentType.BATTERY -> {
                drawLine(color, Offset(p.x - 35, p.y), Offset(p.x - 7, p.y), 3f)
                drawLine(color, Offset(p.x - 7, p.y - 20), Offset(p.x - 7, p.y + 20), 4f)
                drawLine(color, Offset(p.x + 7, p.y - 12), Offset(p.x + 7, p.y + 12), 3f)
                drawLine(color, Offset(p.x + 7, p.y), Offset(p.x + 35, p.y), 3f)
            }
            else -> {
                drawRoundRect(Color(0xFF213846), Offset(p.x - 28, p.y - 17), Size(56f, 34f), androidx.compose.ui.geometry.CornerRadius(6f))
                drawRoundRect(color, Offset(p.x - 28, p.y - 17), Size(56f, 34f), androidx.compose.ui.geometry.CornerRadius(6f), style = Stroke(2f))
                drawLine(color, Offset(p.x - 40, p.y), Offset(p.x - 28, p.y), 3f)
                drawLine(color, Offset(p.x + 28, p.y), Offset(p.x + 40, p.y), 3f)
            }
        }
        drawContext.canvas.nativeCanvas.drawText(c.label, p.x - 30f, p.y + 38f, android.graphics.Paint().apply { this.color = android.graphics.Color.WHITE; textSize = 23f })
    }
}

private fun ComponentType.arName() = when (this) {
    ComponentType.BATTERY -> "بطارية"; ComponentType.RESISTOR -> "مقاومة"; ComponentType.CAPACITOR -> "مكثف"
    ComponentType.INDUCTOR -> "ملف"; ComponentType.DIODE -> "دايود"; ComponentType.LED -> "LED"
    ComponentType.SWITCH -> "مفتاح"; ComponentType.GROUND -> "أرضي"
}
private fun ComponentType.symbol() = when (this) {
    ComponentType.BATTERY -> "🔋"; ComponentType.RESISTOR -> "▰"; ComponentType.CAPACITOR -> "Ⅱ"
    ComponentType.INDUCTOR -> "∿"; ComponentType.DIODE -> "▷"; ComponentType.LED -> "✦"
    ComponentType.SWITCH -> "⏻"; ComponentType.GROUND -> "⏚"
}
private fun ComponentType.defaultValue(): Pair<Double, String> = when (this) {
    ComponentType.BATTERY -> 9.0 to "V"; ComponentType.RESISTOR -> 1000.0 to "Ω"
    ComponentType.CAPACITOR -> 100.0 to "µF"; ComponentType.INDUCTOR -> 10.0 to "mH"
    else -> 0.0 to ""
}
