package uk.saul.colonyship.game

/** Prompts for log entries viewed later in the archive (no live ship state needed). */
object LogPrompts {
    fun image(ev: EventDef, e: LogEntry): String =
        "${Prompts.STYLE}. Scene from the ship's log, day ${e.day}: ${ev.title}. ${e.outcome.take(220)} Featuring a long ribbed colony ship with a rotating habitat ring${if (e.choice.isNotBlank()) ", as the AI chooses to ${e.choice.lowercase()}" else ""}. Portrait composition, 2:3. ${Prompts.NEG}"
    fun video(ev: EventDef, e: LogEntry): String =
        "8-second cinematic shot, a ${Prompts.STYLE} brought to life. Day ${e.day} of the voyage: ${ev.title}. ${e.outcome.take(180)} Slow push-in, drifting particles, glowing consoles, subtle film flicker, ambient synth hum. Vertical 9:16. ${Prompts.NEG}"
}
