SWE02 P240 Nabat repair follow-up — full per-file regression
Date: 2026-10-16 (America/Sao_Paulo)

Result: 382 files executed individually (376 regular + 6 stress); 3,458 tests, 3,456 passed, 0 failed, 2 skipped. The skipped checks are offline Gradle-wrapper packaging checks because the JDK `jar` executable is unavailable.

Focused suites: P240 Nabat 15/15; EotE vehicle criticals 18/18. The ship repair is exercised against real damage state and persisted through three controller reloads. The invalid destroyed-ship case rejects atomically. Machine-readable summary and raw TAP logs are in this directory. No APK, CI, Gradle release or device gate was run.
