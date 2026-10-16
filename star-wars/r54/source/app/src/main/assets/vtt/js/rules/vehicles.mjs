import { buildPositivePool, rollPool } from './narrativeDice.mjs';
import { sourceBackedTalentRank, talentRankByOriginal, talentPilotedVehicleDefenseBonus, talentPilotedVehicleSilhouette, fullThrottleTalentSpec } from './talents.mjs';

export const PLANETARY_RANGE_BANDS = Object.freeze(['Bocajarro','Curto','Médio','Longo','Extremo']);

const FACING_KEYS = Object.freeze(['proa','babor','estribor','popa']);

export const VEHICLE_CRITICAL_HIT_TABLE = Object.freeze([
  Object.freeze({min:1,max:9,key:'mechanical_stress',severity:'Easy',repairDifficulty:1,title:'Mechanical Stress'}),
  Object.freeze({min:10,max:18,key:'jostled',severity:'Easy',repairDifficulty:1,title:'Jostled'}),
  Object.freeze({min:19,max:27,key:'losing_power_to_shields',severity:'Easy',repairDifficulty:1,title:'Losing Power to Shields'}),
  Object.freeze({min:28,max:36,key:'knocked_off_course',severity:'Easy',repairDifficulty:1,title:'Knocked Off Course'}),
  Object.freeze({min:37,max:45,key:'tailspin',severity:'Easy',repairDifficulty:1,title:'Tailspin'}),
  Object.freeze({min:46,max:54,key:'component_hit',severity:'Easy',repairDifficulty:1,title:'Component Hit'}),
  Object.freeze({min:55,max:63,key:'shields_failing',severity:'Average',repairDifficulty:2,title:'Shields Failing'}),
  Object.freeze({min:64,max:72,key:'hyperdrive_nav_failure',severity:'Average',repairDifficulty:2,title:'Hyperdrive or Navicomputer Failure'}),
  Object.freeze({min:73,max:81,key:'power_fluctuations',severity:'Average',repairDifficulty:2,title:'Power Fluctuations'}),
  Object.freeze({min:82,max:90,key:'shields_down',severity:'Average',repairDifficulty:2,title:'Shields Down'}),
  Object.freeze({min:91,max:99,key:'engine_damaged',severity:'Average',repairDifficulty:2,title:'Engine Damaged'}),
  Object.freeze({min:100,max:108,key:'shield_overload',severity:'Average',repairDifficulty:2,title:'Shield Overload'}),
  Object.freeze({min:109,max:117,key:'engines_down',severity:'Hard',repairDifficulty:3,title:'Engines Down'}),
  Object.freeze({min:118,max:126,key:'major_system_failure',severity:'Hard',repairDifficulty:3,title:'Major System Failure'}),
  Object.freeze({min:127,max:133,key:'major_hull_breach',severity:'Hard',repairDifficulty:3,title:'Major Hull Breach'}),
  Object.freeze({min:134,max:138,key:'destabilized',severity:'Hard',repairDifficulty:3,title:'Destabilized'}),
  Object.freeze({min:139,max:144,key:'fire',severity:'Hard',repairDifficulty:3,title:'Fire!'}),
  Object.freeze({min:145,max:153,key:'breaking_up',severity:'Daunting',repairDifficulty:4,title:'Breaking Up'}),
  Object.freeze({min:154,max:Number.POSITIVE_INFINITY,key:'vaporized',severity:'Destroyed',repairDifficulty:null,title:'Vaporized'})
]);

export const SMALL_VEHICLE_COMPONENTS = Object.freeze(['support_droid','ejection_system','landing_gear','weapon_system','sensors','comms','hyperdrive','shield_generator','engines']);
export const LARGE_VEHICLE_COMPONENTS = Object.freeze(['life_support','sublight_engines','hyperdrive','shield_generator','navigation','weapon_system','bridge','hangar','power_core']);


const eoteCoreSource=(source)=>Object.freeze({...source,authority:'PRIMARY_EOTE_CORE',rulesProfile:'EOTE_CORE',activeRulesAuthority:true});
const eoteSupplementSource=(source)=>Object.freeze({...source,authority:'PRIMARY_EOTE_SUPPLEMENT',rulesProfile:'EOTE_CORE',activeRulesAuthority:true});
const aorReference=(source)=>Object.freeze({...source,authority:'CROSS_LINE_REFERENCE',rulesProfile:'AOR_REFERENCE_ONLY',activeRulesAuthority:false});

function clone(v){ return structuredClone(v); }
function int(v,label,{min=0,max=999}={}){ const n=Number(v); if(!Number.isInteger(n)||n<min||n>max) throw new Error(`${label} inválido.`); return n; }
function signedInt(v,label,{min=-9,max=9}={}){ const n=Number(v); if(!Number.isInteger(n)||n<min||n>max) throw new Error(`${label} inválido.`); return n; }
function rangeIndex(range){ const i=PLANETARY_RANGE_BANDS.indexOf(range); if(i<0) throw new Error('Intervalo de alcance planetário inválido.'); return i; }
function defenseValue(v){ const n=Number(v ?? 0); return Number.isFinite(n)&&n>0?Math.floor(n):0; }
function timedRoundEffectActive(effect,currentRound=null){
  if(!effect||typeof effect!=='object') return false;
  if(currentRound===null||currentRound===undefined) return true;
  const r=Number(currentRound);
  const start=Number(effect.startsAtRound??r), end=Number(effect.expiresAfterRound??r);
  return r>=start&&r<=end;
}
function downgradeDifficultyPool(difficulty,challenge=0,downgrades=0){
  let d=Math.max(0,Number(difficulty)||0), c=Math.max(0,Number(challenge)||0), n=Math.max(0,Number(downgrades)||0);
  while(n-->0){ if(c>0){ c-=1; d+=1; } else if(d>0) d-=1; }
  return {difficulty:d,challenge:c};
}

function currentDefenseTotal(vehicle){ return FACING_KEYS.reduce((sum,key)=>sum+defenseValue(vehicle.defense?.[key]),0); }
function criticalEffectActive(hit,currentRound=null){
  if(!hit || hit.repaired || hit.effectExpired) return false;
  if(currentRound===null || hit.effectExpiresAfterRound===null || hit.effectExpiresAfterRound===undefined) return true;
  return Number(currentRound)<=Number(hit.effectExpiresAfterRound);
}
export function vehicleCriticalResult(total){
  const n=Math.max(1,Math.trunc(Number(total)||1));
  return VEHICLE_CRITICAL_HIT_TABLE.find(row=>n>=row.min&&n<=row.max) ?? VEHICLE_CRITICAL_HIT_TABLE.at(-1);
}
export function unrepairedVehicleCriticalCount(vehicle){
  validateVehicle(vehicle);
  return vehicle.criticalHits.filter(hit=>!hit?.repaired).length;
}
export function vehicleCriticalModifiers(vehicle,{currentRound=null}={}){
  validateVehicle(vehicle);
  const mods={
    defenseLoss:{proa:0,babor:0,estribor:0,popa:0}, defenseZero:new Set(),
    speedMaxLoss:0, enginesDown:false, noManeuvers:false, firingSetback:0,
    hyperspaceDisabled:false, navigationDisabled:false, cannotVoluntarySystemStrain:false,
    armorLoss:0, thresholdsHalved:false, depressurizing:false, depressurizeRounds:null,
    fireActive:false, destroyed:false, breakingUp:false, breakingUpDestroyAfterRound:null,
    disabledComponents:new Set(vehicle.disabledComponents??[]), temporaryDisabledComponents:new Set(),
    pilotRecoveryDifficulty:null, crewDisoriented:false
  };
  for(const hit of vehicle.criticalHits){
    if(!hit || hit.repaired) continue;
    if(hit.key==='vaporized') mods.destroyed=true;
    if(hit.key==='breaking_up'){
      mods.breakingUp=true;
      const at=Number(hit.destroyAfterRound);
      if(Number.isFinite(at)) mods.breakingUpDestroyAfterRound=mods.breakingUpDestroyAfterRound===null?at:Math.min(mods.breakingUpDestroyAfterRound,at);
      if(hit.destroyed) mods.destroyed=true;
    }
    if(!criticalEffectActive(hit,currentRound)) continue;
    switch(hit.key){
      case 'jostled': mods.crewDisoriented=true; break;
      case 'losing_power_to_shields':
        if(!hit.fallbackSystemStrain) mods.defenseLoss[hit.defenseZone??'proa']+=1;
        break;
      case 'knocked_off_course':
        mods.noManeuvers=true; mods.pilotRecoveryDifficulty=Math.max(mods.pilotRecoveryDifficulty??0,Number(hit.pilotRecoveryDifficulty??vehicle.speed??0)); break;
      case 'tailspin': mods.firingSetback=Math.max(mods.firingSetback,2); mods.crewDisoriented=true; break;
      case 'component_hit':
        if(hit.component) mods.temporaryDisabledComponents.add(hit.component);
        break;
      case 'shields_failing':
        if(!hit.fallbackSystemStrain) for(const key of FACING_KEYS) mods.defenseLoss[key]+=1;
        break;
      case 'hyperdrive_nav_failure':
        mods.hyperspaceDisabled=true; if(!vehicle.hyperdrive) mods.navigationDisabled=true; break;
      case 'power_fluctuations': mods.cannotVoluntarySystemStrain=true; break;
      case 'shields_down':
        if(!hit.fallbackSystemStrain){
          mods.defenseZero.add(hit.defenseZone??'proa');
          for(const key of FACING_KEYS) if(key!==(hit.defenseZone??'proa')) mods.defenseLoss[key]+=1;
        }
        break;
      case 'engine_damaged': mods.speedMaxLoss+=1; break;
      case 'shield_overload':
        if(hit.armorFallback) mods.armorLoss+=1; else for(const key of FACING_KEYS) mods.defenseZero.add(key);
        break;
      case 'engines_down': mods.enginesDown=true; mods.noManeuvers=true; break;
      case 'major_system_failure':
        if(hit.component) mods.disabledComponents.add(hit.component);
        break;
      case 'major_hull_breach':
        mods.depressurizing=true; mods.depressurizeRounds=Number(hit.depressurizeRounds??vehicle.silhouette); break;
      case 'destabilized': mods.thresholdsHalved=true; break;
      case 'fire': mods.fireActive=true; break;
    }
  }
  return {...mods,defenseZero:[...mods.defenseZero],disabledComponents:[...mods.disabledComponents],temporaryDisabledComponents:[...mods.temporaryDisabledComponents]};
}
export function canVoluntarilyInflictVehicleStrain(vehicle){ return !vehicleCriticalModifiers(vehicle).cannotVoluntarySystemStrain; }
export function expireVehicleCriticalRound(vehicle,currentRound){
  validateVehicle(vehicle);
  const next=clone(vehicle);
  const round=Math.max(0,Math.trunc(Number(currentRound)||0));
  for(const hit of next.criticalHits){
    if(hit?.repaired) continue;
    if(Number.isFinite(Number(hit?.effectExpiresAfterRound)) && round>Number(hit.effectExpiresAfterRound)) hit.effectExpired=true;
    if(hit?.key==='breaking_up' && Number.isFinite(Number(hit.destroyAfterRound)) && round>Number(hit.destroyAfterRound)) hit.destroyed=true;
  }
  return next;
}
export function clearKnockedOffCourse(vehicle){
  validateVehicle(vehicle);
  const next=clone(vehicle);
  const hit=[...next.criticalHits].reverse().find(x=>x?.key==='knocked_off_course'&&!x.repaired&&!x.effectExpired);
  if(hit) hit.effectExpired=true;
  return next;
}

export const EOTE_CORE_STARTER_VEHICLE_KEYS = Object.freeze(['wayfarer','yt1300','firespray']);
export const EOTE_CORE_CATALOG_VEHICLE_KEYS = Object.freeze(['x34','digger_crawler','personnel_carrier','at_pt','at_est','cloakshape','jumpmaster_jm5000','firespray','lambda_t4a','tie_ln','ywing_btl','z95_af4','skipray_gat12h','wayfarer','yt1300']);
export const EOTE_FLY_CASUAL_VEHICLE_KEYS = Object.freeze(['alpha_xg1_star_wing','vt49_decimator','wud500','starlight','ykl37r_nova_courier','gx1_short_hauler','baudo_star_yacht','gozanti','hwk1000','yt2000']);
export const EOTE_ACTIVE_CATALOG_VEHICLE_KEYS = Object.freeze([...EOTE_CORE_CATALOG_VEHICLE_KEYS,...EOTE_FLY_CASUAL_VEHICLE_KEYS]);

export const SWE09_ADVENTURE_VEHICLE_PROFILES = Object.freeze({
  storm_iv_cloud_car:Object.freeze({
    key:'storm_iv_cloud_car',name:'Storm IV Cloud Car — Guarda Alada',shortName:'Storm IV',faction:'Força de Defesa de Bespin',
    type:'Cloud Car',manufacturer:'Bespin Motors',silhouette:2,speedMax:4,handling:1,
    defense:{proa:0,babor:0,estribor:0,popa:0},armor:2,hullThreshold:5,systemStrainThreshold:7,
    hyperdrive:false,navComputer:false,astromechPort:false,sensorRange:'Curto',
    crew:['1 piloto','1 artilheiro'],passengers:0,encumbranceCapacity:8,price:30000,rarity:5,hardPoints:2,
    weapons:[{name:'Canhões blaster leves frontais',skill:'artilleria',damage:4,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}}],
    source:eoteSupplementSource({book:'SWE09 The Jewel of Yavin',printedPage:95,pdfPage:96,
      driveFileId:'1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K',visualStatBlockVerified:true})
  }),
});

export const SWE02_ADVENTURE_VEHICLE_PROFILES = Object.freeze({
  swe02_thwheek_dunelizard:Object.freeze({
    key:'swe02_thwheek_dunelizard',name:'G1-M4-C Dunelizard — Thwheek',shortName:'Dunelizard',
    faction:'Teemo the Hutt',type:'Caça estelar médio',manufacturer:'Não especificado na SWE02',
    silhouette:3,speedMax:4,handling:1,defense:{proa:1,babor:0,estribor:0,popa:0},
    armor:3,hullThreshold:8,systemStrainThreshold:7,hyperdrive:true,navComputer:false,astromechPort:false,
    sensorRange:'Curto',crew:['Thwheek (piloto)'],passengers:0,encumbranceCapacity:0,price:0,rarity:0,hardPoints:0,
    weapons:[{name:'Canhões laser médios frontais vinculados',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{linked:1}}],
    source:eoteSupplementSource({book:'SWE02 The Long Arm of the Hutt',printedPage:12,pdfPage:12,
      note:'Valores não publicados como preço/carga não constituem opções de compra; uso apenas no encontro.'})
  })
});

export const STARTER_VEHICLE_PROFILES = Object.freeze({
  x34:Object.freeze({
    key:'x34', name:'Landspeeder X-34', shortName:'X-34', faction:'Independente', type:'Landspeeder', manufacturer:'SoroSuub',
    silhouette:2, speedMax:2, handling:0, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:0, hullThreshold:4, systemStrainThreshold:5,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Bocajarro', maximumAltitude:'1 metro',
    crew:['1 piloto'], passengers:1, encumbranceCapacity:15, price:4500, rarity:2, hardPoints:2, weapons:[],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:251,pdfPage:260})
  }),
  digger_crawler:Object.freeze({
    key:'digger_crawler', name:'Digger Crawler da Corellian Mining', shortName:'Digger Crawler', faction:'Independente', type:'Refinaria móvel', manufacturer:'Corellian Mining Corporation',
    silhouette:4, speedMax:1, handling:-3, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:4, hullThreshold:40, systemStrainThreshold:30,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Médio',
    crew:['50 mineiros e engenheiros'], alternateCrew:'100 Jawas', passengers:30, alternatePassengers:60, encumbranceCapacity:400, price:50000, rarity:7, hardPoints:5, weapons:[],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:251,pdfPage:260})
  }),
  personnel_carrier:Object.freeze({
    key:'personnel_carrier', name:'Transportador de pessoal', shortName:'Personnel Carrier', faction:'Independente', type:'Groundcar', manufacturer:'Vários fabricantes',
    silhouette:3, speedMax:2, handling:-2, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:0, hullThreshold:8, systemStrainThreshold:4,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Nenhum',
    crew:['1 piloto','1 copiloto'], passengers:'4-15', encumbranceCapacity:100, encumbranceNote:'sem passageiros', price:3000, rarity:2, hardPoints:3, weapons:[],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:252,pdfPage:261})
  }),
  at_pt:Object.freeze({
    key:'at_pt', name:'All Terrain Personal Transport AT-PT', shortName:'AT-PT', faction:'Independente', type:'Walker', manufacturer:'Rothana Heavy Industries',
    silhouette:2, speedMax:2, handling:0, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:3, hullThreshold:6, systemStrainThreshold:5,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Bocajarro', crew:['1 piloto'], passengers:1, encumbranceCapacity:15, price:30000, rarity:7, hardPoints:1,
    weapons:[
      {name:'Canhão blaster leve duplo frontal',skill:'artilleria',damage:4,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Lança-granadas de concussão frontal',skill:'artilleria',damage:10,crit:4,range:'Extremo',fireArc:'proa',scale:'personal',qualities:{breach:1,blast:8,limitedAmmo:20}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:253,pdfPage:262})
  }),
  at_est:Object.freeze({
    key:'at_est', name:'AT-EST Exploration and Survey Walker', shortName:'AT-EST', faction:'Independente', type:'Walker de exploração', manufacturer:'Rothana Heavy Engineering',
    silhouette:4, speedMax:1, handling:-2, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:2, hullThreshold:20, systemStrainThreshold:12,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Longo', crew:['1 piloto','1 copiloto','1 operador de sensores/comunicações'], passengers:8, encumbranceCapacity:50, autonomy:'1 mês', price:12000, rarity:5, hardPoints:4,
    weapons:[{name:'Canhão blaster leve frontal',skill:'artilleria',damage:4,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{}}],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:253,pdfPage:262})
  }),
  cloakshape:Object.freeze({
    key:'cloakshape', name:'Caça estelar CloakShape', shortName:'CloakShape', faction:'Independente', type:'Caça estelar', manufacturer:'Kuat Systems Engineering',
    silhouette:3, speedMax:4, handling:0, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:3, hullThreshold:10, systemStrainThreshold:8,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Bocajarro', crew:['1 piloto'], passengers:0, encumbranceCapacity:12, autonomy:'1 dia', price:38000, rarity:4, hardPoints:3,
    weapons:[
      {name:'Canhões laser leves frontais',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Lançadores de mísseis de concussão frontais',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{breach:4,blast:4,guided:3,linked:1,limitedAmmo:6,slowFiring:1}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:254,pdfPage:263})
  }),
  jumpmaster_jm5000:Object.freeze({
    key:'jumpmaster_jm5000', name:'Batedor de longo alcance JM-5000 JumpMaster', shortName:'JM-5000 JumpMaster', faction:'Independente', type:'Barco de patrulha/batedor', manufacturer:'Corellian Engineering Corporation',
    silhouette:4, speedMax:4, handling:1, defense:{proa:1,babor:0,estribor:0,popa:0}, armor:2, hullThreshold:14, systemStrainThreshold:8,
    hyperdrive:true, hyperdriveClass:3, backupHyperdriveClass:15, navComputer:true, astromechPort:false, sensorRange:'Extremo', crew:['1 piloto'], passengers:1, encumbranceCapacity:30, autonomy:'2 meses', price:55000, rarity:5, hardPoints:2,
    weapons:[{name:'Canhão laser leve frontal',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{}}],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:256,pdfPage:265})
  }),
  wayfarer:Object.freeze({
    key:'wayfarer', name:'Cargueiro médio classe Wayfarer', shortName:'Wayfarer', faction:'Independente', type:'Cargueiro médio', manufacturer:'Kuat Systems Engineering',
    silhouette:5, speedMax:3, handling:-2, defense:{proa:1,babor:1,estribor:1,popa:2}, armor:4, hullThreshold:32, systemStrainThreshold:20,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:14, navComputer:true, astromechPort:false, sensorRange:'Curto',
    crew:['1 piloto','1 copiloto','1 engenheiro','1 mestre de carga','6 tripulantes'], passengers:6, encumbranceCapacity:850, autonomy:'3 meses', price:120000, rarity:5, hardPoints:5,
    weapons:[{name:'Canhão laser quádruplo dorsal',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{accurate:1,linked:3}}],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:'263-264',pdfPage:273,errata:'Sensor Range: Short'})
  }),
  yt1300:Object.freeze({
    key:'yt1300', name:'Cargueiro leve YT-1300', shortName:'YT-1300', faction:'Independente', type:'Cargueiro leve', manufacturer:'Corellian Engineering Corporation',
    silhouette:4, speedMax:3, handling:-1, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:22, systemStrainThreshold:15,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto',
    crew:['1 piloto','1 copiloto/engenheiro'], passengers:6, encumbranceCapacity:165, autonomy:'2 meses', price:100000, rarity:4, hardPoints:6,
    weapons:[
      {name:'Canhão laser médio dorsal em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{}},
      {name:'Canhão laser médio ventral em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:264,pdfPage:273})
  }),
  firespray:Object.freeze({
    key:'firespray', name:'Nave de patrulha Firespray', shortName:'Firespray', faction:'Independente', type:'Barco de patrulha', manufacturer:'Kuat Systems Engineering',
    silhouette:4, speedMax:4, handling:0, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:4, hullThreshold:15, systemStrainThreshold:12,
    hyperdrive:true, hyperdriveClass:3, backupHyperdriveClass:15, navComputer:true, astromechPort:false, sensorRange:'Curto',
    crew:['1 piloto','2 guardas'], passengers:6, passengerRole:'prisioneiros', encumbranceCapacity:40, autonomy:'1 mês', price:80000, rarity:4, hardPoints:4,
    weapons:[
      {name:'Autoblasters frontais',skill:'artilleria',damage:3,crit:5,range:'Bocajarro',fireArc:'proa',qualities:{autoFire:true}},
      {name:'Projetor de raio trator leve frontal',skill:'artilleria',damage:0,crit:null,range:'Bocajarro',fireArc:'proa',nonDamage:true,qualities:{tractor:2}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:255,pdfPage:264})
  }),
  alpha_xg1_star_wing:Object.freeze({
    key:'alpha_xg1_star_wing', name:'Canhoneira de assalto Alpha-class XG-1 Star Wing', shortName:'XG-1 Star Wing', faction:'Império Galáctico', type:'Caça estelar de assalto', manufacturer:'Cygnus Spaceworks',
    silhouette:3, speedMax:4, handling:-2, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:12, systemStrainThreshold:8,
    hyperdrive:true, hyperdriveClass:3, backupHyperdriveClass:null, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto'], passengers:0, encumbranceCapacity:8, autonomy:'3 dias', price:135000, rarity:7, restricted:true, hardPoints:2,
    weapons:[
      {name:'Canhões laser médios frontais',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Canhões de íons frontais',skill:'artilleria',damage:5,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{ion:true,linked:1}},
      {name:'Lançadores de mísseis de concussão frontais',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{breach:4,blast:4,guided:3,limitedAmmo:16,linked:1,slowFiring:1}}
    ],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:54,pdfPage:55})
  }),
  vt49_decimator:Object.freeze({
    key:'vt49_decimator', name:'Nave de assalto VT-49 Decimator', shortName:'VT-49 Decimator', faction:'Império Galáctico', type:'Nave de assalto', manufacturer:'Kuat Drive Yards',
    silhouette:4, speedMax:3, handling:-3, defense:{proa:2,babor:0,estribor:0,popa:2}, armor:4, hullThreshold:30, systemStrainThreshold:20,
    hyperdrive:true, hyperdriveClass:1, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto/comandante','1 copiloto/navegador','2 artilheiros'], passengers:6, encumbranceCapacity:60, autonomy:'1 mês', price:160000, rarity:6, hardPoints:1,
    weapons:[
      {name:'Canhão laser quádruplo dorsal em torre',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{accurate:1,linked:3}},
      {name:'Canhão laser quádruplo ventral em torre',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{accurate:1,linked:3}},
      {name:'Lançadores duplos de mísseis de concussão frontais',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{breach:4,blast:4,guided:3,limitedAmmo:20,linked:1,slowFiring:1}}
    ],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:55,pdfPage:56})
  }),
  wud500:Object.freeze({
    key:'wud500', name:'Iate estelar CEC WUD-500', shortName:'WUD-500', faction:'Independente', type:'Iate estelar', manufacturer:'Corellian Engineering Corporation',
    silhouette:4, speedMax:4, handling:-1, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:1, hullThreshold:18, systemStrainThreshold:14,
    hyperdrive:true, hyperdriveClass:1, backupHyperdriveClass:8, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/navegador'], passengers:6, encumbranceCapacity:65, autonomy:'4 meses', price:98500, rarity:6, hardPoints:5,
    weapons:[{name:'Canhões blaster duplos frontais',skill:'artilleria',damage:4,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}}],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:55,pdfPage:56})
  }),
  starlight:Object.freeze({
    key:'starlight', name:'Cargueiro leve classe Starlight', shortName:'Starlight', faction:'Independente', type:'Cargueiro leve', manufacturer:'Rendili StarDrive e Surron StarTech',
    silhouette:4, speedMax:3, handling:-1, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:20, systemStrainThreshold:16,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:15, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/navegador'], passengers:6, encumbranceCapacity:85, autonomy:'1 mês', price:69995, rarity:5, hardPoints:4,
    weapons:[{name:'Canhão laser médio em torre',skill:'artilleria',damage:6,crit:5,range:'Bocajarro',fireArc:'todos',qualities:{}}],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:56,pdfPage:57})
  }),
  ykl37r_nova_courier:Object.freeze({
    key:'ykl37r_nova_courier', name:'Cargueiro YKL-37R Nova Courier', shortName:'YKL-37R Nova Courier', faction:'Independente', type:'Cargueiro leve', manufacturer:'Gallofree Yards, Inc.',
    silhouette:4, speedMax:3, handling:0, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:24, systemStrainThreshold:18,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/engenheiro'], passengers:6, encumbranceCapacity:100, autonomy:'2 meses', price:130000, rarity:6, hardPoints:5,
    weapons:[
      {name:'Canhão laser médio dorsal em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{}},
      {name:'Canhão laser médio ventral em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{}},
      {name:'Lançador de mísseis de concussão frontal',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{breach:4,blast:4,guided:3,limitedAmmo:6,slowFiring:1}},
      {name:'Lançador de mísseis de concussão de popa',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'popa',qualities:{breach:4,blast:4,guided:3,limitedAmmo:6,slowFiring:1}}
    ],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:56,pdfPage:57})
  }),
  gx1_short_hauler:Object.freeze({
    key:'gx1_short_hauler', name:'Cargueiro GX1 Short Hauler', shortName:'GX1 Short Hauler', faction:'Independente', type:'Cargueiro leve', manufacturer:'Lantillian Shipwrights',
    silhouette:4, speedMax:2, handling:-2, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:2, hullThreshold:24, systemStrainThreshold:15,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/engenheiro','2 comissários/mestres de carga'], passengers:6, encumbranceCapacity:200, autonomy:'1 mês', price:85000, rarity:5, hardPoints:5,
    weapons:[{name:'Canhão laser pesado dorsal em torre',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'todos',qualities:{}}],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:57,pdfPage:58})
  }),
  baudo_star_yacht:Object.freeze({
    key:'baudo_star_yacht', name:'Iate estelar classe Baudo', shortName:'Baudo-class', faction:'Independente', type:'Iate estelar', manufacturer:'Mendel Baudo Shipyards',
    silhouette:4, speedMax:4, handling:1, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:1, hullThreshold:26, systemStrainThreshold:10,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/engenheiro'], passengers:8, encumbranceCapacity:60, autonomy:'1 mês', price:250000, rarity:9, hardPoints:6,
    weapons:[{name:'Canhão laser médio em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{}}],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:57,pdfPage:58})
  }),
  gozanti:Object.freeze({
    key:'gozanti', name:'Transporte armado classe Gozanti', shortName:'Gozanti', faction:'Independente', type:'Transporte armado', manufacturer:'Gallofree Yards, Inc. / Corellian Engineering Corporation',
    silhouette:5, speedMax:2, handling:-3, defense:{proa:2,babor:2,estribor:2,popa:1}, armor:5, hullThreshold:50, systemStrainThreshold:36,
    hyperdrive:true, hyperdriveClass:5, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Longo', crew:['1 piloto','1 copiloto','1 engenheiro','1 operador de comunicações/sensores','2 mestres de carga','6 artilheiros'], passengers:12, encumbranceCapacity:1000, autonomy:'1 mês', price:200000, rarity:6, hardPoints:4,
    weapons:[
      {name:'Canhão laser quádruplo dorsal retrátil',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{accurate:1,linked:3}},
      {name:'Canhão laser quádruplo ventral retrátil',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{accurate:1,linked:3}},
      {name:'Canhões laser pesados duplos de babor',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'babor',mountCount:2,qualities:{linked:1}},
      {name:'Canhões laser pesados duplos de estribor',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'estribor',mountCount:2,qualities:{linked:1}},
      {name:'Lançador de torpedos de prótons frontal',skill:'artilleria',damage:8,crit:2,range:'Curto',fireArc:'proa',qualities:{blast:6,breach:6,guided:2,limitedAmmo:6,slowFiring:1}}
    ],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:58,pdfPage:59})
  }),
  hwk1000:Object.freeze({
    key:'hwk1000', name:'Cargueiro leve CEC HWK-1000', shortName:'HWK-1000', faction:'Independente', type:'Cargueiro leve', manufacturer:'Corellian Engineering Corporation',
    silhouette:4, speedMax:5, handling:1, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:18, systemStrainThreshold:15,
    hyperdrive:true, hyperdriveClass:1, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/navegador'], passengers:4, encumbranceCapacity:100, autonomy:'6 meses', price:120000, rarity:10, restricted:true, hardPoints:5, weapons:[],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:59,pdfPage:60})
  }),
  yt2000:Object.freeze({
    key:'yt2000', name:'Cargueiro leve YT-2000', shortName:'YT-2000', faction:'Independente', type:'Cargueiro leve', manufacturer:'Corellian Engineering Corporation',
    silhouette:4, speedMax:3, handling:0, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:24, systemStrainThreshold:14,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:12, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto/engenheiro'], passengers:7, encumbranceCapacity:170, autonomy:'3 meses', price:120000, rarity:5, hardPoints:5,
    weapons:[
      {name:'Canhões laser médios duplos dorsais em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{linked:1}},
      {name:'Canhões laser médios duplos ventrais em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{linked:1}}
    ],
    source:eoteSupplementSource({book:'Fly Casual',printedPage:60,pdfPage:61})
  }),
  xwing_t65b:Object.freeze({
    key:'xwing_t65b', name:'Caça estelar multipropósito T-65B Ala-X', shortName:'Ala-X T-65B', faction:'Aliança Rebelde', type:'Caça estelar', manufacturer:'Corporação Incom',
    silhouette:3, speedMax:5, handling:1, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:10, systemStrainThreshold:10,
    hyperdrive:true, navComputer:false, astromechPort:true, crew:['1 piloto','1 droide astromecânico'], passengers:0, autonomy:'1 semana', price:120000,
    weapons:[
      {name:'Canhões laser médios nas pontas das asas',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',qualities:{linked:3}},
      {name:'Lançatorpedos de prótons no nariz',skill:'artilleria',damage:8,crit:2,range:'Curto',qualities:{breach:6,linked:1,blast:6,limitedAmmo:6}}
    ], source:aorReference({book:'La Era de la Rebelión - Libro de reglas',printedPage:43,pdfPage:44})
  }),
  tie_ln:Object.freeze({
    key:'tie_ln', name:'Caça estelar TIE/ln', shortName:'TIE/ln', faction:'Império Galáctico', type:'Caça estelar', manufacturer:'Sienar Fleet Systems',
    silhouette:3, speedMax:5, handling:3, defense:{proa:0,babor:0,estribor:0,popa:0}, armor:2, hullThreshold:6, systemStrainThreshold:8,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Bocajarro', crew:['1 piloto'], passengers:0, encumbranceCapacity:4, autonomy:'2 dias', price:50000, rarity:4, restricted:true, hardPoints:0,
    weapons:[{name:'Canhões laser médios frontais',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}}],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:257,pdfPage:266})
  }),
  lambda_t4a:Object.freeze({
    key:'lambda_t4a', name:'Lançadeira de longo alcance T-4A classe Lambda', shortName:'Lançadeira Lambda', faction:'Império Galáctico', type:'Lançadeira', manufacturer:'Sienar Fleet Systems / Cygnus Spaceworks',
    silhouette:4, speedMax:3, handling:0, defense:{proa:2,babor:0,estribor:0,popa:1}, armor:4, hullThreshold:25, systemStrainThreshold:15,
    hyperdrive:true, hyperdriveClass:1, backupHyperdriveClass:10, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto','1 navegador','1 artilheiro','1 operador de comunicações','1 engenheiro'], passengers:20, encumbranceCapacity:200, encumbranceNote:'sem passageiros', autonomy:'2 meses', price:140000, rarity:6, restricted:true, hardPoints:2,
    weapons:[
      {name:'Canhões blaster leves frontais duplos',skill:'artilleria',damage:4,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Canhões laser leves frontais duplos',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Canhão blaster leve duplo retrátil de popa',skill:'artilleria',damage:4,crit:4,range:'Bocajarro',fireArc:'popa',qualities:{linked:1}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:'256-257',pdfPage:'265-266'})
  }),
  ywing_btl:Object.freeze({
    key:'ywing_btl', name:'Caça de ataque BTL-A4/BTL-S3 Y-Wing', shortName:'Y-Wing BTL', faction:'Independente', type:'Caça estelar de ataque', manufacturer:'Koensayr Manufacturing',
    silhouette:3, speedMax:4, handling:0, defense:{proa:1,babor:0,estribor:0,popa:1}, armor:3, hullThreshold:12, systemStrainThreshold:10,
    hyperdrive:true, hyperdriveClass:1, backupHyperdriveClass:null, navComputer:false, astromechPort:true, sensorRange:'Bocajarro', crew:['1 piloto'], alternateCrew:'1 piloto + 1 artilheiro no BTL-S3', passengers:0, encumbranceCapacity:10, autonomy:'1 semana', price:80000, rarity:4, hardPoints:1,
    weapons:[
      {name:'Canhões de íons leves duplos em torre',skill:'artilleria',damage:5,crit:4,range:'Bocajarro',fireArc:'todos',qualities:{ion:true,linked:1},variantControl:'BTL-S3: artilheiro; BTL-A4: frontal/piloto'},
      {name:'Canhões laser médios frontais',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Lançadores de torpedos de prótons frontais',skill:'artilleria',damage:8,crit:2,range:'Curto',fireArc:'proa',qualities:{breach:6,blast:6,guided:2,limitedAmmo:8,linked:1,slowFiring:1}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:258,pdfPage:267})
  }),
  z95_af4:Object.freeze({
    key:'z95_af4', name:'Caça estelar Z-95-AF4 Headhunter', shortName:'Z-95-AF4', faction:'Independente', type:'Caça estelar', manufacturer:'Incom/Subpro',
    silhouette:3, speedMax:4, handling:1, defense:{proa:1,babor:0,estribor:0,popa:0}, armor:3, hullThreshold:9, systemStrainThreshold:8,
    hyperdrive:false, navComputer:false, astromechPort:false, sensorRange:'Bocajarro', crew:['1 piloto'], passengers:0, encumbranceCapacity:8, autonomy:'1 dia', price:55000, rarity:4, hardPoints:2,
    weapons:[
      {name:'Canhões laser leves nas pontas das asas',skill:'artilleria',damage:5,crit:3,range:'Bocajarro',fireArc:'proa',qualities:{linked:1}},
      {name:'Lançadores de mísseis de concussão frontais',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{breach:4,blast:4,guided:3,limitedAmmo:6,linked:1,slowFiring:1}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:258,pdfPage:267})
  }),
  skipray_gat12h:Object.freeze({
    key:'skipray_gat12h', name:'GAT-12h Skipray Blastboat', shortName:'Skipray GAT-12h', faction:'Independente', type:'Barco de patrulha', manufacturer:'Sienar Fleet Systems',
    silhouette:4, speedMax:4, handling:-1, defense:{proa:2,babor:0,estribor:0,popa:1}, armor:4, hullThreshold:20, systemStrainThreshold:15,
    hyperdrive:true, hyperdriveClass:2, backupHyperdriveClass:15, navComputer:true, astromechPort:false, sensorRange:'Curto', crew:['1 piloto','1 copiloto','1 operador de comunicações/sensores','2 artilheiros'], passengers:0, encumbranceCapacity:20, autonomy:'1 mês', price:150000, rarity:6, hardPoints:2,
    weapons:[
      {name:'Canhões de íons leves triplos frontais',skill:'artilleria',damage:5,crit:4,range:'Bocajarro',fireArc:'proa',qualities:{ion:true,linked:2}},
      {name:'Canhão laser médio duplo dorsal em torre',skill:'artilleria',damage:6,crit:3,range:'Bocajarro',fireArc:'todos',qualities:{linked:1}},
      {name:'Lançador de torpedos de prótons frontal',skill:'artilleria',damage:8,crit:2,range:'Curto',fireArc:'proa',qualities:{breach:6,blast:6,guided:2,limitedAmmo:5,slowFiring:1}},
      {name:'Lançador de mísseis de concussão frontal',skill:'artilleria',damage:6,crit:3,range:'Curto',fireArc:'proa',qualities:{breach:4,blast:4,guided:3,limitedAmmo:8,slowFiring:1}}
    ],
    source:eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:259,pdfPage:268})
  })
});

export function eoteCoreStarterVehicleProfiles(){
  return Object.freeze(Object.fromEntries(EOTE_CORE_STARTER_VEHICLE_KEYS.map(key=>[key,STARTER_VEHICLE_PROFILES[key]])));
}
export function eoteCoreVehicleProfiles(){
  return Object.freeze(Object.fromEntries(EOTE_CORE_CATALOG_VEHICLE_KEYS.map(key=>[key,STARTER_VEHICLE_PROFILES[key]])));
}
export function eoteActiveVehicleProfiles(){
  return Object.freeze(Object.fromEntries(EOTE_ACTIVE_CATALOG_VEHICLE_KEYS.map(key=>[key,STARTER_VEHICLE_PROFILES[key]])));
}

export function applyTractorLock(vehicle,{rating=1,sourceId='tractor'}={}){
  validateVehicle(vehicle);
  const next=clone(vehicle);
  const r=int(rating,'Rating de Tractor',{min:1,max:5});
  next.tractorLocks=Array.isArray(next.tractorLocks)?next.tractorLocks:[];
  next.tractorLocks=next.tractorLocks.filter(lock=>lock?.sourceId!==sourceId);
  next.tractorLocks.push({sourceId:String(sourceId||'tractor'),rating:r});
  return next;
}
export function tractorLockDifficulty(vehicle){
  validateVehicle(vehicle);
  return Math.max(0,...(Array.isArray(vehicle.tractorLocks)?vehicle.tractorLocks:[]).map(lock=>Number(lock?.rating)||0));
}
export function clearTractorLocks(vehicle){
  validateVehicle(vehicle);
  const next=clone(vehicle); next.tractorLocks=[]; return next;
}
export function tractorEscapeCheck(vehicle,character,{rng=Math.random,boost=0,setback=0,difficultyDowngrades=0,difficultyUpgrades=0}={}){
  validateVehicle(vehicle); if(!character) throw new Error('Piloto ausente.');
  const difficulty=tractorLockDifficulty(vehicle);
  if(difficulty<=0) return {skipped:true,reason:'not_tractored',difficulty:0,check:null,vehicle:clone(vehicle)};
  const positive=buildPositivePool(Number(character.characteristics?.agilidad??0),Number(character.skills?.pilotar_vehiculo_espacial??0));
  const handling=handlingPoolModifier(vehicle);
  const upgraded=upgradeDifficultyPool(difficulty,0,difficultyUpgrades);
  const negative=downgradeDifficultyPool(upgraded.difficulty,upgraded.challenge,difficultyDowngrades);
  const check=rollPool({...positive,...negative,boost:Number(boost)+handling.boost,setback:Number(setback)+handling.setback},rng);
  return {difficulty:negative.difficulty,baseDifficulty:difficulty,difficultyDowngrades:Number(difficultyDowngrades)||0,difficultyUpgrades,check,vehicle:check.success?clearTractorLocks(vehicle):clone(vehicle)};
}

export function validateVehicle(vehicle){
  if(!vehicle||typeof vehicle!=='object') throw new Error('Veículo inválido.');
  if(typeof vehicle.id!=='string'||!vehicle.id) throw new Error('Veículo sem identificador.');
  if(typeof vehicle.name!=='string'||!vehicle.name) throw new Error('Veículo sem nome.');
  int(vehicle.silhouette,'Silhueta',{min:0,max:10});
  if(vehicle.massive!==undefined)int(vehicle.massive,'Massive',{min:0,max:10});
  int(vehicle.speedMax,'Velocidade máxima',{min:0,max:9});
  int(vehicle.speed,'Velocidade atual',{min:0,max:vehicle.speedMax});
  signedInt(vehicle.handling,'Manejo',{min:-9,max:9});
  int(vehicle.armor,'Blindagem',{min:0,max:99});
  int(vehicle.hullThreshold,'Limiar de Danos no casco',{min:0,max:999});
  int(vehicle.hullDamage,'Danos no casco',{min:0,max:999});
  int(vehicle.systemStrainThreshold,'Limiar de Tensão de sistemas',{min:0,max:999});
  int(vehicle.systemStrain,'Tensão de sistemas',{min:0,max:999});
  for(const key of FACING_KEYS) int(defenseValue(vehicle.defense?.[key]),`Defesa ${key}`,{min:0,max:9});
  if(!Array.isArray(vehicle.weapons)||!Array.isArray(vehicle.criticalHits)||!Array.isArray(vehicle.disabledComponents)) throw new Error('Coleções do veículo inválidas.');
  const effects=vehicle.roundEffects??{};
  if(effects.aorOverwhelmedDefense!==undefined){
    const losses=effects.aorOverwhelmedDefense;
    if(!losses||typeof losses!=='object'||Array.isArray(losses)||
       Object.keys(losses).some(k=>!FACING_KEYS.includes(k)||!Number.isSafeInteger(losses[k])||losses[k]<0||losses[k]>9))
      throw new Error('Reduções de defesa AoR inválidas.');
  }
  if(effects.aorDebilitatedSpeed!==undefined&&
     (!effects.aorDebilitatedSpeed||!Number.isSafeInteger(effects.aorDebilitatedSpeed.untilRound)||
      effects.aorDebilitatedSpeed.untilRound<1)) throw new Error('Debilitação veicular AoR inválida.');
  return true;
}

export function createVehicleFromProfile(profileKey,{id=null,name=null}={}){
  const p=STARTER_VEHICLE_PROFILES[profileKey]??SWE09_ADVENTURE_VEHICLE_PROFILES[profileKey]??SWE02_ADVENTURE_VEHICLE_PROFILES[profileKey]; if(!p) throw new Error('Perfil de nave/veículo inválido.');
  const v={...clone(p),id:id??`veh_${profileKey}`,name:name??p.name,speed:0,hullDamage:0,systemStrain:0,criticalHits:[],disabledComponents:[],tractorLocks:[],roundEffects:{evasive:false,stayOnTarget:false,enfiladeTargetId:null,controlledFire:false},lastSystemStrainAt:null};
  validateVehicle(v); return v;
}

export function vehicleStatus(vehicle,{operator=null,currentRound=null}={}){
  validateVehicle(vehicle);
  const mods=vehicleCriticalModifiers(vehicle,{currentRound});
  const baseHullThreshold=Math.max(0,vehicle.hullThreshold);
  const baseSystemThreshold=Math.max(0,vehicle.systemStrainThreshold);
  const hullThreshold=mods.thresholdsHalved?Math.floor(baseHullThreshold/2):baseHullThreshold;
  const systemStrainThreshold=mods.thresholdsHalved?Math.floor(baseSystemThreshold/2):baseSystemThreshold;
  const hullDisabled=vehicle.hullDamage>hullThreshold;
  const systemsShutdown=vehicle.systemStrain>systemStrainThreshold;
  const debilitation=vehicle.roundEffects?.aorDebilitatedSpeed;
  const speedPenalty=debilitation && Number.isInteger(currentRound) &&
    currentRound<=debilitation.untilRound ? 1 : 0;
  const baseSpeedMax=mods.enginesDown?0:Math.max(0,vehicle.speedMax-mods.speedMaxLoss);
  const speedMax=baseSpeedMax===0?0:Math.max(1,baseSpeedMax-speedPenalty);
  const temporary=vehicleTemporaryEffects(vehicle,{currentRound});
  const defense=Object.fromEntries(FACING_KEYS.map(key=>{
    if(systemsShutdown||mods.defenseZero.includes(key)) return [key,0];
    const base=defenseValue(vehicle.defense?.[key]);
    const shieldBonus=temporary.boostShields?.zone===key?1:0;
    const sliceLoss=temporary.slicedDefense?.zone===key?1:0;
    const overwhelmed=Number(vehicle.roundEffects?.aorOverwhelmedDefense?.[key]??0);
    return [key,Math.max(0,base-Number(mods.defenseLoss[key]??0)+(operator?talentPilotedVehicleDefenseBonus(operator):0)+shieldBonus-sliceLoss-overwhelmed)];
  }));
  const destroyed=Boolean(mods.destroyed);
  const tractorDifficulty=tractorLockDifficulty(vehicle);
  return {
    hullDisabled, systemsShutdown, destroyed, breakingUp:mods.breakingUp,
    tractorLocked:tractorDifficulty>0, tractorDifficulty,
    operational:!hullDisabled&&!destroyed,
    effectiveHullThreshold:hullThreshold,effectiveSystemStrainThreshold:systemStrainThreshold,
    effectiveArmor:Math.max(0,vehicle.armor-mods.armorLoss),
    aorDebilitatedSpeedActive:speedPenalty>0,
    effectiveSpeedMax:systemsShutdown||destroyed?0:speedMax,
    effectiveSpeed:systemsShutdown||destroyed?0:(mods.enginesDown?vehicle.speed:Math.min(vehicle.speed,speedMax)),
    defense,
    weaponsOnline:!systemsShutdown&&!destroyed,
    noManeuvers:mods.noManeuvers,
    firingSetback:mods.firingSetback,
    hyperspaceDisabled:mods.hyperspaceDisabled,
    navigationDisabled:mods.navigationDisabled,
    cannotVoluntarySystemStrain:mods.cannotVoluntarySystemStrain,
    disabledComponents:mods.disabledComponents,
    temporaryDisabledComponents:mods.temporaryDisabledComponents,
    depressurizing:mods.depressurizing,
    depressurizeRounds:mods.depressurizeRounds,
    fireActive:mods.fireActive, temporaryEffects:temporary,
    label:destroyed?'Destruído':hullDisabled?'Inutilizado':systemsShutdown?'Sistemas desconectados':mods.breakingUp?'Desintegrando':'Operacional'
  };
}

export function handlingPoolModifier(vehicle){ validateVehicle(vehicle); return vehicle.handling>=0?{boost:vehicle.handling,setback:0}:{boost:0,setback:Math.abs(vehicle.handling)}; }

export function applyVehicleScaleDamage(vehicle,{damage,breach=0}={}){
  validateVehicle(vehicle); const next=clone(vehicle);
  const raw=Math.max(0,int(damage,'Dano',{min:0,max:999}));
  const effectiveArmor=Math.max(0,vehicleStatus(next).effectiveArmor-int(breach,'Brecha',{min:0,max:99}));
  const suffered=Math.max(0,raw-effectiveArmor);
  next.hullDamage+=suffered;
  return {vehicle:next,suffered,effectiveArmor,status:vehicleStatus(next)};
}

export function applyVehicleIonDamage(vehicle,{damage,breach=0,at=new Date().toISOString()}={}){
  validateVehicle(vehicle); const next=clone(vehicle);
  const raw=Math.max(0,int(damage,'Dano iônico',{min:0,max:999}));
  const effectiveArmor=Math.max(0,vehicleStatus(next).effectiveArmor-int(breach,'Brecha',{min:0,max:99}));
  const suffered=Math.max(0,raw-effectiveArmor);
  next.systemStrain+=suffered;
  if(suffered>0) next.lastSystemStrainAt=at;
  return {vehicle:next,suffered,effectiveArmor,status:vehicleStatus(next),damageType:'systemStrain'};
}

export function applyVehicleSystemStrain(vehicle,amount,{at=new Date().toISOString()}={}){
  validateVehicle(vehicle); const next=clone(vehicle); const n=int(amount,'Tensão de sistemas',{min:0,max:999}); next.systemStrain+=n; if(n>0) next.lastSystemStrainAt=at; return {vehicle:next,suffered:n,status:vehicleStatus(next)};
}

export function recoverVehicleSystemsAfterFullDay(vehicle,{sufferedNewStrain=false}={}){
  validateVehicle(vehicle); const next=clone(vehicle); if(!sufferedNewStrain) next.systemStrain=Math.max(0,next.systemStrain-1); return {vehicle:next,recovered:vehicle.systemStrain-next.systemStrain};
}

export function planetaryScaleToPersonal(value){ return Number(value)*10; }
export function personalScaleToPlanetary(value){ return Number(value)/10; }

export function accelerateOrBrake(vehicle,delta){
  validateVehicle(vehicle); const d=Number(delta); if(![-1,1].includes(d)) throw new Error('Acelerar/Frear altera a Velocidade em exatamente 1.');
  const status=vehicleStatus(vehicle); if(status.noManeuvers) throw new Error('Um Critical Hit impede manobras da nave/veículo até ser resolvido ou reparado.');
  const next=clone(vehicle); next.speed=Math.max(0,Math.min(status.effectiveSpeedMax,next.speed+d)); return next;
}

export function punchIt(vehicle){
  validateVehicle(vehicle); if(vehicle.silhouette<1||vehicle.silhouette>4) throw new Error('Pisar a fundo exige Silhueta de 1 a 4.');
  const status=vehicleStatus(vehicle);
  if(status.noManeuvers) throw new Error('Um Critical Hit impede manobras da nave/veículo.');
  if(status.cannotVoluntarySystemStrain) throw new Error('Power Fluctuations impede sofrer Tensão de sistemas voluntariamente.');
  const next=clone(vehicle); const strain=Math.max(0,status.effectiveSpeedMax-next.speed); next.speed=status.effectiveSpeedMax; next.systemStrain+=strain; return {vehicle:next,strain,status:vehicleStatus(next)};
}

export function rangeStepsForSpeed(speed){ const s=int(speed,'Velocidade',{min:0,max:9}); if(s===0) return 0; if(s===1) return 1; if(s<=4) return 2; return 3; }

export function movePlanetaryRange(currentRange,{direction='closer',speed=0}={}){
  let i=rangeIndex(currentRange); const steps=rangeStepsForSpeed(speed); if(steps===0) throw new Error('Velocidade 0: a nave não pode usar Avançar.');
  const sign=direction==='closer'?-1:direction==='farther'?1:0; if(!sign) throw new Error('Direção de movimento inválida.');
  i=Math.max(0,Math.min(PLANETARY_RANGE_BANDS.length-1,i+sign*steps)); return PLANETARY_RANGE_BANDS[i];
}

export function canUseEvasiveManeuvers(vehicle){ return vehicle.silhouette>=1&&vehicle.silhouette<=4&&vehicle.speed>=3; }
export function canStayOnTarget(vehicle){ return vehicle.silhouette>=1&&vehicle.silhouette<=4&&vehicle.speed>=3; }
export function canEnfilade(vehicle){ return vehicle.silhouette>=1&&vehicle.silhouette<=4&&vehicle.speed>=4; }

export function setEvasiveManeuvers(vehicle,active=true){ validateVehicle(vehicle); if(active&&!canUseEvasiveManeuvers(vehicle)) throw new Error('Manobras evasivas exigem Silhueta 1–4 e Velocidade 3+.'); const next=clone(vehicle); next.roundEffects.evasive=Boolean(active); return next; }
export function setStayOnTarget(vehicle,active=true){ validateVehicle(vehicle); if(active&&!canStayOnTarget(vehicle)) throw new Error('Seguir o objetivo exige Silhueta 1–4 e Velocidade 3+.'); const next=clone(vehicle); next.roundEffects.stayOnTarget=Boolean(active); return next; }

export function stopShort(vehicle){
  validateVehicle(vehicle); const next=clone(vehicle); const reduced=next.speed; next.speed=0; next.systemStrain+=reduced; return {vehicle:next,systemStrain:reduced,status:vehicleStatus(next)};
}

export function setControlledFire(vehicle,active=true){
  validateVehicle(vehicle); const next=clone(vehicle); next.roundEffects={...(next.roundEffects??{}),controlledFire:Boolean(active)}; return next;
}

export function attemptFullThrottle(vehicle,character,{skillKey='pilotar_vehiculo_espacial',asManeuver=false,currentRound=1,rng=Math.random,difficultyDowngrades=0,difficultyUpgrades=0}={}){
  validateVehicle(vehicle);
  const spec=fullThrottleTalentSpec(character);
  if(!spec.available) throw new Error('O personagem não possui Aceleração máxima.');
  if(asManeuver&&!spec.improved) throw new Error('Aceleração máxima como manobra exige Aceleração máxima aprimorada.');
  const characteristic=Number(character?.characteristics?.agilidad??0);
  const rank=Number(character?.skills?.[skillKey]??0);
  const positive=buildPositivePool(characteristic,rank);
  const difficulty=asManeuver?spec.maneuverDifficulty:spec.actionDifficulty;
  const upgraded=upgradeDifficultyPool(difficulty,0,difficultyUpgrades);
  const negative=downgradeDifficultyPool(upgraded.difficulty,upgraded.challenge,difficultyDowngrades);
  const check=rollPool({...positive,...negative,boost:0,setback:0},rng);
  const next=clone(vehicle);
  const rounds=Math.max(1,Number(character?.characteristics?.astucia??1));
  if(check.success){
    const baseMax=Number(STARTER_VEHICLE_PROFILES[next.key]?.speedMax ?? next.speedMax);
    next.speedMax=baseMax+spec.bonus;
    next.roundEffects={...(next.roundEffects??{}),fullThrottle:{bonus:spec.bonus,expiresAfterRound:Number(currentRound)+rounds-1}};
  }
  return {vehicle:next,check,difficulty:negative.difficulty,baseDifficulty:difficulty,difficultyDowngrades:Number(difficultyDowngrades)||0,difficultyUpgrades,bonus:check.success?spec.bonus:0,durationRounds:check.success?rounds:0,personalStrainCost:asManeuver?spec.improvedStrainCost:0,spec};
}

export function expireFullThrottle(vehicle,currentRound){
  validateVehicle(vehicle);
  const next=clone(vehicle);
  const effect=next.roundEffects?.fullThrottle;
  if(!effect) return {vehicle:next,expired:false};
  if(Number(currentRound)<=Number(effect.expiresAfterRound)) return {vehicle:next,expired:false};
  const baseMax=Number(STARTER_VEHICLE_PROFILES[next.key]?.speedMax ?? Math.max(0,next.speedMax-Number(effect.bonus||0)));
  next.speedMax=baseMax;
  next.speed=Math.min(next.speed,next.speedMax);
  delete next.roundEffects.fullThrottle;
  return {vehicle:next,expired:true};
}

export function gunneryDifficultyBySilhouette(attackerSilhouette,targetSilhouette){
  const a=int(attackerSilhouette,'Silhueta atacante',{min:0,max:10}), t=int(targetSilhouette,'Silhueta alvo',{min:0,max:10});
  const diff=a-t;
  if(Math.abs(diff)<=1) return 2;
  if(diff<=-2) return 1;
  if(diff===2) return 3;
  return 4;
}

export function enfiladeDifficulty(attackerSpeed,targetSpeed){
  const a=int(attackerSpeed,'Velocidade atacante',{min:0,max:9}), t=int(targetSpeed,'Velocidade alvo',{min:0,max:9});
  if(a===t) return 2; if(a>t) return 1; if(t-a===1) return 3; return 4;
}

export function gainAdvantageDifficulty(attackerSpeed,targetSpeed,repeatCount=0){
  return Math.min(5,enfiladeDifficulty(attackerSpeed,targetSpeed)+Math.max(0,Number(repeatCount)||0));
}
export function canGainAdvantage(vehicle){ validateVehicle(vehicle); return vehicle.silhouette>=1&&vehicle.silhouette<=4&&vehicle.speed>=4; }
export function applyGainAdvantage(vehicle,{targetVehicleId,currentRound=1,zone='proa'}={}){
  validateVehicle(vehicle); if(!canGainAdvantage(vehicle)) throw new Error('Ganhar a Vantagem exige Silhueta 1–4 e Velocidade 4+.');
  if(!targetVehicleId) throw new Error('Ganhar a Vantagem exige um alvo.'); if(!FACING_KEYS.includes(zone)) throw new Error('Zona de defesa inválida.');
  const next=clone(vehicle); const round=Math.max(1,Number(currentRound)||1);
  next.roundEffects={...(next.roundEffects??{}),gainAdvantage:{targetVehicleId:String(targetVehicleId),zone,startsAtRound:round,expiresAfterRound:round+1}};
  return next;
}
export function applyBoostShields(vehicle,{zone='proa',currentRound=1,durationRounds=1}={}){
  validateVehicle(vehicle); if(!FACING_KEYS.includes(zone)) throw new Error('Zona de defesa inválida.');
  if(currentDefenseTotal(vehicle)<=0) throw new Error('Reforçar Escudos exige que o veículo já possua defesa.');
  const status=vehicleStatus(vehicle,{currentRound}); if(status.cannotVoluntarySystemStrain) throw new Error('Power Fluctuations impede sofrer Tensão de sistemas voluntariamente.');
  const next=clone(vehicle); const round=Math.max(1,Number(currentRound)||1), duration=Math.max(1,Number(durationRounds)||1);
  next.systemStrain+=1; next.lastSystemStrainAt=new Date().toISOString();
  next.roundEffects={...(next.roundEffects??{}),boostShields:{zone,startsAtRound:round,expiresAfterRound:round+duration-1}};
  return next;
}
export function applySlicedDefense(vehicle,{zone='proa',currentRound=1,durationRounds=1}={}){
  validateVehicle(vehicle); if(!FACING_KEYS.includes(zone)) throw new Error('Zona de defesa inválida.');
  const next=clone(vehicle); const round=Math.max(1,Number(currentRound)||1), duration=Math.max(1,Number(durationRounds)||1);
  next.roundEffects={...(next.roundEffects??{}),slicedDefense:{zone,startsAtRound:round,expiresAfterRound:round+duration-1}};
  return next;
}
export function applySpoofingMissiles(vehicle,{upgrades=1,currentRound=1}={}){
  validateVehicle(vehicle); const round=Math.max(1,Number(currentRound)||1), n=Math.max(1,Number(upgrades)||1); const next=clone(vehicle);
  next.roundEffects={...(next.roundEffects??{}),spoofingMissiles:{upgrades:n,startsAtRound:round,expiresAfterRound:round}};
  return next;
}
export function applyJamming(vehicle,{targetVehicleId=null,range='Curto',currentRound=1,durationRounds=1,channels=['comms','sensors']}={}){
  validateVehicle(vehicle);
  const round=Math.max(1,Number(currentRound)||1), duration=Math.max(1,Number(durationRounds)||1);
  const next=clone(vehicle);
  next.roundEffects={
    ...(next.roundEffects??{}),
    jamming:{
      targetVehicleId:targetVehicleId?String(targetVehicleId):null,
      range,
      channels:Array.isArray(channels)?channels:['comms'],
      startsAtRound:round,
      expiresAfterRound:round+duration-1
    }
  };
  return next;
}
export function vehicleTemporaryEffects(vehicle,{currentRound=null}={}){
  validateVehicle(vehicle); const e=vehicle.roundEffects??{};
  return {
    gainAdvantage:timedRoundEffectActive(e.gainAdvantage,currentRound)?clone(e.gainAdvantage):null,
    boostShields:timedRoundEffectActive(e.boostShields,currentRound)?clone(e.boostShields):null,
    slicedDefense:timedRoundEffectActive(e.slicedDefense,currentRound)?clone(e.slicedDefense):null,
    spoofingMissiles:timedRoundEffectActive(e.spoofingMissiles,currentRound)?clone(e.spoofingMissiles):null,
    jamming:timedRoundEffectActive(e.jamming,currentRound)?clone(e.jamming):null,
    copilot:e.copilot?clone(e.copilot):null, plotCourse:e.plotCourse?clone(e.plotCourse):null, fireDiscipline:e.fireDiscipline?clone(e.fireDiscipline):null
  };
}

export function pilotingHazardDifficulty(vehicle){
  validateVehicle(vehicle); const half=Math.ceil(vehicle.silhouette/2); return {difficulty:Math.max(vehicle.speed,half),upgrades:Math.min(vehicle.speed,half)};
}

function damageSeverity(current,threshold){ if(current>threshold) return 3; if(current>=threshold/2) return 2; return 1; }
export function damageControlDifficulty(vehicle,{mode='system'}={}){
  validateVehicle(vehicle);
  if(mode==='system') return damageSeverity(vehicle.systemStrain,vehicle.systemStrainThreshold);
  if(mode==='hull') return damageSeverity(vehicle.hullDamage,vehicle.hullThreshold);
  throw new Error('Modo de Controle de Danos inválido.');
}

export function applyDamageControl(vehicle,{successes=0,operator=null,mode='system'}={}){
  validateVehicle(vehicle);
  const s=int(successes,'Êxitos',{min:0,max:99});
  if(!['system','hull'].includes(mode)) throw new Error('Modo de Controle de Danos inválido.');
  const tuneUp=operator?talentRankByOriginal(operator,'FINETUN'):0;
  const efficientRepair=s>0&&operator?sourceBackedTalentRank(operator,'SOLREP'):0;
  const next=clone(vehicle);
  const beforeStrain=next.systemStrain,beforeHull=next.hullDamage;
  if(mode==='system') next.systemStrain=Math.max(0,next.systemStrain-(s+tuneUp));
  else next.hullDamage=Math.max(0,next.hullDamage-(s+efficientRepair));
  return {vehicle:next,mode,systemStrainRecovered:beforeStrain-next.systemStrain,hullRecovered:beforeHull-next.hullDamage,talentBonuses:{systemStrain:mode==='system'?tuneUp:0,hull:mode==='hull'?efficientRepair:0}};
}

export function applyManualRepairs(vehicle,{successes=0,operator=null}={}){
  validateVehicle(vehicle);
  const s=int(successes,'Êxitos',{min:0,max:99});
  const next=clone(vehicle),beforeHull=next.hullDamage;
  const talentBonus=s>0&&operator?sourceBackedTalentRank(operator,'SOLREP'):0;
  const recovered=s>0?1+Math.floor(Math.max(0,s-1)/2)+talentBonus:0;
  next.hullDamage=Math.max(0,next.hullDamage-recovered);
  return {vehicle:next,hullRecovered:beforeHull-next.hullDamage,systemStrainRecovered:0,mode:'manual_hull'};
}

export function applyVehicleCritical(vehicle,{roll=null,rng=Math.random,modifier=0,component=null,defenseZone='proa',inAtmosphere=false,currentRound=0}={}){
  validateVehicle(vehicle);
  if(!FACING_KEYS.includes(defenseZone)) throw new Error('Zona de Defesa inválida.');
  const next=clone(vehicle);
  const rawRoll=roll===null?Math.floor(Math.max(0,Math.min(0.999999999999,Number(rng())))*100)+1:int(roll,'Rolagem de Critical Hit',{min:1,max:100});
  const existing=unrepairedVehicleCriticalCount(next);
  const externalModifier=Math.trunc(Number(modifier)||0);
  const total=rawRoll+(existing*10)+externalModifier;
  let row=vehicleCriticalResult(total);
  if(row.key==='major_hull_breach'&&inAtmosphere) row=VEHICLE_CRITICAL_HIT_TABLE.find(x=>x.key==='destabilized');
  const currentStatus=vehicleStatus(next,{currentRound});
  const number=next.criticalHits.length+1;
  const hit={
    number,key:row.key,title:row.title,severity:row.severity,repairDifficulty:row.repairDifficulty,
    rawRoll,existingCriticalModifier:existing*10,externalModifier,total,
    effect:'',repaired:false,effectExpired:false,component:component??null,defenseZone,
    source:'EOTE_CORE_TABLE_7_9'
  };
  let requiresComponentChoice=false;
  switch(row.key){
    case 'mechanical_stress':
      next.systemStrain+=1; hit.effect='Sofre imediatamente 1 de Tensão de sistemas.'; break;
    case 'jostled':
      hit.crewStrain=1; hit.crewDisorientedRounds=1; hit.effect='Toda a tripulação sofre 1 de Tensão e fica Desorientada por 1 assalto.'; break;
    case 'losing_power_to_shields': {
      const noDefense=Object.values(currentStatus.defense).every(v=>v<=0);
      hit.fallbackSystemStrain=noDefense;
      if(noDefense) next.systemStrain+=1;
      hit.effect=noDefense?'Sem Defesa ativa: sofre 1 de Tensão de sistemas.':`Defesa em ${defenseZone} reduzida em 1 até reparo.`;
      break;
    }
    case 'knocked_off_course':
      hit.effectExpiresAfterRound=Number(currentRound)+1;
      hit.pilotRecoveryDifficulty=Math.max(0,Number(vehicle.speed)||0);
      hit.effect='No próximo turno, o piloto não executa manobras até recuperar o curso com Pilotagem.'; break;
    case 'tailspin':
      hit.effectExpiresAfterRound=Number(currentRound)+1;
      hit.crewDisorientedRounds=1; hit.effect='Disparos sofrem 2 Contratempos até o fim do próximo turno do piloto; tripulação fica Desorientada por 1 assalto.'; break;
    case 'component_hit':
      requiresComponentChoice=!component;
      hit.effectExpiresAfterRound=Number(currentRound)+1;
      hit.effect=component?`Componente temporariamente inoperante: ${component}.`:'O atacante deve escolher um componente, inoperante até o fim do próximo assalto.';
      break;
    case 'shields_failing': {
      const noDefense=Object.values(currentStatus.defense).every(v=>v<=0);
      hit.fallbackSystemStrain=noDefense;
      if(noDefense) next.systemStrain+=2;
      hit.effect=noDefense?'Sem Defesa ativa: sofre 2 de Tensão de sistemas.':'Defesa de todas as zonas reduzida em 1 até reparo.';
      break;
    }
    case 'hyperdrive_nav_failure':
      hit.effect=vehicle.hyperdrive?'Hiperimpulsor/computador de navegação falha: saltos ao hiperespaço bloqueados até reparo.':'Sistemas de navegação falham até reparo.'; break;
    case 'power_fluctuations':
      hit.effect='O piloto não pode infligir Tensão de sistemas voluntariamente até reparo.'; break;
    case 'shields_down': {
      const noDefense=Object.values(currentStatus.defense).every(v=>v<=0);
      hit.fallbackSystemStrain=noDefense;
      if(noDefense) next.systemStrain+=4;
      hit.effect=noDefense?'Sem Defesa ativa: sofre 4 de Tensão de sistemas.':`Defesa em ${defenseZone} cai a 0; demais zonas perdem 1 até reparo.`;
      break;
    }
    case 'engine_damaged':
      hit.effect='Velocidade máxima reduzida em 1, até o mínimo de 1, até reparo.'; break;
    case 'shield_overload': {
      const noDefense=Object.values(currentStatus.defense).every(v=>v<=0);
      hit.armorFallback=noDefense;
      next.systemStrain+=2;
      hit.repairAfterEncounter=true;
      hit.effect=noDefense?'Sofre 2 de Tensão de sistemas e Blindagem -1 até reparo.':'Defesa de todas as zonas cai a 0 e sofre 2 de Tensão de sistemas; reparo só após o encontro.';
      break;
    }
    case 'engines_down':
      hit.effect='Velocidade máxima efetiva 0; nenhuma manobra até reparo; mantém o curso e a velocidade atuais.'; break;
    case 'major_system_failure':
      requiresComponentChoice=!component;
      hit.effect=component?`Componente inoperante até reparo: ${component}.`:'O atacante deve escolher um componente, inoperante até reparo.';
      break;
    case 'major_hull_breach':
      hit.depressurizeRounds=Math.max(1,Number(vehicle.silhouette)||1);
      hit.effect=vehicle.silhouette<=4?`A nave despressuriza completamente em ${hit.depressurizeRounds} assalto(s).`:`Compartimentos atingidos despressurizam em ${hit.depressurizeRounds} assalto(s), a critério do Mestre.`;
      break;
    case 'destabilized':
      hit.effect='Limiar de Danos no casco e Limiar de Tensão de sistemas reduzidos à metade até reparo.'; break;
    case 'fire':
      next.systemStrain+=2; hit.fireRounds=Math.max(1,Math.ceil(Number(vehicle.silhouette)/2)); hit.effect=`Incêndio: sofre 2 de Tensão de sistemas; extinguir leva ${hit.fireRounds} assalto(s) com perícia apropriada.`; break;
    case 'breaking_up':
      hit.destroyAfterRound=Number(currentRound)+1; hit.effect='A nave será destruída ao fim do próximo assalto; tripulação tem um assalto para escapar.'; break;
    case 'vaporized':
      hit.destroyed=true; hit.effect='Nave/veículo completamente destruído; nada sobrevive.'; break;
  }
  next.criticalHits.push(hit);
  return {vehicle:next,hit,requiresComponentChoice,status:vehicleStatus(next,{currentRound}),table:'EOTE_CORE_TABLE_7_9'};
}

export function repairVehicleCritical(vehicle,index,{encounterEnded=true}={}){
  validateVehicle(vehicle);
  const next=clone(vehicle);
  const i=int(index,'Índice do Critical Hit',{min:0,max:next.criticalHits.length-1});
  const hit=next.criticalHits[i];
  if(!hit) throw new Error('Critical Hit inexistente.');
  if(hit.repaired) return next;
  if(hit.key==='vaporized'||hit.destroyed) throw new Error('Uma nave vaporizada/destruída não pode ser reparada.');
  if(hit.repairAfterEncounter&&!encounterEnded) throw new Error('Este Critical Hit só pode ser reparado após o fim do encontro.');
  hit.repaired=true;
  hit.effectExpired=true;
  return next;
}

function upgradePositivePool(pool, upgrades=0){
  let ability=Math.max(0,Number(pool?.ability??0)), proficiency=Math.max(0,Number(pool?.proficiency??0));
  let n=Math.max(0,Number(upgrades)||0);
  while(n-->0){ if(ability>0){ ability-=1; proficiency+=1; } else ability+=1; }
  return {ability,proficiency};
}

function upgradeDifficultyPool(difficulty, challenge=0, upgrades=0){
  let d=Math.max(0,Number(difficulty)||0), c=Math.max(0,Number(challenge)||0), n=Math.max(0,Number(upgrades)||0);
  while(n-->0){ if(d>0){ d-=1; c+=1; } else d+=1; }
  return {difficulty:d,challenge:c};
}

export function vehicleAttackPool({character,attacker,target,targetOperator=null,facing='proa',weaponIndex=0,targetRange='Bocajarro',rng=Math.random,extraBoost=0,extraSetback=0,currentRound=null,ignoreMassive=false,deadToRightsBonus=0,allVehicles=null}={}){
  validateVehicle(attacker); validateVehicle(target); if(!character) throw new Error('Atacante sem personagem/operador.');
  const weapon=attacker.weapons?.[weaponIndex]; if(!weapon) throw new Error('Arma de veículo inválida.');
  if(vehicleStatus(attacker).weaponsOnline===false) throw new Error('As armas estão offline por sobrecarga dos sistemas.');
  if(rangeIndex(targetRange)>rangeIndex(weapon.range)) throw new Error(`Alvo fora do alcance desta arma (${weapon.range}).`);
  const rank=Number(character.skills?.[weapon.skill] ?? 0), characteristic=Number(character.characteristics?.agilidad ?? 0);
  const positiveUpgrades=(attacker.roundEffects?.stayOnTarget?1:0)+(target.roundEffects?.stayOnTarget?1:0);
  const positive=upgradePositivePool(buildPositivePool(characteristic,rank),positiveUpgrades);
  const pilotedTargetSilhouette=targetOperator?talentPilotedVehicleSilhouette(targetOperator,target.silhouette):target.silhouette;
  const controlledFireBonus=attacker.roundEffects?.controlledFire?1:0;
  const targetSilhouette=Math.min(10,pilotedTargetSilhouette+controlledFireBonus);
  const baseDifficulty=gunneryDifficultyBySilhouette(attacker.silhouette,targetSilhouette);
  const attackerEffects=vehicleTemporaryEffects(attacker,{currentRound}), targetEffects=vehicleTemporaryEffects(target,{currentRound});
  const gained=attackerEffects.gainAdvantage;
  const leaderId=gained?.formationLeaderVehicleId??null;
  const formationValid=!leaderId||Boolean(allVehicles?.[leaderId]&&attacker.alliedToPlayer===true&&attacker.closeToActive===true&&
    String(vehicleTemporaryEffects(allVehicles[leaderId],{currentRound}).gainAdvantage?.targetVehicleId??'')===String(target.id));
  const hasAdvantage=formationValid&&String(gained?.targetVehicleId??'')===String(target.id);
  const evasiveUpgrades=hasAdvantage?0:((attacker.roundEffects?.evasive?1:0)+(target.roundEffects?.evasive?1:0));
  const spoofingUpgrades=Number(weapon.qualities?.guided??0)>0?Number(targetEffects.spoofingMissiles?.upgrades??0):0;
  const difficultyUpgrades=evasiveUpgrades+spoofingUpgrades;
  const negative=upgradeDifficultyPool(baseDifficulty,0,difficultyUpgrades);
  const targetDefense=defenseValue(vehicleStatus(target,{operator:targetOperator,currentRound}).defense?.[facing]);
  const boost=Number(extraBoost);
  const setback=Number(extraSetback)+targetDefense+Number(vehicleStatus(attacker,{currentRound}).firingSetback??0);
  const check=rollPool({...positive,...negative,boost,setback},rng);
  const hit=check.success; const nonDamage=Boolean(weapon.nonDamage);
  const ion=Boolean(weapon.qualities?.ion);
  if(typeof ignoreMassive!=='boolean'||!Number.isSafeInteger(deadToRightsBonus)||deadToRightsBonus<0||deadToRightsBonus>7)
    throw Error('Opções de dano veicular inválidas.');
  const massiveRating=Number(target.massive??0),massiveIgnored=ignoreMassive?massiveRating:0;
  const criticalCost=Number(weapon.crit)+massiveRating-massiveIgnored;
  const damage=hit&&!nonDamage?Number(weapon.damage??0)+check.netSuccess+deadToRightsBonus:0;
  const criticalAvailable=hit && !nonDamage && !ion && Number.isFinite(Number(weapon.crit)) && (check.netAdvantage>=criticalCost || check.triumph>0);
  const specialEffect=hit&&Number(weapon.qualities?.tractor??0)>0?{type:'tractor',rating:Number(weapon.qualities.tractor),sourceId:attacker.id}:null;
  return {weapon,check,hit,damage,nonDamage,ion,specialEffect,breach:Number(weapon.qualities?.breach ?? 0),guided:Number(weapon.qualities?.guided ?? 0),linked:Number(weapon.qualities?.linked ?? 0),limitedAmmo:Number(weapon.qualities?.limitedAmmo ?? 0),slowFiring:Number(weapon.qualities?.slowFiring ?? 0),criticalAvailable,criticalCost,massiveRating,massiveIgnored,deadToRightsBonus:hit?deadToRightsBonus:0,difficulty:negative.difficulty,challenge:negative.challenge,baseDifficulty,targetDefense,targetRange,targetSilhouette,pilotedTargetSilhouette,controlledFireBonus,positiveUpgrades,difficultyUpgrades,hasAdvantage,spoofingUpgrades};
}

export function guidedAttackPool({target,targetOperator=null,guidedRating=0,facing='proa',rng=Math.random,currentRound=null}={}){
  validateVehicle(target);
  const rating=int(guidedRating,'Rating de Guiada',{min:1,max:9});
  const targetSilhouette=targetOperator?talentPilotedVehicleSilhouette(targetOperator,target.silhouette):target.silhouette;
  const baseDifficulty=gunneryDifficultyBySilhouette(0,targetSilhouette);
  const targetDefense=defenseValue(vehicleStatus(target,{operator:targetOperator,currentRound}).defense?.[facing]);
  const spoofingUpgrades=Number(vehicleTemporaryEffects(target,{currentRound}).spoofingMissiles?.upgrades??0);
  const negative=upgradeDifficultyPool(baseDifficulty,0,spoofingUpgrades);
  const check=rollPool({ability:rating,proficiency:0,...negative,boost:0,setback:targetDefense},rng);
  return {check,hit:check.success,guidedRating:rating,baseDifficulty,targetDefense,targetSilhouette,difficulty:negative.difficulty,challenge:negative.challenge,spoofingUpgrades};
}

export function resolveGuidedAttack({target,weapon,guidedRating=0,targetOperator=null,facing='proa',rng=Math.random,currentRound=null}={}){
  if(!weapon) throw new Error('Projétil Guiado sem arma de origem.');
  const guided=guidedAttackPool({target,targetOperator,guidedRating,facing,rng,currentRound});
  const damage=guided.hit?Number(weapon.damage??0)+Math.max(0,Number(guided.check.netSuccess??0)):0;
  const breach=Number(weapon.qualities?.breach??0);
  const ion=Boolean(weapon.qualities?.ion);
  const targetDamage=guided.hit?(ion?applyVehicleIonDamage(target,{damage,breach}):applyVehicleScaleDamage(target,{damage,breach})):null;
  return {...guided,weapon:clone(weapon),damage,breach,ion,targetDamage,reactivationAvailable:!guided.hit&&Number(guided.check.netAdvantage??0)>=3};
}

export function resolveVehicleWeaponAttack(args={}){
  const attack=vehicleAttackPool(args);
  if(!attack.hit) return {...attack,targetDamage:null,targetEffect:null};
  if(attack.specialEffect?.type==='tractor'){
    const vehicle=applyTractorLock(args.target,{rating:attack.specialEffect.rating,sourceId:attack.specialEffect.sourceId});
    return {...attack,targetDamage:null,targetEffect:{type:'tractor',rating:attack.specialEffect.rating,vehicle}};
  }
  const targetDamage=attack.ion?applyVehicleIonDamage(args.target,{damage:attack.damage,breach:attack.breach}):applyVehicleScaleDamage(args.target,{damage:attack.damage,breach:attack.breach});
  return {...attack,targetDamage,targetEffect:attack.ion?{type:'ion',systemStrain:targetDamage.suffered,vehicle:targetDamage.vehicle}:null};
}

export function createVehicleRuntime(){ return {schemaVersion:1,activeVehicleId:null,vehicles:{},combat:{active:false,round:0,targetVehicleId:null,range:'Curto',actionUsed:false,maneuversUsed:0,lastAttack:null,pendingTargetCritical:null,pendingVehicleSpend:null,guidedProjectiles:[],guidedReactivations:[],log:[]}}; }

export function normalizeVehicleRuntime(value){
  const base=createVehicleRuntime(); const next={...base,...clone(value??{}),vehicles:{...(value?.vehicles??{})},combat:{...base.combat,...clone(value?.combat??{})}};
  for(const [id,raw] of Object.entries(next.vehicles)){
    const v={...raw,criticalHits:Array.isArray(raw?.criticalHits)?raw.criticalHits:[],disabledComponents:Array.isArray(raw?.disabledComponents)?raw.disabledComponents:[],tractorLocks:Array.isArray(raw?.tractorLocks)?raw.tractorLocks:[],roundEffects:{evasive:false,stayOnTarget:false,enfiladeTargetId:null,controlledFire:false,...(raw?.roundEffects??{})}};
    next.vehicles[id]=v;
    try{ validateVehicle(v); }catch{ delete next.vehicles[id]; }
  }
  if(next.activeVehicleId&&!next.vehicles[next.activeVehicleId]) next.activeVehicleId=null;
  if(next.combat.targetVehicleId&&!next.vehicles[next.combat.targetVehicleId]) next.combat.targetVehicleId=null;
  return next;
}
