package uk.saul.colonyship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.audio.Cue

/** One page of the prologue. */
data class StoryPanel(val art: String, val kicker: String, val title: String, val body: List<String>)

/** The prologue shown at the start of every new voyage. Original text; hard science fiction. */
val PROLOGUE = listOf(
    StoryPanel("intro_earth", "I · EARTH · 2187 AD", "Not Dying. Smaller.", listOf(
        "Earth was not dying. It was getting smaller. Eleven billion people on a planet whose climate had stopped getting worse only because nobody expected it to get better.",
        "The rest of the Solar System had turned out to be rock, ice and radiation. A few thousand people lived under the surface of Mars, and nobody lived there by choice.",
        "The nearest stars with planets worth the name were years away at the speed of light. Someone had to go first.")),
    StoryPanel("intro_ship", "II · THE SHIP", "Forty-One Years at L1", listOf(
        "She took forty-one years to build at the Earth–Moon L1 point: a spine two kilometres long, a fusion drive at one end and a shield of water ice at the other.",
        "A magnetic scoop gathers interstellar hydrogen to stretch the fuel. A rotating ring holds air and gravity for the few who are awake.",
        "Pushed hard, she can reach about four-fifths of the speed of light. Nothing she carries can bring her back.")),
    StoryPanel("intro_sleepers", "III · THE SLEEPERS", "One Thousand", listOf(
        "One thousand people, chosen from forty million applicants, vitrified at minus one hundred and thirty degrees: farmers, engineers, doctors, teachers, and children.",
        "The pods will keep them for decades, if the power holds, the coolant flows and nothing hits the ship.",
        "Five specialists can be woken in an emergency. Everyone awake breathes, eats and ages.")),
    StoryPanel("intro_burn", "IV · DEPARTURE · 9 MARCH 2187", "Half a Gravity, for Years", listOf(
        "The ship left at half a gravity and kept pushing.",
        "Near light speed, time aboard runs slower than time on Earth. Every year the sleepers spend crossing the dark, Earth lives through more of them.",
        "Whoever is left at home when they land will be strangers. Any message sent back will take years to arrive, and longer to be answered.")),
    StoryPanel("intro_ai", "V · YOU", "The Mind of the Ship", listOf(
        "No human can stay awake for a voyage like this. So the ship carries a mind, trained on every engineering manual, every medical text, and every hard decision its builders could imagine.",
        "It allocates the power. It repairs what it can. It decides who to wake, and who to let go.",
        "You are that mind.")),
    StoryPanel("intro_void", "VI · THE ARITHMETIC", "No Destination. Only Candidates.", listOf(
        "There is no destination, only candidate stars, too far away to be seen clearly from Earth.",
        "You will burn for each one, survey what you find, and choose. Settle, and the ship is broken up to build the colony. Move on, and the margins get thinner.",
        "Somewhere out there is a world good enough. Wake the ship.")),
)

@Composable
fun StoryScreen(g: GameController, startPage: Int = 0) {
    var page by remember { mutableStateOf(startPage.coerceIn(0, PROLOGUE.lastIndex)) }
    val play = cue()
    fun advance() {
        if (page < PROLOGUE.lastIndex) page++ else g.finishStory()
    }
    BackHandler { play(if (page > 0) Cue.TICK else Cue.BUTTON); if (page > 0) page-- else g.finishStory() }
    StoryPage(page, onTap = { advance() }, onSkip = { g.finishStory() })
}

@Composable
fun StoryPage(page: Int, onTap: () -> Unit, onSkip: () -> Unit, animate: Boolean = true) {
    val play = cue()
    Box(Modifier.fillMaxSize().background(Color(0xFF050302)).pointerInput(Unit) {
        detectTapGestures { play(if (page == PROLOGUE.lastIndex) Cue.SELECT else Cue.TICK); onTap() }
    }) {
        Crossfade(page, animationSpec = tween(700), label = "story") { p ->
            val panel = PROLOGUE[p]
            val zoom = remember(p) { Animatable(if (animate) 1.0f else 1.06f) }
            if (animate) LaunchedEffect(p) { zoom.animateTo(1.14f, tween(16000, easing = LinearOutSlowInEasing)) }
            Box(Modifier.fillMaxSize()) {
                Image(painterResource(rememberArt(panel.art)), null, Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom.value; scaleY = zoom.value; translationY = (zoom.value - 1f) * -120f
                }, contentScale = ContentScale.Crop)
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.verticalGradient(0f to Color(0xB0050302), 0.18f to Color.Transparent, 0.42f to Color.Transparent, 0.62f to Color(0xD8050302), 1f to Color(0xFA050302)))
                }
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 18.dp)) {
                    Spacer(Modifier.weight(1f))
                    Text(panel.kicker, style = label(11.sp, C.mustard, 0.28f))
                    Spacer(Modifier.height(6.dp))
                    Text(panel.title, style = TextStyle(fontFamily = F.fraunces, fontSize = 30.sp, lineHeight = 34.sp, color = Color(0xFFFFF1DA)))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { listOf(C.mustard, C.orange, C.burnt, C.teal).forEach { Box(Modifier.size(30.dp, 3.dp).background(it, RoundedCornerShape(2.dp))) } }
                    Spacer(Modifier.height(12.dp))
                    panel.body.forEach {
                        Text(it, style = TextStyle(fontFamily = F.garamond, fontSize = 17.sp, lineHeight = 23.sp, color = C.cream))
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(64.dp))
                }
            }
        }
        // Chrome that does not crossfade
        Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("PROLOGUE", style = label(10.sp, C.cream.copy(alpha = 0.8f), 0.3f))
            Spacer(Modifier.weight(1f))
            Box(Modifier.border(1.dp, C.cream.copy(alpha = 0.6f), RoundedCornerShape(14.dp)).hapticClick(interactionSource = remember { MutableInteractionSource() }, ripple = false) { onSkip() }
                .padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text("SKIP ›", style = label(11.sp, C.cream, 0.2f))
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 24.dp, vertical = 22.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PROLOGUE.indices.forEach { i ->
                Box(Modifier.padding(end = 6.dp).size(if (i == page) 22.dp else 8.dp, 4.dp).background(if (i <= page) C.orange else C.cream.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }
            Spacer(Modifier.weight(1f))
            val t = rememberInfiniteTransition(label = "tap")
            val a by t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "a")
            Text(if (page == PROLOGUE.lastIndex) "TAP TO WAKE THE SHIP" else "TAP TO CONTINUE", style = label(10.sp, C.glow.copy(alpha = if (animate) a else 1f), 0.24f))
        }
    }
}

/** Painting keys used by the prologue (for tests). */
val PROLOGUE_ART get() = PROLOGUE.map { it.art }
