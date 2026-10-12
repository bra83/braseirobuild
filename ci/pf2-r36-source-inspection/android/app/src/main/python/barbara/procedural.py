from __future__ import annotations
import hashlib
import json
import random
from copy import deepcopy

from .state import _validate_canonical_value


class TerrainGenerator:
    def generate(self,seed,q,r,biomes=('plains','forest','hills','water')):
        if (not isinstance(seed,(str,int)) or isinstance(seed,bool) or isinstance(seed,str) and (not seed or len(seed)>512) or isinstance(seed,int) and not -(2**63)<=seed<2**63):raise ValueError('invalid_procedural_seed')
        raw=f'{seed}:{q}:{r}'.encode(); n=int(hashlib.sha256(raw).hexdigest()[:15],16)
        # Canonical replay generation; this RNG never protects secrets.
        rng=random.Random(n)  # nosec B311
        return {'q':q,'r':r,'biome':biomes[rng.randrange(len(biomes))],'seed':n}


class EncounterEngine:
    """Deterministic encounter selection from canonical context.

    The engine returns structured facts/events only. It never writes encounter prose;
    narration can happen later from the committed event.
    """
    MAX_ROWS=1000

    def _validate_seed(self,seed):
        if not isinstance(seed,(str,int)) or isinstance(seed,bool):raise ValueError('invalid_encounter_seed')
        if isinstance(seed,str) and (not seed or len(seed)>512):raise ValueError('invalid_encounter_seed')
        if isinstance(seed,int) and not -(2**63)<=seed<2**63:raise ValueError('invalid_encounter_seed')
        return seed

    def _validate_table(self,table):
        if not isinstance(table,list) or not table or len(table)>self.MAX_ROWS: raise ValueError('invalid_encounter_table')
        try:_validate_canonical_value(table)
        except ValueError as exc:raise ValueError('invalid_encounter_table') from exc
        out=[]
        for row in table:
            if not isinstance(row,dict) or not isinstance(row.get('id'),str) or not row['id']: raise ValueError('invalid_encounter_row')
            weight=row.get('weight',1)
            if not isinstance(weight,(int,float)) or isinstance(weight,bool) or weight<=0: raise ValueError('invalid_encounter_weight')
            terrain=row.get('terrain')
            if terrain is not None and (not isinstance(terrain,(list,tuple,set)) or not all(isinstance(x,str) and x for x in terrain)): raise ValueError('invalid_encounter_terrain')
            out.append(deepcopy(row))
        return out

    def resolve(self,seed,context,table):
        seed=self._validate_seed(seed)
        table=self._validate_table(table)
        if not isinstance(context,dict): raise ValueError('invalid_encounter_context')
        danger=float(context.get('danger',0.5)); terrain=str(context.get('terrain',''))
        if not 0<=danger<=1: raise ValueError('invalid_encounter_danger')
        tick=int(context.get('tick',0)); material={'seed':str(seed),'terrain':terrain,'tick':tick,'danger':danger}
        raw=json.dumps(material,sort_keys=True,separators=(',',':')).encode(); seed_int=int(hashlib.sha256(raw).hexdigest()[:15],16)
        # Deterministic encounter replay; this RNG never protects secrets.
        rng=random.Random(seed_int)  # nosec B311
        chance=min(.95,max(.0,danger)); roll=rng.random()
        trace={'algorithm':'sha256+python_random_v1','seed_material':material,'seed_int':seed_int,'chance_roll':roll,'chance':chance}
        if roll>chance:return {'encounter':None,'roll':roll,'chance':chance,'rng_trace':trace}
        eligible=[x for x in table if not x.get('terrain') or terrain in x.get('terrain',[])]
        if not eligible: eligible=table
        total=sum(float(x.get('weight',1)) for x in eligible); selection_roll=rng.random(); pick=selection_roll*total; acc=0
        trace.update({'selection_roll':selection_roll,'eligible_ids':[x['id'] for x in eligible],'total_weight':total})
        for row in eligible:
            acc+=float(row.get('weight',1))
            if pick<=acc:
                return {'encounter':row['id'],'roll':roll,'chance':chance,'disposition':row.get('disposition','neutral'),'rng_trace':trace,'row':deepcopy(row)}
        row=eligible[-1]
        return {'encounter':row['id'],'roll':roll,'chance':chance,'disposition':row.get('disposition','neutral'),'rng_trace':trace,'row':deepcopy(row)}

    def plan_event(self,seed,context,table,*,event_id,due_tick=None,origin=None,visibility='public'):
        if not isinstance(event_id,str) or not event_id: raise ValueError('invalid_encounter_event_id')
        resolved=self.resolve(seed,context,table)
        result={k:deepcopy(v) for k,v in resolved.items() if k!='row'}
        if resolved['encounter'] is None:
            return {'result':result,'event':None}
        row=resolved['row']
        tick=int(context.get('tick',0)); due=tick if due_tick is None else due_tick
        if not isinstance(due,int) or isinstance(due,bool) or due<tick: raise ValueError('invalid_encounter_due_tick')
        event={
            'id':event_id,'kind':'encounter','due_tick':due,'origin':origin or context.get('location_id'),
            'visibility':visibility,'encounter_id':resolved['encounter'],'disposition':resolved.get('disposition','neutral'),
            'rng_trace':deepcopy(resolved['rng_trace']),'payload':{
                'encounter_id':resolved['encounter'],'disposition':resolved.get('disposition','neutral'),
                'tags':deepcopy(row.get('tags',[])),'actors':deepcopy(row.get('actors',[])),
            },
        }
        # Data-defined canonical consequences are allowed; prose is not synthesized here.
        for key in ('site_changes','spawn_events'):
            if key in row:event[key]=deepcopy(row[key])
        return {'result':result,'event':event}
