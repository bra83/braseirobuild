import test from 'node:test';
import assert from 'node:assert/strict';
import {
  EOTE_CORE_CATALOG_VEHICLE_KEYS,EOTE_FLY_CASUAL_VEHICLE_KEYS,EOTE_ACTIVE_CATALOG_VEHICLE_KEYS,
  STARTER_VEHICLE_PROFILES,eoteActiveVehicleProfiles,createVehicleFromProfile
} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { genericEconomyCatalog, assetAvailability } from '../app/src/main/assets/vtt/js/rules/economy.mjs';

const expected={
  alpha_xg1_star_wing:{sil:3,speed:4,handling:-2,def:{proa:1,babor:0,estribor:0,popa:1},armor:3,hull:12,strain:8,price:135000,rarity:7,page:54,restricted:true},
  vt49_decimator:{sil:4,speed:3,handling:-3,def:{proa:2,babor:0,estribor:0,popa:2},armor:4,hull:30,strain:20,price:160000,rarity:6,page:55},
  wud500:{sil:4,speed:4,handling:-1,def:{proa:1,babor:0,estribor:0,popa:1},armor:1,hull:18,strain:14,price:98500,rarity:6,page:55},
  starlight:{sil:4,speed:3,handling:-1,def:{proa:1,babor:0,estribor:0,popa:1},armor:3,hull:20,strain:16,price:69995,rarity:5,page:56},
  ykl37r_nova_courier:{sil:4,speed:3,handling:0,def:{proa:1,babor:0,estribor:0,popa:1},armor:3,hull:24,strain:18,price:130000,rarity:6,page:56},
  gx1_short_hauler:{sil:4,speed:2,handling:-2,def:{proa:1,babor:0,estribor:0,popa:1},armor:2,hull:24,strain:15,price:85000,rarity:5,page:57},
  baudo_star_yacht:{sil:4,speed:4,handling:1,def:{proa:1,babor:0,estribor:0,popa:1},armor:1,hull:26,strain:10,price:250000,rarity:9,page:57},
  gozanti:{sil:5,speed:2,handling:-3,def:{proa:2,babor:2,estribor:2,popa:1},armor:5,hull:50,strain:36,price:200000,rarity:6,page:58},
  hwk1000:{sil:4,speed:5,handling:1,def:{proa:1,babor:0,estribor:0,popa:1},armor:3,hull:18,strain:15,price:120000,rarity:10,page:59,restricted:true},
  yt2000:{sil:4,speed:3,handling:0,def:{proa:1,babor:0,estribor:0,popa:1},armor:3,hull:24,strain:14,price:120000,rarity:5,page:60}
};

test('R36: Fly Casual adiciona exatamente 10 perfis EotE suplementares e catálogo ativo chega a 25',()=>{
  assert.equal(EOTE_CORE_CATALOG_VEHICLE_KEYS.length,15);
  assert.equal(EOTE_FLY_CASUAL_VEHICLE_KEYS.length,10);
  assert.equal(EOTE_ACTIVE_CATALOG_VEHICLE_KEYS.length,25);
  assert.deepEqual(Object.keys(eoteActiveVehicleProfiles()),EOTE_ACTIVE_CATALOG_VEHICLE_KEYS);
});

for(const [key,e] of Object.entries(expected)) test(`R36: ${key} reproduz ficha/preço/raridade de Fly Casual`,()=>{
  const v=createVehicleFromProfile(key);
  assert.deepEqual({sil:v.silhouette,speed:v.speedMax,handling:v.handling,def:v.defense,armor:v.armor,hull:v.hullThreshold,strain:v.systemStrainThreshold,price:v.price,rarity:v.rarity},
    {sil:e.sil,speed:e.speed,handling:e.handling,def:e.def,armor:e.armor,hull:e.hull,strain:e.strain,price:e.price,rarity:e.rarity});
  assert.equal(v.source.authority,'PRIMARY_EOTE_SUPPLEMENT');
  assert.equal(v.source.rulesProfile,'EOTE_CORE');
  assert.equal(v.source.activeRulesAuthority,true);
  assert.equal(v.source.book,'Fly Casual');
  assert.equal(v.source.printedPage,e.page);
  assert.equal(Boolean(v.restricted),Boolean(e.restricted));
});

test('R36: hiperdrive, carga e armamento essencial dos cargueiros ficam preservados',()=>{
  assert.equal(STARTER_VEHICLE_PROFILES.vt49_decimator.hyperdriveClass,1);
  assert.equal(STARTER_VEHICLE_PROFILES.wud500.backupHyperdriveClass,8);
  assert.equal(STARTER_VEHICLE_PROFILES.starlight.encumbranceCapacity,85);
  assert.equal(STARTER_VEHICLE_PROFILES.ykl37r_nova_courier.weapons.length,4);
  assert.equal(STARTER_VEHICLE_PROFILES.gx1_short_hauler.weapons[0].range,'Curto');
  assert.equal(STARTER_VEHICLE_PROFILES.baudo_star_yacht.hardPoints,6);
  assert.equal(STARTER_VEHICLE_PROFILES.gozanti.encumbranceCapacity,1000);
  assert.equal(STARTER_VEHICLE_PROFILES.gozanti.weapons.at(-1).qualities.limitedAmmo,6);
  assert.equal(STARTER_VEHICLE_PROFILES.hwk1000.weapons.length,0);
  assert.equal(STARTER_VEHICLE_PROFILES.yt2000.weapons[0].qualities.linked,1);
});

test('R36: economia expõe 25 veículos EotE auditados e continua excluindo X-Wing de referência cruzada',()=>{
  const vehicles=genericEconomyCatalog().vehicles;
  assert.equal(vehicles.length,25);
  assert.deepEqual(new Set(vehicles.map(x=>x.id)),new Set(EOTE_ACTIVE_CATALOG_VEHICLE_KEYS));
  assert.equal(vehicles.some(x=>x.id==='xwing_t65b'),false);
});

test('R36: restrição/raridade suplementar alimentam o mesmo motor de disponibilidade',()=>{
  const starWing=assetAvailability('vehicle','alpha_xg1_star_wing');
  const hwk=assetAvailability('vehicle','hwk1000');
  const yt=assetAvailability('vehicle','yt2000');
  assert.deepEqual([starWing.baseRarity,starWing.restricted],[7,true]);
  assert.deepEqual([hwk.baseRarity,hwk.restricted],[10,true]);
  assert.deepEqual([yt.baseRarity,yt.restricted],[5,false]);
});
