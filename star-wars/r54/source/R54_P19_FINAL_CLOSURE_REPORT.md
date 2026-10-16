# R54 P19 — FINAL CLOSURE REPORT

Data: 2026-09-18

## A. Baseline

- Entrada: `STAR_WARS_EDGE_OF_THE_GALAXY_ANDROID_STUDIO_R54_P18_FINAL_CLOSURE.zip`.
- SHA-256 conferido: `1569e50c294599e7d953b772b0a33eefcd5f8960cc06a066a36dcd4f645e324f`.
- Projeto preservado; nenhum projeto novo foi criado e a shell R46 não foi redesenhada.

## B. Correções reproduzíveis

1. `tools/run-node-suite.py`: fixa reporter TAP e normaliza o filtro de stress para Windows/Unix. Antes, Node 24 produzia resumo visual não parseável e o Windows incluía stress na rodada regular.
2. `tests/gradle-wrapper-bootstrap.test.mjs`: usa `gradlew.bat`/`gradle.bat` no Windows e preserva a validação de checksum/argumentos.
3. `tools/verify-final-source.mjs`: usa runtimes nativos por plataforma para Python, XML, descoberta de ferramentas e SDK/ADB.
4. `GeminiBridge.java`: adiciona fallback persistente AES-GCM em SharedPreferences com alias Android Keystore independente; migra `apiKey` legado e remove-o após verificar a cópia criptografada.
5. `settings.gradle.kts`: mapeia o plugin marker Android ao módulo AGP versionado, permitindo resolução offline quando o módulo está no cache.

## C. Gates de código

- Regular: **780/780 PASS**, 0 fail.
- Stress: **6/6 PASS**, 0 skipped.
- Total executado: **786/786 PASS**.
- Verificador: JS/ESM **103**, Java **5**, JSON **40/40**, XML **2/2**, secret scan **0**, APK/AAB **0**, Gradle Wrapper **PASS**.
- Bundle shell/engine: estrutura preservada; foram aplicadas apenas as alterações de nomenclatura/filtragem necessárias para o fluxo sandbox no bridge e na shell.

## D. Android/build

- JDK: Android Studio JBR 17.
- Gradle: wrapper 8.13, distribuição local encontrada e executada.
- SDK: Android SDK local com platform 36 e build-tools 36.0.0.
- ADB: localizado em `platform-tools`.
- Resultado: build iniciado e chegou à resolução AGP, mas não concluiu.
- Causa externa: `org.jetbrains.kotlin:kotlin-reflect:2.0.21` não está no cache; a tentativa online falhou com `Permission denied: getsockopt` no processo Gradle.
- APK/AAB: não gerado. Instalação e smoke Android não validados.

## E. Gemini/TTS

- Contratos de Barbara/Gemini/TTS multibloco e fallback permanecem cobertos pela regressão estática.
- Nenhuma chave real foi usada ou registrada.
- Latência auditiva Charon/Gemini, quota/rede e stop/restart em dispositivo permanecem dependentes de chave/rede/aparelho.

## F. Mundo Vivo e conteúdo

- Mundo Vivo, Barbara, regras Star Wars, facções, aftermaths, adversário persistente e catálogos não foram reestruturados.
- Stress preservou ciclos/reaberturas conforme as seis suítes canônicas.
- Contagens atuais mantidas: 272 entradas pessoais ativas, 25 veículos/naves, 82 localidades.
- Escopo corrigido: a entrega é um sandbox de regras e complementos; nenhum PDF ou roteiro da pasta Drive `Aventuras` foi incorporado como campanha.
- A inicialização visual agora é `INICIAR SANDBOX`/`startSandboxSession`; o nome antigo permanece somente como alias interno do controller, sem ser exposto pela UI bridge.
- Fontes desativadas da Biblioteca continuam visíveis para reativação futura, mas são filtradas antes do RAG.
- O contrato de extensão para aventuras futuras permanece em `sandboxOpening`, opportunities/hooks/clocks, Biblioteca/RAG e propostas validadas de Barbara.
- Auditoria do Drive: os livros-base, complementos, imagens e mapas foram confirmados como acervo-fonte; eles não são todos copiados para dentro do APK/projeto como PDFs. O app oferece importação/indexação privada via Biblioteca/PDFBox para ativação deliberada das regras desejadas.
- Auditoria visual P6: 243/243 armas e 29/29 itens/equipamentos possuem binding exato e arquivo resolvível; 82/82 localidades possuem asset de mapa válido. Desses locais, 1 usa mapa dedicado confirmado (`shantipole_base.webp`) e 81 usam esquemas locais jogáveis gerados, rotulados explicitamente como esquemas e não como cartografia oficial.
- A biblioteca visual embarcada continua sendo a V3.8 validada (369 referências, 148 locais e 221 externas); isso não deve ser confundido com "todo arquivo existente no Drive".

## G. Pendências verdadeiras

- Reexecutar build em host com `kotlin-reflect:2.0.21` disponível ou rede Maven funcional.
- Instalar APK em Android/emulador e executar smoke matrix.
- Homologar Gemini/TTS/Charon real, OCR e bridges nativos com chave e dispositivo.

Essas pendências são dependências externas concretas, não falhas convertidas em PASS.
