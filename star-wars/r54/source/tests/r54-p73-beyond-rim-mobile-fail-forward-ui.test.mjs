import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { startBeyondTheRim, adjudicateBeyondTheRimCheck, beyondTheRimStatus } from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
const css=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.css',import.meta.url),'utf8');
const failNodes=['cholganna-search','crash-trail','imperial-contingency','raxus-arrival','dropoff-point','imperial-interruption','flying-junk'];

test('P73: UI publica tracker de Além da Orla sem flags internas',()=>{
  const start=runtime.indexOf("if(tracker.kind==='beyond-the-rim')");
  const end=runtime.indexOf("const inv=tracker.investigation",start);
  const block=runtime.slice(start,end);
  assert.ok(start>=0&&end>start);
  assert.match(block,/Progresso de Além da Orla/);
  assert.doesNotMatch(block,/searchComplication|trailComplication|imperialEscapeComplication|finalEscapeComplication|finalEscapeCostly/);
});

test('P73: escolhas fail-forward continuam acionáveis em 415x915',()=>{
  assert.match(css,/@media\(max-width:415px\)/);
  assert.match(css,/\.final-published-choices button\{min-height:46px\}/);
  assert.match(css,/\.final-published-action-zone\{padding:7px\}/);
  for(const nodeId of failNodes){
    let {world}=startBeyondTheRim({campaignMode:'EOTE',party:{obligationTotal:25}});
    world.publishedAdventure.currentNodeId=nodeId;
    if(!world.publishedAdventure.visitedNodeIds.includes(nodeId)) world.publishedAdventure.visitedNodeIds.push(nodeId);
    world=adjudicateBeyondTheRimCheck(world,{checkIndex:0,result:{success:false,netAdvantage:-1}}).world;
    const s=beyondTheRimStatus(world);
    assert.ok(s.node.choices.length>0,`${nodeId} ficou sem escolha`);
    for(const c of s.node.choices){assert.ok(c.label.length<=68,`${nodeId}: rótulo excessivo: ${c.label}`);}
  }
});

test('P73: status público pós-falha não serializa spoilers de flags futuras',()=>{
  let {world}=startBeyondTheRim({campaignMode:'EOTE'});
  world.publishedAdventure.currentNodeId='cholganna-search';world.publishedAdventure.visitedNodeIds.push('cholganna-search');
  world=adjudicateBeyondTheRimCheck(world,{checkIndex:0,result:{success:false}}).world;
  const pub=JSON.stringify(beyondTheRimStatus(world).tracker);
  assert.doesNotMatch(pub,/Raxus|Scrapheap|finalEscape|imperialEscape|Harsol|Cratala/i);
});
