/**
 * U26/G05 — real IndexedDB version upgrades 12, 13, 14 -> 15.
 * A consumable resource die contributes one burden unit just like the live
 * EncumbranceRulesEngine, including legacy characters imported before V15.
 */
import 'fake-indexeddb/auto';
import assert from 'node:assert/strict';
import { openDB, deleteDB } from 'idb';
import { getDb, invalidateDbConnection } from '../src/db/db.ts';
import { EncumbranceRulesEngine } from '../src/services/EncumbranceRulesEngine.ts';

import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const OLD_VERSIONS=[12,13,14];
if(process.argv[2]!=='--child'){
  let total=0;
  for(const version of OLD_VERSIONS){
    const args=[...process.execArgv,fileURLToPath(import.meta.url),'--child',String(version)];
    const subprocess=spawnSync(process.execPath,args,{stdio:'inherit',env:process.env,timeout:65000});
    if(subprocess.error||subprocess.status!==0)
      throw new Error('Migration '+version+' failed/blocked: exit='+subprocess.status+
        ' signal='+subprocess.signal+' error='+String(subprocess.error||''));
    total+=12;
  }
  console.log('U26_G05_LEGACY_RESOURCE_DICE_MIGRATIONS PASS '+total+'/'+total+
    ' (3 independent real IndexedDB schema upgrades 12/13/14->15; no fake migration mocks)');
}else{
  const oldVersion=Number(process.argv[3]);
  assert.ok(OLD_VERSIONS.includes(oldVersion),'valid schema fixture');
  console.log('UPGRADE_BEGIN '+oldVersion+' -> 15');
  invalidateDbConnection();
  // Each schema version runs in a fresh Node process and fresh IDB factory.
  const legacy=await openDB('braseiro-db',oldVersion,{
    upgrade(db) { db.createObjectStore('characters',{keyPath:'id'}); },
  });
  const hero={
    id:'legacy-encumbrance-'+oldVersion,
    name:'Sobrevivente '+oldVersion,
    attributes:{strength:{current:3,max:3},agility:{current:2,max:3},
      wits:{current:2,max:3},empathy:{current:2,max:3}},
    weapons:[{id:'axe',name:'Machado',quantity:1,encumbranceClass:'normal'}],
    armor:[],gear:[],coins:{copper:100,silver:0,gold:0},
    resourceDice:{food:'d6',water:'d8',arrows:'d10',torches:null},
    encumbrance:{current:777,max:777},willpowerPoints:3,
  };
  await legacy.put('characters',hero);
  const special={...hero,id:'source-weight-'+oldVersion,
    weapons:[{id:'zero',name:'Arma zerada',quantity:0,encumbranceClass:'normal'}],
    gear:[{id:'custom',name:'Conjunto de pesos',quantity:2,encumbranceClass:'heavy',sourceWeightUnits:.25},
      {id:'none',name:'Item vazio',quantity:0,encumbranceClass:'heavy'}],
    coins:{copper:Infinity,silver:100,gold:0},
    resourceDice:{food:null,water:null,arrows:null,torches:null},
    encumbrance:{current:999,max:999}};
  await legacy.put('characters',special);
  legacy.close();
  invalidateDbConnection();
  console.log('UPGRADE_SEEDED '+oldVersion);
  const upgraded=await getDb();
  let checks=0;
  assert.equal(upgraded.version,15,'real IndexedDB upgrade to version 15');checks++;
  const migrated=await upgraded.get('characters',hero.id);
  assert.ok(migrated,'legacy character retained');checks++;
  const expected=EncumbranceRulesEngine.calculate(migrated);
  assert.equal(expected.current,4.5,'1 axe + half coin weight + 3 active resource dice');checks++;
  assert.equal(migrated.encumbrance.current,expected.current,'migrated carried weight matches live engine');checks++;
  assert.equal(migrated.encumbrance.max,expected.max,'migrated capacity matches live engine');checks++;
  assert.deepEqual(migrated.resourceDice,hero.resourceDice,'no resource die lost in migration');checks++;
  const restoredSpecial=await upgraded.get('characters',special.id);
  assert.ok(restoredSpecial,'nonstandard but legal legacy item weights preserved');checks++;
  const specialWeight=EncumbranceRulesEngine.calculate(restoredSpecial);
  assert.equal(specialWeight.current,1,'custom .25*2 + valid 100 coins .5, Infinity ignored');checks++;
  assert.equal(restoredSpecial.encumbrance.current,specialWeight.current,'custom-weight migration matches runtime');checks++;
  assert.equal(restoredSpecial.encumbrance.max,specialWeight.max,'custom-weight capacity matches runtime');checks++;
  assert.equal(restoredSpecial.weapons[0].quantity,0,'zero-quantity weapon preserved');checks++;
  assert.equal(restoredSpecial.coins.copper,Infinity,'nonfinite old coin remains available for user inspection');checks++;
  upgraded.close();invalidateDbConnection();
  console.log('UPGRADE_PASS '+oldVersion+' '+checks+'/'+checks);
}
