"""Make daily twins preparations reachable through actual logged camp sleep."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError("Camp sleep anchor count="+str(s.count(a))+" "+a[:85])
 return s.replace(a,b,1)
def sleep_receipt_repository(s):
 s=once(s,'int day=Math.max(1,camp.optInt("day_number",1))+1;','int day=jsonObject(camp.optString("rest_json","{}")).optInt("completed_day",Math.max(1,camp.optInt("day_number",1))+1);')
 anchor='JSONObject persisted=upsertFeature("companion:twins_persona"'
 added='''if(previous.optInt("prepared_day")==day){out[0]=previous;return;}
            JSONObject clock=db.getSceneState();if(Math.max(1,clock.optInt("day_number",1))!=day)throw new IllegalStateException("Registre um descanso deste dia antes das preparações.");double start=clock.optDouble("clock_hour",SceneTemporalResolver.hourForLabel(clock.optString("time_of_day","Manhã"))),total=start+0.5d;
            double exact=total%24d;clock.put("clock_hour",exact).put("day_number",Math.max(1,clock.optInt("day_number",1))+(int)Math.floor(total/24d)).put("time_of_day",SceneTemporalResolver.labelForHour(exact));
            db.saveSceneStateChecked(clock);chosen.put("preparation_duration_minutes",30);
            '''
 s=once(s,anchor,added+anchor)
 helper='''    public JSONObject recordCompletedCampSleep(final String campKey)throws Exception{
        final JSONObject[] out={new JSONObject()};
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){public void run()throws Exception{
            JSONObject camp=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),clean(campKey)});
            if(camp.length()==0||!"active".equals(camp.optString("status")))throw new IllegalStateException("Selecione um acampamento ainda ativo.");
            if("RANDOM_ENCOUNTER".equals(camp.optString("phase"))||db.getCombatState().optInt("running")==1)throw new IllegalStateException("Resolva o encontro e conclua o combate antes de registrar o descanso.");
            List<JSONObject> heroes=db.listActiveCharacters();if(heroes.isEmpty())throw new IllegalStateException("Não há personagens ativos.");
            JSONObject rest=jsonObject(camp.optString("rest_json","{}"));
            if("logged_camp_hours".equals(rest.optString("sleep_authority"))){out[0]=camp;return;}
            JSONObject receipt=rules.resolveSleepAndRest(heroes.size(),1,0,"normal",new JSONArray(),camp.optInt("current_encounter_dc"),camp.optInt("encounter_dc"),camp.optInt("elapsed_hours"));
            int required=receipt.optInt("total_rest_hours"),logged=KingmakerCompanionSemantics.loggedSleepingHours(jsonArray(camp.optString("activities_json","[]")));
            if(logged<required||camp.optInt("elapsed_hours")<required)throw new IllegalStateException("Registre "+required+" horas de Descansar, incluindo a rotação de vigias, antes de confirmar esse descanso.");
            JSONArray ids=new JSONArray();for(JSONObject hero:heroes)ids.put(hero.optLong("id"));
            receipt=new JSONObject().put("total_rest_hours",required).put("watch_shift_duration",receipt.optString("watch_shift_duration")).put("sleep_completed",true);
            receipt.put("sleep_authority","logged_camp_hours").put("logged_sleep_hours",logged).put("completed_day",Math.max(1,db.getSceneState().optInt("day_number",1))).put("present_character_ids",ids).put("description","Descanso confirmado pelo histórico de horas e vigias do acampamento.");
            ContentValues v=new ContentValues();v.put("rest_json",receipt.toString());v.put("phase","SLEEP_AND_REST");v.put("step",5);
            db.getWritableDatabase().update("kingmaker_camps",v,"world_profile=? AND camp_key=?",new String[]{profile(),campKey});
            JSONObject actual=db.queryOne("SELECT * FROM kingmaker_camps WHERE world_profile=? AND camp_key=?",new String[]{profile(),campKey});
            if(!"logged_camp_hours".equals(jsonObject(actual.optString("rest_json","{}")).optString("sleep_authority")))throw new IllegalStateException("O comprovante do descanso não foi salvo.");
            audit("Descanso de acampamento confirmado",logged+" hora(s)",camp,actual);out[0]=actual;
        }});return out[0];
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
def sleep_receipt_ui(s):
 start=s.index('    private void showKingmakerCompleteCampDialog(');end=s.index('\n    private ',start+20);part=s[start:end]
 anchor='new AlertDialog.Builder(this).setTitle("Concluir acampamento")'
 part=once(part,anchor,'final CheckBox slept=check("Confirmar sono completo pelas horas de Descansar já registradas e pela rotação de vigias",false);form.addView(slept);'+anchor)
 anchor='kingmakerRepository.completeCamp(camp.optString("camp_key"),'
 part=once(part,anchor,'if(slept.isChecked())kingmakerRepository.recordCompletedCampSleep(camp.optString("camp_key"));'+anchor)
 return s[:start]+part+s[end:]

def sleep_receipt_rules(s):
 start=s.index('    public JSONObject aggregateCampEffects(');end=s.index('\n    public ',start+20)
 part=s[start:end]
 part=once(part,'.put("spells_and_focus_restored", true)','.put("spells_and_focus_restored", restResult!=null&&restResult.optBoolean("spells_prepared_restored")&&restResult.optBoolean("focus_points_restored"))')
 part=once(part,'.put("daily_preparations_complete", true)','.put("daily_preparations_complete", restResult!=null&&restResult.optBoolean("daily_preparations_complete"))')
 part=once(part,'.put("full_recovery", true)','.put("full_recovery", false)')
 part=once(part,'"Acampamento concluído conforme o procedimento oficial de 5 passos do Kingmaker."','"Acampamento encerrado; consulte os registros de refeições, descanso e preparações."')
 return s[:start]+part+s[end:]
