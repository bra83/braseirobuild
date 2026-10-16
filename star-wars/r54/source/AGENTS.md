# Star Wars VTT execution constraints

- P244 is the immutable source base. Apply only the hash-verified cumulative WIP5 MERGED overlay; it remains non-canonical until acceptance gates pass.
- Limit changes to Star Wars R54. Preserve other VTTs and every unrelated mono-repository path.
- Exactly five adventures are in scope: Long Arm of the Hutt, Under a Black Sun, Beyond the Rim, The Jewel of Yavin, and Mask of the Pirate Queen. Do not import other adventures.
- Do not create P245 or run Gradle, APK, CI, or device gates before all five adventures and rules have semantic certification.
- Deterministic code alone owns mechanics and state. RAG retrieves cited source text; it does not apply effects. Gemini and the Barbara narrator may narrate or propose but cannot bypass deterministic validation.
- Editorial evidence names edition, PDF and printed page, rule/scene, executor, controller/UI path, persistence/reopen evidence, and test. Missing evidence stays PENDENTE.
- AoR has 360 individual consumer positions. Aggregate flags and structural tests certify no individual position; maintain per-position source-to-effect evidence.
- Preserve Android assets, Java bridges, saves, migrations, rollback, offline behavior, TTS, images, and audio. Never add API keys, keystores, licensed PDFs, node_modules, APKs, unrelated backups, or other VTT content to Git.
- Run every test file individually and all six stress files. Preserve raw logs; record exit codes, counts, and skips accurately.
- Push recoverable WIP to `codex/star-wars-hibrido-integral-20261016`; never force-push. Record local and remote SHAs.
- Use only `CERTIFICADO`, `IMPLEMENTADO_NAO_CERTIFICADO`, or `PENDENTE`. Presence in a graph or catalog is not certification.
