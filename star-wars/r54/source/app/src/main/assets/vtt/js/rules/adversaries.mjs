import { newId } from '../core/ids.mjs';
import { SKILLS } from './character.mjs';
import { criticalInjuryTable } from './critical.mjs';

const aorReference=(source)=>Object.freeze({...source,authority:'CROSS_LINE_REFERENCE',rulesProfile:'AOR_REFERENCE_ONLY',activeRulesAuthority:false});

const BASE_SOURCE = aorReference({
  title:'La Era de la Rebelión - Caja de Inicio - Libro de reglas',
  language:'es',
  section:'VII Adversarios',
  pages:[44,45]
});

const HARD_PURCHASE_SOURCE = aorReference({
  title:'La Era de la Rebelión - Una compra difícil',
  language:'es',
  section:'Llegada a Coruscant / Tiroteo',
  pages:[4,6,8]
});

const UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE = Object.freeze({
  title:'Under a Black Sun',
  system:'Star Wars: Edge of the Empire',
  productCode:'SWEP01',
  authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION',
  note:'Perfil funcional do BRASEIRO para integrar a aventura publicada ao motor de combate; não substitui a ficha editorial.'
});


export const ADVERSARY_PROFILES = Object.freeze({
  imperial_soldier: Object.freeze({
    key:'imperial_soldier',
    name:'Soldado do Exército Imperial',
    kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:3,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_distancia_ligeras','armas_distancia_pesadas','disciplina'],
    skills:{},
    talents:[],
    abilities:[],
    weapons:[
      {name:'Fuzil blaster',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:3},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:2}
    ],
    source:BASE_SOURCE
  }),
  imperial_officer: Object.freeze({
    key:'imperial_officer',
    name:'Oficial do Exército Imperial',
    kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:4,
    woundsThreshold:15,
    strainThreshold:null,
    groupSkills:[],
    skills:{alerta:2,armas_distancia_ligeras:2,conhecimentos:2,disciplina:3},
    talents:[{key:'adversario',name:'Adversário',rank:1}],
    abilities:[
      {key:'direcao_tatica',name:'Direção tática'},
      {key:'fogo_concentrado',name:'Fogo concentrado'}
    ],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3}],
    source:BASE_SOURCE
  }),
  customs_officer: Object.freeze({
    key:'customs_officer',
    name:'Oficial de Controle de Aduana',
    kind:'rival',
    characteristics:{fortaleza:2,agilidad:2,intelecto:3,astucia:3,voluntad:2,presencia:3},
    soak:2,
    woundsThreshold:12,
    strainThreshold:null,
    groupSkills:[],
    skills:{alerta:3,callejeo:3,coaccion:2,disciplina:2,percepcion:3},
    talents:[], abilities:[],
    weapons:[{name:'Pistola blaster leve',skill:'armas_distancia_ligeras',range:'Médio',damage:5,crit:4}],
    source:HARD_PURCHASE_SOURCE
  }),
  stormtrooper: Object.freeze({
    key:'stormtrooper',
    name:'Soldado de Assalto',
    kind:'minion',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:3,presencia:1},
    soak:5,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_cuerpo_a_cuerpo','armas_distancia_pesadas','atletismo','disciplina'],
    skills:{}, talents:[], abilities:[],
    weapons:[
      {name:'Fuzil blaster',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:3},
      {name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:4,crit:2,pierce:2,lethal:1},
      {name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto',damage:8,crit:4,blast:6,blastActivationCost:2,quantity:2}
    ],
    source:HARD_PURCHASE_SOURCE
  }),
  son_barab: Object.freeze({
    key:'son_barab',
    name:'Capitão Son Barab, ISB',
    kind:'nemesis',
    characteristics:{fortaleza:3,agilidad:3,intelecto:3,astucia:3,voluntad:4,presencia:3},
    soak:4,
    woundsThreshold:13,
    strainThreshold:15,
    groupSkills:[],
    skills:{alerta:2,armas_distancia_ligeras:2,callejeo:2,carisma:2,coaccion:2,disciplina:3,engano:2,frialdad:2,liderazgo:2},
    talents:[{key:'adversario',name:'Adversário',rank:1},{key:'golpes_letais',name:'Golpes letais',rank:2}],
    abilities:[],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3}],
    source:HARD_PURCHASE_SOURCE
  })
});


export const PUBLISHED_ADVENTURE_ADVERSARY_PROFILES = Object.freeze({
  // P156: verified against Beyond the Rim, p. 29 (group skills and gear).
  // Numeric characteristic/threshold boxes in the supplied PDF are not yet
  // transcribed and remain explicitly provisional; do not claim full stat certification.
  yiyar_salvager: Object.freeze({
    key:'yiyar_salvager', name:'Integrantes do clã Yiyar', kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:4,woundsThreshold:5,strainThreshold:null,
    groupSkills:['atletismo','disciplina','armas_cuerpo_a_cuerpo','pilotar_vehiculo_planetario','armas_distancia_ligeras','sigilo'],
    skills:{},talents:[],abilities:[],
    weapons:[
      {name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,stunSetting:true},
      {name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:3,crit:3,pierce:2,vicious:1}
    ],
    source:Object.freeze({title:'Beyond the Rim',productCode:'SWE05',printedPage:29,authority:'PARTIALLY_PDF_VERIFIED',verifiedFields:['groupSkills','weapons','armorBonus'],provisionalFields:['characteristics','woundsThreshold','totalSoak']})
  }),
  // The named leader and his equipment/skills are confirmed by the printed
  // description, p. 29. Stat-box attributes remain provisional pending visual transcription.
  talo_yiyar: Object.freeze({
    key:'talo_yiyar',name:'Talo "Too Low" — Yiyar',kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:3,voluntad:3,presencia:2},
    soak:5,woundsThreshold:12,strainThreshold:null,groupSkills:[],
    skills:{atletismo:2,frialdad:1,engano:1,liderazgo:1,armas_cuerpo_a_cuerpo:2,negociacion:2,pilotar_vehiculo_planetario:2,armas_distancia_ligeras:2,alerta:2},
    talents:[{key:'adversario',name:'Adversário',rank:1}],abilities:[],
    weapons:[
      {name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,stunSetting:true},
      {name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:3,crit:3,pierce:2,vicious:1}
    ],
    source:Object.freeze({title:'Beyond the Rim',productCode:'SWE05',printedPage:29,authority:'PARTIALLY_PDF_VERIFIED',verifiedFields:['skills','talents','weapons','armorBonus'],provisionalFields:['characteristics','woundsThreshold','totalSoak']})
  }),
  trandoshan_yiyar_mercenary: Object.freeze({
    key:'trandoshan_yiyar_mercenary',name:'Mercenário trandoshano de Talo',kind:'rival',
    characteristics:{fortaleza:3,agilidad:2,intelecto:2,astucia:2,voluntad:2,presencia:1},
    soak:4,woundsThreshold:13,strainThreshold:null,groupSkills:[],
    skills:{armas_distancia_pesadas:2,armas_cuerpo_a_cuerpo:2,atletismo:2},talents:[],abilities:[],
    weapons:[{name:'Carabina blaster',skill:'armas_distancia_pesadas',range:'Médio',damage:9,crit:3}],
    source:Object.freeze({title:'Beyond the Rim',productCode:'SWE05',printedPage:87,authority:'BRASEIRO_PROVISIONAL_COMBAT_PROFILE',note:'Presença do mercenário confirmada no capítulo 3; números e armamento não cotejados com a ficha gráfica.'})
  }),
  black_sun_enforcer: Object.freeze({
    key:'black_sun_enforcer',
    name:'Agente da Black Sun',
    kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:3,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_distancia_ligeras','sigilo','percepcion'],
    skills:{}, talents:[], abilities:[{key:'perseguidor_urbano',name:'Perseguidor urbano'}],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  kaato_leeachos: Object.freeze({
    key:'kaato_leeachos',
    name:'Kaa’to Leeachos',
    kind:'nemesis',
    characteristics:{fortaleza:3,agilidad:4,intelecto:2,astucia:3,voluntad:3,presencia:2},
    soak:4,
    woundsThreshold:15,
    strainThreshold:13,
    groupSkills:[],
    skills:{alerta:2,armas_distancia_ligeras:3,atletismo:2,frialdad:3,percepcion:2,pilotar_vehiculo_planetario:2,sigilo:2,supervivencia:2},
    talents:[{key:'adversario',name:'Adversário',rank:1}],
    abilities:[{key:'jet_pack',name:'Jet pack e mobilidade vertical'},{key:'cacador_experiente',name:'Caçador experiente'}],
    weapons:[{name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3},{name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:5,crit:2,pierce:2}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  // SWE02, printed p.21 (PDF p.20): official Human and Aqualish Thugs minions.
  // Individual soldiers are tracked only inside their confirmed combat group.
  laoth_drombb_thug: Object.freeze({
    key:'laoth_drombb_thug',name:'Capangas humanos e aqualish de Angu Drombb',kind:'minion',
    characteristics:{fortaleza:3,agilidad:2,intelecto:2,astucia:2,voluntad:1,presencia:1},
    soak:3,woundsThreshold:6,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:['pelea','coaccion','armas_cuerpo_a_cuerpo','armas_distancia_ligeras'],
    skills:{},talents:[],abilities:[],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,stunSetting:true},
      {name:'Soco inglês',skill:'pelea',range:'Engajado',damage:4,crit:4,disorient:3}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:21,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
  // SWE02, printed p.22 (PDF p.21): official Angu Drombb rival statblock.
  laoth_angu_drombb: Object.freeze({
    key:'laoth_angu_drombb',name:'Angu Drombb',kind:'rival',
    characteristics:{fortaleza:3,agilidad:2,intelecto:2,astucia:2,voluntad:2,presencia:3},
    soak:3,woundsThreshold:14,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:[],skills:{pelea:1,coaccion:1,armas_cuerpo_a_cuerpo:1,armas_distancia_ligeras:1},
    talents:[],abilities:[],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,stunSetting:true},
      {name:'Soco inglês',skill:'pelea',range:'Engajado',damage:4,crit:4,disorient:3}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:22,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
  laoth_bounty_hunter: Object.freeze({
    key:'laoth_bounty_hunter',name:'Caçador de recompensas de Teemo',kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:5,woundsThreshold:13,strainThreshold:null,groupSkills:[],
    skills:{pelea:1,coaccion:1,coordinacion:1,armas_distancia_pesadas:1,armas_distancia_ligeras:1,supervivencia:2,alerta:1},
    talents:[{key:'golpe_letal',name:'Lethal Blow',rank:1}],abilities:[],
    weapons:[{name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3,stunSetting:true}],
    source:{title:'Long Arm of the Hutt',productCode:'SWE02',page:16,authority:'SOURCE_CHECKED_PROFILE'}
  }),
  laoth_gand_bounty_hunter: Object.freeze({
    key:'laoth_gand_bounty_hunter',name:'Caçador Gand de Teemo',kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:5,woundsThreshold:13,strainThreshold:null,groupSkills:[],
    skills:{pelea:1,coaccion:1,coordinacion:1,armas_distancia_pesadas:1,armas_distancia_ligeras:1,supervivencia:2,alerta:1},
    talents:[{key:'golpe_letal',name:'Lethal Blow',rank:1}],abilities:[],
    weapons:[{name:'Fuzil blaster',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:3,stunSetting:true},
      {name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3,stunSetting:true}],
    source:{title:'Long Arm of the Hutt',productCode:'SWE02',page:16,authority:'SOURCE_CHECKED_PROFILE'}
  }),
  // SWE02 printed p.41: elite Gamorrean guard, distinct from legacy
  // generic Gamorrean profile (which has different Brawn/Soak/Agility).
  laoth_geonosian_soldier: Object.freeze({
    key:'laoth_geonosian_soldier',name:'Soldado geonosiano AA7',kind:'rival',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:5,woundsThreshold:8,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:[],skills:{pelea:1,armas_distancia_ligeras:1},talents:[],abilities:[],
    weapons:[{name:'Pistola blaster pesada geonosiana',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:2,stunSetting:true}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:37,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
  laoth_vrixxtt_findsman: Object.freeze({
    key:'laoth_vrixxtt_findsman',name:'Vrixx’tt — Gand Findsman',kind:'rival',
    characteristics:{fortaleza:3,agilidad:4,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:5,woundsThreshold:13,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:[],skills:{pelea:1,coaccion:1,coordinacion:1,armas_distancia_pesadas:1,armas_distancia_ligeras:1,supervivencia:2,alerta:1},
    talents:[{key:'golpe_letal',name:'Lethal Blow',rank:1}],abilities:[],
    weapons:[{name:'Fuzil blaster geonosiano',skill:'armas_distancia_pesadas',range:'Longo',damage:9,crit:2,stunSetting:true}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:37,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
  laoth_elite_gamorrean_guard: Object.freeze({
    key:'laoth_elite_gamorrean_guard',name:'Guarda Gamorreano de elite de Teemo',kind:'minion',
    characteristics:{fortaleza:4,agilidad:3,intelecto:1,astucia:1,voluntad:1,presencia:1},
    soak:4,woundsThreshold:8,strainThreshold:null,defense:{melee:0,ranged:0},
    groupSkills:['armas_cuerpo_a_cuerpo'],skills:{},talents:[],abilities:[],
    weapons:[{name:'Machado vibratório Gamorreano rudimentar',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:7,crit:2,pierce:2}],
    source:{title:'The Long Arm of the Hutt',productCode:'SWE02',printedPage:41,authority:'OFFICIAL_PDF_TEXT_VERIFIED'}
  }),
  gamorrean_guard: Object.freeze({
    key:'gamorrean_guard',
    name:'Guarda Gamorreano de Teemo',
    kind:'minion',
    characteristics:{fortaleza:4,agilidad:2,intelecto:1,astucia:2,voluntad:2,presencia:1},
    soak:5,
    woundsThreshold:6,
    strainThreshold:null,
    groupSkills:['pelea','armas_cuerpo_a_cuerpo','coaccion'],
    skills:{}, talents:[], abilities:[],
    weapons:[{name:'Machado vibro',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:7,crit:3,pierce:2}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  teemo_the_hutt: Object.freeze({
    key:'teemo_the_hutt',
    name:'Teemo o Hutt',
    kind:'nemesis',
    characteristics:{fortaleza:5,agilidad:1,intelecto:3,astucia:4,voluntad:4,presencia:3},
    soak:6,
    woundsThreshold:20,
    strainThreshold:18,
    groupSkills:[],
    skills:{callejeo:3,coaccion:3,disciplina:3,engano:3,negociacion:3},
    talents:[{key:'adversario',name:'Adversário',rank:1}],
    abilities:[{key:'resistencia_hutt',name:'Fisiologia Hutt contra venenos e controle mental'}],
    weapons:[{name:'Pistola blaster oculta',skill:'armas_distancia_ligeras',range:'Curto',damage:6,crit:3}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  // SWE09 The Jewel of Yavin, printed pp.12-13; profiles cross-checked
  // against the user's JoY NPC compendium cards (Wing Guard Officer/Sergeant).
  // SWE09 printed p.90, Shen's Goons; stats cross-checked against JoY:87
  // in the user's NPC card compendium (shens_goons_90). Four actual minions,
  // not a guessed generic Bespin patrol or a four-PC scaling rule.
  shens_goons: Object.freeze({
    key:'shens_goons',name:'Capangas de Arend Shen',kind:'minion',
    characteristics:{fortaleza:3,agilidad:3,intelecto:1,astucia:2,voluntad:1,presencia:2},
    soak:4,woundsThreshold:5,strainThreshold:null,
    groupSkills:['pelea','coaccion','artilleria','armas_distancia_pesadas','callejeo'],skills:{},
    talents:[{key:'golpes_letais',name:'Golpes Letais',rank:1}],abilities:[],
    weapons:[
      {name:'Carabina blaster',skill:'armas_distancia_pesadas',range:'Médio',damage:9,crit:3,stunSetting:true},
      {name:'Soco inglês',skill:'pelea',range:'Engajado',damage:4,crit:4,disorient:3}
    ],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:90,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION',crossCheck:'NPC card JoY:87; shens_goons_90'}
  }),
  // SWE09 printed p.90 and NPC compendium JoY:90, Arend Shen card 429.
  arend_shen: Object.freeze({
    key:'arend_shen',name:'Arend Shen',kind:'nemesis',
    characteristics:{fortaleza:2,agilidad:3,intelecto:4,astucia:4,voluntad:3,presencia:3},
    soak:3,woundsThreshold:14,strainThreshold:16,defense:{melee:0,ranged:0},
    groupSkills:[],skills:{carisma:4,coaccion:2,frialdad:4,engano:3,educacion:5,negociacion:5,percepcion:2,armas_distancia_ligeras:2,actividad_criminal:2,callejeo:4,bajos_fondos:4},
    talents:[{key:'adversario',name:'Adversário',rank:3},{key:'ninguém_me_engana',name:"Nobody's Fool",rank:2},{key:'resoluto',name:'Resolve',rank:1},{key:'conversador_habil',name:'Smooth Talker',rank:2}],abilities:[],
    weapons:[{name:'Pistola disruptora',skill:'armas_distancia_ligeras',range:'Curto',damage:10,crit:2,vicious:4}],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:90,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION',crossCheck:'JoY:90; arend_shen_429; roupa pesada +1 absorção; 5.000 créditos não transferidos automaticamente'}
  }),
  // SWE09 printed p.90, JoY:90 NPC card 267: three independent rivals at residence entrance.
  k4_security_droid: Object.freeze({
    key:'k4_security_droid',name:'Droide de segurança K4',kind:'rival',
    characteristics:{fortaleza:2,agilidad:3,intelecto:1,astucia:1,voluntad:1,presencia:1},
    soak:3,woundsThreshold:12,strainThreshold:null,defense:{melee:2,ranged:1},
    groupSkills:[],skills:{armas_cuerpo_a_cuerpo:2,armas_distancia_ligeras:3,alerta:2},talents:[],
    abilities:[{key:'droide',name:'Droide: imune a venenos e toxinas; não respira'}],
    weapons:[{name:'Bastão de segurança',skill:'armas_cuerpo_a_cuerpo',range:'Engajado',damage:4,crit:2,disorient:2,defensive:1,pierce:2},
      {name:'Pistola blaster integrada',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,stunSetting:true}],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:90,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION',crossCheck:'JoY:90; k4_security_droid_267'}
  }),
  // SWE09 printed p.91, JoY:91 card 122: three distinct groups of three yacht crew.
  yacht_crew: Object.freeze({
    key:'yacht_crew',name:'Tripulação do iate Venture',kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:1,voluntad:2,presencia:2},
    soak:3,woundsThreshold:6,strainThreshold:null,groupSkills:['artilleria','pilotar_vehiculo_espacial'],skills:{},talents:[],abilities:[],
    weapons:[{name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3,stunSetting:true}],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:91,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION',crossCheck:'JoY:91; yacht_crew_122; coveralls +1 absorção'}
  }),
  // SWE09 printed p.78 and user JoY:78 compendium card private_security_guard_75.
  // These are Grayson's security minions, not invented Wing Guard or named squad leaders.
  grayson_private_security_guard: Object.freeze({
    key:'grayson_private_security_guard',name:'Guarda particular de Marus Grayson',kind:'minion',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:4,woundsThreshold:5,strainThreshold:null,
    defense:{melee:1,ranged:1},
    groupSkills:['disciplina','armas_cuerpo_a_cuerpo','percepcion','armas_distancia_pesadas','alerta'],
    skills:{},talents:[],abilities:[],
    weapons:[
      {name:'Arma antidistúrbios',skill:'armas_distancia_pesadas',range:'Médio',damage:7,crit:3,autoFire:true,stunSetting:true},
      {name:'Vibrofaca',skill:'armas_cuerpo_a_cuerpo',range:'Engajado',damage:4,crit:2,pierce:2,vicious:1}
    ],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:78,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION',crossCheck:'JoY:78; private_security_guard_75'}
  }),
  bespin_wing_guard_officer: Object.freeze({
    key:'bespin_wing_guard_officer', name:'Oficial da Guarda Alada de Bespin', kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:3, woundsThreshold:5, strainThreshold:null,
    groupSkills:['disciplina','armas_cuerpo_a_cuerpo','pilotar_vehiculo_planetario','armas_distancia_ligeras','alerta'],
    skills:{}, talents:[],
    abilities:[{key:'covering_fire',name:'Covering Fire — manobra; +1 defesa à distância para até dois aliados próximos; não acumula',manualAdjudication:true,implementationStatus:'ACTIVE_ON_GM_QUEUED_TURN'}],
    weapons:[
      {name:'Pistola blaster Relby-K23',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,pierce:2,stunSetting:true},
      {name:'Cassetete de choque',skill:'armas_cuerpo_a_cuerpo',range:'Engajado',damage:5,crit:2,disorient:2}
    ],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:12,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION'}
  }),
  bespin_wing_guard_sergeant: Object.freeze({
    key:'bespin_wing_guard_sergeant', name:'Sargento da Guarda Alada de Bespin', kind:'rival',
    characteristics:{fortaleza:2,agilidad:3,intelecto:3,astucia:2,voluntad:3,presencia:3},
    soak:3, woundsThreshold:12, strainThreshold:null, groupSkills:[],
    skills:{coaccion:1,disciplina:1,liderazgo:2,armas_cuerpo_a_cuerpo:2,pilotar_vehiculo_planetario:2,armas_distancia_ligeras:2,alerta:2},
    talents:[],
    abilities:[{key:'tactical_direction',name:'Tactical Direction — manobra; grupo de oficiais aliado faz manobra livre ou recebe Reforço',manualAdjudication:true,implementationStatus:'BOOST_AND_SINGLE_BAND_FREE_MOVE_GM_QUEUED'}],
    weapons:[
      {name:'Pistola blaster Relby-K23',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3,pierce:2,stunSetting:true},
      {name:'Cassetete de choque',skill:'armas_cuerpo_a_cuerpo',range:'Engajado',damage:5,crit:2,disorient:2}
    ],
    source:{title:'The Jewel of Yavin',productCode:'SWE09',printedPage:13,authority:'PUBLISHED_ADVENTURE_IMPLEMENTATION'}
  }),
  bespin_security_officer: Object.freeze({
    key:'bespin_security_officer',
    name:'Agente da Segurança de Bespin',
    kind:'minion',
    characteristics:{fortaleza:2,agilidad:3,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:3,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_distancia_ligeras','disciplina','alerta'],
    skills:{}, talents:[], abilities:[],
    weapons:[{name:'Pistola de choque',skill:'armas_distancia_ligeras',range:'Curto',damage:6,crit:4}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  arlo_serpentine: Object.freeze({
    key:'arlo_serpentine',
    name:'Arlo Serpentine',
    kind:'nemesis',
    characteristics:{fortaleza:2,agilidad:3,intelecto:4,astucia:4,voluntad:3,presencia:4},
    soak:3,
    woundsThreshold:13,
    strainThreshold:14,
    groupSkills:[],
    skills:{alerta:2,callejeo:3,carisma:3,computadoras:3,engano:4,frialdad:3,negociacion:3},
    talents:[{key:'adversario',name:'Adversário',rank:1}],
    abilities:[{key:'rede_de_informantes',name:'Contatos em Cloud City'}],
    weapons:[{name:'Pistola blaster esportiva',skill:'armas_distancia_ligeras',range:'Médio',damage:5,crit:3}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  // SWE13 pp. 26, 90-94: verified against the user's Mask of the Pirate Queen
  // PDF and MPQ entries in the user's NPC-card catalog. These replace the
  // invented P181 bridge/queen stand-ins; full book certification is pending.
  mpq_bridge_guard: Object.freeze({
    key:'mpq_bridge_guard',name:'Guarda da ponte da Irmandade',kind:'minion',
    characteristics:{fortaleza:4,agilidad:4,intelecto:2,astucia:3,voluntad:3,presencia:2},
    soak:6,woundsThreshold:5,strainThreshold:null,defense:{melee:1,ranged:1},
    groupSkills:['armas_cuerpo_a_cuerpo','armas_distancia_ligeras'],skills:{},talents:[],abilities:[],
    weapons:[{name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3},
      {name:'Vibroespada',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:6,crit:2,pierce:2,defensive:1,vicious:1}],
    source:{title:'Mask of the Pirate Queen',productCode:'SWE13',page:90,card:'MPQ:90',authority:'SOURCE_CHECKED_PROFILE'}
  }),
  mpq_noira: Object.freeze({
    key:'mpq_noira',name:'Noira, Rainha Pirata',kind:'nemesis',
    characteristics:{fortaleza:2,agilidad:4,intelecto:4,astucia:4,voluntad:4,presencia:5},
    soak:5,woundsThreshold:16,strainThreshold:18,defense:{melee:1,ranged:0},groupSkills:[],
    skills:{carisma:4,coaccion:3,frialdad:5,engano:4,liderazgo:5,armas_cuerpo_a_cuerpo:3,negociacion:4,armas_distancia_ligeras:2,callejeo:4,bajos_fondos:3},
    talents:[{key:'adversario',name:'Adversário',rank:2},{key:'comando',name:'Comando',rank:2},{key:'nervos_de_aco',name:'Nervos de Aço',rank:1}],
    abilities:[{key:'presenca_imponente',name:'Presença imponente',description:'Ataques contra Noira sofrem um dado de dificuldade adicional salvo gasto de Destino.'}],
    weapons:[{name:'Vibrorrapieira',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:4,crit:2,pierce:5,defensive:1},
      {name:'Pistola blaster leve',skill:'armas_distancia_ligeras',range:'Médio',damage:5,crit:4}],
    source:{title:'Mask of the Pirate Queen',productCode:'SWE13',page:91,card:'MPQ:91',authority:'SOURCE_CHECKED_PROFILE'}
  }),
  mpq_porel: Object.freeze({
    key:'mpq_porel',name:'Porel Vakra',kind:'nemesis',
    characteristics:{fortaleza:3,agilidad:3,intelecto:3,astucia:3,voluntad:4,presencia:2},
    soak:4,woundsThreshold:16,strainThreshold:18,groupSkills:[],
    skills:{pelea:3,coaccion:2,frialdad:3,negociacion:2,percepcion:3,armas_distancia_ligeras:3,callejeo:3,bajos_fondos:3},
    talents:[{key:'adversario',name:'Adversário',rank:1},{key:'esquiva',name:'Esquiva',rank:1},{key:'tiro_a_queima_roupa',name:'Tiro à Queima-Roupa',rank:2}],abilities:[],
    weapons:[{name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3},
      {name:'Pistola blaster de bolso',skill:'armas_distancia_ligeras',range:'Curto',damage:6,crit:4}],
    source:{title:'Mask of the Pirate Queen',productCode:'SWE13',page:94,card:'MPQ:93',authority:'SOURCE_CHECKED_PROFILE'}
  }),
  veiled_sorority_corsair: Object.freeze({
    key:'veiled_sorority_corsair',
    name:'Corsária da Irmandade Oculta',
    kind:'minion',
    characteristics:{fortaleza:3,agilidad:3,intelecto:2,astucia:3,voluntad:2,presencia:2},
    soak:4,
    woundsThreshold:5,
    strainThreshold:null,
    groupSkills:['armas_distancia_pesadas','armas_cuerpo_a_cuerpo','sigilo'],
    skills:{}, talents:[], abilities:[],
    weapons:[{name:'Carabina blaster',skill:'armas_distancia_pesadas',range:'Médio',damage:8,crit:3}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  }),
  pirate_queen_nemesis: Object.freeze({
    key:'pirate_queen_nemesis',
    name:'A Rainha Pirata',
    kind:'nemesis',
    characteristics:{fortaleza:3,agilidad:4,intelecto:3,astucia:4,voluntad:3,presencia:4},
    soak:4,
    woundsThreshold:16,
    strainThreshold:15,
    groupSkills:[],
    skills:{alerta:3,armas_cuerpo_a_cuerpo:3,armas_distancia_ligeras:3,atletismo:3,coaccion:3,disciplina:3,frialdad:3,liderazgo:3},
    talents:[{key:'adversario',name:'Adversário',rank:2},{key:'golpes_letais',name:'Golpes letais',rank:1}],
    abilities:[{key:'mascara_enigmatica',name:'Identidade e presença intimidadora'}],
    weapons:[{name:'Vibroespada cerimonial',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:6,crit:2,pierce:2},{name:'Pistola blaster pesada',skill:'armas_distancia_ligeras',range:'Médio',damage:7,crit:3}],
    source:UNDER_BLACK_SUN_IMPLEMENTATION_SOURCE
  })
});


export const ALLY_NPC_CONTROL_MODES = Object.freeze({
  narrative:Object.freeze({key:'narrative',label:'Enfoque narrativo',summary:'Resolva o efeito do aliado narrativamente, sem rolagem própria.'}),
  abstract_combat:Object.freeze({key:'abstract_combat',label:'Combate abstrato',summary:'Aplique ao fim do assalto um efeito mecânico menor e perceptível sem turno completo.'}),
  delegated_combat:Object.freeze({key:'delegated_combat',label:'Combate encomendado',summary:'Um jogador controla o aliado/grupo durante o combate; o Mestre retoma o controle ao final.'})
});

export const AD_HOC_NPC_SOURCE = aorReference({
  title:'La Era de la Rebelión - Una compra difícil',
  section:'¡Socorro! ¡No tengo sus características!',
  guidance:'Características 2; limiar de Ferimentos 8; grau 1 nas perícias que o senso comum indique como treinadas.'
});

export function createAdHocRival({name='PNPC improvisado',trainedSkills=[]}={}){
  if(!Array.isArray(trainedSkills)) throw new Error('Perícias treinadas do PNPC devem ser uma lista.');
  const skills={};
  for(const skill of trainedSkills){
    if(!(skill in SKILLS)) throw new Error(`Perícia não mapeada para PNPC improvisado: ${skill}.`);
    skills[skill]=1;
  }
  return {
    id:newId('adv'), profileKey:'ad_hoc_rival', name:String(name||'PNPC improvisado').trim()||'PNPC improvisado', kind:'rival',
    range:'Médio',
    characteristics:{fortaleza:2,agilidad:2,intelecto:2,astucia:2,voluntad:2,presencia:2},
    soak:2, wounds:{current:0,threshold:8,perMember:8}, strain:null, membersTotal:1, skills, groupSkills:[], talents:[], abilities:[], weapons:[], criticalInjuries:[], status:'active',
    source:clone(AD_HOC_NPC_SOURCE)
  };
}

function clone(value){ return structuredClone(value); }
function assertProfile(profile){
  if(!profile || typeof profile!=='object') throw new Error('Perfil de adversário inválido.');
  if(!['minion','rival','nemesis'].includes(profile.kind)) throw new Error('Categoria de adversário inválida.');
}

export function createAdversary(profileKey,{count=1,range='Médio',name=null}={}){
  const profile=ADVERSARY_PROFILES[profileKey] ?? PUBLISHED_ADVENTURE_ADVERSARY_PROFILES[profileKey];
  assertProfile(profile);
  const members=profile.kind==='minion' ? Number(count) : 1;
  if(!Number.isInteger(members)||members<1||members>20) throw new Error('Quantidade de adversários inválida.');
  return {
    id:newId('adv'),
    profileKey,
    name:name?.trim() || (profile.kind==='minion' && members>1 ? `${profile.name} x${members}` : profile.name),
    kind:profile.kind,
    range,
    characteristics:clone(profile.characteristics),
    soak:profile.soak,
    defense:clone(profile.defense??{melee:0,ranged:0}),
    wounds:{current:0,threshold:profile.woundsThreshold * members,perMember:profile.woundsThreshold},
    strain:profile.kind==='nemesis' ? {current:0,threshold:profile.strainThreshold ?? 0} : null,
    membersTotal:members,
    skills:clone(profile.skills),
    groupSkills:clone(profile.groupSkills),
    talents:clone(profile.talents),
    abilities:clone(profile.abilities),
    weapons:clone(profile.weapons),
    criticalInjuries:[],
    status:'active',
    source:clone(profile.source)
  };
}

export function aliveMinions(adversary){
  if(adversary?.kind!=='minion') return adversaryHealthStatus(adversary).incapacitated ? 0 : 1;
  const total=Number(adversary.membersTotal)||0;
  const per=Number(adversary.wounds?.perMember)||0;
  const current=Math.max(0,Number(adversary.wounds?.current)||0);
  if(total<1||per<1) return 0;
  const defeated=Math.min(total, current===0 ? 0 : Math.floor((current-1)/per));
  return Math.max(0,total-defeated);
}

export function adversarySkillRank(adversary,skillKey){
  if(!adversary) throw new Error('Adversário inválido.');
  if(!(skillKey in SKILLS)) throw new Error('Perícia de adversário não mapeada.');
  if(adversary.kind==='minion'){
    if(!adversary.groupSkills?.includes(skillKey)) return 0;
    return Math.max(0,aliveMinions(adversary)-1);
  }
  return Math.max(0,Number(adversary.skills?.[skillKey])||0);
}

export function adversaryCharacteristicForSkill(adversary,skillKey){
  const characteristic=SKILLS[skillKey];
  if(!characteristic) throw new Error('Perícia de adversário não mapeada.');
  return Number(adversary.characteristics?.[characteristic] ?? 0);
}

export function adversaryHealthStatus(adversary){
  if(!adversary?.wounds) throw new Error('Estado de adversário inválido.');
  const minionsAlive=adversary.kind==='minion' ? aliveMinionsRaw(adversary) : null;
  const woundIncapacitated=adversary.kind==='minion'
    ? minionsAlive<=0
    : Number(adversary.wounds.current)>Number(adversary.wounds.threshold);
  const strainIncapacitated=adversary.kind==='nemesis' && adversary.strain
    ? Number(adversary.strain.current)>Number(adversary.strain.threshold)
    : false;
  return {
    incapacitated:woundIncapacitated||strainIncapacitated,
    woundIncapacitated,
    strainIncapacitated,
    minionsAlive,
    label:woundIncapacitated||strainIncapacitated?'Incapacitado':'Ativo'
  };
}

function aliveMinionsRaw(adversary){
  const total=Number(adversary.membersTotal)||0;
  const per=Number(adversary.wounds?.perMember)||0;
  const current=Math.max(0,Number(adversary.wounds?.current)||0);
  if(total<1||per<1) return 0;
  const defeated=Math.min(total,current===0?0:Math.floor((current-1)/per));
  return Math.max(0,total-defeated);
}

export function applyAdversaryDamage(adversary,{damage,pierce=0,damageType='wounds'}={}){
  if(!adversary) throw new Error('Adversário inválido.');
  const raw=Number(damage), p=Number(pierce);
  if(!Number.isInteger(raw)||raw<0) throw new Error('Dano inválido.');
  if(!Number.isInteger(p)||p<0) throw new Error('Perfuração inválida.');
  if(!['wounds','strain'].includes(damageType)) throw new Error('Tipo de dano inválido.');
  const next=clone(adversary);
  const hasCortosis = Boolean(next.cortosis || (Array.isArray(next.abilities) && next.abilities.some(a=>String(a.key||a.name||a).toLowerCase().includes('cortosis'))));
  const effectiveSoak=hasCortosis ? Math.max(0,Number(next.soak)) : Math.max(0,Number(next.soak)-p);
  const suffered=Math.max(0,raw-effectiveSoak);
  const convertedToWounds=damageType==='strain' && next.kind!=='nemesis';
  if(damageType==='strain' && next.kind==='nemesis') next.strain.current+=suffered;
  else next.wounds.current+=suffered;
  const status=adversaryHealthStatus(next);
  next.status=status.incapacitated?'incapacitated':'active';
  return {adversary:next,rawDamage:raw,effectiveSoak,suffered,damageType:convertedToWounds?'wounds':damageType,convertedToWounds,status,hasCortosis};
}

export function sufferAdversaryStrain(adversary,amount){
  const n=Number(amount);
  if(!Number.isInteger(n)||n<0) throw new Error('Quantidade de Tensão inválida.');
  const next=clone(adversary);
  if(next.kind==='nemesis' && next.strain){
    next.strain.current+=n;
    const status=adversaryHealthStatus(next); next.status=status.incapacitated?'incapacitated':'active';
    return {adversary:next,amount:n,convertedToWounds:false,status};
  }
  next.wounds.current+=n;
  const status=adversaryHealthStatus(next); next.status=status.incapacitated?'incapacitated':'active';
  return {adversary:next,amount:n,convertedToWounds:true,status};
}

export function applyAdversaryCritical(adversary,reason='Ferimento crítico',{modifier=0,rng=Math.random,roll=null}={}){
  const next=clone(adversary);
  if(next.kind==='minion'){
    const alive=aliveMinionsRaw(next);
    if(alive<=0) return {adversary:next,applied:false,woundsAdded:0,status:adversaryHealthStatus(next)};
    const per=next.wounds.perMember;
    const defeated=next.membersTotal-alive;
    const target=per*(defeated+1)+1;
    const add=Math.max(1,target-next.wounds.current);
    next.wounds.current+=add;
    const status=adversaryHealthStatus(next); next.status=status.incapacitated?'incapacitated':'active';
    return {adversary:next,applied:true,woundsAdded:add,status,modifierApplied:0};
  }
  const baseRoll=roll===null?Math.min(100,Math.max(1,Math.floor(Number(rng())*100)+1)):Math.min(100,Math.max(1,Number(roll)||1));
  const existing=(next.criticalInjuries??[]).length*10;
  const external=Math.max(0,Number(modifier)||0);
  const total=baseRoll+existing+external;
  const table=criticalInjuryTable();
  const rule=table.find(row=>total>=row.min&&total<=row.max)??table.at(-1);
  const injury={id:newId('advcrit'),reason,at:new Date().toISOString(),baseRoll,total,existingModifier:existing,externalModifier:external,code:rule?.code??null,label:rule?.label??'Ferimento crítico',severity:rule?.severity??null,description:rule?.description??''};
  next.criticalInjuries.push(injury);
  return {adversary:next,applied:true,woundsAdded:0,status:adversaryHealthStatus(next),injury,modifierApplied:external};
}

export function upgradeDifficultyForAdversaryTalent(adversary){
  return Math.max(0,Number(adversary?.talents?.find(t=>t.key==='adversario')?.rank)||0);
}
