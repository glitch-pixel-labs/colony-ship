# Changelog

All notable changes to Colony Ship. Versions follow semver.

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
