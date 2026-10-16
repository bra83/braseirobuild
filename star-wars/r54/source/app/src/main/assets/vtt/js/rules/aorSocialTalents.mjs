/** P168 — AoR Diplomat social actions. Persistent, round-scoped effects.
 * This module does not invent NPCs or targets: callers must select real combat
 * opponents or player-created companions in confirmed short range.
 */
import { recoverStrain } from './health.mjs';
import { sufferAdversaryStrain } from './adversaries.mjs';

const int = n => Number.isSafeInteger(n) && n >= 0 && n <= 1000000;
const copy = structuredClone;

export function aorSocialTalentStatus(character, rank) {
  const {base=0,improved=0,supreme=0} = rank;
  return {available:base>0,improved:improved>0,supreme:supreme>0,
    leadership:Number(character?.skills?.liderazgo??0),coercion:Number(character?.skills?.coaccion??0)};
}
export function isShortRange(range) {
  return ['Curto','Interação','Engajado','Engaged','Short'].includes(range);
}
export function activeInspiringBoost(character, round, encounterSerial) {
  const until=character?.combatEffects?.aorInspiringUntilRound;
  const savedSerial=character?.combatEffects?.aorInspiringEncounterSerial;
  return int(until) && int(round) && round>0 && round<=until &&
    int(encounterSerial) && encounterSerial>=1 && savedSerial===encounterSerial ? 1 : 0;
}
export function activeScathingSetback(adversary, round) {
  const until=adversary?.aorScathingUntilRound;
  return int(until) && int(round) && round>0 && round<=until ? 1 : 0;
}
export function applyInspiringRhetoric(member, {strainRecovered=1, untilRound=null, encounterSerial=null}={}) {
  if (!int(strainRecovered)) throw new Error('Recuperação inspiradora inválida.');
  const next=copy(member);
  const result=recoverStrain(next.character,strainRecovered);
  next.character=result.character;
  if(untilRound!==null){
    if(!int(untilRound)||untilRound<1)throw new Error('Duração inspiradora inválida.');
    if(!int(encounterSerial)||encounterSerial<1)throw new Error('Encontro inspirador inválido.');
    next.character.combatEffects={...(next.character.combatEffects??{}),aorInspiringUntilRound:untilRound,aorInspiringEncounterSerial:encounterSerial};
  }
  return {member:next,recovered:result.recovered};
}
export function applyScathingTirade(adversary, {strain=1,untilRound=null}={}) {
  if(!int(strain))throw new Error('Tensão da reprimenda inválida.');
  const result=sufferAdversaryStrain(adversary,strain),next=result.adversary;
  if(untilRound!==null){
    if(!int(untilRound)||untilRound<1)throw new Error('Duração da reprimenda inválida.');
    next.aorScathingUntilRound=untilRound;
  }
  return {...result,adversary:next};
}
export function validateAoRSocialState(state) {
  if(!Array.isArray(state?.group?.companions))throw new Error('Estado de companheiros inválido.');
  if(!Array.isArray(state?.combat?.adversaries))throw new Error('Estado de adversários inválido.');
  const validUntil=(n)=>n===undefined || (int(n)&&n>=1);
  for(const adv of state?.combat?.adversaries??[])
    if(!validUntil(adv?.aorScathingUntilRound))throw new Error('Efeito AoR de reprimenda inválido.');
  for(const member of state?.group?.companions??[])
    if(!validUntil(member?.character?.combatEffects?.aorInspiringUntilRound)||
       !validUntil(member?.character?.combatEffects?.aorInspiringEncounterSerial))
      throw new Error('Efeito AoR de inspiração inválido.');
  return true;
}
