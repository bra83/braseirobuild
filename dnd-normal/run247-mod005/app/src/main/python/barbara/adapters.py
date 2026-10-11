from dataclasses import dataclass
from copy import deepcopy
from .system_module import SystemModuleRegistry, BUILTIN_MODULES

# Compatibility/export surface. Identity comes from the installed builtin modules,
# not a second per-system mechanics table in the core.
SUPPORTED=tuple(module.system_id for module in BUILTIN_MODULES)


@dataclass(frozen=True)
class Adapter:
    system_id:str
    family:str
    rules_required:bool=True
    lore_scope:str|None=None
    min_rule_authority:float=0.5
    module:object|None=None

    def validate_campaign(self,state):
        if state.system_id!=self.system_id: raise ValueError('adapter_system_mismatch')
        return True

    def supports(self, capability):
        if not isinstance(capability,str) or not capability: raise ValueError('invalid_capability')
        return bool(self.module and self.module.supports(capability))

    def capabilities(self):
        return tuple(sorted(self.module.capabilities if self.module is not None else ()))

    def rules_ready(self,rag,campaign_id):
        if not self.rules_required:return True
        for (c,s,_,_),e in rag._docs.items():
            if c==campaign_id and s==self.system_id and e.kind=='RULE' and not e.secret and e.authority>=self.min_rule_authority:return True
        return False

    def mechanics_profile(self):
        if self.module is None: raise ValueError('adapter_without_system_module')
        snap=self.module.snapshot()
        primary=snap['roll_definitions'].get('primary',{})
        skill=snap['roll_definitions'].get('skill',{})
        combat=snap['combat_model']
        inventory=snap['inventory_model']
        return {
            'roll_model':primary.get('model','unknown'),
            'skill_model':skill.get('model','unknown'),
            'combat_model':combat.get('model','unknown'),
            'resource_style':inventory.get('resource_style',inventory.get('model','generic')),
            'system_id':self.system_id,'family':self.family,'capabilities':list(self.capabilities()),
            'module_version':self.module.version,'time_model':snap['time_model'],
            'roll_definitions':deepcopy(snap['roll_definitions']),
        }

    def validate_resolution(self,resolution):
        if resolution is None:return None
        if not isinstance(resolution,dict):raise ValueError('invalid_resolution')
        bound=resolution.get('system_id')
        if bound is not None and bound!=self.system_id:raise ValueError('resolution_system_mismatch')
        family=resolution.get('family')
        if family is not None and family!=self.family:raise ValueError('resolution_family_mismatch')
        out=deepcopy(resolution); out.setdefault('system_id',self.system_id); out.setdefault('family',self.family); return out

    def narrator_profile(self,rag,campaign_id):
        if self.module is None: raise ValueError('adapter_without_system_module')
        return {
            'system_id':self.system_id,'family':self.family,'lore_scope':self.lore_scope,
            'rules_ready':self.rules_ready(rag,campaign_id),'min_rule_authority':self.min_rule_authority,
            'mechanics':self.mechanics_profile(),'capabilities':list(self.capabilities()),
            'narrative_vocabulary':deepcopy(dict(self.module.narrative_vocabulary)),
            'ui_schema':deepcopy(dict(self.module.ui_schema)),
        }


class AdapterRegistry:
    def __init__(self,modules=None):
        self.modules=modules if modules is not None else SystemModuleRegistry()
        self._items={}
        # Installed package registries may intentionally contain only one system.
        # Never assume all builtin compatibility profiles are present.
        for module in self.modules.all():
            self._items[module.system_id]=Adapter(
                system_id=module.system_id,
                family=module.family,
                rules_required=True,
                lore_scope='mystara' if module.system_id=='mystara' else None,
                min_rule_authority=0.5,
                module=module,
            )

    def get(self,system_id):
        try: module=self.modules.get(system_id)
        except KeyError as exc: raise KeyError(system_id) from exc
        current=self._items.get(system_id)
        if current is None or current.module is not module:
            current=Adapter(
                system_id=module.system_id,family=module.family,rules_required=True,
                lore_scope='mystara' if module.system_id=='mystara' else None,
                min_rule_authority=0.5,module=module,
            )
            self._items[system_id]=current
        return current

    def supports(self,system_id,capability): return self.get(system_id).supports(capability)
    def all(self):
        order=[k for k in SUPPORTED if k in self.modules._items]
        order.extend(sorted(k for k in self.modules._items if k not in set(SUPPORTED)))
        return tuple(self.get(k) for k in order)
