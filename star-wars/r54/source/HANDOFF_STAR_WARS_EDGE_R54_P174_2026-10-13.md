# BRASEIRO STAR WARS VTT — R54 / P174 — CHECKPOINT DE CÓDIGO

**Data:** 2026-10-13  **Base:** P173, SHA256 `c0d5db0c49b660ede3f3a50f642d6dbf97c3c6c7454af4862a1c72039e931670`.
**Android:** `versionName=1.0.174`, `versionCode=10174`. **APK NÃO COMPILADO**.

## Implementação P174 — AoR Engineer / Mechanic, cross-system

- `SISTEMAS REDUNDANTES` (Redundant Systems, Core p.154): uma ação por sessão ativa, teste Fácil de Mecânica, com doador funcional do **mesmo perfil de catálogo**, sem consumo/dano ao doador; no sucesso e com aprovação do Mestre, o ativo danificado é reparado integralmente com custo zero. Modelos de mesmo tamanho/nível tecnológico, mas de catálogo diferente, permanecem **não suportados** até haver metadados dimensionais normativos.
- `PIEZA DEFECTUOSA` (Bad Motivator, Core p.142): uma ação por sessão ativa, teste Difícil de Mecânica, aprovação explícita do Mestre e falha persistente em dispositivo catalogado possuído. Armas afetadas ficam bloqueadas no equipamento, saque, preparo, recarga e ataque até reparo. Dispositivos inimigos e de cenário que não estejam no registro de ativos continuam **não suportados**; não inventar efeitos.
- Ambos exigem nó AoR realmente comprado (não apenas texto na ficha), campanha AoR/MIXED, sessão ativa, recibo causal único, idempotência, validação antiadulteração, rollback de teste/efeito/registro e migração de saves P173.
- UI Android PT-BR: seleção de alvo e doador, confirmação do Mestre, rolagem e recibo. Bridge offline recompilada: **82 módulos**.
- As cinco aventuras reais: testes de sessão publicada + ação mecânica + 3 reaberturas, preservando exatamente `publishedAdventureRuntime`. Não confundir com certificação editorial.
- Kokoro/TTS: oito fontes Java byte-idênticas a P173; nenhum modelo embutido.

## Evidências verificadas

- `python tools/run-node-suite.py --regular`: **2506/2506 PASS**, 293 arquivos.
- `python tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**.
- `node tools/java-syntax-gate.mjs`: **PASS**, 8 fontes Java.
- `node tools/audit-published-adventures-p148.mjs`: **PASS** (auditoria automatizada, NÃO comparação com os PDFs).
- `node tools/build-ui-bridge-bundle.mjs`: **PASS**, 82 módulos.
- `node --check` bundle, controlador e shell JS: **PASS**.
- `node tools/verify-final-source.mjs`: **NÃO CONCLUÍDO**, execução interrompida por timeout; NÃO declarar PASS.
- `./gradlew :app:assembleDebug --offline`: **BLOCKED_EXTERNAL** (`UnknownHostException: downloads.gradle.org`, Gradle 8.13 indisponível). Não houve APK, instalação ou teste físico Kokoro.

## Cobertura e pendências

- AoR Core: **325/360** posições com consumidor declarado, **35** posições sem consumidor (`evidence/P174/AOR_PENDING_35_POSITIONS.json`). A contagem é de *wiring*; não prova fidelidade semântica de cada talento.
- EotE/AoR expansões: certificação completa de regras contra os PDFs **pendente**.
- Cinco aventuras: integração/regressão automatizadas aprovadas; confronto editorial integral com os PDFs **pendente**.
- Universal Adventure Engine: cobertura funcional existente de gatilhos/objetivos/consequências/persistência, mas sem certificado global de semântica.
- A fonte espanhola disponível localmente nesta sessão é um PDF de 50 páginas digitalizadas, sem texto extraível; não é o Core integral. Foram cruzadas descrições de talentos com referências de Age of Rebellion Core p.142/154 e páginas de talentos da árvore; isso NÃO substitui auditoria integral do livro.
- Para o próximo bloco, atacar as 35 posições com execução source→engine→state→UI→test, e fazer build no BRELGA quando fonte estiver estável. Não modificar Kokoro sem teste físico.

**Estado:** P174 é um checkpoint de código Android Studio recuperável; NÃO é entrega final nem APK homologado.
