#!/usr/bin/env python3
"""Export the pinned XMM actions that still require manual effect resolution.

The report contains identifiers and JSON pointers only. It deliberately omits
source prose so it can be used as a durable implementation backlog.
"""
import argparse
import json
from pathlib import Path

import verify_xmm_503_source as verify


def export(source_bytes, project_root):
    source_doc = json.loads(source_bytes)
    source = source_doc["monster"]
    compiled_path = project_root / verify.JAVA_RUNTIME
    compiled_rows = verify.java_rows(compiled_path.read_text(encoding="utf-8"))
    compiled_by_id = {row["id"]: row for row in compiled_rows}
    mappings = (("action", "actions", None), ("bonus", "special_actions", "bonus_action"),
                ("reaction", "special_actions", "reaction"),
                ("legendary", "special_actions", "legendary_action"))
    entries = []
    for monster_index, monster in enumerate(source):
        monster_name = monster.get("name", "")
        compiled_monster = compiled_by_id.get(monster_name, {})
        for source_field, compiled_field, category in mappings:
            destination = compiled_monster.get(compiled_field, [])
            if category:
                destination = [action for action in destination
                               if action.get("category") == category]
            for slot, action in enumerate(monster.get(source_field, [])):
                if not verify.source_action_requires_manual_resolution(action, monster_name):
                    continue
                compiled_action = destination[slot] if slot < len(destination) else {}
                if verify.source_action_has_typed_condition_contract(
                        monster_name, action, compiled_action):
                    continue
                if not compiled_action.get("manual_unmodeled_source_effect_required", False):
                    raise ValueError(f"missing_manual_guard:{monster_name}:{action.get('name', '')}")
                entries.append({
                    "source_pointer": f"/monster/{monster_index}/{source_field}/{slot}",
                    "monster": monster_name,
                    "action": verify.RCH.sub("", action.get("name", "")).strip(),
                    "category": category or "action",
                    "compiled_manual_guard": True,
                })
    entries.sort(key=lambda item: (item["monster"].casefold(), item["category"],
                                   item["source_pointer"]))
    errors, expected_count = verify.check_unmodeled_action_effects(
        source, compiled_rows)
    if errors:
        raise ValueError("source_comparison_failed:" + ";".join(errors[:10]))
    if expected_count != len(entries):
        raise ValueError(f"manual_backlog_count_mismatch:{expected_count}:{len(entries)}")
    return {
        "schema": "XMM_MANUAL_EFFECT_BACKLOG_V1",
        "source_commit": verify.PIN,
        "bestiary_git_blob_sha1": verify.blob_sha(source_bytes),
        "source_monsters": len(source),
        "manual_effect_count": len(entries),
        "entries": entries,
        "source_prose_included": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--project-root", type=Path, default=verify.ROOT)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    source_bytes = (args.source_dir / "bestiary-xmm.json").read_bytes()
    result = export(source_bytes, args.project_root.resolve())
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8")
    print(json.dumps({key: result[key] for key in (
        "schema", "source_commit", "bestiary_git_blob_sha1", "source_monsters",
        "manual_effect_count", "source_prose_included")}, indent=2))


if __name__ == "__main__":
    main()
