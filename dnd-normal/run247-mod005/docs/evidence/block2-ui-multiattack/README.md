# Bloco 2 — integração de conteúdo, sem homologação

Foram auditados os diffs limitados a esta app, usando os merge-bases reais de HEAD:

- PR39: `893b6aee23a6b74afebf10b3de14585d6d33ea2f` → `origin/integration/dnd-core-ready-20261015`. Integrados os hunks da MainActivity e as quatro suítes Robolectric, preservando os métodos XMM posteriores do HEAD.
- PR40: `b2c0eee6cb061f9a219ec0d2da6c1ff32dffbc26` → `origin/codex/dnd-xmm-familias-20261015`. Integrados os hunks do índice Multiattack, três testes e verificador de fonte/seus testes. O inventário antigo permanece porque previews não comprovam execução. Evidências históricas das refs não foram tratadas como evidência deste trabalho.

Nenhuma PR original foi mesclada ou alterada. Nenhum commit, push ou mudança de branch foi executado.

## Código conectado

A seleção de composições e componentes chega à sobrecarga de `applyCompiledStatblockMultiattack`, que revalida o índice dentro da transação existente. Custos, rolagens, dano tipado, riders, mapa, tokens e recibos continuam no executor original. O recibo inclui seleção e identidade da fonte. Componentes não físicos recebem um bloqueio específico; o código não executa apenas parte da composição.

Ataques e saves exibem confirmação e recibo/erro tipado. Saves da UI exigem geometria compilada verificável; áreas sem seletor de origem/direção são bloqueadas com `source_save_geometry_not_compiled`. As prévias de efeitos periódicos, condições e efeitos a 0 PV têm escolhas explícitas e não alteram fichas. Aplicação desses efeitos permanece bloqueada por `effect_lifecycle_host_executor_not_installed`.

O inventário original tem 70 linhas: 39 possuem composição física candidata e 31 não têm plano estruturado suficiente. Isso não certifica execução de nenhuma linha. `original-70-admission.json` lista cada ponteiro e variante, inclusive bloqueios `spell`, `use` e `action`. Rakshasa e Sphinx of Secrets usam ponteiros de inventário terminados em `/entries/0`, filhos do ponteiro `/entries` do plano; ambos são registrados separadamente.

## Validação deste worktree

- PASS: 65 testes Python do verificador de fonte (fixtures; não compara o blob upstream real).
- PASS: 10 gates JVM isolados dos lotes 04–13, 62 ponteiros.
- PASS: gate JVM de admissão, 441 assertions; mantém `NOT_INTEGRATED` para aquelas famílias no host.
- PASS: compilação do adaptador Multiattack e dependências com `javac` e `android.jar`; análise sintática Java dos arquivos alterados; `git diff --check`.
- NÃO EXECUTADO: Robolectric. O wrapper não consegue baixar Gradle 8.11.1 (`SocketException: Operation not permitted`). Gradle instalado 9.7.1 falha na inicialização do lock por indisponibilidade de socket/IP. Os logs estão neste diretório.
- NÃO CONCLUÍDA: compilação completa de MainActivity por javac avulso, por ausência das dependências AndroidX, ML Kit, PDFBox, Chaquopy e BuildConfig gerado. Análise sintática não substitui compilação da app.

As quatro suítes da PR39 estão em `app/src/test`. Foram adaptados testes da confirmação de ataque e acrescentados cancelamento da confirmação/prévia sem mutação. `XmmMultiattackHostRobolectricTest` cobre SQLite, ação única, rollback, componente não suportado, persistência e replay. `Xmm70MultiattackHostPlanTest` cobre o escopo original. Esses testes JUnit novos e as suítes Robolectric ainda NÃO foram executados.

Permanecem sem regressão Android neste estado: criação de personagem, exploração, ataque/save/multiattack, PV, recursos, turnos, restauração, backup, tokens, condições, modos de companheiros, Barbara, RAG e TTS PT-BR. Ciclos periódicos/24h e composições com magia/uso não estão implementados como execução atômica. Não foi gerado APK nem executado BRELGA. A UI não está homologada; executar Robolectric/BRELGA no SHA final produzido pelo shell.
