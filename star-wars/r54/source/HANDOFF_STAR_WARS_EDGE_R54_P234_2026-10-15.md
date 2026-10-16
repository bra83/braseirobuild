# HANDOFF CANÔNICO — BRASEIRO STAR WARS R54 P234

**Continuidade real e direta P233→P234. NÃO recomeçar e NÃO adicionar aventuras.**

## Escopo definitivo: cinco aventuras e jogo completo

1. `under-black-sun` SWEP01 — Sob um Sol Negro.
2. `beyond-the-rim` SWE05 — Além da Orla.
3. `long-arm-of-the-hutt` SWE02 — O Longo Braço do Hutt.
4. `jewel-of-yavin` SWE09 — A Joia de Yavin.
5. `mask-of-the-pirate-queen` SWE13 — A Máscara da Rainha Pirata.

Nenhuma sexta aventura no registro ou no RAG ativo do Mestre. Preservar absolutamente regras EotE/AoR e expansões, Motor Bárbara, mundo vivo, companions, fichas, TTS PT-BR/Kokoro, saves, veículos, UI Android e perfis existentes. Não excluir nenhum arquivo legado ou PDF armazenado pelo usuário.

## Implementação real P234

- `adventure/hybridFiveAdventurePolicy.mjs`: cinco IDs congelados; validação da registry; identificação dos documentos oficiais por títulos/códigos SWEP01/SWE05/SWE02/SWE09/SWE13; filtro contra outras aventuras e decisões tipadas de autoridade por finalidade. Não atribuir mecânica a texto RAG/LLM.
- `adventure/publishedAdventureRegistry.mjs`: valida exatamente cinco adaptadores no boot do módulo.
- `library/nativeLibrary.mjs` e `rag/sourceSearch.mjs`: fontes de aventuras extras são excluídas tanto na busca Android quanto na fusão local; BASE/ERRATA/SUPLEMENTO continuam ativos.
- `master/context.mjs`: contexto inclui escopo e contrato híbrido; detecção de narrador HTTP ou Gemini nativo configurado; `contextDigest` registra versão do contrato.
- `master/provider.mjs`: instrução HTTP/Gemini preserva somente cinco aventuras, proíbe regra inventada, usa motores reais para rolagens/efeitos e respeita segredos de cenas.
- `core/uiBridge.mjs`: regra inventada pelo Gemini não é silenciosamente excluída do `sourceRuleIds`; a proposta é rejeitada. Falha/rejeição de Gemini usa fallback local com registro somente da intenção, sem consequências mecânicas ou narrativa falsamente atribuída a Gemini.
- Offline WebView bundle **120 módulos**, refeito pelo builder canônico. `app/build.gradle.kts`: versão fonte **1.0.234**, `versionCode 10234`; diagnóstico/UI sincronizados. **Oito Java/Kokoro byte-idênticos a P233**.

## Auditoria de cobertura (inventário, NÃO certificação editorial)

- `validation/P234_HYBRID_COVERAGE_LEDGER.json`: **cinco módulos, 100 nós alcançáveis, 141 checks declarados, zero nós inalcançáveis**, 3 episódios declarados por módulo.
- Corpus RAG atual: **54 registros `TESTADO`**. Isso NÃO significa que todo o universo EotE/AoR/expansões esteja certificado. Nenhuma certificação integral contra PDFs foi realizada neste checkpoint.
- `P234_HYBRID_SCOPE_AND_CLOSURE.md`: contrato completo de autoridade, rollback, consulta, fallback e critérios de fechamento.

## Testes reais P234

- **353 arquivos regulares: 3.218/3.218 PASS**.
- **6 arquivos stress individuais: 6/6 PASS**.
- **TOTAL 3.224/3.224 PASS em 359 arquivos individuais**, zero skipped, zero falhas. Não foi usada suíte monolítica.
- Oito testes específicos em `tests/uae-p234-five-adventures-hybrid-safety.test.mjs`: allowlist, aliases das cinco aventuras, filtro RAG Android/mesclado, roteamento, contexto e prompt, modo offline, Gemini adulterando fonte→fallback seguro, bundle offline.
- **Sintaxe 540 MJS + 2 JS runtime + 22 JSON + 2 XML PASS**; 8 Java/Kokoro idênticos a P233. Bundle 120 módulos.
- Logs: `evidence/P234/{TEST_SUMMARY.json,PER_FILE_RESULTS.json,SYNTAX_AND_JAVA.json,test-results/}`.

## PENDÊNCIAS OBRIGATÓRIAS — não declarar VTT pronto

**O usuário não deseja jogar uma versão incompleta e exige todas as regras/mecânicas das cinco aventuras com arquitetura híbrida.**

1. Gerar matriz oficial PDF→cena→escolha→teste→ficha/rolagem→consequência→recibo→persistência para os cinco módulos; verificar tudo, incluindo New Meen/Geonosis/Teemo, Arend/Grayson, Holo-Park e recompensas, sem inventar estatísticas ausentes.
2. Completar e certificar regras EotE, AoR e expansões por famílias; cada efeito crítico com executor determinístico, RAG apenas para texto/fonte, Gemini só para interpretação/narração supervisionadas.
3. Testar campanhas inteiras reais dos cinco módulos e todos os resultados, salvamentos, três reaberturas, reloads/offline, mundo vivo, NPCs, companions, TTS Kokoro e contexto Gemini.
4. **Só depois** executar Gradle, gerar APK e homologar Android físico Galaxy A57/S23 Ultra; não antecipar estas etapas.

**Atenção:** não há garantia absoluta de ausência de falha em dispositivo, rede ou modelo. Arquitetura deve falhar de forma contida (sem corrupção, regra inventada nem consequência antecipada). Nunca declarar 100% sem evidência.

Sem APK, SDK/Gradle, CI BRELGA, testes de dispositivo ou push GitHub P234. Biblioteca cheia: **não houve upload nem readback P234**. Preservar P233 e todos os checkpoints anteriores, sem apagar sem autorização. Trabalhar em lotes GRANDES de famílias de regras e cenas de aventura, e não um botão/micro-regra por execução horária. Registrar provas individualizadas e avançar no fechamento completo.
