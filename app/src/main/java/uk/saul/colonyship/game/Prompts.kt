package uk.saul.colonyship.game

/** Builds copy-ready image and video prompts from what actually happened in the run. */
object Prompts {
    const val STYLE = "1970s retro-futurist science-fiction paperback cover painting, gouache and airbrush, visible brush strokes, film grain, warm palette of burnt orange, mustard, teal and cream, dramatic rim lighting"
    const val NEG = "No text, no letters, no logos, no watermarks."

    fun shipCondition(s: GameState): String {
        val hull = s.h(Sys.HULL)
        val shield = s.h(Sys.SHIELD)
        return buildList {
            add(when { hull > 75 -> "a long ribbed colony ship with a rotating habitat ring, hull in good order"; hull > 45 -> "a weathered ribbed colony ship with a rotating habitat ring, scorched and patched plating"; else -> "a battered, half-ruined colony ship with a rotating habitat ring, gaping hull scars and trailing debris" })
            if (shield < 40) add("a flickering, failing teal shield bubble")
            if (s.awake.isNotEmpty()) add("tiny lit windows where ${if (s.awake.size == 1) "one crew member is" else "${s.awake.size} crew are"} awake")
        }.joinToString(", ")
    }

    fun planetLook(p: Planet): String {
        val surface = when {
            p.water >= 82 -> "a deep teal ocean world with swirling cloud bands"
            p.water >= 40 -> "a world of wide shallow seas and rust-orange continents"
            p.temperature < -30 -> "a frozen white-and-blue ice world"
            p.atmosphere <= 1 -> "a hazy, sulphur-yellow world"
            p.water < 5 -> "a dry, cracked desert world in ochre and red"
            else -> "a mottled world of ochre plains and teal lakes"
        }
        val f = p.features.take(2).joinToString(" and ") { it.lowercase() }
        return "$surface, with $f" + if (p.moons > 0) ", ${p.moons} small moon${if (p.moons > 1) "s" else ""} nearby" else ""
    }

    fun eventImage(s: GameState, ev: EventDef, entry: LogEntry?): String {
        val choice = entry?.choice?.takeIf { it.isNotBlank() }?.let { " The ship's AI has chosen to ${it.lowercase()}." } ?: ""
        return "$STYLE. Scene: ${ev.title}: ${sceneFor(ev.id)} Featuring ${shipCondition(s)}.$choice Day ${s.day} of the voyage, ${s.alive()} of 1,000 colonists still alive in cryosleep. Portrait composition, 2:3. $NEG"
    }

    fun eventVideo(s: GameState, ev: EventDef, entry: LogEntry?): String {
        val choice = entry?.choice?.takeIf { it.isNotBlank() }?.let { " Then the ship responds: ${it.lowercase()}." } ?: ""
        return "8-second cinematic shot, a $STYLE brought to life. Slow push-in. ${ev.title}: ${sceneFor(ev.id)} ${shipCondition(s).replaceFirstChar { it.uppercase() }}.$choice Gentle camera drift, particles and glow animate, subtle film flicker, ambient synth hum. Vertical 9:16. $NEG"
    }

    /** "YEAR 40 · 2302 AD" -> "Year 40 · 2302 AD". */
    fun yearTitle(label: String) = label.lowercase().replaceFirstChar { it.uppercase() }.replace(" ad", " AD").replace("ship year", "Ship year")
    private fun yl(ch: Chapter) = yearTitle(ch.yearLabel)

    fun chapterImage(planet: Planet?, settlement: String, ch: Chapter, traits: List<Trait>, survivors: Int): String {
        val world = planet?.let { planetLook(it) } ?: "deep interstellar space"
        val tr = traits.take(3).joinToString(", ") { it.label.lowercase() }
        return "$STYLE. Illustrated history-book plate, “${yl(ch)}: ${ch.title}”, for the colony of $settlement on $world. ${ch.paragraphs.firstOrNull()?.take(260) ?: ""} Colonists in cream and orange 1970s jumpsuits${if (tr.isNotBlank()) ", a culture known as $tr" else ""}, descended from $survivors founders. Landscape composition, 4:3. $NEG"
    }

    fun chapterVideo(planet: Planet?, settlement: String, ch: Chapter, traits: List<Trait>, survivors: Int): String {
        val world = planet?.let { planetLook(it) } ?: "deep interstellar space"
        return "10-second cinematic sequence, a painted 1970s retro-futurist paperback cover come to life. ${yl(ch)}, “${ch.title}”: the colony of $settlement on $world. ${ch.paragraphs.firstOrNull()?.take(200) ?: ""} Slow aerial drift over the scene, warm dusk light, gouache textures shimmering, soft analogue synth score. 16:9. $NEG"
    }

    private fun sceneFor(id: String): String = when (id) {
        "storm", "dust" -> "the colony ship ploughs through a dense front of interstellar dust, sparks of vaporised grit flaring on its ice bow shield."
        "flare" -> "a proton storm from a nearby flaring red dwarf washes over the ship, the hull glowing faintly, crew sheltering behind water tanks."
        "derelict" -> "a vast dark derelict vessel drifts beside the colony ship, a single light still burning inside it."
        "signal" -> "the ship's dish array turns toward a faint, regular radio source among the stars."
        "cascade", "elders", "coolant", "dream", "stowaway", "vault", "will", "drift" -> "inside the cryopod bay, rows of frosted sleep pods, amber warning lights blinking, cold mist on the floor."
        "breach", "bearing" -> "an engineer in a bulky suit welds a cracked seam on the ship's rotating ring, Milky Way behind."
        "comet", "organics" -> "drones grapple glittering comet ice beside the slowed ship."
        "core", "lonely", "firmware", "inspection" -> "the ship's AI core, a single glowing amber-and-teal camera eye surrounded by chunky dials, in a dark chamber."
        "rogue" -> "a dark starless rogue planet looms close as the ship passes in the gloom."
        "giant" -> "the ship skims past the banded clouds of a huge orange gas giant with rings."
        "reactor", "chief", "fuel", "radiator" -> "the engineering deck, conduits around a glowing teal fusion reactor, an engineer in a frosted jumpsuit at work."
        "petition", "record" -> "a small group of thawed crew in thermal blankets argue around a wardroom table under the AI's camera eye."
        "blight", "garden" -> "a hydroponic garden deck inside the ship, racks of plants under warm lamps."
        "triage", "pregnancy", "thawsick" -> "a medical bay with a physician tending a thawed colonist beside an open cryopod."
        "nebula" -> "the ship crosses the edge of a glowing ionised nebula."
        "pulsar" -> "a spinning pulsar's beams sweep past the ship like a lighthouse."
        "ghost" -> "a dim empty deck with a single drone light, and a small rodent silhouette in the beam."
        "probe" -> "an ancient tiny Earth space probe with a golden disc drifts beside the giant colony ship."
        "letter", "anniversary" -> "a thin blue Earth seen in memory, projected on a console screen inside the dark ship."
        "overtaken" -> "a faster, sleeker human ship overtakes the old colony ship, its drive flare bright."
        "charts" -> "a glowing holographic star chart over a retro console, stars slightly misaligned."
        else -> "the colony ship crossing deep space at relativistic speed, stars bunched ahead and blue-shifted."
    }
}
