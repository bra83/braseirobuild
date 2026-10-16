# STAR WARS EDGE R54 — P79 — 2026-09-24

## Baseline
- Entrada exata: `BRASEIRO_STAR_WARS_EDGE_R54_P78_SOURCE_CHECKPOINT.zip`.
- Projeto não recriado.
- Fonte corrigida localmente; GitHub não é autoridade de fonte.
- EotE / AoR / MIXED, Duty / Obligation, Sob um Sol Negro P75 e pipeline TTS P76/P77/P78 preservados.

## Defeitos reais tratados
1. Aplicativo continuava muito lento no aparelho.
2. Conteúdo voltou a ficar por baixo das barras/menu do telefone.

## Causa de performance encontrada
- Existiam múltiplos `MutationObserver` globais concorrentes em P0, P1, P2, P6, mapa e vários blocos de `final-runtime.js`.
- Cada mutação de DOM podia disparar novas varreduras de cards, imagens, mapa e reparos, inclusive após mutações produzidas pelos próprios reparos.
- P2 mantinha um observer global embora seu `tick()` estivesse vazio.
- Mapa galáctico móvel carregava `galaxy_map.jpg` de 6.782.476 bytes / 5042x3263 mesmo no viewport móvel.

## Correções P79
### Performance
- Criado `r54-dom-scheduler.js`: um único scheduler/observer global, coalescido e throttled.
- Reparos P0/P1/P6/P31/mapa e seis blocos do final-runtime registram tarefas no scheduler em vez de observar o documento separadamente.
- Observer vazio do P2 removido completamente.
- Permanecem apenas observers locais específicos: mudança de zoom do card e texto selecionado do preview do mapa.
- Scheduler usa janela de supressão para não entrar em ciclo pelas próprias mutações e `requestIdleCallback` quando disponível.
- Clique só agenda reparo para controles acionáveis, não para todo evento arbitrário.
- `markBroken()` do P6 só revarre imagens quando a quantidade de imagens muda; eventos `error` cuidam do restante.
- Thumbnails/visuais P6 passam a usar `loading=lazy` e `decoding=async` quando apropriado.
- `body:before` deixou de usar `mix-blend-mode:screen` permanente, reduzindo custo de composição.
- WebView força camada de hardware, cache padrão e desativa offscreen preraster.

### Mapa móvel
- Adicionado `assets/maps/galaxy_map_mobile.webp` 2521x1632, 844.444 bytes.
- Redução de ~87,5% em bytes contra o JPG original de 6.782.476 bytes.
- Resolução escolhida ainda cobre aproximadamente o zoom móvel máximo 6x em viewport ~415 px.
- P0/P1/map-travel usam o asset móvel; JPG original permanece no projeto para preservação/fallback.

### Barras do telefone / safe viewport
- `MainActivity` agora usa `FrameLayout` raiz e aplica fisicamente os insets de system bars + display cutout como padding real do viewport.
- Em Android <30 usa stable insets para não confundir teclado com barra de navegação.
- Em Android 30+ usa `WindowInsets.Type.systemBars() | displayCutout()`.
- Inset é reaplicado em `onResume()` e `onWindowFocusChanged()` para impedir regressão após abrir/fechar UI do telefone.
- CSS Android safe vars são zeradas quando o viewport já foi fisicamente insetado, evitando padding duplo.
- `android:hardwareAccelerated=true` explicitado no manifest.
- `adjustResize` preservado.

## Versão
- Android `versionCode 10079` / `versionName 1.0.79`.
- Shell/runtime/bundle: 1.0.79 / R54 P79.
- Dois builders canônicos do uiBridge geram bundle byte-a-byte idêntico, 61 módulos.

## Evidência
- P79 performance/insets: 4/4 PASS.
- Gate mobile R38 + P79: 14/14 PASS.
- Regressão protegida P76-P79: 20/20 PASS.
- Regressão canônica final: 982/982 PASS, 0 FAIL.
- `node --check`: scheduler, runtime, device scripts, map-travel e bundle PASS.
- Build local Android foi tentado e bloqueado antes do Gradle por `UnknownHostException: github.com`; não é falha de compilação de fonte.

## Gate Android pendente
Compilar a fonte P79 congelada no BRELGA e só declarar APK PASS se:
1. source SHA/tamanho conferirem;
2. source gates P76/P77/P78/P79 passarem;
3. `assembleDebug` passar;
4. APK contiver P79, scheduler, viewport físico, P78 gameplay fixes e TTS;
5. `aapt` confirmar `com.braseiro.starwarsedge`, versionCode 10079, versionName 1.0.79;
6. `apksigner verify` passar;
7. APK/prova forem persistidos fora da cota de Actions artifact, preferencialmente Release + Google Drive.

## Teste físico prioritário depois do APK
- Abrir app com navegação de 3 botões e/ou gestos: menu inferior nunca deve ficar sob a barra do telefone.
- Alternar app / abrir painel do sistema / voltar: insets continuam corretos.
- Navegar CENA -> MAPA -> FICHA -> NAVE -> CENA: sem engasgo acumulativo.
- Abrir mapa galáctico e zoom/pan: resposta perceptivelmente mais rápida.
- Teste publicado: resultado persistente continua funcionando.
