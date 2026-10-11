from __future__ import annotations
from copy import deepcopy


class ComponentRegistry:
    BUILTIN={
        'attribute_number','attribute_bar','resource_track','resource_boxes','skill_list','trait_list',
        'condition_track','inventory.weight','inventory.slot_zones','inventory.encumbrance','inventory.load',
        'inventory.cards','combat.armor_class','combat.hit_locations','combat.auto_hit','combat.pool',
        'combat.stances','combat.conditions','combat.armor_damage','dice_pool','clock','relationship_track','journey_track'
    }
    def __init__(self,renderers=None): self.renderers=dict(renderers or {})
    def validate_component(self,name):
        if name not in self.BUILTIN and name not in self.renderers: raise ValueError('unknown_ui_component')
        return True
    def validate_schema(self,schema):
        if not isinstance(schema,dict): raise ValueError('invalid_ui_schema')
        for components in schema.values():
            if not isinstance(components,list): raise ValueError('invalid_ui_schema')
            for name in components:self.validate_component(name)
        return True


class UiProjection:
    def project(self,state,module):
        return {'state_version':state.state_version,'tick':state.tick,'location':state.location,'player':deepcopy(state.player_state),'ui_schema':module.snapshot()['ui_schema']}


class UiPhase:
    VALUES={'IDLE','INTERPRETING','RESOLVING','WAITING_FOR_ROLL','WAITING_FOR_TARGET','WAITING_FOR_CHOICE','WAITING_FOR_REACTION','WAITING_FOR_PUSH','WAITING_FOR_CONFIRMATION','NARRATING','ERROR'}
    @classmethod
    def validate(cls,value):
        if value not in cls.VALUES: raise ValueError('invalid_ui_phase')
        return value


class ThemePolicy:
    def resolve(self,semantic,theme):
        if not isinstance(theme,dict): raise ValueError('invalid_theme')
        mapping=theme.get('semantics',{})
        return deepcopy(mapping.get(semantic,{'token':semantic}))
