from pathlib import Path
import base64,json,re
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
WWW=ROOT/'app/src/main/assets/www'

def data_url(rel):
    p=(WWW/rel).resolve(); return 'data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode('ascii')

def build_html():
    html=(WWW/'scr015.html').read_text(encoding='utf-8')
    css=(WWW/'scr015.css').read_text(encoding='utf-8')
    js=(WWW/'scr015.js').read_text(encoding='utf-8')
    geom=json.loads((WWW/'data/scr015.geometry.json').read_text(encoding='utf-8'))
    fixture=json.loads((WWW/'data/scr015.fixture.json').read_text(encoding='utf-8'))
    derived=json.loads((WWW/'data/scr015.derived-assets.json').read_text(encoding='utf-8'))
    scene_rules=json.loads((WWW/'data/scr015.rules.json').read_text(encoding='utf-8'))
    wave3_character=json.loads((WWW/'data/wave3.core-character.fixture.json').read_text(encoding='utf-8'))
    vg=(WWW/'core/visual-geometry.js').read_text(encoding='utf-8').replace('export function ','function ').replace('export const ','const ')
    ar=(WWW/'core/asset-resolver.js').read_text(encoding='utf-8').replace('export class ','class ')
    sn=(WWW/'core/shared-navigation.js').read_text(encoding='utf-8').replace('export const ','const ').replace('export function ','function ')
    sn=sn.replace("'assets/shared_chrome/bottom_nav_reference_415x86.png'",repr(data_url('assets/shared_chrome/bottom_nav_reference_415x86.png')))
    # inline all asset paths used by manifests or implementation source
    paths={r['path'] for r in derived}
    paths |= set(re.findall(r"['\"]\./(assets/[^'\"]+\.png)['\"]",js))
    urlmap={rel:data_url(rel) for rel in paths}
    for r in derived: r['path']=urlmap[r['path']]
    for rel,url in urlmap.items():
        js=js.replace(f"'./{rel}'",repr(url)).replace(f'"./{rel}"',json.dumps(url))
    js=js.replace("`./${record.path}`", "record.path")
    js=re.sub(r"^import\s+.*?;\s*$",'',js,flags=re.M)
    js=re.sub(r"const \[geometry, fixture, derived, sceneRules, wave3Character\] = await Promise\.all\(\[.*?\]\);",'const geometry=window.__GEOMETRY; const fixture=window.__FIXTURE; const derived=window.__DERIVED; const sceneRules=window.__SCENE_RULES; const wave3Character=window.__WAVE3_CHARACTER;',js,flags=re.S)
    body=re.search(r'<body>(.*?)</body>',html,flags=re.S).group(1)
    body=re.sub(r'<script[^>]*src="scr015.js"[^>]*></script>','',body)
    return f'''<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>{css}</style></head><body>{body}<script>
window.__events=[]; window.addEventListener('braseiro-intent',e=>window.__events.push(e.detail));
window.__GEOMETRY={json.dumps(geom,ensure_ascii=False)}; window.__FIXTURE={json.dumps(fixture,ensure_ascii=False)}; window.__DERIVED={json.dumps(derived,ensure_ascii=False)}; window.__SCENE_RULES={json.dumps(scene_rules,ensure_ascii=False)}; window.__WAVE3_CHARACTER={json.dumps(wave3_character,ensure_ascii=False)};
function createWave3Runtime(){{return {{status:'QA_HARNESS',actionService:null,campaignId:null,persistence:null}};}}
class CombatSessionService {{}}
class DeterministicNarrativeProvider {{}}
function createPhysicalDiceClient(){{return {{available:()=>false,roll:async()=>({{status:'UNAVAILABLE'}})}};}}
function createAudioSessionCoordinator(){{return {{sync:()=>({{status:'QA_NOOP'}})}};}}
{vg}\n{ar}\n{sn}\n{js}</script></body></html>''',geom

def main():
    page_html,geom=build_html(); results={}
    with sync_playwright() as pw:
        browser=pw.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
        ctx=browser.new_context(viewport={'width':415,'height':915},device_scale_factor=1,has_touch=True,is_mobile=True)
        page=ctx.new_page(); page.set_content(page_html,wait_until='load')
        page.wait_for_function("document.fonts ? document.fonts.status === 'loaded' : true")
        page.wait_for_function("Array.from(document.images).every(i=>i.complete && i.naturalWidth>0)")
        # geometry + viewport
        runtime=page.evaluate('''() => ({w:innerWidth,h:innerHeight,sw:document.documentElement.scrollWidth,sh:document.documentElement.scrollHeight,layers:[...document.querySelectorAll('#screen > [id]')].map(e=>{let r=e.getBoundingClientRect();return {id:e.id,x:r.x,y:r.y,w:r.width,h:r.height}})})''')
        expected={l['stable_id']:l['geometry'] for l in geom['layers']}; errors=[]
        for a in runtime['layers']:
            g=expected[a['id']]
            for k in ('x','y','w','h'):
                if abs(a[k]-g[k])>0.01: errors.append(f"{a['id']}.{k}:{a[k]}!={g[k]}")
        results['viewport_415x915']=(runtime['w'],runtime['h'])==(415,915)
        results['document_no_outer_scroll']=runtime['sw']<=415 and runtime['sh']<=915
        results['geometry_27_exact']=len(runtime['layers'])==27 and not errors
        results['geometry_errors']=errors
        results['images_loaded']=page.evaluate("Array.from(document.images).every(i=>i.complete && i.naturalWidth>0)")
        results['utf8_accents']=page.locator('#HEX_ENCOUNTER_RUINAS_08').inner_text().startswith('RUÍNAS') and 'DISTÂNCIA' in page.locator('#HEX_ENCOUNTER_RUINAS_11').inner_text()
        results['one_active_nav']=page.locator('.nav-item.active').count()==1 and page.locator('#HEX_ENCOUNTER_RUINAS_24').get_attribute('class').find('active')>=0
        # real touch/tap on all player-action buttons
        for sid in ['16','17','18','19','20']:
            page.locator(f'#HEX_ENCOUNTER_RUINAS_{sid}').tap()
        events=page.evaluate('window.__events')
        results['touch_player_actions']=len([e for e in events if e['type']=='PLAYER_ACTION_INTENT'])==5
        results['player_action_intents']=[e.get('intent') for e in events if e['type']=='PLAYER_ACTION_INTENT']
        # Top UI and route touch
        page.locator('#HEX_ENCOUNTER_RUINAS_05').tap(); page.locator('#HEX_ENCOUNTER_RUINAS_24').tap()
        events=page.evaluate('window.__events')
        results['touch_top_action']=any(e.get('action')=='open_notes' for e in events)
        results['touch_route']=any(e.get('route')=='nav_explore' for e in events)
        # open/collapsed state
        page.evaluate("document.querySelector('#HEX_ENCOUNTER_RUINAS_21').classList.remove('collapsed')")
        note_btn=page.locator('#HEX_ENCOUNTER_RUINAS_22')
        note_btn.tap(); results['notes_collapsed']=page.locator('#HEX_ENCOUNTER_RUINAS_21').evaluate("e=>e.classList.contains('collapsed')")
        note_btn.tap(); results['notes_reopened']=not page.locator('#HEX_ENCOUNTER_RUINAS_21').evaluate("e=>e.classList.contains('collapsed')")
        # long text scrollability without changing outer layout
        results['long_narrative_scroll']=page.locator('#HEX_ENCOUNTER_RUINAS_14').evaluate("e=>{e.textContent=(e.textContent+' ' ).repeat(8); return e.scrollHeight>e.clientHeight && (e.scrollTop=e.scrollHeight, e.scrollTop>0)}")
        results['long_notes_scroll']=page.locator('#HEX_ENCOUNTER_RUINAS_21').evaluate("e=>{let p=e.querySelector('p');p.textContent=(p.textContent+' ' ).repeat(10); return e.scrollHeight>e.clientHeight && (e.scrollTop=e.scrollHeight, e.scrollTop>0)}")
        # keyboard/IME applicability: this screen intentionally has no editable control
        results['editable_controls']=page.locator('input,textarea,[contenteditable="true"]').count()
        results['keyboard_ime']='N/A_NO_EDITABLE_CONTROL_ON_SCR015'
        browser.close()
    out=ROOT/'docs/wave2/SCR015_RUNTIME_QA.json'; out.write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    failed=[k for k,v in results.items() if isinstance(v,bool) and not v]
    print(json.dumps(results,ensure_ascii=False,indent=2)); print('QA_PASS' if not failed else 'QA_FAIL',failed)
    raise SystemExit(1 if failed else 0)
if __name__=='__main__': main()
