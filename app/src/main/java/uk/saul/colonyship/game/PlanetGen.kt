package uk.saul.colonyship.game

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** Display-ready line for one readout row. */
data class Readout(val row: String, val damaged: Boolean, val main: String, val sub: String, val bars: Int, val warn: Boolean)

object PlanetGen {
    val ROWS = listOf("ATMOSPHERE", "GRAVITY", "TEMPERATURE", "WATER", "RESOURCES", "LIFE SIGNS", "FEATURES")

    private val pre = listOf("Cinn", "Vel", "Ost", "Mar", "Hesp", "Ty", "Cal", "Ar", "Bel", "Dor", "Ess", "Fen", "Gal", "Hal",
        "Ish", "Kor", "Lyr", "Mor", "Nem", "Or", "Pell", "Quor", "Ros", "Sel", "Tam", "Ul", "Vas", "Wyn", "Yar", "Zeph")
    private val mid = listOf("a", "e", "i", "o", "u", "ae", "ia", "")
    private val suf = listOf("bar", "ros", "thea", "mire", "dun", "lis", "vane", "cott", "wick", "ra", "nor", "sted", "holm", "ven", "dra", "mont")
    private val letters = listOf("b", "c", "d", "e")
    private val featurePool = listOf("Rings", "Rust deserts", "Long archipelagos", "Cyclones", "Glacier belts", "Volcanic chains",
        "Crystal forests", "Salt flats", "Twin sunsets", "Aurorae", "Tidally locked", "Canyon systems", "Floating kelp mats",
        "Ancient ruins", "Lights on the night side")

    fun name(r: Random): String {
        val n = pre.random(r) + mid.random(r) + suf.random(r)
        return n.replaceFirstChar { it.uppercase() } + " " + letters.random(r).uppercase()
    }

    fun generate(r: Random, jump: Int, distance: Double): Planet {
        // Later systems are slightly kinder, so persistence is rewarded (and the ship pays for it).
        val luck = (jump * 0.05).coerceAtMost(0.4)
        val atm = weighted(r, listOf(1.0, 1.4, 1.6, 1.4, 1.1 + luck * 2, 0.8 + luck))
        val gravity = (0.3 + r.nextDouble() * 1.6 + (if (r.nextDouble() < 0.5 + luck) (1.0 - (0.3 + r.nextDouble() * 1.6)) * 0.5 else 0.0)).coerceIn(0.25, 2.4)
        val temp = (-90 + r.nextInt(170) + (if (r.nextDouble() < 0.4 + luck) (15 - (-90 + r.nextInt(170))) / 2 else 0)).coerceIn(-110, 95)
        val water = when {
            temp < -40 -> r.nextInt(0, 15)
            temp > 60 -> r.nextInt(0, 8)
            else -> listOf(0, r.nextInt(3, 25), r.nextInt(25, 70), r.nextInt(55, 92), r.nextInt(85, 100)).random(r)
        }
        val resources = weighted(r, listOf(0.7, 1.2, 1.5, 1.4, 1.0, 0.6))
        val lifeBase = if (atm >= 3 && water > 5 && temp in -30..55) listOf(0.6, 1.4, 1.4, 0.9, 0.15) else listOf(2.0, 0.8, 0.15, 0.05, 0.08)
        val life = weighted(r, lifeBase)
        val feats = featurePool.filter { f ->
            when (f) {
                "Ancient ruins" -> r.nextDouble() < 0.08
                "Lights on the night side" -> life == 4
                "Floating kelp mats" -> water > 60 && life >= 2 && r.nextDouble() < 0.5
                "Glacier belts" -> temp < 0 && r.nextDouble() < 0.6
                "Rust deserts", "Salt flats" -> water < 50 && r.nextDouble() < 0.35
                "Long archipelagos" -> water in 40..92 && r.nextDouble() < 0.5
                else -> r.nextDouble() < 0.14
            }
        }.take(3).ifEmpty { listOf(featurePool.take(3).random(r)) }
        val hue = when {
            water > 75 -> 0.5f; temp < -30 -> 0.58f; atm <= 1 -> 0.18f; water < 15 -> 0.05f; else -> r.nextDouble(0.0, 1.0).toFloat()
        }
        return Planet(name(r), (distance * 10).roundToInt() / 10.0, weighted(r, listOf(1.0, 1.5, 0.8, 0.4)), atm,
            (gravity * 100).roundToInt() / 100.0, temp, water, resources, life, feats, hue)
    }

    private fun weighted(r: Random, w: List<Double>): Int {
        var x = r.nextDouble() * w.sum()
        w.forEachIndexed { i, v -> x -= v; if (x <= 0) return i }
        return w.lastIndex
    }

    fun trueValue(p: Planet, row: String): Double = when (row) {
        "ATMOSPHERE" -> p.atmosphere.toDouble(); "GRAVITY" -> p.gravity; "TEMPERATURE" -> p.temperature.toDouble()
        "WATER" -> p.water.toDouble(); "RESOURCES" -> p.resources.toDouble(); "LIFE SIGNS" -> p.life.toDouble()
        else -> p.features.size.toDouble()
    }
    fun span(row: String): Double = when (row) {
        "ATMOSPHERE" -> 3.0; "GRAVITY" -> 0.8; "TEMPERATURE" -> 60.0; "WATER" -> 40.0; "RESOURCES" -> 3.0; "LIFE SIGNS" -> 2.0; else -> 0.0
    }

    val ATM_NAMES = listOf("None to speak of", "Corrosive soup", "Thin, toxic", "Thin, breathable", "Breathable", "Dense, breathable")
    val ATM_SUBS = listOf("Trace gas · effectively vacuum", "Sulphur and chlorine compounds", "Low O₂ · CO₂ rich · lethal", "Low pressure · O₂ 14%", "N₂/O₂ · near Earth-normal", "High pressure · O₂ 26%")
    val RES_NAMES = listOf("Barren", "Poor", "Modest", "Good", "Rich", "Abundant")
    val LIFE_NAMES = listOf("None detected", "Microbial mats", "Plant analogues", "Animal life", "Signs of others")

    fun habitability(row: String, v: Double): Int = when (row) {
        "ATMOSPHERE" -> listOf(0, 0, 1, 3, 5, 4)[v.roundToInt().coerceIn(0, 5)]
        "GRAVITY" -> (5 - (abs(v - 1.0) / 0.15)).roundToInt().coerceIn(0, 5)
        "TEMPERATURE" -> (5 - (abs(v - 15) / 12)).roundToInt().coerceIn(0, 5)
        "WATER" -> when { v < 2 -> 0; v < 10 -> 1; v < 30 -> 3; v < 80 -> 5; v < 95 -> 3; else -> 2 }
        "RESOURCES" -> v.roundToInt().coerceIn(0, 5)
        "LIFE SIGNS" -> listOf(1, 2, 4, 3, 2)[v.roundToInt().coerceIn(0, 4)]
        else -> 3
    }

    fun readout(p: Planet, row: String): Readout {
        val o = p.obs[row] ?: return Readout(row, true, "SCANNER DAMAGED ??", "", 0, true)
        if (!o.ok) return Readout(row, true, "SCANNER DAMAGED ??", "", 0, true)
        val v = o.value
        val fuzzy = o.error > 0.22
        val q = if (fuzzy) "?" else ""
        val (main, sub) = when (row) {
            "ATMOSPHERE" -> { val i = v.roundToInt().coerceIn(0, 5); ATM_NAMES[i] + q to ATM_SUBS[i] }
            "GRAVITY" -> "%.2f g%s".format(v.coerceIn(0.1, 3.0), q) to when {
                v < 0.6 -> "Bone and muscle loss over decades"; v < 0.85 -> "Light · adaptable"; v < 1.15 -> "Near Earth-normal"; v < 1.5 -> "Cardiac strain · long-term"; else -> "Debilitating · lethal for the frail" }
            "TEMPERATURE" -> "${v.roundToInt()}°C mean$q" to when {
                v < -40 -> "Cryogenic · sealed habitats only"; v < 0 -> "Cold · farmable near the equator"; v < 30 -> "Temperate"; v < 55 -> "Heat stress · nocturnal working"; else -> "Lethal heat" }
            "WATER" -> { val w = v.coerceIn(0.0, 100.0).roundToInt()
                (when { w < 2 -> "Bone dry"; w < 12 -> "Ice caps, scattered lakes"; w < 40 -> "Inland seas"; w < 80 -> "Wide shallow seas"; else -> "Global ocean" } + q) to "$w% surface coverage" }
            "RESOURCES" -> { val i = v.roundToInt().coerceIn(0, 5); RES_NAMES[i] + q to listOf("Silicates only", "Thin seams", "Workable iron and copper", "Good metals · some fissiles", "Rich metals and fissiles", "Exceptional · surface deposits")[i] }
            "LIFE SIGNS" -> { val i = v.roundToInt().coerceIn(0, 4); LIFE_NAMES[i] + q to listOf("No biosignatures", "Biofilms · O₂ disequilibrium", "Photosynthetic analogues", "Motile multicellular life", "Structured emissions · night side")[i] }
            else -> p.features.take(2).joinToString(", ") to (p.features.drop(2).firstOrNull() ?: "${p.moons} moon${if (p.moons == 1) "" else "s"}")
        }
        val bars = habitability(row, v)
        return Readout(row, false, main, sub, bars, bars <= 2 || fuzzy)
    }

    fun confidence(p: Planet): String {
        val ok = p.obs.values.filter { it.ok }
        if (ok.size < 5) return "LOW"
        val e = ok.map { it.error }.average()
        return when { e < 0.12 -> "HIGH"; e < 0.25 -> "MIDDLING"; else -> "LOW" }
    }
}
