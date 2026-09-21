# HANDOFF STAR WARS R54 P28 — MIXED + SOB UM SOL NEGRO

Data: 2026-09-21
Branch: star-wars-r54-p28-adventure-engine

## Baseline
Não recriar. Montagem autoritativa:
P25 Android Studio completo -> P26 validado -> patches P27 -> patches P28.

## Correção MIXED
O motor base já suporta simultaneamente Obligation e Duty em sessão MIXED. O defeito estava na ponte de criação, que tratava MIXED como EotE.

P28:
- EOTE continua usando criação EotE nativa.
- AOR continua usando criação AoR nativa.
- MIXED oferece as duas origens de criação no mesmo fluxo.
- Espécie e carreira precisam pertencer à mesma origem.
- EotE usa Obrigação e sua economia de criação.
- AoR usa Dever e sua economia de criação.
- Os bônus de Obrigação e Dever NÃO são somados na criação.
- A campanha MIXED continua mantendo ambos os subsistemas ativos em sessão e no grupo.

## UI de vínculos
- EotE: Obrigação.
- AoR: Dever + Contribuição.
- MIXED: Obrigação + Dever + Contribuição simultaneamente.
- Não reutilizar obligationType como representação de Duty no fluxo MIXED.

## Sob um Sol Negro
- hot-pursuit -> chop-talk;
- Episódio II aberto: Umbra Club / The Spyder / Zelcomm Tower;
- Setor 943 só é desbloqueado quando a pista correspondente é conquistada no Spyder;
- first-impressions permanece opcional;
- Setor 943 -> NovaCORE -> showdown;
- estado persistido em localStorage e retomável após reinício;
- Barbara recebe apenas o nó atual e contexto pertinente;
- consequências numéricas de Obrigação/Dever continuam no motor de regras.

## Evidência P28 — run 35635063307
VALIDAÇÃO FUNCIONAL/BUILD PASS antes da etapa de publicação:
- P27 herdado: PASS.
- P28 semantic/syntax gate: PASS: P28 MIXED native creation + persistent Under a Black Sun engine.
- Teste focado Sob um Sol Negro: 21/21 PASS.
- Regressão Node: 6 testes, 6 pass, 0 fail.
- assembleDebug: BUILD SUCCESSFUL.
- assembleRelease: BUILD SUCCESSFUL.
- APK debug: 53.398.543 bytes; SHA-256 0fca1c4e648c8da9bb6e0632424c56d4d7989ca99218b65352d3fe6a73ac55d4.
- APK release unsigned: 52.482.992 bytes; SHA-256 bf03c04d1556d5a9290dbd30e2faa46140158e202dca236c4311833258e82bf1.
- Persistência BRELGA: C:\actions-runner\artifacts\star-wars\r54\p28, 2 APKs + hashes + logs + handoff.

## Falha de infraestrutura isolada
O workflow 35635063307 ficou vermelho somente na etapa final `Publish P28 release via REST`. A exclusão/listagem de assets da Release via REST retornou HTTP 404 depois de todos os gates, builds, hashes e persistência local terem passado. Não tratar esse 404 como falha do código P28.

## Contorno preparado
Foi adicionado `.github/workflows/publish-star-wars-r54-p28-artifact.yml`, que evita a API de Releases: valida novamente os dois SHA-256, gate P28, engine 21/21, regressão fail=0 e Gradle debug/release=0 diretamente na pasta persistida do BRELGA; só depois publica `Braseiro-Star-Wars-R54-P28-VERIFIED` com actions/upload-artifact@v4. Não afrouxar essas validações.

## Próxima continuidade
1. Executar/confirmar o workflow de publicação do artifact e obter o artifact remoto.
2. Não refazer EotE/AoR/MIXED: P28 já tem evidência de PASS funcional e build.
3. Continuar Sob um Sol Negro a partir do motor persistente existente, enriquecendo conteúdo PT-BR sem substituir regras próprias do sistema nem expor informação futura ao Mestre/Barbara.
4. Manter gate P27 antes das mutações P28 e gate P28 depois delas.

## Critério de fechamento
Gate P27 + gate P28 + teste do engine + regressão Node + node --check + assembleDebug + assembleRelease no BRELGA + publicação verificável do artifact.
