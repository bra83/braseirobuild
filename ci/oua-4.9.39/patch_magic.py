from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_magic.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text:
        raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

# Dice result.
rel="lib/dice.ts"
t=read(rel)
t=req(t,
"  miserable?: boolean; // legacy storage name; UI calls this Arrasado\n",
"""  miserable?: boolean; // legacy storage name; UI calls this Arrasado
  magicalSuccess?: boolean;
  magicalSource?: string;
""","RollResult magical fields")
magical="""export function rollMagicalSuccess(
  successDiceCount: number,
  skillName = '',
  source = 'Sucesso Mágico',
  weary = false,
): RollResult {
  const count = Math.max(0, Math.trunc(Number(successDiceCount) || 0));
  const successResults = Array.from({ length: count }, () => secureRoll(1, 6));
  const tengwarCount = successResults.filter(value => value === 6).length;
  const total = successResults.reduce((sum, value) => sum + (weary && value <= 3 ? 0 : value), 0);
  const successLevel: NonNullable<RollResult['successLevel']> =
    tengwarCount >= 2 ? 'extraordinary-success' : tengwarCount === 1 ? 'great-success' : 'success';
  return {
    id: 'roll_' + Date.now() + '_' + secureRoll(100, 999),
    timestamp: Date.now(),
    notation: String(count) + 'd6 · Sucesso Mágico',
    total,
    modifier: 0,
    results: [...successResults],
    type: 'one-ring',
    featResults: [],
    successResults,
    skillName: skillName.trim() || undefined,
    success: true,
    successLevel,
    tengwarCount,
    weary,
    miserable: false,
    magicalSuccess: true,
    magicalSource: source,
    physics: { engine: 'webcrypto', mode: 'fallback' },
  };
}

"""
t=req(t,"// Helper to format roll description",magical+"// Helper to format roll description","magical roll function")
t=req(t,
"""  } else {
    const featStr = roll.featResult === 11 """,
"""  } else {
    if (roll.magicalSuccess) {
      const successStr = roll.successResults && roll.successResults.length > 0
        ? 'Dados de Sucesso: [' + roll.successResults.map(r => r === 6 ? 'Tengwar (6)' : String(r)).join(', ') + ']'
        : 'Sem Dados de Sucesso';
      return 'O Um Anel · SUCESSO MÁGICO' + (roll.magicalSource ? ' (' + roll.magicalSource + ')' : '') + ' | ' + successStr + ' | ' + String(roll.successLevel || 'success');
    }
    const featStr = roll.featResult === 11 ""","format magical")
write(rel,t)

# DB.
rel="lib/db.ts"
t=read(rel)
t=req(t,
"  characterName?: string;\n}",
"""  characterName?: string;
  magicalSuccess?: boolean;
  magicalSource?: string;
}""","PendingRoll magical")
write(rel,t)

# DiceView.
rel="components/DiceView.tsx"
t=read(rel)
t=t.replace(
"import { rollOneRing, rollOneRingFromObservation, rollStandard, rollStandardFromObservation, type RollResult } from '../lib/dice';",
"import { rollMagicalSuccess, rollOneRing, rollOneRingFromObservation, rollStandard, rollStandardFromObservation, type RollResult } from '../lib/dice';",
1)
t=req(t,
"import { skillAttribute } from '../lib/rules/tests';",
"import { skillAttribute } from '../lib/rules/tests';\nimport type { MasterState } from '../lib/master/types';\nimport { canSpendHopeForSupplementMagicalSuccess, gandalfIsAutomaticMagicalSuccess, magicalSuccessSource } from '../lib/rules/supplementMechanics';",
"Dice magical imports")
# Allow either 2-arg or 3-arg current signature.
if "onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired?: boolean) => Promise<number>;" in t:
    t=t.replace(
        "onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired?: boolean) => Promise<number>;",
        "onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired?: boolean) => Promise<number>;\n  masterState: MasterState;",
        1)
elif "onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits') => Promise<number>;" in t:
    t=t.replace(
        "onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits') => Promise<number>;",
        "onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits', inspired?: boolean) => Promise<number>;\n  masterState: MasterState;",
        1)
else:
    raise SystemExit("missing anchor: Dice onSpendHope signature")
t=req(t,"  onSpendHope,\n}) => {","  onSpendHope,\n  masterState,\n}) => {","Dice master destructure")
t=req(t,
"  const [rollingPhysical, setRollingPhysical] = useState(false);\n",
"""  const [rollingPhysical, setRollingPhysical] = useState(false);
  const terrain = masterState.journey.segments?.at(-1)?.terrain || '';
  const isNight = masterState.worldClock.hour < 6 || masterState.worldClock.hour >= 20;
""","Dice context")
t=req(t,
"      result = { ...result, characterId: selectedCharacter?.id, characterName: selectedCharacter?.sheet.identity.name || selectedCharacter?.name };\n",
"""      if (selectedCharacter && gandalfIsAutomaticMagicalSuccess(selectedCharacter.sheet, Number(result.featResult) || 0)) {
        result = { ...result, success: true, magicalSuccess: true, magicalSource: 'Habilidade dos Eldar' };
      }
      result = { ...result, characterId: selectedCharacter?.id, characterName: selectedCharacter?.sheet.identity.name || selectedCharacter?.name };
""","Eldar result")
handler="""  const handleMagicalSuccess = async () => {
    if (!selectedCharacter || selectedCharacter.sheet.attributes.hopeCurrent <= 0) return;
    const context = { kind: 'skill' as const, skill: oneRingSkillName, terrain, isNight };
    const source = magicalSuccessSource(selectedCharacter.sheet, context);
    if (!source || !canSpendHopeForSupplementMagicalSuccess(selectedCharacter.sheet, context)) {
      setDiceError('Sucesso Mágico indisponível para esta Perícia e situação.');
      return;
    }
    setDiceError(null);
    try {
      const attribute = skillAttribute(selectedCharacter.sheet, oneRingSkillName);
      const spent = await onSpendHope(selectedCharacter.id, attribute, false);
      if (spent <= 0) throw new Error('Não foi possível gastar Esperança.');
      let result = rollMagicalSuccess(oneRingSuccessCount, oneRingSkillName, source, Boolean(selectedCharacter.sheet.attributes.weary));
      result = { ...result, characterId: selectedCharacter.id, characterName: selectedCharacter.sheet.identity.name || selectedCharacter.name };
      await saveStoreData('rolls', result);
      await loadHistory();
      if (pendingTest) await onSendRollToMaster(result);
      setHopeBonus(0);
      setHopeSpentForRoll(false);
    } catch (error) {
      setDiceError(error instanceof Error ? error.message : String(error));
    }
  };

"""
t=req(t,"  const handleRollStandard = async () => {",handler+"  const handleRollStandard = async () => {","Dice magical handler")
t=t.replace(
'<p className="mt-1 text-[10px] text-slate-500">Calculado pela ficha: 20 − atributo relacionado.</p>',
"<p className=\"mt-1 text-[10px] text-slate-500\">Calculado pela ficha: {selectedCharacter.sheet.other.rulesMode === 'strider' ? '18' : '20'} − atributo relacionado.</p>",
1)
button="""            {canSpendHopeForSupplementMagicalSuccess(selectedCharacter.sheet, { kind:'skill', skill:oneRingSkillName, terrain, isNight }) && (
              <button type="button" onClick={() => void handleMagicalSuccess()} disabled={rollingPhysical || selectedCharacter.sheet.attributes.hopeCurrent <= 0} className="rounded border border-violet-600 bg-violet-950/35 px-3 py-3 text-sm font-bold text-violet-100 disabled:opacity-40">
                Gastar 1 Esperança · Sucesso Mágico
              </button>
            )}
"""
anchor="""            <button disabled={rollingPhysical} onClick={() => void handleRollOneRing()} className="flex items-center justify-center gap-2 rounded border border-cyan-600 bg-cyan-950/35 px-4 py-3 font-bold text-cyan-100 hover:bg-cyan-900/35"><Dices className="h-5 w-5" /> Rolar por {selectedCharacter.sheet.identity.name}</button>"""
t=req(t,anchor,button+anchor,"Dice magical button")
write(rel,t)

# App.
rel="App.tsx"
t=read(rel)
if "rollMagicalSuccess" not in t:
    t=t.replace("import { rollOneRing", "import { rollMagicalSuccess, rollOneRing",1)
if "magicalSuccessSource" not in t:
    t=t.replace(
        "import { supplementBonusDice, supplementInspired, supplementRollMode } from './lib/rules/supplementMechanics';",
        "import { canSpendHopeForSupplementMagicalSuccess, magicalSuccessSource, supplementBonusDice, supplementInspired, supplementRollMode } from './lib/rules/supplementMechanics';",
        1)
t=req(t,
"""    mountVigour = 0,
    spendHope = false,
  ) => {""",
"""    mountVigour = 0,
    spendHope = false,
    useMagicalSuccess = false,
  ) => {""","Journey magic parameter")
t=req(t,
"""    if (spendHope && hero.sheet.attributes.hopeCurrent <= 0) throw new Error('Esperança insuficiente.');
    const roll = rollJourneySkill(hero, skill, extraDice, forcedMode, spendHope ? 1 : 0);
    const commands: MasterCommand[] = [];
    if (spendHope) {""",
"""    if ((spendHope || useMagicalSuccess) && hero.sheet.attributes.hopeCurrent <= 0) throw new Error('Esperança insuficiente.');
    if (spendHope && useMagicalSuccess) throw new Error('Escolha entre o bônus normal de Esperança e o Sucesso Mágico.');
    const terrain = master.journey.segments?.at(-1)?.terrain || '';
    const isNight = master.worldClock.hour < 6 || master.worldClock.hour >= 20;
    const magicalContext = { kind: 'skill' as const, skill, terrain, isNight, journeyMode: hero.sheet.other.rulesMode === 'strider' ? 'strider' as const : 'standard' as const };
    const source = useMagicalSuccess ? magicalSuccessSource(hero.sheet, magicalContext) : null;
    if (useMagicalSuccess && !canSpendHopeForSupplementMagicalSuccess(hero.sheet, magicalContext)) throw new Error('Sucesso Mágico indisponível para esta Perícia/situação.');
    const roll = source
      ? rollMagicalSuccess(Math.max(0, Number(findSkill(hero.sheet, skill)?.rank) || 0) + Math.trunc(extraDice), skill, source, Boolean(hero.sheet.attributes.weary))
      : rollJourneySkill(hero, skill, extraDice, forcedMode, spendHope ? 1 : 0);
    const commands: MasterCommand[] = [];
    if (spendHope || useMagicalSuccess) {""","Journey magic body")
t=t.replace(
"reason: 'Esperança gasta explicitamente na rolagem de Jornada.',",
"reason: useMagicalSuccess ? 'Esperança gasta para obter Sucesso Mágico na Jornada.' : 'Esperança gasta explicitamente na rolagem de Jornada.',",
1)
# Persist pending metadata if current handler anchor exists.
if "magicalSuccess: roll.magicalSuccess" not in t:
    t=t.replace(
        "        tengwarCount: roll.tengwarCount,\n        characterId:",
        "        tengwarCount: roll.tengwarCount,\n        magicalSuccess: roll.magicalSuccess,\n        magicalSource: roll.magicalSource,\n        characterId:",
        1)
t=t.replace("onSpendHope={handleSpendHope} />","onSpendHope={handleSpendHope} masterState={masterState} />")
write(rel,t)

# Journey UI.
rel="components/JourneyResolutionPanel.tsx"
t=read(rel)
if "magicalSuccessSource" not in t:
    t=req(t,
        "import { findSkill, targetNumberForSkill } from '../lib/characterRules';",
        "import { findSkill, targetNumberForSkill } from '../lib/characterRules';\nimport { canSpendHopeForSupplementMagicalSuccess, magicalSuccessSource } from '../lib/rules/supplementMechanics';",
        "Journey magic imports")
t=t.replace(
"onRollJourney: (kind: 'march' | 'event' | 'end', characterId: string, skill: string, extraDice?: number, forcedMode?: RollMode, mountVigour?: number, spendHope?: boolean) => Promise<void>;",
"onRollJourney: (kind: 'march' | 'event' | 'end', characterId: string, skill: string, extraDice?: number, forcedMode?: RollMode, mountVigour?: number, spendHope?: boolean, useMagicalSuccess?: boolean) => Promise<void>;",
1)
t=req(t,
"  const [spendHope,setSpendHope]=useState(false);\n",
"""  const [spendHope,setSpendHope]=useState(false);
  const [useMagicalSuccess,setUseMagicalSuccess]=useState(false);
""","Journey magic state")
helper="""  const hopeControls=(hero:Character|undefined,skill:string)=>{
    const terrain=masterState.journey.segments?.at(-1)?.terrain||'';
    const isNight=masterState.worldClock.hour<6||masterState.worldClock.hour>=20;
    const context={kind:'skill' as const,skill,terrain,isNight,journeyMode:hero?.sheet.other.rulesMode==='strider'?'strider' as const:'standard' as const};
    const source=hero?magicalSuccessSource(hero.sheet,context):null;
    const canMagic=Boolean(hero&&canSpendHopeForSupplementMagicalSuccess(hero.sheet,context));
    return <div style={{display:'grid',gap:5,marginTop:8,fontSize:11}}>
      <label style={{display:'flex',alignItems:'center',gap:7,opacity:hero&&hero.sheet.attributes.hopeCurrent>0?1:.45}}>
        <input type="checkbox" checked={spendHope} disabled={!hero||hero.sheet.attributes.hopeCurrent<=0||useMagicalSuccess} onChange={e=>{setSpendHope(e.target.checked);if(e.target.checked)setUseMagicalSuccess(false);}}/>
        Gastar 1 Esperança {strider?'· Peregrino: Inspirado (+2d)':'· bônus normal'}
      </label>
      {canMagic&&<label style={{display:'flex',alignItems:'center',gap:7,color:'#d8b4fe'}}>
        <input type="checkbox" checked={useMagicalSuccess} disabled={!hero||hero.sheet.attributes.hopeCurrent<=0||spendHope} onChange={e=>{setUseMagicalSuccess(e.target.checked);if(e.target.checked)setSpendHope(false);}}/>
        Gastar 1 Esperança · Sucesso Mágico{source?' · '+source:''}
      </label>}
    </div>;
  };
"""
t=req(t,"  const endDone = masterState.journey.endRollsByHero || {};\n","  const endDone = masterState.journey.endRollsByHero || {};\n\n"+helper,"Journey magic controls helper")
# End-of-Journey global Hope checkbox becomes per-hero controls.
t=t.replace(
"""        <label style={{ display:'flex', alignItems:'center', gap:7, margin:'0 0 8px', fontSize:11 }}>
          <input type="checkbox" checked={spendHope} onChange={change=>setSpendHope(change.target.checked)} />
          Gastar 1 Esperança na próxima rolagem {strider ? '· Inspirado (+2d)' : ''}
        </label>
""","")
t=t.replace(
"""                    <button disabled={props.busy} onClick={() => void props.onRollJourney('end', hero.id, 'Viagem', 0, undefined, mountVigour[hero.id] || 0, spendHope)} style={{ ...primaryButton, minHeight: 38 }}>
                      Rolar Viagem
                    </button>
                  </div>""",
"""                    <button disabled={props.busy} onClick={() => void props.onRollJourney('end', hero.id, 'Viagem', 0, undefined, mountVigour[hero.id] || 0, spendHope, useMagicalSuccess)} style={{ ...primaryButton, minHeight: 38 }}>
                      Rolar Viagem
                    </button>
                  </div>
                  {hopeControls(hero,'Viagem')}""")
# Event controls.
t=t.replace(
"""            <label style={{ display:'flex', alignItems:'center', gap:7, margin:'0 0 7px', fontSize:11 }}>
              <input type="checkbox" checked={spendHope} onChange={change=>setSpendHope(change.target.checked)} />
              Gastar 1 Esperança {strider ? '· Peregrino fica Inspirado (+2d)' : ''}
            </label>
            <button
              disabled={!target || props.busy}
              onClick={() => target && void props.onRollJourney('event', target.id, event.skill, Number(event.successDiceModifier || 0), event.rollMode, 0, spendHope)}
""",
"""            {hopeControls(target,event.skill)}
            <button
              disabled={!target || props.busy}
              onClick={() => target && void props.onRollJourney('event', target.id, event.skill, Number(event.successDiceModifier || 0), event.rollMode, 0, spendHope, useMagicalSuccess)}
""")
# March controls.
t=t.replace(
"""            <label style={{ display:'flex', alignItems:'center', gap:7, margin:'0 0 7px', fontSize:11 }}>
              <input type="checkbox" checked={spendHope} onChange={change=>setSpendHope(change.target.checked)} />
              Gastar 1 Esperança {strider ? '· Peregrino fica Inspirado (+2d)' : ''}
            </label>
            <button disabled={!guide || props.busy} onClick={() => guide && void props.onRollJourney('march', guide.id, 'Viagem', 0, undefined, 0, spendHope)} style={primaryButton}>Rolar Marcha</button>
""",
"""            {hopeControls(guide,'Viagem')}
            <button disabled={!guide || props.busy} onClick={() => guide && void props.onRollJourney('march', guide.id, 'Viagem', 0, undefined, 0, spendHope, useMagicalSuccess)} style={primaryButton}>Rolar Marcha</button>
""")
write(rel,t)

print("PATCH_4939_MAGIC_OK")
