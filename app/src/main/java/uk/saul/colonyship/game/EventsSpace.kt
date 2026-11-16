package uk.saul.colonyship.game

/** Things the universe does to us: dust, stars, strangers, and the long consequences of speed. */
internal object EventsSpace {
    val list: List<EventDef> = listOf(
        EventDef("storm", "INTERSTELLAR MEDIUM · BOW", "Dust Front", "BOW ALERT", "DENSE FILAMENT · GRAIN FLUX ×40", "storm",
            "GRAINS / HR", { "3,400" },
            { "We are crossing a denser filament of the interstellar medium. At ${"%.2f".format(it.lastBeta.coerceAtLeast(0.5))} of light speed a grain of dust a tenth of a millimetre across arrives with the energy of a rifle round, and the bow ice is taking several thousand an hour. I can pour power into the deflectors, let the ice do what it was built for, or bring someone out to brace the frame behind it." },
            listOf(
                ChoiceDef("Charge the deflectors", "Ionise the grains with the forward laser and turn them aside magnetically.", listOf("−18 POWER", "HULL HOLDS"), 1) {
                    reserve(-18.0); dmg(Sys.HULL, 3.0); dmg(Sys.SHIELD, 4.0, false)
                    "Eleven hours at full field. Most of the grains were charged and pushed aside; the rest arrived as a faint glowing rain on the ice. Hull loss is within tolerance. The deflector coils will take weeks to cool."
                },
                ChoiceDef("Let the ice take it", "The bow shield exists to be eaten. The question is how much of it we can spare.", listOf("0 POWER", "!HULL −12%", "!PODS AT RISK"), 4) {
                    dmg(Sys.HULL, 14.0); failPods(4 + r.nextInt(8)); if (chance(0.4)) dmg(Sys.CRYO, 6.0, false)
                    "We lost four metres of bow ice and, where it thinned, the impacts reached the frame. The shock ran aft along the spine and arrived in ring section three as a series of hammer blows. Several pod seals there have lost their vacuum margin."
                },
                ChoiceDef("Wake Chief Okonkwo", "He can brace the forward frame behind the thinnest ice.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("okonkwo")) {
                    wake("okonkwo"); trust(-1); dmg(Sys.HULL, 5.0)
                    "Chief Okonkwo came out of the thaw shaking, with a headache that will last a week. He read the impact log, asked nothing, and spent thirty hours bracing the forward frame. Before he went back to his bunk he asked me, quietly, how often I planned to do this to him."
                },
                ChoiceDef("Send Okonkwo forward", "He is already awake. Tether, torch, and the narrow space behind the ice.", listOf("−8 SPARES", "HULL HOLDS"), 3, isAwake("okonkwo")) {
                    spares(-8.0); dmg(Sys.HULL, 4.0)
                    if (chance(0.2)) { trust(-1); "A grain came through a gap in the ice and punched a hole in his glove the width of a needle. He sealed it himself, finished the brace, and then sat in the airlock for an hour without speaking. He has asked that next time I give him the odds before he goes, not after." }
                    else "He worked behind the shield for nine hours with the impacts ringing through the frame like rain on a tin roof. The brace is good. He came back in, ate, and slept for a day and a half."
                },
            )),
        EventDef("flare", "STELLAR ACTIVITY · RED DWARF", "Proton Storm", "RADIATION", "FLARE FRONT INBOUND · 40 MIN", "flare",
            "MeV PROTONS", { "×10⁴" },
            { "The red dwarf we are passing has flared. Red dwarfs do this; it is the main reason nobody wanted to settle near one. A front of high-energy protons will reach us in forty minutes. The pods are shielded against cosmic rays, not against this. I can harden the field, put the reaction-mass tanks between the star and the sleepers, or ride it out." },
            listOf(
                ChoiceDef("Field to maximum", "Push the magnetic shield past its rating until the front passes.", listOf("−15 POWER", "!SHIELDS −8%"), 2) {
                    reserve(-15.0); dmg(Sys.SHIELD, 8.0, false); dmg(Sys.SCAN, 3.0, false)
                    "The field held the bulk of the front. Two of the superconducting coils quenched afterwards and will need nursing back, but the dosimeters in the cryo array barely moved."
                },
                ChoiceDef("Roll the tanks to the star", "Turn the ship so twelve thousand tonnes of hydrogen stand between the flare and the pods.", listOf("+40 DAYS", "!ENGINES −5%", "PODS SAFE"), 2) {
                    days(40); dmg(Sys.ENGINE, 5.0, false)
                    "We rolled the ship broadside to the star and let the reaction mass take the dose. Correcting the attitude afterwards cost forty days of trimming and some wear on the gimbals. The sleepers were never safer."
                },
                ChoiceDef("Ride it out", "Do nothing expensive and accept the dose.", listOf("0 POWER", "!PODS AT RISK", "!GENETIC DAMAGE"), 4) {
                    dmg(Sys.SCAN, 10.0, false); dmg(Sys.LIFE, 4.0, false); failPods(5 + r.nextInt(10)); trait("irradiated", "GENETIC LOAD", "Took a red-dwarf flare unshielded")
                    "The front passed in six hours. Nobody died today. But a dose like that breaks DNA, and broken DNA in vitrified cells cannot repair itself until thaw. Some of it will be carried forward. The colony’s children will be the ones who find out how much."
                },
            )),
        EventDef("derelict", "CONTACT · 2,000 KM", "The Derelict", "UNKNOWN HULL", "NON-HUMAN VESSEL · NO EMISSIONS", "derelict",
            "EST. AGE", { "~40,000 YRS" },
            { "We are braking through a quiet system and something is in orbit that we did not put there. Its hull alloy is nothing in my catalogue. Cosmic-ray tracks in the surface put its age at about forty thousand years. There are breaches in its hull and a weak thermal source deep inside. I have no idea what it is. I have every reason to be careful." },
            listOf(
                ChoiceDef("Strip it for material", "Drones cut plate and fittings. We need spares more than we need mysteries.", listOf("+SPARES", "!UNKNOWN RISK"), 3) {
                    spares(25.0); trait("relics", "RELIC-KEEPERS", "Built with alien metal")
                    if (chance(0.3)) { dmg(Sys.HULL, 10.0, false); failPods(6); "We came away with tonnes of an alloy I still cannot fully characterise. One compartment we cut into was still pressurised; it vented through the cutting drone and sprayed debris across our ring. The repairs will take months. The metal is extraordinary." }
                    else "The drones came back with tonnes of a dense, self-annealing alloy that will patch our hull better than anything we brought. Whoever made it did not leave a body, a grave or a name."
                },
                ChoiceDef("Read its memory", "There is structured data in a crystalline lattice. Copy it. The databank will need a transit or two, and power, to finish the reduction.", listOf("−8 POWER", "DATABANK WORK", "TRAIT: ARCHIVE"), 2) {
                    reserve(-8.0); heal(Sys.DATA, 6.0)
                    trait("curious", "CURIOUS", "Read a dead ship’s memory")
                    trait("archive", "THE ALIEN ARCHIVE", "Copied a non-human library that is still being reduced")
                    val roll = r.nextDouble()
                    val kind = if (roll < 0.42) "world" else if (roll < 0.72) "tech" else "warning"
                    val payload = when (kind) {
                        "world" -> when { r.nextDouble() < 0.28 -> "paradise"; r.nextDouble() < 0.50 -> "grave"; else -> "alien" }
                        "tech" -> listOf("HULL", "ENGINE", "CRYO", "DATA", "SCAN", "SHIELD").random(r)
                        else -> listOf("reactor", "cryo", "hull", "coolant", "dust", "flare").random(r)
                    }
                    startInsight(kind, payload, jumps = if (chance(0.45)) 1 else 2)
                    "I imaged the lattice for nine days and took away a structured store I cannot yet read. Star positions, already, corrected for forty millennia of drift; the rest is still reduction, not meaning. The databank will keep at it through the next coast, at a cost in reserve power. In a transit or two I may have something we can actually use."
                },
                ChoiceDef("Leave it alone", "Log it, photograph it, and keep our distance.", listOf("0 POWER", "TRAIT: CAUTIOUS"), 1) {
                    trait("cautious", "CAUTIOUS", "Left the derelict untouched")
                    "We passed at two thousand kilometres and I took every image I could. Then we left it as we found it. It may be the most important object any human has ever seen, and I chose not to touch it. I would make the same choice again, and I would not be sure."
                },
            )),
        EventDef("signal", "COMMS · 1420 MHz", "A Narrowband Signal", "SIGNAL", "ARTIFICIAL CARRIER · HYDROGEN LINE", "derelict",
            "BANDWIDTH", { "0.4 Hz" },
            { "Nature does not make signals one hertz wide. This one sits on the hydrogen line, drifting in frequency the way a transmitter does on a rotating planet. Its source is about thirty light years off our course. Whatever sent it did so when our launch was still being argued over in committee." },
            listOf(
                ChoiceDef("Answer it", "A tight beam back along the bearing. Primes, then a picture of us. If anything replies, the databank will catch it later.", listOf("−6 POWER", "TRAIT: HERALDS", "!WE ARE SEEN"), 3) {
                    reserve(-6.0); trait("heralds", "HERALDS", "Answered a voice in the dark")
                    startInsight("reply", "pending", jumps = 2)
                    "I sent the primes, then hydrogen, then a diagram of the ship with one thousand dots in it. Any reply is years away at best. I have left a reduction running on that bearing. We have told someone we exist and where we are going. That cannot be undone."
                },
                ChoiceDef("Record everything", "Listen, record, and let the databank work on it over the next coasts.", listOf("−4 POWER", "DATABANK WORK", "TRAIT: CURIOUS"), 1) {
                    reserve(-4.0); heal(Sys.DATA, 4.0); trait("curious", "CURIOUS", "Recorded a stranger’s signal")
                    val kind = if (chance(0.5)) "warning" else "tech"
                    val payload = if (kind == "tech") listOf("SCAN", "DATA").random(r) else listOf("dust", "flare", "hull").random(r)
                    startInsight(kind, payload, jumps = 2)
                    "I recorded for eleven days until the source fell out of our antenna’s field. It repeats with variations, like a long sentence revised many times. I have started a reduction. If there is anything in it we can use — a caution, a method, a heading — the databank will say so after another coast or two."
                },
                ChoiceDef("Go silent", "Drive emissions down and cross its sky as quietly as we can.", listOf("0 POWER", "TRAIT: HIDDEN"), 1) {
                    trait("hidden", "THE QUIET ONES", "Hid from the first voice we heard")
                    "I shut down the transponder and dimmed everything that radiates. A fusion torch cannot really hide, but we tried. If anyone was watching, they now know we are cautious, which may be worth something."
                },
            )),
        EventDef("rogue", "NAV · UNCHARTED MASS", "Rogue World", "GRAVITY", "STARLESS PLANET · APPROACH CORRIDOR", "ev_rogue",
            "CLOSEST PASS", { "1.9 Gm" },
            { "There is a planet in our braking corridor that belongs to no star. It was flung out of its own system long ago and has been cooling in the dark ever since; its surface is frozen nitrogen over a faintly warm core. It has a ring of debris. We are slow enough now that it matters." },
            listOf(
                ChoiceDef("Divert wide", "Burn sideways and give it a wide berth.", listOf("+60 DAYS", "!ENGINES −6%"), 1) {
                    days(60); dmg(Sys.ENGINE, 6.0, false); reserve(-6.0)
                    "We burned off-axis for three days and passed it at fifty million kilometres. Safe, and expensive in reaction mass and engine hours."
                },
                ChoiceDef("Thread the gap", "Keep the corridor and pass between the planet and its debris ring.", listOf("0 POWER", "!HULL RISK"), 4) {
                    if (chance(0.5)) { dmg(Sys.HULL, 12.0); failPods(5); "A fragment the size of a fist met the ring at eleven kilometres a second. It went through two bulkheads before the third stopped it." }
                    else { dmg(Sys.HULL, 3.0); "We threaded it. The debris passed on both sides, each piece lit for a moment by our floodlights. Nothing larger than sand touched us." }
                },
                ChoiceDef("Map it as we pass", "Use it to calibrate the scanners against a known body. Costs power, sharpens our eyes.", listOf("−10 POWER", "+SCANNERS", "TRAIT: CURIOUS"), 2) {
                    reserve(-10.0); heal(Sys.SCAN, 12.0); trait("curious", "CURIOUS", "Mapped a rogue world"); dmg(Sys.HULL, 4.0)
                    "I mapped every metre of its night side and used it to recalibrate our survey instruments. There is liquid water under the ice near its core. Something may live there. We cannot stop to find out."
                },
            )),
        EventDef("giant", "ARRIVAL · GAS GIANT", "Skimming the Giant", "FUEL", "UPPER ATMOSPHERE · DEUTERIUM RICH", "ev_giant",
            "D/H RATIO", { "2.1×10⁻⁵" },
            { "This system has a gas giant with a usable deuterium fraction, and we are slow enough to use it. A pass through its upper atmosphere would refill the fusion reserve. It would also mean flying a two-kilometre ship through a hydrogen storm at sixty kilometres a second." },
            listOf(
                ChoiceDef("Dive and scoop", "One deep pass, scoop open. Fill the reserve and the tanks.", listOf("+30 POWER", "+FUEL", "!HULL RISK"), 3) {
                    reserve(30.0); fuel(18.0)
                    if (chance(0.35)) { dmg(Sys.HULL, 12.0); dmg(Sys.SCAN, 8.0, false); "We went deeper than planned when a downdraft caught the ring. Ablation took some plating and a scanner mast. The reserve is full, and so are the tanks." }
                    else { dmg(Sys.HULL, 4.0); "The scoop ran for nineteen minutes. We came out trailing plasma, with the reserve and the deuterium tanks full. The heat shield is scorched but sound." }
                },
                ChoiceDef("Send drones", "Tethered collectors on a slower, safer orbit. Less fuel.", listOf("−6 SPARES", "+15 POWER", "+FUEL", "+20 DAYS"), 2) {
                    spares(-6.0); reserve(15.0); fuel(10.0); days(20)
                    "Six drones made forty passes on long tethers. Two were lost to turbulence. We have half what a dive would have given us, and all of our hull."
                },
                ChoiceDef("Keep going", "Fuel is useful. So is not dying in a gas giant.", listOf("0 POWER"), 1) {
                    "We passed at a respectful distance. Its storms looked like slow brushwork from here. I logged the deuterium fraction for anyone who comes this way later."
                },
            )),
        EventDef("comet", "ARRIVAL · OUTER CLOUD", "Comet Harvest", "VOLATILES", "COMETARY NUCLEUS · WITHIN REACH", "ev_comet",
            "WATER ICE", { "68%" },
            { "We are braking through this star’s outer cometary cloud and there is a nucleus four kilometres across within reach. It is two-thirds water ice, with ammonia and carbon compounds. Ice is water, and water split by electrolysis is air. Matching its velocity will cost time and fuel." },
            listOf(
                ChoiceDef("Match orbit and mine", "Park beside it for two months and cut ice.", listOf("+60 DAYS", "+WATER", "+AIR", "−10 POWER"), 2) {
                    days(60); reserve(-10.0); water(22.0); air(12.0)
                    "Sixty days alongside a black, slowly venting mountain. The drones cut eight thousand tonnes of ice. The tanks are full and the electrolysers are running."
                },
                ChoiceDef("Harpoon a fragment", "Grab a loose block on the way past without stopping.", listOf("+WATER", "!HULL RISK"), 3) {
                    water(10.0)
                    if (chance(0.3)) { dmg(Sys.HULL, 9.0); "The harpoon held, then the block split. Half of it struck the ring before the tether parted. We have some water and a new dent." }
                    else "The tether held. We reeled in a block of dirty ice the size of a house and melted it into the tanks."
                },
                ChoiceDef("Pass it by", "We have enough. Probably.", listOf("0 POWER"), 1) {
                    "We let it go by. Comets are common; the fuel to stop for one is not."
                },
            )),
        EventDef("nebula", "NAV · H II REGION", "Through the Glow", "DENSE MEDIUM", "IONISED HYDROGEN · ×200 DENSITY", "ev_nebula",
            "ATOMS / cm³", { "220" },
            { "Our course crosses the edge of an ionised hydrogen region, lit from inside by young blue stars. For the scoop it is a feast. For the bow, at our speed, every atom is a small radiation event, and there are a great many atoms." },
            listOf(
                ChoiceDef("Plough through", "Scoop open. Fill the reserve and accept the heating.", listOf("+20 POWER", "!HULL −8%", "!PODS AT RISK"), 3) {
                    reserve(20.0); dmg(Sys.HULL, 8.0); failPods(3 + r.nextInt(5))
                    "We went through glowing. The scoop gathered more hydrogen in a week than in the previous year. The bow is hotter than I like and some pods near the front picked up a dose."
                },
                ChoiceDef("Throttle back", "Cut speed while we cross. Slower, cooler, safer.", listOf("+120 DAYS", "+5 POWER"), 1) {
                    days(120); reserve(5.0)
                    "We eased off to a third of light speed while crossing. It cost four months. The view was worth some of it."
                },
                ChoiceDef("Go around", "A long detour clear of the cloud.", listOf("+200 DAYS", "!ENGINES −6%"), 2) {
                    days(200); dmg(Sys.ENGINE, 6.0, false)
                    "We went around. The cloud filled half the sky for months, lit pink and teal by stars a few million years old. The sleepers missed it, as they miss everything."
                },
            )),
        EventDef("dust", "STRUCTURE · BOW ICE", "Erosion", "ABLATION", "BOW SHIELD · 61% REMAINING", "storm",
            "ICE LOSS", { "1.2 m/yr" },
            { "Nothing dramatic: just the steady erosion of the bow ice by the medium we fly through. At this rate the shield will be thin before our fifth star. I can rebuild it from our water, run the deflectors harder, or accept the loss and plan around it." },
            listOf(
                ChoiceDef("Re-ice the bow", "Spray water forward and let it freeze. Costs water we may want later.", listOf("−15 WATER", "+HULL"), 2) {
                    water(-15.0); heal(Sys.HULL, 12.0)
                    "Drones sprayed water across the bow in thin layers and the cold did the rest. The shield is two metres thicker. The tanks are lighter."
                },
                ChoiceDef("Harder deflection", "Raise the deflector duty cycle for the rest of the transit.", listOf("−12 POWER", "!SHIELDS −5%"), 1) {
                    reserve(-12.0); dmg(Sys.SHIELD, 5.0, false); heal(Sys.HULL, 3.0)
                    "Erosion is down by a third. The deflector coils are running warm and will age faster."
                },
                ChoiceDef("Accept it", "The ice is there to be used up.", listOf("0 POWER", "!HULL −7%"), 3) {
                    dmg(Sys.HULL, 7.0)
                    "Accepted. I have moved the date of failure in my plans. That is all accepting it means."
                },
            )),
        EventDef("pulsar", "NAV · NEUTRON STAR", "The Lighthouse", "PULSAR WIND", "MAGNETISED WIND · 0.3 PC", "ev_pulsar",
            "PERIOD", { "89 ms" },
            { "We are passing within a third of a parsec of a pulsar: a dead star twenty kilometres across, spinning eleven times a second, with a magnetic field strong enough to distort atoms. Its wind is hard on electronics. Its timing, on the other hand, is better than any clock I carry." },
            listOf(
                ChoiceDef("Fix our clocks on it", "Use its pulses to correct our navigation and time. Costs exposure.", listOf("−6 POWER", "+SCANNERS", "!SHIELDS −6%"), 2) {
                    reserve(-6.0); heal(Sys.SCAN, 10.0); dmg(Sys.SHIELD, 6.0, false); trait("navigators", "THE NAVIGATORS", "Steered by a dead star’s clock")
                    "I timed four million pulses. Our position is now known to within a few hundred kilometres across many light years, which is better than Earth could have told us. The shield coils took a beating doing it."
                },
                ChoiceDef("Harden and pass", "Shut down sensitive systems and pass quickly.", listOf("−10 POWER"), 1) {
                    reserve(-10.0); dmg(Sys.SCAN, 3.0, false)
                    "We passed with half the ship asleep in a stronger sense than usual. A few sensors came back confused. Nothing serious."
                },
                ChoiceDef("Ignore it", "It is far enough. Probably.", listOf("0 POWER", "!SCANNERS −10%"), 3) {
                    dmg(Sys.SCAN, 10.0, false); dmg(Sys.CRYO, 4.0, false)
                    "It was not quite far enough. Induced currents tripped a scanner bank and two cryo controllers. Both are back up. One of them lost its pod’s temperature history, which I will now never know."
                },
            )),
        EventDef("probe", "CONTACT · SMALL OBJECT", "The First Probe", "ARTEFACT", "LASER-SAIL PROBE · 2081 AD · DORMANT", "ev_probe",
            "VELOCITY", { "0.19c" },
            { s -> "We are overtaking something human. It is one of the gram-scale laser-sail probes launched in the 2080s: a chip on a sail of reflective film, aimed at a star none of them ever reached. It has drifted in silence for over a century. We are faster now; we caught it in ${earth(s)}." },
            listOf(
                ChoiceDef("Bring it aboard", "Match velocity for a day and take it in. A relic of the first attempt.", listOf("+20 DAYS", "−6 POWER", "TRAIT: RELICS"), 2) {
                    days(20); reserve(-6.0); trait("relics", "RELIC-KEEPERS", "Carried the first probe home to a new world")
                    "It weighs four grams. The sail is shredded by a century of dust. The chip still holds the images it took of a star it never reached, and a forty-second recording of the children who chose its name. It will be the oldest object in the colony."
                },
                ChoiceDef("Read it and let it go", "Copy its memory with a laser link. Leave it on its way.", listOf("−2 POWER", "TRAIT: SENTIMENTAL"), 1) {
                    reserve(-2.0); trait("sentimental", "SENTIMENTAL", "Listened to the first probe")
                    "I woke it with a pulse and read its memory in three seconds. Then it went back to drifting. It is still going somewhere, which is more than most things can say."
                },
                ChoiceDef("Leave it be", "It is not ours to disturb.", listOf("0 POWER"), 1) {
                    "We passed it at four hundred kilometres. I logged its position and did not wake it. Some things should be allowed to finish."
                },
            ), condition = { it.jumps >= 1 && Physics.distanceFromSol(it) < Physics.HUMAN_RELIC_LY }),
        EventDef("letter", "COMMS · LASER LINK · SOL", "A Letter from Home", "TRANSMISSION", "EARTH TRANSMISSION · DELAYED", "intro_earth",
            "SENT", { "${(earth(it) - Physics.distanceFromSol(it).toInt()).coerceAtLeast(Physics.LAUNCH_YEAR)} AD" },
            { s -> val sent = (earth(s) - Physics.distanceFromSol(s).toInt()).coerceAtLeast(Physics.LAUNCH_YEAR)
                "A laser transmission from the Solar System has caught up with us. It was sent in $sent AD and has been crossing ${ly(s)} light years ever since. It carries a catalogue update, some news, and four thousand personal messages addressed to sleepers. Some of the people who recorded them are certainly dead. It is now ${earth(s)} on Earth." },
            listOf(
                ChoiceDef("Answer it", "Send a full report home. It will arrive long after anyone who asked is gone.", listOf("−8 POWER", "TRUST +1"), 1) {
                    reserve(-8.0); trust(1)
                    "I sent our position, our losses and our hopes, and the names of everyone still alive. It will reach Sol in ${Physics.messageArrives(s)} AD. Somebody there, born long after we left, will read it."
                },
                ChoiceDef("Store the messages", "Keep the personal messages for each sleeper to hear on landing.", listOf("TRAIT: LETTERS"), 1) {
                    trait("letters", "LETTERS FROM HOME", "Carried four thousand messages to a new world")
                    "I filed every message under its addressee. One woman in ring six has eleven from her brother, sent over thirty years. She will hear all of them in an afternoon."
                },
                ChoiceDef("Read the news", "Decode the general broadcast and decide what to tell the crew.", listOf("−2 POWER", "?NEWS"), 2) {
                    reserve(-2.0)
                    if (chance(0.5)) { trust(1); trait("second-wave", "THE SECOND WAVE", "Heard that Earth launched again"); "The news is good. Two more ships have left Sol since we did, both faster than us. Earth is still sending. We are not the only attempt any more." }
                    else { trait("silence", "THE LAST BROADCAST", "Heard Earth go quiet"); "The news is not good. The general broadcast ends mid-sentence, eleven years after it began. The carrier continues, automated, with no new content. I do not know what happened. I will probably never know." }
                },
            ), weight = 0.9, condition = { it.jumps >= 2 }),
        EventDef("charts", "NAV · ASTROMETRY", "The Wrong Distance", "NAV ERROR", "TARGET PARALLAX · REVISED", "planet",
            "ERROR", { "+31%" },
            { "Now that we are close, our own parallax measurements disagree with the catalogue we left Earth with. The next star is about a third further away than charted. The catalogue was the best Earth could make. It was not good enough." },
            listOf(
                ChoiceDef("Push on", "Same star, longer transit.", listOf("+DISTANCE", "0 POWER"), 2) {
                    s.nextStarLy += 3.0
                    "Course held. The transit will be several years longer than planned, in both frames."
                },
                ChoiceDef("Retarget", "Switch to a closer star with a poorer catalogue entry.", listOf("−DISTANCE", "!UNKNOWN WORLD"), 3) {
                    s.nextStarLy = (s.nextStarLy - 2.0).coerceAtLeast(2.2)
                    "Retargeted. The new star is nearer and we know almost nothing about its planets. That is not new."
                },
                ChoiceDef("Measure properly", "Spend power on a long astrometric baseline before deciding.", listOf("−12 POWER", "+SCANNERS"), 1) {
                    reserve(-12.0); heal(Sys.SCAN, 8.0); s.nextStarLy += 1.0
                    "Six weeks of careful measurement. The error is real but smaller than feared. Our instruments are better calibrated for it."
                },
            )),
        EventDef("overtaken", "CONTACT · FUSION PLUME ASTERN", "The Latecomers", "VESSEL", "HUMAN SHIP · 0.94c · CLOSING", "intro_burn",
            "LAUNCHED", { "${Physics.LAUNCH_YEAR + 58} AD" },
            { s -> "A ship is overtaking us. It is human. It left Sol fifty-eight years after we did, with a better drive, and it will reach our destinations first. Its crew are awake; they are hailing us. They say they can spare parts, and that they have room for fifty sleepers who would arrive decades sooner." },
            listOf(
                ChoiceDef("Trade and talk", "Take the parts and the news. Thank them.", listOf("+SPARES", "+SHIELDS", "TRUST +1"), 1) {
                    spares(20.0); heal(Sys.SHIELD, 10.0); trust(1); trait("latecomers", "THE LATECOMERS", "Overtaken by a faster ship")
                    "Their engineers sent parts across on a cable and their captain spoke to me for an hour. She was courteous and a little embarrassed. Our whole voyage was, in a sense, made obsolete by her ship. It does not feel obsolete from in here."
                },
                ChoiceDef("Send fifty sleepers", "Transfer fifty pods. They arrive sooner, and somewhere else.", listOf("−50 PODS", "+POWER", "TRAIT: SCATTERED"), 3) {
                    reserve(15.0); val c = s.pods.toCharArray(); var n = 0
                    for (i in c.indices) if (n < 50 && c[i] == 'S') { c[i] = 'L'; n++ }
                    s.pods = String(c); deltas += "−$n PODS (TRANSFERRED)"
                    trait("scattered", "SCATTERED FAMILIES", "Gave fifty sleepers to a faster ship")
                    "Fifty pods crossed on the cable, chosen by lottery as the originals were. Their families stay with us. They will wake on a different world, decades before we land. It was the right thing to do and it felt like an amputation."
                },
                ChoiceDef("Decline contact", "Wish them well and keep our course.", listOf("0 POWER"), 2) {
                    trust(-1)
                    "I thanked them and declined. Their plume shrank ahead of us over a month and then was gone. The awake crew, when they read the log, will not understand why I did that. I am not sure I do."
                },
            ), weight = 0.8, condition = { it.jumps >= 2 && Physics.distanceFromSol(it) < Physics.HUMAN_RELIC_LY }),
        EventDef("organics", "CONTACT · INTERSTELLAR OBJECT", "Dark Chemistry", "SAMPLE", "TUMBLING BODY · COMPLEX ORGANICS", "ev_comet",
            "AMINO ACIDS", { "14 TYPES" },
            { "A small interstellar body is tumbling across our path during braking. Spectra show complex organic chemistry, including amino acids, some with the opposite handedness to Earth life. It is evidence, perhaps, of how life starts. It is also exactly the sort of thing a biologist would warn me not to bring aboard." },
            listOf(
                ChoiceDef("Take a sealed sample", "A drone cores it and seals the sample outside the hull.", listOf("−6 POWER", "TRAIT: CURIOUS"), 2) {
                    reserve(-6.0); trait("curious", "CURIOUS", "Sampled dark chemistry between stars"); trait("panspermia", "THE SEED QUESTION", "Carried primordial chemistry")
                    "The core is sealed in a vacuum flask on the outer hull and will stay there until a laboratory exists to open it. It may be the most important sample in the history of biology. It may be tar."
                },
                ChoiceDef("Analyse remotely", "Spectroscopy only. Nothing touches the ship.", listOf("−3 POWER"), 1) {
                    reserve(-3.0); heal(Sys.SCAN, 3.0)
                    "I took spectra for a week. The chemistry is real and complex. It is not alive. I think."
                },
                ChoiceDef("Ignore it", "Interesting is not the same as useful.", listOf("0 POWER"), 1) {
                    "We let it tumble by. Somewhere in its tar is an answer to a question I care about. I care more about the pods."
                },
            ), weight = 0.7),
    )
}
