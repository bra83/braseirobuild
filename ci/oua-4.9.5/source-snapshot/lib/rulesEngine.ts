import { StoryChoice, TestRequirement } from '../types';
import { CANONICAL_SKILLS, canonicalSkillName, skillAliasesFor } from './skillRegistry';

const DOMAIN_TERMS: Record<string, string[]> = {
  testes: ['teste', 'rolagem', 'dado de proeza', 'dados de sucesso', 'numero alvo', 'número alvo', 'favorecido', 'desfavorecido', 'tengwar', 'runa de gandalf', 'olho de sauron'],
  jornada: ['jornada', 'viagem', 'viajar', 'marcha', 'caminho', 'estrada', 'papel de viagem', 'guia', 'caçador', 'vigia', 'batedor', 'fadiga de viagem', 'evento de jornada'],
  movimento: ['movimento', 'mover', 'deslocar', 'distancia', 'distância', 'hex', 'mapa', 'terreno', 'travessia', 'rio', 'montanha', 'floresta', 'pântano', 'pantano'],
  combate: ['combate', 'batalha', 'ataque', 'defesa', 'bloqueio', 'parada', 'postura', 'ferimento', 'dano', 'proteção', 'protecao', 'armadura', 'iniciativa', 'adversário', 'adversario'],
  conselho: ['conselho', 'audiência', 'audiencia', 'tolerância', 'tolerancia', 'apresentação', 'apresentacao', 'interação', 'interacao', 'persuasão', 'persuasao', 'cortesias', 'enigma'],
  descanso: ['descanso', 'repouso', 'recuperação', 'recuperacao', 'curar', 'sono', 'acampamento', 'fase de sociedade', 'pontos de esperança'],
  sombra: ['sombra', 'desespero', 'arrasado', 'corrupção', 'corrupcao', 'crise de loucura', 'esperança', 'esperanca'],
  inventario: ['inventário', 'inventario', 'carga', 'equipamento', 'provisões', 'provisoes', 'arma', 'armadura', 'escudo', 'tesouro'],
};

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

export function detectRuleDomains(...texts: Array<string | undefined>): string[] {
  const haystack = normalize(texts.filter(Boolean).join(' '));
  const domains: string[] = [];
  for (const [domain, terms] of Object.entries(DOMAIN_TERMS)) {
    if (terms.some(term => haystack.includes(normalize(term)))) domains.push(domain);
  }
  return domains.length ? domains : ['testes'];
}

export function buildRuleSearchTerms(action: string, location: string, recentNarration = ''): string[] {
  const domains = detectRuleDomains(action, location, recentNarration);
  const terms = new Set<string>([location, action]);
  for (const domain of domains) {
    terms.add(domain);
    for (const term of DOMAIN_TERMS[domain] || []) terms.add(term);
  }
  return Array.from(terms);
}

const SKILL_SEARCH_TERMS = CANONICAL_SKILLS.map(skill => ({ skill, terms: Array.from(new Set([normalize(skill), ...skillAliasesFor(skill)])) }));


export function inferTestFromText(text: string): TestRequirement | null {
  const normalized = normalize(text);
  const bracket = text.match(/\[(?:teste|rolagem)\s+de\s+([^\]]+)\]/i);
  const named = bracket?.[1]?.trim();
  let canonical = named ? canonicalSkillName(named) : null;
  if (!canonical) {
    for (const entry of SKILL_SEARCH_TERMS) {
      if (entry.terms.some(term => term.length > 2 && normalized.includes(term))) { canonical = entry.skill; break; }
    }
  }
  if (!canonical && !/(teste|rolagem|exige.*dado|deve rolar)/i.test(text)) return null;
  const skill = canonical || 'Perícia apropriada';
  const tnMatch = text.match(/(?:ND|NA|número[- ]alvo|dificuldade)\s*[:=]?\s*(\d{1,2})/i);
  return {
    id: `test_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
    skill,
    targetNumber: tnMatch ? Number(tnMatch[1]) : undefined,
    rollMode: /desfavorecid/i.test(text) ? 'ill-favoured' : /favorecid/i.test(text) ? 'favoured' : 'normal',
    reason: text.replace(/\[(?:teste|rolagem)[^\]]+\]/ig, '').trim() || `Teste de ${skill}`,
  };
}

export function normalizeStoryChoices(choices: StoryChoice[] | undefined): StoryChoice[] {
  // Sugestões são apenas exemplos de ações possíveis. O teste só é decidido
  // depois que o jogador realmente escolhe ou escreve uma ação e o Diretor a
  // avalia no contexto completo. Isso evita transformar a cena em livro-jogo.
  return (choices || []).slice(0, 4).map((choice, index) => ({
    id: choice.id || `choice_${index + 1}`,
    text: String(choice.text || '').replace(/\[(?:teste|rolagem)[^\]]+\]/ig, '').trim(),
    requiresTest: false,
    test: undefined,
  })).filter(choice => choice.text.length > 0);
}
