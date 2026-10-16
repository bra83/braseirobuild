import { WEAPON_COMPENDIUM, WEAPON_COMPENDIUM_COUNT, weaponCompendium, weaponCompendiumEntry } from "./weaponCompendium.mjs";
const aorReference=(source)=>Object.freeze({...source,authority:'CROSS_LINE_REFERENCE',rulesProfile:'AOR_REFERENCE_ONLY',activeRulesAuthority:false});

const SRC_WEAPONS=aorReference({book:'La Era de la Rebelión - Pantalla',screenPage:6,printedPages:'173/182',section:'Armas a distância / Armas corpo a corpo'});
const SRC_ARMOR=aorReference({book:'La Era de la Rebelión - Pantalla',screenPage:7,printedPage:184,section:'Blindagens'});

const SRC_STARTER_ARMOR_PRICE=aorReference({book:'La Era de la Rebelión - Libro de reglas',pdfPage:35,section:'Tabla 5-2: Blindajes'});

const SRC_GENERAL_GEAR=aorReference({book:'La Era de la Rebelión - Libro de reglas',pdfPage:36,section:'Tabla 5-3: Equipo general'});
const SRC_GENERAL_GEAR_EFFECTS=aorReference({book:'La Era de la Rebelión - Libro de reglas',pdfPage:37,printedPage:36,section:'Medpac / Medpac de emergencia'});

const eoteCoreSource=(source)=>Object.freeze({...source,authority:'PRIMARY_RULE_SOURCE',rulesProfile:'EOTE_CORE',activeRulesAuthority:true});
const SRC_EOTE_CORE_GEAR=eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:181,section:'Table 5-8: Gear and Equipment'});
const SRC_EOTE_CORE_ARMOR=eoteCoreSource({book:'Edge of the Empire Core Rulebook',printedPage:169,section:'Table 5-7: Armor'});
const SRC_FLY_CASUAL_GEAR=eoteCoreSource({book:'Fly Casual',printedPage:48,section:'Table 2-4: Gear'});

function g(id,sourceNameEs,name,price,{medicine=null,effectSource=null}={}){
  const entry={id,name,sourceNameEs,category:'general_gear',price,rarity:null,restricted:null,source:SRC_GENERAL_GEAR};
  if (medicine) entry.medicine=Object.freeze({medicalSupplies:Boolean(medicine.medicalSupplies),boost:Number(medicine.boost ?? 0)});
  if (effectSource) entry.effectSource=effectSource;
  return Object.freeze(entry);
}

export const STARTER_GENERAL_GEAR_REFERENCE=Object.freeze([
  g('glow_rod','Barra luminosa','Bastão luminoso',10),
  g('comlink','Comunicador','Comunicador',25),
  g('electrobinoculars','Electrobinoculares','Eletrobinóculos',250),
  g('hand_scanner','Escáner manual','Scanner manual',100),
  g('multi_scanner','Escáner multiuso','Scanner multiuso',500),
  g('electronic_lockpick','Ganzúa electrónica','Gazua eletrônica',1000),
  g('binders','Grilletes','Algemas',25),
  g('stimpack_injector','Inyector de estimulantes','Injetor de estimulantes',25),
  g('tool_kit','Juego de herramientas','Kit de ferramentas',350),
  g('macrobinoculars','Macrobinoculares','Macrobinóculos',75),
  g('restraining_device','Mando de retención','Dispositivo de retenção',35),
  g('breath_mask','Mascarilla respiratoria','Máscara respiratória',25),
  g('disguise_material','Material para disfrazarse','Material para disfarce',100),
  g('medpac','Medpac','Medpac',400,{medicine:{medicalSupplies:true,boost:1},effectSource:SRC_GENERAL_GEAR_EFFECTS}),
  g('emergency_medpac','Medpac de emergencia','Medpac de emergência',100,{medicine:{medicalSupplies:true,boost:0},effectSource:SRC_GENERAL_GEAR_EFFECTS}),
  g('backpack','Mochila','Mochila',50),
  g('data_module','Módulo de datos','Módulo de dados',75),
  g('emergency_repair_patch','Parche de reparaciones de emergencia','Remendo de reparo de emergência',25),
  g('rations','Raciones','Rações',5),
  g('space_suit','Traje espacial','Traje espacial',100),
  g('power_pack_or_ammo_reload','Unidad de alimentación o recarga de munición','Unidade de alimentação ou recarga de munição',25)
]);

function w(id,name,skill,{damage=null,damageBonus=null,crit=null,range,encumbrance,hardPoints,price,rarity,restricted=false,qualities=[]}={}){
  return Object.freeze({id,name,category:'weapon',skill,damage,damageBonus,crit,range,encumbrance,hardPoints,price,rarity,restricted,qualities:Object.freeze([...qualities]),source:SRC_WEAPONS});
}

export const CORE_WEAPON_CATALOG=Object.freeze([
  w('pocket_blaster','Blaster de bolso','armas_distancia_ligeras',{damage:5,crit:4,range:'Curto',encumbrance:1,hardPoints:1,price:200,rarity:4,qualities:['Modo atordoante']}),
  w('light_blaster_pistol','Pistola blaster leve','armas_distancia_ligeras',{damage:5,crit:4,range:'Médio',encumbrance:1,hardPoints:2,price:300,rarity:4,qualities:['Modo atordoante']}),
  w('blaster_pistol','Pistola blaster','armas_distancia_ligeras',{damage:6,crit:3,range:'Médio',encumbrance:1,hardPoints:3,price:400,rarity:4,qualities:['Modo atordoante']}),
  w('heavy_blaster_pistol','Pistola blaster pesada','armas_distancia_ligeras',{damage:7,crit:3,range:'Médio',encumbrance:2,hardPoints:3,price:700,rarity:6,qualities:['Modo atordoante']}),
  w('blaster_carbine','Carabina blaster','armas_distancia_pesadas',{damage:9,crit:3,range:'Médio',encumbrance:3,hardPoints:4,price:850,rarity:5,qualities:['Modo atordoante']}),
  w('blaster_rifle','Fuzil blaster','armas_distancia_pesadas',{damage:9,crit:3,range:'Longo',encumbrance:4,hardPoints:4,price:900,rarity:5,qualities:['Modo atordoante']}),
  w('heavy_blaster_rifle','Fuzil blaster pesado','armas_distancia_pesadas',{damage:10,crit:3,range:'Longo',encumbrance:6,hardPoints:4,price:1500,rarity:6,qualities:['Aparatosa 3','Automática']}),
  w('light_repeating_blaster','Blaster de repetição leve','armas_distancia_pesadas',{damage:11,crit:3,range:'Longo',encumbrance:7,hardPoints:4,price:2250,rarity:7,restricted:true,qualities:['Aparatosa 4','Automática','Perfurante 1']}),
  w('heavy_repeating_blaster','Blaster de repetição pesado','artilleria',{damage:15,crit:2,range:'Longo',encumbrance:9,hardPoints:4,price:6000,rarity:8,restricted:true,qualities:['Aparatosa 5','Automática','Letal 1','Perfurante 2']}),
  w('ion_blaster','Blaster ionizador','armas_distancia_ligeras',{damage:10,crit:5,range:'Curto',encumbrance:3,hardPoints:3,price:250,rarity:3,qualities:['Desorientação 5','Iônica']}),
  w('disruptor_pistol','Pistola disruptora','armas_distancia_ligeras',{damage:10,crit:2,range:'Curto',encumbrance:2,hardPoints:2,price:3000,rarity:6,restricted:true,qualities:['Letal 4']}),
  w('disruptor_rifle','Fuzil disruptor','armas_distancia_pesadas',{damage:10,crit:2,range:'Longo',encumbrance:5,hardPoints:4,price:5000,rarity:6,restricted:true,qualities:['Aparatosa 2','Letal 5']}),
  w('slugthrower_pistol','Pistola de projéteis sólidos','armas_distancia_ligeras',{damage:4,crit:5,range:'Curto',encumbrance:1,hardPoints:0,price:100,rarity:3}),
  w('slugthrower_rifle','Fuzil de projéteis sólidos','armas_distancia_pesadas',{damage:7,crit:5,range:'Médio',encumbrance:5,hardPoints:1,price:250,rarity:3,qualities:['Aparatosa 2']}),
  w('flamethrower','Lança-chamas','armas_distancia_pesadas',{damage:8,crit:2,range:'Curto',encumbrance:6,hardPoints:2,price:1000,rarity:6,qualities:['Explosão 8','Incendiária 3']}),
  w('missile_tube','Lança-mísseis','artilleria',{damage:20,crit:2,range:'Extremo',encumbrance:7,hardPoints:4,price:7500,rarity:8,restricted:true,qualities:['Aparatosa 3','Brecha 1','Explosão 10','Guiada 3','Munição limitada 6','Preparação 1']}),
  w('stun_grenade','Granada atordoante','armas_distancia_ligeras',{damage:8,crit:null,range:'Curto',encumbrance:1,hardPoints:0,price:75,rarity:4,qualities:['Dano de atordoamento','Desorientação 3','Explosão 8','Munição limitada 1']}),
  w('frag_grenade','Granada de fragmentação','armas_distancia_ligeras',{damage:8,crit:4,range:'Curto',encumbrance:1,hardPoints:0,price:50,rarity:5,qualities:['Explosão 6','Munição limitada 1']}),
  w('concussion_grenade','Granada perfurante','armas_distancia_ligeras',{damage:13,crit:3,range:'Curto',encumbrance:1,hardPoints:0,price:100,rarity:6,qualities:['Explosão 4','Munição limitada 1','Perfurante 3']}),
  w('anti_personnel_mine','Mina antipessoal','mecanica',{damage:12,crit:3,range:'Interação',encumbrance:3,hardPoints:0,price:850,rarity:6,restricted:true,qualities:['Explosão 4','Letal 4','Munição limitada 1']}),
  w('anti_vehicle_mine','Mina antiveículo','mecanica',{damage:25,crit:2,range:'Interação',encumbrance:4,hardPoints:0,price:1400,rarity:6,restricted:true,qualities:['Brecha 4','Explosão 2','Munição limitada 1']}),
  w('thermal_detonator','Detonador térmico','armas_distancia_ligeras',{damage:20,crit:2,range:'Curto',encumbrance:1,hardPoints:0,price:2000,rarity:8,restricted:true,qualities:['Brecha 1','Explosão 15','Letal 4','Munição limitada 1']}),
  w('shock_gloves','Luvas de choque','pelea',{damageBonus:0,crit:5,range:'Interação',encumbrance:0,hardPoints:1,price:500,rarity:2,qualities:['Atordoadora 3']}),
  w('brass_knuckles','Soqueiras','pelea',{damageBonus:1,crit:4,range:'Interação',encumbrance:1,hardPoints:0,price:25,rarity:0,qualities:['Desorientação 3']}),
  w('combat_knife','Faca de combate','armas_cuerpo_a_cuerpo',{damageBonus:1,crit:3,range:'Interação',encumbrance:1,hardPoints:0,price:25,rarity:1}),
  w('force_pike','Pique de força','armas_cuerpo_a_cuerpo',{damageBonus:3,crit:2,range:'Interação',encumbrance:3,hardPoints:3,price:500,rarity:4,qualities:['Modo atordoante','Perfurante 2']}),
  w('truncheon','Cassetete','armas_cuerpo_a_cuerpo',{damageBonus:2,crit:5,range:'Interação',encumbrance:2,hardPoints:0,price:15,rarity:1,qualities:['Desorientação 2']}),
  w('lightsaber','Sabre de luz','sabre_de_luz',{damage:10,crit:1,range:'Interação',encumbrance:1,hardPoints:0,price:10000,rarity:10,restricted:true,qualities:['Brecha 1','Hendedura','Letal 2']}),
  w('vibroknife','Vibrofaca','armas_cuerpo_a_cuerpo',{damageBonus:1,crit:2,range:'Interação',encumbrance:1,hardPoints:2,price:250,rarity:3,qualities:['Letal 1','Perfurante 2']}),
  w('vibrosword','Vibroespada','armas_cuerpo_a_cuerpo',{damageBonus:2,crit:2,range:'Interação',encumbrance:3,hardPoints:3,price:750,rarity:5,qualities:['Defensiva 1','Letal 1','Perfurante 2']}),
  w('vibroaxe','Vibromachado','armas_cuerpo_a_cuerpo',{damageBonus:3,crit:2,range:'Interação',encumbrance:4,hardPoints:3,price:750,rarity:5,qualities:['Hendedura','Letal 3','Perfurante 2']})
]);

export const CORE_ARMOR_REFERENCE=Object.freeze([
  Object.freeze({id:'padded_armor',name:'Armadura acolchoada',defense:0,soak:2,price:500,priceSource:SRC_STARTER_ARMOR_PRICE,source:SRC_ARMOR}),
  Object.freeze({id:'laminate_armor',name:'Armadura laminada',defense:0,soak:2,source:SRC_ARMOR}),
  Object.freeze({id:'heavy_battle_armor',name:'Armadura pesada de combate',defense:1,soak:2,price:5000,priceSource:SRC_STARTER_ARMOR_PRICE,source:SRC_ARMOR}),
  Object.freeze({id:'hostile_environment_gear',name:'Equipamento para ambientes hostis',defense:0,soak:1,source:SRC_ARMOR}),
  Object.freeze({id:'personal_deflector_shield',name:'Escudo defletor pessoal',defense:2,soak:0,source:SRC_ARMOR}),
  Object.freeze({id:'armored_clothing',name:'Roupa blindada',defense:1,soak:1,price:1000,priceSource:SRC_STARTER_ARMOR_PRICE,source:SRC_ARMOR}),
  Object.freeze({id:'heavy_clothing',name:'Roupa grossa',defense:0,soak:1,price:50,priceSource:SRC_STARTER_ARMOR_PRICE,source:SRC_ARMOR})
]);

function eg(id,name,price,encumbrance,rarity,{encumbranceThresholdBonus=0,medicine=null,effectSource=null,aliases=[],source=SRC_EOTE_CORE_GEAR}={}){
  const entry={id,name,category:'general_gear',price,encumbrance,rarity,restricted:false,source,aliases:Object.freeze([...aliases])};
  if(encumbranceThresholdBonus) entry.encumbranceThresholdBonus=encumbranceThresholdBonus;
  if(medicine) entry.medicine=Object.freeze({...medicine});
  if(effectSource) entry.effectSource=effectSource;
  return Object.freeze(entry);
}
export const EOTE_CORE_GENERAL_GEAR=Object.freeze([
  eg('glow_rod','Bastão luminoso',10,1,0), eg('comlink','Comunicador',25,0,0), eg('long_range_comlink','Comunicador de longo alcance',200,2,1),
  eg('electrobinoculars','Eletrobinóculos',250,1,1), eg('hand_scanner','Scanner manual',100,0,2), eg('multi_scanner','Scanner geral',500,2,3,{aliases:['Scanner multiuso']}),
  eg('macrobinoculars','Macrobinóculos',75,1,2), eg('breath_mask','Máscara respiratória',25,1,1), eg('space_suit','Traje espacial',100,4,1),
  eg('emergency_medpac','Medpac de emergência',100,1,1,{medicine:{medicalSupplies:true,boost:0},effectSource:SRC_GENERAL_GEAR_EFFECTS}), eg('medpac','Medpac',400,2,2,{medicine:{medicalSupplies:true,boost:1},effectSource:SRC_GENERAL_GEAR_EFFECTS}),
  eg('stimpack_injector','Injetor de estimulantes',25,0,1), eg('backpack','Mochila',50,0,0,{encumbranceThresholdBonus:4}),
  eg('climbing_gear','Equipamento de escalada',50,1,2), eg('data_module','Datapad',75,1,1,{aliases:['Módulo de dados']}),
  eg('emergency_repair_patch','Remendo de reparo de emergência',25,0,1), eg('power_pack_or_ammo_reload','Recarga adicional',25,1,1),
  eg('fusion_lantern','Lanterna de fusão',150,2,2), eg('tool_kit','Kit de ferramentas',350,4,2), eg('utility_belt','Cinto utilitário',25,0,0,{encumbranceThresholdBonus:1}),
  eg('rations','Rações',5,0,0),
  eg('appraisers_eye','Olho do Avaliador',700,0,7,{source:SRC_FLY_CASUAL_GEAR,aliases:["Appraiser's Eye"]})
]);
function ar(id,name,defense,soak,price,encumbrance,hardPoints,rarity,{restricted=false,aliases=[]}={}){return Object.freeze({id,name,category:'armor',defense,soak,price,encumbrance,hardPoints,rarity,restricted,aliases:Object.freeze([...aliases]),source:SRC_EOTE_CORE_ARMOR});}
export const EOTE_CORE_ARMOR_CATALOG=Object.freeze([
  ar('adverse_environment_gear','Equipamento para ambientes adversos',0,1,500,2,1,1,{aliases:['Equipamento para ambientes hostis']}),
  ar('armored_clothing','Roupa blindada',1,1,1000,3,1,6), ar('heavy_battle_armor','Armadura pesada de combate',1,2,5000,6,4,7,{restricted:true}),
  ar('heavy_clothing','Roupa grossa',0,1,50,1,0,0), ar('laminate_armor','Armadura laminada',0,2,2500,4,3,5),
  ar('personal_deflector_shield','Escudo defletor pessoal',2,0,10000,3,0,8), ar('padded_armor','Armadura acolchoada',0,2,500,2,0,1)
]);

export const ALL_WEAPON_CATALOG=Object.freeze((()=>{const byName=new Map(); for(const v of CORE_WEAPON_CATALOG) byName.set(String(v.name).toLocaleLowerCase('pt-BR'),v); for(const v of WEAPON_COMPENDIUM) if(!byName.has(String(v.name).toLocaleLowerCase('pt-BR'))) byName.set(String(v.name).toLocaleLowerCase('pt-BR'),v); return [...byName.values()];})());
export const FULL_WEAPON_CATALOG_COUNT=ALL_WEAPON_CATALOG.length;
export { WEAPON_COMPENDIUM_COUNT, weaponCompendium };
export function equipmentCatalog(){ return {weapons:ALL_WEAPON_CATALOG.map(v=>structuredClone(v)),armor:EOTE_CORE_ARMOR_CATALOG.map(v=>structuredClone(v)),generalGear:EOTE_CORE_GENERAL_GEAR.map(v=>structuredClone(v))}; }
export function weaponCatalogEntry(id){ return CORE_WEAPON_CATALOG.find(v=>v.id===id) ?? weaponCompendiumEntry(id) ?? null; }
export function generalGearCatalogEntry(id){ return EOTE_CORE_GENERAL_GEAR.find(v=>v.id===id||v.aliases?.includes(id)) ?? STARTER_GENERAL_GEAR_REFERENCE.find(v=>v.id===id) ?? null; }
export function armorCatalogEntry(id){ return EOTE_CORE_ARMOR_CATALOG.find(v=>v.id===id||v.aliases?.includes(id)) ?? CORE_ARMOR_REFERENCE.find(v=>v.id===id) ?? null; }
