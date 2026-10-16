# Braseiro Star Wars VTT — R54 P139 (11/10/2026)

## Baseline e integridade

Checkpoint de origem: `BRASEIRO_STAR_WARS_EDGE_R54_P138_SOURCE_CHECKPOINT.zip`, SHA-256 conferido antes da extração. P138 não foi alterado. Nova versão `versionCode=10139`, `versionName=1.0.139`, shell offline `1.0.139` e bundle com **75 módulos**.

## Código real implementado

1. **Tempo de tarefas AoR/EotE com confirmação explícita**: `world/taskTimeLedger.mjs` implementa `recordResolvedTaskTime` puro, persistente e idempotente. Minutos não são confundidos com os ticks do mundo, que representam passos de eventos. Durações inválidas, divergências de ID, registros corrompidos e overflow são rejeitados antes de persistir.
2. **Controller + bridge Android**: `resolveSkillCheck` atribui um ID único a cada teste com duração; o resultado e o ID sobrevivem à reabertura. `commitLastTaskTime` debita somente o tempo ajustado pelos talentos efetivamente adquiridos, apenas após confirmação do jogador, e nunca duas vezes no mesmo ID. API `BraseiroUiBridge.commitLastTaskTime` expõe erros estruturados.
3. **Universal Adventure Engine e cinco aventuras**: após confirmação, `TASK_TIME_COMMITTED` é gravado no journal UAE com `idempotencyKey` durável para as cinco aventuras. O estado do mundo e o journal são preparados antes do commit; se o checkpoint UAE for rejeitado, não se persiste o débito. Triggers existentes podem consumir esse evento sem receber um encontro ou recompensa fictícios.
4. **UI Android**: botão `CONFIRMAR TEMPO NO MUNDO` após resolver a tarefa, exibição dos minutos acumulados e recuperação de confirmação pendente após reinício. Reenvios são inofensivos. O relógio de eventos não é avançado automaticamente.
5. **Teste legado intermitente**: o teste P94 de abandonar cobertura após avançar o turno agora fornece RNG determinístico, evitando uma falha aleatória por Ferimento Crítico durante a ação do adversário. Nenhuma regra de combate foi relaxada.

## Testes concluídos

- `python3 tools/run-node-suite.py --regular`: **1.474/1.474 PASS**, 252 arquivos, incluindo 22 testes novos P139.
- `python3 tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test --test-reporter=tap tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**.
- `node tools/build-ui-bridge-bundle.mjs`: **75 módulos**; `node --check` bundle e scripts: PASS.
- `node tools/java-syntax-gate.mjs`: **PASS sintático** para 8 fontes Java, 699 símbolos Android não resolvidos por ausência do SDK neste ambiente.
- Parse de JSON **22/22** e XML **2/2**: PASS.
- Verificador global `tools/verify-final-source.mjs`: **não concluiu** dentro do limite; as suítes regular/stress foram executadas independentemente e concluídas.

## Limites e próximos passos

- A integração de tempo P139 é deliberadamente explícita e não converte minutos em ticks nem altera ciclo de dia/noite sem uma regra de conversão homologada. Testes de perícia sem duração continuam sem débito.
- **149 posições AoR** continuam sem efeito mecânico completo (211/360 cobertas); esta rodada não adicionou talentos novos.
- Aventura *Além da Orla*: combate oficial ainda sem especificação certificada; `encounters:false` permanece. As cinco aventuras têm UAE integrado, não auditoria integral de conteúdo contra livros.
- Expansões EotE/AoR ainda exigem comparação com PDFs oficiais, não incluídos no checkpoint.
- APK Android **não compilado**: falta SDK/runner BRELGA, instalação em Galaxy S23 Ultra/A57 e teste físico de Kokoro/TTS. Os testes de Kokoro são de código, não áudio em dispositivo.

**Próxima base canônica: P139; fallback íntegro: P138. Não declarar conclusão total nem homologação Android.**
