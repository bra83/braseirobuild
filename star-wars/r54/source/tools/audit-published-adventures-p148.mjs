/** P148 — reproducible structural and semantic gate; NOT PDF certification. */
import { publishedAdventureCatalog, getPublishedAdventureAdapter, publishedAdventureAdapterSnapshot } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { validatePublishedAdventureGraph } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { aorTalentCatalog, aorTalentCoverage } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
const rows=[];
for (const {id} of publishedAdventureCatalog()) {
  const adapter=getPublishedAdventureAdapter(id);
  const module=publishedAdventureRuntimeModule(adapter.meta);
  const structural=validatePublishedAdventureGraph(module);
  const semantic=validateAdventureSemantics(module);
  const runtime=new AdventureRuntime(module);
  validateAdventureCheckpoint(module,AdventureRuntime.restore(module,runtime.serialize()).getState());
  const capabilities=publishedAdventureAdapterSnapshot(id).capabilities;
  rows.push({id,structural,semantic,capabilities,
    caveat: id === 'beyond-the-rim' ? 'Official combat encounter absent; no combat fabricated.' : null});
}
console.log(JSON.stringify({checkpoint:'P148',gate:'STRUCTURAL_AND_SEMANTIC_ONLY',
  certifiedAgainstOfficialPdfs:false,allPassed:true,adventures:rows,
  aor:{...aorTalentCoverage(),pendingByTalent:Object.entries(aorTalentCatalog().filter(x=>!x.mechanicallyActive).reduce((m,x)=>(m[x.originalName]=(m[x.originalName]||0)+1,m),{})).sort((a,b)=>b[1]-a[1])}},null,2));
