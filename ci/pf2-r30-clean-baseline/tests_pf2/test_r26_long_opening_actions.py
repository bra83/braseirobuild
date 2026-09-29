from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text('utf-8')
OPTIONS=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/SceneOptionDiversifier.java').read_text('utf-8')
PROPS=(ROOT/'version.properties').read_text('utf-8')

def test_r26_or_newer_version():
    values=dict(line.split('=',1) for line in PROPS.strip().splitlines() if '=' in line)
    major,minor,patch=(int(x) for x in values['VERSION_NAME'].split('.'))
    assert (major,minor,patch) >= (9,0,27)
    assert int(values['VERSION_CODE']) >= 90027

def test_opening_target_is_long_and_immersive():
    assert '650 e 900 palavras em 6 a 8 parágrafos substanciais' in MAIN
    assert 'final boolean needsExpansion=words<650||paragraphs<6;' in MAIN
    assert '650 A 900 PALAVRAS EM 6 A 8 PARÁGRAFOS' in MAIN

def test_opening_has_second_continuation_pass_without_restarting_scene():
    assert 'repairAttempt<2' in MAIN
    assert 'BRAS_OPENING_CONTINUATION BRAS_SINGLE_CALL' in MAIN
    assert 'CENA JÁ ESCRITA (NÃO REPITA)' in MAIN
    assert 'draft+"\\n\\n"+continuation' in MAIN

def test_opening_rebuilds_actions_from_final_scene():
    assert 'repairedOptions=rep.optJSONArray("options")' in MAIN
    assert 'stableEnvelope.put("options",repairedOptions)' in MAIN
    assert 'Gere 4 a 6 opções concretas e diferentes ligadas ao ponto de decisão final' in MAIN

def test_patrol_scene_has_multiple_distinct_grounded_choices():
    for label in [
        'MAPEAR A ROTA DA PATRULHA',
        'ESPERAR A JANELA ENTRE AS RONDAS',
        'CONTORNAR A PATRULHA',
        'CRIAR UMA DISTRAÇÃO LONGE DA PASSAGEM',
        'RECUAR E OBSERVAR MAIS UMA RONDA',
    ]:
        assert label in OPTIONS


def test_long_form_repair_uses_non_lite_text_model_when_default_is_lite():
    assert 'openingLongFormModel(model)' in MAIN
    assert '"gemini-3.5-flash-lite".equalsIgnoreCase(model)' in MAIN
    assert 'return "gemini-3.5-flash"' in MAIN

def test_parser_does_not_truncate_generated_opening_choices_at_four():
    assert 'out.length()<6' in MAIN
    assert 'options.length()>=6' in MAIN
    assert 'numbers.size()>=6' in MAIN
