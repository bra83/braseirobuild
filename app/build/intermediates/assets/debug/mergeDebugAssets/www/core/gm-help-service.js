import { canonicalJson } from './canonical-json.js';

export function gmHelpReadOnly({ state, question = '' }) {
  const fingerprintBefore = canonicalJson(state);
  const screenId = state?.encounterState?.screenId ?? 'sem encontro ativo';
  const answer = question.trim()
    ? `Ajuda de Mestre (somente leitura) para ${screenId}: a pergunta foi recebida sem avançar a ficção nem alterar estado.`
    : `Ajuda de Mestre (somente leitura) para ${screenId}: nenhuma mutação foi solicitada.`;
  const fingerprintAfter = canonicalJson(state);
  if (fingerprintAfter !== fingerprintBefore) throw new Error('GM_HELP mutated state');
  return Object.freeze({ kind: 'GM_HELP', mutatesState: false, advancesTime: false, answer });
}
