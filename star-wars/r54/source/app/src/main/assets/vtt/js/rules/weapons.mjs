function norm(value){return String(value??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR').trim();}
function qualityEntries(weapon){
  const q=weapon?.qualities;
  if(Array.isArray(q)) return q.map(v=>String(v));
  if(q&&typeof q==='object') return Object.entries(q).map(([k,v])=>`${k} ${v===true?'':v}`.trim());
  return [];
}
const ALIASES=Object.freeze({
  accurate:['precisa','accurate'],
  autofire:['automatica','auto-fire','autofire','automatic'],
  breach:['brecha','breach'],
  pierce:['perfurante','pierce'],
  vicious:['letal','viciosa','vicious'],
  blast:['explosao','blast'],
  stunDamage:['dano de atordoamento','stun damage'],
  stunSetting:['modo atordoante','stun setting'],
  inaccurate:['imprecisa','inaccurate'],
  cumbersome:['aparatosa','cumbersome'],
  unwieldy:['desajeitada','unwieldy'],
  defensive:['defensiva','defensive'],
  deflection:['deflexao','deflection'],
  prepare:['preparacao','prepare'],
  limitedAmmo:['municao limitada','limited ammo'],
  slowFiring:['cadencia lenta','slow-firing','slow firing'],
  burn:['incendiaria','burn'],
  disorient:['desorientacao','disorient'],
  stun:['atordoadora','stun'],
  sunder:['hendedura','sunder'],
  concussive:['concussiva','concussive'],
  ensnare:['enredante','ensnare'],
  linked:['vinculada','linked'],
  guided:['guiada','guided'],
  knockdown:['derrubar','knockdown'],
  ion:['ionica','ion'],
  cortosis:['cortosis'],
  superior:['superior'],
  inferior:['inferior']
});
function qualityEntryMatches(entry, aliases) {
  const label = norm(entry);
  return aliases.some(alias => {
    const name = norm(alias);
    if (label === name) return true;
    if (!label.startsWith(`${name} `)) return false;
    return /^\d+$/.test(label.slice(name.length).trim());
  });
}
export function weaponHasQuality(weapon,key){
  const aliases=ALIASES[key]??[key];
  if(weapon?.[key]===true||weapon?.qualities?.[key]===true)return true;
  return qualityEntries(weapon).some(entry=>qualityEntryMatches(entry,aliases));
}
export function weaponQualityRank(weapon,key){
  const aliases=ALIASES[key]??[key];
  const direct=Number(weapon?.[key]??weapon?.qualities?.[key]);
  if(Number.isFinite(direct)&&direct>0)return direct;
  for(const entry of qualityEntries(weapon)){
    if(!qualityEntryMatches(entry,aliases))continue;
    const m=String(entry).match(/(\d+)/); return m?Number(m[1]):1;
  }
  return 0;
}
export function weaponUsageModifiers(weapon,character=null){
  const inaccurate=weaponQualityRank(weapon,'inaccurate');
  const cumbersome=weaponQualityRank(weapon,'cumbersome');
  const unwieldy=weaponQualityRank(weapon,'unwieldy');
  const prepare=weaponQualityRank(weapon,'prepare');
  const limitedAmmo=weaponQualityRank(weapon,'limitedAmmo');
  const slowFiring=weaponQualityRank(weapon,'slowFiring');
  const defensive=weaponQualityRank(weapon,'defensive');
  const deflection=weaponQualityRank(weapon,'deflection');
  const brawn=Math.max(0,Number(character?.characteristics?.fortaleza??0));
  const agility=Math.max(0,Number(character?.characteristics?.agilidad??0));
  const cumbersomeDifficulty=Math.max(0,cumbersome-brawn);
  const unwieldyDifficulty=Math.max(0,unwieldy-agility);
  return {inaccurate,cumbersome,unwieldy,prepare,limitedAmmo,slowFiring,defensive,deflection,cumbersomeDifficulty,unwieldyDifficulty,difficultyIncrease:cumbersomeDifficulty+unwieldyDifficulty,setback:inaccurate};
}
export function weaponDefenseModifiers(weapon){
  return {melee:weaponQualityRank(weapon,'defensive'),ranged:weaponQualityRank(weapon,'deflection')};
}

export function activeWeaponQualities(weapon,{hit=false,useAutoFire=false}={}){
  const out=[];
  const add=(key,label,rank,cost,extra={})=>{if(rank>0)out.push({key,label,rank,cost,...extra});};
  if(useAutoFire&&weaponHasQuality(weapon,'autofire')&&hit) add('autofire','Automática',1,2,{repeatable:true});
  if(hit){
    add('blast','Explosão',weaponQualityRank(weapon,'blast'),2,{requiresSecondaryTargets:true});
    add('disorient','Desorientação',weaponQualityRank(weapon,'disorient'),2);
    add('stun','Atordoadora',weaponQualityRank(weapon,'stun'),2);
    add('burn','Incendiária',weaponQualityRank(weapon,'burn'),2);
    add('concussive','Concussiva',weaponQualityRank(weapon,'concussive'),2);
    add('ensnare','Enredante',weaponQualityRank(weapon,'ensnare'),2);
    if(weaponHasQuality(weapon,'sunder')) add('sunder','Hendedura',1,1,{repeatable:true});
    add('linked','Vinculada',weaponQualityRank(weapon,'linked'),2,{repeatable:true});
    if(weaponHasQuality(weapon,'knockdown')) add('knockdown','Derrubar',1,2);
  } else {
    add('blast','Explosão',weaponQualityRank(weapon,'blast'),3,{missBlast:true,requiresSecondaryTargets:false});
    add('guided','Guiada',weaponQualityRank(weapon,'guided'),3);
  }
  return out;
}

export function weaponAttackModifiers(weapon,{useAutoFire=false,character=null}={}){
  const accurate=weaponQualityRank(weapon,'accurate');
  const breach=weaponQualityRank(weapon,'breach');
  const pierce=weaponQualityRank(weapon,'pierce');
  const vicious=weaponQualityRank(weapon,'vicious');
  const blast=weaponQualityRank(weapon,'blast');
  const superior=weaponHasQuality(weapon,'superior');
  const inferior=weaponHasQuality(weapon,'inferior');
  const cortosis=weaponHasQuality(weapon,'cortosis');
  const usage=weaponUsageModifiers(weapon,character);
  const autoFireAvailable=weaponHasQuality(weapon,'autofire');
  if(useAutoFire&&!autoFireAvailable)throw new Error('A arma selecionada não possui a qualidade Automática.');
  return {accurate,breach,pierce,vicious,blast,superior,inferior,cortosis,...usage,autoFireAvailable,useAutoFire:Boolean(useAutoFire&&autoFireAvailable),difficultyIncrease:(useAutoFire&&autoFireAvailable?1:0)+usage.difficultyIncrease};
}
export function resolveWeaponHit(weapon,check,{useAutoFire=false}={}){
  if(!weapon||typeof weapon!=='object')throw new Error('Arma inválida.');
  if(!check||typeof check!=='object')throw new Error('Resultado de ataque inválido.');
  let damage=Number(weapon.damage);
  if(!Number.isInteger(damage)||damage<0)throw new Error('Dano da arma inválido.');
  const mods=weaponAttackModifiers(weapon,{useAutoFire});
  if(mods.superior) damage += 1;
  if(mods.inferior) damage = Math.max(1, damage - 1);
  if(!check.success)return {hit:false,damage:0,criticalAvailable:false,blastAvailable:false,autoFireAvailable:mods.autoFireAvailable,autoFireUsed:mods.useAutoFire,additionalHits:0,accurate:mods.accurate,breach:mods.breach,pierce:mods.pierce,vicious:mods.vicious,qualityModifiers:mods,weapon:structuredClone(weapon),check};
  const netSuccess=Math.max(0,Number(check.netSuccess)||0);
  const netAdvantage=Math.max(0,Number(check.netAdvantage)||0) + (mods.superior ? 1 : 0) - (mods.inferior ? 1 : 0);
  const blast=Number.isInteger(weapon.blast)?weapon.blast:(mods.blast>0?mods.blast:null);
  const damageType=weapon.damageType??(weaponHasQuality(weapon,'stunDamage')?'strain':'wounds');
  return {
    hit:true,damage:damage+netSuccess,pierce:Number(weapon.pierce??mods.pierce??0),breach:Number(weapon.breach??mods.breach??0),
    accurate:mods.accurate,vicious:mods.vicious,damageType,stunSettingAvailable:Boolean(weapon.stunSetting||weaponHasQuality(weapon,'stunSetting')),
    superior:mods.superior,inferior:mods.inferior,cortosis:mods.cortosis,
    criticalAvailable:Number.isInteger(weapon.crit)?(Number(check.triumph??0)>0||netAdvantage>=weapon.crit):false,
    criticalCost:Number.isInteger(weapon.crit)?weapon.crit:null,criticalByTriumph:Number.isInteger(weapon.crit)?Number(check.triumph??0)>0:false,
    blastAvailable:Number.isInteger(blast)&&netAdvantage>=Number(weapon.blastActivationCost??2),blastDamage:Number.isInteger(blast)?blast+netSuccess:null,
    autoFireAvailable:mods.autoFireAvailable,autoFireUsed:mods.useAutoFire,additionalHits:0,
    qualityModifiers:mods,weapon:structuredClone(weapon),check
  };
}
