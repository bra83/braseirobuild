# WWN — Continuação do fechamento de fonte — 2026-09-16

Este passe não depende de Gradle/APK/device.

## Implementado
- Expedição operacional na Jornada: forrageio WWN, alocação persistente de comida/água/lenha e exposição do estado de suprimentos.
- Recuperação operacional na Jornada: descanso noturno com evidências explícitas do jogador, primeiros socorros e recuperação pós-estabilização.
- Primeiros socorros não fabricam evidência: a UI só envia `hasHealersKit=true` quando encontra `HEALERS_POUCH` no inventário canônico.
- PlayerMechanicsFacade agora projeta inventário portátil suficiente para validações de UI e expõe forage/allocateForage sem duplicar regras.
- Regra de descanso não presume abrigo, alimentação ou conforto: essas condições agora são declaradas explicitamente na interface.

## Validação
- Todos os JS WebView/core passam `node --check`.
- Todas as suítes QA existentes continuam PASS.
- Nova suíte `expedition-health-regression.mjs`: forage + allocation + night rest PASS.
- Referências DOM da Jornada verificadas: zero IDs ausentes.
- Gradle/APK não foram usados como gate neste passe.
