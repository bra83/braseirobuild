#!/usr/bin/env python3
"""
Gera docs/EDITORIAL_SECTION_ACCEPTANCE.csv a partir de docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv.
Colunas obrigatórias:
book_id,chapter_id,section_id,expected_behavior,runtime_symbol,assertion_id,actual_result,source_version,rights_basis,status
"""

import csv

# AVISO: este gerador cria SOMENTE candidatos à revisão. NÃO executa testes
# nem valida fonte, licenças ou semântica por seção. Nunca emita ACCEPTED.
import os

SOURCE_CSV = "docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv"
OUTPUT_CSV = "docs/EDITORIAL_SECTION_ACCEPTANCE.csv"

def to_runtime_symbol(path):
    p = path.replace("\\", "/")
    if p.endswith(".java"):
        # app/src/main/java/com/braseiro/... -> com.braseiro...
        if "app/src/main/java/" in p:
            fqcn = p.split("app/src/main/java/")[1].replace("/", ".").replace(".java", "")
            return fqcn
    if "PLANO_MESTRE_MATRIZ" in p:
        return "com.braseiro.dnd5e.canon.OfficialBookRuntimeGateway"
    return "com.braseiro.dnd5e.NarrativeContentEngine"

def determine_assertion(book_id, content_type, section_id):
    if book_id == "XMM":
        if "ecology" in section_id or "dragon" in section_id:
            return "EditorialSectionProofTest#testArcanaUnleashedAndMultiverseSections"
        return "GateBMonstersAndRulingsTest#testMonsterStatBlocks"
    elif book_id == "XSAC":
        if "adjudication" in section_id:
            return "EditorialSectionProofTest#testArcanaUnleashedAndMultiverseSections"
        return "GateBMonstersAndRulingsTest#testSageAdviceCompendiumRulings"
    elif book_id == "FRHOF":
        return "GateCFaerunIntegrationTest#testFaerunCanonIntegration"
    elif book_id == "FRAIF":
        if content_type in ("official_adventure_published", "adventure_narrative"):
            if "tomb_damara" in section_id:
                return "EditorialSectionProofTest#testTombOfDamaraBranchingStateDivergence"
            elif "lost_library" in section_id or "lethchauntos" in section_id:
                return "EditorialSectionProofTest#testLostLibraryBranchingConsequencesDivergence"
            elif "heir_trial" in section_id:
                return "EditorialSectionProofTest#testHeirTrialBranchingConsequences"
            return "EditorialSectionProofTest#testFaerunAdventureModulesFiveSpecsNonLinear"
        return "EditorialSectionProofTest#testFraifEditorialSectionsCoverage"
    elif book_id == "EFA":
        return "GateDEberronAndArcanaIntegrationTest#testEberronForgeEngine"
    elif book_id == "AU":
        if "tomb" in section_id or "conclave" in section_id or "bringers" in section_id:
            return "EditorialSectionProofTest#testArcanaUnleashedAndMultiverseSections"
        return "GateDEberronAndArcanaIntegrationTest#testArcanaUnleashedEngine"
    elif book_id == "RHW":
        if "domain" in section_id or "barovia" in section_id or "darkon" in section_id:
            return "EditorialSectionProofTest#testRavenloftAndHungersEditorialSections"
        return "GateERavenloftAndHungersIntegrationTest#testRavenloftHorrorEngine"
    elif book_id == "ABH":
        if "cazador" in section_id or "palace" in section_id or "hungers" in section_id or "rats_run" in section_id:
            return "EditorialSectionProofTest#testRavenloftAndHungersEditorialSections"
        return "GateERavenloftAndHungersIntegrationTest#testAstarionHungersEngine"
    elif book_id == "LFL":
        return "GateFLorwynAndNetherilIntegrationTest#testLorwynAndNetherilEngines"
    elif book_id == "NF":
        return "GateFLorwynAndNetherilIntegrationTest#testLorwynAndNetherilEngines"
    return "EditorialSectionProofTest#testSourceCodesAndUniqueEntityIds"

def determine_expected_behavior(book_id, content_type, title, delta):
    title_clean = title.strip()
    if book_id == "XMM":
        return f"Adjudicar estatísticas e reações da criatura sem auto-rolls: {title_clean}"
    elif book_id == "XSAC":
        return f"Processar esclarecimento oficial de adjudicação de regras 2025: {title_clean}"
    elif book_id == "FRHOF":
        return f"Disponibilizar opções heroicas, magias ou antecedentes de Faerûn: {title_clean}"
    elif book_id == "FRAIF":
        if "adventure" in content_type:
            return f"Executar nós de aventura com escolhas ramificadas e consequências persistentes: {title_clean}"
        return f"Integrar lore e atlas regional de Faerûn com firewall de spoilers: {title_clean}"
    elif book_id == "EFA":
        return f"Executar regras de artífice, dirigíveis ou facções de Eberron: {title_clean}"
    elif book_id == "AU":
        return f"Processar regras de magia arcanista, nós de trama e facções cósmicas: {title_clean}"
    elif book_id == "RHW":
        return f"Processar regras de horror/medo e domínios do Pavor sob isolamento de Ravenloft: {title_clean}"
    elif book_id == "ABH":
        return f"Executar regras de fomes vampíricas de Astarion e intrigas de Baldur's Gate: {title_clean}"
    elif book_id == "LFL":
        return f"Executar mecânicas de Lorwyn, aurora feérica e isolamento de plano: {title_clean}"
    elif book_id == "NF":
        return f"Processar perigos de Netheril, mythallar e trama arcana ancestral: {title_clean}"
    return f"Executar cânone oficial de {book_id}: {title_clean}"

def generate():
    if not os.path.exists(SOURCE_CSV):
        raise FileNotFoundError(f"Fonte não encontrada: {SOURCE_CSV}")

    rows_out = []

    # Resguardar homologações FEITAS COM EVIDÊNCIA. Um gerador não pode sobrescrevê-las.
    prior_proofs = {}
    if os.path.isfile(OUTPUT_CSV):
        with open(OUTPUT_CSV, "r", encoding="utf-8-sig", newline="") as existing_file:
            for previous in csv.DictReader(existing_file):
                k = (previous.get("book_id"), previous.get("chapter_id"), previous.get("section_id"))
                if previous.get("status") == "IMPLEMENTED_AND_TESTED" and previous.get("evidence_ref"):
                    prior_proofs[k] = previous

    with open(SOURCE_CSV, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            b_id = row["book_id"]
            c_id = row["chapter_id"]
            s_id = row["section_id"]
            title = row["section_title"]
            ctype = row["content_type"]
            path = row["implementation_path"]
            delta = row["delta"]

            expected = determine_expected_behavior(b_id, ctype, title, delta)
            runtime = to_runtime_symbol(path)
            assertion = determine_assertion(b_id, ctype, s_id)

            candidate = {
                "book_id": b_id,
                "chapter_id": c_id,
                "section_id": s_id,
                "expected_behavior": expected,
                "runtime_symbol": runtime,
                "assertion_id": assertion,
                "actual_result": "NOT_RUN",
                "source_version": "UNVERIFIED",
                "rights_basis": "REVIEW_REQUIRED",
                "status": "IMPLEMENTED_NEEDS_TEST",
                "evidence_ref": "",
                "review_note": ""
            }
            rows_out.append(prior_proofs.get((b_id, c_id, s_id), candidate))

    fieldnames = [
        "book_id", "chapter_id", "section_id", "expected_behavior",
        "runtime_symbol", "assertion_id", "actual_result",
        "source_version", "rights_basis", "status", "evidence_ref", "review_note"
    ]

    with open(OUTPUT_CSV, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows_out)

    print(f"Sucesso: {len(rows_out)} seções gravadas em {OUTPUT_CSV}")

if __name__ == "__main__":
    generate()
