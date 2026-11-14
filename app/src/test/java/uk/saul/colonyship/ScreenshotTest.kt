package uk.saul.colonyship

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uk.saul.colonyship.audio.Cues
import uk.saul.colonyship.data.Store
import uk.saul.colonyship.game.*
import uk.saul.colonyship.ui.*
import java.io.File
import kotlin.random.Random

/** Renders key screens at Galaxy A54 size (1080×2340, ~411×891 dp). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-port-420dpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val out = System.getProperty("user.dir") + "/build/shots"

    private fun controller(): GameController {
        val ctx = rule.activity.applicationContext
        return GameController(Store(ctx), Cues(ctx))
    }

    /** A believable mid-voyage state, nudged to show off the console. */
    private fun midRun(): GameState {
        var s = Engine.newGame(7, 20261114L, 0L)
        repeat(5) { i ->
            s = Engine.jump(s)
            if (s.phase == Phase.EVENT) s = Engine.choose(s, if (i % 2 == 0) 0 else 1)
            if (s.phase == Phase.OUTCOME) s = Engine.arrive(s)
        }
        s.health[Sys.SCAN] = 44.0; s.health[Sys.SHIELD] = 67.0; s.health[Sys.ENGINE] = 80.0; s.health[Sys.HULL] = 74.0
        s.health[Sys.LIFE] = 91.0; s.health[Sys.CRYO] = 88.0
        s.air = 71.0; s.food = 84.0; s.water = 77.0; s.spares = 41.0; s.reserve = 36.0
        s.power[Channel.LIFE] = 32; s.power[Channel.SCAN] = 14; s.power[Channel.SHIELD] = 22; s.power[Channel.ENGINE] = 18; s.power[Channel.REPAIR] = 8
        val c = s.pods.toCharArray(); val r = Random(4)
        for (i in c.indices) c[i] = 'S'
        (0 until 13).forEach { c[r.nextInt(1000)] = 'L' }; (0 until 6).forEach { c[r.nextInt(1000)] = 'F' }
        s.pods = String(c)
        if ("okonkwo" !in s.awake) s.awake += "okonkwo"
        return s
    }

    private fun show(g: GameController, name: String, ms: Long = 1200, screen: Boolean = false) {
        rule.mainClock.autoAdvance = false
        rule.setContent { ColonyApp(g) }
        rule.mainClock.advanceTimeBy(ms)
        if (screen) captureScreenRoboImage("$out/$name.png") else rule.onRoot().captureRoboImage("$out/$name.png")
    }

    @Test fun s01_intro() { val g = controller(); g.debugSet(midRun()); g.screen = Screen.INTRO; show(g, "00-intro") }

    @Test fun s02_ship() { val g = controller(); val s = midRun(); s.phase = Phase.ORBIT; g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.SHIP; show(g, "01-ship-status") }

    @Test fun s03_event() {
        val g = controller(); val s = midRun()
        s.phase = Phase.EVENT; s.currentEvent = "storm"
        s.awake.clear()
        s.currentEvent = "derelict"; s.log += LogEntry("e99-derelict", s.day, "derelict", "The Derelict", "CONTACT", "derelict", earthYear = Physics.earthYear(s))
        g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.EVENTS; show(g, "02-event")
    }

    @Test fun s04_scan() {
        val g = controller(); var s = midRun()
        s.phase = Phase.OUTCOME
        s = Engine.arrive(s)
        // A promising world with two rows hidden by the damaged scanner.
        val p = Planet("Cinnabar B", 14.2, 1, 5, 0.87, 18, 62, 3, 1, listOf("Long archipelagos", "Rust deserts", "Cyclones"), 0.5f)
        val obs = PlanetGen.ROWS.associateWith { row -> Observation(row != "TEMPERATURE" && row != "RESOURCES", PlanetGen.trueValue(p, row), 0.15) }
        s.planet = p.copy(obs = obs, passes = 3)
        g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.SCAN; show(g, "03-planet-scan")
    }

    private fun endedRecord(g: GameController, withPicture: Boolean): RunRecord {
        var s = midRun()
        s.trust = -1
        s.traits.add(Trait("practical", "RUTHLESSLY PRACTICAL", "Day 312: elder pods sacrificed", 312))
        s.traits.add(Trait("grudge-okonkwo", "GRUDGE · OKONKWO LINE", "Okonkwo was overruled", 500))
        s.traits.add(Trait("dreamers", "DREAMERS", "Shared a dream", 400))
        s.planet = Planet("Cinnabar B", 14.2, 1, 4, 0.87, 18, 62, 4, 2, listOf("Long archipelagos", "Rust deserts"), 0.5f)
        s.phase = Phase.ORBIT
        s = Engine.settle(s, 1L)
        var rec = s.toRecord()
        if (withPicture) {
            val ctx = rule.activity.applicationContext
            val f = File(ctx.filesDir, "pictures").apply { mkdirs() }.let { File(it, "test-ch2.jpg") }
            ctx.resources.openRawResource(R.drawable.art_mid_town).use { i -> f.outputStream().use { i.copyTo(it) } }
            rec = rec.copy(chapters = rec.chapters.mapIndexed { i, c -> if (i == 1) c.copy(imagePath = f.absolutePath) else c })
        }
        return rec
    }

    @Test fun s05_book() {
        val g = controller()
        val rec = endedRecord(g, true)
        val older = listOf(
            rec.copy(runId = "r5", runNumber = 5, endingTitle = "The Tide League", settlementName = "Saltwater", planet = rec.planet?.copy(water = 90)),
            rec.copy(runId = "r6", runNumber = 6, endingTitle = "The Silent Landing", settlementName = "Hollis Landing", planet = rec.planet?.copy(water = 2)),
        )
        g.debugSetArchive(listOf(rec) + older)
        g.debugSet(null)
        g.screen = Screen.GAME; g.tab = Tab.ARCHIVE; g.openBook = rec.runId; g.bookPage = 1
        show(g, "04-history-book")
    }

    @Test fun s06_book_ch1() {
        val g = controller(); val rec = endedRecord(g, false)
        g.debugSetArchive(listOf(rec)); g.screen = Screen.GAME; g.tab = Tab.ARCHIVE; g.openBook = rec.runId; g.bookPage = 0
        show(g, "05-history-book-landfall")
    }

    @Test fun s07_archive() {
        val g = controller(); val rec = endedRecord(g, false)
        g.debugSetArchive(listOf(rec, rec.copy(runId = "b", runNumber = 5, endingTitle = "The Tide League"), rec.copy(runId = "c", runNumber = 6, endingTitle = "The Silent Landing")))
        g.debugSet(midRun()); g.screen = Screen.GAME; g.tab = Tab.ARCHIVE; g.openBook = null
        show(g, "06-archive")
    }

    @Test fun s08_settings() { val g = controller(); g.screen = Screen.SETTINGS; show(g, "07-settings") }

    @Test fun s09_outcome() {
        val g = controller(); var s = midRun()
        s.phase = Phase.EVENT; s.currentEvent = "letter"
        s.log += LogEntry("e98-letter", s.day, "letter", "Letter", "COMMS", "intro_earth")
        s = Engine.choose(s, 1)
        g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.EVENTS; show(g, "08-event-outcome")
    }

    @Test fun s10_prompt() {
        val g = controller(); val s = midRun()
        s.phase = Phase.EVENT; s.currentEvent = "flare"
        s.log += LogEntry("e97-flare", s.day, "flare", "Coronal Flare", "STELLAR", "flare")
        g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.EVENTS
        rule.mainClock.autoAdvance = false
        rule.setContent { ColonyApp(g) }
        rule.mainClock.advanceTimeBy(800)
        rule.onNode(androidx.compose.ui.test.hasText("IMAGE PROMPT")).performClickSafe()
        rule.mainClock.advanceTimeBy(800)
        captureScreenRoboImage("$out/09-image-prompt.png")
    }

    @Test fun s11_jump() {
        val g = controller(); val s = midRun(); s.phase = Phase.CRUISE; g.debugSet(s); g.screen = Screen.GAME; g.tab = Tab.SHIP
        rule.mainClock.autoAdvance = false
        rule.setContent { ColonyApp(g) }
        rule.mainClock.advanceTimeBy(300)
        g.jump()
        rule.mainClock.advanceTimeBy(900)
        rule.onRoot().captureRoboImage("$out/10a-transit-boost.png")
        rule.mainClock.advanceTimeBy(1000)
        rule.onRoot().captureRoboImage("$out/10b-transit-coast.png")
        rule.mainClock.advanceTimeBy(1900)
        rule.onRoot().captureRoboImage("$out/10c-transit-braking.png")
    }

    private fun story(page: Int) {
        rule.mainClock.autoAdvance = false
        rule.setContent { Backdrop { StoryPage(page, {}, {}, animate = false) } }
        rule.mainClock.advanceTimeBy(1200)
        rule.onRoot().captureRoboImage("$out/11-story-${page + 1}.png")
    }
    @Test fun s12_story1() = story(0)
    @Test fun s12_story2() = story(1)
    @Test fun s12_story3() = story(2)
    @Test fun s12_story4() = story(3)
    @Test fun s12_story5() = story(4)
    @Test fun s12_story6() = story(5)

    /** Settle a planet and show its history book pages. */
    private fun book(tag: String, p: Planet, trust: Int, traits: List<Trait> = emptyList(), page: Int = 0, seed: Long = 1L) {
        val g = controller(); var s = midRun()
        s.trust = trust; s.traits.addAll(traits); s.planet = p; s.phase = Phase.ORBIT
        s = Engine.settle(s, seed)
        val rec = s.toRecord()
        println("BOOK $tag: ${rec.endingTitle} art=${rec.chapters.map { it.art }}")
        g.debugSetArchive(listOf(rec)); g.debugSet(null)
        g.screen = Screen.GAME; g.tab = Tab.ARCHIVE; g.openBook = rec.runId; g.bookPage = page
        show(g, "12-book-$tag-p$page")
    }
    private val oceanP = Planet("Thalassa B", 9.0, 2, 4, 1.02, 17, 93, 3, 2, listOf("Floating kelp mats", "Long archipelagos"), 0.5f)
    private val iceP = Planet("Rimeholm C", 9.0, 1, 3, 0.92, -44, 9, 4, 1, listOf("Glacier belts", "Aurorae"), 0.58f)
    private val desertP = Planet("Ochre D", 9.0, 0, 4, 0.96, 31, 5, 4, 1, listOf("Rust deserts", "Salt flats"), 0.05f)
    private val toxicP = Planet("Murk B", 9.0, 0, 2, 1.0, 12, 30, 5, 0, listOf("Canyon systems"), 0.2f)
    private val jungleP = Planet("Verdance E", 9.0, 1, 5, 1.05, 27, 55, 4, 3, listOf("Crystal forests"), 0.3f)
    private val archive = listOf(Trait("archive", "THE ARCHIVE", "Kept the archive", 100))
    @Test fun s13_ocean0() = book("ocean", oceanP, 4, archive, 0)
    @Test fun s13_ocean1() = book("ocean", oceanP, 4, archive, 1)
    @Test fun s13_ocean2() = book("ocean", oceanP, 4, archive, 2)
    @Test fun s13_ice0() = book("ice", iceP, 1, emptyList(), 0)
    @Test fun s13_ice2() = book("ice", iceP, 1, emptyList(), 2)
    @Test fun s13_desert0() = book("desert", desertP, -2, emptyList(), 0)
    @Test fun s13_desert1() = book("desert", desertP, -2, emptyList(), 1)
    @Test fun s13_toxic1() = book("toxic", toxicP, 0, emptyList(), 1)
    @Test fun s13_jungle0() = book("jungle", jungleP, 3, archive, 0)
    @Test fun s13_jungle3() = book("jungle", jungleP, 3, archive, 3)
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.performClickSafe() { try { performClick() } catch (_: Throwable) {} }
