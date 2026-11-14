package uk.saul.colonyship.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.GameState
import uk.saul.colonyship.game.Physics
import kotlin.math.*
import kotlin.random.Random

private const val TRANSIT_MS = 4800

/** Smoothstep. */
private fun ss(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3 - 2 * t) }

/** Velocity profile for the animation: boost, coast, brake. Returns fraction of peak β. */
fun transitProfile(p: Float): Float = when {
    p < 0.30f -> ss(p / 0.30f)
    p < 0.72f -> 1f
    else -> 1f - ss((p - 0.72f) / 0.28f)
}

private class Star(val theta: Double, val phi: Double, val temp: Float, val mag: Float)
private class Spark(val angle: Float, val seed: Float, val speed: Float)

/**
 * Cinematic relativistic transit. Stars are drawn with special-relativistic aberration
 * (cos θ' = (cos θ + β)/(1 + β cos θ)) and Doppler shift (D = 1/(γ(1 − β cos θ'))), so the sky bunches
 * forward and turns blue ahead, red and dim behind, as the ship approaches its cruise speed.
 */
@Composable
fun TransitOverlay(s: GameState, fixedProgress: Float? = null, onDone: () -> Unit) {
    val progress = remember { Animatable(fixedProgress ?: 0f) }
    val play = cue()
    var finished by remember { mutableStateOf(false) }
    fun done() { if (!finished) { finished = true; onDone() } }
    if (fixedProgress == null) {
        LaunchedEffect(Unit) { progress.animateTo(1f, tween(TRANSIT_MS, easing = LinearEasing)); delay(120); done() }
        // Haptic ticks as the drive phases change.
        LaunchedEffect(Unit) { delay((TRANSIT_MS * 0.30f).toLong()); play(Cue.TICK); delay((TRANSIT_MS * 0.42f).toLong()); play(Cue.BUTTON) }
    }
    val stars = remember {
        val r = Random(2187)
        List(900) {
            // Uniform on the sphere; θ measured from the direction of travel.
            val ct = r.nextDouble(-1.0, 1.0)
            Star(acos(ct), r.nextDouble(0.0, 2 * PI), r.nextFloat().pow(1.6f) * 0.85f, r.nextFloat().pow(3f))
        }
    }
    val sparks = remember { val r = Random(9); List(110) { Spark(r.nextFloat() * 6.2832f, r.nextFloat(), 0.6f + r.nextFloat() * 0.8f) } }
    val peak = s.lastBeta.coerceIn(0.2, 0.95)
    val p = progress.value
    val beta = (peak * transitProfile(p))
    val gamma = Physics.gamma(beta)
    val phase = when { p < 0.30f -> "BOOST · 0.5 g"; p < 0.72f -> "COAST"; else -> "BRAKING · 0.5 g" }
    val shipDays = s.prevDay + (s.day - s.prevDay) * p
    val earthDays = s.prevEarthDays + (s.earthDays - s.prevEarthDays) * p
    val earthYear = Physics.LAUNCH_YEAR + floor(earthDays / Physics.DAYS_PER_YEAR).toInt()
    val shipYr = shipDays / Physics.DAYS_PER_YEAR

    Box(Modifier.fillMaxSize().background(Color(0xFF020104)).pointerInput(Unit) {
        detectTapGestures { if (fixedProgress == null && progress.value > 0.25f) { play(Cue.BUTTON); done() } }
    }) {
        Canvas(Modifier.fillMaxSize()) {
            drawSky(stars, sparks, beta, peak, p)
            drawShip(beta / peak, p >= 0.72f, p)
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xCC000000)), center = Offset(size.width / 2, size.height * 0.42f), radius = size.maxDimension * 0.72f))
        }
        Column(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("RELATIVISTIC TRANSIT", style = vfd(26.sp))
            Text(phase, style = label(11.sp, if (p >= 0.72f) C.nix else C.cream, 0.3f))
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 14.dp, vertical = 20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudCell("VELOCITY", "%.3f c".format(beta), Modifier.weight(1f))
                HudCell("LORENTZ γ", "%.3f".format(gamma), Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudCell("SHIP CLOCK", "%.2f YRS".format(shipYr), Modifier.weight(1f))
                HudCell("EARTH DATE", "$earthYear AD", Modifier.weight(1f), C.nix)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(p.coerceIn(0.001f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(C.teal, C.glow)), RoundedCornerShape(2.dp)))
            }
            Spacer(Modifier.height(5.dp))
            Text("THE SLEEPERS AGE ${"%.1f".format((s.day - s.prevDay) / Physics.DAYS_PER_YEAR)} YEARS · EARTH AGES ${"%.1f".format((s.earthDays - s.prevEarthDays) / Physics.DAYS_PER_YEAR)}",
                style = label(9.sp, C.muted, 0.14f))
        }
    }
}

@Composable
private fun HudCell(k: String, v: String, modifier: Modifier, col: Color = C.glow) {
    Column(modifier.background(Color(0xB0061210), RoundedCornerShape(5.dp)).border(1.dp, Color(0x6640C8B0), RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(k, style = label(8.sp, C.teal, 0.16f))
        Text(v, style = vfd(22.sp, col), maxLines = 1)
    }
}

/** Colour for a star of base temperature t (0 red .. 1 blue), shifted by Doppler factor d. */
private fun starColour(t: Float, d: Double): Color {
    val x = (t + 0.55f * log2(d.coerceIn(0.05, 20.0)).toFloat()).coerceIn(-0.6f, 1.6f)
    return when {
        x < 0f -> lerp(Color(0xFF7A1E10), Color(0xFFFF8A4A), (x + 0.6f) / 0.6f)
        x < 0.5f -> lerp(Color(0xFFFF8A4A), Color(0xFFFFF4E4), x / 0.5f)
        x < 1.0f -> lerp(Color(0xFFFFF4E4), Color(0xFF9CC4FF), (x - 0.5f) / 0.5f)
        else -> lerp(Color(0xFF9CC4FF), Color(0xFF8A7CFF), (x - 1f) / 0.6f)
    }
}

private fun aberrate(theta: Double, beta: Double): Double {
    val c = cos(theta)
    return acos(((c + beta) / (1 + beta * c)).coerceIn(-1.0, 1.0))
}

private fun DrawScope.drawSky(stars: List<Star>, sparks: List<Spark>, beta: Double, peak: Double, p: Float) {
    val cx = size.width / 2; val cy = size.height * 0.42f
    val scale = size.maxDimension * 0.62f / (PI / 2).toFloat()
    val gamma = 1.0 / sqrt(1 - beta * beta)
    // Forward glow: the bunched, blueshifted sky ahead.
    val fwd = (beta / 0.9).toFloat().coerceIn(0f, 1f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF9CC4FF).copy(alpha = 0.32f * fwd), Color(0xFF3A3AA8).copy(alpha = 0.12f * fwd), Color.Transparent),
        center = Offset(cx, cy), radius = size.minDimension * (0.55f - 0.25f * fwd)), size.minDimension * 0.6f, Offset(cx, cy))
    // Trails: compare with the sky at a slightly different speed, in the direction the view is changing.
    val accel = p < 0.30f; val braking = p >= 0.72f
    val trailBeta = when {
        accel -> (beta - 0.10 - 0.10 * beta).coerceAtLeast(0.0)
        braking -> (beta + 0.10 + 0.10 * beta).coerceAtMost(0.97)
        else -> (beta - 0.04).coerceAtLeast(0.0)
    }
    for (st in stars) {
        val t1 = aberrate(st.theta, beta)
        if (t1 > 2.3) continue
        val t0 = aberrate(st.theta, trailBeta)
        val d = 1.0 / (gamma * (1 - beta * cos(t1)))
        val r1 = (t1 * scale).toFloat(); val r0 = (t0 * scale).toFloat()
        val ca = cos(st.phi).toFloat(); val sa = sin(st.phi).toFloat()
        val a = Offset(cx + ca * r1, cy + sa * r1)
        if (a.x < -20 || a.x > size.width + 20 || a.y < -20 || a.y > size.height + 20) continue
        val bright = ((0.25f + 0.75f * st.mag) * d.pow(1.4).toFloat()).coerceIn(0.05f, 1.6f)
        val col = starColour(st.temp, d)
        val w = (1.1f + 2.4f * st.mag) * bright.coerceAtMost(1.3f)
        val b = Offset(cx + ca * r0, cy + sa * r0)
        if (abs(r1 - r0) > 1.5f) {
            drawLine(Brush.linearGradient(listOf(col.copy(alpha = 0f), col.copy(alpha = (0.75f * bright).coerceAtMost(1f))), b, a), b, a, w * 0.8f, StrokeCap.Round)
        }
        drawCircle(col.copy(alpha = bright.coerceAtMost(1f)), w * 0.75f, a)
        if (st.mag > 0.55f && bright > 0.6f) drawCircle(col.copy(alpha = 0.18f * bright.coerceAtMost(1f)), w * 3.2f, a)
    }
    // Interstellar medium flaring against the shield: thin streaks racing outward, strongest at cruise.
    val k = (beta / peak).toFloat()
    if (k > 0.05f) for (sp in sparks) {
        val prog = ((sp.seed + p * 6f * sp.speed) % 1f)
        val r = prog.pow(2.2f) * size.maxDimension * 0.75f
        val len = (12f + 120f * k) * prog
        val dir = Offset(cos(sp.angle), sin(sp.angle))
        val from = Offset(cx, cy) + dir * r
        val to = Offset(cx, cy) + dir * (r + len)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color(0xFFBFE6FF).copy(alpha = 0.55f * k * prog)), from, to), from, to, 1f + 1.6f * prog, StrokeCap.Round)
    }
}

/** The ship from behind and above: spine, habitat ring, ice shield ahead, drive glow at the stern (or the bow when braking). */
private fun DrawScope.drawShip(k: Double, braking: Boolean, p: Float) {
    val w = size.width; val h = size.height
    val near = Offset(w * 0.5f, h * 0.80f); val far = Offset(w * 0.5f, h * 0.60f)
    val hull = Color(0xFF0B0A0E); val rim = Color(0xFF8FB8FF).copy(alpha = 0.35f + 0.4f * k.toFloat())
    // Spine (tapered quad)
    val spine = Path().apply {
        moveTo(near.x - w * 0.035f, near.y); lineTo(far.x - w * 0.012f, far.y); lineTo(far.x + w * 0.012f, far.y); lineTo(near.x + w * 0.035f, near.y); close()
    }
    // Drive glow at stern while boosting/coasting: the camera sits behind it.
    val glowAt = if (braking) far else near
    val glowR = (if (braking) w * 0.10f else w * 0.22f) * (0.6f + 0.4f * k.toFloat()) * (if (p in 0.30f..0.72f) 0.55f else 1f)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), Color(0xFFFFB46A).copy(alpha = 0.65f), Color(0xFFE8692C).copy(alpha = 0.2f), Color.Transparent), center = glowAt, radius = glowR), glowR, glowAt)
    // Ice shield at the bow
    val shieldW = w * 0.11f; val shieldH = w * 0.04f
    drawOval(Brush.verticalGradient(listOf(Color(0xFFCFE6FF).copy(alpha = 0.9f), Color(0xFF3C5A7A)), startY = far.y - shieldH, endY = far.y + shieldH),
        Offset(far.x - shieldW / 2, far.y - shieldH / 2 - h * 0.006f), Size(shieldW, shieldH))
    drawPath(spine, hull)
    drawPath(spine, rim, style = androidx.compose.ui.graphics.drawscope.Stroke(1.6f))
    // Habitat ring around the spine
    val ringC = Offset(w * 0.5f, h * 0.70f); val rw = w * 0.30f; val rh = w * 0.07f
    drawOval(hull, Offset(ringC.x - rw / 2, ringC.y - rh / 2), Size(rw, rh), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.016f))
    drawOval(rim, Offset(ringC.x - rw / 2, ringC.y - rh / 2 - 2f), Size(rw, rh), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f))
    // Ring windows
    for (i in 0 until 14) {
        val a = (i / 14f) * 2 * PI.toFloat() + p * 0.8f
        if (sin(a) < 0) continue
        drawCircle(Color(0xFFFFC27A).copy(alpha = 0.8f), 1.6f, Offset(ringC.x + cos(a) * rw / 2, ringC.y + sin(a) * rh / 2))
    }
    // Radiator fins
    val fin = Path().apply { moveTo(w * 0.47f, h * 0.76f); lineTo(w * 0.36f, h * 0.775f); lineTo(w * 0.36f, h * 0.785f); lineTo(w * 0.47f, h * 0.775f); close() }
    drawPath(fin, Color(0xFF3A1A10)); drawPath(fin, Color(0xFFE8692C).copy(alpha = 0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
    val fin2 = Path().apply { moveTo(w * 0.53f, h * 0.76f); lineTo(w * 0.64f, h * 0.775f); lineTo(w * 0.64f, h * 0.785f); lineTo(w * 0.53f, h * 0.775f); close() }
    drawPath(fin2, Color(0xFF3A1A10)); drawPath(fin2, Color(0xFFE8692C).copy(alpha = 0.35f), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
}
