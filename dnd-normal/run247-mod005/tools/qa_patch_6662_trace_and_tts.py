#!/usr/bin/env python3
"""Second handoff QA: accepted perception TTS, private opt-in turn trace."""
from pathlib import Path
p=Path("app/src/main/java/com/braseiro/dnd5e/MainActivity.java")
s=p.read_text(encoding="utf-8")
def rep(label,old,new,count=1):
    global s
    n=s.count(old)
    if n!=count:raise AssertionError(f"{label}: expected {count}, found {n}")
    s=s.replace(old,new,count)
    print("AUDITED_PATCH",label,count)

rep("tts-accepted-choice","    private boolean activeTurnNonAdvancing(String userText){",
    "    private String currentAudibleNarration(){\n"
    "        return FictionTurnAudio.select(lastNarration,currentFictionAnswer());\n"
    "    }\n"
    "    private boolean activeTurnNonAdvancing(String userText){")
rep("strixhaven-charon", "speakCharonNarration(lastNarration);",
    "speakCharonNarration(currentAudibleNarration());",2)
assert s.count("speakNativeNarrationWithSfx(lastNarration);")==3
s=s.replace("speakNativeNarrationWithSfx(lastNarration);",
    "speakNativeNarrationWithSfx(currentAudibleNarration());",2)
rep("narrator-charon","speakGemini(stripMarkdown(lastNarration))",
    "speakGemini(stripMarkdown(currentAudibleNarration()))")
rep("narrator-android","speakNativeNarrationWithSfx(stripMarkdown(lastNarration))",
    "speakNativeNarrationWithSfx(stripMarkdown(currentAudibleNarration()))")
rep("avoid-last-scene-SFX",
    'if(engine!=null)engine.prepareSceneAsync(clean,environmentalDirector==null?lastAudioPlan:environmentalDirector.mergeAudioPlan(lastAudioPlan,lastPrompt),db.getSceneState(),null);',
    'if(engine!=null)engine.prepareSceneAsync(clean,\n'
    '    FictionTurnAudio.observation(clean,currentFictionAnswer())?new JSONObject():\n'
    '        (environmentalDirector==null?lastAudioPlan:environmentalDirector.mergeAudioPlan(lastAudioPlan,lastPrompt)),\n'
    '    db.getSceneState(),null);',2)

controls='''
    /** Opt-in private diagnostic; no events leave the app without explicit share. */
    private void addFictionTraceControls(LinearLayout parent){
        if(parent==null)return;
        Button toggle=button(FictionTurnTrace.isEnabled(prefs)?
            "DESLIGAR E APAGAR DIAGNÓSTICO LOCAL":"LIGAR DIAGNÓSTICO LOCAL DO MESTRE",PANEL2);
        toggle.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            boolean enabled=!FictionTurnTrace.isEnabled(prefs);
            FictionTurnTrace.setEnabled(MainActivity.this,prefs,enabled);
            toggle.setText(enabled?"DESLIGAR E APAGAR DIAGNÓSTICO LOCAL":"LIGAR DIAGNÓSTICO LOCAL DO MESTRE");
            toast(enabled?"Diagnóstico privado ativado. Inclui texto da sessão, sem chaves API.":"Diagnóstico desligado e arquivo privado apagado.");
        }});
        parent.addView(toggle);
        Button export=button("COMPARTILHAR DIAGNÓSTICO (POR SUA ESCOLHA)",PANEL2);
        export.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            if(!FictionTurnTrace.isEnabled(prefs)){toast("Primeiro ative o diagnóstico.");return;}
            try{
                java.io.File f=FictionTurnTrace.destination(MainActivity.this);
                if(!f.isFile()){toast("Ainda não há eventos gravados.");return;}
                byte[] bytes=java.nio.file.Files.readAllBytes(f.toPath());
                int start=Math.max(0,bytes.length-46000);
                String text=new String(bytes,start,bytes.length-start,java.nio.charset.StandardCharsets.UTF_8);
                Intent intent=new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT,
                    "Braseiro D&D / diagnóstico opt-in / "+BuildConfig.VERSION_NAME+"\\n"+text);
                startActivity(Intent.createChooser(intent,"Compartilhar diagnóstico do Mestre"));
            }catch(Exception ex){toast("Não foi possível ler o diagnóstico local.");}
        }});
        parent.addView(export);
    }
'''
rep("diagnostic-controls",
    "    private void addGeminiMasterChoiceButtons(LinearLayout parent){",
    controls+"\n    private void addGeminiMasterChoiceButtons(LinearLayout parent){")
rep("narrator-debug-toggle","        panel.addView(offline);parent.addView(panel);",
    "        panel.addView(offline);addFictionTraceControls(panel);parent.addView(panel);")
rep("strixhaven-debug-toggle",'        Button academic=button("VIDA ACADÊMICA",GREEN);',
    '        addFictionTraceControls(body);\n        Button academic=button("VIDA ACADÊMICA",GREEN);')

request='''
        if(FictionTurnTrace.isEnabled(prefs))try{
            FictionTurnTrace.append(this,prefs,"request",new JSONObject()
                .put("request_id",pipelineId)
                .put("mode",mode).put("channel",inputChannel==null?"":inputChannel)
                .put("scene_fingerprint",SceneActionEvidence.hash(lastNarration))
                .put("state_revision",db.getStoryState().optLong("updated_at",0L))
                .put("intent",FictionTurnIntent.contract(requestedText,db.getSceneState(),inputChannel))
                .put("selected_model",prefs.getString("text_model",""))
                .put("request_text",requestedText));
        }catch(Exception ignored){}
'''
rep("request-diagnostic",'        final long pipelineId=++aiPipelineGeneration;',
    '        final long pipelineId=++aiPipelineGeneration;'+request)
plan='''
        if(FictionTurnTrace.isEnabled(prefs))try{
            FictionTurnTrace.append(this,prefs,"plan_prepared",new JSONObject()
                .put("request_id",aiPipelineGeneration)
                .put("plan_ready",preparedPlan==null?"no_plan":preparedPlan.contextReady)
                .put("scene_fingerprint",SceneActionEvidence.hash(lastNarration))
                .put("context_length",preparedPlan==null?0:preparedPlan.compactContext.length())
                .put("context_error",preparedPlan==null?"":preparedPlan.contextError)
                .put("context_hash",preparedPlan==null?"":preparedPlan.stateHash)
                .put("narrative_intent",preparedPlan==null?"":preparedPlan.narrativeIntent)
                .put("context_budget",34000)
                .put("input_channel",inputChannel==null?"":inputChannel));
        }catch(Exception ignored){}
'''
rep("prepared-diagnostic",'        activeMasterPlan=preparedPlan;',
    '        activeMasterPlan=preparedPlan;'+plan)

entry='    private void acceptOrRepairNarration(final String mode,final String originalUserText,final String raw,final String systemPrompt,final String key,final String model,final long requestId,final int repairAttempt){'
raw='''
        if(FictionTurnTrace.isEnabled(prefs))try{
            String snippet=raw==null?"":raw.substring(0,Math.min(raw.length(),8500));
            FictionTurnTrace.append(this,prefs,"model_received",new JSONObject()
                .put("request_id",requestId).put("mode",mode).put("model",model)
                .put("repair_attempt",repairAttempt).put("raw_length",raw==null?0:raw.length())
                .put("raw_response",snippet));
        }catch(Exception ignored){}
'''
rep("raw-reply-diagnostic",entry,entry+raw)

apply='    private void applyAiResponseToUi(final AiResponseUiPayload payload, final long requestGen) {'
answer='''
        if(FictionTurnTrace.isEnabled(prefs)&&payload!=null)try{
            FictionTurnTrace.append(this,prefs,"answer_validated",new JSONObject()
                .put("request_id",requestGen).put("accepted",payload.masterEnvelopeValid)
                .put("non_advancing",payload.nonAdvancing)
                .put("structured_changes_pending",payload.changes)
                .put("transaction_id",payload.transactionId)
                .put("scene_fingerprint_before",SceneActionEvidence.hash(lastNarration))
                .put("accepted_text",payload.masterEnvelopeValid?payload.lastNarration:"")
                .put("status",payload.statusMessage));
        }catch(Exception ignored){}
'''
rep("validation-diagnostic",apply,apply+answer)

# Never approve a playable scene with only one or two generic buttons.
rep("scene-four-choice-fail-closed",
    '            newOptions = nonAdvancing?previousOptions:(narrativeResponse ? SceneOptionDiversifier.ensureAlways(choiceSplit.options, optionHistory, calculatedNarration, action, projectedScene, db.getMasterRuntimeState().optInt("beat_no", 0) + 1) : new JSONArray());',
    '            newOptions = nonAdvancing?previousOptions:(narrativeResponse ? SceneOptionDiversifier.ensureAlways(choiceSplit.options, optionHistory, calculatedNarration, action, projectedScene, db.getMasterRuntimeState().optInt("beat_no", 0) + 1) : new JSONArray());\n'
    '            if(!SceneChoiceCommitGate.canCommit(narrativeResponse,nonAdvancing,\n'
    '                    newPendingTest!=null&&newPendingTest.length()>0,newOptions.length())){\n'
    '                final int offered=newOptions.length();\n'
    '                ui.post(new Runnable(){public void run(){\n'
    '                    if(requestGen!=aiRequestGeneration)return;\n'
    '                    markMasterRequestFailed("Narrador",action,"PLAYER_ACTION",\n'
    '                        SceneChoiceCommitGate.reason(offered),false);\n'
    '                }});\n'
    '                return;\n'
    '            }')
rep("four-choices-error-status",
    '        final boolean contextFailure=!apiFailure&&error!=null&&error.startsWith("CONTEXTO DO MESTRE:");',
    '        final boolean contextFailure=!apiFailure&&error!=null&&error.startsWith("CONTEXTO DO MESTRE:");\n'
    '        final boolean choicesFailure=!apiFailure&&error!=null&&error.startsWith("ESCOLHAS:");')
rep("four-choices-status-label",
    '        setStatus(contextFailure?"O contexto do Mestre não ficou pronto. A ação continua pendente; tente novamente.":(',
    '        setStatus(choicesFailure?"A cena não foi aceita: faltaram escolhas válidas. A ação continua pendente.":\n'
    '            (contextFailure?"O contexto do Mestre não ficou pronto. A ação continua pendente; tente novamente.":(')
# The status label now has one extra parenthesis to close the new branch.
rep("four-choices-status-close",
    '            "PROPOSTA DE TESTE INVÁLIDA. A ação continua pendente, sem rolagem ou avanço.")));',
    '            "PROPOSTA DE TESTE INVÁLIDA. A ação continua pendente, sem rolagem ou avanço."))));')
rep("four-choices-dialog-title",
    'new AlertDialog.Builder(this).setTitle(contextFailure?"A preparação do Mestre foi interrompida":(',
    'new AlertDialog.Builder(this).setTitle(choicesFailure?"A cena não apresentou quatro ações úteis":(contextFailure?"A preparação do Mestre foi interrompida":(')
# Title now needs one extra paren. The original had nested ternaries.
rep("four-choices-dialog-title-end",
    '"O Mestre não conseguiu preparar o teste"))',
    '"O Mestre não conseguiu preparar o teste")))')
rep("four-choices-dialog-detail",
    '.setMessage((contextFailure?"O contexto operacional não ficou pronto. A jogada não foi concluída e nenhuma cena nova foi confirmada. ":',
    '.setMessage((choicesFailure?"As opções não passaram pelo critério de quatro ações válidas. Nenhuma cena nova foi confirmada. ":\n'
    '                    (contextFailure?"O contexto operacional não ficou pronto. A jogada não foi concluída e nenhuma cena nova foi confirmada. ":')
rep("four-choices-dialog-detail-close",
    '"A nova narração contradiz registros canônicos. ":"A proposta mecânica não passou na validação. ")))+',
    '"A nova narração contradiz registros canônicos. ":"A proposta mecânica não passou na validação. "))))+')

p.write_text(s,encoding="utf8")
print("DONE: TTS follows accepted observation, local opted-in trace redacts secrets")
