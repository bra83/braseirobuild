# Braseiro Star Wars VTT — R54 P129 — checkpoint integrado

**Base de origem:** `BRASEIRO_STAR_WARS_EDGE_R54_P128_SOURCE_CHECKPOINT.zip`. Esta versão mantém a árvore P128, incluindo cinco adaptadores, UAE v2, regras, assets, bundle offline e código Android. **Versão Android:** `versionCode=10129`, `versionName=1.0.129`; shell `data-version=1.0.129`. O projeto é para Android Studio, com Gradle wrapper e JDK 17 / Android SDK 36 na máquina de compilação.

## Implementações reais neste bloco

1. **UAE / migração de campanhas publicadas:** sincronização idempotente dos objetivos de episódios quando um save legado foi importado ou um avanço de cena ocorreu fora do journal UAE. Recupera somente marcos comprováveis por episódios já visitados; não inventa escolhas, testes, recompensas nem encontros. Saves legados encerrados/arquivados recuperam evento de conclusão, objetivo final e arquivamento uma única vez. Início de cada uma das cinco aventuras registra `NODE_ENTER` além de `ADAPTER_STARTED`.
2. **Reconciliação e persistência:** flags, contadores e sistemas do adaptador legado são autoridade sobre regras específicas; flags removidas não ressurgem após restauração. Projeção de objetivos funciona em save legado antes de qualquer nova ação, sem alterar o mundo original. Divergência externa de cena é auditada e os marcos são recuperados, sem duplicar eventos de início.
3. **Validação de checkpoints:** novo `validatePublishedAdventureRuntimeState` bloqueia IDs duplicados no journal, sequência regressiva, cena inexistente, histórico inconsistente, objetivos e relógios inválidos e divergência entre UAE e aventura publicada. Validação executada antes de persistir cada resultado integrado; falhas não modificam o mundo de entrada.
4. **Equipamentos / attachments:** a resolução de combate agora confere cada attachment persistido com o catálogo mecânico, compatibilidade com a arma, capacidade de hard points (quando declarada) e identidade única do ativo. Mods e efeitos de attachments incompatíveis, duplicados ou que excedam capacidade não são aplicados; armas legadas sem capacidade declarada preservam attachments compatíveis. A mesma seleção autorizada vale para efeitos base e mods.
5. **Capacidade real das cinco aventuras:** `Beyond the Rim` não anuncia mais encontro de combate implementado apenas porque existe uma função stub que lança exceção. Checks, perseguições e conclusão seguem funcionando. As outras quatro aventuras mantêm suas capacidades reais de combate.
6. **Bundle offline e versão:** bundle recompilado a partir das fontes `.mjs` canônicas, **71 módulos**; builders raiz e `tools/` byte-idênticos. Código nativo Kokoro/TTS não foi alterado.

## Validação executada nesta rodada

- `python3 tools/run-node-suite.py`: **1.282/1.282 PASS**, 247 arquivos (241 regulares + 6 stress de 33 sessões), 0 falhas e 0 pulados. P128: 1.264; **18 testes P129 novos**.
- `node tools/verify-final-source.mjs`: **PASS** (121 arquivos JS/ESM, 8 Java em syntax gate, 77 JSON, 2 XML, 0 segredos, wrapper Gradle íntegro). A validação Java não resolve dependências Android sem SDK.
- TTS/Kokoro: **27/27 PASS** em testes Node específicos; **não é teste real de voz em aparelho**.
- `node build-ui-bridge-bundle.mjs` e `node tools/build-ui-bridge-bundle.mjs`: **PASS**, 71 módulos. `node --check` do bundle: **PASS**.
- Checkpoint ZIP P129: SHA-256 e integridade do ZIP verificados no empacotamento.

## Limites reais / próximas prioridades

- O UAE continua como journal persistente sincronizado; os adaptadores legados ainda são executores das regras específicas de cada aventura. Não alegar UAE como único motor de regras.
- `beyond-the-rim` não tem `encounterSpec` de combate ativo; a API lança erro. Não declarar cobertura de combate dessa aventura. Integrar encontros somente com fonte de regras conferida.
- As 16 famílias de expansões EotE/AoR ainda RAG/declaração não foram implementadas neste bloco, pois não houve auditoria das regras primárias. Equipamentos seguem parciais, embora a execução de attachments esteja mais protegida.
- **APK não compilado aqui:** Android SDK/adb/sdkmanager ausentes. Compilar com `./gradlew assembleDebug` no GitHub runner self-hosted BRELGA ou Android Studio; homologar no Galaxy S23 Ultra/A57, incluindo Kokoro real e persistência longa.

## Retomada sem regressão

1. Usar exclusivamente o ZIP completo P129 como baseline; não reverter para P128/P113. Validar SHA-256.
2. Executar `python3 tools/run-node-suite.py` e `node tools/verify-final-source.mjs` antes de alterar a árvore.
3. Conferir livros primários de expansões EotE/AoR e as cinco aventuras, implementar regras por famílias inteiras, com testes de estado, UI, persistência e combate. Não inferir cobertura a partir de assinaturas de funções.
4. Compilar no BRELGA; não confundir syntax gate Java com APK Android validado.
