/** P234 — Inventory audit, never substitute for PDF source-to-code certification. */
import {writeFileSync} from 'node:fs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {FIVE_PUBLISHED_ADVENTURE_IDS,assertFivePublishedAdventures} from '../app/src/main/assets/vtt/js/adventure/hybridFiveAdventurePolicy.mjs';
import {VERIFIED_RULE_CORPUS} from '../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs';
import {validateAdventureModule} from '../app/src/main/assets/vtt/js/adventure/adventureSchema.mjs';
const result={checkpoint:'P234',kind:'IMPLEMENTATION_INVENTORY_NOT_OFFICIAL_CERTIFICATION',hybrid:{mechanics:'deterministic executors',ruleLookup:'RAG with source and reference',narration:'Gemini proposal validated by local controller; offline fallback'},officialPdfComparisonComplete:false,androidCompilationPerformed:false,adventures:[]};
const ids=publishedAdventureCatalog().map(x=>x.id);
assertFivePublishedAdventures(ids);
for(const id of FIVE_PUBLISHED_ADVENTURE_IDS){
  const adapter=getPublishedAdventureAdapter(id),meta=adapter.meta;
  validateAdventureModule(meta);
  const nodes=Object.entries(meta.nodes);
  const visited=new Set([meta.startNodeId]),queue=[meta.startNodeId];
  while(queue.length){
    const n=meta.nodes[queue.shift()];
    for(const choice of n.choices??[]) if(!visited.has(choice.to)){visited.add(choice.to);queue.push(choice.to);}
  }
  const counts={nodes:nodes.length,reachableNodes:visited.size,unreachableNodeIds:nodes.map(([key])=>key).filter(key=>!visited.has(key)),episodesDeclared:[...new Set(nodes.map(([,n])=>n.episode).filter(x=>x!=null))].sort(),choices:nodes.reduce((x,[,n])=>x+(n.choices?.length??0),0),authoredChecks:nodes.reduce((x,[,n])=>x+(n.checks?.length??0),0),terminalNodes:nodes.filter(([,n])=>(n.choices?.length??0)===0).map(([key])=>key)};
  result.adventures.push({id,title:meta.title,sourceCode:meta.source?.productCode??null,campaignModes:[...meta.campaignModes],...counts,
    graphValidated:true,sourceToScenePdfCertified:false,allOfficialEncounterStatsCertified:false,completePlaythroughOnAndroid:false});
}
result.rules={auditedCorpusEntries:VERIFIED_RULE_CORPUS.length,entriesMarkedTestado:VERIFIED_RULE_CORPUS.filter(x=>x.status==='TESTADO').length,externalPdfCoverageNotProven:true,eoteAoRExpansionSemanticCertification:false};
result.total={modules:ids.length,nodes:result.adventures.reduce((n,a)=>n+a.nodes,0),graphUnreachable:result.adventures.reduce((n,a)=>n+a.unreachableNodeIds.length,0),checks:result.adventures.reduce((n,a)=>n+a.authoredChecks,0)};
const file=new URL('../validation/P234_HYBRID_COVERAGE_LEDGER.json',import.meta.url);
writeFileSync(file,JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify({adventures:result.adventures.map(a=>({id:a.id,nodes:a.nodes,episodes:a.episodesDeclared,unreachable:a.unreachableNodeIds.length})),totals:result.total,corpusEntries:result.rules.auditedCorpusEntries}));
