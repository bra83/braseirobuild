import React from 'react';
import type { DiceRoll } from '../types';

type DieResult = DiceRoll['results'][number];

const D6_ROTATION: Record<number, { x: number; y: number }> = {
  1: { x: 0, y: 0 },
  2: { x: -90, y: 0 },
  3: { x: 0, y: -90 },
  4: { x: 0, y: 90 },
  5: { x: 90, y: 0 },
  6: { x: 0, y: 180 },
};

const dieLabel = (die: DieResult) => {
  if (die.type === 'base') return 'Base';
  if (die.type === 'skill') return 'Perícia';
  if (die.type === 'negativeSkill') return 'Perícia −';
  if (die.type === 'gear') return 'Equip.';
  if (die.type === 'artifact') return `D${die.sides}`;
  return `D${die.sides}`;
};

const toneClass = (die: DieResult) => {
  if (die.type === 'base') return 'dice3d-base';
  if (die.type === 'skill') return 'dice3d-skill';
  if (die.type === 'negativeSkill') return 'dice3d-negative';
  if (die.type === 'gear') return 'dice3d-gear';
  if (die.type === 'artifact') return 'dice3d-artifact';
  return 'dice3d-custom';
};

function D6Cube({ die, index, rollKey }: { die: DieResult; index: number; rollKey: string }) {
  const final = D6_ROTATION[die.value] || D6_ROTATION[1];
  const style = {
    '--dice-final-x': `${final.x}deg`,
    '--dice-final-y': `${final.y}deg`,
    '--dice-spin-x': `${720 + (index % 4) * 90}deg`,
    '--dice-spin-y': `${900 + (index % 5) * 90}deg`,
    '--dice-delay': `${Math.min(index * 32, 220)}ms`,
  } as React.CSSProperties;
  return <div className="dice3d-slot" aria-label={`${dieLabel(die)}: ${die.value}`}>
    <div key={`${rollKey}-${index}-${die.value}`} className={`dice3d-cube ${toneClass(die)}`} style={style}>
      <span className="dice3d-face dice3d-front">1</span>
      <span className="dice3d-face dice3d-back">6</span>
      <span className="dice3d-face dice3d-right">3</span>
      <span className="dice3d-face dice3d-left">4</span>
      <span className="dice3d-face dice3d-top">2</span>
      <span className="dice3d-face dice3d-bottom">5</span>
    </div>
    <span className="dice3d-caption">{dieLabel(die)}</span>
  </div>;
}

function PolyDie({ die, index, rollKey }: { die: DieResult; index: number; rollKey: string }) {
  const style = {
    '--dice-spin-x': `${630 + (index % 4) * 90}deg`,
    '--dice-spin-y': `${810 + (index % 5) * 72}deg`,
    '--dice-delay': `${Math.min(index * 32, 220)}ms`,
  } as React.CSSProperties;
  return <div className="dice3d-slot" aria-label={`${dieLabel(die)}: ${die.value}`}>
    <div key={`${rollKey}-${index}-${die.value}`} className={`dice3d-poly dice3d-poly-${die.sides} ${toneClass(die)}`} style={style}>
      <span className="dice3d-poly-core">{die.value}</span>
      <span className="dice3d-poly-shine" />
    </div>
    <span className="dice3d-caption">D{die.sides}</span>
  </div>;
}

export function Dice3DStage({ roll }: { roll: DiceRoll }) {
  const rollKey = `${roll.id}-${roll.isPushed ? 'push' : 'roll'}-${roll.results.map(item => `${item.sides}:${item.value}`).join('.')}`;
  return <div className="dice3d-stage" role="group" aria-label="Rolagem de dados em 3D">
    <div className="dice3d-floor" aria-hidden="true" />
    <div className="dice3d-tray">
      {roll.results.map((die, index) => die.sides === 6
        ? <D6Cube key={`${rollKey}-${index}`} die={die} index={index} rollKey={rollKey} />
        : <PolyDie key={`${rollKey}-${index}`} die={die} index={index} rollKey={rollKey} />)}
    </div>
  </div>;
}
