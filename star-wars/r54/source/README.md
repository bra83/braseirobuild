# ATUALIZAÇÃO CANÔNICA P102 — 28/09/2026

> **Este cabeçalho substitui baselines antigas citadas abaixo.** A autoridade atual é `RETOMADA_ATUAL.md` + `HANDOFF_STAR_WARS_EDGE_R54_P102_2026-09-28.md` + `MATRIZ_COBERTURA_MECANICA_REAL_STAR_WARS_P102.md`.
>
> Baseline: **1.0.102 / P102** · regressão **1141/1141 PASS** · Android/Gradle **não executado** · não usar GitHub para corrigir · não recriar projeto.

# ATUALIZAÇÃO CANÔNICA P100 — 27/09/2026

> **Este cabeçalho substitui baselines antigas citadas abaixo.** A autoridade atual é `RETOMADA_ATUAL.md` + `HANDOFF_STAR_WARS_EDGE_R54_P100_2026-09-27.md` + `MATRIZ_COBERTURA_MECANICA_REAL_STAR_WARS_P100.md`.
>
> Baseline: **1.0.100 / P100** · regressão **1124/1124 PASS** · Android/Gradle **não executado** · não usar GitHub para corrigir · não recriar projeto.

# STAR WARS — EDGE OF THE GALAXY — Android Studio R54 P20 MASTER LONG NARRATION CLOSURE

Projeto Android Studio mobile-first do VTT, com shell R46 em 415×915 e Motor Barbara local. O modo padrão continua **Edge of the Empire (`EOTE_CORE`) + Obligation**, com seleção explícita de **EOTE / AOR / MIXED**. Duty de criação e a estrutura Duty/Contribution usam somente as autoridades AoR recuperadas/auditadas; comportamento de runtime sem página primária preservada fica marcado como política local testada.

## Abrir no Android Studio

1. Extraia o ZIP final inteiro.
2. Abra a pasta raiz que contém `settings.gradle.kts`.
3. Use JDK 17.
4. Instale/seleciona Android SDK 36.
5. Aguarde o primeiro sync. `gradlew`/`gradlew.bat` delegam ao `gradle-wrapper.jar`; URL e SHA-256 da distribuição Gradle 8.13 ficam centralizados em `gradle-wrapper.properties`.
6. Execute o módulo `app` em Android 8.0 (API 26) ou superior.

Não é necessário GitHub, npm, servidor web ou build separado da interface. A shell, React runtime, Motor Barbara e assets usados pelo app ficam em `app/src/main/assets/vtt/`.

## Primeiro uso

- O app cria explicitamente a campanha local canônica no primeiro boot, antes de montar a shell.
- Em **PERSONAGEM**, abra o fluxo de criação e crie uma ficha pelas regras auditadas ou importe uma ficha `.json`.
- Em **AJUSTES → Gemini / provedor**, salve sua chave Gemini para habilitar narração automática e geração opcional de imagem de cena. A chave usa Android Keystore; o fallback de reinício também é AES-GCM protegido, e valores plaintext legados são migrados e removidos após verificação.
- Sem chave/rede, o Motor Barbara local continua preservando campanha, regras, intenções, contexto, fichas, snapshots, mundo e resolução mecânica; ele não inventa prosa remota.
- PDFs podem ser importados pela Biblioteca/RAG local.

## Arquitetura congelada

Navegação principal: **CENA / PERSONAGEM / ITENS / NAVE / REGISTROS**. Combate pessoal e espacial são estados de CENA. Mapa e Ajustes são utilidades. A UI não aplica mecânica por conta própria; mutações passam pelo Motor Barbara.

## Recursos fechados em 1.0.0

- shell visual R46 415×915;
- Motor Barbara com estado canônico e validação de propostas;
- dados narrativos FFG/Edge e resolução de perícias;
- combate pessoal e veicular/espacial;
- NPCs persistentes, memória e mundo vivo;
- situação, participantes, ações sugeridas e teste sugerido ligados à CENA real;
- criação manual e aleatória de personagem;
- inventário e aquisição mediada pelo Mestre;
- save/load de ficha JSON, backup de campanha e snapshots;
- importação/indexação de PDF e RAG;
- Gemini nativo para Mestre e imagem de cena;
- contrato de narração longa Barbara/Gemini alinhado em até 12.000 caracteres, compatível com o pipeline TTS;
- TTS PT-BR somente da narração visível;
- acessibilidade e preferências locais;
- biblioteca visual V3.8 congelada com 369 assets validados.

O conteúdo inicial é deliberadamente um **sandbox aberto**: regras, complementos, catálogo, mundo vivo e infraestrutura de encontros. Nenhuma aventura publicada da pasta `Aventuras` do Drive foi incorporada como campanha ou roteiro. A Biblioteca/RAG mantém o ponto de extensão para ativar um pacote futuro explicitamente; fontes desativadas não chegam ao Mestre. Consulte `SANDBOX_CONTENT_SCOPE.md`.

## Validação

Estado reproduzível desta rodada: **783/783 testes regulares PASS + 6/6 stress PASS = 789/789**. O verificador final passou com **103 JS/ESM, 5 Java, 40 JSON, 2 XML, 0 secrets e 0 APK/AAB**. Catálogo atual: **272 entradas pessoais ativas + 25 veículos/naves**. Neste host a distribuição Gradle 8.13 não está em cache e `services.gradle.org` não resolve, então o build não chegou à resolução do projeto. Nenhum APK/AAB foi gerado ou incluído. Veja `R54_P20_MASTER_LONG_NARRATION_CLOSURE_REPORT.md`, `SANDBOX_CONTENT_SCOPE.md`, `RETOMADA_ATUAL.md` e `validation/LATEST_SOURCE_VALIDATION.txt`.

### Integridade do Gradle Wrapper
O pacote final inclui `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew` e `gradlew.bat`, além dos arquivos Gradle Kotlin DSL do projeto.
