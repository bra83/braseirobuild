import test from 'node:test';
import assert from 'node:assert/strict';
import { renownPurchaseCost } from '../work/core/wwn-major-project-core.js';

test('70 Renown points remain fully priced because the SRD has a final next-64 band',()=>{
  const c=renownPurchaseCost(70);
  assert.equal(c.buyablePoints,70);
  assert.equal(c.prohibitivelyExpensivePoints,0);
  assert.equal(c.costSilver,874000);
  assert.equal(c.fullyPriced,true);
});

test('the six SRD money bands price exactly the first 128 Renown points',()=>{
  const c=renownPurchaseCost(128);
  assert.equal(c.buyablePoints,128);
  assert.equal(c.prohibitivelyExpensivePoints,0);
  assert.equal(c.costSilver,2730000);
  assert.equal(c.fullyPriced,true);
});

test('only Renown beyond the first 128 points is prohibitively expensive',()=>{
  const c=renownPurchaseCost(129);
  assert.equal(c.buyablePoints,128);
  assert.equal(c.prohibitivelyExpensivePoints,1);
  assert.equal(c.costSilver,2730000);
  assert.equal(c.fullyPriced,false);
});
