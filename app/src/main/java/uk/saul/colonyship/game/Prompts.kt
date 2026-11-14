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

    private fun yl(ch: Chapter) = ch.yearLabel.lowercase().replaceFirstChar { it.uppercase() }

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
        "storm" -> "the colony ship ploughs through a blazing micrometeoroid storm, streaks of glowing debris, impact flashes on the hull."
        "flare" -> "a colossal coronal flare erupts from an orange star and arcs toward the ship."
        "derelict", "sister" -> "a vast dark derelict vessel drifts in a teal nebula, a lone light glowing inside it."
        "signal" -> "the ship's dish array turns toward a faint pulsing signal among the stars."
        "cascade", "elders", "coolant", "dream", "stowaway", "vault" -> "inside the cryopod bay, rows of frosted sleep pods, amber warning lights blinking, cold mist on the floor."
        "breach" -> "a hull breach venting glittering air into space from the ship's ring section."
        "comet" -> "drones grapple glittering comet ice chunks beside the ship."
        "core", "drift", "lonely", "party" -> "the ship's AI core, a single glowing amber-and-teal camera eye surrounded by chunky dials, in a dark chamber."
        "rogue" -> "a dark starless rogue planet looms close as the ship swings past."
        "giant" -> "the ship skims the banded upper clouds of a huge orange gas giant with rings."
        "reactor", "chief", "petition" -> "the engineering deck, sparking conduits around a glowing teal fusion reactor, a crew member in a frosted jumpsuit working on it."
        "blight", "garden" -> "a hydroponic garden deck inside the ship, racks of plants under warm lamps."
        "triage" -> "a medical bay with a physician tending thawing colonists beside open cryopods."
        "record" -> "an archivist writing in a leather logbook by the glow of the AI's screens."
        "nebula" -> "the ship enters a glowing teal ion nebula crackling with lightning."
        "dust" -> "a haze of fine glittering interstellar dust sanding the ship's forward armour."
        "ghost" -> "a dim empty deck with a single drone light, and a small rat silhouette."
        "probe" -> "an ancient tiny Earth space probe with a golden plate drifts beside the giant colony ship."
        "wave" -> "space itself ripples like water around the ship as a gravity wave passes."
        "charts" -> "a glowing holographic star chart over a retro console, stars slightly misaligned."
        else -> "the colony ship crossing deep space."
    }
}
