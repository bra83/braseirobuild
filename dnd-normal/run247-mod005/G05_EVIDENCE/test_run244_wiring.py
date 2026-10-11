from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/braseiro/dnd5e'
db=(src/'AppDb.java').read_text()
ui=(src/'MainActivity.java').read_text()
a=db[db.index('public JSONObject applyCompiledStatblockAttack(final long actorId,final long targetId,\n        final String statblockId,final String action,final int natural,final int secondNatural,\n        final int damageTotal,final int riderSaveRoll,final long additionalThreatId,\n        final int destinationX'):]
a=a[:a.index('    /** RUN223:')]
assert 'forced=MonsterForcedMovementRules.compiled(' in a
assert a.index('compiledMonsterForcedMovementPlan(')<a.index('spendCombatantTurnResource(')
assert a.index('commitCompiledMonsterForcedMovement(')>a.index('spendCombatantTurnResource(')
assert 'sourceAction.optString("effect_on_fail"' in a
assert 'forced_movement_not_applicable' in a
assert 'compiled_effect_source_changed' in a
assert 'destX,destY,token' in ui
assert 'labeledField(form,"Destino X' in ui
assert 'labeledField(form,"Destino Y' in ui
assert 'Run244G05AttackRiderForcedMovementRobolectricTest.java' in str(list((root/'app/src/test/java/com/braseiro/dnd5e').glob('Run244G05AttackRiderForcedMovementRobolectricTest.java')))
print('PASS RUN244 wiring source assertions 9 (not Android execution)')
