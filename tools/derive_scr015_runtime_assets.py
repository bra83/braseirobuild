from pathlib import Path
from PIL import Image
import shutil, json, hashlib
import cv2
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
SRC=Path('/mnt/data/wwn_m3b/unpacked/assets/encounter_hex_ruinas')
OUT=ROOT/'app/src/main/assets/www/assets/encounter_hex_ruinas_exact'
OUT.mkdir(parents=True,exist_ok=True)

# Fixed controls/navigation are exact approved components. Their labels are invariant UI copy.
static_names=['03_top_search.png','04_top_layers.png','05_top_notes.png','16_choices_heading.png','24_bottom_home.png','25_bottom_explore_active.png','26_bottom_sessions.png','27_bottom_favorites.png','28_bottom_settings.png']
records=[]
for n in static_names:
    dst=OUT/n; shutil.copy2(SRC/n,dst)
    records.append({'source':n,'derived':n,'sha256':hashlib.sha256(dst.read_bytes()).hexdigest(),'dynamic_text_removed':False,'method':'exact_static_component_copy'})

# Mutable/runtime text is removed from the approved component while border/icon/material texture is retained.
# Detection is restricted to known text zones; warm/light glyph pixels and antialiasing are dilated, then inpainted.
# Native DOM text is rendered on top at runtime.
zones={
 '08_contact_badge.png':[(45,7,160,63,44)],
 '11_stat_perigo.png':[(50,5,144,79,34)],
 '12_stat_distancia.png':[(54,5,154,79,34)],
 '13_stat_cobertura.png':[(55,5,156,79,34)],
 '14_stat_reacao.png':[(52,5,152,79,34)],
 '15_narrative_panel.png':[(26,20,615,128,38)],
 '17_button_observar.png':[(90,20,236,72,42)],
 '18_button_abordar.png':[(90,20,238,72,42)],
 '19_button_emboscar.png':[(90,20,236,72,42)],
 '20_button_recuar.png':[(90,20,238,72,42)],
 '21_button_iniciar_combate.png':[(215,20,490,72,42)],
 '22_notes_panel.png':[(68,10,553,43,34),(30,43,553,120,34)],
}
for name,bxs in zones.items():
    src=cv2.imread(str(SRC/name),cv2.IMREAD_UNCHANGED)
    if src is None: raise FileNotFoundError(SRC/name)
    bgr=src[:,:,:3].copy()
    alpha=src[:,:,3] if src.shape[2]==4 else np.full(src.shape[:2],255,np.uint8)
    rgb=cv2.cvtColor(bgr,cv2.COLOR_BGR2RGB)
    vmax=rgb.max(axis=2)
    rr,gg,bb=rgb[:,:,0].astype(int),rgb[:,:,1].astype(int),rgb[:,:,2].astype(int)
    mask=np.zeros(src.shape[:2],np.uint8)
    for x0,y0,x1,y1,thr in bxs:
        z=((vmax[y0:y1,x0:x1]>thr)
           &(rr[y0:y1,x0:x1]>=bb[y0:y1,x0:x1])
           &(gg[y0:y1,x0:x1]>=bb[y0:y1,x0:x1]-4)).astype(np.uint8)*255
        z=cv2.dilate(z,np.ones((5,5),np.uint8),iterations=1)
        mask[y0:y1,x0:x1]=cv2.max(mask[y0:y1,x0:x1],z)
    mask=cv2.bitwise_and(mask,alpha)
    clean=cv2.inpaint(bgr,mask,5,cv2.INPAINT_TELEA)
    out=np.dstack([clean,alpha])
    dst=OUT/name.replace('.png','_chrome.png')
    if not cv2.imwrite(str(dst),out): raise RuntimeError(f'write failed: {dst}')
    records.append({
        'source':name,'derived':dst.name,'sha256':hashlib.sha256(dst.read_bytes()).hexdigest(),
        'dynamic_text_removed':True,'method':'restricted_glyph_inpaint_native_text_overlay',
        'mask_pixels':int(np.count_nonzero(mask))
    })

(OUT/'PROVENANCE.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
print('DERIVE_PASS',len(static_names),'static',len(zones),'dynamic chrome')
