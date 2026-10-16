# Braseiro Star Wars VTT — R54 P162 | 12/10/2026

## Checkpoint canônico

- **Entrada verificada:** `BRASEIRO_STAR_WARS_EDGE_R54_P161_SOURCE_CHECKPOINT.zip` — SHA-256 `d98db98b1d370008601f5419059d298e1dab59cffc980237da0f7565e418ea5a`.
- **Saída:** `BRASEIRO_STAR_WARS_EDGE_R54_P162_SOURCE_CHECKPOINT.zip`, projeto completo para Android Studio (não é APK).
- Android `versionCode=10162`, `versionName=1.0.162`; bundle offline regenerado com 79 módulos.
- Os 8 arquivos Java, inclusive `TtsBridge.java` e `TtsChunker.java`, permanecem byte a byte idênticos ao P161; nenhum modelo neural incorporado.

## Implementações reais P162

1. **AoR +4 posições:** `INVERSIONES RENTABLES` (2) e `LABIA` (2) com consumidores reais. Cobertura **287/360 (79,7%)**; restam **73** em `evidence/P162/AOR_PENDING_73_POSITIONS.json`.
2. **Investimentos rentáveis:** 100 créditos por graduação ao **iniciar cada nova sessão**; sem bônus no carregamento, sem repetição dentro da sessão. Valor e graduação registrados em `session.campaignRules.soundInvestments`, persistidos com créditos e estado de sessão na mesma transação.
3. **Atomicidade de início de sessão:** Destiny, obrigação/dever, créditos, reinicialização de talentos e início da sessão são commitados juntos. Se o repositório falha ou a sessão já está ativa, todo o estado anterior é restaurado. Rejeita overflow de créditos.
4. **Lábia:** perícia social escolhida por nó na compra (`carisma`, `coaccion`, `engano`, `negociacion`). Um Triunfo **real** pode ser gasto para adicionar sucessos iguais às graduações adquiridas naquela perícia. Sem Triunfo, nenhum bônus; impede gasto duplicado de Triunfo e parâmetros inválidos.
5. **Migração de compras antigas:** `assignSmoothTalker` vincula uma compra AoR prévia ainda sem perícia sem custo em PE, apenas uma vez, verificando ID e nome originais da árvore; com rollback.
6. **Android PT-BR:** painel de Investimentos rentáveis/Lábia, ação de teste social e vinculação de nó legado. **Corrigido também o formulário de compra de talentos** para oferecer seletores corretos de perícia social e Conhecimento (antes ambos caíam no seletor de Característica).
7. **Cinco aventuras:** testes por aventura com gatilho → objetivo → relógio → consequência, duas transições, reexecução idempotente, rollback e três restaurações. Isso **não é certificação editorial** dos PDFs.
8. **Regressão histórica:** asserções de versões antigas sobre Lábia permanecer inativa atualizadas sem afrouxar os demais talentos não implementados.

## Testes e evidências

- `evidence/P162/NODE_FOCAL.txt`: **24/24** novos testes.
- `evidence/P162/NODE_REGULAR.txt`: **2.194/2.194**, 277 arquivos de testes.
- `evidence/P162/NODE_STRESS.txt`: **6/6**.
- `evidence/P162/KOKORO_TTS_REGRESSION.txt`: **27/27** testes de código.
- `evidence/P162/JAVA_SYNTAX.txt`: **8/8** arquivos Java sintaticamente válidos; não equivale à compilação Android.
- `evidence/P162/AUDIT.json`: `allPassed=true`; cinco aventuras com grafos/semântica válidos, 79 módulos, Java preservado e versão conferida.
- `evidence/P162/ANDROID_BUILD.txt`: `bash gradlew --offline :app:assembleDebug` falhou porque a distribuição Gradle 8.13 não pôde ser baixada. **Nenhum APK compilado ou homologado**.

## Pendências reais

1. 73 posições AoR sem consumidor; não inflar cobertura sem efeitos e testes.
2. Auditoria completa de todas as expansões EotE/AoR contra PDFs primários do Drive.
3. Certificação integral das cinco aventuras contra PDFs (cenas, NPCs, fichas, consequências, recompensas); perfis provisórios de *Além da Orla* permanecem provisórios.
4. Compilar no runner BREGA/BRELGA com Gradle/SDK, instalar em S23 Ultra/A57, testar três reaberturas e áudio Kokoro físico.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p162-sound-investments-smooth-talker-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p162-release.mjs
```

**Continuidade:** preservar P162 e evitar confundir cobertura de código, auditoria editorial e homologação física. Nenhuma alegação de 100% do VTT.
