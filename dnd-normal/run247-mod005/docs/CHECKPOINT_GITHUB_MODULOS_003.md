# BRASEIRO UNIVERSAL — GH MÓDULOS 003 — 2026-10-10

## Fonte canônica desta rodada
- **Projeto correto**: Braseiro D&D 5e NORMAL (Java Android), da linhagem RUN245, sobreposto por Manual 001 e GitHub Módulos 002.
- **Artefato**: `BRASEIRO_DND_NORMAL_RUN245_GITHUB_MODULOS_003_FULL_SOURCE.zip` — código completo, não só patch.
- **Escopo desta rodada**: primeira MESA jogável via Gemini, conectando os módulos GitHub já adaptados ao journal original do Braseiro.
- **Sem rebuild, sem troca de linguagem, sem mexer em HYBRID_FASTPLAY, G05 ou APK.**

## Implementação real (não apenas arquitetura)
- `UniversalAiProposalPort.java` (novo): valida respostas JSON do Gemini, recusa dados/limites inválidos, monta `begin_check` sem confiar em efeitos narrados.
- `UniversalLiveSession.java` (novo): módulo de campanha persistente neutro, `MODULE_ID=braseiro-universal-live-v1`, `PARTY_ID=909041`, prompt multissistema com ficha, últimas cenas e recibos já confirmados.
- `UniversalRpgEngine.java` (alterado): comando `accept_ai_scene` faz **um** commit lógico de narração + teste pendente, identifica resposta por `event_id`, recusa duplicatas, respeita bloqueio de teste pendente e armazena até 30 cenas na mesma `AdventureState.universal_rpg_state`.
- `MainActivity.java` (alterado): botão `MESA UNIVERSAL · GITHUB · JOGAR` na tela `Aventura`; criação de ficha `Tome`, seleção de perfil; narração por Gemini usando chave existente; exibição de rolagem e confirmação por resultado manual ou dados JSON; histórico/recursos consultáveis; grava no `AdventureEngine.processUniversal`/SQLite. Não altera a campanha D&D normal (usa ID e party separados).
- `UniversalGithubModulesIntegrationTest.java` (alterado): 3 novos testes de regressão para conexão Gemini simulado -> Tome -> Year Zero / SWRPG -> Loreweaver -> journal. Total 9 testes do contrato.

## Evidência de QA
- `tools/github_qa/run_java_contract_tests.sh`: **9/9 testes da lógica PASS**, 2/2 execuções do laboratório anterior PASS; usam `org.json` mock e persistência simulada. Não é teste real de Android.
- Parser oficial JDK 21 (`JavacTask.parse`) dos 5 Java editados: **0 erros de sintaxe**.
- ZIP final: verificar SHA256 + CRC no sidecar da entrega.

## Limites explícitos
- Chamada real à API Gemini ainda NÃO foi efetuada com chave do usuário; não certificar latência/resposta/erro de rede.
- Build Gradle, testes Robolectric, SQLite no aparelho, Audio/TTS e interface visual ainda NÃO foram executados nesta rodada.
- A UI não suporta ainda alternar entre múltiplas fichas simultâneas por perfil; há um slot persistido por perfil (`live-<id>`); pode cadastrar fichas com outros IDs por API. O formato JSON de atributos exige que o usuário informe valores válidos; não geramos ficha inventada.
- `StarWarsNarrativeDicePort` e `YearZeroDicePort` são subconjuntos portados e testados, não a implementação total dos respectivos RPGs.
- Tombamento/criação de projeto desde zero, cópia de conteúdo protegido e troca de versão de regras permanecem proibidos.

## Próxima intervenção técnica sem recomeçar
1. Teste instrumental de `AdventureEngine.processUniversal` com o `AdventurePersistenceAdapter` e `AppDb` SQLite Android, validando duas aberturas do mesmo módulo/party e rollback de falha de escrita.
2. Revisão de tela real no aparelho: criação de personagem, Gemini JSON, escolha de resultado e próxima cena.
3. Transferir a mesa universal para seleção de personagem existente, em vez de apenas criar um perfil inicial.
4. Integrar operações tipadas de efeitos após confirmação, com inventário/recursos/relógio no journal e migração Backup21. Nenhum resultado da IA deve aplicar efeitos sozinho.
5. Avaliar integração gráfico dos algoritmos de PlanarAlly somente após verificar compatibilidade com MapBoardView/Android.

## Dependências externas reaproveitadas
Tome (MIT), Loreweaver (MIT), AetherTable (MIT), Year Zero Roller (MIT) e SWRPG Dice (MIT), vide `docs/THIRD_PARTY_NOTICES.md` e `docs/AUDITORIA_REPOSITORIOS_GITHUB.md` (revisão anterior). Os seus serviços completos não são automaticamente importados nem afirmados como implementados.
