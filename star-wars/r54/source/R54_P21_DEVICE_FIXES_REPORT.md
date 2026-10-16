# R54 P21 — DEVICE FIXES REPORT

Data: 2026-09-19

## A. Baseline

- Entrada: `STAR_WARS_EDGE_OF_THE_GALAXY_R54_P20_WITH_ANDROID_FOLDER.zip`.
- SHA-256 da entrada: `483fa8e7158ceeb0cebbae58289d2ced35b70a632f5ee131d95579fa55c65f9c`.
- Continuidade incremental: nenhum projeto foi recriado; regras Star Wars, Mundo Vivo, Barbara e catálogos existentes foram preservados.

## B. Bugs reproduzidos e corrigidos

### 1. REINICIAR apagava personagem

O runtime removia diretamente `braseiro.starwars.edge.state.v1` e `braseiro.starwars.character.photo.v1`. O reset agora passa por `restartSandboxPreservingParty()`: cria um sandbox novo, mas preserva personagem/player, grupo, nave/veículos, modo de campanha e ajustes. O botão limpa apenas snapshots da aventura e sessionStorage; a foto do personagem não é removida.

### 2. Chave Gemini parecia não persistir

A persistência nativa já usa Android Keystore + fallback AES-GCM, mas a shell interpretava `BraseiroGemini.getStatus()` como se o host retornasse um wrapper `{ok,data}`. O host retorna o objeto de status diretamente. O adaptador da shell agora normaliza esse status para `{ok:true,data}`, portanto uma chave persistida volta a aparecer como configurada após recarregar/reabrir. Nenhuma chave é gravada no estado do jogo ou no ZIP.

### 3. TTS sem voz

- Quando Gemini TTS falha, o fluxo continua para Android TTS.
- Quando a bridge Android existe mas devolve indisponível, o runtime não encerra mais a tentativa: continua para Web Speech quando disponível.
- O TTS nativo tenta primeiro `pt-BR` e depois `pt`, cobrindo engines/OEMs que não expõem a variante regional exata.
- O encadeamento multibloco e os limites do P20 permanecem preservados.

### 4. Mapas quebrados no seletor

- O seletor principal agora recebe os 82 mapas locais empacotados do P6 além do catálogo V3.8 congelado.
- São 81 `generated_play_map` explicitamente rotulados como esquemas locais jogáveis e 1 `local_map` dedicado (Shantipole).
- O manifesto V3.8 histórico continua intacto com 18 mapas; o overlay P21 é opt-in no runtime, evitando quebrar os contratos R36–R46.
- Mapas externos não são mais exibidos por URL remota direta no Android antes de cache local; ao selecionar um externo, o app inicia o cache privado automaticamente.
- O contador da UI mostra a quantidade realmente disponível no seletor.

## C. Validação

- Gate focal P21: 13/13 PASS antes do reforço final; gate P21 ampliado: 9/9 PASS.
- Regressão regular final: **792/792 PASS**, 0 fail.
- Stress: **6/6 PASS**, executados individualmente sem mascarar timeout.
- Total de testes executados: **798/798 PASS**.
- Java syntax gate: **PASS**, 5 arquivos Java.
- JSON: **40 PASS**.
- XML: **2 PASS**.
- Secret scan: **0 hits**.
- APK/AAB dentro do source: **0**.
- Dois builders de `uiBridge.bundle.js`: saída idêntica, 58 módulos.

As seis evidências TAP de stress estão em `validation/p21/`.

## D. Build Android

O build foi tentado com:

`./gradlew :app:assembleDebug --offline --stacktrace`

Neste host o wrapper não encontra Gradle 8.13 local e tenta acessar `services.gradle.org`, que falha com `UnknownHostException`. Portanto não há APK desta rodada e não foi possível instalar/smoke-testar aqui.

## E. O que precisa ser retestado no aparelho

1. Salvar a chave Gemini, fechar o app por completo, abrir novamente e confirmar status configurado.
2. Iniciar TTS com Android e com Gemini/Charon; confirmar primeiro áudio e continuidade entre blocos.
3. Abrir o seletor de mapas, escolher mapas locais (Tatooine/Hoth/etc.) e confirmar renderização offline.
4. Selecionar um mapa externo e confirmar download/cache antes da exibição.
5. Criar/carregar personagem, iniciar sandbox, usar REINICIAR e confirmar que personagem, retrato, grupo e nave permanecem.

Esses cinco itens dependem de execução Android real; não foram convertidos em PASS estático.
