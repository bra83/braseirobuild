# BRASEIRO OSE

Projeto Android Studio do BRASEIRO OSE, mobile-first, com perfis separados de **Old-School Essentials Classic Fantasy** e **Advanced Fantasy**.

## Estado desta entrega

**Release local 1.0.0 — fechamento funcional concluído nos gates executáveis deste ambiente.**

Implementado e integrado:

- criação nativa Classic/Advanced, loadout, inventário, CA, carga, XP e progressão dentro da evidência disponível;
- loop de SESSÃO narrative-first com `PLAYER_REACTION` como único canal intencional de mutação e `GM_HELP` zero-delta;
- combate, encontro, movimento, luz, magia/runtime, tesouro/XP e fadiga de viagem;
- dungeon procedural determinística e exploração state-first;
- hexcrawl Geometry V1, coerência geográfica, viagem diária, perder-se, encontros, forragear, caçar, descanso e marcha forçada;
- settlement state-driven, NPCs persistentes, memória e scheduler causal de mundo;
- persistência versionada, checkpoints, idempotência, backup/restore e contratos de recovery pós-crash;
- AppViewState único para SESSÃO / MAPA / FICHA / COMPANHIA;
- visual 415×915 aprovado e preservado; MAPA/Geometry V1 bloqueados contra redesign;
- 3 decisões contextuais por cena, Gemini texto/cena, Gemini TTS de narração visível, retratos e miniaturas como apresentação-only.

## QA de fechamento

- 33 sessões finais pelo Rules/Referee runtime real: PASS;
- 33 sessões pelo SessionEngine com retries idempotentes: PASS;
- Classic wilderness encounter corpus: 960/960 PASS;
- Advanced encounter corpus: 1.600/1.600 wilderness + 360/360 dungeon PASS;
- dungeon deterministic corpus: 1.024/1.024 mundos PASS;
- stress causal: 300 NPCs + 300 eventos/missões PASS;
- persistência/recovery contracts + migration/checksum/idempotency: PASS;
- visual regression: 7/7 capturas em 415×915 PASS;
- handoff contracts: 17/17 PASS;
- web local build + JS/JSON/static checks: PASS;
- secret scan: PASS, sem chaves Gemini literais.

## Gates externos explicitamente fora desta entrega

Neste ambiente não é possível executar o Gradle/Android toolchain completo, Room/Robolectric real, emulador, APK/AAB ou teste em dispositivo físico. Por decisão do usuário, esses itens **não bloqueiam o fechamento local** e nenhum APK/AAB é gerado. O teste real das APIs Gemini também depende das credenciais cadastradas pelo usuário no aplicativo.

## Regra de fonte

OSE Rules Engine é a única autoridade mecânica. Classic e Advanced permanecem isolados. Ausência de evidência é preservada como `MISSING_EVIDENCE` / arbitragem do Mestre; o runtime não preenche lacunas com D&D, B/X histórico, Pathfinder, memória do modelo ou fallback silencioso entre perfis.
