import assert from 'node:assert/strict'; import * as g from './galaxyGraphDarkElfR49.mjs';
assert.equal(g.coverage51().verifiedEdgesR51,18); assert.equal(g.coverage51().verifiedEdgesTotal,38); assert.equal(g.coverage51().locationEnrichmentR51,16);
assert.equal(g.neighbors51('Enarc').length,4); assert.equal(g.neighbors51('Kwenn').length,2); assert.equal(g.neighbors51('Socorro').length,2);
let p=g.routePlan51('Naboo','Zhar'); assert.equal(p.legs.length,3); assert.equal(p.totalHours,30.4); assert.equal(p.mechanicalMutation,false); assert.equal(p.requiresMasterValidation,true);
p=g.routePlan51('Tatooine','Reuss'); assert.equal(p.legs.length,4); assert.equal(p.totalHours,60.8);
assert.equal(g.routePlan51('Hinakuu','Sanctuary').legs.length,1); assert.equal(g.routePlan51('Kwenn','Nal Hutta').totalHours,30.4);
assert.equal(g.LOCATION_ENRICHMENT_R51.some(x=>x.name==='D’Qar'),true); assert.equal(g.LOCATION_ENRICHMENT_R51.some(x=>x.name==='Barab I'),true);
console.log('GALAXY_GRAPH_R51 15/15 PASS',JSON.stringify(g.coverage51()));
