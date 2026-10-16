# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P36
## 2026-09-22 — Sob um Sol Negro: subsistemas, combate persistente e UI

Continue deste P36. Não recrie o projeto. Não use GitHub como trilho de correção.

## Proveniência
- Baseline usada: `BRASEIRO_STAR_WARS_EDGE_R54_P35_SOURCE_CHECKPOINT.zip` do Google Drive.
- SHA-256 da baseline materializada: `7d2e591a3584cd33cf2483af80a642e04d4bd8b7a70dc445429c06de94683f2c`.
- Alterações incrementais sobre o P35.

## Concluído neste bloco
- Sob um Sol Negro ganhou estado persistente compatível com saves P35 antigos (`systems` migra de forma lazy sem resetar progresso).
- Perseguições `hot-pursuit` e `speng-pursuit`: avanço/pressão, rodadas, estados e consequências de Advantage/Threat/Triumph/Despair.
- Spyder: rodada funcional de sabacc usando teste narrativo real; créditos ganhos/perdidos atingem o personagem e ficam persistidos.
- Zelcomm: relógio local de segurança 0–6, lockdown e consequências persistentes.
- NovaCORE: risco industrial 0–6, estado crítico e Tensão real quando a rolagem produz consequências graves.
- Perfis funcionais de aventura separados do catálogo global: `black_sun_enforcer` e `kaato_leeachos` alimentam o combate sem contaminar a autoridade visual global.
- Encontro Black Sun e confronto Kaa’to integrados ao motor de combate existente.
- Kaa’to usa o NPC persistente `uabs.npc.kaato`; wounds/strain/critical/status sobrevivem ao combate.
- Corrigido bug real em `seedUnderBlackSunWorld`: o seed recriava NPCs e apagava status/combatState. Agora mergeia defaults sem apagar estado vivo.
- Resultado do encontro volta ao estado da aventura e alimenta flags/consequências.
- UI principal mostra perseguição, sabacc, segurança Zelcomm, risco NovaCORE e botão do encontro atual; início do encontro navega para Cena — Combate pessoal.
- Bridge expõe `beginPublishedAdventureEncounter`; bundle final reconstruído com 59 módulos.
- EotE/AoR/MIXED e separação Duty/Obligation preservados nos testes focais e de regras.
- Anti-spoiler continua restrito ao nó atual: sistemas e encontro publicados na UI são somente os do ponto atual; destinos/efeitos de escolhas continuam ocultos.

## Evidência
- `evidence/P36_FINAL_FOCAL.txt`: 27/27 PASS.
- `evidence/P36_RULES_COMBAT_REGRESSION.txt`: 44/44 PASS (combate, encontros, EotE/MIXED, Duty e progressão).
- `evidence/P36_VISUAL_AUTHORITY_REGRESSION.txt`: 15/15 PASS.
- `evidence/P36_SOURCE_GATES_FINAL.txt`: sintaxe + smoke dos perfis publicados PASS.
- Regressão ampla em 4 lotes: 823 subtestes; 813 PASS; 10 FAIL conhecidos da baseline P35; 0 FAIL novo P36 restante.
- Reproduções dos FAILs existentes no P35 intocado: `P36_P35_BASELINE_KNOWN_FAILURES.txt`, `P36_P35_BASELINE_FINAL_ANDROID_COMPLETENESS.txt`, `P36_P35_BASELINE_BATCH3_FAILURES.txt`.

## Falhas pré-existentes ainda abertas
1. Guard antigo do file picker JSON.
2. Guard legado da shell FINAL EotE contra símbolos AoR/Duty.
3. Contratos antigos de export/import `BraseiroFiles` na shell.
4. Guard antigo de sincronização do bundle esperando `modules:58`, embora o próprio P35 já tenha 59 módulos.
5. Três guards R38 de UX/Gemini já quebrados no P35.
6. Contrato R46 congelado em 53 métodos, já incompatível com a bridge P35 antes deste bloco.
7. Teste R54 de projeção de cena/Nera Voss já quebrado no P35.

## Compilação Android
- O marco tentou `./gradlew :app:assembleDebug --no-daemon` e o wrapper falhou antes da compilação por `java.net.UnknownHostException: services.gradle.org`.
- Não havia Gradle do sistema nem distribuição wrapper em cache para contorno offline.
- Evidência do bloqueio: `evidence/P36_ANDROID_BUILD.txt`.
- NÃO existe APK P36 validado neste checkpoint; não declarar build PASS.

## Próximo bloco recomendado
1. Corrigir os 10 guards/dívidas pré-existentes começando pelo contrato da bridge e `BraseiroFiles`, sem alterar semântica EotE/AoR/MIXED.
2. Continuar Sob um Sol Negro: efeitos canônicos das escolhas sobre facções/Obligation, estados finais de Kaa’to (capturado/morto/desaparecido) e consequências pós-missão no mundo vivo.
3. Expandir encontros intermediários onde fizer sentido, mantendo o mesmo motor de combate e sem mini-motores paralelos.
4. Auditar conteúdo/visual dos NPCs da aventura contra o catálogo visual já indexado e cache on-demand.
5. Quando houver Gradle local/rede, compilar e executar gates Android; só então produzir APK.
