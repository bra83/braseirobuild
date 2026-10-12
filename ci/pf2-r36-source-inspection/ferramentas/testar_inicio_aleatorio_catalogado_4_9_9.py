#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
main=(SRC/'MainActivity.java').read_text('utf-8')
appdb=(SRC/'AppDb.java').read_text('utf-8')
selector=(SRC/'AdventureStartSelector.java').read_text('utf-8')
version=dict(line.split('=',1) for line in (ROOT/'version.properties').read_text('utf-8').splitlines() if '=' in line)
checks={
 'versão 4.9.9 ou superior':int(version.get('VERSION_CODE','0'))>=490900,
 'backup preservado':int(version.get('BACKUP_FORMAT','0'))>=17,
 'hex geométrico sozinho não inicia':'isCatalogedAdventureStartHex' in main and 'não contém cidade, local nomeado ou terreno catalogado' in main,
 'tipos narrativos explícitos':all(x in appdb for x in ['"cidade"','"vila"','"porto"','"fortaleza"','"ruina"','"masmorra"','"templo"','"caverna"','"marco_magico"']),
 'terreno confiável continua candidato':'start_kind","terrain"' in appdb and 'terrain_confidence' in appdb,
 'sorteio ponderado existe':'AdventureStartSelector.choose' in main and 'start_weight' in selector and 'random.nextDouble()' in selector,
 'aleatoriedade não é sequência fixa':'System.nanoTime()' in selector and 'System.currentTimeMillis()' in selector and 'UUID.randomUUID()' in main,
 'último hex é evitado':'candidate.optString("hex_id").equalsIgnoreCase(mostRecentHex)' in selector,
 'histórico reduz repetição':'recent_adventure_start_points_' in main and 'region' in selector and 'type' in selector,
 'cidades têm peso maior':'return 140' in appdb and 'return 120' in appdb,
 'locais não somem entre milhares de terrenos':'locationSql' in appdb and 'terrainSql' in appdb and 'rawLocations' in appdb and 'rawTerrains' in appdb,
 'contexto recebe nome e região':'start_location_name' in main and 'start_location_region' in main and 'REFERÊNCIA DO CATÁLOGO' in main,
 'mestre é ancorado no local':'Use este local como âncora concreta da cena' in main and 'não comece em um espaço genérico' in main,
 'migração preserva posições':'migrateConfirmedLocationPlacementsToPackage' in appdb and 'normalized_same_map_migration' in appdb,
 'migração não bloqueia interface':'adventureStartCatalogMigrationRunning' in main and 'AppExecutors.io().execute' in main,
 'migração não inventa verbete':('location_id=? OR lower(name)=lower(?)' in appdb) or ('targetById' in appdb and 'targetByName' in appdb),
 'botão comunica variedade':'SORTEAR COM VARIEDADE' in main,
}
failed=[]
for name,ok in checks.items():
    print(('OK  ' if ok else 'FALHA  ')+name)
    if not ok: failed.append(name)
if failed: raise SystemExit(f'{len(failed)} falha(s): '+', '.join(failed))
print(f'RESULTADO 4.9.9: {len(checks)}/{len(checks)} verificações aprovadas.')
