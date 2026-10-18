// Star Wars: Edge Studio / Fantasy Flight Games
// Canonical Career Sourcebook Specializations (24 canonical specializations)
// Sources: Enter the Unknown, Dangerous Covenants, Far Horizons, Fly Casual,
// No Disintegrations, Special Modifications, Lead by Example, Forged in Battle,
// Stay on Target, Keeping the Peace, Disciples of Harmony, Endless Vigil, Savage Spirits, Knights of Fate.

const source = (book, printedPage) => Object.freeze({ book, printedPage });

export const EXPANDED_CAREER_SPECIALIZATIONS = Object.freeze({
  // EotE Careers:
  bounty_hunter: Object.freeze({
    martial_artist: Object.freeze({
      key: 'martial_artist',
      name: 'Artista Marcial (Martial Artist)',
      careerKey: 'bounty_hunter',
      source: source('No Disintegrations', 28),
      careerSkills: Object.freeze(['pelea', 'atletismo', 'coordinacion', 'disciplina']),
      description: 'Mestre no combate desarmado e imobilizações letais que transformam o próprio corpo em uma arma de captura de recompensas.'
    }),
    skip_tracer: Object.freeze({
      key: 'skip_tracer',
      name: 'Rastreador de Fugitivos (Skip Tracer)',
      careerKey: 'bounty_hunter',
      source: source('No Disintegrations', 30),
      careerSkills: Object.freeze(['frialdad', 'engano', 'conocimiento_bajos_fondos', 'callejero']),
      description: 'Especialista em investigação urbana, inteligência de submundo e caça a foragidos que se escondem em populosos centros planetários.'
    })
  }),

  smuggler: Object.freeze({
    gunslinger: Object.freeze({
      key: 'gunslinger',
      name: 'Pistoleiro (Gunslinger)',
      careerKey: 'smuggler',
      source: source('Fly Casual', 26),
      careerSkills: Object.freeze(['armas_distancia_ligeras', 'coaccion', 'coordinacion', 'engano']),
      description: 'Saque rápido e tiroteio com empunhadura dupla de blasters; o terror das cantinas e duelos nos becos espaciais.'
    }),
    charmer: Object.freeze({
      key: 'charmer',
      name: 'Sedutor (Charmer)',
      careerKey: 'smuggler',
      source: source('Fly Casual', 24),
      careerSkills: Object.freeze(['encanto', 'frialdad', 'engano', 'negociacion']),
      description: 'Mestre da lábia, charme aristocrático e manipulação social para escapar de alfândegas e negociações perigosas.'
    })
  }),

  hired_gun: Object.freeze({
    heavy: Object.freeze({
      key: 'heavy',
      name: 'Especialista em Armas Pesadas (Heavy)',
      careerKey: 'hired_gun',
      source: source('Dangerous Covenants', 26),
      careerSkills: Object.freeze(['artilleria', 'armas_distancia_pesadas', 'atletismo', 'resistencia']),
      description: 'Operador de metralhadoras blaster giratórias, canhões de repetição e explosivos pesados de supressão.'
    }),
    enforcer: Object.freeze({
      key: 'enforcer',
      name: 'Cobrador / Carrasco (Enforcer)',
      careerKey: 'hired_gun',
      source: source('Dangerous Covenants', 24),
      careerSkills: Object.freeze(['pelea', 'coaccion', 'callejero', 'conocimiento_bajos_fondos']),
      description: 'Especialista em intimidação implacável, extorsão e violência direta a serviço de chefões do crime.'
    })
  }),

  technician: Object.freeze({
    droid_tech: Object.freeze({
      key: 'droid_tech',
      name: 'Técnico em Droides (Droid Tech)',
      careerKey: 'technician',
      source: source('Special Modifications', 28),
      careerSkills: Object.freeze(['mecanica', 'computadoras', 'conocimiento_educacion', 'engano']),
      description: 'Engenheiro mestre na programação neural, reprogramação tática e reconstrução avançada de droides.'
    }),
    cyber_tech: Object.freeze({
      key: 'cyber_tech',
      name: 'Cibertecnólogo (Cyber Tech)',
      careerKey: 'technician',
      source: source('Special Modifications', 26),
      careerSkills: Object.freeze(['mecanica', 'medicina', 'computadoras', 'disciplina']),
      description: 'Pioneiro em próteses cibernéticas aprimoradas e implantes biotecnológicos no limite da cibernética galáctica.'
    })
  }),

  // AoR Careers:
  commander: Object.freeze({
    figurehead: Object.freeze({
      key: 'figurehead',
      name: 'Líder Inspirador (Figurehead)',
      careerKey: 'commander',
      source: source('Lead by Example', 26),
      careerSkills: Object.freeze(['encanto', 'frialdad', 'engano', 'liderazgo']),
      description: 'Símbolo visível da causa rebelde, capaz de erguer a moral de frotas inteiras com discursos épicos.'
    }),
    instructor: Object.freeze({
      key: 'instructor',
      name: 'Instrutor Militar (Instructor)',
      careerKey: 'commander',
      source: source('Lead by Example', 28),
      careerSkills: Object.freeze(['liderazgo', 'disciplina', 'conocimiento_mundos_del_nucleo', 'resistencia']),
      description: 'Treinador de novos recrutas e organizador de planos de instrução tática rigorosos para o exército rebelde.'
    })
  }),

  soldier: Object.freeze({
    vanguard: Object.freeze({
      key: 'vanguard',
      name: 'Vanguarda (Vanguard)',
      careerKey: 'soldier',
      source: source('Forged in Battle', 28),
      careerSkills: Object.freeze(['atletismo', 'resistencia', 'armas_distancia_ligeras', 'vigilancia']),
      description: 'Tropas de choque que abrem brechas nas linhas imperiais e operam sob fogo concentrado na linha de frente.'
    }),
    trailblazer: Object.freeze({
      key: 'trailblazer',
      name: 'Pioneiro / Batedor de Terreno (Trailblazer)',
      careerKey: 'soldier',
      source: source('Forged in Battle', 30),
      careerSkills: Object.freeze(['supervivencia', 'percepcion', 'armas_distancia_pesadas', 'sigilo']),
      description: 'Especialista em guerra de guerrilha florestal e desbravamento de terrenos hostis desconhecidos.'
    })
  }),

  // F&D Careers:
  guardian: Object.freeze({
    colossus: Object.freeze({
      key: 'colossus',
      name: 'Colosso (Colossus)',
      careerKey: 'guardian',
      source: source('Keeping the Peace', 26),
      careerSkills: Object.freeze(['atletismo', 'resistencia', 'disciplina', 'armas_cuerpo_a_cuerpo']),
      description: 'Guardião de tenacidade lendária que canaliza a Força para absorver impactos que destruiriam blindados.'
    }),
    armorer: Object.freeze({
      key: 'armorer',
      name: 'Armeiro da Força (Armorer)',
      careerKey: 'guardian',
      source: source('Keeping the Peace', 24),
      careerSkills: Object.freeze(['mecanica', 'sabre_de_luz', 'conocimiento_lore', 'resistencia']),
      description: 'Artesão místico que forja armaduras imbuídas da Força e aprimora sabres de luz com precisão sagrada.'
    })
  }),

  mystic: Object.freeze({
    steel_hand_adept: Object.freeze({
      key: 'steel_hand_adept',
      name: 'Adepto da Mão de Aço (Steel Hand Adept)',
      careerKey: 'mystic',
      source: source('Disciples of Harmony', 28),
      careerSkills: Object.freeze(['pelea', 'coordinacion', 'disciplina', 'vigilancia']),
      description: 'Praticante de Teräs Käsi e artes marciais arcanas que desferem golpes com a força de impactos cinéticos de sabre.'
    }),
    magus: Object.freeze({
      key: 'magus',
      name: 'Mago Arcano (Magus)',
      careerKey: 'mystic',
      source: source('Unlimited Power', 28),
      careerSkills: Object.freeze(['coaccion', 'conocimiento_lore', 'supervivencia', 'disciplina']),
      description: 'Erudito em rituais esquecidos e conhecimentos proibidos da Força viva nas bordas selvagens da galáxia.'
    })
  }),

  consular: Object.freeze({
    arbiter: Object.freeze({
      key: 'arbiter',
      name: 'Árbitro da Paz (Arbiter)',
      careerKey: 'consular',
      source: source('Disciples of Harmony', 24),
      careerSkills: Object.freeze(['sabre_de_luz', 'negociacion', 'conocimiento_mundos_del_nucleo', 'percepcion']),
      description: 'Juiz e negociador implacável que desce em zonas de guerra para mediar tratados sob o fio do sabre de luz.'
    }),
    teacher: Object.freeze({
      key: 'teacher',
      name: 'Mestre / Preceptor (Teacher)',
      careerKey: 'consular',
      source: source('Disciples of Harmony', 30),
      careerSkills: Object.freeze(['disciplina', 'liderazgo', 'conocimiento_lore', 'percepcion']),
      description: 'Guia sábio que orienta aprendizes e padawans no domínio dos caminhos da Força e da sabedoria Jedi.'
    })
  }),

  sentinel: Object.freeze({
    investigator: Object.freeze({
      key: 'investigator',
      name: 'Investigador Urbano (Investigator)',
      careerKey: 'sentinel',
      source: source('Endless Vigil', 26),
      careerSkills: Object.freeze(['percepcion', 'callejero', 'conocimiento_bajos_fondos', 'vigilancia']),
      description: 'Detetive que utiliza intuição da Força e métodos forenses para desvendar conspirações nas megalópoles.'
    }),
    racer: Object.freeze({
      key: 'racer',
      name: 'Piloto de Corridas (Racer)',
      careerKey: 'sentinel',
      source: source('Endless Vigil', 28),
      careerSkills: Object.freeze(['coordinacion', 'frialdad', 'pilotar_planeta', 'pilotar_espacio']),
      description: 'Reflexos pré-cognitivos da Força aplicados a velocidade extrema em circuitos de Podracer e speeders.'
    })
  }),

  seeker: Object.freeze({
    executioner: Object.freeze({
      key: 'executioner',
      name: 'Executor (Executioner)',
      careerKey: 'seeker',
      source: source('Savage Spirits', 26),
      careerSkills: Object.freeze(['armas_cuerpo_a_cuerpo', 'armas_distancia_pesadas', 'supervivencia', 'vigilancia']),
      description: 'Caçador implacável especializado em rastrear e eliminar abominações do Lado Sombrio e predadores colossais.'
    }),
    hermit: Object.freeze({
      key: 'hermit',
      name: 'Eremita Selvagem (Hermit)',
      careerKey: 'seeker',
      source: source('Savage Spirits', 28),
      careerSkills: Object.freeze(['supervivencia', 'sigilo', 'disciplina', 'conocimiento_lore']),
      description: 'Sensitivo à Força em profunda simbiose com a vida selvagem, capaz de comungar telepaticamente com feras.'
    })
  }),

  warrior: Object.freeze({
    shii_cho_knight: Object.freeze({
      key: 'shii_cho_knight',
      name: 'Cavaleiro Shii-Cho (Shii-Cho Knight)',
      careerKey: 'warrior',
      source: source('Knights of Fate', 26),
      careerSkills: Object.freeze(['sabre_de_luz', 'atletismo', 'resistencia', 'armas_cuerpo_a_cuerpo']),
      description: 'Mestre da Forma I de esgrima Jedi (A Determinação), desferindo varreduras amplas contra múltiplos oponentes.'
    }),
    juyo_berserker: Object.freeze({
      key: 'juyo_berserker',
      name: 'Furioso Juyo (Juyo Berserker)',
      careerKey: 'warrior',
      source: source('Knights of Fate', 28),
      careerSkills: Object.freeze(['sabre_de_luz', 'coaccion', 'atletismo', 'resistencia']),
      description: 'Praticante da Forma VII, canalizando paixão e ferocidade cinética calculada em ataques avassaladores.'
    })
  })
});
