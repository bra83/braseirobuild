# WWN — Macro 3: advanced systems

Implemented in one macro pass, with Gradle/APK/device still deferred:
- Advanced magic management wired to canonical services: research laboratories, custom spell research and resolution, magical Workings, magic-item projects and resolution.
- Special combat management wired to the existing WWN service: Hold, Screen an Ally, Shove, Grapple start/move, dual wield, snap attack and swarm attack.
- Explicit-context gates are preserved; UI does not silently invent GM approval, research context, project time, targets, or execution prerequisites.
- Campaign integrity now validates advanced-magic collections and special-combat state/maps/history.
- Existing progression, standard magic, crafting/mods, factions, major projects, living NPCs, travel, dungeon, combat, backup/restore and journal remain connected.

QA: all JS syntax checks PASS; all .mjs regression suites PASS, including the new advanced integration and deep integrity suites.
