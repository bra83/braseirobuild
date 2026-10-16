# STAR WARS EDGE R54 — P80 — 2026-09-24

## Baseline
- Entrada exata: `BRASEIRO_STAR_WARS_EDGE_R54_P79_SOURCE_CHECKPOINT.zip` do Google Drive.
- Projeto não recriado. GitHub não usado.
- EotE / AoR / MIXED, Duty / Obligation, P79 performance/insets e conteúdo anterior de Sob um Sol Negro preservados.

## Trabalho P80 — Sob um Sol Negro
- Endurecida a fronteira anti-spoiler do estado público da aventura.
- `startUnderBlackSun`, `advanceUnderBlackSun` e `underBlackSunStatus` agora devolvem snapshot público sanitizado em vez do progresso mestre bruto.
- Flags explicitamente internas/GM (`secret*`, `spoiler*`, `internal*`, `gmOnly*`, `masterOnly*`) ficam no save/motor, mas não atravessam a projeção do jogador.
- Compatibilidade de saves antigos preservada: flags customizadas não-secretas continuam disponíveis.
- `choiceEffects` devolvido à UI também passa pela sanitização.
- Testes novos cobrem contaminação de save antigo, start/advance, rotas futuras invisíveis e save/reopen.
- Versão fonte avançada para Android 1.0.80 / versionCode 10080 e runtime P80.

## Evidência
- Gate focal anti-spoiler P80: 4/4 PASS.
- Gate completo de Sob um Sol Negro P33/P36/P41/P42/P46/P49/P57/P58/P63/P80: 53/53 PASS.
- Regressão canônica completa: 986/986 PASS, 0 FAIL.
- `node --check` em `underBlackSun.mjs` e `final-runtime.js`: PASS.

## Build
- Não compilado neste bloco: mudança é de fonte/estado e a regressão completa passou; compilação fica para marco Android relevante, conforme orientação do projeto.

## Próximo bloco
- Continuar conteúdo jogável de Sob um Sol Negro: enriquecer resolução das cenas/checks e consequências de Advantage/Threat/Triumph/Despair onde ainda genéricas, mantendo anti-spoiler e persistência.
- Depois avançar a próxima aventura publicada sem quebrar EotE/AoR/MIXED e Duty/Obligation.
