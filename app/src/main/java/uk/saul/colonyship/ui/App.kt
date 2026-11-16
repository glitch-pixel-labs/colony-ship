package uk.saul.colonyship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

const val VERSION = "0.3.0"

@Composable
fun ColonyApp(g: GameController) {
    CompositionLocalProvider(LocalCues provides g.cues) {
        Backdrop {
            when (g.screen) {
                Screen.INTRO -> IntroScreen(g)
                Screen.STORY -> StoryScreen(g)
                Screen.SETTINGS -> SettingsScreen(g)
                Screen.GAME -> GameShell(g)
            }
            if (g.jumping) g.state?.let { st -> TransitOverlay(st) { g.finishJump() } }
        }
    }
}

@Composable
fun GameShell(g: GameController) {
    var prompt by remember { mutableStateOf<PromptShown?>(null) }
    var image by remember { mutableStateOf<Triple<String?, Int, String>?>(null) }
    val play = cue()
    BackHandler(enabled = true) {
        play(Cue.BUTTON)
        when {
            g.chartOpen -> g.chartOpen = false
            g.openBook != null && g.tab == Tab.ARCHIVE -> g.openBook = null
            g.tab != Tab.SHIP -> g.tab = Tab.SHIP
            else -> g.screen = Screen.INTRO
        }
    }
    val s = g.state
    val (l1, l2) = when {
        s?.inCrisis() == true && !g.chartOpen && g.tab != Tab.ARCHIVE ->
            "CATASTROPHIC" to if (s.crisisLeft > 0) "SETTLE OR 1 BURN" else "SETTLE NOW"
        g.chartOpen -> (s?.let { "${"%.1f".format(Physics.distanceFromSol(it))} LY · SOL" } ?: "CHART") to (s?.let { "${Physics.earthYear(it)} AD" } ?: "CARTOGRAPHY")
        g.tab == Tab.SHIP -> (s?.let { "${"%.1f".format(Physics.distanceFromSol(it))} LY · SOL" } ?: "SYSTEM STATUS") to (s?.let { "${Physics.earthYear(it)} AD" } ?: "STANDBY")
        g.tab == Tab.EVENTS -> eventHeader(s)
        g.tab == Tab.SCAN -> scanHeader(s)
        else -> archiveHeader(g)
    }
    val stress = s?.let { Voice.stress(it) } ?: 0
    val lamps = when {
        s?.inCrisis() == true -> listOf(Color(0xFFFF3B1F), Color(0xFFFF3B1F), Color(0xFFFF3B1F))
        stress >= 3 -> listOf(Color(0xFFFF3B1F), Color(0xFFFF3B1F), Color(0xFFFFC23A))
        stress >= 1 -> listOf(Color(0xFF3FE0B0), Color(0xFFFF8A2A), Color(0xFFFFC23A))
        else -> listOf(Color(0xFF3FE0B0), Color(0xFF3FE0B0), null)
    }
    Column(Modifier.fillMaxSize().scanlines(if (s?.inCrisis() == true) 0.12f else 0.07f)) {
        Box(Modifier.statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Header(l1, l2, lamps) { g.settingsFrom = Screen.GAME; g.screen = Screen.SETTINGS }
        }
        if (s != null) {
            Column(Modifier.padding(horizontal = 10.dp).padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusRibbon(s)
                CrisisBanner(s)
            }
        }
        key(g.tab, g.openBook, g.chartOpen) {
            val scroll = rememberScrollState()
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = 10.dp).padding(bottom = 10.dp)) {
                if (g.chartOpen && s != null) {
                    ChartScreen(g, s)
                } else if (s == null && g.tab != Tab.ARCHIVE) {
                    Panel {
                        AiQuote("No voyage in progress. One thousand sleepers are waiting at Earth–Moon L1 for a decision.", "SHIP AI")
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
            Text("ONE THOUSAND SLEEPERS · ONE MACHINE · NO WAY BACK", style = label(11.sp, C.cream, 0.18f), textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            val s = g.state
            ConsoleKey("NEW RUN", Modifier.fillMaxWidth(), KeyStyle.ORANGE, height = 50.dp, textSize = 18, cueType = Cue.SELECT) { if (g.hasRun) confirmNew = true else g.newRun() }
            Spacer(Modifier.height(8.dp))
            ConsoleKey("CONTINUE", Modifier.fillMaxWidth(), KeyStyle.TEAL, height = 46.dp, textSize = 16, enabled = g.hasRun,
                sub = s?.takeIf { g.hasRun }?.let { "RUN ${it.runNumber.toString().padStart(2, '0')} · ${Physics.earthYear(it)} AD · ${it.alive()} ALIVE" } ?: "NO VOYAGE IN PROGRESS") { g.continueRun() }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("ARCHIVE", Modifier.weight(1f), KeyStyle.DARK, height = 42.dp, sub = "${g.archive.size} HISTORIES") { g.tab = Tab.ARCHIVE; g.openBook = null; g.screen = Screen.GAME }
                ConsoleKey("SETTINGS", Modifier.weight(1f), KeyStyle.DARK, height = 42.dp, sub = "SOUND · HAPTICS") { g.settingsFrom = Screen.INTRO; g.screen = Screen.SETTINGS }
            }
            Spacer(Modifier.height(10.dp))
            Text("v$VERSION · ALL WORDS, ART, MUSIC AND CODE ORIGINAL", style = label(8.sp, C.muted.copy(alpha = 0.8f), 0.14f))
        }
    }
    val play = cue()
    if (confirmNew) Dialog(onDismissRequest = { play(Cue.BUTTON); confirmNew = false }) {
        Panel {
            SectionTitle("!!", "ABANDON CURRENT VOYAGE?")
            AiQuote("Starting again means this voyage ends here. The sleepers will never know. I will keep the record.", null, eye = 36.dp, size = 13)
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
    val play = cue()
    BackHandler { play(Cue.BUTTON); g.screen = g.settingsFrom }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Header("SETTINGS", "MENU", listOf(Color(0xFF3FE0B0), null, null), null)
        Panel {
            SectionTitle("01", "SOUND")
            VolumeRow("MUSIC VOLUME", "Original ambient score", g.musicVolume) { g.setMusic(it) }
            Spacer(Modifier.height(8.dp))
            VolumeRow("SOUND EFFECTS", "Clicks, alarms, burns", g.sfxVolume) { g.setSfx(it); }
        }
        Panel {
            SectionTitle("02", "HAPTICS", if (g.haptics) "ON" else "OFF", if (g.haptics) C.glow else C.muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("VIBRATION FEEDBACK", style = label(12.sp, C.cream))
                    Text("Ticks on controls, rumbles on drive burns, thuds on impacts and pod losses.", style = body(11.sp, C.muted))
                }
                Toggle(g.haptics) { g.toggleHaptics(it) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("TEST IMPACT", Modifier.weight(1f), KeyStyle.DARK, height = 34.dp, textSize = 11, cueType = Cue.IMPACT) {}
                ConsoleKey("TEST BURN", Modifier.weight(1f), KeyStyle.DARK, height = 34.dp, textSize = 11, cueType = Cue.JUMP) {}
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
            ConsoleKey("REPLAY THE PROLOGUE", Modifier.fillMaxWidth(), KeyStyle.DARK) { g.replayStory() }
            Spacer(Modifier.height(8.dp))
            Text("Your run saves automatically after every decision.", style = body(11.sp, C.muted))
        }
        Panel {
            SectionTitle("04", "ABOUT", "v$VERSION")
            Text("Colony Ship v$VERSION. All writing, art, music, sound and code are original to this game. Typefaces: Righteous, VT323, Barlow, Fraunces and EB Garamond, used under the SIL Open Font Licence.", style = body(11.sp, C.muted))
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
    Box(Modifier.size(54.dp, 28.dp).background(if (on) C.tealDk else Color(0xFF221913), RoundedCornerShape(14.dp)).border(1.5.dp, Color(0xFF8C7556), RoundedCornerShape(14.dp))
        .hapticClick { onChange(!on) }.padding(3.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(Modifier.size(22.dp).background(Brush.radialGradient(listOf(Color(0xFFFFF6E0), Color(0xFFD9C69C), Color(0xFF8A7350))), RoundedCornerShape(11.dp)))
    }
}

