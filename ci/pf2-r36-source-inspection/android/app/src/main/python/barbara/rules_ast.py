from __future__ import annotations

from copy import deepcopy
from collections.abc import Mapping

from .numeric import is_finite_number
from .state import _validate_canonical_value


class RuleExpressionRuntime:
    """Small safe AST runtime for declarative SystemModule mechanics.

    The runtime can inspect only explicitly supplied intent/context values and can
    return a mechanical requirement/roll expression/effect templates. It has no
    persistence, reflection, imports, callbacks or arbitrary code execution.
    """

    MAX_DEPTH = 24
    COMPARATORS = {'eq','ne','in','not_in','contains','truthy','falsy','gte','lte','gt','lt'}

    def _path(self, root, path):
        if not isinstance(path, str) or not path or path.startswith('_') or '..' in path:
            raise ValueError('invalid_rule_path')
        cur = root
        for part in path.split('.'):
            if not part or part.startswith('_'):
                raise ValueError('invalid_rule_path')
            if not isinstance(cur, dict):
                return None
            cur = cur.get(part)
        return cur

    def _value(self, node, env, depth):
        if depth > self.MAX_DEPTH:
            raise ValueError('rule_ast_too_deep')
        if isinstance(node, float) and not is_finite_number(node):
            raise ValueError('invalid_rule_value')
        if isinstance(node, (str, int, float, bool)) or node is None:
            return node
        if isinstance(node, list):
            return [self._value(x, env, depth + 1) for x in node]
        if not isinstance(node, dict):
            raise ValueError('invalid_rule_value')
        if set(node) == {'ref'}:
            ref = node['ref']
            if not isinstance(ref, str) or '.' not in ref:
                raise ValueError('invalid_rule_ref')
            namespace, path = ref.split('.', 1)
            if namespace not in {'intent','context','state'}:
                raise ValueError('invalid_rule_ref_namespace')
            return self._path(env.get(namespace, {}), path)
        if set(node) == {'const'}:
            try:_validate_canonical_value(node['const'])
            except ValueError as exc:raise ValueError('invalid_rule_value') from exc
            return deepcopy(node['const'])
        raise ValueError('invalid_rule_value_expression')

    def predicate(self, expr, env, depth=0):
        if depth > self.MAX_DEPTH:
            raise ValueError('rule_ast_too_deep')
        if expr is True or expr is False:
            return expr
        if not isinstance(expr, dict) or len(expr) != 1:
            raise ValueError('invalid_rule_predicate')
        op, payload = next(iter(expr.items()))
        if op == 'all':
            if not isinstance(payload, list) or not payload: raise ValueError('invalid_rule_predicate')
            values=[self.predicate(x, env, depth + 1) for x in payload]
            return all(values)
        if op == 'any':
            if not isinstance(payload, list) or not payload: raise ValueError('invalid_rule_predicate')
            values=[self.predicate(x, env, depth + 1) for x in payload]
            return any(values)
        if op == 'not':
            return not self.predicate(payload, env, depth + 1)
        if op not in self.COMPARATORS or not isinstance(payload, list) or len(payload) not in {1,2}:
            raise ValueError('invalid_rule_predicate')
        left = self._value(payload[0], env, depth + 1)
        right = self._value(payload[1], env, depth + 1) if len(payload) == 2 else None
        if op == 'eq': return left == right
        if op == 'ne': return left != right
        if op == 'in': return isinstance(right, (list, tuple, set, frozenset, str)) and left in right
        if op == 'not_in': return not (isinstance(right, (list, tuple, set, frozenset, str)) and left in right)
        if op == 'contains': return isinstance(left, (list, tuple, set, frozenset, str, dict)) and right in left
        if op == 'truthy': return bool(left)
        if op == 'falsy': return not bool(left)
        if not isinstance(left, (int,float)) or isinstance(left,bool) or not isinstance(right,(int,float)) or isinstance(right,bool):
            return False
        if not is_finite_number(left) or not is_finite_number(right): raise ValueError('invalid_rule_value')
        return {'gte':left >= right,'lte':left <= right,'gt':left > right,'lt':left < right}[op]

    def validate_rule(self, rule):
        if not isinstance(rule, dict): raise ValueError('invalid_rule_definition')
        allowed={'rule_id','when','requirement','mechanic','roll','modifiers','effects','costs','time_cost','source_refs','priority'}
        if set(rule)-allowed: raise ValueError('unknown_rule_definition_field')
        if not isinstance(rule.get('rule_id'),str) or not rule['rule_id']: raise ValueError('invalid_rule_id')
        if 'when' not in rule: raise ValueError('missing_rule_predicate')
        # evaluate against an empty environment to validate AST shape without assuming match
        try: self.predicate(rule['when'], {'intent':{},'context':{},'state':{}})
        except (TypeError, KeyError): raise ValueError('invalid_rule_predicate')
        req=rule.get('requirement','no_roll')
        if req not in {
            'no_roll','roll_required','target_required','choice_required','player_choice_required',
            'reaction_required','push_required','confirmation_required','opposed_roll'
        }:
            raise ValueError('invalid_rule_requirement')
        if 'roll' in rule and rule['roll'] is not None and not isinstance(rule['roll'],dict): raise ValueError('invalid_rule_roll')
        for name in ('modifiers','effects','costs','source_refs'):
            if name in rule and not isinstance(rule[name],list): raise ValueError('invalid_rule_collection')
        if 'priority' in rule and (not isinstance(rule['priority'],int) or isinstance(rule['priority'],bool)): raise ValueError('invalid_rule_priority')
        return True

    def resolve(self, rules, *, intent, context=None, state=None):
        if isinstance(rules, Mapping): rules=list(rules.values())
        if not isinstance(rules, (list, tuple)): raise ValueError('invalid_rule_set')
        env={'intent':deepcopy(intent or {}),'context':deepcopy(context or {}),'state':deepcopy(state or {})}
        matches=[]
        for rule in rules:
            self.validate_rule(rule)
            if self.predicate(rule['when'], env): matches.append(rule)
        if not matches: return None
        matches.sort(key=lambda r:(int(r.get('priority',0)), r['rule_id']), reverse=True)
        rule=matches[0]
        requirement=rule.get('requirement','no_roll')
        if requirement=='player_choice_required':
            requirement='choice_required'
        return {
            'requirement':requirement,
            'mechanic':rule.get('mechanic') or intent.get('action') or 'none',
            'roll':deepcopy(rule.get('roll')),
            'modifiers':deepcopy(rule.get('modifiers',[])),
            'effects':deepcopy(rule.get('effects',[])),
            'costs':deepcopy(rule.get('costs',[])),
            'time_cost':deepcopy(rule.get('time_cost')),
            'rule_refs':[rule['rule_id'], *deepcopy(rule.get('source_refs',[]))],
            'source':'system_module_rule_ast',
        }
