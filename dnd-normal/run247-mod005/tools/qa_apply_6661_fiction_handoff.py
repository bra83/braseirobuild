#!/usr/bin/env python3
"""Patch ONLY the verified 6.6.60 successor. Abort on any missing anchor."""
from pathlib import Path
r=Path("app/src/main/java/com/braseiro/dnd5e")
def apply(file,edits):
    p=r/file
    s=p.read_text(encoding="utf-8")
    for label,old,new in edits:
        hits=s.count(old)
        if hits!=1:raise AssertionError(f"{file}: {label}: expected one anchor, found {hits}")
        s=s.replace(old,new,1)
    p.write_text(s,encoding="utf-8")
    print("PATCHED",file,len(edits),len(s))

apply("MainActivity.java",[
    ("master-router",
     '    /* package */ static boolean shouldRouteNarratorQuestionToMaster(String text){\n        return MasterQuestionIntentGuard.isConsultation(text);\n    }',
     '    /* package */ static boolean shouldRouteNarratorQuestionToMaster(String text){\n'
     '        // PLAYER_ACTION is its own input channel. A question mark never\n'
     '        // redirects the current scene to the blue GM_HELP panel.\n'
     '        return false;\n    }'),
    ("active-non-advancing",
     '        if("PLAYER_ACTION".equals(channel))return false;\n        return BraseiroNarrativeDirector.isNonAdvancing(userText);',
     '        if("PLAYER_ACTION".equals(channel))return FictionTurnIntent.freezesWorld(\n'
     '            FictionTurnIntent.classify(userText),userText,db==null?null:db.getSceneState());\n'
     '        return FictionTurnIntent.freezesWorld(FictionTurnIntent.classify(userText),\n'
     '            userText,db==null?null:db.getSceneState())||BraseiroNarrativeDirector.isNonAdvancing(userText);'),
    ("ephemeral-fields",
     '    private String lastPrompt="";',
     '    private String lastPrompt="";\n'
     '    private String lastFictionAnswer="";\n'
     '    private String lastFictionAnswerSceneHash="";'),
    ("overlay-helper",
     '    private boolean activeTurnNonAdvancing(String userText){',
     '    private String currentFictionAnswer(){\n'
     '        String fingerprint=SceneActionEvidence.hash(lastNarration==null?"":lastNarration);\n'
     '        if(fingerprint.equals(lastFictionAnswerSceneHash))return lastFictionAnswer;\n'
     '        if(prefs==null)return "";\n'
     '        try{JSONObject saved=new JSONObject(prefs.getString("fiction_observation_"+activeWorldProfile(),"{}"));\n'
     '            if(fingerprint.equals(saved.optString("scene_fingerprint","")))\n'
     '                return saved.optString("answer","");\n'
     '        }catch(Exception ignored){}\n'
     '        return "";\n'
     '    }\n\n'
     '    private boolean activeTurnNonAdvancing(String userText){'),
    ("render-strixhaven-answer",
     'narrativeBox.addView(strixhavenNarrationOutput);',
     'narrativeBox.addView(strixhavenNarrationOutput);\n'
     '        String fictionAnswer=currentFictionAnswer();\n'
     '        if(!fictionAnswer.isEmpty()){\n'
     '            narrativeBox.addView(text("ESCLARECIMENTO NESTA CENA",11,GOLD,true));\n'
     '            narrativeBox.addView(text(fictionAnswer,13,Color.rgb(235,225,199),false));\n'
     '        }'),
    ("render-master-answer",
     'addNarrationChapters(response,shownNarration);b.addView(response);',
     'addNarrationChapters(response,shownNarration);\n'
     '        String fictionAnswer=currentFictionAnswer();\n'
     '        if(!fictionAnswer.isEmpty()){\n'
     '            response.addView(text("ESCLARECIMENTO NESTA CENA",12,GOLD,true));\n'
     '            response.addView(text(fictionAnswer,14,TEXT,false));\n'
     '        }\n'
     '        b.addView(response);'),
    ("preserve-options-for-observation",
     '            newOptions = narrativeResponse ? SceneOptionDiversifier.ensureAlways(choiceSplit.options, optionHistory, calculatedNarration, action, projectedScene, db.getMasterRuntimeState().optInt("beat_no", 0) + 1) : new JSONArray();',
     '            // A perceptive answer must NOT replace the scene or rebind choices.\n'
     '            newOptions = nonAdvancing?previousOptions:(narrativeResponse ? SceneOptionDiversifier.ensureAlways(choiceSplit.options, optionHistory, calculatedNarration, action, projectedScene, db.getMasterRuntimeState().optInt("beat_no", 0) + 1) : new JSONArray());'),
    ("preserve-canonical-scene-on-ui",
     '        this.lastNarration = canonicalize2024Narration(payload.lastNarration);',
     '        if(payload.nonAdvancing){\n'
     '            // Answer in the same scene, never replace the accepted scene\n'
     '            // narration, options, pending test, TTS source or world clock.\n'
     '            this.lastFictionAnswer=canonicalize2024Narration(payload.lastNarration);\n'
     '            this.lastFictionAnswerSceneHash=SceneActionEvidence.hash(lastNarration==null?"":lastNarration);\n'
     '            if(prefs!=null)try{prefs.edit().putString("fiction_observation_"+activeWorldProfile(),\n'
     '                new JSONObject().put("scene_fingerprint",lastFictionAnswerSceneHash)\n'
     '                    .put("answer",lastFictionAnswer).toString()).apply();}catch(Exception ignored){}\n'
     '            clearFailedGeminiRequest();\n'
     '            setStatus("Esclarecimento respondido sem avançar a cena, o relógio ou o turno.");\n'
     '            if(isStrixhavenAdventure())showStrixhavenSession();\n'
     '            else if("Mestre IA".equals(currentScreen))showNarrator();\n'
     '            return;\n'
     '        }\n'
     '        lastFictionAnswer="";lastFictionAnswerSceneHash="";\n'
     '        if(prefs!=null)prefs.edit().remove("fiction_observation_"+activeWorldProfile()).apply();\n'
     '        this.lastNarration = canonicalize2024Narration(payload.lastNarration);'),
    ("optional-npc-knowledge-receipt",
     '                if (masterOrchestrator != null && !changes) {\n                    masterOrchestrator.afterAccepted',
     '                if (!changes && masterEnvelopeValid && narrativeResponse)try{\n'
     '                    // Only the ACCEPTED text can register a character as known.\n'
     '                    NpcPerceptionLedger.commitAcceptedPresentation(db,db.getSceneState(),calculatedNarration,"party");\n'
     '                }catch(Exception receiptFailure){AppLog.w("NpcPerception","Could not persist accepted visible NPC presentation",receiptFailure);}\n'
     '                if (masterOrchestrator != null && !changes) {\n                    masterOrchestrator.afterAccepted'),
    ("final-priority-scene-directive",
     '        p.append("ESTADO COMPACTO AUTORITATIVO E PLANO LOCAL:\\n")',
     '        p.append("PRIORIDADE DA INTENÇÃO E DO CONHECIMENTO NESTE TURNO: ")\n'
     '            .append(FictionTurnDirective.forTurn(userText,db.getSceneState(),activeInputChannel())).append("\\n");\n'
     '        p.append("ESTADO COMPACTO AUTORITATIVO E PLANO LOCAL:\\n")'),
    ("composer-hint",
     'narratorInput=input("Descreva somente a ação que o personagem ou grupo executa. Dúvidas ficam no quadro azul acima."',
     'narratorInput=input("Declare uma ação, fale com um NPC ou pergunte o que o personagem percebe nesta cena."'),
])

apply("HumanMasterNarrativeProfile.java",[
    ("perception-profile",
     '        if(!advancing)return "MODO CONSULTA FORA DA FICÇÃO: responda diretamente ao jogador, sem avançar relógio, NPC, estado ou história. Não reintroduza a cena.";',
     '        if(!advancing){\n'
     '            FictionTurnIntent.Kind intent=FictionTurnIntent.classify(action);\n'
     '            if(intent==FictionTurnIntent.Kind.FICTION_OBSERVATION||intent==FictionTurnIntent.Kind.MIXED_CONDITIONAL)\n'
     '                return "MODO PERCEPÇÃO NA FICÇÃO: responda ao personagem em sua situação atual, sem avançar relógio, cena, NPCs, combate, recursos ou estado. Não reintroduza a cena; não revele segredo nem escreva ações do personagem.";\n'
     '            return "MODO CONSULTA FORA DA FICÇÃO: responda diretamente ao jogador, sem avançar relógio, NPC, estado ou história. Não reintroduza a cena.";\n'
     '        }'),
])

apply("MasterOrchestrator.java",[
    ("real-composer-intent",
     'boolean nonAdvancing=narrative&&(explicitHelp||(!explicitAction&&BraseiroNarrativeDirector.isNonAdvancing(routedIntent)));',
     'FictionTurnIntent.Kind fictionKind=FictionTurnIntent.classify(rawAction);'
     'boolean nonAdvancing=narrative&&(explicitHelp||'
     'FictionTurnIntent.freezesWorld(fictionKind,rawAction,db.getSceneState()));'),
    ("turn-metadata-in-bus",
     '            root.put("scene",compactSceneState);root.put("party",compactPartyState);',
     '            root.put("scene",compactSceneState);\n'
     '            root.put("fiction_turn",FictionTurnIntent.contract(rawAction,sourceScene,inputChannel));\n'
     '            root.put("onstage_perception",NpcPerceptionLedger.publicOnstage(db,sourceScene,\n'
     '                db.getStoryState().optString("last_narration",""),"party"));\n'
     '            root.put("party",compactPartyState);'),
    ("classifier-as-authority",
     'explicitAction?"ação do jogador":BraseiroNarrativeDirector.intentLabel(routedIntent)',
     'explicitAction?fictionKind.name():BraseiroNarrativeDirector.intentLabel(routedIntent)'),
    ("authoritative-player-channel",
     'PLAYER_ACTION continua a ficção mesmo contendo pergunta ou fala; GM_HELP congela tempo, posição, NPCs e Mundo Vivo.',
     'PLAYER_ACTION é a origem da mensagem e pode conter observação SEM avanço, fala ou ação; GM_HELP continua consulta normativa sem avanços.'),
    ("priority-directions",
     '        if(narrative)prompt=HumanMasterNarrativeProfile.systemInstruction()+"\\n\\n"',
     '        if(narrative)prompt=FictionTurnDirective.forTurn(rawAction,db.getSceneState(),inputChannel)+"\\n\\n"+HumanMasterNarrativeProfile.systemInstruction()+"\\n\\n"'),
    ("protected-lean-context",
     '"narrative_intent","fiction_frozen","spotlight_hint","authority"',
     '"fiction_turn","onstage_perception","narrative_intent","fiction_frozen","spotlight_hint","authority"'),
    ("protected-emergency-context",
     'emergency.put("narrative_intent",lean.optString("narrative_intent"));',
     'emergency.put("fiction_turn",lean.optJSONObject("fiction_turn"));'
     'emergency.put("onstage_perception",lean.optJSONArray("onstage_perception"));'
     'emergency.put("relevant_memories",firstJson(lean.optJSONArray("relevant_memories"),2));'
     'emergency.put("narrative_intent",lean.optString("narrative_intent"));'),
    ("protected-hard-limit-context",
     '"local_tick","narrative_intent","fiction_frozen","authority"',
     '"local_tick","fiction_turn","onstage_perception","relevant_memories","narrative_intent","fiction_frozen","authority"'),
])
print("DONE: handoff channel+intention, observer NPC memory, nonadvancing overlay and compact protected context")
