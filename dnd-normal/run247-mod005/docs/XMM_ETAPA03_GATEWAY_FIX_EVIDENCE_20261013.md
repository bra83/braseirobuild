# XMM etapa 03 — correção do roteamento de conjuração

## Unidade e fonte

- Fonte pinada: `5etools-mirror-3/5etools-src@8c026b807fac21862a309379a0c3228a0683198b`.
- Bestiário XMM: blob `9a4137b508ef37562ed948894a70b1594b57e85c`; registro `Aarakocra Aeromancer`, JSON Pointer `/monster/0/spellcasting`.
- Registro fonte/runtime: `Xmm2025SpellUsageIndex#options` e `#check`, encaminhados via `OfficialBookRuntimeGateway#executeXmm`.

## Defeito reproduzido e alteração

Antes da correção, os ramos `spell_options` e `spell_usage_check` estavam dentro do handler Ravenloft. Assim, `execute("xmm", ...)` retornava `action_not_implemented`; o teste Android falhou em `XmmSpellUsageGatewayTest.androidGatewayRoutesCatalogAndEligibility` no SHA `53bee6fc7ec442cbc0f993312f18a3d22b27c6c9` (CI 38378831588: 1 falha / 1.642 testes).

No commit `b7627b4df8165979732839c151755d7a720cfb94`, os dois ramos foram movidos para `executeXmm` e removidos de `executeRavenloft`. O teste agora confere o ID do conjurador retornado, a disponibilidade de Mage Hand sem efeitos automáticos e que RHW não executa essa ação XMM.

## Resultado executado

- Teste: `XmmSpellUsageGatewayTest#androidGatewayRoutesCatalogAndEligibility` e suíte Android completa.
- BRELGA run `38380092340`: 84 suites, 1.642 testes, 0 falhas, 0 erros, 0 ignorados; SHA de código `b7627b4df8165979732839c151755d7a720cfb94`.
- Repetição com o registro documental incluído: BRELGA run `38381018048`, SHA `5fb607be60fdd5baef6eefd225fe2fc6410e34e5`, 84 suites, 1.642 testes, 0 falhas, 0 erros, 0 ignorados.
- XMLs brutos e hashes arquivados no branch `dnd-normal-qa-evidence`, em `runs/38381018048/attempt-1.json`. O XML específico `TEST-com.braseiro.dnd5e.XmmSpellUsageGatewayTest.xml` contém 5/5 testes verdes e SHA-256 `700b80118f116de46c01e69c8fbeee1944e3fb70efc9c3b6156561a5c9449214`.
- Fonte comparada independentemente: `verify_xmm_503_source.py` reportou 503 statblocks, 36 seções, 120 recargas e 132 registros de conjuração; `full_monster_combat_automation_certified=false`.

## Limite desta prova

Esta mudança corrige disponibilidade do índice/decisão de conjuração pelo gateway. Não conjura magias, não aplica dano/condições, não consome usos nem persiste resultados de combate. Não certifica semanticamente as 36 seções, os 503 conjuntos de efeitos, os 549 riders ou 525 traits; a etapa XMM permanece aberta.
