"""Apply Harrim's actual camping protection to the persisted encounter check."""
def once(s,a,b):
 if s.count(a)!=1:raise ValueError("Harrim camp anchor count="+str(s.count(a))+" "+a[:90])
 return s.replace(a,b,1)
def harrim_camp_repository(s):
 start=s.index('    public JSONObject recordCampHour(');end=s.index('    public JSONObject resumeCampAfterEncounter(',start)
 part=s[start:end]
 old='JSONObject activity=jsonObject(activityJson);JSONArray activities=jsonArray(before.optString("activities_json","[]"));activities.put(activity);'
 new='''JSONObject activity=jsonObject(activityJson);JSONArray activities=jsonArray(before.optString("activities_json","[]"));
        int harrimProtection=0;
        if("harrim_blend_into_night".equals(activity.optString("companion_activity"))){
            JSONObject companion=db.queryOne("SELECT * FROM kingmaker_companions WHERE world_profile=? AND companion_key=?",new String[]{profile(),"harrim"});
            if(companion.length()==0||companion.optInt("available",1)!=1)throw new IllegalStateException("Harrim precisa estar disponível e participar deste acampamento.");
            if(before.optInt("current_encounter_dc")<=0)throw new IllegalStateException("Registre a CD de encontros da zona antes da proteção de Harrim.");
            harrimProtection=KingmakerCompanionSemantics.harrimCampingProtection(activities);
            activity.put("activity","Harrim — Mesclar-se à Noite").put("actor_companion_key","harrim").put("source_ref","Kingmaker Companion Guide p.95").put("encounter_dc_bonus",harrimProtection).put("result","A proteção aumenta a CD dos encontros noturnos em 2 durante este acampamento.");
        }
        activities.put(activity);'''
 part=once(part,old,new)
 part=once(part,'int dc=Math.max(0,before.optInt("current_encounter_dc",before.optInt("encounter_dc"))),hour=','int baseDc=Math.max(0,before.optInt("current_encounter_dc",before.optInt("encounter_dc")));int nightBonus=KingmakerCompanionSemantics.harrimEncounterDcBonus(activities,db.getSceneState().optString("time_of_day"));int dc=baseDc>0?baseDc+nightBonus:0,hour=')
 part=once(part,'v.put("current_encounter_dc",Math.max(0,dc-1))','v.put("current_encounter_dc",Math.max(0,baseDc-1))')
 return s[:start]+part+s[end:]
def harrim_camp_ui(s):
 start=s.index('    private void showKingmakerCampHourDialog(');end=s.index('\n    private ',start+20);part=s[start:end]
 part=once(part,'"Interagir com companheiro","Outra atividade"','"Interagir com companheiro","Harrim — Mesclar-se à Noite","Outra atividade"')
 part=once(part,'JSONObject updated=kingmakerRepository.recordCampHour(','''if("Harrim — Mesclar-se à Noite".equals(String.valueOf(activity.getSelectedItem())))activityJson.put("companion_activity","harrim_blend_into_night");JSONObject updated=kingmakerRepository.recordCampHour(''')
 return s[:start]+part+s[end:]
