package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Deterministic runtime for the official four-year Strixhaven campaign. */
public final class StrixhavenOfficialCampaignRuntime {
    private StrixhavenOfficialCampaignRuntime(){}
    public static final String PREF_INDEX="strixhaven_official_event_index_v1";
    public static final String PREF_DONE="strixhaven_official_campaign_done_v1";
    private static final String PREFS="braseiro_prefs";

    public static final class Event {
        public final String id,title; public final int year,semester,chapter,levelAfter;
        Event(String id,int year,int semester,int chapter,String title,int levelAfter){
            this.id=id;this.year=year;this.semester=semester;this.chapter=chapter;this.title=title;this.levelAfter=levelAfter;
        }
    }
    private static Event e(String id,int y,int s,int c,String t,int lvl){return new Event(id,y,s,c,t,lvl);}
    private static final Event[] EVENTS=new Event[]{
        e("y1_first_day",1,1,3,"First Day on Campus",0),
        e("y1_orientation",1,1,3,"Orientation Challenge",0),
        e("y1_frogs",1,1,3,"Frog's the Word",0),
        e("y1_exam_slaadi",1,1,3,"Magical Physiologies — Exam: Slaadi",2),
        e("y1_work_hard",1,1,3,"Work Hard, Play Harder",0),
        e("y1_exam_owlbears",1,2,3,"Magical Physiologies — Exam: Owlbears",0),
        e("y1_daredevils",1,2,3,"Campus Daredevils",3),
        e("y1_exam_otyughs",1,2,3,"Magical Physiologies — Exam: Otyughs",0),
        e("y1_stage",1,2,3,"All the World's a Stage",0),
        e("y1_sedgemoor",1,2,3,"Secrets in Sedgemoor",4),

        e("y2_back",2,1,4,"Back in Session",0),
        e("y2_mascots",2,1,4,"Mascots Everywhere!",0),
        e("y2_exam_glyph",2,1,4,"Scrivening and Symbology — Exam: Glyph of Warding",5),
        e("y2_rose_party",2,1,4,"Party at the Rose Stage",0),
        e("y2_practice",2,1,4,"Practice Makes Perfect",0),
        e("y2_exam_symbol",2,1,4,"Scrivening and Symbology — Exam: Symbol Glyphs",0),
        e("y2_air",2,2,4,"Getting Some Air",0),
        e("y2_knowledge",2,2,4,"Dangerous Knowledge",0),
        e("y2_exam_augury",2,2,4,"Scrivening and Symbology — Exam: Augury",0),
        e("y2_song",2,2,4,"Tell Us in Song!",0),
        e("y2_battle",2,2,4,"Battle of Strixhaven",6),

        e("y3_invited",3,1,5,"Cordially Invited",0),
        e("y3_reception",3,1,5,"Reception at Bow's End",0),
        e("y3_furygale",3,1,5,"Fate at Furygale",0),
        e("y3_exam_relics",3,1,5,"Advanced Arcane History — Exam: Relics",0),
        e("y3_dressing",3,1,5,"Dressing for Success",0),
        e("y3_party_prep",3,1,5,"Party Preparations",0),
        e("y3_exam_devices",3,1,5,"Advanced Arcane History — Exam: Devices",0),
        e("y3_suspicions",3,2,5,"Suspicions",0),
        e("y3_footwork",3,2,5,"Fancying Up Footwork",0),
        e("y3_hall",3,2,5,"Preparing the Hall",7),
        e("y3_exam_unearthed",3,2,5,"Advanced Arcane History — Exam: Unearthed Arcana",0),
        e("y3_starlit",3,2,5,"A Starlit Night",0),
        e("y3_midnight",3,2,5,"Midnight at Lorehold",0),
        e("y3_aftermath",3,2,5,"Aftermath",8),

        e("y4_shadow",4,1,6,"A Sinister Shadow",0),
        e("y4_distraction",4,1,6,"A Brief Distraction",0),
        e("y4_exam_vegetation",4,1,6,"Arcane Herbology — Exam: Vegetation Identification",0),
        e("y4_bayou",4,1,6,"Clue in the Bayou",9),
        e("y4_no_time",4,1,6,"No Time to Lose",0),
        e("y4_badlands",4,2,6,"Into the Badlands",0),
        e("y4_surreal",4,2,6,"This Is Surreal, Isn't It?",0),
        e("y4_caerdoon",4,2,6,"Ruins of Caerdoon",10),
        e("y4_murgaxor",4,2,6,"Confronting Murgaxor",0)
    };

    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public static int count(){return EVENTS.length;}
    public static Event eventAt(int index){return EVENTS[Math.max(0,Math.min(EVENTS.length-1,index))];}
    public static int index(Context c){return Math.max(0,Math.min(EVENTS.length,prefs(c).getInt(PREF_INDEX,0)));}
    public static boolean finished(Context c){return prefs(c).getBoolean(PREF_DONE,false)||index(c)>=EVENTS.length;}
    public static Event current(Context c){int i=index(c);return i>=EVENTS.length?EVENTS[EVENTS.length-1]:EVENTS[i];}
    public static void reset(Context c){prefs(c).edit().putInt(PREF_INDEX,0).putBoolean(PREF_DONE,false).apply();}
    public static JSONObject progress(Context c){
        JSONObject o=new JSONObject();try{
            int i=index(c);Event ev=current(c);o.put("index",i).put("total",EVENTS.length).put("done",finished(c))
             .put("event_id",ev.id).put("title",ev.title).put("year",ev.year).put("semester",ev.semester)
             .put("chapter",ev.chapter).put("level_after",ev.levelAfter)
             .put("completed_events",Math.min(i,EVENTS.length)).put("percent",(int)Math.floor(100d*Math.min(i,EVENTS.length)/EVENTS.length));
        }catch(Exception ignored){}return o;
    }
    public static JSONObject advance(Context c,String expectedId){
        JSONObject out=new JSONObject();try{
            int i=index(c);if(i>=EVENTS.length)return progress(c).put("advanced",false);
            Event ev=EVENTS[i];if(expectedId!=null&&expectedId.length()>0&&!ev.id.equals(expectedId))return progress(c).put("advanced",false).put("blocked","event_mismatch");
            int next=i+1;SharedPreferences.Editor ed=prefs(c).edit().putInt(PREF_INDEX,next);if(next>=EVENTS.length)ed.putBoolean(PREF_DONE,true);ed.apply();
            out=progress(c);out.put("advanced",true).put("completed_event_id",ev.id).put("completed_title",ev.title).put("level_after",ev.levelAfter);
        }catch(Exception ignored){}return out;
    }
    public static boolean completionDeclared(JSONObject env,String narration,Event ev){
        if(ev==null)return false;
        if(env!=null&&env.optBoolean("strixhaven_event_complete",false)){
            String id=env.optString("strixhaven_event_id","");if(id.length()==0||id.equals(ev.id))return true;
        }
        String marker="[[BR_STRIX_EVENT_DONE:"+ev.id+"]]";
        return narration!=null&&narration.contains(marker);
    }
    public static String stripMarkers(String text){
        if(text==null)return "";return text.replaceAll("\\[\\[BR_STRIX_EVENT_DONE:[^\\]]+\\]\\]","").replaceAll("\\s{3,}","  ").trim();
    }
    private static String norm(String x){
        String n=Normalizer.normalize(x==null?"":x,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.US);
        return n.replace('’','\'').replace('“','"').replace('”','"');
    }
    private static String readAsset(Context c,String path){
        try(InputStream in=c.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
    private static int bestHeading(String source,String title){
        String ns=norm(source),nt=norm(title);int from=0,best=-1,bestScore=-999999;
        while(true){int p=ns.indexOf(nt,from);if(p<0)break;int score=p;
            int lo=Math.max(0,p-80),hi=Math.min(ns.length(),p+nt.length()+160);
            String around=ns.substring(lo,hi);
            if(around.contains("\n"+nt))score+=1000000;
            if(p>4000)score+=300000;
            if(score>bestScore){bestScore=score;best=p;}from=p+Math.max(1,nt.length());
        }return best;
    }
    public static String sourceForCurrentEvent(Context c){
        Event ev=current(c);String src=readAsset(c,"strixhaven_runtime/ch"+ev.chapter+".txt");if(src.length()==0)return "";
        int p=bestHeading(src,ev.title);if(p<0){
            String shortTitle=ev.title.replaceAll("^.*—\\s*","").trim();p=bestHeading(src,shortTitle);
        }
        if(p<0)return src.substring(0,Math.min(9000,src.length()));
        int a=Math.max(0,p-900),b=Math.min(src.length(),p+10500);return src.substring(a,b);
    }
    public static String promptContext(Context c){
        if(finished(c))return " CAMPANHA OFICIAL STRIXHAVEN: os quatro anos foram concluídos. Não reinicie eventos já resolvidos. ";
        Event ev=current(c);String source=sourceForCurrentEvent(c);
        String bestiary=StrixhavenBestiaryRuntimeEngine.sourceForMentions(c,source,4,11000);
        StringBuilder b=new StringBuilder();
        b.append("\n\n=== STRIXHAVEN OFICIAL — RUNTIME COMPILADO ===\n");
        b.append("Evento atual [").append(index(c)+1).append("/").append(EVENTS.length).append("]: ").append(ev.title)
         .append(" | id=").append(ev.id).append(" | ano ").append(ev.year).append(" | semestre ").append(ev.semester).append(".\n");
        b.append("Use o trecho-fonte compilado abaixo como autoridade desta etapa. Não substitua a aventura por conteúdo escolar genérico e não antecipe revelações de eventos futuros. ");
        b.append("Só declare strixhaven_event_complete=true quando os objetivos/conflitos centrais deste evento tiverem sido efetivamente resolvidos na ficção. ");
        b.append("Quando declarar conclusão, inclua também strixhaven_event_id='").append(ev.id).append("' no envelope JSON. Não conclua evento por mera conversa, entrada na área ou uma rolagem parcial.\n");
        if(source.length()>0)b.append("FONTE COMPILADA DO EVENTO:\n").append(source);
        if(bestiary.length()>0)b.append("\nBESTIÁRIO COMPILADO RELEVANTE:\n").append(bestiary);
        b.append("\n=== FIM DO RUNTIME STRIXHAVEN ===\n");
        return b.toString();
    }
    public static JSONArray eventManifest(){
        JSONArray a=new JSONArray();for(Event ev:EVENTS)try{a.put(new JSONObject().put("id",ev.id).put("title",ev.title).put("year",ev.year).put("semester",ev.semester).put("chapter",ev.chapter).put("level_after",ev.levelAfter));}catch(Exception ignored){}return a;
    }
}
