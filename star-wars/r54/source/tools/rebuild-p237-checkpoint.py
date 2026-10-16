#!/usr/bin/env python3
"""Reproducible full source ZIP for Braseiro R54 P237 (no APK compilation).

Usage: python tools/rebuild-p237-checkpoint.py /path/to/output.zip
Reads the project root as the parent of this script's parent. Archive entries
are sorted, metadata timestamps fixed, and bytes are preserved.
"""
from pathlib import Path
import zipfile,sys,stat
root=Path(__file__).resolve().parents[1]
out=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P237_SOURCE_CHECKPOINT.zip')
files=sorted((p for p in root.rglob('*') if p.is_file() and not p.is_symlink()),key=lambda p:p.relative_to(root).as_posix())
with zipfile.ZipFile(out,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for p in files:
  info=zipfile.ZipInfo(p.relative_to(root).as_posix(),(2020,1,1,0,0,0))
  info.create_system=3
  info.external_attr=(stat.S_IFREG|stat.S_IMODE(p.stat().st_mode))<<16
  info.compress_type=zipfile.ZIP_DEFLATED
  z.writestr(info,p.read_bytes(),compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
print(f'ZIP={out} FILES={len(files)} BYTES={out.stat().st_size}')
