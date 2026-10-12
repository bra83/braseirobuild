from __future__ import annotations

from copy import deepcopy
from dataclasses import dataclass, field
from types import MappingProxyType

from .migrations import MigrationRegistry
from .numeric import is_finite_number


@dataclass(frozen=True)
class TimeModel:
    """System-owned game-time semantics.

    Values are expressed in seconds only as a neutral transport unit; the core never
    assumes that a turn, round, watch or travel leg has the same duration across RPGs.
    """

    round_seconds: int | None = None
    exploration_turn_seconds: int | None = None
    travel_turn_seconds: int | None = None
    calendar_day_seconds: int = 86400

    def __post_init__(self):
        for name in ('round_seconds', 'exploration_turn_seconds', 'travel_turn_seconds', 'calendar_day_seconds'):
            value = getattr(self, name)
            if value is not None and (not isinstance(value, int) or isinstance(value, bool) or value <= 0):
                raise ValueError('invalid_time_model')

    def duration_for(self, unit: str, amount: int | float = 1):
        if not isinstance(unit, str) or not unit:
            raise ValueError('invalid_time_unit')
        if not is_finite_number(amount) or amount < 0:
            raise ValueError('invalid_time_amount')
        table = {
            'round': self.round_seconds,
            'exploration_turn': self.exploration_turn_seconds,
            'travel_turn': self.travel_turn_seconds,
            'day': self.calendar_day_seconds,
        }
        duration=table.get(unit)
        if duration is None:
            raise ValueError('unsupported_time_unit')
        return duration * amount


@dataclass(frozen=True)
class SystemModule:
    system_id: str
    family: str
    version: str
    capabilities: frozenset[str]
    entity_schemas: MappingProxyType
    action_definitions: MappingProxyType
    roll_definitions: MappingProxyType
    rule_definitions: MappingProxyType
    effect_definitions: MappingProxyType
    inventory_model: MappingProxyType
    combat_model: MappingProxyType
    time_model: TimeModel
    ui_schema: MappingProxyType
    rag_sources: tuple
    narrative_vocabulary: MappingProxyType
    migrations: tuple
    validation_tests: tuple
    atlas_contract: MappingProxyType = field(default_factory=lambda: MappingProxyType({
        'ownership': 'system_package',
        'formats': ['directory-json-v1', 'barbara-atlas-bundle-v1'],
        'bundle_manifest': 'atlas-manifest.json',
    }))

    def __post_init__(self):
        if not all(isinstance(value,str) and value for value in (self.system_id,self.family,self.version)):
            raise ValueError('invalid_system_module_identity')
        if not isinstance(self.capabilities,frozenset) or not self.capabilities or not all(isinstance(value,str) and value for value in self.capabilities):
            raise ValueError('invalid_system_module_capabilities')
        required_maps = (
            self.entity_schemas, self.action_definitions, self.roll_definitions,
            self.rule_definitions, self.effect_definitions, self.inventory_model,
            self.combat_model, self.ui_schema, self.narrative_vocabulary,
            self.atlas_contract,
        )
        if any(not isinstance(v, MappingProxyType) for v in required_maps):
            raise ValueError('mutable_system_module_contract')
        if not self.entity_schemas or not self.action_definitions or not self.roll_definitions:
            raise ValueError('incomplete_system_module')
        if not self.validation_tests:
            raise ValueError('system_module_without_validation_tests')

    def supports(self, capability: str) -> bool:
        if not isinstance(capability, str) or not capability:
            raise ValueError('invalid_capability')
        return capability in self.capabilities

    def snapshot(self):
        return {
            'system_id': self.system_id,
            'family': self.family,
            'version': self.version,
            'capabilities': sorted(self.capabilities),
            'entity_schemas': deepcopy(dict(self.entity_schemas)),
            'action_definitions': deepcopy(dict(self.action_definitions)),
            'roll_definitions': deepcopy(dict(self.roll_definitions)),
            'rule_definitions': deepcopy(dict(self.rule_definitions)),
            'effect_definitions': deepcopy(dict(self.effect_definitions)),
            'inventory_model': deepcopy(dict(self.inventory_model)),
            'combat_model': deepcopy(dict(self.combat_model)),
            'time_model': {
                'round_seconds': self.time_model.round_seconds,
                'exploration_turn_seconds': self.time_model.exploration_turn_seconds,
                'travel_turn_seconds': self.time_model.travel_turn_seconds,
                'calendar_day_seconds': self.time_model.calendar_day_seconds,
            },
            'ui_schema': deepcopy(dict(self.ui_schema)),
            'rag_sources': deepcopy(list(self.rag_sources)),
            'narrative_vocabulary': deepcopy(dict(self.narrative_vocabulary)),
            'migrations': deepcopy(list(self.migrations)),
            'validation_tests': deepcopy(list(self.validation_tests)),
            'atlas_contract': deepcopy(dict(self.atlas_contract)),
        }


def _freeze(value):
    if isinstance(value, MappingProxyType):
        return value
    if not isinstance(value, dict):
        raise ValueError('system_module_mapping_required')
    return MappingProxyType(deepcopy(value))


def _roll_contract(model):
    """Return safe Dice AST + system-owned interpretation/binding metadata.

    Bindings point only at canonical state/context paths. The RulesKernel resolves
    them before a RollRequest is exposed to a host, so UI code cannot choose pool
    size, target number or damage die as part of a result submission.
    """
    target_ref='state.player_state.roll_context.target'
    if model in {'d20','mixed'}:
        return {'model':model,'expression':{'op':'die','sides':20},'bindings':{},'interpretation':{'kind':'compare','comparator':'>=','target':'system_rule','target_ref':target_ref}}
    if model in {'d20_roll_under','save_roll_under'}:
        return {'model':model,'expression':{'op':'die','sides':20},'bindings':{},'interpretation':{'kind':'compare','comparator':'<=','target':'system_rule','target_ref':target_ref}}
    if model=='3d6_roll_under':
        return {'model':model,'expression':{'op':'sum','args':[{'op':'dice','count':3,'sides':6}]},'bindings':{},'interpretation':{'kind':'compare','comparator':'<=','target':'system_rule','target_ref':target_ref,'margin_direction':'target_minus_roll'}}
    if model in {'2d6','2d6_target','2d6_skill'}:
        return {'model':model,'expression':{'op':'sum','args':[{'op':'dice','count':2,'sides':6}]},'bindings':{},'interpretation':{'kind':'compare','comparator':'>=','target':'system_rule','target_ref':target_ref}}
    if model in {'d6_pool','year_zero_pool'}:
        return {'model':model,'expression':{'op':'dice','count':{'op':'var','name':'pool'},'sides':6},'bindings':{'pool':'state.player_state.roll_context.pool'},'interpretation':{'kind':'count_successes','threshold':6,'minimum_successes':1}}
    if model=='feat_die_plus_success_dice':
        return {'model':model,'expression':{'op':'sum','args':[{'op':'die','sides':12},{'op':'dice','count':{'op':'var','name':'success_dice'},'sides':6}]},'bindings':{'success_dice':'state.player_state.roll_context.success_dice'},'interpretation':{'kind':'compare','comparator':'>=','target':'system_rule','target_ref':target_ref}}
    if model=='target_number':
        return {'model':model,'expression':{'op':'sum','args':[{'op':'die','sides':12},{'op':'dice','count':{'op':'var','name':'success_dice'},'sides':6}]},'bindings':{'success_dice':'state.player_state.roll_context.success_dice'},'interpretation':{'kind':'compare','comparator':'>=','target':'system_rule','target_ref':target_ref}}
    return {'model':model,'expression':{'op':'die','sides':{'op':'var','name':'sides'}},'bindings':{'sides':'state.player_state.roll_context.sides'},'interpretation':{'kind':'system_rule'}}


def _module(system_id, family, capabilities, roll_model, skill_model, combat_model, inventory, time_model, *, special=()):
    common = {
        'rules.deterministic', 'rules.provenance', 'state.events', 'actions.interruptible',
        'dice.ast', 'rules.ast', 'time.model', 'narrative.profile', 'context.environment',
        'narrative.world_introduction', 'atlas.dynamic_poi', 'audio.event_director',
    }
    caps = frozenset(common | set(capabilities) | set(special))
    entity_schemas = {
        'Actor': {'required': ['id'], 'optional': ['attributes', 'resources', 'traits', 'conditions', 'position']},
        'Item': {'required': ['id'], 'optional': ['quantity', 'condition', 'container', 'tags']},
        'Location': {'required': ['id'], 'optional': ['terrain', 'geometry', 'connections', 'environment']},
        'CampaignPoi': {'required': ['id', 'name', 'logical_map_id', 'layer'], 'optional': ['hex_id', 'canonical_position', 'origin', 'hidden', 'discovered', 'status', 'parent_poi_id', 'owner_npc_id', 'faction_id', 'development_level', 'history']},
    }
    actions = {
        'observe': {'kind': 'investigation', 'uncertainty': 'system_rule'},
        'interact': {'kind': 'action', 'uncertainty': 'system_rule'},
        'move': {'kind': 'movement', 'uncertainty': 'system_rule'},
        'attack': {'kind': 'combat', 'uncertainty': 'system_rule'},
    }
    rolls = {
        'primary': _roll_contract(roll_model),
        'skill': _roll_contract(skill_model),
        'damage': {'model':'damage_die','expression':{'op':'die','sides':{'op':'var','name':'damage_sides'}},'bindings':{'damage_sides':'state.player_state.roll_context.damage_sides'},'interpretation':{'kind':'damage_total'}},
    }
    # Executable-safe rule AST declarations. The core evaluates these as data;
    # modules never provide arbitrary Python callbacks. Roll expressions remain
    # separate from outcome semantics.
    attack_mechanic = 'damage' if 'combat.auto_hit_damage' in caps else 'attack'
    rules = {
        'attack': {
            'rule_id': f'{system_id}.attack',
            'when': {'eq': [{'ref':'intent.action'}, 'attack']},
            'requirement': 'roll_required',
            'mechanic': attack_mechanic,
            'roll': {'definition':('damage' if attack_mechanic=='damage' else 'primary')},
            'time_cost': {'unit':'round','amount':1},
            'source_refs': ['module:core.attack'],
            'priority': 100,
        },
        'social': {
            'rule_id': f'{system_id}.social',
            'when': {'in': [{'ref':'intent.social_intent'}, ['persuade','deceive','intimidate','bargain']]},
            'requirement': 'roll_required',
            'mechanic': 'social',
            'roll': {'definition':'skill'},
            'source_refs': ['module:core.social'],
            'priority': 90,
        },
        'basic_dialogue': {
            'rule_id': f'{system_id}.conversation',
            'when': {'eq': [{'ref':'intent.intent_type'}, 'dialogue']},
            'requirement': 'no_roll',
            'mechanic': 'conversation',
            'source_refs': ['module:core.conversation'],
            'priority': 10,
        },
    }
    effects = {
        'damage': {'typed': True}, 'condition': {'typed': True}, 'movement': {'typed': True},
        'resource': {'typed': True}, 'time': {'typed': True},
    }
    ui = {
        'character': ['attribute_number', 'resource_track', 'trait_list'],
        'inventory': [inventory.get('component', 'inventory.generic')],
        'combat': [combat_model.get('component', 'combat.generic')],
    }
    vocab = {'system_name': system_id, 'family': family, 'forbidden_hardcodes': ['fantasy_medieval']}
    return SystemModule(
        system_id, family, '1.0', caps, _freeze(entity_schemas), _freeze(actions), _freeze(rolls),
        _freeze(rules), _freeze(effects), _freeze(inventory), _freeze(combat_model), time_model,
        _freeze(ui), tuple(), _freeze(vocab), tuple(),
        ('module_contract', 'dice_binding', 'time_binding', 'ui_schema'),
    )


BUILTIN_MODULES = (
    _module('dnd', 'dnd', {'roll.d20','roll.advantage','inventory.weight','combat.armor_class','combat.saves'}, 'd20', 'd20', {'model':'d20','component':'combat.armor_class'}, {'model':'weight','component':'inventory.weight'}, TimeModel(round_seconds=6, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('mystara', 'dnd', {'roll.d20','inventory.weight','combat.armor_class','combat.saves'}, 'd20', 'd20', {'model':'d20','component':'combat.armor_class'}, {'model':'weight','component':'inventory.weight'}, TimeModel(round_seconds=10, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('mausritter', 'mausritter', {'roll.d20_under','inventory.slot_zones','combat.auto_hit_damage','conditions.slot_pressure'}, 'd20_roll_under', 'save_roll_under', {'model':'auto_hit_damage','component':'combat.auto_hit'}, {'model':'slot_zones','component':'inventory.slot_zones','zones':{'paws':2,'body':2,'pack':6}}, TimeModel(round_seconds=10, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('forbidden_lands', 'year_zero', {'roll.d6_pool','roll.push','resources.year_zero','combat.year_zero'}, 'd6_pool', 'year_zero_pool', {'model':'year_zero_pool','component':'combat.pool'}, {'model':'encumbrance','component':'inventory.encumbrance'}, TimeModel(round_seconds=15, exploration_turn_seconds=900, travel_turn_seconds=21600)),
    _module('the_one_ring', 'the_one_ring', {'roll.feat_die','roll.success_dice','combat.stances','resources.hope','resources.shadow','journey.track'}, 'feat_die_plus_success_dice', 'target_number', {'model':'stance_and_attack_roll','component':'combat.stances'}, {'model':'load','component':'inventory.load'}, TimeModel(round_seconds=10, exploration_turn_seconds=600, travel_turn_seconds=86400)),
    _module('gurps', 'gurps', {'roll.3d6_under','combat.active_defense','combat.hit_locations','encumbrance.weight','resources.fp_hp'}, '3d6_roll_under', '3d6_roll_under', {'model':'3d6_attack_defense','component':'combat.hit_locations'}, {'model':'weight','component':'inventory.weight'}, TimeModel(round_seconds=1, exploration_turn_seconds=60, travel_turn_seconds=3600)),
    _module('worlds_without_number', 'xwn', {'roll.2d6_skill','roll.d20_attack','combat.shock','inventory.encumbrance'}, 'mixed', '2d6_skill', {'model':'d20_attack','component':'combat.armor_class'}, {'model':'encumbrance','component':'inventory.encumbrance'}, TimeModel(round_seconds=6, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('stars_without_number', 'xwn', {'roll.2d6_skill','roll.d20_attack','vehicles.starships','inventory.encumbrance'}, 'mixed', '2d6_skill', {'model':'d20_attack','component':'combat.armor_class'}, {'model':'encumbrance','component':'inventory.encumbrance'}, TimeModel(round_seconds=6, exploration_turn_seconds=600, travel_turn_seconds=21600)),
    _module('cities_without_number', 'xwn', {'roll.2d6_skill','roll.d20_attack','cyberware','inventory.encumbrance'}, 'mixed', '2d6_skill', {'model':'d20_attack','component':'combat.armor_class'}, {'model':'encumbrance','component':'inventory.encumbrance'}, TimeModel(round_seconds=6, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('ashes_without_number', 'xwn', {'roll.2d6_skill','roll.d20_attack','survival.resources','inventory.encumbrance'}, 'mixed', '2d6_skill', {'model':'d20_attack','component':'combat.armor_class'}, {'model':'encumbrance','component':'inventory.encumbrance'}, TimeModel(round_seconds=6, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('tales_from_the_loop', 'year_zero', {'roll.d6_pool','conditions.drive','combat.condition_driven'}, 'd6_pool', 'year_zero_pool', {'model':'condition_driven','component':'combat.conditions'}, {'model':'narrative_items','component':'inventory.cards'}, TimeModel(round_seconds=10, exploration_turn_seconds=600, travel_turn_seconds=3600)),
    _module('traveller_2e', 'traveller', {'roll.2d6_target','combat.armor_damage','vehicles.starships','characteristics.damage'}, '2d6', '2d6_target', {'model':'2d6_attack','component':'combat.armor_damage'}, {'model':'mass','component':'inventory.weight'}, TimeModel(round_seconds=6, exploration_turn_seconds=600, travel_turn_seconds=21600)),
)


class SystemModuleRegistry:
    def __init__(self, modules=None, migration_registry=None):
        items = tuple(BUILTIN_MODULES if modules is None else modules)
        self._items = {}
        self.migrations = migration_registry or MigrationRegistry()
        self._migration_specs=set()
        for module in items:
            if not isinstance(module, SystemModule):
                raise ValueError('invalid_system_module')
            if module.system_id in self._items:
                raise ValueError('duplicate_system_module')
            SystemModuleConformance().validate(module)
            self._register_migrations(module)
            self._items[module.system_id] = module

    def _register_migrations(self,module):
        for definition in module.migrations:
            if not isinstance(definition,dict): raise ValueError('invalid_module_migration')
            signature=(module.system_id,repr(sorted(definition.items(),key=lambda x:x[0])))
            if signature in self._migration_specs: continue
            self.migrations.register_declarative(module.system_id,deepcopy(definition))
            self._migration_specs.add(signature)

    def get(self, system_id):
        try:
            return self._items[system_id]
        except KeyError as exc:
            raise KeyError(system_id) from exc

    def supports(self, system_id, capability):
        return self.get(system_id).supports(capability)

    def all(self):
        return tuple(self._items[k] for k in sorted(self._items))

    def install(self,module,*,replace=False,conformance=None):
        if not isinstance(module,SystemModule): raise ValueError('invalid_system_module')
        if module.system_id in self._items and not replace: raise ValueError('duplicate_system_module')
        (conformance or SystemModuleConformance()).validate(module)
        # Migrations are registered before replacing the active version. If the
        # migration declaration is invalid the old module remains installed.
        self._register_migrations(module)
        self._items[module.system_id]=module
        return module

    def remove(self,system_id):
        if system_id not in self._items: raise KeyError(system_id)
        return self._items.pop(system_id)

    def migrate_document(self,document,system_id,from_version,to_version=None):
        module=self.get(system_id)
        target=str(to_version or module.version)
        source=str(from_version)
        if source==target: return deepcopy(document)
        return self.migrations.migrate(document,system_id,source,target)


class SystemModuleLoader:
    """Loads installable declarative system packages from validated mappings.

    The loader accepts data only. Executable Python callbacks, arbitrary class names
    and persistence code are deliberately not part of the package contract.
    """
    REQUIRED = {
        'system_id','family','version','capabilities','entity_schemas','action_definitions',
        'roll_definitions','rule_definitions','effect_definitions','inventory_model',
        'combat_model','time_model','ui_schema','rag_sources','narrative_vocabulary',
        'migrations','validation_tests'
    }
    OPTIONAL = {'atlas_contract'}

    def load(self, manifest):
        if not isinstance(manifest, dict):
            raise ValueError('invalid_system_module_manifest')
        unknown = set(manifest) - self.REQUIRED - self.OPTIONAL
        missing = self.REQUIRED - set(manifest)
        if unknown:
            raise ValueError('unknown_system_module_fields:'+','.join(sorted(unknown)))
        if missing:
            raise ValueError('missing_system_module_fields:'+','.join(sorted(missing)))
        capabilities = manifest['capabilities']
        if not isinstance(capabilities, (list, tuple, set)) or not all(isinstance(x,str) and x for x in capabilities):
            raise ValueError('invalid_system_module_capabilities')
        for key in ('system_id','family','version'):
            if not isinstance(manifest[key],str) or not manifest[key]:
                raise ValueError('invalid_system_module_identity')
        time = manifest['time_model']
        if not isinstance(time, dict):
            raise ValueError('invalid_time_model')
        time_unknown = set(time) - {'round_seconds','exploration_turn_seconds','travel_turn_seconds','calendar_day_seconds'}
        if time_unknown:
            raise ValueError('unknown_time_model_fields')
        tm = TimeModel(**deepcopy(time))
        rag_sources = manifest['rag_sources']
        migrations = manifest['migrations']
        validation_tests = manifest['validation_tests']
        atlas_contract = manifest.get('atlas_contract', {
            'ownership': 'system_package',
            'formats': ['directory-json-v1', 'barbara-atlas-bundle-v1'],
            'bundle_manifest': 'atlas-manifest.json',
        })
        if not isinstance(rag_sources, (list, tuple)) or not all(isinstance(x, dict) for x in rag_sources):
            raise ValueError('invalid_module_rag_sources')
        if not isinstance(migrations, (list, tuple)) or not all(isinstance(x, dict) for x in migrations):
            raise ValueError('invalid_module_migrations')
        if not isinstance(validation_tests, (list, tuple)) or not all(isinstance(x, str) and x for x in validation_tests):
            raise ValueError('invalid_module_validation_tests')
        if not isinstance(atlas_contract, dict):
            raise ValueError('invalid_module_atlas_contract')
        return SystemModule(
            manifest['system_id'], manifest['family'], manifest['version'], frozenset(capabilities),
            _freeze(manifest['entity_schemas']), _freeze(manifest['action_definitions']), _freeze(manifest['roll_definitions']),
            _freeze(manifest['rule_definitions']), _freeze(manifest['effect_definitions']), _freeze(manifest['inventory_model']),
            _freeze(manifest['combat_model']), tm, _freeze(manifest['ui_schema']), tuple(deepcopy(rag_sources)),
            _freeze(manifest['narrative_vocabulary']), tuple(deepcopy(migrations)), tuple(validation_tests),
            _freeze(atlas_contract),
        )


class SystemModuleConformance:
    """Checks that an installed module can participate in the Barbara runtime."""
    REQUIRED_CAPABILITIES={'rules.deterministic','rules.provenance','state.events','actions.interruptible','dice.ast','time.model','narrative.profile','context.environment'}
    REQUIRED_ENTITIES={'Actor','Item','Location'}

    def validate(self, module, component_registry=None):
        if not isinstance(module, SystemModule):
            raise ValueError('invalid_system_module')
        missing_caps=self.REQUIRED_CAPABILITIES-set(module.capabilities)
        if missing_caps:
            raise ValueError('module_missing_core_capability:'+','.join(sorted(missing_caps)))
        missing_entities=self.REQUIRED_ENTITIES-set(module.entity_schemas)
        if missing_entities:
            raise ValueError('module_missing_core_entity:'+','.join(sorted(missing_entities)))
        if component_registry is not None:
            component_registry.validate_schema(module.snapshot()['ui_schema'])
        for action_id, definition in module.action_definitions.items():
            if not isinstance(action_id,str) or not action_id or not isinstance(definition,dict):
                raise ValueError('invalid_module_action_definition')
        for roll_id, definition in module.roll_definitions.items():
            if not isinstance(roll_id,str) or not roll_id or not isinstance(definition,dict) or not definition.get('model'):
                raise ValueError('invalid_module_roll_definition')
        if module.supports('rules.ast'):
            from .rules_ast import RuleExpressionRuntime
            runtime=RuleExpressionRuntime()
            for rule_id, definition in module.rule_definitions.items():
                if not isinstance(rule_id,str) or not rule_id or not isinstance(definition,dict):
                    raise ValueError('invalid_module_rule_definition')
                runtime.validate_rule(definition)
        atlas = dict(module.atlas_contract)
        if set(atlas) - {'ownership', 'formats', 'bundle_manifest'}:
            raise ValueError('unknown_module_atlas_contract_field')
        if atlas.get('ownership') != 'system_package':
            raise ValueError('invalid_module_atlas_ownership')
        formats = atlas.get('formats')
        if not isinstance(formats, list) or not formats or not all(isinstance(x, str) and x for x in formats):
            raise ValueError('invalid_module_atlas_formats')
        manifest_name = atlas.get('bundle_manifest', 'atlas-manifest.json')
        if not isinstance(manifest_name, str) or not manifest_name or '/' in manifest_name or '\\' in manifest_name:
            raise ValueError('invalid_module_atlas_manifest')
        return {'system_id':module.system_id,'version':module.version,'valid':True,'validation_tests':tuple(module.validation_tests)}
