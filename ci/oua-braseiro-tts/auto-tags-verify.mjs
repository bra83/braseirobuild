import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
const root=process.argv[2]||process.cwd();
const get=f=>fs.readFileSync(path.join(root,f),'utf8');
const app=get('App.tsx'),browser=get('services/geminiService.ts'),native=get('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
const module=get('lib/braseiroVocalDirector.ts');
const checks={
  'dialogue uses real voiceScript':app.includes('buildNarrationUnits(text, node.voiceScript || [], provisionalPlan.sfx)'),
  'vocal tags applied on first block':app.includes('const firstAudioPromise = synthesizeVocalUnit(provisionalUnits[0])'),
  'vocal tags applied on subsequent blocks':app.includes('synthesizeVocalUnit(provisionalUnits[index + 1])'),
  'mood and event annotation exists':module.includes("'<sigh>'")&&module.includes("'<breath>'")&&module.includes("'<laugh>'")&&module.includes("'<short pause>'"),
  'Gemini 3.8 uses separate metadata':browser.includes('speech_metadata: { style: stableStyle }')&&native.includes('part.put("speech_metadata", JSONObject().put("style", stableStyle))'),
  'old Gemini receives no audible directions':!browser.includes("'Leia apenas a narração em português brasileiro: ' + transcript")&&!native.includes('else "Leia apenas a narração em português brasileiro: $transcript"'),
  'old models strip unsupported tags':browser.includes('transcript.replace(/<(?:short pause|breath|sigh|laugh|cough)>/gi')&&native.includes('transcript.replace(Regex("<(short pause|breath|sigh|laugh|cough)>"'),
  'fresh cache avoids earlier hallucinated instructions':browser.includes('tts-v11-')&&native.includes('tts-v11|'),
  'version bumped':get('lib/version.ts').includes("'4.9.212'")&&get('android/app/build.gradle.kts').includes('versionCode = 49212')&&JSON.parse(get('package.json')).version==='4.9.212',
};
for(const [name,ok] of Object.entries(checks))console.log((ok?'PASS ':'FAIL ')+name);
if(Object.values(checks).some(ok=>!ok))process.exit(1);
console.log('BRASEIRO_TTS_49212_VOCAL_STATIC_VERIFY_PASS '+Object.keys(checks).length);
