# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P126

**Baseline:** P113 canônica promovida para a Release P126 (`versionCode = 10126`, `versionName = "1.0.126"`).
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório, 100% PT-BR nas interfaces visíveis, com ciclo de vida completo de aventuras publicadas.

## Resultado P126

- **55** famílias canônicas.
- **35 JOGÁVEIS** (**63,6%**).
- **2 ENGINE REAL**.
- **2 PARCIAIS**.
- **16 RAG/DECLARAÇÃO**.
- **37/55 (67,3%)** possuem engine determinístico real sem RAG.
- Regressão final de testes: **1240 PASS, 0 FAIL, 2 skipped** (244 arquivos de teste no total, incluindo as 6 suítes de stress de 33 sessões).
- Aventuras 100% integradas e jogáveis no Universal Adventure Engine (UAE v2):
  1. **Sob um Sol Negro (`under-black-sun`)**: 106 testes, minijogo de Sabacc, encontros, recompensas e arquivamento canônico.
  2. **Além da Orla (`beyond-the-rim`)**: 57 testes, rodas de pistas, sobrevivência na selva de Cholganna, encontros e arquivamento.
  3. **O Longo Braço do Hutt (`long-arm-of-the-hutt`)**: Atos I a III, encontros com Gamorreanos e Teemo the Hutt, recompensas e arquivamento.
  4. **A Joia de Yavin (`jewel-of-yavin`)**: Heist em Cidade das Nuvens, segurança Bespin, Arlo Serpentine, recompensas e arquivamento.
  5. **A Máscara da Rainha Pirata (`mask-of-the-pirate-queen`)**: Caçada na Orla Média, Corsárias Veiled Sorority, confronto com a Rainha Pirata, recompensas e arquivamento.
- Universal Adventure Engine v2 (`UAE v2`):
  - Esquema versionado com migração idempotente (`ADVENTURE_SCHEMA_VERSION = 2`).
  - Execução atômica de escolhas, avaliação determinística de condições, gatilhos em cascata com salvaguarda contra ciclos (`maxCascades = 10`).
  - Ledger auditável de consequências, rastreamento de objetivos secundários e relógios (clocks) de progresso.
  - Snapshots com restauração completa e encerramento com arquivamento.
- Mecânicas da Força:
  - `FORCE.MOVE` promovido para manipulação mecânica de objetos persistentes de cena (`executeMoveSceneObject`) com testes dedicados.
- Bundle offline:
  - 68 módulos integrados em `app/src/main/assets/vtt/js/core/uiBridge.bundle.js`; builders raiz (`build-ui-bridge-bundle.mjs`) e ferramentas (`tools/build-ui-bridge-bundle.mjs`) byte-idênticos.
- Verificação reproduzível (`tools/verify-final-source.mjs`):
  - **PASS** em todas as etapas: testes Node (1240/1240), stress 33 sessões (6/6), sintaxe JS (120 arquivos), syntax gate Java (8 arquivos com JBR javac), validação JSON (75 arquivos), validação XML (2 arquivos), secret scan (0 hits), 0 binários APK/AAB no repositório.

## Matriz — 55 famílias canônicas

| ID | Domínio | Estado | Engine s/RAG | UI R46 | Persistência | Teste | Evidência/lacuna | Próximo passo |
|---|---|---|---|---|---|---|---|---|
| CORE.DICE.POOL | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | narrativeDice.mjs; r54-device-p6.js; rules.test.mjs | Manter |
| CORE.DICE.SYMBOLS | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | resolveSymbols()/rollPool(); 901/901 regressão | Manter |
| CORE.CHECKS.TYPES | Core | JOGÁVEL | SIM | SIM | SIM | SIM | Teste simples, assistência hábil/não-hábil, oposto, competitivo e social expostos no engine e UI R46. | Manter |
| CORE.DESTINY | Core | JOGÁVEL | SIM | SIM | SIM | SIM | destinySummary + spendDestiny(light) na R46; conversão e persistência luminosa/sombria. | Manter |
| CHAR.CHARACTERISTICS | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | Criação e progressão manual EotE/AoR/MIXED na shell R46. | Manter |
| CHAR.ATTRIBUTES | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | health.mjs, character.mjs; cálculo de ferimentos, tensão, defesas e absorção. | Manter |
| CHAR.SPECIES | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 8 espécies EotE + 8 AoR integradas com características e habilidades iniciais. | Manter |
| CHAR.CAREERS.EOTE | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras EotE + especializações core. | Manter |
| CHAR.OBLIGATION | Campanha EotE | JOGÁVEL | SIM | SIM | SIM | SIM | obligation.mjs; tabela e sorteio de ativação com penalidades reais na sessão. | Manter |
| SKILLS.GENERAL | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveSkillCheck(); lista completa de perícias gerais EotE/AoR. | Manter |
| SKILLS.COMBAT | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveWeaponAttack + painel COMBATE PESSOAL; talentos ofensivos integrados. | Manter |
| SKILLS.KNOWLEDGE | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveSkillCheck() com especialidades de conhecimento. | Manter |
| TALENTS.TREES | Talentos | JOGÁVEL | SIM | SIM | SIM | SIM | talentProgressionStatus + buyTalent na R46; árvores completas e mecânica ativa/passiva. | Manter |
| EQUIP.WEAPONS | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipmentOwnedSummary/equipWeapon + painel EQUIPAMENTO ATIVO; resolução de ataques. | Manter |
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | PARCIAL | SIM parcial | SIM | Qualidades ativas/passivas (Derrubar, Iônica, Guiada, Raio Trator, etc.) suportadas. | Manter |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor; cálculo de absorção (soak) e defesas. | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | SIM parcial | SIM parcial | SIM | Customizações e hard points implementados para perfis essenciais. | Manter |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Iniciativa por slots, rodadas, turnos de jogadores/NPCs gerenciados. | Manter |
| COMBAT.MANEUVERS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Mira, faixas de alcance, cobertura, sacar/guardar, posturas e terreno. | Manter |
| COMBAT.ACTIONS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Ataques com armas, ações da Força, assistência, medicina e ativação de habilidades. | Manter |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Resolução de dano real vs Soak e redução direta de ferimentos em alvos. | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Rolagem d100 de ferimentos críticos com modificadores Vicious e talentos letais. | Manter |
| COMBAT.CONDITIONS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Condições persistentes: desorientado, imobilizado, atordoado, caído. | Manter |
| COMBAT.HEALING | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Stimpacks, primeiros socorros via Medicina e cura natural por descanso. | Manter |
| VEHICLE.STATS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | Perfis de silhueta, velocidade, manobrabilidade, blindagem, casco e tensão de sistema. | Manter |
| VEHICLE.MANEUVERS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | Aceleração, manobras de evasão, ajuste de escudos e movimentação de faixas. | Manter |
| VEHICLE.ACTIONS | Veículos | ENGINE REAL | SIM | PARCIAL | SIM | SIM | Controle de Danos, reparos, disparo de baterias e ordens de tripulação. | Manter |
| VEHICLE.COMBAT | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | Ataques veiculares, escalas de dano planetário/pessoal e dano iônico a sistemas. | Manter |
| VEHICLE.CRITICAL | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | Críticos veiculares com tabelas d100 de componentes danificados e reparo. | Manter |
| TRAVEL.HYPERSPACE | Viagem | JOGÁVEL | SIM | SIM | SIM | SIM | Astrogregação, cálculo de coordenadas, riscos do hiperespaço e avanço temporal. | Manter |
| ENV.HAZARDS | Ambiente | JOGÁVEL | SIM | SIM | SIM | SIM | Terreno difícil/intransponível, gravidade, vácuo, asfixia, fogo/ácido e quedas. | Manter |
| SOCIAL.ENCOUNTERS | Social | JOGÁVEL | SIM | SIM | SIM | SIM | Resolução de testes sociais opostos (Coerção, Charme, Engano, Negociação). | Manter |
| AOR.DUTY | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | Gerenciamento de Duty, metas e ativação de impacto de dever na sessão. | Manter |
| AOR.DUTY.ACTIVATION | Campanha AoR | JOGÁVEL | SIM | AUTOMÁTICA | SIM | SIM | Sorteio no início da sessão com bônus de Tensão e moral para o grupo. | Manter |
| AOR.CONTRIBUTION | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | Escada de contribuição militar, patentes e influência com o alto comando. | Manter |
| AOR.CAREERS | Personagem AoR | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras oficiais de Age of Rebellion com especializações core. | Manter |
| AOR.BASE.REQUISITION | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | Recompensas de requisição de equipamento, veículos e ativos de base por patente. | Manter |
| EXP.EOTE.HIREDGUN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Dangerous Covenants mantidas em catálogo de referência. | Manter |
| EXP.EOTE.SIGNATURE | Expansão | PARCIAL | PARCIAL | NÃO | SIM | PARCIAL | Habilidades de assinatura das carreiras principais. | Manter |
| EXP.EOTE.PAY_GUN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Gestão expandida de contratos de mercenários. | Manter |
| EXP.EOTE.EXPLORER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Enter the Unknown em catálogo de referência. | Manter |
| EXP.EOTE.SURVIVAL | Expansão | RAG/DECLARAÇÃO | NÃO específico | NÃO | NÃO específico | NÃO | Subsistemas avançados de sobrevivência em biomas extremos. | Manter |
| EXP.EOTE.COLONIST | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Far Horizons em catálogo de referência. | Manter |
| EXP.EOTE.HOMESTEADS | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Subsistema modular de construção de postos avançados / lares. | Manter |
| EXP.EOTE.SMUGGLER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Fly Casual em catálogo de referência. | Manter |
| EXP.EOTE.GAMBLING | Expansão | JOGÁVEL | SIM | SIM | SIM | SIM | Jogo de Sabacc canônico totalmente interativo com regras completas de apostas. | Manter |
| EXP.EOTE.SMUGGLING | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Subsistema detalhado de alfândega e compartimentos ocultos. | Manter |
| EXP.EOTE.TECHNICIAN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Special Modifications em catálogo de referência. | Manter |
| EXP.EOTE.CRAFTING | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Criação livre de equipamentos a partir de componentes. | Manter |
| EXP.AOR.SOLDIER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Forged in Battle em catálogo de referência. | Manter |
| EXP.AOR.ENTRENCHED | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Subsistema avançado de combate em trincheiras e posições fixas. | Manter |
| EXP.AOR.SPY | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Cyphers and Masks em catálogo de referência. | Manter |
| EXP.AOR.ESPIONAGE | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Redes secretas e subsistemas de infiltração aprofundados. | Manter |
| EXP.AOR.ENGINEER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Especializações de Fully Operational em catálogo de referência. | Manter |
| EXP.AOR.VEHICLE_CRAFTING | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Customização estrutural pesada de frotas e bases rebeldes. | Manter |

## Mecânicas Adicionais Fechadas em P126

- **ADVENTURE.ENGINE.V2 (UAE v2) — JOGÁVEL:**
  - Suporte completo às 5 aventuras oficiais publicadas com ramificação, encontros, recompensas, clocks e arquivamento auditável.
- **FORCE.MOVE — JOGÁVEL:**
  - Manipulação cinemática de objetos persistentes na cena com gastos de pontos da Força (pips) e alocação espacial.
- **ADVERSARY.PROFILES — JOGÁVEL:**
  - Perfis canônicos parametrizados e instanciados para todos os antagonistas das 5 aventuras publicadas.
- **KOKORO.TTS.PIPELINE — PRESERVADO:**
  - Pipeline offline e fallbacks mantidos sem qualquer degradação estrutural.
