applyAdditionalObligation = applyAdditionalObligation;
totalEoteCharacteristicUpgradeCost = totalEoteCharacteristicUpgradeCost;
createEoteCoreCharacter = createEoteCoreCharacter;
createRandomEoteCoreCharacter = createRandomEoteCoreCharacter;
import * as character_mjs_1 from "./character.mjs";
export const EOTE_CORE = 'EOTE_CORE';
export const SOLO_STARTING_OBLIGATION_LOCAL_POLICY = 20;
const K = Object.freeze({
    ASTRO: 'astronavegacion', ATH: 'atletismo', CHARM: 'carisma', COER: 'coaccion', COMP: 'computadoras', COOL: 'frialdad', COORD: 'coordinacion',
    DECEP: 'engano', DISC: 'disciplina', GUN: 'artilleria', LEAD: 'liderazgo', MECH: 'mecanica', MED: 'medicina', NEG: 'negociacion', PERC: 'percepcion',
    PILOT_P: 'pilotar_vehiculo_planetario', PILOT_S: 'pilotar_vehiculo_espacial', RES: 'aguante', SKUL: 'actividad_criminal', STEALTH: 'sigilo',
    STREET: 'callejeo', SURV: 'supervivencia', VIG: 'alerta', BRAWL: 'pelea', MELEE: 'armas_cuerpo_a_cuerpo', RH: 'armas_distancia_pesadas',
    RL: 'armas_distancia_ligeras', EDU: 'educacion', LORE: 'saber_arcaico', OUTER: 'borde_exterior', UNDER: 'bajos_fondos', CORE: 'mundos_del_nucleo', XENO: 'xenologia'
});
export const EOTE_SPECIES = Object.freeze({
    bothan: { label: 'Bothan', characteristics: { fortaleza: 1, agilidad: 2, intelecto: 2, astucia: 3, voluntad: 2, presencia: 2 }, woundBase: 10, strainBase: 11, xp: 100, freeSkill: { skill: K.STREET }, freeTalents: ['Atitude convincente'], abilities: ['Começa com 1 grau em Vida nas ruas.', 'Começa com 1 grau do talento Atitude convincente.'] },
    droid: { label: 'Droide', characteristics: { fortaleza: 1, agilidad: 1, intelecto: 1, astucia: 1, voluntad: 1, presencia: 1 }, woundBase: 10, strainBase: 10, xp: 175, careerFreeCount: 6, specFreeCount: 3, soakBonus: 1, freeTalents: ['Resistente'], abilities: ['Não precisa comer, dormir ou respirar e é imune a toxinas e venenos.', 'Ser inorgânico: tratamento usa Mecânica em vez de Medicina.', 'Não pode se tornar sensível à Força.'] },
    gand: { label: 'Gand', characteristics: { fortaleza: 2, agilidad: 2, intelecto: 2, astucia: 2, voluntad: 3, presencia: 1 }, woundBase: 10, strainBase: 10, xp: 100, freeSkill: { skill: K.DISC }, speciesChoice: { type: 'gandRespiration', variants: ['lungs', 'lungless'] }, abilities: ['Começa com 1 grau em Disciplina.', 'Gand com pulmões recebe +10 PE iniciais, precisa de respirador de amônia e trata oxigênio como atmosfera perigosa.', 'Gand sem pulmões não precisa respirar e é imune a sufocamento, mas não aos efeitos do vácuo.'] },
    human: { label: 'Humano', characteristics: { fortaleza: 2, agilidad: 2, intelecto: 2, astucia: 2, voluntad: 2, presencia: 2 }, woundBase: 10, strainBase: 10, xp: 110, speciesChoice: { type: 'twoNonCareer' }, abilities: ['Começa com 1 grau em duas perícias não profissionais diferentes.'] },
    rodian: { label: 'Rodiano', characteristics: { fortaleza: 2, agilidad: 3, intelecto: 2, astucia: 2, voluntad: 1, presencia: 2 }, woundBase: 10, strainBase: 10, xp: 100, freeSkill: { skill: K.SURV }, freeTalents: ['Rastreador experiente'], abilities: ['Começa com 1 grau em Sobrevivência.', 'Começa com 1 grau do talento Rastreador experiente.'] },
    trandoshan: { label: 'Trandoshano', characteristics: { fortaleza: 3, agilidad: 1, intelecto: 2, astucia: 2, voluntad: 2, presencia: 2 }, woundBase: 12, strainBase: 9, xp: 90, freeSkill: { skill: K.PERC }, abilities: ['Começa com 1 grau em Percepção.', 'Regeneração: recupera 1 Ferimento adicional quando recupera Ferimentos por descanso natural.', 'Garras: ataques de Briga causam +1 dano e têm Crítico 3.'] },
    twilek: { label: "Twi'lek", characteristics: { fortaleza: 1, agilidad: 2, intelecto: 2, astucia: 2, voluntad: 2, presencia: 3 }, woundBase: 10, strainBase: 11, xp: 100, speciesChoice: { type: 'oneSkill', skills: [K.CHARM, K.DECEP] }, abilities: ['Escolhe 1 grau em Carisma ou Enganação.', 'Remove 1 dado de Contratempo imposto por condições ambientais áridas ou quentes.'] },
    wookiee: { label: 'Wookiee', characteristics: { fortaleza: 3, agilidad: 2, intelecto: 2, astucia: 2, voluntad: 1, presencia: 2 }, woundBase: 14, strainBase: 8, xp: 90, freeSkill: { skill: K.BRAWL }, abilities: ['Começa com 1 grau em Briga.', 'Fúria Wookiee: +1 dano em Briga/Corpo a corpo quando ferido; +2 se estiver com ferimento crítico.'] }
});
export const EOTE_CAREERS = Object.freeze({
    bounty_hunter: { label: 'Caçador de Recompensas', skills: [K.ATH, K.BRAWL, K.PERC, K.PILOT_P, K.PILOT_S, K.RH, K.STREET, K.VIG], specializations: {
            assassin: { label: 'Assassino', skills: [K.MELEE, K.RH, K.SKUL, K.STEALTH] },
            gadgeteer: { label: 'Especialista em Gadgets', skills: [K.BRAWL, K.COER, K.MECH, K.RL] },
            survivalist: { label: 'Sobrevivencialista', skills: [K.XENO, K.PERC, K.RES, K.SURV] }
        } },
    colonist: { label: 'Colono', skills: [K.CHARM, K.DECEP, K.CORE, K.EDU, K.LORE, K.LEAD, K.NEG, K.STREET], specializations: {
            doctor: { label: 'Médico', skills: [K.COOL, K.EDU, K.MED, K.RES] },
            politico: { label: 'Político', skills: [K.CHARM, K.COER, K.DECEP, K.CORE] },
            scholar: { label: 'Acadêmico', skills: [K.OUTER, K.UNDER, K.XENO, K.PERC] }
        } },
    explorer: { label: 'Explorador', skills: [K.ASTRO, K.COOL, K.PILOT_S, K.SURV, K.PERC, K.LORE, K.OUTER, K.XENO], specializations: {
            fringer: { label: 'Fronteiriço', skills: [K.ASTRO, K.COORD, K.NEG, K.STREET] },
            scout: { label: 'Batedor', skills: [K.ATH, K.MED, K.PILOT_P, K.SURV] },
            trader: { label: 'Comerciante', skills: [K.DECEP, K.CORE, K.UNDER, K.NEG] }
        } },
    hired_gun: { label: 'Mercenário', skills: [K.ATH, K.BRAWL, K.DISC, K.MELEE, K.PILOT_P, K.RL, K.RES, K.VIG], specializations: {
            bodyguard: { label: 'Guarda-costas', skills: [K.GUN, K.PERC, K.PILOT_P, K.RH] },
            marauder: { label: 'Saqueador', skills: [K.ATH, K.BRAWL, K.MELEE, K.RES] },
            mercenary_soldier: { label: 'Soldado Mercenário', skills: [K.DISC, K.GUN, K.LEAD, K.RH] }
        } },
    smuggler: { label: 'Contrabandista', skills: [K.COORD, K.DECEP, K.UNDER, K.PERC, K.PILOT_S, K.SKUL, K.STREET, K.VIG], specializations: {
            pilot: { label: 'Piloto', skills: [K.ASTRO, K.GUN, K.PILOT_P, K.PILOT_S] },
            scoundrel: { label: 'Patife', skills: [K.CHARM, K.COOL, K.DECEP, K.RL] },
            thief: { label: 'Ladrão', skills: [K.COMP, K.SKUL, K.STEALTH, K.VIG] }
        } },
    technician: { label: 'Técnico', skills: [K.ASTRO, K.COMP, K.COORD, K.DISC, K.OUTER, K.MECH, K.PERC, K.PILOT_P], specializations: {
            mechanic: { label: 'Mecânico', skills: [K.BRAWL, K.MECH, K.PILOT_S, K.SKUL] },
            outlaw_tech: { label: 'Técnico Fora da Lei', skills: [K.EDU, K.UNDER, K.MECH, K.STREET] },
            slicer: { label: 'Slicer', skills: [K.COMP, K.EDU, K.UNDER, K.STEALTH] }
        } }
});
export const OBLIGATION_TYPES_PT_BR = Object.freeze([
    'Vício', 'Traição', 'Chantagem', 'Recompensa', 'Criminal', 'Dívida', 'Dever assumido', 'Família', 'Favor', 'Juramento', 'Obsessão', 'Responsabilidade'
]);
function uniq(values) { return [...new Set((values ?? []).filter(Boolean))]; }
function int(value, label, { min = 0, max = 999 } = {}) { const n = Number(value); if (!Number.isInteger(n) || n < min || n > max)
    throw new Error(`${label} inválido.`); return n; }
function requireSelectedSkills(selected, allowed, count, label) { const unique = uniq(selected); if (unique.length !== count)
    throw new Error(`${label}: escolha exatamente ${count} perícias.`); if (unique.some(k => !allowed.includes(k)))
    throw new Error(`${label}: há uma perícia fora da lista permitida.`); return unique; }
function addFreeRank(character, skill) { if (!(skill in character.skills))
    throw new Error(`Perícia não reconhecida: ${skill}.`); if (character.skills[skill] >= 2)
    throw new Error('Nenhuma perícia pode ultrapassar grau 2 durante a criação.'); character.skills[skill] += 1; }
export function startingObligationForPartySize(partySize, { soloPolicy = SOLO_STARTING_OBLIGATION_LOCAL_POLICY } = {}) {
    const n = int(partySize, 'Quantidade de jogadores', { min: 1, max: 99 });
    if (n === 1)
        return { value: int(soloPolicy, 'Obligation solo', { min: 0, max: 100 }), official: false, note: 'Política local do VTT: a tabela oficial de Obligation começa em 2 personagens.' };
    if (n === 2)
        return { value: 20, official: true };
    if (n === 3)
        return { value: 15, official: true };
    if (n === 4 || n === 5)
        return { value: 10, official: true };
    return { value: 5, official: true };
}
export function applyAdditionalObligation(startingObligation, { xp5 = false, xp10 = false, credits1000 = false, credits2500 = false } = {}) {
    const start = int(startingObligation, 'Obligation inicial', { min: 0, max: 100 });
    if (xp5 && xp10)
        throw new Error('Escolha no máximo um benefício de PE por Obligation adicional.');
    if (credits1000 && credits2500)
        throw new Error('Escolha no máximo um benefício de créditos por Obligation adicional.');
    const added = (xp5 ? 5 : 0) + (xp10 ? 10 : 0) + (credits1000 ? 5 : 0) + (credits2500 ? 10 : 0);
    if (added > start)
        throw new Error('A Obligation adicional não pode exceder a Obligation inicial do personagem.');
    return { obligation: start + added, additionalObligation: added, xpBonus: (xp5 ? 5 : 0) + (xp10 ? 10 : 0), creditBonus: (credits1000 ? 1000 : 0) + (credits2500 ? 2500 : 0) };
}
export function characteristicUpgradeCost(base, target) { const a = int(base, 'Característica base', { min: 1, max: 5 }); const b = int(target, 'Característica final', { min: a, max: 5 }); let cost = 0; for (let rating = a + 1; rating <= b; rating++)
    cost += rating * 10; return cost; }
export function totalEoteCharacteristicUpgradeCost(speciesKey, targets = {}) { const species = EOTE_SPECIES[speciesKey]; if (!species)
    throw new Error('Espécie inválida.'); return Object.entries(species.characteristics).reduce((sum, [key, base]) => sum + characteristicUpgradeCost(base, targets[key] ?? base), 0); }
export function validateEoteSpeciesChoice({ speciesKey, choiceSkills = [], careerSkills = [], speciesVariant = null } = {}) {
    const species = EOTE_SPECIES[speciesKey];
    if (!species)
        throw new Error('Espécie inválida.');
    if (!species.speciesChoice)
        return { skills: [], variant: null, xpBonus: 0 };
    if (species.speciesChoice.type === 'oneSkill')
        return { skills: requireSelectedSkills(choiceSkills, species.speciesChoice.skills, 1, 'Escolha da espécie'), variant: null, xpBonus: 0 };
    if (species.speciesChoice.type === 'twoNonCareer') {
        const choices = uniq(choiceSkills);
        if (choices.length !== 2)
            throw new Error('Humano: escolha exatamente 2 perícias não profissionais diferentes.');
        if (choices.some(skill => !(skill in character_mjs_1.SKILLS)))
            throw new Error('Humano: perícia inválida.');
        if (choices.some(skill => careerSkills.includes(skill)))
            throw new Error('Humano: as duas perícias escolhidas devem ser não profissionais.');
        return { skills: choices, variant: null, xpBonus: 0 };
    }
    if (species.speciesChoice.type === 'gandRespiration') {
        const variant = String(speciesVariant ?? '').trim();
        if (!species.speciesChoice.variants.includes(variant))
            throw new Error('Gand: escolha se o personagem possui pulmões ou não possui pulmões.');
        return { skills: [], variant, xpBonus: variant === 'lungs' ? 10 : 0 };
    }
    return { skills: [], variant: null, xpBonus: 0 };
}
export function createEoteCoreCharacter(options = {}) {
    const name = String(options.name ?? '').trim();
    if (!name)
        throw new Error('Nome do personagem é obrigatório.');
    const species = EOTE_SPECIES[options.speciesKey];
    if (!species)
        throw new Error('Escolha uma espécie válida de Edge of the Empire.');
    const career = EOTE_CAREERS[options.careerKey];
    if (!career)
        throw new Error('Escolha uma carreira válida de Edge of the Empire.');
    const specialization = career.specializations?.[options.specializationKey];
    if (!specialization)
        throw new Error('Escolha uma especialização da carreira selecionada.');
    const careerFreeCount = species.careerFreeCount ?? 4;
    const specFreeCount = species.specFreeCount ?? 2;
    const careerFree = requireSelectedSkills(options.careerFreeSkills, career.skills, careerFreeCount, 'Perícias gratuitas da carreira');
    const specFree = requireSelectedSkills(options.specializationFreeSkills, specialization.skills, specFreeCount, 'Perícias gratuitas da especialização');
    const allCareer = uniq([...career.skills, ...specialization.skills]);
    const speciesChoice = validateEoteSpeciesChoice({ speciesKey: options.speciesKey, choiceSkills: options.speciesChoiceSkills, careerSkills: allCareer, speciesVariant: options.speciesVariant });
    const party = startingObligationForPartySize(options.partySize ?? 1, { soloPolicy: options.soloStartingObligation ?? SOLO_STARTING_OBLIGATION_LOCAL_POLICY });
    const obligationStart = options.startingObligation === undefined ? party.value : int(options.startingObligation, 'Obligation inicial', { min: 0, max: 100 });
    const obligation = applyAdditionalObligation(obligationStart, options.obligationBenefits ?? {});
    const xpBudget = species.xp + speciesChoice.xpBonus + obligation.xpBonus;
    const targets = { ...species.characteristics, ...(options.characteristics ?? {}) };
    const characteristicCost = totalEoteCharacteristicUpgradeCost(options.speciesKey, targets);
    if (characteristicCost > xpBudget)
        throw new Error(`PE insuficientes para as Características: custo ${characteristicCost}, orçamento ${xpBudget}.`);
    const motivation = String(options.motivation ?? '').trim();
    if (!motivation)
        throw new Error('Motivação é obrigatória na criação.');
    const obligationType = String(options.obligationType ?? OBLIGATION_TYPES_PT_BR[0]).trim();
    if (!OBLIGATION_TYPES_PT_BR.includes(obligationType))
        throw new Error('Tipo de Obligation inválido para o perfil EotE Core.');
    const c = character_mjs_1.createBlankCharacter(name);
    c.source = { type: 'custom_eote_core', title: 'Star Wars: Edge of the Empire — Core Rulebook', language: 'en', verifiedSections: ['Character Creation', 'Species', 'Careers and Specializations', 'Obligation', 'Experience', 'Derived Attributes'], notes: party.official ? null : party.note };
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
    for (const skill of speciesChoice.skills)
        addFreeRank(c, skill);
    c.talents = [...(species.freeTalents ?? [])];
    c.derived.soak = c.characteristics.fortaleza + (species.soakBonus ?? 0);
    c.derived.wounds.threshold = species.woundBase + c.characteristics.fortaleza;
    c.derived.strain.threshold = species.strainBase + c.characteristics.voluntad;
    c.xp = { total: xpBudget, available: xpBudget - characteristicCost };
    c.credits = 500 + obligation.creditBonus;
    c.obligations = [{ type: obligationType, value: obligation.obligation, startingValue: obligationStart, officialStartingValue: party.official, additionalObligation: obligation.additionalObligation }];
    c.motivations = [{ text: motivation }];
    c.description = { background: String(options.background ?? '').trim(), speciesAbilities: [...(species.abilities ?? [])], speciesVariant: speciesChoice.variant };
    c.creation = { rulesProfile: EOTE_CORE, speciesKey: options.speciesKey, speciesVariant: speciesChoice.variant, careerKey: options.careerKey, specializationKey: options.specializationKey, careerFreeSkills: careerFree, specializationFreeSkills: specFree, speciesChoiceSkills: speciesChoice.skills, startingXp: species.xp, speciesXpBonus: speciesChoice.xpBonus, obligationXpBonus: obligation.xpBonus, characteristicXpSpent: characteristicCost, startingCredits: c.credits, partySize: Number(options.partySize ?? 1), startingObligation: obligationStart, additionalObligation: obligation.additionalObligation, soloObligationPolicy: party.official ? null : party.value };
    return c;
}
export function eoteCreationPreview(options = {}) {
    const species = EOTE_SPECIES[options.speciesKey];
    if (!species)
        return null;
    const career = EOTE_CAREERS[options.careerKey] ?? null;
    const specialization = career?.specializations?.[options.specializationKey] ?? null;
    const allCareer = career ? uniq([...career.skills, ...(specialization?.skills ?? [])]) : [];
    let speciesChoice = { skills: [], variant: null, xpBonus: 0 };
    if (species.speciesChoice?.type === 'gandRespiration' && options.speciesVariant)
        speciesChoice = validateEoteSpeciesChoice({ speciesKey: options.speciesKey, careerSkills: allCareer, speciesVariant: options.speciesVariant });
    const party = startingObligationForPartySize(options.partySize ?? 1, { soloPolicy: options.soloStartingObligation ?? SOLO_STARTING_OBLIGATION_LOCAL_POLICY });
    const obligation = applyAdditionalObligation(options.startingObligation === undefined ? party.value : Number(options.startingObligation), options.obligationBenefits ?? {});
    const characteristicCost = totalEoteCharacteristicUpgradeCost(options.speciesKey, { ...species.characteristics, ...(options.characteristics ?? {}) });
    const xpBudget = species.xp + speciesChoice.xpBonus + obligation.xpBonus;
    return { xpBudget, characteristicCost, xpRemaining: xpBudget - characteristicCost, credits: 500 + obligation.creditBonus, obligation: obligation.obligation, startingObligation: options.startingObligation === undefined ? party.value : Number(options.startingObligation), additionalObligation: obligation.additionalObligation, obligationOfficial: party.official, obligationNote: party.note ?? null, speciesXpBonus: speciesChoice.xpBonus };
}
function randomUnit(rng) { const value = Number(rng()); if (!Number.isFinite(value) || value < 0 || value >= 1)
    throw new Error('O gerador aleatório deve retornar valor entre 0 (inclusive) e 1 (exclusive).'); return value; }
function randomPick(values, rng) { if (!Array.isArray(values) || !values.length)
    throw new Error('Catálogo aleatório vazio.'); return values[Math.floor(randomUnit(rng) * values.length)]; }
function randomSample(values, count, rng) { const pool = uniq(values); const n = int(count, 'Quantidade aleatória', { min: 0, max: pool.length }); const result = []; while (result.length < n) {
    result.push(pool.splice(Math.floor(randomUnit(rng) * pool.length), 1)[0]);
} return result; }
export function createRandomEoteCoreCharacter(options = {}) {
    const name = String(options.name ?? '').trim();
    if (!name)
        throw new Error('Nome do personagem é obrigatório; o gerador não inventa nomes.');
    const motivation = String(options.motivation ?? '').trim();
    if (!motivation)
        throw new Error('Motivação é obrigatória; o gerador não inventa motivações.');
    const rng = options.rng ?? Math.random;
    const speciesKey = randomPick(Object.keys(EOTE_SPECIES), rng);
    const careerKey = randomPick(Object.keys(EOTE_CAREERS), rng);
    const species = EOTE_SPECIES[speciesKey];
    const career = EOTE_CAREERS[careerKey];
    const specializationKey = randomPick(Object.keys(career.specializations), rng);
    const specialization = career.specializations[specializationKey];
    const allCareer = uniq([...career.skills, ...specialization.skills]);
    let speciesChoiceSkills = [];
    let speciesVariant = null;
    const protectedSpeciesRanks = [];
    if (species.freeSkill?.skill)
        protectedSpeciesRanks.push(species.freeSkill.skill);
    if (species.speciesChoice?.type === 'oneSkill') {
        const skill = randomPick(species.speciesChoice.skills, rng);
        speciesChoiceSkills = [skill];
        protectedSpeciesRanks.push(skill);
    }
    else if (species.speciesChoice?.type === 'twoNonCareer')
        speciesChoiceSkills = randomSample(Object.keys(character_mjs_1.SKILLS).filter(skill => !allCareer.includes(skill)), 2, rng);
    else if (species.speciesChoice?.type === 'gandRespiration')
        speciesVariant = randomPick(species.speciesChoice.variants, rng);
    const careerFreeSkills = randomSample(career.skills, species.careerFreeCount ?? 4, rng);
    const specPool = specialization.skills.filter(skill => !(protectedSpeciesRanks.includes(skill) && careerFreeSkills.includes(skill)));
    const specializationFreeSkills = randomSample(specPool, species.specFreeCount ?? 2, rng);
    const character = createEoteCoreCharacter({ name, motivation, background: String(options.background ?? '').trim(), partySize: options.partySize ?? 1, soloStartingObligation: options.soloStartingObligation, speciesKey, speciesVariant, careerKey, specializationKey, careerFreeSkills, specializationFreeSkills, speciesChoiceSkills, characteristics: { ...species.characteristics }, obligationType: randomPick(OBLIGATION_TYPES_PT_BR, rng), obligationBenefits: {} });
    character.creation = { ...character.creation, randomizedMechanics: true };
    return character;
}
