#!/usr/bin/env python3
"""Exact, fail-closed patch to the Android 6.6.59 pre-Gemini stage.
The large MainActivity is edited only after content anchors match exactly.
Run from repo root or dnd-normal/run247-mod005.
"""
from pathlib import Path

root=Path("dnd-normal/run247-mod005")
if not root.exists(): root=Path(".")
target=root/"app/src/main/java/com/braseiro/dnd5e/MainActivity.java"
original=target.read_text(encoding="utf-8")
code=original

def replace_once(old,new,label):
    global code
    matches=code.count(old)
    if matches!=1: raise AssertionError(f"{label}: expected exactly 1 matching anchor, found {matches}")
    code=code.replace(old,new,1)

replace_once("import java.util.concurrent.atomic.AtomicBoolean;",
             "import java.util.concurrent.atomic.AtomicBoolean;\nimport java.util.concurrent.Callable;",
             "Callable import")
replace_once("    private static final long OPENING_RULE_SEARCH_WATCHDOG_MS=3500L;",
             """    private static final long OPENING_RULE_SEARCH_WATCHDOG_MS=3500L;
    // Pre-Gemini world/party/knowledge snapshot must never leave aiBusy locked.
    private static final long MASTER_PREPARATION_WATCHDOG_MS=30000L;
    private MasterPreparationCoordinator masterContextPreparation;""",
             "Master preparation guard fields")
replace_once("    protected void onDestroy(){if(uiResponsivenessMonitor!=null)uiResponsivenessMonitor.stop();",
             """    protected void onDestroy(){
        if(masterContextPreparation!=null)masterContextPreparation.cancel();
        if(uiResponsivenessMonitor!=null)uiResponsivenessMonitor.stop();""",
             "cancel on destroy")
replace_once('          .remove(RETRY_PREFIX+"scene").remove(RETRY_PREFIX+"error").remove(RETRY_PREFIX+"api_failure").apply();',
             '          .remove(RETRY_PREFIX+"scene").remove(RETRY_PREFIX+"error").remove(RETRY_PREFIX+"api_failure").remove(RETRY_PREFIX+"context_failure").apply();',
             "clear context-specific retry marker")

needle="    private void markGeminiRequestFailed(final String mode,final String action,final String channel,final String error){"
replace_once(needle,'''    private void markMasterPreparationFailed(final String mode,final String action,
            final String channel,final String explanation){
        // No Gemini request has started; the action may be retried from the same
        // persisted scene, but NEVER silently finished by an offline engine.
        activeMasterPlan=null;pendingAiInputChars=0;
        if(masterOrchestrator!=null)masterOrchestrator.setActivePlanForValidation(null);
        markMasterRequestFailed(mode,action,channel,
            "CONTEXTO DO MESTRE: "+(explanation==null?"preparação interrompida":explanation),false);
    }

'''+needle,"preparation failure and retry path")

start=code.index("    private void markMasterRequestFailed(")
end=code.index("    private void retryFailedGeminiRequest()",start)
block=code[start:end]
def repl_block(old,new,label):
    global block
    n=block.count(old)
    if n!=1:raise AssertionError(f"{label}: expected 1, got {n}")
    block=block.replace(old,new,1)
repl_block('        aiBusy=false;streamingNarration="";',
'''        aiBusy=false;streamingNarration="";
        final boolean contextFailure=!apiFailure&&error!=null&&error.startsWith("CONTEXTO DO MESTRE:");''',
"derive context failure kind")
repl_block('.putString(RETRY_PREFIX+"error",error==null?"Falha desconhecida":error).putBoolean(RETRY_PREFIX+"api_failure",apiFailure).commit();',
'''.putString(RETRY_PREFIX+"error",error==null?"Falha desconhecida":error)
                .putBoolean(RETRY_PREFIX+"api_failure",apiFailure)
                .putBoolean(RETRY_PREFIX+"context_failure",contextFailure).commit();''',
"persist new failure type")
repl_block('        setStatus(apiFailure?"GEMINI INDISPONÍVEL. A ação não foi concluída.":',
'''        setStatus(contextFailure?"O contexto do Mestre não ficou pronto. A ação continua pendente; tente novamente.":(
            apiFailure?"GEMINI INDISPONÍVEL. A ação não foi concluída.":''',
"correct pre-Gemini footer")
repl_block('            "PROPOSTA DE TESTE INVÁLIDA. A ação continua pendente, sem rolagem ou avanço."));',
'            "PROPOSTA DE TESTE INVÁLIDA. A ação continua pendente, sem rolagem ou avanço.")));',
"close footer ternary")
repl_block('new AlertDialog.Builder(this).setTitle(apiFailure?"Gemini não conseguiu responder":',
'new AlertDialog.Builder(this).setTitle(contextFailure?"A preparação do Mestre foi interrompida":(apiFailure?"Gemini não conseguiu responder":',
"accurate dialog title")
repl_block('(chronologyFailure?"Contradição de continuidade bloqueada":"O Mestre não conseguiu preparar o teste"))',
'(chronologyFailure?"Contradição de continuidade bloqueada":"O Mestre não conseguiu preparar o teste")))',
"close title ternary")
repl_block('.setMessage((apiFailure?"Nenhuma resposta offline será gerada automaticamente. ":(chronologyFailure?"A nova narração contradiz registros canônicos. ":"A proposta mecânica não passou na validação. "))+"Sua ação foi preservada para tentar novamente.\\n\\n"+',
'''.setMessage((contextFailure?"O contexto operacional não ficou pronto. A jogada não foi concluída e nenhuma cena nova foi confirmada. ":
                    (apiFailure?"Nenhuma resposta offline será gerada automaticamente. ":
                    (chronologyFailure?"A nova narração contradiz registros canônicos. ":"A proposta mecânica não passou na validação. ")))+
                    "Sua ação foi preservada para tentar novamente.\\n\\n"+''',
"accurate context dialog message")
code=code[:start]+block+code[end:]

start=code.index("    private void addGeminiMasterChoiceButtons(")
end=code.index("    private void sendAi(final String mode,String userText)",start)
block=code[start:end]
old='panel.addView(text(prefs.getBoolean(RETRY_PREFIX+"api_failure",true)?"O Gemini falhou. A ação continua pendente e NÃO foi resolvida pelo Mestre offline.":"O Mestre gerou uma proposta inválida de teste. A ação continua pendente, sem rolagem e sem avançar a cena.",11,RED,false));'
new='panel.addView(text(prefs.getBoolean(RETRY_PREFIX+"context_failure",false)?"Preparação do Mestre interrompida. A ação foi preservada sem avançar a cena.":(prefs.getBoolean(RETRY_PREFIX+"api_failure",true)?"O Gemini falhou. A ação continua pendente e NÃO foi resolvida pelo Mestre offline.":"O Mestre gerou uma proposta inválida de teste. A ação continua pendente, sem rolagem e sem avançar a cena."),11,RED,false));'
if block.count(old)!=1:raise AssertionError("retry panel: old status not located")
block=block.replace(old,new,1)
block=block.replace('Button retry=button("TENTAR NOVAMENTE (GEMINI)",GREEN);',
'''Button retry=button(prefs.getBoolean(RETRY_PREFIX+"context_failure",false)?
                "TENTAR NOVAMENTE":"TENTAR NOVAMENTE (GEMINI)",GREEN);''')
code=code[:start]+block+code[end:]

start=code.index("    private void deliverAiRuleSearch(")
end=code.index("    /** Continua a chamada somente depois que a biblioteca local terminou. */",start)
block=code[start:end]
begin=block.index("        AppExecutors.io().execute(new Runnable(){public void run(){")
stop=block.index("        }});",begin)+len("        }});")
previous=block[begin:stop]
for required in ("masterOrchestrator.prepare(", "readyFailure", "continueSendAiAfterRuleSearch"):
    if required not in previous:raise AssertionError("Unexpected original master orchestration body missing "+required)
replacement='''        if(masterContextPreparation==null)
            masterContextPreparation=new MasterPreparationCoordinator(
                ui,AppExecutors.masterPreparation(),MASTER_PREPARATION_WATCHDOG_MS);
        masterContextPreparation.prepare(new Callable<MasterOrchestrator.Plan>(){
            @Override public MasterOrchestrator.Plan call(){
                // The source scene, party, NPC and knowledge state are assembled
                // off the UI thread, isolated from RAG/index/image I/O queues.
                return masterOrchestrator.prepare(mode,requestedText,mapContext,
                    audioDiagnosticSnapshot(),retrieval==null?new JSONObject():retrieval.bundle,inputChannel);
            }
        },new MasterPreparationCoordinator.Callbacks<MasterOrchestrator.Plan>(){
            @Override public void onPrepared(MasterOrchestrator.Plan readyPlan){
                if(!aiBusy||pipelineId!=aiPipelineGeneration)return;
                if(readyPlan==null){
                    markMasterPreparationFailed(mode,requestedText,inputChannel,
                        "O contexto retornou vazio; não houve chamada ao Gemini.");
                    return;
                }
                try {
                    continueSendAiAfterRuleSearch(mode,requestedText,mapContext,
                        retrieval,readyPlan,inputChannel);
                }catch(Throwable error){
                    AppLog.e("MainActivity","Falha ao concluir contexto preparado do Mestre.",error);
                    markMasterPreparationFailed(mode,requestedText,inputChannel,
                        "Não foi possível continuar depois de montar o contexto: "+safeMessage(error));
                }
            }
            @Override public void onFailure(Throwable error){
                if(!aiBusy||pipelineId!=aiPipelineGeneration)return;
                AppLog.e("MainActivity","Falha ao montar contexto do Mestre fora da UI.",error);
                markMasterPreparationFailed(mode,requestedText,inputChannel,
                    "Falha de preparação, tarefa ocupada ou rejeitada: "+safeMessage(error));
            }
            @Override public void onTimeout(){
                if(!aiBusy||pipelineId!=aiPipelineGeneration)return;
                ++aiPipelineGeneration; // invalidate every late callback from this turn
                markMasterPreparationFailed(mode,requestedText,inputChannel,
                    "A montagem do estado da campanha excedeu 30 segundos. "+
                    "A tarefa anterior foi cancelada; nenhuma nova cena foi confirmada.");
            }
        });'''
block=block[:begin]+replacement+block[stop:]
code=code[:start]+block+code[end:]

start=code.index("    private void continueSendAiAfterRuleSearch(")
end=code.index("        if(masterOrchestrator!=null)masterOrchestrator.setActivePlanForValidation(activeMasterPlan);",start)
block=code[start:end]
oldstart=block.index("        if(activeMasterPlan!=null&&!activeMasterPlan.contextReady){")
block=block[:oldstart]+'''        if(activeMasterPlan!=null&&!activeMasterPlan.contextReady){
            String reason=activeMasterPlan.contextError.length()==0?
                "O estado operacional não pôde ser montado com segurança.":
                activeMasterPlan.contextError;
            markMasterPreparationFailed(mode,userText,inputChannel,reason);
            return;
        }
'''
code=code[:start]+block+code[end:]

assert "MASTER_PREPARATION_WATCHDOG_MS=30000L" in code
assert "masterContextPreparation.prepare(new Callable<MasterOrchestrator.Plan>()" in code
assert "AppExecutors.masterPreparation()" in code
assert "markMasterPreparationFailed" in code
assert "CONTEXTO DO MESTRE:" in code
assert "AppExecutors.io().execute(new Runnable(){public void run(){\n            MasterOrchestrator.Plan plan=null;" not in code
if code==original: raise AssertionError("No source changes")
target.write_text(code,encoding="utf-8")
version=root/"version.properties"
v=version.read_text(encoding="utf-8")
assert v.count("VERSION_CODE=203900622")==1 and "DB_SCHEMA=61" in v and "BACKUP_FORMAT=21" in v,v
v=v.replace("VERSION_CODE=203900622","VERSION_CODE=203900623",1)
assert v.count("VERSION_NAME=6.6.59-live-scene-actions-recovery-QA")==1,v
v=v.replace("VERSION_NAME=6.6.59-live-scene-actions-recovery-QA",
"VERSION_NAME=6.6.60-master-context-watchdog-QA",1)
version.write_text(v,encoding="utf-8")
print("MASTER_PREP_WATCHDOG_PATCH=PASS")
print("MAIN_BEFORE_BYTES",len(original),"MAIN_AFTER_BYTES",len(code))
