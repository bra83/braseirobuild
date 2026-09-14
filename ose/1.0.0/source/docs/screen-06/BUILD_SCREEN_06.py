from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path
import hashlib, json, random

W,H=415,915
here=Path(__file__).resolve().parent
ose=here.parents[1]
assets=ose/'web-ui'/'src'/'assets'
out=here/'SCREEN_06_SESSION_ACTIVE_CANONICAL_415x915.png'
report=here/'SCREEN_06_SESSION_ACTIVE_BUILD_REPORT.json'
PARCH=(236,218,178); INK=(42,31,23); OX=(116,32,29); MUTED=(111,84,55); PANEL=(243,228,196)

random.seed(6)
img=Image.new('RGB',(W,H),PARCH); px=img.load()
for y in range(H):
    for x in range(W):
        n=random.randint(-5,5); c=px[x,y]; px[x,y]=tuple(max(0,min(255,v+n)) for v in c)
img=img.filter(ImageFilter.GaussianBlur(.25)); d=ImageDraw.Draw(img)
fd='/usr/share/fonts/truetype/dejavu'
def F(n,s): return ImageFont.truetype(f'{fd}/{n}',s)
serif=F('DejaVuSerif.ttf',11); serif9=F('DejaVuSerif.ttf',9); serif10=F('DejaVuSerif.ttf',10)
b12=F('DejaVuSerif-Bold.ttf',12); b13=F('DejaVuSerif-Bold.ttf',13); b16=F('DejaVuSerif-Bold.ttf',16)
small=F('DejaVuSans.ttf',7); small8=F('DejaVuSans.ttf',8); smallb8=F('DejaVuSans-Bold.ttf',8)

def A(name,b):
    im=Image.open(assets/name).convert('RGBA'); x,y,w,h=b; r=min(w/im.width,h/im.height)
    im=im.resize((max(1,round(im.width*r)),max(1,round(im.height*r))),Image.Resampling.LANCZOS)
    img.paste(im,(x+(w-im.width)//2,y+(h-im.height)//2),im)
def R(b,r=5,fill=None,outline=INK,w=1): d.rounded_rectangle(b,radius=r,fill=fill,outline=outline,width=w)
def L(x1,y1,x2,y2,fill=INK,w=1): d.line((x1,y1,x2,y2),fill=fill,width=w)
def T(x,y,s,f=serif,fill=INK,anchor=None): d.text((x,y),s,font=f,fill=fill,anchor=anchor)
def lines(s,f,maxw):
    out=[]; cur=''
    for word in s.split():
        t=(cur+' '+word).strip()
        if d.textbbox((0,0),t,font=f)[2] <= maxw: cur=t
        else:
            if cur: out.append(cur)
            cur=word
    if cur: out.append(cur)
    return out
def P(b,s,f=serif,fill=INK,spacing=2):
    x,y,w,h=b; yy=y; bb=d.textbbox((0,0),'Ag',font=f); lh=bb[3]-bb[1]
    for q in lines(s,f,w):
        if yy+lh>y+h: break
        T(x,yy,q,f,fill); yy+=lh+spacing

A('001_logo_ose.png',(15,9,101,48)); T(128,14,'CRIPTA SOB O OUTEIRO',b16); T(129,40,'SESSÃO ATIVA · Mestre automático',smallb8,OX)
A('003_icone_configuracoes.png',(366,12,31,31)); L(15,67,400,67)
T(18,78,'CENA',smallb8,OX)
R((18,90,151,139),5,PANEL); A('032_icone_porta.png',(23,95,34,34)); T(61,99,'CORREDOR',b12); T(61,118,'de pedra · cripta',small8,MUTED)
R((157,90,231,139),5,PANEL); T(194,98,'DIA 3',smallb8,OX,'ma'); T(194,115,'14:20',b13,INK,'ma')
R((237,90,311,139),5,PANEL); T(274,98,'CLIMA',smallb8,OX,'ma'); T(274,116,'NUBLADO',b12,INK,'ma')
R((317,90,397,139),5,PANEL); A('008_status_tocha_30m.png',(326,94,37,37)); T(378,99,'TOCHA',smallb8,OX,'ma'); T(378,116,'30m',b12,INK,'ma')
T(29,156,'MESTRE',smallb8,OX); R((28,169,322,407),6,PANEL,INK,2); R((32,173,318,403),4,None,MUTED,1); L(46,190,304,190,OX); T(175,184,'◆',b12,OX,'mm')
narr=('A escadaria termina num patamar de pedra onde a umidade cobre as juntas do piso. A chama da tocha cria sombras instáveis no corredor atrás de vocês. À frente, uma porta de madeira escurecida pelo tempo fecha a passagem. O ar que vem das frestas é mais frio e traz um cheiro de terra revolvida. Nenhuma criatura está à vista. O silêncio é quebrado apenas pelo estalo da chama.')
P((44,202,260,179),narr,serif10,INK,3); T(302,384,'↕',b12,OX,'ma')
R((333,170,397,240),5,PANEL); A('OSE_SESSION_NEW_A701_icone_tts_ouvir.png',(342,177,46,46)); T(365,222,'OUVIR',smallb8,OX,'ma'); T(365,232,'NARRAÇÃO',small,INK,'ma')
R((333,250,397,331),5,PANEL); A('087_tile_corredor.png',(339,257,52,52)); T(365,311,'LOCAL',smallb8,OX,'ma'); T(365,322,'corredor',small,INK,'ma')
R((333,341,397,407),5,PANEL); T(365,351,'AJUDA',smallb8,OX,'ma'); A('OSE_SESSION_A501_icone_gm_help.png',(349,363,31,31)); T(365,396,'somente consulta',small,INK,'ma')
R((28,420,397,455),4,(232,211,170),MUTED); T(39,427,'RESULTADO VISÍVEL',smallb8,OX); T(39,440,'Posição preservada · contexto atualizado pelo Mestre',serif9)
T(29,470,'SUGESTÕES DO MESTRE',smallb8,OX); T(154,470,'toque para preencher sua reação',small,MUTED)
for label,b in [('EXAMINAR A PORTA',(29,486,139,515)),('OUVIR DO OUTRO LADO',(147,486,275,515)),('VOLTAR AO CORREDOR',(283,486,397,515))]:
    R(b,4,PANEL,OX); T((b[0]+b[2])//2,(b[1]+b[3])//2,label,smallb8,OX,'mm')
T(29,533,'SUA REAÇÃO / PLAYER_REACTION',b12,OX); R((28,551,317,650),5,(244,228,193)); T(40,565,'O que seu personagem faz ou diz?',small8,MUTED)
for y in (588,610,632): L(42,y,301,y,MUTED)
R((326,578,397,624),4,(125,62,37)); T(361,600,'ENVIAR',smallb8,(248,232,199),'mm'); T(361,612,'REAÇÃO',smallb8,(248,232,199),'mm'); T(361,640,'único canal de jogo',small,MUTED,'ma')
L(28,670,397,670,MUTED); T(29,684,'HISTÓRICO / DIÁRIO',b12); T(388,688,'estado conhecido pelo jogador',small,MUTED,'ra'); R((28,705,397,810),4,PANEL)
T(40,719,'14:20 · CRIPTA SOB O OUTEIRO',smallb8,OX); P((40,735,344,56),'O Mestre descreveu uma porta antiga no fim do corredor. A passagem anterior permanece conhecida e a posição atual do grupo não mudou.',serif9,INK,2); L(40,793,385,793,(153,125,85)); T(40,797,'Abrir histórico completo',smallb8,OX)
L(15,833,400,833)
for label,icon,x1,x2 in [('SESSÃO','006_icone_pergaminho.png',25,87),('MAPA','013_icone_mapa.png',113,174),('FICHA','012_icone_livros.png',207,268),('COMPANHIA','014_icone_grupo.png',306,396)]:
    cx=(x1+x2)//2; A(icon,(cx-16,846,32,32)); T(cx,883,label,smallb8,OX if label=='SESSÃO' else INK,'ma')
    if label=='SESSÃO': L(x1,908,x2,908,OX,3)

img.save(out,optimize=True)
b=out.read_bytes(); sha=hashlib.sha256(b).hexdigest()
map_data=json.loads((here/'SCREEN_06_SESSION_ACTIVE_MAP.json').read_text(encoding='utf-8'))
assert Image.open(out).size==(415,915)
for e in map_data['elements']:
    x,y,w,h=e['bbox']; assert x>=0 and y>=0 and w>0 and h>0 and x+w<=415 and y+h<=915
report.write_text(json.dumps({'screen_id':'SCREEN_06_SESSION_ACTIVE','width':415,'height':915,'sha256':sha,'anchors_in_bounds':True,'css_or_app_modified':False,'wave3_started':False},indent=2),encoding='utf-8')
print('SCREEN06_CONCEPT=PASS'); print('SCREEN06_SHA256='+sha); print('SCREEN06_ANCHORS_IN_BOUNDS=PASS')
