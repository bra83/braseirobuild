import type { LivingWorldState, OrganizationProject, OrganizationState, StrongholdLootItem, StrongholdState } from './master/types';
import { ITEM_CATALOG } from './itemCatalog';

function clamp(value: number, min: number, max: number): number { return Math.max(min, Math.min(max, Number(value) || 0)); }
function now(): number { return Date.now(); }
function hash(value: string): number { let h = 2166136261; for (let i = 0; i < value.length; i += 1) h = Math.imul(h ^ value.charCodeAt(i), 16777619) >>> 0; return h; }
function uniq(values: string[]): string[] { return Array.from(new Set(values.map(value => String(value || '').trim()).filter(Boolean))); }

const SAFE_LOOT_IDS = [
  'dagger', 'cudgel', 'short-sword', 'sword', 'spear', 'axe', 'bow',
  'leather-shirt', 'leather-corslet', 'cap', 'buckler', 'shield',
  'travel-gear', 'winter-gear', 'rope', 'lantern', 'torch', 'healer-kit', 'craft-tools',
  'tent', 'bedroll', 'cooking-pot', 'provisions', 'waterskin', 'travelling-cloak', 'fine-clothes', 'journal',
];
const LOOT_CATALOG = ITEM_CATALOG.filter(item => SAFE_LOOT_IDS.includes(item.id));

function lootItem(catalogId: string, quantity: number, source: string, rarity: StrongholdLootItem['rarity'] = 'common'): StrongholdLootItem | null {
  const catalog = LOOT_CATALOG.find(item => item.id === catalogId);
  if (!catalog || quantity <= 0) return null;
  return {
    id: `loot_${catalogId}_${hash(`${source}:${catalogId}`)}`,
    catalogId: catalog.id,
    name: catalog.name,
    category: catalog.category,
    quantity: Math.max(1, Math.trunc(quantity)),
    load: Math.max(0, Number(catalog.load) || 0),
    value: catalog.value,
    description: catalog.description,
    tags: [...catalog.tags],
    rarity,
    state: 'stored',
    source,
  };
}

/**
 * Gera apenas bens mundanos compatíveis com O Um Anel. Relíquias, recompensas
 * mágicas e artefatos nunca são inventados aqui: esses itens só podem entrar
 * pela ficção/regras ou por edição explícita do Mestre/jogador.
 */
export function generateStrongholdLoot(input: Pick<StrongholdState, 'id' | 'name' | 'facilities' | 'defenders' | 'supplies' | 'treasure' | 'ownerOrganizationId'>): StrongholdLootItem[] {
  const seed = hash(`${input.id}|${input.name}|${input.ownerOrganizationId || ''}`);
  const defenders = Math.max(0, Math.trunc(input.defenders || 0));
  const facilities = input.facilities.map(item => item.toLowerCase());
  const result: Array<StrongholdLootItem | null> = [];
  const source = `Estoque de ${input.name}`;

  // Aproximadamente metade do estoque é sobrevivência/uso cotidiano.
  const provisionBundles = Math.max(1, Math.min(8, Math.ceil(Math.max(1, input.supplies) / 3)));
  result.push(lootItem('provisions', provisionBundles, source));
  result.push(lootItem('waterskin', 1 + (seed % 3), source));
  result.push(lootItem('torch', 2 + (seed % 5), source));
  result.push(lootItem('rope', 1 + ((seed >>> 3) % 2), source));
  result.push(lootItem('bedroll', Math.max(1, Math.min(4, 1 + ((seed >>> 5) % 4))), source));

  // Ferramentas seguem as instalações — não aparecem só para "dar prêmio".
  if (facilities.some(value => /forja|oficina|ferreiro|artes/.test(value))) result.push(lootItem('craft-tools', 1 + ((seed >>> 7) % 2), source, 'uncommon'));
  if (facilities.some(value => /cura|enferm|ervas|curande/.test(value))) result.push(lootItem('healer-kit', 1, source, 'uncommon'));
  if (facilities.some(value => /estáb|viagem|caravana|depósito|armaz/.test(value))) result.push(lootItem('travel-gear', 1 + ((seed >>> 9) % 2), source));
  if (facilities.some(value => /cozinha|salão|refeit/.test(value))) result.push(lootItem('cooking-pot', 1, source));
  if (facilities.some(value => /acamp|barraca|pátio/.test(value))) result.push(lootItem('tent', 1, source));
  if (facilities.some(value => /arquivo|escrib|bibli|mapa/.test(value))) result.push(lootItem('journal', 1, source, 'uncommon'));

  // Armas/armaduras representam reserva real dos defensores e ficam abaixo
  // do número da guarnição para não transformar cada posto em arsenal.
  if (defenders > 0) {
    const weaponPool = ['spear', 'cudgel', 'bow', 'axe'];
    const weapon = weaponPool[seed % weaponPool.length];
    result.push(lootItem(weapon, Math.max(1, Math.min(4, Math.ceil(defenders / 5))), source));
    if (defenders >= 6) result.push(lootItem((seed & 1) ? 'shield' : 'buckler', Math.max(1, Math.min(2, Math.floor(defenders / 8))), source));
    if (defenders >= 10) result.push(lootItem('leather-shirt', 1, source, 'uncommon'));
  }

  // Um local mais rico pode ter roupa ou arma melhor, mas nunca tesouro mágico.
  if (input.treasure >= 3) result.push(lootItem('fine-clothes', 1, source, 'uncommon'));
  if (input.treasure >= 6 && defenders >= 8) result.push(lootItem((seed & 2) ? 'sword' : 'short-sword', 1, source, 'notable'));

  return result.filter((item): item is StrongholdLootItem => Boolean(item));
}

export function normalizeOrganization(source: Partial<OrganizationState> & Pick<OrganizationState, 'id' | 'name' | 'kind'>, previous?: OrganizationState): OrganizationState {
  return {
    id: source.id,
    name: String(source.name || previous?.name || 'Organização sem nome').trim(),
    kind: source.kind || previous?.kind || 'settlement-council',
    culture: String(source.culture ?? previous?.culture ?? '').trim(),
    agenda: String(source.agenda ?? previous?.agenda ?? '').trim(),
    resources: String(source.resources ?? previous?.resources ?? '').trim(),
    territory: uniq(source.territory ?? previous?.territory ?? []),
    influence: clamp(source.influence ?? previous?.influence ?? 1, 0, 10),
    partyReputation: clamp(source.partyReputation ?? previous?.partyReputation ?? 0, -10, 10),
    members: source.members ?? previous?.members ?? [],
    contracts: source.contracts ?? previous?.contracts ?? [],
    projects: source.projects ?? previous?.projects ?? [],
    relations: source.relations ?? previous?.relations ?? [],
    secrets: source.secrets ?? previous?.secrets ?? [],
    nextAction: String(source.nextAction ?? previous?.nextAction ?? '').trim(),
    active: source.active ?? previous?.active ?? true,
    archived: source.archived ?? previous?.archived ?? false,
    generatedBy: source.generatedBy ?? previous?.generatedBy ?? 'master',
    createdAt: source.createdAt ?? previous?.createdAt ?? now(),
    updatedAt: now(),
  };
}

export function normalizeStronghold(source: Partial<StrongholdState> & Pick<StrongholdState, 'id' | 'name'>, previous?: StrongholdState): StrongholdState {
  const base: StrongholdState = {
    id: source.id,
    name: String(source.name || previous?.name || 'Refúgio sem nome').trim(),
    locationId: String(source.locationId ?? previous?.locationId ?? '').trim(),
    hexId: source.hexId ?? previous?.hexId ?? null,
    ownerOrganizationId: source.ownerOrganizationId ?? previous?.ownerOrganizationId,
    previousOwnerOrganizationId: source.previousOwnerOrganizationId ?? previous?.previousOwnerOrganizationId,
    ownerKind: source.ownerKind ?? previous?.ownerKind ?? ((source.ownerOrganizationId ?? previous?.ownerOrganizationId) ? 'organization' : 'unclaimed'),
    status: source.status ?? previous?.status ?? 'discovered',
    knownToCompany: source.knownToCompany ?? previous?.knownToCompany ?? true,
    autoGenerated: source.autoGenerated ?? previous?.autoGenerated ?? !previous,
    facilities: uniq(source.facilities ?? previous?.facilities ?? []),
    defenders: Math.max(0, Math.trunc(Number(source.defenders ?? previous?.defenders ?? 0))),
    supplies: Math.max(0, Math.trunc(Number(source.supplies ?? previous?.supplies ?? 0))),
    treasure: Math.max(0, Math.trunc(Number(source.treasure ?? previous?.treasure ?? 0))),
    threats: uniq(source.threats ?? previous?.threats ?? []),
    projects: source.projects ?? previous?.projects ?? [],
    loot: source.loot ?? previous?.loot ?? [],
    ownershipHistory: source.ownershipHistory ?? previous?.ownershipHistory ?? [],
    discoveredDay: source.discoveredDay ?? previous?.discoveredDay,
    capturedDay: source.capturedDay ?? previous?.capturedDay,
    lastWorldTickDay: source.lastWorldTickDay ?? previous?.lastWorldTickDay,
    notes: String(source.notes ?? previous?.notes ?? '').trim(),
    updatedAt: now(),
  };
  if (!previous && base.loot.length === 0) base.loot = generateStrongholdLoot(base);
  return base;
}

export function advanceOrganizationProject(world: LivingWorldState, organizationId: string, projectId: string, amount: number, resolution: string): LivingWorldState {
  const organizations = (world.organizations || []).map(organization => {
    if (organization.id !== organizationId) return organization;
    const projects = organization.projects.map(project => {
      if (project.id !== projectId) return project;
      const progress = clamp(project.progress + Math.trunc(amount), 0, Math.max(1, project.target));
      return { ...project, progress, status: progress >= project.target ? 'completed' as const : project.status, updatedAt: now() };
    });
    return { ...organization, projects, updatedAt: now() };
  });
  return {
    ...world,
    organizations,
    activityLog: [...(world.activityLog || []), { id: `activity_org_${now()}_${organizationId}`, kind: 'organization' as const, title: 'Projeto de organização', detail: resolution, day: 0, createdAt: now(), sourceId: organizationId }].slice(-300),
    updatedAt: now(),
  };
}

export function chooseOrganizationAction(organization: OrganizationState, day: number): { title: string; detail: string; project?: OrganizationProject } {
  const activeProject = organization.projects.find(project => project.status === 'active');
  if (activeProject) return { title: `${organization.name} avança ${activeProject.title}`, detail: activeProject.description || activeProject.stakes, project: activeProject };
  const agenda = organization.agenda || 'proteger seus interesses e sua gente';
  const actions = organization.kind === 'ranger-company'
    ? ['vigia uma rota ameaçada', 'compara relatos de viajantes', 'desloca um pequeno grupo de batedores']
    : organization.kind === 'merchant-house' || organization.kind === 'craft-guild'
      ? ['negocia suprimentos escassos', 'protege uma caravana', 'reúne artesãos e mensageiros']
      : organization.kind === 'cult' || organization.kind === 'enemy-band'
        ? ['recruta aliados discretamente', 'testa as defesas locais', 'espalha medo e informação falsa']
        : ['convoca representantes', 'procura apoio entre famílias', 'reage a notícias vindas da estrada'];
  const index = Math.abs((day + organization.id.length + organization.name.length) % actions.length);
  return { title: `${organization.name} ${actions[index]}`, detail: `A ação busca ${agenda}. Recursos conhecidos: ${organization.resources || 'limitados ou não registrados'}.` };
}
