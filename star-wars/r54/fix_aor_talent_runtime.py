#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_aor_talent_runtime.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
vtt = root / 'app/src/main/assets/vtt'
source = vtt / 'js/rules/talentTreesData.mjs'
aor_source = vtt / 'js/expansion/aorTalentTreesData.mjs'
bundle = vtt / 'js/core/uiBridge.bundle.js'
for p in (source, aor_source, bundle):
    if not p.is_file():
        raise SystemExit(f'missing AoR runtime input: {p}')


def decode_object_after(text: str, marker: str) -> tuple[dict, int, int]:
    pos = text.find(marker)
    if pos < 0:
        raise SystemExit(f'marker not found: {marker}')
    start = text.find('{', pos + len(marker))
    if start < 0:
        raise SystemExit(f'object start not found after: {marker}')
    obj, consumed = json.JSONDecoder().raw_decode(text[start:])
    if not isinstance(obj, dict):
        raise SystemExit(f'expected object after: {marker}')
    return obj, start, start + consumed


def counts(trees: dict) -> tuple[int, int, int]:
    careers = len(trees)
    specs = 0
    positions = 0
    for specializations in trees.values():
        if not isinstance(specializations, dict):
            raise SystemExit('invalid talent tree career payload')
        specs += len(specializations)
        for tree in specializations.values():
            if not isinstance(tree, dict) or not isinstance(tree.get('nodes'), list):
                raise SystemExit('invalid specialization tree payload')
            positions += len(tree['nodes'])
    return careers, specs, positions


aor_text = aor_source.read_text('utf-8')
aor, _, _ = decode_object_after(aor_text, 'export const AOR_TALENT_TREES=Object.freeze(')
aor_counts = counts(aor)
if aor_counts != (6, 18, 360):
    raise SystemExit(f'AoR catalog unexpectedly incomplete: {aor_counts}')


def merge_catalog(text: str, marker: str, label: str) -> tuple[str, tuple[int, int, int], tuple[int, int, int], bool]:
    catalog, start, end = decode_object_after(text, marker)
    before_counts = counts(catalog)
    overlap = set(catalog).intersection(aor)
    aor_keys = set(aor)

    if overlap:
        if overlap != aor_keys:
            raise SystemExit(f'{label}: partial AoR career overlap: {sorted(overlap)}')
        for key in sorted(aor_keys):
            if catalog.get(key) != aor.get(key):
                raise SystemExit(f'{label}: AoR career payload diverged: {key}')
        merged_counts = counts(catalog)
        if merged_counts != (12, 36, 720):
            raise SystemExit(f'{label}: AoR keys present but combined counts are wrong: {merged_counts}')
        base_counts = tuple(a - b for a, b in zip(merged_counts, aor_counts))
        if base_counts != (6, 18, 360):
            raise SystemExit(f'{label}: inferred EotE baseline counts are wrong: {base_counts}')
        return text, base_counts, merged_counts, False

    if before_counts != (6, 18, 360):
        raise SystemExit(f'{label}: unexpected EotE-only baseline: {before_counts}')
    merged = dict(catalog)
    merged.update(aor)
    merged_counts = counts(merged)
    if merged_counts != (12, 36, 720):
        raise SystemExit(f'{label}: merged talent catalog count mismatch: {merged_counts}')
    payload = json.dumps(merged, ensure_ascii=False, separators=(',', ':'))
    return text[:start] + payload + text[end:], before_counts, merged_counts, True


src_text = source.read_text('utf-8')
src_text, eote_counts, merged_counts, source_changed = merge_catalog(
    src_text,
    'export const TALENT_TREES = Object.freeze(',
    'source',
)
src_text = src_text.replace(
    '// Generated EotE Core talent trees. Source: Edge of the Empire Core specialization trees.\n// 6 careers / 18 specializations / 360 positions; no active Age of Rebellion tree data.',
    '// Combined audited Core talent trees: Edge of the Empire + Age of Rebellion.\n// AoR data is sourced from ../expansion/aorTalentTreesData.mjs and activated additively; EotE data is preserved unchanged.',
    1,
)
source.write_text(src_text, 'utf-8')

bundle_text = bundle.read_text('utf-8')
bundle_text, bundle_eote_counts, bundle_merged_counts, bundle_changed = merge_catalog(
    bundle_text,
    'exports.TALENT_TREES = Object.freeze(',
    'bundle',
)
if bundle_eote_counts != eote_counts or bundle_merged_counts != merged_counts:
    raise SystemExit('source/bundle talent catalog counts diverge')
bundle_text = bundle_text.replace(
    '// Generated EotE Core talent trees. Source: Edge of the Empire Core specialization trees.\n// 6 careers / 18 specializations / 360 positions; no active Age of Rebellion tree data.',
    '// Combined audited Core talent trees: Edge of the Empire + Age of Rebellion.\n// Existing AoR Core tree data is active additively; EotE data is preserved unchanged.',
    1,
)
bundle.write_text(bundle_text, 'utf-8')

# Functional gates on a real AoR tree and preservation of the original EotE catalog.
merged_source, _, _ = decode_object_after(source.read_text('utf-8'), 'export const TALENT_TREES = Object.freeze(')
merged_bundle, _, _ = decode_object_after(bundle.read_text('utf-8'), 'exports.TALENT_TREES = Object.freeze(')
for label, catalog in [('source', merged_source), ('bundle', merged_bundle)]:
    if counts(catalog) != (12, 36, 720):
        raise SystemExit(f'{label}: final combined counts are wrong: {counts(catalog)}')
    ace_gunner = catalog.get('ace', {}).get('gunner')
    if not isinstance(ace_gunner, dict) or len(ace_gunner.get('nodes', [])) != 20 or not ace_gunner.get('edges'):
        raise SystemExit(f'{label}: AoR ace/gunner tree is not active')
    if 'bounty_hunter' not in catalog:
        raise SystemExit(f'{label}: EotE talent trees were not preserved')
    text = source.read_text('utf-8') if label == 'source' else bundle.read_text('utf-8')
    if 'no active Age of Rebellion tree data' in text:
        raise SystemExit(f'{label}: inactive AoR marker still present')

print('R54_AOR_TALENT_RUNTIME_PASS', {
    'eote': eote_counts,
    'aor': aor_counts,
    'merged': merged_counts,
    'source_changed': source_changed,
    'bundle_changed': bundle_changed,
    'sample': 'ace/gunner',
})
