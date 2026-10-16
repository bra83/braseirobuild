const SOURCE=Object.freeze({kind:'community_reference',title:'Star Wars RPG Galaxy Map — TheDarkElf007 Gaming Blog',url:'https://thedarkelf007.home.blog/blog/star-wars/star-wars-rpg-galaxy-map/',authority:'TRAVEL_REFERENCE_ONLY',mechanicalAuthority:false,note:'Tempos/dificuldades são decisões de design do autor; não substituem EotE/AoR.'});
export const LANE_CLASSES=Object.freeze({MAJOR:{difficulty:1,hoursPerSquare:12},MINOR:{difficulty:2,hoursPerSquare:24},UNSHOWN:{difficulty:3,hoursPerSquare:48},NONE:{difficulty:4,hoursPerSquare:96},SEGMENT:{difficulty:3,hoursPerSquare:48}});
export const REGION_MODIFIERS=Object.freeze({'Deep Core':{upgrade:2,setback:0},'Core Worlds':{upgrade:0,setback:0},'Colonies':{upgrade:0,setback:1},'Inner Rim':{upgrade:0,setback:2},'Expansion Region':{upgrade:1,setback:0},'Mid Rim':{upgrade:1,setback:1},'Outer Rim':{upgrade:1,setback:2},'Unknown Regions':{upgrade:2,setback:0}});
export const SPECIAL_REGIONS=Object.freeze({BothanSpace:1,Centrality:1,CorporateSector:1,DeveronSpace:1,GordianReach:1,HapesConsortium:1,HuttSpace:1,MandalorianSpace:1,TionCluster:1,TingelArm:2,WildSpace:2});
export const MAJOR_HYPERLANES=Object.freeze(['Perlemian Trade Route','Corellian Run','Corellian Trade Spine','Rimma Trade Route','Hydian Way']);
export const RISKY_HYPERLANES=Object.freeze(['Bakura Trace','Burke’s Trailing','Carbonite Run','Coruscant-Dantooine hyperlane','Coyn Route','Hidakai Pool','Ison Corridor','Metellos Trade Route','Minntooine Spur','Myto’s Arrow Trade Route','Prously’s Rim Run','Relgim Run','Seswenna Expansion','Shapani Bypass','Tidal Circuit']);
export const SECRET_HYPERLANES=Object.freeze(['Byss Run','Daragon Trail','Koros Trunk Line','Mandalorian Road','Metellos-Ilum hyperlane','Shapani Bypass']);
export const MINOR_HYPERLANES=Object.freeze(['Ado Spine','Annaj-Houche Run','Braxant Run','Celanon Spur','Cerean Reach','Commenor Run','Coruscant-N’Zoth hyperlane','D’Aelgoth Trade Route','Dubrillion-Bescane hyperlane','Duros Space Run','Enarc Run','Entralla Route','Fedalle-Kuat hyperlane','Garqi-Borosk hyperlane','Giju Run','Great Gran Run','Harrin Trade Corridor','Koda Spur','Kuat-Commenor hyperlane','Lipsec Run','Lutrillian Cross','Murgo Choke','Namadii Corridor','Nothoiin Corridor','Ootmian Pabol','Overic Griplink','Orvise Reach','Pabol Hutta','Rago Run','Shag Pabol','Sharlissian Trade Corridor','Shipwrights’ Trace','Shiritoku Way','Shwuy Exchange','Spar Trade Route','Trellen Trade Route','Triellus Trade Route','Trition Trade Route','Vaathkree Trade Corridor','Veragi Trade Route','Yankirk Route']);
export const VERIFIED_SEGMENT_NODES=Object.freeze([
 {id:'anx_minor',name:'Anx Minor',region:'Outer Rim',coord:[12.3,4.5],segment:'L4',flags:['NS'],lanes:['Veragi Trade Route']},
 {id:'ord_trasi',name:'Ord Trasi',region:'Outer Rim',coord:[12.1,4.8],segment:'L4',flags:[],lanes:['Veragi Trade Route']},
 {id:'ord_mantell',name:'Ord Mantell',region:'Mid Rim',coord:[12.0,7.4],segment:'L7',flags:[],lanes:['Celanon Spur','Entralla Route']},
 {id:'halm',name:'Halm',region:'Mid Rim',coord:[10.4,16.2],segment:'J16',flags:['NS'],lanes:['Great Gran Run']},
 {id:'natalon',name:'Natalon',region:'Expansion Region',coord:[10.7,16.2],segment:'J16',flags:['NS'],lanes:['Great Gran Run']},
 {id:'new_apsolon',name:'New Apsolon',region:'Mid Rim',coord:[16.6,10.4],segment:'P10',flags:[],lanes:[]},
 {id:'umbara',name:'Umbara',region:'Expansion Region',coord:[16.1,10.1],segment:'P10',flags:[],lanes:[]},
 {id:'aduba_3',name:'Aduba-3',region:'Outer Rim',coord:[21.3,11.1],segment:'U11',flags:[],lanes:['Triellus Trade Route']}
]);
export const VERIFIED_EDGES=Object.freeze([
 {from:'Dantooine',to:'Anx Minor',lane:'Veragi Trade Route',class:'MINOR',distance:.6,hours:14.4},
 {from:'Anx Minor',to:'Ord Trasi',lane:'Veragi Trade Route',class:'MINOR',distance:.4,hours:9.6},
 {from:'Dohu',to:'Ord Mantell',lane:'Celanon Spur',class:'MINOR',distance:.5,hours:12},
 {from:'Ord Mantell',to:'Genassa',lane:'Celanon Spur',class:'MINOR',distance:1.3,hours:31.2},
 {from:'Natalon',to:'Halm',lane:'Great Gran Run',class:'MINOR',distance:.3,hours:7.2},
 {from:'New Apsolon',to:'Belasco',lane:null,class:'UNSHOWN',distance:1.7,hours:27.2},
 {from:'New Apsolon',to:'Randon',lane:null,class:'MINOR',distance:.8,hours:12.8},
 {from:'Umbara',to:'Kashyyyk',lane:null,class:'UNSHOWN',distance:1,hours:16},
 {from:'Umbara',to:'Zeltros',lane:null,class:'MINOR',distance:.8,hours:12.8}
]);

export const VERIFIED_EDGES_R50=Object.freeze([
 {from:'Ord Mantell',to:'Orinda',lane:'Entralla Route',class:'MINOR',distance:1.2,hours:28.8,difficulty:3,upgrade:2,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2020/10/22/sw-gg-entralla-route/'},
 {from:'Orinda',to:'Borosk',lane:'Entralla Route',class:'MINOR',distance:1.6,hours:38.4,difficulty:3,upgrade:2,setback:3,sourceUrl:'https://thedarkelf007.home.blog/2020/10/22/sw-gg-entralla-route/'},
 {from:'Borosk',to:'Yaga Minor',lane:'Entralla Route',class:'MINOR',distance:.2,hours:4.8,difficulty:2,upgrade:2,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2020/10/22/sw-gg-entralla-route/'},
 {from:'Yaga Minor',to:'Entralla',lane:'Entralla Route',class:'MINOR',distance:.5,hours:12,difficulty:2,upgrade:2,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2020/10/22/sw-gg-entralla-route/'},
 {from:'Entralla',to:'Muunilinst',lane:'Entralla Route',class:'MINOR',distance:.4,hours:9.6,difficulty:2,upgrade:2,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2020/10/22/sw-gg-entralla-route/'},
 {from:'Fedalle',to:'Kuat',lane:'Fedalle-Kuat hyperlane',class:'MINOR',distance:.4,hours:9.6,difficulty:2,upgrade:0,setback:0,sourceUrl:'https://thedarkelf007.home.blog/2020/12/15/sw-trade-fedalle-kuat-hyperlane/'},
 {from:'Teth',to:'Barab',lane:'Triellus Trade Route',class:'MINOR',distance:.8,hours:19.2,difficulty:2,upgrade:2,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2021/02/16/sw-trade-triellus-trade-route/'},
 {from:'Barab',to:'Dubrava',lane:'Triellus Trade Route',class:'MINOR',distance:.6,hours:14.4,difficulty:2,upgrade:2,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2021/02/16/sw-trade-triellus-trade-route/'},
 {from:'Cerea',to:'Abbaji',lane:'Spar Trade Route',class:'MINOR',distance:.7,hours:16.8,difficulty:null,upgrade:null,setback:null,sourceUrl:'https://thedarkelf007.home.blog/2020/08/22/sw-gg-spar-trade-route/'},
 {from:'Abbaji',to:'Annaj',lane:'Spar Trade Route',class:'MINOR',distance:.5,hours:12,difficulty:2,upgrade:4,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2020/08/22/sw-gg-spar-trade-route/'},
 {from:'Skynara',to:'Vohai',lane:'Five Veils Route',class:'MINOR',distance:1.9,hours:30.4,difficulty:3,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/08/sw-gg-p19/'},
]);
export const LOCATION_ENRICHMENT_R50=Object.freeze([
 {name:'Alzoc',region:'Outer Rim',coord:[16.4,19.3],segment:'P19',flags:['WS']},
 {name:'Karazak',region:'Outer Rim',coord:[16.6,19.6],segment:'P19',flags:['WS']},
 {name:'Skynara',region:'Outer Rim',coord:[16.8,19.2],segment:'P19',flags:[],lanes:['Five Veils Route']},
 {name:'Coachelle',region:'Mid Rim',coord:[16.9,10.3],segment:'P10',flags:[]},
 {name:'Gravlex Med',region:'Outer Rim',coord:[12.7,4.3],segment:'L4',flags:['WS']},
]);
export function allVerifiedEdges50(){return [...VERIFIED_EDGES,...VERIFIED_EDGES_R50];}
export function neighbors50(name){const n=clean(name);return allVerifiedEdges50().filter(e=>clean(e.from)===n||clean(e.to)===n).map(e=>({...e,destination:clean(e.from)===n?e.to:e.from}));}
export function routePlan50(from,to){const start=clean(from),goal=clean(to); if(!start||!goal)return null; const edges=allVerifiedEdges50(); const q=[[from,[]]]; const seen=new Set([start]); while(q.length){const [cur,path]=q.shift(); if(clean(cur)===goal)return {from,to,legs:path,totalHours:Number(path.reduce((a,e)=>a+(e.hours||0),0).toFixed(2)),totalDistance:Number(path.reduce((a,e)=>a+(e.distance||0),0).toFixed(2)),requiresMasterValidation:true,mechanicalMutation:false}; for(const e of edges){let nxt=null;if(clean(e.from)===clean(cur))nxt=e.to;else if(clean(e.to)===clean(cur))nxt=e.from;if(nxt&&!seen.has(clean(nxt))){seen.add(clean(nxt));q.push([nxt,[...path,{...e,destination:nxt}]]);}}} return null;}
export function coverage50(){return {...coverage49(),verifiedEdgesR50:VERIFIED_EDGES_R50.length,verifiedEdgesTotal:allVerifiedEdges50().length,locationEnrichmentR50:LOCATION_ENRICHMENT_R50.length};}

function clean(s){return String(s??'').trim().toLowerCase();}
export function source49(){return SOURCE;}
export function coverage49(){return {major:MAJOR_HYPERLANES.length,risky:RISKY_HYPERLANES.length,secret:SECRET_HYPERLANES.length,minor:MINOR_HYPERLANES.length,verifiedNodes:VERIFIED_SEGMENT_NODES.length,verifiedEdges:VERIFIED_EDGES.length,mechanicalAuthority:false};}
export function findRoutes49(q=''){const s=clean(q);return [...new Set([...MAJOR_HYPERLANES,...RISKY_HYPERLANES,...SECRET_HYPERLANES,...MINOR_HYPERLANES])].filter(x=>!s||clean(x).includes(s));}
export function neighbors49(name){const n=clean(name);return VERIFIED_EDGES.filter(e=>clean(e.from)===n||clean(e.to)===n).map(e=>({...e,destination:clean(e.from)===n?e.to:e.from}));}
export function estimateTravel49({distance,laneClass='MINOR',fromRegion='Core Worlds',toRegion='Core Worlds',notShown=false,interdicted=false,specialUpgrade=0}={}){const d=Number(distance); if(!Number.isFinite(d)||d<0) throw new Error('distance inválida'); const cls=LANE_CLASSES[laneClass]; if(!cls) throw new Error('laneClass inválida'); const from=REGION_MODIFIERS[fromRegion]??{upgrade:0,setback:0}; const to=REGION_MODIFIERS[toRegion]??{upgrade:0,setback:0}; return {hours:Number((d*cls.hoursPerSquare).toFixed(2)),difficulty:cls.difficulty+Math.floor(d),upgrade:from.upgrade+to.upgrade+(notShown?1:0)+(interdicted?1:0)+Number(specialUpgrade||0),setback:from.setback+to.setback,source:SOURCE,requiresMasterValidation:true};}
export function prepareTravel49(input){return {status:'PENDING_MASTER',kind:'GALAXY_TRAVEL',proposal:estimateTravel49(input),input:{...input},source:SOURCE,mechanicalMutation:false};}

export const VERIFIED_EDGES_R51=Object.freeze([
 {from:'Enarc',to:'Bannistar Station',lane:'Enarc Run',class:'MINOR',distance:1.9,hours:30.4,difficulty:3,upgrade:1,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/06/sw-gg-o17/'},
 {from:'Enarc',to:'Farstine',lane:'Triellus Trade Route',class:'MINOR',distance:.8,hours:12.8,difficulty:2,upgrade:1,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/06/sw-gg-o17/'},
 {from:'Enarc',to:'Naboo',lane:null,class:'UNSHOWN',distance:.2,hours:3.2,difficulty:2,upgrade:1,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/06/sw-gg-o17/'},
 {from:'Enarc',to:'Sanrafsix',lane:'Sanrafsix Corridor',class:'MINOR',distance:1.3,hours:20.8,difficulty:3,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/06/sw-gg-o17/'},
 {from:'Naboo',to:'Kalinda',lane:null,class:'UNSHOWN',distance:.9,hours:14.4,difficulty:2,upgrade:1,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/06/sw-gg-o17/'},
 {from:'Farstine',to:'Trigalis',lane:null,class:'UNSHOWN',distance:.4,hours:6.4,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/08/sw-gg-p17/'},
 {from:'Farstine',to:'Zhar',lane:'Triellus Trade Route',class:'MINOR',distance:.9,hours:14.4,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/08/sw-gg-p17/'},
 {from:'Trigalis',to:'Vergesso',lane:null,class:'UNSHOWN',distance:.8,hours:12.8,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/08/sw-gg-p17/'},
 {from:'Zhar',to:'Llanic',lane:'Triellus Trade Route',class:'MINOR',distance:.7,hours:11.2,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/08/sw-gg-p17/'},
 {from:'Nelvaan',to:'Llanic',lane:null,class:'UNSHOWN',distance:.6,hours:9.6,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/10/sw-gg-q17/'},
 {from:'Nelvaan',to:'Tatooine',lane:null,class:'UNSHOWN',distance:.9,hours:14.4,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/10/sw-gg-q17/'},
 {from:'Socorro',to:'Llanic',lane:'Llanic Spice Run',class:'MINOR',distance:.8,hours:12.8,difficulty:2,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/10/sw-gg-q17/'},
 {from:'Socorro',to:'Reuss',lane:'Llanic Spice Run',class:'MINOR',distance:1.5,hours:24,difficulty:3,upgrade:1,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/10/sw-gg-q17/'},
 {from:'Chalacta',to:'Nimban',lane:null,class:'UNSHOWN',distance:1,hours:16,difficulty:3,upgrade:2,setback:2,sourceUrl:'https://thedarkelf007.home.blog/2021/01/12/sw-gg-r10/'},
 {from:'Chalacta',to:'Randon',lane:null,class:'UNSHOWN',distance:1.4,hours:22.4,difficulty:3,upgrade:1,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/12/sw-gg-r10/'},
 {from:'Kwenn',to:'Nal Hutta',lane:'Ootmian Pabol',class:'MINOR',distance:1.9,hours:30.4,difficulty:3,upgrade:2,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/12/sw-gg-r10/'},
 {from:'Kwenn',to:'Ubrikkia',lane:'Ootmian Pabol',class:'MINOR',distance:.8,hours:12.8,difficulty:2,upgrade:2,setback:1,sourceUrl:'https://thedarkelf007.home.blog/2021/01/12/sw-gg-r10/'},
 {from:'Hinakuu',to:'Sanctuary',lane:'Tidal Circuit',class:'UNSHOWN',distance:.4,hours:19.2,difficulty:3,upgrade:5,setback:4,sourceUrl:'https://thedarkelf007.home.blog/2021/02/09/sw-trade-tidal-circuit/'}
]);
export const LOCATION_ENRICHMENT_R51=Object.freeze([
 {name:'D’Qar',region:'Outer Rim',coord:[15.3,17.5],segment:'O17'}, {name:'Enarc',region:'Mid Rim',coord:[15.4,17.1],segment:'O17',lanes:['Duros Space Run','Enarc Run','Triellus Trade Route']}, {name:'Naboo',region:'Mid Rim',coord:[15.3,17.1],segment:'O17'}, {name:'Rugosa',region:'Outer Rim',coord:[15.7,17.5],segment:'O17'},
 {name:'Farstine',region:'Mid Rim',coord:[16.1,17.0],segment:'P17',lanes:['Triellus Trade Route']}, {name:'Trigalis',region:'Outer Rim',coord:[16.0,17.4],segment:'P17'}, {name:'Zhar',region:'Outer Rim',coord:[16.9,17.1],segment:'P17',lanes:['Triellus Trade Route']},
 {name:'Lok',region:'Outer Rim',coord:[17.5,17.2],segment:'Q17',flags:['WS'],lanes:['Triellus Trade Route']}, {name:'Nelvaan',region:'Outer Rim',coord:[17.8,17.0],segment:'Q17'}, {name:'Socorro',region:'Outer Rim',coord:[17.1,17.6],segment:'Q17',lanes:['Llanic Spice Run']},
 {name:'Chalacta',region:'Mid Rim',coord:[18.5,10.2],segment:'R10'}, {name:'Kwenn',region:'Mid Rim',coord:[18.5,10.8],segment:'R10',lanes:['Ootmian Pabol']}, {name:'Nar Bo Sholla',region:'Mid Rim',coord:[18.8,10.9],segment:'R10'},
 {name:'Barab I',region:'Outer Rim',coord:[21.0,12.9],segment:'U12',lanes:['Triellus Trade Route']}, {name:'Chantaano',region:'Outer Rim',coord:[21.1,12.6],segment:'U12'}, {name:'Teth',region:'Outer Rim',coord:[21.1,12.1],segment:'U12'}
]);
export function allVerifiedEdges51(){return [...allVerifiedEdges50(),...VERIFIED_EDGES_R51];}
export function neighbors51(name){const n=clean(name);return allVerifiedEdges51().filter(e=>clean(e.from)===n||clean(e.to)===n).map(e=>({...e,destination:clean(e.from)===n?e.to:e.from}));}
export function routePlan51(from,to){const start=clean(from),goal=clean(to);if(!start||!goal)return null;const edges=allVerifiedEdges51();const q=[[from,[]]];const seen=new Set([start]);while(q.length){const [cur,path]=q.shift();if(clean(cur)===goal)return {from,to,legs:path,totalHours:Number(path.reduce((a,e)=>a+(e.hours||0),0).toFixed(2)),totalDistance:Number(path.reduce((a,e)=>a+(e.distance||0),0).toFixed(2)),requiresMasterValidation:true,mechanicalMutation:false};for(const e of edges){let nxt=null;if(clean(e.from)===clean(cur))nxt=e.to;else if(clean(e.to)===clean(cur))nxt=e.from;if(nxt&&!seen.has(clean(nxt))){seen.add(clean(nxt));q.push([nxt,[...path,{...e,destination:nxt}]]);}}}return null;}
export function coverage51(){return {...coverage50(),verifiedEdgesR51:VERIFIED_EDGES_R51.length,verifiedEdgesTotal:allVerifiedEdges51().length,locationEnrichmentR51:LOCATION_ENRICHMENT_R51.length};}
