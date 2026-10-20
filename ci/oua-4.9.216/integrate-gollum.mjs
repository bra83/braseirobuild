import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
const root=process.argv[2];if(!root||!fs.existsSync(path.join(root,'App.tsx')))throw Error('Missing OUA root');
const here=path.dirname(fileURLToPath(import.meta.url));
for(const [source,destination] of [['GollumAdvisor.tsx','components/GollumAdvisor.tsx'],['gollumCounsel.ts','lib/gollumCounsel.ts']]){
  const out=path.join(root,destination);fs.mkdirSync(path.dirname(out),{recursive:true});fs.copyFileSync(path.join(here,source),out);
}
const filename=path.join(root,'App.tsx');let app=fs.readFileSync(filename,'utf8');
function swap(a,b,label){const count=app.split(a).length-1;if(count!==1)throw Error('OUA_49216_ANCHOR_'+label+'_'+count);app=app.replace(a,b)}
swap("import { AppHeader } from './components/AppHeader';",
  "import { AppHeader } from './components/AppHeader';\nimport { GollumAdvisor } from './components/GollumAdvisor';",'import');
const marker='<QuickAudioPanel open={quickAudioOpen} onClose={() => setQuickAudioOpen(false)} onStopAll={handleStopAll} />';
const component=[
  marker,
  '      <GollumAdvisor campaignId={campaign.id} visibleState={{',
  "        pendingTest: campaign.pendingTest ? {skill:String(campaign.pendingTest.skill||'teste')}:null,",
  '        pendingRollResolved: Boolean(campaign.pendingDiceRoll),',
  '        journeyPending: Boolean(normalizeMasterState(campaign).journey?.pendingRoll),',
  "        heroName: String(campaign.characterSheet?.identity?.name||''),",
  '        hope: (()=>{const sh=campaign.characterSheet as any;const n=Number(sh?.resources?.hope??sh?.other?.hope);return Number.isFinite(n)?n:null})(),',
  '        currentTab:activeTab,',
  "        locationName:String(campaign.currentLocation||''),",
  '        storyBusy:isStoryLoading,',
  '      }} onSpeak={text=>speakWithAndroid(text)}/>'
].join('\n');
swap(marker,component,'render');
fs.writeFileSync(filename,app);
const versionFile=path.join(root,'lib/version.ts');let ver=fs.readFileSync(versionFile,'utf8');ver=ver.replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.216'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49216');fs.writeFileSync(versionFile,ver);
const packageFile=path.join(root,'package.json');let pkg=JSON.parse(fs.readFileSync(packageFile,'utf8'));pkg.version='4.9.216';fs.writeFileSync(packageFile,JSON.stringify(pkg,null,2)+'\n');
const gradleFile=path.join(root,'android/app/build.gradle.kts');let gradle=fs.readFileSync(gradleFile,'utf8');gradle=gradle.replace(/versionCode\s*=\s*\d+/,'versionCode = 49216').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.216"');fs.writeFileSync(gradleFile,gradle);
console.log('OUA_49216_GOLLUM_READONLY_INTEGRATED');
