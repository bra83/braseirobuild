import { CraftingService } from '../app/src/main/assets/www/core/crafting-service.js';
import { AdvancedMagicService } from '../app/src/main/assets/www/core/advanced-magic-service.js';
import { MajorProjectService } from '../app/src/main/assets/www/core/major-project-service.js';
import { RNG_ALGORITHM } from '../app/src/main/assets/www/core/rng-core.js';

const assert=(v,m)=>{if(!v)throw new Error(m)};
class MemoryPersistence{
  constructor(state){this.state=structuredClone(state);}
  loadCampaign(){return {status:'OK',revision:this.state.revision,stateJson:JSON.stringify(this.state)};}
  commit({expectedRevision,newStateJson,eventId}){if(expectedRevision!==this.state.revision)return {status:'REVISION_CONFLICT'};this.state=JSON.parse(newStateJson);return {status:'OK',revision:this.state.revision,eventId};}
}

const basePc={
  characterId:'pc',level:6,classId:'PARTIAL_EXPERT',partialClasses:['EXPERT','MAGE'],
  attributes:{Strength:10,Dexterity:10,Constitution:10,Intelligence:14,Wisdom:10,Charisma:10},
  skills:{Craft:1,Magic:2},foci:[],silver:200000,copper:0,inventory:[],
  craftingResources:{arcaneSalvage:5},unspentSkillPoints:0,
};
const baseState={
  revision:0,
  rngState:{algorithm:RNG_ALGORITHM,masterSeed:'wwn-stateful-smoke',counters:{}},
  characterState:structuredClone(basePc),
  charactersState:{pc:structuredClone(basePc)},
  worldState:{majorProjects:{}},
  factionState:{},
};

// Crafting lifecycle: runtime execution, not just syntax.
{
  const p=new MemoryPersistence(baseState),svc=new CraftingService({persistence:p});
  let r=svc.startCraft({campaignId:'c',pcId:'pc',blueprint:{name:'Mastercrafted blade',kind:'WEAPON',quality:'MASTERCRAFTED',marketCostSilver:100,normalBuildDays:7,consumable:false,scrapSalvage:false,workshopPlausible:true,partsSourcePlausible:true,encumbrance:1}});
  assert(r.status==='OK','startCraft');
  const jobId=r.jobId;
  r=svc.completeJob({campaignId:'c',pcId:'pc',jobId});
  assert(r.status==='EXPLICIT_CONTEXT_CONTRACT_NO_MUTATION','crafting time contract');
  r=svc.completeJob({campaignId:'c',pcId:'pc',jobId,elapsedDaysEvidence:14});
  assert(r.status==='OK','crafting completion');
  const item=r.character.inventory.find(x=>x.crafting?.quality==='MASTERCRAFTED');
  assert(item,'mastercrafted item materialized');
  // Add a maintained mod directly so recordMaintenance executes its capacity path.
  p.state.charactersState.pc.inventory[0].mods=[{modId:'CUSTOMIZED',status:'ACTIVE',minimumCraftSkill:1,maintenanceRequired:true,specificUserId:'pc'}];
  p.state.characterState=structuredClone(p.state.charactersState.pc);
  r=svc.recordMaintenance({campaignId:'c',pcId:'pc',itemId:p.state.charactersState.pc.inventory[0].itemId,modId:'CUSTOMIZED',hoursSincePrior:1});
  assert(r.status==='OK','maintenance runtime binding');
}

// Advanced magic: lab is persistent, materials are consumed, and only one research can be active.
{
  const p=new MemoryPersistence(baseState),svc=new AdvancedMagicService({persistence:p});
  let r=svc.establishResearchLab({campaignId:'c',pcId:'pc',maxSpellLevel:1,secureServiceableSite:true,materialsAvailableInMajorCity:true});
  assert(r.status==='OK','lab establishment');
  r=svc.startSpellResearch({campaignId:'c',pcId:'pc',spellId:'CUSTOM_1',name:'Custom Spell',spellLevel:1,spellConcept:'A narrow custom effect',gmApproved:true,gmAssignedLevel:true});
  assert(r.status==='OK','spell research start');
  const blocked=svc.startSpellResearch({campaignId:'c',pcId:'pc',spellId:'CUSTOM_2',name:'Second Spell',spellLevel:1,spellConcept:'Another effect',gmApproved:true,gmAssignedLevel:true});
  assert(blocked.status==='ONE_ACTIVE_SPELL_RESEARCH_ONLY_NO_MUTATION','single active research gate');
  const noTime=svc.resolveSpellResearch({campaignId:'c',pcId:'pc'});
  assert(noTime.status==='EXPLICIT_CONTEXT_CONTRACT_NO_MUTATION','research elapsed-time contract');
  r=svc.resolveSpellResearch({campaignId:'c',pcId:'pc',elapsedMonthsEvidence:1,onFailure:'ABANDON'});
  assert(r.status==='OK','spell research deterministic resolution');
}

// Major Project extreme aid: same-location evidence and actual HP sacrifice.
{
  const state=structuredClone(baseState);
  state.worldState.majorProjects.p1={projectId:'p1',title:'Raise the bridge',status:'ACTIVE',baseDifficulty:10,currentDifficulty:10,committedRenown:0,gmContract:{location:'HEX-01'},factionSupport:{},history:[]};
  state.factionState.F={wealth:2,force:2,cunning:2,treasure:3,assets:[{assetId:'A1',hp:5,location:'HEX-01'}]};
  const p=new MemoryPersistence(state),svc=new MajorProjectService({persistence:p});
  const r=svc.extremeFactionAid({campaignId:'c',projectId:'p1',factionId:'F',contributorId:'A1',hitPointsCommitted:2,physicalDice:{rolls:[1],forcedSettle:false}});
  assert(r.status==='OK','extreme faction aid runtime');
  assert(r.project.currentDifficulty===8,'project difficulty reduced by committed HP');
  assert(r.contributorHpAfter===2,'contributor loses committed HP plus d4 damage');
}

console.log('WWN stateful parity smoke tests: OK');
