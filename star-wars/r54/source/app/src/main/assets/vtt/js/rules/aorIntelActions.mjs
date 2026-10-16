/** P176 — AoR Core Known Schematic / Bought Info. No invented map/intelligence.
 * Source: AoR Core p.143 and Known Schematic (FFG). The Master must confirm
 * access to purchasable information and reveal actual layout facts.
 */
export const safeIntelId=v=>typeof v==='string'&&/^[A-Za-z0-9:_-]{1,120}$/.test(v);
export const KNOWLEDGE_SKILLS=Object.freeze(['bajos_fondos','borde_exterior','educacion','estrategia_militar','mundos_del_nucleo','saber_arcaico','xenologia']);
export function validateAoRIntelLedger(state){
 const list=state.master?.aorIntelActions??[];
 if(!Array.isArray(list)||list.length>128)throw Error('Histórico AoR de informações inválido.');
 const ids=new Set(),uses=new Set();
 for(const r of list){
  if(!r||!safeIntelId(r.actionId)||ids.has(r.actionId)||!['schematic','bought'].includes(r.kind)||
    !safeIntelId(r.subjectId)||!safeIntelId(r.sessionId)||r.campaignId!==state.campaignId||
    !safeIntelId(r.campaignId)||!(typeof r.characterName==='string'&&r.characterName.trim().length>0&&r.characterName.length<=160)||r.approved!==true||
    !Number.isSafeInteger(r.difficulty)||r.difficulty<1||r.difficulty>5||
    !Number.isSafeInteger(r.creditsSpent)||r.creditsSpent<0||
    !Number.isSafeInteger(r.netSuccess)||r.netSuccess<0||
    typeof r.success!=='boolean'||
    (r.kind==='schematic'&&(r.difficulty!==3||r.creditsSpent!==0||r.skillKey!=='educacion'||r.success!==(r.netSuccess>0)))||
    (r.kind==='bought'&&(!KNOWLEDGE_SKILLS.includes(r.skillKey)||r.creditsSpent!==r.difficulty*50||r.netSuccess!==1||!r.success)))
    throw Error('Recibo AoR de informação adulterado.');
  ids.add(r.actionId);
  if(r.kind==='schematic'){
    const useKey=r.sessionId+'\0'+r.characterName;
    if(uses.has(useKey))throw Error('Projeto conhecido usado mais de uma vez por sessão.');
    uses.add(useKey);
  }
  const matching=(state.events??[]).filter(e=>e.type==='AOR_INTEL_ACTION'&&e.payload?.receipt?.actionId===r.actionId);
  if(matching.length!==1||JSON.stringify(matching[0].payload.receipt)!==JSON.stringify(r))
    throw Error('Ação AoR de informação sem recibo causal.');
 }
 return true;
}
