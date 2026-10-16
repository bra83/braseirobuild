# HANDOFF CANÔNICO — STAR WARS EDGE R54 P238 — 2026-10-15

**Estado: P238 código + regressão concluídos; não é entrega final.** P237 anterior SHA256 `4582342fd84cb2dd62a1656271a94788a63713e6e764d3c177336f0c67679783` (13.146 arquivos). Não reiniciar. Preservar P237, P236, P234/P233 e P126/P113 até retenção segura, PDFs e saves sempre.

## Alterações REAIS P238

- `app/src/main/assets/vtt/js/adventure/swe02KraytFangP238.mjs`: máquina determinística de onze verificações da nave Krayt Fang, páginas oficiais SWE02 7–11, estado/recibos e verificação de adulteração, rastreador, curso, ruído, peles e B’ura.
- `app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs`: novo fluxo opcional verificado da nave, inspeção do casco em terra, checks com índices estáveis, flags de interceptação e rota antiga preservada **não certificada**.
- `app/src/main/assets/vtt/js/core/controller.mjs`: modificadores de odor de personagens oficiais aplicados em rolagem real da inspeção; bundle offline 122 módulos.
- `tests/uae-p238-swe02-krayt-fang-ship-source.test.mjs`: 12 testes focais com controlador, três reaberturas e adulteração negada.
- `app/build.gradle.kts`, `app/src/main/assets/vtt/r46-shell.html`, `app/src/main/assets/vtt/r46/final-runtime.js`: Android apenas fonte 1.0.238/code 10238.

## Gates executados

**363 arquivos individuais: 357 regulares, 3266 PASS + 6 stress, 6 PASS = 3272 PASS, 0 FAIL, 0 SKIP.** Logs individuais em `evidence/P238/test-results`, resumo JSON. Sintaxe 544 JS/MJS, 22 JSON, dois XML PASS, oito Java/Kokoro preservados byte-idênticos ao P237. Nenhum APK, Gradle, GitHub, CI, ADB ou aparelho físico.

## Bloqueios e próxima prioridade

**SWE02 ainda incompleta.** Interceptação de Thwheek sinalizada, mas **não** há combate espacial implementado neste lote. Falta concluir Ato 1, Ato 2, Ato 3, fontes oficiais e cinco aventuras, mais EotE/AoR/expansões e 360 consumidores AoR. Detalhes `validation/P238_CLOSURE_AND_BLOCKERS.md`. Implementar em megalotes, sem falsa certificação. Só depois compilar APK e testar Galaxy A57/S23 Ultra e Kokoro.

## Backup

ZIP P238 integral, SHA256, delta P237→P238 e manifesto serão gerados após fechamento dos gates. O backup Drive só pode ser marcado PASS após upload em partes <=32MiB e readback de TODAS as partes, hash reconstruído e comparação byte a byte. Drive `BRASEIRO_STAR_WARS_CHECKPOINTS`, pasta ID `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`. Se falhar, não promover como checkpoint remoto, preservar ZIP local. Nenhuma exclusão automática de versões históricas.
