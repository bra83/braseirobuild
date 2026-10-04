import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(process.argv[2] || '.');

function patchFile(relative, operations) {
  const path = resolve(root, relative);
  let text = readFileSync(path, 'utf8');
  for (const [from, to, label] of operations) {
    if (!text.includes(from)) {
      if (text.includes(to)) {
        console.log('ALREADY_PATCHED', relative, label);
        continue;
      }
      throw new Error(`Patch anchor not found: ${relative} :: ${label}`);
    }
    text = text.replace(from, to);
    console.log('PATCHED', relative, label);
  }
  writeFileSync(path, text, 'utf8');
}

const helperJourney = `  return { db, campaign, hero1, hero2, hexA02, hexB01, hexB03, hexWater };
};

const prepareValidLandTravel = async (campaignId, guideId) => {
  await JourneyService.assignRole(campaignId, 'guide', guideId);
  const db = await getDb();
  const guide = await db.get('characters', guideId);
  const baseDice = Math.max(1, Math.trunc(Number(guide?.attributes?.wits?.current) || 0));
  let rollIndex = 0;
  const successRng = () => rollIndex++ < baseDice ? 0.999 : 0;
  const outcome = await JourneyService.resolveActivity(campaignId, {
    characterId: guideId,
    activity: 'leadWay',
    rng: successRng,
  });
  assert.notEqual(outcome.event.status, 'open', 'Desbravar preparado para viagem não pode deixar infortúnio aberto');
  return outcome;
};

`;

patchFile('scripts/verify-journey-closure.mjs', [
  [
    `  return { db, campaign, hero1, hero2, hexA02, hexB01, hexB03, hexWater };
};

`,
    helperJourney,
    'helper prepareValidLandTravel',
  ],
  [
    `  await db.put('campaign', { ...campaign, currentHexId: 'B01' });

  const result1 = await TravelCommitService.commit({
`,
    `  await db.put('campaign', { ...campaign, currentHexId: 'B01' });

  await assert.rejects(
    () => TravelCommitService.commit({ target: hexB03, periods: 0.5, distanceKm: 5, actorId: hero1.id }),
    /Guia/,
  );
  await prepareValidLandTravel(campaign.id, hero1.id);

  const result1 = await TravelCommitService.commit({
`,
    'manual guide rejection + valid preparation',
  ],
  [
    `  assert.equal(forcedChecks.length, 2);

  const commitResult = await TravelCommitService.commit({
`,
    `  assert.equal(forcedChecks.length, 2);
  await prepareValidLandTravel(campaign.id, hero1.id);

  const commitResult = await TravelCommitService.commit({
`,
    'forced march with guide',
  ],
  [
    `  assert.equal(checks[0].roll.skillName, 'animal_handling');

  const commitRes = await TravelCommitService.commit({
`,
    `  assert.equal(checks[0].roll.skillName, 'animal_handling');
  await prepareValidLandTravel(campaign.id, hero1.id);

  const commitRes = await TravelCommitService.commit({
`,
    'mounted forced march with guide',
  ],
  [
    `  // Avançar primeira etapa da rota sem encontro bloqueante
  const step1 = await JourneyService.resumeJourneyRoute(campaign.id, () => 0.1);
`,
    `  // Avançar primeira etapa da rota sem encontro bloqueante
  await prepareValidLandTravel(campaign.id, campaign.activeCharacterId);
  const step1 = await JourneyService.resumeJourneyRoute(campaign.id, () => 0.1);
`,
    'planned route with guide',
  ],
]);

patchFile('scripts/verify-encumbrance-full.mjs', [
  [
    `import { JourneyRulesEngine } from '../src/services/JourneyRulesEngine.ts';

`,
    `import { JourneyRulesEngine } from '../src/services/JourneyRulesEngine.ts';
import { JourneyService } from '../src/services/JourneyService.ts';
import { getDb } from '../src/db/db.ts';

`,
    'JourneyService + getDb import',
  ],
  [
    `const character = (overrides = {}) => ({ weapons: [], armor: [], gear: [], coins: { copper: 0, silver: 0, gold: 0 }, attributes, ...overrides });

`,
    `const character = (overrides = {}) => ({ weapons: [], armor: [], gear: [], coins: { copper: 0, silver: 0, gold: 0 }, attributes, ...overrides });

const prepareValidLandTravel = async (campaignId, guideId) => {
  await JourneyService.assignRole(campaignId, 'guide', guideId);
  const db = await getDb();
  const guide = await db.get('characters', guideId);
  const baseDice = Math.max(1, Math.trunc(Number(guide?.attributes?.wits?.current) || 0));
  let rollIndex = 0;
  const successRng = () => rollIndex++ < baseDice ? 0.999 : 0;
  const outcome = await JourneyService.resolveActivity(campaignId, { characterId: guideId, activity: 'leadWay', rng: successRng });
  assert.notEqual(outcome.event.status, 'open', 'Desbravar preparado para viagem não pode deixar infortúnio aberto');
  return outcome;
};

`,
    'land travel helper',
  ],
  [
    `  await db.put('world_state', WorldStateService.empty(campaign.id));
  const checks = await TravelCommitService.prepareOverloadChecks({ sceneId: campaign.id, targetId: target.id, periods: 1, characterIds: [hero.id], rng: () => 0 });
`,
    `  await db.put('world_state', WorldStateService.empty(campaign.id));
  await prepareValidLandTravel(campaign.id, hero.id);
  const checks = await TravelCommitService.prepareOverloadChecks({ sceneId: campaign.id, targetId: target.id, periods: 1, characterIds: [hero.id], rng: () => 0 });
`,
    'overload travel with guide',
  ],
  [
    `  await db.put('world_state',{...WorldStateService.empty(campaign.id),journey:{...WorldStateService.empty(campaign.id).journey,walkingDay:1,walkingQuartersToday:2}});
  const thirdChecks=await TravelCommitService.prepareForcedMarchChecks({sceneId:campaign.id,targetId:first.id,rng:()=>.999});
`,
    `  await db.put('world_state',{...WorldStateService.empty(campaign.id),journey:{...WorldStateService.empty(campaign.id).journey,walkingDay:1,walkingQuartersToday:2}});
  await prepareValidLandTravel(campaign.id, hero.id);
  const thirdChecks=await TravelCommitService.prepareForcedMarchChecks({sceneId:campaign.id,targetId:first.id,rng:()=>.999});
`,
    'third forced march guide',
  ],
  [
    `  assert.equal(third.worldState.journey.walkingQuartersToday,3);
  const fourthChecks=await TravelCommitService.prepareForcedMarchChecks({sceneId:campaign.id,targetId:second.id,rng:()=>.999});
`,
    `  assert.equal(third.worldState.journey.walkingQuartersToday,3);
  await prepareValidLandTravel(campaign.id, hero.id);
  const fourthChecks=await TravelCommitService.prepareForcedMarchChecks({sceneId:campaign.id,targetId:second.id,rng:()=>.999});
`,
    'fourth forced march guide',
  ],
  [
    `  await db.put('world_state',{...WorldStateService.empty(failedCampaign.id),journey:{...WorldStateService.empty(failedCampaign.id).journey,walkingDay:1,walkingQuartersToday:2}});
  const failedChecks=await TravelCommitService.prepareForcedMarchChecks({sceneId:failedCampaign.id,targetId:first.id,rng:()=>0});
`,
    `  await db.put('world_state',{...WorldStateService.empty(failedCampaign.id),journey:{...WorldStateService.empty(failedCampaign.id).journey,walkingDay:1,walkingQuartersToday:2}});
  await prepareValidLandTravel(failedCampaign.id, hero.id);
  const failedChecks=await TravelCommitService.prepareForcedMarchChecks({sceneId:failedCampaign.id,targetId:first.id,rng:()=>0});
`,
    'failed forced march guide',
  ],
  [
    `  assert.equal(checks[0].roll.results.filter(die=>die.type==='base').length,1);
  await assert.rejects(()=>TravelCommitService.commit({target,periods:0.1,distanceKm:1,actorId:rider.id,forcedMarchChecks:[]}),/shorter than the terrain cost/);
`,
    `  assert.equal(checks[0].roll.results.filter(die=>die.type==='base').length,1);
  await prepareValidLandTravel(campaign.id, rider.id);
  await assert.rejects(()=>TravelCommitService.commit({target,periods:0.1,distanceKm:1,actorId:rider.id,forcedMarchChecks:[]}),/shorter than the terrain cost/);
`,
    'mounted forced march guide',
  ],
  [
    `  await db.put('individual_hexes',target);
  await assert.rejects(()=>TravelCommitService.commit({target,periods:0.5,distanceKm:5,actorId:character.id}),/interrompeu a marcha/);
  const repeated=await JourneyService.resolveWeatherTravelChecks(campaign.id,()=>.999);
  assert.equal(repeated.alreadyResolved,true);
  const path=await JourneyService.resolveActivity(campaign.id,{characterId:character.id,activity:'leadWay',rng:()=>.999});
  assert.equal(path.event.result.weatherModifier,-2);
`,
    `  await db.put('individual_hexes',target);
  const path=await prepareValidLandTravel(campaign.id, character.id);
  assert.equal(path.event.result.weatherModifier,-2);
  await assert.rejects(()=>TravelCommitService.commit({target,periods:0.5,distanceKm:5,actorId:character.id}),/interrompeu a marcha/);
  const repeated=await JourneyService.resolveWeatherTravelChecks(campaign.id,()=>.999);
  assert.equal(repeated.alreadyResolved,true);
`,
    'weather-stopped travel with guide',
  ],
]);

console.log('Forbidden Lands 3.3.2 stale travel verification gates updated for mandatory Guide + Desbravar.');
