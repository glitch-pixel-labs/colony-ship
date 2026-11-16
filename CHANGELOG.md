# Changelog

All notable changes to Colony Ship. Versions follow semver.

## [0.3.0] - 2026-11-16

Shorter, higher-stakes voyages, a clearer ship, and rarer wonders — from Saul's playtest of 0.2.1 and later notes.

### Added
- **Fuel:** a real propellant reserve (boost and brake), shown in a pinned status ribbon with power, hull and colonist counts. Typical burns cost 8–15; a voyage is now about 8–12 jumps, not a long wander.
- **Burn risk** is shown before each departure, and rises with jump count, wear and a thinning tank.
- **Ship systems console:** nine colour-graded modules (scanners, engines, hull, power core, cryo, landing bay, databank and the rest) with percentages, a pulsing red state when a system is in the red, and a damage log that states the failure and what it does to the game.
- **Planets** now range from rare Earth-analogues to genuinely hellish worlds (tidally locked furnaces, crushing atmospheres, sterilising radiation, acid seas, rogue worlds). The survey states a clear verdict.
- **Woken passengers** are named, and their fates are resolved in later events and in the history book.
- **Deep-space mysteries:** more than ten new hard-SF signal and anomaly events, far from Sol. Human artefacts (the 2080s probe, the overtaking ship) now only appear near the Sun. A letter from Earth can still catch up anywhere; it is a delayed transmission, not a found relic.
- **Tabby's Star** (KIC 8462852, Boyajian's Star): a named rare encounter. A distant-photometry beacon can offer a 1,470-light-year detour to Cygnus. The fuel cost is a boost and a brake, not a 1,470-unit drain; the real price is centuries of coast and wear. Arrival reveals a shattered ancient Dyson swarm as the cause of the irregular dips (up to ~22%). The builders are not explained. Salvage, survey or pass by; the history book remembers.
- **Stellar cartography:** a 70s star chart, opened from the console, with Sol at the origin, the route as a line, notable stops (signals, Tabby's Star, a recommended world, a forced landfall, the settled planet) marked, and tappable nodes showing name, distance from Sol, Earth year of arrival and ship time. The history book includes a map page of the voyage.
- **Forced landfall:** rare catastrophic failures (reactor containment, cascading cryo, hull fatigue, coolant loss) whose chance rises with jump count and wear. A red alert and a strong haptic; you must settle at the next world, or risk one more burn that can kill the colony or the ship. The history book calls it a forced landfall. It happens in a minority of runs.
- **Alien data that pays off:** reading a derelict's memory (and several other signals) starts a databank reduction that finishes a jump or two later. The result is one of: coordinates to a recommended world (sometimes a genuine paradise, sometimes a graveyard or a world favoured by alien criteria, with clues before you divert); a procedure that repairs or upgrades a system; or a warning that can later match a real failure, if you prepared. The history book records what you learned, and what you did with it.
- Longer, more detailed history-book chapters, tying together the planet, the woken crew, sacrifices, the ship's state, and the AI's standing.

### Changed
- Legs between stars are shorter (typically a few light years). Wear, event severity and pod failure all rise with each burn and with ship age.
- Image and video prompt buttons are smaller, in a compact row under pictures.
- British English and hard-SF tone unchanged.

## [0.2.1] - 2026-11-14

Haptics fix, from playtesting 0.2.0 on a Samsung Galaxy A54.

### Changed
- **Haptics on every press:** all clickable controls now go through a shared `hapticClick` modifier (or an explicit `play()` in tap gestures, back navigation and dialog dismiss), so a new button cannot ship without a buzz.
- **Stronger, Samsung-safe clicks:** button, tab, toggle and tick haptics now use the Vibrator with a 28–40 ms one-shot at default amplitude. The old `VibrationEffect.createPredefined(EFFECT_CLICK / TICK)` path is gone — Samsung often swallows those when system "touch vibration" is off, which is why many presses felt like they did nothing.
- If the vibrator is missing, the code falls back to `View.performHapticFeedback` with a strong constant. The in-game haptics toggle is still respected and still defaults to on.
- Sliders tick while dragging and click on release (power channels and volume). Tapping a plate, attached picture, history-book page, legacy list item, skip/next, dialog dismiss and the system back key now also buzz.

## [0.2.0] - 2026-11-14

First signed release, for GitHub / Obtainium. This version responds to Saul's feedback on 0.1.0.

### Added
- **Prologue:** six painted panels on every New Run, each with original text: why humanity launched, the ship, the 1,000 sleepers, departure and time dilation, the AI's role, and the mission.
  - Tap to advance, with a haptic tick on each page.
  - SKIP is available at any time. The prologue can be replayed from Settings.
- **Relativistic travel:** each transit is a 0.5 g boost, a coast and a 0.5 g brake.
  - Cruise speed comes from engine health and power: about 0.82c when healthy, plus a little more if Lt Wren is awake.
  - Ship time and Earth time are computed separately using the Lorentz factor.
  - The ship tracks its position from Sol in light years.
- **Navigation panel:** shows the Earth date (AD), distance from Sol, the year a message sent now would reach Earth, ship years, and last peak β and γ.
  - The header shows light years and the Earth year.
  - The log, archive, event outcomes and history book are all dated in Earth years.
- **Transit sequence:** a high-resolution Canvas animation, replacing the old warp effect.
  - Star positions use special-relativistic aberration. Colour and brightness use the Doppler factor: the sky bunches and blueshifts ahead.
  - Streaks show the interstellar medium hitting the ship. The ship is seen from behind, with the drive glow moving to the bow while braking.
  - A HUD shows live β, γ, the ship clock and the Earth date. Haptic cues mark coast and braking. Tap to skip.
- **54 new paintings:** landfall by biome (ocean, ice, desert, jungle, tidally locked, high gravity, toxic, volcanic, storm, temperate/ringed, crash), mid-history scenes, thousand-year legacies, AI fates, event scenes and prologue panels.
  - History-book plates are chosen from the planet's true stats and the ending, and never repeat within a book.

### Changed
- **Writing:** every event and every ending was rewritten from scratch.
  - There are now 42 events, up from 32, in hard SF with real consequences: devitrification, proton storms, bow erosion, cryoprotectant toxicity, light-lag, value drift in the AI, and more.
  - The AI's voice is dry and serious, with no quips. Moods are operational (NOMINAL … LOSING THE SHIP).
  - Scan readouts are factual.
  - British English throughout.
- **Music:** the loop has been regenerated so it is seamless. Drone frequencies fit whole cycles per loop, filters are circular, and a 6 s tail is appended.
  - Playback uses two players that crossfade over identical audio.
  - The music fades in over 3 s on start and fades out before pausing.
- **Art:** all paintings converted to WebP (67 images, 9.9 MB), looked up by key at runtime.
- The version is now 0.2.0 (versionCode 2).

### Removed
- "Jump drive" and "warp" language, and the pixel-style warp animation.

## [0.1.0] - 2026-11-14

First playable build for the Samsung Galaxy A54 (Android 14), portrait only.

### Added
- Title screen: painted 70s-style cover art with slow drift, twinkling stars and a glowing title. New Run, Continue, Archive and Settings.
- Ship status console:
  - External ship camera with live callouts.
  - AI mood line.
  - A 1,000-pod cryo grid with nixie counter.
  - A 100-unit power budget across five channels, with a reserve dial and under-power warnings.
  - Six system health gauges, consumable tanks and the awake crew list.
- Travel loop with 32 original events, each with 2-3 choices, cost tags and a risk meter.
  - Waking named specialists costs trust.
  - Systems wear over time, and under-powered systems degrade.
  - A brownout happens when the reserve runs dry.
- Planet scan:
  - Procedurally generated worlds.
  - Readouts get unreliable as the scanners are damaged, down to "SCANNER DAMAGED ??".
  - Re-scan costs power (up to 5 passes).
  - Settle or keep searching.
- Endings:
  - Nine ending types, decided by planet stats, traits from your choices, trust and survivors.
  - Eight possible fates for the AI.
  - Each ending is a 3-4 chapter illustrated history book with a timeline.
- "Copy image prompt" and "Copy video prompt" on every event, chapter and log entry. Each copies to the clipboard and offers the share sheet.
- Attach a picture to any event, chapter or log entry with the system Photo Picker. The picture is copied into app storage and opens full screen with pinch-to-zoom.
- Legacy archive of finished runs and the current ship's log.
- Bottom navigation: Ship / Events / Scan / Archive.
- Automatic save and resume after every decision.
- Haptics:
  - A light tick on buttons, sliders and toggles.
  - Heavier patterns for impacts, alarms, pod losses, jumps (rumble) and settling or endings.
  - Uses VibrationEffect, with fallbacks for older APIs.
  - On/off toggle in Settings.
- Audio:
  - An original procedurally synthesised ambient loop, about 64 s long.
  - Eight original synthesised sound effects.
  - Music and SFX volume sliders.
- All settings persist between sessions.
