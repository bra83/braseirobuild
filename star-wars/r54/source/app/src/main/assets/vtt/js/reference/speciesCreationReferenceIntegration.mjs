import data from './speciesCreationReferenceCatalog.json' with {type:'json'};
export const SPECIES_CREATION_REFERENCE=Object.freeze(data);
const MODES=Object.freeze(['EOTE','AOR','MIXED']);
export function speciesForMode(mode='MIXED',query=''){
 if(!MODES.includes(mode)) throw new Error('campaign mode');
 const q=String(query).trim().toLocaleLowerCase('pt-BR');
 return SPECIES_CREATION_REFERENCE.filter(x=>(mode==='MIXED'||x.activeLines.includes(mode))&&(!q||`${x.name} ${x.references.filter(r=>r.line==='EOTE'||r.line==='AOR').map(r=>r.citation).join(' ')}`.toLocaleLowerCase('pt-BR').includes(q)));
}
export function speciesCreationCandidate(key,{mode='MIXED'}={}){
 const x=speciesForMode(mode).find(v=>v.key===key);if(!x)throw new Error('species reference');
 return {key:x.key,name:x.name,mode,mechanicallyActive:false,requiresMechanicalSourceBinding:true,references:x.references.filter(r=>r.line==='EOTE'||r.line==='AOR'),source:x.source};
}
export function speciesSupplementCoverage(){
 const byLine={EOTE:0,AOR:0},books={};
 for(const x of SPECIES_CREATION_REFERENCE){for(const l of x.activeLines)if(l in byLine)byLine[l]++;for(const r of x.references.filter(r=>r.line==='EOTE'||r.line==='AOR')){const b=r.citation.replace(/ p\.\d+.*$/,'').replace(/,$/,'');books[b]=(books[b]||0)+1;}}
 return {uniqueSpecies:SPECIES_CREATION_REFERENCE.length,byLine,books};
}
