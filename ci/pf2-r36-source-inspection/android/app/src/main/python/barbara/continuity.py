from __future__ import annotations

import re
from copy import deepcopy

from .security import public_view


class ContinuityGate:
    """Post-generation barrier over canonical truth, secrets and resolved outcomes."""

    _DEFEATED_UP = (r'\b(?:se levanta|fica de pé|permanece de pé|continua lutando)\b', r'\b(?:stands up|remains standing|keeps fighting)\b')

    def _private_strings(self, state):
        public = public_view(state.to_dict() if hasattr(state, 'to_dict') else {})
        raw = state.to_dict() if hasattr(state, 'to_dict') else {}
        pub_blob = set()
        def collect(value, out):
            if isinstance(value, dict):
                for v in value.values(): collect(v, out)
            elif isinstance(value, (list, tuple, set)):
                for v in value: collect(v, out)
            elif isinstance(value, str) and len(value.strip()) >= 6:
                out.add(value.strip().lower())
        all_strings, public_strings = set(), set()
        collect(raw, all_strings); collect(public, public_strings)
        return all_strings - public_strings

    def validate(self, narration, state, *, claims=None, resolution=None):
        if not isinstance(narration, str) or not narration.strip():
            raise ValueError('invalid_narration')
        if claims is not None and (not isinstance(claims, list) or not all(isinstance(c, str) for c in claims)):
            raise ValueError('invalid_claims')
        rendered=[narration]
        if claims: rendered.extend(claims)
        low = '\n'.join(rendered).lower()
        for secret in self._private_strings(state):
            if secret in low:
                raise ValueError('hidden_knowledge_leak')
        defeated = []
        for npc_id, npc in state.npcs.items():
            if isinstance(npc, dict) and (npc.get('alive') is False or 'DEFEATED' in set(npc.get('conditions', []))):
                defeated.append((npc_id, str(npc.get('name', npc_id)).lower()))
        if defeated and any(re.search(p, low, re.I) for p in self._DEFEATED_UP):
            for _, name in defeated:
                if name in low:
                    raise ValueError('canonical_outcome_contradiction')
        return True
