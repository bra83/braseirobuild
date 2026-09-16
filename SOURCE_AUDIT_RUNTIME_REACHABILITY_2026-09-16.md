# WWN source audit — runtime reachability

## Scope
Global source pass after canonical NPC registry repair. This pass searched for mechanically implemented services that had no reachable UI/controller integration.

## Defects fixed
1. `gestao.js` still retained two legacy reads from `worldState.npcs`, including NPC memory preservation during update. Both now use canonical `npcState` only.
2. `ArtRuntime071Service` was mechanically implemented but unreachable from the application UI.
3. `MagicRuntime071Service` was mechanically implemented but unreachable from the application UI.

## Implementation
- Added a protected Runtime 071 GM panel to Gestão.
- All 5 implemented Art Runtime 071 operations are reachable.
- All 22 implemented Magic Runtime 071 operations are reachable.
- Runtime dispatch is allowlisted; arbitrary service methods cannot be invoked.
- `campaignId` is injected from the active campaign and cannot be redirected by the UI workflow.
- Rule evidence remains explicit: targets, context, saves, physical dice and GM approvals are not fabricated by the interface.

## Validation
- 141 JavaScript source modules: syntax PASS.
- 14 QA suites: PASS.
- 66 accumulated simulated campaign sessions: PASS.
- Canonical runtime global regression: PASS.
- No direct application UI read of obsolete `worldState.npcs` remains. Legacy reads remain only in migration/integrity compatibility paths.
