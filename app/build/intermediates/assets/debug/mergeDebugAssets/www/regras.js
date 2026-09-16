import { RagClient } from './core/rag-client.js';
import { resolveAidingSkillCheck, combineAidBonuses } from './core/wwn-rules-core.js';
import { resolveMoraleCheck, resolveInstinctCheck, instinctFailureRate } from './core/wwn-encounter-core.js';
import { fallingDiceCount, controlledDescentDifficulty, resolveFallingDamage, suffocationGraceRounds, suffocationFailureDamage, typicalDungeonPoisonDamage } from './core/wwn-hazard-core.js';
import { calculateMajorProjectDifficulty, renownPurchaseCost, applyFactionProjectAid, projectRenownReadiness } from './core/wwn-major-project-core.js';
import { canModifyGear, craftingPlan, modInstallationDays, modMaintenanceCapacity, capStackedModBonus } from './core/wwn-crafting-core.js';
import { resolveIndividualInitiative, orderIndividualInitiative, surpriseRoundPlan, automaticInitiativePriority, snapAttackPlan, swarmAttackPlan, screenCapacity, resolveScreenContest, proneCombatModifiers, attackSituationModifier, holdActionPlan, shoveOutcome, grappleStartOutcome, grappleEscapeOutcome, grappleMoveOutcome, sizePenaltyForShoveOrGrapple, dualWieldPlan, rangedExecutionDifficulty, executionPhysicalSavePlan, resolveExecutionAfterSave } from './core/wwn-special-combat-core.js';
import { footFleeingPace, footPursuitModifier, resolveFootChase, mountedPursuitModifier, resolveMountedPursuer, resolveMountedPursuitGroup } from './core/wwn-chase-core.js';
const $=id=>document.getElementById(id);const client=new RagClient();
function toast(text){$('toast').textContent=text;$('toast').classList.remove('hidden');setTimeout(()=>$('toast').classList.add('hidden'),2400)}
function renderDocs(){const r=client.list(),box=$('documents');box.replaceChildren();const docs=r.status==='OK'?(r.documents??[]):[];$('count').textContent=String(docs.length);if(!docs.length){const e=document.createElement('div');e.className='empty';e.textContent=globalThis.BraseiroNative?'Nenhum PDF indexado.':'Importação PDF disponível no Android.';box.appendChild(e);return}for(const d of docs){const row=document.createElement('article');row.className='doc';const b=document.createElement('b');b.textContent=d.title;const s=document.createElement('span');const label=d.authorityProfile==='WWN_RULEBOOK_AUTHORIZED'?'MANUAL WWN':d.authorityProfile==='GM_PRIVATE_REFERENCE'?'PRIVADO DO MESTRE':'REFERÊNCIA';s.textContent=`${label} • ${d.pageCount} pág. • ${d.chunkCount} trechos`;const del=document.createElement('button');del.textContent='REMOVER';del.onclick=()=>{if(!confirm(`Remover índice de ${d.title}? O PDF original não é apagado.`))return;const x=client.remove(d.documentId);toast(x.status==='OK'?'Índice removido.':`Falha: ${x.reason??x.status}`);renderDocs()};row.append(b,s,del);box.appendChild(row)}}
function renderResults(r){const box=$('results');box.replaceChildren();if(r.status!=='OK'||!(r.results??[]).length){const e=document.createElement('div');e.className='empty';e.textContent=r.status==='MISSING_EVIDENCE'?'MISSING_EVIDENCE — nada nos manuais WWN autorizados sustenta esta consulta.':`Consulta indisponível: ${r.reason??r.status}`;box.appendChild(e);return}for(const h of r.results){const card=document.createElement('article');card.className='hitcard';const b=document.createElement('b');b.textContent=h.title;const em=document.createElement('em');em.textContent=`p. ${h.page}`;const p=document.createElement('p');p.textContent=h.text;const sm=document.createElement('small');sm.textContent=`${h.authorityProfile} • ${String(h.sourceHash??'').slice(0,12)} • evidência documental, não resolução mecânica`;card.append(b,em,p,sm);box.appendChild(card)}}
$('importRules').onclick=()=>{const r=client.importPdf('WWN_RULEBOOK_AUTHORIZED');if(r.status==='UNAVAILABLE')toast('Importação de PDF requer o app Android.');else if(r.status!=='PENDING')toast(`Falha: ${r.reason??r.status}`)};
$('importReference').onclick=()=>{const r=client.importPdf('CAMPAIGN_REFERENCE');if(r.status==='UNAVAILABLE')toast('Importação de PDF requer o app Android.');else if(r.status!=='PENDING')toast(`Falha: ${r.reason??r.status}`)};
$('searchButton').onclick=async()=>{const q=$('query').value.trim();if(!q)return toast('Digite o que deseja localizar.');const btn=$('searchButton');btn.disabled=true;$('searchStatus').textContent='Buscando nos manuais sem bloquear a interface…';try{const r=await client.searchAsync(q,{limit:6,scope:'RULES',timeoutMs:45000});$('searchStatus').textContent=r.status==='OK'?`${r.results.length} evidência(s) com página e origem. O Rules Engine não é alterado por esta busca.`:'Nenhuma regra será inferida quando a evidência faltar.';renderResults(r)}catch(err){const r={status:'UNAVAILABLE',reason:err?.message??'SEARCH_FAILED'};$('searchStatus').textContent='A busca não concluiu. A interface continua disponível.';renderResults(r)}finally{btn.disabled=false}};
$('query').addEventListener('keydown',e=>{if(e.key==='Enter')$('searchButton').click()});
window.addEventListener('braseiro-native-file',e=>{if(e.detail?.kind!=='RULES_PDF_IMPORT')return;let detail=null;try{detail=e.detail.text?JSON.parse(e.detail.text):e.detail}catch{detail=e.detail}const status=detail?.status??e.detail?.status;if(status==='PROCESSING'){const page=Number(detail?.page??0),total=Number(detail?.totalPages??0);const label=page>0&&total>0?`Indexando PDF… página ${page}/${total}`:'Abrindo e validando PDF…';$('searchStatus').textContent=label;return;}if(status==='OK'){toast(`PDF indexado: ${detail.title??''}`);$('searchStatus').textContent='PDF pronto para consulta local com proveniência.';}else if(status==='ALREADY_INDEXED'){toast('PDF já estava indexado.');$('searchStatus').textContent='Este PDF já está disponível para consulta.';}else if(status==='CANCELLED'){$('searchStatus').textContent='Importação cancelada.';}else{toast(`PDF não indexado: ${detail?.reason??status??'erro'}`);$('searchStatus').textContent=`Falha ao indexar PDF: ${detail?.reason??status??'erro'}`;}renderDocs()});
renderDocs();window.__BRASEIRO_RULES_RAG__={client,renderDocs};

if(location.hash==='#query')requestAnimationFrame(()=>document.getElementById('query')?.focus());


// Closure 070: read-only, source-bound rules parity API. This does not mutate campaign state
// and intentionally adds no UI controls; game services may consume these contracts explicitly.
window.__BRASEIRO_WWN_RULE_PARITY_070__ = Object.freeze({
  resolveAidingSkillCheck, combineAidBonuses,
  resolveMoraleCheck, resolveInstinctCheck, instinctFailureRate,
  fallingDiceCount, controlledDescentDifficulty, resolveFallingDamage,
  suffocationGraceRounds, suffocationFailureDamage, typicalDungeonPoisonDamage,
  calculateMajorProjectDifficulty, renownPurchaseCost, applyFactionProjectAid, projectRenownReadiness,
  canModifyGear, craftingPlan, modInstallationDays, modMaintenanceCapacity, capStackedModBonus,
  resolveIndividualInitiative, orderIndividualInitiative, surpriseRoundPlan, automaticInitiativePriority,
  snapAttackPlan, swarmAttackPlan, screenCapacity, resolveScreenContest, proneCombatModifiers, attackSituationModifier, holdActionPlan,
  shoveOutcome, grappleStartOutcome, grappleEscapeOutcome, grappleMoveOutcome, sizePenaltyForShoveOrGrapple, dualWieldPlan,
  rangedExecutionDifficulty, executionPhysicalSavePlan, resolveExecutionAfterSave,
  footFleeingPace, footPursuitModifier, resolveFootChase, mountedPursuitModifier, resolveMountedPursuer, resolveMountedPursuitGroup,
});
