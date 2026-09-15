# BRASEIRO WWN — M4 ARCHITECTURE DECISION RECORD

STATUS: ACCEPTANCE_PENDING_GUIDE

| ADR | Decision | Selected | Rejected/Deferred | Reason / consequence |
|---|---|---|---|---|
| ADR-001 | Player UI | Local WebView HTML/CSS/ES modules | fully native Compose; remote hosted app | Preserves Forge/DOM geometry workflow and Android final packaging; offline UI works. |
| ADR-002 | JS framework | No mandatory framework initially | React/Vue/Svelte as hard dependency | Fewer moving parts, direct geometry control, faster visual auditing. Framework can be introduced only with measured benefit. |
| ADR-003 | Canonical persistence | Native Room/SQLite through narrow bridge | localStorage; two production DBs; cloud-only DB | Strong transactions, migrations, backup/lifecycle safety; WebView storage is not campaign authority. |
| ADR-004 | State structure | Versioned aggregate + append-only events + snapshots | pure mutable tables only; full event sourcing only | Gives replay/audit/recovery without forcing every query through event replay. |
| ADR-005 | Mechanical runtime | Pure JS domain/rules modules | rules in DOM handlers; duplicated Kotlin rules | Browser-testable and one mechanical authority. |
| ADR-006 | RNG | Versioned seeded deterministic streams | `Math.random()` | Replayable dice/procedural state and deterministic QA. |
| ADR-007 | GM model | Provider adapter separated from rules/state | LLM as rules engine | Model can narrate/interpret but cannot silently change mechanics. |
| ADR-008 | RAG baseline | Local source catalog + SQLite FTS metadata; model adapter consumes cited evidence | paid vector DB requirement | Offline/free baseline and explicit evidence. Embeddings may be optional later. |
| ADR-009 | Map geometry | Axial HexGeometryV1 + camera transform | per-cell pixel coordinates/nudges | Equal-sided hexes, deterministic distance/adjacency, pan/zoom without state corruption. |
| ADR-010 | Visual authority | Forge geometry + real runtime capture/diff | generated screenshot as runtime; eyeballing | Matches user-approved deterministic composition workflow. |
| ADR-011 | Backup | Logical portable ZIP + temp-DB verification | raw copy overwrite | Safer migrations and state comparison; original preserved on failure. |
| ADR-012 | Audio | Native Android TTS/audio bridge with session controller | browser-only TTS as sole production path | Better lifecycle/STOP reliability; narration text remains authority. |
| ADR-013 | Secrets | Android secure preference/keystore-backed storage | campaign JSON or WebView localStorage | API/provider keys never enter backup/game state. |
| ADR-014 | Development QA | Playwright-style 415×915 capture + Android instrumentation | user physical phone as routine debug loop | Faster deterministic iteration; physical device remains acceptance gate. |
| ADR-015 | Dependency policy | local/offline/free where sufficient; pin versions | paid service as mandatory infrastructure | Keeps project portable and buildable from Android Studio package. |

## Deferred decisions

- Exact model provider and API protocol: deferred to Wave 8; adapter contract is fixed now.
- Semantic embedding model: optional; FTS evidence path must work without it.
- Final Room table/index tuning: M5 implementation concern; state ownership and transactional semantics are already locked.
- Flat vs pointy visual hex orientation: versioned per map metadata; axial canonical coordinates are orientation-independent.

## Non-negotiable prohibitions

- no OSE/D&D/Pathfinder/SWN/Godbound mechanical fallback;
- no second persistence authority in WebView;
- no UI-owned HP/position/time truth;
- no model-generated mechanical commit without rules validation;
- no whole-screen fake UI;
- no non-proportional stretch;
- no false Visual Lock;
- no destructive read/migration fallback to empty campaign.
