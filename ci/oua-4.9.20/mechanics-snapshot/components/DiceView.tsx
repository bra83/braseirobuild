import React, { useEffect, useMemo, useState } from 'react';
import { AlertCircle, Dices, Send, Trash2, UserRound } from 'lucide-react';
import { rollOneRing, rollOneRingFromObservation, rollStandard, rollStandardFromObservation, type RollResult } from '../lib/dice';
import { clearStore, getStoreData, saveStoreData, type Character, type PendingRoll, type PendingTest, type RollItem } from '../lib/db';
import { findSkill, targetNumberForSkill } from '../lib/characterRules';
import { skillAttribute } from '../lib/rules/tests';

interface DiceViewProps {
  onSendRollToMaster: (roll: RollResult) => void;
  pendingRoll: PendingRoll | null;
  onClearPendingRoll: () => void;
  pendingTest?: PendingTest | null;
  characters: Character[];
  activeCharacterId?: string | null;
  onActiveCharacterChange: (characterId: string) => void | Promise<void>;
  onSpendHope: (characterId: string, attribute: 'strength' | 'heart' | 'wits') => Promise<number>;
}

export const DiceView: React.FC<DiceViewProps> = ({
  onSendRollToMaster,
  pendingRoll,
  onClearPendingRoll,
  pendingTest,
  characters,
  activeCharacterId,
  onActiveCharacterChange,
  onSpendHope,
}) => {
  const [selectedCharacterId, setSelectedCharacterId] = useState(activeCharacterId || characters[0]?.id || '');
  const selectedCharacter = useMemo(
    () => characters.find(character => character.id === selectedCharacterId) || characters.find(character => character.id === activeCharacterId) || characters[0] || null,
    [characters, selectedCharacterId, activeCharacterId],
  );
  const [standardCount, setStandardCount] = useState(1);
  const [standardSides, setStandardSides] = useState(20);
  const [modifier, setModifier] = useState(0);
  const [oneRingSuccessCount, setOneRingSuccessCount] = useState(0);
  const [oneRingModifier, setOneRingModifier] = useState(0);
  const [oneRingTargetNumber, setOneRingTargetNumber] = useState(14);
  const [oneRingSkillName, setOneRingSkillName] = useState('Viagem');
  const [oneRingRollMode, setOneRingRollMode] = useState<'normal' | 'favoured' | 'ill-favoured'>('normal');
  const [history, setHistory] = useState<RollResult[]>([]);
  const [hopeSpentForRoll, setHopeSpentForRoll] = useState(false);
  const [hopeBonus, setHopeBonus] = useState(0);
  const [diceError, setDiceError] = useState<string | null>(null);
  const [rollingPhysical, setRollingPhysical] = useState(false);

  const loadHistory = async () => {
    try {
      const rolls = await getStoreData<RollItem>('rolls');
      setHistory((rolls as unknown as RollResult[]).sort((a, b) => b.timestamp - a.timestamp));
    } catch (error) { console.error('[BRASEIRO_DICE] histórico:', error); }
  };
  useEffect(() => { void loadHistory(); }, []);
  useEffect(() => {
    const requestedCharacterId = pendingTest?.characterId;
    if (requestedCharacterId && characters.some(character => character.id === requestedCharacterId)) {
      if (requestedCharacterId !== selectedCharacterId) {
        setSelectedCharacterId(requestedCharacterId);
        void onActiveCharacterChange(requestedCharacterId);
      }
      return;
    }
    if (activeCharacterId && activeCharacterId !== selectedCharacterId) setSelectedCharacterId(activeCharacterId);
  }, [activeCharacterId, pendingTest?.characterId, characters.map(character => character.id).join('|')]);

  const applySkill = (skillName: string, character = selectedCharacter) => {
    setOneRingSkillName(skillName);
    setHopeSpentForRoll(false);
    setHopeBonus(0);
    setOneRingModifier(0);
    if (!character) return;
    const skill = findSkill(character.sheet, skillName);
    setOneRingSuccessCount(Math.max(0, skill?.rank || 0));
    setOneRingTargetNumber(targetNumberForSkill(character.sheet, skillName));
    setOneRingRollMode(skill?.favorite ? 'favoured' : 'normal');
  };

  useEffect(() => {
    if (!selectedCharacter) return;
    const requested = pendingTest?.skill || oneRingSkillName || selectedCharacter.sheet.skills[0]?.name || 'Perícia apropriada';
    applySkill(requested, selectedCharacter);
    if (pendingTest?.targetNumber) setOneRingTargetNumber(pendingTest.targetNumber);
    else if (pendingTest?.rollMode) setOneRingRollMode(pendingTest.rollMode);
  }, [selectedCharacter?.id, pendingTest?.id]);

  const changeCharacter = async (id: string) => {
    setSelectedCharacterId(id);
    await onActiveCharacterChange(id);
    const character = characters.find(item => item.id === id);
    if (character) applySkill(pendingTest?.skill || oneRingSkillName, character);
  };

  const handleSpendHope = async () => {
    if (!selectedCharacter || hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0) return;
    const attribute = skillAttribute(selectedCharacter.sheet, oneRingSkillName);
    const bonus = await onSpendHope(selectedCharacter.id, attribute);
    if (bonus > 0) {
      setOneRingModifier(current => current + bonus);
      setHopeBonus(bonus);
      setHopeSpentForRoll(true);
    }
  };

  const handleRollOneRing = async () => {
    const rollMode = oneRingRollMode;
    const weary = Boolean(selectedCharacter?.sheet.attributes.weary);
    setDiceError(null);
    setRollingPhysical(true);
    try {
      let result: RollResult;
      const native = window.BraseiroAndroid?.rollOneRingPhysical;
      if (native) {
        const observed = await native(oneRingSuccessCount, rollMode, selectedCharacter?.id || '', selectedCharacter?.sheet.identity.name || selectedCharacter?.name || '');
        result = rollOneRingFromObservation(
          observed.feat_results,
          observed.success_results,
          oneRingModifier,
          oneRingTargetNumber,
          oneRingSkillName,
          rollMode,
          weary,
          {
            engine: observed.engine || 'braseiro-physical-dice', mode: 'physical_3d',
            seedFeat: observed.physics_seed_feat, seedSuccess: observed.physics_seed_success,
            stepsFeat: observed.physics_steps_feat, stepsSuccess: observed.physics_steps_success,
            forcedSettle: Boolean(observed.forced_settle_feat || observed.forced_settle_success),
          },
        );
        if (window.BraseiroAndroid?.barbaraInterpretRoll) {
          const validation = await window.BraseiroAndroid.barbaraInterpretRoll(
            result.featResults || [], result.successResults || [], oneRingTargetNumber, rollMode, weary, oneRingModifier, result as unknown as Record<string, unknown>,
          );
          if (validation?.available && validation.valid === false) throw new Error(`Barbara recusou a interpretação da rolagem: ${validation.reason || validation.error || 'divergência mecânica'}`);
        }
      } else {
        result = rollOneRing(oneRingSuccessCount, oneRingModifier, oneRingTargetNumber, oneRingSkillName, rollMode, weary);
      }
      result = { ...result, characterId: selectedCharacter?.id, characterName: selectedCharacter?.sheet.identity.name || selectedCharacter?.name };
      await saveStoreData('rolls', result);
      await loadHistory();
      if (pendingTest) await onSendRollToMaster(result);
      if (hopeBonus) setOneRingModifier(current => current - hopeBonus);
      setHopeBonus(0);
      setHopeSpentForRoll(false);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (message !== 'dice_cancelled') setDiceError(message);
    } finally {
      setRollingPhysical(false);
    }
  };

  const handleRollStandard = async () => {
    setDiceError(null);
    setRollingPhysical(true);
    try {
      let result: RollResult;
      const native = window.BraseiroAndroid?.rollPhysicalDice;
      const physicallySupported = [4, 6, 8, 10, 12, 20, 100].includes(standardSides) && standardCount <= 20;
      if (native && physicallySupported) {
        const suffix = modifier === 0 ? '' : modifier > 0 ? `+${modifier}` : String(modifier);
        const observed = await native(`${standardCount}d${standardSides}${suffix}`, selectedCharacter?.id || '', selectedCharacter?.sheet.identity.name || selectedCharacter?.name || '');
        result = rollStandardFromObservation(observed.dice, standardSides, modifier, {
          engine: observed.engine || 'braseiro-physical-dice', mode: 'physical_3d', seed: observed.physics_seed,
          steps: observed.physics_steps, forcedSettle: Boolean(observed.forced_settle),
        });
      } else {
        result = rollStandard(standardCount, standardSides, modifier);
      }
      result = { ...result, characterId: selectedCharacter?.id, characterName: selectedCharacter?.sheet.identity.name || selectedCharacter?.name };
      await saveStoreData('rolls', result);
      await loadHistory();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (message !== 'dice_cancelled') setDiceError(message);
    } finally {
      setRollingPhysical(false);
    }
  };

  const clearHistory = async () => {
    if (!confirm('Apagar o histórico de rolagens?')) return;
    await clearStore('rolls');
    await loadHistory();
  };

  if (!selectedCharacter) {
    return <div className="medieval-panel mx-auto max-w-2xl p-8 text-center text-slate-300"><UserRound className="mx-auto mb-3 h-9 w-9 text-blue-500" />Crie um personagem antes de rolar os dados.</div>;
  }

  const skills = selectedCharacter.sheet.skills;
  const currentSkill = findSkill(selectedCharacter.sheet, oneRingSkillName);

  return (
    <div className="mx-auto grid w-full max-w-5xl grid-cols-1 gap-6 pb-16 font-serif text-blue-100 lg:grid-cols-[1.25fr_.75fr]">
      <div className="space-y-6">
        <section className="medieval-panel p-4 sm:p-5">
          <div className="mb-4 flex items-center gap-2 border-b border-blue-950 pb-3">
            <Dices className="h-5 w-5 text-cyan-400" /><h2 className="medieval-title text-lg">Rolagem de O Um Anel</h2>
          </div>

          <label className="mb-1 block text-[11px] font-bold uppercase tracking-wider text-blue-400">Quem vai rolar?</label>
          <select value={selectedCharacter.id} onChange={event => void changeCharacter(event.target.value)} className="mb-4 w-full rounded border border-blue-800 bg-[#050b17] px-3 py-3 text-blue-100">
            {characters.map(character => <option key={character.id} value={character.id}>{character.sheet.identity.name || character.name} — {character.sheet.identity.culture || character.culture}</option>)}
          </select>

          {pendingTest && (
            <div className="mb-4 rounded border border-cyan-500/60 bg-cyan-950/25 p-3 text-sm text-cyan-100">
              <strong className="block uppercase tracking-wide">Teste obrigatório: {pendingTest.skill}</strong>
              <span className="mt-1 block text-cyan-200/80">{pendingTest.reason}</span>
              {pendingTest.consequences && <span className="mt-1 block text-cyan-300/70">Em risco: {pendingTest.consequences}</span>}
            </div>
          )}

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label className="text-[10px] font-bold uppercase text-blue-500">Perícia</label>
              <select value={oneRingSkillName} onChange={event => applySkill(event.target.value)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-3 py-2">
                {skills.map(skill => <option key={skill.name} value={skill.name}>{skill.name} — {skill.rank}d6{skill.favorite ? ' · favorecida' : ''}</option>)}
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase text-blue-500">Número-alvo</label>
              <input type="number" min={1} max={30} value={oneRingTargetNumber} onChange={event => setOneRingTargetNumber(Math.max(1, Number(event.target.value) || 14))} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-3 py-2" />
              <p className="mt-1 text-[10px] text-slate-500">Calculado pela ficha: 20 − atributo relacionado.</p>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase text-blue-500">Dados de sucesso (d6)</label>
              <input type="number" min={0} max={12} value={oneRingSuccessCount} onChange={event => setOneRingSuccessCount(Math.max(0, Number(event.target.value) || 0))} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-3 py-2" />
              <p className="mt-1 text-[10px] text-cyan-500">Puxado de {currentSkill?.name || oneRingSkillName}: {currentSkill?.rank || 0} dados.</p>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase text-blue-500">Condição</label>
              <select value={oneRingRollMode} onChange={event => setOneRingRollMode(event.target.value as typeof oneRingRollMode)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-3 py-2">
                <option value="normal">Normal — 1 dado de Proeza</option>
                <option value="favoured">Favorecido — 2, fica o melhor</option>
                <option value="ill-favoured">Desfavorecido — 2, fica o pior</option>
              </select>
            </div>
            <div>
              <label className="text-[10px] font-bold uppercase text-blue-500">Modificador</label>
              <input type="number" value={oneRingModifier} onChange={event => setOneRingModifier(Number(event.target.value) || 0)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-3 py-2" />
            </div>
          </div>
          {(selectedCharacter.sheet.attributes.weary || selectedCharacter.sheet.attributes.miserable) && (
            <div className="mt-4 rounded border border-amber-700/60 bg-amber-950/20 p-3 text-xs text-amber-100">
              {selectedCharacter.sheet.attributes.weary && <p><strong>Exausto:</strong> resultados 1–3 nos Dados de Sucesso valem 0.</p>}
              {selectedCharacter.sheet.attributes.miserable && <p><strong>Arrasado:</strong> um Olho de Sauron faz a ação falhar automaticamente, independentemente do total.</p>}
            </div>
          )}
          {diceError && <div className="mt-4 rounded border border-red-700/60 bg-red-950/25 p-3 text-xs text-red-200"><AlertCircle className="mr-1 inline h-4 w-4" />{diceError}</div>}
          <div className="mt-5 grid grid-cols-1 gap-2 sm:grid-cols-[.8fr_1.2fr]">
            <button type="button" onClick={() => void handleSpendHope()} disabled={hopeSpentForRoll || selectedCharacter.sheet.attributes.hopeCurrent <= 0} className="rounded border border-amber-700 bg-amber-950/25 px-3 py-3 text-sm font-bold text-amber-200 disabled:opacity-40">
              {hopeSpentForRoll ? 'Esperança aplicada' : `Gastar 1 Esperança (+${selectedCharacter.sheet.attributes[skillAttribute(selectedCharacter.sheet, oneRingSkillName)]})`}
            </button>
            <button disabled={rollingPhysical} onClick={() => void handleRollOneRing()} className="flex items-center justify-center gap-2 rounded border border-cyan-600 bg-cyan-950/35 px-4 py-3 font-bold text-cyan-100 hover:bg-cyan-900/35"><Dices className="h-5 w-5" /> Rolar por {selectedCharacter.sheet.identity.name}</button>
          </div>
        </section>

        <section className="medieval-panel p-4 sm:p-5">
          <h3 className="mb-3 text-sm font-bold uppercase tracking-wider text-blue-400">Rolagem livre</h3>
          <div className="grid grid-cols-3 gap-3">
            <label className="text-xs text-slate-400">Quantidade<input type="number" min={1} max={30} value={standardCount} onChange={event => setStandardCount(Math.max(1, Number(event.target.value) || 1))} className="mt-1 w-full rounded border border-blue-950 bg-zinc-950 px-2 py-2" /></label>
            <label className="text-xs text-slate-400">Lados<input type="number" min={2} max={100} value={standardSides} onChange={event => setStandardSides(Math.max(2, Number(event.target.value) || 20))} className="mt-1 w-full rounded border border-blue-950 bg-zinc-950 px-2 py-2" /></label>
            <label className="text-xs text-slate-400">Modificador<input type="number" value={modifier} onChange={event => setModifier(Number(event.target.value) || 0)} className="mt-1 w-full rounded border border-blue-950 bg-zinc-950 px-2 py-2" /></label>
          </div>
          <button disabled={rollingPhysical} onClick={() => void handleRollStandard()} className="mt-4 w-full rounded border border-blue-700 px-4 py-2 text-blue-100">Rolar dados livres</button>
        </section>
      </div>

      <aside className="medieval-panel p-4 sm:p-5">
        <div className="mb-4 flex items-center justify-between border-b border-blue-950 pb-3"><h3 className="font-bold text-blue-300">Histórico</h3><button onClick={() => void clearHistory()} className="text-red-500" title="Apagar histórico"><Trash2 className="h-4 w-4" /></button></div>
        {pendingRoll && <div className="mb-4 rounded border border-amber-600/50 bg-amber-950/20 p-3 text-xs text-amber-100"><AlertCircle className="mb-1 inline h-4 w-4" /> Resultado aguardando o Mestre: {pendingRoll.total}<button onClick={onClearPendingRoll} className="mt-2 block text-amber-300 underline">Descartar</button></div>}
        <div className="max-h-[700px] space-y-3 overflow-auto pr-1">
          {history.map(roll => <div key={roll.id} className="rounded border border-blue-950 bg-black/30 p-3 text-xs"><div className="flex justify-between gap-2"><strong className="text-blue-200">{roll.characterName || 'Rolagem livre'}</strong><span className="text-slate-500">{new Date(roll.timestamp).toLocaleString('pt-BR')}</span></div><p className="mt-1 text-slate-400">{roll.skillName || roll.notation}</p><div className="mt-2 text-lg font-bold text-cyan-300">{roll.total}</div>{roll.success !== undefined && <p className={roll.success ? 'text-emerald-400' : 'text-red-400'}>{roll.success ? 'Sucesso' : 'Falha'}{roll.tengwarCount ? ` · ${roll.tengwarCount} Tengwar` : ''}</p>}{pendingTest && <button onClick={() => void onSendRollToMaster(roll)} className="mt-2 flex items-center gap-1 text-cyan-400"><Send className="h-3 w-3" /> Enviar ao Mestre</button>}</div>)}
          {!history.length && <p className="py-8 text-center text-sm text-slate-500">Nenhuma rolagem ainda.</p>}
        </div>
      </aside>
    </div>
  );
};
