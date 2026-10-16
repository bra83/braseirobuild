/** SWE09 The Jewel of Yavin, printed pp.90–91: Arend's four holdings.
 * Each security force and Arend are separate encounters. Physical access is
 * GM-confirmed; a security group is never silently teleported into his room.
 */
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, pp.90–91, Sweet Revenge / Shen holdings';
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
export const AREND_PROPERTY_SECURITY=Object.freeze({
 'plaza-residence':Object.freeze({label:'Residência de Arend — segurança da entrada e sala',guards:Object.freeze([
  {profileKey:'k4_security_droid',count:1,name:'Droide K4 — guarda 1'},
  {profileKey:'k4_security_droid',count:1,name:'Droide K4 — guarda 2'},
  {profileKey:'k4_security_droid',count:1,name:'Droide K4 — guarda 3'},
  {profileKey:'shens_goons',count:6,name:'Capangas da sala — seis indivíduos'}])}),
 'figg-hall-office':Object.freeze({label:'Escritório de Arend — segurança de Grayson',guards:Object.freeze([
  {profileKey:'grayson_private_security_guard',count:4,name:'Quatro seguranças particulares de Grayson'}])}),
 'port-town-building':Object.freeze({label:'Port Town — capangas da passarela',guards:Object.freeze([
  {profileKey:'shens_goons',count:5,name:'Capangas da passarela — grupo 1'},
  {profileKey:'shens_goons',count:5,name:'Capangas da passarela — grupo 2'}])}),
 'plaza-yacht':Object.freeze({label:'Iate Venture — tripulação de segurança',guards:Object.freeze([
  {profileKey:'yacht_crew',count:3,name:'Tripulação — grupo 1'},
  {profileKey:'yacht_crew',count:3,name:'Tripulação — grupo 2'},
  {profileKey:'yacht_crew',count:3,name:'Tripulação — grupo 3'}])})
});
const STAGES=['security','interior','arend'];
const receiptKeys=['id','kind','version','source','holdingId','stage','encounterId','range','gmPhysicalPresenceConfirmed','gmDistinctPersonnelConfirmed','gmAccessConfirmed','gmSecurityBypassedConfirmed'];
const encounterId=(holdingId,stage)=>`arend-property-${holdingId}-${stage}`;
function receipts(p){return Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind==='JEWEL_AREND_PROPERTY_ENCOUNTER');}
export function replayJewelArendProperty(p){
 const found=p?.operations?.arendRevenge?.foundAt,seen=new Set(),stages=[];
 for(const [id,r] of receipts(p)){
  if(p?.id!=='jewel-of-yavin'||!AREND_PROPERTY_SECURITY[found]||r.holdingId!==found||
   !validId(id)||r.id!==id||r.kind!=='JEWEL_AREND_PROPERTY_ENCOUNTER'||r.version!==1||r.source!==SOURCE||
   Object.keys(r).some(k=>!receiptKeys.includes(k))||!STAGES.includes(r.stage)||(r.stage==='interior'&&found!=='plaza-residence')||seen.has(r.stage)||
   r.encounterId!==encounterId(found,r.stage)||!['Engajado','Curto','Médio','Longo','Extremo'].includes(r.range)||
   r.gmPhysicalPresenceConfirmed!==true||r.gmDistinctPersonnelConfirmed!==true||r.gmAccessConfirmed!==true||
   typeof r.gmSecurityBypassedConfirmed!=='boolean'||(r.stage==='security'&&r.gmSecurityBypassedConfirmed))
   throw Error('Recibo do imóvel de Arend adulterado ou sem presença real.');
  if(r.stage==='interior'||r.stage==='arend'){
   const needed=r.stage==='interior'?['security']:found==='plaza-residence'?['security','interior']:['security'];
   const allResolved=needed.every(stage=>{
    const prior=stages.find(x=>x.stage===stage);
    return prior&&p.encounterHistory?.some(x=>x.encounterId===prior.encounterId&&
      ['adversaries_defeated','escaped','negotiated'].includes(x.outcome));
   });
   if(!allResolved&&!r.gmSecurityBypassedConfirmed)throw Error('Acesso sem segurança resolvida ou evasão confirmada.');
  }
  seen.add(r.stage);stages.push(clone(r));
 }
 if(stages.length>(found==='plaza-residence'?3:2)||stages.some((x,i)=>i>0&&STAGES.indexOf(x.stage)<STAGES.indexOf(stages[i-1].stage)))throw Error('Ordem dos encontros do imóvel adulterada.');
 for(const r of stages){
  const outcomes=(p.encounterHistory??[]).filter(x=>x.encounterId===r.encounterId);
  if(outcomes.length>1)throw Error('Encontro do imóvel duplicado no histórico.');
 }
 return stages.length?{holdingId:found,stages}:null;
}
export function validateJewelArendProperty(p){
 const expected=replayJewelArendProperty(p);
 if(!equal(expected,p?.operations?.arendProperty??null))throw Error('Estado do imóvel de Arend diverge dos recibos.');
 return expected;
}
export function prepareJewelArendProperty(world,combat,details={}){
 const w=clone(world),p=w?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
    !p.operations?.escapeBomb||!p.encounterHistory?.some(x=>x.encounterId==='arend-shen-four-goons'&&x.outcome))
  throw Error('Confronto de Arend exige a investigação oficial após a traição.');
 if(combat?.active||p.activeEncounterId)throw Error('Encerre o combate anterior antes do imóvel de Arend.');
 const found=p.operations?.arendRevenge?.foundAt;
 if(!AREND_PROPERTY_SECURITY[found]||details.holdingId!==found)throw Error('Arend não foi localizado neste imóvel.');
 if(p.operations?.shenAftermath?.arend)throw Error('Destino de Arend já registrado; não ressuscitar adversário.');
 if(!validId(details.actionId)||p.operations.receipts?.[details.actionId])throw Error('Identificador do confronto inválido ou repetido.');
 if(!STAGES.includes(details.stage)||(details.stage==='interior'&&found!=='plaza-residence')||details.gmPhysicalPresenceConfirmed!==true||
    details.gmDistinctPersonnelConfirmed!==true||details.gmAccessConfirmed!==true||
    !['Engajado','Curto','Médio','Longo','Extremo'].includes(details.range))
  throw Error('Mestre deve confirmar presença física, acesso e distância real.');
 const previous=validateJewelArendProperty(p),stages=previous?.stages??[];
 if(stages.some(x=>x.stage===details.stage))throw Error('Este encontro já foi preparado e não pode duplicar NPCs.');
 if(stages.length&&STAGES.indexOf(details.stage)<STAGES.indexOf(stages.at(-1).stage))
  throw Error('Etapa anterior não pode reaparecer depois de entrar no imóvel.');
 if(details.stage==='interior'||details.stage==='arend'){
  const needed=details.stage==='interior'?['security']:found==='plaza-residence'?['security','interior']:['security'];
  const resolved=needed.every(stage=>{
   const prior=stages.find(x=>x.stage===stage);
   return prior&&p.encounterHistory?.some(x=>x.encounterId===prior.encounterId&&
    ['adversaries_defeated','escaped','negotiated'].includes(x.outcome));
  });
  if(!resolved&&details.gmSecurityBypassedConfirmed!==true)
   throw Error('Resolva ou evite fisicamente os guardas antes de avançar.');
 }
 const r={id:details.actionId,kind:'JEWEL_AREND_PROPERTY_ENCOUNTER',version:1,source:SOURCE,
  holdingId:found,stage:details.stage,encounterId:encounterId(found,details.stage),range:details.range,
  gmPhysicalPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmAccessConfirmed:true,
  gmSecurityBypassedConfirmed:details.stage!=='security'&&details.gmSecurityBypassedConfirmed===true};
 p.operations.receipts[r.id]=clone(r);p.operations.arendProperty=replayJewelArendProperty(p);
 validateJewelArendProperty(p);
 return {world:w,adjudication:r,encounterId:r.encounterId};
}
export function jewelArendPropertyEncounter(p,id){
 const state=validateJewelArendProperty(p),r=state?.stages.find(x=>x.encounterId===id);
 if(!r||p.encounterHistory?.some(x=>x.encounterId===id&&x.outcome))return null;
 const profile=AREND_PROPERTY_SECURITY[r.holdingId];
 const chosen=r.stage==='security'&&r.holdingId==='plaza-residence'?profile.guards.slice(0,3):
  r.stage==='interior'?profile.guards.slice(3):profile.guards;
 const adversaries=r.stage==='arend'?[{profileKey:'arend_shen',count:1,range:r.range,name:'Arend Shen — adversário único'}]:
  chosen.map(x=>({...x,range:r.range}));
 return {id,label:r.stage==='security'?profile.label:r.stage==='interior'?'Residência de Arend — seis capangas na sala':`Arend Shen — ${profile.label}`,
  available:true,status:p.activeEncounterId===id?'active':'ready',playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',adversaries};
}
