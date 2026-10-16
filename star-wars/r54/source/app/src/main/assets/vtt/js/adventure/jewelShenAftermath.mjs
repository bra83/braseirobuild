/** SWE09 The Jewel of Yavin, printed pp.90–91 and 95 (Sweet Revenge;
 * Extended Campaigns; Episode III rewards). The adventure explicitly
 * distinguishes dead, imprisoned and free Arend, and an Aris alliance.
 * These are GM-confirmed consequences of play, NOT automatic combat/XP.
 */
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, pp.90-91,95, Sweet Revenge / Extended Campaigns';
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const AR=['DEAD','INCARCERATED','AT_LARGE'];
const AR_KEYS=['id','kind','version','source','status','holdingId','gmArendIdentified','gmPhysicalConfrontationConfirmed','gmOutcomeConfirmed','gmDeathConfirmed','gmCustodyConfirmed','gmEscapeConfirmed'];
const AS_KEYS=['id','kind','version','source','gmArisMetInPerson','gmArisAgreedToAlliance','gmPartyAcceptedAlliance','gmArisNoLongerHostile','allied'];
const holdings=['plaza-residence','figg-hall-office','port-town-building','plaza-yacht'];
function receipts(p,kind){return Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind===kind);}
function checkArend(p){
 const rr=receipts(p,'JEWEL_AREND_OUTCOME');if(rr.length>1)throw Error('Desfecho de Arend duplicado.');
 if(!rr.length)return null;
 const [id,r]=rr[0],located=p.operations?.arendRevenge?.foundAt;
 if(p.id!=='jewel-of-yavin'||!validId(id)||r.id!==id||r.kind!=='JEWEL_AREND_OUTCOME'||r.version!==1||r.source!==SOURCE||
  Object.keys(r).some(k=>!AR_KEYS.includes(k))||!AR.includes(r.status)||!holdings.includes(r.holdingId)||r.holdingId!==located||
  r.gmArendIdentified!==true||r.gmPhysicalConfrontationConfirmed!==true||r.gmOutcomeConfirmed!==true||
  r.gmDeathConfirmed!==(r.status==='DEAD')||r.gmCustodyConfirmed!==(r.status==='INCARCERATED')||r.gmEscapeConfirmed!==(r.status==='AT_LARGE'))
   throw Error('Desfecho de Arend sem localização real ou com recibo adulterado.');
 return clone(r);
}
function checkAris(p){
 const rr=receipts(p,'JEWEL_ARIS_ALLIANCE');if(rr.length>1)throw Error('Aliança de Aris duplicada.');
 if(!rr.length)return null;
 const [id,r]=rr[0];
 if(p.id!=='jewel-of-yavin'||!validId(id)||r.id!==id||r.kind!=='JEWEL_ARIS_ALLIANCE'||r.version!==1||r.source!==SOURCE||
  Object.keys(r).some(k=>!AS_KEYS.includes(k))||r.gmArisMetInPerson!==true||r.gmArisAgreedToAlliance!==true||
  r.gmPartyAcceptedAlliance!==true||r.gmArisNoLongerHostile!==true||r.allied!==true)
   throw Error('Aliança com Aris sem confirmação de ambas as partes ou recibo adulterado.');
 return clone(r);
}
export function replayJewelShenAftermath(p){
 const arend=checkArend(p),aris=checkAris(p);
 return arend||aris?{arend,aris}:null;
}
export function validateJewelShenAftermath(p){
 const expected=replayJewelShenAftermath(p),stored=p?.operations?.shenAftermath??null;
 if(!equal(expected,stored))throw Error('Estado Arend/Aris diverge dos recibos persistidos.');
 return expected;
}
function preflight(world,combat,details){
 const p=world?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||!['ACTIVE','COMPLETED'].includes(p.status)||!p.operations?.escapeBomb)
  throw Error('Desfecho de Arend/Aris exige a traição em The Jewel of Yavin.');
 if(combat?.active||p.activeEncounterId)throw Error('Encerre o combate/encontro antes de registrar o desfecho.');
 if(!p.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome))
  throw Error('Resolva o encontro anterior dos capangas antes de registrar o desfecho.');
 if(!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('Identificador de desfecho inválido ou repetido.');
 validateJewelShenAftermath(p);
 return p;
}
export function recordJewelArendOutcome(world,combat,details={}){
 const w=clone(world),p=preflight(w,combat,details);
 if(checkArend(p))throw Error('Desfecho de Arend já registrado.');
 const holdingId=p.operations?.arendRevenge?.foundAt;
 if(!holdings.includes(holdingId))throw Error('Arend precisa ter sido localizado por pesquisa e visita reais.');
 if(!AR.includes(details.status)||details.gmArendIdentified!==true||details.gmPhysicalConfrontationConfirmed!==true||details.gmOutcomeConfirmed!==true)
  throw Error('Mestre deve confirmar identidade, confronto físico e resultado real.');
 const proof={gmDeathConfirmed:details.status==='DEAD',gmCustodyConfirmed:details.status==='INCARCERATED',gmEscapeConfirmed:details.status==='AT_LARGE'};
 if(Object.entries(proof).some(([k,v])=>details[k]!==v))throw Error('Confirmação específica do destino de Arend ausente ou contraditória.');
 const receipt={id:details.actionId,kind:'JEWEL_AREND_OUTCOME',version:1,source:SOURCE,status:details.status,holdingId,
  gmArendIdentified:true,gmPhysicalConfrontationConfirmed:true,gmOutcomeConfirmed:true,...proof};
 p.operations.receipts[receipt.id]=clone(receipt);
 p.operations.shenAftermath=replayJewelShenAftermath(p);validateJewelShenAftermath(p);
 // No fabricated bounties, imprisonment releases, credits, XP or travel.
 return {world:w,adjudication:receipt,aftermath:clone(p.operations.shenAftermath)};
}
export function recordJewelArisAlliance(world,combat,details={}){
 const w=clone(world),p=preflight(w,combat,details);
 if(checkAris(p))throw Error('Aliança com Aris já registrada.');
 for(const k of ['gmArisMetInPerson','gmArisAgreedToAlliance','gmPartyAcceptedAlliance','gmArisNoLongerHostile'])
  if(details[k]!==true)throw Error('Aliança exige encontro, consentimento mútuo e ausência de hostilidade confirmados.');
 const receipt={id:details.actionId,kind:'JEWEL_ARIS_ALLIANCE',version:1,source:SOURCE,
  gmArisMetInPerson:true,gmArisAgreedToAlliance:true,gmPartyAcceptedAlliance:true,gmArisNoLongerHostile:true,allied:true};
 p.operations.receipts[receipt.id]=clone(receipt);
 p.operations.shenAftermath=replayJewelShenAftermath(p);validateJewelShenAftermath(p);
 // SWE09 lists 5 XP for alliance; reward adjudication remains separate.
 return {world:w,adjudication:receipt,aftermath:clone(p.operations.shenAftermath)};
}
