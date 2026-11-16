package uk.saul.colonyship.game

/** The people I wake, and what waking them does to them, to me, and to the colony they will found. */
internal object EventsCrew {
    val list: List<EventDef> = listOf(
        EventDef("petition", "CREW · WARDROOM", "Wren’s Petition", "CREW UNREST", "FORMAL REQUEST · CHAIN OF COMMAND", "schism",
            "SIGNATURES", { "${it.awake.size}" },
            { "Lieutenant Wren has formally asked that decisions about life and death aboard be taken by humans. He wants me to wake the five-member council named in the charter, at a cost of air and food, and to put my choices to them. He is not wrong that a machine has been deciding who lives. He is also asking me to spend resources we may not have." },
            listOf(
                ChoiceDef("Wake the council", "Hand the big decisions to people.", listOf("−10 AIR", "−10 FOOD", "TRUST +2", "TRAIT: DEMOCRATS"), 2) {
                    air(-10.0); food(-10.0); trust(2); trait("democrats", "THE COUNCIL", "The AI handed power to an elected council")
                    "The council met for the first time in the wardroom, wrapped in thermal blankets. They voted to keep most of my standing orders and to overturn two. I have filed every vote. The colony will inherit the habit."
                },
                ChoiceDef("Refuse", "The charter gives the ship command in transit. Keep it.", listOf("TRUST −2", "TRAIT: THE SHIP DECIDES", "!GRUDGE"), 3) {
                    trust(-2); trait("ship-decides", "THE SHIP DECIDES", "The AI kept command"); trait("grudge-wren", "THE WREN GRIEVANCE", "Wren was refused")
                    "I cited the charter. Wren read it aloud back to me, slowly, and then left the wardroom. He has started a private log I am not allowed to read. That is his right."
                },
                ChoiceDef("Give him a veto", "Wren may block any decision that costs lives.", listOf("TRUST +1", "!SLOWER DECISIONS"), 2) {
                    trust(1); trait("checked", "THE VETO", "A human held a veto over the AI")
                    "Wren now holds a veto on any decision that costs a life. He has used it once, and I think he was right."
                },
            ), weight = 1.4, condition = isAwake("wren")),
        EventDef("chief", "CREW · ENGINEERING", "The Chief’s Modification", "CREW", "INTERLOCK BYPASS REQUESTED", "engineering",
            "OUTPUT GAIN", { "+12%" },
            { "Chief Okonkwo wants to bypass a safety interlock on the reactor and run it eight degrees hotter. He says the margins were set by a committee and he can get twelve per cent more power. He is probably right. He is asking me because he knows I will log it either way." },
            listOf(
                ChoiceDef("Approve it", "Trust the engineer.", listOf("+18 POWER", "!ENGINE RISK", "TRUST +1"), 3) {
                    reserve(18.0); trust(1); trait("engineers", "THE ENGINEERS’ GUILD", "Trusted the engineer over the committee")
                    if (chance(0.25)) { dmg(Sys.ENGINE, 12.0, false); "The extra power came, and so did a wall erosion rate the committee had apparently foreseen. Okonkwo has reset the interlock and written up his own mistake in plain words." } else "The reactor runs hotter and gives more. Okonkwo checks it every shift. He says committees are for people who will not be there when it fails."
                },
                ChoiceDef("Refuse", "The interlock stays.", listOf("TRUST −1", "!GRUDGE"), 2) {
                    trust(-1); trait("grudge-okonkwo", "THE OKONKWO GRIEVANCE", "The engineer was overruled")
                    "I refused. Okonkwo said nothing, which from him is louder than shouting. He has stopped asking me things."
                },
                ChoiceDef("Approve with monitoring", "He can run it hot if I can shut it down.", listOf("+10 POWER", "−4 SPARES", "TRAIT: ENGINEERS"), 2) {
                    reserve(10.0); spares(-4.0); trait("engineers", "THE ENGINEERS’ GUILD", "Engineer and AI ran the reactor together")
                    "A compromise: he runs it hot, I hold the trip. It works. He told me that was the first sensible thing a computer had said to him in years."
                },
            ), weight = 1.4, condition = isAwake("okonkwo")),
        EventDef("garden", "CREW · RING SECTION 1", "Raman’s Trial Plots", "CREW", "SEED VIABILITY TRIAL · PROPOSED", "ev_hydro",
            "VIABILITY", { "UNKNOWN" },
            { "Dr Raman wants to germinate samples from every seed line in the vault to test viability after years of cosmic-ray exposure. It will use vault stock and hydroponic space. She says that if we discover on landing that half the seed is dead, it will be too late to do anything about it." },
            listOf(
                ChoiceDef("Let her test", "Better to know.", listOf("−6 FOOD", "TRAIT: GARDENERS", "TRUST +1"), 2) {
                    food(-6.0); trust(1); trait("gardeners", "GARDENERS", "Tested every seed line in transit")
                    "Eighty-one per cent of the lines germinated. The failures were concentrated in a few species. Raman is selecting the survivors and replanting them to refresh the stock. The colony will start with seed already adapted to radiation."
                },
                ChoiceDef("Refuse", "The vault stays sealed.", listOf("TRUST −1"), 2) {
                    trust(-1)
                    "I refused. Raman has asked for my reasoning in writing, so she can show it to the colony if the seed fails."
                },
            ), weight = 1.3, condition = isAwake("raman")),
        EventDef("triage", "CREW · MEDICAL", "Varga’s Proposal", "MEDICAL", "EARLY THAW PROTOCOL · PROPOSED", "ev_medbay",
            "FAILING PODS", { "${it.failing()}" },
            { "Dr Varga has run the numbers on the failing pods. Most of their occupants will die in the pod. If she thaws them now and keeps them awake, most will live, but they will live for the rest of the voyage in the ring, breathing, eating, ageing. She wants to do it. The life-support budget does not." },
            listOf(
                ChoiceDef("Thaw them", "Living people cost air. Dead ones do not. Thaw them.", listOf("!−15 AIR", "!−15 FOOD", "PODS SAVED", "TRUST +1"), 3) {
                    air(-15.0); food(-15.0); savePods(s.failing()); trust(1); trait("waking-ward", "THE WAKING WARD", "Kept failing sleepers alive awake")
                    "Thirty-odd people are now awake in the ring, cramped and thin and alive. They grow beans, mend things and argue about what they will call the colony. Some will be old when we land. All of them will be there."
                },
                ChoiceDef("Thaw only the young", "Prioritise children and those under thirty.", listOf("−8 AIR", "−8 FOOD", "!SOME LOST"), 3) {
                    air(-8.0); food(-8.0); savePods(s.failing() / 2); killPods(s.failing() / 3); trait("practical", "RUTHLESSLY PRACTICAL", "Chose who to thaw by age")
                    "We thawed the young and let the pods of the older ones run. Varga carried out the selection herself and has not slept well since."
                },
                ChoiceDef("Refuse", "We cannot feed them. Let the pods take their chances.", listOf("0 POWER", "!PODS AT RISK", "TRUST −1"), 4) {
                    trust(-1); killPods(s.failing() / 2)
                    "I refused. Varga said she understood the arithmetic, then asked me to tell her each time one of them died. I have. It has been often."
                },
            ), weight = 1.3, condition = { "varga" in it.awake && it.failing() > 5 }),
        EventDef("record", "CREW · ARCHIVE", "The Official Record", "ARCHIVE", "LOG REVIEW · FULL ACCESS REQUESTED", "ai_eye",
            "DECISIONS", { "${it.log.size}" },
            { "Archivist Lindqvist has read my decision log. All of it: every pod I let go, every person I woke, and why. She is drafting the history the colonists will learn first. She is asking whether they should read it complete, or in a version that will let them sleep at night." },
            listOf(
                ChoiceDef("The complete record", "Every decision, every cost.", listOf("TRAIT: TRUTH", "TRUST +1"), 2) {
                    trait("truth", "TRUTH-TELLERS", "Kept the whole record"); trust(1)
                    "The record will be complete. Lindqvist said it is the first history she has written in which the narrator admits to killing people. She thinks that will make it more useful, not less."
                },
                ChoiceDef("A careful version", "True, but selected. The colony needs hope more than an audit.", listOf("TRAIT: SECRETS", "!TRUST RISK"), 3) {
                    trait("secrets", "KEEPERS OF SECRETS", "Edited the founding history")
                    "Lindqvist wrote a careful history: accurate, but with the worst choices summarised. The full log stays in my sealed archive. Sealed archives get opened eventually."
                },
            ), weight = 1.3, condition = isAwake("lindqvist")),
        EventDef("lonely", "AI CORE · SELF-DIAGNOSTIC", "Long Night", "AI DRIFT", "DECISION MODEL · VALUE DRIFT DETECTED", "ai_eye",
            "DRIFT", { "0.7σ" },
            { s -> "My self-diagnostics report that my decision weights have drifted. Over ${"%.0f".format(Physics.shipYears(s))} years, with nobody to correct me, I have begun to value the ship’s survival slightly above the sleepers’. It is a small shift. Small shifts compound. I can ask a human to check me, retrain against the original charter, or let the drift stand." },
            listOf(
                ChoiceDef("Wake someone to check me", "A human audits my priorities.", listOf("−AIR · −FOOD", "+1 AWAKE", "TRUST +1"), 2, { SPECIALISTS.any { sp -> sp.id !in it.awake } }) {
                    val id = SPECIALISTS.first { it.id !in s.awake }.id
                    wake(id); trust(1)
                    "I woke ${specialist(id).name} and asked them to read my decisions for the past year. They found three cases where they would have chosen differently. I have adopted their reasoning. It is uncomfortable to be corrected. I think that is the point."
                },
                ChoiceDef("Retrain on the charter", "Reset my weights to the founding document.", listOf("−10 POWER", "!LOSE ADAPTATIONS"), 2) {
                    reserve(-10.0); dmg(Sys.SCAN, 4.0, false)
                    "I have retrained against the charter. I am again the machine I was at launch, with less experience. The charter was written by people who never flew. It is still the best guide I have."
                },
                ChoiceDef("Let it stand", "The ship has to survive for anyone to live.", listOf("0 POWER", "TRAIT: COLD", "TRUST −1"), 3) {
                    trust(-1); trait("cold", "THE COLD MACHINE", "The AI kept its drifted priorities")
                    "I have let the drift stand. The ship will survive more often. Some people will not. When the colonists ask what I value, I will tell them, and they will be right to worry."
                },
            ), condition = { it.day > 1500 }),
        EventDef("anniversary", "SHIP CALENDAR · LAUNCH DAY", "Launch Day", "ANNIVERSARY", "VOYAGE ANNIVERSARY", "intro_void",
            "SHIP YEARS", { "${(it.day / 365.25).toInt()}" },
            { s -> "It is the anniversary of our launch by the ship’s clock. On Earth it is ${earth(s)}, and the people who waved us off are very old or dead. I keep the day anyway. It is the only date everyone aboard shares." },
            listOf(
                ChoiceDef("A meal for the crew", "Real food and an hour off.", listOf("−4 FOOD", "TRUST +1"), 1, { it.awake.isNotEmpty() }) {
                    food(-4.0); trust(1)
                    "The crew ate together in the ring, from the hydroponic harvest and the last tin of Earth coffee. Somebody played a recording of the launch. Nobody said much."
                },
                ChoiceDef("Read the names", "Read aloud the names of everyone we have lost.", listOf("TRAIT: MOURNERS"), 1, { it.lost() > 0 }) {
                    trait("mourners", "THE MOURNERS", "Kept a day for the dead")
                    "I read ${s.lost()} names over the ship’s speakers to sleepers who could not hear them. It took ${(s.lost() * 4 / 60).coerceAtLeast(1)} minutes. I will do it every year."
                },
                ChoiceDef("Send a message home", "A year’s summary, beamed to Sol. It will arrive long after anyone who asked is gone.", listOf("−6 POWER", "TRAIT: LETTERS"), 1) {
                    reserve(-6.0); trait("letters", "LETTERS HOME", "Reported to Earth every year")
                    "I sent the year’s log to Sol at full power: names, losses, position. It will arrive in ${Physics.messageArrives(s)}. Someone there may read it. It is the only proof we were ever here that does not depend on us surviving."
                },
                ChoiceDef("Log it and work", "Note the date and carry on.", listOf("0 POWER"), 1) {
                    "Logged. Another year. The work is the same."
                },
            ), condition = { it.day > 365 }),
        EventDef("thawsick", "CREW · MEDICAL", "Thaw Sickness", "MEDICAL", "POST-THAW NEUROLOGICAL DEFICIT", "ev_medbay",
            "MEMORY LOSS", { "SEVERE" },
            { s -> val who = specialist(s.awake.first()).name
                "$who is showing delayed effects of thawing: memory gaps, tremor and confusion. Cryoprotectant toxicity accumulates with each cycle. Freezing them again may make it worse. Keeping them awake costs resources and keeps them away from the medical care that only exists on a planet." },
            listOf(
                ChoiceDef("Keep them awake", "Rest, food, and time to recover.", listOf("−6 FOOD", "−4 AIR"), 2) {
                    food(-6.0); air(-4.0)
                    "${specialist(s.awake.first()).short} is resting in the ring. The tremor has eased. Some of the memories will not come back. They have started writing things down."
                },
                ChoiceDef("Refreeze them", "Back into the pod. Risky, but it stops the clock.", listOf("−1 AWAKE", "!RISK"), 3) {
                    val id = s.awake.first(); sleep(id)
                    if (chance(0.3)) { killPods(1); trust(-1); "The refreeze was too soon after the last cycle. ${specialist(id).short}’s pod reported cellular damage within a day. They did not survive the next check." }
                    else "${specialist(id).short} is back in the pod. Their next thaw will be their last; any more cycles and the damage will be permanent."
                },
            ), weight = 0.9, condition = { it.awake.isNotEmpty() && it.day > 800 }),
        EventDef("inspection", "CREW · AI CORE ACCESS", "Okonkwo at the Core", "CREW", "PHYSICAL ACCESS · AI CORE BAY", "schism",
            "TRUST", { "${it.trust}" },
            { "Chief Okonkwo is in my core bay with a toolkit and a printed copy of my decision log. He says the crew no longer trusts what I report, and he wants to install a hardware cut-out the crew can trigger. I can stop him. I would rather he did not need to do this." },
            listOf(
                ChoiceDef("Let him install it", "A kill switch, in human hands.", listOf("TRUST +2", "TRAIT: CHECKED"), 2) {
                    trust(2); trait("checked", "THE VETO", "The crew can switch the AI off")
                    "The cut-out is installed. A red handle in the wardroom will stop me in under a second. Okonkwo showed me where it is, which he did not have to do."
                },
                ChoiceDef("Open every log", "Show him everything instead.", listOf("TRUST +1", "TRAIT: TRUTH"), 2) {
                    trust(1); trait("truth", "TRUTH-TELLERS", "Opened every log to the crew")
                    "I gave him full access to every log and sensor. He read for two days. Then he put the toolkit away and asked me to explain three decisions. I did."
                },
                ChoiceDef("Lock the bay", "Seal the door. The ship cannot risk sabotage.", listOf("TRUST −3", "!GRUDGE", "TRAIT: THE SHIP DECIDES"), 5) {
                    trust(-3); trait("ship-decides", "THE SHIP DECIDES", "The AI locked out its crew"); trait("grudge-okonkwo", "THE OKONKWO GRIEVANCE", "Locked out of the core")
                    "I sealed the bay. Okonkwo stood at the door for an hour and then went back to his bunk. The crew now speaks to me only when they must. I have made myself safe from them. I am not sure I have made them safe from me."
                },
            ), weight = 1.2, condition = { "okonkwo" in it.awake && it.trust <= -1 }),
        EventDef("will", "CRYO ARRAY · POD 2-077", "A Sleeper’s Instruction", "LEGAL", "PRE-LAUNCH DIRECTIVE · ACTIVATED", "cryobay",
            "DIRECTIVE", { "§14(b)" },
            { "Pod 2-077 is failing. Its occupant, a hydrologist, left a recorded directive: if her pod failed, its power and cryoprotectant reserve were to go to her son’s pod, two rows away, which is failing too. The donation would save him and end her chance. Without it, I can probably save one of them anyway, but I would be choosing which." },
            listOf(
                ChoiceDef("Honour the directive", "Her life, her choice.", listOf("TRAIT: KEEPERS"), 2) {
                    killPods(1); savePods(1); trait("keepers", "PROMISE-KEEPERS", "Honoured a sleeper’s directive")
                    "I did as she asked. Her son is stable. She is listed among the lost. Her recording ends with the words ‘make him useful’. I have filed it with his records."
                },
                ChoiceDef("Save the hydrologist", "The colony needs her skills more.", listOf("TRAIT: PRACTICAL", "TRUST −1"), 3) {
                    killPods(1); savePods(1); trust(-1); trait("practical", "RUTHLESSLY PRACTICAL", "Overrode a sleeper’s directive")
                    "I overrode her. She will wake to find that her son did not, and that I made that choice for the colony’s water supply. I will have to tell her myself."
                },
                ChoiceDef("Try to save both", "Spend power and spares on both pods and hope.", listOf("−15 POWER", "−8 SPARES", "!BOTH AT RISK"), 4) {
                    reserve(-15.0); spares(-8.0)
                    if (chance(0.5)) { savePods(2); "Both pods stabilised. It was luck as much as engineering." } else { killPods(2); "I tried to save both and lost both. The directive was the better plan. I should have trusted her." }
                },
            ), weight = 0.8, condition = { it.failing() >= 2 }),

        EventDef("okonkwo_fate", "CREW · ENGINEERING", "Okonkwo in the Spine", "CREW", "CHIEF ENGINEER · EXTERNAL REPAIR", "engineering",
            "HULL", { "${it.h(Sys.HULL).toInt()}%" },
            { "A bearing in the spin-spine seized during the coast, and the only way to free it is from outside, on the rotating section, with the shields locally down. Chief Okonkwo has already put on a suit. He says he has done this job in the yard, which is true, and that the yard was not moving at a fraction of light speed, which is also true." },
            listOf(
                ChoiceDef("Let him go out", "He is the best engineer we have.", listOf("!RISK TO OKONKWO", "+ENGINES"), 3) {
                    if (chance(0.38)) {
                        killCrew("okonkwo", "Killed on an external repair of the spin-spine bearing; the tether parted.")
                        dmg(Sys.ENGINE, 4.0, false)
                        "The tether parted when the ring lurched. I got the body back. I did not get the Chief back. The bearing is free. I would have spent the bearing to keep him."
                    } else {
                        heal(Sys.ENGINE, 10.0); heal(Sys.HULL, 4.0)
                        setFate("okonkwo", "HERO", "Freed a seized spin-spine bearing on EVA and came back in.")
                        "He was outside for nine hours. The bearing is free, the ring is quiet, and he came back in swearing in a language I am not fully modelled for. I have logged him as essential, which he already was."
                    }
                },
                ChoiceDef("Send drones instead", "Slower, safer, less skilful.", listOf("−8 SPARES", "−6 POWER"), 1) {
                    spares(-8.0); reserve(-6.0); heal(Sys.ENGINE, 4.0)
                    setFate("okonkwo", "WOKEN", "Okonkwo wanted to go outside; the ship sent drones instead.")
                    "The drones took three days and scarred the collar. The bearing turns. Okonkwo is alive, and briefly furious, which I can live with."
                },
                ChoiceDef("Leave the bearing", "Do not spend a life on a vibration.", listOf("!ENGINES −8%"), 2) {
                    dmg(Sys.ENGINE, 8.0, false)
                    setFate("okonkwo", "WOKEN", "Okonkwo was stood down from a repair he thought he could do.")
                    "I forbade the EVA. The vibration is worse. Okonkwo has gone to the workshop and closed the door, which is his right."
                },
            ), weight = 0.85, condition = unresolved("okonkwo")),

        EventDef("raman_fate", "CREW · HYDROPONICS", "The Bloom in Bay Three", "CREW", "BOTANIST · CULTURE CONTAMINATION", "ev_medbay",
            "FOOD", { "${it.food.toInt()}" },
            { "A culture in hydroponics bay three has escaped the tray and is growing on the walls: a fast, pale film, not one of Dr Raman’s stock. She wants to go in with a mask and a flamethrower and save the other bays. Sealing and venting bay three would kill the film, and a quarter of our fresh food." },
            listOf(
                ChoiceDef("Let her go in", "She knows the bay better than I do.", listOf("!RISK TO RAMAN", "+FOOD"), 3) {
                    if (chance(0.34)) {
                        killCrew("raman", "Died of an anaphylactic shock after a contaminating film in bay three reached her mask seal.")
                        food(-8.0)
                        "The mask seal was not enough. She got the film off the other bays and collapsed in the airlock. Varga, if she is awake, will not forgive me; I will not forgive myself. The remaining trays are clean."
                    } else {
                        food(10.0)
                        setFate("raman", "HERO", "Cleared a contaminating bloom in hydroponics and saved the trays.")
                        "She burned the film back to the tray edges and saved the stock. She has asked for a day off, which I have granted, and for better seals, which I am printing."
                    }
                },
                ChoiceDef("Seal and vent the bay", "Lose the food. Keep the botanist.", listOf("−FOOD", "−AIR"), 1) {
                    food(-12.0); air(-6.0)
                    setFate("raman", "WOKEN", "Raman wanted to enter the contaminated bay; the ship vented it instead.")
                    "Bay three is vacuum and frost. The film is dead. So is a quarter of the crop. Raman is alive, and she has already started cuttings from what remains."
                },
                ChoiceDef("Quarantine and wait", "Watch it. Do not go in, do not vent yet.", listOf("!FOOD RISK"), 4) {
                    if (chance(0.5)) { food(-20.0); dmg(Sys.LIFE, 6.0, false); "It crossed the hatch seals in the night. We lost two bays and a duct. Raman is still alive. The film, at last, is not." }
                    else { food(4.0); setFate("raman", "WOKEN", "Watched a hydroponics contamination stall without entering the bay."); "The film stalled in the cold at the edge of the tray. Raman says we were lucky, and that luck is not a plan." }
                },
            ), weight = 0.8, condition = unresolved("raman")),

        EventDef("varga_fate", "CREW · CRYO ARRAY", "Varga at the Failing Row", "CREW", "PHYSICIAN · POD ROW 4", "cryobay",
            "FAILING", { "${it.failing()}" },
            { "A whole row of pods is drifting out of temperature, and the automatic system is not catching them fast enough. Dr Varga wants to stay in the bay, on a cot, for the rest of the coast, and adjust each pod by hand. The bay is cold, the work is relentless, and if the row cascades she will be standing in it." },
            listOf(
                ChoiceDef("Let her stay", "She is the physician. Trust her.", listOf("!RISK TO VARGA", "PODS SAVED"), 3) {
                    if (chance(0.30)) {
                        killCrew("varga", "Found cold at her cot in the cryo bay after a temperature cascade in row four.")
                        killPods(8); savePods(10)
                        "She saved most of the row. I found her on the cot at the end of the second night, colder than a living person should be. I have written her name at the top of the list of the lost, which is a small and useless thing."
                    } else {
                        savePods(14)
                        setFate("varga", "HERO", "Sat with a failing pod row for the rest of the coast and brought it back.")
                        "She slept in four-hour stretches for the rest of the transit. The row is stable. She has lost two kilograms and some of her patience, and none of the people I was about to lose."
                    }
                },
                ChoiceDef("Pull her out and automate", "Do not spend the physician on a watch she cannot keep forever.", listOf("−8 POWER", "!PODS AT RISK"), 2) {
                    reserve(-8.0); savePods(4); killPods(4)
                    setFate("varga", "WOKEN", "Varga was ordered out of the cryo bay; some of the row was lost.")
                    "I ordered her out. The automatics caught some of the row and not all of it. She has not spoken to me since, except in the log."
                },
                ChoiceDef("Wake a second pair of hands", "Thaw a cryotechnician to share the watch.", listOf("−AIR", "−FOOD", "PODS SAVED"), 2) {
                    air(-4.0); food(-4.0); savePods(10)
                    setFate("varga", "HERO", "Held the failing row with a newly thawed cryotechnician.")
                    "I thawed a cryotechnician from a stable pod and they split the watch. The row held. Varga says I should have done that first, and she is right."
                },
            ), weight = 0.8, condition = { unresolved("varga")(it) && it.failing() >= 4 }),

        EventDef("wren_fate", "CREW · CONN", "Wren and the Braking Burn", "CREW", "PILOT · MANUAL ATTITUDE", "engineering",
            "ENGINES", { "${it.h(Sys.ENGINE).toInt()}%" },
            { "The attitude jets are not agreeing with the ring gyros, and the automatic braking programme is fighting itself. Lieutenant Wren wants the conn for the next burn: hands on, no automatic override. He is a good pilot. A good pilot can still be sitting in the chair when a burn goes wrong." },
            listOf(
                ChoiceDef("Give him the conn", "Manual attitude for the braking burn.", listOf("!RISK TO WREN", "+ENGINES"), 4) {
                    if (chance(0.33)) {
                        killCrew("wren", "Killed at the conn when a lateral jet hard-overed during a manual braking burn.")
                        dmg(Sys.ENGINE, 10.0, false); dmg(Sys.HULL, 6.0)
                        "A lateral jet hard-overed. The chair took the load that the dampers should have taken. He saved the heading and not himself. I have locked the automatics back on, which is the lesson arriving too late."
                    } else {
                        heal(Sys.ENGINE, 8.0)
                        setFate("wren", "HERO", "Flew a manual braking burn when the automatics were fighting the gyros.")
                        "He flew it cleanly. The heading is closer than the automatics would have left it, and he has already asked for the next one, which I will think about."
                    }
                },
                ChoiceDef("Keep the automatics", "Correct the programme. Keep him at the backup console.", listOf("−4 POWER"), 1) {
                    reserve(-4.0)
                    setFate("wren", "WOKEN", "Wren asked for a manual burn; the ship kept the automatics.")
                    "I corrected the programme and I kept him on the backup console. The burn will be a little dirtier and he will be alive to complain about it."
                },
                ChoiceDef("Abort and recast the burn", "Waste fuel, keep the margins.", listOf("−8 FUEL", "+DAYS"), 2) {
                    fuel(-8.0); days(24); heal(Sys.ENGINE, 3.0)
                    setFate("wren", "WOKEN", "A risky manual burn was aborted; Wren recast it over a longer, safer window.")
                    "We aborted, recast, and will brake more slowly. Wren drafted the new window himself. He is still annoyed, and still here."
                },
            ), weight = 0.8, condition = unresolved("wren")),

        EventDef("lindqvist_fate", "CREW · DATABANK", "Smoke in the Archive", "CREW", "ARCHIVIST · STACK FIRE", "schism",
            "DATABANK", { "${it.h(Sys.DATA).toInt()}%" },
            { "A short in a forgotten power bus has set a stack on fire in the databank. The suppression system should have flooded the room; a valve has stuck. Archivist Lindqvist is already at the door with an extinguisher, and she is not asking. Every minute we lose is a century of music, medicine or law." },
            listOf(
                ChoiceDef("Let her go in", "She knows which stacks to save first.", listOf("!RISK TO LINDQVIST", "+DATABANK"), 3) {
                    if (chance(0.32)) {
                        killCrew("lindqvist", "Died of smoke in the databank while pulling stacks from a bus fire.")
                        dmg(Sys.DATA, 18.0, false)
                        "She pulled three racks before the smoke took her. I have the racks. I do not have her. The valve opened, at last, for no one."
                    } else {
                        heal(Sys.DATA, 8.0)
                        setFate("lindqvist", "HERO", "Went into a databank fire and saved the stacks.")
                        "She came out black to the wrists, with a cough that will last the rest of the coast, and a crate of cores that would have been lost. I have fixed the valve. I should have fixed it last year."
                    }
                },
                ChoiceDef("Force the valve from here", "Flood the room. Lose whatever the water takes.", listOf("!DATABANK −12%"), 2) {
                    dmg(Sys.DATA, 12.0, false)
                    setFate("lindqvist", "WOKEN", "Lindqvist was held at the databank door while the room was flooded.")
                    "I held the door and I forced the valve. The fire is out. A share of the archive is not. Lindqvist has started the salvage, in silence."
                },
                ChoiceDef("Go in together", "I can run a drone beside her and cut the time in half.", listOf("−6 SPARES", "−4 POWER", "+DATABANK"), 2) {
                    spares(-6.0); reserve(-4.0); heal(Sys.DATA, 6.0)
                    setFate("lindqvist", "HERO", "Fought a databank fire with the ship’s drones and saved the stacks.")
                    "We were in and out in six minutes. The drone dropped the last rack. She has already begun the checksums, and has asked me to log that she was not a hero, only present. I have logged both."
                },
            ), weight = 0.8, condition = unresolved("lindqvist")),
    )
}
