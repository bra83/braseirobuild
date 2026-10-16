# HANDOFF — BRASEIRO STAR WARS EDGE R54 P95

Data: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P94_SOURCE_CHECKPOINT.zip` materializada do Google Drive.
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler e TTS preservados.

## OBJETIVO DESTE BLOCO
Executar uma onda grande de fechamento mecânico concentrada em tipos de teste que estavam parciais, resolução social e ações/reparos veiculares. Não promover attachments/qualidades/Signature Abilities sem cobertura mecânica suficiente.

## IMPLEMENTADO

### Testes assistidos
- assistência hábil combina a melhor característica e a melhor graduação de perícia entre personagem e assistente;
- assistência não-hábil mantém o pool do personagem e adiciona 1 Boost;
- resultado passa pelo mesmo `resolveSkillCheck`, portanto respeita talentos, estados, críticos, encumbrance, Destiny e persistência.

### Testes opostos
- pool negativo é derivado da característica/perícia da oposição:
  - maior valor determina o total de dados negativos;
  - menor valor transforma essa quantidade de Difficulty em Challenge;
- aceita oponente explícito ou adversário real do encontro;
- resultado completo é persistido em `advancedChecks`.

### Testes competitivos
- todos usam a mesma dificuldade;
- maior número de sucessos líquidos decide entre resultados bem-sucedidos;
- empate usa Triumph e depois Advantage;
- se ninguém obtém sucesso, é empate;
- empate total depois dos critérios permanece empate;
- resultado/comparação persistem.

### Resolução social dedicada
- Coerção, Engano e Liderança → oposição por Disciplina;
- Carisma → oposição por Frialdade;
- Negociação → Negociação ou Frialdade;
- combinações que tentam contornar a oposição permitida são rejeitadas;
- `social.lastInfluence` e `social.history` persistem e sobrevivem a reopen;
- shell R46 rota 01 ganhou painel de social/oposto/competitivo/assistência;
- não existe dependência de RAG durante a resolução.

### Veículos — Controle de Danos e Critical Hits
- Controle de Danos está alcançável pela shell R46 e continua usando teste real de Mecânica.
- reparo de Critical Hit deixou de ser uma mutação direta:
  - usa a dificuldade de reparo registrada no Critical Hit;
  - rola Intelecto + Mecânica com modificadores/talentos;
  - falha preserva o Critical Hit;
  - em combate, a tentativa consome a ação;
  - sucesso marca o Critical Hit como reparado e persiste.
- bridge público NÃO expõe `applyActiveVehicleCritical`: a UI não pode fabricar dano crítico na própria nave.
- `VEHICLE.CRITICAL` permanece ENGINE REAL com UI parcial, pois a aplicação de um novo crítico ainda precisa ser ligada automaticamente ao fluxo de ataque veicular/ataques recebidos.

### Bridge/UI/contratos
- novos métodos públicos: `resolveOpposedSkillCheck`, `resolveAssistedSkillCheck`, `resolveCompetitiveSkillCheck`, `resolveSocialInfluenceCheck`, `attemptRepairActiveVehicleCritical`.
- TypeScript contracts raiz + shell R46 atualizados.
- `final-runtime.js`: painel P95 nas rotas 01, 03 e 11.
- bundle offline regenerado com 63 módulos.

## MATRIZ MECÂNICA P95
- 55 famílias canônicas.
- JOGÁVEL: **30/55 = 54,5%** (P94: 28/55 = 50,9%).
- ENGINE REAL sem UI completa: 5.
- PARCIAL: 3.
- RAG/DECLARAÇÃO: 17.
- Engine determinístico completo sem RAG: **35/55 = 63,6%**.
- `CORE.CHECKS.TYPES`: PARCIAL → JOGÁVEL.
- `SOCIAL.ENCOUNTERS`: PARCIAL → JOGÁVEL.
- `VEHICLE.CRITICAL`: permanece ENGINE REAL/UI parcial por falta de ligação automática de aplicação do crítico.

## EVIDÊNCIA FINAL
- P92–P95 focal: **45/45 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão não-stress: **1064/1064 PASS, 0 FAIL**.
- Stress: **6/6 PASS, 0 FAIL**.
- Total automatizado: **1070/1070 PASS, 0 FAIL**.
- Sintaxe dos módulos alterados/runtime/bundle: PASS.
- Bundle: 63 módulos, `1.0.95-r54-p95`.

## ANDROID
**NÃO EXECUTADO**, por instrução explícita do usuário. Compilação Android/APK somente no fechamento final.

## VERSÃO
- Android versionCode: 10095
- Android versionName: 1.0.95
- Runtime/shell: 1.0.95
- Bundle label: 1.0.95-r54-p95

## PRÓXIMO BLOCO P96
1. Ligar Critical Hit veicular à ativação real em ataques veiculares/ataques recebidos, sem criação manual pela UI.
2. Completar qualidades de armas source-bound presentes no catálogo, começando por Aparatosa, Preparação, Disorient, Blast, Limited Ammo, Stun/Stun Setting e Defensive.
3. Implementar `Prepare` e ações/manobras pessoais restantes sustentadas por fonte.
4. Continuar attachments apenas com perfis de efeito estruturados e autorizados.
5. Completar Signature Abilities por carreira e runtime de ativação antes de promover a família.
