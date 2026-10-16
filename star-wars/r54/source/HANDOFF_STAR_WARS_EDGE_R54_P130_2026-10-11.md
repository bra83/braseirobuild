# Braseiro Star Wars Edge of the Galaxy — R54 P130

**Baseline imutável:** `BRASEIRO_STAR_WARS_EDGE_R54_P129_SOURCE_CHECKPOINT.zip` (não usar snapshots P128/P113).  **Entrega:** `BRASEIRO_STAR_WARS_EDGE_R54_P130_SOURCE_CHECKPOINT.zip`.  **Android:** `versionCode=10130`, `versionName=1.0.130`, shell `data-version=1.0.130`.  **Status:** checkpoint de fonte, NÃO APK homologado.

## Código implementado neste bloco

1. **AoR Core — aquisição de talentos integrada ao jogo:** as 18 árvores do catálogo AoR já existente (6 carreiras × 3 especializações, 360 posições) agora são reconhecidas pelo motor de talentos usado pelo `GameController`, `BraseiroUiBridge` e armazenamento do Android. Compras verificam especialização realmente possuída, conexões no grafo, PE disponíveis e posição não adquirida; debitam PE e registram fonte, posição e compra em `talentPurchases`, sem mutar a entrada. Reiniciar e reabrir a campanha preserva as compras. A compra não concede outra especialização nem inventa acesso a outras árvores.
2. **Efeitos AoR de aplicação imediata:** as quatro famílias já suportadas no código compartilhado (`APLOMO`, `ENDURECIDO`, `RESISTENTE`, `DEDICACIÓN`) passam a ser aplicadas no personagem AoR. Dedicação exige escolha válida de característica, respeita o teto existente. Contagem de graus agora preserva talentos raciais e não duplica graus comprados quando os nomes traduzidos diferem de EotE.
3. **Cobertura honesta do catálogo:** todos os 360 espaços são adquiríveis; apenas 72 posições dessas quatro famílias têm efeito mecânico imediato implementado. `mechanicallyActive` não é mais anunciado como verdadeiro para as demais. Os outros talentos AoR ainda exigem implementação de seus efeitos próprios, não estão certificados como ativos.
4. **UAE / cinco aventuras:** verificação adicional de journal persistido antes de projetar estado e antes de aceitar transições dos adaptadores. Bloqueia IDs de evento de outra aventura, sequências regressivas/reordenadas, payload/timestamp malformados, objetivos contraditórios e consequências duplicadas. Preserva compatibilidade com journals válidos com lacunas de sequência. A validação protege os cinco adaptadores sem adicionar encontros inventados.
5. **Equipamentos:** mods falhados ocupam slots também durante a resolução da arma; um snapshot com mais sucessos do que slots restantes não produz bônus extras. O motor usa definições mecânicas do catálogo, não efeitos enviados no save.
6. **Bundle e Android:** bundle offline recompilado a partir dos módulos canônicos (**73 módulos**); dois builders byte-idênticos, versionamento Gradle/shell atualizado. Nenhum arquivo Java Kokoro/TTS foi modificado.

## Validação real

- `python3 tools/run-node-suite.py`: **1.297/1.297 PASS**, 248 arquivos, 6 stress, 0 falhas, 0 pulados. +15 testes sobre P129 (1282).
- `node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS** (testes automatizados, NÃO síntese real em aparelho).
- `node tools/verify-final-source.mjs`: **PASS**; 121 JS/ESM, 8 Java em syntax gate, 77 JSON, 2 XML; varredura de segredos sem achados, wrapper íntegro. Java não compilado contra Android SDK.
- `node tools/build-ui-bridge-bundle.mjs` e `node build-ui-bridge-bundle.mjs`: **PASS**, 73 módulos, saída idêntica.
- Logs: `validation/P130_NODE_SUITE.txt`, `validation/P130_TTS_SUITE.txt`, `validation/P130_SOURCE_GATE.txt`.

## Pendências que não devem ser declaradas concluídas

- AoR: efeitos ativos/passivos específicos das demais famílias de talentos; aquisição de especializações adicionais AoR ainda não auditada/integrada. Catálogo completo ≠ regras completas.
- EotE/AoR: demais expansões, livros e equipamentos pendentes de confronto com fonte primária; **não inventar regras**.
- Além da Orla: encontro de combate ainda não implementado, função stub permanece indisponível; não anunciar cobertura de combate.
- Homologação de APK e áudio Kokoro real no Galaxy S23 Ultra/A57: requer Android SDK/ADB e compilação no runner self-hosted BRELGA. **Não houve APK nesta execução**.

## Retomada segura

1. Usar exclusivamente o ZIP completo P130, verificar SHA-256 e manter P129 como fallback.
2. Reexecutar testes, gate de fonte e builder antes de modificar. Não sobrescrever bundle manualmente; regenerar com script.
3. Implementar famílias de efeitos AoR faltantes por referência mecânica auditada, com testes de combate/perícia/estado/persistência. Em seguida auditar expansões EotE/AoR e o encontro de Além da Orla com material oficial.
4. Compilar `./gradlew assembleDebug` no runner BRELGA, validar instalação, áudio e saves reais. Não equiparar sintaxe Java a build Android.
