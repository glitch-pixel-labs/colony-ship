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
internal fun earth(s: GameState) = Physics.earthYear(s)
internal fun ly(s: GameState) = "%.1f".format(Physics.distanceFromSol(s))

/**
 * Every incident the ship can meet in transit. All writing is original to Colony Ship.
 * Hard science fiction: the physics, biology and engineering are meant to hold up, and choices have consequences.
 */
object Events {
    val all: List<EventDef> = EventsSpace.list + EventsShip.list + EventsCrew.list

    fun byId(id: String) = all.firstOrNull { it.id == id }

    fun pick(s: GameState, r: Random): EventDef {
        val recent = s.seenEvents.takeLast(14).toSet()
        val pool = all.filter { it.condition(s) && it.id !in recent && it.available(s).size >= 2 }
            .ifEmpty { all.filter { it.condition(s) && it.available(s).size >= 2 } }
        var x = r.nextDouble() * pool.sumOf { it.weight }
        for (e in pool) { x -= e.weight; if (x <= 0) return e }
        return pool.last()
    }
}
