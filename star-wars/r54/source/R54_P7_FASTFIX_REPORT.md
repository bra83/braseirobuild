# STAR WARS — EDGE OF THE GALAXY — R54 P7 FASTFIX

Data: 2026-09-18
Base: `STAR_WARS_EDGE_OF_THE_GALAXY_ANDROID_STUDIO_R54_DEVICE_P6_FINAL_8dd2f49.zip`

## Correções aplicadas

- Removido o fallback fuzzy de imagens: entidades só recebem asset por correspondência exata.
- Cobertura visual exata do catálogo pessoal Core: 31 armas + 7 armaduras + 22 itens gerais.
- Criados/integrados assets específicos adicionais para:
  - Pistola blaster leve
  - Faca de combate
  - Sabre de luz
  - Armadura laminada
  - Comunicador
  - Scanner geral
  - Macrobinóculos
  - Mochila
  - Equipamento de escalada
  - Remendo de reparo de emergência
  - Lanterna de fusão
  - Rações
  - Olho do Avaliador
- Assets adicionais de veículos integrados: TIE/ln, Y-Wing BTL-A4 e YT-1300, além dos já existentes.
- 82 locais agora possuem mapa de localização real baseado no mapa galáctico do projeto, com marcador do local.
- Shantipole preserva seu mapa local dedicado.
- A UI diferencia explicitamente `Mapa local` de `Mapa de localização galáctica`; não mascara recorte galáctico como mapa local.
- Removido o texto técnico `detalhes factuais adicionais devem ser estabelecidos pelo Mestre com apoio do RAG`.
- Seed de mundo vivo deixa de fixar o mesmo primeiro NPC/facção em toda aventura; NPCs/facções são rotacionados por nova seed.
- `Nera Voss` e `Kerr Voss` não permanecem como NPC universal/fixo.
- `gradlew` e `MainActivity.java` normalizados para LF.
- Manifesto/corpus RAG regenerados e sincronizados.

## Validações executadas

### Gate focado
15/15 PASS:
- r54-device-p6-audit
- r54-visual-semantic-audit
- gradle-wrapper-bootstrap
- pdf-policy-index-race-r20
- rag-sync

### Regressão de estado/mundo/catálogo
53/53 PASS, incluindo:
- catálogo veicular Core
- catálogo pessoal Core
- sandbox/mundo vivo
- estado/export/import
- World R15
- UI sandbox R33
- mixed campaign sandbox
- novo gate P7 de imagens/mapas/seed/fallback

## Limitação do ambiente

A tentativa de `:app:assembleDebug --offline` não pôde concluir porque a distribuição Gradle 8.13 não está presente no cache local e o ambiente de execução não possui acesso a `services.gradle.org`. Isso não é falha de código detectada pelos testes; o ZIP Android Studio foi preservado para compilação normal no Android Studio/ambiente com Gradle disponível.
