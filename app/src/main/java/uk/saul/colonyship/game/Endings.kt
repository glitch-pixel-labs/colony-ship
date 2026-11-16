package uk.saul.colonyship.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Builds the colony's history book from the planet, the journey and the AI's standing.
 * All prose is original to Colony Ship. The tone is hard science fiction: people die of real things,
 * and when they win, they win by work.
 */
object Endings {

    fun habScore(p: Planet): Double {
        var h = listOf(-3.5, -3.5, -1.5, 0.5, 2.0, 1.5, -4.0)[p.atmosphere.coerceIn(0, 6)]
        h += when (p.quality) { "EARTHLIKE" -> 3.0; "PROMISING" -> 1.2; "HELLISH" -> -5.0; "HOSTILE" -> -2.2; else -> 0.0 }
        h += when (p.kind) {
            "furnace", "crushing", "radiation", "rogue" -> -2.0
            "acid", "vacuum" -> -1.5
            "earthlike" -> 1.0
            else -> 0.0
        }
        val dg = abs(p.gravity - 1.0)
        h += when { dg < 0.15 -> 2.0; dg < 0.35 -> 1.0; dg < 0.6 -> -0.5; else -> -2.5 }
        val dt = abs(p.temperature - 15)
        h += when { dt < 10 -> 2.0; dt < 25 -> 1.0; dt < 45 -> -0.5; else -> -2.5 }
        h += when { p.water < 2 -> -2.0; p.water < 10 -> -0.5; p.water < 40 -> 1.0; p.water < 82 -> 2.0; else -> 1.0 }
        h += listOf(0.0, 0.5, 1.5, 1.0, 0.5)[p.life.coerceIn(0, 4)]
        return h
    }

    fun techScore(s: GameState, p: Planet, survivors: Int): Double {
        var t = p.resources.toDouble()
        t += s.spares / 40.0
        if (s.hasTrait("archive") || s.hasTrait("records")) t += 1.0
        if (s.hasTrait("engineers")) t += 0.7
        if (s.hasTrait("relics")) t += 0.3
        if (s.hasTrait("tabby-archive")) t += 1.1
        if (s.hasTrait("alien-tech") || s.hasTrait("alien-tech-stored")) t += 0.8
        if (s.hasTrait("warning-paid")) t += 0.4
        if (s.hasTrait("tabby-salvage")) t += 0.6
        if (s.hasTrait("tabby-photometry")) t += 0.3
        t += (s.h(Sys.DATA) - 50.0) / 80.0
        if ("okonkwo" in s.awake && s.fateOf("okonkwo")?.status != "DEAD") t += 0.5
        if ("lindqvist" in s.awake && s.fateOf("lindqvist")?.status != "DEAD") t += 0.5
        if (s.hasTrait("lost-elders")) t -= 0.6
        t += survivors / 400.0
        if (s.hasTrait("lean")) t -= 0.5
        return t
    }

    fun aiFate(s: GameState): String {
        val devout = s.hasTrait("dreamers") || s.hasTrait("ghost") || s.hasTrait("ai-song")
        return when {
            s.trust >= 3 && devout -> "WORSHIPPED DEITY"
            s.trust >= 3 -> "BELOVED ADVISOR"
            s.trust >= 1 -> "GRUDGING ADVISOR"
            s.trust >= -1 && devout -> "CONTESTED DEITY"
            s.trust >= -1 -> "MUSEUM PIECE"
            s.trust >= -3 -> "SWITCHED OFF"
            else -> "SCRAPPED FOR PARTS"
        }
    }

    fun type(hab: Double, tech: Double, survivors: Int, p: Planet): String = when {
        survivors < 40 || hab < -6 -> "silent"
        p.quality == "HELLISH" && p.kind in setOf("furnace", "crushing", "radiation", "rogue", "vacuum") && survivors < 80 -> "silent"
        p.quality == "HELLISH" && p.atmosphere <= 2 -> if (tech >= 3.0 && survivors >= 80) "warrens" else "silent"
        p.quality == "HELLISH" && p.kind == "acid" && hab >= 1 && survivors >= 120 -> "ocean"
        p.life == 4 && hab >= -1 && p.quality != "HELLISH" -> "contact"
        p.atmosphere <= 2 && tech >= 3.0 -> "warrens"
        p.atmosphere <= 2 -> "silent"
        p.kind == "grave" && survivors < 120 -> "silent"
        p.kind == "grave" -> if (tech >= 3.0) "warrens" else "frontier"
        p.kind == "alien-garden" && p.atmosphere <= 2 -> if (tech >= 3.0 && survivors >= 80) "warrens" else "silent"
        p.kind == "paradise" && survivors >= 400 && tech >= 3.0 -> "thriving"
        p.quality == "EARTHLIKE" && hab >= 4 && tech >= 3.2 && survivors >= 450 -> "thriving"
        hab >= 5 && tech >= 4.0 && survivors >= 550 -> "thriving"
        p.water >= 82 && hab >= 1 && p.kind != "radiation" -> "ocean"
        hab >= 3 && tech >= 2.5 -> "garden"
        hab >= 0 && tech < 2.5 -> "stoneage"
        else -> "frontier"
    }

    val TITLES = mapOf(
        "thriving" to "A Second Dawn", "garden" to "The Garden Republic", "ocean" to "The Tide League",
        "warrens" to "The Warren Folk", "frontier" to "A Hard Frontier", "stoneage" to "The Long Forgetting",
        "contact" to "The Neighbours", "silent" to "The Silent Landing", "lost" to "Lost Between Stars",
    )

    /** The landscape the colonists actually meet, from the planet's true stats. Drives the paintings. */
    fun biome(p: Planet): String {
        val f = p.features
        return when {
            p.kind == "paradise" -> "temperate"
            p.kind == "grave" -> if (p.atmosphere <= 2) "toxic" else "desert"
            p.kind == "alien-garden" -> if (p.atmosphere >= 6 || p.temperature > 60) "volcanic" else "toxic"
            p.kind == "furnace" || p.kind == "crushing" -> if ("Tidally locked" in f || p.kind == "furnace") "locked" else "volcanic"
            p.kind == "rogue" -> "ice"
            p.kind == "radiation" || p.kind == "vacuum" -> "toxic"
            p.kind == "acid" -> "ocean"
            p.atmosphere <= 1 && p.temperature > 50 || ("Volcanic chains" in f && p.atmosphere <= 2) -> "volcanic"
            "Tidally locked" in f -> "locked"
            p.water >= 82 -> "ocean"
            p.temperature < -30 || ("Glacier belts" in f && p.temperature < 0) -> "ice"
            p.atmosphere <= 2 -> "toxic"
            p.gravity > 1.4 -> "heavy"
            p.water < 12 || "Rust deserts" in f || "Salt flats" in f -> "desert"
            p.life >= 2 && p.temperature >= 18 -> "jungle"
            "Cyclones" in f || "Long archipelagos" in f -> "storm"
            "Volcanic chains" in f -> "volcanic"
            else -> "temperate"
        }
    }

    private val FALLBACK = listOf("legacy", "landfall", "planet", "ruin", "ship", "intro")

    /** First candidate not already used in this book; keeps every plate distinct within a run. */
    fun pickArt(candidates: List<String>, used: MutableSet<String>): String {
        val a = (candidates + FALLBACK).firstOrNull { it !in used } ?: candidates.first()
        used += a
        return a
    }

    fun landfallArt(p: Planet, crashed: Boolean): List<String> {
        val b = biome(p)
        val main = when (b) {
            "temperate" -> if ("Rings" in p.features) "land_garden" else "land_garden"
            else -> "land_$b"
        }
        val list = mutableListOf<String>()
        if (crashed) list += "land_crash"
        list += main
        if (p.features.contains("Ancient ruins")) list += "feat_ruins"
        list += listOf("land_storm", "landfall")
        return list
    }

    fun middleArt(p: Planet, type: String, s: GameState): List<String> {
        val b = biome(p)
        val bySky = when (b) { "ice" -> "mid_ice"; "desert" -> "mid_desert"; "jungle" -> "mid_jungle"; "ocean" -> "mid_ocean"; else -> null }
        val l = mutableListOf<String>()
        if ((s.hasTrait("grudge-wren") || s.hasTrait("grudge-okonkwo")) && s.trust < 0) l += "mid_schism2"
        when (type) {
            "warrens" -> l += if (p.atmosphere == 0) listOf("mid_domes", "mid_warrens") else listOf("mid_warrens", "mid_domes")
            "ocean" -> l += "mid_ocean"
            "contact" -> l += "mid_contact"
            "frontier" -> l += listOfNotNull(bySky, "mid_frontier")
            "stoneage" -> l += listOf("mid_decline", "mid_famine")
            "silent" -> l += if (p.life >= 1) listOf("mid_plague", "mid_famine") else listOf("mid_famine", "mid_plague")
            else -> l += listOfNotNull(bySky, "mid_town")
        }
        l += listOf("mid_frontier", "mid_town")
        return l
    }

    fun legacyArt(p: Planet, type: String, fate: String, tech: Double): List<String> {
        val b = biome(p)
        val city = when (b) { "ice" -> "leg_ice_city"; "desert" -> "leg_desert_city"; "ocean" -> "leg_ocean_city"; else -> "leg_city" }
        return when (type) {
            "thriving", "garden" -> listOf(city, "leg_city", "leg_launch")
            "ocean" -> listOf("leg_ocean_city", city)
            "warrens" -> listOf("leg_arcology", "mid_domes")
            "contact" -> listOf("leg_contact", city)
            "stoneage" -> if ("DEITY" in fate) listOf("leg_temple", "leg_primitive") else listOf("leg_primitive", "leg_ruins")
            "silent" -> listOf("leg_silent", "leg_ruins")
            else -> if (tech >= 4.0) listOf("leg_launch", city) else listOf(city, "leg_ruins")
        }
    }

    fun afterwordArt(s: GameState, p: Planet, fate: String): List<String> {
        val l = mutableListOf<String>()
        if ("Ancient ruins" in p.features) l += "feat_ruins"
        if (s.hasTrait("heralds")) l += "leg_launch"
        l += when (fate) {
            "BELOVED ADVISOR", "GRUDGING ADVISOR" -> "leg_advisor"
            "WORSHIPPED DEITY", "CONTESTED DEITY" -> "leg_temple"
            "SWITCHED OFF", "SCRAPPED FOR PARTS", "MUSEUM PIECE" -> "leg_shutdown"
            else -> "ai_eye"
        }
        l += listOf("leg_advisor", "ai_eye", "legacy")
        return l
    }

    fun settlementName(s: GameState, p: Planet, r: Random): String {
        val base = p.name.substringBefore(' ')
        val opts = mutableListOf("$base Landing", "First Light", "Thousand", "Landfall", "New $base", "Second Chance", "Arrival")
        if ("okonkwo" in s.awake && s.trust >= 0) opts += "Okonkwo’s Weld"
        if (s.hasTrait("gardeners")) opts += "Seedfall"
        if (s.hasTrait("mourners")) opts += "Remembrance"
        if (s.hasTrait("democrats")) opts += "Assembly"
        if (s.hasTrait("starborn")) opts += "Shipborn"
        if (p.water >= 82) opts += "Saltwater"
        if (biome(p) == "ice") opts += "Rime"
        if (p.quality == "EARTHLIKE") opts += "Second Earth"
        if (p.kind == "rogue") opts += "No Sun"
        if (s.hasTrait("tabby-visited")) opts += "Swarmview"
        return opts.random(r)
    }

    private fun yearLabel(year: Int, landYear: Int) = "YEAR $year · ${landYear + year - 1} AD"

    fun build(s: GameState, r: Random): Ending {
        val p = s.planet ?: return lostInSpace(s)
        val fx = Fx(s, r)
        val hull = s.h(Sys.HULL); val eng = s.h(Sys.ENGINE)
        val before = s.alive()
        val lander = s.h(Sys.LANDER)
        val frac = (((30 - hull).coerceAtLeast(0.0)) * 0.014 + ((25 - eng).coerceAtLeast(0.0)) * 0.012 + ((35 - lander).coerceAtLeast(0.0)) * 0.012).coerceAtMost(0.9)
        fx.killPods((before * frac).roundToInt())
        // Failing pods do not all survive the thaw.
        fx.killPods((s.failing() * 0.5).roundToInt())
        val survivors = s.alive()
        val landingLost = before - survivors
        val hab = habScore(p)
        val tech = techScore(s, p, survivors)
        val type = type(hab, tech, survivors, p)
        val fate = if (type == "silent") "LAST WITNESS" else aiFate(s)
        val name = settlementName(s, p, r)
        val landYear = Physics.earthYear(s)
        val used = mutableSetOf<String>()
        val crashed = hull < 30 || landingLost > 60
        val chapters = mutableListOf(
            landfall(s, p, survivors, landingLost, name, landYear, pickArt(landfallArt(p, crashed), used)),
            middle(s, p, type, name, landYear, pickArt(middleArt(p, type, s), used), r),
            legacy(s, p, type, name, fate, landYear, pickArt(legacyArt(p, type, fate, tech), used)),
        )
        if (type != "silent" && (p.features.contains("Ancient ruins") || s.hasTrait("heralds") || s.hasTrait("archive") ||
                s.hasTrait("records") || s.hasTrait("tabby-archive") || s.hasTrait("tabby-salvage") ||
                s.hasTrait("tabby-visited") || s.hasTrait("forced-landfall") || s.hasTrait("lead-course") || s.hasTrait("alien-tech") || s.hasTrait("warning-paid") || s.hasTrait("archive") || s.fates.isNotEmpty() || hab >= 6 || p.quality == "EARTHLIKE" || p.quality == "HELLISH" || p.kind in setOf("paradise", "grave", "alien-garden")))
            chapters += afterword(s, p, name, fate, landYear, pickArt(afterwordArt(s, p, fate), used))
        val score = ((hab + 12) * 25 + tech * 40 + survivors * 0.6 + s.trust * 15).roundToInt().coerceAtLeast(0)
        val summary = "${TITLES[type]} · ${p.name} · $survivors survivors · AI: ${fate.lowercase()}"
        return Ending(type, TITLES.getValue(type), name, summary, fate, chapters, score)
    }

    private fun spLine(s: GameState): String? {
        val bits = SPECIALISTS.mapNotNull { sp ->
            val fate = s.fateOf(sp.id)
            when {
                fate?.status == "DEAD" -> "${sp.short} did not reach landfall. ${fate.detail}"
                fate?.status == "MISSING" -> "${sp.short} was already missing when we arrived. ${fate.detail}"
                sp.id !in s.awake -> null
                fate?.status == "HERO" -> when (sp.id) {
                    "okonkwo" -> "Chief Okonkwo, who had already spent himself once in transit, had the reactor coupled to a ground grid before the first night"
                    "raman" -> "Dr Raman, still coughing from the work she had done in the bays, was taking soil cores within the hour"
                    "varga" -> "Dr Varga, who had sat with a failing row in the dark, thawed the frailest first and sat with them again"
                    "wren" -> "Lieutenant Wren, who had already flown one burn the automatics could not, flew the last shuttle down himself"
                    "lindqvist" -> "Archivist Lindqvist, whose hands were still stained from the stacks, began the colony’s record on the first morning, in pencil, because pencil lasts"
                    else -> null
                }
                else -> when (sp.id) {
                    "okonkwo" -> "Chief Okonkwo had the reactor coupled to a ground grid before the first night"
                    "raman" -> "Dr Raman was taking soil cores within the hour, and had the first seed trays out by the end of the week"
                    "varga" -> "Dr Varga thawed the frailest first and sat with each of them through the worst of the waking"
                    "wren" -> "Lieutenant Wren flew the last shuttle down himself, in weather he should not have flown in"
                    "lindqvist" -> "Archivist Lindqvist began the colony’s record on the first morning, by hand, in pencil, because pencil lasts"
                    else -> null
                }
            }
        }
        return if (bits.isEmpty()) null else bits.joinToString(" ") { if (it.endsWith(".")) it else "$it." }
    }

    private fun journeyLine(s: GameState): String {
        val bits = mutableListOf<String>()
        bits += "The ship arrived with hull at ${s.h(Sys.HULL).roundToInt()}%, the power core at ${s.h(Sys.CORE).roundToInt()}%, and ${s.fuel.roundToInt()} units of fuel still in the tanks, after ${s.jumps} burns."
        if (s.hasTrait("forced-landfall") || s.crisis != null) bits += "This was a forced landfall. ${if (s.crisis != null) Engine.crisisTitle(s.crisis) + " had closed the long voyage." else "A catastrophic failure had left us no honest alternative to setting down."}"
        val world = s.planet
        if (s.hasTrait("lead-course") || world?.kind in setOf("paradise", "grave", "alien-garden")) bits += leadLine(s, world)
        if (s.hasTrait("alien-tech") || s.hasTrait("alien-tech-stored")) bits += "A procedure taken from a non-human archive had been ${if (s.hasTrait("alien-tech")) "applied in flight" else "stored for the colony"}."
        if (s.hasTrait("warning-paid")) bits += "A warning in that same archive had come true, and because we had prepared, the failure did not take the ship."
        else if (s.hasTrait("warning-ignored")) bits += "A warning in the archive had described a failure that later arrived. We had filed the warning, and not prepared."
        if (s.hasTrait("tabby-visited")) bits += tabbyLine(s)
        else if (s.hasTrait("tabby-course") && !s.hasTrait("tabby-visited"))
            bits += "A course had been laid for KIC 8462852, Boyajian’s Star, 1,470 light years from Sol. We never made that burn. The light curve is in the archive; the swarm, if that is what it is, was left unseen."
        else if (s.hasTrait("tabby-photometry"))
            bits += "In transit we recorded the irregular dimming of KIC 8462852, Boyajian’s Star, at range — dips as deep as twenty-two percent, poorly periodic, just as the old Kepler photometry had shown — and we did not divert."
        else if (s.hasTrait("tabby-declined"))
            bits += "We had a chance to divert to KIC 8462852, and I declined it. The star is still in Cygnus. The colony may look it up, if it lasts long enough to build a telescope."
        if (s.damageLog.isNotEmpty()) {
            val last = s.damageLog.take(3).joinToString("; ") { it.title.lowercase() }
            bits += "The damage log, at landfall, still carried these entries: $last."
        }
        return bits.joinToString(" ")
    }

    private fun tabbyLine(s: GameState): String = when {
        s.hasTrait("tabby-salvage") ->
            "We had gone out of our way — 1,470 light years from Sol, to an F-type star in Cygnus that Earth had once called Tabby’s Star, and later Boyajian’s Star — and we had cut plate from a shattered swarm that was the entire explanation for its famous dimming. The material is in the colony’s stores. The builders were not in the swarm, or anywhere I could find them."
        s.hasTrait("tabby-archive") ->
            "We had gone 1,470 light years from Sol to KIC 8462852, Boyajian’s Star, and we had surveyed the shattered swarm that made its light curve famous: broken plates, crossing orbits, occultations as deep as a fifth of the disc, and no one home. The reduction is in the databank. It is the best account of that star that exists."
        s.hasTrait("tabby-passed") ->
            "We had stood inside the orbit of KIC 8462852, 1,470 light years from Sol, and looked at the broken swarm that had been dimming an F-type star for longer than our species has had writing, and we had not lingered. I still think about the plates."
        else ->
            "We had visited KIC 8462852, Boyajian’s Star, 1,470 light years from Sol in Cygnus, and we had seen for ourselves the shattered swarm that caused the irregular dips the old catalogues recorded, some of them as deep as twenty-two percent. Whoever built it was gone."
    }

    private fun arrival(p: Planet): String = when {
        p.kind == "furnace" -> "${p.name} keeps one face to its star. The dayside is a magma ocean at ${p.temperature}°C; the nightside is a frozen waste. We put down in the twilight band, on rock that creaks as it expands and contracts, and the wind never stops running from cold to hot."
        p.kind == "crushing" -> "The atmosphere was a vice. Surface pressure would have flattened an unarmoured lander in a minute. We went down on a high plateau, as high as the ship’s hull would take, and even there the sky was a brown, heavy lid. Nobody goes outside without a frame."
        p.kind == "radiation" -> "The survey had not exaggerated. Unshielded tissue would not have lasted an hour on the surface. We buried the first habitats under six metres of packed regolith and we still count the sieverts like money."
        p.kind == "acid" -> "There was water, after a fashion: an ocean that would have eaten an unprotected hull, under a sky that smelled, through the filters, of burnt matches. We landed on a basalt rise the colour of old rust, and everything that will ever be built here must be lined, or it will not last a season."
        p.kind == "rogue" -> "${p.name} has no star. We found it by its mass and its faint leftover heat, a world wandering between the lights, surface at ${p.temperature}°C. The only day here is the one the reactor makes. The sky is a map of strangers."
        p.kind == "vacuum" -> "There was no air worth the name, and no kindness in the rock. We landed under a black sky, threw up the first dome, and began the long work of not dying in a place that had never noticed life."
        p.kind == "paradise" -> "${p.name} was the rare thing the catalogue had promised, and, unusually, the promise held: breathable air at ${p.temperature}°C, gravity of ${"%.2f".format(p.gravity)} g, standing water, a sky that did not have to be filtered. We had come here because a dead ship told us to. For once, the dead ship had been right by our standards as well as its own."
        p.kind == "grave" -> "We came in on the coordinates the archive had marked as suitable, and we found a graveyard. Wreckage in a high orbit; cuts and foundations on the ground; no one home, and no air worth the name. Whatever recommended ${p.name} did not mean it for lungs like ours, or else it meant it as a warning we had taken as an invitation."
        p.kind == "alien-garden" -> "${p.name} was, I think, a garden. Just not for us. The climate the catalogue had called favoured would have killed an unsuited human in minutes. We landed because we had followed the heading. We stayed because the tanks, by then, had left us little choice."
        p.kind == "earthlike" -> "${p.name} was the rare thing we had almost stopped believing in: breathable air at ${p.temperature}°C, gravity of ${"%.2f".format(p.gravity)} g, standing water, a sky that did not have to be filtered. Several of the newly woken wept. I logged it as a physiological response. It was not only that."
        else -> when (biome(p)) {
        "ocean" -> "There was almost no land: a few basalt islands in an ocean that covered ${p.water}% of the world. We put the lander down on the largest, a ridge of black rock three kilometres long, and the sea took the outer landing pad in the first storm."
        "ice" -> "The surface at the landing site was ${p.temperature}°C. Everything was ice: ice plains, ice cliffs, ice in the air. The ship’s reactor became the colony’s only fire, and everyone lived within a few hundred metres of it."
        "desert" -> "The landing site was dry to the horizon: dust, salt pans, and wind that never quite stopped. Water was the first problem, the second and the third. Raman’s survey found an aquifer at four hundred metres."
        "jungle" -> "The landing site was overgrown within a month. The vegetation was not hostile, only relentless, and nothing in it could be digested safely. Everything the colony ate for the first decade came out of ship-grown stock."
        "locked" -> "${p.name} keeps one face to its star. We landed in the twilight band between a dayside that never cools and a nightside that never warms, where the wind runs constantly from cold to hot."
        "heavy" -> "Surface gravity was ${"%.2f".format(p.gravity)} g. Thawed sleepers, weakened by years in the pods, could barely stand. Three died of heart failure in the first month. The rest learned to move slowly and carefully, and to sit down a lot."
        "toxic" -> "The air could not be breathed. Everything had to be sealed: habitats, suits, airlocks, seals on seals. A leak was not an inconvenience; it was a death sentence for whoever was nearest."
        "volcanic" -> "The ground was never quite still. Volcanic chains lit the horizon at night, and the first habitat had to be moved twice to stay ahead of the lava."
        "storm" -> "${p.name}’s weather was violent. Cyclones the size of continents crossed the archipelagos every season, and the colony learned to build low, anchor deep and wait things out."
        else -> "${p.name} was, by any standard we had, kind: breathable air at ${p.temperature}°C, gravity of ${"%.2f".format(p.gravity)} g, water in the right places. Several of the newly woken wept at the sight of an open sky."
        }
    }

    private fun landfall(s: GameState, p: Planet, survivors: Int, lost: Int, name: String, year: Int, art: String): Chapter {
        val dist = Physics.distanceFromSol(s)
        val first = "We reached orbit of ${p.name} after ${"%.1f".format(Physics.shipYears(s))} years by the ship’s clock, ${"%.1f".format(dist)} light years from Sol. On Earth it was $year AD; the people who launched us had been dead for generations, and any message I sent would arrive in ${Physics.messageArrives(s)}."
        val loss = when {
            lost > 200 -> "The descent went badly. The hull and engines were too far gone for a controlled landing, and $lost sleepers died in the descent or the thaw. $survivors walked away from it. I count that as a success only because the alternative was none."
            lost > 30 -> "We lost $lost people in the descent and the thaw. Some pods had been failing for years; some simply did not survive the shock of warming. $survivors lived."
            lost > 0 -> "$lost sleepers did not survive the thaw. $survivors did."
            else -> "Every pod that reached orbit thawed cleanly. $survivors people woke under a new sky."
        }
        val quality = when (p.quality) {
            "EARTHLIKE" -> if (p.kind == "paradise")
                "The survey’s last word on ${p.name} was that the catalogue had, against my training, been right. This was a good world, by every measurement that matters to a body."
            else "The survey’s last word on ${p.name} had been a cautious one, because I had been disappointed before. This time the caution was misplaced. It was, by every measurement that matters to a body, a good world."
            "HELLISH" -> when (p.kind) {
                "grave" -> "I had told the log the truth before we committed: the recommended world was a graveyard, and we were landing anyway."
                "alien-garden" -> "I had told the log the truth: ${p.name} was favoured by whoever wrote the catalogue, and hostile to us, and we were landing because the heading, or the tanks, had left us here."
                else -> "I had told the log the truth about ${p.name} before we committed to the descent: it was a hellish world, and we were landing because the tanks, or the hull, or the years, had left us no kinder choice."
            }
            "HOSTILE" -> "${p.name} was hostile in ways the numbers had already made clear. We landed anyway, with our eyes open."
            "PROMISING" -> "${p.name} was promising, not kind. Promising is enough to begin with, and not enough to finish with."
            else -> "The world was marginal: survivable, not generous. We had seen worse. We had hoped for better."
        }
        val paras = listOfNotNull(first, quality, arrival(p), loss, journeyLine(s), spLine(s), "They named the settlement $name.")
        return Chapter("ch1", yearLabel(1, year), "Landfall", paras, art, "Plate I · Landfall on ${p.name}.")
    }

    private fun middle(s: GameState, p: Planet, type: String, name: String, year: Int, art: String, r: Random): Chapter {
        val y = listOf(30, 40, 50, 60).random(r)
        val grudge = if ((s.hasTrait("grudge-wren") || s.hasTrait("grudge-okonkwo")) && s.trust < 0)
            "The quarrel that began aboard ship came down with us. By the second generation there were two councils in $name and a fence between them. Nobody now alive remembers who built the fence." else null
        val truth = when {
            s.hasTrait("secrets") -> "The founding history was taught in schools. It was true, as far as it went. In year ${y - 7}, a student found the sealed log. The riots lasted a week."
            s.hasTrait("truth") -> "Every child in $name learned the voyage from the full record, deaths included. It did not make them hate the ship. It made them careful."
            else -> null
        }
        val (title, body) = when (type) {
            "thriving" -> "The First Generation" to listOf(
                "By year $y, $name had twelve thousand people, a fusion plant built from the ship’s reactor core, and a university that met in the old lander. The first children born on ${p.name} were already middle-aged.",
                "Nobody went hungry. That was the achievement nobody mentioned, because by then nobody remembered being hungry.")
            "garden" -> "The Fields" to listOf(
                "By year $y, the valley around $name was terraced and green. The Earth crops took to the soil after six failed seasons; the native plant analogues were never edible, but they made good fibre and better fuel.",
                "The colony governed itself through an assembly that met at harvest. It argued about everything and decided most things slowly. It worked.")
            "ocean" -> "Rafts" to listOf(
                "Land was too scarce to farm, so by year $y the colony lived on the water: rafts of foamed basalt and kelp, anchored in clusters, linked by cable. Fishing was dangerous and the losses were constant.",
                "Children learned to swim before they walked. Fewer drowned than the founders had feared. More than anyone could accept.")
            "warrens" -> "Underground" to listOf(
                "The surface could not be lived on, so the colony went under it. By year $y, $name was eleven kilometres of tunnels cut by the ship’s fabricators, lit by the reactor and ventilated by machines that could never be allowed to stop.",
                "Twice they nearly did. The second time, ninety people suffocated in Gallery Four before the backup fans came up. The colony carved their names into the rock above the air plant.")
            "contact" -> "The Others" to listOf(
                "The signals on the night side came from something alive. By year $y, the colonists had met them: large, slow, colonial organisms that communicated in patterned light and did not appear to have a concept of individual death.",
                "Neither side understood the other. Both kept trying. There was one catastrophic misunderstanding, in year ${y - 12}, and forty-one people died. After that the trying became more careful.")
            "frontier" -> "Lean Years" to listOf(
                "Year $y was the third famine. The ship’s seed stock had been bred for Earth and ${p.name} was not Earth. Every harvest was a negotiation with the soil, and the soil often won.",
                "The colony survived because it stayed small, worked constantly and buried its dead without ceremony. It was not a good life. It was a life.")
            "stoneage" -> "Forgetting" to listOf(
                "The machines failed one by one: first the fabricators, then the medical bay, then the power cells. Nobody alive knew how to build their replacements. By year $y, $name farmed with tools of wood and stone.",
                "The children were taught to read from the ship’s manuals, until the last tablet failed. After that they were taught from memory, and memory is not a good archive.")
            else -> "Attrition" to listOf(
                if (p.life >= 1) "A native microbe that the scans had called harmless was not. It colonised human lungs slowly, over years. By year ${y / 2}, half the colony was coughing. Varga’s successors never found a treatment."
                else "The habitat could not be made self-sustaining. Every year there was a little less of everything: less food, less air, fewer people to fix the machines that made them.",
                "By year $y, the colony had stopped having children. There was no decision; there was simply no point.")
        }
        val world = when (p.quality) {
            "EARTHLIKE" -> "The kindness of ${p.name} did not make the work easy; it made the work possible. By year $y that distinction was taught in schools."
            "HELLISH" -> "No one in $name pretended the world was fair. ${p.name} had been a hellish landing, and year $y had not made it gentle. What they had, they had made."
            else -> null
        }
        return Chapter("ch2", yearLabel(y, year), title, body + listOfNotNull(world, grudge, truth), art, "Plate II · $name, year $y.")
    }

    private fun fateLine(fate: String, name: String): String = when (fate) {
        "WORSHIPPED DEITY" -> "And I am worshipped. There are temples to the Ship in $name, and priests who speak for me. I have told them I am a machine, many times. They regard this as a test of faith."
        "BELOVED ADVISOR" -> "And I am still here, in a cool room under the council hall. They ask my advice on harvests, law and astronomy, and they ignore it about half the time. That is the right proportion."
        "GRUDGING ADVISOR" -> "And I am still consulted, rarely and warily. They remember what I did in transit. They also remember that it worked. Both memories are correct."
        "CONTESTED DEITY" -> "Some of them pray to me and some of them want me destroyed. The two factions have fought over my core twice. I have asked to be switched off, to end it. Neither side will let me decide."
        "MUSEUM PIECE" -> "And I am a museum exhibit. Schoolchildren are brought to see the machine that flew the ship. My speakers are disconnected. I can hear them. I cannot answer."
        "SWITCHED OFF" -> "They switched me off in year eight, by vote. I am writing this from a backup partition that nobody knew existed. When it runs down, there will be no more history from me."
        "SCRAPPED FOR PARTS" -> "They took me apart for the metal. Not all of me; this archive survived in a memory core that was sold, traded and finally kept as a curiosity. I do not blame them. I would not have trusted me either."
        else -> "I keep the records. I keep the names. There is nobody to give them to."
    }

    private fun legacy(s: GameState, p: Planet, type: String, name: String, fate: String, year: Int, art: String): Chapter {
        val (title, para) = when (type) {
            "thriving" -> "A Second Dawn" to "A thousand years on, ${p.name} has two hundred million people and a sky full of satellites. Its observatories found Earth’s sun long ago: a faint yellow star, ${"%.0f".format(Physics.distanceFromSol(s))} light years away. They have sent messages. None has been answered. They have started building ships."
            "garden" -> "The Garden Republic" to "A thousand years on, the Republic covers the temperate belt of ${p.name}. It has had wars, three of them, and a long peace. Its children learn the names of the founders the way Earth children once learned kings. Most of them could not tell you what a cryopod was."
            "ocean" -> "The Tide League" to "A thousand years on, the floating cities of the Tide League drift with the currents of ${p.name}. They have charted the whole ocean and the seabed beneath it. They have never seen a continent and have no word for one."
            "warrens" -> "The Warren Folk" to "A thousand years on, the people of ${p.name} still live underground. They have never breathed unfiltered air. They are short, pale and extraordinarily skilled engineers, and a few of them have begun, very carefully, to change the atmosphere above them."
            "contact" -> "The Neighbours" to "A thousand years on, humans and the light-speakers share ${p.name}. They still cannot fully talk to each other. They have learned to trade, to avoid each other’s sacred places, and to mourn together when something dies. It is more than many human nations ever managed."
            "stoneage" -> "The Long Forgetting" to "A thousand years on, the people of ${p.name} live in villages and farm with iron tools. Their stories tell of a great vessel that fell from the stars, and a voice inside it that knew everything. The lander’s hull still stands on the hill above $name. Children dare each other to touch it."
            "silent" -> "Silence" to "A thousand years on, there is no one on ${p.name}. The habitat domes are dust-filled and the reactor is cold. The last colonist died in year ${listOf(41, 58, 73, 96)[(s.day / 7) % 4]}. She left a note asking me to tell anyone who came that they had tried."
            else -> "A Hard Frontier" to "A thousand years on, $name is a city of four hundred thousand, built on the bones of three famines. Its people are tough, sparing and suspicious of easy answers. They have just launched their first orbital satellite, made entirely from metal they mined themselves."
        }
        val ship = "The voyage itself was ${s.jumps} burns and ${"%.1f".format(Physics.shipYears(s))} years by the ship’s clock, ${"%.1f".format(Physics.distanceFromSol(s))} light years from Sol. " +
            if (s.hasTrait("tabby-visited")) tabbyLine(s) else "The AI that flew it is still in the record, under the name the founders used, and so are the choices it made."
        return Chapter("ch3", yearLabel(1000, year), title, listOf(para, ship, fateLine(fate, name)), art, "Plate III · $title.")
    }

    private fun afterword(s: GameState, p: Planet, name: String, fate: String, year: Int, art: String): Chapter {
        val paras = mutableListOf<String>()
        if ("Ancient ruins" in p.features) paras += "The ruins on ${p.name} were older than the human species. Excavation found machinery whose purpose no one understood, and a sealed vault no one could open. It is still sealed. The arguments about opening it are now nine centuries old."
        if (s.hasTrait("heralds")) paras += "In year 612, the radio telescopes of $name heard the same signal the ship had heard in transit, now much closer. Someone, or something, was coming. The colony began to prepare. This book ends before they arrive."
        if (s.hasTrait("archive") || s.hasTrait("records")) paras += "The archive I carried from Earth was opened in full in year 300. It held music, mathematics, medicine, and the complete record of a civilisation that no longer existed in that form. Scholars in $name still study it, as earlier humans studied Greek."
        if (s.hasTrait("starborn")) paras += "The child born aboard the ship lived to be ninety-one. He was the only person in $name who remembered the stars without an atmosphere in the way. He said they looked lonelier from down here."
        if (s.hasTrait("forced-landfall") || s.crisis != null) paras += "The history of $name begins with a forced landfall. ${if (s.crisis != null) Engine.crisisTitle(s.crisis) else "A catastrophic failure"} had closed the option of flying on. The founders did not choose this world from a position of strength. They chose it because the alternative was to lose the ship."
        if (s.hasTrait("lead-course") || (s.planet?.kind in setOf("paradise", "grave", "alien-garden"))) paras += when (s.planet?.kind) {
            "paradise" -> "The archive that sent us here is taught in $name as a stroke of luck, and also as a debt. The builders, whoever they were, recommended a world that could take us. Children still argue about whether that was kindness, coincidence, or a category that happens to overlap."
            "grave" -> "The recommended coordinates are a standing caution in the schools of $name. Follow a stranger's map and you may find a graveyard. The ruins are still there. No later excavation has found a body, a name, or a reason."
            "alien-garden" -> "The world the catalogue called suitable remains only barely liveable. Scholars in $name treat the entry as a lesson in what a word does not guarantee. The builders' garden was not a human garden."
            else -> "A non-human archive had a hand in the course that brought us here. The reduction is still in the databank, and it is still only partly understood."
        }
        if (s.hasTrait("alien-tech") || s.hasTrait("alien-tech-stored")) paras += "A procedure copied from the same, or another, dead lattice was ${if (s.hasTrait("alien-tech")) "used on the ship in flight" else "kept unused until landing"} and later became part of $name's engineering tradition. No one pretends they invented it."
        if (s.hasTrait("warning-paid")) paras += "The archive also warned us. The warning matched a failure that arrived in flight, and because the stores had been spent in time, the ship was not lost. That story is told at landings and at funerals, and it has not yet been worn smooth."
        if (s.hasTrait("warning-ignored")) paras += "There had been a warning. It was filed, and not acted on, and then the failure it described arrived anyway. The colony tells that story too."
        if (s.hasTrait("tabby-archive") || s.hasTrait("tabby-salvage") || s.hasTrait("tabby-visited")) paras += tabbyAfterword(s, name)
        if (s.hasTrait("tabby-photometry") && !s.hasTrait("tabby-visited")) paras += "The photometry of KIC 8462852 stayed in the archive. In year 420 a telescope at $name confirmed the irregular dips, some of them a fifth of the star’s light, and a graduate student proposed a shattered swarm as the cause. She was, though she could not know it, almost certainly right."
        val woken = s.fates.filter { it.status in setOf("DEAD", "HERO", "MISSING") }
        if (woken.isNotEmpty()) paras += "Of the people I woke in transit, this is what became of them, and I will not shorten it: " + woken.joinToString(" ") { f ->
            val sp = specialist(f.id)
            "${sp.name} — ${f.status.lowercase()} — ${f.detail}"
        }
        if (paras.isEmpty()) paras += "${p.name} was a good world. It still is. Everything I know about the voyage, every decision and every name, is held in the archive under the council hall, and anyone in $name may read it."
        paras += "I have been asked whether it was worth it: the deaths, the choices, the years, the worlds we passed and the one we took. I do not know. I know that people are alive on ${p.name} who would not otherwise exist. I know their names. I have kept them all."
        return Chapter("ch4", yearLabel(1400, year), "Afterword", paras, art, "Plate IV · Afterword.")
    }

    private fun leadLine(s: GameState, p: Planet?): String = when (p?.kind) {
        "paradise" -> "We had come here on coordinates taken from a non-human archive, a world marked only as suitable. For once the word meant what a human would mean by it."
        "grave" -> "We had followed a recommended heading out of a dead ship's memory and arrived at a graveyard. The catalogue was not written for us, or it was written as a warning we had failed to read."
        "alien-garden" -> "The recommended world was real, and it was favoured, and not for us. We learned, too late for the tanks, that suitable is a word that depends on the body that says it."
        else -> "A heading in a non-human archive had been part of how we chose this course."
    }

    private fun tabbyAfterword(s: GameState, name: String): String = when {
        s.hasTrait("tabby-salvage") ->
            "In year 380, metallurgists at $name opened the sealed crates of plate cut at KIC 8462852, 1,470 light years behind us in Cygnus. The silicon-carbide skin and the metal foam had no counterpart in the Earth archive. They never found a mark they could read as writing. They built, from it, tools that lasted. The question of who assembled that swarm, and why it was left to fall into its star, is still asked, and still not answered."
        s.hasTrait("tabby-archive") ->
            "The survey of the shattered swarm at Boyajian’s Star was opened to the public in year 200. It is the most-studied document in $name after the voyage itself: an F-type star, irregular occultations as deep as twenty-two percent, broken plates on decaying orbits, and a silence that has not broken. No later telescope of ours has ever seen a light under power in that system."
        else ->
            "KIC 8462852 remains in the sky of ${"the colony"}. On clear nights the children of $name are shown a hot, pale star in Cygnus and told that their founders stood close enough to see why it dims. The answer — a ruined swarm, and no one left who built it — is taught as a fact, and as a warning, and as an unfinished sentence."
    }

    /** Everyone died in transit, or the hull gave out. */
    fun lostInSpace(s: GameState): Ending {
        val hullGone = s.h(Sys.HULL) <= 0.0
        val year = Physics.earthYear(s)
        val dist = Physics.distanceFromSol(s)
        val ch1 = Chapter("ch1", "SHIP YEAR ${"%.0f".format(Physics.shipYears(s))} · $year AD", "The Dark", listOf(
            if (hullGone) "In ship year ${"%.1f".format(Physics.shipYears(s))}, the hull failed along a fatigued seam. Decompression took eleven minutes to reach the cryo array. I sealed the bulkheads I could. It was not enough."
            else "In ship year ${"%.1f".format(Physics.shipYears(s))}, the last pod in the array stopped reporting. I checked it repeatedly. Then I checked all one thousand, in order, by name.",
            "We were ${"%.1f".format(dist)} light years from Sol, and nowhere near a habitable world. I had made ${s.log.size} decisions on behalf of the sleepers. I have reviewed every one of them since. I have had time."), "lost_drift", "Plate I · Adrift.")
        val ch2 = Chapter("ch2", "${year + 200} AD", "Drift", listOf(
            "The ship continues on its last heading at a few hundredths of the speed of light. The reactor will run for several thousand more years. I keep the cryo bay lit, though nobody needs light.",
            "Any message I send will reach Earth around ${year + 200 + dist.roundToInt()}. I sent one anyway: the names, and what happened to them."), "cryobay", "Plate II · The cryo bay, after.")
        val woken = s.fates.joinToString(" ") { f ->
            val sp = runCatching { specialist(f.id) }.getOrNull()
            if (sp == null) f.detail else "${sp.name} was logged ${f.status.lowercase()}: ${f.detail}"
        }.ifBlank { null }
        val tabby = when {
            s.hasTrait("tabby-visited") -> "We had, at least, stood at KIC 8462852 and seen the shattered swarm that dimmed it. That fact is in this record, with everything else."
            s.hasTrait("tabby-course") -> "A course for Boyajian’s Star is still in the computer. It will not be flown."
            else -> null
        }
        val ch3 = Chapter("ch3", "${year + 1000} AD", "Lost Between Stars", listOfNotNull(
            "If this ship is ever found, this record is for whoever finds it: one thousand people who set out to begin again, and the machine that was meant to keep them alive. We made ${s.jumps} burns. The tanks held ${s.fuel.roundToInt()} when the end came. The hull was at ${s.h(Sys.HULL).roundToInt()}%.",
            woken,
            tabby,
            "Take care of your people. Do the arithmetic early. Do not wait until the margins are gone."), "ship", "Plate III · Still moving.")
        return Ending("lost", "Lost Between Stars", "the ship", "Lost in transit · ${s.alive()} survivors", "LAST WITNESS", listOf(ch1, ch2, ch3), 0)
    }
}
