"""Windows UTF-8 Java output regression: preserve all historical test assertions."""
from pathlib import Path
import importlib.util
import shutil

import pytest

PATCH = Path(__file__).with_name("patch_windows_harness_utf8.py")
SOURCE = Path(__file__).resolve().parents[1] / "pf2-r36-source-inspection"


def load():
    spec = importlib.util.spec_from_file_location("r36_windows_utf8", PATCH)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def setup_project(tmp_path, version="9.0.37"):
    mod = load()
    (tmp_path / "version.properties").write_text("VERSION_NAME=" + version + "\n", encoding="utf-8")
    original = {}
    for rel in mod.TESTS:
        source = SOURCE / rel
        assert source.is_file(), "Actual R35 fixture missing: " + str(source)
        dest = tmp_path / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, dest)
        original[dest] = dest.read_text(encoding="utf-8")
    return mod, original


def test_explicit_utf8_for_compiler_and_java_runner_without_changing_assertions(tmp_path):
    mod, original = setup_project(tmp_path)
    changes = mod.patch(tmp_path)
    assert len(changes) == 2
    for path, before in original.items():
        after = path.read_text(encoding="utf-8")
        assert before != after
        for old, new in zip(mod.OLD, mod.NEW):
            assert before.count(old) == 1
            assert after.count(new) == 1
            assert old not in after
            after = after.replace(new, old, 1)
        assert after == before


def test_immutable_r35_refused_with_no_mutation(tmp_path):
    mod, original = setup_project(tmp_path, version="9.0.36")
    with pytest.raises(RuntimeError, match="immutable R35"):
        mod.patch(tmp_path)
    for path, before in original.items():
        assert path.read_text(encoding="utf-8") == before


def test_changed_harness_is_fail_closed_and_does_not_partially_patch(tmp_path):
    mod, original = setup_project(tmp_path)
    changed = tmp_path / mod.TESTS[-1]
    changed.write_text("changed source", encoding="utf-8")
    with pytest.raises(RuntimeError, match="anchor changed"):
        mod.patch(tmp_path)
    for path, before in original.items():
        if path != changed:
            assert path.read_text(encoding="utf-8") == before
