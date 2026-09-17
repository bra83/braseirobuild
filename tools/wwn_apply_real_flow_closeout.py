from pathlib import Path


def rep(path, old, new, label):
    p = Path(path)
    s = p.read_text(encoding='utf-8')
    if new in s:
        return
    if old not in s:
        raise SystemExit(f'{label}: pattern not found')
    p.write_text(s.replace(old, new, 1), encoding='utf-8')


# Jornada: make fixes canonical rather than relying on DOM hotfixes.
p = Path('app/src/main/assets/www/jornada.js')
s = p.read_text(encoding='utf-8')
old = "import { PlayerMechanicsFacade } from './core/player-mechanics-facade.js';"
new = old + "\nimport { skillLabelPtBr } from './core/ptbr-labels.js';\nimport { selectJourneyFeed, knownJourneyLocations } from './core/journey-discovery.js';\nimport { attachSceneImageToCampaign, storePendingSceneImageForCampaign, removeSceneImageFromCampaign, pendingSceneImageFromState } from './core/scene-image-state-service.js';"
if "scene-image-state-service.js" not in s:
    if old not in s: raise SystemExit('jornada imports: pattern not found')
    s = s.replace(old, new, 1)

old = "function renderBars(){const m=activeCharacter();if(!m)return;const b=m.statusBars??m;$('statusName').textContent=m.name;$('hpFill').style.width=`${Math.round((b?.hp?.fill??0)*100)}%`;$('strainFill').style.width=`${Math.round((b?.systemStrain?.fill??0)*100)}%`;$('hpText').textContent=b?.hp?`${b.hp.current} / ${b.hp.max}`:'—';$('strainText').textContent=b?.systemStrain?`${b.systemStrain.current} / ${b.systemStrain.max}`:'—'}"
new = "function renderBars(){const m=activeCharacter();if(!m){$('statusName').textContent='SEM PERSONAGEM';$('hpFill').style.width='0%';$('strainFill').style.width='0%';$('hpText').textContent='—';$('strainText').textContent='—';return;}const b=m.statusBars??m;$('statusName').textContent=m.name;$('hpFill').style.width=`${Math.round((b?.hp?.fill??0)*100)}%`;$('strainFill').style.width=`${Math.round((b?.systemStrain?.fill??0)*100)}%`;$('hpText').textContent=b?.hp?`${b.hp.current} / ${b.hp.max}`:'—';$('strainText').textContent=b?.systemStrain?`${b.systemStrain.current} / ${b.systemStrain.max}`:'—'}"
if "$('statusName').textContent='SEM PERSONAGEM'" not in s:
    if old not in s: raise SystemExit('renderBars: pattern not found')
    s = s.replace(old, new, 1)

old = "label:`${k}${known.has(k)?` ${c.skills[k]>=0?`+${c.skills[k]}`:c.skills[k]}`:' (não treinado)'}`"
new = "label:`${skillLabelPtBr(k)}${known.has(k)?` ${c.skills[k]>=0?`+${c.skills[k]}`:c.skills[k]}`:' (não treinado)'}`"
if 'skillLabelPtBr(k)' not in s:
    if old not in s: raise SystemExit('skill label: pattern not found')
    s = s.replace(old, new, 1)

old = "const starterHooks=content?.startingHooks??[],campaignRevision=Number(currentCanonicalState()?.revision??0),hookHost=$('hooks');if(starterHooks.length){const visible=Math.min(2,starterHooks.length),start=((campaignRevision%starterHooks.length)+starterHooks.length)%starterHooks.length;for(let i=0;i<visible;i++)hookHost.appendChild(card(starterHooks[(start+i)%starterHooks.length],'startingHook'));}const rumorPool=Array.isArray(pack?.rumors)?pack.rumors:[];if(rumorPool.length)hookHost.appendChild(rumorCard(rumorPool[((campaignRevision*3+1)%rumorPool.length+rumorPool.length)%rumorPool.length]));for(const s of content?.majorSites??[])$('sites').appendChild(card(s,'majorSite'));"
new = "const journeyState=currentCanonicalState(),hookHost=$('hooks');const journeyFeed=selectJourneyFeed(journeyState,{startingHooks:content?.startingHooks??[],rumors:Array.isArray(pack?.rumors)?pack.rumors:[]},{hookCount:2,rumorCount:1});for(const hook of journeyFeed.hooks)hookHost.appendChild(card(hook,'startingHook'));for(const rumor of journeyFeed.rumors)hookHost.appendChild(rumorCard(rumor));const knownLocations=knownJourneyLocations(journeyState,{majorSites:content?.majorSites??[],minorPois:content?.minorPois??[]});for(const site of knownLocations.majorSites)$('sites').appendChild(card(site,'majorSite'));"
if 'knownJourneyLocations(journeyState' not in s:
    if old not in s: raise SystemExit('dynamic opportunities: pattern not found')
    s = s.replace(old, new, 1)

if 'attachSceneImageToCampaign({persistence,campaignId,result' not in s:
    start = s.find('function attachSceneImageResult(')
    end = s.find("\n$('sceneMasterButton').onclick", start)
    if start < 0 or end < 0: raise SystemExit('attachSceneImageResult block not found')
    replacement = """function attachSceneImageResult(result,{generated=false,interactionId=null,model=null}={}){const attached=attachSceneImageToCampaign({persistence,campaignId,result,generated,interactionId,model});if(attached.status==='OK'){loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(loaded.narrative);}return attached}\nfunction pendingSceneImage(){return pendingSceneImageFromState(currentCanonicalState())}\nfunction removeCurrentSceneImage(){const removed=removeSceneImageFromCampaign({persistence,campaignId});if(removed.status==='OK'){loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(loaded.narrative);}return removed}\n"""
    s = s[:start] + replacement + s[end:]

old = "async function openScene(contentType,contentId){const result=await scenes.openContentScene({campaignId,contentType,contentId});if(!['OK','SCENE_ALREADY_OPEN_NO_MUTATION'].includes(result.status))return;loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(result.narrative??loaded.narrative);const deferred=globalThis.__BRASEIRO_DEFERRED_SCENE_IMAGE_RESULT??null;if(deferred){const attached=attachSceneImageResult(deferred,{generated:false});if(attached.status==='OK'){globalThis.__BRASEIRO_DEFERRED_SCENE_IMAGE_RESULT=null;$('sceneImageStatus').textContent='Imagem recebida anteriormente e vinculada a esta cena.';}}syncAudio();document.querySelector('.content').scrollTo({top:0,behavior:'smooth'})}"
new = "async function openScene(contentType,contentId){const result=await scenes.openContentScene({campaignId,contentType,contentId});if(!['OK','SCENE_ALREADY_OPEN_NO_MUTATION'].includes(result.status))return;loaded=scenes.load({campaignId});projection=loaded.projection;renderScene(result.narrative??loaded.narrative);const deferred=pendingSceneImage()??globalThis.__BRASEIRO_DEFERRED_SCENE_IMAGE_RESULT??null;if(deferred){const attached=attachSceneImageResult(deferred,{generated:false});if(attached.status==='OK'){globalThis.__BRASEIRO_DEFERRED_SCENE_IMAGE_RESULT=null;$('sceneImageStatus').textContent='Imagem recebida anteriormente e vinculada a esta cena.';}}syncAudio();document.querySelector('.content').scrollTo({top:0,behavior:'smooth'})}"
if 'const deferred=pendingSceneImage()' not in s:
    if old not in s: raise SystemExit('openScene pending image: pattern not found')
    s = s.replace(old, new, 1)

old = "sceneImages.onImport(result=>{if(result.status!=='OK'){$('sceneImageStatus').textContent=result.status==='CANCELLED'?'Importação cancelada.':`Imagem não importada: ${result.reason??result.status}`;return;}if(!projection.scene||projection.scene.status!=='OK'){globalThis.__BRASEIRO_DEFERRED_SCENE_IMAGE_RESULT=result;$('sceneImageStatus').textContent='Imagem recebida. Abra uma oportunidade ou local; ela será vinculada à próxima cena.';return;}const attached=attachSceneImageResult(result,{generated:false});$('sceneImageStatus').textContent=attached.status==='OK'?'Imagem importada e vinculada à cena atual.':`Imagem importada, mas não vinculada: ${attached.status}`;});"
new = "sceneImages.onImport(result=>{if(result.status!=='OK'){$('sceneImageStatus').textContent=result.status==='CANCELLED'?'Importação cancelada.':`Imagem não importada: ${result.reason??result.status}`;return;}if(!projection.scene||projection.scene.status!=='OK'){const stored=storePendingSceneImageForCampaign({persistence,campaignId,result});if(stored.status==='OK'){globalThis.__BRASEIRO_DEFERRED_SCENE_IMAGE_RESULT=result;$('sceneImageStatus').textContent='Imagem recebida e guardada nesta campanha. Abra uma oportunidade ou local; ela será vinculada à próxima cena.';}else $('sceneImageStatus').textContent=`Imagem recebida, mas não persistida: ${stored.status}`;return;}const attached=attachSceneImageResult(result,{generated:false});$('sceneImageStatus').textContent=attached.status==='OK'?'Imagem importada e vinculada à cena atual.':`Imagem importada, mas não vinculada: ${attached.status}`;});"
if 'storePendingSceneImageForCampaign({persistence,campaignId,result})' not in s:
    if old not in s: raise SystemExit('scene import persistence: pattern not found')
    s = s.replace(old, new, 1)

old = "$('sceneImageButton').onclick=()=>{const r=sceneImages.requestImport();$('sceneImageStatus').textContent=r.status==='PENDING'?'Escolha uma imagem local para esta cena.':`Importação indisponível: ${r.reason??r.status}`};"
new = old + "\n$('sceneRemoveImageButton').onclick=()=>{const removed=removeCurrentSceneImage();$('sceneImageStatus').textContent=removed.status==='OK'?'Imagem removida desta cena.':removed.status==='NO_SCENE_IMAGE_NO_MUTATION'?'Esta cena não possui imagem para remover.':`Não foi possível remover a imagem: ${removed.status}`};"
if "$('sceneRemoveImageButton').onclick" not in s:
    if old not in s: raise SystemExit('scene remove button handler: pattern not found')
    s = s.replace(old, new, 1)

old = "renderBars();renderScene(loaded.narrative);renderMasterStatus();syncAudio();setupMechanicsUi();window.__BRASEIRO_JORNADA__={campaignId,projection:()=>structuredClone(projection),mechanicsSummary:()=>structuredClone(mechanicsSummary),refreshMechanics,openScene};"
new = "renderBars();renderScene(loaded.narrative);const bootPending=pendingSceneImage();if(bootPending&&projection.scene?.status==='OK'){const attached=attachSceneImageResult(bootPending,{generated:false});if(attached.status==='OK')$('sceneImageStatus').textContent='Imagem pendente restaurada e vinculada à cena atual.';}renderMasterStatus();syncAudio();setupMechanicsUi();window.__BRASEIRO_JORNADA__={campaignId,projection:()=>structuredClone(projection),mechanicsSummary:()=>structuredClone(mechanicsSummary),refreshMechanics,openScene};"
if 'const bootPending=pendingSceneImage()' not in s:
    if old not in s: raise SystemExit('boot pending image: pattern not found')
    s = s.replace(old, new, 1)
p.write_text(s, encoding='utf-8')

rep('app/src/main/assets/www/jornada.html', '<button id="sceneImageButton">IMPORTAR IMAGEM</button></div>', '<button id="sceneImageButton">IMPORTAR / SUBSTITUIR IMAGEM</button><button id="sceneRemoveImageButton">REMOVER IMAGEM</button></div>', 'scene image controls')
rep('app/src/main/assets/www/scr014.js', "label.textContent=String(rec.poiName??'POI').slice(0,18);", "label.textContent=String(rec.poiName??'POI');", 'POI full label')

css = Path('app/src/main/assets/www/scr014.css')
ct = css.read_text(encoding='utf-8')
marker = '/* WWN_REAL_FLOW_POI_HORIZONTAL_V1 */'
if marker not in ct:
    ct += "\n" + marker + "\n.poi-structure.poi-symbol{left:5px;top:37px;width:82px;height:auto;min-height:20px;overflow:visible;clip-path:none;filter:drop-shadow(0 2px 3px #000c);border:0;background:transparent}.poi-symbol>i{display:block;width:82px;max-width:82px;padding:3px 4px;border:1px solid #b18a4f;background:#08100fea;color:#f0d7a1;font:700 7px/1.05 Arial,sans-serif;font-style:normal;text-align:center;transform:none;white-space:normal;overflow:visible;text-overflow:clip;overflow-wrap:anywhere}\n"
    css.write_text(ct, encoding='utf-8')

# Make offline major scenes substantive without inventing hidden content.
rp = Path('app/src/main/assets/www/core/runtime-narrative-provider.js')
rs = rp.read_text(encoding='utf-8')
anchor = 'export class RuntimeBarbaraProvider{'
if 'function majorLocalFallback(' not in rs:
    helper = r'''function majorLocalFallback(mechanicalEvent,state){
  const scene=mechanicalEvent?.scene??{};const title=String(scene.title??'CENA').trim();const summary=String(scene.summary??'').trim();const situation=String(scene.situation??scene.premise??'').trim();const leads=(scene.leads??[]).map(v=>String(v??'').trim()).filter(Boolean);const pos=state?.worldState?.position??{};const where=String(pos.name??'').trim();
  const facts=[summary,situation,...leads].filter(Boolean);if(!facts.length)return null;const core=facts.join(' ');const place=where?`A posição registrada do grupo é ${where}. `:'';
  const paragraphs=[`${title}. ${core}`,`${place}A cena se abre sem antecipar o que está oculto. O que o personagem pode tomar como certo é apenas o que já está exposto pelos fatos acima; qualquer ameaça, intenção, passagem secreta, criatura, recurso ou consequência que não esteja presente nesse contexto continua desconhecida.`,`Antes de qualquer resolução mecânica, o Mestre mantém o foco no presente: ${summary||core} Isso é situação, não resultado. Nenhum teste foi rolado por este texto, nenhum recurso foi gasto, nenhuma posição mudou e nenhuma decisão foi tomada pelo personagem.`,situation?`A tensão imediata vem desta situação pública: ${situation} Ela permanece aberta. O personagem pode observar, conversar, recuar, investigar, viajar ou declarar outra intenção; quando alguma dessas escolhas exigir risco ou regra, a resolução mecânica deve acontecer antes de a ficção afirmar sucesso ou falha.`:`A situação ainda não determina um caminho obrigatório. O personagem pode declarar livremente o que tenta fazer; se a intenção exigir risco ou regra, a resolução mecânica deve acontecer antes de a ficção afirmar sucesso ou falha.`,leads.length?`Entre os elementos já perceptíveis que podem orientar uma decisão estão: ${leads.join('; ')}. Eles são possibilidades, não missões obrigatórias, e podem ser ignorados.`:`Não há uma missão obrigatória embutida nesta abertura. O mundo permanece disponível para exploração e a próxima direção depende da declaração do jogador.`,`Sem uma resposta do modelo narrativo, Barbara local não acrescenta clima, pessoas, objetos ou lore que o estado não forneceu. Em vez de preencher lacunas com invenção, ela preserva o quadro público e deixa espaço para que a próxima ação revele mais do mundo por meios canônicos.`,`O instante fica suspenso nesse ponto: ${core} O que acontece em seguida ainda não foi decidido. Diga o que seu personagem faz, pergunta, procura ou tenta alcançar; a campanha continua a partir dessa escolha, mantendo separados descrição, segredo do Mestre e resultado mecânico.`];return paragraphs.join('\n\n');
}

'''
    if anchor not in rs: raise SystemExit('narrative class anchor missing')
    rs = rs.replace(anchor, helper + anchor, 1)
old = "if(mechanicalEvent?.kind==='SCENE_OPENED'&&scene){const visible=[scene.summary,scene.situation,scene.premise].map(v=>String(v??'').trim()).filter(Boolean);if(visible.length)return Object.freeze({status:'OK',provider:'LOCAL_SCENE',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:visible.join('\\n\\n')})}"
new = "if(mechanicalEvent?.kind==='SCENE_OPENED'&&scene){const text=majorLocalFallback(mechanicalEvent,state);if(text)return Object.freeze({status:'OK',provider:'LOCAL_SCENE',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text})}"
if 'const text=majorLocalFallback(mechanicalEvent,state);if(text)return Object.freeze({status:\'OK\',provider:\'LOCAL_SCENE\'' not in rs:
    if old not in rs: raise SystemExit('opening fallback pattern missing')
    rs = rs.replace(old, new, 1)
old = "if(scene&&['travelArrival','travelEncounter','hexExploration','worldEvent'].includes(scene.contentType)){const visible=[scene.title,scene.summary,scene.situation,scene.premise].map(v=>String(v??'').trim()).filter(Boolean);if(visible.length)return Object.freeze({status:'OK',provider:'LOCAL_WORLD_SCENE',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:visible.join('\\n\\n')})}"
new = "if(scene&&['travelArrival','travelEncounter','hexExploration','worldEvent'].includes(scene.contentType)){const text=majorLocalFallback(mechanicalEvent,state);if(text)return Object.freeze({status:'OK',provider:'LOCAL_WORLD_SCENE',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text})}"
if "provider:'LOCAL_WORLD_SCENE'" in rs and 'scene&&[\'travelArrival\'' in rs and 'majorLocalFallback(mechanicalEvent,state)' not in rs[rs.find("if(scene&&['travelArrival'"):rs.find("if(scene&&['travelArrival'")+500]:
    if old not in rs: raise SystemExit('world fallback pattern missing')
    rs = rs.replace(old, new, 1)
rp.write_text(rs, encoding='utf-8')

print('WWN_REAL_FLOW_SOURCE_PATCH_OK')
