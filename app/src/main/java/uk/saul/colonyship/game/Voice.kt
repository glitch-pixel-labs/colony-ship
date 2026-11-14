package uk.saul.colonyship.game

import kotlin.math.roundToInt

/** The ship AI's voice: dry, precise, serious. It tells the truth about the odds. */
object Voice {
    /** 0 = serene, 4 = coming apart at the seams. */
    fun stress(s: GameState): Int {
        val avg = Sys.entries.map { s.h(it) }.average()
        val worst = Sys.entries.minOf { s.h(it) }
        val podLoss = s.lost() / TOTAL_PODS.toDouble()
        val cons = listOf(s.air, s.water, s.food).min()
        var x = 0.0
        x += (100 - avg) / 18
        x += if (worst < 25) 1.2 else if (worst < 45) 0.6 else 0.0
        x += podLoss * 8
        x += if (cons < 20) 1.0 else 0.0
        return x.roundToInt().coerceIn(0, 4)
    }

    private val moods = listOf(
        listOf("NOMINAL", "STEADY", "ALL GREEN"),
        listOf("WATCHFUL", "ATTENTIVE", "MONITORING"),
        listOf("STRAINED", "RATIONING", "MARGINS THIN"),
        listOf("DEGRADED", "TRIAGE MODE", "FAILING SAFE"),
        listOf("CRITICAL", "LAST RESERVES", "LOSING THE SHIP"),
    )

    fun mood(s: GameState): String = moods[stress(s)][(s.jumps + s.day / 7) % 3]

    fun statusLine(s: GameState): String {
        val alive = s.alive(); val lost = s.lost()
        val words = numberWords(alive).cap()
        val worst = Sys.entries.minBy { s.h(it) }
        val st = stress(s)
        val dist = "%.1f".format(Physics.distanceFromSol(s))
        return when {
            s.phase == Phase.CRUISE && s.jumps == 0 ->
                "One thousand sleepers aboard, all stable. We are in the Sol system still, and the nearest candidate star is ${"%.1f".format(s.nextStarLy)} light years out. Set the power allocation, then light the drive. Every year we spend getting there, Earth will spend more."
            st == 0 -> "$words sleepers stable. All systems within tolerance. We are $dist light years from Sol; on Earth it is ${Physics.earthYear(s)}. Nothing is wrong. I am looking for what will be."
            st == 1 -> "$words sleepers stable. ${worst.label} is at ${s.h(worst).roundToInt()}% and falling slowly. Ships do not fail all at once. They fail one percentage point at a time."
            st == 2 -> "$words still alive.${if (lost > 0) " ${numberWords(lost).cap()} lost so far." else ""} The margins are thin. Each decision from here costs something we will not get back."
            st == 3 -> "${worst.label} at ${s.h(worst).roundToInt()}%. $words alive. I am prioritising the cryo array over everything else, including my own processors. We need a world soon."
            else -> "$alive alive. I am running on reserves and the ship is failing around me. Any world with air and water is better than this. Find one."
        }
    }

    fun scanLine(s: GameState, p: Planet): String {
        val damaged = p.obs.values.count { !it.ok }
        val okRows = PlanetGen.ROWS.filter { p.obs[it]?.ok == true }
        val hab = if (okRows.isEmpty()) 0.0 else okRows.map { PlanetGen.habitability(it, p.obs.getValue(it).value) }.average()
        val base = when {
            hab >= 4 -> "On these numbers, people could live here without walls. That is rare. Check what the numbers do not say."
            hab >= 3 -> "Survivable, with work. Not kind, but survivable."
            hab >= 2 -> "Marginal. A colony here would live inside its machines for generations, and some would die learning the planet."
            else -> "Hostile. Landing here would be a way of dying more slowly than in the pods."
        }
        val tail = when {
            damaged >= 3 -> " Most of my instruments are damaged. The question marks are not caution; they are ignorance."
            damaged >= 1 -> " Some instruments are damaged, so the question marks matter as much as the numbers."
            p.passes >= 3 -> " I have scanned it repeatedly. This is as certain as I can be from orbit."
            else -> " Another pass would reduce the error bars, at a cost in power."
        }
        return base + tail
    }

    fun numberWords(n: Int): String {
        if (n == 0) return "none"
        if (n == 1000) return "one thousand"
        val ones = listOf("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
        val tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
        fun two(x: Int) = if (x < 20) ones[x] else tens[x / 10] + if (x % 10 != 0) "-" + ones[x % 10] else ""
        val h = n / 100; val rem = n % 100
        return listOfNotNull(if (h > 0) ones[h] + " hundred" else null, if (rem > 0) (if (h > 0) "and " else "") + two(rem) else null).joinToString(" ")
    }
    private fun String.cap() = replaceFirstChar { it.uppercase() }
}
