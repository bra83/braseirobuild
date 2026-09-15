# POST071 Android Safe-Area / Width Hotfix — 2026-09-13

Trigger: physical-device screenshots showed two Android-only layout defects: WebView content under the status/navigation bars and the 415 px design canvas cropped on phones with a narrower CSS viewport.

Preserved: Closure 071 mechanics/runtime, 225/225 evidence, audio hotfix, visual assets, navigation semantics, rules, saves and native integrations.

Changes:
- MainActivity wraps the WebView in an inset-aware FrameLayout.
- Window is explicitly edge-to-edge; system-bar/display-cutout insets are applied as root padding.
- WebView enables wide viewport + overview mode so the 415 px design canvas is fitted instead of cropped horizontally.
- Fixed-canvas pages use a 415 px viewport without forcing initial-scale=1, allowing WebView fit-to-width.
- Jornada/Diário/Regras shells use dynamic viewport height, keeping bottom navigation above Android navigation controls.
- Atlas keeps canonical 415 px geometry while dynamically shrinking the map viewport and anchoring terrain info above the bottom nav.
- Combat and Mochila receive equivalent dynamic-height treatment where their existing layout supports it.
- Ficha/SCR016 keep exact 415x915 coordinate authority; the page can scroll vertically on shorter safe viewports rather than clipping content.

Local gates:
- production JS syntax: 116/116 PASS
- Static Guard: PASS
- Release Security Guard: PASS
- Closure 069 audio/TTS contract: 19/19 PASS
- POST071 current contract: PASS
- Safe-area/layout static contract: PASS
- current physical-dice source sanity: PASS

Note: tools/closure068_physical_dice_contract.py references the historical WwnDice3DOverlay class and is not the current post-071 physical-dice authority; the active MainActivity uses BraseiroDice3DOverlay/PhysicalDiceEngine and passed the current-source sanity gate.
