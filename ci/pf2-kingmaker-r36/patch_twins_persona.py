"""Shared influence, daily persona and the active twin's invested leadership."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError('Twins anchor count='+str(s.count(a))+' '+a[:70])
 return s.replace(a,b,1)
def twins_repository(s):
 s=once(s,'KingmakerCompanionSemantics.automaticInvestment(companion,role);','KingmakerCompanionSemantics.automaticInvestment(companion,role,twinsCurrentPersona());')
 s=once(s,'KingmakerCompanionSemantics.automaticInvestment(companion,leader.optString("role_key"));','KingmakerCompanionSemantics.automaticInvestment(companion,leader.optString("role_key"),twinsCurrentPersona());')
 helper='''    public String twinsCurrentPersona(){
        JSONObject feature=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"companion:twins_persona"});
        String saved=jsonObject(feature.optString("effects_json","{}")).optString("persona");
        return saved.length()>0?saved:("infamy".equals(state().optString("reputation_mode"))?"kanerah":"kalikke");
    }
    public JSONObject prepareTwinsPersona(final String requested,final String campKey)throws Exception{
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject companion=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),"kalikke_kanerah"});
            JSONObject feature=db.queryOne("SELECT * FROM kingmaker_features WHERE world_profile=? AND feature_key=?",new String[]{profile(),"companion:twins_persona"});
            JSONObject previous=jsonObject(feature.optString("effects_json","{}"));
            JSONObject camp=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
            if(camp.length()==0||!"complete".equals(camp.optString("status"))||jsonObject(camp.optString("rest_json","{}")).optInt("total_rest_hours")<8)throw new IllegalStateException("As preparações exigem um descanso concluído e registrado.");
            if(db.getCombatState().optInt("running")==1)throw new IllegalStateException("Conclua o combate antes das preparações diárias.");
            int day=Math.max(1,camp.optInt("day_number",1))+1;
            JSONObject chosen=KingmakerCompanionSemantics.selectTwinsPersona(companion,previous,requested,day,twinsCurrentPersona()).put("rest_receipt",camp.optString("camp_key"));
            JSONObject persisted=upsertFeature("companion:twins_persona","companion_state","Preparações diárias — Kalikke/Kanerah",1,chosen.toString(),"Kingmaker Companion Guide pp.98–101","");
            JSONObject actual=jsonObject(persisted.optString("effects_json","{}"));
            if(!chosen.optString("persona").equals(actual.optString("persona"))||chosen.optInt("prepared_day",-1)!=actual.optInt("prepared_day",-2))throw new IllegalStateException("A persona não foi salva.");
            audit("Preparações diárias das gêmeas",requested,previous,chosen);out[0]=chosen;
        }});return out[0];
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def twins_ui(s):
 start=s.index('    private void showKingmakerCompanionDetails(');end=s.index('\n    private ',start+20);part=s[start:end]
 anchor='final String[] actions=KingmakerCompanionSemantics.structureRewards(companion).length()>0?'
 part=once(part,anchor,'final String[] actions="kalikke_kanerah".equals(companion.optString("companion_key"))?new String[]{"Ajustar influência","Editar função e missão","Preparações diárias — escolher persona"}:KingmakerCompanionSemantics.structureRewards(companion).length()>0?')
 part=once(part,'else showCompanionStructureRewardDialog(companion);','else if("kalikke_kanerah".equals(companion.optString("companion_key")))showTwinsDailyPersonaDialog();else showCompanionStructureRewardDialog(companion);')
 s=s[:start]+part+s[end:]
 helper='''    private void showTwinsDailyPersonaDialog(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(12),0,dp(12),dp(16));
        final List<JSONObject> rested=new ArrayList<JSONObject>();for(JSONObject camp:kingmakerRepository.camps(50))if("complete".equals(camp.optString("status"))&&parseLooseJson(camp.optString("rest_json","{}")).optInt("total_rest_hours")>=8)rested.add(camp);
        if(rested.isEmpty()){toast("Conclua e registre um descanso no acampamento antes das preparações.");return;}
        String[] receipts=new String[rested.size()];for(int i=0;i<receipts.length;i++)receipts[i]="Descanso do dia "+rested.get(i).optInt("day_number")+" — "+rested.get(i).optString("hex_id");
        final Spinner rest=spinner(receipts,receipts[0]);form.addView(cardWithLabel("DESCANSO CONCLUÍDO",rest));
        form.addView(text("Persona presente: "+kingmakerRepository.twinsCurrentPersona()+". A partir de Influência 8, escolha a irmã durante as preparações diárias após o descanso. Ela permanece até as próximas preparações, permitidas uma vez por dia. A influência é compartilhada.",13,TEXT,false));
        final Spinner persona=spinner(new String[]{"kalikke","kanerah"},kingmakerRepository.twinsCurrentPersona());form.addView(cardWithLabel("PERSONA",persona));
        new AlertDialog.Builder(this).setTitle("Preparações diárias das gêmeas").setView(scrollableDialogView(form)).setNegativeButton("Cancelar",null).setPositiveButton("PREPARAR",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){try{kingmakerRepository.prepareTwinsPersona(String.valueOf(persona.getSelectedItem()),rested.get(rest.getSelectedItemPosition()).optString("camp_key"));showKingmaker();toast("Persona salva para estas preparações diárias.");}catch(Exception error){toast(error.getMessage());}}}).show();
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
