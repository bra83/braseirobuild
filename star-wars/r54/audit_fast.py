#!/usr/bin/env python3
from __future__ import annotations
import json,re,sys
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT=Path(sys.argv[1]).resolve(); OUT=Path(sys.argv[2]).resolve(); OUT.mkdir(parents=True,exist_ok=True)
NODE=Path(sys.argv[3]).resolve() if len(sys.argv)>3 else None
ANDROID=int(sys.argv[4]) if len(sys.argv)>4 else -999
V=ROOT/'app/src/main/assets/vtt'; R=V/'r46/final-runtime.js'; C=V/'r46/final-runtime.css'; CTRL=V/'js/core/controller.mjs'; SHELL=V/'r46/r46-shell.bundle.js'; MAIN=ROOT/'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'; GEM=ROOT/'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'; LIB=V/'image-library/local/r54'

def rd(p):
    try:return p.read_text('utf-8',errors='replace')
    except:return ''
def fbody(src,name):
    m=re.search(rf'\b{name}\s*\([^)]*\)\s*\{{',src)
    if not m:return ''
    b=src.find('{',m.start());d=0;ins=False;q='';esc=False
    for i in range(b,len(src)):
        ch=src[i]
        if ins:
            if esc:esc=False
            elif ch=='\\':esc=True
            elif ch==q:ins=False
            continue
        if ch in "'\"`":ins=True;q=ch
        elif ch=='{':d+=1
        elif ch=='}':
            d-=1
            if d==0:return src[m.start():i+1]
    return ''
def wc(s):return len(re.findall(r"\b[\wÀ-ÿ'-]+\b",s))
def senses(s):
    x=s.casefold(); groups={'som':['som','ruído','ruido','zumbido','chiado','eco','vozes'],'cheiro':['cheiro','odor','aroma','fedor'],'temperatura':['calor','frio','quente','gelado','temperatura'],'textura':['poeira','areia','áspero','aspero','úmido','umido','vibra','metal'],'luz':['luz','sombra','brilho','clarão','clarao','névoa','nevoa','fumaça','fumaca']};return [k for k,v in groups.items() if any(t in x for t in v)]

rt,css,ctrl,shell,main,gem=map(rd,[R,C,CTRL,SHELL,MAIN,GEM]); findings=[];metrics={}
def add(i,t,s,d,e=None):findings.append({'id':i,'title':t,'status':s,'detail':d,'evidence':e})

starts=json.loads(rd(LIB/'r54-start-locations.json') or '[]'); bindings=json.loads(rd(LIB/'bindings.json') or '{}')
weapons=list((LIB/'weapons').glob('*.webp')); equipment=list((LIB/'equipment').glob('*.webp')); portraits=list((LIB/'portraits').glob('*')); maps=list((LIB/'maps').glob('*'))
metrics.update(start_locations=len(starts),weapon_assets=len(weapons),equipment_assets=len(equipment),portrait_assets=len(portraits),map_assets=len(maps))
add('01','82 pontos iniciais','PASS' if len(starts)==82 else 'FAIL',f'{len(starts)} locais no catálogo.')
add('02','Safe-area Android','PASS' if 'systemInsetTop / density' in main and 'systemInsetBottom / density' in main else 'FAIL','Insets físicos são convertidos por densidade antes de virar CSS.' if 'systemInsetTop / density' in main else 'Conversão por densidade ausente.')
add('03','Menu inferior / CENA','PASS' if 'r54-scene-icon' in rt and '[aria-current="page"]{transform:none!important}' in css else 'FAIL','SVG limpo e seleção sem deslocamento encontrados no R54.')
rb=fbody(rt,'r53RestartAdventure'); add('04','Reiniciar campanha','PASS' if "removeItem('braseiro.starwars.edge.state.v1')" in rb and '.newCampaign' not in rb else 'FAIL','Não usa mais newCampaign não exposto; limpa estado persistido.')
sb=fbody(rt,'enhanceSceneGeminiButton');sp=fbody(rt,'sceneGeminiPrompt');add('05','Gerar ilustração → Gemini externo','PASS' if 'openGeminiWithPrompt(prompt)' in sb and 'generateSceneImage(' not in sb and 'Intent.ACTION_SEND' in gem else 'FAIL','O VTT monta prompt e faz handoff para Gemini; não gera a imagem dentro do VTT.')
add('06','Prompt visual da cena','PASS' if all(x in sp for x in ['FATOS VISÍVEIS DA CENA','Não coloque texto','16:9']) else 'FAIL','Prompt usa fatos visíveis e proíbe texto/HUD.')
add('07','Armas com assets','PASS' if len(weapons)>=30 else 'FAIL',f'{len(weapons)} imagens de armas extraídas.')
add('08','Equipamentos com assets','PASS' if len(equipment)>=20 else 'FAIL',f'{len(equipment)} imagens de equipamento extraídas.')
add('09','Retrato Veya/Twi’lek','PASS' if 'twilek_female_blue.webp' in rt and portraits else 'FAIL',f'{len(portraits)} retrato(s) R54; binding Twi’lek encontrado.')

# Map coordinate integration: engine has support but current R54 selector must actually pass it.
call=''.join(re.findall(r"initializeLivingSandbox'\s*,\s*\{[^}]{0,500}\}",rt))
anchor_ok='startMapPoint' in call and 'startMapPoint' in ctrl
add('10','Local inicial ancora coordenadas no mapa galáctico','PASS' if anchor_ok else 'FAIL','O motor suporta startMapPoint, mas a UI R54 não envia x/y do local escolhido.' if not anchor_ok else 'Coordenadas são enviadas e persistidas.')

# Existing map catalog is broader than the explicit R54 preview hook.
all_manifest='\n'.join(rd(p) for p in (V/'image-library').rglob('*.json') if p.is_file())
map_refs=len(re.findall(r'"mapKind"\s*:\s*"(?:settlement|planet|district|streets|interior|location|ship|galaxy)"',all_manifest))
metrics['catalogued_map_refs']=map_refs
add('11','Mapas existentes do acervo são aproveitados','WARN' if map_refs>3 else 'PASS',f'Foram localizadas {map_refs} referências de mapa na biblioteca; o hook R54 explicita preview apenas para Shantipole, Mos Eisley e Tatooine. A exposição ainda não cobre toda a biblioteca.')

# Mutation loop in item image patch.
ib=fbody(rt,'r54EnhanceItemImages'); idempotent=('dataset.r54' in ib or 'r54VisualBound' in ib or ('existing' in ib and 'return' in ib))
loop_risk=('querySelectorAll(\'.r53-item-visual,.r54-item-visual\')' in ib and '.remove()' in ib and 'insertBefore' in ib and not idempotent)
add('12','Binding de imagens de itens é idempotente','FAIL' if loop_risk else 'PASS','O MutationObserver chama r54Tick; r54EnhanceItemImages remove e reinsere a imagem em toda mutação, podendo criar ciclo contínuo de DOM/reload.' if loop_risk else 'Binding não recria indefinidamente o mesmo asset.')

# Full visual categories: hotfix itself only indexes weapons/equipment.
idx=fbody(rt,'r54AssetIndex'); categories=[x for x in ['weapons','equipment','vehicles','npcs','creatures','droids','scenery'] if x in idx]
add('13','Assets visuais de todas as categorias do VTT','WARN' if len(categories)<7 else 'PASS',f'Binding R54 indexa explicitamente: {categories}. NPCs, criaturas, droides, veículos/cenário do acervo não estão todos ligados por esse hotfix.')

# Master narration: initial local template and subsequent native Gemini instruction.
opening=fbody(ctrl,'startLivingAdventure'); template_short=('A campanha começa em ${locationName}' in opening and "narration: parts.join(' ')" in opening)
add('14','Abertura é narrativa longa, não resumo-template','FAIL' if template_short else 'PASS','startLivingAdventure ainda monta frases-resumo fixas localmente.' if template_short else 'Abertura não usa o resumo curto antigo.')
concise='A narração deve ser cinematográfica, concisa e em português do Brasil.' in gem
add('15','Mestre Gemini exige texto grande','FAIL' if concise else 'PASS','O system prompt atual manda explicitamente: “A narração deve ser cinematográfica, concisa”. Isso contradiz o requisito de narração grande.')
long_contract=bool(re.search(r'\d+\s*[–-]\s*\d+\s+palavras|\d+\s*(?:a|–|-)\s*\d+\s+parágrafos',gem+shell+ctrl,re.I))
add('16','Contrato mínimo de extensão narrativa','PASS' if long_contract else 'FAIL','Não há mínimo de palavras/parágrafos; há somente teto de até 5000 caracteres no schema.')
sensory_terms=[x for x in ['sons','cheiros','temperatura','texturas','sensações','sensacoes'] if x in (gem+shell+ctrl).casefold()]
add('17','Contrato sensorial do Mestre','PASS' if len(sensory_terms)>=3 else 'FAIL',f'Não existe checklist sensorial suficiente no prompt Star Wars. Termos encontrados: {sensory_terms}.')
add('18','Ponte Barbara ↔ Gemini','PASS' if 'requestMasterProposal' in shell and 'braseiroGeminiNativeEvent' in shell and 'generateMasterRemote' in gem else 'FAIL','Request/callback do Mestre nativo estão implementados.')

# Node suite
if NODE and NODE.exists():
    tap=rd(NODE);tm=re.search(r'^# tests\s+(\d+)',tap,re.M);pm=re.search(r'^# pass\s+(\d+)',tap,re.M);fm=re.search(r'^# fail\s+(\d+)',tap,re.M);fails=re.findall(r'^not ok\s+\d+\s+-\s+(.+)$',tap,re.M);nt=int(tm.group(1)) if tm else None;np=int(pm.group(1)) if pm else None;nf=int(fm.group(1)) if fm else len(fails);metrics.update(node_tests=nt,node_pass=np,node_fail=nf,node_fail_names=fails[:30]);add('19','Suite Node completa','PASS' if nf==0 else 'FAIL',f'tests={nt}, pass={np}, fail={nf}',fails[:20])
else:add('19','Suite Node completa','WARN','Log não disponível.')
add('20','Compilação Android','PASS' if ANDROID==0 and list((ROOT/'app/build/outputs/apk/debug').glob('*.apk')) else 'FAIL',f'Gradle exit={ANDROID}; APK debug presente={bool(list((ROOT/"app/build/outputs/apk/debug").glob("*.apk")))}')

# Bounded real browser flow. Never enters the known item MutationObserver loop.
b={};errs=[]
try:
  with sync_playwright() as p:
    br=p.chromium.launch(headless=True);ctx=br.new_context(viewport={'width':415,'height':915});pg=ctx.new_page();pg.set_default_timeout(8000);pg.on('pageerror',lambda e:errs.append(str(e)))
    pg.add_init_script("""window.__p='';window.BraseiroGemini={getStatus:()=>JSON.stringify({configured:true}),openGeminiWithPrompt:(p)=>{window.__p=String(p||'');return JSON.stringify({ok:true,target:'gemini_app',clipboard:true})},configureModels:()=>JSON.stringify({ok:true}),pickSceneImage:()=>JSON.stringify({ok:true})};window.confirm=()=>true;""")
    pg.goto('http://127.0.0.1:8765/r46-shell.html?screen=00',wait_until='domcontentloaded',timeout=15000);pg.wait_for_timeout(800)
    b['overlay']=pg.locator('#braseiro-sandbox-setup').count();b['points']=pg.locator('#sandbox-list button[data-point]').count()
    s=pg.locator('#sandbox-point-search');s.fill('Shantipole');pg.wait_for_timeout(100);b['filter']=pg.locator('#sandbox-list button[data-point]:visible').count();s.fill('')
    sh=pg.locator('#sandbox-list button[data-point]',has_text='Shantipole').first;sh.click();pg.locator('#sandbox-start-button').click();pg.wait_for_selector('#braseiro-sandbox-setup',state='detached');pg.wait_for_timeout(300)
    st=pg.evaluate('window.BraseiroGameBridge.readState().data');b['anchor']=(((st or {}).get('world') or {}).get('sandbox') or {}).get('startMapPoint')
    # Engine-level character + opening; avoids UI timing ambiguity.
    pg.evaluate("window.BraseiroGameBridge.generateRandomCharacter({name:'Veya Tann',motivation:'Sobreviver sem perder a liberdade.'},{createSnapshot:true})")
    pg.evaluate("window.BraseiroGameBridge.startLivingAdventure({})");pg.wait_for_timeout(500)
    st=pg.evaluate('window.BraseiroGameBridge.readState().data');nar=str((((st or {}).get('master') or {}).get('narration')) or (((st or {}).get('scene') or {}).get('narration')) or '');b['narration_words']=wc(nar);b['narration_chars']=len(nar);b['senses']=senses(nar);(OUT/'OPENING_NARRATION_ACTUAL.txt').write_text(nar,'utf-8')
    pg.reload(wait_until='domcontentloaded');pg.wait_for_timeout(500);ill=pg.locator('.final-scene-gemini button');b['illustration_button']=ill.count();ill.click();pg.wait_for_timeout(100);pr=pg.evaluate('window.__p');b['illustration_prompt_chars']=len(pr or '');b['illustration_ok']=bool(pr and 'FATOS VISÍVEIS DA CENA' in pr and 'Não coloque texto' in pr)
    icons=pg.locator('.bottom-navigation .reference-icon-mask');boxes=[icons.nth(i).bounding_box() for i in range(min(5,icons.count()))];ys=[x['y'] for x in boxes if x];hs=[x['height'] for x in boxes if x];b['nav_y_span']=max(ys)-min(ys) if ys else None;b['nav_h_span']=max(hs)-min(hs) if hs else None
    # Force only visual species/name in persisted copy to verify the Twi'lek portrait hook without changing engine rules.
    pg.evaluate("""()=>{const k='braseiro.starwars.edge.state.v1',s=JSON.parse(localStorage.getItem(k));if(s?.player?.character){s.player.character.name='Veya Tann';s.player.character.species=\"Twi'lek\";s.player.character.speciesName=\"Twi'lek\";localStorage.setItem(k,JSON.stringify(s));}}""")
    pg.goto('http://127.0.0.1:8765/r46-shell.html?screen=06',wait_until='domcontentloaded',timeout=15000);pg.wait_for_timeout(500);pi=pg.locator('.r54-character-portrait img');b['portrait_count']=pi.count();b['portrait_loaded']=pi.first.evaluate('i=>i.complete&&i.naturalWidth>0') if pi.count() else False
    pg.goto('http://127.0.0.1:8765/r46-shell.html?screen=00',wait_until='domcontentloaded',timeout=15000);pg.wait_for_timeout(400);r=pg.locator('button',has_text=re.compile('REINICIAR',re.I)).first;b['restart_button']=r.count();r.click();pg.wait_for_timeout(700);b['restart_setup']=pg.locator('#braseiro-sandbox-setup').count()==1
    ctx.close();ctx2=br.new_context(viewport={'width':360,'height':800});p2=ctx2.new_page();e2=[];p2.on('pageerror',lambda e:e2.append(str(e)));p2.goto('http://127.0.0.1:8765/r46-shell.html?screen=00',wait_until='domcontentloaded',timeout=15000);p2.wait_for_timeout(400);b['nav360']=p2.locator('.bottom-nav-button').count();b['errors360']=e2;ctx2.close();br.close()
except Exception as e:b['exception']=repr(e)
b['errors415']=errs;metrics['browser']=b
add('21','Fluxo first-boot/lista/busca','PASS' if b.get('points')==82 and b.get('filter')==1 else 'FAIL',f"points={b.get('points')}; filtro={b.get('filter')}")
add('22','Âncora real do local inicial','PASS' if b.get('anchor') else 'FAIL',f"startMapPoint={b.get('anchor')}")
add('23','Abertura real longa/sensorial','PASS' if (b.get('narration_words') or 0)>=250 and len(b.get('senses') or [])>=3 else 'FAIL',f"{b.get('narration_words')} palavras / {b.get('narration_chars')} caracteres / sentidos={b.get('senses')}. O piso de 250 palavras é apenas um teste conservador; o problema principal é o contrato atual mandar ser conciso.")
add('24','Handoff de ilustração em runtime','PASS' if b.get('illustration_ok') else 'FAIL',f"button={b.get('illustration_button')}; prompt={b.get('illustration_prompt_chars')} chars")
add('25','Alinhamento runtime do menu inferior','PASS' if b.get('nav_y_span') is not None and b.get('nav_y_span')<=2 and b.get('nav_h_span') is not None and b.get('nav_h_span')<=1 else 'FAIL',f"y-span={b.get('nav_y_span')}; h-span={b.get('nav_h_span')}")
add('26','Retrato Twi’lek carrega em runtime','PASS' if b.get('portrait_count',0)>=1 and b.get('portrait_loaded') else 'FAIL',f"count={b.get('portrait_count')}; loaded={b.get('portrait_loaded')}")
add('27','REINICIAR ponta a ponta','PASS' if b.get('restart_setup') else 'FAIL',f"button={b.get('restart_button')}; voltou setup={b.get('restart_setup')}")
add('28','Sem erro JS em 415×915 e 360×800','PASS' if not b.get('exception') and not b.get('errors415') and not b.get('errors360') else 'FAIL',f"exception={b.get('exception')}; 415={b.get('errors415')}; 360={b.get('errors360')}")

counts={s:sum(x['status']==s for x in findings) for s in ['PASS','WARN','FAIL']};metrics['counts']=counts
lines=['# STAR WARS — EDGE OF THE GALAXY — AUDITORIA FINAL R54','',f"PASS **{counts['PASS']}** · WARN **{counts['WARN']}** · FAIL **{counts['FAIL']}**",'','## Achados','']
for x in findings:
 lines += [f"### {x['status']} — {x['id']} — {x['title']}",'',x['detail'],'']
 if x.get('evidence') not in (None,[], ''): lines += ['```json',json.dumps(x['evidence'],ensure_ascii=False,indent=2)[:5000],'```','']
lines += ['## Métricas','```json',json.dumps(metrics,ensure_ascii=False,indent=2),'```','']
(OUT/'FULL_AUDIT_FINAL.md').write_text('\n'.join(lines),'utf-8');(OUT/'FULL_AUDIT_FINAL.json').write_text(json.dumps({'counts':counts,'findings':findings,'metrics':metrics},ensure_ascii=False,indent=2),'utf-8');print(json.dumps(counts))
