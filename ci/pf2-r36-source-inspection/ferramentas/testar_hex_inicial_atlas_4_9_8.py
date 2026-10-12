#!/usr/bin/env python3
from pathlib import Path
import json, math, zipfile
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(SRC/'MainActivity.java').read_text('utf-8')
appdb=(SRC/'AppDb.java').read_text('utf-8')
version=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text('utf-8').splitlines() if '=' in line)
package=ROOT/'android/app/src/main/assets/maps/faerun_maxres_integrated.zip'
checks={
 'versão 4.9.8+':int(version.get('VERSION_CODE','0'))>=490800,
 'validação manual usa geometria':'JSONObject hex=getMapHex(packageId,hexId.trim())' in appdb and 'center_x' in appdb and 'center_y' in appdb,
 'terreno pendente não bloqueia':'confidence>=0.60d' in appdb and 'return true;' in appdb[appdb.index('public boolean isAdventureStartHex'):appdb.index('public List<JSONObject> listAdventureStartHexes')],
 'água confiável continua rejeitada':'"water".equals(terrain)' in appdb and '"map_border".equals(terrain)' in appdb,
 'sorteio prioriza locais ou revisão':'EXISTS(SELECT 1 FROM map_locations' in appdb and 'EXISTS(SELECT 1 FROM map_hex_corrections' in appdb,
 'correções são mescladas':'mergeMapHex(item,getMapHexCorrection(packageId,hexId))' in appdb,
 'mensagem terrestre removida':'hexes terrestres validados' not in main and 'ponto inicial terrestre validado' not in main,
 'escolha direta catalogada':'isCatalogedAdventureStartHex' in main,
 'prompt distingue local ou terreno':'PONTO INICIAL CATALOGADO DESTA AVENTURA' in main and 'TERRENO CATALOGADO E CONFIRMADO' in main,
 'backup preservado':int(version.get('BACKUP_FORMAT','0'))>=17,
}
with zipfile.ZipFile(package) as z:
    manifest=json.load(z.open('map_manifest_faerun_maxres_v2.json'))
    atlas=json.load(z.open('faerun_5e_hex_atlas_v2.json'))['hexes']
checks['manifesto declara terreno não confirmado']=manifest['rulesSafety']['terrainStatus']=='unconfirmed'
checks['atlas real tem 59.334 pontos']=len(atlas)==59334
checks['ER83 existe geometricamente']='ER83' in atlas and all(math.isfinite(float(atlas['ER83']['center'][k])) for k in ('x','y'))
checks['política antiga retornaria zero']=sum(1 for h in atlas.values() if h['terrain']['primary'] not in (None,'','water','sea','ocean','coast_water','map_border','unassigned','null') and float(h['terrain']['confidence'] or 0)>=.60 and 'needs' not in h['review']['status'])==0
failed=[]
for name,ok in checks.items():
    print(('OK  ' if ok else 'FALHA  ')+name)
    if not ok: failed.append(name)
if failed: raise SystemExit(f'{len(failed)} falha(s): '+', '.join(failed))
print(f'RESULTADO 4.9.8: {len(checks)}/{len(checks)} verificações aprovadas.')
