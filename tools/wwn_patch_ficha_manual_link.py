from pathlib import Path
p=Path('app/src/main/assets/www/ficha.js')
s=p.read_text()
old='<button type="button" class="generate-hero" data-guided>CRIAR ASSISTIDO</button><button type="button" class="generate-hero" data-generate>GERAR ALEATÓRIO</button>'
new='<button type="button" class="generate-hero" data-manual>CRIAÇÃO MANUAL</button><button type="button" class="generate-hero" data-guided>CRIAR ASSISTIDO</button><button type="button" class="generate-hero" data-generate>GERAR ALEATÓRIO</button>'
if 'data-manual' not in s:
    if old not in s: raise SystemExit('hero action marker not found')
    s=s.replace(old,new,1)
anchor="drawer.querySelector('[data-close]').onclick=()=>{drawer.hidden=true};"
link=anchor+"\ndrawer.querySelector('[data-manual]').onclick=()=>{location.href='criacao.html'};"
if "[data-manual]').onclick" not in s:
    if anchor not in s: raise SystemExit('drawer close marker not found')
    s=s.replace(anchor,link,1)
p.write_text(s)
