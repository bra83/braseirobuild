# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P201

**Base preservada:** P200, SHA-256 `7a89484adc5d13e5063849fd07ac07a80cb8db4c495c15625cd673513be2b327` (6.384 arquivos). P201 continua o mesmo projeto Android Studio. Não reiniciar nem declarar homologação sem prova.

## Código real P201 — bomba do airspeeder SWE09

PDF oficial **The Jewel of Yavin**, Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, seção *Episode III / Countdown to Disaster / Detonation Detection* (cotejo direcionado em 14/10/2026, não certificação integral). Novo `jewelEscapeBomb.mjs` implementa rolagens reais: detectar alteração do speeder por Mecânica/Computadores Média ou Percepção Difícil; desarmar por Mecânica/Frieza Difícil; conduzir a uma plataforma por Pilotagem Planetária Média; saltar com Atletismo/Coordenação Fácil após aproximação ou Média sem ela; teste Difícil de Coordenação para segurar a joia não acondicionada. A bomba é armada para explodir **30 segundos depois da entrada**, mas NÃO foi inventado tempo por rolagem. Falha no desarme registra explosão e **10 danos brutos antes da Absorção**, mais queda pendente; o motor NÃO aplica ferimentos automaticamente sem calcular Absorção e regras de queda. Aviso narrativo sem teste exige confirmação do Mestre. Nenhuma recompensa ou crédito inventado.

Recibos `JEWEL_ESCAPE_BOMB` persistem faces exatas, pool, personagem, estado, decisões e ordem. `validateJewelEscapeBomb` reconstitui a sequência, rejeita recibos adulterados/duplicados, valida transições e compatibilidade com saves P200 sem bomba. Controller usa `atomicAction`, UI bridge e painel Android PT-BR integram o fluxo. Quando a armadilha foi iniciada, a aventura não avança durante fases intermediárias. Android **1.0.201** (`versionCode 10201`), bundle offline **97 módulos**; oito Java/Kokoro preservados sem alterações.

## Testes

**320 arquivos regulares: 2.947/2.947; seis stress sequenciais: 6/6; TOTAL 2.953/2.953 em 326 arquivos por execução individual**, 10 testes focais P201. Não declarar suíte monolítica. Evidências em `evidence/P201` (TEST_SUMMARY, PER_FILE_RESULTS, SYNTAX_AND_JAVA, PDF_TARGETED_SOURCE_CROSSCHECK, IMPLEMENTATION_EVIDENCE). Sintaxe 131 MJS + 2 JS validada. ZIP e delta íntegros conforme SHA externo e manifesto.

## Limites e P202+

A bomba não possui temporizador de relógio em segundos com tempo por ação (não definido pela fonte), e **não** existe aplicação automática de Absorção/queda, encontro de quatro capangas de Arend ou perseguição integral; o fluxo está em `cloud-city-pursuit` (grafo legado), não em uma nova cena exclusiva. Faltam gatilhos de Naboka, consequências completas de Arend, SWE02 New Meen/Geonosis/Teemo, cotejo cena a cena das cinco aventuras e 360 regras AoR/EotE/expansões. Próximo bloco: completar consequências da explosão e fuga, integrar encontro de quatro capangas e perseguição real sem inventar regras; cotejar PDFs oficiais. Preservar Kokoro, saves e regressões; gerar P202+ somente com código real, testes focais + regressão por arquivo + seis stress, ZIP completo + SHA + delta + handoff e readback Biblioteca.

**Sem APK, SDK/Gradle/adb, CI BRELGA, publicação GitHub ou homologação física.** Manter tarefa ativa até homologação comprovada.
