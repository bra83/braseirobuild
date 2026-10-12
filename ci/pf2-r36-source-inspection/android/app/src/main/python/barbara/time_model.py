from __future__ import annotations

from copy import deepcopy


class GameClock:
    """Canonical game-time clock independent from wall-clock/Android time."""

    def __init__(self, module_registry):
        self.modules = module_registry

    def _ensure(self, state):
        clock = state.game_time
        if not isinstance(clock, dict):
            raise ValueError('invalid_game_time')
        clock.setdefault('elapsed_seconds', 0)
        clock.setdefault('round', 0)
        clock.setdefault('exploration_turn', 0)
        clock.setdefault('travel_turn', 0)
        if not isinstance(clock['elapsed_seconds'], (int, float)) or isinstance(clock['elapsed_seconds'], bool) or clock['elapsed_seconds'] < 0:
            raise ValueError('invalid_game_time')
        return clock

    def advance(self, state, unit='exploration_turn', amount=1, *, emit_event=True, request_id='system:clock'):
        module = self.modules.get(state.system_id)
        clock = self._ensure(state)
        seconds = module.time_model.duration_for(unit, amount)
        clock['elapsed_seconds'] += seconds
        if unit in {'round', 'exploration_turn', 'travel_turn'}:
            clock[unit] = clock.get(unit, 0) + amount
        if emit_event:
            state.event_log.append({
                'event_id': f'clock:{state.state_version}:{len(state.event_log)}',
                'type': 'game_time_advanced',
                'request_id': request_id,
                'tick': state.tick,
                'state_version': state.state_version,
                'payload': {'unit': unit, 'amount': amount, 'seconds': seconds},
            })
        return deepcopy(clock)
