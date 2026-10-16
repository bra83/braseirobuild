# BRASEIRO STAR WARS — R54 / P170 — CHECKPOINT INTEGRADO

**Origem obrigatória:** `BRASEIRO_STAR_WARS_EDGE_R54_P169_SOURCE_CHECKPOINT.zip` (P169), com SHA-256 validado antes da extração. **Destino:** P170 / Android `1.0.170` / `versionCode=10170`.

## Alterações efetivamente implementadas

1. **AoR — oito posições de talentos ativadas no catálogo (314 → 322/360):** `TENACIDAD` (2), `TENACIDAD MEJORADA` (2), `GUARDAESPALDAS` (2) e `MEJORAS CASERAS` (2). A contagem representa *consumidores mecânicos presentes*, não uma certificação integral contra todos os livros.
2. **Tenacidade / aprimorada:** ação com rolagem real de Disciplina, dificuldade por graduação, remoção de Atordoado/Desorientado após sucesso; variante aprimorada permite recuperação da incapacitação por Tensão no turno de combate. Validação de fonte comprada, limites e rollback.
3. **Guarda-costas:** manobra, Tensão voluntária, proteção persistente de companheiro em alcance de Interação confirmado, melhorias na dificuldade de ataque recebido, expiração no próximo turno do jogador; ataque contra companheiro com rolagem real e dano transacional, acionado explicitamente pelo Mestre.
4. **Melhorias caseiras:** alocação permanente de cada graduação adquirida a uma arma pessoal específica; bônus real de +1 de dano ou -1 no custo de Crítico (mínimo 1) durante a resolução do ataque. Validação de posse, IDs únicos, proibição de empilhamento na mesma arma, rejeição de cópia fraudulenta do identificador e persistência.
5. **UAE / cinco aventuras:** condições explicitamente inválidas (`false`, `0`) não liberam gatilhos/ações por truthiness; metadados reservados de consequência não podem ser forjados. Novos testes atravessam os cinco módulos publicados com objetivos, gatilhos, consequências, relógios, rollback, reaberturas e idempotência.
6. **Android PT-BR:** bridge com `aorP170Status`, `useHardHeaded`, `useBodyGuard`, `resolveIncomingCompanionAttack`, `assignJuryRigged`; painéis contextuais nas telas 02 (combate) e 08 (equipamentos); shell/bundle reconstruídos para 1.0.170.
7. **Kokoro/TTS:** oito arquivos Java idênticos aos de P169 por SHA-256; nenhuma mudança no pipeline nativo.

## Evidências e testes

- Node.js regular: **2.413/2.413** em 288 arquivos (inclui 26 testes específicos P170).
- Stress: **6/6**.
- Kokoro/TTS: **27/27**; gate de sintaxe Java: **8 arquivos PASS**.
- Auditoria de cinco aventuras P148: **EXIT_CODE=0**.
- Gradle `:app:assembleDebug --offline`: **bloqueado** — `java.net.UnknownHostException: downloads.gradle.org` (distribuição Gradle 8.13 não disponível no host).
- Verificador global `tools/verify-final-source.mjs`: tentativa encerrada por timeout; **NÃO** certificado. O suite regular/stress foi executado separadamente e passou.
- Provas em `evidence/P170/`: `NODE_REGULAR.txt`, `NODE_STRESS.txt`, `KOKORO_TTS_REGRESSION.txt`, `JAVA_HASH_COMPARE.txt`, `JAVA_SYNTAX.txt`, `FIVE_ADVENTURES_AUDIT.txt`, `ANDROID_GRADLE_ATTEMPT.txt`.

## Pendências reais e limites da entrega

- **38 posições AoR** ainda sem consumidores; lista completa: `evidence/P170/AOR_PENDING_38_POSITIONS.json`.
- **Melhorias caseiras:** opções adicionais de armadura, carga e custo de qualidades ainda não integradas. As duas posições foram ativadas com as opções de dano/crítico; **cobertura semântica total ainda não certificada**.
- **Tenacidade aprimorada:** o fluxo ativo permite recuperação no turno do jogador, mas o gate estrito de "apenas no turno seguinte à incapacitação" e o encerramento automático de encontros após incapacitação por Tensão precisam de teste e ajuste adicional.
- **Guarda-costas:** ataque ao companheiro funciona como ação explícita do Mestre; a IA de adversários ainda não escolhe companheiros automaticamente.
- **Cinco aventuras:** integração mecânica de testes passou; **comparação integral capítulo-a-capítulo com os PDFs não realizada**.
- **Expansões EotE/AoR:** auditoria integral contra PDFs não concluída; não afirmar 100%.
- **APK Android / áudio físico / três reaberturas em aparelho:** não homologados neste ambiente.

## Continuidade sem regressão

1. Começar pelo ZIP **P170 completo**, nunca por uma cópia anterior. Conferir SHA-256 antes de editar.
2. Finalizar semântica de `MEJORAS CASERAS`, `TENACIDAD MEJORADA` e `GUARDAESPALDAS` no fluxo de NPC automático, com testes reais de combate, persistência e falha de gravação.
3. Implementar os 38 slots restantes a partir do JSON, sem liberar nomes sem consumidores; priorizar grupos funcionais completos e confirmar os efeitos nas fontes.
4. Conferir as cinco aventuras contra PDFs oficiais e registrar cobertura objetiva, gatilho, consequência e transição por cena; não usar apenas testes sintéticos como certificação editorial.
5. Compilar no GitHub self-hosted runner **BRELGA** ou ambiente com Gradle 8.13 e SDK 36; homologar instalação, três reaberturas e áudio Kokoro real. Não alterar TTS sem regressão física.
6. Preservar o bundle offline gerado de `.mjs` canônicos com `node tools/build-ui-bridge-bundle.mjs` e executar `python tools/run-node-suite.py --regular`, `--stress`, TTS e Java gate antes de nova promoção.
