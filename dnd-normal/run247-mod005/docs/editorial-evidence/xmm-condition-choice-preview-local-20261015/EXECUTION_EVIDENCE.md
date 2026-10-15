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

O verificador independente usa o bestiário e livro XMM pinados em `8c026b807fac21862a309379a0c3228a0683198b`. O manifesto `manual-effect-inventory-168.json` tem 168 ponteiros únicos para ações que ainda não são executáveis integralmente. Cada linha contém contrato mecânico, dono do executor, situação, evidência e bloqueio.

### B1 — riders e condições

Foi criado `Xmm2025ConditionChoiceEngine` para seis ataques fonte-verificados de Death Slaad, Gray Slaad, Green Slaad, Githzerai Psion, Pixie e Pixie Wonderbringer. O executor exige o d4 ou escolha explícita, respeita o limite de tamanho e os gatilhos de duração. Rejeita fonte, ponteiro, condição, resultado de ataque ou campo de entrada inválidos.

Os resultados são propostas sem aplicação, persistência, rolagem ou mutação. Os seis registros permanecem classificados como `BLOCKED_MISSING_CONTEXT` até a integração do fluxo de combate após o PR #39.

### B2 — Multiattack

O plano fonte-verificado de Sphinx of Secrets agora admite as três Garras ou a substituição de exatamente uma das três por Curse of the Riddle em qualquer posição. A sequência exige escolhas explícitas, total de slots exato e ações compiladas fonte-vinculadas. O passo da maldição continua com resolução manual; o plano não executa efeitos nem consome recursos.

O índice sobe de 224 para 225 planos estruturados em 306 Multiattacks; 81 continuam manuais. O resolvedor foi exposto pelo batch preview XMM com ponteiro exato e lista de campos permitidos.

### B3 — conjuradores e exceções

Não foi introduzido um executor novo de magia nesta execução. O comparador validou novamente os 132 registros de uso de magia, e o inventário mantém efeitos que dependem de concentração, área, múltiplos alvos ou estado de combate como pendentes até haver contrato e integração comprováveis. A aplicação de magia continua sem ser homologada por esta Frente B.

### B4 — testes e publicação

- Comparador XMM: aprovado; 503 monstros, 36 seções, 120 recargas, 132 registros de magia, 178 contratos de traits, 59 ações de dano por salvaguarda, 110 contratos simples de Multiattack.
- Testes Python: 95 aprovados.
- Gradle/Robolectric: `testDebugUnitTest` aprovado; XML contém 2.588 testes, 0 falhas, 0 erros e 0 ignorados.
- Fonte de verificação: `xmm-source-verification.json`; SHA-256: `1216818abd9a653c6da098f2e2a0ad871c40ea8d2a02328fce6438b1d018fbcd`.
- O comparador continua declarando `full_monster_combat_automation_certified=false`.
- Não foram executados fluxos E2E de estado real, SQLite ou Backup21 para estes previews.

## Pendências e integração

Dos 168 efeitos manuais, 6 possuem preview tipado bloqueado para integração e 162 seguem sem executor completo. O trabalho atual não reduz artificialmente essa contagem: os efeitos propostos não foram aplicados ao combate. A integração posterior exige os componentes de fluxo/UI e persistência protegidos, entre eles `MainActivity.java`, `AppDb.java` e contratos Backup21, fora desta branch.

O draft PR deve permanecer aberto e sem merge até a aprovação e integração da Frente A.
