from pathlib import Path
import sys

root = Path(sys.argv[1])
source = root / 'app/src/main/java/com/braseiro/dnd5e/AppDb.java'
text = source.read_text(encoding='utf-8')
old = "CREATE UNIQUE INDEX IF NOT EXISTS idx_guild_member_npc ON guild_members(world_profile,guild_id,npc_name COLLATE NOCASE) WHERE member_kind='npc' AND TRIM(npc_name)<>''"
new = "CREATE UNIQUE INDEX IF NOT EXISTS idx_guild_member_npc ON guild_members(world_profile,guild_id,npc_name COLLATE NOCASE) WHERE member_kind='npc' AND npc_name<>''"
if text.count(old) == 1:
    if "String npc=member.optString(\"npc_name\").trim();" not in text:
        raise SystemExit('GUILD_NPC_NAME_WRITER_NORMALIZATION_MISSING')
    source.write_text(text.replace(old, new), encoding='utf-8', newline='\n')
elif text.count(new) == 1 and text.count(old) == 0:
    print('SQLITE_PARTIAL_INDEX_COMPAT=ALREADY_APPLIED')
else:
    raise SystemExit(f'GUILD_NPC_PARTIAL_INDEX_SOURCE_DRIFT old={text.count(old)} new={text.count(new)}')
test = root / 'app/src/test/java/com/braseiro/dnd5e/Run245G05RhwOnHitConditionRobolectricTest.java'
test_text = test.read_text(encoding='utf-8')
old_assert = 'assertEquals(29,plain.optInt("applied_damage"));'
new_assert = 'assertEquals("legacy attack result: "+plain,29,plain.optInt("applied_damage"));'
if test_text.count(old_assert) == 1:
    test.write_text(test_text.replace(old_assert, new_assert), encoding='utf-8', newline='\n')
elif test_text.count(new_assert) != 1:
    raise SystemExit('LEGACY_FLOW_ASSERTION_SOURCE_DRIFT')
print('SQLITE_PARTIAL_INDEX_COMPAT=PASS; TRIMMED_WRITER_PRESERVED=YES; DB_SCHEMA_UNCHANGED=YES')
print('LEGACY_FLOW_ASSERTION_DIAGNOSTIC=PASS; EXPECTED_DAMAGE_UNCHANGED=29')
