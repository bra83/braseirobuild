# HANDOFF STAR WARS EDGE R54 P65 — 2026-09-23

## Baseline
P65 deriva diretamente do checkpoint fonte P64 autêntico do Google Drive. Sem recriação e sem GitHub.

## Correção estrutural
- Corrigido roteamento genérico do controller para aventura publicada ativa.
- `advancePublishedAdventure`, `resolvePublishedAdventureCheck`, `beginPublishedAdventureEncounter` e `continuePublishedAdventureToSandbox` agora inferem o ID da aventura ativa quando a UI não envia `adventureId`.
- Antes, ações genéricas da UI caíam silenciosamente no default `under-black-sun`, quebrando Além da Orla apesar de ela estar ativa.
- Fallback legado para `under-black-sun` continua preservado quando não há aventura ativa.

## Persistência / regressão
- Novo gate P65 cobre Além da Orla em controller real com LocalStorageRepository: avanço genérico, save/reopen, MIXED, Duty, Obligation, conclusão, reopen, transição para mundo livre e archive.
- Além da Orla não concede créditos nem altera Duty/Obligation indevidamente.
- Bundle offline reconstruído.

## Evidência
- focal P33/P36/P63/P64/P65: 31/31 PASS.
- regressão canônica completa: 927/927 PASS, 0 FAIL.
- node --check do bundle: PASS.

## Android
Não recompilado neste marco: mudança restrita ao controller JS/bundle; build Android anterior continua bloqueado externamente no Gradle Wrapper. Não mascarar como PASS.

## Próximo bloco
- smoke visual/mobile real do seletor e continuidade de Além da Orla;
- publicar encontros mecânicos de Além da Orla apenas onde houver perfil validado, sem inventar statblocks;
- ampliar tracker/epílogo/ramificações visíveis em PT-BR mantendo anti-spoiler.
