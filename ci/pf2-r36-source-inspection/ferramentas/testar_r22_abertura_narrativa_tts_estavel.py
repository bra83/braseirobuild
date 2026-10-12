#!/usr/bin/env python3
"""Gate histórico R22. A implementação atual é validada pelo gate cumulativo R23."""
from pathlib import Path
import subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
print('R22_SUPERSEDED: executando invariantes atuais R23 de abertura + TTS.')
raise SystemExit(subprocess.call([sys.executable,str(ROOT/'ferramentas'/'testar_r23_direcao_narrativa_tts.py')],cwd=str(ROOT)))
