package uk.saul.colonyship.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Gapless ambient loop with fades.
 *
 * The score file is LOOP_MS of seamlessly looping music followed by a TAIL_MS copy of its own opening.
 * Two players alternate: when the active one reaches LOOP_MS it is playing the tail, so the other player
 * starts from 0 and the two crossfade over identical audio for XFADE_MS. Any start latency or decoder
 * gap is masked because both players carry the same sound. Starting fades in; pausing fades out first.
 */
class MusicLooper(private val ctx: Context, private val res: Int) {
    companion object {
        const val LOOP_MS = 64_000
        const val TAIL_MS = 6_000
        const val XFADE_MS = 4_000
        const val FADE_IN_MS = 3_000
        const val FADE_OUT_MS = 700
        private const val TICK_MS = 40L
    }

    var volume = 0.54f
        set(v) { field = v; apply() }

    private val handler = Handler(Looper.getMainLooper())
    private var a: MediaPlayer? = null
    private var b: MediaPlayer? = null
    private var active: MediaPlayer? = null
    private var next: MediaPlayer? = null
    private var crossStart = -1L
    private var master = 0f          // 0..1 fade envelope
    private var target = 0f
    private var fadeStep = 0f
    private var running = false

    private fun create(): MediaPlayer? = try { MediaPlayer.create(ctx, res)?.apply { isLooping = false; setVolume(0f, 0f) } } catch (e: Exception) { Log.w("Music", "create", e); null }

    fun start() {
        try {
            if (a == null) { a = create(); b = create(); active = a; next = b }
            val p = active ?: return
            target = 1f; fadeStep = TICK_MS.toFloat() / FADE_IN_MS
            if (!p.isPlaying) p.start()
            if (crossStart >= 0) next?.takeIf { !it.isPlaying }?.start()
            if (!running) { running = true; handler.post(tick) }
        } catch (e: Exception) { Log.w("Music", "start", e) }
    }

    fun pause() {
        target = 0f; fadeStep = TICK_MS.toFloat() / FADE_OUT_MS
        if (!running && master > 0f) { running = true; handler.post(tick) }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null); running = false
        try { a?.release(); b?.release() } catch (_: Exception) {}
        a = null; b = null; active = null; next = null
    }

    private val tick = object : Runnable {
        override fun run() {
            try { step() } catch (e: Exception) { Log.w("Music", "tick", e) }
            if (running) handler.postDelayed(this, TICK_MS)
        }
    }

    private fun step() {
        // Fade envelope
        master = if (master < target) (master + fadeStep).coerceAtMost(target) else (master - fadeStep).coerceAtLeast(target)
        if (master <= 0f && target <= 0f) {
            active?.takeIf { it.isPlaying }?.pause(); next?.takeIf { it.isPlaying }?.pause()
            running = false; apply(); return
        }
        val p = active ?: return
        val pos = p.currentPosition
        // Start the other player so that its 0 ms lines up with our LOOP_MS mark, over the shared tail.
        if (crossStart < 0 && pos >= LOOP_MS - 50) {
            next?.let { n -> n.seekTo((pos - LOOP_MS).coerceAtLeast(0)); n.start() }
            crossStart = System.currentTimeMillis()
        }
        if (crossStart >= 0) {
            val k = ((System.currentTimeMillis() - crossStart).toFloat() / XFADE_MS).coerceIn(0f, 1f)
            setVol(p, (1f - k)); setVol(next, k)
            if (k >= 1f || !p.isPlaying) {
                try { p.pause(); p.seekTo(0) } catch (_: Exception) {}
                active = next; next = p; crossStart = -1
                setVol(active, 1f); setVol(next, 0f)
            }
        } else setVol(p, 1f)
    }

    private var lastMix = mutableMapOf<MediaPlayer, Float>()
    private fun setVol(mp: MediaPlayer?, mix: Float) {
        mp ?: return
        lastMix[mp] = mix
        // Equal-power is unnecessary: both players carry identical audio during the crossfade, so linear sums to unity.
        val v = (volume * master * mix).coerceIn(0f, 1f)
        try { mp.setVolume(v, v) } catch (_: Exception) {}
    }
    private fun apply() { lastMix.forEach { (mp, m) -> setVol(mp, m) } }
}
