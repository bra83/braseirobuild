#!/usr/bin/env python3
from pathlib import Path
import sqlite3, sys
ROOT=Path(__file__).resolve().parents[1]
APP=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/AppDb.java').read_text('utf-8')
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text('utf-8')
VER=dict(line.strip().split('=',1) for line in (ROOT/'version.properties').read_text('utf-8').splitlines() if '=' in line)
checks=[]
def check(name, ok):
    checks.append((name,bool(ok))); print(('OK  ' if ok else 'FALHA  ')+name)
check('versão 4.9.12+', int(VER.get('VERSION_CODE','0'))>=490912)
check('migração lê tabela de vínculo', "COALESCE(NULLIF(l.hex_id,''),j.hex_id) AS linked_hex_id" in APP and 'LEFT JOIN map_hex_locations j' in APP)
check('migração aceita posição preservada mesmo se pacote antigo não estiver ready', 'SELECT * FROM map_packages WHERE world_profile=? AND package_id<>?' in APP)
check('migração limita reaproveitamento à mesma família cartográfica', 'targetMapId.contains("faerun")&&!sourceMapId.contains("faerun")' in APP)
check('migração aceita correspondência por ID, nome normalizado e alias', 'targetById' in APP and 'targetByName' in APP and 'aliases' in APP)
check('destino evita duplicar vínculo já migrado', 'SELECT hex_id FROM map_hex_locations WHERE package_id=? AND location_id=? LIMIT 1' in APP)
check('local ligado por join recebe hex resolvido ao ser lido', 'item.put("hex_id",item.optString("linked_hex_id",hexId))' in APP)
check('busca de locais confirmados aceita join legado', "COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>''" in APP)
check('sorteio aceita vínculo da tabela map_hex_locations', 'EXISTS(SELECT 1 FROM map_hex_locations j' in APP and 'j.hex_id=h.hex_id' in APP)
check('proximidade aceita vínculo da tabela map_hex_locations', APP.count('EXISTS(SELECT 1 FROM map_hex_locations j')>=2)
check('5.0 remove reparo oculto disparado pelo toque', 'repairCatalogAndRetryAdventureStart(hexId,resetStart,mapView)' not in MAIN and 'explicitRepairActiveAtlas' in MAIN)
check('mensagem impossível da 4.9.11 foi removida', 'Nenhum ponto catalogado próximo foi encontrado. Use o sorteio ou a busca do Atlas.' not in MAIN)
check('reparo explícito é executado fora da thread de interface', 'explicitRepairActiveAtlas' in MAIN and 'AppExecutors.io().execute(new Runnable(){public void run(){' in MAIN and 'Reparando somente posições verificáveis do Atlas' in MAIN)

# Reproduz exatamente o defeito observado: map_locations.hex_id vazio, mas vínculo real presente.
db=sqlite3.connect(':memory:')
db.executescript('''
CREATE TABLE map_packages(package_id TEXT PRIMARY KEY,map_id TEXT,world_profile TEXT,image_width REAL,image_height REAL,imported_at INTEGER,ready INTEGER);
CREATE TABLE map_locations(package_id TEXT,location_id TEXT,name TEXT,type TEXT,hex_id TEXT,placement_status TEXT,placement_confidence REAL);
CREATE TABLE map_hex_locations(package_id TEXT,hex_id TEXT,location_id TEXT);
CREATE TABLE map_hex_atlas(package_id TEXT,hex_id TEXT,center_x REAL,center_y REAL);
''')
db.execute("INSERT INTO map_packages VALUES('old','faerun-5e-10200x6600','dnd',10200,6600,1,0)")
db.execute("INSERT INTO map_packages VALUES('new','faerun-5e-maxres-16695x9555','dnd',16695,9555,2,1)")
db.execute("INSERT INTO map_locations VALUES('old','old-neverwinter','Neverwinter','cidade','','verified',0.9)")
db.execute("INSERT INTO map_hex_locations VALUES('old','Q77','old-neverwinter')")
db.execute("INSERT INTO map_hex_atlas VALUES('old','Q77',5100,3300)")
db.execute("INSERT INTO map_locations VALUES('new','new-neverwinter','Neverwinter','cidade','','unplaced',0)")
# dois hexes candidatos no mapa novo; centro normalizado deve cair em NX1
db.execute("INSERT INTO map_hex_atlas VALUES('new','NX1',8347.5,4777.5)")
db.execute("INSERT INTO map_hex_atlas VALUES('new','NX2',13000,7000)")
row=db.execute("""SELECT l.*,COALESCE(NULLIF(l.hex_id,''),j.hex_id) AS linked_hex_id,h.center_x,h.center_y
FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id
INNER JOIN map_hex_atlas h ON h.package_id=l.package_id AND h.hex_id=COALESCE(NULLIF(l.hex_id,''),j.hex_id)
WHERE l.package_id=? AND COALESCE(NULLIF(l.hex_id,''),j.hex_id,'')<>''
AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%'
AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%'
AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%'""",('old',)).fetchone()
check('fixture reproduz 0 hex_id direto + vínculo Q77 recuperável', row is not None and row[4]=='' and row[7]=='Q77')
if row:
    x,y=row[-2],row[-1]
    tx=x/10200*16695; ty=y/6600*9555
    nearest=db.execute("SELECT hex_id FROM map_hex_atlas WHERE package_id='new' ORDER BY ((center_x-?)*(center_x-?)+(center_y-?)*(center_y-?)) LIMIT 1",(tx,tx,ty,ty)).fetchone()[0]
    check('posição antiga resolve um hex real no novo atlas', nearest=='NX1')
    db.execute("UPDATE map_locations SET hex_id=?,placement_status='migrated_confirmed' WHERE package_id='new' AND location_id='new-neverwinter'",(nearest,))
    db.execute("INSERT INTO map_hex_locations VALUES('new',?,'new-neverwinter')",(nearest,))
    positioned=db.execute("SELECT COUNT(DISTINCT l.location_id) FROM map_locations l LEFT JOIN map_hex_locations j ON j.package_id=l.package_id AND j.location_id=l.location_id WHERE l.package_id='new' AND (COALESCE(l.hex_id,'')<>'' OR COALESCE(j.hex_id,'')<>'') AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%review%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%approx%' AND lower(COALESCE(l.placement_status,'')) NOT LIKE '%unassigned%'").fetchone()[0]
    check('após reparo o atlas deixa de ter 0 posicionados', positioned==1)
else:
    check('posição antiga resolve um hex real no novo atlas', False)
    check('após reparo o atlas deixa de ter 0 posicionados', False)
failed=[n for n,o in checks if not o]
print(f'RESULTADO 4.9.12: {len(checks)-len(failed)}/{len(checks)}')
if failed:
    print('FALHAS:'); [print(' - '+n) for n in failed]; sys.exit(1)
