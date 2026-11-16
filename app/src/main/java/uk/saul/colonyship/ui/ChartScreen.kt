package uk.saul.colonyship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.ChartStop
import uk.saul.colonyship.game.GameState
import uk.saul.colonyship.game.Physics
import uk.saul.colonyship.game.RunRecord
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class ChartNode(val stop: ChartStop, val px: Float, val py: Float)

@Composable
fun ChartScreen(g: GameController, s: GameState) {
    val play = cue()
    BackHandler {
        play(Cue.BUTTON)
        g.chartOpen = false
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Panel {
            SectionTitle("SC", "STELLAR CARTOGRAPHY", "${"%.1f".format(Physics.distanceFromSol(s))} LY · ${Physics.earthYear(s)} AD")
            Text(
                "Sol at the origin. Each node is a system we reached. Lines are the burns we flew. Arrival years are Earth’s, after the lag of light and of speed.",
                style = body(11.sp, C.muted)
            )
            Spacer(Modifier.height(8.dp))
            VoyageChart(route = s.route.toList(), curX = s.posX, curY = s.posY, curZ = s.posZ, interactive = true)
            Spacer(Modifier.height(8.dp))
            ChartLegend()
            Spacer(Modifier.height(8.dp))
            ConsoleKey("RETURN TO CONSOLE", Modifier.fillMaxWidth(), KeyStyle.TEAL, height = 42.dp) { g.chartOpen = false }
        }
    }
}

/** History-book map page: the same chart, with the settled world as ‘here’. */
@Composable
fun BookChartPage(r: RunRecord) {
    val last = r.route.lastOrNull()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("PLATE · THE VOYAGE", style = label(10.sp, C.inkRed, 0.14f))
        Spacer(Modifier.height(4.dp))
        Text("From Sol to ${r.planetName}", style = body(16.sp, C.ink))
        Text(
            "${"%.1f".format(r.distanceLy)} light years · Earth ${r.earthYear} AD · ship time ${"%.1f".format(r.shipYears)} years",
            style = label(9.sp, Color(0xFF6A4A32), 0.08f)
        )
        Spacer(Modifier.height(8.dp))
        VoyageChart(
            route = r.route,
            curX = last?.x ?: 0.0, curY = last?.y ?: 0.0, curZ = last?.z ?: 0.0,
            interactive = true,
            paper = true,
        )
        Spacer(Modifier.height(8.dp))
        ChartLegend(paper = true)
    }
}

@Composable
fun VoyageChart(
    route: List<ChartStop>,
    curX: Double,
    curY: Double,
    curZ: Double,
    interactive: Boolean,
    paper: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var selected by remember(route) { mutableStateOf(route.lastOrNull { it.kind != "sol" }?.id ?: route.lastOrNull()?.id) }
    val play = cue()
    val pulse = rememberInfiniteTransition(label = "here")
    val glow by pulse.animateFloat(0.45f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val stops = route.ifEmpty {
        listOf(ChartStop("sol", "Sol", 0.0, 0.0, 0.0, 0.0, Physics.LAUNCH_YEAR, 0.0, "sol", "Launch from Earth–Moon L1."))
    }
    val plate = RoundedCornerShape(6.dp)
    val edge = if (paper) Color(0xFF7A5A38) else Color(0xFFC4A46A)
    Column(modifier.fillMaxWidth()) {
        var layout by remember { mutableStateOf(emptyList<ChartNode>()) }
        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(1.05f).clip(plate)
                .background(Brush.verticalGradient(listOf(Color(0xFF16100A), Color(0xFF0B141C), Color(0xFF07080E))))
                .border(2.dp, edge, plate)
        ) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            val nodes = remember(stops, curX, curY, curZ, w, h) { projectStops(stops, curX, curY, curZ, w, h) }
            layout = nodes
            Canvas(
                Modifier.fillMaxSize().then(
                    if (!interactive) Modifier else Modifier.pointerInput(nodes) {
                        detectTapGestures { tap ->
                            val nearest = nodes.filter { it.stop.kind != "here" }.minByOrNull { hypot(it.px - tap.x, it.py - tap.y) }
                            if (nearest != null && hypot(nearest.px - tap.x, nearest.py - tap.y) < 64f) {
                                selected = nearest.stop.id
                                play(Cue.TICK)
                            }
                        }
                    }
                )
            ) {
                drawChartField(nodes, glow)
            }
            val density = LocalDensity.current
            nodes.filter { it.stop.kind != "here" }.forEach { n ->
                val label = if (n.stop.kind == "sol") "SOL" else n.stop.name.uppercase().take(18)
                Text(
                    label,
                    style = label(7.sp, Color(0xFFE8D6A8), 0.06f),
                    maxLines = 1,
                    modifier = Modifier.offset {
                        IntOffset((n.px - 18).roundToInt(), (n.py + 8).roundToInt())
                    }.hapticClick(Cue.TICK, enabled = interactive) { selected = n.stop.id }
                )
            }
            // silence unused density if labels use raw px already (canvas space == box px)
            density.let { }
        }
        val sel = stops.firstOrNull { it.id == selected } ?: stops.lastOrNull()
        if (sel != null) {
            Spacer(Modifier.height(8.dp))
            StopCard(sel, paper)
        }
    }
}

@Composable
private fun StopCard(s: ChartStop, paper: Boolean) {
    val fg = if (paper) C.ink else C.cream
    val sub = if (paper) Color(0xFF6A4A32) else C.muted
    val bg = if (paper) Color(0x33A07040) else Color(0xFF14100C)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(bg)
            .border(1.dp, if (paper) Color(0xFF9A7A5A) else Color(0xFF6B5440), RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(kindLabel(s.kind), style = label(8.sp, if (paper) C.inkRed else C.mustard, 0.14f))
        Text(s.name, style = if (paper) body(16.sp, fg) else vfd(18.sp, C.glow))
        Text(
            "${"%.1f".format(s.ly)} LY FROM SOL  ·  EARTH ${s.earthYear} AD  ·  SHIP ${"%.1f".format(s.shipYears)} YRS",
            style = label(8.sp, sub, 0.08f)
        )
        if (s.note.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(s.note, style = body(11.sp, fg))
        }
    }
}

@Composable
fun ChartLegend(paper: Boolean = false) {
    val c = if (paper) Color(0xFF5A3A22) else C.muted
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(
            Color(0xFFFFC857) to "SOL",
            Color(0xFF7FF5DC) to "SHIP",
            Color(0xFFF0A45A) to "SYSTEM",
            Color(0xFF8EC7FF) to "SIGNAL",
            Color(0xFFE8D15A) to "TABBY",
            Color(0xFFFF6A4A) to "CRISIS",
            Color(0xFF9AE08A) to "SETTLED",
        ).forEach { (col, name) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(col, RoundedCornerShape(1.dp)))
                Spacer(Modifier.size(3.dp))
                Text(name, style = label(6.5.sp, c, 0.06f), maxLines = 1)
            }
        }
    }
}

private fun kindLabel(kind: String) = when (kind) {
    "sol" -> "ORIGIN"
    "tabby" -> "KIC 8462852 · BOYAJIAN’S STAR"
    "signal" -> "NOTABLE SIGNAL"
    "crisis" -> "FORCED LANDFALL · ALERT"
    "settled" -> "LANDFALL"
    "lead" -> "RECOMMENDED WORLD"
    else -> "VISITED SYSTEM"
}

private fun projectStops(stops: List<ChartStop>, curX: Double, curY: Double, curZ: Double, w: Float, h: Float): List<ChartNode> {
    val here = ChartStop("here", "Here", curX, curY, curZ, hypot(curX, hypot(curY, curZ)), 0, 0.0, "here", "")
    val all = stops + here
    // Project X/Y onto the page; add a little of Z so a 3D path still opens out.
    fun px(s: ChartStop) = s.x + 0.35 * s.z
    fun py(s: ChartStop) = s.y - 0.22 * s.z
    val xs = all.map { px(it) }
    val ys = all.map { py(it) }
    val minX = xs.min() - 1.0
    val maxX = xs.max() + 1.0
    val minY = ys.min() - 1.0
    val maxY = ys.max() + 1.0
    val span = max(maxX - minX, 1e-6)
    val spanY = max(maxY - minY, 1e-6)
    val pad = 48f
    val usableW = (w - pad * 2).coerceAtLeast(8f)
    val usableH = (h - pad * 2).coerceAtLeast(8f)
    val scale = minOf(usableW / span.toFloat(), usableH / spanY.toFloat())
    val cx = (minX + maxX) / 2.0
    val cy = (minY + maxY) / 2.0
    return all.map { s ->
        ChartNode(
            s,
            (w / 2f) + ((px(s) - cx) * scale).toFloat(),
            (h / 2f) - ((py(s) - cy) * scale).toFloat(),
        )
    }
}

private fun DrawScope.drawChartField(nodes: List<ChartNode>, glow: Float) {
    // Star field
    val rnd = Random(7)
    repeat(90) {
        val x = rnd.nextFloat() * size.width
        val y = rnd.nextFloat() * size.height
        val a = 0.15f + rnd.nextFloat() * 0.55f
        drawCircle(Color(0xFFFFF4E0).copy(alpha = a), rnd.nextFloat() * 1.6f + 0.4f, Offset(x, y))
    }
    // Fine grid
    val step = 28.dp.toPx()
    var x = 0f
    while (x < size.width) {
        drawLine(Color(0x22C4A46A), Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(Color(0x22C4A46A), Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
    // Concentric range rings around Sol if present
    val sol = nodes.firstOrNull { it.stop.kind == "sol" }
    if (sol != null) {
        for (i in 1..4) {
            drawCircle(Color(0x33C4A46A), 28.dp.toPx() * i, Offset(sol.px, sol.py), style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))))
        }
    }
    val pathNodes = nodes.filter { it.stop.kind != "here" }
    if (pathNodes.size >= 2) {
        val path = Path().apply {
            moveTo(pathNodes.first().px, pathNodes.first().py)
            pathNodes.drop(1).forEach { lineTo(it.px, it.py) }
        }
        drawPath(path, Color(0xAAF0A45A), style = Stroke(3.5f))
        drawPath(path, Color(0x66FFE2A0), style = Stroke(1.2f))
    }
    // Dotted leg from last stop to current position, if we have moved on
    val last = pathNodes.lastOrNull()
    val here = nodes.firstOrNull { it.stop.kind == "here" }
    if (last != null && here != null && hypot(last.px - here.px, last.py - here.py) > 8f) {
        drawLine(
            Color(0xAA7FF5DC), Offset(last.px, last.py), Offset(here.px, here.py), 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
        )
    }
    nodes.forEach { n ->
        when (n.stop.kind) {
            "sol" -> drawSol(n.px, n.py)
            "here" -> drawHere(n.px, n.py, glow)
            "tabby" -> drawStar(n.px, n.py, Color(0xFFE8D15A), 9f)
            "signal" -> drawDiamond(n.px, n.py, Color(0xFF8EC7FF), 8f)
            "crisis" -> drawTriangle(n.px, n.py, Color(0xFFFF6A4A), 9f)
            "settled" -> drawSquare(n.px, n.py, Color(0xFF9AE08A), 8f)
            "lead" -> {
                drawCircle(Color(0x447FF5DC), 12f, Offset(n.px, n.py))
                drawCircle(Color(0xFF7FF5DC), 5.5f, Offset(n.px, n.py))
            }
            else -> {
                drawCircle(Color(0xFFF0A45A), 5f, Offset(n.px, n.py))
                drawCircle(Color(0xFFFFF0C8), 2.2f, Offset(n.px, n.py))
            }
        }
    }
}

private fun DrawScope.drawSol(x: Float, y: Float) {
    val c = Offset(x, y)
    drawCircle(Color(0x55FFC857), 16f, c)
    drawCircle(Color(0xFFFFC857), 6.5f, c)
    for (i in 0 until 8) {
        val a = i * Math.PI / 4
        val x0 = x + (cos(a) * 8).toFloat()
        val y0 = y + (sin(a) * 8).toFloat()
        val x1 = x + (cos(a) * 15).toFloat()
        val y1 = y + (sin(a) * 15).toFloat()
        drawLine(Color(0xFFFFC857), Offset(x0, y0), Offset(x1, y1), 2f)
    }
}

private fun DrawScope.drawHere(x: Float, y: Float, glow: Float) {
    val c = Offset(x, y)
    drawCircle(Color(0xFF7FF5DC).copy(alpha = 0.25f * glow), 14f * glow, c)
    drawCircle(Color(0xFF7FF5DC), 4.5f, c)
    drawLine(Color(0xFF7FF5DC), Offset(x - 12f, y), Offset(x + 12f, y), 1.6f)
    drawLine(Color(0xFF7FF5DC), Offset(x, y - 12f), Offset(x, y + 12f), 1.6f)
}

private fun DrawScope.drawStar(x: Float, y: Float, col: Color, r: Float) {
    val path = Path()
    for (i in 0 until 8) {
        val a = i * Math.PI / 4 - Math.PI / 2
        val rad = if (i % 2 == 0) r else r * 0.45f
        val px = x + (cos(a) * rad).toFloat()
        val py = y + (sin(a) * rad).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, col)
}

private fun DrawScope.drawDiamond(x: Float, y: Float, col: Color, r: Float) {
    val path = Path().apply {
        moveTo(x, y - r); lineTo(x + r, y); lineTo(x, y + r); lineTo(x - r, y); close()
    }
    drawPath(path, col)
}

private fun DrawScope.drawTriangle(x: Float, y: Float, col: Color, r: Float) {
    val path = Path().apply {
        moveTo(x, y - r); lineTo(x + r, y + r * 0.7f); lineTo(x - r, y + r * 0.7f); close()
    }
    drawPath(path, col)
}

private fun DrawScope.drawSquare(x: Float, y: Float, col: Color, r: Float) {
    drawRect(col, Offset(x - r, y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2))
}
