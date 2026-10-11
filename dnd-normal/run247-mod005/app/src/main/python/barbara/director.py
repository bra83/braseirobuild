from __future__ import annotations

from copy import deepcopy


class SceneDirector:
    OCCASIONS = {
        'FIRST_ARRIVAL','FIRST_VISIT_SETTLEMENT','FIRST_VISIT_REGION','FIRST_VISIT_LANDMARK',
        'ENTER_NEW_GEOGRAPHIC_AREA','LONG_ABSENCE_RETURN','MAJOR_POLITICAL_TRANSITION',
        'RETURN_UNCHANGED','RETURN_CHANGED','CHANGED_RETURN','AFTERMATH','DISCOVERY',
        'ESCALATION','REUNION','INTERRUPTION','TRANSITION','CONTINUATION','CAMPAIGN_OPENING'
    }

    def classify_occasion(self, *, previous=None, current=None, campaign_started=True, discovered=False, interrupted=False):
        if not campaign_started:
            return 'CAMPAIGN_OPENING'
        if interrupted:
            return 'INTERRUPTION'
        if previous is None and current is not None:
            return 'FIRST_ARRIVAL'
        if discovered:
            return 'DISCOVERY'
        if previous is not None and current is not None:
            return 'RETURN_UNCHANGED' if previous == current else 'RETURN_CHANGED'
        return 'CONTINUATION'

    def scene_state(self, state, kind='action', occasion='CONTINUATION'):
        if occasion not in self.OCCASIONS:
            raise ValueError('invalid_scene_occasion')
        mode = 'combat' if kind == 'combat' else ('dialogue' if kind == 'dialogue' else ('investigation' if kind == 'investigation' else 'exploration'))
        return {'mode': mode, 'occasion': occasion, 'location': state.location, 'tick': state.tick, 'state_version': state.state_version}


class NarrativeDirector:
    """Builds structured narrative input; it does not resolve mechanics or mutate state."""

    def build_input(self, state, *, text, context, resolution=None, evidence=None, policy=None, scene=None):
        return {
            'player_input': str(text),
            'scene': deepcopy(scene or {}),
            'canonical_context': deepcopy(context or {}),
            'resolution': deepcopy(resolution),
            'evidence': deepcopy(evidence or []),
            'policy': deepcopy(policy or {}),
            'constraints': {
                'may_mutate_state': False,
                'may_invent_mechanics': False,
                'may_reveal_hidden_knowledge': False,
            },
        }
