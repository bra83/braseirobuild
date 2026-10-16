import test from 'node:test';
import assert from 'node:assert/strict';
import { VERIFIED_RULE_CORPUS } from '../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs';
import { startingDutyForPartySize, applyDutyReductions } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { dutyCheck, contributionMilestoneReady, resolveContributionMilestone } from '../app/src/main/assets/vtt/js/rules/duty.mjs';

test('R37: Duty de criação recupera autoridade AoR sem transformar política solo em oficial',()=>{
 assert.deepEqual(startingDutyForPartySize(2),{value:20,official:true});
 assert.deepEqual(startingDutyForPartySize(6),{value:5,official:true});
 assert.equal(startingDutyForPartySize(1).official,false);
 assert.deepEqual(applyDutyReductions(20,{xp10:true,credits2500:true}),{duty:0,xpBonus:10,creditBonus:2500,dutySpent:20});
});

test('R37: RAG distingue regra AoR recuperada de runtime local guardado',()=>{
 const creation=VERIFIED_RULE_CORPUS.find(r=>r.id==='SW-AOR-CHAR-DUTY-002');
 const base=VERIFIED_RULE_CORPUS.find(r=>r.id==='SW-AOR-BASE-DUTY-CONTRIBUTION-001');
 const runtime=VERIFIED_RULE_CORPUS.find(r=>r.id==='VTT-AOR-DUTY-RUNTIME-001');
 assert.equal(creation?.official,true); assert.equal(base?.official,true); assert.equal(runtime?.official,false);
 assert.match(String(creation?.source),/Age of Rebellion/i);
});

test('R37: runtime Duty testado permanece separado da autoridade editorial',()=>{
 const c={name:'Nira',derived:{wounds:{threshold:12}}};
 const g={dutyEntries:[{id:'d',value:50,member:'Nira',type:'Apoio'}],contributionRank:0};
 const r=dutyCheck(g,c,{roll:22}); assert.equal(r.triggered,true); assert.equal(r.doubles,true); assert.equal(r.characterWoundThresholdBonus,4);
 assert.equal(contributionMilestoneReady(g,c),false);
 const g2={...g,dutyEntries:[{...g.dutyEntries[0],value:100}]};
 assert.equal(contributionMilestoneReady(g2,c),true);
 const done=resolveContributionMilestone(g2); assert.equal(done.contributionRank,1); assert.equal(done.group.dutyEntries[0].value,0);
});
