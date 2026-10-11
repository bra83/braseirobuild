from copy import deepcopy
import hashlib
import json

from .rules_ast import RuleExpressionRuntime
from .knowledge_engine import RuleResolutionCache

class RuleGate:
    MIN_AUTHORITY=0.5
    def applicable(self,evidence):
        return [e for e in evidence
                if getattr(e,'kind',None)=='RULE'
                and not getattr(e,'secret',False)
                and isinstance(getattr(e,'authority',None),(int,float))
                and not isinstance(getattr(e,'authority',None),bool)
                and getattr(e,'authority',0)>=self.MIN_AUTHORITY]
    def require(self,mechanical:bool,evidence):
        if not isinstance(mechanical,bool): raise ValueError('invalid_mechanical_flag')
        rules=self.applicable(evidence)
        if mechanical and not rules: raise LookupError('regra_sem_evidencia_canonica')
        return rules


class RulesKernel:
    """Deterministic mechanical planner over a SystemModule and canonical context.

    The kernel resolves a safe SystemModule rule AST, binds named roll definitions to
    executable Dice AST, preserves rule provenance and caches only by explicit system/
    context/rule revision inputs. It never narrates, calls an LLM or commits state.
    """
    def __init__(self,module_registry, runtime=None, cache=None):
        self.modules=module_registry
        self.runtime=runtime or RuleExpressionRuntime()
        self.cache=cache or RuleResolutionCache()

    def _rules_revision(self,module):
        payload={
            'system_id':module.system_id,'version':module.version,
            'rules':deepcopy(dict(module.rule_definitions)),
            'rolls':deepcopy(dict(module.roll_definitions)),
        }
        raw=json.dumps(payload,ensure_ascii=False,sort_keys=True,separators=(',',':'),default=str).encode()
        return hashlib.sha256(raw).hexdigest()

    def _context_tags(self,context,state):
        context=context or {}
        vis=context.get('visibility_context') or {}
        hazards=[]
        for hazard in context.get('hazards') or ():
            if isinstance(hazard,dict):
                hazards.append((str(hazard.get('id') or hazard.get('name') or ''),bool(hazard.get('requires_check')),repr(hazard.get('applies_to'))))
            else:
                hazards.append((str(hazard),False,''))
        return {
            f"state_version:{getattr(state,'state_version',0)}",
            f"location:{context.get('location_id') or ''}",
            f"terrain:{context.get('terrain') or ''}",
            f"visibility:{vis.get('quality') or ''}",
            f"hazards:{repr(sorted(hazards))}",
        }

    def _binding_root(self,state,context):
        return {
            'state': {
                'system_id': state.system_id,
                'state_version': getattr(state,'state_version',0),
                'location': getattr(state,'location',''),
                'player_state': deepcopy(getattr(state,'player_state',{})),
                'scene': deepcopy(getattr(state,'scene',{})),
                'facts': deepcopy(getattr(state,'facts',{})),
                'npcs': deepcopy(getattr(state,'npcs',{})),
            },
            'context': deepcopy(context or {}),
        }

    def _resolve_ref(self,root,ref):
        if not isinstance(ref,str) or not ref or ref.startswith('_') or '..' in ref:
            raise ValueError('invalid_roll_binding_ref')
        cur=root
        for part in ref.split('.'):
            if not isinstance(cur,dict) or part not in cur:
                return None
            cur=cur[part]
        return deepcopy(cur)

    def _bind_roll(self,module,roll,state=None,context=None):
        if roll is None:
            return None
        if not isinstance(roll,dict):
            raise ValueError('invalid_rule_roll')
        if set(roll)=={'definition'}:
            definition_id=roll.get('definition')
            if not isinstance(definition_id,str) or not definition_id:
                raise ValueError('invalid_roll_definition_id')
            try:
                definition=deepcopy(dict(module.roll_definitions[definition_id]))
            except KeyError as exc:
                raise ValueError('unknown_roll_definition') from exc
            if not isinstance(definition.get('expression'),dict):
                raise ValueError('roll_definition_without_dice_ast')
            bindings=deepcopy(definition.get('bindings') or {})
            if not isinstance(bindings,dict) or any(not isinstance(k,str) or not k or not isinstance(v,str) or not v for k,v in bindings.items()):
                raise ValueError('invalid_roll_bindings')
            root=self._binding_root(state,context) if state is not None else {'state':{},'context':deepcopy(context or {})}
            variables={}
            for name,ref in bindings.items():
                value=self._resolve_ref(root,ref)
                if value is not None:
                    if not isinstance(value,(int,float)) or isinstance(value,bool): raise ValueError('invalid_authoritative_roll_variable')
                    variables[name]=value
            interpretation=deepcopy(definition.get('interpretation') or {})
            target_ref=interpretation.get('target_ref')
            if target_ref is not None:
                target=self._resolve_ref(root,target_ref)
                if target is not None:
                    if not isinstance(target,(int,float)) or isinstance(target,bool): raise ValueError('invalid_authoritative_roll_target')
                    interpretation['target']=target
            return {
                'definition':definition_id,
                'model':definition.get('model'),
                'expression':deepcopy(definition['expression']),
                'interpretation':interpretation,
                'bindings':bindings,
                'variables':variables,
                'system_id':module.system_id,
                'system_version':module.version,
            }
        # Installable modules may inline a safe roll definition. Keep the same output
        # contract as named definitions and fail closed if no Dice AST is supplied.
        if not isinstance(roll.get('expression'),dict):
            raise ValueError('roll_definition_without_dice_ast')
        bindings=deepcopy(roll.get('bindings') or {})
        if not isinstance(bindings,dict) or any(not isinstance(k,str) or not k or not isinstance(v,str) or not v for k,v in bindings.items()):
            raise ValueError('invalid_roll_bindings')
        root=self._binding_root(state,context) if state is not None else {'state':{},'context':deepcopy(context or {})}
        variables={}
        for name,ref in bindings.items():
            value=self._resolve_ref(root,ref)
            if value is not None:
                if not isinstance(value,(int,float)) or isinstance(value,bool): raise ValueError('invalid_authoritative_roll_variable')
                variables[name]=value
        interpretation=deepcopy(roll.get('interpretation') or {})
        target_ref=interpretation.get('target_ref')
        if target_ref is not None:
            target=self._resolve_ref(root,target_ref)
            if target is not None:
                if not isinstance(target,(int,float)) or isinstance(target,bool): raise ValueError('invalid_authoritative_roll_target')
                interpretation['target']=target
        return {
            'definition':roll.get('definition'),
            'model':roll.get('model'),
            'expression':deepcopy(roll['expression']),
            'interpretation':interpretation,
            'bindings':bindings,
            'variables':variables,
            'system_id':module.system_id,
            'system_version':module.version,
        }

    def _resolve_module_plan(self,module,state,intent,context):
        revision=self._rules_revision(module)
        normalized_intent=json.dumps(intent,ensure_ascii=False,sort_keys=True,separators=(',',':'),default=str)
        key=self.cache.key(module.version,normalized_intent,self._context_tags(context,state),revision)
        cached=self.cache.get(key)
        if cached is not None:
            return cached
        plan=self.runtime.resolve(
            module.rule_definitions, intent=intent, context=context,
            state={'system_id':state.system_id,'state_version':getattr(state,'state_version',0)},
        )
        if plan is not None:
            plan=deepcopy(plan)
            plan['roll']=self._bind_roll(module,plan.get('roll'),state=state,context=context)
            plan['rules_revision']=revision
            plan['system_version']=module.version
        self.cache.put(key,plan)
        return deepcopy(plan)

    def plan(self,state,intent,context=None):
        if not isinstance(intent,dict): raise ValueError('invalid_action_intent')
        module=self.modules.get(state.system_id); action=intent.get('action','freeform'); context=context or {}
        intent_type=intent.get('intent_type')
        # Meta/UI/thought channels are globally non-mechanical and never reach system rules.
        if intent_type in {'rules_query','meta_command','ui_command','character_thought'}:
            return {'requirement':'no_roll','action':action,'mechanic':'none','modifiers':[],'rule_refs':[],'source':'rules_kernel','system_version':module.version,'rules_revision':self._rules_revision(module)}
        module_plan=self._resolve_module_plan(module,state,intent,context)
        if module_plan is not None:
            module_plan['action']='speak' if intent_type=='dialogue' else action
            module_plan['rule_source']=module_plan.get('source')
            module_plan['source']='rules_kernel'
            # Context-derived modifiers are appended below when relevant; system rule
            # declarations remain the authoritative source of requirement/roll shape.
            if action!='search':
                return module_plan
        mechanic='none'
        if action=='attack':
            # Compatibility for installable modules that intentionally omit an attack rule.
            requirement='roll_required'
            mechanic='damage' if module.supports('combat.auto_hit_damage') else 'attack'
        elif action in {'move','search','interact'}:
            hazards=context.get('hazards') or ()
            requires_check=False
            for hazard in hazards:
                if not isinstance(hazard,dict) or not hazard.get('requires_check'):
                    continue
                applies=hazard.get('applies_to',())
                if applies in (None,'all') or action in (applies if isinstance(applies,(list,tuple,set)) else (applies,)):
                    requires_check=True; break
            requirement='roll_required' if requires_check else 'no_roll'
            mechanic=action
        else:
            requirement='no_roll'
            mechanic=action
        modifiers=[]
        vis=context.get('visibility_context') or {}
        if action=='search' and isinstance(vis,dict):
            quality=vis.get('quality')
            if quality in {'none','poor'}: modifiers.append({'source':'visibility','tag':'impaired_visibility','quality':quality})
            elif quality in {'good','excellent'}: modifiers.append({'source':'visibility','tag':'clear_visibility','quality':quality})
        return {'requirement':requirement,'action':action,'mechanic':mechanic,'modifiers':modifiers,'rule_refs':[],'source':'rules_kernel','system_version':module.version,'rules_revision':self._rules_revision(module)}
