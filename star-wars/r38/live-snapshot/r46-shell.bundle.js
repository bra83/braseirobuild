/* R47 Android host bundle: approved R46 shell sources transpiled for offline WebView. */
(function(g){'use strict';
const modules=Object.create(null),cache=Object.create(null);
function define(id,fn){modules[id]=fn} function resolve(s){const m={"react":"react","react/jsx-runtime":"react/jsx-runtime","@/src/config/screens":"screens","@/src/state/r21Projection":"r21Projection","@/src/state/gameBridgeClient":"gameBridgeClient","@/src/state/libraryBridgeClient":"libraryBridgeClient","@/src/state/geminiBridgeClient":"geminiBridgeClient","@/src/state/imageLibraryBridgeClient":"imageLibraryBridgeClient","./Icon":"Icon","./ReferenceIcon":"ReferenceIcon","./primitives":"primitives","./DebugGallery":"DebugGallery","./Templates":"Templates"};return m[s]||s} function req(s){const id=resolve(s); if(id==='react')return g.React;if(id==='react/jsx-runtime')return {jsx:(t,p,k)=>g.React.createElement(t,k==null?p:{...p,key:k}),jsxs:(t,p,k)=>g.React.createElement(t,k==null?p:{...p,key:k}),Fragment:g.React.Fragment}; if(cache[id])return cache[id].exports;if(!modules[id])throw new Error('R46 shell module not found: '+s+' => '+id);const m=cache[id]={exports:{}};modules[id](m,m.exports,req);return m.exports;}
define("screens",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.utilityNavigation = exports.mainNavigation = exports.screenRegistry = exports.qaStateLabels = exports.qaStates = void 0;
exports.isQaState = isQaState;
exports.getScreen = getScreen;
exports.getScreenById = getScreenById;
exports.screensForDomain = screensForDomain;
exports.qaStates = ['default', 'loading', 'skeleton', 'empty', 'error', 'offline', 'partial', 'demo'];
function isQaState(value) {
    return exports.qaStates.includes(value);
}
exports.qaStateLabels = {
    default: 'Padrão', loading: 'Carregando', skeleton: 'Estrutura', empty: 'Vazio', error: 'Erro', offline: 'Offline · QA', partial: 'Parcial', demo: 'Demonstração',
};
const screen = (n, title, domain, template, icon, description) => ({
    id: `R46_${String(n + 1).padStart(2, '0')}`,
    route: String(n).padStart(2, '0'),
    title,
    domain,
    template,
    icon,
    description,
    availableQaStates: exports.qaStates,
});
/**
 * R46 é a autoridade de arquitetura visual.
 * Combate pessoal e espacial são estados de CENA, não abas principais.
 * MAPA é utilidade/contexto. A UI não resolve mecânicas.
 */
exports.screenRegistry = [
    screen(0, 'Cena — Exploração', 'Cena', 'scene', 'scene', 'Ambiente, narrativa dominante e informação já descoberta.'),
    screen(1, 'Cena — Social', 'Cena', 'scene', 'npc', 'Interação narrativa com participantes e informações públicas.'),
    screen(2, 'Cena — Combate pessoal', 'Cena', 'scene', 'danger', 'Combate narrativo sem HUD tático pesado nem resolução automática.'),
    screen(3, 'Cena — Combate veicular / espacial', 'Cena', 'scene', 'ship', 'Situação dramática da nave; o estado aparece somente quando estiver disponível.'),
    screen(4, 'Cena — Viagem / hiperespaço', 'Cena', 'scene', 'star', 'Destino e rota como intenção; seleção não confirma deslocamento.'),
    screen(5, 'Cena — Consequência / recuperação', 'Cena', 'scene', 'alert', 'Consequências e mudanças de estado já confirmadas.'),
    screen(6, 'Personagem — Ficha', 'Personagem', 'character', 'character', 'Dados atuais do personagem.'),
    screen(7, 'Personagem — Progressão', 'Personagem', 'character', 'star', 'Experiência e opções de progressão disponíveis.'),
    screen(8, 'Itens — Inventário', 'Itens', 'list', 'inventory', 'Somente itens realmente possuídos.'),
    screen(9, 'Itens — Aquisição pendente', 'Itens', 'detail', 'inventory', 'Pedido ao Mestre; sem compra, disponibilidade ou desconto automáticos.'),
    screen(10, 'Nave — Resumo', 'Nave', 'detail', 'ship', 'Estado e estatísticas atuais da nave quando disponíveis.'),
    screen(11, 'Nave — Danos críticos', 'Nave', 'detail', 'alert', 'Críticos já registrados; a UI não cria nem remove críticos.'),
    screen(12, 'Registros — Missão', 'Registros', 'detail', 'mission', 'Objetivos e fatos públicos da missão.'),
    screen(13, 'Registros — Grupo / Base', 'Registros', 'list', 'party', 'Grupo e base somente quando existirem dados reais.'),
    screen(14, 'Registros — Contatos conhecidos', 'Registros', 'list', 'contacts', 'Somente contatos descobertos e informações públicas.'),
    screen(15, 'Registros — Histórico', 'Registros', 'list', 'journal', 'Eventos, decisões e consequências confirmadas.'),
    screen(16, 'Mapa — Galáctico', 'Mapa', 'map', 'map', 'Zoom, arraste e proposta de destino; mapa não move o grupo.'),
    screen(17, 'Mapa — Missão', 'Mapa', 'map', 'location', 'Somente áreas e locais já revelados.'),
    screen(18, 'Criação — Identidade', 'Criação', 'wizard', 'character', 'Nome, retrato e apresentação permitida.'),
    screen(19, 'Criação — Espécie', 'Criação', 'wizard', 'character', 'Espécies disponíveis para o perfil de criação atual.'),
    screen(20, 'Criação — Carreira', 'Criação', 'wizard', 'character', 'Carreiras disponíveis para o perfil de criação atual.'),
    screen(21, 'Criação — Especialização', 'Criação', 'wizard', 'star', 'Especializações disponíveis para o perfil de criação atual.'),
    screen(22, 'Criação — Perícias', 'Criação', 'wizard', 'datapad', 'Perícias disponíveis para o perfil de criação atual.'),
    screen(23, 'Criação — Características', 'Criação', 'wizard', 'datapad', 'Características e orçamento do perfil de criação atual.'),
    screen(24, 'Criação — Vínculo de campanha', 'Criação', 'wizard', 'objective', 'Vínculo de campanha quando aplicável ao perfil atual.'),
    screen(25, 'Criação — Motivação', 'Criação', 'wizard', 'objective', 'Opções de motivação do perfil de criação atual.'),
    screen(26, 'Criação — Equipamento / orçamento', 'Criação', 'wizard', 'inventory', 'Equipamento e orçamento do perfil de criação; aquisição depende de resolução confirmada pelo Mestre.'),
    screen(27, 'Ajustes — Interface / acessibilidade', 'Ajustes', 'settings', 'settings', 'Preferências visuais e de acessibilidade da interface.'),
    screen(28, 'Ajustes — Narração / voz', 'Ajustes', 'settings', 'audio', 'Configuração e estado de narração, voz e áudio.'),
    screen(29, 'Ajustes — Gemini / provedor', 'Ajustes', 'settings', 'datapad', 'Configuração e estado do provedor Gemini.'),
    screen(30, 'Ajustes — Dados / diagnóstico', 'Ajustes', 'settings', 'save', 'Persistência, biblioteca e informações de diagnóstico disponíveis.'),
];
exports.mainNavigation = [
    { route: '00', label: 'CENA', icon: 'scene' },
    { route: '06', label: 'PERSONAGEM', icon: 'character' },
    { route: '08', label: 'ITENS', icon: 'items' },
    { route: '10', label: 'NAVE', icon: 'ship' },
    { route: '12', label: 'REGISTROS', icon: 'records' },
];
exports.utilityNavigation = [
    { route: '16', label: 'MAPA', icon: 'map' },
    { route: '30', label: 'BIBLIOTECA', icon: 'records' },
    { route: '27', label: 'AJUSTES', icon: 'settings' },
];
function getScreen(route) {
    return exports.screenRegistry.find((item) => item.route === route) ?? exports.screenRegistry[0];
}
function getScreenById(id) {
    return exports.screenRegistry.find((item) => item.id === id) ?? exports.screenRegistry[0];
}
function screensForDomain(domain) {
    return exports.screenRegistry.filter((item) => item.domain === domain);
}

});
define("r21Projection",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.EMPTY_R21_READ_MODEL = exports.R21_STATE_SCHEMA = exports.R21_STATE_CHANGED_EVENT = exports.R21_STORAGE_KEY = void 0;
exports.projectR21CampaignState = projectR21CampaignState;
exports.readR21CampaignState = readR21CampaignState;
exports.R21_STORAGE_KEY = 'braseiro.starwars.edge.state.v1';
exports.R21_STATE_CHANGED_EVENT = 'braseiro:r21-state-changed';
exports.R21_STATE_SCHEMA = 14;
exports.EMPTY_R21_READ_MODEL = Object.freeze({
    status: 'missing',
    scene: null,
    character: null,
    inventory: null,
    vehicle: null,
    records: null,
});
const CHARACTERISTIC_LABELS_PT_BR = {
    fortaleza: 'Fortaleza', agilidad: 'Agilidade', intelecto: 'Intelecto', astucia: 'Astúcia', voluntad: 'Vontade', presencia: 'Presença',
};
const SKILL_LABELS_PT_BR = {
    actividad_criminal: 'Atividade criminosa', aguante: 'Resistência', alerta: 'Alerta', astronavegacion: 'Astronavegação', atletismo: 'Atletismo',
    callejeo: 'Vida nas ruas', carisma: 'Carisma', coaccion: 'Coerção', computadoras: 'Computadores', conocimientos: 'Conhecimentos',
    coordinacion: 'Coordenação', disciplina: 'Disciplina', engano: 'Enganação', frialdad: 'Frieza', liderazgo: 'Liderança', mecanica: 'Mecânica',
    medicina: 'Medicina', negociacion: 'Negociação', percepcion: 'Percepção', pilotar: 'Pilotagem', pilotar_vehiculo_espacial: 'Pilotagem espacial',
    pilotar_vehiculo_planetario: 'Pilotagem planetária', sigilo: 'Furtividade', supervivencia: 'Sobrevivência',
    armas_distancia_ligeras: 'Armas de distância leves', armas_distancia_pesadas: 'Armas de distância pesadas', armas_cuerpo_a_cuerpo: 'Armas corpo a corpo',
    artilleria: 'Artilharia', pelea: 'Briga', bajos_fondos: 'Submundo', borde_exterior: 'Orla Exterior', educacion: 'Educação',
    estrategia_militar: 'Estratégia militar', mundos_del_nucleo: 'Mundos do Núcleo', saber_arcaico: 'Conhecimento arcaico', xenologia: 'Xenologia',
};
function record(value) {
    return value !== null && typeof value === 'object' && !Array.isArray(value) ? value : null;
}
function array(value) {
    return Array.isArray(value) ? value : [];
}
function text(value) {
    return typeof value === 'string' ? value.trim() : '';
}
function integer(value) {
    return Number.isInteger(value) ? Number(value) : null;
}
function display(value) {
    const s = text(value);
    return s || '—';
}
function listText(value) {
    const direct = text(value);
    if (direct)
        return direct;
    const item = record(value);
    if (!item)
        return '';
    return text(item.text) || text(item.name) || text(item.type) || text(item.description);
}
function quantityMeta(value) {
    const q = integer(value.quantity);
    if (q === null)
        return undefined;
    if (q === 0)
        return 'Esgotado';
    return q > 1 ? `x${q}` : undefined;
}
function itemProjection(value) {
    const direct = text(value);
    if (direct)
        return { title: direct };
    const item = record(value);
    if (!item)
        return null;
    const title = text(item.name) || text(item.itemName) || text(item.title);
    if (!title)
        return null;
    return { title, meta: quantityMeta(item) };
}
const INVENTORY_CATEGORY_LABEL = {
    weapon: 'Arma', armor: 'Armadura', gear: 'Equipamento',
};
function explicitSourceMeta(item) {
    const catalogId = text(item.sourceCatalogId);
    if (catalogId)
        return `Catálogo auditado · ${catalogId}`;
    const source = record(item.source);
    if (!source)
        return null;
    const title = text(source.title) || text(source.name);
    const type = text(source.type);
    if (title && type)
        return `${title} · ${type}`;
    return title || type || null;
}
function inventoryItemProjection(value, category) {
    const direct = text(value);
    if (direct)
        return { title: direct, meta: INVENTORY_CATEGORY_LABEL[category] };
    const item = record(value);
    if (!item)
        return null;
    const title = text(item.name) || text(item.itemName) || text(item.title);
    if (!title)
        return null;
    const meta = [INVENTORY_CATEGORY_LABEL[category]];
    const quantity = integer(item.quantity);
    if (quantity !== null)
        meta.push(quantity === 0 ? 'Esgotado' : `Qtd. ${quantity}`);
    if (category === 'weapon') {
        const skill = text(item.skill);
        if (skill)
            meta.push(SKILL_LABELS_PT_BR[skill] ?? skill);
        const range = text(item.range);
        if (range)
            meta.push(`Alcance ${range}`);
        const damage = integer(item.damage);
        const damageBonus = integer(item.damageBonus);
        if (damage !== null)
            meta.push(`Dano ${damage}`);
        else if (damageBonus !== null)
            meta.push(`Dano Fortaleza ${damageBonus >= 0 ? '+' : ''}${damageBonus}`);
        const crit = integer(item.crit);
        if (crit !== null)
            meta.push(`Crítico ${crit}`);
        const qualities = array(item.qualities).map((entry) => text(entry)).filter(Boolean);
        if (qualities.length)
            meta.push(qualities.join(', '));
    }
    else if (category === 'armor') {
        const defense = integer(item.defense);
        const soak = integer(item.soak);
        if (defense !== null)
            meta.push(`Defesa ${defense}`);
        if (soak !== null)
            meta.push(`Proteção ${soak}`);
    }
    const source = explicitSourceMeta(item);
    if (source)
        meta.push(`Origem ${source}`);
    return { title, meta: meta.join(' · ') };
}
function projectCharacter(characterValue) {
    const c = record(characterValue);
    if (!c)
        return null;
    const derived = record(c.derived) ?? {};
    const wounds = record(derived.wounds) ?? {};
    const strain = record(derived.strain) ?? {};
    const xp = record(c.xp) ?? {};
    const specializations = array(c.specializations).map(listText).filter(Boolean);
    const motivations = array(c.motivations).map(listText).filter(Boolean);
    const obligations = array(c.obligations);
    const obligationTotal = obligations.reduce((sum, entry) => sum + (integer(record(entry)?.value) ?? 0), 0);
    const obligationTypes = obligations.map(entry => text(record(entry)?.type)).filter(Boolean);
    const characteristics = Object.entries(record(c.characteristics) ?? {})
        .filter(([, value]) => Number.isInteger(value))
        .map(([key, value]) => ({ label: CHARACTERISTIC_LABELS_PT_BR[key] ?? key, value: String(value) }));
    const trainedSkills = Object.entries(record(c.skills) ?? {})
        .filter(([, value]) => Number.isInteger(value) && Number(value) > 0)
        .sort((a, b) => Number(b[1]) - Number(a[1]) || (SKILL_LABELS_PT_BR[a[0]] ?? a[0]).localeCompare(SKILL_LABELS_PT_BR[b[0]] ?? b[0], 'pt-BR'))
        .map(([key, value]) => ({ label: SKILL_LABELS_PT_BR[key] ?? key, value: String(value) }));
    const totalXp = integer(xp.total);
    const availableXp = integer(xp.available);
    const spentXp = totalXp !== null && availableXp !== null ? Math.max(0, totalXp - availableXp) : null;
    return {
        identity: [
            { label: 'Nome', value: display(c.name) },
            { label: 'Espécie', value: display(c.species) },
            { label: 'Carreira', value: display(c.profession) },
            { label: 'Especialização', value: specializations.join(' · ') || '—' },
        ],
        state: [
            { label: 'Ferimentos', value: integer(wounds.current) !== null && integer(wounds.threshold) !== null ? `${integer(wounds.current)}/${integer(wounds.threshold)}` : '—' },
            { label: 'Tensão', value: integer(strain.current) !== null && integer(strain.threshold) !== null ? `${integer(strain.current)}/${integer(strain.threshold)}` : '—' },
            { label: 'Obligation', value: obligations.length ? `${obligationTotal}${obligationTypes.length ? ` · ${obligationTypes.join(' · ')}` : ''}` : '—' },
            { label: 'Motivação', value: motivations.join(' · ') || '—' },
        ],
        characteristics,
        trainedSkills,
        progression: [
            { label: 'PE disponível', value: availableXp === null ? '—' : String(availableXp) },
            { label: 'PE gasto', value: spentXp === null ? '—' : String(spentXp) },
            { label: 'Escolhas elegíveis', value: '—' },
            { label: 'Histórico de progressão', value: '—' },
        ],
    };
}
function projectInventory(state, characterValue) {
    const c = record(characterValue);
    const master = record(state.master) ?? {};
    if (!c && !record(master.pendingAcquisition))
        return null;
    const owned = [];
    for (const [category, values] of [
        ['weapon', array(c?.weapons)],
        ['armor', array(c?.armor)],
        ['gear', array(c?.gear)],
    ]) {
        for (const value of values) {
            const item = inventoryItemProjection(value, category);
            if (item)
                owned.push(item);
        }
    }
    const pendingRaw = record(master.pendingAcquisition);
    const pending = pendingRaw && (text(pendingRaw.itemName) || text(pendingRaw.itemId))
        ? {
            title: text(pendingRaw.itemName) || text(pendingRaw.itemId),
            meta: text(pendingRaw.status) === 'aguardando_mestre' ? 'Aguardando Mestre' : display(pendingRaw.status),
        }
        : null;
    return { credits: integer(c?.credits), owned, pending };
}
function defenseLabel(defenseValue) {
    const d = record(defenseValue);
    if (!d)
        return '—';
    const values = ['proa', 'babor', 'estribor', 'popa'].map((key) => integer(d[key]));
    return values.every((v) => v !== null) ? values.join(' / ') : '—';
}
function projectVehicle(state) {
    const runtime = record(state.vehicles);
    if (!runtime)
        return null;
    const activeId = text(runtime.activeVehicleId);
    const vehicles = record(runtime.vehicles) ?? {};
    const vehicle = activeId ? record(vehicles[activeId]) : null;
    if (!vehicle)
        return { active: false, name: null, summary: [], weapons: [], crew: [], criticalHits: [] };
    const speed = integer(vehicle.speed);
    const speedMax = integer(vehicle.speedMax);
    const hullDamage = integer(vehicle.hullDamage);
    const hullThreshold = integer(vehicle.hullThreshold);
    const systemStrain = integer(vehicle.systemStrain);
    const systemStrainThreshold = integer(vehicle.systemStrainThreshold);
    const weapons = array(vehicle.weapons).map(itemProjection).filter((v) => Boolean(v));
    const crew = array(vehicle.crew).map(itemProjection).filter((v) => Boolean(v));
    const criticalHits = array(vehicle.criticalHits).map((value, index) => {
        const hit = record(value);
        if (!hit)
            return null;
        return {
            title: text(hit.effect) || `Dano crítico ${index + 1}`,
            meta: hit.repaired === true ? 'Reparado' : 'Ativo',
        };
    }).filter((v) => Boolean(v));
    return {
        active: true,
        name: text(vehicle.shortName) || text(vehicle.name) || null,
        summary: [
            { label: 'Velocidade', value: speed !== null && speedMax !== null ? `${speed}/${speedMax}` : '—' },
            { label: 'Manejo', value: integer(vehicle.handling) === null ? '—' : String(integer(vehicle.handling)) },
            { label: 'Silhueta', value: integer(vehicle.silhouette) === null ? '—' : String(integer(vehicle.silhouette)) },
            { label: 'Blindagem', value: integer(vehicle.armor) === null ? '—' : String(integer(vehicle.armor)) },
            { label: 'Casco', value: hullDamage !== null && hullThreshold !== null ? `${hullDamage}/${hullThreshold}` : '—' },
            { label: 'Tensão de sistemas', value: systemStrain !== null && systemStrainThreshold !== null ? `${systemStrain}/${systemStrainThreshold}` : '—' },
            { label: 'Defesa', value: defenseLabel(vehicle.defense) },
            { label: 'Hiperimpulsor', value: vehicle.hyperdrive === true ? 'Sim' : vehicle.hyperdrive === false ? 'Não' : '—' },
        ],
        weapons,
        crew,
        criticalHits,
    };
}
function projectRecords(state) {
    const group = record(state.group) ?? {};
    const base = record(group.base) ?? {};
    const resources = array(group.resources).map(listText).filter(Boolean);
    const belongings = array(group.belongings).map(listText).filter(Boolean);
    const contacts = array(group.contacts).map(itemProjection).filter((v) => Boolean(v));
    const history = array(state.sessionHistory).map((value, index) => {
        const session = record(value);
        if (!session)
            return null;
        const number = integer(session.number) ?? index + 1;
        const summary = text(session.summary);
        return { title: `Sessão ${number}`, meta: summary || undefined };
    }).filter((v) => Boolean(v));
    return {
        // R21 não possui um campo canônico dedicado de objetivo da missão.
        mission: [],
        group: [
            { label: 'Base', value: display(base.name) },
            { label: 'Localização', value: display(base.location) },
            { label: 'Descrição', value: display(base.description) },
            { label: 'Obligation total', value: String(array(group.obligationEntries).reduce((sum,entry)=>sum+(Number(record(entry)?.value)||0),0)) },
            { label: 'Recursos', value: resources.join(' · ') || '—' },
            { label: 'Pertences', value: belongings.join(' · ') || '—' },
        ],
        contacts,
        history,
    };
}
function projectScene(state) {
    const scene = record(state.scene);
    const master = record(state.master) ?? {};
    if (!scene && !text(master.pendingQuestion))
        return null;
    return {
        title: text(scene?.title) || null,
        narration: text(scene?.narration) || null,
        question: text(master.pendingQuestion) || null,
    };
}
function projectReadyState(state) {
    const characterValue = record(state.player)?.character ?? null;
    return {
        status: 'ready',
        campaignName: text(state.name) || undefined,
        scene: projectScene(state),
        character: projectCharacter(characterValue),
        inventory: projectInventory(state, characterValue),
        vehicle: projectVehicle(state),
        records: projectRecords(state),
    };
}
function projectR21CampaignState(value) {
    const state = record(value);
    if (!state)
        return { ...exports.EMPTY_R21_READ_MODEL, status: 'unavailable', detail: 'Estado da campanha inválido.' };
    const schema = integer(state.schemaVersion);
    if (schema !== exports.R21_STATE_SCHEMA) {
        return { ...exports.EMPTY_R21_READ_MODEL, status: 'unsupported', detail: `Versão de estado não suportada pela interface: ${String(state.schemaVersion ?? 'desconhecida')}.` };
    }
    const player = record(state.player);
    const master = record(state.master);
    const vehicles = record(state.vehicles);
    const group = record(state.group);
    const combat = record(state.combat);
    const session = record(state.session);
    const world = record(state.world);
    const vehicleCombat = record(vehicles?.combat);
    const structurallyValid = Boolean(text(state.campaignId)
        && text(state.sessionId)
        && text(state.name)
        && text(state.rulesProfile)
        && player
        && master
        && vehicles
        && group
        && combat
        && session
        && world
        && vehicleCombat
        && typeof combat?.active === 'boolean'
        && typeof vehicleCombat?.active === 'boolean'
        && ['nao_iniciada', 'ativa', 'encerrada'].includes(text(session?.status))
        && Array.isArray(state.events)
        && Array.isArray(state.appliedActionIds)
        && Array.isArray(state.sessionHistory));
    if (!structurallyValid) {
        return { ...exports.EMPTY_R21_READ_MODEL, status: 'unavailable', detail: 'Estado da campanha incompleto ou corrompido.' };
    }
    return projectReadyState(state);
}
function readR21CampaignState(storage) {
    const source = storage ?? (typeof window !== 'undefined' ? window.localStorage : null);
    if (!source)
        return exports.EMPTY_R21_READ_MODEL;
    let raw = null;
    try {
        raw = source.getItem(exports.R21_STORAGE_KEY);
    }
    catch {
        return { ...exports.EMPTY_R21_READ_MODEL, status: 'unavailable', detail: 'Não foi possível ler o estado local da campanha.' };
    }
    if (!raw)
        return exports.EMPTY_R21_READ_MODEL;
    try {
        return projectR21CampaignState(JSON.parse(raw));
    }
    catch {
        return { ...exports.EMPTY_R21_READ_MODEL, status: 'unavailable', detail: 'O estado local da campanha não pôde ser interpretado.' };
    }
}

});
define("gameBridgeClient",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getBridgeAvailability = getBridgeAvailability;
exports.getDataBridgeCapabilities = getDataBridgeCapabilities;
exports.submitPlayerIntentThroughBridge = submitPlayerIntentThroughBridge;
exports.exportCharacterJsonThroughBridge = exportCharacterJsonThroughBridge;
exports.importCharacterJsonThroughBridge = importCharacterJsonThroughBridge;
exports.exportCampaignBackupThroughBridge = exportCampaignBackupThroughBridge;
exports.importCampaignBackupThroughBridge = importCampaignBackupThroughBridge;
exports.listSnapshotsThroughBridge = listSnapshotsThroughBridge;
exports.createSnapshotThroughBridge = createSnapshotThroughBridge;
exports.restoreSnapshotThroughBridge = restoreSnapshotThroughBridge;
exports.generateRandomCharacterThroughBridge = generateRandomCharacterThroughBridge;
exports.getCreationBridgeCapabilities = getCreationBridgeCapabilities;
exports.loadCreationCatalogThroughBridge = loadCreationCatalogThroughBridge;
exports.previewCharacterCreationThroughBridge = previewCharacterCreationThroughBridge;
exports.createCustomCharacterThroughBridge = createCustomCharacterThroughBridge;
exports.getEquipmentBridgeCapabilities = getEquipmentBridgeCapabilities;
exports.loadEquipmentCatalogThroughBridge = loadEquipmentCatalogThroughBridge;
exports.proposeWeaponAcquisitionThroughBridge = proposeWeaponAcquisitionThroughBridge;
exports.getImageLibraryBridgeCapabilities = getImageLibraryBridgeCapabilities;
exports.loadImageLibraryInfoThroughBridge = loadImageLibraryInfoThroughBridge;
exports.searchImageLibraryThroughBridge = searchImageLibraryThroughBridge;
exports.loadImageMapCatalogThroughBridge = loadImageMapCatalogThroughBridge;
exports.loadImageVisualEntityCatalogThroughBridge = loadImageVisualEntityCatalogThroughBridge;
exports.getImageAssetThroughBridge = getImageAssetThroughBridge;
exports.getImageAssetRuntimeThroughBridge = getImageAssetRuntimeThroughBridge;
exports.downloadBridgeJson = downloadBridgeJson;
function currentBridge() {
    if (typeof window === 'undefined')
        return undefined;
    return window.BraseiroGameBridge;
}
function isBridge(value) {
    return !!value && typeof value === 'object' && typeof value.submitPlayerIntent === 'function';
}
function bridgeOrError() {
    const bridge = currentBridge();
    if (bridge === undefined)
        return { ok: false, error: 'BRIDGE_UNAVAILABLE', message: 'Motor Barbara indisponível neste ambiente.' };
    if (!isBridge(bridge))
        return { ok: false, error: 'INVALID_BRIDGE', message: 'Ponte do motor incompatível com esta shell.' };
    return bridge;
}
function normalizeResult(result, invalidMessage) {
    if (!result || typeof result !== 'object') {
        return { ok: false, error: 'INVALID_BRIDGE', message: invalidMessage };
    }
    const candidate = result;
    if (candidate.ok === true) {
        if (!Object.prototype.hasOwnProperty.call(candidate, 'data')) {
            return { ok: false, error: 'INVALID_BRIDGE', message: invalidMessage };
        }
        return { ok: true, data: candidate.data };
    }
    if (candidate.ok === false && typeof candidate.error === 'string' && candidate.error.trim() && typeof candidate.message === 'string' && candidate.message.trim()) {
        return { ok: false, error: candidate.error, message: candidate.message };
    }
    return { ok: false, error: 'INVALID_BRIDGE', message: invalidMessage };
}
function isRecord(value) {
    return !!value && typeof value === 'object' && !Array.isArray(value);
}
function invalidPayload(message) {
    return { ok: false, error: 'INVALID_BRIDGE', message };
}
function hasValidSnapshotId(value) {
    if (!Object.prototype.hasOwnProperty.call(value, 'snapshotId'))
        return false;
    const snapshotId = value.snapshotId;
    return snapshotId === null || (typeof snapshotId === 'string' && !!snapshotId.trim());
}
function validateFilePayloadResult(result, message) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || typeof data.fileName !== 'string' || !data.fileName.trim() || typeof data.json !== 'string' || !data.json.trim()) {
        return invalidPayload(message);
    }
    try {
        JSON.parse(data.json);
    }
    catch {
        return invalidPayload(message);
    }
    return { ok: true, data: { fileName: data.fileName, json: data.json } };
}
function validateCharacterMutationResult(result, message) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || !isRecord(data.character) || !isRecord(data.state) || !hasValidSnapshotId(data))
        return invalidPayload(message);
    return result;
}
function validateCampaignImportResult(result, message) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || !isRecord(data.state) || !hasValidSnapshotId(data))
        return invalidPayload(message);
    return result;
}
function validateSnapshotItemResult(result, message) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data))
        return invalidPayload(message);
    const id = typeof data.id === 'string' ? data.id.trim() : '';
    const label = typeof data.label === 'string' ? data.label.trim() : '';
    const createdAt = typeof data.createdAt === 'string' ? data.createdAt.trim() : '';
    if (!id || !label || !createdAt)
        return invalidPayload(message);
    return { ok: true, data: { id, label, createdAt } };
}
function validateRestoreResult(result, message) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || !isRecord(data.state))
        return invalidPayload(message);
    return result;
}
function callOptional(method, args = []) {
    const bridge = bridgeOrError();
    if (!isBridge(bridge))
        return bridge;
    const fn = bridge[method];
    if (typeof fn !== 'function')
        return { ok: false, error: 'UNSUPPORTED', message: 'Esta capacidade exige uma versão compatível do Motor Barbara.' };
    try {
        return normalizeResult(fn.apply(bridge, args), 'Resposta inválida da ponte do motor.');
    }
    catch (error) {
        return { ok: false, error: 'ENGINE_REJECTED', message: error instanceof Error ? error.message : String(error ?? 'Falha no motor.') };
    }
}
function getBridgeAvailability() {
    const bridge = currentBridge();
    if (bridge === undefined)
        return 'unavailable';
    return isBridge(bridge) ? 'available' : 'invalid';
}
function getDataBridgeCapabilities() {
    const bridge = currentBridge();
    if (!isBridge(bridge))
        return { characterJson: false, campaignBackup: false, snapshots: false, randomCharacter: false };
    return {
        characterJson: typeof bridge.exportCharacterJson === 'function' && typeof bridge.importCharacterJson === 'function',
        campaignBackup: typeof bridge.exportCampaignBackup === 'function' && typeof bridge.importCampaignBackup === 'function',
        snapshots: typeof bridge.listSnapshots === 'function' && typeof bridge.createSnapshot === 'function' && typeof bridge.restoreSnapshot === 'function',
        randomCharacter: typeof bridge.generateRandomCharacter === 'function',
    };
}
async function submitPlayerIntentThroughBridge(text) {
    const intent = String(text ?? '').trim();
    if (!intent)
        return { ok: false, error: 'INVALID_INPUT', message: 'Digite uma ação.' };
    const bridge = bridgeOrError();
    if (!isBridge(bridge))
        return bridge;
    try {
        return normalizeResult(await bridge.submitPlayerIntent(intent), 'Resposta inválida do canal do Mestre.');
    }
    catch (error) {
        return { ok: false, error: 'ENGINE_REJECTED', message: error instanceof Error ? error.message : String(error ?? 'Falha ao enviar a ação.') };
    }
}
function exportCharacterJsonThroughBridge() {
    return validateFilePayloadResult(callOptional('exportCharacterJson'), 'Arquivo de ficha inválido retornado pelo motor.');
}
function validJsonInput(text, label) {
    const value = String(text ?? '').trim();
    if (!value)
        return { ok: false, error: 'INVALID_INPUT', message: `${label} está vazio.` };
    try {
        JSON.parse(value);
        return null;
    }
    catch {
        return { ok: false, error: 'INVALID_INPUT', message: `${label} não contém JSON válido.` };
    }
}
function importCharacterJsonThroughBridge(text) {
    const invalid = validJsonInput(text, 'O arquivo da ficha');
    if (invalid)
        return invalid;
    return validateCharacterMutationResult(callOptional('importCharacterJson', [text, { createSnapshot: true }]), 'Resultado de importação de ficha inválido retornado pelo motor.');
}
function exportCampaignBackupThroughBridge() {
    return validateFilePayloadResult(callOptional('exportCampaignBackup'), 'Arquivo de backup inválido retornado pelo motor.');
}
function importCampaignBackupThroughBridge(text) {
    const invalid = validJsonInput(text, 'O backup da campanha');
    if (invalid)
        return invalid;
    return validateCampaignImportResult(callOptional('importCampaignBackup', [text, { createSnapshot: true }]), 'Resultado de importação de backup inválido retornado pelo motor.');
}
function listSnapshotsThroughBridge() {
    const result = callOptional('listSnapshots');
    if (!result.ok)
        return result;
    if (!Array.isArray(result.data))
        return { ok: false, error: 'INVALID_BRIDGE', message: 'Lista de snapshots inválida.' };
    const items = [];
    const ids = new Set();
    for (const value of result.data) {
        if (!value || typeof value !== 'object')
            return { ok: false, error: 'INVALID_BRIDGE', message: 'Lista de snapshots inválida.' };
        const candidate = value;
        const id = typeof candidate.id === 'string' ? candidate.id.trim() : '';
        const label = typeof candidate.label === 'string' ? candidate.label.trim() : '';
        const createdAt = typeof candidate.createdAt === 'string' ? candidate.createdAt.trim() : '';
        if (!id || !label || !createdAt || ids.has(id))
            return { ok: false, error: 'INVALID_BRIDGE', message: 'Lista de snapshots inválida.' };
        ids.add(id);
        items.push({ id, label, createdAt });
    }
    return { ok: true, data: items };
}
function createSnapshotThroughBridge(label = 'Backup manual') {
    return validateSnapshotItemResult(callOptional('createSnapshot', [label]), 'Snapshot inválido retornado pelo motor.');
}
function restoreSnapshotThroughBridge(id) {
    return validateRestoreResult(callOptional('restoreSnapshot', [id]), 'Estado restaurado inválido retornado pelo motor.');
}
function generateRandomCharacterThroughBridge(options) {
    return validateCharacterMutationResult(callOptional('generateRandomCharacter', [options, { createSnapshot: true }]), 'Ficha aleatória inválida retornada pelo motor.');
}
function normalizeCatalogWeapon(value) {
    if (!isRecord(value))
        return null;
    const id = typeof value.id === 'string' ? value.id.trim() : '';
    const name = typeof value.name === 'string' ? value.name.trim() : '';
    const price = Number(value.price);
    const rarity = Number(value.rarity);
    const damageRaw = value.damage;
    const damageBonusRaw = value.damageBonus;
    const critRaw = value.crit;
    const range = typeof value.range === 'string' ? value.range.trim() : '';
    if (!id || !name || !Number.isInteger(price) || price < 0 || !Number.isInteger(rarity) || rarity < 0 || rarity > 10 || typeof value.restricted !== 'boolean' || !range)
        return null;
    const nullableInt = (raw) => raw === null || raw === undefined ? null : Number.isInteger(Number(raw)) ? Number(raw) : NaN;
    const damage = nullableInt(damageRaw);
    const damageBonus = nullableInt(damageBonusRaw);
    const crit = nullableInt(critRaw);
    if (Number.isNaN(damage) || Number.isNaN(damageBonus) || Number.isNaN(crit))
        return null;
    if (!Array.isArray(value.qualities) || value.qualities.some((item) => typeof item !== 'string'))
        return null;
    return { id, name, price, rarity, restricted: value.restricted, damage, damageBonus, crit, range, qualities: [...value.qualities] };
}
function validStringArray(value) {
    return Array.isArray(value) && value.every(item => typeof item === 'string' && !!item.trim());
}
function validateCreationCatalogResult(result) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || data.profile !== 'EOTE_CORE' || !Array.isArray(data.species) || !Array.isArray(data.careers) || !validStringArray(data.obligationTypes) || !isRecord(data.characteristicLabels) || !isRecord(data.skillLabels)) {
        return invalidPayload('Catálogo de criação inválido retornado pelo motor.');
    }
    const species = [];
    const speciesKeys = new Set();
    for (const raw of data.species) {
        if (!isRecord(raw) || typeof raw.key !== 'string' || !raw.key.trim() || speciesKeys.has(raw.key) || typeof raw.label !== 'string' || !raw.label.trim() || !isRecord(raw.characteristics))
            return invalidPayload('Catálogo de espécies inválido retornado pelo motor.');
        const characteristics = {};
        for (const [key, value] of Object.entries(raw.characteristics)) {
            if (!Number.isInteger(value) || Number(value) < 1 || Number(value) > 5)
                return invalidPayload('Características de espécie inválidas.');
            characteristics[key] = Number(value);
        }
        const numeric = ['woundBase', 'strainBase', 'xp', 'careerFreeCount', 'specializationFreeCount'];
        if (numeric.some(key => !Number.isInteger(raw[key]) || Number(raw[key]) < 0))
            return invalidPayload('Metadados de espécie inválidos.');
        const freeSkill = raw.freeSkill === null ? null : isRecord(raw.freeSkill) && typeof raw.freeSkill.skill === 'string' && raw.freeSkill.skill.trim() ? { skill: raw.freeSkill.skill } : undefined;
        if (freeSkill === undefined || !validStringArray(raw.freeTalents) || !validStringArray(raw.abilities))
            return invalidPayload('Metadados de espécie inválidos.');
        let speciesChoice = null;
        if (raw.speciesChoice !== null) {
            if (!isRecord(raw.speciesChoice) || (raw.speciesChoice.type !== 'oneSkill' && raw.speciesChoice.type !== 'twoNonCareer' && raw.speciesChoice.type !== 'gandRespiration'))
                return invalidPayload('Escolha de espécie inválida.');
            if (raw.speciesChoice.skills !== undefined && !validStringArray(raw.speciesChoice.skills))
                return invalidPayload('Escolha de espécie inválida.');
            if (raw.speciesChoice.variants !== undefined && !validStringArray(raw.speciesChoice.variants)) return invalidPayload('Escolha de espécie inválida.');
            speciesChoice = { type: raw.speciesChoice.type, ...(raw.speciesChoice.skills ? { skills: [...raw.speciesChoice.skills] } : {}), ...(raw.speciesChoice.variants ? { variants: [...raw.speciesChoice.variants] } : {}) };
        }
        speciesKeys.add(raw.key);
        species.push({ key: raw.key, label: raw.label, characteristics, woundBase: Number(raw.woundBase), strainBase: Number(raw.strainBase), xp: Number(raw.xp), careerFreeCount: Number(raw.careerFreeCount), specializationFreeCount: Number(raw.specializationFreeCount), freeSkill, freeTalents: [...raw.freeTalents], speciesChoice, abilities: [...raw.abilities] });
    }
    const careers = [];
    const careerKeys = new Set();
    for (const raw of data.careers) {
        if (!isRecord(raw) || typeof raw.key !== 'string' || !raw.key.trim() || careerKeys.has(raw.key) || typeof raw.label !== 'string' || !raw.label.trim() || !validStringArray(raw.skills) || !Array.isArray(raw.specializations))
            return invalidPayload('Catálogo de carreiras inválido retornado pelo motor.');
        const specializations = [];
        const specKeys = new Set();
        for (const spec of raw.specializations) {
            if (!isRecord(spec) || typeof spec.key !== 'string' || !spec.key.trim() || specKeys.has(spec.key) || typeof spec.label !== 'string' || !spec.label.trim() || !validStringArray(spec.skills))
                return invalidPayload('Catálogo de especializações inválido retornado pelo motor.');
            specKeys.add(spec.key);
            specializations.push({ key: spec.key, label: spec.label, skills: [...spec.skills] });
        }
        careerKeys.add(raw.key);
        careers.push({ key: raw.key, label: raw.label, skills: [...raw.skills], specializations });
    }
    const characteristicLabels = Object.fromEntries(Object.entries(data.characteristicLabels).filter((entry) => typeof entry[1] === 'string' && !!entry[1].trim()));
    const skillLabels = Object.fromEntries(Object.entries(data.skillLabels).filter((entry) => typeof entry[1] === 'string' && !!entry[1].trim()));
    if (!species.length || !careers.length || !Object.keys(characteristicLabels).length || !Object.keys(skillLabels).length)
        return invalidPayload('Catálogo de criação incompleto retornado pelo motor.');
    return { ok: true, data: { profile: 'EOTE_CORE', species, careers, obligationTypes: [...data.obligationTypes], characteristicLabels, skillLabels } };
}
function validateCreationPreviewResult(result) {
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data))
        return invalidPayload('Preview de criação inválido retornado pelo motor.');
    for (const key of ['xpBudget', 'characteristicCost', 'xpRemaining', 'credits', 'obligation', 'startingObligation', 'additionalObligation', 'speciesXpBonus'])
        if (typeof data[key] !== 'number' || !Number.isFinite(data[key]))
            return invalidPayload('Preview de criação inválido retornado pelo motor.');
    if (typeof data.obligationOfficial !== 'boolean' || !(data.obligationNote === null || typeof data.obligationNote === 'string'))
        return invalidPayload('Preview de criação inválido retornado pelo motor.');
    return { ok: true, data: data };
}
function getCreationBridgeCapabilities() {
    const bridge = currentBridge();
    if (!isBridge(bridge))
        return { catalog: false, preview: false, create: false };
    return { catalog: typeof bridge.creationCatalog === 'function', preview: typeof bridge.creationPreview === 'function', create: typeof bridge.createCustomCharacter === 'function' };
}
function loadCreationCatalogThroughBridge() { return validateCreationCatalogResult(callOptional('creationCatalog')); }
function previewCharacterCreationThroughBridge(options) { return validateCreationPreviewResult(callOptional('creationPreview', [options])); }
function createCustomCharacterThroughBridge(options) {
    if (!options.name?.trim() || !options.motivation?.trim())
        return { ok: false, error: 'INVALID_INPUT', message: 'Nome e motivação são obrigatórios.' };
    return validateCharacterMutationResult(callOptional('createCustomCharacter', [options, { createSnapshot: true }]), 'Resultado de criação manual inválido retornado pelo motor.');
}
function getEquipmentBridgeCapabilities() {
    const bridge = currentBridge();
    if (!isBridge(bridge))
        return { catalog: false, proposeWeapon: false };
    return { catalog: typeof bridge.equipmentCatalog === 'function', proposeWeapon: typeof bridge.proposeWeaponAcquisition === 'function' };
}
function loadEquipmentCatalogThroughBridge() {
    const result = callOptional('equipmentCatalog');
    if (!result.ok)
        return result;
    if (!isRecord(result.data) || !Array.isArray(result.data.weapons))
        return invalidPayload('Catálogo de equipamento inválido retornado pelo motor.');
    const weapons = [];
    const ids = new Set();
    for (const raw of result.data.weapons) {
        const item = normalizeCatalogWeapon(raw);
        if (!item || ids.has(item.id))
            return invalidPayload('Catálogo de armas inválido retornado pelo motor.');
        ids.add(item.id);
        weapons.push(item);
    }
    return { ok: true, data: { weapons } };
}
function proposeWeaponAcquisitionThroughBridge(itemId) {
    const id = String(itemId ?? '').trim();
    if (!id)
        return { ok: false, error: 'INVALID_INPUT', message: 'Selecione uma arma do catálogo auditado.' };
    const result = callOptional('proposeWeaponAcquisition', [id]);
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || !isRecord(data.state) || !isRecord(data.pendingAcquisition))
        return invalidPayload('Pedido de aquisição inválido retornado pelo motor.');
    const pending = data.pendingAcquisition;
    if (typeof pending.itemId !== 'string' || pending.itemId !== id || typeof pending.itemName !== 'string' || !pending.itemName.trim() || pending.status !== 'aguardando_mestre') {
        return invalidPayload('Pedido de aquisição inválido retornado pelo motor.');
    }
    return result;
}
function validateImageAssetValue(value) {
    if (!isRecord(value))
        return null;
    const assetId = typeof value.assetId === 'string' ? value.assetId.trim() : '';
    const title = typeof value.title === 'string' ? value.title.trim() : '';
    const category = typeof value.category === 'string' ? value.category.trim() : '';
    const sourceType = typeof value.sourceType === 'string' ? value.sourceType.trim() : '';
    if (!assetId || !title || !category || !sourceType || !Array.isArray(value.tags) || value.tags.some((tag) => typeof tag !== 'string'))
        return null;
    if (!(value.mechanicalBinding === null || isRecord(value.mechanicalBinding)))
        return null;
    const stringOpt = (raw) => typeof raw === 'string' && raw.trim() ? raw.trim() : undefined;
    const asset = {
        assetId, title, category, sourceType, tags: [...value.tags], mechanicalBinding: value.mechanicalBinding,
    };
    for (const [key, raw] of Object.entries(value)) {
        if (['subtype', 'mapKind', 'sourceName', 'sourceRef', 'sourcePageUrl', 'remoteUrl', 'cachePolicy', 'localPath', 'usage', 'publishedAt'].includes(key)) {
            const clean = stringOpt(raw);
            if (clean)
                asset[key] = clean;
        }
    }
    if (typeof value.remoteReady === 'boolean')
        asset.remoteReady = value.remoteReady;
    if (typeof value.offlineReady === 'boolean')
        asset.offlineReady = value.offlineReady;
    if (value.variants !== undefined) {
        if (!Array.isArray(value.variants))
            return null;
        const variants = [];
        for (const raw of value.variants) {
            if (!isRecord(raw) || typeof raw.variantId !== 'string' || !raw.variantId.trim() || typeof raw.remoteUrl !== 'string' || !raw.remoteUrl.trim())
                return null;
            variants.push({ variantId: raw.variantId.trim(), remoteUrl: raw.remoteUrl.trim(), ...(typeof raw.title === 'string' && raw.title.trim() ? { title: raw.title.trim() } : {}) });
        }
        asset.variants = variants;
    }
    if (sourceType === 'external_reference' && (!asset.sourcePageUrl || (asset.remoteReady && !asset.remoteUrl)))
        return null;
    if (sourceType !== 'external_reference' && !asset.localPath)
        return null;
    return asset;
}
function validateImageAssetListResult(result, message) {
    if (!result.ok)
        return result;
    if (!Array.isArray(result.data))
        return invalidPayload(message);
    const items = [];
    const ids = new Set();
    for (const raw of result.data) {
        const item = validateImageAssetValue(raw);
        if (!item || ids.has(item.assetId))
            return invalidPayload(message);
        ids.add(item.assetId);
        items.push(item);
    }
    return { ok: true, data: items };
}
function getImageLibraryBridgeCapabilities() {
    const bridge = currentBridge();
    if (!isBridge(bridge))
        return { info: false, search: false, maps: false, visuals: false, asset: false, runtime: false };
    return {
        info: typeof bridge.imageLibraryInfo === 'function', search: typeof bridge.searchImageLibrary === 'function', maps: typeof bridge.imageMapCatalog === 'function', visuals: typeof bridge.imageVisualEntityCatalog === 'function',
        asset: typeof bridge.imageAsset === 'function', runtime: typeof bridge.imageAssetRuntime === 'function',
    };
}
function loadImageLibraryInfoThroughBridge() {
    const result = callOptional('imageLibraryInfo');
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || typeof data.libraryId !== 'string' || !data.libraryId.trim() || !Number.isInteger(data.schemaVersion) || !isRecord(data.counts) || !isRecord(data.policy))
        return invalidPayload('Informações da biblioteca de imagens inválidas.');
    const counts = {};
    for (const [key, value] of Object.entries(data.counts)) {
        if (typeof value !== 'number' || !Number.isInteger(value) || value < 0)
            return invalidPayload('Contadores da biblioteca de imagens inválidos.');
        counts[key] = value;
    }
    return { ok: true, data: { libraryId: data.libraryId, schemaVersion: Number(data.schemaVersion), counts, policy: data.policy } };
}
function searchImageLibraryThroughBridge(criteria = {}) {
    return validateImageAssetListResult(callOptional('searchImageLibrary', [criteria]), 'Busca da biblioteca de imagens inválida.');
}
function loadImageMapCatalogThroughBridge(criteria = {}) {
    const result = validateImageAssetListResult(callOptional('imageMapCatalog', [criteria]), 'Catálogo de mapas inválido retornado pelo motor.');
    if (!result.ok)
        return result;
    if (result.data.some((item) => item.category !== 'map'))
        return invalidPayload('Catálogo de mapas contém asset de categoria incorreta.');
    return result;
}
function loadImageVisualEntityCatalogThroughBridge(criteria = {}) {
    const result = validateImageAssetListResult(callOptional('imageVisualEntityCatalog', [criteria]), 'Catálogo visual de NPCs/droides/criaturas/veículos inválido retornado pelo motor.');
    if (!result.ok)
        return result;
    const allowed = new Set(['portrait', 'droid', 'creature', 'vehicle']);
    if (result.data.some((item) => !allowed.has(item.category) || item.mechanicalBinding !== null))
        return invalidPayload('Catálogo visual contém asset incompatível ou vínculo mecânico implícito.');
    return result;
}
function getImageAssetThroughBridge(assetId) {
    const id = String(assetId ?? '').trim();
    if (!id)
        return { ok: false, error: 'INVALID_INPUT', message: 'Informe o assetId da imagem.' };
    const result = callOptional('imageAsset', [id]);
    if (!result.ok)
        return result;
    const asset = validateImageAssetValue(result.data);
    return asset ? { ok: true, data: asset } : invalidPayload('Asset de imagem inválido retornado pelo motor.');
}
function getImageAssetRuntimeThroughBridge(assetId) {
    const id = String(assetId ?? '').trim();
    if (!id)
        return { ok: false, error: 'INVALID_INPUT', message: 'Informe o assetId da imagem.' };
    const result = callOptional('imageAssetRuntime', [id]);
    if (!result.ok)
        return result;
    const data = result.data;
    if (!isRecord(data) || data.assetId !== id || (data.mode !== 'packaged' && data.mode !== 'external_cache_required') || typeof data.offlineReady !== 'boolean')
        return invalidPayload('Descritor de runtime de imagem inválido.');
    if (data.mode === 'packaged' && (typeof data.localPath !== 'string' || !data.localPath.trim()))
        return invalidPayload('Descritor de imagem local inválido.');
    if (data.mode === 'external_cache_required' && typeof data.remoteReady !== 'boolean')
        return invalidPayload('Descritor de imagem externa inválido.');
    return result;
}
function downloadBridgeJson(payload) {
    if (typeof window === 'undefined')
        return { ok: false, error: 'UNSUPPORTED', message: 'Download indisponível neste ambiente.' };
    const value = payload;
    if (!value || typeof value.fileName !== 'string' || !value.fileName.trim() || typeof value.json !== 'string' || !value.json.trim()) {
        return { ok: false, error: 'INVALID_BRIDGE', message: 'Arquivo retornado pelo motor é inválido.' };
    }
    try {
        JSON.parse(value.json);
    }
    catch {
        return { ok: false, error: 'INVALID_BRIDGE', message: 'O motor retornou um arquivo que não contém JSON válido.' };
    }
    try {
        const nativeFiles = window.BraseiroFiles;
        if (nativeFiles && typeof nativeFiles.saveJson === 'function') {
            const raw = nativeFiles.saveJson(value.fileName, value.json);
            const saved = typeof raw === 'string' ? JSON.parse(raw) : raw;
            if (!saved || saved.ok !== true) return { ok: false, error: saved?.error || 'SAVE_FAILED', message: saved?.message || 'Falha ao salvar arquivo no Android.' };
            return { ok: true, data: { fileName: saved.fileName || value.fileName, location: saved.location || null, uri: saved.uri || null, path: saved.path || null } };
        }
        const blob = new Blob([value.json], { type: 'application/json;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = value.fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(url);
        return { ok: true, data: { fileName: value.fileName } };
    }
    catch (error) {
        return { ok: false, error: 'ENGINE_REJECTED', message: error instanceof Error ? error.message : 'Falha ao exportar arquivo.' };
    }
}

});
define("imageLibraryBridgeClient",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getImageLibraryNativeAvailability = getImageLibraryNativeAvailability;
exports.getImageLibraryNativeCapabilities = getImageLibraryNativeCapabilities;
exports.getImageCacheStatus = getImageCacheStatus;
exports.cacheExternalImage = cacheExternalImage;
exports.clearCachedImage = clearCachedImage;
exports.parseImageLibraryNativeEvent = parseImageLibraryNativeEvent;
exports.packagedImageUrl = packagedImageUrl;
exports.browserExternalFallbackUrl = browserExternalFallbackUrl;
function bridge() { return typeof window === 'undefined' ? undefined : window.BraseiroImages; }
function isBridge(value) {
    return !!value && typeof value === 'object' && typeof value.getStatus === 'function' && typeof value.cacheRemoteImage === 'function' && typeof value.clearCachedAsset === 'function';
}
function parse(raw) {
    if (typeof raw !== 'string' || !raw.trim())
        return null;
    try {
        const value = JSON.parse(raw);
        return value && typeof value === 'object' && !Array.isArray(value) ? value : null;
    }
    catch {
        return null;
    }
}
function normalizeId(value) { const id = String(value ?? '').trim(); return /^[A-Za-z0-9._:-]{1,180}$/.test(id) ? id : ''; }
function allowedRemoteUrl(value) {
    try {
        const url = new URL(String(value ?? ''));
        return url.protocol === 'https:' && url.hostname === 'kainrath.wordpress.com' && url.pathname.startsWith('/wp-content/uploads/') ? url.toString() : '';
    }
    catch {
        return '';
    }
}
function getImageLibraryNativeAvailability() {
    const value = bridge();
    if (value === undefined)
        return 'unavailable';
    return isBridge(value) ? 'available' : 'invalid';
}
function getImageLibraryNativeCapabilities() { const ok = isBridge(bridge()); return { status: ok, cache: ok, clear: ok }; }
function getImageCacheStatus(assetId) {
    const id = normalizeId(assetId);
    if (!id)
        return { ok: false, message: 'assetId de imagem inválido.' };
    const value = bridge();
    if (!isBridge(value))
        return { ok: false, message: 'Cache nativo de imagens indisponível neste ambiente.' };
    const raw = parse(value.getStatus(id));
    if (!raw || raw.ok !== true || raw.assetId !== id || typeof raw.cached !== 'boolean')
        return { ok: false, message: 'Resposta inválida do cache nativo de imagens.' };
    const data = { assetId: id, cached: raw.cached };
    if (typeof raw.runtimeUrl === 'string' && raw.runtimeUrl.startsWith('https://braseiro.local/library-image/'))
        data.runtimeUrl = raw.runtimeUrl;
    if (typeof raw.sourceUrl === 'string')
        data.sourceUrl = raw.sourceUrl;
    if (typeof raw.mimeType === 'string')
        data.mimeType = raw.mimeType;
    if (typeof raw.bytes === 'number' && Number.isFinite(raw.bytes) && raw.bytes >= 0)
        data.bytes = raw.bytes;
    if (typeof raw.sha256 === 'string')
        data.sha256 = raw.sha256;
    if (typeof raw.cachedAt === 'string')
        data.cachedAt = raw.cachedAt;
    return { ok: true, data };
}
function cacheExternalImage(asset) {
    const id = normalizeId(asset?.assetId);
    if (!id)
        return { ok: false, message: 'assetId de imagem inválido.' };
    if (asset.sourceType !== 'external_reference' || asset.remoteReady !== true)
        return { ok: false, message: 'Este asset não possui arquivo externo aprovado para cache.' };
    const remote = allowedRemoteUrl(asset.remoteUrl);
    if (!remote)
        return { ok: false, message: 'A URL externa deste asset não está na lista permitida.' };
    const value = bridge();
    if (!isBridge(value))
        return { ok: false, message: 'Cache nativo de imagens indisponível neste ambiente.' };
    const raw = parse(value.cacheRemoteImage(id, remote));
    if (!raw || raw.ok !== true || typeof raw.status !== 'string')
        return { ok: false, message: typeof raw?.message === 'string' ? raw.message : 'Não foi possível iniciar o cache da imagem.' };
    return { ok: true, status: raw.status };
}
function clearCachedImage(assetId) {
    const id = normalizeId(assetId);
    if (!id)
        return { ok: false, message: 'assetId de imagem inválido.' };
    const value = bridge();
    if (!isBridge(value))
        return { ok: false, message: 'Cache nativo de imagens indisponível neste ambiente.' };
    const raw = parse(value.clearCachedAsset(id));
    return raw?.ok === true ? { ok: true } : { ok: false, message: typeof raw?.message === 'string' ? raw.message : 'Não foi possível remover a imagem do cache.' };
}
function parseImageLibraryNativeEvent(raw) {
    try {
        const decoded = JSON.parse(raw);
        if (!decoded || typeof decoded !== 'object' || Array.isArray(decoded) || typeof decoded.type !== 'string')
            return null;
        const data = decoded.data && typeof decoded.data === 'object' && !Array.isArray(decoded.data) ? decoded.data : {};
        return { type: decoded.type, data };
    }
    catch {
        return null;
    }
}
function packagedImageUrl(asset) {
    if (!asset.localPath)
        return null;
    const clean = asset.localPath.replace(/^\/+/, '');
    return `file:///android_asset/vtt/${clean}`;
}
function browserExternalFallbackUrl(asset) {
    return asset.sourceType === 'external_reference' && asset.remoteReady === true ? allowedRemoteUrl(asset.remoteUrl) || null : null;
}

});
define("Icon",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Icon = Icon;
const jsx_runtime_1 = require("react/jsx-runtime");
const paths = { back: 'M15 18l-6-6 6-6', close: 'M6 6l12 12M18 6L6 18', search: 'M11 4a7 7 0 1 0 0 14a7 7 0 0 0 0-14m5 12l4 4', scene: 'M4 5h16v14H4zM7 15l3-3 2 2 3-4 3 5', location: 'M12 21s6-5.2 6-11a6 6 0 1 0-12 0c0 5.8 6 11 6 11zm0-8a3 3 0 1 1 0-6 3 3 0 0 1 0 6z', ship: 'M3 13l7-3 2-6 2 6 7 3-7 2-2 5-2-5z', character: 'M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm-7 8c1-4 3-6 7-6s6 2 7 6', datapad: 'M6 3h12v18H6zM9 7h6M9 11h6M9 15h4', journal: 'M5 4h14v16H5zM8 8h8M8 12h8M8 16h5', inventory: 'M4 8h16v12H4zM8 8V5h8v3', items: 'M4 8h16v12H4zM8 8V5h8v3', combat: 'M6 18L18 6M8 6l10 10M6 6l12 12', party: 'M8 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm8 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 20c.5-4 2-6 5-6s4.5 2 5 6m-2 0c.5-4 2-6 5-6s4.5 2 5 6', mission: 'M5 4h14v16H5zM8 8h8M8 12h6', records: 'M5 4h14v16H5zM8 8h8M8 12h8M8 16h5', communication: 'M4 5h16v12H8l-4 3z', tts: 'M4 9v6h4l5 4V5L8 9zm12 1a4 4 0 0 1 0 4m2-6a7 7 0 0 1 0 8', settings: 'M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm0-5v3m0 12v3M3 12h3m12 0h3M5.6 5.6l2.1 2.1m8.6 8.6 2.1 2.1m0-12.8-2.1 2.1m-8.6 8.6-2.1 2.1', alert: 'M12 3l10 18H2zM12 9v5m0 3v1', medical: 'M9 3h6v6h6v6h-6v6H9v-6H3V9h6z' };
function Icon({ name, className = '' }) { const d = paths[name] || paths.datapad; return (0, jsx_runtime_1.jsx)("svg", { className: className, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: '1.8', strokeLinecap: 'round', strokeLinejoin: 'round', "aria-hidden": 'true', children: (0, jsx_runtime_1.jsx)("path", { d: d }) }); }

});
define("ReferenceIcon",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ReferenceIcon = ReferenceIcon;
const jsx_runtime_1 = require("react/jsx-runtime");
const Icon_1 = require("./Icon");
const assetAliases = {
    scene: 'scene', edit: 'scene', map: 'map', character: 'character', datapad: 'journal', journal: 'journal',
    inventory: 'inventory', items: 'inventory', weapon: 'combat', party: 'party', contacts: 'party', faction: 'party', mission: 'mission', rumor: 'mission', records: 'journal',
    communication: 'tts', settings: 'settings', audio: 'tts', tts: 'tts', save: 'journal', ship: 'ship',
    objective: 'target', npc: 'character', location: 'location', info: 'journal', alert: 'medical', danger: 'combat',
    dice: 'dice', range: 'target', star: 'target', time: 'target',
};
function ReferenceIcon({ name, className = '' }) {
    const asset = assetAliases[name];
    if (!asset)
        return (0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: name, className: className });
    const source = `assets/ui/icons-v2/${asset}.png`;
    return (0, jsx_runtime_1.jsx)("span", { className: `reference-icon reference-icon-mask ${className}`, style: { '--ref-image': `url(${source})` }, "aria-hidden": "true" });
}

});
define("primitives",function(module,exports,require){
'use client';
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TopBar = exports.AppShell = exports.NarrativePanel = exports.ExpandableCard = exports.MissingImage = exports.PartialState = exports.OfflineState = exports.ErrorState = exports.EmptyState = exports.LoadingState = exports.Toast = exports.Tooltip = exports.Dialog = exports.Drawer = exports.SegmentedControl = exports.ShipListItem = exports.LocationListItem = exports.RumorListItem = exports.QuestListItem = exports.NPCListItem = exports.EquipmentItem = exports.ArmorItem = exports.WeaponItem = exports.InventoryItem = exports.ShipPortrait = exports.LocationPortrait = exports.NPCPortrait = exports.CharacterPortrait = exports.ContextAction = exports.SecondaryAction = exports.PrimaryAction = exports.DataChip = exports.StatusChip = exports.SectionHeader = exports.AlertPanel = exports.DataPanel = exports.Card = exports.ScreenContainer = void 0;
exports.ScreenHeader = ScreenHeader;
exports.CompactCard = CompactCard;
exports.ActionButton = ActionButton;
exports.IconButton = IconButton;
exports.Portrait = Portrait;
exports.ListItem = ListItem;
exports.SearchField = SearchField;
exports.Tabs = Tabs;
exports.Modal = Modal;
exports.BottomSheet = BottomSheet;
exports.Skeleton = Skeleton;
exports.BottomNavButton = BottomNavButton;
exports.BottomNavigation = BottomNavigation;
const jsx_runtime_1 = require("react/jsx-runtime");
const Icon_1 = require("./Icon");
const ReferenceIcon_1 = require("./ReferenceIcon");
const ScreenContainer = ({ children, className = '' }) => (0, jsx_runtime_1.jsx)("main", { className: `screen-container ${className}`, children: children });
exports.ScreenContainer = ScreenContainer;
function ScreenHeader({ eyebrow, title, detail, onBack }) { return (0, jsx_runtime_1.jsxs)("header", { className: "screen-header", children: [onBack && (0, jsx_runtime_1.jsx)(IconButton, { icon: "back", label: "Voltar", onClick: onBack }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("span", { className: "eyebrow", children: eyebrow }), (0, jsx_runtime_1.jsx)("h1", { children: title }), detail && (0, jsx_runtime_1.jsx)("p", { children: detail })] })] }); }
const Card = ({ children, className = '' }) => (0, jsx_runtime_1.jsx)("section", { className: `card ${className}`, children: children });
exports.Card = Card;
function CompactCard({ children, onClick }) {
    const content = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [children, onClick && (0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: "back", className: "chevron" })] });
    return onClick
        ? (0, jsx_runtime_1.jsx)("button", { type: "button", className: "compact-card", onClick: onClick, children: content })
        : (0, jsx_runtime_1.jsx)("div", { className: "compact-card compact-card-static", children: content });
}
exports.DataPanel = exports.Card;
const AlertPanel = ({ children, tone = 'amber' }) => (0, jsx_runtime_1.jsx)("aside", { className: `alert-panel ${tone}`, children: children });
exports.AlertPanel = AlertPanel;
const SectionHeader = ({ title, aside }) => (0, jsx_runtime_1.jsxs)("div", { className: "section-header", children: [(0, jsx_runtime_1.jsx)("h2", { children: title }), aside && (0, jsx_runtime_1.jsx)("span", { children: aside })] });
exports.SectionHeader = SectionHeader;
const StatusChip = ({ children, tone = 'neutral' }) => (0, jsx_runtime_1.jsx)("span", { className: `chip ${tone}`, children: children });
exports.StatusChip = StatusChip;
exports.DataChip = exports.StatusChip;
function ActionButton({ variant = 'secondary', icon, children, type = 'button', ...props }) { return (0, jsx_runtime_1.jsxs)("button", { type: type, className: `action ${variant}`, ...props, children: [icon && (0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: icon }), (0, jsx_runtime_1.jsx)("span", { children: children })] }); }
const PrimaryAction = (props) => (0, jsx_runtime_1.jsx)(ActionButton, { variant: "primary", ...props });
exports.PrimaryAction = PrimaryAction;
const SecondaryAction = (props) => (0, jsx_runtime_1.jsx)(ActionButton, { variant: "secondary", ...props });
exports.SecondaryAction = SecondaryAction;
const ContextAction = (props) => (0, jsx_runtime_1.jsx)(ActionButton, { variant: "context", ...props });
exports.ContextAction = ContextAction;
function IconButton({ icon, label, type = 'button', ...props }) { return (0, jsx_runtime_1.jsx)("button", { type: type, className: "icon-button", "aria-label": label, title: label, ...props, children: (0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: icon }) }); }
function Portrait({ name, kind = 'character' }) { return (0, jsx_runtime_1.jsxs)("figure", { className: `portrait ${kind}`, "aria-label": `Retrato de referência visual de ${name}`, children: [(0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: kind === 'location' ? 'location' : kind }), (0, jsx_runtime_1.jsx)("figcaption", { children: name.split(' ').map((part) => part[0]).join('').slice(0, 2) })] }); }
exports.CharacterPortrait = Portrait;
exports.NPCPortrait = Portrait;
exports.LocationPortrait = Portrait;
exports.ShipPortrait = Portrait;
function ListItem({ icon = 'datapad', title, meta, text, onClick }) {
    const content = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("span", { className: "list-icon", children: (0, jsx_runtime_1.jsx)(ReferenceIcon_1.ReferenceIcon, { name: icon }) }), (0, jsx_runtime_1.jsxs)("span", { className: "list-copy", children: [(0, jsx_runtime_1.jsx)("strong", { children: title }), meta && (0, jsx_runtime_1.jsx)("small", { children: meta }), text && (0, jsx_runtime_1.jsx)("span", { children: text })] }), onClick && (0, jsx_runtime_1.jsx)("span", { className: "list-arrow", "aria-hidden": "true", children: "\u203A" })] });
    return onClick
        ? (0, jsx_runtime_1.jsx)("button", { type: "button", className: "list-item", onClick: onClick, children: content })
        : (0, jsx_runtime_1.jsx)("div", { className: "list-item list-item-static", children: content });
}
exports.InventoryItem = ListItem;
exports.WeaponItem = ListItem;
exports.ArmorItem = ListItem;
exports.EquipmentItem = ListItem;
exports.NPCListItem = ListItem;
exports.QuestListItem = ListItem;
exports.RumorListItem = ListItem;
exports.LocationListItem = ListItem;
exports.ShipListItem = ListItem;
function SearchField({ value, onChange, placeholder = 'Buscar' }) { return (0, jsx_runtime_1.jsxs)("label", { className: "search-field", children: [(0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: "search" }), (0, jsx_runtime_1.jsx)("span", { className: "sr-only", children: placeholder }), (0, jsx_runtime_1.jsx)("input", { value: value, onChange: (event) => onChange(event.target.value), placeholder: placeholder })] }); }
function Tabs({ options, value, onChange }) { return (0, jsx_runtime_1.jsx)("div", { className: "tabs", role: "tablist", children: options.map((option) => (0, jsx_runtime_1.jsx)("button", { type: "button", role: "tab", "aria-selected": value === option, onClick: () => onChange(option), children: option }, option)) }); }
exports.SegmentedControl = Tabs;
function Modal({ open, title, children, onClose }) { if (!open)
    return null; return (0, jsx_runtime_1.jsx)("dialog", { open: true, className: "overlay", "aria-label": title, children: (0, jsx_runtime_1.jsxs)("section", { className: "modal", children: [(0, jsx_runtime_1.jsxs)("header", { children: [(0, jsx_runtime_1.jsx)("h2", { children: title }), (0, jsx_runtime_1.jsx)(IconButton, { icon: "close", label: "Fechar", onClick: onClose })] }), children] }) }); }
function BottomSheet({ open, title, children, onClose }) { if (!open)
    return null; return (0, jsx_runtime_1.jsx)("dialog", { open: true, className: "overlay", "aria-label": title, children: (0, jsx_runtime_1.jsxs)("section", { className: "bottom-sheet", children: [(0, jsx_runtime_1.jsx)("span", { className: "sheet-handle" }), (0, jsx_runtime_1.jsxs)("header", { children: [(0, jsx_runtime_1.jsx)("h2", { children: title }), (0, jsx_runtime_1.jsx)(IconButton, { icon: "close", label: "Fechar", onClick: onClose })] }), children] }) }); }
exports.Drawer = BottomSheet;
exports.Dialog = Modal;
const Tooltip = ({ label, children }) => (0, jsx_runtime_1.jsx)("span", { className: "tooltip", "data-tip": label, children: children });
exports.Tooltip = Tooltip;
const Toast = ({ children }) => (0, jsx_runtime_1.jsx)("output", { className: "toast", children: children });
exports.Toast = Toast;
function Skeleton({ lines = 4 }) { return (0, jsx_runtime_1.jsx)("div", { className: "skeleton", "aria-label": "Estrutura visual de QA", children: Array.from({ length: lines }, (_, i) => (0, jsx_runtime_1.jsx)("span", { style: { width: `${92 - i * 9}%` } }, i)) }); }
const LoadingState = () => (0, jsx_runtime_1.jsxs)("div", { className: "state-panel", children: [(0, jsx_runtime_1.jsx)("span", { className: "spinner" }), (0, jsx_runtime_1.jsx)("h2", { children: "Carregando interface" }), (0, jsx_runtime_1.jsx)("p", { children: "Estado visual de QA; n\u00E3o indica sincroniza\u00E7\u00E3o ou servi\u00E7o externo real." })] });
exports.LoadingState = LoadingState;
const EmptyState = () => (0, jsx_runtime_1.jsxs)("div", { className: "state-panel", children: [(0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: "datapad" }), (0, jsx_runtime_1.jsx)("h2", { children: "Vazio \u00B7 estado de QA" }), (0, jsx_runtime_1.jsx)("p", { children: "Este estado visual n\u00E3o afirma aus\u00EAncia real de dados da campanha." })] });
exports.EmptyState = EmptyState;
const ErrorState = () => (0, jsx_runtime_1.jsxs)("div", { className: "state-panel danger", children: [(0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: "alert" }), (0, jsx_runtime_1.jsx)("h2", { children: "Erro \u00B7 estado de QA" }), (0, jsx_runtime_1.jsx)("p", { children: "Este estado visual n\u00E3o comprova falha real de carregamento ou conex\u00E3o." })] });
exports.ErrorState = ErrorState;
const OfflineState = () => (0, jsx_runtime_1.jsxs)("div", { className: "state-panel", children: [(0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: "communication" }), (0, jsx_runtime_1.jsx)("h2", { children: "Offline \u00B7 estado de QA" }), (0, jsx_runtime_1.jsx)("p", { children: "Este estado visual n\u00E3o comprova funcionamento offline real." })] });
exports.OfflineState = OfflineState;
const PartialState = () => (0, jsx_runtime_1.jsxs)(exports.AlertPanel, { tone: "cyan", children: [(0, jsx_runtime_1.jsx)("strong", { children: "Conte\u00FAdo parcial" }), (0, jsx_runtime_1.jsx)("p", { children: "Estado de QA com dados incompletos; n\u00E3o implica sincroniza\u00E7\u00E3o ativa." })] });
exports.PartialState = PartialState;
const MissingImage = ({ label = 'Imagem indisponível' }) => (0, jsx_runtime_1.jsxs)("div", { className: "missing-image", children: [(0, jsx_runtime_1.jsx)(Icon_1.Icon, { name: "scene" }), (0, jsx_runtime_1.jsx)("span", { children: label })] });
exports.MissingImage = MissingImage;
const ExpandableCard = ({ title, children }) => (0, jsx_runtime_1.jsxs)("details", { className: "expandable", children: [(0, jsx_runtime_1.jsxs)("summary", { children: [title, (0, jsx_runtime_1.jsx)("span", { children: "+" })] }), (0, jsx_runtime_1.jsx)("div", { children: children })] });
exports.ExpandableCard = ExpandableCard;
const NarrativePanel = ({ children }) => (0, jsx_runtime_1.jsx)(exports.Card, { className: "narrative-panel", children: children });
exports.NarrativePanel = NarrativePanel;
const AppShell = ({ children, className = '' }) => (0, jsx_runtime_1.jsx)("div", { className: `app-shell ${className}`.trim(), children: children });
exports.AppShell = AppShell;
const TopBar = ({ onStatus }) => (0, jsx_runtime_1.jsxs)("div", { className: "top-bar", children: [(0, jsx_runtime_1.jsxs)("button", { type: "button", className: "top-bar-menu", "aria-label": "Abrir utilidades", onClick: onStatus, children: [(0, jsx_runtime_1.jsx)("span", {}), (0, jsx_runtime_1.jsx)("span", {}), (0, jsx_runtime_1.jsx)("span", {})] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "BRASEIRO" }), (0, jsx_runtime_1.jsx)("small", { children: "STAR WARS \u00B7 EDGE OF THE GALAXY" })] })] });
exports.TopBar = TopBar;
function BottomNavButton({ label, icon, active, onClick }) { return (0, jsx_runtime_1.jsxs)("button", { type: "button", className: "bottom-nav-button", "aria-current": active ? 'page' : undefined, onClick: onClick, children: [(0, jsx_runtime_1.jsx)(ReferenceIcon_1.ReferenceIcon, { name: icon === 'more' ? 'settings' : icon }), (0, jsx_runtime_1.jsx)("span", { children: label })] }); }
function BottomNavigation({ items, current, onNavigate }) { return (0, jsx_runtime_1.jsx)("nav", { className: "bottom-navigation", "aria-label": "Navega\u00E7\u00E3o principal", children: items.map((item) => (0, jsx_runtime_1.jsx)(BottomNavButton, { ...item, active: current === item.route, onClick: () => onNavigate(item.route) }, item.route)) }); }

});
define("DebugGallery",function(module,exports,require){
'use client';
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DebugGallery = DebugGallery;
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const screens_1 = require("@/src/config/screens");
const ReferenceIcon_1 = require("./ReferenceIcon");
const primitives_1 = require("./primitives");
const templateLabels = {
    scene: 'Cena',
    detail: 'Detalhe',
    list: 'Lista',
    map: 'Mapa',
    character: 'Personagem',
    settings: 'Ajustes',
    wizard: 'Fluxo guiado',
    utility: 'Utilidade',
    combat: 'Combate legado',
    debug: 'Diagnóstico',
    boot: 'Inicialização',
};
function DebugGallery({ qaState, setQaState, navigate, }) {
    const [search, setSearch] = (0, react_1.useState)('');
    const shown = (0, react_1.useMemo)(() => {
        const query = search.trim().toLocaleLowerCase('pt-BR');
        if (!query)
            return screens_1.screenRegistry;
        return screens_1.screenRegistry.filter((screen) => `${screen.id} ${screen.title} ${screen.domain}`.toLocaleLowerCase('pt-BR').includes(query));
    }, [search]);
    return ((0, jsx_runtime_1.jsxs)(primitives_1.ScreenContainer, { className: "debug-screen", children: [(0, jsx_runtime_1.jsx)(primitives_1.ScreenHeader, { eyebrow: "DESENVOLVIMENTO \u00B7 QA R46", title: "Diagn\u00F3stico da interface", detail: "Os 31 estados usam o registry R46; este painel n\u00E3o cria conte\u00FAdo de campanha nem resolve regras.", onBack: () => navigate('00') }), (0, jsx_runtime_1.jsx)(primitives_1.SearchField, { value: search, onChange: setSearch, placeholder: "Buscar tela ou dom\u00EDnio" }), (0, jsx_runtime_1.jsxs)(primitives_1.Card, { children: [(0, jsx_runtime_1.jsxs)("label", { className: "debug-select", children: [(0, jsx_runtime_1.jsx)("span", { children: "Estado global de QA" }), (0, jsx_runtime_1.jsx)("select", { value: qaState, onChange: (event) => { if ((0, screens_1.isQaState)(event.target.value))
                                    setQaState(event.target.value); }, children: screens_1.qaStates.map((state) => (0, jsx_runtime_1.jsx)("option", { value: state, children: screens_1.qaStateLabels[state] }, state)) })] }), (0, jsx_runtime_1.jsxs)("div", { className: "viewport-tags", children: [(0, jsx_runtime_1.jsx)(primitives_1.StatusChip, { tone: "amber", children: "Alvo 415 \u00D7 915" }), (0, jsx_runtime_1.jsxs)(primitives_1.StatusChip, { children: [screens_1.screenRegistry.length, " estados R46"] }), (0, jsx_runtime_1.jsx)(primitives_1.StatusChip, { children: "PT-BR" })] })] }), (0, jsx_runtime_1.jsx)("div", { className: "debug-list", children: shown.map((screen) => ((0, jsx_runtime_1.jsxs)("button", { type: "button", onClick: () => navigate(screen.route), children: [(0, jsx_runtime_1.jsx)("span", { className: "list-icon", children: (0, jsx_runtime_1.jsx)(ReferenceIcon_1.ReferenceIcon, { name: screen.icon }) }), (0, jsx_runtime_1.jsxs)("span", { children: [(0, jsx_runtime_1.jsxs)("small", { children: [screen.id, " \u00B7 ", screen.domain] }), (0, jsx_runtime_1.jsx)("strong", { children: screen.title })] }), (0, jsx_runtime_1.jsx)("i", { children: templateLabels[screen.template] ?? 'Estrutura' })] }, screen.id))) })] }));
}

});
define("Templates",function(module,exports,require){
'use client';
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.renderTemplate = renderTemplate;
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const screens_1 = require("@/src/config/screens");
const ReferenceIcon_1 = require("./ReferenceIcon");
const libraryBridgeClient_1 = require("@/src/state/libraryBridgeClient");
const C = {
    spaceBlack: '#111315',
    lead: '#2B3034',
    durasteel: '#D5D8DC',
    oldGold: '#D49D42',
    rust: '#652514',
    paper: '#D8CDB8',
    paperInk: '#26211B',
    cyan: '#64A9BA',
    red: '#9D4132',
};
const page = {
    display: 'grid',
    gap: 12,
    padding: '12px 12px 96px',
    maxWidth: 560,
    margin: '0 auto',
    color: C.durasteel,
};
const panel = {
    border: `1px solid ${C.oldGold}66`,
    background: 'rgba(17,19,21,.90)',
    padding: 14,
    borderRadius: 8,
    boxShadow: '0 8px 24px rgba(0,0,0,.22)',
};
const softPanel = {
    ...panel,
    borderColor: '#596269',
    background: 'rgba(43,48,52,.76)',
};
const narrative = {
    ...panel,
    background: C.paper,
    color: C.paperInk,
    borderColor: '#8B7450',
    padding: 16,
};
const label = {
    textTransform: 'uppercase',
    letterSpacing: '.08em',
    fontSize: 13,
    fontWeight: 800,
    color: C.oldGold,
};
const button = {
    minHeight: 46,
    border: `1px solid ${C.oldGold}99`,
    background: 'rgba(43,48,52,.96)',
    color: C.durasteel,
    borderRadius: 7,
    padding: '10px 12px',
    fontWeight: 800,
    letterSpacing: '.025em',
};
const mutedButton = {
    ...button,
    borderColor: '#596269',
    color: '#A9B0B5',
};
function Header({ screen, qaState }) {
    return ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'flex', gap: 10, alignItems: 'center' }, children: [(0, jsx_runtime_1.jsx)("div", { className: "screen-domain-icon", style: { width: 42, height: 42, display: 'grid', placeItems: 'center', border: `1px solid ${C.oldGold}66`, borderRadius: 8 }, children: (0, jsx_runtime_1.jsx)(ReferenceIcon_1.ReferenceIcon, { name: screen.icon }) }), (0, jsx_runtime_1.jsxs)("div", { style: { minWidth: 0, flex: 1 }, children: [(0, jsx_runtime_1.jsx)("div", { style: label, children: screen.domain }), (0, jsx_runtime_1.jsx)("h1", { style: { margin: '2px 0 0', fontSize: 24, lineHeight: 1.05 }, children: screen.title.replace(`${screen.domain} — `, '') })] }), qaState !== 'default' && ((0, jsx_runtime_1.jsx)("span", { style: { ...label, border: `1px solid ${C.oldGold}`, padding: '6px 8px', borderRadius: 999 }, children: (screens_1.qaStateLabels[qaState] ?? qaState).toUpperCase() }))] }), (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#B8BEC2', fontSize: 16, lineHeight: 1.45 }, children: screen.description })] }));
}
function SiblingNav({ screen, navigate }) {
    const siblings = (0, screens_1.screensForDomain)(screen.domain);
    const activeRef = (0, react_1.useRef)(null);
    (0, react_1.useEffect)(() => {
        activeRef.current?.scrollIntoView({ block: 'nearest', inline: 'nearest' });
    }, [screen.route]);
    if (siblings.length < 2)
        return null;
    return ((0, jsx_runtime_1.jsx)("nav", { "aria-label": `Estados de ${screen.domain}`, style: { display: 'flex', gap: 7, overflowX: 'auto', paddingBottom: 2 }, children: siblings.map((item) => ((0, jsx_runtime_1.jsx)("button", { ref: item.route === screen.route ? activeRef : undefined, type: "button", onClick: () => navigate(item.route), style: {
                ...button,
                flex: '0 0 auto',
                minHeight: 44,
                padding: '7px 10px',
                background: item.route === screen.route ? C.rust : 'rgba(43,48,52,.9)',
            }, "aria-current": item.route === screen.route ? 'page' : undefined, children: item.title.replace(`${item.domain} — `, '') }, item.route))) }));
}
function Card({ title, children, tone = 'dark' }) {
    const style = tone === 'paper' ? narrative : tone === 'soft' ? softPanel : tone === 'warning' ? { ...panel, borderColor: C.red, background: '#251817' } : panel;
    return ((0, jsx_runtime_1.jsxs)("section", { style: { ...style, minWidth: 0 }, children: [(0, jsx_runtime_1.jsx)("div", { style: { ...label, overflowWrap: 'anywhere', wordBreak: 'break-word', color: tone === 'paper' ? '#684B25' : tone === 'warning' ? '#E5A28F' : C.oldGold }, children: title }), (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 8, minWidth: 0, overflowWrap: 'anywhere', fontSize: tone === 'paper' ? 18 : 16, lineHeight: tone === 'paper' ? 1.55 : 1.5 }, children: children })] }));
}
function DataField({ field }) {
    return ((0, jsx_runtime_1.jsxs)("div", { style: { ...softPanel, padding: 10, minHeight: 66 }, children: [(0, jsx_runtime_1.jsx)("div", { style: label, children: field.label }), (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 8, fontSize: 18, overflowWrap: 'anywhere' }, children: field.value || '—' })] }));
}
function DataGrid({ fields }) {
    const normalized = fields.map((field) => typeof field === 'string' ? { label: field, value: '—' } : field);
    return ((0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gridTemplateColumns: 'repeat(2,minmax(0,1fr))', gap: 8 }, children: normalized.map((field, index) => (0, jsx_runtime_1.jsx)(DataField, { field: field }, `${field.label}-${index}`)) }));
}
function DisplayList({ items, emptyText }) {
    if (!items.length)
        return (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: emptyText });
    return ((0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 8 }, children: items.map((item, index) => ((0, jsx_runtime_1.jsxs)("div", { style: { ...softPanel, padding: 10 }, children: [(0, jsx_runtime_1.jsx)("strong", { children: item.title }), item.meta && (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 4, color: '#A9B0B5', fontSize: 14 }, children: item.meta })] }, `${item.title}-${index}`))) }));
}
function meaningfulFields(fields) {
    return (fields ?? []).filter((field) => field.value.trim() !== '' && field.value !== '—');
}
function CampaignReadNotice({ model }) {
    if (model.status === 'ready')
        return null;
    if (model.status === 'missing')
        return (0, jsx_runtime_1.jsx)(Card, { title: "Campanha", tone: "soft", children: "Nenhuma campanha local carregada." });
    return (0, jsx_runtime_1.jsx)(Card, { title: "Estado da campanha", tone: "warning", children: model.detail || 'Não foi possível ler o estado local da campanha.' });
}
function SceneContextHeader({ screen, qaState, openMenu }) {
    const stateTitle = screen.title.replace('Cena — ', '');
    return ((0, jsx_runtime_1.jsxs)("header", { className: "scene-context-header", children: [(0, jsx_runtime_1.jsxs)("button", { type: "button", className: "scene-context-menu", "aria-label": "Abrir utilidades", onClick: openMenu, children: [(0, jsx_runtime_1.jsx)("span", {}), (0, jsx_runtime_1.jsx)("span", {}), (0, jsx_runtime_1.jsx)("span", {})] }), (0, jsx_runtime_1.jsx)("div", { className: "scene-context-title", "aria-label": `Cena — ${stateTitle}`, children: (0, jsx_runtime_1.jsx)("strong", { children: stateTitle }) }), (0, jsx_runtime_1.jsx)("span", { className: "scene-context-spacer", "aria-hidden": "true" })] }));
}
function SceneStatusGlyph({ kind }) {
    const paths = {
        hull: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("path", { d: "M12 3c3 3 5 7 5 11l3 3-4 1-1 3-3-4-3 4-1-3-4-1 3-3c0-4 2-8 5-11z" }), (0, jsx_runtime_1.jsx)("path", { d: "M9 13h6" })] }),
        strain: (0, jsx_runtime_1.jsx)("path", { d: "M2 13h4l2-6 3 12 3-9 2 3h6" }),
        speed: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("path", { d: "M4 16a8 8 0 0 1 16 0" }), (0, jsx_runtime_1.jsx)("path", { d: "m12 15 4-6" }), (0, jsx_runtime_1.jsx)("path", { d: "M7 15h.01M17 15h.01" })] }),
        defense: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("path", { d: "m12 3 7 3v5c0 5-3.5 8-7 10-3.5-2-7-5-7-10V6z" }), (0, jsx_runtime_1.jsx)("path", { d: "M12 7v9" })] }),
    };
    return (0, jsx_runtime_1.jsx)("svg", { className: "scene-status-glyph", viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: "1.8", strokeLinecap: "round", strokeLinejoin: "round", "aria-hidden": "true", children: paths[kind] });
}
function SceneVehicleStatusStrip({ qaState }) {
    const cells = [
        { label: 'Casco', kind: 'hull', tone: 'cyan' },
        { label: 'Tensão', kind: 'strain', tone: 'amber' },
        { label: 'Velocidade', kind: 'speed', tone: 'cyan' },
        { label: 'Defesa', kind: 'defense', tone: 'neutral' },
    ];
    return ((0, jsx_runtime_1.jsx)("section", { className: `scene-status-strip ${qaState === 'demo' ? 'is-demo' : ''}`, "aria-label": "Estado imediato da nave", children: cells.map((item) => ((0, jsx_runtime_1.jsxs)("div", { className: `scene-status-cell tone-${item.tone}`, children: [(0, jsx_runtime_1.jsx)(SceneStatusGlyph, { kind: item.kind }), (0, jsx_runtime_1.jsxs)("div", { className: "scene-status-copy", children: [(0, jsx_runtime_1.jsx)("span", { children: item.label }), (0, jsx_runtime_1.jsx)("span", { "aria-label": `${item.label}: valor não fornecido`, style: { color: '#82939A', fontSize: '.72rem', lineHeight: 1 }, children: "\u2014" })] })] }, item.label))) }));
}
function SceneMediaSlot({ qaState, sceneImageUrl }) {
    const qaVisual = qaState === 'demo';
    const hasImage = Boolean(sceneImageUrl);
    return ((0, jsx_runtime_1.jsxs)("figure", { className: `scene-media-slot ${hasImage ? 'has-scene-image' : 'is-empty'}`, children: [hasImage
                ? (0, jsx_runtime_1.jsx)("img", { className: "scene-media-image", src: sceneImageUrl ?? undefined, alt: "Imagem cinematogr\u00E1fica da cena atual" })
                : (0, jsx_runtime_1.jsx)("div", { className: "scene-media-surface", "aria-hidden": "true" }), !hasImage && (0, jsx_runtime_1.jsxs)("figcaption", { className: qaVisual ? 'is-demo-caption' : undefined, children: [(0, jsx_runtime_1.jsx)("span", { children: qaVisual ? 'QA VISUAL — SEM ASSET DE CENA' : 'IMAGEM DE CENA NÃO FORNECIDA' }), (0, jsx_runtime_1.jsx)("small", { children: qaVisual ? 'SEM CONTEÚDO FICTÍCIO' : 'AGUARDANDO IMAGEM' })] })] }));
}
function SceneSectionGlyph({ icon }) {
    const paths = {
        speaker: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("path", { d: "M4 10v4h4l5 4V6L8 10z" }), (0, jsx_runtime_1.jsx)("path", { d: "M16.5 9c1.7 1.8 1.7 4.2 0 6M19 6.5c3.4 3.2 3.4 7.8 0 11" })] }),
        target: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("circle", { cx: "12", cy: "12", r: "7" }), (0, jsx_runtime_1.jsx)("circle", { cx: "12", cy: "12", r: "2" }), (0, jsx_runtime_1.jsx)("path", { d: "M12 2v4M12 18v4M2 12h4M18 12h4" })] }),
        group: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("circle", { cx: "9", cy: "9", r: "3" }), (0, jsx_runtime_1.jsx)("circle", { cx: "17", cy: "10", r: "2" }), (0, jsx_runtime_1.jsx)("path", { d: "M3 20c.7-4 2.8-6 6-6s5.3 2 6 6M15 15c3.2 0 5 1.7 5.5 5" })] }),
        chat: (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("path", { d: "M4 5h16v11H9l-5 4z" }), (0, jsx_runtime_1.jsx)("path", { d: "M8 9h.01M12 9h.01M16 9h.01" })] }),
    };
    return (0, jsx_runtime_1.jsx)("svg", { className: "scene-heading-svg", viewBox: "0 0 24 24", fill: "none", stroke: "currentColor", strokeWidth: "1.8", strokeLinecap: "round", strokeLinejoin: "round", "aria-hidden": "true", children: paths[icon] });
}
function SceneSectionHeading({ title, tone = 'cyan', icon }) {
    return ((0, jsx_runtime_1.jsxs)("div", { className: `scene-section-heading ${tone}`, children: [(0, jsx_runtime_1.jsx)("span", { className: "scene-section-icon", "aria-hidden": "true", children: (0, jsx_runtime_1.jsx)(SceneSectionGlyph, { icon: icon }) }), (0, jsx_runtime_1.jsx)("h2", { children: title })] }));
}
function SceneNarrativePanel({ qaState, campaignRead }) {
    const [expanded, setExpanded] = (0, react_1.useState)(false);
    const expandButtonRef = (0, react_1.useRef)(null);
    const closeButtonRef = (0, react_1.useRef)(null);
    (0, react_1.useEffect)(() => {
        if (!expanded)
            return;
        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        closeButtonRef.current?.focus();
        const handleKeyDown = (event) => {
            if (event.key === 'Escape')
                setExpanded(false);
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => {
            document.body.style.overflow = previousOverflow;
            window.removeEventListener('keydown', handleKeyDown);
            requestAnimationFrame(() => expandButtonRef.current?.focus());
        };
    }, [expanded]);
    const sceneNarration = campaignRead.status === 'ready' ? campaignRead.scene?.narration : null;
    const sceneQuestion = campaignRead.status === 'ready' ? campaignRead.scene?.question : null;
    const narrativeContent = sceneNarration
        ? (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("p", { children: sceneNarration }), sceneQuestion && (0, jsx_runtime_1.jsx)("p", { children: sceneQuestion })] })
        : qaState === 'demo'
            ? (0, jsx_runtime_1.jsx)("p", { children: "QA VISUAL \u2014 aguardando conte\u00FAdo de narra\u00E7\u00E3o." })
            : (0, jsx_runtime_1.jsx)("p", { children: "Aguardando narra\u00E7\u00E3o do Mestre." });
    return ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsxs)("section", { className: "scene-paper-panel scene-narrative-paper", children: [(0, jsx_runtime_1.jsxs)("div", { className: "scene-narrative-heading-row", children: [(0, jsx_runtime_1.jsx)(SceneSectionHeading, { title: "Narra\u00E7\u00E3o do Mestre", tone: "rust", icon: "speaker" }), (0, jsx_runtime_1.jsx)("button", { ref: expandButtonRef, type: "button", className: "scene-narrative-expand-button", "aria-label": "Maximizar Narra\u00E7\u00E3o do Mestre", "aria-expanded": expanded, onClick: () => setExpanded(true), children: (0, jsx_runtime_1.jsx)("svg", { viewBox: "0 0 24 24", "aria-hidden": "true", children: (0, jsx_runtime_1.jsx)("path", { d: "M8 3H3v5M16 3h5v5M8 21H3v-5M21 16v5h-5" }) }) })] }), (0, jsx_runtime_1.jsx)("div", { className: "scene-paper-copy", children: narrativeContent })] }), expanded && ((0, jsx_runtime_1.jsx)("div", { className: "scene-narrative-reader-overlay", role: "dialog", "aria-modal": "true", "aria-label": "Leitura ampliada da Narra\u00E7\u00E3o do Mestre", children: (0, jsx_runtime_1.jsxs)("section", { className: "scene-paper-panel scene-narrative-paper scene-narrative-reader", children: [(0, jsx_runtime_1.jsxs)("div", { className: "scene-narrative-heading-row scene-reader-heading", children: [(0, jsx_runtime_1.jsx)(SceneSectionHeading, { title: "Narra\u00E7\u00E3o do Mestre", tone: "rust", icon: "speaker" }), (0, jsx_runtime_1.jsx)("button", { ref: closeButtonRef, type: "button", className: "scene-narrative-expand-button is-minimize", "aria-label": "Minimizar Narra\u00E7\u00E3o do Mestre", onClick: () => setExpanded(false), children: (0, jsx_runtime_1.jsx)("svg", { viewBox: "0 0 24 24", "aria-hidden": "true", children: (0, jsx_runtime_1.jsx)("path", { d: "M9 9H4V4M15 9h5V4M9 15H4v5M20 20v-5h-5" }) }) })] }), (0, jsx_runtime_1.jsx)("div", { className: "scene-paper-copy scene-reader-copy", tabIndex: 0, children: narrativeContent })] }) }))] }));
}
function SceneSituationAndParticipants() {
    return ((0, jsx_runtime_1.jsxs)("div", { className: "scene-situation-grid", children: [(0, jsx_runtime_1.jsxs)("section", { className: "scene-paper-panel scene-situation-paper", children: [(0, jsx_runtime_1.jsx)(SceneSectionHeading, { title: "Situa\u00E7\u00E3o imediata", tone: "rust", icon: "target" }), (0, jsx_runtime_1.jsx)("p", { children: "Aguardando contexto p\u00FAblico e j\u00E1 revelado." })] }), (0, jsx_runtime_1.jsxs)("section", { className: "scene-metal-panel scene-participants-panel", children: [(0, jsx_runtime_1.jsx)(SceneSectionHeading, { title: "Participantes vis\u00EDveis", icon: "group" }), (0, jsx_runtime_1.jsx)("div", { className: "scene-participant-strip", "aria-label": "Participantes vis\u00EDveis", children: Array.from({ length: 3 }, (_, index) => ((0, jsx_runtime_1.jsxs)("div", { className: "scene-participant-slot", children: [(0, jsx_runtime_1.jsx)("span", { className: "scene-participant-frame is-empty", "aria-hidden": "true", children: (0, jsx_runtime_1.jsx)("span", { className: "scene-participant-empty-mark" }) }), (0, jsx_runtime_1.jsx)("strong", { children: "\u2014" }), (0, jsx_runtime_1.jsx)("small", { children: "Aguardando participante" })] }, index))) })] })] }));
}
function SceneActionGrid() {
    return ((0, jsx_runtime_1.jsxs)("section", { className: "scene-metal-panel scene-actions-panel", children: [(0, jsx_runtime_1.jsx)(SceneSectionHeading, { title: "A\u00E7\u00F5es dispon\u00EDveis", icon: "target" }), (0, jsx_runtime_1.jsx)("div", { className: "scene-action-grid", "aria-label": "A\u00E7\u00F5es contextuais", children: Array.from({ length: 6 }, (_, index) => ((0, jsx_runtime_1.jsxs)("div", { className: "scene-action-slot is-empty", children: [(0, jsx_runtime_1.jsx)("span", { className: "scene-action-glyph", "aria-hidden": "true" }), (0, jsx_runtime_1.jsxs)("span", { className: "scene-action-copy", children: [(0, jsx_runtime_1.jsx)("strong", { children: "\u2014" }), (0, jsx_runtime_1.jsx)("small", { children: "Aguardando a\u00E7\u00E3o dispon\u00EDvel" })] }), (0, jsx_runtime_1.jsx)("span", { className: "scene-action-empty-end", "aria-hidden": "true" })] }, index))) })] }));
}
function SceneIntentComposer({ sceneDraft, setSceneDraft, masterBridge, campaignRead, intentSubmitState, intentSubmitMessage, submitSceneIntent, }) {
    const bridgeReady = masterBridge === 'available' && campaignRead.status === 'ready';
    const sending = intentSubmitState === 'sending';
    const canSend = bridgeReady && !sending && sceneDraft.trim().length > 0;
    const buttonLabel = sending ? 'Enviando…' : 'Propor ao Mestre';
    return ((0, jsx_runtime_1.jsxs)("section", { className: "scene-metal-panel scene-composer", children: [(0, jsx_runtime_1.jsx)(SceneSectionHeading, { title: "O que voc\u00EA faz?", icon: "chat" }), (0, jsx_runtime_1.jsxs)("div", { className: "scene-composer-row", children: [(0, jsx_runtime_1.jsx)("textarea", { value: sceneDraft, onChange: (event) => setSceneDraft(event.target.value), placeholder: "Descreva sua a\u00E7\u00E3o neste momento\u2026", rows: 2, "aria-label": "Rascunho da inten\u00E7\u00E3o do personagem", disabled: sending }), bridgeReady ? ((0, jsx_runtime_1.jsx)("button", { type: "button", className: "scene-submit-guard", disabled: !canSend, "aria-busy": sending || undefined, onClick: () => { void submitSceneIntent(); }, style: { appearance: 'none', fontFamily: 'inherit', cursor: canSend ? 'pointer' : 'default', opacity: canSend || sending ? 1 : .62 }, children: buttonLabel })) : ((0, jsx_runtime_1.jsx)("div", { role: "note", className: "scene-submit-guard", children: "Envio indispon\u00EDvel" }))] }), intentSubmitMessage && ((0, jsx_runtime_1.jsx)("div", { role: intentSubmitState === 'error' ? 'alert' : 'status', style: { marginTop: 6, color: intentSubmitState === 'error' ? '#E5A28F' : '#9CCBDA', fontSize: '.7rem', lineHeight: 1.25 }, children: intentSubmitMessage })), (0, jsx_runtime_1.jsx)("span", { className: "sr-only", children: bridgeReady ? 'Canal do Mestre disponível.' : 'Rascunho local. O envio ao Mestre está indisponível.' })] }));
}
function SceneScreen(props) {
    const { screen, qaState, sceneDraft, setSceneDraft, openMenu, campaignRead, masterBridge, intentSubmitState, intentSubmitMessage, submitSceneIntent } = props;
    const vehicleCombat = screen.route === '03';
    return ((0, jsx_runtime_1.jsxs)("main", { className: `scene-concept-shell scene-route-${screen.route}`, children: [(0, jsx_runtime_1.jsx)(SceneContextHeader, { screen: screen, qaState: qaState, openMenu: openMenu }), vehicleCombat && (0, jsx_runtime_1.jsx)(SceneVehicleStatusStrip, { qaState: qaState }), (0, jsx_runtime_1.jsx)(SceneMediaSlot, { qaState: qaState, sceneImageUrl: props.sceneImageUrl }), (0, jsx_runtime_1.jsx)(SceneNarrativePanel, { qaState: qaState, campaignRead: campaignRead }), (0, jsx_runtime_1.jsx)(SceneSituationAndParticipants, {}), (0, jsx_runtime_1.jsx)(SceneActionGrid, {}), (0, jsx_runtime_1.jsx)(SceneIntentComposer, { sceneDraft: sceneDraft, setSceneDraft: setSceneDraft, masterBridge: masterBridge, campaignRead: campaignRead, intentSubmitState: intentSubmitState, intentSubmitMessage: intentSubmitMessage, submitSceneIntent: submitSceneIntent })] }));
}
function DataActionNotice({ state, message }) {
    if (state === 'idle' || (!message && state !== 'working'))
        return null;
    const body = state === 'working' ? 'Processando…' : message;
    return (0, jsx_runtime_1.jsx)(Card, { title: "Dados", tone: state === 'error' ? 'warning' : 'soft', children: body });
}
function CharacterScreen(props) {
    const progress = props.screen.route === '07';
    const model = props.campaignRead;
    const character = model.status === 'ready' ? model.character : null;
    const canCharacterJson = props.dataCapabilities.characterJson;
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)(CampaignReadNotice, { model: model }), character ? progress ? ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Progress\u00E3o", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: character.progression }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Progress\u00E3o", tone: "soft", children: "Valores exibidos s\u00E3o do estado atual. Nenhuma compra \u00E9 aplicada por esta tela." })] })) : ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Identidade", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: character.identity }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Estado", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: character.state }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Caracter\u00EDsticas", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: character.characteristics }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Per\u00EDcias treinadas", tone: "soft", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: character.trainedSkills.length ? character.trainedSkills : [{ label: 'Perícias treinadas', value: 'Nenhuma' }] }) }), (0, jsx_runtime_1.jsxs)(Card, { title: "Arquivo da ficha", tone: "soft", children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canCharacterJson ? 1 : .45 }, disabled: !canCharacterJson, onClick: props.exportCharacterJson, children: "Exportar JSON" }), (0, jsx_runtime_1.jsxs)("label", { style: { ...button, display: 'grid', placeItems: 'center', textAlign: 'center', opacity: canCharacterJson ? 1 : .45, cursor: canCharacterJson ? 'pointer' : 'default' }, children: ["Importar JSON", (0, jsx_runtime_1.jsx)("input", { type: "file", accept: "application/json,.json", hidden: true, disabled: !canCharacterJson, onChange: (event) => { const file = event.currentTarget.files?.[0]; if (file)
                                                    void props.importCharacterJsonFile(file); event.currentTarget.value = ''; } })] })] }), !canCharacterJson && (0, jsx_runtime_1.jsx)("p", { style: { margin: '8px 0 0', color: '#A9B0B5' }, children: "Importa\u00E7\u00E3o/exporta\u00E7\u00E3o exige o Motor Barbara R23 ou posterior." })] }), (0, jsx_runtime_1.jsx)(DataActionNotice, { state: props.dataActionState, message: props.dataActionMessage })] })) : ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Personagem", tone: "soft", children: "Nenhum personagem carregado." }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: button, onClick: () => props.navigate('18'), children: "Abrir fluxo de cria\u00E7\u00E3o" })] }))] }));
}
function ItemsScreen(props) {
    const acquisition = props.screen.route === '09';
    const model = props.campaignRead;
    const inventory = model.status === 'ready' ? model.inventory : null;
    const pending = inventory?.pending ?? null;
    const canPropose = model.status === 'ready' && Boolean(model.character) && props.equipmentCapabilities.proposeWeapon && !pending && props.equipmentActionState !== 'working';
    const weaponMeta = (item) => {
        const damage = item.damageBonus !== null ? `Fortaleza ${item.damageBonus >= 0 ? '+' : ''}${item.damageBonus}` : item.damage === null ? '—' : String(item.damage);
        return `Dano ${damage} · Crítico ${item.crit ?? '—'} · ${item.range} · ${item.price.toLocaleString('pt-BR')} créditos · raridade ${item.rarity}${item.restricted ? ' · restrita' : ''}`;
    };
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)(CampaignReadNotice, { model: model }), model.status !== 'ready' ? null : acquisition ? ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Pedido atual", tone: pending ? 'warning' : 'soft', children: pending
                            ? (0, jsx_runtime_1.jsx)(DisplayList, { items: [pending], emptyText: "Nenhum item selecionado." })
                            : (0, jsx_runtime_1.jsx)("p", { style: { margin: 0 }, children: "Nenhum pedido de aquisi\u00E7\u00E3o pendente." }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Cat\u00E1logo sustentado", tone: "soft", children: !props.equipmentCapabilities.catalog ? ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: 0 }, children: "O Motor Barbara conectado ainda n\u00E3o exp\u00F5e o cat\u00E1logo auditado para esta shell." }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, width: '100%', marginTop: 10 }, onClick: props.refreshEquipmentCatalog, children: "Tentar novamente" })] })) : !props.equipmentWeapons.length ? ((0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Nenhuma arma dispon\u00EDvel no cat\u00E1logo auditado." })) : ((0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 8 }, children: props.equipmentWeapons.map((item) => ((0, jsx_runtime_1.jsxs)("div", { style: { ...softPanel, padding: 10, display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: item.name }), (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 4, color: '#A9B0B5', fontSize: 14, overflowWrap: 'anywhere' }, children: weaponMeta(item) }), item.qualities.length > 0 && (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 4, color: '#A9B0B5', fontSize: 14, overflowWrap: 'anywhere' }, children: item.qualities.join(' · ') })] }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canPropose ? 1 : .45 }, disabled: !canPropose, onClick: () => props.proposeWeaponAcquisition(item.id), children: pending ? 'Aguardando Mestre' : 'Propor ao Mestre' })] }, item.id))) })) }), (0, jsx_runtime_1.jsx)(Card, { title: "Regra de seguran\u00E7a", tone: "soft", children: "A sele\u00E7\u00E3o cria somente um pedido ao Mestre. Disponibilidade local, pre\u00E7o final, aprova\u00E7\u00E3o, cr\u00E9ditos e invent\u00E1rio s\u00F3 mudam ap\u00F3s resolu\u00E7\u00E3o confirmada pelo motor." }), props.equipmentActionState !== 'idle' && ((0, jsx_runtime_1.jsx)(Card, { title: "Aquisi\u00E7\u00E3o", tone: props.equipmentActionState === 'error' ? 'warning' : 'soft', children: props.equipmentActionState === 'working' ? 'Enviando pedido ao Mestre…' : props.equipmentActionMessage }))] })) : !inventory ? ((0, jsx_runtime_1.jsx)(Card, { title: "Invent\u00E1rio", tone: "soft", children: "Nenhum invent\u00E1rio de personagem dispon\u00EDvel." })) : ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Cr\u00E9ditos", children: inventory.credits === null
                            ? (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Saldo n\u00E3o registrado." })
                            : (0, jsx_runtime_1.jsx)(DataGrid, { fields: [{ label: 'Saldo', value: String(inventory.credits) }] }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Invent\u00E1rio", children: (0, jsx_runtime_1.jsx)(DisplayList, { items: inventory.owned, emptyText: "Nenhum item registrado." }) })] }))] }));
}
function ShipScreen(props) {
    const critical = props.screen.route === '11';
    const model = props.campaignRead;
    const vehicle = model.status === 'ready' ? model.vehicle : null;
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)(CampaignReadNotice, { model: model }), model.status !== 'ready' ? null : !vehicle?.active ? ((0, jsx_runtime_1.jsx)(Card, { title: "Nave", tone: "soft", children: "Nenhuma nave ativa registrada." })) : critical ? ((0, jsx_runtime_1.jsx)(Card, { title: "Danos cr\u00EDticos", children: (0, jsx_runtime_1.jsx)(DisplayList, { items: vehicle.criticalHits, emptyText: "Nenhum dano cr\u00EDtico registrado." }) })) : ((0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: vehicle.name ? `Resumo da nave · ${vehicle.name}` : 'Resumo da nave', children: meaningfulFields(vehicle.summary).length
                            ? (0, jsx_runtime_1.jsx)(DataGrid, { fields: meaningfulFields(vehicle.summary) })
                            : (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Nenhum dado t\u00E9cnico da nave registrado." }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Armamentos", children: (0, jsx_runtime_1.jsx)(DisplayList, { items: vehicle.weapons, emptyText: "Nenhum armamento registrado." }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Tripula\u00E7\u00E3o", tone: "soft", children: (0, jsx_runtime_1.jsx)(DisplayList, { items: vehicle.crew, emptyText: "Nenhuma tripula\u00E7\u00E3o registrada." }) })] }))] }));
}
function RecordsScreen(props) {
    const model = props.campaignRead;
    const records = model.status === 'ready' ? model.records : null;
    let content;
    let title = 'Registros';
    switch (props.screen.route) {
        case '12':
            title = 'Missão';
            content = (0, jsx_runtime_1.jsx)(DisplayList, { items: records?.mission ?? [], emptyText: "Nenhum objetivo de miss\u00E3o registrado." });
            break;
        case '13':
            title = 'Grupo / Base';
            content = meaningfulFields(records?.group).length
                ? (0, jsx_runtime_1.jsx)(DataGrid, { fields: meaningfulFields(records?.group) })
                : (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Nenhum dado de grupo ou base registrado." });
            break;
        case '14':
            title = 'Contatos conhecidos';
            content = (0, jsx_runtime_1.jsx)(DisplayList, { items: records?.contacts ?? [], emptyText: "Nenhum contato conhecido registrado." });
            break;
        case '15':
            title = 'Histórico';
            content = (0, jsx_runtime_1.jsx)(DisplayList, { items: records?.history ?? [], emptyText: "Nenhuma sess\u00E3o encerrada registrada." });
            break;
        default:
            content = (0, jsx_runtime_1.jsx)("p", { style: { margin: 0 }, children: "Nenhum registro dispon\u00EDvel." });
    }
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)(CampaignReadNotice, { model: model }), model.status === 'ready' && (0, jsx_runtime_1.jsx)(Card, { title: title, children: content })] }));
}
function MapScreen(props) {
    const galactic = props.screen.route === '16';
    const query = props.imageMapQuery.trim().toLocaleLowerCase('pt-BR');
    const filteredMaps = props.imageMaps
        .filter(item => galactic ? item.mapKind === 'galaxy' : item.mapKind !== 'galaxy')
        .filter(item => !query || [item.title, item.mapKind, item.subtype, item.sourceName, ...item.tags].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR').includes(query));
    const selected = props.selectedImageAsset;
    const selectedIsExternal = selected?.sourceType === 'external_reference';
    const canCache = selectedIsExternal && selected?.remoteReady === true && props.imageLibraryNative === 'available';
    const cached = Boolean(props.selectedImageCache?.cached);
    const sourceLabel = selected?.sourceType === 'external_reference' ? (selected.sourceName ?? 'Fonte externa') : 'Biblioteca local';
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)("div", { style: { ...softPanel, minHeight: 260, display: 'grid', placeItems: 'center', textAlign: 'center', overflow: 'hidden' }, children: selected && props.selectedImageUrl ? ((0, jsx_runtime_1.jsxs)("div", { style: { width: '100%', display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("img", { src: props.selectedImageUrl, alt: selected.title, style: { display: 'block', width: '100%', maxHeight: 420, objectFit: 'contain', borderRadius: 6, background: '#080A0B' } }), (0, jsx_runtime_1.jsxs)("div", { style: { textAlign: 'left' }, children: [(0, jsx_runtime_1.jsx)("strong", { children: selected.title }), (0, jsx_runtime_1.jsxs)("div", { style: { fontSize: 13, color: '#A9B0B5', marginTop: 2 }, children: [sourceLabel, cached ? ' · cache privado' : selectedIsExternal ? ' · online' : ' · offline'] })] })] })) : selected ? ((0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 8, placeItems: 'center' }, children: [(0, jsx_runtime_1.jsx)(ReferenceIcon_1.ReferenceIcon, { name: "map" }), (0, jsx_runtime_1.jsx)("h2", { style: { margin: 0 }, children: selected.title }), (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: selectedIsExternal && props.imageLibraryNative === 'available' ? 'Mapa externo encontrado. Armazene no cache privado para exibir no aplicativo.' : 'Imagem ainda não disponível neste host.' })] })) : ((0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)(ReferenceIcon_1.ReferenceIcon, { name: "map" }), (0, jsx_runtime_1.jsx)("h2", { style: { marginBottom: 6 }, children: galactic ? 'Mapa galáctico' : 'Mapa da missão' }), (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: props.imageLibraryCapabilities.maps ? 'Selecione um mapa da biblioteca.' : 'Biblioteca de mapas indisponível neste host.' })] })) }), selected && (0, jsx_runtime_1.jsx)(Card, { title: "Mapa selecionado", tone: "soft", children: (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 7 }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Tipo:" }), " ", selected.mapKind ?? selected.subtype ?? 'mapa'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Origem:" }), " ", sourceLabel] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Offline:" }), " ", selectedIsExternal ? cached ? 'sim, no cache privado' : 'não' : 'sim, empacotado'] }), selected.sourcePageUrl && (0, jsx_runtime_1.jsx)("a", { href: selected.sourcePageUrl, target: "_blank", rel: "noreferrer", style: { color: '#9CCBDA', overflowWrap: 'anywhere' }, children: "Abrir p\u00E1gina da fonte" }), selectedIsExternal && (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canCache && !cached ? 1 : .45 }, disabled: !canCache || cached || props.imageActionState === 'working', onClick: props.cacheSelectedImageMap, children: cached ? 'Em cache' : 'Guardar offline' }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: cached && props.imageLibraryNative === 'available' ? 1 : .45 }, disabled: !cached || props.imageLibraryNative !== 'available', onClick: props.clearSelectedImageMapCache, children: "Remover cache" })] }), props.imageActionMessage && (0, jsx_runtime_1.jsxs)("div", { role: props.imageActionState === 'error' ? 'alert' : 'status', style: { color: props.imageActionState === 'error' ? '#E5A28F' : '#9CCBDA', fontSize: 13 }, children: [props.imageActionState === 'working' ? 'Processando… ' : '', props.imageActionMessage] })] }) }), (0, jsx_runtime_1.jsx)(Card, { title: `Biblioteca de mapas${props.imageLibraryInfo ? ` · ${props.imageLibraryInfo.counts.maps ?? props.imageMaps.length}` : ''}`, children: (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: 'minmax(0,1fr) auto', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("input", { value: props.imageMapQuery, onChange: event => props.setImageMapQuery(event.currentTarget.value), placeholder: galactic ? 'Buscar mapa galáctico' : 'Buscar Tatooine, Mos Shuuta, Hoth…', style: { width: '100%', minWidth: 0, minHeight: 42, border: '1px solid #596269', borderRadius: 6, background: '#171A1C', color: C.durasteel, padding: '8px 10px', fontSize: 15 } }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, minWidth: 84 }, onClick: props.refreshImageLibrary, children: "Atualizar" })] }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 7, maxHeight: 330, overflowY: 'auto', paddingRight: 2 }, children: [filteredMaps.map(item => (0, jsx_runtime_1.jsxs)("button", { type: "button", onClick: () => props.selectImageMap(item.assetId), style: { ...softPanel, padding: 10, textAlign: 'left', color: C.durasteel, cursor: 'pointer', borderColor: selected?.assetId === item.assetId ? C.oldGold : '#596269' }, children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: 'minmax(0,1fr) auto', gap: 8, alignItems: 'start' }, children: [(0, jsx_runtime_1.jsx)("strong", { style: { overflowWrap: 'anywhere' }, children: item.title }), (0, jsx_runtime_1.jsx)("span", { style: { fontSize: 11, color: item.sourceType === 'external_reference' ? C.cyan : '#91C9A0', textTransform: 'uppercase' }, children: item.sourceType === 'external_reference' ? 'externo' : 'local' })] }), (0, jsx_runtime_1.jsxs)("div", { style: { fontSize: 13, color: '#A9B0B5', marginTop: 3 }, children: [item.mapKind ?? item.subtype ?? 'mapa', " \u00B7 ", (item.tags || []).filter(tag => !['star-wars', 'map', 'kainrath'].includes(tag)).slice(0, 4).join(' · ') || 'sem tags adicionais'] })] }, item.assetId)), !filteredMaps.length && (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Nenhum mapa corresponde ao filtro atual." })] })] }) }), (0, jsx_runtime_1.jsx)(Card, { title: galactic ? 'Deslocamento' : 'Áreas visíveis', tone: "warning", children: galactic ? 'Selecionar um destino ou trocar o mapa exibido não confirma a viagem; o deslocamento só muda após confirmação do Mestre.' : 'A biblioteca é uma superfície de consulta. Selecionar ou armazenar um mapa não revela local, não move o grupo e não altera o estado da campanha.' })] }));
}
function CreationScreen(props) {
    const step = Number(props.screen.route) - 17;
    const route = props.screen.route;
    const isFirstStep = route === '18';
    const isLastStep = route === '26';
    const canRandom = props.dataCapabilities.randomCharacter;
    const [randomName, setRandomName] = (0, react_1.useState)('');
    const [randomMotivation, setRandomMotivation] = (0, react_1.useState)('');
    const catalog = props.creationCatalog;
    const draft = props.creationDraft;
    const species = catalog?.species.find(item => item.key === draft.speciesKey) ?? null;
    const career = catalog?.careers.find(item => item.key === draft.careerKey) ?? null;
    const specialization = career?.specializations.find(item => item.key === draft.specializationKey) ?? null;
    const fieldStyle = { width: '100%', minWidth: 0, minHeight: 42, border: '1px solid #596269', borderRadius: 6, background: '#171A1C', color: C.durasteel, padding: '8px 10px', fontSize: 16 };
    const labelForSkill = (key) => catalog?.skillLabels[key] ?? key;
    const setDraft = (patch) => props.setCreationDraft(current => ({ ...current, ...patch }));
    const toggleLimited = (list, key, limit) => list.includes(key) ? list.filter(item => item !== key) : list.length < limit ? [...list, key] : list;
    const renderSkillChoices = (title, skills, selected, limit, setter) => ((0, jsx_runtime_1.jsxs)(Card, { title: title, tone: "soft", children: [(0, jsx_runtime_1.jsxs)("p", { style: { margin: '0 0 8px', color: '#A9B0B5' }, children: ["Escolha ", limit, ". Selecionadas: ", selected.length, "/", limit, "."] }), (0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 6 }, children: skills.map(key => (0, jsx_runtime_1.jsxs)("label", { style: { display: 'flex', gap: 8, alignItems: 'center', minHeight: 36 }, children: [(0, jsx_runtime_1.jsx)("input", { type: "checkbox", checked: selected.includes(key), onChange: () => setter(toggleLimited(selected, key, limit)) }), (0, jsx_runtime_1.jsx)("span", { children: labelForSkill(key) })] }, key)) })] }));
    const manualUnavailable = !props.creationCapabilities.catalog || !catalog;
    let manualContent = null;
    if (isFirstStep) {
        manualContent = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Identidade manual", tone: "soft", children: (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsxs)("label", { style: { display: 'grid', gap: 5 }, children: [(0, jsx_runtime_1.jsx)("span", { children: "Nome" }), (0, jsx_runtime_1.jsx)("input", { value: draft.name, onChange: event => setDraft({ name: event.currentTarget.value }), placeholder: "Informe o nome", style: fieldStyle })] }), (0, jsx_runtime_1.jsxs)("label", { style: { display: 'grid', gap: 5 }, children: [(0, jsx_runtime_1.jsx)("span", { children: "Apresenta\u00E7\u00E3o / hist\u00F3rico" }), (0, jsx_runtime_1.jsx)("textarea", { value: draft.background ?? '', onChange: event => setDraft({ background: event.currentTarget.value }), rows: 4, placeholder: "Texto livre; n\u00E3o altera regras.", style: { ...fieldStyle, resize: 'vertical' } })] }), (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5', fontSize: 13 }, children: "O schema atual n\u00E3o possui campo can\u00F4nico de retrato; a shell n\u00E3o inventa persist\u00EAncia para imagem de personagem." })] }) }), (0, jsx_runtime_1.jsxs)(Card, { title: "Cria\u00E7\u00E3o mec\u00E2nica aleat\u00F3ria", tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#A9B0B5' }, children: "Alternativa: o motor escolhe somente entre op\u00E7\u00F5es auditadas. Nome e motiva\u00E7\u00E3o continuam fornecidos por voc\u00EA." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("input", { value: randomName, onChange: (e) => setRandomName(e.target.value), placeholder: "Nome para gera\u00E7\u00E3o aleat\u00F3ria", style: fieldStyle }), (0, jsx_runtime_1.jsx)("textarea", { value: randomMotivation, onChange: (e) => setRandomMotivation(e.target.value), placeholder: "Motiva\u00E7\u00E3o", rows: 3, style: { ...fieldStyle, resize: 'vertical' } }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canRandom ? 1 : .45 }, disabled: !canRandom || props.dataActionState === 'working', onClick: () => props.generateRandomCharacter(randomName, randomMotivation), children: "Gerar ficha mec\u00E2nica" })] }), !canRandom && (0, jsx_runtime_1.jsx)("p", { style: { margin: '8px 0 0', color: '#A9B0B5' }, children: "Gera\u00E7\u00E3o aleat\u00F3ria indispon\u00EDvel neste host." })] }), (0, jsx_runtime_1.jsx)(DataActionNotice, { state: props.dataActionState, message: props.dataActionMessage })] });
    }
    else if (route === '19') {
        manualContent = (0, jsx_runtime_1.jsxs)(Card, { title: "Esp\u00E9cie", tone: "soft", children: [(0, jsx_runtime_1.jsxs)("select", { value: draft.speciesKey, style: fieldStyle, onChange: event => {
                        const next = catalog?.species.find(item => item.key === event.currentTarget.value);
                        setDraft({ speciesKey: event.currentTarget.value, speciesVariant: next?.speciesChoice?.type === 'gandRespiration' ? (next.speciesChoice.variants?.[0] ?? null) : null, characteristics: next ? { ...next.characteristics } : {}, careerFreeSkills: [], specializationFreeSkills: [], speciesChoiceSkills: [] });
                    }, children: [(0, jsx_runtime_1.jsx)("option", { value: "", children: "Selecione uma esp\u00E9cie" }), catalog?.species.map(item => (0, jsx_runtime_1.jsx)("option", { value: item.key, children: item.label }, item.key))] }), species && (0, jsx_runtime_1.jsxs)("div", { style: { marginTop: 10, display: 'grid', gap: 6 }, children: [(0, jsx_runtime_1.jsx)(DataGrid, { fields: [{ label: 'PE inicial', value: String(species.xp) }, { label: 'Ferimentos base', value: String(species.woundBase) }, { label: 'Tensão base', value: String(species.strainBase) }] }), species.abilities.map(text => (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#C3C8CB' }, children: text }, text))] })] });
    }
    else if (route === '20') {
        manualContent = (0, jsx_runtime_1.jsxs)(Card, { title: "Carreira", tone: "soft", children: [(0, jsx_runtime_1.jsxs)("select", { value: draft.careerKey, style: fieldStyle, onChange: event => setDraft({ careerKey: event.currentTarget.value, specializationKey: '', careerFreeSkills: [], specializationFreeSkills: [], speciesChoiceSkills: [] }), children: [(0, jsx_runtime_1.jsx)("option", { value: "", children: "Selecione uma carreira" }), catalog?.careers.map(item => (0, jsx_runtime_1.jsx)("option", { value: item.key, children: item.label }, item.key))] }), career && (0, jsx_runtime_1.jsxs)("p", { style: { margin: '10px 0 0', color: '#C3C8CB' }, children: ["Per\u00EDcias de carreira: ", career.skills.map(labelForSkill).join(' · ')] })] });
    }
    else if (route === '21') {
        manualContent = (0, jsx_runtime_1.jsxs)(Card, { title: "Especializa\u00E7\u00E3o", tone: "soft", children: [(0, jsx_runtime_1.jsxs)("select", { value: draft.specializationKey, disabled: !career, style: { ...fieldStyle, opacity: career ? 1 : .5 }, onChange: event => setDraft({ specializationKey: event.currentTarget.value, specializationFreeSkills: [], speciesChoiceSkills: [] }), children: [(0, jsx_runtime_1.jsx)("option", { value: "", children: "Selecione uma especializa\u00E7\u00E3o" }), career?.specializations.map(item => (0, jsx_runtime_1.jsx)("option", { value: item.key, children: item.label }, item.key))] }), specialization && (0, jsx_runtime_1.jsxs)("p", { style: { margin: '10px 0 0', color: '#C3C8CB' }, children: ["Per\u00EDcias: ", specialization.skills.map(labelForSkill).join(' · ')] })] });
    }
    else if (route === '22') {
        const careerCount = species?.careerFreeCount ?? 0;
        const specCount = species?.specializationFreeCount ?? 0;
        const allCareer = new Set([...(career?.skills ?? []), ...(specialization?.skills ?? [])]);
        const speciesSkills = species?.speciesChoice?.type === 'oneSkill' ? (species.speciesChoice.skills ?? []) : species?.speciesChoice?.type === 'twoNonCareer' ? Object.keys(catalog?.skillLabels ?? {}).filter(key => !allCareer.has(key)) : [];
        const speciesLimit = species?.speciesChoice?.type === 'oneSkill' ? 1 : species?.speciesChoice?.type === 'twoNonCareer' ? 2 : 0;
        manualContent = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [career && species ? renderSkillChoices('Perícias gratuitas da carreira', career.skills, draft.careerFreeSkills, careerCount, next => setDraft({ careerFreeSkills: next })) : (0, jsx_runtime_1.jsx)(Card, { title: "Per\u00EDcias", tone: "warning", children: "Selecione esp\u00E9cie e carreira primeiro." }), specialization && species ? renderSkillChoices('Perícias gratuitas da especialização', specialization.skills, draft.specializationFreeSkills, specCount, next => setDraft({ specializationFreeSkills: next })) : null, species && speciesLimit > 0 ? renderSkillChoices(species.speciesChoice?.type === 'twoNonCareer' ? 'Escolha da espécie — Humano' : 'Escolha da espécie', speciesSkills, draft.speciesChoiceSkills, speciesLimit, next => setDraft({ speciesChoiceSkills: next })) : null] });
    }
    else if (route === '23') {
        manualContent = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Caracter\u00EDsticas", tone: "soft", children: species ? (0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: Object.entries(species.characteristics).map(([key, base]) => (0, jsx_runtime_1.jsxs)("label", { style: { display: 'grid', gap: 4 }, children: [(0, jsx_runtime_1.jsx)("span", { children: catalog?.characteristicLabels[key] ?? key }), (0, jsx_runtime_1.jsx)("select", { value: draft.characteristics[key] ?? base, style: fieldStyle, onChange: event => setDraft({ characteristics: { ...draft.characteristics, [key]: Number(event.currentTarget.value) } }), children: Array.from({ length: 6 - base }, (_, i) => base + i).map(value => (0, jsx_runtime_1.jsx)("option", { value: value, children: value }, value)) }), (0, jsx_runtime_1.jsxs)("small", { style: { color: '#899196' }, children: ["base ", base] })] }, key)) }) : (0, jsx_runtime_1.jsx)("p", { style: { margin: 0 }, children: "Selecione uma esp\u00E9cie primeiro." }) }), props.creationPreview && (0, jsx_runtime_1.jsx)(Card, { title: "Or\u00E7amento de PE", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: [{ label: 'PE disponível', value: String(props.creationPreview.xpBudget) }, { label: 'Gasto em características', value: String(props.creationPreview.characteristicCost) }, { label: 'PE restante', value: String(props.creationPreview.xpRemaining) }] }) }), props.creationPreviewError && (0, jsx_runtime_1.jsx)(Card, { title: "Preview", tone: "warning", children: props.creationPreviewError })] });
    }
    else if (route === '24') {
        manualContent = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsxs)(Card, { title: "Obligation / v\u00EDnculo de campanha", tone: "soft", children: [(0, jsx_runtime_1.jsxs)("label", { style: { display: 'grid', gap: 5 }, children: [(0, jsx_runtime_1.jsx)("span", { children: "Tipo de Obligation" }), (0, jsx_runtime_1.jsxs)("select", { value: draft.obligationType, style: fieldStyle, onChange: event => setDraft({ obligationType: event.currentTarget.value }), children: [(0, jsx_runtime_1.jsx)("option", { value: "", children: "Selecione uma Obligation" }), catalog?.obligationTypes.map(value => (0, jsx_runtime_1.jsx)("option", { value: value, children: value }, value))] })] }), (0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 6, marginTop: 10 }, children: [['xp5', '+5 Obligation → +5 PE'], ['xp10', '+10 Obligation → +10 PE'], ['credits1000', '+5 Obligation → +1.000 créditos'], ['credits2500', '+10 Obligation → +2.500 créditos']].map(([key, text]) => (0, jsx_runtime_1.jsxs)("label", { style: { display: 'flex', gap: 8, alignItems: 'center' }, children: [(0, jsx_runtime_1.jsx)("input", { type: "checkbox", checked: Boolean(draft.obligationBenefits[key]), onChange: event => setDraft({ obligationBenefits: { ...draft.obligationBenefits, [key]: event.currentTarget.checked } }) }), (0, jsx_runtime_1.jsx)("span", { children: text })] }, key)) })] }), props.creationPreview && (0, jsx_runtime_1.jsxs)(Card, { title: "Resultado do v\u00EDnculo", children: [(0, jsx_runtime_1.jsx)(DataGrid, { fields: [{ label: 'Obligation final', value: String(props.creationPreview.obligation) }, { label: 'Obligation adicional', value: String(props.creationPreview.additionalObligation) }, { label: 'Créditos iniciais', value: String(props.creationPreview.credits) }, { label: 'PE total', value: String(props.creationPreview.xpBudget) }] }), props.creationPreview.obligationNote && (0, jsx_runtime_1.jsx)("p", { style: { margin: '8px 0 0', color: '#A9B0B5' }, children: props.creationPreview.obligationNote })] }), props.creationPreviewError && (0, jsx_runtime_1.jsx)(Card, { title: "Preview", tone: "warning", children: props.creationPreviewError })] });
    }
    else if (route === '25') {
        manualContent = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Motiva\u00E7\u00E3o", tone: "soft", children: (0, jsx_runtime_1.jsx)("textarea", { value: draft.motivation, onChange: event => setDraft({ motivation: event.currentTarget.value }), rows: 5, placeholder: "Motiva\u00E7\u00E3o \u00E9 obrigat\u00F3ria; o motor n\u00E3o a inventa.", style: { ...fieldStyle, resize: 'vertical' } }) }), (0, jsx_runtime_1.jsxs)(Card, { title: "Confirma\u00E7\u00E3o", tone: "soft", children: [(0, jsx_runtime_1.jsx)(DataGrid, { fields: [{ label: 'Nome', value: draft.name || '—' }, { label: 'Espécie', value: species?.label ?? '—' }, { label: 'Carreira', value: career?.label ?? '—' }, { label: 'Especialização', value: specialization?.label ?? '—' }, { label: 'Obligation', value: draft.obligationType || '—' }, { label: 'PE restante', value: props.creationPreview ? String(props.creationPreview.xpRemaining) : '—' }, { label: 'Créditos', value: props.creationPreview ? String(props.creationPreview.credits) : '—' }] }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, width: '100%', marginTop: 10, opacity: props.creationCapabilities.create ? 1 : .45 }, disabled: !props.creationCapabilities.create || props.creationActionState === 'working', onClick: props.createManualCharacter, children: props.creationActionState === 'working' ? 'Validando no motor…' : 'Criar ficha manual' }), props.creationActionMessage && (0, jsx_runtime_1.jsx)("div", { role: props.creationActionState === 'error' ? 'alert' : 'status', style: { marginTop: 8, color: props.creationActionState === 'error' ? '#E5A28F' : '#9CCBDA' }, children: props.creationActionMessage })] })] });
    }
    else if (isLastStep) {
        manualContent = (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Or\u00E7amento atual", tone: "soft", children: props.campaignRead.status === 'ready' && props.campaignRead.character ? (props.campaignRead.inventory?.credits === null || props.campaignRead.inventory?.credits === undefined ? (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Saldo de cr\u00E9ditos n\u00E3o registrado." }) : (0, jsx_runtime_1.jsx)(DataGrid, { fields: [{ label: 'Créditos disponíveis', value: String(props.campaignRead.inventory.credits) }] })) : (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Crie ou carregue uma ficha para consultar o or\u00E7amento." }) }), (0, jsx_runtime_1.jsxs)(Card, { title: "Equipamento auditado", children: [props.equipmentCapabilities.catalog ? (0, jsx_runtime_1.jsxs)("p", { style: { margin: 0 }, children: [props.equipmentWeapons.length, " arma(s) do cat\u00E1logo auditado dispon\u00EDveis para consulta."] }) : (0, jsx_runtime_1.jsx)("p", { style: { margin: 0, color: '#A9B0B5' }, children: "Cat\u00E1logo auditado indispon\u00EDvel neste host." }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, width: '100%', marginTop: 10 }, onClick: () => props.navigate('09'), children: "Abrir aquisi\u00E7\u00E3o em ITENS" })] }), (0, jsx_runtime_1.jsx)(Card, { title: "Regra de seguran\u00E7a", tone: "warning", children: "A etapa de cria\u00E7\u00E3o n\u00E3o desconta cr\u00E9ditos nem adiciona equipamento diretamente. Toda aquisi\u00E7\u00E3o permanece no fluxo can\u00F4nico de ITENS e s\u00F3 muda a ficha ap\u00F3s resolu\u00E7\u00E3o confirmada pelo Mestre." })] });
    }
    return (0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)(Card, { title: `Etapa ${step} de 9`, children: (0, jsx_runtime_1.jsx)("p", { style: { margin: 0 }, children: props.screen.description }) }), manualUnavailable && !isFirstStep && !isLastStep ? (0, jsx_runtime_1.jsxs)(Card, { title: "Cria\u00E7\u00E3o manual indispon\u00EDvel", tone: "warning", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: 0 }, children: "Esta etapa exige o bridge de cria\u00E7\u00E3o do Motor Barbara R33/R34. A shell n\u00E3o simula op\u00E7\u00F5es locais." }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, width: '100%', marginTop: 10 }, onClick: props.refreshCreationCatalog, children: "Tentar novamente" })] }) : manualContent, (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: isFirstStep ? .45 : 1 }, disabled: isFirstStep, onClick: isFirstStep ? undefined : () => props.navigate(String(Number(props.screen.route) - 1).padStart(2, '0')), children: "Anterior" }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: isLastStep ? .45 : 1 }, disabled: isLastStep, onClick: isLastStep ? undefined : () => props.navigate(String(Number(props.screen.route) + 1).padStart(2, '0')), children: "Pr\u00F3xima" })] })] });
}
function SettingsScreen(props) {
    const visualQuery = props.imageVisualQuery.trim().toLocaleLowerCase('pt-BR');
    const visualKindMatches = (asset) => props.imageVisualKind === 'all' || (props.imageVisualKind === 'npc' && asset.category === 'portrait') || (props.imageVisualKind === 'droid' && asset.category === 'droid') || (props.imageVisualKind === 'beast' && asset.category === 'creature') || (props.imageVisualKind === 'vehicle' && asset.category === 'vehicle');
    const visualItems = props.imageVisualEntities.filter(visualKindMatches).filter(asset => !visualQuery || [asset.title, asset.assetId, asset.category, asset.subtype, ...asset.tags].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR').includes(visualQuery)).slice(0, 30);
    const route = props.screen.route;
    const canBackup = props.dataCapabilities.campaignBackup;
    const canSnapshots = props.dataCapabilities.snapshots;
    const canLibrary = props.libraryBridge === 'available' && props.libraryCapabilities.list;
    const [libraryQuery, setLibraryQuery] = (0, react_1.useState)('');
    const [geminiApiKey, setGeminiApiKey] = (0, react_1.useState)('');
    const [showScenePrompt, setShowScenePrompt] = (0, react_1.useState)(false);
    const authorityOptions = ['ERRATA', 'BASE', 'SUPLEMENTO', 'AVENTURA', 'REFERENCIA'];
    const fieldStyle = { width: '100%', minWidth: 0, minHeight: 42, border: '1px solid #596269', borderRadius: 6, background: '#171A1C', color: C.durasteel, padding: '8px 10px', fontSize: 15 };
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), route === '27' && (0, jsx_runtime_1.jsx)(Card, { title: "Interface", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: ['Escala de texto', 'Contraste', 'Movimento reduzido', 'Preferência visual'] }) }), route === '28' && (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Narra\u00E7\u00E3o / voz", children: (0, jsx_runtime_1.jsx)(DataGrid, { fields: ['Voz', 'TTS', 'Saída de áudio', 'Estado'] }) }), (0, jsx_runtime_1.jsx)(Card, { title: "Estado", tone: "warning", children: "Narra\u00E7\u00E3o por voz e TTS est\u00E3o indispon\u00EDveis at\u00E9 uma integra\u00E7\u00E3o real ser confirmada." })] }), route === '29' && (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Gemini / imagem de cena", children: (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 7 }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Host:" }), " ", props.geminiBridge === 'available' ? 'bridge Android disponível' : props.geminiBridge === 'invalid' ? 'bridge incompatível' : 'indisponível neste host'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Modelo:" }), " ", props.geminiStatus?.model ?? 'gemini-3.1-flash-image'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Chave:" }), " ", props.geminiStatus?.configured ? 'configurada e protegida no Android' : 'não configurada'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Imagem da cena atual:" }), " ", props.sceneImageUrl ? 'disponível' : props.geminiStatus?.imageUrl ? 'existe para outra cena' : 'não fornecida'] })] }) }), (0, jsx_runtime_1.jsxs)(Card, { title: "Chave da API Gemini", tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#C3C8CB' }, children: "A chave \u00E9 enviada diretamente ao host Android e protegida pelo Android Keystore. Ela n\u00E3o entra no estado da campanha, backup, RAG nem armazenamento da interface." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("input", { type: "password", value: geminiApiKey, onChange: (event) => setGeminiApiKey(event.currentTarget.value), disabled: !props.geminiCapabilities.configure || props.geminiActionState === 'working', autoComplete: "new-password", spellCheck: false, placeholder: "Cole sua chave Gemini", style: fieldStyle }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: props.geminiCapabilities.configure && geminiApiKey.trim() ? 1 : .45 }, disabled: !props.geminiCapabilities.configure || !geminiApiKey.trim() || props.geminiActionState === 'working', onClick: () => { if (props.configureGeminiApiKey(geminiApiKey))
                                                    setGeminiApiKey(''); }, children: "Salvar chave" }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: props.geminiCapabilities.clearKey && props.geminiStatus?.configured ? 1 : .45 }, disabled: !props.geminiCapabilities.clearKey || !props.geminiStatus?.configured || props.geminiActionState === 'working', onClick: props.clearGeminiApiKey, children: "Remover chave" })] })] })] }), (0, jsx_runtime_1.jsxs)(Card, { title: "Imagem cinematogr\u00E1fica da cena", tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#C3C8CB' }, children: "O prompt usa somente t\u00EDtulo, narra\u00E7\u00E3o e pergunta j\u00E1 projetados publicamente para a CENA. Mem\u00F3ria secreta de NPC, RAG privado e notas do Mestre n\u00E3o s\u00E3o enviados." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: props.geminiCapabilities.generate && props.geminiStatus?.configured && props.sceneImagePrompt && !props.geminiStatus?.generating ? 1 : .45 }, disabled: !props.geminiCapabilities.generate || !props.geminiStatus?.configured || !props.sceneImagePrompt || Boolean(props.geminiStatus?.generating), onClick: props.generateSceneImage, children: props.geminiStatus?.generating ? 'Gerando…' : 'Gerar com Gemini' }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: props.geminiCapabilities.importImage && props.sceneImagePrompt ? 1 : .45 }, disabled: !props.geminiCapabilities.importImage || !props.sceneImagePrompt, onClick: props.importSceneImage, children: "Importar imagem" })] }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginTop: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: props.sceneImagePrompt ? 1 : .45 }, disabled: !props.sceneImagePrompt, onClick: () => setShowScenePrompt(value => !value), children: showScenePrompt ? 'Ocultar prompt' : 'Mostrar prompt' }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: props.geminiCapabilities.clearImage && props.geminiStatus?.imageUrl ? 1 : .45 }, disabled: !props.geminiCapabilities.clearImage || !props.geminiStatus?.imageUrl, onClick: props.clearSceneImage, children: "Remover imagem" })] }), showScenePrompt && props.sceneImagePrompt && (0, jsx_runtime_1.jsx)("textarea", { readOnly: true, value: props.sceneImagePrompt.prompt, rows: 8, "aria-label": "Prompt de imagem da cena", style: { ...fieldStyle, minHeight: 150, resize: 'vertical', marginTop: 8, lineHeight: 1.35 } }), props.geminiActionMessage && (0, jsx_runtime_1.jsxs)("div", { role: props.geminiActionState === 'error' ? 'alert' : 'status', style: { marginTop: 9, color: props.geminiActionState === 'error' ? '#E5A28F' : '#9CCBDA', fontSize: 13 }, children: [props.geminiActionState === 'working' ? 'Processando… ' : '', props.geminiActionMessage] }), props.geminiBridge !== 'available' && (0, jsx_runtime_1.jsx)("p", { style: { margin: '9px 0 0', color: '#A9B0B5' }, children: "A web shell n\u00E3o armazena chave nem chama Gemini diretamente. Gera\u00E7\u00E3o e importa\u00E7\u00E3o exigem o host Android compat\u00EDvel." })] })] }), route === '30' && (0, jsx_runtime_1.jsxs)(jsx_runtime_1.Fragment, { children: [(0, jsx_runtime_1.jsx)(Card, { title: "Dados / diagn\u00F3stico", children: (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 6 }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Motor / dados:" }), " ", props.masterBridge === 'available' ? 'ponte canônica disponível' : props.masterBridge === 'invalid' ? 'ponte incompatível' : 'indisponível neste host'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Biblioteca PDF:" }), " ", props.libraryBridge === 'available' ? 'bridge Android disponível' : props.libraryBridge === 'invalid' ? 'bridge incompatível' : 'disponível somente no host Android compatível'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Biblioteca visual:" }), " ", props.imageLibraryInfo ? `${props.imageLibraryInfo.counts.total ?? props.imageMaps.length} assets · ${props.imageLibraryInfo.counts.maps ?? props.imageMaps.length} mapas` : 'não consultada'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Cache visual Android:" }), " ", props.imageLibraryNative === 'available' ? 'disponível' : props.imageLibraryNative === 'invalid' ? 'bridge incompatível' : 'indisponível neste host'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Fontes locais:" }), " ", canLibrary ? `${props.libraryDocuments.length} documento(s)` : 'não consultadas'] })] }) }), (0, jsx_runtime_1.jsxs)(Card, { title: "Biblioteca visual", tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#C3C8CB' }, children: "Assets locais ficam offline. Mapas externos indexados do Kainrath s\u00F3 s\u00E3o baixados por a\u00E7\u00E3o expl\u00EDcita e v\u00E3o para cache privado; imagem nunca cria regra, fac\u00E7\u00E3o, esp\u00E9cie ou identidade por infer\u00EAncia." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 6 }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Manifesto:" }), " ", props.imageLibraryInfo?.libraryId ?? 'indisponível'] }), (0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: "Locais:" }), " ", props.imageLibraryInfo?.counts.local ?? '—', " \u00B7 ", (0, jsx_runtime_1.jsx)("strong", { children: "Externos:" }), " ", props.imageLibraryInfo?.counts.external ?? '—', " \u00B7 ", (0, jsx_runtime_1.jsx)("strong", { children: "Mapas:" }), " ", props.imageLibraryInfo?.counts.maps ?? '—', " \u00B7 ", (0, jsx_runtime_1.jsx)("strong", { children: "Ve\u00EDculos/Naves:" }), " ", props.imageLibraryInfo?.counts.vehicles ?? 0] }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, marginTop: 4, opacity: props.imageLibraryCapabilities.info ? 1 : .45 }, disabled: !props.imageLibraryCapabilities.info, onClick: props.refreshImageLibrary, children: "Atualizar biblioteca visual" })] })] }), (0, jsx_runtime_1.jsxs)(Card, { title: `Biblioteca de entidades visuais${props.imageLibraryInfo ? ` · ${(props.imageLibraryInfo.counts.portraits ?? 0) + (props.imageLibraryInfo.counts.droids ?? 0) + (props.imageLibraryInfo.counts.creatures ?? 0) + (props.imageLibraryInfo.counts.vehicles ?? 0)}` : ''}`, tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#C3C8CB' }, children: "Refer\u00EAncias visuais externas para NPCs, droides, criaturas e ve\u00EDculos/naves. Nenhuma ficha, perfil de nave ou estat\u00EDstica \u00E9 criada a partir destas imagens." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr auto', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("input", { value: props.imageVisualQuery, onChange: event => props.setImageVisualQuery(event.currentTarget.value), placeholder: "Buscar Cratala, 2-1B, Krayt\u2026", style: { ...fieldStyle, minWidth: 0 } }), (0, jsx_runtime_1.jsxs)("select", { value: props.imageVisualKind, onChange: event => props.setImageVisualKind(event.currentTarget.value), style: { ...fieldStyle, minWidth: 112 }, children: [(0, jsx_runtime_1.jsx)("option", { value: "all", children: "Todos" }), (0, jsx_runtime_1.jsx)("option", { value: "npc", children: "NPCs" }), (0, jsx_runtime_1.jsx)("option", { value: "droid", children: "Droides" }), (0, jsx_runtime_1.jsx)("option", { value: "beast", children: "Criaturas" }), (0, jsx_runtime_1.jsx)("option", { value: "vehicle", children: "Ve\u00EDculos/Naves" })] })] }), !props.imageLibraryCapabilities.visuals && (0, jsx_runtime_1.jsx)("p", { style: { margin: '9px 0 0', color: '#A9B0B5' }, children: "O host atual ainda n\u00E3o exp\u00F5e o cat\u00E1logo visual R37." }), props.imageLibraryCapabilities.visuals && visualItems.length === 0 && (0, jsx_runtime_1.jsx)("p", { style: { margin: '9px 0 0', color: '#A9B0B5' }, children: "Nenhuma refer\u00EAncia visual encontrada." }), (0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 8, marginTop: 10 }, children: visualItems.map(asset => {
                                    const cached = props.imageCacheStatuses?.[asset.assetId]?.cached === true;
                                    return (0, jsx_runtime_1.jsxs)("div", { style: { border: '1px solid #40484D', borderRadius: 6, padding: 9, display: 'grid', gap: 6, minWidth: 0 }, children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, minWidth: 0 }, children: [(0, jsx_runtime_1.jsx)("strong", { style: { overflowWrap: 'anywhere' }, children: asset.title }), (0, jsx_runtime_1.jsx)("span", { style: { color: '#A9B0B5', fontSize: 12 }, children: asset.category === 'portrait' ? 'NPC' : asset.category === 'droid' ? 'DROIDE' : asset.category === 'vehicle' ? 'VEÍCULO/NAVE' : 'CRIATURA' })] }), (0, jsx_runtime_1.jsxs)("div", { style: { fontSize: 12, color: '#A9B0B5', overflowWrap: 'anywhere' }, children: [asset.sourceName ?? 'Kainrath', " \u00B7 ", cached ? 'offline' : 'online'] }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: props.imageLibraryNative === 'available' && !cached ? 1 : .45 }, disabled: props.imageLibraryNative !== 'available' || cached, onClick: () => props.cacheVisualEntity(asset.assetId), children: "Guardar offline" }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: props.imageLibraryNative === 'available' && cached ? 1 : .45 }, disabled: props.imageLibraryNative !== 'available' || !cached, onClick: () => props.clearVisualEntityCache(asset.assetId), children: "Remover cache" })] }), asset.sourcePageUrl && (0, jsx_runtime_1.jsx)("a", { href: asset.sourcePageUrl, target: "_blank", rel: "noreferrer", style: { fontSize: 12, color: '#9CCBDA' }, children: "Abrir fonte" })] }, asset.assetId);
                                }) })] }), (0, jsx_runtime_1.jsxs)(Card, { title: "Biblioteca / PDFs", tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#C3C8CB' }, children: "PDFs importados s\u00E3o indexados no armazenamento privado do aplicativo. Apenas fontes indexadas e habilitadas entram na consulta documental." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: props.libraryCapabilities.importPdf ? 1 : .45 }, disabled: !props.libraryCapabilities.importPdf, onClick: props.importPdfToLibrary, children: "Importar PDF" }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: canLibrary ? 1 : .45 }, disabled: !canLibrary, onClick: () => props.refreshLibraryDocuments(), children: "Atualizar lista" })] }), !canLibrary && (0, jsx_runtime_1.jsxs)("p", { style: { margin: '9px 0 0', color: '#A9B0B5' }, children: ["A importa\u00E7\u00E3o/indexa\u00E7\u00E3o f\u00EDsica exige o bridge ", (0, jsx_runtime_1.jsx)("code", { children: "BraseiroLibrary" }), " do aplicativo Android. A web shell n\u00E3o finge esse recurso."] })] }), canLibrary && props.libraryDocuments.length === 0 && (0, jsx_runtime_1.jsx)(Card, { title: "Fontes locais", children: "Nenhum PDF importado neste armazenamento." }), props.libraryDocuments.map((document) => (0, jsx_runtime_1.jsx)(Card, { title: document.fileName, tone: "soft", children: (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gap: 8 }, children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, alignItems: 'start' }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: (0, libraryBridgeClient_1.libraryStatusLabel)(document.status) }), (0, jsx_runtime_1.jsxs)("div", { style: { marginTop: 3, color: '#A9B0B5', fontSize: 13 }, children: [document.pageCount !== null ? `${document.pageCount} pág.` : 'páginas não informadas', document.indexedPages !== null ? ` · ${document.indexedPages} indexadas` : '', document.textChars !== null ? ` · ${document.textChars.toLocaleString('pt-BR')} caracteres` : ''] })] }), (0, jsx_runtime_1.jsx)("span", { style: { ...label, color: document.status === 'INDEXADO' ? '#91C9A0' : document.status === 'ERRO' ? '#E5A28F' : C.oldGold }, children: document.enabled ? 'ATIVA' : 'DESATIVADA' })] }), document.lastError && (0, jsx_runtime_1.jsx)("div", { style: { color: '#E5A28F', fontSize: 13 }, children: document.lastError }), (0, jsx_runtime_1.jsxs)("label", { style: { display: 'grid', gap: 4, fontSize: 13, color: '#B8BEC2' }, children: ["Autoridade da fonte", (0, jsx_runtime_1.jsx)("select", { value: document.authorityType, disabled: !props.libraryCapabilities.policy, style: fieldStyle, onChange: (event) => props.updateLibraryPolicy(document.docId, event.currentTarget.value, document.enabled), children: authorityOptions.map((authority) => (0, jsx_runtime_1.jsx)("option", { value: authority, children: (0, libraryBridgeClient_1.libraryAuthorityLabel)(authority) }, authority)) })] }), (0, jsx_runtime_1.jsxs)("label", { style: { display: 'flex', gap: 8, alignItems: 'center', fontSize: 14 }, children: [(0, jsx_runtime_1.jsx)("input", { type: "checkbox", checked: document.enabled, disabled: !props.libraryCapabilities.policy, onChange: (event) => props.updateLibraryPolicy(document.docId, document.authorityType, event.currentTarget.checked) }), "Usar esta fonte nas consultas do Mestre"] }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, minHeight: 40, opacity: props.libraryCapabilities.reindex ? 1 : .45 }, disabled: !props.libraryCapabilities.reindex, onClick: () => props.reindexLibraryDocument(document.docId), children: "Reindexar" }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, minHeight: 40, borderColor: '#81483E', color: '#E5A28F', opacity: props.libraryCapabilities.remove ? 1 : .45 }, disabled: !props.libraryCapabilities.remove, onClick: () => props.removeLibraryDocument(document.docId), children: "Remover" })] })] }) }, document.docId)), (0, jsx_runtime_1.jsxs)(Card, { title: "Consultar PDFs indexados", children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) auto', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("input", { value: libraryQuery, onChange: (event) => setLibraryQuery(event.currentTarget.value), onKeyDown: (event) => { if (event.key === 'Enter' && props.libraryCapabilities.search)
                                            props.searchLibrary(libraryQuery); }, disabled: !props.libraryCapabilities.search, placeholder: "Buscar nos manuais importados", style: fieldStyle }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, minWidth: 78, opacity: props.libraryCapabilities.search ? 1 : .45 }, disabled: !props.libraryCapabilities.search, onClick: () => props.searchLibrary(libraryQuery), children: "Buscar" })] }), props.librarySearchResults.length > 0 && (0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 8, marginTop: 10 }, children: props.librarySearchResults.map((hit) => (0, jsx_runtime_1.jsxs)("div", { style: { ...softPanel, padding: 10 }, children: [(0, jsx_runtime_1.jsx)("strong", { children: hit.title }), (0, jsx_runtime_1.jsxs)("div", { style: { marginTop: 3, color: '#A9B0B5', fontSize: 13 }, children: [hit.page !== null ? `Página ${hit.page}` : 'Página não informada', " \u00B7 ", (0, libraryBridgeClient_1.libraryAuthorityLabel)(hit.authorityType)] }), (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 6, fontSize: 14 }, children: hit.excerpt })] }, hit.id)) }), props.librarySearchResults.length > 0 && (0, jsx_runtime_1.jsx)("p", { style: { margin: '9px 0 0', color: props.librarySearchGrounded ? '#91C9A0' : '#E5A28F', fontSize: 13 }, children: props.librarySearchGrounded ? 'Trechos recuperados do índice local.' : 'Resultado não deve ser tratado como regra conhecida.' })] }), (0, jsx_runtime_1.jsx)(DataActionNotice, { state: props.libraryActionState, message: props.libraryActionMessage }), (0, jsx_runtime_1.jsxs)(Card, { title: "Backup e recupera\u00E7\u00E3o", tone: "soft", children: [(0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canBackup ? 1 : .45 }, disabled: !canBackup, onClick: props.exportCampaignBackup, children: "Exportar backup" }), (0, jsx_runtime_1.jsxs)("label", { style: { ...button, display: 'grid', placeItems: 'center', textAlign: 'center', opacity: canBackup ? 1 : .45, cursor: canBackup ? 'pointer' : 'default' }, children: ["Importar backup", (0, jsx_runtime_1.jsx)("input", { type: "file", accept: "application/json,.json", hidden: true, disabled: !canBackup, onChange: (event) => { const file = event.currentTarget.files?.[0]; if (file)
                                                    void props.importCampaignBackupFile(file); event.currentTarget.value = ''; } })] }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canSnapshots ? 1 : .45 }, disabled: !canSnapshots, onClick: props.createManualSnapshot, children: "Criar snapshot" }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, opacity: canSnapshots ? 1 : .45 }, disabled: !canSnapshots, onClick: () => props.refreshSnapshots(), children: "Listar snapshots" })] }), !canBackup && !canSnapshots && (0, jsx_runtime_1.jsx)("p", { style: { margin: '8px 0 0', color: '#A9B0B5' }, children: "Backup e snapshots exigem o Motor Barbara R23 ou posterior." })] }), props.snapshots.length > 0 && (0, jsx_runtime_1.jsx)(Card, { title: "Snapshots", children: (0, jsx_runtime_1.jsx)("div", { style: { display: 'grid', gap: 8 }, children: props.snapshots.map((snapshot) => (0, jsx_runtime_1.jsxs)("div", { style: { ...softPanel, padding: 10, display: 'grid', gridTemplateColumns: '1fr auto', gap: 8, alignItems: 'center' }, children: [(0, jsx_runtime_1.jsxs)("div", { children: [(0, jsx_runtime_1.jsx)("strong", { children: snapshot.label || 'Snapshot' }), (0, jsx_runtime_1.jsx)("div", { style: { marginTop: 3, color: '#A9B0B5', fontSize: 13 }, children: snapshot.createdAt })] }), (0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...mutedButton, minHeight: 38, padding: '6px 10px' }, onClick: () => props.restoreSnapshot(snapshot.id), children: "Restaurar" })] }, snapshot.id)) }) }), (0, jsx_runtime_1.jsx)(DataActionNotice, { state: props.dataActionState, message: props.dataActionMessage }), (0, jsx_runtime_1.jsx)(Card, { title: "Valida\u00E7\u00E3o", tone: "warning", children: "Build Android/WebView em aparelho f\u00EDsico, OCR, IA/Gemini e TTS continuam sem homologa\u00E7\u00E3o neste ambiente. A Biblioteca s\u00F3 \u00E9 marcada dispon\u00EDvel quando o bridge Android real existe." })] })] }));
}
function renderQaState(props) {
    if (props.qaState === 'default' || props.qaState === 'demo')
        return null;
    const copy = {
        loading: { title: 'Carregando · estado de QA', body: 'Simulação visual de carregamento; não comprova sincronização, rede ou serviço externo real.', tone: 'soft' },
        skeleton: { title: 'Estrutura · estado de QA', body: 'Espaços reservados testam apenas a geometria; nenhum conteúdo ou carregamento real é afirmado.', tone: 'soft' },
        empty: { title: 'Vazio · estado de QA', body: 'Simulação visual de vazio; não afirma ausência real de dados da campanha.', tone: 'dark' },
        error: { title: 'Erro · estado de QA', body: 'Simulação visual de erro; não comprova falha real de carregamento, rede ou serviço.', tone: 'warning' },
        offline: { title: 'Offline · estado de QA', body: 'Este estado apenas testa a apresentação sem conexão; não comprova funcionamento offline real.', tone: 'warning' },
        partial: { title: 'Parcial · estado de QA', body: 'Simulação visual de dados incompletos; campos ausentes permanecem vazios e nenhum sincronismo é presumido.', tone: 'soft' },
    };
    const current = copy[props.qaState] ?? copy.empty;
    return ((0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(SiblingNav, { screen: props.screen, navigate: props.navigate }), (0, jsx_runtime_1.jsx)(Card, { title: current.title, tone: current.tone, children: current.body })] }));
}
function renderTemplate(props) {
    const qa = renderQaState(props);
    if (qa)
        return qa;
    switch (props.screen.domain) {
        case 'Cena': return (0, jsx_runtime_1.jsx)(SceneScreen, { ...props });
        case 'Personagem': return (0, jsx_runtime_1.jsx)(CharacterScreen, { ...props });
        case 'Itens': return (0, jsx_runtime_1.jsx)(ItemsScreen, { ...props });
        case 'Nave': return (0, jsx_runtime_1.jsx)(ShipScreen, { ...props });
        case 'Registros': return (0, jsx_runtime_1.jsx)(RecordsScreen, { ...props });
        case 'Mapa': return (0, jsx_runtime_1.jsx)(MapScreen, { ...props });
        case 'Criação': return (0, jsx_runtime_1.jsx)(CreationScreen, { ...props });
        case 'Ajustes': return (0, jsx_runtime_1.jsx)(SettingsScreen, { ...props });
        default: return (0, jsx_runtime_1.jsxs)("div", { style: page, children: [(0, jsx_runtime_1.jsx)(Header, { screen: props.screen, qaState: props.qaState }), (0, jsx_runtime_1.jsx)(Card, { title: "Estado", children: "Tela sem template associado." })] });
    }
}

});
define("VTTApp",function(module,exports,require){
'use client';
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.VTTApp = VTTApp;
const jsx_runtime_1 = require("react/jsx-runtime");
const react_1 = require("react");
const screens_1 = require("@/src/config/screens");
const primitives_1 = require("./primitives");
const DebugGallery_1 = require("./DebugGallery");
const Templates_1 = require("./Templates");
const r21Projection_1 = require("@/src/state/r21Projection");
const gameBridgeClient_1 = require("@/src/state/gameBridgeClient");
const libraryBridgeClient_1 = require("@/src/state/libraryBridgeClient");
const geminiBridgeClient_1 = require("@/src/state/geminiBridgeClient");
const imageLibraryBridgeClient_1 = require("@/src/state/imageLibraryBridgeClient");
const allowedRoutes = new Set(screens_1.screenRegistry.map((item) => item.route));
function readRoute() {
    if (typeof window === 'undefined')
        return '00';
    const requested = new URLSearchParams(window.location.search).get('screen') ?? '00';
    return allowedRoutes.has(requested) ? requested : '00';
}
function readState() {
    if (typeof window === 'undefined')
        return 'default';
    const requested = new URLSearchParams(window.location.search).get('state') ?? 'default';
    return (0, screens_1.isQaState)(requested) ? requested : 'default';
}
function readDebugMode() {
    if (typeof window === 'undefined')
        return false;
    return new URLSearchParams(window.location.search).get('debug') === '1';
}
const rootRouteByDomain = {
    Cena: '00',
    Personagem: '06',
    Itens: '08',
    Nave: '10',
    Registros: '12',
};
const MAX_JSON_IMPORT_BYTES = 16 * 1024 * 1024;
const EMPTY_CREATION_DRAFT = {
    name: '', background: '', speciesKey: '', careerKey: '', specializationKey: '', partySize: 1,
    speciesVariant: null, obligationType: '', obligationBenefits: { xp5: false, xp10: false, credits1000: false, credits2500: false },
    characteristics: {}, careerFreeSkills: [], specializationFreeSkills: [], speciesChoiceSkills: [], motivation: '',
};
function VTTApp() {
    const [route, setRoute] = (0, react_1.useState)('00');
    const [qaState, setQaStateValue] = (0, react_1.useState)('default');
    const [sceneDraft, setSceneDraft] = (0, react_1.useState)('');
    const [statusOpen, setStatusOpen] = (0, react_1.useState)(false);
    const [debugMode, setDebugMode] = (0, react_1.useState)(false);
    const [campaignRead, setCampaignRead] = (0, react_1.useState)(r21Projection_1.EMPTY_R21_READ_MODEL);
    const [masterBridge, setMasterBridge] = (0, react_1.useState)('unavailable');
    const [intentSubmitState, setIntentSubmitState] = (0, react_1.useState)('idle');
    const [intentSubmitMessage, setIntentSubmitMessage] = (0, react_1.useState)('');
    const [dataCapabilities, setDataCapabilities] = (0, react_1.useState)({ characterJson: false, campaignBackup: false, snapshots: false, randomCharacter: false });
    const [dataActionState, setDataActionState] = (0, react_1.useState)('idle');
    const [dataActionMessage, setDataActionMessage] = (0, react_1.useState)('');
    const [snapshots, setSnapshots] = (0, react_1.useState)([]);
    const [equipmentCapabilities, setEquipmentCapabilities] = (0, react_1.useState)({ catalog: false, proposeWeapon: false });
    const [equipmentWeapons, setEquipmentWeapons] = (0, react_1.useState)([]);
    const [equipmentActionState, setEquipmentActionState] = (0, react_1.useState)('idle');
    const [equipmentActionMessage, setEquipmentActionMessage] = (0, react_1.useState)('');
    const [creationCapabilities, setCreationCapabilities] = (0, react_1.useState)({ catalog: false, preview: false, create: false });
    const [creationCatalog, setCreationCatalog] = (0, react_1.useState)(null);
    const [creationDraft, setCreationDraft] = (0, react_1.useState)(EMPTY_CREATION_DRAFT);
    const [creationPreview, setCreationPreview] = (0, react_1.useState)(null);
    const [creationPreviewError, setCreationPreviewError] = (0, react_1.useState)('');
    const [creationActionState, setCreationActionState] = (0, react_1.useState)('idle');
    const [creationActionMessage, setCreationActionMessage] = (0, react_1.useState)('');
    const [imageLibraryCapabilities, setImageLibraryCapabilities] = (0, react_1.useState)({ info: false, search: false, maps: false, visuals: false, asset: false, runtime: false });
    const [imageLibraryInfo, setImageLibraryInfo] = (0, react_1.useState)(null);
    const [imageMaps, setImageMaps] = (0, react_1.useState)([]);
    const [imageMapQuery, setImageMapQuery] = (0, react_1.useState)('');
    const [imageVisualEntities, setImageVisualEntities] = (0, react_1.useState)([]);
    const [imageVisualQuery, setImageVisualQuery] = (0, react_1.useState)('');
    const [imageVisualKind, setImageVisualKind] = (0, react_1.useState)('all');
    const [selectedImageAssetId, setSelectedImageAssetId] = (0, react_1.useState)(null);
    const [imageCacheStatuses, setImageCacheStatuses] = (0, react_1.useState)({});
    const [imageLibraryNative, setImageLibraryNative] = (0, react_1.useState)('unavailable');
    const [imageActionState, setImageActionState] = (0, react_1.useState)('idle');
    const [imageActionMessage, setImageActionMessage] = (0, react_1.useState)('');
    const [libraryBridge, setLibraryBridge] = (0, react_1.useState)('unavailable');
    const [libraryCapabilities, setLibraryCapabilities] = (0, react_1.useState)({ list: false, search: false, importPdf: false, policy: false, reindex: false, remove: false });
    const [libraryDocuments, setLibraryDocuments] = (0, react_1.useState)([]);
    const [libraryActionState, setLibraryActionState] = (0, react_1.useState)('idle');
    const [libraryActionMessage, setLibraryActionMessage] = (0, react_1.useState)('');
    const [librarySearchResults, setLibrarySearchResults] = (0, react_1.useState)([]);
    const [librarySearchGrounded, setLibrarySearchGrounded] = (0, react_1.useState)(false);
    const [geminiBridge, setGeminiBridge] = (0, react_1.useState)('unavailable');
    const [geminiCapabilities, setGeminiCapabilities] = (0, react_1.useState)({ status: false, configure: false, clearKey: false, generate: false, importImage: false, clearImage: false });
    const [geminiStatus, setGeminiStatus] = (0, react_1.useState)(null);
    const [geminiActionState, setGeminiActionState] = (0, react_1.useState)('idle');
    const [geminiActionMessage, setGeminiActionMessage] = (0, react_1.useState)('');
    const sceneImagePrompt = (0, react_1.useMemo)(() => campaignRead.status === 'ready' ? (0, geminiBridgeClient_1.buildGeminiScenePrompt)(campaignRead.scene) : null, [campaignRead]);
    const sceneImageUrl = geminiStatus?.imageUrl && sceneImagePrompt && geminiStatus.imageSceneKey === sceneImagePrompt.sceneKey ? geminiStatus.imageUrl : null;
    const selectedImageAsset = (0, react_1.useMemo)(() => selectedImageAssetId ? imageMaps.find(item => item.assetId === selectedImageAssetId) ?? null : null, [imageMaps, selectedImageAssetId]);
    const selectedImageCache = selectedImageAssetId ? imageCacheStatuses[selectedImageAssetId] ?? null : null;
    const selectedImageUrl = (0, react_1.useMemo)(() => {
        if (!selectedImageAsset)
            return null;
        if (selectedImageAsset.sourceType !== 'external_reference') {
            if (typeof window !== 'undefined' && window.location.protocol !== 'file:')
                return selectedImageAsset.localPath ? `/${selectedImageAsset.localPath.replace(/^\/+/, '')}` : null;
            return (0, imageLibraryBridgeClient_1.packagedImageUrl)(selectedImageAsset);
        }
        if (selectedImageCache?.cached && selectedImageCache.runtimeUrl)
            return selectedImageCache.runtimeUrl;
        return imageLibraryNative === 'unavailable' ? (0, imageLibraryBridgeClient_1.browserExternalFallbackUrl)(selectedImageAsset) : null;
    }, [selectedImageAsset, selectedImageCache, imageLibraryNative]);
    (0, react_1.useEffect)(() => {
        const sync = () => {
            setRoute(readRoute());
            setQaStateValue(readState());
            setDebugMode(readDebugMode());
        };
        sync();
        addEventListener('popstate', sync);
        return () => removeEventListener('popstate', sync);
    }, []);
    const refreshCampaignRead = (0, react_1.useCallback)(() => {
        setCampaignRead((0, r21Projection_1.readR21CampaignState)());
    }, []);
    const refreshMasterBridge = (0, react_1.useCallback)(() => {
        setMasterBridge((0, gameBridgeClient_1.getBridgeAvailability)());
        setDataCapabilities((0, gameBridgeClient_1.getDataBridgeCapabilities)());
        setEquipmentCapabilities((0, gameBridgeClient_1.getEquipmentBridgeCapabilities)());
        setCreationCapabilities((0, gameBridgeClient_1.getCreationBridgeCapabilities)());
        setImageLibraryCapabilities((0, gameBridgeClient_1.getImageLibraryBridgeCapabilities)());
        setImageLibraryNative((0, imageLibraryBridgeClient_1.getImageLibraryNativeAvailability)());
    }, []);
    const refreshEquipmentCatalog = (0, react_1.useCallback)((silent = false) => {
        const capabilities = (0, gameBridgeClient_1.getEquipmentBridgeCapabilities)();
        setEquipmentCapabilities(capabilities);
        if (!capabilities.catalog) {
            setEquipmentWeapons([]);
            if (!silent && (0, gameBridgeClient_1.getBridgeAvailability)() === 'invalid') {
                setEquipmentActionState('error');
                setEquipmentActionMessage('Ponte de equipamento incompatível com esta shell.');
            }
            return false;
        }
        const result = (0, gameBridgeClient_1.loadEquipmentCatalogThroughBridge)();
        if (!result.ok) {
            setEquipmentWeapons([]);
            if (!silent) {
                setEquipmentActionState('error');
                setEquipmentActionMessage(result.message);
            }
            return false;
        }
        setEquipmentWeapons(result.data.weapons);
        return true;
    }, []);
    const refreshCreationCatalog = (0, react_1.useCallback)((silent = false) => {
        const capabilities = (0, gameBridgeClient_1.getCreationBridgeCapabilities)();
        setCreationCapabilities(capabilities);
        if (!capabilities.catalog) {
            setCreationCatalog(null);
            setCreationPreview(null);
            if (!silent && (0, gameBridgeClient_1.getBridgeAvailability)() === 'invalid') {
                setCreationActionState('error');
                setCreationActionMessage('Ponte de criação incompatível com esta shell.');
            }
            return false;
        }
        const result = (0, gameBridgeClient_1.loadCreationCatalogThroughBridge)();
        if (!result.ok) {
            setCreationCatalog(null);
            setCreationPreview(null);
            if (!silent) {
                setCreationActionState('error');
                setCreationActionMessage(result.message);
            }
            return false;
        }
        setCreationCatalog(result.data);
        return true;
    }, []);
    const refreshImageLibrary = (0, react_1.useCallback)((silent = false) => {
        const capabilities = (0, gameBridgeClient_1.getImageLibraryBridgeCapabilities)();
        setImageLibraryCapabilities(capabilities);
        setImageLibraryNative((0, imageLibraryBridgeClient_1.getImageLibraryNativeAvailability)());
        if (!capabilities.info || !capabilities.maps) {
            setImageLibraryInfo(null);
            setImageMaps([]);
            setImageVisualEntities([]);
            if (!silent && (0, gameBridgeClient_1.getBridgeAvailability)() === 'invalid') {
                setImageActionState('error');
                setImageActionMessage('Ponte da biblioteca de imagens incompatível com esta shell.');
            }
            return false;
        }
        const info = (0, gameBridgeClient_1.loadImageLibraryInfoThroughBridge)();
        const catalog = (0, gameBridgeClient_1.loadImageMapCatalogThroughBridge)({ limit: 500 });
        const visuals = capabilities.visuals ? (0, gameBridgeClient_1.loadImageVisualEntityCatalogThroughBridge)({ kind: 'all', limit: 500 }) : { ok: true, data: [] };
        if (!info.ok || !catalog.ok || !visuals.ok) {
            setImageLibraryInfo(null);
            setImageMaps([]);
            setImageVisualEntities([]);
            if (!silent) {
                setImageActionState('error');
                setImageActionMessage(!info.ok ? info.message : !catalog.ok ? catalog.message : visuals.message);
            }
            return false;
        }
        const maps = catalog.data;
        const visualItems = visuals.data;
        setImageLibraryInfo(info.data);
        setImageMaps(maps);
        setImageVisualEntities(visualItems);
        setSelectedImageAssetId(current => current && maps.some(item => item.assetId === current) ? current : null);
        return true;
    }, []);
    const refreshImageCacheStatus = (0, react_1.useCallback)((assetId, silent = false) => {
        const availability = (0, imageLibraryBridgeClient_1.getImageLibraryNativeAvailability)();
        setImageLibraryNative(availability);
        if (availability !== 'available')
            return false;
        const result = (0, imageLibraryBridgeClient_1.getImageCacheStatus)(assetId);
        if (!result.ok) {
            if (!silent) {
                setImageActionState('error');
                setImageActionMessage(result.message);
            }
            return false;
        }
        setImageCacheStatuses(current => ({ ...current, [assetId]: result.data }));
        return true;
    }, []);
    const refreshLibraryBridge = (0, react_1.useCallback)(() => {
        setLibraryBridge((0, libraryBridgeClient_1.getLibraryBridgeAvailability)());
        setLibraryCapabilities((0, libraryBridgeClient_1.getLibraryCapabilities)());
    }, []);
    const refreshGeminiBridge = (0, react_1.useCallback)((silent = false) => {
        const availability = (0, geminiBridgeClient_1.getGeminiBridgeAvailability)();
        setGeminiBridge(availability);
        setGeminiCapabilities((0, geminiBridgeClient_1.getGeminiCapabilities)());
        if (availability !== 'available') {
            setGeminiStatus(null);
            if (!silent && availability === 'invalid') {
                setGeminiActionState('error');
                setGeminiActionMessage('Ponte Gemini incompatível com esta shell.');
            }
            return false;
        }
        const result = (0, geminiBridgeClient_1.getGeminiStatusThroughBridge)();
        if (!result.ok) {
            setGeminiStatus(null);
            if (!silent) {
                setGeminiActionState('error');
                setGeminiActionMessage(result.message);
            }
            return false;
        }
        setGeminiStatus(result.data);
        return true;
    }, []);
    const refreshLibraryDocuments = (0, react_1.useCallback)((silent = false) => {
        const availability = (0, libraryBridgeClient_1.getLibraryBridgeAvailability)();
        setLibraryBridge(availability);
        setLibraryCapabilities((0, libraryBridgeClient_1.getLibraryCapabilities)());
        if (availability !== 'available') {
            setLibraryDocuments([]);
            if (!silent && availability === 'invalid') {
                setLibraryActionState('error');
                setLibraryActionMessage('Ponte da Biblioteca incompatível com esta shell.');
            }
            return false;
        }
        const result = (0, libraryBridgeClient_1.listLibraryDocumentsThroughBridge)();
        if (!result.ok) {
            setLibraryDocuments([]);
            if (!silent) {
                setLibraryActionState('error');
                setLibraryActionMessage(result.message);
            }
            return false;
        }
        setLibraryDocuments(result.data);
        return true;
    }, []);
    (0, react_1.useEffect)(() => {
        refreshCampaignRead();
        refreshMasterBridge();
        refreshImageLibrary(true);
        refreshLibraryBridge();
        refreshGeminiBridge(true);
        const onStorage = (event) => {
            if (event.key === null || event.key === r21Projection_1.R21_STORAGE_KEY)
                refreshCampaignRead();
        };
        addEventListener('storage', onStorage);
        const onStateChanged = () => {
            refreshCampaignRead();
            refreshMasterBridge();
            refreshImageLibrary(true);
            refreshLibraryBridge();
            refreshGeminiBridge(true);
        };
        addEventListener(r21Projection_1.R21_STATE_CHANGED_EVENT, onStateChanged);
        const onFocus = () => {
            refreshCampaignRead();
            refreshMasterBridge();
            refreshImageLibrary(true);
            refreshLibraryBridge();
            refreshGeminiBridge(true);
            if ((0, libraryBridgeClient_1.getLibraryBridgeAvailability)() === 'available')
                refreshLibraryDocuments(true);
        };
        addEventListener('focus', onFocus);
        return () => {
            removeEventListener('storage', onStorage);
            removeEventListener(r21Projection_1.R21_STATE_CHANGED_EVENT, onStateChanged);
            removeEventListener('focus', onFocus);
        };
    }, [refreshCampaignRead, refreshMasterBridge, refreshImageLibrary, refreshLibraryBridge, refreshLibraryDocuments, refreshGeminiBridge]);
    (0, react_1.useEffect)(() => {
        if ((route === '16' || route === '17') && masterBridge === 'available')
            refreshImageLibrary(true);
        if (route === '16')
            setSelectedImageAssetId('local.map.galaxy');
    }, [route, masterBridge, refreshImageLibrary]);
    (0, react_1.useEffect)(() => {
        if (selectedImageAssetId && imageLibraryNative === 'available')
            refreshImageCacheStatus(selectedImageAssetId, true);
    }, [selectedImageAssetId, imageLibraryNative, refreshImageCacheStatus]);
    (0, react_1.useEffect)(() => {
        if ((route === '09' || route === '26') && masterBridge === 'available')
            refreshEquipmentCatalog(true);
    }, [route, masterBridge, refreshEquipmentCatalog]);
    (0, react_1.useEffect)(() => {
        const n = Number(route);
        if (n >= 18 && n <= 26 && masterBridge === 'available')
            refreshCreationCatalog(true);
    }, [route, masterBridge, refreshCreationCatalog]);
    (0, react_1.useEffect)(() => {
        if (!creationCapabilities.preview || !creationDraft.speciesKey) {
            setCreationPreview(null);
            setCreationPreviewError('');
            return;
        }
        const result = (0, gameBridgeClient_1.previewCharacterCreationThroughBridge)(creationDraft);
        if (!result.ok) {
            setCreationPreview(null);
            setCreationPreviewError(result.message);
            return;
        }
        setCreationPreview(result.data);
        setCreationPreviewError('');
    }, [creationCapabilities.preview, creationDraft]);
    (0, react_1.useEffect)(() => {
        if (route === '30' && libraryBridge === 'available')
            refreshLibraryDocuments(true);
    }, [route, libraryBridge, refreshLibraryDocuments]);
    (0, react_1.useEffect)(() => {
        if ((route === '29' || screens_1.screenRegistry.find(item => item.route === route)?.domain === 'Cena') && geminiBridge === 'available')
            refreshGeminiBridge(true);
    }, [route, geminiBridge, refreshGeminiBridge]);
    (0, react_1.useEffect)(() => {
        if (typeof window === 'undefined')
            return;
        const previous = window.braseiroLibraryNativeEvent;
        window.braseiroLibraryNativeEvent = (raw) => {
            if (typeof previous === 'function')
                previous(raw);
            const event = (0, libraryBridgeClient_1.parseLibraryNativeEvent)(raw);
            refreshLibraryBridge();
            refreshLibraryDocuments(true);
            if (!event) {
                setLibraryActionState('error');
                setLibraryActionMessage('Evento inválido recebido da Biblioteca Android.');
                return;
            }
            const document = event.payload.document && typeof event.payload.document === 'object' ? event.payload.document : null;
            const fileName = document && typeof document.fileName === 'string' ? document.fileName : 'PDF';
            const messages = {
                cancelado: 'Importação cancelada.',
                indexando: `Anexando e indexando ${fileName}…`,
                concluido: `Biblioteca atualizada: ${fileName}.`,
                duplicado: 'Este PDF já existe na Biblioteca; nenhuma cópia foi criada.',
                removido: 'PDF removido da Biblioteca local.',
                politica: `Autoridade de ${fileName} atualizada.`,
                erro: typeof event.payload.message === 'string' ? event.payload.message : 'Falha ao importar ou indexar o PDF.',
            };
            setLibraryActionState(event.type === 'erro' ? 'error' : 'success');
            setLibraryActionMessage(messages[event.type] ?? 'Biblioteca atualizada.');
        };
        return () => { window.braseiroLibraryNativeEvent = previous; };
    }, [refreshLibraryBridge, refreshLibraryDocuments]);
    (0, react_1.useEffect)(() => {
        if (typeof window === 'undefined')
            return;
        const previous = window.braseiroImageLibraryNativeEvent;
        window.braseiroImageLibraryNativeEvent = (raw) => {
            if (typeof previous === 'function')
                previous(raw);
            const event = (0, imageLibraryBridgeClient_1.parseImageLibraryNativeEvent)(raw);
            setImageLibraryNative((0, imageLibraryBridgeClient_1.getImageLibraryNativeAvailability)());
            if (!event) {
                setImageActionState('error');
                setImageActionMessage('Evento inválido recebido do cache de imagens.');
                return;
            }
            const assetId = typeof event.data.assetId === 'string' ? event.data.assetId : '';
            if (assetId)
                refreshImageCacheStatus(assetId, true);
            const messages = {
                queued: 'Download do mapa colocado na fila.',
                cached: 'Mapa armazenado no cache privado do aplicativo.',
                cleared: 'Mapa removido do cache privado.',
                error: typeof event.data.message === 'string' ? event.data.message : 'Falha ao obter o mapa externo.',
            };
            setImageActionState(event.type === 'error' ? 'error' : event.type === 'queued' ? 'working' : 'success');
            setImageActionMessage(messages[event.type] ?? 'Biblioteca de imagens atualizada.');
        };
        return () => { window.braseiroImageLibraryNativeEvent = previous; };
    }, [refreshImageCacheStatus]);
    (0, react_1.useEffect)(() => {
        if (typeof window === 'undefined')
            return;
        const previous = window.braseiroGeminiNativeEvent;
        window.braseiroGeminiNativeEvent = (raw) => {
            if (typeof previous === 'function')
                previous(raw);
            const event = (0, geminiBridgeClient_1.parseGeminiNativeEvent)(raw);
            refreshGeminiBridge(true);
            if (!event) {
                setGeminiActionState('error');
                setGeminiActionMessage('Evento inválido recebido do host Gemini.');
                return;
            }
            const messages = {
                configured: 'Chave Gemini protegida no host Android.',
                cleared: 'Chave Gemini removida do dispositivo.',
                generating: 'Gemini está gerando a imagem da cena…',
                generated: 'Imagem da cena gerada e vinculada ao contexto atual.',
                imported: 'Imagem importada e vinculada à cena atual.',
                image_cleared: 'Imagem da cena removida.',
                cancelado: 'Importação de imagem cancelada.',
                erro: typeof event.payload.message === 'string' ? event.payload.message : 'Falha no host Gemini.',
            };
            setGeminiActionState(event.type === 'erro' ? 'error' : event.type === 'generating' ? 'working' : 'success');
            setGeminiActionMessage(messages[event.type] ?? 'Gemini atualizado.');
        };
        return () => { window.braseiroGeminiNativeEvent = previous; };
    }, [refreshGeminiBridge]);
    const navigate = (0, react_1.useCallback)((next) => {
        const safeNext = allowedRoutes.has(next) ? next : '00';
        const url = new URL(window.location.href);
        url.searchParams.set('screen', safeNext);
        url.searchParams.set('state', qaState);
        url.searchParams.delete('debug');
        history.pushState({}, '', url);
        setRoute(safeNext);
        setDebugMode(false);
        window.scrollTo(0, 0);
    }, [qaState]);
    const setQaState = (0, react_1.useCallback)((next) => {
        const safeNext = (0, screens_1.isQaState)(next) ? next : 'default';
        const url = new URL(window.location.href);
        url.searchParams.set('state', safeNext);
        history.replaceState({}, '', url);
        setQaStateValue(safeNext);
    }, []);
    const submitSceneIntent = (0, react_1.useCallback)(async () => {
        if (intentSubmitState === 'sending')
            return;
        const text = sceneDraft.trim();
        if (!text) {
            setIntentSubmitState('error');
            setIntentSubmitMessage('Digite uma ação.');
            return;
        }
        setIntentSubmitState('sending');
        setIntentSubmitMessage('');
        const result = await (0, gameBridgeClient_1.submitPlayerIntentThroughBridge)(text);
        if (!result.ok) {
            setIntentSubmitState('error');
            setIntentSubmitMessage(result.message);
            refreshMasterBridge();
            return;
        }
        setIntentSubmitState('sent');
        setIntentSubmitMessage('Ação entregue ao Mestre.');
        setSceneDraft('');
        refreshMasterBridge();
        refreshCampaignRead();
    }, [intentSubmitState, sceneDraft, refreshMasterBridge, refreshCampaignRead]);
    const finishDataAction = (0, react_1.useCallback)((result, successMessage, refreshState = false) => {
        if (!result.ok) {
            setDataActionState('error');
            setDataActionMessage(result.message || 'Operação rejeitada pelo motor.');
            refreshMasterBridge();
            return false;
        }
        setDataActionState('success');
        setDataActionMessage(successMessage);
        if (refreshState)
            refreshCampaignRead();
        refreshMasterBridge();
        return true;
    }, [refreshCampaignRead, refreshMasterBridge]);
    const exportCharacterJson = (0, react_1.useCallback)(() => {
        setDataActionState('working');
        setDataActionMessage('');
        const result = (0, gameBridgeClient_1.exportCharacterJsonThroughBridge)();
        if (!result.ok)
            return void finishDataAction(result, '');
        const downloaded = (0, gameBridgeClient_1.downloadBridgeJson)(result.data);
        finishDataAction(downloaded, 'Ficha exportada em JSON.');
    }, [finishDataAction]);
    const importCharacterJsonFile = (0, react_1.useCallback)(async (file) => {
        setDataActionState('working');
        setDataActionMessage('');
        try {
            if (file.size > MAX_JSON_IMPORT_BYTES)
                throw new Error('O arquivo JSON excede 16 MB.');
            const result = (0, gameBridgeClient_1.importCharacterJsonThroughBridge)(await file.text());
            finishDataAction(result, 'Ficha importada. A proteção por snapshot foi solicitada ao motor.', true);
        }
        catch (error) {
            setDataActionState('error');
            setDataActionMessage(error instanceof Error ? error.message : 'Falha ao ler o arquivo JSON.');
        }
    }, [finishDataAction]);
    const exportCampaignBackup = (0, react_1.useCallback)(() => {
        setDataActionState('working');
        setDataActionMessage('');
        const result = (0, gameBridgeClient_1.exportCampaignBackupThroughBridge)();
        if (!result.ok)
            return void finishDataAction(result, '');
        const downloaded = (0, gameBridgeClient_1.downloadBridgeJson)(result.data);
        finishDataAction(downloaded, 'Backup da campanha exportado.');
    }, [finishDataAction]);
    const importCampaignBackupFile = (0, react_1.useCallback)(async (file) => {
        setDataActionState('working');
        setDataActionMessage('');
        try {
            if (file.size > MAX_JSON_IMPORT_BYTES)
                throw new Error('O backup excede 16 MB.');
            const result = (0, gameBridgeClient_1.importCampaignBackupThroughBridge)(await file.text());
            finishDataAction(result, 'Backup restaurado. A proteção por snapshot foi solicitada ao motor.', true);
        }
        catch (error) {
            setDataActionState('error');
            setDataActionMessage(error instanceof Error ? error.message : 'Falha ao ler o backup.');
        }
    }, [finishDataAction]);
    const refreshSnapshots = (0, react_1.useCallback)(() => {
        const result = (0, gameBridgeClient_1.listSnapshotsThroughBridge)();
        if (!result.ok) {
            setSnapshots([]);
            return finishDataAction(result, '');
        }
        const items = result.data;
        setSnapshots(items);
        setDataActionState('idle');
        setDataActionMessage('');
        return true;
    }, [finishDataAction]);
    const createManualSnapshot = (0, react_1.useCallback)(() => {
        setDataActionState('working');
        setDataActionMessage('');
        const result = (0, gameBridgeClient_1.createSnapshotThroughBridge)('Backup manual');
        if (finishDataAction(result, 'Snapshot criado.'))
            refreshSnapshots();
    }, [finishDataAction, refreshSnapshots]);
    const restoreSnapshot = (0, react_1.useCallback)((id) => {
        if (!window.confirm('Restaurar este snapshot? O estado atual será substituído.'))
            return;
        setDataActionState('working');
        setDataActionMessage('');
        const result = (0, gameBridgeClient_1.restoreSnapshotThroughBridge)(id);
        if (finishDataAction(result, 'Snapshot restaurado.', true))
            refreshSnapshots();
    }, [finishDataAction, refreshSnapshots]);
    const generateRandomCharacter = (0, react_1.useCallback)((name, motivation) => {
        const cleanName = name.trim();
        const cleanMotivation = motivation.trim();
        if (!cleanName || !cleanMotivation) {
            setDataActionState('error');
            setDataActionMessage('Informe nome e motivação. O gerador não inventa esses dados.');
            return;
        }
        if (campaignRead.status === 'ready' && campaignRead.character && !window.confirm('Gerar uma nova ficha mecânica e substituir a ficha atual? A operação solicitará um snapshot antes da substituição.'))
            return;
        setDataActionState('working');
        setDataActionMessage('');
        const result = (0, gameBridgeClient_1.generateRandomCharacterThroughBridge)({ name: cleanName, motivation: cleanMotivation });
        finishDataAction(result, 'Ficha mecânica criada. Revise em PERSONAGEM.', true);
    }, [campaignRead, finishDataAction]);
    const createManualCharacter = (0, react_1.useCallback)(() => {
        if (!creationCapabilities.create) {
            setCreationActionState('error');
            setCreationActionMessage('Criação manual indisponível neste host.');
            return false;
        }
        if (campaignRead.status === 'ready' && campaignRead.character && !window.confirm('Criar esta ficha manual e substituir a ficha atual? O motor solicitará um snapshot antes da substituição.'))
            return false;
        setCreationActionState('working');
        setCreationActionMessage('');
        const result = (0, gameBridgeClient_1.createCustomCharacterThroughBridge)(creationDraft);
        if (!result.ok) {
            setCreationActionState('error');
            setCreationActionMessage(result.message);
            refreshMasterBridge();
            return false;
        }
        setCreationActionState('success');
        setCreationActionMessage('Ficha manual criada pelo Motor Barbara.');
        refreshCampaignRead();
        refreshMasterBridge();
        navigate('26');
        return true;
    }, [campaignRead, creationCapabilities.create, creationDraft, refreshCampaignRead, refreshMasterBridge, navigate]);
    const proposeWeaponAcquisition = (0, react_1.useCallback)((itemId) => {
        if (campaignRead.status !== 'ready' || !campaignRead.character) {
            setEquipmentActionState('error');
            setEquipmentActionMessage('Carregue um personagem antes de propor uma aquisição.');
            return;
        }
        if (campaignRead.inventory?.pending) {
            setEquipmentActionState('error');
            setEquipmentActionMessage('Já existe uma aquisição aguardando decisão do Mestre.');
            return;
        }
        setEquipmentActionState('working');
        setEquipmentActionMessage('');
        const result = (0, gameBridgeClient_1.proposeWeaponAcquisitionThroughBridge)(itemId);
        if (!result.ok) {
            setEquipmentActionState('error');
            setEquipmentActionMessage(result.message);
            refreshMasterBridge();
            return;
        }
        setEquipmentActionState('success');
        setEquipmentActionMessage('Pedido entregue ao Mestre. Créditos e inventário não foram alterados.');
        refreshMasterBridge();
        refreshCampaignRead();
        refreshEquipmentCatalog(true);
    }, [campaignRead, refreshMasterBridge, refreshCampaignRead, refreshEquipmentCatalog]);
    const selectImageMap = (0, react_1.useCallback)((assetId) => {
        const id = String(assetId ?? '').trim();
        if (!id || !imageMaps.some(item => item.assetId === id))
            return;
        setSelectedImageAssetId(id);
        setImageActionState('idle');
        setImageActionMessage('');
    }, [imageMaps]);
    const cacheSelectedImageMap = (0, react_1.useCallback)(() => {
        if (!selectedImageAsset) {
            setImageActionState('error');
            setImageActionMessage('Selecione um mapa externo.');
            return false;
        }
        setImageActionState('working');
        setImageActionMessage('');
        const result = (0, imageLibraryBridgeClient_1.cacheExternalImage)(selectedImageAsset);
        if (!result.ok) {
            setImageActionState('error');
            setImageActionMessage(result.message);
            return false;
        }
        setImageActionMessage(result.status === 'already_cached' ? 'Mapa já está no cache privado.' : 'Baixando mapa para o cache privado…');
        if (result.status === 'already_cached') {
            setImageActionState('success');
            refreshImageCacheStatus(selectedImageAsset.assetId, true);
        }
        return true;
    }, [selectedImageAsset, refreshImageCacheStatus]);
    const clearSelectedImageMapCache = (0, react_1.useCallback)(() => {
        if (!selectedImageAssetId)
            return false;
        const result = (0, imageLibraryBridgeClient_1.clearCachedImage)(selectedImageAssetId);
        if (!result.ok) {
            setImageActionState('error');
            setImageActionMessage(result.message);
            return false;
        }
        setImageCacheStatuses(current => ({ ...current, [selectedImageAssetId]: { assetId: selectedImageAssetId, cached: false } }));
        setImageActionState('success');
        setImageActionMessage('Mapa removido do cache privado.');
        return true;
    }, [selectedImageAssetId]);
    const cacheVisualEntity = (0, react_1.useCallback)((assetId) => {
        const asset = imageVisualEntities.find(item => item.assetId === assetId);
        if (!asset) {
            setImageActionState('error');
            setImageActionMessage('Asset visual não encontrado.');
            return false;
        }
        setImageActionState('working');
        setImageActionMessage('');
        const result = (0, imageLibraryBridgeClient_1.cacheExternalImage)(asset);
        if (!result.ok) {
            setImageActionState('error');
            setImageActionMessage(result.message);
            return false;
        }
        setImageActionMessage(result.status === 'already_cached' ? 'Imagem já está no cache privado.' : 'Baixando imagem para o cache privado…');
        if (result.status === 'already_cached') {
            setImageActionState('success');
            refreshImageCacheStatus(asset.assetId, true);
        }
        return true;
    }, [imageVisualEntities, refreshImageCacheStatus]);
    const clearVisualEntityCache = (0, react_1.useCallback)((assetId) => {
        const result = (0, imageLibraryBridgeClient_1.clearCachedImage)(assetId);
        if (!result.ok) {
            setImageActionState('error');
            setImageActionMessage(result.message);
            return false;
        }
        setImageCacheStatuses(current => ({ ...current, [assetId]: { assetId, cached: false } }));
        setImageActionState('success');
        setImageActionMessage('Imagem removida do cache privado.');
        return true;
    }, []);
    const finishLibraryAction = (0, react_1.useCallback)((result, successMessage, refresh = false) => {
        if (!result.ok) {
            setLibraryActionState('error');
            setLibraryActionMessage(result.message || 'Operação rejeitada pela Biblioteca Android.');
            refreshLibraryBridge();
            return false;
        }
        setLibraryActionState('success');
        setLibraryActionMessage(successMessage);
        refreshLibraryBridge();
        if (refresh)
            refreshLibraryDocuments(true);
        return true;
    }, [refreshLibraryBridge, refreshLibraryDocuments]);
    const importPdfToLibrary = (0, react_1.useCallback)(() => {
        setLibraryActionState('working');
        setLibraryActionMessage('');
        const result = (0, libraryBridgeClient_1.requestPdfImportThroughBridge)();
        finishLibraryAction(result, 'Seletor de PDF aberto. A indexação ocorrerá no armazenamento privado do aplicativo.');
    }, [finishLibraryAction]);
    const searchLibrary = (0, react_1.useCallback)((query) => {
        setLibraryActionState('working');
        setLibraryActionMessage('');
        const result = (0, libraryBridgeClient_1.searchLibraryThroughBridge)(query, 8);
        if (!result.ok) {
            setLibrarySearchResults([]);
            setLibrarySearchGrounded(false);
            finishLibraryAction(result, '');
            return;
        }
        setLibrarySearchResults(result.data.results);
        setLibrarySearchGrounded(result.data.grounded);
        setLibraryActionState('success');
        setLibraryActionMessage(result.data.results.length ? `${result.data.results.length} trecho(s) encontrado(s) no índice offline.` : 'Nenhum trecho encontrado nos PDFs indexados.');
    }, [finishLibraryAction]);
    const updateLibraryPolicy = (0, react_1.useCallback)((docId, authorityType, enabled) => {
        setLibraryActionState('working');
        setLibraryActionMessage('');
        const result = (0, libraryBridgeClient_1.updateLibraryDocumentPolicyThroughBridge)(docId, authorityType, enabled);
        finishLibraryAction(result, 'Autoridade da fonte atualizada.', true);
    }, [finishLibraryAction]);
    const reindexLibraryDocument = (0, react_1.useCallback)((docId) => {
        setLibraryActionState('working');
        setLibraryActionMessage('');
        const result = (0, libraryBridgeClient_1.reindexLibraryDocumentThroughBridge)(docId);
        finishLibraryAction(result, 'Reindexação iniciada.', true);
    }, [finishLibraryAction]);
    const removeLibraryDocument = (0, react_1.useCallback)((docId) => {
        if (!window.confirm('Remover este PDF e seu índice offline da Biblioteca?'))
            return;
        setLibraryActionState('working');
        setLibraryActionMessage('');
        const result = (0, libraryBridgeClient_1.removeLibraryDocumentThroughBridge)(docId);
        if (finishLibraryAction(result, 'PDF removido da Biblioteca local.', true)) {
            setLibrarySearchResults(current => current.filter(item => item.docId !== docId));
        }
    }, [finishLibraryAction]);
    const finishGeminiAction = (0, react_1.useCallback)((result, successMessage) => {
        if (!result.ok) {
            setGeminiActionState('error');
            setGeminiActionMessage(result.message || 'Operação rejeitada pelo host Gemini.');
            refreshGeminiBridge(true);
            return false;
        }
        setGeminiActionState('success');
        setGeminiActionMessage(successMessage);
        refreshGeminiBridge(true);
        return true;
    }, [refreshGeminiBridge]);
    const configureGeminiApiKey = (0, react_1.useCallback)((apiKey) => {
        setGeminiActionState('working');
        setGeminiActionMessage('');
        return finishGeminiAction((0, geminiBridgeClient_1.configureGeminiApiKeyThroughBridge)(apiKey), 'Chave Gemini protegida no host Android.');
    }, [finishGeminiAction]);
    const clearGeminiApiKey = (0, react_1.useCallback)(() => {
        if (!window.confirm('Remover a chave Gemini protegida deste dispositivo?'))
            return false;
        setGeminiActionState('working');
        setGeminiActionMessage('');
        return finishGeminiAction((0, geminiBridgeClient_1.clearGeminiApiKeyThroughBridge)(), 'Chave Gemini removida do dispositivo.');
    }, [finishGeminiAction]);
    const generateSceneImage = (0, react_1.useCallback)(() => {
        if (!sceneImagePrompt) {
            setGeminiActionState('error');
            setGeminiActionMessage('A cena ainda não tem narração pública suficiente para gerar imagem.');
            return false;
        }
        setGeminiActionState('working');
        setGeminiActionMessage('');
        return finishGeminiAction((0, geminiBridgeClient_1.generateGeminiSceneImageThroughBridge)(sceneImagePrompt), 'Geração iniciada. A imagem só será exibida quando o host confirmar a conclusão.');
    }, [sceneImagePrompt, finishGeminiAction]);
    const importSceneImage = (0, react_1.useCallback)(() => {
        if (!sceneImagePrompt) {
            setGeminiActionState('error');
            setGeminiActionMessage('A cena atual ainda não possui contexto visual identificável.');
            return false;
        }
        setGeminiActionState('working');
        setGeminiActionMessage('');
        return finishGeminiAction((0, geminiBridgeClient_1.requestGeminiSceneImageImportThroughBridge)(sceneImagePrompt.sceneKey), 'Seletor de imagem aberto.');
    }, [sceneImagePrompt, finishGeminiAction]);
    const clearSceneImage = (0, react_1.useCallback)(() => {
        if (!window.confirm('Remover a imagem vinculada à cena?'))
            return false;
        setGeminiActionState('working');
        setGeminiActionMessage('');
        return finishGeminiAction((0, geminiBridgeClient_1.clearGeminiSceneImageThroughBridge)(), 'Imagem da cena removida.');
    }, [finishGeminiAction]);
    const screen = (0, screens_1.getScreen)(route);
    const activeNav = (0, react_1.useMemo)(() => rootRouteByDomain[screen.domain] ?? '', [screen.domain]);
    const props = {
        screen,
        qaState,
        navigate,
        sceneDraft,
        setSceneDraft,
        openMenu: () => setStatusOpen(true),
        campaignRead,
        masterBridge,
        intentSubmitState,
        intentSubmitMessage,
        submitSceneIntent,
        dataCapabilities,
        dataActionState,
        dataActionMessage,
        snapshots,
        exportCharacterJson,
        importCharacterJsonFile,
        exportCampaignBackup,
        importCampaignBackupFile,
        refreshSnapshots,
        createManualSnapshot,
        restoreSnapshot,
        generateRandomCharacter,
        creationCapabilities,
        creationCatalog,
        creationDraft,
        setCreationDraft,
        creationPreview,
        creationPreviewError,
        creationActionState,
        creationActionMessage,
        refreshCreationCatalog: () => refreshCreationCatalog(false),
        createManualCharacter,
        equipmentCapabilities,
        equipmentWeapons,
        equipmentActionState,
        equipmentActionMessage,
        refreshEquipmentCatalog: () => refreshEquipmentCatalog(false),
        proposeWeaponAcquisition,
        imageLibraryCapabilities,
        imageLibraryInfo,
        imageMaps,
        imageMapQuery,
        setImageMapQuery,
        imageVisualEntities,
        imageVisualQuery,
        setImageVisualQuery,
        imageVisualKind,
        setImageVisualKind,
        selectedImageAsset,
        selectedImageUrl,
        selectedImageCache,
        imageCacheStatuses,
        imageLibraryNative,
        imageActionState,
        imageActionMessage,
        refreshImageLibrary: () => refreshImageLibrary(false),
        selectImageMap,
        cacheSelectedImageMap,
        clearSelectedImageMapCache,
        cacheVisualEntity,
        clearVisualEntityCache,
        libraryBridge,
        libraryCapabilities,
        libraryDocuments,
        libraryActionState,
        libraryActionMessage,
        librarySearchResults,
        librarySearchGrounded,
        refreshLibraryDocuments: () => refreshLibraryDocuments(false),
        importPdfToLibrary,
        searchLibrary,
        updateLibraryPolicy,
        reindexLibraryDocument,
        removeLibraryDocument,
        geminiBridge,
        geminiCapabilities,
        geminiStatus,
        geminiActionState,
        geminiActionMessage,
        sceneImageUrl,
        sceneImagePrompt,
        configureGeminiApiKey,
        clearGeminiApiKey,
        generateSceneImage,
        importSceneImage,
        clearSceneImage,
    };
    return ((0, jsx_runtime_1.jsxs)(primitives_1.AppShell, { className: screen.domain === 'Cena' ? 'scene-mode' : '', children: [(0, jsx_runtime_1.jsx)(primitives_1.TopBar, { onStatus: () => setStatusOpen(true) }), (0, jsx_runtime_1.jsx)("div", { className: "app-content", children: debugMode
                    ? (0, jsx_runtime_1.jsx)(DebugGallery_1.DebugGallery, { qaState: qaState, setQaState: setQaState, navigate: navigate })
                    : (0, Templates_1.renderTemplate)(props) }), (0, jsx_runtime_1.jsx)(primitives_1.BottomNavigation, { items: screens_1.mainNavigation, current: debugMode ? '' : activeNav, onNavigate: navigate }), (0, jsx_runtime_1.jsxs)(primitives_1.BottomSheet, { open: statusOpen, title: "Utilidades", onClose: () => setStatusOpen(false), children: [screens_1.utilityNavigation.map((item) => ((0, jsx_runtime_1.jsx)(primitives_1.ListItem, { icon: item.icon, title: item.label, meta: "Utilidade", text: item.route === '16' ? 'Consultar mapas disponíveis.' : item.route === '30' ? 'Biblioteca, PDFs, backup e diagnóstico.' : 'Configurar interface e recursos disponíveis.', onClick: () => {
                            setStatusOpen(false);
                            navigate(item.route);
                        } }, item.route))), debugMode && ((0, jsx_runtime_1.jsxs)("div", { className: "qa-state-control", children: [(0, jsx_runtime_1.jsx)("label", { htmlFor: "qa-state", children: "Estado de QA" }), (0, jsx_runtime_1.jsx)("select", { id: "qa-state", value: qaState, onChange: (event) => { if ((0, screens_1.isQaState)(event.target.value))
                                    setQaState(event.target.value); }, children: screen.availableQaStates.map((state) => (0, jsx_runtime_1.jsx)("option", { value: state, children: screens_1.qaStateLabels[state] ?? state }, state)) })] }))] })] }));
}

});
define("libraryBridgeClient",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.libraryStatusLabel = exports.libraryAuthorityLabel = void 0;
exports.getLibraryBridgeAvailability = getLibraryBridgeAvailability;
exports.getLibraryCapabilities = getLibraryCapabilities;
exports.listLibraryDocumentsThroughBridge = listLibraryDocumentsThroughBridge;
exports.searchLibraryThroughBridge = searchLibraryThroughBridge;
exports.requestPdfImportThroughBridge = requestPdfImportThroughBridge;
exports.updateLibraryDocumentPolicyThroughBridge = updateLibraryDocumentPolicyThroughBridge;
exports.reindexLibraryDocumentThroughBridge = reindexLibraryDocumentThroughBridge;
exports.removeLibraryDocumentThroughBridge = removeLibraryDocumentThroughBridge;
exports.parseLibraryNativeEvent = parseLibraryNativeEvent;
const bridge = () => typeof window !== 'undefined' ? window.BraseiroLibrary : null;
const parse = (v, f) => { try {
    return typeof v === 'string' ? JSON.parse(v) : v ?? f;
}
catch {
    return f;
} };
const libraryAuthorityLabel = (v) => ({ mechanical: 'Mecânica', setting: 'Cenário', reference: 'Referência', supplemental: 'Suplementar' }[v] ?? String(v ?? ''));
exports.libraryAuthorityLabel = libraryAuthorityLabel;
const libraryStatusLabel = (v) => ({ INDEXADO: 'Indexado', INDEXANDO: 'Indexando', ERRO: 'Erro' }[v] ?? String(v ?? ''));
exports.libraryStatusLabel = libraryStatusLabel;
function getLibraryBridgeAvailability() { return bridge() ? 'available' : 'unavailable'; }
;
function getLibraryCapabilities() { const b = bridge(); return { list: !!b?.listDocuments, search: !!b?.searchDocuments, importPdf: !!b?.pickPdf, policy: !!b?.setDocumentPolicy, reindex: !!b?.reindexDocument, remove: !!b?.removeDocument }; }
;
function listLibraryDocumentsThroughBridge() { const b = bridge(); if (!b?.listDocuments)
    return { ok: false, message: 'Biblioteca nativa indisponível.' }; return { ok: true, documents: parse(b.listDocuments(), []) }; }
;
function searchLibraryThroughBridge(q, l = 8) { const b = bridge(); if (!b?.searchDocuments)
    return { ok: false, message: 'Busca nativa indisponível.', results: [], grounded: false }; const x = parse(b.searchDocuments(q, l), {}); return { ok: true, results: x.results ?? [], grounded: !!x.grounded }; }
;
function requestPdfImportThroughBridge() { const b = bridge(); return b?.pickPdf ? parse(b.pickPdf(), { ok: false }) : { ok: false, message: 'Importação nativa indisponível.' }; }
;
function updateLibraryDocumentPolicyThroughBridge(id, a, e) { const b = bridge(); return b?.setDocumentPolicy ? parse(b.setDocumentPolicy(id, a, e), { ok: false }) : { ok: false, message: 'Política nativa indisponível.' }; }
;
function reindexLibraryDocumentThroughBridge(id) { const b = bridge(); return b?.reindexDocument ? parse(b.reindexDocument(id), { ok: false }) : { ok: false, message: 'Reindexação nativa indisponível.' }; }
;
function removeLibraryDocumentThroughBridge(id) { const b = bridge(); return b?.removeDocument ? parse(b.removeDocument(id), { ok: false }) : { ok: false, message: 'Remoção nativa indisponível.' }; }
;
function parseLibraryNativeEvent(raw) { return parse(raw, null); }
;

});
define("geminiBridgeClient",function(module,exports,require){
"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getGeminiBridgeAvailability = getGeminiBridgeAvailability;
exports.getGeminiCapabilities = getGeminiCapabilities;
exports.getGeminiStatusThroughBridge = getGeminiStatusThroughBridge;
exports.configureGeminiApiKeyThroughBridge = configureGeminiApiKeyThroughBridge;
exports.clearGeminiApiKeyThroughBridge = clearGeminiApiKeyThroughBridge;
exports.buildGeminiScenePrompt = buildGeminiScenePrompt;
exports.generateGeminiSceneImageThroughBridge = generateGeminiSceneImageThroughBridge;
exports.requestGeminiSceneImageImportThroughBridge = requestGeminiSceneImageImportThroughBridge;
exports.clearGeminiSceneImageThroughBridge = clearGeminiSceneImageThroughBridge;
exports.parseGeminiNativeEvent = parseGeminiNativeEvent;
const bridge = () => typeof window !== 'undefined' ? window.BraseiroGemini : null;
const parse = (v, f) => { try {
    return typeof v === 'string' ? JSON.parse(v) : v ?? f;
}
catch {
    return f;
} };
function getGeminiBridgeAvailability() { return bridge() ? 'available' : 'unavailable'; }
;
function getGeminiCapabilities() { const b = bridge(); return { status: !!b?.getStatus, configure: !!b?.configureApiKey, clearKey: !!b?.clearApiKey, generate: !!b?.generateSceneImage, importImage: !!b?.pickSceneImage, clearImage: !!b?.clearSceneImage }; }
;
function getGeminiStatusThroughBridge() { const b = bridge(); return b?.getStatus ? parse(b.getStatus(), { configured: false, generating: false }) : { configured: false, generating: false }; }
;
function configureGeminiApiKeyThroughBridge(k) { const b = bridge(); return b?.configureApiKey ? parse(b.configureApiKey(k), { ok: false }) : { ok: false, message: 'Gemini nativo indisponível.' }; }
;
function clearGeminiApiKeyThroughBridge() { const b = bridge(); return b?.clearApiKey ? parse(b.clearApiKey(), { ok: false }) : { ok: false }; }
;
function stableGeminiSceneKey(value) { const text = String(value ?? 'Cena').trim() || 'Cena'; let h = 2166136261; for (let i = 0; i < text.length; i++) { h ^= text.charCodeAt(i); h = Math.imul(h, 16777619); } return `scene_${(h >>> 0).toString(16).padStart(8, '0')}`; }
function buildGeminiScenePrompt(scene) { if (!scene)
    return null; const title = String(scene.title ?? scene.locationName ?? 'Cena').trim(); const narrative = String(scene.narration ?? scene.narrative ?? scene.description ?? '').trim(); const sceneKey = stableGeminiSceneKey(scene.id ?? scene.sceneId ?? title); const prompt = ['Ilustração cinematográfica de Star Wars para VTT.', title, narrative].filter(Boolean).join('\n').slice(0, 5000); return prompt.length >= 20 ? { sceneKey, prompt } : null; }
;
function generateGeminiSceneImageThroughBridge(p) { const b = bridge(); return b?.generateSceneImage && p ? parse(b.generateSceneImage(p.prompt, p.sceneKey), { ok: false }) : { ok: false, message: 'Geração indisponível.' }; }
;
function requestGeminiSceneImageImportThroughBridge(k) { const b = bridge(); return b?.pickSceneImage ? parse(b.pickSceneImage(k), { ok: false }) : { ok: false }; }
;
function clearGeminiSceneImageThroughBridge() { const b = bridge(); return b?.clearSceneImage ? parse(b.clearSceneImage(), { ok: false }) : { ok: false }; }
;
function parseGeminiNativeEvent(raw) { return parse(raw, null); }
;

});
try{const App=req('VTTApp').VTTApp;const root=document.getElementById('root');if(!root)throw new Error('root missing');g.ReactDOM.createRoot(root).render(g.React.createElement(App));document.documentElement.dataset.r46Shell='mounted';}catch(err){console.error(err);document.documentElement.dataset.r46Shell='error';const root=document.getElementById('root');if(root)root.innerHTML='<pre id="r46-boot-error"></pre>',root.querySelector('#r46-boot-error').textContent=String(err&&err.stack||err);}
})(globalThis);
