function keyOf(ref){return String(ref?.sourceId??ref?.id??'').trim();}
function clone(v){return structuredClone(v);}
export class SourceAuthorityRegistry{
  constructor({excludedSystems=['OSE','D&D','PATHFINDER','SWN','GODBOUND']}={}){this.sources=new Map();this.rules=new Map();this.excluded=new Set(excludedSystems.map(v=>v.toUpperCase()));}
  registerSource(source){const id=keyOf(source);if(!id)throw new Error('sourceId required');const system=String(source.system??'WWN').toUpperCase();if(this.excluded.has(system))throw new Error(`excluded system ${system}`);const priority=Number.isInteger(source.priority)?source.priority:100;this.sources.set(id,Object.freeze({...clone(source),sourceId:id,system,priority}));return this.sources.get(id);}
  registerRule(rule){if(!rule?.ruleId)throw new Error('ruleId required');if(!Array.isArray(rule.sourceRefs)||rule.sourceRefs.length<1)throw new Error('sourceRefs required');for(const ref of rule.sourceRefs){const id=keyOf(ref);if(!this.sources.has(id))throw new Error(`unknown source ${id}`);}const list=this.rules.get(rule.ruleId)??[];list.push(Object.freeze(clone(rule)));list.sort((a,b)=>this._priority(a)-this._priority(b));this.rules.set(rule.ruleId,list);return list[0];}
  _priority(rule){return Math.min(...rule.sourceRefs.map(ref=>this.sources.get(keyOf(ref))?.priority??9999));}
  lookup(ruleId){const candidates=this.rules.get(ruleId)??[];if(!candidates.length)return Object.freeze({status:'MISSING_EVIDENCE',ruleId});const primary=candidates[0];const conflict=candidates.slice(1).filter(r=>canonicalMechanics(r)!==canonicalMechanics(primary));return Object.freeze({status:conflict.length?'KNOWN_WITH_CONFLICT':'KNOWN',ruleId,primary:clone(primary),conflicts:clone(conflict),sourcePriority:this._priority(primary)});}
  trace(ref){const id=keyOf(ref);return this.sources.has(id)?Object.freeze({status:'KNOWN',source:clone(this.sources.get(id))}):Object.freeze({status:'MISSING_EVIDENCE',sourceId:id});}
}
function canonicalMechanics(rule){return JSON.stringify(rule.mechanics??rule.contract??null,Object.keys(rule.mechanics??rule.contract??{}).sort());}
