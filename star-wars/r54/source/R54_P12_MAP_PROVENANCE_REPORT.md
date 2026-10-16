# R54 P12 — MAP PROVENANCE HONESTY

Base: R54 P11 TTS UI Green

## Correção real
A auditoria encontrou uma inconsistência semântica importante: 81 imagens geradas a partir de perfis de local estavam registradas internamente com `strategy: local_map`, o que permitia à UI/testes chamá-las de “mapa local”/“mapa real”. Apenas Shantipole tinha proveniência de asset dedicado confirmada.

P12 separa explicitamente:
- `local_map`: mapa local dedicado confirmado;
- `generated_play_map`: esquema local jogável gerado pelo VTT;
- cartografia galáctica/referência continua separada.

A UI agora escreve “Esquema local de jogo” para os 81 gerados e “Mapa local dedicado” apenas quando a proveniência permite. Nenhum arquivo visual foi removido: os 82 locais continuam com superfície jogável.

## Fonte externa já integrada
O projeto já contém `galaxyGraphDarkElfR49.mjs`, que registra TheDarkElf007 como `TRAVEL_REFERENCE_ONLY`, com rotas/nós verificados e `mechanicalAuthority:false`. A fonte é útil para localização galáctica/rotas, mas não deve ser falsamente convertida em mapa local de cidade/planeta.

## Gates
- Gate focal mapas + TTS: 14/14 PASS.
- Regressão regular: **772/772 PASS**, 148 arquivos.
- O runner agora aceita `--regular` e `--stress`, preservando timeout de 180s e permitindo diagnosticar stress sem misturar os gates rápidos.

## Ainda não validado neste ambiente
- stress completo desta rodada: o host excedeu a janela de execução antes de produzir resultado confiável; não foi mascarado nem declarado PASS;
- APK Android: continua dependente de Gradle/cache/rede adequada;
- latência acústica real Charon/Gemini e sequência física no aparelho.
