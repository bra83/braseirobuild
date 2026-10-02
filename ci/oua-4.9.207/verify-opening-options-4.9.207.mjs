import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const app=read('App.tsx');
const view=read('components/GameView.tsx');
const selector=read('components/CampaignStartSelector.tsx');
const resolver=read('lib/adventureStartResolver.ts');
const state=read('lib/adventureState.ts');
const modules=read('lib/adventureModules.ts');
const missions=read('lib/striderPatronMissions.ts');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const striderStart=resolver.indexOf("if(selection.mode==='strider')");
const striderEnd=resolver.indexOf("const module=selection.mode==='lone-lands'?",striderStart);
const striderBlock=resolver.slice(striderStart,striderEnd);

const checks={
  'version-4.9.207':version.includes("APP_VERSION = '4.9.207'")&&version.includes('APP_VERSION_CODE = 49207')&&pkg.version==='4.9.207'&&gradle.includes('versionCode = 49207')&&gradle.includes('versionName = "4.9.207"'),
  'selector-five-start-modes':selector.includes('CAMPAIGN_START_OPTIONS')&&modules.includes("mode:'star-of-mist'")&&modules.includes("mode:'lone-lands'")&&modules.includes("mode:'strider'")&&modules.includes("mode:'sandbox'")&&modules.includes("mode:'tavern'"),
  'strider-patron-ui':selector.includes('1. Escolha o Patrono')&&selector.includes('STRIDER_PATRONS')&&selector.includes('onPatronChange'),
  'strider-mission-ui':selector.includes('2. Escolha a missão')&&selector.includes('Sortear missão')&&selector.includes('STRIDER_MISSIONS')&&selector.includes('onMissionChange'),
  'six-patrons':missions.includes('balin:[')&&missions.includes('bilbo:[')&&missions.includes('cirdan:[')&&missions.includes('gandalf:[')&&missions.includes('gilraen:[')&&missions.includes('tom:['),
  'strider-requires-explicit-patron':striderBlock.includes('strider-patron-required')&&!striderBlock.includes("||'gandalf'")&&!striderBlock.includes('patronId(hero'),
  'strider-requires-explicit-mission':striderBlock.includes('strider-mission-required')&&striderBlock.includes('Number(selection.missionIndex)'),
  'start-button-gated-for-strider':resolver.includes("selection.mode==='strider'")&&resolver.includes('STRIDER_PATRONS[selection.patronId]')&&resolver.includes('missionIndex<6'),
  'host-random-persists':app.includes("mission === 'random' ? drawStriderMissionIndex(patronId) : mission")&&app.includes("mission === 'random' ? 'missão do Patrono sorteada' : 'missão do Patrono escolhida'"),
  'gameview-props-wired':view.includes('campaignStartPatronId')&&view.includes('campaignStartMissionIndex')&&view.includes('onCampaignStartPatronChange')&&view.includes('onCampaignStartMissionChange'),
  'readable-summary':state.includes("Patrono: '+patron.label")&&state.includes("STRIDER_MISSIONS[start.patronId]"),
  'starter-still-source-gated':!modules.includes('Starter Set'),
  'first-valid-scene-still-used':app.includes('await generateOpeningNarrationDirect(')&&app.includes("persistCampaign(updated, 'abertura direta Gemini validada')"),
  'sandbox-remains-manual':resolver.includes('player-atlas-required')&&resolver.includes('No modo Sandbox, o sistema não escolhe por você.'),
  'tavern-remains-host-resolved':resolver.includes("campaign.id+':tavern-start'")&&resolver.includes('tavernCandidates'),
};

const failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length)throw new Error('VERIFY_OUA_4.9.207_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.207_OK');
