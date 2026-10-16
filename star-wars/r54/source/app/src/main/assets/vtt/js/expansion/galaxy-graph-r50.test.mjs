import assert from 'node:assert/strict'; import * as g from './galaxyGraphDarkElfR49.mjs';
assert.equal(g.coverage50().verifiedEdgesTotal,20); assert.equal(g.coverage50().locationEnrichmentR50,5);
assert.equal(g.neighbors50('Ord Mantell').some(x=>x.destination==='Orinda'),true);
let p=g.routePlan50('Ord Mantell','Muunilinst'); assert.equal(p.legs.length,5); assert.equal(p.totalDistance,3.9); assert.equal(p.totalHours,93.6); assert.equal(p.mechanicalMutation,false);
p=g.routePlan50('Teth','Dubrava'); assert.equal(p.legs.length,2); assert.equal(p.totalHours,33.6);
assert.equal(g.routePlan50('Fedalle','Kuat').legs.length,1); assert.equal(g.routePlan50('Fedalle','Kuat').requiresMasterValidation,true);
console.log('GALAXY_GRAPH_R50 10/10 PASS',JSON.stringify(g.coverage50()));
