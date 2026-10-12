from copy import deepcopy

from .inventory import InventoryEngine
from .system_module import SystemModuleRegistry
from .time_model import GameClock
from .numeric import is_finite_number


class EffectResolver:
    """Applies only engine-authorized, typed effects to a campaign draft.

    LLM/provider outputs never enter this API. The caller is responsible for
    passing effects only after MechanicsAuthority and the active system adapter
    have validated the resolution envelope.
    """

    MAX_EFFECTS = 128

    def __init__(self, modules=None, game_clock=None, inventory=None):
        self.modules = modules or SystemModuleRegistry()
        self.game_clock = game_clock or GameClock(self.modules)
        self.inventory = inventory or InventoryEngine()

    _SET_EFFECTS = {
        'player_state_set': ('player_state', False),
        'scene_state_set': ('scene', False),
        'note_set': ('notes', False),
        'fact_set': ('facts', False),
        'world_flag_set': ('world_flags', False),
        'npc_state_set': ('npcs', True),
        'faction_state_set': ('factions', True),
        'site_state_set': ('sites', True),
    }
    _SCOPES = {'player_state','scene','facts','world_flags','npcs','factions','sites','clocks'}
    _ALLOWED = set(_SET_EFFECTS) | {
        'location_set','resource_delta','condition_add','condition_remove','clock_advance',
        'time_advance','light_source_ignite','light_source_extinguish',
        'damage','actor_move','item_move','item_consume'
    }

    def _path_parts(self, path):
        if not isinstance(path, str) or not path or path.startswith('.') or '..' in path:
            raise ValueError('invalid_effect_path')
        parts = path.split('.')
        if any(not part or part.startswith('_') for part in parts):
            raise ValueError('invalid_effect_path')
        return parts

    def _validate_entity(self, entity_id):
        if not isinstance(entity_id, str) or not entity_id:
            raise ValueError('invalid_effect_entity')

    def _number(self, value, code):
        if not is_finite_number(value):
            raise ValueError(code)
        return value

    def _validate_set_effect(self, effect, entity_scoped):
        required = {'type', 'path', 'value'} | ({'entity_id'} if entity_scoped else set())
        if set(effect) != required:
            raise ValueError('invalid_effect_fields')
        self._path_parts(effect['path'])
        if entity_scoped:
            self._validate_entity(effect['entity_id'])

    def _validate_resource_delta(self, effect):
        allowed = {'type','scope','path','delta','entity_id','minimum','maximum'}
        if set(effect) - allowed or not {'type','scope','path','delta'} <= set(effect):
            raise ValueError('invalid_effect_fields')
        if effect['scope'] not in self._SCOPES:
            raise ValueError('invalid_effect_scope')
        self._path_parts(effect['path'])
        if effect['scope'] in {'npcs','factions','sites'}:
            self._validate_entity(effect.get('entity_id'))
        elif 'entity_id' in effect:
            raise ValueError('invalid_effect_entity')
        self._number(effect['delta'], 'invalid_effect_delta')
        for key in ('minimum','maximum'):
            if key in effect:
                self._number(effect[key], 'invalid_effect_bound')
        if 'minimum' in effect and 'maximum' in effect and effect['minimum'] > effect['maximum']:
            raise ValueError('invalid_effect_bounds')

    def _validate_condition(self, effect):
        allowed = {'type','scope','condition','entity_id'}
        if set(effect) - allowed or not {'type','scope','condition'} <= set(effect):
            raise ValueError('invalid_effect_fields')
        if effect['scope'] not in {'player_state','npcs'}:
            raise ValueError('invalid_effect_scope')
        if not isinstance(effect['condition'], str) or not effect['condition']:
            raise ValueError('invalid_effect_condition')
        if effect['scope'] == 'npcs':
            self._validate_entity(effect.get('entity_id'))
        elif 'entity_id' in effect:
            raise ValueError('invalid_effect_entity')

    def validate(self, effects):
        if effects is None:
            return []
        if not isinstance(effects, list) or len(effects) > self.MAX_EFFECTS:
            raise ValueError('invalid_effects')
        out = []
        for effect in effects:
            if not isinstance(effect, dict):
                raise ValueError('invalid_effect')
            effect_type = effect.get('type')
            if effect_type not in self._ALLOWED:
                raise ValueError('unsupported_effect_type')
            if effect_type == 'location_set':
                if set(effect) != {'type', 'location'}:
                    raise ValueError('invalid_effect_fields')
                if not isinstance(effect['location'], str):
                    raise ValueError('invalid_effect_location')
            elif effect_type in self._SET_EFFECTS:
                _, entity_scoped = self._SET_EFFECTS[effect_type]
                self._validate_set_effect(effect, entity_scoped)
            elif effect_type == 'resource_delta':
                self._validate_resource_delta(effect)
            elif effect_type in {'condition_add','condition_remove'}:
                self._validate_condition(effect)
            elif effect_type == 'damage':
                allowed={'type','scope','amount','resource_path','entity_id','minimum'}
                if set(effect)-allowed or not {'type','scope','amount'} <= set(effect): raise ValueError('invalid_effect_fields')
                if effect['scope'] not in {'player_state','npcs'}: raise ValueError('invalid_effect_scope')
                if effect['scope']=='npcs': self._validate_entity(effect.get('entity_id'))
                elif 'entity_id' in effect: raise ValueError('invalid_effect_entity')
                self._number(effect['amount'],'invalid_damage_amount')
                if effect['amount']<0: raise ValueError('invalid_damage_amount')
                if 'resource_path' in effect: self._path_parts(effect['resource_path'])
                if 'minimum' in effect: self._number(effect['minimum'],'invalid_effect_bound')
            elif effect_type == 'actor_move':
                allowed={'type','scope','location','entity_id','position'}
                if set(effect)-allowed or not {'type','scope','location'}<=set(effect): raise ValueError('invalid_effect_fields')
                if effect['scope'] not in {'player','npc'}: raise ValueError('invalid_effect_scope')
                if not isinstance(effect['location'],str) or not effect['location']: raise ValueError('invalid_effect_location')
                if effect['scope']=='npc': self._validate_entity(effect.get('entity_id'))
                elif 'entity_id' in effect: raise ValueError('invalid_effect_entity')
                if 'position' in effect and not isinstance(effect['position'],dict): raise ValueError('invalid_effect_position')
            elif effect_type == 'item_move':
                if set(effect)!={'type','item_id','from_container','to_container'}: raise ValueError('invalid_effect_fields')
                if not all(isinstance(effect[k],str) and effect[k] for k in ('item_id','from_container','to_container')): raise ValueError('invalid_inventory_effect')
            elif effect_type == 'item_consume':
                if set(effect)!={'type','item_id','quantity'}: raise ValueError('invalid_effect_fields')
                if not isinstance(effect['item_id'],str) or not effect['item_id']: raise ValueError('invalid_inventory_effect')
                self._number(effect['quantity'],'invalid_inventory_item_quantity')
                if effect['quantity']<=0: raise ValueError('invalid_inventory_item_quantity')
            elif effect_type == 'clock_advance':
                if set(effect) != {'type','clock_id','amount'}:
                    raise ValueError('invalid_effect_fields')
                if not isinstance(effect['clock_id'], str) or not effect['clock_id']:
                    raise ValueError('invalid_clock_id')
                self._number(effect['amount'],'invalid_effect_delta')
            elif effect_type == 'time_advance':
                if set(effect) != {'type','unit','amount'}:
                    raise ValueError('invalid_effect_fields')
                if not isinstance(effect['unit'], str) or not effect['unit']:
                    raise ValueError('invalid_time_unit')
                self._number(effect['amount'],'invalid_time_amount')
                if effect['amount'] < 0: raise ValueError('invalid_time_amount')
            elif effect_type == 'light_source_ignite':
                allowed={'type','location_id','source_id','intensity','radius','position','owner','duration_seconds'}
                if set(effect)-allowed or not {'type','location_id','source_id','intensity'} <= set(effect):
                    raise ValueError('invalid_effect_fields')
                if not isinstance(effect['location_id'],str) or not effect['location_id'] or not isinstance(effect['source_id'],str) or not effect['source_id']:
                    raise ValueError('invalid_light_source')
                self._number(effect['intensity'],'invalid_light_source_intensity')
                if not 0<=effect['intensity']<=1: raise ValueError('invalid_light_source_intensity')
                if 'duration_seconds' in effect:
                    self._number(effect['duration_seconds'],'invalid_light_duration')
                    if effect['duration_seconds']<=0: raise ValueError('invalid_light_duration')
            elif effect_type == 'light_source_extinguish':
                if set(effect) != {'type','location_id','source_id'}:
                    raise ValueError('invalid_effect_fields')
                if not all(isinstance(effect[k],str) and effect[k] for k in ('location_id','source_id')):
                    raise ValueError('invalid_light_source')
            out.append(deepcopy(effect))
        return out

    def _set_nested(self, target, path, value):
        current = target
        parts = self._path_parts(path)
        for part in parts[:-1]:
            old = current.get(part)
            if old is None:
                current[part] = {}
                old = current[part]
            if not isinstance(old, dict):
                raise ValueError('effect_path_type_conflict')
            current = old
        current[parts[-1]] = deepcopy(value)

    def _get_nested_parent(self, target, path):
        current = target
        parts = self._path_parts(path)
        for part in parts[:-1]:
            current = current.get(part)
            if not isinstance(current, dict):
                raise ValueError('effect_path_type_conflict')
        return current, parts[-1]

    def _scope_target(self, state, scope, entity_id=None):
        root = getattr(state, scope)
        if scope in {'npcs','factions','sites'}:
            entity = root.get(entity_id)
            if entity is None:
                raise ValueError('effect_entity_not_found')
            if not isinstance(entity, dict):
                raise ValueError('effect_entity_type_conflict')
            return entity
        return root

    def _apply_resource_delta(self, state, effect):
        target = self._scope_target(state, effect['scope'], effect.get('entity_id'))
        parent, leaf = self._get_nested_parent(target, effect['path'])
        if leaf not in parent:
            raise ValueError('effect_resource_not_found')
        old = parent[leaf]
        if not is_finite_number(old):
            raise ValueError('effect_resource_not_numeric')
        value = old + effect['delta']
        if 'minimum' in effect:
            value = max(effect['minimum'], value)
        if 'maximum' in effect:
            value = min(effect['maximum'], value)
        parent[leaf] = value

    def _conditions(self, state, effect):
        target = self._scope_target(state, effect['scope'], effect.get('entity_id'))
        conditions = target.setdefault('conditions', [])
        if not isinstance(conditions, list):
            raise ValueError('effect_conditions_not_list')
        return conditions

    def _apply_one(self, state, effect):
        effect_type = effect['type']
        if effect_type == 'location_set':
            state.location = effect['location']
            return
        if effect_type == 'resource_delta':
            self._apply_resource_delta(state, effect)
            return
        if effect_type == 'condition_add':
            conditions = self._conditions(state, effect)
            if effect['condition'] not in conditions:
                conditions.append(effect['condition'])
            return
        if effect_type == 'condition_remove':
            conditions = self._conditions(state, effect)
            while effect['condition'] in conditions:
                conditions.remove(effect['condition'])
            return
        if effect_type == 'damage':
            translated={'type':'resource_delta','scope':effect['scope'],'path':effect.get('resource_path','resources.hp'),'delta':-effect['amount'],'minimum':effect.get('minimum',0)}
            if 'entity_id' in effect: translated['entity_id']=effect['entity_id']
            self._apply_resource_delta(state,translated); return
        if effect_type == 'actor_move':
            if effect['scope']=='player':
                state.location=effect['location']
                if 'position' in effect: state.player_state['position']=deepcopy(effect['position'])
            else:
                npc=state.npcs.get(effect['entity_id'])
                if not isinstance(npc,dict): raise ValueError('effect_entity_not_found')
                npc['location']=effect['location']
                if 'position' in effect: npc['position']=deepcopy(effect['position'])
            return
        if effect_type == 'item_move':
            self.inventory.move(state,{**deepcopy(effect),'request_id':'effect'},self.modules.get(state.system_id),emit_event=False); return
        if effect_type == 'item_consume':
            self.inventory.consume(state,{**deepcopy(effect),'request_id':'effect'},self.modules.get(state.system_id),emit_event=False); return
        if effect_type == 'clock_advance':
            current = state.clocks.get(effect['clock_id'], 0)
            if not isinstance(current, (int,float)) or isinstance(current, bool):
                raise ValueError('invalid_clock_value')
            state.clocks[effect['clock_id']] = current + effect['amount']
            return
        if effect_type == 'time_advance':
            self.game_clock.advance(state,effect['unit'],effect['amount'],emit_event=False)
            return
        if effect_type == 'light_source_ignite':
            site=state.sites.setdefault(effect['location_id'],{})
            if not isinstance(site,dict): raise ValueError('invalid_site_state')
            sources=site.setdefault('light_sources',[])
            if not isinstance(sources,list): raise ValueError('invalid_light_sources')
            elapsed=float((state.game_time or {}).get('elapsed_seconds',0))
            source={'id':effect['source_id'],'status':'lit','intensity':float(effect['intensity'])}
            for key in ('radius','position','owner'):
                if key in effect: source[key]=deepcopy(effect[key])
            if 'duration_seconds' in effect: source['expires_at_seconds']=elapsed+float(effect['duration_seconds'])
            sources[:]=[x for x in sources if not (isinstance(x,dict) and x.get('id')==effect['source_id'])]
            sources.append(source)
            return
        if effect_type == 'light_source_extinguish':
            site=state.sites.get(effect['location_id'])
            if not isinstance(site,dict): raise ValueError('effect_entity_not_found')
            sources=site.get('light_sources',[])
            if not isinstance(sources,list): raise ValueError('invalid_light_sources')
            found=False
            for source in sources:
                if isinstance(source,dict) and source.get('id')==effect['source_id']:
                    source['status']='extinguished'; found=True
            if not found: raise ValueError('light_source_not_found')
            return
        root_name, entity_scoped = self._SET_EFFECTS[effect_type]
        root = getattr(state, root_name)
        if entity_scoped:
            entity_id = effect['entity_id']
            entity = root.get(entity_id)
            if entity is None:
                root[entity_id] = {}
                entity = root[entity_id]
            if not isinstance(entity, dict):
                raise ValueError('effect_entity_type_conflict')
            self._set_nested(entity, effect['path'], effect['value'])
        else:
            self._set_nested(root, effect['path'], effect['value'])

    def apply(self, state, effects, request_id, state_version, transaction_id=None):
        if not isinstance(request_id,str) or not request_id:
            raise ValueError('invalid_effect_request_id')
        if not isinstance(state_version,int) or isinstance(state_version,bool) or state_version<0:
            raise ValueError('invalid_effect_state_version')
        validated = self.validate(effects)
        draft = deepcopy(state)
        for index, effect in enumerate(validated):
            self._apply_one(draft, effect)
            draft.event_log.append({
                'event_id': f'{request_id}:effect:{index}',
                'type': effect['type'],
                'request_id': request_id,
                'tick': draft.tick,
                'state_version': state_version,
                'payload': {**deepcopy(effect), **({'transaction_id':transaction_id} if transaction_id else {})},
            })
        draft.validate()
        state.__dict__.clear()
        state.__dict__.update(deepcopy(draft.__dict__))
        return validated

    def apply_replay(self,state,effect):
        """Apply one canonical event without emitting a duplicate replay event."""
        validated=self.validate([deepcopy(effect)])
        draft=deepcopy(state)
        self._apply_one(draft,validated[0])
        draft.validate()
        state.__dict__.clear()
        state.__dict__.update(deepcopy(draft.__dict__))
        return deepcopy(validated[0])
