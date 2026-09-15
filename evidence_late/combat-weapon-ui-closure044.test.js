import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { projectCombatForPlayer } from '../app/src/main/assets/www/core/combat-projection.js';
import { freshActionEconomy } from '../app/src/main/assets/www/core/wwn-tactical-combat.js';
import { WWN_EQUIPMENT_CATALOG } from '../app/src/main/assets/www/core/wwn-equipment-catalog.js';

function weaponItem(cat,id,{loaded=true,carryState='READIED'}={}){return {itemId:id,catalogId:cat.catalogId,kind:'WEAPON',name:cat.name,encumbrance:cat.encumbrance,carryState,traits:structuredClone(cat.traits),weaponModes:structuredClone(cat.modes),activeModeId:cat.modes[0]?.modeId??null,ammoType:cat.ammoType??null,loaded};}
function hero(inventory){return {characterId:'pc',name:'PC',level:1,classId:'FULL_EXPERT',attributes:{Strength:10,Dexterity:14,Constitution:10,Intelligence:10,Wisdom:10,Charisma:10},baseAttackBonus:0,hp:{current:5,max:5},systemStrain:{current:0,max:10},skills:{Shoot:1,Stab:0,Punch:0},inventory};}
function state(pc){return {characterState:structuredClone(pc),charactersState:{pc:structuredClone(pc)},npcState:{e:{npcId:'e',name:'Enemy',hp:5,maxHp:5}},encounterState:{combat:{active:true,round:1,order:['PC','ENEMY'],activeSideId:'PC',pcId:'pc',pcIds:['pc'],enemyIds:['e'],tactical:{units:'feet',actors:{pc:{actorId:'pc',sideId:'PC',xFeet:0,yFeet:0},e:{actorId:'e',sideId:'ENEMY',xFeet:20,yFeet:0}}},economy:{pc:freshActionEconomy(),e:freshActionEconomy()}}}};}

test('combat projection exposes only owned weapon choices plus built-in unarmed attack',()=>{const bow=weaponItem(WWN_EQUIPMENT_CATALOG.BOW_SMALL,'bow',{loaded:false}),arrows={itemId:'arrows',kind:'AMMO',name:'Flechas',encumbrance:1,carryState:'READIED',ammoType:'ARROW',quantity:20};const view=projectCombatForPlayer(state(hero([bow,arrows])));const choices=view.pcs[0].weapons.choices;assert.ok(choices.some(x=>x.weaponId==='UNARMED'&&x.canAttack));const b=choices.find(x=>x.weaponId==='bow');assert.equal(b.readiness,'RELOAD_REQUIRED');assert.equal(b.reloadRequired,true);assert.equal(b.actionCost,'ON_TURN');assert.equal(b.canAttack,false);});

test('thrown weapon projection offers the two SRD combat-skill choices Shoot and Stab, never Exert',()=>{const dagger=weaponItem(WWN_EQUIPMENT_CATALOG.DAGGER,'dagger');const view=projectCombatForPlayer(state(hero([dagger])));const thrown=view.pcs[0].weapons.choices.filter(x=>x.weaponId==='dagger'&&x.modeId==='THROWN');assert.deepEqual(thrown.map(x=>x.combatSkill).sort(),['Shoot','Stab']);assert.equal(thrown.some(x=>x.combatSkill==='Exert'),false);});

test('SCR-022 weapon UI sends selected weapon, mode and combat skill to the authoritative session service',()=>{const js=fs.readFileSync(new URL('../app/src/main/assets/www/scr022.js',import.meta.url),'utf8');const html=fs.readFileSync(new URL('../app/src/main/assets/www/scr022.html',import.meta.url),'utf8');assert.match(js,/weaponId:choice\.weaponId,modeId:choice\.modeId,combatSkill:choice\.combatSkill/);assert.match(js,/session\.reloadWeapon/);assert.doesNotMatch(js,/weaponId:'UNARMED'/);assert.match(html,/id="weaponButton"/);assert.match(html,/id="reloadButton"/);assert.match(html,/id="weaponPopup"/);});
