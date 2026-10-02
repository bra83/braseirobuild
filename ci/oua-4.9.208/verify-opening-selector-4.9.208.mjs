import fs from 'node:fs';
import path from 'node:path';

const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const view=read('components/GameView.tsx');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const pkg=JSON.parse(read('package.json'));

const selectorPos=view.indexOf('<CampaignStartSelector');
const narrationPos=view.indexOf('{visibleNarration || visibleActionResult ? (');
const profilePos=view.indexOf('{ADVENTURE_PROFILES.map');

const checks={
  'version-4.9.208':version.includes("APP_VERSION = '4.9.208'")&&version.includes('APP_VERSION_CODE = 49208')&&pkg.version==='4.9.208'&&gradle.includes('versionCode = 49208')&&gradle.includes('versionName = "4.9.208"'),
  'explicit-opening-type-heading':view.includes('Tipo de abertura / início da campanha'),
  'selector-before-narration-conditional':selectorPos>=0&&narrationPos>=0&&selectorPos<narrationPos,
  'selector-not-buried-with-profiles':selectorPos>=0&&profilePos>=0&&selectorPos<profilePos,
  'new-campaign-only':view.includes('history.length === 0 && !isViewingHistory'),
  'all-selector-controls-wired':view.includes('onCampaignStartModeChange')&&view.includes('campaignStartPatronId')&&view.includes('onCampaignStartMissionChange'),
  'clear-followup-copy':view.includes('Depois de definir o tipo de abertura acima, escolha o estilo de narração.'),
};
const failed=[];
for(const [name,ok] of Object.entries(checks)){console.log((ok?'PASS ':'FAIL ')+name);if(!ok)failed.push(name);}
if(failed.length)throw new Error('VERIFY_OUA_4.9.208_FAILED: '+failed.join(', '));
console.log('VERIFY_OUA_4.9.208_OK');
