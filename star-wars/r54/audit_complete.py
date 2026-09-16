#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
import time
from pathlib import Path
from typing import Any

from playwright.sync_api import sync_playwright

if len(sys.argv) < 3:
    raise SystemExit('usage: audit_complete.py <android-project-root> <output-dir> [node-tap] [android-exit]')

ROOT = Path(sys.argv[1]).resolve()
OUT = Path(sys.argv[2]).resolve()
NODE_TAP = Path(sys.argv[3]).resolve() if len(sys.argv) > 3 and sys.argv[3] else None
ANDROID_EXIT = int(sys.argv[4]) if len(sys.argv) > 4 and str(sys.argv[4]).strip() else None
OUT.mkdir(parents=True, exist_ok=True)

VTT = ROOT / 'app/src/main/assets/vtt'
RUNTIME = VTT / 'r46/final-runtime.js'
CSS = VTT / 'r46/final-runtime.css'
SHELL = VTT / 'r46/r46-shell.bundle.js'
CONTROLLER = VTT / 'js/core/controller.mjs'
UIBRIDGE = VTT / 'js/core/uiBridge.bundle.js'
MAIN = ROOT / 'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'
GEMINI = ROOT / 'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'
R54ROOT = VTT / 'image-library/local/r54'
STARTS = R54ROOT / 'r54-start-locations.json'
BINDINGS = R54ROOT / 'bindings.json'

findings: list[dict[str, Any]] = []
metrics: dict[str, Any] = {}

def read(path: Path) -> str:
    try:
        return path.read_text(encoding='utf-8', errors='replace')
    except Exception:
        return ''

def add(fid: str, title: str, status: str, detail: str, evidence: Any = None) -> None:
    findings.append({'id': fid, 'title': title, 'status': status, 'detail': detail, 'evidence': evidence})

def function_body(src: str, name: str) -> str:
    m = re.search(rf'\b{name}\s*\([^)]*\)\s*\{{', src)
    if not m:
        return ''
    brace = src.find('{', m.start())
    depth = 0
    ins = False
    quote = ''
    esc = False
    for i in range(brace, len(src)):
        c = src[i]
        if ins:
            if esc:
                esc = False
            elif c == '\\':
                esc = True
            elif c == quote:
                ins = False
            continue
        if c in ('"', "'", '`'):
            ins = True
            quote = c
        elif c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return src[m.start():i + 1]
    return ''

def words(s: str) -> int:
    return len(re.findall(r"\b[\wÀ-ÿ'-]+\b", s, re.UNICODE))

def sensory_hits(s: str) -> list[str]:
    low = s.casefold()
    groups = {
        'som': ['som ', 'sons', 'ruído', 'ruidos', 'zumbido', 'chiado', 'eco', 'silêncio', 'silencio', 'voz', 'vozes'],
        'cheiro': ['cheiro', 'odor', 'fedor', 'aroma'],
        'temperatura': ['calor', 'frio', 'quente', 'gelado', 'temperatura'],
        'tato/textura': ['áspero', 'aspero', 'úmido', 'umido', 'seco', 'poeira', 'areia', 'metal', 'vibra', 'vibração', 'vibracao'],
        'luz/visão': ['luz', 'sombra', 'brilho', 'clarão', 'clarao', 'escuro', 'névoa', 'nevoa', 'fumaça', 'fumaca'],
    }
    return [k for k, vals in groups.items() if any(v in low for v in vals)]

runtime = read(RUNTIME)
css = read(CSS)
shell = read(SHELL)
controller = read(CONTROLLER)
uibridge = read(UIBRIDGE)
main_java = read(MAIN)
gemini_java = read(GEMINI)

# A. Inputs / syntax-sensitive corrections
required_files = [RUNTIME, CSS, SHELL, CONTROLLER, UIBRIDGE, MAIN, GEMINI, STARTS, BINDINGS]
missing = [str(p.relative_to(ROOT)) for p in required_files if not p.is_file()]
add('A01', 'Arquivos essenciais do runtime', 'PASS' if not missing else 'FAIL',
    'Todos os arquivos necessários estão presentes.' if not missing else 'Faltam arquivos essenciais.', missing)

try:
    starts = json.loads(read(STARTS))
except Exception:
    starts = []
metrics['start_locations'] = len(starts) if isinstance(starts, list) else 0
add('A02', 'Catálogo de pontos iniciais', 'PASS' if isinstance(starts, list) and len(starts) >= 82 else 'FAIL',
    f'{len(starts) if isinstance(starts, list) else 0} locais carregados no catálogo R54.',
    [x.get('name') for x in starts[:8]] if isinstance(starts, list) else None)

safe_ok = all(x in main_java for x in ['systemInsetTop / density', 'systemInsetBottom / density', 'cssTop', 'cssBottom'])
add('A03', 'Safe-area Android usa px físicos convertidos para CSS/dp', 'PASS' if safe_ok else 'FAIL',
    'Conversão por densidade encontrada no host Android.' if safe_ok else 'Conversão por densidade não encontrada integralmente.')

nav_ok = all(x in css for x in ['.bottom-navigation .bottom-nav-button[aria-current="page"]{transform:none!important}', '.bottom-navigation .r54-scene-icon svg']) and 'r54-scene-icon' in runtime
add('A04', 'Ícone CENA e alinhamento do menu inferior', 'PASS' if nav_ok else 'FAIL',
    'R54 força a mesma geometria do item selecionado e substitui o recorte por SVG limpo.' if nav_ok else 'Correção completa do menu inferior não encontrada.')

restart_body = function_body(runtime, 'r53RestartAdventure')
restart_ok = all(x in restart_body for x in ["localStorage.removeItem('braseiro.starwars.edge.state.v1')", "localStorage.removeItem('braseiro.starwars.edge.snapshots.v1')", 'sessionStorage.clear()']) and '.newCampaign' not in restart_body
add('A05', 'REINICIAR não depende de newCampaign não exposto', 'PASS' if restart_ok else 'FAIL',
    'Reinício limpa estado/snapshots e retorna ao bootstrap.' if restart_ok else 'Reinício ainda depende de API inexistente ou não limpa o estado por completo.')

scene_button = function_body(runtime, 'enhanceSceneGeminiButton')
scene_prompt = function_body(runtime, 'sceneGeminiPrompt')
illustration_ok = 'openGeminiWithPrompt(prompt)' in scene_button and 'generateSceneImage(' not in scene_button
handoff_native_ok = all(x in gemini_java for x in ['openGeminiWithPrompt(String prompt)', 'ClipboardManager', 'Intent.ACTION_SEND', 'Intent.EXTRA_TEXT', 'com.google.android.apps.bard'])
add('A06', 'GERAR ILUSTRAÇÃO faz handoff externo para Gemini', 'PASS' if illustration_ok and handoff_native_ok else 'FAIL',
    'O botão monta o prompt, copia/envia via Android e não chama geração interna.' if illustration_ok and handoff_native_ok else 'Handoff externo incompleto ou geração interna ainda presente.')
prompt_quality = all(x in scene_prompt for x in ['FATOS VISÍVEIS DA CENA', 'Não coloque texto', 'Composição horizontal 16:9'])
add('A07', 'Prompt visual usa fatos reais da cena', 'PASS' if prompt_quality else 'FAIL',
    'Prompt contém fatos visíveis, proibição de UI/texto e composição 16:9.' if prompt_quality else 'Contrato visual da cena está incompleto.')

# B. Visual asset coverage
weapon_files = list((R54ROOT / 'weapons').glob('*.webp')) if (R54ROOT / 'weapons').is_dir() else []
equipment_files = list((R54ROOT / 'equipment').glob('*.webp')) if (R54ROOT / 'equipment').is_dir() else []
portrait_files = list((R54ROOT / 'portraits').glob('*')) if (R54ROOT / 'portraits').is_dir() else []
map_files = list((R54ROOT / 'maps').glob('*')) if (R54ROOT / 'maps').is_dir() else []
metrics.update({'weapon_assets': len(weapon_files), 'equipment_assets': len(equipment_files), 'portrait_assets_r54': len(portrait_files), 'map_assets_r54': len(map_files)})
add('B01', 'Imagens de armas extraídas e ligadas', 'PASS' if len(weapon_files) >= 30 and 'r54EnhanceItemImages' in runtime else 'FAIL',
    f'{len(weapon_files)} imagens de armas no pack R54; binding runtime presente.')
add('B02', 'Imagens de equipamentos extraídas e ligadas', 'PASS' if len(equipment_files) >= 20 and 'r54EnhanceItemImages' in runtime else 'FAIL',
    f'{len(equipment_files)} imagens de equipamentos no pack R54; binding runtime presente.')

portrait_species_logic = 'twilek_female_blue.webp' in runtime and "species.includes('twi lek')" in runtime
portrait_status = 'WARN' if portrait_species_logic else 'FAIL'
add('B03', 'Retrato do personagem', portrait_status,
    f'R54 possui {len(portrait_files)} arquivo(s) de retrato e o binding atual cobre Twi’lek. A correção resolve Veya/Twi’lek, mas não é um sistema geral de retratos por espécie/personagem.')

# Existing larger manifest/map catalog vs R54 surfaced maps
manifest_path = VTT / 'image-library/manifest.json'
manifest_text = read(manifest_path)
manifest_map_refs = len(re.findall(r'"(?:subtype|mapKind)"\s*:\s*"(?:galaxy|settlement|planet|district|streets|interior|location|ship)"', manifest_text))
metrics['manifest_map_refs_approx'] = manifest_map_refs
preview_patterns = len(re.findall(r'/shantipole|/mos eisley|/tatooine', runtime, re.I))
add('B04', 'Mapas do acervo são realmente expostos pelo runtime', 'WARN' if manifest_map_refs > 3 else 'PASS',
    f'O manifesto contém aproximadamente {manifest_map_refs} referências de mapa, mas o preview R54 tem tratamento explícito apenas para Shantipole/Mos Eisley/Tatooine. A biblioteca existe; a exposição contextual ainda não é completa.')

# Start-point coordinate plumbing. R38 controller supports startMapPoint; R54 must forward it.
init_call_contexts = [m.group(0) for m in re.finditer(r"initializeLivingSandbox'\s*,\s*\{[^}]{0,350}\}", runtime)]
forwards_anchor = any('startMapPoint' in x for x in init_call_contexts)
controller_supports_anchor = 'startMapPoint' in controller
add('B05', 'Ponto inicial escolhido ancora a coordenada galáctica no mundo', 'PASS' if forwards_anchor and controller_supports_anchor else 'FAIL',
    'A UI repassa x/y para startMapPoint e o motor persiste a âncora.' if forwards_anchor and controller_supports_anchor else 'O motor suporta startMapPoint, mas o R54 não repassa as coordenadas x/y do local selecionado. Os 82 locais aparecem, porém a âncora galáctica pode ficar ausente.')

# Other VTT visual categories: R54 hotfix itself currently only binds weapons/equipment/one portrait/maps.
other_bindings = [x for x in ['vehicles', 'npcs', 'creatures', 'droids', 'scenery'] if f"'{x}'" in runtime or f'"{x}"' in runtime]
add('B06', 'Assets visuais completos do VTT (NPCs, criaturas, naves, cenário)', 'WARN',
    'O hotfix R54 recupera armas/equipamentos/retrato/mapas, mas não há binding equivalente comprovado no R54 para todas as categorias do acervo (NPCs, criaturas, droides, veículos e cenário).', other_bindings)

# C. Master Barbara / narration contract
start_body = function_body(controller, 'startLivingAdventure')
start_is_template = "const parts = [`A campanha começa em ${locationName}.`]" in start_body and "narration: parts.join(' ')" in start_body
add('C01', 'Abertura da aventura usa Mestre narrativo em vez de resumo-template', 'FAIL' if start_is_template else 'PASS',
    'A abertura atual é montada localmente com frases-resumo fixas (local + oportunidade + NPC + facção + liberdade de ação), exatamente o padrão curto visto no aparelho.' if start_is_template else 'A abertura não depende do resumo-template curto antigo.')

schema_max_only = 'até 5000 caracteres' in gemini_java
length_contract = bool(re.search(r'\b(?:300|400|500|600|700|800|900|1000|1100)\s*[–-]\s*\d+\s+palavras|\b\d+\s*(?:a|–|-)\s*\d+\s+parágrafos', shell + controller + gemini_java, re.I))
sensory_contract_terms = [t for t in ['sons', 'cheiros', 'temperatura', 'texturas', 'sensações', 'sensacoes'] if t in (shell + controller + gemini_java).casefold()]
add('C02', 'Contrato explícito de texto longo do Mestre', 'PASS' if length_contract else 'FAIL',
    'Existe meta explícita de extensão em palavras/parágrafos.' if length_contract else 'Não existe meta mínima de palavras/parágrafos no Star Wars. O schema apenas permite narração até 5000 caracteres.' if schema_max_only else 'Não foi encontrado contrato mínimo de extensão.')
add('C03', 'Contrato explícito de narração sensorial', 'PASS' if len(sensory_contract_terms) >= 3 else 'FAIL',
    'O prompt do Mestre exige múltiplos sentidos.' if len(sensory_contract_terms) >= 3 else 'Não foi encontrado contrato Star Wars exigindo sons, cheiros, temperatura, textura e presença física da cena.', sensory_contract_terms)

master_native_ok = all(x in shell + gemini_java for x in ['requestMasterProposal', 'braseiroGeminiNativeEvent'])
add('C04', 'Ponte do Mestre Gemini / Barbara', 'PASS' if master_native_ok else 'FAIL',
    'Request/retorno nativo do Mestre estão presentes.' if master_native_ok else 'Ponte nativa do Mestre incompleta.')

# D. Existing automated test suite snapshot
if NODE_TAP and NODE_TAP.is_file():
    tap = read(NODE_TAP)
    tests_m = re.search(r'^# tests\s+(\d+)', tap, re.M)
    pass_m = re.search(r'^# pass\s+(\d+)', tap, re.M)
    fail_m = re.search(r'^# fail\s+(\d+)', tap, re.M)
    fail_names = re.findall(r'^not ok\s+\d+\s+-\s+(.+)$', tap, re.M)
    node_tests = int(tests_m.group(1)) if tests_m else None
    node_pass = int(pass_m.group(1)) if pass_m else None
    node_fail = int(fail_m.group(1)) if fail_m else len(fail_names)
    metrics.update({'node_tests': node_tests, 'node_pass': node_pass, 'node_fail': node_fail, 'node_fail_names': fail_names[:30]})
    add('D01', 'Suite Node completa do projeto', 'PASS' if node_fail == 0 else 'FAIL',
        f'{node_tests if node_tests is not None else "?"} testes; {node_pass if node_pass is not None else "?"} passaram; {node_fail} falharam.', fail_names[:20])
else:
    add('D01', 'Suite Node completa do projeto', 'WARN', 'Log da suíte Node não foi fornecido ao auditor.')

if ANDROID_EXIT is not None:
    apk_candidates = list((ROOT / 'app/build/outputs/apk/debug').glob('*.apk'))
    metrics['android_exit'] = ANDROID_EXIT
    metrics['apk_candidates'] = [p.name for p in apk_candidates]
    add('D02', 'Compilação Android', 'PASS' if ANDROID_EXIT == 0 and apk_candidates else 'FAIL',
        f'Gradle exit={ANDROID_EXIT}; APKs encontrados={len(apk_candidates)}.')
else:
    add('D02', 'Compilação Android', 'WARN', 'Resultado da compilação Android não foi fornecido ao auditor.')

# E. Browser end-to-end on the actual patched R54 source
browser_details: dict[str, Any] = {}
base_url = 'http://127.0.0.1:8765/r46-shell.html'

try:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={'width': 415, 'height': 915})
        page = ctx.new_page()
        page_errors: list[str] = []
        page.on('pageerror', lambda e: page_errors.append(str(e)))
        page.add_init_script(r'''
          window.__auditIllustrationPrompt='';
          window.__auditMasterRequests=[];
          window.BraseiroGemini={
            getStatus:()=>JSON.stringify({configured:true,masterModel:'audit-master',model:'audit-image',ttsModel:'audit-tts'}),
            openGeminiWithPrompt:(p)=>{window.__auditIllustrationPrompt=String(p||'');return JSON.stringify({ok:true,target:'gemini_app',clipboard:true})},
            requestMasterProposal:(id,p)=>{window.__auditMasterRequests.push({id,p});return JSON.stringify({ok:true,status:'started',requestId:id})},
            configureModels:()=>JSON.stringify({ok:true}),pickSceneImage:()=>JSON.stringify({ok:true})
          };
          window.confirm=()=>true;
        ''')
        page.goto(base_url + '?screen=00', wait_until='networkidle', timeout=120000)
        page.wait_for_timeout(1200)
        overlay = page.locator('#braseiro-sandbox-setup')
        overlay_count = overlay.count()
        browser_details['first_boot_overlay'] = overlay_count
        if overlay_count:
            total_points = page.locator('#sandbox-list button[data-point]').count()
            browser_details['start_points_rendered'] = total_points
            search = page.locator('#sandbox-point-search')
            if search.count():
                search.fill('Shantipole')
                page.wait_for_timeout(120)
                browser_details['shantipole_visible_after_filter'] = page.locator('#sandbox-list button[data-point]:visible').count()
                search.fill('')
            sh = page.locator('#sandbox-list button[data-point]', has_text='Shantipole').first
            if sh.count(): sh.click()
            page.locator('#sandbox-start-button').click()
            page.wait_for_selector('#braseiro-sandbox-setup', state='detached', timeout=10000)
            page.wait_for_timeout(500)
        state = page.evaluate('window.BraseiroGameBridge.readState().data')
        browser_details['sandbox_start_anchor'] = (((state or {}).get('world') or {}).get('sandbox') or {}).get('startMapPoint')
        # Create character if necessary.
        if not (((state or {}).get('player') or {}).get('character')):
            rnd = page.locator('[data-random-character]')
            if rnd.count():
                rnd.click(); page.wait_for_timeout(900)
        state = page.evaluate('window.BraseiroGameBridge.readState().data')
        browser_details['character_created'] = bool((((state or {}).get('player') or {}).get('character')))
        start_btn = page.locator('[data-start-adventure]')
        if start_btn.count():
            start_btn.click(); page.wait_for_timeout(1200)
        state = page.evaluate('window.BraseiroGameBridge.readState().data')
        narration = str((((state or {}).get('master') or {}).get('narration')) or (((state or {}).get('scene') or {}).get('narration')) or '')
        browser_details['opening_narration_chars'] = len(narration)
        browser_details['opening_narration_words'] = words(narration)
        browser_details['opening_sensory_groups'] = sensory_hits(narration)
        (OUT / 'opening-narration.txt').write_text(narration, encoding='utf-8')

        # Illustration after overlay is gone / game exists.
        ill = page.locator('.final-scene-gemini button')
        if ill.count():
            ill.click(); page.wait_for_timeout(200)
        ill_prompt = page.evaluate('window.__auditIllustrationPrompt')
        browser_details['illustration_prompt_chars'] = len(str(ill_prompt or ''))
        browser_details['illustration_handoff_dynamic'] = bool(ill_prompt and 'FATOS VISÍVEIS DA CENA' in ill_prompt and 'Não coloque texto' in ill_prompt)

        # Navigation icon geometry in selected Scene state.
        icons = page.locator('.bottom-navigation .reference-icon-mask')
        boxes = [icons.nth(i).bounding_box() for i in range(min(5, icons.count()))]
        browser_details['nav_icon_boxes'] = boxes
        if boxes:
            ys = [round(b['y'], 2) for b in boxes if b]
            hs = [round(b['height'], 2) for b in boxes if b]
            browser_details['nav_icon_y_span'] = (max(ys) - min(ys)) if ys else None
            browser_details['nav_icon_height_span'] = (max(hs) - min(hs)) if hs else None

        # Character portrait.
        page.goto(base_url + '?screen=06', wait_until='networkidle', timeout=120000); page.wait_for_timeout(800)
        portraits = page.locator('.r54-character-portrait img')
        browser_details['character_portrait_count'] = portraits.count()
        if portraits.count():
            browser_details['character_portrait_loaded'] = portraits.first.evaluate('img=>img.complete&&img.naturalWidth>0')

        # Acquisition / catalog item visuals.
        page.goto(base_url + '?screen=09', wait_until='networkidle', timeout=120000); page.wait_for_timeout(1000)
        item_imgs = page.locator('.r54-item-visual img')
        browser_details['item_visual_count'] = item_imgs.count()
        loaded = 0
        for i in range(min(item_imgs.count(), 20)):
            try:
                if item_imgs.nth(i).evaluate('img=>img.complete&&img.naturalWidth>0'): loaded += 1
            except Exception:
                pass
        browser_details['item_visual_loaded_first20'] = loaded

        # Restart full dynamic path.
        page.goto(base_url + '?screen=00', wait_until='networkidle', timeout=120000); page.wait_for_timeout(700)
        restart = page.locator('button', has_text=re.compile('REINICIAR', re.I)).first
        browser_details['restart_button_found'] = restart.count()
        if restart.count():
            restart.click(); page.wait_for_timeout(1200)
            browser_details['restart_returned_to_setup'] = page.locator('#braseiro-sandbox-setup').count() == 1
        else:
            browser_details['restart_returned_to_setup'] = False

        # 360x800 basic no-crash / nav geometry check.
        ctx2 = browser.new_context(viewport={'width': 360, 'height': 800})
        p2 = ctx2.new_page(); errs2: list[str] = []; p2.on('pageerror', lambda e: errs2.append(str(e)))
        p2.goto(base_url + '?screen=00', wait_until='networkidle', timeout=120000); p2.wait_for_timeout(700)
        browser_details['360x800_nav_count'] = p2.locator('.bottom-nav-button').count()
        browser_details['360x800_errors'] = errs2
        ctx2.close()

        browser_details['page_errors'] = page_errors
        ctx.close(); browser.close()
except Exception as exc:
    browser_details['audit_exception'] = repr(exc)

metrics['browser'] = browser_details

# Browser findings
add('E01', 'Primeiro boot mostra todos os locais e busca funciona',
    'PASS' if browser_details.get('start_points_rendered') == 82 and browser_details.get('shantipole_visible_after_filter') == 1 else 'FAIL',
    f"renderizados={browser_details.get('start_points_rendered')}, filtro Shantipole={browser_details.get('shantipole_visible_after_filter')}")
add('E02', 'Âncora galáctica persiste após escolher ponto inicial',
    'PASS' if browser_details.get('sandbox_start_anchor') else 'FAIL',
    f"startMapPoint={browser_details.get('sandbox_start_anchor')}")

nw = int(browser_details.get('opening_narration_words') or 0)
ns = browser_details.get('opening_sensory_groups') or []
# This is intentionally a descriptive gate: the user's requirement is large/sensory; 250 words and >=3 sensory groups is a conservative floor.
add('E03', 'Abertura real é longa e sensorial', 'PASS' if nw >= 250 and len(ns) >= 3 else 'FAIL',
    f'Abertura medida: {nw} palavras, {browser_details.get("opening_narration_chars",0)} caracteres, grupos sensoriais={ns}. Critério conservador de auditoria: >=250 palavras e >=3 grupos sensoriais.')
add('E04', 'Handoff de ilustração funciona depois de iniciar o jogo', 'PASS' if browser_details.get('illustration_handoff_dynamic') else 'FAIL',
    f"prompt chars={browser_details.get('illustration_prompt_chars')}")
nav_ok_dynamic = (browser_details.get('nav_icon_y_span') is not None and browser_details.get('nav_icon_y_span') <= 2.0 and browser_details.get('nav_icon_height_span') is not None and browser_details.get('nav_icon_height_span') <= 1.0)
add('E05', 'Ícones inferiores ficam nivelados em runtime', 'PASS' if nav_ok_dynamic else 'FAIL',
    f"y-span={browser_details.get('nav_icon_y_span')}, height-span={browser_details.get('nav_icon_height_span')}")
add('E06', 'Retrato do personagem carrega em runtime', 'PASS' if browser_details.get('character_portrait_count',0) >= 1 and browser_details.get('character_portrait_loaded') else 'FAIL',
    f"count={browser_details.get('character_portrait_count')}, loaded={browser_details.get('character_portrait_loaded')}")
add('E07', 'Imagens de itens aparecem e carregam em runtime', 'PASS' if browser_details.get('item_visual_count',0) >= 1 and browser_details.get('item_visual_loaded_first20',0) >= 1 else 'FAIL',
    f"count={browser_details.get('item_visual_count')}, loaded first20={browser_details.get('item_visual_loaded_first20')}")
add('E08', 'REINICIAR funciona de ponta a ponta', 'PASS' if browser_details.get('restart_returned_to_setup') else 'FAIL',
    f"button={browser_details.get('restart_button_found')}, returned_to_setup={browser_details.get('restart_returned_to_setup')}")
add('E09', 'Sem erros JS nos viewports móveis', 'PASS' if not browser_details.get('page_errors') and not browser_details.get('360x800_errors') and not browser_details.get('audit_exception') else 'FAIL',
    f"415x915={browser_details.get('page_errors')}; 360x800={browser_details.get('360x800_errors')}; exception={browser_details.get('audit_exception')}")

# Summaries
counts = {s: sum(1 for f in findings if f['status'] == s) for s in ['PASS','WARN','FAIL']}
report = []
report.append('# STAR WARS — EDGE OF THE GALAXY — AUDITORIA COMPLETA R54')
report.append('')
report.append('Auditoria executada sobre o projeto R53 reconstruído + hotfix R54 atual, incluindo leitura de código, suíte Node, compilação Android (quando fornecida) e fluxo Playwright mobile.')
report.append('')
report.append(f"## Resultado\n\n- PASS: **{counts['PASS']}**\n- WARN: **{counts['WARN']}**\n- FAIL: **{counts['FAIL']}**")
report.append('')
report.append('## Achados')
report.append('')
for f in findings:
    report.append(f"### {f['status']} — {f['id']} — {f['title']}")
    report.append('')
    report.append(f['detail'])
    if f.get('evidence') not in (None, [], ''):
        ev = json.dumps(f['evidence'], ensure_ascii=False, indent=2)
        if len(ev) > 4000: ev = ev[:4000] + '\n…'
        report.append('')
        report.append('```json')
        report.append(ev)
        report.append('```')
    report.append('')
report.append('## Métricas')
report.append('')
report.append('```json')
report.append(json.dumps(metrics, ensure_ascii=False, indent=2))
report.append('```')
report.append('')
report.append('## Critério narrativo')
report.append('')
report.append('O requisito do usuário é narração grande e sensorial. Para o teste dinâmico foi usado um piso conservador de 250 palavras e pelo menos 3 grupos sensoriais (som, cheiro, temperatura, tato/textura, luz/visão). Mais importante que esse piso: a auditoria também verifica se existe contrato explícito no prompt/bridge; atualmente o schema aceitar até 5000 caracteres não equivale a exigir texto longo.')

(OUT / 'FULL_AUDIT.md').write_text('\n'.join(report), encoding='utf-8')
(OUT / 'FULL_AUDIT.json').write_text(json.dumps({'counts':counts,'findings':findings,'metrics':metrics}, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'counts':counts,'report':str(OUT/'FULL_AUDIT.md')},ensure_ascii=False))
