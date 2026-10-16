# Braseiro Star Wars VTT — R54 P147 | 11/10/2026

## Base, entrega e escopo

- Base recuperada: `BRASEIRO_STAR_WARS_EDGE_R54_P146_SOURCE_CHECKPOINT.zip`.
- Checkpoint integral Android Studio: `BRASEIRO_STAR_WARS_EDGE_R54_P147_SOURCE_CHECKPOINT.zip`.
- `versionCode=10147`; `versionName=1.0.147`; shell `data-version=1.0.147`.
- Bundle offline recompilado de fontes canônicas, 78 módulos.

## Código implementado

1. **Identidade persistente das execuções**: `publishedAdventureRunSerial` incrementa transacionalmente a cada START nas cinco aventuras. `runKey=run:<adventureId>:<serial>` substitui timestamps como prova de identidade da execução. Reinícios no mesmo milissegundo não colidem. Checkpoints P146 sem contador continuam compatíveis; recibos históricos permanecem preservados.
2. **Prova causal UAE↔recibos P147**: evento `PUBLISHED_COMMAND_COMMITTED` e recibo recebem `proofVersion=2`; o journal vincula a assinatura canônica do comando, a sequência do recibo e a chave da execução. Importação, migração e validação rejeitam adulterações ou perdas em qualquer lado. Eventos/recibos P146 continuam aceitos sem reescrita.
3. **Correção real de ACK perdido no encerramento de combate**: `finishCombat()` verifica e recupera primeiro a ação pendente do outbox. Se o combate publicado já terminou e o ACK se perdeu, não cai no caminho de combate sandbox nem repete efeitos. Também protege quando a ação pendente é uma escolha.
4. **Regressão integrada**: 18 novos testes executam as cinco aventuras, três reinícios congelados no mesmo milissegundo, reabertura, idempotência, adulterações de recibo/journal, importação, compatibilidade P146, perda de ACK e rollback de disco.

## Evidências verificadas

- `evidence/P147/NODE_SUITE_FULL.txt`: **1720/1720 PASS**, 267 arquivos (261 regulares, 6 stress).
- `evidence/P147/P147_FOCAL_TESTS.txt`: **18/18 PASS**.
- `evidence/P147/KOKORO_TTS_REGRESSION.txt`: **27/27 PASS** (testes de código).
- 128 fontes JS/MJS, 22 JSON com sintaxe válida; 8 fontes Java byte a byte iguais a P146.
- Integridade SHA-256 do ZIP: arquivo `.sha256` ao lado do ZIP.

## Limites não homologados

- AoR permanece em **224/360 posições executáveis**, **136 pendentes**; sem inventar mecânicas ou alegar expansão certificada.
- As cinco aventuras têm integração e persistência exercitadas, mas **não** comparação integral com PDFs originais.
- *Além da Orla*: combate oficial ainda não certificado/implementado.
- Sem SDK Android nesta execução: **APK não compilado**, sem homologação física, sem áudio Kokoro real no S23 Ultra/A57.

## Reproduzir

Na raiz do projeto: `node build-ui-bridge-bundle.mjs`; `python tools/run-node-suite.py all`; para o subconjunto TTS, veja a lista no log. Para APK usar Android Studio ou o runner BRELGA com SDK e Gradle configurados.
