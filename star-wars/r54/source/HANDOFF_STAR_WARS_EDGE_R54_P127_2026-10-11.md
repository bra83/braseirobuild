# Braseiro Star Wars — R54 P127 — checkpoint de continuidade

**Base real:** R54 P126 do Google Drive, arquivo `BRASEIRO_STAR_WARS_EDGE_R54_P126_SOURCE_CHECKPOINT.zip`, SHA-256 `71abfabd4658ae0623a77e092160a45785a466f87072d7f7f6121e71c8e6639e`. O conteúdo foi extraído integralmente, sem regressão para P113.

**Versão deste checkpoint:** Android `versionCode=10127`, `versionName=1.0.127`; UI offline `data-version=1.0.127`.

## Implementado neste bloco

1. Universal Adventure Engine v2: `dispatchEvent` com fila de eventos encadeados, prioridades estáveis, limites globais de cascata, contador persistente de disparos e eventos de auditoria.
2. `transition` transacional: erros de efeito/trigger restauram o snapshot integral; valida estado ativo, mantém `from` correto, aplica entrada do nó e conclusão/arquivamento sem permitir nova transição após término.
3. Condições compostas `all:`/`any:` e listas conjuntivas; condição desconhecida não abre escolha por padrão.
4. Eventos mecânicos para progressão/conclusão/falha de objetivos, clocks, pistas, consequências e transições. Clocks respeitam limites e estados pausados/expirados, e `CLOCK_COMPLETED` só dispara na passagem para concluído.
5. Contrato de módulo reforçado: grafo de escolhas, IDs duplicados, gatilhos, objetivos, relógios e tipos; migração idempotente de snapshots antigos, conciliação de objetivos/relógios, rejeição de snapshots de outra aventura ou versão futura.
6. IDs sequenciais persistentes de eventos do runtime, sem colisão após restauração; bundle offline de 68 módulos regenerado pelos builders canônicos.
7. Nove testes de integração/regressão adicionais (`tests/uae-v2-p127-regression.test.mjs`), incluindo verificação dos cinco adaptadores publicados e de seus grafos.

## Testes efetivamente executados

- Base P126 antes das mudanças: `1242/1242 PASS`.
- P127 completo: `1251/1251 PASS`, 0 falhas, 245 arquivos de testes (6 suites de stress incluídas).
- Verificador `node tools/verify-final-source.mjs`: PASS; JS 120 arquivos, Java syntax gate 8 arquivos, JSON 75, XML 2, secret scan 0 hits, Gradle wrapper íntegro.
- Kokoro/TTS/fallbacks: 27/27 PASS em suíte focal. Nenhum código nativo de TTS foi alterado.
- Android APK e teste em dispositivo: NÃO executados neste ambiente (Android SDK, adb e sdkmanager ausentes). Compilação deve usar o fluxo do self-hosted runner BRELGA.

## Limites ainda abertos (não declarar 100%)

- As cinco aventuras têm adaptadores, fluxo e testes existentes; o P127 reforça a engine e valida compatibilidade, **não reescreve os cinco adaptadores legados para usar AdventureRuntime como única fonte de estado**.
- A matriz P126 ainda registra `EQUIP.QUALITIES` e `EQUIP.ATTACHMENTS` parciais e 16 famílias de expansão EotE/AoR em estado RAG/declaração. Este bloco não promove essas famílias sem implementação mecânica e testes.
- Testes Java de sintaxe não equivalem a `assembleDebug`, instrumentação Android ou teste real de voz.

## Retomada segura

1. Extraia o ZIP P127 completo e valide SHA-256. Não misture com main antiga do GitHub nem com P113.
2. Rode `python3 tools/run-node-suite.py` e `node tools/verify-final-source.mjs`.
3. Prossiga com `EQUIP.QUALITIES` + `EQUIP.ATTACHMENTS` e, depois, expansões com base nos livros autorizados, por famílias completas.
4. Mantenha TTS Android e os 27 testes focais intactos. Para APK use o workflow GitHub/self-hosted BRELGA com SDK 36 e Java 17.

**Fontes:** pasta de checkpoints Google Drive `BRASEIRO_STAR_WARS_CHECKPOINTS` (ID `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`); materiais `Star Wars - Edge of the empire` (ID `1xIV-Ltig3Efzgs54owyyfnIaakuS30cN`).
