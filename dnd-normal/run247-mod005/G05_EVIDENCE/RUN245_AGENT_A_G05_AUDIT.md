> Static source review. Current source/mechanics counts are in G05_SOURCE_AUDIT_RUN245.md and G05_SOURCE_TO_EXECUTOR_MATRIX.csv. The latest expanded suite in run 38061684317 passed 76/76 tests, including the G05 focused cases and the G08/G09 replay receipt-key correction. See current gate audit.

# RUN245 / G05 — auditoria do agente A

## Escopo e resultado

Foi feita leitura dos achados em `G05_ACTIONS_NEEDING_SOURCE_REVIEW.csv` e dos executores de statblock já existentes. Não foi feita alteração de regra ou de runtime: os achados residuais exigem semântica que não está completa nos registros compilados. Não editei `AppDb.java` nem `MainActivity.java`.

O inventário tem 280 linhas, 1504 ações examinadas e duas fontes (`Ravenloft: The Horrors Within` e `Monster Manual (2025)`). As contagens são sobrepostas: `G05_CATALOG_AUDIT.json` as descreve como itens para revisão de fonte, não como lacunas definitivas.

## Cobertura confirmada no código

- Ações comuns consomem somente `action` ou `bonus_action`; reações e ações lendárias são recusadas pelo executor comum até existir orçamento e gatilho próprios. Ver `MonsterActionEconomyGuard.java:14-28` e `AppDb.java:2947-2953`.
- Salvaguardas genéricas exigem DC e atributo válido. Um DC sem atributo é recusado porque pode ser CD de escapar de agarrão ou outro uso; o código proíbe inferir sua finalidade. Ver `MonsterActionEconomyGuard.java:41-50` e `AppDb.java:2940-2942`.
- Efeitos de falha são aceitos se repetirem exatamente condições reconhecidas, ou se forem push/pull simples compilados. Durações/turnos e efeitos compostos continuam bloqueados. Ver `MonsterEffectOnFailGuard.java:14-36`, `MonsterConditionRiderPlan.java:24-47` e `MonsterForcedMovementRules.java:28-40`.
- Push/pull têm destino manual, validação da rota e dos cantos, revalidação antes da gravação e transação com dano/condição. Ver `MonsterForcedMovementRules.java:61-109` e os caminhos de ataque/salvaguarda em `AppDb.java:2510-2563` e `2954-2994`.
- Condição temporizada só expira se o registro declarar rodada e gatilho. Sem duração compilada, a rotina não cria temporizador. Ver `MonsterConditionTimerLedger.java:24-45`.

## Achados residuais que não podem ser fechados com os metadados atuais

- **67 categorias especiais:** incluem `legendary_action`, `reaction` e outras categorias que não usam ação/ação bônus normal. O registro do catálogo não fornece em geral limite/orçamento e gatilho individual suficiente para tornar o executor correto. Não foi introduzido orçamento padrão.
- **53 ataques com DC sem atributo:** os registros incluem casos de agarrar, como `Giant Octopus / Tentacles` (DC 13, `grappled, restrained`) e `Bodytaker Plant / Vine` (DC 14, `grappled`). O formato não distingue explicitamente CD de escape de salvaguarda e omite dados necessários em vários casos, como limites de tamanho. Converter isso em salvaguarda ou aplicar condições no acerto mudaria a regra.
- **105 ataques com rider sem salvaguarda ou handler tipado:** há efeitos de condição, deslocamento, teleportes e outros riders. `condition_on_fail` não prova duração nem que o rider ocorre no acerto. O executor recusa esses casos em vez de aplicar condição sem duração.
- **28 efeitos de salvaguarda sem executor tipado:** incluem grapple, teleport, combinações e uma ação com push e pull no mesmo campo (`Ankhtepot / Hand of Fate`). O handler atual cobre somente push/pull simples, condições idênticas à condição declarada e duração/recovery explicitamente compilados. Não há base para escolher semântica/direção de efeitos compostos.
- **27 ações com DC genérica sem atributo:** várias são `Spellcasting` ou opções de magia. O DC geral não identifica a salvaguarda de uma ação específica; atribuir atributo seria inferência.

## Verificações e limites desta execução

Executados:

- `python G05_EVIDENCE/test_run245_wiring.py` — passou, 15 asserções estáticas de wiring; não compila nem executa Android/Robolectric.
- `python G05_EVIDENCE/test_run244_wiring.py` — passou, 9 asserções estáticas; não é execução Android.

Não reproduzidos nesta worktree:

- Harness Java puro RUN245: esta sessão tem `/usr/bin/java`, mas não `javac`; a tentativa de compilação terminou em `FileNotFoundError: javac`. `RUN245_VALIDATION.log` já registra anteriormente `RUN245_G05_FORCED_MOVEMENT_PURE_PASS checks=13`; isso não foi revalidado aqui.
- Gradle, Robolectric, APK/aparelho e assinatura: permanecem não executados, conforme `RUN245_VALIDATION.log` (distribuição Gradle ausente por `UnknownHostException`; sem SDK/distribuição local).

Nenhum arquivo de produto foi alterado. Este relatório é evidência de auditoria e mantém G05 parcial; não homologa gate.
