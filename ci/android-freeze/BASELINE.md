# Braseiro DnD 6.6.2 — frozen Android compile baseline

Status: candidate frozen baseline prepared locally before the single GitHub diagnostic/build attempt.

Package: `com.braseiro.dnd5e`

Frozen source archive:
- SHA-256: `2acf32fe57437c5d2472f7a65104791828a5305f44fb722ee545af0a01a52937`
- Top-level directory: `BRASEIRO_DND_6.6.2_ANDROID_STUDIO/`
- Android root: `BRASEIRO_DND_6.6.2_ANDROID_STUDIO/android/`

Frozen chunks:
- P00 `7799a6d20fa5b0f362cf4ab386139c31130b5807da44afd4841b9b6bb8fe2ac6`
- P01 `06864dd74d1c3ef62a2010210cbd63275bd3046476d7c1e1af3b15ed6bdeb61d`
- P02 `86534362f7892834b7de98e154a6d122bf7e73f95cb2d6a80d8c2884c9434ebc`
- P03 `8ba61dc700efc75aa1499621cd502ed46bab5e0f59952850aad5c89af3f5d7d5`
- P04 `c3984239c2db218db020180c71a67069276abe6c0ea8f8085a0c1cee0131e5fc`

Local preflight before GitHub:
- project/settings/build/wrapper/Manifest present
- namespace/applicationId = `com.braseiro.dnd5e`
- 234 Java classes in active package
- active old-package references = 0
- Java syntax parser diagnostics = 0
- local no-Android-classpath javac: 0 `unreported exception` diagnostics after compile-compatibility containment
- maps/assets/gameplay content not removed

The GitHub diagnostic workflow MUST NOT mutate source. It only verifies this frozen archive in ordered gates and publishes only if all gates are green.
