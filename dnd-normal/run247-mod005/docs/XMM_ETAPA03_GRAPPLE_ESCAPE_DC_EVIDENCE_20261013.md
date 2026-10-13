# XMM etapa 03 — CDs de fuga nos ataques de agarrão

## Fonte e divergência

Fonte pinada `5etools-mirror-3/5etools-src@8c026b807fac21862a309379a0c3228a0683198b`; blob do bestiário `9a4137b508ef37562ed948894a70b1594b57e85c`. O contrato independente `Xmm2025AttackConditionEngine` aponta 36 ações com Grappled e seus JSON Pointers, CDs de fuga e tamanhos-limite explícitos. Três outras ações com Grappled não declaram limite de tamanho e não recebem um limite presumido.

Nas ações cobertas, o alvo fica Grappled ao ser atingido e a CD indicada é de escape. Na maioria, não há saving throw para o Grappled. No runtime compilado, os registros haviam codificado a CD como `save_dc` e a condição como `condition_on_fail`, inclusive várias ações que também declaram outra condição atrelada ao agarrão. Grell / Paralyzing Tentacles é a exceção que exige preservar uma salvaguarda separada (Constituição CD 11) além do escape CD 12.

## Correção

Os 36 blocos agora registram a CD em `escape_dc`, as condições do rider em `condition_on_hit` e o limite de tamanho em `condition_max_target_size`. Grell também preserva separadamente sua salvaguarda de Constituição CD 11. O guard conserva o bloqueio do rider enquanto a aplicação completa, a fuga e sua persistência não estiverem resolvidas; não trata a CD de escape como salvaguarda nem aplica efeito parcial sem autorização mecânica.

Cada registro é checado contra seu texto/pointer pinado, DC de escape, condição e tamanho máximo. A verificação também detecta salvaguardas separadas existentes no mesmo texto de ação para não apagá-las nem confundi-las com o escape.

## Validação

- Comparador Python: 13/13 passou.
- `verify_xmm_503_source.py`: 503 criaturas, 36 seções, 120 recargas, 132 conjuradores, 178 contratos de traits e 36 CDs de fuga comparados; automação integral de combate continua `false`.
- `verify_xmm_65_attack_conditions.py`: 65 riders conferidos (29 Prone, 36 Grappled); 21 efeitos adicionais continuam marcados como manuais.
- BRELGA run `38411724879`, código SHA `a7837a276482eda028f02e565e42c17d4fb65d41`: 93 suites, 2.450 testes, 0 falhas/erros/ignorados. Evidência `dnd-normal-qa-evidence/runs/38411724879/attempt-1.json`; XML `Xmm503SourceAndRechargeTest` SHA-256 `20bb2b8fd0edcc018780363935055154a8f0ae5ecaee94d50d058b7c6e194652`.

Auditoria GitHub-hosted run `38411724847` foi bloqueada antes dos steps por pendência de billing/limite da conta (check annotation). Esse run antecede a ampliação para 36 ações. Na tentativa local desta revisão, Gradle 8.11.1 não pôde ser baixado por conexão recusada; não há ainda resultado Android vinculado ao novo SHA.

A mudança corrige os metadados de fonte; não implementa a fuga/persistência do agarrão e não fecha ETAPA 03.
