# HANDOFF — BRASEIRO STAR WARS EDGE R54 P102

**Data:** 28/09/2026  
**Baseline de entrada real:** `BRASEIRO_STAR_WARS_EDGE_R54_P101_SOURCE_CHECKPOINT.zip` já persistida no Google Drive antes deste chat.  
**Nova versão:** `1.0.102`  
**versionCode:** `10102`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P100/P101 após este checkpoint e não usar GitHub para corrigir. Android/Gradle/APK continuam proibidos até o fechamento final solicitado explicitamente pelo usuário.

## REBASE P101 → P102
Durante o fechamento foi detectado que o Google Drive já possuía uma P101 mais nova que a P100 usada inicialmente neste chat. A P101 existente foi materializada e tratada como autoridade. O bloco novo foi rebaseado sobre ela, preservando integralmente:

- Encumbrance/levantamento;
- gear assist com propriedade real;
- remendo de reparo de emergência para droide;
- Fear source-bound/persistente;
- Mods/attachments e Postura Defensiva anteriores;
- EotE/AoR/MIXED, Duty/Obligation, Mundo Vivo, anti-spoiler e aventuras.

Nenhum P101 existente foi sobrescrito.

## P102 — AÇÕES VEICULARES EotE CORE
Fonte primária auditada: **Edge of the Empire Core Rulebook**, ações de naves/veículos e Table 7-7.

Implementado ponta a ponta (`R46 → bridge → controller/rules → estado → efeito → persistência → teste`):

1. **Ganhar a Vantagem** — Silhueta 1–4, Velocidade 4+, dificuldade por velocidade relativa, escalada por sucessos repetidos, ignora Evasivas no período válido e usa zona de defesa escolhida no ataque real.
2. **Copiloto** — Pilotagem Média; Êxitos rebaixam o próximo teste de Pilotagem e persistem até consumo.
3. **Traçar Curso** — Astronavegação Média por padrão (Percepção Difícil suportada); Êxitos reduzem Setback externo do próximo perigo de Pilotagem sem apagar Manejo negativo.
4. **Reforçar Escudos** — Mecânica Difícil; exige Defesa; sucesso causa 1 Tensão de Sistemas e +1 Defesa na zona escolhida pelo período válido.
5. **Disciplina de Fogo** — Disciplina Difícil por padrão (Liderança suportada); gera Boosts consumíveis nos disparos seguintes.
6. **Escanear Inimigo** — Percepção Difícil; revela armamento, modificações e limiares. Níveis atuais não são revelados gratuitamente.
7. **Fatiar Sistemas** — Computadores Difícil; reduz Defesa de uma zona pelo período calculado.
8. **Despistar Mísseis** — Computadores Média por padrão (Alerta Difícil suportada); armas Guiadas recebem upgrade real de dificuldade enquanto o efeito está ativo.

## NÃO DECLARAR `VEHICLE.ACTIONS` COMPLETA
Permanece **ENGINE REAL / UI PARCIAL**. Ainda faltam, no mínimo:

- Jamming com runtime real de comunicações;
- Reparo Manual ligado ao mesmo limite/regra de Damage Control;
- gastos opcionais de símbolos de Fire Discipline / Scan / Slice / Spoof quando aplicáveis.

## CORREÇÃO DE AUDITORIA — `ENV.HAZARDS`
A matriz anterior citava `environmental-hazards.test.mjs`, mas esse arquivo/módulo não existe no checkpoint canônico. `pilotingHazardDifficulty()` comprova perigo de Pilotagem veicular, não queda/fogo/sufocamento/radiação gerais.

P102 corrige `ENV.HAZARDS` para **RAG/DECLARAÇÃO**. Por isso a contagem com engine determinístico passa de 35 para **34/55**; é correção de evidência, não regressão de código.

## MATRIZ P102
- 55 famílias canônicas.
- **31/55 JOGÁVEIS = 56,4%**.
- **34/55 com engine determinístico real = 61,8%**.
- 3 ENGINE REAL sem fluxo completo.
- 3 PARCIAIS.
- 18 RAG/DECLARAÇÃO.

Autoridade: `MATRIZ_COBERTURA_MECANICA_REAL_STAR_WARS_P102.md`.

## EVIDÊNCIA FINAL
- gate combinado P101 + P102: **17/17 PASS**;
- pós-bundle: **18/18 PASS**;
- focal P92→P102: **116/116 PASS**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- regressão regular: **1135/1135 PASS**;
- stress: **6/6 PASS**;
- total: **1141/1141 PASS, 0 FAIL**;
- bundle offline: **64 módulos**;
- sintaxe: PASS.

## ANDROID
**NÃO EXECUTADO.** Compilar apenas no fechamento final por pedido explícito do usuário.

## PRÓXIMO GRANDE BLOCO — P103
1. Fechar o restante de `VEHICLE.ACTIONS` apenas onde a fonte e o runtime permitirem efeito verdadeiro.
2. Priorizar `COMBAT.MANEUVERS` / `COMBAT.ACTIONS` que já tenham engine e só precisem de reachability/UI.
3. Não abrir `ENV.HAZARDS` por declaração; exigir fonte + estado + persistência + teste.
4. Manter Attachments/Mods e Signature Abilities conservadores até cobertura completa.
5. Preservar integralmente os gates das duas aventuras.
