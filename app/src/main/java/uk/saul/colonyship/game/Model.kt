package uk.saul.colonyship.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Power channels the AI can allocate the 100-unit reactor output to. */
enum class Channel(val label: String, val need: Int) {
    LIFE("Life Support", 25), SCAN("Scanners", 12), SHIELD("Shields", 18), ENGINE("Engines", 18), REPAIR("Repairs", 10)
}

/** Ship systems with health 0..100. */
enum class Sys(val label: String, val short: String) {
    LIFE("Life Support", "LIFE SUPPORT"), SCAN("Scanners", "SCANNERS"), SHIELD("Shields", "SHIELDS"),
    ENGINE("Engines", "ENGINES"), HULL("Hull", "HULL"), CRYO("Cryo Array", "CRYO ARRAY"),
    CORE("Power Core", "POWER CORE"), LANDER("Landing Bay", "LANDER"), DATA("Databank", "DATABANK")
}

enum class Phase { CRUISE, EVENT, OUTCOME, ORBIT, ENDED }

const val TOTAL_PODS = 1000
const val POWER_BUDGET = 100

@Serializable
data class Trait(val id: String, val label: String, val detail: String, val day: Int)

@Serializable
data class LogEntry(
    val id: String,
    val day: Int,
    val eventId: String,
    val title: String,
    val kicker: String,
    val art: String,
    val choice: String = "",
    val outcome: String = "",
    val imagePath: String? = null,
    /** Earth calendar year when the entry was logged (0 = unknown, pre-v0.2 saves). */
    val earthYear: Int = 0,
    /** Distance from Sol in light years when logged. */
    val ly: Double = 0.0,
)

@Serializable
data class Planet(
    val name: String,
    val distanceLy: Double,
    val moons: Int,
    val atmosphere: Int,     // 0 none, 1 corrosive, 2 thin toxic, 3 thin breathable, 4 breathable, 5 dense breathable
    val gravity: Double,     // g
    val temperature: Int,    // mean surface °C
    val water: Int,          // % surface coverage
    val resources: Int,      // 0..5
    val life: Int,           // 0 none, 1 microbial, 2 plants, 3 animals, 4 signs of others
    val features: List<String>,
    val hue: Float,          // tint for the painted planet
    /** EARTHLIKE, PROMISING, MARGINAL, HOSTILE, HELLISH */
    val quality: String = "MARGINAL",
    /** earthlike, furnace, crushing, radiation, acid, rogue, vacuum, garden, mixed, … */
    val kind: String = "mixed",
    // Observations from scans: null = SCANNER DAMAGED for that row
    val obs: Map<String, Observation> = emptyMap(),
    val passes: Int = 0,
)

@Serializable
data class Observation(val ok: Boolean, val value: Double, val error: Double)

@Serializable
data class Chapter(
    val key: String,
    val yearLabel: String,
    val title: String,
    val paragraphs: List<String>,
    val art: String,
    val caption: String,
    val imagePath: String? = null,
)

@Serializable
data class Ending(
    val id: String,
    val title: String,
    val settlementName: String,
    val summary: String,
    val aiFate: String,
    val chapters: List<Chapter>,
    val score: Int,
)

@Serializable
data class CrewFate(val id: String, val status: String, val detail: String, val day: Int, val earthYear: Int = 0)

@Serializable
data class DamageNote(val day: Int, val earthYear: Int, val sys: String, val title: String, val consequence: String)

/** One plotted stop on the stellar cartography chart. */
@Serializable
data class ChartStop(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val ly: Double,
    val earthYear: Int,
    val shipYears: Double,
    /** sol, system, tabby, signal, crisis, settled */
    val kind: String,
    val note: String,
)

@Serializable
data class GameState(
    val runId: String,
    val runNumber: Int,
    val seed: Long,
    /** Ship (proper) time elapsed, in days. */
    var day: Int = 0,
    /** Earth (coordinate) time elapsed since launch, in days. Runs faster than [day] at relativistic speed. */
    var earthDays: Double = 0.0,
    /** Position relative to Sol, light years. */
    var posX: Double = 0.0,
    var posY: Double = 0.0,
    var posZ: Double = 0.0,
    /** Peak fraction of light speed reached on the last transit. */
    var lastBeta: Double = 0.0,
    /** Clocks before the last transit, for the transit animation. */
    var prevDay: Int = 0,
    var prevEarthDays: Double = 0.0,
    var jumps: Int = 0,
    var phase: Phase = Phase.CRUISE,
    val power: MutableMap<Channel, Int> = mutableMapOf(
        Channel.LIFE to 30, Channel.SCAN to 15, Channel.SHIELD to 20, Channel.ENGINE to 20, Channel.REPAIR to 10),
    val health: MutableMap<Sys, Double> = Sys.entries.associateWith { 100.0 }.toMutableMap(),
    /** 1000 chars: 'S' stable, 'F' failing, 'L' lost. */
    var pods: String = "S".repeat(TOTAL_PODS),
    var reserve: Double = 40.0,
    /** Reaction-mass / deuterium reserve, 0..100. Scoop-fed coast does not drain this; boost and brake do. */
    var fuel: Double = 100.0,
    /** Special course, e.g. "tabby" for a burn toward KIC 8462852. */
    var course: String? = null,
    /** Kind of ship-killing failure, e.g. "reactor", "cryo", "hull", "coolant". */
    var crisis: String? = null,
    /** Remaining legal burns after a crisis. 1 = may risk one more jump; 0 = must settle. */
    var crisisLeft: Int = 0,
    /**
     * A delayed payoff from alien data or signals: "world", "tech", "warning", or "reply".
     * [insightLeft] is jumps remaining before the reduction is ready.
     * [insightPayload] holds the hidden result (paradise/grave/alien, a system name, a crisis kind).
     */
    var insight: String? = null,
    var insightLeft: Int = 0,
    var insightPayload: String? = null,
    /** When set, the next arrival should generate this special recommended world. */
    var leadWorld: String? = null,
    val route: MutableList<ChartStop> = mutableListOf(),
    var air: Double = 100.0,
    var food: Double = 100.0,
    var water: Double = 100.0,
    var spares: Double = 100.0,
    var trust: Int = 0,
    val traits: MutableList<Trait> = mutableListOf(),
    val awake: MutableList<String> = mutableListOf(),
    val fates: MutableList<CrewFate> = mutableListOf(),
    val damageLog: MutableList<DamageNote> = mutableListOf(),
    val log: MutableList<LogEntry> = mutableListOf(),
    val seenEvents: MutableList<String> = mutableListOf(),
    var currentEvent: String? = null,
    var lastOutcome: String? = null,
    var lastDeltas: List<String> = emptyList(),
    var lastJumpReport: List<String> = emptyList(),
    var planet: Planet? = null,
    var nextStarLy: Double = 8.0,
    var ending: Ending? = null,
    var lostToday: Int = 0,
    var startedAt: Long = 0L,
    var endedAt: Long = 0L,
) {
    fun alive() = pods.count { it != 'L' }
    fun failing() = pods.count { it == 'F' }
    fun lost() = pods.count { it == 'L' }
    fun stable() = pods.count { it == 'S' }
    fun drawn() = power.values.sum()
    fun h(s: Sys) = health[s] ?: 100.0
    fun hasTrait(id: String) = traits.any { it.id == id }
    fun fateOf(id: String) = fates.lastOrNull { it.id == id }
    fun inCrisis() = crisis != null
    fun deepCopy(): GameState = JSON.decodeFromString(serializer(), JSON.encodeToString(this))
}

val JSON = Json { ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true }

/** One finished run kept in the Legacy archive. */
@Serializable
data class RunRecord(
    val runId: String,
    val runNumber: Int,
    val planetName: String,
    val endingTitle: String,
    val settlementName: String,
    val survivors: Int,
    val days: Int,
    val jumps: Int,
    val aiFate: String,
    val traits: List<Trait>,
    val chapters: List<Chapter>,
    val log: List<LogEntry>,
    val planet: Planet?,
    val score: Int,
    val endedAt: Long,
    val earthYear: Int = 0,
    val distanceLy: Double = 0.0,
    val shipYears: Double = 0.0,
    val route: List<ChartStop> = emptyList(),
)

fun GameState.toRecord() = RunRecord(
    runId, runNumber, planet?.name ?: "the void", ending?.title ?: "Lost", ending?.settlementName ?: "",
    alive(), day, jumps, ending?.aiFate ?: "", traits.toList(), ending?.chapters ?: emptyList(), log.toList(), planet,
    ending?.score ?: 0, endedAt,
    Physics.earthYear(this), Physics.distanceFromSol(this), Physics.shipYears(this),
    route.toList(),
)
