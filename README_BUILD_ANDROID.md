# BRASEIRO WWN — Android Studio Build Handoff

## Project status
- SOURCE: validated in the available environment.
- BUILD_PASS: NOT_PROVEN.
- EMULATOR_PASS: NOT_PROVEN.
- DEVICE_PASS: NOT_PROVEN.
- Reason: the current execution container has no Android SDK/adb/sdkmanager and cannot resolve services.gradle.org.

## Toolchain pins
- Android Gradle Plugin: 9.4.0
- Gradle distribution: 9.6.0
- Java source/target: 17
- compileSdk: 37
- targetSdk: 37
- minSdk: 28

## Open in Android Studio
1. Extract the ZIP.
2. Open the project root containing settings.gradle.kts.
3. Use JDK 17 or a compatible Android Studio bundled JDK accepted by AGP 9.4.0.
4. Allow Gradle to download the pinned Gradle 9.6.0 distribution and dependencies.
5. Sync the project.
6. Build the debug variant and run on an emulator/device.

## Build commands
- ./gradlew help
- ./gradlew build --dry-run
- ./gradlew assembleDebug
- ./gradlew test

## Required final Android runtime verification
Exercise startup/WebView, navigation, JSON backup/restore, PDF import/RAG, TTS, scene-image SAF import, persistence/reopen, and one real Gemini narration request with a user-provided API key. No API key is bundled in this project.

## Latest validated source closure
BIG_CLOSURE_065_RECOVERY_PARITY_NATIVE_REPAIR

Available-environment QA included current active regression, JS syntax/import checks, security/UTF-8 guards, deterministic stress (33 sessions x 144 turns), SQLite contracts, and Kotlin source compile harnesses for persistence, bridge, RAG and Gemini.

## Important
The Gradle wrapper launcher is present and demonstrably invokes the pinned Gradle 9.6.0 distribution. In this container it stops only because services.gradle.org cannot be resolved. This is not claimed as BUILD_PASS.
