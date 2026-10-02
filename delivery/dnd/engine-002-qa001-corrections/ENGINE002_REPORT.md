# BRASEIRO D&D — ENGINE002 REPORT

AGENTE: ENGINE
BLOCO: ENGINE-002
BASE_BRANCH: `dnd/master-run164-integration-candidate`
BASE_COMMIT: `b8e59f6c0a8d35bbc93865830359afecd0642d10`
BRANCH/COMMIT: `dnd/engine-002-qa001-corrections` / código ancorado em `7ec0d9956d87067bcad4a6aa705cc68cd0243fae`; o HEAD final desta branch contém os metadados de publicação.
CHECKPOINT DRIVE: https://drive.google.com/file/d/1giSedFL2dkjfXE51ngs84B95RRc96kbx/view?usp=drivesdk — ID `1giSedFL2dkjfXE51ngs84B95RRc96kbx`
SHA-256: `fb5975ecf25b33270b5c3601f2bec9a8d43fca8c9f4c14e88af7e509756671e3`

ARQUIVOS ALTERADOS:
- android/app/src/main/java/com/braseiro/dnd5e/Run149ForgottenSecretsSpellEngine.java
- android/app/src/main/java/com/braseiro/dnd5e/Run143ForgottenSecretsEngine.java
- android/app/src/main/java/com/braseiro/dnd5e/ForgottenRealmsHeroesEngine.java
- android/app/src/main/java/com/braseiro/dnd5e/AppDb.java
- tools/test_engine002_run149_atomicity.sh
- tools/test_engine002_run143_atomicity.sh
- tools/test_engine002_heroes_strict_dispatch.sh

B-003A CORRIGIDO: SIM. RUN149 valida resolução manual antes do débito; pending/cancelamento/input inválido não gastam; retomada válida gasta exatamente uma vez; replay com mesmo token não gasta novamente; pending legado já debitado não sofre segundo débito.
B-003B CORRIGIDO: SIM. Grand Finale valida modo/targets/save manual antes do gasto; Cold-Blooded exige reptile_only_confirmed=true antes do gasto; cancelamento e replay não debitam.
M-001 CORRIGIDO: SIM. Action Heroes desconhecida retorna resolved=false/status=acao_nao_suportada a partir do estado original; AppDb rejeita persistência de acao_nao_suportada/blocked/error e preserva pending conhecido.

TESTES ADVERSARIAIS:
- RUN149: 8→8→8→8→7→7→6 — PASS.
- Grand Finale: sem targets, target malformado, save manual ausente e cancelamento mantêm 1; válida vai a 0; replay mantém 0 — PASS.
- Cold-Blooded: ausência/false/cancelamento mantêm 1; válida vai a 0; replay mantém 0 — PASS.
- Heroes: Banneret, Harper Agent e Bright Fungal Cloak rejeitam acao_inventada_qa; group_recovery real permanece pending — PASS.

REGRESSÕES 13 RUNs: 13/13 PASS.
GAMEPLAY-001 REGRESSION: 3/3 PASS.
RESULTADOS: 19/19 scripts PASS; git apply --check do patch sobre reconstrução limpa da base = PASS; nenhum APK/AAB.

CONFLITOS:
- AppDb.java é o único arquivo compartilhado alterado; a mudança é exclusivamente a guarda de persistência M-001.
- MainActivity.java, WorldStateEngine.java, GameplaySandboxRuntimeEngine.java, Gradle, Manifest, TTS/Kokoro, Android UI e Adventure Engine não foram alterados.

PENDÊNCIAS:
- Findings QA-001 fora do ownership ENGINE-002 continuam abertos para seus blocos: B-001, B-002, B-004 e N-001.
- Reexecutar QA global após integração dos blocos corretivos.

SOURCE_BLOCKED PRESERVADOS:
- Eberron Bestiary 20/27 cartões numéricos — BLOCKED_SOURCE.
- Heroes of Faerûn source-fidelity — PARTIAL.
- Runeterra Pistoleiro/Armeiro — BLOCKED_SOURCE_RULE_TEXT.
- Grung / One Grung Above — BLOCKED_SOURCE.
- Xamã/Ritualista — BLOCKED_SOURCE_RULE_TEXT.

BOOK_COMPLETE: false
APK/AAB: NÃO
RISCO: BAIXO-MÉDIO; AppDb.java exige reconciliação consciente pelo MASTER.
RECOMENDAÇÃO PARA O MASTER: aplicar somente sobre a reconstrução RUN164 candidate do QA001_RELEASE.md, verificar o patch SHA, rodar 19/19 testes e encaminhar novo candidato ao QA; não fazer merge automático na main.
