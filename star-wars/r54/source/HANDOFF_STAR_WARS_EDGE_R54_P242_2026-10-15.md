# HANDOFF — BRASEIRO STAR WARS EDGE R54 P242 — 2026-10-15

**NÃO RECOMEÇAR.** Base P241 canônica e verificada SHA256 `a726c6fa51fb5d5f72f96b2feb563ecbd146ec9f09f69ef0d57c9d440f2a1ea1` (15.375 arquivos). O P242 mantém integralmente todos os arquivos anteriores, não exclui regras, aventuras, saves, PT-BR, Kokoro externo ou Motor Bárbara.

## Implementação real P242 — SWE02 New Meen

Fonte primária https://images-cdn.fantasyflightgames.com/filer_public/80/93/80935c12-24ac-442b-b423-687d4e28ae89/long_arm_of_the_hutt_lr.pdf, páginas impressas 20–23. Os recibos trazem `productCode:'SWE02'` e página impressa 23. Não confundir incapacitação com morte: o Mestre confirma fatalidades explícitas apenas até o número de capangas retirados pelo dano REAL de combate. Recibos vinculam grupo, encontro, ferimentos, número de minions e rodadas; o fechamento confronta os recibos com o estado final do combate.

- `app/src/main/assets/vtt/js/adventure/swe02NewMeenP242.mjs`: confirmação de fatalidades, Coerção com 1–4 mortos = Difícil/3, 5–8 = Médio/2, 9+ = Fácil/1; nenhum teste permitido com zero mortes. Usa perícia real `coaccion`, faces de dados verificadas e ação real. Apenas capangas vivos se rendem após sucesso, Drombb não. Cofre oficial de 2.000 créditos: chave de Drombb derrotado ou Atividade Criminal Difícil; transação atômica, não repetível. Falha não concede créditos.
- `adventure/longArmOfTheHutt.mjs`: prova de rendição, snapshots de grupos com ferimentos e contagem, derrota com `newMeenOutcome='LOST'`, resgate de B'ura e Ota após combate perdido, sem vitória automática. `rules/encounter.mjs` ignora grupos rendidos nas ações da iniciativa sem falsificar ferimentos.
- `core/controller.mjs`: três ações reais `adjudicateSWE02NewMeenDeaths`, `coerceSWE02NewMeenThugs`, `openSWE02DrombbSafe`; impede finalizar derrota de New Meen com personagem consciente. `core/uiBridge.mjs`, `ui/app.mjs`, `index.html` oferecem botões offline PT-BR. `r46-shell.html`, `r46/final-runtime.js`, `app/build.gradle.kts` versão SOURCE 1.0.242/code 10242.
- `tests/uae-p242-swe02-newmeen-fatalities-coercion-rescue.test.mjs`: **13/13 PASS** — limites, coerção 3/2/1, ação, rendição, Drombb hostil, noite→cantina, derrotas, Ota, cofre, adulteração, rollback, bridge/UI e três reaberturas.

## Gates executados nesta execução

- **367 arquivos de teste individuais: 3.324 PASS, zero FAIL, zero SKIP**, sendo 361 arquivos regulares + seis stress (6/6). Logs individuais `evidence/P242/test-results/*.tap`; resumo `evidence/P242/regression_summary.json`.
- Sintaxe **555 MJS, dois JS runtime, 22 JSON, dois XML PASS**; oito arquivos Java/Kokoro byte-idênticos ao P241; bundle offline gerado com **126 módulos**. `evidence/P242/syntax_preservation.json`.
- Testes simulam dano via executor `applyAdversaryDamage` e o controlador real; **não equivalem a homologação editorial completa nem a teste em Android físico**.

## Pendências reais

1. SWE02: escolta New Meen até Nabat, aliados Twi'lek armados e consequências; suprimentos/armas EotE primárias de Nyn e defeitos na primeira utilização; nave Beginner, combustível/carga/relógio, iniciativa veicular; atos 2 e 3 e finais, cotejo página por página.
2. Outras quatro aventuras: Under a Black Sun, Beyond the Rim, The Jewel of Yavin, Mask of the Pirate Queen; todas as regras EotE/AoR/expansões e 360 consumidores AoR **individualmente** fonte→página→executor→UI→3 reaberturas→teste.
3. APK, Gradle, BRELGA, GitHub push, ADB, Galaxy A57/S23 Ultra e Kokoro físico **NÃO executados**, conforme ordem de serem os últimos gates.

## Checkpoint, backup e retenção

ZIP P242 COMPLETO, SHA sidecar, delta P241→P242 reconstruível, matriz, evidências e relatório de integridade devem ser produzidos e enviados para Google Drive pasta `BRASEIRO_STAR_WARS_CHECKPOINTS` id `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`. Dividir ZIP em quatro partes ≤32 MiB, criar manifesto, verificar listagem, READBACK de todas as partes, reconstrução ZIP SHA e byte compare antes de promover. Até esse gate, **P241 continua canônico remoto**. Não excluir nenhum backup; preservar P241, P240 e marcos P126/P113, assim como P239/P238/P237/P236/P234/P233 até prova de redundância.

**Autoridade mecânica exclusiva:** código determinístico; RAG apenas cita fontes; Gemini/Motor Bárbara narram/propõem sem aplicar estado. Sem PE, créditos, dano ou vitórias fabricados. Recorrência horária permanece ativa; NÃO criar nova automação nem pedir OK.
