import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root||!fs.existsSync(path.join(root,'App.tsx')))throw Error('OUA complete Android source missing');
const get=f=>fs.readFileSync(path.join(root,f),'utf8');
const put=(f,v)=>{const out=path.join(root,f);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,v,'utf8');};
const must=(ok,label)=>{if(!ok)throw Error('BRASEIRO_VOCAL_ANCHOR '+label);};
function once(s,from,to,label){const i=s.indexOf(from);must(i>=0&&s.indexOf(from,i+from.length)<0,label);return s.slice(0,i)+to+s.slice(i+from.length);}
const here=path.dirname(new URL(import.meta.url).pathname);
put('lib/braseiroVocalDirector.ts',fs.readFileSync(path.join(here,'braseiroVocalDirector.ts'),'utf8'));
let app=get('App.tsx');
app=once(app,
  "import { getBraseiroTtsConfig,voiceDirection } from './lib/braseiroTtsConfig';",
  "import { getBraseiroTtsConfig,voiceDirection } from './lib/braseiroTtsConfig';\nimport { prepareBraseiroVocalTurn } from './lib/braseiroVocalDirector';",'app import');
const oldUnits=[
"      const provisionalUnits: NarrationUnit[] = splitRange(text, 0, text.length, 680).map(piece => {",
"        const end = piece.start + piece.text.length;",
"        return {",
"          ...piece,",
"          segment: narratorSegment(piece.text),",
"          events: provisionalPlan.sfx.filter(event => Number.isFinite(event.charIndex) && event.charIndex >= piece.start && event.charIndex < end),",
"        };",
"      });"
].join('\n');
app=once(app,oldUnits,
"      // Interpret actual scene voiceScript. Never change the saved narrative.\n      const provisionalUnits: NarrationUnit[] = buildNarrationUnits(text, node.voiceScript || [], provisionalPlan.sfx);",'voiceScript integration');
const marker="      const firstProfile = voiceForSegment(provisionalUnits[0].segment);";
const synthesis=[
"      const synthesizeVocalUnit = (unit: NarrationUnit) => {",
"        const identity = voiceForSegment(unit.segment);",
"        const direction = prepareBraseiroVocalTurn(",
"          unit.text, identity.style, unit.segment.emotion || '',",
"          [node.location || '', node.weather || '', node.narrativeMeta?.rhythmCadence || ''].join(' '),",
"          unit.segment.speakerType,",
"        );",
"        AudioDiagnostics.log({ action: 'automatic-vocal-direction', channel: 'voice', level: 'info',",
"          message: 'As instruções vocais ficam apenas na síntese; a narração visível não foi alterada.',",
"          details: { emotion: direction.mood, tagCount: direction.tags.length, speaker: unit.segment.speakerType } });",
"        return generateNarration(direction.transcript, identity.voice, direction.style);",
"      };",
marker
].join('\n');
app=once(app,marker,synthesis,'synthesis helper');
app=once(app,
  'const firstAudioPromise = generateNarration(provisionalUnits[0].text, firstProfile.voice, firstProfile.style);',
  'const firstAudioPromise = synthesizeVocalUnit(provisionalUnits[0]);','first synthesis');
const next=[
"generateNarration(",
"              provisionalUnits[index + 1].text,",
"              voiceForSegment(provisionalUnits[index + 1].segment).voice,",
"              voiceForSegment(provisionalUnits[index + 1].segment).style,",
"            )"
].join('\n');
app=once(app,next,'synthesizeVocalUnit(provisionalUnits[index + 1])','all remaining chunks');
put('App.tsx',app);
let browser=get('services/geminiService.ts');
browser=once(browser,
  "{ text: 'Leia apenas a narração em português brasileiro: ' + transcript }",
  "{ text: transcript.replace(/<(?:short pause|breath|sigh|laugh|cough)>/gi, '') }",
  'legacy prompt must never become spoken text');
must(browser.includes('speech_metadata: { style: stableStyle }'),'Gemini 3.8 metadata');
browser=once(browser,'tts-v10-','tts-v11-','browser cache version');
put('services/geminiService.ts',browser);
let native=get('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt');
native=once(native,
  'if (modern) transcript else "Leia apenas a narração em português brasileiro: $transcript"',
  'if (modern) transcript else transcript.replace(Regex("<(short pause|breath|sigh|laugh|cough)>", RegexOption.IGNORE_CASE), "")',
  'legacy Android prompt');
native=once(native,'tts-v10|','tts-v11|','native cache version');
must(native.includes('part.put("speech_metadata", JSONObject().put("style", stableStyle))'),'native metadata');
put('android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt',native);
let ver=get('lib/version.ts').replace(/APP_VERSION\s*=\s*'[^']+'/, "APP_VERSION = '4.9.212'").replace(/APP_VERSION_CODE\s*=\s*\d+/, 'APP_VERSION_CODE = 49212');
put('lib/version.ts',ver);
let pkg=JSON.parse(get('package.json'));pkg.version='4.9.212';put('package.json',JSON.stringify(pkg,null,2)+'\n');
let gradle=get('android/app/build.gradle.kts').replace(/versionCode\s*=\s*\d+/,'versionCode = 49212').replace(/versionName\s*=\s*"[^"]+"/,'versionName = "4.9.212"');
put('android/app/build.gradle.kts',gradle);
console.log('BRASEIRO_TTS_49212_AUTOMATIC_VOCAL_TAGS_APPLIED');
