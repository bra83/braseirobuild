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

Auditoria GitHub-hosted run `38414597372`, código SHA `52ea9806da9d2b0dbad217f4eee936cc5fc24ac3`, passou: fonte pinada, 503 monstros, 36 seções e 65 riders. A suíte BRELGA run `38415921354`, código SHA `e343288bebeb414d6c2f8a6bb9d02fab7e11a0fe`, passou com 93 suites e 2.475 testes sem falhas/erros/ignorados. XML `Xmm503SourceAndRechargeTest` SHA-256 `f484a86b9b9919a4c5665f975dae6b87764666f13c59d952382787113c9f59f7`. A tentativa Gradle local anterior não iniciou porque o ambiente recusou o download de Gradle 8.11.1.

No HEAD combinado seguinte (`ba6b7c1e9270e856bdbc2097197064142bc2374c`), BRELGA run `38417191768` passou com 94 suites, 2.481 testes, 0 falhas/erros/ignorados. Hash do XML `Xmm503SourceAndRechargeTest`: `17c06b62cc8f745d2580d24a7033c74ab7e79b642bf3cd634aeb333b69ed6fbc`. Essa execução também cobriu os testes de bridge de rider por etapa explícita do multiataque. APK não foi compilado.

A mudança corrige os metadados de fonte; não implementa a fuga/persistência do agarrão e não fecha ETAPA 03.
