# BRASEIRO OSE — P0 GLOBAL COMPONENTS

Authority: `BRASEIRO_OSE_NARRATIVE_FIRST_SCREEN_ARCHITECTURE_V1` + user-supplied OSE asset pack.

Reusable P0 contracts implemented in `index.html`, `p1.css`, and `p1.js`:

- `p0-topbar`: OSE brand, screen title, Barbara automatic-GM status, global settings utility.
- `p0-statusbar`: read-only campaign context (time, weather, torch, rations).
- `p0-card`: parchment content surface for dynamic DOM text/state.
- `p0-primary-nav`: exactly `SESSÃO / MAPA / FICHA / COMPANHIA`.
- `p0-toast`: transient UI feedback; never a mechanical authority.
- Session shell: narrative, visible-only TTS, suggestions that only prefill `PLAYER_REACTION`, GM_HELP read-only, concise mechanical feedback.
- Map shell: Geometry V1 camera/pan/zoom inspection. No direct movement mutation.
- Sheet shell: read-only character state.
- Company shell: read-only party state using the single canonical PositionState.

Mechanical mutation contract: only the element with `data-channel="PLAYER_REACTION"` carries `data-intentional-mutation="true"`. All map camera, navigation, suggestion, TTS, settings, sheet and company controls are inspection/presentation only.

Dynamic text and state are DOM/NATIVE_UI. No variable campaign value is baked into an image asset.
