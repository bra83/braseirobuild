from copy import copy, deepcopy
from functools import wraps
import json
from threading import RLock

from .context import ContextEngine, WorldConsistencyGate
from .fallback import FallbackNarrator, ProviderFailurePolicy
from .intent import ActionClassifier, IntentCompiler
from .continuity import ContinuityGate
from .quality import NarrativeQualityGate
from .narrative import NarrativePolicy
from .rules import RulesKernel
from .audit import AuditTrail, MasterEvidenceProjection
from .audio import AudioDirector
from .ui import UiProjection
from .roll_resolution import SystemRollResolver


def _pipeline_synchronized(method):
    @wraps(method)
    def wrapped(self,*args,**kwargs):
        with self._lock:
            return method(self,*args,**kwargs)
    return wrapped


class ActionPipeline:
    """Pre-RAG command router plus resilience/continuity boundary for hosts."""

    def __init__(self, engine, classifier=None, intent_compiler=None, context_engine=None, consistency=None, continuity=None, fallback=None, failure_policy=None, quality=None, rules_kernel=None, audit=None, evidence_projection=None, audio_director=None, ui_projection=None, roll_resolver=None):
        self._lock = RLock()
        self.engine = engine
        self.classifier = classifier or ActionClassifier()
        self.intent_compiler = intent_compiler or IntentCompiler()
        self.context_engine = context_engine or ContextEngine()
        self.consistency = consistency or WorldConsistencyGate()
        self.continuity = continuity or ContinuityGate()
        self.fallback = fallback or FallbackNarrator()
        self.failure_policy = failure_policy or ProviderFailurePolicy()
        self.quality = quality or NarrativeQualityGate()
        self.narrative_policy = getattr(engine,'narrative',None) or NarrativePolicy()
        self.presentation_policy = getattr(engine,'presentation_policy',None)
        self.world_intro_validator = getattr(engine,'world_intro_validator',None)
        self.audit = audit or AuditTrail()
        self.evidence_projection = evidence_projection or MasterEvidenceProjection()
        self.audio_director = audio_director or AudioDirector()
        self.ui_projection = ui_projection or UiProjection()
        self.roll_resolver = roll_resolver or SystemRollResolver()
        modules = getattr(getattr(engine, 'adapters', None), 'modules', None)
        self.rules_kernel = rules_kernel or (RulesKernel(modules) if modules is not None else None)

    def _noop_result(self, state, text, request_id, classification):
        return {
            'tick': state.tick,
            'state_version': getattr(state, 'state_version', 0),
            'text': text,
            'request_id': request_id,
            'input_type': classification['input_type'],
            'mode': 'ui' if classification['input_type'] == ActionClassifier.UI_COMMAND else 'thought',
            'world_advanced': False,
            'presentation': deepcopy(classification['presentation']),
            'resolution': None,
            'event_ids': [],
            'phase': 'COMPLETED',
        }

    def _offline_copy(self):
        offline = copy(self.engine)
        offline.provider = None
        if hasattr(offline, '_request_bindings'):
            offline._request_bindings = deepcopy(getattr(self.engine, '_request_bindings', {}))
        return offline

    def _run_with_provider_fallback(self, operation, offline_operation):
        try:
            return operation(), None
        except Exception as exc:
            recovery = getattr(self.engine, 'recovery', None)
            if not self.failure_policy.is_fallback_safe(exc, recovery):
                raise
            return offline_operation(), str(exc).split(':', 1)[0] or exc.__class__.__name__

    def _persist_final_result(self, state, request_id, result):
        canonical=json.loads(json.dumps(result,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False))
        result.clear(); result.update(canonical)
        entry = state.request_log.get(request_id) if hasattr(state, 'request_log') else None
        if isinstance(entry, dict) and isinstance(entry.get('result'), dict):
            entry['result'] = deepcopy(result)
        bindings = getattr(self.engine, '_request_bindings', None)
        binding_key = getattr(self.engine, '_binding_key', None)
        if isinstance(bindings, dict) and callable(binding_key):
            key = binding_key(state, request_id)
            if key in bindings and isinstance(bindings[key], dict):
                bindings[key]['result'] = deepcopy(result)

    def _finalize_narration(self, state, text, request_id, result, importance, provider_failure=None):
        context = self.context_engine.build(state)
        result['canonical_context'] = deepcopy(context)
        narration = result.get('narration')
        reason = provider_failure or result.get('narrative_error')

        occasion=(result.get('turn_plan') or {}).get('story_obligation','continuation')
        obligation=None
        if self.presentation_policy is not None and result.get('mode','fiction')=='fiction':
            obligation=self.presentation_policy.build(state,occasion,context=context,evidence=[])

        if narration and not reason:
            try:
                self.consistency.validate(narration, context)
                self.consistency.validate_depth(
                    narration,
                    context,
                    mode=result.get('mode', 'fiction'),
                    importance=importance,
                    story_obligation=occasion,
                )
                self.continuity.validate(narration, state, claims=result.get('claims'), resolution=result.get('resolution'))
                self.narrative_policy.validate_story_obligation(narration,occasion)
                if getattr(self.engine,'strict_world_introductions',False) and self.world_intro_validator is not None and isinstance(obligation,dict):
                    self.world_intro_validator.validate(narration,obligation,presentation=result.get('presentation'),context=context,evidence=[],strict_structured=False)
                quality = self.quality.inspect(narration)
                result['narrative_quality'] = deepcopy(quality)
                if quality.get('errors'):
                    raise ValueError(quality['errors'][0])
                result['narration_source'] = 'provider'
            except ValueError as exc:
                reason = str(exc).split(':', 1)[0]

        if not narration or reason:
            result['narration'] = self.fallback.render(state, text, result, context, reason=reason or 'provider_missing')
            result['narration_source'] = 'deterministic_fallback'
            result['narrative_fallback_reason'] = reason or 'provider_missing'
            self.consistency.validate(result['narration'], context)
            self.continuity.validate(result['narration'], state, claims=result.get('claims'), resolution=result.get('resolution'))
            self.narrative_policy.validate_story_obligation(result['narration'],occasion)
            if getattr(self.engine,'strict_world_introductions',False) and self.world_intro_validator is not None and isinstance(obligation,dict):
                self.world_intro_validator.validate(result['narration'],obligation,presentation=result.get('presentation'),context=context,evidence=[],strict_structured=False)
            result['narrative_quality'] = deepcopy(self.quality.inspect(result['narration']))
            if result.get('mode') == 'fiction':
                self.consistency.validate_depth(result['narration'], context, mode='fiction', importance=importance, story_obligation=occasion)

        if result.get('mode')=='fiction' and occasion!='continuation' and result.get('narration'):
            marker=getattr(self.engine,'_mark_presentation_delivered',None)
            if callable(marker): marker(state,result.get('turn_plan') or {})
            visit_memory=getattr(self.engine,'visit_memory',None)
            if visit_memory is not None:
                result['presentation_receipt']=visit_memory.receipt(state,occasion)
        self._persist_final_result(state, request_id, result)
        return result

    def _attach_audit(self, state, request_id, result, intent=None, rules_plan=None):
        resolution=result.get('resolution') or {}
        refs=list((rules_plan or {}).get('rule_refs',[])) or list(resolution.get('rule_refs',[]))
        modifiers=list((rules_plan or {}).get('modifiers',[])) or list(resolution.get('modifiers',[]))
        metadata=resolution.get('metadata') or {}
        result['audit']=self.audit.record_resolution(
            intent=intent,rule_refs=refs,modifiers=modifiers,rng_trace=resolution.get('rng_trace'),
            effects=result.get('effects_applied'),event_ids=result.get('event_ids'),state_version=result.get('state_version'),
            retrieval=result.get('retrieval'),transaction_id=result.get('transaction_id'),resolution_id=result.get('resolution_id'),
            system_version=resolution.get('system_version') or metadata.get('system_version'),rules_revision=metadata.get('rules_revision'),
        )
        result['master_evidence']=self.evidence_projection.build(result)
        # UI/audio are projections over the already committed canonical state/events.
        # They can be discarded and rebuilt without changing campaign truth.
        event_ids=set(result.get('event_ids') or [])
        canonical_events=[deepcopy(e) for e in getattr(state,'event_log',[]) if isinstance(e,dict) and e.get('event_id') in event_ids]
        audio_context=self.context_engine.build(state)
        result['audio_commands']=self.audio_director.commands_for_events(canonical_events, context=audio_context)
        result['audio_scene_plan']=self.audio_director.scene_plan(canonical_events,context=audio_context,narration_active=False)
        result['narration_lifecycle']={
            'start_event':'master_narration_started','end_event':'master_narration_ended',
            'audio_ducking':True,'visual_effect_hook':'host_defined','canonical_state_affected':False,
        }
        modules=getattr(getattr(self.engine,'adapters',None),'modules',None)
        if modules is not None:
            result['ui_projection']=self.ui_projection.project(state,modules.get(state.system_id))
        self._persist_final_result(state,request_id,result)
        return result

    @_pipeline_synchronized
    def execute(self, state, text, request_id, mechanical=False, importance='normal', resolution=None, expected_state_version=None, input_channel=None, defer_narrative=False):
        validate_request=getattr(self.engine,'_validate_request_id',None)
        if callable(validate_request):validate_request(request_id)
        elif not isinstance(request_id,str) or not request_id or len(request_id)>160:raise ValueError('invalid_request_id')
        if not isinstance(text,str) or not text.strip() or len(text)>getattr(self.engine,'MAX_INPUT',20000):
            raise ValueError('invalid_turn_text')
        state.validate()
        validate_version=getattr(self.engine,'_validate_expected_state_version',None)
        if callable(validate_version):validate_version(state,expected_state_version)
        if not isinstance(defer_narrative,bool): raise ValueError('invalid_defer_narrative')
        classification = self.classifier.classify(text, input_channel=input_channel)
        intent = self.intent_compiler.compile(text, classification)
        input_type = classification['input_type']

        if input_type in {ActionClassifier.UI_COMMAND, ActionClassifier.CHARACTER_THOUGHT}:
            result=self._noop_result(state, text, request_id, classification); result['intent']=deepcopy(intent); return result

        canonical_context = self.context_engine.build(state)
        rules_plan = self.rules_kernel.plan(state, intent, canonical_context) if self.rules_kernel is not None else None
        if resolution is not None:
            # A host may submit a mechanical result only while resuming an action that
            # the Barbara already placed in a WAITING_* state. Initial outcomes/targets
            # are never accepted as authority at the command boundary.
            raise ValueError('resolution_requires_pending_action')
        trusted_input_resolution = None
        if isinstance(rules_plan, dict):
            requirement = rules_plan.get('requirement') or 'no_roll'
            effective_mechanical = bool(input_type in {ActionClassifier.GAME_ACTION,ActionClassifier.DIALOGUE} and requirement != 'no_roll')
            if effective_mechanical:
                trusted_input_resolution = {
                    'source': 'rules_kernel',
                    'requirement': requirement,
                    'action': rules_plan.get('action') or intent.get('action'),
                    'mechanic': rules_plan.get('mechanic') or rules_plan.get('action') or intent.get('action'),
                    'modifiers': deepcopy(rules_plan.get('modifiers', [])),
                    'rule_refs': deepcopy(rules_plan.get('rule_refs', [])),
                    'metadata': {
                        'roll_definition': deepcopy(rules_plan.get('roll')),
                        'time_cost': deepcopy(rules_plan.get('time_cost')),
                        'rule_source': rules_plan.get('rule_source') or rules_plan.get('source'),
                        'system_version': rules_plan.get('system_version'),
                        'rules_revision': rules_plan.get('rules_revision'),
                    },
                }
        else:
            # Minimal compatibility for isolated/fake engines used by host tests. Real
            # BarbaraEngine instances always expose SystemModules/RulesKernel.
            effective_mechanical = bool(mechanical and classification['mechanics_allowed'])
        kwargs = dict(
            mechanical=effective_mechanical,
            importance=importance,
            resolution=trusted_input_resolution,
            expected_state_version=expected_state_version,
        )
        # Preserve compatibility with third-party/fake engines implementing the 1.0.1
        # signature. New channel/fast-scene kwargs are sent only when requested.
        if input_channel is not None: kwargs['input_channel']=input_channel
        if defer_narrative: kwargs['defer_narrative']=True
        offline = self._offline_copy()
        result, provider_failure = self._run_with_provider_fallback(
            lambda: self.engine.turn(state, text, request_id, **kwargs),
            lambda: offline.turn(state, text, request_id, **kwargs),
        )
        result['input_type'] = input_type
        result['input_channel'] = input_channel
        result['narrative_deferred'] = bool(defer_narrative)
        result['input_classification'] = deepcopy(classification)
        result['intent'] = deepcopy(intent)
        result['rules_plan'] = deepcopy(rules_plan)
        result=self._finalize_narration(state, text, request_id, result, importance, provider_failure)
        return self._attach_audit(state,request_id,result,intent,rules_plan)

    @_pipeline_synchronized
    def resume(self, state, action_id, request_id, resolution, expected_state_version=None, input_channel=None, defer_narrative=False):
        validate_request=getattr(self.engine,'_validate_request_id',None)
        if callable(validate_request):validate_request(request_id)
        elif not isinstance(request_id,str) or not request_id or len(request_id)>160:raise ValueError('invalid_request_id')
        state.validate()
        if not isinstance(action_id, str) or not action_id:
            raise ValueError('invalid_action_id')
        if resolution is None:
            raise ValueError('missing_resume_resolution')
        entry=getattr(state,'request_log',{}).get(request_id)
        if isinstance(entry,dict) and isinstance(entry.get('result'),dict):
            saved=self.engine._stored_result(entry)
            if saved.get('resumed_action_id')!=action_id:
                raise ValueError('request_id_collision')
            recorded=saved.get('resume_submission')
            if recorded is not None and recorded!=resolution:
                raise ValueError('request_id_collision')
            return deepcopy(saved)
        pending = deepcopy(getattr(state, 'pending_action', None) or {})
        text = str((pending.get('payload') or {}).get('text') or 'Continuar a ação pendente.')
        importance = str((pending.get('payload') or {}).get('importance') or 'normal')
        pending_channel=(pending.get('payload') or {}).get('input_channel')
        if input_channel is not None and pending_channel is not None and input_channel!=pending_channel: raise ValueError('pending_input_channel_mismatch')
        input_channel=pending_channel if pending_channel is not None else input_channel
        if pending.get('action_id') != action_id:
            raise ValueError('pending_action_mismatch')
        if not isinstance(resolution,dict):
            raise ValueError('invalid_resume_resolution')
        # Host/UI submits observations only. Difficulty, effects, costs, rule refs and
        # bindings come from the pending Barbara-authorized requirement envelope.
        allowed_submission={'source','outcome','result','degree','successes','tags','roll','total','rolls','rng_trace','details','resolution_id','requirement'}
        forbidden=set(resolution)-allowed_submission
        if forbidden:
            raise ValueError('host_resolution_authority_violation')
        if 'requirement' in resolution and resolution['requirement']!='resolved':
            raise ValueError('host_resolution_authority_violation')
        authority=deepcopy((pending.get('payload') or {}).get('requirement_envelope') or {})
        metadata=deepcopy(authority.get('metadata') or {})
        submitted_source=resolution.get('source','host')
        authoritative_roll=None
        if pending.get('phase')=='WAITING_FOR_ROLL' and isinstance(metadata.get('roll_definition'),dict):
            authoritative_roll=self.roll_resolver.resolve_observation(authority,resolution)
        if authoritative_roll is not None:
            trusted_resolution=deepcopy(authoritative_roll)
            trusted_resolution['source']='adapter'
            trusted_resolution['metadata']={**deepcopy(trusted_resolution.get('metadata') or {}),'submission_source':submitted_source,'outcome_authority':'system_module'}
        else:
            trusted_resolution={k:deepcopy(v) for k,v in resolution.items() if k not in {'source','resolution_id','requirement'}}
            trusted_resolution.update({
                'source':'adapter', 'requirement':'resolved',
                'action':authority.get('action'), 'mechanic':authority.get('mechanic'),
                'modifiers':deepcopy(authority.get('modifiers',[])),
                'rule_refs':deepcopy(authority.get('rule_refs',[])),
                'metadata':{**metadata,'submission_source':submitted_source,'outcome_authority':'adapter_compatibility'},
            })
        trusted_resolution={k:v for k,v in trusted_resolution.items() if v is not None}
        authoritative_effects=deepcopy(authority.get('effects',[]))
        time_cost=metadata.get('time_cost')
        if isinstance(time_cost,dict) and set(time_cost)>={'unit','amount'}:
            authoritative_effects.append({'type':'time_advance','unit':time_cost['unit'],'amount':time_cost['amount']})
        if authoritative_effects: trusted_resolution['effects']=authoritative_effects
        offline = self._offline_copy()
        result, provider_failure = self._run_with_provider_fallback(
            lambda: self.engine.resume_action(
                state,
                action_id,
                request_id,
                deepcopy(trusted_resolution),
                expected_state_version=expected_state_version,
                input_channel=input_channel,
                defer_narrative=defer_narrative,
            ),
            lambda: offline.resume_action(
                state,
                action_id,
                request_id,
                deepcopy(trusted_resolution),
                expected_state_version=expected_state_version,
                input_channel=input_channel,
                defer_narrative=defer_narrative,
            ),
        )
        result['input_type'] = 'mechanical_resume'
        result['input_channel'] = input_channel
        result['narrative_deferred'] = bool(defer_narrative)
        result['resume_submission'] = deepcopy(resolution)
        result=self._finalize_narration(state, text, request_id, result, importance, provider_failure)
        pending_env=(pending.get('payload') or {}).get('requirement_envelope') or {}
        synthetic_intent={'action':pending_env.get('action'),'intent_type':'mechanical_resume'}
        return self._attach_audit(state,request_id,result,synthetic_intent,pending_env)
    @_pipeline_synchronized
    def roll_pending(self, state, action_id, request_id, *, expected_state_version=None, random_source=None):
        """Roll a pending Barbara-authorized RollRequest and resume it atomically.

        No target/pool/die information is accepted from the UI. All variables and
        interpretation data must already be bound into the pending SystemModule roll
        contract from canonical state/context.
        """
        pending=deepcopy(getattr(state,'pending_action',None) or {})
        if pending.get('action_id')!=action_id:
            raise ValueError('pending_action_mismatch')
        if pending.get('phase')!='WAITING_FOR_ROLL':
            raise ValueError('pending_action_not_roll')
        authority=deepcopy((pending.get('payload') or {}).get('requirement_envelope') or {})
        resolved=self.roll_resolver.roll_authoritatively(authority,random_source=random_source)
        submission={k:deepcopy(v) for k,v in resolved.items() if k in {'outcome','result','degree','successes','tags','roll','total','rolls','rng_trace','details'}}
        submission['source']='dice'; submission['requirement']='resolved'
        return self.resume(state,action_id,request_id,submission,expected_state_version=expected_state_version)

    @_pipeline_synchronized
    def accept_external_narrative(self, state, text, request_id, result, candidate, importance='normal'):
        """Validate a Kotlin/local-model narrative after canonical mechanics committed.

        This is intentionally presentation-only. A candidate cannot contain patches or
        effects; failure leaves the deterministic fallback already stored in `result`.
        """
        validate_request=getattr(self.engine,'_validate_request_id',None)
        if callable(validate_request):validate_request(request_id)
        elif not isinstance(request_id,str) or not request_id or len(request_id)>160:raise ValueError('invalid_request_id')
        state.validate()
        if not isinstance(text,str) or not text.strip():
            raise ValueError('invalid_turn_text')
        if not isinstance(result, dict):
            raise ValueError('invalid_turn_result')
        entry=state.request_log.get(request_id)
        if not isinstance(entry,dict) or not isinstance(entry.get('result'),dict):
            raise ValueError('unknown_narrative_request')
        canonical=self.engine._stored_result(entry)
        if state.state_version!=canonical.get('state_version'):
            raise ValueError('stale_narrative_result')
        for field in ('command_id','state_version','transaction_id','phase','text'):
            if result.get(field)!=canonical.get(field):
                raise ValueError('narrative_result_mismatch')
        if result.get('command_id')!=request_id or text!=canonical.get('text'):
            raise ValueError('narrative_result_mismatch')
        if result!=canonical:
            raise ValueError('narrative_result_mismatch')
        result=deepcopy(canonical)
        presentation={}
        if isinstance(candidate, str):
            narration=candidate; claims=[]
        elif isinstance(candidate, dict):
            if set(candidate)-{'narration','claims','presentation_hints','presentation'}:
                raise ValueError('invalid_narrative_candidate_fields')
            narration=candidate.get('narration'); claims=candidate.get('claims',[])
            presentation=deepcopy(candidate.get('presentation') or candidate.get('presentation_hints') or {})
            if claims not in (None,[]):
                # External claims need an evidence-bound grounding round; until that
                # contract is supplied, do not silently canonize or trust them.
                raise ValueError('external_claims_require_grounding')
        else:
            raise ValueError('invalid_narrative_candidate')
        if not isinstance(narration,str) or not narration.strip():
            raise ValueError('invalid_narration')
        self.engine.narrative.validate_player_agency(text,narration)
        self.engine.mechanics.validate_narration(narration,bool((result.get('turn_plan') or {}).get('check_required')),result.get('resolution'))
        context=self.context_engine.build(state)
        self.consistency.validate(narration,context)
        occasion=(result.get('turn_plan') or {}).get('story_obligation','continuation')
        strict_intro=bool(getattr(self.engine,'strict_world_introductions',False))
        # Raw legacy external narrators predate the 1.1.1 presentation contract.  Keep
        # that low-level API source-compatible; real Android/platform/Supervisor paths
        # opt into strict introductions explicitly.
        depth_occasion=occasion if strict_intro else 'continuation'
        self.consistency.validate_depth(narration,context,mode=result.get('mode','fiction'),importance=importance,story_obligation=depth_occasion)
        if strict_intro:
            self.narrative_policy.validate_story_obligation(narration,occasion,strict=True)
        if strict_intro and self.presentation_policy is not None and self.world_intro_validator is not None:
            obligation=self.presentation_policy.build(state,occasion,context=context,evidence=[])
            self.world_intro_validator.validate(narration,obligation,presentation=presentation,context=context,evidence=[],strict_structured=False)
        self.continuity.validate(narration,state,claims=[],resolution=result.get('resolution'))
        quality=self.quality.inspect(narration)
        if quality.get('errors'):
            raise ValueError(quality['errors'][0])
        updated=deepcopy(result)
        updated['narration']=narration
        updated['claims']=[]
        updated['presentation']=presentation
        updated['narration_source']='external_validated'
        updated['narrative_quality']=deepcopy(quality)
        updated.pop('narrative_fallback_reason',None)
        if result.get('mode')=='fiction' and occasion!='continuation':
            marker=getattr(self.engine,'_mark_presentation_delivered',None)
            if callable(marker): marker(state,result.get('turn_plan') or {})
            visit_memory=getattr(self.engine,'visit_memory',None)
            if visit_memory is not None: updated['presentation_receipt']=visit_memory.receipt(state,occasion)
        # Only presentation-memory changes here; mechanics/world truth remain untouched.
        return updated
