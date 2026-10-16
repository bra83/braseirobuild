# HANDOFF — BRASEIRO STAR WARS EDGE R54 P103

**Data:** 28/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P102_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.103`  
**versionCode:** `10103`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P102 ou anteriores, não usar GitHub para correção e não compilar Android/Gradle/APK até o fechamento final solicitado pelo usuário.

## P103 — FECHAMENTO VEICULAR SOURCE-BOUND

1. **Controle de Danos corrigido**
   - não repara mais Casco e Tensão simultaneamente;
   - o jogador escolhe Tensão de Sistemas ou Trauma de Casco;
   - dificuldade é calculada pelo tipo realmente reparado;
   - Êxitos e talentos afetam somente o tipo escolhido;
   - reparo de Casco é limitado a uma tentativa por encontro e exige confirmação de contexto quando em combate.

2. **Reparo Manual**
   - Atletismo Difícil;
   - exige Kit de ferramentas realmente possuído;
   - exige confirmação explícita de que o contexto permite alcançar/reparar o casco;
   - recupera 1 Trauma de Casco no sucesso +1 por cada dois Êxitos adicionais;
   - compartilha o mesmo limite de uma tentativa de reparo de Casco por encontro com Controle de Danos.

3. **Ações P102 aprofundadas**
   - Disciplina de Fogo: 1 disparo beneficiado base +1 por cada dois Êxitos adicionais; gasto opcional de Vantagens disponível quando gerado;
   - Escanear Inimigo: níveis atuais só são revelados se o usuário escolher e puder gastar 2 Vantagens;
   - Fatiar Sistemas: 2 Vantagens podem infligir 1 Tensão de Sistemas; Triunfo pode desativar uma arma por um assalto;
   - Despistar Mísseis: +1 upgrade base e +1 por cada dois Êxitos adicionais.

4. **UI R46**
   - separa Controle de Danos de Sistemas/Casco;
   - expõe Reparo Manual;
   - expõe escolhas de gastos opcionais sem aplicar símbolos inexistentes.

## VEHICLE.ACTIONS
Continua **ENGINE REAL / UI PARCIAL** de forma conservadora. O único bloqueio estrutural remanescente identificado neste bloco é **Jamming**, pois ainda não existe runtime genérico de comunicações inimigas que possa ser realmente bloqueado e testado. Não criar flag cosmética para promovê-lo.

## ENV.HAZARDS
Permanece **RAG/DECLARAÇÃO**. Não reabrir sem fonte + engine + estado + persistência + teste.

## EVIDÊNCIA P103
- focal: **37/37 PASS**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- regressão regular: **1142/1142 PASS**;
- stress: **6/6 PASS**;
- total: **1148/1148 PASS, 0 FAIL**;
- bundle offline: **64 módulos**;
- sintaxe: **PASS**.

## PRÓXIMO GRANDE BLOCO — P104
1. Priorizar `COMBAT.MANEUVERS` e `COMBAT.ACTIONS` já existentes no engine, verificando reachability real pela R46.
2. Só implementar Jamming quando houver runtime genérico real de comunicações; caso contrário, manter a família parcial e avançar.
3. Depois continuar Attachments/Mods e qualidades somente com perfis source-bound.
4. Signature Abilities só avançam com cobertura de carreira, ativação, custo, duração, efeito, persistência, UI e testes.
5. Preservar integralmente EotE/AoR/MIXED, Duty/Obligation, Mundo Vivo, anti-spoiler, Sob um Sol Negro e Além da Orla.
