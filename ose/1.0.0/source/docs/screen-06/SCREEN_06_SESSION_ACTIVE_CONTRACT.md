# BRASEIRO OSE — SCREEN_06_SESSION_ACTIVE CANONICAL CONTRACT

STATUS: EXECUTOR_PROPOSED_FOR_GUIDE_REAUDIT
VIEWPORT: 415×915 portrait
ARCHITECTURAL AUTHORITY: BRASEIRO_OSE_NARRATIVE_FIRST_SCREEN_ARCHITECTURE_V1

## 1. PURPOSE

`SCREEN_06_SESSION_ACTIVE` is the primary gameplay surface. It presents visible Master narration first, then the player reacts through one canonical `PLAYER_REACTION` channel. The screen does not expose a human-GM console and does not provide any parallel action system.

## 2. VISIBLE ELEMENT CLASSIFICATION

| Element | Classification | Source / State | Interaction |
|---|---|---|---|
| OSE logo | STATIC_ASSET | `001_logo_ose.png` | none |
| Settings gear | STATIC_ASSET | `003_icone_configuracoes.png` | global utility only |
| Scene title | DYNAMIC_STATE | `scene.title` | read-only |
| Location context | DYNAMIC_STATE | `scene.location` | read-only |
| Day/time | DYNAMIC_STATE | `campaign.time` | read-only |
| Weather | DYNAMIC_STATE | player-visible `world.weather_visible` | read-only |
| Light/resource status | DYNAMIC_STATE | player-visible party resource state | read-only |
| Master narration panel | DYNAMIC_STATE | `master.visible_narration` | scroll/read |
| TTS | STATIC_ASSET + NATIVE_UI | `OSE_SESSION_NEW_A701_icone_tts_ouvir.png` | reads visible Master narration only |
| Location illustration | STATIC_ASSET | `087_tile_corredor.png` in this concept | context only |
| Help | STATIC_ASSET + NATIVE_UI | `OSE_SESSION_A501_icone_gm_help.png` | read-only help; zero campaign/RNG delta |
| Mechanical feedback | DYNAMIC_STATE | player-visible resolution summary | read-only, post-resolution only |
| Suggested reactions | DYNAMIC_STATE | `master.suggested_reactions[]` | only prefill/structure PLAYER_REACTION |
| Reaction text field | NATIVE_UI | local draft | canonical PLAYER_REACTION input |
| Submit reaction | NATIVE_UI | submits reaction intent | does not resolve mechanics in UI |
| History/journal | DYNAMIC_STATE | player-visible journal entries | read-only inspection |
| Bottom navigation | NATIVE_UI + STATIC_ASSET icons | Session / Map / Sheet / Company | contextual navigation only |

## 3. RULES → UI CONTRACT

The Rules Engine and Master presentation layer may provide only player-visible state to this screen: scene/location context, visible time/day/weather, visible resources, visible narration, concise visible resolution feedback, and player-visible history. Hidden rolls, secret reaction/morale values, debug state, secret map information, and referee-only evidence are forbidden from the player UI.

The Rules Engine remains the sole mechanical authority. The UI renders already-resolved mechanical outcomes and never computes or commits authoritative OSE mechanics.

## 4. UI → PLAYER_REACTION CONTRACT

The only active-play campaign-advancing intent is `PLAYER_REACTION`. Three source forms are legal and converge on the same intent pipeline:

- `FREE_TEXT`;
- `MASTER_SUGGESTED_REACTION`;
- `MASTER_PROMPT_CHOICE`.

A suggested reaction chip must only populate or structure the reaction draft. It cannot directly move the party, advance time, spend resources, roll dice, apply damage, resolve combat, mutate NPC state, or reveal secret checks.

Submission sequence:

`PLAYER_REACTION draft → intent submission → Motor Barbara/Referee orchestration → OSE Rules Engine resolution → canonical state mutation → visible consequence → new Master narration`.

## 5. TTS CONTRACT

TTS scope is exactly `master.visible_narration`. It must not read hidden mechanics, debug values, secret rolls, referee-only material, mechanical engine traces, or non-visible history. TTS controls are utilities and produce zero canonical campaign/RNG mutation.

## 6. MECHANICAL FEEDBACK CONTRACT

Mechanical feedback appears only after resolution and is concise, player-visible, and read-only. It may clarify the consequence already resolved by the engine, but it cannot replace Master narration or become a second gameplay channel.

## 7. NAVIGATION CONTRACT

Primary domains are frozen to:

`SESSÃO / MAPA / FICHA / COMPANHIA`

Settings remains a global utility. MAPA, FICHA and COMPANHIA are inspection/context surfaces. Navigation does not advance world time or mutate mechanical state.

## 8. VISUAL / ASSET CONTRACT

The canonical concept is a mapped 415×915 reference. Exact imagery is tied to reusable assets; all text and runtime values remain DOM/state in implementation. The full-screen PNG is not a production UI asset. Geometry is recorded in `SCREEN_06_SESSION_ACTIVE_MAP.json`.

## 9. GUIDE GATE

This artifact set is proposed for `GUIDE_REAUDIT`. Do not propagate the shell to MAPA/FICHA/COMPANHIA and do not start Wave 3 before guide approval.
