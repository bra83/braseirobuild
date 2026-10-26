# USER REQUEST PATCH — QA

Data: 2026-09-08
Escopo: correções visuais/funcionais pedidas diretamente pelo usuário, sem geração de concept.

## Implementado na baseline Android/Compose

- Bottom navigation global: 7 células, 61 dp, ícones ciano maiores, sem divisórias verticais, estado ativo iluminado; componente único via `AppShell`.
- Header/clima: removido o container visual do clima; permanece somente informação + ícone.
- KID:
  - `TROCAR FOTO` diretamente na ficha;
  - `IMPORTAR JSON` diretamente na ficha;
  - `EXPORTAR JSON` diretamente na ficha;
  - retrato escolhido é normalizado para JPEG <= 512 px, persistido em Base64, incluído no JSON da ficha e no snapshot local.
- Biblioteca automática de ilustrações:
  - 24 imagens canônicas empacotadas no Android;
  - seletor determinístico por local + narrativa + clima + luz/hora;
  - não altera mecânica/estado, apenas escolhe a ilustração exibida;
  - fallback para `visualAssetKey` explícito ou `scene_loop_gate`.

## Provas executadas

- `tools/static_audit.py`: PASS — 13/13 telas, 0 drawables ausentes.
- `tools/product_feature_audit.py`: PASS.
- motor/regressão já compilado: 113/113 PASS.
- `SceneIllustrationSelector`: compilado isoladamente com Kotlin e validado em 24 contextos distintos: 24/24 PASS, 24 assets únicos.
- `GameSnapshotCodec`: compilado isoladamente com Kotlin; retrato Base64 round-trip PASS.
- runtime web de prova visual 415×915:
  - 7/7 telas: `scrollHeight=915`;
  - 0 page errors;
  - 0 asset requests falhadas;
  - footer normalizado CENA/KID/GRUPO/MISTÉRIO/MAPA/MOCHILA/MENU: diff binário T16 = 0 em todas.
- biblioteca web dual Tales/Kids + Flood/Teens: 24 contextos únicos por perfil, PASS.

## Limite de ambiente

`./gradlew :app:compileDebugKotlin --offline` não pôde executar porque o wrapper ainda precisa baixar Gradle 9.3.1 de `services.gradle.org`, inacessível neste ambiente. Não é afirmado `ANDROID_DEVICE_PASS`.

## Revision — MAPA aprovado pelo usuário (2026-09-08)

- O mapa aprovado foi travado com a regra **1 POI = 1 ícone canônico**.
- A base visual passou a usar `map_malaren_clean3.png`: labels permanecem no raster ilustrado e não carregam um segundo ícone conflitante.
- Marcadores sobrepostos usam apenas os assets canônicos correspondentes:
  - Escola → `loc_school`
  - Casa de Lasse → `loc_home`
  - Praça Central / Loja do Kjell → `loc_shop`
  - Esconderijo → `loc_hideout`
  - Torre de Comunicação → `loc_radio_tower`
  - Loop Industries → `loc_loop_facility`
  - Estrada Fechada → `state_blocked`
  - Antiga Pedreira → `loc_quarry`
- O runtime web foi recapturado em 415×915 após a mudança: 7/7 telas com `scrollHeight=915`, 0 page errors, 0 assets falhados.
- Footer global permanece travado: diff T16 normalizado = 0 nas 7 telas primárias.
- Android `MapScreen.kt` foi atualizado para a mesma base limpa e para os mesmos nove marcadores canônicos.
- Hash da base `map_malaren_clean3.png` é idêntico entre web e Android: `d78e05ef21842c518df2a09ac230c8969dda180fa10ba1d9617cff3199e71d3c`.
- Auditoria estática pós-mapa: PASS — 13/13 telas, 0 drawables ausentes.
- Auditoria de produto pós-mapa: PASS.
- Regressão do motor reexecutada: **113/113 PASS**.

O mapa está **VISUAL_LOCKED_BY_USER**. Não redesenhar marcadores, labels, distribuição ou identidade dos POIs sem novo pedido explícito.

### MAPA runtime lock proof

- `docs/MAP_VISUAL_LOCK_QA.json`: PASS.
- 9/9 POIs usam exatamente o asset canônico previsto.
- 0 pins legados no DOM.
- 0 camada antiga de repaint de labels visível.
- base = `map_malaren_clean3.png`.
- 7 células no footer; altura total = 915 px.
- 0 page errors / 0 failed requests.
- Evidência visual: `docs/MAP_RUNTIME_APPROVED_415x915.png`.


## Footer Android — lock exato pós-auditoria

- `BottomNavigation.kt` não referencia mais os PNGs legados de navegação.
- Foram adicionados 7 vetores `nav_*_approved.xml` derivados das mesmas geometrias lineares do footer aprovado do runtime: CENA, KID/TEEN, GRUPO, MISTÉRIO, MAPA, MOCHILA e MENU.
- Shell Android: 61 dp, fundo `#001116`, borda superior `#28545C`, ícone 28 dp e linha ativa 3 dp.
- Rotas internas preservam a aba-pai: COTIDIANO/ENCRENCA/CLÍMAX/LOCAL → CENA; CONEXÕES/ENCERRAMENTO → MISTÉRIO; BIBLIOTECA → MENU.
- `tools/user_visual_lock_audit.py` e `docs/USER_VISUAL_LOCK_AUDIT.json`: **PASS**.
- Vetores XML: 7/7 parseados com sucesso.
- Regressão pós-patch: **113/113 PASS**.
- Runtime dual-profile: 24 estados capturados, altura 915 px, footer y=854/h=61 em todos, 0 page errors, 0 assets falhados.
