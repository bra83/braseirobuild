import test from 'node:test';
import assert from 'node:assert/strict';
import { UNDER_BLACK_SUN, startUnderBlackSun, underBlackSunStatus } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';

test('P70: grafo publicado inteiro é alcançável a partir da abertura e não contém destinos quebrados',()=>{
 const nodes=UNDER_BLACK_SUN.nodes, seen=new Set(), q=[UNDER_BLACK_SUN.startNodeId];
 while(q.length){const id=q.shift(); if(seen.has(id))continue; seen.add(id); const n=nodes[id]; assert.ok(n,`nó ausente: ${id}`); for(const c of n.choices||[]){assert.ok(nodes[c.to],`destino ausente ${id}/${c.id} -> ${c.to}`); if(!seen.has(c.to))q.push(c.to)}}
 assert.deepEqual([...Object.keys(nodes).filter(id=>!seen.has(id))],[], 'há nós órfãos no conteúdo publicado');
});

test('P70: todos os nós jogáveis têm texto PT-BR, local e escolhas ou são epílogo terminal',()=>{
 for(const [id,n] of Object.entries(UNDER_BLACK_SUN.nodes)){
  assert.ok(String(n.title||'').trim(),`${id}: título`); assert.ok(String(n.summary||'').trim(),`${id}: resumo`); assert.ok(String(n.narration||'').trim(),`${id}: narração`); assert.ok(n.locationId,`${id}: local`);
  if(id!=='epilogue') assert.ok((n.choices||[]).length>0,`${id}: sem escolha`);
 }
});

test('P70: projeção inicial ao jogador não vaza nós futuros nem consequências finais',()=>{
 const s=underBlackSunStatus(startUnderBlackSun(createWorld()).world); const publicSurface={node:s.node,tracker:s.tracker,knownClues:s.knownClues,knownConsequences:s.knownConsequences,npcCards:s.npcCards}; const text=JSON.stringify(publicSurface);
 assert.doesNotMatch(text,/NovaCORE|kaatoOutcome|resolutionMode|obligationPendingDelta|kaato-return|zelcomm-retaliation/i);
 assert.match(text,/Perseguição/);
});
