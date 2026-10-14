"""Fail-closed checks: historical JVM suites keep assertions; add genuine R36 sources."""
import ast
import importlib.util
from pathlib import Path
import pytest

PATCH_FILE = Path(__file__).with_name("patch_legacy_javac_dependencies.py")


def module():
    spec = importlib.util.spec_from_file_location("kingmaker_harness_deps", PATCH_FILE)
    obj = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(obj)
    return obj


def fixture(tmp_path, version="9.0.37"):
    mod = module()
    (tmp_path / "version.properties").write_text(f"VERSION_NAME={version}\n", encoding="utf-8")
    for name in mod.DEPENDENCIES:
        p = tmp_path / mod.JAVA_SOURCE / name
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text("package com.braseiro.pathfinder2e; class Dummy{}\n", encoding="utf-8")
    originals = {}
    for i, (relative, anchor) in enumerate(mod.TESTS.items()):
        p = tmp_path / relative
        p.parent.mkdir(parents=True, exist_ok=True)
        # Match the exact historical javac list indentation and shape.
        file_body = (
            "from pathlib import Path\n"
            "SRC=Path('android/app/src/main/java/com/braseiro/pathfinder2e')\n"
            "compile_cmd=[\n" + anchor + "\n" + ("        'existing-harness',\n" if i else "            'existing-harness',\n") + "]\n"
            "def test_preserved_mechanics_assertion():\n"
            "    assert 2+2==4\n"
        )
        ast.parse(file_body)
        p.write_text(file_body, encoding="utf-8")
        originals[p] = file_body
    return mod, originals


def test_adds_all_four_real_sources_to_every_legacy_compilation_without_removing_tests(tmp_path):
    mod, originals = fixture(tmp_path)
    changes = mod.patch(tmp_path)
    assert len(changes) == 4
    for p, before in originals.items():
        after = p.read_text(encoding="utf-8")
        assert "def test_preserved_mechanics_assertion():" in after
        assert "assert 2+2==4" in after
        assert "'existing-harness'" in after
        for name in mod.DEPENDENCIES:
            assert after.count(name) == 1
        assert after != before
        ast.parse(after)


def test_rejects_r35_and_refuses_to_mutate_files(tmp_path):
    mod, originals = fixture(tmp_path, version="9.0.36")
    with pytest.raises(RuntimeError, match="canonical R35"):
        mod.patch(tmp_path)
    for p, content in originals.items():
        assert p.read_text(encoding="utf-8") == content


def test_preflight_rejects_missing_deps_atomically(tmp_path):
    mod, originals = fixture(tmp_path)
    missing = tmp_path / mod.JAVA_SOURCE / mod.DEPENDENCIES[-1]
    missing.unlink()
    with pytest.raises(RuntimeError, match="Production Java dependency not found"):
        mod.patch(tmp_path)
    for p, content in originals.items():
        assert p.read_text(encoding="utf-8") == content


def test_preflight_refuses_modified_anchor_before_writing_any_harness(tmp_path):
    mod, originals = fixture(tmp_path)
    p = tmp_path / next(iter(mod.TESTS))
    p.write_text("unexpected upstream changes", encoding="utf-8")
    with pytest.raises(RuntimeError, match="source anchor changed"):
        mod.patch(tmp_path)
    for path, content in originals.items():
        if path != p:
            assert path.read_text(encoding="utf-8") == content
