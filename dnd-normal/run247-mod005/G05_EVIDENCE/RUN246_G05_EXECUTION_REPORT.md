# RUN246 — Braseiro D&D 5e NORMAL — G05 source patch (NÃO HOMOLOGADO)

## Genealogia

Base: `BRASEIRO_DND_NORMAL_RUN245_PUBLICACAO_20261010.zip` (entrega Codex, partes 001+002), SHA256 `4e5fe706c12453465023946cc6d85b249d6cf91c7d56ffe7cf205aeeb5d78c2f`. Não usar HYBRID_FASTPLAY. Todas as 1.389 entradas da base foram preservadas; exatamente quatro arquivos da base foram modificados e dois arquivos de evidência/teste foram acrescentados.

Versão de código-fonte proposta `6.6.14-r246-g05-source-unhomologated`, versionCode `606546`, DB61, Backup21 e appId `com.braseiro.dnd5e` preservados. **Nenhum APK novo foi compilado ou homologado nesta sessão.**

## Implementação

A auditoria auxiliar `G05_SOURCE_TO_EXECUTOR_MATRIX.csv` continha quinze ações com condição sem salvaguarda no acerto, mas com `condition_on_fail` (e possivelmente `effect_on_fail`) em vez de `condition_on_hit`. A regra da fonte auxiliar descrevia o efeito até o começo do turno seguinte de seu autor. Para cada ação individualmente:

- Reconciliado o gatilho com `condition_on_hit` explícito e removidos os campos de falha que induziam save inexistente.
- Codificados `condition_duration_rounds=1` e `condition_duration_tick=source_turn_start`; utiliza o executor transacional e o temporizador existentes sem criar motor paralelo.
- Para XMM, `manual_rider_resolution_required` virou `false` somente para a condição agora tipada.
- Mantidas as ações de multiataque com comportamento *fail-closed* para rider por componente, pois o executor do multiataque ainda não faz commit desses temporizadores por componente. Não confundir a implementação de ataque isolado com liberação do multiataque.

As quinze ações estão em `RUN246_TYPED_ON_HIT_SOURCE_RECONCILIATION.json`, com criatura, ação, estado e URL da fonte auxiliar. Livros licenciados não foram validados página a página. O conteúdo da fonte original não foi inferido além das notas da auditoria anterior.

## Testes

**PASS executado localmente:** verificador Python recompilando os 572 registros JSON, revisando contrato das 15 ações e auditando preservação byte-idêntica das demais 1.385 entradas da base; parser AST Java dos dois arquivos fonte e do novo teste (sem erros); verificações estáticas anteriores RUN244 (9) e RUN245 (27).

**ESCRITO, NÃO EXECUTADO:** `Run246G05TypedOnHitBatchRobolectricTest.java`, que testa as quinze ações, o contrato de acerto/falha, ausência de DC inventada, temporizador por turno de origem, rejeição de duração inválida e recusa de multiataque sem executor de rider. **Não confundir com PASS Robolectric.**

**NÃO EXECUTADO:** Android Gradle, Robolectric completo, BRELGA, APK, assinatura e E2E no aparelho. O ambiente atual não possui Android SDK/Gradle para reproduzir os testes Android. Os 76 testes BRELGA históricos são da versão anterior e **não** validam RUN246.

## G05 ainda aberto

Matriz de 280 issues: 4 executores fonte-bound já trabalhados na entrega anterior, 15 ações individuais tipadas na RUN246 aguardando Robolectric/Android e 261 restantes sem executor aprovado. Além disso, as 15 podem exigir outras validações mecânicas de ficha/condições antes de um fechamento do G05. Outras categorias incluem agarrões com escape DC (não salvar automaticamente), ações lendárias e reações (orçamento/gatilho próprio), múltiplos efeitos com opção de escolha, poderes mágicos, efeitos contínuos, tamanho/alvo/alcance, teleporte, ações com cura máxima reduzida e ações de covil. Não liberar esses efeitos a partir de simples nomes de campo.

## Próximo gate exato no BRELGA

1. Recuperar ZIP integral da RUN246, confirmar SHA256/CRC e todos os recursos.
2. Executar `:app:compileDebugJavaWithJavac` e `:app:testDebugUnitTest --tests 'com.braseiro.dnd5e.Run246G05TypedOnHitBatchRobolectricTest'` e suite histórica inteira; corrigir falhas reais.
3. Executar `:app:assembleDebug` e checar appId, DB61, Backup21 e assinatura sem sugerir atualização de produção antes de verificar a chave.
4. Continuar implementação em blocos de ações restantes, respeitando o `G05_SOURCE_TO_EXECUTOR_MATRIX.csv`, registrando o gate de cada linha (pendente/implementado/testado/homologado), sem porcentagens inventadas.
