#!/usr/bin/env python3
"""Stable alias for the newest Pathfinder 8.x release validator."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
GATE=ROOT/'ferramentas'/'validar_pathfinder_8_5_v6_map.py'
raise SystemExit(subprocess.call([sys.executable,str(GATE)],cwd=str(ROOT)))
