#!/usr/bin/env python3
from pathlib import Path
import sys,json
root=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path.cwd()
vtt=root/'app/src/main/assets/vtt'
ui=vtt/'js/core/uiBridge.bundle.js'; rt=vtt/'r46/final-runtime.js'; shell=vtt/'r46-shell.html'
engine=vtt/'adventures/under-a-black-sun-engine.js'; pack=vtt/'adventures/under-a-black-sun.pt-BR.json'
errors=[]
for p in (ui,rt,shell,engine,pack):
    if not p.is_file(): errors.append('ausente: '+str(p))
if not errors:
    controller=(vtt/'js/core/controller.mjs').read_text(encoding='utf-8',errors='replace')
    u=ui.read_text(encoding='utf-8',errors='replace')
    r=rt.read_text(encoding='utf-8',errors='replace')
    h=shell.read_text(encoding='utf-8',errors='replace')
    e=engine.read_text(encoding='utf-8',errors='replace')
    # Gate the concrete P28 published-session contract instead of depending on
    # incidental method ordering inside the bundled BraseiroUiBridge class.
    published_bridge_ok=all(token in u for token in (
        'R54_P28_PUBLISHED_SESSION',
        'startPublishedAdventureSession(options = {})',
        'this._controllerForExistingCampaign()',
        "game.commit('SCENE_UPDATED'",
        'publishedAnchorId',
        'startLivingAdventure(options = {})'
    ))
    checks={
      'ui marcador P28':'R54_P28_MIXED_CREATION' in u,
      'controller random por origem':'R54_P28_RANDOM_CREATION' in controller and 'createRandomAorCoreCharacter' in controller and 'createRandomEoteCoreCharacter' in controller,
      'MIXED duas origens':"mixedProfiles:['EOTE_CORE','AOR_CORE']" in u,
      'espécies EotE prefixadas':"mapSpecies(eoteCharacterCreation_mjs_1.EOTE_SPECIES,'EOTE_CORE','EotE')" in u,
      'espécies AoR prefixadas':"mapSpecies(characterCreation_mjs_1.AOR_SPECIES,'AOR_CORE','AoR')" in u,
      'Duty usa criador AoR':'game.createAorCharacter(mapped)' in u,
      'Obligation usa criador EotE':'game.createCustomCharacter(native)' in u,
      'não empilha economia':"origin==='AOR_CORE'" in u and "eoteCharacterCreation_mjs_1.createEoteCoreCharacter(native)" in u,
      'runtime engine P28':'R54_P28_BLACK_SUN_ENGINE' in r,
      'UI simultânea de vínculos':'R54_P28_BONDS_UI' in r and 'Obrigação total:' in r and 'Dever total:' in r and 'Faixa de Contribuição:' in r,
      'random creation MIXED':'Escolha a origem da ficha aleatória:' in r,
      'runtime contexto por nó':'[NO:${ctx.id}]' in r,
      'runtime anti-spoiler':'Não revele nós futuros' in r,
      'world anchor Coruscant':"restartSandboxPreservingParty" in r and "startingPoint:'Coruscant'" in r and "startLocationId:'coruscant'" in r and 'startMapPoint:{x:53.4,y:51.2}' in r and 'worldAnchored' in r and "worldAnchor:'Coruscant'" in r,
      'published session sem opening genérica':published_bridge_ok and "bridgeResult('startPublishedAdventureSession'" in r and 'sessionStarted' in r,
      'painel continuar':'Continuar com Barbara' in r,
      'painel concluir':'Marcar etapa concluída' in r,
      'sem literal slash-n em handlers':"startBarbaraOpening());\\\\n panel.querySelector" not in r,
      'script engine carregado':'<script src="adventures/under-a-black-sun-engine.js"></script>' in h,
      'engine persistência':'braseiro.starwars.adventure.under-black-sun.v2' in e,
      'engine pista Setor 943':"s.completedNodes.includes('spyder')" in e,
      'engine fim':"id==='showdown'" in e,
    }
    errors += [name for name,ok in checks.items() if not ok]
    data=json.loads(pack.read_text(encoding='utf-8'))
    if data.get('id')!='under-a-black-sun': errors.append('pack id incorreto')
    if data.get('rules',{}).get('profile')!='EOTE': errors.append('pack profile não EOTE')
    if data.get('rules',{}).get('aorPureModeAllowed') is not False: errors.append('AoR puro não bloqueado no pack')
if errors:
    print('FAIL: P28')
    for x in errors: print('- '+x)
    raise SystemExit(1)
print('PASS: P28 MIXED native creation + persistent Under a Black Sun engine')