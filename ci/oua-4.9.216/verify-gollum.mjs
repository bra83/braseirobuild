import fs from 'node:fs';import path from 'node:path';
const root=process.argv[2];if(!root)throw Error('Missing OUA root');
const rd=f=>fs.readFileSync(path.join(root,f),'utf8');
const app=rd('App.tsx'),guide=rd('components/GollumAdvisor.tsx'),rules=rd('lib/gollumCounsel.ts'),rag=rd('lib/gollumRules.ts');
const tests={
  'guide linked into real app':app.includes("import { GollumAdvisor }")&&app.includes('<GollumAdvisor campaignId={campaign.id}'),
  'public state only':app.includes('visibleState={{')&&app.includes('pendingTest: campaign.pendingTest')&&!app.includes('visibleState={normalizeMasterState(campaign)}'),
  'no game writes':!guide.includes('persistCampaign(')&&!guide.includes('applyMasterCommands(')&&!guide.includes('onRunCommands'),
  'speech only by touch':guide.includes('onClick={()=>void speak(t)}')&&!guide.includes('useEffect(()=>void speak'),
  'uses existing Android voice':app.includes('onSpeak={text=>speakWithAndroid(text)}'),
  'interactive source-grounded rules':app.includes('onAsk={askGollumRules}')&&guide.includes('Consultando manuais')&&rag.includes('rankManualIndex(')&&rag.includes('answerRulesQuestion('),
  'no adventure secrets used in rule Q&A':rag.includes('mayProvideMechanics(m)')&&!rag.includes('mayProvideNarrativeContext'),
  'alert controls':guide.includes('Só importantes')&&guide.includes('Desativados')&&guide.includes('Já entendi'),
  'secrets never passed':!rules.includes('hiddenNpc')&&!rules.includes('gmSecrets')&&!rules.includes('futurePlot'),
  'version 4.9.216':rd('lib/version.ts').includes("'4.9.216'")&&rd('android/app/build.gradle.kts').includes('versionCode = 49216'),
  '352 catalog entries':JSON.parse(rd('public/tor2e-ui/hero-token-pack-352.json')).tokens.length===352,
};
for(const [name,ok] of Object.entries(tests))console.log((ok?'PASS ':'FAIL ')+name);
if(Object.values(tests).some(ok=>!ok))process.exit(1);
console.log('OUA_49216_GOLLUM_CLEAN_TOKENS_SOURCE_PASS');
