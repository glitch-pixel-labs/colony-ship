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
    )
}
