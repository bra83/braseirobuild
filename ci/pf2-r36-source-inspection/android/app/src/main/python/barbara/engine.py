from copy import deepcopy
import base64
from functools import wraps
import hashlib,json,re
from threading import RLock
import zlib
from .rag import RAG
from .rules import RuleGate
from .world import WorldTick
from .memory import Memory
from .security import public_view
from .recovery import RecoveryPolicy
from .narrative import NarrativePolicy
from .knowledge import KnowledgeBoundary, LoreDisclosureGate
from .grounding import ClaimGrounding
from .telemetry import Telemetry
from .adapters import AdapterRegistry
from .mechanics import MechanicsAuthority
from .effects import EffectResolver
from .action_state import ActionStateMachine
from .context import ContextEngine
from .director import SceneDirector, NarrativeDirector
from .knowledge_engine import KnowledgeIngestionPipeline, IngestionCheckpointStore, DocumentIngestor
from .procedural import EncounterEngine
from .numeric import is_finite_number
from .presentation import NarrativePresentationPolicy, VisitMemory, WorldIntroductionValidator
from .poi import DynamicPoiContract, PoiCommandBuilder, AtlasPoiProjection, PoiGenerationPolicy


def _synchronized(method):
    @wraps(method)
    def wrapped(self,*args,**kwargs):
        with self._lock:
            return method(self,*args,**kwargs)
    return wrapped

class BarbaraEngine:
    MAX_INPUT=20000
    MAX_NARRATION=20000
    MAX_REQUEST_LOG=1000
    MAX_REQUEST_BINDINGS=2000
    FULL_REQUEST_RESULTS=32
    MAX_STORED_RESULT_BYTES=4*1024*1024
    _RETRIEVAL_HINTS={'combat':'combat attack ataque ataco golpe strike fight damage dano defense defesa','travel':'travel journey viagem viajo road estrada trail trilha movement movimento','investigation':'investigation investigate investigação exam examine examino search procura clue pista perception percepção','dialogue':'dialogue social conversa persuasion persuasão reaction reação influence influência','action':'action ação check teste skill perícia ability habilidade','meta':'rule regra rules regras mechanics mecânica'}
    def __init__(self,provider=None,recovery=None,narrative=None,knowledge=None,grounding=None,rag=None,rag_db_path=None,embedder=None,telemetry=None,adapters=None,mechanics=None,effects=None,action_state=None,context_engine=None,disclosure=None,scene_director=None,narrative_director=None,document_ingestor=None,ingestion_checkpoints=None,encounter_engine=None,presentation_policy=None,visit_memory=None,world_intro_validator=None,poi_contract=None,poi_commands=None,poi_projection=None,poi_generation=None,strict_world_introductions=None):
        if rag is not None and rag_db_path is not None:raise ValueError('rag_configuration_conflict')
        self._lock=RLock(); self._closed=False; self._owns_rag=rag is None; self._owns_checkpoints=ingestion_checkpoints is None
        self.provider=provider; self.rag=rag if rag is not None else RAG(rag_db_path); self.rules=RuleGate(); self.world=WorldTick(); self.memory=Memory(); self.recovery=recovery or RecoveryPolicy(); self.narrative=narrative or NarrativePolicy(); self.knowledge=knowledge or KnowledgeBoundary(); self.grounding=grounding or ClaimGrounding(); self.embedder=embedder; self.telemetry=telemetry or Telemetry(); self.adapters=adapters or AdapterRegistry(); self.mechanics=mechanics or MechanicsAuthority(); self.effects=effects or EffectResolver(); self.action_state=action_state or ActionStateMachine(); self.context_engine=context_engine or ContextEngine(); self.disclosure=disclosure or LoreDisclosureGate(); self.scene_director=scene_director or SceneDirector(); self.narrative_director=narrative_director or NarrativeDirector()
        checkpoint_store=ingestion_checkpoints or IngestionCheckpointStore(rag_db_path or getattr(self.rag,'db_path',None))
        self.ingestion=KnowledgeIngestionPipeline(self.rag,ingestor=document_ingestor or DocumentIngestor(),checkpoints=checkpoint_store,embedder=self.embedder); self.encounters=encounter_engine or EncounterEngine(); self.presentation_policy=presentation_policy or NarrativePresentationPolicy(); self.visit_memory=visit_memory or VisitMemory(); self.world_intro_validator=world_intro_validator or WorldIntroductionValidator(); self.poi_contract=poi_contract or DynamicPoiContract(); self.poi_commands=poi_commands or PoiCommandBuilder(self.poi_contract); self.poi_projection=poi_projection or AtlasPoiProjection(self.poi_contract); self.poi_generation=poi_generation or PoiGenerationPolicy(); self.strict_world_introductions=(bool(getattr(provider,'enforce_world_introduction_contract',False) or getattr(provider,'supports_presentation_coverage',False)) if strict_world_introductions is None else bool(strict_world_introductions)); self._request_bindings={}

    @_synchronized
    def close(self):
        if self._closed:return
        if self._owns_checkpoints:self.ingestion.checkpoints.close()
        if self._owns_rag:self.rag.close()
        self._request_bindings.clear(); self._closed=True

    def _ensure_open(self):
        if self._closed:raise RuntimeError('barbara_engine_closed')

    @_synchronized
    def ingest_pages(self,pages,*,source_id,system_id,campaign_id,kind='RULE',authority=1.0,secret=False,**metadata):
        self._ensure_open()
        return self.ingestion.ingest_pages(pages,source_id=source_id,system_id=system_id,campaign_id=campaign_id,kind=kind,authority=authority,secret=secret,**metadata)

    @_synchronized
    def ingest_pdf(self,source,*,source_id,system_id,campaign_id,kind='RULE',authority=1.0,secret=False,**metadata):
        self._ensure_open()
        return self.ingestion.ingest_pdf(source,source_id=source_id,system_id=system_id,campaign_id=campaign_id,kind=kind,authority=authority,secret=secret,**metadata)

    @_synchronized
    def schedule_encounter(self,state,request_id,*,seed,table,expected_state_version=None,due_tick=None):
        """Deterministically schedule an encounter as canonical structured state.

        This API never calls a generative provider and never creates encounter prose.
        The encounter fact is persisted first and can later be narrated from events.
        """
        self._ensure_open(); self._validate_request_id(request_id); state.validate(); self._validate_expected_state_version(state,expected_state_version)
        seed=self.encounters._validate_seed(seed); validated_table=self.encounters._validate_table(table)
        fingerprint=[state.campaign_id,state.system_id,'encounter',str(seed),hashlib.sha256(json.dumps(validated_table,ensure_ascii=False,sort_keys=True,allow_nan=False).encode()).hexdigest(),due_tick]
        if request_id in state.request_log:
            entry=state.request_log[request_id]
            if entry['fingerprint']!=fingerprint: raise ValueError('request_id_collision')
            return self._stored_result(entry)
        if any(event.get('request_id')==request_id for event in state.event_log if isinstance(event,dict)):
            raise ValueError('request_id_history_expired')
        if state.pending_action: raise ValueError('pending_action_must_be_resumed')
        context=self.context_engine.build(state); site=state.sites.get(state.location,{}) if state.location else {}
        danger=(site.get('danger') if isinstance(site,dict) else None)
        if not isinstance(danger,(int,float)) or isinstance(danger,bool): danger=state.world_flags.get('danger',0.5)
        context={**deepcopy(context),'danger':danger,'tick':state.tick}
        planned=self.encounters.plan_event(seed,context,validated_table,event_id=f'{request_id}:encounter',due_tick=due_tick,origin=state.location or None)
        draft=state.snapshot(); next_version=state.state_version+1; transaction_id=f'{request_id}:tx:{next_version}'; draft.state_version=next_version
        event=deepcopy(planned.get('event'))
        if event is not None: draft.events.append(event)
        payload={'encounter_id':planned['result'].get('encounter'),'chance':planned['result'].get('chance'),'roll':planned['result'].get('roll'),'rng_trace':deepcopy(planned['result'].get('rng_trace')),'scheduled_event':deepcopy(event),'transaction_id':transaction_id}
        log_id=f'{request_id}:encounter-check'; draft.event_log.append({'event_id':log_id,'type':'encounter_check','request_id':request_id,'tick':draft.tick,'state_version':next_version,'payload':payload})
        result={'tick':draft.tick,'state_version':next_version,'command_id':request_id,'transaction_id':transaction_id,'event_ids':[log_id],'encounter':planned['result'].get('encounter'),'scheduled':event is not None,'rng_trace':deepcopy(planned['result'].get('rng_trace')),'phase':'COMPLETED','mode':'world'}
        self._remember_request(draft,request_id,fingerprint,result); draft.validate(); self._commit_draft(state,draft); self._bind_request(state,request_id,fingerprint,result)
        self.telemetry.record('encounter','scheduled' if event is not None else 'none',campaign=state.campaign_id,system=state.system_id)
        return deepcopy(result)

    def _fingerprint(self,state,text,mechanical,importance,resolution,input_channel=None,defer_narrative=False):return [state.campaign_id,state.system_id,text,mechanical,importance,deepcopy(resolution),input_channel,bool(defer_narrative)]
    def _validate_request_id(self,request_id):
        if not isinstance(request_id,str) or not request_id or len(request_id)>160:raise ValueError('invalid_request_id')
    def _validate_expected_state_version(self,state,expected_state_version):
        if expected_state_version is None:return
        if not isinstance(expected_state_version,int) or isinstance(expected_state_version,bool) or expected_state_version<0:raise ValueError('invalid_expected_state_version')
        if expected_state_version!=state.state_version:raise ValueError('state_version_conflict')
    def _binding_key(self,state,request_id):return (state.campaign_id,request_id)
    def _check_binding(self,state,request_id,fingerprint):
        entry=self._request_bindings.get(self._binding_key(state,request_id))
        if entry is None:return None
        if entry['fingerprint']!=fingerprint:raise ValueError('request_id_collision')
        if request_id not in state.request_log:raise ValueError('request_id_collision')
        return deepcopy(entry['result'])
    def _bind_request(self,state,request_id,fingerprint,result):
        self._request_bindings[self._binding_key(state,request_id)]={'fingerprint':deepcopy(fingerprint),'result':deepcopy(result)}
        while len(self._request_bindings)>self.MAX_REQUEST_BINDINGS:self._request_bindings.pop(next(iter(self._request_bindings)))
    def _pack_result(self,result):
        raw=json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False).encode()
        if len(raw)>self.MAX_STORED_RESULT_BYTES:raise ValueError('request_result_too_large')
        return {
            '_compressed_result':base64.b64encode(zlib.compress(raw,9)).decode('ascii'),
            '_result_sha256':hashlib.sha256(raw).hexdigest(),
            '_result_size':len(raw),
            '_tick':int(result.get('tick',0)),
        }
    def _stored_result(self,entry):
        result=entry.get('result') if isinstance(entry,dict) else None
        if not isinstance(result,dict):raise ValueError('invalid_request_log_entry')
        if '_compressed_result' not in result:return deepcopy(result)
        if set(result)!={'_compressed_result','_result_sha256','_result_size','_tick'}:raise ValueError('corrupt_request_result')
        size=result.get('_result_size')
        if not isinstance(size,int) or isinstance(size,bool) or not 0<=size<=self.MAX_STORED_RESULT_BYTES:raise ValueError('corrupt_request_result')
        try:
            compressed=base64.b64decode(result['_compressed_result'],validate=True)
            decoder=zlib.decompressobj(); raw=decoder.decompress(compressed,self.MAX_STORED_RESULT_BYTES+1)
            if decoder.unconsumed_tail or len(raw)>self.MAX_STORED_RESULT_BYTES:raise ValueError
            raw+=decoder.flush(self.MAX_STORED_RESULT_BYTES+1-len(raw))
            if not decoder.eof or decoder.unused_data or decoder.unconsumed_tail or len(raw)!=size or len(raw)>self.MAX_STORED_RESULT_BYTES:raise ValueError
            if hashlib.sha256(raw).hexdigest()!=result.get('_result_sha256'):raise ValueError
            decoded=json.loads(raw)
        except (TypeError,ValueError,zlib.error,json.JSONDecodeError) as exc:raise ValueError('corrupt_request_result') from exc
        if not isinstance(decoded,dict):raise ValueError('corrupt_request_result')
        return decoded
    def _result_tick(self,entry):
        result=entry.get('result',{}) if isinstance(entry,dict) else {}
        return int(result.get('_tick',result.get('tick',0))) if isinstance(result,dict) else 0
    def _compact_request_results(self,state,current_request_id):
        full=[rid for rid,entry in state.request_log.items() if isinstance(entry.get('result'),dict) and '_compressed_result' not in entry['result']]
        while len(full)>self.FULL_REQUEST_RESULTS:
            candidates=[rid for rid in full if rid!=current_request_id] or full
            victim=min(candidates,key=lambda rid:(self._result_tick(state.request_log[rid]),rid))
            state.request_log[victim]['result']=self._pack_result(state.request_log[victim]['result'])
            full.remove(victim)
    def _retrieval_query(self,text,plan,rule_required=False,state=None):
        q=str(text)
        if rule_required:
            hint=self._RETRIEVAL_HINTS.get(plan.get('kind'),'')
            if hint:q=f'{q} {hint}'
        if state is not None and plan.get('mode')=='fiction':
            intro_hint=self.presentation_policy.retrieval_terms(state,plan.get('story_obligation','continuation'))
            if intro_hint:q=f'{q} {intro_hint}'
        return q
    def _query_vector(self,text):
        if self.embedder is None:return None
        fn=getattr(self.embedder,'embed',None)
        if not callable(fn):raise ValueError('invalid_embedder')
        vector=fn(text)
        if not isinstance(vector,(list,tuple)) or not vector or not all(is_finite_number(x) for x in vector):raise ValueError('invalid_query_vector')
        return tuple(float(x) for x in vector)
    def _retrieval_payload(self,bundle):
        if bundle is None:return None
        return {
            'confidence':float(bundle.confidence),
            'insufficient_evidence':bool(bundle.insufficient_evidence),
            'conflicts':[deepcopy(c.__dict__) for c in bundle.conflicts],
            'evidence':[
                {
                    'chunk_id':e.chunk_id,'source_id':e.source_id,'page':e.page,'section':e.section,
                    'semantic_score':e.semantic_score,'lexical_score':e.lexical_score,
                    'final_score':e.final_score,'canonicality':e.canonicality,'priority':e.priority,
                }
                for e in bundle.evidence
            ],
        }

    def _error_code(self,exc):
        msg=str(exc).split(':',1)[0]; return msg if re.fullmatch(r'[A-Za-z0-9_\-]{1,80}',msg or '') else exc.__class__.__name__
    def _adapter(self,state):
        try:adapter=self.adapters.get(state.system_id)
        except KeyError as exc:raise ValueError(f'unsupported_system:{state.system_id}') from exc
        adapter.validate_campaign(state); return adapter
    def _system_profile(self,state):return self._adapter(state).narrator_profile(self.rag,state.campaign_id)
    def _public_world_context(self,state):
        site=deepcopy(state.sites.get(state.location,{})) if state.location else {}; ledger=[deepcopy(e) for e in state.public_ledger[-20:] if isinstance(e,dict) and (e.get('origin') in {None,state.location} or state.location=='')]; return {'site':public_view(site),'ledger':public_view(ledger)}
    def _world_imprint(self,state):
        world=self._public_world_context(state); data={'location':state.location,'site':world['site'],'ledger':world['ledger'],'weather':public_view(state.weather),'economy':public_view(state.economy)}
        raw=json.dumps(data,ensure_ascii=False,sort_keys=True,separators=(',',':'),default=str).encode(); return hashlib.sha256(raw).hexdigest()
    def _story_occasion(self,state,plan):
        if plan.get('mode')!='fiction':return 'continuation'
        discovery=state.discovery; locations=discovery.get('locations',{})
        # Opening presentation is tracked separately from the mere fact that world
        # time advanced. A failed/deferred narration cannot consume the introduction.
        if not discovery.get('campaign_introduction_delivered',False):return 'campaign_opening'
        if not state.location:return 'continuation'
        previous=locations.get(state.location)
        if not isinstance(previous,dict) or not previous.get('introduction_delivered'):
            region_id=self.presentation_policy.region_id(state)
            region_entry=(discovery.get('regions') or {}).get(region_id) if region_id else None
            if region_id and (not isinstance(region_entry,dict) or not region_entry.get('introduction_delivered')):
                return 'first_visit_region'
            return self.presentation_policy.classify_first_visit(state)
        if self.visit_memory.long_absence(state):return 'long_absence_return'
        if previous.get('imprint')!=self._world_imprint(state):return 'changed_return'
        return 'continuation'
    def _mark_discovery(self,state,plan):
        if plan.get('mode')!='fiction':return
        # Visiting and successfully *presenting* a place are different facts.
        state.discovery['campaign_started']=True
        if not state.location:return
        self.visit_memory.mark_visit(state,imprint=self._world_imprint(state),region_id=self.presentation_policy.region_id(state))
    def _mark_presentation_delivered(self,state,plan):
        if plan.get('mode')!='fiction':return False
        occasion=plan.get('story_obligation','continuation')
        if occasion=='continuation':return False
        self.visit_memory.mark_introduction_delivered(state,occasion)
        return True
    def narrator_context(self,state,evidence,text='',importance='normal',turn_plan=None,resolution=None):
        allowed=self.disclosure.filter_evidence(state,evidence,self.knowledge)
        safe=[{'source_id':e.source_id,'kind':e.kind,'text':e.text,'checksum':e.checksum} for e in allowed if not e.secret]
        qcount=self.narrative.question_count(text); world=self._public_world_context(state); memories=self.memory.compact_context(state,query=text,location=state.location); occasion=(turn_plan or {}).get('story_obligation','continuation'); scene_context=self.context_engine.build(state)
        scene=self.scene_director.scene_state(state,(turn_plan or {}).get('kind','action'),str(occasion).upper()) if str(occasion).upper() in self.scene_director.OCCASIONS else self.scene_director.scene_state(state,(turn_plan or {}).get('kind','action'),'CONTINUATION')
        obligation=self.presentation_policy.build(state,occasion,context=scene_context,evidence=allowed)
        policy=self.narrative.narrator_directives(importance,qcount,turn_plan); policy['world_introduction']=deepcopy(obligation)
        narrative_input=self.narrative_director.build_input(state,text=text,context=scene_context,resolution=resolution,evidence=safe,policy=policy,scene=scene)
        return public_view({'location':state.location,'scene_context':scene_context,'scene':scene,'narrative_input':narrative_input,'facts':state.facts,'memory':memories,'rumors':self.world.visible_rumors(state),'npcs':self.knowledge.visible_npcs(state),'site':world['site'],'public_ledger':world['ledger'],'world_state_for_dramatization':{'weather':state.weather,'economy':state.economy},'world_experience':{'occasion':occasion,'player_setting_familiarity':obligation['player_setting_familiarity'],'player_has_preexisting_local_knowledge':occasion=='continuation','introduction_obligation':deepcopy(obligation),'instruction':'Transform current world state into perceivable fiction. Geography in scene_context is canonical. Explain unfamiliar setting terms naturally when familiarity is ZERO. Never invent unsupported lore or spatial facts.'},'evidence':safe,'resolution':deepcopy(resolution),'system_profile':self._system_profile(state),'narrative_policy':policy})
    def _validate_provider_output(self,out,state,evidence,context,user_text,importance='normal',plan=None,resolution=None):
        legacy_string=isinstance(out,str); legacy_opt_out=bool(legacy_string and getattr(self.provider,'legacy_text',False))
        if legacy_string:out={'narration':out,'claims':[]}
        if not isinstance(out,dict):raise ValueError('invalid_provider_output')
        if set(out)-{'narration','claims','state_patch','presentation'}:raise ValueError('unknown_provider_field')
        narration=out.get('narration'); claims=out.get('claims',[]); patches=out.get('state_patch',[]); presentation=deepcopy(out.get('presentation') or {})
        if patches not in (None,[]):raise ValueError('provider_state_patch_forbidden')
        if not isinstance(narration,str) or not narration.strip() or len(narration)>self.MAX_NARRATION:raise ValueError('invalid_narration')
        if len(narration)<self.narrative.minimum_acceptable_chars(importance):raise ValueError('narrativa_resumida_demais')
        self.narrative.validate_player_agency(user_text,narration); self.mechanics.validate_narration(narration,bool(plan and plan.get('check_required')),resolution)
        if not legacy_opt_out:
            self.narrative.validate_response_coverage(user_text,narration); self.narrative.validate_scene_ending(narration,importance)
            if getattr(self.provider,'enforce_story_contract',False):self.narrative.validate_story_obligation(narration,(plan or {}).get('story_obligation','continuation'),strict=self.strict_world_introductions)
            obligation=(context.get('world_experience') or {}).get('introduction_obligation') if isinstance(context,dict) else None
            if isinstance(obligation,dict) and self.strict_world_introductions:
                self.world_intro_validator.validate(narration,obligation,presentation=presentation,context=context.get('scene_context',{}),evidence=evidence,strict_structured=bool(getattr(self.provider,'supports_presentation_coverage',False)))
        claims=self.grounding.validate(claims,state,evidence,self.world.visible_rumors(state),public_context=context)
        return {'narration':narration,'claims':deepcopy(claims),'presentation':presentation}
    def _remember_request(self,state,request_id,fingerprint,result):
        state.request_log[request_id]={'fingerprint':deepcopy(fingerprint),'result':deepcopy(result)}
        self._compact_request_results(state,request_id)
        if len(state.request_log)>self.MAX_REQUEST_LOG:
            victim=min(state.request_log,key=lambda rid:(self._result_tick(state.request_log[rid]),rid))
            if victim!=request_id:state.request_log.pop(victim,None)
            else:
                others=[rid for rid in state.request_log if rid!=request_id]
                if others:state.request_log.pop(min(others,key=lambda rid:(self._result_tick(state.request_log[rid]),rid)),None)
    def _append_turn_event(self,state,request_id,state_version,text,plan,resolution,transaction_id=None):
        payload={'mode':plan['mode'],'world_advanced':bool(plan['world_advances']),'text':text}
        if transaction_id: payload['transaction_id']=transaction_id
        if isinstance(resolution,dict):
            payload['outcome']=resolution.get('outcome'); payload['resolution_id']=resolution.get('resolution_id')
            payload['mechanic']=resolution.get('mechanic'); payload['action']=resolution.get('action')
            payload['rule_refs']=deepcopy(resolution.get('rule_refs',[]))
            payload['roll']=resolution.get('roll'); payload['total']=resolution.get('total')
            payload['target']=resolution.get('target'); payload['margin']=resolution.get('margin')
            payload['successes']=resolution.get('successes'); payload['rng_trace']=deepcopy(resolution.get('rng_trace'))
            meta=resolution.get('metadata') or {}
            payload['rules_revision']=meta.get('rules_revision'); payload['system_version']=resolution.get('system_version') or meta.get('system_version')
        event={'event_id':f'{request_id}:turn','type':'turn_committed','request_id':request_id,'tick':state.tick,'state_version':state_version,'payload':payload}
        state.event_log.append(event); return event['event_id']
    def _append_wait_event(self,state,request_id,state_version,pending,transaction_id=None):
        payload={'action_id':pending['action_id'],'phase':pending['phase']}
        if transaction_id: payload['transaction_id']=transaction_id
        event={'event_id':f'{request_id}:wait','type':'action_waiting','request_id':request_id,'tick':state.tick,'state_version':state_version,'payload':payload}
        state.event_log.append(event); return event['event_id']
    def _commit_draft(self,state,draft):
        draft.validate(); state.__dict__.clear(); state.__dict__.update(deepcopy(draft.__dict__)); state.validate()
    def _migrate_draft_module(self,draft,adapter,request_id,state_version,transaction_id):
        module=getattr(adapter,'module',None)
        if module is None:return []
        target=str(module.version); current=str(getattr(draft,'system_module_version','') or '')
        if current==target:return []
        if current:
            migrated=self.adapters.modules.migrate_document(draft.to_dict(),draft.system_id,current,target)
            migrated['system_module_version']=target
            restored=draft.__class__.from_dict(migrated)
            draft.__dict__.clear(); draft.__dict__.update(deepcopy(restored.__dict__))
            event_type='system_module_migrated'
        else:
            draft.system_module_version=target
            event_type='system_module_bound'
        history={'from':(current or None),'to':target,'request_id':request_id,'state_version':state_version}
        draft.migration_history.append(history)
        event_id=f'{request_id}:module-migration'
        draft.event_log.append({
            'event_id':event_id,'type':event_type,'request_id':request_id,'tick':draft.tick,
            'state_version':state_version,'payload':{**deepcopy(history),'transaction_id':transaction_id},
        })
        return [event_id]

    def _waiting_phase(self,requirement):
        return {
            'roll_required':'WAITING_FOR_ROLL',
            'target_required':'WAITING_FOR_TARGET',
            'choice_required':'WAITING_FOR_CHOICE',
            'reaction_required':'WAITING_FOR_REACTION',
            'push_required':'WAITING_FOR_PUSH',
            'confirmation_required':'WAITING_FOR_CONFIRMATION',
            'opposed_roll':'WAITING_FOR_OPPOSED_ROLL',
        }.get(requirement)
    def _bind_resolution_identity(self,adapter,resolution,request_id):
        if resolution is None:return None
        out=adapter.validate_resolution(self.mechanics.validate_resolution(resolution))
        if out is None:return None
        out=deepcopy(out)
        out.setdefault('resolution_id',f'{request_id}:resolution')
        out.setdefault('command_id',request_id)
        if getattr(adapter,'module',None) is not None:
            out.setdefault('system_version',str(adapter.module.version))
        return self.mechanics.validate_resolution(out)

    def _persist_result_after_commit(self,state,request_id,fingerprint,result):
        entry=state.request_log.get(request_id)
        if not isinstance(entry,dict):
            self._remember_request(state,request_id,fingerprint,result)
        else:
            entry['fingerprint']=deepcopy(fingerprint); entry['result']=deepcopy(result)
        self._bind_request(state,request_id,fingerprint,result)

    def _render_committed_narrative(self,state,text,request_id,importance,plan,evidence,resolution,fingerprint,result):
        """Presentation phase. Canonical state is already committed before this runs."""
        if not self.provider:
            self._persist_result_after_commit(state,request_id,fingerprint,result)
            return result
        context=self.narrator_context(state,evidence,text,importance,plan,resolution)
        result['narrative_input']=json.loads(json.dumps(context.get('narrative_input'),ensure_ascii=False,sort_keys=True,default=str))
        try:
            provider_state=state.snapshot()
            raw=self.recovery.run(lambda:self.provider.generate(text,context,provider_state))
            validated=self._validate_provider_output(raw,state,evidence,context,text,importance,plan,resolution)
            result.update(validated)
            self._mark_presentation_delivered(state,plan)
            result['presentation_receipt']=self.visit_memory.receipt(state,plan.get('story_obligation','continuation'))
            result.pop('narrative_error',None)
        except Exception as exc:
            # Narrative failure is presentation-only. Persist the committed result before
            # surfacing the presentation error; ActionPipeline can then replace it with
            # deterministic fallback without reapplying mechanics or world time.
            result.pop('narration',None); result.pop('claims',None)
            result['narrative_error']=self._error_code(exc)
            self.telemetry.record('narrative_reject',result['narrative_error'],campaign=state.campaign_id,system=state.system_id,mode=plan['mode'])
            self._persist_result_after_commit(state,request_id,fingerprint,result)
            raise
        self._persist_result_after_commit(state,request_id,fingerprint,result)
        return result

    def _wait_for_requirement(self,state,draft,text,request_id,importance,plan,evidence,profile,fingerprint,requirement='roll_required',requirement_envelope=None,retrieval_bundle=None,input_channel=None,defer_narrative=False):
        phase=self._waiting_phase(requirement)
        if phase is None:raise ValueError('invalid_wait_requirement')
        next_version=state.state_version+1; transaction_id=f'{request_id}:tx:{next_version}'; draft.state_version=next_version
        migration_event_ids=self._migrate_draft_module(draft,self._adapter(draft),request_id,next_version,transaction_id)
        pending=self.action_state.begin_wait(draft,request_id,phase,{'text':text,'importance':importance,'mechanical':True,'system_id':state.system_id,'turn_plan':deepcopy(plan),'evidence':[e.checksum for e in evidence],'requirement':requirement,'requirement_envelope':deepcopy(requirement_envelope),'input_channel':input_channel,'defer_narrative':bool(defer_narrative)})
        event_id=self._append_wait_event(draft,request_id,next_version,pending,transaction_id)
        result={'tick':draft.tick,'state_version':next_version,'evidence':[e.checksum for e in evidence],'text':text,'mode':plan['mode'],'world_advanced':False,'importance':importance,'system_profile':profile,'turn_plan':deepcopy(plan),'presentation':deepcopy(plan['channels']),'retrieval':self._retrieval_payload(retrieval_bundle),'resolution':deepcopy(requirement_envelope),'requirement':requirement,'effects_applied':[],'event_ids':migration_event_ids+[event_id],'phase':phase,'pending_action':deepcopy(pending),'command_id':request_id,'transaction_id':transaction_id,'resolution_id':(requirement_envelope or {}).get('resolution_id') if isinstance(requirement_envelope,dict) else None}
        # Persist the interruptible state before asking any generative provider to describe it.
        self._remember_request(draft,request_id,fingerprint,result); draft.validate(); self._commit_draft(state,draft)
        if defer_narrative:
            result['narrative_deferred']=True
            self._persist_result_after_commit(state,request_id,fingerprint,result)
        else:
            result=self._render_committed_narrative(state,text,request_id,importance,plan,evidence,requirement_envelope,fingerprint,result)
        self.telemetry.record('turn','waiting',campaign=state.campaign_id,system=state.system_id,mode=plan['mode'],phase=phase); return deepcopy(result)

    def _wait_for_resolution(self,state,draft,text,request_id,importance,plan,evidence,profile,fingerprint,retrieval_bundle=None,input_channel=None,defer_narrative=False):
        return self._wait_for_requirement(state,draft,text,request_id,importance,plan,evidence,profile,fingerprint,'roll_required',None,retrieval_bundle,input_channel=input_channel,defer_narrative=defer_narrative)
    @_synchronized
    def turn(self,state,text,request_id,mechanical=False,importance='normal',resolution=None,expected_state_version=None,_pending_authority=False,input_channel=None,defer_narrative=False):
        self._ensure_open(); self._validate_request_id(request_id)
        if not isinstance(text,str) or not text.strip() or len(text)>self.MAX_INPUT:raise ValueError('invalid_turn_text')
        state.validate(); adapter=self._adapter(state); base_plan=self.narrative.turn_plan(text,mechanical,importance,input_channel=input_channel); inferred=self.mechanics.requires_rule(text,mechanical,base_plan); effective_mechanical=bool(mechanical or (inferred and base_plan['mode']=='fiction')); plan=self.narrative.turn_plan(text,effective_mechanical,importance,input_channel=input_channel); plan=self.narrative.apply_story_obligation(plan,self._story_occasion(state,plan)); trusted_resolution=self._bind_resolution_identity(adapter,resolution,request_id); profile=adapter.narrator_profile(self.rag,state.campaign_id); fingerprint=self._fingerprint(state,text,effective_mechanical,importance,trusted_resolution,input_channel=input_channel,defer_narrative=defer_narrative)
        bound=self._check_binding(state,request_id,fingerprint)
        if bound is not None:self.telemetry.record('turn','idempotent',campaign=state.campaign_id,system=state.system_id); return bound
        if request_id in state.request_log:
            entry=state.request_log[request_id]
            if entry['fingerprint']!=fingerprint:raise ValueError('request_id_collision')
            saved=self._stored_result(entry); self._bind_request(state,request_id,fingerprint,saved); self.telemetry.record('turn','idempotent',campaign=state.campaign_id,system=state.system_id); return saved
        if any(event.get('request_id')==request_id for event in state.event_log if isinstance(event,dict)):
            raise ValueError('request_id_history_expired')
        self._validate_expected_state_version(state,expected_state_version)
        if state.pending_action:raise ValueError('pending_action_must_be_resumed')
        try:
            compiled_rule_authority=bool(isinstance(trusted_resolution,dict) and trusted_resolution.get('rule_refs') and (trusted_resolution.get('source')=='rules_kernel' or _pending_authority))
            # Compiled SystemModule rules are the mechanical authority. RAG remains
            # evidence/audit support and is required only when a mechanical request
            # reaches the engine without an authoritative compiled rule binding.
            gate_required=bool((effective_mechanical and not compiled_rule_authority) or (self.provider is not None and inferred and plan['mode']=='meta'))
            intro_required=plan.get('story_obligation','continuation')!='continuation'
            retrieval_kinds={'RULE','LORE','MEMORY'} | ({'LOCATION','EVENT'} if intro_required else set())
            retrieval_limit=12 if intro_required else 6
            retrieval_query=self._retrieval_query(text,plan,bool(effective_mechanical or gate_required),state=state); query_vector=self._query_vector(retrieval_query); evidence=self.rag.retrieve(retrieval_query,state.campaign_id,state.system_id,kinds=retrieval_kinds,allow_secret=False,limit=retrieval_limit,query_vector=query_vector); self.rules.require(gate_required,evidence)
            retrieval_bundle=self.rag.retrieve_bundle(retrieval_query,state.campaign_id,state.system_id,kinds=retrieval_kinds,allow_secret=False,limit=retrieval_limit,query_vector=query_vector) if hasattr(self.rag,'retrieve_bundle') else None
            draft=state.snapshot()
            unresolved_requirement=trusted_resolution.get('requirement') if isinstance(trusted_resolution,dict) else None
            if self._waiting_phase(unresolved_requirement):
                return self._wait_for_requirement(state,draft,text,request_id,importance,plan,evidence,profile,fingerprint,unresolved_requirement,trusted_resolution,retrieval_bundle,input_channel=input_channel,defer_narrative=defer_narrative)
            if plan['check_required'] and trusted_resolution is None:
                return self._wait_for_resolution(state,draft,text,request_id,importance,plan,evidence,profile,fingerprint,retrieval_bundle,input_channel=input_channel,defer_narrative=defer_narrative)
            next_version=state.state_version+1; transaction_id=f'{request_id}:tx:{next_version}'
            self._migrate_draft_module(draft,adapter,request_id,next_version,transaction_id)
            draft.state_version=next_version
            if plan['world_advances']:self.world.advance(draft,request_id=request_id,state_version=next_version,transaction_id=transaction_id)
            resolution_effects=trusted_resolution.get('effects',[]) if isinstance(trusted_resolution,dict) else []
            applied_effects=self.effects.apply(draft,resolution_effects,request_id,next_version,transaction_id=transaction_id)
            self._mark_discovery(draft,plan); draft.state_version=next_version; self._append_turn_event(draft,request_id,next_version,text,plan,trusted_resolution,transaction_id); draft.validate()
            event_ids=[e['event_id'] for e in draft.event_log if e.get('request_id')==request_id]
            context=self.narrator_context(draft,evidence,text,importance,plan,trusted_resolution); result={'tick':draft.tick,'state_version':next_version,'evidence':[e.checksum for e in evidence],'retrieval':self._retrieval_payload(retrieval_bundle),'narrative_input':json.loads(json.dumps(context.get('narrative_input'),ensure_ascii=False,sort_keys=True,default=str)),'text':text,'mode':plan['mode'],'world_advanced':plan['world_advances'],'importance':importance,'system_profile':profile,'turn_plan':deepcopy(plan),'presentation':deepcopy(plan['channels']),'resolution':deepcopy(trusted_resolution),'effects_applied':deepcopy(applied_effects),'event_ids':event_ids,'phase':'COMPLETED','command_id':request_id,'transaction_id':transaction_id,'resolution_id':trusted_resolution.get('resolution_id') if isinstance(trusted_resolution,dict) else None}
            # Transaction boundary: commit state + event log + idempotency record before narration.
            self._remember_request(draft,request_id,fingerprint,result); draft.validate(); self._commit_draft(state,draft)
            if defer_narrative:
                result['narrative_deferred']=True
                self._persist_result_after_commit(state,request_id,fingerprint,result)
            else:
                result=self._render_committed_narrative(state,text,request_id,importance,plan,evidence,trusted_resolution,fingerprint,result)
        except Exception as exc:
            self.telemetry.record('reject',self._error_code(exc),campaign=state.campaign_id,system=state.system_id); raise
        self.telemetry.record('turn','ok',campaign=state.campaign_id,system=state.system_id,mode=result['mode']); return deepcopy(result)
    @_synchronized
    def resume_action(self,state,action_id,request_id,resolution,expected_state_version=None,input_channel=None,defer_narrative=False):
        self._ensure_open(); self._validate_request_id(request_id); state.validate(); self._validate_expected_state_version(state,expected_state_version)
        if request_id in state.request_log:
            saved=self._stored_result(state.request_log[request_id])
            if saved.get('resolution')!=resolution:raise ValueError('request_id_collision')
            return deepcopy(saved)
        if any(event.get('request_id')==request_id for event in state.event_log if isinstance(event,dict)):
            raise ValueError('request_id_history_expired')
        pending=self.action_state.resume(state,action_id); payload=deepcopy(pending['payload'])
        if payload.get('system_id')!=state.system_id:raise ValueError('pending_system_mismatch')
        draft=state.snapshot(); self.action_state.clear(draft,action_id)
        result=self.turn(draft,payload['text'],request_id,mechanical=True,importance=payload.get('importance','normal'),resolution=deepcopy(resolution),expected_state_version=state.state_version,_pending_authority=bool((payload.get('requirement_envelope') or {}).get('rule_refs')),input_channel=(payload.get('input_channel') if input_channel is None else input_channel),defer_narrative=defer_narrative)
        result['resumed_action_id']=action_id; result['phase']='COMPLETED'; self._commit_draft(state,draft); return deepcopy(result)
