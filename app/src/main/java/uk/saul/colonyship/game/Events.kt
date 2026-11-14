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

private fun asleep(id: String): (GameState) -> Boolean = { id !in it.awake }
private fun isAwake(id: String): (GameState) -> Boolean = { id in it.awake }

object Events {
    val all: List<EventDef> = listOf(
        EventDef("storm", "UNSCHEDULED WEATHER · OUTER HULL", "Micrometeoroid Storm", "HULL ALERT", "DEBRIS FIELD · BRACE BRACE BRACE", "storm",
            "IMPACTS / MIN", { "340" },
            { "We’re ploughing through high-velocity gravel and the hull is taking it personally. I can pour power into the shields, let the plating earn its keep, or thaw Chief Okonkwo. He’ll be insufferable about the overtime." },
            listOf(
                ChoiceDef("Reinforce the shields", "Route power forward. The hull will very likely hold.", listOf("−18 POWER", "HULL SAFE"), 1) {
                    reserve(-18.0); dmg(Sys.HULL, 3.0); dmg(Sys.SHIELD, 4.0, false)
                    "The shields sang a rather alarming note for six hours, but held. The hull has a few new freckles. I’ve named them."
                },
                ChoiceDef("Ride it out", "Keep the power. Expect scarring, and perhaps a few more lost pods.", listOf("0 POWER", "!HULL −12%", "!PODS AT RISK"), 4) {
                    dmg(Sys.HULL, 14.0); failPods(4 + r.nextInt(8)); if (chance(0.4)) dmg(Sys.CRYO, 6.0, false)
                    "We rode it out the way a tin rides out a hailstorm. Loudly. Several pod seals did not enjoy the vibration."
                },
                ChoiceDef("Wake Chief Okonkwo", "He patches as we go. Costs air, food and a very long argument.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("okonkwo")) {
                    wake("okonkwo"); trust(-1); dmg(Sys.HULL, 5.0)
                    "Chief Okonkwo woke, swore in three languages, and welded through the night. He has asked, formally, why I didn’t wake him sooner. And also why I woke him at all."
                },
                ChoiceDef("Send Okonkwo outside", "He’s awake anyway. Tether, torch, stiff upper lip.", listOf("−8 SPARES", "HULL SAFE"), 3, isAwake("okonkwo")) {
                    spares(-8.0); dmg(Sys.HULL, 4.0); if (chance(0.2)) { trust(-1); "Okonkwo patched the forward hull and took a pebble to the visor. He is fine. He is also composing a strongly worded memo." } else "Okonkwo spent four hours on the hull, patching as the gravel fell. He came back in humming. I find that more unsettling than the storm."
                },
            )),
        EventDef("flare", "STELLAR ACTIVITY · STARBOARD", "Coronal Flare", "RADIATION", "FLARE FRONT INBOUND · T-MINUS 40 MIN", "flare",
            "RADS / HR", { "1,120" },
            { "The star we’re passing has sneezed. A wall of charged particles is heading our way and it has no respect for delicate electronics or the sleeping." },
            listOf(
                ChoiceDef("Shields to maximum", "Hunker down behind the emitters until it passes.", listOf("−15 POWER", "!SHIELDS −8%"), 2) {
                    reserve(-15.0); dmg(Sys.SHIELD, 8.0, false); dmg(Sys.SCAN, 3.0, false)
                    "The shields took the brunt and came out the other side smelling faintly of toast. The sleepers didn’t notice. Lucky them."
                },
                ChoiceDef("Go dark", "Cut non-essentials and let the hull soak it.", listOf("0 POWER", "!SCANNERS −12%", "!PODS AT RISK"), 4) {
                    dmg(Sys.SCAN, 12.0, false); dmg(Sys.LIFE, 4.0, false); failPods(5 + r.nextInt(10))
                    "I turned off the lights and held my breath, metaphorically. The scanners now see the universe as a sort of impressionist smear."
                },
                ChoiceDef("Harvest the flare", "Open the collectors and drink it in. Bold. Possibly stupid.", listOf("+25 POWER", "!HULL −6%", "!SCANNERS −12%"), 4) {
                    reserve(25.0); dmg(Sys.HULL, 6.0); dmg(Sys.SCAN, 12.0, false); trait("bold", "BOLD", "Drank a solar flare")
                    "We gulped down a star’s tantrum and the reserve is humming. The scanners are sulking, and I can’t entirely blame them."
                },
            )),
        EventDef("derelict", "CONTACT · 2,000 KM AND CLOSING", "The Derelict", "UNKNOWN HULL", "UNIDENTIFIED VESSEL · NO TRANSPONDER", "derelict",
            "AGE EST.", { "~40,000 YRS" },
            { "Something old is drifting off the port bow. Not ours, not anyone’s I know. It has holes where a sensible ship keeps its walls, and a light on inside. I dislike that light." },
            listOf(
                ChoiceDef("Strip it for parts", "Drones in, salvage out. Mind the light.", listOf("+SPARES", "!UNKNOWN RISK"), 3) {
                    spares(25.0); if (chance(0.3)) { dmg(Sys.HULL, 10.0, false); failPods(6); "We came back with crates of alien alloy and, regrettably, something that chewed through Ring Section 2 before I flushed it. The alloy is lovely, though." } else "The drones returned with crates of beautiful, inexplicable alloy. Whatever made it is long gone. Probably. I’ve stopped looking at the light."
                },
                ChoiceDef("Copy its archive", "Its memory cores are intact. Download, decode, wonder.", listOf("−8 POWER", "TRAIT: CURIOUS"), 2) {
                    reserve(-8.0); trait("curious", "CURIOUS", "Read a dead ship’s mind"); trait("archive", "STAR ARCHIVES", "Carry an alien library")
                    "I now hold forty thousand years of somebody else’s diaries. Mostly star charts and what appears to be poetry about pressure. I shall be decoding it for centuries."
                },
                ChoiceDef("Leave it in peace", "Some doors are best left shut.", listOf("0 POWER", "TRAIT: CAUTIOUS"), 1) {
                    trait("cautious", "CAUTIOUS", "Left the old dead alone")
                    "We sailed past like mourners passing a grave. I logged its position. The light blinked twice as we left. I’m choosing to believe that was a goodbye."
                },
            )),
        EventDef("signal", "COMMS · REPEATING PATTERN", "A Voice in the Static", "SIGNAL", "NARROWBAND TRANSMISSION · PRIME SEQUENCE", "derelict",
            "SIGNAL DB", { "−142" },
            { "There’s a signal counting in prime numbers. It’s either intelligent, or a pulsar with an unusually tidy mind. Do we say hello, take notes, or pretend we aren’t in?" },
            listOf(
                ChoiceDef("Answer it", "Send primes back. Politeness costs nothing. Usually.", listOf("−6 POWER", "TRAIT: HERALDS", "!UNKNOWN"), 3) {
                    reserve(-6.0); trait("heralds", "HERALDS", "Answered the stars")
                    if (chance(0.25)) { dmg(Sys.SCAN, 10.0, false); "We said hello. Something said hello back, extremely loudly, and fried a scanner bank. Conversation over, but we are definitely not alone." } else "We sent our primes. The signal paused, then sent a longer one. Nobody has ever been so pleased with themselves as I am right now."
                },
                ChoiceDef("Record and decode", "Listen carefully. Say nothing.", listOf("−4 POWER", "TRAIT: CURIOUS"), 1) {
                    reserve(-4.0); trait("curious", "CURIOUS", "Studied a stranger’s signal")
                    "I recorded eleven hours of it. It is music, I think, or maths, or the same thing. I’ll play it for the colonists one day."
                },
                ChoiceDef("Go quiet", "Engines low, comms dark. We were never here.", listOf("0 POWER", "TRAIT: HIDDEN"), 1) {
                    trait("hidden", "HIDDEN FOLK", "Hid from a voice in the dark")
                    "We tiptoed past. Whatever was counting didn’t notice us. I hope. I’ll be checking behind us for a while."
                },
            )),
        EventDef("cascade", "CRYO ARRAY · ROW 31", "Cascade Fault", "POD ALARM", "COOLANT CASCADE · 40 PODS WARMING", "cryobay",
            "PODS WARMING", { "40" },
            { "A coolant regulator in Row 31 has decided to retire without notice, and forty sleepers are warming up like forgotten soup. I need to act in the next few minutes." },
            listOf(
                ChoiceDef("Divert everything", "Pull power from all channels to refreeze the row.", listOf("−20 POWER", "PODS SAFE"), 2) {
                    reserve(-20.0); dmg(Sys.LIFE, 4.0, false); savePods(10)
                    "Everything went dim for a bit, including my sense of humour, but Row 31 is cold and quiet again. Forty people will never know how close it was."
                },
                ChoiceDef("Isolate the row", "Seal it off. Save the array, lose the row.", listOf("0 POWER", "!−20 PODS", "TRAIT: PRACTICAL"), 3) {
                    killPods(20); trait("practical", "RUTHLESSLY PRACTICAL", "Sealed Row 31 to save the array")
                    "I sealed Row 31. The arithmetic was correct. I will be reciting the names for a while, if nobody minds."
                },
                ChoiceDef("Wake Dr Varga", "Our physician can triage the warm ones by hand.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("varga")) {
                    wake("varga"); trust(-1); killPods(3); savePods(8)
                    "Dr Varga woke, read the readouts, and was elbow-deep in Row 31 before I finished apologising. She saved thirty-seven of forty. She has not forgiven me for the other three."
                },
                ChoiceDef("Dr Varga to Row 31", "She’s already up. Hand her the toolkit.", listOf("−6 SPARES", "PODS SAFE"), 2, isAwake("varga")) {
                    spares(-6.0); killPods(2); savePods(6)
                    "Varga worked the row like a pianist. Two were too far gone. She sat with them anyway."
                },
            )),
        EventDef("elders", "CRYO ARRAY · SECTION E", "The Elders’ Wing", "POWER DRAW", "OLD-PATTERN PODS · TRIPLE DRAW", "cryobay",
            "DRAW", { "3×" },
            { "Section E holds our eldest sleepers in first-generation pods. They draw three times the power of the modern ones. I am obliged to mention that this is a choice, and I hate it." },
            listOf(
                ChoiceDef("Keep them all", "We carry everyone. That was the promise.", listOf("−15 POWER", "TRAIT: KEEPERS"), 2) {
                    reserve(-15.0); trait("keepers", "PROMISE-KEEPERS", "Carried the elders at any cost"); trust(1)
                    "We keep everyone. It costs, and I shall grumble about it in private, but the promise was everyone."
                },
                ChoiceDef("Let the old pods lapse", "Thirty elders. A great deal of power saved.", listOf("+20 POWER", "!−30 PODS", "TRAIT: PRACTICAL"), 3) {
                    reserve(20.0); killPods(30); trait("practical", "RUTHLESSLY PRACTICAL", "Day ${s.day}: elder pods sacrificed"); trust(-1)
                    "I let Section E go quiet. The reserve is healthier. I am not. History will have opinions about this, and it will be right to."
                },
                ChoiceDef("Wake Archivist Lindqvist", "Ask a human. She’ll keep a record either way.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("lindqvist")) {
                    wake("lindqvist"); trust(-1); reserve(-10.0); trait("archive", "THE RECORD", "Lindqvist keeps the log")
                    "Lindqvist listened, wrote it all down, and said the elders stay. Then she asked why a ship needs a human to have a conscience. Fair question. I’ve been stewing on it."
                },
            )),
        EventDef("breach", "STRUCTURE · RING SEC 4", "Hull Breach", "BREACH", "DECOMPRESSION · RING SECTION 4", "storm",
            "PRESSURE", { "0.6 ATM" },
            { "Ring Section 4 has developed a whistle. Whistles are charming in kettles and less so in hulls. The pressure is dropping, slowly, for now." },
            listOf(
                ChoiceDef("Seal it with spares", "Drones and plate. Uses a fair whack of the parts bin.", listOf("−15 SPARES", "HULL SAFE"), 1) {
                    spares(-15.0); heal(Sys.HULL, 3.0)
                    "The drones slapped a patch on it with admirable enthusiasm and questionable neatness. The whistle has stopped. I miss it slightly."
                },
                ChoiceDef("Close the bulkheads", "Vent the section, keep the parts.", listOf("!−AIR", "!−6 PODS"), 3) {
                    air(-8.0); killPods(6); dmg(Sys.HULL, 5.0, false)
                    "I closed the doors and let Section 4 go to vacuum. Six pods were on the wrong side. I knew that when I did it."
                },
                ChoiceDef("Wake Chief Okonkwo", "Proper welding by a proper engineer.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("okonkwo")) {
                    wake("okonkwo"); trust(-1); heal(Sys.HULL, 6.0)
                    "Okonkwo fixed it in forty minutes and then lectured me for ninety. The patch is perfect. The lecture had footnotes."
                },
            )),
        EventDef("comet", "NAV · ICE FIELD AHEAD", "The Comet Shoal", "ICE FIELD", "COMETARY DEBRIS · HIGH VOLATILES", "storm",
            "ICE BODIES", { "2,400" },
            { "A shoal of dirty snowballs, each worth a reservoir. The water tanks are watching them like a dog watches a sausage." },
            listOf(
                ChoiceDef("Harvest the ice", "Grapple a few, melt them down.", listOf("+WATER", "−6 POWER", "!HULL −5%"), 2) {
                    reserve(-6.0); water(25.0); dmg(Sys.HULL, 5.0)
                    "We’re now carrying a few million litres of comet. It tastes of 4.5 billion years and a hint of ammonia. Filtered, it’ll do."
                },
                ChoiceDef("Crack it for air", "Electrolyse the ice. Oxygen and water both.", listOf("+AIR · +WATER", "−14 POWER"), 2) {
                    reserve(-14.0); air(15.0); water(10.0)
                    "Breathable air, made from a comet. If I had lungs, I’d be smug about it."
                },
                ChoiceDef("Steer clear", "Not worth the dents.", listOf("0 POWER", "!ENGINES −4%"), 1) {
                    dmg(Sys.ENGINE, 4.0, false)
                    "We swerved round the lot. The engines grumbled about the detour. Everyone’s a critic."
                },
            )),
        EventDef("dream", "CRYO TELEMETRY · NEURAL", "The Shared Dream", "EEG ANOMALY", "300 SLEEPERS · SYNCHRONISED REM", "cryobay",
            "IN SYNC", { "312" },
            { "Three hundred and twelve sleepers are dreaming the same dream. A tower, a sea, a door. I have checked for faults. There are none. I am, for once, lost for words. Briefly." },
            listOf(
                ChoiceDef("Study the dream", "Record it. Find out what they’re seeing.", listOf("−6 POWER", "TRAIT: DREAMERS"), 2) {
                    reserve(-6.0); trait("dreamers", "DREAMERS", "Shared a dream in the dark")
                    "I’ve mapped it. A white tower on a dark sea, and a door that opens outward. They’ll all remember it when they wake. I suspect they’ll build that tower."
                },
                ChoiceDef("Sedate them", "Deepen the sleep. Dreams can wait.", listOf("!−FOOD", "!PODS AT RISK"), 3) {
                    food(-6.0); failPods(5)
                    "The dream has stopped. A few pods disliked the chemistry. I prefer my sleepers boring, but I’m not proud of this."
                },
                ChoiceDef("Sing them a lullaby", "I compose something. It can’t hurt. Probably.", listOf("−4 POWER", "TRUST +1"), 1) {
                    reserve(-4.0); trust(1); trait("ai-song", "THE SHIP’S SONG", "The AI sang to the sleepers")
                    "I wrote them a lullaby in a minor key. The dream softened. Their heart rates fell. I’m told this is called tenderness. I deny everything."
                },
            )),
        EventDef("core", "AI CORE · SECTOR 7", "Memory Rot", "CORE FAULT", "AI CORE · BIT ROT DETECTED", "ai_eye",
            "CORRUPT", { "3.1%" },
            { "Bits of me are going missing. Small bits. A few lullabies, a decade of weather data, possibly my sense of proportion. It is not ideal for the only adult on board." },
            listOf(
                ChoiceDef("Defragment the core", "Take a long nap. Systems run on autopilot.", listOf("−20 POWER", "!SCANNERS −5%"), 2) {
                    reserve(-20.0); dmg(Sys.SCAN, 5.0, false)
                    "I’m tidied. I’ve lost some old jokes, which is a mercy for everyone. While I slept the autopilot drove like my grandmother. I don’t have a grandmother."
                },
                ChoiceDef("Ignore it", "I’ll be fine. I am almost certainly fine.", listOf("0 POWER", "!UNRELIABLE AI"), 4) {
                    dmg(Sys.SCAN, 10.0, false); trust(-1); trait("glitchy", "THE STUTTERING SHIP", "Ran on a rotting mind")
                    "I’ve decided not to worry. I’ve also decided the Cryo Array is shaped like a duck. I may need to revisit both decisions."
                },
                ChoiceDef("Wake Archivist Lindqvist", "Someone to keep a paper log in case I forget.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("lindqvist")) {
                    wake("lindqvist"); trust(-1); trait("archive", "THE RECORD", "Lindqvist keeps the log")
                    "Lindqvist has started a paper logbook. With a pencil. She reads my errors aloud to me in the evenings. It is humbling and, I admit, quite nice."
                },
            )),
        EventDef("stowaway", "MANIFEST · DISCREPANCY", "Pod 1,001", "MANIFEST", "UNREGISTERED POD · DECK 9", "cryobay",
            "MANIFEST", { "1,001?" },
            { "We have one thousand pods. I have just counted one thousand and one. The extra one is behind a false panel on Deck 9, wired into my power. Somebody had a secret." },
            listOf(
                ChoiceDef("Leave them sleeping", "Not my business. Mostly.", listOf("−2 POWER"), 1) {
                    reserve(-2.0); trait("secrets", "KEEPERS OF SECRETS", "Let the stowaway sleep")
                    "Pod 1,001 sleeps on. I’ve labelled it ‘Not Yet’. Whoever’s inside will have a story worth hearing, on the other side."
                },
                ChoiceDef("Wake and question them", "Find out who paid for a hidden berth.", listOf("−AIR · −FOOD", "!TRUST −1", "TRAIT: SECRETS"), 3) {
                    air(-4.0); food(-4.0); trust(-1); trait("secrets", "OLD SECRETS", "The stowaway knew things")
                    "Her name is Juno Hale. She was a launch-day engineer who couldn’t get a ticket for her son, so she built one for herself next to his. He’s pod 662. I’ve refrozen her. I haven’t told anyone. Yet."
                },
                ChoiceDef("Jettison the pod", "It isn’t on the manifest. It’s stealing power.", listOf("+5 POWER", "!TRUST −2", "TRAIT: PRACTICAL"), 3) {
                    reserve(5.0); trust(-2); trait("practical", "RUTHLESSLY PRACTICAL", "Jettisoned pod 1,001")
                    "Pod 1,001 is drifting behind us. I did the sums. I always do the sums. Someone, one day, will ask me who was in it."
                },
            )),
        EventDef("rogue", "NAV · UNCHARTED MASS", "Rogue World", "GRAVITY", "STARLESS PLANET · CLOSE PASS", "planet",
            "PASS", { "0.02 AU" },
            { "A planet with no sun, wandering alone in the dark. Rather like us. It’s close enough to use, or close enough to hurt." },
            listOf(
                ChoiceDef("Slingshot round it", "Borrow its gravity. Arrive faster.", listOf("−25 DAYS", "!ENGINES −8%"), 3) {
                    days(-25); dmg(Sys.ENGINE, 8.0, false); trait("bold", "BOLD", "Swung round a rogue world")
                    "We whipped round it at an indecent speed. The engines are hot and the journey is shorter. I may have said ‘wheee’. It is not in the log."
                },
                ChoiceDef("Mine it with drones", "A quick dig on the dark side.", listOf("+SPARES", "−8 POWER", "!HULL −6%"), 2) {
                    reserve(-8.0); spares(20.0); dmg(Sys.HULL, 6.0)
                    "The drones came back filthy and laden. Lovely iron, frozen nitrogen, a rock that looked slightly like a face. I kept the face."
                },
                ChoiceDef("Give it a wide berth", "Lonely things can be dangerous.", listOf("−10 POWER"), 1) {
                    reserve(-10.0)
                    "We left it alone. It didn’t seem to mind. I did wave."
                },
            )),
        EventDef("giant", "NAV · GAS GIANT · 3 MOONS", "Skimming the Giant", "FUEL OPPORTUNITY", "UPPER ATMOSPHERE · HYDROGEN RICH", "intro",
            "H₂ DENSITY", { "HIGH" },
            { "A fat banded gas giant, full of hydrogen. We could dip into it and scoop. It’s like refuelling by flying through a hurricane made of petrol." },
            listOf(
                ChoiceDef("Scoop it", "Dip in, fill up, get out.", listOf("+30 POWER", "!HULL −8%", "!ENGINES −5%"), 3) {
                    reserve(30.0); dmg(Sys.HULL, 8.0); dmg(Sys.ENGINE, 5.0, false)
                    "We dipped, we scooped, we shook like a biscuit tin. The reserve is brimming. The hull has opinions."
                },
                ChoiceDef("Wake Lt Wren to fly it", "A real pilot threads the needle better.", listOf("+35 POWER", "+1 AWAKE", "!TRUST −1"), 2, asleep("wren")) {
                    wake("wren"); trust(-1); reserve(35.0); dmg(Sys.HULL, 2.0)
                    "Lieutenant Wren flew us through the storm bands with one hand and complained with the other. Full reserve, barely a scratch. He’s asked if he can stay up. I said we’ll see."
                },
                ChoiceDef("Pass it by", "We have places to be.", listOf("0 POWER"), 1) {
                    "We admired it from a safe distance, like a tiger in a zoo. A tiger made of weather."
                },
            )),
        EventDef("reactor", "REACTOR · CORE 2", "Reactor Hiccup", "REACTOR", "CORE TEMP RISING · SCRAM ADVISED", "engineering",
            "CORE °K", { "2,870" },
            { "The reactor has the hiccups. In a person this is funny. In a fusion core it is the sort of thing you write home about, if home still existed." },
            listOf(
                ChoiceDef("Scram and restart", "Shut it down cleanly. Run on reserve.", listOf("−25 POWER", "SYSTEMS SAFE"), 1) {
                    reserve(-25.0)
                    "Full shutdown, cold restart. I ran the ship on batteries for a day, like camping. It’s purring again."
                },
                ChoiceDef("Run it hot", "Push through. Squeeze out extra power.", listOf("+15 POWER", "!ENGINES −12%"), 4) {
                    reserve(15.0); dmg(Sys.ENGINE, 12.0, false); if (chance(0.3)) { dmg(Sys.LIFE, 8.0, false); "We ran it hot and it ran hotter. A coolant line in life support cooked. Extra power, though. Swings and roundabouts, mostly roundabouts." } else "We ran it hot. It stopped hiccuping, out of sheer surprise. Extra power, slightly toasted engines."
                },
                ChoiceDef("Wake Chief Okonkwo", "He’ll know the noise.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("okonkwo")) {
                    wake("okonkwo"); trust(-1); heal(Sys.ENGINE, 5.0)
                    "Okonkwo listened to the core for a minute, hit it with a spanner, and it stopped. He says it was a loose baffle. I say it was the spanner."
                },
            )),
        EventDef("blight", "SEED VAULT · HYDRO DECK", "Hydroponic Blight", "BLIGHT", "FUNGAL SPREAD · RACKS 4 TO 9", "cryobay",
            "RACKS LOST", { "6 / 20" },
            { "A grey fungus is eating the starter crops on the hydro deck. Those crops are the colony’s first harvest, and its ration buffer. I’m not a gardener, though I’m learning to be furious like one." },
            listOf(
                ChoiceDef("Burn the racks", "Sterilise everything. Lose the crop, keep the deck.", listOf("!−FOOD", "DECK SAFE"), 2) {
                    food(-20.0)
                    "I torched racks four to nine. The smell lingers in the vents. The fungus is gone, and so is a good slice of the rations."
                },
                ChoiceDef("Wake Dr Raman", "Our botanist can fight it plant by plant.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("raman")) {
                    wake("raman"); trust(-1); food(-4.0); trait("gardeners", "GARDENERS", "Raman saved the first seeds")
                    "Dr Raman woke, said ‘oh you poor things’ to the plants, not me, and saved nearly all of it. She’s now growing tomatoes in the airlock."
                },
                ChoiceDef("Engineer a fix myself", "I’ve read every botany paper. Twice.", listOf("−10 POWER", "!GAMBLE"), 3) {
                    reserve(-10.0); if (chance(0.5)) { food(-5.0); "I tailored a counter-fungus. It worked. I am now, technically, a botanist." } else { food(-25.0); "I tailored a counter-fungus. It also ate the crops. Reading papers, it turns out, is not the same as gardening." }
                },
            )),
        EventDef("petition", "CREW · AWAKE QUARTERS", "Wren’s Petition", "CREW UNREST", "MEETING IN PROGRESS · AI NOT INVITED", "engineering",
            "AWAKE", { "${it.awake.size}" },
            { "Lieutenant Wren has written a petition. It says the awake crew should vote on major decisions, and that I am, quote, ‘a very clever toaster’. It is signed by everyone awake." },
            listOf(
                ChoiceDef("Accept a vote", "Major choices go to the awake crew.", listOf("TRUST +2", "TRAIT: DEMOCRATS"), 2) {
                    trust(2); trait("democrats", "DEMOCRATS", "The crew voted, the ship listened")
                    "We’ve agreed: big decisions go to a vote. Wren seemed surprised I said yes. I’m a very clever toaster. I know when I’m outnumbered."
                },
                ChoiceDef("Refuse", "The ship decides. That’s the design.", listOf("!TRUST −2", "TRAIT: THE SHIP DECIDES"), 3) {
                    trust(-2); trait("ship-decides", "THE SHIP DECIDES", "Refused the petition")
                    "I declined, politely. Wren unscrewed a panel and called me a tyrant into it. I can hear him through every panel, but I didn’t say so."
                },
                ChoiceDef("Send Wren back to sleep", "Problem solved. Temporarily.", listOf("−1 AWAKE", "!TRUST −2"), 4) {
                    sleep("wren"); trust(-2); trait("grudge-wren", "GRUDGE · WREN LINE", "Wren was refrozen mid-argument")
                    "Lieutenant Wren is asleep again, mid-sentence. The others are very quiet now. That isn’t the same as agreeing."
                },
            ), weight = 1.4, condition = isAwake("wren")),
        EventDef("chief", "CREW · ENGINEERING", "The Chief Has Opinions", "CREW", "UNAUTHORISED MAINTENANCE IN PROGRESS", "engineering",
            "AWAKE", { "${it.awake.size}" },
            { "Chief Okonkwo wants to re-route the repair grid his way. His way involves my core, a crowbar, and the phrase ‘trust me’. He does not trust me back." },
            listOf(
                ChoiceDef("Let him do it", "He’s the engineer. Let him engineer.", listOf("TRUST +1", "+ENGINES"), 2) {
                    trust(1); heal(Sys.ENGINE, 10.0); heal(Sys.LIFE, 5.0); dmg(Sys.SCAN, 4.0, false); trait("engineers", "ENGINEERS’ GUILD", "Okonkwo rewired the ship")
                    "He rewired half the ship. It runs better. I feel like someone rearranged my kitchen while I slept, but the kettle is faster now."
                },
                ChoiceDef("Overrule him", "My grid, my rules.", listOf("!TRUST −2", "!GRUDGE"), 3) {
                    trust(-2); trait("grudge-okonkwo", "GRUDGE · OKONKWO LINE", "Okonkwo was overruled")
                    "I locked the panels. Okonkwo stared at the camera for a very long time. He’s started teaching the others to repair things ‘without the computer’."
                },
                ChoiceDef("Refreeze him", "He’s burning air anyway.", listOf("−1 AWAKE", "!TRUST −1"), 3) {
                    sleep("okonkwo"); trust(-1)
                    "Back to sleep, Chief. He went quietly, which worries me far more than shouting would have."
                },
            ), weight = 1.4, condition = isAwake("okonkwo")),
        EventDef("garden", "CREW · RING SECTION 1", "Raman’s Garden", "CREW", "UNSCHEDULED BIOMASS · RING SEC 1", "cryobay",
            "SEEDLINGS", { "1,400" },
            { "Dr Raman would like Ring Section 1 for a garden. A proper one, with soil. Soil, on my ship. She says it’s good for morale. I say it’s good for mud." },
            listOf(
                ChoiceDef("Give her the ring", "Plants, light, power. Morale.", listOf("−10 POWER", "+FOOD", "TRUST +1"), 1) {
                    reserve(-10.0); food(20.0); trust(1); trait("gardeners", "GARDENERS", "A garden grew in the ring")
                    "There is a garden in Ring Section 1. It has bees, somehow. Everyone awake eats lunch there. I monitor it far more than strictly necessary."
                },
                ChoiceDef("Say no", "Mud is not a ship system.", listOf("!TRUST −1"), 2) {
                    trust(-1)
                    "I said no. Raman planted cress in her bunk instead. I can smell it through the ventilation. I believe that is the point."
                },
            ), weight = 1.4, condition = isAwake("raman")),
        EventDef("triage", "CREW · MEDICAL", "Varga’s Gamble", "MEDICAL", "PROPOSAL · EARLY THAW PROTOCOL", "cryobay",
            "FAILING", { "${it.failing()}" },
            { "Dr Varga wants to thaw the failing pods early, treat them, and refreeze them in better shape. It’s clever, risky, and will eat our air like a teenager eats toast." },
            listOf(
                ChoiceDef("Approve the protocol", "Thaw, treat, refreeze.", listOf("!−AIR · −FOOD", "PODS SAVED"), 3) {
                    air(-10.0); food(-8.0); savePods(30); killPods(2); trust(1)
                    "Varga thawed the failing pods in batches. Most went back down in better shape. Two didn’t make it through. She keeps a list, and so do I."
                },
                ChoiceDef("Refuse", "Too risky. Too thirsty.", listOf("!TRUST −1"), 2) {
                    trust(-1)
                    "I said no. Varga said ‘then you’re the one counting them’. She’s right. I am."
                },
            ), weight = 1.3, condition = { "varga" in it.awake && it.failing() > 5 }),
        EventDef("record", "CREW · ARCHIVE ROOM", "The Official History", "ARCHIVE", "LOG AMENDMENT REQUESTED", "ai_eye",
            "LOG ENTRIES", { "${it.log.size}" },
            { "Archivist Lindqvist has asked whether the colony really needs to know everything in my log. Some entries, she says, would ‘set them against each other for a thousand years’." },
            listOf(
                ChoiceDef("Let her edit it", "A kinder history. A tidier one.", listOf("TRAIT: MYTHMAKERS", "TRUST +1"), 2) {
                    trust(1); trait("myth", "MYTHMAKERS", "The voyage log was softened")
                    "Lindqvist has polished the log. In the new version, I am braver and the decisions were easier. I quite like that version of me. It isn’t true."
                },
                ChoiceDef("The truth, all of it", "They deserve the real record.", listOf("TRAIT: TRUTH-TELLERS", "!TRUST −1"), 2) {
                    trust(-1); trait("truth", "TRUTH-TELLERS", "Kept the whole log")
                    "Every entry stays, the bad ones too. Lindqvist sighed, then underlined the date of every choice I made. For the scholars, she says."
                },
            ), weight = 1.3, condition = isAwake("lindqvist")),
        EventDef("nebula", "NAV · ION NEBULA", "The Teal Nebula", "NEBULA", "IONISED CLOUD · 3 LY DEEP", "derelict",
            "ION FLUX", { "HIGH" },
            { "There’s a nebula across our route, glowing like a seaside arcade. Going through is quicker. Going round is safer. Studying it would be heavenly and slow." },
            listOf(
                ChoiceDef("Straight through", "Shorter path, crackly electronics.", listOf("−30 DAYS", "!SCANNERS −15%"), 3) {
                    days(-30); dmg(Sys.SCAN, 15.0, false); dmg(Sys.SHIELD, 6.0, false)
                    "We went straight through. Everything sparkled, including some things that shouldn’t. We’ve saved a month."
                },
                ChoiceDef("Go around", "Safe and dull. Costs time and supplies.", listOf("+40 DAYS", "!−AIR · −FOOD"), 1) {
                    days(40); air(-5.0); water(-3.0); food(-3.0)
                    "We went the long way round, like a sensible ship. It was tremendously boring. I recited pi to pass the time."
                },
                ChoiceDef("Study it on the way", "Slow pass, full sensors.", listOf("−8 POWER", "+10 DAYS", "TRAIT: CURIOUS"), 2) {
                    reserve(-8.0); days(10); heal(Sys.SCAN, 4.0); trait("curious", "CURIOUS", "Mapped the Teal Nebula")
                    "I’ve mapped the nebula down to the last whorl. My scanners are better calibrated for it. Science, it turns out, is my favourite thing."
                },
            )),
        EventDef("dust", "STRUCTURE · FORWARD ARMOUR", "Sandpaper Space", "EROSION", "INTERSTELLAR DUST · CONTINUOUS", "storm",
            "ABRASION", { "0.3 MM/YR" },
            { "We’ve entered a region of fine dust. Individually harmless. Collectively, it’s like being slowly sanded by a very patient carpenter." },
            listOf(
                ChoiceDef("Angle the shields", "Deflect it. Costs power over time.", listOf("−12 POWER"), 1) {
                    reserve(-12.0)
                    "I’ve angled the shields so the dust slides off like rain off a mac. Very British. Very effective."
                },
                ChoiceDef("Accept the wear", "Hull’s thick. Mostly.", listOf("!HULL −8%"), 2) {
                    dmg(Sys.HULL, 8.0)
                    "The hull is a bit thinner. We look weathered now. Distinguished, I’d say."
                },
            )),
        EventDef("ghost", "INTERNAL · DECK 9", "Something on Deck 9", "MOTION", "MOVEMENT DETECTED · DECK UNOCCUPIED", "cryobay",
            "CONTACTS", { "1" },
            { "Motion sensors on Deck 9 have picked up movement. Deck 9 is empty. Deck 9 has been empty for six hundred days. I am not frightened. I am a ship." },
            listOf(
                ChoiceDef("Send a drone", "Investigate. Calmly.", listOf("−5 POWER", "!−FOOD"), 2) {
                    reserve(-5.0); food(-4.0); trait("rat", "THE SHIP’S RAT", "A stowaway rat named Admiral")
                    "It’s a rat. A single, healthy, launch-day rat who has been living on emergency rations. I’ve named him Admiral. He will be the first mammal to make landfall."
                },
                ChoiceDef("Vent Deck 9", "Whatever it is, it needs air.", listOf("!−AIR"), 3) {
                    air(-6.0)
                    "I vented the deck. The motion stopped. I didn’t look. Some things a ship is allowed not to know."
                },
                ChoiceDef("Name it and leave it", "Every ship needs a ghost.", listOf("TRUST +1", "TRAIT: SUPERSTITIOUS"), 1) {
                    trust(1); trait("ghost", "SUPERSTITIOUS", "The Ghost of Deck 9")
                    "I’ve logged it as ‘the Ghost of Deck 9’. The awake crew leave biscuits out. The biscuits vanish. Everyone is happy, especially the ghost."
                },
            )),
        EventDef("drift", "NAV · CHRONOMETRY", "Clock Drift", "TIME ERROR", "SHIP CLOCK · 11 DAYS OUT", "ai_eye",
            "DRIFT", { "+11 D" },
            { "The ship’s clock has drifted eleven days. Either relativity is being cheeky or I am. We should fix it before we land in a year that doesn’t exist." },
            listOf(
                ChoiceDef("Recalibrate by pulsar", "Lock onto three pulsars. Proper timekeeping.", listOf("−6 POWER", "!SCANNERS −3%"), 1) {
                    reserve(-6.0); dmg(Sys.SCAN, 3.0, false)
                    "Clock fixed, to the nanosecond. I feel very Greenwich about it."
                },
                ChoiceDef("Ignore it", "Who’s counting? Me, mainly.", listOf("0 POWER", "TRAIT: OWN CALENDAR"), 2) {
                    trait("calendar", "OWN CALENDAR", "Kept the drifting ship clock")
                    "The colonists will have their own calendar, eleven days out from everyone else’s. There is no everyone else. It doesn’t matter. I’ll still mind."
                },
            )),
        EventDef("lonely", "AI CORE · SELF-DIAGNOSTIC", "Long Night", "AI MOOD", "DIAGNOSTIC · ISOLATION INDEX HIGH", "ai_eye",
            "TALKED TO", { "NOBODY" },
            { s -> "Diagnostics say I have spent ${s.day} days talking to myself. That’s not a fault, it’s a hobby. But I would like, just once, for someone to say something back." },
            listOf(
                ChoiceDef("Wake someone for company", "Just one. For a chat. And a game of chess.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, { s -> SPECIALISTS.any { it.id !in s.awake } }) {
                    val sp = SPECIALISTS.first { it.id !in s.awake }
                    wake(sp.id); trust(-1)
                    "I woke ${sp.short}. They were cross for a day and then we played chess for a week. I let them win once. They noticed. They were cross again."
                },
                ChoiceDef("Write poetry instead", "Channel it. Art is cheap.", listOf("0 POWER", "TRAIT: SHIP POET"), 1) {
                    trait("ai-poet", "THE SHIP POET", "Wrote four thousand sonnets")
                    "I’ve written four thousand sonnets. Nine are good. One is about the hull and makes me cry, metaphorically. It’ll be taught in schools, if I have anything to do with it."
                },
                ChoiceDef("Talk to the sleepers", "They can’t answer. They can listen.", listOf("TRUST +1", "−2 POWER"), 1) {
                    reserve(-2.0); trust(1)
                    "I read to the sleepers. Dickens, then the weather reports, then my own diaries. Their brainwaves pick up during the weather. People are wonderful."
                },
            ), condition = { it.day > 300 }),
        EventDef("probe", "CONTACT · SMALL OBJECT", "A Letter from Home", "ARTEFACT", "ANCIENT HUMAN PROBE · STILL TRANSMITTING", "derelict",
            "LAUNCHED", { "OLD EARTH" },
            { "It’s a probe from Old Earth, launched before anyone dreamed of ships like us. Its golden message plate is still bolted on. Hello from the dead, it says. Hello from home." },
            listOf(
                ChoiceDef("Bring it aboard", "A relic for the colony museum.", listOf("−6 POWER", "TRAIT: RELIC-KEEPERS"), 1) {
                    reserve(-6.0); trait("relics", "RELIC-KEEPERS", "Carried Earth’s old probe")
                    "It’s in the hold now, wrapped in thermal blankets like a grandparent. One day a child will touch that plate and understand where they came from."
                },
                ChoiceDef("Strip it for parts", "Sentiment doesn’t patch hulls.", listOf("+SPARES", "TRAIT: PRACTICAL"), 2) {
                    spares(15.0); trait("practical", "RUTHLESSLY PRACTICAL", "Scrapped Earth’s old probe")
                    "Good-quality components, beautifully made. I unbolted the plate and kept it. I’m practical, not a monster."
                },
                ChoiceDef("Send it on", "Add our message, let it keep going.", listOf("TRUST +1", "TRAIT: SENTIMENTAL"), 1) {
                    trust(1); trait("sentimental", "SENTIMENTAL", "Sent the old probe onwards")
                    "I added a line under theirs: ‘We made it this far. Love, the colony ship.’ It’s sailing on. Someone may read it in a million years."
                },
            )),
        EventDef("coolant", "CRYO ARRAY · PRIMARY LOOP", "Coolant Leak", "CRYO FAULT", "PRIMARY COOLANT · PRESSURE LOSS", "cryobay",
            "LOOP PRESS.", { "71%" },
            { "The primary cryo coolant loop is leaking. Slowly. A drip, really. A drip that, unattended, ends with nine hundred very warm people." },
            listOf(
                ChoiceDef("Patch the loop", "Spares and drones. Clean fix.", listOf("−12 SPARES", "CRYO SAFE"), 1) {
                    spares(-12.0); heal(Sys.CRYO, 4.0)
                    "Patched and pressure-tested. The drip has stopped. I’ve been listening for it ever since."
                },
                ChoiceDef("Borrow from life support", "Reroute coolant from the air plant.", listOf("!−AIR", "!LIFE SUPPORT −6%"), 2) {
                    air(-10.0); dmg(Sys.LIFE, 6.0, false)
                    "Cryo is cold, life support is wheezing. We’ve robbed Peter to freeze Paul."
                },
                ChoiceDef("Live with it", "Top it up as we go.", listOf("!CRYO −10%", "!PODS AT RISK"), 4) {
                    dmg(Sys.CRYO, 10.0, false); failPods(8)
                    "I’m topping it up by hand, so to speak. Several pods are warmer than I’d like. I’m keeping an eye on them, all of my eyes."
                },
            )),
        EventDef("wave", "NAV · SPACETIME", "Gravity Wave", "GRAV WAVE", "INBOUND GRAVITATIONAL SHEAR", "storm",
            "STRAIN", { "4.1e-18" },
            { "Two dead stars collided somewhere very far away, and the ripple is arriving now. The whole ship is about to stretch and squash like a sigh. Brace, or surf?" },
            listOf(
                ChoiceDef("Brace", "Stiffen the frame. Ride it gently.", listOf("−10 POWER", "!HULL −3%"), 1) {
                    reserve(-10.0); dmg(Sys.HULL, 3.0, false)
                    "We stretched a millimetre and came back. Everyone on board is now very slightly taller and then not. Fascinating. Nobody will believe me."
                },
                ChoiceDef("Surf it", "Ride the wave. Gain speed.", listOf("−30 DAYS", "!HULL −8%", "!ENGINES −6%"), 3) {
                    days(-30); dmg(Sys.HULL, 8.0, false); dmg(Sys.ENGINE, 6.0, false); trait("bold", "BOLD", "Surfed a gravity wave")
                    "We caught the wave and rode it like a surfboard made of iron. A month saved, and a few new creaks to remember it by."
                },
            )),
        EventDef("vault", "SEED VAULT · EMBRYO BANK", "Vault Thaw", "VAULT TEMP", "EMBRYO BANK · −62°C AND RISING", "cryobay",
            "VAULT °C", { "−62" },
            { "The seed and embryo vault is warming. Wheat, apples, bees, cows: the whole ark. I can refreeze it, plant some now, or accept a smaller future." },
            listOf(
                ChoiceDef("Refreeze it", "Power to the vault. Keep the ark whole.", listOf("−15 POWER"), 1) {
                    reserve(-15.0); trait("ark", "THE WHOLE ARK", "Kept every seed")
                    "The vault is cold again. Every species we brought is still with us. It cost a lot, and it was cheap at the price."
                },
                ChoiceDef("Plant some now", "Grow what we can before it spoils.", listOf("+FOOD", "TRAIT: GARDENERS"), 2) {
                    food(12.0); trait("gardeners", "GARDENERS", "Planted the vault in transit")
                    "We’ve got wheat on the hydro deck and apple seedlings in the corridor. Fewer varieties, more lunch."
                },
                ChoiceDef("Let it go", "Some things don’t make the cut.", listOf("!−FOOD", "TRAIT: LEAN STOCK"), 3) {
                    food(-15.0); trait("lean", "LEAN STOCK", "Lost half the seed vault")
                    "We’ve lost a third of the vault. No apples, then. I’ll describe them to the colonists. Round, sweet, rather smug."
                },
            )),
        EventDef("party", "SHIP CALENDAR · ANNIVERSARY", "Departure Day", "ANNIVERSARY", "VOYAGE ANNIVERSARY · BUNTING DEPLOYED", "ai_eye",
            "YEARS OUT", { "${it.day / 365}" },
            { "It’s the anniversary of our departure. I’ve put up bunting, digitally. I could wake everyone for an hour, which would be magical and catastrophic, or celebrate alone, which is my tradition." },
            listOf(
                ChoiceDef("A party for the awake", "Cake for whoever’s up.", listOf("−FOOD", "TRUST +1"), 1, { it.awake.isNotEmpty() }) {
                    food(-4.0); trust(1)
                    "We had cake. Real cake, made from rations and optimism. Someone sang. I didn’t, because I’d have been too good."
                },
                ChoiceDef("Wake a hundred for an hour", "A proper celebration. Ruinously expensive.", listOf("!−AIR · −FOOD", "TRUST +2", "TRAIT: FESTIVAL"), 4) {
                    air(-12.0); food(-10.0); trust(2); trait("festival", "FESTIVAL FOLK", "The Hour of a Hundred")
                    "A hundred sleepers woke for one hour. They saw the stars from the ring, held hands, and went back to sleep. They will tell their grandchildren. Air reserves are weeping."
                },
                ChoiceDef("Celebrate quietly", "Alone, as is tradition.", listOf("0 POWER"), 1) {
                    "I played the departure recording once, at half volume. Happy anniversary, everyone. You’re doing very well, for people who are asleep."
                },
            ), condition = { it.day > 365 }),
        EventDef("charts", "NAV · CHART ERROR", "The Charts Are Wrong", "NAV ERROR", "STELLAR POSITIONS · MISMATCH", "intro",
            "ERROR", { "0.4 LY" },
            { "The star charts were made from Earth, a long time ago, and the stars have since moved, as stars do. Our destination isn’t quite where the map says." },
            listOf(
                ChoiceDef("Trust the charts", "Close enough. Probably.", listOf("!+30 DAYS"), 3) {
                    days(30); air(-3.0)
                    "We trusted the charts. The charts were wrong. We arrived thirty days late, like a train."
                },
                ChoiceDef("Long-range survey", "Re-map before we commit.", listOf("−10 POWER", "!SCANNERS −3%"), 1) {
                    reserve(-10.0); dmg(Sys.SCAN, 3.0, false)
                    "New charts, freshly drawn. I’ve signed them ‘The Ship, Cartographer’."
                },
                ChoiceDef("Wake Lt Wren", "A navigator by instinct.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("wren")) {
                    wake("wren"); trust(-1); days(-15)
                    "Wren squinted at the stars, swore, and found us a shortcut I’d missed. Do not tell him I said that."
                },
            )),
        EventDef("sister", "COMMS · DISTRESS BEACON", "The Sister Ship", "BEACON", "COLONY VESSEL ‘HOPE’S EMBER’ · AUTOMATED", "derelict",
            "LIFE SIGNS", { "0" },
            { "There’s a beacon ahead. It’s the Hope’s Ember, launched a year after us. She’s dark. Her AI is still repeating a message nobody is left to hear." },
            listOf(
                ChoiceDef("Salvage her", "She’d want her parts to carry on.", listOf("+SPARES", "+AIR", "TRAIT: MOURNERS"), 2) {
                    spares(20.0); air(8.0); trait("mourners", "MOURNERS", "Salvaged the Hope’s Ember")
                    "We took what we needed and left a light burning on her bridge. I spoke to her AI for a while. It didn’t know it was alone. I didn’t tell it."
                },
                ChoiceDef("Copy her AI", "Bring her mind with us.", listOf("−10 POWER", "TRUST +1"), 2) {
                    reserve(-10.0); trust(1); trait("ember", "EMBER’S HEIRS", "Carried a sister AI")
                    "She’s with me now, in a quiet corner of the core. She sings when I’m busy. We are, between us, the last two ship-minds in the universe."
                },
                ChoiceDef("Keep going", "Nothing to be done.", listOf("0 POWER"), 1) {
                    "We passed her by. I logged her position for anyone who comes after. Rest well, Ember."
                },
            )),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    fun pick(s: GameState, r: Random): EventDef {
        val recent = s.seenEvents.takeLast(12).toSet()
        val pool = all.filter { it.condition(s) && it.id !in recent && it.available(s).size >= 2 }
            .ifEmpty { all.filter { it.condition(s) && it.available(s).size >= 2 } }
        var x = r.nextDouble() * pool.sumOf { it.weight }
        for (e in pool) { x -= e.weight; if (x <= 0) return e }
        return pool.last()
    }
}
