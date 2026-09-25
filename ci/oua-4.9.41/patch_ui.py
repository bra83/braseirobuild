from pathlib import Path
import shutil, sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_ui.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text):
    p=root/rel; p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

templates=Path(__file__).resolve().parent/"templates"
for src_rel,dst_rel in [
    ("components/CharacterPathsCreator.tsx","components/CharacterPathsCreator.tsx"),
    ("components/TreasureIndexPanel.tsx","components/TreasureIndexPanel.tsx"),
]:
    shutil.copy2(templates/src_rel,root/dst_rel)

# Mechanics Hub gets Treasure Index.
rel="components/MechanicsHub.tsx"
t=read(rel)
t=req(t,
"import { supplementBonusDice, supplementRollMode } from '../lib/rules/supplementMechanics';",
"import { supplementBonusDice, supplementRollMode } from '../lib/rules/supplementMechanics';\nimport { TreasureIndexPanel } from './TreasureIndexPanel';",
"Treasure panel import")
t=req(t,
"    <SupplementRulesPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
"    <SupplementRulesPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>\n    <TreasureIndexPanel masterState={masterState} characters={characters} onApplyCommands={onApplyCommands}/>",
"Treasure panel render")
write(rel,t)

# App integrates Character Paths creator + party-delta reconciliation.
rel="App.tsx"
app=read(rel)
app=req(app,
"import { CharacterRegistryView } from './components/CharacterRegistryView';",
"import { CharacterRegistryView } from './components/CharacterRegistryView';\nimport { CharacterPathsCreator } from './components/CharacterPathsCreator';",
"Character Paths UI import")
app=app.replace(
"import { createRandomCharacter } from './lib/characterFactory';",
"import { createCharacterFromPath, createRandomCharacter, type CharacterPathCreationOptions } from './lib/characterFactory';",
1)
app=app.replace(
"import { sheetFeatOneBecomesEye } from './lib/characterPaths';",
"import { characterPathCompanyDeltas, sheetFeatOneBecomesEye } from './lib/characterPaths';",
1)

helper="""function reconcileCharacterPathPartyEffects(campaign:CampaignState,characters:Character[],partyCharacterIds:string[]):CampaignState {
  const master=normalizeMasterState(campaign);
  const next=characterPathCompanyDeltas(characters,partyCharacterIds);
  const oldFellowship=Number(master.company.characterPathFellowshipDelta)||0;
  const oldEye=Number(master.company.characterPathEyeDelta)||0;
  const fellowshipMax=Math.max(0,(Number(master.company.fellowshipMax)||0)-oldFellowship+next.fellowship);
  const fellowshipCurrent=Math.max(0,Math.min(fellowshipMax,(Number(master.company.fellowshipCurrent)||0)-oldFellowship+next.fellowship));
  const eyeAwareness=Math.max(0,(Number(master.company.eyeAwareness)||0)-oldEye+next.eye);
  return {
    ...campaign,
    masterState:{
      ...master,
      company:{
        ...master.company,
        fellowshipMax,
        fellowshipCurrent,
        eyeAwareness,
        characterPathFellowshipDelta:next.fellowship,
        characterPathEyeDelta:next.eye,
      },
      updatedAt:Date.now(),
    },
    updatedAt:Date.now(),
  };
}

"""
app=req(app,"function immersiveJourneyUiEnabled(): boolean {",helper+"function immersiveJourneyUiEnabled(): boolean {","party path reconciler")

# ActivateCharacter can add a hero to party, so reconcile.
app=req(app,
"""    const updated: CampaignState = {
      ...base,
      activeCharacterId: selected.id,
      partyCharacterIds: Array.from(new Set([...(base.partyCharacterIds || []), selected.id])),
      characterSheet: normalizeCharacterSheet(selected.sheet),
      updatedAt: Date.now(),
    };
    await persistCampaign(updated, 'trocar personagem ativo');""",
"""    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds || []), selected.id]));
    const updated = reconcileCharacterPathPartyEffects({
      ...base,
      activeCharacterId: selected.id,
      partyCharacterIds,
      characterSheet: normalizeCharacterSheet(selected.sheet),
      updatedAt: Date.now(),
    },characters,partyCharacterIds);
    await persistCampaign(updated, 'trocar personagem ativo');""",
"activate character path effects")

# Toggle party.
app=req(app,
"""    await persistCampaign({
      ...base,
      activeCharacterId,
      partyCharacterIds,
      characterSheet: activeSheet,
      updatedAt: Date.now(),
    }, isInGame ? 'retirar herói da companhia' : 'adicionar herói à companhia');""",
"""    const updatedPartyCampaign=reconcileCharacterPathPartyEffects({
      ...base,
      activeCharacterId,
      partyCharacterIds,
      characterSheet: activeSheet,
      updatedAt: Date.now(),
    },list,partyCharacterIds);
    await persistCampaign(updatedPartyCampaign, isInGame ? 'retirar herói da companhia' : 'adicionar herói à companhia');""",
"toggle party path effects")

# Add explicit Character Paths creator after ordinary random creator.
anchor="""  const importRegistryCharacterJson = async (json: string) => {"""
handler="""  const createCharacterPathCharacter = async (options:CharacterPathCreationOptions) => {
    const base=campaignRef.current;
    if(!base)return;
    const master=normalizeMasterState(base);
    const character=createCharacterFromPath({
      ...options,
      existingNames:characters.map(item=>item.sheet.identity.name||item.name),
      rulesMode:master.company.rulesMode==='strider'?'strider':'standard',
    });
    character.createdAt=Date.now();
    character.updatedAt=Date.now();
    const list=[...characters,character];
    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds||[]),character.id]));
    const updatedCampaign=reconcileCharacterPathPartyEffects({
      ...base,
      activeCharacterId:character.id,
      partyCharacterIds,
      characterSheet:character.sheet,
      updatedAt:Date.now(),
    },list,partyCharacterIds);
    const persisted=await saveCanonicalTransaction({
      campaign:updatedCampaign,
      characters:list,
      reason:'criar herói por Trilhas de Personagens',
    });
    campaignRef.current=persisted;
    setCampaign(persisted);
    setCharacters(list);
    setActiveTab('personagens');
  };

"""
app=req(app,anchor,handler+anchor,"Character Paths creation handler")

# Imported path characters also reconcile party effects.
app=req(app,
"""    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, activeCharacterId: character.id, partyCharacterIds: Array.from(new Set([...(base.partyCharacterIds || []), character.id])), characterSheet: character.sheet, updatedAt: Date.now() },
      characters: list,
      reason: 'importar ficha JSON validada',
    });""",
"""    const partyCharacterIds=Array.from(new Set([...(base.partyCharacterIds||[]),character.id]));
    const importedCampaign=reconcileCharacterPathPartyEffects({...base,activeCharacterId:character.id,partyCharacterIds,characterSheet:character.sheet,updatedAt:Date.now()},list,partyCharacterIds);
    const persisted = await saveCanonicalTransaction({
      campaign: importedCampaign,
      characters: list,
      reason: 'importar ficha JSON validada',
    });""",
"import path effects")

# Remove path hero reconciles after removal.
app=req(app,
"""    const persisted = await saveCanonicalTransaction({
      campaign: { ...base, activeCharacterId: activeCharacter.id, partyCharacterIds: Array.from(new Set([activeCharacter.id, ...partyCharacterIds])), characterSheet: normalizeCharacterSheet(activeCharacter.sheet), updatedAt: Date.now() },
      characters: list,
      deleteCharacterIds: [characterId],
      reason: 'apagar personagem',
    });""",
"""    const finalParty=Array.from(new Set([activeCharacter.id,...partyCharacterIds]));
    const removedCampaign=reconcileCharacterPathPartyEffects({...base,activeCharacterId:activeCharacter.id,partyCharacterIds:finalParty,characterSheet:normalizeCharacterSheet(activeCharacter.sheet),updatedAt:Date.now()},list,finalParty);
    const persisted = await saveCanonicalTransaction({
      campaign: removedCampaign,
      characters: list,
      deleteCharacterIds: [characterId],
      reason: 'apagar personagem',
    });""",
"remove path effects")

# Character registry view becomes a fragment with the supplement creator above it.
old="""      case 'personagens': return <CharacterRegistryView characters={characters} activeCharacterId={campaign.activeCharacterId} partyCharacterIds={campaign.partyCharacterIds || []} onSelectCharacter={activateCharacter} onToggleCharacterInGame={toggleCharacterInGame} onCreateRandom={createCompleteRandomCharacter} onCreateBlank={createBlankCharacter} onSaveCharacter={saveRegistryCharacter} onDeleteCharacter={removeRegistryCharacter} onImportCharacterJson={importRegistryCharacterJson} />;"""
new="""      case 'personagens': return <><CharacterPathsCreator rulesMode={masterState.company.rulesMode==='strider'?'strider':'standard'} onCreate={createCharacterPathCharacter}/><CharacterRegistryView characters={characters} activeCharacterId={campaign.activeCharacterId} partyCharacterIds={campaign.partyCharacterIds || []} onSelectCharacter={activateCharacter} onToggleCharacterInGame={toggleCharacterInGame} onCreateRandom={createCompleteRandomCharacter} onCreateBlank={createBlankCharacter} onSaveCharacter={saveRegistryCharacter} onDeleteCharacter={removeRegistryCharacter} onImportCharacterJson={importRegistryCharacterJson} /></>;"""
app=req(app,old,new,"Character Paths registry render")
write(rel,app)

print("PATCH_4941_UI_OK")
