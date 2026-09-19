import {
  createCampaignSnapshot, getStoreData, listSoundMetadata, openDb, recoverLatestCampaignSnapshot, recoverPendingCanonicalTransactions,
  saveCanonicalTransaction, saveMasterTransaction, saveStoreData, type CampaignState, type Character, type Manual, type Npc,
} from '../db';
import { reconcileCanonicalState } from '../consistency';
import { hasGeminiApiKey } from '../apiKey';
import { loadMapDiscovery, loadRuntimeMapManifest } from '../map';
import { rebuildAssetRegistry } from './assetRegistry';
import { migrateCampaignToAutonomy350 } from './migrationManager';
import { recoverCampaignIfNeeded } from './recoveryManager';
import { restoreCanonicalProjection, verifyCanonicalProjection } from './canonicalProjector';
import { buildAutonomousEnvironmentContext } from './environment';
import { resolveRulesProfile } from './rulesProfile';
import { verifyRulesFolder } from '../rulesSync';
import type { HealthCheckItem, HealthReport, HealthStatus, TurnTransaction } from './types';

function check(id: string, label: string, status: HealthStatus, detail: string, repaired = false): HealthCheckItem { return { id, label, status, detail, repaired, updatedAt: Date.now() }; }
function overall(items: HealthCheckItem[]): HealthStatus {
  if (items.some(item => item.status === 'error')) return 'error';
  if (items.some(item => item.status === 'warning')) return 'warning';
  if (items.some(item => item.status === 'untested')) return 'untested';
  return 'ok';
}

export interface StartupSupervisorResult { campaign: CampaignState; characters: Character[]; npcs: Npc[]; report: HealthReport; }

async function closeStaleBackupRestores(campaignId: string, repairs: string[]): Promise<{ interrupted: number; failed: number }> {
  const rows = await getStoreData<any>('backup_restore_staging').catch(() => []);
  const relevant = rows.filter(row => row.campaignId === campaignId);
  let interrupted = 0;
  for (const row of relevant.filter(row => row.status === 'validated')) {
    await saveStoreData('backup_restore_staging', {
      ...row,
      status: 'interrupted-before-commit',
      updatedAt: Date.now(),
      error: 'A aplicação foi encerrada depois da validação e antes da transação de restauração; o estado anterior permaneceu intacto.',
    }, 'neutralização de restauração interrompida');
    interrupted += 1;
  }
  if (interrupted) repairs.push(`${interrupted} tentativa(s) de restauração interrompida(s) foram neutralizadas antes do commit.`);
  return { interrupted, failed: relevant.filter(row => row.status === 'failed').length };
}

async function closeStaleTurns(campaignId: string, repairs: string[]): Promise<number> {
  const rows = await getStoreData<TurnTransaction>('turn_transactions').catch(() => []);
  const stale = rows.filter(row => row.campaignId === campaignId && ['preparing','mechanics-applied'].includes(row.status) && Date.now() - row.updatedAt > 90_000);
  for (const row of stale) await saveStoreData('turn_transactions', { ...row, status: 'aborted', error: 'Turno interrompido; a outbox canónica foi recuperada ou o estado anterior permaneceu válido.', updatedAt: Date.now() } satisfies TurnTransaction, 'autorreparo de turno interrompido');
  if (stale.length) repairs.push(`${stale.length} turno(s) interrompido(s) foram neutralizados sem reaplicar consequências.`);
  return stale.length;
}

async function nativeHealthItem(): Promise<HealthCheckItem[]> {
  if (!window.BraseiroAndroid) return [check('native-runtime', 'Android nativo', 'untested', 'Executando fora do APK; áudio, TTS, armazenamento e permissões não foram testados.')];
  try {
    const status = await window.BraseiroAndroid.getNativeHealthStatus?.();
    if (!status) return [check('native-runtime', 'Android nativo', 'warning', 'Ponte presente, mas o probe nativo não respondeu.')];
    const storage = status.storage || {};
    const audio = status.audio || {};
    const tts = status.tts || {};
    const drive = status.drive || {};
    const gemini = status.gemini || {};
    const freeBytes = Number(storage.freeBytes || 0);
    const importJob = audio.latestImportJob || null;
    const importState = String(importJob?.status || 'idle');
    return [
      check('native-origin', 'Origem segura da WebView', status.originTrusted ? 'ok' : 'error', status.originTrusted ? 'Ponte nativa limitada à origem local do aplicativo.' : 'A WebView não está na origem local confiável.'),
      check('native-storage', 'Armazenamento Android', storage.writable && freeBytes > 256 * 1024 * 1024 ? 'ok' : 'error', storage.writable ? `${(freeBytes / 1024 / 1024 / 1024).toFixed(2)} GB livres, com margem mínima de 256 MB.` : `Diretório privado não está gravável: ${storage.error || 'falha desconhecida'}.`),
      check('native-audio', 'Catálogo de áudio nativo', audio.status === 'verified' ? (Number(audio.unavailableCount || 0) ? 'warning' : 'ok') : 'untested', `${Number(audio.catalogCount || 0)} ficheiro(s) no SQLite; ${Number(audio.unavailableCount || 0)} indisponível(is).`),
      check('native-import', 'Importação nativa retomável', ['failed','permission_lost','interrupted'].includes(importState) ? 'warning' : importState === 'idle' ? 'untested' : 'ok', importJob ? `Último trabalho: ${importState}; ${Number(importJob.imported || 0)} importado(s), ${Number(importJob.failed || 0)} falha(s).` : 'Nenhuma importação nativa foi executada neste aparelho.'),
      check('android-tts', 'TTS Android', tts.status === 'verified' ? 'ok' : 'warning', tts.ready ? 'Mecanismo TTS inicializado.' : 'TTS não disponível ou ainda não inicializado.'),
      check('native-drive', 'Autorização Google Drive', drive.status === 'verified' ? 'ok' : 'untested', drive.authorized ? 'Token protegido presente; acesso ainda deve ser confirmado pela sincronização.' : 'Conta não autorizada neste aparelho.'),
      check('native-gemini', 'Gemini nativo', gemini.status === 'configured_not_tested' ? 'untested' : 'warning', gemini.freeKey || gemini.paidKey ? 'Chave protegida configurada; quota/conectividade não foram testadas nesta abertura.' : 'Nenhuma chave nativa configurada.'),
    ];
  } catch (error) {
    return [check('native-runtime', 'Android nativo', 'warning', `Probe nativo falhou: ${error instanceof Error ? error.message : String(error)}.`)];
  }
}


export async function runStartupSupervisor(
  campaign: CampaignState,
  characters: Character[],
  npcs: Npc[],
  repair = true,
  verifyRemoteRules = false,
): Promise<StartupSupervisorResult> {
  const items: HealthCheckItem[] = [];
  const repairs: string[] = [];
  await openDb();
  items.push(check('database', 'Banco principal', 'ok', 'IndexedDB abriu e concluiu uma transação de leitura.'));
  const outboxRecovered = repair ? await recoverPendingCanonicalTransactions() : 0;
  items.push(check('transaction-outbox', 'Outbox de transações', 'ok', outboxRecovered ? `${outboxRecovered} transação(ões) pendente(s) recuperada(s).` : 'Nenhuma transação canónica pendente.', outboxRecovered > 0));
  if (outboxRecovered) repairs.push(`${outboxRecovered} transação(ões) recuperada(s) pela outbox.`);

  const recovery = await recoverCampaignIfNeeded(campaign);
  campaign = recovery.campaign;
  items.push(check('recovery-manager', 'Gerenciador de recuperação', 'ok', recovery.detail, recovery.recovered));
  if (recovery.recovered) repairs.push(recovery.detail);

  const migration = await migrateCampaignToAutonomy350(campaign);
  if (migration.report) {
    campaign = await saveCanonicalTransaction({ campaign: migration.campaign, characters, npcs, reason: 'migração estrutural 3.5.0' });
    repairs.push(`Campanha migrada do schema ${migration.report.fromSchema} para ${migration.report.toSchema}.`);
    items.push(check('migration-350', 'Migração estrutural', 'ok', `Schema ${migration.report.fromSchema} → ${migration.report.toSchema}.`, true));
  } else items.push(check('migration-350', 'Migração estrutural', 'ok', 'Schema 350 já ativo.'));

  const discovery = await loadMapDiscovery(campaign.id);
  // O supervisor só precisa de IDs e geometria de leitura. O manifesto completo
  // também hidratava o PNG importado e os datasets pesados durante o cold start.
  const manifest = await loadRuntimeMapManifest().catch(() => null);
  if (!manifest) items.push(check('map', 'Mapa', 'error', 'Manifesto cartográfico não pôde ser carregado.'));
  else if (manifest.hexes.length < 12_000) items.push(check('map', 'Mapa', 'warning', `Manifesto possui apenas ${manifest.hexes.length} hexes.`));
  else items.push(check('map', 'Mapa', 'ok', `${manifest.hexes.length.toLocaleString('pt-BR')} hexes válidos em ${manifest.image.width} × ${manifest.image.height}.`));

  let canonicalCampaign = campaign;
  let canonicalCharacters = characters;
  let canonicalNpcs = npcs;
  const mapHex = discovery.currentHexId || null;
  const masterHex = campaign.masterState?.position.currentHexId || null;
  if (mapHex !== masterHex) {
    if (repair) {
      const reconciled = reconcileCanonicalState(campaign, characters, npcs, discovery, { mapWins: true });
      canonicalCampaign = await saveMasterTransaction(reconciled.campaign, reconciled.characters, [], 'autorreparo de posição na inicialização', reconciled.npcs, reconciled.discovery);
      canonicalCharacters = reconciled.characters; canonicalNpcs = reconciled.npcs;
      repairs.push(`Posição reconciliada em ${reconciled.discovery.currentHexId || 'nenhum hex'}.`);
      items.push(check('map-position', 'Posição canónica', 'ok', `Mapa e campanha alinhados em ${reconciled.discovery.currentHexId || 'nenhum hex'}.`, true));
    } else items.push(check('map-position', 'Posição canónica', 'warning', `Mapa=${mapHex || 'nenhum'}; Mestre=${masterHex || 'nenhum'}.`));
  } else items.push(check('map-position', 'Posição canónica', mapHex ? 'ok' : 'warning', mapHex ? `Hex ${mapHex}.` : 'Campanha ainda não posicionada; ambiente não pode ser verificado.'));

  const snapshots = (await getStoreData<{ id: string; campaignId: string; createdAt: number; data?: CampaignState }>('snapshots').catch(() => []))
    .filter(item => item.campaignId === campaign.id).sort((a, b) => b.createdAt - a.createdAt);
  if (!snapshots.length && repair) {
    await createCampaignSnapshot(canonicalCampaign, 'snapshot inicial da correção estrutural 3.5');
    repairs.push('Snapshot preventivo inicial criado.');
    items.push(check('snapshot', 'Recuperação', 'ok', 'Snapshot preventivo criado.', true));
  } else items.push(check('snapshot', 'Recuperação', snapshots.length ? 'ok' : 'warning', snapshots.length ? `Último snapshot: ${new Date(snapshots[0].createdAt).toLocaleString('pt-BR')}.` : 'Nenhum snapshot encontrado.'));
  const latestSnapshot = await recoverLatestCampaignSnapshot(campaign.id).catch(() => null);
  const snapshotAhead = latestSnapshot && (Number(latestSnapshot.revision || 0) > Number(canonicalCampaign.revision || 0) || Number(latestSnapshot.updatedAt || 0) > Number(canonicalCampaign.updatedAt || 0));
  items.push(check('snapshot-ahead', 'Integridade da revisão', snapshotAhead ? 'warning' : 'ok', snapshotAhead ? 'Há snapshot com revisão/data superior; a recuperação usará revisão e digest, não apenas tamanho do histórico.' : `Banco na revisão ${canonicalCampaign.revision || 0}.`));

  const staleTurns = await closeStaleTurns(campaign.id, repairs);
  items.push(check('turns', 'Turnos interrompidos', 'ok', staleTurns ? `${staleTurns} turno(s) antigo(s) neutralizado(s).` : 'Nenhum turno incompleto reaplicável.', staleTurns > 0));
  const restoreStaging = await closeStaleBackupRestores(campaign.id, repairs);
  items.push(check('backup-restore-staging', 'Restauração de backup', restoreStaging.failed ? 'warning' : 'ok', restoreStaging.failed
    ? `${restoreStaging.failed} tentativa(s) anterior(es) falharam; os dados atuais não foram substituídos.`
    : restoreStaging.interrupted ? `${restoreStaging.interrupted} tentativa(s) interrompida(s) foram neutralizadas antes do commit.` : 'Nenhuma restauração incompleta pendente.', restoreStaging.interrupted > 0));

  let projection = await verifyCanonicalProjection(canonicalCampaign);
  if (repair && projection.status === 'diverged' && projection.snapshot) {
    canonicalCampaign = await restoreCanonicalProjection(projection.snapshot, 'autorreparo da projeção canónica na inicialização');
    canonicalCharacters = projection.snapshot.characters || canonicalCharacters;
    canonicalNpcs = projection.snapshot.npcs || canonicalNpcs;
    repairs.push('Campanha, elenco, mapa e ambiente foram reconstruídos pelo último checkpoint canónico completo.');
    projection = await verifyCanonicalProjection(canonicalCampaign);
    items.push(check('canonical-projection', 'Projeção do diário', projection.status === 'ok' ? 'ok' : 'warning', projection.detail, true));
  } else {
    items.push(check('canonical-projection', 'Projeção do diário', projection.status === 'ok' ? 'ok' : 'warning', projection.detail));
  }

  const profile = resolveRulesProfile(canonicalCampaign);
  const allManuals = await getStoreData<Manual>('manuals').catch(() => []);
  const manuals = allManuals.filter(item => item.profileId === profile.id || item.rulesSourceFolderId === profile.sourceFolderId || (!item.profileId && item.system === profile.displayName));
  const ready = manuals.filter(item => item.indexed && item.chunkCount > 0 && (!item.indexState || item.indexState === 'ready'));
  const incomplete = manuals.filter(item => !item.indexed || item.chunkCount <= 0 || item.indexState === 'failed');
  items.push(check('manuals', 'Manuais do perfil', incomplete.length ? 'warning' : ready.length ? 'ok' : 'warning', `${ready.length} prontos; ${incomplete.length} incompletos; perfil ${profile.id}.`));
  if (profile.sourceFolderId && verifyRemoteRules) {
    const sync = await verifyRulesFolder(profile.id, profile.sourceFolderId, allManuals);
    items.push(check('rules-sync', 'Pasta de regras', sync.status === 'failed' ? 'warning' : sync.status === 'changes-detected' ? 'warning' : 'ok', sync.status === 'current' ? `${sync.files.length} arquivo(s) verificado(s), sem alterações.` : sync.status === 'changes-detected' ? `${sync.changedFileIds.length} alterado(s) e ${sync.missingFileIds.length} ausente(s); reimporte antes da sessão.` : `Falha: ${sync.error || 'desconhecida'}.`));
  } else if (profile.sourceFolderId) {
    items.push(check('rules-sync', 'Pasta de regras', 'untested', 'Verificação remota adiada para a Biblioteca de Manuais ou para a Central de Saúde.'));
  } else items.push(check('rules-sync', 'Pasta de regras', 'warning', 'O perfil ativo não possui pasta de regras vinculada.'));

  const sounds = await listSoundMetadata().catch(() => []);
  const failedSounds = sounds.filter(sound => sound.available === false || (sound.failCount || 0) >= 3);
  items.push(check('audio-catalog', 'Catálogo de áudio', failedSounds.length ? 'warning' : sounds.length ? 'ok' : 'warning', `${sounds.length.toLocaleString('pt-BR')} catalogados; ${failedSounds.length} indisponíveis/desativados.`));
  const [tokenRows, individualRows, npcCatalog] = await Promise.all([
    getStoreData<{ id: string }>('tokens').catch(() => []), getStoreData<{ id: string }>('individual_tokens').catch(() => []), getStoreData<{ id: string }>('npc_token_catalog').catch(() => []),
  ]);
  const missingNpcTokens = canonicalNpcs.filter(npc => npc.presentInScene && !npc.tokenId).length;
  items.push(check('tokens', 'Tokens', missingNpcTokens ? 'warning' : 'ok', `${tokenRows.length + individualRows.length + npcCatalog.length} registros; ${missingNpcTokens} NPC(s) presente(s) sem retrato.`));

  const geminiReady = hasGeminiApiKey('free') || hasGeminiApiKey('paid');
  items.push(check('gemini', 'Gemini', geminiReady ? 'ok' : 'warning', geminiReady ? 'Ao menos uma chave está configurada.' : 'Sem chave; o modo local degradado permanece disponível.'));
  items.push(...await nativeHealthItem());

  const environment = await buildAutonomousEnvironmentContext(canonicalCampaign).catch(() => null);
  if (environment?.currentHexId) {
    const nearest = environment.nearbySettlements[0];
    items.push(check('environment', 'Leitura ambiental do mapa', 'ok', `${environment.currentHexId} · ${environment.climateZone} · ${environment.currentWeather.kind}${environment.snowEligible ? ' · neve elegível' : ' · neve bloqueada'}${nearest ? ` · ${nearest.name} a ${nearest.distanceHexes} hex(es)` : ''}.`));
  } else items.push(check('environment', 'Leitura ambiental do mapa', 'warning', 'Sem hex canónico; clima e aproximações ainda não foram validados.'));

  const registry = await rebuildAssetRegistry(canonicalCampaign).catch(() => []);
  items.push(check('assets', 'Registro central de recursos', registry.length ? 'ok' : 'warning', `${registry.length.toLocaleString('pt-BR')} recursos verificados.`, registry.length > 0));
  const report: HealthReport = { id: `health:${campaign.id}:${Date.now()}`, campaignId: campaign.id, createdAt: Date.now(), overall: overall(items), items, repairs };
  await saveStoreData('health_reports', report, 'relatório de saúde 3.5');
  await saveStoreData('app_meta', { id: 'lastHealthReport', value: report.id, updatedAt: Date.now() }, 'ponte do relatório de saúde');
  return { campaign: canonicalCampaign, characters: canonicalCharacters, npcs: canonicalNpcs, report };
}

export async function getLatestHealthReport(campaignId: string): Promise<HealthReport | null> {
  const rows = await getStoreData<HealthReport>('health_reports').catch(() => []);
  return rows.filter(item => item.campaignId === campaignId).sort((a, b) => b.createdAt - a.createdAt)[0] || null;
}
