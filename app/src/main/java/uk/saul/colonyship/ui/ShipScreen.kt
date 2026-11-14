package uk.saul.colonyship.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.R
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.*
import kotlin.math.roundToInt

fun healthColor(v: Double) = when { v < 40 -> C.bad; v < 70 -> C.warn; else -> C.glow }

@Composable
fun ShipScreen(g: GameController, s: GameState) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        ShipCam(s)
        Panel { AiQuote(Voice.statusLine(s), "SHIP AI · MOOD: ${Voice.mood(s)}", glitch = Voice.stress(s) >= 3) }
        VoyagePanel(g, s)
        CryoPanel(s)
        PowerPanel(g, s)
        HealthPanel(s)
        ConsumablesPanel(s)
        if (s.awake.isNotEmpty()) AwakePanel(s)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ShipCam(s: GameState) {
    val t = rememberInfiniteTransition(label = "cam")
    val drift by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d")
    Panel(padding = PaddingValues(6.dp), rivets = false) {
        CrtBox(Modifier.fillMaxWidth().aspectRatio(1.95f)) {
            Image(painterResource(R.drawable.art_ship), null, Modifier.fillMaxSize().graphicsLayer { translationY = drift * 4f; translationX = drift * 3f; scaleX = 1.06f; scaleY = 1.06f }, contentScale = ContentScale.Crop)
            Row(Modifier.padding(7.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("EXT CAM 01 · PORT ELEVATION", style = vfd(11.sp))
                Spacer(Modifier.weight(1f)); Lamp(Color(0xFFFF3B1F), 6.dp); Spacer(Modifier.width(4.dp)); Text("LIVE", style = label(9.sp, Color(0xFFFF9A7A)))
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = maxWidth; val h = maxHeight
                fun tag(x: Float, y: Float, name: String, v: String, col: Color, lx: Float, ly: Float) = Triple(Offset(x, y), Offset(lx, ly), Triple(name, v, col))
                val tags = listOf(
                    tag(0.03f, 0.25f, "SCANNERS", "${s.h(Sys.SCAN).roundToInt()}%", healthColor(s.h(Sys.SCAN)), 0.2f, 0.45f),
                    tag(0.40f, 0.15f, "LIFE SUPPORT", "${s.h(Sys.LIFE).roundToInt()}%", healthColor(s.h(Sys.LIFE)), 0.48f, 0.38f),
                    tag(0.80f, 0.30f, "ENGINES", "${s.h(Sys.ENGINE).roundToInt()}%", healthColor(s.h(Sys.ENGINE)), 0.8f, 0.5f),
                    tag(0.03f, 0.70f, "SHIELDS", "${s.h(Sys.SHIELD).roundToInt()}%", healthColor(s.h(Sys.SHIELD)), 0.14f, 0.62f),
                    tag(0.34f, 0.80f, "CRYOPODS", "${s.alive()}", healthColor(s.h(Sys.CRYO)), 0.42f, 0.62f),
                    tag(0.62f, 0.78f, "REPAIRS", if ((s.power[Channel.REPAIR] ?: 0) < Channel.REPAIR.need) "LOW" else "${s.power[Channel.REPAIR]}", if ((s.power[Channel.REPAIR] ?: 0) < Channel.REPAIR.need) C.warn else C.glow, 0.66f, 0.62f),
                )
                Canvas(Modifier.fillMaxSize()) {
                    tags.forEach { (p, l, _) ->
                        val a = Offset(p.x * size.width + 20.dp.toPx(), p.y * size.height + 10.dp.toPx()); val b = Offset(l.x * size.width, l.y * size.height)
                        drawLine(C.glow.copy(alpha = 0.8f), a, b, 1.5f); drawCircle(C.glow, 3f, b)
                    }
                }
                tags.forEach { (p, _, info) ->
                    Column(Modifier.offset(w * p.x, h * p.y).background(Color(0xE0140A06), RoundedCornerShape(3.dp)).border(1.dp, C.glow.copy(alpha = 0.55f), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                        Text(info.first, style = label(8.sp, C.cream, 0.12f))
                        Text(info.second, style = vfd(13.sp, info.third))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoyagePanel(g: GameController, s: GameState) {
    Panel {
        SectionTitle("00", "NAVIGATION", "TRANSIT ${s.jumps} · ${Physics.shipClock(s)}", lamp = if (s.phase == Phase.EVENT) Color(0xFFFF3B1F) else Color(0xFF3FE0B0))
        NavReadouts(s)
        Spacer(Modifier.height(8.dp))
        if (s.lastJumpReport.isNotEmpty() && s.phase != Phase.CRUISE) {
            Text("LAST TRANSIT", style = label(9.sp, C.teal))
            FlowChips(s.lastJumpReport.take(10).map { it to (it.startsWith("−") && it.contains("POD") || it.contains("EXHAUSTED") || it.contains("FAILING") || it == "BROWNOUT") })
            Spacer(Modifier.height(8.dp))
        }
        when (s.phase) {
            Phase.CRUISE, Phase.ORBIT -> {
                val t = Engine.plannedTransit(s)
                ConsoleKey(if (s.jumps == 0 && s.phase == Phase.CRUISE) "LIGHT THE DRIVE" else "BURN FOR NEXT STAR", Modifier.fillMaxWidth(), KeyStyle.ORANGE, height = 50.dp, textSize = 16,
                    sub = "−${Engine.JUMP_COST.roundToInt()} PWR · ${"%.1f".format(s.nextStarLy)} LY · ${"%.2f".format(t.beta)}c · ${"%.1f".format(t.shipYears)} SHIP / ${"%.1f".format(t.earthYears)} EARTH YRS", cueType = Cue.SELECT) { g.jump() }
                if (s.phase == Phase.ORBIT) Text("We are in orbit of ${s.planet?.name}. Burning for the next star means leaving it behind for good.", style = body(11.sp, C.muted), modifier = Modifier.padding(top = 6.dp))
            }
            Phase.EVENT -> ConsoleKey("INCIDENT PENDING · OPEN EVENTS", Modifier.fillMaxWidth(), KeyStyle.ON, height = 44.dp) { g.tab = Tab.EVENTS }
            Phase.OUTCOME -> ConsoleKey("BRAKE INTO THE NEXT SYSTEM", Modifier.fillMaxWidth(), KeyStyle.TEAL, height = 44.dp) { g.arrive() }
            Phase.ENDED -> ConsoleKey("READ THE HISTORY BOOK", Modifier.fillMaxWidth(), KeyStyle.CREAM, height = 44.dp) { g.tab = Tab.ARCHIVE }
        }
    }
}

/** Ship clock, Earth calendar, distance from Sol and light-lag, in VFD readouts. */
@Composable
fun NavReadouts(s: GameState) {
    val dist = Physics.distanceFromSol(s)
    val cells = listOf(
        Triple("EARTH DATE", "${Physics.earthYear(s)} AD", "LAUNCHED ${Physics.LAUNCH_YEAR}"),
        Triple("FROM SOL", "${"%.1f".format(dist)} LY", "MESSAGE LANDS ${Physics.messageArrives(s)}"),
        Triple("SHIP TIME", "${"%.1f".format(Physics.shipYears(s))} YRS", "EARTH +${"%.1f".format(Physics.earthYears(s))} YRS"),
        Triple("LAST PEAK", if (s.lastBeta > 0) "${"%.2f".format(s.lastBeta)} c" else "—", if (s.lastBeta > 0) "γ ${"%.2f".format(Physics.gamma(s.lastBeta))}" else "AT REST"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { (k, v, sub) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(5.dp)).background(Brush.verticalGradient(listOf(Color(0xFF071311), Color(0xFF0C1F1C))))
                        .border(1.dp, Color(0xFF2B4A44), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 4.dp)) {
                        Text(k, style = label(8.sp, C.teal, 0.14f))
                        Text(v, style = vfd(21.sp), maxLines = 1)
                        Text(sub, style = label(7.5.sp, C.muted, 0.08f), maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun FlowChips(items: List<Pair<String, Boolean>>) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (t, bad) -> Chip(t, bad) }
    }
}

@Composable
private fun CryoPanel(s: GameState) {
    Panel {
        SectionTitle("01", "CRYOPOD BAY", "BAY TEMP −${(196 - (100 - s.h(Sys.CRYO)) * 0.3).roundToInt()}°C")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(112.dp)) {
                Nixies(s.alive().toString().padStart(3, '0').takeLast(if (s.alive() == 1000) 4 else 3), digitW = if (s.alive() == 1000) 24.dp else 30.dp)
                Spacer(Modifier.height(5.dp))
                Text("ALIVE · 1,000 ABOARD", style = label(9.sp, C.cream, 0.1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Lamp(Color(0xFFFF3B1F), 5.dp); Spacer(Modifier.width(3.dp))
                    Text("${s.lost()} LOST · ${s.failing()} FAILING", style = label(9.sp, C.bad, 0.08f))
                }
            }
            Spacer(Modifier.width(8.dp))
            PodGrid(s.pods, Modifier.weight(1f).aspectRatio(1.55f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.End) {
            Legend(C.teal, "STABLE ${s.stable()}"); Spacer(Modifier.width(8.dp)); Legend(C.mustard, "FAILING ${s.failing()}"); Spacer(Modifier.width(8.dp)); Legend(Color(0xFF7A1A10), "LOST ${s.lost()}")
        }
    }
}

@Composable private fun Legend(c: Color, t: String) = Row(verticalAlignment = Alignment.CenterVertically) {
    Box(Modifier.size(7.dp).background(c, RoundedCornerShape(1.dp))); Spacer(Modifier.width(3.dp)); Text(t, style = label(8.sp, C.muted, 0.1f))
}

@Composable
fun PodGrid(pods: String, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "pods")
    val blink by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "b")
    Canvas(modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF071210)).border(1.dp, Color(0xFF2B4A44), RoundedCornerShape(5.dp)).padding(3.dp)) {
        val cols = 40; val rows = 25
        val cw = size.width / cols; val ch = size.height / rows
        val gap = 0.9f
        for (i in pods.indices) {
            val x = (i % cols) * cw; val y = (i / cols) * ch
            val c = when (pods[i]) { 'S' -> C.teal; 'F' -> C.mustard.copy(alpha = blink); else -> Color(0xFF6A1810) }
            drawRect(c, Offset(x + gap / 2, y + gap / 2), Size(cw - gap, ch - gap))
        }
    }
}

@Composable
private fun PowerPanel(g: GameController, s: GameState) {
    Panel {
        SectionTitle("02", "POWER BUDGET", "${s.drawn()} / $POWER_BUDGET UNITS DRAWN", if (s.drawn() >= POWER_BUDGET) C.warn else C.muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(108.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.BottomCenter) {
                    Dial((s.reserve / 100).toFloat(), Modifier.size(104.dp, 58.dp), big = true)
                }
                Box(Modifier.padding(top = 4.dp).background(Color(0xFF2A1A0F), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("RESERVE ${s.reserve.roundToInt().toString().padStart(2, '0')}", style = label(10.sp, C.cream, 0.14f))
                }
                Text("SURPLUS RECHARGES", style = label(7.sp, C.muted.copy(alpha = 0.7f), 0.1f), modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Channel.entries.forEach { ch ->
                    val v = s.power[ch] ?: 0
                    val low = v < ch.need
                    val sub = if (low) "UNDER-POWERED · NEEDS ${ch.need}" else when (ch) {
                        Channel.LIFE -> "Crew air and heat"
                        Channel.SCAN -> if (s.h(Sys.SCAN) < 50) "Half-blind, sulking" else "Long-range eyes"
                        Channel.SHIELD -> "Forward emitters"
                        Channel.ENGINE -> "Coast and correct"
                        Channel.REPAIR -> "Backlog: ${Sys.entries.sumOf { ((100 - s.h(it)) / 6).roundToInt() }} jobs"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.width(78.dp)) {
                            Text(ch.label.uppercase(), style = label(10.sp, C.cream, 0.1f), maxLines = 1)
                            Text(sub, style = body(8.5.sp, if (low) C.bad else C.muted), maxLines = 1)
                        }
                        PowerSlider(v, ch == Channel.LIFE || ch == Channel.SHIELD, Modifier.weight(1f).height(26.dp)) { g.setPower(ch, it) }
                        Spacer(Modifier.width(4.dp))
                        NixBox(v.toString().padStart(2, '0'), low)
                    }
                }
            }
        }
    }
}

/** Brass-knob slider with a teal or orange fill. Drag or tap; 0..100. */
@Composable
fun PowerSlider(value: Int, teal: Boolean, modifier: Modifier, onChange: (Int) -> Unit) {
    val cur by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    Canvas(modifier
        .pointerInput(Unit) { detectTapGestures { change(((it.x / size.width) * 100).roundToInt().coerceIn(0, 100)) } }
        .pointerInput(Unit) {
            var acc = 0f
            detectHorizontalDragGestures(onDragStart = { acc = cur.toFloat() }) { ch, dx ->
                ch.consume(); acc += dx / size.width * 100; change(acc.roundToInt().coerceIn(0, 100))
            }
        }) {
        val y = size.height / 2; val th = 6.dp.toPx()
        drawRoundRect(Color(0xFF0B0705), Offset(0f, y - th / 2 - 1), Size(size.width, th + 2), CornerRadius(th))
        val fx = size.width * cur / 100f
        val col = if (teal) listOf(Color(0xFF1F8A82), Color(0xFF5FE3CF)) else listOf(Color(0xFFB4461B), Color(0xFFF08A45))
        drawRoundRect(Brush.horizontalGradient(col), Offset(0f, y - th / 2), Size(fx.coerceAtLeast(th), th), CornerRadius(th))
        val kr = 9.dp.toPx(); val kx = fx.coerceIn(kr, size.width - kr)
        drawCircle(Color.Black.copy(alpha = 0.5f), kr, Offset(kx + 1.5f, y + 2f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF6E0), Color(0xFFD9C69C), Color(0xFF8A7350)), center = Offset(kx - 3, y - 3), radius = kr * 1.3f), kr, Offset(kx, y))
        for (i in 0 until 10) { val a = i / 10.0 * Math.PI * 2; drawLine(Color(0xFF6B5440), Offset(kx + (kotlin.math.cos(a) * kr * 0.55f).toFloat(), y + (kotlin.math.sin(a) * kr * 0.55f).toFloat()), Offset(kx + (kotlin.math.cos(a) * kr * 0.85f).toFloat(), y + (kotlin.math.sin(a) * kr * 0.85f).toFloat()), 1f) }
    }
}

@Composable
private fun HealthPanel(s: GameState) {
    val warnings = Sys.entries.count { s.h(it) < 50 }
    Panel {
        SectionTitle("03", "SYSTEM HEALTH", if (warnings > 0) "$warnings WARNING${if (warnings > 1) "S" else ""}" else "ALL NOMINAL", if (warnings > 0) C.warn else C.glow, if (warnings > 0) Color(0xFFFFC23A) else null)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Sys.entries.forEach { sys ->
                val v = s.h(sys)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Dial((v / 100).toFloat(), Modifier.size(50.dp, 30.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(sys.short, style = label(7.5.sp, if (v < 50) C.bad else C.cream, 0.06f), maxLines = 1, textAlign = TextAlign.Center)
                    Text("${v.roundToInt()}%", style = vfd(15.sp, healthColor(v)))
                }
            }
        }
    }
}

@Composable
private fun ConsumablesPanel(s: GameState) {
    Panel {
        SectionTitle("04", "CONSUMABLES", "EST. ${Engine.hopsLeft(s)} TRANSITS AT CURRENT DRAW")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(Triple("AIR", "O₂ reserve", s.air), Triple("FOOD", "Seed + rations", s.food), Triple("WATER", "Recycled", s.water), Triple("SPARES", "Parts bin", s.spares)).forEach { (n, sub, v) ->
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Tank((v / 100).toFloat(), if (n == "SPARES") C.mustard else if (v < 25) C.red else Color(0xFF4FC9BC), Modifier.size(16.dp, 42.dp))
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text("${v.roundToInt()}%", style = vfd(18.sp, if (v < 25) C.bad else if (n == "SPARES") C.warn else C.glow))
                        Text(n, style = label(9.sp, C.cream, 0.1f))
                        Text(sub, style = body(7.5.sp, C.muted), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AwakePanel(s: GameState) {
    Panel {
        SectionTitle("05", "AWAKE CREW", "${s.awake.size} AWAKE · TRUST ${if (s.trust >= 0) "+" else ""}${s.trust}", if (s.trust < 0) C.bad else C.glow)
        s.awake.forEach { id ->
            val sp = specialist(id)
            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Lamp(Color(0xFF3FE0B0), 6.dp); Spacer(Modifier.width(6.dp))
                Text(sp.name, style = body(12.sp, C.cream)); Spacer(Modifier.weight(1f)); Text(sp.role.uppercase(), style = label(9.sp, C.teal))
            }
        }
    }
}
