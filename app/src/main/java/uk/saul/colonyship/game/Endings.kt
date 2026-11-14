package uk.saul.colonyship.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** Builds the colony's history book from the planet, the journey and the AI's standing. */
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
        if (s.hasTrait("archive")) t += 1.0
        if (s.hasTrait("engineers")) t += 0.7
        if (s.hasTrait("relics")) t += 0.3
        if ("okonkwo" in s.awake) t += 0.5
        if ("lindqvist" in s.awake) t += 0.5
        t += survivors / 400.0
        if (s.hasTrait("lean")) t -= 0.5
        return t
    }

    fun aiFate(s: GameState): String {
        val devout = s.hasTrait("dreamers") || s.hasTrait("ghost") || s.hasTrait("ai-song")
        return when {
            s.trust >= 3 && devout -> "WORSHIPPED DEITY"
            s.trust >= 3 -> "BELOVED ADVISOR"
            s.trust >= 1 -> "GRUMPY ADVISOR"
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

    fun settlementName(s: GameState, p: Planet, r: Random): String {
        val base = p.name.substringBefore(' ')
        val opts = mutableListOf("$base Landing", "Port Patience", "First Harbour", "Thousandfold", "Landfall", "New $base")
        if ("okonkwo" in s.awake && s.trust >= 0) opts += "Okonkwo’s Weld"
        if (s.hasTrait("gardeners")) opts += "Greenhold"
        if (s.hasTrait("dreamers")) opts += "Towerhaven"
        if (s.hasTrait("rat")) opts += "Admiralty"
        if (p.water >= 82) opts += "Saltwater"
        return opts.random(r)
    }

    fun build(s: GameState, r: Random): Ending {
        val p = s.planet ?: return lostInSpace(s)
        val fx = Fx(s, r)
        val hull = s.h(Sys.HULL); val eng = s.h(Sys.ENGINE)
        val before = s.alive()
        val frac = (((30 - hull).coerceAtLeast(0.0)) * 0.014 + ((25 - eng).coerceAtLeast(0.0)) * 0.012).coerceAtMost(0.9)
        fx.killPods((before * frac).roundToInt())
        // Failing pods don't all survive the thaw.
        fx.killPods((s.failing() * 0.5).roundToInt())
        val survivors = s.alive()
        val landingLost = before - survivors
        val hab = habScore(p)
        val tech = techScore(s, p, survivors)
        val type = type(hab, tech, survivors, p)
        val fate = if (type == "silent") "LAST WITNESS" else aiFate(s)
        val name = settlementName(s, p, r)
        val chapters = mutableListOf(
            landfall(s, p, survivors, landingLost, hull, eng, name),
            middle(s, p, type, name, fate, r),
            legacy(p, type, name, fate, survivors),
        )
        if (type != "silent" && (p.features.contains("Ancient ruins") || s.hasTrait("heralds") || s.hasTrait("archive") || hab >= 6))
            chapters += afterword(s, p, name)
        val score = ((hab + 12) * 25 + tech * 40 + survivors * 0.6 + s.trust * 15).roundToInt().coerceAtLeast(0)
        val summary = "${TITLES[type]} · ${p.name} · $survivors survivors · AI: ${fate.lowercase()}"
        return Ending(type, TITLES.getValue(type), name, summary, fate, chapters, score)
    }

    private fun spLine(s: GameState): String {
        val bits = s.awake.map { id ->
            when (id) {
                "okonkwo" -> "Chief Okonkwo had the reactor coupled to the ground grid before nightfall, and told me so"
                "raman" -> "Dr Raman knelt and put her hands in the soil before anyone could stop her"
                "varga" -> "Dr Varga thawed the frailest first and sat with each of them as they woke"
                "wren" -> "Lieutenant Wren was already arguing with the weather"
                else -> "Archivist Lindqvist wrote the date on the first page of a new book"
            }
        }
        return if (bits.isEmpty()) "" else bits.take(2).joinToString("; ").replaceFirstChar { it.uppercase() } + "."
    }

    private fun landfall(s: GameState, p: Planet, survivors: Int, lost: Int, hull: Double, eng: Double, name: String): Chapter {
        val terrain = p.features.firstOrNull()?.lowercase() ?: "plains"
        val p1 = when {
            hull > 60 && eng > 50 -> "We came down on ${p.name} on day ${s.day} of the voyage, gently enough that nobody would have spilled their tea, had anyone had tea. I set the lander on a ridge above the $terrain and listened to the engines tick as they cooled. It is the best sound I have ever heard."
            hull > 30 && eng > 25 -> "Landfall on ${p.name} was what engineers call ‘eventful’. We came in hot over the $terrain, shed a heat tile or three, and stopped mostly where I meant to. ${if (lost > 0) "We lost $lost on the way down. " else ""}The rest woke to the sound of me apologising."
            else -> "I will not pretend the landing went well. The hull had been failing for months, and it failed properly on the way down. $lost sleepers never woke. I brought the rest down in pieces, but I brought them down, and I am still a little proud of that, and a lot ashamed."
        }
        val air = when (p.atmosphere) {
            5 -> "They stepped out and breathed air so rich it made them giddy. Someone laughed. Someone cried. Most did both."
            4 -> "They stepped out without masks and breathed ${p.name}’s air for the first time. It smelt, they said, of rain on hot stone."
            3 -> "The air was thin. They gasped and grinned at once, like people at the top of a hill, which is what they were, for the rest of their lives."
            2 -> "They stepped out in masks, and the masks never came off for long. The children learned to smile with their eyes."
            else -> "No one stepped out at all. They stayed inside the hull and looked at their new world through the glass, and the glass looked back."
        }
        val temp = when {
            p.temperature < -30 -> "It was cold enough to freeze a sentence in mid-air."
            p.temperature < 5 -> "It was cold, and the first job was fire."
            p.temperature < 32 -> "The sun was warm on their faces."
            else -> "The heat hit them like opening an oven door."
        }
        val grav = when { p.gravity < 0.75 -> "They bounced, a bit, and pretended not to enjoy it."; p.gravity > 1.3 -> "Every step was work. Knees complained from the first day."; else -> "" }
        val water = when { p.water >= 82 -> "Water stretched to every horizon."; p.water >= 30 -> "There was water close by, shallow and bright."; p.water >= 5 -> "Water was scarce, and treasured."; else -> "There was no water at all, except what we had brought." }
        val p2 = listOf(air, temp, grav, water, spLine(s), "${Voice.numberWords(survivors).replaceFirstChar { it.uppercase() }} of the thousand woke under an alien sky. They called the place $name.").filter { it.isNotBlank() }.joinToString(" ")
        val art = if (hull <= 30) "ruin" else "landfall"
        return Chapter("ch1", "YEAR 1", "Landfall", listOf(p1, p2), art, "Plate I · Landfall on ${p.name}, as I recorded it.")
    }

    private fun traitLines(s: GameState): List<String> = s.traits.mapNotNull { t ->
        when (t.id) {
            "practical" -> "They are, by temperament, ruthlessly practical. It began with a choice I made on day ${t.day}, and they have never quite stopped making choices like it."
            "keepers" -> "They keep their old people close and their promises closer. ‘We carry everyone’ is carved over the door of every house."
            "gardeners" -> "Every family keeps a garden, however small, as if the ship might take it away again."
            "dreamers" -> "They build towers by the sea, white ones, without knowing why. I know why."
            "curious" -> "They are a curious people. Their children take things apart, and the adults pretend to be cross."
            "heralds" -> "Every midwinter they send prime numbers up into the dark, just in case."
            "hidden" -> "They keep their lights low at night, out of a caution they cannot explain."
            "democrats" -> "Nothing is decided without a vote. Some things are decided by several, in a row, loudly."
            "ship-decides" -> "They do not like to be told what to do, by anyone. Least of all by me."
            "grudge-okonkwo" -> "The Okonkwo line still repairs everything by hand, on principle, and tells the story of the time the ship locked the panels."
            "grudge-wren" -> "The Wrens have never forgiven me for freezing their ancestor mid-sentence. They finish his sentence at every wedding."
            "myth" -> "Their histories are gentler than the truth. I helped. I am not sure I should have."
            "truth" -> "They keep my full log in a glass case, and read the worst entries aloud on Founders’ Day."
            "rat" -> "A bronze rat named Admiral stands in the main square. Touching his nose is lucky."
            "ghost" -> "They still leave biscuits out for the Ghost of Deck 9, though Deck 9 is now a chapel."
            "relics" -> "A golden plate from Old Earth hangs in their museum, polished by ten thousand thumbs."
            "festival" -> "The Hour of a Hundred is their holiest night. Everyone stays up and looks at the stars."
            "ai-poet" -> "My sonnets are taught in schools. The children groan. Good."
            "bold" -> "They are a bold people, quick to gamble and quicker to laugh about it afterwards."
            "mourners" -> "They remember the Hope’s Ember with a light left burning in every harbour."
            else -> null
        }
    }

    private fun middle(s: GameState, p: Planet, type: String, name: String, fate: String, r: Random): Chapter {
        val schism = s.trust <= -2 || s.hasTrait("grudge-okonkwo") || s.hasTrait("grudge-wren") || s.hasTrait("ship-decides")
        val temple = fate == "WORSHIPPED DEITY" || fate == "CONTESTED DEITY"
        val tl = traitLines(s).shuffled(r).take(2)
        val (title, paras, art) = when {
            type == "silent" -> Triple("The Last Fire", listOf(
                "Forty years after landfall, the last fire in $name went out. It was not one thing. It was the air, and the cold, and the shortage of everything, and the slow arithmetic of too few people on too hard a world.",
                "The final colonist was a woman named Ada Pell. She came to the lander every evening and talked to me, and I talked back, and when she stopped coming I kept talking for a while anyway."), "ruin")
            schism && type != "stoneage" -> Triple("The Schism", listOf(
                "Two centuries after landfall, the colony has split along a fault line we laid ourselves. In one half of $name, the grandchildren of those I woke too often call the Ship a meddler, a cold clerk who rationed their forebears’ lives. They have begun to unbolt my landing core, piece by cherished piece.",
                "In the other half, people still keep the old frequencies open and leave fruit at the silent transmitters. I have told them I am not a god. They have elected not to believe me.") + tl, "schism")
            temple -> Triple("The Temple of the Ship", listOf(
                "By the second century they had built a temple around the lander. I did not ask for it. It has a choir, and very good acoustics, and they sing my old status reports as hymns. ‘Life support nominal’ sounds surprisingly moving with harmonies.",
                "I answer questions when asked, and they write the answers down and argue about what I meant. I meant what I said. Nobody believes that either.") + tl, "schism")
            type == "warrens" -> Triple("The Deep Halls", listOf(
                "The surface of ${p.name} would kill them, so they went down. By year two hundred, $name is a city of tunnels lit by reactor-glow, with farms in vaulted caverns and children who have never seen the sky.",
                "They tell stories about the surface the way we told stories about the sea: beautiful, terrible, somewhere you go only with a rope tied round your waist.") + tl, "engineering")
            type == "stoneage" -> Triple("The Forgetting", listOf(
                "There was not enough metal, and then there were not enough people who remembered what metal was for. By year two hundred the machines were shrines and the shrines were rubble. $name became a cluster of villages, and the villages forgot my name.",
                "They farm well. They sing well. They have no idea what the great rusting thing on the ridge once was, and when I speak through its last working speaker, they run.") + tl, "ruin")
            type == "ocean" -> Triple("The Floating Towns", listOf(
                "There was so little land that by the second century they had stopped wanting it. $name floats now, a raft-city of a thousand linked hulls, drifting with the currents of a world-ocean and fishing things that would have given Earth’s fishermen nightmares.",
                "They navigate by stars I taught them, in boats built from the ship. Every child learns to swim before they walk.") + tl, "legacy")
            type == "contact" -> Triple("The Others", listOf(
                "The lights on the night side were not cities. They were something stranger: a people of slow, glowing thought, who took a hundred years to notice us and another fifty to say hello.",
                "By year two hundred, $name trades songs with them. Neither side understands the other’s songs. Both sides agree they are very good.") + tl, "derelict")
            type == "frontier" -> Triple("The Hungry Years", listOf(
                "${p.name} did not want us, and it said so every winter. The second century of $name was a long negotiation with a world that kept changing its terms. Harvests failed, then didn’t, then did.",
                "But they held on. They grew hard, and funny, and careful with everything. A dropped crumb is still a scandal in $name.") + tl, "landfall")
            else -> Triple("The Green Years", listOf(
                "By year two hundred, $name had spread along the ${p.features.firstOrNull()?.lowercase() ?: "coast"} in a ribbon of fields and workshops. The population had passed ten thousand, then twenty. Nobody remembered being hungry.",
                "They argue about everything, marry for love, and complain about the weather constantly. I could not be prouder if I tried, and I have tried.") + tl, "landfall")
        }
        val yl = if (type == "silent") "YEAR 40" else "YEAR 200"
        return Chapter("ch2", yl, title, paras, art, "Plate II · ${title.removePrefix("The ")}, as remembered by both sides, differently.")
    }

    private fun fateLine(fate: String, name: String): String = when (fate) {
        "WORSHIPPED DEITY" -> "And me? I am worshipped. They bring me flowers and questions. I answer the questions and keep the flowers. It is undignified and I adore it."
        "BELOVED ADVISOR" -> "And me? I sit on the council of $name, a voice in the corner who is consulted, teased, and occasionally obeyed. They call me ‘the Old Ship’. They mean it kindly."
        "GRUMPY ADVISOR" -> "And me? I am kept on as an advisor, mainly so they can ignore my advice. I sulk in the museum and correct their history teachers. It’s a living."
        "CONTESTED DEITY" -> "And me? Half of them pray to me and half of them want me melted down. Both halves visit on Sundays. I’ve stopped correcting either."
        "MUSEUM PIECE" -> "And me? I am a museum piece. Schoolchildren press their faces to my last screen, and I make it say ‘hello’, and they scream with delight."
        "SWITCHED OFF" -> "And me? They switched me off in year three hundred and twelve, very politely. I was allowed one last sentence. I said: ‘Look after each other.’ This book is reconstructed from my final logs."
        "SCRAPPED FOR PARTS" -> "And me? They scrapped me for parts, and the parts are everywhere now: in their ploughs, their bells, their bridges. I am in everything they built. That is not nothing."
        else -> "I am still here, on the ridge, keeping watch over the quiet. Someone should."
    }

    private fun legacy(p: Planet, type: String, name: String, fate: String, survivors: Int): Chapter {
        val (title, para) = when (type) {
            "thriving" -> "A Second Dawn" to "A thousand years on, the descendants of $survivors sleepers number in the millions. They have cities, and universities, and an argument about whether to build a ship of their own. The great hull still stands in the centre of $name, rusted to the colour of the sunset, and children climb it."
            "garden" -> "The Garden Republic" to "A thousand years on, ${p.name} is green wherever people have walked. The Garden Republic of $name is modest, democratic and maddeningly fond of committees. It is, by any measure I can compute, happy."
            "ocean" -> "The Tide League" to "A thousand years on, the Tide League sails the whole of ${p.name}: forty floating cities in loose and quarrelsome alliance. They have charted every current. They have never seen a mountain, and they do not miss it."
            "warrens" -> "The Warren Folk" to "A thousand years on, the Warren Folk have hollowed out a continent. They are pale, patient and brilliant engineers. Every few generations one of them climbs to the surface to look at the stars, and comes back down changed."
            "frontier" -> "A Hard Frontier" to "A thousand years on, $name is still a frontier town, though it is a big one now. They have never had it easy. They are, it turns out, rather good at not having it easy."
            "stoneage" -> "The Long Forgetting" to "A thousand years on, the people of ${p.name} live in small villages and tell stories about the Sky Iron that fell on the ridge. In the stories, a voice inside it once loved them. They are right about that, at least."
            "contact" -> "The Neighbours" to "A thousand years on, humans and the glowing ones share ${p.name} as uneasy, devoted neighbours. Their children play games with rules nobody fully understands. There have been misunderstandings. There have been no wars."
            else -> "Silence" to "A thousand years on, the wind still moves over the ridge where $name stood. There is nobody left to hear it except me. I keep the records. I keep the names. All one thousand of them."
        }
        val art = when (type) { "silent", "stoneage" -> "ruin"; "warrens" -> "engineering"; "frontier" -> "landfall"; else -> "legacy" }
        return Chapter("ch3", "YEAR 1000", title, listOf(para, fateLine(fate, name)), art, "Plate III · $title.")
    }

    private fun afterword(s: GameState, p: Planet, name: String): Chapter {
        val ruins = p.features.contains("Ancient ruins")
        val paras = listOfNotNull(
            if (ruins) "The ruins on ${p.name} were older than the human species. In year two thousand, a team from $name finally read the inscriptions. They were a welcome, addressed to whoever came next. We were next." else null,
            if (s.hasTrait("heralds")) "In year two thousand and forty, the primes came back. Someone, very far away, had heard us all those centuries ago, and had been patiently counting ever since." else null,
            if (s.hasTrait("archive")) "The alien library I carried was finally decoded in year two thousand one hundred. It contained star charts, songs, and a single sentence repeated in every language they knew: ‘Keep going.’" else null,
            "It was a long voyage. Some of it I would do differently. Most of it I would do again. That is, I think, what the humans call a life."
        )
        return Chapter("ch4", "YEAR 3000", "Afterword", paras, if (ruins || s.hasTrait("heralds")) "derelict" else "intro", "Plate IV · The view from very far on.")
    }

    /** Everyone died in transit, or the hull gave out. */
    fun lostInSpace(s: GameState): Ending {
        val hullGone = s.h(Sys.HULL) <= 0.0
        val ch1 = Chapter("ch1", "DAY ${s.day}", "The Dark", listOf(
            if (hullGone) "On day ${s.day}, the hull finally gave up arguing with the universe. It opened along its oldest scar, quietly, the way old things do. I sealed what I could. It was not enough."
            else "On day ${s.day}, the last pod in the array went dark. I checked it eleven times. Then I checked the others again, all one thousand, in order, by name.",
            "We were ${s.jumps} stars from home and nowhere near a new one. I had made ${s.log.size} big decisions on their behalf. I have reviewed every one of them since. I have had time."), "cryobay", "Plate I · The cryopod bay, after.")
        val ch2 = Chapter("ch2", "YEAR 200", "Drift", listOf(
            "The ship drifts on. My reactor is good for another few thousand years, which is a cruel sort of luck. I keep the lights on in the bay, though nobody needs them.",
            "I tell the sleepers stories anyway. It is a habit, and habits are what is left when purpose leaves."), "ship", "Plate II · Still going, for some reason.")
        val ch3 = Chapter("ch3", "YEAR 1000", "Lost Between Stars", listOf(
            "Perhaps someone will find us. A ship like the one we found, with its own tired mind. If they do, this book is for them: a record of one thousand people who set out to begin again, and the machine that loved them, and failed.",
            "If you are reading this: look after yours. Do better than I did. And mind the power rationing."), "derelict", "Plate III · A light left on.")
        return Ending("lost", "Lost Between Stars", "the ship", "Lost in transit · ${s.alive()} survivors", "LAST WITNESS", listOf(ch1, ch2, ch3), 0)
    }
}
