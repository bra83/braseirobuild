/** P174 — AoR Mechanic, source-gated equipment actions.
 * REDUNDANT SYSTEMS: Easy Mechanics, once per session; a functional identical
 * device can supply compatible components without being consumed or damaged.
 * BAD MOTIVATOR: Hard Mechanics, once per session; GM-approved target device
 * is marked failed only after a successful check. Only registered owned
 * equipment is currently targetable; unmodeled enemy devices remain gated.
 * Source: Age of Rebellion Core pp. 142,154; EotE Core pp.132,141.
 */
const TYPES = Object.freeze({redundant:'REDUNDANT_SYS',motivator:'BAD_MOTIVATOR'});
const safeId=v=>typeof v==='string'&&/^[a-zA-Z0-9:_-]{1,120}$/.test(v);
const validName=v=>typeof v==='string'&&v.trim().length>0&&v.length<=160;
const validKind=new Set(['gear','weapon','armor','cybernetic','attachment']);
export function validateMechanicFieldLedger(state){
 const ledger=state.master?.mechanicFieldActions??[];
 if(!Array.isArray(ledger)||ledger.length>128)throw Error('Histórico de ações mecânicas inválido.');
 const ids=new Set(),uses=new Set();
 for(const r of ledger){
  if(!r||!safeId(r.actionId)||ids.has(r.actionId)||!Object.hasOwn(TYPES,r.kind)||
     !safeId(r.targetId)||!validName(r.characterName)||!safeId(r.sessionId)||!safeId(r.campaignId)||
     r.campaignId!==state.campaignId||!Number.isFinite(r.netSuccess)||typeof r.success!=='boolean'||
     typeof r.approved!=='boolean'||typeof r.applied!=='boolean'||
     (r.applied&&(!r.success||!r.approved))||
     (r.kind==='redundant'&&!safeId(r.donorId)))
    throw Error('Recibo de ação mecânica adulterado.');
  ids.add(r.actionId);
  const key=`${r.characterName}\u0000${r.sessionId}\u0000${r.kind}`;
  if(uses.has(key))throw Error('Talento mecânico usado mais de uma vez na sessão.');
  uses.add(key);
  const events=(state.events??[]).filter(e=>e.type==='AOR_MECHANIC_ACTION'&&e.payload?.receipt?.actionId===r.actionId);
  if(events.length!==1||JSON.stringify(events[0].payload.receipt)!==JSON.stringify(r))
   throw Error('Ação mecânica sem recibo causal correspondente.');
 }
 return true;
}
export function mechanicFieldPreflight(state,{kind,targetId,donorId,actionId,approved}){
 if(!Object.hasOwn(TYPES,kind)||!safeId(actionId)||!safeId(targetId)||typeof approved!=='boolean')
  throw Error('Ação mecânica inválida.');
 if(!['AOR','MIXED'].includes(state.campaignMode)||state.session?.status!=='ativa')
  throw Error('Ação mecânica exige sessão AoR ativa.');
 const character=state.player?.character;
 if(!character?.name||character.creation?.rulesProfile!=='AOR_CORE')throw Error('Personagem AoR ausente.');
 const ledger=state.master?.mechanicFieldActions??[];
 const existing=ledger.find(r=>r.actionId===actionId);
 if(existing){
  if(existing.kind!==kind||existing.targetId!==targetId||existing.donorId!==(kind==='redundant'?donorId:null)||existing.approved!==approved)
   throw Error('Ação mecânica repetida com parâmetros divergentes.');
  return {duplicate:existing};
 }
 if(ledger.some(r=>r.sessionId===state.sessionId&&r.characterName===character.name&&r.kind===kind))
  throw Error('Talento mecânico já usado nesta sessão.');
 if(ledger.length>=128)throw Error('Histórico de ações mecânicas cheio; exporte a campanha.');
 const assets=state.group?.ownedAssets??[];
 const target=assets.find(a=>a.assetId===targetId);
 if(!target||!validKind.has(target.kind))throw Error('Dispositivo alvo deve constar do inventário mecânico.');
 if(kind==='redundant'){
  if(!safeId(donorId)||donorId===targetId)throw Error('Dispositivo doador inválido.');
  const donor=assets.find(a=>a.assetId===donorId);
  // Conservative equivalence: matching catalog entries are guaranteed to be
  // the same size/tech level; cross-model substitutions await source metadata.
  if(!donor||donor.catalogId!==target.catalogId||donor.kind!==target.kind||
      donor.state?.condition!=='operational'||Number(donor.state?.damage??0)!==0||
      target.state?.condition!=='damaged'||!(Number(target.state?.damage)>0))
   throw Error('Reparo redundante exige dispositivo idêntico operacional e alvo danificado.');
 }else if(target.state?.condition!=='operational')
  throw Error('Peça defeituosa exige dispositivo operacional.');
 return {target};
}
export function mechanicFieldReceipt(state,{kind,targetId,donorId=null,actionId,approved,check,applied}){
 if(!check||typeof check.success!=='boolean'||!Number.isFinite(check.netSuccess))
  throw Error('Teste de Mecânica inválido.');
 return {actionId,kind,targetId,donorId:kind==='redundant'?donorId:null,
  campaignId:state.campaignId,sessionId:state.sessionId,
  characterName:state.player.character.name,success:check.success,
  netSuccess:check.netSuccess,approved,applied,at:new Date().toISOString()};
}

/** Shared equipment gate: a mechanically failed registered device cannot be
 * equipped, prepared, reloaded or used to attack merely because its old
 * character inventory entry still exists. Unregistered starter gear is not
 * affected by this catalog-specific condition check. */
export function assertOwnedEquipmentOperational(state,item){
 const id=item?.ownedAssetId;
 if(!id)return true;
 const owned=(state.group?.ownedAssets??[]).find(a=>a.assetId===id);
 if(owned&&owned.state?.condition!=='operational')
  throw Error('Dispositivo avariado ou defeituoso: repare o ativo antes de usá-lo.');
 return true;
}
