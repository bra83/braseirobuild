# Frente B XMM — evidência da execução

Data: 2026-10-15

## Isolamento e base

- Repositório: `bra83/braseirobuild`
- Branch: `codex/dnd-xmm-familias-20261015`
- Worktree: `/workspace/braseirobuild-xmm-b`
- SHA-base da branch canônica obtido com `git fetch origin`: `47a2298a6ac75551aca2a8c27175f65f4eb6c319`
- O PR #39 e os arquivos protegidos não foram editados.
- A Frente A ainda estava pendente; não houve merge ou integração na branch canônica.

## Lotes implementados

### B0 — inventário e isolamento

O verificador independente usa o bestiário e livro XMM pinados em `8c026b807fac21862a309379a0c3228a0683198b`. O manifesto `manual-effect-inventory-242.json` tem 242 ponteiros únicos para ações que ainda não são executáveis integralmente. Cada linha contém contrato mecânico, dono do executor, situação, evidência e bloqueio. A contagem de inventário inclui 168 efeitos compilados antes da expansão desta rodada e entradas para Multiattacks sem sequência estruturada; ela não representa 242 executores ausentes.

### B1 — riders e condições

Foi criado `Xmm2025ConditionChoiceEngine` para seis ataques fonte-verificados de Death Slaad, Gray Slaad, Green Slaad, Githzerai Psion, Pixie e Pixie Wonderbringer. O executor exige o d4 ou escolha explícita, respeita o limite de tamanho e os gatilhos de duração. Rejeita fonte, ponteiro, condição, resultado de ataque ou campo de entrada inválidos.

Os resultados são propostas sem aplicação, persistência, rolagem ou mutação. Os seis registros permanecem classificados como `BLOCKED_MISSING_CONTEXT` até a integração do fluxo de combate após o PR #39.

Também foram adicionados previews source-bound para 58 famílias de condição originada por salvaguarda e três ticks de dano recorrente (Pit Fiend, Stirge e Vine Blight). Os previews exigem resultado/contexto explícito, validam ponteiro e blob fonte, recusam valores fora dos limites mecânicos e não aplicam condições/dano, não rolam dados e não persistem estado. O perfil Psychic Drain de Aboleth permanece `SOURCE_REVIEW_REQUIRED` porque a fonte compilada não declara salvaguarda/CD; não foi inferido um valor. Os perfis restantes continuam bloqueados até integração do ciclo de vida/fluxo de combate.

### B2 — Multiattack

O plano fonte-verificado de Sphinx of Secrets admite as três Garras ou a substituição de exatamente uma das três por Curse of the Riddle em qualquer posição. Foram adicionadas sequências reusáveis para Werebear, Wereboar, Wererat, Weretiger, Werewolf, Merrow e Pit Fiend. A sequência exige escolhas explícitas, total de slots exato e ações compiladas fonte-vinculadas. O passo da maldição continua com resolução manual; os planos descrevem a sequência, sem executar efeitos nem consumir recursos.

O índice agora tem 232 planos estruturados entre 306 Multiattacks; 74 ainda não têm sequência estruturada. O comparador confirma 197 sequências simples e verifica separadamente a substituição do Sphinx e as cinco variantes de licantropos. O inventário contabiliza 76 efeitos Multiattack sem executor completo, pois dois planos estruturados ainda incluem efeitos especiais que precisam de resolução manual. O resolvedor permanece um preview com ponteiro exato e lista de campos permitidos.

### B3 — conjuradores e exceções

Não foi introduzido um executor novo de magia nesta execução. O comparador validou novamente os 132 registros de uso de magia, e o inventário mantém efeitos que dependem de concentração, área, múltiplos alvos ou estado de combate como pendentes até haver contrato e integração comprováveis. A aplicação de magia continua sem ser homologada por esta Frente B. Foram adicionados previews puros para Phase Spider e Vampire Familiar quando um acerto reduz o alvo a 0 PV; ambos propõem as condições fonte-declaradas e continuam bloqueados até integração.

### B4 — testes e publicação

- Comparador XMM: aprovado; 503 monstros, 36 seções, 120 recargas, 132 registros de magia, 178 contratos de traits, 59 ações de dano por salvaguarda e 306 Multiattacks analisados.
- Testes Python: 103 aprovados (`PYTHONPATH=tools python3 -m unittest discover -s tools -p 'test_*.py'`).
- O comparador independente passou com 58 perfis save-condition, 3 perfis recurring-damage e 2 perfis zero-HP; `source_field_and_catalog_gate_pass=true`.
- Gradle/Robolectric local: `JAVA_HOME=/workspace/jdk21 ANDROID_HOME=/workspace/android-sdk bash gradlew --offline --console=plain testDebugUnitTest` aprovado; XMLs agregam 2.603 testes, 0 falhas, 0 erros e 0 ignorados em 134 classes. Inclui 5 testes do preview zero-HP.
- Fonte de verificação: `xmm-source-verification.json`; SHA-256: `97d7f6454086ec99193a8046ca045529205c13b8a66c9abe05ff53c382b07bc3`.
- O comparador continua declarando `full_monster_combat_automation_certified=false`.
- Não foram executados fluxos E2E de estado real, SQLite ou Backup21 para estes previews.

## Pendências e integração

Dos 242 efeitos no inventário manual, 67 possuem preview tipado bloqueado para integração, 1 exige revisão do contrato fonte e 174 seguem sem executor completo. A contagem de previews inclui 6 escolhas de condição, 56 perfis de condição após salvaguarda, 3 ticks recorrentes e 2 efeitos em zero PV. O trabalho atual não reduz artificialmente essa contagem: os efeitos propostos não foram aplicados ao combate. A integração posterior exige componentes de fluxo/UI e persistência protegidos, entre eles `MainActivity.java`, `AppDb.java` e contratos Backup21, fora desta branch.

O draft PR deve permanecer aberto e sem merge até a aprovação e integração da Frente A.
