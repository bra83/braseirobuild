import { AudioDiagnostics } from './audioDiagnostics';
import type { CampaignState, Character, Npc } from './db';
import { getStoreData, saveStoreData } from './db';
import { validateCanonicalState, type ConsistencyCheck } from './consistency';
import { loadMapDiscovery, loadMapManifest } from './map';
import { APP_VERSION } from './version';

export interface HomologationCheck extends ConsistencyCheck {
  group: 'estado' | 'mapa' | 'audio' | 'persistencia' | 'conteudo';
}

export interface FinalHomologationReport {
  id: string;
  campaignId: string;
  appVersion: string;
  generatedAt: number;
  checks: HomologationCheck[];
  summary: { pass: number; warning: number; fail: number; ready: boolean };
  notes: string[];
}

function check(group: HomologationCheck['group'], id: string, label: string, status: HomologationCheck['status'], detail: string): HomologationCheck {
  return { group, id, label, status, detail };
}

export async function runFinalHomologation(campaign: CampaignState): Promise<FinalHomologationReport> {
  const [characters, npcs, discovery, manifest, migrationReports] = await Promise.all([
    getStoreData<Character>('characters'),
    getStoreData<Npc>('npcs'),
    loadMapDiscovery(campaign.id),
    loadMapManifest(),
    getStoreData<{ id: string; campaignId?: string; completedAt?: number }>('migration_reports'),
  ]);
  const canonical = validateCanonicalState(campaign, characters, npcs, discovery);
  const checks: HomologationCheck[] = canonical.checks.map(item => ({ ...item, group: 'estado' }));
  checks.push(check('mapa', 'map-manifest', 'Geometria oficial do mapa', manifest?.hexes?.length ? 'pass' : 'fail', manifest ? `${manifest.hexes.length} hexes carregados.` : 'Manifesto indisponível.'));
  checks.push(check('mapa', 'map-position', 'Posição persistida no atlas', discovery.currentHexId || !campaign.history.length ? 'pass' : 'warning', discovery.currentHexId || 'A Companhia ainda não foi posicionada.'));
  checks.push(check('persistencia', 'history', 'Histórico preservado', Array.isArray(campaign.history) ? 'pass' : 'fail', `${campaign.history?.length || 0} cenas.`));
  checks.push(check('persistencia', 'schema', 'Migração 3.0 aplicada', Number(campaign.schemaVersion || 0) >= 300 && Number(campaign.masterState?.schemaVersion || 0) >= 3 ? 'pass' : 'fail', `Campanha ${campaign.schemaVersion || 0}; Mestre ${campaign.masterState?.schemaVersion || 0}.`));
  const migration = migrationReports.filter(item => item.campaignId === campaign.id).sort((a, b) => Number(b.completedAt || 0) - Number(a.completedAt || 0))[0];
  checks.push(check('persistencia', 'migration-report', 'Relatório de migração', migration || Number(campaign.schemaVersion || 0) >= 300 ? 'pass' : 'warning', migration ? `Migração registrada em ${new Date(Number(migration.completedAt || 0)).toLocaleString('pt-BR')}.` : 'Campanha já criada no schema 3.0; migração legada não foi necessária.'));
  checks.push(check('conteudo', 'multi-hero', 'Mesa multi-herói', (campaign.partyCharacterIds || []).length >= 1 ? 'pass' : 'fail', `${campaign.partyCharacterIds?.length || 0} Heróis-jogadores.`));

  const audioEvents = AudioDiagnostics.snapshot(false);
  const channelStarted = (channel: string) => audioEvents.some(event => event.channel === channel && event.level === 'success' && /start|started|playing|playback|native|ready|conclu/i.test(`${event.action} ${event.result || ''} ${event.message}`));
  const audioErrors = audioEvents.filter(event => event.level === 'error');
  checks.push(check('audio', 'audio-diagnostics', 'Diagnóstico de áudio executado', audioEvents.length ? (audioErrors.length ? 'warning' : 'pass') : 'warning', audioEvents.length ? `${audioEvents.length} eventos; ${audioErrors.length} erros.` : 'Execute o autoteste na aba Sons para homologação física.'));
  for (const [channel, label] of [['voice', 'Voz'], ['music', 'Música'], ['ambience', 'Ambiência'], ['sfx', 'SFX']] as const) {
    checks.push(check('audio', `audio-${channel}`, label, channelStarted(channel) ? 'pass' : 'warning', channelStarted(channel) ? 'Início confirmado no diagnóstico.' : 'Ainda sem confirmação física nesta instalação.'));
  }

  const report: FinalHomologationReport = {
    id: `homologation_${campaign.id}_${Date.now()}`,
    campaignId: campaign.id,
    appVersion: APP_VERSION,
    generatedAt: Date.now(),
    checks,
    summary: {
      pass: checks.filter(item => item.status === 'pass').length,
      warning: checks.filter(item => item.status === 'warning').length,
      fail: checks.filter(item => item.status === 'fail').length,
      ready: checks.every(item => item.status !== 'fail'),
    },
    notes: [
      'Avisos de áudio significam que o canal ainda precisa ser confirmado fisicamente no aparelho; não bloqueiam os dados da campanha.',
      'Uma falha impede a homologação e deve ser corrigida antes de continuar a campanha.',
    ],
  };
  await saveStoreData('homologation_reports', report, 'homologação final 3.0');
  return report;
}
