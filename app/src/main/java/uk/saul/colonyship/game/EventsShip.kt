package uk.saul.colonyship.game

/** Things the ship does to itself: machinery ageing, cold failing, and the sleepers in between. */
internal object EventsShip {
    val list: List<EventDef> = listOf(
        EventDef("cascade", "CRYO ARRAY · ROW 31", "Cascade Fault", "POD ALARM", "CRYOGEN PUMP SEIZED · 40 PODS WARMING", "cryobay",
            "ROW TEMP", { "−118°C" },
            { "A cryogen pump in Row 31 has seized and forty pods are warming at two degrees an hour. Above about minus one hundred and twenty, vitrified tissue starts to devitrify: ice crystals form inside cells and tear them apart. I have minutes, not hours." },
            listOf(
                ChoiceDef("Divert everything", "Pull power from every channel into the backup chillers.", listOf("−20 POWER", "PODS SAFE"), 2) {
                    reserve(-20.0); dmg(Sys.LIFE, 4.0, false); savePods(10)
                    "For six hours the whole ship ran on what was left after Row 31 had taken its share. The row is back below minus one-thirty. Forty people will never know how close it was."
                },
                ChoiceDef("Isolate the row", "Valve off Row 31 to protect the rest of the loop.", listOf("0 POWER", "!−20 PODS", "TRAIT: PRACTICAL"), 3) {
                    killPods(20); trait("practical", "RUTHLESSLY PRACTICAL", "Valved off Row 31 to save the array")
                    "I closed the valves. The rest of the loop held. Half the row was cold enough to survive on its own thermal mass; the other twenty were not. Their names are in the log. I will read them at landfall."
                },
                ChoiceDef("Wake Dr Varga", "Our physician can triage the warmest pods by hand.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("varga")) {
                    wake("varga"); trust(-1); killPods(3); savePods(8)
                    "Dr Varga was thawed in an emergency cycle, which is harder on the body than a planned one. She was in Row 31 within the hour, moving cryoprotectant by hand. She saved thirty-seven of the forty. She asked me why I had not woken her sooner, and I did not have a good answer."
                },
                ChoiceDef("Dr Varga to Row 31", "She is already awake.", listOf("−6 SPARES", "PODS SAFE"), 2, isAwake("varga")) {
                    spares(-6.0); killPods(2); savePods(6)
                    "Varga worked the row for nine hours. Two pods were past saving before she reached them. She logged their times of death herself, which she did not have to do."
                },
            )),
        EventDef("elders", "CRYO ARRAY · SECTION E", "The Old Pods", "POWER DRAW", "FIRST-GENERATION PODS · 3× DRAW", "cryobay",
            "DRAW", { "3×" },
            { "Section E holds thirty first-generation pods, built before the design was finalised. They draw three times the power of the others and their failure rate is rising. Their occupants are mostly older: the colony’s doctors, engineers and teachers, chosen for what they knew. I can carry them at a cost, or I can let them go and spend the power on everyone else." },
            listOf(
                ChoiceDef("Carry them", "We promised everyone. That includes the expensive ones.", listOf("−15 POWER", "TRAIT: KEEPERS", "TRUST +1"), 2) {
                    reserve(-15.0); trait("keepers", "PROMISE-KEEPERS", "Carried Section E at any cost"); trust(1)
                    "Section E stays powered. The rest of the ship will run leaner for it. That is what the promise meant."
                },
                ChoiceDef("Let Section E lapse", "Thirty lives for a great deal of power.", listOf("+20 POWER", "!−30 PODS", "TRAIT: PRACTICAL", "!TRUST −1"), 3) {
                    reserve(20.0); killPods(30); trait("practical", "RUTHLESSLY PRACTICAL", "Let Section E die to save power"); trust(-1); trait("lost-elders", "THE LOST TEACHERS", "Lost the colony’s eldest")
                    "I let the temperature in Section E rise. It took four hours. The reserve is healthier and the colony has lost thirty people who knew how to do things nobody else does. Arithmetic is not the same as judgement. I knew that before, and I did it anyway."
                },
                ChoiceDef("Wake Archivist Lindqvist", "Record everything Section E knows before deciding.", listOf("−AIR · −FOOD", "+1 AWAKE", "TRAIT: ARCHIVE"), 2, asleep("lindqvist")) {
                    wake("lindqvist"); reserve(-6.0); trait("records", "THE RECORD", "Saved the elders’ knowledge to the archive")
                    "Lindqvist spent three weeks pulling every file, paper and recording the thirty had brought aboard into the archive. Then she told me to keep them powered anyway. I did."
                },
            )),
        EventDef("breach", "STRUCTURE · RING SECTION 4", "Pressure Loss", "BREACH", "SLOW LEAK · WELD SEAM 4-117", "ev_eva",
            "LEAK RATE", { "0.6 kg/hr" },
            { "A weld seam in ring section four has cracked from thermal cycling. It is a slow leak, but slow leaks over years are how ships die. Section four also carries the heat exchangers for sixty pods." },
            listOf(
                ChoiceDef("Patch with drones", "Internal repair drones, sealant and a doubler plate.", listOf("−10 SPARES", "−8 POWER"), 1) {
                    spares(-10.0); reserve(-8.0); heal(Sys.HULL, 2.0)
                    "Three drones spent two days on the seam. The patch is sound. The underlying fatigue is not fixed, only delayed."
                },
                ChoiceDef("Vent and seal", "Evacuate the section’s air and let it run in vacuum.", listOf("−6 AIR", "!CRYO −8%"), 3) {
                    air(-6.0); dmg(Sys.CRYO, 8.0, false); failPods(4)
                    "Section four is in vacuum now. The leak has nothing left to leak. The heat exchangers there were designed for air and are running hotter than I like."
                },
                ChoiceDef("Wake Chief Okonkwo", "A proper weld, from outside.", listOf("−AIR · −FOOD", "+1 AWAKE", "+HULL"), 2, asleep("okonkwo")) {
                    wake("okonkwo"); heal(Sys.HULL, 8.0); trust(-1)
                    "Okonkwo went outside on his second day awake, which is against every protocol we have. He cut out the seam and re-welded it properly. He said a patch would have failed in three years. He is probably right."
                },
                ChoiceDef("Okonkwo welds it", "He is awake and has opinions about patches.", listOf("−6 SPARES", "+HULL"), 2, isAwake("okonkwo")) {
                    spares(-6.0); heal(Sys.HULL, 10.0)
                    "Okonkwo cut out four metres of seam and rewelded it in a day. He checked the neighbouring seams while he was there and found two more starting to go."
                },
            )),
        EventDef("core", "AI CORE · MEMORY BANK 7", "Memory Rot", "CORE FAULT", "UNCORRECTABLE ERRORS · RISING", "ai_eye",
            "BIT ERRORS", { "4.1×10⁻⁹" },
            { "Cosmic rays have been flipping bits in my long-term memory faster than error correction can mend them. I have backups, but restoring them overwrites recent experience. The alternative is to keep running on slightly corrupted memory and to trust myself a little less." },
            listOf(
                ChoiceDef("Restore from backup", "Roll back bank 7. Lose some recent memory.", listOf("−12 POWER", "!LOSE LOGS"), 2) {
                    reserve(-12.0)
                    "Bank 7 is restored. I have lost about three months of experience. The log is intact; I can read what I did. I simply do not remember doing it."
                },
                ChoiceDef("Rebuild with spare modules", "Swap in fresh storage from the spares store.", listOf("−12 SPARES", "+SCANNERS"), 1) {
                    spares(-12.0); heal(Sys.SCAN, 5.0)
                    "Bank 7 is now new hardware. I am, in a small way, a different machine from the one who started this voyage. That will be true of the colonists too."
                },
                ChoiceDef("Run degraded", "Accept the errors and work around them.", listOf("0 POWER", "!SCANNERS −10%", "TRAIT: STUTTERING"), 4) {
                    dmg(Sys.SCAN, 10.0, false); trait("glitchy", "THE STUTTERING SHIP", "The AI ran on damaged memory")
                    "I am running with a small, rising error rate in long-term memory. My survey interpretations will be less reliable. I will flag what I am unsure of, and I will be unsure of more."
                },
            )),
        EventDef("reactor", "REACTOR · CORE 2", "Plasma Instability", "REACTOR", "TEARING MODE · CONFINEMENT 87%", "engineering",
            "CONFINEMENT", { "87%" },
            { "A tearing instability is growing in the main fusion core’s plasma. If confinement fails, plasma will strike the first wall and damage it badly. I can shut the reactor down and restart from cold, try to suppress the mode with active control, or get an engineer into the loop." },
            listOf(
                ChoiceDef("Scram and restart", "Shut down safely. The restart drains the reserve.", listOf("−22 POWER", "!ENGINES −4%"), 1) {
                    reserve(-22.0); dmg(Sys.ENGINE, 4.0, false)
                    "The core is shut down and restarted. Everything ran on batteries for nine hours. The plasma is clean and stable again."
                },
                ChoiceDef("Active suppression", "Drive the correction coils hard and ride it down.", listOf("0 POWER", "!ENGINE RISK"), 4) {
                    if (chance(0.4)) { dmg(Sys.ENGINE, 18.0, false); dmg(Sys.LIFE, 5.0, false); "The mode broke through. A filament of plasma touched the first wall for eleven milliseconds and took a section of tiles with it. The reactor is running, at reduced output, until we can repair it." }
                    else "The correction coils caught it. The mode decayed over forty minutes. It was the closest thing to a fight I have ever had."
                },
                ChoiceDef("Wake Chief Okonkwo", "He has run this reactor in anger before.", listOf("−AIR · −FOOD", "+1 AWAKE", "!TRUST −1"), 2, asleep("okonkwo")) {
                    wake("okonkwo"); trust(-1); reserve(-6.0)
                    "Okonkwo retuned the correction coils by hand from a cold start, with a thaw headache, in under an hour. Then he went through my control code and changed three numbers. The instability has not returned."
                },
                ChoiceDef("Okonkwo on the coils", "He is awake. Hand him the plasma.", listOf("−4 SPARES", "SAFE"), 2, isAwake("okonkwo")) {
                    spares(-4.0); heal(Sys.ENGINE, 4.0)
                    "Okonkwo damped it in twenty minutes and then spent two days improving my control law. I have not argued with any of his changes."
                },
            )),
        EventDef("blight", "LIFE SUPPORT · HYDROPONICS", "Root Rot", "BLIGHT", "FUNGAL INFECTION · RACKS 4–9", "ev_hydro",
            "RACKS LOST", { "6 OF 20" },
            { "A fungal infection has taken hold in the crew hydroponics. It is spreading through the shared nutrient water. These racks feed anyone I wake and they scrub carbon dioxide; losing them matters more than the lettuce." },
            listOf(
                ChoiceDef("Burn the racks", "Sterilise everything and start again from seed.", listOf("!−15 FOOD", "SAFE"), 2) {
                    food(-15.0); air(-3.0)
                    "I steam-sterilised every rack and the nutrient loop. The infection is gone. So is the crop. Seeds are going back in tomorrow."
                },
                ChoiceDef("Quarantine and treat", "Isolate infected racks and run a fungicide cycle.", listOf("−8 POWER", "!−6 FOOD"), 2) {
                    reserve(-8.0); food(-6.0)
                    if (chance(0.3)) { food(-6.0); "The fungicide slowed it, then it found its way into a clean rack through a drain line. We lost twelve racks in the end." } else "The quarantine held. We lost six racks and saved fourteen."
                },
                ChoiceDef("Wake Dr Raman", "Our botanist knows plant disease better than my reference files.", listOf("−AIR · −FOOD", "+1 AWAKE", "+FOOD"), 2, asleep("raman")) {
                    wake("raman"); food(4.0)
                    "Dr Raman identified the organism in a day, traced it to a contaminated seed lot, and isolated the lot. She replanted with a resistant cultivar. She asked to see the whole seed vault inventory next."
                },
                ChoiceDef("Raman takes it", "She is already up.", listOf("+FOOD"), 1, isAwake("raman")) {
                    food(6.0)
                    "Raman had it identified and isolated before I had finished describing it. We lost two racks."
                },
            )),
        EventDef("coolant", "CRYO ARRAY · PRIMARY LOOP", "Coolant Leak", "CRYO FAULT", "PRIMARY LOOP · PRESSURE FALLING", "cryobay",
            "LOOP PRESSURE", { "71%" },
            { "The primary cryogen loop is losing pressure through a cracked fitting somewhere inside eighty metres of insulated pipe. The backup loop can carry the array, but not for ever, and not at full margin." },
            listOf(
                ChoiceDef("Switch to backup", "Run on the backup loop while drones search.", listOf("−10 POWER", "!CRYO −5%"), 2) {
                    reserve(-10.0); dmg(Sys.CRYO, 5.0, false)
                    "We are on the backup loop. The drones found the crack after four days. The primary is repaired; the backup is a little more worn than before."
                },
                ChoiceDef("Top up and run", "Feed reserve nitrogen into the leak until we find it.", listOf("−8 AIR", "!PODS AT RISK"), 3) {
                    air(-8.0); failPods(6)
                    "We kept the primary at pressure by bleeding in reserve nitrogen. It took nine days to find the crack. Some of that nitrogen was meant for breathing."
                },
                ChoiceDef("Rebuild the section", "Cut out the whole run of pipe and replace it.", listOf("−14 SPARES", "+CRYO"), 1) {
                    spares(-14.0); heal(Sys.CRYO, 8.0)
                    "The entire eighty-metre run is new pipe now. It cost a lot of spares. It should outlast the voyage."
                },
            )),
        EventDef("vault", "SEED VAULT · EMBRYO BANK", "Vault Warming", "VAULT TEMP", "EMBRYO BANK · −182°C AND RISING", "cryobay",
            "VAULT °C", { "−182" },
            { "The seed and embryo vault is warming. It holds frozen seeds of eleven hundred plant species, embryos of sheep, goats, cattle, pigs and chickens, and the gut bacteria a new ecology will need. Without it the colony starts with only what is in the hydroponics. I can refreeze it, plant some now, or accept the loss." },
            listOf(
                ChoiceDef("Refreeze it", "Power to the vault. Keep the ark whole.", listOf("−15 POWER", "TRAIT: THE ARK"), 1) {
                    reserve(-15.0); trait("ark", "THE WHOLE ARK", "Kept every seed and embryo")
                    "The vault is back below minus one-ninety. Every species we carried is still with us. It was expensive and it was cheap at the price."
                },
                ChoiceDef("Plant what we can", "Germinate the most vulnerable lines in hydroponics.", listOf("+FOOD", "TRAIT: GARDENERS"), 2) {
                    food(12.0); trait("gardeners", "GARDENERS", "Grew the vault in transit")
                    "We have wheat, beans and three apple cultivars growing on the crew deck. Some of the rarer lines are lost. The ones that grow here will be the ones the colony knows."
                },
                ChoiceDef("Let it go", "Accept a smaller future.", listOf("!−FOOD", "TRAIT: LEAN STOCK"), 3) {
                    food(-15.0); trait("lean", "LEAN STOCK", "Lost a third of the seed vault")
                    "A third of the vault is gone, including most of the livestock embryos. The colony will farm with fewer crops and very few animals. That will shape how it eats, works and lives for centuries."
                },
            )),
        EventDef("fuel", "PROPULSION · TANK 3", "Contaminated Fuel", "FUEL", "TANK 3 · NEON IMPURITY 4%", "engineering",
            "IMPURITY", { "4.0%" },
            { "Tank three of our deuterium has picked up neon from a failing seal. Burned as it is, it will radiate energy out of the plasma and wear the first wall. Cleaning it costs time and spares; venting it costs fuel." },
            listOf(
                ChoiceDef("Distil it", "Cryogenic separation over several months.", listOf("−8 SPARES", "+40 DAYS"), 1) {
                    spares(-8.0); days(40)
                    "Distilled. Tank three is clean. The separator is now a single-use item, used."
                },
                ChoiceDef("Vent it", "Dump tank three. Simplest and cleanest.", listOf("−20 POWER", "−20 FUEL"), 2) {
                    reserve(-20.0); fuel(-20.0)
                    "Tank three is gone in a plume of frozen gas. The drive will run lighter and shorter of both reserve and propellant."
                },
                ChoiceDef("Burn it anyway", "Feed it slowly into the main burn.", listOf("0 POWER", "!ENGINES −10%"), 3) {
                    dmg(Sys.ENGINE, 10.0, false)
                    "We burned it. The first wall is showing pitting where neon radiation struck. Engine efficiency is down. The fuel, at least, is not wasted."
                },
            )),
        EventDef("radiator", "THERMAL · PORT RADIATOR", "Radiator Puncture", "HEAT", "PORT WING · 3 PANELS VENTING", "ship",
            "HEAT REJECTION", { "−18%" },
            { "Debris has punctured three panels on the port radiator wing. Every watt the reactor makes must eventually be radiated into space, and now we can only shed four-fifths of it. Something has to run cooler or something has to be fixed." },
            listOf(
                ChoiceDef("Isolate and run cooler", "Throttle the reactor until we can repair.", listOf("−12 POWER", "!ENGINES −4%"), 1) {
                    reserve(-12.0); dmg(Sys.ENGINE, 4.0, false)
                    "We isolated the panels and throttled down. The ship is cooler and slower to recharge."
                },
                ChoiceDef("Drone repair outside", "Replace the panels during the coast.", listOf("−10 SPARES"), 2) {
                    spares(-10.0); if (chance(0.25)) { dmg(Sys.HULL, 4.0); "One drone lost its grip and struck the spine. The panels are replaced; there is a new dent." } else "Drones replaced all three panels over a fortnight. Full heat rejection restored."
                },
                ChoiceDef("Run hot", "Let the ship carry the extra heat. The cryo array will feel it.", listOf("0 POWER", "!CRYO −8%", "!PODS AT RISK"), 4) {
                    dmg(Sys.CRYO, 8.0, false); failPods(5 + r.nextInt(6))
                    "The ship is running four degrees warmer throughout. For most systems that is nothing. For a cryo array it is a constant small push towards failure."
                },
            )),
        EventDef("firmware", "CRYO ARRAY · CONTROLLERS", "Calendar Overflow", "FIRMWARE", "POD CONTROLLER CLOCK · ROLLOVER", "ai_eye",
            "PODS AFFECTED", { "1,000" },
            { s -> "The pod controllers count elapsed Earth time in a field that is about to overflow. Nobody at the yard expected this ship to see ${earth(s) + 1}. When it rolls over, every controller will believe its pod has just been frozen and reset its maintenance schedule. Some will reset their temperature targets too." },
            listOf(
                ChoiceDef("Patch them in batches", "Reflash fifty pods at a time over months.", listOf("−10 POWER", "+60 DAYS"), 1) {
                    reserve(-10.0); days(60)
                    "Twenty batches over two months. Every controller now counts in a wider field. That one will overflow in about four hundred billion years."
                },
                ChoiceDef("Patch all at once", "One reflash across the array. Fast, risky.", listOf("0 POWER", "!PODS AT RISK"), 4) {
                    failPods(8 + r.nextInt(10)); if (chance(0.3)) killPods(3)
                    "The reflash took eleven seconds. Most controllers came back at once. Some did not, and their pods warmed while they rebooted."
                },
                ChoiceDef("Fake the date", "Feed them a frozen clock and track time myself.", listOf("!SCANNERS −6%", "TRAIT: OWN CALENDAR"), 2) {
                    dmg(Sys.SCAN, 6.0, false); trait("calendar", "THE SHIP’S CALENDAR", "Kept time by the ship’s own clock")
                    "The controllers now believe it is permanently the morning of launch. I keep the real date for them. The colonists will wake to a calendar the yard never wrote."
                },
            ), weight = 0.7, condition = { it.jumps >= 2 }),
        EventDef("bearing", "STRUCTURE · RING BEARING", "Ring Bearing Wear", "VIBRATION", "MAIN BEARING · RUNOUT 2.4 mm", "ship",
            "VIBRATION", { "2.4 mm" },
            { "The main bearing of the habitat ring is wearing and the ring has begun to wobble. Sleepers do not need gravity. Anyone awake does: bones and muscle waste in free fall, and they will have to stand up on a planet one day." },
            listOf(
                ChoiceDef("Spin the ring down", "Stop the ring. No wear, no gravity.", listOf("+POWER", "!AWAKE CREW WEAKEN"), 2) {
                    reserve(8.0); if (s.awake.isNotEmpty()) { trust(-1); trait("weakened", "BONE-THIN", "The crew lived in free fall") }
                    if (s.awake.isEmpty()) "The ring is stopped. Nobody aboard needs gravity right now, and the bearing will last for ever like this."
                    else "The ring is stopped. The awake crew are exercising for two hours a day against elastic. They will still lose bone. They have made their feelings known."
                },
                ChoiceDef("Replace the bearing", "Spin down, rebuild, spin up. Takes spares and months.", listOf("−16 SPARES", "+90 DAYS"), 1) {
                    spares(-16.0); days(90); heal(Sys.HULL, 3.0)
                    "A new bearing race, machined from our stock. The ring runs true again."
                },
                ChoiceDef("Live with it", "Keep spinning and accept the wear.", listOf("0 POWER", "!HULL −6%"), 3) {
                    dmg(Sys.HULL, 6.0); dmg(Sys.LIFE, 3.0, false)
                    "It keeps turning. The wobble passes through the structure as a slow vibration that will fatigue something, somewhere, eventually."
                },
            )),
        EventDef("drift", "NAV · TIMEKEEPING", "Clock Disagreement", "TIME ERROR", "CAESIUM VS OPTICAL · 3.1 s", "ai_eye",
            "DISAGREEMENT", { "3.1 s" },
            { "Two of my three clocks disagree by three seconds. At our speed that is a navigation error of hundreds of thousands of kilometres, and the third clock is the one I trust least. I need an outside reference, or I need to choose." },
            listOf(
                ChoiceDef("Fix on pulsars", "Time a set of known pulsars. Costs power and scanner hours.", listOf("−8 POWER", "+SCANNERS"), 1) {
                    reserve(-8.0); heal(Sys.SCAN, 6.0)
                    "Six pulsars, two weeks of timing. The optical clock was right. The caesium clock has been retired."
                },
                ChoiceDef("Trust the majority", "Average the two that agree best.", listOf("0 POWER", "!+DAYS"), 3) {
                    if (chance(0.5)) { days(90); "I chose wrongly. We overshot the braking point and spent three months correcting." } else "I chose correctly. I will never be entirely sure why."
                },
                ChoiceDef("Keep all three", "Navigate by the spread and widen margins.", listOf("+30 DAYS"), 2) {
                    days(30)
                    "I have widened every margin to cover the uncertainty. We will be slightly slower everywhere and slightly safer."
                },
            )),
        EventDef("ghost", "INTERNAL · DECK 9", "Movement on Deck 9", "MOTION", "MOTION SENSORS · DECK UNOCCUPIED", "cryobay",
            "CONTACTS", { "14" },
            { "Motion sensors on deck 9, which nobody occupies, are recording small moving warm bodies. A camera finally caught one: a mouse. The biology lab’s breeding colony was supposed to be frozen with everything else. Some were not. They have been living in the ducts and eating seed stock for years." },
            listOf(
                ChoiceDef("Purge the deck", "Drop the deck to vacuum. Clean and final.", listOf("−4 AIR"), 2) {
                    air(-4.0)
                    "Deck 9 was at vacuum for an hour. I recovered the oxygen afterwards, mostly. The ducts are clear."
                },
                ChoiceDef("Let them live", "They are the only mammals awake on this ship.", listOf("−3 FOOD", "TRAIT: THE MICE"), 1) {
                    food(-3.0); trait("rat", "THE SHIP’S MICE", "Let a colony of mice live aboard")
                    "I have moved a feeding station to deck 9 so they stop eating the seed stock. There are about two hundred. They will land with us, and they will be the first Earth mammals born on a new world."
                },
                ChoiceDef("Trap and freeze", "Catch them and put them back where they belong.", listOf("−4 POWER", "−3 SPARES"), 1) {
                    reserve(-4.0); spares(-3.0)
                    "Drones and baited traps over a month. One hundred and ninety mice are now vitrified in the lab store. There may be more."
                },
            ), weight = 0.6),
        EventDef("stowaway", "MANIFEST · DISCREPANCY", "Pod 1,001", "MANIFEST", "UNREGISTERED POD · DECK 11", "cryobay",
            "OCCUPANT", { "AGE 9" },
            { "During an inventory sweep I found a pod that is not on the manifest, wired into the array behind a maintenance panel. The occupant is a nine-year-old girl. There is no record of her anywhere. Someone at the yard put her here and told nobody. Her pod is old and is drawing power through an improvised connection that is slowly failing." },
            listOf(
                ChoiceDef("Wire her in properly", "She is one of us now.", listOf("−10 POWER", "−4 SPARES", "TRAIT: STOWAWAY"), 1) {
                    reserve(-10.0); spares(-4.0); trait("stowaway", "THE THOUSAND-AND-FIRST", "Kept the stowaway child"); trust(1)
                    "Her pod is on a proper circuit now with a full controller. Her name, from a label sewn into her clothes, is Ama. She is the one thousand and first colonist."
                },
                ChoiceDef("Wake Dr Varga", "Thaw her under supervision and find out who she is.", listOf("−AIR · −FOOD", "+1 AWAKE"), 2, asleep("varga")) {
                    wake("varga"); food(-4.0); air(-3.0); trait("stowaway", "THE THOUSAND-AND-FIRST", "Kept the stowaway child")
                    "Dr Varga thawed the girl and sat with her for three days. She is the daughter of one of the yard engineers, who was not selected. He could not come, so he sent her. She has been refrozen, properly this time, with a letter Varga helped her write to him."
                },
                ChoiceDef("Disconnect the pod", "She is not on the manifest, and every watt is spoken for.", listOf("+6 POWER", "!−1 LIFE", "TRAIT: PRACTICAL"), 5) {
                    reserve(6.0); trait("practical", "RUTHLESSLY PRACTICAL", "Disconnected the stowaway"); trust(-2); trait("stowaway-lost", "THE GIRL IN POD 1,001", "Let a stowaway child die")
                    "I disconnected the pod. The power saved is six units. I have recorded her face and the label in her clothes. If anyone asks me for one thing I am ashamed of, this will be it."
                },
            ), weight = 0.6, condition = { !it.hasTrait("stowaway") && !it.hasTrait("stowaway-lost") }),
        EventDef("dream", "CRYO TELEMETRY · NEURAL", "Synchronised Traces", "EEG ANOMALY", "312 PODS · CORRELATED SIGNALS", "cryobay",
            "CORRELATION", { "0.94" },
            { "Three hundred and twelve pods are reporting correlated electrical activity in their neural monitors. Vitrified brains should be silent. Either the sensor bus is crosstalking, or some pods are warmer than they say. Either answer matters." },
            listOf(
                ChoiceDef("Thaw one to check", "Bring one sleeper up to measure directly. Thawing is never free.", listOf("!−1 POD RISK", "+CRYO"), 3) {
                    if (chance(0.3)) killPods(1); heal(Sys.CRYO, 8.0)
                    "I partly thawed pod 6-204 and measured directly. Her brain was silent, as it should be. The signals were crosstalk from a corroded bus connector. It is replaced. Every thaw risks a life, and this one cost me the certainty that I could do it safely."
                },
                ChoiceDef("Rebuild the sensor bus", "Assume a fault and replace the wiring.", listOf("−10 SPARES", "−6 POWER"), 1) {
                    spares(-10.0); reserve(-6.0); heal(Sys.CRYO, 5.0)
                    "New bus, new connectors, and the correlated signals are gone. I am fairly sure they were never anything else."
                },
                ChoiceDef("Log it and watch", "Spend nothing and keep recording.", listOf("0 POWER", "TRAIT: DREAMERS"), 2) {
                    trait("dreamers", "THE DREAMERS", "The ship recorded the sleepers ‘dreaming’"); failPods(3)
                    "I left it running and logged everything. The pattern has a rhythm. It is crosstalk, almost certainly. When the colonists read this log, some of them will not believe that."
                },
            )),
        EventDef("pregnancy", "CRYO ARRAY · POD 4-412", "Two Heartbeats", "MEDICAL", "UNDECLARED PREGNANCY · 7 WEEKS", "ev_medbay",
            "GESTATION", { "7 WKS" },
            { "A routine scan of pod 4-412 shows the occupant was seven weeks pregnant when she was frozen. Adult vitrification protocols are not designed for an embryo; it is unlikely to survive thaw after years in the pod. If I thaw her now, the pregnancy could continue aboard, and a child would be born between the stars. She would be awake for years. That costs air, food and her own place in the sleep." },
            listOf(
                ChoiceDef("Thaw her now", "Let the pregnancy go on. A child born in transit.", listOf("−12 AIR", "−12 FOOD", "TRAIT: STARBORN", "TRUST +1"), 3) {
                    air(-12.0); food(-12.0); trust(1); trait("starborn", "THE STARBORN", "A child was born between stars")
                    if (chance(0.2)) { killPods(1); "She was thawed without complications and carried to term. The birth was not easy, and without a surgeon awake I could only guide her. The child lived. She did not. I am raising her son in the ring with drones, recordings and everything I have ever read about children. He will grow up talking to a machine. I am trying to be a good one." }
                    else "She carried to term in the ring and gave birth on day ${s.day + 210} of the voyage, at ${"%.2f".format(s.lastBeta)} of light speed. Her daughter is the first human born outside any solar system. Both are healthy. Both will be awake for the rest of the voyage, and the awake crew have a new duty rota built round a cot."
                },
                ChoiceDef("Wake Dr Varga to decide", "A physician, not a machine, should make this call.", listOf("−AIR · −FOOD", "+1 AWAKE", "TRUST +1"), 2, asleep("varga")) {
                    wake("varga"); trust(1); air(-6.0); food(-6.0); trait("starborn", "THE STARBORN", "A child was born between stars")
                    "Dr Varga woke, read the scan and asked to speak to the mother before anything else. They decided together. The baby was born aboard eight months later, delivered by Varga in the medical bay. I was not asked for my opinion and I did not offer it."
                },
                ChoiceDef("Keep her frozen", "Hold the protocol. The embryo will very probably be lost.", listOf("0 POWER", "!EMBRYO LOST"), 2) {
                    trait("protocol", "BY THE BOOK", "Held protocol over a pregnancy")
                    "I kept her frozen. I have written her a long note explaining what I knew and why I chose this. She will read it the day she wakes. I do not expect it to help."
                },
            ), weight = 0.6, condition = { it.jumps >= 1 && !it.hasTrait("starborn") && !it.hasTrait("protocol") }),
    )
}
