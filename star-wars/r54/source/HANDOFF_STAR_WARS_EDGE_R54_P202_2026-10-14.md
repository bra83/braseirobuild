# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P202

**Base preservada:** P201, ZIP SHA-256 `29bc82c1c0118acdc09e03458ef21ad3b3be12aeb775ebcceb875ace797ae35f`, 6.393 arquivos. Não reiniciar. Android Studio integral; sem APK, CI ou homologação física.

## Código real P202 — SWE09 The Jewel of Yavin, fuga de Cloud City

Cotejo DIRECIONADO em 14/10/2026 com **SWE09 p.90** (Google Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`) e **Edge of the Empire Core Rulebook Tabela 6-9** (`1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`). A explosão do airspeeder agora aplica **10 dano bruto menos Absorção REAL** na ficha do jogador, com ferimentos, limiar e crítico processados pelo motor existente. Queda **Curta** (10 dano bruto, 10 Tensão) ou **Média** (30 dano bruto, 20 Tensão), com faixa confirmada pelo Mestre, permite teste **Médio de Atletismo ou Coordenação**; sucessos reduzem dano, vantagens reduzem Tensão; Absorção não reduz Tensão. Dano e Tensão persistem, rollback atômico, 3 reaberturas, recibos `JEWEL_ESCAPE_IMPACT` reexecutáveis e validados contra faces, ficha, estado e duplicatas. `jewelEscapeImpact.mjs`, controller, bridge, interface Android PT-BR integrados.

**Limite explícito:** quedas Longa/Extrema NÃO são convertidas em pontos fictícios: o livro exige incapacitação/crítico, que permanecem para adjudicação específica; pontos de Destino e consequências em companheiros também não foram automatizados. Não há contagem inventada de segundos por ação.

**Quatro capangas de Arend:** o perfil oficial **Shen's Goons** foi cotejado com SWE09 p.90 e carta do compêndio `shens_goons_90` (`JoY:87`): quatro minions, Absorção 4, 5 Ferimentos por minion, carabina blaster dano 9 e soco inglês, Golpes Letais 1. O encontro `arend-shen-four-goons` surge no alcance Médio após a resolução da armadilha e consequências e inicia COMBATE REAL via `beginPublishedAdventureEncounter`. Avanço da cena é bloqueado até a resolução. Saves P201 sem bomba preservam o encontro legado de segurança. Perseguição veicular completa, Arrow-7 e escolhas de Naboka NÃO concluídas.

## Testes e integridade

Regressão por arquivo executada novamente sobre P202: **321 arquivos regulares, 2.957/2.957 testes; seis stress 6/6; TOTAL 2.963/2.963 em 327 arquivos individuais**. Não declarar suíte monolítica. Focais P202: 10/10. Evidências `evidence/P202/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`. Sintaxe **132 MJS**, JSON/XML e JS conferidos. Android **1.0.202** (`versionCode 10202`), bundle offline **98 módulos**; oito Java/Kokoro byte a byte iguais a P201.

## Prioridades P203+

1. Completar Longa/Extrema na Tabela 6-9 com crítico, incapacitação e intervenção de Destino sem falsear consequências. Aplicar explosão/queda separadamente aos companheiros e personagens atingidos com confirmação da posição real.
2. Arrow-7 real com perseguição veicular, armas pessoais vs escala planetária, fuga/negociação e desfechos; Arend e Naboka somente com gatilhos oficiais.
3. SWE02 New Meen/Geonosis/Teemo, cotejo cena a cena das cinco aventuras e 360 consumidores AoR/EotE/expansões.
4. Rodar focal + regressão por arquivo + 6 stress; preservar Kokoro, gerar ZIP Android Studio integral, SHA, delta, handoff, upload Biblioteca e readback. GitHub/BRELGA/Gradle somente com acesso e execução comprovados.

**Sem APK, sem CI BRELGA, sem homologação física, sem certificação integral das cinco aventuras.**
