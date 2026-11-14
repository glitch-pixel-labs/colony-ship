package uk.saul.colonyship.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import uk.saul.colonyship.R
import kotlin.random.Random

object C {
    val orange = Color(0xFFE8692C); val burnt = Color(0xFFB4461B); val rust = Color(0xFF8A3214)
    val teal = Color(0xFF2FA39C); val tealDk = Color(0xFF17605C); val glow = Color(0xFF7FF5DC)
    val cream = Color(0xFFF0E3C4); val cream2 = Color(0xFFD9C69C); val mustard = Color(0xFFE6A92A)
    val brown = Color(0xFF3B2717); val dk = Color(0xFF140D09); val red = Color(0xFFE0472C); val nix = Color(0xFFFFAD55)
    val muted = Color(0xFFC9B48E); val warn = Color(0xFFFFD36A); val bad = Color(0xFFFF8A6A)
    val paper = Color(0xFFEFE2C2); val ink = Color(0xFF2A1A0F); val inkRed = Color(0xFFB23A16)
}

object F {
    val righteous = FontFamily(Font(R.font.righteous))
    val vt = FontFamily(Font(R.font.vt323))
    val barlowC = FontFamily(Font(R.font.barlowc_semibold, FontWeight.SemiBold), Font(R.font.barlowc_bold, FontWeight.Bold), Font(R.font.barlowc_extrabold, FontWeight.ExtraBold))
    val barlow = FontFamily(Font(R.font.barlowsc_regular), Font(R.font.barlowsc_medium, FontWeight.Medium), Font(R.font.barlowsc_italic, style = FontStyle.Italic))
    val fraunces = FontFamily(Font(R.font.fraunces_bold, FontWeight.Bold), Font(R.font.fraunces_semibold, FontWeight.SemiBold), Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic))
    val garamond = FontFamily(Font(R.font.garamond_regular), Font(R.font.garamond_italic, style = FontStyle.Italic))
}

fun label(size: TextUnit = 11.sp, color: Color = C.muted, spacing: Float = 0.18f) =
    TextStyle(fontFamily = F.barlowC, fontWeight = FontWeight.ExtraBold, fontSize = size, color = color, letterSpacing = (size.value * spacing).sp)
fun vfd(size: TextUnit, color: Color = C.glow) = TextStyle(fontFamily = F.vt, fontSize = size, color = color,
    shadow = Shadow(color.copy(alpha = 0.8f), Offset.Zero, 10f))
fun body(size: TextUnit = 13.sp, color: Color = C.cream) = TextStyle(fontFamily = F.barlow, fontSize = size, color = color, lineHeight = (size.value * 1.3f).sp)
fun quote(size: TextUnit = 14.sp) = TextStyle(fontFamily = F.fraunces, fontStyle = FontStyle.Italic, fontSize = size, color = Color(0xFFF3E6C8), lineHeight = (size.value * 1.32f).sp)

/** Shared grain texture, generated once. */
object Grain {
    val bitmap: ImageBitmap by lazy {
        val w = 160; val bmp = ImageBitmap(w, w); val canvas = Canvas(bmp); val p = Paint(); val r = Random(7)
        repeat(4200) {
            p.color = if (r.nextBoolean()) Color.Black.copy(alpha = r.nextFloat() * 0.35f) else Color.White.copy(alpha = r.nextFloat() * 0.08f)
            canvas.drawRect(r.nextInt(w).toFloat(), r.nextInt(w).toFloat(), r.nextInt(w).toFloat() + 1f, r.nextInt(w).toFloat() + 1f, p)
        }
        bmp
    }
    val brush: ShaderBrush by lazy { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
}

/** Faint CRT scanlines + vignette over a surface. */
fun Modifier.scanlines(alpha: Float = 0.10f, vignette: Boolean = true) = this.drawWithCache {
    val lines = Path().apply {
        var y = 0f
        while (y < size.height) { addRect(androidx.compose.ui.geometry.Rect(0f, y, size.width, y + 1.2f)); y += 3.6f }
    }
    val vig = Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)), center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension * 0.75f)
    onDrawWithContent {
        drawContent()
        drawPath(lines, Color.Black.copy(alpha = alpha))
        if (vignette) drawRect(vig)
    }
}

fun Modifier.grain(alpha: Float = 1f) = this.drawWithCache {
    onDrawBehind { drawRect(Grain.brush, alpha = alpha) }
}

@Composable
fun rememberArt(name: String): Int = remember(name) { artRes(name) }

fun artRes(name: String): Int = when (name) {
    "ai_eye" -> R.drawable.art_ai_eye; "cryobay" -> R.drawable.art_cryobay; "derelict" -> R.drawable.art_derelict
    "engineering" -> R.drawable.art_engineering; "flare" -> R.drawable.art_flare; "intro" -> R.drawable.art_intro
    "landfall" -> R.drawable.art_landfall; "legacy" -> R.drawable.art_legacy; "planet" -> R.drawable.art_planet
    "ruin" -> R.drawable.art_ruin; "schism" -> R.drawable.art_schism; "ship" -> R.drawable.art_ship
    else -> R.drawable.art_storm
}
