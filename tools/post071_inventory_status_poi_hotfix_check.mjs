import assert from 'node:assert/strict';
import fs from 'node:fs';
import { projectCharacterForSheet } from '../app/src/main/assets/www/core/character-projection.js';
import { characterStatusProjection } from '../app/src/main/assets/www/core/party-state.js';

const root=new URL('../',import.meta.url);
const read=p=>fs.readFileSync(new URL(p,root),'utf8');

const hero={
  characterId:'iria',name:'IRIA',classId:'FULL_EXPERT',level:1,
  attributes:{Strength:7,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:14,Charisma:9},
  hp:{current:5,max:5},systemStrain:{current:0,max:11},skills:{Survive:1},
  inventory:[{itemId:'club1',name:'Clava grande',kind:'WEAPON',carryState:'READIED',encumbrance:2,loaded:true}]
};
const sheet=projectCharacterForSheet(hero);
assert.equal(sheet.status,'OK');
assert.equal(sheet.inventory.length,1);
assert.equal(sheet.inventory[0].name,'Clava grande');
assert.equal(sheet.inventory[0].carryState,'READIED');
assert.equal(sheet.load.readiedPoints,2);
assert.equal(sheet.load.readiedCapacity,3);
assert.equal(sheet.load.stowedPoints,0);
assert.equal(sheet.load.stowedCapacity,7);

const bars=characterStatusProjection(hero);
assert.equal(bars.status,'OK');
assert.equal(bars.hp.fill,1);
assert.equal(bars.systemStrain.fill,0);

const ficha=read('app/src/main/assets/www/ficha.js');
assert.ok(ficha.includes("NativePersistencePort"));
assert.ok(ficha.includes("resolveActiveCampaignId"));
assert.ok(ficha.includes('renderEquipmentPane'));
assert.ok(ficha.includes('sheet.inventory'));
assert.ok(!ficha.includes('ITENS NÃO CARREGADOS'));
assert.ok(ficha.includes("loadPair(sheet.load?.readiedPoints,sheet.load?.readiedCapacity)"));

const jornadaCss=read('app/src/main/assets/www/jornada.css');
assert.ok(!jornadaCss.includes('hp_strain_reference_106x39.png'));
assert.ok(jornadaCss.includes('.track i{display:block;height:100%;width:0'));
const combatHtml=read('app/src/main/assets/www/scr022.html');
assert.ok(!combatHtml.includes('hp_strain_frame_106x39.png'));

const atlasJs=read('app/src/main/assets/www/scr014.js');
const atlasCss=read('app/src/main/assets/www/scr014.css');
assert.ok(atlasJs.includes("poiWrap.className='poi-structure'"));
assert.ok(atlasCss.includes('.poi-structure{'));
assert.ok(!atlasCss.includes('.hex-poi{'));

console.log('POST071_INVENTORY_STATUS_POI_HOTFIX_PASS inventory=live bars=dynamic poi=structure');
