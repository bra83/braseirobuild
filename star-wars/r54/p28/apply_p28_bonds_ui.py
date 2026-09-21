#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
rt=ROOT/'app/src/main/assets/vtt/r46/final-runtime.js'
if not rt.is_file(): raise SystemExit('FAIL: final-runtime.js ausente')
src=rt.read_text(encoding='utf-8')
marker='R54_P28_BONDS_UI'
if marker not in src:
    helper=r'''// R54_P28_BONDS_UI
function campaignBondSnapshot(){
 const state=currentState()||{},mode=campaignMode(),character=state.player?.character||{},group=state.group||{},name=String(character.name||'Personagem');
 const uniq=items=>{const m=new Map();for(const raw of items){if(!raw)continue;const e={...raw,member:String(raw.member||name),type:String(raw.type||'').trim(),value:Number(raw.value||0)};const k=[e.member.toLocaleLowerCase('pt-BR'),e.type.toLocaleLowerCase('pt-BR'),e.value].join('|');if(!m.has(k))m.set(k,e)}return [...m.values()]};
 const obligations=uniq([...(Array.isArray(character.obligations)?character.obligations:[]),...(Array.isArray(group.obligationEntries)?group.obligationEntries:[])]);
 const duties=uniq([...(Array.isArray(character.duties)?character.duties:[]),...(Array.isArray(group.dutyEntries)?group.dutyEntries:[])]);
 return {mode,obligations,duties,obligationTotal:obligations.reduce((n,e)=>n+e.value,0),dutyTotal:duties.reduce((n,e)=>n+e.value,0),contributionRank:Number(group.contributionRank||character.groupContributionRank||0)};
}
function renderCampaignBondsPanel(){
 const heading=[...document.querySelectorAll('h1,h2')].find(el=>/grupo\s*\/\s*base/i.test(String(el.textContent||'')));
 let panel=document.querySelector('.final-campaign-bonds');
 if(!heading){if(panel)panel.remove();return}
 const data=campaignBondSnapshot(),showO=data.mode!=='AOR',showD=data.mode!=='EOTE';
 const lines=[];
 if(showO)lines.push('<div><b>Obrigação total:</b> '+data.obligationTotal+'</div>');
 if(showD)lines.push('<div><b>Dever total:</b> '+data.dutyTotal+'</div><div><b>Faixa de Contribuição:</b> '+data.contributionRank+'</div>');
 const entries=[];
 if(showO)for(const e of data.obligations)entries.push('<li>Obrigação · '+(e.type||'Sem tipo')+' · '+e.value+(e.member?' · '+e.member:'')+'</li>');
 if(showD)for(const e of data.duties)entries.push('<li>Dever · '+(e.type||'Sem tipo')+' · '+e.value+(e.member?' · '+e.member:'')+'</li>');
 const html='<strong>Vínculos de campanha · '+data.mode+'</strong>'+lines.join('')+(entries.length?'<ul style="margin:4px 0 0;padding-left:18px">'+entries.join('')+'</ul>':'<small>Nenhum vínculo registrado.</small>');
 const sig=data.mode+'|'+data.obligationTotal+'|'+data.dutyTotal+'|'+data.contributionRank+'|'+entries.join('');
 if(panel&&panel.dataset.signature===sig)return;
 if(!panel){panel=document.createElement('section');panel.className='final-campaign-bonds';heading.insertAdjacentElement('afterend',panel)}
 panel.dataset.signature=sig;panel.style.cssText='margin:8px 0 12px;padding:10px 12px;border:1px solid rgba(150,170,180,.35);border-radius:10px;background:rgba(12,18,24,.82);display:grid;gap:4px';panel.innerHTML=html;
}'''
    anchor='function currentState()'
    i=src.find(anchor)
    if i<0: raise SystemExit('FAIL: currentState anchor ausente')
    src=src[:i]+helper+'\n'+src[i:]
    old='function enhance(){enhanceNarration();enhanceSceneFromEngine();enhanceAccessibility();enhanceTtsSettings();relabelCreationForCampaign();setupOverlay();enhanceAdventureGate();enhanceSceneGeminiButton();enhanceGeminiModels();enhanceCharacterEmptyActions();enhanceUtilityShortcuts();renderBlackSunProgress()}'
    new='function enhance(){enhanceNarration();enhanceSceneFromEngine();enhanceAccessibility();enhanceTtsSettings();relabelCreationForCampaign();setupOverlay();enhanceAdventureGate();enhanceSceneGeminiButton();enhanceGeminiModels();enhanceCharacterEmptyActions();enhanceUtilityShortcuts();renderBlackSunProgress();renderCampaignBondsPanel()}'
    if old not in src: raise SystemExit('FAIL: enhance P28 adventure não encontrado')
    src=src.replace(old,new,1)
    rt.write_text(src,encoding='utf-8')
print('P28_BONDS_UI_PATCHED',rt)
