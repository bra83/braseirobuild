import ast
import glob
import os

test_files = sorted(glob.glob("tests_pf2/test_kingmaker_*.py"))
print(f"Total test_kingmaker_*.py files: {len(test_files)}")

suspicious_patterns = [
    "assert True",
    "assert 1 == 1",
    "assert 1",
    "mock.return_value",
    "pytest.skip",
    "unittest.mock",
    "MagicMock",
]

report = []

for tf in test_files:
    fname = os.path.basename(tf)
    with open(tf, "r", encoding="utf-8") as f:
        content = f.read()

    lines = content.splitlines()

    # Search for suspicious patterns
    found_patterns = []
    for p in suspicious_patterns:
        for idx, line in enumerate(lines):
            stripped = line.strip()
            if p in stripped and not stripped.startswith("#"):
                found_patterns.append((p, idx + 1, stripped))

    # AST check: find all test_* functions and check if any are empty, just pass, or just assert True
    try:
        tree = ast.parse(content, filename=tf)
        test_funcs = [node for node in ast.walk(tree) if isinstance(node, ast.FunctionDef) and node.name.startswith("test_")]
        empty_tests = []
        for fn in test_funcs:
            # check body
            if len(fn.body) == 0:
                empty_tests.append(fn.name)
            elif len(fn.body) == 1:
                stmt = fn.body[0]
                if isinstance(stmt, ast.Pass):
                    empty_tests.append(f"{fn.name} (only pass)")
                elif isinstance(stmt, ast.Expr) and isinstance(stmt.value, ast.Constant) and isinstance(stmt.value.value, str):
                    empty_tests.append(f"{fn.name} (only docstring)")
                elif isinstance(stmt, ast.Assert):
                    if isinstance(stmt.test, ast.Constant) and stmt.test.value is True:
                        empty_tests.append(f"{fn.name} (only assert True)")
    except Exception as e:
        empty_tests = [f"AST ERROR: {e}"]
        test_funcs = []

    report.append({
        "file": fname,
        "total_lines": len(lines),
        "test_count": len(test_funcs),
        "suspicious": found_patterns,
        "empty_tests": empty_tests
    })

print("\n--- TEST SUITE FORENSIC AUDIT ---")
for r in report:
    print(f"\n{r['file']} (tests: {r['test_count']}, lines: {r['total_lines']}):")
    if r['empty_tests']:
        print("  EMPTY / SHORT-CIRCUITED TESTS FOUND:", r['empty_tests'])
    else:
        print("  Empty tests: 0")
    if r['suspicious']:
        print("  Suspicious patterns found:")
        for s in r['suspicious']:
            print(f"    Line {s[1]}: {s[0]} -> {s[2]}")
    else:
        print("  Suspicious patterns: 0")
