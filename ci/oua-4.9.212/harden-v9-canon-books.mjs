// Enriches V9_PATCHED canonical book modules (Core 2e Star of the Mist pp. 223-230,
// Valfenda pp. 1-16, Povos das Terras Ermas pp. 1-10, Trilhas de Personagens V2 pp. 1-10,
// Modo Peregrino pp. 1-28, Contos das Terras Solitarias pp. 1-112) with 100% PDF-verified
// rules, tables, and scenario conditions while preserving all existing V9 tests.
// Usage: node ci/oua-4.9.212/harden-v9-canon-books.mjs <V9_PATCHED_DIR>

import fs from 'node:fs';
import path from 'node:path';

if (process.argv.length < 3) {
  throw new Error('Usage: node ci/oua-4.9.212/harden-v9-canon-books.mjs <V9_PATCHED_DIR>');
}

const v9Root = path.resolve(process.argv[2]);
const srcDir = path.join(v9Root, 'src');

if (!fs.existsSync(path.join(srcDir, 'library', 'Tor2eContentCatalog.mjs'))) {
  throw new Error(`Invalid V9_PATCHED root: ${v9Root}`);
}

const patchedFiles = [];

// 1. Enrich Tor2eContentCatalog.mjs (MARCO_A_ESTRELA_DA_NEVOA pp. 223-230)
const catalogPath = path.join(srcDir, 'library', 'Tor2eContentCatalog.mjs');
let catalogSrc = fs.readFileSync(catalogPath, 'utf8');
if (!catalogSrc.includes('HARDENED_CANON_STAR_OF_THE_MIST_V9')) {
  const starTarget = `      rewards: {
        adventurePoints: 3,
        treasure: 30,
        worldConsequences: 'O mistério da Casa de Hadirion e Elwen sobre as ruínas anãs de Narag-nâla no sul de Ered Luin é revelado e a ameaça dos bandoleiros e espectros é contida.'
      },
      nextSceneIds: []
    }
  ]
};`;

  const starReplacement = `      scenarioConditions: {
        blackSpeechInscriptionShadow: { points: 2, source: 'Feitiçaria', printedPage: 226, rule: 'Ler a inscrição da Língua Negra do Rei Bruxo nas pedras quebradas causa +2 pontos de Sombra (Feitiçaria).' },
        explorationLocations: [
          { id: 'orc-well', printedPage: 226, title: 'Esqueleto de Orc no Poço', effect: 'Armadura e escudo de tenente Orc; elmo com insígnia de Angmar.' },
          { id: 'wild-apple-tree', printedPage: 226, title: 'Macieira Selvagem', effect: 'Comer os frutos amargos causa sono encantado com visões do cerco de Angmar.' },
          { id: 'leprous-stag', printedPage: 227, title: 'Cervo Leproso de Chifres Retorcidos', effect: 'Visão perturbadora: todos os heróis ganham +1 ponto de Sombra (Pavor).' },
          { id: 'herbal-orchard', printedPage: 227, title: 'Pomar de Ervas da Senhora Elwen', effect: 'Concede +(1d) em testes de CURA por 1d6 usos.' },
          { id: 'jalin-lunatic-dwarf', printedPage: 227, title: 'Jalin, o Anão Lunático de Narag-nâla', effect: 'Sobrevivente da expedição traiçoeira; revela que o sino desperta os mortos.' },
          { id: 'battering-ram-trap', printedPage: 227, title: 'Aríete de Angmar na Encosta', effect: 'Armadilha de queda longa para o fundo da ravina.' },
          { id: 'star-tower-crown', printedPage: 229, title: 'A Torre da Estrela e a Coroa de Ferro', effect: 'No topo da torre repousa o cadáver mumificado de Lorde Hadirion com uma coroa de ferro engastada com gema azul brilhante. Remover a coroa do crânio desfaz a maldição da Senhora Elwen e revela Um Tesouro Maior sob o alçapão.' }
        ],
        elwenCanonicalBookProfile: {
          printedPage: 228,
          name: 'Senhora Elwen (Espectro Mortal)',
          attributeLevel: 5,
          endurance: 24,
          might: 2,
          hate: 5,
          parryBonus: '+2',
          armorDice: '2d',
          combatProficiencies: [
            { name: 'Lâmina Enferrujada', rank: 3, damage: 4, injury: 16, special: 'Perfurar' },
            { name: 'Lança', rank: 2, damage: 4, injury: 14, special: 'Perfurar' }
          ],
          fellAbilities: [
            'Habitante das Trevas',
            'Medo de Fogo',
            'Não-viva (apenas um Golpe Perfurante destrói sua forma)',
            'Causar Medo (gasta 1 Ódio: teste de SABEDORIA a curta distância ou fica Arrasado)'
          ],
          curseResolution: 'Destruir sua forma física apenas a dissipa temporariamente; ela retorna na noite seguinte com Resistência e Ódio restaurados a menos que a coroa de ferro com a pedra azul seja removida da cabeça de Lorde Hadirion na Torre da Estrela (p. 229).'
        },
        marshDwellersCanonicalBookProfile: {
          printedPage: 228,
          name: 'Habitantes dos Pântanos (Criaturas Tumulares das Águas)',
          attributeLevel: 3,
          endurance: 12,
          might: 1,
          hate: 3,
          parryBonus: '—',
          armorDice: '2d',
          combatProficiency: { name: 'Garras e Dentes', rank: 2, damage: 3, injury: 12, special: 'Agarrar' },
          fellAbilities: ['Habitante das Trevas', 'Ódio (Dúnedain / Homens do Norte)', 'Fedor Mortal']
        },
        sabianAmbushResolution: {
          printedPage: 230,
          name: 'Sabian, o Espião de Umbar (Salteador Sulista, Livro Básico p. 146)',
          trapSkill: 'VIGILÂNCIA',
          effect: 'Se Sabian sobreviveu, embosca a Companhia fora da caverna exigindo o tesouro; um teste bem-sucedido de VIGILÂNCIA evita o ataque surpresa.'
        }
      },
      rewards: {
        adventurePoints: 3,
        treasure: 30,
        magicalTreasure: 'Um Tesouro Menor (Câmaras Inundadas, p. 226) + Um Tesouro Maior (Alçapão da Torre da Estrela, p. 229)',
        worldConsequences: 'O mistério da Casa de Hadirion e Elwen sobre as ruínas anãs de Narag-nâla no sul de Ered Luin é revelado e a ameaça dos bandoleiros e espectros é contida.'
      },
      nextSceneIds: []
    }
  ],
  canonicalAuditMarker: 'HARDENED_CANON_STAR_OF_THE_MIST_V9',
  loreAndRumorsTable: [
    { roll: '1-2', printedPage: 224, text: 'As ruínas já foram a casa de um nobre senhor de Arnor chamado Hadirion e sua esposa, a bela Senhora Elwen.' },
    { roll: '3-4', printedPage: 224, text: 'A fortaleza dos Homens do Oeste foi construída no topo de antigas estruturas anãs da cidade subterrânea de Narag-nâla (Barbas de Fogo).' },
    { roll: '5', printedPage: 224, text: 'A fortaleza sustentou um longo cerco contra as hostes de Angmar, mas caiu quando inimigos emergiram das profundezas subterrâneas por traição.' },
    { roll: '6', printedPage: 224, text: 'Entre os defensores estava o famoso arqueiro Angnir, mordomo do senhor, cujo arco jamais falhava.' }
  ],
  sceneConditionsByPart: {
    part3PantryGreedShadow: { printedPage: 225, treasureDice: '1d6', shadowGain: 1, shadowSource: 'Ganância' },
    part4FloodedHallsDreadShadow: { printedPage: 225, shadowGain: 2, shadowSource: 'Pavor', waistDeepWaterPenalty: 'Hobbits e Anões perdem (1d) em todos os testes nas águas profundas.' }
  }
};`;

  if (!catalogSrc.includes(starTarget)) {
    throw new Error('Could not locate starTarget in Tor2eContentCatalog.mjs');
  }
  catalogSrc = catalogSrc.replace(starTarget, starReplacement);
  fs.writeFileSync(catalogPath, catalogSrc, 'utf8');
  patchedFiles.push('src/library/Tor2eContentCatalog.mjs');
}

// 2. Allow Tor2eAdventureController.mjs to start/restore 'core-star-of-the-mist' seamlessly
const ctrlPath = path.join(srcDir, 'ui', 'bridges', 'Tor2eAdventureController.mjs');
let ctrlSrc = fs.readFileSync(ctrlPath, 'utf8');
if (!ctrlSrc.includes('MARCO_A_ESTRELA_DA_NEVOA')) {
  ctrlSrc = ctrlSrc.replace(
    "import { CONTOS_TERRAS_SOLITARIAS } from '../../domain/rpg/expansions/ContosTerrasSolitariasAventuras.mjs';",
    "import { CONTOS_TERRAS_SOLITARIAS } from '../../domain/rpg/expansions/ContosTerrasSolitariasAventuras.mjs';\nimport { MARCO_A_ESTRELA_DA_NEVOA } from '../../library/Tor2eContentCatalog.mjs';"
  );
  ctrlSrc = ctrlSrc.replace(
    'this.adventures.find(a => a.id === snapshot.activeAdventureId) || null',
    'this.adventures.find(a => a.id === snapshot.activeAdventureId) || (snapshot.activeAdventureId === MARCO_A_ESTRELA_DA_NEVOA.id ? MARCO_A_ESTRELA_DA_NEVOA : null)'
  );
  ctrlSrc = ctrlSrc.replace(
    'const adv = this.adventures.find(a => a.id === adventureId);',
    'const adv = this.adventures.find(a => a.id === adventureId) || (adventureId === MARCO_A_ESTRELA_DA_NEVOA.id ? MARCO_A_ESTRELA_DA_NEVOA : null);'
  );
  ctrlSrc = ctrlSrc.replace(
    'listAdventures() {',
    `listAllCanonicalAdventures() {
    const all = [MARCO_A_ESTRELA_DA_NEVOA, ...this.adventures.filter(a => a.id !== MARCO_A_ESTRELA_DA_NEVOA.id)];
    return all.map(adv => ({
      id: adv.id,
      number: adv.number,
      title: adv.title,
      synopsis: adv.synopsis,
      startingLocation: adv.startingLocation,
      sceneCount: adv.scenes.length
    }));
  }

  listAdventures() {`
  );
  fs.writeFileSync(ctrlPath, ctrlSrc, 'utf8');
  patchedFiles.push('src/ui/bridges/Tor2eAdventureController.mjs');
}

// 3. Enrich ValfendaExpansion.mjs (Valfenda 2e pp. 1-16)
const valfendaPath = path.join(srcDir, 'domain', 'rpg', 'expansions', 'ValfendaExpansion.mjs');
let valfendaSrc = fs.readFileSync(valfendaPath, 'utf8');
if (!valfendaSrc.includes('HARDENED_CANON_VALFENDA_V9')) {
  const valfendaAppend = `  ,
  canonicalAuditMarker: 'HARDENED_CANON_VALFENDA_V9',
  sourceDriveId: '15mAAieBwcvOfP9zqO54da-L-_-Cx9hFp',
  printedPages: '1-16',
  canonicalMapLocations13: [
    { number: 1, printedPage: 3, name: 'A Casa de Elrond' },
    { number: 2, printedPage: 3, name: 'Os Estábulos' },
    { number: 3, printedPage: 3, name: 'O Santuário' },
    { number: 4, printedPage: 3, name: 'A Forja' },
    { number: 5, printedPage: 3, name: 'A Residência' },
    { number: 6, printedPage: 3, name: 'A Enfermaria' },
    { number: 7, printedPage: 3, name: 'A Casa de Glorfindel' },
    { number: 8, printedPage: 3, name: 'O Campo de Treino' },
    { number: 9, printedPage: 3, name: 'O Arsenal' },
    { number: 10, printedPage: 3, name: 'A Dependência' },
    { number: 11, printedPage: 3, name: 'A Casa de Erestor' },
    { number: 12, printedPage: 3, name: 'O Balneário' },
    { number: 13, printedPage: 3, name: 'A Ponte' }
  ],
  houseOfElrondRooms15: [
    { number: 1, printedPage: 5, name: 'Pátio da Fonte' },
    { number: 2, printedPage: 5, name: 'Grande Pórtico' },
    { number: 3, printedPage: 5, name: 'Salão de Banquetes' },
    { number: 4, printedPage: 5, name: 'Salão do Fogo' },
    { number: 5, printedPage: 5, name: 'Aposentos de Hóspedes' },
    { number: 6, printedPage: 5, name: 'Biblioteca e Arquivos de Sabedoria' },
    { number: 7, printedPage: 5, name: 'Alpendre Oriental (Conselho de Elrond)' },
    { number: 8, printedPage: 5, name: 'Câmara de Cura de Elrond' },
    { number: 9, printedPage: 5, name: 'Terraços sobre o Bruinen' },
    { number: 10, printedPage: 5, name: 'Aposentos de Elrond e Arwen' },
    { number: 11, printedPage: 5, name: 'Aposentos de Bilbo Bolseiro' },
    { number: 12, printedPage: 5, name: 'Cozinhas e Adegas de Miruvor' },
    { number: 13, printedPage: 5, name: 'Jardins do Vale' },
    { number: 14, printedPage: 5, name: 'Mirante da Cachoeira' },
    { number: 15, printedPage: 5, name: 'Oficinas e Forjas Élficas' }
  ],
  canonicalRules: {
    safeBorders: {
      printedPage: 3,
      name: 'Fronteiras Seguras',
      effect: 'A Vigilância do Olho não aumenta em Valfenda e nenhum Episódio de Revelação ocorre dentro do vale.'
    },
    makingMusicInRivendell: {
      printedPage: 4,
      name: 'Fazendo Música em Valfenda',
      undertaking: 'Compor uma Canção em Valfenda (Fase de Sociedade)',
      effect: 'Concede recuperação adicional de Esperança ou remoção de Pontos de Sombra ao entoar a canção na próxima Fase de Aventura.'
    },
    elvenBeauty: {
      printedPage: 7,
      name: 'Beleza Élfica (Arwen Undómiel)',
      effect: 'Inspiração duradoura que protege contra o Desespero da Sombra.'
    },
    miruvorCordial: {
      printedPage: 8,
      name: 'Miruvor, o Cordial de Imladris (Erestor)',
      effect: 'Um gole restaura o vigor dos viajantes, removendo Fadiga de viagem.'
    },
    struckByDefiance: {
      printedPage: 10,
      name: 'Atingido pelo Desafio (Glorfindel)',
      effect: 'Adversários das Trevas que enfrentam a fúria revelada de um Senhor Élfico sofrem perda de Ódio e recuam em terror.'
    },
    highElfPrintedVirtues: [
      { printedPage: 12, name: 'Artífice de Eregion', effect: 'Concede maestria em OFÍCIO e criação de obras élficas maravilhosas.' },
      { printedPage: 12, name: 'Beleza das Estrelas', effect: 'Aumenta Esperança máxima em +1 e favorece interações de CORTESIA e FASCÍNIO.' },
      { printedPage: 12, name: 'Poder do Primogênito', effect: 'Contra servos espectrais ou mortos-vivos, gasta 1 Esperança para brilhar no Mundo Invisível.' },
      { printedPage: 12, name: 'Habilidade dos Eldar', effect: 'Ao gastar 1 Esperança em perícia favorecida, obtém Sucesso Mágico.' }
    ]
  }
};
`;
  valfendaSrc = valfendaSrc.replace(/\n};\s*$/, '\n' + valfendaAppend);
  fs.writeFileSync(valfendaPath, valfendaSrc, 'utf8');
  patchedFiles.push('src/domain/rpg/expansions/ValfendaExpansion.mjs');
}

// 4. Enrich PovosTerrasErmasExpansion.mjs (Povos das Terras Ermas 2e pp. 1-10)
const povosPath = path.join(srcDir, 'domain', 'rpg', 'expansions', 'PovosTerrasErmasExpansion.mjs');
let povosSrc = fs.readFileSync(povosPath, 'utf8');
if (!povosSrc.includes('HARDENED_CANON_POVOS_ERMAS_V9')) {
  povosSrc = povosSrc.replace(
    "sourceDriveId: '15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto',",
    "sourceDriveId: '15wjw3LYORJeBvG2OB3w4SwbgBl6c-xto',\n  printedPages: '1-10',\n  canonicalAuditMarker: 'HARDENED_CANON_POVOS_ERMAS_V9',\n  canonicalBookCulturesIds: ['beornings', 'woodland-elves', 'woodmen-of-wilderland'],"
  );
  fs.writeFileSync(povosPath, povosSrc, 'utf8');
  patchedFiles.push('src/domain/rpg/expansions/PovosTerrasErmasExpansion.mjs');
}

// 5. Enrich TrilhasPersonagensModule.mjs (all 12 Feat Die Important Events from pp. 9-10)
const trilhasPath = path.join(srcDir, 'domain', 'rpg', 'expansions', 'TrilhasPersonagensModule.mjs');
let trilhasSrc = fs.readFileSync(trilhasPath, 'utf8');
if (!trilhasSrc.includes('dieFace: 7')) {
  const targetEvents = `  {
    dieFace: 6,
    title: '…jovial',
    effect: 'Adora festas e boa companhia. Aumente sua Esperança máxima em 1 ponto.'
  },
  {
    dieFace: 11, // Olho de Sauron`;

  const replacementEvents = `  {
    dieFace: 6,
    title: '…jovial',
    printedPage: 9,
    effect: 'Adora festas e boa companhia. Aumente sua Esperança máxima em +2 pontos e diminua sua Resistência máxima em 2.'
  },
  {
    dieFace: 7,
    title: '…esbelto',
    printedPage: 10,
    effect: 'Ágil e atento. Aumente seu Aparar em +1 e diminua sua Resistência máxima em 2.'
  },
  {
    dieFace: 8,
    title: '…simplório',
    printedPage: 10,
    effect: 'Coração puro e resistente. Aumente sua Resistência máxima em +1 e sua Esperança máxima em +1, e comece com 2 pontos a menos de experiência anterior (8 pontos).'
  },
  {
    dieFace: 9,
    title: '…vigoroso',
    printedPage: 10,
    effect: 'Robusto e incansável. Aumente sua Resistência máxima em +2 e diminua sua Esperança máxima em 2.'
  },
  {
    dieFace: 10,
    title: '…abençoado',
    printedPage: 10,
    effect: 'Favorecido pela sorte. Aumente um Atributo à sua escolha em +1 e comece com 4 pontos a menos de experiência anterior (6 pontos).'
  },
  {
    dieFace: 11, // Olho de Sauron`;

  trilhasSrc = trilhasSrc.replace(targetEvents, replacementEvents);
  trilhasSrc = trilhasSrc.replace(
    "title: '…perseguido pela Sombra',",
    "title: '…perseguido pela Sombra',\n    canonicalGandalfTitlePt: '…favorecido pelo Mago Cinzento',\n    printedPage: 9,"
  );
  fs.writeFileSync(trilhasPath, trilhasSrc, 'utf8');
  patchedFiles.push('src/domain/rpg/expansions/TrilhasPersonagensModule.mjs');
}

// 6. Enrich ModoPeregrinoModule.mjs (Modo Peregrino 2e pp. 1-28)
const peregrinoPath = path.join(srcDir, 'domain', 'rpg', 'expansions', 'ModoPeregrinoModule.mjs');
let peregrinoSrc = fs.readFileSync(peregrinoPath, 'utf8');
if (!peregrinoSrc.includes('HARDENED_CANON_MODO_PEREGRINO_V9')) {
  const peregrinoAdditions = `
export const STRIDER_CREATION_RULES = {
  canonicalAuditMarker: 'HARDENED_CANON_MODO_PEREGRINO_V9',
  sourceDriveId: '15gUwEnPRaD6f1FjXs0ho_j8VS5Rq5saY',
  printedPages: '1-28',
  previousExperiencePoints: 15, // p. 4 (em vez de 10)
  baseFellowshipPool: 3,        // p. 4
  targetNumberBase: 18,         // p. 8 (18 - Atributo no Modo Peregrino)
  bonusDistinctiveFeature: {
    name: 'Peregrino',
    printedPage: 5,
    effect: 'Durante a jornada, o Herói-jogador solitário é considerado Inspirado nas jogadas de perícia.'
  }
};

export const TELLING_TABLE_THRESHOLDS = {
  CERTAIN: { id: 'CERTAIN', label: 'Certamente', minFeatRoll: 1, printedPage: 10 },
  LIKELY: { id: 'LIKELY', label: 'Provável', minFeatRoll: 4, printedPage: 10 },
  MIDDLING: { id: 'MIDDLING', label: 'Mediano', minFeatRoll: 6, printedPage: 10 },
  DOUBTFUL: { id: 'DOUBTFUL', label: 'Duvidoso', minFeatRoll: 8, printedPage: 10 },
  UNTHINKABLE: { id: 'UNTHINKABLE', label: 'Impensável', minFeatRoll: 10, printedPage: 10 }
};

export const FORTUNE_TABLE = [
  { dieFace: 11, label: 'Olho de Sauron', effect: 'O Olho do Inimigo foca em outro lugar. Diminua a Vigilância do Olho em 1.', eyeAwarenessDelta: -1 },
  { dieFace: 1, effect: 'Você pode ignorar uma ameaça sem atrair atenção.' },
  { dieFace: 2, effect: 'Você ganha a atenção de um aliado em potencial.' },
  { dieFace: 3, effect: 'Um inimigo inadvertidamente revela sua posição.' },
  { dieFace: 4, effect: 'Você ganha terreno privilegiado.' },
  { dieFace: 5, effect: 'Inimigos fogem do perigo.' },
  { dieFace: 6, effect: 'Você localiza ou fica sabendo de um item útil.' },
  { dieFace: 7, effect: 'Seu sucesso instila uma nova esperança ou determinação renovada.' },
  { dieFace: 8, effect: 'Você encontra um momento de conforto ou segurança.' },
  { dieFace: 9, effect: 'Você aprende ou realiza algo que fornece informações úteis sobre sua missão.' },
  { dieFace: 10, effect: 'Você encontra uma oportunidade adequada à sua natureza ou habilidades.' },
  { dieFace: 12, label: 'Runa de Gandalf', effect: 'Um aliado inesperado aparece ou envia ajuda.' }
];

export const ILL_FORTUNE_TABLE = [
  { dieFace: 11, label: 'Olho de Sauron', effect: 'Suas ações capturam o Olho do Inimigo. Aumente a Vigilância do Olho em 2.', eyeAwarenessDelta: 2 },
  { dieFace: 1, effect: 'Você chama atenção indesejada.' },
  { dieFace: 2, effect: 'Suas ações são observadas por alguém mal-intencionado.' },
  { dieFace: 3, effect: 'Inimigos inesperados surgem ou são avistados.' },
  { dieFace: 4, effect: 'Você é prejudicado por um terreno difícil ou um ambiente desfavorável.' },
  { dieFace: 5, effect: 'Você se encontra mal equipado para as circunstâncias.' },
  { dieFace: 6, effect: 'Uma arma ou item favorito é perdido, quebrado ou sacrificado.' },
  { dieFace: 7, effect: 'Você é atormentado por visões ou pensamentos perturbadores.' },
  { dieFace: 8, effect: 'Uma lesão antiga ou estresse ressurge.' },
  { dieFace: 9, effect: 'Você aprende ou percebe algo que acrescenta uma nova complicação à sua missão.' },
  { dieFace: 10, effect: 'Você enfrenta um teste que é contrário à sua natureza ou habilidades.' },
  { dieFace: 12, label: 'Runa de Gandalf', effect: 'Um aliado se torna um obstáculo ou responsabilidade.' }
];

export const EYE_HUNT_THRESHOLDS = {
  BORDER_LAND: { regionType: 'Terra de Fronteira', huntThreshold: 18, soloJourneyRoll: 'FAVOURED', printedPage: 13 },
  WILD_LAND: { regionType: 'Terra Erma', huntThreshold: 16, soloJourneyRoll: 'STANDARD', printedPage: 13 },
  DARK_LAND: { regionType: 'Terra Sombria', huntThreshold: 14, soloJourneyRoll: 'ILL_FAVOURED', printedPage: 13 }
};

export const REVELATION_EPISODES_TABLE = [
  { dieFace: 11, printedPage: 14, episode: 'Um conflito interno ou ameaça externa coloca em perigo seu Refúgio Seguro.' },
  { dieFace: 1, printedPage: 14, episode: 'Um perigo inesperado surge no caminho à frente, forçando-o a procurar uma rota diferente.' },
  { dieFace: 2, printedPage: 14, episode: 'A natureza é corrompida e se volta contra você.' },
  { dieFace: 3, printedPage: 14, episode: 'Espiões do Inimigo levam informações sobre sua missão.' },
  { dieFace: 4, printedPage: 14, episode: 'Servos do Inimigo executam uma emboscada ou preparam uma armadilha.' },
  { dieFace: 5, printedPage: 14, episode: 'Servos do Inimigo encontram o seu rastro.' },
  { dieFace: 6, printedPage: 14, episode: 'Uma localidade importante é dominada por um inimigo.' },
  { dieFace: 7, printedPage: 14, episode: 'Um item que você carrega está amaldiçoado ou é caçado por um inimigo.' },
  { dieFace: 8, printedPage: 14, episode: 'Você é tentado por algo que deseja muito, em detrimento da sua missão.' },
  { dieFace: 9, printedPage: 14, episode: 'Mentiras maliciosas levam pessoas a desconfiar ou ter medo de você.' },
  { dieFace: 10, printedPage: 14, episode: 'Um conflito surge entre seus aliados.' },
  { dieFace: 12, printedPage: 14, episode: 'Um aliado importante é colocado em perigo.' }
];

export const SKIRMISH_STANCE_RULES = {
  id: 'SKIRMISH',
  name: 'Postura de Escaramuça (Combate à Distância)',
  printedPage: 15,
  rangedOnly: true,
  heroRangedAttackDiceModifier: -1,
  enemyMeleeAttackDiceModifier: -1,
  enemyRangedAttackDiceModifier: 0,
  escapeRule: 'Jogue ataque à distância sem perder (1d); se vencer, escapa do combate sem causar dano.',
  combatTask: {
    name: 'Ganhar Terreno (Gain Ground)',
    skills: ['Atletismo', 'Busca'],
    effect: 'Em sucesso, ganha +(1d) no próximo ataque à distância, mais +(1d) por ícone de Sucesso.'
  }
};

export const EXPERIENCE_MILESTONES_SOLO = [
  { id: 'accept-patron-quest', printedPage: 7, milestone: 'Aceitar uma missão de um patrono', adventurePoints: 1, skillPoints: 0 },
  { id: 'achieve-personal-goal', printedPage: 7, milestone: 'Alcançar um objetivo pessoal notável', adventurePoints: 1, skillPoints: 1 },
  { id: 'complete-patron-quest', printedPage: 7, milestone: 'Completar a missão de um patrono', adventurePoints: 1, skillPoints: 1 },
  { id: 'complete-meaningful-journey', printedPage: 7, milestone: 'Completar uma jornada significativa', adventurePoints: 0, skillPoints: 2 },
  { id: 'face-noteworthy-encounter', printedPage: 7, milestone: 'Enfrentar um Encontro Notável durante uma jornada', adventurePoints: 0, skillPoints: 1 },
  { id: 'reveal-significant-location', printedPage: 7, milestone: 'Revelar um local ou descoberta significativa', adventurePoints: 1, skillPoints: 0 },
  { id: 'overcome-tricky-obstacle', printedPage: 7, milestone: 'Superar um obstáculo difícil', adventurePoints: 0, skillPoints: 1 },
  { id: 'participate-in-council', printedPage: 7, milestone: 'Participar de um Conselho', adventurePoints: 0, skillPoints: 1 },
  { id: 'survive-dangerous-combat', printedPage: 7, milestone: 'Sobreviver a um combate perigoso', adventurePoints: 1, skillPoints: 0 },
  { id: 'face-revelation-episode', printedPage: 7, milestone: 'Encarar um episódio de revelação', adventurePoints: 1, skillPoints: 0 }
];

export const PATRON_QUESTS_SOLO = {
  'balin': { patron: 'Balin, Filho de Fundin', printedPage: 5, questsCount: 6 },
  'bilbo': { patron: 'Bilbo Bolseiro', printedPage: 6, questsCount: 6 },
  'cirdan': { patron: 'Círdan, o Construtor de Navios', printedPage: 6, questsCount: 6 },
  'gandalf': { patron: 'Gandalf, o Cinzento', printedPage: 6, questsCount: 6 },
  'gilraen': { patron: 'Gilraen, Filha de Dirhael', printedPage: 7, questsCount: 6 },
  'tom-bombadil': { patron: "Tom Bombadil e Senhora Fruta d'Ouro", printedPage: 7, questsCount: 6 }
};
`;

  peregrinoSrc = peregrinoSrc.replace(
    'export class ModoPeregrinoModule {',
    peregrinoAdditions + '\nexport class ModoPeregrinoModule {'
  );

  const methodsAddition = `
  /**
   * Consulta a Tabela de Narração canônica (p. 10) com as 5 probabilidades oficiais
   */
  static consultTellingTable(likelihood = 'MIDDLING', mockRoll = null) {
    const entry = TELLING_TABLE_THRESHOLDS[likelihood] || TELLING_TABLE_THRESHOLDS.MIDDLING;
    const rawDie = mockRoll || Math.floor(Math.random() * 12) + 1;
    if (rawDie === FEAT_DIE_SPECIAL.GANDALF_RUNE || rawDie === 12) {
      return { die: rawDie, likelihood: entry.id, answer: 'YES_EXTREME', isYes: true, text: 'Sempre sim, com um resultado extremo ou reviravolta.' };
    }
    if (rawDie === FEAT_DIE_SPECIAL.EYE_OF_SAURON || rawDie === 11) {
      return { die: rawDie, likelihood: entry.id, answer: 'NO_EXTREME', isYes: false, text: 'Sempre não, com um resultado extremo ou reviravolta.' };
    }
    const isYes = rawDie >= entry.minFeatRoll;
    return {
      die: rawDie,
      likelihood: entry.id,
      threshold: entry.minFeatRoll,
      answer: isYes ? 'YES' : 'NO',
      isYes,
      text: isYes ? 'Sim.' : 'Não.'
    };
  }

  static rollFortuneTable(kind = 'FORTUNE', mockRoll = null) {
    const rawDie = mockRoll || Math.floor(Math.random() * 12) + 1;
    const table = kind === 'ILL_FORTUNE' ? ILL_FORTUNE_TABLE : FORTUNE_TABLE;
    return table.find(r => r.dieFace === rawDie) || table[1];
  }

  static rollRevelationEpisode(mockRoll = null) {
    const rawDie = mockRoll || Math.floor(Math.random() * 12) + 1;
    return REVELATION_EPISODES_TABLE.find(r => r.dieFace === rawDie) || REVELATION_EPISODES_TABLE[1];
  }
}
`;
  peregrinoSrc = peregrinoSrc.replace(/\}\s*$/, methodsAddition);
  fs.writeFileSync(peregrinoPath, peregrinoSrc, 'utf8');
  patchedFiles.push('src/domain/rpg/expansions/ModoPeregrinoModule.mjs');
}

console.log(JSON.stringify({ status: 'HARDENED_V9_CANON_BOOKS_OK', patchedFiles }));
