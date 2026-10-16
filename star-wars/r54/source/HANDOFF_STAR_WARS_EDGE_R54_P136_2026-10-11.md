# BRASEIRO STAR WARS — R54 P136 | Checkpoint Android Studio

## Base e versão

Fonte canônica: `BRASEIRO_STAR_WARS_EDGE_R54_P135_SOURCE_CHECKPOINT.zip`, SHA-256 `36a5c5c890a677a12b91453303acd6938e9d211158685f6aae62e4f38a9613bf`. Extraído em pasta limpa, sem mesclar estados antigos. Versão Android `versionCode=10136`, `versionName=1.0.136`; WebView shell `1.0.136`. Bundle offline reconstruído com **74 módulos**.

## Código efetivamente implementado

1. **Universal Adventure Engine / checkpoint gate P136**: novo `adventureCheckpointGate.mjs` compartilhado pelo runtime e pela ponte das cinco aventuras. Verifica IDs e sequência crescente dos eventos, payloads, datas, histórico de cenas, gatilhos e limites, contadores, relógios, objetivos concluídos/falhados, consequências e integridade das coleções. Bloqueia alterações inválidas em transações e nas APIs públicas antes do commit; rollback mantém journal e estado anterior. `serialize()` valida antes de gravar.
2. **Restauração sem regressão**: imports v2 não normalizam silenciosamente progressos/relógios inválidos. Compatibilidade P128 preservada: `AdventureRuntime.restore()` recupera **apenas** o contador atrasado de um journal válido, sem renumerar IDs. A ponte publicada valida o snapshot bruto e continua rejeitando contador regressivo no save oficial.
3. **Segurança dos módulos e condições**: IDs que colidem com protótipos são recusados em nós, escolhas, objetivos, relógios e gatilhos. Valores booleanos, objetos, arrays, hexadecimais e valores não finitos não passam por números em efeitos. Comparações de clocks e counters malformadas falham fechadas.
4. **Cinco aventuras**: a ponte UAE usa o gate comum além da validação própria; testes integrados cobrem eventos, consequências, objetivos, clocks, salvamento, três reaberturas e 100 turnos por reabertura em cada aventura. Não foram inventados novos encontros nem recompensas.
5. **AoR**: o talento `LEVANTARSE DE UN SALTO` foi conectado ao consumidor já implementado `JUMP` (levantar rapidamente em combate). Cobertura declarada agora **178/360 posições AoR** com efeitos, **360/360 compráveis**. Restam 182 posições sem efeito completo.
6. **Kokoro/TTS**: nenhum código de voz alterado; suíte específica preservada.

## Evidências de testes

- `python3 tools/run-node-suite.py --regular`: **1.418/1.418 PASS**, 249 arquivos.
- `python3 tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test --test-reporter=tap tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**.
- `node --test --test-reporter=tap tests/uae-p136-checkpoint-atomicity-and-aor.test.mjs`: **32/32 PASS**.
- `node --check app/src/main/assets/vtt/js/core/uiBridge.bundle.js`: **PASS**; bundle **74 módulos**.
- Pós-bundle focal: **55/55 PASS**.
- SHA-256 e integridade do ZIP verificados na entrega.

## Pendências e limites

- **182/360** posições AoR ainda sem efeito completo; outras expansões EotE/AoR não certificadas contra PDFs primários (PDFs não estão neste ZIP).
- Encontros de combate oficiais de *Além da Orla* não implementados/certificados; capacidade permanece `encounters: false`.
- Não houve build APK nem homologação em Galaxy S23 Ultra/A57. Android SDK não está instalado neste ambiente. Executar build no runner self-hosted BRELGA ou Android Studio; validar instalação, fluxo EotE/AoR, persistência prolongada, 3 reaberturas, áudio Kokoro físico e cenas.
- A validação das cinco aventuras é de integração e persistência, **não** certificação narrativa integral contra os PDFs.

## Retomada

Usar exclusivamente o ZIP completo P136 como base, preservar P135 como fallback, executar suítes acima, obter PDFs primários e fechar as 182 posições restantes por efeito real (não apenas rótulo), depois compilar/homologar Android.
