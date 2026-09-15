import test from 'node:test';
import assert from 'node:assert/strict';
import { DeterministicRng } from '../app/src/main/assets/www/core/rng-core.js';
import { assessEncumbrance, bundledGearEncumbrance, encumbranceCapacities, inspectInventoryEvidence } from '../app/src/main/assets/www/core/wwn-inventory-core.js';
import { expeditionSupplyNeeds, exploreHexDays, planTravel, resolveTravelDay, travelRateMph } from '../app/src/main/assets/www/core/wwn-travel-core.js';
import { factionIncome, factionMaximumHp, factionMovementMiles, repairAssetAmount, resolveFactionAttributeCheck, resolveFactionAttack } from '../app/src/main/assets/www/core/wwn-faction-core.js';
import { appendNpcMemory, projectNpcForGm, projectNpcForPlayer } from '../app/src/main/assets/www/core/npc-registry.js';

function fixedRng(results){let i=0;return{die(sides,stream='fixed'){const result=results[i++];if(!Number.isInteger(result)||result<1||result>sides)throw new Error(`bad result ${result}/d${sides}`);return{algorithm:'fixed-test',stream,counterBefore:i-1,rawHex:'0',sides,result};}};}

test('IRIA Strength 7 yields 7 Stowed and 3 Readied capacity',()=>assert.deepEqual(encumbranceCapacities(7),{stowed:7,readied:3}));
test('encumbrance overload tiers yield 30/20/10 movement and impractical overflow',()=>{
  assert.equal(assessEncumbrance({strengthScore:10,readiedPoints:5,stowedPoints:10}).moveFeet,30);
  assert.equal(assessEncumbrance({strengthScore:10,readiedPoints:6,stowedPoints:10}).moveFeet,20);
  assert.equal(assessEncumbrance({strengthScore:10,readiedPoints:8,stowedPoints:10}).moveFeet,10);
  assert.equal(assessEncumbrance({strengthScore:10,readiedPoints:10,stowedPoints:10}).status,'IMPRACTICAL');
});
test('bundled eligible small gear counts 3 items per encumbrance point',()=>assert.equal(bundledGearEncumbrance({itemCount:7,eligible:true}).encumbrance,3));
test('missing inventory remains MISSING_EVIDENCE instead of invented equipment',()=>assert.equal(inspectInventoryEvidence({name:'Iria'}).status,'MISSING_EVIDENCE'));

test('overland rates follow terrain and modifiers',()=>{
  assert.equal(travelRateMph({terrain:'PLAINS'}),3);
  assert.equal(travelRateMph({terrain:'LIGHT_FOREST_OR_DESERT',road:true}),3); // road cap
  assert.equal(travelRateMph({terrain:'MOUNTAINS_OR_DIRE_WASTELANDS',foulWeather:true}),0.25);
  assert.equal(planTravel({terrain:'DENSE_FOREST_OR_RUGGED_HILLS',hours:10}).miles,15);
});
test('six-mile hex exploration takes GM-selected 1/2/3 days and supplies scale by people',()=>{
  assert.equal(exploreHexDays({concealmentMultiplier:1}).days,1); assert.equal(exploreHexDays({concealmentMultiplier:3}).days,3);
  assert.deepEqual(expeditionSupplyNeeds({people:4,days:2,hotDryWaterMultiplier:2}).foodUnits,8);
  assert.deepEqual(expeditionSupplyNeeds({people:4,days:2,hotDryWaterMultiplier:2}).waterUnits,16);
});
test('travel day uses two explicit GM encounter dice and consumes explicit supplies',()=>{
  const r=resolveTravelDay({rng:fixedRng([1,4]),terrain:'PLAINS',hours:10,dayEncounterDie:6,nightEncounterDie:8,supplies:{foodUnits:3,waterUnits:3},people:2});
  assert.equal(r.plan.miles,30); assert.equal(r.dayEncounter.encounter,true); assert.equal(r.nightEncounter.encounter,false); assert.deepEqual(r.supplyResult.after,{foodUnits:1,waterUnits:1});
});

test('faction income and maximum HP follow canonical tables',()=>{
  assert.equal(factionIncome({force:3,cunning:2,wealth:5}),4);
  assert.equal(factionMaximumHp({force:3,cunning:2,wealth:5}),15);
  assert.equal(factionMovementMiles({mobile:true}),200);
  assert.deepEqual(repairAssetAmount({relevantAttribute:5,applicationNumber:2}).hpRepaired,3);
  assert.deepEqual(repairAssetAmount({relevantAttribute:5,applicationNumber:2}).treasureCost,2);
});
test('faction attribute-check tie belongs to defender',()=>{
  const r=resolveFactionAttributeCheck({rng:fixedRng([5,5]),attackerAttribute:4,defenderAttribute:4});
  assert.equal(r.attackerWins,false); assert.equal(r.defenderWins,true);
});
test('faction attack rolls attack damage on success and counterattack damage on failure',()=>{
  const success=resolveFactionAttack({rng:fixedRng([10,1,6]),attackerAttribute:4,defenderAttribute:4,attackDamage:'1d8',counterDamage:'1d6'});
  assert.equal(success.outcome,'ATTACK_SUCCESS'); assert.equal(success.damageToDefender,6);
  const fail=resolveFactionAttack({rng:fixedRng([1,10,5]),attackerAttribute:4,defenderAttribute:4,attackDamage:'1d8',counterDamage:'1d6'});
  assert.equal(fail.outcome,'ATTACK_FAILED_COUNTERATTACK'); assert.equal(fail.damageToAttacker,5);
});

test('NPC player projection never leaks GM secrets and exposes only PLAYER_KNOWN memories',()=>{
  const npc={npcId:'n1',name:'Mara',location:'Ruínas',publicFacts:['mercadora'],gmSecrets:['agente rival'],memories:[]};
  const secret=appendNpcMemory(npc,{eventId:'e1',summary:'viu o ritual',visibility:'GM_ONLY'});
  const updated=appendNpcMemory(secret,{eventId:'e2',summary:'foi apresentada ao grupo',visibility:'PLAYER_KNOWN'});
  const player=projectNpcForPlayer(updated); const gm=projectNpcForGm(updated);
  assert.equal('gmSecrets' in player,false);
  assert.deepEqual(player.memories,[{eventId:'e2',summary:'foi apresentada ao grupo',visibility:'PLAYER_KNOWN'}]);
  assert.deepEqual(gm.gmSecrets,['agente rival']); assert.equal(gm.memories.length,2);
});

test('travel resolution deterministic under same seed',()=>{
  const a=new DeterministicRng('world-seed'); const b=new DeterministicRng('world-seed');
  const args={terrain:'PLAINS',hours:10,dayEncounterDie:6,nightEncounterDie:8,people:1};
  assert.deepEqual(resolveTravelDay({rng:a,...args}),resolveTravelDay({rng:b,...args}));
});

test('foraging difficulty table applies full-day -2 and repeated-hex +1',async()=>{
  const { forageDifficulty } = await import('../app/src/main/assets/www/core/wwn-travel-core.js');
  assert.equal(forageDifficulty({terrain:'WOODLANDS_OR_HEAVY_VEGETATION'}).difficulty,8);
  assert.equal(forageDifficulty({terrain:'MOUNTAINS_SCRUBLANDS_OR_SAVANNAS',fullDay:true,successiveDaysInSameHex:2}).difficulty,9);
});

test('successful foraging rolls units, caps at 10, and makes exactly one encounter check',async()=>{
  const { resolveForagingAttempt } = await import('../app/src/main/assets/www/core/wwn-travel-core.js');
  const fixed={results:[6,6,2,6],i:0,die(sides,stream){const result=this.results[this.i++];return{algorithm:'fixed',stream,counterBefore:this.i-1,rawHex:'0',sides,result};}};
  const r=resolveForagingAttempt({rng:fixed,wisdomScore:14,bestSurviveLevel:1,groupSurviveLevels:[1,0,null],terrain:'WOODLANDS_OR_HEAVY_VEGETATION',encounterDie:6});
  assert.equal(r.check.success,true);assert.equal(r.encounter.encounter,false);assert.equal(r.unitsFound,6);assert.equal(r.allocationRequired,true);
});
