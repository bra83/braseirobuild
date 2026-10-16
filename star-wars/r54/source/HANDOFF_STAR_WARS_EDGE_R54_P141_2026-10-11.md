# Braseiro Star Wars VTT — R54 P141 (11/10/2026)

## Baseline e versão

Fonte canônica de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P140_SOURCE_CHECKPOINT.zip` (não reconstruir a partir de versões antigas). Nova versão: Android `versionCode=10141`, `versionName=1.0.141`, shell offline `1.0.141`. `uiBridge.bundle.js` recompilado a partir dos `.mjs`, **77 módulos**. O projeto completo está no ZIP P141.

## Implementação real nesta rodada

1. **Universal Adventure Engine — auditoria causal:** novo `adventure/uaeJournalCausality.mjs` integra-se ao gate persistente `adventureCheckpointGate.mjs`. Verifica eventos internos `TRIGGER_FIRED`, `CONSEQUENCE_RECORDED`, `CONSEQUENCE_RESOLVED`, `OBJECTIVE_COMPLETED`, `OBJECTIVE_FAILED`, `CLOCK_COMPLETED`; referências, origem causal e contadores, sem alterar journal ou inventar ações. Snapshots anteriores com histórico incompleto permanecem compatíveis quando não há trilha causal auditada. Operações inválidas falham atomicamente.
2. **Objetivos:** `validateAdventureCheckpoint` agora exige ID, título, tipo e alvo idênticos ao módulo publicado; importação não pode reclassificar objetivo oculto como visível nem inflar meta. A projeção pública usa a definição de visibilidade do módulo, não somente o save.
3. **Cinco aventuras:** novo `publishedAdventureGraphGate.mjs` certifica estrutura dos cinco grafos publicados: início, nós alcançáveis, desfecho alcançável, referências válidas; executado na construção do módulo UAE. NÃO é certificação do texto, encontros ou regras contra PDFs.
4. **Age of Rebellion + Edge of the Empire — Mañoso/Ajustador (Tinkerer):** quatro posições AoR passam a ter mecânica executável: cada rank permite atribuir **+1 hard point a um item diferente**, uma vez por item. Capacidade refletida em equipamento/attachments, inventário, interface bridge Android e saves/reaberturas. O motor EotE compartilha o identificador TINK. API: `tinkererStatus`, `applyTinkererHardPoint({kind,itemIndex})`. Não concede efeitos aos outros 145 espaços AoR pendentes.
5. **Integração Android:** `GameController`, `BraseiroUiBridge`, `equipmentOwnedSummary`, `itemHardPoints` e bundle offline atualizados; nenhuma alteração em código Kokoro/TTS.

## Testes e evidências

- Regressão regular: **1554/1554 PASS** (255 arquivos).
- Stress: **6/6 PASS**. Total **1560/1560 PASS**.
- Kokoro/TTS: **27/27 PASS** (subconjunto; não é teste de áudio no aparelho).
- Testes novos P141: **38/38 PASS**, cobrindo os cinco adaptadores, eventos falsificados, rollback, reabertura JSON, objetivos e Mañoso.
- Verificações: bundle offline 77 módulos; sintaxe JS PASS, Java 8 arquivos PASS com símbolos Android não resolvidos; JSON 22, XML 2 PASS. SHA/ZIP conferidos separadamente.
- `evidence/P141/`: logs, matriz de talentos e auditoria dos cinco grafos.

## Pendências e limites

- AoR: **215/360 posições** com efeito executável; **145 posições pendentes**. As árvores podem ser compradas, mas não equiparar compra a mecânica concluída.
- Cinco aventuras integradas estruturalmente ao UAE; **não homologadas integralmente contra PDFs oficiais**. Em *Além da Orla*, o adaptador mantém `encounters:false`; não inventar o combate oficial.
- Expansões EotE/AoR não certificadas integralmente contra os livros. Sem PDFs locais nesta rodada.
- APK Android **não compilado** neste host; não houve testes de dispositivo, áudio Kokoro real nem homologação S23 Ultra/A57. Exige Android SDK 36/Gradle/runner e smoke em aparelho.
- Próxima execução: usar P141; implementar as 145 posições AoR restantes com base textual verificável, confrontar cinco aventuras e expansões com PDFs, fechar *Além da Orla* sem encontros inventados e compilar/homologar Android.
