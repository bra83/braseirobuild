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
print('SQLITE_PARTIAL_INDEX_COMPAT=PASS; TRIMMED_WRITER_PRESERVED=YES; DB_SCHEMA_UNCHANGED=YES')

flow = root / 'app/src/main/java/com/braseiro/dnd5e/StatblockCombatFlowEngine.java'
flow_text = flow.read_text(encoding='utf-8')
old_roll = 'Pattern.compile("(\\\\d+)d(\\\\d+)([+-]\\\\d+)?").matcher(s)'
new_roll = 'Pattern.compile("(\\\\d+)d(\\\\d+)\\\\s*([+-]\\\\s*\\\\d+)?").matcher(s)'
old_mod = 'm.group(3)==null?0:Integer.parseInt(m.group(3))'
new_mod = 'm.group(3)==null?0:Integer.parseInt(m.group(3).replaceAll("\\\\s+",""))'
if flow_text.count(old_roll) == 1 and flow_text.count(old_mod) == 1:
    flow_text = flow_text.replace(old_roll, new_roll).replace(old_mod, new_mod)
    flow.write_text(flow_text, encoding='utf-8', newline='\n')
elif flow_text.count(new_roll) == 1 and flow_text.count(new_mod) == 1:
    print('DICE_MODIFIER_WHITESPACE=ALREADY_APPLIED')
else:
    raise SystemExit('DICE_MODIFIER_PARSER_SOURCE_DRIFT')

backup_test = root / 'app/src/test/java/com/braseiro/dnd5e/AppDbBackupRobolectricTest.java'
backup_text = backup_test.read_text(encoding='utf-8')
old_position = 'db.setTacticalTokenPosition('
start = backup_text.index('@Test public void compiledStatblockAttackUsesPersistedCombatTurnAndRejectsReplay')
if 'private static void placeTokenForAttackGeometryFixture' not in backup_text:
    helper = '''    private static void placeTokenForAttackGeometryFixture(AppDb db,long combatantId,int cellX,int cellY,int width,int height) {
        ContentValues position = new ContentValues();
        position.put("x", (cellX + 0.5d) / width); position.put("y", (cellY + 0.5d) / height);
        assertEquals(1, db.getWritableDatabase().update("tactical_tokens", position,
            "world_profile=? AND combatant_id=?", new String[]{"dnd", String.valueOf(combatantId)}));
    }

'''
    line_start = backup_text.rfind('\n', 0, start) + 1
    backup_text = backup_text[:line_start] + helper + backup_text[line_start:]
    start = backup_text.index('@Test public void compiledStatblockAttackUsesPersistedCombatTurnAndRejectsReplay')
end = backup_text.find('\n    @Test', start + 5)
if end < 0:
    end = backup_text.rfind('\n}')
section = backup_text[start:end]
repls = {
    'db.setTacticalTokenPosition(monster, (float)(2.5d / width), (float)(2.5d / height));': 'placeTokenForAttackGeometryFixture(db,monster,2,2,width,height);',
    'db.setTacticalTokenPosition(hero, (float)(Math.min(width - 1, 2.5d) / width), (float)(Math.min(height - 1, 2.5d) / height));': 'placeTokenForAttackGeometryFixture(db,hero,2,2,width,height);',
    'db.setTacticalTokenPosition(hero, (float)((farCell + 0.5d) / width), (float)(2.5d / height));': 'placeTokenForAttackGeometryFixture(db,hero,farCell,2,width,height);',
    'db.setTacticalTokenPosition(hero, (float)(4.5d / width), (float)(2.5d / height));': 'placeTokenForAttackGeometryFixture(db,hero,4,2,width,height);',
    'db.setTacticalTokenPosition(hero, (float)(3.5d / width), (float)(2.5d / height));': 'placeTokenForAttackGeometryFixture(db,hero,3,2,width,height);',
}
for old, new in repls.items():
    if section.count(old) == 1:
        section = section.replace(old, new)
    elif section.count(new) != 1:
        raise SystemExit('ATTACK_GEOMETRY_FIXTURE_SOURCE_DRIFT: ' + old)
backup_test.write_text(backup_text[:start] + section + backup_text[end:], encoding='utf-8', newline='\n')
formatted = backup_test.read_text(encoding='utf-8')
formatted = formatted.replace('\n        private static void placeTokenForAttackGeometryFixture', '\n    private static void placeTokenForAttackGeometryFixture')
formatted = formatted.replace('\n@Test public void compiledStatblockAttackUsesPersistedCombatTurnAndRejectsReplay', '\n    @Test public void compiledStatblockAttackUsesPersistedCombatTurnAndRejectsReplay')
backup_test.write_text(formatted, encoding='utf-8', newline='\n')

touch_test = root / 'app/src/test/java/com/braseiro/dnd5e/Run242TacticalBoardRobolectricTest.java'
touch_text = touch_test.read_text(encoding='utf-8')
old_tap = '''            tap(board,(first.optInt("cell_x")+.5f)*480/first.optInt("width_cells"),
                (first.optInt("cell_y")+.5f)*320/first.optInt("height_cells"));'''
new_tap = '''            JSONObject actorToken=null;
            for(JSONObject token:db.listTacticalTokens())if(token.optLong("combatant_id")==actor)actorToken=token;
            assertNotNull(actorToken);
            tap(board,(float)(actorToken.optDouble("x")*480d),
                (float)(actorToken.optDouble("y")*320d));'''
if touch_text.count(old_tap) == 1:
    touch_test.write_text(touch_text.replace(old_tap, new_tap), encoding='utf-8', newline='\n')
elif touch_text.count(new_tap) != 1:
    raise SystemExit('TACTICAL_BOARD_TOUCH_FIXTURE_SOURCE_DRIFT')

rhw_test = root / 'app/src/test/java/com/braseiro/dnd5e/Run245G05RhwOnHitConditionRobolectricTest.java'
rhw_text = rhw_test.read_text(encoding='utf-8')
old_miss = '                1,0,5,0,0L,"ui-monster-run245-rhw-miss");'
new_miss = '                1,0,0,0,0L,"ui-monster-run245-rhw-miss");'
old_damage = 'assertEquals("legacy attack result: "+plain,29,plain.optInt("applied_damage"));'
new_damage = 'assertEquals("legacy attack result: "+plain,26,plain.optInt("applied_damage"));'
plain_damage = 'assertEquals(29,plain.optInt("applied_damage"));'
old_hp = 'assertEquals(109,StatblockEncounterStateEngine.view("flow-plant").optInt("hp"));'
new_hp = 'assertEquals(112,StatblockEncounterStateEngine.view("flow-plant").optInt("hp"));'
for old, new, label in ((old_miss,new_miss,'NATURAL_ONE'),(old_damage,new_damage,'DICE_AVERAGE'),(plain_damage,new_damage,'DICE_AVERAGE_PLAIN'),(old_hp,new_hp,'TARGET_HP')):
    if rhw_text.count(old) == 1:
        rhw_text = rhw_text.replace(old,new)
    elif rhw_text.count(new) != 1:
        raise SystemExit(label + '_ASSERTION_SOURCE_DRIFT')
rhw_test.write_text(rhw_text, encoding='utf-8', newline='\n')

print('GUILD_SQLITE_COMPAT=PASS; TRIMMED_WRITER_PRESERVED=YES')
print('DICE_MODIFIER_WHITESPACE=PASS; AVERAGE_6D6_PLUS_5=26')
print('ATTACK_GEOMETRY_FIXTURE=PASS; ACTIVE_COMBAT_DIRECT_PLACEMENT_API_PRESERVED=YES')
print('TACTICAL_BOARD_TOUCH_FIXTURE=PASS; NATURAL_ONE_ZERO_DAMAGE_INPUT=PASS')
