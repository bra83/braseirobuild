# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P211

## Base e proibição de regressão

Continuar **P211 sobre P210**, sem reconstruir o projeto. Base P210: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P210_SOURCE_CHECKPOINT.zip`, SHA-256 `6e3e1aec577dc5c2ce72188909ec9b29d990cc9cca81ac54b616bc269a8bed8a`, 6.482 arquivos. Checkpoint P211: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P211_SOURCE_CHECKPOINT.zip`. Delta P210→P211, sidecar SHA-256 e evidências entregues separadamente. Preservar EotE/AoR, saves, P198–P210, Kokoro.

## Implementação REAL P211

PDF oficial *The Jewel of Yavin*, Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, pp.93–94, lido diretamente e cotejado apenas para os dois efeitos abaixo. **Não** declarar certificação integral.

- **Alderaan — thranta holográfica (`alderaan_thranta`):** exige Vantagem realmente disponível de ataque acertado e confirmação expressa do Mestre de embarque físico na plataforma da thranta. Consome uma Vantagem, registra recibo `JEWEL_HOLO_EFFECT` e acrescenta **um dado de Benefício** ao pool real dos ataques à distância nas **duas rodadas seguintes**; não beneficia corpo a corpo, não acumula nem inventa movimentação.
- **Christophsis — sombra de cristal (`christophsis_shadow`):** exige Vantagem real de ataque acertado; aplica **Defesa à distância +1** ao NPC real recém-atacado até a rodada seguinte. Altera o conjunto de dados efetivo do ataque contra esse NPC; não afeta defesa corpo a corpo, não acumula nem cria NPCs.

Implementação em `app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs`, integração real de dados em `js/core/controller.mjs`, interface Android PT-BR `r46/final-runtime.js`, bundle offline recompilado com 104 módulos. Os recibos verificam tipo, símbolos, origem, alvo, duração, embarque, ordem, ID, idempotência e replay. Rollback atômico e três reaberturas. Compatibilidade com saves P210 e testes anteriores.

**Cobertura total dos efeitos de paisagem: 6/16.** Os outros dez permanecem sob adjudicação manual, sem afirmar conclusão.

## Testes P211

- `tests/uae-p211-holopark-thranta-shadow.test.mjs`: **7/7** testes novos, inclusive alterações nos pools reais de dados, confirmação de embarque, adulteração de recibos, rollback e três reaberturas.
- Regressão global final **por arquivo**, não monolítica: **330 arquivos regulares, 3.039/3.039**, seis arquivos stress **6/6**. **Total 3.045/3.045 em 336 arquivos**. `evidence/P211/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`.
- Sintaxe: **500 MJS**, dois JS runtime, 22 JSON, dois XML, sem erros. `evidence/P211/SYNTAX_AND_JAVA.json`.
- **8/8 Java/Kokoro byte-idênticos ao P210**. Android `versionName=1.0.211`, `versionCode=10211`, bundle offline **104 módulos**.

## Pendências prioritárias P212+

Implementar os **dez efeitos restantes** de Alderaan, Christophsis, Coruscant e Kashyyyk, conferindo fonte e símbolos, respeitando mecânica real e sem inventar efeitos; integrar facções Arend/Grayson com presença física e fichas oficiais; modo narrativo Holo-Park com relógio real; Arend/Aris/Lando e Favor +5 somente sob gatilhos publicados; Arrow-7 avançado apenas com estatísticas verificadas; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.

**Não executado:** SDK/Gradle/adb/APK/CI BRELGA/homologação física, push GitHub, certificação integral. Não alegar que foram feitos. Gerar próximo checkpoint completo + SHA + delta + handoff apenas após avanço substancial, regressão global por arquivo + seis stress, upload Biblioteca e readback SHA/CRC. Manter tarefa ativa até homologação comprovada.
