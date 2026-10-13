"""Persist objective evidence and link companion arcs to their actual published quests."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Journal anchor count='+str(s.count(a))+' '+a[:80])
 return s.replace(a,b,1)
def journal_repository(s):
 # Generic legacy personal status must not supersede the published quest record.
 s=once(s,'if(!KingmakerCompanionSemantics.questAvailable(companionKey,partyLevel,true,companion.optString("personal_quest_status"),vordakai,bloom))', 'if(!KingmakerCompanionSemantics.questAvailable(companionKey,partyLevel,true,officialCompanionArcStatus(companionKey),vordakai,bloom))')
 s=once(s,'String status=key(companion.optString("personal_quest_status"));','String status=key(officialCompanionArcStatus(companionKey));')
 start=s.index('    public void updateCompanion(');end=s.index('    public JSONObject pendingCompanionEvent(',start)
 part=s[start:end]
 old='if(KingmakerCampaignGate.completed(before.optString("personal_quest_status"))&&!"complete".equals(next))throw new IllegalStateException("O arco pessoal já foi concluído.");'
 part=once(part,old,'if(KingmakerCampaignGate.completed(officialCompanionArcStatus(companionKey))&&!"complete".equals(next))throw new IllegalStateException("O arco pessoal já foi concluído.");')
 old='if("complete".equals(next)&&!"in_progress".equals(before.optString("personal_quest_status")))throw new IllegalStateException("Inicie e resolva o arco pessoal antes de concluí-lo.");'
 part=once(part,old,'if("complete".equals(next)&&!KingmakerCampaignGate.completed(officialCompanionArcStatus(companionKey)))throw new IllegalStateException("Conclua os objetivos da aventura publicada no Diário da Campanha.");')
 s=s[:start]+part+s[end:]
 s=once(s,'String nextRole=clean(leadershipRole);','if(KingmakerCampaignGate.completed(clean(questStatus))&&!KingmakerCampaignGate.completed(officialCompanionArcStatus(companionKey)))throw new IllegalStateException("Conclua os objetivos da aventura publicada no Diário da Campanha.");String nextRole=clean(leadershipRole);')
 s=once(s,'v.put("personal_quest_status",clean(questStatus));','v.put("personal_quest_status",KingmakerCampaignGate.completed(officialCompanionArcStatus(companionKey))?"complete":clean(questStatus));')
 start=s.index('    public void ensureDefaultCompanions()');end=s.index('    public JSONObject addCompanion(',start);part=s[start:end];closing=part.rfind('\n    }');part=part[:closing]+'\n        try{migrateCompanionAliases();}catch(Exception error){throw new IllegalStateException("Falha na migração dos companheiros.",error);}'+part[closing:];s=s[:start]+part+s[end:]
 helper='''
    private void migrateCompanionAliases()throws Exception{
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            for(JSONObject alias:db.queryList("SELECT * FROM kingmaker_companions WHERE world_profile=?",new String[]{profile()})){
                String oldKey=alias.optString("companion_key"),canonical=KingmakerRulesEngine.canonicalCompanionKey(oldKey.replaceFirst("^npc_",""));
                if(!oldKey.startsWith("npc_")||canonical.equals(oldKey)||!java.util.Arrays.asList("amiri","ekundayo","harrim","jaethal","jubilost","kalikke_kanerah","linzi","nok_nok","octavia","regongar","tristian","valerie").contains(canonical))continue;
                JSONObject definition=rules.companionProfile(canonical);if(definition.length()==0)continue;
                JSONObject current=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),canonical});
                JSONObject archive=upsertFeature("legacy:companion_alias:"+alias.optLong("id"),"legacy_companion_alias","Histórico: "+alias.optString("name"),1,alias.toString(),"R36 migração de identidades","Estado anterior preservado integralmente.");
                ContentValues inactive=new ContentValues();inactive.put("status","inactive");db.getWritableDatabase().update("kingmaker_features",inactive,"id=? AND world_profile=?",new String[]{String.valueOf(archive.optLong("id")),profile()});
                long canonicalId=current.length()==0?alias.optLong("id"):current.optLong("id");
                if(current.length()==0){ContentValues v=new ContentValues();v.put("companion_key",canonical);v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_companions",v,"id=? AND world_profile=?",new String[]{String.valueOf(alias.optLong("id")),profile()});}
                else{
                    JSONObject merged=KingmakerCompanionSemantics.mergeLegacyAlias(current,alias);ContentValues v=new ContentValues();
                    for(String field:new String[]{"influence","npc_id","available"})v.put(field,merged.optInt(field));
                    for(String field:new String[]{"attitude","assignment","leadership_role","personal_quest_status","personal_quest_notes","unlocked_rewards_json","notes"})v.put(field,merged.optString(field));
                    v.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_companions",v,"id=? AND world_profile=?",new String[]{String.valueOf(canonicalId),profile()});
                    db.getWritableDatabase().execSQL("UPDATE kingmaker_leaders SET entity_id=? WHERE world_profile=? AND entity_kind='companion' AND entity_id=?",new Object[]{canonicalId,profile(),alias.optLong("id")});
                    db.getWritableDatabase().delete("kingmaker_companions","id=? AND world_profile=?",new String[]{String.valueOf(alias.optLong("id")),profile()});
                }
                db.getWritableDatabase().execSQL("UPDATE kingmaker_events SET source_ref=? WHERE world_profile=? AND source_ref=?",new Object[]{"companion:"+canonical,profile(),"companion:"+oldKey});
                audit("Identidade de companheiro reconciliada",oldKey+" → "+canonical,alias,new JSONObject().put("canonical_id",canonicalId).put("archive_id",archive.optLong("id")));
            }
            for(JSONObject c:db.queryList("SELECT * FROM kingmaker_companions WHERE world_profile=?",new String[]{profile()})){
                if(!KingmakerCampaignGate.completed(c.optString("personal_quest_status")))continue;
                JSONObject quest=officialCompanionQuest(c.optString("companion_key"));if(quest.length()==0||KingmakerCampaignGate.completed(quest.optString("status")))continue;
                JSONObject archived=upsertFeature("legacy:companion_arc:"+c.optLong("id"),"legacy_companion_arc","Conclusão antiga: "+c.optString("name"),1,c.toString(),"R36 migração de aventuras publicadas","A conclusão genérica não comprova os objetivos da aventura publicada.");
                ContentValues inactive=new ContentValues();inactive.put("status","inactive");db.getWritableDatabase().update("kingmaker_features",inactive,"id=? AND world_profile=?",new String[]{String.valueOf(archived.optLong("id")),profile()});
                ContentValues corrected=new ContentValues();corrected.put("personal_quest_status",officialCompanionArcStatus(c.optString("companion_key")));corrected.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_companions",corrected,"id=? AND world_profile=?",new String[]{String.valueOf(c.optLong("id")),profile()});
            }
        }});
    }
    private JSONObject officialCompanionQuest(String companionKey){
        String canonical=KingmakerRulesEngine.canonicalCompanionKey(companionKey.replaceFirst("^npc_",""));
        for(JSONObject q:db.queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{profile()}))if(("q_cg_"+canonical).equals(KingmakerCampaignGate.questKey(q)))return q;
        return new JSONObject();
    }
    public String officialCompanionArcStatus(String companionKey){
        JSONObject q=officialCompanionQuest(companionKey);if(q.length()==0)return "not_started";
        if(KingmakerCampaignGate.completed(q.optString("status")))return "complete";
        JSONObject progress=jsonObject(q.optString("progress_json","{}"));return progress.optBoolean("journal_started")?"in_progress":"not_started";
    }
    public List<JSONObject> campaignJournal(){return db.queryList("SELECT * FROM quests WHERE world_profile=? ORDER BY id",new String[]{profile()});}
    private void requireJournalEligibility(JSONObject quest)throws Exception{
        if(!KingmakerCampaignGate.ready(quest,campaignJournal()))throw new IllegalStateException("Conclua os pré-requisitos da missão.");
        String questKey=KingmakerCampaignGate.questKey(quest);
        if(questKey.startsWith("q_cg_")){
            String companionKey=questKey.substring(5);JSONObject companion=companion(companionKey);int level=1;
            for(JSONObject hero:db.listActiveCharacters())level=Math.max(level,hero.optInt("level",1));
            boolean vordakai=false,bloom=false;for(JSONObject q:campaignJournal())if(KingmakerCampaignGate.official(q)&&!KingmakerCampaignGate.retiredQuest(q)&&KingmakerCampaignGate.completed(q.optString("status"))){if("q_vordakai".equals(KingmakerCampaignGate.questKey(q)))vordakai=true;if("q_utero_lamashtu".equals(KingmakerCampaignGate.questKey(q)))bloom=true;}
            if(!KingmakerCompanionSemantics.questAvailable(companionKey,level,companion.optInt("available",0)==1,"in_progress",vordakai,bloom))throw new IllegalStateException("O companheiro ou o momento da história ainda não permite esta aventura.");
            if("ekundayo".equals(companionKey)&&settlements().isEmpty())throw new IllegalStateException("O banquete ocorre após fundar o Reino.");
            if("linzi".equals(companionKey)&&settlements().isEmpty())throw new IllegalStateException("A investigação começa em um assentamento do Reino.");
        }
    }
    public JSONObject recordCampaignObjective(final long questId,final String objectiveId,final boolean waived,final String evidence)throws Exception{
        requireEnabled();if(clean(evidence).length()<5)throw new IllegalArgumentException("Registre o resultado da cena ou a decisão que resolveu o objetivo.");
        final JSONObject[] result={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject quest=db.queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(questId),profile()});
            if(quest.length()==0||!KingmakerCampaignGate.official(quest)||KingmakerCampaignGate.retiredQuest(quest))throw new IllegalArgumentException("Missão publicada inexistente.");
            if(KingmakerCampaignGate.completed(quest.optString("status")))throw new IllegalStateException("A missão já foi concluída.");
            requireJournalEligibility(quest);
            JSONObject progress=KingmakerCampaignGate.resolveObjective(quest,objectiveId,waived,clean(evidence));progress.put("journal_started",true);
            ContentValues v=new ContentValues();v.put("progress_json",progress.toString());v.put("status","in_progress");v.put("updated_at",System.currentTimeMillis());
            if(db.getWritableDatabase().update("quests",v,"id=? AND world_profile=?",new String[]{String.valueOf(questId),profile()})!=1)throw new IllegalStateException("O objetivo não foi persistido.");
            String k=KingmakerCampaignGate.questKey(quest);if(k.startsWith("q_cg_")){ContentValues c=new ContentValues();c.put("personal_quest_status","in_progress");c.put("updated_at",System.currentTimeMillis());db.getWritableDatabase().update("kingmaker_companions",c,"world_profile=? AND companion_key=?",new String[]{profile(),k.substring(5)});}
            result[0]=db.queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(questId),profile()});audit("Objetivo da campanha resolvido",objectiveId,quest,result[0]);
        }});return result[0];
    }
    public JSONObject finishCampaignQuest(final long questId)throws Exception{
        requireEnabled();final JSONObject[] result={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject before=db.queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(questId),profile()});
            if(before.length()==0||!KingmakerCampaignGate.official(before))throw new IllegalArgumentException("Missão da campanha inexistente.");
            if(KingmakerCampaignGate.completed(before.optString("status"))){result[0]=before;return;}
            requireJournalEligibility(before);if(!KingmakerCampaignGate.objectivesResolved(before))throw new IllegalStateException("Resolva todos os objetivos ou registre a ramificação que os dispensa.");
            String k=KingmakerCampaignGate.questKey(before);
            if(k.startsWith("q_cg_"))throw new IllegalStateException("Aventura de companheiro exige escolha de desfecho publicado e aplicação de consequências. Use ESCOLHER DESFECHO.");
            db.updateQuestStatus(questId,"completed","Objetivos resolvidos no Diário da Campanha");
            result[0]=db.queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(questId),profile()});audit("Missão da campanha concluída",k,before,result[0]);
        }});return result[0];
    }
    public JSONObject resolveCompanionAdventureCheck(final String companionKey,final String checkId,final int naturalRoll,final int totalModifier,final int dc)throws Exception{
        requireEnabled();final String canonical=KingmakerRulesEngine.canonicalCompanionKey(clean(companionKey).replaceFirst("^npc_",""));
        final JSONObject[] result={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject quest=officialCompanionQuest(canonical);if(quest.length()==0)throw new IllegalArgumentException("Aventura oficial de companheiro não encontrada: "+canonical);
            if(KingmakerCampaignGate.completed(quest.optString("status")))throw new IllegalStateException("A aventura deste companheiro já foi concluída.");
            requireJournalEligibility(quest);
            JSONObject progress=KingmakerCampaignGate.progress(quest);
            if("ekundayo".equals(canonical)){
                JSONObject c=companion(canonical);
                progress.put("ekundayo_helpful","helpful".equals(c.optString("attitude"))||c.optInt("influence",0)>=8);
            }
            JSONObject outcome=KingmakerCompanionSemantics.resolveCompanionAdventureCheck(canonical,checkId,naturalRoll,totalModifier,dc,progress);
            JSONObject nextProgress=outcome.getJSONObject("progress");
            ContentValues v=new ContentValues();v.put("progress_json",nextProgress.toString());v.put("status","in_progress");v.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("quests",v,"id=? AND world_profile=?",new String[]{String.valueOf(quest.optLong("id")),profile()});
            ContentValues c=new ContentValues();c.put("personal_quest_status","in_progress");c.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_companions",c,"world_profile=? AND companion_key=?",new String[]{profile(),canonical});
            result[0]=outcome;audit("Teste de aventura de companheiro",canonical+" • "+checkId,quest,outcome);
        }});return result[0];
    }
    public JSONObject resolveCompanionAdventureBranch(final String companionKey,final String branchKey)throws Exception{
        requireEnabled();final String canonical=KingmakerRulesEngine.canonicalCompanionKey(clean(companionKey).replaceFirst("^npc_",""));
        final JSONObject[] result={new JSONObject()};db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            String requestedBranch=clean(branchKey);
            if(requestedBranch.length()==0)throw new IllegalArgumentException("Escolha um desfecho oficial da aventura.");
            String receiptKey="companion_branch_receipt:q_cg_"+canonical;
            JSONObject existingReceipt=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),receiptKey});
            if(existingReceipt.length()>0){
                JSONObject previous=jsonObject(existingReceipt.optString("effects_json","{}"));
                if(!requestedBranch.equals(previous.optString("branch_key","")))
                    throw new IllegalStateException("Esta aventura já foi concluída com outro desfecho; a escolha anterior não pode mudar.");
                result[0]=previous.put("idempotent_replay",true);return;
            }
            JSONObject quest=officialCompanionQuest(canonical);if(quest.length()==0)throw new IllegalArgumentException("Aventura oficial de companheiro não encontrada: "+canonical);
            if(KingmakerCampaignGate.completed(quest.optString("status")))
                throw new IllegalStateException("Missão concluída sem recibo de desfecho; reparar os dados antes de aplicar recompensas.");
            requireJournalEligibility(quest);
            if(!KingmakerCampaignGate.objectivesResolved(quest))throw new IllegalStateException("Resolva todos os objetivos da aventura antes de consolidar o desfecho.");
            JSONObject comp=companion(canonical);if(comp.length()==0)throw new IllegalArgumentException("Companheiro inexistente: "+canonical);
            JSONObject progress=KingmakerCampaignGate.progress(quest);
            JSONObject outcome=KingmakerCompanionSemantics.resolveCompanionAdventureBranch(canonical,requestedBranch,comp,progress);
            ContentValues cv=new ContentValues();
            cv.put("influence",outcome.optInt("influence_after"));
            cv.put("attitude",outcome.optString("attitude_after"));
            cv.put("available",outcome.optInt("available_after",1));
            cv.put("unlocked_rewards_json",outcome.optJSONArray("unlocked_rewards_json").toString());
            cv.put("personal_quest_status","complete");
            cv.put("personal_quest_notes",outcome.optString("summary"));
            cv.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("kingmaker_companions",cv,"world_profile=? AND companion_key=?",new String[]{profile(),canonical});
            if(!KingmakerCampaignGate.completed(quest.optString("status"))){
                db.updateQuestStatus(quest.optLong("id"),"completed",outcome.optString("summary"));
            }
            ContentValues qv=new ContentValues();qv.put("progress_json",outcome.getJSONObject("progress").toString());qv.put("updated_at",System.currentTimeMillis());
            db.getWritableDatabase().update("quests",qv,"id=? AND world_profile=?",new String[]{String.valueOf(quest.optLong("id")),profile()});
            int xpAward=outcome.optInt("xp_awarded",0);
            if(xpAward>0){
                for(JSONObject hero:db.listActiveCharacters()){
                    JSONObject sheet=jsonObject(hero.optString("sheet_json","{}"));
                    sheet.put("experience_points",sheet.optInt("experience_points",0)+xpAward);
                    db.updateCharacterSheetChecked(hero.optLong("id"),sheet.toString());
                }
            }
            JSONArray awardedItems=outcome.optJSONArray("items_awarded");
            if(awardedItems!=null&&awardedItems.length()>0){
                JSONObject party=db.getPartyState();
                JSONObject resources=jsonObject(party.optString("resources_json","{}"));
                JSONArray inv=resources.optJSONArray("shared_inventory");if(inv==null)inv=new JSONArray();
                for(int i=0;i<awardedItems.length();i++)inv.put(new JSONObject().put("name",awardedItems.optString(i)).put("quantity",1).put("source","q_cg_"+canonical));
                resources.put("shared_inventory",inv);party.put("resources_json",resources.toString());
                db.savePartyStateChecked(party);
            }
            upsertFeature(receiptKey,"companion_adventure_branch","Desfecho: "+quest.optString("title"),KingmakerCompanionSemantics.questLevel(canonical),outcome.toString(),"Kingmaker Companion Guide",outcome.optString("summary"));
            result[0]=outcome;audit("Desfecho de aventura de companheiro",canonical+" • "+branchKey,comp,outcome);
        }});return result[0];
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def journal_ui(s):
 anchor='km.addView(kingdomButton);'
 s=once(s,anchor,anchor+'''if(kingmakerEnabled){Button journal=button("DIÁRIO DA CAMPANHA E COMPANHEIROS",PANEL2);journal.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showKingmakerCampaignJournal();}});km.addView(journal);}''')
 s=once(s,'p.append("MUNDO VIVO ESTRUTURADO:', 'if(kingmakerService!=null&&kingmakerService.enabled())p.append("KINGMAKER: melhorar estrutura existente usa build_structure com upgrade_from (estrutura original) e structure_key (destino); o motor calcula o custo efetivo e mantém a antiga ativa até concluir. Para missões publicadas, quest_updates deve preservar os IDs dos objetivos. Registre cenas efetivamente resolvidas em objective_resolutions:[{id,waived,resolution_notes}], usando waived somente quando uma escolha do grupo dispensar aquela ramificação. Não conclua missões com objetivos pendentes, não invente objetivos ou recompensas e respeite o nível e o momento das aventuras dos companheiros.\\n");p.append("MUNDO VIVO ESTRUTURADO:')
 # Generic completion uses the same backend, preventing a bypass around objective validation.
 s=once(s,'db.updateQuestStatus(q.optLong("id"),"completed","Concluída pelo jogador");','if(kingmakerService!=null&&kingmakerService.enabled()&&KingmakerCampaignGate.official(q))kingmakerRepository.finishCampaignQuest(q.optLong("id"));else db.updateQuestStatus(q.optLong("id"),"completed","Concluída pelo jogador");')
 helper='''
    private void showKingmakerCampaignJournal(){
        final List<JSONObject> rows=new ArrayList<JSONObject>();final List<String> labels=new ArrayList<String>();
        for(JSONObject q:kingmakerRepository.campaignJournal())if(KingmakerCampaignGate.official(q)&&!KingmakerCampaignGate.retiredQuest(q)){rows.add(q);String k=KingmakerCampaignGate.questKey(q);labels.add((k.startsWith("q_cg_")?"Companheiro":KingmakerCampaignGate.officialTitle(KingmakerCampaignGate.officialChapterForQuest(q)))+" • "+q.optString("title")+" • "+q.optString("status"));}
        new AlertDialog.Builder(this).setTitle("Diário da Campanha").setItems(labels.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){showKingmakerQuestObjectives(rows.get(which));}}).setNegativeButton("Voltar",null).show();
    }
    private void showKingmakerQuestObjectives(final JSONObject q){
        final JSONArray objectives=KingmakerCampaignGate.objectives(q);final List<String> labels=new ArrayList<String>();
        for(int n=0;n<objectives.length();n++){JSONObject o=objectives.optJSONObject(n);labels.add((o.optBoolean("completed")||"waived".equals(o.optString("status"))?"✓ ":"○ ")+o.optString("description",o.optString("desc")));}
        final boolean isCompanion=KingmakerCampaignGate.questKey(q).startsWith("q_cg_");
        new AlertDialog.Builder(this).setTitle(q.optString("title")).setItems(labels.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){showKingmakerObjectiveOutcome(q,objectives.optJSONObject(which));}}).setNegativeButton("Voltar",null).setNeutralButton(isCompanion?"ROLAGENS DA AVENTURA":null,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){if(isCompanion)showKingmakerCompanionSkillChecks(q);}}).setPositiveButton(isCompanion?"ESCOLHER DESFECHO":"CONCLUIR MISSÃO",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{if(isCompanion)showKingmakerCompanionBranchChoices(q);else{kingmakerRepository.finishCampaignQuest(q.optLong("id"));showKingmakerCampaignJournal();}}catch(Exception e){toast(e.getMessage());}}}).show();
    }
    private void showKingmakerCompanionSkillChecks(final JSONObject q){
        final JSONArray scenes=KingmakerCampaignGate.progress(q).optJSONArray("scenes");
        if(scenes==null){toast("Esta aventura não possui testes cadastrados.");return;}
        final List<JSONObject> checks=new ArrayList<JSONObject>();
        final List<String> labels=new ArrayList<String>();
        for(int i=0;i<scenes.length();i++){
            JSONObject scene=scenes.optJSONObject(i);if(scene==null)continue;
            JSONArray rolls=scene.optJSONArray("skill_checks");if(rolls==null)continue;
            for(int j=0;j<rolls.length();j++){
                JSONObject check=rolls.optJSONObject(j);if(check==null)continue;
                checks.add(check);
                labels.add(scene.optString("title")+" • "+check.optString("skill")+" CD "+check.optInt("dc")+" • "+check.optString("description"));
            }
        }
        if(checks.isEmpty()){toast("Nenhum teste publicado nesta aventura.");return;}
        new AlertDialog.Builder(this).setTitle("Testes oficiais • "+q.optString("title"))
         .setItems(labels.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
            showKingmakerCompanionSkillRoll(q,checks.get(which));
         }}).setNegativeButton("Voltar",null).show();
    }
    private void showKingmakerCompanionSkillRoll(final JSONObject q,final JSONObject check){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text(check.optString("description"),14,TEXT,false));
        final EditText natural=labeledField(form,"Resultado natural do d20 (1 a 20)","",true);
        final EditText bonus=labeledField(form,"Modificador total (sem o d20)","0",true);
        final List<Integer> availableDcs=new ArrayList<Integer>();availableDcs.add(check.optInt("dc"));
        final List<String> dcLabels=new ArrayList<String>();dcLabels.add("CD publicada "+check.optInt("dc"));
        if(check.has("alternative_dc_when_helpful")){
            availableDcs.add(check.optInt("alternative_dc_when_helpful"));
            dcLabels.add("CD se Ekundayo prestativo "+check.optInt("alternative_dc_when_helpful"));
        }
        if(check.has("alternative_dc_for_crafting")){
            availableDcs.add(check.optInt("alternative_dc_for_crafting"));
            dcLabels.add("CD alternativa de Manufatura "+check.optInt("alternative_dc_for_crafting"));
        }
        final JSONArray alternateSkillDcs=check.optJSONArray("alternate_skill_dcs");
        if(alternateSkillDcs!=null)for(int a=0;a<alternateSkillDcs.length();a++){
            JSONObject alternative=alternateSkillDcs.optJSONObject(a);
            if(alternative==null)continue;
            availableDcs.add(alternative.optInt("dc"));
            dcLabels.add(alternative.optString("skill")+" • CD "+alternative.optInt("dc"));
        }
        final Spinner chosenDc=spinner(dcLabels.toArray(new String[0]),dcLabels.get(0));
        form.addView(cardWithLabel("CD DO TESTE",chosenDc));
        final String companion=KingmakerCampaignGate.questKey(q).substring(5);
        new AlertDialog.Builder(this).setTitle(check.optString("skill")+" • CD "+check.optInt("dc"))
         .setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null)
         .setPositiveButton("APLICAR TESTE",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){
            try{
                int d20=Integer.parseInt(value(natural).trim());
                int mod=Integer.parseInt(value(bonus).trim());
                JSONObject result=kingmakerRepository.resolveCompanionAdventureCheck(companion,check.optString("check_id"),d20,mod,availableDcs.get(chosenDc.getSelectedItemPosition()));
                JSONObject roll=result.optJSONObject("check");
                toast("Teste aplicado e salvo: "+(roll==null?"":roll.optString("degree")));
                showKingmakerCampaignJournal();
            }catch(Exception e){toast(e.getMessage());}
         }}).show();
    }
    private void showKingmakerCompanionBranchChoices(final JSONObject q){
        final String key=KingmakerCampaignGate.questKey(q);if(!key.startsWith("q_cg_")){toast("Esta não é uma aventura pessoal.");return;}
        final String companion=key.substring(5);
        final JSONArray branches=KingmakerCampaignGate.progress(q).optJSONArray("branches");
        if(branches==null||branches.length()==0){toast("O catálogo desta aventura não contém desfechos oficiais.");return;}
        final List<String> labels=new ArrayList<String>();
        for(int i=0;i<branches.length();i++){JSONObject b=branches.optJSONObject(i);labels.add(b==null?"Desfecho inválido":b.optString("title",b.optString("branch_key")));}
        new AlertDialog.Builder(this).setTitle("Desfecho de "+q.optString("title"))
         .setItems(labels.toArray(new String[0]),new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
            final JSONObject selected=branches.optJSONObject(which);if(selected==null){toast("Desfecho inválido.");return;}
            new AlertDialog.Builder(MainActivity.this).setTitle(selected.optString("title"))
             .setMessage(selected.optString("description")+"\\n\\nAs consequências serão persistidas e não poderão ser concedidas duas vezes.")
             .setNegativeButton("Cancelar",null)
             .setPositiveButton("CONFIRMAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
                try{kingmakerRepository.resolveCompanionAdventureBranch(companion,selected.optString("branch_key"));showKingmakerCampaignJournal();}
                catch(Exception e){toast(e.getMessage());}
             }}).show();
         }}).setNegativeButton("Voltar",null).show();
    }
    private void showKingmakerObjectiveOutcome(final JSONObject q,final JSONObject objective){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));form.addView(text(objective.optString("description",objective.optString("desc")),14,TEXT,false));
        final Spinner result=spinner(new String[]{"Objetivo cumprido","Dispensado por decisão / ramificação"},"Objetivo cumprido");form.addView(cardWithLabel("RESULTADO",result));final EditText evidence=labeledField(form,"O que ocorreu na cena / decisão tomada",objective.optString("resolution_notes"),true);
        new AlertDialog.Builder(this).setTitle("Resolver objetivo").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("REGISTRAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{JSONObject fresh=kingmakerRepository.recordCampaignObjective(q.optLong("id"),objective.optString("id"),result.getSelectedItemPosition()==1,value(evidence));showKingmakerQuestObjectives(fresh);}catch(Exception e){toast(e.getMessage());}}}).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]

def journal_appdb(s):
 anchor='KingmakerCampaignGate.requireTransition(previous,q.optString("status",previous.optString("status")),queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}));'
 s=once(s,anchor,'requirePublishedCompanionQuest(previous);KingmakerCampaignGate.requireTransition(KingmakerCampaignGate.transitionState(previous,q),q.optString("status",previous.optString("status")),queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}));')
 s=once(s,'String title=q.has("title")?q.optString("title").trim():previous.optString("title");','if(previous.length()==0&&KingmakerCampaignGate.official(q))throw new IllegalStateException("Missões publicadas são carregadas pelo importador da campanha; a narradora deve atualizar os registros existentes.");String title=q.has("title")?q.optString("title").trim():previous.optString("title");')
 helper='''
    private void requirePublishedCompanionQuest(JSONObject quest){
        String key=KingmakerCampaignGate.questKey(quest);if(!key.startsWith("q_cg_")||KingmakerCampaignGate.completed(quest.optString("status")))return;
        String companionKey=key.substring(5);JSONObject companion=queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{getActiveMapProfile(),companionKey});int level=1;
        for(JSONObject hero:listActiveCharacters())level=Math.max(level,hero.optInt("level",1));
        boolean vordakai=false,bloom=false;for(JSONObject q:queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}))if(KingmakerCampaignGate.official(q)&&!KingmakerCampaignGate.retiredQuest(q)&&KingmakerCampaignGate.completed(q.optString("status"))){if("q_vordakai".equals(KingmakerCampaignGate.questKey(q)))vordakai=true;if("q_utero_lamashtu".equals(KingmakerCampaignGate.questKey(q)))bloom=true;}
        if(!KingmakerCompanionSemantics.questAvailable(companionKey,level,companion.optInt("available",0)==1,"in_progress",vordakai,bloom))throw new IllegalStateException("A aventura do companheiro ainda não está disponível neste momento da campanha.");
        if(("ekundayo".equals(companionKey)||"linzi".equals(companionKey))&&queryOne("SELECT id FROM kingmaker_settlements WHERE world_profile=? LIMIT 1",new String[]{getActiveMapProfile()}).length()==0)throw new IllegalStateException("Esta aventura requer um assentamento do Reino.");
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]

def journal_director(s):
 for name in ['activeQuestsList','unstartedQuests']:
  anchor=name+'=KingmakerCampaignGate.playable('+name+',allCampaignQuests);'
  s=once(s,anchor,anchor+'\n            '+name+'=filterPublishedCompanionQuests('+name+',allCampaignQuests,profile);')
 helper='''
    private List<JSONObject> filterPublishedCompanionQuests(List<JSONObject> candidates,List<JSONObject> all,String profile){
        List<JSONObject> result=new java.util.ArrayList<JSONObject>();int level=1;for(JSONObject hero:db.listActiveCharacters())level=Math.max(level,hero.optInt("level",1));
        boolean vordakai=false,bloom=false;for(JSONObject q:all)if(KingmakerCampaignGate.official(q)&&!KingmakerCampaignGate.retiredQuest(q)&&KingmakerCampaignGate.completed(q.optString("status"))){if("q_vordakai".equals(KingmakerCampaignGate.questKey(q)))vordakai=true;if("q_utero_lamashtu".equals(KingmakerCampaignGate.questKey(q)))bloom=true;}
        for(JSONObject quest:candidates){String key=KingmakerCampaignGate.questKey(quest);if(!key.startsWith("q_cg_")){result.add(quest);continue;}
            String companionKey=key.substring(5);JSONObject companion=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile,companionKey});
            if(!KingmakerCompanionSemantics.questAvailable(companionKey,level,companion.optInt("available",0)==1,quest.optString("status"),vordakai,bloom))continue;
            if(("linzi".equals(companionKey)||"ekundayo".equals(companionKey))&&db.queryOne("SELECT id FROM kingmaker_settlements WHERE world_profile=? LIMIT 1",new String[]{profile}).length()==0)continue;
            result.add(quest);
        }return result;
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
