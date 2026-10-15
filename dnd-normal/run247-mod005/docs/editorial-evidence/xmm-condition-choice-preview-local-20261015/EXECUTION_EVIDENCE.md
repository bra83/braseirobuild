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

O verificador independente usa o bestiário e livro XMM pinados em `8c026b807fac21862a309379a0c3228a0683198b`. O manifesto `manual-effect-inventory-199.json` tem 199 ponteiros únicos para ações que ainda não são executáveis integralmente. Cada linha contém contrato mecânico, dono do executor, situação, evidência e bloqueio. A contagem de inventário inclui entradas de Multiattacks sem plano estruturado e efeitos ainda parciais; ela não representa 199 executores ausentes.

### B1 — riders e condições

Foi criado `Xmm2025ConditionChoiceEngine` para seis ataques fonte-verificados de Death Slaad, Gray Slaad, Green Slaad, Githzerai Psion, Pixie e Pixie Wonderbringer. O executor exige o d4 ou escolha explícita, respeita o limite de tamanho e os gatilhos de duração. Rejeita fonte, ponteiro, condição, resultado de ataque ou campo de entrada inválidos.

Os resultados são propostas sem aplicação, persistência, rolagem ou mutação. Os seis registros permanecem classificados como `BLOCKED_MISSING_CONTEXT` até a integração do fluxo de combate após o PR #39.

Também foram adicionados previews source-bound para 58 famílias de condição originada por salvaguarda e três ticks de dano recorrente (Pit Fiend, Stirge e Vine Blight). Os previews exigem resultado/contexto explícito, validam ponteiro e blob fonte, recusam valores fora dos limites mecânicos e não aplicam condições/dano, não rolam dados e não persistem estado. O perfil Psychic Drain de Aboleth permanece `SOURCE_REVIEW_REQUIRED` porque a fonte compilada não declara salvaguarda/CD; não foi inferido um valor. Os perfis restantes continuam bloqueados até integração do ciclo de vida/fluxo de combate.

### B2 — Multiattack

O plano fonte-verificado de Sphinx of Secrets admite as três Garras ou a substituição de exatamente uma das três por Curse of the Riddle em qualquer posição. Foram adicionadas sequências reusáveis para Werebear, Wereboar, Wererat, Weretiger, Werewolf, Merrow e Pit Fiend. A sequência exige escolhas explícitas, total de slots exato e ações compiladas fonte-vinculadas. O passo da maldição continua com resolução manual; os planos descrevem a sequência, sem executar efeitos nem consumir recursos.

O índice agora tem 275 planos estruturados entre 306 Multiattacks; 31 ainda não têm sequência estruturada. O comparador confirma 197 sequências simples, substituição do Sphinx, cinco variantes de licantropos, 24 variantes compostas (incluindo as 11 substituições e as 3 sequências de Chimera, Gladiator e Jackalwere adicionadas neste lote), quatro variantes de dragões jovens e 19 sequências de dragões adultos/ancient/Dracolich que substituem uma posição por uma magia fonte-listada ou sopro. A escolha da magia passa pelo índice de uso fonte-verificado; nível, limites de uso e efeito continuam manuais e nenhum feitiço é lançado/debitado pelo preview. O Clay Golem exige contexto explícito de Hasten. O inventário contabiliza 33 efeitos Multiattack sem execução completa, pois alguns planos estruturados ainda incluem efeitos especiais que precisam de resolução manual.

### B3 — conjuradores e exceções

Não foi introduzido um executor novo de magia nesta execução. O comparador validou novamente os 132 registros de uso de magia, e o inventário mantém efeitos que dependem de concentração, área, múltiplos alvos ou estado de combate como pendentes até haver contrato e integração comprováveis. A aplicação de magia continua sem ser homologada por esta Frente B. Foram adicionados previews puros para Phase Spider e Vampire Familiar quando um acerto reduz o alvo a 0 PV; ambos propõem as condições fonte-declaradas e continuam bloqueados até integração.

### B4 — testes e publicação

- Comparador XMM: aprovado; 503 monstros, 36 seções, 120 recargas, 132 registros de magia, 178 contratos de traits, 59 ações de dano por salvaguarda e 306 Multiattacks analisados.
- Testes Python: 105 aprovados (`PYTHONPATH=tools python3 -m unittest discover -s tools -p 'test_*.py'`).
- O comparador independente passou com 58 perfis save-condition, 3 perfis recurring-damage, 2 perfis zero-HP e 24 variantes compostas de Multiattack; `source_field_and_catalog_gate_pass=true`.
- Gradle/Robolectric local: `JAVA_HOME=/workspace/jdk21 ANDROID_HOME=/workspace/android-sdk bash gradlew --offline --console=plain testDebugUnitTest` aprovado; XMLs agregam 2.605 testes, 0 falhas, 0 erros e 0 ignorados em 134 classes. Inclui 5 testes do preview zero-HP e os 2 testes novos para variantes compostas/contexto de Hasten e a validação das 275 sequências fonte-estruturadas.
- Fonte de verificação: `xmm-source-verification.json`; SHA-256: `6eb90f25a525bcc4dd660d377c1b5913cb54d601d24514f3078a3a5a67292c0d`.
- O comparador continua declarando `full_monster_combat_automation_certified=false`.
- Não foram executados fluxos E2E de estado real, SQLite ou Backup21 para estes previews.

## Pendências e integração

Dos 199 efeitos no inventário manual, 67 possuem preview tipado bloqueado para integração, 1 exige revisão do contrato fonte e 131 seguem sem executor completo. A contagem de previews inclui 6 escolhas de condição, 56 perfis de condição após salvaguarda, 3 ticks recorrentes e 2 efeitos em zero PV. O trabalho atual não reduz artificialmente essa contagem: os efeitos propostos não foram aplicados ao combate. A integração posterior exige componentes de fluxo/UI e persistência protegidos, entre eles `MainActivity.java`, `AppDb.java` e contratos Backup21, fora desta branch.

O draft PR deve permanecer aberto e sem merge até a aprovação e integração da Frente A.
