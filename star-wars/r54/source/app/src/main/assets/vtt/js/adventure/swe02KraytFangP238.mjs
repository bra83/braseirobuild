// SWE02 printed pp.6–11: Krayt Fang shipboard sequence. This module owns the
// deterministic rules and receipts; narration/UI must call these operations.
export const SHIP_SEQUENCE_VERSION = 1;
export const SHIP_ALERTS = Object.freeze([
  Object.freeze({ id:'fuel-low', label:'Fuel Low', shape:'large-flashing-red-script', page:7 }),
  Object.freeze({ id:'tracker', label:'Crescent/fang tracker', shape:'flashing-crescent-or-fang', page:7 }),
  Object.freeze({ id:'trandoshan-popup', label:'Trandoshan software popup', shape:'intermittent-popup', page:7 }),
  Object.freeze({ id:'captive', label:'Captive reminder', shape:'flashing-cage', page:7 })
]);

// A manifest is a source reference for discoveries, not automatic ownership.
// Prices/descriptions remain leads; finding an entry never silently sells it.
export const SHIP_MANIFEST = Object.freeze({
  'number-two-hold': Object.freeze([
    { id:'broken-astromech-parts', quantity:null, page:11, note:'broken parts; a skilled mechanic could build an astromech after arduous work or sell parts for up to 2,000 credits' },
    { id:'empty-crates-number-two', quantity:null, page:11 },
    { id:'poorly-maintained-tools', quantity:null, page:11 },
    { id:'medpac', quantity:1, page:11 }, { id:'general-purpose-scanner', quantity:1, page:11 },
    { id:'electrobinoculars', quantity:1, page:11 }, { id:'trandoshan-respirator', quantity:1, page:11 },
    { id:'glow-rod-number-two', quantity:2, page:11 }, { id:'tool-kit', quantity:1, page:11 }
  ]),
  'main-cargo-hold': Object.freeze([
    { id:'empty-crates-main-hold', quantity:null, page:11 },
    { id:'wookiee-pelts', quantity:6, page:9, pageAlso:11 },
    { id:'holochess-set', quantity:1, page:11, note:'good quality; challenging solo-game AI' }
  ]),
  galley: Object.freeze([
    { id:'raw-meat-meals', quantity:2, page:11, note:'decent meals if the PCs choose an exclusively carnivorous diet' },
    { id:'ration-packs', quantity:6, page:11 }
  ]),
  'crew-quarters': Object.freeze([
    { id:'beds', quantity:3, page:11 }, { id:'wampa-skin-rug', quantity:1, page:11, valueCredits:250, note:'may fetch this price from the right buyer' },
    { id:'glow-rod-quarters', quantity:1, page:11 }, { id:'heavy-blaster-power-pack', quantity:2, page:11 },
    { id:'escape-from-mos-shuuta-credits', quantity:1000, page:11, note:'GM may place the previously promised credits here' }
  ]),
  engineering: Object.freeze([
    { id:'hand-scanner', quantity:1, page:11 },
    { id:'hypermatter-reactor-igniter', quantity:1, page:11, note:'working perfectly' },
    { id:'fuel-reserves', quantity:null, page:11, note:'low; source gives no numeric fuel amount' }
  ]),
  'number-three-hold': Object.freeze([
    { id:'binders', quantity:7, page:11 }, { id:'portable-cell-units', quantity:6, page:9 },
    { id:'bura-baan', quantity:1, page:9, note:'one cell occupied; free without a skill check' },
    { id:'marked-chitin', quantity:null, page:9, note:'other tracked cell; source does not give a piece count' }
  ])
});

// Only source-named, countable objects with an exact Core gear analogue can
// transfer to a PC sheet. Narrative cargo stays in the ship receipt until its
// handling has its own source-backed rule.
export const SHIP_CARGO_GEAR_MAPPINGS = Object.freeze({
  'medpac': Object.freeze({catalogId:'medpac',rulePage:177,label:'Medpac'}),
  'general-purpose-scanner': Object.freeze({catalogId:'multi_scanner',rulePage:175,label:'Scanner geral'}),
  'electrobinoculars': Object.freeze({catalogId:'electrobinoculars',rulePage:175,label:'Eletrobinóculos'}),
  'hand-scanner': Object.freeze({catalogId:'hand_scanner',rulePage:176,label:'Scanner manual'}),
  'trandoshan-respirator': Object.freeze({catalogId:'breath_mask',rulePage:179,label:'Respirador trandoshano'}),
  'glow-rod-number-two': Object.freeze({catalogId:'glow_rod',rulePage:181,label:'Bastão luminoso (porão 2)'}),
  'glow-rod-quarters': Object.freeze({catalogId:'glow_rod',rulePage:181,label:'Bastão luminoso (alojamento)'}),
  'tool-kit': Object.freeze({catalogId:'tool_kit',rulePage:181,label:'Kit de ferramentas'}),
  'ration-packs': Object.freeze({catalogId:'rations',rulePage:179,label:'Rações'}),
  'raw-meat-meals': Object.freeze({catalogId:null,rulePage:null,label:'Refeições de carne crua'}),
  'wookiee-pelts': Object.freeze({catalogId:null,rulePage:null,label:'Peles Wookiee'}),
  'holochess-set': Object.freeze({catalogId:null,rulePage:null,label:'Conjunto de holochess'}),
  'heavy-blaster-power-pack': Object.freeze({catalogId:null,rulePage:null,label:'Carga de blaster pesado'})
});

export const BURA_CONVERSATION_FACTS = Object.freeze({
  injury:{ printedPage:10, fact:'A Trandoshan bounty hunter burned away B’ura’s left lekku before placing him in the cell.' },
  captureStory:{ printedPage:10, fact:'B’ura organized Twi’leks to protect New Meen from Aqualish extortion; after the Aqualish disappeared in an underworld takeover, outsiders bought nearby land to spy and harass the mines; rogues captured B’ura while he traveled to Nabat.' },
  teemoTheory:{ printedPage:10, fact:'B’ura suspects Teemo’s interest in ryll mining may be changing because the Trandoshan who captured him worked for Teemo.' },
  shipHelp:{ printedPage:10, fact:'B’ura suggests the navigation computer can stop the noise, says Ryloth is in range with affordable fuel, and offers contacts who can repair and refuel the ship in exchange for a ride to Nabat.' },
  oskaraConnection:{ printedPage:10, fact:'B’ura does not know Oskara or her sister Makara; if Oskara shares her story, he agrees the developers near his mine could be part of Teemo’s operation.' },
  mineReward:{ printedPage:10, fact:'If the PCs help save the mine, B’ura says his friends will offer lucrative ryll-smuggling opportunities.' },
  drombbDetails:{ printedPage:11, referencedPrintedPage:18, fact:'If the PCs press for more information, B’ura can tell them about Angu Drombb.' }
});

const SHIP_OBSERVATIONS = Object.freeze({
  'observe-alert-icons': { page:7, once:true },
  'notice-smell-source-in-main-hold': { page:9, once:true },
  'press-cage-icon': { page:9, once:true },
  'bura-free': { page:9, once:true },
  'lowhhrick-pelt-strain': { page:9, once:true },
  'complete-hyperspace-transit': { page:11, once:true },
  'keep-marked-chitin': { page:10, once:true }
});

function ensureShipSequence(p) {
  p.shipSequence ||= {
    version:SHIP_SEQUENCE_VERSION,
    observations:[],
    discoveredRooms:[],
    cargoReceipts:[],
    cargoTransfers:[],
    chitinKnowledge:null,
    lowhhrickStrainReceipt:null,
    transit:null,
    keptChitin:false,
    source:{ productCode:'SWE02', printedPages:[6,7,8,9,10,11] }
  };
  return p.shipSequence;
}

export function shipIssueState(p) {
  const f=p?.flags||{};
  return {
    alerts: SHIP_ALERTS.map(x=>({ ...x, status:f.shipAlertIconsObserved?'observed':'unresolved' })),
    smell: { unresolved:!f.shipPeltsJettisoned, mainHoldSourceKnown:!!p?.shipSequence?.observations?.some(x=>x.id==='notice-smell-source-in-main-hold') },
    noise: { klaxonActive:!f.shipKlaxonOff, musicActive:!f.shipMusicOff, concentrationPenalty:!f.shipKlaxonOff&&!f.shipMusicOff },
    tracker: { located:!!f.shipTrackerLocated, slowed:!!f.shipTrackerSlowed, removed:!!f.shipTrackerRemoved },
    fuel: { alertActive:true, quantity:null, source:{productCode:'SWE02',printedPage:7} }
  };
}

export function recordShipObservation(p,id,details={}) {
  if(!p||p.id!=='long-arm-of-the-hutt')throw Error('Krayt Fang: aventura inválida.');
  const def=SHIP_OBSERVATIONS[id];
  if(!def)throw Error('Krayt Fang: observação não reconhecida.');
  if(id==='complete-hyperspace-transit'&&!p.flags?.shipDepartureVerified)throw Error('Krayt Fang: a viagem exige rota e B’ura registrados.');
  if(id==='keep-marked-chitin'&&!p.flags?.shipChitinIdentified)throw Error('Krayt Fang: a carapaça marcada ainda não foi examinada.');
  if(id==='lowhhrick-pelt-strain'&&!p.flags?.shipPeltsFound)throw Error('Krayt Fang: não há peles descobertas.');
  if(id==='bura-free'&&!p.flags?.shipBuraFound)throw Error('Krayt Fang: localize B’ura antes de libertá-lo.');
  const s=ensureShipSequence(p);
  if(def.once&&s.observations.some(x=>x.id===id))throw Error('Krayt Fang: observação já registrada.');
  const receipt={id,printedPage:def.page,source:{productCode:'SWE02',printedPage:def.page},details:structuredClone(details||{})};
  s.observations.push(receipt);
  if(id==='observe-alert-icons')p.flags.shipAlertIconsObserved=true;
  if(id==='notice-smell-source-in-main-hold')p.flags.shipSmellSourceLocated=true;
  if(id==='press-cage-icon'){
    p.flags.shipCageReminderRead=true;
    receipt.details.oskaraRecognition=details.oskaraPresent===true;
    receipt.details.buraLocated=true;
    p.flags.shipBuraIdentified=true;
  }
  if(id==='bura-free'){
    p.flags.shipBuraFreed=true;
  }
  if(id==='lowhhrick-pelt-strain'){
    // This is an optional GM-directed consequence from the source, not automatic.
    s.lowhhrickStrainReceipt={amount:2,applied:details.applied===true,characterId:details.characterId||null,source:{productCode:'SWE02',printedPage:9}};
  }
  if(id==='complete-hyperspace-transit'){
    const routeFailed=p.flags?.shipCourseNonOptimal===true;
    s.transit={elapsedHours:6,elapsedHoursIsMinimum:true,
      durationStatus:routeFailed?'LONGER_THAN_OPTIMAL_UNQUANTIFIED':'A_LITTLE_MORE_THAN_SIX_HOURS',
      extraDurationStatus:routeFailed?'GM_TIME_ADJUDICATION_REQUIRED':'NOT_REQUIRED',
      uneventful:true,restAndRecuperationAvailable:true,completed:true,source:{productCode:'SWE02',printedPage:11}};
    p.flags.shipHyperspaceTransitComplete=true;
  }
  if(id==='keep-marked-chitin'){
    s.keptChitin=true;
    p.flags.shipMarkedChitinKept=true;
  }
  return receipt;
}

export function adjudicateShipTransitExtraDuration(p,ticks){
  if(!Number.isSafeInteger(ticks)||ticks<1||ticks>100000)
    throw Error('Krayt Fang: informe pelo menos um bloco adicional válido de seis horas.');
  const transit=p?.shipSequence?.transit;
  if(!p||p.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE'||p.flags?.shipCourseNonOptimal!==true||
    p.currentNodeId!=='rylos-arrival-decision'||!transit?.completed||transit.extraDurationStatus!=='GM_TIME_ADJUDICATION_REQUIRED')
    throw Error('Krayt Fang: não há atraso de rota aguardando adjudicação do Mestre.');
  transit.extraDurationStatus='GM_ADJUDICATED';
  transit.extraDurationTicks=ticks;
  transit.extraDurationSource={productCode:'SWE02',printedPage:8,authority:'GM_ADJUDICATION'};
  return structuredClone(transit);
}

export function recordShipRoomDiscovery(p,roomId) {
  if(!p||p.id!=='long-arm-of-the-hutt'||!SHIP_MANIFEST[roomId])throw Error('Krayt Fang: compartimento desconhecido.');
  if(p.currentNodeId!=='krayt-fang-logs')throw Error('Krayt Fang: inspeção de compartimento fora da sequência a bordo.');
  const s=ensureShipSequence(p);
  if(s.discoveredRooms.includes(roomId))throw Error('Krayt Fang: compartimento já registrado.');
  const contents=SHIP_MANIFEST[roomId].map(x=>({...x,source:{productCode:'SWE02',printedPage:x.page}}));
  s.discoveredRooms.push(roomId);
  s.cargoReceipts.push({roomId,items:contents,ownership:'DISCOVERED_NOT_TRANSFERRED',source:{productCode:'SWE02',printedPage:11}});
  return structuredClone(contents);
}

export function shipCargoTransferOptions(p){
  const s=p?.shipSequence;
  if(p?.id!=='long-arm-of-the-hutt'||!Array.isArray(s?.cargoReceipts))return [];
  return s.cargoReceipts.flatMap(receipt=>receipt.items.flatMap(item=>{
    const mapping=SHIP_CARGO_GEAR_MAPPINGS[item.id];
    if(!mapping?.catalogId||!Number.isSafeInteger(item.quantity)||item.quantity<1)return [];
    const transferred=(s.cargoTransfers??[]).filter(x=>x.roomId===receipt.roomId&&x.itemId===item.id).reduce((sum,x)=>sum+x.quantity,0);
    const available=item.quantity-transferred;
    return available>0?[{roomId:receipt.roomId,itemId:item.id,catalogId:mapping.catalogId,
      availableQuantity:available,printedPage:item.source?.printedPage??item.page,
      rulePage:mapping.rulePage,label:mapping.label}]:[];
  }));
}

export function recordShipCargoTransfer(p,{roomId,itemId,quantity,characterId}={}){
  if(!p||p.id!=='long-arm-of-the-hutt'||p.status!=='ACTIVE')throw Error('Krayt Fang: aventura não está ativa.');
  const transferQuantity=Number(quantity),pcId=String(characterId??'').trim();
  if(!/^[A-Za-z0-9:_-]{1,120}$/.test(pcId))throw Error('Krayt Fang: selecione um PC registrado para receber o item.');
  if(!Number.isSafeInteger(transferQuantity)||transferQuantity<1)throw Error('Krayt Fang: quantidade transferida deve ser um inteiro positivo.');
  const receipt=p.shipSequence?.cargoReceipts?.find(x=>x.roomId===String(roomId));
  const item=receipt?.items?.find(x=>x.id===String(itemId));
  const mapping=SHIP_CARGO_GEAR_MAPPINGS[String(itemId)];
  if(!receipt||!item||!mapping?.catalogId)throw Error('Krayt Fang: item não foi descoberto ou não tem mapeamento de equipamento validado.');
  if(!Number.isSafeInteger(item.quantity)||item.quantity<1)throw Error('Krayt Fang: fonte não define uma quantidade transferível para este item.');
  const s=ensureShipSequence(p);s.cargoTransfers=Array.isArray(s.cargoTransfers)?s.cargoTransfers:[];
  const alreadyTransferred=s.cargoTransfers.filter(x=>x.roomId===receipt.roomId&&x.itemId===item.id).reduce((sum,x)=>sum+x.quantity,0);
  if(transferQuantity>item.quantity-alreadyTransferred)throw Error('Krayt Fang: quantidade excede o item ainda disponível no compartimento.');
  const transfer={roomId:receipt.roomId,itemId:item.id,quantity:transferQuantity,characterId:pcId,
    catalogId:mapping.catalogId,source:{productCode:'SWE02',printedPage:item.source?.printedPage??item.page},
    rulesSource:{book:'Edge of the Empire Core Rulebook',printedPage:mapping.rulePage},
    ownership:'PC_INVENTORY',status:'TRANSFERRED'};
  s.cargoTransfers.push(transfer);
  return structuredClone(transfer);
}

export function recordBuraConversation(p,{topics=[],oskaraPresent=false,oskaraSharedHistory=false,oskaraRaisedCorruption=false}={}){
  if(!p||p.id!=='long-arm-of-the-hutt'||!p.flags?.shipBuraFound)throw Error('Krayt Fang: B’ura ainda não foi encontrado.');
  if(!Array.isArray(topics)||topics.some(x=>!['injury','captureStory','teemoTheory','shipHelp','mineReward','drombbDetails'].includes(x)))throw Error('Krayt Fang: tópico de conversa inválido.');
  const s=ensureShipSequence(p);
  if(s.buraConversation)throw Error('Krayt Fang: conversa com B’ura já registrada.');
  const facts=topics.map(id=>({id,...BURA_CONVERSATION_FACTS[id],source:{productCode:'SWE02',printedPage:BURA_CONVERSATION_FACTS[id].printedPage}}));
  if(oskaraPresent)facts.push({id:'oskaraRecognition',printedPage:10,fact:'B’ura does not recognize Oskara but is pleased to meet another Twi’lek.',source:{productCode:'SWE02',printedPage:10}});
  if(oskaraPresent&&oskaraSharedHistory)facts.push({id:'oskaraConnection',...BURA_CONVERSATION_FACTS.oskaraConnection,source:{productCode:'SWE02',printedPage:10}});
  if(oskaraPresent&&oskaraRaisedCorruption)facts.push({id:'organizationCorruption',printedPage:10,fact:'B’ura admits the hastily formed movement may include people seeking personal gain, while he still believes its cause is righteous.',source:{productCode:'SWE02',printedPage:10}});
  s.buraConversation={facts,oskaraPresent:Boolean(oskaraPresent),oskaraSharedHistory:Boolean(oskaraSharedHistory),oskaraRaisedCorruption:Boolean(oskaraRaisedCorruption),source:{productCode:'SWE02',printedPages:[10,11]}};
  return structuredClone(s.buraConversation);
}

// SWE02 pp.7–8. Odor penalties apply to ordinary activities; the search for
// the smell source is exempt. Aid aimed at Lowhhrick adds two more setbacks.
export const SHIP_CHECKS = Object.freeze([
  { key:'course',skill:'Astrogação',difficulty:1,page:8,purpose:'Traçar rota a Ryloth; falha produz rota não ótima e interceptação' },
  { key:'klaxon',skill:'Computadores',difficulty:1,page:8,purpose:'Desligar o alarme de combustível' },
  { key:'music',skill:'Computadores',difficulty:2,page:8,purpose:'Desligar a ópera gamorreana' },
  { key:'translate',skill:'Computadores',difficulty:1,page:8,purpose:'Traduzir interface trandoshana do rastreador' },
  { key:'locateTracker',skill:'Computadores',difficulty:3,page:8,purpose:'Localizar o transponder no casco' },
  { key:'slowTracker',skill:'Computadores',difficulty:1,page:8,purpose:'Reduzir a emissão do rastreador a uma vez por hora' },
  { key:'findPelts',skill:'Percepção',difficulty:2,page:9,purpose:'Localizar seis peles Wookiee no compartimento oculto' },
  { key:'jettisonPelts',skill:'Computadores',difficulty:1,page:9,purpose:'Ejetar as peles do compartimento de carga' },
  { key:'removeTracker',skill:'Mecânica',difficulty:2,page:8,purpose:'Retirar transponder no solo, nunca durante o voo' },
  { key:'identifyChitin',skill:'Conhecimento',difficulty:3,page:9,purpose:'Analisar quitina encontrada no porão três' },
  { key:'inspectDroidParts',skill:'Mecânica',difficulty:3,page:11,purpose:'Identificar peças de astromecânico no porão dois' }
]);
const FLAG = Object.freeze({course:'shipCoursePlotted',klaxon:'shipKlaxonOff',music:'shipMusicOff',translate:'shipTrandoshanTranslated',locateTracker:'shipTrackerLocated',slowTracker:'shipTrackerSlowed',findPelts:'shipPeltsFound',jettisonPelts:'shipPeltsJettisoned',removeTracker:'shipTrackerRemoved',identifyChitin:'shipChitinIdentified',inspectDroidParts:'shipDroidPartsIdentified'});
export const shipFlag = key => FLAG[key];
export function shipCheckAvailable(p,index){
  const f=p?.flags||{};
  if(!Number.isInteger(index)||!SHIP_CHECKS[index])return false;
  if(index===7&&!f.shipPeltsFound)return false;
  if(index===8&&p.currentNodeId!=='krayt-fang-ground-inspection')return false;
  if(index!==8&&p.currentNodeId!=='krayt-fang-logs')return false;
  if(index===5&&!f.shipTrandoshanTranslated&&!f.shipTrackerLocated)return false;
  return !f[FLAG[SHIP_CHECKS[index].key]];
}
export function shipNoisePool(p,base){
  const f=p?.flags||{};
  // SWE02 p.7: both klaxon and music operating upgrades one difficulty die.
  const both=!f.shipKlaxonOff&&!f.shipMusicOff;
  return {difficulty:base-(both?1:0),challenge:both?1:0,setback:0};
}
export function shipSmellSetback(p,characterName,checkKey){
  if(p?.flags?.shipPeltsJettisoned || ['findPelts','locateSmell','investigateSmellSource'].includes(checkKey))return 0;
  const name=String(characterName||'').toLowerCase();
  if(name.includes('41-vex'))return 0;
  if(name.includes('lowhhrick'))return 2;
  // The named beginner-game organics suffer one setback. Unknown PCs are
  // intentionally left to the GM, not assigned a fictitious species effect.
  return ['mathus','oskara','pash','sasha'].some(x=>name.includes(x))?1:0;
}
// Core helper for any shipboard task, including medical aid that is not one of
// the eleven fixed checks. The source's two extra setbacks for aiding
// Lowhhrick stack with the acting character's own smell penalty.
export function shipTaskPool(p,base,{characterName='',task='generic',aidingLowhhrick=false,locatingSmell=false,concentration=true,announcedSearch=false}={}){
  const noise=shipNoisePool(p,base);
  return {
    difficulty:concentration?noise.difficulty:base,
    challenge:concentration?noise.challenge:0,
    setback:shipSmellSetback(p,characterName,locatingSmell?'locateSmell':task)+(aidingLowhhrick?2:0),
    boost:task==='findPelts'&&announcedSearch?1:0
  };
}
export function shipCheckPool(p,index,{characterName='',announcedSearch=false,aidingLowhhrick=false}={}){
  const check=SHIP_CHECKS[index];
  if(!check)throw Error('Krayt Fang: operação desconhecida.');
  return shipTaskPool(p,check.difficulty,{characterName,task:check.key,announcedSearch,aidingLowhhrick,concentration:true});
}
export function recordShipCheck(p,index,success){
  const check=SHIP_CHECKS[index];
  if(!shipCheckAvailable(p,index))throw Error('Krayt Fang: teste fora da cena, repetido ou sem pré-requisito.');
  const result=typeof success==='boolean'?{success}:success;
  if(!result||typeof result.success!=='boolean')throw Error('Krayt Fang: resultado de teste inválido.');
  p.shipChecks=Array.isArray(p.shipChecks)?p.shipChecks:[];
  const receipt={key:check.key,index,success:result.success,printedPage:check.page,nodeId:p.currentNodeId};
  for(const key of ['netSuccess','netFailure','netAdvantage','netThreat'])if(Number.isFinite(result[key]))receipt[key]=Number(result[key]);
  if(typeof result.triumph==='boolean'||Number.isFinite(Number(result.triumph)))receipt.triumph=typeof result.triumph==='boolean'?result.triumph:Number(result.triumph)>0;
  p.shipChecks.push(receipt);
  p.flags=p.flags||{};
  if(index===0){p.flags.shipCoursePlotted=true;p.flags.shipCourseNonOptimal=!result.success;}
  else if(result.success)p.flags[FLAG[check.key]]=true;
  if(index===6&&result.success){
    const s=ensureShipSequence(p);
    if(!s.cargoReceipts.some(x=>x.roomId==='main-cargo-hold'))s.cargoReceipts.push({roomId:'main-cargo-hold',items:[{id:'wookiee-pelts',quantity:6,source:{productCode:'SWE02',printedPage:9}}],ownership:'DISCOVERED_NOT_TRANSFERRED',source:{productCode:'SWE02',printedPage:9}});
  }
  if(index===9&&result.success){
    p.shipChitinKnowledge={
      intelligentInsectileSpecies:Number.isFinite(result.netSuccess)?result.netSuccess>=1:null,
      mayHaveShedShell:Number.isFinite(result.netSuccess)?result.netSuccess>=2:null,
      redDustProbablyGeonosis:Number.isFinite(result.netAdvantage)?result.netAdvantage>=1:null,
      clanOrOwnershipMarkings:result.triumph===true,
      exactOriginKnown:false,
      source:{productCode:'SWE02',printedPage:9}
    };
  }
  if(index===10&&result.success)p.shipDroidPartsAssessment={
    skilledMechanicCouldBuildAstromech:true,
    processArduous:true,
    immediatelyAvailable:false,
    possibleSaleValueCredits:{maximum:2000,guaranteed:false},
    source:{productCode:'SWE02',printedPage:11}
  };
  if(index===8){
    if(Number.isFinite(result.netThreat)&&result.netThreat>0)p.shipTrackerRemovalHullDamage={
      status:'GM_HULL_DAMAGE_ADJUDICATION_REQUIRED',threats:Number(result.netThreat),hullTraumaAmount:null,
      source:{productCode:'SWE02',printedPage:8}
    };
    if(Number.isFinite(result.netFailure)&&result.netFailure>0)p.flags.shipTrackerRemovalFailedToLocate=true;
  }
  if(p.flags.shipCoursePlotted&&p.flags.shipBuraFound)p.flags.shipDepartureVerified=true;
}
export function recordShipChoice(p,id){
  p.flags=p.flags||{};
  p.shipChoices=Array.isArray(p.shipChoices)?p.shipChoices:[];
  if(id==='ship-find-bura'){
    if(p.flags.shipBuraFound)throw Error('Krayt Fang: B’ura B’an já localizado.');
    p.flags.shipBuraFound=true;
    p.flags.shipBuraFreed=true;
    const s=ensureShipSequence(p);
    if(!s.observations.some(x=>x.id==='bura-free'))s.observations.push({id:'bura-free',printedPage:9,source:{productCode:'SWE02',printedPage:9},details:{freeWithoutCheck:true}});
  }else if(id==='ship-depart-verified'){
    if(!p.flags.shipCoursePlotted||!p.flags.shipBuraFound)throw Error('Krayt Fang: falta traçar a rota ou encontrar B’ura B’an.');
    p.flags.shipDepartureVerified=true;
    p.flags.shipThwheekInterceptionRequired=Boolean(p.flags.shipCourseNonOptimal||!p.flags.shipTrackerSlowed);
  }else if(id==='ship-legacy-landing'){
    p.flags.shipSourceUnverified=true;
  }else return;
  p.shipChoices.push({choiceId:id,nodeId:p.currentNodeId});
}
export function validateShip(p){
  if(!p || p.id!=='long-arm-of-the-hutt')return true;
  const checks=p.shipChecks||[], choices=p.shipChoices||[], flags=p.flags||{};
  if(!Array.isArray(checks)||!Array.isArray(choices))throw Error('Krayt Fang: recibos inválidos.');
  const success=new Set(),course=checks.find(x=>x.key==='course');
  const completed=new Set();
  for(const x of checks){
    const d=SHIP_CHECKS[x.index];
    if(!d||x.key!==d.key||x.printedPage!==d.page||x.nodeId!==(x.index===8?'krayt-fang-ground-inspection':'krayt-fang-logs')||typeof x.success!=='boolean')throw Error('Krayt Fang: recibo de teste inválido.');
    if(completed.has(x.key))throw Error('Krayt Fang: teste repetido após sucesso ou navegação concluída.');
    if(x.success)success.add(x.key);
    if(x.success||x.key==='course')completed.add(x.key);
  }
  for(const [key,flag] of Object.entries(FLAG)){
    if(key==='course')continue;
    if(Boolean(flags[flag])!==success.has(key))throw Error(`Krayt Fang: ${flag} sem recibo de sucesso ou com recibo omitido.`);
  }
  if(Boolean(flags.shipCoursePlotted)!==Boolean(course))throw Error('Krayt Fang: rota sem teste de Astrogação ou recibo ocultado.');
  if(Boolean(flags.shipCourseNonOptimal)!==Boolean(course&&course.success===false))throw Error('Krayt Fang: rota não ótima sem falha documentada ou falha ocultada.');
  if(flags.shipBuraFound===true&&!choices.some(x=>x.choiceId==='ship-find-bura'&&x.nodeId==='krayt-fang-logs'))throw Error('Krayt Fang: B’ura sem descoberta documentada.');
  if(flags.shipDepartureVerified===true&&(!course||!flags.shipBuraFound))throw Error('Krayt Fang: partida certificada sem os dois pré-requisitos.');
  if(flags.shipThwheekInterceptionRequired!==undefined){
    if(!choices.some(x=>x.choiceId==='ship-depart-verified'))throw Error('Krayt Fang: interceptação sem partida verificada.');
    if(flags.shipThwheekInterceptionRequired!==Boolean(flags.shipCourseNonOptimal||!flags.shipTrackerSlowed))throw Error('Krayt Fang: interceptação adulterada.');
  }
  if(p.shipChitinKnowledge){
    const k=p.shipChitinKnowledge;
    if(!flags.shipChitinIdentified||k.source?.productCode!=='SWE02'||k.source?.printedPage!==9||k.exactOriginKnown!==false)throw Error('Krayt Fang: resultado de Conhecimento inválido.');
  }
  if(p.shipDroidPartsAssessment&&(p.shipDroidPartsAssessment.source?.printedPage!==11||p.shipDroidPartsAssessment.possibleSaleValueCredits?.maximum!==2000||p.shipDroidPartsAssessment.possibleSaleValueCredits?.guaranteed!==false))throw Error('Krayt Fang: avaliação das peças de astromecânico inválida.');
  if(p.shipSequence?.buraConversation){
    const c=p.shipSequence.buraConversation;
    if(!flags.shipBuraFound||c.source?.productCode!=='SWE02'||!Array.isArray(c.facts)||c.facts.some(x=>x.source?.productCode!=='SWE02'||!Number.isInteger(x.printedPage)))throw Error('Krayt Fang: conversa com B’ura inválida.');
    if(c.facts.some(x=>x.id==='oskaraConnection')&&(!c.oskaraPresent||!c.oskaraSharedHistory))throw Error('Krayt Fang: ligação de Oskara sem pré-requisito.');
    if(c.facts.some(x=>x.id==='organizationCorruption')&&(!c.oskaraPresent||!c.oskaraRaisedCorruption))throw Error('Krayt Fang: fala sobre corrupção sem pré-requisito.');
  }
  if(p.shipTrackerRemovalHullDamage&&(p.shipTrackerRemovalHullDamage.source?.printedPage!==8||p.shipTrackerRemovalHullDamage.status!=='GM_HULL_DAMAGE_ADJUDICATION_REQUIRED'||p.shipTrackerRemovalHullDamage.hullTraumaAmount!==null))throw Error('Krayt Fang: efeito de Ameaça no casco inválido.');
  if(p.shipTrackerRemovalFailedToLocate&&(!checks.some(x=>x.key==='removeTracker'&&Number(x.netFailure)>0)||flags.shipTrackerRemoved))throw Error('Krayt Fang: falha de localização sem dado de Falha ou com transponder removido.');
  if(p.shipSequence!==undefined){
    const s=p.shipSequence;
    if(s?.version!==SHIP_SEQUENCE_VERSION||!Array.isArray(s.observations)||!Array.isArray(s.discoveredRooms)||!Array.isArray(s.cargoReceipts))throw Error('Krayt Fang: recibos da sequência de bordo inválidos.');
    const ids=new Set();
    for(const x of s.observations){
      const d=SHIP_OBSERVATIONS[x?.id];
      if(!d||x.printedPage!==d.page||x.source?.productCode!=='SWE02'||x.source?.printedPage!==d.page||ids.has(x.id))throw Error('Krayt Fang: recibo de observação inválido.');
      ids.add(x.id);
    }
    for(const room of s.discoveredRooms)if(!SHIP_MANIFEST[room])throw Error('Krayt Fang: descoberta de compartimento inválida.');
    for(const r of s.cargoReceipts)if(!SHIP_MANIFEST[r.roomId]||r.ownership!=='DISCOVERED_NOT_TRANSFERRED'||!Array.isArray(r.items))throw Error('Krayt Fang: recibo de carga inválido.');
    if(s.cargoTransfers!==undefined&&!Array.isArray(s.cargoTransfers))throw Error('Krayt Fang: histórico de transferência de carga inválido.');
    const moved=new Map();
    for(const transfer of s.cargoTransfers??[]){
      const manifestItem=SHIP_MANIFEST[transfer?.roomId]?.find(x=>x.id===transfer?.itemId);
      const mapping=SHIP_CARGO_GEAR_MAPPINGS[transfer?.itemId];
      if(!manifestItem||!mapping?.catalogId||!s.discoveredRooms.includes(transfer.roomId)||!Number.isSafeInteger(manifestItem.quantity)||
        !Number.isSafeInteger(transfer.quantity)||transfer.quantity<1||transfer.catalogId!==mapping.catalogId||
        !/^[A-Za-z0-9:_-]{1,120}$/.test(String(transfer.characterId??''))||
        transfer.source?.productCode!=='SWE02'||transfer.source?.printedPage!==(manifestItem.page)||
        transfer.rulesSource?.book!=='Edge of the Empire Core Rulebook'||transfer.rulesSource?.printedPage!==mapping.rulePage||
        transfer.ownership!=='PC_INVENTORY'||transfer.status!=='TRANSFERRED')
        throw Error('Krayt Fang: recibo de transferência de carga inválido.');
      const key=`${transfer.roomId}:${transfer.itemId}`,sum=(moved.get(key)||0)+transfer.quantity;
      if(sum>manifestItem.quantity)throw Error('Krayt Fang: quantidade transferida excede a carga de origem.');
      moved.set(key,sum);
    }
    if(Boolean(flags.shipAlertIconsObserved)!==ids.has('observe-alert-icons'))throw Error('Krayt Fang: alertas sem recibo de observação.');
    if(Boolean(flags.shipSmellSourceLocated)!==ids.has('notice-smell-source-in-main-hold'))throw Error('Krayt Fang: fonte do odor sem recibo.');
    if(Boolean(flags.shipCageReminderRead)!==ids.has('press-cage-icon'))throw Error('Krayt Fang: alerta da cela sem recibo.');
    if(Boolean(flags.shipBuraFreed)!==ids.has('bura-free'))throw Error('Krayt Fang: libertação de B’ura sem recibo.');
    if(Boolean(flags.shipHyperspaceTransitComplete)!==ids.has('complete-hyperspace-transit'))throw Error('Krayt Fang: viagem sem recibo.');
    if(Boolean(flags.shipMarkedChitinKept)!==ids.has('keep-marked-chitin'))throw Error('Krayt Fang: carapaça preservada sem recibo.');
    if(s.transit&&(!ids.has('complete-hyperspace-transit')||s.transit.elapsedHours!==6||s.transit.uneventful!==true||
      (s.transit.elapsedHoursIsMinimum!==undefined&&s.transit.elapsedHoursIsMinimum!==true)||
      (s.transit.durationStatus!==undefined&&!['A_LITTLE_MORE_THAN_SIX_HOURS','LONGER_THAN_OPTIMAL_UNQUANTIFIED'].includes(s.transit.durationStatus))||
      (s.transit.extraDurationStatus!==undefined&&!['NOT_REQUIRED','GM_TIME_ADJUDICATION_REQUIRED','GM_ADJUDICATED'].includes(s.transit.extraDurationStatus))||
      (s.transit.durationStatus!==undefined&&s.transit.durationStatus!==(flags.shipCourseNonOptimal?'LONGER_THAN_OPTIMAL_UNQUANTIFIED':'A_LITTLE_MORE_THAN_SIX_HOURS'))||
      (s.transit.extraDurationStatus!==undefined&&((flags.shipCourseNonOptimal&& !['GM_TIME_ADJUDICATION_REQUIRED','GM_ADJUDICATED'].includes(s.transit.extraDurationStatus))||(!flags.shipCourseNonOptimal&&s.transit.extraDurationStatus!=='NOT_REQUIRED')))||
      (s.transit.extraDurationStatus==='GM_ADJUDICATED'&&(!Number.isSafeInteger(s.transit.extraDurationTicks)||s.transit.extraDurationTicks<1||s.transit.extraDurationTicks>100000))||
      (s.transit.extraDurationStatus!=='GM_ADJUDICATED'&&s.transit.extraDurationTicks!==undefined)||
      (s.transit.extraDurationStatus==='GM_ADJUDICATED'&&s.transit.extraDurationSource?.authority!=='GM_ADJUDICATION')))
      throw Error('Krayt Fang: recibo de viagem contraditório.');
    if(s.lowhhrickStrainReceipt&&(s.lowhhrickStrainReceipt.amount!==2||s.lowhhrickStrainReceipt.source?.printedPage!==9))throw Error('Krayt Fang: consequência de Lowhhrick inválida.');
    if(Boolean(s.lowhhrickStrainReceipt)!==ids.has('lowhhrick-pelt-strain'))throw Error('Krayt Fang: recibo de strain de Lowhhrick inconsistente.');
  }
  return true;
}
