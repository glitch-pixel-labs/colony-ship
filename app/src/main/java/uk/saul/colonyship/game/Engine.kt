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
        if (a >= 5.0) {
            val crossed = (before >= 70 && s.h(sys) < 70) || (before >= 40 && s.h(sys) < 40)
            if (crossed || a >= 8.0) note(sys, "${sys.label} damaged", Engine.consequence(sys, s.h(sys)))
        }
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
        if (id !in s.awake) {
            s.awake += id; deltas += "+1 AWAKE"; air(-3.0); food(-3.0)
            val sp = specialist(id)
            setFate(id, "WOKEN", "${sp.short} was thawed and came on duty.")
        }
    }
    fun setFate(id: String, status: String, detail: String) {
        s.fates.removeAll { it.id == id && it.status == status && it.detail == detail }
        s.fates += CrewFate(id, status, detail, s.day, Physics.earthYear(s))
        if (status == "DEAD" || status == "MISSING") {
            s.awake.remove(id)
            deltas += "${specialist(id).short.uppercase()} · $status"
        }
    }
    fun killCrew(id: String, detail: String) {
        if (id !in s.awake && s.fateOf(id)?.status == "DEAD") return
        setFate(id, "DEAD", detail)
    }
    fun fuel(d: Double) {
        val before = s.fuel
        s.fuel = (s.fuel + d).coerceIn(0.0, 100.0)
        val dd = s.fuel - before
        if (kotlin.math.abs(dd) >= 0.5) deltas += "FUEL ${if (dd >= 0) "+" else "−"}${kotlin.math.abs(dd).roundToInt()}"
    }
    fun note(sys: Sys, title: String, consequence: String) {
        s.damageLog.add(0, DamageNote(s.day, Physics.earthYear(s), sys.name, title, consequence))
        if (s.damageLog.size > 24) s.damageLog.subList(24, s.damageLog.size).clear()
    }
    fun declareCrisis(kind: String) {
        s.crisis = kind
        s.crisisLeft = 1
        val title = Engine.crisisTitle(kind)
        trait("forced-landfall", "FORCED LANDFALL", "A $title left no choice but to land at the next world, or the one after.")
        note(
            when (kind) {
                "reactor" -> Sys.CORE
                "cryo" -> Sys.CRYO
                "hull" -> Sys.HULL
                else -> Sys.LIFE
            },
            title,
            "The ship will not survive a long voyage. Settle at the next world, or risk one more burn."
        )
        deltas += "FORCED LANDFALL"
    }

    /**
     * Start (or hurry) a delayed payoff from alien data or a signal.
     * Only one reduction runs at a time; new material shortens the wait.
     */
    fun startInsight(kind: String, payload: String, jumps: Int = 2) {
        if (s.insight != null) {
            s.insightLeft = minOf(s.insightLeft, 1)
            deltas += "DATABANK · REDUCTION HURRIED"
            return
        }
        s.insight = kind
        s.insightPayload = payload
        s.insightLeft = jumps.coerceIn(1, 3)
        deltas += "DATABANK · REDUCTION STARTED"
    }
    fun sleep(id: String) { if (s.awake.remove(id)) deltas += "−1 AWAKE" }
    fun awake(id: String) = id in s.awake
    fun chance(p: Double) = r.nextDouble() < p
    /** Time passes equally on both clocks when the ship is not moving relativistically. */
    fun days(d: Int) { s.day = max(0, s.day + d); s.earthDays = max(0.0, s.earthDays + d); if (d != 0) deltas += "${if (d > 0) "+" else "−"}${kotlin.math.abs(d)} DAYS" }

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

data class BurnRisk(val level: Int, val label: String, val detail: String, val fuelCost: Double, val canBurn: Boolean)

object Engine {
    const val JUMP_COST = 12.0
    const val RESCAN_COST = 6.0
    const val MAX_PASSES = 5
    /** Propellant for boost+brake toward KIC 8462852. The long coast is scoop-fed. */
    const val TABBY_FUEL = 22.0

    fun rng(s: GameState, salt: Int) = Random(s.seed * 1_000_003L + s.jumps * 7919L + s.day * 31L + salt)

    fun newGame(runNumber: Int, seed: Long, now: Long): GameState {
        val s = GameState(runId = "run-$seed", runNumber = runNumber, seed = seed, startedAt = now)
        val r = Random(seed)
        s.day = 0
        s.fuel = 100.0
        s.nextStarLy = 3.1 + r.nextDouble() * 2.3
        s.route += ChartStop(
            id = "sol", name = "Sol", x = 0.0, y = 0.0, z = 0.0, ly = 0.0,
            earthYear = Physics.LAUNCH_YEAR, shipYears = 0.0, kind = "sol",
            note = "Launch from Earth–Moon L1 in ${Physics.LAUNCH_YEAR}. One thousand sleepers, one machine, no way back."
        )
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

    /** Estimated number of further transits the air and water will last at the current settings. */
    fun hopsLeft(s: GameState): Int {
        val lifeEff = efficiency(s, Channel.LIFE, Sys.LIFE)
        val perJumpAir = (2.2 + 1.8 * s.awake.size) * (1.7 - min(1.2, lifeEff))
        val perJumpWater = (1.8 + 1.2 * s.awake.size) * (1.7 - min(1.2, lifeEff))
        val byLife = min(s.air / max(0.1, perJumpAir), s.water / max(0.1, perJumpWater)).toInt()
        val typ = fuelCost(s.copy(course = null))
        val byFuel = (s.fuel / max(0.1, typ)).toInt()
        return min(byLife, byFuel)
    }

    /** Propellant for the next boost–brake. Long coasts are scoop-fed and do not drain the tanks. */
    fun fuelCost(s: GameState): Double {
        if (s.course == "tabby") return TABBY_FUEL
        val eng = min(1.15, efficiency(s, Channel.ENGINE, Sys.ENGINE))
        return ((6.5 + s.nextStarLy * 1.2) / (0.70 + 0.45 * eng)).coerceIn(7.5, 15.5)
    }

    fun canBurn(s: GameState): Boolean =
        s.fuel + 0.05 >= fuelCost(s) && s.h(Sys.HULL) > 0.0 && s.alive() > 0 && s.phase != Phase.ENDED

    /** A legal departure: enough fuel, and not past the last allowed burn of a crisis. */
    fun canDepart(s: GameState): Boolean = canBurn(s) && (s.crisis == null || s.crisisLeft > 0)

    fun crisisTitle(kind: String?): String = when (kind) {
        "reactor" -> "Reactor containment breach"
        "cryo" -> "Cascading cryo failure"
        "hull" -> "Structural fatigue — hull breach"
        "coolant" -> "Primary coolant loss"
        else -> "Catastrophic failure"
    }

    fun crisisBanner(s: GameState): Pair<String, String> {
        val title = crisisTitle(s.crisis).uppercase()
        val body = if (s.crisisLeft > 0)
            "Settle at this world, or risk one more burn. Another transit may kill the colony, or the ship."
        else
            "No further burn is possible. Land here, or lose the ship."
        return title to body
    }

    fun plannedDistance(s: GameState): Double =
        if (s.course == "tabby") Physics.remainingToTabby(s) else s.nextStarLy

    fun stakeFactor(s: GameState): Double = (1.0 + 0.11 * s.jumps + 0.02 * Physics.shipYears(s)).coerceAtMost(3.2)

    fun burnRisk(s: GameState): BurnRisk {
        val cost = fuelCost(s)
        val after = s.fuel - cost
        val hull = s.h(Sys.HULL)
        val worst = Sys.entries.minOf { s.h(it) }
        var level = 1
        val bits = mutableListOf<String>()
        if (s.jumps >= 3) { level++; bits += "wear rising with each burn" }
        if (s.jumps >= 6) { level++; bits += "the ship is ageing" }
        if (s.jumps >= 9) { level++; bits += "margins almost gone" }
        if (hull < 55) { level++; bits += "hull ${hull.roundToInt()}%" }
        if (worst < 40) { level++; bits += "a system is in the red" }
        if (after < 18) { level++; bits += "fuel after burn ${after.coerceAtLeast(0.0).roundToInt()}" }
        if (after < 8) { level += 1; bits += "may not have a burn after this one" }
        if (s.course == "tabby") { level = max(level, 5); bits += "1,470 ly detour to KIC 8462852 — centuries of coast" }
        if (s.course == "lead") { level = max(level, 3); bits += "diverting to the recommended coordinates" }
        if (s.crisis != null) {
            level = max(level, if (s.crisisLeft <= 0) 5 else 4)
            bits += if (s.crisisLeft <= 0) "forced landfall — no legal burn remains" else "forced landfall — this may be the last legal burn"
        }
        level = level.coerceIn(1, 5)
        if (!canDepart(s)) {
            val why = when {
                s.crisis != null && s.crisisLeft <= 0 -> "Forced landfall. The ${crisisTitle(s.crisis).lowercase()} leaves no further legal burn."
                s.fuel + 0.05 < cost -> "Not enough fuel for the boost and brake (${cost.roundToInt()} needed, ${s.fuel.roundToInt()} in the tanks)."
                s.h(Sys.HULL) <= 0 -> "The hull would not survive a burn."
                s.alive() <= 0 -> "There is no one left to carry."
                else -> "The ship cannot burn."
            }
            return BurnRisk(5, "NO BURN", why, cost, false)
        }
        val label = listOf("", "LOW", "MODERATE", "ELEVATED", "HIGH", "SEVERE")[level]
        val detail = bits.distinct().joinToString("; ").ifBlank { "Systems nominal for a transit of ${"%.1f".format(plannedDistance(s))} ly." }.replaceFirstChar { it.uppercase() }
        return BurnRisk(level, label, detail, cost, true)
    }

    fun consequence(sys: Sys, health: Double): String {
        val bad = health < 40
        return when (sys) {
            Sys.SCAN -> if (bad) "Survey readouts will be patchy or missing." else "Scanner accuracy is reduced."
            Sys.ENGINE -> if (bad) "Cruise speed drops and each burn costs more fuel." else "The drive is less efficient."
            Sys.HULL -> if (bad) "A structural failure in transit is now a real risk; landings will kill more people." else "The hull is weaker; dust and burns will cut deeper."
            Sys.SHIELD -> if (bad) "The bow is almost unprotected; hull wear will climb sharply." else "The shields will take less of the interstellar medium."
            Sys.LIFE -> if (bad) "Air and water will run down faster, and more pods will fail." else "Life support is drawing more from the reserves."
            Sys.CRYO -> if (bad) "Many more pods will fail on the next transit." else "The array is less stable; expect more failing pods."
            Sys.CORE -> if (bad) "Reserve power will be unreliable; brownouts are likely." else "The reactor is off its best output."
            Sys.LANDER -> if (bad) "A controlled landing will be difficult; expect heavy losses at landfall." else "The landing bays and fabricators are degraded."
            Sys.DATA -> if (bad) "The archive is damaged; the colony will inherit less of Earth." else "Parts of the databank are corrupted."
        }
    }

    /** Cruise speed as a fraction of c. Healthy, well-fed engines reach about 0.82c; a pilot trims the burn. */
    fun cruiseBeta(s: GameState): Double {
        val eng = min(1.15, efficiency(s, Channel.ENGINE, Sys.ENGINE))
        val wren = if ("wren" in s.awake) 0.04 else 0.0
        return (0.40 + 0.42 * eng + wren).coerceIn(0.30, 0.90)
    }

    fun plannedTransit(s: GameState): Physics.Transit = Physics.transit(plannedDistance(s), cruiseBeta(s))

    /** Ship-days for the next transit (kept for UI and tests). */
    fun travelDays(s: GameState): Int = plannedTransit(s).shipDays

    /** Light the drive: simulate the relativistic transit, then queue an event. */
    fun jump(src: GameState): GameState {
        val s = src.deepCopy()
        val r = rng(s, 1)
        val fx = Fx(s, r)
        s.lostToday = 0
        val toTabby = s.course == "tabby"
        val cost = fuelCost(s)
        if (!canDepart(s)) {
            // Called in error, or a crisis has closed the window. Do not start a burn.
            if (s.crisis != null && s.crisisLeft <= 0) {
                s.ending = Endings.lostInSpace(s)
                s.phase = Phase.ENDED
                s.endedAt = System.currentTimeMillis()
                fx.deltas += "FORCED LANDFALL IGNORED"
                s.lastJumpReport = fx.deltas.toList()
                return s
            }
            return src
        }
        fx.reserve(-JUMP_COST)
        fx.fuel(-cost)
        val t = plannedTransit(s)
        s.prevDay = s.day; s.prevEarthDays = s.earthDays
        s.day += t.shipDays
        s.earthDays += t.earthDays
        s.lastBeta = t.beta
        if (toTabby) Physics.placeAtTabby(s) else Physics.advance(s, t.distanceLy, r)
        s.jumps += 1
        fx.deltas += "%.2fc PEAK".format(t.beta)
        fx.deltas += "+%.1f SHIP YRS".format(t.shipYears)
        fx.deltas += "+%.1f EARTH YRS".format(t.earthYears)
        if (toTabby) fx.deltas += "ARRIVAL · KIC 8462852"

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

        // Wear and tear, worse when under-powered, and rising with each jump and with ship age.
        val stake = stakeFactor(s) * (if (toTabby) 7.0 else 1.0)
        val map = mapOf(Sys.LIFE to Channel.LIFE, Sys.SCAN to Channel.SCAN, Sys.SHIELD to Channel.SHIELD, Sys.ENGINE to Channel.ENGINE)
        for (sys in Sys.entries) {
            val before = s.h(sys)
            var wear = (0.8 + r.nextDouble() * 2.4) * stake
            map[sys]?.let { ch -> val pwr = s.power[ch] ?: 0; if (pwr < ch.need) wear += (ch.need - pwr) * 0.45 * stake }
            if (sys == Sys.CRYO && lifeEff < 0.7) wear += (0.7 - lifeEff) * 10 * stake
            if (sys == Sys.HULL) wear *= (1.3 - fx.shieldFactor()) * (0.7 + 0.5 * t.beta)
            if (sys == Sys.CORE) wear += 0.4 * stake
            if (sys == Sys.LANDER) wear += 0.3 * stake
            if (sys == Sys.DATA) wear += 0.25 * stake
            s.health[sys] = (s.h(sys) - wear).coerceIn(0.0, 100.0)
            if (before >= 70 && s.h(sys) < 70 || before >= 40 && s.h(sys) < 40) {
                fx.note(sys, "${sys.label} degraded in transit", consequence(sys, s.h(sys)))
            }
        }

        // Cryopods — failure chance rises with jump count and with a long coast.
        val cryo = s.h(Sys.CRYO)
        val newFail = (((100 - cryo) / 100.0 * 16 + (if (lifeEff < 0.6) 7 else 0) + r.nextInt(4)) * (0.85 + 0.08 * s.jumps) * (if (toTabby) 2.8 else 1.0)).roundToInt()
        val lossP = (if ("varga" in s.awake) 0.22 else 0.42) * (0.9 + 0.06 * s.jumps) * (if (toTabby) 1.5 else 1.0)
        val failingNow = s.pods.indices.filter { s.pods[it] == 'F' }
        val toLose = failingNow.count { r.nextDouble() < lossP.coerceAtMost(0.85) }
        fx.killPods(toLose)
        fx.failPods(newFail)
        if (toTabby) fx.failPods(20 + r.nextInt(30))
        if (s.air <= 0.5) { fx.killPods(30 + r.nextInt(30)); fx.deltas += "AIR EXHAUSTED" }
        if (s.water <= 0.5) { fx.killPods(15 + r.nextInt(15)); fx.deltas += "WATER EXHAUSTED" }
        if (s.food <= 0.5) { fx.killPods(8 + r.nextInt(10)); fx.deltas += "FOOD EXHAUSTED" }
        if (cryo <= 1) fx.killPods(60)

        if (s.crisis != null) {
            applyCrisisTransit(s, fx, r)
            s.crisisLeft = (s.crisisLeft - 1).coerceAtLeast(0)
            fx.deltas += "CRISIS BURN · ${s.crisisLeft} LEFT"
        }

        s.lastJumpReport = fx.deltas.toList()
        s.planet = null
        if (deadCheck(s)) return s

        // Databank reduction of alien material, if any, ticks down with each coast.
        if (s.insight != null && s.insightLeft > 0) {
            s.insightLeft -= 1
            fx.reserve(-3.0)
            fx.heal(Sys.DATA, 2.0)
            fx.deltas += "DATABANK · REDUCTION"
        }
        val toLead = s.course == "lead"
        if (toLead) {
            s.leadWorld = s.insightPayload
            s.course = null
            fx.trait("lead-course", "THE RECOMMENDED WORLD", "Diverted to coordinates found in a non-human archive")
            fx.deltas += "ARRIVAL · RECOMMENDED COORDINATES"
        }

        val ev = when {
            toTabby -> {
                s.course = null
                fx.trait("tabby-visited", "BOYAJIAN'S STAR", "Reached KIC 8462852")
                Events.byId("tabby_swarm") ?: Events.pick(s, r)
            }
            s.insight != null && s.insightLeft <= 0 -> {
                val id = when (s.insight) {
                    "world" -> "decode_world"
                    "tech" -> "decode_tech"
                    "warning" -> "decode_warn"
                    "reply" -> "signal_reply"
                    else -> "decode_tech"
                }
                Events.byId(id) ?: Events.pick(s, r)
            }
            else -> Events.pick(s, r)
        }
        if (ev.id.startsWith("crisis_") && s.hasTrait("archive-warning") && !s.hasTrait("warning-honoured") && !s.hasTrait("warning-ignored")) {
            fx.trait("warning-ignored", "THE WARNING WE FILED", "A failure the archive had described arrived, and we had not prepared.")
        }
        s.currentEvent = ev.id
        s.seenEvents += ev.id
        s.lastOutcome = null
        s.lastDeltas = emptyList()
        s.phase = Phase.EVENT
        s.log += LogEntry(id = "e${s.jumps}-${ev.id}", day = s.day, eventId = ev.id, title = ev.title, kicker = ev.kicker, art = ev.art,
            earthYear = Physics.earthYear(s), ly = Physics.distanceFromSol(s))
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
        val p = when (val lw = s.leadWorld) {
            "paradise", "grave", "alien" -> {
                s.leadWorld = null
                PlanetGen.generateLead(r, lw, Physics.distanceFromSol(s))
            }
            else -> PlanetGen.generate(r, s.jumps, Physics.distanceFromSol(s))
        }
        // Braking into orbit and the first survey take weeks, at sub-relativistic speed.
        val settleIn = 30 + r.nextInt(40)
        s.day += settleIn; s.earthDays += settleIn
        s.planet = scanPass(s, p, r)
        s.nextStarLy = 2.4 + r.nextDouble() * 3.4
        s.phase = Phase.ORBIT
        recordArrival(s)
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
        s.day += 6; s.earthDays += 6
        s.planet = scanPass(s, p, rng(s, 5 + p.passes))
        return s
    }

    fun settle(src: GameState, now: Long): GameState {
        val s = src.deepCopy()
        // Anyone still awake is resolved in the landing.
        s.awake.toList().forEach { id ->
            if (s.fateOf(id)?.status !in setOf("DEAD", "MISSING")) {
                val sp = specialist(id)
                s.fates += CrewFate(id, "SETTLED", "${sp.short} walked down the ramp at landfall and lived among the colonists.", s.day, Physics.earthYear(s))
            }
        }
        s.course = null
        if (s.route.isNotEmpty()) {
            val last = s.route.last()
            val extra = if (s.crisis != null) " Forced landfall: ${crisisTitle(s.crisis).lowercase()}." else " The colony was founded here."
            s.route[s.route.lastIndex] = last.copy(kind = "settled", note = last.note.trim() + extra)
        }
        s.ending = Endings.build(s, rng(s, 9))
        s.phase = Phase.ENDED
        s.endedAt = now
        return s
    }

    private fun applyCrisisTransit(s: GameState, fx: Fx, r: Random) {
        when (s.crisis) {
            "reactor" -> {
                fx.dmg(Sys.CORE, 22.0 + r.nextDouble() * 14); fx.dmg(Sys.LIFE, 10.0); fx.dmg(Sys.ENGINE, 8.0, false)
                fx.killPods(40 + r.nextInt(70))
                if (r.nextDouble() < 0.16) s.health[Sys.CORE] = 0.0
            }
            "cryo" -> {
                fx.dmg(Sys.CRYO, 28.0 + r.nextDouble() * 16); fx.failPods(80 + r.nextInt(80)); fx.killPods(90 + r.nextInt(90))
            }
            "hull" -> {
                fx.dmg(Sys.HULL, 18.0 + r.nextDouble() * 16); fx.dmg(Sys.SHIELD, 10.0); fx.killPods(30 + r.nextInt(50))
                if (r.nextDouble() < 0.14) s.health[Sys.HULL] = 0.0
            }
            "coolant" -> {
                fx.dmg(Sys.LIFE, 16.0); fx.dmg(Sys.CRYO, 18.0); fx.dmg(Sys.CORE, 10.0, false)
                fx.killPods(50 + r.nextInt(60)); fx.failPods(40 + r.nextInt(40))
            }
        }
    }

    fun recordArrival(s: GameState) {
        val p = s.planet
        val ev = s.log.lastOrNull()
        val evId = ev?.eventId
        val kind = when {
            evId == "tabby_swarm" || (s.hasTrait("tabby-visited") && evId == "tabby_swarm") -> "tabby"
            p?.kind in setOf("paradise", "grave", "alien-garden") || s.hasTrait("lead-course") && p != null && s.route.none { it.kind == "lead" } -> "lead"
            evId != null && evId.startsWith("crisis_") -> "crisis"
            evId != null && evId in NOTABLE_SIGNALS -> "signal"
            evId != null && evId.startsWith("decode_") -> "signal"
            else -> "system"
        }
        val name = when {
            kind == "tabby" -> "KIC 8462852"
            kind == "lead" -> p?.name ?: "Recommended world"
            else -> p?.name ?: "Unnamed system"
        }
        val note = buildString {
            if (kind == "tabby") append("Arrival at Boyajian’s Star, 1,470 ly from Sol in Cygnus. ")
            if (p != null) append("${p.name}: ${p.quality.lowercase()} world (${p.kind}). ")
            if (ev != null) {
                append("In transit: ${ev.title}")
                if (ev.choice.isNotBlank()) append(" — ${ev.choice}")
                append(".")
            }
            if (s.crisis != null) append(" ${crisisTitle(s.crisis)}. ${s.crisisLeft} legal burn${if (s.crisisLeft == 1) "" else "s"} remain.")
        }.trim()
        s.route += ChartStop(
            id = "j${s.jumps}-${kind}",
            name = name,
            x = s.posX, y = s.posY, z = s.posZ,
            ly = Physics.distanceFromSol(s),
            earthYear = Physics.earthYear(s),
            shipYears = Physics.shipYears(s),
            kind = kind,
            note = note,
        )
    }

    val NOTABLE_SIGNALS = setOf(
        "signal", "derelict", "wow_burst", "maser_tick", "occulter", "ratio_chirps",
        "hydrogen_hymn", "wound_clock", "faraday_grid", "inscribed_ice", "missing_lens",
        "isotope_wake", "local_void", "edge_cloud", "tabby_beacon", "organics",
    )

    fun attachToLog(src: GameState, logId: String, path: String): GameState {
        val s = src.deepCopy()
        val i = s.log.indexOfFirst { it.id == logId }
        if (i >= 0) s.log[i] = s.log[i].copy(imagePath = path)
        return s
    }
}
