#!/usr/bin/env python3
"""RUN188: Explicit-only offline Master, Gemini-first, recoverable retries, no auto fallback."""
from pathlib import Path
import sys,json,shutil
root=Path(sys.argv[1]); pkg=root/'android/app/src/main/java/com/braseiro/dnd5e';p=pkg/'MainActivity.java'; t=p.read_text(encoding='utf-8')
if 'RUN188_GEMINI_EXPLICIT_OFFLINE' in t:
 print('RUN188_ALREADY_APPLIED');sys.exit(0)
assert 'HumanMasterDynamicDirector.instruction(this' in t,'Requires RUN187'

def once(a,b,tag):
 global t
 count=t.count(a)
 if count!=1:raise SystemExit(f'{tag}: expected 1 got {count}')
 t=t.replace(a,b,1)

# Preserve user choice only for a single new pipeline; do not couple to old context.
once('    private long aiPipelineGeneration=0L;', '''    private long aiPipelineGeneration=0L;
    // RUN188_GEMINI_EXPLICIT_OFFLINE: offline may run ONLY after a direct tap.
    private boolean manualOfflineNextRequest=false;
    private long manualOfflinePipelineId=-1L;
    private boolean showingGeminiFailureDialog=false;
    private static final String RETRY_PREFIX="run188_gemini_failed_";
''','FIELDS')

methods='''    // RUN188: A failed Gemini request is not a scene. Preserve exact input, channel and
    // world/scene identity so Retry never silently moves the campaign forward.
    private String geminiRetryIdentity(){
        JSONObject campaign=db==null?new JSONObject():db.getCampaign();
        JSONObject scene=db==null?new JSONObject():db.getSceneState();
        JSONObject story=db==null?new JSONObject():db.getStoryState();
        return activeWorldProfile()+"|"+campaign.optString("name","")+"|"+
            scene.optString("location",scene.optString("location_name",""))+"|"+
            story.optString("last_narration","").hashCode();
    }
    private boolean hasFailedGeminiRequest(){
        return prefs!=null&&prefs.getBoolean(RETRY_PREFIX+"pending",false);
    }
    private void clearFailedGeminiRequest(){
        if(prefs!=null)prefs.edit().remove(RETRY_PREFIX+"pending").remove(RETRY_PREFIX+"mode")
          .remove(RETRY_PREFIX+"action").remove(RETRY_PREFIX+"channel")
          .remove(RETRY_PREFIX+"scene").remove(RETRY_PREFIX+"error").apply();
    }
    private void markGeminiRequestFailed(final String mode,final String action,final String channel,final String error){
        aiBusy=false;streamingNarration="";
        if(prefs!=null){
            prefs.edit().putBoolean(RETRY_PREFIX+"pending",true)
                .putString(RETRY_PREFIX+"mode",mode==null?"Narrador":mode)
                .putString(RETRY_PREFIX+"action",action==null?"":action)
                .putString(RETRY_PREFIX+"channel",channel==null?"PLAYER_ACTION":channel)
                .putString(RETRY_PREFIX+"scene",geminiRetryIdentity())
                .putString(RETRY_PREFIX+"error",error==null?"Falha desconhecida":error).commit();
        }
        setStatus("GEMINI INDISPONÍVEL. A ação não foi concluída. Escolha TENTAR NOVAMENTE ou MESTRE OFFLINE.");
        updateNarratorBusyUi();
        if(isStrixhavenAdventure())showStrixhavenSession();else if("Mestre IA".equals(currentScreen))showNarrator();
        if(!showingGeminiFailureDialog&&canShowDialog()){
            showingGeminiFailureDialog=true;
            new AlertDialog.Builder(this).setTitle("Gemini não conseguiu responder")
                .setMessage("Nenhuma resposta offline será gerada automaticamente. Sua ação foi guardada para tentar novamente.\\n\\n"+
                    (error==null?"Falha de conexão ou resposta incompleta.":error))
                .setPositiveButton("TENTAR NOVAMENTE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){retryFailedGeminiRequest();}})
                .setNeutralButton("MESTRE OFFLINE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){runExplicitOfflineMaster();}})
                .setNegativeButton("AGORA NÃO",null)
                .setOnDismissListener(new DialogInterface.OnDismissListener(){public void onDismiss(DialogInterface d){showingGeminiFailureDialog=false;}})
                .show();
        }
    }
    private void retryFailedGeminiRequest(){
        if(aiBusy){toast("O Mestre já está trabalhando.");return;}
        if(!hasFailedGeminiRequest()){toast("Nenhuma ação pendente do Gemini.");return;}
        String recorded=prefs.getString(RETRY_PREFIX+"scene","");
        if(!recorded.equals(geminiRetryIdentity())){
            toast("A cena mudou. A ação anterior foi preservada, mas deve ser revisada antes de ser reenviada.");
            return;
        }
        String mode=prefs.getString(RETRY_PREFIX+"mode","Narrador");
        String action=prefs.getString(RETRY_PREFIX+"action","");
        String channel=prefs.getString(RETRY_PREFIX+"channel","PLAYER_ACTION");
        if(action.trim().isEmpty()){toast("A ação pendente está vazia.");return;}
        manualOfflineNextRequest=false;
        sendAi(mode,action,channel);
    }
    private void runExplicitOfflineMaster(){
        if(aiBusy){toast("Aguarde a resposta atual.");return;}
        final boolean pending=hasFailedGeminiRequest();
        final String mode=pending?prefs.getString(RETRY_PREFIX+"mode","Narrador")
            :(narratorMode==null?"Narrador":String.valueOf(narratorMode.getSelectedItem()));
        final String channel=pending?prefs.getString(RETRY_PREFIX+"channel","PLAYER_ACTION"):"PLAYER_ACTION";
        final String action=pending?prefs.getString(RETRY_PREFIX+"action","")
            :(narratorInput==null?"":narratorInput.getText().toString().trim());
        if(action.trim().isEmpty()){toast("Escreva uma ação ou escolha a tentativa pendente.");return;}
        if(pending&&!prefs.getString(RETRY_PREFIX+"scene","").equals(geminiRetryIdentity())){
            toast("A cena foi alterada. Revise a ação antiga antes de escolher Mestre offline.");return;
        }
        new AlertDialog.Builder(this).setTitle("Usar Mestre offline somente nesta ação?")
            .setMessage("O motor local será usado por escolha manual. As próximas ações voltarão a tentar o Gemini normalmente.")
            .setNegativeButton("CANCELAR",null)
            .setPositiveButton("USAR OFFLINE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
                manualOfflineNextRequest=true;
                sendAi(mode,action,channel);
            }}).show();
    }
    private void addGeminiMasterChoiceButtons(LinearLayout parent){
        if(parent==null)return;
        LinearLayout panel=card();
        panel.addView(text("MESTRE: GEMINI POR PADRÃO",12,GOLD,true));
        if(hasFailedGeminiRequest()){
            panel.addView(text("O Gemini falhou. A ação continua pendente e NÃO foi resolvida pelo Mestre offline.",11,RED,false));
            Button retry=button("TENTAR NOVAMENTE (GEMINI)",GREEN);
            retry.setEnabled(!aiBusy);
            retry.setOnClickListener(new View.OnClickListener(){public void onClick(View v){retryFailedGeminiRequest();}});
            panel.addView(retry);
        }else panel.addView(text("A narração usa o Gemini. Falha de rede não ativa o motor offline.",11,MUTED,false));
        Button offline=button("MESTRE OFFLINE • USAR MANUALMENTE",PANEL2);
        offline.setEnabled(!aiBusy);
        offline.setOnClickListener(new View.OnClickListener(){public void onClick(View v){runExplicitOfflineMaster();}});
        panel.addView(offline);parent.addView(panel);
    }

'''
once('    private void sendAi(final String mode,String userText){',methods+'    private void sendAi(final String mode,String userText){','NEW_HANDLERS')
# One-time only manual offline selection at pipeline start.
once('        final long pipelineId=++aiPipelineGeneration;', '''        final long pipelineId=++aiPipelineGeneration;
        manualOfflinePipelineId=manualOfflineNextRequest?pipelineId:-1L;
        manualOfflineNextRequest=false;''','OFFLINE_PIPELINE')
# Cached narrator response not a Gemini call, skip on narrator turns.
once('if(cached.length()>0){pendingAiInputChars=0;', 'if(cached.length()>0&&!"Narrador".equals(mode)&&manualOfflinePipelineId!=preparationPipeline){pendingAiInputChars=0;', 'NARRATOR_CACHE_DISABLED')
# Change preparation pipeline branch to ONLY explicit offline.
once('''                    if(key.trim().length()<20){prefs.edit().putString("gemini_connection_state","offline").putString("gemini_last_error","chave Gemini não salva").apply();runOfflineMaster(mode,userText,localHits,"chave Gemini não salva; abra CONFIG e use SALVAR E TESTAR GEMINI");return;}''','''                    if(manualOfflinePipelineId==preparationPipeline){
                        manualOfflinePipelineId=-1L;
                        ++aiRequestGeneration;
                        runOfflineMaster(mode,userText,localHits,"escolha manual do jogador");
                        return;
                    }
                    if(key.trim().length()<20){
                        prefs.edit().putString("gemini_connection_state","offline").putString("gemini_last_error","chave Gemini não salva").apply();
                        markGeminiRequestFailed(mode,userText,inputChannel,"Chave Gemini não configurada. Abra Configurações e salve a chave, depois tente novamente.");
                        return;
                    }''','NO_KEY_MANUAL')
start='''                            String streamProblem=narrationQualityProblem(streamingNarration,userText);'''
end='''                            aiBusy=false;streamingNarration="";runOfflineMaster(mode,userText,localHits,"falha de conexão: "+error);'''
i=t.index(start);j=t.index(end,i)+len(end)
t=t[:i]+'''                            // No automatic acceptance of partial streaming and NO offline fallback.
                            // Partial output is not a validated scene and must never be committed.
                            if(aiQuotaGovernor!=null)aiQuotaGovernor.recordCall(pendingAiInputChars,streamingNarration.length());
                            markGeminiRequestFailed(mode,userText,inputChannel,"Falha da API Gemini: "+(error==null?"sem detalhes":error));'''+t[j:]
# Every successful response clears pending failure ONLY after processing, not on intermediate API success.
once('''    private void handleAiResponse(String raw,String action){
        JSONObject env=parseAiEnvelope(raw);''','''    private void handleAiResponse(String raw,String action){
        JSONObject env=parseAiEnvelope(raw);''','RESPONSE_ANCHOR')
# Insert near tail after successful response, only if reached end; support GM_HELP early return.
once('''restoreAfterMasterSideQuery();setStatus("Pergunta respondida no quadro azul.''','''restoreAfterMasterSideQuery();clearFailedGeminiRequest();setStatus("Pergunta respondida no quadro azul.''','HELP_SUCCESS_CLEAR')
once('''        setStatus(nonAdvancing?"Dúvida respondida.''','''        clearFailedGeminiRequest();
        setStatus(nonAdvancing?"Dúvida respondida.''','SCENE_SUCCESS_CLEAR')
# Rejected response path: no auto offline, same exact request retained.
once('''                runOfflineMaster(mode,originalUserText,new ArrayList<JSONObject>(),"resposta remota incompleta");''','''                markGeminiRequestFailed(mode,originalUserText,activeInputChannel(),"O Gemini retornou uma resposta incompleta. Nenhum turno foi confirmado.");''','SHORT_REMOTE')
once('''if(hasInternalNarrationLeak(draft)){aiBusy=false;streamingNarration="";runOfflineMaster(mode,originalUserText,new ArrayList<JSONObject>(),"resposta remota continha instruções internas");return;}''','''if(hasInternalNarrationLeak(draft)){aiBusy=false;streamingNarration="";markGeminiRequestFailed(mode,originalUserText,activeInputChannel(),"A resposta continha instruções internas e foi bloqueada. Nada foi aplicado.");return;}''','LEAK_REMOTE')
# Preparation problem no automatic fallback but preserve pending retry.
once('''ui.post(new Runnable(){public void run(){if(!aiBusy||preparationPipeline!=aiPipelineGeneration)return;aiBusy=false;streamingNarration="";setStatus("Falha ao montar o contexto do Mestre: "+safeMessage(failure));toast("A preparação da cena falhou sem travar a interface.");if("Mestre IA".equals(currentScreen))showNarrator();}});''','''ui.post(new Runnable(){public void run(){if(!aiBusy||preparationPipeline!=aiPipelineGeneration)return;markGeminiRequestFailed(mode,userText,inputChannel,"Falha ao preparar o contexto: "+safeMessage(failure));}});''','CONTEXT_FAILURE')
# Render choice on Strixhaven screen always, and narrator main.
once('''        addStrixhavenMasterConversationCard(drawer);''','''        addStrixhavenMasterConversationCard(drawer);
        addGeminiMasterChoiceButtons(drawer);''','STRIX_UI')
once('''        narratorProgress=new ProgressBar(this);''','''        addGeminiMasterChoiceButtons(b);
        narratorProgress=new ProgressBar(this);''','NARRATOR_UI')
# Avoid automatic fallback from other parts of primary narrator path.
assert t.count('runOfflineMaster(')==2,[(i,l.strip()[:160]) for i,l in enumerate(t.splitlines(),1) if 'runOfflineMaster(' in l]
p.write_text(t,encoding='utf-8',newline='\n')
print(json.dumps({'result':'RUN188_PATCH_APPLIED','source':str(p),'remainingOfflineCalls':t.count('runOfflineMaster('),'geminiFailureChoice':t.count('markGeminiRequestFailed('),'manualOfflineButton':'MESTRE OFFLINE • USAR MANUALMENTE' in t},ensure_ascii=False))
