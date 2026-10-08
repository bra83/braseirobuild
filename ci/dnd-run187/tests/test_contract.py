from pathlib import Path
import sys
p=Path(__file__).resolve().parents[1]
files={x.name:x.read_text(encoding='utf-8') for x in (p/'java').glob('*.java')}
assert len(files)==3
m=files['HumanMasterDynamicDirector.java']
e=files['SpontaneousEventEngine.java']
s=files['DynamicMasterState.java']
checks={
  'no_base_reset':'deleteDatabase' not in '\n'.join(files.values()),
  'no_mechanical_state_update':all(x not in m for x in ['saveSceneState','updateCharacterSheet','applyDamage','updateNpcMemory']),
  'stable_profile_location':'locations' in s and 'location' in m,
  'dynamic_tone':'chooseTone' in m and 'recent_tones' in m,
  'surprise_schedule':'surprise_pressure' in e and 'event_cooldown' in e,
  'event_categories':all(x in e for x in ['social','humor','mistério','oportunidade','consequência']),
  'canonical_guard':'canonicalLocked' in m and 'protectedScene' in e,
  'nonadvancing_guard':'nonAdvancing' in m and 'nonAdvancing' in e,
  'npc_personality_persistence':'personality_json' in m and '300 perfis' in m,
  'ptbr':'constrangedor' in m,
  'no_imposed_rolls':'nunca aplique custo/rolagem automaticamente' in m,
  'no_save_schema_migration':'SharedPreferences' in s and 'CREATE TABLE' not in '\n'.join(files.values()),
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'),k)
if not all(checks.values()):sys.exit('FAIL')
print('RUN187_STATIC_TESTS_PASS')
