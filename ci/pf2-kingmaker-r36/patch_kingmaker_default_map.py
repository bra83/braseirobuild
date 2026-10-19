#!/usr/bin/env python3
"""Patch MainActivity and MapPackageManager to establish Kingmaker Stolen Lands 8K as default map.

Megablock 1:
- CANONICAL_KINGMAKER_MAP_ID = "kingmaker_stolenlands_accurate_hd_8k"
- Bundle kingmaker_stolen_lands_canonical_8k.zip into assets
- Restov (AB2) starting hex
- Pointy-top explicit geometry
- Unverified terrain fallback
"""
from pathlib import Path
import shutil
import sys

def exact_replace(content, old, new):
    if old not in content:
        raise ValueError(f"Anchor not found: {old[:80]}")
    if content.count(old) != 1:
        raise ValueError(f"Ambiguous anchor (count {content.count(old)}): {old[:80]}")
    return content.replace(old, new, 1)

def patch(root_path):
    root = Path(root_path).resolve()
    main_java = root / "android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java"
    map_pack_java = root / "android/app/src/main/java/com/braseiro/pathfinder2e/MapPackageManager.java"
    assets_dir = root / "android/app/src/main/assets/pathfinder/atlas"
    assets_dir.mkdir(parents=True, exist_ok=True)

    # 1. Patch MainActivity.java
    main_code = main_java.read_text(encoding="utf-8")

    # Add CANONICAL_KINGMAKER_MAP_ID
    if "CANONICAL_KINGMAKER_MAP_ID" not in main_code:
        main_code = exact_replace(
            main_code,
            '    private static final String CANONICAL_INNER_SEA_MAP_ID="pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat";\n    private static final int CANONICAL_INNER_SEA_BUNDLE_REVISION=8;',
            '    public static final String CANONICAL_KINGMAKER_MAP_ID="kingmaker_stolenlands_accurate_hd_8k";\n    public static final int CANONICAL_KINGMAKER_BUNDLE_REVISION=1;\n    public static final String CANONICAL_INNER_SEA_MAP_ID="pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat";\n    public static final int CANONICAL_INNER_SEA_BUNDLE_REVISION=8;'
        )

    # In prepareKingmakerRestovStart, set hex_id to "AB2"
    if 'scene.put("hex_id","AB2");' not in main_code:
        main_code = exact_replace(
            main_code,
            '            scene.put("hex_id","");\n            scene.put("time_of_day",daypart);',
            '            scene.put("hex_id","AB2");\n            scene.put("time_of_day",daypart);'
        )

    # In ensureBundledMapForProfileAsync, default to Kingmaker Stolen Lands
    old_bundled = '''    private void ensureBundledMapForProfileAsync(String profile){
        if(db==null||mapPackageManager==null)return;
        db.setActiveMapProfile(WORLD_PF2);
        JSONObject state=db.getMapState();
        JSONObject current=db.getMapPackage(state.optString("map_package_id",""));
        int installedRevision=prefs==null?0:prefs.getInt("bundled_inner_sea_revision",0);
        boolean canonicalCurrent=current.optInt("ready",0)==1&&CANONICAL_INNER_SEA_MAP_ID.equals(current.optString("map_id"))&&installedRevision>=CANONICAL_INNER_SEA_BUNDLE_REVISION;
        if(canonicalCurrent)return;
        JSONObject canonicalReady=db.getLatestMapPackageByMapId(WORLD_PF2,CANONICAL_INNER_SEA_MAP_ID);
        // Revision 7 installs the rectified V6 high-resolution coordinate frame. Existing V5 rows
        // are rebuilt from the immutable bundled package while user/campaign state is migrated by hexId.
        if(installedRevision>=CANONICAL_INNER_SEA_BUNDLE_REVISION&&canonicalReady.optInt("ready",0)==1){
            db.activateMapPackage(canonicalReady.optString("package_id"));
            if(prefs!=null)prefs.edit().putInt("bundled_inner_sea_revision",CANONICAL_INNER_SEA_BUNDLE_REVISION).putBoolean("bundled_inner_sea_v6_ready",true).apply();
            return;
        }
        if(!bundledMapInstallRunning.compareAndSet(false,true))return;
        setStatus("Preparando o Atlas canônico do Mar Interior…");
        AppExecutors.io().execute(new Runnable(){public void run(){try{
            File dir=new File(getFilesDir(),"bundled_maps");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Não foi possível criar a pasta do Atlas.");
            File target=new File(dir,"pathfinder_inner_sea_canonical_v6_grid_native_hires_compat.zip");
            try(InputStream in=getAssets().open("pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip");FileOutputStream out=new FileOutputStream(target,false)){byte[] buffer=new byte[65536];int n;long total=0;while((n=in.read(buffer))>0){out.write(buffer,0,n);total+=n;}out.flush();if(total<1024L)throw new Exception("Pacote canônico do Atlas está vazio.");}
            mapPackageManager.importZip(Uri.fromFile(target),new MapPackageManager.Callback(){
                public void onProgress(final String stage,final int current,final int total){ui.post(new Runnable(){public void run(){setStatus("Atlas • "+stage);}});}
                public void onSuccess(final MapPackageManager.ImportResult result){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){
                    if(!CANONICAL_INNER_SEA_MAP_ID.equals(result.mapId)){setStatus("Atlas rejeitado: mapId canônico divergente.");return;}
                    prefs.edit().putInt("bundled_inner_sea_revision",CANONICAL_INNER_SEA_BUNDLE_REVISION).putBoolean("bundled_inner_sea_v6_ready",true).apply();
                    setStatus("Atlas canônico pronto • "+result.hexCount+" hexes • "+result.locationCount+" locais.");screenCache.remove("Mapa");screenCacheTime.remove("Mapa");if("Mapa".equals(currentScreen))showMap();
                }});}
                public void onError(final String error){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){setStatus("Atlas canônico não pôde ser preparado: "+error+". A campanha permanece jogável.");}});}
            });
        }catch(final Exception error){bundledMapInstallRunning.set(false);AppLog.e("BundledAtlas","Falha ao preparar Atlas canônico.",error);ui.post(new Runnable(){public void run(){setStatus("Atlas canônico temporariamente indisponível. A campanha permanece jogável.");}});}}});
    }'''

    new_bundled = '''    private void ensureBundledInnerSeaMapAsync(String profile){
        if(db==null||mapPackageManager==null)return;
        db.setActiveMapProfile(WORLD_PF2);
        JSONObject state=db.getMapState();
        JSONObject current=db.getMapPackage(state.optString("map_package_id",""));
        int installedRevision=prefs==null?0:prefs.getInt("bundled_inner_sea_revision",0);
        boolean canonicalCurrent=current.optInt("ready",0)==1&&CANONICAL_INNER_SEA_MAP_ID.equals(current.optString("map_id"))&&installedRevision>=CANONICAL_INNER_SEA_BUNDLE_REVISION;
        if(canonicalCurrent)return;
        JSONObject canonicalReady=db.getLatestMapPackageByMapId(WORLD_PF2,CANONICAL_INNER_SEA_MAP_ID);
        // Revision 7 installs the rectified V6 high-resolution coordinate frame. Existing V5 rows
        // are rebuilt from the immutable bundled package while user/campaign state is migrated by hexId.
        if(installedRevision>=CANONICAL_INNER_SEA_BUNDLE_REVISION&&canonicalReady.optInt("ready",0)==1){
            db.activateMapPackage(canonicalReady.optString("package_id"));
            if(prefs!=null)prefs.edit().putInt("bundled_inner_sea_revision",CANONICAL_INNER_SEA_BUNDLE_REVISION).putBoolean("bundled_inner_sea_v6_ready",true).apply();
            return;
        }
        if(!bundledMapInstallRunning.compareAndSet(false,true))return;
        setStatus("Preparando o Atlas canônico do Mar Interior…");
        AppExecutors.io().execute(new Runnable(){public void run(){try{
            File dir=new File(getFilesDir(),"bundled_maps");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Não foi possível criar a pasta do Atlas.");
            File target=new File(dir,"pathfinder_inner_sea_canonical_v6_grid_native_hires_compat.zip");
            try(InputStream in=getAssets().open("pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip");FileOutputStream out=new FileOutputStream(target,false)){byte[] buffer=new byte[65536];int n;long total=0;while((n=in.read(buffer))>0){out.write(buffer,0,n);total+=n;}out.flush();if(total<1024L)throw new Exception("Pacote canônico do Atlas está vazio.");}
            mapPackageManager.importZip(Uri.fromFile(target),new MapPackageManager.Callback(){
                public void onProgress(final String stage,final int current,final int total){ui.post(new Runnable(){public void run(){setStatus("Atlas • "+stage);}});}
                public void onSuccess(final MapPackageManager.ImportResult result){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){
                    if(!CANONICAL_INNER_SEA_MAP_ID.equals(result.mapId)){setStatus("Atlas rejeitado: mapId canônico divergente.");return;}
                    prefs.edit().putInt("bundled_inner_sea_revision",CANONICAL_INNER_SEA_BUNDLE_REVISION).putBoolean("bundled_inner_sea_v6_ready",true).apply();
                    setStatus("Atlas canônico pronto • "+result.hexCount+" hexes • "+result.locationCount+" locais.");screenCache.remove("Mapa");screenCacheTime.remove("Mapa");if("Mapa".equals(currentScreen))showMap();
                }});}
                public void onError(final String error){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){setStatus("Atlas canônico não pôde ser preparado: "+error+". A campanha permanece jogável.");}});}
            });
        }catch(final Exception error){bundledMapInstallRunning.set(false);AppLog.e("BundledAtlas","Falha ao preparar Atlas canônico.",error);ui.post(new Runnable(){public void run(){setStatus("Atlas canônico temporariamente indisponível. A campanha permanece jogável.");}});}}});
    }

    private void ensureBundledMapForProfileAsync(String profile){
        ensureBundledKingmakerMapAsync(profile);
    }

    public void ensureBundledKingmakerMapAsync(String profile){
        if(db==null||mapPackageManager==null)return;
        db.setActiveMapProfile(WORLD_PF2);
        JSONObject state=db.getMapState();
        JSONObject current=db.getMapPackage(state.optString("map_package_id",""));
        int installedRevision=prefs==null?0:prefs.getInt("bundled_kingmaker_map_revision",0);
        boolean kingmakerCurrent=current.optInt("ready",0)==1&&CANONICAL_KINGMAKER_MAP_ID.equals(current.optString("map_id"))&&installedRevision>=CANONICAL_KINGMAKER_BUNDLE_REVISION;
        if(kingmakerCurrent)return;
        JSONObject kingmakerReady=db.getLatestMapPackageByMapId(WORLD_PF2,CANONICAL_KINGMAKER_MAP_ID);
        if(installedRevision>=CANONICAL_KINGMAKER_BUNDLE_REVISION&&kingmakerReady.optInt("ready",0)==1){
            db.activateMapPackage(kingmakerReady.optString("package_id"));
            if(prefs!=null)prefs.edit().putInt("bundled_kingmaker_map_revision",CANONICAL_KINGMAKER_BUNDLE_REVISION).putBoolean("bundled_kingmaker_ready",true).apply();
            return;
        }
        if(!bundledMapInstallRunning.compareAndSet(false,true))return;
        setStatus("Preparando o Atlas das Terras Roubadas (Kingmaker 8K)…");
        AppExecutors.io().execute(new Runnable(){public void run(){try{
            File dir=new File(getFilesDir(),"bundled_maps");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Não foi possível criar a pasta do Atlas.");
            File target=new File(dir,"kingmaker_stolen_lands_canonical_8k.zip");
            try(InputStream in=getAssets().open("pathfinder/atlas/kingmaker_stolen_lands_canonical_8k.zip");FileOutputStream out=new FileOutputStream(target,false)){byte[] buffer=new byte[65536];int n;long total=0;while((n=in.read(buffer))>0){out.write(buffer,0,n);total+=n;}out.flush();if(total<1024L)throw new Exception("Pacote Kingmaker das Terras Roubadas está vazio.");}
            mapPackageManager.importZip(Uri.fromFile(target),new MapPackageManager.Callback(){
                public void onProgress(final String stage,final int current,final int total){ui.post(new Runnable(){public void run(){setStatus("Atlas Kingmaker • "+stage);}});}
                public void onSuccess(final MapPackageManager.ImportResult result){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){
                    if(!CANONICAL_KINGMAKER_MAP_ID.equals(result.mapId)){setStatus("Atlas rejeitado: mapId Kingmaker divergente.");return;}
                    prefs.edit().putInt("bundled_kingmaker_map_revision",CANONICAL_KINGMAKER_BUNDLE_REVISION).putBoolean("bundled_kingmaker_ready",true).apply();
                    setStatus("Atlas das Terras Roubadas pronto • "+result.hexCount+" hexes • "+result.locationCount+" locais.");screenCache.remove("Mapa");screenCacheTime.remove("Mapa");if("Mapa".equals(currentScreen))showMap();
                }});}
                public void onError(final String error){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){setStatus("Atlas Kingmaker não pôde ser preparado: "+error+". A campanha permanece jogável.");}});}
            });
        }catch(final Exception error){bundledMapInstallRunning.set(false);AppLog.e("BundledAtlas","Falha ao preparar Atlas Kingmaker.",error);ui.post(new Runnable(){public void run(){setStatus("Atlas Kingmaker temporariamente indisponível. A campanha permanece jogável.");}});}}});
    }'''

    if "bundled_kingmaker_map_revision" not in main_code:
        main_code = exact_replace(main_code, old_bundled, new_bundled)

    main_java.write_text(main_code, encoding="utf-8")
    print("PATCHED_MAIN_ACTIVITY_KINGMAKER_DEFAULT_MAP")

    # 2. Patch MapPackageManager.java for Restov AB2 initial token placement
    map_code = map_pack_java.read_text(encoding="utf-8")
    old_init = 'MapManifestGeometry.ManifestHex initial = geometry.nearest(geometry.getImageWidth() / 2f, geometry.getImageHeight() / 2f);'
    new_init = 'MapManifestGeometry.ManifestHex initial = geometry.byId("AB2");\n                    if (initial == null) initial = geometry.nearest(geometry.getImageWidth() / 2f, geometry.getImageHeight() / 2f);'
    if old_init in map_code:
        map_code = exact_replace(map_code, old_init, new_init)
        map_pack_java.write_text(map_code, encoding="utf-8")
        print("PATCHED_MAP_PACKAGE_MANAGER_RESTOV_INITIAL_HEX")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python patch_kingmaker_default_map.py <PROJECT_DIR>")
        sys.exit(1)
    patch(sys.argv[1])
