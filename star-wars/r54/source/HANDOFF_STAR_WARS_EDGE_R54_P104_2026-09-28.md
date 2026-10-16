# HANDOFF — BRASEIRO STAR WARS EDGE R54 P104

**Data:** 28/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P103_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.104`  
**versionCode:** `10104`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P103 ou anteriores, não usar GitHub para correção e não compilar Android/Gradle/APK até o fechamento final solicitado explicitamente pelo usuário.

## P104 — MANOBRAS PESSOAIS EotE CORE

Fonte primária auditada: `EotE Core Rulebook.pdf` do Google Drive do usuário, file ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, especialmente Chapter VI: Conflict and Combat / Maneuvers / Actions.

### 1. Orçamento de manobra unificado
Todas estas rotas passam pelo mesmo resolvedor de custo:
- Mirar;
- Buscar Cobertura;
- Postura Defensiva;
- Ficar Caído / Levantar-se;
- sacar/trocar arma;
- Preparação;
- recarga de Munição Limitada;
- Stimpack;
- movimento entre faixas.

Carga severa e Ferimentos Críticos que removem a manobra gratuita agora afetam todas igualmente. A segunda manobra continua exigindo 2 de Tensão ou troca da ação, respeitando o limite de duas manobras.

### 2. Ficar Caído / Prone
- nova manobra real `dropProne()`;
- usa o mesmo status persistente `prone` já usado por Derrubar;
- ranged contra alvo caído recebe Setback e melee recebe Boost pelo engine já existente;
- R46 alterna entre `FICAR CAÍDO` e `LEVANTAR-SE` conforme o estado;
- não há mutação bruta de status pela UI.

### 3. Mirar corrigido
- modo normal: +1 Boost; duas miras consecutivas: +2 Boost;
- modo item/parte específica: 2 Setbacks com uma mira, 1 Setback com duas miras consecutivas;
- exige descrição do item/parte no modo específico;
- qualquer outra manobra ou ação antes do ataque cancela a mira;
- dano que ultrapassa soak também cancela a mira;
- o ataque consome o estado de Aim.

### 4. Movimento por faixas corrigido
- Interação↔Curto: 1 manobra;
- Curto↔Médio: 1 manobra;
- Médio↔Longo: 2 manobras;
- Longo↔Extremo: 2 manobras;
- nas transições de duas manobras, a faixa NÃO muda após a primeira;
- progresso fica persistido e pode sobreviver entre turnos/reabertura;
- movimento continua removendo Cobertura e usa o orçamento central de manobras.

## MANOBRAS QUE NÃO FORAM FALSAMENTE AUTOMATIZADAS
`Assist`, `Interact with the Environment` e `Mount or Dismount` permanecem contextuais até existir estado real de aliado engajado, objeto/interagível ou ocupação de veículo/montaria. Não foi criado botão genérico sem efeito de jogo.

## MATRIZ P104
- 55 famílias canônicas.
- 31/55 JOGÁVEIS = 56,4%.
- 34/55 com engine determinístico real = 61,8%.
- COMBAT.MANEUVERS continua ENGINE REAL / UI PARCIAL por critério conservador.
- A evidência antiga de EQUIP.QUALITIES foi corrigida: Derrubar/Prone já está implementado desde P99; a família segue PARCIAL apenas por falta de cobertura integral dos demais perfis.

## EVIDÊNCIA FINAL
- focal P104: 14/14 PASS;
- Sob um Sol Negro: 106/106 PASS;
- Além da Orla: 57/57 PASS;
- regressão regular: 1156/1156 PASS;
- stress: 6/6 PASS;
- total: 1162/1162 PASS, 0 FAIL;
- bundle offline: 64 módulos;
- sintaxe: PASS.

## ANDROID
**NÃO EXECUTADO.**

## PRÓXIMO GRANDE BLOCO — P105
1. Auditar `COMBAT.ACTIONS` contra o Core e expor apenas ações/talentos com runtime determinístico real.
2. Não simular manobras contextuais sem aliado/interagível/ocupação real.
3. `VEHICLE.ACTIONS`: Jamming só entra quando houver runtime genérico de comunicações.
4. Continuar corrigindo evidência antiga antes de promover qualquer família.
5. Preservar integralmente os gates das duas aventuras.
