import React from 'react';
import type { Character } from '../lib/db';
import type { MasterState } from '../lib/master/types';

type Props = {
  masterState: MasterState;
  characters: Character[];
  activeCharacterId?: string | null;
  immersive?: boolean;
};

function Pill(props: { label: string; value: React.ReactNode; alert?: boolean }) {
  return (
    <div style={{
      flex: '0 0 auto',
      minWidth: 70,
      padding: '6px 8px',
      borderRadius: 8,
      border: '1px solid ' + (props.alert ? 'rgba(214,119,75,.78)' : 'rgba(191,151,91,.42)'),
      background: props.alert ? 'rgba(92,37,24,.88)' : 'rgba(15,12,9,.90)',
      color: '#f3e1bd',
    }}>
      <div style={{ fontSize: 8, textTransform: 'uppercase', letterSpacing: '.08em', opacity: .68 }}>{props.label}</div>
      <div style={{ fontSize: 12, fontWeight: 900, whiteSpace: 'nowrap' }}>{props.value}</div>
    </div>
  );
}

export const MechanicalStatusStrip: React.FC<Props> = ({ masterState, characters, activeCharacterId, immersive }) => {
  const active = characters.find(character => character.id === activeCharacterId) || characters[0];
  if (!active) return null;

  const a = active.sheet.attributes;
  const pendingFatigue = Math.max(0, Number(masterState.journey.travelFatigueByHero?.[active.id]) || 0);
  const routeRemaining = masterState.journey.plannedRouteHexIds?.length || 0;
  const eventState = masterState.journey.pendingEvent
    ? masterState.journey.pendingEvent.description
    : masterState.journey.awaitingEventGeneration
      ? 'ROLAR EVENTO'
      : masterState.journey.awaitingEndRoll
        ? 'CHEGADA'
        : masterState.journey.stageComplete
          ? 'ETAPA'
          : '—';
  const conditions = [
    a.wounded ? 'Ferido' : '',
    a.weary ? 'Cansado' : '',
    a.miserable ? 'Arrasado' : '',
  ].filter(Boolean).join(' · ') || 'Normal';
  const clock = masterState.worldClock;
  const summary = masterState.lastResolutionSummary;

  return (
    <div style={{
      position: immersive ? 'fixed' : 'sticky',
      top: immersive ? 'calc(env(safe-area-inset-top, 0px) + 82px)' : 0,
      left: immersive ? 8 : undefined,
      right: immersive ? 8 : undefined,
      zIndex: 86,
      pointerEvents: 'auto',
    }}>
      <div style={{
        display: 'flex',
        gap: 5,
        overflowX: 'auto',
        padding: 5,
        borderRadius: 10,
        border: '1px solid rgba(191,151,91,.32)',
        background: 'rgba(4,6,8,.88)',
        boxShadow: '0 6px 20px rgba(0,0,0,.35)',
        scrollbarWidth: 'none',
      }}>
        <Pill label="Endurance" value={String(a.enduranceCurrent) + '/' + String(a.enduranceMax)} alert={a.enduranceCurrent <= Math.max(1, Math.floor(a.enduranceMax / 3))} />
        <Pill label="Carga" value={a.load} />
        <Pill label="Fadiga" value={a.fatigue} alert={a.fatigue > 0} />
        <Pill label="Fadiga Jornada" value={pendingFatigue} alert={pendingFatigue > 0} />
        <Pill label="Esperança" value={String(a.hopeCurrent) + '/' + String(a.hopeMax)} alert={a.hopeCurrent <= 1} />
        <Pill label="Sombra" value={a.shadow} alert={a.shadow >= a.hopeCurrent} />
        <Pill label="Condições" value={conditions} alert={Boolean(a.wounded || a.weary || a.miserable)} />
        <Pill label="Sociedade" value={String(masterState.company.fellowshipCurrent) + '/' + String(masterState.company.fellowshipMax)} />
        {masterState.company.eyeAwarenessEnabled ? (
          <Pill
            label="Olho"
            value={String(masterState.company.eyeAwareness) + '/' + String(masterState.company.eyeHuntThreshold || 16)}
            alert={Boolean(masterState.company.eyeRevelationPending)}
          />
        ) : null}
        {Number(a.supplies) > 0 ? <Pill label="Provisões" value={a.supplies} /> : null}
        <Pill label="Rota" value={String(routeRemaining) + ' hex'} />
        <Pill label="Marcha" value={masterState.journey.marchHexesRemaining || 0} />
        <Pill label="Evento" value={eventState} alert={eventState !== '—'} />
        <Pill label="Tempo" value={'Dia ' + String(clock.day) + ' · ' + String(clock.hour).padStart(2, '0') + ':' + String(clock.minute).padStart(2, '0')} />
      </div>

      {summary ? (
        <details style={{
          marginTop: 3,
          marginLeft: 'auto',
          maxWidth: 440,
          borderRadius: 8,
          border: '1px solid rgba(191,151,91,.28)',
          background: 'rgba(9,8,6,.92)',
          color: '#f1dfbb',
          fontSize: 10,
        }}>
          <summary style={{ cursor: 'pointer', padding: '7px 9px', fontWeight: 900 }}>
            FIM DO TURNO · {summary.action.slice(0, 54)}
          </summary>
          <div style={{ padding: '0 9px 9px', lineHeight: 1.45 }}>
            <div><strong>Tempo:</strong> {summary.timeBefore} → {summary.timeAfter} {summary.elapsedMinutes ? '(+' + String(summary.elapsedMinutes) + ' min)' : '(sem avanço)'}</div>
            <div style={{ marginTop: 5 }}><strong>Alterações:</strong> {summary.changes.join(' · ')}</div>
            {summary.consequences.length > 0 ? <div style={{ marginTop: 5 }}><strong>Consequências:</strong> {summary.consequences.join(' · ')}</div> : null}
            <div style={{ marginTop: 5 }}><strong>Próximo turno:</strong> {summary.nextSteps.join(' · ')}</div>
          </div>
        </details>
      ) : null}
    </div>
  );
};
