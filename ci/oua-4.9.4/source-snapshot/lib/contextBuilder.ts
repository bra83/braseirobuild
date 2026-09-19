import { CampaignState, CharacterSheet, ManualChunk, Npc } from './db';
import { SceneSourceReference } from '../types';
import { getCharacters, getStoreData, searchChunks } from './db';
import { buildRuleSearchTerms, detectRuleDomains } from './rulesEngine';
import { loadMapDiscovery, loadRuntimeMapManifest, loadRuntimeTerraMediaLocations, resolveHexNarrativeLocation, TerraMediaLocation } from './map';
import { normalizeMasterState, publicKnowledge } from './master/state';
import { findRuleCards } from './rules/ruleCards';
import { buildAutonomousEnvironmentContext, formatEnvironmentContextForPrompt } from './autonomy/environment';
import { resolveRulesProfile } from './autonomy/rulesProfile';
import type { EnvironmentContext } from './autonomy/types';
import { sourceForCulture, virtuesForCulture, SUPPLEMENT_PATRONS, SUPPLEMENT_SOURCES } from './supplementRules';


export interface RulesConsultationStatus {
  scopedManualCount: number;
  readyManualCount: number;
  retrievedCount: number;
  profileId: string;
  searchPasses: number;
}

function scopedReadyManuals(manuals: import('./db').Manual[], profile: ReturnType<typeof resolveRulesProfile>): import('./db').Manual[] {
  return manuals.filter(manual => {
    const belongs = manual.profileId === profile.id
      || manual.systemProfileId === profile.systemId
      || manual.rulesSourceFolderId === profile.sourceFolderId
      || (!manual.profileId && manual.system === profile.displayName);
    return belongs && manual.indexed === true && (manual.indexState === undefined || manual.indexState === 'ready') && Number(manual.chunkCount || 0) > 0;
  });
}

function baselineRuleTerms(domains: string[]): string[] {
  const terms = new Set<string>(['O Um Anel','regras','teste','Dado de Proeza','Dados de Sucesso','Número Alvo','Historiador','Mestre do Saber']);
  for (const domain of domains) {
    if (domain === 'jornada' || domain === 'movimento') ['Jornada','papéis de jornada','Marcha','evento de jornada','fadiga'].forEach(v => terms.add(v));
    if (domain === 'combate') ['combate','posturas','Bloqueio','Parada','Ferimento','Proteção','Ódio'].forEach(v => terms.add(v));
    if (domain === 'conselho') ['Conselho','Tolerância','Apresentação','Interação'].forEach(v => terms.add(v));
    if (domain === 'descanso') ['descanso','recuperação','Fase de Sociedade'].forEach(v => terms.add(v));
    if (domain === 'sombra') ['Sombra','Esperança','Arrasado','crise de loucura'].forEach(v => terms.add(v));
    if (domain === 'inventario') ['carga','provisões','equipamento','tesouro'].forEach(v => terms.add(v));
  }
  return Array.from(terms);
}

export function formatCharacterSheetForPrompt(sheet: CharacterSheet): string {
  const name = sheet.identity.name?.trim() || 'Herói sem nome';
  const equipped = sheet.inventory.filter(item => item.equipped).slice(0, 5);
  const activeWeapons = sheet.combat.weapons.filter(item => item.active).slice(0, 5);
  return `
--- ESTADO ENXUTO DO PERSONAGEM ---
Identidade: ${name}; ${sheet.identity.culture || 'cultura não definida'}; ${sheet.identity.vocation || 'vocação não definida'}
Atributos: força=${sheet.attributes.strength}; coração=${sheet.attributes.heart}; astúcia=${sheet.attributes.wits}; resistência=${sheet.attributes.enduranceCurrent}/${sheet.attributes.enduranceMax}; esperança=${sheet.attributes.hopeCurrent}/${sheet.attributes.hopeMax}; sombra=${sheet.attributes.shadow}; fadiga=${sheet.attributes.fatigue}; carga=${sheet.attributes.load}; provisões=${sheet.attributes.supplies}%
Condições: ${[
    sheet.attributes.wounded ? 'Ferido' : '',
    sheet.attributes.weary ? 'Exausto' : '',
    sheet.attributes.miserable ? 'Arrasado' : '',
  ].filter(Boolean).join(', ') || 'Nenhuma'}
Perícias: ${sheet.skills.map(skill => `${skill.name}=${skill.rank}${skill.favorite ? '*' : ''}`).join('; ') || 'nenhuma'}
Defesa: bloqueio=${sheet.combat.parry}; armadura=${sheet.combat.armor || 'nenhuma'}; capacete=${sheet.combat.helmet || 'nenhum'}; escudo=${sheet.combat.shield || 'nenhum'}
Cinco espaços de mão/equipamento: ${[...activeWeapons.map(item => `${item.name}(dano ${item.damage}, ferimento ${item.injury})`), ...equipped.map(item => `${item.name} x${item.quantity}`)].slice(0, 5).join('; ') || 'vazios'}
Moedas: ${sheet.resources.coins}; tesouro=${sheet.resources.treasure}; experiência=${sheet.resources.experience}; pontos de aventura=${sheet.resources.adventurePoints}; pontos de perícia=${sheet.resources.skillPoints}
Virtudes/recompensas: ${sheet.other.virtues || 'nenhuma'} / ${sheet.other.rewards || 'nenhuma'}
Condições narrativas: ${(sheet.other.narrativeConditions || sheet.other.notes || 'nenhuma').slice(0, 800)}
`;
}

function formatSourceBoundSupplementContext(campaign: CampaignState): string {
  const culture = campaign.characterSheet.identity.culture || '';
  const sourceId = sourceForCulture(culture);
  const source = SUPPLEMENT_SOURCES.find(item => item.id === sourceId);
  const virtues = virtuesForCulture(culture);
  const patronName = campaign.masterState?.company?.patron || campaign.characterSheet.identity.patron || '';
  const patron = SUPPLEMENT_PATRONS.find(item => item.name === patronName);
  if (sourceId === 'tor2e-core' && !patron) return '';
  return `
--- INTEGRAÇÃO SOURCE-BOUND 2e ---
Cultura ativa: ${culture || 'não definida'}. Fonte autorizada: ${source?.title || sourceId}. Autoridade: ${source?.authority || 'mechanics'}.
${virtues.length ? `Virtudes culturais disponíveis pela fonte: ${virtues.map(item => `${item.name} — ${item.summary}`).join(' | ')}` : 'Nenhuma Virtude suplementar estruturada para esta Cultura.'}
${patron ? `Patrono da Companhia: ${patron.name}; chamados favorecidos: ${patron.favouredCallings.join(', ')}; agenda: ${patron.agenda}. Fonte: ${patron.sourceId}.` : ''}
Esses dados estruturados são auxiliares e nunca autorizam uma regra além da fonte 2e indicada. Aventuras/cenário fornecem contexto narrativo, não precedência mecânica.
`;
}

function formatCompanionForPrompt(sheet: CharacterSheet, active: boolean): string {
  const conditions = [
    sheet.attributes.wounded ? 'Ferido' : '',
    sheet.attributes.weary ? 'Exausto' : '',
    sheet.attributes.miserable ? 'Arrasado' : '',
  ].filter(Boolean).join(', ') || 'Nenhuma';
  return `${active ? '[HERÓI DA VEZ]' : '[COMPANHEIRO]'} ${sheet.identity.name || 'Herói sem nome'} — ${sheet.identity.culture || 'cultura não definida'}, ${sheet.identity.vocation || 'vocação não definida'}; Resistência ${sheet.attributes.enduranceCurrent}/${sheet.attributes.enduranceMax}; Esperança ${sheet.attributes.hopeCurrent}/${sheet.attributes.hopeMax}; Condições: ${conditions}; armas: ${sheet.combat.weapons.filter(item => item.active).map(item => item.name).join(', ') || 'nenhuma ativa'}.`;
}

function formatPendingRoll(campaign: CampaignState): string {
  const roll = campaign.pendingDiceRoll;
  if (!roll) return '';
  const featLabel = roll.featResult === 11
    ? 'Olho de Sauron'
    : roll.featResult === 12
      ? 'Runa de Gandalf'
      : String(roll.featResult ?? 'não informado');
  return `
--- ROLAGEM JÁ REALIZADA; RESOLVER AGORA ---
Personagem: ${roll.characterName || campaign.characterSheet.identity.name || 'herói ativo'}
Perícia: ${roll.skillName || campaign.pendingTest?.skill || 'não informada'}
Número-Alvo: ${roll.targetNumber ?? campaign.pendingTest?.targetNumber ?? 'usar a regra aplicável'}
Modo: ${roll.rollMode || campaign.pendingTest?.rollMode || 'normal'}
Dado de Proeza: ${featLabel}
Dados de Sucesso: ${(roll.successResults || roll.results || []).join(', ')}
Total: ${roll.total}; Tengwar: ${roll.tengwarCount || 0}
Resultado calculado: ${roll.success === undefined ? 'interpretar pelas regras recuperadas' : roll.success ? 'SUCESSO' : 'FALHA'} (${roll.successLevel || 'comum'})
Ação que originou o teste: ${campaign.pendingTest?.originalChoice || 'ação atual'}
NÃO peça novamente o mesmo teste. Narre a consequência real do resultado.
`;
}

function formatMapLocation(location: TerraMediaLocation, includeDescription: boolean): string {
  const status = location.placement.precision === 'printed-symbol-center'
    ? 'marcador cartográfico conferido no mapa'
    : location.placement.status === 'verified-label'
      ? 'rótulo conferido no mapa'
      : `posição derivada (${Math.round(location.placement.confidence * 100)}% de confiança)`;
  const base = `${location.name}${location.nameOriginal ? ` / ${location.nameOriginal}` : ''} — ${location.type}; ${location.regions.join(', ') || 'região não informada'}; ${status}`;
  if (!includeDescription) return base;
  return `${base}. ${location.state2965 || ''} ${location.description || ''} ${location.description2 || ''}`.trim();
}

async function buildMapContext(campaignId: string, currentLocation: string): Promise<string> {
  try {
    const [manifest, discovery, allLocations] = await Promise.all([
      loadRuntimeMapManifest(),
      loadMapDiscovery(campaignId),
      loadRuntimeTerraMediaLocations(),
    ]);
    if (!manifest || !discovery.currentHexId) return '';
    const currentHex = manifest.hexes.find(hex => hex.id === discovery.currentHexId);
    if (!currentHex) return '';
    const currentLocations = allLocations.filter(location => location.hexIds.includes(currentHex.id));
    const resolvedLocation = resolveHexNarrativeLocation(manifest, allLocations, currentHex.id);
    const neighborIds = new Set(currentHex.neighbors);
    const nearbyLocations = allLocations
      .filter(location => location.hexIds.some(hexId => neighborIds.has(hexId)))
      .map(location => ({ hexId: location.hexIds.find(hexId => neighborIds.has(hexId)) || location.hexId || '?', location }))
      .slice(0, 24);

    return `
--- CONTEXTO CARTOGRÁFICO DA TERRA-MÉDIA ---
Posição atual da Companhia confirmada pelo Atlas. O identificador técnico do hex foi omitido da prosa narrativa.
Local cartográfico canônico: ${resolvedLocation.name}.
Região cartográfica: ${resolvedLocation.regionName}.
Terreno cartográfico: ${resolvedLocation.terrainName}.
Origem da resolução: ${resolvedLocation.source}; confiança ${Math.round(resolvedLocation.confidence * 100)}%.
Locais vinculados ao hex atual:
${currentLocations.length ? currentLocations.map(location => `- ${formatMapLocation(location, true)}`).join('\n') : '- Nenhum local catalogado neste hex.'}

Locais nos seis hexes adjacentes, pré-carregados apenas para planejamento do Mestre:
${nearbyLocations.length ? nearbyLocations.map(item => `- ${formatMapLocation(item.location, false)}`).join('\n') : '- Nenhum local catalogado nos hexes adjacentes.'}

REGRAS CARTOGRÁFICAS:
1. O hex atual indica a posição da companhia; um local adjacente NÃO é o local atual.
2. Uma região-pai citada no cadastro de um rio, estrada ou local próximo NÃO pode substituir o nome cartográfico canônico acima. Exemplo: Rohan em uma entrada do Limlume não transforma automaticamente o hex atual em Rohan.
3. Entradas com “marcador cartográfico conferido” foram ancoradas no símbolo impresso; entradas com “rótulo conferido” usam o nome ou a área impressa no mapa.
4. Entradas com “posição derivada” são aproximações explícitas; não as apresente como coordenadas exatas.
5. Não mova a companhia para outro hex apenas por narrar uma paisagem distante. O movimento precisa ocorrer no jogo e ser persistido pelo mapa.
6. Códigos de hex pertencem apenas ao Atlas e ao diagnóstico. Nunca escreva códigos como BO44 na narração, no resultado da ação ou nas sugestões. Use o nome da região, terreno ou local conhecido.
`;
  } catch (error) {
    console.warn('[BRASEIRO_MAPA] contexto cartográfico indisponível:', error);
    return '';
  }
}

function formatRuleCards(cards: ReturnType<typeof findRuleCards>): string {
  if (!cards.length) return '';
  return `
--- CARTÕES DE REGRA ESTRUTURADOS (PRIORIDADE SOBRE INFERÊNCIA DO MODELO) ---
${cards.map(card => `- ${card.title}: ${card.summary} [${card.source}, pág. ${card.page}]`).join('\n')}
`;
}

function isSoloRulesExcerpt(item: ManualChunk): boolean {
  const haystack = `${item.manualName || ''} ${item.text || ''}`
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
  return /modo peregrino|jornada solo|aventura solo|heroi-jogador solitario|herói-jogador solitário|jogar sozinho/.test(haystack);
}

function formatManualExcerpts(excerpts: ManualChunk[]): string {
  if (!excerpts.length) return `
--- REGRAS RECUPERADAS ---
Nenhum trecho relevante foi encontrado nos manuais locais. Não invente uma regra. Quando uma mecânica exata for indispensável, declare de forma curta que a regra não foi localizada e peça ao jogador para importar/consultar o manual, sem apagar ou avançar estados mecânicos.
`;
  return `
--- REGRAS RECUPERADAS DOS MANUAIS LOCAIS (HIERARQUIA OBRIGATÓRIA) ---
${excerpts.slice(0, 5).map((item, index) => `[FONTE ${index + 1}: ${item.manualName}, pág. ${item.pageNum}]\n${item.text.slice(0, 1800)}`).join('\n\n')}
Use essas fontes para testes, turnos, movimento, jornada, combate, descanso e alterações de ficha. Não substitua uma regra recuperada por uma regra genérica de outro RPG.
`;
}

export async function buildTurnContext(
  campaign: CampaignState,
  playerAction: string
): Promise<{ contextText: string; selectedManuals: SceneSourceReference[]; environment: EnvironmentContext; rulesConsultation: RulesConsultationStatus }> {
  const recentNodes = campaign.history.filter(node => node.narrativeMeta?.advancesFiction !== false).slice(-5);
  const recentNarration = recentNodes.map(item => `${item.playerAction} ${item.atmospheric_narration} ${item.action_result}`).join(' ');
  const ruleDomains = detectRuleDomains(playerAction, campaign.currentLocation, recentNarration, campaign.pendingTest?.skill);
  const searchTerms = buildRuleSearchTerms(playerAction, campaign.currentLocation, recentNarration);
  const ruleCards = findRuleCards(`${playerAction} ${recentNarration} ${campaign.pendingTest?.skill || ''}`, ruleDomains);
  if (campaign.pendingTest?.skill) searchTerms.push(campaign.pendingTest.skill, 'teste', 'número alvo', 'dados de sucesso');
  const masterForRules = normalizeMasterState(campaign);
  const journeyRelevant = masterForRules.activeMode === 'journey'
    || /jornada|viaj|marchar|estrada|caminho|rota|hex|montanha|floresta|pântano|pantano|neve|cidade|vila/i.test(`${playerAction} ${recentNarration}`);
  if (journeyRelevant) searchTerms.push('Jornada', 'caminho da Jornada', 'Jogada de Marcha', 'Eventos de Jornada', 'estradas e terreno difícil', 'descrevendo eventos de jornada');

  let partyText = '';
  try {
    const characters = await getCharacters();
    const partyIds = new Set(campaign.partyCharacterIds?.length ? campaign.partyCharacterIds : [campaign.activeCharacterId].filter(Boolean) as string[]);
    const party = characters.filter(character => partyIds.has(character.id));
    if (party.length) {
      party.sort((a, b) => Number(b.id === campaign.activeCharacterId) - Number(a.id === campaign.activeCharacterId));
      partyText = `
--- COMPANHIA EM JOGO (${party.length}) ---
${party.map(character => formatCompanionForPrompt(character.sheet, character.id === campaign.activeCharacterId)).join('\n')}
A ação atual pertence ao HERÓI DA VEZ. Os demais continuam presentes na cena e podem cooperar, falar e sofrer consequências narrativas, mas não altere suas fichas sem uma ação ou teste explicitamente atribuído a eles.
`;
    }
  } catch (error) {
    console.warn('[BRASEIRO_PARTY] não foi possível carregar toda a companhia:', error);
  }

  let manualExcerpts: ManualChunk[] = [];
  const rulesProfile = resolveRulesProfile(campaign);
  let rulesConsultation: RulesConsultationStatus = { scopedManualCount: 0, readyManualCount: 0, retrievedCount: 0, profileId: rulesProfile.id, searchPasses: 0 };
  try {
    const allManuals = await getStoreData<import('./db').Manual>('manuals').catch(() => []);
    const scoped = allManuals.filter(manual => manual.profileId === rulesProfile.id
      || manual.systemProfileId === rulesProfile.systemId
      || manual.rulesSourceFolderId === rulesProfile.sourceFolderId
      || (!manual.profileId && manual.system === rulesProfile.displayName));
    const ready = scopedReadyManuals(allManuals, rulesProfile);
    rulesConsultation = { ...rulesConsultation, scopedManualCount: scoped.length, readyManualCount: ready.length };
    const profileScopes = Array.from(new Set([
      rulesProfile.id,
      rulesProfile.systemId,
      rulesProfile.settingId,
      rulesProfile.displayName,
      campaign.rulesProfileName || '',
    ].filter(Boolean)));
    rulesConsultation.searchPasses += 1;
    manualExcerpts = (await searchChunks(searchTerms, profileScopes)).filter(item => !isSoloRulesExcerpt(item));
    if (manualExcerpts.length < 2 && ready.length) {
      rulesConsultation.searchPasses += 1;
      const fallback = (await searchChunks(baselineRuleTerms(ruleDomains), profileScopes)).filter(item => !isSoloRulesExcerpt(item));
      const byId = new Map([...manualExcerpts, ...fallback].map(item => [item.id, item]));
      manualExcerpts = Array.from(byId.values()).slice(0, 12);
    }
    rulesConsultation = { ...rulesConsultation, retrievedCount: manualExcerpts.length };
  } catch (error) {
    console.warn('[BRASEIRO_RULES] busca de manuais falhou:', error);
  }

  const [mapContextText, environment] = await Promise.all([
    buildMapContext(campaign.id, campaign.currentLocation),
    buildAutonomousEnvironmentContext(campaign),
  ]);
  const autonomousEnvironmentText = formatEnvironmentContextForPrompt(environment);

  const historyText = recentNodes.map((node, index) => {
    const isLatest = index === recentNodes.length - 1;
    return `
[${isLatest ? 'INSTANTE IMEDIATAMENTE ANTERIOR' : `CENA ${campaign.history.length - recentNodes.length + index + 1}`}]
Local: ${node.location}; Período: ${node.timeOfDay || 'não informado'}; Clima: ${node.weather || 'não informado'}
NPCs presentes: ${(node.presentNpcIds || []).join(', ') || 'nenhum registrado'}
Ação: ${node.playerAction}
Narração: ${isLatest ? node.atmospheric_narration.slice(-1800) : node.atmospheric_narration.slice(0, 900)}
Resultado: ${node.action_result.slice(0, isLatest ? 800 : 500)}
Intenção narrativa registrada: ${node.narrativeMeta?.intentClass || 'não classificada'}; alterou ficção=${node.narrativeMeta?.advancesFiction === false ? 'não' : 'sim'}
Eventos estruturados: ${(node.events || []).map(event => event.type).join(', ') || 'nenhum'}
`;
  }).join('\n');

  const contextText = `
${formatCharacterSheetForPrompt(campaign.characterSheet)}
${formatSourceBoundSupplementContext(campaign)}
${partyText}
--- ESTADO DA CAMPANHA ---
Campanha: ${campaign.name}
Capítulo: ${campaign.currentChapter}
Local atual: ${campaign.currentLocation}
Resumo: ${campaign.campaignSummary.slice(0, 2400)}
Eventos recentes: ${campaign.recentEvents.slice(0, 1200)}
Pistas: ${campaign.clues.slice(0, 1200)}
Mistérios: ${campaign.mysteries.slice(0, 900)}
Missões: ${campaign.quests.slice(0, 1200)}
Domínios de regra relevantes detectados: ${ruleDomains.join(', ')}
Teste pendente: ${campaign.pendingTest ? `${campaign.pendingTest.skill} — ${campaign.pendingTest.reason}` : 'nenhum'}
${mapContextText}
${autonomousEnvironmentText}
--- HISTÓRICO RECENTE ---
${historyText || 'Nenhuma cena anterior.'}

${formatPendingRoll(campaign)}
${formatRuleCards(ruleCards)}
${formatManualExcerpts(manualExcerpts)}

--- AUDITORIA DA CONSULTA LOCAL ---
Perfil: ${rulesConsultation.profileId}; manuais vinculados=${rulesConsultation.scopedManualCount}; prontos=${rulesConsultation.readyManualCount}; trechos recuperados=${rulesConsultation.retrievedCount}; passagens de busca=${rulesConsultation.searchPasses}.
Se trechos recuperados for zero, NÃO resolva nem proponha consequência mecânica.

--- REGRAS DE CONTINUIDADE ---
1. Não apague personagem, inventário, pistas, localização ou eventos anteriores.
2. Não marque combate, dia de jornada, encontro ou descanso sem emitir o evento estruturado correspondente.
3. Se a ação exige teste e não há rolagem enviada, emita requiredTest e pare a resolução antes do resultado.
4. Se há rolagem enviada, resolva-a uma única vez e emita test_resolved.
5. Uma mudança de local deve emitir location_changed e atualizar o plano de áudio.
6. Descreva sons reais da cena no audioPlan: ambiente-base, detalhes (lareira, chuva, vento etc.) e SFX pontuais.
7. Esta campanha NÃO usa Modo Peregrino nem regras de jornada solo. Um jogador humano controla vários Heróis-jogadores da Companhia.
8. Use apenas os estados e procedimentos de O Um Anel 2e: Sociedade, Patrono, Esperança, Sombra, Fadiga, Ferimentos, Exausto, Arrasado, Valor, Sabedoria, Posturas, Ódio, Conselho, Jornada, Fase de Sociedade, Percepção do Olho e Olho de Sauron.
9. A cena mais recente é o instante imediatamente anterior. Responda primeiro ao que o jogador acabou de fazer; não abandone NPC, assunto ou situação ainda em curso para introduzir outro gancho.
10. Horário e clima vêm do estado canônico do mundo. Não escolha manhã, sol ou calor como padrão narrativo.
11. Antes de narrar, trate informação como PERCEPTÍVEL → OBSERVÁVEL → INFERÍVEL → OCULTO. O Narrador recebe apenas o que pode aparecer; não transforme hipótese em confirmação.
12. O mundo já está em movimento: NPCs trabalham, hesitam, conversam, carregam coisas, observam ou reagem. Evite cenário parado esperando o jogador.
13. Preserve geografia, objetos e pistas já estabelecidos. Improvisação aceita vira cânone: inventar → fixar → respeitar.
`;

  const boundedContext = contextText.length <= 28_000
    ? contextText
    : `${contextText.slice(0, 12_000)}

--- CONTEXTO INTERMEDIÁRIO COMPACTADO ---

${contextText.slice(-16_000)}`;
  return {
    contextText: boundedContext,
    environment,
    rulesConsultation,
    selectedManuals: manualExcerpts.slice(0, 5).map(item => ({
      manualId: item.manualId,
      manualName: item.manualName,
      pageNum: item.pageNum,
      excerpt: item.text.slice(0, 1800),
      usage: ruleDomains.join(', '),
    })),
  };
}


function formatNpcForDirector(npc: Npc): string {
  return `[NPC ${npc.id}] ${npc.name}; cultura=${npc.culture || npc.peoples || 'não informada'}; papel=${npc.role || 'não informado'}; facção=${npc.faction || 'nenhuma'}; localização=${npc.location || 'desconhecida'}; estado=${npc.state || 'normal'}; emoção=${npc.emotionalState || 'neutra'}; próxima ação=${npc.nextAction || 'não definida'}; atitude=${npc.attitude || 'neutra'}; confiança=${npc.trust}; personalidade=${npc.personality || 'não informada'}; fala=${npc.speechStyle || 'natural'}; objetivos=${npc.objectives || 'não informados'}; medos=${npc.fears || 'não informados'}; SEGREDOS=${npc.secrets || 'nenhum'}; voz=${npc.voiceProfile || 'automática'}; retrato=${npc.tokenId ? 'atribuído' : 'sem retrato'}; notas=${npc.notes || 'nenhuma'}.`;
}

function formatNpcForNarrator(npc: Npc): string {
  // O Narrador não recebe papel, facção, emoção, atitude, objetivos nem segredos.
  // Se qualquer um desses fatos for perceptível, o Diretor precisa liberá-lo em
  // visibleFacts/npcDecisions. Assim o modelo não pode confessar o mistério só
  // porque o banco conhece a resposta.
  return `[NPC ${npc.id}] ${npc.name}; localização=${npc.location || 'desconhecida'}; estado fisicamente observável=${npc.state || 'normal'}; forma de falar já estabelecida=${npc.speechStyle || 'natural'}; aparência=${npc.appearance || 'não informada'}; retrato=${npc.tokenId ? 'atribuído' : 'sem retrato'}.`;
}

export async function buildOrchestratorContext(
  campaign: CampaignState,
  playerAction: string,
): Promise<{ directorContext: string; publicContext: string; selectedManuals: SceneSourceReference[]; environment: EnvironmentContext; rulesConsultation: RulesConsultationStatus }> {
  const { contextText, selectedManuals, environment, rulesConsultation } = await buildTurnContext(campaign, playerAction);
  const [characters, npcs, priorRuleDecisions] = await Promise.all([
    getCharacters().catch(() => []),
    getStoreData<Npc>('npcs').catch(() => []),
    getStoreData<{ id: string; campaignId: string; source: string; command: unknown; timestamp: number }>('rule_decisions').catch(() => []),
  ]);
  const partyIds = new Set(campaign.partyCharacterIds || []);
  const party = characters.filter(character => partyIds.has(character.id));
  const activeNpcs = npcs.filter(npc => campaign.presentNpcIds?.includes(npc.id) || npc.presentInScene);
  const master = normalizeMasterState(campaign);
  const masterForPrompt = {
    ...master,
    position: { ...master.position, currentHexId: null, routeHexIds: [] },
    journey: {
      ...master.journey,
      originHexId: null,
      destinationHexId: null,
      segments: master.journey.segments.map(segment => ({ ...segment, fromHexId: '[oculto]', toHexId: '[oculto]' })),
    },
  };
  const publicMaster = {
    activeMode: master.activeMode,
    position: masterForPrompt.position,
    company: master.company,
    journey: masterForPrompt.journey,
    council: master.council,
    combat: master.combat,
    fellowshipPhase: master.fellowshipPhase,
    livingWorld: {
      companyStyle: master.livingWorld.companyStyle,
      troupeName: master.livingWorld.troupeName,
      baseLocation: master.livingWorld.baseLocation,
      // A visão do Narrador contém apenas entidades que já entraram no campo
      // de conhecimento dos jogadores. Agenda, próxima ação, projetos secretos,
      // revisão pendente e atividade autônoma ficam exclusivamente no Diretor.
      organizations: (master.livingWorld.organizations || [])
        .filter(item => item.active && !item.archived)
        .map(item => ({
          id: item.id,
          name: item.name,
          kind: item.kind,
          territory: item.territory,
          partyReputation: item.partyReputation,
          contracts: item.contracts.filter(contract => contract.status === 'accepted' || contract.status === 'completed'),
        })),
      strongholds: (master.livingWorld.strongholds || [])
        .filter(item => item.knownToCompany)
        .map(item => ({
          id: item.id,
          name: item.name,
          locationId: item.locationId,
          status: item.status,
          ownerKind: item.ownerKind,
          ownerOrganizationId: item.ownerOrganizationId,
          facilities: item.facilities,
          supplies: item.ownerKind === 'company' ? item.supplies : undefined,
          treasure: item.ownerKind === 'company' ? item.treasure : undefined,
          loot: item.ownerKind === 'company' ? item.loot.filter(loot => loot.state === 'stored').map(loot => ({ name: loot.name, quantity: loot.quantity, category: loot.category })) : undefined,
        })),
    },
    quests: master.quests.filter(quest => quest.status !== 'dormant'),
    knowledge: publicKnowledge(master, campaign.partyCharacterIds || []),
    memory: {
      campaignSummary: master.memory.campaignSummary,
      promises: master.memory.promises.filter(item => partyIds.has(item.fromId) || partyIds.has(item.toId)),
    },
    scene: {
      id: master.scene.id,
      title: master.scene.title,
      kind: master.scene.kind,
      participantHeroIds: master.scene.participantHeroIds,
      participantNpcIds: master.scene.participantNpcIds,
    },
  };

  const relevantRuleDecisions = priorRuleDecisions.filter(item => item.campaignId === campaign.id).sort((a, b) => b.timestamp - a.timestamp).slice(0, 12);
  const priorRulesText = relevantRuleDecisions.length ? relevantRuleDecisions.map(item => `- ${item.source}: ${JSON.stringify(item.command)}`).join('\n') : 'Nenhuma decisão anterior registrada.';

  const partyFull = party.map(character => `[ID DO HERÓI: ${character.id}]\n${formatCharacterSheetForPrompt(character.sheet)}`).join('\n');
  const directorContext = `${contextText}\n\n--- ESTADO CANÔNICO DO MESTRE (OCULTO) ---\n${JSON.stringify(masterForPrompt)}\n\n--- FICHAS COMPLETAS DA COMPANHIA ---\n${partyFull || 'Nenhuma ficha carregada.'}\n\n--- NPCS PRESENTES: ESTADO COMPLETO E SEGREDOS ---\n${activeNpcs.map(formatNpcForDirector).join('\n') || 'Nenhum NPC presente.'}\n\n--- DECISÕES DE REGRA ANTERIORES DA MESA ---\n${priorRulesText}\n\nREGRA DE AUTORIDADE: o Diretor pode planejar mudanças, mas o aplicativo validará cada comando. Deve indicar targetCharacterId em toda consequência individual. Um único jogador humano controla todos os Heróis-jogadores; nunca trate a Companhia como protagonista único. O estado formal da cena define participantes, objetivo, tensão e foco. NPC novo deve ser criado por upsert_npc com descrição detalhada; o aplicativo aplica uma das 300 personalidades e escolhe ou gera um retrato persistente automaticamente. Poderes, Patronos, casas, clãs, Guardiões, guildas de ofício, ordens, cultos, bandos e conselhos podem ser atualizados com upsert_organization; refúgios, salões, acampamentos, torres, ruínas ocupadas, covis e fortalezas relevantes usam upsert_stronghold e devem se tornar persistentes quando entram de verdade na ficção. Depois de conquista validada e combate encerrado use claim_stronghold; nunca coloque o loot diretamente na ficha. O estoque conquistado permanece no local para decisão do jogador. Nunca use classes, magias, CA, PV ou mecânicas de outros RPGs.`;

  const publicContext = `${contextText}\n\n--- VISÃO PERMITIDA AOS JOGADORES ---\n${JSON.stringify(publicMaster)}\n\n--- NPCS PRESENTES: SOMENTE O VISÍVEL ---\n${activeNpcs.map(formatNpcForNarrator).join('\n') || 'Nenhum NPC presente.'}\n\n--- PRECEDENTES DE REGRA DA CAMPANHA ---\n${priorRulesText}\n\nNão revele segredos, objetivos ocultos, suspeitas internas ou conhecimento marcado como master.`;
  const bound = (value: string, max = 46_000) => value.length <= max
    ? value
    : `${value.slice(0, Math.floor(max * .48))}\n\n--- CONTEXTO CENTRAL COMPACTADO ---\n\n${value.slice(-Math.floor(max * .52))}`;
  return { directorContext: bound(directorContext), publicContext: bound(publicContext, 40_000), selectedManuals, environment, rulesConsultation };
}
