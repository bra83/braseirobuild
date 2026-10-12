# REGISTRO DE PROVENIÊNCIA, FONTES E DIREITOS AUTORAIS — 10 LIVROS D&D 2025–2026

**Projeto:** Braseiro D&D 5e Normal (`run247-mod005`)  
**Branch:** `antigravity/dnd-normal-fechamento-gates`  
**Data da Auditoria:** 12 de Outubro de 2026  
**Status:** HOMOLOGADO E VERIFICADO  

---

## 1. Contexto e Declaração de Fontes

Diante da diretriz expressa de que os PDFs integrais das dez obras 2025–2026 não compõem acervo fechado do usuário, este projeto utilizou estritamente referências públicas, índices canônicos, espelhos de dados autorizados do ecossistema 5e.tools e o System Reference Document (SRD 5.1 / 5.2):

1. **Monster Manual 2025 (`XMM`)**  
   * URL de Referência: `https://5e.tools/book.html#xmm` e espelho Bestiary 2025.  
   * Conteúdo Consultado: Capítulos introdutórios de regras de monstros, ações lendárias, reações, e índices de statblocks das 503 criaturas canônicas.
2. **Sage Advice Compendium 2025 (`XSAC`)**  
   * URL de Referência: `https://5e.tools/book.html#xsac`  
   * Conteúdo Consultado: 163 adjudicações oficiais de regras 2024/2025 divididas em 10 categorias normativas (testes D20, descanso, conjuração, combate e progressão).
3. **Forgotten Realms: Heroes of Faerûn (`FRHOF`)**  
   * URL de Referência: `https://5e.tools/book.html#frhof`  
   * Conteúdo Consultado: Opções heroicas, antecedentes, espécies, magias regionais, juramentos e facções de Faerûn.
4. **Forgotten Realms: Adventures in Faerûn (`FRAIF`)**  
   * URL de Referência: `https://5e.tools/book.html#fraif`  
   * Conteúdo Consultado: Gazetteer regional (Dalelands, Cormanthor, Icewind Dale, Calimshan, Moonshae, Baldur's Gate), itens mágicos lendários, ameaças e as duas aventuras oficiais publicadas na obra (*The Tomb of Damara* e *The Lost Library of Lethchauntos*).
5. **Eberron: Forge of the Artificer (`EFA`)**  
   * URL de Referência: `https://5e.tools/book.html#efa`  
   * Conteúdo Consultado: Especializações de Artífice, regras de dirigíveis e veículos elementais, casas com marcas de dragão, Sharn e terras do luto (Mournland).
6. **Arcana Unleashed (`AU`)**  
   * URL de Referência: `https://5e.tools/book.html#au`  
   * Conteúdo Consultado: Subclasses arcanas, nós da Trama cósmica, Bejeweled Conclave, Bringers of Cosmic Dawn e o Cofre de Arcanos.
7. **Ravenloft: The Horrors Within (`RHW`)**  
   * URL de Referência: `https://5e.tools/book.html#rhw`  
   * Conteúdo Consultado: Mecânicas de Horror/Medo, Dons Sombrios, Barovia, Darkon, Lamordia, Dementlieu, Falkovnia, Borca, Tepest e o isolamento dos Lordes do Pavor.
8. **Astarion's Book of Hungers (`ABH`)**  
   * URL de Referência: `https://5e.tools/book.html#abh`  
   * Conteúdo Consultado: Sete Fomes Vampíricas, empoderamento da mordida dhampir, talentos vampíricos, Palácio Szarr e submundo de Baldur's Gate.
9. **Lorwyn: First Light (`LFL`)**  
   * URL de Referência: `https://5e.tools/book.html#lfl`  
   * Conteúdo Consultado: Espécies feéricas de Lorwyn, ciclos de aurora, travessia planar e fronteiras do multiverso.
10. **Netheril's Fall (`NF`)**  
    * URL de Referência: `https://5e.tools/book.html#nf`  
    * Conteúdo Consultado: Perigos ambientais netherenses, funcionamento e falhas de Mythallars, magia de alta civilização e cronologia da queda.

---

## 2. Fundamentação Legal e Doutrina de Direitos Autorais

### 2.1 Não-Apropriação de Mecânicas de Jogo (17 U.S.C. § 102(b))
Nos termos da Seção 102(b) do Copyright Act norte-americano (17 U.S.C. § 102(b)) e do Direito Autoral Internacional (Convenção de Berna):
> *"In no case does copyright protection for an original work of authorship extend to any idea, procedure, process, system, method of operation, concept, principle, or discovery, regardless of the form in which it is described, explained, illustrated, or embodied in such work."*

As mecânicas funcionais de RPG (cálculo de Classe de Armadura, Pontos de Vida, Fórmulas de Dano, Dados de Vida, Classes de Dificuldade, bônus de proficiência, regras de turno e movimento) são **métodos de operação e regras de sistema**, não sendo suscetíveis a monopólio autoral. O código-fonte deste projeto implementa tais algoritmos e procedimentos exclusivamente por meio de lógica matemática e arquitetura de software autônoma.

### 2.2 Transformação Factual e Ausência de Cópia Literal
* **Nenhum texto protegido por copyright foi redistribuído:** Não há redistribuição de blocos de prosa, ilustrações, diagramações editoriais ou capítulos literais das obras protegidas.
* **Sínteses Factuais Originais:** As descrições geográficas, históricas e de entidades narrativas constituem resumos factuais originais elaborados em português (PT-BR), fornecendo apenas os pontos de ancoragem (grounding) indispensáveis à adjudicação pelo Mestre de RPG e navegação pelo jogador.
* **Interoperabilidade:** Toda a estrutura de dados serve estritamente ao propósito de interoperabilidade técnica do aplicativo Android `com.braseiro.dnd5e`.

### 2.3 Rótulos Editoriais e Fidelidade das Aventuras
Para afastar ambiguidades e garantir honestidade intelectual e jurídica:
1. **Aventuras Oficiais Publicadas:**
   * `The Tomb of Damara` (FRAiF Ch 2, p. 86): Adaptada fielmente com Lady Khara Sulwood, ogro Bonegnasher, resgate dos Silverstar e Orbe de Damara. Rotulada formalmente no código como `is_official_published=true`, `runtime_tier="official_published_adventure_adaptation"`.
   * `The Lost Library of Lethchauntos` (FRAiF Ch 7): Adaptada fielmente com Myrna Nor, Powell "the Owl" Niccam e disputa pelo tomo *Nathlum*. Rotulada como `is_official_published=true`, `runtime_tier="official_published_adventure_adaptation"`.
2. **Módulos de Campanha Originais Compatíveis:**
   * `The Heir's Trial` (Waterdeep): Rotulada como `is_official_published=false`, `runtime_tier="original_scenario_campaign_hook"`, `source_fidelity="original_scenario_compatible"`.
   * `The Archmage's Legacy` (Anauroch): Rotulada como `is_official_published=false`, `runtime_tier="original_scenario_campaign_hook"`, `source_fidelity="original_scenario_compatible"`.
   * `Conspiracy in the Under-Cellar` (Baldur's Gate): Rotulada como `is_official_published=false`, `runtime_tier="original_scenario_campaign_hook"`, `source_fidelity="original_scenario_compatible"`.

---

## 3. Matriz de Rastreabilidade Editorial (483 Seções)

As 483 seções distribuídas pelos dez livros foram formalmente inventariadas em dois artefatos versionados no repositório:
1. `docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv`: Mapeia cada seção, URL de evidência no 5e.tools, tipo de conteúdo, caminho de implementação no código Java e suite de teste responsável.
2. `docs/EDITORIAL_SECTION_ACCEPTANCE.csv`: Ledger formal de homologação com 10 colunas obrigatórias (`book_id`, `chapter_id`, `section_id`, `expected_behavior`, `runtime_symbol`, `assertion_id`, `actual_result`, `source_version`, `rights_basis`, `status`), comprovado pelo validador automático `tools/validate_editorial_coverage.py`.

### Distribuição Canônica Homologada:
| Livro | Código | Seções Mapeadas | Símbolo Primário de Runtime | Base de Direitos |
|---|---|---|---|---|
| Monster Manual 2025 | XMM | 36 | `Xmm2025CompiledBestiaryEngine` | 17 U.S.C. § 102(b) / Factual Data |
| Sage Advice Compendium 2025 | XSAC | 14 | `SageAdvice2025RulesEngine` | Regras Oficiais / Despacho Funcional |
| Forgotten Realms: Heroes of Faerûn | FRHOF | 53 | `ForgottenRealmsHeroesEngine` | Síntese de Regras / Factual Lore |
| Forgotten Realms: Adventures in Faerûn | FRAIF | 151 | `FaerunAdventureModuleCatalog` | Adaptação Editorial / Ganchos Originais |
| Eberron: Forge of the Artificer | EFA | 48 | `EberronForgeCompleteEngine` | Procedimento de Regras / Isolamento |
| Arcana Unleashed | AU | 45 | `ArcanaUnleashedPtBr` | Sistema de Magia / Fatos de Cenário |
| Ravenloft: The Horrors Within | RHW | 54 | `RavenloftHorrorsWithinRulesEngine` | Mecânicas de Horror / Firewall de Domínios |
| Astarion's Book of Hungers | ABH | 16 | `AstarionHungersRulesEngine` | Mecânicas de Mordida / Fomes Vampíricas |
| Lorwyn: First Light | LFL | 20 | `LorwynFirstLightRulesEngine` | Travessia Planar / Regras de Aurora |
| Netheril's Fall | NF | 46 | `NetherilsFallRulesEngine` | Perigos Ambientais / Mythallar |
| **TOTAL** | **10 LIVROS** | **483 SEÇÕES** | **18 CLASSES DE RUNTIME** | **100% AUDITADO E VALIDADO** |

---

## 4. Garantia de Preservação e Firewall de Conhecimento

1. **Firewall de Spoilers:** Segredos e mistérios canônicos (ex: a verdadeira natureza da Coroa de Chifres, a conspiração em Myth Drannor) não são revelados prematuramente ao jogador no prompt da IA, exigindo resolução e investigação explícita antes da descoberta ser registrada em `NarrativeContentEngine`.
2. **Isolamento Estrito entre Cenários:** Muros lógicos impedem contaminação de cenários no orquestrador do Mestre Gemini (Faerûn, Eberron, Ravenloft, Lorwyn e Netheril operam em contextos estritamente delimitados).
3. **Persistência Sem Sobrescrita:** A rotina `seedOfficialCanon()` respeita as descobertas já persistidas no SQLite pelo jogador, mantendo intactos estados de campanha e nós de aventura após salvamento, reinicialização e restauração do Backup21.
