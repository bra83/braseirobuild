import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  effectiveArmorClass071,
  applyPcDamageMitigations071,
  activeEverlastingEffect071,
  inspectThermalShieldReaction071,
  applyThermalShieldReactionInPlace071,
  expireRoundEffectsInPlace071,
} from '../core/combat-effect-lifecycle071.js';

function art(name,classId='ELEMENTALIST'){return {name,classId};}
function pc(id='pc',overrides={}){return {characterId:id,level:4,attributes:{Strength:10,Dexterity:14,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10},hp:{current:10,max:10},inventory:[],arts:[],effort:{ELEMENTALIST:{max:2,current:2,committed:{day:0,scene:0,indefinite:0}}},...overrides};}
function state(chars,effects=[]){const first=Object.values(chars)[0];return {characterState:structuredClone(first),charactersState:structuredClone(chars),encounterState:{magicEffects:structuredClone(effects)}};}
function hit({damage=10,pre=10,shock=0,shockEligible=false,hit=true}={}){return {hit,damage,preShockDamage:pre,shockDamage:shock,shockEligible,damageKind:hit?'HIT':'SHOCK'};}

test('Pavis stacks with Total Defense but the resulting AC never exceeds 18',()=>{const c=pc('p',{artState:{pavisOfElements:{active:true}}});const r=effectiveArmorClass071({character:c,armorProjection:{status:'OK',armorClass:15,dexterityModifier:2},totalDefense:true});assert.equal(r.status,'OK');assert.equal(r.armorClass,18);assert.equal(r.applied.some(x=>x.effect==='PAVIS_OF_ELEMENTS'&&x.cap===18),true);});

test('Vowed Unarmored Defense uses 13 + half level + Dex only when no armor or shield is readied',()=>{const c=pc('v',{arts:[art('Unarmored Defense','VOWED')]});let r=effectiveArmorClass071({character:c,armorProjection:{status:'OK',armorClass:12,dexterityModifier:2}});assert.equal(r.armorClass,17);const shield={kind:'SHIELD',carryState:'READIED'};r=effectiveArmorClass071({character:{...c,inventory:[shield]},armorProjection:{status:'OK',armorClass:14,dexterityModifier:2}});assert.equal(r.armorClass,14);});

test('Cold Flesh supplies its exact natural AC floor without inventing extra shield or Dexterity text',()=>{const c=pc('n',{level:6,arts:[art('Cold Flesh','NECROMANCER')]});const r=effectiveArmorClass071({character:c,armorProjection:{status:'OK',armorClass:10,dexterityModifier:2}});assert.equal(r.armorClass,15);assert.equal(r.applied[0].effect,'COLD_FLESH_NATURAL_AC');});

test('Cold Flesh caps a pure Shock instance at 2 damage',()=>{const c=pc('n',{arts:[art('Cold Flesh','NECROMANCER')]});const s=state({n:c});const r=applyPcDamageMitigations071({state:s,combat:{round:1},targetId:'n',resolution:hit({damage:5,pre:0,shock:5,shockEligible:true,hit:false})});assert.equal(r.resolution.damage,2);assert.equal(r.resolution.coldFleshShockCapped,true);});

test('Cold Flesh caps only the Shock floor and preserves larger ordinary hit damage',()=>{const c=pc('n',{arts:[art('Cold Flesh','NECROMANCER')]});const s=state({n:c});const r=applyPcDamageMitigations071({state:s,combat:{round:1},targetId:'n',resolution:hit({damage:7,pre:4,shock:7,shockEligible:true,hit:true})});assert.equal(r.resolution.damage,4);});

test('Elemental Resilience ignores explicitly-classified mundane cold or sub-furnace heat',()=>{const c=pc('e',{arts:[art('Elemental Resilience')]});const s=state({e:c});const r=applyPcDamageMitigations071({state:s,combat:{round:1},targetId:'e',resolution:hit({damage:9}),damageContext:{element:'FIRE',elementalResilienceClass:'MUNDANE_COLD_OR_SUBFURNACE_HEAT'}});assert.equal(r.resolution.damage,0);});

test('Elemental Resilience halves explicit magical/extreme flame or frost damage with integer floor',()=>{const c=pc('e',{arts:[art('Elemental Resilience')]});const s=state({e:c});const r=applyPcDamageMitigations071({state:s,combat:{round:1},targetId:'e',resolution:hit({damage:9}),damageContext:{element:'FROST',elementalResilienceClass:'MAGICAL_OR_EXTREME'}});assert.equal(r.resolution.damage,4);});

test('Elemental Resilience does not guess from an unclassified damage source',()=>{const c=pc('e',{arts:[art('Elemental Resilience')]});const s=state({e:c});const r=applyPcDamageMitigations071({state:s,combat:{round:1},targetId:'e',resolution:hit({damage:9}),damageContext:{element:'FIRE'}});assert.equal(r.resolution.damage,9);});

test('Everlasting clamps incoming damage so an affected living ally remains at 1 HP',()=>{const ally=pc('a',{hp:{current:3,max:10}}),caster=pc('m');const e={effectId:'ever',spellId:'EVERLASTING',casterId:'m',allyIds:['a'],startsRound:4,endsAfterRound:8,minimumHp:1};const s=state({m:caster,a:ally},[e]);assert.equal(activeEverlastingEffect071(s,'a',6)?.effectId,'ever');const r=applyPcDamageMitigations071({state:s,combat:{round:6},targetId:'a',resolution:hit({damage:20})});assert.equal(r.resolution.damage,2);assert.equal(r.resolution.everlastingFloorApplied,true);});

test('Everlasting is inactive outside its five-round window and never revives a target already at zero',()=>{const a=pc('a',{hp:{current:3,max:10}}),z=pc('z',{hp:{current:0,max:10}}),m=pc('m');const e={effectId:'ever',spellId:'EVERLASTING',casterId:'m',allyIds:['a','z'],startsRound:4,endsAfterRound:8,minimumHp:1};let s=state({m,a,z},[e]);assert.equal(applyPcDamageMitigations071({state:s,combat:{round:9},targetId:'a',resolution:hit({damage:20})}).resolution.damage,20);assert.equal(applyPcDamageMitigations071({state:s,combat:{round:6},targetId:'z',resolution:hit({damage:20})}).resolution.damage,20);});

test('Everlasting expires after its fifth completed round and leaves a living caster at exactly 1 HP',()=>{const m=pc('m',{hp:{current:8,max:10}}),a=pc('a');const s=state({m,a},[{effectId:'ever',spellId:'EVERLASTING',casterId:'m',allyIds:['a'],startsRound:4,endsAfterRound:8,minimumHp:1}]);const expired=expireRoundEffectsInPlace071({state:s,completedRound:8});assert.equal(expired.length,1);assert.equal(s.charactersState.m.hp.current,1);assert.equal(s.encounterState.magicEffects[0].expired,true);assert.equal(activeEverlastingEffect071(s,'a',8),null);});

test('Everlasting expiration never resurrects a caster already at zero HP',()=>{const m=pc('m',{hp:{current:0,max:10}}),a=pc('a');const s=state({m,a},[{effectId:'ever',spellId:'EVERLASTING',casterId:'m',allyIds:['a'],startsRound:4,endsAfterRound:8,minimumHp:1}]);expireRoundEffectsInPlace071({state:s,completedRound:8});assert.equal(s.charactersState.m.hp.current,0);});

test('Thermal Shield requires an explicit decision when a visible eligible caster can react',()=>{const caster=pc('m',{arts:[art('Thermal Shield')]}),target=pc('a');const s=state({m:caster,a:target});const i=inspectThermalShieldReaction071({state:s,targetId:'a',resolution:hit({damage:7}),damageContext:{element:'FIRE'},visibleCasterIds:['m']});assert.equal(i.status,'DECISION_REQUIRED');assert.equal(i.candidates[0].casterId,'m');const gate=applyThermalShieldReactionInPlace071({state:s,targetId:'a',resolution:hit({damage:7}),damageContext:{element:'FIRE'},visibleCasterIds:['m']});assert.equal(gate.status,'THERMAL_SHIELD_DECISION_REQUIRED');assert.equal(s.charactersState.m.effort.ELEMENTALIST.committed.scene,0);});

test('Thermal Shield USE commits one scene Effort and negates exactly that fire/frost damage instance',()=>{const caster=pc('m',{arts:[art('Thermal Shield')]}),target=pc('a');const s=state({m:caster,a:target});const r=applyThermalShieldReactionInPlace071({state:s,targetId:'a',resolution:hit({damage:7}),damageContext:{element:'FROST'},visibleCasterIds:['m'],decision:{action:'USE',casterId:'m'}});assert.equal(r.status,'USED');assert.equal(r.resolution.damage,0);assert.equal(r.resolution.damageBeforeThermalShield,7);assert.equal(s.charactersState.m.effort.ELEMENTALIST.committed.scene,1);});

test('Thermal Shield DECLINE preserves damage and spends no Effort',()=>{const caster=pc('m',{arts:[art('Thermal Shield')]}),target=pc('a');const s=state({m:caster,a:target});const r=applyThermalShieldReactionInPlace071({state:s,targetId:'a',resolution:hit({damage:7}),damageContext:{element:'FIRE'},visibleCasterIds:['m'],decision:{action:'DECLINE'}});assert.equal(r.status,'DECLINED');assert.equal(r.resolution.damage,7);assert.equal(s.charactersState.m.effort.ELEMENTALIST.committed.scene,0);});

test('Thermal Shield does not invent visibility or eligibility when no visible caster evidence exists',()=>{const caster=pc('m',{arts:[art('Thermal Shield')]}),target=pc('a');const s=state({m:caster,a:target});const r=inspectThermalShieldReaction071({state:s,targetId:'a',resolution:hit({damage:7}),damageContext:{element:'FIRE'},visibleCasterIds:[]});assert.equal(r.status,'NO_ELIGIBLE_CASTER');});

test('combat-service A is wired to the 071 lifecycle rather than a parallel replacement engine',()=>{const src=fs.readFileSync(new URL('../core/combat-service.js',import.meta.url),'utf8');assert.match(src,/effectiveArmorClass071/);assert.match(src,/applyPcDamageMitigations071/);assert.match(src,/applyThermalShieldReactionInPlace071/);assert.match(src,/expireRoundEffectsInPlace071/);assert.match(src,/THERMAL_SHIELD_DECISION_REQUIRED/);assert.match(src,/resolveAttackFromPhysicalDice/);assert.match(src,/applyDefensesToAttack/);});
