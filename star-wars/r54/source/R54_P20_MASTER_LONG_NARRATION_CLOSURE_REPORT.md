# R54 P20 — MASTER LONG NARRATION + BUNDLE BUILDER CLOSURE

Data: 2026-09-18

## A. Baseline

- Entrada: `STAR_WARS_EDGE_OF_THE_GALAXY_ANDROID_STUDIO_R54_P19_SANDBOX_SCOPE_CLOSURE.zip`.
- SHA-256 conferido: `8e90b9738033ac260cb5d8b23cba32913bb1c0b91b7edeabee3abdbceda38538`.
- Projeto preservado; nenhuma recriação, retorno a P18/P8, fuzzy binding ou NPC fixo foi introduzido.
- Shell R46, regras Star Wars, Mundo Vivo, TTS multibloco e sandbox P19 preservados.

## B. Bugs reproduzíveis corrigidos

### 1. Contrato de narração Barbara/Gemini incompatível

O host nativo instruía o Gemini a produzir **900–1400 palavras** em cenas principais, enquanto `validateMasterProposal` recusava qualquer `narration` acima de **5.000 caracteres**. Uma resposta que obedecesse ao prompt podia portanto ser rejeitada pelo próprio Motor Barbara.

Correção:

- novo teto canônico: **12.000 caracteres**;
- `proposal.mjs` e `GeminiBridge.java` usam o mesmo limite;
- o bundle offline embarcado foi reconstruído a partir dos módulos canônicos;
- o limite continua estrito: 12.001 caracteres são rejeitados;
- o teto coincide com o limite já aceito pelo pipeline Gemini TTS (`speakTts`).

### 2. Builder duplicado da raiz apontava para o diretório errado

`build-ui-bridge-bundle.mjs` na raiz calculava o projeto como o diretório pai e falhava com `Missing local module js/core/uiBridge.mjs`. O builder em `tools/` funcionava.

Correção:

- ambos os builders agora resolvem a raiz canônica de forma segura;
- teste P20 executa os dois builders e exige bundle byte-a-byte idêntico.

## C. Validação

- Regressão regular final: **783/783 PASS**, 0 fail.
- Stress: **6/6 PASS**, executado arquivo por arquivo sem remover asserts:
  1. `stress-33-aftermath-r20.test.mjs` — PASS;
  2. `stress-33-faction-plan-r19.test.mjs` — PASS;
  3. `stress-33-sessions-r15.test.mjs` — PASS;
  4. `stress-faction-plans-r19.test.mjs` — PASS;
  5. `stress-factions-r18.test.mjs` — PASS;
  6. `stress-persistent-adversary-r17.test.mjs` — PASS.
- Total validado: **789/789 PASS**.
- Gate P20 focal: narração longa + sincronismo host/Barbara + dois builders — PASS.
- JS/ESM syntax: **103/103 PASS**.
- Java syntax/declaration gate: **5/5 PASS** (`unresolved_android_symbols=479`, esperado sem SDK/classpath Android).
- JSON: **40/40 PASS**.
- XML: **2/2 PASS**.
- Secret scan: **0 hits**.
- APK/AAB no source: **0**.
- Gradle Wrapper: **PASS**, SHA-256 `14013df926465d9e70158430a0b560b1aa83da0f98732da7882015e6bdff9c88`.

### Observação sobre o verificador agregado

Neste host, duas suítes stress pesadas em sequência sob o mesmo processo supervisor entram em contenção e excedem a janela do executor, embora cada arquivo finalize verde quando executado isoladamente. Por isso **não foi registrado um falso PASS do verificador agregado**. A evidência canônica deste P20 é a regressão regular completa e as seis suítes stress gravadas individualmente no pacote.

## D. Android/build

Tentativa executada:

`./gradlew :app:assembleDebug --offline --stacktrace`

Resultado neste host:

- `gradle-wrapper.jar` inicia corretamente;
- a distribuição Gradle 8.13 não existe no cache deste ambiente;
- o wrapper tenta `services.gradle.org` e falha com `java.net.UnknownHostException`;
- a compilação não chega à resolução do projeto/AGP neste host;
- nenhum APK/AAB foi gerado.

Isso é diferente da execução P19 em outro host/cache, que chegou até `kotlin-reflect:2.0.21`. Ambas as evidências apontam para dependência externa de cache/rede, não para uma falha de código convertida em PASS.

## E. O que permanece dependente de ambiente real

- build Android completo em host com Gradle 8.13 e dependências Maven disponíveis;
- instalação/smoke em aparelho ou emulador;
- chave/rede Gemini real;
- latência e continuidade auditiva Charon/Gemini em dispositivo;
- OCR e bridges nativos em Android real.

## F. Escopo preservado

- Sandbox aberto P19 permanece a autoridade de conteúdo.
- Aventuras publicadas do Drive continuam fora do corpus inicial.
- 272 entradas pessoais ativas, 25 veículos/naves e 82 localidades permanecem sem alteração de catálogo nesta rodada.
- Nenhum mapa/esquema foi promovido semanticamente nesta rodada.
