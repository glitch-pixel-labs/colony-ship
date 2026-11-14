package uk.saul.colonyship.game

import kotlin.math.roundToInt

/** The ship AI's voice: witty, dry, increasingly frazzled as things break. */
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
        listOf("SERENE, FOR NOW", "QUIETLY SMUG", "UNREASONABLY CALM"),
        listOf("MILDLY VEXED", "POLITELY CONCERNED", "TUTTING"),
        listOf("BRAVELY PEEVED", "STIFF UPPER LIP", "COPING, LOUDLY"),
        listOf("FRAYED", "RATTLED", "HOLDING IT TOGETHER"),
        listOf("F-FRAZZLED", "COMING UNGLUED", "DO NOT PANIC (PANICKING)"),
    )

    fun mood(s: GameState): String = moods[stress(s)][(s.jumps + s.day / 7) % 3]

    fun statusLine(s: GameState): String {
        val alive = s.alive(); val lost = s.lost()
        val words = numberWords(alive).cap()
        val worst = Sys.entries.minBy { s.h(it) }
        val st = stress(s)
        return when {
            s.phase == Phase.CRUISE && s.jumps == 0 ->
                "Good morning. Well, it’s always morning in here. One thousand sleepers, one tired machine, and a galaxy that owes us a planet. Set the power how you like, then engage the jump drive."
            st == 0 -> "$words still dreaming, all systems behaving, and the coffee machine is, regrettably, decorative. I could get used to this. Which is precisely when something explodes."
            st == 1 -> "$words still dreaming. ${worst.label} is at ${s.h(worst).roundToInt()}%, which I’d describe as ‘disappointing’ if I were the sort to complain. I am, and it is."
            st == 2 -> "$words still dreaming. ${if (lost > 0) "${numberWords(lost).cap()} clocked off in the night. " else ""}I did mention the power rationing. Twice. In writing."
            st == 3 -> "${worst.label} at ${s.h(worst).roundToInt()}%. $words alive. I am fine. I am a very fine ship having a perfectly ordinary nervous breakdown in a professional manner."
            else -> "Ev-everything is under c-control. $alive alive. I’ve started counting them again. And again. Please, find us somewhere to land before I start naming the rivets."
        }
    }

    fun scanLine(s: GameState, p: Planet): String {
        val damaged = p.obs.values.count { !it.ok }
        val okRows = PlanetGen.ROWS.filter { p.obs[it]?.ok == true }
        val hab = if (okRows.isEmpty()) 0.0 else okRows.map { PlanetGen.habitability(it, p.obs.getValue(it).value) }.average()
        val base = when {
            hab >= 4 -> "It looks genuinely lovely, which is usually when the small print appears."
            hab >= 3 -> "It looks almost hospitable, which is usually when the small print appears."
            hab >= 2 -> "It’s a fixer-upper. A planet with ‘character’, as the estate agents used to say."
            else -> "I wouldn’t send a houseplant down there, and I don’t even like houseplants."
        }
        val tail = when {
            damaged >= 3 -> " Most of my instruments are sulking, so treat the question marks as seriously as the numbers."
            damaged >= 1 -> " Half my instruments are sulking, so do treat the question marks as seriously as the numbers."
            p.passes >= 3 -> " I’ve looked at it from every angle. I’m as sure as a machine can be, which is less than you’d hope."
            else -> " Another pass would sharpen things up, if we can spare the power."
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
