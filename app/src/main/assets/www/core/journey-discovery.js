function clone(v){return structuredClone(v)}
function norm(v){return String(v??'').normalize('NFKD').replace(/[\u0300-\u036f]/g,'').trim().toUpperCase()}
function fnv1a(text){let h=0x811c9dc5;for(const c of String(text)){h^=c.codePointAt(0);h=Math.imul(h,0x01000193)>>>0;}return h>>>0}

export function selectJourneyFeed(state,content,{hookCount=1,rumorCount=2}={}){
  const hooks=Array.isArray(content?.startingHooks)?content.startingHooks:[];const rumors=Array.isArray(content?.rumors)?content.rumors:[];
  const revision=Math.max(0,Number(state?.revision??0));const seed=String(state?.rngState?.masterSeed??state?.campaignId??'wwn');const base=fnv1a(seed);
  const selectedHooks=[];if(hooks.length){const start=(base+revision)%hooks.length;for(let i=0;i<Math.min(hookCount,hooks.length);i++)selectedHooks.push(clone(hooks[(start+i)%hooks.length]));}
  const selectedRumors=[];if(rumors.length){const start=(Math.imul(base,3)+revision*2)>>>0;for(let i=0;i<Math.min(rumorCount,rumors.length);i++)selectedRumors.push(String(rumors[(start+i*3)%rumors.length]));}
  return Object.freeze({hooks:Object.freeze(selectedHooks),rumors:Object.freeze(selectedRumors),revision,seedDigest:base});
}

export function knownJourneyLocations(state,content){
  const names=new Set();const add=v=>{const n=norm(v);if(n)names.add(n)};add(state?.worldState?.position?.name);
  for(const hex of Object.values(state?.worldState?.atlas?.hexes??{})){if(!(hex?.discovered===true||hex?.entered===true||hex?.visited===true||hex?.poiRevealed===true))continue;add(hex.poiName);add(hex.placeName);}
  const majorSites=(content?.majorSites??[]).filter(x=>names.has(norm(x?.name))).map(clone);
  const minorPois=(content?.minorPois??[]).filter(x=>names.has(norm(x?.name))).map(clone);
  return Object.freeze({majorSites:Object.freeze(majorSites),minorPois:Object.freeze(minorPois),knownNames:Object.freeze([...names])});
}
