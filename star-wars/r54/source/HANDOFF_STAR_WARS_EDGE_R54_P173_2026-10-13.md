# BRASEIRO STAR WARS VTT — R54 / P173 — CHECKPOINT INTEGRADO

**Data:** 2026-10-13  
**Base comprovada:** `BRASEIRO_STAR_WARS_EDGE_R54_P172_SOURCE_CHECKPOINT.zip`  
**SHA-256 da base:** `edb4fb8ffbbe410a94f1cc49f029126d662f2ce0cbd190b71a385be883932978`  
**Versão Android:** `1.0.173` (`versionCode=10173`)  
**Status:** código e testes de integração aprovados; **APK não compilado**.

## Implementação real P173

1. **Negociação AoR com recibo de rolagem persistente.** `GameController.resolveMerchantNegotiation` cobra 2 Tensão *antes* de rolar Negociação e registra um recibo com sucesso/falha, personagem, campanha, sessão e ativo/pedido. Não aceita compra/venda com bônus de Mercador especialista sem recibo legítimo, compatível e não consumido no novo fluxo Android.
2. **Economia transacional.** `completeMerchantSale` e `completeMerchantAcquisition` consomem recibo uma única vez; a Tensão pré-paga não é cobrada novamente; compra fracassada não recebe desconto. Rollback integral em falha de gravação, sem duplicar créditos, itens ou gastos. APIs antigas de economia foram mantidas para compatibilidade, mas o novo fluxo do controlador/Android exige recibo.
3. **Persistência e validação.** Novo módulo `rules/merchantNegotiationReceipts.mjs`; ledger de até 32 recibos, eventos de emissão/consumo, validação de integridade cruzada, migração de salvamentos P172 sem inventar recibos e proteção contra repetição, adulteração, troca de personagem/sessão/ativo. Limite não descarta recibos pendentes.
4. **Android PT-BR.** UI real com botões para rolar Negociação de venda/compra, selecionar recibos persistidos, exigir confirmação do Mestre, concluir transações e exibir resultado. Bridge offline recompilado com **81 módulos**.
5. **Cinco aventuras.** Testes com cada aventura publicada real e transação de Mercador em campanha MISTA: o estado UAE da aventura permanece idêntico, mesmo após **três reaberturas**. Sem alterações inventadas no conteúdo editorial.
6. **Regressão.** Corrigidos seis testes antigos que exigiam exatamente 80 módulos, substituindo por gate monotônico `>=80` para permitir novos módulos sem enfraquecer a cobertura.
7. **Kokoro/TTS.** Oito arquivos Java preservados byte a byte (hashes iguais aos do P172).

## Evidências executadas

- `python tools/run-node-suite.py --regular`: **2.489/2.489 PASS** (292 arquivos; +23 testes P173).
- `python tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test ...tts...`: **27/27 PASS**.
- `node tools/java-syntax-gate.mjs`: **PASS** em 8 fontes Java.
- `node tools/audit-published-adventures-p148.mjs`: **PASS** (grafo/semântica automatizados, **não** comparação editorial com PDFs).
- `node tools/build-ui-bridge-bundle.mjs`: **PASS**, 81 módulos.
- `node --check app/src/main/assets/vtt/r46/final-runtime.js`: **PASS**.
- `./gradlew :app:assembleDebug --offline`: **BLOCKED_EXTERNAL** — `UnknownHostException: downloads.gradle.org`; distribuição Gradle 8.13 não disponível localmente.
- Verificação global `verify-final-source.mjs` **não foi concluída nesta execução** (interrompida pelo limite de execução); não a declarar aprovada.
- Evidências em `evidence/P173/`.

## Escopo pendente e próximo bloco

- AoR: **323/360 (89,7%)** posições com consumidor declarado, **37** restantes, lista `evidence/P172/AOR_PENDING_37_POSITIONS.json`. P173 melhora a certificação do talento Mercador especialista, mas não aumenta a contagem.
- Certificação integral das expansões EotE/AoR contra PDFs e comparação editorial das cinco aventuras: **pendentes**.
- Semântica completa de todas as posições AoR já contadas: **não certificada**.
- Android: compilação Gradle 8.13 em runner BRELGA/SDK, instalação, três reaberturas reais e áudio Kokoro físico: **pendentes**.
- Próxima execução: priorizar grupos funcionais dos 37 talentos (piloto/veículos, engenheiro, comando) somente após confirmar regras no material oficial, manter testes e gerar P174.
- Não alterar oito fontes Java Kokoro sem homologação de áudio real.

**Não declarar VTT finalizado ou APK homologado.** P173 é um checkpoint de código Android Studio completo e recuperável.
