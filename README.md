# Voxora Studio

**Your Voice. Your Studio.**  
A professional Android vocal recording studio by Nexauren.

Voxora Studio is being built as an offline-first Android app for singers to record, review, organize, and export vocal takes. The first milestone focuses on a stable foundation and real recording workflows; audio effects and advanced processing will be added incrementally and tested on-device.

## Product principles

- **Local-first:** recordings stay on the device unless the user explicitly exports or shares them.
- **Bilingual:** Portuguese and English, with Android system-language support and an in-app language setting planned.
- **Singer-focused:** fast access to recording, takes, and projects.
- **Honest controls:** only show audio features once the underlying processing has been implemented.
- **Accessible:** readable contrast, touch-friendly controls, and proper permission explanations.

## Planned milestones

1. Android project foundation, app theme, navigation, and bilingual strings.
2. Microphone permission flow and real recording/playback.
3. Local recordings library and safe file management.
4. Editing tools: trim, gain adjustment, and WAV export.
5. Vocal processing: EQ, echo, and reverb, followed by device testing.
6. Release hardening: accessibility, privacy documentation, and build verification.

## Technology

- Kotlin
- AndroidX / Jetpack Compose
- Material 3
- MVVM-style UI state
- Android audio APIs for recording and playback
- Room and DataStore when persisted metadata and preferences are introduced

## Development

Open the project root in Android Studio and allow Gradle sync. The Android project files will be added in the initial implementation commits.

## Status

🚧 Early development. Features are added in small, testable milestones; this repository should not yet be considered a release-ready application.
