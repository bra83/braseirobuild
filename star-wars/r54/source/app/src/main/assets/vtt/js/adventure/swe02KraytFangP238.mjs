// SWE02 pp.7–11 (printed): Krayt Fang shipboard problems. This is an
// authoritative state machine; narrative output may not mutate its receipts.
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
  if(p?.flags?.shipPeltsJettisoned || checkKey==='findPelts')return 0;
  const name=String(characterName||'').toLowerCase();
  if(name.includes('41-vex'))return 0;
  if(name.includes('lowhhrick'))return 2;
  // The named beginner-game organics suffer one setback. Unknown PCs are
  // intentionally left to the GM, not assigned a fictitious species effect.
  return ['mathus','oskara','pash','sasha'].some(x=>name.includes(x))?1:0;
}
export function recordShipCheck(p,index,success){
  const check=SHIP_CHECKS[index];
  if(!shipCheckAvailable(p,index))throw Error('Krayt Fang: teste fora da cena, repetido ou sem pré-requisito.');
  p.shipChecks=Array.isArray(p.shipChecks)?p.shipChecks:[];
  p.shipChecks.push({key:check.key,index,success,printedPage:check.page,nodeId:p.currentNodeId});
  p.flags=p.flags||{};
  if(index===0){p.flags.shipCoursePlotted=true;p.flags.shipCourseNonOptimal=!success;}
  else if(success)p.flags[FLAG[check.key]]=true;
  if(p.flags.shipCoursePlotted&&p.flags.shipBuraFound)p.flags.shipDepartureVerified=true;
}
export function recordShipChoice(p,id){
  p.flags=p.flags||{};
  p.shipChoices=Array.isArray(p.shipChoices)?p.shipChoices:[];
  if(id==='ship-find-bura'){
    if(p.flags.shipBuraFound)throw Error('Krayt Fang: B’ura B’an já localizado.');
    p.flags.shipBuraFound=true;
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
  return true;
}
