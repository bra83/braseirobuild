# ENGINE-002 — CODE DELIVERY ANCHOR

AGENTE: ENGINE
BLOCO: ENGINE-002
BASE_BRANCH: dnd/master-run164-integration-candidate
BASE_COMMIT: b8e59f6c0a8d35bbc93865830359afecd0642d10
BRANCH: dnd/engine-002-qa001-corrections
PATCH_SHA256: 323892809682860d480cbc39f82786f53eb91d2c3ab51b1f840e329b9629f2e1

## Escopo corrigido
- B-003A: RUN149 valida resolução manual antes do gasto e aplica idempotência por token.
- B-003B: Grand Finale e Cold-Blooded validam input/cancelamento antes do gasto e rejeitam replay sem novo débito.
- M-001: actions Heroes desconhecidas retornam `acao_nao_suportada` e o AppDb impede sua persistência.

## Arquivos alterados no delta
- android/app/src/main/java/com/braseiro/dnd5e/Run149ForgottenSecretsSpellEngine.java
  SHA-256: 600479923f41f578f56f89fe0993daee503d91dd2a2f0484227770883ef71401
- android/app/src/main/java/com/braseiro/dnd5e/Run143ForgottenSecretsEngine.java
  SHA-256: a148ab937e1f719850ed05f875b5ff7292c1b296033d32fb3358bc44f3e603a5
- android/app/src/main/java/com/braseiro/dnd5e/ForgottenRealmsHeroesEngine.java
  SHA-256: 484046c8b6992f566a28d9e40b3e6c4736cbd7c09538d57336c9194538f3de30
- android/app/src/main/java/com/braseiro/dnd5e/AppDb.java
  SHA-256: 590a1b2051e6434e473e3a7c3c9cf3170435eaaa3143a98dda4fd2469d8ef886
- tools/test_engine002_run149_atomicity.sh
  SHA-256: b4cd9ba3ed1c87befd32870dac77f28f4fce52e58d6b2fb78404ca8be4b99b18
- tools/test_engine002_run143_atomicity.sh
  SHA-256: 0910104bb9dc9e26a8e209afbf656130fff541edc8172944db325c587eb3f261
- tools/test_engine002_heroes_strict_dispatch.sh
  SHA-256: 2a0ac39ee7b7f42ede3f827146f2c5fdfdbd394bb18fb698112e35345025a7c5

## Testes
19/19 PASS:
- 13/13 RUN141→RUN163
- 3/3 GAMEPLAY-001
- 3/3 ENGINE-002 adversariais

RUN149 adversarial: 8→8→8→8→7→7→6.
RUN143 Grand Finale: 1→1→1→1→1→0→0.
RUN143 Cold-Blooded: 1→1→1→1→0→0.
Heroes unknown action: Banneret, Harper Agent e Bright Fungal Cloak bloqueados; manual action conhecida permanece pending.

## Restrições preservadas
Nenhuma alteração em MainActivity.java, WorldStateEngine.java, GameplaySandboxRuntimeEngine.java, Gradle, Manifest, TTS/Kokoro, Android UI ou Adventure Engine.
Nenhum APK/AAB.
BOOK_COMPLETE=false.
