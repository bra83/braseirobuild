# BRASEIRO D&D — ANDROID-001

AGENTE: ANDROID  
BLOCO: ANDROID-001 — FULL ANDROID CHASSIS RECONSTRUCTION + RUN163 SURFACE WIRING  
STATUS: NÃO-PASS FINAL — implementação, regressões mecânicas, estrutura e wiring estático concluídos; compile Gradle real bloqueado pelo executor.

## Base
- RUN163: `BRASEIRO_DND_6.6.2_RUN163_HEROES_FAERUN_MEGA_PTBR_PATCH.zip`
- SHA-256 RUN163: `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`
- Chassis: RUN109 full Android Studio (`be1565cf347c37c46b4c2d33d0053bb25c71d382fdaa8f39752f0397c4d1cad7`) + RUN110 Android/TTS delta (`e0e02a1711012e8b251caab3133570252e7fcec09f077aae3cd1e37a3a19803a`)
- TTS histórico recuperado cirurgicamente de RUN115 (`e89b72bd4c8398a7bf9c29a184a71489b829760f4dafa4cc3b4e841a59927592`)
- GitHub base observada: `main@bad580e389b56fc8d1766adadb6571e02d2a5a04`; `main` não continha RUN163 nem a árvore D&D Android integral.
- Branch: `android/android-001-run163-chassis`

## Arquivos Android alterados
- `android/app/src/main/java/com/braseiro/dnd5e/MainActivity.java`
- `android/app/src/main/java/com/braseiro/dnd5e/CharacterRulesValidator.java` — mudança mínima de gate; revisão ENGINE/MASTER necessária.
- `android/app/src/main/java/com/braseiro/dnd5e/AndroidTtsBackend.java`
- `android/app/src/main/java/com/braseiro/dnd5e/NarrationChunker.java`
- `android/app/src/main/AndroidManifest.xml`

## Implementado
- Chassis Android Studio completo reconstruído por RUN109 → RUN110 Android delta → RUN163 mechanical overlay.
- Package preservado: `com.braseiro.dnd5e`.
- Creation wiring: Artífice usa ID interno `Artificer`; catálogos Eberron/Heroes são expostos; espécies/backgrounds compilados deixam de ser bloqueados pelo validador.
- Runtime wiring: entrypoint RUN163 chama 15 gateways reais de `AppDb` para Eberron e Heroes of Faerûn.
- UI não replica cálculo mecânico: coleta payload manual, valida formato/campos críticos, chama gateway e apresenta resultado.
- `auto_rolls=false` explícito; sem chamada de rolagem automática no bloco RUN163.
- Proteção contra double tap e validação antes do gateway.
- Compatibilidade de saves validada em teste de engines.
- TTS/Kokoro RUN115 restaurado: engines dinâmicas, Sherpa release/debug, vozes reais, chunking Kokoro, `QUEUE_ADD`, avanço por `onDone`, query TTS no Manifest.
- Gemini preservado; nenhuma mudança de contrato mecânico introduzida.
- Nenhum APK/AAB gerado.

## Testes PASS
13 suítes mecânicas RUN141→RUN163, incluindo Eberron e Heroes; RUN115 TTS static; ANDROID001 save compatibility; ANDROID001 surface wiring/input/atomic static; XML parse 10/10; patch reproducibility byte a byte nos 5 arquivos Android alterados.

## Compile Android
Tentado sem tarefas de empacotamento:
`:app:compileDebugJavaWithJavac :app:processDebugResources :app:processDebugMainManifest`.

Bloqueio do executor:
`java.net.UnknownHostException: services.gradle.org` ao obter `gradle-8.11.1-all.zip`. O ambiente também não possui Android SDK/cache Gradle local.

Auditoria `javac` sem SDK: 0 diagnósticos de sintaxe Java nos arquivos modificados; falhas restantes são tipos/imports Android/terceiros indisponíveis. Isso NÃO substitui compile Gradle real.

## Pendências obrigatórias antes de PASS/merge
1. compile-only Gradle + resource processing + manifest merge em executor com rede/SDK, sem assemble/bundle;
2. creation save/reopen em dispositivo/emulador para Eberron/Heroes/Forgotten/Arcana/Runeterra;
3. TTS físico Samsung/Google/Kokoro;
4. Gemini failure instrumentation sem mutação de save/recurso;
5. QA de cancelamento, input parcial, erro, sucesso e double tap.

RISCO: MÉDIO.  
APK GERADO: NÃO.  
BOOK_COMPLETE: false.

RECOMENDAÇÃO PARA O MASTER: não marcar ANDROID-001 como PASS nem integrar `main` ainda. Executar os gates Gradle/device sobre o mesmo pacote; se verdes, integrar preservando RUN163.
