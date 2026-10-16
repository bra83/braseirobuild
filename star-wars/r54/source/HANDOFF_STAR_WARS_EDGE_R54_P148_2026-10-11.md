# Braseiro Star Wars VTT — R54 P148 | 11/10/2026

## Baseline e preservação

- Base integral: `BRASEIRO_STAR_WARS_EDGE_R54_P147_SOURCE_CHECKPOINT.zip`.
- SHA-256 da base: `e88da0168d17df79c19bc31dd0d7b18f2c284d5d3864101a56a32a3172b190e4`.
- Novo projeto Android Studio: `BRASEIRO_STAR_WARS_EDGE_R54_P148_SOURCE_CHECKPOINT.zip`.
- `versionCode=10148`, `versionName=1.0.148`, shell offline `data-version=1.0.148`.
- Fontes Java/Kokoro preservadas **8/8 byte a byte**, nenhum modelo ONNX incluído.

## Implementação integrada

1. **Preflight semântico executável das cinco aventuras:** novo `adventureSemanticGate.mjs` valida a linguagem de condições, referências a cenas, relógios e objetivos, tipos de efeitos, deltas e gatilhos ANTES de abrir uma campanha. O gate está conectado à construção canônica de `publishedAdventureRuntimeModule`, usada pelo controlador, UAE, restauração e validação de campanha. Condições inexistentes não passam silenciosamente como cenas jogáveis.
2. **UAE: rollback integral de cascatas em transações:** loops de gatilhos que excedem o limite não deixam mais escolhas, recompensas, flags, relógios ou recibos parciais em `commitAction` e `transaction`. A execução inteira é revertida. Para compatibilidade, `dispatchEvent` isolado mantém o evento auditável `TRIGGER_CASCADE_LIMIT` já esperado pelos testes antigos.
3. **UAE: bloqueio de transações assíncronas:** `transaction(async ...)` e callbacks que retornam Promise/thenable são rejeitados e revertidos; o motor não pode confirmar o snapshot antes de um efeito assíncrono.
4. **Auditoria reproduzível:** `node tools/audit-published-adventures-p148.mjs` produz JSON com os cinco grafos, contagem de cenas/escolhas/testes/condições/efeitos/gatilhos, capacidades reais de encontros e a lista AoR ainda pendente. Esta auditoria é **estrutural e semântica, NÃO certificação dos PDFs**.
5. **Regressão de ponta a ponta:** 32 testes P148 cobrindo cinco aventuras, rejeição de módulos adulterados, cascatas cíclicas, rollback, reabertura e idempotência dos comandos Android.

## Testes e evidências

- `evidence/P148/NODE_SUITE_FULL.txt` — resultado integral da suíte Node, incluindo stress.
- `evidence/P148/P148_FOCAL_TESTS.txt` — testes focais P148.
- `evidence/P148/KOKORO_TTS_REGRESSION.txt` — regressão Kokoro/TTS (testes de código).
- `evidence/P148/ADVENTURE_SEMANTIC_AUDIT.json` — relatório reproduzível dos cinco módulos.
- `evidence/P148/VALIDATION.json` — manifesto de integridade e limites.

## Pendências reais (não declarar 100%)

- **AoR: 224/360 posições mecânicas executáveis; 136 pendentes.** P148 não altera esse número.
- As expansões EotE/AoR não foram comparadas integralmente com os PDFs originais.
- As cinco aventuras têm validação estrutural/semântica e testes de comandos, mas não comparação integral de cenas/recompensas/combates com PDFs.
- *Além da Orla* permanece com `encounters:false`: **não fabricar encontro oficial sem fonte verificada**.
- Android SDK ausente neste ambiente: APK não compilado, sem homologação em S23 Ultra/A57 ou reprodução real Kokoro.

## Reproduzir

Na raiz: `node build-ui-bridge-bundle.mjs`, `node tools/audit-published-adventures-p148.mjs`, `python tools/run-node-suite.py all`. Para APK, usar Android Studio ou runner BRELGA com SDK Android. O ZIP contém o projeto completo e os testes, não um APK instalado.
