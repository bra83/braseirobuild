import json,os,hashlib
from pathlib import Path
from playwright.sync_api import sync_playwright

IDS=['path-of-truth','remote-vision','visions-of-past','divination','intuition','mountain-voice','speak-dead']
origin=os.getenv('FL_E2E_ORIGIN','http://127.0.0.1:4173')
question='Qual decisão é a mais sábia segundo os fatos conhecidos desta campanha?'
rows=[]
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True)
    context=browser.new_context(viewport={'width':415,'height':915})
    page=context.new_page()
    for spell in IDS:
        page.goto(origin+'/e2e/u27-gm-browser.html?spell='+spell)
        page.wait_for_function('window.u27Ready === true')
        page.get_by_role('button',name='Pedir resposta ao Mestre IA',exact=True).wait_for()
        page.get_by_role('textbox',name='Pergunta para o Mestre IA').fill(question)
        page.get_by_role('button',name='Pedir resposta ao Mestre IA',exact=True).click()
        page.get_by_text('resposta registrada',exact=False).wait_for()
        page.reload();page.wait_for_function('window.u27Ready === true')
        page.get_by_text('resposta registrada',exact=False).wait_for()
        assert page.get_by_role('button',name='Pedir resposta ao Mestre IA',exact=True).count()==0
        state=page.evaluate('''async spell => {
          const request=indexedDB.open('braseiro-db');
          const db=await new Promise((resolve,reject)=>{request.onsuccess=()=>resolve(request.result);request.onerror=()=>reject(request.error)});
          const tx=db.transaction(['app_meta','characters','world_state'],'readonly');
          const read=(store,id)=>new Promise((resolve,reject)=>{const r=tx.objectStore(store).get(id);r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error)});
          const id='u27-browser-'+spell;
          const [character,record,world]=await Promise.all([read('characters',id),read('app_meta','magic-casts:'+id),read('world_state','u27-browser-gm')]);
          db.close();return {willpower:character.willpowerPoints,strength:character.attributes.strength.current,
            receipts:record.value.events.length,event:record.value.events[0],worldId:world.id};
        }''',spell)
        assert state['willpower']==4 and state['strength']==3 and state['receipts']==1
        assert state['event']['gmDecision']['question']==question
        assert state['event']['gmDecision']['provider']=='gemini'
        assert 'spell-effect-requires-gm' not in state['event']['pendingConsequences']
        rows.append({'spellId':spell,'status':'PASS','reload':True,'willpower':4,'receiptCount':1})
    Path('e2e-results').mkdir(exist_ok=True)
    page.screenshot(path='e2e-results/u27-gm-browser.png')
    browser.close()
report={'status':'PASS','browserExecuted':True,'expected':7,'passed':7,'cases':rows,
        'scope':'Actual React UI and Chromium IndexedDB: production casting -> click AI answer -> receipt -> reload. Gemini transport explicitly canned, not live API or Android.'}
Path('e2e-results/u27-gm-browser.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('U27_G02_CHROMIUM_MAGIC_RECEIPTS PASS 7/7 real UI + IndexedDB + reload; canned Gemini transport')
