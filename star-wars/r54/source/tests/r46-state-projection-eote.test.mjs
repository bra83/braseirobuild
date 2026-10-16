import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createEoteCoreCharacter } from '../app/src/main/assets/vtt/js/rules/eoteCharacterCreation.mjs';

const bundle = fs.readFileSync('app/src/main/assets/vtt/r46/r46-shell.bundle.js','utf8');

function loadBundleModule(name) {
  const marker = `define("${name}",function(module,exports,require){`;
  const start = bundle.indexOf(marker);
  assert.notEqual(start, -1, `module ${name} missing`);
  const bodyStart = start + marker.length;
  const next = bundle.indexOf('\ndefine("', bodyStart);
  const raw = bundle.slice(bodyStart, next === -1 ? bundle.length : next);
  const end = raw.lastIndexOf('\n});');
  const body = end >= 0 ? raw.slice(0, end) : raw;
  const module = { exports: {} };
  const fn = new Function('module','exports','require', body);
  fn(module, module.exports, () => { throw new Error(`unexpected require in ${name}`); });
  return module.exports;
}

test('R46 projection accepts the canonical EOTE schema 14 state', () => {
  const projection = loadBundleModule('r21Projection');
  assert.equal(projection.R21_STATE_SCHEMA, 14);
  const state = createCampaign('Teste EotE');
  const view = projection.projectR21CampaignState(state);
  assert.equal(view.status, 'ready');
  assert.equal(view.campaignName, 'Teste EotE');
});

test('R46 projection validates state.group and no longer requires state.rebelGroup', () => {
  const projection = loadBundleModule('r21Projection');
  const state = createCampaign('Grupo EotE');
  assert.ok(state.group);
  assert.equal('rebelGroup' in state, false);
  assert.equal(projection.projectR21CampaignState(state).status, 'ready');
  const broken = structuredClone(state);
  delete broken.group;
  assert.equal(projection.projectR21CampaignState(broken).status, 'unavailable');
});

test('compiled R46 projection contains no active rebelGroup dependency', () => {
  const start = bundle.indexOf('define("r21Projection"');
  const next = bundle.indexOf('\ndefine("', start + 1);
  const moduleText = bundle.slice(start, next);
  assert.match(moduleText, /R21_STATE_SCHEMA = 14/);
  assert.match(moduleText, /record\(state\.group\)/);
  assert.doesNotMatch(moduleText, /record\(state\.rebelGroup\)/);
});


test('R46 character projection shows EotE Obligation instead of obsolete Duty slot', () => {
  const projection = loadBundleModule('r21Projection');
  const state = createCampaign('Personagem EotE');
  state.player.character = createEoteCoreCharacter({
    name:'Nira', speciesKey:'human', careerKey:'smuggler', specializationKey:'pilot', partySize:1,
    careerFreeSkills:['coordinacion','engano','bajos_fondos','percepcion'],
    specializationFreeSkills:['astronavegacion','artilleria'], speciesChoiceSkills:['atletismo','medicina'],
    motivation:'Pagar a dívida e manter a tripulação viva.', obligationType:'Dívida', obligationBenefits:{}
  });
  const view = projection.projectR21CampaignState(state);
  assert.equal(view.status, 'ready');
  const campaign = view.character.state.find(item => item.label === 'Obligation');
  assert.ok(campaign);
  assert.match(campaign.value, /20/);
  assert.match(campaign.value, /Dívida/);
  assert.equal(view.character.state.some(item => item.label === 'Mecânica de campanha'), false);
});
