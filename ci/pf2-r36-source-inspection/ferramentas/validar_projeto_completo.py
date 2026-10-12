#!/usr/bin/env python3
"""Compatibility entry point for the current Pathfinder 2e release gate.

Historical D&D/Mystara and older Pathfinder validators remain in the tree only
for release archaeology. The authoritative project-level gate for 8.5.0 is
`validar_pathfinder_8_5_v6_map.py`.
"""
from pathlib import Path
import subprocess, sys
ROOT=Path(__file__).resolve().parents[1]
GATE=ROOT/'ferramentas'/'validar_pathfinder_8_5_v6_map.py'
if not GATE.is_file():
    print(f'ERRO: gate PF2e ausente: {GATE}',file=sys.stderr);raise SystemExit(2)
print('PF2_RELEASE_VALIDATOR_REDIRECT: current Pathfinder 8.5.0 / Atlas V6 gate')
raise SystemExit(subprocess.call([sys.executable,str(GATE)],cwd=str(ROOT)))
