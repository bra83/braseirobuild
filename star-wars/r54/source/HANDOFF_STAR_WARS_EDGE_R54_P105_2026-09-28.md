# HANDOFF — BRASEIRO STAR WARS EDGE R54 P105

**Data:** 28/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P104_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.105`  
**versionCode:** `10105`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P104 ou anteriores, não usar GitHub para correção e não compilar Android/Gradle/APK até o fechamento final solicitado explicitamente pelo usuário.

## P105 — AÇÕES DA FORÇA EotE CORE

Fonte primária auditada: `EotE Core Rulebook.pdf` do Google Drive do usuário, file ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, especialmente **Activating Powers / Ongoing Effects / Sense / Influence**.

### 1. Sentir — efeito contínuo defensivo
- exige Sentir comprado + `control_defense` realmente adquirido;
- ativação consome a ação do turno em combate;
- compromete exatamente 1 dado da Força;
- reduz Force Rating disponível enquanto mantido;
- ao receber ataque, aprimora de verdade a dificuldade do pool;
- `strength` aumenta o número de upgrades do pool;
- `duration` aumenta o número de gatilhos permitidos por assalto;
- uso/gatilho fica persistido no estado contínuo para impedir benefício ilimitado.

### 2. Sentir — efeito contínuo ofensivo
- exige a cadeia real até `control_offense`;
- ativação consome a ação e compromete 1 dado;
- no ataque do jogador, aprimora de verdade o pool positivo;
- respeita Strength/Duration comprados;
- não concede bônus se o efeito não estiver realmente ativo.

### 3. Influência — efeito básico
- exige poder comprado;
- usa alvo persistente do encontro, sem NPC inventado;
- respeita alcance Engajado + upgrades de Alcance efetivamente comprados;
- respeita Magnitude e limite real de alvos;
- rola dados da Força reais e verifica custo em pontos;
- a ação é consumida mesmo quando a rolagem não gera pontos suficientes;
- sucesso aplica Tensão real pelo engine do adversário; rivais/minions sem strain convertem pelo mesmo runtime já existente;
- Strength comprado continua determinando 2 de Tensão por ativação;
- resultados sombrios NÃO são usados automaticamente pela R46.

### 4. R46 / bridge
- novo painel de Força na rota de combate;
- só mostra Sentir/Influência se o personagem realmente os possui;
- Sentir só oferece os modos cujos controles foram comprados;
- Influência exige alvo persistente presente no encontro;
- nenhum efeito é criado por texto/RAG.

## NÃO IMPLEMENTADO ARTIFICIALMENTE
- `Mover` continua calculável no engine, mas não recebeu botão de efeito porque o VTT ainda não possui objeto de cena persistente seguro para mover/arremessar/manipular.
- Sentir básico de “detectar vida/emoção/pensamento” continua sem resposta automática porque isso depende de informação real da cena/Mestre.
- conversão de resultados sombrios na ação de Influência não é automática; o P105 prefere falhar a tomar essa decisão pelo jogador.

## MATRIZ P105
- 55 famílias canônicas.
- **31/55 JOGÁVEIS = 56,4%**.
- **34/55 com engine determinístico real = 61,8%**.
- `COMBAT.ACTIONS` continua **ENGINE REAL / UI PARCIAL** por critério conservador.
- `FORCE.ACTIONS` adicional: **ENGINE REAL / UI PARCIAL** — Sentir ofensivo/defensivo e Influência básica chegam ao gameplay; Mover permanece sem UI mecânica.

## EVIDÊNCIA FINAL
- focal P105: **7/7 PASS**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- regressão regular: **1163/1163 PASS**;
- stress: **6/6 PASS**;
- total: **1169/1169 PASS, 0 FAIL**;
- bundle offline: **64 módulos**;
- sintaxe: PASS.

## ANDROID
**NÃO EXECUTADO.**

## PRÓXIMO GRANDE BLOCO — P106
1. Continuar `COMBAT.ACTIONS` apenas com talentos/abilities já source-bound e determinísticos.
2. Não expor `Mover` até existir objeto/alvo de cena persistente para receber o efeito.
3. Revisar `COMBAT.MANEUVERS` contextuais (Assist/Interact/Mount) somente quando o estado de aliado/interagível/ocupação existir.
4. Continuar sem reabrir `ENV.HAZARDS` por declaração.
5. Preservar integralmente os gates das duas aventuras.
