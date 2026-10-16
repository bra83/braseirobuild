import test from 'node:test';
import assert from 'node:assert/strict';
import {startUnderBlackSun,advanceUnderBlackSun,adjudicateUnderBlackSunCheck,recordUnderBlackSunEncounterOutcome} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
const step=(w,id)=>advanceUnderBlackSun(w,id).world;
function toSpyder(){let w=startUnderBlackSun({clock:{tick:20},locations:{},npcs:{},factions:{},livingDepth:{npcs:{},factions:{}}}).world;for(const id of ['escape-clean','begin-investigation','go-spyder'])w=step(w,id);return w}
function finish(w){for(const id of ['spyder-return','go-zelcomm','zelcomm-weapons','move-endgame','plan-direct','through-refinery','enter-abandoned','face-kaato'])w=step(w,id);w=recordUnderBlackSunEncounterOutcome(w,{encounterId:'kaato-showdown',outcome:'adversaries_defeated',adversaries:[{profileKey:'kaato_leeachos',status:'incapacitated'}]}).world;return step(w,'kaato-captured')}

test('P90: dívida do Spyder e saque da Zelcomm acumulam Obligation em vez de sobrescrever',()=>{
 let w=toSpyder();
 w=adjudicateUnderBlackSunCheck(w,{checkIndex:3,result:{success:false,netSuccess:-1,netAdvantage:-2,despair:1}}).world;
 assert.equal(w.publishedAdventure.flags.obligationPendingDelta,5);
 assert.equal(w.publishedAdventure.flags.obligationPendingSources.spyder,5);
 w=step(w,'spyder-return'); w=step(w,'go-zelcomm'); w=step(w,'zelcomm-weapons');
 assert.equal(w.publishedAdventure.flags.obligationPendingDelta,10);
 assert.deepEqual(w.publishedAdventure.flags.obligationPendingSources,{spyder:5,zelcomm:5});
});

test('P90: save/reopen preserva total e origem de Obligation acumulada',()=>{
 let w=toSpyder();
 w=adjudicateUnderBlackSunCheck(w,{checkIndex:3,result:{success:false,netSuccess:-1,netAdvantage:-2,despair:1}}).world;
 w=JSON.parse(JSON.stringify(w));
 w=step(w,'spyder-return'); w=step(w,'go-zelcomm'); w=step(w,'zelcomm-weapons');
 w=JSON.parse(JSON.stringify(w));
 assert.equal(w.publishedAdventure.flags.obligationPendingDelta,10);
 assert.equal(w.publishedAdventure.flags.obligationPendingSources.spyder,5);
 assert.equal(w.publishedAdventure.flags.obligationPendingSources.zelcomm,5);
});

test('P90: save legado sem ledger migra saldo anterior para legacy antes de somar nova origem',()=>{
 let w=toSpyder();
 w.publishedAdventure.flags.obligationPendingDelta=7;
 delete w.publishedAdventure.flags.obligationPendingSources;
 w=step(w,'spyder-return'); w=step(w,'go-zelcomm'); w=step(w,'zelcomm-weapons');
 assert.equal(w.publishedAdventure.flags.obligationPendingDelta,12);
 assert.deepEqual(w.publishedAdventure.flags.obligationPendingSources,{legacy:7,zelcomm:5});
});

test('P90: epílogo persiste total e ledger de Obligation',()=>{
 let w=toSpyder();
 w=adjudicateUnderBlackSunCheck(w,{checkIndex:3,result:{success:false,netSuccess:-1,netAdvantage:-2,despair:1}}).world;
 w=finish(w);
 const c=w.publishedAdventureConsequences['under-black-sun'];
 assert.equal(c.obligationPendingDelta,10);
 assert.deepEqual(c.obligationPendingSources,{spyder:5,zelcomm:5});
});
