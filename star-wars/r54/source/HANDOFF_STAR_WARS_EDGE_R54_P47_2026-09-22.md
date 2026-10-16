# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P47
## 2026-09-22 — HARDENING SOB UM SOL NEGRO / ANTI-SPOILER / ROTAS NOVACORE

Baseline: R54 P46. Incremental, sem GitHub, sem recriação.

## Concluído
- Corrigido vazamento anti-spoiler no pedido ao Mestre: `currentUnderBlackSunMasterRequest()` agora só envia ações realmente disponíveis pelo gating atual. Antes, opções condicionais futuras podiam entrar em `suggestedActions` mesmo invisíveis ao jogador.
- NovaCORE consolidada em duas rotas mecanicamente distintas, não apenas cosméticas:
  - Escritórios/segurança: Computadores + Percepção.
  - Refinaria: Percepção + Frieza.
- Corrigida inconsistência entre os testes mostrados na UI e os testes adjudicados pelo motor nas rotas filtradas da NovaCORE.
- Índices canônicos dos checks são preservados; adjudicação procura primeiro o índice original e mantém fallback compatível.
- Mantidos EotE/AoR/MIXED, Duty/Obligation, epílogo, consequências persistentes e retorno ao mundo livre do P46.
- Bundle `uiBridge.bundle.js` reconstruído: 59 módulos.

## Evidência
- `P47_FOCAL.txt`: 9/9 PASS (P46 completion + novos hardening tests).
- `P47_BROAD_REGRESSION.txt`: 166 arquivos regulares, 855/855 PASS, 0 FAIL.
- `P47_JAVA_GATE.txt`: JAVA_SYNTAX_PASS.
- `P47_BUNDLE_SYNTAX.txt`: bundle JS syntax PASS.
- Novo teste: `tests/r54-p47-under-black-sun-hardening.test.mjs`.

## Android
Não recompilado neste checkpoint: nenhuma alteração Java/Gradle e o P46 já documentou bloqueio externo do wrapper Gradle. Não declarar APK/Android PASS.

## Próxima prioridade
1. Validar P46/P47 em Android assim que houver Gradle funcional/self-hosted disponível.
2. Fazer playthrough real de Sob um Sol Negro e corrigir somente defeitos observáveis.
3. Depois iniciar a segunda aventura publicada sobre esta baseline, sem alterar os contratos MIXED/Duty/Obligation e anti-spoiler.
