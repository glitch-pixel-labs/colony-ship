# Colony Ship (Android)

You are the mind of a relativistic colony ship carrying 1,000 vitrified colonists away from an overcrowded Earth. You allocate power, decide who to wake and who to let go, burn for candidate stars, and choose where to settle. Hard science fiction: special-relativistic travel, real time dilation, consequences that stick. Kotlin and Jetpack Compose, minSdk 26, targetSdk 34, portrait only.

Releases are published at <https://github.com/glitch-pixel-labs/colony-ship/releases>. To install with [Obtainium](https://github.com/ImranR98/Obtainium), add that releases URL.

Build (on the box):

    export JAVA_HOME=/home/box/jdk17 ANDROID_HOME=/home/box/android
    ./gradlew testDebugUnitTest assembleDebug assembleRelease

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Signed release APK: `app/build/outputs/apk/release/app-release.apk` (copied to `/workspace/colonyship/ColonyShip-v0.3.0.apk`). The release keystore lives outside the repo at `/workspace/colonyship/keystore/` and is never committed.
- Screenshots: the Robolectric/Roborazzi tests write them to `app/build/shots/`. Copies live in `/workspace/colonyship/screens/v0.3/` (earlier shots in `screens/v0.2/` and `screens/v0.1/`).
- Paintings: originals in `art-src/` and `art-src/v2/`. Bundled as WebP in `res/drawable-nodpi/art_<key>.webp`, looked up by key at runtime.
- Audio generators: `/workspace/colonyship/audio/music2.py` (seamless 64 s loop plus a 6 s tail for crossfading) and `sfx.py`.

## Code map
- `game/Physics.kt`: boost–coast–brake transits at 0.5 g. It computes proper time and Earth time, the Lorentz factor, position from Sol and light-lag.
- `game/EventsSpace.kt`, `EventsShip.kt`, `EventsCrew.kt`, `EventsDeep.kt`, `EventsCrisis.kt`, `EventsInsight.kt`: space, ship, crew, deep-space mysteries, forced-landfall crises, and delayed artefact payoffs.
- `game/Endings.kt`: history-book prose, and which paintings illustrate it. Plates are chosen by biome and ending, with no repeats in a book.
- `ui/StoryScreen.kt`: the prologue. `ui/TransitOverlay.kt`: the relativistic transit sequence. `ui/ChartScreen.kt`: stellar cartography. `ui/StatusUi.kt`: the pinned fuel/power/hull ribbon. `audio/MusicLooper.kt`: the two-player crossfading loop with fades.

All words, art, music, sound and code are original. Fonts are used under the SIL OFL.
