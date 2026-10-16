/** SWE09 pp.78, 92–94. GM-confirmed pursuit groups; no invented arrival, count or named leader.
 * The Wing Guard are the default first pursuers. Arend/Grayson never appear automatically.
 */
const clone=structuredClone;
const SOURCE='SWE09 The Jewel of Yavin, pp.78,92–94, Flight over Fight';
const IDS=Object.freeze({arend_goons:'holo-park-arend-goons',grayson_security:'holo-park-grayson-security'});
const PROFILES=Object.freeze({arend_goons:'shens_goons',grayson_security:'grayson_private_security_guard'});
const MAX=Object.freeze({arend_goons:4,grayson_security:6});
const validId=x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
export const JEWEL_HOLO_FACTION_SPECS=Object.freeze({
 arend_goons:Object.freeze({encounterId:IDS.arend_goons,profileKey:PROFILES.arend_goons,maxCount:MAX.arend_goons,label:'Capangas de Arend — presença confirmada'}),
 grayson_security:Object.freeze({encounterId:IDS.grayson_security,profileKey:PROFILES.grayson_security,maxCount:MAX.grayson_security,label:'Segurança privada de Grayson — presença confirmada'})
});
function receipts(progress){return Object.entries(progress?.operations?.receipts??{}).filter(([,r])=>r?.kind==='JEWEL_HOLO_FACTION_ARRIVAL');}
export function replayJewelHoloFactions(progress){
 const found={};
 for(const [id,r] of receipts(progress)){
  if(progress?.id!=='jewel-of-yavin'||!progress.operations?.holoPark||
     !progress.encounterHistory?.some(x=>x.encounterId==='holo-park-wing-guard'&&x.outcome))
   throw Error('Facção sem perseguição e Guarda Alada resolvidas.');
  const spec=JEWEL_HOLO_FACTION_SPECS[r.faction];
  if(!spec||!validId(id)||id!==r.id||r.version!==1||r.source!==SOURCE||
    Object.keys(r).some(k=>!['id','kind','version','source','faction','encounterId','profileKey','count','range','gmParkPresenceConfirmed','gmDistinctPersonnelConfirmed','gmWingGuardNotEngagedConfirmed','wingEncounterResolved'].includes(k))||
    r.encounterId!==spec.encounterId||r.profileKey!==spec.profileKey||
    !Number.isSafeInteger(r.count)||r.count<1||r.count>spec.maxCount||
    !['Engajado','Curto','Médio','Longo','Extremo'].includes(r.range)||
    r.gmParkPresenceConfirmed!==true||r.wingEncounterResolved!==true||
    r.gmDistinctPersonnelConfirmed!==true||r.gmWingGuardNotEngagedConfirmed!==true||found[r.faction])throw Error('Recibo da facção Holo-Park adulterado.');
  found[r.faction]=clone(r);
 }
 return found;
}
export function validateJewelHoloFactions(progress){
 const expected=replayJewelHoloFactions(progress);
 if(JSON.stringify(expected)!==JSON.stringify(progress?.operations?.holoFactions??{}))
  throw Error('Presença de facções Holo-Park diverge dos recibos.');
 return expected;
}
export function prepareJewelHoloFaction(world,details={}){
 const w=clone(world),p=w?.publishedAdventure,spec=JEWEL_HOLO_FACTION_SPECS[details.faction];
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!spec)
  throw Error('Facção fora da fuga publicada de Cloud City.');
 if(!p.operations?.holoPark||p.operations.holoPark.status!=='ACTIVE'||!p.encounterHistory?.some(x=>x.encounterId==='holo-park-wing-guard'&&x.outcome))
  throw Error('A Guarda Alada precisa ter sido resolvida antes de outro encontro.');
 if(p.activeEncounterId)throw Error('Outro encontro ainda está ativo.');
 if(details.gmParkPresenceConfirmed!==true||details.gmDistinctPersonnelConfirmed!==true||details.gmWingGuardNotEngagedConfirmed!==true)
  throw Error('Mestre deve confirmar presença física e indivíduos distintos, sem teleporte ou duplicação.');
 if(!validId(details.actionId)||p.operations.receipts?.[details.actionId])throw Error('Identificador repetido ou inválido.');
 const count=details.count;
 if(!Number.isSafeInteger(count)||count<1||count>spec.maxCount)throw Error('Quantidade não confirmada ou acima do grupo documentado.');
 if(!['Engajado','Curto','Médio','Longo','Extremo'].includes(details.range))throw Error('Distância física não confirmada.');
 const found=validateJewelHoloFactions(p);
 if(found[details.faction]||p.encounterHistory.some(x=>x.encounterId===spec.encounterId))throw Error('Facção já utilizada neste Holo-Park.');
 const r={id:details.actionId,kind:'JEWEL_HOLO_FACTION_ARRIVAL',version:1,source:SOURCE,faction:details.faction,
  encounterId:spec.encounterId,profileKey:spec.profileKey,count,range:details.range,
  gmParkPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmWingGuardNotEngagedConfirmed:true,wingEncounterResolved:true};
 p.operations.receipts[r.id]=clone(r);p.operations.holoFactions=replayJewelHoloFactions(p);
 validateJewelHoloFactions(p);
 return {world:w,adjudication:r,encounterId:spec.encounterId};
}
export function jewelHoloFactionEncounter(progress,encounterId){
 const arrivals=validateJewelHoloFactions(progress);
 for(const [faction,r] of Object.entries(arrivals)){
  if(r.encounterId!==encounterId)continue;
  const spec=JEWEL_HOLO_FACTION_SPECS[faction];
  if(progress.encounterHistory?.some(x=>x.encounterId===encounterId&&x.outcome))return null;
  return {id:encounterId,label:spec.label,available:true,status:progress.activeEncounterId===encounterId?'active':'ready',
   playerInitiativeSkill:'alerta',npcInitiativeSkill:'alerta',
   adversaries:[{profileKey:r.profileKey,count:r.count,range:r.range,name:spec.label}]};
 }
 return null;
}
