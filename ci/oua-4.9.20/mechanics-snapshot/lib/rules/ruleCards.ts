export interface RuleCard {
  id: string;
  title: string;
  domains: string[];
  keywords: string[];
  summary: string;
  source: string;
  page: number;
}

export const CORE_RULE_CARDS: RuleCard[] = [
  { id: 'when-to-roll', title: 'Quando pedir uma jogada', domains: ['teste', 'cena'], keywords: ['teste', 'rolar', 'perigo', 'conhecimento', 'manipulação'], summary: 'Peça uma jogada apenas quando houver risco relevante: perigo, informação oculta ou tentativa de influenciar alguém não cooperativo. Caso contrário, diga sim e prossiga.', source: 'O Um Anel 2e — Livro Básico', page: 16 },
  { id: 'single-hero-test', title: 'Quem faz o teste', domains: ['teste', 'companhia'], keywords: ['quem', 'herói', 'grupo', 'companhia', 'ajudar'], summary: 'Em geral, um único Herói-jogador, escolhido como o mais adequado, faz a jogada. Testes de todos só ocorrem quando a situação realmente envolve toda a Companhia.', source: 'O Um Anel 2e — resumo de regras', page: 13 },
  { id: 'target-number', title: 'Número-Alvo', domains: ['teste'], keywords: ['número alvo', 'na', 'atributo'], summary: 'O Número-Alvo de um atributo é 20 menos o valor do atributo relacionado. A ficha e o motor local são a fonte de verdade do cálculo.', source: 'O Um Anel 2e — Livro Básico', page: 18 },
  { id: 'weary', title: 'Exausto', domains: ['teste', 'condição'], keywords: ['exausto', 'fadiga', 'carga', 'resistência'], summary: 'Quando a Resistência atual é igual ou menor que a Carga total, o herói fica Exausto. Em suas jogadas, resultados 1, 2 e 3 nos Dados de Sucesso contam como zero.', source: 'O Um Anel 2e — Livro Básico', page: 22 },
  { id: 'miserable', title: 'Arrasado e Sombra', domains: ['sombra', 'condição', 'teste'], keywords: ['arrasado', 'sombra', 'esperança', 'olho de sauron'], summary: 'Quando a Sombra iguala ou supera a Esperança atual, o herói fica Arrasado. Se o Dado de Façanha mostrar o Olho de Sauron, a ação falha independentemente do total; Arrasado não torna a jogada Desfavorecida.', source: 'O Um Anel 2e — Livro Básico', page: 22 },
  { id: 'fellowship', title: 'Sociedade da Companhia', domains: ['companhia', 'sociedade'], keywords: ['sociedade', 'companhia', 'esperança', 'patrono'], summary: 'Sociedade é um recurso compartilhado entre todos os Heróis-jogadores, usado principalmente para recuperar Esperança ao descansar e para efeitos do Patrono.', source: 'O Um Anel 2e — Livro Básico', page: 54 },
  { id: 'journey-roles', title: 'Papéis e eventos de Jornada', domains: ['jornada'], keywords: ['jornada', 'guia', 'batedor', 'vigia', 'caçador', 'fadiga'], summary: 'A Companhia distribui papéis de Jornada. Eventos podem desafiar Batedores com Exploração, Vigias com Vigilância e Caçadores com Caçada, aplicando consequências e Fadiga ao alvo ou ao grupo conforme o evento.', source: 'O Um Anel 2e — Livro Básico', page: 112 },
  { id: 'fellowship-phase', title: 'Fase de Sociedade', domains: ['sociedade', 'progressão'], keywords: ['fase de sociedade', 'empreendimento', 'recuperação', 'progressão'], summary: 'Ao encerrar um arco da Fase de Aventura, a Companhia pode entrar na Fase de Sociedade para recuperação, empreendimentos e evolução entre aventuras.', source: 'O Um Anel 2e — Livro Básico', page: 117 },
  { id: 'eye-awareness', title: 'Vigilância do Olho', domains: ['companhia', 'sombra'], keywords: ['olho', 'vigilância', 'mordor', 'companhia'], summary: 'A Vigilância do Olho é da Companhia e sua pontuação inicial depende da composição do grupo; grupos maiores ou formados por povos mais vigiados atraem mais atenção.', source: 'O Um Anel 2e — Livro Básico', page: 169 },
];

export function findRuleCards(text: string, domains: string[] = []): RuleCard[] {
  const normalized = String(text ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  return CORE_RULE_CARDS
    .map(card => ({ card, score: card.keywords.filter(word => normalized.includes(word.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase())).length * 3 + card.domains.filter(domain => domains.includes(domain)).length * 2 }))
    .filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, 5)
    .map(item => item.card);
}
