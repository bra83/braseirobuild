import fitz, argparse, hashlib, json
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--pdf',type=Path,required=True);args=parser.parse_args()
out=Path(__file__).resolve().parent/'maps';out.mkdir(exist_ok=True)
source_hash=hashlib.sha256(args.pdf.read_bytes()).hexdigest();manifest=out/'provenance.json'
if manifest.exists() and any(r['source_sha256']!=source_hash for r in json.loads(manifest.read_text())):raise ValueError('Source PDF differs from the verified Kingmaker edition')
source=fitz.open(args.pdf);records=[]
for name,index,bounds,compass,title in [('east',26,(89,69,528,351),[(105,82,162,145),(125,145,137,157)],(289,225,528,251)),('west',34,(95,69,529,350),[(456,82,517,145),(480,145,490,164)],(128,223,340,249))]:
 clip=next(d for d in source[index].get_drawings(extended=True) if d['type']=='clip' and len(d.get('items',[]))>4)
 doc=fitz.open();doc.insert_pdf(source,from_page=index,to_page=index);page=doc[0];shape=page.new_shape();shape.draw_rect(fitz.Rect(bounds))
 for item in clip['items']:
  if item[0]=='l':shape.draw_line(item[1],item[2])
  elif item[0]=='c':shape.draw_bezier(item[1],item[2],item[3],item[4])
 for rect in compass+[title]:shape.draw_rect(fitz.Rect(rect))
 shape.finish(color=None,fill=(1,1,1),even_odd=True);shape.commit();pix=page.get_pixmap(matrix=fitz.Matrix(3,3),clip=fitz.Rect(bounds));pix.save(str(out/('manor_'+name+'_primary.jpg')),jpg_quality=92);print(name,pix.width,pix.height)
 records.append({'asset':'manor_'+name+'_primary.jpg','source_document':'Kingmaker.pdf','source_sha256':source_hash,'printed_page':index,'pdf_page':index+1,'width':pix.width,'height':pix.height,'feet_per_square':5,'image_sha256':hashlib.sha256((out/('manor_'+name+'_primary.jpg')).read_bytes()).hexdigest(),'extraction':'Original PDF artwork, room labels and scale; original vector clipping path preserved. Unrelated page text excluded.'})

manifest.write_text(json.dumps(records,ensure_ascii=False,indent=2)+"\n")
