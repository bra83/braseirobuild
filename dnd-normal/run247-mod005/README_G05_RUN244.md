# BRASEIRO D&D NORMAL — RUN244 — G05 (EM ANDAMENTO)

**Esta é a árvore Android Studio original**, recuperada de `braseiro_unpacked_run243/android` do ZIP fornecido. Abra **esta pasta**, contendo `settings.gradle.kts`, no Android Studio. O projeto Kotlin/Compose genérico da raiz do ZIP enviado foi descartado.

## O que foi codificado nesta rodada

- `AppDb.java`: novo overload para ataque de monstro com destino explícito `destinationX/destinationY` para *rider* de salvaguarda com `push:Nft`, `pull:Nft` e `;prone` reconhecidos por `MonsterForcedMovementRules` **somente quando o bloco compilado autorizou o efeito**.
- A implementação verifica a fonte, a salvaguarda, a cena, a distância, cada obstáculo e ocupação antes de gastar ação/aplicar dano. Confirma posição e recibo em `combatAtomic`. Chamadores antigos continuam funcionando e recusam deslocamentos sem destino.
- `MainActivity.java`: campos manuais X/Y no diálogo de ataque; nunca teleporta automaticamente nem aceita decisão arbitrária da IA.
- `Run244G05AttackRiderForcedMovementRobolectricTest.java`: teste de regressão nova API, ausência de destino, obstáculos, ação/HP/condição/posição, replay e Backup21.
- `G05_ACTIONS_NEEDING_SOURCE_REVIEW.csv`: mapeia **280 registros de pendências** (com sobreposição) provenientes de 572 criaturas e 1.504 ações auditadas. Não são correções automáticas.

## Validações reais

- **PASS:** RUN238 pure Java, auditoria catálogo e verificação estática, logs `run238_after.log`.
- **PASS:** `JavacTask.parse` em `AppDb.java`, `MainActivity.java`, teste RUN244 (3 fontes).
- **PASS:** 9 verificações de wiring de código pelo script `test_run244_wiring.py`.
- **PASS parcial:** histórico `Run237ForcedMovementJvmTest` (53.720 verificações), backup RUN236 (66.022), recuperação RUN235 (28.073); suíte restante interrompida por limite de execução.
- **NÃO EXECUTADO:** compilação Gradle/Android, Robolectric RUN244, testes em Galaxy S23 Ultra/A57 e assinatura APK.

## Para validar no BRELGA

No diretório raiz do projeto, com JDK e Android SDK adequados:

```
./gradlew :app:compileDebugJavaWithJavac :app:testDebugUnitTest :app:lintDebug --stacktrace --no-daemon
```

No Windows executar `gradlew.bat` no lugar de `./gradlew`. Rodar especificamente `Run244G05AttackRiderForcedMovementRobolectricTest`, `Run238ActionEconomyRobolectricTest`, `Run237ForcedMovementRobolectricTest`, `Run236ConditionBackupRobolectricTest` e demais RUN231–RUN243 antes de assinar qualquer APK. **Nunca publicar como final sem CI verde e teste real de atualização.**

## O que NÃO foi fechado

G05 continua ABERTO. Segundo inventário automatizado: 105 riders de ataque sem CD/handler, 53 ataques com habilidade de resistência inválida/ausente, 27 ações de salvaguarda com habilidade ausente, 67 categorias especiais sem orçamento/gatilho próprio, 28 efeitos de falha sem executor tipado, com sobreposição. Confirmar cada caso no livro de origem antes de mudar o runtime. O CSV aponta IDs, ação, arquivo Java, categoria e campos para revisão. Não inventar CD, atributo, gatilho nem efeito.

Esta entrega é uma fonte intermediária com avanço concreto, **não G05 completo**.
