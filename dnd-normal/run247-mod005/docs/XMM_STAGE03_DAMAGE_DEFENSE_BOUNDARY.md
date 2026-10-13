# XMM — Etapa 03: defesas tipadas (escopo conservador)

Fonte mecânica versionada: `5etools-mirror-3/5etools-src`, commit `8c026b807fac21862a309379a0c3228a0683198b`, blob do bestiário `9a4137b508ef37562ed948894a70b1594b57e85c`.

Implementado: 503 perfis de criaturas; 272 perfis com pelo menos uma resistência, imunidade, vulnerabilidade ou imunidade a condição explícita; cálculo de resistência (arredondamento para baixo), vulnerabilidade (dobro), imunidade (zero), checagem de condições e composição das 59 ações de salvaguarda já homologadas com defesas do alvo.

Os seis casos condicionais **não são interpretados como regras incondicionais**: Archmage (imunidade condicional a condição), Half-Dragon (resistência por origem), Juvenile Shadow Dragon e Shadow Dragon (resistência dependente de trait), Rakshasa (vulnerabilidade condicional), Vampire Familiar (imunidade condicional a condição). O cálculo bloqueia a dimensão afetada. Isso não cobre traits, auras, resistências de classe, proteção situacional, concentração, recursos, efeitos de magia, rolagens, pontos de vida ou alterações de estado. Nenhuma transcrição de prosa de regras foi incorporada.

Contrato: `xmm.defense_profile`, `xmm.defense_resolve`, `xmm.condition_immunity_check`, `xmm.save_damage_vs_defender`, `xmm.defense_coverage`. Saídas são consultivas e exigem adjudicação e aplicação manual; `auto_mutation_allowed=false`.

**Gate de encerramento da etapa 03 permanece aberto.** Evidência necessária: suíte completa BRELGA verde no HEAD, homologação integral dos demais riders, traits, multiattacks e conjuração. Mystara e etapas 01–02 não foram alterados.
