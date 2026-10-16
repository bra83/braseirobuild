import test from 'node:test';
import assert from 'node:assert/strict';
import { initiativeEntry, sortInitiative, buildInitiativeSlots } from '../app/src/main/assets/vtt/js/rules/initiative.mjs';

test('iniciativa ordena por Êxitos, Vantagens, Triunfos e favorece PJ no empate final',()=>{
  const entries=[
    initiativeEntry({id:'n1',side:'npc',check:{netSuccess:2,netAdvantage:1,triumph:0}}),
    initiativeEntry({id:'p1',side:'player',check:{netSuccess:2,netAdvantage:1,triumph:0}}),
    initiativeEntry({id:'n2',side:'npc',check:{netSuccess:1,netAdvantage:5,triumph:1}}),
    initiativeEntry({id:'p2',side:'player',check:{netSuccess:2,netAdvantage:2,triumph:0}})
  ];
  assert.deepEqual(sortInitiative(entries).map(x=>x.id),['p2','p1','n1','n2']);
  assert.deepEqual(buildInitiativeSlots(entries).map(x=>x.side),['player','player','npc','npc']);
});
