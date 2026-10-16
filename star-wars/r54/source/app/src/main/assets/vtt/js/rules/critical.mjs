import { durableCriticalModifier, sourceBackedTalentRank } from './talents.mjs';

const CRITICAL_INJURIES = Object.freeze([
  {min:1,max:5,healDifficulty:1,severity:'Easy',code:'minor_nick',label:'Minor Nick',description:'Sofre 1 de Tensão.',effect:{immediateStrain:1}},
  {min:6,max:10,healDifficulty:1,severity:'Easy',code:'slowed_down',label:'Slowed Down',description:'Só pode agir no último slot aliado de Iniciativa no próximo turno.',effect:{lastAlliedInitiativeNextTurn:true,duration:'next_turn'}},
  {min:11,max:15,healDifficulty:1,severity:'Easy',code:'sudden_jolt',label:'Sudden Jolt',description:'Derruba o que estiver segurando.',effect:{dropHeldItem:true,immediate:true}},
  {min:16,max:20,healDifficulty:1,severity:'Easy',code:'distracted',label:'Distracted',description:'Não pode realizar manobra gratuita no próximo turno.',effect:{noFreeManeuver:true,duration:'next_turn'}},
  {min:21,max:25,healDifficulty:1,severity:'Easy',code:'off_balance',label:'Off-Balance',description:'Adiciona 1 Contratempo ao próximo teste de perícia.',effect:{setback:1,duration:'next_check'}},
  {min:26,max:30,healDifficulty:1,severity:'Easy',code:'discouraging_wound',label:'Discouraging Wound',description:'Vira 1 Destiny Point luminoso para sombrio (inverso para NPC).',effect:{destinyFlip:true,immediate:true}},
  {min:31,max:35,healDifficulty:1,severity:'Easy',code:'stunned',label:'Stunned',description:'Fica staggered até o fim do próximo turno.',effect:{staggered:true,duration:'next_turn'}},
  {min:36,max:40,healDifficulty:1,severity:'Easy',code:'stinger',label:'Stinger',description:'Aumenta a dificuldade do próximo teste em 1.',effect:{difficultyIncrease:1,duration:'next_check'}},
  {min:41,max:45,healDifficulty:2,severity:'Average',code:'bowled_over',label:'Bowled Over',description:'Fica prone e sofre 1 de Tensão.',effect:{prone:true,immediateStrain:1,immediate:true}},
  {min:46,max:50,healDifficulty:2,severity:'Average',code:'head_ringer',label:'Head Ringer',description:'Aumenta em 1 a dificuldade de testes de Intelecto e Astúcia até o fim do encontro.',effect:{difficultyCharacteristics:['intelecto','astucia'],difficultyIncrease:1,duration:'encounter'}},
  {min:51,max:55,healDifficulty:2,severity:'Average',code:'fearsome_wound',label:'Fearsome Wound',description:'Aumenta em 1 a dificuldade de testes de Presença e Vontade até o fim do encontro.',effect:{difficultyCharacteristics:['presencia','voluntad'],difficultyIncrease:1,duration:'encounter'}},
  {min:56,max:60,healDifficulty:2,severity:'Average',code:'agonizing_wound',label:'Agonizing Wound',description:'Aumenta em 1 a dificuldade de testes de Fortaleza e Agilidade até o fim do encontro.',effect:{difficultyCharacteristics:['fortaleza','agilidad'],difficultyIncrease:1,duration:'encounter'}},
  {min:61,max:65,healDifficulty:2,severity:'Average',code:'slightly_dazed',label:'Slightly Dazed',description:'Fica disoriented até o fim do encontro.',effect:{setback:1,duration:'encounter'}},
  {min:66,max:70,healDifficulty:2,severity:'Average',code:'scattered_senses',label:'Scattered Senses',description:'Remove todos os dados de Benefício dos testes até o fim do encontro.',effect:{removeBoost:true,duration:'encounter'}},
  {min:71,max:75,healDifficulty:2,severity:'Average',code:'hamstrung',label:'Hamstrung',description:'Perde a manobra gratuita até o fim do encontro.',effect:{noFreeManeuver:true,duration:'encounter'}},
  {min:76,max:80,healDifficulty:2,severity:'Average',code:'overpowered',label:'Overpowered',description:'O atacante pode realizar imediatamente outro ataque gratuito com o mesmo pool.',effect:{freeRepeatAttack:true,immediate:true}},
  {min:81,max:85,healDifficulty:2,severity:'Average',code:'winded',label:'Winded',description:'Não pode sofrer Tensão voluntariamente para habilidades ou manobras adicionais até o fim do encontro.',effect:{cannotVoluntarilySufferStrain:true,duration:'encounter'}},
  {min:86,max:90,healDifficulty:2,severity:'Average',code:'compromised',label:'Compromised',description:'Aumenta em 1 a dificuldade de todos os testes de perícia até o fim do encontro.',effect:{difficultyIncrease:1,duration:'encounter'}},
  {min:91,max:95,healDifficulty:3,severity:'Hard',code:'at_the_brink',label:'At the Brink',description:'Sofre 1 de Tensão cada vez que realiza uma ação.',effect:{strainPerAction:1,duration:'until_healed'}},
  {min:96,max:100,healDifficulty:3,severity:'Hard',code:'crippled',label:'Crippled',description:'Um membro fica inutilizado até ser curado/substituído; testes que o exigem aumentam a dificuldade em 1.',effect:{crippledLimb:true,duration:'until_healed'}},
  {min:101,max:105,healDifficulty:3,severity:'Hard',code:'maimed',label:'Maimed',description:'Um membro é perdido permanentemente; ações que o exigem não podem ser realizadas e as demais recebem 1 Contratempo.',effect:{maimedLimb:true,setback:1,duration:'permanent'}},
  {min:106,max:110,healDifficulty:3,severity:'Hard',code:'horrific_injury',label:'Horrific Injury',description:'Uma característica aleatória é tratada como 1 ponto menor até o crítico ser curado.',effect:{temporaryCharacteristicPenalty:1,duration:'until_healed'}},
  {min:111,max:115,healDifficulty:3,severity:'Hard',code:'temporarily_lame',label:'Temporarily Lame',description:'Não pode realizar mais de uma manobra por turno até ser curado.',effect:{maxManeuvers:1,duration:'until_healed'}},
  {min:116,max:120,healDifficulty:3,severity:'Hard',code:'blinded',label:'Blinded',description:'Aumenta duas vezes a dificuldade de todos os testes; Percepção e Vigilância, três vezes.',effect:{difficultyUpgrades:2,perceptionVigilanceUpgrades:3,duration:'until_healed'}},
  {min:121,max:125,healDifficulty:3,severity:'Hard',code:'knocked_senseless',label:'Knocked Senseless',description:'Fica staggered pelo restante do encontro.',effect:{staggered:true,duration:'encounter'}},
  {min:126,max:130,healDifficulty:4,severity:'Daunting',code:'gruesome_injury',label:'Gruesome Injury',description:'Uma característica aleatória é reduzida permanentemente em 1, mínimo 1.',effect:{permanentCharacteristicPenalty:1,immediate:true}},
  {min:131,max:140,healDifficulty:4,severity:'Daunting',code:'bleeding_out',label:'Bleeding Out',description:'No início de cada turno sofre 1 Ferimento e 1 Tensão; a cada 5 Ferimentos além do limiar sofre outro crítico.',effect:{bleedingOut:true,duration:'until_healed'}},
  {min:141,max:150,healDifficulty:4,severity:'Daunting',code:'end_is_nigh',label:'The End is Nigh',description:'Morre após o último slot de Iniciativa da próxima rodada.',effect:{deathNextRound:true,duration:'until_resolved'}},
  {min:151,max:Infinity,healDifficulty:null,severity:'Dead',code:'dead',label:'Dead',description:'Morte completa e definitiva.',effect:{dead:true,immediate:true}}
]);

const CHARACTERISTIC_D10 = Object.freeze(['fortaleza','fortaleza','fortaleza','agilidad','agilidad','agilidad','intelecto','astucia','presencia','voluntad']);

function clone(character){ if(!character) throw new Error('Nenhum personagem carregado.'); return structuredClone(character); }
function int(value,label,min=0){ const n=Number(value); if(!Number.isInteger(n)||n<min) throw new Error(`${label} inválido.`); return n; }
function rollD100(rng){ return Math.min(100,Math.max(1,Math.floor(Number(rng())*100)+1)); }
function randomCharacteristic(rng){ const n=Math.min(9,Math.max(0,Math.floor(Number(rng())*10))); return CHARACTERISTIC_D10[n]; }
function findRule(total){ return CRITICAL_INJURIES.find(x=>total>=x.min && total<=x.max) ?? CRITICAL_INJURIES.at(-1); }
function activeInjury(i){ return i && i.active!==false; }

export function criticalInjuryTable(){ return CRITICAL_INJURIES.map(x=>structuredClone(x)); }
export function starterCriticalRules(){ return criticalInjuryTable(); }

export function addStarterCritical(character, reason='Ferimento crítico', options={}) {
  if (reason && typeof reason==='object') { options=reason; reason=options.reason ?? 'Ferimento crítico'; }
  const next=clone(character);
  next.criticalInjuries=Array.isArray(next.criticalInjuries)?next.criticalInjuries:[];
  const rng=typeof options.rng==='function'?options.rng:Math.random;
  const baseRoll=options.roll===undefined?rollD100(rng):Math.min(100,Math.max(1,int(options.roll,'Rolagem de crítico',1)));
  const existingModifier=next.criticalInjuries.length*10;
  const externalModifier=int(options.modifier??0,'Modificador de crítico');
  const durableModifier=durableCriticalModifier(next);
  const total=Math.max(1,baseRoll+existingModifier+externalModifier+durableModifier);
  // AoR Commando / Unstoppable: a final 1 is NOT a Critical Injury.
  // Return before immediate strain, permanent stat loss or death can be applied.
  if(total===1 && sourceBackedTalentRank(next,'UNSTOPPABLE')>0)
    return {character:next,added:false,prevented:true,reason:String(reason),roll:baseRoll,total,talent:'UNSTOPPABLE'};
  const rule=findRule(total);
  // P179: reactive Medicine is checked before a Critical Injury takes effect.
  // The callback is supplied only by the validated combat controller; it is
  // never inferred from an untrusted save or from an ordinary talent label.
  const interception=typeof options.onBeforeCritical==='function'?options.onBeforeCritical():null;
  if(interception?.prevented===true)return {character:next,added:false,prevented:true,
    reason:String(reason),roll:baseRoll,total,talent:'ITS_NOT_THAT_BAD',interception};
  const injury={
    id:`crit_${next.criticalInjuries.length+1}_${baseRoll}_${total}`,
    code:rule.code,label:rule.label,description:rule.description,severity:rule.severity,
    healDifficulty:rule.healDifficulty,baseRoll,total,existingModifier,externalModifier,durableModifier,
    reason:String(reason??'Ferimento crítico'),effect:structuredClone(rule.effect),active:true,
    pending:rule.effect?.duration==='next_check',turnsRemaining:rule.effect?.duration==='next_turn'?1:null
  };
  if (rule.code==='horrific_injury' || rule.code==='gruesome_injury') injury.characteristicAffected=randomCharacteristic(rng);
  if (rule.effect?.immediateStrain) next.derived.strain.current += Number(rule.effect.immediateStrain);
  if (rule.code==='gruesome_injury') {
    const key=injury.characteristicAffected;
    next.characteristics[key]=Math.max(1,Number(next.characteristics[key]??1)-1);
  }
  next.criticalInjuries.push(injury);
  return {character:next,added:true,injury,roll:baseRoll,total};
}

export function criticalCheckModifiers(character,{skillKey=null,characteristicKey=null,consume=false}={}){
  if(!character) return {character,setback:0,difficultyIncrease:0,difficultyUpgrades:0,removeBoost:false,characteristicPenalty:0,cannotAct:false,cannotVoluntarilySufferStrain:false,maxManeuvers:null,strainPerAction:0};
  const next=clone(character);
  const out={setback:0,difficultyIncrease:0,difficultyUpgrades:0,removeBoost:false,characteristicPenalty:0,cannotAct:false,cannotVoluntarilySufferStrain:false,maxManeuvers:null,noFreeManeuver:false,lastAlliedInitiativeNextTurn:false,strainPerAction:0};
  // Heroic Fortitude: a Destiny flip ignores ongoing Critical Injury CHECK
  // penalties on Brawn/Agility checks for this encounter, not injuries, death,
  // stagger, involuntary strain or restrictions on maneuvering.
  const heroicCheck=next.combatEffects?.heroicFortitude === true && sourceBackedTalentRank(next,'HERFORT') > 0 &&
    ['fortaleza','agilidad'].includes(characteristicKey);
  const steelyCheck=next.combatEffects?.steelyNerves === true && sourceBackedTalentRank(next,'STEELYNERVES') > 0 &&
    ['presencia','voluntad'].includes(characteristicKey);
  const mentalCheck=next.combatEffects?.mentalFortress === true && sourceBackedTalentRank(next,'MENTALFORT') > 0 &&
    ['intelecto','astucia'].includes(characteristicKey);
  const ignoreCriticalCheck=heroicCheck||steelyCheck||mentalCheck;
  for(const injury of next.criticalInjuries??[]){
    if(!activeInjury(injury)) continue;
    const e=injury.effect??{};
    if(e.staggered) out.cannotAct=true;
    if(e.cannotVoluntarilySufferStrain) out.cannotVoluntarilySufferStrain=true;
    if(e.noFreeManeuver) out.noFreeManeuver=true;
    if(e.lastAlliedInitiativeNextTurn) out.lastAlliedInitiativeNextTurn=true;
    if(Number.isInteger(e.maxManeuvers)) out.maxManeuvers=out.maxManeuvers===null?e.maxManeuvers:Math.min(out.maxManeuvers,e.maxManeuvers);
    out.strainPerAction += Number(e.strainPerAction??0);
    if(!ignoreCriticalCheck){
      if(e.removeBoost) out.removeBoost=true;
      if(e.setback && (e.duration!=='next_check' || injury.pending!==false)) out.setback += Number(e.setback);
      if(e.difficultyIncrease && (!e.difficultyCharacteristics || e.difficultyCharacteristics.includes(characteristicKey)) && (e.duration!=='next_check' || injury.pending!==false)) out.difficultyIncrease += Number(e.difficultyIncrease);
      if(e.difficultyUpgrades) out.difficultyUpgrades += (['percepcion','alerta'].includes(skillKey) && e.perceptionVigilanceUpgrades)?Number(e.perceptionVigilanceUpgrades):Number(e.difficultyUpgrades);
      if(e.temporaryCharacteristicPenalty && injury.characteristicAffected===characteristicKey) out.characteristicPenalty += Number(e.temporaryCharacteristicPenalty);
    }
    if(e.duration==='next_check' && injury.pending!==false && consume && !ignoreCriticalCheck) injury.pending=false;
  }
  return {character:next,...out};
}

export function criticalSetbackForNextCheck(character,{consume=false}={}){
  const r=criticalCheckModifiers(character,{consume});
  return {character:r.character,setback:r.setback};
}

export function expirePlayerTurnCriticalEffects(character){
  const next=clone(character);
  for(const injury of next.criticalInjuries??[]){
    if(!activeInjury(injury) || injury.effect?.duration!=='next_turn') continue;
    injury.turnsRemaining=Math.max(0,Number(injury.turnsRemaining??1)-1);
    if(injury.turnsRemaining===0) injury.active=false;
  }
  return next;
}

export function expireEncounterCriticalEffects(character){
  const next=clone(character);
  for(const injury of next.criticalInjuries??[]) if(activeInjury(injury) && injury.effect?.duration==='encounter') injury.active=false;
  return next;
}

export function healCriticalInjury(character,injuryId){
  const next=clone(character);
  const index=(next.criticalInjuries??[]).findIndex(i=>i.id===injuryId);
  if(index<0) throw new Error('Ferimento Crítico não encontrado.');
  const [injury]=next.criticalInjuries.splice(index,1);
  return {character:next,injury};
}

export function isCriticalIncapacitated(character){
  return Boolean(character?.criticalInjuries?.some(i=>activeInjury(i) && i.code==='dead'));
}

export function isDeadFromCritical(character){ return Boolean(character?.criticalInjuries?.some(i=>i.code==='dead')); }
