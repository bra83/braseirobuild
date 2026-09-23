export interface RollResult {
  id: string;
  timestamp: number;
  notation: string;
  total: number;
  modifier: number;
  results: number[];
  type: 'standard' | 'one-ring';
  featResult?: number; // selected Feat Die, 1 to 12
  featResults?: number[]; // all Feat Dice rolled for favoured/ill-favoured tests
  rollMode?: 'normal' | 'favoured' | 'ill-favoured';
  successResults?: number[]; // 1 to 6
  targetNumber?: number;
  skillName?: string;
  success?: boolean;
  successLevel?: 'failure' | 'success' | 'great-success' | 'extraordinary-success';
  tengwarCount?: number;
  characterId?: string;
  characterName?: string;
  weary?: boolean;
  miserable?: boolean; // legacy storage name; UI calls this Arrasado
  physics?: {
    engine: string;
    mode: 'physical_3d' | 'fallback';
    seedFeat?: string;
    seedSuccess?: string;
    seed?: string;
    stepsFeat?: number;
    stepsSuccess?: number;
    steps?: number;
    forcedSettle?: boolean;
  };
}

// Cryptographically secure and exactly uniform random integer in range [min, max].
// Rejection sampling avoids the small modulo bias of `random % range`.
export function secureRoll(min: number, max: number): number {
  if (!Number.isInteger(min) || !Number.isInteger(max) || max < min) {
    throw new Error('Interval inválido para a rolagem.');
  }
  const range = max - min + 1;
  const uint32Range = 0x1_0000_0000;
  if (range > uint32Range) throw new Error('Intervalo de rolagem grande demais.');
  const unbiasedLimit = Math.floor(uint32Range / range) * range;
  const array = new Uint32Array(1);
  let value: number;
  do {
    window.crypto.getRandomValues(array);
    value = array[0];
  } while (value >= unbiasedLimit);
  return min + (value % range);
}

// Parse custom formula or handle standard rols
export function rollStandardFromObservation(
  results: number[],
  sides: number,
  modifier: number,
  physics?: RollResult['physics'],
): RollResult {
  if (!Number.isInteger(sides) || sides < 2) throw new Error('Número de lados inválido.');
  if (!Array.isArray(results) || results.length < 1) throw new Error('A observação física não contém dados.');
  for (const value of results) {
    if (!Number.isInteger(value) || value < 1 || value > sides) throw new Error(`Face inválida para d${sides}: ${value}`);
  }
  const total = results.reduce((sum, value) => sum + value, 0) + modifier;
  const notation = `${results.length}d${sides}${modifier >= 0 ? '+' : ''}${modifier !== 0 ? modifier : ''}`;
  return {
    id: 'roll_' + Date.now() + '_' + secureRoll(100, 999),
    timestamp: Date.now(), notation, total, modifier, results: [...results], type: 'standard', physics,
  };
}

export function rollStandard(diceCount: number, sides: number, modifier: number): RollResult {
  const results: number[] = [];
  for (let i = 0; i < diceCount; i++) results.push(secureRoll(1, sides));
  return rollStandardFromObservation(results, sides, modifier, { engine: 'webcrypto', mode: 'fallback' });
}

export interface OneRingInterpretation {
  featResult: number;
  featResults: number[];
  successResults: number[];
  total: number;
  success: boolean;
  successLevel: NonNullable<RollResult['successLevel']>;
  tengwarCount: number;
}

/** Pure rules interpreter. It never rolls. Physical 3D, browser fallback and tests all feed this same function. */
export function interpretOneRingObservation(
  featResults: number[],
  successResults: number[],
  targetNumber = 14,
  rollMode: 'normal' | 'favoured' | 'ill-favoured' = 'normal',
  weary = false,
  modifier = 0,
  miserable = false,
): OneRingInterpretation {
  const expectedFeat = rollMode === 'normal' ? 1 : 2;
  if (!Array.isArray(featResults) || featResults.length !== expectedFeat) {
    throw new Error(`O teste ${rollMode} exige ${expectedFeat} Dado(s) de Proeza; recebido(s): ${featResults?.length ?? 0}.`);
  }
  if (featResults.some(value => !Number.isInteger(value) || value < 1 || value > 12)) throw new Error('Face inválida no Dado de Proeza.');
  if (!Array.isArray(successResults) || successResults.some(value => !Number.isInteger(value) || value < 1 || value > 6)) throw new Error('Face inválida em Dado de Sucesso.');
  if (!Number.isFinite(targetNumber)) throw new Error('Número-alvo inválido.');

  const featRank = (value: number) => value === 12 ? 12 : value === 11 ? 0 : value;
  const featResult = rollMode === 'favoured'
    ? featResults.reduce((best, value) => featRank(value) > featRank(best) ? value : best)
    : rollMode === 'ill-favoured'
      ? featResults.reduce((worst, value) => featRank(value) < featRank(worst) ? value : worst)
      : featResults[0];

  const successSum = successResults.reduce((sum, value) => sum + (weary && value <= 3 ? 0 : value), 0);
  const featValue = featResult === 11 ? 0 : featResult === 12 ? 10 : featResult;
  const total = featValue + successSum + modifier;
  const tengwarCount = successResults.filter(value => value === 6).length;
  const success = miserable && featResult === 11 ? false : (featResult === 12 || total >= targetNumber);
  const successLevel: OneRingInterpretation['successLevel'] = !success
    ? 'failure'
    : tengwarCount >= 2 ? 'extraordinary-success' : tengwarCount === 1 ? 'great-success' : 'success';
  return { featResult, featResults: [...featResults], successResults: [...successResults], total, success, successLevel, tengwarCount };
}

export function rollOneRingFromObservation(
  featResults: number[],
  successResults: number[],
  modifier: number,
  targetNumber = 14,
  skillName = '',
  rollMode: 'normal' | 'favoured' | 'ill-favoured' = 'normal',
  weary = false,
  physics?: RollResult['physics'],
  miserable = false,
): RollResult {
  const observed = interpretOneRingObservation(featResults, successResults, targetNumber, rollMode, weary, modifier, miserable);
  const notation = `${rollMode === 'normal' ? '1' : '2'}d12(${rollMode}) + ${successResults.length}d6${modifier >= 0 ? '+' : ''}${modifier !== 0 ? modifier : ''}`;
  return {
    id: 'roll_' + Date.now() + '_' + secureRoll(100, 999), timestamp: Date.now(), notation,
    total: observed.total, modifier, results: [observed.featResult, ...successResults], type: 'one-ring',
    featResult: observed.featResult, featResults: observed.featResults, rollMode, successResults: observed.successResults,
    targetNumber, skillName: skillName.trim() || undefined, success: observed.success, successLevel: observed.successLevel,
    tengwarCount: observed.tengwarCount, weary, miserable, physics,
  };
}

export function rollOneRing(
  successDiceCount: number,
  modifier: number,
  targetNumber = 14,
  skillName = '',
  rollMode: 'normal' | 'favoured' | 'ill-favoured' = 'normal',
  weary = false,
  miserable = false,
): RollResult {
  const featResults = Array.from({ length: rollMode === 'normal' ? 1 : 2 }, () => secureRoll(1, 12));
  const successResults = Array.from({ length: successDiceCount }, () => secureRoll(1, 6));
  return rollOneRingFromObservation(featResults, successResults, modifier, targetNumber, skillName, rollMode, weary, { engine: 'webcrypto', mode: 'fallback' }, miserable);
}

// Helper to format roll description
export function formatRollResult(roll: RollResult): string {
  if (roll.type === 'standard') {
    return `Resultado: ${roll.total} (Dados: [${roll.results.join(', ')}] Modificador: ${roll.modifier >= 0 ? '+' : ''}${roll.modifier})`;
  } else {
    const featStr = roll.featResult === 11 
      ? 'Olho de Sauron (0)' 
      : roll.featResult === 12 
        ? 'Runa de Gandalf (Sucesso)' 
        : `Feat Die: ${roll.featResult}`;
    
    const successStr = roll.successResults && roll.successResults.length > 0 
      ? `Dados de Sucesso: [${roll.successResults.map(r => r === 6 ? 'Tengwar (6)' : r).join(', ')}]` 
      : 'Sem dados de sucesso';
      
    const wearyText = roll.weary ? ' | Exausto: dados 1–3 valem 0' : '';
    const miserableText = roll.miserable ? ' | Arrasado: Olho de Sauron causa falha automática' : '';
    const outcome = roll.success === undefined ? '' : ` | ${roll.success ? 'SUCESSO' : 'FALHA'}${roll.targetNumber ? ` contra ND ${roll.targetNumber}` : ''}`;
    const mode = roll.rollMode && roll.rollMode !== 'normal'
      ? ` | ${roll.rollMode === 'favoured' ? 'Favorecido' : 'Desfavorecido'}: [${roll.featResults?.join(', ') || roll.featResult}]`
      : '';
    return `O Um Anel: ${roll.total} (${featStr}${mode}${wearyText}${miserableText} | ${successStr} | Modificador: ${roll.modifier >= 0 ? '+' : ''}${roll.modifier}${outcome})`;
  }
}
