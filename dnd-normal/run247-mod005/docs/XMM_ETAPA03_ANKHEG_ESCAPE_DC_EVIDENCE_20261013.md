# XMM etapa 03 — Ankheg Bite e CD de fuga

## Fonte e divergência

Fonte pinada `5etools-mirror-3/5etools-src@8c026b807fac21862a309379a0c3228a0683198b`, blob do bestiário `9a4137b508ef37562ed948894a70b1594b57e85c`, registro `Ankheg`, ação `Bite`, `/monster/32/action/0/entries`.

A entrada fonte especifica ataque +5, alcance 5 pés, 2d6+3 cortante mais 1d6 ácido; ao acertar, alvo Grande ou menor fica Grappled e pode escapar com CD 13. Esse DC não é salvaguarda. O runtime compilado anterior registrava `save_dc:13` e `condition_on_fail:grappled`, confundindo o gatilho e o tipo de teste. Além disso, o app ainda não possui fuga de agarrão integrada ao estado persistente do combate.

## Alteração e limite seguro

O bloco XMM agora usa `escape_dc:13`, `condition_on_hit:grappled` e `condition_max_target_size:large`. O guard retorna `unsupported_grapple_escape_rider`; portanto não cria salvaguarda fictícia nem concede um agarrão sem suporte de fuga e persistência. A ação continua sem resolução completa para o jogador.

O comparador independente lê o texto do registro na revisão pinada e exige correspondência para ataque, alcance, dados de dano, limite de tamanho e CD de fuga, rejeitando `save_dc`, `save_ability` ou `condition_on_fail`. O teste Android de 503 criaturas contém também uma asserção focada para o registro compilado e motivo seguro de bloqueio.

## Validação desta alteração

- `python3 -m unittest discover -s tools -p test_verify_xmm_503_source.py -v`: 10/10 passou.
- `python3 tools/verify_xmm_503_source.py`: comparou 503 criaturas, 36 seções, 120 recargas e 132 registros de conjuração; `source_field_and_catalog_gate_pass=true`; automação total de combate permanece `false`.
- Android `:app:testDebugUnitTest --tests com.braseiro.dnd5e.Xmm503SourceAndRechargeTest`: não executou localmente. Gradle wrapper tentou baixar Gradle 8.11.1 e recebeu `Connection refused`; aguarda runner BRELGA para o SHA desta alteração.

Esta prova corrige uma discrepância localizada e não fecha ETAPA 03.
