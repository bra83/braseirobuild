#!/usr/bin/env python3
from __future__ import annotations
import json,re,sys,time
from collections import Counter,defaultdict
from pathlib import Path
from typing import Any
from playwright.sync_api import sync_playwright

if len(sys.argv)!=3:
    raise SystemExit('usage: audit_deep_vtt.py <android-project-root> <output-dir>')
ROOT=Path(sys.argv[1]).resolve(); OUT=Path(sys.argv[2]).resolve(); OUT.mkdir(parents=True,exist_ok=True)
VTT=ROOT/'app/src/main/assets/vtt'
RUNTIME=VTT/'r46/final-runtime.js'; SHELL=VTT/'r46/r46-shell.bundle.js'; CSS=VTT/'r46/final-runtime.css'
UIBRIDGE=VTT/'js/core/uiBridge.bundle.js'; CONTROLLER=VTT/'js/core/controller.mjs'

results:dict[str,Any]={'screens':{},'assets':{},'rules':{},'buttons':{},'runtime':{},'findings':[]}
def rd(p:Path)->str:
    try:return p.read_text('utf-8',errors='replace')
    except:return ''
def add(fid:str,status:str,title:str,detail:str,evidence:Any=None):
    results['findings'].append({'id':fid,'status':status,'title':title,'detail':detail,'evidence':evidence})

def norm(s:str)->str:return re.sub(r'\s+',' ',str(s or '')).strip()

def iter_manifest_entries(obj:Any):
    if isinstance(obj,dict):
        if any(k in obj for k in ('assetId','localPath','path','imagePath')): yield obj
        for v in obj.values(): yield from iter_manifest_entries(v)
    elif isinstance(obj,list):
        for v in obj: yield from iter_manifest_entries(v)

# ---------------- SCREEN REGISTRY ----------------
shell=rd(SHELL); runtime=rd(RUNTIME); controller=rd(CONTROLLER); uibridge=rd(UIBRIDGE)
pat=re.compile(r"screen\((\d+),\s*'([^']+)',\s*'([^']+)',\s*'([^']+)',\s*'([^']+)',\s*'([^']*)'\)")
registry=[]
for m in pat.finditer(shell):
    n=int(m.group(1)); registry.append({'id':n,'route':f'{n:02d}','title':m.group(2),'domain':m.group(3),'template':m.group(4),'icon':m.group(5),'description':m.group(6)})
# de-dup in case bundle contains repeated source copies
seen=set(); registry=[x for x in registry if not (x['route'] in seen or seen.add(x['route']))]
results['screens']['registry']=registry
add('S01','PASS' if len(registry)==31 else 'FAIL','Registro de telas',f'{len(registry)} rotas únicas encontradas; esperado 31.')

# ---------------- ASSET INVENTORY / BINDINGS ----------------
img_ext={'.png','.jpg','.jpeg','.webp','.gif','.svg'}
images=[p for p in VTT.rglob('*') if p.is_file() and p.suffix.lower() in img_ext]
results['assets']['image_files_total']=len(images)
by_top=Counter()
for p in images:
    try: rel=p.relative_to(VTT/'image-library/local'); top=rel.parts[0] if rel.parts else 'root'
    except: top='other'
    by_top[top]+=1
results['assets']['by_top_folder']=dict(by_top)

manifest_candidates=list(VTT.rglob('manifest.json'))+list(VTT.rglob('*visual*manifest*.json'))
manifest_entries=[]
for mp in manifest_candidates:
    try:
        obj=json.loads(rd(mp));
        for e in iter_manifest_entries(obj):
            ee=dict(e);ee['_manifest']=str(mp.relative_to(VTT));manifest_entries.append(ee)
    except:pass
# dedupe by assetId/localPath tuple
uniq={}
for e in manifest_entries:
    k=str(e.get('assetId') or e.get('localPath') or e.get('path') or e.get('imagePath') or id(e));uniq[k]=e
manifest_entries=list(uniq.values())
results['assets']['manifest_entries']=len(manifest_entries)
cat=Counter(); subtype=Counter(); missing=[]; mech=0
for e in manifest_entries:
    cat[str(e.get('category') or 'unknown')]+=1; subtype[str(e.get('subtype') or 'unknown')]+=1
    if e.get('mechanicalBinding') not in (None,'',False,[]): mech+=1
    lp=e.get('localPath') or e.get('path') or e.get('imagePath')
    if isinstance(lp,str) and lp and not lp.startswith(('http://','https://','data:')):
        p=VTT/lp
        if not p.exists(): missing.append({'assetId':e.get('assetId'),'path':lp})
results['assets']['manifest_categories']=dict(cat);results['assets']['manifest_subtypes']=dict(subtype);results['assets']['mechanical_binding_count']=mech;results['assets']['missing_manifest_files']=missing[:200]
add('A01','PASS' if not missing else 'FAIL','Integridade física do manifest',f'{len(manifest_entries)} entradas indexadas; {len(missing)} caminhos locais ausentes.',missing[:12])

binding_files=list(VTT.rglob('bindings.json'))+list(VTT.rglob('*binding*.json'))
binding_blob='\n'.join(rd(p) for p in binding_files)
code_files=[p for p in VTT.rglob('*') if p.is_file() and p.suffix.lower() in {'.js','.mjs','.html','.css','.ts','.tsx'}]
code_blob='\n'.join(rd(p) for p in code_files)

expected_completion=[
'outer_rim_settlement_02.webp','cargo_warehouse_04.webp','wreck_field_01.webp','customs_checkpoint_01.webp','customs_checkpoint_06.webp','customs_checkpoint_04.webp','industrial_district_01.webp','cargo_warehouse_02.webp','customs_checkpoint_03.webp','repair_dock_01.webp','outer_rim_settlement_05.webp','relay_station_01.webp','outer_rim_settlement_01.webp','medbay_03.webp','medbay_01.webp','outer_rim_settlement_03.webp','cargo_warehouse_03.webp','customs_checkpoint_02.webp','relay_station_02.webp','outer_rim_settlement_04.webp','medbay_02.webp','cargo_warehouse_01.webp','medbay_04.webp','customs_checkpoint_05.webp','remote_hideout_01.webp','mining_complex_01.webp',
'alpha_xg1_star_wing.png','skipray_gat12h.png','cloakshape.png','jumpmaster_jm5000.png','wud500.png','vt49_decimator.png','digger_crawler.png','at_est.png','firespray.png',
'lurking_scrange.png','gigantic_dragonsnake.png','kowakian_monkey_lizard.png','son_barab_portrait.webp','son_barab_dossier.webp','power_pack_reload.webp','spacesuit.webp']
file_names=defaultdict(list)
for p in images:file_names[p.name].append(str(p.relative_to(VTT)))
completion=[]
for name in expected_completion:
    paths=file_names.get(name,[]); completion.append({'name':name,'present':bool(paths),'paths':paths,'in_bindings':name in binding_blob,'explicit_code_ref':name in code_blob})
results['assets']['visual_completion']=completion
pres=sum(x['present'] for x in completion); bound=sum(x['in_bindings'] or x['explicit_code_ref'] for x in completion)
add('A02','PASS' if pres==len(completion) else 'FAIL','Pacote visual histórico presente',f'{pres}/{len(completion)} assets esperados encontrados fisicamente.')
add('A03','PASS' if bound==len(completion) else 'WARN','Pacote visual histórico ligado',f'{bound}/{len(completion)} têm referência explícita em bindings/código. Presença física sem ligação não conta como integração.',[x['name'] for x in completion if x['present'] and not (x['in_bindings'] or x['explicit_code_ref'])][:30])

r54root=VTT/'image-library/local/r54'
r54_counts={d:len(list((r54root/d).glob('*'))) if (r54root/d).is_dir() else 0 for d in ['weapons','equipment','portraits','maps']}
results['assets']['r54_counts']=r54_counts
add('A04','PASS' if r54_counts['weapons']>=30 and r54_counts['equipment']>=20 else 'FAIL','R54 armas/equipamentos',json.dumps(r54_counts,ensure_ascii=False))
# Important: current R54 specific resolver only indexes weapons/equipment and one Twi'lek portrait.
r54_groups=set(re.findall(r"\['(weapons|equipment|vehicles|npcs|creatures|droids|scenery)'",runtime))
results['assets']['r54_explicit_groups']=sorted(r54_groups)
add('A05','WARN' if not {'vehicles','npcs','creatures','droids','scenery'}.intersection(r54_groups) else 'PASS','Cobertura do resolver visual R54',f'Grupos explicitamente indexados no hotfix: {sorted(r54_groups)}. Isto não prova ausência no resolver legado; a renderização é auditada abaixo.')

# ---------------- RULES / LIVE ENGINE ----------------
rule_files=list(ROOT.rglob('rules_matrix.json'))
rule_matrix=[]
for rf in rule_files:
    try:
        obj=json.loads(rd(rf));
        if isinstance(obj,list) and len(obj)>len(rule_matrix):rule_matrix=obj
    except:pass
statuses=Counter(str(x.get('status') or x.get('estado') or 'UNKNOWN').upper() for x in rule_matrix if isinstance(x,dict))
results['rules']['matrix_path']=str(rule_files[0].relative_to(ROOT)) if rule_files else None
results['rules']['matrix_statuses']=dict(statuses)
non_test=[x for x in rule_matrix if isinstance(x,dict) and str(x.get('status') or x.get('estado') or '').upper()!='TESTADO']
results['rules']['non_tested']=[{'id':x.get('regra_id') or x.get('id'),'status':x.get('status') or x.get('estado'),'section':x.get('secao') or x.get('title')} for x in non_test[:100]]
if rule_matrix:add('R01','WARN' if non_test else 'PASS','Matriz de regras',f'{len(rule_matrix)} contratos; {dict(statuses)}.',results['rules']['non_tested'])
else:add('R01','WARN','Matriz de regras','rules_matrix.json não localizado na árvore reconstruída.')

aor_inactive='no active Age of Rebellion tree data' in uibridge
results['rules']['aor_talent_tree_inactive_marker']=aor_inactive
add('R02','FAIL' if aor_inactive else 'PASS','Árvores de talentos AoR no runtime vivo','O bundle atual declara explicitamente que não há dados ativos de árvore AoR.' if aor_inactive else 'Marcador de AoR inativo não encontrado no bundle vivo.')

for feature,terms in {
 'dice':['rollPool','resolveSymbols'],'character':['createCustomCharacter'],'combat':['resolveWeaponAttack'],'vehicle_combat':['resolveVehicleWeaponAttack'],'world':['initializeLivingSandbox'],'travel':['travelToLocation'],'master':['requestMasterTurn'],'snapshots':['createSnapshot'],'json':['importCharacterJson','exportCharacterJson']}.items():
    results['rules'].setdefault('feature_presence',{})[feature]=all(t in (uibridge+controller) for t in terms)
add('R03','PASS' if all(results['rules']['feature_presence'].values()) else 'WARN','Presença dos subsistemas centrais',json.dumps(results['rules']['feature_presence'],ensure_ascii=False))

# ---------------- BROWSER DEEP AUDIT ----------------
base='http://127.0.0.1:8765/r46-shell.html'
route_rows={}; all_button_rows=[]; page_errors=[]
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True)
    ctx=browser.new_context(viewport={'width':415,'height':915})
    page=ctx.new_page();page.on('pageerror',lambda e:page_errors.append(str(e)))
    page.add_init_script(r'''window.__auditIllustrationPrompt='';window.__auditMasterRequests=[];window.confirm=()=>true;window.BraseiroGemini={getStatus:()=>JSON.stringify({configured:true,masterModel:'audit-master',model:'audit-image',ttsModel:'audit-tts'}),openGeminiWithPrompt:(p)=>{window.__auditIllustrationPrompt=String(p||'');return JSON.stringify({ok:true,target:'gemini_app',clipboard:true})},requestMasterProposal:(id,p)=>{window.__auditMasterRequests.push({id,p});return JSON.stringify({ok:true,status:'started',requestId:id})},configureModels:()=>JSON.stringify({ok:true}),pickSceneImage:()=>JSON.stringify({ok:true})};''')
    page.goto(base+'?screen=00',wait_until='networkidle',timeout=120000);page.wait_for_timeout(800)
    # bootstrap world through UI to audit the real first-boot controls
    overlay=page.locator('#braseiro-sandbox-setup')
    results['runtime']['first_boot_overlay']=overlay.count()
    if overlay.count():
        results['runtime']['start_points']=page.locator('#sandbox-list button[data-point]').count()
        srch=page.locator('#sandbox-point-search')
        if srch.count():
            srch.fill('Shantipole');page.wait_for_timeout(100);results['runtime']['start_filter_visible']=page.locator('#sandbox-list button[data-point]:visible').count();srch.fill('')
        sh=page.locator('#sandbox-list button[data-point]',has_text='Shantipole').first
        if sh.count():sh.click()
        page.locator('#sandbox-start-button').click();page.wait_for_selector('#braseiro-sandbox-setup',state='detached',timeout=10000);page.wait_for_timeout(300)
    try:state=page.evaluate('window.BraseiroGameBridge.readState().data') or {}
    except:state={}
    results['runtime']['startMapPoint']=(((state.get('world') or {}).get('sandbox') or {}).get('startMapPoint'))
    if not ((state.get('player') or {}).get('character')):
        rnd=page.locator('[data-random-character]')
        if rnd.count():rnd.click();page.wait_for_timeout(700)
    try:state=page.evaluate('window.BraseiroGameBridge.readState().data') or {}
    except:state={}
    if page.locator('[data-start-adventure]').count():page.locator('[data-start-adventure]').click();page.wait_for_timeout(900)
    # exposed bridge functions
    try:
        methods=page.evaluate("()=>{const b=window.BraseiroGameBridge||{};return Array.from(new Set([...Object.keys(b),...Object.getOwnPropertyNames(Object.getPrototypeOf(b)||{})])).sort()}")
    except:methods=[]
    results['runtime']['bridge_methods']=methods
    for feature in ['readState','initializeLivingSandbox','startLivingAdventure','setCampaignMode','campaignModeSummary']:
        results['runtime'].setdefault('bridge_required',{})[feature]=feature in methods or feature in uibridge

    # dynamic illustration handoff
    ill=page.locator('.final-scene-gemini button')
    if ill.count():ill.click();page.wait_for_timeout(150)
    try:prompt=page.evaluate('window.__auditIllustrationPrompt')
    except:prompt=''
    results['buttons']['illustration']={'button':ill.count(),'prompt_chars':len(prompt or ''),'real_contract':bool(prompt and 'FATOS VISÍVEIS DA CENA' in prompt and 'Não coloque texto' in prompt)}

    # Dice global button
    dice=page.locator('button',has_text=re.compile('DADOS',re.I)).first
    dice_ok=False
    if dice.count():
        try:dice.click();page.wait_for_timeout(100);dice_ok=page.locator('.r53-dice-overlay').count()==1;page.locator('.r53-dice-overlay .r53-close').click() if page.locator('.r53-dice-overlay .r53-close').count() else None
        except:pass
    results['buttons']['dice']={'found':dice.count(),'overlay':dice_ok}

    # Every route: render, accessibility, broken images, overflow, button inventory.
    for entry in registry:
        route=entry['route']; before=len(page_errors)
        try:
            page.goto(base+f'?screen={route}',wait_until='networkidle',timeout=120000);page.wait_for_timeout(260)
            title=norm(page.locator('h1').first.inner_text()) if page.locator('h1').count() else ''
            bodytxt=norm(page.locator('body').inner_text())[:300]
            broken=page.locator('img').evaluate_all("imgs=>imgs.filter(i=>i.complete&&i.naturalWidth===0).map(i=>i.getAttribute('src'))")
            overflow=page.evaluate('document.documentElement.scrollWidth-window.innerWidth')
            buttons=page.locator('button:visible')
            bl=[]; unnamed=[]
            for i in range(buttons.count()):
                b=buttons.nth(i)
                try:
                    txt=norm(b.inner_text()); aria=norm(b.get_attribute('aria-label') or ''); titleattr=norm(b.get_attribute('title') or ''); name=txt or aria or titleattr
                    row={'route':route,'name':name,'disabled':b.is_disabled(),'type':b.get_attribute('type') or ''}; bl.append(row);all_button_rows.append(row)
                    if not name:unnamed.append(i)
                except:pass
            route_rows[route]={'expected':entry['title'],'rendered_h1':title,'body_head':bodytxt,'buttons':bl,'unnamed_buttons':unnamed,'broken_images':broken,'overflow_px':overflow,'new_errors':page_errors[before:]}
        except Exception as exc:route_rows[route]={'expected':entry['title'],'exception':repr(exc)}
    results['screens']['runtime']=route_rows
    bad_routes=[r for r,v in route_rows.items() if v.get('exception') or v.get('new_errors') or v.get('broken_images') or (v.get('overflow_px') or 0)>2]
    unnamed_routes=[r for r,v in route_rows.items() if v.get('unnamed_buttons')]
    add('S02','PASS' if not bad_routes else 'FAIL','Render das 31 telas',f'{len(route_rows)}/31 visitadas; rotas com crash/erro/imagem quebrada/overflow: {bad_routes}.')
    add('B01','PASS' if not unnamed_routes else 'WARN','Acessibilidade/identificação de botões',f'Rotas com botão visível sem texto/aria-label/title: {unnamed_routes}.')

    # Bottom navigation functional routes
    nav_test={}
    page.goto(base+'?screen=00',wait_until='networkidle');page.wait_for_timeout(300)
    for label,target in [('CENA','00'),('PERSONAGEM','06'),('ITENS','08'),('NAVE','10'),('REGISTROS','12')]:
        loc=page.locator('.bottom-nav-button',has_text=label).first
        ok=False
        try:
            if loc.count():loc.click();page.wait_for_timeout(80);ok=(new URL(page.url)).searchParams.get('screen')==target
        except:pass
        # Python parse fallback
        if loc.count():
            try:ok=(re.search(r'[?&]screen=([0-9]+)',page.url).group(1)==target)
            except:pass
        nav_test[label]={'found':loc.count(),'target':target,'ok':ok}
    results['buttons']['bottom_navigation']=nav_test
    add('B02','PASS' if all(v['ok'] for v in nav_test.values()) else 'FAIL','Menu inferior funcional',json.dumps(nav_test,ensure_ascii=False))

    # Ficha / Progressão tabs
    page.goto(base+'?screen=06',wait_until='networkidle');page.wait_for_timeout(250)
    prog=page.locator('button',has_text=re.compile('Progressão',re.I)).first
    prog_ok=False
    if prog.count():
        try:prog.click();page.wait_for_timeout(100);prog_ok='screen=07' in page.url
        except:pass
    results['buttons']['character_progression']={'found':prog.count(),'ok':prog_ok}

    # Map runtime: image + marker + basic zoom/pan controls if available.
    page.goto(base+'?screen=16',wait_until='networkidle');page.wait_for_timeout(350)
    map_imgs=page.locator('img:visible'); map_buttons=[norm(page.locator('button:visible').nth(i).inner_text()) for i in range(page.locator('button:visible').count())]
    results['buttons']['map']={'images':map_imgs.count(),'buttons':map_buttons,'broken':map_imgs.evaluate_all("imgs=>imgs.filter(i=>i.complete&&i.naturalWidth===0).length") if map_imgs.count() else 0}

    # Settings surface: ensure Gemini/TTS/library controls are reachable.
    settings_text=''
    for rr in ['27','28','29','30']:
        page.goto(base+f'?screen={rr}',wait_until='networkidle');page.wait_for_timeout(180);settings_text+=' '+norm(page.locator('body').inner_text())
    results['buttons']['settings_surface']={k:bool(re.search(p,settings_text,re.I)) for k,p in {'tts':'TTS|voz','gemini':'Gemini','library':'Biblioteca','diagnostic':'diagnóstico|diagnostico','json':'JSON'}.items()}

    # Restart is deliberately last because it clears state.
    page.goto(base+'?screen=00',wait_until='networkidle');page.wait_for_timeout(250)
    restart=page.locator('button',has_text=re.compile('REINICIAR',re.I)).first; restart_ok=False
    if restart.count():
        try:restart.click();page.wait_for_timeout(600);restart_ok=page.locator('#braseiro-sandbox-setup').count()==1
        except:pass
    results['buttons']['restart']={'found':restart.count(),'returned_setup':restart_ok}
    add('B03','PASS' if results['buttons']['illustration']['real_contract'] else 'FAIL','GERAR ILUSTRAÇÃO',json.dumps(results['buttons']['illustration'],ensure_ascii=False))
    add('B04','PASS' if dice_ok else 'FAIL','Botão DADOS',json.dumps(results['buttons']['dice'],ensure_ascii=False))
    add('B05','PASS' if restart_ok else 'FAIL','Botão REINICIAR',json.dumps(results['buttons']['restart'],ensure_ascii=False))
    add('B06','PASS' if prog_ok else 'WARN','Ficha → Progressão',json.dumps(results['buttons']['character_progression'],ensure_ascii=False))
    add('M01','FAIL' if results['runtime']['startMapPoint'] is None else 'PASS','Âncora do ponto inicial no mapa',f"startMapPoint={results['runtime']['startMapPoint']}")

    # Mobile secondary viewport: all routes basic render/errors/overflow.
    c2=browser.new_context(viewport={'width':360,'height':800});p2=c2.new_page();errs2=[];p2.on('pageerror',lambda e:errs2.append(str(e)))
    bad360=[]
    for entry in registry:
        try:
            p2.goto(base+f"?screen={entry['route']}",wait_until='domcontentloaded',timeout=60000);p2.wait_for_timeout(80)
            if p2.evaluate('document.documentElement.scrollWidth-window.innerWidth')>2:bad360.append(entry['route'])
        except:bad360.append(entry['route'])
    results['screens']['360x800']={'bad_routes':bad360,'errors':errs2}
    add('S03','PASS' if not bad360 and not errs2 else 'FAIL','31 telas em 360×800',f'bad_routes={bad360}; errors={errs2[:10]}')
    c2.close();ctx.close();browser.close()

results['buttons']['inventory']=all_button_rows
results['runtime']['page_errors']=page_errors

# Summaries
counts=Counter(f['status'] for f in results['findings']);results['summary']=dict(counts)
# Compact Markdown
lines=['# STAR WARS — EDGE OF THE GALAXY — AUDITORIA PROFUNDA R54','',f"Resumo: **{counts.get('PASS',0)} PASS · {counts.get('WARN',0)} WARN · {counts.get('FAIL',0)} FAIL**",'', '## Achados']
for f in results['findings']:
    lines.append(f"- **{f['status']} {f['id']} — {f['title']}**: {f['detail']}")
lines += ['', '## Regras', f"- Status da matriz: `{json.dumps(results['rules'].get('matrix_statuses',{}),ensure_ascii=False)}`", f"- Não TESTADO: `{json.dumps(results['rules'].get('non_tested',[]),ensure_ascii=False)}`", f"- AoR árvore inativa no bundle: `{results['rules'].get('aor_talent_tree_inactive_marker')}`",'', '## Assets',f"- Imagens físicas no VTT: **{results['assets'].get('image_files_total',0)}**",f"- Entradas de manifest: **{results['assets'].get('manifest_entries',0)}**",f"- Arquivos ausentes do manifest: **{len(results['assets'].get('missing_manifest_files',[]))}**",f"- Pacote visual histórico presente: **{pres}/{len(completion)}**",f"- Pacote visual histórico com binding/referência explícita: **{bound}/{len(completion)}**",f"- R54: `{json.dumps(r54_counts,ensure_ascii=False)}`",'', '## Telas']
for r,v in route_rows.items():
    lines.append(f"- `{r}` {v.get('expected')}: h1=`{v.get('rendered_h1','')}` · buttons={len(v.get('buttons',[]))} · broken={len(v.get('broken_images',[]))} · overflow={v.get('overflow_px')} · errors={len(v.get('new_errors',[]))}")
lines += ['', '## Botões-chave', f"- Menu inferior: `{json.dumps(results['buttons'].get('bottom_navigation',{}),ensure_ascii=False)}`",f"- Ilustração: `{json.dumps(results['buttons'].get('illustration',{}),ensure_ascii=False)}`",f"- Dados: `{json.dumps(results['buttons'].get('dice',{}),ensure_ascii=False)}`",f"- Reiniciar: `{json.dumps(results['buttons'].get('restart',{}),ensure_ascii=False)}`",f"- Progressão: `{json.dumps(results['buttons'].get('character_progression',{}),ensure_ascii=False)}`",f"- Ajustes: `{json.dumps(results['buttons'].get('settings_surface',{}),ensure_ascii=False)}`",'', '## Arquivos de prova', '- `DEEP_AUDIT.json`: inventário completo de telas, botões, assets, regras e runtime.', '- `DEEP_AUDIT.md`: este resumo.']
(OUT/'DEEP_AUDIT.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'DEEP_AUDIT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('\n'.join(lines[:80]))
