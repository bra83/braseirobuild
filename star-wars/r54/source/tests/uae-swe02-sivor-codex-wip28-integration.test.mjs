import test from 'node:test';
import assert from 'node:assert/strict';
import {recordGeonosisChoice,validateGeonosis} from '../app/src/main/assets/vtt/js/adventure/swe02GeonosisP237.mjs';
import {getSWE02SivorCustodyStatus,SIVOR_SHELL_ID,requireSWE02SivorProofForDimmock} from '../app/src/main/assets/vtt/js/adventure/swe02SivorCustodyIntegration.mjs';

const fixture=()=>({
 id:'long-arm-of-the-hutt',currentNodeId:'krayt-fang-logs',status:'ACTIVE',
 flags:{shipMarkedChitinKept:true},shipChitinKnowledge:{
  source:{productCode:'SWE02',printedPage:9},
  checkReceipt:{checkIndex:9,success:true,skillKey:'xenologia'}
 },geonosisEvidence:[],geonosisArguments:[],checkHistory:[]
});

test('SWE02 Codex Knowledge evidence + WIP28 physical shell receipt feed one Piddock proof',()=>{
 const p=fixture();
 recordGeonosisChoice(p,'sivor-shell','krayt-fang-logs');
 assert.equal(getSWE02SivorCustodyStatus(p).status,'HELD_UNIDENTIFIED');
 assert.equal(p.sivorShellCustody.assetId,SIVOR_SHELL_ID);
 assert.equal(p.sivorShellCustody.knowledgeReceiptVerified,true);
 p.currentNodeId='piddock-party';
 recordGeonosisChoice(p,'piddock-identify-shell','piddock-party');
 assert.equal(getSWE02SivorCustodyStatus(p).status,'IDENTIFIED');
 assert.equal(p.geonosisEvidence.filter(x=>x.key==='identifiedShell').length,1);
 p.currentNodeId='dimmock-official-audience';
 recordGeonosisChoice(p,'dimmock-shell','dimmock-official-audience');
 assert.equal(p.geonosisArguments.filter(x=>x.key==='shell').length,1);
 assert.equal(validateGeonosis(p),true);
 const persisted=JSON.parse(JSON.stringify(p));
 for(let reopen=0;reopen<3;reopen++){
  assert.equal(validateGeonosis(persisted),true);
  assert.equal(getSWE02SivorCustodyStatus(persisted).identified,true);
 }
});

test('SWE02 legacy shell claims are readable, but cannot certify Dimmock without physical receipt',()=>{
 const p=fixture();
 p.geonosisEvidence=[
  {choiceId:'sivor-shell',key:'shell',nodeId:'krayt-fang-logs',page:10},
  {choiceId:'piddock-identify-shell',key:'identifiedShell',nodeId:'piddock-party',page:30}
 ];
 Object.assign(p.flags,{geo_shell:true,geo_identifiedShell:true});
 p.currentNodeId='dimmock-official-audience';
 assert.equal(validateGeonosis(p),true);
 assert.equal(getSWE02SivorCustodyStatus(p).status,'LEGACY_UNCERTIFIED');
 assert.throws(()=>recordGeonosisChoice(p,'dimmock-shell','dimmock-official-audience'),/legado|física/i);
});

test('SWE02 tampered custody and invented witness receipt fail validation',()=>{
 const base=fixture();
 recordGeonosisChoice(base,'sivor-shell','krayt-fang-logs');
 base.currentNodeId='piddock-party';
 recordGeonosisChoice(base,'piddock-identify-shell','piddock-party');
 for(const mutate of [
  p=>{p.sivorShellCustody.holder='NO_ONE';},
  p=>{p.sivorShellCustody.transfers=10;},
  p=>{p.sivorShellIdentification.assetId='FAKE';},
  p=>{p.sivorShellIdentification.physicalItemShown=false;}
 ]){
  const p=structuredClone(base);mutate(p);
  assert.throws(()=>validateGeonosis(p),/SWE02/);
 }
});

test('SWE02 preexisting Codex result without graded Knowledge receipt is not retroactively certified',()=>{
 const p=fixture();delete p.shipChitinKnowledge.checkReceipt;
 recordGeonosisChoice(p,'sivor-shell','krayt-fang-logs');
 assert.equal(p.sivorShellCustody.knowledgeReceiptVerified,false);
 const loaded=JSON.parse(JSON.stringify(p));
 assert.equal(validateGeonosis(loaded),true);
 assert.equal(getSWE02SivorCustodyStatus(loaded).status,'HELD_UNIDENTIFIED');
 assert.throws(()=>requireSWE02SivorProofForDimmock(loaded),/legado/i);
});
