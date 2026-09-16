# WWN — Fechamento de fluxo de combate

Checkpoint: 2026-09-16

Implementado neste bloco:
- fachada de Jornada para Defesa Total;
- Recuar Lutando;
- Ir ao Chão / Levantar;
- Encerrar turno do lado dos PCs;
- Resolver lado dos inimigos;
- atualização automática da projeção mecânica após cada ação;
- regressão de bindings UI -> fachada -> CombatActionService.

Validação local sem Gradle/APK:
- 137 módulos JavaScript: sintaxe PASS;
- closure-regression: 33/33 PASS;
- deep-closure-regression: 33/33 PASS;
- large-scale-regression: PASS;
- turn-cycle-regression: PASS (6 novos controles).
