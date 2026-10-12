from pathlib import Path
from zipfile import ZipFile
import json

ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/'android/app'
SRC=APP/'src/main/java/com/braseiro/pathfinder2e'
BUNDLE=APP/'src/main/assets/pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip'
MAP_ID='pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat'
LOGICAL_ID='pathfinder:golarion:inner_sea'

def test_v6_bundle_contract_and_geometry():
    assert BUNDLE.is_file()
    with ZipFile(BUNDLE) as z:
        assert z.testzip() is None
        names=set(z.namelist())
        for n in ['package.json','map.png','hex_ids_overlay.png','map_regions_ptbr_overlay.png','nation_labels_ptbr_overlay.png','map_manifest.json','map_manifest_pathfinder2e_inner_sea_4270x5388.json','atlas_map_profile_v1.json','pathfinder2e_inner_sea_hex_atlas_v1.json','pathfinder2e_inner_sea_locations_v1.json']:
            assert n in names
        pkg=json.loads(z.read('package.json'))
        manifest=json.loads(z.read('map_manifest.json'))
        profile=json.loads(z.read('atlas_map_profile_v1.json'))
        atlas=json.loads(z.read('pathfinder2e_inner_sea_hex_atlas_v1.json'))
        locations=json.loads(z.read('pathfinder2e_inner_sea_locations_v1.json'))
        assert pkg['mapId']==MAP_ID
        assert pkg['dataRevision']=='V6-GRID-NATIVE-HIRES'
        assert manifest['image']=={'width':4270,'height':5388,'expectedFilename':'map.png'}
        layout=manifest['layout']
        assert layout['orientation']=='flat-top'
        assert layout['originX']==25.0 and layout['originY']==48.0
        assert abs(layout['adjacentColumnCenterDx']-77.7)<1e-8
        assert abs(layout['sameColumnCenterDy']-89.72023183206784)<1e-8
        assert abs(layout['alternateColumnOffsetY']-44.86011591603392)<1e-8
        assert profile['logical_map_id']==LOGICAL_ID
        assert profile['coordinate_frame_id']=='pf2e:golarion:inner_sea:shieldwulf_hexmap_rectified_v1'
        assert profile['hexScaleMiles']==40
        assert len(atlas['hexes'])==3300
        assert len(locations['locations'])==67
        assert all(x.get('hexId') for x in locations['locations'])

def test_v6_host_import_preserves_identity_and_campaign_state():
    main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
    db=(SRC/'AppDb.java').read_text(encoding='utf-8')
    mgr=(SRC/'MapPackageManager.java').read_text(encoding='utf-8')
    geom=(SRC/'MapManifestGeometry.java').read_text(encoding='utf-8')
    assert MAP_ID in main
    assert 'inner_sea_canonical_v6_grid_native_hires_compat.zip' in main
    assert 'CANONICAL_INNER_SEA_BUNDLE_REVISION=8' in main
    assert 'getLogicalMapId()' in geom and 'coordinateFrameId' in geom
    assert LOGICAL_ID in mgr and 'sameLogicalMapFamily' in mgr
    assert 'coordinateFrameChanged' in mgr
    assert 'expectedWidth=v6?4270f:2135f' in mgr
    assert 'expectedHeight=v6?5388f:2720f' in mgr
    assert 'shieldwulf_hexmap_rectified_v1' in mgr
    assert 'UPDATE campaign_pois SET map_id=? WHERE map_id=?' in db
    assert 'logicalMapId(row.optString("manifest_json")' in db

def test_v6_uses_manifest_geometry_without_legacy_v5_calibration():
    geom=(SRC/'MapManifestGeometry.java').read_text(encoding='utf-8')
    assert 'pathfinder2e-inner-sea-2135x2720-canonical-v5-max-compat' in geom  # legacy import only
    assert 'pathfinder2e-inner-sea-4270x5388' not in geom[geom.index('private void applyRuntimeRasterCalibration'):geom.index('private static void parseExplicit')]
    assert 'layout.optDouble("originX"' in geom
    assert 'layout.optDouble("adjacentColumnCenterDx"' in geom
    assert 'layout.optDouble("sameColumnCenterDy"' in geom

def test_v6_runtime_validator_accepts_the_bundled_hires_frame():
    mgr=(SRC/'MapPackageManager.java').read_text(encoding='utf-8')
    main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
    assert 'v6?4270f:2135f' in mgr and 'v6?5388f:2720f' in mgr
    assert 'Math.abs(geometry.getOriginX()-25f)' in mgr
    assert 'Math.abs(geometry.getStepX()-77.7f)' in mgr
    assert 'INSTALAR / TENTAR NOVAMENTE' in main

def test_fog_is_real_and_pois_respect_player_fog():
    main=(SRC/'MainActivity.java').read_text(encoding='utf-8')
    view=(SRC/'MapBoardView.java').read_text(encoding='utf-8')
    assert 'NÉVOA ON' in main and 'NÉVOA OFF' in main
    assert 'mapView.setFog(true)' in main
    assert 'if (fog && !masterView) drawFog(canvas);' in view
    assert 'PorterDuff.Mode.CLEAR' in view
    assert view.count('!masterView&&fog&&!isHexRevealed(hex.id)')>=2

def test_canonical_labels_are_screen_readable_on_hires_map():
    view=(SRC/'MapBoardView.java').read_text(encoding='utf-8')
    assert 'drawCanonicalPois(canvas)' in view
    assert 'if(getSourceZoom()>=1.05f)' in view
    assert 'canvas.drawText(name,tx,ty,canonicalPoiOutlinePaint)' in view
