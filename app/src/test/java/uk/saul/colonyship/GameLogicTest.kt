package uk.saul.colonyship

import org.junit.Assert.*
import org.junit.Test
import uk.saul.colonyship.game.*
import kotlin.random.Random
import uk.saul.colonyship.ui.PROLOGUE_ART

class GameLogicTest {
    private fun fresh(seed: Long = 42L) = Engine.newGame(1, seed, 0L)

    @Test fun newGameHasThousandPods() {
        val s = fresh()
        assertEquals(TOTAL_PODS, s.pods.length)
        assertEquals(TOTAL_PODS, s.alive())
        assertTrue(s.failing() in 2..5)
        assertEquals(95, s.drawn())
    }

    @Test fun powerNeverExceedsBudget() {
        var s = fresh()
        s = Engine.setPower(s, Channel.LIFE, 90)
        assertTrue(s.drawn() <= POWER_BUDGET)
        s = Engine.setPower(s, Channel.SCAN, 100)
        assertTrue(s.drawn() <= POWER_BUDGET)
        s = Engine.setPower(s, Channel.SCAN, -5)
        assertEquals(0, s.power[Channel.SCAN])
    }

    @Test fun atLeastFortyEventsAllWithTwoOrThreeChoices() {
        assertTrue("events=${Events.all.size}", Events.all.size >= 40)
        assertEquals(Events.all.size, Events.all.map { it.id }.toSet().size)
        val all = fresh(); all.day = 2000
        SPECIALISTS.forEach { all.awake += it.id }
        Fx(all, Random(1)).failPods(20)
        val sleepy = fresh().also { it.day = 2000 }
        for (e in Events.all) {
            listOf(all, sleepy).forEach { st ->
                if (e.condition(st)) assertTrue("${e.id} choices", e.available(st).size in 2..3)
            }
            e.choices.forEach { assertTrue(it.risk in 1..5) }
        }
    }

    @Test fun everyChoiceResolvesWithoutCrashing() {
        for (e in Events.all) for (i in 0 until 3) for (allAwake in listOf(false, true)) {
            val s = fresh(); s.day = 2000
            if (allAwake) SPECIALISTS.forEach { s.awake += it.id }
            Fx(s, Random(1)).failPods(20)
            if (!e.condition(s)) continue
            s.currentEvent = e.id; s.phase = Phase.EVENT
            s.log += LogEntry("x", 0, e.id, e.title, e.kicker, e.art)
            if (i >= e.available(s).size) continue
            val out = Engine.choose(s, i)
            assertNotNull(out.lastOutcome)
            assertTrue(out.lastOutcome!!.isNotBlank())
            assertEquals(TOTAL_PODS, out.pods.length)
            assertTrue(out.reserve in 0.0..100.0)
            assertEquals(e.available(s)[i].title, out.log.last().choice)
        }
    }

    @Test fun jumpAdvancesTimeAndQueuesEvent() {
        val s = Engine.jump(fresh())
        assertEquals(1, s.jumps)
        assertTrue(s.day >= 200)
        // Relativistic: Earth ages more than the ship, and we are light years from Sol.
        assertTrue("earth ${s.earthDays} ship ${s.day}", s.earthDays > s.day * 1.1)
        assertTrue(Physics.distanceFromSol(s) > 3.0)
        assertTrue(Physics.earthYear(s) > Physics.LAUNCH_YEAR)
        assertTrue(s.lastBeta in 0.3..0.95)
        assertEquals(Phase.EVENT, s.phase)
        assertNotNull(s.currentEvent)
        assertTrue(s.air < 100.0)
        assertEquals(1, s.log.size)
    }

    @Test fun underPoweredSystemsDegradeFaster() {
        var a = fresh(7); var b = fresh(7)
        b = Engine.setPower(b, Channel.SHIELD, 0)
        a = Engine.jump(a); b = Engine.jump(b)
        assertTrue(b.h(Sys.SHIELD) < a.h(Sys.SHIELD))
    }

    @Test fun damagedScannersHideRows() {
        var hidden = 0; var total = 0
        for (seed in 1L..40L) {
            val s = fresh(seed); s.health[Sys.SCAN] = 25.0
            val p = Engine.scanPass(s, PlanetGen.generate(Random(seed), 1, 8.0), Random(seed))
            hidden += p.obs.values.count { !it.ok }; total += p.obs.size
        }
        assertTrue("hidden=$hidden of $total", hidden > total / 5)
        var hiddenGood = 0
        for (seed in 1L..40L) {
            val s = fresh(seed)
            val p = Engine.scanPass(s, PlanetGen.generate(Random(seed), 1, 8.0), Random(seed))
            hiddenGood += p.obs.values.count { !it.ok }
        }
        assertEquals(0, hiddenGood)
    }

    @Test fun rescanCostsPowerAndImproves() {
        var s = Engine.arrive(Engine.choose(Engine.jump(fresh()), 0))
        assertEquals(Phase.ORBIT, s.phase)
        val r0 = s.reserve; val p0 = s.planet!!.passes
        s = Engine.rescan(s)
        assertEquals(p0 + 1, s.planet!!.passes)
        assertTrue(s.reserve < r0 || r0 == 0.0)
    }

    @Test fun fullRunsReachEndingsWithChapters() {
        val types = mutableSetOf<String>()
        for (seed in 1L..300L) {
            var s = fresh(seed)
            var guard = 0
            val target = (seed % 6).toInt()
            while (s.phase != Phase.ENDED && guard++ < 60) {
                s = when (s.phase) {
                    Phase.CRUISE -> Engine.jump(s)
                    Phase.EVENT -> Engine.choose(s, (seed % 2).toInt())
                    Phase.OUTCOME -> Engine.arrive(s)
                    Phase.ORBIT -> if (s.jumps >= target + 1) Engine.settle(s, 1L) else Engine.jump(s)
                    Phase.ENDED -> s
                }
            }
            assertEquals("seed $seed", Phase.ENDED, s.phase)
            val e = s.ending!!
            assertTrue(e.chapters.size in 3..4)
            e.chapters.forEach { c -> assertTrue(c.paragraphs.all { it.isNotBlank() }); assertFalse(c.paragraphs.any { it.contains("\${") }) }
            types += e.id
            val rec = s.toRecord()
            val back = JSON.decodeFromString(RunRecord.serializer(), JSON.encodeToString(RunRecord.serializer(), rec))
            assertEquals(rec.chapters.size, back.chapters.size)
        }
        println("Ending types seen: $types")
        assertTrue("ending variety: $types", types.size >= 4)
    }

    @Test fun planetStatsDriveEndings() {
        val s = fresh()
        val good = Planet("Eden B", 5.0, 1, 4, 1.0, 15, 60, 5, 2, listOf("Rings"), 0.3f)
        val bad = Planet("Hell C", 5.0, 0, 1, 2.2, 90, 0, 0, 0, listOf("Volcanic chains"), 0.1f)
        assertTrue(Endings.habScore(good) > Endings.habScore(bad) + 10)
        s.planet = good; val g = Engine.settle(s, 1L).ending!!
        s.planet = bad; val b = Engine.settle(s, 1L).ending!!
        assertNotEquals(g.id, b.id)
        assertEquals("silent", b.id)
    }

    @Test fun trustShapesAiFate() {
        val s = fresh(); s.trust = 5
        assertEquals("BELOVED ADVISOR", Endings.aiFate(s))
        s.trust = -5
        assertEquals("SCRAPPED FOR PARTS", Endings.aiFate(s))
    }

    @Test fun stateRoundTripsThroughJson() {
        val s = Engine.jump(fresh())
        val txt = JSON.encodeToString(GameState.serializer(), s)
        val back = JSON.decodeFromString(GameState.serializer(), txt)
        assertEquals(s, back)
    }

    @Test fun promptsAreBuiltFromRunDetails() {
        val s = Engine.jump(fresh())
        val ev = Events.byId(s.currentEvent!!)!!
        val img = Prompts.eventImage(s, ev, s.log.last())
        assertTrue(img.contains("1970s"))
        assertTrue(img.contains("Day ${s.day}"))
        assertTrue(Prompts.eventVideo(s, ev, null).contains("9:16"))
    }

    @Test fun voiceGetsFrazzled() {
        val s = fresh(); s.jumps = 1
        assertTrue(Voice.stress(s) <= 1)
        Sys.entries.forEach { s.health[it] = 10.0 }
        assertEquals(4, Voice.stress(s))
        assertEquals("nine hundred and eighty-seven", Voice.numberWords(987))
    }

    @Test fun relativisticTransitIsPhysical() {
        val t = Physics.transit(6.0, 0.8)
        assertEquals(0.8, t.beta, 1e-9)
        assertTrue("earth ${t.earthYears} must exceed light time", t.earthYears > 6.0)
        assertTrue("ship ${t.shipYears} < earth ${t.earthYears}", t.shipYears < t.earthYears)
        // Coast-phase dilation is γ = 1/0.6
        assertEquals(1.0 / 0.6, t.gamma, 1e-9)
        // A short hop never reaches cruise speed.
        val short = Physics.transit(0.3, 0.9)
        assertTrue(short.beta < 0.9)
        // Slower ships take longer in both frames.
        val slow = Physics.transit(6.0, 0.4)
        assertTrue(slow.earthYears > t.earthYears && slow.shipYears > t.shipYears)
    }

    @Test fun historyArtMatchesPlanetAndNeverRepeats() {
        fun art(p: Planet, trust: Int = 3): List<String> {
            val s = fresh(); s.planet = p; s.trust = trust; s.traits += Trait("archive", "A", "a", 1)
            return Engine.settle(s, 1L).ending!!.chapters.map { it.art }
        }
        val ocean = art(Planet("Thalassa B", 6.0, 1, 4, 1.0, 16, 94, 3, 2, listOf("Floating kelp mats"), 0.5f))
        assertEquals("land_ocean", ocean[0])
        val ice = art(Planet("Rime C", 6.0, 1, 3, 0.9, -45, 8, 4, 1, listOf("Glacier belts"), 0.5f))
        assertEquals("land_ice", ice[0])
        val desert = art(Planet("Ochre D", 6.0, 0, 4, 0.95, 28, 4, 4, 1, listOf("Rust deserts"), 0.5f))
        assertEquals("land_desert", desert[0])
        val toxic = art(Planet("Murk B", 6.0, 0, 2, 1.0, 12, 30, 5, 0, listOf("Canyon systems"), 0.5f))
        assertEquals("land_toxic", toxic[0])
        // Every plate in a book is distinct, and every key resolves to a bundled painting.
        val drawables = R.drawable::class.java.fields.map { it.name }.toSet()
        for (seed in 1L..200L) {
            val s = fresh(seed); s.planet = PlanetGen.generate(Random(seed), (seed % 6).toInt(), 8.0); s.trust = (seed % 9).toInt() - 4
            if (seed % 3 == 0L) s.traits += Trait("heralds", "H", "h", 1)
            val e = Engine.settle(s, 1L).ending!!
            val keys = e.chapters.map { it.art }
            assertEquals("seed $seed $keys", keys.size, keys.toSet().size)
            keys.forEach { assertTrue("missing art_$it", "art_$it" in drawables) }
        }
        Events.all.forEach { assertTrue("missing art_${it.art} for ${it.id}", "art_${it.art}" in drawables) }
        PROLOGUE_ART.forEach { assertTrue("art_$it" in drawables) }
    }

    @Test fun writingAvoidsOldJokesAndAmericanSpelling() {
        val text = Events.all.joinToString(" ") { e -> e.title + " " + e.choices.joinToString(" ") { it.title + " " + it.desc } }
        listOf("jump drive", "warp", "Warp", "color", "center ", "meter ", "lol").forEach { assertFalse("found '$it'", text.contains(it)) }
    }
}
