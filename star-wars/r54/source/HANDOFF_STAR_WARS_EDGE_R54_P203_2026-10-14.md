# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P203

**Base integral P202 preservada:** `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P202_SOURCE_CHECKPOINT.zip`, SHA-256 `e9157868d8292435ea3f38a29c90987d679a026f9c2064beff52566b989495cc`, 6.402 arquivos. Não reiniciar, não substituir por reescrita, não confundir testes Node com homologação Android.

## Implementação REAL P203 — SWE09 fuga de Cloud City: quedas Longa e Extrema

Fonte **Edge of the Empire Core Rulebook**, PDF oficial Google Drive ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, **Tabela 6-9, página impressa 215**, cotejada diretamente com o texto do PDF em 14/10/2026. Queda **Longa** incapacita o personagem, adiciona Ferimento Crítico com modificador **+50** e impõe **30 Tensão**. Queda **Extrema** incapacita, Ferimento Crítico **+75** e **40 Tensão**; morte alternativa é **decisão do Mestre**, não aplicada automaticamente. Ambas exigem faixa confirmada pelo Mestre, rolagem Média de Atletismo ou Coordenação, com Vantagens líquidas reduzindo Tensão. Sucessos não convertem incapacitação em dano numérico inventado. Triunfo **real** pode, com autorização explícita do Mestre, reduzir a faixa uma etapa: Longa→Média (30 dano/20 Tensão, com Absorção real), Extrema→Longa (+50/30). Não existe redução sem Triunfo.

Implementação reutiliza `rules/environment.mjs` (`applyForcedFall`, com `netAdvantage` opcional compatível com chamadas antigas) e `rules/critical.mjs`. `adventure/jewelEscapeImpact.mjs` agora grava `impactVersion:2`, `gmTriumphReduce`, `effectiveFallRange`, `forcedCriticalCode` para quedas longas/extremas; valida faces, pool, ordem, ferimentos, Tensão, crítico, idempotência e rollback. **Recibos P202 Curta/Média e explosão continuam no formato original** e reexecutam sem migração. Interface Android PT-BR oferece quatro faixas, opção de Triunfo sob confirmação do Mestre e aviso explícito de morte não automática. `controller.mjs`, bridge e bundle offline integrados.

**Limites reais:** ainda não há efeitos da explosão/queda em companheiros com posição confirmada; pontos de Destino não são gastos automaticamente para mitigar quedas; não há perseguição Arrow-7 completa nem finais de Arend/Naboka. A certificação integral de cinco aventuras e 360 consumidores EotE/AoR/expansões segue pendente. Não declarar esses blocos completos.

## Provas e build

Android **1.0.203**, `versionCode 10203`; bundle offline **98 módulos**; **8/8 arquivos Java (incluindo Kokoro) byte-idênticos P202**. Sintaxe verificada em **132 MJS, 16 JS, 22 JSON e 2 XML**. **328 arquivos de teste executados individualmente**: 322 regulares **2.966/2.966** e seis stress **6/6** = **2.972/2.972**. Nove testes focais P203, inclusive replay adulterado, Triunfo, crítico +50/+75, três reaberturas e rollback; P202/regressões preservados. **NÃO houve suíte monolítica, Gradle/SDK/adb, APK, CI BRELGA nem teste físico Android.** Evidências em `evidence/P203/`.

## Próxima prioridade P204+

1. Impactos independentes para cada personagem/companheiro efetivamente presente, com posição confirmada pelo Mestre, sem aplicar dano a ausentes e sem duplicar recibos; manter atomicidade e saves.
2. Perseguição veicular Arrow-7 com os quatro capangas de Arend, distâncias, economia de ações, diferença de escala pessoal/planetária, encontros e persistência; desfechos de Arend/Naboka somente com gatilhos oficiais SWE09.
3. SWE02 New Meen/Geonosis/Teemo; cotejo das cinco aventuras cena a cena e certificação semântica individual das regras EotE/AoR/expansões.
4. Rodar testes focais, regressão por arquivo e seis stress, preservar Kokoro, produzir ZIP integral Android Studio + SHA-256 + delta com reconstrução de todos os hashes + handoff. Upload à Biblioteca e readback com SHA/CRC. GitHub `bra83/braseirobuild` e runner BRELGA somente se realmente executados; nunca declarar APK/CI/homologação sem evidências.
