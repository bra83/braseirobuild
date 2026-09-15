from pathlib import Path
import tempfile
from PIL import Image, ImageDraw
from visual_diff import compare

def main():
    with tempfile.TemporaryDirectory() as td:
        td=Path(td); a=td/'a.png'; b=td/'b.png'
        Image.new('RGB',(415,915),'black').save(a)
        Image.new('RGB',(415,915),'black').save(b)
        r=compare(a,b,td/'same',16); assert r.changed_pixels==0
        im=Image.open(b); ImageDraw.Draw(im).rectangle((10,10,30,30),fill='white'); im.save(b)
        r=compare(a,b,td/'changed',16); assert r.changed_pixels>0
        assert (td/'changed/overlay_50.png').exists() and (td/'changed/binary_diff.png').exists()
        print('VISUAL_DIFF_HARNESS_PASS self=0 changed_detected=PASS tolerance=16')
if __name__=='__main__': main()
