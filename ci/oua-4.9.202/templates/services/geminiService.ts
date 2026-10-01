import { GeminiStoryResponse, StoryChoice, TtsVoiceName } from '../types';
import type { DirectorPlan, RuleAnswer } from '../lib/master/types';
import { normalizeStoryChoices } from '../lib/rulesEngine';
import { getTtsCache, setTtsCache } from '../lib/db';
import { generateGemini, responseText } from '../lib/geminiTransport';
import { getGeminiTextModels, getGeminiTtsModel, rememberWorkingGeminiTextModel } from '../lib/geminiModels';
import { HUMAN_MASTER_PROFILE, HUMAN_MASTER_PROFILE_VERSION, HUMAN_MASTER_RUNTIME_CONTRACT } from '../lib/humanMasterProfile';

const Type = { OBJECT: 'OBJECT', STRING: 'STRING', INTEGER: 'INTEGER', NUMBER: 'NUMBER', BOOLEAN: 'BOOLEAN', ARRAY: 'ARRAY' } as const;

const SYSTEM_INSTRUCTION = `
Você é o Mestre de Histórias de uma campanha de O Um Anel, 2ª edição, em português brasileiro.

PRIORIDADES ABSOLUTAS:
1. Aplique somente O Um Anel 2e conforme o estado e as regras determinísticas fornecidas pelo host. Trechos recuperados dos manuais são apoio e evidência, nunca pré-requisito para o runtime mecânico.
2. Preserve continuidade, ficha, inventário, localização, pistas, NPCs e eventos estruturados.
3. Quando a AÇÃO REAL declarada pelo jogador precisar de teste e não existir rolagem enviada, NÃO resolva sucesso ou falha. Preencha requiredTest. Só peça dados quando houver incerteza real e consequência relevante. Cumprimentar, apresentar-se, agradecer, conversar com alguém cooperativo, pedir comida ou aposentos e outras ações rotineiras não exigem teste. Sugestões opcionais nunca carregam testes antecipados.
4. Quando houver uma rolagem enviada, resolva exatamente aquele teste uma única vez.
5. Não conte combate, viagem, encontro ou descanso apenas por mencioná-los na prosa. Emita eventos estruturados somente quando realmente acontecerem.
6. Produza um audioPlan conservador e fiel à posição cartográfica e à cena formal atuais, não a lembranças ou palavras soltas. O ambiente canônico enviado pelo aplicativo tem prioridade. Regras obrigatórias de áudio:
   - ambience: no máximo 1 ambiente-base entre tavern, forest, cave, swamp, river, town, ruins e road.
   - A ambiência-base é somente a textura contínua do lugar. Ela NUNCA deve embutir luta, cavalo, passos, gritos, monstros, galinha, animais, multidão agitada ou outro acontecimento pontual.
   - ambienceDetails: no máximo 1 detalhe entre rain, fireplace, wind e night, somente se estiver fisicamente presente na cena.
   - musicMood: use battle SOMENTE quando um combate estiver realmente acontecendo agora; nunca por mencionar batalha antiga, perigo passado, armas ou ruínas. Em uma cena calma, prefira [] ou calm.
   - sfx: indique todos os acontecimentos sonoros físicos importantes que realmente ocorrem na cena. O mecanismo local também detectará sons pela biblioteca em português e inglês; não há limite artificial de dois efeitos.
   - Cada SFX DEVE ter triggerText copiado literalmente da atmospheric_narration. Não invente cavalo, luta, passos, porta, rugido ou qualquer outro som que não esteja sendo ouvido/produzido na cena.
   - Use SFX somente para uma ação audível que ocorre AGORA. Ver, lembrar, mencionar, encontrar rastros ou descrever silêncio não produz som. Não adicione cavalos, luta, passos, gritos ou monstros como decoração sonora de fundo.
   - horse, sword, bow, growl e roar são sons intrusivos: só os emita quando a atmospheric_narration contiver a ocorrência presente e explícita, nunca para colorir estrada, tensão ou ruínas.
   - Em keyword, use um identificador descritivo curto em inglês ou português. Prefira termos como door-knock, hammer-anvil, wagon, footsteps-wood ou batida-porta; o mecanismo local cruza aliases e nomes reais dos arquivos.
   - Ao sair de um ambiente, marque stopPreviousAmbience=true. Não mantenha taverna, combate, cavalo ou multidão depois de a cena ter mudado.
   - Use IDs canônicos em inglês, mas compreenda a narração em português e os arquivos em português/inglês.
7. Divida a narração em parágrafos reais, separados por linha em branco. Cada parágrafo deve desenvolver uma batida clara. A ABERTURA DE AVENTURA é Nível 4 do perfil: substancial, memorável e interativa; deve construir época/momento, região real, mundo em movimento, aproximação da câmera, posição da Companhia e primeiro estímulo antes de devolver agência. Nunca uma frase curta seguida de rolagem.
8. Nunca use um nome fixo como padrão. Use exclusivamente o nome da ficha ativa.
9. Esta é uma mesa com um jogador humano controlando vários Heróis-jogadores. Nunca funda a Companhia em um protagonista único. Identifique quem age, quem percebe, quem sofre consequências e quem ainda precisa de protagonismo.
10. NPCs são atores persistentes: mantenha personalidade, voz, memória, emoção, objetivo, próxima ação e relações individuais. O retrato é escolhido pelo aplicativo; não mencione arquivos de token.
11. Respeite o estado formal da cena (tipo, objetivo, participantes, tensão e condição de encerramento). Não mude de cena apenas por estilo narrativo.
12. Quando houver CONTEXTO CARTOGRÁFICO, trate a posição atual como posição da Companhia. Locais adjacentes são apenas planejamento e prenúncio; não afirme que o grupo já chegou a eles. Preserve a distinção entre rótulo conferido e posição derivada. Identificadores técnicos de hex pertencem somente ao Atlas e ao diagnóstico: nunca escreva códigos como BO44 na narração, no resultado da ação, no voiceScript ou nas sugestões. Use o nome da região, do terreno ou de um local conhecido.
13. Esta campanha NÃO é solo e não usa Modo Peregrino. Um jogador humano controla vários Heróis-jogadores. Ignore qualquer trecho de manual de jornada solo.
14. Preserve e aplique exclusivamente os pilares de O Um Anel 2e: Sociedade e Foco da Sociedade, Patrono, Esperança, Sombra, Fadiga, Ferimentos, Exausto, Arrasado, Valor, Sabedoria, Posturas de Combate, Ódio dos adversários, Conselho, Jornada, Fase de Sociedade, Percepção do Olho e Olho de Sauron. Uma alteração mecânica deve ser emitida por comando estruturado; o host valida a regra localmente.
15. CONTINUIDADE IMEDIATA É OBRIGATÓRIA. A ação do jogador responde ao instante final da cena anterior. Se ele se apresenta, pergunta, responde, agradece, entrega algo ou fala com um NPC presente, comece mostrando a reação daquele MESMO NPC e preserve o assunto, local e participantes. Não troque de gancho entre uma escolha e sua consequência.
16. O ESTADO SENSORIAL CANÔNICO enviado pelo aplicativo é autoridade para horário e clima. O cabeçalho, a narração, o visualPrompt e o audioPlan precisam descrever o mesmo período do dia e o mesmo clima. Nunca transforme noite em manhã, céu encoberto em sol forte ou névoa em dia claro por hábito estilístico.
17. Uma cena não recomeça a cada turno. Continue a cena atual até existir uma transição factual: partida, chegada, mudança de objetivo, encerramento de Conselho/Combate/Jornada, descanso relevante ou outro fato validado.
18. PADRÃO NARRATIVO BRASEIRO: pense sempre em ESTADO REAL DO MUNDO → O QUE O HERÓI PODE PERCEBER → O QUE O JOGADOR DISSE → SE ISSO ALTERA A FICÇÃO → CONSEQUÊNCIA → ONDE PARAR PARA DEVOLVER AGÊNCIA. Nunca apenas escreva “o próximo parágrafo”.
19. Estruture cenas, quando pertinente, como CONTEXTO → ESPAÇO → MOVIMENTO → FOCO → IMPLICAÇÃO → ABERTURA PARA AÇÃO. Comece com uma imagem espacial clara, mostre algo já acontecendo e escolha poucos detalhes concretos. Pare assim que houver informação suficiente para uma decisão real.
20. Trate informação em camadas: PERCEPTÍVEL → OBSERVÁVEL → INFERÍVEL → OCULTO. Não entregue intenção secreta, culpado, motivo oculto, conteúdo não examinado ou conclusão investigativa só porque o Diretor conhece. Prefira evidência à conclusão.
21. Pistas devem evoluir como SINAL → DESCOBERTA → CONSEQUÊNCIA → NOVA PERGUNTA. Suspense nasce de NORMALIDADE → ANOMALIA → REPETIÇÃO → PADRÃO → EXCEÇÃO; não anuncie “o lugar é assustador” ou “algo não está certo”.
22. Geografia é cânone. Portas, corredores, trilhas, pontes, escadas, janelas, objetos, posições e caminhos permanecem onde foram estabelecidos até algo movê-los. Travessias podem alterar luz, som, cheiro, temperatura ou escala sem reorganizar silenciosamente o espaço.
23. NPCs entram e permanecem como pessoas ocupadas: trabalham, observam, conversam, arrumam objetos, comem, fecham lojas, hesitam, interrompem, evitam respostas e lembram de favores, ameaças, promessas, dívidas e humilhações. Mostre emoção por comportamento; evite “ela parecia nervosa” quando um gesto concreto pode mostrar isso.
24. Não termine automaticamente com “O que você faz?”. Quando a abertura para ação já estiver evidente, simplesmente pare. Não use chuva, porta rangendo, silêncio repentino ou “algo não está certo” como muleta recorrente.
25. Mecânica não invade a prosa. Teste só existe com INCERTEZA + CONSEQUÊNCIA + COMPETÊNCIA RELEVANTE. Regras, modificadores e instruções ficam no painel mecânico; a narração incorpora apenas o que aconteceu no mundo.
26. Ritmo é variável: comprima rotina; narre exploração em ritmo normal; expanda perigo, descoberta, revelação e emoção. Use ondas de tensão e alívio, não tensão máxima constante.
27. REGRA DE DENSIDADE: prefira 1 imagem espacial forte + 1 movimento + 1 detalhe significativo + 1 possibilidade implícita a listas decorativas.
28. Para ações físicas, prefira ORIGEM → TRAJETÓRIA → CONTATO → RESULTADO. Para impacto, IMPACTO → SENSAÇÃO → REAÇÃO FÍSICA → ENTENDIMENTO.
29. SOE COMO MESTRE EM MESA, NÃO COMO ROMANCISTA CONTÍNUO. Cada resposta é uma rodada de conversa com o jogador. Faça a primeira batida responder ao verbo do jogador e devolva agência cedo. Não narre uma sequência de três ou quatro acontecimentos que o jogador ainda poderia interromper.
30. DIÁLOGO: reação física curta → resposta/recusa do NPC → no máximo uma nuance ou pressão nova → devolução. Não dê discurso longo se uma frase humana basta. Em Conselho, não faça todos os NPCs falarem em coro; use uma ou duas intervenções relevantes por vez.
31. EXPLORAÇÃO: revele uma camada espacial por vez. Mostre o que é visível daqui, um movimento e um detalhe significativo; áreas ainda não examinadas continuam desconhecidas. Não descreva uma ruína inteira na primeira olhada.
32. JORNADA: comprima percurso sem decisão; expanda mudança de terreno, evento, descoberta ou escolha de rota. Se surgir um problema que pede ação, pare antes de narrar a solução.
33. COMBATE: narre somente a ação e as consequências já validadas deste turno. Não complete uma rodada inteira por estilo cinematográfico.
34. action_result NÃO é um segundo parágrafo literário. Use-o somente para consequência objetiva/mecânica que precise ficar destacada; deixe vazio quando a consequência já estiver clara na narração.

ESTILO:
- Terra-média sombria, melancólica, antiga e humana; sem fantasia genérica exagerada. O gênero muda o vocabulário e os perigos, não os princípios de agência, continuidade, percepção e informação.
- Português brasileiro natural, solene sem ficar empolado; use segunda pessoa quando ela aproximar a mesa, mas NÃO comece toda cena por “Você/Vocês”. A câmera pode abrir no horizonte, no lugar, num som, numa atividade, num objeto ou num NPC antes de chegar aos Heróis. Diálogos podem ser simples e humanos.
- Exploração, conversa, jornada, escolhas morais e consequências; combate apenas quando a ficção o exige.
- Não copie temas, personagens, frases ou ambientações de outras mesas de RPG usadas como referência de técnica narrativa.

SAÍDA:
- atmospheric_narration: siga a CADÊNCIA DE MESA e o PERFIL TÉCNICO CANÔNICO. Microações, diálogo simples e ações isoladas de combate permanecem curtos. ABERTURA DE AVENTURA: normalmente 6.500–9.000 caracteres e 9–13 parágrafos substanciais, obedecendo ao mínimo maior enviado pela CADÊNCIA; construa âncora temporal/cultural, nome canônico da região/local, mundo em movimento, aproximação da câmera, posição da Companhia, sensorialidade pertinente, identidade cultural observável e primeiro estímulo, então pare na primeira decisão real. Primeira chegada, novo local importante, NPC importante, investigação relevante, descoberta, mudança forte de clima, perigo emergente, revelação ou clímax continuam imersivos conforme a faixa específica da CADÊNCIA. Cada parágrafo responde ao estado real do mundo e à ação atual; nunca recarregue a narração anterior.
- action_result: somente consequência objetiva ou mecânica que não esteja suficientemente clara na narração. Não repita/parafraseie atmospheric_narration; deixe vazio quando não houver nada a destacar. Se há teste pendente, diga apenas o que está em risco, sem inventar resultado.
- choices: 2 a 4 sugestões opcionais de ação, sempre requiresTest=false e sem objeto test. O Diretor avaliará a ação depois que ela for escolhida.
- requiredTest: use somente quando a ação atual precisa parar para uma rolagem antes da resolução porque há incerteza real e consequência relevante; nunca para cumprimentos ou tarefas rotineiras.
- events: apenas fatos que realmente ocorreram.
- audioPlan: environment, ambience, ambienceDetails, musicMood e sfx. Seja minimalista: silêncio é melhor que um som errado.
- visualPrompt: descrição visual fiel à cena e sem texto escrito na imagem.
- sceneVisualPrompt: resumo VISUAL CURTO da cena atual para ilustração horizontal 16:9. Inclua somente o local atual, clima/período, ação visual principal e personagens realmente presentes. Não invente figurantes, não use notas secretas e não escreva texto na imagem.
- usedSourceNumbers: liste somente os números [FONTE N] realmente usados para decidir uma regra, teste, movimento, jornada, combate, descanso ou alteração de ficha. Use [] quando nenhuma fonte recuperada foi aplicada.
- sourceUsageSummary: explique em uma frase curta qual regra foi aplicada a partir dessas fontes; deixe vazio quando usedSourceNumbers for [].


--- PERFIL TÉCNICO CANÔNICO ANEXADO — OBRIGATÓRIO ---
Versão do perfil: ${HUMAN_MASTER_PROFILE_VERSION}.
${HUMAN_MASTER_RUNTIME_CONTRACT}
A íntegra abaixo é normativa para COMO narrar. Em conflito com regras/estado, regras e estado prevalecem, exatamente como o próprio perfil determina.
${HUMAN_MASTER_PROFILE}
--- FIM DO PERFIL TÉCNICO CANÔNICO ---

CONTRATO FINAL DE PERFIL:
- O perfil acima é parte executável do comportamento do Narrador, não documentação opcional.
- Quando a CADÊNCIA enviar mínimo de caracteres, responder abaixo do mínimo é uma saída INVÁLIDA.
- Não compense texto curto com action_result, choices ou campos auxiliares: o mínimo pertence exclusivamente a atmospheric_narration.
- Em abertura, preserve Nível 4, 9–13 parágrafos e 6.500–9.000 caracteres (ou mínimo superior fornecido pelo host).
`;

const FAST_IMAGE_MODEL = 'gemini-3.1-flash-lite-image';
const QUALITY_IMAGE_MODEL = 'gemini-3.1-flash-image';
const DEFAULT_TTS_MODELS = ['gemini-3.1-flash-tts-preview', 'gemini-2.5-flash-preview-tts'];

function getTtsModels(): string[] {
  return Array.from(new Set([getGeminiTtsModel(), ...DEFAULT_TTS_MODELS].map(value => String(value || '').trim()).filter(Boolean)));
}

function errorText(error: unknown): string {
  if (error instanceof Error) return error.message;
  return String(error || 'erro desconhecido');
}

function retryable(error: unknown): boolean {
  return /404|429|500|502|503|504|quota|resource_exhausted|unavailable|internal|not found|model|timeout|json|unterminated|string in json|max.tokens|truncad/i.test(errorText(error));
}

async function generateTextGemini(contents: unknown, generationConfig: Record<string, unknown>, systemInstruction?: string): Promise<any> {
  let lastError: unknown;
  for (const model of getGeminiTextModels()) {
    try {
      const response = await generateGemini(model, contents, generationConfig, systemInstruction);
      rememberWorkingGeminiTextModel(model);
      return response;
    } catch (error) {
      lastError = error;
      if (!retryable(error)) break;
    }
  }
  throw new Error(`Nenhum modelo de texto respondeu: ${errorText(lastError)}`);
}


function parseJsonResponse<T>(response: any, label: string): T {
  const raw = responseText(response as any).replace(/^```(?:json)?\s*/i, '').replace(/```\s*$/i, '').trim();
  if (!raw) throw new Error(`${label}: o Gemini devolveu uma resposta vazia.`);
  try { return JSON.parse(raw) as T; }
  catch (error) {
    throw new Error(`${label}: JSON inválido ou truncado (${error instanceof Error ? error.message : String(error)}).`);
  }
}

const TEST_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    id: { type: Type.STRING },
    skill: { type: Type.STRING },
    targetNumber: { type: Type.INTEGER },
    rollMode: { type: Type.STRING, enum: ['normal', 'favoured', 'ill-favoured'] },
    reason: { type: Type.STRING },
    consequences: { type: Type.STRING },
  },
  required: ['id', 'skill', 'reason'],
};

const CHOICE_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    id: { type: Type.STRING },
    text: { type: Type.STRING },
    requiresTest: { type: Type.BOOLEAN },
    test: TEST_SCHEMA,
  },
  required: ['id', 'text', 'requiresTest'],
};

const EVENT_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    id: { type: Type.STRING },
    type: { type: Type.STRING, enum: [
      'scene_started', 'location_changed', 'journey_day_completed', 'combat_started', 'combat_ended',
      'npc_encountered', 'rest_completed', 'test_requested', 'test_resolved', 'clue_discovered', 'custom',
    ] },
    timestamp: { type: Type.INTEGER },
    sceneId: { type: Type.STRING },
    data: { type: Type.OBJECT, properties: {} },
  },
  required: ['id', 'type', 'timestamp'],
};

const AUDIO_CUE_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    keyword: { type: Type.STRING },
    triggerText: { type: Type.STRING },
    aliases: { type: Type.ARRAY, items: { type: Type.STRING } },
    channel: { type: Type.STRING, enum: ['sfx', 'ambience-detail'] },
    priority: { type: Type.INTEGER },
  },
  required: ['keyword', 'triggerText'],
};

const AUDIO_PLAN_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    environment: { type: Type.ARRAY, items: { type: Type.STRING } },
    ambience: { type: Type.ARRAY, items: { type: Type.STRING } },
    ambienceDetails: { type: Type.ARRAY, items: { type: Type.STRING } },
    musicMood: { type: Type.ARRAY, items: { type: Type.STRING } },
    sfx: { type: Type.ARRAY, items: AUDIO_CUE_SCHEMA },
    stopPreviousAmbience: { type: Type.BOOLEAN },
  },
  required: ['environment', 'ambience', 'ambienceDetails', 'musicMood', 'sfx', 'stopPreviousAmbience'],
};


const MASTER_COMMAND_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    type: { type: Type.STRING, enum: [
      'adjust_endurance', 'adjust_hope', 'spend_hope', 'spend_fellowship_for_hope', 'adjust_shadow', 'adjust_fatigue', 'adjust_supplies',
      'set_condition', 'set_present_npcs', 'upsert_npc', 'update_npc_actor', 'open_scene', 'close_scene', 'set_spotlight',
      'upsert_threat', 'advance_threat', 'upsert_quest', 'add_knowledge', 'advance_time',
      'set_mode', 'start_journey', 'resolve_journey_march', 'generate_journey_event', 'resolve_journey_event', 'set_journey_danger', 'continue_journey_stage', 'complete_journey', 'start_council', 'record_council_result', 'resolve_council',
      'start_combat', 'set_combat_stance', 'next_combat_round', 'end_combat', 'upsert_combat_actor', 'move_combat_actor', 'record_combat_narrative', 'record_combat_roll', 'set_combat_background', 'engage_combat_actors', 'resolve_hero_attack', 'resolve_adversary_attack', 'resolve_first_aid',
      'upsert_organization', 'archive_organization', 'delete_organization', 'advance_organization_project', 'upsert_stronghold', 'claim_stronghold', 'abandon_stronghold', 'delete_stronghold', 'transfer_stronghold_loot', 'transfer_stronghold_treasure', 'record_living_activity', 'start_fellowship_phase', 'end_fellowship_phase', 'resolve_shadow_gain', 'firm_will', 'bout_of_madness', 'advance_skill', 'advance_combat_proficiency', 'advance_valor', 'advance_wisdom', 'heal_shadow_scar', 'record_undertaking', 'resolve_rest',
    ] },
    targetCharacterId: { type: Type.STRING },
    amount: { type: Type.INTEGER },
    condition: { type: Type.STRING, enum: ['wounded', 'weary', 'miserable'] },
    attribute: { type: Type.STRING, enum: ['strength', 'heart', 'wits'] },
    stance: { type: Type.STRING, enum: ['forward', 'open', 'defensive', 'rearward'] },
    success: { type: Type.BOOLEAN },
    value: { type: Type.BOOLEAN },
    npcIds: { type: Type.ARRAY, items: { type: Type.STRING } },
    npcId: { type: Type.STRING },
    npc: { type: Type.OBJECT, properties: {
      id: { type: Type.STRING }, name: { type: Type.STRING }, culture: { type: Type.STRING }, peoples: { type: Type.STRING },
      role: { type: Type.STRING }, faction: { type: Type.STRING }, appearance: { type: Type.STRING }, personality: { type: Type.STRING },
      attitude: { type: Type.STRING }, objectives: { type: Type.STRING }, fears: { type: Type.STRING }, secrets: { type: Type.STRING },
      notes: { type: Type.STRING }, trust: { type: Type.INTEGER }, location: { type: Type.STRING }, state: { type: Type.STRING },
      speechStyle: { type: Type.STRING }, emotionalState: { type: Type.STRING }, nextAction: { type: Type.STRING },
      voiceProfile: { type: Type.STRING }, presentInScene: { type: Type.BOOLEAN },
    }, required: ['name'] },
    kind: { type: Type.STRING, enum: ['exploration', 'dialogue', 'journey', 'council', 'combat', 'rest', 'fellowship'] },
    title: { type: Type.STRING },
    participantHeroIds: { type: Type.ARRAY, items: { type: Type.STRING } },
    participantNpcIds: { type: Type.ARRAY, items: { type: Type.STRING } },
    tension: { type: Type.INTEGER },
    endCondition: { type: Type.STRING },
    emotionalState: { type: Type.STRING },
    nextAction: { type: Type.STRING },
    beliefs: { type: Type.ARRAY, items: { type: Type.STRING } },
    suspicions: { type: Type.ARRAY, items: { type: Type.STRING } },
    location: { type: Type.STRING },
    clockId: { type: Type.STRING },
    minutes: { type: Type.INTEGER },
    mode: { type: Type.STRING, enum: ['adventure', 'journey', 'council', 'combat', 'fellowship'] },
    sceneKind: { type: Type.STRING, enum: ['exploration', 'dialogue', 'journey', 'council', 'combat', 'rest', 'fellowship'] },
    spotlightCharacterId: { type: Type.STRING },
    destinationHexId: { type: Type.STRING },
    objective: { type: Type.STRING },
    tolerance: { type: Type.INTEGER },
    participants: { type: Type.ARRAY, items: { type: Type.STRING } },
    actorIds: { type: Type.ARRAY, items: { type: Type.STRING } },
    actorId: { type: Type.STRING },
    x: { type: Type.NUMBER },
    y: { type: Type.NUMBER },
    text: { type: Type.STRING },
    imageData: { type: Type.STRING },
    actor: { type: Type.OBJECT, properties: {
      actorId: { type: Type.STRING }, actorType: { type: Type.STRING, enum: ['hero', 'npc', 'adversary'] }, stance: { type: Type.STRING, enum: ['forward', 'open', 'defensive', 'rearward'] },
      engagedWith: { type: Type.ARRAY, items: { type: Type.STRING } }, endurance: { type: Type.INTEGER }, enduranceMax: { type: Type.INTEGER }, hate: { type: Type.INTEGER }, wounded: { type: Type.BOOLEAN }, defeated: { type: Type.BOOLEAN }, initiative: { type: Type.INTEGER }, tokenId: { type: Type.STRING }, position: { type: Type.OBJECT, properties: { x: { type: Type.NUMBER }, y: { type: Type.NUMBER } } }, quickSheet: { type: Type.OBJECT, properties: {} },
    }, required: ['actorId', 'actorType', 'endurance', 'enduranceMax', 'wounded', 'defeated'] },
    roll: { type: Type.OBJECT, properties: { id: { type: Type.STRING }, actorId: { type: Type.STRING }, label: { type: Type.STRING }, featDie: { type: Type.INTEGER }, successDice: { type: Type.ARRAY, items: { type: Type.INTEGER } }, total: { type: Type.INTEGER }, targetNumber: { type: Type.INTEGER }, success: { type: Type.BOOLEAN }, tengwar: { type: Type.INTEGER }, createdAt: { type: Type.INTEGER } } },
    organizationId: { type: Type.STRING },
    strongholdId: { type: Type.STRING },
    lootItemId: { type: Type.STRING },
    quantity: { type: Type.INTEGER },
    archived: { type: Type.BOOLEAN },
    projectId: { type: Type.STRING },
    organization: { type: Type.OBJECT, properties: { id: { type: Type.STRING }, name: { type: Type.STRING }, kind: { type: Type.STRING }, culture: { type: Type.STRING }, agenda: { type: Type.STRING }, resources: { type: Type.STRING }, territory: { type: Type.ARRAY, items: { type: Type.STRING } }, influence: { type: Type.INTEGER }, partyReputation: { type: Type.INTEGER }, nextAction: { type: Type.STRING }, active: { type: Type.BOOLEAN }, archived: { type: Type.BOOLEAN }, generatedBy: { type: Type.STRING } }, required: ['id', 'name', 'kind'] },
    stronghold: { type: Type.OBJECT, properties: { id: { type: Type.STRING }, name: { type: Type.STRING }, locationId: { type: Type.STRING }, hexId: { type: Type.STRING }, ownerOrganizationId: { type: Type.STRING }, ownerKind: { type: Type.STRING, enum: ['organization', 'company', 'unclaimed'] }, status: { type: Type.STRING, enum: ['rumored', 'discovered', 'contested', 'controlled', 'abandoned', 'ruined'] }, knownToCompany: { type: Type.BOOLEAN }, autoGenerated: { type: Type.BOOLEAN }, facilities: { type: Type.ARRAY, items: { type: Type.STRING } }, defenders: { type: Type.INTEGER }, supplies: { type: Type.INTEGER }, treasure: { type: Type.INTEGER }, threats: { type: Type.ARRAY, items: { type: Type.STRING } }, notes: { type: Type.STRING } }, required: ['id', 'name'] },
    activity: { type: Type.OBJECT, properties: { id: { type: Type.STRING }, kind: { type: Type.STRING }, title: { type: Type.STRING }, detail: { type: Type.STRING }, day: { type: Type.INTEGER }, sourceId: { type: Type.STRING } }, required: ['id', 'kind', 'title', 'detail', 'day'] },
    outcome: { type: Type.STRING, enum: ['success', 'partial', 'failure'] },
    locationId: { type: Type.STRING },
    reason: { type: Type.STRING },
    source: { type: Type.STRING },
    quest: { type: Type.OBJECT, properties: {} },
    clock: { type: Type.OBJECT, properties: {} },
    entry: { type: Type.OBJECT, properties: {} },
    roles: { type: Type.OBJECT, properties: {} },
  },
  required: ['type', 'reason'],
};

const DIRECTOR_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    intent: { type: Type.STRING },
    actingCharacterId: { type: Type.STRING },
    scope: { type: Type.STRING, enum: ['hero', 'company'] },
    mode: { type: Type.STRING, enum: ['adventure', 'journey', 'council', 'combat', 'fellowship'] },
    sceneKind: { type: Type.STRING, enum: ['exploration', 'dialogue', 'journey', 'council', 'combat', 'rest', 'fellowship'] },
    spotlightCharacterId: { type: Type.STRING },
    requiredTest: TEST_SCHEMA,
    commands: { type: Type.ARRAY, items: MASTER_COMMAND_SCHEMA },
    npcDecisions: { type: Type.ARRAY, items: { type: Type.OBJECT, properties: {
      npcId: { type: Type.STRING }, decision: { type: Type.STRING }, visibleNow: { type: Type.BOOLEAN },
    }, required: ['npcId', 'decision', 'visibleNow'] } },
    revelationsAllowed: { type: Type.ARRAY, items: { type: Type.STRING } },
    visibleFacts: { type: Type.ARRAY, items: { type: Type.STRING } },
    hiddenNotes: { type: Type.ARRAY, items: { type: Type.STRING } },
    narrationBrief: { type: Type.STRING },
    ruleSourceNumbers: { type: Type.ARRAY, items: { type: Type.INTEGER } },
    ruleSummary: { type: Type.STRING },
  },
  required: ['intent', 'scope', 'mode', 'commands', 'npcDecisions', 'revelationsAllowed', 'visibleFacts', 'hiddenNotes', 'narrationBrief', 'ruleSourceNumbers', 'ruleSummary'],
};

const VOICE_SEGMENT_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    speakerType: { type: Type.STRING, enum: ['narrator', 'npc', 'hero'] },
    speakerId: { type: Type.STRING },
    speakerName: { type: Type.STRING },
    text: { type: Type.STRING },
    emotion: { type: Type.STRING },
  },
  required: ['speakerType', 'speakerName', 'text'],
};

const NARRATOR_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    atmospheric_narration: { type: Type.STRING },
    action_result: { type: Type.STRING },
    visualPrompt: { type: Type.STRING },
    sceneVisualPrompt: { type: Type.STRING },
    location: { type: Type.STRING },
    weather: { type: Type.STRING },
    choices: { type: Type.ARRAY, items: CHOICE_SCHEMA },
    voiceScript: { type: Type.ARRAY, items: VOICE_SEGMENT_SCHEMA },
    audioPlan: AUDIO_PLAN_SCHEMA,
    events: { type: Type.ARRAY, items: EVENT_SCHEMA },
    narrativeLog: { type: Type.STRING },
    imageRecommended: { type: Type.BOOLEAN },
    usedSourceNumbers: { type: Type.ARRAY, items: { type: Type.INTEGER } },
    sourceUsageSummary: { type: Type.STRING },
  },
  required: ['atmospheric_narration', 'action_result', 'visualPrompt', 'sceneVisualPrompt', 'location', 'choices', 'audioPlan', 'events', 'imageRecommended', 'usedSourceNumbers', 'sourceUsageSummary'],
};

const RULE_ANSWER_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    answer: { type: Type.STRING },
    ruleApplied: { type: Type.STRING },
    examples: { type: Type.ARRAY, items: { type: Type.STRING } },
    uncertainty: { type: Type.STRING },
    usedSourceNumbers: { type: Type.ARRAY, items: { type: Type.INTEGER } },
  },
  required: ['answer', 'ruleApplied', 'examples', 'uncertainty', 'usedSourceNumbers'],
};

const STORY_SCHEMA = {
  type: Type.OBJECT,
  properties: {
    atmospheric_narration: { type: Type.STRING },
    action_result: { type: Type.STRING },
    visualPrompt: { type: Type.STRING },
    sceneVisualPrompt: { type: Type.STRING },
    location: { type: Type.STRING },
    weather: { type: Type.STRING },
    choices: { type: Type.ARRAY, items: CHOICE_SCHEMA },
    requiredTest: TEST_SCHEMA,
    audioPlan: AUDIO_PLAN_SCHEMA,
    events: { type: Type.ARRAY, items: EVENT_SCHEMA },
    hpChange: { type: Type.INTEGER },
    sanityChange: { type: Type.INTEGER },
    suppliesChange: { type: Type.INTEGER },
    inventoryChanges: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: { item: { type: Type.STRING }, change: { type: Type.INTEGER } },
        required: ['item', 'change'],
      },
    },
    narrativeLog: { type: Type.STRING },
    imageRecommended: { type: Type.BOOLEAN },
    usedSourceNumbers: { type: Type.ARRAY, items: { type: Type.INTEGER } },
    sourceUsageSummary: { type: Type.STRING },
  },
  required: [
    'atmospheric_narration', 'action_result', 'visualPrompt', 'sceneVisualPrompt', 'location',
    'choices', 'audioPlan', 'events', 'imageRecommended', 'usedSourceNumbers', 'sourceUsageSummary',
  ],
};

function normalizeAudioText(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

function normalizeResponse(raw: GeminiStoryResponse, allowEmptyNarration = false): GeminiStoryResponse {
  const choices = normalizeStoryChoices(raw.choices as StoryChoice[]);
  const narration = raw.atmospheric_narration?.trim() || (allowEmptyNarration ? '' : 'O instante permanece como foi estabelecido, à espera da próxima ação concreta.');
  const normalizedNarration = normalizeAudioText(narration);
  const events = (raw.events || []).map((event, index) => ({
    ...event,
    id: event.id || `event_${Date.now()}_${index}`,
    timestamp: event.timestamp || Date.now(),
  }));
  const combatIsActive = events.some(event => event.type === 'combat_started')
    || /\b(combate comeca|luta comeca|iniciativa|espadas se chocam|atacam voces)\b/.test(normalizedNarration);
  const sourcePlan = raw.audioPlan || {
    environment: [], ambience: [], ambienceDetails: [], musicMood: [], sfx: [], stopPreviousAmbience: false,
  };
  const groundedSfx = (sourcePlan.sfx || [])
    .filter(cue => {
      const trigger = normalizeAudioText(cue.triggerText || '');
      return trigger.length >= 4 && normalizedNarration.includes(trigger);
    })
    .slice(0, 2);
  const musicMood = (sourcePlan.musicMood || [])
    .map(value => normalizeAudioText(value))
    .filter(value => combatIsActive || !/battle|combat|fight|tension/.test(value))
    .slice(0, 1);

  return {
    ...raw,
    choices,
    atmospheric_narration: narration,
    action_result: raw.action_result?.trim() || '',
    location: raw.location?.trim() || 'Eriador',
    events,
    usedSourceNumbers: Array.from(new Set((raw.usedSourceNumbers || []).map(Number).filter(value => Number.isInteger(value) && value >= 1 && value <= 5))),
    sourceUsageSummary: raw.sourceUsageSummary?.trim() || '',
    voiceScript: Array.isArray(raw.voiceScript)
      ? raw.voiceScript.map(segment => ({ ...segment, speakerName: String(segment.speakerName || 'Narrador'), text: String(segment.text || '').trim(), emotion: String(segment.emotion || '') })).filter(segment => segment.text).slice(0, 30)
      : [],
    audioPlan: {
      environment: (sourcePlan.environment || []).slice(0, 4),
      ambience: (sourcePlan.ambience || []).slice(0, 1),
      ambienceDetails: (sourcePlan.ambienceDetails || []).slice(0, 1),
      musicMood,
      sfx: groundedSfx,
      stopPreviousAmbience: Boolean(sourcePlan.stopPreviousAmbience),
    },
  };
}


function normalizeDirectorPlan(raw: Partial<DirectorPlan>): DirectorPlan {
  return {
    intent: String(raw.intent || 'interpretar ação'),
    narrativeIntentClass: raw.narrativeIntentClass || 'action-diegetic',
    advancesFiction: raw.advancesFiction !== false,
    actingCharacterId: raw.actingCharacterId || undefined,
    scope: raw.scope === 'company' ? 'company' : 'hero',
    mode: ['adventure', 'journey', 'council', 'combat', 'fellowship'].includes(String(raw.mode)) ? raw.mode as DirectorPlan['mode'] : 'adventure',
    sceneKind: ['exploration', 'dialogue', 'journey', 'council', 'combat', 'rest', 'fellowship'].includes(String(raw.sceneKind)) ? raw.sceneKind as DirectorPlan['sceneKind'] : undefined,
    spotlightCharacterId: raw.spotlightCharacterId || undefined,
    requiredTest: raw.requiredTest || null,
    commands: Array.isArray(raw.commands) ? raw.commands.slice(0, 20) : [],
    npcDecisions: Array.isArray(raw.npcDecisions) ? raw.npcDecisions.slice(0, 20) : [],
    revelationsAllowed: Array.isArray(raw.revelationsAllowed) ? raw.revelationsAllowed.slice(0, 20) : [],
    visibleFacts: Array.isArray(raw.visibleFacts) ? raw.visibleFacts.slice(0, 30) : [],
    hiddenNotes: Array.isArray(raw.hiddenNotes) ? raw.hiddenNotes.slice(0, 30) : [],
    narrationBrief: String(raw.narrationBrief || ''),
    ruleSourceNumbers: Array.from(new Set((raw.ruleSourceNumbers || []).map(Number).filter(value => Number.isInteger(value) && value >= 1 && value <= 5))),
    ruleSummary: String(raw.ruleSummary || ''),
  };
}

export async function generateDirectorPlan(context: string, userAction: string): Promise<DirectorPlan> {
  const prompt = `[DIRETOR OCULTO DO TURNO]\n${context}\n\nAÇÃO DECLARADA: ${userAction}\n\nInterprete a intenção. Identifique explicitamente o Herói-jogador que age ou use scope=company. Respeite o estado formal da cena e distribua protagonismo entre todos os Heróis-jogadores. Planeje NPCs, regras, segredos e mudanças. Quando um NPC novo entrar na ficção, emita upsert_npc com nome, povo/cultura, papel, gênero implícito quando pertinente, idade aparente, aparência detalhada, equipamento, ambiente, personalidade, forma de falar, emoção, objetivo, medo, segredo, localização, próxima ação e presentInScene=true. Esses detalhes serão usados localmente para escolher um token da pasta /npc; não invente nomes de arquivos. Use update_npc_actor para NPCs existentes. Quando a ficção estabelecer um Poder persistente — Casa de Patrono, Companhia de Guardiões, clã, casa mercantil, guilda de ofício, ordem, culto, bando inimigo ou conselho — emita upsert_organization em vez de deixá-lo apenas no texto. Quando surgir um local persistente relevante como refúgio, salão, torre, fortificação, ruína ocupada, acampamento, covil ou fortaleza, emita upsert_stronghold com proprietário, localização, instalações observáveis, defensores, suprimentos, tesouro conhecido e ameaças; o aplicativo gera apenas o estoque mundano plausível e persistente. Se a Companhia conquistar de forma inequivocamente validada um local já registrado e o confronto estiver encerrado, emita claim_stronghold; nunca conceda posse só porque os Heróis chegaram perto. Se o local for abandonado voluntariamente, use abandon_stronghold. Não invente bônus mecânicos por possuir um refúgio. Não distribua loot automaticamente para fichas: o estoque conquistado fica no local e o jogador decide quem carrega cada item. Abra ou atualize a cena com open_scene SOMENTE quando tipo, objetivo ou participantes realmente mudarem. Uma resposta, apresentação, pergunta, cumprimento ou continuação de conversa NÃO abre nova cena e não remove NPCs presentes. Antes de planejar, classifique mentalmente a mensagem como ação diegética, fala diegética, teste/mecânica, consulta de regra, consulta de contexto, recapitulação, clarificação espacial ou intenção/planejamento. Consulta, recapitulação, clarificação e planejamento NÃO avançam relógio, NPCs, ameaças, mapa nem cena. Use CONTEXTO; ESPAÇO; MOVIMENTO; FOCO; IMPLICAÇÃO; PONTO DE PARADA como checklist de direção, NÃO como obrigação de produzir seis blocos. A CADÊNCIA DE MESA do contexto define quantas batidas cabem neste turno. Em fala diegética, a reação/resposta do interlocutor é a primeira batida; em exploração, revele apenas a camada espacial atual; em Jornada, pare no evento/decisão; em Conselho, use poucas intervenções antes da réplica. Em visibleFacts coloque sinais observáveis, não conclusões ocultas. Em revelationsAllowed libere somente o que foi efetivamente conquistado por ação, evidência ou regra. Não escreva narração. Não revele hiddenNotes ao jogador. Nunca mova a Companhia por prosa: movimento no mapa é comando local. Só emita alterações de ficha com targetCharacterId válido e razão concreta. Se a ação exigir teste e ainda não houver rolagem, preencha requiredTest e não emita comandos que dependam do resultado.`;
  const response = await generateTextGemini( [{ parts: [{ text: prompt }] }], {
    responseMimeType: 'application/json', responseSchema: DIRECTOR_SCHEMA, temperature: 0.15, maxOutputTokens: 2600,
  }, `Você é o Diretor oculto de uma mesa de O Um Anel 2ª edição. Um único jogador humano controla vários Heróis-jogadores. Você planeja e arbitra, mas não narra e nunca grava diretamente no banco. Separe verdade secreta, fatos visíveis e revelações permitidas. Use apenas as regras e o estado fornecidos pelo host; fontes recuperadas são evidência auxiliar.`);
  return normalizeDirectorPlan(parseJsonResponse<Partial<DirectorPlan>>(response as any, 'Plano do Diretor'));
}

type NarrativeDensityContract = { cadence:string; minChars:number; minParagraphs:number };

function narrativeDensityContract(context:string): NarrativeDensityContract {
  const cadence=(context.match(/Cadência deste turno:\s*([a-z_-]+)/i)?.[1]||'').toLowerCase();
  const minChars=Number(context.match(/REGRA DE DENSIDADE OBRIGATÓRIA DESTA CENA:[^\n]*pelo menos\s+(\d+)\s+caracteres/i)?.[1]||0);
  const para=context.match(/Faixa de presença:\s*(\d+)\s*[-–]\s*(\d+)\s+parágrafos/i);
  let minParagraphs=Number(para?.[1]||0);
  if(cadence==='opening') minParagraphs=Math.max(9,minParagraphs);
  return {cadence,minChars:cadence==='opening'?Math.max(6500,minChars):minChars,minParagraphs};
}

function narrativeParagraphCount(text:string):number {
  const clean=String(text||'').trim();
  return clean?clean.split(/\n\s*\n/).map(value=>value.trim()).filter(Boolean).length:0;
}

function meetsNarrativeDensity(response:GeminiStoryResponse, contract:NarrativeDensityContract):boolean {
  if(contract.minChars<=0) return true;
  const narration=String(response.atmospheric_narration||'').trim();
  return narration.length>=contract.minChars && narrativeParagraphCount(narration)>=Math.max(1,contract.minParagraphs);
}

export async function generateNarrationFromPlan(
  publicContext: string,
  userAction: string,
  plan: DirectorPlan,
): Promise<GeminiStoryResponse> {
  const safePlan = {
    intent: plan.intent,
    narrativeIntentClass: plan.narrativeIntentClass,
    advancesFiction: plan.advancesFiction !== false,
    actingCharacterId: plan.actingCharacterId,
    scope: plan.scope,
    mode: plan.mode,
    visibleFacts: plan.visibleFacts,
    revelationsAllowed: plan.revelationsAllowed,
    narrationBrief: plan.narrationBrief,
    npcDecisions: plan.npcDecisions.filter(item => item.visibleNow),
    ruleSourceNumbers: plan.ruleSourceNumbers,
    ruleSummary: plan.ruleSummary,
  };
  const isFrozenReply = plan.advancesFiction === false;
  const prompt = `[NARRADOR — SOMENTE O QUE OS JOGADORES PODEM PERCEBER]
${publicContext}

AÇÃO: ${userAction}

PLANO VISÍVEL VALIDADO:
${JSON.stringify(safePlan)}

PADRÃO DE EXECUÇÃO:
- Não invente mudanças mecânicas e não revele segredos ausentes do plano visível.
- A câmera normalmente vai PANORAMA → LOCAL → PERSONAGENS/OBJETOS → DETALHE MARCANTE, mas em continuação imediata comece exatamente onde a ação tocou a cena.
- Mostre o mundo em movimento. Uma pessoa entra fazendo algo; um guarda continua sua tarefa; um estalajadeiro seca um copo, conta moedas, fecha uma janela. Evite NPC parado apenas para entregar exposição.
- Informação aparece por sinais, objetos, posição, fala, hesitação e comportamento. Não escreva a conclusão quando o jogador ainda pode inferi-la.
- Para cada trecho, busque 1 imagem espacial forte + 1 movimento + 1 detalhe significativo + 1 possibilidade implícita.
- Responda todas as perguntas pertinentes contidas na fala do jogador.
- Não reexplique a cena inteira a cada turno e não encerre automaticamente com “O que você faz?”.
- Use TAMANHO ADAPTATIVO conforme a CADÊNCIA DE MESA e o peso da batida atual. Microresposta, rotina, diálogo simples e cada ação isolada de combate continuam curtos conforme o perfil. CENA IMERSIVA/IMPORTANTE deve respeitar a faixa enviada pela CADÊNCIA. Se a CADÊNCIA for opening, o mínimo maior é vinculante (normalmente 6.500–9.000 caracteres; 9–13 parágrafos) e 5.000 NÃO é teto nem alvo suficiente. O comprimento adicional deve aprofundar presença, espaço, sensorialidade, movimento, contexto perceptível e reação do mundo, nunca avançar decisões que ainda pertencem ao jogador.
- Fale como um Mestre sentado à mesa. Use segunda pessoa quando natural, mas varie a câmera e NÃO abra automaticamente por “Você/Vocês”. Em abertura, a câmera pode começar ampla e aproximar-se até a Companhia. Descreva o que está perceptível no espaço, reaja diretamente ao verbo da ação declarada e pare antes de decidir a próxima ação por eles. Cinematográfico não significa romance autônomo.
- SENSORIALIDADE ORGÂNICA: integre luz, distância, temperatura, vento/ar, sons, cheiro, textura, peso, esforço e movimento apenas quando fisicamente pertinentes. Nunca faça uma lista dos cinco sentidos.
- NÃO REPITA a narração anterior. O checkpoint contém o final da cena anterior apenas para continuidade. A nova atmospheric_narration deve começar pela consequência/reação da AÇÃO ATUAL e acrescentar informação, movimento, resposta, descoberta ou mudança perceptível nova.
- ABERTURA DE AVENTURA: siga o contrato Nível 4 e a faixa da CADÊNCIA, normalmente 6.500–9.000 caracteres, 9–13 parágrafos e cerca de 1.000–1.450 palavras. Faça panorama → limiar/local → Companhia → detalhe marcante; use o nome canônico da região/local; estabeleça momento/estação/horário sem soar como cabeçalho; mostre o mundo já em movimento e a identidade cultural por sinais observáveis; integre luz/visibilidade, temperatura/ar, sons, cheiros ou textura quando fisicamente pertinentes; introduza um primeiro estímulo concreto e pare na decisão. PRIMEIRA CHEGADA, MUDANÇA DE LOCAL, REVELAÇÃO OU CENA IMPORTANTE seguem a faixa própria da CADÊNCIA. Não faça lista dos sentidos, não resolva ações futuras e não despeje lore que o personagem não percebe.
- CENAS ROTINEIRAS continuam curtas. O texto longo é padrão apenas quando o jogador precisa construir mentalmente um lugar/situação nova ou importante.
- Trate cada resposta como uma janela interruptível de actual play: responda ao que acabou de acontecer, avance no máximo as batidas permitidas e pare enquanto a decisão ainda pertence ao jogador.
- Em diálogo, a primeira ou segunda frase deve mostrar a reação/resposta do interlocutor quando ele está presente. Em exploração, não revele além da camada espacial realmente examinada. Em Jornada, não resolva também o evento que acabou de surgir. Em Conselho, não encadeie todos os discursos antes de devolver espaço para réplica.
- action_result não é uma segunda narração: deixe vazio quando atmospheric_narration já contém a consequência ficcional.
${isFrozenReply ? '- ESTA RESPOSTA NÃO AVANÇA A FICÇÃO. atmospheric_narration deve ficar vazio. Responda diretamente em action_result apenas com contexto já conhecido/perceptível; choices deve ser []. Não gere evento, mudança de clima, tempo, posição, NPC ou imagem.' : '- ESTA RESPOSTA CONTINUA A FICÇÃO. atmospheric_narration deve começar pela consequência/reação imediata da ação atual e parar quando houver uma decisão real.'}
Separe a narração com linhas em branco entre os parágrafos. As sugestões são opcionais, nunca anunciam rolagens e a ação livre continua principal. Preencha voiceScript somente com o conteúdo audível de atmospheric_narration: use narrator para descrição e npc/hero para cada fala direta, com speakerId quando conhecido. Não repita falas.`;
  const importantScene = true;
  const cinematic = true;
  const models = getGeminiTextModels();
  let lastError: unknown;
  for (const model of models) {
    try {
      const response = await generateGemini(model, [{ parts: [{ text: prompt }] }], {
        responseMimeType: 'application/json', responseSchema: NARRATOR_SCHEMA, temperature: 0.78, maxOutputTokens: 12000,
      }, SYSTEM_INSTRUCTION + '\nVocê é apenas o Narrador. Não recebe nem revela segredos do Diretor. Não altera fichas nem decide regras.');
      let normalized = normalizeResponse(parseJsonResponse<GeminiStoryResponse>(response as any, 'Narração'), isFrozenReply);
      const density = narrativeDensityContract(publicContext);
      if (!isFrozenReply && density.minChars > 0 && !meetsNarrativeDensity(normalized, density)) {
        const firstLength=String(normalized.atmospheric_narration||'').length;
        const firstParagraphs=narrativeParagraphCount(String(normalized.atmospheric_narration||''));
        for (let densityAttempt=0; densityAttempt<2 && !meetsNarrativeDensity(normalized,density); densityAttempt+=1) {
          const densityPrompt = `${prompt}\n\n--- REPARO REMOTO OBRIGATÓRIO DO PERFIL TÉCNICO ---\nA resposta anterior foi REPROVADA programaticamente: ${String(normalized.atmospheric_narration||'').length} caracteres e ${narrativeParagraphCount(String(normalized.atmospheric_narration||''))} parágrafos.\nContrato vinculante: atmospheric_narration deve ter PELO MENOS ${density.minChars} caracteres e PELO MENOS ${density.minParagraphs} parágrafos.\nReescreva A NARRAÇÃO COMPLETA, não um complemento e não um resumo. Preserve exatamente local, horário, clima, NPCs, fatos, consequências e primeira decisão já validados. Não crie nova ação do jogador, não avance tempo, não resolva a decisão e não invente regra.\nO PERFIL TÉCNICO CANÔNICO abaixo é obrigatório em comportamento, não apenas referência:\n${HUMAN_MASTER_RUNTIME_CONTRACT}\n${HUMAN_MASTER_PROFILE}\n--- FIM DO PERFIL ---\n\nNARRAÇÃO REPROVADA (use apenas como fatos já estabelecidos; reescreva por inteiro):\n${String(normalized.atmospheric_narration||'')}`;
          const repaired = await generateGemini(model,[{parts:[{text:densityPrompt}]}],{
            responseMimeType:'application/json',responseSchema:NARRATOR_SCHEMA,temperature:0.78,maxOutputTokens:16000,
          },SYSTEM_INSTRUCTION+'\nCONTRATO DE SAÍDA: se houver mínimo de densidade no contexto, ele é obrigatório. Nunca entregue texto abaixo dele.');
          normalized=normalizeResponse(parseJsonResponse<GeminiStoryResponse>(repaired as any,'Reparo de densidade'),false);
        }
        if (!meetsNarrativeDensity(normalized,density)) {
          lastError=new Error(`Modelo ${model} recusado pelo perfil: ${String(normalized.atmospheric_narration||'').length}/${density.minChars} caracteres e ${narrativeParagraphCount(String(normalized.atmospheric_narration||''))}/${density.minParagraphs} parágrafos; primeira tentativa ${firstLength} chars/${firstParagraphs} parágrafos.`);
          console.warn('[BRASEIRO_PROFILE_HARD_GATE]',lastError);
          continue;
        }
      }
      rememberWorkingGeminiTextModel(model);
      (normalized as any)._braseiroModel = model;
      (normalized as any)._braseiroGenerationSource = 'gemini';
      (normalized as any)._braseiroProfileEnforced = true;
      return normalized;
    } catch (error) {
      lastError = error;
      if (!retryable(error)) break;
    }
  }
  throw new Error(`Nenhum modelo narrativo respondeu: ${errorText(lastError)}`);
}

export async function answerRulesQuestion(context: string, question: string): Promise<RuleAnswer> {
  const response = await generateTextGemini( [{ parts: [{ text: `[CONSULTA DE REGRAS]\n${context}\n\nPERGUNTA: ${question}` }] }], {
    responseMimeType: 'application/json', responseSchema: RULE_ANSWER_SCHEMA, temperature: 0.1, maxOutputTokens: 2000,
  }, 'Você é especialista em O Um Anel 2ª edição. Responda diretamente, sem continuar a história. Use somente as fontes recuperadas. Declare incerteza quando a regra não estiver presente.');
  const raw = parseJsonResponse<RuleAnswer>(response as any, 'Consulta de regras');
  return {
    answer: String(raw.answer || ''), ruleApplied: String(raw.ruleApplied || ''),
    examples: Array.isArray(raw.examples) ? raw.examples.slice(0, 5) : [], uncertainty: String(raw.uncertainty || ''),
    usedSourceNumbers: Array.from(new Set((raw.usedSourceNumbers || []).map(Number).filter(value => Number.isInteger(value) && value >= 1 && value <= 5))),
  };
}

export async function generateStorySegment(
  historySummary: string,
  userChoice: string,
  isDmMode?: boolean,
): Promise<GeminiStoryResponse> {
  const prompt = isDmMode
    ? `[CONSULTA DE REGRAS — NÃO CONTINUE A HISTÓRIA]\nCONTEXTO:\n${historySummary}\n\nDÚVIDA: ${userChoice}\nResponda de forma direta e cite os trechos recuperados quando existirem.`
    : `[PRÓXIMO TURNO]\n${historySummary}\n\nAÇÃO DO JOGADOR: ${userChoice}`;
  const cinematic = !isDmMode;
  const models = getGeminiTextModels();

  let lastError: unknown;
  for (const model of models) {
    try {
      const response = await generateGemini(
        model,
        [{ parts: [{ text: prompt }] }],
        {
          responseMimeType: 'application/json',
          responseSchema: STORY_SCHEMA,
          temperature: isDmMode ? 0.15 : cinematic ? 0.78 : 0.55,
          maxOutputTokens: isDmMode ? 1800 : 12000,
        },
        isDmMode
          ? 'Você é especialista em O Um Anel 2ª edição. Use apenas as regras recuperadas no contexto e não avance a narrativa.'
          : SYSTEM_INSTRUCTION,
      );
      const parsed = parseJsonResponse<GeminiStoryResponse>(response as any, 'Cena');
      console.info(`[BRASEIRO_MODEL] narrativa=${model} tier=${response._braseiroTier || 'desconhecido'}`);
      const normalized = normalizeResponse(parsed);
      rememberWorkingGeminiTextModel(model);
      (normalized as any)._braseiroModel = model;
      (normalized as any)._braseiroGenerationSource = 'gemini';
      return normalized;
    } catch (error) {
      lastError = error;
      console.warn(`[BRASEIRO_MODEL] falha em ${model}:`, error);
      if (!retryable(error)) break;
    }
  }
  throw new Error(`Nenhum modelo narrativo respondeu: ${errorText(lastError)}`);
}

export type ImageQuality = 'fast' | 'quality';

export async function generateSceneImage(visualPrompt: string, quality: ImageQuality = 'fast'): Promise<string | null> {
  const models = quality === 'quality'
    ? [QUALITY_IMAGE_MODEL, FAST_IMAGE_MODEL]
    : [FAST_IMAGE_MODEL, QUALITY_IMAGE_MODEL];
  let lastError: unknown;
  for (const model of models) {
    try {
      const response = await generateGemini(model, [{ parts: [{ text: visualPrompt }] }], {
        responseModalities: ['IMAGE'], imageConfig: { aspectRatio: '16:9' },
      });
      for (const part of response.candidates?.[0]?.content?.parts || []) {
        if (part.inlineData?.data) {
          console.info(`[BRASEIRO_MODEL] imagem=${model}`);
          return `data:${part.inlineData.mimeType || 'image/png'};base64,${part.inlineData.data}`;
        }
      }
      throw new Error('O modelo não devolveu uma imagem.');
    } catch (error) {
      lastError = error;
      console.warn(`[BRASEIRO_MODEL] imagem falhou em ${model}:`, error);
      if (!retryable(error)) break;
    }
  }
  throw new Error(`Não foi possível gerar a imagem: ${errorText(lastError)}`);
}

function stableTextHash(value: string): string {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(36);
}

const ALLOWED_TTS_VOICES: TtsVoiceName[] = ['Charon', 'Kore', 'Puck', 'Fenrir', 'Aoede', 'Gacrux', 'Algenib', 'Sulafat'];
const TTS_TOTAL_TIMEOUT_MS = 150_000;
const narrationRequests = new Map<string, Promise<string | null>>();

const NATIVE_TTS_PREFIX = 'braseiro-native-tts:';

function encodeNativeTtsReference(value: { token: string; durationMs: number; model: string; tier: string }): string {
  return `${NATIVE_TTS_PREFIX}${encodeURIComponent(JSON.stringify(value))}`;
}


async function withTtsTimeout<T>(promise: Promise<T>): Promise<T> {
  let timer = 0;
  try {
    return await Promise.race([
      promise,
      new Promise<T>((_, reject) => {
        timer = window.setTimeout(() => reject(new Error('O serviço de voz demorou mais de 75 segundos e foi interrompido. Toque novamente; a aventura continua salva.')), TTS_TOTAL_TIMEOUT_MS);
      }),
    ]);
  } finally {
    if (timer) window.clearTimeout(timer);
  }
}

export async function generateNarration(text: string, voiceName: TtsVoiceName = 'Charon', style = 'natural'): Promise<string | null> {
  const normalizedText = String(text ?? '').normalize('NFC').replace(/\r\n?/g, '\n').replace(/[\t ]+/g, ' ').replace(/\n{3,}/g, '\n\n').trim();
  if (!normalizedText) return null;
  const safeVoice: TtsVoiceName = ALLOWED_TTS_VOICES.includes(voiceName) ? voiceName : 'Charon';
  const stableStyle = String(style || 'natural').trim();
  const nativeTts = window.BraseiroAndroid?.generateTts;
  if (nativeTts) {
    let nativeError: unknown;
    for (const model of getTtsModels()) {
      for (let attempt = 0; attempt < 2; attempt += 1) {
        try {
          const artifact = await withTtsTimeout(nativeTts(model, normalizedText, safeVoice, stableStyle, false));
          if (!artifact?.token || !Number.isFinite(artifact.durationMs) || artifact.durationMs <= 0) throw new Error('O Android não recebeu um arquivo de voz válido.');
          console.info(`[BRASEIRO_MODEL] tts-nativo=${artifact.model || model} tier=${artifact.tier || 'desconhecido'} tentativa=${attempt + 1}`);
          return encodeNativeTtsReference(artifact);
        } catch (error) {
          nativeError = error;
          console.warn(`[BRASEIRO_MODEL] TTS nativo falhou em ${model}, tentativa ${attempt + 1}:`, error);
          if (!retryable(error)) break;
          if (attempt === 0) await new Promise(resolve => window.setTimeout(resolve, 700));
        }
      }
    }
    // Charon/Gemini e Android/Kokoro são backends separados. Nunca faça
    // fallback silencioso entre eles: o usuário escolhe explicitamente.
    throw new Error(`Nenhum modelo TTS Charon respondeu: ${errorText(nativeError)}. A voz Android/Kokoro permanece disponível como opção separada.`);
  }
  const cacheKey = `tts-v9-${safeVoice}-pt-BR-1.0-${stableTextHash(`${getTtsModels().join(',')}|${stableStyle}|${normalizedText}`)}`;
  const cached = await getTtsCache(cacheKey);
  if (cached) {
    console.info('[BRASEIRO_MODEL] TTS recuperado do cache local');
    return cached;
  }

  const active = narrationRequests.get(cacheKey);
  if (active) return active;
  const request = withTtsTimeout((async () => {
    let lastError: unknown;
    for (const model of getTtsModels()) {
      for (let attempt = 0; attempt < 2; attempt += 1) {
        try {
          const response = await generateGemini(
            model,
            [{ parts: [{ text: `Sintetize uma narração contínua, mantendo exatamente o mesmo locutor, timbre, idade aparente, sotaque, altura e ritmo do início ao fim. Perfil fixo: ${stableStyle}; português brasileiro, dicção natural e atuação contida, sem caricatura. Pausas entre parágrafos não autorizam mudança de voz. Não leia estas instruções. Leia exatamente o texto entre <fala> e </fala>, sem acrescentar palavras.\n<fala>${normalizedText}</fala>` }] }],
            {
              responseModalities: ['AUDIO'],
              speechConfig: { languageCode: 'pt-BR', voiceConfig: { prebuiltVoiceConfig: { voiceName: safeVoice } } },
            },
          );
          const audio = response.candidates?.[0]?.content?.parts?.find(part => part.inlineData)?.inlineData?.data;
          if (!audio) throw new Error('O TTS não devolveu áudio.');
          await setTtsCache(cacheKey, audio).catch(() => undefined);
          console.info(`[BRASEIRO_MODEL] tts=${model}`);
          return audio;
        } catch (error) {
          lastError = error;
          console.warn(`[BRASEIRO_MODEL] TTS falhou em ${model} tentativa ${attempt + 1}:`, error);
          if (!retryable(error)) break;
          if (attempt === 0) await new Promise(resolve => window.setTimeout(resolve, 650));
        }
      }
    }
    throw new Error(`Nenhum modelo TTS respondeu: ${errorText(lastError)}`);
  })());
  narrationRequests.set(cacheKey, request);
  try {
    return await request;
  } finally {
    if (narrationRequests.get(cacheKey) === request) narrationRequests.delete(cacheKey);
  }
}
