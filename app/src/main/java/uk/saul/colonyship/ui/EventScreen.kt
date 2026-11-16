package uk.saul.colonyship.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.*
import kotlin.math.roundToInt

fun eventHeader(s: GameState?): Pair<String, String> {
    val e = s?.log?.lastOrNull()?.let { Events.byId(it.eventId) }
    return if (e == null) "INCIDENT LOG" to "ALL QUIET" else "INCIDENT ${(s.log.size * 37 + 400).toString().padStart(4, '0')}" to e.vfd
}

@Composable
fun EventScreen(g: GameController, s: GameState, showPrompt: (PromptShown) -> Unit, showImage: (String?, Int, String) -> Unit) {
    val entry = s.log.lastOrNull()
    val ev = entry?.let { Events.byId(it.eventId) }
    if (entry == null || ev == null) {
        Panel {
            AiQuote("No incidents logged. Light the drive on the Ship screen; nothing happens to a ship sitting still.", "SHIP AI · INCIDENT LOG")
            Spacer(Modifier.height(10.dp))
            ConsoleKey("GO TO SHIP CONSOLE", Modifier.fillMaxWidth(), KeyStyle.ORANGE) { g.tab = Tab.SHIP }
        }
        return
    }
    val ctx = LocalContext.current
    val pick = rememberPhotoPicker { uri -> if (!g.attachToLog(s.runId, entry.id, uri)) Toast.makeText(ctx, "Couldn’t read that picture", Toast.LENGTH_SHORT).show() }
    val pending = s.phase == Phase.EVENT && s.currentEvent == ev.id
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        EventHero(ev, s, entry, pending)
        Panel(padding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
            Text(ev.kicker, style = label(10.sp, C.teal, 0.16f))
            Text(ev.title.uppercase(), style = TextStyle(fontFamily = F.righteous, fontSize = 27.sp, color = C.nix, lineHeight = 29.sp,
                shadow = Shadow(Color(0xFF5A2008), Offset(3f, 3f), 0f)), modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            AiQuote(if (pending || entry.outcome.isBlank()) ev.ai(s) else entry.outcome, if (pending) null else (if (entry.earthYear > 0) "OUTCOME · ${entry.earthYear} AD · ${"%.1f".format(entry.ly)} LY" else "OUTCOME · DAY ${entry.day}"), eye = 40.dp, size = 13)
            Spacer(Modifier.height(10.dp))
            PromptRow(
                onImage = { showPrompt(PromptShown("Image prompt", Prompts.eventImage(s, ev, entry))) },
                onVideo = { showPrompt(PromptShown("Video prompt", Prompts.eventVideo(s, ev, entry))) },
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConsoleKey(if (entry.imagePath == null) "+ ATTACH PICTURE" else "REPLACE PICTURE", Modifier.weight(1f), KeyStyle.DARK, height = 34.dp, textSize = 11) { pick() }
                if (entry.imagePath != null) {
                    Spacer(Modifier.width(8.dp))
                    val bmp = rememberFileBitmap(entry.imagePath)
                    Box(Modifier.size(54.dp, 36.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, C.cream2, RoundedCornerShape(4.dp)).hapticClick { showImage(entry.imagePath, artRes(ev.art), ev.title) }) {
                        if (bmp != null) Image(bmp, "Attached picture", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
        }
        if (pending) {
            ev.available(s).forEachIndexed { i, c -> ChoiceCard(i, c) { g.choose(i) } }
        } else {
            if (entry.choice.isNotBlank()) {
                Panel {
                    SectionTitle("✓", "YOU CHOSE · ${entry.choice.uppercase()}")
                    if (s.lastDeltas.isNotEmpty() && s.phase == Phase.OUTCOME) FlowChips(s.lastDeltas.map { it to (it.contains("−") && !it.contains("POWER") || it == "BROWNOUT" || it.contains("FAILING")) })
                }
            }
            when (s.phase) {
                Phase.OUTCOME -> ConsoleKey("BRAKE INTO THE NEXT SYSTEM", Modifier.fillMaxWidth(), KeyStyle.TEAL, height = 50.dp, textSize = 16, sub = "DECELERATE · RUN FIRST SURVEY PASS", cueType = Cue.SELECT) { g.arrive() }
                Phase.ORBIT -> ConsoleKey("VIEW PLANET SURVEY", Modifier.fillMaxWidth(), KeyStyle.ON, height = 44.dp) { g.tab = Tab.SCAN }
                Phase.CRUISE -> ConsoleKey("BACK TO SHIP CONSOLE", Modifier.fillMaxWidth(), KeyStyle.DARK) { g.tab = Tab.SHIP }
                else -> {}
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun EventHero(ev: EventDef, s: GameState, entry: LogEntry, pending: Boolean) {
    val t = rememberInfiniteTransition(label = "hero")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    val flick by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "f")
    Panel(padding = PaddingValues(6.dp), rivets = false) {
        CrtBox(Modifier.fillMaxWidth().aspectRatio(0.98f)) {
            Image(painterResource(artRes(ev.art)), null, Modifier.fillMaxSize().graphicsLayer {
                scaleX = 1.12f; scaleY = 1.12f; translationX = (drift - 0.5f) * 24f
                if (pending && flick > 0.94f) translationX += 5f
            }, contentScale = ContentScale.Crop)
            val crisis = ev.id.startsWith("crisis_")
            Box(Modifier.fillMaxWidth().height(if (crisis) 32.dp else 24.dp).hazard(), contentAlignment = Alignment.Center) {
                Box(Modifier.background(if (crisis) Color(0xFF4A0E0C) else Color(0xFF160D08), RoundedCornerShape(3.dp)).border(1.dp, if (crisis) Color(0xFFFF4A32) else C.warn, RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 1.dp)) {
                    Text(if (crisis) "CATASTROPHIC · ${ev.hazard}" else ev.hazard, style = label(10.sp, if (crisis) Color(0xFFFF8A6A) else C.warn, 0.16f), maxLines = 1)
                }
            }
            Row(Modifier.padding(start = 10.dp, end = 10.dp, top = 30.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val hh = ((entry.day * 7 + 3) % 24); val mm = (entry.day * 13) % 60
                Text("FWD OBSERVATION · ${"%02d:%02d".format(hh, mm)} SHIP TIME", style = vfd(11.sp))
                Spacer(Modifier.weight(1f)); Lamp(Color(0xFFFF3B1F), 6.dp); Spacer(Modifier.width(4.dp)); Text("REC", style = label(9.sp, Color(0xFFFF9A7A)))
            }
            if (ev.art in setOf("storm", "derelict", "flare")) TargetBoxes(ev.id.hashCode(), ev.art)
            Row(Modifier.align(Alignment.BottomCenter).padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Meter(ev.metricLabel, ev.metric(s), Modifier.weight(1f), C.bad, Color(0xB3FF5032))
                Meter("SHIELDS", "${s.h(Sys.SHIELD).roundToInt()}%", Modifier.weight(1f), if (s.h(Sys.SHIELD) < 70) C.warn else C.glow, Color(0x99FFBE3C))
                Meter("HULL", "${s.h(Sys.HULL).roundToInt()}%", Modifier.weight(1f), if (s.h(Sys.HULL) < 70) C.warn else C.glow, Color(0x99FFBE3C))
                Meter("ALIVE", "${s.alive()}", Modifier.weight(1f), C.glow, Color(0x737FF5DC))
            }
        }
    }
}

@Composable
private fun BoxScope.TargetBoxes(seed: Int, art: String = "storm") {
    val tags = when (art) {
        "storm" -> listOf("IMPACT · RING SEC ${2 + (seed and 0xF) % 6}", "BREACH · FWD HULL")
        "flare" -> listOf("FLARE FRONT", "SHIELD LOAD 140%")
        "derelict" -> listOf("ORIGIN · UNKNOWN", "ENERGY TRACE")
        "cryobay" -> listOf("POD BANK 7", "THERMAL DRIFT")
        "engineering" -> listOf("CORE TEMP", "COOLANT LOOP")
        "planet", "giant" -> listOf("GRAVITY WELL", "TRACKING")
        else -> listOf("TRACKING", "ANOMALY")
    }
    val r = kotlin.random.Random(seed)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val h = maxHeight
        repeat(2) { i ->
            val x = 0.2f + r.nextFloat() * 0.5f; val y = 0.25f + i * 0.25f + r.nextFloat() * 0.1f
            Box(Modifier.offset(w * x, h * y).size(46.dp, 42.dp).border(1.5.dp, Color(0xE6FF5A32)))
            Box(Modifier.offset(w * x, h * y + 44.dp).background(Color(0xCC140A06), RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text(tags[i], style = label(8.sp, Color(0xFFFFB27A), 0.1f))
            }
        }
    }
}

@Composable
private fun Meter(k: String, v: String, modifier: Modifier, vc: Color, border: Color) {
    Column(modifier.background(Color(0xD10A0806), RoundedCornerShape(5.dp)).border(1.dp, border, RoundedCornerShape(5.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
        Text(k, style = label(7.5.sp, Color(0xFFB9A582), 0.12f), maxLines = 1)
        Text(v, style = vfd(19.sp, vc), maxLines = 1)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChoiceCard(i: Int, c: ChoiceDef, onClick: () -> Unit) {
    val letter = "ABC"[i].toString()
    val (bg, btn, title, desc, chipBg, chipFg, glow) = when (i) {
        0 -> ChoiceColors(listOf(Color(0xFF3BB5AC), Color(0xFF17605C)), listOf(Color(0xFFD8FFF6), Color(0xFF7FF5DC), Color(0xFF1F8A82)), Color(0xFFF2FFF9), Color(0xFFE3FBF5), Color(0x59000000), Color(0xFFC9FFF3), Color(0x592FA39C))
        1 -> ChoiceColors(listOf(Color(0xFF43352A), Color(0xFF221913)), listOf(Color(0xFFFFF6E0), Color(0xFFD9C69C), Color(0xFF8A7350)), C.cream, Color(0xFFCDBB98), Color(0x1AFFEBC8), Color(0xFFE9D9B6), Color.Transparent)
        else -> ChoiceColors(listOf(Color(0xFFF08A45), Color(0xFFB4461B)), listOf(Color(0xFFFFF0D0), Color(0xFFFFC27A), Color(0xFFC4501C)), Color(0xFF1F0B03), Color(0xFF2E1206), Color(0x4D280C02), Color(0xFFFFF0DC), Color(0x59E8692C))
    }
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxWidth().shadow(if (glow != Color.Transparent) 10.dp else 4.dp, shape, ambientColor = glow, spotColor = glow)
        .clip(shape).background(Brush.verticalGradient(bg)).grain(0.5f)
        .hapticClick(Cue.SELECT) { onClick() }.padding(horizontal = 10.dp, vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).shadow(4.dp, CircleShape).border(2.dp, Color(0xFF8C7556), CircleShape).clip(CircleShape).background(Brush.radialGradient(btn, center = Offset(30f, 26f))), contentAlignment = Alignment.Center) {
                Text(letter, style = TextStyle(fontFamily = F.righteous, fontSize = 22.sp, color = if (i == 1) C.ink else Color(0xFF06302D).takeIf { i == 0 } ?: Color(0xFF4A1606)))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title.uppercase(), style = label(16.sp, title, 0.08f))
                Text(c.desc, style = body(12.sp, desc), modifier = Modifier.padding(top = 2.dp, bottom = 5.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    c.chips.forEach { ch -> Chip(ch.removePrefix("!"), ch.startsWith("!"), chipBg, chipFg) }
                }
            }
            Spacer(Modifier.width(6.dp))
            RiskMeter(c.risk)
        }
    }
}

private data class ChoiceColors(val a: List<Color>, val b: List<Color>, val c: Color, val d: Color, val e: Color, val f: Color, val g: Color)

@Composable fun PicIcon(c: Color) = Canvas(Modifier.size(14.dp)) {
    drawRoundRect(c, style = Stroke(2f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
    drawCircle(c, size.width * 0.12f, Offset(size.width * 0.35f, size.height * 0.38f))
    drawLine(c, Offset(2f, size.height * 0.85f), Offset(size.width * 0.45f, size.height * 0.5f), 2f)
    drawLine(c, Offset(size.width * 0.45f, size.height * 0.5f), Offset(size.width - 2f, size.height * 0.8f), 2f)
}
@Composable fun CamIcon(c: Color) = Canvas(Modifier.size(16.dp, 14.dp)) {
    drawRoundRect(c, Offset(0f, size.height * 0.2f), androidx.compose.ui.geometry.Size(size.width * 0.65f, size.height * 0.6f), style = Stroke(2f))
    drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(size.width * 0.7f, size.height * 0.5f); lineTo(size.width, size.height * 0.25f); lineTo(size.width, size.height * 0.75f); close() }, c)
}
