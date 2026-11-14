package uk.saul.colonyship.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import uk.saul.colonyship.R

enum class Cue { TICK, BUTTON, SELECT, IMPACT, ALARM, POD_LOSS, JUMP, SCAN, SETTLE }

/**
 * Sound effects, ambient music and haptics in one place, with graceful fallbacks.
 *
 * Button-press haptics use [Vibrator] one-shot pulses, not [VibrationEffect.createPredefined]
 * or weak [HapticFeedbackConstants]. Samsung treats predefined EFFECT_CLICK / EFFECT_TICK
 * (and many View haptic constants) as system "touch vibration" and silently drops them
 * when that setting is off. [VibrationEffect.createOneShot] and waveforms still run
 * with the VIBRATE permission. [View.performHapticFeedback] is only a fallback if the
 * vibrator is missing.
 */
class Cues(private val ctx: Context) {
    var musicVolume = 0.6f
        set(v) { field = v; looper.volume = v * 0.9f }
    var sfxVolume = 0.8f
    var hapticsOn = true

    private val pool: SoundPool? = try {
        SoundPool.Builder().setMaxStreams(6).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()
    } catch (e: Exception) { null }
    private val ids = mutableMapOf<Cue, Int>()
    private var lastTick = 0L

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } catch (e: Exception) { null }

    init {
        pool?.let { p ->
            val map = mapOf(Cue.TICK to R.raw.sfx_click, Cue.BUTTON to R.raw.sfx_click, Cue.SELECT to R.raw.sfx_select, Cue.IMPACT to R.raw.sfx_impact,
                Cue.ALARM to R.raw.sfx_alarm, Cue.POD_LOSS to R.raw.sfx_podloss, Cue.JUMP to R.raw.sfx_jump, Cue.SCAN to R.raw.sfx_scan, Cue.SETTLE to R.raw.sfx_settle)
            map.forEach { (c, r) -> try { ids[c] = p.load(ctx, r, 1) } catch (_: Exception) {} }
        }
    }

    private val looper = MusicLooper(ctx, R.raw.music_ambient)

    /** Fade the score in (on launch and on returning to the app). */
    fun startMusic() = looper.start()
    /** Fade the score out, then pause (on leaving the app). */
    fun pauseMusic() = looper.pause()
    fun release() { looper.release(); try { pool?.release() } catch (_: Exception) {} }

    fun play(c: Cue, view: View? = null) {
        val now = System.currentTimeMillis()
        if (c == Cue.TICK) { if (now - lastTick < 45) return; lastTick = now }
        val vol = when (c) { Cue.TICK -> 0.35f; Cue.BUTTON -> 0.6f; else -> 1f } * sfxVolume
        if (vol > 0.01f) ids[c]?.let { pool?.play(it, vol, vol, 1, 0, 1f) }
        vibrate(c, view)
    }

    fun vibrate(c: Cue, view: View? = null) {
        if (!hapticsOn) return
        val v = vibrator
        if (v != null && v.hasVibrator()) {
            try {
                v.vibrate(hapticEffect(c, v.hasAmplitudeControl()))
                return
            } catch (e: Exception) { Log.w("Cues", "vibrate", e) }
        }
        viewHaptic(view, c)
    }

    private fun viewHaptic(view: View?, c: Cue) {
        if (view == null) return
        val constant = when (c) {
            Cue.TICK -> HapticFeedbackConstants.CLOCK_TICK
            Cue.BUTTON, Cue.SELECT -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.KEYBOARD_TAP
            else -> HapticFeedbackConstants.LONG_PRESS
        }
        try { view.performHapticFeedback(constant) } catch (_: Exception) {}
    }
}

/** Durations chosen to be clearly felt on Samsung phones (25–40 ms, default amplitude). */
internal const val HAPTIC_TICK_MS = 28L
internal const val HAPTIC_BUTTON_MS = 36L
internal const val HAPTIC_SELECT_MS = 40L

/** Build the vibration for a cue. Button presses are one-shots, never createPredefined. */
internal fun hapticEffect(c: Cue, amplitudeControl: Boolean): VibrationEffect = when (c) {
    Cue.TICK -> VibrationEffect.createOneShot(HAPTIC_TICK_MS, VibrationEffect.DEFAULT_AMPLITUDE)
    Cue.BUTTON -> VibrationEffect.createOneShot(HAPTIC_BUTTON_MS, VibrationEffect.DEFAULT_AMPLITUDE)
    Cue.SELECT -> VibrationEffect.createOneShot(HAPTIC_SELECT_MS, VibrationEffect.DEFAULT_AMPLITUDE)
    Cue.SCAN -> hapticWave(longArrayOf(0, 20, 60, 20, 60, 20), intArrayOf(0, 90, 0, 120, 0, 160), amplitudeControl)
    Cue.IMPACT -> hapticWave(longArrayOf(0, 90, 40, 140, 50, 60), intArrayOf(0, 255, 0, 200, 0, 120), amplitudeControl)
    Cue.ALARM -> hapticWave(longArrayOf(0, 180, 120, 180, 120, 180), intArrayOf(0, 220, 0, 220, 0, 220), amplitudeControl)
    Cue.POD_LOSS -> hapticWave(longArrayOf(0, 60, 200, 260), intArrayOf(0, 200, 0, 90), amplitudeControl)
    Cue.JUMP -> hapticWave(longArrayOf(0, 200, 200, 300, 400, 300, 200), intArrayOf(0, 40, 80, 140, 255, 140, 50), amplitudeControl)
    Cue.SETTLE -> hapticWave(longArrayOf(0, 60, 120, 60, 120, 60, 160, 500), intArrayOf(0, 120, 0, 160, 0, 200, 0, 255), amplitudeControl)
}

internal fun hapticWave(t: LongArray, a: IntArray, amp: Boolean): VibrationEffect =
    if (amp) VibrationEffect.createWaveform(t, a, -1) else VibrationEffect.createWaveform(t, -1)
