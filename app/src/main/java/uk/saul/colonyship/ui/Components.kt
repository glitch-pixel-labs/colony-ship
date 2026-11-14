package uk.saul.colonyship.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.R
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.audio.Cues
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalCues = staticCompositionLocalOf<Cues?> { null }

@Composable
fun cue(): (Cue) -> Unit {
    val c = LocalCues.current
    val view = LocalView.current
    return { c?.play(it, view) }
}

/**
 * The one clickable path used everywhere in the UI. Plays a haptic (and the matching
 * sound) then runs [onClick], so a new control cannot forget the buzz.
 * [ripple] defaults to the theme indication; console keys pass false to keep the bakelite press.
 */
fun Modifier.hapticClick(
    cue: Cue = Cue.BUTTON,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    interactionSource: MutableInteractionSource? = null,
    ripple: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val cues = LocalCues.current
    val view = LocalView.current
    val src = interactionSource ?: remember { MutableInteractionSource() }
    val indication = if (ripple) LocalIndication.current else null
    clickable(src, indication, enabled, role = role) {
        cues?.play(cue, view)
        onClick()
    }
}

/** Dark brushed-metal panel with rivets, matching the mock-up. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp), rivets: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, shape)
            .border(1.dp, Color(0xFF0E0906), shape)
            .padding(1.dp)
            .border(1.dp, Color(0xFF4A3828), shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3D3026), Color(0xFF2A2019), Color(0xFF1E1712))))
            .grain(0.9f)
            .drawBehind {
                drawLine(Color(0x29FFEBC8), Offset(8f, 1.5f), Offset(size.width - 8f, 1.5f), 2f)
                if (rivets) {
                    val r = 3.dp.toPx(); val m = 7.dp.toPx()
                    listOf(Offset(m, m), Offset(size.width - m, m), Offset(m, size.height - m), Offset(size.width - m, size.height - m)).forEach { rivet(it, r) }
                }
            }
            .padding(padding),
        content = content,
    )
}

fun DrawScope.rivet(c: Offset, r: Float) {
    drawCircle(Color.Black.copy(alpha = 0.6f), r * 1.15f, c + Offset(0.5f, 0.8f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF3E3C0), Color(0xFF9B8364), Color(0xFF3B2C1F)), center = c - Offset(r * 0.3f, r * 0.3f), radius = r * 1.3f), r, c)
}

/** Numbered section header like "01 CRYOPOD BAY ........ BAY TEMP". */
@Composable
fun SectionTitle(num: String, title: String, right: String? = null, rightColor: Color = C.muted, lamp: Color? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.background(C.mustard, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
            Text(num, style = label(9.sp, C.ink, 0.05f))
        }
        Spacer(Modifier.width(6.dp))
        Text(title, style = label(13.sp, C.nix, 0.16f))
        Spacer(Modifier.weight(1f))
        if (lamp != null) { Lamp(lamp, 7.dp); Spacer(Modifier.width(4.dp)) }
        if (right != null) Text(right, style = label(10.sp, rightColor, 0.12f), maxLines = 1)
    }
}

@Composable
fun Lamp(color: Color?, size: Dp = 7.dp) {
    val t = rememberInfiniteTransition(label = "lamp")
    val a by t.animateFloat(0.75f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Canvas(Modifier.size(size)) {
        if (color == null) {
            drawCircle(Color(0xFF2A1D12), size.toPx() / 2 + 1)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF777766), Color(0xFF2B2620)), center = center - Offset(1f, 1f)), size.toPx() / 2)
        } else {
            drawCircle(color.copy(alpha = 0.35f * a), size.toPx())
            drawCircle(Color(0xFF2A1D12), size.toPx() / 2 + 1)
            drawCircle(Brush.radialGradient(listOf(Color.White, color, Color(0xFF0A0A0A)), center = center - Offset(1.5f, 1.5f), radius = size.toPx() * 0.8f), size.toPx() / 2, alpha = a)
        }
    }
}

enum class KeyStyle { CREAM, DARK, ON, TEAL, ORANGE }

/** Chunky bakelite console key with press travel, sound and haptics. */
@Composable
fun ConsoleKey(
    text: String, modifier: Modifier = Modifier, style: KeyStyle = KeyStyle.DARK, height: Dp = 40.dp, sub: String? = null,
    enabled: Boolean = true, textSize: Int = 13, icon: (@Composable () -> Unit)? = null, cueType: Cue = Cue.BUTTON, onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val (top, bottom, fg, edge) = when (style) {
        KeyStyle.CREAM -> listOf(Color(0xFFF7ECD4), Color(0xFFD6C399), C.ink, Color(0xFF4B3826))
        KeyStyle.DARK -> listOf(Color(0xFF3E3027), Color(0xFF221913), C.cream, Color.Black)
        KeyStyle.ON -> listOf(Color(0xFFFFB066), Color(0xFFE06A2A), Color(0xFF1C0F07), Color(0xFF4A1C08))
        KeyStyle.TEAL -> listOf(Color(0xFF3BB5AC), Color(0xFF17605C), Color(0xFFF2FFF9), Color(0xFF061E1C))
        KeyStyle.ORANGE -> listOf(Color(0xFFF08A45), Color(0xFFB4461B), Color(0xFF1F0B03), Color(0xFF3A1404))
    }
    val shape = RoundedCornerShape(8.dp)
    val travel = if (pressed) 2.dp else 0.dp
    Box(
        modifier
            .height(height + 3.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .hapticClick(cueType, enabled = enabled, role = Role.Button, interactionSource = src, ripple = false) { onClick() },
    ) {
        Box(Modifier.fillMaxWidth().height(height).offset(y = 3.dp).background(edge, shape))
        Box(
            Modifier.fillMaxWidth().height(height).offset(y = travel)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(top, bottom)))
                .grain(0.6f)
                .drawBehind {
                    drawLine(Color.White.copy(alpha = 0.35f), Offset(6f, 1.5f), Offset(size.width - 6f, 1.5f), 2f)
                    drawLine(Color.Black.copy(alpha = 0.3f), Offset(6f, size.height - 2f), Offset(size.width - 6f, size.height - 2f), 3f)
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) { icon(); Spacer(Modifier.width(6.dp)) }
                    Text(text.uppercase(), style = label(textSize.sp, fg, 0.14f), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
                if (sub != null) Text(sub.uppercase(), style = label(9.sp, fg.copy(alpha = 0.8f), 0.1f), maxLines = 1)
            }
        }
    }
}


/** Circular AI eye cropped from the painted art. */
@Composable
fun AiEye(size: Dp = 40.dp, glitch: Boolean = false) {
    val t = rememberInfiniteTransition(label = "eye")
    val g by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "g")
    Box(Modifier.size(size).shadow(4.dp, CircleShape).border(2.dp, Color(0xFFB09670), CircleShape).padding(2.dp).border(1.dp, Color(0xFF4D3A2A), CircleShape).clip(CircleShape)) {
        Image(painterResource(R.drawable.art_ai_eye), null, Modifier.fillMaxSize().graphicsLayer {
            scaleX = 1.18f; scaleY = 1.18f
            if (glitch && g > 0.92f) translationX = 3f
        }, contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
    }
}

/** Highlights *starred* words in the AI quote in amber, like the mock-up. */
fun quoteText(text: String) = buildAnnotatedString {
    val parts = text.split("*")
    parts.forEachIndexed { i, p ->
        if (i % 2 == 1) withStyle(SpanStyle(color = C.nix, fontWeight = FontWeight.SemiBold, fontStyle = androidx.compose.ui.text.font.FontStyle.Normal)) { append(p) } else append(p)
    }
}

@Composable
fun AiQuote(text: String, who: String? = null, eye: Dp = 44.dp, size: Int = 14, glitch: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AiEye(eye, glitch)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (who != null) Text(who, style = label(9.sp, C.teal, 0.2f), modifier = Modifier.padding(bottom = 3.dp))
            Text(quoteText("“$text”"), style = quote(size.sp))
        }
    }
}

/** Dark CRT viewport with scanlines. */
@Composable
fun CrtBox(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.clip(shape).background(Color(0xFF05080A)).scanlines(0.12f).border(1.dp, Color(0xFF4A3828), shape), content = content)
}

@Composable
fun Chip(text: String, bad: Boolean = false, bg: Color = Color(0x1AFFEBC8), fg: Color = Color(0xFFE9D9B6)) {
    Box(Modifier.background(if (bad) Color(0xFF5A1406) else bg, RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(text, style = label(9.sp, if (bad) Color(0xFFFFB4A0) else fg, 0.1f), maxLines = 1)
    }
}

@Composable
fun RiskMeter(level: Int) {
    Column(
        Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 5 downTo 1) {
            val on = i <= level
            val hot = on && i >= 4
            Box(Modifier.size(15.dp, 5.dp).background(when { hot -> Color(0xFFFF4A2A); on -> Color(0xFFFFB13A); else -> Color.White.copy(alpha = 0.08f) }, RoundedCornerShape(1.5.dp)))
        }
        Text("RISK", style = label(7.sp, Color(0xBFFFF0DC), 0.1f))
    }
}

/** Glowing nixie-tube digits. */
@Composable
fun Nixies(value: String, digitW: Dp = 26.dp, digitH: Dp = 42.dp, fontSize: Int = 38) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        value.forEach { ch ->
            Box(
                Modifier.size(digitW, digitH).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF2A1A12), Color(0xFF120A06))))
                    .border(1.dp, Color(0xFF5A4030), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(ch.toString(), style = TextStyle(fontFamily = F.vt, fontSize = fontSize.sp, color = Color(0xFFFFB15A), shadow = Shadow(Color(0xFFFF6A00), Offset.Zero, 22f)))
            }
        }
    }
}

/** Small boxed value like the nixie readouts beside the sliders. */
@Composable
fun NixBox(text: String, warn: Boolean = false) {
    Box(Modifier.width(30.dp).height(22.dp).background(Color(0xFF140B06), RoundedCornerShape(3.dp)).border(1.dp, Color(0xFF5A4030), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
        Text(text, style = vfd(19.sp, if (warn) Color(0xFFFF6A4A) else Color(0xFFFFB15A)))
    }
}

/** Semicircular needle dial. */
@Composable
fun Dial(value: Float, modifier: Modifier = Modifier, big: Boolean = false) {
    val anim by animateFloatAsState(value.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "dial")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val r = minOf(w / 2f, h) * 0.92f
        val c = Offset(w / 2, h * 0.95f)
        // bezel
        drawArc(Brush.verticalGradient(listOf(Color(0xFFD9C69C), Color(0xFF6B5440))), 180f, 180f, true, c - Offset(r + 4, r + 4), Size((r + 4) * 2, (r + 4) * 2))
        drawArc(Brush.radialGradient(listOf(Color(0xFFF6EBD2), Color(0xFFD8C7A0)), center = c, radius = r), 180f, 180f, true, c - Offset(r, r), Size(r * 2, r * 2))
        val band = r * 0.16f
        val inner = r - band * 0.9f
        fun arc(from: Float, to: Float, col: Color) = drawArc(col, 180f + from * 180f, (to - from) * 180f, false, c - Offset(inner, inner), Size(inner * 2, inner * 2), style = Stroke(band))
        arc(0f, 0.25f, C.red); arc(0.25f, 0.5f, C.mustard); arc(0.5f, 1f, C.teal)
        for (i in 0..10) {
            val a = PI + i / 10.0 * PI
            val o = Offset(c.x + (cos(a) * (r * 0.78f)).toFloat(), c.y + (sin(a) * (r * 0.78f)).toFloat())
            val o2 = Offset(c.x + (cos(a) * (r * (if (i % 5 == 0) 0.6f else 0.68f))).toFloat(), c.y + (sin(a) * (r * (if (i % 5 == 0) 0.6f else 0.68f))).toFloat())
            drawLine(C.ink, o, o2, if (i % 5 == 0) 2.2f else 1.2f)
        }
        val a = PI + anim * PI
        val tip = Offset(c.x + (cos(a) * r * 0.82f).toFloat(), c.y + (sin(a) * r * 0.82f).toFloat())
        drawLine(Color.Black.copy(alpha = 0.3f), c + Offset(2f, 2f), tip + Offset(2f, 2f), if (big) 4f else 2.5f)
        drawLine(Color(0xFFB4261B), c, tip, if (big) 4f else 2.5f, StrokeCap.Round)
        drawCircle(Color(0xFF2A1A0F), r * 0.1f, c)
    }
}

/** Vertical glass tank for consumables. */
@Composable
fun Tank(value: Float, color: Color, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(value.coerceIn(0f, 1f), tween(900), label = "tank")
    Canvas(modifier) {
        val cr = CornerRadius(size.width / 2.2f)
        drawRoundRect(Color(0xFF8C7556), cornerRadius = cr)
        drawRoundRect(Color(0xFF0B0907), Offset(2f, 2f), Size(size.width - 4, size.height - 4), cr)
        val fillH = (size.height - 8) * anim
        drawRoundRect(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color, color.copy(alpha = 0.6f))), Offset(4f, size.height - 4 - fillH), Size(size.width - 8, fillH), CornerRadius(size.width / 3))
        drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(size.width * 0.22f, 6f), Size(size.width * 0.14f, size.height - 12), CornerRadius(4f))
        for (i in 1..3) drawLine(Color.Black.copy(alpha = 0.4f), Offset(size.width * 0.6f, size.height * i / 4), Offset(size.width - 4, size.height * i / 4), 1.2f)
    }
}

/** Hazard stripes band. */
fun Modifier.hazard() = this.drawBehind {
    val step = 12.dp.toPx()
    clipRectSafe {
        drawRect(Color(0xFF1A110A))
        var x = -size.height
        while (x < size.width + size.height) {
            val p = Path().apply { moveTo(x, size.height); lineTo(x + step / 2, size.height); lineTo(x + step / 2 + size.height, 0f); lineTo(x + size.height, 0f); close() }
            drawPath(p, C.mustard)
            x += step
        }
    }
}
private inline fun DrawScope.clipRectSafe(block: DrawScope.() -> Unit) { drawContext.canvas.save(); drawContext.canvas.clipRect(0f, 0f, size.width, size.height); block(); drawContext.canvas.restore() }

/** The cream bakelite header plate with stripes, emblem and VFD display. */
@Composable
fun Header(line1: String, line2: String, lamps: List<Color?>, onMenu: (() -> Unit)?) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().height(58.dp).shadow(8.dp, shape).border(1.5.dp, Color(0xFF1A110B), shape).padding(1.5.dp).border(1.dp, Color(0xFF6B5440), shape)
            .clip(shape).background(Brush.verticalGradient(listOf(Color(0xFFF6EAD0), Color(0xFFE7D6B0), Color(0xFFCDB88D)))).grain(0.5f)
    ) {
        // retro stripes
        Canvas(Modifier.fillMaxSize()) {
            val sw = 8.5.dp.toPx(); val right = size.width - 128.dp.toPx()
            listOf(C.mustard, C.orange, C.burnt, C.brown).forEachIndexed { i, col ->
                val x0 = right - (4 - i) * sw
                val skew = size.height * 0.53f
                drawPath(Path().apply { moveTo(x0, size.height); lineTo(x0 + sw, size.height); lineTo(x0 + sw + skew, 0f); lineTo(x0 + skew, 0f); close() }, col)
            }
        }
        Row(Modifier.fillMaxHeight().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).hapticClick(enabled = onMenu != null) { onMenu?.invoke() }) {
                Emblem(Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(7.dp))
            Column {
                Text("COLONY SHIP", style = TextStyle(fontFamily = F.righteous, fontSize = 23.sp, color = Color(0xFF2A1A0F), shadow = Shadow(Color.White.copy(alpha = 0.6f), Offset(0f, 1f), 0f)), maxLines = 1)
                Text(if (onMenu != null) "SYSTEMS CONSOLE · MK IV · ☰ MENU" else "SYSTEMS CONSOLE · MK IV", style = label(7.5.sp, Color(0xFF7A4A26), 0.25f), maxLines = 1)
            }
        }
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 9.dp).size(112.dp, 40.dp).clip(RoundedCornerShape(5.dp))
                .border(1.5.dp, Color(0xFF8C7556), RoundedCornerShape(5.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF071311), Color(0xFF0C1F1C)))).padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Row(Modifier.align(Alignment.TopEnd).padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) { lamps.forEach { Lamp(it, 5.dp) } }
            Column {
                Text(line1, style = vfd(10.sp).copy(color = C.glow.copy(alpha = 0.75f), letterSpacing = 1.sp), maxLines = 1, modifier = Modifier.padding(end = 22.dp))
                Text(line2, style = vfd(19.sp), maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }
}

@Composable
fun Emblem(modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension; val c = center
        drawCircle(Color(0xFF4D3A2A), s / 2)
        drawCircle(Color(0xFFB09670), s / 2 - 1.5f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF4A3322), Color(0xFF1C120B)), center = c - Offset(s * 0.12f, s * 0.15f), radius = s * 0.6f), s / 2 - 3.5f)
        drawCircle(C.teal, s * 0.42f, c, style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 3f))))
        drawOval(Brush.horizontalGradient(listOf(Color(0xFFFFCF6A), C.orange, C.burnt)), Offset(c.x - s * 0.16f, c.y - s * 0.32f), Size(s * 0.32f, s * 0.64f), style = Stroke(s * 0.065f))
        drawRoundRect(Color(0xFFEFE2C4), Offset(c.x - s * 0.36f, c.y - s * 0.04f), Size(s * 0.72f, s * 0.08f), CornerRadius(s * 0.04f))
        drawCircle(C.glow, s * 0.05f, Offset(c.x + s * 0.36f, c.y))
        drawPath(Path().apply { moveTo(c.x - s * 0.42f, c.y); lineTo(c.x - s * 0.34f, c.y - s * 0.06f); lineTo(c.x - s * 0.34f, c.y + s * 0.06f); close() }, C.teal)
    }
}

enum class Tab(val label: String) { SHIP("SHIP"), EVENTS("EVENTS"), SCAN("SCAN"), ARCHIVE("ARCHIVE") }

@Composable
fun BottomNav(tab: Tab, onTab: (Tab) -> Unit, alerts: Set<Tab>) {
    Row(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF1A110B), Color(0xFF0E0906)))).drawBehind {
            drawLine(Color(0xFF4A3828), Offset(0f, 0f), Offset(size.width, 0f), 2f)
        }.navigationBarsPadding().padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Tab.entries.forEach { t ->
            val on = t == tab
            ConsoleKey("", Modifier.weight(1f), if (on) KeyStyle.ON else KeyStyle.DARK, height = 40.dp, cueType = Cue.BUTTON, onClick = { onTab(t) }, icon = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Lamp(when { on -> Color(0xFFFF8A2A); t in alerts -> Color(0xFFFF3B1F); else -> null }, 5.dp)
                    Spacer(Modifier.height(3.dp))
                    Text(t.label, style = label(11.sp, if (on) Color(0xFF1C0F07) else C.cream, 0.14f))
                }
            })
        }
    }
}

/** The whole-screen backdrop: warm gradient, grain and scanlines. */
@Composable
fun Backdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF231710), Color(0xFF120B07), Color(0xFF1A110B))))
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(Color(0x29E8692C), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.width * 1.1f))
                drawRect(Brush.radialGradient(listOf(Color(0x1A2FA39C), Color.Transparent), center = Offset(size.width / 2, size.height), radius = size.width))
            }
            .grain(0.7f),
        content = content,
    )
}

@Composable
fun SmallCaps(text: String, color: Color = C.muted, size: Int = 10, modifier: Modifier = Modifier) = Text(text, style = label(size.sp, color, 0.14f), modifier = modifier)
