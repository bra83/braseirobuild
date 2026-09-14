# BRASEIRO OSE — RETOMADA ATUAL

Data do checkpoint: 2026-09-14
Release: `1.0.0`
Status: `FINAL_LOCAL_RELEASE_PASS_EXTERNAL_ANDROID_GATES_EXCLUDED`

## Autoridade e locks preservados
- Continuidade construída sobre `BRASEIRO_OSE_POST_NATIVE_CREATION_BRIDGE_SNAPSHOT.zip` + deltas posteriores; nenhuma baseline antiga foi restaurada por cima.
- OSE Rules Engine permanece única autoridade mecânica.
- Classic e Advanced permanecem isolados, sem fallback silencioso.
- `PLAYER_REACTION` permanece o único canal intencional de mutação da sessão; `GM_HELP` e superfícies de inspeção são zero-delta.
- SESSÃO / MAPA / FICHA / COMPANHIA permanecem a navegação primária.
- Visual aprovado 415×915, pergaminho + navegação escura/dourada, preservado.
- MAPA permanece PASS/LOCKED; Geometry V1 preservada.
- Nenhum APK/AAB foi criado.

## Runtime fechado localmente
- Criação nativa Classic/Advanced → riqueza inicial → compra/equipar → `ACTIVE`.
- Catálogo de loadout por IDs canônicos; autoridade de preço/peso/restrição fora da UI.
- Bootstrap de mundo para Classic/Advanced com 120 hexes coerentes, dungeon alcançável, settlement, NPCs e eventos persistentes.
- Dungeon procedural determinística, exploração por posição canônica e conhecimento/visibilidade.
- Hexcrawl com viagem diária, terreno, perder-se, encontro, rações, forragear, caçar, descanso e marcha forçada.
- Penalidade de fadiga vencida ligada ao combate: -1 ataque / -1 dano, preservando dano mínimo 1.
- Classic encounter resolver completo para as 960 combinações auditadas.
- Advanced encounter resolver completo: 1.600 wilderness + 360 dungeon combinações, com IDs validados.
- Advanced monster registry: 327/327; v1.2 onde presente e v0.3 somente como gap source explicitamente marcado.
- Settlement state-driven; transições e descoberta por estado.
- NPCs com SpatialRef tipado, memória, conhecimento plausível e scheduler causal.
- Persistência: transition IDs idempotentes, checkpoints, migração monotônica, backup com checksum e recovery pós-crash narração-only sem replay de mecânica/RNG.
- AppViewState e bridge ligados ao estado canônico.
- 3 decisões contextuais por cena são prefill-only; campo livre permanece.
- Gemini texto/cena, TTS, retrato e miniaturas permanecem presentation-only; duas credenciais separadas e mascaradas em armazenamento seguro.

## QA final executada
- `FINAL_33_ACTUAL_REFEREE_PASS`: 33 sessões, 528 ações, 52 encontros; ClassicRefs=25, AdvancedRefs=27.
- `SESSION_ENGINE_33_PASS`: 33 sessões, 33 retries idempotentes, 33 narrações, 66 commits.
- `WILDERNESS_FATIGUE_COMBAT_PASS`: 200 pares de ataques comparados.
- `CLASSIC_WILDERNESS_RUNTIME_PASS`: 960/960.
- `ADVANCED_ENCOUNTER_RUNTIME_PASS`: wilderness=1600/1600, dungeon=360/360.
- `LOCAL_CLOSURE_GATES_PASS`: 1.024 dungeons determinísticas/alcançáveis, settlement PASS, 300 NPCs + 300 eventos resolvidos.
- `PERSISTENCE_CLOSURE_PASS`: migration/backup/checkpoint/portable/crashRecovery/idempotency PASS.
- Persistência Room source static: 12/12 PASS, sem destructive migration fallback.
- Visual regression final: 7/7 capturas 415×915 PASS; MAPA com Geometry V1; sem page/image errors; navegação e 3 sugestões preservadas.
- Handoff additions contracts: 17/17 PASS.
- Web local build, `node --check`, JSON parse e scripts Python: PASS.
- Secret scan: PASS, 0 chaves/API secrets literais.
- Projeto contém 0 APK, 0 AAB e 0 DEX.

## Evidência canônica incompleta preservada (não é preenchida artificialmente)
- `XP_SPLIT_REMAINDER = MISSING_EVIDENCE`.
- Classic potion roll 83–84 permanece `MISSING_EVIDENCE` porque a tabela fornecida salta de 79–82 para 85–86.
- Advanced pós-nível 14 permanece não autorizado onde a fonte não prova progressão.
- Situações delegadas pelo OSE ao árbitro permanecem `REFEREE_JUDGMENT`, não regra inventada.

## Gates externos fora do gate por decisão do usuário
- Gradle/Android full compile: não executável neste ambiente.
- Room/Robolectric real, emulador e dispositivo físico: não executáveis aqui.
- APK/AAB: deliberadamente não gerar.
- Gemini real smoke: requer as duas credenciais do usuário configuradas no app.

Esses itens externos não alteram o status dos gates locais executados; também não são reportados como PASS quando não foram executados.

## Próxima ação fora deste ambiente
Abrir o ZIP final como projeto Android Studio em ambiente com toolchain disponível para aceitação externa opcional. Não há outra fase local de implementação planejada.
