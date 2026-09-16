import { WWN_SPELL_EFFECT_CONTRACT_CATALOG, spellEffectCoverageAudit } from './wwn-spell-effect-runtime.js';
import { WWN_ART_EFFECT_CONTRACT_CATALOG, artEffectCoverageAudit } from './wwn-art-effect-runtime.js';
import { WWN_FACTION_ASSET_EFFECTS } from './wwn-faction-asset-effects.js';

const freeze=v=>Object.freeze(v);
const terminalContract=x=>Boolean(x&&typeof x.classification==='string'&&x.classification.length>0&&(x.resolverId||x.classification==='GM_ADJUDICATION_REQUIRED'));
const terminalFaction=x=>Boolean(x&&typeof x.effectKind==='string'&&x.effectKind.length>0&&typeof x.automation==='string'&&x.automation.length>0);

export const CLOSURE071_DOWNSTREAM_TERMINAL_CONTRACTS=freeze({
  SCORN_THE_FETTERS_OF_EARTH_SPATIAL:freeze({status:'EXPLICIT_CONTEXT_CONTRACT',contract:'EXPLICIT_3D_GEOMETRY_REQUIRED_WHEN_3D_MOVEMENT_IS_USED'}),
  LIKE_THE_STONES_WATER_APERTURE:freeze({status:'EXPLICIT_CONTEXT_CONTRACT',contract:'EXPLICIT_MOUSE_SIZED_APERTURE_EVIDENCE_REQUIRED'}),
  LIKE_THE_STONES_FIRE_AURA:freeze({status:'EXECUTABLE_WITH_EXPLICIT_INPUT',contract:'EXPLICIT_MELEE_COVERAGE_AND_DAMAGE_ROLL_POLICY_REQUIRED'}),
  AUGMENT_MORTAL_VITALITY_INSTANT_UNDO:freeze({status:'EXECUTABLE',contract:'STATEFUL_ONCE_PER_SCENE_EXPLICIT_INJURY_DAMAGE'}),
  TUMEFACTION_GENERIC_VIGOROUS_ACTION:freeze({status:'EXECUTABLE',contract:'EXPLICIT_VIGOROUS_ACTION_EVENT_MAX_ONCE_PER_ROUND'}),
});

export function closure071FinalizationAudit(){
  const spells=spellEffectCoverageAudit(),arts=artEffectCoverageAudit();
  const spellTerminal=WWN_SPELL_EFFECT_CONTRACT_CATALOG.filter(terminalContract).length;
  const artTerminal=WWN_ART_EFFECT_CONTRACT_CATALOG.filter(terminalContract).length;
  const factionEntries=Object.entries(WWN_FACTION_ASSET_EFFECTS),factionTerminal=factionEntries.filter(([,v])=>terminalFaction(v)).length;
  const downstreamEntries=Object.entries(CLOSURE071_DOWNSTREAM_TERMINAL_CONTRACTS),downstreamTerminal=downstreamEntries.filter(([,v])=>Boolean(v?.status&&v?.contract)).length;
  const closed=spells.spellCount===83&&spellTerminal===83&&arts.artCount===202&&artTerminal===202&&factionEntries.length===59&&factionTerminal===59&&downstreamTerminal===downstreamEntries.length;
  return freeze({
    status:closed?'CLOSED_SOURCE_RUNTIME':'INCOMPLETE',
    spells:freeze({...spells,terminalCount:spellTerminal}),
    arts:freeze({...arts,terminalCount:artTerminal}),
    factionEffects:freeze({count:factionEntries.length,terminalCount:factionTerminal}),
    downstream:freeze({count:downstreamEntries.length,terminalCount:downstreamTerminal,contracts:CLOSURE071_DOWNSTREAM_TERMINAL_CONTRACTS}),
    noUnclassifiedContent:closed,
  });
}
