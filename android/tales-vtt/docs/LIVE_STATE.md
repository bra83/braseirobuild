# TFTL VTT — LIVE STATE FINAL SOURCE

Date: 2026-09-08
Baseline: `braseiro-tales-vtt(2).zip` (AI Studio shell)
Package: `com.braseiro.talesvtt`
Visual baseline: **APPROVED / LOCKED**

## Current state

- 13/13 screens/states present in Compose.
- Real asset layer incorporated; all referenced drawables resolve.
- ENCRENCA remains the frozen approved mechanical/visual authority.
- Approved 13-screen source-parity preview is stored at `docs/VISUAL_BASELINE_APPROVED_13_SCREENS.html`.
- MAPA: visual aprovado em 2026-09-08; base limpa + 1 ícone canônico por POI; `VISUAL_LOCKED_BY_USER`.
- Footer user-approved lock: 7 approved cyan line icons, 61 dp shell, no legacy nav PNG refs in Android.
- Internal screens preserve the correct parent tab: COTIDIANO/ENCRENCA/CLÍMAX/LOCAL→CENA; CONEXÕES/ENCERRAMENTO→MISTÉRIO; BIBLIOTECA→MENU.
- Motor Barbara is isolated behind typed actions + firewall; transport never writes GameState directly.
- Player/GM visibility firewall remains enforced.
- Save/autosave and GM-private snapshot remain separated.
- Package identifiers cleaned to `com.braseiro.talesvtt`.

## Final mechanical closure

- Standard tests, Luck, Push, Pride, Help, Conditions/Broken, Items, XP and Change are deterministic.
- Extended Trouble now supports current-Kid scene start, linked ENCRENCA test, final-result handoff, verified external Kid result, exact Compromise cost, decline, and safe close.
- Session end and Mystery end are separate states.
- Session end does not heal Conditions.
- Mystery end heals Conditions.
- Birthday is explicit, +1 Attribute max 5, age +1, Luck max follows `15-age`, and age 16 requires a new Kid.
- Snapshot codec version 8 preserves all new state.

## Validation

- Pure deterministic rules/firewall regression: **113 PASS / 0 FAIL**
- Static source audit: **PASS**
- 13/13 screens
- 0 missing drawable references
- 0 hidden `???`
- 0 tactical radar / HP / initiative / encumbrance
- 0 demo triggers
- 0 active placeholder screens
- 0 old package identifiers
- Android Auto Backup disabled


## Scene illustration library

- Current integrated scene library: **63 individual PNGs**.
- Web and Android copies: **63/63 byte-identical by SHA-256**.
- Web asset catalog coverage: **63/63 scene assets**.
- Legacy selector regression: **48/48 PASS** (24 canonical contexts × Tales/Flood).
- Expanded scene reachability: **78/78 PASS** (39 added contexts × Tales/Flood).
- No medieval/fantasy misgeneration is part of the active scene library.
- Target remains 100 scenes; **37 future images are optional expansion and are not a blocker for current implementation**.

## Final closure

- Primary playable runtime: **Tales/Kids**.
- Flood/Teens core: mechanically isolated and regression-tested, but intentionally not routed into the main Android navigation until a complete profile router exists.
- Scene library: 63 active images; future expansion to 100 remains optional.
- No approved visual lock was changed during closure.

## Runtime boundary

Android/Gradle execution is not available in this environment because the wrapper cannot download Gradle/dependencies without network access. No APK or Android device PASS is claimed here. Final deliverable is the Android Studio source project.
