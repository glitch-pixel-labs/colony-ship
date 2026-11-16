package uk.saul.colonyship.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.game.Engine
import uk.saul.colonyship.game.GameState
import uk.saul.colonyship.game.Sys
import kotlin.math.roundToInt

/** Always-visible fuel, power, hull and colonist readouts, pinned under the header. */
@Composable
fun StatusRibbon(s: GameState) {
    val pulse = rememberInfiniteTransition(label = "rib")
    val a by pulse.animateFloat(0.7f, 1f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
    val cells = listOf(
        Triple("FUEL", "${s.fuel.roundToInt()}", healthColor(s.fuel)),
        Triple("POWER", "${s.reserve.roundToInt()}", healthColor(s.reserve)),
        Triple("HULL", "${s.h(Sys.HULL).roundToInt()}%", healthColor(s.h(Sys.HULL))),
        Triple("COLONISTS", "${s.alive()}", if (s.alive() < 700) C.bad else C.glow),
    )
    val crisis = s.inCrisis()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(if (crisis) Color(0xFF2A0A08) else Color(0xFF071311), if (crisis) Color(0xFF1A0706) else Color(0xFF0C1F1C))))
            .border(1.dp, if (crisis) Color(0xFF8A2018) else Color(0xFF2B4A44), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        cells.forEach { (k, v, col) ->
            val warn = col == C.bad || col == C.warn
            Column(
                Modifier.weight(1f).graphicsLayer { alpha = if (warn) a else 1f },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(k, style = label(7.5.sp, if (crisis) Color(0xFFFF9A7A) else C.teal, 0.12f), maxLines = 1)
                Text(v, style = vfd(20.sp, col), maxLines = 1)
            }
        }
    }
}

@Composable
fun CrisisBanner(s: GameState) {
    if (!s.inCrisis()) return
    val pulse = rememberInfiniteTransition(label = "cr")
    val a by pulse.animateFloat(0.75f, 1f, infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "c")
    val (title, body) = Engine.crisisBanner(s)
    Column(
        Modifier.fillMaxWidth().graphicsLayer { alpha = a }.clip(RoundedCornerShape(5.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF4A0E0C), Color(0xFF2A0806))))
            .border(1.5.dp, Color(0xFFFF4A32), RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text("CATASTROPHIC ALERT", style = label(9.sp, Color(0xFFFF8A6A), 0.18f))
        Text(title, style = vfd(16.sp, Color(0xFFFFC8B4)))
        Text(body, style = body(11.sp, Color(0xFFFFE0D0)))
        val left = if (s.crisisLeft > 0) "ONE LEGAL BURN REMAINS" else "NO FURTHER BURN · SETTLE AT THIS WORLD"
        Spacer(Modifier.height(3.dp))
        Text(left, style = label(9.sp, Color(0xFFFFD36A), 0.12f))
    }
}

@Composable
fun PromptRow(onImage: () -> Unit, onVideo: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ConsoleKey("IMAGE", Modifier.weight(1f), KeyStyle.CREAM, height = 30.dp, textSize = 10, icon = { PicIcon(C.ink) }) { onImage() }
        ConsoleKey("VIDEO", Modifier.weight(1f), KeyStyle.DARK, height = 30.dp, textSize = 10, icon = { CamIcon(C.cream) }) { onVideo() }
    }
}

@Composable
fun BurnRiskLine(s: GameState) {
    val risk = Engine.burnRisk(s)
    val col = when (risk.level) {
        1 -> C.glow
        2 -> C.teal
        3 -> C.warn
        4 -> C.orange
        else -> C.bad
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0x22FFFFFF)).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BURN RISK", style = label(8.sp, C.muted, 0.12f))
            Spacer(Modifier.weight(1f))
            Text(risk.label, style = label(10.sp, col, 0.14f))
            Text("  ·  FUEL −${risk.fuelCost.roundToInt()}", style = label(9.sp, if (s.fuel < risk.fuelCost) C.bad else C.cream, 0.08f))
        }
        Text(risk.detail, style = body(10.sp, C.muted))
    }
}
