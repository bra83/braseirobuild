# HANDOFF STAR WARS EDGE R54 P90 — 2026-09-24

Baseline real: P89 materializado do Google Drive. Projeto não recriado. GitHub não usado.

## Correção principal
- Corrigido bug de Obligation em Sob um Sol Negro: efeitos de escolhas não sobrescrevem mais `obligationPendingDelta` já acumulado.
- Desespero no Spyder (+5) seguido de saque de armas da Zelcomm (+5) agora resulta em +10, não +5.
- Novo ledger `obligationPendingSources` preserva as origens separadamente (`spyder`, `zelcomm`).
- Saves legados com `obligationPendingDelta` mas sem ledger migram automaticamente o saldo anterior para `legacy` antes de novas somas.
- Save/reopen preserva total e ledger.
- Epílogo persiste `obligationPendingDelta` e `obligationPendingSources` em `publishedAdventureConsequences`.

## Preservação
- EotE/AoR/MIXED preservados.
- Duty/Obligation já existentes preservados; a correção altera apenas a acumulação pendente da aventura publicada.
- Sob um Sol Negro, Além da Orla, Mundo Vivo e anti-spoiler passam na regressão ampla.

## Versão
- 1.0.90
- versionCode 10090
- runtime r54-p90
- uiBridge.bundle.js regenerado de 61 módulos canônicos.

## Evidência
- Gate focal P90/P89/P81: 11/11 PASS, 0 FAIL.
- Sob um Sol Negro: 106/106 PASS, 0 FAIL.
- Regressão completa em 4 shards: 268 + 212 + 270 + 269 = 1019/1019 PASS, 0 FAIL.
- `node --check` dos módulos modificados e runtime: PASS.

## Build Android
Não compilado: não houve alteração nativa funcional; compilar não acrescentaria evidência à correção de estado JS. O marco Android continua reservado para validação real do share-back Gemini -> Star Wars.

## Próximo
- Continuar memória/resposta de NPCs e repercussões Black Sun em Sob um Sol Negro.
- Validar share-back Gemini em APK/dispositivo antes de declarar PASS nativo.
