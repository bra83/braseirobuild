# HANDOFF STAR WARS EDGE R54 P61 — 2026-09-23

Baseline: R54 P60 recuperado do arquivo de conversa `BRASEIRO_STAR_WARS_EDGE_R54_P60_SOURCE_CHECKPOINT.zip`.

## Objetivo do bloco
Fechar a lacuna de UX entre o tracker persistente/anti-spoiler do P60 e a superfície jogável mobile 415×915 de Sob um Sol Negro, sem recriar o projeto e sem alterar EotE/AoR/MIXED ou Duty/Obligation.

## Implementado
- tracker visual da aventura no topo da sessão usando somente `status.tracker` público;
- objetivo atual em PT-BR por fase da aventura;
- progresso de investigação 0–3 com indicação de pistas suficientes somente após a investigação ser aberta;
- pressão visível: Alerta, Momentum e Kaa’to alertado quando já conhecido;
- milestones apenas alcançados, sem listar marcos futuros;
- ordem da UI alterada para “ação primeiro”: encontro/testes/escolhas antes de informações secundárias;
- NPCs, sistemas, pistas, feedback, consequências e caminho percorrido agrupados em `INFORMAÇÕES DA CENA` recolhível;
- alvos touch aumentados e layout específico para até 415 px;
- `aria-live` no output das resoluções;
- bundle offline reconstruído: 61 módulos.

## Preservado
- EotE / AoR / MIXED;
- Duty / Obligation;
- Sob um Sol Negro completa: cenas, checks, ramificações, encontros, consequências, epílogo, persistência e anti-spoiler;
- journal/timeline e Mundo Vivo.

## Evidência
- focal P33/P36/P41/P42/P46/P49/P50/P55/P56/P57/P58/P60/P61: 66/66 PASS;
- regressão total: 915/915 PASS, 0 FAIL;
- `node --check` do runtime: PASS;
- bundle: PASS, 61 módulos.

## Android
Tentativa de `./gradlew assembleDebug --offline` feita neste marco.
Resultado: FAIL externo antes da compilação, `java.net.UnknownHostException: github.com`, enquanto o wrapper tenta obter a distribuição Gradle. Não declarar APK/Android PASS.

## Próximo
P61 é a baseline. A próxima prioridade é obter build em ambiente com Gradle disponível/rede (BRELGA ou cache), instalar e executar um smoke test real no Android começando pelo botão JOGAR SOB UM SOL NEGRO.
