import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const immersive=read('components/immersive/ImmersiveJourneyView.tsx');
const view=read('components/GameView.tsx');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const checks={
 'version-4.9.209':version.includes("APP_VERSION = '4.9.209'")&&version.includes('APP_VERSION_CODE = 49209')&&pkg.version==='4.9.209'&&gradle.includes('versionCode = 49209')&&gradle.includes('versionName = "4.9.209"'),
 'immersive-selector-import':immersive.includes("CampaignStartSelector"),
 'immersive-selector-visible':immersive.includes('data-braseiro-opening-selector="true"')&&immersive.includes('Tipo de abertura'),
 'immersive-five-mode-selector-wired':immersive.includes('mode={props.campaignStartMode}')&&immersive.includes('onChange={props.onCampaignStartModeChange}')&&immersive.includes('onPatronChange={props.onCampaignStartPatronChange}')&&immersive.includes('onMissionChange={props.onCampaignStartMissionChange}'),
 'gameview-passes-selector-props':view.includes('campaignStartMode={campaignStartMode}')&&view.includes('onCampaignStartModeChange={onCampaignStartModeChange}')&&view.includes('campaignStartPatronId={campaignStartPatronId}')&&view.includes('campaignStartMissionIndex={campaignStartMissionIndex}')&&view.includes('canUseStrider={canUseStrider}'),
 'selector-before-start-button':immersive.indexOf('data-braseiro-opening-selector="true"')>=0&&immersive.indexOf('data-braseiro-opening-selector="true"')<immersive.indexOf('Começar aventura'),
 'sandbox-hint-scoped':immersive.includes("props.campaignStartMode === 'sandbox'"),
 'mobile-nav-preserved':immersive.includes("['jogo', 'Mapa'")&&immersive.includes("['combate', 'Combate'")&&immersive.includes("['ficha', 'Ficha'")&&immersive.includes("['dados', 'Dados'")&&immersive.includes('<span>Mais</span>'),
};
const failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length)throw new Error('VERIFY_OUA_4.9.209_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.209_OK');
