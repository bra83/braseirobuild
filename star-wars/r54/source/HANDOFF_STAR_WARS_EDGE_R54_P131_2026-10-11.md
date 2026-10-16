# Braseiro Star Wars Edge of the Galaxy — R54 P131

**Base preservada:** `BRASEIRO_STAR_WARS_EDGE_R54_P130_SOURCE_CHECKPOINT.zip`. **Entrega:** `BRASEIRO_STAR_WARS_EDGE_R54_P131_SOURCE_CHECKPOINT.zip`. Android `versionCode=10131`, `versionName=1.0.131`, shell `data-version=1.0.131`. **Checkpoint de fonte; não é APK homologado.**

## Código efetivamente implementado

1. **Progressão de especializações AoR:** `rules/progression.mjs` passa a reconhecer exclusivamente as 18 especializações de seis carreiras do catálogo AoR Core, isoladas de EotE. Custo de segunda/terceira especialização por número de árvores já possuídas (dentro da carreira: 20/30 PE; fora: 30/40 PE), valida saldo, duplicação e pertença ao catálogo. Ao adquirir, debita PE, adiciona quatro perícias de carreira da especialização, registra `rulesProfile` e fonte de compra no personagem. As compras são persistidas pela cadeia `GameController` / `BraseiroUiBridge` / repositório existente. A especialização inicial continua sendo a fonte de verdade.
2. **Talentos em múltiplas árvores AoR:** `rules/talents.mjs` resolve a árvore a partir das especializações realmente adquiridas, inclusive compras entre carreiras, e bloqueia talentos de árvores não possuídas. Grafo, custo, saldo, persistência e efeitos imediatos já implementados permanecem inalterados. Nenhum talento incompleto foi anunciado como mecanicamente ativo.
3. **Checkpoint UAE endurecido nas cinco aventuras:** `publishedAdventureUaeBridge.mjs` valida snapshots v2 ANTES da migração, impedindo que correções automáticas mascarem corrupção. Valida histórico de cenas, status, contadores de gatilhos, estrutura de eventos, turnos, consequências, simetria de histórico de objetivos e status de relógios. O mundo original não é mutado ao rejeitar snapshots inválidos. Saves anteriores v1 ainda passam pelo caminho de migração existente.
4. **Compatibilidade:** interface web legada EotE mantida sem introduzir formulários AoR no seu fluxo; API de progressão AoR já exposta pelo `BraseiroUiBridge` para a UI Android. Java de Kokoro/TTS não foi alterado. Bundle offline regenerado dos módulos canônicos, sem edição manual.

## Testes

- `python3 tools/run-node-suite.py`: **1.306/1.306 PASS**, 249 arquivos (243 regulares + 6 stress), 0 falhas, 0 pulados. +9 testes sobre P130.
- `node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**, sem teste de síntese real em dispositivo.
- `node tools/build-ui-bridge-bundle.mjs` e `node build-ui-bridge-bundle.mjs`: PASS, byte-idênticos.
- `node tools/verify-final-source.mjs`: **PASS** (`validation/P131_SOURCE_GATE.txt`, exit=0); 121 arquivos JS/ESM, 8 Java no gate sintático, 77 JSON, 2 XML, segredos 0. Android SDK/ADB indisponíveis, portanto APK não compilado.
- Novos testes: `tests/uae-p131-aor-multispec-and-checkpoint-gate.test.mjs` cobre compras AoR dentro/fora de carreira, 18 especializações, compra e restauração no bridge, e oito corrupções diferentes em cada uma das cinco aventuras.

## Não concluído / próxima execução

- Efeitos ativos e passivos das demais famílias de talentos AoR e expansão EotE/AoR: ainda incompletos. Catálogo e aquisição não equivalem a implementação de todos os efeitos.
- Conteúdo original dos cinco livros de aventura e combate de *Além da Orla*: ainda exigem confronto com PDFs; não inventar encontros.
- APK e áudio Kokoro em Galaxy S23 Ultra/A57: compilar e homologar pelo runner self-hosted BRELGA, com SDK/ADB reais. Sem APK nesta rodada.
- Para retomar: usar P131 como fonte canônica, P130 como fallback; executar suíte e gate antes de novos patches; não regredir versão nem substituir regras por RAG.
