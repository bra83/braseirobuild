import * as ids_mjs_1 from '../core/ids.mjs';
const WORLD_POLICY_SOURCE = Object.freeze({
    kind: 'local_policy',
    label: 'Política local do VTT para mundo persistente',
    note: 'Agenda/eventos determinísticos e consequências causais para preservar continuidade sem chamar a IA para cada PNPC a cada turno.'
});
function clone(value) { return structuredClone(value); }
function nowIso() { return new Date().toISOString(); }
function int(value, label, { min = 0, max = 1_000_000_000 } = {}) {
    const n = Number(value);
    if (!Number.isInteger(n) || n < min || n > max)
        throw new Error(`${label} inválido.`);
    return n;
}
function text(value, label, { required = false, max = 500 } = {}) {
    const s = String(value ?? '').trim();
    if (required && !s)
        throw new Error(`${label} é obrigatório.`);
    if (s.length > max)
        throw new Error(`${label} excede ${max} caracteres.`);
    return s;
}
const NPC_SPECIES = Object.freeze(['Humano','Humano','Humano','Humano','Twi\'lek','Bothan','Duros','Rodiano','Mon Calamari','Zabrak','Trandoshano','Wookiee','Gand','Sullustano','Ithoriano','Gran','Nikto','Mirialano','Droide']);
const NPC_CAREERS = Object.freeze({
    'Caçador de Recompensas':['Assassino','Gadgeteer','Sobrevivencialista'],
    'Colono':['Doutor','Político','Acadêmico'],
    'Explorador':['Fronteiriço','Batedor','Comerciante'],
    'Mercenário':['Guarda-costas','Marauder','Especialista em Pesados'],
    'Contrabandista':['Piloto','Canalha','Ladrão'],
    'Técnico':['Mecânico','Slicer','Técnico Fora-da-Lei'],
    'Ás':['Piloto','Artilheiro','Motorista'],
    'Comandante':['Comodoro','Líder de Esquadrão','Tático'],
    'Diplomata':['Agitador','Embaixador','Intendente'],
    'Engenheiro':['Mecânico','Sabotador','Cientista'],
    'Soldado':['Comando','Médico de Combate','Especialista em Pesados'],
    'Espião':['Infiltrador','Slicer','Batedor']
});
const NPC_ORIGINS = Object.freeze(['Mundos do Núcleo','Colônias','Borda Interior','Região de Expansão','Borda Média','Borda Exterior','Espaço Hutt','Setor Corporativo']);
const SPECIES_APPEARANCE = Object.freeze({
    'Humano':'traços humanos',"Twi'lek":'lekku íntegros e pele de coloração própria da espécie','Bothan':'pelagem facial densa e focinho curto','Duros':'pele azul-esverdeada, crânio liso e olhos grandes','Rodiano':'pele verde, olhos multifacetados e focinho curto','Mon Calamari':'pele úmida, olhos grandes e mãos largas','Zabrak':'pequenos chifres cranianos e tatuagens discretas','Trandoshano':'pele escamosa e olhos reptilianos','Wookiee':'pelagem longa e porte muito alto','Gand':'traços quitinosos e aparato respiratório quando necessário','Sullustano':'orelhas grandes, olhos escuros e bochechas marcadas','Ithoriano':'crânio martelo e pescoço alongado','Gran':'três olhos e focinho curto','Nikto':'pele coriácea e traços angulares','Mirialano':'pele esverdeada e tatuagens geométricas','Droide':'chassi humanoide utilitário com peças funcionais'
});
const OCCUPATION_BY_SPECIALIZATION = Object.freeze({
    'Assassino':'caçador de recompensas','Gadgeteer':'especialista em equipamentos','Sobrevivencialista':'rastreador','Doutor':'médico','Político':'negociador','Acadêmico':'pesquisador','Fronteiriço':'guia de fronteira','Batedor':'batedor','Comerciante':'comerciante','Guarda-costas':'segurança','Marauder':'combatente','Especialista em Pesados':'artilheiro','Piloto':'piloto','Canalha':'intermediário do submundo','Ladrão':'infiltrador','Mecânico':'mecânico','Slicer':'slicer','Técnico Fora-da-Lei':'técnico autônomo','Artilheiro':'artilheiro','Motorista':'condutor','Comodoro':'oficial naval','Líder de Esquadrão':'líder militar','Tático':'analista tático','Agitador':'organizador político','Embaixador':'diplomata','Intendente':'logística','Sabotador':'sabotador','Cientista':'cientista','Comando':'soldado de operações','Médico de Combate':'médico de combate','Infiltrador':'agente infiltrado'
});
function npcHash(value){let h=2166136261;for(const c of String(value??'')){h^=c.charCodeAt(0);h=Math.imul(h,16777619)}return h>>>0}
function npcPick(seed,salt,list){const h=npcHash(`${seed}:${salt}`);return list[h%list.length]}
function ageForSpecies(species,seed){if(species==='Droide')return 2+(npcHash(`${seed}:age`)%79);if(species==='Wookiee')return 24+(npcHash(`${seed}:age`)%116);return 18+(npcHash(`${seed}:age`)%55)}
function normalizeNpcProfile(value={}){
    const seed=String(value.id||'')+':'+String(value.name||'PNPC');
    const species=String(value.species||npcPick(seed,'species',NPC_SPECIES)).trim()||'Humano';
    const synthetic=species==='Droide';
    const sex=String(value.sex||value.biologicalSex||(synthetic?'não aplicável':npcPick(seed,'sex',['masculino','masculino','feminino','feminino','intersexo']))).trim();
    const gender=String(value.gender||(synthetic?'não aplicável':(sex==='masculino'?'homem':sex==='feminino'?'mulher':npcPick(seed,'gender',['não-binário','homem','mulher'])))).trim();
    const career=String(value.career||value.className||npcPick(seed,'career',Object.keys(NPC_CAREERS))).trim();
    const specialization=String(value.specialization||npcPick(seed,'specialization',NPC_CAREERS[career]||['Generalista'])).trim();
    const occupation=String(value.occupation||OCCUPATION_BY_SPECIALIZATION[specialization]||career.toLocaleLowerCase('pt-BR')).trim();
    const origin=String(value.origin||value.cultureOrigin||(synthetic?'linha de fabricação não registrada':npcPick(seed,'origin',NPC_ORIGINS))).trim();
    const ageRaw=Number(value.ageYears);const ageYears=Number.isFinite(ageRaw)&&ageRaw>=0&&ageRaw<=500?Math.trunc(ageRaw):ageForSpecies(species,seed);
    const build=npcPick(seed,'build',['esguio','mediano','robusto','atlético']);
    const marker=npcPick(seed,'marker',['sem marca evidente','cicatriz discreta','roupas muito gastas','acessório funcional característico','postura vigilante','aparência cuidadosamente mantida']);
    const appearance=String(value.appearance||`${SPECIES_APPEARANCE[species]||'anatomia própria da espécie'}, porte ${build}, ${marker}`).trim();
    return {profileVersion:1,species,sex,gender,origin,career,specialization,occupation,ageYears,appearance};
}
export function createWorld() {
    return { schemaVersion: 6, npcs: {}, locations: {}, factions: {}, clock: { tick: 0 }, events: [], hooks: [], aftermaths: [], encounterHistory: [], history: [], policy: clone(WORLD_POLICY_SOURCE) };
}
const FACTION_PLAN_STATUSES = new Set(['planned', 'active', 'completed', 'aborted']);
const FACTION_PLAN_STEP_STATUSES = new Set(['planned', 'active', 'completed', 'skipped']);
function normalizeFactionPlan(plan) {
    if (!plan || typeof plan !== 'object')
        return null;
    const steps = (Array.isArray(plan.steps) ? plan.steps : []).slice(0, 20).map((step, index) => {
        const src = typeof step === 'string' ? { text: step } : step ?? {};
        const cleanText = String(src.text ?? '').trim();
        if (!cleanText)
            return null;
        return { id: src.id ?? (0, ids_mjs_1.newId)('fstep'), text: cleanText, status: FACTION_PLAN_STEP_STATUSES.has(src.status) ? src.status : 'planned', locationId: src.locationId ? String(src.locationId) : null, knownToPlayer: Boolean(src.knownToPlayer), order: index };
    }).filter(Boolean);
    const status = FACTION_PLAN_STATUSES.has(plan.status) ? plan.status : 'planned';
    return { id: plan.id ?? (0, ids_mjs_1.newId)('fplan'), title: String(plan.title ?? 'Plano').trim() || 'Plano', objective: String(plan.objective ?? '').trim(), status, steps, knownToPlayer: Boolean(plan.knownToPlayer), updatedAt: plan.updatedAt ?? nowIso() };
}
function normalizeFaction(faction, id) {
    if (!faction || typeof faction !== 'object')
        return null;
    const relations = {};
    for (const [targetId, relation] of Object.entries(faction.relations ?? {})) {
        const clean = String(relation ?? '').trim();
        if (clean && targetId !== id)
            relations[String(targetId)] = clean;
    }
    const facts = (Array.isArray(faction.facts) ? faction.facts : []).map(f => ({
        id: f?.id ?? (0, ids_mjs_1.newId)('ffact'),
        text: String(f?.text ?? f ?? '').trim(),
        knownToPlayer: Boolean(f?.knownToPlayer)
    })).filter(f => f.text);
    return {
        id: faction.id ?? id,
        name: String(faction.name ?? 'Facção').trim() || 'Facção',
        objective: String(faction.objective ?? faction.goal ?? '').trim(),
        status: String(faction.status ?? 'active').trim() || 'active',
        resources: [...new Set((Array.isArray(faction.resources) ? faction.resources : []).map(v => String(v).trim()).filter(Boolean))],
        relations,
        facts,
        plans: (Array.isArray(faction.plans) ? faction.plans : []).map(normalizeFactionPlan).filter(Boolean),
        knownToPlayer: Boolean(faction.knownToPlayer),
        tags: Array.isArray(faction.tags) ? faction.tags.map(String) : [],
        updatedAt: faction.updatedAt ?? nowIso()
    };
}
function normalizeNpcCombatState(value) {
    if (!value || typeof value !== 'object')
        return null;
    const wounds = value.wounds && typeof value.wounds === 'object' ? { current: Math.max(0, Number(value.wounds.current) || 0), threshold: Math.max(0, Number(value.wounds.threshold) || 0), perMember: Math.max(0, Number(value.wounds.perMember) || 0) } : null;
    const strain = value.strain && typeof value.strain === 'object' ? { current: Math.max(0, Number(value.strain.current) || 0), threshold: Math.max(0, Number(value.strain.threshold) || 0) } : null;
    return { profileKey: value.profileKey ? String(value.profileKey) : null, kind: value.kind ? String(value.kind) : null, wounds, strain, criticalInjuries: Array.isArray(value.criticalInjuries) ? clone(value.criticalInjuries) : [], status: String(value.status ?? 'active') };
}
function normalizeAftermath(item) {
    if (!item || typeof item !== 'object')
        return null;
    const status = ['active', 'closed'].includes(item.status) ? item.status : 'active';
    const summary = String(item.summary ?? '').trim();
    if (!summary)
        return null;
    return {
        id: item.id ?? (0, ids_mjs_1.newId)('waft'), hookId: item.hookId ? String(item.hookId) : null, encounterId: item.encounterId ? String(item.encounterId) : null,
        factionId: item.factionId ? String(item.factionId) : null, planId: item.planId ? String(item.planId) : null, npcId: item.npcId ? String(item.npcId) : null, locationId: item.locationId ? String(item.locationId) : null,
        outcome: String(item.outcome ?? '').trim(), summary, status, knownToPlayer: Boolean(item.knownToPlayer),
        createdAtTick: Math.max(0, Number(item.createdAtTick) || 0), closedAtTick: item.closedAtTick === null || item.closedAtTick === undefined ? null : Math.max(0, Number(item.closedAtTick) || 0),
        resolution: item.resolution ? String(item.resolution).trim() : null, source: item.source ?? clone(WORLD_POLICY_SOURCE)
    };
}
function normalizeEncounterRecord(item) {
    if (!item || typeof item !== 'object')
        return null;
    const outcome = String(item.outcome ?? '').trim();
    if (!outcome)
        return null;
    return {
        id: item.id ?? (0, ids_mjs_1.newId)('wenc'), hookId: item.hookId ? String(item.hookId) : null, factionId: item.factionId ? String(item.factionId) : null, planId: item.planId ? String(item.planId) : null, locationId: item.locationId ? String(item.locationId) : null,
        outcome, summary: String(item.summary ?? '').trim(), rounds: Math.max(0, Number(item.rounds) || 0),
        adversaries: (Array.isArray(item.adversaries) ? item.adversaries : []).slice(0, 40).map(a => ({ id: String(a?.id ?? ''), name: String(a?.name ?? ''), worldNpcId: a?.worldNpcId ? String(a.worldNpcId) : null, factionId: a?.factionId ? String(a.factionId) : null, status: String(a?.status ?? 'active'), wounds: Math.max(0, Number(a?.wounds) || 0), strain: Math.max(0, Number(a?.strain) || 0), criticalCount: Math.max(0, Number(a?.criticalCount) || 0) })).filter(a => a.id || a.name),
        createdAtTick: Math.max(0, Number(item.createdAtTick) || 0), source: item.source ?? clone(WORLD_POLICY_SOURCE)
    };
}
function normalizeHook(hook) {
    if (!hook || typeof hook !== 'object')
        return null;
    const status = ['pending', 'resolved', 'dismissed'].includes(hook.status) ? hook.status : 'pending';
    return {
        id: hook.id ?? (0, ids_mjs_1.newId)('whook'),
        kind: String(hook.kind ?? 'world_consequence'),
        sourceEventId: hook.sourceEventId ? String(hook.sourceEventId) : null,
        npcId: hook.npcId ? String(hook.npcId) : null,
        factionId: hook.factionId ? String(hook.factionId) : null,
        planId: hook.planId ? String(hook.planId) : null,
        locationId: hook.locationId ? String(hook.locationId) : null,
        summary: String(hook.summary ?? '').trim(),
        status,
        dedupeKey: hook.dedupeKey ? String(hook.dedupeKey) : null,
        createdAtTick: Math.max(0, Number(hook.createdAtTick) || 0),
        resolvedAtTick: hook.resolvedAtTick === null || hook.resolvedAtTick === undefined ? null : Math.max(0, Number(hook.resolvedAtTick) || 0),
        resolution: hook.resolution ? String(hook.resolution).trim() : null,
        source: hook.source ?? clone(WORLD_POLICY_SOURCE)
    };
}
export function normalizeWorld(value) {
    const base = createWorld();
    const src = value && typeof value === 'object' ? clone(value) : {};
    const next = {
        ...base, ...src,
        npcs: { ...(src.npcs ?? {}) },
        locations: { ...(src.locations ?? {}) },
        factions: { ...(src.factions ?? {}) },
        clock: { ...base.clock, ...(src.clock ?? {}) },
        events: Array.isArray(src.events) ? src.events : [],
        hooks: Array.isArray(src.hooks) ? src.hooks : [],
        aftermaths: Array.isArray(src.aftermaths) ? src.aftermaths : [],
        encounterHistory: Array.isArray(src.encounterHistory) ? src.encounterHistory : [],
        history: Array.isArray(src.history) ? src.history : []
    };
    next.schemaVersion = 6;
    next.clock.tick = Math.max(0, Number(next.clock.tick) || 0);
    next.policy = clone(WORLD_POLICY_SOURCE);
    for (const [id, npc0] of Object.entries(next.npcs)) {
        if (!npc0 || typeof npc0 !== 'object') {
            delete next.npcs[id];
            continue;
        }
        const npc = { ...npc0, id: npc0.id ?? id };
        npc.name = String(npc.name ?? 'PNPC').trim() || 'PNPC';
        npc.locationId = npc.locationId ?? null;
        npc.factionId = npc.factionId ?? null;
        npc.goal = String(npc.goal ?? npc.objective ?? '').trim();
        npc.relation = String(npc.relation ?? npc.relationship ?? 'neutral').trim() || 'neutral';
        npc.status = String(npc.status ?? 'active');
        npc.knownToPlayer = Boolean(npc.knownToPlayer);
        Object.assign(npc, normalizeNpcProfile(npc));
        const legacyFacts = Array.isArray(npc.knownFacts) ? npc.knownFacts.map(f => ({ id: (0, ids_mjs_1.newId)('fact'), text: String(f), knownToPlayer: true })) : [];
        npc.facts = (Array.isArray(npc.facts) ? npc.facts : legacyFacts).map(f => ({ id: f.id ?? (0, ids_mjs_1.newId)('fact'), text: String(f.text ?? f).trim(), knownToPlayer: Boolean(f.knownToPlayer) })).filter(f => f.text);
        npc.tags = Array.isArray(npc.tags) ? npc.tags.map(String) : [];
        npc.combatState = normalizeNpcCombatState(npc.combatState);
        npc.updatedAt = npc.updatedAt ?? nowIso();
        delete npc.objective;
        delete npc.relationship;
        delete npc.knownFacts;
        next.npcs[id] = npc;
    }
    for (const [id, loc0] of Object.entries(next.locations)) {
        if (!loc0 || typeof loc0 !== 'object') {
            delete next.locations[id];
            continue;
        }
        next.locations[id] = { id: loc0.id ?? id, name: String(loc0.name ?? id), description: String(loc0.description ?? ''), knownToPlayer: Boolean(loc0.knownToPlayer), tags: Array.isArray(loc0.tags) ? loc0.tags.map(String) : [] };
    }
    for (const [id, faction0] of Object.entries(next.factions)) {
        const faction = normalizeFaction(faction0, id);
        if (!faction) {
            delete next.factions[id];
            continue;
        }
        next.factions[id] = faction;
    }
    for (const npc of Object.values(next.npcs)) {
        if (npc.factionId && !next.factions[npc.factionId])
            npc.factionId = null;
    }
    for (const faction of Object.values(next.factions)) {
        for (const targetId of Object.keys(faction.relations))
            if (!next.factions[targetId])
                delete faction.relations[targetId];
    }
    next.events = next.events.map(e => normalizeEvent(e)).filter(Boolean);
    next.hooks = next.hooks.map(h => normalizeHook(h)).filter(h => h?.summary);
    next.aftermaths = next.aftermaths.map(normalizeAftermath).filter(Boolean);
    next.encounterHistory = next.encounterHistory.map(normalizeEncounterRecord).filter(Boolean);
    return next;
}
export function createLocation({ id = null, name, description = '', knownToPlayer = false, tags = [] } = {}) {
    const cleanName = text(name, 'Nome do local', { required: true, max: 120 });
    return { id: id || (0, ids_mjs_1.newId)('loc'), name: cleanName, description: text(description, 'Descrição do local', { max: 1000 }), knownToPlayer: Boolean(knownToPlayer), tags: Array.isArray(tags) ? tags.map(v => String(v).trim()).filter(Boolean) : [] };
}
export function upsertLocation(world, location) {
    if (!location?.id)
        throw new Error('Local sem ID estável.');
    const next = normalizeWorld(world);
    next.locations[location.id] = clone(location);
    return next;
}
export function createFaction({ name, objective = '', status = 'active', resources = [], knownToPlayer = false, tags = [] } = {}) {
    const cleanName = text(name, 'Nome da facção', { required: true, max: 120 });
    return normalizeFaction({
        id: (0, ids_mjs_1.newId)('fac'), name: cleanName, objective: text(objective, 'Objetivo da facção', { max: 500 }),
        status: text(status, 'Estado da facção', { max: 80 }) || 'active',
        resources: Array.isArray(resources) ? resources : [], relations: {}, facts: [], knownToPlayer: Boolean(knownToPlayer),
        tags: Array.isArray(tags) ? tags : [], plans: [], updatedAt: nowIso()
    }, null);
}
export function upsertFaction(world, faction) {
    if (!faction?.id)
        throw new Error('Facção sem ID estável.');
    const next = normalizeWorld(world);
    const clean = normalizeFaction(faction, faction.id);
    if (!clean)
        throw new Error('Facção inválida.');
    next.factions[clean.id] = clean;
    return next;
}
export function setFactionObjective(world, factionId, objective) {
    const next = normalizeWorld(world);
    if (!next.factions[factionId])
        throw new Error('Facção não encontrada.');
    next.factions[factionId].objective = text(objective, 'Objetivo da facção', { max: 500 });
    next.factions[factionId].updatedAt = nowIso();
    return next;
}
export function setFactionStatus(world, factionId, status) {
    const next = normalizeWorld(world);
    if (!next.factions[factionId])
        throw new Error('Facção não encontrada.');
    next.factions[factionId].status = text(status, 'Estado da facção', { required: true, max: 80 });
    next.factions[factionId].updatedAt = nowIso();
    return next;
}
export function addFactionResource(world, factionId, resource) {
    const next = normalizeWorld(world);
    const f = next.factions[factionId];
    if (!f)
        throw new Error('Facção não encontrada.');
    const clean = text(resource, 'Recurso da facção', { required: true, max: 160 });
    if (!f.resources.includes(clean))
        f.resources.push(clean);
    f.updatedAt = nowIso();
    return next;
}
export function removeFactionResource(world, factionId, resource) {
    const next = normalizeWorld(world);
    const f = next.factions[factionId];
    if (!f)
        throw new Error('Facção não encontrada.');
    const clean = text(resource, 'Recurso da facção', { required: true, max: 160 });
    const i = f.resources.indexOf(clean);
    if (i < 0)
        throw new Error('Recurso da facção não encontrado.');
    f.resources.splice(i, 1);
    f.updatedAt = nowIso();
    return next;
}
export function setFactionRelation(world, factionId, targetFactionId, relation) {
    const next = normalizeWorld(world);
    const f = next.factions[factionId];
    const target = next.factions[targetFactionId];
    if (!f || !target)
        throw new Error('Facção não encontrada.');
    if (factionId === targetFactionId)
        throw new Error('Uma facção não pode ter relação consigo mesma.');
    f.relations[targetFactionId] = text(relation, 'Relação entre facções', { required: true, max: 120 });
    f.updatedAt = nowIso();
    return next;
}
export function recordFactionFact(world, factionId, { text: factText, knownToPlayer = false } = {}) {
    const next = normalizeWorld(world);
    const f = next.factions[factionId];
    if (!f)
        throw new Error('Facção não encontrada.');
    const clean = text(factText, 'Fato da facção', { required: true, max: 1000 });
    f.facts.push({ id: (0, ids_mjs_1.newId)('ffact'), text: clean, knownToPlayer: Boolean(knownToPlayer) });
    f.updatedAt = nowIso();
    return next;
}
export function createFactionPlan(world, factionId, { title, objective = '', steps = [], knownToPlayer = false } = {}) {
    const next = normalizeWorld(world);
    const faction = next.factions[factionId];
    if (!faction)
        throw new Error('Facção não encontrada.');
    const cleanTitle = text(title, 'Título do plano', { required: true, max: 160 });
    if (!Array.isArray(steps) || steps.length < 1 || steps.length > 20)
        throw new Error('O plano da facção exige de 1 a 20 etapas.');
    const normalizedSteps = steps.map((step, index) => {
        const src = typeof step === 'string' ? { text: step } : step;
        if (!src || typeof src !== 'object')
            throw new Error(`Etapa ${index + 1} do plano inválida.`);
        const locationId = src.locationId ? String(src.locationId) : null;
        if (locationId && !next.locations[locationId])
            throw new Error(`Local não encontrado na etapa do plano: ${locationId}`);
        return { id: (0, ids_mjs_1.newId)('fstep'), text: text(src.text, 'Etapa do plano', { required: true, max: 300 }), status: 'planned', locationId, knownToPlayer: Boolean(src.knownToPlayer), order: index };
    });
    const plan = normalizeFactionPlan({ id: (0, ids_mjs_1.newId)('fplan'), title: cleanTitle, objective: text(objective, 'Objetivo do plano', { max: 500 }), status: 'planned', steps: normalizedSteps, knownToPlayer: Boolean(knownToPlayer), updatedAt: nowIso() });
    faction.plans.push(plan);
    faction.updatedAt = nowIso();
    return { world: next, plan: clone(plan) };
}
export function setFactionPlanStatus(world, factionId, planId, status) {
    const next = normalizeWorld(world);
    const faction = next.factions[factionId];
    if (!faction)
        throw new Error('Facção não encontrada.');
    const plan = faction.plans.find(p => p.id === planId);
    if (!plan)
        throw new Error('Plano da facção não encontrado.');
    if (!FACTION_PLAN_STATUSES.has(status))
        throw new Error('Estado do plano da facção inválido.');
    if (plan.status === 'completed' || plan.status === 'aborted')
        throw new Error('Plano encerrado não pode mudar de estado.');
    if (plan.status === 'active' && status === 'planned')
        throw new Error('Plano ativo não pode voltar a planejado.');
    plan.status = status;
    if (status === 'active' && !plan.steps.some(s => s.status === 'active')) {
        const first = plan.steps.find(s => s.status === 'planned');
        if (first)
            first.status = 'active';
    }
    if (status === 'completed')
        for (const step of plan.steps)
            if (step.status === 'planned' || step.status === 'active')
                step.status = 'skipped';
    if (status === 'aborted')
        for (const step of plan.steps)
            if (step.status === 'planned' || step.status === 'active')
                step.status = 'skipped';
    plan.updatedAt = nowIso();
    faction.updatedAt = nowIso();
    return next;
}
export function advanceFactionPlan(world, factionId, planId) {
    const next = normalizeWorld(world);
    const faction = next.factions[factionId];
    if (!faction)
        throw new Error('Facção não encontrada.');
    const plan = faction.plans.find(p => p.id === planId);
    if (!plan)
        throw new Error('Plano da facção não encontrado.');
    if (plan.status === 'completed' || plan.status === 'aborted')
        throw new Error('Plano encerrado não pode avançar.');
    if (plan.status === 'planned') {
        plan.status = 'active';
        const first = plan.steps.find(s => s.status === 'planned');
        if (first)
            first.status = 'active';
        plan.updatedAt = nowIso();
        faction.updatedAt = nowIso();
        return { world: next, plan: clone(plan), completedStep: null, nextStep: clone(first ?? null) };
    }
    const active = plan.steps.find(s => s.status === 'active');
    if (!active)
        throw new Error('Plano ativo sem etapa ativa.');
    active.status = 'completed';
    const following = plan.steps.find(s => s.status === 'planned');
    if (following)
        following.status = 'active';
    else
        plan.status = 'completed';
    plan.updatedAt = nowIso();
    faction.updatedAt = nowIso();
    return { world: next, plan: clone(plan), completedStep: clone(active), nextStep: clone(following ?? null) };
}
export function playerVisibleFaction(faction) {
    const copy = clone(faction);
    copy.facts = (copy.facts ?? []).filter(f => f.knownToPlayer);
    copy.plans = (copy.plans ?? []).filter(p => p.knownToPlayer).map(p => ({ ...p, steps: (p.steps ?? []).filter(s => s.knownToPlayer || s.status === 'completed') }));
    return copy;
}
export function setNpcFaction(world, npcId, factionId = null) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    if (factionId && !next.factions[factionId])
        throw new Error('Facção não encontrada.');
    next.npcs[npcId].factionId = factionId || null;
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function createNpc({ name, locationId = null, factionId = null, goal = '', relation = 'neutral', status = 'active', knownToPlayer = false, tags = [], species = '', sex = '', gender = '', origin = '', career = '', specialization = '', occupation = '', ageYears = null, appearance = '' } = {}) {
    const cleanName = text(name, 'Nome do PNPC', { required: true, max: 120 });
    const id = (0, ids_mjs_1.newId)('npc');
    const profile = normalizeNpcProfile({ id, name: cleanName, species, sex, gender, origin, career, specialization, occupation, ageYears, appearance });
    return { id, name: cleanName, ...profile, locationId, factionId, goal: text(goal, 'Objetivo do PNPC', { max: 500 }), relation: text(relation, 'Relação', { max: 80 }) || 'neutral', status: text(status, 'Estado', { max: 80 }) || 'active', knownToPlayer: Boolean(knownToPlayer), facts: [], tags: Array.isArray(tags) ? tags.map(v => String(v).trim()).filter(Boolean) : [], combatState: null, updatedAt: nowIso() };
}
export function upsertNpc(world, npc) {
    if (!npc?.id)
        throw new Error('PNPC sem ID estável.');
    const next = normalizeWorld(world);
    next.npcs[npc.id] = clone(npc);
    return next;
}
export function moveNpc(world, npcId, locationId) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].locationId = locationId;
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function setNpcGoal(world, npcId, goal) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].goal = text(goal, 'Objetivo do PNPC', { max: 500 });
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function setNpcRelation(world, npcId, relation) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].relation = text(relation, 'Relação', { required: true, max: 80 });
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function setNpcStatus(world, npcId, status) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].status = text(status, 'Estado do PNPC', { required: true, max: 80 });
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function setNpcVisibility(world, npcId, knownToPlayer) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].knownToPlayer = Boolean(knownToPlayer);
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function setNpcCombatState(world, npcId, combatState) {
    const next = normalizeWorld(world);
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].combatState = normalizeNpcCombatState(combatState);
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function recordNpcFact(world, npcId, { text: factText, knownToPlayer = false } = {}) {
    const next = normalizeWorld(world);
    const clean = text(factText, 'Fato', { required: true, max: 1000 });
    if (!next.npcs[npcId])
        throw new Error('PNPC não encontrado.');
    next.npcs[npcId].facts.push({ id: (0, ids_mjs_1.newId)('fact'), text: clean, knownToPlayer: Boolean(knownToPlayer) });
    next.npcs[npcId].updatedAt = nowIso();
    return next;
}
export function playerVisibleNpc(npc) {
    const copy = clone(npc);
    copy.facts = (copy.facts ?? []).filter(f => f.knownToPlayer);
    return copy;
}
function normalizeEvent(event) {
    if (!event || typeof event !== 'object')
        return null;
    const dueTick = Math.max(0, Number(event.dueTick) || 0);
    return { id: event.id ?? (0, ids_mjs_1.newId)('wevt'), type: String(event.type ?? ''), dueTick, payload: clone(event.payload ?? {}), status: ['pending', 'done', 'failed', 'cancelled'].includes(event.status) ? event.status : 'pending', dedupeKey: event.dedupeKey ? String(event.dedupeKey) : null, createdAt: event.createdAt ?? nowIso(), executedAtTick: event.executedAtTick ?? null, error: event.error ?? null, source: event.source ?? clone(WORLD_POLICY_SOURCE) };
}
export function scheduleWorldEvent(world, { type, dueTick, payload = {}, dedupeKey = null } = {}) {
    const next = normalizeWorld(world);
    const cleanType = text(type, 'Tipo de evento', { required: true, max: 80 });
    const tick = int(dueTick, 'Tick do evento', { min: next.clock.tick });
    const key = dedupeKey ? text(dedupeKey, 'Chave de deduplicação', { required: true, max: 160 }) : null;
    if (key) {
        const existing = next.events.find(e => e.dedupeKey === key && e.status !== 'cancelled');
        if (existing)
            return { world: next, event: clone(existing), created: false };
    }
    const event = normalizeEvent({ id: (0, ids_mjs_1.newId)('wevt'), type: cleanType, dueTick: tick, payload, dedupeKey: key, status: 'pending', createdAt: nowIso(), source: clone(WORLD_POLICY_SOURCE) });
    next.events.push(event);
    return { world: next, event: clone(event), created: true };
}
export function cancelWorldEvent(world, eventId) {
    const next = normalizeWorld(world);
    const event = next.events.find(e => e.id === eventId);
    if (!event)
        throw new Error('Evento de mundo não encontrado.');
    if (event.status === 'done')
        throw new Error('Evento já executado não pode ser cancelado.');
    event.status = 'cancelled';
    return next;
}
function applyWorldEvent(world, event) {
    let next = world;
    const p = event.payload ?? {};
    const factionEvent = event.type.startsWith('set_faction_') || event.type === 'add_faction_resource' || event.type === 'remove_faction_resource' || event.type === 'record_faction_fact' || event.type === 'advance_faction_plan';
    if (factionEvent && p.locationId && !next.locations?.[p.locationId])
        throw new Error(`Local não encontrado para evento de facção: ${p.locationId}`);
    if (event.type === 'move_npc')
        next = moveNpc(next, String(p.npcId ?? ''), p.locationId ?? null);
    else if (event.type === 'set_npc_goal')
        next = setNpcGoal(next, String(p.npcId ?? ''), p.goal ?? '');
    else if (event.type === 'set_npc_relation')
        next = setNpcRelation(next, String(p.npcId ?? ''), p.relation ?? 'neutral');
    else if (event.type === 'set_npc_status')
        next = setNpcStatus(next, String(p.npcId ?? ''), p.status ?? 'active');
    else if (event.type === 'record_npc_fact')
        next = recordNpcFact(next, String(p.npcId ?? ''), { text: p.text, knownToPlayer: Boolean(p.knownToPlayer) });
    else if (event.type === 'set_npc_faction')
        next = setNpcFaction(next, String(p.npcId ?? ''), p.factionId ?? null);
    else if (event.type === 'set_faction_objective')
        next = setFactionObjective(next, String(p.factionId ?? ''), p.objective ?? '');
    else if (event.type === 'set_faction_status')
        next = setFactionStatus(next, String(p.factionId ?? ''), p.status ?? 'active');
    else if (event.type === 'add_faction_resource')
        next = addFactionResource(next, String(p.factionId ?? ''), p.resource ?? '');
    else if (event.type === 'remove_faction_resource')
        next = removeFactionResource(next, String(p.factionId ?? ''), p.resource ?? '');
    else if (event.type === 'set_faction_relation')
        next = setFactionRelation(next, String(p.factionId ?? ''), String(p.targetFactionId ?? ''), p.relation ?? '');
    else if (event.type === 'record_faction_fact')
        next = recordFactionFact(next, String(p.factionId ?? ''), { text: p.text, knownToPlayer: Boolean(p.knownToPlayer) });
    else if (event.type === 'advance_faction_plan')
        next = advanceFactionPlan(next, String(p.factionId ?? ''), String(p.planId ?? '')).world;
    else
        throw new Error(`Tipo de evento de mundo não suportado: ${event.type}`);
    return next;
}
function hookSummaryForEvent(world, event) {
    const p = event.payload ?? {};
    const npcId = String(p.npcId ?? '');
    const npc = npcId ? world.npcs?.[npcId] ?? null : null;
    const factionId = String(p.factionId ?? '');
    const faction = factionId ? world.factions?.[factionId] ?? null : null;
    const planForEvent = event.type === 'advance_faction_plan' && faction ? faction.plans?.find(x => x.id === String(p.planId ?? '')) : null;
    const activePlanStep = planForEvent?.steps?.find(x => x.status === 'active') ?? null;
    const locationId = npc?.locationId ?? (p.locationId && world.locations?.[p.locationId] ? String(p.locationId) : null) ?? (activePlanStep?.locationId && world.locations?.[activePlanStep.locationId] ? activePlanStep.locationId : null);
    const location = locationId ? world.locations?.[locationId] : null;
    let summary = '';
    if (event.type === 'move_npc' && npc)
        summary = `${npc.name} chegou a ${location?.name ?? locationId ?? 'um destino não identificado'}.`;
    else if (event.type === 'set_npc_goal' && npc)
        summary = `${npc.name} mudou de objetivo: ${npc.goal || 'sem objetivo definido'}.`;
    else if (event.type === 'set_npc_relation' && npc)
        summary = `${npc.name} agora mantém relação ${npc.relation}.`;
    else if (event.type === 'set_npc_status' && npc)
        summary = `${npc.name} mudou de estado para ${npc.status}.`;
    else if (event.type === 'record_npc_fact' && npc)
        summary = `Um novo fato persistente foi registrado para ${npc.name}.`;
    else if (event.type === 'set_npc_faction' && npc)
        summary = `${npc.name} agora está ligado à facção ${world.factions?.[npc.factionId]?.name ?? 'nenhuma'}.`;
    else if (event.type === 'set_faction_objective' && faction)
        summary = `${faction.name} mudou de objetivo: ${faction.objective || 'sem objetivo definido'}.`;
    else if (event.type === 'set_faction_status' && faction)
        summary = `${faction.name} mudou de estado para ${faction.status}.`;
    else if (event.type === 'add_faction_resource' && faction)
        summary = `${faction.name} passou a dispor de ${String(p.resource ?? 'um novo recurso')}.`;
    else if (event.type === 'remove_faction_resource' && faction)
        summary = `${faction.name} deixou de dispor de ${String(p.resource ?? 'um recurso')}.`;
    else if (event.type === 'set_faction_relation' && faction)
        summary = `A relação de ${faction.name} com ${world.factions?.[p.targetFactionId]?.name ?? p.targetFactionId ?? 'outra facção'} mudou para ${String(p.relation ?? '')}.`;
    else if (event.type === 'record_faction_fact' && faction)
        summary = `Um novo fato persistente foi registrado para a facção ${faction.name}.`;
    else if (event.type === 'advance_faction_plan' && faction) {
        const plan = faction.plans?.find(x => x.id === String(p.planId ?? ''));
        const active = plan?.steps?.find(x => x.status === 'active');
        summary = plan?.status === 'completed' ? `${faction.name} concluiu o plano ${plan.title}.` : `${faction.name} avançou o plano ${plan?.title ?? 'em andamento'}${active ? `: ${active.text}` : ''}.`;
    }
    if (!summary)
        return null;
    return normalizeHook({
        id: (0, ids_mjs_1.newId)('whook'), kind: faction ? 'faction_consequence' : 'world_consequence', sourceEventId: event.id, npcId: npc?.id ?? null, factionId: faction?.id ?? null, planId: event.type === 'advance_faction_plan' ? String(p.planId ?? '') || null : null, locationId,
        summary, status: 'pending', dedupeKey: `event:${event.id}`, createdAtTick: world.clock.tick, source: clone(WORLD_POLICY_SOURCE)
    });
}
function appendHookFromEvent(world, event) {
    const next = normalizeWorld(world);
    const hook = hookSummaryForEvent(next, event);
    if (!hook)
        return { world: next, hook: null, created: false };
    if (next.hooks.some(h => h.dedupeKey === hook.dedupeKey))
        return { world: next, hook: null, created: false };
    next.hooks.push(hook);
    return { world: next, hook: clone(hook), created: true };
}
export function resolveWorldHook(world, hookId, { resolution = '', dismiss = false } = {}) {
    const next = normalizeWorld(world);
    const hook = next.hooks.find(h => h.id === hookId);
    if (!hook)
        throw new Error('Consequência do mundo não encontrada.');
    if (hook.status !== 'pending')
        throw new Error('Essa consequência do mundo já foi resolvida.');
    hook.status = dismiss ? 'dismissed' : 'resolved';
    hook.resolvedAtTick = next.clock.tick;
    hook.resolution = text(resolution, 'Resolução da consequência', { max: 800 }) || null;
    next.history.push({ id: (0, ids_mjs_1.newId)('whist'), hookId: hook.id, type: 'resolve_world_hook', tick: next.clock.tick, status: hook.status, resolution: hook.resolution });
    return next;
}
export function recordEncounterOutcome(world, { hookId = null, outcome, summary = '', rounds = 0, adversaries = [] } = {}) {
    const next = normalizeWorld(world);
    const cleanOutcome = text(outcome, 'Desfecho do encontro', { required: true, max: 80 });
    const hook = hookId ? next.hooks.find(h => h.id === hookId) : null;
    if (hookId && !hook)
        throw new Error('Consequência causal do encontro não encontrada.');
    const existing = hookId ? next.encounterHistory.find(e => e.hookId === hookId) : null;
    if (existing)
        return { world: next, encounter: clone(existing), created: false };
    const record = normalizeEncounterRecord({
        id: (0, ids_mjs_1.newId)('wenc'), hookId: hook?.id ?? null, factionId: hook?.factionId ?? null, planId: hook?.planId ?? null, locationId: hook?.locationId ?? null,
        outcome: cleanOutcome, summary: text(summary, 'Resumo do encontro', { max: 1000 }), rounds: int(rounds, 'Rodadas do encontro', { min: 0, max: 100000 }),
        adversaries, createdAtTick: next.clock.tick, source: clone(WORLD_POLICY_SOURCE)
    });
    next.encounterHistory.push(record);
    let after = null;
    if (hook) {
        after = normalizeAftermath({
            id: (0, ids_mjs_1.newId)('waft'), hookId: hook.id, encounterId: record.id, factionId: record.factionId, planId: record.planId, locationId: record.locationId,
            outcome: record.outcome, summary: record.summary || `Encontro encerrado com desfecho ${record.outcome}.`, status: 'active', knownToPlayer: true, createdAtTick: next.clock.tick, source: clone(WORLD_POLICY_SOURCE)
        });
        next.aftermaths.push(after);
    }
    next.history.push({ id: (0, ids_mjs_1.newId)('whist'), type: 'record_encounter_outcome', tick: next.clock.tick, encounterId: record.id, hookId: hook?.id ?? null, outcome: record.outcome, aftermathId: after?.id ?? null });
    return { world: next, encounter: clone(record), aftermath: after ? clone(after) : null, created: true };
}
export function closeWorldAftermath(world, aftermathId, { resolution = '' } = {}) {
    const next = normalizeWorld(world);
    const item = next.aftermaths.find(a => a.id === aftermathId);
    if (!item)
        throw new Error('Consequência de longo prazo não encontrada.');
    if (item.status !== 'active')
        throw new Error('Essa consequência de longo prazo já foi encerrada.');
    item.status = 'closed';
    item.closedAtTick = next.clock.tick;
    item.resolution = text(resolution, 'Resolução da consequência de longo prazo', { max: 1000 }) || null;
    next.history.push({ id: (0, ids_mjs_1.newId)('whist'), type: 'close_aftermath', tick: next.clock.tick, aftermathId: item.id, resolution: item.resolution });
    return next;
}
export function activeWorldAftermaths(world, { limit = 50, knownOnly = false } = {}) {
    const w = normalizeWorld(world);
    const max = int(limit, 'Limite de consequências de longo prazo', { min: 1, max: 200 });
    return w.aftermaths.filter(a => a.status === 'active' && (!knownOnly || a.knownToPlayer)).sort((a, b) => a.createdAtTick - b.createdAtTick || String(a.id).localeCompare(String(b.id))).slice(0, max).map(clone);
}
export function causalEncounterCandidates(world, locationId, { limit = 12 } = {}) {
    const w = normalizeWorld(world);
    if (!locationId)
        return [];
    const max = int(limit, 'Limite de consequências', { min: 1, max: 50 });
    const hooks = w.hooks.filter(h => h.status === 'pending' && h.locationId === locationId).sort((a, b) => a.createdAtTick - b.createdAtTick || String(a.id).localeCompare(String(b.id)));
    return hooks.slice(0, max).map(h => {
        const npc = h.npcId ? w.npcs[h.npcId] : null;
        const faction = h.factionId ? w.factions[h.factionId] : null;
        return { hookId: h.id, sourceEventId: h.sourceEventId, planId: h.planId ?? null, locationId: h.locationId, summary: h.summary, npc: npc ? { id: npc.id, name: npc.name, goal: npc.goal, relation: npc.relation, status: npc.status, factionId: npc.factionId ?? null } : null, faction: faction ? { id: faction.id, name: faction.name, objective: faction.objective, status: faction.status } : null, source: clone(WORLD_POLICY_SOURCE) };
    });
}
export function advanceWorld(world, { ticks = 1, maxEvents = 50 } = {}) {
    let next = normalizeWorld(world);
    const delta = int(ticks, 'Avanço do mundo', { min: 0, max: 100000 });
    const budget = int(maxEvents, 'Orçamento de eventos', { min: 1, max: 1000 });
    next.clock.tick += delta;
    const due = next.events.filter(e => e.status === 'pending' && e.dueTick <= next.clock.tick).sort((a, b) => a.dueTick - b.dueTick || String(a.id).localeCompare(String(b.id)));
    const processed = [];
    const hooksCreated = [];
    let used = 0;
    for (const dueEvent of due) {
        if (used >= budget)
            break;
        const index = next.events.findIndex(e => e.id === dueEvent.id);
        if (index < 0)
            continue;
        try {
            next = applyWorldEvent(next, next.events[index]);
            const current = next.events.find(e => e.id === dueEvent.id);
            current.status = 'done';
            current.executedAtTick = next.clock.tick;
            current.error = null;
            const hooked = appendHookFromEvent(next, current);
            next = hooked.world;
            if (hooked.created && hooked.hook)
                hooksCreated.push(hooked.hook);
            next.history.push({ id: (0, ids_mjs_1.newId)('whist'), eventId: dueEvent.id, type: dueEvent.type, tick: next.clock.tick, status: 'done', hookId: hooked.hook?.id ?? null });
            processed.push({ eventId: dueEvent.id, type: dueEvent.type, status: 'done', hookId: hooked.hook?.id ?? null });
        }
        catch (error) {
            const current = next.events.find(e => e.id === dueEvent.id);
            current.status = 'failed';
            current.executedAtTick = next.clock.tick;
            current.error = String(error?.message ?? error);
            next.history.push({ id: (0, ids_mjs_1.newId)('whist'), eventId: dueEvent.id, type: dueEvent.type, tick: next.clock.tick, status: 'failed', error: current.error });
            processed.push({ eventId: dueEvent.id, type: dueEvent.type, status: 'failed', error: current.error });
        }
        used += 1;
    }
    const pendingDue = next.events.filter(e => e.status === 'pending' && e.dueTick <= next.clock.tick).length;
    return { world: next, processed, hooksCreated, pendingDue, tick: next.clock.tick, budgetUsed: used };
}
export function masterWorldSummary(world) {
    const w = normalizeWorld(world);
    return {
        schemaVersion: w.schemaVersion,
        tick: w.clock.tick,
        locations: Object.values(w.locations).map(l => ({ id: l.id, name: l.name, description: l.description, knownToPlayer: l.knownToPlayer, tags: [...l.tags] })),
        npcs: Object.values(w.npcs).map(n => ({ id: n.id, name: n.name, species: n.species, sex: n.sex, gender: n.gender, origin: n.origin, career: n.career, specialization: n.specialization, occupation: n.occupation, ageYears: n.ageYears, appearance: n.appearance, locationId: n.locationId, factionId: n.factionId ?? null, goal: n.goal, relation: n.relation, status: n.status, knownToPlayer: Boolean(n.knownToPlayer), facts: clone(n.facts), tags: [...n.tags], combat: n.combatState ? { profileKey: n.combatState.profileKey, status: n.combatState.status, wounds: clone(n.combatState.wounds), strain: clone(n.combatState.strain), criticalCount: n.combatState.criticalInjuries.length } : null })),
        factions: Object.values(w.factions).map(f => ({ id: f.id, name: f.name, objective: f.objective, status: f.status, resources: [...f.resources], relations: clone(f.relations), facts: clone(f.facts), plans: clone(f.plans ?? []), knownToPlayer: f.knownToPlayer, tags: [...f.tags] })),
        pendingEvents: w.events.filter(e => e.status === 'pending').slice(0, 100).map(e => ({ id: e.id, type: e.type, dueTick: e.dueTick, payload: clone(e.payload), dedupeKey: e.dedupeKey })),
        pendingHooks: w.hooks.filter(h => h.status === 'pending').slice(0, 100).map(h => clone(h)),
        hookCounts: { pending: w.hooks.filter(h => h.status === 'pending').length, resolved: w.hooks.filter(h => h.status === 'resolved').length, dismissed: w.hooks.filter(h => h.status === 'dismissed').length },
        activeAftermaths: w.aftermaths.filter(a => a.status === 'active').slice(0, 100).map(a => clone(a)),
        aftermathCounts: { active: w.aftermaths.filter(a => a.status === 'active').length, closed: w.aftermaths.filter(a => a.status === 'closed').length },
        recentEncounters: w.encounterHistory.slice(-50).map(e => clone(e)),
        recentHistory: w.history.slice(-50).map(h => clone(h)),
        policy: clone(WORLD_POLICY_SOURCE)
    };
}
