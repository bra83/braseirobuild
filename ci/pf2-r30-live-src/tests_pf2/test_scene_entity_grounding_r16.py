from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'android/app/src/main/java/com/braseiro/pathfinder2e'

def read(name):
    return (JAVA / name).read_text(encoding='utf-8')


def test_r16_release_identity_and_locks():
    props = (ROOT / 'version.properties').read_text(encoding='utf-8')
    assert ('VERSION_NAME=9.0.31' in props or 'VERSION_NAME=9.0.29' in props)
    assert ('VERSION_CODE=90031' in props or 'VERSION_CODE=90029' in props)
    assert 'DB_SCHEMA=59' in props
    assert 'BACKUP_FORMAT=22' in props


def test_place_and_player_identities_cannot_be_promoted_to_npc():
    grounding = read('SceneEntityGrounding.java')
    validator = read('MasterResponseValidator.java')
    state = read('SceneStateEngine.java')
    assert 'placeAliases' in grounding and 'nonNpcAliases' in grounding
    assert 'isNonNpcIdentity' in validator
    assert 'filterPresentNpcs' in validator
    assert 'SceneEntityGrounding.isNonNpcIdentity' in state


def test_narration_extractor_handles_exact_nidal_barbara_guards_regression():
    extractor = read('NarratedNpcExtractor.java')
    harness = (ROOT / 'ferramentas/NarratedNpcExtractorSelfTest.java').read_text(encoding='utf-8')
    assert 'INVERTED_ATTRIBUTION' in extractor
    assert 'Set<String> excludedIdentities' in extractor
    assert 'Barbara de Vetro' in harness
    assert 'região de Nidal exige' in harness
    assert 'Dois guardas vigiam' in harness


def test_runtime_repairs_existing_bad_scene_cast_and_does_not_persist_prose_only_names():
    main = read('MainActivity.java')
    assert 'sanitizeReservedNpcAliases' in main
    assert 'SceneEntityGrounding.isNonNpcIdentity(db,scene,name)' in main
    assert 'SceneEntityGrounding.isWeakAutoNpc(memory)' in main
    assert 'narration_only' in main
    assert 'npc.put("transient",true)' in main


def test_master_prompt_and_validator_share_same_entity_contract():
    orchestrator = read('MasterOrchestrator.java')
    contract = read('PathfinderNarrativeContract.java')
    assert 'scene_entity_grounding' in orchestrator
    assert 'SceneEntityGrounding.promptDirective' in orchestrator
    assert 'Local nunca é NPC' in contract
