import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root||!fs.existsSync(path.join(root,'package.json')))throw Error('OUA root not found');
const src=path.join(root,'src');
if(!fs.existsSync(src))throw Error('src missing');
function crawl(p){return fs.readdirSync(p,{withFileTypes:true}).flatMap(x=>x.isDirectory()?crawl(path.join(p,x.name)):/\.(tsx?|jsx?)$/.test(x.name)?[path.join(p,x.name)]:[]);}
const candidates=crawl(src).map(p=>({p,t:fs.readFileSync(p,'utf8')})).filter(x=>/generateTTS|generateTts|generateSpeech|speechConfig|prebuiltVoiceConfig|synthesizeSpeech/i.test(x.t));
console.log('OUA TTS CANDIDATES',candidates.map(x=>path.relative(root,x.p)));
fs.writeFileSync(path.join(root,'BRASEIRO_TTS_INTEGRATION_DIAGNOSTIC.json'),JSON.stringify({sources:candidates.map(x=>path.relative(root,x.p)),status:'NOT_INTEGRATED'},null,2));
throw Error('Source inspection gate: shared TTS must be wired to OUA real audio runtime before APK release; refusing a fake completed build.');
