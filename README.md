# Colony Ship (Android)

You are the ship's AI, carrying 1,000 frozen colonists to a new world. You get more frazzled the more things break. Kotlin and Jetpack Compose, minSdk 26, targetSdk 34, portrait only.

Build (on the box):

    export JAVA_HOME=/home/box/jdk17 ANDROID_HOME=/home/box/android
    ./gradlew testDebugUnitTest assembleDebug

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Screenshots: the Robolectric/Roborazzi tests write them to `app/build/shots/`. Copies live in `/workspace/colonyship/screens/v0.1/`.
- Audio generators: `/workspace/colonyship/audio/music.py` and `sfx.py`.

All words, art, music, sound and code are original. Fonts are used under the SIL OFL.
Only debug builds are made here. Releases and signing need Saul's approval.
