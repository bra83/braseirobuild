#!/usr/bin/env python3
import json, re, sys
from pathlib import Path

root=Path(sys.argv[1]).resolve();vtt=root/'app/src/main/assets/vtt';r54=vtt/'image-library/local/r54'
items=json.loads((r54/'ITEM_VISUAL_BINDING_AUDIT.json').read_text('utf-8'))
locations=json.loads((r54/'LOCATION_MAP_AUDIT.json').read_text('utf-8'))
runtime=(vtt/'r54-device-p7.js').read_text('utf-8');bridge=(vtt/'js/core/uiBridge.bundle.js').read_text('utf-8');final=(vtt/'r46/final-runtime.js').read_text('utf-8')
by_id={re.sub(r'[^a-z0-9]+',' ',x['id'].lower()).strip():x for x in items}
critical=['blaster pistol','heavy blaster pistol','blaster rifle','blaster carbine','disruptor pistol','disruptor rifle','frag grenade','stun grenade','thermal detonator','combat knife']
for item_id in critical:
    row=by_id.get(item_id)
    assert row and row['asset_exists'] and not row['is_generic_fallback'],(item_id,row)
    assert (vtt/row['current_asset']).is_file(),row
for place in ['Tatooine','Corellia','Kashyyyk','Naboo']:
    row=next((x for x in locations if x['location_name'].lower()==place.lower()),None);assert row,place
    assert row['show_map_section']==row['local_map_exists']==row['local_map_loads'],row
    if row['local_map_exists']:assert (vtt/row['local_map_asset']).is_file(),row
assert 'R54_DEVICE_P7_SEMANTIC_SCENE' in bridge
assert 'visibleParticipantNames: localNpcs.map' in bridge
assert '.filter(item => atLocation(item) && visible(item))' in bridge
assert "suggestions.push(`Conversar com ${knownNpc.name}`)" in bridge
assert "braseiro.starwars.edge.scene.v1" in final and "sessionStorage.clear()" in final
assert 'Mapa local indisponível para este local.' in runtime
assert 'itemsExactByName' in runtime and 'pickVisual' not in runtime
report={'status':'CODE_PASS_DEVICE_PENDING','critical_item_bindings':'PASS','local_map_policy':'PASS','npc_scene_scope':'PASS','action_scene_scope':'PASS','reset_state':'PASS','physical_device':'NOT_RUN'}
print(json.dumps(report,ensure_ascii=False,indent=2))
