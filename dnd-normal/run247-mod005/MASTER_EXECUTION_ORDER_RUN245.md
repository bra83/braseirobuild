# EXECUÇÃO NO CODEX — BRASEIRO D&D 5e NORMAL — RUN245 G05

**DATA DO CHECKPOINT:** 2026-10-10
**PAPEL:** Engenheiro principal e orquestrador de até 4 subagentes de implementação/QA.
**MISSÃO:** Avançar e finalizar a versão NORMAL do VTT Android, com prioridade de fechamento total de G05; em seguida, atacar G06–G16 conforme dependências, SEM regressão e SEM declarar 100% por suposição. Produzir código real, testes, prova de proveniência e ZIP COMPLETO baixável no PC ANTES de esgotar a cota.

## 0. Identidade e URLs — ler antes de editar

- **Projeto correto:** BRASEIRO D&D 5e NORMAL, Android nativo em Java, `com.braseiro.dnd5e`, SQLite DB_SCHEMA=61, backup BACKUP_FORMAT=21.
- **ÚLTIMO CHECKPOINT / FONTE DE TRABALHO:** RUN245 G05 WIP, `VERSION_NAME=6.6.14-r245-g05-source`, `VERSION_CODE=606545`. Status: parcialmente implementado; **NÃO HOMOLOGADO** em Gradle/Android.
- **Pasta de checkpoint RUN245 destinada a este handoff:** https://drive.google.com/drive/folders/1FmHU3byadycibofjR40bxvd-5P3_tl7K (verifique conteúdo e leitura após acesso). Ali estão o manifesto, as partes ZIP, o CSV de lacunas e estas instruções. Se um item ainda não estiver acessível, declare a falha exata, não substitua pela base antiga.
- **Fonte canônica anterior RUN243 G08, para comparação de genealogia e recuperação:** https://drive.google.com/drive/folders/1UjpT_yWAMqmmKJKvSCYHl3ZnC9to_PjA
- **DND_SANDBOX_WORKING, auditorias históricas:** https://drive.google.com/drive/folders/1v_KCxY1KNhazW1pqJTHgUSfs1yyuH_wp
- **LIVROS/MATERIAIS D&D 5e:** https://drive.google.com/drive/folders/13wB-KfYCe2k5xGmb-z2D09KRDJtSyfHu
- **ORIGINAL HISTÓRICO CONGELADO — SOMENTE LEITURA:** https://drive.google.com/drive/folders/1ux8-EenjNGBlq5tcS44PM9_FPwCfd0SB
- **Repositório:** https://github.com/bra83/braseirobuild — main não é a fonte canônica automaticamente; avaliar branch/commit e nunca sobrescrever outros projetos. Criar branch de desenvolvimento EXCLUSIVA, ex. `braseiro-dnd5e-normal-run245-codex` (se disponível). NÃO usar a branch do híbrido.
- **Compilação Android/APK:** runner Windows self-hosted BRELGA do usuário. Identificar labels reais, permissões, workflow e assinatura antes de executar. Não supor que o runner está online ou que é permitido acessar chaves.

**PROIBIÇÃO ABSOLUTA:** este NÃO é o HYBRID_FASTPLAY, não é o ZIP genérico do AI Studio com `Hello Android`, não é a RUN238, e não é a main antiga. NÃO migrar para React/Kotlin/Compose/Flutter; NÃO importar ou mesclar `braseiro-dnd5e-hybrid-fastplay`.

## 1. Primeira ação: recuperar e selar o checkpoint

1. Conectar ao Drive de forma autorizada; listar a pasta RUN245. Não presumir OAuth/acesso/escrita por existir um link. Ler `RUN245_MANIFEST.json` e nomes completos de todas as partes. Se a fonte integral ZIP única existir, preferir ela; caso contrário, baixar `part00..part04` em ordem e concatenar bytes, sem extrair individualmente. Conferir SHA256 do ZIP integral **`7f69f02e5b77a100cfd517b80c4de96c10d0b33cf3284d78112ebd3755e0222b`**, tamanho **140317801 bytes**, e CRC do ZIP. O ZIP da RUN245 tem **1213 entradas** (contagem verificada no empacotamento; não confundir com total de outros checkpoints).
2. Inspecionar o ZIP e localizar `version.properties`, `settings.gradle.kts`, `gradlew.bat`, `app/src/main/java/com/braseiro/dnd5e/`, `app/src/main/assets/`, `app/src/test/...`, G05_EVIDENCE. O conteúdo do ZIP já é a RAIZ DO PROJETO ANDROID; não adicionar uma app genérica. Confirmar `VERSION_CODE=606545`, `DB_SCHEMA=61`, `BACKUP_FORMAT=21`, `com.braseiro.dnd5e`.
3. Abrir `README_G05_RUN245.md`, `G05_EVIDENCE/RUN245_MANIFEST.json`, `G05_EVIDENCE/RUN245_VALIDATION.log`, testes RUN244/RUN245 e `G05_ACTIONS_NEEDING_SOURCE_REVIEW.csv`.
4. Comparar com a RUN244 (pai) e a RUN243 (histórica) para capturar omissões/regressões, mas **NÃO aplicar patches antigos cegamente**. Criar manifesto de genealogia, hashes, baseline de testes, branch e commit inicial. Copiar a fonte para workspace de trabalho; Drive e original congelado ficam intactos.
5. Garantir que a assinatura histórica permaneça compatível. Nunca versionCode decrescente, nunca alterar package/applicationId ou schema só para compilar. Não inserir chaves de assinatura, tokens Gemini ou credenciais nos commits, relatórios públicos ou ZIP distribuído.

## 2. Estado real e trabalho já preservado

**RUN243:** Dijkstra por terreno difícil/rastreamento de rota e opportunity candidates; fonte integrada, mas Android/Robolectric não homologados.
**RUN244:** integração de empurrar/puxar quando a ação de ataque acertou e falhou a salvaguarda, com destino manual, preflight, transação e recibo; teste Robolectric escrito, não executado no Android.
**RUN245:** bloqueio de deslocamento forçado diagonal passando por cantos obstruídos; multiataque agora permite empurrar/puxar no **último** golpe elegível, com destinação manual e preflight; componentes intermediários permanecem fail-closed para não usar geometria obsoleta. Dois novos testes Robolectric escritos, não executados.
**Verificações locais relatadas em RUN245_VALIDATION:** harness Java de movimento forçado 13 PASS; economia de ações histórica 600893 verificações PASS; teste estático RUN244 9 PASS; RUN245 15 PASS; parse sintático cinco arquivos PASS. **Isso não substitui Gradle, Robolectric, instalação, assinatura ou E2E.**

## 3. Orquestração MULTIAGENTES obrigatória

Iniciar todos os agentes paralelos realmente disponíveis, idealmente 4. Se o runtime Codex não permitir subagentes reais, declarar isso e executar as frentes sequencialmente ou em worktrees isoladas, SEM fingir paralelismo. Nomear um **integrador único** responsável pelos conflitos em `AppDb.java`, `MainActivity.java`, assets/manifest/version.properties.

**FASE PRIORITÁRIA G05 — 4 frentes paralelas:**
- **Agente A — regras e efeitos dos monstros:** ler motores `MonsterActionEconomyGuard`, `MonsterEffectOnFailGuard`, `MonsterForcedMovementRules`, `MonsterConditionTimerLedger`, `MonsterConditionRecoveryLedger`, `DamageMitigationRules`, `MultiattackActionRules`, `OpportunityAttackRules` e afins. Implementar handlers tipados para efeitos especiais legitimamente descritos nas fontes, com CD/atributo/alvo/duração/dano reais; não quebrar economia de ação/reação/lendárias/covil/recarga.
- **Agente B — auditoria bibliográfica do bestiário:** ler os livros a que cada registro se refere, identificar edição e autoridade. Processar `G05_ACTIONS_NEEDING_SOURCE_REVIEW.csv` por lote grande, com uma linha por criatura/ação: origem, regra, metadado correto, executor e teste. Não inventar dados ausentes nem copiar extensos textos protegidos para o ZIP. Duplicatas/variações 2014/2024 não se misturam.
- **Agente C — integração/UI/persistência:** mapear ações de monstros da UI até `AppDb` e executor, garantir atomicidade `preview→validate→commit→receipt→SQLite→Backup21`, rollback integral, replay/idempotência, estado correto após reabertura; incorporar patches de A e B por contrato, não editar a mesma região em paralelo sem lock.
- **Agente D — testes/QA/CI:** executar ou preparar teste de cada família de ações, zerar falhas reais de compilação e Robolectric, documentar regressões, salvar logs e preparar workflow para Windows self-hosted BRELGA se autorizado; não pular/desabilitar testes nem tratar sucesso Java puro como homologação Android.

**Política de merges:** um branch/worktree por agente, merge pequeno e rastreável ao fim de cada bloco; conflitos em `AppDb` e `MainActivity` resolvidos apenas pelo integrador, com testes; nunca substituir um arquivo completo por versão mais antiga. Os agentes podem continuar nas outras áreas após fechar sua frente, mas devem evitar duplicação, conflitos e invenções.

## 4. Definição estrita de G05 concluído

Executar auditavelmente TODO o conteúdo mecânico de monstros dentro do escopo autorizado, não só as ações básicas. A auditoria RUN244 listou ~280 ocorrências de lacunas (sobreposição possível) e auditorias anteriores registraram 105 ações com rider sem CD, 53 ataques com CD sem habilidade e 27 save-actions com CD sem atributo. Essas contagens são ALVOS DE INVESTIGAÇÃO, não provas de que um campo ausente deva receber CD inventada. Fazer novo levantamento atual da RUN245.

Para cada ação elegível: fonte/edição → parser metadados → handler determinístico → UI ou NPC IA sob orçamento/autorização → recibo → commit atômico no DB61 → export/import Backup21 → testes positivos, negativos e de reentrada. Cobrir, conforme fonte: ataque normal, bônus, reação, gatilhos, multiataques e ordem, dano/resistência/imunidade/vulnerabilidade, riders e condições, DC/save, concentração quando aplicável, recarga, usos limitados, alcance, obstrução, empurrões e puxões, ações especiais/lendárias/de covil, suas contagens e reset por rodada, dano e efeitos de sucesso/falha. Uma habilidade sem regra autorizada deve permanecer BLOQUEADA com motivo auditável, não marcada como implementada.

**Casos regressivos mandatórios:** movimento diagonal fechado; destino ocupado; acerto/erro em cada componente do multiataque; último componente com rider e save falho; save bem-sucedido; custo excedido; rolamento manual, repeat/retry, scene/encounter ID obsoleto; import Backup21 após reload; interrupção de rede/IA não aplica HP ou condição parcialmente; sem ação fantasma ou duplicação de recurso. Reproduzir e corrigir falha histórica `Run236ConditionBackupRobolectricTest.legacyEncounterCannotBypassPendingRecovery`; confirmar a correção RUN241 dentro da RUN245.

G05 só recebe status **FECHADO** após código, cobertura G05 fonte→executor e prova de testes verificáveis. Caso falta de livro/licença/especificação, registrar EXCLUDED/NO_SOURCE com motivo e impacto, sem inventar 100%.

## 5. Em seguida, avançar o restante do VTT sem parar no G05

Após os gates críticos G05 avançarem, distribuir equipes paralelas por dependência:
- **G06:** magia 2014/2024, slots, componentes, alvos, alcance, duração, concentração, inventário, salvaguarda, efeitos e rollback; derivados de regras dos livros.
- **G07:** fichas, classes/subclasses, progressão 1–20, antecedentes, talentos, multiclasse, equipamento, persistência.
- **G08/G09:** tática avançada, elevação, linha de visão/cobertura, sentidos, hazards e **ponte UNIVERSAL UI→executor→mutação→recibo→SQLite**, inclusive ações narradas pelo Mestre; JSON descritivo não basta.
- **G10/G11:** modos EU CONTROLO, MESTRE CONTROLA e MESTRE COM CURADORIA, respostas tardias e curadoria; mundo vivo autônomo, NPCs fora de cena, relógio, clima e consequências persistentes.
- **G12/G13:** preservar e testar Gemini, perfil do Mestre, geração/importação de imagens, áudio e TTS Charon/Kokoro, sem regressões de interface ou perda de música entre cenas.
- **G14:** validar todas as seções Backup21, remapeamento, arquivo corrompido/alterado, SQLite round-trip, restauro atômico, saves legados.
- **G15/G16:** só após gates acima, compilar e testar no BRELGA e homologar upgrade/assinatura/saves em Galaxy S23 Ultra e A57 quando hardware realmente acessível.

O jogo precisa executar regras e mecânicas diretamente no código. **RAG ajuda a consultar fontes, NÃO substitui handlers de gameplay.** Mestre Gemini propõe/narra, mas não altera arbitrariamente HP, dado, posição, itens, slots, XP, hora, facção ou progresso sem validação/recibo.

## 6. Anti-regressão / proibições — ler por todos os agentes

- Nunca trocar o projeto pela branch híbrida, main antiga, RUN238, demo vazia, novo scaffold, UI genérica nem refazer do zero.
- Nunca eliminar código, assets, mapas, NPCs, sons, imagens, localizações PT-BR, fontes, testes ou saves apenas por parecer obsoleto. Só remover com diff, prova de não uso e regressão aprovada.
- Não mudar assinatura, `applicationId`, DB61, Backup21 ou IDs de estado sem migração/testes de compatibilidade.
- Não marcar PASS testes não executados, não editar fixtures para forçar sucesso, não comentar asserts, não reduzir conjuntos de teste, não desligar verificações de segurança, não ocultar pendências ou atribuir precisão fictícia a percentuais.
- Não misturar regras 2014 e 2024 sem seleção clara. Não inventar CDs, atributos, ações, estatísticas de monstros ou conteúdo dos PDFs inacessíveis.
- Não passar semanas discutindo bloqueios: registrar a falha externa e continuar todas as tarefas independentes. Sem solicitações de confirmação para decisões técnicas locais reversíveis e compatíveis; reportar somente dependências reais de autorização externa.
- Limitações de OAuth, Gradle, GitHub Actions, runner offline e cota são bloqueios de ferramenta, não prova de bug da aplicação; distinguir os dois.

## 7. BUILD e matriz de QA

1. Examinar scripts reais `gradlew.bat`, wrapper e plugin/Chaquopy; executar JDK e Gradle compatíveis, não `gradle` global 9.3.1 do AI Studio se incompatível.
2. Usar GitHub Actions BRELGA (`bra83/braseirobuild`) se houver autorização/conexão e label real. Criar/ajustar workflow limitado à branch normal, nunca à main de outros jogos.
3. Antes de APK: `compileDebugJavaWithJavac`, todas as suites de teste puro/Robolectric, suites por RUN, testes de persistência/retry/idempotência, validação de assets e PT-BR. Registrar comandos, tempos, logs e contagem.
4. Só assinar APK se a chave de upgrade legítima estiver acessível e a atualização preserve appId e saves. Caso contrário, produzir projeto compilável e status 'assinatura NÃO VERIFICADA'; não anunciar APK instalável por upgrade.
5. Testes em aparelho só contam com evidência real. Emulator não equivale a S23 Ultra/A57 físico; informe nível exato.

## 8. PROTOCOLO DE COTA E ENTREGA — OBRIGATÓRIO, NÃO DEIXAR PARA O FIM

**PRIMEIRO OBJETIVO:** na primeira etapa, reconstruir o ZIP RUN245, confirmar hash e acesso. Criar IMEDIATAMENTE uma cópia de trabalho versionada, `CHECKPOINT_STATUS.md`, `COVERAGE_MATRIX_G01_G16.csv` e `DELIVERY_MANIFEST.json`. Depois da primeira alteração coerente, publicar um primeiro snapshot de fonte integral e link; repetir ao término de cada bloco grande ou quando houver risco de limite de contexto/cota. Não espere atingir 95% da cota para iniciar empacotamento.

**A CADA CHECKPOINT:** salvar TODOS os arquivos necessários para abrir no Android Studio num único ZIP completo, inclusive gradle wrapper, código, testes, assets, manifests, configuração não secreta, README, evidências, relatório, `sha256sum`, diff desde RUN245. Não devolver apenas patch, hashes, uma pasta vazia, JSON ou repositório com arquivos grandes faltando. Não usar ZIPs aninhados confusos, sem informar o layout. Fazer `unzip -t` e ler ZIP de volta, checar versão, árvores e SHA256.

**LOCAL DE PUBLICAÇÃO:** preferir uma NOVA subpasta de entrega autorizada dentro de `DND_SANDBOX_WORKING` no Google Drive; preservar todas as fontes anteriores. Se o Drive não tiver escrita mas GitHub permitir, publicar release/asset completo no repositório (branch normal), não apenas artifact efêmero de Actions; se nenhum funcionar, indicar de modo verificável onde foi guardado e qual conexão é necessária. NÃO AFIRMAR upload concluído antes de readback. Não solicitar ao usuário baixar pastas e enviar arquivo por arquivo.

**LINK PARA BAIXAR NO PC:** entregar URL HTTP(S) real, navegável de **ZIP ÚNICO**, testada após upload ou acompanhada de verificação de metadados/readback. Se limites de upload só permitirem volumes, oferecer pasta com partes numeradas **mais** manifesto SHA256 e instruções de remontagem, explicando que não é um ZIP único; tentar também release/Drive single ZIP se possível. Não inventar URL nem usar `sandbox:/mnt/data` como se Codex pudesse acessá-la.

**ANTES DE ESBOÇO DE RESPOSTA OU INTERRUPÇÃO POR COTA:** finalizar bloco atual sem deixar código parcialmente editado, rodar as verificações possíveis, criar ZIP completo, publicar/linkar, produzir `HANDOFF_NEXT_AGENT.md` com estado exato do último commit, testes executados, falhas, próximos arquivos/funções, SHA/URLs. O link para baixar é prioridade, mesmo que G05 não tenha sido totalmente concluído. Não alegar que tem como medir cota restante se o runtime não expõe esse número; salve checkpoints desde cedo.

## 9. Critérios de resposta final do Codex

Devolver UM relatório objetivo com:
- Qual fonte RUN245 exata foi lida (hash), árvore/branch/commit;
- O que cada agente implementou (classes/métodos/arquivos), com mudanças reais e sem dupla contagem;
- Matriz G05 por habilidade/fonte/edição/executor/teste e contagem de recusas legítimas;
- Gate G01–G16 com `IMPLEMENTADO`, `TESTADO`, `HOMOLOGADO` separados;
- Testes executados x não executados, links dos logs e erros remanescentes;
- Informações do build BRELGA real, APK assinado/atualizável se e SOMENTE SE comprovado;
- **Link único para download no PC do ZIP integral mais recente**, SHA256 e tamanho; na ausência de ZIP único, explicar volumes e reconstrução;
- `HANDOFF_NEXT_AGENT.md` e link do checkpoint anterior preservado.

**REGRA FINAL:** COMEÇAR EXECUTANDO, NÃO RESPONDER APENAS COM PLANO. Usar os multiagentes reais disponíveis, conservar progresso, corrigir cada falha sem regredir, entregar máximo implementado COM ZIP integral publicado e verificável antes de qualquer limite de cota.
