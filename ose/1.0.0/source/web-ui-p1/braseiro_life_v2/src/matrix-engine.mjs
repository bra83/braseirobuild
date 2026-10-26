/** Hidden, deterministic, five-axis matrix with bounded draws and coherence checks. */
const assert=(x,m)=>{if(!x)throw Error(`BRASEIRO_MATRIX: ${m}`);};
const clone=x=>structuredClone(x);
const key=x=>typeof x==='string'&&/^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,159}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const AXES=['cause','manifestation','domain','timing','response'];
const aliases={'ose':'ose-classic','ose-fantasy':'ose-classic','dnd5e-2024':'dnd5e','oua':'o-um-anel','tor2e':'o-um-anel','pf2e':'pf2e-kingmaker','gurps-investigacao':'gurps','gurps-csi':'gurps','gurps-serial-killer':'gurps','star-wars-aor':'star-wars','star-wars-eote':'star-wars'};
const profiles={
 'gurps-investigacao':['gurps','investigacao'], 'gurps-csi':['gurps','csi'], 'gurps-serial-killer':['gurps','serial-killer'],
 'o-um-anel':['o-um-anel','default'],'dnd5e':['dnd5e','default'],'forbidden-lands':['forbidden-lands','default'],'pf2e-kingmaker':['pf2e-kingmaker','default'],
 'star-wars-aor':['star-wars','aor'],'star-wars-eote':['star-wars','eote'],
 'ose-classic':['ose-classic','classic'],'ose-advanced':['ose-advanced','advanced']
};
export function selectMatrixProfile(packId,system){
 const profile=profiles[packId];assert(profile&&(aliases[system]??system)===profile[0],'perfil pertence a outro sistema');
 return {packId,system:profile[0],mode:profile[1]};
}
export function validateMatrixBundle(bundle){
 assert(bundle?.schema==='braseiro.matrix-bundle/v1'&&key(bundle.pack?.id)&&typeof bundle.pack.version==='string','pacote inválido');
 assert(key(bundle.pack.system)&&key(bundle.pack.mode),'sistema/modalidade inválidos');
 assert(bundle.tables&&AXES.every(a=>Array.isArray(bundle.tables[a])&&bundle.tables[a].length>0&&bundle.tables[a].length<=10000),'tabelas incompletas');
 for(const axis of AXES){const seen=new Set();for(const row of bundle.tables[axis]){
  assert(key(row.id)&&!seen.has(row.id),'ID de entrada inválido/duplicado');seen.add(row.id);
  assert(typeof row.text==='string'&&row.text.length>0&&row.text.length<=4000,'texto inválido');
  assert(['verified_origin','context_only','narrative_opportunity','schedule_only','player_choice'].includes(row.agency),'agência inválida');
  assert(row.weight===undefined||(Number.isInteger(row.weight)&&row.weight>=1&&row.weight<=20),'peso inválido');
  assert((row.requiredCapabilities??[]).every(key),'capacidade inválida');
  if(row.minElapsedDays!==undefined)assert(Number.isSafeInteger(row.minElapsedDays)&&row.minElapsedDays>=0&&Number.isSafeInteger(row.maxElapsedDays)&&row.maxElapsedDays>=row.minElapsedDays,'janela inválida');
 }}
 return bundle;
}
function stageMatches(row,ctx){const elapsed=ctx.day-ctx.origin.day;return (row.minElapsedDays===undefined||(elapsed>=row.minElapsedDays&&elapsed<=row.maxElapsedDays))&&(!row.allowedStages||row.allowedStages.includes(ctx.stage));}
function reason(row,ctx,selection,axis){
 if(!stageMatches(row,ctx))return 'outside_time_window';
 if((row.requiredCapabilities??[]).some(x=>!ctx.capabilities.includes(x)))return 'missing_capability';
 if(row.setting&&row.setting!==ctx.packId)return 'wrong_setting';
 if(row.agency==='mechanical'||row.diagnosis||row.automaticVoluntaryAction)return 'forbidden_authority';
 if(row.intensity!==undefined&&row.intensity>ctx.maxIntensity)return 'intensity_limit';
 if(row.domain&&ctx.characterProfile?.domainWeights?.[row.domain]===0)return 'personal_domain_disabled';
 if(row.causeTags?.some(tag=>ctx.characterProfile?.causeWeights?.[tag]===0))return 'personal_cause_disabled';
 if(row.domain&&ctx.allowedDomains?.length&&!ctx.allowedDomains.includes(row.domain))return 'domain_limit';
 for(const [k,v] of Object.entries(row.requiresFacts??{}))if(!Object.hasOwn(ctx.facts,k)||!equal(ctx.facts[k],v))return 'required_canon_missing';
 for(const [k,v] of Object.entries(row.forbidsFacts??{}))if(Object.hasOwn(ctx.facts,k)&&equal(ctx.facts[k],v))return 'contradicts_canon';
 // A proposed claim is allowed only if canon already supports it. Nothing rewrites history.
 for(const [k,v] of Object.entries(row.assertedFacts??{}))if(!Object.hasOwn(ctx.facts,k)||!equal(ctx.facts[k],v))return 'unsupported_claim';
 if(axis==='cause'&&row.originCode!==ctx.origin.causeId)return 'different_origin';
 if(row.causeTags&&selection.cause&&!row.causeTags.some(t=>selection.cause.causeTags.includes(t)))return 'incompatible_cause';
 if(row.domain&&selection.manifestation&&row.domain!==selection.manifestation.domain)return 'incompatible_domain';
 if(axis==='manifestation'){
  const domainCaps={family:'family_contact',intimacy:'intimate_contact',home:'home_exists',npc:'related_person'};
  if(domainCaps[row.domain]&&!ctx.capabilities.includes(domainCaps[row.domain]))return 'missing_relationship_or_home';
  const matches=ctx.recent.filter(x=>x.packId===ctx.packId&&x.situationId===(row.baseSituationId??row.baseId??row.id)&&(!x.characterId&&!x.result?.characterId|| (x.characterId??x.result?.characterId)===ctx.characterId));
  const used=matches.reduce((latest,x)=>!latest||x.day>latest.day?x:latest,null);
  const arc=ctx.arcs?.find(a=>a.packId===ctx.packId&&a.characterId===ctx.characterId&&a.originEventId===ctx.origin.id&&a.situationId===(row.baseSituationId??row.baseId??row.id));
  if(arc&&['resolved','rejected'].includes(arc.status))return 'arc_closed';
  if(arc&&!ctx.revalidating&&['active','resurfaced','dormant','resolving'].includes(arc.status)&&arc.nextDueDay>ctx.day)return 'arc_not_due';
  if(used&&ctx.day-used.day<(row.cooldownDays??7))return 'repetition_cooldown';
 }
 return null;
}
export function checkMatrixCoherence(bundle,selection,context){
 const ctx=normalizeContext(bundle,context), reasons=[];
 for(const axis of AXES){const row=selection[axis];if(!row){reasons.push({axis,reason:'missing_axis'});continue;}if(!bundle.tables[axis].some(x=>equal(x,row))){reasons.push({axis,reason:'foreign_row'});continue;}const why=reason(row,ctx,selection,axis);if(why)reasons.push({axis,reason:why});}
 return {valid:reasons.length===0,reasons};
}
function normalizeContext(bundle,c){
 assert(Number.isSafeInteger(c?.day)&&Number.isSafeInteger(c.origin?.day)&&c.day>=c.origin.day,'tempo/origem inválidos');
 assert(key(c.origin.id)&&key(c.origin.causeId)&&key(c.characterId),'identidade/origem inválida');
 assert((c.origin.actorIds??[]).includes(c.characterId)||(c.origin.witnessIds??[]).includes(c.characterId),'personagem sem vínculo com origem');
 assert((aliases[c.system]??c.system)===bundle.pack.system&&c.mode===bundle.pack.mode,'sistema ou modalidade incompatível');
 assert(Number.isInteger(c.maxIntensity??2)&&(c.maxIntensity??2)>=0&&(c.maxIntensity??2)<=5,'intensidade inválida');
 const elapsed=c.day-c.origin.day;
 const stage=elapsed<=1?'immediate':elapsed<=7?'settling':elapsed<=30?'established':elapsed<=365?'dormant':'long_term';
 return {...c,stage,packId:bundle.pack.id,facts:c.facts??{},capabilities:c.capabilities??[],recent:c.recent??[],maxIntensity:c.maxIntensity??2};
}
/** Two independent d100, interpreted as a base-100 number, never summed. */
export function candidateWeight(row,context){
 const p=context.characterProfile??{};let value=(row.weight??1)*(context.characterProfile?4:1);
 if(row.domain)value*=(p.domainWeights?.[row.domain]??1)*(p.decisionWeights?.[row.domain]??1);
 if(['work','legacy','belief'].includes(row.domain)&&row.causeTags?.length){const experience=Math.max(...row.causeTags.map(t=>p.experience?.[t]??0));value*=1+Math.min(0.5,experience/10);}
 if(row.causeTags?.length)value*=Math.max(...row.causeTags.map(t=>p.causeWeights?.[t]??1));
 const protection=Math.min(5,Object.values(p.protections??{}).reduce((a,b)=>a+b,0));
 if(row.valence==='positive')value*=1+protection/5;
 if(row.valence==='challenging')value*=1-protection/10;
 return value<=0?0:Math.max(1,Math.min(20,Math.round(value)));
}
function drawWeighted(rows,state,axis,audit,context){
 const weights=rows.map(r=>candidateWeight(r,context)),total=weights.reduce((a,b)=>a+b,0);assert(total<=10000,'peso total excede 10.000');
 function d100(){state.value=(Math.imul(1664525,state.value)+1013904223)>>>0;return Math.floor(state.value/4294967296*100)+1;}
 for(let attempt=0;attempt<16;attempt++){
  const dice=[d100(),d100()],raw=(dice[0]-1)*100+dice[1]-1,cutoff=Math.floor(10000/total)*total;
  audit.push({axis,dice,raw,accepted:raw<cutoff,seedState:state.value,totalEffectiveWeight:total});
  if(raw>=cutoff)continue;
  let index=raw%total;for(let i=0;i<rows.length;i++){if(index<weights[i])return rows[i];index-=weights[i];}
 }
 return null;
}
/** All output is a proposal. Caller commits neither new crimes nor diagnoses from this draw. */
export function sampleCrossedMatrix(bundle,context,seed=1){
 validateMatrixBundle(bundle);const ctx=normalizeContext(bundle,context),selection={},draws=[],filters=[];const state={value:seed>>>0};
 if(ctx.characterStatus==='dead'||ctx.facts[`dead-${ctx.characterId}`]===true)return {status:'none',reason:'character_dead',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
 const chance=ctx.occurrenceChance??100;
 assert(Number.isInteger(chance)&&chance>=0&&chance<=100,'chance de ocorrência inválida');
 if(chance<100){
  state.value=(Math.imul(1664525,state.value)+1013904223)>>>0;const value=Math.floor(state.value/4294967296*100)+1;
  draws.push({axis:'occurrence',dice:[value],accepted:value<=chance,seedState:state.value,chance});
  if(value>chance)return {status:'none',reason:'ordinary_life',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
 }
 for(const axis of AXES){
  const reasons={},eligible=[];
  for(const row of bundle.tables[axis]){const why=reason(row,ctx,selection,axis);if(why)reasons[why]=(reasons[why]??0)+1;else eligible.push(row);}
  filters.push({axis,total:bundle.tables[axis].length,eligible:eligible.length,rejected:reasons});
  if(!eligible.length)return {status:'none',reason:'no_coherent_crossing',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
  const row=drawWeighted(eligible,state,axis,draws,ctx);if(!row)return {status:'none',reason:'draw_limit',packId:bundle.pack.id,seedBefore:seed>>>0,seedAfter:state.value,draws,filters};
  selection[axis]=clone(row);
 }
 const check=checkMatrixCoherence(bundle,selection,ctx);
 if(!check.valid)return {status:'none',reason:'coherence_rejected',check,draws,filters,seedAfter:state.value};
 return {status:'proposal',packId:bundle.pack.id,version:bundle.pack.version,seedBefore:seed>>>0,seedAfter:state.value,originEventId:ctx.origin.id,characterId:ctx.characterId,day:ctx.day,stage:ctx.stage,situationId:selection.manifestation.baseSituationId??selection.manifestation.baseId??selection.manifestation.id,selection,draws,filters,proposal:{situation:selection.manifestation.text,focus:selection.domain.text,playerOption:selection.response.text,timing:selection.timing.mode,requiresPlayerChoice:true,mechanicalEffect:'none',canonMutation:'none'}};
}
/** Build runtime inputs only from the host's confirmed world state. */
export function matrixContextFromStore(store,{originEventId,characterId,mode,capabilities=[],maxIntensity=2,allowedDomains}={}){
 const origin=store.events.find(e=>e.id===originEventId);
 assert(origin&&origin.lifeCauseId,'evento de origem não classificado pelo host');
 if(store.matrixProfile&&mode!==undefined)assert(mode===store.matrixProfile.mode,'modalidade difere do perfil salvo');
 return {system:store.legacy.system,mode:store.matrixProfile?.mode??mode,day:store.day,characterId,origin:{...origin,causeId:origin.lifeCauseId},facts:Object.fromEntries(Object.entries(store.facts).map(([k,v])=>[k,v.value])),capabilities,maxIntensity,allowedDomains,recent:store.matrixHistory??[],characterProfile:store.characterLifeProfiles?.[characterId],characterStatus:store.legacy.characters[characterId]?.status,arcs:Object.values(store.lifeArcs??[])};
}

/** Recheck a frozen proposal against present facts/time/capabilities before exposure. */
export function revalidateMatrixProposal(result,context){
 if(result?.version==='2.1.0')return {valid:false,reasons:[{reason:'legacy_proposal_requires_redraw'}]};
 if(result?.status!=='proposal')return {valid:false,reasons:[{reason:'not_a_proposal'}]};
 try{
  const profile=selectMatrixProfile(result.packId,context.system);
  const bundle={schema:'braseiro.matrix-bundle/v1',pack:{id:result.packId,version:result.version,system:profile.system,mode:profile.mode},tables:Object.fromEntries(AXES.map(a=>[a,[result.selection[a]]]))};
  if(context.characterStatus==='dead'||context.facts?.[`dead-${context.characterId}`]===true)return {valid:false,reasons:[{reason:'character_dead'}]};
  return checkMatrixCoherence(bundle,result.selection,{...context,revalidating:true});
 }catch(error){return {valid:false,reasons:[{reason:'context_changed',message:error.message}]};}
}
