#!/usr/bin/env python3
"""Bounded pre-Gemini planning; preserve actions and invalidate late callbacks."""
from pathlib import Path
p=Path("app/src/main/java/com/braseiro/dnd5e/MainActivity.java")
s=p.read_text(encoding="utf-8")
old='''        setStatus(retrievalSkipped?"Contexto local pronto; montando o estado da campanha sem nova busca de regras...":"Regras localizadas; montando o estado da campanha...");
        AppExecutors.io().execute(new Runnable(){public void run(){
'''
new='''        setStatus(retrievalSkipped?"Contexto local pronto; montando o estado da campanha sem nova busca de regras...":"Regras localizadas; montando o estado da campanha...");
        // 6.6.60: the previous prepare() had no watchdog. On a stalled IO queue
        // aiBusy remained true forever, blocking every further player action.
        // This timeout NEVER pretends the action succeeded or invokes offline AI.
        final java.util.concurrent.atomic.AtomicBoolean planCallbackArrived=new java.util.concurrent.atomic.AtomicBoolean(false);
        ui.postDelayed(new Runnable(){public void run(){
            if(planCallbackArrived.get()||!aiBusy||pipelineId!=aiPipelineGeneration)return;
            ++aiPipelineGeneration; // reject any stale planning callback/turn.
            AppLog.e("MainActivity","Master context prepare exceeded 20 seconds; preserving action",new IllegalStateException("MasterOrchestrator.prepare timeout"));
            markMasterRequestFailed(mode,requestedText,inputChannel,
                "CONTEXTO: A preparação do estado da campanha excedeu 20 segundos. Nenhuma chamada ao Gemini foi feita. A ação foi preservada para tentar novamente.",false);
        }},20000L);
        AppExecutors.io().execute(new Runnable(){public void run(){
'''
assert s.count(old)==1,('missing plan anchor',s.count(old))
s=s.replace(old,new,1)
old2='''                if(!aiBusy||pipelineId!=aiPipelineGeneration)return;
                if(readyFailure!=null){aiBusy=false;activeMasterPlan=null;'''
new2='''                planCallbackArrived.set(true);
                if(!aiBusy||pipelineId!=aiPipelineGeneration)return;
                if(readyFailure!=null){aiBusy=false;activeMasterPlan=null;'''
assert s.count(old2)==1,('missing plan callback',s.count(old2))
s=s.replace(old2,new2,1)
old3='''        final long preparationPipeline=aiPipelineGeneration;
        if(openingRequest)adventureStartupCoordinator.phase'''
new3='''        final long preparationPipeline=aiPipelineGeneration;
        final long promptStartRequestGeneration=aiRequestGeneration;
        // The prompt-assembly stage was also unbounded; never strand the UI
        // if it is starved behind another IO job. Once a Gemini request starts
        // aiRequestGeneration changes and this watchdog becomes inert.
        ui.postDelayed(new Runnable(){public void run(){
            if(!aiBusy||preparationPipeline!=aiPipelineGeneration
                ||aiRequestGeneration!=promptStartRequestGeneration)return;
            ++aiPipelineGeneration;
            AppLog.e("MainActivity","Master prompt assembly exceeded 25 seconds; preserving action",new IllegalStateException("Prompt assembly timeout"));
            markMasterRequestFailed(mode,userText,inputChannel,
                "CONTEXTO: A montagem do prompt excedeu 25 segundos. O Gemini não recebeu esta ação. A ação foi preservada para nova tentativa.",false);
        }},25000L);
        if(openingRequest)adventureStartupCoordinator.phase'''
assert s.count(old3)==1,('missing prompt anchor',s.count(old3))
s=s.replace(old3,new3,1)
old4='''        final boolean chronologyFailure=!apiFailure&&error!=null&&error.startsWith("CONTINUIDADE:");
        setStatus(apiFailure?"GEMINI INDISPONÍVEL. A ação não foi concluída.":
            (chronologyFailure?"CONTRADIÇÃO NA CONTINUIDADE. A cena foi preservada e a nova resposta não foi aplicada.":
            "PROPOSTA DE TESTE INVÁLIDA. A ação continua pendente, sem rolagem ou avanço."));'''
new4='''        final boolean chronologyFailure=!apiFailure&&error!=null&&error.startsWith("CONTINUIDADE:");
        final boolean contextTimeout=!apiFailure&&error!=null&&error.startsWith("CONTEXTO:");
        setStatus(contextTimeout?"PREPARAÇÃO DEMOROU DEMAIS. A ação está salva para tentar novamente.":
            (apiFailure?"GEMINI INDISPONÍVEL. A ação não foi concluída.":
            (chronologyFailure?"CONTRADIÇÃO NA CONTINUIDADE. A cena foi preservada e a nova resposta não foi aplicada.":
            "PROPOSTA DE TESTE INVÁLIDA. A ação continua pendente, sem rolagem ou avanço.")));'''
assert s.count(old4)==1,('missing status anchor',s.count(old4))
s=s.replace(old4,new4,1)
old5='''.setTitle(apiFailure?"Gemini não conseguiu responder":(chronologyFailure?"Contradição de continuidade bloqueada":"O Mestre não conseguiu preparar o teste"))
                .setMessage((apiFailure?"Nenhuma resposta offline será gerada automaticamente. ":(chronologyFailure?"A nova narração contradiz registros canônicos. ":"A proposta mecânica não passou na validação. "))+"Sua ação foi preservada para tentar novamente.\\n\\n"+'''
new5='''.setTitle(contextTimeout?"Preparação do Mestre demorou demais":(apiFailure?"Gemini não conseguiu responder":(chronologyFailure?"Contradição de continuidade bloqueada":"O Mestre não conseguiu preparar o teste")))
                .setMessage((contextTimeout?"O contexto não ficou pronto a tempo. Nenhuma ação foi aplicada. ":(apiFailure?"Nenhuma resposta offline será gerada automaticamente. ":(chronologyFailure?"A nova narração contradiz registros canônicos. ":"A proposta mecânica não passou na validação. ")))+"Sua ação foi preservada para tentar novamente.\\n\\n"+'''
assert s.count(old5)==1,('missing dialog anchor',s.count(old5))
s=s.replace(old5,new5,1)
p.write_text(s,encoding="utf-8")
print('PATCHED_PLAN_AND_PROMPT_WATCHDOGS',s.count('CONTEXTO: '),s.count('planCallbackArrived'))
