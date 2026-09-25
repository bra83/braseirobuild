import React, { useEffect, useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { JourneyRole, MasterState } from '../lib/master/types';
import { findSkill, targetNumberForSkill } from '../lib/characterRules';

type RollMode = 'favoured' | 'normal' | 'ill-favoured';

type Props = {
  masterState: MasterState;
  characters: Character[];
  busy?: boolean;
  onSetEyeEnabled: (enabled: boolean) => Promise<void>;
  onStartJourney: (roles: Partial<Record<JourneyRole, string>>, mounted: boolean, forcedMarch: boolean) => Promise<void>;
  onRollJourney: (kind: 'march' | 'event' | 'end', characterId: string, skill: string, extraDice?: number, forcedMode?: RollMode, mountVigour?: number, spendHope?: boolean) => Promise<void>;
  onOpenNotableEvent: () => Promise<void>;
  onResolveNotableEvent: (success: boolean) => Promise<void>;
  onApplyMarch: () => Promise<void>;
  onGenerateEvent: () => Promise<void>;
  onApplyEvent: () => Promise<void>;
  onContinueStage: () => Promise<void>;
  onCompleteJourney: () => Promise<void>;
  onOpenEncounter: () => Promise<void>;
  onOpenEyeRevelation: () => Promise<void>;
  onAdjustEye: (amount: number, reason: string) => Promise<void>;
};

const roleInfo: Array<{ role: JourneyRole; title: string; skill: string; description: string }> = [
  { role: 'guide', title: 'Guia', skill: 'Viagem', description: 'Conduz a Companhia e realiza a Jogada de Marcha.' },
  { role: 'scout', title: 'Batedor', skill: 'Exploração', description: 'Encontra caminhos, passagens e locais adequados.' },
  { role: 'hunter', title: 'Caçador', skill: 'Caçada', description: 'Busca alimento e recursos e pode ser alvo de Eventos.' },
  { role: 'lookout', title: 'Vigia', skill: 'Vigilância', description: 'Percebe perigos, emboscadas e ameaças.' },
];

const box: React.CSSProperties = {
  position: 'fixed',
  left: 10,
  right: 10,
  bottom: 'calc(env(safe-area-inset-bottom, 0px) + 76px)',
  zIndex: 96,
  maxWidth: 540,
  margin: '0 auto',
  maxHeight: '48dvh',
  overflowY: 'auto',
  borderRadius: 12,
  border: '1px solid rgba(203,158,91,.68)',
  background: 'rgba(13,10,7,.97)',
  boxShadow: '0 14px 38px rgba(0,0,0,.62)',
  color: '#f3e1bd',
  padding: 12,
};

const primaryButton: React.CSSProperties = {
  width: '100%',
  minHeight: 44,
  borderRadius: 8,
  border: '1px solid #a37643',
  background: '#5a3b1d',
  color: '#fff0cf',
  fontWeight: 900,
};

function bestFor(party: Character[], skill: string): string {
  return [...party].sort((a, b) => (findSkill(b.sheet, skill)?.rank || 0) - (findSkill(a.sheet, skill)?.rank || 0))[0]?.id || '';
}

function rollText(roll: any): string {
  if (!roll) return '—';
  const feat = roll.featResult === 11 ? 'Olho de Sauron' : roll.featResult === 12 ? 'Gandalf' : 'Façanha ' + String(roll.featResult || '—');
  return (roll.success ? 'SUCESSO' : 'FALHA') + ' · ' + feat + ' · total ' + String(roll.total ?? '—') + ' · ' + String(Math.max(0, Number(roll.tengwarCount) || 0)) + ' ícone(s)';
}

export const JourneyResolutionPanel: React.FC<Props> = props => {
  const { masterState, characters } = props;
  const [roles, setRoles] = useState<Partial<Record<JourneyRole, string>>>(masterState.company.journeyRoles || {});
  const [mounted, setMounted] = useState(Boolean(masterState.journey.mounted));
  const [forcedMarch, setForcedMarch] = useState(Boolean(masterState.journey.forcedMarch));
  const [mountVigour, setMountVigour] = useState<Record<string, number>>({});
  const [spendHope, setSpendHope] = useState(false);
  const strider = masterState.company.rulesMode === 'strider' && characters.length <= 2;

  useEffect(() => {
    setRoles(masterState.company.journeyRoles || {});
  }, [masterState.journey.status, masterState.company.journeyRoles]);

  const route = masterState.journey.plannedRouteHexIds || [];
  const activeJourney = ['planned', 'travelling', 'paused'].includes(masterState.journey.status);
  const event = masterState.journey.pendingEvent;
  const pending = masterState.journey.pendingRoll;
  const requiredEnd = useMemo(
    () => characters.filter(hero => Math.max(0, Number(masterState.journey.travelFatigueByHero?.[hero.id]) || 0) > 0),
    [characters, masterState.journey.travelFatigueByHero],
  );
  const endDone = masterState.journey.endRollsByHero || {};

  if (masterState.company.eyeAwarenessEnabled === undefined) {
    return (
      <div style={box}>
        <div style={{ fontSize: 10, letterSpacing: '.12em', opacity: .72 }}>REGRA OPCIONAL</div>
        <h3 style={{ margin: '3px 0 5px', fontSize: 18 }}>Vigilância do Olho</h3>
        <p style={{ margin: '0 0 10px', fontSize: 12, lineHeight: 1.45, opacity: .84 }}>
          Quando ativada, ações que chamam a atenção do Inimigo aumentam a Vigilância. Ao alcançar o Limiar de Caça, ocorre um episódio de Revelação. A escolha fica salva na campanha.
        </p>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
          <button disabled={props.busy} onClick={() => void props.onSetEyeEnabled(true)} style={primaryButton}>Ativada</button>
          <button disabled={props.busy} onClick={() => void props.onSetEyeEnabled(false)} style={{ ...primaryButton, borderColor: '#6e6558', background: '#26221c' }}>Desativada</button>
        </div>
      </div>
    );
  }

  if (masterState.company.eyeAwarenessEnabled && masterState.company.eyeRevelationPending) {
    return (
      <div style={box}>
        <h3 style={{ margin: 0, fontSize: 17 }}>Companhia revelada ao Olho</h3>
        <p style={{ fontSize: 12, lineHeight: 1.45 }}>
          Vigilância {masterState.company.eyeAwareness}/{masterState.company.eyeHuntThreshold || 16}. A regra exige um episódio de Revelação; ele não será escolhido silenciosamente.
        </p>
        <button disabled={props.busy} onClick={() => void props.onOpenEyeRevelation()} style={primaryButton}>Abrir episódio de Revelação</button>
      </div>
    );
  }

  if (route.length > 0 && !activeJourney) {
    const valid = strider ? characters.length >= 1 && characters.length <= 2 : roleInfo.every(item => Boolean(roles[item.role]));
    return (
      <div style={box}>
        <div style={{ fontSize: 10, letterSpacing: '.12em', opacity: .72 }}>PREPARAÇÃO DA JORNADA</div>
        <h3 style={{ margin: '3px 0 8px', fontSize: 18 }}>{strider ? 'Modo Peregrino · Jornada sem papéis' : 'Escolha os papéis'}</h3>
        {strider ? (
          <div style={{ border: '1px solid rgba(191,151,91,.28)', borderRadius: 8, padding: 9, fontSize: 12, lineHeight: 1.45 }}>
            Não há papéis de Jornada nesta variante para um ou dois Heróis. Os Eventos desafiam diretamente Caçada, Exploração ou Vigilância; com dois Heróis, o alvo é determinado quando o Evento é gerado.
          </div>
        ) : (
          <>
            <div style={{ display: 'grid', gap: 7 }}>
              {roleInfo.map(info => (
                <label key={info.role} style={{ display: 'grid', gridTemplateColumns: '1fr minmax(130px,45%)', gap: 8, alignItems: 'center', border: '1px solid rgba(191,151,91,.22)', borderRadius: 8, padding: 8 }}>
                  <span>
                    <strong>{info.title}</strong> · {info.skill}
                    <small style={{ display: 'block', opacity: .72, lineHeight: 1.3 }}>{info.description}</small>
                  </span>
                  <select
                    value={roles[info.role] || ''}
                    onChange={eventChange => setRoles(current => ({ ...current, [info.role]: eventChange.target.value }))}
                    style={{ minHeight: 38, background: '#1a1510', color: '#f5e3bf', border: '1px solid #6f5433', borderRadius: 6, padding: '0 6px' }}
                  >
                    <option value="">Escolher...</option>
                    {characters.map(hero => <option key={hero.id} value={hero.id}>{hero.sheet.identity.name || hero.name}</option>)}
                  </select>
                </label>
              ))}
            </div>
            <button
              onClick={() => setRoles({
                guide: bestFor(characters, 'Viagem'),
                scout: bestFor(characters, 'Exploração'),
                hunter: bestFor(characters, 'Caçada'),
                lookout: bestFor(characters, 'Vigilância'),
              })}
              style={{ ...primaryButton, marginTop: 8, minHeight: 38, borderColor: '#655844', background: '#201b15', color: '#dcc9a9' }}
            >
              Preencher automaticamente
            </button>
          </>
        )}
        <div style={{ display: 'flex', gap: 10, marginTop: 8, fontSize: 11 }}>
          <label><input type="checkbox" checked={mounted} onChange={change => setMounted(change.target.checked)} /> Companhia montada</label>
          <label><input type="checkbox" checked={forcedMarch} onChange={change => setForcedMarch(change.target.checked)} /> Marcha forçada</label>
        </div>
        <button disabled={!valid || props.busy} onClick={() => void props.onStartJourney(strider ? {} : roles, mounted, forcedMarch)} style={{ ...primaryButton, marginTop: 9, opacity: valid ? 1 : .5 }}>
          Iniciar Jornada
        </button>
      </div>
    );
  }

  if (!activeJourney) return null;

  if (masterState.journey.encounterPending) {
    return (
      <div style={box}>
        <h3 style={{ margin: 0 }}>Encontro Fortuito</h3>
        <p style={{ fontSize: 12, lineHeight: 1.45 }}>
          O Evento foi resolvido com sucesso. Abra a cena para o Mestre apresentar os NPCs do encontro. Nenhum NPC será apagado ou criado silenciosamente.
        </p>
        <button disabled={props.busy} onClick={() => void props.onOpenEncounter()} style={primaryButton}>Abrir cena do encontro</button>
      </div>
    );
  }

  if (masterState.journey.awaitingEndRoll) {
    const allDone = requiredEnd.every(hero => Boolean(endDone[hero.id]));
    return (
      <div style={box}>
        <h3 style={{ margin: '0 0 7px' }}>Chegada · Fadiga de Jornada</h3>
        <p style={{ margin: '0 0 8px', fontSize: 12, lineHeight: 1.4 }}>
          Cada Herói com Fadiga pendente pode reduzi-la com Vigor da montaria e uma rolagem final de Viagem. A Fadiga restante só será aplicada ao confirmar o encerramento.
        </p>
        <label style={{ display:'flex', alignItems:'center', gap:7, margin:'0 0 8px', fontSize:11 }}>
          <input type="checkbox" checked={spendHope} onChange={change=>setSpendHope(change.target.checked)} />
          Gastar 1 Esperança na próxima rolagem {strider ? '· Inspirado (+2d)' : ''}
        </label>
        <div style={{ display: 'grid', gap: 6 }}>
          {requiredEnd.map(hero => {
            const fatigue = Math.max(0, Number(masterState.journey.travelFatigueByHero?.[hero.id]) || 0);
            const done = endDone[hero.id];
            return (
              <div key={hero.id} style={{ border: '1px solid rgba(191,151,91,.22)', borderRadius: 8, padding: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <strong>{hero.sheet.identity.name || hero.name}</strong><span>Fadiga pendente {fatigue}</span>
                </div>
                {done ? (
                  <div style={{ marginTop: 5, fontSize: 11 }}>{rollText(done.roll)} · Vigor {done.mountVigour || 0}</div>
                ) : (
                  <div style={{ display: 'grid', gridTemplateColumns: '90px 1fr', gap: 6, marginTop: 6 }}>
                    <input
                      type="number"
                      min={0}
                      value={mountVigour[hero.id] || 0}
                      onChange={change => setMountVigour(value => ({ ...value, [hero.id]: Math.max(0, Number(change.target.value) || 0) }))}
                      title="Vigor da montaria"
                      style={{ minHeight: 38, borderRadius: 6, background: '#18130f', color: '#f3e1bd', border: '1px solid #655034', padding: '0 6px' }}
                    />
                    <button disabled={props.busy} onClick={() => void props.onRollJourney('end', hero.id, 'Viagem', 0, undefined, mountVigour[hero.id] || 0, spendHope)} style={{ ...primaryButton, minHeight: 38 }}>
                      Rolar Viagem
                    </button>
                  </div>
                )}
              </div>
            );
          })}
          {!requiredEnd.length ? <div style={{ fontSize: 12, opacity: .8 }}>Nenhum Herói possui Fadiga de Jornada pendente.</div> : null}
        </div>
        <button disabled={!allDone || props.busy} onClick={() => void props.onCompleteJourney()} style={{ ...primaryButton, marginTop: 9, opacity: allDone ? 1 : .5 }}>
          Aplicar Fadiga restante e encerrar Jornada
        </button>
      </div>
    );
  }

  if (masterState.journey.stageComplete) {
    return (
      <div style={box}>
        <h3 style={{ margin: 0 }}>Etapa concluída</h3>
        <p style={{ fontSize: 12 }}>A rota possui mais de 20 hexes. Confirme antes de abrir a próxima etapa.</p>
        <button disabled={props.busy} onClick={() => void props.onContinueStage()} style={primaryButton}>Continuar para a próxima etapa</button>
      </div>
    );
  }

  if (masterState.journey.awaitingEventGeneration) {
    return (
      <div style={box}>
        <h3 style={{ margin: 0 }}>Evento de Jornada</h3>
        <p style={{ fontSize: 12, lineHeight: 1.45 }}>
          {strider
            ? 'A Marcha foi consumida. Role o Dado de Façanha do Evento e um Dado de Sucesso para revelar o Detalhe. O Evento desafiará diretamente uma Perícia — sem papéis.'
            : 'A Marcha foi consumida. Agora role explicitamente o alvo do Evento: 1–2 Batedor, 3–4 Vigia, 5–6 Caçador, além do Dado de Façanha conforme a região.'}
        </p>
        <button disabled={props.busy} onClick={() => void props.onGenerateEvent()} style={primaryButton}>{strider ? 'Rolar Evento e Detalhe' : 'Rolar alvo e Evento'}</button>
      </div>
    );
  }

  if (event) {
    const targetId = masterState.company.journeyRoles[event.targetRole] || '';
    const target = characters.find(hero => hero.id === targetId);
    const staged = pending?.kind === 'event' ? pending : null;
    const modeLabel = event.rollMode === 'favoured' ? 'Favorecida' : event.rollMode === 'ill-favoured' ? 'Desfavorecida' : 'Normal';
    return (
      <div style={box}>
        <div style={{ fontSize: 10, opacity: .72 }}>
          EVENTO · alvo d6 {event.targetDie ?? '—'} · Façanha {(event.featResults || []).join('/') || event.featResult || '—'}
        </div>
        <h3 style={{ margin: '3px 0 5px' }}>{event.description}</h3>
        {event.detail ? <div style={{ marginBottom: 7, borderRadius: 7, background: 'rgba(191,151,91,.09)', padding: 7, fontSize: 11 }}><strong>Detalhe:</strong> {event.detail}</div> : null}
        <p style={{ fontSize: 12, lineHeight: 1.45, margin: '0 0 7px' }}>
          {target?.sheet.identity.name || target?.name || 'Herói'} · {event.skill} · {modeLabel} · modificador de dados {Number(event.successDiceModifier || 0) >= 0 ? '+' : ''}{event.successDiceModifier || 0}.
        </p>
        {event.notable && !staged ? (
          <>
            <p style={{ fontSize: 11, lineHeight: 1.45, opacity: .8 }}>Encontro Notável: resolva a cena estendida. Depois informe se o resultado geral foi favorável ou desfavorável para aplicar as consequências da Jornada.</p>
            <button disabled={props.busy} onClick={() => void props.onOpenNotableEvent()} style={{...primaryButton, marginBottom:7}}>Abrir cena do Encontro Notável</button>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 7 }}>
              <button disabled={props.busy} onClick={() => void props.onResolveNotableEvent(true)} style={primaryButton}>Resultado favorável</button>
              <button disabled={props.busy} onClick={() => void props.onResolveNotableEvent(false)} style={{...primaryButton, background:'#40231c'}}>Resultado desfavorável</button>
            </div>
          </>
        ) : staged ? (
          <>
            <div style={{ border: '1px solid rgba(191,151,91,.32)', borderRadius: 8, padding: 8, fontWeight: 900 }}>{rollText(staged.result)}</div>
            <button disabled={props.busy} onClick={() => void props.onApplyEvent()} style={{ ...primaryButton, marginTop: 8 }}>Aplicar consequência do Evento</button>
          </>
        ) : (
          <>
            <label style={{ display:'flex', alignItems:'center', gap:7, margin:'0 0 7px', fontSize:11 }}>
              <input type="checkbox" checked={spendHope} onChange={change=>setSpendHope(change.target.checked)} />
              Gastar 1 Esperança {strider ? '· Peregrino fica Inspirado (+2d)' : ''}
            </label>
            <button
              disabled={!target || props.busy}
              onClick={() => target && void props.onRollJourney('event', target.id, event.skill, Number(event.successDiceModifier || 0), event.rollMode, 0, spendHope)}
              style={primaryButton}
            >
              Rolar {event.skill} com {target?.sheet.identity.name || target?.name || 'Herói'}
            </button>
          </>
        )}
      </div>
    );
  }

  if ((masterState.journey.marchHexesRemaining || 0) <= 0 && route.length > 0) {
    const guideId = masterState.company.journeyRoles.guide || '';
    const guide = characters.find(hero => hero.id === guideId);
    const staged = pending?.kind === 'march' ? pending : null;
    const tn = guide ? targetNumberForSkill(guide.sheet, 'Viagem') : 14;
    return (
      <div style={box}>
        <div style={{ fontSize: 10, opacity: .72 }}>JOGADA DE MARCHA</div>
        <h3 style={{ margin: '3px 0 5px' }}>Guia · {guide?.sheet.identity.name || guide?.name || 'não definido'}</h3>
        <p style={{ fontSize: 12, lineHeight: 1.45, margin: '0 0 7px' }}>
          Perícia Viagem · ND {tn}. Sucesso autoriza 3 + ícones de Sucesso hexes; falha autoriza 2 na primavera/verão ou 1 no outono/inverno.
        </p>
        {staged ? (
          <>
            <div style={{ border: '1px solid rgba(191,151,91,.32)', borderRadius: 8, padding: 8, fontWeight: 900 }}>{rollText(staged.result)}</div>
            <button disabled={props.busy} onClick={() => void props.onApplyMarch()} style={{ ...primaryButton, marginTop: 8 }}>Aplicar resultado da Marcha</button>
          </>
        ) : (
          <>
            <label style={{ display:'flex', alignItems:'center', gap:7, margin:'0 0 7px', fontSize:11 }}>
              <input type="checkbox" checked={spendHope} onChange={change=>setSpendHope(change.target.checked)} />
              Gastar 1 Esperança {strider ? '· Peregrino fica Inspirado (+2d)' : ''}
            </label>
            <button disabled={!guide || props.busy} onClick={() => guide && void props.onRollJourney('march', guide.id, 'Viagem', 0, undefined, 0, spendHope)} style={primaryButton}>Rolar Marcha</button>
          </>
        )}
      </div>
    );
  }

  if (masterState.company.eyeAwarenessEnabled) {
    return (
      <details style={{ ...box, maxHeight: 170 }}>
        <summary style={{ cursor: 'pointer', fontWeight: 900 }}>Vigilância do Olho · ajustes do Mestre</summary>
        <p style={{ fontSize: 11, opacity: .78 }}>Para exibição de magia, a regra opcional exige julgamento do Mestre: menor +1, maior +2, poderosa +3.</p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 6 }}>
          <button onClick={() => void props.onAdjustEye(1, '+1 por exibição menor de magia.')} style={{ ...primaryButton, minHeight: 36 }}>Magia +1</button>
          <button onClick={() => void props.onAdjustEye(2, '+2 por exibição maior de magia.')} style={{ ...primaryButton, minHeight: 36 }}>Magia +2</button>
          <button onClick={() => void props.onAdjustEye(3, '+3 por exibição poderosa de magia.')} style={{ ...primaryButton, minHeight: 36 }}>Magia +3</button>
        </div>
      </details>
    );
  }

  return null;
};
