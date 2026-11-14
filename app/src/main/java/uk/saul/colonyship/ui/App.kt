package uk.saul.colonyship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import uk.saul.colonyship.R
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.*
import kotlin.random.Random

const val VERSION = "0.1.0"

@Composable
fun ColonyApp(g: GameController) {
    CompositionLocalProvider(LocalCues provides g.cues) {
        Backdrop {
            when (g.screen) {
                Screen.INTRO -> IntroScreen(g)
                Screen.SETTINGS -> SettingsScreen(g)
                Screen.GAME -> GameShell(g)
            }
            if (g.jumping) JumpOverlay { g.finishJump() }
        }
    }
}

@Composable
fun GameShell(g: GameController) {
    var prompt by remember { mutableStateOf<PromptShown?>(null) }
    var image by remember { mutableStateOf<Triple<String?, Int, String>?>(null) }
    BackHandler(enabled = true) {
        when {
            g.openBook != null && g.tab == Tab.ARCHIVE -> g.openBook = null
            g.tab != Tab.SHIP -> g.tab = Tab.SHIP
            else -> g.screen = Screen.INTRO
        }
    }
    val s = g.state
    val (l1, l2) = when (g.tab) {
        Tab.SHIP -> "SYSTEM STATUS" to (s?.let { "DAY ${it.day}" } ?: "STANDBY")
        Tab.EVENTS -> eventHeader(s)
        Tab.SCAN -> scanHeader(s)
        Tab.ARCHIVE -> archiveHeader(g)
    }
    val stress = s?.let { Voice.stress(it) } ?: 0
    val lamps = when { stress >= 3 -> listOf(Color(0xFFFF3B1F), Color(0xFFFF3B1F), Color(0xFFFFC23A)); stress >= 1 -> listOf(Color(0xFF3FE0B0), Color(0xFFFF8A2A), Color(0xFFFFC23A)); else -> listOf(Color(0xFF3FE0B0), Color(0xFF3FE0B0), null) }
    Column(Modifier.fillMaxSize().scanlines(0.07f)) {
        Box(Modifier.statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Header(l1, l2, lamps) { g.settingsFrom = Screen.GAME; g.screen = Screen.SETTINGS }
        }
        key(g.tab, g.openBook) {
            val scroll = rememberScrollState()
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 10.dp).padding(bottom = 10.dp)) {
                if (s == null && g.tab != Tab.ARCHIVE) {
                    Panel {
                        AiQuote("No voyage in progress. One thousand sleepers are waiting for someone to press the big button.", "SHIP AI")
                        Spacer(Modifier.height(10.dp))
                        ConsoleKey("BEGIN A NEW VOYAGE", Modifier.fillMaxWidth(), KeyStyle.ORANGE) { g.newRun() }
                    }
                } else when (g.tab) {
                    Tab.SHIP -> ShipScreen(g, s!!)
                    Tab.EVENTS -> EventScreen(g, s!!, { prompt = it }, { a, b, c -> image = Triple(a, b, c) })
                    Tab.SCAN -> ScanScreen(g, s!!)
                    Tab.ARCHIVE -> ArchiveScreen(g, { prompt = it }, { a, b, c -> image = Triple(a, b, c) })
                }
            }
        }
        val alerts = buildSet {
            if (s?.phase == Phase.EVENT) add(Tab.EVENTS)
            if (s?.phase == Phase.ORBIT) add(Tab.SCAN)
            if (s?.phase == Phase.ENDED) add(Tab.ARCHIVE)
        }
        BottomNav(g.tab, { g.tab = it; if (it != Tab.ARCHIVE) Unit }, alerts)
    }
    prompt?.let { PromptSheet(it) { prompt = null } }
    image?.let { (p, r, c) -> FullImage(p, r, c) { image = null } }
}

@Composable
fun IntroScreen(g: GameController) {
    val t = rememberInfiniteTransition(label = "intro")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d")
    val glow by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val twinkle by t.animateFloat(0f, (Math.PI * 2).toFloat(), infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "t")
    val stars = remember { val r = Random(3); List(90) { Triple(Offset(r.nextFloat(), r.nextFloat() * 0.55f), r.nextFloat() * 1.6f + 0.6f, r.nextFloat() * 6.28f) } }
    var confirmNew by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.art_intro), null, Modifier.fillMaxSize().graphicsLayer {
            scaleX = 1.12f; scaleY = 1.12f; translationX = (drift - 0.5f) * 40f; translationY = (drift - 0.5f) * -26f
        }, contentScale = ContentScale.Crop)
        Canvas(Modifier.fillMaxSize()) {
            stars.forEach { (p, r, ph) ->
                val a = (0.35f + 0.65f * ((kotlin.math.sin(twinkle + ph) + 1f) / 2f))
                drawCircle(Color(0xFFFFF4E0).copy(alpha = a * 0.9f), r, Offset(p.x * size.width, p.y * size.height))
            }
            drawRect(Brush.verticalGradient(listOf(Color(0xCC0A0604), Color.Transparent, Color.Transparent, Color(0xF00A0604)), startY = 0f, endY = size.height))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(36.dp))
            Text("A NOVEL OF THE LONG SLEEP", style = label(11.sp, C.mustard, 0.3f))
            Spacer(Modifier.height(6.dp))
            Text("COLONY\nSHIP", style = TextStyle(fontFamily = F.righteous, fontSize = 64.sp, lineHeight = 60.sp, color = Color(0xFFFFF1DA), textAlign = TextAlign.Center,
                shadow = Shadow(Color(0xFFFF7A2A).copy(alpha = glow), Offset(0f, 0f), 38f * glow)))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { listOf(C.mustard, C.orange, C.burnt, C.teal).forEach { Box(Modifier.size(46.dp, 5.dp).background(it, RoundedCornerShape(2.dp))) } }
            Spacer(Modifier.height(10.dp))
            Text("ONE THOUSAND SLEEPERS · ONE TIRED MACHINE", style = label(11.sp, C.cream, 0.18f), textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            val s = g.state
            ConsoleKey("NEW RUN", Modifier.fillMaxWidth(), KeyStyle.ORANGE, height = 50.dp, textSize = 18, cueType = Cue.SELECT) { if (g.hasRun) confirmNew = true else g.newRun() }
            Spacer(Modifier.height(8.dp))
            ConsoleKey("CONTINUE", Modifier.fillMaxWidth(), KeyStyle.TEAL, height = 46.dp, textSize = 16, enabled = g.hasRun,
                sub = s?.takeIf { g.hasRun }?.let { "RUN ${it.runNumber.toString().padStart(2, '0')} · DAY ${it.day} · ${it.alive()} ALIVE" } ?: "NO VOYAGE IN PROGRESS") { g.continueRun() }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("ARCHIVE", Modifier.weight(1f), KeyStyle.DARK, height = 42.dp, sub = "${g.archive.size} HISTORIES") { g.tab = Tab.ARCHIVE; g.openBook = null; g.screen = Screen.GAME }
                ConsoleKey("SETTINGS", Modifier.weight(1f), KeyStyle.DARK, height = 42.dp, sub = "SOUND · HAPTICS") { g.settingsFrom = Screen.INTRO; g.screen = Screen.SETTINGS }
            }
            Spacer(Modifier.height(10.dp))
            Text("v$VERSION · ALL WORDS, ART, MUSIC AND CODE ORIGINAL", style = label(8.sp, C.muted.copy(alpha = 0.8f), 0.14f))
        }
    }
    if (confirmNew) Dialog(onDismissRequest = { confirmNew = false }) {
        Panel {
            SectionTitle("!!", "ABANDON CURRENT VOYAGE?")
            AiQuote("Starting again means the current run is gone. The sleepers won’t know. I will.", null, eye = 36.dp, size = 13)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("KEEP IT", Modifier.weight(1f), KeyStyle.DARK) { confirmNew = false }
                ConsoleKey("NEW RUN", Modifier.weight(1f), KeyStyle.ORANGE) { confirmNew = false; g.newRun() }
            }
        }
    }
}

@Composable
fun SettingsScreen(g: GameController) {
    BackHandler { g.screen = g.settingsFrom }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Header("SETTINGS", "MENU", listOf(Color(0xFF3FE0B0), null, null), null)
        Panel {
            SectionTitle("01", "SOUND")
            VolumeRow("MUSIC VOLUME", "Original ambient score", g.musicVolume) { g.setMusic(it) }
            Spacer(Modifier.height(8.dp))
            VolumeRow("SOUND EFFECTS", "Clicks, alarms, jumps", g.sfxVolume) { g.setSfx(it); }
        }
        Panel {
            SectionTitle("02", "HAPTICS", if (g.haptics) "ON" else "OFF", if (g.haptics) C.glow else C.muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VIBRATION FEEDBACK", style = label(12.sp, C.cream))
                    Text("Ticks on controls, rumbles on jumps, thuds on impacts and pod losses.", style = body(11.sp, C.muted))
                }
                Toggle(g.haptics) { g.toggleHaptics(it) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("TEST IMPACT", Modifier.weight(1f), KeyStyle.DARK, height = 34.dp, textSize = 11, cueType = Cue.IMPACT) {}
                ConsoleKey("TEST JUMP", Modifier.weight(1f), KeyStyle.DARK, height = 34.dp, textSize = 11, cueType = Cue.JUMP) {}
            }
        }
        Panel {
            SectionTitle("03", "VOYAGE")
            if (g.screen == Screen.SETTINGS && g.settingsFrom == Screen.GAME) {
                ConsoleKey("BACK TO THE CONSOLE", Modifier.fillMaxWidth(), KeyStyle.TEAL) { g.screen = Screen.GAME }
                Spacer(Modifier.height(8.dp))
            }
            ConsoleKey("TITLE SCREEN", Modifier.fillMaxWidth(), KeyStyle.DARK) { g.screen = Screen.INTRO }
            Spacer(Modifier.height(8.dp))
            Text("Your run saves automatically after every decision.", style = body(11.sp, C.muted))
        }
        Panel {
            SectionTitle("04", "ABOUT", "v$VERSION")
            Text("Colony Ship v$VERSION (debug build). All writing, art, music, sound and code are original to this game. Typefaces: Righteous, VT323, Barlow, Fraunces and EB Garamond, used under the SIL Open Font Licence.", style = body(11.sp, C.muted))
        }
        ConsoleKey("DONE", Modifier.fillMaxWidth(), KeyStyle.ORANGE) { g.screen = g.settingsFrom }
    }
}

@Composable
private fun VolumeRow(title: String, sub: String, v: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(110.dp)) { Text(title, style = label(11.sp, C.cream)); Text(sub, style = body(9.sp, C.muted)) }
        PowerSlider((v * 100).toInt(), true, Modifier.weight(1f).height(30.dp)) { onChange(it / 100f) }
        Spacer(Modifier.width(6.dp)); NixBox((v * 100).toInt().toString().padStart(2, '0').take(3))
    }
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    val play = cue()
    Box(Modifier.size(54.dp, 28.dp).background(if (on) C.tealDk else Color(0xFF221913), RoundedCornerShape(14.dp)).border(1.5.dp, Color(0xFF8C7556), RoundedCornerShape(14.dp))
        .clickable { play(Cue.TICK); onChange(!on) }.padding(3.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(22.dp).background(Brush.radialGradient(listOf(Color(0xFFFFF6E0), Color(0xFFD9C69C), Color(0xFF8A7350))), RoundedCornerShape(11.dp)))
    }
}

/** Warp-streak animation played while the jump drive runs. */
@Composable
fun JumpOverlay(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(2400, easing = LinearEasing)); delay(100); onDone() }
    val streaks = remember { val r = Random(11); List(140) { Triple(r.nextFloat() * 6.283f, r.nextFloat(), r.nextFloat()) } }
    Box(Modifier.fillMaxSize().background(Color(0xF0050302)).clickable(enabled = false) {}) {
        Canvas(Modifier.fillMaxSize()) {
            val p = progress.value
            val c = Offset(size.width / 2, size.height / 2)
            val speed = if (p < 0.7f) p / 0.7f else 1f - (p - 0.7f) / 0.3f * 0.6f
            streaks.forEach { (a, d, col) ->
                val dist = ((d + p * 2.2f) % 1f)
                val r0 = dist * size.maxDimension * 0.7f
                val len = 8f + speed * 160f * dist
                val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
                val color = if (col < 0.6f) C.glow else if (col < 0.85f) C.nix else Color.White
                drawLine(color.copy(alpha = (0.3f + dist * 0.7f) * (1f - p * 0.3f)), c + dir * r0, c + dir * (r0 + len), 1.5f + dist * 2.5f, StrokeCap.Round)
            }
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f * speed), Color.Transparent), center = c, radius = 120f), 120f, c)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("JUMP DRIVE ENGAGED", style = vfd(26.sp))
            Text("FOLDING SPACE · PLEASE REMAIN ASLEEP", style = label(10.sp, C.cream, 0.2f))
        }
    }
}
