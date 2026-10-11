import json
from copy import deepcopy
from dataclasses import asdict, dataclass, field

from .numeric import is_finite_number

MAX_STATE_JSON_CHARS=32*1024*1024


def _validate_canonical_value(value, seen=None, depth=0, counter=None):
    """Reject values which cannot round-trip through strict portable JSON."""
    if seen is None:
        seen = set()
    if counter is None:
        counter = [0]
    counter[0] += 1
    if counter[0] > 100_000:
        raise ValueError('state_too_large')
    if depth > 64:
        raise ValueError('state_too_deep')
    if value is None or isinstance(value, (str, bool)):
        return
    if isinstance(value,int):
        if not -(2**63)<=value<2**63:raise ValueError('invalid_state_integer')
        return
    if isinstance(value, float):
        if not is_finite_number(value):
            raise ValueError('invalid_state_number')
        return
    if isinstance(value, dict):
        identity = id(value)
        if identity in seen:
            raise ValueError('invalid_state_cycle')
        seen.add(identity)
        try:
            for key, item in value.items():
                if not isinstance(key, str):
                    raise ValueError('invalid_state_key')
                _validate_canonical_value(item, seen, depth + 1, counter)
        finally:
            seen.remove(identity)
        return
    if isinstance(value, (list, tuple)):
        identity = id(value)
        if identity in seen:
            raise ValueError('invalid_state_cycle')
        seen.add(identity)
        try:
            for item in value:
                _validate_canonical_value(item, seen, depth + 1, counter)
        finally:
            seen.remove(identity)
        return
    raise ValueError('invalid_state_value')

@dataclass
class CampaignState:
    campaign_id:str
    system_id:str
    tick:int=0
    location:str=''
    facts:dict=field(default_factory=dict)
    world_flags:dict=field(default_factory=dict)
    npcs:dict=field(default_factory=dict)
    factions:dict=field(default_factory=dict)
    rumors:list=field(default_factory=list)
    events:list=field(default_factory=list)
    memory:list=field(default_factory=list)
    clocks:dict=field(default_factory=dict)
    economy:dict=field(default_factory=dict)
    weather:dict=field(default_factory=dict)
    player_state:dict=field(default_factory=dict)
    scene:dict=field(default_factory=dict)
    notes:dict=field(default_factory=dict)
    sites:dict=field(default_factory=dict)
    public_ledger:list=field(default_factory=list)
    secret_ledger:list=field(default_factory=list)
    request_log:dict=field(default_factory=dict)
    discovery:dict=field(default_factory=dict)
    state_version:int=0
    event_log:list=field(default_factory=list)
    pending_action:dict=field(default_factory=dict)
    game_time:dict=field(default_factory=dict)
    system_module_version:str=''
    migration_history:list=field(default_factory=list)
    def snapshot(self): return deepcopy(self)
    def validate(self):
        if not isinstance(self.campaign_id,str) or not self.campaign_id or len(self.campaign_id)>160: raise ValueError('invalid_campaign_id')
        if not isinstance(self.system_id,str) or not self.system_id or len(self.system_id)>160: raise ValueError('invalid_system_id')
        if not isinstance(self.tick,int) or isinstance(self.tick,bool) or self.tick<0: raise ValueError('invalid_tick')
        if not isinstance(self.state_version,int) or isinstance(self.state_version,bool) or self.state_version<0: raise ValueError('invalid_state_version')
        if not isinstance(self.location,str): raise ValueError('invalid_location')
        if not isinstance(self.system_module_version,str): raise ValueError('invalid_system_module_version')
        for name in ('facts','world_flags','npcs','factions','clocks','economy','weather','player_state','scene','notes','sites','request_log','discovery','pending_action','game_time'):
            if not isinstance(getattr(self,name),dict): raise ValueError('invalid_'+name)
        for name in ('rumors','events','event_log','memory','public_ledger','secret_ledger','migration_history'):
            if not isinstance(getattr(self,name),list): raise ValueError('invalid_'+name)
        for name in ('npcs','factions','sites'):
            if any(not isinstance(key,str) or not key or not isinstance(value,dict) for key,value in getattr(self,name).items()):
                raise ValueError('invalid_'+name+'_entry')
        for name in ('rumors','events','memory','public_ledger','secret_ledger'):
            if any(not isinstance(entry,dict) for entry in getattr(self,name)):
                raise ValueError('invalid_'+name+'_entry')
        seen_event_ids=set()
        for entry in self.event_log:
            if not isinstance(entry,dict): raise ValueError('invalid_event_log_entry')
            if not isinstance(entry.get('event_id'),str) or not entry['event_id']: raise ValueError('invalid_event_log_entry')
            if entry['event_id'] in seen_event_ids: raise ValueError('duplicate_event_id')
            seen_event_ids.add(entry['event_id'])
            if not isinstance(entry.get('type'),str) or not entry['type']: raise ValueError('invalid_event_log_entry')
            if not isinstance(entry.get('request_id'),str) or not entry['request_id']: raise ValueError('invalid_event_log_entry')
            if not isinstance(entry.get('tick'),int) or isinstance(entry['tick'],bool) or entry['tick']<0: raise ValueError('invalid_event_log_entry')
            if not isinstance(entry.get('state_version'),int) or isinstance(entry['state_version'],bool) or entry['state_version']<0: raise ValueError('invalid_event_log_entry')
            if not isinstance(entry.get('payload'),dict): raise ValueError('invalid_event_log_entry')
        for entry in self.migration_history:
            if not isinstance(entry,dict): raise ValueError('invalid_migration_history')
            if not isinstance(entry.get('to'),str) or not entry['to']: raise ValueError('invalid_migration_history')
            if entry.get('from') is not None and (not isinstance(entry.get('from'),str) or not entry['from']): raise ValueError('invalid_migration_history')
        if self.pending_action:
            required={'action_id','request_id','phase','state_version','payload'}
            if set(self.pending_action)!=required: raise ValueError('invalid_pending_action_fields')
            if not isinstance(self.pending_action['action_id'],str) or not self.pending_action['action_id']: raise ValueError('invalid_pending_action_id')
            if not isinstance(self.pending_action['request_id'],str) or not self.pending_action['request_id']: raise ValueError('invalid_pending_request_id')
            if self.pending_action['phase'] not in {'IDLE','INTERPRETING','RESOLVING','WAITING_FOR_ROLL','WAITING_FOR_TARGET','WAITING_FOR_CHOICE','WAITING_FOR_REACTION','WAITING_FOR_PUSH','WAITING_FOR_CONFIRMATION','WAITING_FOR_OPPOSED_ROLL','COMMITTING','NARRATING','COMPLETED','CANCELLED'}: raise ValueError('invalid_pending_phase')
            if not isinstance(self.pending_action['state_version'],int) or isinstance(self.pending_action['state_version'],bool) or self.pending_action['state_version']<0: raise ValueError('invalid_pending_state_version')
            if not isinstance(self.pending_action['payload'],dict): raise ValueError('invalid_pending_payload')
        if 'campaign_started' in self.discovery and not isinstance(self.discovery['campaign_started'],bool): raise ValueError('invalid_discovery_campaign_started')
        if 'locations' in self.discovery and not isinstance(self.discovery['locations'],dict): raise ValueError('invalid_discovery_locations')
        if isinstance(self.discovery.get('locations'),dict):
            for location_id,entry in self.discovery['locations'].items():
                if not isinstance(location_id,str) or not location_id or not isinstance(entry,dict): raise ValueError('invalid_discovery_location')
                visible=entry.get('visible_now',[])
                discovered=entry.get('discovered',{})
                if not isinstance(visible,(list,tuple)) or any(not isinstance(x,str) or not x for x in visible): raise ValueError('invalid_discovery_visible')
                if not isinstance(discovered,dict) or any(not isinstance(k,str) or not k or not isinstance(v,dict) for k,v in discovered.items()): raise ValueError('invalid_discovery_state')
        for rid,entry in self.request_log.items():
            if not isinstance(rid,str) or not rid or len(rid)>160: raise ValueError('invalid_request_id')
            if not isinstance(entry,dict) or set(entry)!={'fingerprint','result'}: raise ValueError('invalid_request_log_entry')
            if not isinstance(entry['fingerprint'],list) or not isinstance(entry['result'],dict): raise ValueError('invalid_request_log_entry')
            if '_compressed_result' in entry['result']:
                packed=entry['result']
                if set(packed)!={'_compressed_result','_result_sha256','_result_size','_tick'}: raise ValueError('invalid_request_log_entry')
                if not isinstance(packed['_compressed_result'],str) or len(packed['_compressed_result'])>8_000_000: raise ValueError('invalid_request_log_entry')
                if not isinstance(packed['_result_sha256'],str) or len(packed['_result_sha256'])!=64 or any(ch not in '0123456789abcdef' for ch in packed['_result_sha256']): raise ValueError('invalid_request_log_entry')
                for key in ('_result_size','_tick'):
                    if not isinstance(packed[key],int) or isinstance(packed[key],bool) or packed[key]<0: raise ValueError('invalid_request_log_entry')
                if packed['_result_size']>4*1024*1024:raise ValueError('invalid_request_log_entry')
        _validate_canonical_value(self.__dict__)
        return True
    def to_dict(self): self.validate(); return deepcopy(asdict(self))
    def to_json(self): return json.dumps(self.to_dict(),ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)
    @classmethod
    def from_dict(cls,data):
        if not isinstance(data,dict): raise ValueError('invalid_state_document')
        allowed={f.name for f in cls.__dataclass_fields__.values()}
        unknown=set(data)-allowed
        if unknown: raise ValueError('unknown_state_fields:'+','.join(sorted(unknown)))
        obj=cls(**deepcopy(data)); obj.validate(); return obj
    @classmethod
    def from_json(cls,raw):
        if not isinstance(raw,str) or len(raw)>MAX_STATE_JSON_CHARS:raise ValueError('invalid_state_json')
        try: data=json.loads(raw)
        except (TypeError,json.JSONDecodeError) as e: raise ValueError('invalid_state_json') from e
        return cls.from_dict(data)
