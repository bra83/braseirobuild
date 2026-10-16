# OUA — PRONTIDÃO PARA BUILD — 16/10/2026

## Objetivo

Registrar o estado de pré-build do Braseiro O Um Anel 2e.

Este documento não altera código, mapa, manifesto, saves, TTS, Gemini, imagens ou regras existentes.
O objetivo é deixar claro o que deve ser executado antes de compilar e testar o APK.

## Fonte canônica

- Repositório: https://github.com/bra83/braseirobuild
- Branch base: `oua/base-playable-20261014`
- HEAD verificado: `dfd1fd7d0e5ef71d7f93562891f89ce95cd56037`
- Fonte canônica: release `OUA-v4.9.211`
- ZIP completo Android Studio 4.9.211:
  https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.211/BRASEIRO_O_UM_ANEL_4.9.211_ANDROID_STUDIO_COMPLETE.zip
- APK 4.9.211:
  https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.211/BRASEIRO_O_UM_ANEL_4.9.211_STRIDER_OPENING.apk

A pasta `android/` da raiz do monorepositório não deve ser tratada como fonte canônica do OUA.

## Estado funcional confirmado

Os seguintes sistemas já estão implementados e devem ser preservados:

- Mapa hexagonal e manifesto de runtime.
- Planejamento de rota no Atlas.
- Movimento restrito ao próximo hex da rota planejada.
- Jornada manual: papéis, montaria, marcha forçada, Guia, Marcha, Evento e encerramento.
- Evento de Jornada vinculado ao hex, região, terreno e Dado de Façanha.
- Bloqueio de movimento enquanto houver rolagem, evento ou etapa pendente.
- Descanso Curto e Prolongado via `resolve_rest`.
- Vigilância do Olho: valor inicial, limiar por região, incremento por rolagem de 11 e Revelação.
- Persistência transacional com `saveMasterTransaction`, `saveCanonicalTransaction` e espelho de emergência.
- Proteção contra toque duplo e rolagem duplicada via `oncePerPendingAction`.
- Testes de rolagem única, evento único, falha sem redesenho e independência entre ações.

## Regra de execução

Patch incremental apenas.

Não reescrever:

- Mapa.
- Manifesto.
- Jornada.
- Fadiga/Descanso.
- Vigilância do Olho.
- Save.
- Combate.
- TTS.
- Gemini.
- Geração/importação de imagens.
- Campanha persistente.

Nenhuma nova regra deve ser implementada sem falha comprovada em auditoria ou teste.

## Cadeia obrigatória antes do build

1. Extrair o ZIP canônico `OUA-v4.9.211`.
2. Confirmar o SHA256 do ZIP:
   `ce636e4059402d3294aefc661cc56658c41136a287fc44e1ed7e01c40381c058`
3. Aplicar os patches da branch `oua/base-playable-20261014` sobre essa fonte.
4. Confirmar que o patch `ci/oua-base-20261014/apply-manual-roll-once.mjs` foi aplicado.
5. Confirmar que `lib/rules/oncePerPendingAction.ts` existe.
6. Rodar `ci/oua-base-20261014/manual-roll-once.test.ts`.
7. Rodar as auditorias BRELGA disponíveis na branch.
8. Registrar comandos, saídas, ambiente e resultado em evidência de QA.

## Gates antes de compilar

Só considerar pronto para compilar se:

- `OUA_BASE_MANUAL_ROLL_SINGLE_FLIGHT_APPLIED` for confirmado.
- `OUA_BASE_MANUAL_ROLL_TEST_PASS count=4` for confirmado.
- Nenhum anchor obrigatório falhar.
- Nenhum teste de regressão falhar.
- O manifesto do mapa carregar sem erro.
- O save/reabertura não duplicar rolagens, eventos ou consequências.
- Nenhum arquivo da fonte canônica for substituído por snapshot antigo.

## Build no Android Studio

1. Abrir o projeto Android gerado a partir da fonte canônica + patches.
2. Rodar sincronização Gradle.
3. Executar build de debug.
4. Instalar em emulador ou aparelho.
5. Testar:
   - Criar/continuar campanha.
   - Abrir mapa.
   - Planejar rota.
   - Iniciar Jornada.
   - Rolar Marcha.
   - Aplicar Marcha.
   - Gerar Evento.
   - Resolver Evento.
   - Avançar hex.
   - Descansar.
   - Fechar e reabrir o app.
   - Confirmar que nenhuma rolagem ou evento foi duplicado.
6. Somente depois gerar APK release assinado.

## Status

- Código das regras de Jornada/mapa: implementado.
- Proteção anti-duplicação: implementada.
- Persistência: implementada.
- Novas regras: não necessárias nesta etapa.
- Próxima ação: aplicar patches, rodar testes/auditorias e compilar.
