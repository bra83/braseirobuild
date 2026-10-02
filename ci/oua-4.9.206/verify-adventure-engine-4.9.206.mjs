import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const app=read('App.tsx');
const view=read('components/GameView.tsx');
const resolver=read('lib/adventureStartResolver.ts');
const state=read('lib/adventureState.ts');
const modules=read('lib/adventureModules.ts');
const missions=read('lib/striderPatronMissions.ts');
const db=read('lib/db.ts');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const checks={
  'version-4.9.206':version.includes("APP_VERSION = '4.9.206'")&&version.includes('APP_VERSION_CODE = 49206')&&pkg.version==='4.9.206'&&gradle.includes('versionCode = 49206')&&gradle.includes('versionName = "4.9.206"'),
  'adventure-state-persisted':db.includes("campaignStart?: import('./adventureTypes').CampaignStartSelection")&&db.includes("adventureRuntime?: import('./adventureTypes').AdventureRuntimeState"),
  'star-of-mist-module':modules.includes("id:'core-star-of-the-mist'")&&modules.includes("title:'A Estrela da Névoa'")&&modules.includes("driveFileId:'1M1rIXVz1L4Qzz98l5_aTF1c8sOokW4uA'"),
  'lone-lands-six-adventures':modules.includes("id:'lone-lands-troll-hole'")&&modules.includes('Pônei Saltitante')&&modules.includes('A Busca de Amon Guruthos'),
  'no-unbacked-starter-set':!modules.includes('Starter Set'),
  'strider-six-patrons':missions.includes('balin:[')&&missions.includes('bilbo:[')&&missions.includes('cirdan:[')&&missions.includes('gandalf:[')&&missions.includes('gilraen:[')&&missions.includes('tom:['),
  'strider-source-backed':modules.includes("printedPages:'223+'")&&missions.includes('Patrulheiro desaparecido')&&modules.includes("label:'Gandalf, o Cinzento'"),
  'sandbox-only-manual':resolver.includes("selection.mode==='sandbox'")&&resolver.includes('player-atlas-required')&&resolver.includes('No modo Sandbox, o sistema não escolhe por você.'),
  'tavern-deterministic':resolver.includes("campaign.id+':tavern-start'")&&resolver.includes('mapped-settlement-tavern')&&resolver.includes('tavernCandidates'),
  'official-auto-atlas':resolver.includes("selection.mode==='lone-lands'")&&state.includes('saveMapDiscovery')&&state.includes('currentHexId:resolved.hexId'),
  'strider-auto-atlas':resolver.includes("selection.mode==='strider'")&&resolver.includes('STRIDER_PATRONS')&&resolver.includes('missionIndex'),
  'opening-resolves-before-gemini':app.indexOf('await resolveCampaignStart(activeCampaign, characters)')<app.indexOf('await generateOpeningNarrationDirect(')&&app.includes('applyResolvedCampaignStart(activeCampaign, resolvedStart)'),
  'opening-adventure-context':app.includes('buildAdventureOpeningContext(sourceCampaign, resolvedStart)')&&app.includes('gemini-direct-opening-adventure-engine'),
  'first-valid-scene-preserved':app.includes("persistCampaign(updated, 'abertura direta Gemini validada')")&&app.includes('activateAdventureAfterOpening(updated, sceneId, canonicalLocation)'),
  'runtime-context-every-turn':app.includes('buildAdventureRuntimeContext(base)')&&app.includes('directorContext += adventureRuntimeContext')&&app.includes('publicContext += adventureRuntimeContext'),
  'runtime-history-tracked':app.includes('recordAdventureScene(updated, newNode.id, canonicalLocation)'),
  'ui-selector':view.includes('<CampaignStartSelector')&&view.includes('campaignStartMode')&&view.includes('canUseStrider'),
  'auto-modes-not-atlas-blocked':app.includes('canStartAdventure={campaignStartCanBegin(campaign)}')&&!app.includes('canStartAdventure={Boolean(normalizeMasterState(campaign).position.currentHexId)}'),
  'sandbox-message-specific':view.includes('No modo Sandbox, escolha primeiro a posição da Companhia no Atlas.'),
};

let failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length)throw new Error('VERIFY_OUA_4.9.206_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.206_OK');
