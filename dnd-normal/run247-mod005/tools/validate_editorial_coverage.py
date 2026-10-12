#!/usr/bin/env python3
"""
Validador de Cobertura Editorial e Anti-Falso-Positivo.
Comprova a matriz dos 10 livros D&D 2025-2026:
1. Verifica docs/PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv e docs/EDITORIAL_SECTION_ACCEPTANCE.csv.
2. Comprova correspondência de 483 seções únicas sem duplicações.
3. Rejeita placeholders ou strings genéricas.
4. Valida se cada runtime_symbol corresponde a um arquivo/classe real em app/src/main/java.
5. Valida se cada assertion_id corresponde a uma classe e método de teste real em app/src/test/java.
6. Emite erro e encerra com código não-zero se qualquer divergência for detectada.
"""

import csv
import os
import sys

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SECTIONS_CSV = os.path.join(BASE_DIR, "docs", "PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv")
ACCEPTANCE_CSV = os.path.join(BASE_DIR, "docs", "EDITORIAL_SECTION_ACCEPTANCE.csv")
MAIN_JAVA_DIR = os.path.join(BASE_DIR, "app", "src", "main", "java")
TEST_JAVA_DIR = os.path.join(BASE_DIR, "app", "src", "test", "java")

REQUIRED_BOOKS = {"XMM", "XSAC", "FRHOF", "FRAIF", "EFA", "AU", "RHW", "ABH", "LFL", "NF"}
REQUIRED_ACCEPTANCE_COLS = [
    "book_id", "chapter_id", "section_id", "expected_behavior",
    "runtime_symbol", "assertion_id", "actual_result",
    "source_version", "rights_basis", "status"
]

FORBIDDEN_PLACEHOLDERS = ["TODO", "TBD", "PLACEHOLDER", "FAKE", "DUMMY", "MOCK_ONLY", "PENDING_PROOF"]

def find_java_file_for_symbol(symbol):
    # com.braseiro.dnd5e.canon.FaerunCanonCatalog -> app/src/main/java/com/braseiro/dnd5e/canon/FaerunCanonCatalog.java
    rel_path = symbol.replace(".", os.sep) + ".java"
    full_path = os.path.join(MAIN_JAVA_DIR, rel_path)
    if os.path.exists(full_path):
        return full_path
    # Try finding in tree
    class_name = symbol.split(".")[-1] + ".java"
    for root, _, files in os.walk(MAIN_JAVA_DIR):
        if class_name in files:
            return os.path.join(root, class_name)
    return None

def find_test_method(assertion_id):
    # Format: TestClass#testMethod
    if "#" not in assertion_id:
        return False, f"Formato inválido de assertion_id: {assertion_id} (esperado Classe#metodo)"
    class_name, method_name = assertion_id.split("#", 1)
    file_name = class_name + ".java"
    
    target_path = None
    for root, _, files in os.walk(TEST_JAVA_DIR):
        if file_name in files:
            target_path = os.path.join(root, file_name)
            break
            
    if not target_path or not os.path.exists(target_path):
        return False, f"Classe de teste não encontrada: {file_name}"
        
    with open(target_path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
        if method_name not in content:
            return False, f"Método {method_name} não encontrado em {file_name}"
            
    return True, "OK"

def validate():
    print("=" * 70)
    print("INICIANDO VALIDAÇÃO EDITORIAL ANTI-FALSO-POSITIVO (10 LIVROS D&D)")
    print("=" * 70)
    
    errors = []
    
    if not os.path.exists(SECTIONS_CSV):
        print(f"ERRO FATAL: {SECTIONS_CSV} não encontrado.")
        sys.exit(1)
    if not os.path.exists(ACCEPTANCE_CSV):
        print(f"ERRO FATAL: {ACCEPTANCE_CSV} não encontrado.")
        sys.exit(1)
        
    # 1. Carregar Matriz de Seções
    sections_map = {}
    with open(SECTIONS_CSV, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for i, row in enumerate(reader, start=2):
            key = (row["book_id"], row["chapter_id"], row["section_id"])
            if key in sections_map:
                errors.append(f"Chave duplicada em PLANO_MESTRE_MATRIZ_10_LIVROS_SECOES.csv linha {i}: {key}")
            sections_map[key] = row
            
    print(f"[OK] Matriz base carregada: {len(sections_map)} seções mapeadas.")
    
    # 2. Carregar e Validar Ledger de Aceitação
    acceptance_map = {}
    book_counts = {b: 0 for b in REQUIRED_BOOKS}
    symbol_cache = {}
    test_cache = {}
    
    with open(ACCEPTANCE_CSV, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for col in REQUIRED_ACCEPTANCE_COLS:
            if col not in reader.fieldnames:
                errors.append(f"Coluna obrigatória ausente em EDITORIAL_SECTION_ACCEPTANCE.csv: {col}")
                
        for i, row in enumerate(reader, start=2):
            for col in REQUIRED_ACCEPTANCE_COLS:
                val = (row.get(col) or "").strip()
                if not val:
                    errors.append(f"Linha {i}: Coluna '{col}' vazia.")
                for ph in FORBIDDEN_PLACEHOLDERS:
                    if ph in val.upper():
                        errors.append(f"Linha {i}: Placeholder proibido '{ph}' na coluna '{col}': {val}")
                        
            key = (row.get("book_id"), row.get("chapter_id"), row.get("section_id"))
            if key in acceptance_map:
                errors.append(f"Chave duplicada em EDITORIAL_SECTION_ACCEPTANCE.csv linha {i}: {key}")
            acceptance_map[key] = row
            
            b_id = row.get("book_id")
            if b_id in book_counts:
                book_counts[b_id] += 1
            else:
                errors.append(f"Linha {i}: Livro desconhecido '{b_id}'")
                
            # Validar status e actual_result
            if row.get("status") != "ACCEPTED":
                errors.append(f"Linha {i}: Status inválido '{row.get('status')}'; esperado 'ACCEPTED'")
            if row.get("actual_result") != "VERIFIED_PASSED":
                errors.append(f"Linha {i}: actual_result inválido '{row.get('actual_result')}'; esperado 'VERIFIED_PASSED'")
                
            # Validar runtime_symbol
            sym = row.get("runtime_symbol")
            if sym not in symbol_cache:
                path = find_java_file_for_symbol(sym)
                if not path:
                    symbol_cache[sym] = (False, f"Símbolo de runtime não encontrado no código: {sym}")
                else:
                    symbol_cache[sym] = (True, path)
            if not symbol_cache[sym][0]:
                errors.append(f"Linha {i}: {symbol_cache[sym][1]}")
                
            # Validar assertion_id
            ast = row.get("assertion_id")
            if ast not in test_cache:
                test_cache[ast] = find_test_method(ast)
            if not test_cache[ast][0]:
                errors.append(f"Linha {i}: {test_cache[ast][1]}")

    print(f"[OK] Ledger de aceitação carregado: {len(acceptance_map)} seções auditadas.")
    
    # 3. Correspondência 1-para-1 estrita
    if len(sections_map) != 483:
        errors.append(f"Total de seções na matriz base diferente de 483: {len(sections_map)}")
    if len(acceptance_map) != 483:
        errors.append(f"Total de seções no ledger de aceitação diferente de 483: {len(acceptance_map)}")
        
    diff_missing = set(sections_map.keys()) - set(acceptance_map.keys())
    if diff_missing:
        errors.append(f"Seções presentes na matriz base mas ausentes no ledger: {len(diff_missing)} chaves.")
        
    diff_extra = set(acceptance_map.keys()) - set(sections_map.keys())
    if diff_extra:
        errors.append(f"Seções presentes no ledger mas ausentes na matriz base: {len(diff_extra)} chaves.")
        
    # 4. Exibir sumário por livro
    print("\n--- DISTRIBUIÇÃO AUDITADA POR LIVRO ---")
    for b in sorted(REQUIRED_BOOKS):
        print(f"  * {b:<6}: {book_counts[b]:>3} seções comprovadas e aceitas")
        if book_counts[b] == 0:
            errors.append(f"Livro sem seções aceitas: {b}")
            
    print(f"\n--- SÍMBOLOS E TESTES VALIDADOS NO CÓDIGO FONTE ---")
    print(f"  * Símbolos de runtime únicos verificados: {len(symbol_cache)}")
    print(f"  * Assertivas de teste únicas verificadas: {len(test_cache)}")

    if errors:
        print("\n" + "!" * 70)
        print(f"FALHA NA VALIDAÇÃO EDITORIAL: {len(errors)} ERROS DETECTADOS:")
        for e in errors[:30]:
            print(f"  [X] {e}")
        if len(errors) > 30:
            print(f"  ... e mais {len(errors) - 30} erros suprimidos.")
        print("!" * 70)
        sys.exit(1)
        
    print("\n" + "=" * 70)
    print("SUCESSO: TODAS AS 483 SEÇÕES FORAM COMPROVADAS E ACEITAS SEM FALSOS POSITIVOS!")
    print("=" * 70)
    sys.exit(0)

if __name__ == "__main__":
    validate()
