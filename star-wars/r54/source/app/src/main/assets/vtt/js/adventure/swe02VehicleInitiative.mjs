// SWE02 printed p.12: Thwheek and each PC roll Cool for the vehicle encounter.
// Dice and ordering are deterministic engine work; this module only records the
// source-backed participants and initiative slots.
import {buildPositivePool,rollPool} from '../rules/narrativeDice.mjs';
import {initiativeEntry,sortInitiative} from '../rules/initiative.mjs';

export const SWE02_VEHICLE_INITIATIVE_SOURCE=Object.freeze({
  productCode:'SWE02',printedPage:12,authority:'OFFICIAL_PDF_TEXT_VERIFIED'
});

export function rollSWE02VehicleInitiative(roster,{rng=Math.random}={}){
  const entries=roster?.entries;
  if(!Array.isArray(entries)||entries.length<1||entries.length>8)
    throw Error('SWE02 p.12: roster de PCs inválido para iniciativa.');
  const seen=new Set(),rolled=[];
  for(const entry of entries){
    const character=entry?.character,id=String(entry?.id??'');
    if(!id||seen.has(id)||!character?.name||!character.characteristics||!character.skills)
      throw Error('SWE02 p.12: cada participante precisa de uma ficha de PC válida e única.');
    seen.add(id);
    const presence=Number(character.characteristics.presencia),cool=Number(character.skills.frialdad??0);
    if(!Number.isSafeInteger(presence)||presence<1||presence>6||!Number.isSafeInteger(cool)||cool<0||cool>5)
      throw Error('SWE02 p.12: atributo Presença ou perícia Frialdade inválido.');
    const check=rollPool(buildPositivePool(presence,cool),rng);
    rolled.push({entry:initiativeEntry({id,side:'player',check,label:character.name}),check});
  }
  // The stat block lists Presence 2 and no Cool ranks for Thwheek.
  const thwheekCheck=rollPool({ability:2,proficiency:0,difficulty:0,challenge:0,boost:0,setback:0},rng);
  rolled.push({entry:initiativeEntry({id:'swe02-thwheek',side:'npc',check:thwheekCheck,label:'Thwheek'}),check:thwheekCheck});
  const ordered=sortInitiative(rolled.map(x=>x.entry));
  const byId=new Map(rolled.map(x=>[x.entry.id,x]));
  return {
    source:SWE02_VEHICLE_INITIATIVE_SOURCE,
    slots:ordered.map((entry,index)=>({index,side:entry.side,actorId:entry.id,label:entry.label,
      successes:entry.successes,advantages:entry.advantages,triumphs:entry.triumphs,
      check:structuredClone(byId.get(entry.id).check)})),
    round:1,index:0,completedRounds:0,actionReceipts:[]
  };
}

export function validateSWE02VehicleInitiative(combat,roster=null){
  const initiative=combat?.swe02Initiative;
  if(combat?.publishedVehicleContext?.id!=='swe02-thwheek-p239')return true;
  if(!combat.active)return true;
  if(!initiative||initiative.source?.printedPage!==12||initiative.source?.productCode!=='SWE02'||
    !Array.isArray(initiative.slots)||initiative.slots.length<2||initiative.slots.length>9||
    !Number.isSafeInteger(initiative.index)||initiative.index<0||initiative.index>=initiative.slots.length||
    initiative.completedRounds!==initiative.round-1||initiative.round!==combat.round||
    initiative.round!==combat.round||initiative.slots.filter(x=>x.actorId==='swe02-thwheek').length!==1)
    throw Error('SWE02 p.12: iniciativa veicular ausente ou inválida.');
  const ids=new Set();
  for(let i=0;i<initiative.slots.length;i++){
    const slot=initiative.slots[i];
    if(slot.index!==i||!['player','npc'].includes(slot.side)||typeof slot.actorId!=='string'||ids.has(slot.actorId)||
      !Number.isSafeInteger(slot.successes)||slot.successes<0||!Number.isSafeInteger(slot.advantages)||slot.advantages<0||
      !Number.isSafeInteger(slot.triumphs)||slot.triumphs<0||!slot.check)
      throw Error('SWE02 p.12: slot de iniciativa adulterado.');
    ids.add(slot.actorId);
  }
  const ordered=[...initiative.slots].sort((a,b)=>b.successes-a.successes||b.advantages-a.advantages||
    b.triumphs-a.triumphs||(a.side==='player'&&b.side==='npc'?-1:a.side==='npc'&&b.side==='player'?1:0));
  if(ordered.some((slot,i)=>slot.actorId!==initiative.slots[i].actorId))
    throw Error('SWE02 p.12: a ordem salva diverge dos resultados de iniciativa.');
  if(initiative.slots.some(x=>x.actorId==='swe02-thwheek'&&x.side!=='npc')||
    initiative.slots.some(x=>x.actorId!=='swe02-thwheek'&&x.side!=='player'))
    throw Error('SWE02 p.12: identidade de participante incompatível com iniciativa.');
  if(roster&&(!Array.isArray(roster.entries)||roster.entries.length!==initiative.slots.length-1||
    roster.entries.some(x=>!initiative.slots.some(slot=>slot.actorId===x.id))))
    throw Error('SWE02 p.12: roster de PCs diverge dos participantes da iniciativa.');
  if(!Array.isArray(initiative.actionReceipts)||initiative.actionReceipts.length>4096||
    initiative.actionReceipts.some(x=>!x||!Number.isSafeInteger(x.round)||x.round<1||
      !Number.isSafeInteger(x.slotIndex)||x.slotIndex<0||x.slotIndex>=initiative.slots.length||
      x.actorId!==initiative.slots[x.slotIndex]?.actorId||!['action','maneuver','attack','crew-action'].includes(x.kind)))
    throw Error('SWE02 p.12: recibo de ação fora da iniciativa.');
  return true;
}
