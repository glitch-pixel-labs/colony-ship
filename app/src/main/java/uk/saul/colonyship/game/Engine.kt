package uk.saul.colonyship.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class Specialist(val id: String, val name: String, val short: String, val role: String, val pronoun: String)

val SPECIALISTS = listOf(
    Specialist("okonkwo", "Chief Engineer Emeka Okonkwo", "Chief Okonkwo", "engineer", "he"),
    Specialist("raman", "Dr Priya Raman", "Dr Raman", "botanist", "she"),
    Specialist("varga", "Dr Ilse Varga", "Dr Varga", "physician", "she"),
    Specialist("wren", "Lieutenant Tomasz Wren", "Lt Wren", "pilot", "he"),
    Specialist("lindqvist", "Archivist Mae Lindqvist", "Archivist Lindqvist", "archivist", "she"),
)
fun specialist(id: String) = SPECIALISTS.first { it.id == id }

/** Effect context handed to event choices. Records human-readable deltas. */
class Fx(val s: GameState, val r: Random) {
    val deltas = mutableListOf<String>()
    private fun pct(d: Double) = (if (d >= 0) "+" else "−") + kotlin.math.abs(d).roundToInt() + "%"

    fun shieldFactor(): Double {
        val p = s.power[Channel.SHIELD] ?: 0
        return (s.h(Sys.SHIELD) / 100.0) * min(1.25, p / Channel.SHIELD.need.toDouble()) * 0.6
    }
    /** Damage a system. Hull damage is softened by shields when [shielded]. */
    fun dmg(sys: Sys, amount: Double, shielded: Boolean = sys == Sys.HULL) {
        var a = amount
        if (shielded) a *= (1.0 - shieldFactor())
        val before = s.h(sys)
        s.health[sys] = (before - a).coerceIn(0.0, 100.0)
        val d = s.h(sys) - before
        if (kotlin.math.abs(d) >= 0.5) deltas += "${sys.short} ${pct(d)}"
    }
    fun heal(sys: Sys, amount: Double) {
        val before = s.h(sys); s.health[sys] = (before + amount).coerceIn(0.0, 100.0)
        val d = s.h(sys) - before
        if (d >= 0.5) deltas += "${sys.short} ${pct(d)}"
    }
    /** Spend (negative) or gain (positive) reserve power; shortfall browns out systems. */
    fun reserve(d: Double) {
        if (d >= 0) { s.reserve = min(100.0, s.reserve + d); deltas += "+${d.roundToInt()} POWER"; return }
        val need = -d
        if (s.reserve >= need) { s.reserve -= need; deltas += "−${need.roundToInt()} POWER"; return }
        val short = need - s.reserve
        s.reserve = 0.0
        deltas += "−${need.roundToInt()} POWER"
        deltas += "BROWNOUT"
        dmg(listOf(Sys.LIFE, Sys.ENGINE, Sys.CRYO, Sys.SCAN).random(r), short * 0.6, shielded = false)
    }
    private fun cons(name: String, get: () -> Double, set: (Double) -> Unit, d: Double) {
        val before = get(); set((before + d).coerceIn(0.0, 100.0))
        val dd = get() - before
        if (kotlin.math.abs(dd) >= 0.5) deltas += "$name ${pct(dd)}"
    }
    fun air(d: Double) = cons("AIR", { s.air }, { s.air = it }, d)
    fun food(d: Double) = cons("FOOD", { s.food }, { s.food = it }, d)
    fun water(d: Double) = cons("WATER", { s.water }, { s.water = it }, d)
    fun spares(d: Double) = cons("SPARES", { s.spares }, { s.spares = it }, d)
    fun trust(d: Int) { s.trust = (s.trust + d).coerceIn(-6, 6); if (d != 0) deltas += "TRUST ${if (d > 0) "+" else "−"}${kotlin.math.abs(d)}" }
    fun trait(id: String, label: String, detail: String) {
        if (!s.hasTrait(id)) { s.traits += Trait(id, label, detail, s.day); deltas += "TRAIT: $label" }
    }
    fun wake(id: String) {
        if (id !in s.awake) { s.awake += id; deltas += "+1 AWAKE"; air(-3.0); food(-3.0) }
    }
    fun sleep(id: String) { if (s.awake.remove(id)) deltas += "−1 AWAKE" }
    fun awake(id: String) = id in s.awake
    fun chance(p: Double) = r.nextDouble() < p
    fun days(d: Int) { s.day = max(0, s.day + d); if (d != 0) deltas += "${if (d > 0) "+" else "−"}${kotlin.math.abs(d)} DAYS" }

    /** Stable pods start failing. */
    fun failPods(n: Int) {
        val idx = s.pods.indices.filter { s.pods[it] == 'S' }.shuffled(r).take(n)
        if (idx.isEmpty()) return
        val c = s.pods.toCharArray(); idx.forEach { c[it] = 'F' }; s.pods = String(c)
        deltas += "${idx.size} ${if (idx.size == 1) "POD" else "PODS"} FAILING"
    }
    /** Lose pods: failing ones first, then stable. */
    fun killPods(n: Int) {
        if (n <= 0) return
        val c = s.pods.toCharArray()
        val order = c.indices.filter { c[it] == 'F' }.shuffled(r) + c.indices.filter { c[it] == 'S' }.shuffled(r)
        val take = order.take(n); take.forEach { c[it] = 'L' }; s.pods = String(c)
        if (take.isNotEmpty()) { deltas += "−${take.size} ${if (take.size == 1) "POD" else "PODS"}"; s.lostToday += take.size }
    }
    fun savePods(n: Int) {
        val c = s.pods.toCharArray()
        val take = c.indices.filter { c[it] == 'F' }.shuffled(r).take(n); take.forEach { c[it] = 'S' }; s.pods = String(c)
        if (take.isNotEmpty()) deltas += "${take.size} ${if (take.size == 1) "POD" else "PODS"} STABILISED"
    }
}

object Engine {
    const val JUMP_COST = 12.0
    const val RESCAN_COST = 6.0
    const val MAX_PASSES = 5

    fun rng(s: GameState, salt: Int) = Random(s.seed * 1_000_003L + s.jumps * 7919L + s.day * 31L + salt)

    fun newGame(runNumber: Int, seed: Long, now: Long): GameState {
        val s = GameState(runId = "run-$seed", runNumber = runNumber, seed = seed, startedAt = now)
        val r = Random(seed)
        s.day = 0
        s.nextStarLy = 5.0 + r.nextDouble() * 6
        // A few pods have already had a rough departure.
        Fx(s, r).failPods(2 + r.nextInt(4))
        s.lastJumpReport = emptyList()
        return s
    }

    /** Set a channel, clamping so the total never exceeds the reactor budget. */
    fun setPower(src: GameState, ch: Channel, value: Int): GameState {
        val s = src.deepCopy()
        val others = s.power.filterKeys { it != ch }.values.sum()
        s.power[ch] = value.coerceIn(0, POWER_BUDGET - others)
        return s
    }

    fun efficiency(s: GameState, ch: Channel, sys: Sys): Double =
        (s.h(sys) / 100.0) * min(1.3, (s.power[ch] ?: 0) / ch.need.toDouble())

    /** Estimated days of air/water left at current burn. */
    fun daysLeft(s: GameState): Int {
        val lifeEff = efficiency(s, Channel.LIFE, Sys.LIFE)
        val perJumpAir = (2.2 + 1.8 * s.awake.size) * (1.7 - min(1.2, lifeEff))
        val perJumpWater = (1.8 + 1.2 * s.awake.size) * (1.7 - min(1.2, lifeEff))
        val jumpDays = travelDays(s)
        val j = min(s.air / max(0.1, perJumpAir), s.water / max(0.1, perJumpWater))
        return (j * jumpDays).roundToInt()
    }

    fun travelDays(s: GameState): Int {
        val eng = 0.45 + 0.55 * min(1.25, efficiency(s, Channel.ENGINE, Sys.ENGINE))
        val wren = if ("wren" in s.awake) 0.85 else 1.0
        return max(20, (s.nextStarLy * 9.0 / eng * wren).roundToInt())
    }

    /** Engage the jump drive: simulate the trip, then queue an event. */
    fun jump(src: GameState): GameState {
        val s = src.deepCopy()
        val r = rng(s, 1)
        val fx = Fx(s, r)
        s.lostToday = 0
        fx.reserve(-JUMP_COST)
        val days = travelDays(s)
        s.day += days
        s.jumps += 1
        fx.deltas += "+$days DAYS"

        // Reactor surplus trickles into the reserve.
        val surplus = (POWER_BUDGET - s.drawn()).coerceAtLeast(0)
        s.reserve = min(100.0, s.reserve + surplus * 0.6 + 4)

        // Consumables
        val lifeEff = efficiency(s, Channel.LIFE, Sys.LIFE)
        val awakeN = s.awake.size
        fx.air(-(2.2 + 1.8 * awakeN) * (1.7 - min(1.2, lifeEff)))
        fx.water(-(1.8 + 1.2 * awakeN) * (1.7 - min(1.2, lifeEff)))
        fx.food(-(1.0 + 2.2 * awakeN) + if ("raman" in s.awake) 4.0 else 0.0)

        // Repairs consume spares and patch the weakest systems.
        var repairPts = (s.power[Channel.REPAIR] ?: 0) * 0.9 * (if ("okonkwo" in s.awake) 1.6 else 1.0)
        val spareCost = repairPts * 0.3
        if (spareCost > s.spares) repairPts *= if (spareCost == 0.0) 0.0 else s.spares / spareCost
        if (repairPts > 0) {
            fx.spares(-min(s.spares, spareCost))
            val weakest = Sys.entries.sortedBy { s.h(it) }.take(2)
            weakest.forEach { s.health[it] = min(100.0, s.h(it) + repairPts / 2) }
            fx.savePods((repairPts / 5).toInt())
        }

        // Wear and tear, worse when under-powered.
        val map = mapOf(Sys.LIFE to Channel.LIFE, Sys.SCAN to Channel.SCAN, Sys.SHIELD to Channel.SHIELD, Sys.ENGINE to Channel.ENGINE)
        for (sys in Sys.entries) {
            var wear = 0.6 + r.nextDouble() * 2.2
            map[sys]?.let { ch -> val p = s.power[ch] ?: 0; if (p < ch.need) wear += (ch.need - p) * 0.4 }
            if (sys == Sys.CRYO && lifeEff < 0.7) wear += (0.7 - lifeEff) * 10
            if (sys == Sys.HULL) wear *= (1.3 - fx.shieldFactor())
            s.health[sys] = (s.h(sys) - wear).coerceIn(0.0, 100.0)
        }

        // Cryopods
        val cryo = s.h(Sys.CRYO)
        val newFail = ((100 - cryo) / 100.0 * 16 + (if (lifeEff < 0.6) 7 else 0) + r.nextInt(4)).roundToInt()
        val lossP = if ("varga" in s.awake) 0.22 else 0.42
        val failingNow = s.pods.indices.filter { s.pods[it] == 'F' }
        val toLose = failingNow.count { r.nextDouble() < lossP }
        fx.killPods(toLose)
        fx.failPods(newFail)
        if (s.air <= 0.5) { fx.killPods(30 + r.nextInt(30)); fx.deltas += "AIR EXHAUSTED" }
        if (s.water <= 0.5) { fx.killPods(15 + r.nextInt(15)); fx.deltas += "WATER EXHAUSTED" }
        if (s.food <= 0.5) { fx.killPods(8 + r.nextInt(10)); fx.deltas += "FOOD EXHAUSTED" }
        if (cryo <= 1) fx.killPods(60)

        s.lastJumpReport = fx.deltas.toList()
        s.planet = null
        if (deadCheck(s)) return s

        val ev = Events.pick(s, r)
        s.currentEvent = ev.id
        s.seenEvents += ev.id
        s.lastOutcome = null
        s.lastDeltas = emptyList()
        s.phase = Phase.EVENT
        s.log += LogEntry(id = "e${s.jumps}-${ev.id}", day = s.day, eventId = ev.id, title = ev.title, kicker = ev.kicker, art = ev.art)
        return s
    }

    fun deadCheck(s: GameState): Boolean {
        if (s.alive() == 0 || s.h(Sys.HULL) <= 0.0) {
            s.ending = Endings.lostInSpace(s)
            s.phase = Phase.ENDED
            s.endedAt = System.currentTimeMillis()
            return true
        }
        return false
    }

    fun choose(src: GameState, index: Int): GameState {
        val s = src.deepCopy()
        val ev = Events.byId(s.currentEvent ?: return s) ?: return s
        val choices = ev.available(s)
        val c = choices.getOrNull(index) ?: return s
        val fx = Fx(s, rng(s, 2 + index))
        s.lostToday = 0
        val text = c.effect(fx)
        s.lastOutcome = text
        s.lastDeltas = fx.deltas.toList()
        val i = s.log.indexOfLast { it.eventId == ev.id }
        if (i >= 0) s.log[i] = s.log[i].copy(choice = c.title, outcome = text)
        s.phase = Phase.OUTCOME
        deadCheck(s)
        return s
    }

    /** After an event, arrive at the next star and run the first survey pass. */
    fun arrive(src: GameState): GameState {
        val s = src.deepCopy()
        val r = rng(s, 3)
        s.currentEvent = null
        val p = PlanetGen.generate(r, s.jumps, s.nextStarLy)
        s.planet = scanPass(s, p, r)
        s.nextStarLy = 4.0 + r.nextDouble() * 8
        s.phase = Phase.ORBIT
        return s
    }

    fun scanAccuracy(s: GameState, passes: Int): Double {
        val p = (s.power[Channel.SCAN] ?: 0) / Channel.SCAN.need.toDouble()
        return ((s.h(Sys.SCAN) / 100.0) * (0.6 + 0.4 * min(1.4, p)) + passes * 0.07).coerceIn(0.0, 1.0)
    }

    fun scanPass(s: GameState, p: Planet, r: Random): Planet {
        val acc = scanAccuracy(s, p.passes)
        val newObs = PlanetGen.ROWS.associateWith { row ->
            val trueV = PlanetGen.trueValue(p, row)
            val damaged = r.nextDouble() < (0.78 - acc) * 1.1
            val err = (1.0 - acc) * 0.6
            val noisy = trueV + (r.nextDouble() * 2 - 1) * err * PlanetGen.span(row)
            Observation(!damaged, noisy, err)
        }
        val merged = PlanetGen.ROWS.associateWith { row ->
            val old = p.obs[row]; val nw = newObs.getValue(row)
            when {
                old == null -> nw
                !old.ok -> nw
                !nw.ok -> old
                nw.error < old.error -> nw
                else -> old
            }
        }
        return p.copy(obs = merged, passes = p.passes + 1)
    }

    fun rescan(src: GameState): GameState {
        val s = src.deepCopy()
        val p = s.planet ?: return s
        if (p.passes >= MAX_PASSES) return s
        Fx(s, rng(s, 4)).reserve(-RESCAN_COST)
        s.planet = scanPass(s, p, rng(s, 5 + p.passes))
        return s
    }

    fun settle(src: GameState, now: Long): GameState {
        val s = src.deepCopy()
        s.ending = Endings.build(s, rng(s, 9))
        s.phase = Phase.ENDED
        s.endedAt = now
        return s
    }

    fun attachToLog(src: GameState, logId: String, path: String): GameState {
        val s = src.deepCopy()
        val i = s.log.indexOfFirst { it.id == logId }
        if (i >= 0) s.log[i] = s.log[i].copy(imagePath = path)
        return s
    }
}
