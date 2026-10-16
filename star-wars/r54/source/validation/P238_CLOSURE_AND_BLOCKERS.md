# P238 — fechamento parcial verificado e bloqueadores

**Base íntegra:** P237 SHA256 `4582342fd84cb2dd62a1656271a94788a63713e6e764d3c177336f0c67679783` (13.146 arquivos). Não reiniciar nem reabrir etapas já comprovadas.

## Implementação executável

Ato 1, SWE02 pp.7–11: painel da Krayt Fang com 11 verificações identificadas por página; Astrogação Fácil (falha gera rota não ótima); ruído simultâneo transforma uma dificuldade em desafio, e desligar uma das duas fontes remove upgrade; transponder emite até ser reduzido a uma vez/hora; remoção física só após pouso, Mecânica Média; descoberta e ejeção de seis peles; penalidades de odor de Oskara/Mathus/Pash/Sasha e Lowhhrick em rolagens do controlador durante inspeção, exceção do droide 41-VEX; descoberta de B’ura sem teste; análise opcional da quitina e peças do astromecânico. Recibos por ação com validação de origem, negação de flags adulteradas, três reaberturas e rolagens reais no controlador. Rota de partida verificada exige Astrogação (mesmo falha) + B’ura; interceptação de Thwheek sinalizada se rota não ótima OU tracker não desacelerado. Rotas legadas marcadas `shipSourceUnverified` e NÃO certificadas.

**Não implementado:** combate espacial efetivo com Thwheek/G1-M4-C Dunelizard e efeitos de dano de casco; penalties globais do odor fora da inspeção; consequência física de Threats no casco durante remoção; aplicação automática de tempo/combustível; gerenciamento real de carga e inventário para peles; exame de quitina com níveis de sucesso/triunfo; saque/venda dos itens da nave. A flag de interceptação NÃO é um combate concluído.

## Evidência

- Fonte: PDF oficial Fantasy Flight Games `long_arm_of_the_hutt_lr.pdf`, páginas impressas 7–11. Provas individualizadas em `validation/P238_SWE02_SOURCE_PROOFS.json`.
- Testes focais `tests/uae-p238-swe02-krayt-fang-ship-source.test.mjs`: 12 PASS.
- Regressão GLOBAL por arquivo `evidence/P238/test-results/*.tap`: **357 arquivos regulares / 3266 PASS**, **6 stress / 6 PASS**, total **3272 PASS, zero FAIL, zero SKIP**, 363 arquivos.
- Sintaxe `evidence/P238/SYNTAX_AND_JAVA.json`: 544 JS/MJS, 22 JSON, 2 XML PASS, oito Java/Kokoro idênticos.
- Bundle offline 122 módulos; Android SOURCE 1.0.238; nenhum APK/Gradle/CI/ADB/GitHub.

## Bloqueadores para concluir o VTT

1. SWE02: completar Ato 1 (interceptação Thwheek, Nabat, New Meen não violenta), Ato 2 (Piddock/Anatta/Vrixx’tt/AA7/Lucky Guess), Ato 3 (Teemo, gladiadores, lustre, Jabba, recompensas, epílogos), cotejo integral do PDF e persistência.
2. Quatro aventuras restantes: Under a Black Sun, Beyond the Rim, The Jewel of Yavin, Mask of the Pirate Queen, cada alternativa oficial auditada.
3. Regras EotE/AoR e expansões selecionadas, inclusive 360 consumidores AoR, certificação semântica individual fonte→página→executor→UI→persistência→3 reaberturas→teste.
4. APK reproduzível, CI e homologação física Galaxy A57/S23 Ultra e Kokoro SOMENTE após concluir regras e cinco aventuras.

Não apresentar percentuais inventados nem alegar jogo concluído. Retenção: P238, P237, P126/P113; P236/P234/P233 preservados por segurança até comprovação de redundância.
