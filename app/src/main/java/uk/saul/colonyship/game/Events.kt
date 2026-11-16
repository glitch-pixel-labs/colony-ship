package uk.saul.colonyship.game

import kotlin.random.Random

class ChoiceDef(
    val title: String,
    val desc: String,
    /** Cost tags. Prefix with "!" to mark a tag as a bad outcome. */
    val chips: List<String>,
    /** 1 (safe) .. 5 (reckless) */
    val risk: Int,
    val available: (GameState) -> Boolean = { true },
    val effect: Fx.() -> String,
)

class EventDef(
    val id: String,
    val kicker: String,
    val title: String,
    val vfd: String,
    val hazard: String,
    val art: String,
    val metricLabel: String,
    val metric: (GameState) -> String,
    val ai: (GameState) -> String,
    val choices: List<ChoiceDef>,
    val weight: Double = 1.0,
    val condition: (GameState) -> Boolean = { true },
) {
    fun available(s: GameState) = choices.filter { it.available(s) }.take(3)
}

internal fun asleep(id: String): (GameState) -> Boolean = { id !in it.awake }
internal fun isAwake(id: String): (GameState) -> Boolean = { id in it.awake }
internal fun unresolved(id: String): (GameState) -> Boolean = {
    id in it.awake && it.fateOf(id)?.status !in setOf("DEAD", "MISSING", "SETTLED")
}
internal fun earth(s: GameState) = Physics.earthYear(s)
internal fun ly(s: GameState) = "%.1f".format(Physics.distanceFromSol(s))

/**
 * Every incident the ship can meet in transit. All writing is original to Colony Ship.
 * Hard science fiction: the physics, biology and engineering are meant to hold up, and choices have consequences.
 */
object Events {
    val all: List<EventDef> = EventsSpace.list + EventsShip.list + EventsCrew.list + EventsDeep.list + EventsCrisis.list + EventsInsight.list

    fun byId(id: String) = all.firstOrNull { it.id == id }

    fun pick(s: GameState, r: Random): EventDef {
        val recent = s.seenEvents.takeLast(14).toSet()
        val pool = all.filter { it.condition(s) && it.id !in recent && it.available(s).size >= 2 }
            .ifEmpty { all.filter { it.condition(s) && it.available(s).size >= 2 } }
        fun w(e: EventDef): Double {
            if (!e.id.startsWith("crisis_")) return e.weight
            val wear = Sys.entries.minOf { s.h(it) }
            val worn = if (wear < 40) 0.5 else if (wear < 60) 0.25 else 0.0
            return e.weight * (0.7 + 0.18 * (s.jumps - 4).coerceAtLeast(0) + worn)
        }
        var x = r.nextDouble() * pool.sumOf { w(it) }
        var ev = pool.last()
        for (e in pool) { x -= w(e); if (x <= 0) { ev = e; break } }
        if (ev.id.startsWith("crisis_") && s.hasTrait("warning-honoured")) {
            ev = byId("warning_pays") ?: ev
        }
        return ev
    }
}
