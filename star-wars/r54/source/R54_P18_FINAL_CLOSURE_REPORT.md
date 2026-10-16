# R54 P18 — FINAL CLOSURE

Data: 2026-09-18
Base preservada: **R54 P17 TTS FAILOVER GREEN**. Nenhum retorno a P8/P9 e nenhum fallback fuzzy/NPC fixo reintroduzido.

## Correção reproduzível desta rodada

A tela de validação da shell ainda continha uma mensagem legada dizendo que **OCR, IA/Gemini e TTS continuavam sem homologação**, embora Barbara/Gemini/TTS/fallbacks já estivessem integrados e cobertos por testes. Isso era uma inconsistência real entre o estado técnico atual e o texto exibido ao usuário.

Foram atualizados, em paridade:
- `shell-r46-run87/Templates.tsx`;
- `app/src/main/assets/vtt/r46/r46-shell.bundle.js` efetivamente embarcado;
- novo gate `tests/validation-capabilities-copy-p18.test.mjs` para impedir retorno da mensagem antiga.

O novo texto separa corretamente **integração de código validada** do que ainda exige ambiente real: build/instalação Android, áudio/latência Gemini/Charon, OCR e bridges nativos em aparelho.

## Validação

- gate focal TTS/UI: **11/11 PASS**;
- regressão regular: **779/779 PASS**, 0 fail;
- stress executado isoladamente: **6/6 PASS**, 0 fail, 0 skipped;
- total das suítes executadas: **785/785 PASS**;
- `r46-shell.bundle.js`: `node --check` PASS;
- Java syntax gate: PASS (5 arquivos; símbolos Android ficam para compilador/SDK);
- JSON: **40/40 parse PASS**;
- XML Android: **2/2 parse PASS**;
- secret scan com os mesmos padrões canônicos do verificador: **0 hits**.

## Stress — sem mascarar timeout

O runner agregado excedeu a janela do host. Isso não foi registrado como falha do projeto nem convertido em PASS. As seis suítes foram então executadas **uma a uma**, todas verdes. O log está em `R54_P18_STRESS.txt`.

## Build Android

Foi feita tentativa real de `./gradlew --offline assembleDebug`. O Gradle Wrapper está presente, mas não há distribuição 8.13 em cache e o host não conseguiu resolver `services.gradle.org` (`UnknownHostException`). Portanto:

- **não há erro de compilação reproduzido**;
- **não foi gerado APK/AAB**;
- compilação/instalação ainda depende de ambiente com Gradle/Android SDK/rede disponíveis.

## Escopo preservado

Nenhuma mecânica Star Wars, Mundo Vivo, Barbara, memória de NPC, facções, viagem, economia, clima, TTS multibloco ou failover P17 foi alterada nesta rodada. A diferença funcional em relação ao P17 é apenas a correção do estado exibido pela UI e seu gate permanente.

## Dependências externas restantes

Somente validações que exigem ambiente real:
- áudio e latência Charon/Gemini com chave/rede reais;
- continuidade auditiva entre blocos no Android físico;
- OCR/bridges nativos em aparelho;
- build, instalação e execução Android quando SDK/Gradle estiverem disponíveis.

Não há outra falha reproduzível conhecida nesta base após esta rodada.
