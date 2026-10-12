from __future__ import annotations

import random
import secrets
from copy import deepcopy

from .numeric import is_finite_number


class RandomSource:
    """Injectable RNG boundary used by the deterministic dice runtime."""

    def randint(self, low: int, high: int) -> int:
        raise NotImplementedError


class SecureRandomSource(RandomSource):
    def __init__(self):
        self._rng = secrets.SystemRandom()

    def randint(self, low: int, high: int) -> int:
        return self._rng.randint(low, high)


class SeededRandomSource(RandomSource):
    def __init__(self, seed):
        self.seed = seed
        # Replay RNG, deliberately deterministic; never used for secrets or auth.
        self._rng = random.Random(seed)  # nosec B311

    def randint(self, low: int, high: int) -> int:
        return self._rng.randint(low, high)


class SequenceRandomSource(RandomSource):
    """Exact RNG for unit tests and replay debugging."""

    def __init__(self, values):
        self.values = list(values)
        self.index = 0

    def randint(self, low: int, high: int) -> int:
        if self.index >= len(self.values):
            raise ValueError('rng_sequence_exhausted')
        value = self.values[self.index]
        self.index += 1
        if not isinstance(value, int) or isinstance(value, bool) or not low <= value <= high:
            raise ValueError('rng_sequence_value_out_of_range')
        return value


class DiceEngine:
    """System-agnostic Dice AST evaluator.

    The engine knows arithmetic and random dice operations, but not what a
    success, critical, armor class or skill means in any RPG. System modules
    compile their rules into this safe AST and interpret the resulting values.
    """

    MAX_DEPTH = 32
    MAX_DICE = 200
    MAX_SIDES = 100000
    MAX_ARGS = 256
    MAX_VARIABLES = 256
    COMPARATORS = {'>=', '>', '<=', '<', '==', '!='}

    def __init__(self, random_source=None):
        self.random_source = random_source or SecureRandomSource()

    def _number(self, value, code='invalid_dice_number'):
        if not is_finite_number(value):
            raise ValueError(code)
        return value

    def _integer(self, value, code='invalid_dice_integer'):
        if not isinstance(value, int) or isinstance(value, bool):
            raise ValueError(code)
        return value

    def _compare(self, left, right, operator):
        if operator not in self.COMPARATORS:
            raise ValueError('invalid_dice_comparator')
        return {
            '>=': left >= right,
            '>': left > right,
            '<=': left <= right,
            '<': left < right,
            '==': left == right,
            '!=': left != right,
        }[operator]

    def _fields(self, expr, allowed, code):
        if set(expr) - set(allowed):
            raise ValueError(code)

    def _draw(self, low, high):
        draw = getattr(self.random_source, 'randint', None)
        if not callable(draw):
            raise ValueError('invalid_random_source')
        value = draw(low, high)
        if not isinstance(value, int) or isinstance(value, bool) or not low <= value <= high:
            raise ValueError('rng_value_out_of_range')
        return value

    def _eval(self, expr, trace, depth, variables):
        if depth > self.MAX_DEPTH:
            raise ValueError('dice_ast_too_deep')
        if isinstance(expr, (int, float)) and not isinstance(expr, bool):
            return self._number(expr)
        if not isinstance(expr, dict):
            raise ValueError('invalid_dice_expression')
        op = expr.get('op')
        if not isinstance(op, str):
            raise ValueError('invalid_dice_operation')

        if op == 'var':
            if set(expr) != {'op','name'} or not isinstance(expr.get('name'),str) or not expr['name'] or expr['name'].startswith('_'):
                raise ValueError('invalid_dice_variable')
            if expr['name'] not in variables:
                raise ValueError('missing_dice_variable')
            value=variables[expr['name']]
            return self._number(value,'invalid_dice_variable_value')

        if op == 'const':
            self._fields(expr, {'op','value'}, 'invalid_const_fields')
            return self._number(expr.get('value'))

        if op == 'die':
            if set(expr) - {'op', 'sides', 'id'}:
                raise ValueError('invalid_die_fields')
            raw_sides=expr.get('sides'); sides = self._integer(self._eval(raw_sides,trace,depth+1,variables) if isinstance(raw_sides,dict) else raw_sides, 'invalid_die_sides')
            if sides < 2 or sides > self.MAX_SIDES:
                raise ValueError('invalid_die_sides')
            value = self._draw(1, sides)
            trace.append({'op': 'die', 'id': expr.get('id'), 'sides': sides, 'value': value})
            return value

        if op == 'dice':
            if set(expr) - {'op', 'count', 'sides', 'id'}:
                raise ValueError('invalid_dice_fields')
            raw_count=expr.get('count'); count = self._integer(self._eval(raw_count,trace,depth+1,variables) if isinstance(raw_count,dict) else raw_count, 'invalid_dice_count')
            raw_sides=expr.get('sides'); sides = self._integer(self._eval(raw_sides,trace,depth+1,variables) if isinstance(raw_sides,dict) else raw_sides, 'invalid_die_sides')
            if not 1 <= count <= self.MAX_DICE or not 2 <= sides <= self.MAX_SIDES:
                raise ValueError('invalid_dice_shape')
            values = []
            for index in range(count):
                value = self._draw(1, sides)
                trace.append({'op': 'die', 'id': f"{expr.get('id') or 'dice'}:{index}", 'sides': sides, 'value': value})
                values.append(value)
            return values

        if op in {'sum', 'add'}:
            self._fields(expr, {'op','args'}, 'invalid_dice_args')
            args = expr.get('args')
            if not isinstance(args, list) or not args or len(args) > self.MAX_ARGS:
                raise ValueError('invalid_dice_args')
            values = [self._eval(arg, trace, depth + 1, variables) for arg in args]
            total = 0
            for value in values:
                if isinstance(value, list):
                    total += sum(self._number(x) for x in value)
                else:
                    total += self._number(value)
            return total

        if op == 'subtract':
            self._fields(expr, {'op','left','right'}, 'invalid_dice_arithmetic_fields')
            left = self._number(self._eval(expr.get('left'), trace, depth + 1, variables))
            right = self._number(self._eval(expr.get('right'), trace, depth + 1, variables))
            return left - right

        if op == 'multiply':
            self._fields(expr, {'op','left','right'}, 'invalid_dice_arithmetic_fields')
            left = self._number(self._eval(expr.get('left'), trace, depth + 1, variables))
            right = self._number(self._eval(expr.get('right'), trace, depth + 1, variables))
            return left * right

        if op == 'divide':
            self._fields(expr, {'op','left','right'}, 'invalid_dice_arithmetic_fields')
            left = self._number(self._eval(expr.get('left'), trace, depth + 1, variables))
            right = self._number(self._eval(expr.get('right'), trace, depth + 1, variables))
            if right == 0:
                raise ValueError('dice_division_by_zero')
            return left / right

        if op == 'reroll':
            self._fields(expr, {'op','expr','sides','threshold','comparator','max_rerolls'}, 'invalid_reroll_fields')
            values = self._eval(expr.get('expr'), trace, depth + 1, variables)
            if not isinstance(values, list) or not values:
                raise ValueError('invalid_reroll_expression')
            raw_sides=expr.get('sides'); sides = self._integer(self._eval(raw_sides,trace,depth+1,variables) if isinstance(raw_sides,dict) else raw_sides, 'invalid_die_sides')
            if not 2 <= sides <= self.MAX_SIDES:
                raise ValueError('invalid_die_sides')
            threshold = self._number(expr.get('threshold'))
            comparator = expr.get('comparator', '<=')
            max_rerolls = self._integer(expr.get('max_rerolls', 1), 'invalid_reroll_limit')
            if not 0 <= max_rerolls <= 100:
                raise ValueError('invalid_reroll_limit')
            out = list(values)
            for index, value in enumerate(out):
                rerolls = 0
                while rerolls < max_rerolls and self._compare(self._number(value), threshold, comparator):
                    new_value = self._draw(1, sides)
                    trace.append({'op': 'reroll', 'index': index, 'sides': sides, 'from': value, 'to': new_value, 'attempt': rerolls + 1})
                    value = new_value
                    rerolls += 1
                out[index] = value
            return out

        if op == 'explode':
            self._fields(expr, {'op','expr','sides','trigger','comparator','max_explosions'}, 'invalid_explode_fields')
            values = self._eval(expr.get('expr'), trace, depth + 1, variables)
            if not isinstance(values, list) or not values:
                raise ValueError('invalid_explode_expression')
            raw_sides=expr.get('sides'); sides = self._integer(self._eval(raw_sides,trace,depth+1,variables) if isinstance(raw_sides,dict) else raw_sides, 'invalid_die_sides')
            if not 2 <= sides <= self.MAX_SIDES:
                raise ValueError('invalid_die_sides')
            trigger = expr.get('trigger', sides)
            self._number(trigger, 'invalid_explode_trigger')
            comparator = expr.get('comparator', '==')
            max_explosions = self._integer(expr.get('max_explosions', 20), 'invalid_explosion_limit')
            if not 0 <= max_explosions <= 100:
                raise ValueError('invalid_explosion_limit')
            out = list(values)
            queue = list(values)
            explosions = 0
            while queue and explosions < max_explosions:
                current = queue.pop(0)
                if not self._compare(self._number(current), trigger, comparator):
                    continue
                new_value = self._draw(1, sides)
                explosions += 1
                out.append(new_value)
                queue.append(new_value)
                trace.append({'op': 'explode', 'sides': sides, 'trigger': trigger, 'from': current, 'to': new_value, 'explosion': explosions})
            return out

        if op == 'push':
            self._fields(expr, {'op','expr','sides','locked_values','pushes'}, 'invalid_push_fields')
            values = self._eval(expr.get('expr'), trace, depth + 1, variables)
            if not isinstance(values, list) or not values:
                raise ValueError('invalid_push_expression')
            raw_sides=expr.get('sides'); sides = self._integer(self._eval(raw_sides,trace,depth+1,variables) if isinstance(raw_sides,dict) else raw_sides, 'invalid_die_sides')
            if not 2 <= sides <= self.MAX_SIDES:
                raise ValueError('invalid_die_sides')
            locked_values = expr.get('locked_values', [1, sides])
            if not isinstance(locked_values, list) or len(locked_values)>self.MAX_ARGS or not all(is_finite_number(v) for v in locked_values):
                raise ValueError('invalid_push_locked_values')
            pushes = self._integer(expr.get('pushes', 1), 'invalid_push_count')
            if not 0 <= pushes <= 10:
                raise ValueError('invalid_push_count')
            out = list(values)
            for push_index in range(pushes):
                changed = False
                for index, value in enumerate(list(out)):
                    if value in locked_values:
                        continue
                    new_value = self._draw(1, sides)
                    trace.append({'op': 'push', 'push': push_index + 1, 'index': index, 'sides': sides, 'from': value, 'to': new_value, 'locked_values': list(locked_values)})
                    out[index] = new_value
                    changed = True
                if not changed:
                    break
            return out

        if op in {'keep_highest', 'keep_lowest'}:
            self._fields(expr, {'op','expr','count'}, 'invalid_keep_fields')
            values = self._eval(expr.get('expr'), trace, depth + 1, variables)
            count = self._integer(expr.get('count', 1), 'invalid_keep_count')
            if not isinstance(values, list) or not values or not 1 <= count <= len(values):
                raise ValueError('invalid_keep_expression')
            ordered = sorted((self._number(v) for v in values), reverse=(op == 'keep_highest'))
            kept = ordered[:count]
            trace.append({'op': op, 'input': list(values), 'kept': list(kept)})
            return kept

        if op == 'count_successes':
            self._fields(expr, {'op','expr','threshold','comparator'}, 'invalid_success_fields')
            values = self._eval(expr.get('expr'), trace, depth + 1, variables)
            threshold = self._number(expr.get('threshold'))
            comparator = expr.get('comparator', '>=')
            if not isinstance(values, list):
                raise ValueError('invalid_success_pool')
            successes = sum(1 for value in values if self._compare(self._number(value), threshold, comparator))
            trace.append({'op': 'count_successes', 'threshold': threshold, 'comparator': comparator, 'successes': successes})
            return successes

        if op == 'compare':
            self._fields(expr, {'op','left','right','comparator'}, 'invalid_compare_fields')
            left = self._number(self._eval(expr.get('left'), trace, depth + 1, variables))
            right = self._number(self._eval(expr.get('right'), trace, depth + 1, variables))
            comparator = expr.get('comparator')
            success = self._compare(left, right, comparator)
            margin = left - right
            result = {'success': success, 'left': left, 'right': right, 'comparator': comparator, 'margin': margin}
            trace.append({'op': 'compare', **deepcopy(result)})
            return result

        raise ValueError('unsupported_dice_operation')

    def evaluate(self, expression, variables=None):
        trace = []
        variables={} if variables is None else deepcopy(variables)
        if not isinstance(variables,dict) or len(variables)>self.MAX_VARIABLES or any(not isinstance(k,str) or not k or k.startswith('_') for k in variables): raise ValueError('invalid_dice_variables')
        for value in variables.values():
            self._number(value,'invalid_dice_variable_value')
        value = self._eval(deepcopy(expression), trace, 0, variables)
        if isinstance(value,(int,float)) and not isinstance(value,bool):
            self._number(value)
        elif isinstance(value,list):
            for item in value: self._number(item)
        elif isinstance(value,dict):
            for item in value.values():
                if isinstance(item,(int,float)) and not isinstance(item,bool): self._number(item)
        return {'value': deepcopy(value), 'trace': trace, 'expression': deepcopy(expression)}
