# BRASEIRO WWN — PERFORMANCE CLOSURE 1.0.5

Date: 2026-09-14
Authority: cumulative post-Closure-071 source; Closure 071 remains closed at 225/225 PASS and was not rerun.

## Performance repairs
- Native campaign load cache avoids repeated Room read + SHA-256 + JSON serialization for the same revision.
- JS persistence cache avoids synchronous native load calls on ordinary re-renders.
- Snapshots are no longer loaded with every campaign load; backup/export requests them explicitly.
- Targeted getEvent/latestNarrative native queries replace full event-history serialization in gameplay narration paths.
- Atlas drag is requestAnimationFrame-throttled and uses transform-only fog movement; selecting a hex no longer rebuilds the entire map DOM.
- Imported/generated scene images are normalized to <=2048 px side / <=4 MP before persistent WebView display.
- Procedural ambience/music/SFX synthesis runs off the Android main thread.
- Pending ambience/music renders are invalidated when superseded or disabled.
- SFX synthesis uses a bounded queue and may be dropped under load rather than blocking gameplay/UI.

## Gates on exact 1.0.5 tree
- Production JS syntax: 119/119 PASS.
- post071-performance-hotfix: 5/5 PASS.
- post071-gameflow-closure: 7/7 PASS.
- Static guard: PASS (133 files).
- Release/security/UTF-8 guard: PASS (178 files).
- Closure065 native contract: 35/35 PASS.
- Closure069 audio/TTS contract: 19/19 PASS.
- Stress: 33 sessions x 144 turns = 4,752 PASS; deterministic replay PASS; digest e44f81c9530e12bc80b9616b2f7181221008b26c04f448beadd0806e47f1aaca.

## Build/device truth
Android compilation is verified separately through bra83/braseirobuild. Physical-device responsiveness still remains the final authority for perceived jank; no source-level gate is claimed as equivalent to real-device profiling.
