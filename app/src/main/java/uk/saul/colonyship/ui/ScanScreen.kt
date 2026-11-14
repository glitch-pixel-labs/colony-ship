package uk.saul.colonyship.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import uk.saul.colonyship.R
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.*
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Re-tints the painted planet so each world looks different. */
fun planetFilter(p: Planet): ColorFilter {
    fun hue(deg: Float): FloatArray {
        val a = Math.toRadians(deg.toDouble()); val c = cos(a).toFloat(); val s = sin(a).toFloat()
        return floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f)
    }
    val m = ColorMatrix(when {
        p.water >= 75 -> hue(-25f).also { it[0] *= 0.7f; it[1] *= 0.7f; it[12] *= 1.15f }
        p.temperature < -30 -> ColorMatrix().apply { setToSaturation(0.25f) }.values.also { it[4] = 18f; it[9] = 30f; it[14] = 55f }
        p.atmosphere <= 1 -> hue(38f)
        p.water < 15 -> hue(-8f).also { it[0] *= 1.12f; it[12] *= 0.6f; it[11] *= 0.6f }
        else -> hue((p.hue - 0.5f) * 70f)
    })
    return ColorFilter.colorMatrix(m)
}

fun scanHeader(s: GameState?): Pair<String, String> {
    val p = s?.planet
    return if (p == null || s.phase != Phase.ORBIT) "SURVEY · STANDBY" to "NO TARGET" else "SURVEY · P${p.passes}/${Engine.MAX_PASSES}" to p.name.uppercase()
}

@Composable
fun ScanScreen(g: GameController, s: GameState) {
    val p = s.planet
    if (p == null || s.phase != Phase.ORBIT) {
        Panel {
            SectionTitle("··", "NO WORLD IN RANGE")
            AiQuote(when (s.phase) {
                Phase.EVENT -> "We’re between stars and something needs your attention first. See the Events screen."
                Phase.OUTCOME -> "Nearly there. Continue to the next star and I’ll start the survey."
                Phase.ENDED -> "The voyage is over. The survey drones are, I assume, rusting gently somewhere."
                else -> "Nothing to survey in deep space but more deep space. Engage the jump drive and I’ll find us a candidate."
            }, "SHIP AI · SURVEY")
            Spacer(Modifier.height(10.dp))
            when (s.phase) {
                Phase.EVENT -> ConsoleKey("OPEN EVENTS", Modifier.fillMaxWidth(), KeyStyle.ON) { g.tab = Tab.EVENTS }
                Phase.OUTCOME -> ConsoleKey("CONTINUE TO NEXT STAR", Modifier.fillMaxWidth(), KeyStyle.TEAL) { g.arrive() }
                Phase.CRUISE -> ConsoleKey("GO TO SHIP CONSOLE", Modifier.fillMaxWidth(), KeyStyle.ORANGE) { g.tab = Tab.SHIP }
                else -> {}
            }
        }
        return
    }
    var confirm by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        PlanetHero(p)
        Panel { AiQuote(Voice.scanLine(s, p), null, eye = 40.dp, size = 13, glitch = p.obs.values.any { !it.ok }) }
        Panel {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.background(C.mustard, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) { Text("SCAN", style = label(9.sp, C.ink, 0.05f)) }
                Spacer(Modifier.width(6.dp)); Text("READOUT", style = label(13.sp, C.nix, 0.16f)); Spacer(Modifier.weight(1f))
                Text("CONFIDENCE: ${PlanetGen.confidence(p).let { if (it == "MIDDLING") "MIDDLING" else it }}", style = label(10.sp, C.muted, 0.12f))
            }
            PlanetGen.ROWS.forEach { row -> ReadoutRow(PlanetGen.readout(p, row)) }
        }
        Panel(padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SCANNER ARRAY", style = label(10.sp, C.cream, 0.14f))
                Spacer(Modifier.width(8.dp))
                val h = s.h(Sys.SCAN).toFloat() / 100f
                Canvas(Modifier.weight(1f).height(10.dp)) {
                    drawRoundRect(Color(0xFF0B0705), cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f))
                    clipRect(right = size.width * h) {
                        drawRect(C.mustard)
                        var x = -size.height
                        while (x < size.width) { drawLine(Color(0xFF6B4A10), Offset(x, size.height), Offset(x + size.height, 0f), 3f); x += 7f }
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text("${s.h(Sys.SCAN).roundToInt()}%", style = vfd(17.sp, healthColor(s.h(Sys.SCAN))))
                Spacer(Modifier.width(6.dp))
                ConsoleKey(if (p.passes >= Engine.MAX_PASSES) "MAX PASSES" else "RE-SCAN · −${Engine.RESCAN_COST.roundToInt()} PWR", Modifier.width(118.dp), KeyStyle.DARK, height = 30.dp, textSize = 10,
                    enabled = p.passes < Engine.MAX_PASSES, cueType = Cue.TICK) { g.rescan() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConsoleKey("SETTLE", Modifier.weight(1f), KeyStyle.TEAL, height = 62.dp, textSize = 22, sub = "WAKE ALL ${s.alive()} COLONISTS", cueType = Cue.SELECT) { confirm = true }
            ConsoleKey("KEEP SEARCHING", Modifier.weight(1f), KeyStyle.ORANGE, height = 62.dp, textSize = 17, sub = "−${Engine.JUMP_COST.roundToInt()} POWER · NEXT STAR ${"%.1f".format(s.nextStarLy)} LY", cueType = Cue.SELECT) { g.jump() }
        }
        Spacer(Modifier.height(4.dp))
    }
    if (confirm) Dialog(onDismissRequest = { confirm = false }) {
        Panel {
            SectionTitle("!!", "SETTLE ON ${p.name.uppercase()}?")
            AiQuote("This is it, then. Once I wake them there’s no going back, and the ship won’t fly again. ${if (p.obs.values.any { !it.ok }) "Some readings are still question marks. " else ""}Shall I start the thaw?", null, eye = 36.dp, size = 13)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("NOT YET", Modifier.weight(1f), KeyStyle.DARK) { confirm = false }
                ConsoleKey("BEGIN LANDFALL", Modifier.weight(1.4f), KeyStyle.TEAL, cueType = Cue.SELECT) { confirm = false; g.settle() }
            }
        }
    }
}

@Composable
private fun PlanetHero(p: Planet) {
    val t = rememberInfiniteTransition(label = "planet")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "s")
    val pulse by t.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p")
    val filter = remember(p.name) { planetFilter(p) }
    val conf = PlanetGen.confidence(p)
    Panel(padding = PaddingValues(6.dp), rivets = false) {
        CrtBox(Modifier.fillMaxWidth().aspectRatio(0.86f)) {
            Image(painterResource(R.drawable.art_planet), null, Modifier.fillMaxSize().graphicsLayer { scaleX = 1.05f; scaleY = 1.05f }, contentScale = ContentScale.Crop, colorFilter = filter)
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width * 0.5f, size.height * 0.42f); val r = size.width * 0.47f
                rotate(spin, c) {
                    drawCircle(C.glow.copy(alpha = 0.35f), r, c, style = Stroke(1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))))
                    for (i in 0 until 4) { val a = Math.toRadians(i * 90.0); drawLine(C.glow.copy(alpha = 0.5f), c + Offset((cos(a) * r * 0.92f).toFloat(), (sin(a) * r * 0.92f).toFloat()), c + Offset((cos(a) * r * 1.04f).toFloat(), (sin(a) * r * 1.04f).toFloat()), 2f) }
                }
                drawArc(C.mustard.copy(alpha = 0.7f), 150f, 60f, false, c - Offset(r * 1.02f, r * 1.02f), Size(r * 2.04f, r * 2.04f), style = Stroke(3f))
            }
            val h = p.name.hashCode().let { if (it < 0) -it else it }
            Row(Modifier.padding(9.dp).fillMaxWidth()) {
                Text("PROBE 0${1 + h % 8} · ORBIT ${1 + p.passes} · PASS ${p.passes}/${Engine.MAX_PASSES}", style = vfd(11.sp))
                Spacer(Modifier.weight(1f))
                Text("RA ${h % 24}h ${h % 60}m\nDEC −${h % 80}° ${"%02d".format(h % 60)}′", style = vfd(11.sp))
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth; val hh = maxHeight
                val spots = listOf(Offset(0.08f, 0.24f) to Offset(0.36f, 0.33f), Offset(0.63f, 0.34f) to Offset(0.52f, 0.44f), Offset(0.05f, 0.62f) to Offset(0.3f, 0.52f))
                val labels = p.features.take(3).map { it.uppercase() } + if (p.life == 4 && p.obs["LIFE SIGNS"]?.ok == true) listOf("LIGHTS?? · CHECK") else emptyList()
                Canvas(Modifier.fillMaxSize()) {
                    labels.take(3).forEachIndexed { i, _ ->
                        val (a, b) = spots[i]
                        drawLine(C.glow.copy(alpha = 0.8f), Offset(a.x * size.width + 30.dp.toPx(), a.y * size.height + 9.dp.toPx()), Offset(b.x * size.width, b.y * size.height), 1.4f)
                        drawCircle(C.glow.copy(alpha = pulse), 3.5f, Offset(b.x * size.width, b.y * size.height))
                    }
                }
                labels.take(3).forEachIndexed { i, l ->
                    val a = spots[i].first
                    Box(Modifier.offset(w * a.x, hh * a.y).background(Color(0xE0140A06), RoundedCornerShape(3.dp)).border(1.dp, if (l.contains("??")) C.warn else C.cream2.copy(alpha = 0.7f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(l, style = label(8.5.sp, if (l.contains("??")) C.warn else C.cream, 0.12f))
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(p.name.uppercase(), style = TextStyle(fontFamily = F.righteous, fontSize = 36.sp, color = C.cream, shadow = Shadow(Color(0xFF5A2008), Offset(3f, 3f), 0f)))
                Text("CANDIDATE WORLD · ${p.distanceLy} LIGHT YEARS · ${p.moons} MOON${if (p.moons == 1) "" else "S"}", style = label(10.sp, C.mustard, 0.14f))
            }
            if (conf != "HIGH") Box(Modifier.align(Alignment.BottomEnd).padding(12.dp).rotate(-12f).border(2.dp, Color(0xFFFF6A3A), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PARTIAL", style = label(13.sp, Color(0xFFFF6A3A), 0.08f)); Text("SCAN", style = label(8.sp, Color(0xFFFF6A3A), 0.2f))
                }
            }
        }
    }
}

@Composable
private fun ReadoutRow(r: Readout) {
    val t = rememberInfiniteTransition(label = "glitch")
    val jit by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse), label = "j")
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).then(if (r.damaged) Modifier.background(Brush.horizontalGradient(listOf(Color(0x40E6A92A), Color(0x14E6A92A))), RoundedCornerShape(3.dp)) else Modifier)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(r.row, style = label(11.sp, if (r.damaged) C.mustard else C.teal, 0.1f), modifier = Modifier.width(98.dp))
        if (r.damaged) {
            Box(Modifier.weight(1f)) {
                Text(r.main, style = label(13.sp, Color(0xFFFF2850).copy(alpha = 0.7f), 0.12f), modifier = Modifier.offset(x = (-1.5f + jit).dp))
                Text(r.main, style = label(13.sp, Color(0xFF28F0FF).copy(alpha = 0.7f), 0.12f), modifier = Modifier.offset(x = (1.5f - jit).dp))
                Text(r.main, style = label(13.sp, C.warn, 0.12f))
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(r.main, style = body(13.sp, C.cream).copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), maxLines = 1)
                if (r.sub.isNotBlank()) Text(r.sub, style = body(10.sp, C.muted), maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 1..5) {
                val on = !r.damaged && i <= r.bars
                Box(Modifier.size(9.dp, 12.dp).clip(RoundedCornerShape(2.dp)).background(when {
                    r.damaged -> Color(0x1AFFFFFF)
                    on && r.bars <= 2 -> C.mustard
                    on -> Color(0xFF5FE3CF)
                    else -> Color(0x14FFFFFF)
                }))
            }
        }
    }
}
