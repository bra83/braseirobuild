from copy import deepcopy
import re

from .state import _validate_canonical_value
from .numeric import is_finite_number


class ResolutionEnvelopeValidator:
    """Canonical contract for trusted mechanical resolutions.

    The envelope is system-agnostic: the core validates shape/authority while each
    SystemModule owns the semantics of degrees, tags and effects. Provider/LLM output
    is never a trusted source.
    """

    SOURCES = {'host','adapter','dice','rules_kernel','replay'}
    REQUIREMENTS = {
        'no_roll','roll_required','target_required','choice_required','reaction_required',
        'push_required','confirmation_required','opposed_roll','resolved'
    }
    DISALLOWED_OUTCOMES = {'maybe','unknown','pending','unresolved'}
    MAX_COLLECTION = 256
    ALLOWED = {
        'resolution_id','command_id','transaction_id','system_id','system_version','family',
        'source','mechanic','action','actor','targets','requirement','outcome','result','degree',
        'successes','tags','roll','total','target','margin','details','rolls','modifiers',
        'effects','costs','events','rule_refs','rng_trace','metadata'
    }

    def _string(self, value, code, optional=False):
        if optional and value is None:
            return
        if not isinstance(value, str) or not value:
            raise ValueError(code)

    def _number(self, value, code):
        if not is_finite_number(value):
            raise ValueError(code)

    def _list(self, value, code):
        if not isinstance(value, list):
            raise ValueError(code)

    def _slug(self, value, code):
        self._string(value, code)
        if not re.fullmatch(r'[A-Za-z0-9_.:\-]{1,120}', value):
            raise ValueError(code)

    def validate(self, resolution):
        if resolution is None:
            return None
        if not isinstance(resolution, dict):
            raise ValueError('invalid_resolution')
        unknown = set(resolution) - self.ALLOWED
        if unknown:
            raise ValueError('invalid_resolution_field')

        out = deepcopy(resolution)
        outcome = out.get('outcome')
        if outcome is not None:
            self._slug(outcome, 'invalid_resolution_outcome')
            if outcome.lower() in self.DISALLOWED_OUTCOMES:
                raise ValueError('invalid_resolution_outcome')
        result = out.get('result')
        if result is not None:
            self._slug(result, 'invalid_resolution_result')
        degree = out.get('degree')
        if degree is not None and not isinstance(degree, (str, int, float)):
            raise ValueError('invalid_resolution_degree')
        if isinstance(degree, bool):
            raise ValueError('invalid_resolution_degree')
        if isinstance(degree, float) and not is_finite_number(degree):
            raise ValueError('invalid_resolution_degree')

        requirement = out.get('requirement')
        if requirement is not None and requirement not in self.REQUIREMENTS:
            raise ValueError('invalid_resolution_requirement')
        source = out.get('source', 'host')
        if source not in self.SOURCES:
            raise ValueError('untrusted_resolution_source')
        out['source'] = source

        for key in ('resolution_id','command_id','transaction_id','system_id','system_version','family','mechanic','action','actor'):
            if key in out:
                self._string(out[key], 'invalid_resolution_binding')
        for key in ('roll','total','target','margin','successes'):
            if key in out:
                self._number(out[key], 'invalid_resolution_number')
        if 'successes' in out and out['successes'] < 0:
            raise ValueError('invalid_resolution_successes')
        for key in ('targets','rolls','modifiers','effects','costs','events','rule_refs','tags'):
            if key in out:
                self._list(out[key], 'invalid_resolution_'+key)
                if len(out[key])>self.MAX_COLLECTION: raise ValueError('resolution_collection_too_large')
        if 'tags' in out and not all(isinstance(tag,str) and tag for tag in out['tags']):
            raise ValueError('invalid_resolution_tags')
        if 'rolls' in out:
            for roll in out['rolls']:
                if not isinstance(roll, dict):
                    raise ValueError('invalid_resolution_rolls')
        if 'details' in out and not isinstance(out['details'], (dict, list, str, int, float, bool, type(None))):
            raise ValueError('invalid_resolution_details')
        if 'metadata' in out and not isinstance(out['metadata'], dict):
            raise ValueError('invalid_resolution_metadata')
        if 'rng_trace' in out and not isinstance(out['rng_trace'], (dict, list, str, int, float, type(None))):
            raise ValueError('invalid_resolution_rng_trace')
        try:_validate_canonical_value(out)
        except ValueError as exc:raise ValueError('invalid_resolution_value') from exc

        if requirement == 'resolved' and outcome is None and result is None:
            raise ValueError('resolved_without_outcome')
        if (outcome is not None or result is not None) and requirement in {
            'roll_required','target_required','choice_required','reaction_required',
            'push_required','confirmation_required','opposed_roll'
        }:
            raise ValueError('unresolved_requirement_has_outcome')
        return out
