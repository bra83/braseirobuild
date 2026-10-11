/**
 * G02: real MagicCastingService IndexedDB transaction with a GM-owned spell.
 * A source-proven "requiresGm" targetPatch must be stored as pending, NEVER
 * applied early to a sheet or encounter; already paid WP remains paid.
 */
import assert from 'node:assert/strict';
import 'fake-indexeddb/auto';
import { openDB } from 'idb';
import { readFileSync } from 'node:fs';
import { ForbiddenLandsSpellEffectEngine } from '../src/services/ForbiddenLandsSpellEffectEngine.ts';
const source=readFileSync(new URL('../src/services/MagicCastingService.ts',import.meta.url),'utf8');
assert.match(source,/const genericEffectCommitted=Boolean\(effectResult\?\.valid&&!effectResult\.requiresGm/);
assert.match(source,/if\(effectTarget&&effectResult\?\.targetPatch&&genericEffectCommitted\)/);
assert.match(source,/else if \(effectCombatant && effectResult\?\.targetPatch && genericEffectCommitted/);
const candidate=ForbiddenLandsSpellEffectEngine.resolve({spellId:'iron-song',power:1,context:{ritual:true,rawMaterial:true}});
assert.equal(candidate.valid,true);
assert.equal(candidate.requiresGm,true);
assert.ok(candidate.targetPatch?.equipmentBonus>0);
let checks=4;
const DB_NAME='braseiro-db';
const db14=await openDB(DB_NAME,14,{upgrade(db){
  if(!db.objectStoreNames.contains('characters'))db.createObjectStore('characters',{keyPath:'id'});
  if(!db.objectStoreNames.contains('app_meta'))db.createObjectStore('app_meta',{keyPath:'id'});
  if(!db.objectStoreNames.contains('world_state'))db.createObjectStore('world_state',{keyPath:'id'});
}});
const stats={strength:{current:3,max:3},agility:{current:3,max:3},wits:{current:3,max:3},empathy:{current:3,max:3}};
const caster={id:'gm-caster',name:'Runa',status:'alive',attributes:stats,skills:{},
  campaignId:undefined,talents:{'Caminho das Rochas':3},willpowerPoints:5,
  conditions:[],criticalInjuries:[],gear:[{id:'metal',name:'Barra de ferro',quantity:1}],
  weapons:[],armor:[],coins:{copper:0,silver:0,gold:0}};
const target={...caster,id:'gm-target',name:'Ivar',talents:{},willpowerPoints:0,
  gear:[{id:'blade',name:'Espada de ferro',quantity:1}],
  statusEffects:[]};
await db14.put('characters',caster);
await db14.put('characters',target);
db14.close();
const { MagicCastingService } = await import('../src/services/MagicCastingService.ts');
// Do not keep a v14 handle open: it would block the production v15 schema
// upgrade forever. Use the same upgraded connection as the casting service.
const {getDb} = await import('../src/db/db.ts');
const db=await getDb();
const before=await db.get('characters','gm-target');
const result=await MagicCastingService.castCharacter({
  characterId:'gm-caster',spellId:'iron-song',spellName:'Canção do Ferro',
  spellRank:3,talentName:'Caminho das Rochas',ritual:true,
  effectTargetCharacterId:'gm-target',willpowerSpent:1,effectContext:{ritual:true,rawMaterial:true},rng:()=>.999,
});
assert.equal(result.event.effectResult?.valid,true);checks++;
assert.equal(result.event.effectResult?.requiresGm,true);checks++;
assert.ok(result.event.pendingConsequences.includes('spell-effect-requires-gm'));checks++;
assert.deepEqual(await db.get('characters','gm-target'),before,
  'A spell awaiting GM adjudication must not modify the target before consent');checks++;
const record=await db.get('app_meta','magic-casts:gm-caster');
assert.ok(record.value.events.some(ev=>ev.id===result.event.id&&ev.pendingConsequences.includes('spell-effect-requires-gm')));checks++;
assert.equal((await db.get('characters','gm-caster')).willpowerPoints,4,'one spell paid once');checks++;
db.close();
console.log('U26_G02_GM_SPELL_PENDING_TRANSACTION PASS '+checks+'/'+checks+
  ' (actual source rules engine, IndexedDB cast receipt, target unchanged, one paid cost; not an Android/Gemini test)');
