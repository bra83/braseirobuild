/** P186: real narrative-dice checks for SWE09 Grand Prix. Never invent a die face. */
import { buildPositivePool, rollPool, resolveSymbols, diceFacesForTests } from '../rules/narrativeDice.mjs';

const isInt=(x,min,max)=>Number.isSafeInteger(x)&&x>=min&&x<=max;
const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
export function rollJewelGrandPrixPilotCheck(character, requirements, rng=Math.random, difficultyUpgrades=0) {
  if (!character || typeof character!=='object') throw new Error('Piloto real obrigatório.');
  const characteristicRank=character.characteristics?.agilidad;
  const skillRank=character.skills?.pilotar_vehiculo_planetario;
  if(!isInt(characteristicRank,0,7)||!isInt(skillRank,0,7))throw new Error('Ficha de piloto inválida.');
  if(!requirements||!isInt(requirements.leg,1,7)||!isInt(requirements.terrainSetbacks,0,12)||!isInt(requirements.missedBuoySetbacks,0,12))throw new Error('Trecho e penalidades inválidos.');
  if(!isInt(difficultyUpgrades,0,3))throw new Error('Melhorias de dificuldade inválidas.');
  const pool={...buildPositivePool(characteristicRank,skillRank),difficulty:3-difficultyUpgrades,challenge:difficultyUpgrades,boost:0,setback:requirements.terrainSetbacks+requirements.missedBuoySetbacks};
  const roll=rollPool(pool,rng);
  return {pilotId:String(character.id??character.name??'piloto'),skillKey:'pilotar_vehiculo_planetario',characteristicRank,skillRank,leg:requirements.leg,difficultyUpgrades,pool,roll};
}
export function validateJewelGrandPrixPilotEvidence(evidence,leg,previousMissed) {
  if(!evidence||typeof evidence!=='object'||Array.isArray(evidence)||!isInt(leg,1,7)||typeof previousMissed!=='boolean')throw new Error('Prova de dados inválida.');
  const {characteristicRank,skillRank,pool,roll}=evidence;
  if(!isInt(characteristicRank,0,7)||!isInt(skillRank,0,7)||evidence.leg!==leg||evidence.skillKey!=='pilotar_vehiculo_planetario'||typeof evidence.pilotId!=='string'||!evidence.pilotId||evidence.pilotId.length>120)throw new Error('Prova de piloto adulterada.');
  const terrain=[0,0,0,2,1,0,0][leg-1];
  const upgrades=evidence.difficultyUpgrades;
  if(!isInt(upgrades,0,3))throw new Error('Melhorias de dificuldade adulteradas.');
  const expectedPool={...buildPositivePool(characteristicRank,skillRank),difficulty:3-upgrades,challenge:upgrades,boost:0,setback:terrain+Number(previousMissed)};
  if(!same(pool,expectedPool)||!roll||!same(roll.pool,expectedPool)||!Array.isArray(roll.faces))throw new Error('Pool do Grande Prêmio adulterado.');
  const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
  let offset=0;
  for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){
    for(let i=0;i<expectedPool[type];i++){
      const face=roll.faces[offset++],faces=diceFacesForTests[type];
      if(!face||face.type!==type||!isInt(face.index,0,faces.length-1)||!same(face.face,faces[face.index]))throw new Error('Face de dado adulterada.');
      for(const [symbol,count] of Object.entries(face.face))raw[symbol]+=count;
    }
  }
  if(offset!==roll.faces.length)throw new Error('Quantidade de dados adulterada.');
  const expected=resolveSymbols(raw);
  for(const key of ['success','netSuccess','netAdvantage','triumph','despair','raw'])if(!same(roll[key],expected[key]))throw new Error('Símbolos do dado adulterados.');
  return true;
}
