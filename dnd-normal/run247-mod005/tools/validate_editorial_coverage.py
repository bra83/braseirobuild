#!/usr/bin/env python3
"""Validador de evidência editorial D&D.

Este utilitário valida INVENTÁRIO por padrão. Ele jamais converte a existência
de uma classe/teste em prova da semântica de uma seção. Use --require-complete
como gate de homologação (retorna 2 enquanto houver seções pendentes).
"""
import argparse
import csv
import hashlib
import json
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
SECTIONS = Path("docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv")
ACCEPTANCE = Path("docs/EDITORIAL_SECTION_ACCEPTANCE.csv")
BOOKS = {"XMM", "XSAC", "FRHOF", "FRAIF", "EFA", "AU", "RHW", "ABH", "LFL", "NF"}
STATUSES = {
    "IMPLEMENTED_AND_TESTED", "IMPLEMENTED_NEEDS_TEST", "MISSING",
    "NOT_APPLICABLE", "SOURCE_UNAVAILABLE", "REUSE_NOT_AUTHORIZED"
}
ACCEPTANCE_COLUMNS = {
    "book_id", "chapter_id", "section_id", "expected_behavior", "runtime_symbol",
    "assertion_id", "actual_result", "source_version", "rights_basis", "status"
}
SECTIONS_COLUMNS = {
    "book_id", "chapter_id", "section_id", "section_title", "source_url",
    "evidence_of_access", "verification_result", "implementation_path"
}
PLACEHOLDERS = {"UNVERIFIED", "REVIEW_REQUIRED", "NOT_RUN", "NONE", "N/A", "UNKNOWN",
                "ACCEPTED", "VERIFIED_PASSED", "HTTP 200"}

def load_csv(root, relative, required, errors):
    path = root / relative
    if not path.is_file():
        errors.append(f"Arquivo ausente: {relative}")
        return []
    with path.open(encoding="utf-8-sig", newline="") as fh:
        reader = csv.DictReader(fh)
        if reader.fieldnames is None or not required.issubset(reader.fieldnames):
            errors.append(f"Colunas obrigatórias ausentes em {relative}: {sorted(required-set(reader.fieldnames or []))}")
            return []
        if len(reader.fieldnames) != len(set(reader.fieldnames)):
            errors.append(f"Colunas duplicadas em {relative}")
        rows = list(reader)
    for line, row in enumerate(rows, 2):
        if None in row:
            errors.append(f"{relative}:{line}: CSV com campos extras/desalinhados")
        if any(value is None for value in row.values()):
            errors.append(f"{relative}:{line}: CSV com campos ausentes")
    return rows

def key(row):
    return tuple((row.get(f) or "").strip() for f in ("book_id", "chapter_id", "section_id"))

def within_project(root, relative):
    """Rejeitar caminhos absolutos ou que escapem do checkout."""
    if not relative or Path(relative).is_absolute():
        return None
    base = root.resolve()
    resolved = (base / relative).resolve()
    if resolved == base or base not in resolved.parents:
        return None
    return resolved

def test_method_present(java_file, method_name):
    """Java JUnit methods are void; do not confuse a call with its declaration."""
    if not java_file.is_file():
        return False
    text = java_file.read_text(encoding="utf-8", errors="replace")
    return bool(re.search(r"\bvoid\s+" + re.escape(method_name) + r"\s*\(", text))


def runtime_method_present(java_file, method_name):
    """Accept Java declarations with non-void return types, never mere call sites.

    We require a return type, a parameter list and a method body. Java source
    without a declared implementation cannot certify an editorial section.
    """
    if not java_file.is_file() or not re.fullmatch(r"[A-Za-z_$][\w$]*", method_name or ""):
        return False
    source = java_file.read_text(encoding="utf-8", errors="replace")
    source = re.sub(r"/\*[\s\S]*?\*/|//[^\r\n]*", "", source)
    signature = re.compile(
        r"(?:^|[{};])\s*"
        r"(?:(?:public|protected|private|static|final|synchronized|native|abstract|default)\s+)*"
        r"(?:<[^(){};]+>\s+)?"
        r"(?:[\w$.\[\]<>?,]+\s+)+"
        + re.escape(method_name)
        + r"\s*\([^;{}]*\)\s*(?:throws\s+[^\{;]+)?\{",
        re.MULTILINE,
    )
    return bool(signature.search(source))

def validate_evidence(row, root, errors):
    """Prova somente um caso com fonte, método e XML reais, não mera classe."""
    prefix = ":".join(key(row))
    evidence_rel = (row.get("evidence_ref") or "").strip()
    evidence_file = within_project(root, evidence_rel)
    if not evidence_file or not evidence_file.is_file() or not evidence_rel.startswith("docs/editorial-evidence/"):
        errors.append(f"{prefix}: IMPLEMENTED_AND_TESTED sem comprovante JSON em docs/editorial-evidence/")
        return
    try:
        proof = json.loads(evidence_file.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        errors.append(f"{prefix}: JSON de evidência inválido: {exc}")
        return
    for field in ("book_id", "chapter_id", "section_id"):
        if str(proof.get(field, "")) != str(row.get(field, "")):
            errors.append(f"{prefix}: comprovante não corresponde a {field}")
    source_sha = str(proof.get("source_sha256", ""))
    if not re.fullmatch(r"[0-9a-fA-F]{64}", source_sha):
        errors.append(f"{prefix}: source_sha256 verificável ausente")
    source_ref = str(proof.get("source_url", ""))
    if not source_ref.startswith(("https://", "http://")):
        errors.append(f"{prefix}: URL de origem ausente")
    for f in ("rights_basis", "source_version", "expected_game_behavior", "observed_game_behavior"):
        v = str(proof.get(f, "")).strip()
        if not v or v.upper() in PLACEHOLDERS:
            errors.append(f"{prefix}: evidência insuficiente em {f}")
    if str(row.get("rights_basis", "")).upper() in PLACEHOLDERS or str(row.get("source_version", "")).upper() in PLACEHOLDERS:
        errors.append(f"{prefix}: fonte/direitos genéricos não podem homologar uma seção")
    symbol = str(row.get("runtime_symbol", "")).strip()
    if not re.fullmatch(r"(?:[A-Za-z_]\w*\.)+[A-Za-z_]\w*", symbol):
        errors.append(f"{prefix}: runtime_symbol inválido")
    else:
        java_path = root / "app/src/main/java" / (symbol.replace(".", "/") + ".java")
        method = str(proof.get("runtime_method", "")).strip()
        if not method or not runtime_method_present(java_path, method):
            errors.append(f"{prefix}: método runtime específico não encontrado: {symbol}#{method}")
    assertion = str(row.get("assertion_id", ""))
    if "#" not in assertion:
        errors.append(f"{prefix}: assertion_id deve ter Classe#metodo")
        return
    klass, method = assertion.split("#", 1)
    test_paths = list((root / "app/src/test/java").rglob(klass + ".java"))
    if len(test_paths) != 1 or not test_method_present(test_paths[0], method):
        errors.append(f"{prefix}: método de teste não localizado sem ambiguidade: {assertion}")
        return
    test_source = test_paths[0].read_text(encoding="utf-8", errors="replace")
    # Exige chave exata no teste; não aceita 149 seções ligadas por inferência a teste genérico.
    if f'"{row["section_id"]}"' not in test_source and f'"{row["book_id"]}:{row["chapter_id"]}:{row["section_id"]}"' not in test_source:
        errors.append(f"{prefix}: o teste {assertion} não referencia a seção de modo identificável")
    xml_path = within_project(root, str(proof.get("test_xml", "")))
    if not xml_path or not xml_path.is_file():
        errors.append(f"{prefix}: XML de execução não localizado no projeto")
        return
    try:
        xml_root = ET.parse(xml_path).getroot()
        testcases = [
            case for case in xml_root.iter("testcase")
            if case.get("name") == method and case.get("classname", "").endswith(klass)
        ]
        if len(testcases) != 1 or any(c.find("failure") is not None or c.find("error") is not None or c.find("skipped") is not None for c in testcases):
            errors.append(f"{prefix}: XML não comprova caso executado/aprovado: {assertion}")
    except (ET.ParseError, OSError) as exc:
        errors.append(f"{prefix}: XML ilegível: {exc}")
    if (row.get("actual_result") or "") != "VERIFIED_PASSED":
        errors.append(f"{prefix}: resultado real incompatível com IMPLEMENTED_AND_TESTED")

def inspect(root):
    errors = []
    sections = load_csv(root, SECTIONS, SECTIONS_COLUMNS, errors)
    ledger = load_csv(root, ACCEPTANCE, ACCEPTANCE_COLUMNS, errors)
    section_map = {}
    for i, row in enumerate(sections, 2):
        k = key(row)
        if not all(k) or k[0] not in BOOKS:
            errors.append(f"{SECTIONS}:{i}: chave inválida {k}")
        if k in section_map:
            errors.append(f"{SECTIONS}:{i}: seção duplicada {k}")
        section_map[k] = row
    ledger_map = {}
    certified_assertions = {}
    status_counts = Counter()
    by_book = {b: Counter() for b in sorted(BOOKS)}
    for i, row in enumerate(ledger, 2):
        k = key(row)
        name = ":".join(k)
        if k in ledger_map:
            errors.append(f"{ACCEPTANCE}:{i}: linha duplicada {name}")
        ledger_map[k] = row
        if k[0] not in BOOKS:
            errors.append(f"{ACCEPTANCE}:{i}: livro desconhecido {k[0]}")
            continue
        status = (row.get("status") or "").strip()
        status_counts[status] += 1
        by_book[k[0]][status] += 1
        if status not in STATUSES:
            errors.append(f"{ACCEPTANCE}:{i}: estado proibido: {status!r}")
            continue
        reference = section_map.get(k)
        if reference is None:
            errors.append(f"{ACCEPTANCE}:{i}: seção inexistente na matriz-base: {name}")
            continue
        if status == "NOT_APPLICABLE":
            reason = (row.get("review_note") or "").strip()
            if not reason or reason.upper() in PLACEHOLDERS:
                errors.append(f"{name}: NOT_APPLICABLE exige justificativa editorial individual")
        if status == "IMPLEMENTED_AND_TESTED":
            validate_evidence(row, root, errors)
            # Every accepted editorial section must have its OWN executable JUnit
            # method. A generic shared test may still check runtime inventory,
            # but it cannot certify 149 distinct book sections by implication.
            assertion = (row.get("assertion_id") or "").strip()
            if assertion in certified_assertions:
                errors.append(f"{name}: assertion_id compartilhado com {certified_assertions[assertion]}: {assertion}")
            else:
                certified_assertions[assertion] = name
        else:
            if (row.get("actual_result") or "").strip() == "VERIFIED_PASSED":
                errors.append(f"{name}: resultado VERiFIED_PASSED falso para estado {status}")
        # Comparar progresso documental sem tratar alegação de HTTP 200 como evidência.
        if status == "IMPLEMENTED_AND_TESTED" and (reference.get("verification_result") or "") == "NOT_APPLICABLE":
            errors.append(f"{name}: contradição entre matriz-base e ledger")
    for k in sorted(set(section_map) - set(ledger_map)):
        errors.append(f"{ACCEPTANCE}: seção ausente: {k}")
    if len(ledger_map) != len(section_map):
        errors.append(f"Contagem divergente: {len(section_map)} inventariadas / {len(ledger_map)} no ledger")
    return {
        "inventory_sections": len(section_map),
        "ledger_sections": len(ledger_map),
        "statuses": dict(status_counts),
        "by_book": {k: dict(v) for k, v in by_book.items()},
        "certified": status_counts["IMPLEMENTED_AND_TESTED"],
        "pending": len(ledger_map) - status_counts["IMPLEMENTED_AND_TESTED"],
        "errors": errors
    }

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", type=Path, default=ROOT)
    parser.add_argument("--require-complete", action="store_true", help="Falhar se uma seção não estiver comprovada")
    parser.add_argument("--json", action="store_true", help="Imprimir resumo legível por máquina")
    options = parser.parse_args(argv)
    result = inspect(options.project_root)
    if options.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print(f"Inventário: {result['inventory_sections']} seções; "
              f"com prova individual: {result['certified']}; pendentes: {result['pending']}")
        for status, total in sorted(result["statuses"].items()):
            print(f"  {status}: {total}")
        for e in result["errors"][:30]:
            print("ERRO:", e)
        if len(result["errors"]) > 30:
            print(f"... outros {len(result['errors']) - 30} erros")
    if result["errors"]:
        return 1
    if options.require_complete and result["pending"]:
        print("GATE REPROVADO: existem seções sem comprovação individual.", file=sys.stderr)
        return 2
    print("Inventário consistente; isto NÃO é homologação editorial integral.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
