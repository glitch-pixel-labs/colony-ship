package uk.saul.colonyship.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import uk.saul.colonyship.game.GameState
import uk.saul.colonyship.game.JSON
import uk.saul.colonyship.game.RunRecord
import java.io.File
import java.util.concurrent.Executors

/** Local persistence: current run, legacy archive, attached pictures, settings. */
class Store(private val ctx: Context) {
    private val dir = ctx.filesDir
    private val io = Executors.newSingleThreadExecutor()
    private val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun atomicWrite(f: File, text: String) {
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(text); if (!tmp.renameTo(f)) { f.writeText(text); tmp.delete() }
    }

    fun loadCurrent(): GameState? = try {
        File(dir, "current.json").takeIf { it.exists() }?.readText()?.let { JSON.decodeFromString(GameState.serializer(), it) }
    } catch (e: Exception) { Log.w("Store", "bad save", e); null }

    fun saveCurrent(s: GameState?) {
        val text = s?.let { JSON.encodeToString(GameState.serializer(), it) }
        io.execute {
            try { val f = File(dir, "current.json"); if (text == null) f.delete() else atomicWrite(f, text) } catch (e: Exception) { Log.w("Store", "save", e) }
        }
    }

    fun loadArchive(): List<RunRecord> = try {
        File(dir, "archive.json").takeIf { it.exists() }?.readText()?.let { JSON.decodeFromString(ListSerializer(RunRecord.serializer()), it) } ?: emptyList()
    } catch (e: Exception) { Log.w("Store", "bad archive", e); emptyList() }

    fun saveArchive(list: List<RunRecord>) {
        val text = JSON.encodeToString(ListSerializer(RunRecord.serializer()), list)
        io.execute { try { atomicWrite(File(dir, "archive.json"), text) } catch (e: Exception) { Log.w("Store", "archive", e) } }
    }

    /** Copies a picked image into app storage (downscaled), returning its path. */
    fun importPicture(uri: Uri, key: String): String? = try {
        val pics = File(dir, "pictures").apply { mkdirs() }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1600 || bounds.outHeight / (sample * 2) >= 1600) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        if (bmp == null) null else {
            val out = File(pics, "${key.replace(Regex("[^A-Za-z0-9_-]"), "_")}-${System.currentTimeMillis()}.jpg")
            out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            bmp.recycle()
            out.absolutePath
        }
    } catch (e: Exception) { Log.w("Store", "import", e); null }

    var musicVolume: Float get() = prefs.getFloat("music", 0.6f); set(v) = prefs.edit().putFloat("music", v).apply()
    var sfxVolume: Float get() = prefs.getFloat("sfx", 0.8f); set(v) = prefs.edit().putFloat("sfx", v).apply()
    var haptics: Boolean get() = prefs.getBoolean("haptics", true); set(v) = prefs.edit().putBoolean("haptics", v).apply()
    var runCounter: Int get() = prefs.getInt("runs", 0); set(v) = prefs.edit().putInt("runs", v).apply()
}
