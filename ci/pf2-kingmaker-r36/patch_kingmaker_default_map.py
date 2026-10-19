#!/usr/bin/env python3
"""Patch MainActivity, MapPackageManager, and MapBoardView for Kingmaker Stolen Lands 8K default map.

Megablock 1 and 2:
- CANONICAL_KINGMAKER_MAP_ID = "kingmaker_stolenlands_accurate_hd_8k"
- CANONICAL_KINGMAKER_BUNDLE_REVISION = 3
- Bundle kingmaker_stolen_lands_canonical_8k.zip into assets
- Restov (AB2) starting hex
- Pointy-top explicit geometry
- Vector dynamic overlays for 8 geographic regions and 6 political realms
- Unified LOCAIS / POIs dialog with 33 canonical Kingmaker locations and map centering
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
    map_board_java = root / "android/app/src/main/java/com/braseiro/pathfinder2e/MapBoardView.java"
    assets_dir = root / "android/app/src/main/assets/pathfinder/atlas"
    assets_dir.mkdir(parents=True, exist_ok=True)

    # 0. Asset bundle synchronization
    target_zip = assets_dir / "kingmaker_stolen_lands_canonical_8k.zip"
    source_pack = Path(r"C:\Users\bcruz\Downloads\stolen_lands_pack\KINGMAKER_R36_STOLEN_LANDS_IMPORTAR_VTT.zip")
    if source_pack.is_file() and (not target_zip.exists() or target_zip.stat().st_size != source_pack.stat().st_size):
        shutil.copy2(source_pack, target_zip)
        print("SYNCED_BUNDLED_KINGMAKER_MAP_ZIP")

    # 1. Patch MainActivity.java
    main_code = main_java.read_text(encoding="utf-8")

    # Add CANONICAL_KINGMAKER_MAP_ID
    if "CANONICAL_KINGMAKER_MAP_ID" not in main_code:
        main_code = exact_replace(
            main_code,
            '    private static final String CANONICAL_INNER_SEA_MAP_ID="pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat";\n    private static final int CANONICAL_INNER_SEA_BUNDLE_REVISION=8;',
            '    public static final String CANONICAL_KINGMAKER_MAP_ID="kingmaker_stolenlands_accurate_hd_8k";\n    public static final int CANONICAL_KINGMAKER_BUNDLE_REVISION=3;\n    public static final String CANONICAL_INNER_SEA_MAP_ID="pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat";\n    public static final int CANONICAL_INNER_SEA_BUNDLE_REVISION=8;'
        )
    elif "CANONICAL_KINGMAKER_BUNDLE_REVISION=2;" in main_code:
        main_code = exact_replace(
            main_code,
            "CANONICAL_KINGMAKER_BUNDLE_REVISION=2;",
            "CANONICAL_KINGMAKER_BUNDLE_REVISION=3;"
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
                    db.activateMapPackage(result.packageId);
                    prefs.edit().putString("map_world_profile",WORLD_PF2).putInt("bundled_kingmaker_map_revision",CANONICAL_KINGMAKER_BUNDLE_REVISION).putBoolean("bundled_kingmaker_ready",true).apply();
                    setStatus("Atlas das Terras Roubadas pronto • "+result.hexCount+" hexes • "+result.locationCount+" locais.");screenCache.remove("Mapa");screenCacheTime.remove("Mapa");if("Mapa".equals(currentScreen))showMap();
                }});}
                public void onError(final String error){bundledMapInstallRunning.set(false);ui.post(new Runnable(){public void run(){setStatus("Atlas Kingmaker não pôde ser preparado: "+error+". A campanha permanece jogável.");}});}
            });
        }catch(final Exception error){bundledMapInstallRunning.set(false);AppLog.e("BundledAtlas","Falha ao preparar Atlas Kingmaker.",error);ui.post(new Runnable(){public void run(){setStatus("Atlas Kingmaker temporariamente indisponível. A campanha permanece jogável.");}});}}});
    }'''

    if "bundled_kingmaker_map_revision" not in main_code:
        main_code = exact_replace(main_code, old_bundled, new_bundled)

    # Patch showMap uninstalled card
    old_showmap_card = 'if(initial.optString("map_package_id","").trim().length()==0||initial.optString("image_uri","").trim().length()==0){LinearLayout noAtlas=card();noAtlas.setBackground(makeBg(Color.rgb(40,24,29),GOLD,2));noAtlas.addView(text("ATLAS PATHFINDER • INSTALANDO O MAPA INCLUÍDO",14,GOLD,true));noAtlas.addView(text("O pacote V6 em alta resolução já está dentro do aplicativo. A primeira instalação valida imagem, 3.300 hexes, locais e geometria antes de exibir o mapa.",12,TEXT,false));Button retryBundled=button("INSTALAR / TENTAR NOVAMENTE",GREEN);retryBundled.setOnClickListener(new View.OnClickListener(){public void onClick(View v){prefs.edit().putInt("bundled_inner_sea_revision",0).apply();ensureBundledMapForProfileAsync(WORLD_PF2);setStatus("Reiniciando a instalação do Atlas incluído…");}});noAtlas.addView(retryBundled);root.addView(noAtlas);}'
    new_showmap_card = '''if(initial.optString("map_package_id","").trim().length()==0||initial.optString("image_uri","").trim().length()==0){
            JSONObject kmReady=db.getLatestMapPackageByMapId(WORLD_PF2,CANONICAL_KINGMAKER_MAP_ID);
            if(kmReady.optInt("ready",0)==1){
                db.activateMapPackage(kmReady.optString("package_id"));
                prefs.edit().putString("map_world_profile",WORLD_PF2).putBoolean("bundled_kingmaker_ready",true).apply();
                showMap();
                return;
            }
            ensureBundledMapForProfileAsync(WORLD_PF2);
            LinearLayout noAtlas=card();noAtlas.setBackground(makeBg(Color.rgb(40,24,29),GOLD,2));
            noAtlas.addView(text("ATLAS DAS TERRAS ROUBADAS (KINGMAKER 8K) • CARREGANDO",14,GOLD,true));
            noAtlas.addView(text("O mapa 8K das Terras Roubadas já está incluído no aplicativo com todos os 354 hexágonos. Toque abaixo para carregar imediatamente o mapa oficial.",12,TEXT,false));
            Button retryBundled=button("CARREGAR ATLAS KINGMAKER 8K • INSTALAR / TENTAR NOVAMENTE",GREEN);
            retryBundled.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
                prefs.edit().putInt("bundled_kingmaker_map_revision",0).apply();
                ensureBundledMapForProfileAsync(WORLD_PF2);
                setStatus("Carregando o Atlas de Kingmaker…");
            }});
            noAtlas.addView(retryBundled);
            root.addView(noAtlas);
        }'''
    if old_showmap_card in main_code:
        main_code = exact_replace(main_code, old_showmap_card, new_showmap_card)

    # Patch toolbar button to LOCAIS / POIs
    if 'Button managePois=smallButton("LOCAIS DA CAMPANHA",GREEN);' in main_code:
        main_code = exact_replace(
            main_code,
            'Button managePois=smallButton("LOCAIS DA CAMPANHA",GREEN);',
            'Button managePois=smallButton("LOCAIS / POIs",GREEN);'
        )

    # Patch showCampaignPoisDialog to include canonical locations
    old_pois_dialog = 'private void showCampaignPoisDialog(){\n        if(campaignPoiService==null)return;final List<JSONObject> rows=(mapView!=null&&mapView.isMasterView())?campaignPoiService.listAll():campaignPoiService.visibleForAtlas(db.getMapState().optString("logical_map_id",""));\n        if(rows.isEmpty()){new AlertDialog.Builder(this).setTitle("Locais da campanha").setMessage("Ainda não há POIs dinâmicos nesta campanha. O Atlas oficial continua intacto.").setNegativeButton("Fechar",null).show();return;}\n        String[] labels=new String[rows.size()];for(int i=0;i<rows.size();i++){JSONObject p=rows.get(i);labels[i]=p.optString("icon","•")+" "+p.optString("name","Local")+" • "+p.optString("hex_id","?")+" • "+CampaignPoiService.displayType(p.optString("type"))+((p.optInt("hidden",0)==1||p.optInt("discovered",1)==0)?" • SECRETO":"");}\n        new AlertDialog.Builder(this).setTitle("Locais da campanha").setItems(labels,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){showCampaignPoiDetails(rows.get(which));}}).setNegativeButton("Fechar",null).show();\n    }\n\n    '
    new_pois_dialog = 'private void showCampaignPoisDialog(){\n        JSONObject state=db.getMapState();\n        String packageId=state.optString("map_package_id","");\n        final List<JSONObject> campaignRows=campaignPoiService!=null?((mapView!=null&&mapView.isMasterView())?campaignPoiService.listAll():campaignPoiService.visibleForAtlas(state.optString("logical_map_id",""))):new ArrayList<JSONObject>();\n        final List<JSONObject> canonicalRows=packageId.length()>0?db.listMapLocationsWithHexReference(packageId,500):new ArrayList<JSONObject>();\n        final List<JSONObject> allItems=new ArrayList<JSONObject>();\n        JSONObject addOption=new JSONObject();\n        try{addOption.put("_is_add",true);addOption.put("name","➕ ADICIONAR NOVO LOCAL DA CAMPANHA");}catch(Exception ignored){}\n        allItems.add(addOption);\n        for(JSONObject c:campaignRows){try{JSONObject copy=new JSONObject(c.toString());copy.put("_is_campaign",true);allItems.add(copy);}catch(Exception ignored){}}\n        for(JSONObject k:canonicalRows){try{JSONObject copy=new JSONObject(k.toString());copy.put("_is_canonical",true);allItems.add(copy);}catch(Exception ignored){}}\n        String[] labels=new String[allItems.size()];\n        for(int i=0;i<allItems.size();i++){\n            JSONObject item=allItems.get(i);\n            if(item.optBoolean("_is_add",false)){\n                labels[i]=item.optString("name");\n            }else if(item.optBoolean("_is_campaign",false)){\n                labels[i]=item.optString("icon","•")+" "+item.optString("name","Local")+" • "+item.optString("hex_id","?")+" [CAMPANHA]";\n            }else{\n                String region=item.optString("region","");\n                labels[i]="📍 "+item.optString("name","Local")+" • "+item.optString("hex_id","?")+(region.length()>0?" ("+region+")":"")+" [CANÔNICO]";\n            }\n        }\n        new AlertDialog.Builder(this).setTitle("Locais e Pontos de Interesse (POIs)").setItems(labels,new DialogInterface.OnClickListener(){\n            public void onClick(DialogInterface d,int which){\n                JSONObject selected=allItems.get(which);\n                if(selected.optBoolean("_is_add",false)){\n                    editCampaignPoiDialog(null,mapView!=null?mapView.getCurrentHexId():"AB2");\n                }else if(selected.optBoolean("_is_campaign",false)){\n                    showCampaignPoiDetails(selected);\n                }else{\n                    focusMapLocation(selected);\n                }\n            }\n        }).setNegativeButton("Fechar",null).show();\n    }\n\n    '
    if old_pois_dialog in main_code:
        main_code = exact_replace(main_code, old_pois_dialog, new_pois_dialog)

    main_java.write_text(main_code, encoding="utf-8")
    print("PATCHED_MAIN_ACTIVITY_KINGMAKER_DEFAULT_MAP")

    # 2. Patch MapPackageManager.java for Restov AB2 initial token placement & database sample validation
    map_code = map_pack_java.read_text(encoding="utf-8")
    old_init = 'MapManifestGeometry.ManifestHex initial = geometry.nearest(geometry.getImageWidth() / 2f, geometry.getImageHeight() / 2f);'
    new_init = 'MapManifestGeometry.ManifestHex initial = geometry.byId("AB2");\n                    if (initial == null) initial = geometry.nearest(geometry.getImageWidth() / 2f, geometry.getImageHeight() / 2f);'
    if old_init in map_code:
        map_code = exact_replace(map_code, old_init, new_init)
        print("PATCHED_MAP_PACKAGE_MANAGER_RESTOV_INITIAL_HEX")

    old_sample = '''        if(geometry.byId("0.00")!=null)ids.add("0.00");
        if(geometry.byId("11.09")!=null)ids.add("11.09");'''
    new_sample = '''        if(geometry.getMapId().contains("inner-sea")){
            if(geometry.byId("0.00")!=null)ids.add("0.00");
            if(geometry.byId("11.09")!=null)ids.add("11.09");
        } else {
            if(geometry.byId("AB2")!=null)ids.add("AB2");
            if(geometry.byId("U3")!=null)ids.add("U3");
        }'''
    if old_sample in map_code:
        map_code = exact_replace(map_code, old_sample, new_sample)
        print("PATCHED_MAP_PACKAGE_MANAGER_VALIDATE_SAMPLES")

    map_pack_java.write_text(map_code, encoding="utf-8")

    # 3. Patch MapBoardView.java for dynamic vector regions, politics, and canonical POIs
    if map_board_java.is_file():
        mbv_code = map_board_java.read_text(encoding="utf-8")
        old_draw_flow = '''        if (fog && !masterView) drawFog(canvas);
        drawTravelPlan(canvas);'''
        new_draw_flow = '''        if (fog && !masterView) drawFog(canvas);
        drawDynamicRegions(canvas);
        if (politicalOverlayVisible) drawDynamicPolitical(canvas);
        drawTravelPlan(canvas);'''
        if old_draw_flow in mbv_code:
            mbv_code = exact_replace(mbv_code, old_draw_flow, new_draw_flow)
            print("PATCHED_MAP_BOARD_VIEW_ONDRAW_FLOW")

        old_mbv_block = 'private void drawCanonicalPois(Canvas canvas) {\n        if(geometry==null||canonicalPoisByHex.isEmpty())return;\n        float viewScale=Math.max(.0001f,getSourceZoom());\n        for(Map.Entry<String,ArrayList<JSONObject>> entry:canonicalPoisByHex.entrySet()){\n            MapManifestGeometry.ManifestHex hex=geometry.byId(entry.getKey());if(hex==null||(!masterView&&fog&&!isHexRevealed(hex.id)))continue;\n            ArrayList<JSONObject> rows=entry.getValue();if(rows==null||rows.isEmpty())continue;\n            JSONObject primary=rows.get(0);\n            String placement=primary.optString("placement_status","").toLowerCase(Locale.US);\n            boolean approximate=placement.contains("approx")||placement.contains("review")||placement.contains("unassigned");\n            float radius=Math.max(4.3f/viewScale,Math.min(hex.width,hex.height)*.085f);\n            float x=hex.centerX+hex.width*.23f,y=hex.centerY+hex.height*.19f;\n            canonicalPoiPaint.setStyle(approximate?Paint.Style.STROKE:Paint.Style.FILL);\n            canonicalPoiPaint.setStrokeWidth(Math.max(.8f,1.7f/viewScale));\n            canonicalPoiPaint.setColor(approximate?Color.rgb(238,220,166):Color.rgb(236,190,76));\n            canvas.drawCircle(x,y,radius,canonicalPoiPaint);\n            canonicalPoiOutlinePaint.setColor(Color.argb(235,45,27,15));\n            canonicalPoiOutlinePaint.setStrokeWidth(Math.max(.7f,1.5f/viewScale));\n            canvas.drawCircle(x,y,radius,canonicalPoiOutlinePaint);\n            canonicalPoiPaint.setStyle(Paint.Style.FILL);\n            canonicalPoiPaint.setColor(approximate?Color.rgb(238,220,166):Color.WHITE);\n            canonicalPoiPaint.setTextSize(radius*1.25f);\n            Paint.FontMetrics fm=canonicalPoiPaint.getFontMetrics();\n            canvas.drawText(approximate?"○":"•",x,y-(fm.ascent+fm.descent)/2f,canonicalPoiPaint);\n            if(getSourceZoom()>=1.05f){\n                String name=primary.optString("name","").trim();\n                if(name.length()>0){\n                    float textSize=Math.max(8f/viewScale,Math.min(hex.height*.23f,13f/viewScale));\n                    canonicalPoiOutlinePaint.setTextSize(textSize);canonicalPoiOutlinePaint.setStrokeWidth(Math.max(.7f,2.0f/viewScale));canonicalPoiOutlinePaint.setColor(Color.argb(230,28,18,12));\n                    canonicalPoiPaint.setTextSize(textSize);canonicalPoiPaint.setColor(Color.rgb(255,241,200));canonicalPoiPaint.setTextAlign(Paint.Align.LEFT);canonicalPoiOutlinePaint.setTextAlign(Paint.Align.LEFT);\n                    float tx=x+radius*1.35f,ty=y-radius*.25f;\n                    canvas.drawText(name,tx,ty,canonicalPoiOutlinePaint);canvas.drawText(name,tx,ty,canonicalPoiPaint);\n                    canonicalPoiPaint.setTextAlign(Paint.Align.CENTER);canonicalPoiOutlinePaint.setTextAlign(Paint.Align.CENTER);\n                }\n            }\n        }\n    }\n\n    '
        new_mbv_block = 'private void drawCanonicalPois(Canvas canvas) {\n        if(geometry==null||canonicalPoisByHex.isEmpty())return;\n        float viewScale=Math.max(.0001f,getSourceZoom());\n        for(Map.Entry<String,ArrayList<JSONObject>> entry:canonicalPoisByHex.entrySet()){\n            String hid=entry.getKey();\n            MapManifestGeometry.ManifestHex hex=geometry.byId(hid);\n            if(hex==null||(!masterView&&fog&&!isHexRevealed(hex.id)))continue;\n            boolean isCivilizedStart="AB2".equals(hid)||"U3".equals(hid)||"Y3".equals(hid)||"AA3".equals(hid);\n            ArrayList<JSONObject> rows=entry.getValue();if(rows==null||rows.isEmpty())continue;\n            JSONObject primary=rows.get(0);\n            String placement=primary.optString("placement_status","").toLowerCase(Locale.US);\n            boolean approximate=placement.contains("approx")||placement.contains("review")||placement.contains("unassigned");\n            float radius=Math.max(6.5f/viewScale,Math.min(hex.width,hex.height)*.11f);\n            float x=hex.centerX,y=hex.centerY;\n            canonicalPoiPaint.setStyle(Paint.Style.FILL);\n            canonicalPoiPaint.setColor(isCivilizedStart?Color.rgb(255,215,0):(approximate?Color.rgb(238,205,120):Color.rgb(245,175,55)));\n            canvas.drawCircle(x,y,radius,canonicalPoiPaint);\n            canonicalPoiOutlinePaint.setColor(Color.argb(240,40,25,15));\n            canonicalPoiOutlinePaint.setStrokeWidth(Math.max(1.2f,2.2f/viewScale));\n            canvas.drawCircle(x,y,radius,canonicalPoiOutlinePaint);\n            canonicalPoiPaint.setStyle(Paint.Style.FILL);\n            canonicalPoiPaint.setColor(Color.argb(240,40,25,15));\n            canonicalPoiPaint.setTextSize(radius*1.1f);\n            Paint.FontMetrics fm=canonicalPoiPaint.getFontMetrics();\n            canvas.drawText("★",x,y-(fm.ascent+fm.descent)/2f,canonicalPoiPaint);\n            if(viewScale>=0.18f){ // if(getSourceZoom()>=1.05f)\n                String name=primary.optString("name","").trim();\n                if(name.length()>0){\n                    float textSize=Math.max(9.5f/viewScale,Math.min(hex.height*.26f,14.5f/viewScale));\n                    canonicalPoiOutlinePaint.setTextSize(textSize);canonicalPoiOutlinePaint.setStrokeWidth(Math.max(1.2f,2.8f/viewScale));canonicalPoiOutlinePaint.setColor(Color.argb(240,20,15,10));\n                    canonicalPoiPaint.setTextSize(textSize);canonicalPoiPaint.setColor(isCivilizedStart?Color.rgb(255,235,140):Color.rgb(255,245,215));canonicalPoiPaint.setTextAlign(Paint.Align.CENTER);canonicalPoiOutlinePaint.setTextAlign(Paint.Align.CENTER);\n                    float tx=x,ty=y+radius+textSize*1.05f;\n                    canvas.drawText(name,tx,ty,canonicalPoiOutlinePaint);canvas.drawText(name,tx,ty,canonicalPoiPaint);\n                }\n            }\n        }\n    }\n\n    private static final class DynRegionDef {\n        final String name; final int color; final float labelX, labelY; final String[] hexes;\n        DynRegionDef(String n, int c, float lx, float ly, String[] h){name=n;color=c;labelX=lx;labelY=ly;hexes=h;}\n    }\n\n    private void drawDynamicRegions(Canvas canvas) {\n        if(geometry==null)return;\n        float viewScale=Math.max(.0001f,getSourceZoom());\n        boolean pointy="pointy-top".equals(geometry.getOrientation());\n        DynRegionDef[] defs=new DynRegionDef[]{\n            new DynRegionDef("🌿 CINTURÃO VERDE (GREENBELT)",Color.argb(45,46,139,87),5500f,650f,new String[]{"U2","V2","W2","T3","U3","V3","W3","S4","T4","U4","V4","W4","S5","T5","U5","V5","W5"}),\n            new DynRegionDef("🌲 FLORESTA NARLMARCHES",Color.argb(50,34,110,45),4720f,1600f,new String[]{"P5","Q5","R5","P6","Q6","R6","S6","P7","Q7","R7","S7","P8","Q8","R8","S8","T8","P9","Q9","R9","S9","T9","P10","Q10","R10","S10"}),\n            new DynRegionDef("🌾 KAMELANDS",Color.argb(45,205,133,63),5500f,2155f,new String[]{"U7","V7","W7","X7","U8","V8","W8","X8","U9","V9","W9","X9","U10","V10","W10","X10","U11","V11","W11","X11"}),\n            new DynRegionDef("🐎 DUNSWARD",Color.argb(45,218,165,32),7610f,880f,new String[]{"Z4","AA4","AB4","AC4","AD4","Z5","AA5","AB5","AC5","AD5","Z6","AA6","AB6","AC6","AD6","Z7","AA7","AB7","AC7","AD7","Z8","AA8","AB8","AC8","AD8"}),\n            new DynRegionDef("🌫️ PÂNTANO HOOKTONGUE",Color.argb(50,70,130,120),3420f,2115f,new String[]{"K8","L8","M8","N8","O8","K9","L9","M9","N9","O9","K10","L10","M10","N10","O10","K11","L11","M11","N11","O11","K12","L12","M12","N12","O12"}),\n            new DynRegionDef("⛰️ ALTIPLANOS DE GLENEBON",Color.argb(45,112,128,144),2065f,575f,new String[]{"E2","F2","G2","H2","I2","J2","E3","F3","G3","H3","I3","J3","E4","F4","G4","H4","I4","J4","E5","F5","G5","H5","I5","J5","E6","F6","G6","H6","I6","J6"}),\n            new DynRegionDef("✨ THOUSAND VOICES",Color.argb(50,106,90,205),410f,1900f,new String[]{"A7","B7","C7","D7","A8","B8","C8","D8","A9","B9","C9","D9","A10","B10","C10","D10","A11","B11","C11","D11","A12","B12","C12","D12"}),\n            new DynRegionDef("🏔️ TORS OF LEVENIES",Color.argb(50,139,69,19),7400f,2660f,new String[]{"Y11","Z11","AA11","AB11","AC11","AD11","Y12","Z12","AA12","AB12","AC12","AD12","Y13","Z13","AA13","AB13","AC13","AD13"})\n        };\n        Paint rPaint=new Paint(Paint.ANTI_ALIAS_FLAG);\n        Paint bPaint=new Paint(Paint.ANTI_ALIAS_FLAG);bPaint.setStyle(Paint.Style.STROKE);\n        Paint tPaint=new Paint(Paint.ANTI_ALIAS_FLAG);tPaint.setTextAlign(Paint.Align.CENTER);\n        Paint oPaint=new Paint(Paint.ANTI_ALIAS_FLAG);oPaint.setTextAlign(Paint.Align.CENTER);oPaint.setStyle(Paint.Style.STROKE);\n        for(DynRegionDef r:defs){\n            rPaint.setStyle(Paint.Style.FILL);rPaint.setColor(r.color);\n            bPaint.setColor(Color.argb(140,Color.red(r.color),Color.green(r.color),Color.blue(r.color)));\n            bPaint.setStrokeWidth(Math.max(1.5f,2.8f/viewScale));\n            for(String hid:r.hexes){\n                MapManifestGeometry.ManifestHex h=geometry.byId(hid);\n                if(h!=null){\n                    drawHex(canvas,h.centerX,h.centerY,h.width*.98f,h.height*.98f,pointy,rPaint);\n                    drawHex(canvas,h.centerX,h.centerY,h.width*.98f,h.height*.98f,pointy,bPaint);\n                }\n            }\n            float bannerTextSize=Math.max(18f/viewScale,28f);\n            tPaint.setTextSize(bannerTextSize);tPaint.setTypeface(Typeface.DEFAULT_BOLD);tPaint.setColor(Color.rgb(255,238,175));\n            oPaint.setTextSize(bannerTextSize);oPaint.setTypeface(Typeface.DEFAULT_BOLD);oPaint.setStrokeWidth(Math.max(2.8f,6.5f/viewScale));oPaint.setColor(Color.argb(235,20,15,10));\n            canvas.drawText(r.name,r.labelX,r.labelY,oPaint);canvas.drawText(r.name,r.labelX,r.labelY,tPaint);\n        }\n    }\n\n    private static final class DynFactionDef {\n        final String name, ruler; final int color; final float labelX, labelY; final String[] hexes;\n        DynFactionDef(String n, String ru, int c, float lx, float ly, String[] h){name=n;ruler=ru;color=c;labelX=lx;labelY=ly;hexes=h;}\n    }\n\n    private void drawDynamicPolitical(Canvas canvas) {\n        if(geometry==null)return;\n        float viewScale=Math.max(.0001f,getSourceZoom());\n        boolean pointy="pointy-top".equals(geometry.getOrientation());\n        DynFactionDef[] factions=new DynFactionDef[]{\n            new DynFactionDef("ROSTLAND / BREVOY","Sede de Restov • Jamandi Aldori",Color.argb(60,30,144,255),7400f,320f,new String[]{"Y2","Z2","AA2","AB2","AC2","AD2","Y3","Z3","AA3","AB3","AC3","AD3"}),\n            new DynFactionDef("BARONATO DAS TERRAS ROUBADAS","Reino dos Heróis • Posto de Oleg / Tuskwater",Color.argb(65,46,139,87),5400f,1300f,new String[]{"T3","U3","V3","T4","U4","V4","T5","U5","V5","T6","U6","V6","U7","V7","U8","V8"}),\n            new DynFactionDef("REINO DE PITAX","Rei Castruccio Irovetti",Color.argb(60,148,0,211),1694f,2200f,new String[]{"D9","E9","F9","G9","E10","F10","G10","E11","F11","G11","H11","F12","G12","H12"}),\n            new DynFactionDef("BARONATO DE FORT DRELEV","Barão Hannis Drelev",Color.argb(60,178,34,34),3300f,1500f,new String[]{"M7","N7","O7","L8","M8","N8","O8","L9","M9","N9"}),\n            new DynFactionDef("COLÔNIA DE VARNHOLD","Capitão Maegar Varn (Varnling Host)",Color.argb(60,210,105,30),7100f,1400f,new String[]{"Y5","Z5","AA5","Y6","Z6","AA6","Y7","Z7","AA7"}),\n            new DynFactionDef("SENHORES TIGRES (TIGER LORDS)","Armag Twice-Born • Tribos Bárbaras",Color.argb(55,160,82,45),2300f,850f,new String[]{"F3","G3","H3","I3","F4","G4","H4","I4","F5","G5","H5","I5"})\n        };\n        Paint fPaint=new Paint(Paint.ANTI_ALIAS_FLAG);\n        Paint sPaint=new Paint(Paint.ANTI_ALIAS_FLAG);sPaint.setStyle(Paint.Style.STROKE);\n        Paint lPaint=new Paint(Paint.ANTI_ALIAS_FLAG);lPaint.setTextAlign(Paint.Align.CENTER);\n        Paint oPaint=new Paint(Paint.ANTI_ALIAS_FLAG);oPaint.setTextAlign(Paint.Align.CENTER);oPaint.setStyle(Paint.Style.STROKE);\n        for(DynFactionDef f:factions){\n            fPaint.setStyle(Paint.Style.FILL);fPaint.setColor(f.color);\n            sPaint.setColor(Color.argb(190,Color.red(f.color),Color.green(f.color),Color.blue(f.color)));\n            sPaint.setStrokeWidth(Math.max(2.0f,4.0f/viewScale));\n            for(String hid:f.hexes){\n                MapManifestGeometry.ManifestHex h=geometry.byId(hid);\n                if(h!=null){\n                    drawHex(canvas,h.centerX,h.centerY,h.width*.97f,h.height*.97f,pointy,fPaint);\n                    drawHex(canvas,h.centerX,h.centerY,h.width*.97f,h.height*.97f,pointy,sPaint);\n                }\n            }\n            float titleSize=Math.max(17f/viewScale,27f);\n            float subSize=Math.max(12f/viewScale,19f);\n            lPaint.setTextSize(titleSize);lPaint.setTypeface(Typeface.DEFAULT_BOLD);lPaint.setColor(Color.rgb(255,230,140));\n            oPaint.setTextSize(titleSize);oPaint.setTypeface(Typeface.DEFAULT_BOLD);oPaint.setStrokeWidth(Math.max(2.8f,6.5f/viewScale));oPaint.setColor(Color.argb(240,15,10,5));\n            canvas.drawText("👑 "+f.name,f.labelX,f.labelY,oPaint);canvas.drawText("👑 "+f.name,f.labelX,f.labelY,lPaint);\n            lPaint.setTextSize(subSize);lPaint.setTypeface(Typeface.DEFAULT);lPaint.setColor(Color.rgb(240,240,240));\n            oPaint.setTextSize(subSize);oPaint.setTypeface(Typeface.DEFAULT);oPaint.setStrokeWidth(Math.max(2.0f,4.5f/viewScale));\n            canvas.drawText(f.ruler,f.labelX,f.labelY+subSize*1.3f,oPaint);canvas.drawText(f.ruler,f.labelX,f.labelY+subSize*1.3f,lPaint);\n        }\n    }\n\n    '
        if old_mbv_block in mbv_code:
            mbv_code = exact_replace(mbv_code, old_mbv_block, new_mbv_block)
            print("PATCHED_MAP_BOARD_VIEW_DYNAMIC_OVERLAYS")

        map_board_java.write_text(mbv_code, encoding="utf-8")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python patch_kingmaker_default_map.py <PROJECT_DIR>")
        sys.exit(1)
    patch(sys.argv[1])
