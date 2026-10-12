import hashlib,json
from copy import deepcopy

from .effects import EffectResolver
from .world import WorldTick
from .state import _validate_canonical_value


class ReplayHarness:
    VOLATILE_EVENT_KEYS={'id'}
    def _semantic(self,value,*,strip_event_ids=False):
        if isinstance(value,dict):
            return {
                k:self._semantic(v,strip_event_ids=strip_event_ids)
                for k,v in value.items()
                if not (strip_event_ids and k in self.VOLATILE_EVENT_KEYS)
            }
        if isinstance(value,list):return [self._semantic(v,strip_event_ids=strip_event_ids) for v in value]
        return value
    def digest(self,state):
        state.validate()
        data=deepcopy(state.__dict__)
        data['events']=self._semantic(data.get('events',[]),strip_event_ids=True)
        raw=json.dumps(data,sort_keys=True,ensure_ascii=False,allow_nan=False,separators=(',',':')).encode()
        return hashlib.sha256(raw).hexdigest()
    def mechanical_digest(self,state):
        state.validate()
        data=deepcopy(state.__dict__)
        for key in ('event_log','request_log','pending_action','migration_history','discovery'):
            data.pop(key,None)
        data['events']=self._semantic(data.get('events',[]),strip_event_ids=True)
        raw=json.dumps(data,sort_keys=True,ensure_ascii=False,allow_nan=False,separators=(',',':')).encode()
        return hashlib.sha256(raw).hexdigest()
    def event_digest(self,event_log):
        _validate_canonical_value(event_log)
        raw=json.dumps(self._semantic(deepcopy(event_log),strip_event_ids=True),sort_keys=True,ensure_ascii=False,allow_nan=False,separators=(',',':')).encode()
        return hashlib.sha256(raw).hexdigest()
    def mechanical_transcript(self,state):
        out=[]
        for event in state.event_log:
            if not isinstance(event,dict):continue
            payload=event.get('payload') or {}
            if event.get('type') in {'turn_committed','world_advanced','world_event_resolved','encounter_check'} or event.get('type') in EffectResolver._SET_EFFECTS or event.get('type') in {'resource_delta','condition_add','condition_remove','damage','actor_move','item_move','item_consume','clock_advance','time_advance','light_source_ignite','light_source_extinguish'}:
                out.append({'event_id':event.get('event_id'),'type':event.get('type'),'tick':event.get('tick'),'state_version':event.get('state_version'),'payload':deepcopy(payload)})
        return out
    def validate_event_log(self,state):
        seen=set(); previous_version=-1; previous_tick=-1
        for event in state.event_log:
            if not isinstance(event,dict): raise ValueError('invalid_event_log_entry')
            event_id=event.get('event_id')
            if not isinstance(event_id,str) or not event_id: raise ValueError('invalid_event_id')
            if event_id in seen: raise ValueError('duplicate_event_id')
            seen.add(event_id)
            version=event.get('state_version'); tick=event.get('tick')
            if not isinstance(version,int) or isinstance(version,bool) or version<0: raise ValueError('invalid_event_state_version')
            if not isinstance(tick,int) or isinstance(tick,bool) or tick<0: raise ValueError('invalid_event_tick')
            if version<previous_version: raise ValueError('event_state_version_regression')
            if tick<previous_tick: raise ValueError('event_tick_regression')
            previous_version=max(previous_version,version); previous_tick=max(previous_tick,tick)
        return {'events':len(seen),'digest':self.event_digest(state.event_log),'valid':True}
    def run(self,engine,state,turns,checkpoint_every=None):
        if checkpoint_every is not None and (not isinstance(checkpoint_every,int) or isinstance(checkpoint_every,bool) or checkpoint_every<1):raise ValueError('invalid_checkpoint_interval')
        checkpoints=[]
        for i,t in enumerate(turns):
            if isinstance(t,dict):
                engine.turn(state,t['text'],request_id=t.get('request_id',f'replay-{i}'),mechanical=t.get('mechanical',False),importance=t.get('importance','normal'),resolution=deepcopy(t.get('resolution')))
            else:engine.turn(state,t,request_id=f'replay-{i}')
            if checkpoint_every and (i+1)%checkpoint_every==0:checkpoints.append({'turn':i+1,'tick':state.tick,'digest':self.digest(state)})
        result={'digest':self.digest(state),'tick':state.tick,'checkpoints':checkpoints}
        return result if checkpoint_every else result['digest']
    def compare(self,engine_factory,state_factory,turns):
        a_state=state_factory(); b_state=state_factory(); a=self.run(engine_factory(),a_state,turns,checkpoint_every=1); b=self.run(engine_factory(),b_state,turns,checkpoint_every=1)
        return {'equal':a==b,'left':a,'right':b}
    def replay_events(self,initial_state,event_log,*,world=None,effects=None,migrations=None):
        """Reconstruct deterministic mechanical projections from canonical events.

        Narrative and retrieval projections are intentionally ignored. World advances
        and typed effects are replayed in event order. The caller supplies the same
        initial campaign snapshot used before the log.
        """
        state=deepcopy(initial_state); state.event_log=[]; state.request_log={}; state.pending_action={}
        world=world or WorldTick(); effects=effects or EffectResolver()
        effect_types=set(getattr(effects,'_SET_EFFECTS',{})) | {'location_set','resource_delta','condition_add','condition_remove','damage','actor_move','item_move','item_consume','clock_advance','time_advance','light_source_ignite','light_source_extinguish'}
        for event in event_log:
            if not isinstance(event,dict): raise ValueError('invalid_event_log_entry')
            typ=event.get('type'); payload=deepcopy(event.get('payload') or {})
            if typ=='system_module_bound':
                state.system_module_version=str(payload.get('to') or state.system_module_version)
                state.migration_history.append({k:deepcopy(payload.get(k)) for k in ('from','to','request_id','state_version') if k in payload})
            elif typ=='system_module_migrated':
                source=str(payload.get('from') or state.system_module_version or '')
                target=str(payload.get('to') or '')
                if not source or not target:
                    raise ValueError('invalid_migration_event')
                if migrations is None:
                    raise ValueError('migration_registry_required_for_replay')
                migrated=migrations.migrate(state.to_dict(),state.system_id,source,target)
                migrated['system_module_version']=target
                restored=state.__class__.from_dict(migrated)
                state.__dict__.clear(); state.__dict__.update(deepcopy(restored.__dict__))
                history={k:deepcopy(payload.get(k)) for k in ('from','to','request_id','state_version') if k in payload}
                if not state.migration_history or state.migration_history[-1]!=history:
                    state.migration_history.append(history)
            elif typ=='world_advanced':
                amount=int(payload.get('amount',1)); world.advance(state,amount=amount)
            elif typ in effect_types:
                effect={k:v for k,v in payload.items() if k!='transaction_id'}; effects.apply_replay(state,effect)
            elif typ=='encounter_check':
                scheduled=payload.get('scheduled_event')
                if isinstance(scheduled,dict) and not any(isinstance(e,dict) and e.get('id')==scheduled.get('id') for e in state.events): state.events.append(deepcopy(scheduled))
            elif typ=='turn_committed':
                pass
            state.state_version=max(state.state_version,int(event.get('state_version',state.state_version)))
        state.validate(); return state
