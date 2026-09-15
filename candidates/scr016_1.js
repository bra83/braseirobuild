import { createWave3Runtime } from './core/wave3-runtime.js';
import { installSharedBottomNav } from './core/shared-navigation.js';
import { projectScr016Binding } from './core/release-screen-bindings.js';
const fallback=await fetch('./data/wave4.iria.fixture.json').then(r=>r.json());
const sceneRules=await fetch('./data/scr015.rules.json').then(r=>r.json());
const wave3Character=await fetch('./data/wave3.core-character.fixture.json').then(r=>r.json());
const runtime=createWave3Runtime({sceneRules,character:wave3Character});
const presentation=await fetch('./data/scr016.iria.presentation.json').then(r=>r.json());
const geometry=await fetch('./data/scr016.recovered.geometry.json').then(r=>r.json());
const $=id=>document.getElementById(id);
const state=window.BRASEIRO_CANONICAL_STATE||{characterState:fallback,encounterState:{screenId:'SCR-015',lastResolution:null}};
const binding=projectScr016Binding(state);
const c=state.characterState||fallback;
$('charName').textContent=c.name||'IRIA';
$('charClass').textContent=`${presentation.classDisplay} • NÍVEL ${c.level??presentation.level}`;
$('attackTab').title=binding.attackEnabled?'':'MISSING_EVIDENCE_INVENTORY';
function projectResolution(result){
  const r=result?.mechanicalEvent?.resolution??result?.resolution??null;
  if(!r){$('rollState').textContent='DELEGAÇÃO CONCLUÍDA';$('resultTitle').textContent='SEM RESULTADO PROJETÁVEL';$('resultDetail').textContent='O adaptador não retornou uma resolução mecânica para exibição. A interface não a fabricou.';return;}
  const diceRaw=Array.isArray(r.dice)?r.dice:Array.isArray(r.rolls)?r.rolls:[]; const dice=diceRaw.map(d=>typeof d==='object'&&d!==null?d.result:d);
  const spans=[...document.querySelectorAll('.dice span')];spans.forEach((s,i)=>s.textContent=dice[i]??'—');
  $('rollState').textContent='RESOLVIDO PELO MOTOR DE REGRAS';
  $('resultTitle').textContent=r.success===true?'SUCESSO':r.success===false?'FALHA':'RESULTADO MECÂNICO';
  $('resultDetail').textContent=typeof r.total==='number'?`Total canônico: ${r.total}.`: 'Resultado recebido sem total numérico projetável.';
  const bits=[];if(dice.length)bits.push(`Dados: ${dice.join(' + ')}`);if(r.attributeModifier!==undefined)bits.push(`Atributo: ${r.attributeModifier}`);if(r.skillLevel!==undefined)bits.push(`Perícia: ${r.skillLevel}`);if(r.situationalModifier!==undefined)bits.push(`Situação: ${r.situationalModifier}`);$('resultBreakdown').textContent=bits.join('\n');
}
$('rollBtn').addEventListener('click',async()=>{
  const adapter=window.BRASEIRO_PLAYER_ACTION??runtime.actionService;
  if(!adapter||typeof adapter.perform!=='function'){$('rollState').textContent='MOTOR DE REGRAS INDISPONÍVEL';$('resultTitle').textContent='NENHUMA ROLAGEM EXECUTADA';$('resultDetail').textContent='O PLAYER_ACTION não está disponível neste host. Nenhum resultado foi gerado localmente.';return;}
  $('rollBtn').disabled=true;$('rollState').textContent='AGUARDANDO MOTOR DE REGRAS…';
  try{const result=await adapter.perform({campaignId:runtime.campaignId,screenId:'SCR-015',intent:'observe'});projectResolution(result)}catch(e){$('rollState').textContent='FALHA DE DELEGAÇÃO';$('resultTitle').textContent='SEM MUTAÇÃO LOCAL';$('resultDetail').textContent='O Motor de Regras não concluiu a chamada; a interface não repetiu nem substituiu a resolução.'}finally{$('rollBtn').disabled=false}
});
window.__SCR016_RECOVERY={binding,presentation,geometry,projectResolution};

installSharedBottomNav({root:document.getElementById('scr016'),active:'mapa',zIndex:900});window.__SCR016_RUNTIME=runtime;
