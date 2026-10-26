# HANDOFF MESTRE — BRASEIRO: AVENTURAS IMPORTÁVEIS, MEMÓRIA BIOGRÁFICA E VIDA PERSISTENTE

**Data de consolidação:** 20/10/2026  
**Status:** ESPECIFICAÇÃO PRESERVADA — IMPLEMENTAÇÃO PAUSADA A PEDIDO DO USUÁRIO  
**Repositório principal conhecido:** `bra83/braseirobuild` (privado)  
**Destino:** TODOS OS VTTs Braseiro, com um núcleo compartilhado e adaptadores por sistema  
**Documento:** handoff integral para arquivar, consultar e retomar futuramente.  
**Regra de retomada:** não começar alterações no GitHub, commits, merges, builds, integrações ou agendamentos sem nova ordem expressa do usuário.

---

## 1. VISÃO E INTENÇÃO EXATA DO USUÁRIO

O usuário quer importar uma aventura por JSON em qualquer VTT Braseiro, jogá-la livremente e, ao terminá-la, **continuar usando os mesmos personagens no mesmo mundo**. Todo fato vivido, feito, fracasso, consequência, relacionamento e mudança pessoal relevante deve sobreviver ao encerramento, à exclusão/arquivamento do módulo e ao início de outras aventuras — inclusive aventuras **geradas autonomamente pelo Mestre de IA**, sem novo JSON importado.

O objetivo **não é apenas fama ou uma lista de conquistas**. É criar personagens que se transformam ao viver: comportamento, hábitos, medos, sono, relações amorosas e familiares, emprego, dívidas, sequelas, recuperação, vícios possíveis, virtudes adquiridas, reputação e envelhecimento. O mundo ao redor também evolui e reage.

Frase norteadora: **"A aventura termina; o personagem e o mundo continuam. O passado não precisa voltar como vilão para continuar tendo consequências."**

O usuário solicitou um mecanismo adicional de **tabelas ocultas de aleatoriedade**, uma biblioteca temática para cada VTT, com centenas ou milhares de entradas e cruzamentos entre diferentes colunas (causa, consequência, comportamento, saúde, vida social, doença, vício, tempo, repercussão etc.), selecionáveis por dados como `1d100` e conjuntos de `d100`. Isso deve produzir consequências humanas **plausíveis, variadas, causais e persistentes**; não uma lista mecânica de efeitos desconexos.

**Estado do trabalho:** nesta conversa o usuário disse "Pause" e, depois, pediu somente este handoff para guardar. Tudo abaixo é contrato e orientação para implementação FUTURA, não autorização para executá-la agora.

---

## 2. EXEMPLOS CANÔNICOS DO COMPORTAMENTO DESEJADO

### 2.1. Garrick, o matador de dragão — D&D

1. Uma aventura importada contém um dragão. Garrick de fato o derrota; o motor de combate confirma o resultado.
2. Registra-se o feito, data, lugar, participantes, testemunhas e consequências. O título "matador de dragão" **não deve ser atribuído** só porque o JSON dizia que havia um dragão.
3. A aventura A termina. O personagem mantém ficha, feitos, equipamentos legítimos, cicatrizes, relações, dívidas, fama e inimigos.
4. O usuário importa a aventura B, ou deixa o Mestre criar uma aventura B procedural.
5. Em uma estalagem próxima ao local do feito, alguém pode reconhecê-lo. Em outro reino, uma pessoa pode ter ouvido uma versão distorcida. Quem não teve acesso à informação não deve saber.
6. O dragão morto pode ter deixado um território vago, reorganizando rotas, comércio, cultos ou rivalidades. Essa linha causal pode alimentar aventuras futuras, sem obrigatoriamente retomá-la imediatamente.

### 2.2. Detetive Marrone — GURPS Investigação (EXEMPLO CENTRAL)

**Aventura importada A:** após meses de investigação, Marrone captura um serial killer. Ele perde um colega, vê atrocidades e sofre uma experiência emocionalmente marcante. O criminoso é preso; o estado canônico diz **"preso"**, não "solto" ou "morto".

**Anos depois, aventura B autogerada:** o Mestre inicia uma investigação comum sobre uma pessoa desaparecida. O caso novo não precisa estar conectado ao serial killer. O passado de Marrone pode se manifestar pela insônia, pesadelos, atenção exagerada a detalhes, evitamento de situações, exaustão no trabalho, conflitos conjugais, afastamento de familiares, medicação prescrita, possível abuso/dependência, culpa, mudança de hábitos, apoio de colegas, tratamento, recuperação ou amadurecimento. **Nenhuma consequência é automática nem inevitável.**

**Possível retorno externo do passado:** mais adiante, Marrone recebe cartas assinadas com o nome do serial killer. Podem ser cartas legítimas enviadas de dentro da prisão, uma falsificação, um imitador, um cúmplice ou outra explicação que o mundo e as pistas sustentem. O Mestre precisa estabelecer a verdade do arco e produzir pistas coerentes; não deve decidir arbitrariamente que o criminoso escapou ou reescrever acontecimentos já confirmados.

**Possível sequência alternativa:** nenhuma carta chega. A vida de Marrone muda silenciosamente por causa do que viveu. A história segue e o passado pode permanecer latente por anos.

**Premissa narrativa importante:** o Mestre pode narrar circunstâncias involuntárias plausíveis (um pesadelo, uma lembrança, indisposição), consequências sociais decorrentes de ações verificadas e decisões autônomas de NPCs. **Não pode decidir por conta própria escolhas conscientes do personagem controlado pelo jogador**, como ingerir comprimidos, iniciar um relacionamento, abandonar a família, confessar um crime ou cometer uma agressão. Quadros clínicos, dependência, separação conjugal ou alterações mecânicas duradouras devem ser construídos com suporte causal, escolhas apropriadas e regras do sistema — não impostos por um único sorteio aleatório.

**Contraexemplo proibido:** "Marrone ficou viciado e a esposa foi embora porque saíram 82 e 17 nos dados". Os dados geram uma **possibilidade/pressão narrativa**, cujo desenvolvimento depende de circunstâncias, acontecimentos e agência.

---

## 3. FRONTEIRAS ARQUITETURAIS OBRIGATÓRIAS

Separar entidades que não podem ser confundidas:

1. **AdventurePackage:** descrição/importação imutável do módulo; versão, sistema, mundo, fontes, história, segredos, locais, NPCs, encontros, adversários, objetivos, alternativas, finais e ganchos. Um JSON é dado, **não código executável**.
2. **AdventureInstance:** o que aconteceu ao jogar aquela instância: cenas, pistas obtidas, segredos revelados, escolhas, eventos, sucesso/fracasso, status e datas. Uma mesma aventura pode ser jogada mais de uma vez.
3. **CharacterIdentity & CharacterSheet:** identificador estável e ficha oficial; mesma identidade ao atravessar aventuras. O sistema de regras nativo continua sendo autoridade para perícias, condições, ferimentos, inventário, experiência e morte.
4. **CharacterBiography / Legacy:** histórico durável e verificável, independentemente da aventura ativa. Inclui conquistas, falhas, relações, marcas e observações privadas.
5. **CharacterLifeState:** estado biopsicossocial/biográfico atual: sono, hábitos, relacionamentos, trabalho, recuperação, estresse narrativo, tendências emocionais, desejos e perspectivas, com certeza/origem e distinção entre vivência, hipótese e diagnóstico mecânico autorizado.
6. **WorldTimeline / WorldState:** cronologia, geografia, clima, mudanças políticas, mortes, instituições, facções, NPCs, notícias e consequências persistentes.
7. **Knowledge & Rumor Graph:** quem sabe o quê, desde quando, como soube, nível de confiabilidade e distorções. Diferenciar verdade, boato, suspeita e desinformação.
8. **ConsequenceArc:** consequência narrativa potencial ou ativa, com origem causal, gatilhos, atraso, evolução, recuperação, cadência, responsáveis e situação atual.
9. **RandomMatrixPack:** pacote oculto e versionado de tabelas; dividido em matriz humana universal e complementos temáticos de cada VTT.
10. **RulesAdapter / NarrativeAdapter:** o primeiro aplica e valida regras; o segundo fornece contexto seguro ao Mestre Bárbara/Gemini/RAG e produz propostas, nunca fatos verificáveis sem confirmação.

**Regra de identidade:** o legado deve ser indexado por `systemId + worldId + timelineId + characterId`. `adventureId` é apenas proveniência, nunca a única chave. Personagem morto mantém impacto histórico no mundo; não continua jogável sem mecanismo legítimo do jogo. Campanhas distintas não se misturam por engano; nada de importar fama de Faerûn para Star Wars.

**Regra de segurança de dados:** evitar sobrescrita silenciosa e perda de saves. Backup e recuperação devem abranger, numa versão consistente, personagens + mundos + aventuras + arcos ativos + estados de aleatoriedade.

---

## 4. IMPORTAÇÃO JSON: EXPERIÊNCIA DO USUÁRIO

Em **todos** os VTTs:

- Menu **Aventuras**: `Nova aventura`, `Importar JSON`, `Aventuras importadas`, `Continuar`, `Pausar`, `Encerrar`, `Histórico` e opção de deixar **"Mestre criar a próxima aventura"**.
- Pré-visualização de importação: título, premissa sem spoilers, sistema, mundo, época, participantes sugeridos, área inicial, dependências, referências de regras, mapas/hexes, imagens/tokens e compatibilidade.
- Seleção dos **personagens existentes**; não exigir criação de novos. Preservar opções de controle dos companheiros já existentes no Braseiro.
- Validar arquivo, tamanho, campos, IDs, referências cruzadas, inimigos/itens e localização antes de aplicar. Segredos `gm_only` não aparecem ao jogador.
- O JSON não pode conter scripts executáveis, comandos de terminal, instruções ao agente, caminhos arbitrários ou "vitórias já concedidas". Referências externas não são acessadas automaticamente.
- Permitir mundo sandbox e campanhas com múltiplos caminhos: não forçar roteiro nem teletransportar heróis para cumprir etapas.
- Histórias geradas pelo Mestre usam **o mesmo modelo de AdventurePackage/Instance**, sem sistema paralelo de memória.
- Ficha: aba **Crônicas e Legado** com linha do tempo, acontecimentos, relações, reputação geográfica, consequências adormecidas/ativas, versão pública e notas privadas do Mestre.
- Interface técnica de inspeção apenas se habilitada: acesso ao histórico de sorteios e por que um evento foi elegível, sem estragar segredos ao jogador.

**Se o usuário deixar de importar:** o motor procedural assume criação de novas aventuras a partir do mundo, da localização, do clima, dos NPCs e de ganchos atuais ou inéditos. Ele não reinicia o personagem nem precisa usar um trauma antigo sempre.

---

## 5. PACOTE JSON: CONTRATO CONCEITUAL E EXEMPLO

**Contrato atual V1 de referência:** `braseiro.adventure/v1`, conforme `BRASEIRO_AVENTURAS_UNIVERSAL_V1.zip`, com `id`, `system`, `worldId`, `title`, `start`, `story`, `locations`, além de `npcs`, `enemies`, `objectives`, `encounters`, `secrets`. Para o projeto completo, criar evolução versionada **V2**, em vez de invalidar V1.

Novos grupos de dados recomendados em V2: `timeline`, `hooks`, `clues`, `conditionalEvents`, `possibleOutcomes`, `ruleBindings`, `worldConsequences`, `characterConsequenceSeeds`, `relationships`, `maps`, `assets`, `sources`, `contentWarnings`, `gmOnly` e `migrationVersion`. Campos descrevem **potenciais**; o jogo decide o que ocorreu.

Exemplo **didático**, não um schema final validado:

```json
{
  "schema": "braseiro.adventure/v2-proposal",
  "id": "gurps-investigacao-caso-marrone-01",
  "system": "gurps-investigacao",
  "worldId": "cidade-marrone",
  "title": "O Silêncio da Rua 17",
  "start": {"locationId": "delegacia-central"},
  "story": {
    "premise": "Um investigador persegue um criminoso violento.",
    "background": "Verdade conhecida apenas pelo Mestre.",
    "playerIntroduction": "Um novo desaparecimento mobiliza a delegacia."
  },
  "locations": [{"id": "delegacia-central", "name": "Delegacia Central"}],
  "secrets": [{"id": "verdade-01", "visibility": "gm_only", "truth": "Pista reservada ao Mestre."}],
  "objectives": [{"id": "capturar-suspeito", "description": "Identificar e capturar o culpado, se possível."}],
  "characterConsequenceSeeds": [{
    "id": "caso-marcante",
    "eventType": "exposure_to_violence",
    "possibleDomains": ["sono", "trabalho", "relacoes", "recuperacao"],
    "grantOutcomeAutomatically": false
  }]
}
```

A captura real, ferimentos, perdas, culpabilidade, testemunhas e desfecho serão **eventos gerados durante a partida**, não pressupostos pela importação.

---

## 6. MATRIZES OCULTAS DE VIDA E CONSEQUÊNCIAS

### 6.1. Dois níveis de tabelas

**Camada universal humana:** luto, alegria, memória, culpa, hábitos, sono, relações, confiança, isolamento, apoio social, objetivos pessoais, fama, fracasso, esperança, reconciliação, recuperação, envelhecimento, saúde e adaptação.

**Camada temática por sistema:** exemplos:

| VTT | Pacotes específicos sugeridos |
|---|---|
| GURPS Investigação | cenas de crime, culpa profissional, testemunhas, família, isolamento, paranoia situacional, corrupção, imprensa, sono, carreira, tratamento, dependência como risco contextual, adaptação e recuperação |
| GURPS demais cenários | módulo humano + ambientação e regras próprias, sem presumir temática policial |
| Forbidden Lands | fome, sede, frio, viagens, caça, superstição, clãs, medo, ferimentos, maldições, rivalidades, comunidade, fortalezas |
| D&D 5e | heroísmo, reputação, guildas, fé, pactos, traumas de guerra, rivalidades, corrupção, devoção, famílias, recompensas |
| Pathfinder 2e Kingmaker | lealdade, domínio/reino, guerras, soberania, terras, civis, facções, governantes, legado das decisões |
| Star Wars EotE/AoR | Dever/Obrigação, Imperial, Rebelião, facções, culpa de guerra, dívidas, perseguição, tripulação, naves, reputação por setor |
| O Um Anel 2e | Sombra, esperança, cansaço, saudade, fardos, jornada, coragem, patronos, companheirismo, corrupção e recuperação conforme regras |
| Cyberpunk RED | trauma urbano, implantes, reputação de rua, corporações, favores, laços, precariedade, dependências e recuperação conforme regras |
| Outros Braseiros | a mesma interface de dados, sempre com adaptador mecânico e pacote temático próprios |

### 6.2. Colunas/dimensões

A matriz real pode ter dezenas de grupos, não uma planilha única rígida. Grupos iniciais:

1. **Origem/causa** (perda, vitória, erro, violência, acolhimento, promessa, traição, acidente etc.).
2. **Memória evocada** (lembrança, sensação, sonho, associação, símbolo, evitação).
3. **Resposta emocional** (saudade, raiva, alívio, temor, alegria, orgulho, culpa, gratidão).
4. **Resposta comportamental possível** (cautela, isolamento, busca de ajuda, vigilância, dedicação, rotina).
5. **Sono/energia/saúde** (sono interrompido, fadiga, recuperação, cansaço; efeitos mecânicos apenas quando amparados por regras).
6. **Hábitos e riscos de dependência** (uso, abuso, procura de suporte, proteção social; não presumir vício inevitável).
7. **Família e intimidade** (apoio, conflitos, distanciamento, reconciliação, novas relações).
8. **Carreira/recursos** (promoções, dificuldades, exaustão, dívidas, afastamento, estabilidade).
9. **Vida social** (amizades, confiança, reputação, novas alianças, isolamento).
10. **Relação com NPCs** (lembranças, favores, cobranças, notícias, antigos conhecidos).
11. **Resposta do mundo** (rumores, medidas institucionais, recompensas, novas ameaças).
12. **Saúde mental/recuperação** (mudança de sintoma, tratamento, apoio, recaída, resiliência; sempre plausível e não determinista).
13. **Tempo e latência** (horas, dias, meses, anos, permanência, esquecimento parcial).
14. **Escala/intensidade** (microevento, nuance persistente, arco importante, evento raro).
15. **Grau de exposição** (privado, íntimos, instituição, comunidade, região).
16. **Continuidade** (evento único, recorrente, em remissão, resolvido, reativável).
17. **Modo de manifestação** (sensação, lembrança, mudança ambiental, intervenção NPC, carta, notícia, oportunidade).
18. **Mudança positiva** (fortalecimento, confiança, solidariedade, reconciliação, crescimento).
19. **Proteções/contrapesos** (rede de apoio, recursos, hábitos saudáveis, segurança, cuidados profissionais).
20. **Consequência secundária** (um efeito sobre outro, sem repetição artificial nem cascata infinita).

### 6.3. Dados e cruzamentos

- Cada tabela pode conter 100, 500, 1.000 ou mais entradas. `1d100` indexa uma faixa ou entrada; cinco lançamentos independentes (conceito de **5d100**) podem selecionar dimensões distintas.
- **Não somar 5d100** quando o objetivo for cinco seleções independentes. Documentar sem ambiguidade `d100(A)`, `d100(B)`, ... . Se o dado escolher coluna, fazer esse sorteio separadamente.
- Um `d100` pode apontar para subgrupos ponderados: a matriz não precisa ser limitada a apenas 100 resultados.
- Sorteio por pesos ajustados a causa, idade, gênero do cenário se relevante, histórico específico, papel do personagem, vínculos, lugar, período, ritmo narrativo, regras e riscos existentes. Evitar modelar estereótipos sociais como causalidade rígida.
- A combinação de cinco listas com 100 elementos teria até **10 bilhões** de tuplas brutas (`100^5`); **não** significa 10 bilhões de resultados coerentes ou realmente únicos.
- Invalidar combinações contraditórias; aplicar restrições temporais, causalidade, condições necessárias, exclusões e limites de repetição.
- Registrar `seed`, versão da matriz, dados brutos, filtros, pesos e decisão final para replay/auditoria sem quebrar a imersão.
- O Mestre não recebe a tabela inteira no prompt: recebe só a **proposta contextual aprovada** e memórias pertinentes. Isso economiza tokens e impede vazamento de segredos.

### 6.4. Frequência e vida natural

Não disparar um efeito importante a cada cena. Ter orçamento de eventos por sessão, dias de jogo, período de descanso e relevância narrativa. Alguns eventos são silenciosos; outros passam meses inativos. Repetição de pesadelos só quando apoiada por arco persistente e variação convincente. Garantir que existam longos intervalos de vida comum, oportunidades de felicidade e histórias inteiramente novas.

O mundo funciona **off-scene conforme passagem de tempo ficcional**, não exige servidor de IA gerando constantemente eventos no dispositivo. Em cada avanço de relógio, viagem, descanso, cena ou retorno ao jogo, processar transições e eventos vencidos de modo determinístico, sem criar acontecimentos retrospectivos arbitrários.

---

## 7. CICLO DE CONSEQUÊNCIAS — DO FATO À EVOLUÇÃO

**Estados recomendados:** `candidate -> eligible -> offered -> active -> dormant -> resurfaced -> resolving -> resolved`, além de `rejected`, `expired` e `superseded`. O fluxo não precisa ser linear; registrar transições e motivo.

1. **Fato:** motor de regras/host confirma captura, morte, derrota, confissão, perda, ferimento, fala ou ação relevante. Evento com ID estável, data e contexto.
2. **Análise causal:** comparar fato com histórico, vínculos, tensões prévias e condições do mundo; gerar possibilidades sem declará-las automaticamente verdadeiras.
3. **Elegibilidade:** verificar se categoria é permitida, plausível, compatível com tempo, frequência, preferências e regras, e se não conflita com cânone.
4. **Sorteio:** dados reais do host, pesos/tabelas versionados, `seed` auditável, rejeição de opções inválidas. Evitar loop de rerolls sem limite; fallback seguro.
5. **Proposta:** determinar se é consequência externa, involuntária, social, escolha do jogador ou alteração de ficha. Para escolhas conscientes, oferecer oportunidade de decisão em jogo.
6. **Compromisso:** gravar arco/resultado aplicável em transação junto com proveniência. Não registrar "é viciado" só porque surgiu uma proposta "risco de dependência".
7. **Agendamento ficcional:** estabelecer gatilhos, latência, frequência máxima, formas possíveis de ressurgimento e condições de resolução.
8. **Manifestação narrativa:** o Mestre dramatiza com naturalidade e sem exposição de tabelas, sem revelar segredos que o personagem não conhece.
9. **Reação e evolução:** escolhas, testes válidos, comportamento de NPCs e passagem do tempo alteram o arco.
10. **Encerramento/latência:** o arco pode desaparecer, tornar-se aprendizado, deixar sequela, ser reativado por gatilho forte ou nunca voltar.

### Autoridade da decisão

- **Host/regras:** resultados de teste, ferimentos, inventário, XP, consequências mecânicas, deslocamento, cronologia, prova dos fatos.
- **Mestre de IA:** interpreta contexto, propõe eventos e produz narração coerente; **não grava por si só vitórias ou diagnósticos**.
- **Jogador:** controla decisões e ações voluntárias de seu personagem. Não transformar propostas de comportamento em ações consumadas sem entrada do jogador.
- **NPCs e companheiros:** respeitar os modos existentes `EU CONTROLO`, `MESTRE CONTROLA`, `MESTRE COM CURADORIA` e seus limites; o mundo pode agir autonomamente dentro das regras do modo.

### Pseudocódigo-alvo

```text
onVerifiedWorldEvent(event):
    appendImmutableEvent(event)
    updateWorldAndCharacterFacts(event)
    indexNarrativeMemory(event)
    candidates = consequencePacks.findEligible(event, canon, characterLifeState)
    choices = weightedRollWithAudit(candidates, time, context, preferences)
    storeOnlyValidProposalsAndArcs(choices)

onWorldClockAdvanced(toGameTime):
    for arc in dueEligibleArcs(toGameTime):
        if passesNarrativeBudget(arc) and allPreconditionsHold(arc, canon):
            opportunity = arc.proposeManifestation()
            if opportunity.requiresPlayerChoice:
                presentSituationWithoutChoosingForPlayer(opportunity)
            else:
                commitAllowedWorldChangeOrPresentManifestation(opportunity)
            storeRollAndTransitionLog()

onNewAdventure(source = importedOrGenerated):
    party = loadCharacterIdentitiesAndSheets()
    world = loadWorldCanonAndClock()
    legacy = loadRelevantBiographiesAndOpenArcs()
    adventure = validateOrGenerateAdventurePackage(source)
    startInstanceWithoutResetting(party, world, legacy)
```

---

## 8. FATO, MEMÓRIA, CONHECIMENTO E RUMOR SÃO DIFERENTES

**Não confundir:**

- `Fact`: "Marrone capturou o criminoso em tal data", validado pelo host.
- `PrivateMemory`: "Marrone se lembra da sala", associado ao personagem; a representação subjetiva pode diferir do fato.
- `Belief`: "O investigador suspeita que havia um cúmplice" — pode ser falso.
- `Rumor`: "O assassino escapou da prisão" — pode circular sem ser verdade.
- `NPC_Knowledge`: "A jornalista ouviu o boato ontem de um guarda" — possui cadeia de transmissão.
- `GM_Secret`: "As cartas são feitas por um imitador" — reservado ao Mestre, até revelação válida.
- `Arc`: "Alguns vestígios da experiência podem reaparecer em certas circunstâncias" — não afirma um comportamento consumado.

Sempre registrar **quem sabe, por quê, com qual certeza, desde quando, em que lugar**. Propagar notícias respeitando viagem, comunicação, tecnologias e época do VTT. Não fazer todos os NPCs reconhecerem heróis ou conhecerem traumas particulares magicamente. Uma revelação falsa não pode alterar o fato histórico sem prova e evento corretivo explícito.

---

## 9. MODELO DE DADOS E PERSISTÊNCIA

**Modelo de tabelas/coleções sugerido** (ajustar às bases dos apps, sem destruir tabelas atuais):

- `adventure_packages(id, schema_version, system_id, world_id, content_hash, payload_ref, imported_at)`
- `adventure_instances(id, package_id, world_id, timeline_id, start_time, status, end_time)`
- `character_identities(character_id, system_id, world_id, timeline_id, canonical_name, status)`
- `legacy_events(event_id, world_id, timeline_id, game_time, event_type, payload_json, source_instance_id, verified_by, revision)`
- `event_participants(event_id, character_id, role)` — autor/participante/testemunha/vítima etc.
- `character_life_states(character_id, state_version, state_json, updated_at)`
- `consequence_arcs(arc_id, character_id, origin_event_id, pack_id, pack_version, status, next_due_game_time, state_json)`
- `character_relationships(relation_id, source_actor, target_actor, type, strength, provenance, updated_at)`
- `npc_knowledge(knowledge_id, npc_id, event_id, claim, certainty, learned_at, source_actor)`
- `regional_reputation(region_id, character_id, claim_id, strength, rumor_status, last_seen)`
- `world_consequences(consequence_id, origin_event_id, status, impact_json, time_scope)`
- `random_roll_audit(roll_id, arc_id, pack_id, pack_version, seed, dice_json, filters_json, result_json, game_time)`
- `narrative_checkpoints(checkpoint_id, world_id, timeline_id, snapshot_version, checksum, saved_at)`

Preferir **SQLite transacional** onde o aplicativo já o usa e **IndexedDB** nos VTTs baseados em WebView/JS, com migração versionada. Não depender só de memória do prompt, `localStorage` frágil ou `SharedPreferences` para todo o histórico. Eventos devem ser idempotentes: mesmo `event_id` não concede reputação/XP duas vezes. Proteger concorrência, interrupção de app, upgrade/downgrade e restauração.

**Histórico bruto e contexto IA são camadas diferentes:** guardar todos os fatos no banco. Construir resumos/RAG sob demanda por personagem, NPC, lugar, arco e tempo, sem apagar o registro original quando o resumo envelhece ou sai da janela de contexto.

### Exemplo de registro (conceitual)

```json
{
  "eventId": "evt-caso17-captura-001",
  "worldId": "cidade-marrone",
  "timelineId": "principal",
  "sourceAdventureInstanceId": "inst-caso17-01",
  "gameTime": "ano-2022-mes-09-dia-14",
  "type": "criminal_capture_confirmed",
  "subjectId": "serial-killer-01",
  "participants": [{"id": "detetive-marrone", "role": "captor"}],
  "visibility": "restricted",
  "verifiedBy": "rules-host",
  "evidenceIds": ["ev-combate-encerrado", "ev-prisao-registrada"]
}
```

### Exemplo de arco (conceitual)

```json
{
  "arcId": "arc-marrone-sono-01",
  "characterId": "detetive-marrone",
  "originEventIds": ["evt-caso17-captura-001"],
  "category": "sleep_disruption_possible",
  "status": "dormant",
  "nextDueGameTime": "ano-2025-mes-02-dia-01",
  "preconditions": ["character_alive", "plausible_trigger"],
  "cooldownDays": 21,
  "agencyClass": "involuntary_experience",
  "mechanicalEffect": "none_until_rules_validate",
  "packId": "gurps-investigacao-life",
  "packVersion": 1
}
```

Esses exemplos ilustram intenção, não substituem a validação de nomes reais, calendários e contratos do host.

---

## 10. MESTRE AUTOMÁTICO: CRIAR HISTÓRIAS SEM IMPORTAR MAIS NADA

O gerador procedural deve combinar três fontes, em proporção variável:

1. **Mundo atual:** regiões, facções, NPCs, calendário, economia, clima, rumores e conflitos ainda ativos.
2. **Legado atual:** vínculos, objetivos pessoais, reputações, arcos abertos e consequências que podem aparecer ou continuar latentes.
3. **Novidade independente:** novos crimes, locais, personagens, mistérios, tesouros, oportunidades e ameaças que **não** estejam ligados ao passado.

**Não forçar coincidência:** o serial killer não aparece em toda investigação; o jogador não deve sentir que o mundo inteiro gira em torno de seus traumas. Eventos do passado podem influenciar *como o herói vive a nova investigação*, não necessariamente *quem é o novo vilão*. Deve haver soluções, recuperação, vida cotidiana e mudanças positivas.

**Aventura emergente:** uma carta suspeita gera pista -> decisão do jogador -> verificação de origem -> novos vínculos e mudanças no mundo. Se for um imitador, estabelecer essa verdade no estado privado do Mestre e manter coerência com a prisão do criminoso original. Um final pode produzir outros arcos futuros, mas evitar cascatas sem limite.

**Narração:** seguir perfil universal Mestre Humano Dinâmico e Motor Bárbara: natural, sensorial e não mecânico; não dizer ao jogador "saiu 47 na tabela social". Preservar abertura longa quando apropriada, autonomia real e liberdade de reagir. TTS não deve ler tags, estado oculto ou instruções internas.

---

## 11. REGRAS E ADAPTADORES POR VTT

Todos compartilham esquema, persistência biográfica, sorteios auditáveis e interface, **não** regras mecânicas nem cronologias:

- **GURPS Investigação:** testes de perícias, vantagens/desvantagens, pontos, condições e impactos só pelo adaptador GURPS; experiências humanas tratadas com nuances e autonomia do jogador. Não inventar diagnóstico por sorteio.
- **D&D 5e Normal:** inventário, XP, condições, combate, fama, títulos, pactos e relações seguindo motor D&D. Preservar restrição existente: **Mystara congelado**; nenhum livro de aventura oficial adicionado ao core apenas por instalar o importador.
- **Forbidden Lands:** viagem, fome, clima, ferimentos, povoações, encontros, facções e hexes oficiais; não afetar mapas, cenas, regras e registros já homologados.
- **Pathfinder 2e Kingmaker:** história do herói mais mudanças no reino, assentamentos, território, exércitos, NPCs e capítulos; preservar início sandbox Restov e progressão Kingmaker.
- **Star Wars EotE + AoR:** mesmo motor nas duas modalidades, mas manter Obrigação vs Dever e regras respectivas, aliados/inimigos, naves, setores e facções.
- **O Um Anel 2e:** integrar ao `Adventure Engine` existente sem perder `CampaignState`, `adventureRuntime`, jornada, Sombra, Patronos, posição no Atlas e segredos GM-only.
- **Cyberpunk RED, GURPS outros cenários e demais Braseiros:** mesmo contrato, pacote temático e mecânico específicos e migrações próprias.

**Escopo da persistência:** continuidade dentro do mesmo jogo/mundo/linha temporal e do personagem original. Portar entre universos seria ferramenta futura separada de conversão, jamais automático.

---

## 12. CONTROLE, LIMITES NARRATIVOS E CONFIABILIDADE

- A IA **não** concede vitórias, mortes, itens, experiência, prisões, relacionamentos ou doenças só porque o texto as mencionou.
- O pacote JSON **não** é autoridade para alterar registros de personagens ou NPCs fora de sua instância. Segredos do pacote não vazam para contexto público.
- Sorteio aleatório **não** determina uma ação voluntária de personagem do jogador. Para decisões importantes, o Mestre constrói cena de escolha.
- Evitar estereótipos, punição narrativa repetitiva ou associação automática entre sofrimento e doença/vício. Incluir chances plausíveis de apoio e recuperação.
- O usuário pode configurar intensidade temática/limites, mantendo simulação profunda sem obrigar cenas especialmente sensíveis; mudanças nesses controles são salvas no dispositivo.
- Não mostrar/usar dados privados de um personagem ou NPC quando a perspectiva narrativa não os permitir.
- Notas manuais do jogador podem compor contexto, mas fatos de vitória/efeitos mecânicos requerem evidência do host.
- Por padrão, manter log de sorteios oculto na UI de jogo, disponível em diagnósticos de depuração sem quebrar surpresas narrativas.
- A passagem de meses/anos precisa respeitar tempo de jogo, não a data real do aparelho.

---

## 13. TESTES E GATES DE ACEITE — NÃO DECLARAR PRONTO ANTES DE PASSAR

### Importação e segurança

- JSON válido importado sem perda de saves; inválido rejeitado com erro compreensível; limites e referências cruzadas validados.
- Arquivo importado não executa comandos nem injeta ordens ao Gemini; segredos não aparecem no texto do jogador.
- Sistema/mundo/linha temporal incompatíveis bloqueados, com escolha explícita de migração quando suportada.

### Continuidade factual

- Aventura A importada -> combate/ação real validada -> memória criada -> encerrar -> aventura B importada -> personagem preservado -> evento antigo ainda existe.
- Aventura A -> finalizar -> não importar mais nada -> Mestre cria B -> história, relações e arcos seguem ativos.
- NPC da região conhece o herói se teve fonte; NPC distante não sabe magicamente; boato não equivale a verdade.
- O mesmo evento processado duas vezes não duplica XP, fama ou efeitos; histórico de personagem morto persiste no mundo.

### Vida persistente e tabelas

- Captura de assassino registrada; décadas depois o Mestre pode introduzir manifestação biográfica **sem** reabrir o caso.
- Também pode aparecer uma carta suspeita; prisão original permanece canônica até evento válido que a altere.
- Série fixa de `seed` + estado + versão da tabela reproduz mesmo sorteio; resultado, filtros e motivo registrados.
- Impedir escolhas incompatíveis, eventos repetitivos, cascatas infinitas, latência negativa e eventos futuros antes da passagem do tempo.
- Oferecer eventos positivos, neutros, resiliência e recuperação, não só penalidades.
- O jogador conserva ações voluntárias; alteração mecânica só quando autorizada pelas regras.
- Arco latente não consome orçamento narrativo e não reaparece toda sessão.

### Persistência e regressão

- Fechar/reabrir aplicativo **três vezes** em fases distintas; verificar mundo, biografia, ficha, aventuras, timeline e rolls.
- Backup/restauração íntegros e versionados; migrar saves antigos; evitar mudanças de IDs e perda de condições.
- Retomada de campanha, testes de combate, viagem/hex, NPCs, TTS, menus, configurações, imagens e tokens sem regressões.
- Verificar desempenho Android, especialmente **Galaxy A57**; Galaxy S23 Ultra quando aplicável. Sem travamento de UI nem texto TTS lendo tabelas.

### Gate de entrega

Só após teste unitário, integração real, migração, regressão, fluxo end-to-end e verificação física compilar e publicar APK final por pipeline autorizado (**BRELGA**, quando aplicável); conferir logs, checksum, assinatura e links de artefatos reais. Passar teste unitário **não** equivale a entrega de APK funcional.

---

## 14. BASE EXISTENTE CONFIRMADA NESTA SESSÃO

### 14.1. Pacote isolado V1 guardado anteriormente

Arquivo: **`BRASEIRO_AVENTURAS_UNIVERSAL_V1.zip`**. Conteúdo inspecionado em 20/10/2026:

- `src/legacy-engine.mjs` — núcleo determinístico JavaScript de referência;
- `docs/contrato-aventura.schema.json` — proposta de contrato V1;
- `docs/ORDEM_DE_INTEGRACAO_TODOS_VTTS.md` — integração e gates do núcleo V1;
- `examples/a-maldicao-de-karvath.json` — exemplo Garrick/dragão;
- `examples/demo-persistencia.mjs` — demonstração de continuidade;
- `tests/legacy-engine.test.mjs` — suíte.

**Resultado observado nesta sessão:** `node --test tests/legacy-engine.test.mjs` → **20 testes passaram, 0 falharam**. O README V1 apresenta "19 testes", informação desatualizada; a contagem real verificada foi 20. Testes dizem respeito ao **núcleo isolado**, não à integração com os jogos. O pacote V1 NÃO contém ainda o Motor Biográfico e as Matrizes Ocultas completas deste handoff.

### 14.2. Repositório pesquisado

GitHub conectado: `https://github.com/bra83/braseirobuild` (privado; existência confirmada nesta sessão). Há referências concretas ao Adventure Engine de O Um Anel no diretório `ci/oua-4.9.206/`:

- `templates/lib/adventureTypes.ts`
- `templates/lib/adventureState.ts`
- `templates/lib/adventureModules.ts`
- `apply-adventure-engine-4.9.206.mjs`

Esses arquivos mostram estados de aventura e progressão dentro da campanha, **não** certificam memória biográfica universal implementada. Outros projetos possuem estruturas e ramos próprios; **as últimas versões de cada VTT não foram certificadas aqui**. Antes de qualquer integração futura, levantar HEAD, APKs e mecanismo de persistência atual de cada produto para evitar regressões.

### 14.3. Não feito / limites

- Este handoff **não altera repositórios**, não faz commits/PR/merge, não mexe nas branches.
- **Nenhum APK foi alterado/compilado por causa desta solicitação.**
- Motor biográfico completo, tabelas temáticas grandes, importador visual real, adaptadores mecânicos de todos os sistemas, migrações SQLite/IndexedDB e cenários físicos ainda **NÃO IMPLEMENTADOS**.
- O V1 existente é uma **fundação reutilizável**, não versão final pronta de cada app.

---

## 15. ORDEM DE TRABALHO FUTURA, APENAS QUANDO AUTORIZADO

**Lote 0 — Auditoria sem regressão.** Identificar última versão real/branch e mecanismo de saves de cada Braseiro. Preservar builds existentes, configurações TTS e conteúdo homologado. Fazer backup e testes antes de mudanças.

**Lote 1 — Contrato compartilhado V2.** Evoluir schema sem quebrar V1, eventos canônicos, identidades, arcos, tabelas, estados, referências e migrações. Testar normalização e validação.

**Lote 2 — Matrizes ocultas e motor de vida.** Implementar pacotes universal + temáticos, regras de elegibilidade, `seed` reproduzível, pesos, cooldown, tempo ficcional, resolução, recuperação e log auditável.

**Lote 3 — Persistência verdadeira.** SQLite/IndexedDB conforme cada projeto, eventos atômicos, saves e restaurações, migração de campanhas, IDs estáveis e limites de armazenamento.

**Lote 4 — Integração do Mestre autônomo.** Separar fatos, crenças e segredos, contexto privado/público, geração de nova aventura, eventos biográficos, arcos, NPCs e mundo vivo. Evitar revelar tabelas/segredos na narração ou no TTS.

**Lote 5 — UI em cada VTT.** Importação JSON, preview, validação, gerenciamento de aventuras, modo aventura criada pelo Mestre e Crônicas e Legado na ficha.

**Lote 6 — Adaptadores e testes transversais.** Integrar individualmente cada jogo preservando regras, mapas, combate, companheiros, abertura, TTS e cronologia. Executar cenários Marrone, Garrick e casos positivos/negativos por sistema.

**Lote 7 — Homologação e entrega final.** Executar gates de regressão, device tests e somente então APKs finais pelo BRELGA, verificando resultados reais. Não apresentar build candidata como final sem evidências.

Na retomada, escolher blocos de implementação grandes e reutilizáveis, em vez de reproduzir manualmente centenas de casos no código da UI. As matrizes devem ser **dados versionados**, os cruzamentos devem ser resolvidos por **um algoritmo comum**, e cada VTT deve possuir **adaptador específico**.

---

## 16. PROMPT CURTO DE RETOMADA (PARA NOVO CHAT/CODEX/ANTIGRAVITY)

> Retome a especificação do arquivo `HANDOFF_BRASEIRO_AVENTURAS_MEMORIA_VIDA_PERSISTENTE_2026-10-20.md` e use `BRASEIRO_AVENTURAS_UNIVERSAL_V1.zip` apenas como ponto de partida isolado. O objetivo é implantar, em todos os VTTs Braseiro, importação JSON universal, continuidade factual entre aventuras, Mestre capaz de gerar novas aventuras sem importação, memória biográfica pessoal e mundial, consequências humanas dinâmicas e matrizes ocultas temáticas com dados auditáveis. Preserve a agência do jogador, a autoridade das regras e a integridade de saves e builds. Antes de escrever no GitHub, confirme escopo e autorização, descubra as versões atuais de cada jogo e defina gates verificáveis. Não declare conclusão sem integrar, testar e homologar cada aplicativo.

---

## 17. REQUISITOS QUE NÃO PODEM SER ESQUECIDOS

**A** — Importar aventura rica em JSON; **B** — aventuras futuras sem JSON, criadas pelo sistema; **C** — personagem mantém história e conquistas; **D** — impacto emocional, físico, comportamental, relacional e social plausível; **E** — passado pode retornar como nova trama ou apenas influenciar o cotidiano; **F** — matrizes ocultas extensas, temáticas, cruzadas e não repetitivas; **G** — d100/múltiplos d100 registrados; **H** — mundo, reputação e informação são localizados e persistentes; **I** — eventos respeitam passagem de anos; **J** — jogador mantém controle de decisões voluntárias; **K** — estados duradouros só por causas e regras válidas; **L** — incluir cura, apoio, superação e consequências positivas; **M** — segredos preservados; **N** — saves e personagens existentes não podem regredir; **O** — todos os VTTs, adaptados às regras de cada um; **P** — sem implementação até o usuário autorizar explicitamente.

**FIM DO HANDOFF — ARQUIVAR E MANTER A IMPLEMENTAÇÃO PAUSADA.**
