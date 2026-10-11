/** U09: production receipt projections, all 43 official IDs, and post-choice effects.
 * These are REAL TS method tests, not browser/IndexedDB E2E. */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import ts from './load-typescript.cjs';
import {OfficialEncounterReceiptService as Receipt} from '../src/services/OfficialEncounterReceiptService.ts';
import {OfficialEncounterRulesEngine as Rules} from '../src/services/OfficialEncounterRulesEngine.ts';
let checks=0;
const eq=(actual,expected,message)=>{assert.deepEqual(actual,expected,message);checks++};
const ok=(actual,message)=>{assert.ok(actual,message);checks++};
const e=(id,choiceId='',stage='pending_external_mechanics',more={},status='open',visit=1)=>({
 id:`event-${id}`,type:'encounter',status,title:`Official ${id}`,detail:'PUBLIC',
 gmDetail:'GM_SECRET_SENTINEL',createdAt:1,day:1,period:'Morning',
 result:{mechanic:{kind:'official_random_encounter',encounterId:id},officialEncounterVisitNumber:visit,
  ...(choiceId?{officialSceneDecision:{choiceId,stage,...more}}:{})}});
const get=(...args)=>{const receipt=Receipt.fromEvent(e(...args));ok(receipt,'receipt exists');
 ok(!JSON.stringify(receipt).includes('GM_SECRET_SENTINEL'),'GM secret not projected');
 eq(typeof receipt.followUpPending,'boolean');eq(typeof receipt.resourceTransferCommitted,'boolean');return receipt;};
for(let id=1;id<=43;id++){
 const receipt=get(id);eq(receipt.encounterId,id);eq(receipt.committed,false);
 eq(receipt.followUpPending,false);eq(receipt.resourceTransferCommitted,false);
}
let choices=0;
for(let id=1;id<=43;id++){
 if([9,13,15].includes(id))continue;
 for(const visit of id===18?[1,2,3]:[1])for(const choice of Rules.specialSceneChoices(id,visit)){
  choices++;
  const receipt=get(id,choice.id,choice.closes?'resolved':'pending_external_mechanics',{},choice.closes?'resolved':'open',visit);
  eq(receipt.choiceId,choice.id);eq(receipt.committed,true);eq(receipt.visit,visit);
  ok(receipt.label.includes(choice.closes?'concluída':'pendentes'));
  eq(receipt.resourceTransferCommitted,false);
 }
}
eq(choices,121);
// #12: actual food transfer must be committed before the UI claims delivery.
let r=get(12,'offer_camp');eq(r.resourceTransferCommitted,false);eq(r.followUpPending,false);
ok(r.label.includes('não confirmadas'));
r=get(12,'offer_camp','bandit_offer_pending_reaction',{banditFoodGift:{quantity:2,characterId:'hero'}});
eq(r.resourceTransferCommitted,true);eq(r.followUpPending,true);ok(r.label.includes('2 unidade(s)'));
r=get(12,'offer_camp','bandit_parley_resolved',{banditFoodGift:{quantity:2},banditReaction:'decline'},'resolved');
eq(r.resourceTransferCommitted,true);eq(r.followUpPending,false);ok(r.label.includes('recusado'));
for(const quantity of [0,-1,31,'GM_SECRET_SENTINEL',NaN]){
 r=get(12,'offer_camp','bandit_offer_pending_reaction',{banditFoodGift:{quantity}});
 eq(r.resourceTransferCommitted,false);ok(!r.label.includes('GM_SECRET_SENTINEL'));
}
// #27: event can be resolved while Linn's subsequent daily care is NOT finished.
r=get(27,'comfort','linn_following_party',{linnResponse:'accompany',linnFollowerActive:true},'resolved');
eq(r.status,'resolved');eq(r.followUpPending,true);eq(r.resourceTransferCommitted,false);
r=get(27,'comfort','linn_following_party',{linnResponse:'accompany',linnFollowerActive:true,linnFedDays:[1,2],linnFeedingDaysRequired:4},'resolved');
eq(r.followUpPending,true);eq(r.resourceTransferCommitted,true);ok(r.label.includes('2/4'));
r=get(27,'comfort','linn_following_party',{linnResponse:'accompany',linnFollowerActive:true,linnFedDays:[1,2,3,4],linnFeedingDaysRequired:4,linnDisclosedAt:100},'resolved');
eq(r.followUpPending,false);ok(r.label.includes('contou seu relato'));
// #35: GM accepting gift is not the same as the physical transfer.
r=get(35,'offer_gift','karg_offer_accepted',{kargGiftTransferRequired:true,kargOfferAccepted:true});
eq(r.followUpPending,true);eq(r.resourceTransferCommitted,false);ok(r.label.includes('pendente'));
r=get(35,'offer_gift','karg_gift_transferred',{kargGiftTransferRequired:false,kargGift:{transferredAt:100}},'resolved');
eq(r.followUpPending,false);eq(r.resourceTransferCommitted,true);ok(r.label.includes('transferido'));
r=get(35,'offer_horse','karg_sacrifice_pending',{kargOfferAccepted:true});
eq(r.followUpPending,true);eq(r.resourceTransferCommitted,false);
// U25: imported or partial sacrifice objects never close Karg's follow-up.
r=get(35,'offer_horse','karg_sacrifice_adjudicated',{kargSacrifice:{kind:'horse',targetId:'mount'}},'resolved');
eq(r.followUpPending,true);eq(r.resourceTransferCommitted,false);ok(r.label.includes('incompleto'));
// A fully adjudicated sacrifice is a committed transfer, with exact matching
// outcome, victim identity, GM evidence and atomic resolution timestamp.
r=get(35,'offer_horse','karg_sacrifice_adjudicated',{
  kargOfferAccepted:true,pending:null,adjudicationMode:'official_karg_sacrifice',resolvedAt:100,
  kargSacrifice:{kind:'horse',targetId:'mount',targetName:'Montaria da companhia',
    evidence:'A Mestra confirmou a morte da montaria.',adjudicatedAt:100}
},'resolved');
eq(r.followUpPending,false);eq(r.resourceTransferCommitted,true);ok(r.label.includes('Sacrifício adjudicado'));
// #41: barter and released soul are persisted; resurrection never assumed.
r=get(41,'board','soul_merchant_stock_set',{soulBottleStock:2});
eq(r.resourceTransferCommitted,false);eq(r.followUpPending,false);ok(r.label.includes('2 garrafa(s)'));
r=get(41,'board','soul_merchant_stock_set',{soulBottleStock:1,soulBottleBarters:[{characterId:'hero'}]});
eq(r.resourceTransferCommitted,true);ok(r.label.includes('1 troca(s)'));
r=get(41,'board','soul_merchant_departed',{soulBottleStock:0,soulBottleBarters:[{}],releasedSoulTokens:[{id:'soul'}],soulResurrectionD6Pending:true},'resolved');
eq(r.resourceTransferCommitted,true);eq(r.followUpPending,true);ok(r.label.includes('Ressurreição ainda pendente'));
for(const text of ['gmNotes','gmDetail','GM_SECRET_SENTINEL'])ok(!r.label.includes(text),'no GM secrets');
// The UI must surface persisted state rather than claim success from a click.
const panel=fs.readFileSync(new URL('../src/components/JourneyPanel.tsx',import.meta.url),'utf8');
const source=fs.readFileSync(new URL('../src/services/OfficialEncounterReceiptService.ts',import.meta.url),'utf8');
const browser=fs.readFileSync(new URL('../e2e/official_receipt_progress_ui.py',import.meta.url),'utf8');
for(const [name,code,kind] of [['receipt',source,ts.ScriptKind.TS],['JourneyPanel',panel,ts.ScriptKind.TSX]]){
 const ast=ts.createSourceFile(name,code,ts.ScriptTarget.Latest,true,kind);
 eq(ast.parseDiagnostics.length,0);
}
for(const token of ['data-follow-up-pending=','data-resource-transfer-committed=','OfficialEncounterReceiptService.fromEvent(event)'])ok(panel.includes(token));
for(const token of ['await page.reload','await mount(page,case[\'campaignId\'])','to_have_attribute','get_by_test_id'])ok(browser.includes(token));
console.log(`U09_OFFICIAL_RECEIPT_PROGRESS PASS ${checks}/${checks} (43 IDs, 121 choices, 4 post-choice chains); browser NOT executed`);
