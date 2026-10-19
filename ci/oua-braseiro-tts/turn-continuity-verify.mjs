import fs from 'node:fs';import path from 'node:path';
const root=process.argv[2]||process.cwd();const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const app=read('App.tsx'),svc=read('services/geminiService.ts'),version=read('lib/version.ts'),gradle=read('android/app/build.gradle.kts');
const checks={
  preflightHandled:app.includes("console.error('[BRASEIRO_TURN_PREFLIGHT]'")&&app.includes("friendlyTurnError(preflightError)"),
  pendingTestExplained:(app.match(/Há um teste pendente. Resolva a rolagem/g)||[]).length>=2,
  duplicateHandled:app.includes('Esta ação já foi processada. A cena anterior foi preservada'),
  noSilentEmpty:app.includes('O Gemini não forneceu uma nova cena. A ação não foi gravada'),
  openingStrict:app.includes('isOpeningScene && turnRhythm.targetCharMin > 0'),
  continuationNotHardRejected:!(app.includes('if (narrativeIntent.advancesFiction && turnRhythm.targetCharMin > 0) {')),
  remoteContinuationNoDensityHardGate:svc.includes("minChars:cadence==='opening'?Math.max(6500,minChars):0"),
  stageEvidence:app.includes("BRASEIRO_TURN_STAGE] director-ready")&&app.includes("BRASEIRO_TURN_STAGE] narration-ready")&&app.includes("BRASEIRO_TURN_STAGE] commit-ready"),
  speechPreserved:app.includes('prepareBraseiroVocalTurn')&&svc.includes('speech_metadata'),
  version:version.includes("'4.9.213'")&&gradle.includes('versionCode = 49213')&&JSON.parse(read('package.json')).version==='4.9.213',
};
for(const [name,ok] of Object.entries(checks))console.log((ok?'PASS ':'FAIL ')+name);
if(Object.values(checks).some(x=>!x))process.exit(1);
console.log('OUA_49213_TURN_FLOW_STATIC_PASS '+Object.keys(checks).length);