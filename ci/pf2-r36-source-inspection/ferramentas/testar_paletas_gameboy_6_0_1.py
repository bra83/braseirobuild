#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
root=Path(__file__).resolve().parents[1]
java=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/GameBoyPaletteRegistry.java').read_text(encoding='utf-8')
main=(root/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
asset=json.loads((root/'android/app/src/main/assets/themes/gameboy_hcl_sequential_palettes.json').read_text(encoding='utf-8'))
ver=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line)
checks=[]
def ck(name,ok):
    checks.append((name,bool(ok)))
    print(('OK  ' if ok else 'FALHA  ')+name)
ck('versão 6.0.1 ou sucessora',tuple(map(int,ver.get('VERSION_NAME','0.0.0').split('.')[:3])) >= (6,0,1) and int(ver.get('VERSION_CODE','0'))>=600100)
ck('asset possui 120 paletas',asset.get('palette_count')==120 and len(asset.get('palettes',[]))==120)
codes=[p.get('code') for p in asset['palettes']]
expected=[f'{i:02d}{s}' for i in range(1,61) for s in ('A','B')]
ck('códigos cobrem 01A–60B sem lacunas',codes==expected)
ck('120 nomes preenchidos',all(p.get('name','').strip() for p in asset['palettes']) and len({p['name'] for p in asset['palettes']})==120)
ck('cada paleta possui quatro cores',all(len(p.get('colors',[]))==4 and all(re.fullmatch(r'#[0-9A-F]{6}',c or '') for c in p['colors']) for p in asset['palettes']))
ck('registry Java contém 120 entradas',len(re.findall(r'new Palette\("\d{2}[AB]"',java))==120)
ck('registry possui uiColors adaptativo','uiColors(Palette p)' in java and 'contrast(light,dark)' in java and 'mix(dark,Color.BLACK' in java)
ck('swatches originais permanecem disponíveis','public static int[] swatches' in java)
ck('D&D mantém sete temas e adiciona Game Boy','Sete temas clássicos do Braseiro + 120 paletas Game Boy' in main)
ck('Mystara recebe Game Boy sem perder Old School','Mystara mantém o tema Old School e agora também pode usar as 120 paletas Game Boy' in main)
ck('tema é independente por mundo','theme_mode_dnd' in main and 'theme_mode_mystara' in main and 'themePrefKey()' in main)
ck('Mystara não bloqueia mais o seletor','themeSpinner.setEnabled(!isMystaraWorld())' not in main)
ck('preview exibe quatro swatches','renderThemePreview' in main and 'p.technicalId+" • "+p.name' in main)
ck('seleção é persistida e reaplicada','putString(themePrefKey(),themeMode' in main and 'applyTheme();String screen=currentScreen' in main)
ck('aplicação usa tokens reais da paleta','GameBoyPaletteRegistry.uiColors(mode)' in main and 'gameBoy.length==14' in main)
ck('texto de botão adapta contraste no Game Boy','themedButtonTextColor' in main and 'GameBoyPaletteRegistry.contrast' in main)
ck('nomes extras 58–60 foram incluídos',all(x in codes for x in ('58A','58B','59A','59B','60A','60B')))
print(f'RESULTADO 6.0.1: {sum(v for _,v in checks)}/{len(checks)}')
sys.exit(0 if all(v for _,v in checks) else 1)
