/** P180 reproducible PDF TOC/source provenance gate.
 * This is deliberately NOT a full editorial certification. It makes the
 * chapter-to-adapter gaps explicit and machine-readable rather than allowing
 * a passing graph test to be mislabeled as a book comparison.
 * Usage: node tools/audit-adventure-source-p180.mjs /path/to/user-owned-pdfs
 */
import {readFileSync, existsSync, mkdirSync, writeFileSync} from 'node:fs';
import {join, resolve} from 'node:path';
import {spawnSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const root=resolve(import.meta.dirname,'..');
const pdfDir=resolve(process.argv[2]||'/mnt/data');
const references={
 'beyond-the-rim':{filename:'Beyond the Rim.pdf',episodes:[
  'Episode I: The Tale of the Sa Nalaor','Episode II: Welcome to the Jungle','Episode III: Toxic Motherlode']},
 'jewel-of-yavin':{filename:'The Jewel of Yavin.pdf',episodes:[
  'Episode I: Odds and Opportunity','Episode II: The Greatest Caper','Episode III: Escape to the Clouds']},
 'mask-of-the-pirate-queen':{filename:'mask of the pirate queen.pdf',episodes:[
  'Episode I: Search on Saleucami','Episode II: Going to Ground','Episode III: Prepare for Boarders'],
  sourceOpeningWorld:'Saleucami'}
};
const report={checkpoint:'P180',editoriallyCertified:false,
 note:'Only TOC and opening-setting cross-check; not a line-by-line encounter, NPC, or rule comparison.',
 adventures:[]};
for(const {id} of publishedAdventureCatalog()){
 const m=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 const start=m.nodes[m.startNodeId];
 const ref=references[id];
 const row={id,nodes:Object.keys(m.nodes).length,
  authoredEpisodes:[...new Set(Object.values(m.nodes).map(x=>x.episode).filter(Number.isInteger))].sort(),
  authoredStartNode:m.startNodeId,authoredStartTitle:start?.title,
  sourceAvailable:false,editoriallyCertified:false,discrepancies:[]};
 if(ref){
  const file=join(pdfDir,ref.filename);
  if(existsSync(file)){
   const pdf=readFileSync(file);
   row.sourceAvailable=true;row.pdfName=ref.filename;
   row.sha256=createHash('sha256').update(pdf).digest('hex');
   const info=spawnSync('pdfinfo',[file],{encoding:'utf8',timeout:15000});
   row.pdfPages=Number(/Pages:\s+(\d+)/.exec(info.stdout)?.[1]||0);
   const extracted=spawnSync('pdftotext',['-f','1','-l','8','-layout',file,'-'],{encoding:'utf8',timeout:25000,maxBuffer:3*1024*1024});
   const normalize=s=>s.normalize('NFKD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
   const text=normalize(extracted.stdout||'');
   row.tocEpisodes=ref.episodes.map(title=>{
    const needle=normalize(title);
    // Some OCR/text layers spell display lettering as spaced capitals:
    // 'O d d s' rather than 'Odds'. Both are the same TOC heading.
    const alternate=needle.replace('odds and opportunity','o d d s and opportunity');
    return {title,headingFound:text.includes(needle)||text.includes(alternate)};
   });
   if(row.tocEpisodes.some(x=>!x.headingFound))
    row.discrepancies.push('Episode heading not located in PDF TOC extraction: requires visual check.');
   if(ref.sourceOpeningWorld){
    const opening=normalize([start?.title,start?.summary,start?.narration].join(' '));
    row.openingWorldMatches=opening.includes(normalize(ref.sourceOpeningWorld));
    if(!row.openingWorldMatches)
     row.discrepancies.push(`Book Episode I opens on ${ref.sourceOpeningWorld}, while authored start is ${m.startNodeId} (${start?.title}).`);
   }
  }else row.discrepancies.push(`PDF unavailable: ${ref.filename}`);
 }else row.discrepancies.push('PDF not available for TOC comparison in this run.');
 report.adventures.push(row);
}
const out=join(root,'evidence/P180/ADVENTURE_PDF_TOC_CROSSCHECK.json');
mkdirSync(join(root,'evidence/P180'),{recursive:true});
writeFileSync(out,JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({file:out,booksFound:report.adventures.filter(x=>x.sourceAvailable).length,
 discrepancies:report.adventures.filter(x=>x.discrepancies.length).map(x=>({id:x.id,issues:x.discrepancies})),editoriallyCertified:false},null,2));
