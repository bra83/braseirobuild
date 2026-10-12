#!/usr/bin/env python3
from pathlib import Path
from zipfile import ZipFile
from PIL import Image
import json, re, hashlib, sys
ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/'android/app'
SRC=APP/'src/main/java/com/braseiro/pathfinder2e'
MAIN=(SRC/'MainActivity.java').read_text(encoding='utf-8')
DB=(SRC/'AppDb.java').read_text(encoding='utf-8')
MAPM=(SRC/'MapPackageManager.java').read_text(encoding='utf-8')
RULE=(SRC/'Pathfinder2eRulesEngine.java').read_text(encoding='utf-8')
DICE=(SRC/'PhysicalDiceEngine.java').read_text(encoding='utf-8')
DICEAPP=(SRC/'Dice3DOverlay.java').read_text(encoding='utf-8')
DICEVIEW=(SRC/'Dice3DView.java').read_text(encoding='utf-8')
BACKUP=(SRC/'BackupContractValidator.java').read_text(encoding='utf-8')
CANDLE_GLOBAL=(SRC/'CandleLightOverlayView.java').read_text(encoding='utf-8')
BARB=(APP/'src/main/python/braseiro_barbara_adapter.py').read_text(encoding='utf-8')
BARMAN=json.loads((APP/'src/main/python/BARBARA_EMBEDDED.json').read_text(encoding='utf-8'))
ORCH=(SRC/'MasterOrchestrator.java').read_text(encoding='utf-8')
SUP=(SRC/'BarbaraSupervisor.java').read_text(encoding='utf-8')
GUARD=(SRC/'DatabaseMigrationGuard.java').read_text(encoding='utf-8')
checks=[]
def ck(name,ok,detail=''):
    checks.append((name,bool(ok),detail));
    if not ok: print('FAIL',name,detail)
    else: print('PASS',name,detail)

def sha(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
# Android Studio structure
for rel in ['settings.gradle.kts','build.gradle.kts','gradlew','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties','android/app/build.gradle.kts','android/app/src/main/AndroidManifest.xml']:
    ck('estrutura:'+rel,(ROOT/rel).is_file())
props={}
for line in (ROOT/'version.properties').read_text().splitlines():
    if '=' in line and not line.lstrip().startswith('#'): k,v=line.split('=',1);props[k.strip()]=v.strip()
ck('versão 8.4.0',props.get('VERSION_NAME')=='8.4.0' and props.get('VERSION_CODE')=='80400' and props.get('DB_SCHEMA')=='58' and props.get('BACKUP_FORMAT')=='21',str(props))
gradle=(APP/'build.gradle.kts').read_text()
ck('applicationId PF2e','applicationId = "com.braseiro.pathfinder2e"' in gradle)
ck('SDK Android',all(x in gradle for x in ['compileSdk = 35','minSdk = 24','targetSdk = 35']))
# PF2 product identity + no old visible D&D choices
ck('perfil único runtime',('private static final String WORLD_PF2="pf2"' in MAIN or 'private static final String WORLD_PF2="pathfinder_2e"' in MAIN) and 'private String activeWorldProfile(){return WORLD_PF2;}' in MAIN)
old_styles=['Fantasia heroica','Espada e feitiçaria','Grimdark','Aventura clássica','Horror gótico','Investigação','Dungeon crawler','Alta fantasia cinematográfica']
ck('estilos D&D removidos',not any(x in MAIN for x in old_styles))
for s in ['Contratos e missões de guilda','Exploração de fronteira','Intriga urbana e facções','Expedição a ruínas e masmorras','Caçada a monstros','Mistério e ocultismo','Sobrevivência e viagem','Saga épica de nível 1 a 20']:
    ck('estilo PF2:'+s,s in MAIN)
ck('Atlas sem seletor D&D/Mystara','new String[]{"D&D","Mystara"}' not in MAIN and 'new String[]{"D&D", "Mystara"}' not in MAIN)
ck('rótulo Atlas','if("Mapa".equals(internal))return "Atlas";' in MAIN)
ck('aventura não bloqueia sem mapa','if(packageId.length()>0&&isEligibleAdventureStartHex(current)){launchAdventure(reset,current);return;}' in MAIN and 'launchAdventure(reset,"");' in MAIN and 'atlas_optional' in (SRC/'AdventureStartupCoordinator.java').read_text())
ck('ficha exporta JSON','private void showCharacterSheetDialog' in MAIN and 'EXPORTAR FICHA JSON' in MAIN and 'beginCharacterExport(merged' in MAIN)
ck('posição naturalizada','LOCAL ATUAL' in MAIN and 'Referência cartográfica' in MAIN)
ck('formatter Atlas compilável',len(re.findall(r'private\s+static\s+String\s+formatLocationTypePt\s*\(\s*String\s+raw\s*\)',MAIN))==1 and 'locationTypePt(' not in MAIN and MAIN.count('formatLocationTypePt(')>=2)
# Mechanics
for token in ['Degree { CRITICAL_SUCCESS, SUCCESS, FAILURE, CRITICAL_FAILURE }','if(natural==20)','else if(natural==1)','mapPenalty','recoveryCheck','knockedOut','validConditionMutation']:
    ck('PF2 mecânica:'+token,token in RULE)
ck('firewall mecânico PF2','nenhuma mecânica D&D, Mystara ou PF1 é fallback' in RULE)
# DB guard/current schema
ck('DB PF2e schema 58','private static final String DB_NAME = "braseiro_pathfinder2e.db"' in DB and 'private static final int DB_VERSION = 58' in DB)
ck('migration guard PF2','braseiro_pathfinder2e.db' in GUARD and 'pre_v58' in GUARD and 'db_migration_guard_pf2_v58' in GUARD and 'braseiro_dnd.db' not in GUARD)
# Scene consequence atomicity/cognitive persistence
segment=MAIN[MAIN.find('private void applyMasterConsequences(final long transactionId'):MAIN.find('private JSONObject copyJsonObject',MAIN.find('private void applyMasterConsequences(final long transactionId'))]
ck('consequências transacionais','db.runInTransactionChecked' in segment)
ck('orquestrador dentro da transação','masterOrchestrator.afterConsequencesApplied' in segment and segment.find('masterOrchestrator.afterConsequencesApplied') < segment.find('}});'))
ck('rollback confirmado por quick_check','db.quickIntegrityCheck()' in segment and 'databaseRolledBackCleanly' in segment)
ck('snapshot só após falha do quick_check','if(databaseRolledBackCleanly)' in segment and 'else if(rollback!=null&&safety!=null)' in segment)
ck('estado cognitivo read-back','saveMasterRuntimeStateChecked' in DB and 'Persistência cognitiva divergente' in DB and 'after=getMasterRuntimeState()' in DB)
ck('estado cognitivo no commit','storeValidationState(envelope,plan);' in (SRC/'MasterOrchestrator.java').read_text())
# Candle parchment
candle=(SRC/'CandleParchmentLayout.java').read_text()
for token in ['RadialGradient','LinearGradient','postInvalidateDelayed(33L)','Math.sin(t*5.31)','Math.sin(t*11.73','wander','setFlickerEnabled','setIntensity']:
    ck('vela:'+token,token in candle)
ck('vela ligada ao Mestre','candleParchmentCard()' in MAIN and 'Luz de vela oscilante sobre o pergaminho do Mestre' in MAIN)
ck('vela global montada','private CandleLightOverlayView candleOverlay;' in MAIN and 'candleOverlay=new CandleLightOverlayView(this)' in MAIN and 'shellHost.addView(candleOverlay' in MAIN)
ck('vela global oscilante',all(x in CANDLE_GLOBAL for x in ['RadialGradient','postInvalidateDelayed(66)','Math.sin','setIntensity','onTouchEvent']))
ck('vela hooks de narração',all(x in MAIN and x in CANDLE_GLOBAL for x in ['onMasterNarrationStarted','onNarrationChunk','onMasterNarrationEnded']))
# Dice 3D nativo — mesma física/renderização do Braseiro GURPS, regra PF2 no host
for token in ['ENGINE_ID="braseiro-physical-dice-1"','FIXED_DT=1.0/120.0','rawTopValue()','collideDice','forceSettle','d10()','icosa()','dodeca()']:
    ck('dice nativo:'+token,token in DICE)
for sides in ['4','6','8','10','12','20','100']:
    ck('dice nativo d'+sides,sides in DICE)
ck('dice renderer nativo','PhysicalDiceEngine.Session' in DICEVIEW and 'drawFace' in DICEVIEW and 'drawLabel' in DICEVIEW and 'topFace()' in DICE)
ck('dice overlay sem WebView','android.webkit.WebView' not in DICEAPP and 'dice3d_core.js' not in DICEAPP)
for token in ['PhysicalDiceEngine.create','PhysicalDiceEngine.FIXED_DT','new Dice3DView','spec.resolve(rolls)','APLICAR RESULTADO']:
    ck('dice integração:'+token,token in DICEAPP)
ck('dice regra PF2 continua host','DiceRollSpec spec' in DICEAPP and 'spec.resolve(rolls)' in DICEAPP)
ck('dice áudio impacto',(SRC/'DiceSoundEngine.java').is_file() and 'AudioAttributes.USAGE_GAME' in (SRC/'DiceSoundEngine.java').read_text(encoding='utf-8'))
for material in ['plastic','stone','metal','bone','wood','crystal','glass','obsidian','ivory','leather']:
    ck('dice som:'+material,(APP/f'src/main/res/raw/dice_{material}.wav').is_file())
# Bugs de aparelho — contratos exatos
ck('backup formato 21 aceito','MAX_SUPPORTED_VERSION=21' in BACKUP and 'backupVersion>21' in DB and 'BACKUP_FORMAT=21' in (ROOT/'version.properties').read_text())
for stale in ['429f','450.333f','45.033320996790806f']:
    ck('Atlas sem geometria V2:'+stale,stale not in MAPM)
ck('Atlas valida pelo manifesto',all(x in MAPM for x in ['getOriginX()','getStepX()','getStepY()','getAlternateColumnOffsetY()','expectedX','expectedY']))
# Icon
from PIL import Image
sizes={'mdpi':48,'hdpi':72,'xhdpi':96,'xxhdpi':144,'xxxhdpi':192}
for den,sz in sizes.items():
    p=APP/f'src/main/res/mipmap-{den}/ic_launcher.png'; q=APP/f'src/main/res/mipmap-{den}/ic_launcher_round.png'
    ck('ícone '+den,p.is_file() and q.is_file() and Image.open(p).size==(sz,sz) and Image.open(q).size==(sz,sz))
ck('adaptive icon',(APP/'src/main/res/mipmap-anydpi-v26/ic_launcher.xml').is_file() and (APP/'src/main/res/drawable-nodpi/ic_launcher_foreground.png').is_file())
# Atlas package exact integration
bundle=APP/'src/main/assets/pathfinder/atlas/inner_sea_canonical_v5_max_compat.zip'
ck('bundle Atlas presente',bundle.is_file() and bundle.stat().st_size>10_000_000,str(bundle.stat().st_size if bundle.exists() else 0))
with ZipFile(bundle) as z:
    ck('bundle Atlas íntegro',z.testzip() is None)
    names=set(z.namelist())
    req=['package.json','map.png','hex_ids_overlay.png','map_regions_ptbr_overlay.png','nation_labels_ptbr_overlay.png','map_manifest_pathfinder2e_inner_sea_2135x2720.json','pathfinder2e_inner_sea_hex_atlas_v1.json','pathfinder2e_inner_sea_locations_v1.json','pathfinder2e_inner_sea_nations_v1.json','pathfinder2e_inner_sea_region_profiles_v1.json','pathfinder2e_inner_sea_canonical_gazetteer_v2.json','pathfinder2e_inner_sea_source_index_v1.json']
    for n in req: ck('atlas arquivo:'+n,n in names)
    package=json.loads(z.read('package.json'))
    manifest=json.loads(z.read('map_manifest_pathfinder2e_inner_sea_2135x2720.json'))
    atlas=json.loads(z.read('pathfinder2e_inner_sea_hex_atlas_v1.json'))
    loc=json.loads(z.read('pathfinder2e_inner_sea_locations_v1.json'))
    nations=json.loads(z.read('pathfinder2e_inner_sea_nations_v1.json'))
    regions=json.loads(z.read('pathfinder2e_inner_sea_region_profiles_v1.json'))
    gaz=json.loads(z.read('pathfinder2e_inner_sea_canonical_gazetteer_v2.json'))
    sources=json.loads(z.read('pathfinder2e_inner_sea_source_index_v1.json'))
    ck('mapId V5 MAX',package.get('mapId')=='pathfinder2e-inner-sea-2135x2720-canonical-v5-max-compat',str(package.get('mapId')))
    ck('dataRevision V5-MAX',package.get('dataRevision')=='V5-MAX',str(package.get('dataRevision')))
    ck('runtime V5 otimizado',package.get('runtimePackaging',{}).get('previewAssetsExcluded') is True)
    ck('previews redundantes fora do asset',not any(n in names for n in ['nation_preview_on_map.png','overlay_preview_on_map.png','official_reference_aligned_preview.png','map_regions_ptbr.png']))
    ck('mapa 2135x2720',manifest['image']['width']==2135 and manifest['image']['height']==2720)
    lay=manifest['layout']; ck('flat-top manifesto',lay['orientation']=='flat-top' and lay['columns']==55 and lay['rows']==60)
    ck('3300 hexes',len(atlas['hexes'])==3300)
    ck('67 localidades',len(loc['locations'])==67)
    ck('41 nações',len(nations['nations'])==41)
    ck('10 metarregiões',len(atlas['metaregions'])==10)
    reg_list=regions.get('regions',regions.get('profiles',[]))
    gaz_list=gaz.get('entries',gaz.get('gazetteer',[]))
    ck('10 perfis regionais V5',len(reg_list)==10,str(len(reg_list)))
    ck('239 entradas gazetteer V5',len(gaz_list)==239,str(len(gaz_list)))
    ck('source index V5 presente',isinstance(sources,dict) and len(sources)>0)
    h0=next(iter(atlas['hexes'].values()))
    for k in ['canonicalContext','travelModel','environmentModel','regionalSetting']:
        ck('hex V5 enriquecido:'+k,k in h0)
    ck('escala V5 40mi',h0.get('travelModel',{}).get('hexScaleMiles')==40)
    ck('heurística viagem não canônica',h0.get('travelModel',{}).get('derivedNotCanonical') is True)
    ck('heurística ambiente não canônica',h0.get('environmentModel',{}).get('derivedNotCanonical') is True)
    x=lay['originX']+11*lay['adjacentColumnCenterDx']; y=lay['originY']+9*lay['sameColumnCenterDy']+(11%2)*lay['alternateColumnOffsetY']
    ck('hex 11.09 matemático',abs(x-439.85)<1e-6 and abs(y-450.17110120232223)<0.01,f'{x},{y}')
    # PNG dimensions from zip
    import io
    ck('map.png dimensão real',Image.open(io.BytesIO(z.read('map.png'))).size==(2135,2720))
ck('Atlas importado em SQLite',all(t in MAPM for t in ['importAtlas','importLocations','importNations','importMetaregions']) and all(t in DB for t in ['map_locations','map_nations','map_metaregions']))
ck('overlays separados',all(t in MAPM for t in ['regions_overlay_uri','political_overlay_uri','overlay_uri']))
ck('pacote bundled autoimport','ensureBundledMapForProfileAsync' in MAIN and 'pathfinder/atlas/inner_sea_canonical_v5_max_compat.zip' in MAIN)
ck('V5 raw_json persistido','raw_json' in DB and 'values.put("raw_json", value.toString())' in MAPM)
ck('V5 perfis regionais importados','importRegionProfiles' in MAPM and 'region_profiles' in MAPM)
ck('V5 source index persistido','source_index_json' in DB and 'sourceIndexName' in MAPM)
ck('migração família canônica','sameLogicalMapFamily' in MAPM and 'copyMapUserData' in MAPM and 'canonical-' in MAPM)
ck('hex numérico usa parser manifesto','MapManifestGeometry.parseHexId(a)' in MAIN and 'MapManifestGeometry.parseHexId(b)' in MAIN)
ck('V5 contexto Atlas UI','appendV5AtlasDetails' in MAIN and 'PERTO — POSIÇÕES CARTOGRÁFICAS' in MAIN and 'NA REGIÃO' in MAIN)
ck('V5 contexto IA','appendV5AiContext' in MAIN and 'nunca os trate como presentes aqui' in MAIN)
ck('V5 escala fallback 40mi','startsWith("pathfinder2e-inner-sea-2135x2720-canonical-")' in (SRC/'MapManifestGeometry.java').read_text() and 'g.hexScaleMiles=40f' in (SRC/'MapManifestGeometry.java').read_text())
# Barbara
ck('Barbara adapter PF2 1.1.2','"system_id": "pathfinder_2e"' in BARB and 'Pathfinder2eRulesEngine+RuleRetrievalEngine+MasterRulesArbiter' in BARB)
ck('Barbara runtime 1.1.2',BARMAN.get('motor_version')=='1.1.2' and BARMAN.get('module_count')==60)
ck('Barbara 60 hashes',len(BARMAN.get('modules',[]))==60 and all(x.get('sha256') for x in BARMAN.get('modules',[])))
ck('Barbara sem PF2 mecânico fictício',BARMAN.get('native_barbara_pathfinder_module') is False and 'barbara_mechanics_authority": False' in BARB)
ck('canais explícitos Barbara',BARMAN.get('contracts',{}).get('input_channels')==['PLAYER_ACTION','GM_HELP'] and 'input_channel_authoritative' in BARB)
ck('Main PLAYER_ACTION explícito','sendAi(chosenMode,q,"PLAYER_ACTION")' in MAIN and 'sendAi(mode,query,"PLAYER_ACTION")' in MAIN)
ck('Main GM_HELP explícito','sendAi("Narrador",routed,"GM_HELP")' in MAIN)
ck('ação não reroteada por heurística','if(BraseiroNarrativeDirector.isNonAdvancing(q)){sendMasterSideQuestion(q);return;}' not in MAIN)
ck('orquestrador canal vinculante','explicitAction="PLAYER_ACTION".equals(inputChannel)' in ORCH and 'explicitHelp="GM_HELP".equals(inputChannel)' in ORCH and 'barbara.plan(action,"normal",narrative,nonAdvancing,barbaraLocation,barbaraImprint,inputChannel)' in ORCH)
ck('orquestrador posição canônica','canonical_position' in ORCH and 'canonicalPackage.length()>0&&canonicalHex.length()>0' in ORCH and 'source","atlas"' in ORCH)
ck('cache inclui canal','inputChannel==null?"legacy":inputChannel' in ORCH)
ck('supervisor PF2 1.1','health_or_system_mismatch' in SUP and 'barbara_supervisor_pathfinder2e' in SUP and 'String inputChannel' in SUP)
# Sandbox POIs 8.3
POI=(SRC/'CampaignPoiService.java').read_text(encoding='utf-8')
MAPVIEW=(SRC/'MapBoardView.java').read_text(encoding='utf-8')
ck('POI schema separado',all(x in DB for x in ['campaign_poi_meta','campaign_pois','campaign_poi_history','campaign_poi_proposals','CHECK(canonical=0)']))
ck('POI fontes campaign/procedural',"SOURCE_CAMPAIGN=\"campaign\"" in POI and "SOURCE_PROCEDURAL=\"procedural\"" in POI and "CHECK(source IN ('campaign','procedural'))" in DB)
ck('POI sem escrita no Atlas canônico',not any(x in POI for x in ['insertOrThrow("map_locations"','update("map_locations"','delete("map_locations"','insertOrThrow("map_hex_atlas"','update("map_hex_atlas"','delete("map_hex_atlas"']))
ck('POI criação manual no hex','createManual(JSONObject spec,String hexId)' in POI and 'ADICIONAR LOCAL' in MAIN and 'editCampaignPoiDialog(null,hexId)' in MAIN)
ck('POI settlement integra Mundo Vivo','linkSettlement' in POI and 'settlement_states' in POI and 'campaign-poi:' in POI)
ck('POI sublocais','parent_poi_id' in POI and 'idx_campaign_pois_parent' in DB)
ck('POI segredos filtrados','hidden=0 AND discovered=1' in POI and 'playerProjection' in POI and 'private_pois' in POI and 'conhecimento EXCLUSIVO do Mestre' in ORCH)
ck('POI descoberta/evolução/histórico',all(x in POI for x in ['discover(String poiId','evolve(String poiId','campaign_poi_history','abandoned','ruined','destroyed','rebuilding']))
ck('POI Mundo Vivo causal','tick-last<72' in POI and 'no_world_causal_signal' in POI and 'world_events' in POI and 'factions' in POI and 'Math.random' not in POI and 'new Random' not in POI)
ck('POI geração pondera geografia/mundo',all(x in POI for x in ['\"terrain\"','\"has_road\"','\"has_waterway\"','\"weather\"','\"danger\"','\"regional_trade\"','\"regional_prosperity\"','\"faction_count\"','\"threat_count\"','world_context_incompatible']))
ck('POI Mundo Vivo evolui o mesmo id','applyWorldEvolution(dayBefore,dayAfter)' in POI and 'historyHasReason(eventKey)' in POI and 'worldEvolutionMutation' in POI and 'evolve(target.optString(\"poi_id\")' in POI)
ck('POI assentamento UI estruturada',all(x in MAIN for x in ['POPULAÇÃO','NÍVEL DO ASSENTAMENTO','GOVERNO','LEALDADE / ESTABILIDADE','PROSPERIDADE 0–100','SEGURANÇA 0–100','CONSTRUÇÕES (separadas por vírgula)','NPCs IMPORTANTES (separados por vírgula)','MERCADO','SERVIÇOS','DEFESA']) and 'ASSENTAMENTO — JSON OPCIONAL' not in MAIN)
ck('POI busca/rota','findVisibleByQuery' in POI and 'resolveVisibleByName' in POI and 'campaignPoiService.resolveVisibleByName(userText)' in MAIN and 'routePreview(current,hex,8,mapGeometry)' in MAIN)
ck('POI export/import','braseiro-pathfinder2e-campaign-pois' in POI and 'EXPORTAR POIs' in MAIN and 'IMPORTAR POIs' in MAIN and 'campaign_pois.json' in MAIN)
ck('POI backup round-trip v21',all(x in DB for x in ['campaignPoiMeta','campaignPois','campaignPoiHistory','campaignPoiProposals']) and 'validateCampaignPois' in BACKUP)
ck('POI narrativa propõe sem auto-gravar','proposeFromNarration' in POI and 'Novo POI detectado:' in MAIN and all(x in MAIN for x in ['IGNORAR','EDITAR','SALVAR']))
ck('POI render Atlas','setCampaignPois(List<JSONObject> pois)' in MAPVIEW and 'drawCampaignPois(canvas)' in MAPVIEW and 'refreshCampaignPoiMarkers' in MAIN)
ck('POI Barbara projeções','campaign_pois_public' in ORCH and 'campaign_pois_private' in ORCH and 'nunca o revele' in ORCH)
ck('POI não duplica mecânica PF2','Pathfinder2eRulesEngine' not in POI and 'CampaignPoiService' not in RULE)
# No deleted mechanical legacy classes
for name in ['DndRulesEngine.java','MystaraRulesEngine.java','MystaraManualAuthority.java']:
    ck('classe legada ausente:'+name,not (SRC/name).exists())
# No obvious secrets / signing material
bad=[]
for p in ROOT.rglob('*'):
    if p.is_file() and p.suffix.lower() in {'.jks','.keystore'}: bad.append(str(p.relative_to(ROOT)))
ck('sem keystore embarcado',not bad,','.join(bad))
# XML parsing
import xml.etree.ElementTree as ET
xmlbad=[]
for p in (APP/'src/main/res').rglob('*.xml'):
    try: ET.parse(p)
    except Exception as e: xmlbad.append(f'{p.name}:{e}')
ck('XML resources válidos',not xmlbad,';'.join(xmlbad[:4]))
# Source count and obsolete direct class references
java=list((APP/'src/main/java').rglob('*.java'));ck('fontes Java >=140',len(java)>=140,str(len(java)))
active='\n'.join(p.read_text(encoding='utf-8',errors='ignore') for p in java)
ck('sem referência de tipo DndRulesEngine','DndRulesEngine ' not in active and 'new DndRulesEngine' not in active)
ck('sem referência de tipo MystaraRulesEngine','MystaraRulesEngine ' not in active and 'new MystaraRulesEngine' not in active)

# 8.4 — device regressions: grid, canonical places, POI detector, Barbara 1.1.2
GEOM=(SRC/'MapManifestGeometry.java').read_text(encoding='utf-8')
DIAG=(SRC/'SystemDiagnostics5.java').read_text(encoding='utf-8')
ck('8.4 grade incorporada sem duplicação','private boolean mapUsesBakedHexGrid(String manifestJson){return false;}' not in MAIN and 'CANONICAL_INNER_SEA_MAP_ID.equals(id)' in MAIN and 'mapView.setVectorGridVisible(bakedGrid?false' in MAIN)
ck('8.4 calibração raster runtime',all(x in GEOM for x in ['rasterScale=1.004556327f','originX=originX*rasterScale-4.7674317f','originY=originY*rasterScale+2.5002666f']))
ck('8.4 centros DB usam geometria runtime','runtimeHex=geometry==null?null:geometry.byId(row.hexId)' in MAPM and 'runtimeHex.centerX' in MAPM and 'runtimeHex.centerY' in MAPM)
ck('8.4 reimporta revisão calibrada','CANONICAL_INNER_SEA_BUNDLE_REVISION=6' in MAIN and 'installedRevision>=CANONICAL_INNER_SEA_BUNDLE_REVISION&&canonicalReady.optInt("ready",0)==1' in MAIN)
ck('8.4 catálogo referenciado',all(x in DB for x in ['countMapReferencedLocations','countMapApproximateReferencedLocations','listMapLocationsWithHexReference']))
ck('8.4 POIs canônicos renderizados','setCanonicalPois' in MAPVIEW and 'drawCanonicalPois(canvas)' in MAPVIEW and 'refreshCanonicalPoiMarkers' in MAIN)
ck('8.4 diagnóstico cartográfico honesto','Pontos catalogados com referência cartográfica' in DIAG and 'countMapReferencedLocations(pkg)>0' in DIAG)
ck('8.4 aproximação não promovida a exata','Posição cartográfica aproximada do pacote V5; não é uma coordenada oficial exata.' in MAIN)
persistent_line=next((x for x in POI.splitlines() if 'PERSISTENT_PLACE' in x),'')
proposal_segment=POI[POI.find('public JSONObject proposeFromNarration'):POI.find('/**',POI.find('public JSONObject proposeFromNarration'))]
ck('8.4 guilda não vira lugar','|guilda|' not in persistent_line and 'boolean locative=' in proposal_segment and 'displayType(type)+" de "+hex' not in proposal_segment)
ck('8.4 nome descritivo sem fabricar hex','extractDescriptivePlaceName' in POI and 'String descriptive=base+" "+tail;' in POI)
ck('8.4 Motor supervisor 1.1.2','pathfinder_2e_embedded_1_1_2' in BARB and 'external_host' in BARB and 'configure_external_supervisor_json' in BARB)
ck('8.4 mapa oficial imutável',sha(bundle)=='dbd7262e44a6c614f8262ed23c88bc0080bce91dc3ffc8991a25b84887149c83')

# Final
failed=[x for x in checks if not x[1]]
print(f'FINAL {len(checks)-len(failed)}/{len(checks)} PASS')
if failed:
    print('FAILED:',*[x[0] for x in failed],sep='\n - ');sys.exit(1)
