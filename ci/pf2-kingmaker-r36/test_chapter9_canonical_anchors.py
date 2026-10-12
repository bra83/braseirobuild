"""R36 narrative patch: exact quest matching, preflight and safe repeat tests."""
import importlib.util
import json
from pathlib import Path
import pytest

spec = importlib.util.spec_from_file_location(
    "r36_chapter9", Path(__file__).with_name("patch_chapter9_canonical_anchors.py"))
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

def project(tmp_path):
    (tmp_path / "version.properties").write_text("VERSION_NAME=9.0.37\n", encoding="utf-8")
    directory = tmp_path / mod.ASSETS
    directory.mkdir(parents=True)
    for filename, key, _ in mod.ANCHORS:
        (directory / filename).write_text(json.dumps({
            "quests": [{"quest_key": key, "objective": "Objetivo original", "metadata": 7},
                       {"quest_key": "other", "objective": "Não tocar"}]}, ensure_ascii=False), encoding="utf-8")
    return tmp_path, directory

def snapshot(directory):
    return {p.name: p.read_bytes() for p in directory.glob("*.json")}

def test_exact_targets_and_other_quests_untouched(tmp_path):
    root, directory = project(tmp_path)
    mod.patch(root)
    for filename, key, fields in mod.ANCHORS:
        selected, other = json.loads((directory / filename).read_text(encoding="utf-8"))["quests"]
        assert selected["quest_key"] == key and selected["metadata"] == 7
        assert other["objective"] == "Não tocar"
        if "objective_append" in fields:
            assert selected["objective"] == "Objetivo original " + fields["objective_append"]
        else:
            assert selected["title"] == fields["title"]
            assert selected["stakes"] == fields["stakes"]

def test_idempotent(tmp_path):
    root, directory = project(tmp_path)
    mod.patch(root)
    before = snapshot(directory)
    mod.patch(root)
    assert snapshot(directory) == before

def test_missing_quest_no_partial_write(tmp_path):
    root, directory = project(tmp_path)
    (directory / mod.ANCHORS[-1][0]).write_text('{"quests":[{"quest_key":"wrong"}]}', encoding="utf-8")
    before = snapshot(directory)
    with pytest.raises(ValueError, match="Expected exactly one quest"):
        mod.patch(root)
    assert snapshot(directory) == before

def test_missing_asset_no_partial_write(tmp_path):
    root, directory = project(tmp_path)
    (directory / mod.ANCHORS[-1][0]).unlink()
    before = snapshot(directory)
    with pytest.raises(FileNotFoundError):
        mod.patch(root)
    assert snapshot(directory) == before

def test_duplicate_quest_rejected(tmp_path):
    root, directory = project(tmp_path)
    filename, key, _ = mod.ANCHORS[0]
    (directory / filename).write_text(json.dumps({"quests":[{"quest_key":key},{"quest_key":key}]}),encoding="utf-8")
    before = snapshot(directory)
    with pytest.raises(ValueError, match="found 2"):
        mod.patch(root)
    assert snapshot(directory) == before

def test_r35_never_modified(tmp_path):
    root, directory = project(tmp_path)
    (root / "version.properties").write_text("VERSION_NAME=9.0.36\n", encoding="utf-8")
    before = snapshot(directory)
    with pytest.raises(RuntimeError, match="R36"):
        mod.patch(root)
    assert snapshot(directory) == before

def test_invalid_json_no_partial_write(tmp_path):
    root, directory = project(tmp_path)
    (directory / mod.ANCHORS[-1][0]).write_text("{ broken", encoding="utf-8")
    before = snapshot(directory)
    with pytest.raises(json.JSONDecodeError):
        mod.patch(root)
    assert snapshot(directory) == before
