import { addStarterCritical, isCriticalIncapacitated, isDeadFromCritical } from './critical.mjs';
import { resolveStrainReduction, stimpackSpecializationBonus } from './talents.mjs';
function cloneCharacter(character) { if (!character) throw new Error('Nenhum personagem carregado.'); return structuredClone(character); }
function isDroid(character){ return character?.creation?.speciesKey==='droid' || /^droide$/i.test(String(character?.species??'')); }

export function effectiveWoundThreshold(character){ const base=Number(character?.derived?.wounds?.threshold??0); const bonus=Math.max(0,Number(character?.sessionEffects?.dutyWoundThresholdBonus??0)); return Math.max(0,base+bonus); }
export function effectiveStrainThreshold(character){ const base=Number(character?.derived?.strain?.threshold??0); const penalty=Math.max(0,Number(character?.sessionEffects?.obligationStrainThresholdPenalty??0)); return Math.max(0,base-penalty); }

export function getHealthStatus(character) {
  const wounds=character?.derived?.wounds, strain=character?.derived?.strain;
  if(!wounds||!strain) throw new Error('Ficha sem estado de saúde válido.');
  const woundThreshold=effectiveWoundThreshold(character);
  const woundIncapacitated=wounds.current>woundThreshold;
  const strainThreshold=effectiveStrainThreshold(character);
  const strainIncapacitated=strain.current>strainThreshold;
  const criticalIncapacitated=isCriticalIncapacitated(character);
  const dead=isDeadFromCritical(character);
  return {dead,unconscious:dead||woundIncapacitated||strainIncapacitated||criticalIncapacitated,woundIncapacitated,strainIncapacitated,criticalIncapacitated,effectiveWoundThreshold:woundThreshold,woundThresholdBonus:Math.max(0,Number(character?.sessionEffects?.dutyWoundThresholdBonus??0)),effectiveStrainThreshold:strainThreshold,strainThresholdPenalty:Math.max(0,Number(character?.sessionEffects?.obligationStrainThresholdPenalty??0)),label:dead?'Morto':(woundIncapacitated||strainIncapacitated||criticalIncapacitated?'Incapacitado':'Consciente')};
}

export function sufferStrain(character,amount,{voluntary=false}={}){
  const n=Number(amount); if(!Number.isInteger(n)||n<0) throw new Error('Quantidade de Tensão inválida.');
  const mod=resolveStrainReduction(character,n,{voluntary});
  const next=cloneCharacter(character); next.derived.strain.current+=mod.effective;
  return {character:next,amount:mod.effective,rawAmount:n,resolve:mod,status:getHealthStatus(next)};
}
export function recoverStrain(character,amount){
  const n=Number(amount); if(!Number.isInteger(n)||n<0) throw new Error('Recuperação de Tensão inválida.');
  const next=cloneCharacter(character),before=next.derived.strain.current; next.derived.strain.current=Math.max(0,before-n);
  return {character:next,recovered:before-next.derived.strain.current,status:getHealthStatus(next)};
}

export function applyDamage(character,{damage,pierce=0,damageType='wounds',reason='Dano recebido',criticalRng=Math.random,onBeforeCritical=null}={}){
  const raw=Number(damage),pierceValue=Number(pierce);
  if(!Number.isInteger(raw)||raw<0) throw new Error('Dano inválido.');
  if(!Number.isInteger(pierceValue)||pierceValue<0) throw new Error('Perfuração inválida.');
  if(!['wounds','strain'].includes(damageType)) throw new Error('Tipo de dano inválido.');
  const next=cloneCharacter(character),effectiveSoak=Math.max(0,next.derived.soak-pierceValue),suffered=Math.max(0,raw-effectiveSoak);
  const pool=damageType==='strain'?next.derived.strain:next.derived.wounds,before=pool.current;
  const activeWoundThreshold=effectiveWoundThreshold(next);
  if(damageType==='wounds') pool.current=Math.min(Math.max(pool.threshold*2,activeWoundThreshold*2),pool.current+suffered); else pool.current+=suffered;
  let criticalAdded=false,criticalInjury=null,criticalPrevention=null,finalCharacter=next;
  if(damageType==='wounds'&&before<=activeWoundThreshold&&pool.current>activeWoundThreshold){
    const critical=addStarterCritical(next,reason,{rng:criticalRng,onBeforeCritical}); finalCharacter=critical.character; criticalAdded=Boolean(critical.added); criticalInjury=critical.injury??null; criticalPrevention=critical.interception??null;
  }
  return {character:finalCharacter,rawDamage:raw,effectiveSoak,suffered:damageType==='wounds'?pool.current-before:suffered,damageType,criticalAdded,criticalInjury,criticalPrevention,status:getHealthStatus(finalCharacter)};
}

export function healWounds(character,amount){
  const n=Number(amount); if(!Number.isInteger(n)||n<0) throw new Error('Cura inválida.');
  const next=cloneCharacter(character),before=next.derived.wounds.current; next.derived.wounds.current=Math.max(0,before-n);
  return {character:next,healed:before-next.derived.wounds.current,status:getHealthStatus(next)};
}

export function useStimpack(character){
  const next=cloneCharacter(character);
  if(isDroid(next)) throw new Error('Droides não podem usar Stimpacks; use remendos de reparo de emergência.');
  const index=next.gear.findIndex(item=>item.id==='stimpack_injector'||/injetor(?:es)? de estimulantes?/i.test(item.name));
  if(index<0) throw new Error('Nenhum injetor de estimulantes disponível.');
  const item=next.gear[index],quantity=Number(item.quantity??1); if(!Number.isInteger(quantity)||quantity<=0) throw new Error('Nenhum injetor de estimulantes disponível.');
  const before=next.derived.wounds.current,usesToday=Math.max(0,Number(next.stimpackUsesToday??0)||0),healing=Math.max(0,5-usesToday)+stimpackSpecializationBonus(next);
  next.derived.wounds.current=Math.max(0,before-healing); next.stimpackUsesToday=usesToday+1; item.quantity=quantity-1;
  return {character:next,healed:before-next.derived.wounds.current,healingValue:healing,usesToday:next.stimpackUsesToday,remaining:item.quantity,status:getHealthStatus(next)};
}

export function useEmergencyRepairPatch(character){
  const next=cloneCharacter(character); if(!isDroid(next)) throw new Error('Remendos de reparo de emergência só funcionam em droides.');
  const index=next.gear.findIndex(item=>item.id==='emergency_repair_patch'||/reparo.*emerg|reparaciones.*emerg/i.test(item.name));
  if(index<0) throw new Error('Nenhum remendo de reparo de emergência disponível.');
  const item=next.gear[index],quantity=Number(item.quantity??1); if(!Number.isInteger(quantity)||quantity<=0) throw new Error('Nenhum remendo de reparo de emergência disponível.');
  const uses=Math.max(0,Number(next.repairPatchUsesToday??0)||0); if(uses>=5) throw new Error('Limite diário de cinco remendos de reparo de emergência atingido.');
  const before=next.derived.wounds.current; next.derived.wounds.current=Math.max(0,before-3); next.repairPatchUsesToday=uses+1; item.quantity=quantity-1;
  return {character:next,healed:before-next.derived.wounds.current,healingValue:3,usesToday:next.repairPatchUsesToday,remaining:item.quantity,status:getHealthStatus(next)};
}
