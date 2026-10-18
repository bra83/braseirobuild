// Star Wars: Force and Destiny — Core Rulebook
// Chapter II: Careers and Specializations & Chapter III: Character Creation (pp. 52-115)
// Canonical definition of Force & Destiny Careers, Specializations, and Character initialization.

import { FND_TALENT_TREES } from '../expansion/fndTalentTreesData.mjs';
import { normalizeMoralityState } from './morality.mjs';
import { SKILLS } from './character.mjs';

export const FND_CORE = 'FND_CORE';

export const FND_CAREERS = Object.freeze({
  consular: Object.freeze({
    key: 'consular',
    label: 'Consular',
    careerSkills: Object.freeze(['disciplina', 'lideranca', 'conocimiento_educacion', 'conocimiento_lore', 'negociacion', 'vigilancia']),
    specializations: Object.freeze({
      healer: Object.freeze({ key: 'healer', label: 'Curador', careerSkills: Object.freeze(['medicina', 'disciplina', 'conocimiento_educacion', 'resistencia']) }),
      niman_disciple: Object.freeze({ key: 'niman_disciple', label: 'Discípulo Niman', careerSkills: Object.freeze(['sabre_de_luz', 'disciplina', 'lideranca', 'negociacion']) }),
      sage: Object.freeze({ key: 'sage', label: 'Sábio', careerSkills: Object.freeze(['conocimiento_educacion', 'conocimiento_lore', 'conocimiento_guerra', 'negociacion']) })
    })
  }),
  guardian: Object.freeze({
    key: 'guardian',
    label: 'Guardião',
    careerSkills: Object.freeze(['atletismo', 'disciplina', 'percepcion', 'resistencia', 'supervivencia', 'lideranca']),
    specializations: Object.freeze({
      peacekeeper: Object.freeze({ key: 'peacekeeper', label: 'Pacificador', careerSkills: Object.freeze(['disciplina', 'lideranca', 'percepcion', 'pilotar_planetario']) }),
      protector: Object.freeze({ key: 'protector', label: 'Protetor', careerSkills: Object.freeze(['atletismo', 'resistencia', 'vigilancia', 'medicina']) }),
      soresu_defender: Object.freeze({ key: 'soresu_defender', label: 'Defensor Soresu', careerSkills: Object.freeze(['sabre_de_luz', 'disciplina', 'resistencia', 'conocimiento_lore']) })
    })
  }),
  mystic: Object.freeze({
    key: 'mystic',
    label: 'Místico',
    careerSkills: Object.freeze(['encanto', 'coaccion', 'conocimiento_lore', 'percepcion', 'vigilancia', 'sigilo']),
    specializations: Object.freeze({
      advisor: Object.freeze({ key: 'advisor', label: 'Conselheiro', careerSkills: Object.freeze(['encanto', 'engano', 'conocimiento_bajos_fondos', 'negociacion']) }),
      makashi_duelist: Object.freeze({ key: 'makashi_duelist', label: 'Duelista Makashi', careerSkills: Object.freeze(['sabre_de_luz', 'encanto', 'coordinacion', 'engano']) }),
      seer: Object.freeze({ key: 'seer', label: 'Vidente', careerSkills: Object.freeze(['percepcion', 'vigilancia', 'conocimiento_lore', 'supervivencia']) })
    })
  }),
  seeker: Object.freeze({
    key: 'seeker',
    label: 'Buscador',
    careerSkills: Object.freeze(['coordinacion', 'percepcion', 'pilotar_espacio', 'supervivencia', 'vigilancia', 'armas_distancia_ligeras']),
    specializations: Object.freeze({
      ataru_striker: Object.freeze({ key: 'ataru_striker', label: 'Combatente Ataru', careerSkills: Object.freeze(['sabre_de_luz', 'atletismo', 'coordinacion', 'percepcion']) }),
      hunter: Object.freeze({ key: 'hunter', label: 'Caçador', careerSkills: Object.freeze(['sigilo', 'percepcion', 'supervivencia', 'armas_distancia_pesadas']) }),
      pathfinder: Object.freeze({ key: 'pathfinder', label: 'Batedor de Rotas', careerSkills: Object.freeze(['atletismo', 'astronavegacion', 'supervivencia', 'resistencia']) })
    })
  }),
  sentinel: Object.freeze({
    key: 'sentinel',
    label: 'Sentinela',
    careerSkills: Object.freeze(['computadoras', 'sigilo', 'engano', 'percepcion', 'callejero', 'mecanica']),
    specializations: Object.freeze({
      artisan: Object.freeze({ key: 'artisan', label: 'Artesão', careerSkills: Object.freeze(['mecanica', 'computadoras', 'conocimiento_educacion', 'disciplina']) }),
      shadow: Object.freeze({ key: 'shadow', label: 'Sombra', careerSkills: Object.freeze(['sigilo', 'callejero', 'engano', 'conocimiento_bajos_fondos']) }),
      shien_expert: Object.freeze({ key: 'shien_expert', label: 'Especialista Shien', careerSkills: Object.freeze(['sabre_de_luz', 'atletismo', 'disciplina', 'resistencia']) })
    })
  }),
  warrior: Object.freeze({
    key: 'warrior',
    label: 'Guerreiro',
    careerSkills: Object.freeze(['atletismo', 'pelea', 'resistencia', 'coaccion', 'supervivencia', 'armas_cuerpo_a_cuerpo']),
    specializations: Object.freeze({
      aggressor: Object.freeze({ key: 'aggressor', label: 'Agressor', careerSkills: Object.freeze(['coaccion', 'pelea', 'conocimiento_bajos_fondos', 'resistencia']) }),
      colossus: Object.freeze({ key: 'colossus', label: 'Colosso', careerSkills: Object.freeze(['atletismo', 'resistencia', 'supervivencia', 'disciplina']) }),
      juyo_berserker: Object.freeze({ key: 'juyo_berserker', label: 'Berserker Juyo', careerSkills: Object.freeze(['sabre_de_luz', 'atletismo', 'pelea', 'coaccion']) })
    })
  })
});

export function createFndCharacter({
  id = 'fnd-hero',
  name = 'Cavaleiro da Força',
  careerKey = 'guardian',
  specializationKey = 'peacekeeper',
  moralityScore = 50,
  strength = 'Bravura',
  weakness = 'Ira',
  startingXp = 110,
  characteristics = { fortaleza: 2, agilidad: 2, intelecto: 2, astucia: 2, voluntad: 2, presencia: 2 }
} = {}) {
  const career = FND_CAREERS[careerKey];
  if (!career) throw new Error(`Carreira F&D inválida: ${careerKey}`);
  const spec = career.specializations[specializationKey];
  if (!spec) throw new Error(`Especialização F&D inválida: ${specializationKey}`);

  const combinedCareerSkills = Array.from(new Set([...career.careerSkills, ...spec.careerSkills]));

  const baseChar = {
    id,
    name,
    creation: {
      rulesProfile: FND_CORE,
      careerKey,
      specializationKey
    },
    characteristics: { ...characteristics },
    skills: Object.fromEntries(Object.keys(SKILLS).map(k => [k, 0])),
    schemaVersion: 1,
    specializations: [spec.label],
    careerSkills: combinedCareerSkills,
    derived: {
      wounds: { current: 0, threshold: 10 + characteristics.fortaleza },
      strain: { current: 0, threshold: 10 + characteristics.voluntad },
      soak: characteristics.fortaleza,
      defense: { melee: 0, ranged: 0 }
    },
    force: {
      rating: 1, // All Force & Destiny careers start with Force Rating 1
      committed: 0,
      darkSideUser: moralityScore <= 30,
      ongoing: [],
      powers: {}
    },
    morality: normalizeMoralityState({
      morality: {
        score: moralityScore,
        conflict: 0,
        strength,
        weakness,
        triggeredThisSession: false,
        history: []
      }
    }),
    credits: 500,
    motivations: [],
    obligations: [],
    weapons: [],
    armor: [],
    gear: [],
    criticalInjuries: [],
    statusEffects: [],
    talents: [],
    customSkills: [],
    xp: {
      total: startingXp,
      available: startingXp
    },
    talentPurchases: [],
    specializationPurchases: []
  };

  return baseChar;
}
