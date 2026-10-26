# BRASEIRO TALES VTT — FINAL CLOSURE

Date: 2026-09-08

## Product status

**SOURCE_FINAL = PASS**

Primary playable profile: **Tales from the Loop / Kids**.

The Things from the Flood / Teens mechanical core remains a separately tested expansion module. It is deliberately not exposed through the main Android navigation because the source does not yet contain a complete profile router; this prevents cross-profile contamination.

## Locked visuals preserved

- 415×915 mobile shell.
- User-approved seven-item cyan footer.
- User-approved MAPA with nine canonical POIs and one canonical icon per POI.
- Weather/header without boxed climate widget.
- KID direct portrait + JSON import/export actions.
- No new concept generation.

## Integrated scene library

- 63 individual PNG scenes in Android.
- 63/63 active scene assets unique.
- Selector legacy regression: 48/48 PASS.
- Expanded selector reachability: 78/78 PASS.
- Additional scenes toward 100 are optional, not a blocker.

## Mechanical and source QA

- Tales deterministic engine/firewall: 113 PASS / 0 FAIL.
- Flood isolated core: 46 PASS / 0 FAIL.
- Static audit: PASS, 13/13 screens, zero missing referenced drawables.
- Product feature source audit: PASS.
- User visual lock audit: PASS.
- Profile firewall audit: PASS.
- Long-run source simulation previously completed: 20 campaigns × 33 sessions = 660 sessions / 59,400 turns, STRESS_PASS.
- Aggregate R3 source stress: 170 campaigns / 2,160 sessions / 171,900 turns plus 250,000 reducer-fuzz events.

## External gate

`./gradlew` cannot download Gradle 9.3.1 in this container because external network access is unavailable. Therefore Android Studio sync/build/device execution, Gemini network calls, SAF, PDF OCR and device TTS are **not** labeled DEVICE_PASS here.

This is a limitation of this execution environment, not a hidden PASS claim. The final deliverable is the Android Studio source project for external smoke/build testing.
