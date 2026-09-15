const SRC=Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:'§6.8–6.10 Faction Assets'});
const freeze=(v)=>Object.freeze(v);
function id(name){return name.toUpperCase().replace(/[^A-Z0-9]+/g,'_').replace(/^_|_$/g,'');}
function a(attribute,tier,name,cost,hp,magic,attack,counter,qualities=''){return freeze({assetId:id(name),name,attribute,tier,cost,hp,magic,attack:attack==='None'?null:attack,counter:counter==='None'?null:counter,qualities:freeze(qualities?qualities.split(',').map(x=>x.trim().toUpperCase()):[]),sourceRefs:freeze([SRC])});}
export const WWN_FACTION_ASSETS=freeze([
// Cunning
...[
[1,'Informers',2,3,'None','C v. C/Special','None','Subtle, Special'],[1,'Petty Seers',2,2,'Medium','None','1d6 damage','Subtle'],[1,'Smugglers',2,4,'None','C v. W/1d4 damage','None','Subtle, Action'],[1,'Useful Idiots',1,2,'None','None','None','Subtle, Special'],
[2,'Blackmail',4,4,'None','C v. C/1d4 damage','None','Subtle, Special'],[2,'Dancing Girls',4,3,'None','C v. W/2d4 damage','None','Subtle, Special'],[2,'Hired Friends',4,4,'None','C v. C/1d6 damage','None','Subtle, Special'],[2,'Saboteurs',5,6,'None','C v. W/2d4 damage','None','Subtle, Special'],
[3,'Bewitching Charmer',6,4,'Low','C v. C/Special','None','Subtle, Special'],[3,'Covert Transport',8,4,'None','None','None','Subtle, Special'],[3,'Occult Infiltrators',6,4,'Medium','C v. C/2d6 damage','None','Subtle, Special'],[3,'Spymaster',8,4,'None','C v. C/1d6 damage','2d6 damage','Subtle'],
[4,'Court Patronage',8,8,'None','C v. C/1d6 damage','1d6 damage','Subtle, Special'],[4,'Idealistic Thugs',8,12,'None','C v. F/1d6 damage','1d6 damage','Subtle'],[4,'Seditionists',12,8,'None','Special','None','Subtle'],[4,'Vigilant Agents',12,8,'None','None','1d4 damage','Subtle, Special'],
[5,'Cryptomancers',14,6,'Low','C v. C/Special','None','Subtle'],[5,'Organization Moles',8,10,'None','C v. C/2d6 damage','None','Subtle'],[5,'Shapeshifters',14,8,'Medium','C v. C/2d6 damage','None','Subtle, Special'],
[6,'Interrupted Logistics',20,10,'None','None','None','Subtle, Special'],[6,'Prophet',20,10,'None','C v. C/2d8 damage','1d8 damage','Subtle'],[6,'Underground Roads',18,15,'None','None','None','Subtle, Special'],
[7,'Expert Treachery',10,5,'None','C v. C/Special','None','Subtle'],[7,'Mindbenders',20,10,'Medium','None','2d8 damage','Subtle'],[7,'Popular Movement',25,16,'None','C v. C/2d6 damage','1d6 damage','Subtle, Special'],
[8,'Just As Planned',40,15,'None','None','1d10 damage','Subtle, Special'],[8,'Omniscient Seers',30,10,'High','None','1d8 damage','Subtle, Special']].map(x=>a('CUNNING',...x)),
// Force
...[
[1,'Fearful Intimidation',2,4,'None','None','1d4 damage',''],[1,'Local Guard',3,4,'None','F v. F/1d3+1 damage','1d4+1 damage',''],[1,'Summoned Hunter',4,4,'Medium','C v. F/1d6 damage','None','Subtle'],[1,'Thugs',2,1,'None','F v. C/1d6 damage','None','Subtle'],
[2,'Guerrilla Populace',6,4,'None','F v. F/1d4+1 damage','None',''],[2,'Military Transport',4,6,'None','None','None','Action'],[2,'Reserve Corps',4,4,'None','F v. F/1d6 damage','1d6 damage',''],[2,'Scouts',5,5,'None','F v. F/2d4 damage','1d4+1 damage','Subtle'],
[3,'Enchanted Elites',8,6,'Medium','F v. F/1d10 damage','1d6 damage','Subtle'],[3,'Infantry',6,6,'None','F v. F/1d8 damage','1d6 damage',''],[3,'Temple Fanatics',4,6,'None','F v. F/2d6 damage','2d6 damage','Special'],[3,'Witch Hunters',6,4,'Low','C v. C/1d4+1 damage','1d6 damage',''],
[4,'Cavalry',8,12,'None','F v. F/2d6 damage','1d4 damage',''],[4,'Military Roads',10,10,'None','None','None','Action'],[4,'Vanguard Unit',10,10,'None','None','1d6 damage','Action'],[4,'War Fleet',12,8,'None','F v. F/2d6 damage','1d8 damage','Action'],
[5,'Demonic Slayer',12,4,'High','C v. C/2d6+2 damage','None','Subtle, Special'],[5,'Magical Logistics',14,6,'Medium','None','None','Special'],[5,'Siege Experts',10,8,'None','F v. W/1d6 damage','1d6 damage',''],
[6,'Fortification Program',20,18,'None','None','2d6 damage','Action'],[6,'Knights',18,16,'None','F v. F/2d8 damage','2d6 damage',''],[6,'War Machines',25,14,'Medium','F v. F/2d10+4 damage','1d10 damage',''],
[7,'Brilliant General',25,8,'None','C v. F/1d8 damage','None','Subtle, Special'],[7,'Purity Rites',20,10,'Low','None','2d8+2 damage','Special'],[7,'Warshaped',30,16,'High','F v. F/2d8+2 damage','2d8 damage','Subtle'],
[8,'Apocalypse Engine',35,20,'Medium','F v. F/3d10+4 damage','None',''],[8,'Invincible Legion',40,30,'None','F v. F/2d10+4 damage','2d10+4 damage','Special']].map(x=>a('FORCE',...x)),
// Wealth
...[
[1,'Armed Guards',1,3,'None','W v. F/1d3 damage','1d4 damage',''],[1,'Cooperative Businesses',1,2,'None','W v. W/1d4-1 damage','None','Subtle, Special'],[1,'Farmers',2,4,'None','None','1d4 damage','Action'],[1,'Front Merchant',2,3,'None','W v. W/1d4 damage','1d4-1 damage','Subtle'],
[2,'Caravan',5,4,'None','W v. W/1d4 damage','None','Action'],[2,'Dragomans',4,4,'None','None','1d4 damage','Subtle, Special'],[2,'Pleaders',6,4,'None','C v. W/2d4 damage','1d6 damage','Special'],[2,'Worker Mob',4,6,'None','W v. F/1d4+1 damage','1d4 damage',''],
[3,'Ancient Mechanisms',8,4,'Medium','None','None','Special'],[3,'Arcane Laboratory',6,4,'None','None','None','Special'],[3,'Free Company',8,6,'None','W v. F/2d4+2 damage','1d6 damage','Action, Special'],[3,'Manufactory',8,4,'None','None','1d4 damage','Action'],
[4,'Healers',12,8,'None','None','None','Action'],[4,'Monopoly',8,12,'None','W v. W/1d6 damage','1d6 damage','Action'],[4,'Occult Countermeasures',10,8,'Low','W v. C/2d10 damage','1d10 damage','Special'],[4,'Usurers',12,8,'None','W v. W/1d10 damage','None','Action'],
[5,'Mad Genius',6,2,'None','W v. C/1d6 damage','None','Action'],[5,'Smuggling Fleet',12,6,'None','W v. F/2d6 damage','None','Subtle, Action'],[5,'Supply Interruption',10,8,'None','C v. W/1d6 damage','None','Subtle, Action'],
[6,'Economic Disruption',25,10,'None','W v. W/2d6 damage','None','Subtle, Action'],[6,'Merchant Prince',20,10,'None','W v. W/2d8 damage','1d8 damage','Action'],[6,'Trade Company',15,10,'None','W v. W/2d6 damage','1d6 damage','Action'],
[7,'Ancient Workshop',25,16,'Medium','None','None',''],[7,'Lead or Silver',20,10,'None','W v. W/2d10 damage','2d8 damage',''],[7,'Transport Network',15,5,'None','W v. W/1d12 damage','None','Action'],
[8,'Golden Prosperity',40,30,'Medium','None','2d10 damage',''],[8,'Hired Legion',30,20,'None','W v. F/2d10+4 damage','2d10 damage','Action']].map(x=>a('WEALTH',...x)),
]);
export const WWN_FACTION_ASSET_BY_ID=freeze(Object.fromEntries(WWN_FACTION_ASSETS.map(x=>[x.assetId,x])));
export function factionAsset(assetId){return WWN_FACTION_ASSET_BY_ID[id(assetId)]??null;}
const MAGIC_ORDER=Object.freeze({NONE:0,LOW:1,MEDIUM:2,HIGH:3});
export function canPurchaseFactionAsset({assetId,factionAttributeRating,locationMagic='NONE',treasure}){const asset=factionAsset(assetId);if(!asset)return freeze({ok:false,status:'UNKNOWN_ASSET'});if(!Number.isInteger(factionAttributeRating)||factionAttributeRating<1||factionAttributeRating>8)throw new RangeError('factionAttributeRating must be 1..8');if(!Number.isInteger(treasure)||treasure<0)throw new RangeError('treasure must be >=0');const magic=String(locationMagic).toUpperCase();if(!(magic in MAGIC_ORDER))throw new RangeError('invalid locationMagic');const requiredMagic=String(asset.magic).toUpperCase();const attributeOk=factionAttributeRating>=asset.tier,magicOk=MAGIC_ORDER[magic]>=MAGIC_ORDER[requiredMagic],treasureOk=treasure>=asset.cost;return freeze({ok:attributeOk&&magicOk&&treasureOk,status:attributeOk&&magicOk&&treasureOk?'OK':'PURCHASE_REQUIREMENTS_NOT_MET',assetId:asset.assetId,attributeOk,magicOk,treasureOk,cost:asset.cost,tier:asset.tier,requiredMagic,sourceRefs:asset.sourceRefs});}
