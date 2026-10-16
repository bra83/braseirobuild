import test from 'node:test';
import assert from 'node:assert/strict';
import {
  EOTE_CORE_CATALOG_VEHICLE_KEYS, STARTER_VEHICLE_PROFILES, eoteCoreVehicleProfiles, createVehicleFromProfile
} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { genericEconomyCatalog, assetAvailability } from '../app/src/main/assets/vtt/js/rules/economy.mjs';

const expected={
  x34:{sil:2,speed:2,handling:0,armor:0,hull:4,strain:5,price:4500,rarity:2,page:251},
  digger_crawler:{sil:4,speed:1,handling:-3,armor:4,hull:40,strain:30,price:50000,rarity:7,page:251},
  personnel_carrier:{sil:3,speed:2,handling:-2,armor:0,hull:8,strain:4,price:3000,rarity:2,page:252},
  at_pt:{sil:2,speed:2,handling:0,armor:3,hull:6,strain:5,price:30000,rarity:7,page:253},
  at_est:{sil:4,speed:1,handling:-2,armor:2,hull:20,strain:12,price:12000,rarity:5,page:253},
  cloakshape:{sil:3,speed:4,handling:0,armor:3,hull:10,strain:8,price:38000,rarity:4,page:254},
  jumpmaster_jm5000:{sil:4,speed:4,handling:1,armor:2,hull:14,strain:8,price:55000,rarity:5,page:256},
  firespray:{sil:4,speed:4,handling:0,armor:4,hull:15,strain:12,price:80000,rarity:4,page:255},
  lambda_t4a:{sil:4,speed:3,handling:0,armor:4,hull:25,strain:15,price:140000,rarity:6,page:'256-257'},
  tie_ln:{sil:3,speed:5,handling:3,armor:2,hull:6,strain:8,price:50000,rarity:4,page:257},
  ywing_btl:{sil:3,speed:4,handling:0,armor:3,hull:12,strain:10,price:80000,rarity:4,page:258},
  z95_af4:{sil:3,speed:4,handling:1,armor:3,hull:9,strain:8,price:55000,rarity:4,page:258},
  skipray_gat12h:{sil:4,speed:4,handling:-1,armor:4,hull:20,strain:15,price:150000,rarity:6,page:259},
  wayfarer:{sil:5,speed:3,handling:-2,armor:4,hull:32,strain:20,price:120000,rarity:5,page:'263-264'},
  yt1300:{sil:4,speed:3,handling:-1,armor:3,hull:22,strain:15,price:100000,rarity:4,page:264}
};

test('R35: catálogo veicular Core EotE possui 15 perfis ativos e nenhum AoR',()=>{
  assert.equal(EOTE_CORE_CATALOG_VEHICLE_KEYS.length,15);
  assert.deepEqual(Object.keys(eoteCoreVehicleProfiles()),EOTE_CORE_CATALOG_VEHICLE_KEYS);
  for(const key of EOTE_CORE_CATALOG_VEHICLE_KEYS){
    const p=STARTER_VEHICLE_PROFILES[key];
    assert.equal(p.source.authority,'PRIMARY_EOTE_CORE',key);
    assert.equal(p.source.rulesProfile,'EOTE_CORE',key);
    assert.equal(p.source.activeRulesAuthority,true,key);
    assert.match(p.source.book,/Edge of the Empire Core Rulebook/i,key);
  }
});

for(const [key,e] of Object.entries(expected)) test(`R35: ${key} reproduz perfil/custo/raridade Core`,()=>{
  const v=createVehicleFromProfile(key);
  assert.deepEqual({sil:v.silhouette,speed:v.speedMax,handling:v.handling,armor:v.armor,hull:v.hullThreshold,strain:v.systemStrainThreshold,price:v.price,rarity:v.rarity},
    {sil:e.sil,speed:e.speed,handling:e.handling,armor:e.armor,hull:e.hull,strain:e.strain,price:e.price,rarity:e.rarity});
  assert.equal(v.source.printedPage,e.page);
});

test('R35: perfis terrestres Core preservam capacidade e sensores comprovados',()=>{
  assert.deepEqual(STARTER_VEHICLE_PROFILES.x34.defense,{proa:0,babor:0,estribor:0,popa:0});
  assert.equal(STARTER_VEHICLE_PROFILES.x34.maximumAltitude,'1 metro');
  assert.equal(STARTER_VEHICLE_PROFILES.digger_crawler.encumbranceCapacity,400);
  assert.equal(STARTER_VEHICLE_PROFILES.personnel_carrier.passengers,'4-15');
  assert.equal(STARTER_VEHICLE_PROFILES.at_pt.weapons[1].scale,'personal');
  assert.equal(STARTER_VEHICLE_PROFILES.at_est.sensorRange,'Longo');
});

test('R35: naves Core preservam hiperdrive/defesa/restrição e munição',()=>{
  assert.equal(STARTER_VEHICLE_PROFILES.cloakshape.hyperdrive,false);
  assert.equal(STARTER_VEHICLE_PROFILES.jumpmaster_jm5000.hyperdriveClass,3);
  assert.deepEqual(STARTER_VEHICLE_PROFILES.lambda_t4a.defense,{proa:2,babor:0,estribor:0,popa:1});
  assert.equal(STARTER_VEHICLE_PROFILES.lambda_t4a.restricted,true);
  assert.equal(STARTER_VEHICLE_PROFILES.tie_ln.restricted,true);
  assert.equal(STARTER_VEHICLE_PROFILES.ywing_btl.weapons[2].qualities.limitedAmmo,8);
  assert.equal(STARTER_VEHICLE_PROFILES.z95_af4.weapons[1].qualities.limitedAmmo,6);
  assert.equal(STARTER_VEHICLE_PROFILES.skipray_gat12h.weapons[2].qualities.limitedAmmo,5);
});

test('R35: os 15 veículos Core permanecem disponíveis na economia e X-Wing segue excluído',()=>{
  const vehicles=genericEconomyCatalog().vehicles;
  const ids=new Set(vehicles.map(x=>x.id));
  for(const key of EOTE_CORE_CATALOG_VEHICLE_KEYS) assert.ok(ids.has(key),key);
  assert.equal(vehicles.some(x=>x.id==='xwing_t65b'),false);
});

test('R35: disponibilidade de veículo respeita raridade e restrição do Core',()=>{
  const legal=assetAvailability('vehicle','x34');
  const restricted=assetAvailability('vehicle','lambda_t4a');
  assert.equal(legal.baseRarity,2); assert.equal(legal.restricted,false);
  assert.equal(restricted.baseRarity,6); assert.equal(restricted.restricted,true);
});

test('R35: X-Wing continua fail-closed na economia até fonte EotE própria',()=>{
  assert.equal(STARTER_VEHICLE_PROFILES.xwing_t65b.source.authority,'CROSS_LINE_REFERENCE');
  assert.equal(STARTER_VEHICLE_PROFILES.xwing_t65b.rarity,undefined);
  assert.throws(()=>assetAvailability('vehicle','xwing_t65b'),/preço\/raridade mecânicos comprovados/i);
});
