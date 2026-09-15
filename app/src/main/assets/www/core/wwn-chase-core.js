export const WWN_CHASE_SOURCES = Object.freeze({
  foot: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.6.1 Foot Chases' }),
  mounted: Object.freeze({ sourceId:'SRC-WWN-SRD-1', locator:'§2.6.2 Mounted Chases' }),
});
function int(v,label){if(!Number.isInteger(v))throw new TypeError(`${label} must be integer`);return v;}
function terrain(v){int(v,'terrainFamiliarityModifier');if(v < -2 || v > 2)throw new RangeError('terrain familiarity must be -2..+2');return v;}
function motivation(v){const n=String(v??'NORMAL').toUpperCase();const map={HALF_HEARTED:-1,NORMAL:0,ENRAGED:1};if(!(n in map))throw new RangeError('motivation must be HALF_HEARTED, NORMAL, or ENRAGED');return {name:n,modifier:map[n]};}

export function footFleeingPace({leaderCheckTotal,helperOutcomes=[]}){
  int(leaderCheckTotal,'leaderCheckTotal');if(!Array.isArray(helperOutcomes))throw new TypeError('helperOutcomes must be an array');
  let successBonus=0,harmfulPenalty=0;
  for(const raw of helperOutcomes){const v=String(raw).toUpperCase();if(v==='SUCCESS')successBonus+=1;else if(v==='HARMFUL')harmfulPenalty-=1;else if(v!=='NO_EFFECT')throw new RangeError('helper outcome must be SUCCESS, HARMFUL, or NO_EFFECT');}
  successBonus=Math.min(3,successBonus);
  return Object.freeze({rule:'WWN_FOOT_CHASE_FLEEING_PACE_V1',leaderCheckTotal,successBonus,harmfulPenalty,pace:leaderCheckTotal+successBonus+harmfulPenalty,sourceRefs:[WWN_CHASE_SOURCES.foot]});
}
export function footPursuitModifier({pursuerCount,pursuedCount,headStart='LESS_THAN_MINUTE',terrainFamiliarityModifier=0,motivationState='NORMAL'}){
  int(pursuerCount,'pursuerCount');int(pursuedCount,'pursuedCount');if(pursuerCount<1||pursuedCount<1)throw new RangeError('group counts must be >= 1');
  const head=String(headStart).toUpperCase();const headMap={NONE:2,ONE_ROUND:1,LESS_THAN_MINUTE:0,MORE_THAN_MINUTE:-2};if(!(head in headMap))throw new RangeError('invalid headStart');
  const m=motivation(motivationState);const countModifier=pursuerCount>pursuedCount?-1:0;const t=terrain(terrainFamiliarityModifier);
  const total=countModifier+headMap[head]+t+m.modifier;
  return Object.freeze({rule:'WWN_FOOT_CHASE_PURSUIT_MODIFIER_V1',countModifier,headStartModifier:headMap[head],terrainFamiliarityModifier:t,motivationModifier:m.modifier,total,sourceRefs:[WWN_CHASE_SOURCES.foot]});
}
export function resolveFootChase({fleeingPace,pursuerCheckTotal,pursuitModifier=0}){
  int(fleeingPace,'fleeingPace');int(pursuerCheckTotal,'pursuerCheckTotal');int(pursuitModifier,'pursuitModifier');const pursuingTotal=pursuerCheckTotal+pursuitModifier;
  return Object.freeze({rule:'WWN_FOOT_CHASE_V1',fleeingPace,pursuerCheckTotal,pursuitModifier,pursuingTotal,caught:pursuingTotal>fleeingPace,escaped:pursuingTotal<=fleeingPace,tieMeansEscape:pursuingTotal===fleeingPace,sourceRefs:[WWN_CHASE_SOURCES.foot]});
}
export function mountedPursuitModifier({directSight=true,pursuerFlying=false,pursuedFlying=false,spotterRelaying=false,terrainFamiliarityModifier=0,motivationState='NORMAL'}){
  let total=directSight?0:-2;if(pursuerFlying&&!pursuedFlying)total+=3;else if(!pursuerFlying&&pursuedFlying)total-=3;if(spotterRelaying)total+=1;const t=terrain(terrainFamiliarityModifier);const m=motivation(motivationState);total+=t+m.modifier;
  return Object.freeze({rule:'WWN_MOUNTED_CHASE_MODIFIER_V1',visibilityModifier:directSight?0:-2,flightModifier:pursuerFlying&&!pursuedFlying?3:!pursuerFlying&&pursuedFlying?-3:0,spotterModifier:spotterRelaying?1:0,terrainFamiliarityModifier:t,motivationModifier:m.modifier,total,sourceRefs:[WWN_CHASE_SOURCES.mounted]});
}
export function resolveMountedPursuer({fleeingPace,pursuerCheckTotal,pursuitModifier=0,pursuerMounted=true}){
  int(fleeingPace,'fleeingPace');int(pursuerCheckTotal,'pursuerCheckTotal');int(pursuitModifier,'pursuitModifier');
  if(!pursuerMounted)return Object.freeze({rule:'WWN_MOUNTED_CHASE_V1',eligible:false,caught:false,reason:'UNMOUNTED_PURSUER_CANNOT_GENERALLY_CATCH_SHORT_TERM',sourceRefs:[WWN_CHASE_SOURCES.mounted]});
  const total=pursuerCheckTotal+pursuitModifier;return Object.freeze({rule:'WWN_MOUNTED_CHASE_V1',eligible:true,total,fleeingPace,caught:total>=fleeingPace,fallsBehind:total<fleeingPace,tieCatches:true,sourceRefs:[WWN_CHASE_SOURCES.mounted]});
}
export function resolveMountedPursuitGroup({fleeingPace,pursuers=[]}){
  if(!Array.isArray(pursuers))throw new TypeError('pursuers must be an array');
  const results=pursuers.map(p=>Object.freeze({pursuerId:String(p.pursuerId),...resolveMountedPursuer({fleeingPace,pursuerCheckTotal:p.checkTotal,pursuitModifier:p.modifier??0,pursuerMounted:p.mounted!==false})}));
  return Object.freeze({rule:'WWN_MOUNTED_CHASE_GROUP_V1',caughtBy:Object.freeze(results.filter(r=>r.caught).map(r=>r.pursuerId)),results:Object.freeze(results),sourceRefs:[WWN_CHASE_SOURCES.mounted]});
}
