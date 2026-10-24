# Voxora Studio

**Your Voice. Your Studio.** A professional Android vocal recording studio by Nexauren.

Voxora Studio is an offline-first Android app for singers to record, review, and manage vocal takes. The first implementation milestone includes a Compose interface, bilingual resources, microphone permission handling, real recording/playback, and a local takes library.

## Included in this foundation

- Kotlin and Jetpack Compose with Material 3.
- Dark studio visual system with violet and cyan accents.
- Portuguese (Portugal) and English resources; language can be changed in Settings.
- Real microphone recording to AAC/M4A using Android's MediaRecorder.
- Local playback and deletion of recordings stored in app-private storage.
- Android CI workflow that compiles the app and runs unit tests.

## Planned milestones

1. Project foundation and recording workflow.
2. Audio editing: trim, gain adjustment, and WAV export.
3. Vocal processing: EQ, echo, reverb, and safe presets.
4. Device testing, accessibility, privacy documentation, and release preparation.

Only features implemented in the app are presented as functional. Effects and editing are clearly marked as coming soon until their audio processing is built and tested.

## Technology

- Kotlin, AndroidX, Jetpack Compose, Material 3
- MVVM-friendly state management
- Android MediaRecorder and MediaPlayer for the first recording workflow
- App-private file storage; no account or backend required

## Build

Requirements: Android Studio with Android SDK 35 and JDK 17. The CI workflow installs Gradle 8.9 and runs gradle assembleDebug testDebugUnitTest. If building locally without a Gradle wrapper, use a local Gradle 8.9 installation or generate a wrapper from Android Studio.

Open the repository root as a Gradle project, sync dependencies, then run the app configuration on an Android device or emulator. Recording requires microphone permission.

## Privacy

Recordings are stored in the app's private files directory. This initial version does not upload audio. Uninstalling the app removes its private recordings unless the user exports them first.

## Status

Early development — the recording foundation is implemented, but audio editing and vocal effects are not yet available.
