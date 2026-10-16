# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P193

**Continuidade:** P193 foi implementado sobre o ZIP canônico P192, SHA256 `7a3eed41c69f5003e5fb12db8a41c6e96442723a38231829a28ac9bf57e9bfb8`, 98.206.953 bytes, 6.310 arquivos, CRC aprovado. **Não reconstruir a partir de P191 ou estados anteriores.**

**Entregáveis:** `BRASEIRO_STAR_WARS_EDGE_R54_P193_SOURCE_CHECKPOINT.zip` (Android Studio completo), arquivo `.zip.sha256`, delta `BRASEIRO_STAR_WARS_EDGE_R54_P192_TO_P193_DELTA.zip` com manifesto e hashes, e este handoff. Os hashes e tamanhos do ZIP final devem ser conferidos no SHA externo e manifesto do delta.

## Implementação real P193 — SWE09 *The Jewel of Yavin*

1. **Wing Guard:** `jewelOfYavin.mjs` agora deriva os encontros exclusivamente dos recibos `BANK_SLICE_SPEND` revalidados. Antes do ETA de 240 minutos (ou antecipado em -60 minutos por resultado duplicado) a equipe está `en_route`. Quando o relógio canônico alcança a chegada, a equipe fica `ready` na cena bancária ou na perseguição; o painel Android mostra o botão real de encontro. `beginPublishedAdventureEncounter` instancia um grupo minion `bespin_wing_guard_officer` com **número de oficiais exatamente igual ao número de PCs gravado no recibo**, sem inventar sargento. O combate é iniciado e encerrado pelo motor real; `finishCombat` corrigido para aceitar status `ACTIVE`. Resultado e hora são persistidos e verificáveis. Múltiplas equipes, três reaberturas, rejeição de IDs forjados e bloqueio de avanço em confronto pendente.
2. **Investigação se PCs saíram:** o PDF especifica que a equipe investiga o local da tentativa de slicing se os personagens se deslocaram. O motor não teleporta guardas para uma cena não relacionada; mantém `investigating` e pode interceptar no nó `cloud-city-pursuit`. Não há auto-início de iniciativa no instante da chegada; o encontro passa a estar disponível na UI e exige decisão do Mestre/jogador.
3. **Perfis oficiais:** `adversaries.mjs` contém `bespin_wing_guard_officer` (minion) e `bespin_wing_guard_sergeant` (rival), cotejados com SWE09 impresso pp.12–13 e cartões do compêndio `JoY:12`, `JoY:13`. Oficiais usam Relby-K23 (6, crítico 3, Pierce 2), cassetete (5, crítico 2, Disorient 2), soak 3, ferimentos 5 por minion. Sargento foi catalogado mas não inserido na equipe bancária, pois a Tabela 2-1 exige grupo igual aos PCs. **Covering Fire/Tactical Direction são metadados com adjudicação manual**, ainda não automáticos.
4. **Assinatura eletrônica:** além do G-100, `GameController.resolveSkillCheck` aplica upgrade real da dificuldade a testes **explicitamente identificados como slicing** de Cloud City (`codebreaking`, sistema de slicing confirmado ou sondagem de rede da aventura). Testes genéricos de Computadores não recebem penalidade indevida. O PDF exige substituição completa das ferramentas para remover a assinatura; isso ainda não está implementado porque falta um vínculo de inventário/fonte para as ferramentas.
5. **Android:** `r46/final-runtime.js` apresenta ETA, status de investigação, equipe chegada e botão de encontro. `r46-shell.html`, diagnóstico e `app/build.gradle.kts` sincronizados em **1.0.193**, versionCode **10193**. `uiBridge.bundle.js` reconstruído com **91 módulos**. **8/8 Java Kokoro/TTS byte a byte idênticos ao P192**.

## Evidência, testes e limites

- `evidence/P193/TEST_SUMMARY.json`: **312 arquivos regulares, 2846/2846 testes**; **6 stress sequenciais, 6/6**; **TOTAL 2852/2852 em 318 arquivos por execução individual**, 14 testes novos P193. **NÃO é suíte monolítica verde.**
- Sintaxe: **125 MJS**, **22 JSON**, **2 XML**, sem falhas. `evidence/P193/PER_FILE_RESULTS.json` guarda resultados por arquivo.
- PDF oficial consultado no Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, cotejo **direcionado** da Tabela 2-1 (PDF p.70, impressa 69) e perfis Wing Guard (pp.12–13), **NÃO certificação integral**. Ver `evidence/P193/PDF_TARGETED_SOURCE_CROSSCHECK.json`.
- **Sem Gradle/SDK/adb, sem execução CI BRELGA, sem APK e sem homologação física.** Sem comparação integral dos cinco PDFs ou certificação semântica dos 360 talentos AoR/EotE/expansões. Não declarar concluído.

## PRIORIDADE P194+

1. Integrar substituição **real e comprovada** de ferramentas de slicing à ficha/inventário para limpar assinatura; sem inventar custo, item ou redução. Implementar efeitos de Covering Fire e Tactical Direction com testes de combate e fontes.
2. Finalizar banco SWE09 (liquidação sem crédito inventado, evidências, risco Naboka), traição e fuga, rivalidades e consequências.
3. SWE02 Long Arm of the Hutt: New Meen, Geonosis e Teemo com encontros reais e fonte.
4. Certificar cena a cena os cinco PDFs de aventuras e semanticamente os 360 talentos AoR + expansões EotE/AoR, com matriz de evidência individual.
5. Rodar focais + regressão completa por arquivo + stress; preservar Kokoro. Só gerar P194 após avanço substancial, ZIP Android Studio integral + SHA + delta + handoff, upload à Biblioteca e readback SHA/CRC. GitHub `bra83/braseirobuild`/BRELGA somente com acesso real; nunca declarar CI/APK sem execução e prova.

**Não regredir P192 nem declarar cobertura integral baseada em contagem de consumidores.**
