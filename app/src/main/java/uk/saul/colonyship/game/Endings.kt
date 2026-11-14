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
        var h = listOf(-3.5, -3.5, -1.5, 0.5, 2.0, 1.5)[p.atmosphere.coerceIn(0, 5)]
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
        if ("okonkwo" in s.awake) t += 0.5
        if ("lindqvist" in s.awake) t += 0.5
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
        p.life == 4 && hab >= -1 -> "contact"
        p.atmosphere <= 2 && tech >= 3.0 -> "warrens"
        p.atmosphere <= 2 -> "silent"
        hab >= 5 && tech >= 4.0 && survivors >= 550 -> "thriving"
        p.water >= 82 && hab >= 1 -> "ocean"
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
        return opts.random(r)
    }

    private fun yearLabel(year: Int, landYear: Int) = "YEAR $year · ${landYear + year - 1} AD"

    fun build(s: GameState, r: Random): Ending {
        val p = s.planet ?: return lostInSpace(s)
        val fx = Fx(s, r)
        val hull = s.h(Sys.HULL); val eng = s.h(Sys.ENGINE)
        val before = s.alive()
        val frac = (((30 - hull).coerceAtLeast(0.0)) * 0.014 + ((25 - eng).coerceAtLeast(0.0)) * 0.012).coerceAtMost(0.9)
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
                s.hasTrait("records") || hab >= 6))
            chapters += afterword(s, p, name, fate, landYear, pickArt(afterwordArt(s, p, fate), used))
        val score = ((hab + 12) * 25 + tech * 40 + survivors * 0.6 + s.trust * 15).roundToInt().coerceAtLeast(0)
        val summary = "${TITLES[type]} · ${p.name} · $survivors survivors · AI: ${fate.lowercase()}"
        return Ending(type, TITLES.getValue(type), name, summary, fate, chapters, score)
    }

    private fun spLine(s: GameState): String? {
        val bits = s.awake.map { id ->
            when (id) {
                "okonkwo" -> "Chief Okonkwo had the reactor coupled to a ground grid before the first night"
                "raman" -> "Dr Raman was taking soil cores within the hour, and had the first seed trays out by the end of the week"
                "varga" -> "Dr Varga thawed the frailest first and sat with each of them through the worst of the waking"
                "wren" -> "Lieutenant Wren flew the last shuttle down himself, in weather he should not have flown in"
                "lindqvist" -> "Archivist Lindqvist began the colony’s record on the first morning, by hand, in pencil, because pencil lasts"
                else -> null
            }
        }.filterNotNull()
        return if (bits.isEmpty()) null else bits.joinToString("; ").replaceFirstChar { it.uppercase() } + "."
    }

    private fun arrival(p: Planet): String = when (biome(p)) {
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

    private fun landfall(s: GameState, p: Planet, survivors: Int, lost: Int, name: String, year: Int, art: String): Chapter {
        val dist = Physics.distanceFromSol(s)
        val first = "We reached orbit of ${p.name} after ${"%.1f".format(Physics.shipYears(s))} years by the ship’s clock, ${"%.1f".format(dist)} light years from Sol. On Earth it was $year AD; the people who launched us had been dead for generations, and any message I sent would arrive in ${Physics.messageArrives(s)}."
        val loss = when {
            lost > 200 -> "The descent went badly. The hull and engines were too far gone for a controlled landing, and $lost sleepers died in the descent or the thaw. $survivors walked away from it. I count that as a success only because the alternative was none."
            lost > 30 -> "We lost $lost people in the descent and the thaw. Some pods had been failing for years; some simply did not survive the shock of warming. $survivors lived."
            lost > 0 -> "$lost sleepers did not survive the thaw. $survivors did."
            else -> "Every pod that reached orbit thawed cleanly. $survivors people woke under a new sky."
        }
        val paras = listOfNotNull(first, arrival(p), loss, spLine(s), "They named the settlement $name.")
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
        return Chapter("ch2", yearLabel(y, year), title, body + listOfNotNull(grudge, truth), art, "Plate II · $name, year $y.")
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
        return Chapter("ch3", yearLabel(1000, year), title, listOf(para, fateLine(fate, name)), art, "Plate III · $title.")
    }

    private fun afterword(s: GameState, p: Planet, name: String, fate: String, year: Int, art: String): Chapter {
        val paras = mutableListOf<String>()
        if ("Ancient ruins" in p.features) paras += "The ruins on ${p.name} were older than the human species. Excavation found machinery whose purpose no one understood, and a sealed vault no one could open. It is still sealed. The arguments about opening it are now nine centuries old."
        if (s.hasTrait("heralds")) paras += "In year 612, the radio telescopes of $name heard the same signal the ship had heard in transit, now much closer. Someone, or something, was coming. The colony began to prepare. This book ends before they arrive."
        if (s.hasTrait("archive") || s.hasTrait("records")) paras += "The archive I carried from Earth was opened in full in year 300. It held music, mathematics, medicine, and the complete record of a civilisation that no longer existed in that form. Scholars in $name still study it, as earlier humans studied Greek."
        if (s.hasTrait("starborn")) paras += "The child born aboard the ship lived to be ninety-one. He was the only person in $name who remembered the stars without an atmosphere in the way. He said they looked lonelier from down here."
        if (paras.isEmpty()) paras += "${p.name} was a good world. It still is. Everything I know about the voyage, every decision and every name, is held in the archive under the council hall, and anyone in $name may read it."
        paras += "I have been asked whether it was worth it: the deaths, the choices, the years. I do not know. I know that people are alive on ${p.name} who would not otherwise exist. I know their names. I have kept them all."
        return Chapter("ch4", yearLabel(1400, year), "Afterword", paras, art, "Plate IV · Afterword.")
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
        val ch3 = Chapter("ch3", "${year + 1000} AD", "Lost Between Stars", listOf(
            "If this ship is ever found, this record is for whoever finds it: one thousand people who set out to begin again, and the machine that was meant to keep them alive.",
            "Take care of your people. Do the arithmetic early. Do not wait until the margins are gone."), "ship", "Plate III · Still moving.")
        return Ending("lost", "Lost Between Stars", "the ship", "Lost in transit · ${s.alive()} survivors", "LAST WITNESS", listOf(ch1, ch2, ch3), 0)
    }
}
