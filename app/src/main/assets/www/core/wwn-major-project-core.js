export const WWN_MAJOR_PROJECT_SOURCES=Object.freeze({
  renown:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§7.1.0 Renown'}),
  difficulty:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§7.2.0 Determining the Project Difficulty'}),
  scope:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§7.3.0 Determining the Project Scope'}),
  opposition:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§7.4.0 Determining the Opposition'}),
  buying:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§7.5.0 Decreasing Difficulty — Renown Bought table'}),
  factions:Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§7.9.0 Player-Run Factions and Major Projects'}),
});
const PROBABILITY=Object.freeze({PLAUSIBLE:1,IMPROBABLE:2,IMPOSSIBLE:4});
const SCOPE=Object.freeze({VILLAGE:2,CITY:4,REGION:8,KINGDOM:16,KNOWN_WORLD:32});
const OPPOSITION=Object.freeze({NONE:1,MINOR_FIGURES:2,LOCAL_LEADERS:4,MAJOR_NOBLE_OR_BEAST:8,KING_OR_FAMED_MONSTER:16});
function key(map,v,label){const k=String(v??'').toUpperCase();if(!(k in map))throw new RangeError(`${label} unsupported: ${v}`);return [k,map[k]]}
function int(v,label,min=0){if(!Number.isInteger(v)||v<min)throw new RangeError(`${label} must be integer >= ${min}`);return v}
export function calculateMajorProjectDifficulty({probability,scope,opposition='NONE',popularDisapproval=false,indefiniteMagicalTransmission=false}){
  const [probabilityId,baseDifficulty]=key(PROBABILITY,probability,'probability');const [scopeId,scopeMultiplier]=key(SCOPE,scope,'scope');const [oppositionId,oppositionMultiplier]=key(OPPOSITION,opposition,'opposition');
  const popularMultiplier=popularDisapproval?2:1,indefiniteMultiplier=indefiniteMagicalTransmission?4:1;
  const difficulty=baseDifficulty*scopeMultiplier*oppositionMultiplier*popularMultiplier*indefiniteMultiplier;
  return Object.freeze({rule:'WWN_MAJOR_PROJECT_DIFFICULTY_V1',sourceRefs:[WWN_MAJOR_PROJECT_SOURCES.difficulty,WWN_MAJOR_PROJECT_SOURCES.scope,WWN_MAJOR_PROJECT_SOURCES.opposition],probability:probabilityId,baseDifficulty,scope:scopeId,scopeMultiplier,opposition:oppositionId,oppositionMultiplier,popularDisapproval:Boolean(popularDisapproval),popularMultiplier,indefiniteMagicalTransmission:Boolean(indefiniteMagicalTransmission),indefiniteMultiplier,difficulty});
}
export function renownPurchaseCost(points){int(points,'points',0);const bands=[[4,500],[4,2000],[8,4000],[16,8000],[32,16000],[64,32000]];let remaining=points,cost=0,bought=0;const breakdown=[];for(const [cap,perPoint] of bands){if(remaining<=0)break;const count=Math.min(cap,remaining);cost+=count*perPoint;bought+=count;remaining-=count;breakdown.push(Object.freeze({count,perPoint,cost:count*perPoint}));}return Object.freeze({rule:'WWN_RENOWN_PURCHASE_COST_V1',sourceRefs:[WWN_MAJOR_PROJECT_SOURCES.buying],points,buyablePoints:bought,prohibitivelyExpensivePoints:remaining,costSilver:cost,fullyPriced:remaining===0,breakdown:Object.freeze(breakdown)});}
export function applyFactionProjectAid({difficulty,wealth,force,cunning,ideallySuited=false,treasureAvailable,alreadyAssistedThisTurn=false}){
  int(difficulty,'difficulty',1);int(wealth,'wealth',0);int(force,'force',0);int(cunning,'cunning',0);int(treasureAvailable,'treasureAvailable',0);
  if(alreadyAssistedThisTurn)return Object.freeze({rule:'WWN_FACTION_PROJECT_AID_V1',sourceRefs:[WWN_MAJOR_PROJECT_SOURCES.factions],applied:false,reason:'ALREADY_ASSISTED_THIS_FACTION_TURN',treasureCost:0,difficultyBefore:difficulty,difficultyAfter:difficulty,reduction:0});
  if(treasureAvailable<1)return Object.freeze({rule:'WWN_FACTION_PROJECT_AID_V1',sourceRefs:[WWN_MAJOR_PROJECT_SOURCES.factions],applied:false,reason:'INSUFFICIENT_TREASURE',treasureCost:0,difficultyBefore:difficulty,difficultyAfter:difficulty,reduction:0});
  const raw=(wealth+force+cunning)*(ideallySuited?2:1),after=Math.max(1,difficulty-raw),reduction=difficulty-after;
  return Object.freeze({rule:'WWN_FACTION_PROJECT_AID_V1',sourceRefs:[WWN_MAJOR_PROJECT_SOURCES.factions],applied:true,treasureCost:1,ideallySuited:Boolean(ideallySuited),attributeTotal:wealth+force+cunning,rawReduction:raw,reduction,difficultyBefore:difficulty,difficultyAfter:after});
}
export function projectRenownReadiness({difficulty,committedRenown}){int(difficulty,'difficulty',1);int(committedRenown,'committedRenown',0);return Object.freeze({rule:'WWN_PROJECT_RENOWN_READINESS_V1',sourceRefs:[WWN_MAJOR_PROJECT_SOURCES.renown],difficulty,committedRenown,remaining:Math.max(0,difficulty-committedRenown),readyForFinalSteps:committedRenown>=difficulty});}
