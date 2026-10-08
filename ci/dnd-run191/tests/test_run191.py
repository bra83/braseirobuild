#!/usr/bin/env python3
"""Static regression gates: deliberately NOT a substitute for Android UI/instrumented tests."""
from pathlib import Path
import sys
root=Path(sys.argv[1])
pkg=root/'android/app/src/main/java/com/braseiro/dnd5e'
main=(pkg/'MainActivity.java').read_text(encoding='utf-8')
checks={
 'CANONICAL_RUN190': 'private void showRuleCuriosityDialog()' in main and 'CompanionAgencyEngine.stageCurated' in main,
 'NO_MANUAL_TIMER_JSON': 'Durações em JSON' not in main,
 'NO_MANUAL_RESOURCES_JSON': 'Recursos em JSON' not in main,
 'LOCAL_PARTY_INITIATIVE': '🎲 ROLAR INICIATIVAS (1d20 + DES)' in main and 'combatInitiativeInputOpen.compareAndSet(false,true)' in main,
 'LOCAL_MONSTER_INITIATIVE': '🎲 ROLAR 1d20 + BÔNUS' in main,
 'NO_AUTO_TURN_ON_ROLL': main.index('🎲 ROLAR INICIATIVAS') < main.index('final AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title==null?"Iniciativa":title)'),
 'TIMER_PRESERVE_UNKNOWN_KEYS': 'new JSONObject(originalTimers.getJSONObject(i).toString())' in main,
 'RESOURCE_PRESERVE_UNKNOWN_KEYS': 'new JSONObject(originalResources.toString())' in main,
 'TIMER_ROUNDS_VALIDATED': 'rounds<1||rounds>999' in main,
 'DEATH_SAVE_RANGE_VALIDATED': 'success<0||success>3||fails<0||fails>3' in main,
 'SAFE_PRE_COMMIT': 'save.setEnabled(false)' in main and 'dialog.dismiss();showCombat();' in main,
 'RULE_ENGINES_PRESERVED': all((pkg/x).is_file() for x in ['DndRulesEngine.java','CombatStateMachine.java','RuleRetrievalPolicy.java','AppDb.java','CompanionAgencyEngine.java']),
 'GEMINI_FIRST_PRESERVED': 'RUN188_GEMINI_EXPLICIT_OFFLINE' in main,
 'NO_AUTOMATIC_OFFLINE': 'TENTAR NOVAMENTE' in main and 'MESTRE OFFLINE' in main,
}
for key, result in checks.items():print(('PASS' if result else 'FAIL')+' '+key)
if not all(checks.values()):raise SystemExit('RUN191_STATIC_REGRESSION_FAILURE')
print('RUN191_STATIC_GATES_PASS=',len(checks))