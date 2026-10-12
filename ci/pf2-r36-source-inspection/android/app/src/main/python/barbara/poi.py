from __future__ import annotations

from copy import deepcopy
import re


class DynamicPoiContract:
    LAYERS = frozenset({'CANONICAL','CAMPAIGN','PROCEDURAL'})
    STATUSES = frozenset({'active','abandoned','ruined','destroyed','occupied','contested','rebuilding','growing','migration_pending'})
    ORIGINS = frozenset({'canonical','player_created','campaign_discovery','gm_created','world_generated','imported'})

    def validate(self, poi, *, allow_canonical=False):
        if not isinstance(poi, dict):
            raise ValueError('invalid_poi')
        required = {'id','name','layer','logical_map_id'}
        missing = required - set(poi)
        if missing:
            raise ValueError('missing_poi_fields:' + ','.join(sorted(missing)))
        for key in ('id','name','logical_map_id'):
            if not isinstance(poi.get(key), str) or not poi[key].strip() or len(poi[key]) > 240:
                raise ValueError('invalid_poi_' + key)
        if not re.fullmatch(r'[A-Za-z0-9_.:\-]{1,240}', poi['id']):
            raise ValueError('invalid_poi_id')
        layer = str(poi.get('layer')).upper()
        if layer not in self.LAYERS:
            raise ValueError('invalid_poi_layer')
        if layer == 'CANONICAL' and not allow_canonical:
            raise ValueError('canonical_poi_read_only')
        status = poi.get('status','active')
        if status not in self.STATUSES:
            raise ValueError('invalid_poi_status')
        origin = poi.get('origin', 'canonical' if layer == 'CANONICAL' else 'campaign_discovery')
        if origin not in self.ORIGINS:
            raise ValueError('invalid_poi_origin')
        for key in ('hidden','discovered','canonical'):
            if key in poi and not isinstance(poi[key], bool):
                raise ValueError('invalid_poi_' + key)
        if layer == 'CANONICAL' and poi.get('canonical') is False:
            raise ValueError('canonical_poi_flag_mismatch')
        if layer != 'CANONICAL' and poi.get('canonical') is True:
            raise ValueError('dynamic_poi_cannot_be_canonical')
        locator_count = 0
        if poi.get('hex_id') not in (None,''):
            if not isinstance(poi['hex_id'], str): raise ValueError('invalid_poi_hex_id')
            locator_count += 1
        pos = poi.get('canonical_position')
        if pos is not None:
            if not isinstance(pos, dict): raise ValueError('invalid_poi_canonical_position')
            if not isinstance(pos.get('coordinate_frame_id'), str) or not pos['coordinate_frame_id']:
                raise ValueError('invalid_poi_coordinate_frame')
            if not all(isinstance(pos.get(k),(int,float)) and not isinstance(pos.get(k),bool) and 0 <= float(pos[k]) <= 1 for k in ('u','v')):
                raise ValueError('invalid_poi_normalized_position')
            locator_count += 1
        if layer != 'CANONICAL' and locator_count == 0 and status != 'migration_pending':
            raise ValueError('dynamic_poi_requires_position')
        parent = poi.get('parent_poi_id')
        if parent is not None and (not isinstance(parent,str) or not parent or parent == poi['id']):
            raise ValueError('invalid_parent_poi_id')
        history = poi.get('history', [])
        if not isinstance(history, list) or any(not isinstance(x,dict) for x in history):
            raise ValueError('invalid_poi_history')
        return deepcopy(poi)


class PoiCommandBuilder:
    """Builds host-authoritative POI mutation commands; never mutates a host DB."""
    def __init__(self, contract=None):
        self.contract = contract or DynamicPoiContract()

    def create(self, *, poi_id, campaign_id, logical_map_id, name, poi_type='other', layer='CAMPAIGN',
               hex_id=None, canonical_position=None, origin='player_created', hidden=False,
               discovered=True, map_package_id=None, atlas_namespace=None, coordinate_frame_id=None,
               description='', tags=None, parent_poi_id=None, owner_npc_id=None, faction_id=None,
               status='active', created_tick=0, metadata=None):
        if str(layer).upper() == 'CANONICAL':
            raise ValueError('canonical_poi_read_only')
        poi = {
            'id': poi_id, 'campaign_id': campaign_id, 'logical_map_id': logical_map_id,
            'map_package_id': map_package_id, 'atlas_namespace': atlas_namespace,
            'coordinate_frame_id': coordinate_frame_id, 'name': name, 'type': poi_type,
            'layer': str(layer).upper(), 'origin': origin, 'canonical': False,
            'hidden': bool(hidden), 'discovered': bool(discovered), 'status': status,
            'hex_id': hex_id, 'canonical_position': deepcopy(canonical_position),
            'description': str(description or ''), 'tags': list(tags or []),
            'parent_poi_id': parent_poi_id, 'owner_npc_id': owner_npc_id,
            'faction_id': faction_id, 'development_level': 1,
            'created_tick': int(created_tick), 'updated_tick': int(created_tick),
            'history': [], 'metadata': deepcopy(metadata or {}),
        }
        self.contract.validate(poi)
        return {'type':'poi.create','authority':'host','poi':poi}

    def update(self, poi, *, patch, tick, reason, actor=None):
        current = self.contract.validate(poi)
        if not isinstance(patch,dict) or not patch:
            raise ValueError('invalid_poi_patch')
        forbidden = {'id','campaign_id','logical_map_id','layer','canonical','origin'} & set(patch)
        if forbidden:
            raise ValueError('immutable_poi_fields:' + ','.join(sorted(forbidden)))
        updated = deepcopy(current)
        before = {k:deepcopy(updated.get(k)) for k in patch}
        updated.update(deepcopy(patch)); updated['updated_tick']=int(tick)
        updated.setdefault('history',[]).append({'tick':int(tick),'reason':str(reason),'actor':actor,'before':before,'after':{k:deepcopy(updated.get(k)) for k in patch}})
        self.contract.validate(updated)
        return {'type':'poi.update','authority':'host','poi_id':updated['id'],'patch':deepcopy(patch),'history_entry':deepcopy(updated['history'][-1])}

    def discover(self, poi, *, tick, method='exploration'):
        return self.update(poi, patch={'hidden':False,'discovered':True}, tick=tick, reason='discovered:'+str(method))

    def proposal(self, *, name, poi_type, logical_map_id, hex_id=None, canonical_position=None,
                 origin='gm_created', layer='CAMPAIGN', hidden=False, discovered=True,
                 description='', reason='', source='narrative', requires_confirmation=True):
        layer=str(layer).upper()
        if layer not in {'CAMPAIGN','PROCEDURAL'}: raise ValueError('invalid_poi_layer')
        if origin not in {'gm_created','campaign_discovery','world_generated','player_created'}: raise ValueError('invalid_poi_origin')
        if not isinstance(name,str) or not name.strip(): raise ValueError('invalid_poi_name')
        if not isinstance(logical_map_id,str) or not logical_map_id.strip(): raise ValueError('invalid_poi_logical_map_id')
        if not hex_id and not canonical_position: raise ValueError('poi_proposal_requires_position')
        return {
            'type':'poi.proposal','authority':'host','canonical':False,'layer':layer,
            'name':name.strip(),'poi_type':str(poi_type or 'other'),'logical_map_id':logical_map_id,
            'hex_id':hex_id,'canonical_position':deepcopy(canonical_position),'origin':origin,
            'hidden':bool(hidden),'discovered':bool(discovered),'description':str(description or ''),
            'reason':str(reason or ''),'source':str(source or 'narrative'),
            'requires_confirmation':bool(requires_confirmation),
        }


class AtlasPoiProjection:
    """Merges map layers without leaking GM-only procedural locations."""
    def __init__(self, contract=None):
        self.contract = contract or DynamicPoiContract()

    def visible(self, canonical, campaign, procedural, *, gm=False):
        out=[]
        seen=set()
        for poi in canonical or []:
            item=self.contract.validate({**deepcopy(poi),'layer':'CANONICAL','canonical':True,'origin':'canonical'},allow_canonical=True)
            if item['id'] in seen: raise ValueError('duplicate_poi_id')
            seen.add(item['id']); out.append(item)
        for layer, items in (('CAMPAIGN',campaign or []),('PROCEDURAL',procedural or [])):
            for poi in items:
                item=deepcopy(poi); item.setdefault('layer',layer); item.setdefault('canonical',False)
                self.contract.validate(item)
                if item['id'] in seen: raise ValueError('duplicate_poi_id')
                seen.add(item['id'])
                if gm or (item.get('discovered',False) and not item.get('hidden',False)):
                    out.append(item)
        return out


class PoiGenerationPolicy:
    """Produces generation *proposals* from structured world facts.

    System adapters supply templates; Barbara only validates contextual constraints,
    cadence and secrecy. No prose scraping and no canonical-map mutation.
    """
    def should_consider(self, *, elapsed_seconds=0, last_generation_seconds=None, trigger=None, min_interval_seconds=86400):
        if trigger in {'faction_change','trade_route_change','war','settlement_growth','discovery','world_event'}:
            return True
        if last_generation_seconds is None:
            return elapsed_seconds >= min_interval_seconds
        return elapsed_seconds - last_generation_seconds >= min_interval_seconds

    def propose(self, *, context, templates, trigger, seed_hint=None):
        if not isinstance(context,dict): raise ValueError('invalid_poi_generation_context')
        if not isinstance(templates,(list,tuple)) or any(not isinstance(x,dict) for x in templates): raise ValueError('invalid_poi_templates')
        terrain=str(context.get('terrain') or '').lower()
        tags={str(x).lower() for x in context.get('tags',[]) if str(x)}
        matched=[]
        for template in templates:
            req={str(x).lower() for x in template.get('requires_any',[]) if str(x)}
            exclude={str(x).lower() for x in template.get('exclude_any',[]) if str(x)}
            hay=tags|({terrain} if terrain else set())|{str(trigger).lower()}
            if req and not req & hay: continue
            if exclude & hay: continue
            matched.append(deepcopy(template))
        return {'trigger':str(trigger),'seed_hint':seed_hint,'candidates':matched,'canonical':False,'default_hidden':True}
