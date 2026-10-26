#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/braseiro/talesvtt'
RES=ROOT/'app/src/main/res/drawable'
RESN=ROOT/'app/src/main/res/drawable-nodpi'

def read(p): return p.read_text(encoding='utf-8')
def has(path,*tokens):
    s=read(path); return all(t in s for t in tokens)

checks={}

bottom=JAVA/'components/BottomNavigation.kt'
item=JAVA/'components/BottomNavItem.kt'
header=JAVA/'components/AppHeader.kt'
kid=JAVA/'screens/KidScreen.kt'
mapf=JAVA/'screens/MapScreen.kt'
selector=JAVA/'game/SceneIllustrationSelector.kt'

approved_icons=['scene','kid','group','mystery','map','backpack','menu']
checks['footer_approved_vector_refs']={
    'pass': all(f'R.drawable.nav_{x}_approved' in read(bottom) for x in approved_icons),
    'missing':[x for x in approved_icons if f'R.drawable.nav_{x}_approved' not in read(bottom)]
}
checks['footer_vector_files']={
    'pass': all((RES/f'nav_{x}_approved.xml').is_file() for x in approved_icons),
    'missing':[x for x in approved_icons if not (RES/f'nav_{x}_approved.xml').is_file()]
}
checks['footer_geometry_visual_lock']={
    'pass': has(bottom,'.height(61.dp)','Color(0xFF001116)','Color(0xFF28545C)') and has(item,'Modifier.size(28.dp)','strokeWidth = 3.dp.toPx()','Color(0xFF04525F)','Color(0xFF002C36)'),
}
checks['footer_no_legacy_refs']={
    'pass': not any(re.search(rf'R\.drawable\.nav_{x}(?!_approved)',read(bottom)) for x in approved_icons),
}

# Internal screen parent-tab semantics.
parent_expect={
    'TroubleScreen.kt':'AppDestination.SCENE',
    'ClimaxScreen.kt':'AppDestination.SCENE',
    'EverydayScreen.kt':'AppDestination.SCENE',
    'LocationScreen.kt':'AppDestination.SCENE',
    'ConnectionsScreen.kt':'AppDestination.MYSTERY',
    'EndingScreen.kt':'AppDestination.MYSTERY',
    'LibraryScreen.kt':'AppDestination.MENU',
}
parent_bad=[]
for fn,parent in parent_expect.items():
    s=read(JAVA/'screens'/fn)
    if f'currentDestination = {parent}' not in s: parent_bad.append(f'{fn}:{parent}')
checks['internal_parent_tab_lock']={'pass':not parent_bad,'bad':parent_bad}

# Header world status must remain visually unboxed.
hs=read(header)
right=hs.split('// DIREITA: World Status sem container; informação + ícone de clima',1)[-1]
right=right.split('    }\n  }\n}',1)[0] if '    }\n  }\n}' in right else right
checks['weather_unboxed']={
    'pass':'World Status sem container' in hs and 'RoundedCornerShape' not in right and '.border(' not in right,
}

ks=read(kid)
checks['kid_direct_file_actions']={
    'pass': all(t in ks for t in ['TROCAR FOTO','IMPORTAR JSON','EXPORTAR JSON','CreateDocument("application/json")','OpenDocument()']),
}
checks['kid_portrait_persistence_hook']={'pass':'onChangePortrait' in ks and 'portraitImageBase64' in ks}

ms=read(mapf)
poi_expect=[
    ('Escola','loc_school'),('Casa de Lasse','loc_home'),('Praça Central','loc_shop'),('Loja do Kjell','loc_shop'),
    ('Esconderijo','loc_hideout'),('Torre de Comunicação','loc_radio_tower'),('Loop Industries','loc_loop_facility'),
    ('Estrada Fechada','state_blocked'),('Antiga Pedreira','loc_quarry')
]
poi_bad=[]
for name,asset in poi_expect:
    if name not in ms or f'R.drawable.{asset}' not in ms: poi_bad.append(f'{name}:{asset}')
checks['map_visual_lock']={
    'pass':'R.drawable.map_malaren_clean3' in ms and not poi_bad and len(re.findall(r'MapPoi\("',ms))==9,
    'bad':poi_bad,
    'poi_count':len(re.findall(r'MapPoi\("',ms))
}
checks['map_base_file_present']={'pass':(RESN/'map_malaren_clean3.png').is_file()}

ss=read(selector)
entries=re.findall(r'Entry\("([^"]+)"',ss)
checks['illustration_library_24']={'pass':len(entries)>=24,'count':len(entries),'unique':len(set(entries))}
checks['illustration_selector_mechanics_neutral']={'pass':'never mutates game state' in ss and 'fun choose(scene: SceneState): String' in ss}

failed=[k for k,v in checks.items() if not v.get('pass')]
report={'pass':not failed,'failed':failed,'checks':checks}
out=ROOT/'docs/USER_VISUAL_LOCK_AUDIT.json'
out.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(0 if report['pass'] else 1)
