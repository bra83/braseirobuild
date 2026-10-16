# Fresh individual regression - 2026-10-16

- Command: `python work/star-wars/run_fresh_tests.py`
- Scope: all 376 `tests/*.test.mjs` files, individually invoked with `node --test` (370 regular files and six stress files).
- Result: 3,393 tests; 3,391 passed; zero failed; two skipped. All 376 processes exited successfully and all 376 produced complete summaries.
- The two skips are in `gradle-wrapper-bootstrap.test.mjs`: offline wrapper packaging and wrong-SHA rejection require the JDK `jar` executable, unavailable in this environment. The reason is embedded in the TAP output. No Gradle build or APK/device gate was run.
- Raw per-file TAP logs and machine summary are stored alongside this report. The earlier run had a stale test assertion and a parser mismatch; it is retained outside this evidence folder and is superseded by this run.
