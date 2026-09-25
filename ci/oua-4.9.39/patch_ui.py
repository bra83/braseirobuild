from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

for src_rel,dst_rel in [
    ("templates/components/MechanicsHub.tsx","components/MechanicsHub.tsx"),
    ("templates/components/SupplementRulesPanel.tsx","components/SupplementRulesPanel.tsx"),
    ("templates/components/CombatRulesPanel.tsx","components/CombatRulesPanel.tsx"),
    ("templates/components/JourneyResolutionPanel.tsx","components/JourneyResolutionPanel.tsx"),
]:
    src=here/src_rel; dst=root/dst_rel; dst.parent.mkdir(parents=True,exist_ok=True); shutil.copy2(src,dst)

# ---------------------------------------------------------------------------
# App: Hope + Inspired on Journey rolls, Notable Events, mode-aware creation.
# ---------------------------------------------------------------------------
rel="App.tsx"
app=read(rel)

app=app.replace(
"import { normalizeEquipmentMechanics } from './lib/rules/equipment';",
"import { automaticInspired, normalizeEquipmentMechanics } from './lib/rules/equipment';"
)
app=app.replace(
"import { findSkill, targetNumberForSkill } from './lib/characterRules';",
"import { attributeForSkill, findSkill, targetNumberForSkill } from './lib/characterRules';"
)
if "from './lib/rules/supplementMechanics'" not in app:
    app=req(app,
        "import { shadowAtLimit } from './lib/rules/shadow';",
        "import { shadowAtLimit } from './lib/rules/shadow';\nimport { supplementBonusDice, supplementInspired, supplementRollMode } from './lib/rules/supplementMechanics';",
        "App supplement mechanics import"
    )

old="""function rollJourneySkill(character: Character, skillName: string, extraDice = 0, forcedMode?: 'favoured'|'normal'|'ill-favoured') {
  const skill = findSkill(character.sheet, skillName);
  const mode = forcedMode || (shadowAtLimit(character.sheet) ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal');
  return rollOneRing(
    Math.max(0, Number(skill?.rank) || 0) + Math.trunc(extraDice),
    0,
    targetNumberForSkill(character.sheet, skillName),
    skillName,
    mode,
    Boolean(character.sheet.attributes.weary),
    Boolean(character.sheet.attributes.miserable),
  );
}"""
new="""function rollJourneySkill(
  character: Character,
  skillName: string,
  extraDice = 0,
  forcedMode?: 'favoured'|'normal'|'ill-favoured',
  hopeDice = 0,
) {
  const skill = findSkill(character.sheet, skillName);
  const baseMode = shadowAtLimit(character.sheet) ? 'ill-favoured' : skill?.favorite ? 'favoured' : 'normal';
  const mode = forcedMode || supplementRollMode(character.sheet, { kind: 'journey', skill: skillName, journeyMode: character.sheet.other.rulesMode === 'strider' ? 'strider' : 'standard' }, baseMode);
  const inspired = automaticInspired(character.sheet) || supplementInspired(character.sheet, { kind: 'journey', skill: skillName, journeyMode: character.sheet.other.rulesMode === 'strider' ? 'strider' : 'standard' });
  const effectiveHopeDice = hopeDice > 0 && inspired ? Math.max(2, hopeDice) : hopeDice;
  const supplementDice = supplementBonusDice(character.sheet, { kind: 'journey', skill: skillName, journeyMode: character.sheet.other.rulesMode === 'strider' ? 'strider' : 'standard' });
  return rollOneRing(
    Math.max(0, Number(skill?.rank) || 0) + Math.trunc(extraDice) + effectiveHopeDice + supplementDice,
    0,
    targetNumberForSkill(character.sheet, skillName),
    skillName,
    mode,
    Boolean(character.sheet.attributes.weary),
    Boolean(character.sheet.attributes.miserable),
  );
}"""
app=req(app,old,new,"Journey roll Hope/Inspired")

old="""    mountVigour = 0,
  ) => {
    const base = campaignRef.current;
    if (!base) return;
    const hero = characters.find(character => character.id === characterId);
    if (!hero) throw new Error('Herói da rolagem não encontrado.');
    const roll = rollJourneySkill(hero, skill, extraDice, forcedMode);
    const commands: MasterCommand[] = [{
      type: 'player_record_journey_roll',
      kind,
      targetCharacterId: hero.id,
      skill,
      targetNumber: targetNumberForSkill(hero.sheet, skill),
      roll,
      mountVigour,
      reason: skill + ' rolada manualmente pelo jogador.',
    }];
    const master = normalizeMasterState(base);"""
new="""    mountVigour = 0,
    spendHope = false,
  ) => {
    const base = campaignRef.current;
    if (!base) return;
    const hero = characters.find(character => character.id === characterId);
    if (!hero) throw new Error('Herói da rolagem não encontrado.');
    const master = normalizeMasterState(base);
    if (spendHope && hero.sheet.attributes.hopeCurrent <= 0) throw new Error('Esperança insuficiente.');
    const roll = rollJourneySkill(hero, skill, extraDice, forcedMode, spendHope ? 1 : 0);
    const commands: MasterCommand[] = [];
    if (spendHope) {
      commands.push({
        type: 'spend_hope',
        targetCharacterId: hero.id,
        attribute: attributeForSkill(skill),
        reason: 'Esperança gasta explicitamente na rolagem de Jornada.',
      });
    }
    commands.push({
      type: 'player_record_journey_roll',
      kind,
      targetCharacterId: hero.id,
      skill,
      targetNumber: targetNumberForSkill(hero.sheet, skill),
      roll,
      mountVigour,
      reason: skill + ' rolada manualmente pelo jogador.',
    });"""
app=req(app,old,new,"Journey manual Hope")

# Insert Notable Event resolver before standard Apply Event.
anchor="""  const handleApplyJourneyEvent = async () => {"""
notable="""  const handleOpenJourneyNotable = async () => {
    const base = campaignRef.current;
    if (!base) return;
    const event = normalizeMasterState(base).journey.pendingEvent;
    if (!event?.notable) throw new Error('O Evento atual não é um Encontro Notável.');
    setIsStoryLoading(true);
    setStoryError(null);
    await processTurn('[ENCONTRO NOTÁVEL DA JORNADA] Desenvolva este encontro como uma cena estendida e significativa. Não reduza o encontro a uma única rolagem, não resolva automaticamente o resultado geral e não avance Marcha/Evento/Jornada. O jogador decidirá depois se o desfecho global foi favorável ou desfavorável.', false, base);
  };

  const handleResolveJourneyNotable = async (success: boolean) => {
    const base = campaignRef.current;
    if (!base) return;
    const master = normalizeMasterState(base);
    const event = master.journey.pendingEvent;
    if (!event?.notable) throw new Error('O Evento atual não é um Encontro Notável.');
    const heroId = master.company.journeyRoles[event.targetRole] || base.partyCharacterIds?.[0] || base.activeCharacterId;
    const hero = characters.find(character => character.id === heroId);
    if (!hero) throw new Error('Herói do Encontro Notável não encontrado.');
    const syntheticRoll = {
      id: 'notable_' + String(Date.now()),
      type: 'one-ring',
      notation: 'Encontro Notável',
      results: [],
      modifier: 0,
      total: success ? targetNumberForSkill(hero.sheet, event.skill) : 0,
      timestamp: Date.now(),
      featResult: success ? 12 : 11,
      successResults: [],
      targetNumber: targetNumberForSkill(hero.sheet, event.skill),
      skillName: event.skill,
      success,
      successLevel: success ? 1 : 0,
      tengwarCount: 0,
      characterId: hero.id,
      characterName: hero.sheet.identity.name || hero.name,
    };
    await commitJourneyAction([
      {
        type: 'player_record_journey_roll',
        kind: 'event',
        targetCharacterId: hero.id,
        skill: event.skill,
        targetNumber: targetNumberForSkill(hero.sheet, event.skill),
        roll: syntheticRoll,
        reason: success ? 'Encontro Notável resolvido favoravelmente.' : 'Encontro Notável resolvido desfavoravelmente.',
      },
      {
        type: 'player_confirm_journey_event',
        reason: 'Consequência do Encontro Notável confirmada pelo jogador.',
      },
    ], 'resolver Encontro Notável', success ? 'Encontro Notável · favorável' : 'Encontro Notável · desfavorável');
  };

"""
app=req(app,anchor,notable+anchor,"Notable Journey handler")

# Elrond Patron: +1 modifier to numeric Feat-die results used to determine Journey Events.
app=req(
    app,
    """    const targetDie = secureRoll(1, 6);
    const featResults = journeyFeatResults(region);
    await commitJourneyAction([{""",
    """    const targetDie = secureRoll(1, 6);
    const rawFeatResults = journeyFeatResults(region);
    const featResults = master.company.elrondJourneyFeatBonusActive
      ? rawFeatResults.map(value => value >= 1 && value <= 10 ? Math.min(10, value + 1) : value)
      : rawFeatResults;
    await commitJourneyAction([{""",
    "Elrond Journey Feat bonus",
)

# Mode-aware random character creation.
app=app.replace(
"const character = createRandomCharacter(characters.map(item => item.sheet.identity.name || item.name));",
"const character = createRandomCharacter(characters.map(item => item.sheet.identity.name || item.name), { rulesMode: normalizeMasterState(base).company.rulesMode === 'strider' ? 'strider' : 'standard' });"
)

# Wire Notable callback.
app=req(app,
"            onApplyEvent={handleApplyJourneyEvent}\n",
"            onApplyEvent={handleApplyJourneyEvent}\n            onResolveNotableEvent={handleResolveJourneyNotable}\n",
"Journey notable prop")
write(rel,app)

# ---------------------------------------------------------------------------
# Tactical combat: expose Skirmish only in Strider mode.
# ---------------------------------------------------------------------------
rel="components/CombatTacticalView.tsx"
combat=read(rel)
combat=combat.replace(
"  forward: 'Avançada', open: 'Aberta', defensive: 'Defensiva', rearward: 'Retaguarda',",
"  forward: 'Avançada', open: 'Aberta', defensive: 'Defensiva', rearward: 'Retaguarda', skirmish: 'Escaramuça',"
)
combat=req(combat,
"""<option value="forward">Avançada</option><option value="open">Aberta</option><option value="defensive">Defensiva</option><option value="rearward">Retaguarda</option></select>""",
"""<option value="forward">Avançada</option><option value="open">Aberta</option><option value="defensive">Defensiva</option><option value="rearward">Retaguarda</option>{masterState.company.rulesMode === 'strider' && <option value="skirmish">Escaramuça</option>}</select>""",
"Skirmish stance option")
write(rel,combat)

print("PATCH_4939_UI_OK")
