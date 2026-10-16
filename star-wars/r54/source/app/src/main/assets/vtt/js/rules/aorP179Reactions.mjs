/** AoR Core pp. 150, 158: It’s Not That Bad, Time to Go (Improved).
 * Reactive effects are consumed only by validated, persisted attack receipts.
 * No free damage, free Destiny or GM-unconfirmed spatial assumptions. */
import {weaponQualityRank} from './weapons.mjs';
const safeId=v=>typeof v==='string'&&/^[A-Za-z0-9:_-]{1,120}$/.test(v);
export function npcBlastReceipt({attackId,attacker,weapon,check,hit,sessionId,round,originalTargetCompanionId=null}){
  const rank=weaponQualityRank(weapon,'blast');
  const cost=hit?2:3;
  if(!rank||Number(check?.netAdvantage??0)<cost)return null;
  if(!safeId(attackId)||!safeId(attacker?.id)||!safeId(sessionId)||!Number.isSafeInteger(round)||round<1)
    throw Error('Recibo de explosão NPC inválido.');
  return {attackId,sourceAdversaryId:attacker.id,weaponName:String(weapon.name),
    blastDamage:rank,advantageSpent:cost,hit:Boolean(hit),round,sessionId,
    originalTargetCompanionId:originalTargetCompanionId??null};
}
export function assertBlastReceipt(combat,receipt,state){
  if(!combat?.active||!receipt||!safeId(receipt.attackId)||!safeId(receipt.sourceAdversaryId)||
     !safeId(receipt.sessionId)||receipt.sessionId!==state.sessionId||
     !Number.isSafeInteger(receipt.round)||receipt.round<1||receipt.round>combat.round||
     !Number.isSafeInteger(receipt.blastDamage)||receipt.blastDamage<1||receipt.blastDamage>100||
     ![2,3].includes(receipt.advantageSpent)||receipt.advantageSpent!==(receipt.hit?2:3)||
     typeof receipt.hit!=='boolean'||
     typeof receipt.weaponName!=='string'||receipt.weaponName.length<1||receipt.weaponName.length>120||
     !(combat.adversaries??[]).some(a=>a.id===receipt.sourceAdversaryId&&
       (a.weapons??[]).some(w=>w.name===receipt.weaponName&&weaponQualityRank(w,'blast')===receipt.blastDamage))||
     (receipt.originalTargetCompanionId!==null&&
       !(state.group?.companions??[]).some(c=>c.id===receipt.originalTargetCompanionId))||
     !(combat.adversaries??[]).some(a=>a.id===receipt.sourceAdversaryId)||
     !(combat.log??[]).some(e=>e.type==='AOR_NPC_BLAST_READY'&&e.attackId===receipt.attackId&&
       e.sourceAdversaryId===receipt.sourceAdversaryId&&e.sessionId===receipt.sessionId&&
       e.blastDamage===receipt.blastDamage&&e.round===receipt.round&&e.hit===receipt.hit&&
       e.originalTargetCompanionId===receipt.originalTargetCompanionId))
    throw Error('Explosão AoR sem recibo de ataque NPC válido.');
}
export function validateP179CombatState(state){
  const combat=state.combat??{};
  if(combat.aorCriticalAid!==undefined){
    const a=combat.aorCriticalAid;
    if(!combat.active||!a||!safeId(a.companionId)||!safeId(a.sessionId)||a.sessionId!==state.sessionId||
       !(state.group?.companions??[]).some(c=>c.id===a.companionId)||
       !(combat.log??[]).some(e=>e.type==='AOR_CRITICAL_AID_ARMED'&&e.companionId===a.companionId&&e.sessionId===a.sessionId))
       throw Error('Preparação de medicina reativa AoR inválida.');
  }
  const receipts=combat.pendingNpcBlasts??[];
  if(!Array.isArray(receipts)||receipts.length>16||new Set(receipts.map(r=>r.attackId)).size!==receipts.length)
    throw Error('Recibos de Explosão AoR inválidos ou duplicados.');
  for(const r of receipts)assertBlastReceipt(combat,r,state);
  const escapes=combat.aorBlastEscapes??[];
  if(!Array.isArray(escapes)||escapes.length>100||escapes.some(e=>
      !safeId(e.attackId)||!safeId(e.sessionId)||e.sessionId!==state.sessionId||
      !Number.isSafeInteger(e.round)||e.round<1||e.round>combat.round||
      (e.actorId!=='player'&&!(state.group?.companions??[]).some(c=>c.id===e.actorId))||
      !(combat.log??[]).some(log=>log.type==='AOR_NPC_BLAST_RESOLVED'&&log.attackId===e.attackId&&
        log.timeToGo===true&&log.sessionId===e.sessionId&&log.round===e.round&&
        (e.actorId==='player'||log.improvedCompanionId===e.actorId))))
    throw Error('Deslocamento reativo AoR sem recibo de Explosão resolvida.');
}
