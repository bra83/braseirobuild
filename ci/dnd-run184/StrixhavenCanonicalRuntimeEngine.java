package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class StrixhavenCanonicalRuntimeEngine {
    private static final String SOURCE="Strixhaven: A Curriculum of Chaos";
    private static final String[][] EVENTS={
        {"First Day on Campus","Orientation Challenge","Frog's the Word","Exam: Slaadi","Work Hard, Play Harder","Exam: Owlbears","Campus Daredevils","Exam: Otyughs","All the World's a Stage","Secrets in Sedgemoor"},
        {"Back in Session","Mascots Everywhere!","Exam: Glyph of Warding","Party at the Rose Stage","Practice Makes Perfect","Exam: Symbol Glyphs","Getting Some Air","Dangerous Knowledge","Exam: Augury","Tell Us in Song!","Battle of Strixhaven"},
        {"Cordially Invited","Reception at Bow's End","Fate at Furygale","Exam: Relics","Dressing for Success","Party Preparations","Exam: Devices","Suspicions","Fancying Up Footwork","Preparing the Hall","Exam: Unearthed Arcana","A Starlit Night","Midnight at Lorehold","Aftermath"},
        {"A Sinister Shadow","A Brief Distraction","Exam: Vegetation Identification","Clue in the Bayou","No Time to Lose","Into the Badlands","This Is Surreal, Isn't It?","Ruins of Caerdoon","Confronting Murgaxor"}
    };
    private static final String[] CREATURES={"Archaic","Art Elemental Mascot","Beledros Witherbloom","Brackish Trudge","Cogwork Archivist","Daemogoth","Daemogoth Titan","First-Year Student","Fractal Mascot","Galazeth Prismari","Groff","Inkling Mascot","Lorehold Apprentice","Lorehold Pledgemage","Lorehold Professor of Chaos","Lorehold Professor of Order","Mage Hunter","Oracle of Strixhaven","Oriq Blood Mage","Oriq Recruiter","Pest Mascot","Prismari Apprentice","Prismari Pledgemage","Prismari Professor of Expression","Prismari Professor of Perfection","Quandrix Apprentice","Quandrix Pledgemage","Quandrix Professor of Substance","Quandrix Professor of Theory","Relic Sloth","Ruin Grinder","Shadrix Silverquill","Silverquill Apprentice","Silverquill Pledgemage","Silverquill Professor of Radiance","Silverquill Professor of Shadow","Spirit Statue Mascot","Strixhaven Campus Guide","Tanazir Quandrix","Velomachus Lorehold","Witherbloom Apprentice","Witherbloom Pledgemage","Witherbloom Professor of Decay","Witherbloom Professor of Growth"};
    private static String[] cache=new String[5];
    private StrixhavenCanonicalRuntimeEngine(){}

    private static String read(Context c,int chapter){
        int idx=chapter-3;if(idx<0||idx>=cache.length)return "";if(cache[idx]!=null)return cache[idx];
        try{InputStream in=c.getAssets().open("strixhaven/runtime/ch"+chapter+".txt");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();return cache[idx]=new String(out.toByteArray(),StandardCharsets.UTF_8);}catch(Exception e){AppLog.e("StrixhavenRuntime","Falha ao abrir capítulo "+chapter,e);return "";}
    }
    private static String norm(String s){return s==null?"":s.replace('’','\'').replace('‘','\'').replace('“','"').replace('”','"').toLowerCase(Locale.ROOT);}
    private static int findHeading(String source,String heading,int from){String s=norm(source),h=norm(heading);int p=s.indexOf(h,Math.max(0,from));if(p<0&&h.contains("owlbears"))p=s.indexOf(h.replace("owlbears","owibears"),Math.max(0,from));return p;}
    private static String key(int y){return "strixhaven_canonical_phase_y"+Math.max(1,Math.min(4,y));}
    public static JSONObject current(Context c,SharedPreferences p,int year){
        try{int y=Math.max(1,Math.min(4,year)),idx=Math.max(0,Math.min(EVENTS[y-1].length-1,p.getInt(key(y),0)));String src=read(c,y+2),title=EVENTS[y-1][idx];int start=findHeading(src,title,0);if(start<0)return new JSONObject().put("id","y"+y+"_"+idx).put("title",title).put("source","");
            int end=src.length();if(idx+1<EVENTS[y-1].length){int q=findHeading(src,EVENTS[y-1][idx+1],start+title.length());if(q>start)end=q;}String section=src.substring(start,Math.min(end,src.length())).trim();int level=levelAfter(y,title);
            return new JSONObject().put("id","y"+y+"_"+String.format(Locale.US,"%02d",idx+1)).put("year",y).put("index",idx).put("title",title).put("source",section).put("level_after",level).put("last",idx==EVENTS[y-1].length-1);
        }catch(Exception e){return new JSONObject();}
    }
    private static int levelAfter(int y,String t){if(y==1&&"Exam: Slaadi".equals(t))return 2;if(y==1&&"Campus Daredevils".equals(t))return 3;if(y==1&&"Secrets in Sedgemoor".equals(t))return 4;if(y==2&&"Exam: Glyph of Warding".equals(t))return 5;if(y==2&&"Battle of Strixhaven".equals(t))return 6;if(y==3&&"Preparing the Hall".equals(t))return 7;if(y==3&&"Aftermath".equals(t))return 8;if(y==4&&"Clue in the Bayou".equals(t))return 9;if(y==4&&"Ruins of Caerdoon".equals(t))return 10;return 0;}
    public static String promptContext(Context c,SharedPreferences p,int year,int semester){
        JSONObject e=current(c,p,year);if(e.length()==0)return "";String src=e.optString("source","");if(src.length()>9000)src=src.substring(0,9000);
        StringBuilder b=new StringBuilder("\n\nSTRIXHAVEN CANÔNICO LOCAL — sem PDF/RAG. Evento atual: ").append(e.optString("id")).append(" • ").append(e.optString("title")).append(". Ano ").append(year).append(", semestre ").append(semester).append(". ");
        b.append("Não pule para outro marco. Adapte somente à agência dos jogadores. Em narrative_state use strixhaven_phase_id='").append(e.optString("id")).append("' e strixhaven_phase_complete=true apenas quando este evento estiver realmente resolvido e não houver teste/consequência pendente.\nCONTEXTO OFICIAL COMPILADO:\n").append(src);
        String stats=relevantBestiary(c,src);if(stats.length()>0)b.append("\n\nFRIENDS AND FOES COMPILADO PERTINENTE:\n").append(stats);return b.toString();
    }
    private static String bestBlock(Context c,String name){
        String src=read(c,7),low=norm(src),q=norm(name);int best=-1,bestScore=-1,pos=0;while((pos=low.indexOf(q,pos))>=0){String tail=low.substring(pos,Math.min(low.length(),pos+6500));int score=0;if(tail.indexOf("armor class")>=0)score+=4;if(tail.indexOf("hit points")>=0)score+=4;if(tail.indexOf("challenge")>=0)score+=2;if(tail.indexOf("actions")>=0)score+=2;if(score>bestScore){bestScore=score;best=pos;}pos+=Math.max(1,q.length());}
        if(best<0)return "";int end=Math.min(src.length(),best+6500);return src.substring(best,end).trim();
    }
    private static String relevantBestiary(Context c,String event){
        String low=norm(event);StringBuilder b=new StringBuilder();int count=0;for(String n:CREATURES){if(!low.contains(norm(n)))continue;String block=bestBlock(c,n);if(block.length()==0)continue;if(block.length()>4200)block=block.substring(0,4200);if(count++>0)b.append("\n---\n");b.append(block);if(count>=5)break;}return b.toString();
    }
    public static JSONObject acceptSignal(Context c,SharedPreferences p,int year,JSONObject ns){
        JSONObject out=new JSONObject();try{JSONObject cur=current(c,p,year);if(cur.length()==0||ns==null)return out.put("advanced",false);if(!ns.optBoolean("strixhaven_phase_complete",false)||!cur.optString("id").equals(ns.optString("strixhaven_phase_id")))return out.put("advanced",false);
            int idx=cur.optInt("index"),y=cur.optInt("year"),next=Math.min(EVENTS[y-1].length-1,idx+1);p.edit().putInt(key(y),next).apply();return out.put("advanced",true).put("completed_id",cur.optString("id")).put("level_after",cur.optInt("level_after")).put("year_complete",cur.optBoolean("last")).put("next_title",cur.optBoolean("last")?"":EVENTS[y-1][next]);
        }catch(Exception e){return out;}
    }
    public static void reset(SharedPreferences p){if(p==null)return;SharedPreferences.Editor e=p.edit();for(int y=1;y<=4;y++)e.remove(key(y));e.apply();}
    public static JSONObject coverage(){try{return new JSONObject().put("years",4).put("events",44).put("friends_and_foes_entries",CREATURES.length).put("rag_required",false).put("source",SOURCE);}catch(Exception e){return new JSONObject();}}
}