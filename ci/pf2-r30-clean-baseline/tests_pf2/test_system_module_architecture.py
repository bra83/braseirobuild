from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'android/app/src/main/java'

def txt(rel): return (JAVA/rel).read_text(encoding='utf-8')

def test_shared_contract_exists():
    s=txt('com/braseiro/core/system/GameSystemModule.java')
    for hook in ['rulesAuthorityPrompt','narrativeDirective','visualIdentityDirective','validateCharacter','resolveCheck','turnBudget','campaignIsolationKey']:
        assert hook in s

def test_pf2_adapter_has_no_dnd_fallback():
    s=txt('com/braseiro/pathfinder2e/Pathfinder2eSystemModule.java').lower()
    assert 'return validator.validate' in s
    assert 'return rules.resolvecheck' in s
    assert 'dndrulesengine' not in s
    assert 'mystararulesengine' not in s

def test_main_activity_uses_system_module():
    s=txt('com/braseiro/pathfinder2e/MainActivity.java')
    assert 'GameSystemModule activeSystemModule' in s
    assert 'new SystemModuleRegistry().register(new Pathfinder2eSystemModule())' in s
    assert 'activeSystemModule.rulesAuthorityPrompt()' in s
    assert 'activeSystemModule.visualIdentityDirective()' in s

def test_pf2_validator_not_5e_profile_switch():
    s=txt('com/braseiro/pathfinder2e/CharacterRulesValidator.java').lower()
    assert 'backgrounds_2014' not in s
    assert 'backgrounds_2024' not in s
    assert 'profile.contains("2024")' not in s
    assert 'pf2_remaster_modifiers' in s

def test_pf2_activity_does_not_instantiate_dnd_character_generator():
    s=txt('com/braseiro/pathfinder2e/MainActivity.java')
    assert 'new CharacterSheetGenerator(' not in s
    assert 'new Pathfinder2eCharacterSheetGenerator()' in s
    g=txt('com/braseiro/pathfinder2e/Pathfinder2eCharacterSheetGenerator.java').lower()
    assert 'dndrulesengine' not in g
    assert 'backgrounds_2014' not in g
    assert 'pf2_remaster_modifiers' in g
