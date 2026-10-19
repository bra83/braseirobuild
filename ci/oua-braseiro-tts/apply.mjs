import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root||!fs.existsSync(path.join(root,'package.json')))throw Error('OUA root not found');
const src=path.join(root,'src');
if(!fs.existsSync(src))throw Error('src missing');
function crawl(p){return fs.readdirSync(p,{withFileTypes:true}).flatMap(x=>x.isDirectory()?crawl(path.join(p,x.name)):/\.(tsx?|jsx?)$/.test(x.name)?[path.join(p,x.name)]:[]);}
const re=/generateTTS|generateTts|generateSpeech|speechConfig|prebuiltVoiceConfig|synthesizeSpeech|generateContent.*AUDIO|textToSpeech|speechSynthesis|speakWith/i;
const candidates=crawl(src).map(p=>({p,t:fs.readFileSync(p,'utf8')})).filter(x=>re.test(x.t));
const records=[];
for(const {p,t} of candidates){
 const lines=t.split(/\r?\n/);
 const spans=[];
 for(let i=0;i<lines.length;i++)if(re.test(lines[i])){
    const low=Math.max(0,i-7),high=Math.min(lines.length,i+13);
    spans.push({line:i+1,text:lines.slice(low,high).map((x,k)=>String(low+k+1).padStart(4)+' '+x).join('\n')});
    if(spans.length===5)break;
 }
 records.push({file:path.relative(root,p),size:t.length,spans});
}
const report={version:'4.9.211',status:'DISCOVERY',files:records};
fs.writeFileSync(path.join(root,'BRASEIRO_TTS_INTEGRATION_DIAGNOSTIC.json'),JSON.stringify(report,null,2));
console.log('=== BRASEIRO TTS DISCOVERY ===');
console.log('Found', records.length,'files with TTS references');
for(const r of records.slice(0,16)){
 console.log('\nFILE',r.file,'length',r.size);
 for(const s of r.spans.slice(0,3))console.log('AROUND '+s.line+'\n'+s.text.slice(0,2700));
}
console.log('=== END DISCOVERY ===');
