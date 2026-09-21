#!/usr/bin/env python3
from pathlib import Path
import sys
ROOT=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
candidates=list(ROOT.rglob("final-runtime.js"))
if not candidates: raise SystemExit("FAIL: final-runtime.js não encontrado; P26 não foi montado.")
target=max(candidates,key=lambda p:p.stat().st_size)
src=target.read_text(encoding="utf-8")
start=src.find("function relabelCreationForCampaign(){")
end=src.find("\nfunction currentState()",start)
if start<0 or end<0: raise SystemExit("FAIL: assinatura P26 esperada não encontrada; abortado sem alterar arquivo.")
new="""function relabelCreationForCampaign(){
 const mode=campaignMode(),host=document.body;if(!host)return;host.dataset.campaignMode=mode;
 const cfg={EOTE:{bond:'Obrigação',type:'Tipo de Obrigação',pick:'Selecione uma Obrigação'},AOR:{bond:'Dever',type:'Tipo de Dever',pick:'Selecione um Dever'},MIXED:{bond:'Obrigação e Dever',type:'Vínculos de campanha',pick:'Selecione os vínculos aplicáveis'}}[mode]||{bond:'Obrigação',type:'Tipo de Obrigação',pick:'Selecione uma Obrigação'};
 const replacements=new Map([
  ['Obligation / vínculo de campanha',cfg.bond+' / vínculo de campanha'],['Tipo de Obligation',cfg.type],['Selecione uma Obligation',cfg.pick],
  ['Obligation inicial',mode==='AOR'?'Dever inicial':'Obrigação inicial'],['Obligation adicional',mode==='AOR'?'Dever gasto':'Obrigação adicional'],['Obligation final',mode==='AOR'?'Dever final':'Obrigação final'],
  ['Obligation',mode==='AOR'?'Dever':'Obrigação'],['Duty','Dever'],['Contribution','Contribuição']
 ]);
 const walker=document.createTreeWalker(host,NodeFilter.SHOW_TEXT);let n;while((n=walker.nextNode())){const raw=String(n.nodeValue||''),v=raw.trim();if(replacements.has(v))n.nodeValue=raw.replace(v,replacements.get(v));}
 host.querySelectorAll('[data-campaign-only]').forEach(el=>{const allowed=String(el.dataset.campaignOnly||'').split(',').map(x=>x.trim().toUpperCase());el.hidden=!allowed.includes(mode)});
 window.__BRASEIRO_CAMPAIGN_UI__={mode,obligation:mode!=='AOR',duty:mode!=='EOTE',contribution:mode!=='EOTE'};
}"""
src=src[:start]+new+src[end:]
target.write_text(src,encoding="utf-8")
print(f"PATCHED {target}")
