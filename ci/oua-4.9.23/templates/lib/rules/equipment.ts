import type { CharacterSheet } from '../db';

function norm(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}
function has(text: unknown, pattern: RegExp): boolean { return pattern.test(norm(text)); }
function tagsOf(item: CharacterSheet['inventory'][number]): string[] { return Array.isArray(item.tags) ? item.tags.map(String) : []; }
function rewardText(sheet: CharacterSheet): string { return norm(sheet.other.rewards || ''); }
function itemReward(sheet: CharacterSheet, item: CharacterSheet['inventory'][number], name: string): boolean {
  const n=norm(name);
  if(tagsOf(item).some(tag=>norm(tag)===('recompensa:'+n))) return true;
  const r=rewardText(sheet);
  if(!r.includes(n)) return false;
  const itemName=norm(item.name);
  const open=r.indexOf('('), close=r.indexOf(')',open+1);
  if(open>=0&&close>open) return norm(r.slice(open+1,close))===itemName;
  return true;
}

const ARMOR: Record<string,{protection:number;load:number}> = {
  'camisa de couro':{protection:1,load:3},
  'colete de couro':{protection:2,load:6},
  'camisa de malha':{protection:3,load:9},
  'cota de malha':{protection:4,load:12},
};
const SHIELDS: Record<string,{parry:number;load:number}> = {
  'broquel':{parry:1,load:2},
  'escudo':{parry:2,load:4},
  'escudo grande':{parry:3,load:6},
};
const WEAPONS: Record<string,{damage:number;injury:number;load:number;group:string;twoHanded?:boolean}> = {
  'desarmado':{damage:1,injury:0,load:0,group:'Briga'},
  'adaga':{damage:2,injury:14,load:0,group:'Briga'},
  'cassetete':{damage:3,injury:12,load:0,group:'Briga'},
  'tacape':{damage:4,injury:14,load:1,group:'Briga'},
  'espada curta':{damage:3,injury:16,load:1,group:'Espadas'},
  'espada':{damage:4,injury:16,load:2,group:'Espadas'},
  'espada longa':{damage:5,injury:16,load:3,group:'Espadas',twoHanded:true},
  'lança curta':{damage:3,injury:14,load:2,group:'Lanças'},
  'lanca curta':{damage:3,injury:14,load:2,group:'Lanças'},
  'lança':{damage:4,injury:14,load:3,group:'Lanças',twoHanded:true},
  'lanca':{damage:4,injury:14,load:3,group:'Lanças',twoHanded:true},
  'lança longa':{damage:5,injury:16,load:4,group:'Lanças',twoHanded:true},
  'lanca longa':{damage:5,injury:16,load:4,group:'Lanças',twoHanded:true},
  'machado':{damage:5,injury:18,load:2,group:'Machados'},
  'machado de haste longa':{damage:6,injury:18,load:3,group:'Machados',twoHanded:true},
  'machado grande':{damage:7,injury:20,load:4,group:'Machados',twoHanded:true},
  'picareta':{damage:7,injury:18,load:3,group:'Machados',twoHanded:true},
  'arco':{damage:3,injury:14,load:2,group:'Arcos'},
  'arco grande':{damage:4,injury:16,load:4,group:'Arcos'},
};

export function canonicalWeapon(name: string) { return WEAPONS[norm(name)] || null; }
export function isRangedWeapon(name: string, group?: string): boolean {
  return /arco/.test(norm(name)) || /arcos/.test(norm(group));
}
export function isPierceCapable(name: string, group?: string): boolean {
  const g=norm(group||canonicalWeapon(name)?.group||'');
  return /arco|lan[cç]a|espada/.test(g);
}
export function pierceFeatBonus(group?: string): number {
  const g=norm(group);
  if(g.includes('espada')) return 1;
  if(g.includes('arco')) return 2;
  if(g.includes('lan')) return 3;
  return 0;
}
export function heavyBlowBonus(sheet: CharacterSheet, weaponName: string): number {
  const harsh=/m[aã]os inclementes/.test(norm(sheet.other.virtues));
  const two=Boolean(canonicalWeapon(weaponName)?.twoHanded);
  return Math.max(0,Number(sheet.attributes.strength)||0)+(two?1:0)+(harsh?1:0);
}
export function weaponPiercingThreshold(sheet: CharacterSheet, weaponName: string): number {
  const item=sheet.inventory.find(x=>norm(x.name)===norm(weaponName));
  return item && itemReward(sheet,item,'Afiada') ? 9 : 10;
}
export function protectionRollBonus(sheet: CharacterSheet): number {
  return Math.max(0,Number((sheet.attributes as any).protectionBonus)||0);
}
export function protectionRollMode(sheet: CharacterSheet, rangedPiercing=false): 'normal'|'favoured'|'ill-favoured' {
  if(rangedPiercing && /disparo feroz/.test(norm(sheet.other.virtues))) return 'ill-favoured';
  if(/duro como pedra/.test(norm(sheet.other.virtues)) && !sheet.attributes.miserable) return 'favoured';
  return 'normal';
}
export function automaticInspired(sheet: CharacterSheet): boolean {
  return /coragem no aperto/.test(norm(sheet.other.virtues))
    && Boolean(sheet.attributes.miserable||sheet.attributes.weary||sheet.attributes.wounded);
}
export function rangerIgnoresJourneyFatigue(sheet: CharacterSheet): boolean {
  if(!/resist[eê]ncia do patrulheiro/.test(norm(sheet.other.virtues))) return false;
  const armor=norm(sheet.combat.armor);
  const leather=!armor||armor==='nenhum'||armor.includes('couro');
  const shield=norm(sheet.combat.shield);
  return leather && (!shield||shield==='nenhum');
}
export function restStrengthMultiplier(sheet: CharacterSheet): number {
  return /resistente como ra[ií]zes antigas/.test(norm(sheet.other.virtues)) ? 2 : 1;
}

function canonicalLoad(sheet: CharacterSheet,item: CharacterSheet['inventory'][number]): number {
  const n=norm(item.name);
  let load=ARMOR[n]?.load ?? SHIELDS[n]?.load ?? WEAPONS[n]?.load ?? Math.max(0,Number(item.load ?? item.weight)||0);
  if(itemReward(sheet,item,'Fabricação Habilidosa')) load=Math.max(0,load-2);
  const armorOrHelm=Boolean(ARMOR[n])||/elmo|helmet/.test(n)||/armadura|elmo|headgear/i.test(String(item.category||''));
  if(armorOrHelm && /resoluto/.test(norm(sheet.other.culturalBlessing))) load=Math.ceil(load/2);
  return load;
}
function canonicalProtection(item: CharacterSheet['inventory'][number]): number {
  const n=norm(item.name);
  if(ARMOR[n]) return ARMOR[n].protection;
  if(/elmo|helmet/.test(n)) return 1;
  return Math.max(0,Number(item.protection)||0);
}
function shieldParry(sheet: CharacterSheet,item: CharacterSheet['inventory'][number]): number {
  const base=SHIELDS[norm(item.name)]?.parry ?? 0;
  return base+(itemReward(sheet,item,'Reforçado')?1:0);
}

export function normalizeEquipmentMechanics(input: CharacterSheet): CharacterSheet {
  const next=structuredClone(input);
  next.inventory=(next.inventory||[]).map(item=>{
    const copy={...item};
    copy.load=canonicalLoad(next,copy);
    copy.weight=copy.load;
    const p=canonicalProtection(copy);
    if(p>0) copy.protection=p;
    return copy;
  });

  const equipped=next.inventory.filter(item=>item.equipped!==false);
  const protection=equipped.reduce((sum,item)=>sum+canonicalProtection(item),0);
  const adjusted=equipped.some(item=>itemReward(next,item,'Ajustado')) ? 2 : 0;
  next.attributes.protection=protection;
  (next.attributes as any).protectionBonus=adjusted;
  next.attributes.load=next.inventory.reduce((sum,item)=>sum+canonicalLoad(next,item)*Math.max(1,Number(item.quantity)||1),0);

  const shield=equipped.find(item=>SHIELDS[norm(item.name)]||/escudo|broquel/.test(norm(item.name)));
  const desiredShield=shield?shieldParry(next,shield):0;
  const recorded=Number((next.other as any).equipmentParryBonusApplied);
  let prior=Number.isFinite(recorded)?recorded:0;
  if(!Number.isFinite(recorded)&&shield){
    const tagged=tagsOf(shield).map(tag=>norm(tag)).find(tag=>tag.startsWith('bloqueio:+'));
    if(tagged) prior=Math.max(0,Number(tagged.split('+')[1])||0);
    else if(itemReward(next,shield,'Reforçado')&&norm(shield.name)==='broquel') prior=2;
  }
  next.combat.parry=Math.max(0,(Number(next.combat.parry)||0)-prior+desiredShield);
  (next.other as any).equipmentParryBonusApplied=desiredShield;

  next.combat.weapons=(next.combat.weapons||[]).map(weapon=>{
    const item=next.inventory.find(x=>norm(x.name)===norm(weapon.name));
    const base=canonicalWeapon(weapon.name);
    if(!base) return weapon;
    let damage=base.damage, injury=base.injury;
    if(item&&itemReward(next,item,'Atroz')) damage+=1;
    if(item&&itemReward(next,item,'Cruel')) injury+=2;
    return {...weapon,damage,injury,group:weapon.group||base.group};
  });

  const burden=Math.max(0,next.attributes.load)+Math.max(0,next.attributes.fatigue);
  next.attributes.weary=next.attributes.enduranceCurrent<=burden;
  next.attributes.miserable=next.attributes.hopeCurrent<=next.attributes.shadow;
  return next;
}
