import json
import importlib.util
import os
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PY=ROOT/'android/app/src/main/python'
sys.path.insert(0,str(PY))
import braseiro_barbara_adapter as a


def plan(text, channel):
    return json.loads(a.plan_json(text,'normal',True,False,'continuation',channel))


def test_health_pf2_and_1_1():
    h=json.loads(a.health_json())
    assert h['ok'] is True
    assert h['system_id']=='pathfinder_2e'
    assert h['barbara_version']=='1.1.2'
    assert h['mechanics_authority']=='Pathfinder2eRulesEngine'
    assert h['native_barbara_system_module'] is False
    assert h['duplicate_gemini_call'] is False
    assert h['second_database'] is False


def test_player_action_question_does_not_freeze():
    p=plan('Pego o chapéu e o casaco e digo a Adrian: você não vem?','PLAYER_ACTION')
    assert p['input_channel']=='PLAYER_ACTION'
    assert p['input_classification']['world_advances'] is True
    assert p['turn_plan']['world_advances'] is True
    assert p['turn_plan']['mode']=='fiction'


def test_player_action_rules_looking_phrase_stays_action():
    p=plan('Como funciona esta fechadura? Eu examino e tento abri-la.','PLAYER_ACTION')
    assert p['input_classification']['rules_only'] is False
    assert p['turn_plan']['world_advances'] is True


def test_gm_help_never_advances_even_action_like():
    p=plan('Eu corro pela porta e ataco o guarda. O que acontece?','GM_HELP')
    assert p['input_channel']=='GM_HELP'
    assert p['input_classification']['world_advances'] is False
    assert p['turn_plan']['world_advances'] is False
    assert p['turn_plan']['mode']=='meta'


def test_validation_allows_empty_narration_for_help():
    r=json.loads(a.validate_envelope_json('{}','pergunta','normal','continuation','GM_HELP'))
    assert r['valid'] is True


def test_invalid_channel_rejected():
    try:
        a.plan_json('teste','normal',True,False,'continuation','WRONG')
    except ValueError as exc:
        assert str(exc)=='invalid_input_channel'
    else:
        raise AssertionError('invalid channel accepted')
