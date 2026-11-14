package uk.saul.colonyship.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

fun copyText(ctx: Context, label: String, text: String) {
    try {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(ctx, "$label copied", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

fun shareText(ctx: Context, subject: String, text: String) {
    try {
        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, subject); putExtra(Intent.EXTRA_TEXT, text) }
        ctx.startActivity(Intent.createChooser(i, "Send prompt to…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) { Toast.makeText(ctx, "No app available to share with", Toast.LENGTH_SHORT).show() }
}

data class PromptShown(val title: String, val text: String)

/** Copies on open; shows the prompt with a Share button. */
@Composable
fun PromptSheet(p: PromptShown, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val play = cue()
    LaunchedEffect(p) { copyText(ctx, p.title, p.text) }
    Dialog(onDismissRequest = { play(uk.saul.colonyship.audio.Cue.BUTTON); onClose() }) {
        Panel(Modifier.padding(4.dp)) {
            SectionTitle("✓", p.title.uppercase(), "COPIED", C.glow)
            Box(Modifier.heightIn(max = 300.dp).fillMaxWidth().background(Color(0xFF071311), RoundedCornerShape(6.dp)).border(1.dp, Color(0xFF2B4A44), RoundedCornerShape(6.dp)).padding(10.dp)) {
                Text(p.text, style = body(12.sp, C.cream), modifier = Modifier.verticalScroll(rememberScrollState()))
            }
            Text("Paste it into Grok Imagine or any image/video generator you like, then attach the result here with ‘Attach picture’.", style = body(11.sp, C.muted), modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConsoleKey("SHARE…", Modifier.weight(1f), KeyStyle.TEAL) { shareText(ctx, p.title, p.text) }
                ConsoleKey("COPY AGAIN", Modifier.weight(1f), KeyStyle.CREAM) { copyText(ctx, p.title, p.text) }
                ConsoleKey("CLOSE", Modifier.weight(0.8f), KeyStyle.DARK) { onClose() }
            }
        }
    }
}

/** Android Photo Picker (falls back to the system picker on older devices automatically). */
@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) onPicked(uri) }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
fun rememberFileBitmap(path: String?): ImageBitmap? {
    var bmp by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bmp = path?.let { p ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try { if (File(p).exists()) BitmapFactory.decodeFile(p)?.asImageBitmap() else null } catch (_: Exception) { null }
            }
        }
    }
    return bmp
}

/** Full-screen viewer with pinch-zoom, for attached pictures and plates. */
@Composable
fun FullImage(path: String?, fallbackRes: Int, caption: String, onClose: () -> Unit) {
    val bmp = rememberFileBitmap(path)
    val play = cue()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Dialog(onDismissRequest = { play(uk.saul.colonyship.audio.Cue.BUTTON); onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).hapticClick { onClose() }) {
            val m = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); offset = if (scale == 1f) androidx.compose.ui.geometry.Offset.Zero else offset + pan }
            }.graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
            if (bmp != null) Image(bmp, caption, m, contentScale = ContentScale.Fit)
            else Image(painterResource(fallbackRes), caption, m, contentScale = ContentScale.Fit)
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xB0000000)).navigationBarsPadding().padding(14.dp)) {
                Text(caption, style = quote(13.sp))
                Text("TAP TO CLOSE · PINCH TO ZOOM", style = label(9.sp, C.muted), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
