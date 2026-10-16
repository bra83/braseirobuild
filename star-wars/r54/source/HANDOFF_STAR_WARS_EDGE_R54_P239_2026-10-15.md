# HANDOFF — BRASEIRO STAR WARS EDGE R54 P239 — 2026-10-15

**Checkpoint P239 implementado e testado localmente; backup remoto exige confirmação externa de readback.** Base P238 íntegra SHA256 `8ce6faa8f7e77edefbf07eba5c8cbb429cb00d985cbd1d1ee52d7e09566abced`. Não recomeçar. Não confundir esta etapa com jogo completo.

## Implementação substancial SWE02 — interceptação real de Thwheek

- `rules/vehicles.mjs`: ficha da G1-M4-C Dunelizard da SWE02 p.12, silhueta 3, velocidade 4, manobra +1, defesa 1, blindagem 3, casco 8, sistemas 7, laser médio vinculado de dano 6. Perfil isolado do catálogo de compra.
- `adventure/swe02ThwheekP239.mjs`: missão determinística, causalidade de rota/combate, início único, recibos de ataques, troca de manobras após primeiro acerto, retirada com cinco traumas, perseguição, vitória/queda distintas, prova antifalsificação.
- `adventure/longArmOfTheHutt.mjs`: novo grafo Ryloth (chegada, interceptação, combate, queda, Nabat), sem alterar saves legados, verificação de recibos.
- `core/controller.mjs`: inicia o duelo pelo botão da aventura e usa o motor real de ataque veicular. NPC usa Agilidade 3 e Artilharia 0 da fonte, sem aceitar substituição arbitrária pelo Mestre. Não permite dano manual ou encerrar combate como vitória. Vinculada exige acerto e duas Vantagens. Se Krayt Fang cai, Pilotagem Difícil e Aguante Difícil de cada PC, crítico e Ameaças reais.
- `core/uiBridge.mjs`, bundle offline de 123 módulos, `r46/final-runtime.js` e `index.html/js/ui/app.mjs`: interface para os comandos oficiais. Android SOURCE 1.0.239/code 10239, sem APK.
- `tests/uae-p239-swe02-thwheek-vehicle-combat.test.mjs`: 13 testes focais de regras, controlador, save/reabertura, rollback, UI Bridge e negação de falsificação.

## Evidência e limites

- Fonte primária oficial FFG `https://images-cdn.fantasyflightgames.com/filer_public/80/93/80935c12-24ac-442b-b423-687d4e28ae89/long_arm_of_the_hutt_lr.pdf`, páginas impressas 8,12,13.
- `validation/P239_SWE02_SOURCE_PROOFS.json` mapeia regra → executor → controlador → teste e descreve lacunas semânticas.
- Regressão global por arquivo: **364 arquivos (358 regulares + 6 stress), 3279 PASS regulares + 6 PASS stress = 3285 PASS, zero FAIL, zero SKIP**. Evidência individual em `evidence/P239/test-results`.
- Sintaxe 549 MJS + 2 JS runtime, 22 JSON, 2 XML PASS; oito Java/Kokoro byte-idênticos ao P238; bundle 123 módulos.
- Bloqueios em `validation/P239_CLOSURE_AND_BLOCKERS.md`: a SWE02 e as demais quatro aventuras NÃO estão certificadas integralmente; AoR 360 consumidores NÃO certificados; sem Gradle/APK/CI/ADB/GitHub push/teste físico.

## Backup e retenção

- Produzir ZIP fonte Android Studio completo e delta reconstruível a partir de P238, verificar SHA256/CRC/byte compare.
- Fazer upload ao Google Drive `BRASEIRO_STAR_WARS_CHECKPOINTS`, pasta `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`, em partes de até 32 MiB, SHA sidecar, manifesto, delta, handoff, matriz, evidências.
- **Só marcar backup Drive como OK após readback real de TODAS as partes, hashes individuais, reconstrução CRC/SHA e comparação byte a byte.** Em falha, P238 continua o último backup remoto homologado.
- Manter P239 atual, P238 rollback, P126/P113 marcos; preservar P237/P236/P234/P233 até prova de redundância. Nenhuma exclusão automática. Não apagar PDFs/saves/outros VTTs.

## Próxima prioridade

Megalote SWE02 Nabat/New Meen social, viagem/carga e resolução editorial dos três atos, depois outras quatro aventuras, regras EotE/AoR e 360 consumidores AoR. Somente depois APK e Android real. Recorrência horária existente continua, sem novo agendamento.
