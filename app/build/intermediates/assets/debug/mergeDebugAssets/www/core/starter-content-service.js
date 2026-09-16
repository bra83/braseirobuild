import { projectNpcForPlayer } from './npc-registry.js';

function clone(v){return structuredClone(v)}
function publicMajorSite(site){return {id:site.id,name:site.name,kind:site.kind,summary:site.summary,situation:site.situation,hooks:clone(site.hooks??[])};}
function publicFaction(f){return {id:f.id,name:f.name,publicFace:f.public_face??null};}

export function validateStarterContentPack(pack){
  const errors=[];
  if(!pack||typeof pack!=='object')return Object.freeze({ok:false,errors:Object.freeze(['pack required'])});
  if(pack.pack_id!=='WWN_STARTER_REGION_CONTENT_V1')errors.push('unsupported pack_id');
  if(!pack.region?.name)errors.push('region name required');
  if(!pack.settlement?.name)errors.push('settlement required');
  if(!Array.isArray(pack.major_sites)||pack.major_sites.length<3)errors.push('major_sites must contain at least 3 sites');
  if(!Array.isArray(pack.npcs))errors.push('npcs array required');
  return Object.freeze({ok:errors.length===0,errors:Object.freeze(errors)});
}

export function buildStarterContentState(pack){
  const valid=validateStarterContentPack(pack);if(!valid.ok)throw new Error(valid.errors.join('; '));
  const publicState={
    packId:pack.pack_id,campaign:pack.campaign,
    region:{name:pack.region.name,tone:pack.region.tone,scope:pack.region.scope,atlasScale:pack.region.atlas_scale},
    settlement:{id:pack.settlement.id,name:pack.settlement.name,type:pack.settlement.type,summary:pack.settlement.summary,districts:clone(pack.settlement.districts??[]),services:clone(pack.settlement.services??[]),activeProblems:clone(pack.settlement.active_problems??[])},
    majorSites:pack.major_sites.map(publicMajorSite),
    minorPois:clone(pack.minor_pois??[]),
    factions:(pack.factions??[]).map(publicFaction),
    rumors:clone(pack.rumors??[]),
    startingHooks:clone(pack.starting_hooks??[]),
  };
  return Object.freeze({public:Object.freeze(publicState),gm:Object.freeze(clone(pack))});
}

export function seedNpcStateFromContentPack(pack){
  const valid=validateStarterContentPack(pack);if(!valid.ok)throw new Error(valid.errors.join('; '));
  const out={};
  for(const raw of pack.npcs??[]){
    const npcId=raw.id; if(!npcId||out[npcId])throw new Error('npc id required/unique');
    out[npcId]={
      npcId,name:raw.name,location:raw.location??pack.settlement?.name??null,
      publicFacts:[raw.role?`Função: ${raw.role}`:null].filter(Boolean),
      gmSecrets:[raw.goal?`Objetivo: ${raw.goal}`:null,...(raw.memory_hooks??[]).map(v=>`Memória relevante: ${v}`)].filter(Boolean),
      memories:[],visibleStatus:null,currentIntent:raw.goal??null,
    };
  }
  return Object.freeze(out);
}

export function applyStarterContentToState(state,pack){
  if(!state||typeof state!=='object')throw new TypeError('state required');
  const contentState=buildStarterContentState(pack);const seeded=seedNpcStateFromContentPack(pack);
  const next=clone(state);next.contentState=contentState;next.npcState={...seeded,...(next.npcState??{})};
  return Object.freeze(next);
}

export function projectStarterContentForPlayer(state){
  const c=state?.contentState;if(!c)return Object.freeze({status:'NO_CONTENT'});
  return Object.freeze({status:'OK',content:clone(c.public),npcs:Object.fromEntries(Object.entries(state.npcState??{}).map(([id,npc])=>[id,projectNpcForPlayer(npc)]))});
}
