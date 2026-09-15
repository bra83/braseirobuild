import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { resolveSkillCheckFromPhysicalDice } from '../core/wwn-rules-core.js';

const proof={provider:'ANDROID_NATIVE_PHYSICS',algorithm:'physical-dice-top-face-v1',formula:'2d6',rolls:[6,5],physicalFaces:[6,5],seed:'regression',physicsMs:123,forcedSettle:false,visualQuality:'ALTO'};

test('physical WWN skill resolver preserves settled 2d6 authority without deterministic RNG',()=>{
  const r=resolveSkillCheckFromPhysicalDice({faces:proof.rolls,skillLevel:1,attributeScore:14,difficulty:8,physicalMeta:proof});
  assert.deepEqual(r.dice.map(d=>d.result),[6,5]);
  assert.equal(r.rngAlgorithm,'physical-dice-top-face-v1');
  assert.equal(r.total,13);
  assert.equal(r.success,true);
});

test('physical WWN skill resolver rejects invalid or forced-settle proof',()=>{
  assert.throws(()=>resolveSkillCheckFromPhysicalDice({faces:[7,1],skillLevel:0,attributeScore:10,difficulty:8,physicalMeta:{...proof,rolls:[7,1]}}),/Invalid physical 2d6 faces/);
  assert.throws(()=>resolveSkillCheckFromPhysicalDice({faces:[4,4],skillLevel:0,attributeScore:10,difficulty:8,physicalMeta:{...proof,rolls:[4,4],forcedSettle:true}}),/forced physical settle is not authoritative/);
});

test('PlayerActionService persists diceAuthority instead of dropping native physical proof',()=>{
  const src=fs.readFileSync(new URL('../core/action-service.js',import.meta.url),'utf8');
  assert.ok(src.includes('diceAuthority: hasPhysical'));
  assert.ok(src.includes('physicalFaces: Object.freeze'));
  assert.ok(src.includes("provider:'DETERMINISTIC_INTERNAL'"));
});
