import { WWN_SRD_CLASSES } from './wwn-srd-class-catalog.js';
const freeze=v=>Object.freeze(v);const SRC=(locator)=>freeze({sourceId:'SRC-WWN-SRD-1',locator});
const CLOSED=new Map([
 ['HEALER|Healing Touch',{mode:'CLOSED_HEALING',resolverId:'ART_HEALING_TOUCH_V1',sourceRefs:[SRC('§1.5.9.1 Healer Arts — Healing Touch')] }],
 ['HEALER|Empowered Healer',{mode:'PASSIVE_MODIFIER',resolverId:'ART_EMPOWERED_HEALER_V1',sourceRefs:[SRC('§1.5.9.1 Healer Arts — Empowered Healer')] }],
 ['HEALER|Facile Healer',{mode:'PASSIVE_MODIFIER',resolverId:'ART_FACILE_HEALER_V1',sourceRefs:[SRC('§1.5.9.1 Healer Arts — Facile Healer')] }],
 ['HEALER|Far Healer',{mode:'PASSIVE_MODIFIER',resolverId:'ART_FAR_HEALER_V1',sourceRefs:[SRC('§1.5.9.1 Healer Arts — Far Healer')] }],
 ['HEALER|Refined Restoration',{mode:'PASSIVE_REST_MODIFIER',resolverId:'ART_REFINED_RESTORATION_V1',sourceRefs:[SRC('§1.5.9.1 Healer Arts — Refined Restoration')] }],
 ['HEALER|Vital Furnace',{mode:'CLOSED_REACTION',resolverId:'ART_VITAL_FURNACE_V1',sourceRefs:[SRC('§1.5.9.1 Healer Arts — Vital Furnace')] }],
 ['ELEMENTALIST|Elemental Resilience',{mode:'PASSIVE_DEFENSE',resolverId:'ART_ELEMENTAL_RESILIENCE_V1',sourceRefs:[SRC('§1.5.7.1 Elementalist Arts — Elemental Resilience')] }],
 ['ELEMENTALIST|Pavis of Elements',{mode:'CLOSED_STATUS',resolverId:'ART_PAVIS_OF_ELEMENTS_V1',sourceRefs:[SRC('§1.5.7.1 Elementalist Arts — Pavis of Elements')] }],
 ['ELEMENTALIST|Steps of Air',{mode:'CLOSED_STATUS',resolverId:'ART_STEPS_OF_AIR_V1',sourceRefs:[SRC('§1.5.7.1 Elementalist Arts — Steps of Air')] }],
 ['ELEMENTALIST|Stunning Shock',{mode:'CLOSED_SAVE_EFFECT',resolverId:'ART_STUNNING_SHOCK_V1',sourceRefs:[SRC('§1.5.7.1 Elementalist Arts — Stunning Shock')] }],
 ['ELEMENTALIST|Thermal Shield',{mode:'CLOSED_DAMAGE_NEGATION',resolverId:'ART_THERMAL_SHIELD_V1',sourceRefs:[SRC('§1.5.7.1 Elementalist Arts — Thermal Shield')] }],
 ['NECROMANCER|Cold Flesh',{mode:'PASSIVE_DEFENSE',resolverId:'ART_COLD_FLESH_V1',sourceRefs:[SRC('§1.5.14.1 Necromancer Arts — Cold Flesh')] }],
 ['NECROMANCER|Life Bridge',{mode:'CLOSED_HP_TRANSFER',resolverId:'ART_LIFE_BRIDGE_V1',sourceRefs:[SRC('§1.5.14.1 Necromancer Arts — Life Bridge')] }],
 ['NECROMANCER|Unaging',{mode:'PASSIVE_DEFENSE',resolverId:'ART_UNAGING_V1',sourceRefs:[SRC('§1.5.14.1 Necromancer Arts — Unaging')] }],
 ['NECROMANCER|Unliving Persistence',{mode:'CLOSED_STABILIZATION',resolverId:'ART_UNLIVING_PERSISTENCE_V1',sourceRefs:[SRC('§1.5.14.1 Necromancer Arts — Unliving Persistence')] }],
 ['THOUGHT_NOBLE|Mind Over Matter',{mode:'CLOSED_SELF_EFFECT',resolverId:'ART_MIND_OVER_MATTER_V1',sourceRefs:[SRC('§1.5.16.1 Thought Noble Arts — Mind Over Matter')] }],
 ['THOUGHT_NOBLE|Thoughts Like Razors',{mode:'CLOSED_SAVE_DAMAGE',resolverId:'ART_THOUGHTS_LIKE_RAZORS_V1',sourceRefs:[SRC('§1.5.16.1 Thought Noble Arts — Thoughts Like Razors')] }],
 ['VOWED|Martial Style',{mode:'PASSIVE_COMBAT',resolverId:'ART_MARTIAL_STYLE_V1',sourceRefs:[SRC('§1.5.17.1 Vowed Arts — Martial Style')] }],
 ['VOWED|Unarmed Might',{mode:'PASSIVE_COMBAT',resolverId:'ART_UNARMED_MIGHT_V1',sourceRefs:[SRC('§1.5.17.1 Vowed Arts — Unarmed Might')] }],
 ['VOWED|Unarmored Defense',{mode:'PASSIVE_DEFENSE',resolverId:'ART_UNARMORED_DEFENSE_V1',sourceRefs:[SRC('§1.5.17.1 Vowed Arts — Unarmored Defense')] }],
]);
export const WWN_ART_EFFECT_CONTRACT_CATALOG=freeze(Object.values(WWN_SRD_CLASSES).flatMap(c=>(c.arts??[]).map(name=>{const o=CLOSED.get(`${c.classId}|${name}`);return freeze({classId:c.classId,artName:name,classification:o?.mode??'GM_ADJUDICATION_REQUIRED',resolverId:o?.resolverId??null,sourceRefs:freeze([c.sourceRef,...(o?.sourceRefs??[])].filter(Boolean))});})));
export function artEffectContract(classId,artName){return WWN_ART_EFFECT_CONTRACT_CATALOG.find(x=>x.classId===String(classId??'').toUpperCase()&&x.artName===String(artName??''))??null;}
export function artEffectCoverageAudit(){const closed=WWN_ART_EFFECT_CONTRACT_CATALOG.filter(x=>x.resolverId).length;return freeze({status:'OK',artCount:WWN_ART_EFFECT_CONTRACT_CATALOG.length,contractCount:WWN_ART_EFFECT_CONTRACT_CATALOG.length,closedResolverCount:closed,adjudicationCount:WWN_ART_EFFECT_CONTRACT_CATALOG.length-closed});}
export function pavisArmorClass({baseArmorClass}){if(!Number.isInteger(baseArmorClass))throw new TypeError('baseArmorClass integer required');return freeze({rule:'WWN_PAVIS_OF_ELEMENTS_V1',baseArmorClass,armorClass:Math.min(18,baseArmorClass+4),bonusApplied:Math.min(4,Math.max(0,18-baseArmorClass)),sourceRefs:freeze([SRC('§1.5.7.1 Elementalist Arts — Pavis of Elements')])});}
export function elementalResilience({magicalOrExtreme=false,damage}){if(!Number.isInteger(damage)||damage<0)throw new RangeError('damage must be integer >= 0');return freeze({rule:'WWN_ELEMENTAL_RESILIENCE_V1',immune:!magicalOrExtreme,damageAfter:magicalOrExtreme?Math.floor(damage/2):0,sourceRefs:freeze([SRC('§1.5.7.1 Elementalist Arts — Elemental Resilience')])});}
export function coldFleshDefense({level,shockDamage}){if(!Number.isInteger(level)||level<1||!Number.isInteger(shockDamage)||shockDamage<0)throw new RangeError('invalid cold flesh input');return freeze({rule:'WWN_COLD_FLESH_V1',naturalArmorClass:12+Math.floor(level/2),shockDamageAfter:Math.min(2,shockDamage),sourceRefs:freeze([SRC('§1.5.14.1 Necromancer Arts — Cold Flesh')])});}
export function unarmoredDefense({level}){if(!Number.isInteger(level)||level<1)throw new RangeError('level required');return freeze({rule:'WWN_VOWED_UNARMORED_DEFENSE_V1',baseArmorClass:13+Math.floor(level/2),sourceRefs:freeze([SRC('§1.5.17.1 Vowed Arts — Unarmored Defense')])});}
