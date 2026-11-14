package uk.saul.colonyship

import android.os.VibrationEffect
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.audio.HAPTIC_BUTTON_MS
import uk.saul.colonyship.audio.HAPTIC_SELECT_MS
import uk.saul.colonyship.audio.HAPTIC_TICK_MS
import uk.saul.colonyship.audio.hapticEffect
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HapticsTest {

    @Test
    fun manifestDeclaresVibratePermission() {
        val manifest = sourceFile("src/main/AndroidManifest.xml")
        val text = manifest.readText()
        assertTrue(
            "VIBRATE permission missing in ${manifest.path}",
            text.contains("android.permission.VIBRATE"),
        )
    }

    @Test
    fun buttonCuesAreOneShotsNotPredefined() {
        assertTrue(HAPTIC_TICK_MS in 25L..40L)
        assertTrue(HAPTIC_BUTTON_MS in 25L..40L)
        assertTrue(HAPTIC_SELECT_MS in 25L..40L)
        for ((cue, ms) in listOf(Cue.TICK to HAPTIC_TICK_MS, Cue.BUTTON to HAPTIC_BUTTON_MS, Cue.SELECT to HAPTIC_SELECT_MS)) {
            val text = hapticEffect(cue, amplitudeControl = true).toString()
            assertFalse("$cue should not use a predefined/prebaked effect: $text", text.contains("Prebaked", ignoreCase = true) || text.contains("predefined", ignoreCase = true))
            assertTrue("$cue one-shot should mention duration $ms: $text", text.contains(ms.toString()))
            assertTrue("$cue should be a one-shot: $text", text.contains("OneShot", ignoreCase = true) || text.contains("One-shot", ignoreCase = true) || text.contains("duration"))
        }
        // Key-moment cues stay as waveforms.
        val jump = hapticEffect(Cue.JUMP, true).toString()
        assertTrue("JUMP should be a multi-step effect: $jump", jump.contains("Waveform", ignoreCase = true) || jump.contains("Composed", ignoreCase = true) || jump.contains("segments", ignoreCase = true))
    }

    @Test
    fun everyClickableGoesThroughHapticClickOrPlay() {
        val uiDir = sourceFile("src/main/java/uk/saul/colonyship/ui")
        assertTrue("ui source dir missing: $uiDir", uiDir.isDirectory)
        val violations = mutableListOf<String>()
        uiDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            val withoutHapticClick = stripFunction(text, "fun Modifier.hapticClick")
            withoutHapticClick.lines().forEachIndexed { i, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("import ")) return@forEachIndexed
                if (trimmed.startsWith("//")) return@forEachIndexed
                if (".clickable" in line || Regex("""(?<![A-Za-z])clickable\s*[\({]""").containsMatchIn(line)) {
                    violations += "${file.name}:${i + 1}: raw clickable — use hapticClick(): $trimmed"
                }
            }
            fun requirePlay(label: String, pattern: Regex, window: Int) {
                val lines = text.lines()
                lines.forEachIndexed { i, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import ") || trimmed.startsWith("//")) return@forEachIndexed
                    if (pattern.containsMatchIn(line)) {
                        val slice = lines.drop(i).take(window).joinToString("\n")
                        if (!slice.contains("play(") && !slice.contains("cues?.play") && !slice.contains("hapticClick")) {
                            violations += "${file.name}:${i + 1}: $label without play()/hapticClick in the next $window lines: ${line.trim()}"
                        }
                    }
                }
            }
            requirePlay("detectTapGestures", Regex("""detectTapGestures"""), 16)
            requirePlay("BackHandler", Regex("""BackHandler"""), 12)
            requirePlay("onDismissRequest", Regex("""onDismissRequest"""), 6)
        }
        if (violations.isNotEmpty()) {
            fail("Clickables missing haptics:\n" + violations.joinToString("\n"))
        }
    }

    private fun sourceFile(rel: String): File {
        val here = File(".").canonicalFile
        val candidates = listOf(
            File(here, rel),
            File(here, "app/$rel"),
            File(here.parentFile, rel),
            File(here.parentFile, "app/$rel"),
        )
        return candidates.firstOrNull { it.exists() } ?: File(here, rel)
    }

    /** Drop a function from [startSig] through its balanced braces so the lint can ignore its internals. */
    private fun stripFunction(text: String, startSig: String): String {
        val start = text.indexOf(startSig)
        if (start < 0) return text
        val brace = text.indexOf('{', start)
        if (brace < 0) return text
        var depth = 0
        var i = brace
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.removeRange(start, i + 1)
                }
            }
            i++
        }
        return text
    }
}
