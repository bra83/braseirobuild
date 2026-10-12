"""Apply verified overnight recovery to actual sheets, once per 24 world hours."""
def recovery_repository(s):
 anchor='ContentValues v=new ContentValues();v.put("rest_json",receipt.toString());v.put("phase","SLEEP_AND_REST");v.put("step",5);'
 if s.count(anchor)!=1:raise ValueError('Camp recovery receipt anchor')
 s=s.replace(anchor,'receipt.put("character_recovery",applyVerifiedCampCharacterRecovery(campKey));\n            '+anchor,1)
 helper='''
    private JSONArray applyVerifiedCampCharacterRecovery(String campKey)throws Exception{
        JSONObject clock=db.getSceneState();double hour=clock.optDouble("clock_hour",SceneTemporalResolver.hourForLabel(clock.optString("time_of_day","Manhã")));
        if(Double.isNaN(hour)||Double.isInfinite(hour)||hour<0||hour>=24)throw new IllegalStateException("Horário do mundo inválido para o descanso.");double absolute=Math.max(1,clock.optInt("day_number",1))*24d+hour;
        JSONArray result=new JSONArray();Pathfinder2eRulesEngine core=new Pathfinder2eRulesEngine();
        for(JSONObject hero:db.listActiveCharacters()){
            long id=hero.optLong("id");JSONObject sheet=jsonObject(hero.optString("sheet_json","{}"));String featureKey="character:rest_recovery:"+id;
            JSONObject prior=sheet.optJSONObject("kingmaker_rest_recovery");if(prior==null)prior=new JSONObject();
            JSONObject recovery=KingmakerCompanionSemantics.naturalRestRecovery(hero,core.attributeModifier(hero,"Constituição"),prior,absolute);
            recovery.put("character_id",id).put("name",hero.optString("name")).put("camp_key",campKey);
            if(recovery.optBoolean("eligible")){
                if(recovery.optInt("after_hp")!=hero.optInt("hp"))db.setCharacterHpChecked(id,recovery.optInt("after_hp"));
                JSONObject saved=new JSONObject().put("rest_end_hours",absolute).put("camp_key",campKey).put("healed",recovery.optInt("healed")).put("before_hp",hero.optInt("hp")).put("after_hp",recovery.optInt("after_hp"));
                sheet=jsonObject(db.getCharacter(id).optString("sheet_json","{}"));sheet.put("kingmaker_rest_recovery",new JSONObject(saved.toString()));db.updateCharacterSheetChecked(id,sheet.toString());
                upsertFeature(featureKey,"character_rest","Descanso: "+hero.optString("name"),hero.optInt("level",1),saved.toString(),"PF2e recuperação natural de descanso; Kingmaker Companion Guide acampamento","Recuperação após sono confirmado pelo histórico do acampamento.");
            }
            result.put(recovery);
        }return result;
    }
'''
 i=s.rfind('\n}');return s[:i]+'\n'+helper+s[i:]
