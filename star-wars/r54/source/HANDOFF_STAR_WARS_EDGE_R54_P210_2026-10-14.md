# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P210

## Base obrigatória

**Não recomeçar.** P209 preservado: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P209_SOURCE_CHECKPOINT.zip`, SHA-256 `fe8575a55a933da9b2df1b892eae03e3d487203c27804295811b89441916374a`, 6.474 arquivos, CRC PASS. P210 é o novo checkpoint sobre P209. Não regredir Kokoro, EotE/AoR, saves ou os módulos P198–P209.

## Implementação REAL P210

Fonte consultada **diretamente** no PDF oficial SWE09 *The Jewel of Yavin*, Google Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, páginas impressas 92–94, seção Grand Galaxy Holo-Park. **Cotejo direcionado, NÃO certificação integral.**

Novo `app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs`:

1. Alderaan `alderaan_upheaval`: Desespero do ataque real do PJ, confirmado GM; aplica 2 de Tensão com `health.sufferStrain` e perde uma manobra ainda disponível com `combat.spendManeuver`. Não usa Desespero do NPC nem reaproveita custo já gasto.
2. Christophsis `christophsis_shatter`: Triunfo real ainda disponível do ataque acertado, consumido de `pendingPlayerSpend`, aplica `staggered` por uma rodada ao adversário real com `statusEffects.applyStatusEffect`.
3. Coruscant `coruscant_speeder`: Vantagem real ainda disponível do ataque acertado, consumida, aplica `disoriented` por uma rodada ao alvo real.
4. Kashyyyk `kashyyyk_wroshyr`: Vantagem real ainda disponível, consumida; adiciona um Contratempo defensivo ao pool REAL de ataque NPC à distância contra o PJ até a rodada seguinte. Teste focal confirma diferença de um dado no pool do ataque NPC, não apenas status decorativo.

Preflight exige aventura SWE09 ativa, nó `cloud-city-pursuit`, Holo-Park ativo, paisagem e rodada reais, confirmação explícita do Mestre, símbolos não gastos e IDs válidos. Recibos `JEWEL_HOLO_EFFECT` com contrato v1, replay, idempotência, bloqueio de duplicação por ataque, rollback atômico e três reaberturas. Saves P209 sem recibos permanecem aceitos.

Integração real em `js/core/controller.mjs`, `js/core/uiBridge.mjs`, `js/adventure/jewelOfYavin.mjs`, `js/adventure/jewelOfYavinOperations.mjs`, `r46/final-runtime.js`, `r46-shell.html` e bundle offline gerado. UI Android PT-BR com confirmação do Mestre e seleção da paisagem atual.

## Testes e integridade

- `tests/uae-p210-holopark-landscape-effects.test.mjs`: **9/9**.
- Regressão global FINAL por arquivo, **329 arquivos regulares, 3.032/3.032**, seis stress sequenciais **6/6**; **3.038/3.038 em 335 arquivos**. Sem suíte monolítica. `evidence/P210/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`.
- Sintaxe: 499 MJS, 2 JS runtime, 22 JSON, 2 XML, todos PASS; `evidence/P210/SYNTAX_AND_JAVA.json`.
- Android `versionName=1.0.210`, `versionCode=10210`, bundle offline **104 módulos**.
- **8/8 Java/Kokoro byte-idênticos ao P209**.
- Primeira regressão encontrou duas falhas históricas de versão do shell; `r46-shell.html` corrigido para 1.0.210 e toda a regressão FINAL reexecutada sem falhas. Alderaan foi corrigido para exigir Desespero do jogador, sem reutilizar resultado de NPC.

## Limites e próximos passos P211+

**Ainda não implementados:** os outros 12 efeitos narrativos de paisagem; facções de Arend e segurança de Grayson no Holo-Park (não inventar NPCs ou teleporte); modo narrativo de dois minutos com relógio confirmado; desfechos Arend/Aris/Lando e +5 Obligation: Favor somente após gatilhos oficiais; Arrow-7 avançado sem ficha completa; SWE02 New Meen/Geonosis/Teemo; cotejo integral de cinco aventuras e certificação individual de 360 consumidores EotE/AoR/expansões.

Próximo bloco: conferir símbolos e efeitos restantes em SWE09 pp.93–94, implementar em **código real** com gasto do símbolo, manobras, condições, modificadores e recibos. Depois integrar as outras facções com fichas oficiais, posição e gatilhos, sem substituir combate já resolvido. Fazer testes focais + regressão global por arquivo + seis stress; preservar Java/Kokoro e saves. Só gerar ZIP completo+SHA+delta+handoff após avanço real, upload Biblioteca e readback SHA/CRC. **Não declarar certificação integral sem cotejo.**

**Não executado:** SDK/Gradle/adb/APK/CI BRELGA, instalação física Android, push GitHub. GitHub `bra83/braseirobuild` apenas com fonte correta e acesso real verificável. Manter tarefa ativa até homologação comprovada.
