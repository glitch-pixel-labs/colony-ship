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
        "Ancient ruins", "Lights on the night side", "Sterilising radiation", "Acid seas", "Runaway greenhouse",
        "Crushing atmosphere", "No star — rogue world", "Day-side magma ocean")

    fun name(r: Random): String {
        val n = pre.random(r) + mid.random(r) + suf.random(r)
        return n.replaceFirstChar { it.uppercase() } + " " + letters.random(r).uppercase()
    }

    /** Roll a world with real variance: rare Earth-analogues and truly hellish planets, not a smear of middling ones. */
    fun generate(r: Random, jump: Int, distance: Double): Planet {
        val roll = r.nextDouble()
        val band = when {
            roll < 0.07 -> "EARTHLIKE"
            roll < 0.22 -> "HELLISH"
            roll < 0.40 -> "HOSTILE"
            roll < 0.62 -> "PROMISING"
            else -> "MARGINAL"
        }
        return when (band) {
            "EARTHLIKE" -> earthlike(r, distance)
            "HELLISH" -> hellish(r, distance)
            "HOSTILE" -> hostile(r, distance)
            "PROMISING" -> promising(r, distance)
            else -> marginal(r, distance)
        }
    }

    private fun finish(r: Random, distance: Double, atm: Int, gravity: Double, temp: Int, water: Int, resources: Int, life: Int, feats: List<String>, quality: String, kind: String): Planet {
        val hue = when {
            kind == "rogue" || temp < -80 -> 0.62f
            kind == "furnace" || temp > 80 -> 0.02f
            kind == "acid" -> 0.22f
            kind == "radiation" -> 0.18f
            water > 75 -> 0.5f
            temp < -30 -> 0.58f
            atm <= 1 -> 0.18f
            water < 15 -> 0.05f
            else -> r.nextDouble(0.0, 1.0).toFloat()
        }
        val features = feats.distinct().take(3).ifEmpty { listOf("Canyon systems") }
        return Planet(name(r), (distance * 10).roundToInt() / 10.0, weighted(r, listOf(1.0, 1.5, 0.8, 0.4)), atm,
            (gravity * 100).roundToInt() / 100.0, temp, water.coerceIn(0, 100), resources.coerceIn(0, 5), life.coerceIn(0, 4),
            features, hue, quality, kind)
    }

    private fun earthlike(r: Random, distance: Double): Planet {
        val feats = mutableListOf("Twin sunsets").apply {
            if (r.nextDouble() < 0.5) add("Long archipelagos")
            if (r.nextDouble() < 0.4) add("Aurorae")
            if (r.nextDouble() < 0.25) add("Rings")
            if (distance < Physics.HUMAN_RELIC_LY && r.nextDouble() < 0.12) add("Ancient ruins")
        }
        return finish(r, distance, atm = 4, gravity = 0.92 + r.nextDouble() * 0.16, temp = 8 + r.nextInt(16),
            water = 55 + r.nextInt(25), resources = 3 + r.nextInt(2), life = 2 + r.nextInt(2), feats = feats,
            quality = "EARTHLIKE", kind = "earthlike")
    }

    private fun promising(r: Random, distance: Double): Planet {
        val atm = listOf(3, 4, 4, 5).random(r)
        val temp = -5 + r.nextInt(40)
        val water = 25 + r.nextInt(55)
        val feats = mutableListOf<String>()
        if (water > 50) feats += "Long archipelagos"
        if (temp < 5) feats += "Glacier belts"
        if (r.nextDouble() < 0.3) feats += "Crystal forests"
        if (r.nextDouble() < 0.2) feats += "Rings"
        return finish(r, distance, atm, 0.7 + r.nextDouble() * 0.6, temp, water, 2 + r.nextInt(3),
            if (atm >= 3 && water > 10) 1 + r.nextInt(3) else 0, feats, "PROMISING", "garden")
    }

    private fun marginal(r: Random, distance: Double): Planet {
        val atm = weighted(r, listOf(0.6, 1.0, 1.4, 1.2, 0.8, 0.4))
        val gravity = (0.4 + r.nextDouble() * 1.4).coerceIn(0.3, 1.9)
        val temp = (-70 + r.nextInt(130)).coerceIn(-90, 70)
        val water = when { temp < -40 -> r.nextInt(0, 12); temp > 50 -> r.nextInt(0, 15); else -> r.nextInt(5, 70) }
        val life = if (atm >= 3 && water > 5 && temp in -25..50) weighted(r, listOf(0.8, 1.4, 1.0, 0.4, 0.05)) else weighted(r, listOf(2.0, 0.6, 0.1, 0.02, 0.02))
        val feats = genericFeats(r, temp, water, life, distance)
        return finish(r, distance, atm, gravity, temp, water, weighted(r, listOf(0.8, 1.3, 1.3, 1.0, 0.5, 0.2)), life, feats, "MARGINAL", "mixed")
    }

    private fun hostile(r: Random, distance: Double): Planet {
        val atm = listOf(0, 1, 2, 2, 3).random(r)
        val gravity = listOf(0.28 + r.nextDouble() * 0.2, 1.5 + r.nextDouble() * 0.7).random(r)
        val temp = listOf(-90 + r.nextInt(30), 50 + r.nextInt(45)).random(r)
        val water = if (temp < -20 || temp > 55) r.nextInt(0, 12) else r.nextInt(0, 35)
        val feats = genericFeats(r, temp, water, 0, distance)
        if (gravity > 1.4) feats.add("Canyon systems")
        return finish(r, distance, atm, gravity, temp, water, r.nextInt(0, 3), if (atm >= 2) r.nextInt(0, 2) else 0, feats, "HOSTILE", "mixed")
    }

    private fun hellish(r: Random, distance: Double): Planet = when (r.nextInt(6)) {
        0 -> { // Tidally locked furnace
            finish(r, distance, atm = listOf(1, 2).random(r), gravity = 0.8 + r.nextDouble() * 0.4, temp = 95 + r.nextInt(90),
                water = r.nextInt(0, 4), resources = r.nextInt(0, 3), life = 0,
                feats = listOf("Tidally locked", "Day-side magma ocean", "Runaway greenhouse"), quality = "HELLISH", kind = "furnace")
        }
        1 -> { // Crushing Venus-like atmosphere
            finish(r, distance, atm = 6, gravity = 0.85 + r.nextDouble() * 0.3, temp = 80 + r.nextInt(70),
                water = r.nextInt(0, 6), resources = 1 + r.nextInt(3), life = 0,
                feats = listOf("Crushing atmosphere", "Runaway greenhouse", "Cyclones"), quality = "HELLISH", kind = "crushing")
        }
        2 -> { // Sterilising radiation
            finish(r, distance, atm = listOf(0, 1, 2).random(r), gravity = 0.4 + r.nextDouble() * 0.8, temp = -20 + r.nextInt(70),
                water = r.nextInt(0, 15), resources = r.nextInt(0, 3), life = 0,
                feats = listOf("Sterilising radiation", "Aurorae", "Canyon systems"), quality = "HELLISH", kind = "radiation")
        }
        3 -> { // Acid ocean
            finish(r, distance, atm = 1, gravity = 0.9 + r.nextDouble() * 0.3, temp = 20 + r.nextInt(50),
                water = 80 + r.nextInt(20), resources = r.nextInt(1, 4), life = r.nextInt(0, 2),
                feats = listOf("Acid seas", "Cyclones", "Floating kelp mats".takeIf { r.nextBoolean() } ?: "Volcanic chains"),
                quality = "HELLISH", kind = "acid")
        }
        4 -> { // Rogue world, no star
            finish(r, distance, atm = 0, gravity = 0.7 + r.nextDouble() * 0.8, temp = -180 + r.nextInt(40),
                water = r.nextInt(0, 20), resources = 2 + r.nextInt(3), life = 0,
                feats = listOf("No star — rogue world", "Glacier belts"), quality = "HELLISH", kind = "rogue")
        }
        else -> { // Airless rock
            finish(r, distance, atm = 0, gravity = 0.15 + r.nextDouble() * 0.5, temp = -80 + r.nextInt(40),
                water = r.nextInt(0, 3), resources = r.nextInt(0, 4), life = 0,
                feats = listOf("Rust deserts", "Canyon systems", "Salt flats"), quality = "HELLISH", kind = "vacuum")
        }
    }

    private fun genericFeats(r: Random, temp: Int, water: Int, life: Int, distance: Double): MutableList<String> {
        val feats = mutableListOf<String>()
        if (temp < 0 && r.nextDouble() < 0.6) feats += "Glacier belts"
        if (water < 50 && r.nextDouble() < 0.35) feats += listOf("Rust deserts", "Salt flats").random(r)
        if (water in 40..92 && r.nextDouble() < 0.45) feats += "Long archipelagos"
        if (life == 4) feats += "Lights on the night side"
        if (life >= 2 && water > 60 && r.nextDouble() < 0.4) feats += "Floating kelp mats"
        if (distance < Physics.HUMAN_RELIC_LY && r.nextDouble() < 0.08) feats += "Ancient ruins"
        if (distance >= 40 && r.nextDouble() < 0.07) feats += "Regular structures, origin unknown"
        if (r.nextDouble() < 0.14) feats += listOf("Rings", "Aurorae", "Volcanic chains", "Canyon systems", "Twin sunsets").random(r)
        return feats
    }

    /** A world taken from a non-human catalogue: paradise, grave, or made for someone else. */
    fun generateLead(r: Random, kind: String, distance: Double): Planet = when (kind) {
        "paradise" -> finish(
            r, distance, atm = 4, gravity = 0.96 + r.nextDouble() * 0.08, temp = 12 + r.nextInt(8),
            water = 60 + r.nextInt(18), resources = 4, life = 3,
            feats = listOf("Long archipelagos", "Twin sunsets", "Aurorae"),
            quality = "EARTHLIKE", kind = "paradise"
        )
        "grave" -> finish(
            r, distance, atm = listOf(0, 1, 2).random(r), gravity = 0.6 + r.nextDouble() * 0.7,
            temp = -40 + r.nextInt(90), water = r.nextInt(0, 12), resources = 3 + r.nextInt(2), life = 0,
            feats = listOf("Regular structures, origin unknown", "Ancient ruins", "Rust deserts"),
            quality = "HELLISH", kind = "grave"
        )
        else -> finish(
            r, distance, atm = listOf(1, 5, 6).random(r), gravity = 1.1 + r.nextDouble() * 0.5,
            temp = 55 + r.nextInt(50), water = 20 + r.nextInt(50), resources = 3 + r.nextInt(2), life = 1 + r.nextInt(2),
            feats = listOf("Crushing atmosphere".takeIf { r.nextBoolean() } ?: "Acid seas", "Runaway greenhouse", "Cyclones"),
            quality = "HELLISH", kind = "alien-garden"
        )
    }

    fun verdict(p: Planet): Pair<String, String> {
        val sub = when (p.kind) {
            "paradise" -> "A rare, temperate, breathable world. The catalogue was, for once, right by our standards too."
            "grave" -> "A graveyard. Wreckage in orbit, ruins on the ground. Whatever recommended this place did not mean it for us."
            "alien-garden" -> "Habitable by the criteria of whoever wrote the catalogue. Not by ours."
            "earthlike" -> "Rare Earth-analogue. Breathable air, temperate climate, standing water."
            "garden" -> "A workable world. Hard, but a colony could take root."
            "furnace" -> "Tidally locked furnace. The dayside never cools; the nightside never warms."
            "crushing" -> "Crushing atmosphere. Surface pressure would flatten an unarmoured lander."
            "radiation" -> "Sterilising radiation. Unshielded tissue would not last an hour."
            "acid" -> "An ocean of acid under a corrosive sky."
            "rogue" -> "A rogue world: no star, no day, only ice and leftover heat."
            "vacuum" -> "Airless rock. Vacuum, dust, and hard radiation from the sky."
            else -> when (p.quality) {
                "HOSTILE" -> "Hostile. A colony would live in sealed boxes, if it lived."
                "PROMISING" -> "Promising, with serious caveats."
                else -> "Marginal. Survivable, not kind."
            }
        }
        return p.quality to sub
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

    val ATM_NAMES = listOf("None to speak of", "Corrosive soup", "Thin, toxic", "Thin, breathable", "Breathable", "Dense, breathable", "Crushing")
    val ATM_SUBS = listOf("Trace gas · effectively vacuum", "Sulphur and chlorine compounds", "Low O₂ · CO₂ rich · lethal", "Low pressure · O₂ 14%", "N₂/O₂ · near Earth-normal", "High pressure · O₂ 26%", "90+ bar · CO₂/N₂ · lander would flatten")
    val RES_NAMES = listOf("Barren", "Poor", "Modest", "Good", "Rich", "Abundant")
    val LIFE_NAMES = listOf("None detected", "Microbial mats", "Plant analogues", "Animal life", "Signs of others")

    fun habitability(row: String, v: Double): Int = when (row) {
        "ATMOSPHERE" -> listOf(0, 0, 1, 3, 5, 4, 0)[v.roundToInt().coerceIn(0, 6)]
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
            "ATMOSPHERE" -> { val i = v.roundToInt().coerceIn(0, 6); ATM_NAMES[i] + q to ATM_SUBS[i] }
            "GRAVITY" -> "%.2f g%s".format(v.coerceIn(0.1, 3.0), q) to when {
                v < 0.6 -> "Bone and muscle loss over decades"; v < 0.85 -> "Light · adaptable"; v < 1.15 -> "Near Earth-normal"; v < 1.5 -> "Cardiac strain · long-term"; else -> "Debilitating · lethal for the frail" }
            "TEMPERATURE" -> "${v.roundToInt()}°C mean$q" to when {
                v < -120 -> "Near absolute winter · no unshielded life"; v < -40 -> "Cryogenic · sealed habitats only"; v < 0 -> "Cold · farmable near the equator"; v < 30 -> "Temperate"; v < 55 -> "Heat stress · nocturnal working"; v < 90 -> "Lethal heat"; else -> "Rock melts at noon · furnace world" }
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
