from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
GEM=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/GeminiService.java').read_text(encoding='utf-8')
MAIN=(ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e/MainActivity.java').read_text(encoding='utf-8')
PROPS=(ROOT/'version.properties').read_text(encoding='utf-8')


def test_r19_1_identity():
    assert ('VERSION_NAME=9.0.37' in PROPS or 'VERSION_NAME=9.0.37' in PROPS)
    assert ('VERSION_CODE=90037' in PROPS or 'VERSION_CODE=90037' in PROPS)
    assert 'DB_SCHEMA=59' in PROPS and 'BACKUP_FORMAT=22' in PROPS


def test_model_specific_403_does_not_abort_all_text_fallbacks():
    # 401 remains a credential failure; 403 is allowed to fall through to the next model.
    assert 'if(e.status==401)throw new AuthException' in GEM
    assert 'if(e.status==401||e.status==403)throw new AuthException' not in GEM
    assert 'Nenhum modelo de texto disponível respondeu.' in GEM


def test_tts_25_has_31_automatic_model_fallback():
    block=GEM[GEM.index('private List<String> resolveCandidates'):GEM.index('private static String canonicalTtsModel')]
    assert 'gemini-2.5-flash-preview-tts' in block
    assert 'ordered.add("gemini-3.1-flash-tts-preview")' in block
    assert 'postTtsWithRetry' in GEM


def test_runtime_exposes_real_text_and_tts_diagnostics():
    assert 'TESTAR TEXTO — 1 CHAMADA' in MAIN
    assert 'TESTAR CHARON — TOM + TAGS' in MAIN
    assert 'compactGeminiFailure' in MAIN
    assert 'TTS conectado • ' in MAIN
    assert 'Texto conectado • ' in MAIN


def test_optional_narrative_repair_is_not_reported_as_primary_text_failure():
    assert 'a revisão opcional de estilo não respondeu; o texto principal foi preservado' in MAIN


def test_free_tier_image_zero_quota_gets_specific_guidance():
    assert 'A API de geração de imagens configurada não oferece cota Free Tier para esta chave.' in MAIN
