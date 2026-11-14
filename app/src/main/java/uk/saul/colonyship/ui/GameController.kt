package uk.saul.colonyship.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uk.saul.colonyship.audio.Cue
import uk.saul.colonyship.audio.Cues
import uk.saul.colonyship.data.Store
import uk.saul.colonyship.game.*

enum class Screen { INTRO, STORY, GAME, SETTINGS }

/** Holds the live run and archive, persists every change, and fires sound/haptic cues. */
class GameController(private val store: Store, val cues: Cues) {
    var state by mutableStateOf(store.loadCurrent()?.takeIf { it.phase != Phase.ENDED })
        private set
    var archive by mutableStateOf(store.loadArchive())
        private set
    var screen by mutableStateOf(Screen.INTRO)
    var tab by mutableStateOf(Tab.SHIP)
    var openBook by mutableStateOf<String?>(null)
    var bookPage by mutableStateOf(0)
    var jumping by mutableStateOf(false)
    var settingsFrom by mutableStateOf(Screen.INTRO)
    var musicVolume by mutableStateOf(store.musicVolume)
        private set
    var sfxVolume by mutableStateOf(store.sfxVolume)
        private set
    var haptics by mutableStateOf(store.haptics)
        private set

    init { cues.musicVolume = musicVolume; cues.sfxVolume = sfxVolume; cues.hapticsOn = haptics }

    val hasRun get() = state != null && state?.phase != Phase.ENDED

    fun setMusic(v: Float) { musicVolume = v; store.musicVolume = v; cues.musicVolume = v }
    fun setSfx(v: Float) { sfxVolume = v; store.sfxVolume = v; cues.sfxVolume = v }
    fun toggleHaptics(on: Boolean) { haptics = on; store.haptics = on; cues.hapticsOn = on; if (on) cues.play(Cue.SELECT) }

    private fun commit(new: GameState) {
        val old = state
        state = new
        store.saveCurrent(if (new.phase == Phase.ENDED) null else new)
        if (old != null) {
            when {
                new.lost() > old.lost() -> cues.play(Cue.POD_LOSS)
                new.h(Sys.HULL) < old.h(Sys.HULL) - 4 -> cues.play(Cue.IMPACT)
            }
        }
    }

    fun newRun() {
        val n = store.runCounter + 1
        store.runCounter = n
        val s = Engine.newGame(n, System.nanoTime() xor System.currentTimeMillis(), System.currentTimeMillis())
        state = s; store.saveCurrent(s)
        tab = Tab.SHIP; openBook = null; storyThenGame = true; screen = Screen.STORY
    }

    /** True when the prologue should hand over to the console; false when replayed from settings. */
    var storyThenGame by mutableStateOf(true)
    fun replayStory() { storyThenGame = false; screen = Screen.STORY }
    fun finishStory() { screen = if (storyThenGame && state != null) Screen.GAME else settingsFrom.takeIf { !storyThenGame } ?: Screen.INTRO }

    fun continueRun() { if (state != null) { screen = Screen.GAME } }

    fun setPower(ch: Channel, v: Int) { state?.let { val n = Engine.setPower(it, ch, v); if (n.power != it.power) { state = n; store.saveCurrent(n); cues.play(Cue.TICK) } } }

    /** Starts the jump animation; [finishJump] is called by the overlay when it ends. */
    fun jump() {
        val s = state ?: return
        if (jumping || (s.phase != Phase.CRUISE && s.phase != Phase.ORBIT)) return
        jumping = true
        cues.play(Cue.JUMP)
        commit(Engine.jump(s))
    }

    fun finishJump() {
        jumping = false
        val s = state ?: return
        if (s.phase == Phase.ENDED) { endRun(s); return }
        tab = Tab.EVENTS
        val ev = s.currentEvent?.let { Events.byId(it) }
        if (ev != null && ev.id in setOf("storm", "breach", "cascade", "reactor", "coolant", "flare", "elders", "radiator", "bearing", "fuel", "rogue")) cues.play(Cue.ALARM) else cues.play(Cue.SELECT)
    }

    fun choose(i: Int) {
        val s = state ?: return
        if (s.phase != Phase.EVENT) return
        val n = Engine.choose(s, i)
        commit(n)
        if (n.phase == Phase.ENDED) endRun(n)
    }

    fun arrive() {
        val s = state ?: return
        if (s.phase != Phase.OUTCOME) return
        commit(Engine.arrive(s))
        cues.play(Cue.SCAN)
        tab = Tab.SCAN
    }

    fun rescan() { state?.let { if (it.phase == Phase.ORBIT) { commit(Engine.rescan(it)); cues.play(Cue.SCAN) } } }

    fun settle() {
        val s = state ?: return
        if (s.phase != Phase.ORBIT) return
        val n = Engine.settle(s, System.currentTimeMillis())
        commit(n)
        cues.play(Cue.SETTLE)
        endRun(n)
    }

    private fun endRun(s: GameState) {
        val rec = s.toRecord()
        archive = listOf(rec) + archive.filter { it.runId != rec.runId }
        store.saveArchive(archive)
        store.saveCurrent(null)
        openBook = rec.runId
        bookPage = 0
        tab = Tab.ARCHIVE
    }

    fun abandonRun() { state = null; store.saveCurrent(null); screen = Screen.INTRO }

    fun attachToLog(runId: String, logId: String, uri: Uri): Boolean {
        val path = store.importPicture(uri, "$runId-$logId") ?: return false
        val s = state
        if (s != null && s.runId == runId) commit(Engine.attachToLog(s, logId, path))
        archive = archive.map { r -> if (r.runId == runId) r.copy(log = r.log.map { if (it.id == logId) it.copy(imagePath = path) else it }) else r }
        store.saveArchive(archive)
        cues.play(Cue.SELECT)
        return true
    }

    fun attachToChapter(runId: String, key: String, uri: Uri): Boolean {
        val path = store.importPicture(uri, "$runId-$key") ?: return false
        archive = archive.map { r -> if (r.runId == runId) r.copy(chapters = r.chapters.map { if (it.key == key) it.copy(imagePath = path) else it }) else r }
        store.saveArchive(archive)
        cues.play(Cue.SELECT)
        return true
    }

    /** Test/debug hook: replace the live state without cues. */
    fun debugSet(s: GameState?) { state = s }
    fun debugSetArchive(list: List<RunRecord>) { archive = list }

    fun deleteRecord(runId: String) { archive = archive.filter { it.runId != runId }; store.saveArchive(archive); if (openBook == runId) openBook = null }
}
