package uk.saul.colonyship.game

/**
 * Rare, ship-killing failures. Probability rises with jump count and wear.
 * None of these can be fully repaired in flight: the next world, or the one after, must be landfall.
 */
internal object EventsCrisis {

    private val eligible: (GameState) -> Boolean = { s ->
        s.jumps >= 5 && s.crisis == null && !s.hasTrait("forced-landfall") && s.course != "tabby"
    }

    val list: List<EventDef> = listOf(

        EventDef("crisis_reactor", "REACTOR · CONTAINMENT", "The Bottle Is Open", "CATASTROPHE", "CORE 2 · CONTAINMENT BREACH · GAMMA ALARM", "engineering",
            "DOSE", { "FATAL IN 4 H" },
            { "Core two has lost a section of its first wall. Neutrons and gamma are coming through the shield tank into the ring. I have scrammed the core and isolated the loop, but the confinement geometry is no longer a bottle, it is a wound, and wounds in a fusion reactor do not close. If we try to run it at cruise power for another long coast, the remaining wall will go, and the crew section will follow. We can limp to the next star. We may be able to limp to the one after that. We cannot go on as we have been." },
            listOf(
                ChoiceDef("Scram and patch the wall", "Dump power, throw the remaining spares at a temporary shield, and make for landfall.", listOf("−SPARES", "−POWER", "FORCED LANDFALL"), 4) {
                    spares(-s.spares.coerceAtMost(24.0)); reserve(-18.0); dmg(Sys.CORE, 28.0); dmg(Sys.LIFE, 8.0, false)
                    declareCrisis("reactor")
                    "The patch is a plate and a prayer. Dose rates in the ring are down to a level I can call survivable if nobody lives there. The reactor will give us one more boost and one more brake. After that I will not certify it. Find a world."
                },
                ChoiceDef("Jettison the damaged loop", "Cut core two loose and run on the remaining core, shorter and slower.", listOf("−ENGINES", "−FUEL", "FORCED LANDFALL"), 4) {
                    dmg(Sys.CORE, 22.0); dmg(Sys.ENGINE, 16.0, false); fuel(-14.0)
                    declareCrisis("reactor")
                    "Core two is a receding point of light and a radiation hazard for whoever comes this way later. We are on one core. We can brake into the system ahead, and perhaps burn once more. A third time would be vanity."
                },
                ChoiceDef("Run it hot and sprint", "Hold power and race the wall. Faster arrival, worse damage, more dead.", listOf("!HEAVY LOSSES", "FORCED LANDFALL"), 5) {
                    dmg(Sys.CORE, 36.0); dmg(Sys.LIFE, 14.0); dmg(Sys.HULL, 8.0, false); killPods(40 + r.nextInt(40))
                    declareCrisis("reactor")
                    "I ran it. The wall held for most of the coast and then it did not. We have the speed. We also have a dead section of the ring, a core that will not take a second long cruise, and names I will have to add to the list. Land."
                },
            ), weight = 0.20, condition = eligible),

        EventDef("crisis_cryo", "CRYO ARRAY · CASCADE", "The Array Is Going", "CATASTROPHE", "CASCADE · MULTIPLE ROWS WARMING · NO RESERVE", "cryobay",
            "WARMING", { "ROWS 4–9" },
            { "A cascade has started in the cryo array and I cannot stop it at the source. The primary and secondary pumps in rows four through nine have seized in sequence, the way a bad bearing takes its neighbours, and the remaining cryogen is not enough to re-chill that many pods. I can triage. I cannot restore the array to a state I would trust for a long voyage. Every further week in flight will cost us people. The next world, whatever it is, has to be landfall — or the one after, if we are willing to pay for the time." },
            listOf(
                ChoiceDef("Triage the rows", "Save who can be saved. Let the lost rows go. Prepare to land.", listOf("PODS LOST", "FORCED LANDFALL"), 4) {
                    val n = 60 + r.nextInt(50)
                    killPods(n); savePods(20)
                    dmg(Sys.CRYO, 30.0)
                    declareCrisis("cryo")
                    "I chose. $n pods are now listed among the lost, and a block of the array is a warm room I will not look into again. The rest will hold for one more transit if we are careful, and not for a career of them. We land at the next world, or we spend more names for one more try."
                },
                ChoiceDef("Wake a medical team", "Thaw Varga if she is still asleep, and whoever else can work a failing bay.", listOf("−AIR", "−FOOD", "FORCED LANDFALL"), 3) {
                    air(-8.0); food(-8.0); dmg(Sys.CRYO, 22.0); killPods(35 + r.nextInt(30)); savePods(25)
                    if ("varga" !in s.awake && s.fateOf("varga")?.status != "DEAD") wake("varga")
                    declareCrisis("cryo")
                    "Hands in the bay slowed the cascade. They did not reverse it. The array will carry us to the next star, and I will not promise the one after without a butcher’s bill. We need a surface and a thaw that is on purpose."
                },
                ChoiceDef("Dump power into the remaining pumps", "Burn the reserve to hold temperature. Buy days, not years.", listOf("−POWER", "!CORE WEAR", "FORCED LANDFALL"), 5) {
                    reserve(-s.reserve.coerceAtMost(28.0)); dmg(Sys.CORE, 12.0, false); dmg(Sys.CRYO, 18.0); killPods(25 + r.nextInt(25))
                    declareCrisis("cryo")
                    "The pumps held. The reserve is nearly gone, and so is any argument for another long coast. The next survey has to end in a landing."
                },
            ), weight = 0.20, condition = eligible),

        EventDef("crisis_hull", "STRUCTURE · SPINE", "The Spine Is Tiring", "CATASTROPHE", "FATIGUE CRACK · RING FRAME 7 · SLOW VENT", "ev_eva",
            "RATE", { "RISING" },
            { "A fatigue crack in ring frame seven has opened into a slow vent, and the crack is growing. This is not a puncture I can patch and forget. The spine has been loaded, unloaded and loaded again for years, at a tenth of a g and at a half, and the metal has begun to remember that. I can sister the frame. I cannot give the ship its old margin. A long coast at cruise will work the crack. A landing we can plan for will not." },
            listOf(
                ChoiceDef("Sister the frame and depressurise the bay", "A heavy repair. It will hold for a landing, not for a career.", listOf("−SPARES", "+DAYS", "FORCED LANDFALL"), 4) {
                    spares(-s.spares.coerceAtMost(20.0)); days(16); dmg(Sys.HULL, 20.0); dmg(Sys.LANDER, 6.0, false)
                    declareCrisis("hull")
                    "The sister-plates are on. The bay is in vacuum and will stay there. I have a hull I will take into orbit of the next world, and I will not take it through another five transits. Land, or risk one more burn and watch the crack while you do it."
                },
                ChoiceDef("Abandon the damaged sector", "Seal it off, with whatever and whoever is still inside.", listOf("!PODS LOST", "FORCED LANDFALL"), 5) {
                    killPods(20 + r.nextInt(25)); dmg(Sys.HULL, 16.0); dmg(Sys.LIFE, 8.0, false)
                    declareCrisis("hull")
                    "The sector is a closed door and a pressure reading I no longer watch. We are lighter and shorter of options. The next world is, for practical purposes, the destination."
                },
                ChoiceDef("Nurse it and hope the models are kind", "Minimal intervention. Maximum honesty in the log.", listOf("!HULL RISK", "FORCED LANDFALL"), 5) {
                    dmg(Sys.HULL, 28.0); dmg(Sys.SHIELD, 8.0, false)
                    declareCrisis("hull")
                    "I logged the crack length every hour. It grew. Not fast enough to kill us before the next star, if the models are right. Models have been wrong before. We land there, or we roll the dice once."
                },
            ), weight = 0.18, condition = eligible),

        EventDef("crisis_coolant", "THERMAL · PRIMARY LOOP", "Nothing Left to Dump the Heat Into", "CATASTROPHE", "PRIMARY COOLANT · LOOP DRY · RADIATORS INEFFECTIVE", "engineering",
            "HEAT", { "CLIMBING" },
            { "The primary coolant loop is dry. A micrometeoroid, or a seam, or simple age, opened a line on the radiator wing, and by the time the sensors agreed with each other the inventory was a stain of frozen ammonia on the hull. The secondary loop can reject heat for a landing and for one short transit. It cannot reject the waste heat of a long coast at cruise power. If we try, the core will throttle itself, or it will not, and either way the cryo array will be the first thing to notice." },
            listOf(
                ChoiceDef("Throttle to a crawl and make for port", "Low power, long days, one destination.", listOf("−POWER", "−ENGINES", "FORCED LANDFALL"), 3) {
                    reserve(-16.0); dmg(Sys.CORE, 10.0, false); dmg(Sys.ENGINE, 10.0, false); dmg(Sys.LIFE, 6.0, false)
                    declareCrisis("coolant")
                    "We are crawling. The secondary loop is holding at this draw. I can brake us into the system ahead, and I may be able to light the drive once more at this miserable rate. I will not plan a third time. Find ground."
                },
                ChoiceDef("Cannibalise the landing radiators", "Steal heat-rejection from the lander so the ship can limp.", listOf("−LANDER", "−SPARES", "FORCED LANDFALL"), 4) {
                    spares(-12.0); dmg(Sys.LANDER, 22.0); dmg(Sys.LIFE, 8.0, false)
                    declareCrisis("coolant")
                    "The lander’s radiators are now the ship’s. We can fly a little further. Landing will be hotter and harder, which is a problem for a later hour, and that hour is coming soon."
                },
                ChoiceDef("Hold power and accept the heat", "Keep the schedule. Pay in pods and in core life.", listOf("!HEAVY LOSSES", "FORCED LANDFALL"), 5) {
                    dmg(Sys.CORE, 18.0); dmg(Sys.CRYO, 16.0); dmg(Sys.LIFE, 10.0); killPods(30 + r.nextInt(40)); failPods(20)
                    declareCrisis("coolant")
                    "The core ran, and the heat went into places heat is not meant to go. We have the time we needed and fewer people to spend it on. The next world is the end of the voyage, unless we spend still more to go one further."
                },
            ), weight = 0.18, condition = eligible),
    )
}
