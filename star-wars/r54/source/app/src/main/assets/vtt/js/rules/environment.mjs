import { SKILLS } from './character.mjs';
import { getHealthStatus, effectiveWoundThreshold, sufferStrain } from './health.mjs';
import { addStarterCritical } from './critical.mjs';

const RANGE_ORDER=Object.freeze(['Interação','Curto','Médio','Longo','Extremo']);
const RANGED_SKILLS=new Set(['armas_distancia_ligeras','armas_distancia_pesadas','artilleria']);

function clamp(value,min,max){return Math.max(min,Math.min(max,Number(value)||0));}
function clone(value){return structuredClone(value);}
function characterBrawn(character){return Math.max(0,Number(character?.characteristics?.fortaleza??0));}

export function createEnvironmentRuntime(){
  return {
    schemaVersion:1,
    terrain:'normal',
    gravity:{mode:'normal',level:0},
    concealment:0,
    scriptedSetbackDice:0,
    water:{present:false,submerged:false},
    vacuum:false,
    suffocating:false,
    hazards:{fire:0,acid:0,corrosive:0},
    exposure:{breathRounds:0},
    impassableClearance:0,
    lastTurn:null
  };
}

export function normalizeEnvironmentRuntime(value){
  const base=createEnvironmentRuntime(), v=value&&typeof value==='object'?value:{};
  const terrain=['normal','difficult','impassable'].includes(String(v.terrain))?String(v.terrain):'normal';
  const gravityMode=['normal','heavy','light','zero'].includes(String(v.gravity?.mode))?String(v.gravity.mode):'normal';
  const gravityLevel=gravityMode==='normal'||gravityMode==='zero'?0:Math.round(clamp(v.gravity?.level,1,3));
  return {
    ...base,...clone(v),terrain,
    gravity:{mode:gravityMode,level:gravityLevel},
    concealment:Math.round(clamp(v.concealment,0,3)),
    scriptedSetbackDice:Math.round(clamp(v.scriptedSetbackDice,0,3)),
    water:{present:Boolean(v.water?.present),submerged:Boolean(v.water?.submerged)},
    vacuum:Boolean(v.vacuum),suffocating:Boolean(v.suffocating),
    hazards:{fire:Math.round(clamp(v.hazards?.fire,0,20)),acid:Math.round(clamp(v.hazards?.acid,0,20)),corrosive:Math.round(clamp(v.hazards?.corrosive,0,20))},
    exposure:{breathRounds:Math.max(0,Math.round(Number(v.exposure?.breathRounds)||0))},
    impassableClearance:Math.max(0,Math.round(Number(v.impassableClearance)||0)),
    lastTurn:v.lastTurn&&typeof v.lastTurn==='object'?clone(v.lastTurn):null
  };
}

export function configureEnvironment(runtime, options={}){
  const current=normalizeEnvironmentRuntime(runtime), next=clone(current);
  if(options.terrain!==undefined) next.terrain=String(options.terrain);
  if(options.gravity!==undefined) next.gravity={...next.gravity,...options.gravity};
  if(options.gravityMode!==undefined) next.gravity.mode=String(options.gravityMode);
  if(options.gravityLevel!==undefined) next.gravity.level=Number(options.gravityLevel);
  if(options.concealment!==undefined) next.concealment=Number(options.concealment);
  if(options.scriptedSetbackDice!==undefined) next.scriptedSetbackDice=Number(options.scriptedSetbackDice);
  if(options.water!==undefined) next.water={...next.water,...options.water};
  if(options.waterPresent!==undefined) next.water.present=Boolean(options.waterPresent);
  if(options.submerged!==undefined) next.water.submerged=Boolean(options.submerged);
  if(options.vacuum!==undefined) next.vacuum=Boolean(options.vacuum);
  if(options.suffocating!==undefined) next.suffocating=Boolean(options.suffocating);
  if(options.hazards!==undefined) next.hazards={...next.hazards,...options.hazards};
  for(const key of ['fire','acid','corrosive']) if(options[key]!==undefined) next.hazards[key]=Number(options[key]);
  const normalized=normalizeEnvironmentRuntime(next);
  const breathingExposure=normalized.vacuum||normalized.water.submerged;
  const wasBreathingExposure=current.vacuum||current.water.submerged;
  if(!breathingExposure || !wasBreathingExposure) normalized.exposure.breathRounds=0;
  if(normalized.terrain!=='impassable') normalized.impassableClearance=0;
  return normalized;
}

export function clearEnvironment(){return createEnvironmentRuntime();}

export function environmentSkillModifiers(runtime,skillKey,characteristicKey=SKILLS[skillKey]){
  const env=normalizeEnvironmentRuntime(runtime); let boost=0,setback=0; const notes=[];
  if(env.gravity.mode==='heavy' && ((characteristicKey==='fortaleza'&&skillKey!=='aguante')||skillKey==='coordinacion')){setback+=env.gravity.level;notes.push(`gravidade alta +${env.gravity.level} Contratempo`);}
  if(env.gravity.mode==='light' && ((characteristicKey==='fortaleza'&&skillKey!=='aguante')||skillKey==='coordinacion')){boost+=env.gravity.level;notes.push(`gravidade baixa +${env.gravity.level} Benefício`);}
  if(env.concealment>0){
    if(RANGED_SKILLS.has(skillKey)||skillKey==='percepcion'){setback+=env.concealment;notes.push(`ocultação +${env.concealment} Contratempo`);}
    if(skillKey==='sigilo'){boost+=env.concealment;notes.push(`ocultação +${env.concealment} Benefício`);}
  }
  if(env.scriptedSetbackDice){setback+=env.scriptedSetbackDice;notes.push(`cena noturna publicada +${env.scriptedSetbackDice} Contratempo`);}
  return {boost,setback,notes,gravity:clone(env.gravity),concealment:env.concealment,scriptedSetbackDice:env.scriptedSetbackDice};
}

export function movementManeuverMultiplier(runtime,{swift=false}={}){
  const env=normalizeEnvironmentRuntime(runtime);
  if(swift) return 1;
  return (env.terrain==='difficult'||env.gravity.mode==='zero'||env.water.present)?2:1;
}

export function movementEnvironmentStatus(runtime,{swift=false}={}){
  const env=normalizeEnvironmentRuntime(runtime);
  return {blocked:env.terrain==='impassable'&&env.impassableClearance<1,multiplier:movementManeuverMultiplier(env,{swift}),clearance:env.impassableClearance,terrain:env.terrain,zeroGravity:env.gravity.mode==='zero',water:env.water.present};
}

function addDirectWounds(character,amount,reason,{rng=Math.random,criticalModifier=0,forceCritical=false}={}){
  let next=clone(character); const n=Math.max(0,Math.round(Number(amount)||0)); const before=Number(next.derived?.wounds?.current??0),threshold=effectiveWoundThreshold(next);
  next.derived.wounds.current=Math.min(Math.max(Number(next.derived.wounds.threshold??threshold)*2,threshold*2),before+n);
  let critical=null;
  if(forceCritical || (before<=threshold && next.derived.wounds.current>threshold)){critical=addStarterCritical(next,reason,{rng,modifier:Math.max(0,Number(criticalModifier)||0)});next=critical.character;}
  return {character:next,wounds:next.derived.wounds.current-before,critical:critical?.injury??null,status:getHealthStatus(next)};
}

export function applyEnvironmentTurnStart(runtime,character,{rng=Math.random}={}){
  const env=normalizeEnvironmentRuntime(runtime); let next=clone(character); const effects=[];
  const statusBefore=getHealthStatus(next);
  const wasWoundIncapacitated=statusBefore.woundIncapacitated;
  const wasStrainIncapacitated=statusBefore.strainIncapacitated;

  const fire=Math.max(0,env.hazards.fire), acid=Math.max(0,env.hazards.acid), corrosive=Math.max(0,env.hazards.corrosive);
  for(const [key,rating,label] of [['fire',fire,'Fogo'],['acid',acid,'Ácido'],['corrosive',corrosive,'Atmosfera corrosiva']]) if(rating>0){const r=addDirectWounds(next,rating,label,{rng});next=r.character;effects.push({type:key,label,wounds:r.wounds,critical:r.critical});}
  if(env.vacuum){const r=addDirectWounds(next,3,'Exposição ao vácuo',{rng,forceCritical:wasWoundIncapacitated});next=r.character;effects.push({type:'vacuum',label:'Vácuo',wounds:r.wounds,critical:r.critical});}

  const breathExposure=env.vacuum||env.water.submerged;
  let suffocatingNow=env.suffocating;
  if(breathExposure){const brawn=characterBrawn(next); if(env.exposure.breathRounds>=brawn) suffocatingNow=true; else env.exposure.breathRounds+=1;}
  else env.exposure.breathRounds=0;
  if(suffocatingNow){
    const strain=sufferStrain(next,3); next=strain.character; let critical=null;
    if(wasStrainIncapacitated){const c=addStarterCritical(next,'Sufocamento',{rng});next=c.character;critical=c.injury;}
    effects.push({type:'suffocation',label:'Sufocamento',strain:strain.amount,rawStrain:3,critical});
  }
  env.lastTurn={at:new Date().toISOString(),effects:clone(effects),breathRounds:env.exposure.breathRounds,suffocating:suffocatingNow};
  return {environment:env,character:next,effects,status:getHealthStatus(next),suffocating:suffocatingNow};
}

export function consumeImpassableClearance(runtime){const env=normalizeEnvironmentRuntime(runtime); if(env.impassableClearance>0)env.impassableClearance-=1; return env;}
export function grantImpassableClearance(runtime){const env=normalizeEnvironmentRuntime(runtime); env.impassableClearance=1; return env;}

export function fallingProfile(range){
  const normalized=String(range??''); const idx=RANGE_ORDER.indexOf(normalized); if(idx<1) throw new Error('Faixa de queda inválida.');
  if(normalized==='Curto') return {range:normalized,damage:10,strain:10,criticalModifier:0,forcedIncapacitation:false};
  if(normalized==='Médio') return {range:normalized,damage:30,strain:20,criticalModifier:0,forcedIncapacitation:false};
  if(normalized==='Longo') return {range:normalized,damage:null,strain:30,criticalModifier:50,forcedIncapacitation:true};
  return {range:'Extremo',damage:null,strain:40,criticalModifier:75,forcedIncapacitation:true};
}
export function reduceFallRange(range){const i=RANGE_ORDER.indexOf(String(range)); return i>1?RANGE_ORDER[i-1]:String(range);}
export function applyNumericFall(character,{range,netSuccess=0,netAdvantage=0,rng=Math.random}={}){
  const p=fallingProfile(range); if(p.forcedIncapacitation) throw new Error('Use applyForcedFall para quedas Longas/Extremas.');
  const damage=Math.max(0,p.damage-Math.max(0,Number(netSuccess)||0)), strain=Math.max(0,p.strain-Math.max(0,Number(netAdvantage)||0));
  let next=clone(character); const wounds=addDirectWounds(next,Math.max(0,damage-Number(next.derived?.soak??0)),'Queda',{rng}); next=wounds.character; const strainResult=sufferStrain(next,strain); next=strainResult.character;
  return {character:next,profile:p,rawDamage:p.damage,damageAfterSuccess:damage,soak:Number(character?.derived?.soak??0),wounds:wounds.wounds,rawStrain:p.strain,strain:strainResult.amount,status:getHealthStatus(next),critical:wounds.critical};
}
export function applyForcedFall(character,{range,rng=Math.random,netAdvantage=0}={}){
  const p=fallingProfile(range); if(!p.forcedIncapacitation) throw new Error('Use applyNumericFall para quedas Curtas/Médias.');
  let next=clone(character); const threshold=effectiveWoundThreshold(next); if(Number(next.derived.wounds.current)<=threshold)next.derived.wounds.current=threshold+1;
  const c=addStarterCritical(next,'Queda',{rng,modifier:p.criticalModifier}); next=c.character; const reduction=Math.max(0,Math.floor(Number(netAdvantage)||0));
  const strainResult=sufferStrain(next,Math.max(0,p.strain-reduction)); next=strainResult.character;
  return {character:next,profile:p,woundsForcedTo:next.derived.wounds.current,strain:strainResult.amount,critical:c.injury,status:getHealthStatus(next)};
}
