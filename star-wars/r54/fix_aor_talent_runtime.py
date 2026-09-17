#!/usr/bin/env python3
from __future__ import annotations

import json
import re
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
if aor_counts[0] < 6 or aor_counts[1] < 18 or aor_counts[2] < 360:
    raise SystemExit(f'AoR catalog unexpectedly incomplete: {aor_counts}')


def merge_catalog(text: str, marker: str) -> tuple[str, tuple[int, int, int], tuple[int, int, int]]:
    eote, start, end = decode_object_after(text, marker)
    eote_counts = counts(eote)
    overlap = sorted(set(eote).intersection(aor))
    if overlap:
        raise SystemExit('career key collision while activating AoR: ' + ', '.join(overlap))
    merged = dict(eote)
    merged.update(aor)
    merged_counts = counts(merged)
    if merged_counts != tuple(a + b for a, b in zip(eote_counts, aor_counts)):
        raise SystemExit(f'merged talent catalog count mismatch: {merged_counts}')
    payload = json.dumps(merged, ensure_ascii=False, separators=(',', ':'))
    return text[:start] + payload + text[end:], eote_counts, merged_counts

# Keep the standalone rules module and the embedded browser bundle identical.
src_text = source.read_text('utf-8')
src_text, eote_counts, merged_counts = merge_catalog(
    src_text,
    'export const TALENT_TREES = Object.freeze('
)
src_text = src_text.replace(
    '// Generated EotE Core talent trees. Source: Edge of the Empire Core specialization trees.\n// 6 careers / 18 specializations / 360 positions; no active Age of Rebellion tree data.',
    '// Combined audited Core talent trees: Edge of the Empire + Age of Rebellion.\n// AoR data is sourced from ../expansion/aorTalentTreesData.mjs and activated additively; EotE data is preserved unchanged.',
    1,
)
source.write_text(src_text, 'utf-8')

bundle_text = bundle.read_text('utf-8')
bundle_text, bundle_eote_counts, bundle_merged_counts = merge_catalog(
    bundle_text,
    'exports.TALENT_TREES = Object.freeze('
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
    ace_gunner = catalog.get('ace', {}).get('gunner')
    if not isinstance(ace_gunner, dict) or len(ace_gunner.get('nodes', [])) != 20 or not ace_gunner.get('edges'):
        raise SystemExit(f'{label}: AoR ace/gunner tree is not active')
    if 'bounty_hunter' not in catalog:
        raise SystemExit(f'{label}: EotE talent trees were not preserved')
    if 'no active Age of Rebellion tree data' in (source.read_text('utf-8') if label == 'source' else bundle.read_text('utf-8')):
        raise SystemExit(f'{label}: inactive AoR marker still present')

print('R54_AOR_TALENT_RUNTIME_PASS', {
    'eote': eote_counts,
    'aor': aor_counts,
    'merged': merged_counts,
    'sample': 'ace/gunner',
})
