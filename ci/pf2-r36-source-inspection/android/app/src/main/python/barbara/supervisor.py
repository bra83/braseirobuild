from __future__ import annotations

from copy import deepcopy

from .context import WorldConsistencyGate
from .continuity import ContinuityGate
from .fallback import FallbackNarrator
from .host_contract import HostIntegrationConformance, HostIntegrationContract
from .quality import NarrativeQualityGate
from .state import CampaignState


class SupervisorBridge:
    """Presentation/continuity boundary for full external RPG applications.

    It never mutates host-owned canonical domains. Existing Braseiro projects can
    keep their own Room/SQLite state, atlas, PDFs, travel rules and living world,
    while using Barbara to build context, validate resolutions and gate narrative.
    """

    FORBIDDEN_CANDIDATE_FIELDS = frozenset({'state_patch', 'effects', 'events', 'commands', 'canonical_state'})

    def __init__(self, engine, contract: HostIntegrationContract):
        self.engine = engine
        self.contract = contract
        HostIntegrationConformance().validate(contract)
        if contract.owner('canonical_state') != 'host':
            raise ValueError('supervisor_requires_host_canonical_state')
        self.consistency = WorldConsistencyGate()
        self.continuity = ContinuityGate()
        self.quality = NarrativeQualityGate()
        self.fallback = FallbackNarrator()

    def _state(self, state_json: str) -> CampaignState:
        state = CampaignState.from_json(state_json)
        HostIntegrationConformance().validate(self.contract, system_id=state.system_id)
        return state

    def inspect(self, state_json: str, text: str, *, importance='normal', input_channel=None) -> dict:
        state = self._state(state_json)
        if not isinstance(text, str) or not text.strip():
            raise ValueError('invalid_turn_text')
        plan = self.engine.narrative.turn_plan(text, False, importance, input_channel=input_channel)
        plan = self.engine.narrative.apply_story_obligation(plan, self.engine._story_occasion(state, plan))
        context = self.engine.context_engine.build(state)
        profile = self.engine._adapter(state).narrator_profile(self.engine.rag, state.campaign_id)
        obligation=self.engine.presentation_policy.build(state,plan.get('story_obligation','continuation'),context=context,evidence=[])
        return {
            'protocol_version': self.contract.protocol_version,
            'system_id': state.system_id,
            'system_version': self.contract.system_version,
            'state_version': state.state_version,
            'canonical_context': deepcopy(context),
            'turn_plan': deepcopy(plan),
            'world_introduction': deepcopy(obligation),
            'input_channel': input_channel,
            'system_profile': deepcopy(profile),
            'authority': deepcopy(dict(self.contract.authority)),
            'datasets': deepcopy(dict(self.contract.datasets)),
        }

    def validate_resolution(self, state_json: str, resolution: dict | None):
        state = self._state(state_json)
        if resolution is None:
            return None
        # Validation only: no effects are ever applied to the host projection here.
        return self.engine._adapter(state).validate_resolution(self.engine.mechanics.validate_resolution(deepcopy(resolution)))

    def finalize_narrative(self, state_json: str, text: str, request_id: str, *, resolution=None, candidate=None, importance='normal', input_channel=None) -> dict:
        state = self._state(state_json)
        before = state.to_json()
        if not isinstance(request_id, str) or not request_id or len(request_id) > 160:
            raise ValueError('invalid_request_id')
        package = candidate if isinstance(candidate, dict) else {'narration': candidate}
        if not isinstance(package, dict):
            raise ValueError('invalid_narrative_candidate')
        forbidden = set(package) & self.FORBIDDEN_CANDIDATE_FIELDS
        if forbidden:
            raise ValueError('narrative_candidate_authority_violation')
        unknown = set(package) - {'narration', 'claims', 'presentation'}
        if unknown:
            raise ValueError('unknown_narrative_candidate_fields:' + ','.join(sorted(unknown)))
        trusted_resolution = self.validate_resolution(state_json, resolution)
        inspection = self.inspect(state_json, text, importance=importance, input_channel=input_channel)
        context = inspection['canonical_context']
        narration = package.get('narration')
        claims = deepcopy(package.get('claims'))
        reason = None
        if narration:
            try:
                self.consistency.validate(narration, context)
                self.consistency.validate_depth(
                    narration, context, mode=inspection['turn_plan'].get('mode', 'fiction'), importance=importance,
                    story_obligation=inspection['turn_plan'].get('story_obligation', 'continuation'),
                )
                self.continuity.validate(narration, state, claims=claims, resolution=trusted_resolution)
                self.engine.narrative.validate_story_obligation(narration, inspection['turn_plan'].get('story_obligation', 'continuation'))
                self.engine.world_intro_validator.validate(narration,inspection['world_introduction'],presentation=package.get('presentation'),context=context,evidence=[],strict_structured=False)
                quality = self.quality.inspect(narration)
                if quality.get('errors'):
                    raise ValueError(quality['errors'][0])
            except ValueError as exc:
                reason = str(exc).split(':', 1)[0]
        else:
            reason = 'provider_missing'
        result = {
            'request_id': request_id,
            'state_version': state.state_version,
            'mode': inspection['turn_plan'].get('mode', 'fiction'),
            'importance': importance,
            'turn_plan': deepcopy(inspection['turn_plan']),
            'canonical_context': deepcopy(context),
            'resolution': deepcopy(trusted_resolution),
            'claims': claims,
            'presentation': deepcopy(package.get('presentation') or {}),
        }
        if reason:
            result['narration'] = self.fallback.render(state, text, result, context, reason=reason)
            result['narration_source'] = 'deterministic_fallback'
            result['narrative_fallback_reason'] = reason
        else:
            result['narration'] = str(narration)
            result['narration_source'] = 'host_provider'
        result['presentation_receipt']=self.engine.visit_memory.receipt(state,inspection['turn_plan'].get('story_obligation','continuation'))
        result['presentation_commit_required']=bool(inspection['turn_plan'].get('story_obligation')!='continuation' and result.get('mode')=='fiction')
        # Postcondition: supervisor mode is incapable of changing host truth.
        if state.to_json() != before:
            raise RuntimeError('supervisor_mutated_host_state')
        return result

    def validate_poi_layers(self, *, canonical=(), campaign=(), procedural=(), gm=False):
        return self.engine.poi_projection.visible(canonical,campaign,procedural,gm=gm)

    def create_poi_command(self, **kwargs):
        return self.engine.poi_commands.create(**kwargs)

    def update_poi_command(self, poi, *, patch, tick, reason, actor=None):
        return self.engine.poi_commands.update(poi,patch=patch,tick=tick,reason=reason,actor=actor)

    def discover_poi_command(self, poi, *, tick, method='exploration'):
        return self.engine.poi_commands.discover(poi,tick=tick,method=method)

    def propose_poi_command(self, **kwargs):
        return self.engine.poi_commands.proposal(**kwargs)

    def propose_procedural_pois(self, *, context, templates, trigger, seed_hint=None):
        return self.engine.poi_generation.propose(context=context,templates=templates,trigger=trigger,seed_hint=seed_hint)

