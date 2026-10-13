# XMM etapa 03 — CDs de fuga nos ataques de agarrão

## Fonte e divergência

Fonte pinada `5etools-mirror-3/5etools-src@8c026b807fac21862a309379a0c3228a0683198b`; blob do bestiário `9a4137b508ef37562ed948894a70b1594b57e85c`. O contrato independente `Xmm2025AttackConditionEngine` aponta 11 ações com Grappled e seus JSON Pointers, CDs de fuga e tamanhos-limite.

Nas 11 fontes, o alvo fica Grappled ao ser atingido e a CD indicada é de escape. Nenhuma dessas ações pede saving throw. No runtime compilado, os 11 registros haviam codificado a CD como `save_dc` e a condição como `condition_on_fail`, inclusive várias ações que também declaram outra condição atrelada ao agarrão.

## Correção

Os 11 blocos agora registram a CD em `escape_dc`, as condições do rider em `condition_on_hit` e o limite de tamanho em `condition_max_target_size`. O guard conserva o bloqueio do rider enquanto a aplicação completa, a fuga e sua persistência não estiverem resolvidas; não trata a CD como salvaguarda nem aplica efeito parcial sem autorização mecânica.

A cobertura inclui Ankheg/Bite, Bugbear Warrior/Grab, Crocodile/Bite, Giant Crocodile/Bite, Giant Frog/Bite, Giant Toad/Bite, Graveyard Revenant/Suffocate, Kuo-toa Whip/Pincer Staff, Mimic/Pseudopod, Tyrannosaurus Rex/Bite e Vine Blight/Constricting Vine. Cada registro é checado contra seu texto/pointer pinado, DC de escape, condição e tamanho máximo.

## Validação

- Comparador Python negativo: 12/12 passou.
- `verify_xmm_503_source.py`: 503 criaturas, 36 seções, 120 recargas, 132 conjuradores, 178 contratos de traits e 11 CDs de fuga comparados; automação integral de combate continua `false`.
- `verify_xmm_40_attack_conditions.py`: 40 riders conferidos (29 Prone, 11 Grappled); efeitos adicionais continuam marcados como manuais.
- BRELGA no SHA anterior `89dc40fc4cd985c5a122c3d48707f64ec7180068`: 2.450 testes, 1 falha na asserção que cobria os 11 agarrões enquanto somente Ankheg havia sido corrigido. A nova revisão estende a alteração aos 11 e será submetida à suíte completa novamente.

A mudança corrige os metadados de fonte; não implementa a fuga/persistência do agarrão e não fecha ETAPA 03.
