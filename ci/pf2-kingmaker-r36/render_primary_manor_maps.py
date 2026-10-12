import fitz
from pathlib import Path
source=fitz.open('Kingmaker.pdf');out=Path('current/ci/pf2-kingmaker-r36/maps')
for name,index,bounds,compass,title in [('east',26,(89,69,528,351),[(105,82,162,145),(125,145,137,157)],(289,225,528,251)),('west',34,(95,69,529,350),[(456,82,517,145),(480,145,490,164)],(128,223,340,249))]:
 clip=next(d for d in source[index].get_drawings(extended=True) if d['type']=='clip' and len(d.get('items',[]))>4)
 doc=fitz.open();doc.insert_pdf(source,from_page=index,to_page=index);page=doc[0];shape=page.new_shape();shape.draw_rect(fitz.Rect(bounds))
 for item in clip['items']:
  if item[0]=='l':shape.draw_line(item[1],item[2])
  elif item[0]=='c':shape.draw_bezier(item[1],item[2],item[3],item[4])
 for rect in compass+[title]:shape.draw_rect(fitz.Rect(rect))
 shape.finish(color=None,fill=(1,1,1),even_odd=True);shape.commit();pix=page.get_pixmap(matrix=fitz.Matrix(3,3),clip=fitz.Rect(bounds));pix.save(str(out/('manor_'+name+'_primary.jpg')),jpg_quality=92);print(name,pix.width,pix.height)
