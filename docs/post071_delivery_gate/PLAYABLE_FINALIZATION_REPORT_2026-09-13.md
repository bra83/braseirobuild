# BRASEIRO WWN — PLAYABLE FINALIZATION — 2026-09-13

Closure 071 remains closed and authoritative at 225/225 PASS; it was not rerun.

## Physical-device blockers addressed
- Gemini narration now uses an asynchronous Android bridge so network requests no longer block the WebView.
- Current scene image can be attached to Gemini generateContent as inlineData after bounded native downscale/recompression; image remains non-mechanical context.
- Text model, Gemini/Android TTS provider, TTS model and prebuilt voice (including Charon) are configurable in Configurações.
- Gemini TTS uses audio response mode with pt-BR and 24 kHz PCM playback; Android TTS remains fallback/provider option.
- Atlas now exposes VIAJAR PARA ESTE HEX for adjacent destinations and EXPLORAR HEX on the current hex, committing through WorldActionService.
- Travel persists destination/reveal/party position and uses existing WWN travel resolution; POI defaults to null unless canonically present.
- PDF import remains on background I/O, now emits page progress, persists SAF permission when available, caches the parsed local RAG index, and narration RAG excludes rules manuals from NARRATIVE_PUBLIC.
- Gemini connectivity test is asynchronous.
- App version bumped to 1.0.2 / code 102.

## Local evidence
- Production JS syntax 116/116 PASS.
- Static guard PASS.
- Release/security/UTF-8 guard PASS.
- Post071 current contract PASS.
- Encounter actions PASS.
- Inventory/status/POI hotfix PASS.
- Flow integration PASS.
- Playable-finalization focused tests 6/6 PASS.
- Closure065 native contract 35/35 PASS.
- Closure069 native audio/TTS contract 19/19 PASS.
- Stress: 33 sessions × 144 = 4,752 turns PASS; deterministic replay digest e44f81c9530e12bc80b9616b2f7181221008b26c04f448beadd0806e47f1aaca.
- Browser visual smoke could not be rerun in this tool round because localhost/file navigation is blocked by administrator policy; no browser PASS is claimed.

## Next gate
GitHub Android build (`clean test assembleDebug`), then physical-device validation of Gemini text+image, Gemini/Android TTS, PDF import, Atlas travel/explore and responsiveness.
