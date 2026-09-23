from pathlib import Path
import json, re, sys

if len(sys.argv) != 2:
    raise SystemExit("usage: patch_4922.py <project-root>")
root = Path(sys.argv[1]).resolve()

def read(rel):
    return (root / rel).read_text(encoding="utf-8")

def write(rel, text):
    (root / rel).write_text(text, encoding="utf-8")

def replace_required(text, old, new, label):
    if old not in text:
        raise SystemExit("missing anchor: " + label)
    return text.replace(old, new, 1)

# ---------------------------------------------------------------------------
# REST — TOR 2e core:
# Short Rest: normally max 1/day, at least 1 hour, recover Endurance = Strength;
# Wounded heroes recover no Endurance.
# Long Rest: normally max 1/day; unwounded recover all Endurance; wounded recover
# Strength; heroes at 0 Hope recover 1 Hope.
# Travel Fatigue is NOT cleared by ordinary rest; that requires a protected,
# safe refuge and therefore is intentionally not guessed here.
# ---------------------------------------------------------------------------
types_rel="lib/master/types.ts"
types=read(types_rel)

types=replace_required(
    types,
    """  spotlightByHero: Record<string, number>;
}""",
    """  spotlightByHero: Record<string, number>;
  lastShortRestDayKey?: string;
  lastLongRestDayKey?: string;
}""",
    "CompanyState rest stamps",
)

types=replace_required(
    types,
    """  | { type: 'advance_time'; minutes: number; reason: string; source?: string }""",
    """  | { type: 'advance_time'; minutes: number; reason: string; source?: string }
  | { type: 'resolve_rest'; kind: 'short' | 'long'; reason: string; source?: string }""",
    "resolve_rest command",
)
write(types_rel,types)

commands_rel="lib/master/commands.ts"
commands=read(commands_rel)

# Add deterministic rest handler before generic advance_time.
anchor="""    case 'advance_time': {"""
if anchor not in commands:
    raise SystemExit("advance_time case anchor missing")
rest_case="""    case 'resolve_rest': {
      const dayKey = `${nextState.worldClock.year}:${nextState.worldClock.season}:${nextState.worldClock.day}`;
      const stampField = command.kind === 'short' ? 'lastShortRestDayKey' : 'lastLongRestDayKey';
      if (nextState.company[stampField] === dayKey) {
        throw new Error(command.kind === 'short'
          ? 'Descanso Curto já realizado neste dia.'
          : 'Descanso Prolongado já realizado neste dia.');
      }

      for (const characterId of partyIds) {
        updateHero(characterId, sheet => {
          const next = structuredClone(sheet);
          const strength = Math.max(0, Number(next.attributes.strength) || 0);
          if (command.kind === 'short') {
            if (!next.attributes.wounded) {
              next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength);
            }
          } else {
            if (next.attributes.wounded) {
              next.attributes.enduranceCurrent = Math.min(next.attributes.enduranceMax, next.attributes.enduranceCurrent + strength);
            } else {
              next.attributes.enduranceCurrent = next.attributes.enduranceMax;
            }
            if (next.attributes.hopeCurrent <= 0) {
              next.attributes.hopeCurrent = Math.min(next.attributes.hopeMax, 1);
            }
          }
          return recomputeConditions(next);
        });
      }

      nextState.company = { ...nextState.company, [stampField]: dayKey };
      if (command.kind === 'short') {
        const advanced = advanceWorldClock(nextState.worldClock, 60);
        nextState = { ...nextState, worldClock: advanced.clock };
        const simulated = simulateLivingWorldDetailed(nextState, nextCampaign, nextNpcs, 60);
        nextState = simulated.master;
        nextNpcs = simulated.npcs;
      }
      appendLivingActivity(
        'rest',
        command.kind === 'short' ? 'Descanso Curto' : 'Descanso Prolongado',
        command.kind === 'short'
          ? 'A Companhia descansou por pelo menos uma hora.'
          : 'A Companhia concluiu um Descanso Prolongado.',
        nextState.position.currentHexId || undefined,
      );
      break;
    }
"""
commands=commands.replace(anchor,rest_case+anchor,1)
write(commands_rel,commands)

# State migration: rest stamps live in CompanyState and are optional, so no DB migration.
state_rel="lib/master/state.ts"
state=read(state_rel)
# no source rewrite needed beyond type compatibility, but force preservation gate later.

# ---------------------------------------------------------------------------
# APP — wire the rest handler into GameView. Previously GameView exposed the prop
# but App never supplied it, so the button could not actually resolve rest.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)

insert_before="""  const handlePartyMoved = async (move: PartyMapMove) => {"""
if insert_before not in app:
    raise SystemExit("handlePartyMoved anchor missing")
handler="""  const handlePartyRested = async (kind: 'short' | 'long') => {
    const base = campaignRef.current;
    if (!base) return;
    const mechanical = applyMasterCommands(base, characters, allNpcs, [{
      type: 'resolve_rest',
      kind,
      reason: kind === 'short' ? 'Descanso Curto resolvido pelo mapa.' : 'Descanso Prolongado resolvido pelo mapa.',
      source: 'O Um Anel 2e — Descanso',
    }]);
    if (mechanical.rejected.length) {
      throw new Error(mechanical.rejected.map(item => item.reason).join(' '));
    }
    const committed = await saveMasterTransaction(mechanical.campaign, mechanical.characters, mechanical.audits, kind === 'short' ? 'descanso curto' : 'descanso prolongado');
    saveEmergencyCampaignMirror(committed);
    campaignRef.current = committed;
    setCampaign(committed);
    setCharacters(mechanical.characters);
    setAllNpcs(mechanical.npcs);
  };

"""
app=app.replace(insert_before,handler+insert_before,1)

# Wire both GameView branches.
app=app.replace(
    "onPartyMoved={handlePartyMoved} onMapDiscoveryChanged={handleMapDiscoveryChanged} immersiveMode={immersiveJourneyMode}",
    "onPartyMoved={handlePartyMoved} onMapDiscoveryChanged={handleMapDiscoveryChanged} onPartyRested={handlePartyRested} immersiveMode={immersiveJourneyMode}",
)
if app.count("onPartyRested={handlePartyRested}") < 2:
    raise SystemExit("GameView rest handler not wired in both branches")
write(app_rel,app)

# ---------------------------------------------------------------------------
# GAME VIEW and IMMERSIVE VIEW prop types.
# ---------------------------------------------------------------------------
game_rel="components/GameView.tsx"
game=read(game_rel)
game=game.replace(
    "  onPartyRested?: () => void | Promise<void>;",
    "  onPartyRested?: (kind: 'short' | 'long') => void | Promise<void>;",
)
write(game_rel,game)

imm_rel="components/immersive/ImmersiveJourneyView.tsx"
imm=read(imm_rel)
if "onPartyRested?: () => void | Promise<void>;" in imm:
    imm=imm.replace(
        "onPartyRested?: () => void | Promise<void>;",
        "onPartyRested?: (kind: 'short' | 'long') => void | Promise<void>;",
    )
elif "onPartyRested: () => void | Promise<void>;" in imm:
    imm=imm.replace(
        "onPartyRested: () => void | Promise<void>;",
        "onPartyRested: (kind: 'short' | 'long') => void | Promise<void>;",
    )
elif "onPartyRested" not in imm:
    raise SystemExit("ImmersiveJourneyView rest prop missing")
write(imm_rel,imm)

# ---------------------------------------------------------------------------
# MAP — two explicit rest actions. Do not zero canonical travel Fatigue.
# ---------------------------------------------------------------------------
map_rel="components/EriadorMap.tsx"
mp=read(map_rel)
mp=mp.replace(
    "  onPartyRested?: () => void | Promise<void>;",
    "  onPartyRested?: (kind: 'short' | 'long') => void | Promise<void>;",
)

old="""  const restGroup = async () => {
    if (!onPartyRested) {
      setError('O descanso deve ser resolvido pelo Mestre Virtual, pois Fadiga pertence às fichas dos Heróis.');
      return;
    }
    await onPartyRested();
    if (discovery) {
      const updated = { ...discovery, fatigue: 0, updatedAt: Date.now() };
      await onDiscoveryChanged(updated, 'sincronização visual após descanso');
      setDiscovery(updated);
    }
    setMessage('Descanso enviado ao Mestre Virtual para resolução da Companhia.');
  };"""
new="""  const restGroup = async (kind: 'short' | 'long') => {
    if (!onPartyRested) {
      setError('O descanso deve ser resolvido pelo motor de regras.');
      return;
    }
    try {
      await onPartyRested(kind);
      setError(null);
      setMessage(kind === 'short'
        ? 'Descanso Curto resolvido: pelo menos 1 hora; Feridos não recuperam Resistência.'
        : 'Descanso Prolongado resolvido: Resistência recuperada pelas regras; Esperança 0 recupera 1 ponto.');
    } catch (error) {
      setError(error instanceof Error ? error.message : 'O descanso não pôde ser resolvido.');
    }
  };"""
mp=replace_required(mp,old,new,"restGroup")

old_button="""              {onPartyRested && (
                <button
                  onClick={restGroup}
                  className="ml-1 rounded bg-blue-950/50 px-1.5 py-0.5 text-blue-200 hover:bg-blue-900/80 transition-colors border border-blue-800/40"
                >
                  Resolver descanso
                </button>
              )}"""
new_button="""              {onPartyRested && (
                <div className="ml-1 flex gap-1">
                  <button onClick={() => void restGroup('short')} className="rounded bg-blue-950/50 px-1.5 py-0.5 text-blue-200 hover:bg-blue-900/80 transition-colors border border-blue-800/40">Curto</button>
                  <button onClick={() => void restGroup('long')} className="rounded bg-blue-950/50 px-1.5 py-0.5 text-blue-200 hover:bg-blue-900/80 transition-colors border border-blue-800/40">Prolongado</button>
                </div>
              )}"""
mp=replace_required(mp,old_button,new_button,"classic rest buttons")

# Add immersive rest controls after movement guidance if present.
guide="""            {discovery?.currentHexId && <div style={{ marginTop: 7, fontSize: 10, lineHeight: 1.35, opacity: .72 }}>
              Destino distante: Planejar rota → Avançar 1 hex pela rota.
            </div>}"""
if guide not in mp:
    guide="""            {discovery?.currentHexId && <div style={{ marginTop: 7, fontSize: 10, lineHeight: 1.35, opacity: .72 }}>
              A viagem ocorre um hex por vez. Para um destino distante: Planejar rota → Avançar 1 hex pela rota.
            </div>}"""
if guide in mp:
    immersive_rest=guide+"""
            {onPartyRested && discovery?.currentHexId && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 7, marginTop: 7 }}>
                <button type="button" onClick={() => void restGroup('short')} style={{ minHeight: 42, borderRadius: 8, border: '1px solid rgba(117,151,175,.45)', background: 'rgba(23,48,66,.62)', color: '#d8e9f2', fontWeight: 700 }}>Descanso Curto</button>
                <button type="button" onClick={() => void restGroup('long')} style={{ minHeight: 42, borderRadius: 8, border: '1px solid rgba(117,151,175,.45)', background: 'rgba(23,48,66,.62)', color: '#d8e9f2', fontWeight: 700 }}>Descanso Prolongado</button>
              </div>
            )}"""
    mp=mp.replace(guide,immersive_rest,1)
else:
    raise SystemExit("immersive movement guidance anchor missing")

write(map_rel,mp)

# ---------------------------------------------------------------------------
# Rule cards: keep built-in deterministic summaries aligned with corrected rules.
# ---------------------------------------------------------------------------
cards_rel="lib/rules/ruleCards.ts"
cards=read(cards_rel)
cards=cards.replace(
    "{ id: 'fellowship', title: 'Sociedade da Companhia', domains: ['companhia', 'sociedade'], keywords: ['sociedade', 'companhia', 'esperança', 'patrono'], summary: 'Sociedade é um recurso compartilhado entre todos os Heróis-jogadores, usado principalmente para recuperar Esperança ao descansar e para efeitos do Patrono.', source: 'O Um Anel 2e — Livro Básico', page: 54 },",
    "{ id: 'fellowship', title: 'Sociedade da Companhia', domains: ['companhia', 'sociedade'], keywords: ['sociedade', 'companhia', 'esperança', 'patrono'], summary: 'Sociedade é um recurso compartilhado. Durante a Fase de Aventura, quando a Companhia descansa, pontos de Sociedade podem ser distribuídos como Esperança com acordo do grupo.', source: 'O Um Anel 2e — Livro Básico', page: 72 },",
)
journey_card="{ id: 'journey-roles', title: 'Papéis e eventos de Jornada', domains: ['jornada'], keywords: ['jornada', 'guia', 'batedor', 'vigia', 'caçador', 'fadiga'], summary: 'A Companhia distribui papéis de Jornada. Eventos podem desafiar Batedores com Exploração, Vigias com Vigilância e Caçadores com Caçada, aplicando consequências e Fadiga ao alvo ou ao grupo conforme o evento.', source: 'O Um Anel 2e — Livro Básico', page: 112 },"
if journey_card in cards:
    cards=cards.replace(
        journey_card,
        "{ id: 'journey-roles', title: 'Papéis e duração de Jornada', domains: ['jornada'], keywords: ['jornada', 'guia', 'batedor', 'vigia', 'caçador', 'fadiga', 'terreno'], summary: 'Toda Jornada cobre Guia, Caçador, Vigia e Batedor. A duração básica é 1 dia por hex, adicionando 1 dia para cada hex de terreno difícil. Eventos e Fadiga de Viagem são resolvidos separadamente pelas regras de Jornada.', source: 'O Um Anel 2e — Livro Básico', page: 109 },",
    )
cards += """
"""
write(cards_rel,cards)

# ---------------------------------------------------------------------------
# VERSION 4.9.22
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.22"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.21"','"version": "4.9.22"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
version=read(version_rel)
version=re.sub(r"APP_VERSION = '4\.9\.21'","APP_VERSION = '4.9.22'",version)
version=re.sub(r"APP_VERSION_CODE = 49021","APP_VERSION_CODE = 49022",version)
write(version_rel,version)

gradle_rel="android/app/build.gradle.kts"
gradle=read(gradle_rel)
gradle=re.sub(r"versionCode\s*=\s*49021","versionCode = 49022",gradle)
gradle=re.sub(r'versionName\s*=\s*"4\.9\.21"','versionName = "4.9.22"',gradle)
write(gradle_rel,gradle)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)
main=main.replace("index.html?v=49021&immersive=1","index.html?v=49022&immersive=1")
write(main_rel,main)

# gates
app=read(app_rel); commands=read(commands_rel); mp=read(map_rel); game=read(game_rel); imm=read(imm_rel)
db=read("lib/db.ts"); models=read("lib/geminiModels.ts"); main=read(main_rel)
checks={
 "rest command": "type: 'resolve_rest'" in types and "case 'resolve_rest'" in commands,
 "short rest Strength": "if (!next.attributes.wounded)" in commands and "enduranceCurrent + strength" in commands,
 "short rest hour": "advanceWorldClock(nextState.worldClock, 60)" in commands,
 "long rest full": "next.attributes.enduranceCurrent = next.attributes.enduranceMax" in commands,
 "wounded long rest Strength": "if (next.attributes.wounded)" in commands and "enduranceCurrent + strength" in commands,
 "zero Hope long rest": "next.attributes.hopeCurrent <= 0" in commands and "hopeCurrent = Math.min(next.attributes.hopeMax, 1)" in commands,
 "daily rest stamps": "lastShortRestDayKey" in types and "lastLongRestDayKey" in types and "Descanso Curto já realizado neste dia" in commands,
 "App wires rest": app.count("onPartyRested={handlePartyRested}") >= 2,
 "map explicit rest": "Descanso Curto" in mp and "Descanso Prolongado" in mp and "restGroup('short')" in mp and "restGroup('long')" in mp,
 "no fake fatigue clear": "fatigue: 0, updatedAt: Date.now() };\n      await onDiscoveryChanged(updated, 'sincronização visual após descanso')" not in mp,
 "one model preserved": "const TEXT_KEY = 'braseiro.geminiModel.text'" in models,
 "network retry preserved": "isTransientGeminiNetworkError" in main,
 "party preserved": 'data-party-world-anchored="true"' in mp,
 "db identity": "const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
 "version": "4.9.22" in read(version_rel) and "49022" in read(gradle_rel),
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4922_FAILED: "+", ".join(failed))
print("PATCH_OUA_4.9.22_REST_RULES_OK")
