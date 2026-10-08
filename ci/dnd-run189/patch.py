#!/usr/bin/env python3
"""RUN189: non-destructive character autonomy & transactional curation, requires RUN188.
Core D&D rule implementations are not edited. Fail closed when expected anchors drift.
"""
from pathlib import Path
import sys, shutil, json
root=Path(sys.argv[1]); pkg=root/'android/app/src/main/java/com/braseiro/dnd5e'; dbp=pkg/'AppDb.java'; mp=pkg/'MainActivity.java'
db=dbp.read_text(encoding='utf-8'); m=mp.read_text(encoding='utf-8')
if 'RUN189_AGENCY_SCHEMA' in db: print('RUN189_ALREADY_APPLIED');sys.exit(0)
if 'RUN188_GEMINI_EXPLICIT_OFFLINE' not in m:raise SystemExit('RUN189_REQUIRES_RUN188')

def replace(s,old,new,label):
    n=s.count(old)
    if n!=1:raise SystemExit(f'RUN189_BAD_ANCHOR_{label}: expected 1, got {n}')
    return s.replace(old,new,1)

db=replace(db,'private static final int DB_VERSION = 60;','private static final int DB_VERSION = 61; // RUN189_AGENCY_SCHEMA','version')
db=replace(db,'        ensureBarbaraHardeningSchema(db);\n    }\n\n    public void onUpgrade', '        ensureBarbaraHardeningSchema(db);\n        ensureCompanionProposalsSchema(db);\n    }\n\n    public void onUpgrade','create')
db=replace(db,'        if (oldVersion < 60) createVirtualMasterTables(db);', '        if (oldVersion < 60) createVirtualMasterTables(db);\n        if (oldVersion < 61) ensureCompanionProposalsSchema(db);','upgrade')

methods='''    // RUN189: independent persisted intent queue. Never commits effects of a proposal.
    private static void ensureCompanionProposalsSchema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS companion_proposals (world_profile TEXT NOT NULL, proposal_id TEXT NOT NULL, character_id INTEGER NOT NULL, character_name TEXT NOT NULL, scene_fingerprint TEXT NOT NULL, action_text TEXT NOT NULL, reason_observed TEXT NOT NULL, cost_risk TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'pending', queued_action_id INTEGER DEFAULT 0, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(world_profile, proposal_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_companion_proposals_pending ON companion_proposals(world_profile,status,created_at DESC)");
    }
    public synchronized boolean stageCompanionProposal(String proposalId,long charId,String name,String sceneKey,String action,String reason,String risk){
        if(proposalId==null||proposalId.length()<16||charId<=0||action.trim().isEmpty()||reason.trim().isEmpty())return false;
        SQLiteDatabase sql=getWritableDatabase();ensureCompanionProposalsSchema(sql);
        JSONObject control=getCharacterControl(charId);
        if(!"curated".equals(CompanionAgencyEngine.mode(control.optString("control_mode"))))return false;
        ContentValues v=new ContentValues();v.put("world_profile",getActiveMapProfile());v.put("proposal_id",proposalId);v.put("character_id",charId);v.put("character_name",name);v.put("scene_fingerprint",sceneKey);v.put("action_text",action);v.put("reason_observed",reason);v.put("cost_risk",risk);v.put("status","pending");v.put("created_at",System.currentTimeMillis());v.put("updated_at",System.currentTimeMillis());
        return sql.insertWithOnConflict("companion_proposals",null,v,SQLiteDatabase.CONFLICT_IGNORE)>0;
    }
    public List<JSONObject> listCompanionProposals(String status,int limit){
        ensureCompanionProposalsSchema(getWritableDatabase());
        return queryList("SELECT * FROM companion_proposals WHERE world_profile=? AND status=? ORDER BY created_at DESC LIMIT "+Math.max(1,Math.min(100,limit)),new String[]{getActiveMapProfile(),status});
    }
    public synchronized boolean updateCompanionProposal(String proposalId,String status,String action,String reason,String fingerprint){
        SQLiteDatabase sql=getWritableDatabase();ensureCompanionProposalsSchema(sql);
        JSONObject p=queryOne("SELECT * FROM companion_proposals WHERE world_profile=? AND proposal_id=? AND status='pending'",new String[]{getActiveMapProfile(),proposalId});if(p.length()==0)return false;
        if(!"curated".equals(CompanionAgencyEngine.mode(getCharacterControl(p.optLong("character_id")).optString("control_mode"))))return false;
        JSONObject hero=queryOne("SELECT * FROM characters WHERE world_profile=? AND id=? AND active=1",new String[]{getActiveMapProfile(),String.valueOf(p.optLong("character_id"))});
        if("rejected".equals(status)){
            ContentValues declined=new ContentValues();declined.put("status","rejected");declined.put("updated_at",System.currentTimeMillis());
            return sql.update("companion_proposals",declined,"world_profile=? AND proposal_id=? AND status='pending'",new String[]{getActiveMapProfile(),proposalId})==1;
        }
        if(hero.length()==0||!CompanionAgencyEngine.characterCanPropose(this,hero))return false;
        if(!fingerprint.equals(p.optString("scene_fingerprint"))||!fingerprint.equals(CompanionAgencyEngine.sceneFingerprint(this,hero)))return false;
        if(!"approved".equals(status)&&!"rejected".equals(status)&&!"altered".equals(status))return false;
        boolean owner=!sql.inTransaction();if(owner)sql.beginTransaction();
        try{
            ContentValues v=new ContentValues();v.put("updated_at",System.currentTimeMillis());
            if("altered".equals(status)){if(action==null||action.trim().length()<4)return false;v.put("action_text",action.trim());v.put("reason_observed",reason==null?"Alteração do jogador":reason.trim());}
            else v.put("status",status);
            if("approved".equals(status)){
                ContentValues q=new ContentValues();q.put("world_profile",getActiveMapProfile());q.put("character_id",p.optLong("character_id"));q.put("character_name",p.optString("character_name"));q.put("action_text",action==null?p.optString("action_text"):action.trim());q.put("intent_type","curated_approved");q.put("status","queued");q.put("created_at",System.currentTimeMillis());
                long actionId=sql.insertOrThrow("party_action_queue",null,q);v.put("queued_action_id",actionId);
            }
            int count=sql.update("companion_proposals",v,"world_profile=? AND proposal_id=? AND status='pending'",new String[]{getActiveMapProfile(),proposalId});
            if(count!=1)throw new IllegalStateException("Curadoria concorrente, não aplicar ação duas vezes.");
            if(owner)sql.setTransactionSuccessful();return true;
        }finally{if(owner)sql.endTransaction();}
    }
    private void restoreCompanionProposals(SQLiteDatabase sql,JSONArray rows,Map<Long,Long> characterIdMap,String world){
        ensureCompanionProposalsSchema(sql);sql.delete("companion_proposals","world_profile=?",new String[]{world});
        if(rows==null)return;
        for(int i=0;i<rows.length();i++){
            JSONObject p=rows.optJSONObject(i);if(p==null)continue;Long mapped=characterIdMap.get(p.optLong("character_id",0));if(mapped==null||mapped<=0)continue;
            ContentValues v=jsonValues(p);v.put("world_profile",world);v.put("character_id",mapped);
            // Queued action ids may be remapped during restore; never reuse an old id.
            if("approved".equals(p.optString("status")))v.put("queued_action_id",0);
            sql.insertWithOnConflict("companion_proposals",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

'''
db=replace(db,'    private static void createVirtualMasterTables(SQLiteDatabase db) {', methods+'    private static void createVirtualMasterTables(SQLiteDatabase db) {','methods')
db=replace(db,'        root.put("characterControls",new JSONArray(queryList("SELECT * FROM character_control_profiles WHERE world_profile=? ORDER BY character_id",new String[]{profile})));', '        root.put("characterControls",new JSONArray(queryList("SELECT * FROM character_control_profiles WHERE world_profile=? ORDER BY character_id",new String[]{profile})));\n        root.put("companionProposals",new JSONArray(queryList("SELECT * FROM companion_proposals WHERE world_profile=? ORDER BY created_at",new String[]{profile})));','export')
db=replace(db,'            if(root.has("characterControls"))restoreCharacterControls(db,root.optJSONArray("characterControls"),characterIdMap);','            if(root.has("characterControls"))restoreCharacterControls(db,root.optJSONArray("characterControls"),characterIdMap);\n            if(root.has("companionProposals"))restoreCompanionProposals(db,root.optJSONArray("companionProposals"),characterIdMap,backupProfile);','import')
db=replace(db,'"party_memberships","character_control_profiles","knowledge_facts"','"party_memberships","character_control_profiles","companion_proposals","knowledge_facts"','health')

m=replace(m,'new String[]{"Manual","Assistido","Companheiro","Automático em combate"}', 'new String[]{"EU CONTROLO","MESTRE CONTROLA","MESTRE COM CURADORIA"}','party_modes')
m=replace(m,'new String[]{"Você","Sistema — rotina / combate assistido"},"system".equals(initialControl)?"Sistema — rotina / combate assistido":"Você"','new String[]{"EU CONTROLO","MESTRE CONTROLA","MESTRE COM CURADORIA"},controlLabel(initialControl)','hero_modes')
assert m.count('f.controller.getSelectedItemPosition()==1?"system":"manual"')==2,'hero_save occurrences'; m=m.replace('f.controller.getSelectedItemPosition()==1?"system":"manual"','controlMode(String.valueOf(f.controller.getSelectedItem()))')
m=replace(m,'"Sistema: somente rotina reversível e de baixo risco. Em combate, o sistema sugere e você confirma. Uma fala ou ação que você escrever para este herói sempre prevalece naquele momento."','"EU CONTROLO: nenhuma ação sem comando seu. MESTRE CONTROLA: iniciativas motivadas, limitadas pela ficha e pelas regras. MESTRE COM CURADORIA: ações relevantes aguardam APROVAR / ALTERAR / RECUSAR."','hero_explanation')
m=replace(m,'    private static String controlLabel(String mode){if("assisted".equals(mode))return "Assistido";if("companion".equals(mode))return "Companheiro";if("combat_auto".equals(mode))return "Automático em combate";return "Manual";}\n    private static String controlMode(String label){if("Assistido".equals(label))return "assisted";if("Companheiro".equals(label))return "companion";if("Automático em combate".equals(label))return "combat_auto";return "manual";}',
'''    private static String controlLabel(String mode){String n=CompanionAgencyEngine.mode(mode);if("companion".equals(n))return "MESTRE CONTROLA";if("curated".equals(n))return "MESTRE COM CURADORIA";return "EU CONTROLO";}
    private static String controlMode(String label){if("MESTRE CONTROLA".equals(label))return "companion";if("MESTRE COM CURADORIA".equals(label))return "curated";return "manual";}''','labels')
m=replace(m,'        p.append(LiveTableNarrativeDirector.contract(c,db.getSceneState(),liveParty,liveWorld,braseiroNarrativeMemory(),userText));',
'        p.append(LiveTableNarrativeDirector.contract(c,db.getSceneState(),liveParty,liveWorld,braseiroNarrativeMemory(),userText));\n        p.append(CompanionAgencyEngine.instruction(db,safeActiveCharacters(),activeTurnNonAdvancing(userText)));','narration_agency')
m=replace(m,'        JSONObject env=parseAiEnvelope(raw);if(masterOrchestrator!=null)env=masterOrchestrator.validateEnvelope(env,action);',
'''        JSONObject rawAgencyEnvelope=parseAiEnvelope(raw);
        JSONObject env=copyJsonObject(rawAgencyEnvelope);if(masterOrchestrator!=null)env=masterOrchestrator.validateEnvelope(env,action);
        // RUN189: a curated intention is captured BEFORE narration/state commit.
        // Neither the proposal nor any effects accompanying it are accepted now.
        if(!masterSideQueryActive&&"Narrador".equals(lastAiMode)&&!activeTurnNonAdvancing(action)){
            try{
                JSONObject verdict=env.optJSONObject("_master_validation");
                if(verdict!=null&&verdict.optBoolean("valid",false)){
                    int captured=CompanionAgencyEngine.stageCurated(db,rawAgencyEnvelope,safeActiveCharacters());
                    if(captured>0){
                        aiBusy=false;streamingNarration="";clearFailedGeminiRequest();
                        setStatus("Curadoria: "+captured+" proposta(s) pendente(s). Nenhuma consequência da resposta foi aplicada.");
                        showPartyDirector();return;
                    }
                }
            }catch(Exception e){AppLog.e("CompanionAgency","Falha ao armazenar propostas, sem alterar a campanha",e);}
        }''','response_guard')
m=replace(m,'        Button cognitiveAudit=button("AUDITAR NÚCLEO DO MESTRE",PANEL2);',
'''        Button companionIdeas=button("O QUE OS COMPANHEIROS ACHAM?",PANEL2);
        companionIdeas.setOnClickListener(new View.OnClickListener(){public void onClick(View v){requestCompanionIdeas();}});overview.addView(companionIdeas);
        Button cognitiveAudit=button("AUDITAR NÚCLEO DO MESTRE",PANEL2);''','ideas_button')
m=replace(m,'        LinearLayout controls=card();controls.addView(text("CONTROLE DE CADA HERÓI",16,GOLD,true));',
'''        LinearLayout proposals=card();proposals.addView(text("PROPOSTAS PARA SUA CURADORIA",16,GOLD,true));
        List<JSONObject> pendingProposals=db.listCompanionProposals("pending",35);
        if(pendingProposals.isEmpty())proposals.addView(text("Nenhuma decisão aguardando aprovação.",12,MUTED,false));
        for(final JSONObject proposed:pendingProposals){
            proposals.addView(text(proposed.optString("character_name")+" percebeu: "+proposed.optString("reason_observed")+"\\nPropõe: "+proposed.optString("action_text")+"\\nCusto/risco conhecido: "+proposed.optString("cost_risk"),13,TEXT,false));
            LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
            final String proposalId=proposed.optString("proposal_id"),snapshot=proposed.optString("scene_fingerprint");
            Button approve=smallButton("APROVAR",GREEN),alter=smallButton("ALTERAR",PANEL2),reject=smallButton("RECUSAR",RED);
            approve.setOnClickListener(new View.OnClickListener(){public void onClick(View v){boolean ok=db.updateCompanionProposal(proposalId,"approved",null,null,snapshot);toast(ok?"Ação autorizada e posta na fila; ainda passará pelo motor de regras.":"Proposta obsoleta ou já decidida; nada executado.");showPartyDirector();}});
            reject.setOnClickListener(new View.OnClickListener(){public void onClick(View v){boolean ok=db.updateCompanionProposal(proposalId,"rejected",null,null,snapshot);toast(ok?"Proposta recusada sem custos.":"Proposta obsoleta ou já decidida.");showPartyDirector();}});
            alter.setOnClickListener(new View.OnClickListener(){public void onClick(View v){final EditText e=input("Ação revista",proposed.optString("action_text"),true);new AlertDialog.Builder(MainActivity.this).setTitle("Alterar proposta de "+proposed.optString("character_name")).setView(e).setNegativeButton("CANCELAR",null).setPositiveButton("GUARDAR ALTERAÇÃO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){boolean ok=db.updateCompanionProposal(proposalId,"altered",e.getText().toString(),"Alterada pelo jogador após ouvir a proposta.",snapshot);toast(ok?"Proposta alterada; ainda requer aprovação.":"Proposta obsoleta; peça nova proposta.");showPartyDirector();}}).show();}});
            buttons.addView(approve,new LinearLayout.LayoutParams(0,dp(40),1));buttons.addView(alter,new LinearLayout.LayoutParams(0,dp(40),1));buttons.addView(reject,new LinearLayout.LayoutParams(0,dp(40),1));proposals.addView(buttons);
        }
        b.addView(proposals);
        LinearLayout controls=card();controls.addView(text("CONTROLE DE CADA HERÓI",16,GOLD,true));''','proposals_ui')
m=replace(m,'    private void showPartyDirector(){',
'''    private void requestCompanionIdeas(){
        if(aiBusy){toast("O Gemini está trabalhando; tente novamente depois.");return;}
        String instruction=CompanionAgencyEngine.instruction(db,safeActiveCharacters(),true);
        sendMasterSideQuestion("O QUE OS COMPANHEIROS ACHAM? Consulte somente personagens presentes e suas informações reveladas. "+instruction+
          "Dê 1–3 hipóteses coerentes, identifique o que cada um realmente percebeu e por que suspeita disso. Sem avançar relógio/cena, decidir por mim, criar eventos, revelar segredos ou aplicar efeitos. Nada acontece até o jogador decidir.");
    }
    private void showPartyDirector(){''','advice_method')
# Need keep source type and exact schema changes, no mechanical engine replacements.
for filename in ('CompanionAgencyEngine.java',):
    shutil.copy2(Path(__file__).parent/'java'/filename, pkg/filename)
# Guard all anchors before writing (above already in-memory).
dbp.write_text(db,encoding='utf-8');mp.write_text(m,encoding='utf-8')
print(json.dumps({'result':'RUN189_AGENCY_PATCH_APPLIED','db_version':61,'proposal_schema':True,'curated_guard':True,'modes':3,'files':['AppDb.java','MainActivity.java','CompanionAgencyEngine.java']},ensure_ascii=False))
