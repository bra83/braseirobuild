package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Source-bound academic mechanics from Strixhaven: A Curriculum of Chaos, chapter 3.
 * Pure JSON transitions: no autonomous rolls and no hidden player choices.
 */
public final class StrixhavenAcademicRulesEngine {
    public static final String BOOK="Strixhaven: A Curriculum of Chaos";
    public static final String SCHEMA="strixhaven_academic_v1";
    private StrixhavenAcademicRulesEngine(){}

    public static JSONObject blank(){
        JSONObject o=new JSONObject();
        try{o.put("schema",SCHEMA).put("students",new JSONObject()).put("source_book",BOOK).put("source_pages","44-50").put("mechanics_compiled",true).put("auto_rolls",false);}catch(Exception ignored){}
        return o;
    }

    public static JSONObject normalize(JSONObject state){
        try{
            JSONObject o=state==null?blank():new JSONObject(state.toString());
            if(!SCHEMA.equals(o.optString("schema")))o=blank();
            if(o.optJSONObject("students")==null)o.put("students",new JSONObject());
            return o;
        }catch(Exception e){return blank();}
    }

    public static JSONObject student(JSONObject state,String studentId){
        JSONObject root=normalize(state);String id=clean(studentId,"student");
        try{
            JSONObject students=root.optJSONObject("students");JSONObject s=students.optJSONObject(id);
            if(s==null){s=new JSONObject().put("id",id).put("exam_history",new JSONArray()).put("student_dice",new JSONArray()).put("extracurriculars",new JSONArray()).put("job",new JSONObject()).put("relationships",new JSONObject()).put("tutoring_required",false).put("beloved_inspiration",0);students.put(id,s);}
            return new JSONObject(s.toString());
        }catch(Exception e){return new JSONObject();}
    }

    public static JSONObject putStudent(JSONObject state,String studentId,JSONObject student){
        JSONObject root=normalize(state);String id=clean(studentId,"student");
        try{root.optJSONObject("students").put(id,student==null?student(root,id):new JSONObject(student.toString()));}catch(Exception ignored){}
        return root;
    }

    public static JSONObject studyResolution(boolean checkSuccess,boolean groupStudy,boolean allNighter,boolean skipped){
        JSONObject o=new JSONObject();try{
            int rerolls=skipped?0:(checkSuccess?(allNighter?2:1):0);
            o.put("skipped",skipped).put("group_study_advantage",groupStudy&&!skipped).put("all_nighter",allNighter&&!skipped).put("check_success",checkSuccess&&!skipped).put("testing_rerolls",rerolls).put("exhaustion_levels_due",allNighter&&!skipped?1:0).put("source_page",47);
        }catch(Exception ignored){}return o;
    }

    public static JSONObject examResolution(boolean firstSuccess,boolean secondSuccess,boolean cheating,boolean deceptionSuccess,boolean sleightSuccess){
        JSONObject o=new JSONObject();try{
            String result;int dice;
            if(cheating){if(!deceptionSuccess||!sleightSuccess){result="failed";dice=0;}else{result="aced";dice=2;}}
            else{int successes=(firstSuccess?1:0)+(secondSuccess?1:0);result=successes==0?"failed":successes==1?"passed":"aced";dice=successes;}
            o.put("result",result).put("student_dice_gained",dice).put("tutoring_required","failed".equals(result)).put("cheating",cheating).put("study_rerolls_forbidden",cheating).put("source_page",47);
        }catch(Exception ignored){}return o;
    }

    public static JSONObject recordExam(JSONObject state,String studentId,String examId,int academicYear,String skillOne,String skillTwo,JSONObject examResult){
        JSONObject root=normalize(state),s=student(root,studentId);try{
            String result=examResult==null?"failed":examResult.optString("result","failed");int gained=examResult==null?0:Math.max(0,Math.min(2,examResult.optInt("student_dice_gained",0)));
            JSONArray hist=s.optJSONArray("exam_history");if(hist==null)hist=new JSONArray();JSONObject rec=new JSONObject().put("exam_id",clean(examId,"exam")).put("academic_year",Math.max(1,Math.min(4,academicYear))).put("result",result).put("skills",new JSONArray().put(clean(skillOne,"")).put(clean(skillTwo,""))).put("student_dice_gained",gained);if(examResult!=null){JSONObject study=examResult.optJSONObject("study");if(study!=null)rec.put("study",new JSONObject(study.toString()));rec.put("cheating",examResult.optBoolean("cheating",false));}hist.put(rec);s.put("exam_history",hist);
            String normalizedExamId=clean(examId,"exam");int normalizedYear=Math.max(1,Math.min(4,academicYear));
            if("failed".equals(result)){
                s.put("tutoring_required",true).put("tutoring_from_exam",normalizedExamId).put("tutoring_academic_year",normalizedYear);
            }else{
                boolean tutoring=s.optBoolean("tutoring_required",false);
                if(tutoring){
                    String failedExam=s.optString("tutoring_from_exam","");int failedYear=s.optInt("tutoring_academic_year",normalizedYear);
                    if(failedYear==normalizedYear&&!normalizedExamId.equalsIgnoreCase(failedExam)){
                        s.put("tutoring_required",false);
                        s.remove("tutoring_from_exam");
                        s.remove("tutoring_academic_year");
                    }
                }
                for(int i=0;i<gained;i++)addStudentDie(s,"exam:"+normalizedExamId+":"+i,new JSONArray().put(clean(skillOne,"")).put(clean(skillTwo,"")),"exam",normalizedYear);
            }
            return putStudent(root,studentId,s);
        }catch(Exception e){return root;}
    }

    public static JSONObject joinExtracurricular(JSONObject state,String studentId,String name,JSONArray skills,String memberNpc,boolean startOfAcademicYear,int relationshipDirection){
        JSONObject root=normalize(state),s=student(root,studentId);try{
            if(s.optBoolean("tutoring_required",false))return blocked(root,"tutoring_required");
            if(!startOfAcademicYear)return blocked(root,"join_only_at_start_of_academic_year");
            JSONArray extras=s.optJSONArray("extracurriculars");if(extras==null)extras=new JSONArray();String n=clean(name,"");if(n.isEmpty())return blocked(root,"extracurricular_required");
            for(int i=0;i<extras.length();i++)if(n.equalsIgnoreCase(extras.optJSONObject(i).optString("name")))return blocked(root,"already_joined");
            int max=s.optJSONObject("job")!=null&&s.optJSONObject("job").optBoolean("active",false)?1:2;if(extras.length()>=max)return blocked(root,"extracurricular_limit");
            JSONArray sk=skills==null?new JSONArray():new JSONArray(skills.toString());extras.put(new JSONObject().put("name",n).put("skills",sk).put("member_npc",clean(memberNpc,"")).put("active",true));s.put("extracurriculars",extras);addStudentDie(s,"extracurricular:"+slug(n),sk,"extracurricular",0);
            if(memberNpc!=null&&!memberNpc.trim().isEmpty())s=adjustRelationshipStudent(s,memberNpc,relationshipDirection>=0?1:-1,"","");
            return putStudent(root,studentId,s);
        }catch(Exception e){return root;}
    }

    public static JSONObject quitExtracurricular(JSONObject state,String studentId,String name){
        JSONObject root=normalize(state),s=student(root,studentId);try{
            JSONArray src=s.optJSONArray("extracurriculars"),dst=new JSONArray();String n=clean(name,"");if(src!=null)for(int i=0;i<src.length();i++){JSONObject x=src.optJSONObject(i);if(x!=null&&!n.equalsIgnoreCase(x.optString("name")))dst.put(x);}s.put("extracurriculars",dst);removeStudentDiceByPrefix(s,"extracurricular:"+slug(n));return putStudent(root,studentId,s);
        }catch(Exception e){return root;}
    }

    public static JSONObject setJob(JSONObject state,String studentId,String employer,String title,String coworker,int relationshipDirection){
        JSONObject root=normalize(state),s=student(root,studentId);try{
            if(s.optBoolean("tutoring_required",false))return blocked(root,"tutoring_required");
            JSONArray extras=s.optJSONArray("extracurriculars");if(extras!=null&&extras.length()>1)return blocked(root,"job_requires_at_most_one_extracurricular");
            String e=clean(employer,"");if(e.isEmpty())return blocked(root,"employer_required");s.put("job",new JSONObject().put("active",true).put("employer",e).put("title",clean(title,"")).put("coworker",clean(coworker,"")));
            if(coworker!=null&&!coworker.trim().isEmpty())s=adjustRelationshipStudent(s,coworker,relationshipDirection>=0?1:-1,"","");return putStudent(root,studentId,s);
        }catch(Exception ex){return root;}
    }

    public static JSONObject quitJob(JSONObject state,String studentId){JSONObject root=normalize(state),s=student(root,studentId);try{s.put("job",new JSONObject());return putStudent(root,studentId,s);}catch(Exception e){return root;}}

    public static JSONObject weeklyJobWage(JSONObject state,String studentId){JSONObject s=student(state,studentId),o=new JSONObject();try{boolean active=s.optJSONObject("job")!=null&&s.optJSONObject("job").optBoolean("active",false)&&!s.optBoolean("tutoring_required",false);o.put("eligible",active).put("gold_due",active?5:0).put("source_page",46);}catch(Exception ignored){}return o;}

    public static JSONObject claimWeeklyJobWage(JSONObject state,String studentId,int academicWeek){JSONObject root=normalize(state),s=student(root,studentId);try{JSONObject job=s.optJSONObject("job");boolean active=job!=null&&job.optBoolean("active",false)&&!s.optBoolean("tutoring_required",false);int week=Math.max(1,academicWeek),last=job==null?0:job.optInt("last_wage_week",0);if(!active)return blocked(root,"job_wage_not_active");if(week<=last)return blocked(root,"job_wage_already_claimed_for_week");job.put("last_wage_week",week);s.put("job",job);JSONObject next=putStudent(root,studentId,s);next.put("job_wage_claimed",true).put("gold_due",5).put("academic_week",week).put("source_page",46);return next;}catch(Exception e){return root;}}


    public static JSONObject adjustRelationship(JSONObject state,String studentId,String npc,int delta,String boon,String bane){JSONObject root=normalize(state),s=student(root,studentId);s=adjustRelationshipStudent(s,npc,delta,boon,bane);return putStudent(root,studentId,s);}

    private static JSONObject adjustRelationshipStudent(JSONObject s,String npc,int delta,String boon,String bane){
        try{String key=clean(npc,"");if(key.isEmpty())return s;JSONObject rels=s.optJSONObject("relationships");if(rels==null)rels=new JSONObject();JSONObject r=rels.optJSONObject(key);if(r==null)r=new JSONObject().put("npc",key).put("points",0).put("beloved",false);int points=r.optInt("points",0)+delta;r.put("points",points);if(boon!=null&&!boon.trim().isEmpty())r.put("bond_boon",boon.trim());if(bane!=null&&!bane.trim().isEmpty())r.put("bond_bane",bane.trim());if(r.optBoolean("beloved",false)&&points<1)r.put("beloved",false);String status=r.optBoolean("beloved",false)?"beloved":points>=2?"friend":points<=-2?"rival":"neutral";r.put("status",status);rels.put(key,r);s.put("relationships",rels);}catch(Exception ignored){}return s;
    }

    public static JSONObject deepenBeloved(JSONObject state,String studentId,String npc){JSONObject root=normalize(state),s=student(root,studentId);try{JSONObject rels=s.optJSONObject("relationships");JSONObject r=rels==null?null:rels.optJSONObject(clean(npc,""));if(r==null||r.optInt("points",0)<2)return blocked(root,"friendship_threshold_not_met");r.put("beloved",true).put("status","beloved");rels.put(clean(npc,""),r);s.put("relationships",rels);return putStudent(root,studentId,s);}catch(Exception e){return root;}}

    public static JSONObject endFriendship(JSONObject state,String studentId,String npc){JSONObject root=normalize(state),s=student(root,studentId);try{JSONObject rels=s.optJSONObject("relationships");JSONObject r=rels==null?null:rels.optJSONObject(clean(npc,""));if(r!=null){r.put("points",0).put("beloved",false).put("status","neutral");rels.put(clean(npc,""),r);s.put("relationships",rels);}return putStudent(root,studentId,s);}catch(Exception e){return root;}}

    public static JSONObject longRest(JSONObject state,String studentId,int proficiencyBonus){JSONObject root=normalize(state),s=student(root,studentId);try{JSONArray dice=s.optJSONArray("student_dice");if(dice!=null)for(int i=0;i<dice.length();i++){JSONObject d=dice.optJSONObject(i);if(d!=null)d.put("spent",false);}int beloved=0;JSONObject rels=s.optJSONObject("relationships");if(rels!=null){java.util.Iterator<String> keys=rels.keys();while(keys.hasNext()){JSONObject r=rels.optJSONObject(keys.next());if(r!=null&&r.optBoolean("beloved",false)&&r.optInt("points",0)>=1)beloved++;}}s.put("beloved_inspiration",Math.min(Math.max(0,proficiencyBonus),beloved));return putStudent(root,studentId,s);}catch(Exception e){return root;}}

    public static JSONObject spendBelovedInspiration(JSONObject state,String studentId){JSONObject root=normalize(state),s=student(root,studentId);try{int uses=Math.max(0,s.optInt("beloved_inspiration",0));if(uses<=0)return blocked(root,"no_beloved_inspiration");s.put("beloved_inspiration",uses-1);return putStudent(root,studentId,s);}catch(Exception e){return root;}}

    public static JSONObject studentDieReadiness(JSONObject state,String studentId,String skill){JSONObject s=student(state,studentId),o=new JSONObject();try{JSONArray dice=s.optJSONArray("student_dice");String wanted=clean(skill,"");int eligible=0;boolean tutoring=s.optBoolean("tutoring_required",false);if(dice!=null)for(int i=0;i<dice.length();i++){JSONObject d=dice.optJSONObject(i);if(d==null||d.optBoolean("spent",false))continue;if(tutoring&&"extracurricular".equals(d.optString("source_type")))continue;if(skillAllowed(d.optJSONArray("skills"),wanted))eligible++;}o.put("eligible",eligible>0).put("available_matching_dice",eligible).put("die","d4").put("max_per_check",1).put("source_page",44).put("tutoring_suspends_extracurricular_dice",tutoring);}catch(Exception ignored){}return o;}

    public static JSONObject spendStudentDie(JSONObject state,String studentId,String skill){JSONObject root=normalize(state),s=student(root,studentId);try{JSONArray dice=s.optJSONArray("student_dice");String wanted=clean(skill,"");boolean tutoring=s.optBoolean("tutoring_required",false);if(dice!=null)for(int i=0;i<dice.length();i++){JSONObject d=dice.optJSONObject(i);if(d==null||d.optBoolean("spent",false))continue;if(tutoring&&"extracurricular".equals(d.optString("source_type")))continue;if(skillAllowed(d.optJSONArray("skills"),wanted)){d.put("spent",true);s.put("student_dice",dice);JSONObject next=putStudent(root,studentId,s);try{next.put("student_die_spent",true).put("student_die_source",d.optString("source_id")).put("student_die","d4");}catch(Exception ignored){}return next;}}return blocked(root,"no_matching_student_die");}catch(Exception e){return root;}}

    public static JSONObject finishAcademicYear(JSONObject state,String studentId){JSONObject root=normalize(state),s=student(root,studentId);try{JSONArray src=s.optJSONArray("student_dice"),dst=new JSONArray();if(src!=null)for(int i=0;i<src.length();i++){JSONObject d=src.optJSONObject(i);if(d!=null&&!"exam".equals(d.optString("source_type")))dst.put(d);}s.put("student_dice",dst);return putStudent(root,studentId,s);}catch(Exception e){return root;}}

    public static JSONObject summary(JSONObject state,String studentId){JSONObject s=student(state,studentId),o=new JSONObject();try{JSONArray extras=s.optJSONArray("extracurriculars"),dice=s.optJSONArray("student_dice"),hist=s.optJSONArray("exam_history");JSONObject job=s.optJSONObject("job"),rels=s.optJSONObject("relationships");int beloved=0,friends=0,rivals=0;if(rels!=null){java.util.Iterator<String> keys=rels.keys();while(keys.hasNext()){JSONObject r=rels.optJSONObject(keys.next());if(r==null)continue;String status=r.optString("status");if("beloved".equals(status))beloved++;else if("friend".equals(status))friends++;else if("rival".equals(status))rivals++;}}o.put("tutoring_required",s.optBoolean("tutoring_required",false)).put("extracurricular_count",extras==null?0:extras.length()).put("job_active",job!=null&&job.optBoolean("active",false)).put("job",job==null?new JSONObject():job).put("student_dice_total",dice==null?0:dice.length()).put("exam_count",hist==null?0:hist.length()).put("friends",friends).put("rivals",rivals).put("beloveds",beloved).put("beloved_inspiration",s.optInt("beloved_inspiration",0)).put("source_book",BOOK).put("source_pages","44-50");}catch(Exception ignored){}return o;}

    private static void addStudentDie(JSONObject s,String sourceId,JSONArray skills,String sourceType,int academicYear)throws Exception{JSONArray dice=s.optJSONArray("student_dice");if(dice==null)dice=new JSONArray();dice.put(new JSONObject().put("source_id",sourceId).put("source_type",sourceType).put("skills",skills==null?new JSONArray():new JSONArray(skills.toString())).put("academic_year",academicYear).put("spent",false));s.put("student_dice",dice);}
    private static void removeStudentDiceByPrefix(JSONObject s,String prefix)throws Exception{JSONArray src=s.optJSONArray("student_dice"),dst=new JSONArray();if(src!=null)for(int i=0;i<src.length();i++){JSONObject d=src.optJSONObject(i);if(d!=null&&!d.optString("source_id").startsWith(prefix))dst.put(d);}s.put("student_dice",dst);}
    private static boolean skillAllowed(JSONArray skills,String wanted){if(skills==null||wanted.isEmpty())return false;for(int i=0;i<skills.length();i++)if(wanted.equalsIgnoreCase(skills.optString(i)))return true;return false;}
    private static JSONObject blocked(JSONObject state,String reason){try{return new JSONObject(state.toString()).put("academic_action_blocked",true).put("academic_block_reason",reason);}catch(Exception e){return state;}}
    private static String slug(String s){return clean(s,"").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");}
    private static String clean(String s,String fallback){String x=s==null?"":s.trim();return x.isEmpty()?fallback:x;}
}
