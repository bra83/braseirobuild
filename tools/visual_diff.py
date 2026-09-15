from __future__ import annotations
from dataclasses import dataclass, asdict
from pathlib import Path
import argparse, json
import numpy as np
from PIL import Image, ImageChops

@dataclass
class DiffReport:
    width:int; height:int; tolerance:int; changed_pixels:int; total_pixels:int; changed_ratio:float; mean_abs_error:float; max_abs_error:int

def load_rgb(path): return Image.open(path).convert('RGB')

def compare(reference_path, actual_path, out_dir, tolerance=16, mask_path=None):
    out=Path(out_dir); out.mkdir(parents=True,exist_ok=True)
    ref=load_rgb(reference_path); act=load_rgb(actual_path)
    if ref.size != act.size: raise ValueError(f'size mismatch ref={ref.size} actual={act.size}')
    ra=np.asarray(ref,dtype=np.int16); aa=np.asarray(act,dtype=np.int16)
    delta=np.abs(ra-aa)
    per_pixel=delta.max(axis=2)
    if mask_path:
        mask=np.asarray(Image.open(mask_path).convert('L'))>0
        per_pixel=np.where(mask,0,per_pixel)
        delta=np.where(mask[...,None],0,delta)
    changed=per_pixel>tolerance
    report=DiffReport(ref.width,ref.height,tolerance,int(changed.sum()),ref.width*ref.height,float(changed.mean()),float(delta.mean()),int(delta.max()))
    Image.blend(ref,act,.5).save(out/'overlay_50.png')
    heat=np.clip(delta*4,0,255).astype(np.uint8)
    Image.fromarray(heat,'RGB').save(out/'pixel_diff.png')
    binary=np.where(changed,255,0).astype(np.uint8)
    Image.fromarray(binary,'L').save(out/'binary_diff.png')
    (out/'diff_report.json').write_text(json.dumps(asdict(report),indent=2))
    return report

if __name__=='__main__':
    ap=argparse.ArgumentParser(); ap.add_argument('reference'); ap.add_argument('actual'); ap.add_argument('out_dir'); ap.add_argument('--tolerance',type=int,default=16); ap.add_argument('--mask')
    a=ap.parse_args(); print(json.dumps(asdict(compare(a.reference,a.actual,a.out_dir,a.tolerance,a.mask)),indent=2))
