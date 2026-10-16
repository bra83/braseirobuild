# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P186

**Base:** P185 ZIP SHA256 `ac8110a640908ef0b59ed7b28709b26151663aa5e49e23ae623159eeefba4813`; preservar sem regressão.

## Código P186

- Novo `jewelGrandPrixDice.mjs`: rolagem real com `buildPositivePool` e `rollPool`, característica Agilidade e perícia Pilotagem Planetária do personagem; dificuldade Difícil, revés de tibannuck/beldon e revés de boia perdida no **pool efetivo**; melhorias opcionais de dificuldade até três para Desespero real. Validação de cada face de dado, pool e símbolos em replay, não confiar em resultados narrados.
- `jewelOfYavinOperations.mjs`: P186 recebe `pilotEvidence`/`vehicleId`, valida correspondência entre dados, sucesso, boia e perigos, preserva recibos P184/P185 sem prova de rolagem.
- `GameController.resolveJewelGrandPrixLeg`: ação atômica que exige veículo real ativo, aplica Tensão de sistemas e crítico real via `applyVehicleCritical` tabela EotE, inclusive modificador -20 de tibannuck; salva veículo e mundo juntos, com rollback para erros e duplicação.
- `GameController.adjudicateJewelOperation('race_roll', ...)` e `BraseiroUiBridge.resolveJewelGrandPrixLeg` expõem operação ao Android; bundle offline **87 módulos**. Android `1.0.186`.
- **Limite:** críticos são persistidos no estado de veículo do controlador, não há ainda certificação de todos os obstáculos/rivais nem comparação integral do livro. O modo manual anterior continua acessível para saves legados.

## Testes e integridade

- `tests/uae-p186-grand-prix-dice-vehicle-atomic.test.mjs`: **12/12**, incluindo replay antifalsificação, dificuldade efetiva, Tensão, críticos, 3 reaberturas, idempotência, rollback, bridge.
- `evidence/P186/TEST_SUMMARY.json`: **2751/2751 regulares + 6/6 stress = 2757/2757**, 311 arquivos por execução individual, não suíte monolítica.
- Java/Kokoro **8/8 byte a byte idênticos P185**. ZIP completo + SHA256 + delta verificável. Sem compilação APK nem teste Android físico.

## Próximas prioridades P187+

1. Completar corrida (rival AI, velker, gás), efeitos de veículos e operações bancárias/traição/fuga SWE09 contra PDFs.
2. New Meen, Geonosis e Teemo SWE02; concluir cinco aventuras cena a cena com PDFs e testes de migração/reabertura.
3. Certificar semanticamente AoR 360/360, EotE e expansões; consumidores declarados não equivalem a equivalência com livros.
4. Compilar APK no runner BRELGA somente com CI real verificável, testar instalação, persistência e Kokoro/TTS Android.
5. Fazer P187+ apenas com código substantivo, testes, ZIP Android Studio integral, SHA/delta/handoff e readback Biblioteca. Nunca inventar CI, APK ou certificação.
