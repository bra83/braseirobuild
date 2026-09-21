#!/usr/bin/env python3
from pathlib import Path
import shutil, sys

ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
VTT=ROOT/'app/src/main/assets/vtt'
runtime=VTT/'r46/final-runtime.js'
if not runtime.is_file():
    raise SystemExit('FAIL: final-runtime.js ausente')
src=runtime.read_text(encoding='utf-8')
marker='R54_P27_UNDER_BLACK_SUN'
data_src=Path(__file__).with_name('under_a_black_sun_adventure.pt-BR.json')
if not data_src.is_file():
    raise SystemExit('FAIL: adventure data pack ausente')
dest=VTT/'adventures'/'under-a-black-sun.pt-BR.json'
dest.parent.mkdir(parents=True,exist_ok=True)
shutil.copy2(data_src,dest)

def replace_function(text, signature, replacement):
    start=text.find(signature)
    if start<0: raise SystemExit('FAIL: função não encontrada: '+signature)
    brace=text.find('{',start)
    depth=0; quote=None; esc=False
    for i in range(brace,len(text)):
        ch=text[i]
        if quote is not None:
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch==quote: quote=None
        else:
            if ch in ('"',"'","`"): quote=ch
            elif ch=='{': depth+=1
            elif ch=='}':
                depth-=1
                if depth==0: return text[:start]+replacement+text[i+1:]
    raise SystemExit('FAIL: fechamento da função não encontrado')

if marker not in src:
    helper=r'''async function startBarbaraOpening(){
 // R54_P27_UNDER_BLACK_SUN: published adventure and living sandbox share the canonical Barbara channel.
 const prep=bridgeResult('startLivingAdventure',{});if(!prep?.ok)return prep;
 const game=globalThis.BraseiroGameBridge;if(!game||typeof game.submitPlayerIntent!=='function')return {ok:false,message:'Ponte do Mestre Barbara indisponível.'};
 const req=prep.data?.masterOpeningRequest||{};const location=String(req.locationName||'o ponto inicial');
 const opportunity=String(req.opportunity?.summary||req.opportunity?.title||'').trim();
 const npc=String(req.npc?.name||'').trim();const faction=String(req.faction?.name||'').trim();
 const facts=[`Local inicial: ${location}.`,opportunity?`Situação ativa: ${opportunity}.`:'',npc?`NPC conhecido/presente: ${npc}.`:'',faction?`Facção relevante: ${faction}.`:''].filter(Boolean).join(' ');
 const adventureKey=String(localStorage.getItem('braseiro.starwars.publishedAdventure.v1')||'living');
 let intentText='';
 if(adventureKey==='under-a-black-sun'){
   const mode=campaignMode();if(mode==='AOR')return {ok:false,message:'Sob um Sol Negro é uma aventura de Edge of the Empire. Use campanha EotE ou MISTA.'};
   window.__BRASEIRO_PUBLISHED_ADVENTURE__={id:'under-a-black-sun',title:'Sob um Sol Negro',mode};
   intentText=`[ABERTURA_BARBARA][AVENTURA_PUBLICADA:under-a-black-sun] Inicie "Sob um Sol Negro" em português do Brasil, preservando as regras de Edge of the Empire já implementadas. Estrutura canônica resumida: Episódio I começa em Coruscant com fuga de uma instalação do Sol Negro, perseguição em speeder e depois contato com Chopper; Episódio II é investigação aberta entre Umbra Club, The Spyder e Zelcomm Tower, com consequências conforme a ordem e as ações; Episódio III leva ao Setor 943, fábrica NovaCORE, perigos industriais e confronto com Kaa’to Leeachos. Não revele segredos futuros, não force escolhas e não substitua testes/dificuldades do sistema. Em modo MISTA, preserve simultaneamente Obrigação, Dever e Contribuição, mas a aventura usa Obrigação como seu vínculo narrativo original. Comece somente pela primeira situação jogável e termine deixando a decisão ao jogador.`;
 }else{
   intentText=`[ABERTURA_BARBARA] Inicie a aventura agora como Mestre Barbara. ${facts} Gere a primeira cena a partir do mundo vivo persistente, sem inventar uma missão obrigatória. Não resuma, não escolha nem aja pelo personagem. Narre a situação em português do Brasil conforme o contrato do Mestre e termine deixando a decisão ao jogador.`;
 }
 try{const result=await game.submitPlayerIntent(intentText);if(result?.ok===false)return result;return result?.ok===true?result:{ok:true,data:result}}catch(error){return {ok:false,message:String(error?.message||error)}}
}'''
    src=replace_function(src,'async function startBarbaraOpening(){',helper)
    anchor="root.prepend(gate);const out=gate.querySelector('output');"
    if anchor not in src:
        raise SystemExit('FAIL: anchor do gate de aventura não encontrado')
    insert=anchor+r'''if(character){
   const wrap=document.createElement('label');wrap.className='final-field final-published-adventure-picker';wrap.innerHTML='<span>Modo de aventura</span>';
   const sel=document.createElement('select');sel.setAttribute('data-adventure-choice','');
   sel.innerHTML='<option value="living">Mundo Vivo · sandbox</option><option value="under-a-black-sun">Sob um Sol Negro · EotE</option>';
   const cm=campaignMode(),published=sel.querySelector('option[value="under-a-black-sun"]');if(cm==='AOR')published.disabled=true;
   const saved=String(localStorage.getItem('braseiro.starwars.publishedAdventure.v1')||'living');sel.value=(saved==='under-a-black-sun'&&cm!=='AOR')?saved:'living';
   localStorage.setItem('braseiro.starwars.publishedAdventure.v1',sel.value);sel.addEventListener('change',()=>localStorage.setItem('braseiro.starwars.publishedAdventure.v1',sel.value));
   wrap.appendChild(sel);const startButton=gate.querySelector('[data-start-adventure]');gate.insertBefore(wrap,startButton);
  }'''
    src=src.replace(anchor,insert,1)

if marker not in src or 'under-a-black-sun' not in src or 'Sob um Sol Negro' not in src:
    raise SystemExit('FAIL: integração da aventura não foi persistida no runtime')
runtime.write_text(src,encoding='utf-8')
print('R54_P27_UNDER_BLACK_SUN_PATCHED',dest)
