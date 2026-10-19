import fs from 'node:fs';
import path from 'node:path';
const root=process.argv[2];
if(!root||!fs.existsSync(path.join(root,'package.json')))throw Error('OUA root not found');
const pkg=JSON.parse(fs.readFileSync(path.join(root,'package.json'),'utf8'));
console.log('PROJECT ROOT',root);
console.log('PACKAGE',JSON.stringify({name:pkg.name,scripts:pkg.scripts},null,2));
console.log('DIRECTORY',fs.readdirSync(root).slice(0,90));
const skipped=new Set(['node_modules','.git','dist','build','.gradle','.next','out','vendor','resources','assets','gradle','.idea']);
function crawl(p,depth=0){if(depth>10)return[];return fs.readdirSync(p,{withFileTypes:true}).flatMap(x=>
x.isDirectory()?skipped.has(x.name)?[]:crawl(path.join(p,x.name),depth+1):
/\.(tsx?|jsx?|java|kt)$/.test(x.name)?[path.join(p,x.name)]:[]);}
const re=/generateTTS|generateTts|generateSpeech|speechConfig|prebuiltVoiceConfig|synthesizeSpeech|generateContent.*AUDIO|textToSpeech|speechSynthesis|speakWith|TextToSpeech|VOICE_NAME|voiceName/i;
const candidates=crawl(root).filter(p=>fs.statSync(p).size<1_200_000).map(p=>({p,t:fs.readFileSync(p,'utf8')})).filter(x=>re.test(x.t));
const records=[];
for(const {p,t} of candidates){
 const lines=t.split(/\r?\n/);const spans=[];
 for(let i=0;i<lines.length;i++)if(re.test(lines[i])){
   const low=Math.max(0,i-9),high=Math.min(lines.length,i+17);
   spans.push({line:i+1,text:lines.slice(low,high).map((x,k)=>String(low+k+1).padStart(4)+' '+x).join('\n')});
   if(spans.length===5)break;
 }
 records.push({file:path.relative(root,p),size:t.length,spans});
}
const report={version:'4.9.211',status:'DISCOVERY',package:pkg.name,files:records};
fs.writeFileSync(path.join(root,'BRASEIRO_TTS_INTEGRATION_DIAGNOSTIC.json'),JSON.stringify(report,null,2));
console.log('=== BRASEIRO TTS DISCOVERY ===');
console.log('Found', records.length,'files');
for(const r of records.slice(0,18)){
 console.log('\nFILE',r.file,'length',r.size);
 for(const s of r.spans.slice(0,3))console.log('AROUND '+s.line+'\n'+s.text.slice(0,2700));
}
console.log('=== END DISCOVERY ===');
