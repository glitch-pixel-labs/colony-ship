# Changelog

All notable changes to Colony Ship. Versions follow semver.

## [0.1.0] - 2026-11-14 (debug build, unreleased)

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
