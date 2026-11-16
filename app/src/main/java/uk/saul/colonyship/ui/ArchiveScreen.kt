package uk.saul.colonyship.ui

import android.widget.Toast
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.game.*

const val CURRENT = "current"

fun archiveHeader(g: GameController): Pair<String, String> {
    val rec = g.archive.firstOrNull { it.runId == g.openBook }
    return "COLONY ARCHIVE" to when {
        rec != null -> "RUN ${rec.runNumber.toString().padStart(2, '0')}"
        g.openBook == CURRENT -> "SHIP'S LOG"
        else -> "${g.archive.size} BOOK${if (g.archive.size == 1) "" else "S"}"
    }
}

private val ROMAN = listOf("I", "II", "III", "IV", "V")
private val SPINES = listOf(listOf(Color(0xFF2FA39C), Color(0xFF17605C)), listOf(Color(0xFF3E3027), Color(0xFF1A120D)), listOf(Color(0xFFE8692C), Color(0xFF8A3214)))

@Composable
fun ArchiveScreen(g: GameController, showPrompt: (PromptShown) -> Unit, showImage: (String?, Int, String) -> Unit) {
    val s = g.state
    val open = g.openBook
    if (open == CURRENT && s != null) { BookScreen(g, s.toRecord(), true, showPrompt, showImage); return }
    val rec = g.archive.firstOrNull { it.runId == open }
    if (rec != null) { BookScreen(g, rec, false, showPrompt, showImage); return }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Panel {
            SectionTitle("05", "LEGACY ARCHIVE", "${g.archive.size} HISTORIES")
            AiQuote(if (g.archive.isEmpty()) "No histories yet. Every colony I found, and every voyage that failed, will be recorded here in full." else "Every world we chose, and every voyage that went wrong. I keep them all, with the names.", null, eye = 36.dp, size = 13)
        }
        if (s != null && s.phase != Phase.ENDED) {
            Panel {
                SectionTitle("··", "CURRENT VOYAGE · RUN ${s.runNumber.toString().padStart(2, '0')}", "${Physics.earthYear(s)} AD")
                Text("${s.log.size} log entries · ${s.alive()} alive · ${s.log.count { it.imagePath != null }} pictures attached", style = body(12.sp, C.muted))
                Spacer(Modifier.height(8.dp))
                ConsoleKey("OPEN THE SHIP'S LOG", Modifier.fillMaxWidth(), KeyStyle.CREAM) { g.bookPage = 0; g.openBook = CURRENT }
            }
        }
        g.archive.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { r -> BookSpine(r, Modifier.weight(1f).height(118.dp), false) { g.bookPage = 0; g.openBook = r.runId } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun BookSpine(r: RunRecord, modifier: Modifier, current: Boolean, onClick: () -> Unit) {
    val col = SPINES[(r.runNumber + 1) % 3]
    val shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 7.dp, bottomEnd = 7.dp)
    Box(modifier.shadow(6.dp, shape).clip(shape).background(Brush.verticalGradient(col)).grain(0.8f)
        .then(if (current) Modifier.border(2.dp, C.nix, shape) else Modifier)
        .hapticClick { onClick() }
        .drawBehind {
            drawLine(Color(0x66FFE6B0), Offset(6.dp.toPx(), 0f), Offset(6.dp.toPx(), size.height), 1.5f)
            drawLine(Color(0x66FFE6B0), Offset(size.width - 4, size.height - 14.dp.toPx()), Offset(10.dp.toPx(), size.height - 14.dp.toPx()), 1f)
        }.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Column {
            Text("RUN ${r.runNumber.toString().padStart(2, '0')} · ${worldKind(r.planet).uppercase()}", style = label(8.5.sp, Color(0xFFFFE6C0), 0.1f), maxLines = 1)
            Text(if (current) "Reading now" else r.endingTitle, style = TextStyle(fontFamily = F.fraunces, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFFF4E0), lineHeight = 17.sp), maxLines = 2)
            Spacer(Modifier.weight(1f))
            Text("${r.settlementName.ifBlank { r.planetName }} · ${r.survivors} survived", style = body(9.5.sp, Color(0xD9FFF0DC)), maxLines = 1)
            Text("AI: ${r.aiFate.lowercase()}", style = body(9.sp, Color(0xB3FFF0DC)), maxLines = 1)
        }
    }
}

fun worldKind(p: Planet?): String = if (p == null) "Deep space" else when (Endings.biome(p)) {
    "ocean" -> "Ocean world"; "ice" -> "Ice world"; "desert" -> "Arid world"; "toxic" -> "Toxic world"; "volcanic" -> "Volcanic world"
    "locked" -> "Tidally locked world"; "heavy" -> "High-gravity world"; "jungle" -> "Jungle world"; "storm" -> "Storm world"; else -> "Temperate world"
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BookScreen(g: GameController, r: RunRecord, current: Boolean, showPrompt: (PromptShown) -> Unit, showImage: (String?, Int, String) -> Unit) {
    val ctx = LocalContext.current
    val hasChart = r.route.isNotEmpty()
    val pages = r.chapters.size + 1 + if (hasChart) 1 else 0
    val page = g.bookPage.coerceIn(0, (pages - 1).coerceAtLeast(0))
    val chartIndex = if (hasChart) r.chapters.size else -1
    val ch = r.chapters.getOrNull(page)
    val pick = rememberPhotoPicker { uri ->
        val ok = if (ch != null) g.attachToChapter(r.runId, ch.key, uri) else false
        if (!ok) Toast.makeText(ctx, "Couldn’t read that picture", Toast.LENGTH_SHORT).show()
    }
    val paperShape = RoundedCornerShape(6.dp)
    Box(Modifier.fillMaxWidth().shadow(10.dp, paperShape).clip(paperShape).background(Color(0xFF5A3A22)).padding(7.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF3E7CB), Color(0xFFEADBB8), Color(0xFFE2CFA6)))).grain(0.35f)
                .drawBehind {
                    drawRect(Color(0x55A07040), Offset(8.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 16.dp.toPx(), size.height - 16.dp.toPx()), style = Stroke(1.5f))
                    var y = 0f; while (y < size.height) { drawLine(Color(0x0D5A3A22), Offset(0f, y), Offset(size.width, y), 1f); y += 4f }
                }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val chapterLabel = when {
                ch != null -> "CHAPTER ${ROMAN.getOrElse(page) { "${page + 1}" }}"
                page == chartIndex -> "THE VOYAGE"
                else -> "THE SHIP'S LOG"
            }
            Text(if (current) "THE VOYAGE SO FAR · RUN ${r.runNumber.toString().padStart(2, '0')}" else "A HISTORY OF ${r.settlementName.uppercase().let { if (it.startsWith("THE")) it else "THE $it SETTLEMENT".replace("THE THE", "THE") }} · $chapterLabel",
                style = label(10.sp, C.inkRed, 0.12f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            // Timeline
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                val labels = r.chapters.map { "${it.yearLabel} · ${it.title.removePrefix("The ").uppercase()}" } +
                    (if (hasChart) listOf("CHART · THE VOYAGE") else emptyList()) +
                    "LOG · ${r.log.size} ENTRIES"
                labels.forEachIndexed { i, l ->
                    Column(Modifier.weight(1f).hapticClick(uk.saul.colonyship.audio.Cue.TICK) { g.bookPage = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(11.dp).clip(CircleShape).background(if (i == page) C.orange else if (i < page) Color(0xFF7A5A40) else Color.Transparent).border(1.5.dp, Color(0xFF7A5A40), CircleShape))
                        Text(l, style = label(7.5.sp, if (i == page) C.inkRed else Color(0xFF6A4A32), 0.06f), textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            when {
                ch != null -> ChapterPage(g, r, ch, page, current, showPrompt, showImage, pick)
                page == chartIndex -> BookChartPage(r)
                else -> LogPage(g, r, showPrompt, showImage)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) Text("← PREVIOUS", style = label(10.sp, C.tealDk), modifier = Modifier.hapticClick(uk.saul.colonyship.audio.Cue.TICK) { g.bookPage = page - 1 }.padding(6.dp))
                Spacer(Modifier.weight(1f))
                if (page < pages - 1) Text("NEXT →", style = label(10.sp, C.tealDk), modifier = Modifier.hapticClick(uk.saul.colonyship.audio.Cue.TICK) { g.bookPage = page + 1 }.padding(6.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Text("LEGACY LIST →", style = label(10.sp, C.tealDk, 0.14f), modifier = Modifier.hapticClick { g.openBook = null }.padding(6.dp))
            }
            val shelf = (listOf(r) + g.archive.filter { it.runId != r.runId }).take(3)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                shelf.forEach { b -> BookSpine(b, Modifier.weight(1f).height(70.dp), b.runId == r.runId) { if (b.runId != r.runId) { g.bookPage = 0; g.openBook = b.runId } } }
                repeat(3 - shelf.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(12.dp))
            Text("~ ${if (ch != null) "${ROMAN.getOrElse(page) { "" }} · ${ch.yearLabel.removePrefix("YEAR ")} A.L." else "LOG"} ~", style = label(9.sp, Color(0xFF8A6A50), 0.2f))
            if (!current && r.endingTitle.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                ConsoleKey("BEGIN A NEW VOYAGE", Modifier.fillMaxWidth(), KeyStyle.ORANGE, height = 40.dp) { g.newRun() }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChapterPage(g: GameController, r: RunRecord, ch: Chapter, page: Int, current: Boolean, showPrompt: (PromptShown) -> Unit, showImage: (String?, Int, String) -> Unit, pick: () -> Unit) {
    val ctx = LocalContext.current
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color = C.ink)) { append(Prompts.yearTitle(ch.yearLabel) + ": ") }
        withStyle(SpanStyle(color = C.inkRed)) { append(ch.title) }
    }, style = TextStyle(fontFamily = F.fraunces, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 32.sp), textAlign = TextAlign.Center)
    Text("✦ ✦ ✦", style = TextStyle(fontSize = 10.sp, color = C.inkRed), modifier = Modifier.padding(vertical = 6.dp))
    // Plate
    val bmp = rememberFileBitmap(ch.imagePath)
    Box(Modifier.fillMaxWidth().background(Color(0xFFF7EEDA)).border(1.dp, Color(0xFF9A7A5A)).padding(5.dp).border(1.dp, Color(0xFF9A7A5A)).padding(3.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.333f).hapticClick { showImage(ch.imagePath, artRes(ch.art), ch.caption) }) {
            if (bmp != null) Image(bmp, ch.caption, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Image(painterResource(artRes(ch.art)), ch.caption, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Row(Modifier.padding(6.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.background(Color(0xCC140A06), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Lamp(if (bmp != null) Color(0xFF3FE0B0) else C.mustard, 5.dp); Spacer(Modifier.width(4.dp))
                    Text(if (bmp != null) "YOUR PICTURE · ATTACHED FROM GALLERY" else "SHIP'S RECORD · DEFAULT PLATE", style = label(7.5.sp, C.cream, 0.1f))
                }
                Spacer(Modifier.weight(1f))
                if (!current) Box(Modifier.background(Color(0xFFF0E3C4), RoundedCornerShape(2.dp)).hapticClick { pick() }.padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text(if (bmp != null) "REPLACE" else "ATTACH", style = label(8.sp, C.ink, 0.1f))
                }
            }
        }
    }
    Text(ch.caption, style = TextStyle(fontFamily = F.garamond, fontStyle = FontStyle.Italic, fontSize = 11.sp, color = C.inkRed), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 6.dp))
    ch.paragraphs.forEachIndexed { i, para ->
        Text(buildAnnotatedString {
            if (i == 0 && para.isNotEmpty()) {
                withStyle(SpanStyle(fontFamily = F.fraunces, fontWeight = FontWeight.Bold, fontSize = 34.sp, color = C.inkRed)) { append(para.first()) }
                append(para.drop(1))
            } else append(para)
        }, style = TextStyle(fontFamily = F.garamond, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
    }
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        r.traits.take(6).forEach { t ->
            val grudge = t.label.startsWith("GRUDGE")
            PaperChip(if (grudge) t.label else "TRAIT · ${t.label}" + if (t.id == "practical") " (${t.detail})" else "", C.inkRed)
        }
        if (r.aiFate.isNotBlank()) PaperChip("AI FATE · ${r.aiFate}", C.tealDk)
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PillButton("+ PICTURE", true, Modifier.weight(1f), small = true) { pick() }
        PillButton("IMAGE", false, Modifier.weight(1f), small = true) { showPrompt(PromptShown("Image prompt", Prompts.chapterImage(r.planet, r.settlementName, ch, r.traits, r.survivors))) }
        PillButton("VIDEO", false, Modifier.weight(1f), small = true) { showPrompt(PromptShown("Video prompt", Prompts.chapterVideo(r.planet, r.settlementName, ch, r.traits, r.survivors))) }
        PillButton("SHARE", false, Modifier.weight(1f), small = true) {
            shareText(ctx, "Colony Ship · ${ch.title}", "${ch.yearLabel}: ${ch.title}\n\n" + ch.paragraphs.joinToString("\n\n") + "\n\n— from Colony Ship, run ${r.runNumber}")
        }
    }
}

@Composable
private fun LogPage(g: GameController, r: RunRecord, showPrompt: (PromptShown) -> Unit, showImage: (String?, Int, String) -> Unit) {
    val ctx = LocalContext.current
    Text("The Ship’s Log", style = TextStyle(fontFamily = F.fraunces, fontWeight = FontWeight.Bold, fontSize = 27.sp, color = C.ink))
    Text("✦ ✦ ✦", style = TextStyle(fontSize = 10.sp, color = C.inkRed), modifier = Modifier.padding(vertical = 6.dp))
    if (r.log.isEmpty()) Text("Nothing logged yet. The voyage has not begun.", style = TextStyle(fontFamily = F.garamond, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = C.ink))
    r.log.reversed().forEach { e ->
        val ev = Events.byId(e.eventId)
        var pickFor by remember { mutableStateOf(false) }
        val pick = rememberPhotoPicker { uri -> if (!g.attachToLog(r.runId, e.id, uri)) Toast.makeText(ctx, "Couldn’t read that picture", Toast.LENGTH_SHORT).show() }
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).drawBehind { drawLine(Color(0x405A3A22), Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (e.earthYear > 0) "DAY ${e.day} · ${e.earthYear} AD" else "DAY ${e.day}", style = label(9.sp, C.inkRed, 0.12f)); Spacer(Modifier.width(6.dp))
                Text(e.title, style = TextStyle(fontFamily = F.fraunces, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.ink))
            }
            if (e.choice.isNotBlank()) Text("Chose: ${e.choice}", style = label(9.sp, C.tealDk, 0.08f))
            if (e.imagePath != null) {
                val bmp = rememberFileBitmap(e.imagePath)
                Box(Modifier.padding(vertical = 5.dp).fillMaxWidth().aspectRatio(1.6f).border(1.dp, Color(0xFF9A7A5A)).hapticClick { showImage(e.imagePath, artRes(e.art), e.title) }) {
                    if (bmp != null) Image(bmp, e.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
            if (e.outcome.isNotBlank()) Text(e.outcome, style = TextStyle(fontFamily = F.garamond, fontStyle = FontStyle.Italic, fontSize = 13.5.sp, lineHeight = 18.sp, color = C.ink))
            if (ev != null) Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                PillButton(if (e.imagePath == null) "+ PICTURE" else "REPLACE", true, Modifier.weight(1f), small = true) { pick() }
                PillButton("IMAGE PROMPT", false, Modifier.weight(1f), small = true) { showPrompt(PromptShown("Image prompt", LogPrompts.image(ev, e))) }
                PillButton("VIDEO PROMPT", false, Modifier.weight(1f), small = true) { showPrompt(PromptShown("Video prompt", LogPrompts.video(ev, e))) }
            }
        }
    }
}

@Composable
fun PaperChip(text: String, color: Color) {
    Box(Modifier.border(1.dp, color, RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.08f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(text, style = label(8.5.sp, color, 0.08f))
    }
}

@Composable
fun PillButton(text: String, filled: Boolean, modifier: Modifier, small: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(modifier.height(if (small) 28.dp else 36.dp).shadow(if (filled) 4.dp else 0.dp, shape).clip(shape)
        .then(if (filled) Modifier.background(Brush.verticalGradient(listOf(Color(0xFFF08A45), Color(0xFFB4461B)))) else Modifier.background(Color(0x1A2A1A0F)).border(1.5.dp, C.ink, shape))
        .hapticClick { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, style = label(if (small) 8.5.sp else 10.sp, if (filled) Color(0xFFFFF4E0) else C.ink, 0.1f), maxLines = 1)
    }
}
