#!/usr/bin/env python3
"""Integrate activity-scoped structures, prerequisite-aware campaign and book companions."""
from pathlib import Path
import shutil,sys
from patch_treasury_semantics import repository_treasury,rules_treasury,treasury_ui
from patch_structure_focus import repository_focus,focus_ui
from patch_grid_infrastructure import repository_grid,grid_ui
from patch_twins_persona import twins_repository,twins_ui
from patch_border_infrastructure import border_repository,border_ui
from patch_lot_placement import lot_repository,lot_ui
from patch_occupied_blocks import occupancy_repository
SRC=Path("android/app/src/main/java/com/braseiro/pathfinder2e")
HERE=Path(__file__).parent
MARKER="R36_SEMANTIC_DOMAIN_INTEGRATION"
def replace_once(s,old,new,label):
 if s.count(old)!=1:raise ValueError(label+" anchor count="+str(s.count(old)))
 return s.replace(old,new,1)
def repository(s):
 if MARKER in s:raise ValueError("Semantic integration already applied")
 # Capital bonuses have kingdom-wide reach. Other settlements need explicit context.
 header='    public JSONObject recordCheck(String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)throws Exception{'
 new=header+"""
        return recordCheckAtSettlement("",activityKey,title,role,skillKey,natural,dc,sourceRef,notes,awardReputationOnCrit);
    }
    private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)throws Exception{
"""
 s=replace_once(s,header,new,"recordCheck")
 s=replace_once(s,'int civicItemBonus="repair_structure".equals(activity)?Math.max(skill.optInt("item_bonus"),2):skill.optInt("item_bonus");',
 '''int structureItem=officialStructureActivityBonus(settlementKey,activity,skillKey,new JSONObject());
        int civicItemBonus=Math.max(skill.optInt("item_bonus"),structureItem);
        if("repair_structure".equals(activity))civicItemBonus=Math.max(civicItemBonus,2);''',"scoped item bonus")
 s=s.replace('JSONObject check=recordCheck("build_structure",','JSONObject check=recordCheckAtSettlement(townKey,"build_structure",',1)
 s=s.replace('JSONObject check=recordCheck("repair_structure",','JSONObject check=recordCheckAtSettlement(townKey,"repair_structure",',1)
 # Stop writing fabricated broad skill bonuses on build/demolition.
 begin=s.index("    public void applyStructureSkillBonuses() {")
 end=s.index("\n    public JSONObject buildStructure(",begin)
 s=s[:begin]+'''    public void applyStructureSkillBonuses() {
        // Structure bonuses are calculated for each activity and location.
        // Preserve explicit manual item bonuses rather than overwriting the skill sheet.
    }
'''+s[end:]
 s=s.replace('int maxStorage = rules.commodityStorage(s.optInt("size", 1));','int maxStorage = officialCommodityCapacity(comm);',1)
 # Companion construction benefit is conditional, not a permanent lower catalog DC.
 s=s.replace('naturalRoll,def.optInt("construction_dc",0),"Kingmaker Adventure Path 2e p. 518",notes,false);',
  'naturalRoll,officialConstructionDc(def),"Kingmaker Adventure Path 2e p. 518",notes,false);',1)
 # No secondary pseudo-quest. Trigger primary arcs only at their published levels/events.
 anchor='String status=key(companion.optString("personal_quest_status"));'
 s=replace_once(s,anchor,'''int partyLevel=1;for(JSONObject hero:db.listActiveCharacters())partyLevel=Math.max(partyLevel,hero.optInt("level",1));
        List<JSONObject> questRows=db.queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{profile()});
        boolean vordakai=false,bloom=false;
        for(JSONObject q:questRows)if(KingmakerCampaignGate.completed(q.optString("status"))){String qk=KingmakerCampaignGate.questKey(q);if("q_vordakai".equals(qk))vordakai=true;if("q_utero_lamashtu".equals(qk))bloom=true;}
        if(!KingmakerCompanionSemantics.questAvailable(companionKey,partyLevel,true,companion.optString("personal_quest_status"),vordakai,bloom))return new JSONObject();
        '''+anchor,"companion personal hook")
 # Completed companion arcs cannot regress or finish directly from not_started.
 needle='ContentValues v=new ContentValues();v.put("personal_quest_status",next);'
 s=replace_once(s,needle,'''if(KingmakerCampaignGate.completed(before.optString("personal_quest_status"))&&!"complete".equals(next))throw new IllegalStateException("O arco pessoal já foi concluído.");
        if("complete".equals(next)&&!"in_progress".equals(before.optString("personal_quest_status")))throw new IllegalStateException("Inicie e resolva o arco pessoal antes de concluí-lo.");
        '''+needle,"companion progress")
 helper='''    // R36_SEMANTIC_DOMAIN_INTEGRATION
    private int officialStructureActivityBonus(String settlementKey,String activity,String skill,JSONObject context)throws Exception{
        JSONObject town=settlement(settlementKey);
        if(town.length()==0){String capital=state().optString("capital_name");for(JSONObject candidate:settlements())if(capital.length()>0&&capital.equals(candidate.optString("name"))){town=candidate;break;}}
        if(town.length()==0)return 0;
        return KingmakerStructureSemantics.activityBonus(new JSONArray(structures(town.optString("settlement_key"))),activity,skill,town.optInt("max_item_bonus",1),context);
    }
    public int officialCommodityCapacity(String commodity){
        return rules.commodityStorage(state().optInt("size",1))+KingmakerStructureSemantics.storageIncrease(new JSONArray(structures("")),commodity);
    }
    private int officialConstructionDc(JSONObject definition){
        int dc=definition.optInt("construction_dc",0);
        for(JSONObject c:companions())if(KingmakerCompanionSemantics.constructionDcAdjustment(c,definition.optInt("lumber"))<0)return Math.max(1,dc-2);
        return dc;
    }
    public JSONObject officialSettlementEffects(String settlementKey)throws Exception{
        JSONObject town=settlement(settlementKey);if(town.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        JSONArray rows=new JSONArray(structures(settlementKey));JSONObject out=new JSONObject();
        for(String category:new String[]{"mundane","alchemical","arcane","divine","primal","occult","luxury_magic"})out.put(category+"_item_level",KingmakerStructureSemantics.itemAvailabilityLevel(town.optInt("level",1),rows,category));
        out.put("dangerous_event_unrest_reduction",KingmakerStructureSemantics.dangerousEventUnrestReduction(rows));
        out.put("consumption_reduction",KingmakerStructureSemantics.consumptionReduction(rows,jsonObject(town.optString("grid_json","{}")).optBoolean("water_mill")));
        return out;
    }
'''
 s=s[:s.rfind("\n}")]+ "\n"+helper+s[s.rfind("\n}"):]
 return s

def structure_triggers(s):
 # ABI-compatible overload: UI provides its Ruin choice and recorded d4 results.
 signature="public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice)throws Exception{"
 repl=signature+'''
        return resolveBuildStructure(settlementKey,structureKey,naturalRoll,skillKey,notes,commodityChoice,new JSONObject());
    }
    public JSONObject resolveBuildStructure(String settlementKey,String structureKey,int naturalRoll,String skillKey,String notes,String commodityChoice,final JSONObject buildOptions)throws Exception{'''
 s=replace_once(s,signature,repl,"build options")
 s=replace_once(s,'JSONObject completed=buildStructure(townKey,structureKey,notes,true);','JSONObject completed=buildStructure(townKey,structureKey,notes,true,buildOptions);',"build triggers options")
 signature='private JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes, final boolean prepaid) throws Exception {'
 repl=signature+'''
        return buildStructure(settlementKey,structureKey,notes,prepaid,new JSONObject());
    }
    private JSONObject buildStructure(final String settlementKey, final String structureKey, final String notes, final boolean prepaid, final JSONObject buildOptions) throws Exception {'''
 s=replace_once(s,signature,repl,"build trigger overload")
 begin=s.index('                int unrestRed = def.optInt("unrest_reduction", 0);')
 end=s.index('                sv.put("updated_at", System.currentTimeMillis());',begin)
 s=s[:begin]+'''                JSONObject buildEffects=officialBuildEffects(settlementKey,structureKey,def,buildOptions);
                int reputationCap=s.optInt("level",1)>=20?4:3;
                for(String stat:new String[]{"fame","infamy"})if(buildEffects.has(stat))buildEffects.put(stat,Math.max(0,Math.min(reputationCap,s.optInt(stat)+buildEffects.optInt(stat)))-s.optInt(stat));
'''+s[end:]
 anchor='strV.put("effects_json", effects.toString());'
 s=replace_once(s,anchor,'effects.put("official_build_effects",buildEffects);'+anchor,"persist build effects")
 anchor='db.getWritableDatabase().update("kingmaker_state", sv, "world_profile=?", new String[]{profile()});'
 build_start=s.index("    private JSONObject buildStructure(")
 build_end=s.index("    public JSONObject demolishStructure(",build_start)
 build=s[build_start:build_end]
 build=replace_once(build,anchor,anchor+'\n                adjustState(buildEffects,"Efeitos oficiais da construção");\n                ContentValues anarchyState=new ContentValues();anarchyState.put("anarchy",rules.isAnarchy(state().optInt("unrest"))?1:0);db.getWritableDatabase().update("kingmaker_state",anarchyState,"world_profile=?",new String[]{profile()});',"ruin thresholds use core accounting")
 s=s[:build_start]+build+s[build_end:]
 # One-time migration backs up old generated broad bonuses instead of losing them.
 migration='''    private void migrateLegacyStructureBonuses()throws Exception{
        String marker="migration:r36_structure_scope";
        if(db.queryOne("SELECT id FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),marker}).length()>0)return;
        final JSONArray previous=new JSONArray();
        final Map<String,Integer> legacy=new HashMap<String,Integer>();
        for(JSONObject town:settlements())for(JSONObject row:structures(town.optString("settlement_key"))){
            JSONObject def=rules.structure(row.optString("structure_key"));if(def==null)continue;JSONObject bonuses=def.optJSONObject("skill_bonuses");if(bonuses==null)continue;
            Iterator<String> keys=bonuses.keys();while(keys.hasNext()){String k=keys.next();int value=Math.min(bonuses.optInt(k),town.optInt("max_item_bonus",1));if(!legacy.containsKey(k)||value>legacy.get(k))legacy.put(k,value);}
        }
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            for(JSONObject sk:skills()){String k=sk.optString("skill_key");if(sk.optString("notes").length()==0&&legacy.containsKey(k)&&sk.optInt("item_bonus")==legacy.get(k)&&sk.optInt("item_bonus")>0){
                previous.put(sk);ContentValues v=new ContentValues();v.put("item_bonus",0);v.put("updated_at",System.currentTimeMillis());
                db.getWritableDatabase().update("kingmaker_skills",v,"world_profile=? AND skill_key=?",new String[]{profile(),k});
            }}
            upsertFeature(marker,"migration","Bônus por atividade e assentamento",state().optInt("level",1),new JSONObject().put("previous_skills",previous).toString(),"Kingmaker pp.543-553","Os valores anteriores permanecem neste registro para auditoria.");
        }});
    }
    private JSONObject officialBuildEffects(String townKey,String structureKey,JSONObject def,JSONObject options)throws Exception{
        JSONObject before=state();String k=key(structureKey);int count=KingmakerStructureSemantics.count(new JSONArray(structures(townKey)),k);
        String firstTown="structure_history:town:"+townKey+":"+k,firstKingdom="structure_history:kingdom:"+k,turnKey="structure_history:turn:"+before.optInt("turn_number")+":"+k;
        boolean townFirst=count==0&&db.queryOne("SELECT id FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),firstTown}).length()==0;
        boolean kingdomFirst=KingmakerStructureSemantics.count(new JSONArray(structures("")),k)==0&&db.queryOne("SELECT id FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),firstKingdom}).length()==0;
        boolean turnFirst=db.queryOne("SELECT id FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),turnKey}).length()==0;
        int die1=options.optInt("die1",0),die2=options.optInt("die2",0);
        String traits=def.optString("official_traits");
        JSONObject result=KingmakerStructureSemantics.buildEffects(k,count,townFirst,turnFirst,kingdomFirst,(","+traits+",").contains(",FAMOUS,"),(","+traits+",").contains(",INFAMOUS,"),before.optString("reputation_mode","fame"),options.optString("ruin"),die1,die2);
        for(String marker:new String[]{firstTown,firstKingdom,turnKey})upsertFeature(marker,"structure_history","Construção: "+def.optString("name"),before.optInt("level",1),result.toString(),"Kingmaker pp.543-553","");
        return result;
    }
'''
 index=s.rfind("\n}");s=s[:index]+"\n"+migration+s[index:]
 s=s.replace('        // Structure bonuses are calculated for each activity and location.\n        // Preserve explicit manual item bonuses rather than overwriting the skill sheet.',
 '''        try{migrateLegacyStructureBonuses();}catch(Exception error){throw new IllegalStateException("Falha na migração dos bônus de estruturas.",error);}''')
 s=s.replace('private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)throws Exception{\n',
 '''private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)throws Exception{
        applyStructureSkillBonuses();
''',1)
 # Passive effects reduce Consumption without rewriting the base town Consumption.
 anchor='int settlements=db.queryOne("SELECT COALESCE(SUM(consumption),0) AS total FROM kingmaker_settlements WHERE world_profile=?",new String[]{profile()}).optInt("total",0)'
 s=replace_once(s,anchor,'int settlements=officialSettlementConsumption()',"passive consumption")
 helper='''    private int officialSettlementConsumption(){
        int total=0;for(JSONObject town:settlements()){
            JSONArray rows=new JSONArray(structures(town.optString("settlement_key")));
            boolean waterMill=jsonObject(town.optString("grid_json","{}")).optBoolean("water_mill");
            total+=Math.max(0,town.optInt("consumption")-KingmakerStructureSemantics.consumptionReduction(rows,waterMill));
        }return total;
    }
'''
 index=s.rfind("\n}");s=s[:index]+"\n"+helper+s[index:]
 return s

def structure_ui(s):
 anchor='        form.addView(cardWithLabel("CUSTO ALTERNATIVO (Bridge / Watchtower)",commodity));'
 replacement=anchor+'''
        final String[] ruinKeys={"corruption","crime","decay","strife"};
        final Spinner ruinChoice=spinner(new String[]{"Corrupção","Crime","Decadência","Conflito"},"Corrupção");
        form.addView(cardWithLabel("RUÍNA AFETADA (MONUMENTO / CORTIÇOS)",ruinChoice));'''
 s=replace_once(s,anchor,replacement,"ruin selector")
 anchor='n,skill,value(notes),commodityChoice);'
 repl='''n,skill,value(notes),commodityChoice,new JSONObject().put("ruin",ruinKeys[ruinChoice.getSelectedItemPosition()]).put("die1",new Random().nextInt(4)+1).put("die2",new Random().nextInt(4)+1));'''
 s=replace_once(s,anchor,repl,"recorded build dice")
 return s

def director(s):
 active='List<JSONObject> activeQuestsList = db != null ? db.queryList('
 if s.count(active)!=1:raise ValueError("director quest query drift")
 start=s.index(active);marker='            JSONArray activeQuests = new JSONArray();';at=s.index(marker,start)
 s=s[:at]+'''            List<JSONObject> allCampaignQuests=db.queryList("SELECT * FROM quests WHERE world_profile=? ORDER BY id ASC",new String[]{profile});
            activeQuestsList=KingmakerCampaignGate.playable(activeQuestsList,allCampaignQuests);
'''+s[at:]
 s=s.replace('firstQuest.optString("chapter_key", "")','KingmakerCampaignGate.chapterKey(firstQuest)')
 s=s.replace("ORDER BY id ASC LIMIT 1\",","ORDER BY id ASC\",",1)
 anchor='                if (!unstartedQuests.isEmpty()) {'
 s=replace_once(s,anchor,'''                unstartedQuests=KingmakerCampaignGate.playable(unstartedQuests,allCampaignQuests);
'''+anchor,"unstarted dependencies")
 s=s.replace('unstartedQuests.get(0).optString("chapter_key", "")','KingmakerCampaignGate.chapterKey(unstartedQuests.get(0))')
 anchor='            String guidanceText = buildGuidanceText(activeChapter, chapterTitle, currentStage, activeQuestsList, recommendedScene, sceneAction);'
 s=replace_once(s,anchor,'''            int officialChapter=!activeQuestsList.isEmpty()?KingmakerCampaignGate.officialChapterForQuest(activeQuestsList.get(0)):KingmakerCampaignGate.officialChapter(chapterKey);
            if(officialChapter>0)chapterTitle=KingmakerCampaignGate.officialTitle(officialChapter);
            out.put("official_chapter",officialChapter).put("campaign_chapter_count",11);
'''+anchor,"official chapter identity")
 return s
def rules(s):
 structure='    public JSONObject structure(String key){'
 wrapper='''    public JSONObject structure(String key){
        try{return KingmakerStructureSemantics.normalizeDefinition(structureCanonicalDefinition(key));}
        catch(Exception error){throw new IllegalStateException("Definição oficial de estrutura inválida.",error);}
    }
    private JSONObject structureCanonicalDefinition(String key){'''
 s=replace_once(s,structure,wrapper,"optional upgrades are not build prerequisites")
 anchor='    public JSONObject companionProfile(String companionKey) {'
 wrapper='''    public JSONObject companionProfile(String companionKey) {
        String canonical=canonicalCompanionKey(companionKey);
        try{return KingmakerCompanionSemantics.profile(canonical,companionProfileLegacy(canonical));}
        catch(Exception e){throw new IllegalStateException("Perfil oficial de companheiro inválido.",e);}
    }
    private JSONObject companionProfileLegacy(String companionKey) {'''
 return replace_once(s,anchor,wrapper,"book companion profiles")
def appdb(s):
 anchor='public void updateQuestStatus(long id,String status,String stage){ContentValues v=new ContentValues();'
 repl='''public void updateQuestStatus(long id,String status,String stage){JSONObject current=queryOne("SELECT * FROM quests WHERE id=? AND world_profile=?",new String[]{String.valueOf(id),getActiveMapProfile()});KingmakerCampaignGate.requireTransition(current,status,queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}));ContentValues v=new ContentValues();'''
 s=replace_once(s,anchor,repl,"quest update status gate")
 needle='if(previous.length()>0)id=previous.optLong("id");String title='
 s=replace_once(s,needle,'''if(previous.length()>0){id=previous.optLong("id");q=KingmakerCampaignGate.preserveDefinition(previous,q);KingmakerCampaignGate.requireTransition(previous,q.optString("status",previous.optString("status")),queryList("SELECT * FROM quests WHERE world_profile=?",new String[]{getActiveMapProfile()}));}String title=''', "AI quest update gate")
 return s

def importer(s):
 anchor='''            if (exists) {
                if (cols.contains("quest_key")) {'''
 repl='''            if (exists) {
                Cursor saved=null;
                try{
                    String identity=cols.contains("quest_key")?"quest_key":"title";
                    String match=cols.contains("quest_key")?q.questKey:q.title;
                    saved=db.rawQuery("SELECT * FROM quests WHERE world_profile=? AND "+identity+"=?",new String[]{worldProfile,match});
                    if(saved!=null&&saved.moveToFirst()){
                        for(String field:new String[]{"status","stage","created_at","progress_json"}){
                            int index=saved.getColumnIndex(field);if(index<0)continue;
                            if("created_at".equals(field))v.put(field,saved.getLong(index));
                            else if("progress_json".equals(field)){
                                JSONObject old=new JSONObject(saved.getString(index)==null?"{}":saved.getString(index));
                                JSONObject canonical=new JSONObject(v.get("progress_json")==null?"{}":String.valueOf(v.get("progress_json")));
                                for(String fixed:new String[]{"quest_key","chapter_key","milestone_xp","prerequisites","rewards"})if(canonical.has(fixed))old.put(fixed,canonical.opt(fixed));
                                v.put(field,old.toString());
                            }else v.put(field,saved.getString(index));
                        }
                    }
                }finally{if(saved!=null)saved.close();}
                if (cols.contains("quest_key")) {'''
 return replace_once(s,anchor,repl,"preserve live campaign progress on reimport")

def main(s):
 old='db.updateQuestStatus(q.optLong("id"),"completed","Concluída pelo jogador");showPartyDirector();'
 new='try{db.updateQuestStatus(q.optLong("id"),"completed","Concluída pelo jogador");showPartyDirector();}catch(Exception error){toast(error.getMessage());}'
 return replace_once(s,old,new,"quest UI rejection")
def companion_rewards_runtime(s):
 s=replace_once(s,'KingmakerCompanionSemantics.alignmentHistory(before,result.optInt("influence"),newAttitude,result.optJSONArray("all_unlocked_rewards"))','KingmakerCompanionSemantics.rewardHistory(before,result.optInt("influence"),newAttitude,result.optJSONArray("all_unlocked_rewards"))',"preserve official reward offers")
 helper='''    public JSONObject claimCompanionStructureReward(final String companionKey,final String settlementKey,final String structureKey,final String notes)throws Exception{
        requireEnabled();
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject companion=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
            if(companion.length()==0)throw new IllegalArgumentException("Companheiro inexistente.");
            JSONArray rewards=KingmakerCompanionSemantics.claimStructureReward(companion,structureKey);
            JSONObject structure=buildStructure(settlementKey,structureKey,notes,true,new JSONObject());
            ContentValues v=new ContentValues();v.put("unlocked_rewards_json",rewards.toString());v.put("updated_at",System.currentTimeMillis());
            int changed=db.getWritableDatabase().update("kingmaker_companions",v,"world_profile=? AND companion_key=?",new String[]{profile(),clean(companionKey)});
            if(changed!=1)throw new IllegalStateException("Não foi possível registrar o presente; a instalação foi revertida.");
            result[0]=new JSONObject().put("structure",structure).put("rp_spent",0).put("commodities_spent",0).put("companion_key",companionKey).put("reward_claimed",structureKey);
            audit("Presente de companheiro instalado",companion.optString("name")+" • "+structure.optString("name"),companion,result[0]);
        }});return result[0];
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def companion_rewards_ui(s):
 start=s.index('    private void showKingmakerCompanionDetails(');end=s.index('\n    private ',start+20)
 section=s[start:end]
 old='final String[] actions={"Ajustar influência","Editar função e missão"};'
 new='''final String[] actions=KingmakerCompanionSemantics.structureRewards(companion).length()>0?new String[]{"Ajustar influência","Editar função e missão","Instalar estrutura presenteada"}:new String[]{"Ajustar influência","Editar função e missão"};'''
 section=replace_once(section,old,new,"companion gift entry")
 section=replace_once(section,'else showKingmakerCompanionEditDialog(companion);','else if(which==1)showKingmakerCompanionEditDialog(companion);else showCompanionStructureRewardDialog(companion);',"gift route")
 s=s[:start]+section+s[end:]
 helper='''    private void showCompanionStructureRewardDialog(final JSONObject companion){
        final JSONArray rewards=KingmakerCompanionSemantics.structureRewards(companion);final List<JSONObject> towns=kingmakerRepository.settlements();
        if(rewards.length()==0||towns.isEmpty()){toast("O presente exige um assentamento e uma recompensa ainda disponível.");return;}
        final String[] rewardNames=new String[rewards.length()],townNames=new String[towns.size()];
        for(int i=0;i<rewardNames.length;i++){JSONObject def=kingmakerService.rules().structure(rewards.optString(i));rewardNames[i]=def.optString("name");}
        for(int i=0;i<townNames.length;i++)townNames[i]=towns.get(i).optString("name");
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("Estrutura gratuita do Companion Guide: exige espaço e o nível de construção; o presente é registrado uma única vez. Nenhum RP ou material é cobrado.",13,TEXT,false));
        final Spinner reward=spinner(rewardNames,rewardNames[0]),town=spinner(townNames,townNames[0]);form.addView(cardWithLabel("PRESENTE",reward));form.addView(cardWithLabel("ASSENTAMENTO",town));
        new AlertDialog.Builder(this).setTitle("Presente de "+companion.optString("name")).setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("INSTALAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            kingmakerRepository.claimCompanionStructureReward(companion.optString("companion_key"),towns.get(town.getSelectedItemPosition()).optString("settlement_key"),rewards.optString(reward.getSelectedItemPosition()),"Presente oficial de "+companion.optString("name"));
            toast("Estrutura gratuita instalada.");showKingmakerCompanionsDialog();
        }catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def campaign_runtime(s):
 helper='''    public JSONObject helixState(){
        JSONObject feature=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"campaign:helix_theorem"});
        return jsonObject(feature.optString("effects_json","{}"));
    }
    public boolean hasCanonicalCandlemere(){
        JSONObject chapter=db.queryOne("SELECT * FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?",new String[]{profile(),"chapter_08_they_lurk_below"});
        return "candlemere-r36-primary".equals(jsonObject(chapter.optString("metadata_json","{}")).optString("definition_revision"));
    }
    public JSONObject resolveHelixTheorem(final String room,final int natural,final int modifier,final boolean iconPrepared,final boolean placedClosed,final JSONArray present,final String notes)throws Exception{
        if(!hasCanonicalCandlemere())throw new IllegalStateException("Importe o capítulo de Candlemere antes de resolver o ritual.");
        final JSONObject[] result={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject before=helixState();
            for(int i=0;i<present.length();i++){
                long id=Long.parseLong(present.optString(i));boolean found=false;
                for(JSONObject hero:db.listActiveCharacters())if(hero.optLong("id")==id)found=true;
                if(!found)throw new IllegalArgumentException("Personagem presente não pertence à companhia ativa.");
            }
            result[0]=KingmakerCampaignGate.advanceHelix(before,room,natural,modifier,iconPrepared,placedClosed,present);
            JSONArray history=before.optJSONArray("history");if(history==null)history=new JSONArray();
            history.put(new JSONObject().put("room",room).put("natural_roll",natural).put("modifier",modifier).put("step",result[0].optInt("step")).put("completed",result[0].optBoolean("completed")).put("notes",clean(notes)));
            result[0].put("history",history);
            upsertFeature("campaign:helix_theorem","campaign_ritual","Teorema da Hélice",16,result[0].toString(),"Kingmaker pp.413,456",notes);
            audit("Teorema da Hélice",room+" • etapa "+result[0].optInt("step"),before,result[0]);
        }});return result[0];
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def campaign_importer(s):
 anchor='            persistQuests(db, worldProfile, catalog.chapterKey, catalog.quests, now);'
 s=replace_once(s,anchor,'            retireUnsupportedCampaignContent(db, worldProfile, chapterJson);\n'+anchor,"retire draft before official insertion")
 anchor='        // 1. Checa kingmaker_campaign_chapters'
 revision='''        if("chapter_08_they_lurk_below".equals(chapterKey)){
            Cursor revision=null;
            try{
                revision=db.rawQuery("SELECT metadata_json FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?",new String[]{worldProfile,chapterKey});
                if(revision!=null&&revision.moveToFirst())return "candlemere-r36-primary".equals(new JSONObject(revision.getString(0)).optString("definition_revision"));
            }catch(Exception ignored){}finally{if(revision!=null)revision.close();}
            return false;
        }
'''
 s=replace_once(s,anchor,revision+anchor,"chapter revision migration")
 helper='''    private static void retireUnsupportedCampaignContent(SQLiteDatabase db,String profile,JSONObject chapter)throws JSONException{
        JSONObject metadata=chapter.optJSONObject("metadata");if(metadata==null)return;
        JSONObject retired=metadata.optJSONObject("retirement");if(retired==null)return;
        JSONArray quests=retired.optJSONArray("quests");
        if(quests!=null&&tableExists(db,"quests")){
            Set<String> cols=getTableColumns(db,"quests");
            for(int i=0;i<quests.length();i++){
                JSONObject item=quests.optJSONObject(i);if(item==null)continue;
                ContentValues v=new ContentValues();v.put("status","archived");
                if(cols.contains("stage"))v.put("stage","Rascunho substituído pelo capítulo oficial de Candlemere; progresso anterior preservado no registro.");
                String field=cols.contains("quest_key")?"quest_key":"title";
                db.update("quests",v,"world_profile=? AND "+field+"=?",new String[]{profile,item.optString("quest_key".equals(field)?"key":"title")});
            }
        }
        for(String table:new String[]{"world_clocks","world_events","npc_memory_profiles","campaign_pois","tactical_scenes"}){
            JSONArray names=retired.optJSONArray(table);if(names==null||!tableExists(db,table))continue;
            Set<String> cols=getTableColumns(db,table);String field="world_events".equals(table)?"title":"tactical_scenes".equals(table)?"scene_key":"name";
            if(!cols.contains("world_profile")||!cols.contains(field))continue;
            ContentValues v=new ContentValues();
            if(cols.contains("active"))v.put("active",0);
            if("world_events".equals(table)&&cols.contains("status"))v.put("status","resolved");
            if("tactical_scenes".equals(table)&&cols.contains("status"))v.put("status","archived");
            if("campaign_pois".equals(table)){if(cols.contains("hidden"))v.put("hidden",1);if(cols.contains("discovered"))v.put("discovered",0);}
            if(v.size()==0)continue;
            for(int i=0;i<names.length();i++)db.update(table,v,"world_profile=? AND "+field+"=?",new String[]{profile,names.optString(i)});
        }
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def campaign_ui(s):
 anchor='km.addView(kingdomButton);b.addView(km);'
 s=replace_once(s,anchor,'''km.addView(kingdomButton);if(kingmakerEnabled&&kingmakerRepository.hasCanonicalCandlemere()){Button helix=button("CANDLEMERE: TEOREMA DA HÉLICE",PANEL2);helix.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showHelixTheoremDialog();}});km.addView(helix);}b.addView(km);''',"ritual entry point")
 helper='''    private void showHelixTheoremDialog(){
        JSONObject state=kingmakerRepository.helixState();
        if(state.optBoolean("completed")){new AlertDialog.Builder(this).setTitle("Teorema da Hélice concluído").setMessage("A Espiral foi neutralizada. Os personagens registrados como presentes recebem +4 de status nos testes de resistência contra teletransporte ou banimento enquanto estiverem na Casa na Margem do Tempo.").setPositiveButton("Fechar",null).show();return;}
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        form.addView(text("Etapa "+state.optInt("step")+" de 9. Ocultismo CD 36; falha ou ordem incorreta reinicia o ritual. Após B1, deposite o ícone fechado na alcova.",13,TEXT,false));
        final String[] rooms=KingmakerCampaignGate.HELIX_ROOMS;final Spinner room=spinner(rooms,state.optInt("step")<9?rooms[state.optInt("step")]:"B1");form.addView(cardWithLabel("CÂMARA ATUAL",room));
        final EditText natural=labeledField(form,"d20 natural","10",false),modifier=labeledField(form,"Modificador total de Ocultismo, incluindo Ajuda","0",false),notes=labeledField(form,"Notas","",true);
        final CheckBox icon=check("Ícone obtido em B14 ou preparado com Manufatura CD 38",false),placed=check("Ícone ainda fechado depositado na alcova de B1",false);form.addView(icon);form.addView(placed);
        final List<JSONObject> heroes=db.listActiveCharacters();final List<CheckBox> present=new ArrayList<CheckBox>();
        form.addView(text("PERSONAGENS PRESENTES EM CANDLEMERE AO CONCLUIR",12,GOLD,true));
        for(JSONObject hero:heroes){CheckBox box=check(hero.optString("name"),false);present.add(box);form.addView(box);}
        new AlertDialog.Builder(this).setTitle("Teorema da Hélice").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("RESOLVER ETAPA",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{
            JSONArray ids=new JSONArray();for(int i=0;i<heroes.size();i++)if(present.get(i).isChecked())ids.put(heroes.get(i).optLong("id"));
            JSONObject result=kingmakerRepository.resolveHelixTheorem(String.valueOf(room.getSelectedItem()),num(natural,10),num(modifier,0),icon.isChecked(),placed.isChecked(),ids,value(notes));
            toast(result.optBoolean("completed")?"Teorema concluído; recompensa registrada: 120 XP de aventura.":result.optBoolean("reset")?"Ritual reiniciado em B14.":"Etapa registrada: "+result.optInt("step")+"/9");showHelixTheoremDialog();
        }catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def structure_runtime(s):
 old='''    public JSONObject buildStructure(final String settlementKey,final String structureKey,final String notes)throws Exception{
        return buildStructure(settlementKey,structureKey,notes,false);
    }'''
 new='''    public JSONObject buildStructure(final String settlementKey,final String structureKey,final String notes)throws Exception{
        JSONObject definition=rules.structure(structureKey);if(definition==null)throw new IllegalArgumentException("Estrutura inexistente.");
        String skill=definition.optString("construction_skill");if(skill.length()==0){String options=definition.optString("construction_skills");skill=options.length()>0?options.split(",")[0]:"engineering";}
        JSONObject choices=new JSONObject().put("die1",new java.util.Random().nextInt(4)+1).put("die2",new java.util.Random().nextInt(4)+1);
        return resolveBuildStructure(settlementKey,structureKey,new java.util.Random().nextInt(20)+1,skill,notes,"",choices);
    }'''
 s=replace_once(s,old,new,"legacy builder performs real check instead of unchecked construction")
 anchor='        if(town.length()==0||def==null)throw new IllegalArgumentException("Assentamento ou estrutura inválidos.");'
 start=s.index('    public JSONObject resolveBuildStructure(');end=s.index('    public JSONObject resolveDemolish(',start)
 section=replace_once(s[start:end],anchor,anchor+'\n        KingmakerStructureSemantics.requireCapital(def,town,state().optString("capital_name"));',"palace only in capital")
 s=s[:start]+section+s[end:]
 signature='private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit)throws Exception{'
 s=replace_once(s,signature,signature+'''
        return recordCheckAtSettlement(settlementKey,activityKey,title,role,skillKey,natural,dc,sourceRef,notes,awardReputationOnCrit,new JSONObject());
    }
    private JSONObject recordCheckAtSettlement(String settlementKey,String activityKey,String title,String role,String skillKey,int natural,int dc,String sourceRef,String notes,boolean awardReputationOnCrit,JSONObject structureContext)throws Exception{''',"context-aware check overload")
 s=replace_once(s,'officialStructureActivityBonus(settlementKey,activity,skillKey,new JSONObject())','officialStructureActivityBonus(settlementKey,activity,skillKey,structureContext)',"context reaches structure calculation")
 start=s.index('    private int officialStructureActivityBonus(')
 end=s.index('    public int officialCommodityCapacity(',start)
 s=s[:start]+'''    private int officialStructureActivityBonus(String settlementKey,String activity,String skill,JSONObject context)throws Exception{
        JSONObject chosen=settlement(settlementKey);
        if(clean(settlementKey).length()>0&&chosen.length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        String targetHex=context.optString("hex_id",chosen.optString("hex_id"));
        if(targetHex.length()>0&&hex(targetHex).optInt("claimed")!=1)return 0;
        String capital=state().optString("capital_name");int best=0;
        for(JSONObject town:settlements()){
            boolean applies=capital.length()>0&&capital.equals(town.optString("name"));
            if(targetHex.length()>0&&hexDistance(targetHex,town.optString("hex_id"))<=Math.max(0,town.optInt("influence")))applies=true;
            if(!applies)continue;
            int bonus=KingmakerStructureSemantics.activityBonus(new JSONArray(structures(town.optString("settlement_key"))),activity,skill,town.optInt("max_item_bonus",1),context);
            best=Math.max(best,bonus);
        }return best;
    }
'''+s[end:]
 old='recordCheck("work_sites", "Canteiro de Trabalho: " + clean(siteType), "Tesoureiro", skill, naturalRoll, 0, "Region Activity", notes, false)'
 new='recordCheckAtSettlement("", "work_sites", "Canteiro de Trabalho: " + clean(siteType), "Tesoureiro", skill, naturalRoll, 0, "Region Activity", notes, false,new JSONObject().put("hex_id",hexId).put("site_type",key(siteType)))'
 s=replace_once(s,old,new,"work site location and commodity")
 s=replace_once(s,'recordCheck("establish_trade_agreement",','recordCheckAtSettlement(originSettlementKey,"establish_trade_agreement",',"trade agreement origin")
 # Events carry explicit settlement and danger metadata; kingdom-wide events do not inherit cemeteries.
 old='JSONObject checkResult = recordCheck("resolve_event", ev.optString("title"), "Liderança", skillKey, naturalRoll, targetDc, ev.optString("source_ref"), notes, false);'
 new='''JSONObject eventContext=jsonObject(ev.optString("resolution_json","{}")).optJSONObject("event_context");
        if(eventContext==null)eventContext=new JSONObject();
        String eventTown=eventContext.optString("settlement_key");
        JSONObject checkResult = recordCheckAtSettlement(eventTown,"resolve_event", ev.optString("title"), "Liderança", skillKey, naturalRoll, targetDc, ev.optString("source_ref"), notes, false,eventContext);'''
 s=replace_once(s,old,new,"event settlement context")
 old='        if (stateChanges.length() > 0) {\n            adjustState(stateChanges, "Resolução do evento: " + ev.optString("title"));'
 new='''        if(eventTown.length()>0&&eventContext.optBoolean("dangerous")&&stateChanges.optInt("unrest")>0){
            int reduction=KingmakerStructureSemantics.dangerousEventUnrestReduction(new JSONArray(structures(eventTown)));
            int original=stateChanges.optInt("unrest");stateChanges.put("unrest",Math.max(0,original-reduction));
            resolution.put("cemetery_unrest_prevented",Math.min(original,reduction));
            if(reduction>0)resolution.put("outcome_pt",checkResult.optString("degree_pt")+": Unrest final +"+stateChanges.optInt("unrest")+"; o cemitério impediu "+Math.min(original,reduction)+" ponto(s).");
        }
        resolution.put("event_context",eventContext).put("state_changes",stateChanges);
        if (stateChanges.length() > 0) {
            adjustState(stateChanges, "Resolução do evento: " + ev.optString("title"));'''
 s=replace_once(s,old,new,"cemetery applied to actual event mutation")
 helper='''    public void setSettlementEventContext(long eventId,String settlementKey,boolean dangerous)throws Exception{
        JSONObject before=db.queryOne("SELECT * FROM kingmaker_events WHERE id=? AND world_profile=?",new String[]{String.valueOf(eventId),profile()});
        if(before.length()==0||!"pending".equals(before.optString("status")))throw new IllegalStateException("O evento precisa estar pendente.");
        if(clean(settlementKey).length()>0&&settlement(settlementKey).length()==0)throw new IllegalArgumentException("Assentamento inexistente.");
        JSONObject details=jsonObject(before.optString("resolution_json","{}"));
        details.put("event_context",new JSONObject().put("settlement_key",clean(settlementKey)).put("dangerous",dangerous));
        ContentValues values=new ContentValues();values.put("resolution_json",details.toString());
        db.getWritableDatabase().update("kingmaker_events",values,"id=? AND world_profile=?",new String[]{String.valueOf(eventId),profile()});
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def event_ui(s):
 start=s.index('    private void showAddKingmakerEventDialog()')
 end=s.index('\n    private ',start+20)
 section=s[start:end]
 anchor='dc.setInputType(InputType.TYPE_CLASS_NUMBER);'
 replacement=anchor+'''final List<JSONObject> eventTowns=kingmakerRepository.settlements();final String[] townNames=new String[eventTowns.size()+1];townNames[0]="Todo o Reino";for(int i=0;i<eventTowns.size();i++)townNames[i+1]=eventTowns.get(i).optString("name");final Spinner townChoice=spinner(townNames,"Todo o Reino");form.addView(cardWithLabel("LOCAL DO EVENTO",townChoice));final CheckBox dangerous=new CheckBox(MainActivity.this);dangerous.setText("Evento perigoso do assentamento");form.addView(dangerous);'''
 section=replace_once(section,anchor,replacement,"event location UI")
 old='kingmakerRepository.addEvent(value(title),value(type),value(skill),num(dc,0),value(source),value(notes));'
 new='''JSONObject created=kingmakerRepository.addEvent(value(title),value(type),value(skill),num(dc,0),value(source),value(notes));int selection=townChoice.getSelectedItemPosition();kingmakerRepository.setSettlementEventContext(created.optLong("id"),selection==0?"":eventTowns.get(selection-1).optString("settlement_key"),dangerous.isChecked());'''
 section=replace_once(section,old,new,"event metadata saved")
 return s[:start]+section+s[end:]

def companion_runtime(s):
 s=replace_once(s,'if(comp.length()>0&&comp.optInt("available",1)!=1){','''if(comp.length()>0&&!KingmakerCompanionSemantics.canLead(comp,state().optString("reputation_mode","fame")))throw new IllegalStateException("Este companheiro ainda não aceita assumir liderança nas condições atuais.");
            if(comp.length()>0&&comp.optInt("available",1)!=1){''',"leadership influence gate")
 s=replace_once(s,'leader.optInt("invested")==1&&key(ability)',
 '(leader.optInt("invested")==1||officialAutomaticInvestment(leader))&&key(ability)',"automatic investment calculation")
 s=replace_once(s,'v.put("unlocked_rewards_json",result.optJSONArray("all_unlocked_rewards").toString());',
 'v.put("unlocked_rewards_json",KingmakerCompanionSemantics.alignmentHistory(before,result.optInt("influence"),newAttitude,result.optJSONArray("all_unlocked_rewards")).toString());',"persistent Nok alignment")
 s=replace_once(s,'if(leader.length()>0&&invested&&old.optInt("invested")==0){','if(leader.length()>0&&invested&&!officialAutomaticInvestmentForName(leader,roleKey)){',"extra companion investment does not consume manual quota")
 s=replace_once(s,'int count=db.queryOne("SELECT COUNT(*) AS total FROM kingmaker_leaders WHERE world_profile=? AND invested=1",new String[]{profile()}).optInt("total");','int count=0;for(JSONObject selected:leaders())if(!roleKey.equals(selected.optString("role_key"))&&selected.optInt("invested")==1&&!officialAutomaticInvestment(selected))count++;',"manual investments counted separately")
 helper='''    private boolean officialAutomaticInvestmentForName(String name,String role){
        for(JSONObject companion:companions())if(companion.optString("name").equals(name)||companion.optString("companion_key").equals(name))return KingmakerCompanionSemantics.automaticInvestment(companion,role);
        return false;
    }
    private boolean officialAutomaticInvestment(JSONObject leader){
        for(JSONObject companion:companions())if(companion.optString("name").equals(leader.optString("leader_name")))return KingmakerCompanionSemantics.automaticInvestment(companion,leader.optString("role_key"));
        return false;
    }
'''
 i=s.rfind("\n}");return s[:i]+"\n"+helper+s[i:]

def regression_support(root):
 # SELECT * must return row fields, not the scalar used by existence probes.
 p=root/"tests_pf2/java/mock_android/android/database/sqlite/SQLiteDatabase.java"
 s=p.read_text(encoding="utf-8")
 anchor='        if (sql.contains("FROM quests WHERE world_profile=? AND quest_key=?")) {'
 s=replace_once(s,anchor,'''        if (sql.startsWith("SELECT * FROM quests WHERE world_profile=? AND ")) {
            List<Map<String,Object>> matching=new ArrayList<Map<String,Object>>();
            String identity=sql.contains("AND quest_key=?")?"quest_key":"title";
            for(Map<String,Object> row:tables.get("quests"))
                if(selectionArgs[0].equals(row.get("world_profile"))&&selectionArgs[1].equals(row.get(identity)))matching.add(row);
            return new MockTableCursor(new ArrayList<String>(tableColumns.get("quests")),matching);
        }
'''+anchor,"mock row query")
 anchor='        if (sql.contains("FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?")) {'
 s=replace_once(s,anchor,'''        if(sql.startsWith("SELECT metadata_json FROM kingmaker_campaign_chapters WHERE world_profile=? AND chapter_key=?")){
            List<Map<String,Object>> matching=new ArrayList<Map<String,Object>>();
            for(Map<String,Object> row:tables.get("kingmaker_campaign_chapters"))if(selectionArgs[0].equals(row.get("world_profile"))&&selectionArgs[1].equals(row.get("chapter_key")))matching.add(row);
            return new MockTableCursor(Collections.singletonList("metadata_json"),matching);
        }
'''+anchor,"mock chapter revision query")
 h=root/"tests_pf2/java/Pf2KingmakerCompleteMechanicsHarness.java"
 t=h.read_text(encoding="utf-8")
 t=replace_once(t,'prof.optJSONArray("thresholds").length() >= 3','prof.optJSONArray("thresholds").length() >= 2',"published attitude thresholds")
 replacements={
 'check(newly1 != null && newly1.length() == 1, "reward unlocked at threshold 2");':'check(newly1 != null && newly1.length() == 0, "attitude alone grants no invented reward");',
 'check("amiri_friendly".equals(newly1.optJSONObject(0).optString("reward_id")), "amiri_friendly unlocked");':'check("friendly".equals(inf1.optString("attitude")), "official friendly threshold reached");',
 'check(newly2 != null && newly2.length() == 1, "only 1 newly unlocked reward at threshold 4");':'check(newly2 != null && newly2.length() == 0, "helpful alone grants no invented reward");',
 'check("amiri_helpful".equals(newly2.optJSONObject(0).optString("reward_id")), "amiri_helpful unlocked");':'check("helpful".equals(inf2.optString("attitude")), "official helpful threshold reached");',
 'check(inf2.optJSONArray("all_unlocked_rewards").length() == 2, "total 2 rewards unlocked");':'check(inf2.optJSONArray("all_unlocked_rewards").length() == 0, "no fabricated permanent boons");'}
 for a,b in replacements.items():t=replace_once(t,a,b,"obsolete companion reward contract")
 p.write_text(s,encoding="utf-8");h.write_text(t,encoding="utf-8")

def apply(root):
 root=Path(root);base=root/SRC
 if "VERSION_NAME=9.0.37" not in (root/"version.properties").read_text():raise ValueError("Only verified R36 eligible")
 transforms={"KingmakerRepository.java":lambda s:occupancy_repository(lot_repository(border_repository(twins_repository(repository_grid(repository_focus(repository_treasury(companion_rewards_runtime(campaign_runtime(structure_runtime(companion_runtime(structure_triggers(repository(s))))))))))))),"KingmakerRulesEngine.java":lambda s:rules_treasury(rules(s)),"KingmakerSoloDirector.java":director,"AppDb.java":appdb,"KingmakerCampaignImporter.java":lambda s:campaign_importer(importer(s)),"MainActivity.java":lambda s:lot_ui(border_ui(twins_ui(grid_ui(focus_ui(treasury_ui(companion_rewards_ui(campaign_ui(event_ui(structure_ui(main(s)))))))))))}
 prepared={name:fn((base/name).read_text(encoding="utf-8")) for name,fn in transforms.items()}
 for name,content in prepared.items():(base/name).write_text(content,encoding="utf-8")
 for name in ["KingmakerStructureSemantics.java","KingmakerCampaignGate.java","KingmakerCompanionSemantics.java"]:shutil.copyfile(HERE/"semantic"/name,base/name)
 regression_support(root)
 from patch_campaign_semantics import apply_campaign
 apply_campaign(root)
 from patch_campaign_regressions import apply_regressions
 apply_regressions(root)
 print("R36_THREE_SEMANTIC_DOMAINS_INTEGRATED")
if __name__=="__main__":
 if len(sys.argv)!=2:raise SystemExit("usage: patch_semantic_domains.py <R36_project>")
 apply(sys.argv[1])

