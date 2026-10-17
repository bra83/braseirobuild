// Reconciliação não destrutiva Codex + WIP28: SWE02, pp.9–10 e 30.
// O teste de Conhecimento/causa original permanece no executor Krayt Fang P238.
// Este módulo só registra a POSSE FÍSICA e a identificação feita em cena real.
export const SIVOR_SHELL_ID = 'SWE02:KRAYT_FANG:HOLD3:MARKED_CHITIN';
const ADVENTURE_ID = 'long-arm-of-the-hutt';
const SOURCE = Object.freeze({productCode:'SWE02',printedPage:9,authority:'OFFICIAL_PDF_TEXT_VERIFIED'});

function evidence(p,choiceId,key) {
 return (p?.geonosisEvidence||[]).some(x=>x.choiceId===choiceId&&x.key===key);
}
export function getSWE02SivorCustodyStatus(p) {
 if(p?.id!==ADVENTURE_ID)return {status:'NOT_ACTIVE',physical:false,identified:false};
 const c=p.sivorShellCustody,i=p.sivorShellIdentification;
 const physical=c?.assetId===SIVOR_SHELL_ID&&c?.holder==='PARTY'&&c?.physical===true;
 const identified=Boolean(physical&&i?.assetId===SIVOR_SHELL_ID&&i?.witness==='PIDDOCK');
 const legacy=Boolean((p.flags?.geo_shell&&!physical)||(p.flags?.geo_identifiedShell&&!identified));
 return {status:legacy?'LEGACY_UNCERTIFIED':identified?'IDENTIFIED':physical?'HELD_UNIDENTIFIED':'NOT_COLLECTED',
  physical:Boolean(physical),identified,identifiedBy:identified?'PIDDOCK':null,assetId:physical?SIVOR_SHELL_ID:null};
}
export function recordSWE02SivorPhysicalCollection(p) {
 if(p?.id!==ADVENTURE_ID||p.currentNodeId!=='krayt-fang-logs'||p.sivorShellCustody)
  throw Error('SWE02: carapaça fora do porão três ou já recolhida.');
 if(p.flags?.shipMarkedChitinKept!==true||!p.shipChitinKnowledge||!evidence(p,'sivor-shell','shell'))
  throw Error('SWE02: é preciso examinar, conservar e recolher a carapaça real.');
 const checked=p.shipChitinKnowledge.checkReceipt;
 // A falta de um recibo de perícia em um save antigo não fabrica uma rolagem.
 const knowledgeVerified=Boolean(checked&&checked.checkIndex===9&&checked.success===true);
 p.sivorShellCustody={
  type:'SIVOR_MARKED_CHITIN_COLLECTED',assetId:SIVOR_SHELL_ID,
  origin:'KRAYT_FANG_HOLD3',holder:'PARTY',physical:true,
  collectedAtNode:'krayt-fang-logs',source:{...SOURCE},
  knowledgeReceiptVerified:knowledgeVerified,transfers:0
 };
 return p.sivorShellCustody;
}
export function recordSWE02SivorPiddockIdentification(p) {
 if(p?.id!==ADVENTURE_ID||p.currentNodeId!=='piddock-party'||p.activeEncounterId)
  throw Error('SWE02: Piddock só examina a carapaça durante a recepção, fora de combate.');
 if(p.sivorShellIdentification||p.sivorShellCustody?.assetId!==SIVOR_SHELL_ID||
    p.sivorShellCustody?.holder!=='PARTY'||!evidence(p,'piddock-identify-shell','identifiedShell'))
  throw Error('SWE02: identificação exige apresentação física única da carapaça.');
 p.sivorShellIdentification={
  type:'SIVOR_SHELL_POSITIVE_IDENTIFICATION',assetId:SIVOR_SHELL_ID,
  witness:'PIDDOCK',physicalItemShown:true,conclusion:'BELONGED_TO_SIVOR',
  identifiedAtNode:'piddock-party',
  source:{productCode:'SWE02',printedPage:30,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
 };
 return p.sivorShellIdentification;
}
export function validateSWE02SivorPhysicalProof(p) {
 if(p?.id!==ADVENTURE_ID)return true;
 const c=p.sivorShellCustody,i=p.sivorShellIdentification;
 if(c&&(c.type!=='SIVOR_MARKED_CHITIN_COLLECTED'||c.assetId!==SIVOR_SHELL_ID||
   c.origin!=='KRAYT_FANG_HOLD3'||c.holder!=='PARTY'||c.physical!==true||
   c.collectedAtNode!=='krayt-fang-logs'||c.source?.productCode!=='SWE02'||
   c.source?.printedPage!==9||c.source?.authority!=='OFFICIAL_PDF_TEXT_VERIFIED'||
   typeof c.knowledgeReceiptVerified!=='boolean'||c.transfers!==0||
   !p.flags?.shipMarkedChitinKept||!p.shipChitinKnowledge||!evidence(p,'sivor-shell','shell')))
  throw Error('SWE02: custódia física de Sivor sem origem verificável.');
 if(i&&(!c||i.type!=='SIVOR_SHELL_POSITIVE_IDENTIFICATION'||
   i.assetId!==SIVOR_SHELL_ID||i.witness!=='PIDDOCK'||
   i.identifiedAtNode!=='piddock-party'||i.physicalItemShown!==true||
   i.conclusion!=='BELONGED_TO_SIVOR'||i.source?.productCode!=='SWE02'||
   i.source?.printedPage!==30||i.source?.authority!=='OFFICIAL_PDF_TEXT_VERIFIED'||
   !evidence(p,'piddock-identify-shell','identifiedShell')))
  throw Error('SWE02: identificação de Sivor sem apresentação física verificável.');
 // Leitura de saves antigos preservada. A validação não os promove.
 return true;
}
export function requireSWE02SivorProofForDimmock(p) {
 if(!p?.sivorShellCustody||!p?.sivorShellIdentification) {
  throw Error('SWE02: save legado não certifica a identificação física de Sivor para Dimmock.');
 }
 validateSWE02SivorPhysicalProof(p);
 return true;
}
