from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass
from types import MappingProxyType

from .system_module import SystemModule


def _freeze_map(value, code):
    if not isinstance(value, dict):
        raise ValueError(code)
    return MappingProxyType(deepcopy(value))


@dataclass(frozen=True)
class ModuleExtension:
    module_id: str
    version: str
    system_id: str
    capabilities: frozenset[str]
    entity_schemas: MappingProxyType
    action_definitions: MappingProxyType
    roll_definitions: MappingProxyType
    rule_definitions: MappingProxyType
    effect_definitions: MappingProxyType
    ui_schema: MappingProxyType
    rag_sources: tuple
    narrative_vocabulary: MappingProxyType
    validation_tests: tuple

    def __post_init__(self):
        for value in (self.module_id, self.version, self.system_id):
            if not isinstance(value, str) or not value:
                raise ValueError('invalid_module_extension_identity')
        if not isinstance(self.capabilities, frozenset) or not all(isinstance(x, str) and x for x in self.capabilities):
            raise ValueError('invalid_module_extension_capabilities')
        for value in (
            self.entity_schemas, self.action_definitions, self.roll_definitions,
            self.rule_definitions, self.effect_definitions, self.ui_schema,
            self.narrative_vocabulary,
        ):
            if not isinstance(value, MappingProxyType):
                raise ValueError('mutable_module_extension_contract')
        if not all(isinstance(x, dict) for x in self.rag_sources):
            raise ValueError('invalid_module_extension_rag_sources')
        if not self.validation_tests or not all(isinstance(x, str) and x for x in self.validation_tests):
            raise ValueError('module_extension_without_validation_tests')

    def snapshot(self):
        return {
            'module_id': self.module_id,
            'version': self.version,
            'system_id': self.system_id,
            'capabilities': sorted(self.capabilities),
            'entity_schemas': deepcopy(dict(self.entity_schemas)),
            'action_definitions': deepcopy(dict(self.action_definitions)),
            'roll_definitions': deepcopy(dict(self.roll_definitions)),
            'rule_definitions': deepcopy(dict(self.rule_definitions)),
            'effect_definitions': deepcopy(dict(self.effect_definitions)),
            'ui_schema': deepcopy(dict(self.ui_schema)),
            'rag_sources': deepcopy(list(self.rag_sources)),
            'narrative_vocabulary': deepcopy(dict(self.narrative_vocabulary)),
            'validation_tests': list(self.validation_tests),
        }


class ModuleExtensionLoader:
    REQUIRED = {
        'module_id','version','system_id','capabilities','entity_schemas','action_definitions',
        'roll_definitions','rule_definitions','effect_definitions','ui_schema','rag_sources',
        'narrative_vocabulary','validation_tests',
    }

    def load(self, raw: dict) -> ModuleExtension:
        if not isinstance(raw, dict) or set(raw) != self.REQUIRED:
            raise ValueError('invalid_module_extension_manifest')
        for key in ('module_id','version','system_id'):
            if not isinstance(raw[key], str) or not raw[key]:
                raise ValueError('invalid_module_extension_identity')
        caps = raw['capabilities']
        if not isinstance(caps, (list, tuple, set)) or not all(isinstance(x, str) and x for x in caps):
            raise ValueError('invalid_module_extension_capabilities')
        rag = raw['rag_sources']
        tests = raw['validation_tests']
        if not isinstance(rag, (list, tuple)) or not all(isinstance(x, dict) for x in rag):
            raise ValueError('invalid_module_extension_rag_sources')
        if not isinstance(tests, (list, tuple)) or not tests or not all(isinstance(x, str) and x for x in tests):
            raise ValueError('module_extension_without_validation_tests')
        return ModuleExtension(
            module_id=raw['module_id'], version=raw['version'], system_id=raw['system_id'],
            capabilities=frozenset(caps),
            entity_schemas=_freeze_map(raw['entity_schemas'], 'invalid_module_extension_entities'),
            action_definitions=_freeze_map(raw['action_definitions'], 'invalid_module_extension_actions'),
            roll_definitions=_freeze_map(raw['roll_definitions'], 'invalid_module_extension_rolls'),
            rule_definitions=_freeze_map(raw['rule_definitions'], 'invalid_module_extension_rules'),
            effect_definitions=_freeze_map(raw['effect_definitions'], 'invalid_module_extension_effects'),
            ui_schema=_freeze_map(raw['ui_schema'], 'invalid_module_extension_ui'),
            rag_sources=tuple(deepcopy(rag)),
            narrative_vocabulary=_freeze_map(raw['narrative_vocabulary'], 'invalid_module_extension_narrative'),
            validation_tests=tuple(tests),
        )


def _merge_unique(base: dict, extra: dict, *, code: str) -> dict:
    overlap = set(base) & set(extra)
    if overlap:
        raise ValueError(code + ':' + ','.join(sorted(overlap)))
    return {**deepcopy(base), **deepcopy(extra)}


def compose_system_module(base: SystemModule, extensions) -> SystemModule:
    """Compose enabled Foundry-like Modules onto a System without arbitrary code.

    Extensions are additive by design. They may add new actions/rules/effects/UI and
    capabilities, but cannot silently override the System's canonical definitions.
    This keeps adventures/settings powerful without allowing a module to rewrite the
    game's mechanics behind a World owner's back.
    """
    if not isinstance(base, SystemModule):
        raise TypeError('invalid_system_module')
    caps = set(base.capabilities)
    entities = dict(base.entity_schemas)
    actions = dict(base.action_definitions)
    rolls = dict(base.roll_definitions)
    rules = dict(base.rule_definitions)
    effects = dict(base.effect_definitions)
    ui = dict(base.ui_schema)
    narrative = dict(base.narrative_vocabulary)
    rag = list(base.rag_sources)
    validation = list(base.validation_tests)

    for ext in extensions:
        if not isinstance(ext, ModuleExtension):
            raise TypeError('invalid_module_extension')
        if ext.system_id != base.system_id:
            raise ValueError('module_extension_system_mismatch:' + ext.module_id)
        caps |= set(ext.capabilities)
        entities = _merge_unique(entities, dict(ext.entity_schemas), code='module_entity_conflict')
        actions = _merge_unique(actions, dict(ext.action_definitions), code='module_action_conflict')
        rolls = _merge_unique(rolls, dict(ext.roll_definitions), code='module_roll_conflict')
        rules = _merge_unique(rules, dict(ext.rule_definitions), code='module_rule_conflict')
        effects = _merge_unique(effects, dict(ext.effect_definitions), code='module_effect_conflict')
        ui = _merge_unique(ui, dict(ext.ui_schema), code='module_ui_conflict')
        narrative = _merge_unique(narrative, dict(ext.narrative_vocabulary), code='module_narrative_conflict')
        rag.extend(deepcopy(list(ext.rag_sources)))
        validation.extend(f'{ext.module_id}:{name}' for name in ext.validation_tests)

    return SystemModule(
        base.system_id, base.family, base.version, frozenset(caps),
        MappingProxyType(entities), MappingProxyType(actions), MappingProxyType(rolls),
        MappingProxyType(rules), MappingProxyType(effects), base.inventory_model, base.combat_model,
        base.time_model, MappingProxyType(ui), tuple(rag), MappingProxyType(narrative),
        base.migrations, tuple(validation), base.atlas_contract,
    )
