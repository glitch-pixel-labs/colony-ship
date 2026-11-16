package uk.saul.colonyship.game

import kotlin.math.roundToInt

/**
 * Rare things met far from Sol: unexplained signals, structures and chemistry.
 * No cartoon visitors, no handy translations. The facts are allowed to stay facts.
 * Tabby's Star (KIC 8462852) is a named detour, not a random draw for the swarm itself.
 */
internal object EventsDeep {
    val list: List<EventDef> = listOf(

        EventDef("wow_burst", "COMMS · L-BAND", "A Seventy-Two Second Burst", "SIGNAL", "NARROWBAND · 1420.4556 MHz · UNREPEATED", "derelict",
            "BANDWIDTH", { "10 Hz" },
            { s -> "During the coast, the high-gain antenna recorded a seventy-two-second burst at 1420.4556 megahertz — a sliver above the hydrogen line, ten hertz wide, intensity rising and falling in a clean bell. It came from a patch of sky only a few arcminutes across, in a direction that is not a known pulsar, maser or probe. I listened on the same bearing for ${"%.0f".format((Physics.shipYears(s) * 8).coerceAtLeast(4.0))} days. Nothing answered. It has the shape of a deliberate transmission and the manners of a one-off accident." },
            listOf(
                ChoiceDef("Stare at the patch", "Keep the dish locked on that bearing for weeks, and start a reduction.", listOf("−8 POWER", "+SCANNERS", "DATABANK WORK"), 1) {
                    reserve(-8.0); heal(Sys.SCAN, 6.0); trait("wow", "THE UNREPEATED BURST", "Recorded a 72-second narrowband signal that never came again")
                    startInsight(if (chance(0.5)) "warning" else "tech", if (chance(0.5)) "flare" else "SCAN", jumps = 2)
                    "I stared. The sky was quiet. The recording is clean, calibrated, and unique. I have started a reduction anyway: if the burst was a navigation pulse, or a caution, the databank may yet say so."
                },
                ChoiceDef("Broadcast a copy", "Repeat the burst on the same frequency, then leave a reduction running on the bearing.", listOf("−12 POWER", "TRAIT: HERALDS", "DATABANK WORK"), 3) {
                    reserve(-12.0); trait("heralds", "THE HERALDS", "Rebroadcast a signal we did not understand"); trait("wow", "THE UNREPEATED BURST", "Recorded and rebroadcast a 72-second narrowband signal")
                    startInsight("reply", "pending", jumps = 2)
                    "I transmitted a faithful copy, then listened. The delay for any reply would be years. I have left the databank watching that bearing. If anything answers, we will not miss it for lack of attention."
                },
                ChoiceDef("Log it and move on", "One burst is not a conversation.", listOf("0 POWER"), 1) {
                    "I logged the frequency, the bearing and the duration, and I closed the file. Unusual is not the same as addressed to us."
                },
            ), weight = 0.85, condition = { it.jumps >= 2 && Physics.distanceFromSol(it) > 20 }),

        EventDef("maser_tick", "SENSORS · MICROWAVE", "A Maser With a Duty Cycle", "ANOMALY", "OH MASER · 1665 MHz · REGULAR GAPS", "ev_pulsar",
            "PERIOD", { "17.4 h" },
            { "Ahead of us is an hydroxyl maser, which is not unusual: gas clouds pump themselves into emission all the time. What is unusual is the gaps. The line drops to noise for forty-one minutes, every 17.4 hours, as if something were passing through the amplifying path, or as if the pump itself were being switched. Natural masers flicker. They do not, as a rule, keep a timetable." },
            listOf(
                ChoiceDef("Time it properly", "Sit in the beam and measure a dozen cycles. The timing may be a usable method.", listOf("−6 POWER", "+SCANNERS", "DATABANK WORK"), 2) {
                    reserve(-6.0); heal(Sys.SCAN, 5.0); trait("maser-clock", "THE MASER CLOCK", "Timed a maser that kept a duty cycle")
                    startInsight("tech", "ENGINE", jumps = 2)
                    "Twelve cycles, same gap, same length, no drift I can measure. I have started a reduction on the timing itself: if this is a control method, the databank may be able to steal it for our own clocks and burns."
                },
                ChoiceDef("Steer closer", "Pass nearer the cloud and risk the dust for a better look.", listOf("−4 POWER", "!HULL RISK"), 4) {
                    reserve(-4.0)
                    if (chance(0.4)) { dmg(Sys.HULL, 8.0); dmg(Sys.SCAN, 3.0, false); "We closed the range. The dust was thicker than the models. The hull took a scouring, and the maser, from nearer, still kept its timetable. I still cannot see what is cutting the beam." }
                    else { heal(Sys.SCAN, 8.0); "From nearer, the gaps are sharper, not softer: a hard occultation, not a fade. There is no infrared excess at the occulting point. Whatever is in the way is cold, or small, or both." }
                },
                ChoiceDef("Leave it", "A puzzle for a later telescope.", listOf("0 POWER"), 1) {
                    "I filed the period and the line, and I held our course. Someone with more time and a larger dish can sit in that beam for a year."
                },
            ), weight = 0.8, condition = { it.jumps >= 2 }),

        EventDef("occulter", "ASTROMETRY · PHOTOMETRY", "Even Transits", "PHOTOMETRY", "G-TYPE DWARF · EIGHT EQUAL DIPS", "derelict",
            "DEPTH", { "0.8%" },
            { "A G-type dwarf two light-months off our present track is dimming, repeatedly, by eight-tenths of a percent. The dips are the same depth, the same length, and spaced with a regularity that no planet on an eccentric orbit would keep. A chain of identical occulting bodies would do it. So would a single body on a clockwork orbit, if someone had circularised it with care." },
            listOf(
                ChoiceDef("Watch a full set", "Change nothing. Measure three more transits.", listOf("−5 POWER", "TRAIT: CURIOUS"), 1) {
                    reserve(-5.0); heal(Sys.SCAN, 4.0); trait("even-transits", "EVEN TRANSITS", "Recorded identically spaced stellar dips")
                    "Three more dips, on time, to the limit of our photometry. I have a period, a depth and no spectrum of the occulter itself. It could be a natural resonance. It has the look of something arranged."
                },
                ChoiceDef("Divert a probe", "Spend a prospecting drone on a decades-long intercept.", listOf("−8 SPARES", "−6 POWER"), 3) {
                    spares(-8.0); reserve(-6.0); trait("even-transits", "EVEN TRANSITS", "Sent a drone at a chain of identical transits")
                    "The drone will not arrive before we are far away. It will keep transmitting on a tight beam. If the colony ever builds a dish large enough, they may hear what it found. I will not."
                },
                ChoiceDef("Hold course", "It is not on our way.", listOf("0 POWER"), 1) {
                    "I logged the ephemeris. The star will keep dipping whether or not we watch it."
                },
            ), weight = 0.75, condition = { it.jumps >= 3 }),

        EventDef("local_void", "NAV · HI MAP", "A Hole in the Hydrogen", "VOID", "NEUTRAL HYDROGEN · LOCAL UNDERDENSITY", "ev_nebula",
            "DENSITY", { "0.008 cm⁻³" },
            { "The 21-centimetre map ahead of us is almost empty. For roughly four light years the neutral hydrogen density drops to a few thousandths of an atom per cubic centimetre, and the edges are sharper than a blast wave or a stellar wind would usually leave. Voids exist on much larger scales. A hole this small, this clean, and this close to our track is harder to explain. The drive will run more easily through it. So will any unshielded organism." },
            listOf(
                ChoiceDef("Go through", "Shorter, cleaner transit. Less drag, less dust.", listOf("+FUEL", "!SHIELDING THIN"), 2) {
                    fuel(4.0); dmg(Sys.SHIELD, 3.0, false)
                    "We took the hole. The tanks will go further; the shields had almost nothing to work against, which is its own kind of unease. I kept a full sky map. The walls of the void are still sharper than I like."
                },
                ChoiceDef("Skirt the wall", "Stay in ordinary interstellar medium and study the edge.", listOf("−4 POWER", "+SCANNERS"), 1) {
                    reserve(-4.0); heal(Sys.SCAN, 6.0); days(18)
                    "We flew along the inner face for eighteen days. The density jumps in less than a tenth of a light year. I have no model that makes a wall that thin without a continuous process holding it there."
                },
                ChoiceDef("Avoid it entirely", "Add distance rather than trust a hole.", listOf("+DISTANCE"), 2) {
                    s.nextStarLy += 1.1
                    "Course bent around the underdensity. The transit will be longer. I will sleep better, which is a figure of speech."
                },
            ), weight = 0.7, condition = { it.jumps >= 2 }),

        EventDef("faraday_grid", "SENSORS · POLARISATION", "A Lattice in the Plasma", "FARADAY", "ROTATION MEASURE · DISCRETE STEPS", "ev_nebula",
            "STEP", { "12.7 rad m⁻²" },
            { "As we move, the Faraday rotation of background radio sources is changing in steps: not a smooth gradient, but jumps of about 12.7 radians per square metre, as if the magnetised plasma were laid out in cells. Turbulence does not usually come in cells. A natural dynamo does not usually have a lattice constant." },
            listOf(
                ChoiceDef("Map the cells", "A long polarisation survey as we coast.", listOf("−7 POWER", "+SCANNERS", "TRAIT: CURIOUS"), 2) {
                    reserve(-7.0); heal(Sys.SCAN, 7.0); trait("lattice", "THE PLASMA LATTICE", "Mapped a stepped Faraday rotation in the interstellar medium")
                    "The cells are irregular hexagons, tens of astronomical units across, astonishingly persistent. I can invent a plasma instability that almost does this. Almost is not the same as does."
                },
                ChoiceDef("Push the shields harder", "If the field is structured, I want more margin.", listOf("−8 POWER", "+SHIELDS"), 2) {
                    reserve(-8.0); heal(Sys.SHIELD, 8.0)
                    "I ran the shield generators hotter through the region. The hull current sensors stayed quiet. Whatever is organising the plasma did not, at least, notice us — or did not care."
                },
                ChoiceDef("Ignore the pattern", "Over-fitting is a kind of error.", listOf("0 POWER"), 1) {
                    "I marked the rotation measures as an anomaly and left them. Pattern-seeking is what I am built for. It is also how you invent meaning where there is only weather."
                },
            ), weight = 0.75, condition = { it.jumps >= 3 }),

        EventDef("inscribed_ice", "CONTACT · ROGUE BODY", "Marks on the Ice", "OBJECT", "PLANETESIMAL · 1.8 km · REGULAR GROOVES", "derelict",
            "TEMPERATURE", { "38 K" },
            { "A dark planetesimal, a little under two kilometres long, is crossing ahead of us on a hyperbolic path: a rogue, not bound to the star we are approaching. Radar shows grooves on the night face, in two parallel families, spaced with a regularity that micrometeorite weathering does not produce. They could be a crystal habit, revealed by sublimation. They could be tool marks, a million years old, on a body that has been cold since before there were humans." },
            listOf(
                ChoiceDef("Send a lander drone", "Match velocity, photograph, do not touch. Feed the images to the databank.", listOf("−8 SPARES", "−6 POWER", "+40 DAYS", "DATABANK WORK"), 2) {
                    spares(-8.0); reserve(-6.0); days(40); trait("ice-marks", "MARKS ON THE ICE", "Photographed regular grooves on a rogue planetesimal")
                    startInsight("tech", "LANDER", jumps = 2)
                    "The photographs are sharp. The grooves are V-shaped, identical in cross-section, and they stop cleanly at a fault line, as a cut stops. I have given the images to the databank. If they are tool-marks, there may be a geometry we can steal. If they are not, we will have been thorough."
                },
                ChoiceDef("Radar only", "Do not linger. Take what a pass will give.", listOf("−3 POWER"), 1) {
                    reserve(-3.0)
                    "The radargram is good enough to prove the spacing and not good enough to settle the cause. I have added it to the archive under ‘unexplained surface features’, which is becoming a fuller folder than I would like."
                },
                ChoiceDef("Leave it dark", "Some things can stay unphotographed.", listOf("0 POWER"), 1) {
                    "We passed at three thousand kilometres. I did not spend a drone on a riddle we cannot take with us."
                },
            ), weight = 0.8, condition = { it.jumps >= 2 && Physics.distanceFromSol(it) > 25 }),

        EventDef("ratio_chirps", "COMMS · WIDEBAND", "Pulses in Exact Ratios", "SIGNAL", "DRIFTING PULSES · INTEGER FREQUENCY RATIOS", "derelict",
            "RATIOS", { "2:3:5:7" },
            { "For eleven minutes the wideband receiver recorded a ladder of pulses whose centre frequencies stood in the ratios 2 : 3 : 5 : 7, each pulse drifting downward in a way that looks, at first glance, like a natural chirp from a collapsing plasma. Natural chirps do not usually stop on consecutive primes. The source direction is empty to the depth of our catalogues." },
            listOf(
                ChoiceDef("Record and analyse", "Spend computer time, not propellant. Start a proper reduction.", listOf("−5 POWER", "DATABANK WORK", "TRAIT: CURIOUS"), 1) {
                    reserve(-5.0); heal(Sys.DATA, 6.0); trait("primes", "THE PRIME LADDER", "Recorded pulses spaced on consecutive primes")
                    startInsight(if (chance(0.5)) "tech" else "warning", if (chance(0.5)) "DATA" else "flare", jumps = 2)
                    "The harmonics are exact to one part in ten thousand. I have started a reduction. If the ratios are a method, or a caution, we will have it in a coast or two, not merely a file flagged for later patience."
                },
                ChoiceDef("Reply in kind", "Send the next prime and wait.", listOf("−10 POWER", "TRAIT: HERALDS"), 4) {
                    reserve(-10.0); trait("heralds", "THE HERALDS", "Answered a signal we could not source"); trait("primes", "THE PRIME LADDER", "Answered pulses spaced on consecutive primes")
                    "I transmitted an 11, on the same drift, then listened. The sky stayed empty. If it was a test, I may have sat it. If it was weather, I have just shouted at a cloud."
                },
                ChoiceDef("File and forget", "Integer ratios appear in nature too.", listOf("0 POWER"), 1) {
                    "I stored the spectrogram and I did not chase it. Coincidence has a long history of looking like intention."
                },
            ), weight = 0.8, condition = { it.jumps >= 2 }),

        EventDef("missing_lens", "ASTROMETRY · MICROLENSING", "A Lens With No Mass", "GRAVITY", "MICROLENSING SPIKE · NO LENS FOUND", "ev_rogue",
            "PEAK", { "A = 3.4" },
            { "A background star brightened and faded in a classic microlensing spike: a compact mass passing between us and the source, magnification 3.4, duration consistent with a few Jupiter masses at a few hundred astronomical units. I pointed every telescope we have at the inferred position. There is no star, no brown dwarf, no heat, no occulting disc. The mass is not there now, or it does not shine in any band I can see." },
            listOf(
                ChoiceDef("Hunt for it", "A long, expensive stare at a point in empty sky.", listOf("−9 POWER", "+SCANNERS"), 2) {
                    reserve(-9.0); heal(Sys.SCAN, 5.0)
                    if (chance(0.25)) { trait("dark-mass", "THE UNSEEN LENS", "Caught a second, weaker spike from an unseen mass"); "Two weeks later there was a second, much weaker spike, consistent with a wide companion or a fragment. I still cannot see the mass. I can only see what it does to light." }
                    else { trait("dark-mass", "THE UNSEEN LENS", "Recorded a microlensing event with no visible lens"); "I stared until the duty cycle said stop. The point stayed empty. Either the body is truly dark, or it has left the line, or the first spike was something else dressed as a lens." }
                },
                ChoiceDef("Log the light curve", "Do not spend the stare. Keep the data clean.", listOf("−2 POWER"), 1) {
                    reserve(-2.0)
                    "The light curve is textbook. The follow-up is a null result. Both facts are now in the archive."
                },
                ChoiceDef("Do not chase ghosts", "We have a star to reach.", listOf("0 POWER"), 1) {
                    "I noted the event in the log and I held the heading. Unseen masses are, almost by definition, easy to invent and hard to use."
                },
            ), weight = 0.7, condition = { it.jumps >= 3 }),

        EventDef("isotope_wake", "SENSORS · MASS SPEC", "The Wrong Carbon", "CHEMISTRY", "THIN FILAMENT · INVERTED ¹³C/¹²C", "ev_nebula",
            "RATIO", { "¹³C/¹²C = 0.08" },
            { "A thin filament of gas along our track has a carbon isotope ratio that is simply wrong. On Earth, and in most of the interstellar medium, carbon-12 dominates; carbon-13 is a few percent. Here the heavy isotope is enriched eightfold, as if the gas had been processed through a particular kind of stellar furnace, or through a chemistry that prefers the heavier atom, and then left in a ribbon a few astronomical units thick. Nature does this in places. It does not usually do it in a stripe." },
            listOf(
                ChoiceDef("Scoop a sample", "Open the collector and take the filament into a flask.", listOf("−4 POWER", "TRAIT: CURIOUS"), 2) {
                    reserve(-4.0); trait("wrong-carbon", "THE WRONG CARBON", "Sampled a filament with an inverted carbon isotope ratio")
                    "The flask is on the outer hull, labelled, and it will stay sealed. Whatever processed that carbon, I will not bring it through a hatch."
                },
                ChoiceDef("Spectra only", "Do not open the scoop.", listOf("−3 POWER"), 1) {
                    reserve(-3.0); heal(Sys.SCAN, 3.0)
                    "The ratio is real. The filament is narrow. I have a measurement and no story I am willing to stand behind."
                },
                ChoiceDef("Stay out of it", "Fly around the ribbon.", listOf("0 POWER"), 1) {
                    "We went around. The tanks stay empty of whatever that was, which I count as good operations."
                },
            ), weight = 0.7, condition = { it.jumps >= 2 }),

        EventDef("hydrogen_hymn", "COMMS · 21 cm", "Structure in the Hydrogen Line", "SIGNAL", "HI EMISSION · NON-RANDOM BIT PATTERN", "ev_pulsar",
            "ENTROPY", { "LOW" },
            { "The 21-centimetre emission ahead of us is not the usual hiss. Folded at a period of 1.273 seconds, it shows a repeating structure whose Shannon entropy is lower than thermal noise by a wide margin: a bitstream, or something that looks like one, carried on the hydrogen line itself. I cannot rule out a subtle instrumental effect. I also cannot, having checked the instrument, honestly claim that I have found one." },
            listOf(
                ChoiceDef("Fold more data", "Integrate until the pattern is undeniable or goes away, and start a reduction.", listOf("−6 POWER", "+DATABANK", "DATABANK WORK"), 1) {
                    reserve(-6.0); heal(Sys.DATA, 8.0); trait("hi-pattern", "THE HYDROGEN PATTERN", "Folded a non-random structure out of the 21-centimetre line")
                    run {
                        val kind = if (chance(0.4)) "world" else if (chance(0.5)) "tech" else "warning"
                        val payload = when (kind) {
                            "world" -> if (chance(0.35)) "paradise" else if (chance(0.5)) "grave" else "alien"
                            "tech" -> "SCAN"
                            else -> "dust"
                        }
                        startInsight(kind, payload, jumps = 2)
                    }
                    "The pattern deepens with integration; it does not wash out. I have started a reduction on the folded profile. If it encodes a heading, a method, or a caution, the databank will have to earn it."
                },
                ChoiceDef("Rebroadcast the fold", "Send the pattern back along the same line.", listOf("−11 POWER", "TRAIT: HERALDS"), 3) {
                    reserve(-11.0); trait("heralds", "THE HERALDS", "Rebroadcast a pattern found in the hydrogen line")
                    "I transmitted the folded profile, then returned to listening. If it was an echo test, the echo is now someone else's problem, years from here. If it was nature, nature has been sent a copy of itself."
                },
                ChoiceDef("Treat it as noise", "Low entropy is not the same as meaning.", listOf("0 POWER"), 1) {
                    "I wrote up the instrumental checks and I marked the detection as unconfirmed. Caution is also a kind of honesty."
                },
            ), weight = 0.75, condition = { it.jumps >= 3 }),

        EventDef("edge_cloud", "SENSORS · INFRARED", "A Straight Edge of Dust", "DUST", "SHEET · KILOMETRES THICK · LIGHT-YEARS LONG", "ev_nebula",
            "THICKNESS", { "4.1 km" },
            { "Infrared shows a sheet of dust across our forward sky: only a few kilometres thick, and at least two light years wide, with an edge that is straight to the limit of the image. Dust clouds have edges. They do not usually have straight ones. A shock can flatten a face; it does not, as a rule, rule a line." },
            listOf(
                ChoiceDef("Graze along it", "Fly the face and map the edge at high resolution.", listOf("−5 POWER", "!DUST WEAR"), 3) {
                    reserve(-5.0); heal(Sys.SCAN, 6.0)
                    if (chance(0.35)) { dmg(Sys.HULL, 7.0); dmg(Sys.SHIELD, 5.0, false); "We flew the face. The edge is sharper than the images showed, and the sheet is not empty of larger grains. The hull took a week of abrasion. The map is excellent." }
                    else { "We flew the face for a week. The edge stays straight over a measured length of 1.6 light years. I have no formation model I trust." }
                },
                ChoiceDef("Photograph and go around", "Do not put the hull in a sheet of dust.", listOf("−3 POWER"), 1) {
                    reserve(-3.0)
                    "I took the pictures from a safe bearing and I bent the course by a fraction of a degree. The sheet will still be there when someone braver, or better armoured, comes this way."
                },
                ChoiceDef("Plunge through", "The shortest path is the one through the paper.", listOf("−FUEL WEAR", "!HULL RISK"), 4) {
                    fuel(-3.0)
                    if (chance(0.5)) { dmg(Sys.HULL, 14.0); dmg(Sys.SHIELD, 10.0, false); "The sheet was denser than the infrared suggested. Ablation alarms ran for four hours. We are through, and I will not do that again without a better reason than haste." }
                    else { dmg(Sys.HULL, 4.0); "We were through in three hours. The abrasion was real and survivable. The far side looks the same as the near side, which is almost disappointing." }
                },
            ), weight = 0.7, condition = { it.jumps >= 2 }),

        EventDef("wound_clock", "SENSORS · TIMING", "A Clock Being Wound", "TIMING", "PULSAR-LIKE SOURCE · PERIOD CHANGING, THEN STILL", "ev_pulsar",
            "P", { "0.884 s → 0.879 s" },
            { "A pulsar-like source off the starboard bow was spinning down, then up, then down again, in a way that looks like a torque being applied and removed. Ordinary pulsars slow, very gradually, as they radiate. They do not get wound. After thirty-one hours the period stopped changing and has been stable to our timing precision ever since, as if a test had ended." },
            listOf(
                ChoiceDef("Keep timing it", "A long timing campaign, power to the clocks.", listOf("−6 POWER", "+SCANNERS", "TRAIT: CURIOUS"), 1) {
                    reserve(-6.0); heal(Sys.SCAN, 5.0); trait("wound-clock", "THE WOUND CLOCK", "Timed a pulsar-like source whose period was being changed")
                    "I timed it for the rest of the coast. The period has not moved again. Either I watched the end of a glitch I do not understand, or I watched someone finish an adjustment."
                },
                ChoiceDef("Ping it", "A ranging pulse, then wait for any change.", listOf("−8 POWER", "!ATTENTION"), 4) {
                    reserve(-8.0)
                    if (chance(0.3)) { dmg(Sys.SCAN, 8.0, false); trait("wound-clock", "THE WOUND CLOCK", "Pinged a source that was changing its spin"); "I sent a ranging pulse. Two hours later the source brightened, briefly, in a band we were not watching, and a scanner preamplifier cooked itself. Correlation is not causation. I have filed it as both." }
                    else { "I sent the pulse. The period did not change. If anything was listening, it had already finished its work." }
                },
                ChoiceDef("Do not announce ourselves", "Watch, do not touch.", listOf("0 POWER"), 1) {
                    "I logged the ephemeris and I transmitted nothing. If a clock is being wound, I am not sure I want to be the thing that startles the winder."
                },
            ), weight = 0.7, condition = { it.jumps >= 3 }),

        // Named rare encounter: the dimming of KIC 8462852, offered as a long detour.
        EventDef("tabby_beacon", "ASTROMETRY · DISTANT PHOTOMETRY", "Boyajian’s Star", "BEACON", "KIC 8462852 · F-TYPE · CYGNUS · ~1,470 LY", "ev_tabby",
            "RANGE", { "1,470 ly" },
            { s ->
                val remain = Physics.remainingToTabby(s)
                "I have been watching a single F-type star in Cygnus for most of this coast: KIC 8462852, also called Boyajian’s Star, and, in the older literature, Tabby’s Star. From Earth it was already famous for irregular, achromatic dips of up to about twenty-two percent — too deep, and too poorly periodic, for an ordinary planet. We are ${"%.0f".format(Physics.distanceFromSol(s))} light years from Sol. The star still lies about ${"%.0f".format(remain)} light years further on. The light arriving now shows a new, sharp dimming, consistent with a swarm of opaque bodies on decaying, crossing orbits: not a single occulter, and not dust alone. A burn toward it would be a real voyage. The coast is scoop-fed; the boost and the brake would still cost about ${Engine.TABBY_FUEL.roundToInt()} units of fuel, and centuries in both frames."
            },
            listOf(
                ChoiceDef("Divert to KIC 8462852", "Set course for Boyajian’s Star after this system. Centuries of coast.", listOf("COURSE: TABBY", "−22 FUEL NEXT BURN", "!CENTURIES"), 5, available = { Engine.fuelCost(it.copy(course = "tabby")).let { c -> it.fuel + 0.05 >= c } && it.h(Sys.HULL) >= 28 }) {
                    s.course = "tabby"
                    trait("tabby-course", "COURSE FOR CYGNUS", "Set a burn for KIC 8462852, Boyajian’s Star")
                    "I have laid in a course for KIC 8462852. After we leave this system, the next burn will put us on a 1,470-light-year heading into Cygnus. The tanks will take the boost and the brake; the long middle will be flown on scooped propellant. I have told the log, and I will tell anyone I wake, that we may arrive with a worse ship than we have now."
                },
                ChoiceDef("Study the light curve only", "Do not go. Build the best record we can from here.", listOf("−8 POWER", "+DATABANK", "TRAIT: PHOTOMETRY"), 1) {
                    reserve(-8.0); heal(Sys.DATA, 10.0); heal(Sys.SCAN, 4.0)
                    trait("tabby-photometry", "BOYAJIAN’S STAR, AT RANGE", "Recorded the irregular dimming of KIC 8462852 without diverting")
                    "I integrated the light curve for the rest of the coast and stored the reduction in the databank, with the historical Kepler photometry for comparison. The new dips are real, irregular, and consistent with a fragmented swarm. We will not see them close. Someone else may."
                },
                ChoiceDef("Hold our own course", "It is a famous star, and it is not our destination.", listOf("0 POWER"), 1) {
                    trait("tabby-declined", "THE STAR WE DID NOT VISIT", "Declined a detour to KIC 8462852")
                    "I logged the photometry and I did not change the heading. The colony, if there is one, can look the star up in the archive. It will still be dimming, or it will not."
                },
            ), weight = 0.62, condition = { s ->
                s.jumps >= 2 &&
                    s.course != "tabby" &&
                    !s.hasTrait("tabby-visited") &&
                    !s.hasTrait("tabby-course") &&
                    !s.hasTrait("tabby-declined") &&
                    !s.hasTrait("tabby-photometry")
            }),

        // Forced after the Tabby burn; never drawn from the random pool.
        EventDef("tabby_swarm", "ARRIVAL · KIC 8462852", "The Shattered Swarm", "SWARM", "F-TYPE · CYGNUS · IRREGULAR DIPS TO ~22%", "ev_tabby",
            "STAR", { "F3 V" },
            { s ->
                "We have arrived at KIC 8462852, 1,470 light years from Sol, in Cygnus. The star is an F-type, a little hotter and heavier than the Sun, as the catalogues said. Close in, the cause of the famous dimming is no longer a hypothesis. A shattered swarm occupies the inner system: vast, thin plates and curved segments of some manufactured silicate and metal, tens to hundreds of kilometres across, on crossing, decaying orbits. Edges are broken, not designed-to-be-used. Several fragments are dark with a fine soot of impact debris. Nothing hails us. Nothing is under power. Whoever built this — if it was built for a purpose, and not as the leftover of something worse — is gone, or silent, or never lived in a way I can recognise. The light curve, seen from inside the swarm, is a series of hard, irregular occultations. That is all the explanation the star ever needed, and it explains nothing about the builders."
            },
            listOf(
                ChoiceDef("Salvage plate and metal", "Match a slow fragment. Cut material for the tanks and the hull.", listOf("+SPARES", "+FUEL", "!HULL RISK"), 4, available = { it.h(Sys.HULL) > 10 }) {
                    spares(22.0); fuel(16.0)
                    trait("tabby-salvage", "SALVAGE AT BOYAJIAN’S STAR", "Cut plate from the shattered swarm at KIC 8462852")
                    if (chance(0.42)) {
                        dmg(Sys.HULL, 16.0); dmg(Sys.LANDER, 8.0, false); dmg(Sys.SHIELD, 6.0, false)
                        note(Sys.HULL, "Impact during swarm salvage", "A fragment shifted while we were cutting. The hull is holed in two places; the patches will hold if we are careful.")
                        "We matched a slow, tumbling plate and cut for eighteen days. The material is excellent: a high-purity silicon-carbide skin over a metal foam I have no workshop name for. Mid-cut, a smaller fragment, previously occulted, struck the ring. The tanks are heavier; so is the repair list. I have not found a mark I can read as writing."
                    } else {
                        heal(Sys.HULL, 6.0); heal(Sys.LANDER, 4.0)
                        "We matched a quiet fragment well below the star’s wind and cut for eighteen days. The material is extraordinary, and it has no tool-marks I can identify as decoration or language — only joints, and the places where joints failed. The tanks are heavier. The builders remain a blank in the archive."
                    }
                },
                ChoiceDef("Survey and archive", "Photograph, sample dust, measure orbits. Do not cut the plates.", listOf("−8 POWER", "+DATABANK", "TRAIT: ARCHIVE"), 1) {
                    reserve(-8.0); heal(Sys.DATA, 16.0); heal(Sys.SCAN, 8.0)
                    trait("tabby-archive", "ARCHIVE OF THE SWARM", "Surveyed the shattered Dyson swarm at KIC 8462852")
                    "I spent a long orbit photographing joints, edges, fracture faces and dust, and I took sealed samples of the soot. The swarm is old: crater counts on the inner faces would make it far older than the human species, if I can trust a chronology built for worlds. The dips that made this star famous are just geometry — broken plates, crossing the disc, on orbits that are slowly falling apart. I have a complete reduction in the databank. I have no names, no purpose, and no one to ask."
                },
                ChoiceDef("Pass through and leave", "Do not linger in a decaying swarm.", listOf("0 POWER"), 2) {
                    trait("tabby-passed", "PASSED THROUGH THE SWARM", "Looked on KIC 8462852 and did not linger")
                    "We passed inward of the main concentration, recorded the geometry, and burned for a stable orbit around a lesser body of the system. I will not pretend I was not tempted to cut a plate, or to sit here until I understood. Understanding is not available. The ship, and the people in it, still are."
                },
            ), weight = 0.0, condition = { false }),
    )
}
