#!/usr/bin/env python3
from pathlib import Path
import shutil,sys
ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
VTT=ROOT/'app/src/main/assets/vtt'
rt=VTT/'r46/final-runtime.js'; shell=VTT/'r46-shell.html'
engine_src=Path(__file__).with_name('under-a-black-sun-engine.js')
engine_dst=VTT/'adventures/under-a-black-sun-engine.js'
for p in (rt,shell,engine_src):
    if not p.is_file(): raise SystemExit(f'FAIL: ausente {p}')
engine_dst.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(engine_src,engine_dst)

def replace_js_block(text, signature, replacement):
    start=text.find(signature)
    if start<0: raise SystemExit('FAIL: assinatura não encontrada: '+signature)
    brace=text.find('{',start);depth=0;quote=None;esc=False
    for i in range(brace,len(text)):
        ch=text[i]
        if quote:
            if esc:esc=False
            elif ch=='\\':esc=True
            elif ch==quote:quote=None
            continue
        if ch in ("'",'"','`'):quote=ch;continue
        if ch=='{':depth+=1
        elif ch=='}':
            depth-=1
            if depth==0:return text[:start]+replacement+text[i+1:]
    raise SystemExit('FAIL: bloco sem fechamento: '+signature)

html=shell.read_text(encoding='utf-8')
tag='<script src="adventures/under-a-black-sun-engine.js"></script>'
if tag not in html:
    needle='<script src="r46/final-runtime.js"'
    if needle not in html: raise SystemExit('FAIL: tag final-runtime não encontrada')
    html=html.replace(needle,tag+'\n'+needle,1)
    shell.write_text(html,encoding='utf-8')

src=rt.read_text(encoding='utf-8')
marker='R54_P28_BLACK_SUN_ENGINE'
if marker not in src:
    helper=r'''// R54_P28_BLACK_SUN_ENGINE
function blackSunEngine(){return globalThis.BraseiroBlackSunEngine||null}
function blackSunState(){const e=blackSunEngine();return e?e.read(localStorage):null}
function blackSunPromptContext(state){
 const e=blackSunEngine(),ctx=e?.currentContext(state);if(!ctx)return null;
 return {id:ctx.id,episodeTitle:ctx.episodeTitle,title:ctx.title,visible:ctx.visible,gm:ctx.gm,checks:ctx.checks||[],entities:ctx.entities||[],completedNodes:ctx.completedNodes||[],clues:ctx.clues||[]};
}
function renderBlackSunProgress(){
 const selected=String(localStorage.getItem('braseiro.starwars.publishedAdventure.v1')||'living');
 let panel=document.querySelector('.final-black-sun-progress');
 if(selected!=='under-a-black-sun'){if(panel)panel.remove();return}
 const anchor=document.querySelector('.final-scene-gemini');if(!anchor)return;
 const e=blackSunEngine();if(!e)return;const st=e.read(localStorage);if(!st.started)return;
 const info=e.status(st);const sig=JSON.stringify(info);if(panel&&panel.dataset.signature===sig)return;
 if(!panel){panel=document.createElement('section');panel.className='final-black-sun-progress';anchor.insertAdjacentElement('afterend',panel)}
 panel.dataset.signature=sig;panel.style.cssText='margin:10px 0;padding:12px;border:1px solid rgba(160,180,190,.35);border-radius:12px;background:rgba(10,15,20,.92);color:#eee;display:grid;gap:8px';
 const available=(info.availableNodes||[]).map(n=>'<button type="button" data-bs-node="'+n.id+'">'+n.title+'</button>').join('');
 panel.innerHTML='<strong>Sob um Sol Negro</strong><small>'+(info.completed?'Aventura concluída':(info.episodeTitle||''))+'</small>'+
   (info.currentTitle?'<div><b>Etapa atual:</b> '+info.currentTitle+'</div>':'')+
   (available?'<div><b>Próximos caminhos desbloqueados:</b></div><div style="display:grid;gap:6px">'+available+'</div>':'')+
   (!info.completed&&info.currentNode?'<div style="display:flex;gap:6px;flex-wrap:wrap"><button type="button" data-bs-resume>Continuar com Barbara</button><button type="button" data-bs-complete>Marcar etapa concluída</button></div>':'')+
   (info.completed?'<div>Desfecho alcançado. Consequências de Obrigação/Dever continuam sendo resolvidas pelo sistema.</div>':'');
 panel.querySelector('[data-bs-resume]')?.addEventListener('click',()=>void startBarbaraOpening());
 panel.querySelector('[data-bs-complete]')?.addEventListener('click',()=>{try{const next=e.completeCurrent(localStorage);renderBlackSunProgress();if(next.currentNode)void startBarbaraOpening()}catch(err){alert(String(err?.message||err))}});
 panel.querySelectorAll('[data-bs-node]').forEach(b=>b.addEventListener('click',()=>{try{e.select(localStorage,b.dataset.bsNode);renderBlackSunProgress();void startBarbaraOpening()}catch(err){alert(String(err?.message||err))}}));
}'''
    anchor='function currentState()'
    i=src.find(anchor)
    if i<0: raise SystemExit('FAIL: currentState anchor ausente')
    src=src[:i]+helper+'\n'+src[i:]

opening=r'''async function startBarbaraOpening(){
 const adventureKey=String(localStorage.getItem('braseiro.starwars.publishedAdventure.v1')||'living');
 const game=globalThis.BraseiroGameBridge;if(!game||typeof game.submitPlayerIntent!=='function')return {ok:false,message:'Ponte do Mestre Barbara indisponível.'};
 if(adventureKey==='under-a-black-sun'){
   const mode=campaignMode();if(mode==='AOR')return {ok:false,message:'Sob um Sol Negro é uma aventura de Edge of the Empire. Use campanha EotE ou MISTA.'};
   const engine=blackSunEngine();if(!engine)return {ok:false,message:'Motor de Sob um Sol Negro indisponível.'};
   let state=engine.start(localStorage),prep={ok:true,data:{}};
   if(!state.flags?.worldStarted){prep=bridgeResult('startLivingAdventure',{});if(prep&&prep.ok===false)return prep;state.flags={...(state.flags||{}),worldStarted:true};state=engine.write(localStorage,state)}
   else {try{prep=bridgeResult('livingSandboxStatus',{})}catch{prep={ok:true,data:{}}}}
   const ctx=blackSunPromptContext(state);
   if(!ctx){if(state.completed)return {ok:false,message:'Sob um Sol Negro já foi concluída.'};return {ok:false,message:'Nenhuma etapa da aventura está selecionada.'}}
   window.__BRASEIRO_PUBLISHED_ADVENTURE__={id:'under-a-black-sun',title:'Sob um Sol Negro',mode,nodeId:ctx.id,episode:state.episode};
   const checks=ctx.checks.length?'Testes previstos no material para esta etapa: '+ctx.checks.join('; ')+'.':'';
   const entities=ctx.entities.length?'Entidades pertinentes agora: '+ctx.entities.join(', ')+'.':'';
   const intentText=`[ABERTURA_BARBARA][AVENTURA_PUBLICADA:under-a-black-sun][NO:${ctx.id}] Conduza em português do Brasil somente a etapa atual de "Sob um Sol Negro". ${ctx.episodeTitle}. Etapa: ${ctx.title}. Situação visível: ${ctx.visible} Contexto privado do Mestre: ${ctx.gm} ${checks} ${entities} Preserve integralmente as regras de Edge of the Empire já implementadas. Em campanha MISTA, Obrigação e Dever continuam ativos pelo motor, mas esta aventura usa Obrigação como vínculo narrativo de origem. Não revele nós futuros, pistas não conquistadas, segredos de encontros posteriores ou localização final antecipadamente. Não escolha pelo jogador. Quando um teste for necessário, use o mecanismo de teste do sistema e as dificuldades fornecidas; não invente bônus, penalidades ou valores. Termine no próximo ponto real de decisão.`;
   try{const result=await game.submitPlayerIntent(intentText);if(result?.ok===false)return result;return result?.ok===true?result:{ok:true,data:result}}catch(error){return {ok:false,message:String(error?.message||error)}}
 }
 const prep=bridgeResult('startLivingAdventure',{});if(!prep?.ok)return prep;
 const req=prep.data?.masterOpeningRequest||{},location=String(req.locationName||'o ponto inicial'),opportunity=String(req.opportunity?.summary||req.opportunity?.title||'').trim(),npc=String(req.npc?.name||'').trim(),faction=String(req.faction?.name||'').trim();
 const facts=[`Local inicial: ${location}.`,opportunity?`Situação ativa: ${opportunity}.`:'',npc?`NPC conhecido/presente: ${npc}.`:'',faction?`Facção relevante: ${faction}.`:''].filter(Boolean).join(' ');
 const intentText=`[ABERTURA_BARBARA] Inicie a aventura agora como Mestre Barbara. ${facts} Gere a primeira cena a partir do mundo vivo persistente, sem inventar uma missão obrigatória. Não resuma, não escolha nem aja pelo personagem. Narre a situação em português do Brasil conforme o contrato do Mestre e termine deixando a decisão ao jogador.`;
 try{const result=await game.submitPlayerIntent(intentText);if(result?.ok===false)return result;return result?.ok===true?result:{ok:true,data:result}}catch(error){return {ok:false,message:String(error?.message||error)}}
}'''
src=replace_js_block(src,'async function startBarbaraOpening(){',opening)
old='function enhance(){enhanceNarration();enhanceSceneFromEngine();enhanceAccessibility();enhanceTtsSettings();relabelCreationForCampaign();setupOverlay();enhanceAdventureGate();enhanceSceneGeminiButton();enhanceGeminiModels();enhanceCharacterEmptyActions();enhanceUtilityShortcuts()}'
new='function enhance(){enhanceNarration();enhanceSceneFromEngine();enhanceAccessibility();enhanceTtsSettings();relabelCreationForCampaign();setupOverlay();enhanceAdventureGate();enhanceSceneGeminiButton();enhanceGeminiModels();enhanceCharacterEmptyActions();enhanceUtilityShortcuts();renderBlackSunProgress()}'
if old not in src: raise SystemExit('FAIL: assinatura enhance P27 não encontrada')
src=src.replace(old,new,1)
rt.write_text(src,encoding='utf-8')
print('P28_BLACK_SUN_ENGINE_PATCHED',engine_dst,rt,shell)
