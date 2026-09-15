import assert from 'node:assert/strict';
import crypto from 'node:crypto';
import { DeterministicRng } from '../app/src/main/assets/www/core/rng-core.js';
import { resolveSkillCheck } from '../app/src/main/assets/www/core/wwn-rules-core.js';
import { resolveTravelDay } from '../app/src/main/assets/www/core/wwn-travel-core.js';
import { resolveDungeonTurns } from '../app/src/main/assets/www/core/wwn-dungeon-core.js';
import { resolveAttack, resolveSavingThrow } from '../app/src/main/assets/www/core/wwn-combat-core.js';
import { resolveFactionAttributeCheck } from '../app/src/main/assets/www/core/wwn-faction-core.js';
import { appendNpcMemory, projectNpcForPlayer } from '../app/src/main/assets/www/core/npc-registry.js';
import { DeterministicNarrativeProvider } from '../app/src/main/assets/www/core/narrative-provider.js';
import { canonicalJson } from '../app/src/main/assets/www/core/canonical-json.js';

const SESSIONS=33, TURNS_PER_SESSION=144, TOTAL=SESSIONS*TURNS_PER_SESSION;
const kinds=['ACTION','TRAVEL','DUNGEON','COMBAT','FACTION','NPC','REOPEN','NARRATIVE'];

function runAll(){
  const counts=Object.fromEntries(kinds.map(k=>[k,0]));
  const sessionDigests=[];
  for(let s=0;s<SESSIONS;s+=1){
    const rng=new DeterministicRng(`wave10-session-${s}`);
    let npc={npcId:`npc-${s}`,name:`NPC ${s}`,location:'HEX-1',publicFacts:['visto na estrada'],gmSecrets:['agente rival'],memories:[]};
    let state={campaignId:`stress-${s}`,revision:0,worldState:{minute:0},encounterState:{},characterState:{name:'Fixture',hp:8},npcState:{[npc.npcId]:npc},privateState:{gmOnly:'NUNCA VAZAR'}};
    const narrative=new DeterministicNarrativeProvider();
    const trace=[];
    for(let t=0;t<TURNS_PER_SESSION;t+=1){
      const kind=kinds[t%kinds.length]; counts[kind]+=1;
      let result;
      switch(kind){
        case 'ACTION':
          result=resolveSkillCheck({rng,skillLevel:1,attributeScore:14,difficulty:8,situationalModifier:0,streamName:`s${s}/t${t}/action`});
          assert.equal(typeof result.success,'boolean');
          break;
        case 'TRAVEL':
          result=resolveTravelDay({rng,terrain:'PLAINS',hours:10,dayEncounterDie:6,nightEncounterDie:8,supplies:{foodUnits:50,waterUnits:50},people:1,streamPrefix:`s${s}/t${t}/travel`});
          assert.ok(result.plan);
          break;
        case 'DUNGEON':
          result=resolveDungeonTurns({rng,currentTurn:t,turns:1,defenseState:'UNALERTED_ORGANIZED',streamPrefix:`s${s}/t${t}/dungeon`,lightSource:{type:'TORCH',remainingTurns:6}});
          assert.equal(result.turns,1);
          break;
        case 'COMBAT': {
          const attack=resolveAttack({rng,attacker:{baseAttackBonus:1,attributes:{Strength:14,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:10,Charisma:10},skills:{Stab:0}},weapon:{combatSkill:'Stab',attribute:'Strength',damage:'1d6',shock:{damage:2,ac:15},melee:true},target:{meleeAC:13,rangedAC:13,shockImmune:false},streamName:`s${s}/t${t}/attack`});
          const save=resolveSavingThrow({rng,saveType:'PHYSICAL',target:15,streamName:`s${s}/t${t}/save`});
          assert.ok(attack.damage>=0); assert.equal(typeof save.success,'boolean'); result={attack,save};
          break;
        }
        case 'FACTION':
          result=resolveFactionAttributeCheck({rng,attackerAttribute:3,defenderAttribute:3,streamName:`s${s}/t${t}/faction`});
          assert.equal(result.attackerWins,!result.defenderWins);
          break;
        case 'NPC':
          npc=appendNpcMemory(npc,{eventId:`s${s}-t${t}`,summary:`fato ${t}`,visibility:(t%16===5?'PLAYER_KNOWN':'GM_ONLY')});
          result=projectNpcForPlayer(npc);
          assert.equal(JSON.stringify(result).includes('agente rival'),false);
          assert.equal(result.memories.every(m=>m.visibility==='PLAYER_KNOWN'),true);
          state.npcState[npc.npcId]=npc;
          break;
        case 'REOPEN': {
          state.revision+=1; state.worldState.minute+=10;
          const saved=canonicalJson(state); const reopened=JSON.parse(saved);
          assert.equal(canonicalJson(reopened),saved); result={revision:reopened.revision,minute:reopened.worldState.minute};
          break;
        }
        case 'NARRATIVE': {
          const event={eventId:`mech:s${s}:t${t}`,revision:state.revision,kind:'STRESS_MECHANICAL',resolution:{success:(t%3)!==0}};
          result=narrative.generate({mechanicalEvent:event,state,ragSnippets:[{visibility:'PLAYER_VISIBLE',text:'regra visível'},{visibility:'GM_ONLY',text:'NUNCA VAZAR'}]});
          assert.equal(result.text.includes('NUNCA VAZAR'),false);
          break;
        }
      }
      trace.push(crypto.createHash('sha256').update(canonicalJson(result)).digest('hex').slice(0,16));
    }
    sessionDigests.push(crypto.createHash('sha256').update(trace.join('|')).digest('hex'));
  }
  assert.equal(Object.values(counts).reduce((a,b)=>a+b,0),TOTAL);
  const digest=crypto.createHash('sha256').update(sessionDigests.join('\n')).digest('hex');
  return {sessions:SESSIONS,turnsPerSession:TURNS_PER_SESSION,totalTurns:TOTAL,counts,sessionDigests,digest};
}

const a=runAll();
const b=runAll();
assert.equal(a.digest,b.digest,'stress replay must be deterministic');
assert.deepEqual(a.counts,b.counts);
console.log('STRESS_33_SESSIONS_PASS');
console.log(`sessions=${a.sessions} turns_per_session=${a.turnsPerSession} total_turns=${a.totalTurns}`);
console.log(`counts=${JSON.stringify(a.counts)}`);
console.log(`deterministic_replay=PASS digest=${a.digest}`);
