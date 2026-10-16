import { FORCE_DIE_FACES, FORCE_POWER_TREES } from './forceData.mjs';

const clone=v=>structuredClone(v);
const int=(v,label,{min=0,max=999}={})=>{const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)throw new Error(`${label} inválido.`);return n;};

export { FORCE_DIE_FACES, FORCE_POWER_TREES };

export function normalizeForceState(character){
  const raw=character?.force&&typeof character.force==='object'?character.force:{};
  const powers=raw.powers&&typeof raw.powers==='object'?raw.powers:{};
  return {
    rating:Math.max(0,Number.isInteger(raw.rating)?raw.rating:0),
    committed:Math.max(0,Number.isInteger(raw.committed)?raw.committed:0),
    darkSideUser:Boolean(raw.darkSideUser),
    ongoing:Array.isArray(raw.ongoing)?clone(raw.ongoing):[],
    powers:Object.fromEntries(Object.keys(FORCE_POWER_TREES).map(key=>[key,{
      purchased:Boolean(powers?.[key]?.purchased),
      upgrades:Array.isArray(powers?.[key]?.upgrades)?[...new Set(powers[key].upgrades.map(String))]:[]
    }]))
  };
}
export function forceRating(character){return normalizeForceState(character).rating;}
export function availableForceRating(character){const f=normalizeForceState(character);return Math.max(0,f.rating-f.committed);}
export function isForceSensitive(character){return forceRating(character)>=1;}

export function setForceRating(character,rating){
  const next=clone(character); const f=normalizeForceState(next); f.rating=int(rating,'Force Rating',{min:0,max:10});
  if(f.committed>f.rating) throw new Error('Dados da Força comprometidos excedem o Force Rating.');
  next.force=f; return next;
}
export function grantMinimumForceRating(character,rating=1){return setForceRating(character,Math.max(forceRating(character),int(rating,'Force Rating',{min:1,max:10})));}

export function rollForceDice(character,{rng=Math.random}={}){
  const count=availableForceRating(character); if(count<1) throw new Error('Nenhum dado da Força disponível para rolar.');
  const faces=[]; let light=0,dark=0;
  for(let i=0;i<count;i++){
    const raw=Number(rng()); const idx=Math.max(0,Math.min(11,Math.floor((Number.isFinite(raw)?raw:0)*12)));
    const face=FORCE_DIE_FACES[idx]; faces.push({index:idx+1,...face}); light+=face.light; dark+=face.dark;
  }
  return {dice:count,faces,light,dark};
}

export function resolveForceResources(character,group,roll,{useOppositePoints=0}={}){
  const nextCharacter=clone(character), nextGroup=clone(group);
  const f=normalizeForceState(nextCharacter); nextCharacter.force=f;
  const light=int(roll?.light,'Pontos luminosos',{min:0,max:50}); const dark=int(roll?.dark,'Pontos sombrios',{min:0,max:50});
  const opposite=int(useOppositePoints,'Pontos opostos usados',{min:0,max:50});
  const native=f.darkSideUser?dark:light; const availableOpposite=f.darkSideUser?light:dark;
  if(opposite>availableOpposite) throw new Error('Não há resultados opostos suficientes para converter.');
  let strain=0,flipped=false;
  if(opposite>0){
    const destiny=nextGroup?.destiny;
    if(!destiny||!Number.isInteger(destiny.light)||!Number.isInteger(destiny.dark)) throw new Error('Reserva de Destino inválida.');
    if(f.darkSideUser){
      if(destiny.dark<1) throw new Error('Sem Ponto de Destino sombrio para converter resultados luminosos.');
      destiny.dark-=1; destiny.light+=1;
    }else{
      if(destiny.light<1) throw new Error('Sem Ponto de Destino luminoso para converter resultados sombrios.');
      destiny.light-=1; destiny.dark+=1;
    }
    strain=opposite;
    if(!nextCharacter.derived?.strain||!Number.isInteger(nextCharacter.derived.strain.current)) throw new Error('Ficha sem controle de Tensão.');
    nextCharacter.derived.strain.current+=strain;
    flipped=true;
  }
  return {character:nextCharacter,group:nextGroup,points:native+opposite,nativePoints:native,oppositePointsUsed:opposite,strain,flippedDestiny:flipped};
}

export function purchaseForcePower(character,powerKey){
  if(!isForceSensitive(character)) throw new Error('É necessário Force Rating 1 ou maior para adquirir um poder da Força.');
  const tree=FORCE_POWER_TREES[powerKey]; if(!tree) throw new Error('Poder da Força desconhecido.');
  const next=clone(character), f=normalizeForceState(next); if(f.powers[powerKey].purchased) throw new Error('Este poder da Força já foi adquirido.');
  if(!Number.isInteger(next.xp?.available)||next.xp.available<tree.baseCost) throw new Error(`PE insuficientes: são necessários ${tree.baseCost} PE.`);
  next.xp.available-=tree.baseCost; f.powers[powerKey].purchased=true; next.force=f;
  return {character:next,powerKey,cost:tree.baseCost};
}
function upgradeEntry(powerKey,upgradeId){const tree=FORCE_POWER_TREES[powerKey];return {tree,upgrade:tree?.upgrades?.find(u=>u.id===upgradeId)??null};}
export function eligibleForceUpgrades(character,powerKey){
  const f=normalizeForceState(character),tree=FORCE_POWER_TREES[powerKey]; if(!tree||!f.powers[powerKey]?.purchased)return [];
  const owned=new Set(f.powers[powerKey].upgrades),xp=Number(character?.xp?.available??0);
  return tree.upgrades.map(u=>({...clone(u),purchased:owned.has(u.id),unlocked:u.parents.length===0||u.parents.some(p=>owned.has(p)),available:!owned.has(u.id)&&(u.parents.length===0||u.parents.some(p=>owned.has(p)))&&xp>=u.cost}));
}
export function purchaseForceUpgrade(character,powerKey,upgradeId){
  const {tree,upgrade}=upgradeEntry(powerKey,upgradeId); if(!tree||!upgrade) throw new Error('Aprimoramento da Força desconhecido.');
  const next=clone(character),f=normalizeForceState(next); if(!f.powers[powerKey]?.purchased) throw new Error('Adquira o poder básico antes dos aprimoramentos.');
  const owned=new Set(f.powers[powerKey].upgrades); if(owned.has(upgradeId)) throw new Error('Aprimoramento já adquirido.');
  if(upgrade.parents.length&&!upgrade.parents.some(p=>owned.has(p))) throw new Error('Aprimoramento ainda não está conectado a um aprimoramento adquirido.');
  if(!Number.isInteger(next.xp?.available)||next.xp.available<upgrade.cost) throw new Error(`PE insuficientes: são necessários ${upgrade.cost} PE.`);
  next.xp.available-=upgrade.cost; f.powers[powerKey].upgrades.push(upgradeId); next.force=f; return {character:next,powerKey,upgrade:clone(upgrade),cost:upgrade.cost};
}

export function commitForceDice(character,{count=1,effectId='ongoing'}={}){
  const n=int(count,'Dados da Força comprometidos',{min:1,max:10}); const next=clone(character),f=normalizeForceState(next);
  if(f.rating-f.committed<n) throw new Error('Force Rating disponível insuficiente para comprometer esses dados.');
  const id=String(effectId??'').trim(); if(!id) throw new Error('Identificador do efeito contínuo é obrigatório.');
  if(f.ongoing.some(e=>e.id===id)) throw new Error('Este efeito contínuo já está ativo.');
  f.committed+=n; f.ongoing.push({id,count:n}); next.force=f; return {character:next,effectId:id,committed:n,available:f.rating-f.committed};
}
export function releaseForceDice(character,effectId){
  const id=String(effectId??'').trim(); const next=clone(character),f=normalizeForceState(next); const found=f.ongoing.find(e=>e.id===id); if(!found) throw new Error('Efeito contínuo da Força não encontrado.');
  f.ongoing=f.ongoing.filter(e=>e.id!==id); f.committed=Math.max(0,f.committed-Number(found.count||0)); next.force=f; return {character:next,effectId:id,released:Number(found.count||0),available:f.rating-f.committed};
}

function senseOngoingId(mode){
  const value=String(mode??'').trim().toLowerCase();
  if(!['defense','offense'].includes(value)) throw new Error('Modo contínuo de Sentir inválido.');
  return `sense-${value}`;
}
export function activateSenseOngoingEffect(character,{mode='defense',round=null}={}){
  const caps=forcePowerCapabilities(character,'sense');
  if(!caps.purchased) throw new Error('Sentir não foi adquirido.');
  if(mode==='defense'&&!caps.defense) throw new Error('O aprimoramento defensivo de Sentir não foi adquirido.');
  if(mode==='offense'&&!caps.offense) throw new Error('O aprimoramento ofensivo de Sentir não foi adquirido.');
  const id=senseOngoingId(mode);
  const result=commitForceDice(character,{count:1,effectId:id});
  const next=result.character;
  const f=normalizeForceState(next);
  const entry=f.ongoing.find(e=>e.id===id);
  if(entry){entry.power='sense';entry.mode=mode;entry.activatedRound=Number.isFinite(Number(round))?Number(round):null;entry.lastTriggerRound=null;entry.triggersUsed=0;}
  next.force=f;
  return {...result,character:next,mode,effectId:id};
}
export function endSenseOngoingEffect(character,{mode='defense'}={}){
  const id=senseOngoingId(mode);
  return {...releaseForceDice(character,id),mode};
}
export function consumeSenseOngoingTrigger(character,{mode='defense',round=0}={}){
  const id=senseOngoingId(mode),next=clone(character),f=normalizeForceState(next);
  const entry=f.ongoing.find(e=>e.id===id);
  if(!entry) return {character:next,applied:false,upgrades:0,triggersUsed:0,triggersPerRound:0};
  const caps=forcePowerCapabilities(next,'sense');
  if(mode==='defense'&&!caps.defense) return {character:next,applied:false,upgrades:0,triggersUsed:0,triggersPerRound:caps.triggersPerRound};
  if(mode==='offense'&&!caps.offense) return {character:next,applied:false,upgrades:0,triggersUsed:0,triggersPerRound:caps.triggersPerRound};
  const r=Number.isFinite(Number(round))?Number(round):0;
  if(Number(entry.lastTriggerRound)!==r){entry.lastTriggerRound=r;entry.triggersUsed=0;}
  const used=Math.max(0,Number(entry.triggersUsed)||0);
  if(used>=caps.triggersPerRound){next.force=f;return {character:next,applied:false,upgrades:0,triggersUsed:used,triggersPerRound:caps.triggersPerRound};}
  entry.triggersUsed=used+1;next.force=f;
  return {character:next,applied:true,upgrades:caps.ongoingUpgradeCount,triggersUsed:entry.triggersUsed,triggersPerRound:caps.triggersPerRound};
}

export function forcePowerCapabilities(character,powerKey){
  const tree=FORCE_POWER_TREES[powerKey]; if(!tree) throw new Error('Poder da Força desconhecido.'); const f=normalizeForceState(character),owned=new Set(f.powers[powerKey]?.upgrades??[]);
  const countType=type=>tree.upgrades.filter(u=>u.type===type&&owned.has(u.id)).length;
  if(powerKey==='sense') return {purchased:f.powers.sense.purchased,rangeUpgrades:countType('range'),magnitudeUpgrades:countType('magnitude'),defense:Boolean(owned.has('control_defense')),offense:Boolean(owned.has('control_offense')),surfaceThoughts:Boolean(owned.has('control_thoughts')),triggersPerRound:1+(owned.has('duration')?1:0),ongoingUpgradeCount:owned.has('strength')?2:1};
  if(powerKey==='influence') return {purchased:f.powers.influence.purchased,rangeUpgrades:countType('range'),magnitudeUpgrades:countType('magnitude'),durationUpgrades:countType('duration'),mindControl:Boolean(owned.has('control_mind')),socialControl:Boolean(owned.has('control_social')),strainPerPoint:owned.has('strength')?2:1};
  return {purchased:f.powers.move.purchased,rangeUpgrades:countType('range'),magnitudeUpgrades:countType('magnitude'),strengthUpgrades:countType('strength'),hurl:Boolean(owned.has('control_hurl')),pull:Boolean(owned.has('control_pull')),fineManipulation:Boolean(owned.has('control_fine'))};
}

export function resolveSenseBasic(character,{mode='living',rangeActivations=0,magnitudeActivations=0}={}){
  const c=forcePowerCapabilities(character,'sense'); if(!c.purchased) throw new Error('Sentir não foi adquirido.');
  if(!['living','emotion','thoughts'].includes(mode)) throw new Error('Modo de Sentir inválido.');
  if(mode==='thoughts'&&!c.surfaceThoughts) throw new Error('O aprimoramento para pensamentos superficiais não foi adquirido.');
  const range=int(rangeActivations,'Ativações de Alcance',{min:0,max:20});
  const magnitude=int(magnitudeActivations,'Ativações de Magnitude',{min:0,max:20});
  const baseRange=mode==='living'?'short':'engaged';
  return {mode,baseRange,rangeBandIncrease:c.rangeUpgrades*range,maxTargets:1+c.magnitudeUpgrades*magnitude,forcePointCost:1+range+magnitude};
}

export function resolveInfluenceBasic(character,{activations=1,magnitudeActivations=0,rangeActivations=0}={}){
  const c=forcePowerCapabilities(character,'influence');if(!c.purchased)throw new Error('Influência não foi adquirida.');
  const a=int(activations,'Ativações básicas',{min:1,max:20}); const magnitude=int(magnitudeActivations,'Ativações de Magnitude',{min:0,max:20}); const range=int(rangeActivations,'Ativações de Alcance',{min:0,max:20});
  return {activations:a,strainPerActivation:c.strainPerPoint,totalStrain:a*c.strainPerPoint,maxTargets:1+c.magnitudeUpgrades*magnitude,rangeBandIncrease:c.rangeUpgrades*range,forcePointCost:a+magnitude+range};
}
export function influenceEmotionAllowed({side,emotion}={}){const s=String(side),e=String(emotion);if(!['light','dark'].includes(s))throw new Error('Lado da Força inválido.');if(e==='negative')return s==='dark';if(e==='positive')return s==='light';if(e==='neutral')return true;throw new Error('Tipo de emoção inválido.');}
export function moveBasicLimits(character,{strengthActivations=0,rangeActivations=0,magnitudeActivations=0}={}){
  const c=forcePowerCapabilities(character,'move');if(!c.purchased)throw new Error('Mover não foi adquirido.');
  const strength=int(strengthActivations,'Ativações de Força',{min:0,max:20}); const range=int(rangeActivations,'Ativações de Alcance',{min:0,max:20}); const magnitude=int(magnitudeActivations,'Ativações de Magnitude',{min:0,max:20});
  if(strength>0&&c.strengthUpgrades===0) throw new Error('Nenhum aprimoramento de Força foi adquirido.');
  if(range>0&&c.rangeUpgrades===0) throw new Error('Nenhum aprimoramento de Alcance foi adquirido.');
  if(magnitude>0&&c.magnitudeUpgrades===0) throw new Error('Nenhum aprimoramento de Magnitude foi adquirido.');
  return {maxSilhouette:c.strengthUpgrades*strength,rangeBandIncrease:c.rangeUpgrades*range,maxTargets:1+c.magnitudeUpgrades*magnitude,forcePointCost:1+strength+range+magnitude};
}
export function moveHurlDamage(character,silhouette){const c=forcePowerCapabilities(character,'move');if(!c.hurl)throw new Error('O aprimoramento de arremesso de Mover não foi adquirido.');const s=int(silhouette,'Silhueta',{min:0,max:10});return s===0?5:s*10;}

export function executeMoveSceneObject(character, sceneObject, { destinationRange = 'Curto', hurlAtTarget = null } = {}) {
  const caps = forcePowerCapabilities(character, 'move');
  if (!caps.purchased) throw new Error('Poder Mover não foi adquirido.');
  if (!sceneObject || typeof sceneObject !== 'object') throw new Error('Objeto de cena inválido.');
  const sil = Math.max(0, Number(sceneObject.silhouette) || 0);
  if (sil > caps.strengthUpgrades) {
    throw new Error(`Silhueta ${sil} excede a capacidade de Mover da ficha (máx ${caps.strengthUpgrades}).`);
  }
  const nextObject = clone(sceneObject);
  nextObject.range = destinationRange;
  nextObject.movedAt = new Date().toISOString();
  let damage = 0;
  if (hurlAtTarget) {
    if (!caps.hurl) throw new Error('Aprimoramento de arremesso não adquirido.');
    damage = moveHurlDamage(character, sil);
    nextObject.hurlTarget = hurlAtTarget;
    nextObject.impactDamage = damage;
  }
  return {
    success: true,
    sceneObject: nextObject,
    damage,
    silhouette: sil,
    destinationRange
  };
}
