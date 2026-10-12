from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
def read(n): return (SRC/n).read_text(encoding='utf-8')

def test_initiative_is_perception_and_enemy_wins_tie():
    main=read('MainActivity.java'); db=read('AppDb.java')
    assert 'pathfinderRules.initiativeBonus(c,"Percepção")' in main
    assert 'pathfinderRules.initiativeBonus(h,"Percepção")' in main
    assert 'stats.optInt("initiative_bonus",stats.optInt("perception_bonus",0))' in main
    assert 'ORDER BY initiative DESC, is_player ASC, name' in db

def test_zero_hp_damage_and_recovery_are_wired():
    db=read('AppDb.java'); main=read('MainActivity.java'); rules=read('Pathfinder2eRulesEngine.java')
    for token in ('applyCombatantDamagePf2','applyCombatantHealingPf2','applyRecoveryCheckPf2','showPf2RecoveryDialog'):
        assert token in db+main
    assert 'knockedOut(int wounded,int doomed,boolean nonlethal,boolean critical)' in rules
    assert 'int d=(critical?2:1)+w' in rules

def test_rest_and_travel_follow_pf2_core_math():
    world=read('WorldStateEngine.java'); travel=read('TravelRulesEngine.java')
    assert 'applyPf2LongRestNaturalHealing' in world
    assert 'last_long_rest_start_hour' in world
    assert 'if(n.contains("descanso curto"))return 1;' not in world
    assert '(partyTravelSpeedFeet()/10d)*1.609344d' in travel
    assert 'greater?(1d/3d):.5d' in travel
    assert 'partyHustleLimitHours' in travel

def test_condition_timers_are_not_blindly_decremented():
    db=read('AppDb.java'); rules=read('Pathfinder2eRulesEngine.java'); main=read('MainActivity.java')
    assert 'tickConditionTimers(long id,String phase)' in db
    assert 'timer.optString("tick",timer.optString("phase","manual"))' in db
    assert 'validConditionList' in rules
    assert 'pathfinderRules.validConditionList(value(conditions))' in main


def test_frightened_decreases_at_end_of_turn():
    db=read('AppDb.java')
    assert 'applyPf2EndTurnConditionUpdates(previousId)' in db
    assert '(frightened|amedrontado)' in db


def test_master_hp_updates_cannot_bypass_combat_dying_state():
    main=read('MainActivity.java')
    assert 'Durante combate, mudança de PV deve usar hp_delta' in main
    assert 'db.applyCombatantDamagePf2(combatant.optLong("id")' in main
    assert 'critical_damage' in main and 'nonlethal_damage' in main


def test_massive_damage_instant_death_is_enforced():
    db=read('AppDb.java')
    assert 'incomingDamage>=2*max' in db
    assert 'massive_damage_death' in db


def test_ordinary_npcs_do_not_use_pc_dying_rules_by_default():
    db=read('AppDb.java'); main=read('MainActivity.java')
    assert 'usesDyingRules=c.optInt("is_player",0)==1||state.optBoolean("uses_dying_rules",false)' in db
    assert 'else{dying=0;dead=true;state.put("unconscious",false);}' in db
    assert 'NPC significativo: usar regras de dying/wounded' in main
