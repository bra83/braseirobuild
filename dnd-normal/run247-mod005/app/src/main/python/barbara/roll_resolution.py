from __future__ import annotations

from copy import deepcopy

from .dice import DiceEngine


class SystemRollResolver:
    """Resolve a pending RollRequest using only module-owned interpretation data.

    Hosts may report observed dice, but they do not get to choose target numbers,
    success/failure, margins or damage semantics when the SystemModule provides enough
    information to derive them. Digital rolls are generated through the injected
    ``RandomSource`` used by :class:`DiceEngine` and carry a complete RNG trace.
    """

    COMPARATORS = {
        '>=': lambda a, b: a >= b,
        '>': lambda a, b: a > b,
        '<=': lambda a, b: a <= b,
        '<': lambda a, b: a < b,
        '==': lambda a, b: a == b,
        '!=': lambda a, b: a != b,
    }

    def __init__(self, dice_engine=None):
        self.dice_engine = dice_engine or DiceEngine()

    def _number(self, value, code='invalid_roll_number'):
        if not isinstance(value, (int, float)) or isinstance(value, bool):
            raise ValueError(code)
        return value

    def _definition(self, authority):
        if not isinstance(authority, dict):
            raise ValueError('invalid_roll_authority')
        metadata = authority.get('metadata') or {}
        if not isinstance(metadata, dict):
            raise ValueError('invalid_roll_authority')
        definition = metadata.get('roll_definition')
        if not isinstance(definition, dict):
            raise ValueError('missing_authoritative_roll_definition')
        expression = definition.get('expression')
        interpretation = definition.get('interpretation') or {}
        variables = definition.get('variables') or {}
        if not isinstance(expression, dict) or not isinstance(interpretation, dict) or not isinstance(variables, dict):
            raise ValueError('invalid_authoritative_roll_definition')
        return definition, expression, interpretation, deepcopy(variables)

    def _observed_values(self, submission):
        if not isinstance(submission, dict):
            raise ValueError('invalid_resume_resolution')
        values = []
        rolls = submission.get('rolls')
        if rolls is not None:
            if not isinstance(rolls, list):
                raise ValueError('invalid_resolution_rolls')
            for row in rolls:
                if not isinstance(row, dict) or 'value' not in row:
                    raise ValueError('invalid_resolution_rolls')
                values.append(self._number(row['value']))
        roll = submission.get('roll')
        if roll is not None:
            roll = self._number(roll)
        total = submission.get('total')
        if total is not None:
            total = self._number(total)
        if total is None:
            if roll is not None:
                total = roll
            elif values:
                total = sum(values)
        if roll is not None and total is not None and not values and roll != total:
            raise ValueError('roll_total_mismatch')
        if values and total is not None and abs(sum(values) - total) > 1e-9:
            raise ValueError('roll_total_mismatch')
        return values, total

    def _authoritative_target(self, authority, interpretation, variables):
        candidates = [
            interpretation.get('target'),
            authority.get('target'),
            (authority.get('metadata') or {}).get('target'),
            variables.get('target'),
        ]
        for value in candidates:
            if isinstance(value, (int, float)) and not isinstance(value, bool):
                return value
        return None

    def _resolved_from_value(self, authority, submission, value, values=None, rng_trace=None):
        _, _, interpretation, variables = self._definition(authority)
        kind = interpretation.get('kind', 'system_rule')
        out = {
            'requirement': 'resolved',
            'source': 'adapter',
            'action': authority.get('action'),
            'mechanic': authority.get('mechanic'),
            'modifiers': deepcopy(authority.get('modifiers', [])),
            'rule_refs': deepcopy(authority.get('rule_refs', [])),
            'metadata': deepcopy(authority.get('metadata') or {}),
        }
        if rng_trace is not None:
            out['rng_trace'] = deepcopy(rng_trace)
        if values:
            out['rolls'] = [{'value': self._number(v)} for v in values]
        if isinstance(value, (int, float)) and not isinstance(value, bool):
            out['total'] = value
            out['roll'] = value

        derived = False
        if kind == 'compare':
            target = self._authoritative_target(authority, interpretation, variables)
            comparator = interpretation.get('comparator')
            if target is not None and comparator in self.COMPARATORS and isinstance(value, (int, float)) and not isinstance(value, bool):
                success = bool(self.COMPARATORS[comparator](value, target))
                out['target'] = target
                out['outcome'] = 'success' if success else 'failure'
                direction = interpretation.get('margin_direction')
                if direction == 'target_minus_roll':
                    out['margin'] = target - value
                else:
                    out['margin'] = value - target
                derived = True
        elif kind == 'count_successes':
            threshold = interpretation.get('threshold')
            minimum = interpretation.get('minimum_successes', 1)
            if values and isinstance(threshold, (int, float)) and not isinstance(threshold, bool) and isinstance(minimum, int) and not isinstance(minimum, bool) and minimum >= 0:
                successes = sum(1 for v in values if self._number(v) >= threshold)
                out['successes'] = successes
                out['outcome'] = 'success' if successes >= minimum else 'failure'
                derived = True
        elif kind == 'damage_total':
            if isinstance(value, (int, float)) and not isinstance(value, bool):
                out['outcome'] = 'resolved'
                out['result'] = 'damage'
                out['degree'] = value
                derived = True
        elif kind == 'system_rule':
            derived = False
        else:
            raise ValueError('unsupported_roll_interpretation')

        if not derived:
            return None
        submitted_outcome = submission.get('outcome')
        if submitted_outcome is not None and submitted_outcome != out.get('outcome'):
            raise ValueError('host_roll_outcome_mismatch')
        submitted_result = submission.get('result')
        if submitted_result is not None and out.get('result') is not None and submitted_result != out.get('result'):
            raise ValueError('host_roll_result_mismatch')
        if 'details' in submission:
            out['details'] = deepcopy(submission['details'])
        if 'tags' in submission:
            out['tags'] = deepcopy(submission['tags'])
        return out

    def resolve_observation(self, authority, submission):
        """Interpret host-reported dice when the module contract is sufficient.

        Returns ``None`` when the SystemModule intentionally lacks enough canonical
        target/pool data. In that case the caller may keep a backwards-compatible
        adapter path, but it must not invent missing authority inside this resolver.
        """
        _, _, interpretation, _ = self._definition(authority)
        values, total = self._observed_values(submission)
        kind = interpretation.get('kind', 'system_rule')
        if kind == 'count_successes':
            if not values:
                return None
            # If the roll definition has an authoritative pool size, enforce it.
            variables = ((authority.get('metadata') or {}).get('roll_definition') or {}).get('variables') or {}
            expected_pool = variables.get('pool')
            if expected_pool is not None:
                if not isinstance(expected_pool, int) or isinstance(expected_pool, bool) or expected_pool < 1:
                    raise ValueError('invalid_authoritative_roll_variable')
                if len(values) != expected_pool:
                    raise ValueError('roll_pool_size_mismatch')
            return self._resolved_from_value(authority, submission, total, values=values, rng_trace=submission.get('rng_trace'))
        if total is None:
            return None
        return self._resolved_from_value(authority, submission, total, values=values, rng_trace=submission.get('rng_trace'))

    def roll_authoritatively(self, authority, random_source=None):
        """Generate and interpret a digital roll from the pending authoritative AST."""
        _, expression, _, variables = self._definition(authority)
        engine = DiceEngine(random_source) if random_source is not None else self.dice_engine
        evaluated = engine.evaluate(expression, variables=variables)
        value = evaluated['value']
        trace = evaluated['trace']
        if isinstance(value, list):
            submission = {
                'rolls': [{'value': self._number(v)} for v in value],
                'total': sum(self._number(v) for v in value),
                'rng_trace': trace,
            }
            resolved = self._resolved_from_value(authority, submission, submission['total'], values=value, rng_trace=trace)
        elif isinstance(value, (int, float)) and not isinstance(value, bool):
            submission = {'roll': value, 'total': value, 'rng_trace': trace}
            resolved = self._resolved_from_value(authority, submission, value, rng_trace=trace)
        else:
            raise ValueError('unsupported_digital_roll_value')
        if resolved is None:
            raise ValueError('roll_interpretation_requires_canonical_target')
        resolved['source'] = 'dice'
        return resolved
