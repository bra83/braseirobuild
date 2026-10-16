import { validateJewelGrandPrixPilotEvidence } from './jewelGrandPrixDice.mjs';
import {validateJewelBankState} from './jewelBankingOperations.mjs';
export {performJewelBankStep} from './jewelBankingOperations.mjs';
export {settleJewelBank,validateJewelBankSettlement} from './jewelBankSettlement.mjs';
export {performJewelBankPreparation,validateJewelBankPreparation} from './jewelBankPreparations.mjs';
export {hireJewelSlicer,validateJewelSlicerHire} from './jewelHiredSlicers.mjs';
/**
 * SWE09 The Jewel of Yavin, Episode I Grand Prix (pp. 48-55) and
 * Episode II Party on Cloud Nine (pp. 58-68). No automatic award of
 * race prizes, auction credits or party funds: only GM-adjudicated events.
 * These operations mutate a cloned publishedAdventure progress snapshot.
 */
const BIDDERS = Object.freeze([
  ['elaiza', 'Elaiza', 1],
  ['pos_podura', 'Pos Podura', 2],
  ['shreya_ordassa', 'Shreya Ordassa', 3],
  ['mil_mikkir', 'Mil Mikkir', 4],
  ['vorse_tabarith', 'Vorse Tabarith', 5],
  ['zekra_fol', 'Zekra Fol', 6],
  ['kaltho', 'Kaltho the Hutt', 7]
]);
const own = (obj, key) => Object.prototype.hasOwnProperty.call(obj, key);
const copy = obj => structuredClone(obj);
const assertInt = (v, label, min, max) => {
  if (!Number.isSafeInteger(v) || v < min || v > max) throw new Error(`${label} fora do intervalo permitido.`);
};
const receiptId = (value) => {
  if (typeof value !== 'string' || !/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(value) || ['__proto__','constructor','prototype','toString','valueOf'].includes(value))
    throw new Error('actionId obrigatório e inválido.');
  return value;
};
function context(world, nodes) {
  const w = copy(world || {});
  const p = w.publishedAdventure;
  if (!p || p.id !== 'jewel-of-yavin' || p.status !== 'ACTIVE' || !own(nodes, p.currentNodeId))
    throw new Error('A Joia de Yavin não está ativa em uma cena válida.');
  p.operations ??= {};
  p.operations.receipts ??= {};
  validateJewelOperations(w);
  return { w, p };
}
function guard(p, id) {
  if (own(p.operations.receipts, id)) throw new Error('Ação já registrada: idempotência protegida.');
}
function finish(w, p, id, kind, details) {
  const record = { id, kind, ...details };
  p.operations.receipts[id] = record;
  p.updatedAt = new Date().toISOString();
  return { world:w, progress:copy(p), adjudication:copy(record) };
}
function auction(p) {
  if (!p.operations.auction) {
    p.operations.auction = {
      round:0, highBidCredits:0, highBidderId:null, completed:false,
      bidders:Object.fromEntries(BIDDERS.map(([id,name,base])=>[id,{id,name,base,bidValue:base,active:true}])),
      roundHistory:[], pendingInfluences:[]
    };
  }
  return p.operations.auction;
}
function ensureAuctionScene(p) {
  if (p.currentNodeId !== 'cloud-nine-party') throw new Error('Leilão disponível apenas na recepção Cloud Nine.');
}
export function influenceJewelBidder(world, nodes, { actionId, bidderId, success, netSuccesses = 0, triumphs = 0, favorableImpression = false } = {}) {
  const {w,p} = context(world,nodes); const id = receiptId(actionId); guard(p,id); ensureAuctionScene(p);
  const a=auction(p); if(a.completed) throw new Error('Leilão já encerrado.');
  if(typeof bidderId !== 'string' || !own(a.bidders,bidderId) || !a.bidders[bidderId].active) throw new Error('Licitante inexistente ou eliminado.');
  if(typeof success !== 'boolean' || typeof favorableImpression !== 'boolean') throw new Error('Resultado ou impressão favorável inválidos.');
  assertInt(netSuccesses,'Sucessos líquidos',0,20); assertInt(triumphs,'Triunfos',0,10);
  if(success && netSuccesses < 1) throw new Error('Teste bem-sucedido exige ao menos um sucesso líquido.');
  if(!success && netSuccesses !== 0) throw new Error('Teste fracassado não concede bônus de lance.');
  const delta=success ? netSuccesses+3*triumphs : 0;
  a.pendingInfluences.push({id,bidderId,delta,favorableImpression,success});
  return finish(w,p,id,'AUCTION_INFLUENCE',{bidderId,delta,success,netSuccesses,triumphs,favorableImpression,round:a.round+1});
}
export function adjudicateJewelAuctionRound(world,nodes,{actionId}={}) {
  const {w,p}=context(world,nodes); const id=receiptId(actionId); guard(p,id); ensureAuctionScene(p);
  const a=auction(p); if(a.completed || a.round>=10) throw new Error('Leilão já encerrado.');
  const influenced=a.pendingInfluences.filter(x=>x.delta>0);
  for(const bidder of Object.values(a.bidders)) if(bidder.active) bidder.bidValue += bidder.base;
  for(const effect of influenced) a.bidders[effect.bidderId].bidValue += effect.delta;
  const contenders=Object.values(a.bidders).filter(b=>b.active).sort((x,y)=>y.bidValue-x.bidValue || y.base-x.base);
  const leader=contenders[0];
  for(const b of contenders) if(leader.bidValue-b.bidValue>10) b.active=false;
  a.round++;
  a.highBidCredits += 20000 + (influenced.length ? 10000 : 0);
  a.highBidderId=leader.id;
  a.completed=a.round>=10 || Object.values(a.bidders).filter(b=>b.active).length===1;
  const round={round:a.round,highBidCredits:a.highBidCredits,highBidderId:leader.id,remaining:Object.values(a.bidders).filter(b=>b.active).map(b=>b.id),influenceReceipts:a.pendingInfluences.map(x=>x.id)};
  a.roundHistory.push(round); a.pendingInfluences=[];
  if(a.completed) p.flags.auctionWinnerAdjudicated=true;
  return finish(w,p,id,'AUCTION_ROUND',{...round,completed:a.completed});
}
export function jewelAuctionStatus(world) { return copy(world?.publishedAdventure?.operations?.auction || null); }

const LEG_NAMES = Object.freeze(['open-stretch-1','open-stretch-2','open-stretch-3','tibannuck-feeding-ground','buoy-4-to-5','buoy-5-to-6','buoy-6-to-8']);
const RACE_HAZARD_SETBACKS = Object.freeze([0,0,0,2,1,0,0]);
export function jewelGrandPrixLegRequirements(leg, raceState = null) {
  assertInt(leg,'Trecho',1,7);
  return {leg,name:LEG_NAMES[leg-1],terrainSetbacks:RACE_HAZARD_SETBACKS[leg-1],
    missedBuoySetbacks:raceState?.legs?.at(-1)?.missedBuoy?1:0,
    source:'SWE09 The Jewel of Yavin, Grand Prix pp.50-53',
    special:leg===4?'tibannuck':leg===5?'beldon':null};
}

function race(p) {
  p.operations.race ??= { leg:0,placeValue:0,systemStrain:0,missedBuoys:0,completed:false,legs:[],placement:null };
  return p.operations.race;
}
export function adjudicateJewelGrandPrixLeg(world,nodes,{actionId,leg,success,speed,placeAdjustment=0,systemStrainDelta=0,missedBuoy=false,criticalCollision=false,hazard=null,pilotEvidence=null,vehicleId=null}={}) {
  const {w,p}=context(world,nodes); const id=receiptId(actionId); guard(p,id);
  if(p.currentNodeId!=='grand-prix-race') throw new Error('Trecho disponível somente durante o Grande Prêmio.');
  const r=race(p); if(r.completed) throw new Error('Grande Prêmio já concluído.');
  assertInt(leg,'Trecho',1,7); if(leg!==r.leg+1) throw new Error('Trechos devem ser adjudicados em ordem.');
  if(typeof success!=='boolean'||typeof missedBuoy!=='boolean'||typeof criticalCollision!=='boolean') throw new Error('Resultado da corrida inválido.');
  assertInt(speed,'Velocidade',0,6); assertInt(placeAdjustment,'Ajuste de posição',-3,3); assertInt(systemStrainDelta,'Tensão de sistema',-10,10);
  if(success && missedBuoy) throw new Error('Passagem bem-sucedida não pode registrar boia perdida.');
  const requirements=jewelGrandPrixLegRequirements(leg,r);
  const appliedSetbacks=requirements.missedBuoySetbacks;
  if(pilotEvidence!==null){
    validateJewelGrandPrixPilotEvidence(pilotEvidence,leg,Boolean(r.legs.at(-1)?.missedBuoy));
    if(pilotEvidence.roll.success!==success||missedBuoy===success)throw new Error('Resultado do piloto incompatível com os dados.');
    if(typeof vehicleId!=='string'||!vehicleId||vehicleId.length>120)throw new Error('Veículo da corrida obrigatório.');
    if(leg===4||leg===5){
      if(!hazard||hazard.threats!==Math.max(0,-pilotEvidence.roll.netAdvantage)||hazard.despairs!==pilotEvidence.roll.despair)throw new Error('Perigo ambiental incompatível com os dados.');
    }else if(hazard!==null)throw new Error('Perigo ambiental incompatível com trecho seguro.');
  }
  let hazardStrain=0, criticalModifier=null;
  if(hazard!==null){
    if(leg!==4&&leg!==5)throw new Error('Consequência ambiental não prevista para este trecho.');
    if(!hazard||typeof hazard!=='object'||Array.isArray(hazard)||
       Object.keys(hazard).some(k=>!['threats','despairs','criticalHit'].includes(k)))throw new Error('Consequência ambiental inválida.');
    const {threats=0,despairs=0,criticalHit=false}=hazard;
    assertInt(threats,'Ameaças',0,20);assertInt(despairs,'Desesperos',0,10);
    if(typeof criticalHit!=='boolean')throw new Error('Acerto crítico ambiental inválido.');
    const needsCritical=leg===4?despairs>0:(threats>=3||despairs>0);
    if(criticalHit!==needsCritical)throw new Error('Crítico ambiental incompatível com dados do trecho.');
    if(leg===4&&threats>0)hazardStrain=1;
    if(criticalHit)criticalModifier=leg===4?-20:0;
  }
  if(criticalModifier!==null&&!criticalCollision)throw new Error('Crítico ambiental exige registro de colisão.');
  r.leg=leg; r.placeValue += speed+placeAdjustment;
  r.systemStrain=Math.max(0,r.systemStrain+systemStrainDelta+hazardStrain);
  if(missedBuoy) r.missedBuoys++;
  if(criticalCollision) r.criticalCollisions=(r.criticalCollisions||0)+1;
  const result={leg,name:LEG_NAMES[leg-1],success,speed,placeAdjustment,systemStrainDelta,placeValue:r.placeValue,systemStrain:r.systemStrain,missedBuoy,criticalCollision,appliedSetbacks,terrainSetbacks:requirements.terrainSetbacks,...(hazard!==null?{hazard:{threats:hazard.threats??0,despairs:hazard.despairs??0,criticalHit:hazard.criticalHit??false},hazardStrain,criticalModifier}: {}),...(pilotEvidence!==null?{pilotEvidence:copy(pilotEvidence),vehicleId}: {})};
  r.legs.push(result); if(leg===7) r.completed=true;
  return finish(w,p,id,'GRAND_PRIX_LEG',result);
}
export function adjudicateJewelGrandPrixPlacement(world,nodes,{actionId,place,competitorsPlaceValues}={}) {
  const {w,p}=context(world,nodes); const id=receiptId(actionId); guard(p,id);
  if(!['grand-prix-race','grand-prix-aftermath'].includes(p.currentNodeId)) throw new Error('Classificação fora do Grande Prêmio.');
  const r=race(p); if(!r.completed || r.placement) throw new Error('A corrida precisa dos sete trechos e de classificação inédita.');
  if(!Array.isArray(competitorsPlaceValues)||competitorsPlaceValues.length<1||competitorsPlaceValues.length>40) throw new Error('Valores dos concorrentes obrigatórios.');
  competitorsPlaceValues.forEach(v=>assertInt(v,'Place Value rival',0,200));
  const higher=competitorsPlaceValues.filter(v=>v>r.placeValue).length;
  const ties=competitorsPlaceValues.filter(v=>v===r.placeValue).length;
  assertInt(place,'Colocação',1,competitorsPlaceValues.length+1);
  if(place<higher+1 || place>higher+ties+1) throw new Error('Colocação incompatível com os Place Values; desempate exige adjudicação do Mestre.');
  r.placement=place; p.flags.grandPrixWon=place===1;
  return finish(w,p,id,'GRAND_PRIX_PLACEMENT',{place,placeValue:r.placeValue,competitorsPlaceValues:copy(competitorsPlaceValues)});
}
export function jewelGrandPrixStatus(world){return copy(world?.publishedAdventure?.operations?.race || null);}

/**
 * P185: deterministic receipt replay. The receipt journal is the only source of
 * truth for race/auction totals. Reject edits to derived totals, bidder state,
 * order, flags, or the contents of a round, before the UAE can expose a save.
 * P184 receipts are replayed without migration or additional phantom actions.
 */
export function validateJewelOperations(world) {
  const p=world?.publishedAdventure;
  const o=p?.operations;
  if(o==null)return true;
  if(!o||typeof o!=='object'||Array.isArray(o)||!o.receipts||typeof o.receipts!=='object'||Array.isArray(o.receipts))throw new Error('Operações da aventura adulteradas.');
  const entries=Object.entries(o.receipts);
  if(entries.length>256)throw new Error('Número de recibos excedido.');
  const validKinds=new Set(['AUCTION_INFLUENCE','AUCTION_ROUND','GRAND_PRIX_LEG','GRAND_PRIX_PLACEMENT','BANK_STEP','BANK_PREP','BANK_SLICER_HIRE','BANK_SLICE_SPEND','BANK_EVIDENCE','BANK_DROID','BANK_BOLT_BUY','BANK_TOOL_REPLACE','BANK_SETTLEMENT','JEWEL_ESCAPE_BOMB','JEWEL_ESCAPE_IMPACT','JEWEL_COMPANION_IMPACT',
    'JEWEL_ARROW7_TURRET','JEWEL_ARROW7_CHASE','JEWEL_ARROW7_CHASE_END',
    'JEWEL_AREND_LEADS','JEWEL_AREND_VISIT','JEWEL_HOLO_ENTER','JEWEL_HOLO_ROUND','JEWEL_HOLO_END','JEWEL_HOLO_EFFECT','JEWEL_HOLO_FACTION_ARRIVAL','JEWEL_LANDO_PARDON','JEWEL_AREND_OUTCOME','JEWEL_ARIS_ALLIANCE','JEWEL_AREND_PROPERTY_ENCOUNTER','JEWEL_AREND_PATROL_RESIDENCE_START','JEWEL_AREND_PATROL_RESIDENCE_CHECK','JEWEL_AREND_PATROL_RESIDENCE_END','JEWEL_AREND_PATROL_VENTURE_ALERT','JEWEL_AREND_PATROL_VENTURE_ARRIVAL','JEWEL_EP3_XP','JEWEL_BESPIN_CUSTODY','JEWEL_CUSTODY_TRANSFER','JEWEL_BESPIN_BOARD','JEWEL_BESPIN_ESCAPE','JEWEL_RESIDENCE_WING_RESPONSE','JEWEL_PURSUIT_TRAVEL_CHECK','JEWEL_PURSUIT_GROUP_ARRIVAL','JEWEL_STARPORT_SHIP_ACCESS','JEWEL_STARPORT_ROUTE','JEWEL_STARPORT_DESTRUCTION','JEWEL_STARPORT_CLOUD_ARRIVAL','JEWEL_STARPORT_CLOUD_ENGAGE','JEWEL_STARPORT_CLOUD_OUTCOME','JEWEL_STARPORT_CLOUD_CLEARED','JEWEL_HOLO_NARRATIVE_START','JEWEL_HOLO_NARRATIVE_SHIFT','JEWEL_HOLO_NARRATIVE_END']);
  const seen=new Set();
  for(const [id,r] of entries){
    receiptId(id);
    if(seen.has(id)||!r||typeof r!=='object'||Array.isArray(r)||r.id!==id||!validKinds.has(r.kind))throw new Error('Recibo de operação adulterado.');
    seen.add(id);
  }
  const same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
  const legs=entries.map(([,r])=>r).filter(r=>r.kind==='GRAND_PRIX_LEG');
  const placements=entries.map(([,r])=>r).filter(r=>r.kind==='GRAND_PRIX_PLACEMENT');
  if(!o.race&&(legs.length||placements.length))throw new Error('Recibos da corrida sem estado.');
  if(o.race){
    const r=o.race;
    if(!Array.isArray(r.legs)||r.leg!==legs.length||r.legs.length!==legs.length||legs.length>7||placements.length>1)throw new Error('Corrida adulterada.');
    let place=0,strain=0,missed=0,critical=0;
    for(let i=0;i<legs.length;i++){
      const receipt=legs[i], leg=r.legs[i];
      if(!leg||receipt.leg!==i+1||leg.leg!==i+1||receipt.name!==LEG_NAMES[i]||leg.name!==LEG_NAMES[i])throw new Error('Trechos da corrida inconsistentes.');
      for(const key of ['success','speed','placeAdjustment','systemStrainDelta','missedBuoy','criticalCollision','placeValue','systemStrain','appliedSetbacks','terrainSetbacks','hazard','hazardStrain','criticalModifier','pilotEvidence','vehicleId'])
        if(!same(receipt[key],leg[key]))throw new Error('Recibo de corrida não corresponde ao trecho.');
      if(receipt.pilotEvidence!==undefined){
        validateJewelGrandPrixPilotEvidence(receipt.pilotEvidence,i+1,i>0&&Boolean(legs[i-1].missedBuoy));
        if(receipt.pilotEvidence.roll.success!==receipt.success||receipt.missedBuoy===receipt.success||typeof receipt.vehicleId!=='string'||!receipt.vehicleId)throw new Error('Resultado do piloto adulterado.');
        if(i===3||i===4){
          if(!receipt.hazard||receipt.hazard.threats!==Math.max(0,-receipt.pilotEvidence.roll.netAdvantage)||receipt.hazard.despairs!==receipt.pilotEvidence.roll.despair)throw new Error('Perigo da corrida adulterado.');
        }else if(receipt.hazard!==undefined)throw new Error('Perigo em trecho seguro.');
      }
      if(typeof receipt.success!=='boolean'||typeof receipt.missedBuoy!=='boolean'||typeof receipt.criticalCollision!=='boolean'||(receipt.success&&receipt.missedBuoy))throw new Error('Resultado da corrida adulterado.');
      assertInt(receipt.speed,'Velocidade',0,6);assertInt(receipt.placeAdjustment,'Ajuste',-3,3);assertInt(receipt.systemStrainDelta,'Tensão',-10,10);
      place+=receipt.speed+receipt.placeAdjustment;
      const requiredSetbacks=i>0&&legs[i-1].missedBuoy?1:0;
      if(receipt.appliedSetbacks!==undefined&&receipt.appliedSetbacks!==requiredSetbacks)throw new Error('Penalidade de boia perdida adulterada.');
      if(receipt.terrainSetbacks!==undefined&&receipt.terrainSetbacks!==RACE_HAZARD_SETBACKS[i])throw new Error('Dificuldade ambiental adulterada.');
      let hazardStrain=0;
      if(receipt.hazard!==undefined){
        if(i!==3&&i!==4)throw new Error('Perigo ambiental em trecho inválido.');
        const h=receipt.hazard;
        if(!h||typeof h!=='object'||Array.isArray(h))throw new Error('Perigo ambiental adulterado.');
        assertInt(h.threats,'Ameaças',0,20);assertInt(h.despairs,'Desesperos',0,10);
        if(typeof h.criticalHit!=='boolean')throw new Error('Crítico ambiental adulterado.');
        const crit=i===3?h.despairs>0:(h.threats>=3||h.despairs>0);
        if(h.criticalHit!==crit||receipt.criticalModifier!==(crit?(i===3?-20:0):null)||receipt.hazardStrain!==(i===3&&h.threats>0?1:0)|| (crit&&!receipt.criticalCollision))throw new Error('Consequência ambiental adulterada.');
        hazardStrain=receipt.hazardStrain;
      }
      strain=Math.max(0,strain+receipt.systemStrainDelta+hazardStrain);
      missed+=Number(receipt.missedBuoy);critical+=Number(receipt.criticalCollision);
      if(receipt.placeValue!==place||receipt.systemStrain!==strain)throw new Error('Place Value ou tensão adulterados.');
    }
    if(r.placeValue!==place||r.systemStrain!==strain||r.missedBuoys!==missed||Number(r.criticalCollisions||0)!==critical||r.completed!==(legs.length===7))throw new Error('Resultado da corrida inconsistente.');
    if(Boolean(placements.length)!==Boolean(r.placement))throw new Error('Classificação da corrida adulterada.');
    if(placements.length){
      const x=placements[0];
      if(!r.completed||x.place!==r.placement||x.placeValue!==place||!Array.isArray(x.competitorsPlaceValues)||x.competitorsPlaceValues.length<1||x.competitorsPlaceValues.length>40)throw new Error('Classificação da corrida adulterada.');
      x.competitorsPlaceValues.forEach(v=>assertInt(v,'Place Value rival',0,200));
      const higher=x.competitorsPlaceValues.filter(v=>v>place).length;
      const ties=x.competitorsPlaceValues.filter(v=>v===place).length;
      if(!Number.isSafeInteger(x.place)||x.place<higher+1||x.place>higher+ties+1||p?.flags?.grandPrixWon!==(x.place===1))throw new Error('Classificação da corrida inconsistente.');
    }else if(p?.flags?.grandPrixWon!==undefined)throw new Error('Vitória da corrida sem classificação.');
  }
  const auctionReceipts=entries.map(([,r])=>r).filter(r=>r.kind==='AUCTION_ROUND'||r.kind==='AUCTION_INFLUENCE');
  if(!o.auction&&auctionReceipts.length)throw new Error('Recibos do leilão sem estado.');
  if(o.auction){
    const a=o.auction;
    if(!Array.isArray(a.roundHistory)||!Array.isArray(a.pendingInfluences)||!a.bidders||typeof a.bidders!=='object'||Array.isArray(a.bidders))throw new Error('Leilão adulterado.');
    const replay={round:0,highBidCredits:0,highBidderId:null,completed:false,
      bidders:Object.fromEntries(BIDDERS.map(([id,name,base])=>[id,{id,name,base,bidValue:base,active:true}])),
      roundHistory:[],pendingInfluences:[]};
    for(const record of auctionReceipts){
      if(record.kind==='AUCTION_INFLUENCE'){
        const bidder=replay.bidders[record.bidderId];
        if(replay.completed||!bidder||!bidder.active||record.round!==replay.round+1||typeof record.success!=='boolean'||typeof record.favorableImpression!=='boolean')throw new Error('Influência do leilão adulterada.');
        assertInt(record.delta,'Influência',0,50);
        // P184 receipts lack dice counts: retain them, but every new receipt
        // carries a replayable dice breakdown so a forged delta is detected.
        if(record.netSuccesses!==undefined||record.triumphs!==undefined){
          assertInt(record.netSuccesses,'Sucessos líquidos',0,20);
          assertInt(record.triumphs,'Triunfos',0,10);
          if(record.success!==(record.netSuccesses>0)||record.delta!==(record.success?record.netSuccesses+3*record.triumphs:0))throw new Error('Dados da rolagem do licitante adulterados.');
        }
        if((!record.success&&record.delta!==0)||(record.success&&record.delta<1))throw new Error('Resultado de influência adulterado.');
        replay.pendingInfluences.push({id:record.id,bidderId:record.bidderId,delta:record.delta,favorableImpression:record.favorableImpression,success:record.success});
      }else{
        if(replay.completed||replay.round>=10||record.round!==replay.round+1)throw new Error('Rodada do leilão adulterada.');
        for(const bidder of Object.values(replay.bidders))if(bidder.active)bidder.bidValue+=bidder.base;
        const boosted=replay.pendingInfluences.filter(x=>x.delta>0);
        for(const effect of boosted)replay.bidders[effect.bidderId].bidValue+=effect.delta;
        const contenders=Object.values(replay.bidders).filter(b=>b.active).sort((x,y)=>y.bidValue-x.bidValue||y.base-x.base);
        if(!contenders.length)throw new Error('Licitantes ausentes.');
        const leader=contenders[0];
        for(const bidder of contenders)if(leader.bidValue-bidder.bidValue>10)bidder.active=false;
        replay.round++;
        replay.highBidCredits+=20000+(boosted.length?10000:0);
        replay.highBidderId=leader.id;
        replay.completed=replay.round>=10||Object.values(replay.bidders).filter(b=>b.active).length===1;
        const expected={round:replay.round,highBidCredits:replay.highBidCredits,highBidderId:leader.id,
          remaining:Object.values(replay.bidders).filter(b=>b.active).map(b=>b.id),influenceReceipts:replay.pendingInfluences.map(x=>x.id)};
        for(const key of Object.keys(expected))if(!same(record[key],expected[key]))throw new Error('Recibo da rodada adulterado.');
        if(record.completed!==replay.completed)throw new Error('Conclusão da rodada adulterada.');
        replay.roundHistory.push(expected);replay.pendingInfluences=[];
      }
    }
    for(const key of ['round','highBidCredits','highBidderId','completed','bidders','roundHistory','pendingInfluences'])
      if(!same(a[key],replay[key]))throw new Error(`Estado do leilão inconsistente: ${key}.`);
    if(p?.flags?.auctionWinnerAdjudicated===true!==replay.completed)throw new Error('Vencedor do leilão inconsistente.');
  }else if(p?.flags?.auctionWinnerAdjudicated===true)throw new Error('Vencedor do leilão sem operação.');
  validateJewelBankState(p,world);
  return true;
}
