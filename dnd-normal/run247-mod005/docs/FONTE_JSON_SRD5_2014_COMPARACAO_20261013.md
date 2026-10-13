# Fonte de regras JSON encontrada — SRD5 original (2014)

## Proveniência efetivamente conferida

Fonte indicada pelo usuário:
- Repositório: https://github.com/BTMorton/dnd-5e-srd
- JSON **06 mechanics**: https://github.com/BTMorton/dnd-5e-srd/blob/master/json/06%20mechanics.json
- SHA Git blob: `004cc01b5e6228b7eafcae29830cd2d9d47584db`
- JSON **07 combat**: https://github.com/BTMorton/dnd-5e-srd/blob/master/json/07%20combat.json
- SHA Git blob: `8498fb3a779d7bc781d059e48ab66f77c0e8ed12`
- Licença declarada pelo próprio repositório: https://github.com/BTMorton/dnd-5e-srd/blob/master/LICENSE — OGL 1.0a / SRD5. Não pressupor permissão de redistribuição de Product Identity nem incorporar a obra inteira no APK.
- Fonte oficial distinta para a edição revisada: https://www.dndbeyond.com/srd/ — SRD v5.2.1, CC-BY 4.0, com mecânicas alteradas em relação ao 5.1.

**Atenção:** os dois JSONs do repositório BTMorton refletem o SRD5 original (2014) e **não** certificam os livros/suplementos 2024–2026, os 483 IDs editoriais, Eberron, Faerûn, Netheril ou Mystara.

## Resultados de engenharia ligados diretamente à fonte

| Regra SRD5 original | Java existente | Teste comportamental independente |
| --- | --- | --- |
| Modificador de atributo: floor((score-10)/2) | `DndRulesEngine.modifier` | `Srd5OriginalMechanicsJsonContractTest.srd2014AbilityScoresUseRoundedDownHalfDifference` |
| Seis atributos STR/DEX/CON/INT/WIS/CHA (abreviação e nome por extenso) | `DndRulesEngine.abilityScore` (corrigido) | `srd2014AllSixAbilityAbbreviationsAndLongNamesResolve` |
| Bônus de proficiência por faixa de nível | `DndRulesEngine.proficiencyBonus` | `srd2014ProficiencyProgressesAtLevelsFiveNineThirteenSeventeen` |
| Checagem passiva = 10 + modificadores; vantagem +5; desvantagem −5; cancela quando ambas | `DndRulesEngine.passiveScore` + nova sobrecarga com duas flags | `srd2014PassiveCheckHasPlusFiveMinusFiveAndCancellation` |
| Vantagem e desvantagem: maior/menor de dois d20, com cancelamento | `DndRulesEngine.chooseD20` | `srd2014AdvantageChoosesHighDisadvantageChoosesLow` |
| Capacidade de carga = 15 × Força; empurrar/arrastar/levantar = dobro | `DndRulesEngine.carryingCapacity/pushDragLiftCapacity` | `srd2014CarryingCapacityAndPushDragLiftUseStrengthMultiplier` |
| Ataque com d20 natural 1 sempre erra, natural 20 sempre acerta | `DndRulesEngine.resolveAttack` | `srd2014CombatNaturalOneMissesAndTwentyAlwaysHits` |
| Teste contra morte 1 natural causa duas falhas; 20 restaura 1 PV | `DndRulesEngine.resolveDeathSave` | `srd2014DeathSavesRollOneCountsTwoFailuresRollTwentyRegainsOneHp` |
| CD de concentração 2014 = maior entre 10 e metade do dano | `DndRulesEngine.concentrationDc` | `srd2014ConcentrationUsesTenOrHalfDamageWhicheverHigher` |

Implementação: `app/src/main/java/com/braseiro/dnd5e/DndRulesEngine.java`.
Teste: `app/src/test/java/com/braseiro/dnd5e/Srd5OriginalMechanicsJsonContractTest.java`.

## Condições de certificação

1. Os testes acima verificam **comportamento esperado do SRD5 2014**; só podem ser considerados aprovados quando o workflow do BRELGA concluir e publicar XML verde para o **SHA de código que os inclui**.
2. O perfil revisado 2024–2025 mantém prioridade para novas mecânicas: comparação obrigatória com o SRD 5.2.1; não transplantar indiscriminadamente regras de 2014.
3. Nenhuma dessas oito verificações pode alterar ou marcar `IMPLEMENTED_AND_TESTED` no ledger de **483 seções adicionais** sem vínculo com uma seção exata e evidência independente.
4. Mystara permanece congelado; nenhum arquivo daquele projeto foi alterado.
