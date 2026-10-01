import type { CampaignState, Character, Npc } from './db';
import type { AdventureProfileId } from './adventureProfile';

export interface OpeningArchitecture {
  id: string;
  focus: string;
  entry: string;
  turn: string;
  restriction: string;
}

const ARCHITECTURES: OpeningArchitecture[] = [
  { id: 'hospitalidade_tensa', focus: 'hospitalidade e deveres entre anfitrião e viajantes', entry: 'a Companhia já foi recebida e percebe uma regra social importante', turn: 'um gesto de cortesia revela uma obrigação incompatível com os planos de alguém', restriction: 'não iniciar em taverna genérica nem com contratante oferecendo missão' },
  { id: 'canção_incompleta', focus: 'memória, tradição e perda', entry: 'uma canção, relato ou genealogia é interrompida por uma contradição concreta', turn: 'um presente reconhece um verso, nome ou lugar que deveria estar esquecido', restriction: 'não usar profecia vaga nem escolhido do destino' },
  { id: 'fronteira_vigiada', focus: 'passagem, autoridade e rotas', entry: 'a Companhia chega a ponte, vau, portão, estrada elevada ou trilha controlada', turn: 'a condição para atravessar muda por causa de notícia, pessoa ou sinal verificável', restriction: 'não começar com patrulha atacando sem conversa ou alternativa' },
  { id: 'rotina_do_caminho', focus: 'competência cotidiana dos heróis', entry: 'a cena começa durante acampamento, reparo, caça, vigília ou preparação de provisões', turn: 'um detalhe prático cria duas prioridades legítimas', restriction: 'não usar monstro saltando da escuridão como atalho' },
  { id: 'vestigio_recente', focus: 'investigação e consequências', entry: 'há rastros claros de algo ocorrido há pouco, sem explicação confirmada', turn: 'duas leituras plausíveis apontam para riscos diferentes', restriction: 'não colocar cadáver anônimo apenas para gerar urgência' },
  { id: 'abrigo_compartilhado', focus: 'clima, convivência e confiança', entry: 'viajantes incompatíveis dividem abrigo contra frio, chuva, neblina ou noite', turn: 'um recurso escasso ou notícia incompleta altera as alianças', restriction: 'não iniciar combate enquanto existirem saídas sociais e práticas' },
  { id: 'retorno_transformado', focus: 'memória persistente do mundo', entry: 'a Companhia reencontra local, pessoa ou costume afetado por decisões anteriores', turn: 'algo melhorou enquanto outra consequência inesperada amadureceu', restriction: 'não apagar nem contradizer fatos canônicos da campanha' },
  { id: 'objeto_fora_do_lugar', focus: 'curiosidade e interesse concorrente', entry: 'um objeto, marca, animal, mensagem ou ferramenta aparece onde não deveria', turn: 'um terceiro revela que o achado tem valor diferente do aparente', restriction: 'não declarar magia ou origem sem evidência observável' },
  { id: 'conselho_em_andamento', focus: 'interesses públicos e reputação', entry: 'assembleia, julgamento, divisão de recursos ou negociação já está em curso', turn: 'a chegada da Companhia muda o equilíbrio sem torná-la automaticamente autoridade', restriction: 'não reduzir lados a bons e maus' },
  { id: 'silencio_anormal', focus: 'ausência, rotina quebrada e exploração', entry: 'um lugar normalmente ativo está quieto ou funciona pela metade', turn: 'um sinal pequeno prova que alguém ainda observa ou trabalha ali', restriction: 'não usar ruído genérico atrás de porta como único mistério' },
  { id: 'mensagem_atrasada', focus: 'tempo, promessa e informação', entry: 'uma mensagem chega tarde, incompleta ou ao destinatário errado', turn: 'agir depressa ajuda alguém, mas expõe uma rota ou relação', restriction: 'não usar mensageiro moribundo sem identidade' },
  { id: 'divida_de_reputacao', focus: 'renome, patrono, foco da Sociedade e relações', entry: 'alguém reconhece um herói por ação, povo, patrono ou promessa verdadeira', turn: 'a lembrança pode abrir uma porta e cobrar um preço social', restriction: 'não inventar passado incompatível com a ficha' },
  { id: 'oportunidade_competida', focus: 'recurso, informação ou passagem desejada por mais de um grupo', entry: 'a Companhia descobre a oportunidade ao mesmo tempo que rivais ou moradores', turn: 'o primeiro gesto define posição, não o resultado final', restriction: 'não transformar competição automaticamente em combate' },
  { id: 'perigo_distante', focus: 'preparação diante de ameaça percebida', entry: 'sinais de perigo são visíveis, mas ainda há tempo para escolher posição e objetivo', turn: 'um inocente, rota segura ou recurso importante impede solução óbvia', restriction: 'não forçar rolagem na abertura' },
  { id: 'festa_com_ausencia', focus: 'comunidade, costumes e pessoa ausente', entry: 'uma celebração, feira, funeral ou encontro sazonal ocorre normalmente', turn: 'uma ausência notada por poucos altera o sentido de um costume', restriction: 'não começar com caos ou ataque' },
  { id: 'promessa_cruzada', focus: 'juramentos e deveres incompatíveis', entry: 'duas promessas legítimas puxam pessoas diferentes na mesma cena', turn: 'a Companhia descobre que ajudar um lado afeta alguém respeitável do outro', restriction: 'não oferecer solução moral única' },
  { id: 'guia_indesejado', focus: 'jornada e conhecimento local', entry: 'uma pessoa se oferece ou é imposta como guia por motivo verificável', turn: 'ela conhece a rota, mas esconde uma limitação ou vínculo', restriction: 'não fazer do guia traidor automático' },
  { id: 'refugio_em_reparo', focus: 'santuário, trabalho e proteção', entry: 'um refúgio, salão, ponte ou torre precisa de reparo imediato', turn: 'o reparo revela sinais de uso recente ou decisão antiga', restriction: 'não transformar trabalho em montagem sem escolhas' },
  { id: 'troca_de_noticias', focus: 'rumores com fontes e interesses', entry: 'viajantes, mercadores ou guardiões trocam notícias em movimento', turn: 'duas notícias verdadeiras parecem incompatíveis', restriction: 'não apresentar rumor sem origem ou consequência' },
  { id: 'rastro_de_sombra', focus: 'Sombra percebida sem exposição prematura', entry: 'efeitos sutis da Sombra aparecem em comportamento, natureza ou memória', turn: 'há explicação humana plausível e pista inquietante ao mesmo tempo', restriction: 'não nomear o Inimigo nem revelar segredo antes de evidência' },
  { id: 'presa_que_conduz', focus: 'caça e leitura do território', entry: 'a Companhia segue ou encontra rastros de animal, pessoa ou grupo', turn: 'os rastros parecem escolher o caminho e evitam um perigo maior', restriction: 'não fazer a presa falar ou ser sobrenatural sem suporte' },
  { id: 'hospede_inesperado', focus: 'relações entre povos', entry: 'um hóspede improvável já está presente num local seguro', turn: 'sua presença resolve um problema e cria outro de reputação', restriction: 'não usar estereótipos de povo como personalidade completa' },
  { id: 'mapa_contradito', focus: 'geografia, memória e exploração', entry: 'mapa, marco, estrada ou relato local não corresponde ao terreno atual', turn: 'a mudança pode ser natural, deliberada ou efeito de abandono', restriction: 'não mover a Companhia sem escolha confirmada no atlas' },
  { id: 'custo_da_esperanca', focus: 'Esperança, Sociedade e sacrifício', entry: 'há chance concreta de aliviar medo ou sofrimento sem violência', turn: 'o gesto exige tempo, recurso ou exposição da Companhia', restriction: 'não aplicar perda mecânica sem regra e decisão' },
];

function hash(value: string): number {
  let output = 2166136261;
  for (const char of value) output = Math.imul(output ^ char.charCodeAt(0), 16777619) >>> 0;
  return output;
}

export function chooseOpeningArchitecture(campaign: CampaignState, characters: Character[], npcs: Npc[], profile: AdventureProfileId): OpeningArchitecture {
  const recent = new Set((campaign.openingArchitectureHistory || []).slice(-8));
  const candidates = ARCHITECTURES.filter(item => !recent.has(item.id));
  const pool = candidates.length ? candidates : ARCHITECTURES;
  const seed = [campaign.id, campaign.history.length, campaign.currentLocation, profile, characters.map(item => item.id).join('|'), npcs.length, Date.now()].join('|');
  return pool[hash(seed) % pool.length];
}

export function openingInstruction(architecture: OpeningArchitecture): string {
  return `ARQUITETURA DE ABERTURA OBRIGATÓRIA: ${architecture.id}. Foco: ${architecture.focus}. Entrada: ${architecture.entry}. Virada: ${architecture.turn}. Restrição: ${architecture.restriction}. PERFIL DE ABERTURA — NÍVEL 4 CINEMATOGRÁFICO, MAS INTERATIVO: não comece automaticamente por “Você” ou “Vocês”. Abra com uma âncora temporal/cultural OU uma imagem ampla do lugar real, mostre o mundo já em movimento e aproxime a câmera do panorama para o limiar, a Companhia e um detalhe concreto. Use o NOME CANÔNICO DA REGIÃO/LOCAL fornecido pelo host; nunca o substitua por um rótulo genérico como montanha, colina, floresta ou estrada. Integre organicamente luz/visibilidade, ar/temperatura, sons, cheiro/textura e movimento apenas quando pertinentes. Posicione os Heróis fisicamente na cena sem escolher por eles o que pensam ou fazem. Faça a identidade cultural do lugar aparecer por sinais observáveis — arquitetura, trabalho, vestuário, costumes, comida, fala, animais, tráfego ou marcas do território — sem despejo de lore. Introduza um estímulo, presença, ausência, notícia, obrigação, risco ou oportunidade que realmente pertença ao estado validado e pare quando existir a PRIMEIRA DECISÃO REAL. A abertura deve soar como um Mestre humano à mesa: natural, específica, sensorial, reativa e com variação de ritmo; não como resumo, relatório, guia turístico ou romance autônomo. Escreva 9–13 parágrafos substanciais, normalmente 6.500–9.000 caracteres, sem teste obrigatório e sem decidir ações dos Heróis. Gere 2 ou 3 sugestões opcionais realmente diferentes, cada uma começando por verbo específico e citando pessoa, objeto, rota, dever, recurso ou risco presente na cena.`;
}

export function rememberOpening(campaign: CampaignState, architectureId: string): CampaignState {
  const recent = [...(campaign.openingArchitectureHistory || []).filter(id => id !== architectureId), architectureId].slice(-8);
  return { ...campaign, openingArchitectureHistory: recent };
}

export function chooseOpeningClock(campaign: CampaignState, architectureId: string): { hour: number; minute: number } {
  const byArchitecture: Record<string, number[]> = {
    hospitalidade_tensa: [10, 14, 17, 19],
    canção_incompleta: [16, 18, 20, 21],
    fronteira_vigiada: [6, 9, 15, 18],
    rotina_do_caminho: [5, 7, 12, 17],
    vestigio_recente: [6, 11, 16, 20],
    abrigo_compartilhado: [18, 20, 22, 1],
    retorno_transformado: [8, 13, 17, 19],
    objeto_fora_do_lugar: [7, 12, 16, 21],
    conselho_em_andamento: [10, 14, 16, 19],
    silencio_anormal: [5, 6, 18, 22],
    mensagem_atrasada: [7, 13, 18, 21],
    divida_de_reputacao: [9, 15, 18, 20],
    oportunidade_competida: [8, 12, 16, 19],
    perigo_distante: [6, 14, 18, 21],
    festa_com_ausencia: [14, 17, 19, 21],
    promessa_cruzada: [9, 15, 18, 20],
    guia_indesejado: [6, 9, 16, 19],
    refugio_em_reparo: [7, 11, 15, 18],
    troca_de_noticias: [8, 13, 17, 20],
    rastro_de_sombra: [5, 18, 21, 23],
    presa_que_conduz: [5, 7, 16, 19],
    hospede_inesperado: [10, 15, 19, 22],
    mapa_contradito: [7, 12, 16, 18],
    custo_da_esperanca: [9, 14, 18, 20],
  };
  const hours = byArchitecture[architectureId] || [5, 7, 10, 13, 16, 18, 21, 23];
  const seed = `${campaign.id}|${architectureId}|${campaign.currentLocation}|${campaign.history.length}|clock`;
  const hour = hours[hash(seed) % hours.length];
  const minuteOptions = [0, 10, 20, 30, 40, 50];
  const minute = minuteOptions[hash(`${seed}|minute`) % minuteOptions.length];
  return { hour, minute };
}
