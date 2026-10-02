# ANDROID-001 — relatório de execução

AGENTE: ANDROID  
BLOCO: ANDROID-001 — FULL ANDROID CHASSIS RECONSTRUCTION + RUN163 SURFACE WIRING  
BASE MECÂNICA: RUN163 `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`  
BASE CHASSIS: RUN109 recomposto `be1565cf347c37c46b4c2d33d0053bb25c71d382fdaa8f39752f0397c4d1cad7` + RUN110 `e0e02a1711012e8b251caab3133570252e7fcec09f077aae3cd1e37a3a19803a`  
BRANCH: `android/android-001-run163-chassis`

## Reconstrução comprovada

A árvore Android Studio foi recomposta localmente a partir das quatro partes canônicas RUN109 do Drive, com verificação de SHA por parte e SHA do ZIP recomposto. O overlay RUN110 e depois o overlay mecânico RUN163 foram aplicados nessa ordem. A árvore resultante possui Gradle wrapper, settings/build files, Manifest, resources, assets e source sets, preservando `com.braseiro.dnd5e`.

O repositório `main` não contém essa baseline D&D completa. Por isso o chassis completo não foi substituído silenciosamente no GitHub. A árvore reconstruída contém um asset histórico de aproximadamente 100 MB, acima do limite do Contents API; a entrega do código Android específico deste bloco é mantida como overlay/reconstrução, sem remover os assets históricos.

## Alterações Android do bloco

- `MainActivity.java`
  - criação expõe Artífice com ID interno `Artificer`;
  - espécies/antecedentes/subclasses de RUN163 entram na seleção;
  - superfície PT-BR de runtime adicionada aos personagens;
  - 17 rotas UI→AppDb para Eberron e Heroes of Faerûn;
  - entrada manual por payload JSON/número/ação;
  - proteção contra toque duplo;
  - cancelamento não chama gateway;
  - UI não calcula efeitos nem consome recursos.
- `CharacterRulesValidator.java`
  - aceita Artífice e opções compiladas Eberron/Heroes sem abrir fallback para opção desconhecida.
- `NarrationChunker.java`
  - perfil Kokoro 250–650 caracteres, alvo ~450 e cortes naturais.
- `AndroidTtsBackend.java`
  - descoberta dinâmica de engines;
  - Sherpa release/debug;
  - rótulo `Braseiro Kokoro PT-BR · neural`;
  - vozes reais via `TextToSpeech.getVoices()`;
  - persistência por engine + `Voice.getName()`.
- `AndroidManifest.xml`
  - query de `android.intent.action.TTS_SERVICE`.

`AppDb.java` e `MasterOrchestrator.java` não receberam alteração Android neste bloco; os gateways RUN163 existentes foram preservados.

## Testes

PASS:
- SHA RUN109/RUN110/RUN163;
- ZIP RUN109 íntegro;
- sintaxe Java: 252/252;
- parse XML do Manifest;
- gate Android específico: 17 gateways presentes, sem auto-roll na nova UI, proteção contra double tap e contrato TTS;
- Kokoro chunker: múltiplos chunks, máximo 650;
- RUN141 compact;
- RUN142 reconciliation;
- RUN143 Arcana;
- RUN143 Forgotten Secrets;
- RUN144 Arcana systems;
- RUN145 Arcana bestiary;
- RUN145 Arcana items;
- RUN146 reconciliation;
- RUN147 evolving items;
- RUN148 reconciliation;
- RUN149 Forgotten Secrets spells;
- RUN162 Eberron mega runtime;
- RUN163 Heroes of Faerûn mega runtime.

BLOQUEADO PELO AMBIENTE:
- `:app:compileDebugJavaWithJavac`, `:app:processDebugResources` e `:app:processDebugMainManifest` foram solicitadas sem assemble/bundle, porém o Gradle wrapper não conseguiu baixar Gradle 8.11.1 porque o executor não resolve `services.gradle.org`. O Javac semântico Android/manifest merge não chegou a iniciar.

APK GERADO: NÃO. Nenhum `.apk` ou `.aab` existe na árvore de trabalho.

## Estado de conclusão

ANDROID-001 **não deve ser marcado PASS final ainda**, exclusivamente porque o gate Gradle/Android SDK não pôde ser executado neste ambiente e a árvore integral histórica ainda precisa ser materializada em um armazenamento/repositório que aceite o asset >100 MB. A reconciliação mecânica e os gates estáticos/mecânicos estão verdes.

## Dependências

ENGINE: nenhuma alteração de regra solicitada; os gateways mecânicos RUN163 foram usados como autoridade.  
GAMEPLAY: a superfície expõe os runtimes existentes; fluxos narrativos específicos continuam ownership GAMEPLAY.  
MASTER: executar o gate Gradle em runner com rede/cache e decidir o armazenamento canônico do chassis integral antes de promover ANDROID-001 para PASS.
