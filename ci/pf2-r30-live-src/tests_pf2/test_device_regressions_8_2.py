from pathlib import Path
from zipfile import ZipFile
import json, re

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'

def read(name): return (SRC/name).read_text(encoding='utf-8')

def test_backup_contract_matches_export_format_22():
    backup=read('BackupContractValidator.java')
    db=read('AppDb.java')
    props=(ROOT/'version.properties').read_text(encoding='utf-8')
    assert 'MAX_SUPPORTED_VERSION=22' in backup
    assert '.put("version",22)' in db
    assert 'backupVersion>22' in db
    assert 'BACKUP_FORMAT=22' in props

def test_v6_atlas_is_hires_rectified_and_manifest_driven():
    mm=read('MapPackageManager.java')
    for stale in ('429f','450.333f','45.033320996790806f'):
        assert stale not in mm
    for token in ('getOriginX()','getOriginY()','getStepX()','getStepY()','getAlternateColumnOffsetY()'):
        assert token in mm
    bundle=ROOT/'android/app/src/main/assets/pathfinder/atlas/inner_sea_canonical_v6_grid_native_hires_compat.zip'
    with ZipFile(bundle) as z:
        assert z.testzip() is None
        package=json.loads(z.read('package.json'))
        manifest=json.loads(z.read('map_manifest.json'))
        atlas=json.loads(z.read('pathfinder2e_inner_sea_hex_atlas_v1.json'))
        locations=json.loads(z.read('pathfinder2e_inner_sea_locations_v1.json'))
    assert package['mapId']=='pathfinder2e-inner-sea-4270x5388-canonical-v6-grid-native-hires-compat'
    assert package['dataRevision']=='V6-GRID-NATIVE-HIRES'
    assert manifest['image']['width']==4270 and manifest['image']['height']==5388
    layout=manifest['layout']
    x=layout['originX']+11*layout['adjacentColumnCenterDx']
    y=layout['originY']+9*layout['sameColumnCenterDy']+layout['alternateColumnOffsetY']
    assert abs(x-879.7)<0.001 and abs(y-900.3422024446)<0.001
    assert len(atlas['hexes'])==3300
    assert len(locations['locations'])==67
    assert all(v.get('hexId') for v in locations['locations'])

def test_dice_runtime_is_native_gurps_renderer_and_pf2_resolver():
    overlay=read('Dice3DOverlay.java')
    physics=read('PhysicalDiceEngine.java')
    view=read('Dice3DView.java')
    assert 'android.webkit.WebView' not in overlay
    assert 'dice3d_core.js' not in overlay
    for token in ('PhysicalDiceEngine.create','PhysicalDiceEngine.FIXED_DT','new Dice3DView','spec.resolve(rolls)','APLICAR RESULTADO'):
        assert token in overlay
    assert 'FIXED_DT=1.0/120.0' in physics
    assert 'rawTopValue()' in physics and 'collideDice' in physics
    assert 'drawFace' in view and 'drawLabel' in view and 'drawShadow' in view
    for material in ('plastic','stone','metal','bone','wood','crystal','glass','obsidian','ivory','leather'):
        assert (ROOT/f'android/app/src/main/res/raw/dice_{material}.wav').stat().st_size>0

def test_candle_light_is_global_and_touch_through():
    main=read('MainActivity.java')
    candle=read('CandleLightOverlayView.java')
    assert 'private CandleLightOverlayView candleOverlay;' in main
    assert 'candleOverlay=new CandleLightOverlayView(this)' in main
    assert 'shellHost.addView(candleOverlay' in main
    assert 'candle_light_enabled' in main and 'candle_light_intensity' in main
    for hook in ('onMasterNarrationStarted','onNarrationChunk','onMasterNarrationEnded'):
        assert hook in main and hook in candle
    for token in ('RadialGradient','Math.sin','postInvalidateDelayed(66)','onTouchEvent(MotionEvent e){return false;}'):
        assert token in candle

def test_diagnostic_dialogs_are_opaque_and_dimmed():
    main=read('MainActivity.java')
    assert 'private AlertDialog showReadableAlert' in main
    assert 'new android.graphics.drawable.ColorDrawable(PANEL)' in main
    assert 'lp.dimAmount=.86f' in main
    assert main.count('showReadableAlert(')>=4
