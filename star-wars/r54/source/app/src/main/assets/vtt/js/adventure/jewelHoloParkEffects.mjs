/** SWE09 The Jewel of Yavin, printed pp.93–94: one source-backed result
 * for each active landscape. Symbols are spent from an ACTUAL pending player
 * attack; the GM must confirm the narrative trigger.
 * Additional source-backed results use actual combat dice pools; unsupported results remain manual.
 */
import {validateJewelHoloPark} from './jewelHoloPark.mjs';
import {buildPositivePool,buildOpposedNegativePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
import {adversaryCharacteristicForSkill,adversarySkillRank} from '../rules/adversaries.mjs';
import {sufferStrain} from '../rules/health.mjs';
import {spendManeuver} from '../rules/combat.mjs';
import {applyStatusEffect} from '../rules/statusEffects.mjs';
import {updateAdversaryInCombat} from '../rules/encounter.mjs';
import {adversaryHealthStatus,sufferAdversaryStrain} from '../rules/adversaries.mjs';
const clone=structuredClone, eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const SOURCE='SWE09 The Jewel of Yavin pp.93–94, Holo-Park landscape symbols';
const RULES=Object.freeze({
 alderaan_thranta:Object.freeze({landscape:'alderaan',symbol:'advantage',cost:1,kind:'player-ranged-boost'}),
 christophsis_shadow:Object.freeze({landscape:'christophsis',symbol:'advantage',cost:1,kind:'npc-shadow-cover'}),
 coruscant_building:Object.freeze({landscape:'coruscant',symbol:'triumph',cost:1,kind:'player-building-cover'}),
 alderaan_nerfs:Object.freeze({landscape:'alderaan',symbol:'triumph',cost:1,kind:'player-nerf-stealth'}),
 alderaan_upheaval:Object.freeze({landscape:'alderaan',symbol:'despair',cost:1,kind:'player-strain-and-maneuver'}),
 christophsis_shatter:Object.freeze({landscape:'christophsis',symbol:'triumph',cost:1,kind:'npc-staggered'}),
 coruscant_speeder:Object.freeze({landscape:'coruscant',symbol:'advantage',cost:1,kind:'npc-disoriented'}),
 kashyyyk_wroshyr:Object.freeze({landscape:'kashyyyk',symbol:'advantage',cost:1,kind:'player-ranged-cover'}),
 christophsis_spotlight:Object.freeze({landscape:'christophsis',symbol:'threat',cost:1,kind:'npc-spotlight'}),
 coruscant_transport:Object.freeze({landscape:'coruscant',symbol:'threat',cost:1,kind:'npc-transport'}),
 alderaan_lost_weapon:Object.freeze({landscape:'alderaan',symbol:'threat',cost:1,kind:'npc-drop-weapon'}),
 kashyyyk_lost_weapon:Object.freeze({landscape:'kashyyyk',symbol:'threat',cost:1,kind:'npc-drop-weapon'}),
 christophsis_fountain:Object.freeze({landscape:'christophsis',symbol:'despair',cost:1,kind:'npc-fountain-cover'}),
 coruscant_vanish:Object.freeze({landscape:'coruscant',symbol:'despair',cost:1,kind:'npc-opposed-stealth'}),
 kashyyyk_vertigo:Object.freeze({landscape:'kashyyyk',symbol:'despair',cost:1,kind:'npc-vertigo'}),
 kashyyyk_insect:Object.freeze({landscape:'kashyyyk',symbol:'triumph',cost:1,kind:'player-same-roll-boost'})
});
export const JEWEL_HOLO_EFFECT_RULES=RULES;
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const FIELDS=['id','kind','version','source','effect','landscape','round','gmSymbolConfirmed','gmBoardedThrantaConfirmed','gmHerdConfirmed','attackId','targetId','symbol','cost','result','npcCheck','gmCompanionsPresentConfirmed','affectedCompanionIds','gmWeaponHeldConfirmed','gmTargetPresentConfirmed'];
/** Replay a recorded NPC roll, never accept symbols supplied by the GM. */
function verifyNpcCheck(check,required='threat'){
 if(!check||typeof check!=='object'||!eq(Object.keys(check).sort(),['pool','faces','netAdvantage','despair'].sort())||
   !check.pool||!Array.isArray(check.faces)||!Number.isSafeInteger(check.netAdvantage)||!Number.isSafeInteger(check.despair))
   throw Error('Prova dos dados NPC ausente ou adulterada.');
 const pool=check.pool,types=['ability','proficiency','difficulty','challenge','boost','setback'];
 if(Object.keys(pool).some(k=>!types.includes(k))||types.some(t=>!Number.isSafeInteger(pool[t]??0)||(pool[t]??0)<0||(pool[t]??0)>12))
   throw Error('Pool NPC adulterado.');
 if(check.faces.length!==types.reduce((n,t)=>n+(pool[t]??0),0))throw Error('Número de faces NPC adulterado.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let cursor=0;
 for(const type of types)for(let i=0;i<(pool[type]??0);i++){
  const f=check.faces[cursor++],face=diceFacesForTests[type]?.[f?.index];
  if(!face||f.type!==type||!eq(f.face,face))throw Error('Face de dado NPC adulterada.');
  for(const [symbol,count] of Object.entries(face))raw[symbol]+=count;
 }
 const out=resolveSymbols(raw);
 if(out.netAdvantage!==check.netAdvantage||out.despair!==check.despair||
    (required==='threat'&&check.netAdvantage>=0)||(required==='despair'&&check.despair<1))
  throw Error('Símbolos de Ameaça NPC adulterados.');
 return true;
}
/** Reserve exactly ONE actual NPC Threat for GM landscape adjudication.
 * The baseline NPC-strain fallback consumes only the unreserved symbols. */
export function reserveJewelHoloNpcThreat(combat,progress,attackerId,check,attackId){
 const park=progress?.operations?.holoPark;
 if(progress?.id!=='jewel-of-yavin'||park?.status!=='ACTIVE'||
   !['christophsis','coruscant','alderaan','kashyyyk'].includes(park.landscape)||!combat?.active||
   combat.round!==park.started.combatRound+park.rounds.length||
   !Number.isSafeInteger(check?.netAdvantage)||check.netAdvantage>=0)return {combat,reserved:0};
 if(!validId(attackerId)||!validId(attackId))throw Error('Ataque NPC sem identidade válida.');
 const evidence={attackId,attackerId,round:combat.round,landscape:park.landscape,
  check:{pool:clone(check.pool),faces:clone(check.faces),netAdvantage:check.netAdvantage,despair:check.despair}};
 verifyNpcCheck(evidence.check);
 const c=clone(combat);c.holoParkPendingNpcThreat??=[];
 if(c.holoParkPendingNpcThreat.length>=24)throw Error('Fila de ameaças NPC do Holo-Park cheia.');
 c.holoParkPendingNpcThreat.push(evidence);
 return {combat:c,reserved:1};
}
/** Reserve one REAL Despair separately from Threat, never silently consume both. */
export function reserveJewelHoloNpcDespair(combat,progress,attackerId,check,attackId){
 const park=progress?.operations?.holoPark;
 if(progress?.id!=='jewel-of-yavin'||park?.status!=='ACTIVE'||
   !['christophsis','coruscant','kashyyyk'].includes(park.landscape)||!combat?.active||
   combat.round!==park.started.combatRound+park.rounds.length||!(check?.despair>0))
  return {combat,reserved:0};
 if(!validId(attackerId)||!validId(attackId))throw Error('Ataque NPC sem identidade válida.');
 const evidence={attackId,attackerId,round:combat.round,landscape:park.landscape,
  check:{pool:clone(check.pool),faces:clone(check.faces),netAdvantage:check.netAdvantage,despair:check.despair}};
 verifyNpcCheck(evidence.check,'despair');
 const c=clone(combat);c.holoParkPendingNpcDespair??=[];
 if(c.holoParkPendingNpcDespair.length>=24||c.holoParkPendingNpcDespair.some(x=>x.attackId===attackId))
  throw Error('Desespero NPC duplicado ou fila cheia.');
 c.holoParkPendingNpcDespair.push(evidence);
 return {combat:c,reserved:1};
}
/** Reconstruct a skill roll's exact faces; no fabricated successes or NPC stats. */
function verifySkillRoll(check,pool){
 if(!check||!eq(Object.keys(check).sort(),['pool','faces','success','netSuccess','netAdvantage','triumph','despair'].sort())||
    !eq(check.pool,pool)||!Array.isArray(check.faces))throw Error('Prova do teste oposto adulterada.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let cursor=0;
 for(const type of ['ability','proficiency','difficulty','challenge','boost','setback'])for(let i=0;i<(pool[type]??0);i++){
  const f=check.faces[cursor++],face=diceFacesForTests[type]?.[f?.index];
  if(!face||f.type!==type||!eq(f.face,face))throw Error('Faces do teste oposto adulteradas.');
  for(const [k,v] of Object.entries(face))raw[k]+=v;
 }
 if(cursor!==check.faces.length)throw Error('Dados extras no teste oposto.');
 const out=resolveSymbols(raw);
 if(['success','netSuccess','netAdvantage','triumph','despair'].some(k=>out[k]!==check[k]))
  throw Error('Resultado do teste oposto adulterado.');
 return true;
}
/** Fountain's defense is physical cover, not a permanent modification to the NPC sheet. */
export function holoParkFountainDefense(target,weapon){
 return target?.holoParkFountain?.active===true&&
   !['pelea','armas_cuerpo_a_cuerpo'].includes(weapon?.skill)?2:0;
}
/** The NPC is unseen after a successful opposed roll; no teleport is performed. */
export function assertJewelHoloTargetVisible(target){
 if(target?.holoParkHiddenFromPlayer?.active)throw Error('Alvo oculto entre hologramas: Mestre deve confirmar que voltou a ser visto.');
}
export function confirmJewelHoloNpcMovement(combat,{targetId,gmMovementConfirmed=false}={}){
 const c=clone(combat),target=c?.adversaries?.find(x=>x.id===targetId);
 if(!c?.active||!validId(targetId)||!target?.holoParkFountain?.active||gmMovementConfirmed!==true)
  throw Error('Cobertura exige alvo real e movimento confirmado pelo Mestre.');
 const updated={...target,holoParkFountain:{...target.holoParkFountain,active:false}};
 c.adversaries=updateAdversaryInCombat(c,updated).adversaries;
 c.log.push({type:'JEWEL_HOLO_FOUNTAIN_MOVEMENT',round:c.round,targetId,receiptId:target.holoParkFountain.receiptId});
 return c;
}
export function confirmJewelHoloNpcVisible(combat,{targetId,gmVisibleConfirmed=false}={}){
 const c=clone(combat),target=c?.adversaries?.find(x=>x.id===targetId);
 if(!c?.active||!validId(targetId)||!target?.holoParkHiddenFromPlayer?.active||gmVisibleConfirmed!==true)
  throw Error('Visibilidade exige alvo real e linha de visão confirmada pelo Mestre.');
 const updated={...target,holoParkHiddenFromPlayer:{...target.holoParkHiddenFromPlayer,active:false}};
 c.adversaries=updateAdversaryInCombat(c,updated).adversaries;
 c.log.push({type:'JEWEL_HOLO_TARGET_VISIBLE',round:c.round,targetId,receiptId:target.holoParkHiddenFromPlayer.receiptId});
 return c;
}
/** Player is illuminated until the END of their next actual initiative turn. */
export function holoParkSpotlightSetback(combat){return combat?.active&&combat?.holoParkSpotlight?.active===true?1:0;}
export function clearHoloParkSpotlightAtTurnEnd(combat){
 if(!combat?.holoParkSpotlight?.active)return combat;
 return {...combat,holoParkSpotlight:{...combat.holoParkSpotlight,active:false}};
}
/** Transport obstructs actual player combat pools until the player moves. */
export function holoParkTransportSetback(combat,actorId='player'){
 const effect=combat?.holoParkTransport;
 if(!combat?.active||!effect)return 0;
 return actorId==='player'?(effect.playerActive===true?1:0):effect.companionIds?.includes(actorId)?1:0;
}
export function clearHoloParkTransportOnMovement(combat){
 if(!combat?.holoParkTransport?.playerActive)return combat;
 return {...combat,holoParkTransport:{...combat.holoParkTransport,playerActive:false}};
}
export function replayJewelHoloEffects(progress){
 const receipts=progress?.operations?.receipts??{},history=[];
 for(const [id,r] of Object.entries(receipts)){
  if(r?.kind!=='JEWEL_HOLO_EFFECT')continue;
  const rule=RULES[r.effect];
  if(!validId(id)||r.id!==id||r.version!==1||r.source!==SOURCE||r.gmSymbolConfirmed!==true||
    !rule||(rule.kind==='player-ranged-boost'&&r.gmBoardedThrantaConfirmed!==true)||
    (rule.kind!=='player-ranged-boost'&&r.gmBoardedThrantaConfirmed!==undefined)||
    (rule.kind!=='player-nerf-stealth'&&r.gmHerdConfirmed!==undefined)||r.landscape!==rule.landscape||r.symbol!==rule.symbol||r.cost!==rule.cost||
    !Number.isSafeInteger(r.round)||r.round<1||r.round>100000||!validId(r.attackId)||
    (r.targetId!==null&&!validId(r.targetId))||!r.result||typeof r.result!=='object'||
    Object.keys(r).some(k=>!FIELDS.includes(k)))throw Error('Recibo de efeito Holo-Park adulterado.');
  if(history.some(x=>x.attackId===r.attackId&&x.round===r.round))throw Error('Símbolo Holo-Park duplicado para o mesmo ataque.');
  if(history.length&&r.round<history.at(-1).round)throw Error('Rodadas dos efeitos Holo-Park fora de ordem.');
  if(rule.kind==='player-same-roll-boost'){
   const v=r.result;
   if(r.targetId!==null&&!validId(r.targetId))throw Error('Alvo do inseto adulterado.');
   if(!v||!eq(Object.keys(v).sort(),['base','final','extraBoostFaces','boostDice','triumphSpent'].sort())||
      v.boostDice!==2||v.triumphSpent!==1||!Array.isArray(v.extraBoostFaces)||v.extraBoostFaces.length!==2)
    throw Error('Recibo do inseto adulterado.');
   verifySkillRoll(v.base,v.base.pool);
   if(v.base.triumph<1||!Number.isSafeInteger(v.base.pool.boost??0))throw Error('Triunfo do inseto inexistente.');
   const pool={...v.base.pool,boost:Number(v.base.pool.boost??0)+2};
   const firstSetback=v.base.faces.findIndex(x=>x.type==='setback');
   const at=firstSetback<0?v.base.faces.length:firstSetback;
   const faces=[...v.base.faces.slice(0,at),...v.extraBoostFaces,...v.base.faces.slice(at)];
   if(v.extraBoostFaces.some(x=>x.type!=='boost')||!eq(v.final.pool,pool)||!eq(v.final.faces,faces))
    throw Error('Dados de Benefício do inseto adulterados.');
   verifySkillRoll(v.final,pool);
   if(v.final.triumph!==v.base.triumph)throw Error('Triunfo do inseto adulterado.');
  }else if(rule.kind==='npc-spotlight'||rule.kind==='npc-transport'||rule.kind==='npc-drop-weapon'){
   const dropped=rule.kind==='npc-drop-weapon';
   const transport=rule.kind==='npc-transport';
   if(!r.npcCheck||r.targetId!==null||!validId(r.result?.attackerId)||
     !eq(Object.keys(r.result).sort(),(dropped?['attackerId','weaponIndex','weaponName','recover']:transport?['attackerId','setback','duration','companionIds']:['attackerId','setback','duration']).sort())||
     (dropped?(r.gmWeaponHeldConfirmed!==true||!Number.isSafeInteger(r.result.weaponIndex)||r.result.weaponIndex<0||r.result.weaponIndex>100||typeof r.result.weaponName!=='string'||!r.result.weaponName.trim()||r.result.recover!=='next-round-maneuver-or-one-strain'):
       r.result.setback!==1||r.result.duration!==(transport?'until-real-movement':'until-next-player-turn-end'))||
     (transport? r.gmWeaponHeldConfirmed!==undefined||r.gmCompanionsPresentConfirmed!==true||!Array.isArray(r.affectedCompanionIds)||
       !eq(r.affectedCompanionIds,r.result.companionIds)||
       new Set(r.affectedCompanionIds).size!==r.affectedCompanionIds.length||
       r.affectedCompanionIds.some(id=>!validId(id)):
       r.gmCompanionsPresentConfirmed!==undefined||r.affectedCompanionIds!==undefined||(!dropped&&r.gmWeaponHeldConfirmed!==undefined)))
    throw Error('Efeito de Ameaça NPC adulterado.');
   verifyNpcCheck(r.npcCheck);
  }else if(rule.kind==='npc-fountain-cover'||rule.kind==='npc-opposed-stealth'||rule.kind==='npc-vertigo'){
   if(!r.npcCheck||!validId(r.result?.attackerId)||
      (rule.kind==='npc-vertigo'?(r.targetId!==null||r.gmTargetPresentConfirmed!==undefined):(!validId(r.targetId)||r.gmTargetPresentConfirmed!==true)))
      throw Error('Recibo de Desespero NPC adulterado.');
   verifyNpcCheck(r.npcCheck,'despair');
   if(rule.kind==='npc-vertigo'){
    const v=r.result;
    if(!eq(Object.keys(v).sort(),['attackerId','characteristic','skillRank','check','strainApplied','downgradePending'].sort()))throw Error('Vertigem adulterada.');
    const pool={...buildPositivePool(v.characteristic,v.skillRank),difficulty:3};
    verifySkillRoll(v.check,pool);
    if(v.strainApplied!==(v.check.success?0:2)||v.downgradePending!==!v.check.success)throw Error('Consequências da vertigem adulteradas.');
   }else if(rule.kind==='npc-fountain-cover'){
    if(!eq(Object.keys(r.result).sort(),['attackerId','rangedDefense','untilMovement'].sort())||
       r.result.rangedDefense!==2||r.result.untilMovement!==true)throw Error('Cobertura da fonte adulterada.');
   }else{
    const v=r.result;
    if(!eq(Object.keys(v).sort(),['attackerId','characteristic','skillRank','opponentCharacteristic','opponentSkillRank','check','hidden'].sort()))
      throw Error('Recibo de Furtividade oposta adulterado.');
    const positive=buildPositivePool(v.characteristic,v.skillRank),negative=buildOpposedNegativePool(v.opponentCharacteristic,v.opponentSkillRank);
    verifySkillRoll(v.check,{...positive,difficulty:negative.difficulty,challenge:negative.challenge});
    if(v.hidden!==v.check.success)throw Error('Visibilidade adulterada.');
   }
  }else if(r.npcCheck!==undefined||r.gmTargetPresentConfirmed!==undefined)throw Error('Prova NPC indevida no recibo.');
  if(rule.kind==='player-strain-and-maneuver'){
   if(r.targetId!==null||Object.keys(r.result).sort().join(',')!=='maneuversLost,strainApplied'||!Number.isSafeInteger(r.result.strainApplied)||r.result.strainApplied!==2||r.result.maneuversLost!==1)throw Error('Efeito de Alderaan adulterado.');
  }else if(rule.kind==='player-same-roll-boost'){
   /* Same-roll evidence was replayed above, before any combat consequence. */
  }else if(rule.kind==='player-ranged-boost'){
   if(r.targetId!==null||Object.keys(r.result).sort().join(',')!=='fromRound,rangedBoost,throughRound'||r.result.rangedBoost!==1||r.result.fromRound!==r.round+1||r.result.throughRound!==r.round+2)throw Error('Voo de thranta adulterado.');
  }else if(rule.kind==='npc-shadow-cover'){
   if(!validId(r.targetId)||Object.keys(r.result).sort().join(',')!=='rangedDefense,throughRound'||r.result.rangedDefense!==1||r.result.throughRound!==r.round+1)throw Error('Sombra de Christophsis adulterada.');
  }else if(rule.kind==='player-nerf-stealth'){
   const v=r.result;
   if(r.gmHerdConfirmed!==true||r.targetId!==null||
      Object.keys(v).sort().join(',')!=='check,gmEscapePending,maneuverSpent'||
      v.maneuverSpent!==true||v.gmEscapePending!==true||!v.check||
      !eq(Object.keys(v.check).sort(),['characteristic','despair','faces','netAdvantage','netSuccess','pool','skillRank','success','triumph'].sort()))
     throw Error('Teste de Furtividade entre nerfs adulterado.');
   const q=v.check,positive=buildPositivePool(q.characteristic,q.skillRank),pool={...positive,difficulty:2};
   if(!eq(q.pool,pool)||!Array.isArray(q.faces)||q.faces.length!==Object.values(pool).reduce((a,b)=>a+b,0))
     throw Error('Pool da Furtividade entre nerfs adulterado.');
   const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
   let pos=0;
   for(const [type,count] of Object.entries(pool))for(let i=0;i<count;i++){
    const f=q.faces[pos++],actual=diceFacesForTests[type]?.[f?.index];
    if(!actual||f.type!==type||!eq(f.face,actual))throw Error('Faces da Furtividade entre nerfs adulteradas.');
    for(const [symbol,n] of Object.entries(actual))raw[symbol]+=n;
   }
   const out=resolveSymbols(raw);
   if(q.success!==out.success||q.netSuccess!==out.netSuccess||q.netAdvantage!==out.netAdvantage||
      q.triumph!==out.triumph||q.despair!==out.despair)
    throw Error('Símbolos da Furtividade entre nerfs adulterados.');
  }else if(rule.kind==='player-building-cover'){
   if(r.gmHerdConfirmed!==undefined||r.targetId!==null||Object.keys(r.result).sort().join(',')!=='rangedDefense,untilMovement'||r.result.rangedDefense!==2||r.result.untilMovement!==true)throw Error('Cobertura de Coruscant adulterada.');
  }else if(rule.kind==='npc-spotlight'||rule.kind==='npc-transport'||rule.kind==='npc-drop-weapon'||rule.kind==='npc-fountain-cover'||rule.kind==='npc-opposed-stealth'||rule.kind==='npc-vertigo'||rule.kind==='player-same-roll-boost'){
   /* NPC symbol was validated above, with complete dice-face replay. */
  }else if(rule.kind==='player-ranged-cover'){
   if(r.targetId!==null||Object.keys(r.result).sort().join(',')!=='rangedSetback,throughRound'||r.result.rangedSetback!==1||r.result.throughRound!==r.round+1)throw Error('Cobertura de Kashyyyk adulterada.');
  }else if(!validId(r.targetId)||Object.keys(r.result).sort().join(',')!=='condition,rounds'||r.result.condition!==(rule.kind==='npc-staggered'?'staggered':'disoriented')||r.result.rounds!==1)throw Error('Condição Holo-Park adulterada.');
  history.push(clone(r));
 }
 return history.length?{version:1,history}:null;
}
export function validateJewelHoloEffects(progress){
 const expected=replayJewelHoloEffects(progress);
 if(!eq(expected,progress?.operations?.holoEffects??null))throw Error('Estado dos efeitos Holo-Park diverge dos recibos.');
 return expected;
}
export function applyJewelHoloEffect(world,combat,character,details={},group=null){
 const w=clone(world),c=clone(combat),p=w?.publishedAdventure,rule=RULES[details.effect];
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||!c.active||!character||!rule)
  throw Error('Efeito exige aventura, personagem e combate reais ativos.');
 const park=validateJewelHoloPark(p);validateJewelHoloEffects(p);
 if(!park||park.status!=='ACTIVE'||park.landscape!==rule.landscape||
    c.round!==park.started.combatRound+park.rounds.length)
  throw Error('Paisagem ou rodada real do Holo-Park não corresponde ao efeito.');
 if(rule.kind==='player-same-roll-boost')throw Error('Inseto de Kashyyyk só pode ser aplicado durante a MESMA rolagem do ataque.');
 if(details.gmSymbolConfirmed!==true||!validId(details.actionId)||p.operations.receipts?.[details.actionId])
  throw Error('Mestre deve confirmar símbolo publicado e ID novo.');
 let ch=clone(character),attackId=null,targetId=null,result;
 if(rule.kind==='npc-fountain-cover'||rule.kind==='npc-opposed-stealth'||rule.kind==='npc-vertigo'){
  const evidence=(c.holoParkPendingNpcDespair??[]).find(x=>x.attackId===details.npcAttackId);
  if(!evidence||evidence.landscape!==rule.landscape||evidence.round!==c.round||
     !c.adversaries?.some(a=>a.id===evidence.attackerId)||
     !c.log?.some(x=>x.type==='TURNO_NPC'&&x.attackId===evidence.attackId&&
       x.actorId===evidence.attackerId&&x.round===evidence.round&&
       eq({pool:x.check?.pool,faces:x.check?.faces,netAdvantage:x.check?.netAdvantage,despair:x.check?.despair},evidence.check)))
   throw Error('Desespero NPC não corresponde a ataque real.');
  verifyNpcCheck(evidence.check,'despair');
  if(rule.kind!=='npc-vertigo'&&(details.gmTargetPresentConfirmed!==true||!validId(details.targetId)))
   throw Error('Mestre deve confirmar NPC alvo e sua presença física.');
  targetId=rule.kind==='npc-vertigo'?null:details.targetId;
  const target=targetId?c.adversaries.find(a=>a.id===targetId):null;
  if(rule.kind!=='npc-vertigo'&&(!target||adversaryHealthStatus(target).incapacitated))
   throw Error('NPC alvo não existe ou está incapacitado.');
  attackId=evidence.attackId;
  if(rule.kind==='npc-vertigo'){
   if(c.holoParkVertigo?.active)throw Error('Vertigem pendente: não acumular.');
   const characteristic=Number(ch.characteristics?.voluntad??0),skillRank=Number(ch.skills?.disciplina??0);
   const check=rollPool({...buildPositivePool(characteristic,skillRank),difficulty:3},details.rng??Math.random);
   if(!check.success){
    ch=sufferStrain(ch,2).character;
    c.holoParkVertigo={active:true,receiptId:details.actionId,attackId:evidence.attackId,round:c.round};
   }
   result={attackerId:evidence.attackerId,characteristic,skillRank,
    check:{pool:check.pool,faces:check.faces,success:check.success,netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair},
    strainApplied:check.success?0:2,downgradePending:!check.success};
  }else if(rule.kind==='npc-fountain-cover'){
   if(target.holoParkFountain?.active)throw Error('Fonte já oferece cobertura ao NPC; não acumular.');
   const next=updateAdversaryInCombat(c,{...target,holoParkFountain:{active:true,receiptId:details.actionId,rangedDefense:2}});
   c.adversaries=next.adversaries;
   result={attackerId:evidence.attackerId,rangedDefense:2,untilMovement:true};
  }else{
   if(target.holoParkHiddenFromPlayer?.active)throw Error('Alvo já oculto.');
   const characteristic=adversaryCharacteristicForSkill(target,'sigilo'),skillRank=adversarySkillRank(target,'sigilo');
   const opponentCharacteristic=Number(ch.characteristics?.astucia??0),opponentSkillRank=Number(ch.skills?.percepcion??0);
   const neg=buildOpposedNegativePool(opponentCharacteristic,opponentSkillRank);
   const check=rollPool({...buildPositivePool(characteristic,skillRank),difficulty:neg.difficulty,challenge:neg.challenge},details.rng??Math.random);
   if(check.success){
    const next=updateAdversaryInCombat(c,{...target,holoParkHiddenFromPlayer:{active:true,receiptId:details.actionId}});
    c.adversaries=next.adversaries;
   }
   result={attackerId:evidence.attackerId,characteristic,skillRank,opponentCharacteristic,opponentSkillRank,
    check:{pool:check.pool,faces:check.faces,success:check.success,netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair},hidden:check.success};
  }
  c.holoParkPendingNpcDespair=c.holoParkPendingNpcDespair.filter(x=>x.attackId!==attackId);
  details={...details,npcCheck:clone(evidence.check)};
 }else if(rule.kind==='npc-spotlight'||rule.kind==='npc-transport'||rule.kind==='npc-drop-weapon'){
  const evidence=(c.holoParkPendingNpcThreat??[]).find(x=>x.attackId===details.npcAttackId);
  if(!evidence||evidence.landscape!==rule.landscape||evidence.round!==c.round||
     !c.adversaries?.some(a=>a.id===evidence.attackerId)||
     !c.log?.some(x=>x.type==='TURNO_NPC'&&x.attackId===evidence.attackId&&
       x.actorId===evidence.attackerId&&x.round===evidence.round&&
       eq({pool:x.check?.pool,faces:x.check?.faces,netAdvantage:x.check?.netAdvantage,despair:x.check?.despair},evidence.check)))
   throw Error('Ameaça NPC não corresponde ao ataque real registrado.');
  verifyNpcCheck(evidence.check);
  attackId=evidence.attackId;
  if(rule.kind==='npc-spotlight'){
   if(c.holoParkSpotlight?.active)throw Error('Holofote já ativo; não acumular.');
   c.holoParkSpotlight={receiptId:details.actionId,active:true,attackId};
  }else if(rule.kind==='npc-drop-weapon'){
   if(details.gmWeaponHeldConfirmed!==true||!Number.isSafeInteger(details.weaponIndex)||
      details.weaponIndex<0||!ch.weapons?.[details.weaponIndex])
    throw Error('Mestre deve confirmar a arma realmente empunhada e seu índice na ficha.');
   if(c.holoParkDroppedWeapon?.active)throw Error('Há uma arma caída ainda não recuperada.');
   const weapon=ch.weapons[details.weaponIndex];
   c.holoParkDroppedWeapon={active:true,receiptId:details.actionId,attackId,weaponIndex:details.weaponIndex,
      weaponName:weapon.name,droppedRound:c.round};
  }else{
   if(c.holoParkTransport&&(c.holoParkTransport.playerActive||c.holoParkTransport.companionIds?.length))
    throw Error('Transporte já obstrui os combatentes; não acumular.');
   if(details.gmCompanionsPresentConfirmed!==true||!Array.isArray(details.affectedCompanionIds))
    throw Error('Mestre deve confirmar quais companheiros estavam na área do transporte.');
   const ids=details.affectedCompanionIds;
   if(new Set(ids).size!==ids.length||ids.some(id=>!validId(id)||!group?.companions?.some(m=>m.id===id)))
    throw Error('Companheiro não pertence ao grupo real ou está duplicado.');
   c.holoParkTransport={receiptId:details.actionId,playerActive:true,companionIds:[...ids],attackId};
  }
  c.holoParkPendingNpcThreat=c.holoParkPendingNpcThreat.filter(x=>x.attackId!==attackId);
  result=rule.kind==='npc-drop-weapon'?
   {attackerId:evidence.attackerId,weaponIndex:details.weaponIndex,weaponName:ch.weapons[details.weaponIndex].name,
    recover:'next-round-maneuver-or-one-strain'}:
   {attackerId:evidence.attackerId,setback:1,
    duration:rule.kind==='npc-spotlight'?'until-next-player-turn-end':'until-real-movement',
    ...(rule.kind==='npc-transport'?{companionIds:[...details.affectedCompanionIds]}:{})};
  details={...details,npcCheck:clone(evidence.check)};
 }else if(rule.kind==='player-strain-and-maneuver'){
  const attack=c.lastAttack;
  if(!attack||!validId(attack.attackId)||attack.check?.despair<1||
    Number(c.playerTurn?.maneuversUsed)!==0||c.playerTurn?.actionUsed!==true)
   throw Error('Alderaan exige Desespero real do ataque do jogador e manobra ainda disponível.');
  attackId=attack.attackId;
  ch=sufferStrain(ch,2).character;
  const lost=spendManeuver(c,{method:'normal',label:'Holo-Park Alderaan: manobra perdida'});
  c.playerTurn=lost.combat.playerTurn;
  result={strainApplied:2,maneuversLost:1};
 }else{
  const spend=c.pendingPlayerSpend;
  if(!spend?.attackSnapshot?.hit||!validId(spend.attackId)||!validId(spend.adversaryId)||
     (rule.symbol==='triumph'?spend.remainingTriumph:spend.remainingAdvantage)<rule.cost)
   throw Error('Efeito exige ataque acertado e símbolo real ainda não gasto.');
  attackId=spend.attackId;
  if(rule.symbol==='triumph')spend.remainingTriumph-=rule.cost;
  else spend.remainingAdvantage-=rule.cost;
  c.pendingPlayerSpend=spend;
  if(rule.kind==='player-ranged-boost'){
   if(details.gmBoardedThrantaConfirmed!==true)throw Error('Mestre deve confirmar embarque físico na thranta holográfica.');
   if(c.holoParkThranta&&c.holoParkThranta.throughRound>=c.round)throw Error('Bônus de thranta já ativo; não acumular.');
   c.holoParkThranta={receiptId:details.actionId,fromRound:c.round+1,throughRound:c.round+2,bonus:1};
   result={rangedBoost:1,fromRound:c.round+1,throughRound:c.round+2};
  }else if(rule.kind==='player-nerf-stealth'){
   if(details.gmHerdConfirmed!==true||details.maneuverSpent!==true||
      c.log?.at(-1)?.type!=='MANOBRA'||c.log.at(-1).label!=='Holo-Park Alderaan: ocultar-se entre nerfs'||
      c.log.at(-1).round!==c.round)
    throw Error('Mestre deve confirmar a manada e gastar manobra real de Furtividade.');
   const characteristic=Number(ch.characteristics?.agilidad??0),skillRank=Number(ch.skills?.sigilo??0);
   const check=rollPool({...buildPositivePool(characteristic,skillRank),difficulty:2},details.rng??Math.random);
   c.holoParkNerfStealth={receiptId:details.actionId,round:c.round,success:check.success,gmEscapePending:true};
   result={maneuverSpent:true,gmEscapePending:true,check:{characteristic,skillRank,pool:check.pool,
     faces:check.faces,success:check.success,netSuccess:check.netSuccess,netAdvantage:check.netAdvantage,
     triumph:check.triumph,despair:check.despair}};
  }else if(rule.kind==='player-building-cover'){
   if(c.holoParkCoruscantCover?.active)throw Error('Cobertura de Coruscant já ativa; não acumular.');
   c.holoParkCoruscantCover={receiptId:details.actionId,active:true,rangedDefense:2};
   result={rangedDefense:2,untilMovement:true};
  }else if(rule.kind==='player-ranged-cover'){
   if(c.holoParkCover&&c.holoParkCover.throughRound>=c.round)
    throw Error('Cobertura holográfica já ativa; não acumular bônus.');
   c.holoParkCover={receiptId:details.actionId,throughRound:c.round+1,bonus:1};
   result={rangedSetback:1,throughRound:c.round+1};
  }else{
   targetId=spend.adversaryId;
   const target=c.adversaries?.find(x=>x.id===targetId);
   if(!target||adversaryHealthStatus(target).incapacitated)throw Error('Alvo real do ataque não encontrado ou incapacitado.');
   if(rule.kind==='npc-shadow-cover'){
    if(target.holoParkShadow&&target.holoParkShadow.throughRound>=c.round)throw Error('Sombra já aplicada ao alvo; não acumular.');
    const affected={...target,holoParkShadow:{receiptId:details.actionId,throughRound:c.round+1,rangedDefense:1}};
    const next=updateAdversaryInCombat(c,affected);c.adversaries=next.adversaries;
    result={rangedDefense:1,throughRound:c.round+1};
   }else{
    const condition=rule.kind==='npc-staggered'?'staggered':'disoriented';
    const affected=applyStatusEffect(target,{type:condition,duration:'rounds',rounds:1,source:'SWE09 Holo-Park',sourceId:details.actionId});
    const next=updateAdversaryInCombat(c,affected);c.adversaries=next.adversaries;
    result={condition,rounds:1};
   }
  }
 }
 const previous=validateJewelHoloEffects(p);
 if(previous?.history.some(x=>x.attackId===attackId&&x.round===c.round))throw Error('Ataque já ativou efeito do Holo-Park.');
 const receipt={id:details.actionId,kind:'JEWEL_HOLO_EFFECT',version:1,source:SOURCE,effect:details.effect,
  landscape:rule.landscape,round:c.round,gmSymbolConfirmed:true,
  ...(rule.kind==='player-ranged-boost'?{gmBoardedThrantaConfirmed:true}:{}),
  ...(rule.kind==='player-nerf-stealth'?{gmHerdConfirmed:true}:{}),
  ...(rule.kind==='npc-transport'?{gmCompanionsPresentConfirmed:true,affectedCompanionIds:[...details.affectedCompanionIds]}:{}),
  ...(rule.kind==='npc-drop-weapon'?{gmWeaponHeldConfirmed:true}:{}),
  ...(['npc-fountain-cover','npc-opposed-stealth'].includes(rule.kind)?{gmTargetPresentConfirmed:true}:{}),
  attackId,targetId,symbol:rule.symbol,cost:rule.cost,result,
  ...(details.npcCheck?{npcCheck:details.npcCheck}:{})};
 p.operations.receipts[receipt.id]=clone(receipt);
 p.operations.holoEffects=replayJewelHoloEffects(p);validateJewelHoloEffects(p);
 c.log=[...(c.log??[]),{type:'JEWEL_HOLO_EFFECT',round:c.round,receiptId:receipt.id,effect:details.effect}];
 return {world:w,combat:c,character:ch,adjudication:receipt,effects:clone(p.operations.holoEffects)};
}
/** One published Kashyyyk setback die for ranged attacks against the PC,
 * lasting through the next real combat round, never a permanent armor bonus. */
export function holoParkRangedCover(combat,weapon){
 const cover=combat?.holoParkCover;
 if(!combat?.active||!cover||!Number.isSafeInteger(cover.throughRound)||combat.round>cover.throughRound||
    ['pelea','armas_cuerpo_a_cuerpo'].includes(weapon?.skill))return 0;
 return cover.bonus===1?1:0;
}

/** Alderaan: a holographic thranta's higher ground grants one Boost on
 * ranged attacks for two real combat rounds. No melee or expired bonus. */
export function holoParkThrantaBoost(combat,weapon){
 const flight=combat?.holoParkThranta;
 if(!combat?.active||!flight||!Number.isSafeInteger(flight.fromRound)||!Number.isSafeInteger(flight.throughRound)||
  combat.round<flight.fromRound||combat.round>flight.throughRound||
  ['pelea','armas_cuerpo_a_cuerpo'].includes(weapon?.skill))return 0;
 return flight.bonus===1?1:0;
}
/** Christophsis: shadow of a spire grants cover (ranged defense 1) to the
 * real attacked opponent through the following round, never melee defense. */
export function holoParkShadowDefense(target,weapon,round){
 const shadow=target?.holoParkShadow;
 if(!shadow||!Number.isSafeInteger(round)||round>shadow.throughRound||
  ['pelea','armas_cuerpo_a_cuerpo'].includes(weapon?.skill))return 0;
 return shadow.rangedDefense===1?1:0;
}

/** Coruscant: flickering building provides ranged defense 2, ending on the
 * character's ACTUAL movement, not on arbitrary elapsed narrative time. */
export function holoParkCoruscantCover(combat,weapon){
 const cover=combat?.holoParkCoruscantCover;
 if(!combat?.active||cover?.active!==true||cover.rangedDefense!==2||
  ['pelea','armas_cuerpo_a_cuerpo'].includes(weapon?.skill))return 0;
 return 2;
}
/** Only a real movement maneuver clears the Coruscant building cover. */
export function clearHoloParkCoruscantCover(combat){
 if(!combat?.holoParkCoruscantCover?.active)return combat;
 return {...combat,holoParkCoruscantCover:{...combat.holoParkCoruscantCover,active:false}};
}

/** A companion must spend a GM-confirmed REAL movement maneuver to leave the
 * obstructing Coruscant transport. Other companions remain affected. */
export function clearHoloParkTransportCompanion(combat,companionId,gmMovementConfirmed){
 if(gmMovementConfirmed!==true||!validId(companionId)||!combat?.active||
    !combat?.holoParkTransport?.companionIds?.includes(companionId))
  throw Error('Movimento do companheiro não confirmado ou sem transporte ativo.');
 const c=clone(combat),moved=c.holoParkCompanionMoves??[];
 if(moved.some(x=>x.round===c.round&&x.companionId===companionId))
  throw Error('Companheiro já gastou a manobra de movimento nesta rodada.');
 c.holoParkTransport.companionIds=c.holoParkTransport.companionIds.filter(id=>id!==companionId);
 c.holoParkCompanionMoves=[...moved,{round:c.round,companionId}];
 c.log.push({type:'JEWEL_HOLO_COMPANION_MOVEMENT',round:c.round,companionId});
 return c;
}

/** SWE09 pp.93–94: dropped weapon remains in inventory but cannot fire until
 * recovered. Physical location and the wielded weapon are confirmed by GM. */
export function assertJewelHoloWeaponUsable(combat,weaponIndex){
 const dropped=combat?.holoParkDroppedWeapon;
 if(combat?.active&&dropped?.active&&dropped.weaponIndex===weaponIndex)
  throw Error('Arma caída no Holo-Park: recupere-a antes de atacar.');
}
export function recoverJewelHoloDroppedWeapon(combat,character,{method='maneuver',gmRecoveryConfirmed=false,
 maneuverSpent=false}={}){
 const c=clone(combat),ch=clone(character),d=c.holoParkDroppedWeapon;
 if(!c?.active||!d?.active||!Number.isSafeInteger(d.weaponIndex)||
    ch?.weapons?.[d.weaponIndex]?.name!==d.weaponName||gmRecoveryConfirmed!==true)
  throw Error('Recuperação exige arma caída real, combate ativo e confirmação física do Mestre.');
 if(!['maneuver','strain'].includes(method))throw Error('Método de recuperação inválido.');
 if(method==='maneuver'&&(c.round<=d.droppedRound||maneuverSpent!==true))
  throw Error('Recuperação por manobra somente na rodada seguinte, após gastar manobra real.');
 const updated=method==='strain'?sufferStrain(ch,1).character:ch;
 c.holoParkDroppedWeapon={...d,active:false,recoveredRound:c.round,recoveryMethod:method};
 c.log=[...(c.log??[]),{type:'JEWEL_HOLO_WEAPON_RECOVER',round:c.round,receiptId:d.receiptId,
   weaponIndex:d.weaponIndex,method,strainApplied:method==='strain'?1:0}];
 return {combat:c,character:updated,recovery:clone(c.holoParkDroppedWeapon)};
}
/** GM chooses the baseline NPC-strain fallback for an UNUSED reserved Threat.
 * It cannot also be spent on a landscape effect. */
export function settleJewelHoloUnusedNpcThreat(combat,progress,{npcAttackId,gmFallbackConfirmed=false}={}){
 const c=clone(combat),park=validateJewelHoloPark(progress);
 if(gmFallbackConfirmed!==true||!c.active||park?.status!=='ACTIVE'||!validId(npcAttackId))
  throw Error('Fallback exige ameaça reservada e confirmação do Mestre.');
 const pending=(c.holoParkPendingNpcThreat??[]).find(x=>x.attackId===npcAttackId);
 if(!pending||pending.round!==c.round||pending.landscape!==park.landscape||
   !c.log?.some(x=>x.type==='TURNO_NPC'&&x.attackId===pending.attackId&&
    x.actorId===pending.attackerId&&eq(x.check?.faces,pending.check.faces)))
  throw Error('Ameaça NPC não corresponde a um ataque real não gasto nesta rodada.');
 verifyNpcCheck(pending.check);
 const target=c.adversaries?.find(x=>x.id===pending.attackerId);
 if(!target)throw Error('Atacante NPC ausente do combate real.');
 const applied=sufferAdversaryStrain(target,1);
 c.adversaries=updateAdversaryInCombat(c,applied.adversary).adversaries;
 c.holoParkPendingNpcThreat=c.holoParkPendingNpcThreat.filter(x=>x.attackId!==npcAttackId);
 c.log=[...(c.log??[]),{type:'JEWEL_HOLO_NPC_THREAT_FALLBACK',round:c.round,
  attackId:npcAttackId,attackerId:pending.attackerId,amount:1,convertedToWounds:applied.convertedToWounds}];
 return {combat:c,applied:{attackId:npcAttackId,attackerId:pending.attackerId,amount:1,
  convertedToWounds:applied.convertedToWounds}};
}

/** GM can explicitly retain the legacy NPC strain consequence when a real
 * Despair was reserved but NOT spent on a published landscape effect. */
export function settleJewelHoloUnusedNpcDespair(combat,progress,{npcAttackId,gmFallbackConfirmed=false}={}){
 const c=clone(combat),park=validateJewelHoloPark(progress);
 if(gmFallbackConfirmed!==true||!c.active||park?.status!=='ACTIVE'||!validId(npcAttackId))
  throw Error('Fallback de Desespero exige confirmação do Mestre.');
 const pending=(c.holoParkPendingNpcDespair??[]).find(x=>x.attackId===npcAttackId);
 if(!pending||pending.round!==c.round||pending.landscape!==park.landscape||
   !c.log?.some(x=>x.type==='TURNO_NPC'&&x.attackId===pending.attackId&&
     x.actorId===pending.attackerId&&eq(x.check?.faces,pending.check.faces)))
  throw Error('Desespero NPC não corresponde a ataque real ainda disponível.');
 verifyNpcCheck(pending.check,'despair');
 const target=c.adversaries?.find(x=>x.id===pending.attackerId);
 if(!target)throw Error('Atacante NPC ausente.');
 const applied=sufferAdversaryStrain(target,1);
 c.adversaries=updateAdversaryInCombat(c,applied.adversary).adversaries;
 c.holoParkPendingNpcDespair=c.holoParkPendingNpcDespair.filter(x=>x.attackId!==npcAttackId);
 c.log=[...(c.log??[]),{type:'JEWEL_HOLO_NPC_DESPAIR_FALLBACK',round:c.round,
  attackId:npcAttackId,attackerId:pending.attackerId,amount:1,convertedToWounds:applied.convertedToWounds}];
 return {combat:c,applied:{attackId:npcAttackId,attackerId:pending.attackerId,amount:1,
  convertedToWounds:applied.convertedToWounds}};
}

/** P233: shared, pure positive-pool downgrade for actions whose dice are
 * resolved by a domain engine (Medicine / Stim Application). The domain
 * engine must roll the modified pool BEFORE applying any result; a boolean
 * alone never authorizes consuming a pending effect. */
export function downgradeVertigoPositivePool(pool,enabled=false){
 if(typeof enabled!=='boolean')throw Error('Aplicação da vertigem inválida.');
 const next={...pool};
 if(enabled&&Number(next.proficiency??0)>0){next.proficiency-=1;next.ability=Number(next.ability??0)+1;}
 return next;
}

/** P216: a failed Difficult Discipline test downgrades exactly the next
 * player ACTION's positive pool, never an earlier or an NPC roll. */
export function consumeJewelHoloVertigo(combat,pool,{actionKind='PLAYER_ACTION'}={}){
 if(!combat?.active||combat?.holoParkVertigo?.active!==true)return {combat,pool,downgraded:false};
 if(!/^[A-Z_]{3,45}$/.test(String(actionKind)))throw Error('Ação de vertigem inválida.');
 const c=clone(combat),positive=downgradeVertigoPositivePool(pool,true);
 c.holoParkVertigo={...c.holoParkVertigo,active:false,consumedRound:c.round};
 c.log=[...(c.log??[]),{type:'JEWEL_HOLO_VERTIGO_DOWNGRADE',round:c.round,receiptId:c.holoParkVertigo.receiptId,
  downgraded:Number(pool.proficiency??0)>0,actionKind,poolBefore:clone(pool),poolAfter:clone(positive)}];
 return {combat:c,pool:positive,downgraded:Number(pool.proficiency??0)>0};
}

/** P233: attach the exact rolled faces to the consumption event. A
 * Medicine/Stim result without this proof cannot certify the downgrade. */
export function attachJewelHoloVertigoRoll(combat,check,actionKind){
 if(!combat?.active||!['PLAYER_MEDICINE','PLAYER_STIM_APPLICATION'].includes(actionKind))return combat;
 const c=clone(combat),idx=c.log?.findLastIndex(x=>x.type==='JEWEL_HOLO_VERTIGO_DOWNGRADE'&&x.actionKind===actionKind&&x.checkProof===undefined);
 if(idx<0)return c;
 const proof={pool:clone(check.pool),faces:clone(check.faces),success:check.success,netSuccess:check.netSuccess,
  netAdvantage:check.netAdvantage,triumph:check.triumph,despair:check.despair};
 verifySkillRoll(proof,proof.pool);
 c.log[idx]={...c.log[idx],checkProof:proof};
 return c;
}
/** Verify P233 positive-pool consumption against an original NPC-Despair
 * receipt and (for domain actions) the real dice faces. Old P216 logs are
 * intentionally accepted without newly introduced proof fields. */
export function validateJewelHoloVertigoConsumption(combat,progress){
 const seen=new Set();
 for(const entry of combat?.log??[]){
  if(entry?.type!=='JEWEL_HOLO_VERTIGO_DOWNGRADE')continue;
  if(entry.poolBefore===undefined&&entry.poolAfter===undefined&&entry.actionKind===undefined)continue; // P216 save
  const source=progress?.operations?.receipts?.[entry.receiptId];
  if(!source||source.effect!=='kashyyyk_vertigo'||source.result?.downgradePending!==true||
     !Number.isSafeInteger(entry.round)||entry.round<1||!validId(entry.receiptId)||seen.has(entry.receiptId)||
     !['PLAYER_ACTION','PLAYER_SKILL','PLAYER_WEAPON','PLAYER_MEDICINE','PLAYER_STIM_APPLICATION'].includes(entry.actionKind)||
     !entry.poolBefore||!entry.poolAfter||
     !eq(downgradeVertigoPositivePool(entry.poolBefore,true),entry.poolAfter)||
     entry.downgraded!==(Number(entry.poolBefore.proficiency??0)>0))
   throw Error('Consumo da vertigem adulterado ou duplicado.');
  seen.add(entry.receiptId);
  if(['PLAYER_MEDICINE','PLAYER_STIM_APPLICATION'].includes(entry.actionKind)){
   if(!entry.checkProof)throw Error('Rolagem real da vertigem ausente.');
   verifySkillRoll(entry.checkProof,entry.checkProof.pool);
   if(entry.checkProof.pool.ability!==entry.poolAfter.ability||entry.checkProof.pool.proficiency!==entry.poolAfter.proficiency)
    throw Error('Pool real da vertigem divergente.');
  }else if(entry.checkProof!==undefined)throw Error('Prova de vertigem indevida.');
 }
 return true;
}

/** P216: extra two real Boost dice are rolled INTO the original attack BEFORE
 * hit/damage resolution. The player opts in before the roll; a real Triumph
 * is required, and no earlier resolved attack is ever retroactively changed. */
function compactRoll(r){return {pool:clone(r.pool),faces:clone(r.faces),success:r.success,netSuccess:r.netSuccess,
 netAdvantage:r.netAdvantage,triumph:r.triumph,despair:r.despair};}
export function augmentJewelKashyyykInsectRoll(check,rng=Math.random){
 const base=compactRoll(check);verifySkillRoll(base,base.pool);
 if(base.triumph<1)return {check,proof:null};
 if(Number(base.pool.boost??0)>10)throw Error('Limite de dados de Benefício: inseto exige mais dois.');
 const extra=rollPool({boost:2},rng).faces;
 const pos=base.faces.findIndex(x=>x.type==='setback'),at=pos<0?base.faces.length:pos;
 const faces=[...base.faces.slice(0,at),...extra,...base.faces.slice(at)];
 const pool={...base.pool,boost:Number(base.pool.boost??0)+2};
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};
 for(const f of faces)for(const [k,n] of Object.entries(f.face))raw[k]+=n;
 const symbols=resolveSymbols(raw);
 const final={...symbols,pool,faces};
 const proof={base,final:compactRoll(final),extraBoostFaces:clone(extra),boostDice:2,triumphSpent:1};
 return {check:{...check,...final},proof};
}
/** Store the published effect in the same atomic transaction as the attack.
 * This function NEVER rolls dice and cannot be called as a post-attack spend. */
export function recordJewelKashyyykInsect(world,combat,attack,proof,{gmInsectConfirmed=false}={}){
 const w=clone(world),c=clone(combat),p=w?.publishedAdventure;
 if(gmInsectConfirmed!==true||!proof||p?.id!=='jewel-of-yavin'||p.currentNodeId!=='cloud-city-pursuit'||
    !c.active||!validId(attack?.attackId)||!validId(attack?.targetAdversaryId)||
    !c.adversaries?.some(a=>a.id===attack.targetAdversaryId))throw Error('Inseto exige ataque real, Kashyyyk e confirmação do Mestre.');
 const park=validateJewelHoloPark(p);validateJewelHoloEffects(p);
 if(park?.status!=='ACTIVE'||park.landscape!=='kashyyyk'||c.round!==park.started.combatRound+park.rounds.length)
  throw Error('Inseto exige a paisagem Kashyyyk na rodada atual.');
 if(c.pendingPlayerSpend?.attackId===attack.attackId){
  if(c.pendingPlayerSpend.remainingTriumph<1)throw Error('Triunfo já gasto.');
  c.pendingPlayerSpend.remainingTriumph-=1;
 }
 const id=`holo-insect-${attack.attackId}`;
 if(p.operations.receipts?.[id])throw Error('Inseto já utilizado neste ataque.');
 const receipt={id,kind:'JEWEL_HOLO_EFFECT',version:1,source:SOURCE,effect:'kashyyyk_insect',
  landscape:'kashyyyk',round:c.round,gmSymbolConfirmed:true,attackId:attack.attackId,
  targetId:attack.targetAdversaryId,symbol:'triumph',cost:1,result:clone(proof)};
 p.operations.receipts[id]=receipt;
 p.operations.holoEffects=replayJewelHoloEffects(p);validateJewelHoloEffects(p);
 c.log=[...(c.log??[]),{type:'JEWEL_HOLO_EFFECT',round:c.round,receiptId:id,effect:'kashyyyk_insect',
  baseNetSuccess:proof.base.netSuccess,finalNetSuccess:proof.final.netSuccess}];
 return {world:w,combat:c,receipt};
}
