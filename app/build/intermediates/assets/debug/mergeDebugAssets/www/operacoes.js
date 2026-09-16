import {NativePersistencePort} from './core/persistence-port.js';
import {resolveActiveCampaignId} from './core/runtime-campaign.js';
import {RuleRuntimeService} from './core/rule-runtime-service.js';
import {FactionAssetEffectService} from './core/faction-asset-effect-service.js';
import {CampaignMaintenanceService} from './core/campaign-maintenance-service.js';
import {createPhysicalDiceClient} from './core/physical-dice-client.js';
import {fallingDiceCount} from './core/wwn-hazard-core.js';
import {gmHelpReadOnly} from './core/gm-help-service.js';
import {CampaignMemoryService} from './core/campaign-memory-service.js';
import {NpcWorldService} from './core/npc-world-service.js';
import {projectLivingWorldForMaster} from './core/living-world-core.js';
import {seaTravelPlan,seaEncounterCheckPlan,packLoadPlan,privationDailyPlan,hirelingCost,serviceCost,lifestyleEffects,HIRELING_DAILY_SP,FIXED_SERVICE_SP} from './core/wwn-expedition-services-core.js';
const $=id=>document.getElementById(id);let persistence,campaignId,rules,assets,maintenance,dice,memory,npcWorld,state;
function say(x,bad=false){$('status').textContent=typeof x==='string'?x:JSON.stringify(x,null,2);$('status').style.color=bad?'#e28b8b':'#d6b96d'}
function load(){const l=persistence.loadCampaign(campaignId,{fresh:true});if(l.status!=='OK')throw new Error(l.status);state=JSON.parse(l.stateJson);$('summary').textContent=`${state.displayName??campaignId} • revisão ${state.revision} • dados 3D ${dice.available()?'ATIVOS':'INDISPONÍVEIS'}`;const pcs=Object.values(state.charactersState??{}),ps=$('hazardPc');ps.innerHTML='';for(const p of pcs){const o=document.createElement('option');o.value=p.characterId;o.textContent=p.name??p.characterId;ps.appendChild(o)}const fs=$('factionId');fs.innerHTML='';for(const [id,f] of Object.entries(state.factionState??{})){const o=document.createElement('option');o.value=id;o.textContent=f.name??id;fs.appendChild(o)}renderLivingWorld();return state}
async function rolled(formula){const r=await dice.roll({formula,material:'bone'});if(r.status!=='OK')throw new Error(r.reason??r.status);return {rolls:r.rolls,forcedSettle:r.forcedSettle===true}}
async function run(fn){try{const r=await fn();say(r,!String(r?.status??'OK').startsWith('OK')&&!['CAUGHT','ESCAPED'].includes(r?.status));load()}catch(e){say(e.message??String(e),true)}}
for(const b of document.querySelectorAll('[data-tab]'))b.onclick=()=>{for(const x of document.querySelectorAll('[data-tab]'))x.classList.toggle('active',x===b);for(const p of document.querySelectorAll('[data-panel]'))p.hidden=p.dataset.panel!==b.dataset.tab};
$('reaction').onclick=()=>run(async()=>rules.reaction({campaignId,charismaScore:Number($('reactionCha').value),physicalDice:await rolled('2d6')}));
$('morale').onclick=()=>run(async()=>rules.morale({campaignId,npcId:$('npcId').value.trim(),moraleScore:Number($('moraleScore').value),physicalDice:await rolled('2d6')}));
$('instinct').onclick=()=>run(async()=>rules.instinct({campaignId,npcId:$('npcId').value.trim(),instinctScore:Number($('instinctScore').value),physicalDice:Number($('instinctScore').value)===0?null:await rolled('1d10')}));
$('footChase').onclick=()=>run(()=>rules.footChase({campaignId,chaseId:$('footId').value.trim(),leaderCheckTotal:Number($('leaderTotal').value),helperOutcomes:[],pursuerCheckTotal:Number($('pursuerTotal').value),pursuerCount:Number($('pursuerCount').value),pursuedCount:Number($('pursuedCount').value),headStart:$('headStart').value}));
$('mountedChase').onclick=()=>run(()=>rules.mountedChase({campaignId,chaseId:`${$('footId').value.trim()}-montada`,fleeingPace:Number($('fleeingPace').value),pursuers:$('mountedPursuers').value.split('\n').map(x=>x.trim()).filter(Boolean).map(line=>{const [pursuerId,total,modifier]=line.split(':');return{pursuerId,checkTotal:Number(total),modifier:Number(modifier||0),mounted:true}})}));
$('falling').onclick=()=>run(async()=>{const feet=Number($('fallFeet').value),controlledDescentSuccess=$('controlled').checked,hazardousBottom=$('hazardousBottom').checked,plan=fallingDiceCount({feet,controlledDescentSuccess,hazardousBottom});return rules.falling({campaignId,characterId:$('hazardPc').value,feet,controlledDescentSuccess,hazardousBottom,physicalDice:await rolled(`${plan.totalDice}d6`)})});
$('assetAuto').onclick=()=>run(()=>assets.turnStartAutomatic({campaignId,factionId:$('factionId').value,turnKey:$('turnKey').value.trim()}));
$('farmers').onclick=()=>run(async()=>assets.farmers({campaignId,factionId:$('factionId').value,instanceId:$('assetInstance').value.trim(),turnKey:$('turnKey').value.trim(),physicalDice:await rolled('1d6')}));
$('manufactory').onclick=()=>run(async()=>assets.manufactory({campaignId,factionId:$('factionId').value,instanceId:$('assetInstance').value.trim(),turnKey:$('turnKey').value.trim(),physicalDice:await rolled('1d6')}));
$('workshop').onclick=()=>run(async()=>assets.ancientWorkshop({campaignId,factionId:$('factionId').value,instanceId:$('assetInstance').value.trim(),turnKey:$('turnKey').value.trim(),physicalDice:await rolled('2d6')}));
$('tradeCompany').onclick=()=>run(async()=>{const a=await rolled('1d4'),b=await rolled('1d6');return assets.tradeCompany({campaignId,factionId:$('factionId').value,instanceId:$('assetInstance').value.trim(),turnKey:$('turnKey').value.trim(),physicalDice:{rolls:[a.rolls[0],b.rolls[0]]}})});
$('selfStealth').onclick=()=>run(()=>assets.selfStealth({campaignId,factionId:$('factionId').value,instanceId:$('assetInstance').value.trim(),turnKey:$('turnKey').value.trim()}));

function renderLivingWorld(){if(!state||!$('livingNpc'))return;const living=projectLivingWorldForMaster(state);const lw=living.livingWorld??{};const priv=lw.privateState??{};$('livingPulse').textContent=living.status==='OK'?`Dia ${lw.day??0} • ${lw.dayPart??'—'} • ${lw.weather?.label??'—'}\nPulso ${lw.beat??0} • gatilho ${lw.lastTrigger??'—'}\nNPC em foco: ${priv.spotlightNpcId??'—'} • Facção: ${priv.spotlightFactionId??'—'}\nPressão: ${typeof priv.pressureEncounter==='string'?priv.pressureEncounter:JSON.stringify(priv.pressureEncounter??'—')}`:'Mundo vivo ainda não inicializado.';const sel=$('livingNpc'),prior=sel.value;sel.innerHTML='';for(const n of Object.values(state.npcState??{})){const o=document.createElement('option');o.value=n.npcId;o.textContent=`${n.name??n.npcId} • ${typeof n.location==='string'?n.location:JSON.stringify(n.location??'sem local')}`;sel.appendChild(o)}if([...sel.options].some(o=>o.value===prior))sel.value=prior}
$('livingRefresh').onclick=()=>{load();say({status:'OK',livingWorld:projectLivingWorldForMaster(state)})};
$('livingNpcMove').onclick=()=>run(()=>npcWorld.move({campaignId,npcId:$('livingNpc').value,location:$('livingNpcLocation').value.trim()}));
$('livingNpcRemember').onclick=()=>run(()=>npcWorld.remember({campaignId,npcId:$('livingNpc').value,summary:$('livingNpcMemory').value.trim(),visibility:$('livingNpcMemoryPublic').checked?'PLAYER_KNOWN':'GM_ONLY'}));
$('livingNpcPlayerView').onclick=()=>run(()=>{const r=npcWorld.list({campaignId,visibility:'PLAYER'});const npc=(r.npcs??[]).find(n=>n.npcId===$('livingNpc').value)??null;$('livingNpcOutput').textContent=JSON.stringify(npc,null,2);return{status:r.status,npc}});

function fillSelect(id,values){const e=$(id);e.innerHTML='';for(const v of values){const o=document.createElement('option');o.value=v;o.textContent=v.replaceAll('_',' ');e.appendChild(o)}}
fillSelect('hireKind',Object.keys(HIRELING_DAILY_SP));fillSelect('serviceKind',[...Object.keys(FIXED_SERVICE_SP),'MAGICAL_HEALING_WOUNDS']);
$('seaPlan').onclick=()=>run(()=>({status:'OK',plan:seaTravelPlan({propulsion:$('seaPropulsion').value,hours:Number($('seaHours').value)})}));
$('seaEncounter').onclick=()=>run(async()=>{const plan=seaEncounterCheckPlan({dieSides:Number($('seaDie').value),period:$('seaPeriod').value}),r=await rolled(plan.formula);return{status:'OK',plan,roll:r.rolls[0],encounter:r.rolls[0]===1}});
$('packPlan').onclick=()=>run(()=>({status:'OK',plan:packLoadPlan({carrier:$('packCarrier').value,loadEncumbrance:Number($('packLoad').value)})}));
$('privation').onclick=()=>run(()=>{const pc=state.characterState??{},ss=pc.systemStrain??{};return{status:'OK',plan:privationDailyPlan({consecutiveFoodDays:Number($('foodDays').value),consecutiveWaterDays:Number($('waterDays').value),consecutiveUnshelteredNights:Number($('shelterNights').value),currentSystemStrain:Number(ss.current??0),maxSystemStrain:Number.isInteger(ss.max)?ss.max:null})}});
$('hireCost').onclick=()=>run(()=>({status:'OK',plan:hirelingCost({kind:$('hireKind').value,days:Number($('hireDays').value)})}));
$('serviceCost').onclick=()=>run(()=>({status:'OK',plan:serviceCost({kind:$('serviceKind').value,hitPoints:1})}));
$('lifestyleEffects').onclick=()=>run(()=>({status:'OK',plan:lifestyleEffects({tier:$('lifestyle').value})}));
$('appendFact').onclick=()=>run(()=>memory.appendFact({campaignId,factId:$('factId').value.trim(),summary:$('factSummary').value.trim(),threadId:$('threadId').value.trim()||null,visibility:$('factPublic').checked?'PLAYER_KNOWN':'GM_ONLY'}));
$('revealFact').onclick=()=>run(()=>memory.revealFact({campaignId,factId:$('revealFactId').value.trim()}));
$('scheduleEvent').onclick=()=>run(()=>memory.schedule({campaignId,eventId:$('scheduleId').value.trim(),dueMinute:Number($('scheduleMinute').value),summary:$('scheduleSummary').value.trim(),visibility:'GM_ONLY'}));
$('runSchedule').onclick=()=>run(()=>memory.processDue({campaignId,throughMinute:Number($('throughMinute').value)}));
$('projectMemory').onclick=()=>run(()=>memory.project({campaignId,gm:false}));

$('audit').onclick=()=>run(()=>maintenance.audit({campaignId}));$('repair').onclick=()=>run(()=>maintenance.repair({campaignId}));$('gmHelp').onclick=()=>run(()=>gmHelpReadOnly({state:load(),question:$('gmQuestion').value}));
try{persistence=new NativePersistencePort();campaignId=resolveActiveCampaignId(persistence);if(!campaignId)throw new Error('Nenhuma campanha ativa');rules=new RuleRuntimeService({persistence});assets=new FactionAssetEffectService({persistence});maintenance=new CampaignMaintenanceService({persistence});memory=new CampaignMemoryService({persistence});npcWorld=new NpcWorldService({persistence});dice=createPhysicalDiceClient();load()}catch(e){say(e.message??String(e),true)}
