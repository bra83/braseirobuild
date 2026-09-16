# WWN source audit — campaign bootstrap

- Fixed a product-level split where campaigns created from Settings were persisted as empty worlds while random adventures from Home received starter content/world state.
- CampaignManagerService now accepts the validated starter pack and seeds public/GM content, canonical NPC state, starter position, elapsed day baseline, and living-world presentation state before the first character is created.
- Settings loads the same starter pack used by the rest of the product and creates the world atomically before routing to character creation.
- No faction mechanical ratings are fabricated: narrative factions remain narrative until the GM explicitly supplies WWN mechanical stats through the faction registry UI.
- Added regression: qa/campaign-world-bootstrap-regression.mjs.
- Full QA: 22/22 suites PASS.
- JavaScript syntax check: PASS for all www JS modules.
- Gradle/APK deliberately excluded from this source closure pass.
