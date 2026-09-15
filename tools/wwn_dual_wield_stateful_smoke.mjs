import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { ensureStarterCampaign } from '../app/src/main/assets/www/core/starter-bootstrap.js';
import { SceneSessionService } from '../app/src/main/assets/www/core/scene-session-service.js';
import { DeterministicNarrativeProvider } from '../app/src/main/assets/www/core/narrative-provider.js';
import { CombatSessionService } from '../app/src/main/assets/www/core/combat-session-service.js';
import { EquipmentService } from '../app/src/main/assets/www/core/equipment-service.js';
import { projectCombatForPlayer } from '../app/src/main/assets/www/core/combat-projection.js';

const assert=(v,m)=>{if(!v)throw new Error(m)};
const pack=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wwn.starter-region.v1.json',import.meta.url),'utf8'));
const base=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json',import.meta.url),'utf8'));
let runtime=null;
for(let attempt=1;attempt<=24&&!runtime;attempt++){
  const persistence=new MemoryPersistenceAdapter(),character=structuredClone(base);character.characterId=`dual-${attempt}`;character.name='DUAL TEST';character.skills={...(character.skills??{}),Stab:1};character.inventory=null;delete character.silver;delete character.copper;
  const boot=ensureStarterCampaign({persistence,character,pack,masterSeed:`wwn-dual-wield-smoke-${attempt}`});if(!String(boot.status).startsWith('OK'))continue;const campaignId=boot.campaignId,barbara=new DeterministicNarrativeProvider();const scenes=new SceneSessionService({persistence,barbara});const combat=new CombatSessionService({persistence,barbara});const equipment=new EquipmentService({persistence});
  const opened=await scenes.openContentScene({campaignId,contentType:'majorSite',contentId:'site_vigilia'});if(!['OK','NARRATIVE_PENDING','MECHANICAL_COMMITTED_NARRATIVE_PENDING'].includes(opened.status))continue;
  const gear=equipment.initializeStartingGear({campaignId,pcId:character.characterId});if(gear.status!=='OK')continue;
  const dagger=equipment.purchase({campaignId,pcId:character.characterId,catalogId:'DAGGER',carryState:'READIED'});const sword=equipment.purchase({campaignId,pcId:character.characterId,catalogId:'SWORD_SHORT',carryState:'READIED'});if(dagger.status!=='OK'||sword.status!=='OK')continue;
  const started=await combat.startAtVigilia({campaignId});if(started.status!=='OK')continue;const projection=projectCombatForPlayer(JSON.parse(persistence.loadCampaign(campaignId).stateJson));if(projection.activeSideId==='PC')runtime={persistence,campaignId,combat,pcId:character.characterId,dagger:dagger.item,sword:sword.item};
}
assert(runtime,'could not create deterministic PC-first dual-wield combat');
const {persistence,campaignId,combat,pcId,dagger,sword}=runtime;
let state=JSON.parse(persistence.loadCampaign(campaignId).stateJson),projection=projectCombatForPlayer(state),pc=projection.pcs.find(x=>x.actorId===pcId),enemy=projection.enemies.find(x=>!x.defeated);assert(pc&&enemy,'combatants missing');
const from=pc.position,to=enemy.position,dx=to.xFeet-from.xFeet,dy=to.yFeet-from.yFeet,d=Math.hypot(dx,dy);if(d>5){const step=d-5,move=await combat.movePc({campaignId,pcId,xFeet:from.xFeet+dx/d*step,yFeet:from.yFeet+dy/d*step});assert(move.status==='OK',`move failed: ${move.status}`);}
const attack=await combat.dualWieldAttackNpc({campaignId,pcId,npcId:enemy.actorId,primaryWeaponId:dagger.itemId,primaryModeId:'MELEE',primaryCombatSkill:'Stab',secondaryWeaponId:sword.itemId,secondaryModeId:'MELEE',secondaryCombatSkill:'Stab'});
assert(attack.status==='OK',`dual wield failed: ${attack.status}`);assert(attack.dualWield?.plan?.legal===true,'dual wield plan must be legal');assert(attack.dualWield.plan.hitModifier===-1,'dual wield must impose -1 hit');assert(attack.dualWield.plan.hitDamageBonus===2,'dual wield must add +2 damage when both weapons reach');assert(attack.attackContext?.kind==='DUAL_WIELD','dual wield context missing from result');assert(attack.resolution.damageBonus>=2,'attack resolution must carry dual-wield damage bonus');
state=JSON.parse(persistence.loadCampaign(campaignId).stateJson);const saved=state.encounterState?.lastCombatResolution?.attackContext;assert(saved?.kind==='DUAL_WIELD','dual wield context must persist');assert(saved.secondaryWeaponId===sword.itemId,'secondary weapon identity must persist');assert(state.encounterState.combat.economy[pcId].mainAvailable===false,'dual-wield attack must consume Main action exactly once');
console.log('WWN dual-wield stateful smoke: OK');
