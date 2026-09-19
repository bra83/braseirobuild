import type { CampaignState } from '../db';
import type { SceneAudioPlan } from '../../types';
import { deleteStoreData, getStoreData, getStoreItem, saveStoreData, searchChunks } from '../db';
import {
  loadMapDiscovery,
  loadRuntimeMapManifest,
  loadRuntimeTerraMediaLocations,
  resolveHexNarrativeLocationFromIndex,
  type EriadorHex,
  type TerraMediaLocation,
} from '../map';
import {
  environmentScopeKey,
  resolveRulesProfile,
  rulesProfilePrompt,
  type RulesProfile,
} from './rulesProfile';
import { advanceWeatherState } from './weatherStateMachine';
import type {
  AutonomousEventRecord,
  ClimateZone,
  EncounterOpportunity,
  EnvironmentContext,
  EnvironmentMemoryRecord,
  NearbySettlement,
  RoutePreviewStep,
  SettlementKind,
  TerrainClass,
  WeatherForecast,
  WeatherStage,
} from './types';

interface PersistedEnvironmentState {
  id: string;
  scopeKey: string;
  rulesProfileId: string;
  campaignId: string;
  hexId: string | null;
  worldTimeKey: string;
  weather: WeatherForecast;
  generatedAt: number;
  travelStep: number;
  processedTravelActionIds: string[];
  lastTravelActionId?: string;
  lastSettlementId?: string;
}

interface TerrainReading {
  terrainClass: TerrainClass;
  confidence: number;
  evidence: string[];
  roadEvidence: boolean;
  climateZone: ClimateZone;
  elevationClass: EnvironmentContext['elevationClass'];
}

interface StaticEnvironmentData {
  campaignId: string;
  profile: RulesProfile;
  scopeKey: string;
  currentHexId: string | null;
  worldTimeKey: string;
  season: EnvironmentContext['season'];
  hour: number;
  byId: Map<string, EriadorHex>;
  currentHex?: EriadorHex;
  allLocations: TerraMediaLocation[];
  relevantLocations: TerraMediaLocation[];
  currentLocations: TerraMediaLocation[];
  influential: TerraMediaLocation[];
  terrainReading: TerrainReading;
  route: RoutePreviewStep[];
  routeHexIds: string[];
  possibleWeather: WeatherForecast[];
  nearbySettlements: NearbySettlement[];
  adjacentHexSummary: EnvironmentContext['adjacentHexSummary'];
  evidence: string[];
  humanTrafficLoreSupported: boolean;
  loreEvidence: string[];
}

const TERRAIN_KEYWORDS: Record<TerrainClass, RegExp[]> = {
  snow: [/forochel|forodwaith|ermos do norte|gelo|gelad|neve|snow|ice|glacier|tundra/],
  mountain: [/montanha|montanhas|pico|passo alto|cordilheira|mountain|peak|high pass|ered\b|emyn\b|colinas? do clima/],
  forest: [/floresta|bosque|mata|forest|woods?|chetwood|mirkwood|lothlorien|fangorn|eryn/],
  wetland: [/pantano|pântano|brejo|charco|marsh|swamp|wetland|midgewater|nindalf/],
  desert: [/deserto|arido|árido|harad|haradwaith|khand|desert|dry lands?|waste/],
  coast: [/costa|litoral|baia|baía|golfo|mar\b|oceano|porto|coast|bay|gulf|sea\b|harbour|harbor/],
  fields: [/campo|campos|campina|planicie|planície|pampa|grassland|plain|farmland|rohan|condado|shire|wold/],
  road: [/estrada|caminho|trilha|encruzilhada|road|path|trail|greenway/],
  urban: [/cidade|vila|aldeia|povoado|mercado|city|town|village|settlement/],
  ruins: [/ruina|ruína|arruinad|ruins?|abandonad|barrow|tumulo|túmulo/],
  water: [/rio|lago|água|agua|river|lake|water|sea|ocean/],
  unknown: [],
};

const TERRAIN_LABELS: Record<TerrainClass, string> = {
  snow: 'neve/tundra', mountain: 'montanha/colina', forest: 'floresta', wetland: 'pântano', desert: 'deserto/terra seca',
  coast: 'costa', fields: 'campos/planície', road: 'estrada', urban: 'área urbana', ruins: 'ruínas', water: 'água', unknown: 'terreno não confirmado',
};

function normalize(value: unknown): string {
  return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[_-]+/g, ' ').replace(/\s+/g, ' ').trim();
}

function hashString(value: string): number {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function randomFromSeed(seed: string): () => number {
  let state = hashString(seed) || 0x9e3779b9;
  return () => {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function classifySettlement(value: string): SettlementKind {
  const text = normalize(value);
  if (/cidade portuaria|porto|harbour|harbor|port\b/.test(text)) return 'port';
  if (/metropole|capital|grande cidade|metropolis/.test(text)) return 'city';
  if (/burgo|cidade pequena|town/.test(text)) return 'town';
  if (/cidade|city/.test(text)) return 'city';
  if (/vila|aldeia|povoado|village|settlement/.test(text)) return 'village';
  if (/fortaleza|forte|castelo|cidadela|fortress|fort\b|castle|keep/.test(text)) return 'fortress';
  if (/estalagem|taverna|taberna|pousada|inn|tavern/.test(text)) return 'inn';
  if (/ruina|arruinad|ruins|abandonad/.test(text)) return 'ruin';
  return 'none';
}

function settlementRank(kind: SettlementKind): number {
  return ({ city: 6, port: 5, town: 4, village: 3, fortress: 2, inn: 1, ruin: 0, none: -1 })[kind];
}

function locationSettlementKind(location: TerraMediaLocation): SettlementKind {
  return classifySettlement([location.type, ...location.categories, ...location.tags, location.name, location.description].join(' '));
}

function hexSettlementKind(hex: EriadorHex): SettlementKind {
  return classifySettlement([hex.placeType, hex.poi, hex.name, hex.region, ...(hex.tags || [])].join(' '));
}

function collectHexRings(current: EriadorHex, byId: Map<string, EriadorHex>, maxRing: number): Map<string, number> {
  const distance = new Map<string, number>([[current.id, 0]]);
  const queue = [current.id];
  while (queue.length) {
    const id = queue.shift()!;
    const ring = distance.get(id)!;
    if (ring >= maxRing) continue;
    for (const neighbor of byId.get(id)?.neighbors || []) {
      if (!distance.has(neighbor)) {
        distance.set(neighbor, ring + 1);
        queue.push(neighbor);
      }
    }
  }
  return distance;
}

function environmentalInfluenceRadius(location: TerraMediaLocation): number {
  const text = normalize([location.type, ...location.categories, ...location.tags, location.name].join(' '));
  if (/regiao|reino|terra|region|realm/.test(text)) return 12;
  if (/montanha|cordilheira|pico|mountain|forest|floresta|bosque|pantano|brejo|swamp|marsh/.test(text)) return 6;
  if (/baia|mar|costa|coast|sea|bay|deserto|harad|forochel/.test(text)) return 7;
  if (/estrada|caminho|road|path/.test(text)) return 2;
  return 1;
}

function influentialLocationsForHex(hex: EriadorHex, byId: Map<string, EriadorHex>, locations: TerraMediaLocation[]): Array<{ location: TerraMediaLocation; distance: number; radius: number }> {
  const rings = collectHexRings(hex, byId, 12);
  const result: Array<{ location: TerraMediaLocation; distance: number; radius: number }> = [];
  for (const location of locations) {
    const radius = environmentalInfluenceRadius(location);
    const distances = (location.hexIds || []).map(id => rings.get(id)).filter((value): value is number => value != null);
    if (!distances.length) continue;
    const distance = Math.min(...distances);
    if (distance <= radius) result.push({ location, distance, radius });
  }
  return result.sort((a, b) => a.distance - b.distance || b.radius - a.radius);
}

function scoreTerrainText(scores: Record<TerrainClass, number>, text: string, weight: number): void {
  const normalized = normalize(text);
  for (const [terrainClass, patterns] of Object.entries(TERRAIN_KEYWORDS) as Array<[TerrainClass, RegExp[]]>) {
    if (patterns.some(pattern => pattern.test(normalized))) scores[terrainClass] += weight;
  }
}

function climateFromTerrain(terrainClass: TerrainClass, text: string, elevation: EnvironmentContext['elevationClass']): ClimateZone {
  const normalized = normalize(text);
  if (terrainClass === 'snow' || /forochel|forodwaith|ermos do norte/.test(normalized)) return 'arctic';
  if (terrainClass === 'mountain' || elevation === 'mountain') return 'alpine';
  if (terrainClass === 'wetland') return 'wetland';
  if (terrainClass === 'forest') return 'forest';
  if (terrainClass === 'coast') return 'coastal';
  if (terrainClass === 'desert') return 'dry';
  if (/angmar|rhudaur|arthedain|norte|northern/.test(normalized)) return 'cold-temperate';
  return 'temperate';
}

function readTerrain(hex: EriadorHex, byId: Map<string, EriadorHex>, allLocations: TerraMediaLocation[], extraText = ''): TerrainReading {
  const scores = Object.fromEntries(Object.keys(TERRAIN_KEYWORDS).map(key => [key, 0])) as Record<TerrainClass, number>;
  const evidence: string[] = [];
  const directText = [hex.terrain, hex.region, hex.name, hex.poi, hex.placeType, ...(hex.tags || []), extraText].join(' ');
  scoreTerrainText(scores, directText, 9);
  if (hex.terrain && normalize(hex.terrain) !== 'unknown') evidence.push(`terreno explícito do manifesto: ${hex.terrain}`);

  const influence = influentialLocationsForHex(hex, byId, allLocations);
  for (const item of influence.slice(0, 24)) {
    const exact = item.distance === 0;
    const weight = exact ? 8 : Math.max(0.7, 4.5 * (1 - item.distance / Math.max(1, item.radius + 1)));
    const locationText = [item.location.name, item.location.type, ...item.location.categories, ...item.location.regions, ...item.location.tags, item.location.description].join(' ');
    scoreTerrainText(scores, locationText, weight);
    if (exact || weight >= 2.5) evidence.push(`${item.location.name} (${exact ? 'no hex' : `influência a ${item.distance} hex`})`);
  }

  // Assentamentos e ruínas descrevem ocupação, não substituem automaticamente o terreno natural.
  const naturalOrder: TerrainClass[] = ['snow', 'mountain', 'forest', 'wetland', 'desert', 'coast', 'fields', 'water', 'road', 'urban', 'ruins'];
  if (!Object.values(scores).some(value => value > 0)) scores.fields = 0.35;
  const ranked = naturalOrder.map(key => ({ key, score: scores[key] })).sort((a, b) => b.score - a.score);
  let terrainClass = ranked[0].score > 0 ? ranked[0].key : 'unknown';
  const exactSettlement = [hexSettlementKind(hex), ...allLocations.filter(location => location.hexIds.includes(hex.id)).map(locationSettlementKind)]
    .sort((a, b) => settlementRank(b) - settlementRank(a))[0] || 'none';
  if (exactSettlement !== 'none' && exactSettlement !== 'ruin' && ranked[0].score < 2) terrainClass = 'urban';
  if (exactSettlement === 'ruin' && ranked[0].score < 2) terrainClass = 'ruins';

  const top = ranked[0].score;
  const second = ranked[1]?.score || 0;
  const confidence = top <= 0 ? 0.2 : Math.max(0.32, Math.min(0.96, 0.48 + (top - second) / Math.max(3, top + second)));
  const terrainText = `${directText} ${influence.slice(0, 12).map(item => item.location.name).join(' ')}`;
  const elevationClass: EnvironmentContext['elevationClass'] = terrainClass === 'mountain' ? 'mountain'
    : /colina|planalto|upland|downs|highland/.test(normalize(terrainText)) ? 'upland'
      : ['fields', 'wetland', 'coast', 'desert'].includes(terrainClass) ? 'lowland' : 'unknown';
  const climateZone = climateFromTerrain(terrainClass, terrainText, elevationClass);
  const roadEvidence = scores.road >= 4.5 || /estrada|caminho|road|path|trail/.test(normalize(directText));
  return { terrainClass, confidence, evidence: Array.from(new Set(evidence)).slice(0, 8), roadEvidence, climateZone, elevationClass };
}

function isSnowEligible(zone: ClimateZone, elevation: EnvironmentContext['elevationClass'], season: EnvironmentContext['season'], terrainClass: TerrainClass): boolean {
  if (terrainClass === 'snow' || zone === 'arctic') return true;
  if (zone === 'alpine') return season !== 'summer';
  if (elevation === 'mountain') return season === 'winter' || season === 'autumn' || season === 'spring';
  return season === 'winter' && (zone === 'cold-temperate' || zone === 'forest' || zone === 'temperate');
}

function weatherCandidate(
  kind: WeatherForecast['kind'], intensity: WeatherForecast['intensity'], probability: number, reason: string,
  random: () => number, eligible = true,
): WeatherForecast {
  return {
    id: `weather_${kind}_${Math.round(probability * 1000)}_${Math.round(random() * 9999)}`,
    kind,
    intensity,
    probability: Math.max(0, Math.min(1, probability)),
    reason,
    eligible,
    stage: kind === 'clear' || kind === 'cloudy' ? 'stable' : 'indications',
    stageStep: 0,
    activeStepsRemaining: 0,
  };
}

function buildWeatherCandidates(
  zone: ClimateZone, elevation: EnvironmentContext['elevationClass'], terrainClass: TerrainClass,
  season: EnvironmentContext['season'], hour: number, snowEligible: boolean, route: RoutePreviewStep[], seed: string,
): WeatherForecast[] {
  const random = randomFromSeed(seed);
  const routeMountain = route.some(step => step.terrainClass === 'mountain' || step.terrainClass === 'snow');
  const routeWet = route.some(step => step.terrainClass === 'wetland' || step.terrainClass === 'coast' || step.terrainClass === 'forest');
  const nightFog = hour < 7 || hour > 20 ? 0.12 : 0.04;
  const candidates: WeatherForecast[] = [
    weatherCandidate('clear', 'light', 0.31, 'Tempo estável permanece uma possibilidade real.', random),
    weatherCandidate('cloudy', 'light', 0.24, 'Nebulosidade pode aumentar sem produzir precipitação.', random),
    weatherCandidate('wind', elevation === 'mountain' || zone === 'coastal' ? 'heavy' : 'medium', elevation === 'mountain' ? 0.30 : zone === 'coastal' ? 0.25 : 0.10, 'Exposição do relevo favorece mudança do vento.', random),
    weatherCandidate('fog', 'medium', zone === 'wetland' ? 0.32 : zone === 'forest' ? 0.17 : nightFog, 'Umidade, vegetação e horário permitem névoa.', random),
  ];
  if (routeMountain) candidates.push(weatherCandidate('wind', 'heavy', 0.19, 'A rota entra em terreno elevado e exposto.', random));
  if (zone === 'wetland' || zone === 'forest' || zone === 'coastal' || zone === 'temperate' || zone === 'cold-temperate' || routeWet) {
    const rainBase = zone === 'wetland' ? 0.34 : zone === 'coastal' ? 0.28 : zone === 'forest' ? 0.24 : 0.17;
    candidates.push(weatherCandidate('rain', season === 'autumn' ? 'heavy' : 'medium', rainBase, 'Umidade regional e terreno futuro permitem chuva.', random));
    candidates.push(weatherCandidate('storm', 'heavy', rainBase * 0.25, 'Uma frente forte é possível, mas não começa ativa.', random));
  }
  if (snowEligible) {
    const snowBase = zone === 'arctic' ? 0.42 : zone === 'alpine' ? 0.30 : season === 'winter' ? 0.18 : 0.07;
    candidates.push(weatherCandidate('snow', season === 'winter' || zone === 'arctic' ? 'medium' : 'light', snowBase, 'Frio, estação, altitude ou terreno permitem neve.', random));
    const blizzardBase = zone === 'arctic' && season === 'winter' ? 0.20 : zone === 'alpine' && season === 'winter' ? 0.14 : zone === 'alpine' ? 0.055 : 0.02;
    candidates.push(weatherCandidate('blizzard', 'heavy', blizzardBase, 'Nevasca é apenas uma possibilidade gradual e exige progressão por deslocamentos.', random));
  } else {
    candidates.push(weatherCandidate('snow', 'light', 0, 'Neve bloqueada por terreno, clima, estação ou altitude.', random, false));
    candidates.push(weatherCandidate('blizzard', 'heavy', 0, 'Nevasca bloqueada por terreno, clima, estação ou altitude.', random, false));
  }
  if (zone === 'dry' || terrainClass === 'desert') candidates.push(weatherCandidate('wind', 'heavy', 0.29, 'Área seca favorece vento e poeira; isso não autoriza neve.', random));
  return candidates.sort((a, b) => b.probability - a.probability);
}

function weightedWeather(candidates: WeatherForecast[], seed: string): WeatherForecast {
  const random = randomFromSeed(seed);
  const eligible = candidates.filter(candidate => candidate.eligible && candidate.probability > 0);
  const total = eligible.reduce((sum, candidate) => sum + candidate.probability, 0) || 1;
  let cursor = random() * total;
  for (const candidate of eligible) {
    cursor -= candidate.probability;
    if (cursor <= 0) return structuredClone(candidate);
  }
  return structuredClone(eligible[0] || candidates[0]);
}

function clearWeather(seed: string): WeatherForecast {
  const random = randomFromSeed(seed);
  return { ...weatherCandidate('clear', 'light', 1, 'Estado climático estável.', random), stage: 'stable', stageStep: 0, activeStepsRemaining: 0 };
}


function routeTransition(previous: TerrainClass, next: TerrainClass): string | undefined {
  if (previous === next || next === 'unknown') return undefined;
  return `${TERRAIN_LABELS[previous]} → ${TERRAIN_LABELS[next]}`;
}

function validatePlannedRoute(current: EriadorHex, planned: string[], byId: Map<string, EriadorHex>): string[] {
  const result: string[] = [];
  let previous = current;
  for (const id of planned) {
    if (id === current.id) continue;
    const next = byId.get(id);
    if (!next || !next.playable || !previous.neighbors.includes(next.id)) break;
    result.push(next.id);
    previous = next;
    if (result.length >= 5) break;
  }
  return result;
}

function buildRoutePreview(current: EriadorHex, planned: string[], byId: Map<string, EriadorHex>, locations: TerraMediaLocation[], extraText: string): RoutePreviewStep[] {
  const ids = validatePlannedRoute(current, planned, byId);
  const result: RoutePreviewStep[] = [];
  let previousReading = readTerrain(current, byId, locations, extraText);
  for (let index = 0; index < ids.length; index += 1) {
    const hex = byId.get(ids[index])!;
    const localLocations = locations.filter(location => location.hexIds.includes(hex.id));
    const reading = readTerrain(hex, byId, locations);
    const settlementKind = [hexSettlementKind(hex), ...localLocations.map(locationSettlementKind)].sort((a, b) => settlementRank(b) - settlementRank(a))[0] || 'none';
    result.push({
      step: index + 1,
      hexId: hex.id,
      terrain: normalize(hex.terrain) !== 'unknown' ? hex.terrain : TERRAIN_LABELS[reading.terrainClass],
      terrainClass: reading.terrainClass,
      previousTerrainClass: previousReading.terrainClass,
      transition: routeTransition(previousReading.terrainClass, reading.terrainClass),
      region: resolveHexNarrativeLocationFromIndex(hex, byId, locations).regionName,
      locations: localLocations.map(location => location.name).slice(0, 6),
      settlementKind,
      roadEvidence: reading.roadEvidence,
      confidence: reading.confidence,
    });
    previousReading = reading;
  }
  return result;
}

function shortestDistance(from: EriadorHex, targetHexId: string, byId: Map<string, EriadorHex>, max = 8): number | undefined {
  return collectHexRings(from, byId, max).get(targetHexId);
}

function movementTrendForSettlement(settlement: NearbySettlement, current: EriadorHex, futureRoute: RoutePreviewStep[], previousHex: EriadorHex | undefined, byId: Map<string, EriadorHex>): NearbySettlement['movementTrend'] {
  const futureDistances = futureRoute.map(step => byId.get(step.hexId)).filter((hex): hex is EriadorHex => Boolean(hex))
    .map(hex => shortestDistance(hex, settlement.hexId, byId, 8)).filter((value): value is number => value != null);
  if (futureDistances.length) {
    if (futureDistances[0] < settlement.distanceHexes || futureDistances.at(-1)! < settlement.distanceHexes) return 'approaching';
    if (futureDistances[0] > settlement.distanceHexes) return 'leaving';
  }
  if (!previousHex) return settlement.distanceHexes <= 2 ? 'nearby' : 'unknown';
  const previousDistance = shortestDistance(previousHex, settlement.hexId, byId, 8);
  if (previousDistance == null) return 'unknown';
  if (settlement.distanceHexes < previousDistance) return 'approaching';
  if (settlement.distanceHexes > previousDistance) return 'leaving';
  return 'nearby';
}

function nearbySettlements(current: EriadorHex, byId: Map<string, EriadorHex>, locations: TerraMediaLocation[], futureRoute: RoutePreviewStep[], previousHex?: EriadorHex): NearbySettlement[] {
  const rings = collectHexRings(current, byId, 8);
  const result = new Map<string, NearbySettlement>();
  for (const location of locations) {
    const kind = locationSettlementKind(location);
    if (kind === 'none' || kind === 'ruin') continue;
    for (const hexId of location.hexIds || []) {
      const distanceHexes = rings.get(hexId);
      if (distanceHexes == null || distanceHexes > 5) continue;
      const candidate: NearbySettlement = { id: location.id, name: location.name, kind, hexId, distanceHexes, confidence: Number(location.placement?.confidence || 0.5) };
      const stored = result.get(candidate.id);
      if (!stored || candidate.distanceHexes < stored.distanceHexes) result.set(candidate.id, candidate);
    }
  }
  for (const [hexId, distanceHexes] of rings) {
    if (distanceHexes > 5) continue;
    const hex = byId.get(hexId);
    if (!hex) continue;
    const kind = hexSettlementKind(hex);
    if (kind === 'none' || kind === 'ruin') continue;
    const name = hex.name || hex.poi || hex.region || hex.id;
    const id = `hex-settlement:${hex.id}:${normalize(name)}`;
    if (!result.has(id)) result.set(id, { id, name, kind, hexId, distanceHexes, confidence: Number(hex.confidence || 0.45) });
  }
  return Array.from(result.values()).map(settlement => ({
    ...settlement,
    movementTrend: movementTrendForSettlement(settlement, current, futureRoute, previousHex, byId),
  })).sort((a, b) => {
    const trend = (value: NearbySettlement['movementTrend']) => value === 'approaching' ? 3 : value === 'nearby' ? 2 : value === 'unknown' ? 1 : 0;
    return trend(b.movementTrend) - trend(a.movementTrend) || a.distanceHexes - b.distanceHexes || settlementRank(b.kind) - settlementRank(a.kind) || b.confidence - a.confidence;
  }).slice(0, 10);
}

function weatherStageLabel(stage: WeatherStage | undefined): string {
  return ({ stable: 'estável', indications: 'indícios', forming: 'em formação', active: 'ativo', dissipating: 'dissipando' } as Record<WeatherStage, string>)[stage || 'stable'];
}

function weatherDescription(weather: WeatherForecast): string {
  const labels: Record<WeatherForecast['kind'], string> = { clear: 'tempo aberto', cloudy: 'céu encoberto', rain: 'chuva', storm: 'tempestade', snow: 'neve', blizzard: 'nevasca', fog: 'névoa', wind: 'vento' };
  const intensity = weather.intensity === 'heavy' ? 'forte' : weather.intensity === 'medium' ? 'moderado' : 'leve';
  return `${labels[weather.kind]} ${intensity} — ${weatherStageLabel(weather.stage)}`;
}

function approachSignalPool(settlement: NearbySettlement | undefined, hour: number, weather: WeatherForecast, roadEvidence: boolean, humanTrafficLoreSupported: boolean): string[] {
  if (!settlement || settlement.movementTrend === 'leaving') return [];
  const name = settlement.name;
  const activeBadWeather = weather.stage === 'active' && ['storm', 'blizzard'].includes(weather.kind);
  const day = hour >= 6 && hour < 20;
  const distance = settlement.distanceHexes;
  if (!humanTrafficLoreSupported) {
    if (distance === 0) return [`A Companhia alcançou ${name}; descreva apenas elementos confirmados pelo atlas e pela cena atual.`];
    if (distance >= 4) return [`A influência remota de ${name} aparece apenas por marcos de rota, trilhas convergentes ou sinais geográficos confirmados.`];
    if (distance === 3) return [`A aproximação de ${name} torna os caminhos mais definidos, sem presumir tráfego humano não sustentado pelas fontes do perfil.`];
    if (distance === 2) return [`A dois hexes de ${name}, fumaça, luzes ou construções distantes podem ser sugeridas somente quando compatíveis com o atlas.`];
    return [`Nos arredores de ${name}, cercas, campos, caminhos secundários ou ocupação podem aparecer quando confirmados; não presuma mercadores, patrulhas ou carroças.`];
  }
  if (distance === 0) return [
    `A Companhia alcançou ${name}: portões, guardas, movimento urbano e ambiente sonoro direto substituem os prenúncios.`,
    `Dentro de ${name}, o tráfego deixa de ser remoto; pessoas, ofícios e vigilância pertencem à cena imediata.`,
  ];
  if (distance >= 4) {
    const distanceLabel = distance === 5 ? 'cinco' : 'quatro';
    return [
      `A ${distanceLabel} hexes de ${name}, apenas tráfego remoto, pegadas convergentes e marcos de rota são plausíveis.`,
      roadEvidence ? `A estrada traz sinais antigos de rodas e passagem na direção de ${name}, sem exigir encontro.` : `Sem estrada confirmada, a influência de ${name} aparece como rumores, fumaça muito distante ou trilhas convergentes.`,
    ];
  }
  if (distance === 3) return [
    `A três hexes de ${name}, carroças e viajantes podem aparecer com maior frequência, sobretudo seguindo a mesma direção.`,
    activeBadWeather ? `O mau tempo reduz o tráfego vindo de ${name}; sinais de abrigo e viajantes apressados são mais adequados que uma estrada cheia.` : `Rastros recentes e fluxo ocasional sugerem a aproximação de ${name}.`,
  ];
  if (distance === 2) return [
    `A dois hexes de ${name}, fumaça, vozes, rodas ou sinos distantes podem alcançar a rota.`,
    day ? `Durante o dia, mensageiros e pequenas carroças tornam-se plausíveis nos acessos de ${name}.` : `À noite, luzes, cães, sinos de vigia ou fogueiras distantes são mais plausíveis que um mercado ativo.`,
  ];
  return [
    `A um hex de ${name}, campos, cercas, patrulhas, mercadores e caminhos secundários podem marcar os arredores.`,
    activeBadWeather ? `Nos arredores de ${name}, o clima forte concentra viajantes em busca de abrigo e reduz o comércio ao ar livre.` : `Os acessos de ${name} já permitem tráfego local e sinais claros de ocupação.`,
  ];
}

function probability(value: number): number { return Math.max(0, Math.min(0.95, value)); }

function opportunity(id: string, category: EncounterOpportunity['category'], title: string, description: string, trigger: string, probabilityValue: number, requiredConditions: string[], ambienceLayers: string[] = []): EncounterOpportunity {
  return { id, category, title, description, trigger, probability: probability(probabilityValue), requiredConditions, ambienceLayers };
}

function terrainOpportunityPool(terrain: TerrainClass, hour: number, weather: WeatherForecast): EncounterOpportunity[] {
  const night = hour < 6 || hour >= 20;
  const harsh = weather.stage === 'active' && ['storm', 'blizzard', 'snow'].includes(weather.kind);
  const common = {
    forest: [
      opportunity('fauna-forest-birds', 'fauna', 'Aves e pequenos animais', 'Movimento na copa, cantos interrompidos ou animais fugindo podem revelar mudanças ao redor.', 'Use quando a Companhia observar ou escutar a floresta.', night ? 0.20 : 0.46, ['floresta'], ['forest']),
      opportunity('tracks-forest-game', 'tracks', 'Rastros de caça', 'Pegadas, pelos ou galhos partidos podem indicar caça recente sem obrigar um encontro.', 'Use após observação do chão ou uma ação de Caçada/Exploração.', 0.34, ['floresta']),
      opportunity('resource-forest-herbs', 'resource', 'Ervas, lenha e abrigo natural', 'A vegetação pode oferecer recursos modestos, sujeitos às regras e testes apropriados.', 'Só conceda benefício mecânico após resolução formal aplicável.', 0.28, ['floresta']),
      opportunity('explore-forest-clearing', 'exploration', 'Clareira ou trilha esquecida', 'Uma abertura, árvore marcada ou trilha antiga pode convidar à exploração opcional.', 'Apresente como escolha, nunca desvio obrigatório.', 0.24, ['floresta']),
    ],
    mountain: [
      opportunity('risk-mountain-scree', 'hazard', 'Pedras soltas e passagem exposta', 'O relevo pode anunciar risco de queda, frio ou avanço lento.', 'Consequências mecânicas somente por Evento de Jornada ou teste formal.', harsh ? 0.52 : 0.36, ['montanha']),
      opportunity('fauna-mountain-goats', 'fauna', 'Animais das encostas', 'Cabras, aves de rapina ou silhuetas distantes podem reforçar a altitude.', 'Use como sinal ambiental ou pista.', 0.30, ['montanha']),
      opportunity('resource-mountain-shelter', 'resource', 'Abrigo entre rochas', 'Uma saliência ou cavidade pode oferecer uma opção de descanso ou observação.', 'Não declare segurança sem inspeção.', 0.26, ['montanha']),
      opportunity('explore-mountain-overlook', 'exploration', 'Ponto de observação', 'Uma elevação pode revelar parte da rota futura.', 'Ofereça como oportunidade opcional.', 0.31, ['montanha']),
    ],
    snow: [
      opportunity('tracks-snow-clear', 'tracks', 'Rastros nítidos na neve', 'Pegadas recentes podem revelar direção, quantidade e ritmo de viajantes ou animais.', 'Exige observação; o clima pode apagar as marcas.', harsh ? 0.42 : 0.55, ['neve']),
      opportunity('risk-snow-visibility', 'hazard', 'Visibilidade e frio', 'Vento e neve podem prenunciar orientação difícil e exposição.', 'Não aplique Fadiga ou Ferimento fora da resolução de Jornada.', harsh ? 0.60 : 0.31, ['neve']),
      opportunity('resource-snow-shelter', 'resource', 'Quebra-vento ou abrigo', 'Rochas, árvores baixas ou construções podem oferecer proteção parcial.', 'A proteção deve ser verificada pela ação dos heróis.', 0.27, ['neve']),
    ],
    wetland: [
      opportunity('fauna-wetland', 'fauna', 'Aves, insetos e anfíbios', 'Mudanças nos sons do pântano podem indicar clima, água profunda ou presença próxima.', 'Use como leitura ambiental.', 0.46, ['pântano'], ['swamp']),
      opportunity('risk-wetland-ground', 'hazard', 'Solo traiçoeiro', 'Água escura, lama ou vegetação flutuante podem ocultar terreno ruim.', 'Só produza consequência mecânica por Evento de Jornada ou teste.', 0.44, ['pântano']),
      opportunity('resource-wetland-water', 'resource', 'Água e plantas úteis', 'Há recursos possíveis, mas a qualidade e segurança precisam ser verificadas.', 'Não conceda provisões automaticamente.', 0.22, ['pântano']),
    ],
    desert: [
      opportunity('tracks-desert-wind', 'tracks', 'Rastros no solo seco', 'Pegadas e marcas de rodas podem sobreviver ou desaparecer com o vento.', 'Use quando houver busca ou observação.', 0.31, ['terra seca']),
      opportunity('risk-desert-exposure', 'hazard', 'Exposição e falta de água', 'Calor, vento e distância entre abrigos podem anunciar dificuldade.', 'Não aplique penalidade sem regra/teste correspondente.', 0.43, ['terra seca']),
      opportunity('resource-desert-water', 'resource', 'Sinais de água ou sombra', 'Vegetação, aves ou depressões podem sugerir recursos.', 'Apresente como pista, não como descoberta garantida.', 0.20, ['terra seca']),
    ],
    coast: [
      opportunity('fauna-coast-birds', 'fauna', 'Aves costeiras', 'Bandos, gritos e direção de voo podem indicar costa, pesca ou mudança do tempo.', 'Use como sinal ambiental.', 0.43, ['costa'], ['river']),
      opportunity('risk-coast-tide', 'hazard', 'Maré, vento e terreno escorregadio', 'A costa pode alterar caminhos e visibilidade.', 'Consequências apenas após resolução formal.', 0.30, ['costa']),
      opportunity('resource-coast-driftwood', 'resource', 'Madeira, água e pesca', 'A costa pode oferecer recursos modestos em local adequado.', 'Exige ação e teste quando aplicável.', 0.29, ['costa']),
    ],
    fields: [
      opportunity('fauna-fields', 'fauna', 'Lebres, aves e rebanhos distantes', 'A vida dos campos pode revelar ocupação, estação ou perturbação recente.', 'Use como sinal, não encontro obrigatório.', 0.40, ['campos']),
      opportunity('tracks-fields-road', 'tracks', 'Pegadas e rodas no terreno aberto', 'Marcas convergentes podem apontar estrada, fazenda ou assentamento.', 'Use quando a rota ou observação justificar.', 0.35, ['campos']),
      opportunity('resource-fields-forage', 'resource', 'Água, ervas ou abrigo rural', 'Recursos podem existir em cursos d’água, sebes ou propriedades.', 'Respeite propriedade e testes; não conceda automaticamente.', 0.25, ['campos']),
      opportunity('explore-fields-landmark', 'exploration', 'Marco visível à distância', 'Uma colina, pedra, torre ou árvore solitária pode convidar a um desvio opcional.', 'Não altere a rota sem escolha do jogador.', 0.27, ['campos']),
    ],
  } as Partial<Record<TerrainClass, EncounterOpportunity[]>>;
  return common[terrain] || [];
}

function settlementOpportunityPool(settlement: NearbySettlement | undefined, hour: number, weather: WeatherForecast, roadEvidence: boolean, humanTrafficLoreSupported: boolean): EncounterOpportunity[] {
  if (!settlement || settlement.movementTrend === 'leaving' || settlement.distanceHexes > 5) return [];
  if (!humanTrafficLoreSupported) return [];
  const distance = settlement.distanceHexes;
  const day = hour >= 6 && hour < 20;
  const severe = weather.stage === 'active' && ['storm', 'blizzard'].includes(weather.kind);
  const routeFactor = roadEvidence ? 1 : 0.55;
  const proximity = Math.max(0.15, (6 - distance) / 6);
  const conditions = [`${distance} hex(es) de ${settlement.name}`, settlement.movementTrend || 'tendência desconhecida'];
  const items: EncounterOpportunity[] = [];
  if (distance > 0 && roadEvidence && day && !severe) {
    items.push(opportunity(`merchant:${settlement.id}`, 'merchant', `Mercadores a caminho de ${settlement.name}`, 'Uma pequena comitiva comercial pode cruzar a rota, mas não precisa fazê-lo.', 'Use somente se a narração decidir materializar a possibilidade; benefício mecânico exige Evento de Jornada apropriado.', 0.44 * proximity * routeFactor, [...conditions, 'estrada', 'dia'], ['distant-wagons']));
    items.push(opportunity(`wagon:${settlement.id}`, 'wagon', 'Carroça de abastecimento', 'Rodas e animais podem ser primeiro percebidos ao longe antes de qualquer encontro.', 'Enquanto distante, trate como ambiência; SFX pontual só se a carroça aparecer no texto.', 0.48 * proximity, [...conditions, 'estrada'], ['distant-wagons']));
  }
  if (distance <= 4 && roadEvidence) items.push(opportunity(`messenger:${settlement.id}`, 'messenger', 'Mensageiro na estrada', 'Um cavaleiro ou corredor solitário pode viajar entre a localidade e regiões próximas.', 'Apresente como possibilidade com direção e urgência coerentes.', 0.28 * proximity, [...conditions, 'estrada'], ['distant-road-traffic']));
  if (distance <= 1 && ['city', 'town', 'fortress', 'port'].includes(settlement.kind)) items.push(opportunity(`patrol:${settlement.id}`, 'patrol', `Patrulha dos arredores de ${settlement.name}`, 'Guardas, vigias ou moradores armados podem percorrer os acessos.', 'A atitude depende da localidade, horário, clima e ações da Companhia.', (severe ? 0.18 : 0.36) * proximity, conditions, ['outskirts']));
  if (distance <= 2) items.push(opportunity(`resident:${settlement.id}`, 'resident', 'Moradores dos arredores', 'Camponeses, pastores, lenhadores ou trabalhadores podem aparecer perto de suas atividades.', 'Não os transforme automaticamente em fonte de informação.', (day ? 0.42 : 0.16) * proximity, conditions, ['outskirts']));
  if (distance <= 3) items.push(opportunity(`guide:${settlement.id}`, 'guide', 'Guia ou viajante conhecedor', 'Alguém familiarizado com os arredores pode surgir, sobretudo em rotas difíceis.', 'Ajuda mecânica somente após interação e regra aplicável.', 0.19 * proximity, conditions));
  if (distance <= 5) items.push(opportunity(`traveller:${settlement.id}`, 'traveller', 'Viajantes na mesma direção', 'Pessoas isoladas ou pequenos grupos podem compartilhar parte do caminho.', 'Use como cor local ou encontro opcional.', (severe ? 0.13 : 0.31) * proximity * routeFactor, conditions, ['distant-road-traffic']));
  return items;
}

async function loadEnvironmentMemory(scopeKey: string): Promise<EnvironmentMemoryRecord[]> {
  return (await getStoreData<EnvironmentMemoryRecord>('environment_memory').catch(() => [])).filter(item => item.id.startsWith(`${scopeKey}:`));
}

function rotateOpportunities(items: EncounterOpportunity[], memory: EnvironmentMemoryRecord[], seed: string, limit = 8): EncounterOpportunity[] {
  const memoryByKey = new Map(memory.map(item => [item.key, item]));
  const random = randomFromSeed(seed);
  return items.map(item => {
    const used = memoryByKey.get(item.id);
    const ageHours = used ? (Date.now() - used.lastUsedAt) / 3_600_000 : 999;
    const repetitionPenalty = used ? Math.min(0.75, 0.18 * used.useCount + (ageHours < 6 ? 0.45 : ageHours < 24 ? 0.22 : 0)) : 0;
    return { item, score: item.probability - repetitionPenalty + random() * 0.08 };
  }).sort((a, b) => b.score - a.score).slice(0, limit).map(entry => entry.item);
}

function ambienceForWeather(weather: WeatherForecast): string[] {
  const stage = weather.stage || 'stable';
  if (stage === 'stable') return weather.kind === 'wind' ? ['soft-wind'] : [];
  if (stage === 'indications') return ['soft-wind'];
  if (stage === 'forming') {
    if (weather.kind === 'snow' || weather.kind === 'blizzard') return ['wind', 'light-snow'];
    if (weather.kind === 'rain' || weather.kind === 'storm') return ['wind', 'light-rain'];
    return ['wind'];
  }
  if (stage === 'dissipating') return ['soft-wind'];
  switch (weather.kind) {
    case 'rain': return ['rain'];
    case 'storm': return ['rain', 'wind', 'thunder'];
    case 'snow': return ['wind', 'snow'];
    case 'blizzard': return ['heavy-wind', 'snowstorm'];
    case 'fog': return ['mist', 'quiet'];
    case 'wind': return ['wind'];
    case 'cloudy': return ['soft-wind'];
    default: return [];
  }
}

function distantAmbienceForSettlement(settlement: NearbySettlement | undefined, roadEvidence: boolean, weather: WeatherForecast, humanTrafficLoreSupported: boolean): string[] {
  if (!settlement || settlement.movementTrend === 'leaving' || settlement.distanceHexes === 0 || settlement.distanceHexes > 5) return [];
  if (!humanTrafficLoreSupported) return settlement.distanceHexes <= 2 ? ['distant-settlement'] : [];
  if (weather.stage === 'active' && ['storm', 'blizzard'].includes(weather.kind)) return settlement.distanceHexes <= 1 ? ['outskirts-shelter'] : [];
  if (settlement.distanceHexes >= 4) return roadEvidence ? ['distant-road-traffic'] : [];
  if (settlement.distanceHexes === 3) return roadEvidence ? ['distant-wagons'] : ['distant-settlement'];
  if (settlement.distanceHexes === 2) return ['distant-settlement'];
  return ['outskirts'];
}

async function computeStaticEnvironment(campaign: CampaignState): Promise<StaticEnvironmentData> {
  const profile = resolveRulesProfile(campaign);
  const scopeKey = environmentScopeKey(campaign);
  const loreChunks = profile.systemId === 'unresolved' ? [] : await searchChunks(
    ['comércio viajantes habitantes estrada encontro fortuito mercadores carroças mensageiros patrulhas'],
    [profile.id, profile.systemId, profile.settingId, profile.displayName],
    'narrative',
  ).catch(() => []);
  const loreText = normalize(loreChunks.slice(0, 12).map(chunk => chunk.text).join(' '));
  const humanTrafficLoreSupported = loreChunks.length > 0 && /comerc|viajant|habitante|estrada|encontro fortuito|mercador|carroca|mensage|patrulh/.test(loreText);
  const loreEvidence = loreChunks.slice(0, 4).map(chunk => `${chunk.manualName}, ${chunk.pageLabel || `página ${chunk.pageNum}`}`);
  const [manifest, discovery, allLocations] = await Promise.all([loadRuntimeMapManifest(), loadMapDiscovery(campaign.id), loadRuntimeTerraMediaLocations()]);
  const master = campaign.masterState;
  const season = master?.worldClock.season || 'spring';
  const hour = master?.worldClock.hour ?? 8;
  const day = master?.worldClock.day ?? 1;
  const year = master?.worldClock.year ?? 2965;
  const worldTimeKey = `${year}:${season}:${day}:${Math.floor(hour / 3)}`;
  const currentHexId = discovery.currentHexId || master?.position.currentHexId || null;
  const byId = new Map((manifest?.hexes || []).map(hex => [hex.id, hex]));
  const currentHex = currentHexId ? byId.get(currentHexId) : undefined;
  const emptyReading: TerrainReading = { terrainClass: 'unknown', confidence: 0, evidence: [], roadEvidence: false, climateZone: 'unknown', elevationClass: 'unknown' };
  if (!manifest || !currentHex) {
    return {
      campaignId: campaign.id, profile, scopeKey, currentHexId, worldTimeKey, season, hour, byId, currentHex, allLocations, relevantLocations: [], currentLocations: [], influential: [],
      terrainReading: emptyReading, route: [], routeHexIds: [], possibleWeather: [clearWeather(`${scopeKey}|empty`)], nearbySettlements: [], adjacentHexSummary: [],
      evidence: ['Campanha sem hex canônico ou manifesto válido.'], humanTrafficLoreSupported, loreEvidence,
    };
  }
  const rings = collectHexRings(currentHex, byId, 12);
  const relevantLocations = allLocations.filter(location => location.hexIds.some(id => rings.has(id)));
  const currentLocations = relevantLocations.filter(location => location.hexIds.includes(currentHex.id));
  const influential = influentialLocationsForHex(currentHex, byId, relevantLocations).map(item => item.location);
  const resolvedLocation = resolveHexNarrativeLocationFromIndex(currentHex, byId, allLocations);
  const extraText = `${resolvedLocation.name} ${resolvedLocation.regionName}`;
  const terrainReading = readTerrain(currentHex, byId, relevantLocations, extraText);
  const routeHexIds = validatePlannedRoute(currentHex, discovery.plannedRouteHexIds || [], byId);
  const route = buildRoutePreview(currentHex, routeHexIds, byId, relevantLocations, extraText);
  const snowEligible = isSnowEligible(terrainReading.climateZone, terrainReading.elevationClass, season, terrainReading.terrainClass);
  const seed = `${scopeKey}|${currentHex.id}|${worldTimeKey}|${terrainReading.climateZone}|${routeHexIds.join(',')}`;
  const possibleWeather = buildWeatherCandidates(terrainReading.climateZone, terrainReading.elevationClass, terrainReading.terrainClass, season, hour, snowEligible, route, seed);
  const previousHexId = [...(discovery.routeHexIds || [])].reverse().find(id => id !== currentHex.id);
  const previousHex = previousHexId ? byId.get(previousHexId) : undefined;
  const nearby = nearbySettlements(currentHex, byId, relevantLocations, route, previousHex);
  const adjacentHexSummary = Array.from(rings.entries()).filter(([, ring]) => ring > 0 && ring <= 3).map(([hexId, ring]) => {
    const hex = byId.get(hexId)!;
    const localLocations = relevantLocations.filter(location => location.hexIds.includes(hexId));
    const reading = readTerrain(hex, byId, relevantLocations);
    const settlementKind = [hexSettlementKind(hex), ...localLocations.map(locationSettlementKind)].sort((a, b) => settlementRank(b) - settlementRank(a))[0] || 'none';
    const resolved = resolveHexNarrativeLocationFromIndex(hex, byId, allLocations);
    return { hexId, ring, terrain: normalize(hex.terrain) !== 'unknown' ? hex.terrain : TERRAIN_LABELS[reading.terrainClass], terrainClass: reading.terrainClass, region: resolved.regionName, locations: localLocations.map(location => location.name).slice(0, 5), settlementKind };
  }).sort((a, b) => a.ring - b.ring || a.hexId.localeCompare(b.hexId));
  const evidence = [
    `Hex atual ${currentHex.id}.`,
    `Terreno inferido: ${TERRAIN_LABELS[terrainReading.terrainClass]} (${Math.round(terrainReading.confidence * 100)}% de confiança).`,
    `Zona climática ${terrainReading.climateZone}; relevo ${terrainReading.elevationClass}; estação ${season}.`,
    route.length ? `Rota futura confirmada no atlas: ${route.map(step => step.hexId).join(' → ')}.` : 'Nenhuma rota futura foi planejada; o núcleo não inventou etapas.',
    nearby[0] ? `Localidade de maior influência: ${nearby[0].name}, a ${nearby[0].distanceHexes} hex(es), tendência ${nearby[0].movementTrend}.` : 'Nenhuma localidade detectada em até cinco hexes.',
    ...terrainReading.evidence,
  ];
  return { campaignId: campaign.id, profile, scopeKey, currentHexId, worldTimeKey, season, hour, byId, currentHex, allLocations, relevantLocations, currentLocations, influential, terrainReading, route, routeHexIds, possibleWeather, nearbySettlements: nearby, adjacentHexSummary, evidence: Array.from(new Set(evidence)).slice(0, 16), humanTrafficLoreSupported, loreEvidence };
}

async function ensureEnvironmentState(data: StaticEnvironmentData): Promise<PersistedEnvironmentState> {
  const id = `environment:${data.scopeKey}`;
  const existing = await getStoreItem<PersistedEnvironmentState>('environment_states', id).catch(() => undefined);
  if (existing) {
    // Reconciliamos posição e relógio sem avançar o clima. Apenas um movimento
    // confirmado e inédito pode chamar a máquina de estados meteorológica.
    if (existing.hexId !== data.currentHexId || existing.worldTimeKey !== data.worldTimeKey) {
      const reconciled = { ...existing, hexId: data.currentHexId, worldTimeKey: data.worldTimeKey, generatedAt: Date.now() };
      // Reconciliação não avança o clima, mas é persistida para que uma morte do
      // processo não reabra a campanha com posição/relógio ambiental obsoletos.
      await saveStoreData('environment_states', reconciled, 'reconciliação ambiental sem avanço').catch(() => undefined);
      return reconciled;
    }
    return existing;
  }
  const initial = weightedWeather(data.possibleWeather, `${data.scopeKey}|${data.currentHexId || 'sem-hex'}|${data.worldTimeKey}|initial`);
  const initialStage: WeatherStage = initial.kind === 'clear' || initial.kind === 'cloudy'
    ? 'stable'
    : initial.kind === 'storm' || initial.kind === 'blizzard'
      ? 'indications'
      : (hashString(`${data.scopeKey}|${initial.kind}|stage`) % 3 === 0 ? 'forming' : 'active');
  const initialWeather: WeatherForecast = {
    ...initial,
    stage: initialStage,
    stageStep: 0,
    activeStepsRemaining: initialStage === 'active' ? 1 + (hashString(`${data.scopeKey}|${initial.kind}|duration`) % 3) : 0,
    reason: `${initial.reason} Este é o estado climático já presente quando a crônica começa; não foi criado pela narração.`,
  };
  const state: PersistedEnvironmentState = {
    id, scopeKey: data.scopeKey, rulesProfileId: data.profile.id, campaignId: data.campaignId, hexId: data.currentHexId,
    worldTimeKey: data.worldTimeKey, weather: initialWeather, generatedAt: Date.now(), travelStep: 0, processedTravelActionIds: [],
  };
  await saveStoreData('environment_states', state, 'estado ambiental inicial').catch(() => undefined);
  return state;
}

function buildContext(data: StaticEnvironmentData, state: PersistedEnvironmentState, memory: EnvironmentMemoryRecord[]): EnvironmentContext {
  const terrain = data.terrainReading;
  const snowEligible = isSnowEligible(terrain.climateZone, terrain.elevationClass, data.season, terrain.terrainClass);
  const currentWeather = state.weather;
  const nearest = data.nearbySettlements[0];
  const signals = approachSignalPool(nearest, data.hour, currentWeather, terrain.roadEvidence || data.route.some(step => step.roadEvidence), data.humanTrafficLoreSupported);
  const opportunities = rotateOpportunities([
    ...settlementOpportunityPool(nearest, data.hour, currentWeather, terrain.roadEvidence || data.route.some(step => step.roadEvidence), data.humanTrafficLoreSupported),
    ...terrainOpportunityPool(terrain.terrainClass, data.hour, currentWeather),
  ], memory, `${data.scopeKey}|${data.currentHexId}|${state.travelStep}|opportunities`, 9);
  const faunaHints = opportunities.filter(item => item.category === 'fauna').map(item => item.description);
  const trackHints = opportunities.filter(item => item.category === 'tracks').map(item => item.description);
  const travelRiskHints = opportunities.filter(item => item.category === 'hazard').map(item => item.description);
  const naturalResourceHints = opportunities.filter(item => item.category === 'resource').map(item => item.description);
  const explorationHints = opportunities.filter(item => item.category === 'exploration').map(item => item.description);
  const ambienceHints = Array.from(new Set([
    ...ambienceForWeather(currentWeather),
    ...distantAmbienceForSettlement(nearest, terrain.roadEvidence || data.route.some(step => step.roadEvidence), currentWeather, data.humanTrafficLoreSupported),
    terrain.terrainClass === 'forest' ? 'forest' : '', terrain.terrainClass === 'wetland' ? 'swamp' : '', terrain.terrainClass === 'coast' ? 'coast' : '',
    terrain.terrainClass === 'mountain' ? 'mountain-wind' : '', terrain.terrainClass === 'fields' ? 'open-fields' : '',
  ].filter(Boolean)));
  const resolvedLocation = data.currentHex
    ? resolveHexNarrativeLocationFromIndex(data.currentHex, data.byId, data.allLocations)
    : null;
  return {
    campaignId: state.campaignId,
    systemScope: data.profile.systemId,
    settingScope: data.profile.settingId,
    rulesProfileId: data.profile.id,
    rulesSourceFolderId: data.profile.sourceFolderId,
    currentHexId: data.currentHexId,
    generatedAt: Date.now(),
    worldTimeKey: data.worldTimeKey,
    season: data.season,
    hour: data.hour,
    terrain: data.currentHex && normalize(data.currentHex.terrain) !== 'unknown' ? data.currentHex.terrain : TERRAIN_LABELS[terrain.terrainClass],
    terrainClass: terrain.terrainClass,
    region: resolvedLocation?.regionName || 'região não confirmada',
    climateZone: terrain.climateZone,
    elevationClass: terrain.elevationClass,
    snowEligible,
    currentWeather,
    possibleWeather: data.possibleWeather,
    nearbySettlements: data.nearbySettlements,
    approachSignals: signals,
    ambienceHints,
    encounterHints: opportunities.filter(item => ['merchant', 'messenger', 'wagon', 'patrol', 'guide', 'resident', 'traveller'].includes(item.category)).map(item => item.title),
    encounterOpportunities: opportunities,
    faunaHints,
    trackHints,
    travelRiskHints,
    naturalResourceHints,
    explorationHints,
    futureRoute: data.route,
    adjacentHexSummary: data.adjacentHexSummary,
    evidence: [
      ...data.evidence,
      `Clima persistido: ${weatherDescription(currentWeather)}; passo de viagem ambiental ${state.travelStep}.`,
      data.humanTrafficLoreSupported
        ? `Tráfego humano sensível sustentado por fontes do perfil: ${data.loreEvidence.join('; ') || 'índice ativo'}.`
        : 'Sem fonte de lore recuperada para tráfego humano: mercadores, patrulhas, mensageiros e carroças foram bloqueados pelo fallback conservador.',
      rulesProfilePrompt(data.profile),
    ],
  };
}

export async function buildAutonomousEnvironmentContext(campaign: CampaignState): Promise<EnvironmentContext> {
  const data = await computeStaticEnvironment(campaign);
  const state = await ensureEnvironmentState(data);
  const memory = await loadEnvironmentMemory(data.scopeKey);
  const context = buildContext(data, state, memory);
  await persistAutonomousSuggestions(context).catch(() => undefined);
  return context;
}

/**
 * Única porta que avança o clima. Deve ser chamada depois que o movimento foi
 * confirmado e persistido. PLAY, repetição de narração e reconstrução de tela
 * apenas leem o estado e nunca chegam aqui.
 */

export interface PreparedEnvironmentAdvance {
  context: EnvironmentContext;
  state: PersistedEnvironmentState;
  autonomousEvents: AutonomousEventRecord[];
  duplicate: boolean;
}

export async function prepareEnvironmentAdvance(campaign: CampaignState, travelActionId: string): Promise<PreparedEnvironmentAdvance> {
  const data = await computeStaticEnvironment(campaign);
  const state = await ensureEnvironmentState(data);
  const memory = await loadEnvironmentMemory(data.scopeKey);
  if (state.processedTravelActionIds.includes(travelActionId)) {
    const context = buildContext(data, state, memory);
    return { context, state, autonomousEvents: buildAutonomousSuggestionRecords(context), duplicate: true };
  }
  const weatherSeed = `${data.scopeKey}|${travelActionId}|${state.travelStep + 1}`;
  const nextWeather = (state.weather.stage || 'stable') === 'stable'
    ? (() => {
        const candidate = weightedWeather(data.possibleWeather, `${weatherSeed}|candidate`);
        if (candidate.kind === 'clear' || candidate.kind === 'cloudy') return { ...candidate, stage: 'stable' as const, stageStep: 0 };
        return { ...candidate, stage: 'indications' as const, stageStep: 1, activeStepsRemaining: 0, reason: `${candidate.reason} Por enquanto existem apenas indícios.` };
      })()
    : advanceWeatherState(state.weather, data.possibleWeather, weatherSeed);
  const updated: PersistedEnvironmentState = {
    ...state, hexId: data.currentHexId, worldTimeKey: data.worldTimeKey, weather: nextWeather,
    generatedAt: Date.now(), travelStep: state.travelStep + 1, lastTravelActionId: travelActionId,
    processedTravelActionIds: [...state.processedTravelActionIds, travelActionId].slice(-64),
    lastSettlementId: data.nearbySettlements[0]?.id,
  };
  const context = buildContext(data, updated, memory);
  return { context, state: updated, autonomousEvents: buildAutonomousSuggestionRecords(context), duplicate: false };
}

export async function prepareEnvironmentNarrativeMemory(context: EnvironmentContext, sceneId: string, narration: string): Promise<EnvironmentMemoryRecord[]> {
  const text = normalize(narration);
  if (!text) return [];
  const scopeKey = `${context.rulesProfileId}:${context.campaignId}`;
  const now = Date.now();
  const existing = await loadEnvironmentMemory(scopeKey);
  const byId = new Map(existing.map(row => [row.id, row]));
  const candidates: Array<{ kind: EnvironmentMemoryRecord['kind']; key: string; label: string; tokens: string[] }> = [];
  for (const item of context.encounterOpportunities) {
    const tokens = normalize(`${item.title} ${item.description}`).split(' ').filter(token => token.length >= 5).slice(0, 12);
    candidates.push({ kind: item.category === 'fauna' ? 'fauna' : item.category === 'tracks' ? 'track' : item.category === 'hazard' ? 'risk' : item.category === 'resource' ? 'resource' : item.category === 'exploration' ? 'exploration' : 'encounter', key: item.id, label: item.title, tokens });
  }
  context.approachSignals.forEach((signal, index) => candidates.push({ kind: 'signal', key: `approach:${context.nearbySettlements[0]?.id || 'none'}:${index}`, label: signal, tokens: normalize(signal).split(' ').filter(token => token.length >= 5).slice(0, 14) }));
  const records: EnvironmentMemoryRecord[] = [];
  for (const candidate of candidates) {
    const matches = candidate.tokens.filter(token => text.includes(token)).length;
    if (matches < Math.min(2, candidate.tokens.length)) continue;
    const id = `${scopeKey}:${candidate.kind}:${candidate.key}`;
    const previous = byId.get(id);
    records.push({ id, campaignId: context.campaignId, systemScope: context.rulesProfileId, kind: candidate.kind, key: candidate.key, text: candidate.label,
      useCount: (previous?.useCount || 0) + 1, lastUsedAt: now, lastSceneId: sceneId,
      recentHexIds: Array.from(new Set([context.currentHexId || '', ...(previous?.recentHexIds || [])].filter(Boolean))).slice(0, 8),
      metadata: { rulesProfileId: context.rulesProfileId, settingScope: context.settingScope } });
  }
  return records;
}

/**
 * Persistência ambiental deliberadamente não é exposta como operação isolada.
 * O chamador deve usar prepareEnvironmentAdvance/prepareEnvironmentNarrativeMemory
 * e incluir o resultado em saveCanonicalTransaction juntamente com movimento,
 * campanha e diário. Isso impede avanço climático parcial.
 */

export function buildAutonomousSuggestionRecords(context: EnvironmentContext): AutonomousEventRecord[] {
  const now = Date.now();
  const records: AutonomousEventRecord[] = [];
  if (context.currentWeather.kind !== 'clear') {
    records.push({
      id: `auto-weather:${context.rulesProfileId}:${context.campaignId}:${context.currentHexId}:${context.currentWeather.id}:${context.currentWeather.stage}`,
      systemScope: context.rulesProfileId,
      campaignId: context.campaignId,
      hexId: context.currentHexId,
      kind: 'weather',
      title: context.currentWeather.kind === 'blizzard' ? `Nevasca — ${weatherStageLabel(context.currentWeather.stage)}` : `Clima — ${weatherStageLabel(context.currentWeather.stage)}`,
      description: `${weatherDescription(context.currentWeather)}. ${context.currentWeather.reason}`,
      probability: context.currentWeather.probability,
      status: 'suggested',
      notBefore: now,
      expiresAt: now + 24 * 60 * 60_000,
      createdAt: now,
      metadata: { weather: context.currentWeather, snowEligible: context.snowEligible, rule: 'Só avança após deslocamento confirmado.' },
    });
  }
  const settlement = context.nearbySettlements[0];
  if (settlement && settlement.distanceHexes <= 5 && settlement.movementTrend !== 'leaving') {
    records.push({
      id: `auto-settlement:${context.rulesProfileId}:${context.campaignId}:${settlement.id}:${context.currentHexId}`,
      systemScope: context.rulesProfileId,
      campaignId: context.campaignId,
      hexId: context.currentHexId,
      kind: settlement.distanceHexes <= 2 ? 'road-traffic' : 'approach-settlement',
      title: settlement.distanceHexes === 0 ? `Em ${settlement.name}` : `Aproximação de ${settlement.name}`,
      description: context.approachSignals.join(' '),
      probability: settlement.distanceHexes === 0 ? 1 : settlement.distanceHexes === 1 ? 0.82 : settlement.distanceHexes === 2 ? 0.58 : settlement.distanceHexes === 3 ? 0.38 : 0.20,
      status: 'suggested',
      notBefore: now,
      expiresAt: now + 18 * 60 * 60_000,
      createdAt: now,
      metadata: { settlement, opportunities: context.encounterOpportunities.slice(0, 6), ambienceHints: context.ambienceHints },
    });
  }
  for (const item of context.encounterOpportunities.slice(0, 2)) {
    records.push({
      id: `auto-opportunity:${context.rulesProfileId}:${context.campaignId}:${context.currentHexId}:${item.id}`,
      systemScope: context.rulesProfileId,
      campaignId: context.campaignId,
      hexId: context.currentHexId,
      kind: 'regional-sign',
      title: item.title,
      description: `${item.description} Gatilho: ${item.trigger}`,
      probability: item.probability,
      status: 'suggested',
      notBefore: now,
      expiresAt: now + 12 * 60 * 60_000,
      createdAt: now,
      metadata: { opportunity: item, optional: true },
    });
  }
  return records;
}

async function persistAutonomousSuggestions(context: EnvironmentContext): Promise<void> {
  const now = Date.now();
  const existing = await getStoreData<AutonomousEventRecord>('autonomous_events').catch(() => []);
  for (const stale of existing.filter(item => item.campaignId === context.campaignId && item.systemScope === context.rulesProfileId && item.expiresAt <= now)) await deleteStoreData('autonomous_events', stale.id, false).catch(() => undefined);
  for (const record of buildAutonomousSuggestionRecords(context)) await saveStoreData('autonomous_events', record, 'sugestão ambiental autônoma');
}

export function formatEnvironmentContextForPrompt(context: EnvironmentContext): string {
  const isTheOneRingProfile = context.systemScope === 'the-one-ring-2e';
  const routeRule = isTheOneRingProfile
    ? 'No perfil O Um Anel 2e, o caminho da Jornada considera o relevo e os Eventos de Jornada são determinados e resolvidos pelo procedimento formal de Jogadas de Marcha. Estrada e terreno difícil só modificam esse procedimento quando a regra recuperada assim ordenar.'
    : 'Aplique somente o procedimento de viagem, deslocamento e eventos recuperado dos manuais do perfil ativo. Não transplante regras de O Um Anel ou de qualquer outro jogo.';
  const encounterRule = isTheOneRingProfile
    ? 'Mercadores, patrulhas, fauna, rastros e recursos são possibilidades narrativas com gatilhos. No perfil O Um Anel 2e, um Encontro Fortuito com benefício mecânico exige o resultado formal correspondente.'
    : 'Mercadores, patrulhas, fauna, rastros e recursos são possibilidades narrativas com gatilhos. Qualquer benefício ou consequência mecânica exige a resolução formal encontrada nas fontes do perfil ativo.';
  const possible = context.possibleWeather.filter(item => item.eligible && item.probability >= 0.04).slice(0, 7)
    .map(item => `${item.kind}/${item.intensity} ${Math.round(item.probability * 100)}%`).join(', ');
  const settlements = context.nearbySettlements.slice(0, 5).map(item => `${item.name} (${item.kind}, ${item.distanceHexes} hex, ${item.movementTrend || 'unknown'})`).join('; ');
  const route = context.futureRoute.length ? context.futureRoute.map(item => `etapa ${item.step}: região=${item.region || context.region || 'não confirmada'}, terreno=${item.terrainClass}${item.transition ? `, transição ${item.transition}` : ''}, estrada=${item.roadEvidence ? 'sim' : 'não'}, locais=${item.locations.join('/') || 'nenhum'}`).join('; ') : 'nenhuma rota futura confirmada';
  const opportunities = context.encounterOpportunities.slice(0, 9).map(item => `${item.title} [${Math.round(item.probability * 100)}%; gatilho: ${item.trigger}]`).join('; ');
  return `
--- NÚCLEO AMBIENTAL AUTÔNOMO 3.5 — PERFIL ISOLADO ---
Perfil: ${context.rulesProfileId}; sistema=${context.systemScope}; cenário=${context.settingScope}; pasta de regras=${context.rulesSourceFolderId || 'não configurada'}.
Posição narrativa: região=${context.region}; terreno=${context.terrainClass}; relevo=${context.elevationClass}; zona climática=${context.climateZone}. O código técnico do hex permanece apenas no Atlas e não deve aparecer na prosa.
Rota pré-lida (máximo cinco etapas): ${route}.
Clima persistente: ${context.currentWeather.kind}/${context.currentWeather.intensity}; estágio=${context.currentWeather.stage || 'stable'}; etapa=${context.currentWeather.stageStep || 0}; avanços ativos restantes=${context.currentWeather.activeStepsRemaining || 0}.
REGRA DE ESTADO: não avance o clima por narração, geração de cena, PLAY Android, PLAY Charon ou repetição da mesma ação. O estado só avança quando o atlas confirma e persiste um novo deslocamento com chave inédita.
Neve permitida: ${context.snowEligible ? 'SIM' : 'NÃO'}. Uma nevasca possível começa em indícios, passa por formação e só então pode ficar ativa. Nunca a ative imediatamente.
Possibilidades meteorológicas: ${possible || 'nenhuma'}.
Localidades em até cinco hexes: ${settlements || 'nenhuma'}.
Sinais graduais: ${context.approachSignals.join(' ') || 'nenhum'}
Oportunidades opcionais: ${opportunities || 'nenhuma'}.
Fauna: ${context.faunaHints.join(' ') || 'nenhuma sugestão'}
Rastros: ${context.trackHints.join(' ') || 'nenhuma sugestão'}
Riscos: ${context.travelRiskHints.join(' ') || 'nenhuma sugestão'}
Recursos: ${context.naturalResourceHints.join(' ') || 'nenhuma sugestão'}
Exploração: ${context.explorationHints.join(' ') || 'nenhuma sugestão'}
Camadas de ambiência: ${context.ambienceHints.join(', ') || 'nenhuma'}.

REGRAS OBRIGATÓRIAS DO PERFIL ATIVO:
1. A rota futura é apenas a rota explicitamente planejada no atlas. O núcleo ambiental não cria encontros ou eventos mecânicos por conta própria.
2. ${routeRule}
3. Não invente Fadiga, Ferimento, Sombra, Esperança, bônus, penalidade ou qualquer consequência mecânica a partir de uma sugestão ambiental.
4. ${encounterRule}
5. Um local futuro serve para prenúncio. Não diga que a Companhia chegou antes de entrar no hex do local.
6. Sons remotos de estrada/cidade são ambiência contínua e discreta. Carroça, cavalos, sinos e multidão só viram SFX pontual se o evento realmente aparecer na narração.
7. Use somente regras recuperadas dos manuais locais da pasta ativa. Quando a regra não for encontrada, não a invente. Para outro sistema/cenário, substitua estas regras pelo perfil e fontes vinculados àquela campanha; nunca reutilize mecanicamente este perfil.
8. Nunca escreva identificadores técnicos de hex na narração, no resultado da ação ou nas sugestões. Diga o nome da região, terreno, estrada, marco ou localidade conhecida.
`;
}

export function environmentWeatherLabel(context: EnvironmentContext): string { return weatherDescription(context.currentWeather); }

export function enrichAudioPlanWithEnvironment(plan: SceneAudioPlan | undefined, context: EnvironmentContext): SceneAudioPlan {
  const nearest = context.nearbySettlements[0];
  const base = nearest?.distanceHexes === 0 ? 'town'
    : context.terrainClass === 'forest' ? 'forest'
      : context.terrainClass === 'wetland' ? 'swamp'
        : context.terrainClass === 'coast' ? 'river'
          : context.terrainClass === 'ruins' ? 'ruins'
            : 'road';
  const validBases = new Set(['tavern', 'forest', 'cave', 'swamp', 'river', 'town', 'ruins', 'road']);
  const proposedBase = (plan?.ambience || []).map(normalize).find(value => validBases.has(value));
  const details = new Set<string>();
  for (const hint of context.ambienceHints) {
    const normalized = normalize(hint).replace(/ /g, '-');
    if (['rain', 'wind', 'night', 'fireplace', 'distant-road-traffic', 'distant-wagons', 'distant-settlement', 'outskirts', 'outskirts-shelter'].includes(normalized)) details.add(normalized);
    if (normalized === 'light-rain' && context.currentWeather.stage === 'forming') details.add('rain');
  }
  for (const detail of plan?.ambienceDetails || []) details.add(normalize(detail).replace(/ /g, '-'));
  // Não injeta carroça/cavalo/sino como SFX. Eventos pontuais continuam vindo
  // exclusivamente do detector aplicado ao texto realmente narrado.
  const weatherStage = context.currentWeather.stage || 'stable';
  const audibleWeather = weatherStage === 'active'
    ? [context.currentWeather.kind, context.currentWeather.intensity]
    : weatherStage === 'forming'
      ? [`light ${context.currentWeather.kind}`]
      : weatherStage === 'dissipating'
        ? [`light ${context.currentWeather.kind}`]
        : weatherStage === 'indications'
          ? ['wind indications']
          : [];
  return {
    environment: Array.from(new Set([...(plan?.environment || []), context.terrainClass, context.climateZone, context.region, weatherStage, ...audibleWeather, ...context.ambienceHints].filter(Boolean))),
    ambience: [proposedBase || base],
    ambienceDetails: Array.from(details).slice(0, 4),
    musicMood: plan?.musicMood?.length ? plan.musicMood.slice(0, 2) : ['calm'],
    sfx: [...(plan?.sfx || [])],
    stopPreviousAmbience: Boolean(plan?.stopPreviousAmbience),
  };
}
