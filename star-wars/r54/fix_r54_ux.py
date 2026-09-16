#!/usr/bin/env python3
from __future__ import annotations
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_r54_ux.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
bundle = root / 'app/src/main/assets/vtt/r46/r46-shell.bundle.js'
if not bundle.is_file():
    raise SystemExit(f'missing shell bundle: {bundle}')

s = bundle.read_text('utf-8')
marker = 'R54 UX — FICHA JSON EM DADOS / DIAGNÓSTICO'

if marker not in s:
    old_caps = """    const canBackup = props.dataCapabilities.campaignBackup;\n    const canSnapshots = props.dataCapabilities.snapshots;\n    const canLibrary = props.libraryBridge === 'available' && props.libraryCapabilities.list;"""
    new_caps = """    const canBackup = props.dataCapabilities.campaignBackup;\n    const canSnapshots = props.dataCapabilities.snapshots;\n    // R54 UX — FICHA JSON EM DADOS / DIAGNÓSTICO\n    const canCharacterJson = props.dataCapabilities.characterJson;\n    const hasCharacter = props.campaignRead.status === 'ready' && Boolean(props.campaignRead.character);\n    const canLibrary = props.libraryBridge === 'available' && props.libraryCapabilities.list;"""
    if s.count(old_caps) != 1:
        raise SystemExit(f'capability anchor expected once, got {s.count(old_caps)}')
    s = s.replace(old_caps, new_caps, 1)

    old_anchor = """(0, jsx_runtime_1.jsx)(DataActionNotice, { state: props.libraryActionState, message: props.libraryActionMessage }), (0, jsx_runtime_1.jsxs)(Card, { title: \"Backup e recupera\\u00E7\\u00E3o\", tone: \"soft\", children: ["""
    json_card = r'''(0, jsx_runtime_1.jsx)(DataActionNotice, { state: props.libraryActionState, message: props.libraryActionMessage }), (0, jsx_runtime_1.jsxs)(Card, { title: "Ficha JSON", tone: "soft", children: [(0, jsx_runtime_1.jsx)("p", { style: { margin: '0 0 10px', color: '#C3C8CB' }, children: "Importe ou exporte a ficha do personagem em JSON. A importação usa o Motor Barbara e solicita snapshot de proteção antes de substituir a ficha atual." }), (0, jsx_runtime_1.jsxs)("div", { style: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }, children: [(0, jsx_runtime_1.jsx)("button", { type: "button", style: { ...button, opacity: canCharacterJson && hasCharacter ? 1 : .45 }, disabled: !canCharacterJson || !hasCharacter, onClick: props.exportCharacterJson, children: "Exportar ficha JSON" }), (0, jsx_runtime_1.jsxs)("label", { style: { ...button, display: 'grid', placeItems: 'center', textAlign: 'center', opacity: canCharacterJson ? 1 : .45, cursor: canCharacterJson ? 'pointer' : 'default' }, children: ["Importar ficha JSON", (0, jsx_runtime_1.jsx)("input", { type: "file", accept: "application/json,.json", hidden: true, disabled: !canCharacterJson, onChange: (event) => { const file = event.currentTarget.files?.[0]; if (file) void props.importCharacterJsonFile(file); event.currentTarget.value = ''; } })] })] }), !hasCharacter && canCharacterJson && (0, jsx_runtime_1.jsx)("p", { style: { margin: '8px 0 0', color: '#A9B0B5' }, children: "Nenhuma ficha carregada para exportar. A importação continua disponível." }), !canCharacterJson && (0, jsx_runtime_1.jsx)("p", { style: { margin: '8px 0 0', color: '#A9B0B5' }, children: "Importação/exportação de ficha exige um Motor Barbara compatível." })] }), (0, jsx_runtime_1.jsxs)(Card, { title: "Backup e recupera\u00E7\u00E3o", tone: "soft", children: ['''
    if s.count(old_anchor) != 1:
        raise SystemExit(f'data settings anchor expected once, got {s.count(old_anchor)}')
    s = s.replace(old_anchor, json_card, 1)

bundle.write_text(s, encoding='utf-8')

# Gates: no mechanics are recreated; this UI uses the already-exposed bridge callbacks.
final = bundle.read_text('utf-8')
checks = {
    'marker': marker in final,
    'settings json title': 'title: "Ficha JSON"' in final,
    'export callback': 'onClick: props.exportCharacterJson' in final,
    'import callback': 'props.importCharacterJsonFile(file)' in final,
    'bridge capability': 'const canCharacterJson = props.dataCapabilities.characterJson;' in final,
    'import without character': 'A importação continua disponível.' in final,
}
bad = [k for k, ok in checks.items() if not ok]
if bad:
    raise SystemExit('R54 UX gates failed: ' + ', '.join(bad))
print('R54_UX_JSON_PASS', checks)
