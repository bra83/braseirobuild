from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'android/app/src/main/java/com/braseiro/pathfinder2e'
MAIN=(SRC/'MainActivity.java').read_text(encoding='utf-8')
SUPPORT=(SRC/'MasterSupportPanelEngine.java').read_text(encoding='utf-8')
QUOTA=(SRC/'AiQuotaGovernor.java').read_text(encoding='utf-8')


def test_contextual_gm_help_uses_raw_question_and_skips_rule_search():
    method=MAIN[MAIN.index('private void sendMasterSideQuestion'):MAIN.index('private void restoreAfterMasterSideQuery')]
    assert 'sendAi("Narrador",question,"GM_HELP")' in method
    assert 'CONSULTA AUXILIAR FORA DA FICÇÃO' not in method
    assert 'contextOnlyHelp="GM_HELP".equals(inputChannel)' in MAIN
    assert '"gm_help_context_only"' in MAIN
    assert 'deliverAiRuleSearch' in MAIN


def test_retrieval_timeout_still_builds_full_master_plan_before_gemini():
    block=MAIN[MAIN.index('private void deliverAiRuleSearch'):MAIN.index('/** Continua a chamada somente depois')]
    assert '"retrieval_timeout".equals(status)' in block
    assert 'masterOrchestrator.prepare(mode,requestedText,mapContext,audioDiagnosticSnapshot(),retrieval==null?new JSONObject():retrieval.bundle,inputChannel)' in block
    assert 'continueSendAiAfterRuleSearch(mode,requestedText,mapContext,retrieval,null);return;' not in block.split('if(masterOrchestrator==null)',1)[0]


def test_aux_answer_is_immediately_visible_and_persisted_at_top_of_panel():
    assert 'showMasterSideAnswerDialog(question,answer)' in MAIN
    assert 'ÚLTIMA PERGUNTA' in MAIN
    assert 'String supportQuestion=lastMasterSupport.optString("last_question","")' in MAIN
    assert 'String supportAnswer=lastMasterSupport.optString("last_answer","")' in MAIN
    assert 'Pergunta respondida sem avançar a cena.' in MAIN


def test_context_help_panel_does_not_claim_rule_timeout_or_mechanical_search():
    assert '"gm_help_context_only".equals(status)' in SUPPORT
    assert 'Esta pergunta é contextual e não exige consulta mecânica.' in SUPPORT
    assert 'boolean searched=!"gm_help_context_only".equals(status)' in SUPPORT
    assert 'out.put("rules_searched",!"gm_help_context_only".equals(status)' in SUPPORT


def test_charon_budget_default_migrates_old_24_block_limit_and_is_preflighted_per_scene():
    assert 'DEFAULT_TTS_LIMIT=160' in QUOTA
    assert 'tts_budget_9_0_14_migrated' in MAIN
    assert 'prefs.getInt("ai_daily_tts_limit",160)==24' in MAIN
    assert 'putInt("ai_daily_tts_limit",160)' in MAIN
    assert 'index==0&&aiQuotaGovernor!=null&&!aiQuotaGovernor.canCallTts(Math.max(1,chunks.size()))' in MAIN


def test_later_charon_failure_keeps_same_engine_unless_user_enabled_android_fallback():
    on_error=MAIN[MAIN.index('public void onError(final String error)', MAIN.index('private void synthesizeChunk')):MAIN.index('private void onCharonVoiceStarted')]
    assert 'audio.endVoiceSessionInput()' in on_error
    assert 'ttsAndroidFallbackAllowed()' in on_error
    assert 'scheduleNativeContinuationAfterCharon' in on_error
    assert 'Charon pausou no bloco ' in on_error
    assert 'charon-partial-error-after-retries' in on_error
    assert 'prefs.getBoolean("tts_android_fallback_enabled",false)' in MAIN
    continuation=MAIN[MAIN.index('private void scheduleNativeContinuationAfterCharon'):MAIN.index('private void fallbackToNativeNarration')]
    assert 'audio.isVoiceActive()' in continuation
    assert 'speakNativeNarrationWithSfx(remaining)' in continuation


def test_android_fallback_prefers_pt_br_quality_over_offline_flag():
    method=MAIN[MAIN.index('private void configureNativeTtsVoice'):MAIN.index('private float narrativeSpeechRate')]
    assert 'score+=10000' in method
    assert 'candidate.getQuality()' in method
    assert 'score+=1500' in method
    assert 'score+=80' in method
    assert method.index('candidate.getQuality()') < method.index('!candidate.isNetworkConnectionRequired()')
