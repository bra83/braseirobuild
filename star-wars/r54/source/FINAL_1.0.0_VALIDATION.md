# FINAL 1.0.0 — RELATÓRIO DE VALIDAÇÃO (histórico; estado vigente em R54 P19)

Data de origem: 2026-09-09

Atualizado: 2026-09-18

## Estado vigente R54 P19

- suíte regular atual: **783/783 PASS**;
- stress atual: **6/6 PASS**; total executado **789/789 PASS**;
- verificador atual: **103 JS/ESM, 5 Java, 40 JSON, 2 XML, 0 secrets, 0 APK/AAB**;
- catálogo atual ativo: **272 entradas pessoais + 25 veículos/naves**;
- build Android tentado com JDK/SDK/ADB disponíveis, mas bloqueado externamente por `kotlin-reflect:2.0.21` ausente e rede Gradle indisponível;
- os números dos blocos históricos abaixo permanecem apenas como histórico e não rebaixam o estado R54 P19.

## Resultado do código

- suíte completa final da origem histórica: **742/742 PASS**;
- stress de persistência/mundo: **6/6 PASS**, cada suíte cobrindo 33 sessões simuladas conforme o subsistema;
- baseline padrão: **Edge of the Empire (`EOTE_CORE`) + Obligation**, com modos **EOTE / AOR / MIXED** selecionáveis;
- Motor Barbara/bundle: **56 módulos**;
- RAG mecânico/políticas locais ativo: **54 entradas TESTADO**; somente `SW-AOR-CHAR-DUTY-002` e `SW-AOR-BASE-DUTY-CONTRIBUTION-001` são autoridades AoR oficiais recuperadas, e todo o restante continua guardado por perfil/proveniência;
- catálogo inicial histórico: **60 itens pessoais** (31 armas + 7 armaduras + 22 gear) e **25 veículos/naves** mecanicamente completos;
- contratos shell R46 ↔ Motor Barbara e Android ↔ JavaScript cobertos por testes;
- callbacks Java → JavaScript de Biblioteca, Gemini, Mestre Gemini e cache de imagens cobertos por testes;
- importação JSON de ficha/backup limitada a 16 MB antes de `file.text()`, com contrato do seletor Android protegido;
- scan de segredo privado/API key hardcoded: **0 ocorrências**;
- APK/AAB no pacote-fonte: **0**.

A validação reproduzível completa está em `tools/verify-final-source.mjs`; a continuidade canônica está em `RETOMADA_ATUAL.md`.

## Gate funcional FINAL

Confirmado por testes/inspeção estática:

- Android abre `file:///android_asset/vtt/r46-shell.html`;
- engine offline é carregado antes da shell React;
- primeiro boot inicializa explicitamente a campanha canônica antes da shell;
- shell preserva 31 estados e navegação R46;
- CENA usa situação, participantes e estado veicular provenientes do Motor Barbara;
- até 6 ações sugeridas são provenientes da proposta validada do Mestre;
- teste sugerido chama o resolvedor mecânico canônico e grava o resultado;
- Gemini usa o host Android e toda proposta passa por `requestMasterTurn` + validação antes de mutar estado;
- fallback local registra intenção/contexto sem inventar narrativa remota;
- TTS Android PT-BR, JSON, snapshots, PDF/RAG, biblioteca visual, cache de imagens e acessibilidade permanecem conectados;
- WebView bloqueia universal/file-from-file access e debugging de WebView está desativado no host final.

## Gradle / Android Studio

- Android Gradle Plugin: 8.13.2;
- Gradle: 8.13, fixado por URL e SHA-256 no launcher/propriedades;
- compileSdk/targetSdk: 36;
- minSdk: 26;
- Java: 17;
- app: versionCode 10000 / versionName 1.0.0.

`gradlew` e `gradlew.bat` são launchers determinísticos que delegam somente ao `gradle-wrapper.jar`; o wrapper lê `gradle-wrapper.properties`, baixa a distribuição quando necessário e valida o SHA-256 antes de executar.

## Limite de validação deste runtime

Este ambiente não possui Android SDK, ADB, emulador ou aparelho Android e também não possui acesso DNS de saída suficiente para baixar a distribuição Gradle. Por isso **não foi possível executar aqui `assemble`, instalar o app ou fazer smoke físico de WebView**. Isso é uma limitação do ambiente de validação, não uma funcionalidade deliberadamente ausente do código.

O gate externo restante para homologação física é: abrir o ZIP no Android Studio, sincronizar, compilar `app`, instalar e repetir o smoke em 415×915 no aparelho/emulador.

## Correção de empacotamento — Gradle Wrapper

Após a auditoria final de distribuição, o pacote passou a incluir também `gradle/wrapper/gradle-wrapper.jar`.
O JAR contém `org.gradle.wrapper.GradleWrapperMain`, lê `gradle-wrapper.properties`, usa a distribuição Gradle 8.13 fixada no projeto e valida o SHA-256 configurado antes de delegar a execução.
Os launchers `gradlew` e `gradlew.bat` delegam ao `GradleWrapperMain` empacotado, eliminando implementações paralelas de download/checksum.
Após as correções e guards posteriores, a suíte completa foi repetida: **638/638 PASS + 6/6 stress PASS**.


## Bloco 17 — compatibilidade Gemini Interactions / retomada limpa

- Schema estruturado do Mestre compatível com o subconjunto atual de JSON Schema da Interactions API; `maxLength` não é enviado ao provedor.
- Limites textuais e quantidade de ações continuam validados localmente pelo Motor Barbara.
- O verificador cria `validation/` automaticamente em reconstruções limpas antes de gravar reporter/relatório.
- Regressão atual: **638/638 PASS + 6/6 stress PASS**.


## Bloco 20 — Biblioteca PDF / promoção integral

- Corrigida corrida entre indexação em background e atualização de autoridade/ativação da fonte.
- Corrigida remoção concorrente durante indexação, impedindo recriação de documento fantasma.
- Regressão integral sobre reconstrução limpa: **542/542 PASS**.
- Stress canônico isolado: **6/6 PASS**.
- Verificador final: JS/ESM 54, Java 5, JSON 25, XML 2, secrets 0, APK/AAB 0, Gradle Wrapper PASS.
- Homologação Android física continua bloqueada apenas por ausência de SDK/ADB/sdkmanager neste host.


## Bloco 21 — cache de imagens / clear concorrente

- `clearCachedAsset()` invalida downloads já em andamento por geração de cache.
- Commit de imagem/metadata é curto e serializado; rede permanece fora do lock.
- Downloads invalidados não recriam o cache nem emitem erro tardio depois de `clear`.
- Regressão integral: **561/561 PASS**; stress canônico: **6/6 PASS**.


## Bloco 22 — imagem principal / clear concorrente

- `clearSceneImage()` invalida geração Gemini ou importação manual ainda em andamento.
- Commit da imagem, metadata e evento de sucesso são aceitos somente para a geração atual.
- Eventos de erro tardios de operações invalidadas são suprimidos.
- Regressão integral: **561/561 PASS**; stress canônico: **6/6 PASS**.


## Bloco 23 — leitura consistente de imagens nativas

- Leitura/status da imagem principal e do cache são serializados com os mesmos locks usados em commit/clear.
- Elimina janela de `500` transitório e metadata incoerente durante troca/remoção local.
- Regressão integral: **561/561 PASS**; stress canônico: **6/6 PASS**.


## Bloco 24 — lifecycle do TTS

- Callback tardio de inicialização não reativa TTS após shutdown.
- `speak` e `stop` revalidam lifecycle antes de operar o engine.
- Regressão integral: **561/561 PASS**; stress canônico: **6/6 PASS**.


## Bloco 25 — publicação do JSON no MediaStore

- Exportação Android Q+ só retorna sucesso após remoção efetiva de `IS_PENDING`.
- Falha de escrita/publicação remove o item pendente.
- Regressão integral: **561/561 PASS**; stress canônico: **6/6 PASS**.


## Bloco 27 — Core EotE real: especializações adicionais e raridade

- Restaurado o guard de cache do Bloco 21 ausente no ZIP de handoff; a divergência 557↔561 era empacotamento de teste, não regressão do motor.
- Implementada compra de especializações adicionais segundo o Core EotE: custo por total após compra, sobretaxa fora da carreira original, novas career skills e múltiplas árvores de talentos persistentes.
- Raridade/disponibilidade e mercado negro promovidos para autoridade EotE real; AoR continua excluído da autoridade ativa.
- Corpus RAG passou a 27 entradas auditadas; especialização adicional e rarity/black market deixam de falhar fechado.
- Regressão integral: **569/569 PASS**; stress canônico: **6/6 PASS**; verifier consolidado PASS.
- Próxima lacuna mecânica prioritária: Força, ainda fail-closed até revalidação da implementação local contra o Core EotE real.


## Bloco 28 — Força Core EotE + ponte R46

- `Force Sensitive Exile` implementado como especialização universal oficial, incluindo bloqueio para droides, Force Rating 1 na aquisição, árvore oficial de 20 nós e conexões auditadas visualmente.
- Dado da Força implementado com as 12 faces oficiais; resultados luminosos/sombrios, Destiny e Tensão seguem o Core.
- Efeitos contínuos comprometem dados da Força e reduzem o Force Rating disponível até liberação.
- Poderes Core `Sense`, `Influence` e `Move` implementados com compra, upgrades, custos, conexões e efeitos estruturados.
- Progressão/Força expostas pela bridge R46 sem redesenho: contrato passou de 24 para 37 métodos; bundle Barbara passou de 44 para 46 módulos.
- RAG/matriz passaram de 27 para 34 entradas auditadas; Força deixou de ser `fail-closed` somente após implementação e testes contra o PDF Core real.
- Regressão integral: **589/589 PASS**; stress canônico: **6/6 PASS**; verifier consolidado: JS/ESM 56, Java 5, JSON 26, XML 2, secrets 0, APK/AAB 0, Gradle Wrapper PASS.
- Android físico continua `BLOCKED_EXTERNAL`; próximo trabalho é seguir a matriz livro→regra→código→teste pelo Core e suplementos EotE disponíveis.


## BLOCO 33 — LIVING SANDBOX + CAMPANHA MISTA — PROMOVIDO (2026-09-13)
- EOTE/AOR/MIXED com Duty + Obligation simultâneos no modo MIXED e milestone de Contribution;
- sandbox determinístico: 6 facções, 36 NPCs persistentes, 12 locais, 6 clocks, 12 oportunidades, mercados e rotas;
- passagem de tempo avança planos/eventos fora da cena e gera oportunidades causais persistentes;
- abertura não força missão nem aventura publicada; estado factual fica separado da narrativa do Mestre;
- bridge/runtime R46 atualizados sem redesenho; rollback de snapshot preservado;
- regressão integral: **673/673 PASS, 0 falhas**; stress canônico: **6/6 PASS**;
- sintaxe JS 64 PASS; Java 5 PASS; JSON 25 PASS; XML 2 PASS; secrets 0; APK/AAB 0; Gradle Wrapper PASS;
- Android físico: BLOCKED_EXTERNAL.
- próximo: economia genérica/mercados/ownership/state e catálogo mecânico comprovado.

## BLOCO 34 — ECONOMIA GENÉRICA DO SANDBOX — PROMOVIDO (2026-09-14)

- Economia genérica integrada ao estado persistente para **armas, armaduras, gear e veículos**.
- Fluxos cobertos: **compra, recompensa, saque, presente/roubo, perda, dano, reparo e venda**.
- Veículos e demais bens mantêm separação entre `visual`, `profile`, `availability`, `ownership` e `state`; disponibilidade/raridade/preço ausentes falham fechado.
- Compatibilidade com o fluxo anterior de armas preservada; aquisição genérica usa `kind + itemId` e não inventa ficha mecânica.
- Motor Barbara regenerado para **56 módulos**; o guard antigo de 55 módulos foi atualizado somente após confirmar o grafo real.
- RAG/matriz: **49/49 TESTADO**; as novas entradas são políticas locais de sandbox/economia/campanha mista, não regras AoR promovidas sem fonte.
- Foco R34: **26/26 PASS**. Regressão completa atual: **703/703 PASS, 0 falhas**. Stress canônico: **6/6 PASS**.
- Verificador consolidado pós-reconstrução de fonte: **65 JS/ESM**, **5 Java**, **25 JSON**, **2 XML**, secrets **0**, APK/AAB **0**, Gradle Wrapper PASS.
- Shell **R46 415×915 permanece congelada e inalterada**.
- Android físico continua `BLOCKED_EXTERNAL` enquanto este host não dispuser de SDK/ADB/sdkmanager.
- Próximo bloco legítimo: **expandir catálogo mecânico de veículos/equipamentos somente por fonte real**, representando conteúdo repetitivo por dados e codificando apenas regras/exceções novas.



## BLOCO 35 — CATÁLOGO VEICULAR CORE EotE — PROMOVIDO (2026-09-14)

- `EotE Core Rulebook.pdf` foi usado como autoridade primária para o catálogo veicular deste bloco.
- Catálogo econômico Core EotE ampliado de **3 para 15 perfis mecanicamente completos**, com custo, raridade, Silhueta, Velocidade, Manejo, Defesa, Blindagem, limiares, sensores, carga/tripulação, hard points e armamento quando presentes na fonte.
- Perfis ativos Core: X-34, Digger Crawler, Personnel Carrier, AT-PT, AT-EST, CloakShape, JM-5000 JumpMaster, Firespray, Lambda T-4A, TIE/ln, BTL Y-Wing, Z-95-AF4, GAT-12h Skipray, Wayfarer e YT-1300.
- TIE/ln e Lambda T-4A passam a ter proveniência **EotE Core**; o X-Wing legado continua referência cruzada e **fail-closed na economia** até preço/raridade EotE comprovados.
- RAG/matriz: **50/50 TESTADO**. Motor Barbara permanece em **56 módulos**; nenhum código de regra foi criado para conteúdo que é apenas dado.
- Foco R35 do catálogo: **21/21 PASS**.
- Regressão histórica do bloco: **724/724 PASS, 0 falhas**. Stress canônico: **6/6 PASS**.
- Shell **R46 415×915 permanece congelada e inalterada**.
- Android físico continua `BLOCKED_EXTERNAL` enquanto este host não dispuser de SDK/ADB/sdkmanager.
- Próximo: ampliar equipamentos/veículos de suplementos EotE por proveniência real e dados estruturados; codificar somente exceções mecânicas novas.


## BLOCO 36 — FLY CASUAL / CATÁLOGO VEICULAR

- `Fly Casual` completou o catálogo inicial para **25 veículos/naves EotE** com preço, raridade/restrição e ficha mecânica comprovados.
- Regressão R36: **738/738 PASS + 6/6 stress**.

## BLOCO 37 — DUTY AOR GUARDADO / FECHAMENTO DE CATÁLOGO

- Duty de criação/reduções foi recuperado com autoridade `Star Wars - Age of Rebellion - Core Rulebook (SWA02)`, páginas impressas 46–50.
- Estrutura Base/Duty/Contribution foi recuperada como autoridade AoR separada.
- Apenas essas duas entradas AoR são permitidas como oficiais no corpus; o runtime sem página primária preservada permanece política local testada.
- Catálogo pessoal final: **60 itens**. O item final é `Appraiser's Eye` / Olho do Avaliador, `Fly Casual`, Table 2-4: Gear, 700 créditos, raridade 7.
- Catálogo veicular final: **25 perfis**.
- Motor Barbara: **56 módulos**. RAG: **54/54 TESTADO**.
- Regressão comum final: **736/736 PASS**; stress: **6/6 PASS**; total canônico: **742/742 PASS, 0 falhas**.
- Gates estáticos finais: **218 JS/ESM**, **5 Java**, **27 JSON**, **2 XML**, **0 secrets**, **0 APK/AAB**.
- Único gate não executado: compilação/instalação Android física, bloqueada externamente por ausência de SDK/ADB/sdkmanager.
