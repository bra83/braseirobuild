import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(process.argv[2]||process.cwd());
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');
const panel=read('components/JourneyResolutionPanel.tsx');
const version=read('lib/version.ts');
const gradle=read('android/app/build.gradle.kts');
const checks={
  'manual-roll-state':panel.includes('pendingManualRoll'),
  'roll-request-wrapper':panel.includes("const requestRollJourney")&&panel.includes("run: () => executeRollJourney(...args)"),
  'event-request-wrapper':panel.includes("const requestGenerateEvent")&&panel.includes("run: () => executeGenerateEvent(...args)"),
  'no-wrapper-recursion':!panel.includes("run: () => requestRollJourney(...args)")&&!panel.includes("run: () => requestGenerateEvent("),
  'explicit-roll-copy':panel.includes('Nenhum dado foi rolado ainda')&&/>\s*Rolar\s*<\/button>/.test(panel),
  'rolls-are-staged':!panel.includes('props.onRollJourney('),
  'event-generation-staged':!panel.includes('props.onGenerateEvent('),
  'cancel-present':panel.includes('Cancelar'),
  'version':version.includes("APP_VERSION = '4.9.203'")&&version.includes('APP_VERSION_CODE = 49203'),
  'android-version':gradle.includes('versionCode = 49203')&&gradle.includes('versionName = "4.9.203"'),
};
for(const [k,v] of Object.entries(checks)) console.log((v?'PASS ':'FAIL ')+k);
const bad=Object.entries(checks).filter(([,v])=>!v).map(([k])=>k);
if(bad.length) throw new Error('VERIFY_49203_FAILED: '+bad.join(', '));
console.log('VERIFY_OUA_4.9.203_OK');
