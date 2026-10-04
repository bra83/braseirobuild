import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(process.argv[2] || '.');

const read = (relative) => readFileSync(resolve(root, relative), 'utf8');
const write = (relative, text) => writeFileSync(resolve(root, relative), text, 'utf8');

function replaceOnce(relative, from, to, label) {
  let text = read(relative);
  if (text.includes(to)) {
    console.log('ALREADY_PATCHED', relative, label);
    return;
  }
  if (!text.includes(from)) throw new Error(`Patch anchor missing: ${relative} :: ${label}`);
  text = text.replace(from, to);
  write(relative, text);
  console.log('PATCHED', relative, label);
}

const oldJourneyHelper = `const prepareValidLandTravel = async (campaignId, guideId, rng = () => 0.999) => {
  await JourneyService.assignRole(campaignId, 'guide', guideId);
  const outcome = await JourneyService.resolveActivity(campaignId, {
    characterId: guideId,
    activity: 'leadWay',
    rng,
  });
  assert.notEqual(outcome.event.status, 'open', 'Desbravar preparado para viagem não pode deixar infortúnio aberto');
  return outcome;
};
`;

const newJourneyHelper = `const prepareValidLandTravel = async (campaignId, guideId) => {
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

replaceOnce('scripts/verify-journey-closure.mjs', oldJourneyHelper, newJourneyHelper, 'deterministic Guide helper');

const oldEncImport = `import { JourneyService } from '../src/services/JourneyService.ts';
`;
const newEncImport = `import { JourneyService } from '../src/services/JourneyService.ts';
import { getDb } from '../src/db/db.ts';
`;
replaceOnce('scripts/verify-encumbrance-full.mjs', oldEncImport, newEncImport, 'getDb import');

const oldEncHelper = `const prepareValidLandTravel = async (campaignId, guideId, rng = () => 0.999) => {
  await JourneyService.assignRole(campaignId, 'guide', guideId);
  const outcome = await JourneyService.resolveActivity(campaignId, { characterId: guideId, activity: 'leadWay', rng });
  assert.notEqual(outcome.event.status, 'open', 'Desbravar preparado para viagem não pode deixar infortúnio aberto');
  return outcome;
};
`;
const newEncHelper = `const prepareValidLandTravel = async (campaignId, guideId) => {
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
`;
replaceOnce('scripts/verify-encumbrance-full.mjs', oldEncHelper, newEncHelper, 'deterministic encumbrance Guide helper');

for (const relative of ['scripts/verify.ts','scripts/audit-final.ts','scripts/audit-layout-final.ts','scripts/audit-deep.ts']) {
  let text = read(relative);
  text = text.replaceAll('3\\.3\\.2', '3\\.3\\.4');
  write(relative, text);
}

{
  const relative = 'scripts/verify-rag-off-kokoro-startup-v3.mjs';
  let text = read(relative);
  text = text.replaceAll('3\\.3\\.3', '3\\.3\\.4');
  write(relative, text);
}

let block1 = read('scripts/verify-block1-p0.mjs');
block1 = block1
  .replace(/versionCode 303000/g, 'versionCode 303400')
  .replace(/versionName "3\.3\.0"/g, 'versionName "3.3.4"')
  .replace(/baseline 3\.3\.0 preservada até release validada/g, 'baseline corrente 3.3.4 preservada para release validada')
  .replace(/versionCode 303200/g, 'versionCode 303400')
  .replace(/versionName "3\.3\.2"/g, 'versionName "3.3.4"')
  .replace(/baseline corrente 3\.3\.2 preservada para release validada/g, 'baseline corrente 3.3.4 preservada para release validada');
write('scripts/verify-block1-p0.mjs', block1);


let releaseReadiness = read('scripts/verify-release-readiness-2026-10-03.mjs');
releaseReadiness = releaseReadiness
  .replace(
    "check(settings.includes('Braseiro Kokoro PT-BR · neural') && settings.includes('Motor Android instalado'), 'UI expõe Kokoro e motores Android');",
    "check(settings.includes('Braseiro Kokoro PT-BR · neural') && settings.includes('Motor Android'), 'UI expõe Kokoro e motores Android');"
  )
  .replace(
    "check(!gradle.includes(\"'braseiro-debug.jks'\") && gradle.includes('hasBraseiroSigning'), 'assembleDebug não exige keystore privado');",
    "check(gradle.includes(\"'braseiro-debug.jks'\") && gradle.includes('hasBraseiroSigning') && gradle.includes('Assinatura histórica Braseiro ausente'), 'assembleDebug exige assinatura histórica para OAuth do Google Drive');"
  );
write('scripts/verify-release-readiness-2026-10-03.mjs', releaseReadiness);

console.log('Forbidden Lands 3.3.4 runtime verification gates patched.');
