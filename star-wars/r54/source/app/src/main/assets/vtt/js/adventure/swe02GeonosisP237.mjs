// SWE02 official printed pp.7, 23, 30, 34, 36-37. Deterministic only.
export const FACTS=Object.freeze({
 'sivor-shell':['shell','krayt-fang-logs',10],
 'drombb-messages':['messages','drombb-cantina',23],
 'piddock-kubaz':['kubaz','piddock-party',34],
 'piddock-b1':['b1','piddock-party',34],
 'piddock-identify-shell':['identifiedShell','piddock-party',30]
});
export const ARGUMENTS=Object.freeze({
 'dimmock-anti-empire':['antiEmpire',1],
 'dimmock-loyal-empire':['loyalEmpire',-1],
 'dimmock-rude':['rude',-1],
 'dimmock-kubaz':['kubaz',1,'kubaz'],
 'dimmock-b1':['b1',1,'b1'],
 'dimmock-shell':['shell',1,'identifiedShell'],
 'dimmock-sivor':['sivorKilled',1,'messages'],
 'dimmock-messages':['messages',1,'messages']
});
export const guardCount=score=>score>=6?0:score>=3?3:5;
export function scoreDimmock(p){
 const h=p.checkHistory||[];
 return (h.some(x=>x.nodeId==='dimmock-official-audience'&&x.checkIndex===0&&x.success)?1:0)
  -(h.some(x=>x.nodeId==='dimmock-official-audience'&&x.checkIndex===1&&x.success===false)?2:0)
  +(p.geonosisArguments||[]).reduce((n,x)=>n+(ARGUMENTS[x.choiceId]?.[1]||0),0);
}
export function validateGeonosis(p){
 if(!p)return true;
 const evidence=p.geonosisEvidence||[],argumentsUsed=p.geonosisArguments||[];
 const h=p.checkHistory||[];
 for(const [flag,node] of [['geoPiddockCharmPassed','piddock-party'],['geoDrombbCheckPassed','drombb-cantina']])
  if(p.flags?.[flag]&&!h.some(x=>x.nodeId===node&&x.checkIndex===0&&x.success===true&&x.source?.productCode==='SWE02'))throw Error('SWE02 Geonosis: resultado de perícia sem recibo.');
 const keys=new Set();
 for(const x of evidence){
  const d=FACTS[x.choiceId];
  const legacyShellPage=x.choiceId==='sivor-shell'&&x.page===7;
  if(!d||x.key!==d[0]||x.nodeId!==d[1]||(x.page!==d[2]&&!legacyShellPage)||keys.has(x.key))throw Error('SWE02 Geonosis: recibo de prova inválido ou duplicado.');
  if(x.key==='messages'&&p.flags?.drombbThreatNeutralized!==true)throw Error('SWE02 Geonosis: mensagens sem Drombb neutralizado.');
  keys.add(x.key);
 }
 for(const key of ['shell','messages','kubaz','b1','identifiedShell']){
  if(Boolean(p.flags?.[`geo_${key}`])!==keys.has(key))throw Error('SWE02 Geonosis: flag sem prova de origem.');
 }
 const used=new Set();
 for(const x of argumentsUsed){
  const d=ARGUMENTS[x.choiceId];
  if(!d||x.key!==d[0]||x.nodeId!=='dimmock-official-audience'||used.has(x.key)||d[2]&&!keys.has(d[2]))throw Error('SWE02 Dimmock: argumento inválido ou sem prova.');
  used.add(x.key);
 }
 if(used.has('antiEmpire')&&used.has('loyalEmpire'))throw Error('SWE02 Dimmock: declarações contraditórias.');
 if(p.flags?.geoFinalized&&(p.flags.geoScore!==scoreDimmock(p)||p.flags.geoGuards!==guardCount(scoreDimmock(p))))throw Error('SWE02 Dimmock: pontuação adulterada.');
 if(p.flags?.geoGantryCleared){
  const r=(p.encounterHistory||[]).find(x=>x.encounterId==='laoth-gantry-aa7');
  if(!r||!['adversaries_defeated','escaped','player_incapacitated'].includes(r.outcome)||
     r.combatProof?.groupCount!==r.combatProof?.enemyIds?.length||new Set(r.combatProof.enemyIds).size!==r.combatProof.enemyIds.length)
    throw Error('SWE02 Gantry: embarque sem recibo de combate.');
 }
 return true;
}
export function recordGeonosisChoice(p,choiceId,nodeId){
 const fact=FACTS[choiceId],arg=ARGUMENTS[choiceId];
 if(fact){
  if(nodeId!==fact[1])throw Error('SWE02 Geonosis: cena incorreta.');
  if(choiceId==='sivor-shell'&&(!p.flags?.shipMarkedChitinKept||!p.shipChitinKnowledge||p.shipChitinKnowledge.source?.printedPage!==9))throw Error('SWE02 pp.9–10: pista de Sivor exige identificar e conservar a carapaça marcada.');
  p.geonosisEvidence=p.geonosisEvidence||[];
  if(p.geonosisEvidence.some(x=>x.key===fact[0]))throw Error('SWE02 Geonosis: prova já registrada.');
  p.geonosisEvidence.push({choiceId,key:fact[0],nodeId,page:fact[2]});
  p.flags[`geo_${fact[0]}`]=true;
 }
 if(arg){
  p.geonosisArguments=p.geonosisArguments||[];
  if(nodeId!=='dimmock-official-audience'||p.geonosisArguments.some(x=>x.key===arg[0]))throw Error('SWE02 Dimmock: argumento duplicado.');
  if(arg[2]&&!p.geonosisEvidence?.some(x=>x.key===arg[2]))throw Error('SWE02 Dimmock: prova não adquirida.');
  if(arg[0]==='antiEmpire'&&p.geonosisArguments.some(x=>x.key==='loyalEmpire')||arg[0]==='loyalEmpire'&&p.geonosisArguments.some(x=>x.key==='antiEmpire'))throw Error('SWE02 Dimmock: declarações contraditórias.');
  p.geonosisArguments.push({choiceId,key:arg[0],nodeId,page:36});
 }
 if(choiceId==='dimmock-finish'){
  p.flags.geoFinalized=true;p.flags.geoScore=scoreDimmock(p);p.flags.geoGuards=guardCount(p.flags.geoScore);
 }
 validateGeonosis(p);
}
