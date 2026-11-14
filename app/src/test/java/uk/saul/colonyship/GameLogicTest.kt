package uk.saul.colonyship

import org.junit.Assert.*
import org.junit.Test
import uk.saul.colonyship.game.*
import kotlin.random.Random

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

    @Test fun atLeastThirtyEventsAllWithTwoOrThreeChoices() {
        assertTrue("events=${Events.all.size}", Events.all.size >= 30)
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
        assertTrue(s.day >= 20)
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
}
