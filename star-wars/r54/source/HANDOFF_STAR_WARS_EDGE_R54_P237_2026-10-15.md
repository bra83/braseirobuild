# HANDOFF CANÔNICO — STAR WARS EDGE R54 P237 — 2026-10-15

**P237: implementação e testes concluídos, NÃO é entrega final.** A versão anterior verificada é P236 (SHA256 `a5a7e549b4aec05e18bbc266084019ca01fdb8a25db3caf72f92215f24c8738d`, 12.765 arquivos). Preservar integralmente P236, P234/P233, P126/P113 históricos; nunca remover PDFs, saves, outros VTTs.

## Código real P237

- `app/src/main/assets/vtt/js/adventure/swe02GeonosisP237.mjs`: tabela determinística de fatos, argumentos, pontuação Dimmock e proteção contra pontos/provas duplicados ou inconsistentes.
- `app/src/main/assets/vtt/js/adventure/longArmOfTheHutt.mjs`: Geonosis (Piddock, Dimmock, Gantry AA7 e Lucky Guess) integrada ao controlador e à persistência, com opções e checks oficiais; rota legada preservada sem ajuda falsa.
- `app/src/main/assets/vtt/js/rules/adversaries.mjs`: Geonosian Soldier e Vrixx’tt oficiais distintos.
- `tests/uae-p237-swe02-geonosis-dimmock-gantry.test.mjs`: 12 testes novos PASS, controlador real e 3 reaberturas.
- Android Studio fonte versionName 1.0.237/versionCode 10237, bundle offline refeito 121 módulos.

## Gates efetivamente executados

- Regressão **356 arquivos regulares / 3254 PASS; seis stress / 6 PASS; total 3260 PASS / 0 FAIL em 362 arquivos**.
- Sintaxe 570 JS/MJS + 22 JSON + 2 XML PASS; 8 Java/Kokoro idênticos ao P236.
- Nenhum APK, Gradle, GitHub push, CI BRELGA, ADB ou Android físico.
- Fonte oficial SWE02: `https://images-cdn.fantasyflightgames.com/filer_public/80/93/80935c12-24ac-442b-b423-687d4e28ae89/long_arm_of_the_hutt_lr.pdf`, páginas impressas 7, 23, 30, 34, 36, 37. Evidência detalhada `validation/P237_SWE02_SOURCE_PROOFS.json`.

## Pendências reais e próxima prioridade

Fechar SWE02 inteira: nave/Thwheek/Nabat/New Meen alternativas, Piddock convidados e limites, Anatta pagos/Jabba, cobertura do gantry e pagamento real, Lucky Guess, Teemo (lustre, reforços, gladiadores, fuga, recompensas e finais). Depois cotejar integralmente Under a Black Sun, Beyond the Rim, The Jewel of Yavin, Mask of the Pirate Queen. Certificar todas as regras EotE/AoR e expansões selecionadas, inclusive 360 consumidores AoR individualmente. **Não chamar grafos e testes estruturais de certificação editorial.** APK e Galaxy A57/S23/Kokoro somente após esses gates.

## Backup e recuperação

Checkpoint fonte ZIP integral e SHA sidecar são produzidos externamente a este handoff; exigir CRC, SHA e delta P236→P237 reconstruível. Drive pasta `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`: enviar em partes <=32MiB, manifesto SHA por parte e readback completo. Não declarar backup OK antes do download e reconstrução verificada. Em falha, manter ZIP local e relatar. Não criar outra automação; continuar em megalotes horários.
