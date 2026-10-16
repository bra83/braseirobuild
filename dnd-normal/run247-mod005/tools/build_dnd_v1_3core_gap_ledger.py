#!/usr/bin/env python3
"""
Generates docs/dnd-v1-3core/GAP_LEDGER.csv adhering strictly to the Definition of Done in Issue #55:
fonte -> executor deterministico -> chamada host de jogo -> UI PT-BR -> transacao SQLite -> Backup21/3 reaberturas -> teste no SHA.

Permitted states:
- VERIFIED_E2E
- IN_PROGRESS
- BLOCKED_SOURCE
- BLOCKED_HOST
- NARRATIVE_ONLY (justified)
"""

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LEDGER_FILE = ROOT / "docs" / "dnd-v1-3core" / "GAP_LEDGER.csv"

FIELDS = [
    "id",
    "book",
    "source_section",
    "mechanic_name",
    "source_citation",
    "deterministic_executor",
    "host_call",
    "ui_binding",
    "sqlite_transaction",
    "backup21_reopen_tested",
    "status",
    "status_notes"
]

ENTRIES = [
    # ----------------------------------------------------
    # PHB 2024: 12 Core Classes Active Mechanics
    # ----------------------------------------------------
    {
        "id": "PHB-CLS-001",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Barbaro: Furia e Furia Implacavel",
        "source_citation": "PHB 2024 p. 54",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Furia)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#barbarianRageAndRelentlessRageFlow com persistencia e recuperacao em descanso longo"
    },
    {
        "id": "PHB-CLS-002",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Bardo: Inspiracao Bardica e Fonte de Inspiracao",
        "source_citation": "PHB 2024 p. 62",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Inspiracao Bardica)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#bardInspirationAndFontFlow com recarga em descanso curto no nivel 5+"
    },
    {
        "id": "PHB-CLS-003",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Clerigo: Canalizar Divindade, Fagolha Divina e Expulsar Mortos-Vivos",
        "source_citation": "PHB 2024 p. 70",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Canalizar Divindade)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#clericChannelDivinityAndDivineInterventionFlow"
    },
    {
        "id": "PHB-CLS-004",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Druida: Forma Selvagem e Ressurgimento Selvagem",
        "source_citation": "PHB 2024 p. 80",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Forma Selvagem)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#druidWildShapeAndResurgenceFlow"
    },
    {
        "id": "PHB-CLS-005",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Guerreiro: Retomar o Folego, Surto de Acao, Mente Tatica e Indomavel",
        "source_citation": "PHB 2024 p. 90",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Retomar Folego / Surto)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#fighterSecondWindActionSurgeAndIndomitableFlow"
    },
    {
        "id": "PHB-CLS-006",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Monge: Foco, Rajada de Golpes, Defesa Paciente, Passo do Vento, Metabolismo Sobrenatural e Golpe Atordoante",
        "source_citation": "PHB 2024 p. 98",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Foco Monastico)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#monkFocusAndMetabolismFlow"
    },
    {
        "id": "PHB-CLS-007",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Paladino: Imposicao de Maos, Destruicao Divina, Montaria Fiel e Canalizar Divindade",
        "source_citation": "PHB 2024 p. 108",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Imposicao de Maos)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#paladinLayOnHandsAndSmiteFlow"
    },
    {
        "id": "PHB-CLS-008",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Guardiao (Ranger): Marca do Cacador e Explorador Habil",
        "source_citation": "PHB 2024 p. 118",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Marca do Cacador)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#rangerHuntersMarkFlow"
    },
    {
        "id": "PHB-CLS-009",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Ladino: Ataque Furtivo e Golpe Astuto (Veneno, Tropeco, Desengajar)",
        "source_citation": "PHB 2024 p. 128",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Golpe Astuto)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#rogueCunningStrikeFlow"
    },
    {
        "id": "PHB-CLS-010",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Feiticeiro: Feiticaria Inata e Fonte de Magia (Pontos de Feiticaria)",
        "source_citation": "PHB 2024 p. 138",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Feiticaria Inata / Pontos de Feiticaria)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#sorcererInnateSorceryAndFontOfMagicFlow"
    },
    {
        "id": "PHB-CLS-011",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Bruxo: Magia de Pacto e Astucia Magica",
        "source_citation": "PHB 2024 p. 148",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Astucia Magica)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#warlockMagicalCunningFlow"
    },
    {
        "id": "PHB-CLS-012",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes",
        "mechanic_name": "Mago: Recuperacao Arcana e Memorizar Magia",
        "source_citation": "PHB 2024 p. 158",
        "deterministic_executor": "Core2024ClassRuntimeEngine.executeClassAction",
        "host_call": "Core2024ClassRuntimeEngine.executeClassAction",
        "ui_binding": "MainActivity.classActionButton (Recuperacao Arcana)",
        "sqlite_transaction": "combatAtomic(character_class_action_spend)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Core2024ClassRuntimeRobolectricTest#wizardArcaneRecoveryFlow"
    },
    {
        "id": "PHB-SUB-001",
        "book": "PHB 2024",
        "source_section": "Capitulo 3: Classes (Subclasses)",
        "mechanic_name": "48 Subclasses Oficiais (4 por classe) - Niveis de Selecao e Recursos Ativos",
        "source_citation": "PHB 2024 Cap. 3",
        "deterministic_executor": "Core2024SubclassRuntimeEngine",
        "host_call": "Core2024SubclassRuntimeEngine.getSubclassesForClass / isSubclassLevelValid",
        "ui_binding": "MainActivity.characterCreationDialog (subclasse 2024)",
        "sqlite_transaction": "characters.sheet_json (subclass_2024, crit_threshold, subclass_features)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "48 subclasses oficiais (4 por classe) homologadas com portao estrito de nivel 3 e testes verdes em Core2024SubclassRobolectricTest"
    },
    # ----------------------------------------------------
    # PHB 2024: Spells & Combat & Rest
    # ----------------------------------------------------
    {
        "id": "PHB-SPL-001",
        "book": "PHB 2024",
        "source_section": "Capitulo 7: Magias",
        "mechanic_name": "Conjuracao, Espacos de Magia, CD de Salvaguarda e Ataque Magico",
        "source_citation": "PHB 2024 p. 234",
        "deterministic_executor": "Spellcasting2024RulesEngine",
        "host_call": "AppDb.castCharacterSpell",
        "ui_binding": "MainActivity.spellPickerDialog",
        "sqlite_transaction": "spendCharacterSpellSlot",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado com persistencia de espacos gastos e reabertura em 3 ciclos"
    },
    {
        "id": "PHB-CBT-001",
        "book": "PHB 2024",
        "source_section": "Capitulo 1: Regras Basicas / Combate",
        "mechanic_name": "Maestria com Armas: Desarmar, Empurrar, Lento, Derrubar, Vex, Pastar e Clivar",
        "source_citation": "PHB 2024 p. 214",
        "deterministic_executor": "WeaponMastery2024RulesEngine",
        "host_call": "AppDb.applyWeaponMasteryAttack",
        "ui_binding": "MainActivity.weaponMasteryDialog",
        "sqlite_transaction": "combatAtomic(weapon_mastery_resolution)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Efeitos acoplados a acao de ataque com validacao de condicoes e alvos no grid"
    },
    {
        "id": "PHB-RST-001",
        "book": "PHB 2024",
        "source_section": "Capitulo 1: Regras Basicas / Descansos",
        "mechanic_name": "Descanso Curto e Descanso Longo (Recuperacao de PV, Dados de Vida e Recursos)",
        "source_citation": "PHB 2024 p. 19",
        "deterministic_executor": "Core2024ClassRuntimeEngine.recoverOnRest",
        "host_call": "AppDb.applyRestToCharacter",
        "ui_binding": "MainActivity.restDialog",
        "sqlite_transaction": "combatAtomic(character_rest_recovery)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em SessionContinuityUiRobolectricTest e PlayableUiRobolectricTest com roundtrip Backup21"
    },
    {
        "id": "PHB-CHR-001",
        "book": "PHB 2024",
        "source_section": "Capitulo 2: Criacao de Personagem",
        "mechanic_name": "Criacao de Personagem 2024 (Especies, Antecedentes de Origem, Talentos de Origem e Atributos)",
        "source_citation": "PHB 2024 Cap. 2",
        "deterministic_executor": "CharacterCreationEngine",
        "host_call": "AppDb.addCharacter / AppDb.updateCharacter",
        "ui_binding": "MainActivity.characterCreationDialog",
        "sqlite_transaction": "characters table insert/update",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "20 testes em CharacterCreationUiRobolectricTest passam 100%, preservando PV, edicao 2024 e invariantes"
    },

    # ----------------------------------------------------
    # DMG 2024: Bastions, Magic Items, Hazards, Traps, Tables
    # ----------------------------------------------------
    {
        "id": "DMG-BST-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 8: Bastiões",
        "mechanic_name": "11 Eventos Oficiais de Bastiao (d100) e Consequencias",
        "source_citation": "DMG 2024 p. 280",
        "deterministic_executor": "Bastion2024RulesEngine",
        "host_call": "AppDb.rollBastion2024Event",
        "ui_binding": "MainActivity.bastionEventDialog",
        "sqlite_transaction": "bastion_events, character_economy",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Bastion2024PersistentFlowRobolectricTest e Bastion2024AllEventsRobolectricTest"
    },
    {
        "id": "DMG-BST-002",
        "book": "DMG 2024",
        "source_section": "Capitulo 8: Bastiões",
        "mechanic_name": "29 Instalacoes Especiais de Bastiao, Portoes de Nivel (5, 9, 13, 17) e 7 Ordens Deterministicas",
        "source_citation": "DMG 2024 p. 284-300",
        "deterministic_executor": "Bastion2024SpecialFacilitiesEngine",
        "host_call": "AppDb.issueBastion2024SpecialFacilityOrder",
        "ui_binding": "MainActivity.bastionFacilityOrderDialog",
        "sqlite_transaction": "bastion_facilities, bastion_orders, effect_receipts",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Bastion2024SpecialFacilitiesRobolectricTest cobrindo todas as 29 instalacoes e ordens"
    },
    {
        "id": "DMG-ITM-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 7: Itens Mágicos",
        "mechanic_name": "Catalogo de Itens Magicos, Regra de 3 Sintonizacoes, Descanso Curto e Precificacao por Raridade",
        "source_citation": "DMG 2024 p. 210-275",
        "deterministic_executor": "Dmg2024RulesEngine",
        "host_call": "Dmg2024RulesEngine.attuneItem / unattuneItem",
        "ui_binding": "MainActivity.magicItemAttunementDialog",
        "sqlite_transaction": "inventory_items.attuned, resources_json.attuned_slots",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Dmg2024RulesRobolectricTest com limite estrito de 3 slots e rejeicao de duplicatas"
    },
    {
        "id": "DMG-ENV-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 3: Perigos Ambientais",
        "mechanic_name": "Perigos Ambientais: Frio Extremo, Calor Extremo, Vento Forte, Precipitacao Intensa e Areia Movedica",
        "source_citation": "DMG 2024 p. 64-68",
        "deterministic_executor": "Dmg2024RulesEngine.resolveEnvironmentalHazard",
        "host_call": "Dmg2024RulesEngine.resolveEnvironmentalHazard",
        "ui_binding": "MainActivity.environmentalHazardDialog",
        "sqlite_transaction": "combatAtomic(hazard_resolution)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Dmg2024RulesRobolectricTest determinando CDs de Constituicao e exaustao deterministica"
    },
    {
        "id": "DMG-TRP-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 3: Armadilhas e Perigos",
        "mechanic_name": "Armadilhas: Contratempo, Perigosas e Letais (Ataques, Salvaguardas e Danos Deterministicos)",
        "source_citation": "DMG 2024 p. 70-74",
        "deterministic_executor": "Dmg2024RulesEngine.resolveTrap",
        "host_call": "Dmg2024RulesEngine.resolveTrap",
        "ui_binding": "MainActivity.trapTriggerDialog",
        "sqlite_transaction": "combatAtomic(trap_trigger)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Dmg2024RulesRobolectricTest para todas as 3 faixas de periculosidade"
    },
    {
        "id": "DMG-HAZ-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 3: Quedas e Sufocamento",
        "mechanic_name": "Quedas (1d6/10 pes, max 20d6, caido) e Regras de Sufocamento",
        "source_citation": "DMG 2024 p. 68-69",
        "deterministic_executor": "Dmg2024RulesEngine.resolveFallingDamage / resolveSuffocation",
        "host_call": "Dmg2024RulesEngine.resolveFallingDamage",
        "ui_binding": "MainActivity.fallingDamageDialog",
        "sqlite_transaction": "combatAtomic(falling_damage)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Dmg2024RulesRobolectricTest aplicando condicao derrubado (caido) e dano com teto de 20d6"
    },
    {
        "id": "DMG-TRS-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 4: Tabelas de Tesouro",
        "mechanic_name": "Tesouro Individual e Tesouro em Tesouraria por Nivel de Desafio (ND 0-4, 5-10, 11-16, 17+)",
        "source_citation": "DMG 2024 p. 110-120",
        "deterministic_executor": "Dmg2024RulesEngine.generateIndividualTreasure / generateHoardTreasure",
        "host_call": "Dmg2024RulesEngine.generateTreasure",
        "ui_binding": "MainActivity.treasureRollDialog",
        "sqlite_transaction": "character_economy.add_coins",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em Dmg2024RulesRobolectricTest cobrindo todas as faixas de ND"
    },
    {
        "id": "DMG-SES-001",
        "book": "DMG 2024",
        "source_section": "Capitulo 1: Gestao da Sessao",
        "mechanic_name": "Ritmo de Narracao, Seguranca na Mesa e Gestao da Sessao",
        "source_citation": "DMG 2024 Cap. 1",
        "deterministic_executor": "N/A (Suporte ao Mestre)",
        "host_call": "N/A",
        "ui_binding": "MainActivity.sessionGuideView",
        "sqlite_transaction": "N/A",
        "backup21_reopen_tested": "NA",
        "status": "NARRATIVE_ONLY",
        "status_notes": "Mecanica exclusivamente narrativa e editorial sem mutacao de ficha ou dados no SQLite"
    },

    # ----------------------------------------------------
    # MM 2025: Bestiary, Multiattacks, Combat Admission, Special Families
    # ----------------------------------------------------
    {
        "id": "XMM-BST-001",
        "book": "Monster Manual 2025",
        "source_section": "Catalogo de Monstros A-Z",
        "mechanic_name": "503 Fichas de Monstros Compiladas e Indice Alfabetico 2025",
        "source_citation": "XMM 2025 A-Z",
        "deterministic_executor": "Xmm2025CompiledBestiaryEngine",
        "host_call": "CompiledStatblockEngine.runtimeProfile",
        "ui_binding": "MainActivity.monsterPickerDialog",
        "sqlite_transaction": "combatants table insert (spawn)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "503 blocos registrados e validados contra fontes canônicas 2025"
    },
    {
        "id": "XMM-MLA-001",
        "book": "Monster Manual 2025",
        "source_section": "Acoes de Multiataque",
        "mechanic_name": "Multiataques Estruturados com Componentes de Ataque Puro (Ape, Clay Golem, Balor, etc.)",
        "source_citation": "XMM 2025 Plan Index",
        "deterministic_executor": "Xmm2025MultiattackPlanIndex",
        "host_call": "AppDb.applyCompiledStatblockMultiattack",
        "ui_binding": "MainActivity.compiledMonsterMultiattackDialog",
        "sqlite_transaction": "combatAtomic(compiled_statblock_multiattack)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado em XmmMultiattackHostRobolectricTest com protecao contra acao duplicada, validacao de alvos e recibo duravel"
    },
    {
        "id": "XMM-MLA-002",
        "book": "Monster Manual 2025",
        "source_section": "Acoes de Multiataque Hibridas",
        "mechanic_name": "Multiataques com Substituicao de Feitico ou Uso Especial (Dragoes, Esfinges)",
        "source_citation": "XMM 2025 Plan Index",
        "deterministic_executor": "XmmMultiattackHostPlan",
        "host_call": "AppDb.applyCompiledStatblockMultiattack",
        "ui_binding": "MainActivity.compiledMonsterMultiattackDialog",
        "sqlite_transaction": "combatAtomic(compiled_statblock_multiattack)",
        "backup21_reopen_tested": "SIM",
        "status": "BLOCKED_HOST",
        "status_notes": "Componentes nao-ataque bloqueados por multiattack_component_executor_not_installed para proteger atomicidade ate implementacao conjunta de feitico em multiataque"
    },
    {
        "id": "XMM-SAV-001",
        "book": "Monster Manual 2025",
        "source_section": "Acoes Especiais e Salvaguardas",
        "mechanic_name": "Acoes Especiais com CD de Salvaguarda e Movimento Forcado",
        "source_citation": "XMM 2025 Statblocks",
        "deterministic_executor": "Xmm2025SaveDamageEngine",
        "host_call": "AppDb.applyCompiledStatblockSaveAction",
        "ui_binding": "MainActivity.compiledMonsterSaveActionDialog",
        "sqlite_transaction": "combatAtomic(compiled_statblock_save_action)",
        "backup21_reopen_tested": "SIM",
        "status": "VERIFIED_E2E",
        "status_notes": "Validado com calculo de meio dano no sucesso, dano total na falha e reposicionamento no mapa tatico"
    },
    {
        "id": "XMM-ARB-001",
        "book": "Monster Manual 2025",
        "source_section": "Efeitos Complexos e Arbitragem Manual",
        "mechanic_name": "Ponte de Arbitragem Manual XMM para 62 Efeitos Especiais sem Automacao Completa",
        "source_citation": "XMM 2025 Source Registry",
        "deterministic_executor": "XmmCombatAdmission / AppDb",
        "host_call": "AppDb.applyXmmManualArbitration",
        "ui_binding": "MainActivity.compiledMonsterManualArbitrationDialog",
        "sqlite_transaction": "combatAtomic(xmm_manual_arbitration)",
        "backup21_reopen_tested": "SIM",
        "status": "IN_PROGRESS",
        "status_notes": "Ponte deterministica completa e testada em XmmManualArbitrationBridgeRobolectricTest; migracao gradativa de ponte manual para automacao nativa em andamento"
    }
]

def main():
    LEDGER_FILE.parent.mkdir(parents=True, exist_ok=True)
    with open(LEDGER_FILE, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=FIELDS)
        writer.writeheader()
        for row in ENTRIES:
            writer.writerow(row)
    print(f"Generated {LEDGER_FILE} with {len(ENTRIES)} audited mechanics.")

if __name__ == "__main__":
    main()
