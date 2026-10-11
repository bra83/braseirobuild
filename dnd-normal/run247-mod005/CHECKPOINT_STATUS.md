# Checkpoint status — BRASEIRO D&D 5e NORMAL / RUN245

## Identidade e CI

- Fonte RUN245: SHA-256 `7f69f02e5b77a100cfd517b80c4de96c10d0b33cf3284d78112ebd3755e0222b`, 140,317,801 bytes, 1,213 entradas, ZIP CRC PASS. Identidade `com.braseiro.dnd5e`, versionCode 606545, DB61, Backup21. HYBRID_FASTPLAY não foi usado.
- Último build BRELGA: [run 38061684317](https://github.com/bra83/braseirobuild/actions/runs/38061684317), commit `24364e46d74768131ab569989ba64ab402efd6d3`, branch `dnd/run245-normal-g05-codex`. `compileDebugJavaWithJavac`, `testDebugUnitTest` (76/76, zero falhas/erros/ignorados), `assembleDebug`, verificação de identidade e assinatura APK v2: PASS.
- APK: `com.braseiro.dnd5e`, versionCode 606545, versionName `6.6.14-r245-g05-source-debug`. Certificado é o debug “Braseiro Stable Debug”; continuidade da chave de atualização NÃO verificada. Evidência íntegra (ZIP CRC PASS), SHA-256 `b4dd27e40341b0783cd7c23f7a5ec8c8846056f2bb2ee7b0758930c64e7058fa`: [release BRELGA 38061684317](https://github.com/bra83/braseirobuild/releases/tag/run245-normal-brelga-38061684317).
- JUnit XML do run: 28 classes, 76 testes, 0 falhas, 0 erros, 0 ignorados. Todos os quatro testes de movimento narrado (incluindo replay), riders G05, backup, progressão, proposta de companheiro e catch-up passaram nessa execução.

## Disposição dos gates

- G01 homologado para identidade/proveniência do checkpoint.
- G02/G03 homologados somente para o caminho transacional de ataque simples com arma equipada coberto pelos 3 testes dirigidos.
- G04, G06–G14 parciais; ver matriz com limites por gate.
- G05 permanece aberto e não homologado. Foram revistos 280/280 registros auxiliares; há quatro handlers fonte-bound com casos focados aprovados, mas 276 issue rows sem executor específico aprovado.
- G15 homologado no escopo de compilação debug, suíte unitária, assembly, identidade do pacote e assinatura debug v2. Não comprova chave de update.
- G16 bloqueado por continuidade da chave de update e ensaios em aparelhos Galaxy S23 Ultra/A57 ausentes.

## Reauditoria G05 e OCR XMM

- 149 registros são RHW (*Ravenloft: The Horrors Within*, 2026); 131 são XMM (*Monster Manual*, 2025). RHW 2026 foi mantido distinto de *Van Richten’s Guide to Ravenloft* (2021).
- 5e.tools bestiary/books foi usado apenas como fonte auxiliar. As 280 linhas e 266 pares criatura/ação foram cotejados por identidade, ação, edição/código, página e fatos mecânicos expostos: ataque/salvaguarda, CD ou escape, duração, condição, alvo e efeitos. Os CSVs registram o grau de confiança e lacunas; não se copiou texto integral dos livros.
- PDF RHW exato não foi localizado nos 256 itens diretos do Drive. O PDF XMM de 277,647,654 bytes excedeu o limite de download bruto do conector (HTTP 413; limite 268,435,456 bytes). O Drive retornou 1,066,072 caracteres de OCR ruidoso. Busca local de rótulos perto das páginas estimadas encontrou nome e ação em 87/131 linhas, só nome em 20/131, e nenhum localizador confiável em 24/131. Isso não valida DC, salvaguardas, duração, condições ou efeitos: todos seguem explicitamente não homologados pelo OCR. O detalhe por linha está em `G05_EVIDENCE/qa-ci/G05_XMM_PDF_OCR_CROSSCHECK_131.csv`.
- 18 linhas retêm expansão de magias ou ambiguidade documentada. Com a regra de não inventar dados, nenhuma dessas lacunas foi preenchida por inferência.

Matriz: `COVERAGE_MATRIX_G01_G16.csv`. Auditorias e material de entrega: `GATE_AUDIT_G01_G16_RUN245_20261010.md`, `G05_EVIDENCE/G05_SOURCE_AUDIT_RUN245.md`, `DELIVERY_MANIFEST.json`.
