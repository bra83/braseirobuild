# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P198

**BASE PRESERVADA:** P197 `BRASEIRO_STAR_WARS_EDGE_R54_P197_SOURCE_CHECKPOINT.zip`, SHA256 `e6f0edadb92a0eb9da46f2a791d05f77a24335616bd57da076339f8fb7212cc1`, 98.302.628 bytes, 6.355 arquivos. P198 é continuação sobre o ZIP completo, sem reiniciar o projeto.

## Implementação real P198 — SWE09 Wing Guard, Tactical Direction

1. PDF oficial *The Jewel of Yavin* (SWE09), Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, **página impressa 13**, consultado diretamente em 14/10/2026. A habilidade do sargento permite dar a um grupo minion de oficiais dentro de Médio uma **manobra livre imediata** ou um dado de Reforço ao próximo teste. O modo Reforço P197 foi preservado.
2. `app/src/main/assets/vtt/js/rules/wingGuardTactics.mjs`: novo modo `free_maneuver`, com agendamento pelo Mestre, confirmação do alcance NPC–NPC e do caminho, direção aproximar/afastar, aplicação **IMEDIATA na iniciativa do sargento**, atualização real de `adversary.range` do grupo minion e log `WING_GUARD_FREE_MOVE`. A distância NPC–PC não é confundida com a confirmação NPC–NPC. Sem teleporte e sem movimentar o grupo antes da iniciativa do sargento.
3. Limite intencional: a automação cobre **uma faixa** Engajado↔Curto↔Médio. Deslocamentos envolvendo Longo/Extremo e outras manobras livres permanecem não implementados; o motor rejeita em vez de inventar custo ou geometria. Não declarar cobertura total da habilidade.
4. Proteções: rejeita modo/direção inválidos, alvo fora de grupo, distância sem confirmação, repetição por rodada, duplicidade, token/recibo adulterado e alteração de alcance entre agendamento e iniciativa. Cancelamento seguro se alvo incapacitado; ordem pendente do ator é limpa se incapacitado ou sem ação por staggered. Persistência e três reaberturas JSON verificadas.
5. `controller.mjs` integra ao turno NPC real, `r46/final-runtime.js` oferece seleção de modo/direção/confirmar trajeto em PT-BR, bridge já existente `queueWingGuardTactic` aceita os novos campos, bundle offline reconstruído com **94 módulos**. Android `versionCode=10198`, `versionName=1.0.198`; shell e diagnóstico sincronizados. **8/8 arquivos Java incluindo Kokoro/TTS idênticos ao P197**.

## Testes executados

- `tests/uae-p198-wing-guard-free-maneuver.test.mjs`: **13 testes novos** (fonte oficial, turno do sargento, movimento imediato, caminho confirmado, limites de faixa, antifraude, persistência, compatibilidade, integração real ao turno NPC e UI).
- Regressão por arquivo, **317 arquivos regulares 2.906/2.906**, **6 stress 6/6**, **TOTAL 2.912/2.912 em 323 arquivos**. Execuções independentes `node --test --test-reporter=tap`, resultados `evidence/P198/PER_FILE_RESULTS.json`. **Não é suíte monolítica verde.**
- Sintaxe: **128 MJS, 22 JSON, 2 XML, bundle e runtime JS**, sem erros; `evidence/P198/SYNTAX_CHECK.json`.
- PDF: cotejo **direcionado da habilidade p.13**, NÃO certificação integral de SWE09 ou outras aventuras.

## Artefatos

- ZIP completo `BRASEIRO_STAR_WARS_EDGE_R54_P198_SOURCE_CHECKPOINT.zip` e sidecar `.zip.sha256` com hash real, delta `BRASEIRO_STAR_WARS_EDGE_R54_P197_TO_P198_DELTA.zip` com manifesto de hashes e handoff externo. Conferir SHA do ZIP após qualquer transferência.
- O código Android Studio está no ZIP completo. **NÃO há APK, Gradle/CI BRELGA, adb ou homologação Android comprovados** nesta execução.

## Prioridades P199+

1. Cotejar inventário oficial e implementar `restraining bolt` real (não confundir `restraining_device`) e substituição das ferramentas de slicing para eliminar assinatura eletrônica; sem item ou preço inventado.
2. Concluir as opções restantes de manobra livre com regras de economia de manobras e distância, preservando a implementação imediata.
3. SWE09: liquidação bancária, risco de traição Naboka, fuga, consequências e cronologia; SWE02: New Meen, Geonosis, Teemo.
4. Comparar **cinco aventuras cena a cena** com PDFs e **360 consumidores AoR/EotE/expansões individualmente**. Não declarar cobertura integral sem prova.
5. Testar focal + regressão por arquivo + seis stress; preservar Kokoro; produzir novo ZIP completo + SHA + delta + handoff só com avanço real. Salvar na Biblioteca e confirmar readback SHA/CRC. CI/APK apenas com acesso e evidências reais.

**Tarefa horária permanece ativa até homologação comprovada.**
