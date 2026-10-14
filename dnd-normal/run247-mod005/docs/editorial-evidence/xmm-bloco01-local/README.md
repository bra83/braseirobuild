# BLOCO 1 — entrega parcial verificável

Implementação concluída nesta execução: **Slowing Breath** de Adult Copper Dragon,
Ancient Copper Dragon, Copper Dragon Wyrmling e Young Copper Dragon. Código real
em `XmmSaveEffectLedger`, runtime, AppDb, seleção de ações e interface PT-BR.

O Mestre fornece os alvos do cone e cada total de salvaguarda. A aplicação em lote
usa uma ação e uma transação SQLite: qualquer alvo inválido reverte todo o lote.
Sucesso consome a ação sem aplicar efeito; fracasso proíbe reações, divide o
deslocamento por dois e permite ação ou ação bônus. O efeito termina no fim do
próximo turno do alvo. Não cria uma condição “slowed”, dano, nova salvaguarda ou
recarga. Efeitos sobrepostos não dividem o deslocamento novamente. Movimento já
gasto é preservado. Os recursos, proprietários e recibos são remapeados na
restauração do backup existente, sem mudança de schema ou versão.

A orientação do cone e a inclusão das criaturas são confirmadas pelo Mestre;
o motor valida alcance, posições e caminho físico. Invisibilidade e cegueira não
introduzem uma exigência de visão que esse sopro não possui. Objetos que somente
bloqueiam visão não interrompem seu caminho. Cobertura total física continua
usando o motor tático existente.

## Fonte

Commit XMM: `8c026b807fac21862a309379a0c3228a0683198b`.
Bestiário Git blob: `9a4137b508ef37562ed948894a70b1594b57e85c`.
Livro Git blob: `a21801793df3515d07ef9a56d9febefea1c6f8f7`.
Os arquivos independentes foram obtidos pelo conector GitHub e seus Git blob
SHA-1 foram conferidos pelo comparador. Não foram adicionados PDFs ou textos
completos de blocos ao aplicativo. `source-rule-review.json` registra os 16
contratos, ponteiros e hashes dos trechos usados na conferência.

## Validação efetivamente executada

- 14 testes Java aprovados, com JSON real: fonte instalada, execução de
  salvaguardas, restrições, expiração, sobreposição, serialização, remapeamento,
  replay, idempotência e rejeição de estado inválido.
- 6 testes de contratos contra a fonte independente pinada aprovados.
- 55 testes existentes do comparador aprovados.
- Comparador XMM aprovado: 503 monstros, 36 seções e 4 novos contratos de efeitos
  de salvaguarda. O resultado não certifica automação completa de monstros.
- AppDb e suas dependências compilaram com `javac --release 17` contra o SDK
  Android 35. BuildConfig temporário, somente em `/tmp`, foi usado para a
  verificação de tipos; isso não é um build Gradle ou um APK.
- `git diff --check` aprovado.

O executor independente pode ser reproduzido com
`bash tools/run_xmm_save_effect_contracts.sh DIRETORIO_DAS_FONTES_JSON_JAVA`.
Ele usa as pequenas fixtures de anotação/assert de JUnit já presentes em tools,
mas exige uma implementação real de org.json; não usa a fixture JSON de snapshots.
Para os testes de fonte:
`XMM_SOURCE_DIR=DIRETORIO_XMM python3 -m unittest discover -s tools -p 'test_xmm_save_effect_source.py'`.

## Falhas reais e limites

Foram adicionados **8 testes SQLite/Robolectric**, cobrindo as quatro variantes,
sucesso, economia de ações, lote, rollback, entradas inválidas, reabertura,
replay e exportação/importação de backup. **Não foram executados**: o wrapper
precisa baixar Gradle 8.11.1 e a criação do socket é proibida neste ambiente.
O comando final e a exceção real estão em `gradle-focused-failure.log`.
Uma tentativa inicial também encontrou `gradlew` sem permissão de execução e
outra encontrou a pasta de lock de Gradle fora das raízes graváveis; usar
`bash gradlew` e `GRADLE_USER_HOME=/tmp/xmm-gradle` resolveu esses dois obstáculos,
mas não a restrição de rede.

A tentativa de compilar MainActivity com apenas o SDK falhou por dependências
externas ausentes (AndroidX Work, ML Kit e Chaquopy); o log real foi preservado
em `mainactivity-dependency-failure.log`. A interface modificada não tem
validação de build Android completo nesta execução.

Uma primeira cópia temporária do blob incluiu um newline extra e foi corretamente
rejeitada pelo comparador. A cópia foi corrigida em `/tmp`, e ambos os hashes
pinados foram verificados antes da aprovação final.

**Sleep Breath, Paralyzing Breath e Weakening Breath continuam manuais nas 12
variantes restantes. A fonte foi conferida e é suficiente; falta implementação
completa de seus fluxos, não informação de regra.** Sono e Paralisia exigem uma
segunda salvaguarda antes de aplicar a condição final. Sono também exige dano,
ação para despertar e duração distinta por idade. Enfraquecimento exige
desvantagem nos testes de Força, redução de cada rolagem de dano com dado
informado pelo jogador/Mestre, recuperação e exclusão de alvos já afetados.
Essas famílias não foram habilitadas parcialmente. O BLOCO 1 inteiro e seu
executor genérico para todas as famílias **não estão concluídos**.

Nenhuma outra criatura foi adicionada a esse executor. Mystara mantém o bloqueio
manual preexistente desse sopro. Não foram alterados suplementos, outros VTTs,
workflows, PDFs, versões, assinatura ou checkpoints. Sem commit, push ou merge.
