const SRC=id=>Object.freeze({sourceId:'SRC-WWN-SRD-1',locator:`§1.3.3 Example Backgrounds — ${id}`});
const f=v=>Object.freeze(v);
export const WWN_BACKGROUND_CATALOG=f({
  ARTISAN:f({backgroundId:'ARTISAN',name:'Artisan',freeSkill:'Craft',learning:f(['Connect','Convince','Craft','Exert','Know','Notice','Trade']),sourceRef:SRC('Artisan')}),
  BARBARIAN:f({backgroundId:'BARBARIAN',name:'Barbarian',freeSkill:'Survive',learning:f(['ANY_COMBAT','Connect','Exert','Lead','Notice','Punch','Sneak','Survive']),sourceRef:SRC('Barbarian')}),
});
export const WWN_BACKGROUND_LIST=f(Object.values(WWN_BACKGROUND_CATALOG));
export const WWN_COMBAT_SKILLS=f(['Shoot','Stab','Punch']);
export function backgroundById(id){return WWN_BACKGROUND_CATALOG[String(id??'').toUpperCase()]??null;}
export function legalBackgroundLearningSkills(backgroundId){const bg=backgroundById(backgroundId);if(!bg)return f([]);const out=[];for(const pick of bg.learning){if(pick==='ANY_COMBAT')out.push(...WWN_COMBAT_SKILLS);else out.push(pick);}return f([...new Set(out)]);}
export function buildBackgroundPick({backgroundId,learningPicks}={}){const bg=backgroundById(backgroundId);if(!bg)return f({ok:false,status:'UNKNOWN_BACKGROUND'});if(!Array.isArray(learningPicks)||learningPicks.length!==2)return f({ok:false,status:'BACKGROUND_PICK_REQUIRES_TWO_LEARNING_SKILLS'});const legal=new Set(legalBackgroundLearningSkills(bg.backgroundId));const picks=learningPicks.map(String);if(picks.some(x=>!legal.has(x)))return f({ok:false,status:'BACKGROUND_LEARNING_SKILL_NOT_IN_TABLE',legal:f([...legal])});return f({ok:true,status:'OK',background:f({backgroundId:bg.backgroundId,name:bg.name,freeSkill:bg.freeSkill,mode:'PICK',learningPicks:f(picks),sourceRef:bg.sourceRef}),sourceRefs:f([bg.sourceRef])});}
