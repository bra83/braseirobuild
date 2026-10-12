from pathlib import Path
import hashlib, importlib.util, json, sys

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"android/app/src/main/java/com/braseiro/pathfinder2e"
PY=ROOT/"android/app/src/main/python"

def text(name): return (SRC/name).read_text(encoding="utf-8")

def test_motor_barbara_112_exact_manifest_and_supervisor():
    manifest=json.loads((PY/"BARBARA_EMBEDDED.json").read_text(encoding="utf-8"))
    assert manifest["motor_version"]=="1.1.2"
    assert manifest["module_count"]==60
    assert len(manifest["modules"])==60
    assert {x["file"] for x in manifest["modules"]} >= {"poi.py","presentation.py","android.py"}
    for row in manifest["modules"]:
        p=PY/"barbara"/row["file"]
        assert hashlib.sha256(p.read_bytes()).hexdigest()==row["sha256"]
    sys.path.insert(0,str(PY))
    try:
        import braseiro_barbara_adapter as adapter
        health=json.loads(adapter.health_json())
        assert health["barbara_version"]=="1.1.2"
        assert health["mechanics_authority"]=="Pathfinder2eRulesEngine"
        assert health["second_database"] is False
        assert health["duplicate_gemini_call"] is False
        assert health["supervisor_mode"]=="external_host"
        contract=health["supervisor_contract"]
        assert contract["protocol_version"]==2
        assert contract["authority"]["rules"]=="host"
        assert contract["authority"]["atlas"]=="host"
    finally:
        sys.path.pop(0)

def test_canonical_map_asset_is_v6_runtime_bundle():
    p=ROOT/"android/app/src/main/assets/pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip"
    assert p.is_file() and p.stat().st_size>40_000_000
    assert not (ROOT/"android/app/src/main/assets/pathfinder/atlas/inner_sea_canonical_v5_max_compat.zip").exists()

def test_baked_grid_is_not_drawn_twice_and_runtime_calibration_is_shared():
    main=text("MainActivity.java")
    geom=text("MapManifestGeometry.java")
    pack=text("MapPackageManager.java")
    assert "private boolean mapUsesBakedHexGrid(String manifestJson){return false;}" not in main
    assert 'CANONICAL_INNER_SEA_MAP_ID.equals(id)' in main
    assert "mapView.setVectorGridVisible(bakedGrid?false" in main
    assert 'V6 GRID NATIVE HIRES' in geom
    legacy=geom[geom.index('private void applyRuntimeRasterCalibration'):geom.index('private static void parseExplicit')]
    assert '4270x5388' not in legacy
    assert "runtimeHex=geometry==null?null:geometry.byId(row.hexId)" in pack
    assert 'values.put("center_x", runtimeHex==null?' in pack
    assert 'values.put("center_y", runtimeHex==null?' in pack

def test_existing_map_install_is_reimported_for_v6_coordinate_frame():
    main=text("MainActivity.java")
    assert "CANONICAL_INNER_SEA_BUNDLE_REVISION=8" in main
    shortcut=main[main.index("JSONObject canonicalReady="):main.index("if(!bundledMapInstallRunning",main.index("JSONObject canonicalReady="))]
    assert "installedRevision>=CANONICAL_INNER_SEA_BUNDLE_REVISION" in shortcut

def test_catalog_references_are_truthful_and_visible():
    db=text("AppDb.java"); main=text("MainActivity.java"); view=text("MapBoardView.java"); diag=text("SystemDiagnostics5.java")
    assert "countMapReferencedLocations" in db
    assert "countMapApproximateReferencedLocations" in db
    assert "listMapLocationsWithHexReference" in db
    assert "com referência de hex" in main
    assert "aproximados" in main
    assert "setCanonicalPois" in view and "drawCanonicalPois(canvas)" in view
    assert "refreshCanonicalPoiMarkers" in main
    assert "Pontos catalogados com referência cartográfica" in diag
    assert "countMapReferencedLocations(pkg)>0" in diag

def test_approximate_location_is_not_promoted_to_exact():
    main=text("MainActivity.java")
    assert "Posição cartográfica aproximada do catálogo canônico; não é uma coordenada oficial exata." in main
    assert "Hex confirmado no pacote cartográfico." in main

def test_false_guild_poi_regression_removed():
    poi=text("CampaignPoiService.java")
    persistent_line=next(x for x in poi.splitlines() if "PERSISTENT_PLACE" in x)
    assert "|guilda|" not in persistent_line
    assert 'displayType(type)+" de "+hex' not in poi[poi.index("public JSONObject proposeFromNarration"):poi.index("/**",poi.index("public JSONObject proposeFromNarration"))]
    assert "boolean locative=" in poi
    assert "extractDescriptivePlaceName" in poi
    bad="Onde deveria repousar o pesado carimbo de bronze da guilda — a ferramenta que autentica cada salvo-conduto e contrato da região — resta apenas uma reentrância retangular no feltro verde gasto, marcada pelo pó de tinta seca."
    place_words=["taverna","estalagem","aldeia","acampamento","templo","santuário","caverna","gruta","mina","forte","fortim","torre","loja","casa","esconderijo","dungeon","masmorra","fazenda","posto comercial","posto de guarda","mosteiro","ruína","ponte","mercado","quartel","ferreiro","sede"]
    assert not any(w in bad.lower() for w in place_words)

def test_positive_descriptive_poi_regression_kept():
    poi=text("CampaignPoiService.java")
    assert "no cruzamento" in poi and "na estrada" in poi
    assert 'String descriptive=base+" "+tail;' in poi

def test_pf2_authority_and_channels_preserved():
    adapter=(PY/"braseiro_barbara_adapter.py").read_text(encoding="utf-8")
    main=text("MainActivity.java")
    assert '"mechanics_authority": "Pathfinder2eRulesEngine"' in adapter
    assert '"second_database": False' in adapter
    assert '"duplicate_gemini_call": False' in adapter
    assert '"PLAYER_ACTION"' in adapter and '"GM_HELP"' in adapter
    assert 'sendAi(chosenMode,q,"PLAYER_ACTION")' in main
    assert 'sendAi("Narrador",question,"GM_HELP")' in main
