from __future__ import annotations

import math
import uuid
from copy import deepcopy

from .dice import DiceEngine, SecureRandomSource
from .numeric import is_finite_number
from .state import CampaignState


class VttCommandError(ValueError):
    def __init__(self, code: str):
        super().__init__(code)
        self.code = code


class VttGridMath:
    """Shared deterministic grid math used by the canonical VTT authority.

    Square coordinates snap to *cell centres*, not line intersections. Hexes use
    axial coordinates and cube rounding for flat- and pointy-top layouts.
    """

    @staticmethod
    def validate_grid(grid: dict) -> dict:
        if not isinstance(grid, dict):
            raise VttCommandError('invalid_grid')
        allowed = {'type', 'size', 'scale', 'unit', 'originX', 'originY', 'orientation'}
        if set(grid) - allowed:
            raise VttCommandError('invalid_grid_field')
        g = {
            'type': grid.get('type', 'square'),
            'size': grid.get('size', 70),
            'scale': grid.get('scale', 1.5),
            'unit': grid.get('unit', 'm'),
            'originX': grid.get('originX', 0),
            'originY': grid.get('originY', 0),
            'orientation': grid.get('orientation', 'flat'),
        }
        if g['type'] not in {'square', 'hex'}:
            raise VttCommandError('invalid_grid')
        if g['orientation'] not in {'flat', 'pointy'}:
            raise VttCommandError('invalid_grid')
        for key in ('size', 'scale', 'originX', 'originY'):
            if not is_finite_number(g[key]):
                raise VttCommandError('invalid_grid')
        if not 8 <= g['size'] <= 500 or not 0.001 <= g['scale'] <= 100000:
            raise VttCommandError('invalid_grid')
        if not isinstance(g['unit'], str) or not g['unit'] or len(g['unit']) > 20:
            raise VttCommandError('invalid_grid')
        return g

    @staticmethod
    def _cube_round(q: float, r: float) -> tuple[int, int]:
        x, z, y = q, r, -q-r
        rx, ry, rz = round(x), round(y), round(z)
        dx, dy, dz = abs(rx-x), abs(ry-y), abs(rz-z)
        if dx > dy and dx > dz:
            rx = -ry-rz
        elif dy > dz:
            ry = -rx-rz
        else:
            rz = -rx-ry
        return int(rx), int(rz)

    @classmethod
    def pixel_to_cell(cls, x: float, y: float, grid: dict) -> dict:
        g = cls.validate_grid(grid)
        if not is_finite_number(x) or not is_finite_number(y):
            raise VttCommandError('invalid_coordinates')
        ox, oy, s = g['originX'], g['originY'], g['size']
        if g['type'] == 'square':
            # Nearest cell centre where boundaries are origin + n*size.
            col = math.floor((x-ox)/s)
            row = math.floor((y-oy)/s)
            return {'col': int(col), 'row': int(row)}
        if g['orientation'] == 'pointy':
            q = ((math.sqrt(3)/3)*(x-ox) - (1/3)*(y-oy))/s
            r = ((2/3)*(y-oy))/s
        else:
            q = ((2/3)*(x-ox))/s
            r = ((-1/3)*(x-ox) + (math.sqrt(3)/3)*(y-oy))/s
        q, r = cls._cube_round(q, r)
        return {'q': q, 'r': r}

    @classmethod
    def cell_to_pixel(cls, cell: dict, grid: dict) -> dict:
        g = cls.validate_grid(grid)
        s, ox, oy = g['size'], g['originX'], g['originY']
        if g['type'] == 'square':
            if not isinstance(cell, dict) or not isinstance(cell.get('col'), int) or isinstance(cell.get('col'), bool) or not isinstance(cell.get('row'), int) or isinstance(cell.get('row'), bool):
                raise VttCommandError('invalid_grid_cell')
            return {'x': ox+(cell['col']+0.5)*s, 'y': oy+(cell['row']+0.5)*s}
        if not isinstance(cell, dict) or not isinstance(cell.get('q'), int) or isinstance(cell.get('q'), bool) or not isinstance(cell.get('r'), int) or isinstance(cell.get('r'), bool):
            raise VttCommandError('invalid_grid_cell')
        q, r = cell['q'], cell['r']
        if g['orientation'] == 'pointy':
            return {'x': ox+s*math.sqrt(3)*(q+r/2), 'y': oy+s*1.5*r}
        return {'x': ox+s*1.5*q, 'y': oy+s*math.sqrt(3)*(r+q/2)}

    @classmethod
    def snap(cls, x: float, y: float, grid: dict) -> dict:
        cell = cls.pixel_to_cell(x, y, grid)
        return {**cls.cell_to_pixel(cell, grid), 'cell': cell}

    @classmethod
    def distance_cells(cls, a: dict, b: dict, grid: dict) -> int:
        ca = cls.pixel_to_cell(a['x'], a['y'], grid)
        cb = cls.pixel_to_cell(b['x'], b['y'], grid)
        if 'col' in ca:
            return max(abs(ca['col']-cb['col']), abs(ca['row']-cb['row']))
        ax, az = ca['q'], ca['r']; ay = -ax-az
        bx, bz = cb['q'], cb['r']; by = -bx-bz
        return max(abs(ax-bx), abs(ay-by), abs(az-bz))


class VttAuthority:
    MAX_TOKENS = 2000
    MAX_FOG_POLYGONS = 2000
    MAX_POLYGON_POINTS = 256

    def __init__(self, *, dice_engine: DiceEngine | None = None, atlas_provider=None):
        self.dice = dice_engine or DiceEngine(SecureRandomSource())
        self.atlas_provider = atlas_provider

    @staticmethod
    def _default_vtt() -> dict:
        return {
            'grid': {'type':'square','size':70,'scale':1.5,'unit':'m','originX':0,'originY':0,'orientation':'flat'},
            'map': {'tiles': [], 'width': 2800, 'height': 2100, 'occluders': []},
            'tokens': {},
            'fog': {'revealed': []},
        }

    @staticmethod
    def _segments_intersect(a,b,c,d):
        def orient(p,q,r):
            return (q['x']-p['x'])*(r['y']-p['y'])-(q['y']-p['y'])*(r['x']-p['x'])
        o1,o2,o3,o4=orient(a,b,c),orient(a,b,d),orient(c,d,a),orient(c,d,b)
        return ((o1>0 and o2<0) or (o1<0 and o2>0)) and ((o3>0 and o4<0) or (o3<0 and o4>0))

    @classmethod
    def _has_los(cls, source, target, occluders):
        if not isinstance(source,dict) or not isinstance(target,dict): return False
        a={'x':source.get('x'),'y':source.get('y')}; b={'x':target.get('x'),'y':target.get('y')}
        if not all(is_finite_number(p[k]) for p in (a,b) for k in ('x','y')): return False
        for wall in occluders or []:
            if not isinstance(wall,dict): continue
            c=wall.get('a'); d=wall.get('b')
            if isinstance(c,dict) and isinstance(d,dict) and cls._segments_intersect(a,b,c,d): return False
        return True

    def snapshot(self, state: CampaignState, *, actor_id=None, role='gm') -> dict:
        state.validate()
        current = deepcopy(state.scene.get('vtt') or self._default_vtt())
        if role=='gm' or actor_id is None:
            return current
        if role!='player': raise VttCommandError('unauthorized')
        tokens=current.get('tokens',{}) if isinstance(current.get('tokens'),dict) else {}
        source=None
        for token in tokens.values():
            if isinstance(token,dict) and (token.get('ownerId')==actor_id or token.get('entityRef')=='player'):
                source=token; break
        if source is None:
            current['tokens']={}
            return current
        occluders=(current.get('map') or {}).get('occluders',[])
        visible={}
        for token_id,token in tokens.items():
            if not isinstance(token,dict) or token.get('hidden') is True: continue
            if token is source or self._has_los(source,token,occluders): visible[token_id]=token
        current['tokens']=visible
        return current

    @staticmethod
    def _sync_entity_position(state, token):
        ref=token.get('entityRef')
        if not isinstance(ref,str) or not ref: return
        position={'x':token.get('x'),'y':token.get('y'),'cell':deepcopy(token.get('cell'))}
        if state.location: position['location_id']=state.location
        if ref=='player':
            state.player_state['position']=position
        elif ref.startswith('npc:'):
            npc_id=ref[4:]
            npc=state.npcs.get(npc_id)
            if isinstance(npc,dict):
                npc['position']=position
                if state.location: npc['location']=state.location

    @staticmethod
    def _valid_id(value, *, limit=160):
        return isinstance(value, str) and 0 < len(value) <= limit and all(ch.isalnum() or ch in '_.:-' for ch in value)

    def _event(self, state: CampaignState, command_id: str, event_type: str, actor_id: str, payload: dict) -> dict:
        state.state_version += 1
        event = {
            'event_id': str(uuid.uuid4()),
            'type': 'vtt.' + event_type,
            'request_id': command_id,
            'tick': state.tick,
            'state_version': state.state_version,
            'payload': {'actor_id': actor_id, **deepcopy(payload)},
        }
        state.event_log.append(event)
        # Keep a bounded hot projection while the durable command receipt preserves exactly-once semantics.
        if len(state.event_log) > 10000:
            del state.event_log[:-10000]
        return event

    def apply(self, state: CampaignState, command: dict, *, actor_id: str, role: str = 'player') -> tuple[CampaignState, dict]:
        if not isinstance(state, CampaignState):
            raise TypeError('invalid_campaign_state')
        if not isinstance(command, dict):
            raise VttCommandError('invalid_command')
        command_id = command.get('command_id') or command.get('commandId')
        base_version = command.get('base_version', command.get('baseVersion'))
        kind = command.get('command')
        if not self._valid_id(command_id):
            raise VttCommandError('invalid_command_id')
        if not isinstance(base_version, int) or isinstance(base_version, bool) or base_version != state.state_version:
            raise VttCommandError('state_version_conflict')
        if role not in {'gm', 'player'} or not self._valid_id(actor_id):
            raise VttCommandError('unauthorized')

        draft = state.snapshot()
        vtt = deepcopy(draft.scene.get('vtt') or self._default_vtt())
        grid = VttGridMath.validate_grid(vtt.get('grid') or {})
        vtt['grid'] = grid
        vtt.setdefault('tokens', {}); vtt.setdefault('map', {'tiles': [], 'width':2800, 'height':2100, 'occluders':[]}); vtt.setdefault('fog', {'revealed':[]})
        if not isinstance(vtt['tokens'], dict) or not isinstance(vtt['map'], dict) or not isinstance(vtt['fog'], dict):
            raise VttCommandError('invalid_vtt_state')

        payload = None; event_type = None
        if kind == 'move_token':
            token_id = command.get('token_id', command.get('tokenId'))
            if not self._valid_id(token_id): raise VttCommandError('invalid_token_id')
            token = vtt['tokens'].get(token_id)
            if not isinstance(token, dict): raise VttCommandError('unknown_token')
            if role != 'gm' and token.get('ownerId') not in {None, actor_id}:
                raise VttCommandError('not_token_owner')
            p = VttGridMath.snap(command.get('x'), command.get('y'), grid)
            token.update({'x':p['x'],'y':p['y'],'cell':p['cell']})
            self._sync_entity_position(draft,token)
            event_type='token_moved'; payload={'tokenId':token_id, **p}
        elif kind == 'spawn_token':
            if role != 'gm': raise VttCommandError('gm_only')
            if len(vtt['tokens']) >= self.MAX_TOKENS: raise VttCommandError('token_limit')
            token_id = command.get('token_id', command.get('tokenId')) or str(uuid.uuid4())
            if not self._valid_id(token_id): raise VttCommandError('invalid_token_id')
            if token_id in vtt['tokens']: raise VttCommandError('duplicate_token_id')
            p = VttGridMath.snap(command.get('x'), command.get('y'), grid)
            owner = command.get('owner_id', command.get('ownerId'))
            if owner is not None and not self._valid_id(owner): raise VttCommandError('invalid_owner_id')
            size = command.get('size', 1)
            if not is_finite_number(size) or not 0.25 <= size <= 8: raise VttCommandError('invalid_token_size')
            image = command.get('image')
            if image is not None and (not isinstance(image, str) or len(image) > 4096): raise VttCommandError('invalid_token_image')
            entity_ref=command.get('entity_ref',command.get('entityRef'))
            if entity_ref is not None and (not isinstance(entity_ref,str) or len(entity_ref)>200): raise VttCommandError('invalid_entity_ref')
            hidden=command.get('hidden',False)
            if not isinstance(hidden,bool): raise VttCommandError('invalid_hidden')
            token={'id':token_id,'name':str(command.get('name') or 'Token')[:80],'ownerId':owner,'entityRef':entity_ref,'hidden':hidden,'x':p['x'],'y':p['y'],'cell':p['cell'],'size':size,'image':image}
            vtt['tokens'][token_id]=token
            self._sync_entity_position(draft,token)
            event_type='token_spawned'; payload={'token':deepcopy(token)}
        elif kind == 'delete_token':
            if role != 'gm': raise VttCommandError('gm_only')
            token_id=command.get('token_id', command.get('tokenId'))
            if token_id not in vtt['tokens']: raise VttCommandError('unknown_token')
            removed=vtt['tokens'].pop(token_id)
            event_type='token_deleted'; payload={'tokenId':token_id,'token':removed}
        elif kind == 'set_grid':
            if role != 'gm': raise VttCommandError('gm_only')
            incoming=command.get('grid')
            if not isinstance(incoming,dict): raise VttCommandError('invalid_grid')
            merged={**grid,**incoming}; grid=VttGridMath.validate_grid(merged); vtt['grid']=grid
            # Re-snap all tokens to avoid projection/server disagreement after a grid change.
            for token in vtt['tokens'].values():
                p=VttGridMath.snap(token.get('x'),token.get('y'),grid); token.update({'x':p['x'],'y':p['y'],'cell':p['cell']}); self._sync_entity_position(draft,token)
            event_type='grid_changed'; payload={'grid':deepcopy(grid)}
        elif kind == 'set_map':
            if role != 'gm': raise VttCommandError('gm_only')
            value=command.get('map')
            if not isinstance(value,dict) or set(value)-{'tiles','width','height','atlas_namespace','location_id','location_ref','occluders'}: raise VttCommandError('invalid_map')
            tiles=value.get('tiles',[])
            if not isinstance(tiles,list) or len(tiles)>20000: raise VttCommandError('invalid_map')
            clean=[]
            for tile in tiles:
                if not isinstance(tile,dict) or set(tile)-{'id','src','x','y','w','h','z','color'}: raise VttCommandError('invalid_map_tile')
                if any(k in tile and not is_finite_number(tile[k]) for k in ('x','y','w','h','z')): raise VttCommandError('invalid_map_tile')
                if 'src' in tile and (not isinstance(tile['src'],str) or len(tile['src'])>4096): raise VttCommandError('invalid_map_tile')
                clean.append(deepcopy(tile))
            width=value.get('width',vtt['map'].get('width',2800)); height=value.get('height',vtt['map'].get('height',2100))
            if not is_finite_number(width) or not is_finite_number(height) or width<=0 or height<=0: raise VttCommandError('invalid_map')
            occluders=value.get('occluders',[])
            if not isinstance(occluders,list) or len(occluders)>10000: raise VttCommandError('invalid_occluders')
            clean_occ=[]
            for wall in occluders:
                if not isinstance(wall,dict) or set(wall)!={'a','b'}: raise VttCommandError('invalid_occluder')
                if any(not isinstance(wall.get(end),dict) or set(wall[end])!={'x','y'} or not is_finite_number(wall[end]['x']) or not is_finite_number(wall[end]['y']) for end in ('a','b')): raise VttCommandError('invalid_occluder')
                clean_occ.append(deepcopy(wall))
            vtt['map']={'tiles':clean,'width':width,'height':height,'occluders':clean_occ}
            location_ref=value.get('location_id',value.get('location_ref'))
            if location_ref is not None:
                if not isinstance(location_ref,(str,dict)): raise VttCommandError('invalid_location_ref')
                resolved=None
                resolver=getattr(self.atlas_provider,'resolve_site',None) if self.atlas_provider is not None else None
                if callable(resolver): resolved=resolver(location_ref,namespace=value.get('atlas_namespace'))
                if resolved is None and self.atlas_provider is not None: raise VttCommandError('unknown_atlas_location')
                canonical=str(((resolved or {}).get('_atlas_resolution') or {}).get('site_id') or (resolved or {}).get('id') or location_ref)
                draft.location=canonical; vtt['map']['location_id']=canonical
            if value.get('atlas_namespace') is not None:
                ns=value['atlas_namespace']
                if not isinstance(ns,str) or not ns or len(ns)>200: raise VttCommandError('invalid_map')
                vtt['map']['atlas_namespace']=ns
            event_type='map_changed'; payload={'map':deepcopy(vtt['map'])}
        elif kind == 'reveal_fog':
            if role != 'gm': raise VttCommandError('gm_only')
            revealed=vtt['fog'].setdefault('revealed',[])
            if not isinstance(revealed,list) or len(revealed)>=self.MAX_FOG_POLYGONS: raise VttCommandError('fog_limit')
            polygon=command.get('polygon')
            if not isinstance(polygon,list) or not 3<=len(polygon)<=self.MAX_POLYGON_POINTS: raise VttCommandError('invalid_polygon')
            clean=[]
            for p in polygon:
                if not isinstance(p,dict) or set(p)!={'x','y'} or not is_finite_number(p['x']) or not is_finite_number(p['y']): raise VttCommandError('invalid_polygon')
                clean.append({'x':p['x'],'y':p['y']})
            revealed.append(clean); event_type='fog_revealed'; payload={'polygon':clean}
        elif kind == 'clear_fog':
            if role != 'gm': raise VttCommandError('gm_only')
            vtt['fog']={'revealed':[]}; event_type='fog_cleared'; payload={}
        elif kind == 'roll_dice':
            count=command.get('count',1); sides=command.get('sides',20)
            if not isinstance(count,int) or isinstance(count,bool) or not 1<=count<=100 or not isinstance(sides,int) or isinstance(sides,bool) or not 2<=sides<=10000:
                raise VttCommandError('invalid_dice')
            trace=self.dice.roll({'op':'dice','count':count,'sides':sides})
            rolls=list(trace['value']) if isinstance(trace['value'],list) else [trace['value']]
            event_type='dice_rolled'; payload={'count':count,'sides':sides,'rolls':rolls,'total':sum(rolls),'rng_trace':trace.get('trace',[])}
        else:
            raise VttCommandError('unsupported_command')

        draft.scene['vtt']=vtt
        event=self._event(draft,command_id,event_type,actor_id,payload)
        draft.validate()
        return draft, {'type':'event','state_version':draft.state_version,'event':deepcopy(event),'state':deepcopy(vtt)}
