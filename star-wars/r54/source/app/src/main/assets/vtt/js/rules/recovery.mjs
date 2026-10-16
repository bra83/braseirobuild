import { rollPool, buildPositivePool } from './narrativeDice.mjs';
import { downgradeVertigoPositivePool } from '../adventure/jewelHoloParkEffects.mjs';
import { SKILLS, CHARACTERISTICS } from './character.mjs';
import { getHealthStatus, sufferStrain } from './health.mjs';
import { talentRankByOriginal, sourceBackedTalentRank } from './talents.mjs';
import { generalGearCatalogEntry } from './equipmentCatalog.mjs';
import { healCriticalInjury } from './critical.mjs';

function isDroid(character){ return character?.creation?.speciesKey==='droid'||/^droide$/i.test(String(character?.species??'')); }

export function medicineEquipmentProfile(itemId){
  if(itemId===null||itemId===undefined||itemId==='') return null;
  const item=generalGearCatalogEntry(String(itemId)); if(!item) throw new Error('Equipamento médico desconhecido.');
  if(!item.medicine) throw new Error(`${item.name} não possui efeito de Medicina auditado.`);
  return {itemId:item.id,name:item.name,medicalSupplies:Boolean(item.medicine.medicalSupplies),boost:Number(item.medicine.boost??0),source:structuredClone(item.effectSource??item.source)};
}

export function medicineDifficulty(character,{medicalSupplies=true,selfTreatment=false,droid=isDroid(character)}={}){
  if(!character) throw new Error('Nenhum personagem carregado.');
  const current=character.derived.wounds.current,threshold=character.derived.wounds.threshold;
  let difficulty=current<=threshold/2?1:current<=threshold?2:3;
  if(!medicalSupplies) difficulty+=1;
  if(selfTreatment) difficulty+=droid?1:2;
  return difficulty;
}

export function resolveMedicineCheck(healer,patient=healer,{medicalSupplies=true,medicalItemId=null,boost=0,setback=0,difficultyIncrease=0,difficultyDecrease=0,difficultyUpgrades=0,characteristicPenalty=0,removeBoost=false,vertigoDowngrade=false,rng=Math.random}={}){
  if(!healer||!patient) throw new Error('Personagem inválido para Medicina.');
  const droid=isDroid(patient),medicalEquipment=medicineEquipmentProfile(medicalItemId),hasMedicalSupplies=medicalEquipment?medicalEquipment.medicalSupplies:Boolean(medicalSupplies),equipmentBoost=medicalEquipment?.boost??0;
  const skillKey=droid?'mecanica':'medicina',characteristicKey=SKILLS[skillKey],selfTreatment=healer===patient;
  const effectiveCharacteristic=Math.max(0,Number(healer.characteristics[characteristicKey]??0)-Math.max(0,Number(characteristicPenalty)||0));
  const positive=buildPositivePool(effectiveCharacteristic,healer.skills[skillKey]??0);
  if(!Number.isSafeInteger(difficultyDecrease)||difficultyDecrease<0||difficultyDecrease>1)throw new Error('Redução de dificuldade de Medicina inválida.');
  const baseDifficulty=Math.max(1,medicineDifficulty(patient,{medicalSupplies:hasMedicalSupplies,selfTreatment,droid})+Math.max(0,Number(difficultyIncrease)||0)-difficultyDecrease);
  let difficulty=baseDifficulty,challenge=0,upgrades=Math.max(0,Number(difficultyUpgrades)||0);
  while(upgrades-->0){ if(difficulty>0){ difficulty-=1; challenge+=1; } else difficulty+=1; }
  const effectivePositive=downgradeVertigoPositivePool(positive,vertigoDowngrade);
  const check=rollPool({...effectivePositive,difficulty,challenge,boost:removeBoost?0:Number(boost)+equipmentBoost,setback:Number(setback)},rng);
  const next=structuredClone(patient); let healed=0,strainRecovered=0;
  if(check.success){
    const surgeonBonus=droid?0:talentRankByOriginal(healer,'SURG');
    healed=Math.min(next.derived.wounds.current,Math.max(0,check.netSuccess)+surgeonBonus); next.derived.wounds.current-=healed;
    strainRecovered=Math.min(next.derived.strain.current,Math.max(0,check.netAdvantage)); next.derived.strain.current-=strainRecovered;
  }
  return {character:next,check,difficulty,challenge,healed,strainRecovered,skillKey,selfTreatment,medicalSupplies:hasMedicalSupplies,medicalEquipment,status:getHealthStatus(next)};
}

export function resolveCriticalTreatment(healer,patient=healer,{injuryId,medicalSupplies=true,medicalItemId=null,boost=0,setback=0,rng=Math.random}={}){
  if(!healer||!patient) throw new Error('Personagem inválido para tratamento crítico.');
  const injury=(patient.criticalInjuries??[]).find(i=>i.id===injuryId); if(!injury) throw new Error('Ferimento Crítico não encontrado.');
  if(!Number.isInteger(injury.healDifficulty)) throw new Error('Este Ferimento Crítico não pode ser curado por tratamento normal.');
  const droid=isDroid(patient),medicalEquipment=medicineEquipmentProfile(medicalItemId),hasMedicalSupplies=medicalEquipment?medicalEquipment.medicalSupplies:Boolean(medicalSupplies),selfTreatment=healer===patient;
  const skillKey=droid?'mecanica':'medicina',characteristicKey=SKILLS[skillKey],positive=buildPositivePool(healer.characteristics[characteristicKey],healer.skills[skillKey]??0);
  let difficulty=injury.healDifficulty+(hasMedicalSupplies?0:1)+(selfTreatment?(droid?1:2):0);
  const check=rollPool({...positive,difficulty,challenge:0,boost:Number(boost)+(medicalEquipment?.boost??0),setback:Number(setback)},rng);
  const result=check.success?healCriticalInjury(patient,injuryId):{character:structuredClone(patient),injury:null};
  return {character:result.character,check,difficulty,healedCritical:Boolean(result.injury),injury:result.injury,skillKey,selfTreatment,medicalSupplies:hasMedicalSupplies,medicalEquipment,status:getHealthStatus(result.character)};
}

export function recoverAfterEncounter(character,{skillKey='frialdad',rng=Math.random}={}){
  if(!character) throw new Error('Nenhum personagem carregado.');
  if(!['frialdad','disciplina'].includes(skillKey)) throw new Error('Recuperação pós-encontro exige Frialdade ou Disciplina.');
  const characteristicKey=SKILLS[skillKey],positive=buildPositivePool(character.characteristics[characteristicKey],character.skills[skillKey]??0),check=rollPool({...positive,difficulty:0,challenge:0,boost:0,setback:0},rng);
  const next=structuredClone(character),base=Math.max(0,check.netSuccess),rapidRecovery=base>0?talentRankByOriginal(next,'RAPREC'):0,amount=base+rapidRecovery,before=next.derived.strain.current;
  next.derived.strain.current=Math.max(0,before-amount);
  return {character:next,check,skillKey,recovered:before-next.derived.strain.current,basis:base,talentBonus:rapidRecovery,status:getHealthStatus(next)};
}

export function restFullNight(character){
  if(!character) throw new Error('Nenhum personagem carregado.');
  const next=structuredClone(character),woundsBefore=next.derived.wounds.current,strainBefore=next.derived.strain.current;
  next.derived.wounds.current=Math.max(0,woundsBefore-1); next.derived.strain.current=0; next.stimpackUsesToday=0; next.repairPatchUsesToday=0;
  return {character:next,woundsRecovered:woundsBefore-next.derived.wounds.current,strainRecovered:strainBefore,status:getHealthStatus(next)};
}

export function recoverInBactaTank(character,hours,{supervisor=null}={}){
  if(!character) throw new Error('Nenhum personagem carregado.'); if(isDroid(character)) throw new Error('Droides não se recuperam em tanque de bacta.');
  const h=Number(hours); if(!Number.isFinite(h)||h<0) throw new Error('Horas de bacta inválidas.');
  const next=structuredClone(character),incapacitated=next.derived.wounds.current>next.derived.wounds.threshold,rate=incapacitated?6:2,recover=Math.floor(h/rate),before=next.derived.wounds.current;
  const specialistBonus=recover>0?sourceBackedTalentRank(supervisor,'BACTASPEC'):0;
  next.derived.wounds.current=Math.max(0,before-recover-specialistBonus);
  return {character:next,hours:h,rateHoursPerWound:rate,woundsRecovered:before-next.derived.wounds.current,specialistBonus,status:getHealthStatus(next)};
}

export function recoverInOilBath(character,hours){
  if(!character||!isDroid(character)) throw new Error('Banho de óleo é exclusivo para droides.');
  const h=Number(hours); if(!Number.isFinite(h)||h<0) throw new Error('Horas de banho de óleo inválidas.');
  const next=structuredClone(character),before=next.derived.wounds.current,recover=Math.floor(h); next.derived.wounds.current=Math.max(0,before-recover);
  return {character:next,hours:h,woundsRecovered:before-next.derived.wounds.current,status:getHealthStatus(next)};
}

/** P163 — long-term care applies Bacta Specialist only when a wound would
 * actually heal. The supervisor is an explicit character, never inferred from
 * a translated talent label or a passive flag in the save. */
export function recoverUnderMedicalSupervision(patient,{supervisor=null}={}){
  if(!patient)throw new Error('Paciente inválido.');
  const before=patient.derived.wounds.current;
  const rested=restFullNight(patient);
  const rank=rested.woundsRecovered>0?sourceBackedTalentRank(supervisor,'BACTASPEC'):0;
  rested.character.derived.wounds.current=Math.max(0,rested.character.derived.wounds.current-rank);
  return {...rested,woundsRecovered:before-rested.character.derived.wounds.current,specialistBonus:rank,status:getHealthStatus(rested.character)};
}

/** P163 — a single, testable Stim Application resolution, including improved
 * difficulty/strain and supreme Triumph spending. The caller owns the atomic
 * commit; this pure helper never mutates its arguments or inventory. */
export function resolveStimApplication(healer,patient,{characteristicKey,additionalCharacteristics=[],improved=false,vertigoDowngrade=false,rng=Math.random}={}){
  if(!healer||!patient)throw new Error('Médico ou paciente inválido.');
  if(sourceBackedTalentRank(healer,'STIMAPP')<1)throw new Error('Aplicação de estimulantes não foi adquirida.');
  if(typeof improved!=='boolean')throw new Error('Opção aprimorada inválida.');
  if(improved&&sourceBackedTalentRank(healer,'STIMAPPIMP')<1)throw new Error('Aplicação aprimorada não foi adquirida.');
  if(!Array.isArray(additionalCharacteristics)||additionalCharacteristics.length>5 ||
     additionalCharacteristics.some(key=>!CHARACTERISTICS.includes(key)) ||
     new Set([characteristicKey,...additionalCharacteristics]).size!==1+additionalCharacteristics.length ||
     !CHARACTERISTICS.includes(characteristicKey))throw new Error('Características de estimulantes inválidas ou repetidas.');
  if(additionalCharacteristics.length && sourceBackedTalentRank(healer,'STIMAPPSUP')<1)
    throw new Error('Aplicação suprema não foi adquirida.');
  const available=(healer.gear??[]).filter(x=>Number(x?.quantity??1)>0).find(x=>
    ['stimpack_injector','medpac','emergency_medpac'].includes(x.id??x.sourceCatalogId));
  if(!available)throw new Error('Aplicação de estimulantes exige injetor ou medpac realmente possuído.');
  const existing=patient?.combatEffects?.stimApplicationBonuses??{};
  for(const key of [characteristicKey,...additionalCharacteristics]){
    if(existing[key] || Number(patient.characteristics?.[key]??0)>=6)
      throw new Error('Esta Característica já recebeu estimulante ou atingiu o limite de 6.');
  }
  const positive=buildPositivePool(Number(healer.characteristics?.intelecto??0),Number(healer.skills?.medicina??0));
  const difficulty=improved?3:2;
  const effectivePositive=downgradeVertigoPositivePool(positive,vertigoDowngrade);
  const check=rollPool({...effectivePositive,difficulty,challenge:0,boost:available.id==='medpac'?1:0,setback:0},rng);
  if(check.success && additionalCharacteristics.length>Number(check.triumph??0))
    throw new Error('Aplicação suprema exige um Triunfo real por Característica adicional.');
  const next=structuredClone(patient);
  if(check.success){
    for(const key of [characteristicKey,...additionalCharacteristics])next.characteristics[key]++;
    next.combatEffects={...(next.combatEffects??{}),stimApplicationBonuses:{...existing,...Object.fromEntries([characteristicKey,...additionalCharacteristics].map(key=>[key,1]))}};
    const strained=sufferStrain(next,improved?1:4);return {character:strained.character,check,difficulty,strainSuffered:strained.amount,improved,additional:additionalCharacteristics.length,boosted:[characteristicKey,...additionalCharacteristics]};
  }
  return {character:next,check,difficulty,strainSuffered:0,improved,additional:0,boosted:[]};
}

export function expireStimApplication(patient){
  if(!patient?.combatEffects?.stimApplicationBonuses)return structuredClone(patient);
  const next=structuredClone(patient),bonuses=next.combatEffects.stimApplicationBonuses;
  for(const [key,amount] of Object.entries(bonuses)){
    if(!CHARACTERISTICS.includes(key)||amount!==1||!Number.isInteger(next.characteristics[key])||next.characteristics[key]<=1)
      throw new Error('Registro de estimulantes inconsistente.');
    next.characteristics[key]-=amount;
  }
  delete next.combatEffects.stimApplicationBonuses;
  if(!Object.keys(next.combatEffects).length)delete next.combatEffects;
  return next;
}
