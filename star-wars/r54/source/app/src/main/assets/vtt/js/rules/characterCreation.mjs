import * as character_mjs_1 from './character.mjs';
export const SOLO_STARTING_DUTY_LOCAL_POLICY = 20;
const K = Object.freeze({
    ASTRO: 'astronavegacion', ATH: 'atletismo', CHARM: 'carisma', COER: 'coaccion', COMP: 'computadoras', COOL: 'frialdad', COORD: 'coordinacion',
    DECEP: 'engano', DISC: 'disciplina', GUN: 'artilleria', LEAD: 'liderazgo', MECH: 'mecanica', MED: 'medicina', NEG: 'negociacion', PERC: 'percepcion',
    PILOT_P: 'pilotar_vehiculo_planetario', PILOT_S: 'pilotar_vehiculo_espacial', RES: 'aguante', SKUL: 'actividad_criminal', STEALTH: 'sigilo',
    STREET: 'callejeo', SURV: 'supervivencia', VIG: 'alerta', BRAWL: 'pelea', MELEE: 'armas_cuerpo_a_cuerpo', RH: 'armas_distancia_pesadas',
    RL: 'armas_distancia_ligeras', EDU: 'educacion', LORE: 'saber_arcaico', OUTER: 'borde_exterior', UNDER: 'bajos_fondos', WAR: 'estrategia_militar',
    CORE: 'mundos_del_nucleo', XENO: 'xenologia'
});
export const AOR_SPECIES = Object.freeze({
    bothan: { label: 'Bothan', characteristics: { fortaleza: 1, agilidad: 2, intelecto: 2, astucia: 3, voluntad: 2, presencia: 2 }, woundBase: 10, strainBase: 11, xp: 100, freeSkill: { skill: K.STREET }, freeTalents: ['Comportamento convincente'], abilities: ['Bothans começam com 1 grau em Vida nas ruas.'] },
    droid: { label: 'Droide', characteristics: { fortaleza: 1, agilidad: 1, intelecto: 1, astucia: 1, voluntad: 1, presencia: 1 }, woundBase: 10, strainBase: 10, xp: 175, careerFreeCount: 6, specFreeCount: 3, freeTalents: ['Resistente'], soakBonus: 1, abilities: ['Não precisa comer, dormir ou respirar; é imune a toxinas e venenos.', 'Ser inorgânico: tratamento usa Mecânica em vez de Medicina.', 'Ser mecânico: não pode se tornar sensível à Força.'] },
    duros: { label: 'Duros', characteristics: { fortaleza: 1, agilidad: 2, intelecto: 3, astucia: 2, voluntad: 2, presencia: 2 }, woundBase: 11, strainBase: 10, xp: 100, freeSkill: { skill: K.PILOT_S }, abilities: ['Navegação intuitiva: adiciona 1 Vantagem a testes de Astronavegação.'] },
    gran: { label: 'Gran', characteristics: { fortaleza: 2, agilidad: 2, intelecto: 2, astucia: 1, voluntad: 2, presencia: 3 }, woundBase: 10, strainBase: 9, xp: 100, speciesChoice: { type: 'oneSkill', skills: [K.CHARM, K.NEG] }, abilities: ['Visão aprimorada: remove até 2 dados de Contratempo por ambiente/ocultação em ataques à distância ou Percepção.'] },
    human: { label: 'Humano', characteristics: { fortaleza: 2, agilidad: 2, intelecto: 2, astucia: 2, voluntad: 2, presencia: 2 }, woundBase: 10, strainBase: 10, xp: 110, speciesChoice: { type: 'twoNonCareer' }, abilities: ['Começa com 1 grau em duas perícias não profissionais diferentes.'] },
    ithorian: { label: 'Ithorian', characteristics: { fortaleza: 2, agilidad: 1, intelecto: 2, astucia: 2, voluntad: 3, presencia: 2 }, woundBase: 9, strainBase: 12, xp: 90, freeSkill: { skill: K.SURV }, abilities: ['Berro Ithoriano: arma natural; seu uso causa 3 de Tensão.'] },
    mon_calamari: { label: 'Mon Calamari', characteristics: { fortaleza: 2, agilidad: 2, intelecto: 3, astucia: 1, voluntad: 2, presencia: 2 }, woundBase: 10, strainBase: 10, xp: 100, freeSkill: { skill: K.EDU }, abilities: ['Anfíbio: respira debaixo d’água e não sofre penalidade de movimento na água.'] },
    sullustan: { label: 'Sullustano', characteristics: { fortaleza: 2, agilidad: 3, intelecto: 2, astucia: 1, voluntad: 2, presencia: 2 }, woundBase: 10, strainBase: 10, xp: 100, freeSkill: { skill: K.ASTRO }, freeTalents: ['Piloto habilidoso'], abilities: ['Começa com 1 grau em Astronavegação e 1 grau em Piloto habilidoso.'] }
});
export const AOR_CAREERS = Object.freeze({
    ace: { label: 'Ás', skills: [K.ASTRO, K.COOL, K.GUN, K.MECH, K.PERC, K.PILOT_P, K.PILOT_S, K.RL], specializations: {
            driver: { label: 'Condutor', skills: [K.COOL, K.GUN, K.MECH, K.PILOT_P] }, gunner: { label: 'Artilheiro', skills: [K.DISC, K.GUN, K.RH, K.RES] }, pilot: { label: 'Piloto', skills: [K.ASTRO, K.GUN, K.PILOT_P, K.PILOT_S] }
        } },
    commander: { label: 'Comandante', skills: [K.COER, K.COOL, K.DISC, K.WAR, K.LEAD, K.PERC, K.RL, K.VIG], specializations: {
            commodore: { label: 'Comodoro', skills: [K.ASTRO, K.COMP, K.EDU, K.OUTER] }, squadron_leader: { label: 'Líder de esquadrão', skills: [K.GUN, K.MECH, K.PILOT_P, K.PILOT_S] }, tactician: { label: 'Tático', skills: [K.BRAWL, K.DISC, K.LEAD, K.RH] }
        } },
    diplomat: { label: 'Diplomata', skills: [K.CHARM, K.DECEP, K.CORE, K.LORE, K.OUTER, K.XENO, K.LEAD, K.NEG], specializations: {
            ambassador: { label: 'Embaixador', skills: [K.CHARM, K.DISC, K.CORE, K.NEG] }, agitator: { label: 'Agitador', skills: [K.COER, K.DECEP, K.UNDER, K.STREET] }, quartermaster: { label: 'Intendente', skills: [K.COMP, K.NEG, K.SKUL, K.VIG] }
        } },
    engineer: { label: 'Engenheiro', skills: [K.ATH, K.COMP, K.EDU, K.MECH, K.PERC, K.PILOT_S, K.RL, K.VIG], specializations: {
            mechanic: { label: 'Mecânico', skills: [K.BRAWL, K.MECH, K.PILOT_S, K.SKUL] }, saboteur: { label: 'Sabotador', skills: [K.COORD, K.MECH, K.SKUL, K.STEALTH] }, scientist: { label: 'Cientista', skills: [K.COMP, K.EDU, K.LORE, K.MED] }
        } },
    soldier: { label: 'Soldado', skills: [K.ATH, K.BRAWL, K.WAR, K.MED, K.MELEE, K.RL, K.RH, K.SURV], specializations: {
            commando: { label: 'Comando', skills: [K.BRAWL, K.MELEE, K.RES, K.SURV] }, medic: { label: 'Médico', skills: [K.XENO, K.MED, K.RES, K.VIG] }, sharpshooter: { label: 'Atirador de elite', skills: [K.COOL, K.PERC, K.RL, K.RH] }
        } },
    spy: { label: 'Espião', skills: [K.COMP, K.COOL, K.COORD, K.DECEP, K.WAR, K.PERC, K.SKUL, K.STEALTH], specializations: {
            infiltrator: { label: 'Infiltrador', skills: [K.DECEP, K.MELEE, K.SKUL, K.STREET] }, scout: { label: 'Batedor', skills: [K.ATH, K.MED, K.PILOT_P, K.SURV] }, slicer: { label: 'Slicer', skills: [K.COMP, K.EDU, K.UNDER, K.STEALTH] }
        } }
});
export const DUTY_TYPES_PT_BR = Object.freeze([
    'Vitória em combate', 'Contrainteligência', 'Inteligência', 'Segurança interna', 'Pessoal', 'Apoio político', 'Recrutamento', 'Aquisição de recursos', 'Sabotagem', 'Superioridade espacial', 'Aquisição tecnológica', 'Suporte'
]);
function uniq(values) { return [...new Set((values ?? []).filter(Boolean))]; }
function int(value, label, { min = 0, max = 999 } = {}) { const n = Number(value); if (!Number.isInteger(n) || n < min || n > max)
    throw new Error(`${label} inválido.`); return n; }
export function startingDutyForPartySize(partySize, { soloPolicy = SOLO_STARTING_DUTY_LOCAL_POLICY } = {}) {
    const n = int(partySize, 'Quantidade de jogadores', { min: 1, max: 99 });
    if (n === 1)
        return { value: int(soloPolicy, 'Duty solo', { min: 0, max: 100 }), official: false, note: 'Política local do VTT: a tabela oficial começa em 2 jogadores.' };
    if (n === 2)
        return { value: 20, official: true };
    if (n === 3)
        return { value: 15, official: true };
    if (n === 4 || n === 5)
        return { value: 10, official: true };
    return { value: 5, official: true };
}
export function applyDutyReductions(startingDuty, { xp5 = false, xp10 = false, credits1000 = false, credits2500 = false } = {}) {
    const duty = int(startingDuty, 'Duty inicial', { min: 0, max: 100 });
    const cost = (xp5 ? 5 : 0) + (xp10 ? 10 : 0) + (credits1000 ? 5 : 0) + (credits2500 ? 10 : 0);
    if (cost > duty)
        throw new Error('As reduções escolhidas gastam mais Duty do que o valor inicial.');
    return {
        duty: duty - cost,
        xpBonus: (xp5 ? 5 : 0) + (xp10 ? 10 : 0),
        creditBonus: (credits1000 ? 1000 : 0) + (credits2500 ? 2500 : 0),
        dutySpent: cost
    };
}
export function characteristicUpgradeCost(base, target) {
    const a = int(base, 'Característica base', { min: 1, max: 5 });
    const b = int(target, 'Característica final', { min: a, max: 5 });
    let cost = 0;
    for (let rating = a + 1; rating <= b; rating++)
        cost += rating * 10;
    return cost;
}
export function totalCharacteristicUpgradeCost(speciesKey, targets = {}) {
    const species = AOR_SPECIES[speciesKey];
    if (!species)
        throw new Error('Espécie inválida.');
    return Object.entries(species.characteristics).reduce((sum, [key, base]) => sum + characteristicUpgradeCost(base, targets[key] ?? base), 0);
}
function requireSelectedSkills(selected, allowed, count, label) {
    const unique = uniq(selected);
    if (unique.length !== count)
        throw new Error(`${label}: escolha exatamente ${count} perícias.`);
    if (unique.some(key => !allowed.includes(key)))
        throw new Error(`${label}: há uma perícia fora da lista permitida.`);
    return unique;
}
function addFreeRank(character, skill) {
    if (!(skill in character.skills))
        throw new Error(`Perícia não reconhecida: ${skill}.`);
    if (character.skills[skill] >= 2)
        throw new Error('Nenhuma perícia pode ultrapassar grau 2 durante a criação.');
    character.skills[skill] += 1;
}
export function validateSpeciesChoice({ speciesKey, choiceSkills = [], careerSkills = [] }) {
    const species = AOR_SPECIES[speciesKey];
    if (!species)
        throw new Error('Espécie inválida.');
    if (!species.speciesChoice)
        return [];
    if (species.speciesChoice.type === 'oneSkill')
        return requireSelectedSkills(choiceSkills, species.speciesChoice.skills, 1, 'Escolha da espécie');
    if (species.speciesChoice.type === 'twoNonCareer') {
        const choices = uniq(choiceSkills);
        if (choices.length !== 2)
            throw new Error('Humano: escolha exatamente 2 perícias não profissionais diferentes.');
        if (choices.some(skill => !(skill in character_mjs_1.SKILLS)))
            throw new Error('Humano: perícia inválida.');
        if (choices.some(skill => careerSkills.includes(skill)))
            throw new Error('Humano: as duas perícias escolhidas devem ser não profissionais.');
        return choices;
    }
    return [];
}
export function createAorCoreCharacter(options = {}) {
    const name = String(options.name ?? '').trim();
    if (!name)
        throw new Error('Nome do personagem é obrigatório.');
    const species = AOR_SPECIES[options.speciesKey];
    if (!species)
        throw new Error('Escolha uma espécie válida.');
    const career = AOR_CAREERS[options.careerKey];
    if (!career)
        throw new Error('Escolha uma carreira válida.');
    const specialization = career.specializations?.[options.specializationKey];
    if (!specialization)
        throw new Error('Escolha uma especialização da carreira selecionada.');
    const careerFreeCount = species.careerFreeCount ?? 4;
    const specFreeCount = species.specFreeCount ?? 2;
    const careerFree = requireSelectedSkills(options.careerFreeSkills, career.skills, careerFreeCount, 'Perícias gratuitas da carreira');
    const specFree = requireSelectedSkills(options.specializationFreeSkills, specialization.skills, specFreeCount, 'Perícias gratuitas da especialização');
    const allCareer = uniq([...career.skills, ...specialization.skills]);
    const speciesChoice = validateSpeciesChoice({ speciesKey: options.speciesKey, choiceSkills: options.speciesChoiceSkills, careerSkills: allCareer });
    const party = startingDutyForPartySize(options.partySize ?? 1, { soloPolicy: options.soloStartingDuty ?? SOLO_STARTING_DUTY_LOCAL_POLICY });
    const dutyStart = options.startingDuty === undefined ? party.value : int(options.startingDuty, 'Duty inicial', { min: 0, max: 100 });
    const duty = applyDutyReductions(dutyStart, options.dutyReductions ?? {});
    const xpBudget = species.xp + duty.xpBonus;
    const targets = { ...species.characteristics, ...(options.characteristics ?? {}) };
    const characteristicCost = totalCharacteristicUpgradeCost(options.speciesKey, targets);
    if (characteristicCost > xpBudget)
        throw new Error(`PE insuficientes para as Características: custo ${characteristicCost}, orçamento ${xpBudget}.`);
    const motivation = String(options.motivation ?? '').trim();
    if (!motivation)
        throw new Error('Motivação é obrigatória na criação.');
    const dutyType = String(options.dutyType ?? DUTY_TYPES_PT_BR[0]).trim();
    if (!DUTY_TYPES_PT_BR.includes(dutyType))
        throw new Error('Tipo de Duty inválido para o perfil AoR Core.');
    const c = (0, character_mjs_1.createBlankCharacter)(name);
    c.source = { type: 'custom_aor_core', title: 'Star Wars - Age of Rebellion - Core Rulebook', language: 'en', verifiedSections: ['Character Creation', 'Species', 'Careers', 'Duty', 'Experience', 'Derived Attributes'], notes: party.official ? null : party.note };
    c.species = species.label;
    c.profession = career.label;
    c.specializations = [specialization.label];
    c.characteristics = Object.fromEntries(Object.entries(species.characteristics).map(([key, base]) => [key, int(targets[key] ?? base, `Característica ${key}`, { min: base, max: 5 })]));
    c.careerSkills = allCareer;
    for (const skill of careerFree)
        addFreeRank(c, skill);
    for (const skill of specFree)
        addFreeRank(c, skill);
    if (species.freeSkill)
        addFreeRank(c, species.freeSkill.skill);
    for (const skill of speciesChoice)
        addFreeRank(c, skill);
    c.talents = [...(species.freeTalents ?? [])];
    c.derived.soak = c.characteristics.fortaleza + (species.soakBonus ?? 0);
    c.derived.wounds.threshold = species.woundBase + c.characteristics.fortaleza;
    c.derived.strain.threshold = species.strainBase + c.characteristics.voluntad;
    c.xp = { total: xpBudget, available: xpBudget - characteristicCost };
    c.credits = 500 + duty.creditBonus;
    c.duties = [{ type: dutyType, value: duty.duty, startingValue: dutyStart, officialStartingValue: party.official, dutySpent: duty.dutySpent }];
    c.motivations = [{ text: motivation }];
    c.description = { background: String(options.background ?? '').trim(), speciesAbilities: [...(species.abilities ?? [])] };
    c.creation = { rulesProfile: 'AOR_CORE', speciesKey: options.speciesKey, careerKey: options.careerKey, specializationKey: options.specializationKey, careerFreeSkills: careerFree, specializationFreeSkills: specFree, speciesChoiceSkills: speciesChoice, startingXp: species.xp, dutyXpBonus: duty.xpBonus, characteristicXpSpent: characteristicCost, startingCredits: 500 + duty.creditBonus, partySize: Number(options.partySize ?? 1), soloDutyPolicy: party.official ? null : party.value };
    return c;
}
export function creationPreview(options = {}) {
    const species = AOR_SPECIES[options.speciesKey];
    if (!species)
        return null;
    const party = startingDutyForPartySize(options.partySize ?? 1, { soloPolicy: options.soloStartingDuty ?? SOLO_STARTING_DUTY_LOCAL_POLICY });
    const duty = applyDutyReductions(options.startingDuty === undefined ? party.value : Number(options.startingDuty), options.dutyReductions ?? {});
    const characteristicCost = totalCharacteristicUpgradeCost(options.speciesKey, { ...species.characteristics, ...(options.characteristics ?? {}) });
    const xpBudget = species.xp + duty.xpBonus;
    return { xpBudget, characteristicCost, xpRemaining: xpBudget - characteristicCost, credits: 500 + duty.creditBonus, duty: duty.duty, dutySpent: duty.dutySpent, dutyOfficial: party.official, dutyNote: party.note ?? null };
}
function randomUnit(rng) {
    if (typeof rng !== 'function')
        throw new Error('Gerador aleatório inválido.');
    const value = Number(rng());
    if (!Number.isFinite(value) || value < 0 || value >= 1)
        throw new Error('O gerador aleatório deve retornar valor entre 0 (inclusive) e 1 (exclusive).');
    return value;
}
function randomPick(values, rng) {
    if (!Array.isArray(values) || values.length === 0)
        throw new Error('Catálogo aleatório vazio.');
    return values[Math.floor(randomUnit(rng) * values.length)];
}
function randomSample(values, count, rng) {
    const pool = uniq(values);
    const n = int(count, 'Quantidade aleatória', { min: 0, max: pool.length });
    const result = [];
    while (result.length < n) {
        const index = Math.floor(randomUnit(rng) * pool.length);
        result.push(pool.splice(index, 1)[0]);
    }
    return result;
}
export function createRandomAorCoreCharacter(options = {}) {
    const name = String(options.name ?? '').trim();
    if (!name)
        throw new Error('Nome do personagem é obrigatório; o gerador não inventa nomes.');
    const motivation = String(options.motivation ?? '').trim();
    if (!motivation)
        throw new Error('Motivação é obrigatória; o gerador não inventa motivações.');
    const rng = options.rng ?? Math.random;
    const speciesKey = randomPick(Object.keys(AOR_SPECIES), rng);
    const careerKey = randomPick(Object.keys(AOR_CAREERS), rng);
    const species = AOR_SPECIES[speciesKey];
    const career = AOR_CAREERS[careerKey];
    const specializationKey = randomPick(Object.keys(career.specializations), rng);
    const specialization = career.specializations[specializationKey];
    const allCareer = uniq([...career.skills, ...specialization.skills]);
    let speciesChoiceSkills = [];
    const protectedSpeciesRanks = [];
    if (species.freeSkill?.skill)
        protectedSpeciesRanks.push(species.freeSkill.skill);
    if (species.speciesChoice?.type === 'oneSkill') {
        const skill = randomPick(species.speciesChoice.skills, rng);
        speciesChoiceSkills = [skill];
        protectedSpeciesRanks.push(skill);
    }
    else if (species.speciesChoice?.type === 'twoNonCareer') {
        const nonCareer = Object.keys(character_mjs_1.SKILLS).filter(skill => !allCareer.includes(skill));
        speciesChoiceSkills = randomSample(nonCareer, 2, rng);
    }
    const careerFreeCount = species.careerFreeCount ?? 4;
    const specFreeCount = species.specFreeCount ?? 2;
    const careerFreeSkills = randomSample(career.skills, careerFreeCount, rng);
    const specPool = specialization.skills.filter(skill => !(protectedSpeciesRanks.includes(skill) && careerFreeSkills.includes(skill)));
    const specializationFreeSkills = randomSample(specPool, specFreeCount, rng);
    const character = createAorCoreCharacter({
        name,
        motivation,
        background: String(options.background ?? '').trim(),
        partySize: options.partySize ?? 1,
        soloStartingDuty: options.soloStartingDuty,
        speciesKey,
        careerKey,
        specializationKey,
        careerFreeSkills,
        specializationFreeSkills,
        speciesChoiceSkills,
        characteristics: { ...species.characteristics },
        dutyType: randomPick(DUTY_TYPES_PT_BR, rng),
        dutyReductions: {},
    });
    character.creation = { ...character.creation, randomizedMechanics: true };
    return character;
}
