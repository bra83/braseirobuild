export type StriderLikelihood = 'certain'|'likely'|'even'|'unlikely'|'unthinkable';
export type StriderRisk = 'standard'|'dangerous'|'reckless';
export type StriderMilestone =
  | 'accept-patron-mission'
  | 'notable-personal-goal'
  | 'complete-patron-mission'
  | 'significant-journey'
  | 'notable-journey-encounter'
  | 'significant-discovery'
  | 'difficult-obstacle'
  | 'council'
  | 'dangerous-combat'
  | 'revelation-episode';

export const STRIDER_SOURCE_ID = 'tor2e-strider';

export function striderTargetNumber(attribute: number): number {
  return Math.max(6, 18-Math.max(0,Math.trunc(Number(attribute)||0)));
}

export function resolveNarrationTable(featResult: number, likelihood: StriderLikelihood='even') {
  const feat=Math.max(1,Math.min(12,Math.trunc(Number(featResult)||1)));
  if(feat===12) return { answer:true, extreme:true, twist:true, label:'Sim — resultado extremo/reviravolta' };
  if(feat===11) return { answer:false, extreme:true, twist:true, label:'Não — resultado extremo/reviravolta' };
  const threshold: Record<StriderLikelihood,number> = { certain:1, likely:4, even:6, unlikely:8, unthinkable:10 };
  const answer=feat>=threshold[likelihood];
  return { answer, extreme:false, twist:false, label:answer?'Sim':'Não' };
}

export const STRIDER_SPECIAL_SUCCESS = [
  { id:'insight', label:'Ter uma visão', effect:'Obtenha informação adicional, não necessariamente ligada diretamente à tarefa.' },
  { id:'quietly', label:'Ir em silêncio', effect:'Atinja o objetivo silenciosamente ou sem atrair atenção.' },
  { id:'hurry', label:'Apressar-se', effect:'Conclua a tarefa em cerca de metade do tempo esperado.' },
  { id:'influence', label:'Ampliar Influência', effect:'Afete um alvo/grupo adicional por ícone de Sucesso gasto.' },
  { id:'advantage', label:'Construir vantagem', effect:'Ganhe +1d no próximo teste de Perícia relacionado.' },
  { id:'cancel-failure', label:'Cancelar uma falha', effect:'Em jogo cooperativo, transforme em sucesso a falha de outro Herói no mesmo teste.' },
] as const;

export const STRIDER_RISK_LEVELS: Record<StriderRisk,{failure:string}> = {
  standard:{failure:'Falha simples OU sucesso com infortúnio.'},
  dangerous:{failure:'Falha com infortúnio.'},
  reckless:{failure:'Desastre.'},
};

export function striderMilestoneAward(milestone: StriderMilestone): { adventurePoints:number; skillPoints:number } {
  const table: Record<StriderMilestone,{adventurePoints:number;skillPoints:number}> = {
    'accept-patron-mission':{adventurePoints:1,skillPoints:0},
    'notable-personal-goal':{adventurePoints:1,skillPoints:1},
    'complete-patron-mission':{adventurePoints:1,skillPoints:1},
    'significant-journey':{adventurePoints:0,skillPoints:2},
    'notable-journey-encounter':{adventurePoints:0,skillPoints:1},
    'significant-discovery':{adventurePoints:1,skillPoints:0},
    'difficult-obstacle':{adventurePoints:0,skillPoints:1},
    'council':{adventurePoints:0,skillPoints:1},
    'dangerous-combat':{adventurePoints:1,skillPoints:0},
    'revelation-episode':{adventurePoints:1,skillPoints:0},
  };
  return table[milestone];
}

const FORTUNE_NUMERIC = [
  'Você pode ignorar uma ameaça sem atrair atenção.',
  'Você ganha a atenção de um aliado em potencial.',
  'Um inimigo inadvertidamente revela sua posição.',
  'Você ganha terreno privilegiado.',
  'Inimigos fogem do perigo.',
  'Você localiza ou fica sabendo de um item útil.',
  'Seu sucesso instila uma nova esperança ou determinação renovada.',
  'Você encontra um momento de conforto ou segurança.',
  'Você aprende ou realiza algo que fornece informações úteis sobre sua missão.',
  'Você encontra uma oportunidade adequada à sua natureza ou habilidades.',
];
const MISFORTUNE_NUMERIC = [
  'Você chama atenção indesejada.',
  'Suas ações são observadas por alguém mal-intencionado.',
  'Inimigos inesperados surgem ou são avistados.',
  'Você é prejudicado por terreno difícil ou ambiente desfavorável.',
  'Você se encontra mal equipado para as circunstâncias.',
  'Uma arma ou item favorito é perdido, quebrado ou sacrificado.',
  'Você é atormentado por visões ou pensamentos perturbadores.',
  'Uma lesão antiga ou estresse ressurge.',
  'Você aprende ou percebe algo que acrescenta nova complicação à missão.',
  'Você enfrenta um teste contrário à sua natureza ou habilidades.',
];

export function striderFortune(featResult: number) {
  const feat=Math.max(1,Math.min(12,Math.trunc(Number(featResult)||1)));
  if(feat===12) return { text:'O Olho do Inimigo foca em outro lugar. Diminua a Vigilância do Olho em 1.', eyeDelta:-1 };
  if(feat===11) return { text:'Um aliado inesperado aparece ou envia ajuda.', eyeDelta:0 };
  return { text:FORTUNE_NUMERIC[feat-1], eyeDelta:0 };
}

export function striderMisfortune(featResult: number) {
  const feat=Math.max(1,Math.min(12,Math.trunc(Number(featResult)||1)));
  if(feat===11) return { text:'Suas ações capturam o Olho do Inimigo. Aumente a Vigilância do Olho em 2.', eyeDelta:2 };
  if(feat===12) return { text:'Um aliado se torna um obstáculo ou responsabilidade.', eyeDelta:0 };
  return { text:MISFORTUNE_NUMERIC[feat-1], eyeDelta:0 };
}

type LoreRow = { action:string; aspect:string; focus:string };
const LORE: Record<number,LoreRow[]> = {
  11:[
    {action:'Abandonar',aspect:'Corrompido',focus:'Maldição'},
    {action:'Atacar',aspect:'Cruel',focus:'Desespero'},
    {action:'Trair',aspect:'Mentiroso',focus:'Inimigo'},
    {action:'Corromper',aspect:'Decaído',focus:'Medo'},
    {action:'Derrotar',aspect:'Arruinado',focus:'Sombra'},
    {action:'Enfraquecer',aspect:'Traiçoeiro',focus:'Guerra'},
  ],
  12:[
    {action:'Acreditar',aspect:'Próspero',focus:'Coragem'},
    {action:'Reforçar',aspect:'Belo',focus:'Dever'},
    {action:'Defender',aspect:'Bom',focus:'Sociedade'},
    {action:'Perdoar',aspect:'Amável',focus:'Esperança'},
    {action:'Resistir',aspect:'Gentil',focus:'Amor'},
    {action:'Fortalecer',aspect:'Maravilhoso',focus:'Paz'},
  ],
  1:[
    {action:'Ajudar',aspect:'Ativo',focus:'Batalha'},
    {action:'Chegar',aspect:'Antigo',focus:'Borda'},
    {action:'Esperar',aspect:'Destemido',focus:'Fardo'},
    {action:'Atravessar',aspect:'Brilhante',focus:'Conselho'},
    {action:'Quebrar',aspect:'Quebrado',focus:'Corte'},
    {action:'Capturar',aspect:'Triste',focus:'Criatura'},
  ],
  2:[
    {action:'Mudar',aspect:'Frio',focus:'Trevas'},
    {action:'Capturar',aspect:'Escondido',focus:'Morte'},
    {action:'Comandar',aspect:'Perigoso',focus:'Defesa'},
    {action:'Controlar',aspect:'Obscuro',focus:'Profundezas'},
    {action:'Criar',aspect:'Morto',focus:'Dúvidas'},
    {action:'Desafiar',aspect:'Protegido',focus:'Sonhos'},
  ],
  3:[
    {action:'Demandar',aspect:'Desolado',focus:'Destino'},
    {action:'Descobrir',aspect:'Destruído',focus:'Fogo'},
    {action:'Disfarçar',aspect:'Terrível',focus:'Povo'},
    {action:'Aguentar',aspect:'Vazio',focus:'Seguidores'},
    {action:'Escapar',aspect:'Maligno',focus:'Ganância'},
    {action:'Esquivar',aspect:'Apagado',focus:'Refúgio'},
  ],
  4:[
    {action:'Explorar',aspect:'Distante',focus:'História'},
    {action:'Encontrar',aspect:'Feroz',focus:'Honra'},
    {action:'Focar',aspect:'Pressentido',focus:'Jornada'},
    {action:'Reunir',aspect:'Esquecido',focus:'Parente'},
    {action:'Guardar',aspect:'Frágil',focus:'Conhecimento'},
    {action:'Guiar',aspect:'Aterrorizante',focus:'Terra'},
  ],
  5:[
    {action:'Esconder',aspect:'Deprimido',focus:'Líder'},
    {action:'Atrapalhar',aspect:'Crescente',focus:'Lenda'},
    {action:'Acumular',aspect:'Oculto',focus:'Vida'},
    {action:'Segurar',aspect:'Malfadado',focus:'Luz'},
    {action:'Caçar',aspect:'Impenetrável',focus:'Sorte'},
    {action:'Jornadear',aspect:'Inspirador',focus:'Memória'},
  ],
  6:[
    {action:'Liderar',aspect:'Isolado',focus:'Mensagem'},
    {action:'Aprender',aspect:'Elevado',focus:'Soberania'},
    {action:'Abandonar',aspect:'Perdido',focus:'Natureza'},
    {action:'Perder',aspect:'Ameaçador',focus:'Dor'},
    {action:'Lamentar',aspect:'Poderoso',focus:'Caminho'},
    {action:'Mover',aspect:'Misterioso',focus:'Patrono'},
  ],
  7:[
    {action:'Persistir',aspect:'Nobre',focus:'Perigo'},
    {action:'Preservar',aspect:'Obstruído',focus:'Plano'},
    {action:'Prevenir',aspect:'Velho',focus:'Poder'},
    {action:'Recusar',aspect:'Agourento',focus:'Profecia'},
    {action:'Rejeitar',aspect:'Aberto',focus:'Missão'},
    {action:'Remover',aspect:'Pacífico',focus:'Abrigo'},
  ],
  8:[
    {action:'Reabastecer',aspect:'Restaurado',focus:'Enigma'},
    {action:'Restaurar',aspect:'Resguardado',focus:'Ruínas'},
    {action:'Tramar',aspect:'Silencioso',focus:'Rumor'},
    {action:'Procurar',aspect:'Simples',focus:'Segredo'},
    {action:'Agarrar',aspect:'Pequeno',focus:'Perícia'},
    {action:'Compartilhar',aspect:'Sombrio',focus:'Música'},
  ],
  9:[
    {action:'Eliminar',aspect:'Pétreo',focus:'Conto'},
    {action:'Roubar',aspect:'Robusto',focus:'Força'},
    {action:'Invocar',aspect:'Atingido',focus:'Tempo'},
    {action:'Render-se',aspect:'Teimoso',focus:'Ferramenta'},
    {action:'Cercar',aspect:'Retorcido',focus:'Tesouro'},
    {action:'Ameaçar',aspect:'Anormal',focus:'Confiança'},
  ],
  10:[
    {action:'Transformar',aspect:'Velado',focus:'Verdade'},
    {action:'Agarrar',aspect:'Vigoroso',focus:'Vingança'},
    {action:'Trapacear',aspect:'Exausto',focus:'Riqueza'},
    {action:'Descobrir',aspect:'Ermo',focus:'Arma'},
    {action:'Salvaguardar',aspect:'Desgraçado',focus:'Ermos'},
    {action:'Resistir',aspect:'Jovem',focus:'Desejo'},
  ],
};

export function striderLore(featResult: number, successDie: number): LoreRow {
  const feat=Math.max(1,Math.min(12,Math.trunc(Number(featResult)||1)));
  const die=Math.max(1,Math.min(6,Math.trunc(Number(successDie)||1)));
  return LORE[feat][die-1];
}

export type StriderJourneyEventType =
  | 'terrible-misfortune'|'despair'|'poor-choices'|'mishap'|'shortcut'|'chance-meeting'|'joyful-sight';

export const STRIDER_JOURNEY_EVENT: Record<StriderJourneyEventType,{
  label:string; fatigue:number; onFailure?:string; onSuccess?:string;
}> = {
  'terrible-misfortune':{label:'Infortúnio Terrível',fatigue:3,onFailure:'Ferido'},
  'despair':{label:'Desespero',fatigue:2,onFailure:'+2 Sombra (Pavor)'},
  'poor-choices':{label:'Más Escolhas',fatigue:2,onFailure:'+1 Sombra (Medo)'},
  'mishap':{label:'Azar',fatigue:2,onFailure:'+1 dia de Jornada e +1 Fadiga adicional'},
  'shortcut':{label:'Atalho',fatigue:1,onSuccess:'-1 dia de Jornada'},
  'chance-meeting':{label:'Encontro Fortuito',fatigue:1,onSuccess:'Nenhuma Fadiga; encontro favorável'},
  'joyful-sight':{label:'Visão Alegre',fatigue:0,onSuccess:'+1 Esperança'},
};

export const STRIDER_JOURNEY_DETAILS: Record<StriderJourneyEventType,Array<{detail:string;skill?:'Caçada'|'Exploração'|'Vigilância';notable?:boolean}>> = {
  'terrible-misfortune':[
    {detail:'Encontro Notável',notable:true},
    {detail:'Predador rival',skill:'Caçada'},
    {detail:'Clima violento',skill:'Exploração'},
    {detail:'Perigo oculto',skill:'Vigilância'},
    {detail:'Terreno perigoso',skill:'Exploração'},
    {detail:'Inimigo o perseguindo',skill:'Vigilância'},
  ],
  'despair':[
    {detail:'Servos do Inimigo',skill:'Vigilância'},
    {detail:'Encontro Notável',notable:true},
    {detail:'Presença de pesadelo',skill:'Vigilância'},
    {detail:'Vigor desaparecendo',skill:'Caçada'},
    {detail:'Local corrompido',skill:'Exploração'},
    {detail:'Cena terrível ou presságio agourento',skill:'Vigilância'},
  ],
  'poor-choices':[
    {detail:'Provisões mal administradas',skill:'Caçada'},
    {detail:'Caminho rebelde',skill:'Exploração'},
    {detail:'Perigo negligenciado',skill:'Vigilância'},
    {detail:'Presa perdida',skill:'Caçada'},
    {detail:'Ambientes desorientadores',skill:'Exploração'},
    {detail:'Visões assustadoras',skill:'Vigilância'},
  ],
  'mishap':[
    {detail:'Vida selvagem esparsa',skill:'Caçada'},
    {detail:'Direção perdida',skill:'Exploração'},
    {detail:'Caminho obstruído',skill:'Vigilância'},
    {detail:'Pedreira evasiva',skill:'Caçada'},
    {detail:'Terreno acidentado',skill:'Exploração'},
    {detail:'Inimigos errantes',skill:'Vigilância'},
  ],
  'shortcut':[
    {detail:'Trilha de caça',skill:'Caçada'},
    {detail:'Caminho isolado',skill:'Exploração'},
    {detail:'Trilhas úteis',skill:'Vigilância'},
    {detail:'Guia animal',skill:'Caçada'},
    {detail:'Clima favorável',skill:'Exploração'},
    {detail:'Ponto de referência familiar',skill:'Vigilância'},
  ],
  'chance-meeting':[
    {detail:'Caçador solitário',skill:'Caçada'},
    {detail:'Companheiro de viagem',skill:'Exploração'},
    {detail:'Observador discreto',skill:'Vigilância'},
    {detail:'Nobre besta',skill:'Caçada'},
    {detail:'Acampamento afastado',skill:'Exploração'},
    {detail:'Reunião auspiciosa',notable:true},
  ],
  'joyful-sight':[
    {detail:'Criaturas majestosas',skill:'Caçada'},
    {detail:'Vista inspiradora',skill:'Exploração'},
    {detail:'Ser benévolo',skill:'Vigilância'},
    {detail:'Forragem abundante',skill:'Caçada'},
    {detail:'Monumento antigo',skill:'Exploração'},
    {detail:'Santuário pacífico',notable:true},
  ],
};

export function striderJourneyEventFromFeat(featResult: number): StriderJourneyEventType {
  const feat=Math.max(1,Math.min(12,Math.trunc(Number(featResult)||1)));
  if(feat===12) return 'joyful-sight';
  if(feat===11) return 'terrible-misfortune';
  if(feat===10) return 'chance-meeting';
  if(feat>=8) return 'shortcut';
  if(feat>=4) return 'mishap';
  if(feat>=2) return 'poor-choices';
  return 'despair';
}

export function striderJourneyEventDetail(type: StriderJourneyEventType, detailDie: number) {
  const die=Math.max(1,Math.min(6,Math.trunc(Number(detailDie)||1)));
  return STRIDER_JOURNEY_DETAILS[type][die-1];
}
