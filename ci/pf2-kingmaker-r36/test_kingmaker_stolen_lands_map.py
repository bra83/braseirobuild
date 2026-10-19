import json
import zipfile
from pathlib import Path
import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]
HANDOFF_PACK = Path(r"C:\Users\bcruz\Downloads\stolen_lands_pack")
MAP_ZIP = HANDOFF_PACK / "KINGMAKER_R36_STOLEN_LANDS_IMPORTAR_VTT.zip"

def test_stolen_lands_manifest_geometry_354_hexes():
    with zipfile.ZipFile(MAP_ZIP) as z:
        manifest = json.loads(z.read("map_manifest_kingmaker_8k.json").decode("utf-8"))
    
    assert manifest["sourceMap"]["width"] == 8192
    assert manifest["sourceMap"]["height"] == 2894
    map_entry = manifest["maps"][0]
    assert map_entry["mapId"] == "kingmaker_stolenlands_accurate_hd_8k"
    assert map_entry["identity"]["logical_map_id"] == "pathfinder:kingmaker:stolen_lands:accurate_hd_8k"
    assert map_entry["identity"]["coordinate_frame_id"] == "kingmaker_stolenlands_accurate_hd_8192x2894_v1"
    assert map_entry["scale"]["milesPerHex"] == 12

    layout = map_entry["layout"]
    assert layout["orientation"] == "pointy-top"
    assert layout["columns"] == 31
    assert layout["rows"] == 13
    assert len(layout["hexes"]) == 354

def test_canonical_pois_restov_and_olegs():
    with zipfile.ZipFile(MAP_ZIP) as z:
        manifest = json.loads(z.read("map_manifest_kingmaker_8k.json").decode("utf-8"))
        locations = json.loads(z.read("kingmaker_locations_8k.json").decode("utf-8"))
    
    hex_map = {h["hexId"]: h for h in manifest["maps"][0]["layout"]["hexes"]}
    
    # Restov at AB2
    assert "AB2" in hex_map
    ab2 = hex_map["AB2"]
    assert ab2["column"] == 27 and ab2["row"] == 1
    assert abs(ab2["xCenter"] - 7594.4503) < 0.01
    assert abs(ab2["yCenter"] - 173.7542) < 0.01

    # Oleg's Trading Post at U3
    assert "U3" in hex_map
    u3 = hex_map["U3"]
    assert u3["column"] == 20 and u3["row"] == 2
    assert abs(u3["xCenter"] - 5497.6909) < 0.01
    assert abs(u3["yCenter"] - 415.9234) < 0.01

    loc_map = {loc["id"]: loc for loc in locations["locations"]}
    assert "restov" in loc_map
    assert loc_map["restov"]["hexId"] == "AB2"
    assert loc_map["restov"]["placement"]["status"] == "needs_review"
    assert "oleg_trading_post" in loc_map
    assert loc_map["oleg_trading_post"]["hexId"] == "U3"
    assert loc_map["oleg_trading_post"]["placement"]["status"] == "needs_review"

def test_canonical_pois_all_33_stolen_lands_locations():
    with zipfile.ZipFile(MAP_ZIP) as z:
        manifest = json.loads(z.read("map_manifest_kingmaker_8k.json").decode("utf-8"))
        locations = json.loads(z.read("kingmaker_locations_8k.json").decode("utf-8"))
    
    hex_map = {h["hexId"]: h for h in manifest["maps"][0]["layout"]["hexes"]}
    locs = locations["locations"]
    assert len(locs) == 33, f"Expected 33 Kingmaker locations, got {len(locs)}"

    # Key adventure sites must exist with valid hex assignments
    expected_sites = {
        "restov": "AB2",
        "oleg_trading_post": "U3",
        "fort_serenko": "Y3",
        "nivaktas_crossing": "AA3",
        "varnhold": "Z6",
        "pitax": "G11",
        "littletown": "E9",
        "bokken_hut": "U4",
        "fangberry_thicket": "T4",
        "nettles_crossing": "V5",
        "thorn_river_camp": "T5",
        "old_sycamore": "V6",
        "sootscale_caverns": "U6",
        "temple_of_elk": "R5",
        "tatzlwyrm_den": "W5",
        "tuskgutter_lair": "Q7",
        "stag_lord_fort": "U8",
        "candlemere_island": "T8",
        "lonely_barrow": "X7",
        "hargulka_stronghold": "R7",
        "tatzlford": "R8",
        "vordakais_tomb": "AA7",
        "fort_drelev": "M8",
    }
    loc_by_id = {loc["id"]: loc for loc in locs}
    for site_id, exp_hex in expected_sites.items():
        assert site_id in loc_by_id, f"Missing site {site_id}"
        actual_hex = loc_by_id[site_id].get("hexId")
        assert actual_hex == exp_hex, f"Site {site_id} expected {exp_hex}, got {actual_hex}"
        assert actual_hex in hex_map, f"Hex {actual_hex} for site {site_id} not in layout"

    for loc in locs:
        assert loc.get("hexId") is not None, f"Location {loc['id']} has null hexId"
        assert loc["hexId"] in hex_map, f"Location {loc['id']} hex {loc['hexId']} not in layout"

def test_neighbor_symmetry_pointy_top():
    with zipfile.ZipFile(MAP_ZIP) as z:
        manifest = json.loads(z.read("map_manifest_kingmaker_8k.json").decode("utf-8"))
    
    hex_map = {h["hexId"]: h for h in manifest["maps"][0]["layout"]["hexes"]}
    assert len(hex_map) == 354
    
    # In pointy-top geometry:
    # Staggered rows have horizontal half-step: 279.5679 / 2 = 139.7840 px
    # Vertical step between rows: 242.1692 px
    dx_stagger = 279.5679 / 2.0
    dy_row = 242.1692
    
    ab2 = hex_map["AB2"]
    ab3 = hex_map.get("AB3")
    if ab3:
        assert abs(ab3["yCenter"] - ab2["yCenter"] - dy_row) < 0.01
        assert abs(abs(ab3["xCenter"] - ab2["xCenter"]) - dx_stagger) < 0.01

def test_unverified_terrain_safe_fallback():
    with zipfile.ZipFile(MAP_ZIP) as z:
        atlas = json.loads(z.read("kingmaker_hex_atlas_8k.json").decode("utf-8"))
    
    hexes = atlas["hexes"]
    assert len(hexes) == 354
    
    # All 354 must be explicitly marked needs_review and unverified
    for hex_id, h in hexes.items():
        assert h["review"]["status"] == "needs_review", f"Hex {hex_id} not needs_review"
        assert h["terrain"]["classificationStatus"] == "needs_human_review", f"Hex {hex_id} classification not needs_human_review"
        assert h["travel"]["traversalCostVerified"] is False, f"Hex {hex_id} traversal cost must be False"

def test_production_project_kingmaker_default_map(tmp_path):
    project_root = Path(r"C:\Users\bcruz\Downloads\KINGMAKER_R36_WORK\r36-build")
    main_java = (project_root / "android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java").read_text(encoding="utf-8")
    map_pack_java = (project_root / "android/app/src/main/java/com/braseiro/pathfinder2e/MapPackageManager.java").read_text(encoding="utf-8")
    map_board_java = (project_root / "android/app/src/main/java/com/braseiro/pathfinder2e/MapBoardView.java").read_text(encoding="utf-8")
    bundled_zip = project_root / "android/app/src/main/assets/pathfinder/atlas/kingmaker_stolen_lands_canonical_8k.zip"

    assert bundled_zip.is_file(), "Bundled map zip missing from assets"
    assert bundled_zip.stat().st_size > 19_000_000, "Bundled map zip too small"

    assert 'CANONICAL_KINGMAKER_MAP_ID="kingmaker_stolenlands_accurate_hd_8k"' in main_java
    assert 'CANONICAL_KINGMAKER_BUNDLE_REVISION=3;' in main_java
    assert 'bundled_kingmaker_map_revision' in main_java
    assert 'scene.put("hex_id","AB2");' in main_java
    assert 'LOCAIS / POIs' in main_java
    assert 'showCampaignPoisDialog' in main_java
    assert 'listMapLocationsWithHexReference' in main_java

    assert 'MapManifestGeometry.ManifestHex initial = geometry.byId("AB2");' in map_pack_java

    # Vector dynamic overlays for Regions and Politics
    assert 'drawDynamicRegions' in map_board_java
    assert 'drawDynamicPolitical' in map_board_java
    assert 'CINTURÃO VERDE' in map_board_java
    assert 'FLORESTA NARLMARCHES' in map_board_java
    assert 'BARONATO DAS TERRAS ROUBADAS' in map_board_java
    assert 'REINO DE PITAX' in map_board_java
    assert 'SENHORES TIGRES' in map_board_java
