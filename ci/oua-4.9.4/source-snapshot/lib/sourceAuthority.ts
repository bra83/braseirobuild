import type { Manual } from './db';
export type SourceAuthority = 'core-2e'|'supplement-2e'|'adventure-2e'|'solo-2e'|'legacy-1e'|'lore-reference'|'unknown';
const CORE_IDS=new Set(['15umbJCMoLAortDPoicWeIHOzaREI9PBg','1mOdsOfqQASmLNqMZZIOz6aPatJHV1zhq','1M1rIXVz1L4Qzz98l5_aTF1c8sOokW4uA']);
const SOURCE_ID_AUTHORITY=new Map<string,SourceAuthority>([
  ['15mAAieBwcvOfP9zqO54da-L-_-Cx9hFp','supplement-2e'], // Valfenda
  ['15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto','supplement-2e'], // Povos das Terras Ermas
  ['15uDo5XgBG17xCrr_g2tbmfuIYh8AGpXf','supplement-2e'], // Trilhas de Personagens 2e
  ['15Zq2VPLjH6ys87LlHcH4e1yVakDcX6K-','adventure-2e'], // Contos das Terras Solitárias
  ['15gUwEnPRaD6f1FjXs0ho_j8VS5Rq5saY','solo-2e'], // Modo Peregrino
]);
function norm(v:unknown){return String(v??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();}
export function classifySource(manual: Pick<Manual,'name'|'driveFileId'>): SourceAuthority {
  if (manual.driveFileId && CORE_IDS.has(manual.driveFileId)) return 'core-2e';
  if (manual.driveFileId && SOURCE_ID_AUTHORITY.has(manual.driveFileId)) return SOURCE_ID_AUTHORITY.get(manual.driveFileId)!;
  const n=norm(manual.name);
  if (/manual 2e|livro basico.*2e|core.*2e/.test(n)) return 'core-2e';
  if (/modo peregrino|strider mode/.test(n)) return 'solo-2e';
  if (/livro do aventureiro|livro do mestre|1e|primeira edicao|edicao brasileira beta/.test(n)) return 'legacy-1e';
  if (/complete guide|middle-earth|terra media/.test(n) && !/2e|valfenda|terras ermas/.test(n)) return 'lore-reference';
  if (/contos|tales|aventura|adventure/.test(n)) return 'adventure-2e';
  if (/2ed|2e|valfenda|rivendell|terras ermas|wild lands|trilhas de personagens/.test(n)) return 'supplement-2e';
  return 'unknown';
}
export function mayProvideMechanics(manual: Pick<Manual,'name'|'driveFileId'>): boolean { const a=classifySource(manual); return a==='core-2e'||a==='supplement-2e'; }
export function mayProvideNarrativeContext(manual: Pick<Manual,'name'|'driveFileId'>): boolean { const a=classifySource(manual); return a==='core-2e'||a==='supplement-2e'||a==='adventure-2e'||a==='lore-reference'; }
