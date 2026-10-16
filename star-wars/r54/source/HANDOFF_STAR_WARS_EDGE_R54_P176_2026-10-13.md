# BRASEIRO STAR WARS EDGE — R54 P176 · 2026-10-13

## Baseline e recuperação

- Base imutável: `BRASEIRO_STAR_WARS_EDGE_R54_P175_SOURCE_CHECKPOINT.zip` (P175).
- Checkpoint canônico: `BRASEIRO_STAR_WARS_EDGE_R54_P176_SOURCE_CHECKPOINT.zip` (P176).
- Projeto completo para Android Studio, `app/build.gradle.kts`: `versionCode=10176`, `versionName=1.0.176`.
- Bundle offline reconstruído com **83 módulos**, partindo das fontes `.mjs` locais.
- `app/src/main/java`: **8/8 fontes Java idênticas a P175**, inclusive TtsBridge/TtsChunker (hashes em `evidence/P176/KOKORO_JAVA_SOURCE_SHA256.json`).

## Implementação P176 — sete posições AoR novas e correção semântica

1. **Fuego Controlado / Fire Control:** manobra veicular real, bônus de +1 na silhueta do alvo para os ataques da nave na rodada, não acumulável; limita manobras e cobra 2 de Tensão de sistemas ou ação na segunda manobra; expira inclusive após troca de veículo.
2. **Diseño Familiar / Known Schematic:** manobra, teste real Difícil (3) de Conhecimento (Educação), uma utilização por sessão, recibo causal e resultado persistente; o Mestre precisa confirmar o edifício/nave e revelar os detalhes (não há mapa inventado).
3. **Comprar Información / Bought Info:** ação que troca o teste de Conhecimento por 1 sucesso, consome `50 × dificuldade` créditos, exige confirmação do Mestre e gera recibo causal; sem revelações automáticas inventadas.
4. **Pirata Informático Experto / Master Slicer:** separado corretamente de Skilled Slicer, 2 de Tensão para reduzir dificuldade de Computadores em 1, pelo fluxo já existente de perícias.
5. **Salida Térmica / Exhaust Port:** 1 ponto de Destino luminoso para ignorar Massive no ataque contra alvo que realmente possua Massive; consumo mesmo se errar.
6. **Impacto Directo / Dead to Rights:** ao acertar arma veicular, 1 Destino luminoso acrescenta `ceil(Agilidade/2)` ao dano de um impacto; erro não consome Destino.
7. **Impacto Directo Mejorado:** substitui o bônus anterior pela Agilidade integral, sem somar os dois.

**Correção de regressão semântica anterior:** `Pirata Informático Consumado` (Skilled Slicer) estava mapeado incorretamente para Master Slicer. Agora um Triunfo gasto em Computadores permite testes posteriores naquele sistema como manobras; há confirmação de sistema real, registro de controle, persistência e bloqueio para sistemas diferentes. Master Slicer é ativado apenas por sua própria compra.

Integração de estado: `aorIntelActions` em `master` com recibos causais e validação de restauração; campos de Fire Control e controle Slicer persistentes; ação atômica com rollback em falha de armazenamento; pontes Android PT-BR; painel de engenharia/informações/slicer/combate; compatibilidade com saves P175. A regra Massive foi adicionada ao cálculo real do custo de crítico veicular (inclusive quando o talento não é utilizado).

## Testes executados, evidências reais

- **2.549/2.549** testes regulares (295 arquivos).
- **6/6** stress (6 arquivos; incluindo sessões longas).
- **2.555/2.555** regressão total, 0 falhas (verificador global).
- **23/23** testes focais P176, incluindo cinco aventuras publicadas e três reaberturas cada, antifraude, rollback, Destino, Massive, custos e semântica Slicer.
- **27/27** Kokoro/TTS.
- Bundle 83 módulos, JS syntax 133 arquivos, Java syntax 8 arquivos, JSON 154, XML 2, secrets 0: gates aprovados pelo verificador global.
- Fonte completa validada em `validation/LATEST_SOURCE_VALIDATION.txt` e `evidence/P176/GLOBAL_SOURCE_VERIFICATION.txt`.

## Pendências reais — NÃO declarar 100%

- AoR: **342/360 posições com consumidor mecânico (95,0%)**; **18 posições pendentes**, listadas em `evidence/P176/AOR_PENDING_18_POSITIONS.json`. Métrica de presença de consumidores, **não** certificação semântica integral.
- Expansões EotE/AoR: certificação sistemática contra PDFs pendente.
- Cinco aventuras: integração estrutural e testes de persistência aprovados; comparação editorial completa com PDFs e campanha jogada ponta a ponta pendentes.
- Android: **APK não compilado**, sem instalação/homologação física. Ambiente sem Android SDK, adb e sdkmanager; Gradle 8.13 não disponível localmente. Usar runner self-hosted BRELGA após sincronizar este checkpoint ao repositório.
- Universal Adventure Engine: regressão de triggers/consequences/objectives/persistence passou; **certificação editorial e de jogo real ainda não concluída**.

## Fonte de regras e limites

As descrições dos talentos acima foram cruzadas com os resumos de regras FFG (Age of Rebellion Core pp. 143, 145–147, 151, 156) e referências públicas de talentos. **Não houve comparação exaustiva com todos os PDFs nesta execução.**

## Continuidade prioritária

Implementar as 18 posições restantes em famílias funcionais (social, combate, inventário e técnico), mantendo aquisição source-backed e consumidores reais. Depois fechar certificação das expansões e cinco aventuras contra PDFs, compilar no BRELGA e homologar três reaberturas no Android. Nunca substituir homologação por contagem de testes.
