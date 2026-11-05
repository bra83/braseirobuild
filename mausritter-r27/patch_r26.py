#!/usr/bin/env python3
"""Patch the exact, SHA-verified R26 source in place; fail closed on drift."""
from pathlib import Path
import sys

root=Path(sys.argv[1]).resolve()
JAVA=root/'android/app/src/main/java/com/braseiro/dnd'
ATLAS=root/'android/app/src/main/assets/mausritter_atlas/runtime/atlas_vivo.js'
assert (JAVA/'MausritterRules.java').is_file() and ATLAS.is_file(), 'Not the canonical R26 project'
report=[]

def edit(path, old, new, tag, expected=1):
    content=path.read_text(encoding='utf-8')
    count=content.count(old)
    if count != expected:
        raise AssertionError(f'{tag}: expected {expected} anchors, found {count} in {path}')
    path.write_text(content.replace(old,new),encoding='utf-8')
    report.append(f'{tag}: PATCHED ({count})')

GM=JAVA/'MausritterGameMasterEngine.java'
RULE=JAVA/'MausritterRules.java'
DB=JAVA/'AppDb.java'
UI=JAVA/'MainActivity.java'

edit(GM,
'''    private static int die(String seed, int sides) {
        int value = positiveHash(seed);
        return (value % Math.max(1, sides)) + 1;
    }''',
'''    private static int die(String seed, int sides) {
        final int bound = Math.max(1, sides);
        try {
            byte[] bytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest((seed == null ? "" : seed).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long sample = 0L;
            for (int i = 0; i < 8; i++) sample = (sample << 8) | (bytes[i] & 0xffL);
            return (int) java.lang.Long.remainderUnsigned(sample, bound) + 1;
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 required for reproducible Mausritter dice", error);
        }
    }''','M01 independent deterministic SHA256 rolls')

edit(RULE,
'''        item("lead-cloak", "Capa de chumbo", BODY, 2, 3, "", 1, "Armadura leve: reserva 1 Corpo + 1 Pata secundária.", "r13/r13_lead-cloak.png");''',
'''        item("lead-cloak", "Capa de chumbo", BODY, 2, 3, "", 1, "Armadura pesada: ocupa 2 espaços de Corpo.", "r13/r13_lead-cloak.png");''','M07 lead cloak classification')
edit(RULE,'''("light-armor".equals(item.id)||"lead-cloak".equals(item.id))''',
          '''("light-armor".equals(item.id))''','M07 placement',1)
edit(RULE,'''("light-armor".equals(definition.id)||"lead-cloak".equals(definition.id))''',
          '''("light-armor".equals(definition.id))''','M07 used slots',2)
edit(RULE,
'''    public static int armor(JSONObject sheet) {
        int armor = 0;
        JSONArray inventory = sheet == null ? null : sheet.optJSONArray("inventory");
        for (int i = 0; inventory != null && i < inventory.length(); i++) {
            JSONObject row = inventory.optJSONObject(i);
            if (row != null) armor = Math.max(armor, row.optInt("armor", 0));
        }
        return armor;
    }''',
'''    public static int armor(JSONObject sheet) {
        int armor = 0;
        JSONArray inventory = sheet == null ? null : sheet.optJSONArray("inventory");
        for (int i = 0; inventory != null && i < inventory.length(); i++) {
            JSONObject row = inventory.optJSONObject(i);
            if (row == null || row.optBoolean("derived", false)) continue;
            if (!BODY.equals(normalizeType(row.optString("type"), PACK))) continue;
            int uses = Math.max(0, row.optInt("uses", 0));
            if (uses > 0 && row.optInt("spent", 0) >= uses) continue;
            armor = Math.max(armor, Math.max(0, row.optInt("armor", 0)));
        }
        return armor;
    }''','M03 armor worn/usable only')
edit(RULE,
'''    public static boolean save(int roll, int target) {
        return roll == 1 || (roll != 20 && roll <= clamp(target, 1, 19));
    }''',
'''    public static boolean save(int roll, int target) {
        return roll >= 1 && roll <= 20 && roll <= target;
    }''','M04 normative save boundaries')

# app DB is also used for non-Mausritter profiles; leave them unchanged.
a=DB.read_text(encoding='utf-8')
start=a.find('    public JSONObject nextTurn(){')
end=a.find('    private void decrementConditionTimers(',start)
if start<0 or end<0 or 'public JSONObject nextTurn()' in a[end:]:
    raise AssertionError('M02 nextTurn location ambiguous')
old=a[start:end]
if 'List<JSONObject> list=listCombatants()' not in old or 'decrementConditionTimers' not in old:
    raise AssertionError('M02 nextTurn R26 anchor changed')
new='''    private boolean mausritterTurnEligible(JSONObject row) {
        if (row == null || row.length() == 0) return false;
        JSONObject resources;
        try { resources = new JSONObject(row.optString("resources_json", "{}")); }
        catch (Exception ignored) { resources = new JSONObject(); }
        if (resources.optInt("dead", 0) == 1 ||
            resources.optInt("incapacitated", 0) == 1 ||
            resources.optInt("fled", 0) == 1) return false;
        int str = resources.optInt("str", 10);
        if (row.optInt("is_player", 0) == 1) {
            JSONObject character = findCharacterByName(row.optString("name"));
            if (character.length() > 0) {
                try {
                    JSONObject sheet = new JSONObject(character.optString("sheet_json", "{}"));
                    str = sheet.optInt("str", str);
                } catch (Exception ignored) { /* Existing data remains readable. */ }
            }
        }
        // In Mausritter, zero HP is NOT an automatic disqualification.
        return str > 0;
    }

    public JSONObject nextTurn() {
        List<JSONObject> list = listCombatants();
        if (list.isEmpty()) return new JSONObject();
        JSONObject state = getCombatState();
        int index = state.optInt("turn_index", -1), round = state.optInt("round_no", 1);
        boolean mausritter = "mausritter".equals(getActiveMapProfile());
        JSONObject active = null;
        for (int scanned = 0; scanned < list.size(); scanned++) {
            index++;
            if (index >= list.size()) { index = 0; round++; }
            JSONObject candidate = list.get(index);
            if (!mausritter || mausritterTurnEligible(candidate)) {
                active = candidate;
                break;
            }
        }
        ContentValues clear = new ContentValues();
        clear.put("active_turn", 0);
        getWritableDatabase().update("combatants", clear, "world_profile=?",
                new String[]{getActiveMapProfile()});
        if (active == null) {
            ContentValues stopped = new ContentValues();
            stopped.put("running", 0);
            stopped.put("turn_index", -1);
            stopped.put("state", CombatStateMachine.IDLE);
            stopped.put("pending_roll_json", "{}");
            stopped.put("updated_at", System.currentTimeMillis());
            getWritableDatabase().update("combat_state_profiles", stopped,
                    "world_profile=?", new String[]{getActiveMapProfile()});
            return new JSONObject();
        }
        ContentValues action = new ContentValues();
        action.put("active_turn", 1);
        if (!"mystara".equals(getActiveMapProfile())) action.put("reaction_used", 0);
        getWritableDatabase().update("combatants", action, "id=? AND world_profile=?",
                new String[]{String.valueOf(active.optLong("id")), getActiveMapProfile()});
        if (!"mystara".equals(getActiveMapProfile()))
            decrementConditionTimers(active.optLong("id"));
        JSONObject phase = getCombatState();
        String phaseName = phase.optString("state", "");
        if (CombatStateMachine.PREPARING.equals(phaseName))
            transitionCombatState(CombatStateMachine.INITIATIVE, new JSONObject());
        phase = getCombatState();
        phaseName = phase.optString("state", "");
        if (CombatStateMachine.INITIATIVE.equals(phaseName) ||
            CombatStateMachine.RESOLVING.equals(phaseName) ||
            CombatStateMachine.AWAITING_ROLL.equals(phaseName))
            transitionCombatState(CombatStateMachine.ACTIVE_ROUND, new JSONObject());
        ContentValues stateUpdate = new ContentValues();
        stateUpdate.put("round_no", round);
        stateUpdate.put("turn_index", index);
        stateUpdate.put("running", 1);
        stateUpdate.put("state", CombatStateMachine.ACTIVE_ROUND);
        stateUpdate.put("pending_roll_json", "{}");
        stateUpdate.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().update("combat_state_profiles", stateUpdate,
                "world_profile=?", new String[]{getActiveMapProfile()});
        return active;
    }
'''
DB.write_text(a[:start]+new+a[end:],encoding='utf-8')
report.append('M02 nextTurn filters eligible combatants only for Mausritter')

# The executor must re-read the live actor/target before a click can affect state.
anchor='''    private void resolveMausritterPlayerAttack(JSONObject attacker,JSONObject target,JSONObject choice){
        if(attacker==null||target==null||attacker.optInt("active_turn",0)!=1||attacker.optInt("is_player",0)!=1)return;'''
replacement='''    private boolean mausritterCombatantEligible(JSONObject row) {
        if (row == null || row.length() == 0) return false;
        JSONObject sheet = row.optInt("is_player", 0) == 1
                ? MausritterRules.fromCharacter(db.findCharacterByName(row.optString("name")))
                : new JSONObject();
        JSONObject resources = mausritterCombatResources(row, sheet);
        return resources.optInt("str", 1) > 0 &&
               resources.optInt("dead", 0) != 1 &&
               resources.optInt("incapacitated", 0) != 1 &&
               resources.optInt("fled", 0) != 1;
    }

    private void resolveMausritterPlayerAttack(JSONObject attacker,JSONObject target,JSONObject choice){
        if(attacker==null||target==null)return;
        attacker=db.getCombatant(attacker.optLong("id"));
        target=db.getCombatant(target.optLong("id"));
        if(attacker.optInt("active_turn",0)!=1||attacker.optInt("is_player",0)!=1||
                db.getCombatState().optInt("running",0)!=1||
                !mausritterCombatantEligible(attacker)||!mausritterCombatantEligible(target)||
                target.optInt("is_player",0)==1)return;'''
edit(UI,anchor,replacement,'M02 UI attack live actor and target')
edit(UI,
'''        if(mausritterEnemyResolving)return;mausritterEnemyResolving=true;''',
'''        if(mausritterEnemyResolving||!mausritterCombatantEligible(enemy))return;mausritterEnemyResolving=true;''',
'M02 reject incapacitated enemy callback')
edit(UI,
'''JSONObject live=db.getCombatant(enemy.optLong("id"));if(live.length()==0||live.optInt("active_turn",0)!=1''',
'''JSONObject live=db.getCombatant(enemy.optLong("id"));if(!mausritterCombatantEligible(live)||live.optInt("active_turn",0)!=1''',
'M02 revalidate enemy from database')

# The same eligibility rule applies to the live combatant list.
edit(UI,
'''if(resources.optInt("str",1)<=0||resources.optInt("dead",0)==1||resources.optInt("incapacitated",0)==1||resources.optInt("fled",0)==1)continue;''',
'''if(!mausritterCombatantEligible(row))continue;''',
'M02 one eligibility check on UI/executor')

# Existing saves with a stale active_turn must not execute a dead actor.
edit(UI,
'''        addMausritterTurnPanel(body,state,active);''',
'''        if(state.optInt("running",0)==1 && active.length()>0 && !mausritterCombatantEligible(active)){
            db.nextTurn();
            showMausritterCombat();
            return;
        }
        addMausritterTurnPanel(body,state,active);''','M02 stale-active turn recovery')

# One Watch is six hours, one exploration Turn is ten minutes; do not charge unlit light.
old='''function advanceWatches(data, count) { const per=Number(state.worldRules?.time?.watchesPerDay||4),beforeDay=Number(data.day||1),beforeWatch=Number(data.watch||0);let total=beforeWatch+Math.max(0,Number(count||0));data.day=beforeDay+Math.floor(total/per);data.watch=total%per;const daysPassed=Math.max(0,Number(data.day)-beforeDay);if(daysPassed){advanceFactions(data,daysPassed);advanceNpcWorld(data,daysPassed);}const previousSeason=data.season;syncSeasonToCalendar(data);if(previousSeason!==data.season){data.weather='';data.weatherDay=0;}rollWeather(data,Number(data.weatherDay)!==Number(data.day));ensureSeasonEvent(data);return {beforeDay,beforeWatch,daysPassed,seasonChanged:previousSeason!==data.season,wildernessCheckDue:wildernessCheckDue(beforeDay,beforeWatch,count)}; }'''
new='''function advanceWatches(data, count) {
    const per=Math.max(1,Number(state.worldRules?.time?.watchesPerDay||4));
    const elapsed=Math.max(0,Math.trunc(Number(count||0)));
    const beforeDay=Number(data.day||1),beforeWatch=Number(data.watch||0);
    const total=beforeWatch+elapsed;
    data.day=beforeDay+Math.floor(total/per);
    data.watch=total%per;
    // Mausritter: six hours per Watch, ten minutes per dungeon Turn.
    // This is the same canonical elapsed time used by active light consumption.
    if(elapsed>0) {
      const turnsPerWatch=Math.max(1,Number(state.worldRules?.time?.turnsPerWatch||36));
      data.localTurns=Number(data.localTurns||0)+elapsed*turnsPerWatch;
      reconcileActiveLight(data);
    }
    const daysPassed=Math.max(0,Number(data.day)-beforeDay);
    if(daysPassed){advanceFactions(data,daysPassed);advanceNpcWorld(data,daysPassed);}
    const previousSeason=data.season;
    syncSeasonToCalendar(data);
    if(previousSeason!==data.season){data.weather='';data.weatherDay=0;}
    rollWeather(data,Number(data.weatherDay)!==Number(data.day));
    ensureSeasonEvent(data);
    return {beforeDay,beforeWatch,daysPassed,seasonChanged:previousSeason!==data.season,
            wildernessCheckDue:wildernessCheckDue(beforeDay,beforeWatch,elapsed)};
  }'''
edit(ATLAS,old,new,'M05 watches count toward lit-turn consumption')

# Set monotonically higher version code; preserve applicationId, SDK and schema.
v=root/'version.properties'
edit(v,'VERSION_NAME=6.1.0-mausritter.26','VERSION_NAME=6.1.0-mausritter.27','R27 version name')
edit(v,'VERSION_CODE=610026','VERSION_CODE=610027','R27 version code')
print('\n'.join(report))
print('Patches applied; R26 release and D&D remain unchanged.')
