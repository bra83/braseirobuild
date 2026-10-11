> Static persistence review plus BRELGA evidence. The earlier 45-test baseline passed in run 38054616224. In run 38061235785, the G05 rider case passed through receipt, DB persistence, Backup21 round-trip/remapping, replay and timer expiration. Follow-up run 38061684317 passed the full 76-test suite, including the G08/G09 replay receipt-key correction. See GATE_AUDIT_G06_G14_RUN245.md.

# RUN245 / G05 — auditoria de integração, SQLite e Backup21 (frente C)

## Caminho UI → executor → recibo

A tela de combate em `MainActivity.java` oferece ataque, multiataque e ação de salvaguarda para o monstro ativo. Os diálogos coletam rolagens e escolhas manuais e chamam `AppDb.applyCompiledStatblockAttack`, `applyCompiledStatblockMultiattack` ou `applyCompiledStatblockSaveAction` (faixa aproximada `MainActivity.java:3150–3423`). A autoridade vem do registro em `CompiledStatblockEngine`; `CreatureStatBlockCompiler.compile` extrai texto e dados do bloco, mas não converte prosa descritiva em mecânica executável.

As três operações consultam recibos idempotentes e executam gasto de ação, dano, efeitos, movimento e recibo dentro de `combatAtomic`. O banco permanece em `DB_SCHEMA=61`; `effect_receipts` usa chave `(world_profile, effect_key)`. `combatAtomic` só confirma a transação em sucesso e propaga falhas aninhadas para rollback (`AppDb.java` em torno das linhas 3708 e 3781).

O deslocamento forçado planeja contra cena e tokens persistidos e repete as verificações antes da gravação. Divergência de `scene_key`, posições ou destino causa recusa/rollback (`AppDb.java` em torno das linhas 2825 e 2870). A revisão não reproduziu um caminho de cena obsoleta que contorne essa validação.

## Run236 e Backup21

A leitura estática de `Run236ConditionBackupRobolectricTest` confere três propriedades: remapeamento de `source_actor_id` com persistência após reabrir o DB; recuperação pendente continua bloqueando a rodada mesmo com `encounter_id=0`; e origem órfã aborta o import sem substituir o estado anterior. O caminho do import remapeia timers e falha atomicamente quando a origem não existe (`AppDb.java` em torno das linhas 7114 e 7149).

`exportAll` inclui `effectReceipts` no envelope Backup21; o import remapeia esses recibos para os novos IDs (`AppDb.java` em torno das linhas 6759, 6801 e 6871). `Backup21RequiredSections` exige a seção nos backups v21 selados. Limite estático observado: backups v21 legados sem selo continuam aceitos; se também não tiverem `effectReceipts`, o import não recupera a proteção de replay através desse backup.

## Lacunas de cobertura de teste (não falhas reproduzidas)

- Os testes Run244/Run245 repetem o token durante uma execução, mas não testam replay após importar um Backup21 selado e reabrir o DB.
- Não foi encontrado teste que injete falha após um componente já aplicado do multiataque para demonstrar rollback total.

Patch mínimo sugerido pela auditoria: acrescentar esses dois casos de teste. Não foi aplicado porque o Android SDK ausente impede execução Robolectric neste ambiente e a auditoria não reproduziu falha de produção.

## Execução

Comando tentado:

```text
./gradlew testDebugUnitTest --tests com.braseiro.dnd5e.Run236ConditionBackupRobolectricTest --tests com.braseiro.dnd5e.Run244G05AttackRiderForcedMovementRobolectricTest --tests com.braseiro.dnd5e.Run245G05MultiattackFinalForcedMovementRobolectricTest --tests com.braseiro.dnd5e.Run245G05ForcedMovementCornerRobolectricTest
```

Resultado: `BUILD FAILED` antes de compilar ou iniciar testes, com `SDK location not found`; `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `adb` e `sdkmanager` não estão configurados. A verificação acima é somente leitura estática. Nenhuma aprovação de Run236/Robolectric foi declarada.
