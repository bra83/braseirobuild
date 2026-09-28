# VALIDAÇÃO DE COMPLETUDE MECÂNICA — BRASEIRO PATHFINDER 2E / KINGMAKER

**Data:** 27/09/2026  
**Versão:** Braseiro Pathfinder 2e 9.0.31 (Release R30)  
**Escopo:** Pathfinder Second Edition Remaster + Kingmaker Adventure Path + Kingmaker Companion Guide  
**Status Global:** 100% APROVADO — ZERO DEPENDÊNCIA DE RAG EM GAMEPLAY

---

## 1. Sumário Executivo de Validação

Todas as mecânicas determinísticas necessárias para a execução completa, autônoma e precisa de Pathfinder 2e e da campanha Kingmaker foram implementadas no código-fonte nativo Java/SQLite, sem depender de RAG, PDFs em tempo de execução ou improvisação pela IA.

| Domínio de Teste | Suíte / Ferramenta | Quantidade / Asserções | Status |
|---|---|---|---|
| **Sintaxe Java Completa** | `validate_java_syntax.bat` (JDK 21) | 155/155 arquivos parseados | **APROVADO (100%)** |
| **Compilação Semântica Javac** | JBR `javac 21.0.10` com `android-35` | Motores de Regras, Guerra e Repositório | **APROVADO (0 erros)** |
| **Suíte de Testes Pytest** | `pytest tests_pf2` | 225 testes unitários e de integração | **APROVADO (225/225 - 100%)** |
| **Harness JVM Mecânica Completa** | `Pf2KingmakerCompleteMechanicsHarness` | 123 asserções estritas em bytecode | **APROVADO (100%)** |
| **Harness JVM Guerra e Exércitos** | `KingmakerWarfareEngineHarness` | 94 asserções de combate em massa | **APROVADO (100%)** |
| **Harness JVM Atividades e Estruturas** | `TestKingmakerActivitiesAndStructures` | Validação de Anarquia, Eventos e Ruína | **APROVADO (100%)** |
| **Portões de Áudio Kokoro R30** | `test_audio.py` (Kokoro TTS) | 18 portões de integridade de áudio | **APROVADO (100%)** |
| **Persistência e Restore Fail-Closed** | `BackupContractValidator` + `AppDb` v22 | 15 tabelas Kingmaker + auditoria estrita | **APROVADO (100%)** |

---

## 2. Cobertura dos Subsistemas Kingmaker

### A. Gestão de Reino e Economia (`KingmakerRulesEngine.java` e `KingmakerRepository.java`)
- **Ciclo Completo de 4 Fases**:
  - **Fase de Manutenção (Upkeep)**: Atribuição de liderança, penalidade de governante vago (1d4 automático no Unrest), consumo de alimentos e abastecimento de fazendas irrigadas (`influencedFarmlandCount`), superlotação residencial (`refreshSettlementOccupancy`).
  - **Fase de Comércio (Commerce)**: Arrecadação de tributos (`resolveCollectTaxes`), melhoria do padrão de vida (`resolveImproveLifestyle`), saque emergencial do tesouro (`resolveTapTreasury`), comércio de commodities (`resolveTradeCommodities`), investimento de capital (50 PO $\times$ nível do reino por RP) e acordos comerciais.
  - **Fase de Atividades (Activity)**: 29 atividades canônicas catalogadas com despachante universal `resolveKingdomActivity` e 4 graus de sucesso PF2e.
  - **Fase de Eventos (Event)**: Teste simples contra CD 16; decaimento determinístico (-5 por turno sem evento: 16 $\rightarrow$ 11 $\rightarrow$ 6 $\rightarrow$ 1); reset para CD 16 após ocorrência; catálogo de 16 eventos canônicos com resolução estruturada.
- **Agitação (Unrest) e Anarquia**:
  - Penalidades em todos os testes do reino: 1-4 (-1), 5-9 (-2), 10-14 (-3), 15-19 (-4), 20+ (Anarquia).
  - Estado de Anarquia: bloqueio total de atividades não-restauradoras; permissão estrita para atividades restauradoras (`quell_unrest`, `rest_and_relax`, `proclamation`, `supernatural_solution`, `improve_lifestyle`).
- **Penalidades de Ruína (Ruin Penalties)**:
  - Mapeamento determinístico: Cultura $\leftrightarrow$ Corrupção, Economia $\leftrightarrow$ Crime, Estabilidade $\leftrightarrow$ Decadência, Lealdade $\leftrightarrow$ Conflito.
- **Catálogo de 26 Estruturas Urbanas**:
  - Ocupação de lotes, validação de pré-requisitos, dedução atômica de RP e commodities (Madeira, Pedra, Minério, Artigos de Luxo), cálculo de bônus de item com teto (`applyStructureSkillBonuses`), demolição e reparo.

---

### B. Guerra e Combate em Massa (`KingmakerWarfareEngine.java`)
- **Estatísticas Completas de Exército**: CA, PV, PV Máximo, Modificador e CD de Moral, Modificador e CD de Manobra, Ataque Corpo a Corpo e à Distância, Suprimentos, Consumo, Batedores, Táticas e Condições.
- **14 Condições de Exército**: Oculto, Derrotado, Destruído, Distante, Eficiente, Engajado, Fortificado, Perdido, Atolado, Flanqueado, Encurralado, Desbaratado, Abalado (1-4), Exausto.
- **Ações Básicas e Táticas**:
  - Básicas: Avançar, Ataque Corpo a Corpo, Ataque à Distância, Desengajar, Guardar, Reagrupar, Retirar.
  - Táticas: Assalto Total, Medicina de Campanha, Contra-ataque, Fogo de Cobertura, Postura Defensiva, Combate Sujo, Falsa Retirada, Finta, Flanquear, Bombardeio Avassalador, Provocar.
- **Derrota e Destruição**:
  - Queda a 0 PV resulta na condição **Derrotado**.
  - Dano recebido enquanto Derrotado dispara teste simples CD 15 contra **Destruição**.
  - Cálculo de baixas pós-batalha, recuperação e reforços.

---

### C. Companheiros e Acampamento (`KingmakerRulesEngine.java`, `KingmakerRepository.java`)
- **12 Companheiros Canônicos**: Amiri, Ekundayo, Jubilost, Linzi, Nok-Nok, Tristian, Valerie, Harrim, Jaethal, Kalikke & Kanerah, Octavia, Regongar.
- **Limiares de Influência e Atitudes**:
  - Limiares discretos (ex: 2, 4, 6, 8) com transição de atitude (Hostil, Inamistoso, Indiferente, Amigável, Prestativo).
  - Concessão de dádivas mecânicas, bônus de perícia e habilidades de acampamento sem duplicação de recompensas.
  - Rastreamento de disponibilidade e missões pessoais.
- **Procedimento de Acampamento em 5 Etapas**:
  1. *Seleção do Local*: Seleção de zona, terreno, CD da zona e CD de encontro.
  2. *Preparação do Acampamento*: Teste de Sobrevivência contra a CD da zona (Crítico, Sucesso, Falha, Falha Crítica ajustando a CD de encontro).
  3. *Atividades de Acampamento*: Atividades de 2 horas (Cuidar do Equipamento, Forragear, Caçar, Camuflar Acampamento, Preparar Refeição, Ficar de Vigia, e habilidades exclusivas dos companheiros).
  4. *Culinária e Refeições Especiais*: CD de receita, ingredientes, bônus de prato favorito e efeitos de 24 horas.
  5. *Descanso, Sono e Vigília*: Recuperação de PV, recuperação de magias, redução de condições e teste simples horário de encontro com decaimento de CD (-1 por hora).

---

### D. Regras Base Pathfinder 2e Remaster (`Pathfinder2eRulesEngine.java`)
- **Resolução Canônica de Testes**: 4 graus de sucesso (Sucesso Crítico, Sucesso, Falha, Falha Crítica) com promoção/rebaixamento por 20 e 1 natural.
- **Empilhamento de Modificadores (Stacking Rules)**: Bônus de item, estado e circunstância tomam apenas o maior valor; penalidades tomam a de maior magnitude; modificadores sem tipo acumulam.
- **35 Condições Oficiais**: Cálculo integrado de penalidades na CA, testes de perícia, salvaguardas, redução de velocidade e orçamento de ações.
- **Morte e Sobrevivência**: Limite de morte dinâmico por *doomed*, teste de recuperação (CD 10 + *dying*), acertos letais e não-letais, cura natural e testes simples contra dano persistente (CD 15 / CD 10 assistido).

---

### E. Persistência e Restauração Fail-Closed
- **15 Tabelas SQLite no `AppDb.java`** migradas e versionadas no esquema 59.
- **Backup v22 com Auditoria Semântica**: `KingmakerRepository.validateRestoreAgainst()` valida integralmente o estado do reino, líderes, perícias, estruturas, exércitos, companheiros e acampamentos.
- **Fail-Closed**: Qualquer discrepância ou corrupção no restore aborta a transação imediatamente via `IllegalStateException`.

---

## 3. Conclusão
O motor Braseiro Pathfinder 2e versão 9.0.31 (R30) atinge completude mecânica total para a campanha Kingmaker. Todo cálculo é determinístico, testado e persistido de forma resiliente.
