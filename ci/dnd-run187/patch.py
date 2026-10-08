#!/usr/bin/env python3
"""Braseiro D&D RUN187 incremental patch; apply ONLY after RUN186 is fully reconstructed.
Usage: python3 patch.py /path/to/DND_PROJECT_ROOT
Non-destructive: existing Java is patched by strict anchors; no save/schema reset.
"""
from pathlib import Path
import sys, json, re, shutil

root=Path(sys.argv[1]).resolve()
pkg=root/'android/app/src/main/java/com/braseiro/dnd5e'
here=Path(__file__).resolve().parent
if not (pkg/'MainActivity.java').exists():
    raise SystemExit('RUN187_MISSING_ANDROID_SOURCE')

def once(src, anchor, new, label):
    n=src.count(anchor)
    if n!=1: raise SystemExit(f'RUN187_ANCHOR_{label}: expected 1, got {n}')
    return src.replace(anchor,new,1)

mainpath=pkg/'MainActivity.java'
src=mainpath.read_text(encoding='utf-8')
# Hard version gate: never try to create a "RUN187" from an older/regressed source.
# RUN186 has guided academic rolls, Brazilian Portuguese catalog and official campaign runtime.
run186_signals={
    'academic_period_dialog':any(x in src for x in ('COMO AVANÇA O SEMESTRE / ANO','COMO AVANÇAR SEMESTRE / ANO')),
    'guided_exams':'ROLAR TESTE 1' in src and 'ROLAR TESTE 2' in src,
    'guided_study':('rollStrixhavenExamCheck' in src and 'ROLAR TESTE DE ESTUDO' in src),
    'master_tools':'FERRAMENTAS DO MESTRE' in src,
    'ptbr_catalog':(pkg/'StrixhavenPtBrAcademicCatalog.java').is_file(),
    'official_campaign':(pkg/'StrixhavenOfficialCampaignRuntime.java').is_file(),
    'legacy_run163_label_removed':'RUNTIMES COMPILADOS — RUN163' not in src,
}
missing=[k for k,v in run186_signals.items() if not v]
if missing:
    raise SystemExit('RUN187_MUST_APPLY_AFTER_COMPLETE_RUN186::'+','.join(missing))
if 'HumanMasterDynamicDirector.instruction(' in src:
    print('RUN187_ALREADY_APPLIED')
    raise SystemExit(0)

anchor='''        p.append(LiveTableNarrativeDirector.contract(c,db.getSceneState(),liveParty,liveWorld,braseiroNarrativeMemory(),userText));'''
replacement=anchor+'''
        // RUN187: scene-specific creative direction is a separate, persisted runtime;
        // it does NOT mutate rules/state and never runs on GM_HELP or non-advancing turns.
        try{p.append(HumanMasterDynamicDirector.instruction(this,db,db.getSceneState(),userText,lastNarration,
                isStrixhavenAdventure()?"Strixhaven":(isMystaraWorld()?"Mystara":"D&D"),
                activeTurnNonAdvancing(userText)||"GM_HELP".equals(activeInputChannel()),
                db.getCombatState().optInt("running",0)==1,
                (pendingTest!=null&&pendingTest.length()>0)||db.getSceneState().optBoolean("canonical_locked",false)));
        }catch(Throwable dynamicError){AppLog.w("DynamicMaster","Direção criativa indisponível; Mestre canônico preservado.",dynamicError);}'''
src=once(src,anchor,replacement,'NARRATOR_PROMPT')

anchor='''        if(masterOrchestrator!=null)masterOrchestrator.afterAccepted(activeMasterPlan,lastNarration,env,pendingAiInputChars,raw==null?0:raw.length());'''
replacement=anchor+'''
        if(!nonAdvancing&&masterEnvelopeValid&&"Narrador".equals(lastAiMode)){
            // Only observe ACCEPTED narrative. The transaction manager retains exclusive
            // authority over actual state, NPC, quest, rules and world mutations.
            try{HumanMasterDynamicDirector.accepted(this,db,db.getSceneState(),action,lastNarration,env,
                isStrixhavenAdventure()?"Strixhaven":(isMystaraWorld()?"Mystara":"D&D"));}
            catch(Throwable dynamicError){AppLog.w("DynamicMaster","Falha de memória auxiliar isolada; cena preservada.",dynamicError);}
        }'''
src=once(src,anchor,replacement,'AFTER_ACCEPTED')

# RUN187: bind ONLY the prepared/filtered subset that actually survived the
# transaction and full SQLite COMMITTED barrier, never original proposed envelope.
anchor="""        final String[] combinedHolder=new String[]{""};"""
replacement=anchor+"""
        final JSONObject[] run187CommittedEnvelope=new JSONObject[]{null};"""
src=once(src,anchor,replacement,'COMMITTED_HOLDER')
anchor="""                db.markSceneCommitCommittedChecked(commitId);"""
replacement=anchor+"""
                run187CommittedEnvelope[0]=prepared;"""
src=once(src,anchor,replacement,'COMMITTED_PREPARED_ENVELOPE')

# RUN187 confirmed only after the SQLite journal's COMMITTED state;
# rejected or unapproved world events never enter dynamic-world memory.
anchor="""        // Falhas de UI DEPOIS do commit não podem ser confundidas com falha de banco e jamais
        // disparam rollback/snapshot. O estado já foi confirmado pelo journal COMMITTED.
        if(masterOrchestrator!=null)masterOrchestrator.markConsequencesCommitted(activeMasterPlan);"""
replacement=anchor+"""
        // RUN187: apply narrative memory updates only after successful SQL commit.
        try{HumanMasterDynamicDirector.confirmed(this,db,run187CommittedEnvelope[0]);}
        catch(Throwable dynamicError){AppLog.w("DynamicMaster","Commit mecânico preservado; memória auxiliar indisponível.",dynamicError);}"""
src=once(src,anchor,replacement,'AFTER_COMMIT')

# Preserve baseline save identity, manifest package and all existing functionality.
# Mutations below are permitted only after all integration anchors have matched.
mainpath.write_text(src,encoding='utf-8',newline='\n')
for filename in ['DynamicMasterState.java','SpontaneousEventEngine.java','HumanMasterDynamicDirector.java']:
    shutil.copy2(here/'java'/filename, pkg/filename)

# The older profile class is introduced by prior cumulative patches. Avoid replacing it.
profile=pkg/'HumanMasterNarrativeProfile.java'
if profile.exists():
    s=profile.read_text(encoding='utf-8')
    if 'human_master_dynamic_profile_v2' not in s:
        marker='''    public static JSONObject contract(){'''
        if marker in s:
            s=s.replace(marker,'''    // RUN187 extends the human master profile without altering its existing mechanics contract.
    public static String dynamicProfileId(){return "human_master_dynamic_profile_v2";}

'''+marker,1)
            profile.write_text(s,encoding='utf-8',newline='\n')

checks={
  'android_source':mainpath.exists(),
  'dynamic_narrator_prompt':'HumanMasterDynamicDirector.instruction(this,db,db.getSceneState(),userText,lastNarration' in src,
  'accepted_only':'if(!nonAdvancing&&masterEnvelopeValid&&"Narrador".equals(lastAiMode))' in src,
  'never_gm_help':'"GM_HELP".equals(activeInputChannel())' in src,
  'canonical_protected':'pendingTest!=null&&pendingTest.length()>0' in src,
  'director':(pkg/'HumanMasterDynamicDirector.java').exists(),
  'events':(pkg/'SpontaneousEventEngine.java').exists(),
  'narrative_memory':(pkg/'DynamicMasterState.java').exists(),
  'profile_preserved':profile.exists(),
  'event_commit_guard':'HumanMasterDynamicDirector.confirmed(this,db,run187CommittedEnvelope[0]);' in src,
  'party_backup':'session_preferences_json' in (pkg/'DynamicMasterState.java').read_text(encoding='utf-8'),
  'strixes_preserved':all(run186_signals.values()),
  'no_direct_rule_mutations':'db.saveSceneState' not in (pkg/'HumanMasterDynamicDirector.java').read_text(encoding='utf-8'),
}
print(json.dumps(checks,ensure_ascii=False,indent=2))
if not all(checks.values()): raise SystemExit('RUN187_PATCH_CONTRACT_FAILED')
print('RUN187_LOCAL_PATCH_APPLIED')
