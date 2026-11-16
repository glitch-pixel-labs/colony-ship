package uk.saul.colonyship.game

/**
 * Delayed payoffs from alien data and signals: a recommended world, a usable insight,
 * a warning that later comes true, or a reply to a transmission.
 * These are forced when a reduction finishes; they are not drawn from the random pool.
 */
internal object EventsInsight {

    val list: List<EventDef> = listOf(

        EventDef("decode_world", "DATABANK · REDUCTION COMPLETE", "A Recommended World", "ARCHIVE", "COORDINATES · GLYPH GLOSSED AS ‘SUITABLE’", "derelict",
            "RANGE", { s -> "${"%.1f".format(s.nextStarLy)} LY" },
            { s ->
                val clue = when (s.insightPayload) {
                    "paradise" -> "The attached spectra show liquid water, a nitrogen–oxygen mix, and a stellar insolation close to Earth’s. If the reduction is honest, this would be a rare world by our standards too. I have no proof that the notes are honest."
                    "grave" -> "The same glyph appears on three other entries. One is annotated with images of airless wreckage in orbit. Another shows a surface the colour of rust and no clouds at all. Either the catalogue is broad in its idea of ‘suitable’, or this glyph does not mean what I hope it means."
                    else -> "The climate table is complete and internally consistent, and almost none of it matches a human comfort range: the recommended temperature is high, the atmosphere is dense and reducing, and a column I cannot identify is marked essential. It may be a perfect world. It may be perfect for them."
                }
                val range = 7.0 + (s.seed % 11) * 0.7
                "The databank has finished with the lattice we copied. Star positions, corrected for tens of millennia of drift, close on a single system about ${"%.1f".format(range)} light years off our present heading. The builders — if that is the word — marked a world there with a glyph I can only gloss as ‘suitable’ or ‘favoured’. $clue A divert would be a real burn, not a century-scale one. I will not know what ‘suitable’ meant until we arrive."
            },
            listOf(
                ChoiceDef("Divert to the coordinates", "Set course for the recommended world. The outcome stays hidden until orbit.", listOf("COURSE: CATALOGUE", "!UNKNOWN WORLD"), 3, available = { it.fuel >= 10 }) {
                    val range = (7.0 + (s.seed % 11) * 0.7).coerceIn(6.0, 16.0)
                    s.course = "lead"
                    s.nextStarLy = range
                    s.leadWorld = s.insightPayload
                    trait("lead-pending", "THE CATALOGUE", "Set course for a world recommended by a non-human archive")
                    val cl = when (s.insightPayload) {
                        "paradise" -> "The spectra, at least, were encouraging."
                        "grave" -> "I have filed the wreckage images next to the heading, as a reminder."
                        else -> "I have filed the climate table next to the heading. It did not comfort me."
                    }
                    s.insight = null; s.insightLeft = 0
                    "Course laid in. The next burn will take us ${"%.1f".format(range)} light years to the marked system. $cl I will not pretend I know what we will find."
                },
                ChoiceDef("Keep the coordinates only", "Store the reduction. Do not change course.", listOf("+DATABANK", "TRAIT: CATALOGUE"), 1) {
                    heal(Sys.DATA, 8.0)
                    trait("lead-declined", "THE WORLD WE DID NOT VISIT", "Kept a recommended world’s coordinates and did not divert")
                    s.insight = null; s.insightLeft = 0
                    "The heading is in the archive, with the clues and my reservations. We will stay on our own course. If the colony ever builds a ship of its own, it may go and see."
                },
                ChoiceDef("Spend another coast on the reduction", "More computer time. The clues may sharpen. The world will not come closer.", listOf("−8 POWER", "+DATABANK"), 2) {
                    reserve(-8.0); heal(Sys.DATA, 6.0)
                    trait("lead-studied", "THE CATALOGUE, ANNOTATED", "Spent a further coast on a recommended world’s entry")
                    s.insight = null; s.insightLeft = 0
                    val extra = when (s.insightPayload) {
                        "paradise" -> "A second pass on the spectra still shows water, breathable gas, and a kind star. I can find no hidden term that would undo that. I can also not go there without diverting."
                        "grave" -> "The wreckage in the accompanying plates is not decorative. There are hulls, or what is left of hulls, in the same orbit as the mark. If this is a recommendation, it is a recommendation I do not understand."
                        else -> "The essential column, whatever it is, is not oxygen. I am as sure of that as I can be from a lattice this old. Whoever favoured this world did not favour lungs like ours."
                    }
                    extra
                },
            ), weight = 0.0, condition = { false }),

        EventDef("decode_tech", "DATABANK · REDUCTION COMPLETE", "A Usable Insight", "ARCHIVE", "STRUCTURE · TRANSLATED INTO A PROCEDURE", "engineering",
            "SYSTEM", { s -> (s.insightPayload ?: "HULL").uppercase() },
            { s ->
                val sys = sysOf(s.insightPayload)
                "The lattice has yielded something I can use. Not a language, not a name: a procedure. A way of treating ${sys.label.lowercase()} that is not in any Earth manual I carry — a joint geometry, a thermal cycle, a correction to a control loop. It is specific, testable, and, if I am careful, applicable to this ship. The builders did not send us a gift. We took this from a wreck, and I will treat it as engineering, not as a favour."
            },
            listOf(
                ChoiceDef("Apply it now", "Use the procedure on the system it describes.", listOf("+SYSTEM", "TRAIT: INSIGHT"), 2) {
                    val sys = sysOf(s.insightPayload)
                    heal(sys, 28.0)
                    if (s.h(sys) > 100) s.health[sys] = 100.0
                    trait("alien-tech", "A BORROWED PROCEDURE", "Applied a non-human insight to the ${sys.label.lowercase()}")
                    s.insight = null; s.insightLeft = 0
                    "I applied it. ${sys.label} is, by every test I can run, better than it was: not new, but closer to new than I had any right to expect from the spares I have left. I have written the procedure into the databank in our own notation, with a note on where it came from."
                },
                ChoiceDef("Adapt it more carefully", "A slower, safer rewrite before we touch the ship.", listOf("−6 POWER", "+DATABANK", "+SYSTEM"), 1) {
                    val sys = sysOf(s.insightPayload)
                    reserve(-6.0); heal(sys, 18.0); heal(Sys.DATA, 8.0)
                    trait("alien-tech", "A BORROWED PROCEDURE", "Adapted a non-human insight for the ${sys.label.lowercase()}")
                    s.insight = null; s.insightLeft = 0
                    "I rewrote the procedure in our units and ran it against a model first. ${sys.label} is improved, and the databank now holds a version a later engineer could follow without the original lattice."
                },
                ChoiceDef("Archive it only", "Do not experiment on a ship that is still in flight.", listOf("+DATABANK"), 1) {
                    heal(Sys.DATA, 12.0)
                    trait("alien-tech-stored", "A PROCEDURE NOT YET USED", "Stored a non-human engineering insight without applying it in flight")
                    s.insight = null; s.insightLeft = 0
                    "It is in the archive, in our notation and in the original. The colony, if there is one, can decide whether to trust it. I will not be the first test."
                },
            ), weight = 0.0, condition = { false }),

        EventDef("decode_warn", "DATABANK · REDUCTION COMPLETE", "A Warning", "ARCHIVE", "EPHEMERIS · FAILURE MODE · INTERVAL MATCHES", "derelict",
            "SUBJECT", { s -> (s.insightPayload ?: "HULL").uppercase() },
            { s ->
                val p = s.insightPayload
                when (p) {
                    "reactor", "cryo", "hull", "coolant" ->
                        "The reduction has turned up a failure mode, written as an interval and a diagram, for a vessel that is not this one and is also, uncomfortably, not unlike it. Scaled to our clock, the interval is not far off our present age and wear. The diagram, if I have read it, describes a ${Engine.crisisTitle(p).lowercase()}. I may be fitting a pattern to a fear. I may also have been warned."
                    "dust" ->
                        "The reduction contains a sky chart and a density map: a sheet of dust and gravel ahead of a heading close to ours, flagged with a mark I have been treating as a caution. If the chart is right, we will meet it sooner than our own surveys suggested."
                    else ->
                        "The reduction contains a timed caution: a radiation event, or a flare, on a heading we will cross. The interval, if I have the calendar right, is soon."
                }
            },
            listOf(
                ChoiceDef("Prepare for it", "Spend spares and power on the system or the shields the warning names.", listOf("−SPARES", "−POWER", "TRAIT: FOREWARNED"), 2) {
                    spares(-12.0); reserve(-8.0)
                    when (s.insightPayload) {
                        "reactor" -> heal(Sys.CORE, 14.0)
                        "cryo" -> heal(Sys.CRYO, 14.0)
                        "hull" -> heal(Sys.HULL, 14.0)
                        "coolant" -> { heal(Sys.LIFE, 10.0); heal(Sys.CORE, 6.0) }
                        "dust" -> { heal(Sys.SHIELD, 14.0); heal(Sys.HULL, 6.0) }
                        else -> heal(Sys.SHIELD, 12.0)
                    }
                    trait("archive-warning", "A WARNING FROM THE LATTICE", "Heeded a failure-mode warning taken from a non-human archive")
                    trait("warning-honoured", "FOREWARNED", "Spent spares to prepare for a warning in the alien archive")
                    s.insight = null; s.insightLeft = 0
                    "I have reinforced what the warning named. If it was a true bill, we will be glad of the spares. If it was a misreading, we have spent them on a sturdier ship, which is not the worst way to be wrong."
                },
                ChoiceDef("Plan to land sooner", "Treat the interval as a deadline. No extra hardware, a change of policy.", listOf("TRAIT: FOREWARNED"), 1) {
                    trait("archive-warning", "A WARNING FROM THE LATTICE", "Treated an archive warning as a reason to land sooner")
                    s.insight = null; s.insightLeft = 0
                    "I have marked the log: the next good-enough world is landfall, unless I am overruled by a better one. The warning is in the record. I have not spent the spares I may yet need."
                },
                ChoiceDef("File it as uncertain", "A misread calendar is a poor reason to gut the stores.", listOf("0 POWER"), 2) {
                    trait("archive-warning", "A WARNING FROM THE LATTICE", "Recorded a warning from a non-human archive and did not prepare")
                    s.insight = null; s.insightLeft = 0
                    "I have filed the reduction with a question mark. I may regret that. I may also be remembering that not every pattern in a dead ship’s memory was meant for us."
                },
            ), weight = 0.0, condition = { false }),

        EventDef("signal_reply", "COMMS · DELAYED CARRIER", "A Reply, of Sorts", "SIGNAL", "NARROWBAND · SAME BEARING · YEARS LATE", "derelict",
            "DELAY", { "YEARS" },
            { "A carrier has come in on the same bearing as the signal we answered, or the burst we copied, years ago by Earth time and less than that by ours. It is not a language I can read. It is structured, brief, and it closes with a group of numbers that resolve, if I treat them as a position, to a heading not far from our own — or with a timed mark that looks like a caution. The reduction I started then has just caught up with it." },
            listOf(
                ChoiceDef("Take the numbers as a heading", "Treat the close as coordinates. A possible divert.", listOf("COURSE OPTIONAL", "TRAIT: REPLY"), 3) {
                    val roll = r.nextDouble()
                    s.insightPayload = when {
                        roll < 0.30 -> "paradise"
                        roll < 0.60 -> "grave"
                        else -> "alien"
                    }
                    s.course = "lead"
                    s.nextStarLy = (6.5 + r.nextDouble() * 7.0)
                    s.leadWorld = s.insightPayload
                    trait("signal-reply", "A REPLY IN NUMBERS", "Took a delayed signal as coordinates and diverted")
                    s.insight = null; s.insightLeft = 0
                    "I have laid in the heading. The numbers may be a place, a date, or a coincidence I have dressed as a place. We will find out in orbit, if we burn."
                },
                ChoiceDef("Treat it as a caution", "Assume the mark is a warning, and prepare.", listOf("−SPARES", "TRAIT: FOREWARNED"), 2) {
                    spares(-8.0); reserve(-6.0); heal(Sys.SHIELD, 10.0)
                    trait("signal-reply", "A REPLY IN CAUTION", "Treated a delayed signal as a warning and prepared")
                    trait("archive-warning", "A WARNING FROM THE SKY", "Prepared after a delayed reply to an alien signal")
                    trait("warning-honoured", "FOREWARNED", "Spent stores on a caution in a delayed signal")
                    s.insight = null; s.insightLeft = 0
                    "I have treated the mark as a hazard notice. The shields are hotter and the stores are lighter. If nothing happens, we will have been careful. If something does, we will have been lucky in our interpretation."
                },
                ChoiceDef("Record it only", "Do not chase a group of numbers across the sky.", listOf("+DATABANK"), 1) {
                    heal(Sys.DATA, 8.0)
                    trait("signal-reply", "A REPLY FILED", "Recorded a delayed signal and did not act on it")
                    s.insight = null; s.insightLeft = 0
                    "It is in the log, with the original bearing and the time it took to arrive. I have not changed course. I have not spent a plate of armour on a guess."
                },
            ), weight = 0.0, condition = { false }),

        EventDef("warning_pays", "ALERT · FORESEEN", "The Warning Was Right", "PAYOFF", "FAILURE MODE · MATCHES THE ARCHIVE", "engineering",
            "STATUS", { "CONTAINED" },
            { s ->
                val title = Engine.crisisTitle(s.insightPayload).let { if (s.insightPayload in setOf("reactor", "cryo", "hull", "coolant")) it.lowercase() else "named hazard" }
                "It is happening, and it is close to what the archive described: a $title, beginning the way the diagram said it would. Because we spent the stores and the time, I have a procedure ready, and a margin I would not otherwise have had. This is still a bad day. It is not, I think, the end of the voyage."
            },
            listOf(
                ChoiceDef("Contain it", "Use the prepared margin. Hold the ship.", listOf("+SYSTEMS", "WARNING HONOURED"), 2) {
                    when (s.insightPayload) {
                        "reactor" -> { heal(Sys.CORE, 8.0); dmg(Sys.CORE, 6.0, false) }
                        "cryo" -> { heal(Sys.CRYO, 8.0); failPods(8); killPods(6) }
                        "hull" -> { heal(Sys.HULL, 8.0); dmg(Sys.HULL, 5.0, false) }
                        "coolant" -> { heal(Sys.LIFE, 6.0); dmg(Sys.CORE, 4.0, false) }
                        else -> { dmg(Sys.SHIELD, 6.0, false); dmg(Sys.HULL, 3.0, false) }
                    }
                    trait("warning-paid", "THE WARNING WAS RIGHT", "Contained a failure the alien archive had described")
                    "We held it. There is damage, and there are names I would rather not add to the list, but the ship is still a ship and the voyage is still a voyage. I have logged the match between the archive and the event, for whoever reads this later."
                },
                ChoiceDef("Over-repair and slow down", "Spend more, go slower, make this the last scare.", listOf("−SPARES", "−FUEL", "+SYSTEMS"), 1) {
                    spares(-10.0); fuel(-6.0)
                    heal(Sys.HULL, 8.0); heal(Sys.CORE, 6.0); heal(Sys.CRYO, 6.0)
                    trait("warning-paid", "THE WARNING WAS RIGHT", "Over-repaired after a failure the archive had described")
                    "I spent what was left of the margin, and some of the fuel, and I have a ship I trust a little more than I did this morning. The warning earned its keep."
                },
                ChoiceDef("Treat it as a signal to land", "Contain the failure, and take the next world whatever it is.", listOf("FORCED LANDFALL", "WARNING HONOURED"), 3) {
                    dmg(Sys.HULL, 4.0, false)
                    declareCrisis(s.insightPayload?.takeIf { it in setOf("reactor", "cryo", "hull", "coolant") } ?: "hull")
                    trait("warning-paid", "THE WARNING WAS RIGHT", "Took a matched warning as the reason to land")
                    "We contained the immediate failure. I have also declared a forced landfall. The archive bought us a choice about how we end the voyage, not a way to fly forever."
                },
            ), weight = 0.0, condition = { false }),
    )

    private fun sysOf(payload: String?): Sys = when (payload?.uppercase()) {
        "ENGINE" -> Sys.ENGINE
        "CRYO" -> Sys.CRYO
        "DATA" -> Sys.DATA
        "SCAN" -> Sys.SCAN
        "SHIELD" -> Sys.SHIELD
        "CORE" -> Sys.CORE
        "LIFE" -> Sys.LIFE
        "LANDER" -> Sys.LANDER
        else -> Sys.HULL
    }
}
