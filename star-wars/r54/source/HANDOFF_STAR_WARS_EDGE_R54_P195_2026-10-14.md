# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P195

**Base obrigatória:** P194 ZIP integral `BRASEIRO_STAR_WARS_EDGE_R54_P194_SOURCE_CHECKPOINT.zip`, SHA256 `6daa1915742337fe8e94621faf2a84a7b80e33a792abefe5891a4d3084e3e5c6`, 98.248.349 bytes, 6.324 arquivos. **Não reconstruir de P193 ou anterior.**

## Implementação real P195 — G-100 vivo, SWE09 Leave No Trace

- Novo módulo `app/src/main/assets/vtt/js/adventure/jewelBankLiveDroid.mjs` com operação `BANK_DROID`: negociação real de Carisma (skill `carisma`, atributo `presencia`) com dificuldade **Média** (2 dados de dificuldade) após a fase bancária; exige confirmação explícita do Mestre de alojamento na nave. O PDF oficial SWE09 *The Jewel of Yavin*, Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, seção *Leave No Trace* (impressa ~p.71, PDF ~p.72), requer no mínimo Charme Média contra Frieza e sugere acomodação na nave. **A ficha extraída do G-100 não traz rank de Frieza; não foi inventada uma oposição numérica adicional.**
- Sucesso registra G-100 preservado vivo, impedindo desmontagem e extração da caixa-preta. Falha registra recusa, sem inventar penalidade, créditos ou tempo, e permite nova tentativa ou desmontagem. A tentativa é recusada depois de iniciar desmontagem, com estado/recibos consistentes.
- Replay de `BANK_DROID` confere sequência, faces, pool, símbolos, resultado, confirmação do alojamento e estado derivado. Rejeita ação duplicada, recibos adulterados, escolha posterior à desmontagem e tentativas após sucesso. Integração atômica `GameController.resolveJewelLiveDroid`, `uiBridge.mjs`, `adjudicateJewelOperation('bank_droid')`, adapter e interface PT-BR; três reaberturas de saves. Saves P194 sem `BANK_DROID` permanecem válidos.
- Android **1.0.195** (`versionCode=10195`), bundle offline **93 módulos**, oito arquivos Java incluindo Kokoro/TTS byte-idênticos P194.

## Testes, fontes e limites

- Focal: `tests/uae-p195-jewel-live-g100-droid.test.mjs` (14 casos); **314 arquivos regulares 2869/2869 + seis stress 6/6 = 2875/2875 em 320 arquivos individuais**, sem falhas; resultados globais por arquivo em `evidence/P195/TEST_SUMMARY.json` e `PER_FILE_RESULTS.json`. **Não confundir testes por arquivo com suíte monolítica.** Sintaxe 127 MJS, 22 JSON, 2 XML validada; bundle 93 módulos; Java 8/8 idênticos P194.
- Fonte: cotejo direcionado da seção *Leave No Trace* por texto extraído do PDF via Google Drive. `evidence/P195/PDF_TARGETED_SOURCE_CROSSCHECK.json`. **Não há cotejo integral do livro ou das cinco aventuras.**
- Limites: o dado oposto de Frieza do droid não foi inferido; a nave e o alojamento são fatos confirmados pelo Mestre, não há registro de item de berth, viagem, tempo, novo companheiro automático nem pagamentos. Não há APK/SDK/Gradle/adb/CI BRELGA ou homologação física comprovada.

## Prioridade P196+

1. Consultar fonte oficial e ficha do G-100 para completar a oposição Frieza sem inventar valores. Resolver ferramentas de slicing reais e restraining bolt pelo inventário (não confundir `restraining_device`).
2. Implementar Covering Fire/Tactical Direction com efeitos reais de combate e testes; liquidar banco, consequência da assinatura, risco Naboka, traição e fuga SWE09.
3. Implementar New Meen/Geonosis/Teemo SWE02; cotejar cinco aventuras cena a cena e certificar individualmente os 360 consumidores AoR/EotE/expansões. Não declarar certificação por contagem.
4. Reexecutar focal, regressão por arquivo e seis stress, preservar Java/Kokoro, gerar ZIP Android Studio completo+SHA256+delta+handoff só com avanço real. Salvar Biblioteca e conferir SHA/CRC no readback.
5. GitHub `bra83/braseirobuild` acessível, mas não há workflow/runner/Gradle executado nesta entrega. Não afirmar APK, CI ou homologação sem evidência.

**Manter a tarefa ativa até homologação comprovada.**
