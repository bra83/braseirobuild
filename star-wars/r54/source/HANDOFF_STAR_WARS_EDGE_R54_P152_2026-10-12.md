# Braseiro Star Wars VTT — R54 P152 | 12/10/2026

## Base e entrega

- **Base canônica:** `BRASEIRO_STAR_WARS_EDGE_R54_P151_SOURCE_CHECKPOINT.zip`.
- **Novo checkpoint:** `BRASEIRO_STAR_WARS_EDGE_R54_P152_SOURCE_CHECKPOINT.zip`, projeto integral para Android Studio, **não APK**.
- Android: `versionCode=10152`, `versionName=1.0.152`; WebView shell 1.0.152; bundle offline regenerado a partir de 79 módulos.
- Fonte canônica de regras/Android preservada; nenhuma mecânica AoR foi anunciada como concluída sem implementação.

## Bloco integrado P152 — alterações reais

1. **Atomicidade de 14 fluxos compostos do GameController:** `beginCombat`, `beginEncounter`, `advancePlayerTurn`, `finishPlayerInitiativeTurn`, `resolveWeaponAttack`, `resolveOpposedSkillCheck`, `resolveSocialInfluenceCheck`, `resolveAssistedSkillCheck`, `resolveCompetitiveSkillCheck`, `resolveCheck`, `resolveSuggestedMasterCheck`, `applyEnvironmentTurnStart`, `resolveImpassableTerrain`, `resolveFall`.
2. **Bug corrigido:** `resolveSuggestedMasterCheck` fazia `repository.save` no interior de uma ação atômica e novamente no commit final. A sugestão do Mestre, o teste e os efeitos agora são gravados juntos ou todos revertidos.
3. **Segurança de transações aninhadas:** chamadas diretas de persistência em métodos auxiliares respeitam `_atomicActionDepth`; isso impede gravação prematura ao serem chamadas dentro de uma ação maior, preservando o comportamento das chamadas independentes.
4. **Fluxos sociais e competitivos:** um único commit persiste rolagem, gastos de Destino/Tensão, histórico social/avançado e status da sessão. Falha de disco reverte integralmente.
5. **Combate e ambiente:** começo de combate, iniciativa, troca de turno, ataque, queda e passagem de terreno intransponível passam a uma única transação por ação. Sem estado parcial após erro de gravação.
6. **Cinco aventuras:** testes de regressão de influência social durante aventuras publicadas, com três reaberturas por aventura sem escritas extras. Não substitui homologação contra os PDFs.
7. **Android/Kokoro:** oito fontes Java byte a byte iguais à base auditada; bundle WebView regenerado; sem alterações de motor de áudio.

## Gates executados

- **Node regular:** 1.851/1.851 PASS em 266 arquivos, `evidence/P152/NODE_REGULAR.txt`.
- **Stress:** 6/6 PASS, `evidence/P152/NODE_STRESS.txt`.
- **Novos testes P152:** 31/31 PASS, `evidence/P152/P152_FOCAL_TESTS.txt` (incluídos nos 1.851).
- **Kokoro/TTS:** 27/27 PASS, `evidence/P152/KOKORO_TTS_REGRESSION.txt`, testes de código e integração, **não áudio em dispositivo**.
- **Cinco aventuras:** 5/5 em validação estrutural, transacional e reabertura, `evidence/P152/RELEASE_AUDIT.json`.
- **Java:** gate sintático PASS para oito fontes; símbolos Android indisponíveis sem SDK, `evidence/P152/JAVA_SYNTAX.txt`.
- **Patch:** `BRASEIRO_STAR_WARS_EDGE_R54_P151_TO_P152_SOURCE.patch` reproduz os oito arquivos de código/testes alterados ou adicionados; `evidence/P152/PATCH_REPRODUCTION.txt`.

## Pendências sem certificação

- **AoR:** 247/360 posições com consumidor mecânico, **113 pendentes**; nenhuma posição adicional reivindicada em P152.
- **Expansões EotE/AoR:** cotejo integral com livros oficiais e implementação de mecânicas parciais pendentes.
- **Cinco aventuras:** comparação de todas as cenas, bifurcações, encontros, recompensas e combate oficial de *Além da Orla* com os PDFs ainda não realizada.
- **Android:** não houve compilação Gradle de APK (Android SDK/runner ausente neste ambiente) nem instalação e homologação física no S23 Ultra/A57.

## Reproduzir

Na raiz do ZIP, executar:

```bash
node tools/build-ui-bridge-bundle.mjs
node tools/audit-p152-release.mjs
node --test tests/uae-p152-atomic-composite-recovery.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
```

Retomar a partir de **P152**. Prioridade seguinte: fechamento das 113 posições AoR com texto de regras validado, homologação das cinco aventuras contra PDFs e build Gradle/instalação física sem regressão Kokoro.
