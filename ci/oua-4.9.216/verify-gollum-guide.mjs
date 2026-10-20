import fs from 'node:fs';import path from 'node:path';
const root=process.argv[2];if(!root)throw Error('OUA root missing');
const rd=x=>fs.readFileSync(path.join(root,x),'utf8');
const app=rd('App.tsx'),gui=rd('components/GollumGuide.tsx'),logic=rd('lib/gollumGuidance.ts'),version=rd('lib/version.ts');
const manifest=JSON.parse(rd('public/tor2e-ui/hero-token-pack-352.json'));
const standard=JSON.parse(rd('public/tor2e-ui/party-token-manifest.json'));
const checks={
  importedGuide:app.includes("import { GollumGuide } from './components/GollumGuide'"),
  renderedGuide:app.includes('<GollumGuide')&&app.includes('campaignId={campaign.id}'),
  publicInputOnly:app.includes('pendingTest={campaign.pendingTest}')&&app.includes('journeyPending={Boolean(normalizeMasterState(campaign).journey?.pendingRoll)}'),
  existingTTS:app.includes('speakGollumGuide')&&app.includes('generateNarration(message')&&app.includes('speakWithAndroid(message)'),
  dismissAndMemory:gui.includes('localStorage.setItem(prefKey(props.campaignId)')&&gui.includes('saved.seen.includes(advice.id)'),
  accessibility:gui.includes('role="dialog"')&&gui.includes('aria-label="Fechar guia"'),
  noPrivateGameInputs:!gui.includes('allNpcs')&&!gui.includes('hiddenEncounter')&&!gui.includes('npcMemory')&&!logic.includes('GMSecret'),
  playerOnlyActions:!gui.includes('applyMasterCommands')&&!gui.includes('Math.random')&&!logic.includes('Math.random'),
  waitsForRolls:logic.includes('s.pendingTest&&!s.pendingRoll')&&logic.includes('s.pendingTest&&s.pendingRoll'),
  originalPack:standard.tokens.length===120&&manifest.tokens.length===352,
  preservedNames:manifest.tokens.every(x=>x.id.startsWith('oua352_')),
  imagesExist:manifest.tokens.every(x=>fs.existsSync(path.join(root,'public/tor2e-ui/assets',x.asset))),
  version:version.includes("'4.9.216'")&&rd('android/app/build.gradle.kts').includes('versionCode = 49216'),
};
for(const [label,ok] of Object.entries(checks))console.log((ok?'PASS ':'FAIL ')+label);
if(Object.values(checks).some(ok=>!ok))process.exit(1);
console.log('OUA_49216_GOLLUM_AND_TOKENS_GATE_PASS checks='+Object.keys(checks).length);
