import type { CampaignState, Character } from './db';
import { normalizeMasterState } from './master/state';

function clockLabel(campaign: CampaignState): string {
  const clock = normalizeMasterState(campaign).worldClock;
  return 'Dia ' + String(clock.day) + ' · ' + String(clock.hour).padStart(2, '0') + ':' + String(clock.minute).padStart(2, '0') + ' · ' + String(clock.season);
}

function activeHero(campaign: CampaignState, characters: Character[]): Character | undefined {
  const id = campaign.activeCharacterId || campaign.partyCharacterIds?.[0];
  return characters.find(character => character.id === id) || characters[0];
}

function addChange(changes: string[], label: string, before: unknown, after: unknown) {
  if (String(before) !== String(after)) changes.push(label + ': ' + String(before) + ' → ' + String(after));
}

export function attachResolutionSummary(
  beforeCampaign: CampaignState,
  afterCampaign: CampaignState,
  beforeCharacters: Character[],
  afterCharacters: Character[],
  action: string,
): CampaignState {
  const before = normalizeMasterState(beforeCampaign);
  const after = normalizeMasterState(afterCampaign);
  const changes: string[] = [];
  const consequences: string[] = [];
  const nextSteps: string[] = [];
  const beforeHero = activeHero(beforeCampaign, beforeCharacters);
  const afterHero = activeHero(afterCampaign, afterCharacters);

  if (beforeHero && afterHero && beforeHero.id === afterHero.id) {
    const a = beforeHero.sheet.attributes;
    const b = afterHero.sheet.attributes;
    addChange(changes, 'Endurance', a.enduranceCurrent, b.enduranceCurrent);
    addChange(changes, 'Esperança', a.hopeCurrent, b.hopeCurrent);
    addChange(changes, 'Sombra', a.shadow, b.shadow);
    addChange(changes, 'Fadiga', a.fatigue, b.fatigue);
    addChange(changes, 'Carga', a.load, b.load);
    addChange(changes, 'Provisões', a.supplies, b.supplies);
    if (!a.wounded && b.wounded) consequences.push((afterHero.sheet.identity.name || afterHero.name) + ': Ferido');
    if (!a.weary && b.weary) consequences.push((afterHero.sheet.identity.name || afterHero.name) + ': Cansado');
    if (!a.miserable && b.miserable) consequences.push((afterHero.sheet.identity.name || afterHero.name) + ': Arrasado');
  }

  addChange(changes, 'Sociedade', before.company.fellowshipCurrent, after.company.fellowshipCurrent);
  if (after.company.eyeAwarenessEnabled) addChange(changes, 'Vigilância do Olho', before.company.eyeAwareness, after.company.eyeAwareness);
  addChange(changes, 'Posição', before.position.currentHexId || '—', after.position.currentHexId || '—');
  addChange(changes, 'Marcha restante', before.journey.marchHexesRemaining || 0, after.journey.marchHexesRemaining || 0);
  addChange(changes, 'Rota restante', (before.journey.plannedRouteHexIds || []).length, (after.journey.plannedRouteHexIds || []).length);

  const beforePendingFatigue = Object.values(before.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0);
  const afterPendingFatigue = Object.values(after.journey.travelFatigueByHero || {}).reduce((sum, value) => sum + Math.max(0, Number(value) || 0), 0);
  addChange(changes, 'Fadiga de Jornada pendente', beforePendingFatigue, afterPendingFatigue);

  const beforeNpcIds = beforeCampaign.presentNpcIds || [];
  const afterNpcIds = afterCampaign.presentNpcIds || [];
  const entered = afterNpcIds.filter(id => !beforeNpcIds.includes(id));
  const left = beforeNpcIds.filter(id => !afterNpcIds.includes(id));
  if (entered.length) consequences.push('NPC entrou na cena: ' + entered.join(', '));
  if (left.length) consequences.push('NPC saiu da cena: ' + left.join(', '));
  if (!before.journey.pendingEvent && after.journey.pendingEvent) consequences.push('Evento de Jornada: ' + after.journey.pendingEvent.description);
  if (!before.journey.awaitingEventGeneration && after.journey.awaitingEventGeneration) consequences.push('Evento de Jornada aguardando rolagem');
  if (!before.journey.encounterPending && after.journey.encounterPending) consequences.push('Encontro Fortuito aguardando abertura de cena');
  if (!before.company.eyeRevelationPending && after.company.eyeRevelationPending) consequences.push('Companhia revelada ao Olho: episódio de Revelação pendente');

  if (after.journey.pendingRoll?.kind === 'march') nextSteps.push('Confirmar o resultado da Jogada de Marcha.');
  else if (after.journey.pendingRoll?.kind === 'event') nextSteps.push('Confirmar o teste de ' + after.journey.pendingRoll.skill + ' do Evento.');
  else if (after.journey.pendingEvent) nextSteps.push('Rolar ' + after.journey.pendingEvent.skill + ' para resolver ' + after.journey.pendingEvent.description + '.');
  else if (after.journey.awaitingEventGeneration) nextSteps.push('Rolar alvo e Dado de Façanha do Evento de Jornada.');
  else if (after.journey.awaitingEndRoll) nextSteps.push('Fazer as rolagens finais de Viagem e encerrar a Jornada.');
  else if (after.journey.stageComplete) nextSteps.push('Confirmar o início da próxima etapa da Jornada.');
  else if (after.journey.encounterPending) nextSteps.push('Abrir a cena do Encontro Fortuito.');
  else if (['planned', 'travelling', 'paused'].includes(after.journey.status) && (after.journey.plannedRouteHexIds || []).length > 0 && Math.max(0, Number(after.journey.marchHexesRemaining) || 0) <= 0) nextSteps.push('O Guia precisa fazer uma Jogada de Marcha.');
  else nextSteps.push('O jogador pode escolher a próxima ação.');

  const beforeMinutes = Math.max(0, Number(before.worldClock.elapsedMinutes) || 0);
  const afterMinutes = Math.max(0, Number(after.worldClock.elapsedMinutes) || 0);
  const summary = {
    id: 'resolution_' + String(Date.now()),
    action: action.trim() || 'Ação resolvida',
    timeBefore: clockLabel(beforeCampaign),
    timeAfter: clockLabel(afterCampaign),
    elapsedMinutes: Math.max(0, afterMinutes - beforeMinutes),
    changes: changes.length ? changes : ['Nenhuma alteração mecânica.'],
    consequences,
    nextSteps,
    createdAt: Date.now(),
  };

  return {
    ...afterCampaign,
    masterState: { ...after, lastResolutionSummary: summary, updatedAt: Date.now() },
    updatedAt: Date.now(),
  };
}
