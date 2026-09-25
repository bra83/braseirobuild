from pathlib import Path
import re, sys

if len(sys.argv) != 3:
    raise SystemExit("usage: probe.py <project-root> <output-md>")

root = Path(sys.argv[1]).resolve()
out = Path(sys.argv[2]).resolve()
out.parent.mkdir(parents=True, exist_ok=True)

def read(rel):
    p = root / rel
    return p.read_text(encoding="utf-8", errors="replace") if p.exists() else ""

def section(rel, patterns, before=28, after=90):
    text = read(rel)
    lines = text.splitlines()
    blocks = []
    seen = set()
    for pat in patterns:
        rx = re.compile(pat, re.I)
        for idx, line in enumerate(lines):
            if rx.search(line):
                start = max(0, idx-before)
                end = min(len(lines), idx+after)
                key = (rel, start, end)
                if key in seen:
                    continue
                seen.add(key)
                body = "\n".join(f"{i+1}: {lines[i]}" for i in range(start,end))
                blocks.append(f"### {rel} — /{pat}/ — linha {idx+1}\n\n\`\`\`text\n{body}\n\`\`\`\n")
                break
    return "\n".join(blocks)

parts = ["# OUA 4.9.38 — Journey / Turn / HUD source probe\n"]
parts.append(section("App.tsx", [
    r"const handleMapDiscoveryChanged",
    r"const handlePartyMoved",
    r"const processTurn",
    r"async function processTurn",
    r"function processTurn",
    r"handlePartyRested",
    r"presentNpcIds",
    r"NPCs presentes",
    r"Ações rápidas",
    r"eyeAwareness",
    r"Vigilância do Olho",
]))
parts.append(section("components/immersive/ImmersiveJourneyView.tsx", [
    r"function ImmersiveJourneyView",
    r"Jornada · rota",
    r"hud",
    r"Endurance|Resistência",
    r"Fadiga",
    r"Esperança",
    r"Sombra",
]))
parts.append(section("components/EriadorMap.tsx", [
    r"planRouteToSelected",
    r"advancePlannedRoute",
    r"Movimento livre bloqueado",
]))
parts.append(section("lib/master/types.ts", [
    r"export type JourneyRole",
    r"interface Journey",
    r"journey:",
    r"eyeAwareness",
    r"CompanyState",
]))
parts.append(section("lib/master/state.ts", [
    r"eyeAwareness",
    r"journey:",
    r"scene:",
]))
parts.append(section("lib/master/commands.ts", [
    r"case 'start_journey'",
    r"case 'resolve_journey_march'",
    r"case 'generate_journey_event'",
    r"case 'resolve_journey_event'",
    r"case 'continue_journey_stage'",
    r"case 'complete_journey'",
    r"case 'move_party'",
    r"presentNpcIds",
]))
parts.append(section("lib/rules/journey.ts", [
    r"validateJourneyRoles",
    r"resolveJourneyMarch",
    r"generateJourneyEvent",
    r"resolveJourneyEvent",
    r"completeJourney",
    r"recordJourneyMove",
    r"travelFatigueByHero",
    r"awaitingEventGeneration",
]))
parts.append(section("components/SettingsView.tsx", [
    r"eyeAwareness",
    r"Vigilância",
    r"campanha",
]))
parts.append(section("components/MechanicsHub.tsx", [
    r"Journey",
    r"Jornada",
    r"Marcha",
    r"Evento",
]))

# Search all source files for likely turn-summary/action-time UI hooks and Eye handling.
terms = [
    "eyeAwareness", "journeyRoles", "awaitingEventGeneration", "pendingEvent",
    "turn summary", "FIM DO TURNO", "fim do turno", "processTurn(",
    "presentNpcIds", "participantNpcIds", "activeCharacterId",
]
src_ext = {".ts",".tsx",".js",".jsx",".kt"}
parts.append("## Cross-reference\n")
for term in terms:
    hits=[]
    for p in root.rglob("*"):
        if not p.is_file() or p.suffix.lower() not in src_ext:
            continue
        if any(x in p.parts for x in ("node_modules","build","dist","dist-offline",".offline-vendor")):
            continue
        t=p.read_text(encoding="utf-8",errors="ignore")
        for i,line in enumerate(t.splitlines(),1):
            if term.lower() in line.lower():
                hits.append(f"- \`{p.relative_to(root)}:{i}\` — {line.strip()[:260]}")
                if len(hits)>=40: break
        if len(hits)>=40: break
    parts.append(f"### {term}\n" + ("\n".join(hits) if hits else "- sem ocorrência") + "\n")

out.write_text("\n".join(parts), encoding="utf-8")
print(out)
