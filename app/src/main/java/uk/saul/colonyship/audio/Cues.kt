package uk.saul.colonyship.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import uk.saul.colonyship.R

enum class Cue { TICK, BUTTON, SELECT, IMPACT, ALARM, POD_LOSS, JUMP, SCAN, SETTLE }

/** Sound effects, ambient music and haptics in one place, with graceful fallbacks. */
class Cues(private val ctx: Context) {
    var musicVolume = 0.6f
        set(v) { field = v; try { music?.setVolume(v * 0.9f, v * 0.9f) } catch (_: Exception) {} }
    var sfxVolume = 0.8f
    var hapticsOn = true

    private var music: MediaPlayer? = null
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

    fun startMusic() {
        try {
            if (music == null) {
                music = MediaPlayer.create(ctx, R.raw.music_ambient)?.apply { isLooping = true; setVolume(musicVolume * 0.9f, musicVolume * 0.9f) }
            }
            if (music?.isPlaying == false) music?.start()
        } catch (e: Exception) { Log.w("Cues", "music", e) }
    }
    fun pauseMusic() { try { music?.takeIf { it.isPlaying }?.pause() } catch (_: Exception) {} }
    fun release() { try { music?.release(); music = null; pool?.release() } catch (_: Exception) {} }

    fun play(c: Cue) {
        val now = System.currentTimeMillis()
        if (c == Cue.TICK) { if (now - lastTick < 45) return; lastTick = now }
        val vol = when (c) { Cue.TICK -> 0.35f; Cue.BUTTON -> 0.6f; else -> 1f } * sfxVolume
        if (vol > 0.01f) ids[c]?.let { pool?.play(it, vol, vol, 1, 0, 1f) }
        vibrate(c)
    }

    fun vibrate(c: Cue) {
        val v = vibrator ?: return
        if (!hapticsOn || !v.hasVibrator()) return
        try {
            val amp = v.hasAmplitudeControl()
            val effect: VibrationEffect = when (c) {
                Cue.TICK -> predefined(VibrationEffect.EFFECT_TICK) ?: VibrationEffect.createOneShot(8, if (amp) 60 else VibrationEffect.DEFAULT_AMPLITUDE)
                Cue.BUTTON -> predefined(VibrationEffect.EFFECT_CLICK) ?: VibrationEffect.createOneShot(18, if (amp) 140 else VibrationEffect.DEFAULT_AMPLITUDE)
                Cue.SELECT -> predefined(VibrationEffect.EFFECT_HEAVY_CLICK) ?: VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                Cue.SCAN -> wave(longArrayOf(0, 20, 60, 20, 60, 20), intArrayOf(0, 90, 0, 120, 0, 160), amp)
                Cue.IMPACT -> wave(longArrayOf(0, 90, 40, 140, 50, 60), intArrayOf(0, 255, 0, 200, 0, 120), amp)
                Cue.ALARM -> wave(longArrayOf(0, 180, 120, 180, 120, 180), intArrayOf(0, 220, 0, 220, 0, 220), amp)
                Cue.POD_LOSS -> wave(longArrayOf(0, 60, 200, 260), intArrayOf(0, 200, 0, 90), amp)
                Cue.JUMP -> wave(longArrayOf(0, 200, 200, 300, 400, 300, 200), intArrayOf(0, 40, 80, 140, 255, 140, 50), amp)
                Cue.SETTLE -> wave(longArrayOf(0, 60, 120, 60, 120, 60, 160, 500), intArrayOf(0, 120, 0, 160, 0, 200, 0, 255), amp)
            }
            v.vibrate(effect)
        } catch (e: Exception) { Log.w("Cues", "vibrate", e) }
    }

    private fun predefined(id: Int): VibrationEffect? = if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(id) else null
    private fun wave(t: LongArray, a: IntArray, amp: Boolean): VibrationEffect =
        if (amp) VibrationEffect.createWaveform(t, a, -1) else VibrationEffect.createWaveform(t, -1)
}
