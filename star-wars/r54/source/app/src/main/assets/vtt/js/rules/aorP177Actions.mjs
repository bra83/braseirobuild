/** AoR Core (pp. 144, 148, 150, 157): transactional, source-gated talents.
 * Facts, devices and NPC intentions are approved by the GM; the engine
 * commits only validated mechanical consequences, never generated lore.
 */
export const safeP177Id = x => typeof x === 'string' && /^[a-zA-Z0-9:_-]{1,120}$/.test(x);
export const P177_KINDS = Object.freeze(['contraption','careful_planning','incite_rebellion','hidden_storage','greased_palms']);
const limited = (x,max=300) => typeof x === 'string' && x.trim().length > 0 && x.length <= max && !/[<>\x00-\x08\x0b\x0c\x0e-\x1f]/.test(x);
export function validateP177Ledger(state) {
 const entries=state.master?.aorP177Actions??[];
 if(!Array.isArray(entries)||entries.length>256)throw Error('Histórico de talentos AoR P177 inválido.');
 const ids=new Set(),once=new Set();
 for(const r of entries){
  if(!r||!safeP177Id(r.actionId)||ids.has(r.actionId)||!P177_KINDS.includes(r.kind)||
    r.campaignId!==state.campaignId||!safeP177Id(r.sessionId)||!limited(r.characterName,160)||
    r.approved!==true||!Number.isSafeInteger(r.costCredits)||r.costCredits<0||
    typeof r.success!=='boolean'||!safeP177Id(r.subjectId))throw Error('Recibo de talento AoR P177 inválido.');
  ids.add(r.actionId);
  if(['contraption','careful_planning','incite_rebellion'].includes(r.kind)){
    const k=`${r.sessionId}:${r.characterName}:${r.kind}`;
    if(once.has(k))throw Error('Talento AoR P177 usado duas vezes na sessão.');once.add(k);
  }
  if(r.kind==='greased_palms'&&(!Number.isSafeInteger(r.upgrades)||r.upgrades<1||r.upgrades>5||r.costCredits!==r.upgrades*50||!['carisma','coaccion','engano','liderazgo','negociacion'].includes(r.skillKey)))throw Error('Suborno AoR adulterado.');
  if(r.kind==='hidden_storage'&&(!safeP177Id(r.containerId)||!safeP177Id(r.itemId)||!Number.isSafeInteger(r.encumbrance)||r.encumbrance<1||r.encumbrance>10||r.costCredits!==0))throw Error('Compartimento AoR adulterado.');
  if(r.kind==='contraption'&&(!Number.isSafeInteger(r.difficulty)||r.difficulty!==3||!safeP177Id(r.toolsId)||!safeP177Id(r.partsId)||r.deviceItemId!==null&&!safeP177Id(r.deviceItemId)||r.costCredits!==0))throw Error('Artilugio AoR adulterado.');
  if(r.kind==='careful_planning'&&(!limited(r.fact,300)||r.costCredits!==0))throw Error('Planejamento AoR adulterado.');
  if(r.kind==='incite_rebellion'&&(!Number.isSafeInteger(r.difficulty)||r.difficulty!==3||!Array.isArray(r.targetIds)||r.targetIds.length>5||r.targetIds.some(id=>!safeP177Id(id))||r.costCredits!==0))throw Error('Rebelião AoR adulterada.');
  const matches=(state.events??[]).filter(e=>e.type==='AOR_P177_ACTION'&&e.payload?.receipt?.actionId===r.actionId);
  if(matches.length!==1||JSON.stringify(matches[0].payload.receipt)!==JSON.stringify(r))throw Error('Recibo AoR P177 sem evento causal.');
 }
 const events=(state.events??[]).filter(e=>e.type==='AOR_P177_ACTION');
 if(events.length!==entries.length)throw Error('Eventos AoR P177 órfãos.');
 return true;
}
